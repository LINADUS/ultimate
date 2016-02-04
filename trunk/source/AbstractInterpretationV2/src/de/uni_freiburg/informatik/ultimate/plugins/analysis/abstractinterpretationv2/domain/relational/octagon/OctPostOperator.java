package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.relational.octagon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.ojalgo.optimisation.integer.NewIntegerSolver;

import de.uni_freiburg.informatik.ultimate.boogie.symboltable.BoogieSymbolTable;
import de.uni_freiburg.informatik.ultimate.model.IType;
import de.uni_freiburg.informatik.ultimate.model.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.CallStatement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Declaration;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Procedure;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VarList;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.model.location.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.rcfg.RcfgStatementExtractor;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractPostOperator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.util.BoogieAstUtil;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.util.TypeUtil;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Call;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;

public class OctPostOperator implements IAbstractPostOperator<OctagonDomainState, CodeBlock, IBoogieVar> {

	private final Logger mLogger;
	private final RcfgStatementExtractor mStatementExtractor;
	private final OctStatementProcessor mStatementProcessor;
	private final BoogieSymbolTable mSymbolTable;
	private final int mStatesUntilMerge;
	
	public OctPostOperator(Logger logger, BoogieSymbolTable symbolTable, int statesUntilMerge) {
		mLogger = logger;
		mStatementExtractor = new RcfgStatementExtractor();
		mStatementProcessor = new OctStatementProcessor(mLogger, symbolTable);
		mSymbolTable = symbolTable;
		mStatesUntilMerge = statesUntilMerge;
	}

	@Override
	public List<OctagonDomainState> apply(OctagonDomainState oldState, CodeBlock codeBlock) {
		List<OctagonDomainState> currentState = Collections.singletonList(oldState.deepCopy());
		for (Statement statement : mStatementExtractor.process(codeBlock)) {
			currentState = mStatementProcessor.processStatement(statement, currentState);
			assert currentState != null && currentState.stream().allMatch(s -> s != null): "NULLY";
			assert currentState.size() > 0 : "EMPTYLY";
		}
		return currentState;
	}
	
	private OctagonDomainState join(List<OctagonDomainState> states) {
		assert states.size() > 0 : "nothing to join";
		OctagonDomainState joinedState = null;
		for (OctagonDomainState result : states) {
			if (joinedState == null) {
				joinedState = result;
			} else {
				joinedState = joinedState.join(result);
			}
		}
		return joinedState;
	}

	@Override
	public List<OctagonDomainState> apply(
			OctagonDomainState stateBeforeTransition, OctagonDomainState stateAfterTransition, CodeBlock transition) {

		List<OctagonDomainState> result;
		if (transition instanceof Call) {
			result = applyCall(stateBeforeTransition, stateAfterTransition, (Call) transition);
		} else if (transition instanceof Return) {
			result = Collections.singletonList(
					applyReturn(stateBeforeTransition, stateAfterTransition, (Return) transition));
		} else {
			throw new UnsupportedOperationException("Unsupported transition: " + transition);
		}
		return result;
	}

	private List<OctagonDomainState> applyCall(
			OctagonDomainState stateBeforeCall, OctagonDomainState stateAfterCall, Call callTransition) {

		CallStatement call = callTransition.getCallStatement();
		Procedure procedure = calledProcedure(call);

		Map<String, IBoogieVar> tmpVars = new HashMap<>();
		Map<String, String> mapTmpVarToInParam = new HashMap<>();
		List<AssignmentStatement> tmpAssigns = new ArrayList<>();
		int paramNumber = 0;
		for (VarList inParamList : procedure.getInParams()) {
			IType type = inParamList.getType().getBoogieType();
			if (!TypeUtil.isBoolean(type) && !TypeUtil.isNumeric(type)) {
				paramNumber += inParamList.getIdentifiers().length;
				continue;
				// results in "var := \top" for these variables, which is always assumed for unsupported types
			}
			for (String inParam : inParamList.getIdentifiers()) {
				String tmpVar = "octTmp(" + inParam + ")";
				IBoogieVar tmpBoogieVar = BoogieAstUtil.createTemporaryIBoogieVar(tmpVar, type);
				Expression arg = call.getArguments()[paramNumber];
				++paramNumber;

				tmpVars.put(tmpVar, tmpBoogieVar);
				mapTmpVarToInParam.put(tmpVar, inParam);
				
				ILocation tmpAssignLoc = call.getLocation();
				LeftHandSide[] tmpAssignLhs = {new VariableLHS(tmpAssignLoc, type, tmpVar, null)};
				Expression[] tmpAssignRhs = {arg};
				tmpAssigns.add(new AssignmentStatement(tmpAssignLoc, tmpAssignLhs, tmpAssignRhs));
			}
		}
		// add temporary variables
		List<OctagonDomainState> tmpStates = Collections.singletonList(stateBeforeCall.addVariables(tmpVars));
		
		// assign tmp := args
		for (AssignmentStatement assign : tmpAssigns) {
			// TODO do not build new Statements just to disassemble them later on ==> use method directly
			tmpStates = mStatementProcessor.processStatement(assign, tmpStates);
		}
		
		// copy to scope opened by call (inParam := tmp)
		List<OctagonDomainState> result = new ArrayList<>(tmpStates.size());
		tmpStates.forEach(s -> result.add(stateAfterCall.copyValuesOnScopeChange(s, mapTmpVarToInParam)));
		return result;
		// No need to remove the temporary variables.
		// The state with temporary variables is only a local variable of this method.
	}
	
	private OctagonDomainState applyReturn(
			OctagonDomainState stateBeforeReturn, OctagonDomainState stateAfterReturn, Return returnTransition) {

		CallStatement call = returnTransition.getCallStatement();
		Procedure procedure = calledProcedure(call);
		Map<String, String> mapOutToLhs = generateMapOutToLhs(call.getLhs(), procedure);
		return stateAfterReturn.copyValuesOnScopeChange(stateBeforeReturn, mapOutToLhs);
	}
	
	private Procedure calledProcedure(CallStatement call) {
		List<Declaration> procedureDeclarations = mSymbolTable.getFunctionOrProcedureDeclaration(call.getMethodName());
		Procedure implementation = null;
		for (Declaration d : procedureDeclarations) {
			assert d instanceof Procedure : "call/return of non-procedure " + call.getMethodName() + ": " + d;
			Procedure p = (Procedure) d;
			if (p.getBody() != null) {
				if (implementation != null) {
					throw new UnsupportedOperationException("Multiple implementations of " + call.getMethodName());
				}
				implementation = p;
			}
		}
		if (implementation == null) {
			throw new UnsupportedOperationException("Missing implementation of " + call.getMethodName());
		}
		return implementation;
	}
	
	private Map<String, String> generateMapOutToLhs(VariableLHS[] lhs, Procedure calledProcedure) {
			Map<String, String> mapOutToLhs = new HashMap<>();
		// out-parameters to lhs of call assignment
		int i = 0;
		for (VarList outParamList : calledProcedure.getOutParams()) {
			for (String outParam : outParamList.getIdentifiers()) {
				assert i < lhs.length : "missing left hand side for out-parameter";
				mapOutToLhs.put(outParam, lhs[i].getIdentifier());
				++i;
			}
		}
		assert i == lhs.length : "missing out-parameter for left hand side";
		return mapOutToLhs;
	}
	
}
