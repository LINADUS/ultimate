package de.uni_freiburg.informatik.ultimatetest.svcomp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.Activator;
import de.uni_freiburg.informatik.ultimate.core.coreplugin.preferences.CorePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceStore;
import de.uni_freiburg.informatik.ultimatetest.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimatetest.UltimateStarter;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimatetest.decider.SafetyCheckTestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.summary.ITestSummary;
import de.uni_freiburg.informatik.ultimatetest.util.Util;

public abstract class AbstractSVCOMP14TestSuite extends UltimateTestSuite {

	/**
	 * Provide a path to an XML file describing the toolchain that should be
	 * run. The toolchain should be able to handle .c and .i files.
	 * 
	 * @return A String representing a path to an XML file describing a
	 *         toolchain.
	 */
	protected abstract String getToolchainPath();

	/**
	 * Clients need to provide a deadline in ms for Ultimate. After this time
	 * Ultimate will notify running plugins that they should stop execution. If
	 * the plugin ignores this, nothing happens.
	 * 
	 * @return A deadline in ms.
	 */
	protected abstract long getDeadline();

	/**
	 * Clients need to provide the name of the categories the test should be run
	 * on. As different categories may use different settings, they should
	 * provide a map from category name to settings file path.
	 * 
	 * Valid category names for SVCOMP 2014 are: BitVectors Concurrency
	 * ControlFlowInteger DeviceDrivers64 DriverChallenges HeapManipulation
	 * Loops MemorySafety ProductLines Recursive Sequentialized Simple Stateful
	 * 
	 * @return A map from category name to settings file path.
	 */
	protected abstract Map<String, String> getCategoryToSettingsPathMap();

	/**
	 * Should we save the output of the test case for each run in a logfile?
	 * Useful if you run locally, not so nice for Maven build.
	 * 
	 * It will use the log4j pattern from UltimateCore settings and save to a
	 * file in the same directory as the input file with a name generated by
	 * Util.generateLogFilename(..)
	 * 
	 * @return true iff you want a logfile to be created for each run.
	 */
	protected abstract boolean getCreateLogfileForEachTestCase();

	/**
	 * Clients should provide a path to the root of their local svcomp
	 * repository here (the place were the .set files lie).
	 * 
	 * During Maven builds, this function will only be used if no svcompdir
	 * property is specified in the BA_MavenParentUltimate pom.xml.
	 * 
	 * @return An absolute path to the root of the local SVCOMP repository (the
	 *         place were the .set files lie).
	 */
	protected abstract String getSVCOMP14RootDirectory();

	@Override
	protected ITestSummary[] constructTestSummaries() {
		String svcompRootDir = Util.getFromMavenVariableSVCOMPRoot(getSVCOMP14RootDirectory());
		Collection<File> setFiles = getAllSetFiles(svcompRootDir);

		ITestSummary[] testSummaries = new ITestSummary[setFiles.size()];
		int offset = 0;
		for (File setFile : setFiles) {
			String categoryName = setFile.getName().replace(".set", "");
			testSummaries[offset] = new SVCOMP14TestSummary(categoryName, this.getClass());
			offset++;
		}
		return testSummaries;
	}

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		Collection<UltimateTestCase> rtr = new ArrayList<UltimateTestCase>();

		String svcompRootDir = Util.getFromMavenVariableSVCOMPRoot(getSVCOMP14RootDirectory());

		String toolchainPath = getToolchainPath();
		long deadline = getDeadline();
		String description = this.getClass().getSimpleName();

		Collection<File> inputFiles = getAllInputFiles(svcompRootDir);
		Collection<File> setFiles = getAllSetFiles(svcompRootDir);

		if (inputFiles == null || setFiles == null) {
			System.err
					.println("inputFiles or setFiles are null: did you specify the svcomp root directory correctly? Currently it is: "
							+ svcompRootDir);
			return rtr;
		}

		Map<String, String> categoryToSettings = getCategoryToSettingsPathMap();

		for (Entry<String, String> entry : categoryToSettings.entrySet()) {
			File setFile = findFile(entry.getKey() + ".set", setFiles);
			if (setFile == null) {
				continue;
			}
			try {
				rtr.addAll(createTestCasesForSet(inputFiles, setFile, svcompRootDir, entry.getValue(), toolchainPath,
						deadline, description));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rtr;
	}

	private File findFile(String filename, Collection<File> files) {
		for (File file : files) {
			if (file.getName().equals(filename)) {
				return file;
			}
		}
		return null;
	}

	private Collection<File> getAllInputFiles(String svcomproot) {
		File root = new File(svcomproot);
		ArrayList<File> singleFiles = new ArrayList<File>();
		singleFiles.addAll(Util.getFilesRegex(root, new String[] { ".*\\.c", ".*\\.i" }));
		return singleFiles;
	}

	private Collection<File> getAllSetFiles(String svcomproot) {
		File root = new File(svcomproot);
		File[] setFiles = root.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".set");
			}
		});
		if (setFiles == null) {
			return null;
		}
		return Arrays.asList(setFiles);
	}

	private Collection<UltimateTestCase> createTestCasesForSet(Collection<File> allFiles, File setFile,
			String svcomproot, String settingsPath, String toolchainPath, long deadline, String description)
			throws Exception {

		String categoryName = setFile.getName().replace(".set", "");

		Collection<File> currentFiles = getFilesForSetFile(allFiles, setFile);
		File toolchainFile = new File(toolchainPath);
		File settingsFile = new File(settingsPath);

		ArrayList<UltimateTestCase> rtr = new ArrayList<UltimateTestCase>();

		for (File singleFile : currentFiles) {
			boolean shouldbesafe = singleFile.getName().contains("true");
			if (!shouldbesafe) {
				if (!singleFile.getName().contains("false")) {
					throw new Exception("Should contain false");
				}
			}

			String name = categoryName + ": " + singleFile.getAbsolutePath();
			UltimateRunDefinition urd = new UltimateRunDefinition(singleFile, settingsFile, toolchainFile);
			UltimateStarter starter;
			if (!getCreateLogfileForEachTestCase()) {
				starter = new UltimateStarter(urd, deadline);
			} else {
				String logPattern = new UltimatePreferenceStore(Activator.s_PLUGIN_ID)
						.getString(CorePreferenceInitializer.LABEL_LOG4J_PATTERN);
				starter = new UltimateStarter(urd, deadline,
						new File(Util.generateLogFilename(singleFile, description)), logPattern);
			}

			UltimateTestCase testCase = new UltimateTestCase(name, new SafetyCheckTestResultDecider(urd, false),
					starter, urd, super.getSummaries(), null);
			rtr.add(testCase);

		}
		return rtr;
	}

	private Collection<File> getFilesForSetFile(Collection<File> allFiles, File setFile) {
		ArrayList<File> currentFiles = new ArrayList<File>();

		try {
			DataInputStream in = new DataInputStream(new FileInputStream(setFile));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String regex = ".*" + line.replace(".", "\\.").replace("*", ".*");
				currentFiles.addAll(Util.filter(allFiles, regex));
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currentFiles;
	}
}
