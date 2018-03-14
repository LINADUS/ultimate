package de.uni_freiburg.informatik.ultimate.lib.srparse.pattern;

import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lib.pea.CDD;
import de.uni_freiburg.informatik.ultimate.lib.pea.PhaseEventAutomata;
import de.uni_freiburg.informatik.ultimate.lib.pea.reqcheck.PatternToPEA;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScope;

public class BndReccurrencePattern extends PatternType {
	public BndReccurrencePattern(final SrParseScope scope, final String id, final List<CDD> cdds,
			final List<String> durations) {
		super(scope, id, cdds, durations);
	}

	@Override
	public PhaseEventAutomata transform(final PatternToPEA peaTrans, final Map<String, Integer> id2bounds) {
		final CDD p_cdd = getCdds().get(0);
		final CDD q_cdd = getScope().getCdd1();
		final CDD r_cdd = getScope().getCdd2();

		return peaTrans.periodicPattern(getId(), p_cdd, q_cdd, r_cdd, parseDuration(getDuration().get(0), id2bounds),
				getScope().toString());
	}

	@Override
	public String toString() {
		return "it is always the case that \"" + getCdds().get(0) + "\" holds at least every \"" + getDuration().get(0)
				+ "\" time units";
	}
}
