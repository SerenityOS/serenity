/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * Determines if the input object matches some criteria.
 *
 * <p>All predicate implementations are expected to:
 * <ul>
 *  <li>Provide stable results such that for any {@code t} the result of two
 * {@code eval} operations are always equivalent. ie.<pre>
 * boolean one = predicate.test(a);
 * boolean two = predicate.test(a);
 *
 * assert one == two;
 * </pre></li>
 * <li>Equivalent input objects should map to equivalent output objects. ie.<pre>
 * assert a.equals(b);  // a and b are equivalent
 *
 * boolean x = predicate.test(a);
 * boolean y = predicate.test(ab;
 *
 * assert x == y; // their test results should be the same.
 * </pre></li>
 * <li>The predicate should not modify the input object in any way that would
 * change the evaluation.</li>
 * <li>When used for aggregate operations upon many elements predicates
 * should not assume that the {@code test} operation will be called upon
 * elements in any specific order.</li>
 * </ul>
 *
 * @param <T> the type of input objects provided to {@code test}.
 */
public interface TPredicate<T> {

    /**
     * Return {@code true} if the input object matches some criteria.
     *
     * @param t the input object.
     * @return {@code true} if the input object matched some criteria.
     */
     boolean test(T t);
}
