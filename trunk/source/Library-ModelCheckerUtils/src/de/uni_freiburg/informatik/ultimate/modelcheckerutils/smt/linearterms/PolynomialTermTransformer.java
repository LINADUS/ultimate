package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearterms;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieUtils;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.FunctionSymbol;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermTransformer;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.BitvectorUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.util.datastructures.BitvectorConstant;

/**
 * Transforms a {@link Term} which is "polynomial" into our {@link PolynomialTerm} data
 * structure. The result is an auxiliary error term if the input was not polynomial.
 *
 * The transformation is done by an recursive algorithm. However, in order to
 * circumvent the problem that the performance of Java virtual machines is
 * rather poor for recursive methods calls we implement the algorithm by using
 * our {@link TermTransformer}. The {@link TermTransformer} allows us to
 * implement recursive algorithms for {@link Term}s without recursive methods
 * calls by explicitly using a stack.
 *
 * @author Leonard Fichtner
 *
 */
public class PolynomialTermTransformer extends TermTransformer {

	private final Script mScript;

	/**
	 * Predicate that defines which Terms might be variables of the
	 * {@link PolynomialTerm}. Currently, every {@link TermVariable} and every
	 * {@link ApplicationTerm} can be a variable of the result. In
	 * the future this may become a parameter in order to allow users
	 * of this class to be more restrictive.
	 */
	private final Predicate<Term> mIsPolynomialVariable = (x -> ((x instanceof TermVariable)
			|| (x instanceof ApplicationTerm)));

	public PolynomialTermTransformer(final Script script) {
		mScript = script;
	}

	@Override
	protected void convert(final Term term) {
		// If the term has a sort that is not supported, we report
		// that the input cannot be converted into a PolynomialTerm.
		// This is implemented by returning an special kind
		// of PolynomialTerm.
		if (!hasSupportedSort(term)) {
			inputIsNotPolynomial();
			return;
		}
		// Otherwise, if the terms represents a literal, we convert the literal
		// to a PolynomialTerm and tell the TermTransformer that this
		// is the result (i.e., it should not descend to subformulas).
		final Rational valueOfLiteral = tryToConvertToLiteral(mScript, term);
		if (valueOfLiteral != null) {
			final AffineTerm result = AffineTerm.constructConstant(term.getSort(), valueOfLiteral);
			setResult(result);
			return;
		}
		// Otherwise, if the term represents an "polynomial function" we tell
		// TermTransformer to descend to subformulas.
		if (isPolynomialFunction(term)) {
			super.convert(term);
			return;
		}
		// Otherwise, the term is considered as a variable of our PolynomialTerms,
		// we construct an PolynomialTerm that represents a variable and tell the
		// TermTransformer that this
		// is the result (i.e., it should not descend to subformulas).
		if (mIsPolynomialVariable.test(term)) {
			final AffineTerm result = AffineTerm.constructVariable(term);
			setResult(result);
			return;
		}
		// Otherwise, the input cannot be converted to an PolynomialTerm.
		inputIsNotPolynomial();
		return;
	}

	/**
	 * Sets result to auxiliary error term.
	 */
	private void inputIsNotPolynomial() {
		setResult(new PolynomialTerm());
	}

	/**
	 * Currently, we support the SMT {@link Sort}s Int and Real (called numeric
	 * sorts) and the bitvector sorts.
	 */
	private static boolean hasSupportedSort(final Term term) {
		return SmtSortUtils.isNumericSort(term.getSort()) || SmtSortUtils.isBitvecSort(term.getSort());
	}

	/**
	 * Check if term is an {@link ApplicationTerm} whose {@link FunctionSymbol}
	 * represents an "polynomial function". We call a function an "polynomial function" if
	 * it implements an addition, subtraction, multiplication, or real number
	 * division.
	 */
	private static boolean isPolynomialFunction(final Term term) {
		if (term instanceof ApplicationTerm) {
			final ApplicationTerm appTerm = (ApplicationTerm) term;
			return isPolynomialFunctionSymbol(appTerm.getFunction().getName());
		} else {
			return false;
		}
	}

