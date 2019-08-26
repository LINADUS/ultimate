/*
 * Copyright (C) 2019 Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE Library-SymbolicInterpretation plug-in.
 *
 * The ULTIMATE Library-SymbolicInterpretation plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Library-SymbolicInterpretation plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-SymbolicInterpretation plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-SymbolicInterpretation plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-SymbolicInterpretation plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lib.symbolicinterpretation.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.logic.AnnotatedTerm;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.LetTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

/**
 * Converts a term into one (!) interval. Before evaluation intervals can be assigned to variables.
 * Examples:
 * <ul>
 * <li> 1 evaluates to [1, 1].
 * <li> 1 + 2 evaluates to [3, 3].
 * <li> x evaluates to the given interval for x or [-inf, inf] if no interval was given for this variable.
 * <li> TODO document what happends for terms of non-numeric sort
 * </ul>
 *
 * @author schaetzc@tf.uni-freiburg.de
 */
public class TermToInterval {

	// TODO should we use caching as in TermTransformer? (when implementing, watch out for changing scopes)

	/**
	 * Evaluates a term using interval arithmetic.
	 * @param term The term to be evaluated
	 * @param scope Already known values for some variables
	 * @return Result of the given term according to interval arithmetic
	 */
	public static Interval evaluate(final Term term, final Map<TermVariable, Interval> scope) {
		if (!term.getSort().isNumericSort()) {
			throw new IllegalArgumentException("Tried to use intervals for non-numeric sort: " + term);
		} else if (term instanceof ApplicationTerm) {
			return evaluate((ApplicationTerm) term, scope);
		} else if (term instanceof LetTerm) {
			return evaluate((LetTerm) term, scope);
		} else if (term instanceof AnnotatedTerm) {
			return evaluate((AnnotatedTerm) term, scope);
		} else if (term instanceof QuantifiedFormula) {
			return evaluate((QuantifiedFormula) term, scope);
		} else if (term instanceof ConstantTerm) {
			return evaluate((ConstantTerm) term, scope);
		} else if (term instanceof TermVariable) {
			return evaluate((TermVariable) term, scope);
		}
		throw new UnsupportedOperationException("Could not process term of unknown type: " + term);
	}

	private static Interval evaluate(final ConstantTerm term, final Map<TermVariable, Interval> scope) {
		final Object value = term.getValue();
		if (value instanceof Rational) {
			return Interval.point((Rational) value);
		} else if (value instanceof BigInteger) {
			return Interval.point(SmtUtils.toRational((BigInteger) value));
		} else if (value instanceof BigDecimal) {
			return Interval.point(SmtUtils.toRational((BigDecimal) value));
		}
		return Interval.TOP;
	}

	private static Interval evaluate(final AnnotatedTerm term, final Map<TermVariable, Interval> scope) {
		// TODO are there any annotations we have to consider?
		return evaluate(term.getSubterm(), scope);
	}

	private static Interval evaluate(final ApplicationTerm term, final Map<TermVariable, Interval> scope) {
		final int arity = term.getParameters().length;
		if (arity < 1) {
			return Interval.TOP;
		} else if (arity == 1) {
			return handleUnaryFunction(term, scope);
		} else {
			return handleNAryFunction(term, scope);
		}
	}

	private static Interval handleUnaryFunction(final ApplicationTerm term, final Map<TermVariable, Interval> scope) {
		assert term.getParameters().length == 1 : "Expected unary function but found " + term;
		if ("-".equals(term.getFunction().getName())) {
			final Term param = term.getParameters()[0];
			return evaluate(param, scope).negate();
		} else {
			return Interval.TOP;
		}
	}

	private static Interval handleNAryFunction(final ApplicationTerm term, final Map<TermVariable, Interval> scope) {
		// TODO support if-then-else terms (simply union both possible results?)
		final BiFunction<Interval, Interval, Interval> leftAssociativeOp =
				intervalOpForSmtFunc(term.getFunction().getName());
		if (leftAssociativeOp == null) {
			return Interval.TOP;
		}
		final Term[] params = term.getParameters();
		assert params.length >= 2 : "Expected n-ary function with n >= 2 but found " + term;
		Interval accumulator = evaluate(params[0], scope);
		for (int paramIdx = 1; paramIdx < params.length; ++paramIdx) {
			accumulator = leftAssociativeOp.apply(accumulator, evaluate(params[paramIdx], scope));
			// We cannot use a shortcut, even for an intermediate TOP result.
			// A BOTTOM result of the last parameter will turn everything to BOTTOM.
		}
		return accumulator;
	}

	private static BiFunction<Interval, Interval, Interval> intervalOpForSmtFunc(final String functionName) {
		switch (functionName) {
		case "+":
			return Interval::add;
		case "-":
			return Interval::subtract;
		case "*":
			return Interval::multiply;
		case "/":
			return Interval::divide;
		default:
			return null;
		}
	}

	private static Interval evaluate(final LetTerm term, final Map<TermVariable, Interval> outerScope) {
		// IScopedMap could be used with an intermediate map, but using a completely is easier
		final HashMap<TermVariable, Interval> innerScope = new HashMap<>(outerScope);
		final TermVariable[] letVariables = term.getVariables();
		final Term[] letValues = term.getValues();
		assert letVariables.length == letValues.length : "Number of variables and values does not match: " + term;
		for (int letIndex = 0; letIndex < letVariables.length; ++letIndex) {
			// TODO ignore variables whose values cannot be represented by intervals
			innerScope.put(letVariables[letIndex], evaluate(letValues[letIndex], outerScope));
		}
		return evaluate(term.getSubTerm(), innerScope);
	}

	private static Interval evaluate(final QuantifiedFormula term, final Map<TermVariable, Interval> scope) {
		throw new UnsupportedOperationException("Bool cannot be expressed as an interval.");
	}

	private static Interval evaluate(final TermVariable term, final Map<TermVariable, Interval> scope) {
		return scope.getOrDefault(term, Interval.TOP);
	}

}