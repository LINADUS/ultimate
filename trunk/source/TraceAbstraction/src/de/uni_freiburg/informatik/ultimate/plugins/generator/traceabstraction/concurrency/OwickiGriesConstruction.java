/*
 * Copyright (C) 2020 University of Freiburg
 *
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 *
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.automata.petrinet.IPetriNet;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.ITransition;
import de.uni_freiburg.informatik.ultimate.automata.petrinet.Marking;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.ModelCheckerUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.DefaultIcfgSymbolTable;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaBuilder;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.TransFormulaUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.transitions.UnmodifiableTransFormula;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.BasicPredicateFactory;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.ManagedScript;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.lib.smtlibutils.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.util.datastructures.DataStructureUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;

/**
 * TODO
 *
 * @author Dominik Klumpp (klumpp@informatik.uni-freiburg.de)
 * @author Miriam Lagunes (miriam.lagunes@students.uni-freiburg.de)
 *
 *
 * @param <PLACE>
 */
public class OwickiGriesConstruction<LOC extends IcfgLocation, PLACE, LETTER> {
    
	private final IPetriNet<LETTER, PLACE> mNet;
	private final Map<Marking<LETTER, PLACE>, IPredicate> mFloydHoareAnnotation;

	private final OwickiGriesAnnotation<LETTER, PLACE> mAnnotation;
	private final IUltimateServiceProvider mServices;
	private final ManagedScript mManagedScript;
	private final Script mScript;
	private final BasicPredicateFactory mFactory;
	private final ILogger mLogger;
	private final DefaultIcfgSymbolTable mSymbolTable;
	private final static SimplificationTechnique mSimplificationTechnique = SimplificationTechnique.SIMPLIFY_DDA;
	private final static XnfConversionTechnique mXnfConversionTechnique =
			XnfConversionTechnique.BOTTOM_UP_WITH_LOCAL_SIMPLIFICATION;

	// Variables for Annotation construction
	private final Map<PLACE, IProgramVar> mGhostVariables;
	private final Map<PLACE, IPredicate> mFormulaMappingD;
	private final Map<IProgramVar, Term> mGhostInitAssignment;
	private final Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> mAssignmentMapping;

	public OwickiGriesConstruction(final IUltimateServiceProvider services, final CfgSmtToolkit csToolkit,
			final IPetriNet<LETTER, PLACE> net, final Map<Marking<LETTER, PLACE>, IPredicate> floydHoare) {
		mNet = net;
		mFloydHoareAnnotation = floydHoare;
		mScript = csToolkit.getManagedScript().getScript();
		mManagedScript = csToolkit.getManagedScript();
		mSymbolTable = new DefaultIcfgSymbolTable(csToolkit.getSymbolTable(), csToolkit.getProcedures());
		mServices = services;
		mLogger = services.getLoggingService().getLogger(ModelCheckerUtils.PLUGIN_ID);
		mFactory = new BasicPredicateFactory(mServices, mManagedScript, mSymbolTable);
		mGhostVariables = getGhostVariables();
		mFormulaMappingD = getFormulaMapping();
		mAssignmentMapping = getAssignmentMapping();
		mGhostInitAssignment = getGhostInitAssignment();

		mAnnotation = new OwickiGriesAnnotation<>(mFormulaMappingD, mAssignmentMapping, new HashSet<>(mGhostVariables.values()),
				mGhostInitAssignment, mNet, mSymbolTable);
	}
	
	/**
	 * Predicate: disjunction of Markings predicate. Markings predicate: Conjunction of All GhostVariable and FH
	 * predicate.
	 *
	 * @return a Map with a predicate for each place in Net.
	 */
	public Map<PLACE, IPredicate> getFormulaMapping() {
		final Map<PLACE, IPredicate> mapping = new HashMap<>();
		for (final PLACE place : mNet.getPlaces()) {
			final Set<IPredicate> clauses = new HashSet<>();
			mFloydHoareAnnotation.forEach((marking, formula) -> {
				if (marking.contains(place)) {
					clauses.add(getMarkingPredicate(place, marking));
				}
			});
			mapping.put(place, mFactory.or(clauses));
		}
		return mapping;
	}

	/**
	 * @param place
	 * @param marking
	 * @return Predicate with conjunction of Ghost variables and predicate of marking
	 */
	private IPredicate getMarkingPredicate(final PLACE place, final Marking<LETTER, PLACE> marking) {		
		final Set<IPredicate> terms = new HashSet<>();
		for (final PLACE otherPlace : marking) {
			final IPredicate ghost = getGhostPredicate(otherPlace);
			terms.add(ghost);
		}		
		terms.addAll(getAllNotMarking(marking)); 
		terms.add(mFloydHoareAnnotation.get(marking)); 
		return mFactory.and(terms);
	}

	/**
	 *
	 * @param marking
	 * @return Formula MethodB:Predicate with GhostVariables of all other places not in marking
	 */
	private Set<IPredicate> getAllNotMarking(final Marking<LETTER, PLACE> marking) {
		final Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		final Set<PLACE> notMarking = DataStructureUtils.difference(new HashSet<>(mNet.getPlaces()), markPlaces);
		final Set<IPredicate> predicates = new HashSet<>();
		for(PLACE place: notMarking) {
			 predicates.add(mFactory.not(getGhostPredicate(place)));
		}		
		return predicates;
	}

