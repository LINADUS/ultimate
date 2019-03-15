/*
 * Code taken from https://github.com/johspaeth/PathExpression
 * Copyright (C) 2018 Johannes Spaeth
 * Copyright (C) 2018 Fraunhofer IEM, Paderborn, Germany
 * 
 * Copyright (C) 2019 Claus Schätzle (schaetzc@tf.uni-freiburg.de)
 * Copyright (C) 2019 University of Freiburg
 *
 * This file is part of the ULTIMATE Library-PathExpressions plug-in.
 *
 * The ULTIMATE Library-PathExpressions plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE Library-PathExpressions plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Library-PathExpressions plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Library-PathExpressions plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE Library-PathExpressions plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lib.pathexpressions.regex;

import de.uni_freiburg.informatik.ultimate.lib.pathexpressions.IRegex;

public abstract class Regex<V> implements IRegex<V> {
	public static <V> IRegex<V> union(IRegex<V> a, IRegex<V> b) {
		return new Union<V>(a, b).simplify();
	}

	public static <V> IRegex<V> concatenate(IRegex<V> a, IRegex<V> b) {
		return new Concatenate<V>(a, b).simplify();
	}

	public static <V> IRegex<V> reverse(IRegex<V> a) {
		if (a instanceof EmptySet || a instanceof Epsilon || a instanceof Plain)
			return a;
		if (a instanceof Concatenate) {
			Concatenate concatenate = (Concatenate) a;
			return concatenate(reverse(concatenate.getSecond()), reverse(concatenate.getFirst()));
		}
		if (a instanceof Union) {
			Union union = (Union) a;
			return union(reverse(union.getFirst()), reverse(union.getSecond()));
		}
		if (a instanceof Star) {
			Star star = (Star) a;
			return star(reverse(star.getPlain()));
		}
		throw new IllegalStateException("Cannot reverse this regular expression: " + a);
	}

	public static <V> boolean containsEpsilon(IRegex<V> regex) {
		if (regex instanceof Union) {
			Union con = (Union) regex;
			if (containsEpsilon(con.getFirst()))
				return true;
			if (containsEpsilon(con.getSecond()))
				return true;
			return false;
		}
		return regex instanceof Epsilon;
	}

	public static <V> IRegex<V> star(IRegex<V> reg) {
		return new Star<V>(reg).simplify();
	}

	public abstract IRegex<V> simplify();
}