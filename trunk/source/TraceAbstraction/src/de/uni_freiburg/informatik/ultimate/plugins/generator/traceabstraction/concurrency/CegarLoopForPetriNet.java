/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 *
 * This file is part of the ULTIMATE TraceAbstractionConcurrent plug-in.
 *
 * The ULTIMATE TraceAbstractionConcurrent plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TraceAbstractionConcurrent plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstractionConcurrent plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstractionConcurrent plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstractionConcurrent plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.concurrency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.AutomatonDefinitionPrinter.NamedAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.Word;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.AutomatonWithImplicitSelfloops;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INwaOutgoingLetterAndTransitionProvider;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.Analyze;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.Analyze.SymbolType;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.oldapi.ComplementDD;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.oldapi.DeterminizeDD;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.PetriNetNot1SafeException;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.netdatastructures.BoundedPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.netdatastructures.PetriNetUtils;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.Difference;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.Difference.LoopSyncMethod;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.DifferencePairwiseOnDemand;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.PetriNet2FiniteAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.RemoveDead;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.RemoveRedundantFlow;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding.BranchingProcess;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding.FinitePrefix2PetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding.PetriNetUnfolder;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding.PetriNetUnfolder.EventOrderEnum;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.RunningTaskInfo;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.TaskCanceledException;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.TaskCanceledException.UserDefinedLimit;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.core.lib.results.StatisticsResult;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.model.translation.IProgramExecution;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.debugidentifiers.DebugIdentifier;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula.Infeasibility;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.hoaretriple.IHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.hoaretriple.IncrementalHoareTripleChecker;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.PredicateFactory;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.taskidentifier.SubtaskIterationIdentifier;
import de.uni_freiburg.informatik.ultimate.lib.tracecheckerutils.singletracecheck.InterpolationTechnique;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.BasicCegarLoop;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CFG2NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.CegarLoopStatisticsDefinitions;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PetriCegarLoopStatisticsDefinitions;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.PetriCegarLoopStatisticsGenerator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionStarter;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionUtils;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.automataminimization.AutomataMinimizationStatisticsGenerator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.interpolantautomata.transitionappender.DeterministicInterpolantAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.petrinetlbe.PetriNetLargeBlockEncoding;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.InductivityCheck;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences.Artifact;
import de.uni_freiburg.informatik.ultimate.util.HistogramOfIterable;
import de.uni_freiburg.informatik.ultimate.util.datastructures.DataStructureUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.UnionFind;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Triple;
import de.uni_freiburg.informatik.ultimate.util.statistics.IStatisticsDataProvider;

public class CegarLoopForPetriNet<LETTER extends IIcfgTransition<?>> extends BasicCegarLoop<LETTER> {

	public enum SizeReduction { REMOVE_DEAD, REMOVE_REDUNDANT_FLOW };

	private static final boolean USE_ON_DEMAND_RESULT = false;

	private static final boolean DEBUG_WRITE_NET_HASH_CODES = false;

	/**
	 * Write result of RemoveUnreachable to file if runtime of this operation in
	 * seconds is greater than this number.
	 */
	private static final int DEBUG_DUMP_REMOVEUNREACHABLEINPUT_THRESHOLD = 24 * 60 * 60;

	/**
	 * Write result of RemoveUnreachable to file if runtime of this operation in
	 * seconds is greater than this number.
	 */
	private static final int DEBUG_DUMP_DRYRUNRESULT_THRESHOLD = 24 * 60 * 60;

	public int mCoRelationQueries = 0;
	public int mBiggestAbstractionTransitions;
	/**
	 * Do not enhance the interpolant automaton into a total automaton but construct the enhancement only on-demand and
	 * add only transitions that will be needed for the difference.
	 */
	private final boolean mEnhanceInterpolantAutomatonOnDemand = true;
	/**
	 * Remove unreachable nodes of mAbstraction in each iteration.
	 */
	private final boolean mRemoveDead = false;
	private final boolean mRemoveRedundantFlow = false;

