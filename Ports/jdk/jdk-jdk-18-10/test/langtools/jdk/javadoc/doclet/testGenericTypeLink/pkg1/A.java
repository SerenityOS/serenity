/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

import java.util.List;
import java.util.Map;

/**
 * {@link List<String>}
 * {@linkplain List<? extends CharSequence>}
 * {@link #someMethod(ArrayList<Integer>, int)}
 * {@link A#otherMethod(Map<String, StringBuilder>, double)}
 *
 * @see Map<String, ? extends CharSequence>
 * @see Map<String, ? super A<String, ? extends RuntimeException>>
 * @see #someMethod(List<Number>, int)
 * @see #otherMethod(Map<String, ? extends CharSequence>, double)
 */
public class A<T, E extends Exception> {

    /**
     * {@link A<String, A.SomeException>}
     * {@linkplain Map<String, ? extends CharSequence> link to generic type with label}
     *
     * @see A<String, A.SomeException>
     * @see List<String> Link to generic type with label
     */
    static class SomeException extends Exception {}

    /**
     * @param list a list
     * @param i an int
     */
    public void someMethod(List<? extends Number> list, int i) {}

    /**
     * @param list a list
     * @param d a double
     */
    public void otherMethod(Map<String, ?> list, double d) {}

    /**
     * Here's a generic link: {@link A<Object, RuntimeException>.Inner}
     */
    public void overriddenMethod() {}

    /**
     * @see A<String, java.lang.RuntimeException>.Inner
     * @see A<A<String, java.lang.RuntimeException>.Inner, A.SomeException>
     */
    class Inner {}

}


