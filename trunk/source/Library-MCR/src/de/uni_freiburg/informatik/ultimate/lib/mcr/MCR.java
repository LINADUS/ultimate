package de.uni_freiburg.informatik.ultimate.lib.mcr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.VpAlphabet;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.Intersect;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.IEmptyStackStateFactory;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.StringFactory;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssignmentStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.AssumeStatement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.BoogieASTNode;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Expression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.GeneratedBoogieAstVisitor;
import de.uni_freiburg.informatik.ultimate.boogie.ast.IdentifierExpression;
import de.uni_freiburg.informatik.ultimate.boogie.ast.LeftHandSide;
import de.uni_freiburg.informatik.ultimate.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.boogie.ast.VariableLHS;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgEdge;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.interpolant.IInterpolatingTraceCheck;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.interpolant.InterpolantComputationStatus;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicateUnifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.tracecheck.ITraceCheckPreferences;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.tracecheck.TraceCheckReasonUnknown;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RCFGEdgeVisitor;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;

/**
 * @author Frank Schüssele (schuessf@informatik.uni-freiburg.de)
 */
public class MCR<LETTER extends IIcfgTransition<?>> implements IInterpolatingTraceCheck<LETTER> {
	private final ILogger mLogger;
	private final IPredicateUnifier mPredicateUnifier;
	private final IUltimateServiceProvider mServices;
	private final ManagedScript mManagedScript;
	private final IEmptyStackStateFactory<IPredicate> mEmptyStackStateFactory;

	private final HashRelation<LETTER, String> mReads2Variables;
	private final HashRelation<LETTER, String> mWrites2Variables;
	private final HashRelation<String, LETTER> mVariables2Writes;
	private final HashRelation<LETTER, LETTER> mMhbInverse;
	private final Map<LETTER, Map<String, LETTER>> mPreviousWrite;
	private final Map<LETTER, TermVariable> mActions2TermVars;

	private final ITraceCheckFactory<LETTER> mTraceCheckFactory;
	private final Collection<IInterpolatingTraceCheck<LETTER>> mTraceChecks;
	private final Map<LETTER, Integer> mActions2Indices;
	private final AutomataLibraryServices mAutomataServices;
	private final VpAlphabet<LETTER> mAlphabet;
	private final McrTraceCheckResult<LETTER> mResult;

	public MCR(final ILogger logger, final ITraceCheckPreferences prefs, final IPredicateUnifier predicateUnifier,
			final IEmptyStackStateFactory<IPredicate> emptyStackStateFactory, final List<LETTER> trace,
			final ITraceCheckFactory<LETTER> traceCheckFactory) throws AutomataLibraryException {
		mLogger = logger;
		mPredicateUnifier = predicateUnifier;
		mServices = prefs.getUltimateServices();
		mAutomataServices = new AutomataLibraryServices(mServices);
		// TODO: Seperate correctly
		mAlphabet = new VpAlphabet<>(new HashSet<>(trace));
		mManagedScript = prefs.getCfgSmtToolkit().getManagedScript();
		mEmptyStackStateFactory = emptyStackStateFactory;
		mTraceCheckFactory = traceCheckFactory;
		mReads2Variables = new HashRelation<>();
		mWrites2Variables = new HashRelation<>();
		mVariables2Writes = new HashRelation<>();
		mMhbInverse = new HashRelation<>();
		mPreviousWrite = new HashMap<>();
		mActions2TermVars = new HashMap<>();
		mTraceChecks = new HashSet<>();
		mActions2Indices = new HashMap<>();
		// Explore all the interleavings of trace
		mResult = exploreInterleavings(trace);
	}