	/**
	 *
	 * @param marking
	 * @return Formula MethodA: GhostVariables if marking is subset of other marking
	 */
	private Set<IPredicate> getSubsetMarking(final Marking<LETTER, PLACE> marking) {
		final Set<PLACE> markPlaces = marking.stream().collect(Collectors.toSet());
		final Set<Marking<LETTER, PLACE>> markings = mFloydHoareAnnotation.keySet();
		final Set<PLACE> notInMarking = new HashSet<>();
		for(final Marking<LETTER, PLACE> otherMarking : markings) {
			notInMarking.addAll(getSupPlaces(otherMarking,markPlaces));
		}
		final Set<IPredicate> predicates = new HashSet<>();
		for(final PLACE place: notInMarking) {
			predicates.add(mFactory.not(getGhostPredicate(place)));
		}
		return predicates;
	}

	private Set<PLACE> getSupPlaces(final Marking<LETTER, PLACE> otherMarking, final Set<PLACE> markPlaces) {
		Set<PLACE> subPlaces = new HashSet<>();
		final Set<PLACE> otherPlaces = otherMarking.stream().collect(Collectors.toSet());
		if (otherPlaces.containsAll(markPlaces)) {
			subPlaces = DataStructureUtils.difference(otherPlaces, markPlaces);		
		}
		return subPlaces;
	}

	/**
	 * @param place
	 * @return Predicate place's GhostVariable
	 */
	private IPredicate getGhostPredicate(final PLACE place) {
		return mFactory.newPredicate(mGhostVariables.get(place).getTerm());
	}

	/**
	 * @return Map of GhostVariables to Places
	 */
	private Map<PLACE, IProgramVar> getGhostVariables() {
		final Map<PLACE, IProgramVar> ghostVars = new HashMap<>();
		int i = 0;
		final Collection<PLACE> places = mNet.getPlaces();
		mManagedScript.lock(this);
		try {
			for (final PLACE place : places) {
				final TermVariable tVar =
						mManagedScript.constructFreshTermVariable("np_" + i, SmtSortUtils.getBoolSort(mManagedScript));
				final IProgramVar pVar = ProgramVarUtils.constructGlobalProgramVarPair(tVar.getName(),
						SmtSortUtils.getBoolSort(mManagedScript), mManagedScript, this);
				mSymbolTable.add(pVar);
				ghostVars.put(place, pVar);
				i++;
			}
			return ghostVars;
		} finally {
			mManagedScript.unlock(this);
		}
	}

	/**
	 * @return set of Initial value assignment of all GhostVariables.
	 *
	 */
	private Map<IProgramVar, Term> getGhostInitAssignment() {
		final HashMap<IProgramVar, Term> initAssignments = new HashMap<>();
		//final Set<IProgramVar> initGhostVariables = new HashSet<>();
		for(PLACE place: mNet.getInitialPlaces()) {
			initAssignments.put(mGhostVariables.get(place), mScript.term("true"));
		}
		final Set<IProgramVar> notInitGhostVariables = DataStructureUtils.difference
				(new HashSet<>(mGhostVariables.values()), initAssignments.keySet());
		for(IProgramVar variable: notInitGhostVariables) {
			initAssignments.put(variable, mScript.term("false"));
		}
		return initAssignments;
	}

	/**
	 *
	 * @param place
	 * @return assignment of the place's GhostVariable.
	 */
	private UnmodifiableTransFormula getGhostAssignment(final Collection<IProgramVar> vars, final String term) {
		return TransFormulaBuilder.constructAssignment(new ArrayList<>(vars),
				Collections.nCopies(vars.size(), mScript.term(term)), mSymbolTable, mManagedScript);
	}

	/**
	 *
	 * @return Map of Places' Ghost Variables assignments to Transitions
	 *
	 */
	private Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> getAssignmentMapping() {
		final Map<ITransition<LETTER, PLACE>, UnmodifiableTransFormula> assignmentMapping = new HashMap<>();
		for(ITransition<LETTER,PLACE> transition: mNet.getTransitions()) {
			assignmentMapping.put(transition, getTransitionAssignment(transition));
		}
		return assignmentMapping;
	}

	/**
	 *
	 * @param transition
	 * @return TransFormula of sequential compositions of GhostVariables assignments. GhostVariables of Predecessors
	 *         Places are assign to false, GhostVariables of Successors Places are assign to true.
	 */
	private UnmodifiableTransFormula getTransitionAssignment(final ITransition<LETTER, PLACE> transition) {
		final List<UnmodifiableTransFormula> assignments = new ArrayList<>();
		for(PLACE place: mNet.getPredecessors(transition)) {
			assignments.add(getGhostAssignment(Collections.nCopies(1,  mGhostVariables.get(place)), "false"));
		}
		for(PLACE place: mNet.getSuccessors(transition)) {
			assignments.add(getGhostAssignment(Collections.nCopies(1, mGhostVariables.get(place)), "true"));
		}
		return TransFormulaUtils.sequentialComposition(mLogger, mServices, mManagedScript, false, false, false,
				mXnfConversionTechnique, mSimplificationTechnique, assignments);
	}

	public OwickiGriesAnnotation<LETTER, PLACE> getResult() {
		return mAnnotation;
	}

	public HashRelation<ITransition<LETTER, PLACE>, PLACE> getCoMarkedPlaces() {
		final HashRelation<ITransition<LETTER, PLACE>, PLACE> relation = new HashRelation<>();
		for (final ITransition<LETTER, PLACE> transition : mNet.getTransitions()) {
			final Set<PLACE> predecessors = mNet.getPredecessors(transition);			
			final Set<Marking<LETTER, PLACE>> enabledMarkings = mFloydHoareAnnotation.keySet().stream()
					.filter(marking -> marking.containsAll(predecessors)).collect(Collectors.toSet());		
			for (final Marking<LETTER, PLACE> marking : enabledMarkings) {
				for (final PLACE place : marking) {
					// places that are not predecessors of transition
					if (!predecessors.contains(place)) {
						relation.addPair(transition, place);
					}
				}
			}
		}
		return relation;
	}

}
