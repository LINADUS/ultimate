/*
 * Copyright (C) 2011-2015 Julian Jarecki (jareckij@informatik.uni-freiburg.de)
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2009-2015 University of Freiburg
 *
 * This file is part of the ULTIMATE Automata Library.
 *
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Automata Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.petrinet.unfolding;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.util.datastructures.DataStructureUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;

/**
 * A co-relation between a {@link Condition} and an {@link Event}.
 *
 * @author Julian Jarecki (jareckij@informatik.uni-freiburg.de)
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * @param <LETTER>
 *            symbol type
 * @param <PLACE>
 *            place content type
 */
public class ConditionEventsCoRelation<LETTER, PLACE> implements ICoRelation<LETTER, PLACE> {
	private long mQueryCounterYes;
	private long mQueryCounterNo;

	/**
	 * TODO schaetzc 2018-08-16: This does not seem to store all co-relations between conditions and events.
	 * Document which subset is stored.
	 * For [1] the co-relation between the only a-event and all p3-conditions were missing.
	 * [1] trunk/examples/Automata/regression/pn/operations/removeDead/VitalParallel.ats
	 * TODO Matthias 2019-09-25: I just checked this and saw all three p3-conditions in relation
	 * with the (only) a-event. Maybe the problem has been fixed in the last 13 months.
	 */
	private final HashRelation<Condition<LETTER, PLACE>, Event<LETTER, PLACE>> mCoRelation = new HashRelation<>();
	private final BranchingProcess<LETTER, PLACE> mBranchingProcess;

	/**
	 * Constructor.
	 *
	 * @param branchingProcess
	 *            branching process
	 */
	public ConditionEventsCoRelation(final BranchingProcess<LETTER, PLACE> branchingProcess) {
		mBranchingProcess = branchingProcess;
	}

	@Override
	public long getQueryCounterYes() {
		return mQueryCounterYes;
	}
	
	@Override
	public long getQueryCounterNo() {
		return mQueryCounterNo;
	}


	@Override
	public void initialize(final Set<Condition<LETTER, PLACE>> initialConditions) {
		// there are no events the conditions could be in relation with yet.
		// hence there's nothing to do here
	}