	private McrTraceCheckResult<LETTER> exploreInterleavings(final List<LETTER> initialTrace)
			throws AutomataLibraryException {
		getReadsAndWrites(initialTrace);
		final LinkedList<List<LETTER>> queue = new LinkedList<>();
		final Set<List<LETTER>> visited = new HashSet<>();
		queue.add(initialTrace);
		final NestedWordAutomaton<LETTER, IPredicate> automaton =
				new NestedWordAutomaton<>(mAutomataServices, mAlphabet, mEmptyStackStateFactory);
		while (!queue.isEmpty()) {
			final List<LETTER> trace = queue.remove();
			if (visited.contains(trace)) {
				continue;
			}
			visited.add(trace);
			preprocess(trace);
			IPredicate[] interpolants = getInterpolantsIfAccepted(automaton, trace);
			if (interpolants == null) {
				final IInterpolatingTraceCheck<LETTER> traceCheck = mTraceCheckFactory.getTraceCheck(trace);
				mTraceChecks.add(traceCheck);
				final LBool isCorrect = traceCheck.isCorrect();
				interpolants = traceCheck.getInterpolants();
				if (isCorrect != LBool.UNSAT) {
					// We found a feasible error trace
					return new McrTraceCheckResult<>(trace, isCorrect, automaton, interpolants);
				}
				final INestedWordAutomaton<LETTER, ?> mcrAutomaton = getMcrAutomaton(trace, interpolants);
				// TOOD: Get the interpolants from mcrAutomaton (DAG-interpolation?) and add them to automaton
			}
			queue.addAll(generateSeedInterleavings(trace, interpolants));
		}
		// All traces are infeasible
		// TODO: Are these interpolants safe to return?
		return new McrTraceCheckResult<>(initialTrace, LBool.UNSAT, automaton, new IPredicate[0]);
	}

	private IPredicate[] getInterpolantsIfAccepted(final NestedWordAutomaton<LETTER, IPredicate> automaton,
			final List<LETTER> trace) {
		// TODO: Get an accepting run if possible and return its state sequence, otherwise just return null
		return null;
	}

	private void getReadsAndWrites(final List<LETTER> trace) {
		for (final LETTER action : trace) {
			final ReadWriteFinder finder = new ReadWriteFinder(action);
			mReads2Variables.addAllPairs(action, finder.getReadVars());
			for (final String var : finder.getWrittenVars()) {
				mVariables2Writes.addPair(var, action);
				mWrites2Variables.addPair(action, var);
			}
		}
	}

	private void preprocess(final List<LETTER> trace) {
		mMhbInverse.clear();
		mPreviousWrite.clear();
		mActions2TermVars.clear();
		mActions2Indices.clear();
		// TODO: How to construct the MHB relation?
		final Map<String, LETTER> lastWrittenBy = new HashMap<>();
		for (final LETTER action : trace) {
			final Set<String> reads = mReads2Variables.getImage(action);
			if (!reads.isEmpty()) {
				final Map<String, LETTER> previousWrites = new HashMap<>();
				for (final String read : reads) {
					previousWrites.put(read, lastWrittenBy.get(read));
				}
				mPreviousWrite.put(action, previousWrites);
			}
			for (final String written : mWrites2Variables.getImage(action)) {
				lastWrittenBy.put(written, action);
			}
		}
		final Sort intSort = SmtSortUtils.getIntSort(mManagedScript);
		final Script script = mManagedScript.getScript();
		for (int i = 0; i < trace.size(); i++) {
			// TODO: There might be duplicate actions, is this a problem?
			// TODO: Can this varName collide?
			final String varName = "order_" + i;
			final LETTER action = trace.get(i);
			script.declareFun(varName, new Sort[0], intSort);
			mActions2TermVars.put(action, script.variable(varName, intSort));
			mActions2Indices.put(action, i);
		}
	}

	// TODO: Avoid duplicates by caching?
	private Collection<List<LETTER>> generateSeedInterleavings(final List<LETTER> trace,
			final IPredicate[] interpolants) {
		final Script script = mManagedScript.getScript();
		final List<List<LETTER>> result = new ArrayList<>();
		for (final LETTER read : trace) {
			final Set<String> readVars = mReads2Variables.getImage(read);
			if (readVars.isEmpty()) {
				continue;
			}
			final Term preRead = getPrecondition(read, trace, interpolants);
			for (final String var : readVars) {
				// TODO: Take write=null in account as well (no write before)
				for (final LETTER write : mVariables2Writes.getImage(var)) {
					if (writeDoesNotImply(write, preRead, trace, interpolants) == LBool.UNSAT) {
						continue;
					}
					script.push(1);
					// If the constraints are satisfiable, add a trace based on them
					script.assertTerm(getConstraints(trace, read, write, interpolants));
					if (script.checkSat() == LBool.SAT) {
						result.add(constructTraceFromModel(script.getModel()));
					}
					script.pop(1);
				}
			}
		}
		return result;
	}

