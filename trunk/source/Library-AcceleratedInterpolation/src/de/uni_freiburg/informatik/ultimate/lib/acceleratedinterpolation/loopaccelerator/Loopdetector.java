/*
 * Copyright (C) 2020 Jonas Werner (wernerj@informatik.uni-freiburg.de)
 * Copyright (C) 2020 University of Freiburg
 *
 * This file is part of the ULTIMATE accelerated interpolation library .
 *
 * The ULTIMATE framework is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE framework is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE accelerated interpolation library . If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE PDR library , or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE accelerated interpolation library grant you additional permission
 * to convey the resulting work.
 */

package de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation.loopaccelerator;

import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.lib.acceleratedinterpolation.AcceleratedInterpolation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;

/**
 *
 * @author Jonas Werner (wernerj@informatik.uni-freiburg.de) This class represents the loop detector needed for
 *         {@link AcceleratedInterpolation}
 */
public class Loopdetector<LETTER extends IIcfgTransition<?>> {

	private List<LETTER> mTrace;
	private final List<IcfgLocation> mTraceLocations;
	private final ILogger mLogger;

	public Loopdetector(final List<LETTER> trace, final ILogger logger) {
		mLogger = logger;
		mTrace = trace;
		mTraceLocations = LoopdetectorUtils.statementsToLocations(mTrace);

		mLogger.debug("Loopdetector created.");
	}

	public void getLoops() {
		final Map<IcfgLocation, List<Integer>> possibleLoopHeads =
				LoopdetectorUtils.getPossibleCyclesInTrace(mTraceLocations);
	}

	public void setTrace(final List<LETTER> trace) {
		mTrace = trace;
	}

	public List<LETTER> getTrace() {
		return mTrace;
	}

	public List<IcfgLocation> getTraceLocations() {
		return mTraceLocations;
	}
}
