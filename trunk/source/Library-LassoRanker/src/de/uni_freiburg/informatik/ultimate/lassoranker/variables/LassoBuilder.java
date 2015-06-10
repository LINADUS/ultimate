/*
 * Copyright (C) 2012-2014 University of Freiburg
 *
 * This file is part of the ULTIMATE LassoRanker Library.
 *
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by
 * linking or combining it with Eclipse RCP (or a modified version of
 * Eclipse RCP), containing parts covered by the terms of the Eclipse Public
 * License, the licensors of the ULTIMATE LassoRanker Library grant you
 * additional permission to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lassoranker.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearTransition;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.smtinterpol.util.DAGSize;


/**
 * 
 * The LassoBuilder class holds the lasso components during preprocessing.
 * With the LassoBuilder we are building two lassos at the same time:
 * an overapproximated lasso (for termination analysis) and
 * an underapproximated lasso (fer nontermination analysis).`
 * 
 * This object is *not* immutable.
 * 
 * @author Jan Leike
 */
public class LassoBuilder {
	/**
	 * The Boogie2SMT object
	 */
	private final Boogie2SMT m_boogie2smt;
	
	/**
	 * Collection of all generated replacement TermVariables
	 */
	private final Collection<TermVariable> m_termVariables;
	
	/**
	 * Independent components of the stem transition
	 * (possibly an overapproximation)
	 */
	private List<TransFormulaLR> m_stem_components_t;
	
	/**
	 * Independent components of the stem transition
	 * (possibly an underapproximation)
	 */
	private List<TransFormulaLR> m_stem_components_nt;
	
	/**
	 * Independent components of the loop transition
	 * (possibly an overapproximation)
	 */
	private List<TransFormulaLR> m_loop_components_t;
	
	/**
	 * Independent components of the loop transition
	 * (possibly an underapproximation)
	 */
	private List<TransFormulaLR> m_loop_components_nt;
	
	/**
	 * The script used to create terms in the transition formulas
	 */
	private final Script m_Script;
	
	/**
	 * Object that has to be used for getting and constructing ReplacementVars
	 * that occur in this LassoBuilder.
	 */
	private final ReplacementVarFactory m_ReplacementVarFactory;
	
	/**
	 * Create new (empty) LassoBuilder
	 */
	LassoBuilder(Script script, Boogie2SMT boogie2smt) {
		assert script != null;
		assert boogie2smt != null;
		m_Script = script;
		m_boogie2smt = boogie2smt;
		m_termVariables = new ArrayList<TermVariable>();
		m_stem_components_t = new ArrayList<TransFormulaLR>();
		m_stem_components_nt = m_stem_components_t;
		m_loop_components_t = new ArrayList<TransFormulaLR>();
		m_loop_components_nt = m_loop_components_t;
		m_ReplacementVarFactory =
				new ReplacementVarFactory(m_boogie2smt.getVariableManager());
	}
	
	/**
	 * Create a new LassoBuilder object from components
	 * 
	 * @param script the script that created the transition formulae
	 * @param boogie2smt the boogie smt translator
	 * @param stem the stem transition
	 * @param loop the loop transition
	 */
	public LassoBuilder(Script script, Boogie2SMT boogie2smt, TransFormula stem,
			TransFormula loop) {
		this(script, boogie2smt);
		m_stem_components_t.add(
				TransFormulaLR.buildTransFormula(stem, m_ReplacementVarFactory));
		m_loop_components_t.add(
				TransFormulaLR.buildTransFormula(loop, m_ReplacementVarFactory));
	}
	
	/**
	 * @return the script used to generate the transition formulas
	 */
	public Script getScript() {
		return m_Script;
	}
	
	/**
	 * @return the associated Boogie2SMT object
	 */
	public Boogie2SMT getBoogie2SMT() {
		return m_boogie2smt;
	}
	
	public ReplacementVarFactory getReplacementVarFactory() {
		return m_ReplacementVarFactory;
	}