	/**
	 * Encode a new trace in a formula, that is equivalent to trace, except that write happens right before read.
	 */
	// TODO: All the following methods with formulae can be extracted and this can be the only public method
	private Term getConstraints(final List<LETTER> trace, final LETTER read, final LETTER write,
			final IPredicate[] interpolants) {
		final Script script = mManagedScript.getScript();
		final List<Term> conjuncts = new ArrayList<>();
		// Add the MHB-constraints
		// TODO: We might want to have this as an option
		for (final Entry<LETTER, LETTER> entry : mMhbInverse.entrySet()) {
			final TermVariable var1 = mActions2TermVars.get(entry.getValue());
			final TermVariable var2 = mActions2TermVars.get(entry.getKey());
			conjuncts.add(SmtUtils.less(script, var1, var2));
		}
		conjuncts.addAll(getRwConstraints(read, trace, interpolants));
		if (write != null) {
			conjuncts.addAll(getRwConstraints(write, trace, interpolants));
		}
		// TODO: Handle this, if write=null
		conjuncts.addAll(getValueConstraints(read, getPostcondition(write, trace, interpolants), trace, interpolants));
		return SmtUtils.and(script, conjuncts);
	}

	private Collection<Term> getRwConstraints(final LETTER action, final List<LETTER> trace,
			final IPredicate[] interpolants) {
		final List<Term> result = new ArrayList<>();
		for (final LETTER prevRead : mMhbInverse.getImage(action)) {
			final Term preRead = getPrecondition(prevRead, trace, interpolants);
			result.addAll(getValueConstraints(prevRead, preRead, trace, interpolants));
		}
		return result;
	}

	private Collection<Term> getValueConstraints(final LETTER read, final Term constraint, final List<LETTER> trace,
			final IPredicate[] interpolants) {
		final Script script = mManagedScript.getScript();
		final List<Term> result = new ArrayList<>();
		final TermVariable readVar = mActions2TermVars.get(read);
		for (final String var : mReads2Variables.getImage(read)) {
			final List<Term> disjuncts = new ArrayList<>();
			for (final LETTER write : mVariables2Writes.getImage(var)) {
				if (writeDoesNotImply(write, constraint, trace, interpolants) != LBool.UNSAT) {
					continue;
				}
				final List<Term> conjuncts = new ArrayList<>();
				final TermVariable writeVar = mActions2TermVars.get(write);
				conjuncts.add(SmtUtils.less(script, writeVar, readVar));
				for (final LETTER otherWrite : mVariables2Writes.getImage(var)) {
					// TODO: Can we cache writeDoesNotImply?
					// TODO: Is this the correct condition?
					if (write.equals(otherWrite)
							|| writeDoesNotImply(otherWrite, constraint, trace, interpolants) == LBool.UNSAT) {
						continue;
					}
					final TermVariable otherWriteVar = mActions2TermVars.get(otherWrite);
					final Term beforeWrite = SmtUtils.less(script, otherWriteVar, writeVar);
					final Term afterRead = SmtUtils.less(script, readVar, otherWriteVar);
					conjuncts.add(SmtUtils.or(script, beforeWrite, afterRead));
				}
				disjuncts.add(SmtUtils.and(script, conjuncts));
			}
			result.add(SmtUtils.or(mManagedScript.getScript(), disjuncts));
		}
		return result;
	}

	private LBool writeDoesNotImply(final LETTER write, final Term constraint, final List<LETTER> trace,
			final IPredicate[] interpolants) {
		// TODO: Add necessary program variables first?
		final Script script = mManagedScript.getScript();
		final Term post = getPostcondition(write, trace, interpolants);
		return Util.checkSat(script, SmtUtils.and(script, post, SmtUtils.not(script, constraint)));
	}