	private static boolean isPolynomialFunctionSymbol(final String funName) {
		return (funName.equals("+") || funName.equals("-") || funName.equals("*") || funName.equals("/")
				|| funName.equals("bvadd") || funName.equals("bvsub") || funName.equals("bvmul"));
	}

	/**
	 * Check if term represents a literal. If this is the case, then return its
	 * value as a {@link Rational} otherwise return true.
	 */
	private static Rational tryToConvertToLiteral(final Script script, final Term term) {
		final Rational result;
		if (SmtSortUtils.isBitvecSort(term.getSort())) {
			final BitvectorConstant bc = BitvectorUtils.constructBitvectorConstant(term);
			if (bc != null) {
				result = Rational.valueOf(bc.getValue(), BigInteger.ONE);
			} else {
				result = null;
			}
		} else if (SmtSortUtils.isNumericSort(term.getSort())) {
			if (term instanceof ConstantTerm) {
				result = SmtUtils.convertConstantTermToRational((ConstantTerm) term);
			} else {
				result = null;
			}
		} else {
			result = null;
		}
		return result;
	}

	//TODO: Change PolynomialTermTransformer to use the more efficient class AffineTerm if possible.
	@Override
	public void convertApplicationTerm(final ApplicationTerm appTerm, final Term[] newArgs) {
		// This method is called for every subformula for which we let the
		// TermTransformer descend to subformulas.
		// Here, the arguments are the result of the "recursive" calls for the
		// subformulas.
		assert (isPolynomialFunctionSymbol(appTerm.getFunction().getName())) : "We only descended for polynomial functions";
		// First, we check if some of this arguments is the auxiliary error term.
		// If this is the case, we report that input is not polynomial.
		final IPolynomialTerm[] polynomialArgs = castAndCheckForNonPolynomialArguments(newArgs);
		if (polynomialArgs == null) {
			inputIsNotPolynomial();
			return;
		}
		final String funName = appTerm.getFunction().getName();
		if (funName.equals("*") || funName.equals("bvmul")) {
			final Sort sort = appTerm.getSort();
			final IPolynomialTerm result = multiply(sort, polynomialArgs);
			castAndSetResult(result);
			return;
		} else if (funName.equals("+") || funName.equals("bvadd")) {
			final IPolynomialTerm result = add(polynomialArgs);
			castAndSetResult(result);
			return;
		} else if (funName.equals("-") || funName.equals("bvsub")) {
			final IPolynomialTerm result;
			if (polynomialArgs.length == 1) {
				// unary minus
				result = negate(polynomialArgs[0]);
			} else {
				result = subtract(polynomialArgs);
			}
			castAndSetResult(result);
			return;
		} else if (funName.equals("/")) {
			final Sort sort = appTerm.getSort();
			final IPolynomialTerm result = divide(sort, polynomialArgs);
			if (result.isErrorTerm()) {
				inputIsNotPolynomial();
				return;
			}
			castAndSetResult(result);
			return;
		} else {
			throw new UnsupportedOperationException("unsupported symbol " + funName);
		}
	}

	/**
	 * Cast the interface IPolynomialTerm in a way that the TermTransformer
	 * accepts the result for "setResult". Execute "setResult" afterwards.
	 */
	private void castAndSetResult(final IPolynomialTerm poly){
		if (poly instanceof PolynomialTerm) {
			setResult((PolynomialTerm) poly);
			return;
		}else if(poly instanceof AffineTerm) {
			setResult((AffineTerm) poly);
			return;
		}
		throw new UnsupportedOperationException("This IPolynomialTerm is instance of no known class.");
	}