	/**
	 * @return a collection of all new TermVariable's created with this object
	 */
	public Collection<TermVariable> getGeneratedTermVariables() {
		return Collections.unmodifiableCollection(m_termVariables);
	}
	
	/**
	 * Is the stem the same for termination analysis and nontermination analysis?
	 * @return whether getStemComponentsTermination() == getStemComponentsNonTermination()
	 */
	public boolean isStemApproximated() {
		return m_stem_components_t != m_stem_components_nt;
	}
	
	/**
	 * Is the loop the same for termination analysis and nontermination analysis?
	 * @return whether getLoopComponentsTermination() == getLoopComponentsNonTermination()
	 */
	public boolean isLoopApproximated() {
		return m_loop_components_t != m_loop_components_nt;
	}
	
	/**
	 * @return the stem's components (possibly overapproximation)
	 */
	public List<TransFormulaLR> getStemComponentsTermination() {
		return m_stem_components_t;
	}
	
	/**
	 * @return the stem's components (possibly underapproximation)
	 */
	public List<TransFormulaLR> getStemComponentsNonTermination() {
		return m_stem_components_nt;
	}
	
	/**
	 * @return the loop's components (possibly overapproximation)
	 */
	public List<TransFormulaLR> getLoopComponentsTermination() {
		return m_loop_components_t;
	}
	
	/**
	 * @return the loop's components (possibly underapproximation)
	 */
	public List<TransFormulaLR> getLoopComponentsNonTermination() {
		return m_loop_components_nt;
	}
	
	/**
	 * Set new overapproximation for stem components
	 * @param stem_components the new stem components
	 */
	public void setStemComponentsTermination(
			List<TransFormulaLR> stem_components) {
		m_stem_components_t = stem_components;
	}
	
	/**
	 * Set new underapproximation for stem components
	 * @param stem_components the new stem components
	 */
	public void setStemComponentsNonTermination(
			List<TransFormulaLR> stem_components) {
		m_stem_components_nt = stem_components;
	}
	
	/**
	 * Set new overapproximation for loop components
	 * @param loop_components the new loop components
	 */
	public void setLoopComponentsTermination(
			List<TransFormulaLR> loop_components) {
		m_loop_components_t = loop_components;
	}
	
	/**
	 * Set new underapproximation for loop components
	 * @param loop_components the new loop components
	 */
	public void setLoopComponentsNonTermination(
			List<TransFormulaLR> loop_components) {
		m_loop_components_nt = loop_components;
	}
	
	/**
	 * Run a few sanity checks
	 * @return false if something is fishy
	 */
	public boolean isSane() {
		boolean sane = true;
		sane &= m_stem_components_t.size() == m_loop_components_t.size();
		sane &= m_stem_components_nt.size() == m_loop_components_nt.size();
		for (TransFormulaLR tf : m_stem_components_t) {
			sane &= tf.auxVarsDisjointFromInOutVars();
			sane &= tf.allAreInOutAux(tf.getFormula().getFreeVars()) == null;
		}
		for (TransFormulaLR tf : m_stem_components_nt) {
			sane &= tf.auxVarsDisjointFromInOutVars();
			sane &= tf.allAreInOutAux(tf.getFormula().getFreeVars()) == null;
		}
		for (TransFormulaLR tf : m_loop_components_t) {
			sane &= tf.auxVarsDisjointFromInOutVars();
			sane &= tf.allAreInOutAux(tf.getFormula().getFreeVars()) == null;
		}
		for (TransFormulaLR tf : m_loop_components_nt) {
			sane &= tf.auxVarsDisjointFromInOutVars();
			sane &= tf.allAreInOutAux(tf.getFormula().getFreeVars()) == null;
		}
		return sane;
	}
	