	private List<LETTER> constructTraceFromModel(final Model model) {
		return mActions2TermVars.keySet().stream()
				.sorted((a1, a2) -> getIntValue(model, a1).compareTo(getIntValue(model, a2)))
				.collect(Collectors.toList());
	}

	// TODO: Is this the correct way?
	private BigInteger getIntValue(final Model model, final LETTER action) {
		final Term term = model.evaluate(mActions2TermVars.get(action));
		assert term instanceof ConstantTerm;
		final Object value = ((ConstantTerm) term).getValue();
		if (value instanceof BigInteger) {
			return (BigInteger) value;
		}
		assert value instanceof Rational;
		final Rational rational = (Rational) value;
		assert rational.denominator().equals(BigInteger.ONE);
		return rational.numerator();
	}

	private INestedWordAutomaton<LETTER, ?> getMcrAutomaton(final List<LETTER> trace, final IPredicate[] interpolants)
			throws AutomataLibraryException {
		INestedWordAutomaton<LETTER, String> result = null;
		final StringFactory factory = new StringFactory();
		for (final LETTER read : trace) {
			final Map<String, LETTER> previousWrites = mPreviousWrite.get(read);
			if (previousWrites == null) {
				continue;
			}
			for (final Entry<String, LETTER> entry : previousWrites.entrySet()) {
				// TODO: write can be null, handle this
				final LETTER write = entry.getValue();
				// TODO: Can we cache these post conditions?
				final Term post = getPostcondition(write, trace, interpolants);
				final NestedWordAutomaton<LETTER, String> readNwa =
						new NestedWordAutomaton<>(mAutomataServices, mAlphabet, factory);
				// TODO: These predicates are not sound (but should be also not used)
				final String initial = "q0";
				final String postWrite = "q1";
				final String postRead = "q2";
				readNwa.addState(true, false, initial);
				readNwa.addState(false, false, postWrite);
				readNwa.addState(false, true, postRead);
				final Set<LETTER> correctWrites = new HashSet<>();
				final Set<LETTER> incorrectWrites = new HashSet<>();
				final Set<LETTER> otherActions = new HashSet<>(mActions2TermVars.keySet());
				for (final LETTER otherWrite : mVariables2Writes.getImage(entry.getKey())) {
					if (Objects.equals(write, otherWrite)
							|| writeDoesNotImply(otherWrite, post, trace, interpolants) == LBool.UNSAT) {
						correctWrites.add(otherWrite);
					} else {
						incorrectWrites.add(otherWrite);
					}
					otherActions.remove(otherWrite);
				}
				// TODO: Restrict transitions to the number of actions in trace
				addTransitions(readNwa, initial, otherActions, initial);
				addTransitions(readNwa, initial, incorrectWrites, initial);
				addTransitions(readNwa, initial, correctWrites, postWrite);
				addTransitions(readNwa, postWrite, otherActions, postWrite);
				addTransitions(readNwa, postWrite, correctWrites, postWrite);
				readNwa.addInternalTransition(postWrite, read, postRead);
				addTransitions(readNwa, postRead, mActions2TermVars.keySet(), postRead);
				if (result == null) {
					result = readNwa;
				} else {
					// TODO: Is there a more efficient intersection with multiple automata?
					// TODO: What to use as state factory?
					final Intersect<LETTER, String> intersect =
							new Intersect<>(mAutomataServices, factory, result, readNwa);
					result = intersect.getResult();
				}
			}
		}
		// TODO: Remove self-loops
		return result;
	}

	private <T> void addTransitions(final NestedWordAutomaton<LETTER, T> nwa, final T pred,
			final Collection<LETTER> actions, final T succ) {
		for (final LETTER action : actions) {
			nwa.addInternalTransition(pred, action, succ);
		}
	}

	private Term getPrecondition(final LETTER action, final List<LETTER> trace, final IPredicate[] interpolants) {
		// TODO: Use mPreviousWrite?
		final int index = mActions2Indices.getOrDefault(action, -1);
		if (index == -1) {
			throw new UnsupportedOperationException(action + " is not contained in the trace.");
		}
		if (index == 0) {
			return mManagedScript.getScript().term("true");
		}
		return interpolants[index - 1].getClosedFormula();
	}

