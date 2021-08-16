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
 * Given an input object maps to an appropriate output object. A mapper may
 * variously provide a mapping between types, object instances or keys and
 * values or any other form of transformation upon the input.
 *
 * <p/>All mapper implementations are expected to:
 * <ul>
 *  <li>Provide stable results such that for any {@code t} the result of two
 * {@code map} operations are always equivalent. ie.<pre>
 * Foo one = mapper.map(a);
 * Foo two = mapper.map(a);
 *
 * assert one.equals(two) && two.equals(one);
 * </pre></li>
 * <li>Equivalent input objects should map to equivalent output objects. ie.<pre>
 * assert a.equals(b);  // a and b are equivalent
 *
 * Foo x = mapper.map(a);
 * Foo y = mapper.map(b);
 *
 * assert x.equals(y); // their mapped results should be as equivalent.
 * </pre></li>
 * <li>The mapper should not modify the input object in any way that would
 * change the mapping.</li>
 * <li>When used for aggregate operations upon many elements mappers
 * should not assume that the {@code map} operation will be called upon elements
 * in any specific order.</li>
 * </ul>
 *
 * @param <R> the type of output objects from {@code map} operation. May be the
 * @param <T> the type of input objects provided to the {@code map} operation.
 * same type as {@code <T>}.
 */
public interface TMapper<R, T> {

    /**
     * Map the provided input object to an appropriate output object.
     *
     * @param t the input object to be mapped.
     * @return the mapped output object.
     */
    R map(T t);
}
