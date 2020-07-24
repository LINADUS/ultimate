#!/usr/bin/env python3

import argparse
import collections
import multiprocessing
import ntpath
import os
import re
import signal
import sys
import xml.etree.ElementTree as ET
from functools import lru_cache
from typing import Tuple, List, Iterator, Any, Dict, Optional, cast, Pattern, ChainMap

import yaml
from tqdm import tqdm

# some type defs
# first is category, second is message
Classification = Tuple[str, str]


class UnsupportedLogFile(ValueError):
    """
    Raised when the log file is not an Ultimate log file
    """

    pass


class Result:
    version: str
    call: str
    result: Classification
    logfile: str

    def __init__(
        self,
        logfile: Optional[str],
        result: Optional[Classification],
        call: Optional[str],
        version: Optional[str],
    ) -> None:
        self.logfile = logfile
        self.version = version
        self.call = call
        self.result = result

    def __str__(self) -> str:
        return str(self.result)

    @lru_cache(maxsize=1)
    def input_file(self) -> Optional[str]:
        if self.call is None:
            return None
        regex = r"-i\s+[\"]?(.*)[\"]?\s?"
        match = re.search(regex, self.call)
        if match is None:
            return None
        return match.group(1)


class Run:
    logfile: str
    cputime: float
    walltime: float
    memory: int
    status: str
    category: str
    options: str

    def __init__(self, xml_run: ET.Element, logfile: str) -> None:
        self.logfile = logfile
        self.options = xml_run.attrib["options"]
        self.status = Run.__get_column_value(xml_run, "status")
        self.category = Run.__get_column_value(xml_run, "category")
        self.cputime = Run.__time_to_float(Run.__get_column_value(xml_run, "cputime"))
        self.walltime = Run.__time_to_float(Run.__get_column_value(xml_run, "walltime"))
        self.memory = Run.__byte_to_int(Run.__get_column_value(xml_run, "memory"))

    def is_timeout(self) -> bool:
        return self.status == "TIMEOUT"

    def is_oom(self) -> bool:
        return self.status == "OUT OF MEMORY"

    def is_unsound(self) -> bool:
        return self.category == "wrong"

    @staticmethod
    def __get_column_value(xml_run: ET.Element, title: str) -> Optional[str]:
        column = xml_run.find(f"./column[@title='{title}']")
        if column is None:
            return None
        return column.attrib["value"]

    @staticmethod
    def __time_to_float(val: str) -> float:
        """Remove second unit from benchexec time and convert to float """
        return float(val[:-1]) if val else None

    @staticmethod
    def __byte_to_int(val: str) -> int:
        """Remove byte unit from benchexec time and convert to float """
        return int(val[:-1]) if val else None


class MessageClassifier:
    category: str
    message: str
    show_line: bool
    dump_smt: bool
    delta_debug: bool

    def __init__(
        self, category: str, message: str, values: Dict[str, Any] = None
    ) -> None:
        self.category = category
        self.message = message
        self.show_line = True
        self.dump_smt = False
        self.delta_debug = False

        if values:
            self.show_line = values.get("show_line", self.show_line)
            self.dump_smt = values.get("dump_smt", self.dump_smt)
            self.delta_debug = values.get("delta_debug", self.delta_debug)

    def __str__(self) -> str:
        return f"[{type(self).__name__}] {self.category}: {self.message} show_line={self.show_line} dump_smt={self.dump_smt} delta_debug={self.delta_debug}"


# global variables
order: List[Dict[str, MessageClassifier]] = []
interesting_strings: ChainMap[str, MessageClassifier] = collections.ChainMap()
str_no_result_unknown: str = "Unknown"
str_benchexec_timeout: str = "Timeout by benchexec"
str_benchexec_oom: str = "OOM by benchexec"
version_matcher: Pattern[str] = re.compile(r"^.*(\d+\.\d+\.\d+-\w+).*$")
enable_debug: bool = False


def class_idx(result: Classification) -> int:
    if not result or not result[0]:
        return len(order) + 1
    return [i for i, e in enumerate(order) if result[0] in e][0]


def signal_handler(sig, frame) -> None:
    if sig == signal.SIGTERM:
        print("Killed by {}".format(sig))
    print("Abort by user: you pressed Ctrl+C!")
    sys.exit(2)