	/**
	 * Multiply an array of PolynomialTerms.
	 */
	private static IPolynomialTerm multiply(final Sort sort, final IPolynomialTerm[] polynomialArgs) {
		//TODO: Find out whether passing the sort explicitly in case every polynomialTerm is just a Constant
		//is really necessary (like in the AffineTermTransformer): YES it is in case that the array is empty
		//TODO: Ask Matthias why does AffineTermTransformer not catch this in add?
		if (polynomialArgs.length == 0) {
			return new PolynomialTerm(sort, Rational.ONE, Collections.emptyMap());
		}
		if (polynomialArgs.length == 1) {
			return polynomialArgs[0];
		}

		IPolynomialTerm poly = multiplyTwoPolynomials(polynomialArgs[0], polynomialArgs[1]);
		for (int i = 2; i < polynomialArgs.length; i++) {
			poly = multiplyTwoPolynomials(poly, polynomialArgs[i]);
		}
		return poly;
	}

	/**
	 * Returns the product of poly1 and poly2.
	 */
	private static IPolynomialTerm multiplyTwoPolynomials(final IPolynomialTerm poly1, final IPolynomialTerm poly2) {
		assert poly1.getSort() == poly2.getSort();

		if (productWillBePolynomial(poly1, poly2)) {
			return PolynomialTerm.mulPolynomials(poly1, poly2);
		} else {
			return AffineTerm.mulAffineTerms(poly1, poly2);
		}
	}

	/**
	 * Determines whether the product of two polynomialTerms will be truly polynomial (not affine).
	 * If the result is truly polynomial it returns true, false otherwise.
	 */
	private static boolean productWillBePolynomial(final IPolynomialTerm poly1, final IPolynomialTerm poly2) {
		return !poly1.isAffine() || !poly2.isAffine() || (!poly1.isConstant() && !poly2.isConstant());
	}

	/**
	 * Construct a {@link PolynomialTerm} that is the sum of all inputs.
	 */
	private static IPolynomialTerm add(final IPolynomialTerm[] polynomialArgs) {
		if (someTermIsPolynomial(polynomialArgs)) {
			return PolynomialTerm.sum(polynomialArgs);
		}else {
			return AffineTerm.sum(polynomialArgs);
		}
	}