	/**
	 * Extract the overapproximated lassos (for termination analysis)
	 * 
	 * Only succeeds if the transition formulas are of the required form,
	 * i.e., if preprocessing has been completed.
	 * 
	 * @return a colletion of lassos, one for each component
	 * @throws TermException if the transition formulas are not of the correct
	 *                       form
	 */
	public Collection<Lasso> getLassosTermination() throws TermException {
		assert m_stem_components_t.size() == m_loop_components_t.size();
		int n = m_stem_components_t.size();
		List<Lasso> lassos = new ArrayList<Lasso>(n);
		for (int i = 0; i < n; ++i) {
			TransFormulaLR stemTF = m_stem_components_t.get(i);
			TransFormulaLR loopTF = m_loop_components_t.get(i);
			LinearTransition stem = LinearTransition.fromTransFormulaLR(stemTF);
			LinearTransition loop = LinearTransition.fromTransFormulaLR(loopTF);
			lassos.add(new Lasso(stem, loop));
		}
		return lassos;
	}
	
	/**
	 * Extract the underapproximated lassos (for nontermination analysis)
	 * 
	 * Only succeeds if the transition formulas are of the required form,
	 * i.e., if preprocessing has been completed.
	 * 
	 * @return a collection of lassos, one for each component
	 * @throws TermException if the transition formulas are not of the correct
	 *                       form
	 */
	public Collection<Lasso> getLassosNonTermination() throws TermException {
		assert m_stem_components_nt.size() == m_loop_components_nt.size();
		int n = m_stem_components_nt.size();
		List<Lasso> lassos = new ArrayList<Lasso>(n);
		for (int i = 0; i < n; ++i) {
			TransFormulaLR stemTF = m_stem_components_nt.get(i);
			TransFormulaLR loopTF = m_loop_components_nt.get(i);
			LinearTransition stem = LinearTransition.fromTransFormulaLR(stemTF);
			LinearTransition loop = LinearTransition.fromTransFormulaLR(loopTF);
			lassos.add(new Lasso(stem, loop));
		}
		return lassos;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			Collection<Lasso> lassosNonTermination = getLassosNonTermination();
			Collection<Lasso> lassosTermination = getLassosTermination();
			
			sb.append("Overapproximated lassos:\n");
			for (Lasso lasso : lassosTermination) {
				sb.append(lasso);
				sb.append("\n");
			}
			sb.append("Underapproximated lassos:\n");
			for (Lasso lasso : lassosNonTermination) {
				sb.append(lasso);
				sb.append("\n");
			}
		} catch (TermException e) {
			sb.append("Preprocessing has not been completed.\n");
			
			if (isStemApproximated()) {
				sb.append("Current stem components (overapproximated):\n");
				for (TransFormulaLR tf : m_stem_components_t) {
					sb.append(tf);
					sb.append("\n");
				}
				sb.append("Current stem components (underapproximated):\n");
				for (TransFormulaLR tf : m_stem_components_nt) {
					sb.append(tf);
					sb.append("\n");
				}
				sb.append("Current loop components (overapproximated):\n");
				for (TransFormulaLR tf : m_loop_components_t) {
					sb.append(tf);
					sb.append("\n");
				}
				sb.append("Current loop components (underapproximated):\n");
				for (TransFormulaLR tf : m_loop_components_nt) {
					sb.append(tf);
					sb.append("\n");
				}
			} else {
				sb.append("Current stem components:\n");
				for (TransFormulaLR tf : m_stem_components_t) {
					sb.append(tf);
					sb.append("\n");
				}
				sb.append("Current loop components:\n");
				for (TransFormulaLR tf : m_loop_components_t) {
					sb.append(tf);
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}
	
	public static int computeMaxDagSize(Collection<TransFormulaLR> components) {
		TreeSet<Integer> sizes = new TreeSet<>();
		for (TransFormulaLR tflr : components) {
			int dagSize = (new DAGSize()).size(tflr.getFormula());
			sizes.add(dagSize);
		}
		if (sizes.isEmpty()) {
			return 0;
		} else {
			return sizes.descendingIterator().next();
		}
	}
	
	public int computeMaxDagSizeStem() {
		return computeMaxDagSize(m_stem_components_t);
	}
	
	public int computeMaxDagSizeLoop() {
		return computeMaxDagSize(m_loop_components_t);
	}

}