	private PetriNetLargeBlockEncoding mLBE;

	private final PetriCegarLoopStatisticsGenerator mPetriClStatisticsGenerator;

	private Set<IPredicate> mProgramPointPlaces;

	public CegarLoopForPetriNet(final DebugIdentifier name, final IIcfg<?> rootNode, final CfgSmtToolkit csToolkit,
			final PredicateFactory predicateFactory, final TAPreferences taPrefs,
			final Collection<IcfgLocation> errorLocs, final IUltimateServiceProvider services) {
		super(name, rootNode, csToolkit, predicateFactory, taPrefs, errorLocs,
				InterpolationTechnique.Craig_TreeInterpolation, false, services);
		mPetriClStatisticsGenerator = new PetriCegarLoopStatisticsGenerator(mCegarLoopBenchmark);
	}

	@Override
	protected void getInitialAbstraction() throws AutomataLibraryException {
		final IcfgLocation initialNode = mIcfg.getProcedureEntryNodes().get(TraceAbstractionStarter.ULTIMATE_START);
		if (initialNode == null) {
			throw new UnsupportedOperationException("Program must have " + TraceAbstractionStarter.ULTIMATE_START
					+ " procedure (this is the procedure where all executions start)");
		}
		final BoundedPetriNet<LETTER, IPredicate> cfg = constructPetriNetWithoutDeadTransitions();
		if (DEBUG_WRITE_NET_HASH_CODES) {
			mLogger.debug(PetriNetUtils.printHashCodesOfInternalDataStructures(cfg));
		}
		if (mPref.useLbeInConcurrentAnalysis() != PetriNetLbe.OFF) {
			final long start_time = System.currentTimeMillis();
			mLBE = new PetriNetLargeBlockEncoding(mServices, mIcfg.getCfgSmtToolkit(),
					(BoundedPetriNet<IIcfgTransition<?>, IPredicate>) cfg, mPref.useLbeInConcurrentAnalysis());
			final BoundedPetriNet<LETTER, IPredicate> lbecfg = (BoundedPetriNet<LETTER, IPredicate>) mLBE.getResult();
			mAbstraction = lbecfg;
			final long end_time = System.currentTimeMillis();
			final long difference = end_time - start_time;
			mLogger.info("Time needed for LBE in milliseconds: " + difference);

			mServices.getResultService().reportResult(Activator.PLUGIN_ID, new StatisticsResult<>(Activator.PLUGIN_NAME,
					"PetriNetLargeBlockEncoding benchmarks", mLBE.getPetriNetLargeBlockEncodingStatistics()));
		} else {
			mAbstraction = cfg;
		}
		mProgramPointPlaces = ((BoundedPetriNet<LETTER, IPredicate>) mAbstraction).getPlaces();

		if (mIteration <= mPref.watchIteration()
				&& (mPref.artifact() == Artifact.ABSTRACTION || mPref.artifact() == Artifact.RCFG)) {
			mArtifactAutomaton = mAbstraction;
		}
	}

