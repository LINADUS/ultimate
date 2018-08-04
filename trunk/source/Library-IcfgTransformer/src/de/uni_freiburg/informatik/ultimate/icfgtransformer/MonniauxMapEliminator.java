/*
 * Copyright (C) 2018 Luca Bruder (luca.bruder@gmx.de)
 * Copyright (C) 2018 Lisa Kleinlein (lisa.kleinlein@web.de)
 *
 * This file is part of the ULTIMATE Library-ModelCheckerUtils library.
 *
 * The ULTIMATE Library-ModelCheckerUtils library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * The ULTIMATE Library-ModelCheckerUtils library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-ModelCheckerUtils library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-ModelCheckerUtils library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-ModelCheckerUtils library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.icfgtransformer;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.models.ModelUtils;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.loopacceleration.IdentityTransformer;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermTransformer;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.BasicIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IcfgUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgInternalTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgEdgeIterator;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;

/**
 * @author Luca Bruder (luca.bruder@gmx.de)
 * @author Lisa Kleinlein (lisa.kleinlein@web.de)
 */
public class MonniauxMapEliminator {

	private final ManagedScript mMgdScript;
	private final IIcfg<IcfgLocation> mIcfg;
	private final IIcfg<IcfgLocation> mResultIcfg;
	private final ILogger mLogger;
	private final IBacktranslationTracker mBacktranslationTracker;

	public MonniauxMapEliminator(final ILogger logger, final IIcfg<IcfgLocation> icfg,
			final IBacktranslationTracker backtranslationTracker) {
		mIcfg = Objects.requireNonNull(icfg);
		mMgdScript = mIcfg.getCfgSmtToolkit().getManagedScript();
		mLogger = logger;
		mResultIcfg = eliminateMaps();
		mBacktranslationTracker = backtranslationTracker;
	}

	private IIcfg<IcfgLocation> eliminateMaps() {

		final BasicIcfg<IcfgLocation> resultIcfg =
				new BasicIcfg<>(mIcfg.getIdentifier() + "ME", mIcfg.getCfgSmtToolkit(), IcfgLocation.class);
		final ILocationFactory<IcfgLocation, IcfgLocation> funLocFac = (oldLocation, debugIdentifier, procedure) -> {
			final IcfgLocation rtr = new IcfgLocation(debugIdentifier, procedure);
			ModelUtils.copyAnnotations(oldLocation, rtr);
			return rtr;
		};

		final TransformedIcfgBuilder<?, IcfgLocation> lst = new TransformedIcfgBuilder<>(mLogger, funLocFac,
				mBacktranslationTracker, new IdentityTransformer(mIcfg.getCfgSmtToolkit()), mIcfg, resultIcfg);
		iterate(lst);
		lst.finish();
		return resultIcfg;
	}

	private void iterate(final TransformedIcfgBuilder<?, IcfgLocation> lst) {
		final IcfgEdgeIterator iter = new IcfgEdgeIterator(mIcfg);
		final Script s = mMgdScript.getScript();
		while (iter.hasNext()) {
			final IIcfgTransition<?> transition = iter.next();

			if (transition instanceof IIcfgInternalTransition) {
				final IIcfgInternalTransition<?> internalTransition = (IIcfgInternalTransition<?>) transition;
				final UnmodifiableTransFormula tf = internalTransition.getTransformula();

				// keep or modify tf

			} else {
				throw new UnsupportedOperationException("not yet implemented");
			}

			final TransFormula tf = IcfgUtils.getTransformula(transition);
			int step = 0;

			/*
			 * String expr = "ABCD"; String test = "(sfksanohoa (select x y))"; for (String expr1 : test.split(" ")) {
			 * expr = expr1; expr = expr.substring(1); break; }
			 */

			final Term tfTerm = tf.getFormula();

			String transformula = tf.toString();
			String expr = null;
			String x = null;
			String y = null;
			int index = 0;

			for (final String expr1 : transformula.split(" (select * *)")) {

				for (int i = expr1.length() - 1; i >= 0; i--) {
					index = expr1.length();
					final char c = expr1.charAt(i);
					if (c == '(') {
						expr = expr1.substring(i + 1);
						i = 0;
					}
				}

				for (int i = index + 1; i < transformula.length(); i++) {
					final char c = transformula.charAt(i);
					int index_left = 0;
					boolean left_found = false;
					boolean x_found = false;
					int index_right = 0;
					if (c == ' ') {
						index_left = i + 1;
						left_found = true;
					}
					if (c == ' ' && left_found) {
						index_right = i - 1;
						x = transformula.substring(index_left, index_right);
						x_found = true;
					}
					if (c == ')' && x_found) {
						y = transformula.substring(index_right + 1, i - 1);
						i = transformula.length() + 1;
					}

				}

				final String sub_transformula = "(and (=> (= y i_step) (= a_step_i x_i)) (expr a_step_i)";
				sub_transformula.replaceAll("y", y);
				sub_transformula.replaceAll("x", x);
				sub_transformula.replaceAll("expr", expr);
				sub_transformula.replaceAll("step", Integer.toString(step));

				// TermVariable t = new TermVariable("f_step", sort, null);

				final Map<IProgramVar, TermVariable> inV = tf.getInVars();
				inV.remove(x);
				transformula = transformula.replaceAll("(* (select x y))", sub_transformula);
				final Map<IProgramVar, TermVariable> outV = tf.getOutVars();
				outV.remove(x);
				// outV.merge(null, , null);
				step++;

			}

			/*
			 * for (true) { //todo }
			 *
			 * TransFormula(inVars, outVars, auxVars, nonTheoryConst) newTF;
			 */
		}
	}

	private UnmodifiableTransFormula buildTransitionFormula(final UnmodifiableTransFormula oldFormula,
			final Term newTfFormula, final Map<IProgramVar, TermVariable> inVars,
			final Map<IProgramVar, TermVariable> outVars, final Collection<TermVariable> auxVars) {
		final Set<IProgramConst> nonTheoryConsts = oldFormula.getNonTheoryConsts();
		final boolean emptyAuxVars = auxVars.isEmpty();
		final Collection<TermVariable> branchEncoders = oldFormula.getBranchEncoders();
		final boolean emptyBranchEncoders = branchEncoders.isEmpty();
		final boolean emptyNonTheoryConsts = nonTheoryConsts.isEmpty();
		final TransFormulaBuilder tfb = new TransFormulaBuilder(inVars, outVars, emptyNonTheoryConsts, nonTheoryConsts,
				emptyBranchEncoders, branchEncoders, emptyAuxVars);

		tfb.setFormula(newTfFormula);
		tfb.setInfeasibility(Infeasibility.NOT_DETERMINED);
		auxVars.stream().forEach(tfb::addAuxVar);
		return tfb.finishConstruction(mMgdScript);
	}

	private final class MyTransformer extends TermTransformer {
		@Override
		protected void convert(final Term term) {
			// setResult(term);

			if (term instanceof ApplicationTerm) {
				final ApplicationTerm aterm = (ApplicationTerm) term;
				if (aterm.getFunction().getName().equals("store")) {
					// its a store
				}
			}

			super.convert(term);
		}
	}

}