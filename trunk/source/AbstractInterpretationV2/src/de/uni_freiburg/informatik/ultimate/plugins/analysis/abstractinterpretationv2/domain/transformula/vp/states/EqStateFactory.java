package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.states;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.IIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVarOrConst;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.IEqNodeIdentifier;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.EqFunction;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.EqNode;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.EqNodeAndFunctionFactory;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.IEqFunctionIdentifier;

public class EqStateFactory<ACTION extends IIcfgTransition<IcfgLocation>> {

	private final EqNodeAndFunctionFactory mEqNodeAndFunctionFactory;
	private final EqConstraintFactory<ACTION, EqNode, EqFunction> mEqConstraintFactory;
	private final IIcfgSymbolTable mSymbolTable;
	private EqState<ACTION> mTopStateWithEmptyPvocs;
	
	public EqStateFactory(EqNodeAndFunctionFactory eqNodeAndFunctionFactory, 
			EqConstraintFactory<ACTION, EqNode, EqFunction> eqConstraintFactory,
			IIcfgSymbolTable symbolTable) {
		mEqNodeAndFunctionFactory = eqNodeAndFunctionFactory;
		mEqConstraintFactory = eqConstraintFactory;
		mSymbolTable = symbolTable;
	}

	public EqState<ACTION> disjoinAll(Set<EqState<ACTION>> statesForCurrentEc) {
		final EqDisjunctiveConstraint<ACTION, EqNode, EqFunction> disjunctiveConstraint = 
				mEqConstraintFactory.getDisjunctiveConstraint(
						statesForCurrentEc.stream()
								.map(state -> state.getConstraint())
								.collect(Collectors.toSet()));
		final EqConstraint<ACTION, EqNode, EqFunction> flattenedConstraint = disjunctiveConstraint.flatten();
		return getEqState(flattenedConstraint, flattenedConstraint.getPvocs(mSymbolTable));
	}

	public EqState<ACTION> getTopState() {
		if (mTopStateWithEmptyPvocs == null) {
			mTopStateWithEmptyPvocs = getEqState(mEqConstraintFactory.getEmptyConstraint(), Collections.emptySet());
		}
		return mTopStateWithEmptyPvocs;
	}

	public EqNodeAndFunctionFactory getEqNodeAndFunctionFactory() {
		return mEqNodeAndFunctionFactory;
	}

//	public EqState<ACTION> getEqState(EqDisjunctiveConstraint<ACTION, EqNode, EqFunction> projectedConstraint,
	public <NODE extends IEqNodeIdentifier<NODE, FUNCTION>, FUNCTION extends IEqFunctionIdentifier<NODE, FUNCTION>> 
		EqState<ACTION> getEqState(EqConstraint<ACTION, NODE, FUNCTION> constraint,
				Set<IProgramVarOrConst> variables) {
		// TODO something smarter
		return new EqState<>((EqConstraint<ACTION, EqNode, EqFunction>) constraint, 
				mEqNodeAndFunctionFactory, this, variables);
	}

	public EqConstraintFactory<ACTION, EqNode, EqFunction> getEqConstraintFactory() {
		return mEqConstraintFactory;
	}

	public IIcfgSymbolTable getSymbolTable() {
		return mSymbolTable;
	}
	
}
