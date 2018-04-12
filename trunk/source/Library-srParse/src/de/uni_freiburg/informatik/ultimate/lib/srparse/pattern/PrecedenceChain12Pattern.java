package de.uni_freiburg.informatik.ultimate.lib.srparse.pattern;

import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lib.pea.CDD;
import de.uni_freiburg.informatik.ultimate.lib.pea.PhaseEventAutomata;
import de.uni_freiburg.informatik.ultimate.lib.pea.reqcheck.PatternToPEA;
import de.uni_freiburg.informatik.ultimate.lib.srparse.SrParseScope;

public class PrecedenceChain12Pattern extends PatternType {

	public PrecedenceChain12Pattern(final SrParseScope scope, final String id, final List<CDD> cdds,
			final List<String> durations) {
		super(scope, id, cdds, durations);
	}

	@Override
	public PhaseEventAutomata transform(final PatternToPEA peaTrans, final Map<String, Integer> id2bounds) {
		final CDD p_cdd = getCdds().get(2);
		final CDD q_cdd = getScope().getCdd1();
		final CDD r_cdd = getScope().getCdd2();
		final CDD s_cdd = getCdds().get(1);
		final CDD t_cdd = getCdds().get(0);

		return peaTrans.precedenceChainPattern12(getId(), p_cdd, q_cdd, r_cdd, s_cdd, t_cdd, getScope().toString());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		if (getId() != null) {
			sb.append(getId());
			sb.append(": ");
		}
		if (getScope() != null) {
			sb.append(getScope());
		}
		sb.append("it is always the case that if \"");
		sb.append(getCdds().get(2).toBoogieString());
		sb.append("\" holds and is succeeded by \"");
		sb.append(getCdds().get(1).toBoogieString());
		sb.append("\", then \"");
		sb.append(getCdds().get(0).toBoogieString());
		sb.append("\" previously held");
		return sb.toString();
	}

	@Override
	public PatternType rename(final String newName) {
		return new PrecedenceChain12Pattern(getScope(), newName, getCdds(), getDuration());
	}
}