	/**
	 * Returns true when one of the given Terms is truly polynomial (not representable
	 * by an AffineTerm)
	 */
	private static boolean someTermIsPolynomial(final IPolynomialTerm...polynomialTerms) {
		for (final IPolynomialTerm polynomialTerm: polynomialTerms) {
			if (!polynomialTerm.isAffine()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Construct negation (unary minus).
	 */
	private static IPolynomialTerm negate(final IPolynomialTerm polynomialTerm) {
		if (polynomialTerm.isAffine()) {
			return AffineTerm.mul(polynomialTerm, Rational.MONE);
		}
		return PolynomialTerm.mul(polynomialTerm, Rational.MONE);
	}

	/**
	 * Given {@link PolynomialTerm}s <code>t1,t2,...,tn</code> construct an
	 * {@link PolynomialTerm} that represents the difference <code>t1-t2-...-tn</code>,
	 * i.e., the {@link PolynomialTerm} that is equivalent to
	 * <code>t1-(t2+...+tn)</code>
	 */
	private static IPolynomialTerm subtract(final IPolynomialTerm[] input) {
		assert input.length > 1;
		final IPolynomialTerm[] argumentsForSum = new IPolynomialTerm[input.length];
		// negate all arguments but the first (at position 0)
		argumentsForSum[0] = input[0];
		for (int i = 1; i < argumentsForSum.length; i++) {
			argumentsForSum[i] = negate(input[i]);
		}
		// construct the sum
		return add(argumentsForSum);
	}

	/**
	 * Given {@link PolynomialTerm}s <code>t1,t2,...,tn</code> construct an
	 * {@link PolynomialTerm} that represents the quotient <code>t1/t2/.../tn</code>,
	 * i.e., the {@link PolynomialTerm} that is equivalent to
	 * <code>t1*((1/t2)*...*(1/tn))</code>. Note that the function "/" is only
	 * defined the sort of reals. For integer division we have the function "div"
	 * which is currently not supported by our polynomial terms.
	 */
	private static IPolynomialTerm divide(final Sort sort, final IPolynomialTerm[] polynomialArgs) {
		assert SmtSortUtils.isRealSort(sort);
		if (polynomialArgs.length == 0) {
			return AffineTerm.constructConstant(sort, Rational.ONE);
		}else if (polynomialArgs.length == 1) {
			return polynomialArgs[0];
		}

		//Only Term at position 0 may be not affine.
		if (polynomialArgs[0].isAffine()) {
			return affineDivision(sort, polynomialArgs);
		}else {
			return polynomialDivision(polynomialArgs);
		}
	}

	/**
	 * Returns a PolynomialTerm which represents the quotient of the given arguments (see divide method above).
	 */
	private static IPolynomialTerm polynomialDivision(final IPolynomialTerm[] polynomialTerms) {
		for (int i = 1; i < polynomialTerms.length; i++) {
			if (!polynomialTerms[i].isConstant()) {
				throw new UnsupportedOperationException("Division by Variables not supported!");
			}
		}
		PolynomialTerm inverse = new PolynomialTerm(polynomialTerms[1].getSort(),
													polynomialTerms[1].getConstant().inverse(),
													Collections.emptyMap());
		PolynomialTerm poly = PolynomialTerm.mulPolynomials(polynomialTerms[0], inverse);
		for (int i = 2; i < polynomialTerms.length; i++) {
			inverse = new PolynomialTerm(polynomialTerms[i].getSort(),
					 					 polynomialTerms[i].getConstant().inverse(), 
					 					 Collections.emptyMap());
			poly = PolynomialTerm.mulPolynomials(poly, inverse);
		}
		return poly;
	}

	/**
	 * Returns an AffineTerm which represents the quotient of the given arguments (see divide method above).
	 */
	private static IPolynomialTerm affineDivision(final Sort sort, final IPolynomialTerm[] affineArgs) {
		final IPolynomialTerm affineTerm;
		Rational multiplier;
		if (affineArgs[0].isConstant()) {
			affineTerm = null;
			multiplier = affineArgs[0].getConstant();
		} else {
			affineTerm = affineArgs[0];
			multiplier = Rational.ONE;
		}
		final AffineTerm result;
		for (int i = 1; i < affineArgs.length; i++) {
			if (affineArgs[i].isConstant() && !affineArgs[i].isZero()) {
				multiplier = multiplier.mul(affineArgs[i].getConstant().inverse());
			} else {
				// Only the argument at position 0 may be a non-constant,
				// all other arguments must be literals,
				// divisors must not be zero.
				throw new UnsupportedOperationException("Division by Variables not supported!");
			}
		}
		if (affineTerm == null) {
			result = AffineTerm.constructConstant(sort, multiplier);
		} else {
			result = AffineTerm.mul(affineTerm, multiplier);
		}
		return result;
	}

	//TODO: PolynomialRelation

	/**
	 * Convert an array of {@link Term}s into an an array of {@link PolynomialTerm}s by
	 * casting every single element. In case an element of the input is our
	 * auxiliary error term we return null instead.
	 */
	private static IPolynomialTerm[] castAndCheckForNonPolynomialArguments(final Term[] terms) {
		final IPolynomialTerm[] polynomialTerms = new IPolynomialTerm[terms.length];
		for (int i = 0; i < polynomialTerms.length; i++) {
			if (terms[i] instanceof IPolynomialTerm) {
				polynomialTerms[i] = (IPolynomialTerm) terms[i];
				if (polynomialTerms[i].isErrorTerm()) {
					return null;
				}
			} else {
				throw new AssertionError();
			}
		}
		return polynomialTerms;
	}
}