	@Override
	public void update(final Event<LETTER, PLACE> e) {
		if (e.getTransition() == null) {
			assert e.getPredecessorConditions().isEmpty() : "not initial event";
			return;
		}
//		final HashRelation<Condition<LETTER, PLACE>, Event<LETTER, PLACE>> newCoRelation = new HashRelation<>();
//		final HashRelation<Condition<LETTER, PLACE>, Event<LETTER, PLACE>> oldCoRelation = new HashRelation<>();
		// An existing condition c is in co-relation with e if the predecessor event
		// of c is in co-relation with all predecessor events of e.
		// Successor conditions of e are in co-relation with all events e' that are
		// in co-relation with all predecessor conditions of e.
		{
			// new method - continuity
			final Set<Event<LETTER, PLACE>>[] coRelatedEvents = e.getPredecessorConditions().stream()
					.map(mCoRelation::getImage).toArray(Set[]::new);
			final Set<Event<LETTER, PLACE>> intersection = DataStructureUtils.intersection(Arrays.asList(coRelatedEvents));
			for (final Event<LETTER, PLACE> coRelatedEvent : intersection) {
				for (final Condition<LETTER, PLACE> c : e.getSuccessorConditions()) {
					mCoRelation.addPair(c, coRelatedEvent);
//					newCoRelation.addPair(c, coRelatedEvent);
				}
				for (final Condition<LETTER, PLACE> c : coRelatedEvent.getSuccessorConditions()) {
					mCoRelation.addPair(c, e);
//					newCoRelation.addPair(c, e);
				}
			}
		}

		// for each (strict) ancestor event anct, the anct-successor c is in
		// co-relation with e if for each successor event csucc of c
		// csucc is not in the local configuration of e.
		{
			// new method siblings
			final Set<Event<LETTER, PLACE>> ancestorEvents = new HashSet<>(e.getLocalConfiguration());
			ancestorEvents.add(mBranchingProcess.getDummyRoot());
			for (final Event<LETTER, PLACE> parent : ancestorEvents) {
				if (parent.equals(e)) {
					continue;
				}
				for (final Condition c : parent.getSuccessorConditions()) {
					if (!someSuccessorEventIsAncestorOfE(c, e)) {
						mCoRelation.addPair(c, e);
//						newCoRelation.addPair(c,e);
					}
				}
			}
		}

//		{
//			// old method
//			for (final Event<LETTER, PLACE> e1 : mBranchingProcess.getEvents()) {
//				if (isInIrreflexiveCoRelation(e, e1)) {
//					for (final Condition<LETTER, PLACE> c : e1.getSuccessorConditions()) {
//						assert !e.getPredecessorConditions().contains(c);
//						assert !e.getSuccessorConditions().contains(c);
//						oldCoRelation.addPair(c,e);
//					}
//				}
//			}
//
//			for (final Condition<LETTER, PLACE> c : e.getConditionMark()) {
//				if (!e.getSuccessorConditions().contains(c)) {
//					assert !e.getSuccessorConditions().contains(c);
//					assert !mBranchingProcess.inCausalRelation(c, e) : c + " , " + e
//					+ " in causal relation, not in co-relation!";
//					oldCoRelation.addPair(c,e);
//				}
//			}
//		}

		// Original plan: insert all siblings of predecessors of e, that are not
		// predecessors of e.
		// Problem: there is a situation where this might be incorrect.

		// Solution: only add Conditions that build a CoSet with the new Events
		// Predecessors.

		// Next Problem: this is incomplete.
		// There may be nodes, that are in co-relation with
		// the newly added Event e that are NOT siblings of
		// the predecessor-conditions of e.

		// Solution: iterate through all ancestors and do this

		// (this is the old code)
		/*
		for (Condition<LETTER, PLACE> c : e.getPredecessorConditions()) {
			for (Condition<LETTER, PLACE> sibling : c.getPredecessorEvent()
					.getSuccessorConditions()) {
				if (!e.getPredecessorConditions().contains(sibling)
						&& isCoset(e.getPredecessorConditions(), sibling)) {
					assert (!e.getSuccessorConditions().contains(sibling));
					coRelation.get(sibling).add(e);
				}
			}
		}
		*/
//		oldCoRelation.addAll(mCoRelation);
//		newCoRelation.addAll(mCoRelation);
//		assert oldCoRelation.equals(newCoRelation) : "old " + oldCoRelation + " new: " + newCoRelation;
//		mCoRelation.addAll(newCoRelation);
	}

	private boolean someSuccessorEventIsAncestorOfE(final Condition<LETTER, PLACE> c, final Event<LETTER, PLACE> e) {
		for (final Event<LETTER, PLACE> succEvent : c.getSuccessorEvents()) {
			if (e.getLocalConfiguration().contains(succEvent)) {
					return true;
			}
		}
		return false;
	}

	/*
	private void add(Condition<LETTER, PLACE> c, Event<LETTER, PLACE> e) {
		Set<Event<LETTER, PLACE>> eSet = coRelation.get(c);
		if (eSet == null) {
			eSet = new HashSet<Event<LETTER, PLACE>>();
			coRelation.put(c, eSet);
		}
		eSet.add(e);
	}
	*/

	@Override
	public boolean isInCoRelation(final Condition<LETTER, PLACE> c1, final Condition<LETTER, PLACE> c2) {
		final boolean result = mCoRelation.containsPair(c1, c2.getPredecessorEvent())
				|| mCoRelation.containsPair(c2, c1.getPredecessorEvent())
				|| (c1.getPredecessorEvent() == c2.getPredecessorEvent());
		assert result == isInCoRelationNaive(c1, c2) :
				String.format("contradictory co-Relation for %s,%s: normal=%b != %b=naive", c1, c2, result, !result);
		if (result) {
			mQueryCounterYes++;
		} else {
			mQueryCounterNo++;
		}
		return result;
	}

	private boolean isInCoRelationNaive(final Condition<LETTER, PLACE> c1, final Condition<LETTER, PLACE> c2) {
		return !mBranchingProcess.inCausalRelation(c1, c2) && !mBranchingProcess.inConflict(c1, c2);
	}

