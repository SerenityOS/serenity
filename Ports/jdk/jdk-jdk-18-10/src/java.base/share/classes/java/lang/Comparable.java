/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;
import java.util.*;

/**
 * This interface imposes a total ordering on the objects of each class that
 * implements it.  This ordering is referred to as the class's <i>natural
 * ordering</i>, and the class's {@code compareTo} method is referred to as
 * its <i>natural comparison method</i>.<p>
 *
 * Lists (and arrays) of objects that implement this interface can be sorted
 * automatically by {@link Collections#sort(List) Collections.sort} (and
 * {@link Arrays#sort(Object[]) Arrays.sort}).  Objects that implement this
 * interface can be used as keys in a {@linkplain SortedMap sorted map} or as
 * elements in a {@linkplain SortedSet sorted set}, without the need to
 * specify a {@linkplain Comparator comparator}.<p>
 *
 * The natural ordering for a class {@code C} is said to be <i>consistent
 * with equals</i> if and only if {@code e1.compareTo(e2) == 0} has
 * the same boolean value as {@code e1.equals(e2)} for every
 * {@code e1} and {@code e2} of class {@code C}.  Note that {@code null}
 * is not an instance of any class, and {@code e.compareTo(null)} should
 * throw a {@code NullPointerException} even though {@code e.equals(null)}
 * returns {@code false}.<p>
 *
 * It is strongly recommended (though not required) that natural orderings be
 * consistent with equals.  This is so because sorted sets (and sorted maps)
 * without explicit comparators behave "strangely" when they are used with
 * elements (or keys) whose natural ordering is inconsistent with equals.  In
 * particular, such a sorted set (or sorted map) violates the general contract
 * for set (or map), which is defined in terms of the {@code equals}
 * method.<p>
 *
 * For example, if one adds two keys {@code a} and {@code b} such that
 * {@code (!a.equals(b) && a.compareTo(b) == 0)} to a sorted
 * set that does not use an explicit comparator, the second {@code add}
 * operation returns false (and the size of the sorted set does not increase)
 * because {@code a} and {@code b} are equivalent from the sorted set's
 * perspective.<p>
 *
 * Virtually all Java core classes that implement {@code Comparable}
 * have natural orderings that are consistent with equals.  One
 * exception is {@link java.math.BigDecimal}, whose {@linkplain
 * java.math.BigDecimal#compareTo natural ordering} equates {@code
 * BigDecimal} objects with equal numerical values and different
 * representations (such as 4.0 and 4.00). For {@link
 * java.math.BigDecimal#equals BigDecimal.equals()} to return true,
 * the representation and numerical value of the two {@code
 * BigDecimal} objects must be the same.<p>
 *
 * For the mathematically inclined, the <i>relation</i> that defines
 * the natural ordering on a given class C is:<pre>{@code
 *       {(x, y) such that x.compareTo(y) <= 0}.
 * }</pre> The <i>quotient</i> for this total order is: <pre>{@code
 *       {(x, y) such that x.compareTo(y) == 0}.
 * }</pre>
 *
 * It follows immediately from the contract for {@code compareTo} that the
 * quotient is an <i>equivalence relation</i> on {@code C}, and that the
 * natural ordering is a <i>total order</i> on {@code C}.  When we say that a
 * class's natural ordering is <i>consistent with equals</i>, we mean that the
 * quotient for the natural ordering is the equivalence relation defined by
 * the class's {@link Object#equals(Object) equals(Object)} method:<pre>
 *     {(x, y) such that x.equals(y)}. </pre><p>
 *
 * In other words, when a class's natural ordering is consistent with
 * equals, the equivalence classes defined by the equivalence relation
 * of the {@code equals} method and the equivalence classes defined by
 * the quotient of the {@code compareTo} method are the same.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/java.base/java/util/package-summary.html#CollectionsFramework">
 * Java Collections Framework</a>.
 *
 * @param <T> the type of objects that this object may be compared to
 *
 * @author  Josh Bloch
 * @see java.util.Comparator
 * @since 1.2
 */
public interface Comparable<T> {
    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @apiNote
     * It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *          is less than, equal to, or greater than the specified object.
     *
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    public int compareTo(T o);
}
