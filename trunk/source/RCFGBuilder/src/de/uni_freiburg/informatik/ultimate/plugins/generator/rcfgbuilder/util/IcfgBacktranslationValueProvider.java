/*
 * Copyright (C) 2018 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2018 University of Freiburg
 *
 * This file is part of the ULTIMATE RCFGBuilder plug-in.
 *
 * The ULTIMATE RCFGBuilder plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE RCFGBuilder plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE RCFGBuilder plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE RCFGBuilder plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE RCFGBuilder plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util;

import de.uni_freiburg.informatik.ultimate.core.model.translation.IBacktranslationValueProvider;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdge;

/**
 *
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public class IcfgBacktranslationValueProvider implements IBacktranslationValueProvider<IcfgEdge, Term> {

	@Override
	public int getStartLineNumberFromStep(final IcfgEdge step) {
		return -1;
	}

	@Override
	public int getEndLineNumberFromStep(final IcfgEdge step) {
		return -1;
	}

	@Override
	public String getOriginFileNameFromStep(final IcfgEdge step) {
		return null;
	}

	@Override
	public String getFileNameFromStep(final IcfgEdge step) {
		return null;
	}

	@Override
	public String getStringFromStep(final IcfgEdge step) {
		return step.toString();
	}

	@Override
	public String getStringFromTraceElement(final IcfgEdge traceelement) {
		return traceelement.toString();
	}

	@Override
	public String getStringFromExpression(final Term expression) {
		return expression.toStringDirect();
	}

}