	private BoundedPetriNet<LETTER, IPredicate> constructPetriNetWithoutDeadTransitions()
			throws AutomataOperationCanceledException {
		final boolean addThreadUsageMonitors = true;
		final BoundedPetriNet<LETTER, IPredicate> cfg = CFG2NestedWordAutomaton.constructPetriNetWithSPredicates(
				mServices, mIcfg, mStateFactoryForRefinement, mErrorLocs, false, mPredicateFactory,
				addThreadUsageMonitors);
		try {
			final BoundedPetriNet<LETTER, IPredicate> vitalCfg = new RemoveDead<>(
					new AutomataLibraryServices(mServices), cfg, null, true).getResult();
			return vitalCfg;
		} catch (final AutomataOperationCanceledException aoce) {
			final String taskDescription = "removing dead transitions from Petri net that has " + cfg.sizeInformation();
			aoce.addRunningTaskInfo(new RunningTaskInfo(getClass(), taskDescription));
			throw aoce;
		} catch (final PetriNetNot1SafeException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected boolean isAbstractionEmpty() throws AutomataOperationCanceledException {
		final BoundedPetriNet<LETTER, IPredicate> abstraction = (BoundedPetriNet<LETTER, IPredicate>) mAbstraction;
		final boolean cutOffSameTrans = mPref.cutOffRequiresSameTransition();
		final EventOrderEnum eventOrder = mPref.eventOrder();

		mPetriClStatisticsGenerator.start(PetriCegarLoopStatisticsDefinitions.EmptinessCheckTime.toString());
		PetriNetUnfolder<LETTER, IPredicate> unf;
		try {
			unf = new PetriNetUnfolder<>(new AutomataLibraryServices(mServices), abstraction, eventOrder,
					cutOffSameTrans, true);
		} catch (final PetriNetNot1SafeException e) {
			throw new UnsupportedOperationException(e.getMessage());
		} finally {
			mPetriClStatisticsGenerator.stop(PetriCegarLoopStatisticsDefinitions.EmptinessCheckTime.toString());
		}
		final BranchingProcess<LETTER, IPredicate> finPrefix = unf.getFinitePrefix();
		mCoRelationQueries += (finPrefix.getCoRelation().getQueryCounterYes()
				+ finPrefix.getCoRelation().getQueryCounterNo());

		mCounterexample = unf.getAcceptingRun();
		if (mCounterexample == null) {
			return true;
		}
		if (mPref.dumpAutomata()) {
			mCegarLoopBenchmark.start(CegarLoopStatisticsDefinitions.DumpTime);
			mDumper.dumpNestedRun(mCounterexample);
			mCegarLoopBenchmark.stop(CegarLoopStatisticsDefinitions.DumpTime);
		}
		mLogger.info("Found error trace");

		if (mLogger.isDebugEnabled()) {
			mLogger.debug(mCounterexample.getWord());
		}
		final HistogramOfIterable<LETTER> traceHistogram = new HistogramOfIterable<>(mCounterexample.getWord());
		mCegarLoopBenchmark.reportTraceHistogramMaximum(traceHistogram.getMax());
		if (mLogger.isInfoEnabled()) {
			mLogger.info("trace histogram " + traceHistogram.toString());
		}

		if (mPref.hasLimitTraceHistogram() && (traceHistogram.getMax() > mPref.getLimitTraceHistogram())) {
			final String taskDescription =
					"bailout by trace histogram " + traceHistogram.toString() + " in iteration " + mIteration;
			throw new TaskCanceledException(UserDefinedLimit.TRACE_HISTOGRAM, getClass(), taskDescription);
		}
		return false;
	}

	@Override
	public IProgramExecution<IIcfgTransition<IcfgLocation>, Term> getRcfgProgramExecution() {
		if (mPref.useLbeInConcurrentAnalysis() != PetriNetLbe.OFF) {
			return mLBE.translateExecution(mRcfgProgramExecution);
		}
		return super.getRcfgProgramExecution();
	}

	@Override
	protected boolean refineAbstraction() throws AutomataLibraryException {
		final BoundedPetriNet<LETTER, IPredicate> abstraction = (BoundedPetriNet<LETTER, IPredicate>) mAbstraction;
		final IHoareTripleChecker htc;
		if (mRefinementEngine.getHoareTripleChecker() != null) {
			htc = mRefinementEngine.getHoareTripleChecker();
		} else {
			htc = TraceAbstractionUtils.constructEfficientHoareTripleCheckerWithCaching(mServices,
					mPref.getHoareTripleChecks(), mCsToolkit, mRefinementEngine.getPredicateUnifier());
		}
		mCegarLoopBenchmark.start(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		try {
			// Determinize the interpolant automaton
			final INestedWordAutomaton<LETTER, IPredicate> dia;
			final Pair<INestedWordAutomaton<LETTER, IPredicate>, DifferencePairwiseOnDemand<LETTER, IPredicate, ?>> enhancementResult = enhanceAnddeterminizeInterpolantAutomaton(
					mInterpolAutomaton, htc);
			dia = enhancementResult.getFirst();

			if (mPref.dumpAutomata()) {
				final String filename = new SubtaskIterationIdentifier(mTaskIdentifier, getIteration())
						+ "_AbstractionAfterDifferencePairwiseOnDemand";
				super.writeAutomatonToFile(enhancementResult.getSecond().getResult(), filename);
			}

			// Complement the interpolant automaton
			final INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate> nia = new ComplementDD<>(
					new AutomataLibraryServices(mServices), mPredicateFactoryInterpolantAutomata, dia).getResult();
			// TODO 2018-08-11 Matthias: Complement not needed since we compute difference.
			// Furthermore there is a problem because we would have to concatenate operand
			// with some ∑^* automaton first and we do not yet have an implementation for
			// that.
			// assert !accepts(mServices, nia, mCounterexample.getWord(), false) :
			// "Complementation broken!";
			// mLogger.info("Complemented interpolant automaton has " + nia.size() + "
			// states");

			if (mIteration <= mPref.watchIteration() && mPref.artifact() == Artifact.NEG_INTERPOLANT_AUTOMATON) {
				mArtifactAutomaton = nia;
			}
			if (USE_ON_DEMAND_RESULT) {
				mAbstraction = enhancementResult.getSecond().getResult();
			} else {
				final Difference<LETTER, IPredicate, ?> diff = new Difference<>(new AutomataLibraryServices(mServices),
						mPredicateFactoryInterpolantAutomata, abstraction, dia, LoopSyncMethod.HEURISTIC,
						enhancementResult.getSecond(), false);
				mLogger.info(diff.getAutomataOperationStatistics());
				mAbstraction = diff.getResult();
			}
		} finally {
			mCegarLoopBenchmark.addEdgeCheckerData(htc.getEdgeCheckerBenchmark());
			mCegarLoopBenchmark
					.addPredicateUnifierData(mRefinementEngine.getPredicateUnifier().getPredicateUnifierBenchmark());
			mCegarLoopBenchmark.stop(CegarLoopStatisticsDefinitions.AutomataDifference.toString());
		}

		if (mPref.dumpAutomata()) {
			final String filename =
					new SubtaskIterationIdentifier(mTaskIdentifier, getIteration()) + "_AbstractionAfterDifference";
			super.writeAutomatonToFile(mAbstraction, filename);
		}

		mLogger.info(mProgramPointPlaces.size() + " programPoint places, "
				+ (((BoundedPetriNet<LETTER, IPredicate>) mAbstraction).getPlaces().size() - mProgramPointPlaces.size())
				+ " predicate places.");

		if (mRemoveDead) {
			final Triple<BoundedPetriNet<LETTER, IPredicate>, AutomataMinimizationStatisticsGenerator, Long> minimizationResult = doSizeReduction(
					(BoundedPetriNet<LETTER, IPredicate>) mAbstraction, SizeReduction.REMOVE_DEAD);
			mCegarLoopBenchmark.addAutomataMinimizationData(minimizationResult.getSecond());
			if (mPref.dumpAutomata()
					|| minimizationResult.getThird() > DEBUG_DUMP_REMOVEUNREACHABLEINPUT_THRESHOLD * 1_000_000_000L) {
				final String filename = new SubtaskIterationIdentifier(mTaskIdentifier, getIteration())
						+ "_AbstractionBeforeRemoveDead";
				super.writeAutomatonToFile(mAbstraction, filename);
			}
			mAbstraction = minimizationResult.getFirst();
		}
		if (mRemoveRedundantFlow) {
			final Triple<BoundedPetriNet<LETTER, IPredicate>, AutomataMinimizationStatisticsGenerator, Long> minimizationResult = doSizeReduction(
					(BoundedPetriNet<LETTER, IPredicate>) mAbstraction, SizeReduction.REMOVE_REDUNDANT_FLOW);
			mCegarLoopBenchmark.addAutomataMinimizationData(minimizationResult.getSecond());
			if (mPref.dumpAutomata()
					|| minimizationResult.getThird() > DEBUG_DUMP_REMOVEUNREACHABLEINPUT_THRESHOLD * 1_000_000_000L) {
				final String filename = new SubtaskIterationIdentifier(mTaskIdentifier, getIteration())
						+ "_AbstractionBeforeRemoveRedundantFlow";
				super.writeAutomatonToFile(mAbstraction, filename);
			}
			mAbstraction = minimizationResult.getFirst();
		}

		if (mPref.unfoldingToNet()) {
			final int flowBefore = mAbstraction.size();
			mLogger.info(mProgramPointPlaces.size() + " programPoint places, "
					+ (((BoundedPetriNet<LETTER, IPredicate>) mAbstraction).getPlaces().size() - mProgramPointPlaces.size())
					+ " predicate places.");
			mPetriClStatisticsGenerator.start(PetriCegarLoopStatisticsDefinitions.BackfoldingUnfoldingTime.toString());
			PetriNetUnfolder<LETTER, IPredicate> unf;
			try {
				final boolean cutOffSameTrans = mPref.cutOffRequiresSameTransition();
				final EventOrderEnum eventOrder = mPref.eventOrder();
				unf = new PetriNetUnfolder<>(new AutomataLibraryServices(mServices),
						((BoundedPetriNet<LETTER, IPredicate>) mAbstraction), eventOrder, cutOffSameTrans, false);
			} catch (final PetriNetNot1SafeException e) {
				throw new UnsupportedOperationException(e.getMessage());
			} catch (final AutomataOperationCanceledException aoce) {
				throw aoce;
			} finally {
				mPetriClStatisticsGenerator
						.stop(PetriCegarLoopStatisticsDefinitions.BackfoldingUnfoldingTime.toString());
			}
			mPetriClStatisticsGenerator.start(PetriCegarLoopStatisticsDefinitions.BackfoldingTime.toString());
			final FinitePrefix2PetriNet<LETTER, IPredicate> fp2pn = new FinitePrefix2PetriNet<>(
					new AutomataLibraryServices(mServices), mStateFactoryForRefinement, unf.getFinitePrefix(), true);
			assert fp2pn.checkResult(mPredicateFactoryResultChecking) : fp2pn.getClass().getSimpleName() + " failed";
			mAbstraction = fp2pn.getResult();
			mProgramPointPlaces = fp2pn.getOldToNewPlaces().projectToRange(mProgramPointPlaces);
			final int flowAfterwards = mAbstraction.size();
			mPetriClStatisticsGenerator.reportFlowIncreaseByBackfolding(flowAfterwards - flowBefore);
			mPetriClStatisticsGenerator.stop(PetriCegarLoopStatisticsDefinitions.BackfoldingTime.toString());
			mLogger.info(mProgramPointPlaces.size() + " programPoint places, "
					+ (((BoundedPetriNet<LETTER, IPredicate>) mAbstraction).getPlaces().size() - mProgramPointPlaces.size())
					+ " predicate places.");
		}


		mCegarLoopBenchmark.reportAbstractionSize(mAbstraction.size(), mIteration);
		// if (mBiggestAbstractionSize < mAbstraction.size()){
		// mBiggestAbstractionSize = mAbstraction.size();
		// mBiggestAbstractionTransitions =
		// abstraction.getTransitions().size();
		// mBiggestAbstractionIteration = mIteration;
		// }

		assert !acceptsPetriViaFA(mServices, mAbstraction, mCounterexample.getWord()) : "Intersection broken!";

		// int[] statistic = mAbstraction.transitions();
		// s_Logger.debug("Abstraction has " +
		// mAbstraction.getAllStates().size() + "states, " +
		// statistic[0] + " internal transitions " + statistic[1] +
		// "call transitions " + statistic[2]+ " return transitions ");

		if (mIteration <= mPref.watchIteration()
				&& (mPref.artifact() == Artifact.ABSTRACTION || mPref.artifact() == Artifact.RCFG)) {
			mArtifactAutomaton = mAbstraction;
		}
		if (mPref.dumpAutomata()) {
			final String filename = "Abstraction" + mIteration;
			writeAutomatonToFile(mAbstraction, filename);
		}
		return true;
	}


	private Triple<BoundedPetriNet<LETTER, IPredicate>, AutomataMinimizationStatisticsGenerator, Long> doSizeReduction(
			final BoundedPetriNet<LETTER, IPredicate> input, final SizeReduction method)
			throws AutomataOperationCanceledException, PetriNetNot1SafeException, AssertionError {
		final long automataMinimizationTime;
		final long start = System.nanoTime();
		long statesRemovedByMinimization = 0;
		long transitionsRemovedByMinimization = 0;
		long flowRemovedByMinimization = 0;
		boolean nontrivialMinimizaton = false;
		mPetriClStatisticsGenerator.start(PetriCegarLoopStatisticsDefinitions.RemoveRedundantFlowTime.toString());
		final AutomataMinimizationStatisticsGenerator amsg;
		final BoundedPetriNet<LETTER, IPredicate> reducedNet;
		try {
			final int placesBefore = input.getPlaces().size();
			final int transitionsBefore = input.getTransitions().size();
			final int flowBefore = input.size();
			switch (method) {
			case REMOVE_DEAD:
				reducedNet = new de.uni_freiburg.informatik.ultimate.automata.petrinet.operations.RemoveDead<>(
						new AutomataLibraryServices(mServices), input, null, true).getResult();
				break;
			case REMOVE_REDUNDANT_FLOW:
				final Set<IPredicate> redundancyCandidates = input.getPlaces().stream()
						.filter(x -> !mProgramPointPlaces.contains(x)).collect(Collectors.toSet());
				reducedNet = new RemoveRedundantFlow<>(new AutomataLibraryServices(mServices), input, null, null,
						null).getResult();
				break;
			default:
				throw new AssertionError("unknown value " + method);
			}
			final int placesAfterwards = (reducedNet.getPlaces()).size();
			final int transitionsAfterwards = (reducedNet.getTransitions().size());
			final int flowAfterwards = reducedNet.size();
			statesRemovedByMinimization = placesBefore - placesAfterwards;
			transitionsRemovedByMinimization = transitionsBefore - transitionsAfterwards;
			flowRemovedByMinimization = flowBefore - flowAfterwards;
			// if (transitionsAfterwards != transitionsBefore) {
			// throw new AssertionError("removed transitions: " +
			// transitionsRemovedByMinimization + " Iteration "
			// + mIteration + " Abstraction has " + mAbstraction.sizeInformation());
			// }
			nontrivialMinimizaton = true;
		} finally {
			automataMinimizationTime = System.nanoTime() - start;
			amsg = new AutomataMinimizationStatisticsGenerator(automataMinimizationTime, true, nontrivialMinimizaton,
					flowRemovedByMinimization);
			mPetriClStatisticsGenerator.stop(PetriCegarLoopStatisticsDefinitions.RemoveRedundantFlowTime.toString());
		}
		final Triple<BoundedPetriNet<LETTER, IPredicate>, AutomataMinimizationStatisticsGenerator, Long> minimizationResult = new Triple<BoundedPetriNet<LETTER, IPredicate>, AutomataMinimizationStatisticsGenerator, Long>(
				reducedNet, amsg, automataMinimizationTime);
		return minimizationResult;
	}

	protected Pair<INestedWordAutomaton<LETTER, IPredicate>, DifferencePairwiseOnDemand<LETTER, IPredicate, ?>> enhanceAnddeterminizeInterpolantAutomaton(
			final INestedWordAutomaton<LETTER, IPredicate> interpolAutomaton, final IHoareTripleChecker htc)
			throws AutomataOperationCanceledException, PetriNetNot1SafeException {
		mLogger.debug("Start determinization");
		final INestedWordAutomaton<LETTER, IPredicate> dia;
		final DifferencePairwiseOnDemand<LETTER, IPredicate, ?> dpod;
		switch (mPref.interpolantAutomatonEnhancement()) {
		case NONE:
			final PowersetDeterminizer<LETTER, IPredicate> psd =
					new PowersetDeterminizer<>(interpolAutomaton, true, mPredicateFactoryInterpolantAutomata);
			final DeterminizeDD<LETTER, IPredicate> dabps = new DeterminizeDD<>(new AutomataLibraryServices(mServices),
					mPredicateFactoryInterpolantAutomata, interpolAutomaton, psd);
			dia = dabps.getResult();
			dpod = null;
			break;
		case PREDICATE_ABSTRACTION:
			final DeterministicInterpolantAutomaton<LETTER> raw = new DeterministicInterpolantAutomaton<>(mServices,
					mCsToolkit, htc, interpolAutomaton, mRefinementEngine.getPredicateUnifier(), false, false);
			if (mEnhanceInterpolantAutomatonOnDemand) {
				final Set<LETTER> universalSubtrahendLoopers =
						determineUniversalSubtrahendLoopers(mAbstraction.getAlphabet(), interpolAutomaton.getStates());
				mLogger.info("Number of universal loopers: " + universalSubtrahendLoopers.size() + " out of "
						+ mAbstraction.getAlphabet().size());
				final NestedWordAutomaton<LETTER, IPredicate> ia =
						(NestedWordAutomaton<LETTER, IPredicate>) interpolAutomaton;
				for (final IPredicate state : ia.getStates()) {
					for (final LETTER letter : universalSubtrahendLoopers) {
						ia.addInternalTransition(state, letter, state);
					}
				}
				final long start = System.nanoTime();
				try {
					dpod = new DifferencePairwiseOnDemand<>(
							new AutomataLibraryServices(mServices), mPredicateFactoryInterpolantAutomata,
							(IPetriNet<LETTER, IPredicate>) mAbstraction, raw, universalSubtrahendLoopers);
				} catch (final AutomataOperationCanceledException tce) {
					final String taskDescription = generateOnDemandEnhancementCanceledMessage(interpolAutomaton,
							universalSubtrahendLoopers, mAbstraction.getAlphabet(), mIteration);
					tce.addRunningTaskInfo(new RunningTaskInfo(getClass(), taskDescription));
					throw tce;
				} finally {
					raw.switchToReadonlyMode();
				}
				final AutomatonWithImplicitSelfloops<LETTER, IPredicate> awis = new AutomatonWithImplicitSelfloops<LETTER, IPredicate>(
						new AutomataLibraryServices(mServices), raw, universalSubtrahendLoopers);
				dia = new RemoveUnreachable<>(new AutomataLibraryServices(mServices), awis).getResult();
				final long end = System.nanoTime();
				if ((end - start) > DEBUG_DUMP_DRYRUNRESULT_THRESHOLD * 1_000_000_000L) {
					final String filename = new SubtaskIterationIdentifier(mTaskIdentifier, getIteration())
							+ "_DifferencePairwiseOnDemandInput";
					final String atsHeaderMessage = "inputs of difference operation in iteration " + mIteration;
					final String atsCode = "PetriNet diff = differencePairwiseOnDemand(net, nwa);";
					super.writeAutomataToFile(filename, atsHeaderMessage, atsCode,
							new NamedAutomaton<LETTER, IPredicate>("net", mAbstraction),
							new NamedAutomaton<LETTER, IPredicate>("nwa", dia));
				}
			} else {
				dpod = null;
				try {
					dia = new RemoveUnreachable<>(new AutomataLibraryServices(mServices), raw).getResult();
				} catch (final AutomataOperationCanceledException aoce) {
					final RunningTaskInfo rti = new RunningTaskInfo(getClass(),
							"enhancing interpolant automaton that has " + interpolAutomaton.getStates().size()
									+ " states and an alphabet of " + mAbstraction.getAlphabet().size() + " letters");
					throw new ToolchainCanceledException(aoce, rti);
				}
			}
			final double dfaTransitionDensity = new Analyze<>(new AutomataLibraryServices(mServices), dia, false)
					.getTransitionDensity(SymbolType.INTERNAL);
			mLogger.info("DFA transition density " + dfaTransitionDensity);
			if (mPref.dumpAutomata()) {
				final String filename =
						new SubtaskIterationIdentifier(mTaskIdentifier, getIteration()) + "_EagerFloydHoareAutomaton";
				super.writeAutomatonToFile(dia, filename);
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}

		if (mComputeHoareAnnotation) {
			assert new InductivityCheck<>(mServices, dia, false, true,
					new IncrementalHoareTripleChecker(super.mCsToolkit, false)).getResult() : "Not inductive";
		}
		if (mPref.dumpAutomata()) {
			final String filename = "InterpolantAutomatonDeterminized_Iteration" + mIteration;
			writeAutomatonToFile(dia, filename);
		}
		// assert accepts(mServices, dia, mCounterexample.getWord(),
		// true) : "Counterexample not accepted by determinized interpolant automaton: "
		// + mCounterexample.getWord();
		mLogger.debug("Sucessfully determinized");
		return new Pair<>(dia, dpod);
	}

	private String generateOnDemandEnhancementCanceledMessage(
			final INestedWordAutomaton<LETTER, IPredicate> interpolAutomaton,
			final Set<LETTER> universalSubtrahendLoopers, final Set<LETTER> alphabet, final int iteration) {
		return "enhancing Floyd-Hoare automaton (" + interpolAutomaton.getStates().size() + "states, "
				+ universalSubtrahendLoopers.size() + "/" + alphabet.size() + " universal loopers) in iteration "
				+ iteration;
	}

	private Set<LETTER> determineUniversalSubtrahendLoopers(final Set<LETTER> alphabet, final Set<IPredicate> states) {
		final Set<LETTER> result = new HashSet<>();
		for (final LETTER letter : alphabet) {
			final boolean isUniversalLooper = isUniversalLooper(letter, states);
			if (isUniversalLooper) {
				result.add(letter);
			}
		}
		return result;
	}

	private boolean isUniversalLooper(final LETTER letter, final Set<IPredicate> states) {
		if (letter.getTransformula().isInfeasible() != Infeasibility.UNPROVEABLE) {
			return false;
		}
		for (final IPredicate predicate : states) {
			final boolean isIndependent = isIndependent(letter, predicate);
			if (!isIndependent) {
				return false;
			}
		}
		return true;
	}

	private boolean isIndependent(final LETTER letter, final IPredicate predicate) {
		final Set<IProgramVar> in = letter.getTransformula().getInVars().keySet();
		final Set<IProgramVar> out = letter.getTransformula().getOutVars().keySet();
		return !DataStructureUtils.haveNonEmptyIntersection(in, predicate.getVars())
				&& !DataStructureUtils.haveNonEmptyIntersection(out, predicate.getVars());
	}

	@Override
	protected void computeCFGHoareAnnotation() {
		throw new UnsupportedOperationException("Petri net based analysis cannot compute Hoare annotation.");
	}

	private boolean acceptsPetriViaFA(final IUltimateServiceProvider services,
			final IAutomaton<LETTER, IPredicate> automaton, final Word<LETTER> word)
			throws AutomataOperationCanceledException, PetriNetNot1SafeException {
		final NestedWord<LETTER> nw = NestedWord.nestedWord(word);
		final INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate> petriNetAsFA =
				new PetriNet2FiniteAutomaton<>(new AutomataLibraryServices(services), mPredicateFactoryResultChecking,
						(IPetriNet<LETTER, IPredicate>) automaton).getResult();
		return super.accepts(services, petriNetAsFA, nw, false);
	}

	@Override
	public IStatisticsDataProvider getCegarLoopBenchmark() {
		return mPetriClStatisticsGenerator;
	}



}