def limit(msg: str, lim: int) -> str:
    if lim < 4:
        raise ValueError("limit must be larger or equal 4 but was {}".format(lim))
    if len(msg) > lim:
        return msg[0 : lim - 3] + "..."
    return msg.ljust(lim, " ")


def debug(msg: str) -> None:
    if enable_debug:
        print(msg)


def parse_args() -> argparse.Namespace:
    try:
        parser = argparse.ArgumentParser(
            description="Scan Ultimate log file for exception results"
        )
        parser.add_argument(
            "-i",
            "--input",
            nargs=1,
            metavar="<dir>",
            required=True,
            help="Specify directory containing Ultimate log files or single log file",
        )
        args = parser.parse_args()
        if not os.path.isdir(args.input[0]) and not os.path.isfile(args.input[0]):
            print("Input does not exist")
            sys.exit(1)
        return args
    except argparse.ArgumentError as exc:
        print(exc.message + "\n" + exc.argument)
        sys.exit(1)


def scan_line(
    line: str, result: Optional[Classification], line_iter: Iterator[str]
) -> Classification:
    new_result = None
    debug("Looking at line {}".format(line))

    for message, mc in interesting_strings.items():
        if message in line:
            if mc.show_line:
                new_result = message, line
            else:
                new_result = message, line_iter.__next__()
            debug("Found result {} with line {}".format(message, line))
            break

    if not result and new_result:
        return new_result
    if result and not new_result:
        return result
    if not result and not new_result:
        return result

    new_class = class_idx(new_result)
    old_class = class_idx(result)
    if new_class < old_class:
        return new_result
    debug("Keeping old result because new one has lower priority")
    return result


def rescan_wrapper_preamble(file: str, call: str, version: str) -> List[Result]:
    """
    If there was no result in the wrapper script log so far, we rescan it and search for errors reported directly by
    the wrapper script
    """
    debug("Rescanning wrapper preamble")
    with open(file, "rb") as f:
        # If the wrapper script was killed without any chance to print a message, the last elements are dots.
        # In this case we group the result as timeout and return a hardcoded line
        f.seek(-3, 2)
        last_elems = f.read()
        if b"..." == last_elems:
            return [Result(file, ("Killed from outside", "..."), call, version)]
        else:
            debug("Last 3 elements of file are {}".format(last_elems))

    with open(file) as f:
        lines = [line.rstrip("\n") for line in f].__iter__()

        regex_file_does_not_exist = re.compile(".*File.*does not exist")
        classification = None
        for line in lines:
            if not line:
                continue
            if "Ultimate.py: error: argument" in line:
                debug("Found argument error")
                # hacky special case
                if "--validate" in line and regex_file_does_not_exist.match(line):
                    return [Result(file, scan_line(line, None, lines), None, None)]
                return [Result(file, None, None, None)]
            else:
                classification = scan_line(line, classification, lines)

        return [Result(file, classification, call, version)]


def process_wrapper_script_log(file: str) -> List[Result]:
    results: List[Result] = []
    default: bool = True
    wrapper_preamble: bool = True
    collect_call: bool = False
    version: Optional[str] = None
    classification: Optional[Classification] = None
    default_call: Optional[str] = None
    bitvec_call: Optional[str] = None
    with open(file) as f:
        lines = [line.rstrip("\n") for line in f].__iter__()
        for line in lines:
            if not line:
                continue
            if wrapper_preamble:
                if "Using bit-precise analysis" in line:
                    default = False
                elif line.startswith("Calling Ultimate with:"):
                    call: List[str] = [line]
                    collect_call = True
                elif collect_call:
                    if "Execution finished normally" in line:
                        collect_call = False
                        if default:
                            default_call = cast(str, call[:-1])
                            debug("Found default call {}".format(default_call))
                        else:
                            bitvec_call = cast(str, call[:-1])
                            debug("Found bitvector call {}".format(bitvec_call))
                    else:
                        call += [line]
                elif "--- Real Ultimate output ---" in line:
                    wrapper_preamble = False
            else:
                if line.startswith("This is Ultimate"):
                    new_version = version_matcher.findall(line)[0]
                    if version and not new_version == version:
                        raise ValueError(
                            "Found different Ultimate versions in one log file. First was {} and second was {}".format(
                                version, new_version
                            )
                        )
                    version = new_version
                    debug("Found Ultimate version {}".format(version))
                elif "### Bit-precise run ###" in line:
                    debug("Found default result: {}".format(classification))
                    results += [Result(file, classification, default_call, version)]
                    classification = None
                else:
                    classification = scan_line(line, classification, lines)
    if bitvec_call:
        debug("Found bitvec result: {}".format(classification))
        results += [Result(file, classification, bitvec_call, version)]
    if not results:
        if classification and default_call:
            # case where the bitvector run did not start, e.g., termination
            debug("Using default result: {}".format(classification))
            return [Result(file, classification, default_call, version)]
        debug("No results for file {}".format(file))
        return rescan_wrapper_preamble(
            file,
            default_call if default_call else (bitvec_call if bitvec_call else None),
            version,
        )
    return results