	private Term getPostcondition(final LETTER action, final List<LETTER> trace, final IPredicate[] interpolants) {
		final int index = mActions2Indices.getOrDefault(action, -1);
		if (index == -1) {
			throw new UnsupportedOperationException(action + " is not contained in the trace.");
		}
		if (index == trace.size() - 1) {
			return mManagedScript.getScript().term("false");
		}
		return interpolants[index].getClosedFormula();
	}

	public NestedWordAutomaton<LETTER, IPredicate> getAutomaton() {
		return mResult.getAutomaton();
	}

	public Collection<IInterpolatingTraceCheck<LETTER>> getTraceChecks() {
		return mTraceChecks;
	}

	@Override
	public List<LETTER> getTrace() {
		return mResult.getTrace();
	}

	@Override
	public IPredicate[] getInterpolants() {
		return mResult.getInterpolants();
	}

	@Override
	public IPredicateUnifier getPredicateUnifier() {
		return mPredicateUnifier;
	}

	@Override
	public boolean isPerfectSequence() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InterpolantComputationStatus getInterpolantComputationStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LBool isCorrect() {
		return mResult.isCorrect();
	}

	@Override
	public IPredicate getPrecondition() {
		return mPredicateUnifier.getTruePredicate();
	}

	@Override
	public IPredicate getPostcondition() {
		return mPredicateUnifier.getFalsePredicate();
	}

	@Override
	public Map<Integer, IPredicate> getPendingContexts() {
		return Collections.emptyMap();
	}

	@Override
	public boolean providesRcfgProgramExecution() {
		return isCorrect() != LBool.SAT;
	}

	@Override
	public IProgramExecution<IIcfgTransition<IcfgLocation>, Term> getRcfgProgramExecution() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatisticsDataProvider getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ToolchainCanceledException getToolchainCanceledExpection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TraceCheckReasonUnknown getTraceCheckReasonUnknown() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean wasTracecheckFinishedNormally() {
		// TODO Auto-generated method stub
		return true;
	}

	private class ReadWriteFinder extends RCFGEdgeVisitor {
		private final Set<String> mReadVars;
		private final Set<String> mWrittenVars;

		@Override
		protected void visit(final StatementSequence c) {
			for (final Statement s : c.getStatements()) {
				if (s instanceof AssumeStatement) {
					// TODO: For reads visit ((AssumeStatement)s).getFormula()
					final Expression expression = ((AssumeStatement) s).getFormula();
					mReadVars.addAll(new VariableFinder(expression).getResult());
				}
				if (s instanceof AssignmentStatement) {
					final AssignmentStatement a = (AssignmentStatement) s;
					for (final LeftHandSide lhs : a.getLhs()) {
						mWrittenVars.addAll(new VariableFinder(lhs).getResult());
					}
					for (final Expression rhs : a.getRhs()) {
						mReadVars.addAll(new VariableFinder(rhs).getResult());
					}
				}
				// TODO: Other cases?
			}
			super.visit(c);
		}

		// TODO: How to handle the cast correctly?
		ReadWriteFinder(final LETTER action) {
			mReadVars = new HashSet<>();
			mWrittenVars = new HashSet<>();
			if (action instanceof IcfgEdge) {
				visit((IcfgEdge) action);
			}
		}

		Set<String> getReadVars() {
			return mReadVars;
		}

		Set<String> getWrittenVars() {
			return mWrittenVars;
		}
	}

	private class VariableFinder extends GeneratedBoogieAstVisitor {
		private final Collection<String> mResult;

		VariableFinder(final BoogieASTNode node) {
			mResult = new HashSet<>();
			node.accept(this);
		}

		Collection<String> getResult() {
			return mResult;
		}

		@Override
		public boolean visit(final IdentifierExpression node) {
			mResult.add(node.getIdentifier());
			return super.visit(node);
		}

		@Override
		public boolean visit(final VariableLHS node) {
			mResult.add(node.getIdentifier());
			return super.visit(node);
		}
	}

	public interface ITraceCheckFactory<LETTER extends IIcfgTransition<?>> {
		IInterpolatingTraceCheck<LETTER> getTraceCheck(final List<LETTER> trace);
	}
}