	/**
	 * Checks if two events are in irreflexive co-relation, hereafter "ic".
	 * <p>
	 * x ic y iif (x co y and x != y)
	 * <p>
	 * with *e we denote the predecessor-nodes of e.
	 * <p>
	 * 1. If e1 ic e2 then their parents are pairwise in irreflexive co-relation.<br>
	 * <b>Proof:</b> <br>
	 * let e1 co e2. Furthermore let ci be a predecessor of ei for i \in {1,2}
	 * <p>
	 * If c1#c2 then e1#e2 _|_.<br>
	 * If c1 and c2 are equal then e1#e2 or e1=e2 _|_.<br>
	 * If c1 and c2 are in causal relation, then e1 is in causal relation to e2 or e1 # e2 _|_<br>
	 * q.e.d.
	 * <p>
	 * 2. If for all c1 \in *e1, c2 \in *e2: c1 ic c2 then e1 ci e2.<br>
	 * <b>Proof:</b>Assume the left side of the implication.<br>
	 *
	 * <u>TODO schaetzc 2018-08-16: The next line is not true in the general case.
	 * It is possible for a transition/event to have no predecessors.
	 * In a bounded net such transition must also have no successors.
	 * Do we support such transitions?</u><br>
	 *
	 * If e1 = e2 it is trivial, that there are c1,c2 s.t. c1=c2 _|_<br>
	 * Assume e1 < e2, then there has to be a path between e1 and e2 s.t. \exists c1 \in *e1 s.t. c1 < e2. For each
	 * parent c2 \in *e2 then c1 < c2 holds. (e1 > e2 analogously) _|_<br>
	 * Note: This is based on the assumption, that both Events have at least one predecessor-condition. <br>
	 * Assume e1#e2. There is a Condition c and Events e1', e2' \in c* s.t. e1' < e1 and e2' < e2. There is c1 \in *e1
	 * s.t. either c1 < c or c1=c.<br>
	 * If e2 = e2' then c \in *e2 _|_ (e1=e1' analogously). <br>
	 * If e1 != e1' and e2 != e2' then there are predecessor-conditions c1 \in *e1, c2 \in *e2 s.t. c1#c2 _|_. <br>
	 * q.e.d.
	 *
	 * @param e1 An event
	 * @param e2 Another event
	 * @return e1 ic e2 (e1 and e2 are in irreflexive co-relation)
	 */
	public boolean isInIrreflexiveCoRelation(final Event<LETTER, PLACE> e1, final Event<LETTER, PLACE> e2) {
		if (e1 == e2) {
			return false;
		}
		if (mBranchingProcess.getDummyRoot() == e1 || mBranchingProcess.getDummyRoot() == e2) {
			return false;
		}
		final Collection<Condition<LETTER, PLACE>> conditions1 = e1.getPredecessorConditions();
		final Collection<Condition<LETTER, PLACE>> conditions2 = e2.getPredecessorConditions();
		for (final Condition<LETTER, PLACE> c1 : conditions1) {
			// e1 and e2 are in conflict
			if (conditions2.contains(c1) || !isCoset(conditions2, c1)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isInCoRelation(final Condition<LETTER, PLACE> cond, final Event<LETTER, PLACE> event) {
		if (event.getPredecessorConditions().contains(cond)) {
			return false;
		}
		return isCoset(event.getPredecessorConditions(), cond);
	}

	@Override
	public boolean isCoset(final Collection<Condition<LETTER, PLACE>> coSet, final Condition<LETTER, PLACE> c) {
		for (final Condition<LETTER, PLACE> condition : coSet) {
			if (!isInCoRelation(c, condition)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return mCoRelation.toStringAsTable();
	}

	@Override
	public Set<Condition<LETTER, PLACE>> computeCoRelatatedConditions(final Condition<LETTER, PLACE> cond) {
		final Set<Event<LETTER, PLACE>> coRelatedEvents = mCoRelation.getImage(cond);
		final Set<Condition<LETTER, PLACE>> result = coRelatedEvents.stream()
				.flatMap(x -> x.getSuccessorConditions().stream()).collect(Collectors.toSet());
		cond.getPredecessorEvent().getConditionMark().addTo(result);
		return result;
	}
}