def process_direct_call_log(file: str) -> List[Result]:
    result = None
    with open(file) as f:
        version = ""
        lines = [line.rstrip("\n") for line in f].__iter__()
        for line in lines:
            if not line:
                continue
            if "/java " in line or line.startswith("java "):
                call = line
                debug("Found Ultimate call {}".format(call))
            elif line.startswith("This is Ultimate"):
                version = version_matcher.findall(line)[0]
                debug("Found Ultimate version {}".format(version))
            else:
                result = scan_line(line, result, lines)
        if call is None:
            print(file + " has no call")
        return [Result(file, result, call, version)]


def process_log_file(file: str) -> List[Result]:
    with open(file) as f:
        lines = [line.rstrip("\n") for line in f]
        for line in lines:
            if "Ultimate.py" in line:
                debug("Wrapper script detected")
                return process_wrapper_script_log(file)
            elif "This is Ultimate" in line:
                debug("No wrapper script detected")
                return process_direct_call_log(file)
    raise UnsupportedLogFile(
        "Encountered unrecognized file (not an Ultimate log file): {}".format(file)
    )


def print_results(results: List[Result]) -> None:
    cat_cnt = collections.Counter()
    for r in results:
        cat_cnt[r.result[0]] += 1

    print("Categories")
    for cat, i in cat_cnt.most_common():
        print("{:>7}  {}".format(i, cat))
    print()

    print("Actual results")
    result_cnt = collections.Counter()
    processed = {}
    for r in results:
        if r.result[0] == str_no_result_unknown or not interesting_strings[r.result[0]]:
            key = r.result[1]
        else:
            key = r.result[0]
        result_cnt[key] += 1
        processed[key] = r

    resort = []
    for subcat, j in result_cnt.most_common():
        r = processed[subcat].result
        msg = "{:>7}  {}  {}:".format(j, limit(r[0], 20), r[1])
        if j < 10:
            resort += [msg]
        else:
            print(msg)

    for msg in sorted(resort, reverse=True):
        print(msg)


def set_unknowns(
    results: List[Result], file: str, runs: Dict[str, Run]
) -> List[Result]:
    real_results = []
    for r in results:
        if r.result is None:
            basename = ntpath.basename(file)
            run = runs[basename]
            if run.is_timeout():
                real_results += [
                    Result(file, (str_benchexec_timeout, "..."), r.call, r.version)
                ]
            elif run.is_oom():
                real_results += [
                    Result(file, (str_benchexec_oom, "..."), r.call, r.version)
                ]
            else:
                real_results += [
                    Result(file, (str_no_result_unknown, file), r.call, r.version)
                ]
        else:
            real_results += [r]
    return real_results


def list_logfile_paths_in_dir(input_dir: str) -> Iterator[str]:
    for dirpath, dirnames, files in os.walk(input_dir):
        for file in files:
            if not file.endswith(".log"):
                continue
            yield os.path.join(dirpath, file)


def list_xml_filepaths(input_dir: str) -> Iterator[str]:
    for xml in os.listdir(input_dir):
        file = os.path.join(input_dir, xml)
        if os.path.isfile(file) and file.endswith(".xml"):
            yield file


def consume_task(queue, results, runs, o, i) -> None:
    global order
    global interesting_strings
    order = o
    interesting_strings = i
    tmp_result = []
    while True:
        path = queue.get()
        if path is None:
            break
        try:
            tmp_result += set_unknowns(process_log_file(path), path, runs)
        except UnsupportedLogFile:
            continue
    results += tmp_result


def process_input_dir(input_dir: str, runs: Dict[str, Run]) -> Tuple[int, List[Result]]:
    results: List[Result] = multiprocessing.Manager().list()
    if os.path.isfile(input_dir):
        results += set_unknowns(process_log_file(input_dir), input_dir, runs)
        log_file_count = 1
    else:
        local_cores = max(multiprocessing.cpu_count() - 4, 1)
        queue = multiprocessing.Queue(maxsize=local_cores)
        pool = multiprocessing.Pool(
            local_cores,
            initializer=consume_task,
            initargs=(queue, results, runs, order, interesting_strings),
        )

        progress_bar = tqdm([i for i in list_logfile_paths_in_dir(input_dir)])
        log_file_count = len(progress_bar)

        for path in progress_bar:
            progress_bar.set_description(
                "Processing ...{:100.100} [{:>3}C]".format(
                    path[len(input_dir) :], local_cores
                )
            )
            queue.put(path)

        # tell workers we're done
        for _ in range(local_cores):
            queue.put(None)
        pool.close()
        pool.join()
    return log_file_count, results


def parse_benchexec_xmls(input_dir: str) -> Tuple[Dict[str, Run], bool]:
    xml_files = [f for f in list_xml_filepaths(input_dir)]
    if len(xml_files) == 0:
        print(
            "There are no benchexec .xml files in {}, cannot exclude timeouts properly".format(
                input_dir
            )
        )
        return {}, False

    rtr = {}
    for xml in list_xml_filepaths(input_dir):
        root = ET.parse(xml).getroot()
        result = root.find(".")
        name = result.attrib["name"].split(".")
        tool_name = name[0]
        for elem in root.findall(".//run"):
            # files = elem.attrib["files"]
            yml = elem.attrib["name"]
            base_yml = ntpath.basename(yml)
            logfile_name = "{}.{}.log".format(tool_name, base_yml)
            rtr[logfile_name] = Run(elem, logfile_name)

    return rtr, True


def read_yaml_files() -> None:
    conf_file_name = "get-benchexec-overview.yaml"
    script_dir = os.path.dirname(os.path.realpath(__file__))

    with open(os.path.join(script_dir, conf_file_name)) as conf_file:
        conf = {}
        try:
            conf_yaml = yaml.safe_load(conf_file)
            for category, values in conf_yaml.items():
                conf[category] = {
                    k: MessageClassifier(category, k, v) for k, v in values.items()
                }

        except yaml.YAMLError as exc:
            print(f"Could not load configuration file {conf_file_name}: {exc}")
            sys.exit(1)

        global order
        global interesting_strings

        order = [
            conf["known_exceptions"],
            conf["known_timeouts"],
            {
                str_benchexec_timeout: MessageClassifier(
                    "known_timeouts", str_benchexec_timeout
                ),
                str_benchexec_oom: MessageClassifier(
                    "known_timeouts", str_benchexec_oom
                ),
            },
            conf["known_unsafe"],
            conf["known_unknown"],
            conf["known_safe"],
            conf["known_wrapper_errors"],
        ]

        interesting_strings = collections.ChainMap(*order)

        # todo: populate globals from conf


def main() -> None:
    args = parse_args()
    input_dir = args.input[0]

    read_yaml_files()
    runs, benchexec_xml = parse_benchexec_xmls(input_dir)
    log_file_count, results = process_input_dir(input_dir, runs)

    if log_file_count > len(results):
        if not benchexec_xml:
            msg = "missing benchexec files"
        else:
            msg = "something is wrong"
        print(
            "We processed {} .log files but collected only {} results. Possible reason: {}".format(
                log_file_count, len(results), msg
            )
        )
    else:
        len({k: v for k, v in runs.items() if v.is_timeout()})
        print(
            "Overview of {} results from {} .log files ({} {}, {} {})".format(
                len(results),
                log_file_count,
                len({k: v for k, v in runs.items() if v.is_timeout()}),
                str_benchexec_timeout,
                len({k: v for k, v in runs.items() if v.is_oom()}),
                str_benchexec_oom,
            )
        )
    print_results(results)


if __name__ == "__main__":
    if sys.platform == "linux" or sys.platform == "linux2":
        signal.signal(signal.SIGINT, signal_handler)
        signal.signal(signal.SIGTERM, signal_handler)
        # just ignore pipe exceptions
        signal.signal(signal.SIGPIPE, signal.SIG_DFL)
    main()
