/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import javax.annotation.processing.Generated;

/**
 * Test class for rendering of method signatures. This provides some pathologically
 * complex method signatures that require special handling. Other features are
 * covered by other tests.
 */
public class C {

    /**
     * Annotated constructor.
     */
    @Generated(value = "GeneratedConstructor")
    public C() {}


    public interface F0<T> {
        T apply() throws Exception;
    }

    public static class With8Types<T1, T2, T3, T4, T5, T6, T7, T8> { }


    /**
     * Simple method.
     *
     * @param i param 1
     * @param s param 2
     * @param b param 3
     */
    public static void simpleMethod(int i, String s, boolean b) {}


    /**
     * Annotated method.
     *
     * @param i param 1
     * @param s param 2
     * @param b param 3
     */
    @Generated(
            value = "SomeGeneratedName",
            date = "a date",
            comments = "some comment about the method below")
    public static void annotatedMethod(int i, String s, boolean b) {}


    /**
     * Generic method with eight type args.
     *
     * @param <T1> type 1
     * @param <T2> type 2
     * @param <T3> type 3
     * @param <T4> type 4
     * @param <T5> type 5
     * @param <T6> type 6
     * @param <T7> type 7
     * @param <T8> type 8
     * @param t1 param 1
     * @param t2 param 2
     * @param t3 param 3
     * @param t4 param 4
     * @param t5 param 5
     * @param t6 param 6
     * @param t7 param 7
     * @param t8 param 8
     * @return null
     */
    public static
    <T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable,
            T8 extends AutoCloseable>
    With8Types<T1, T2, T3, T4, T5, T6, T7, T8> bigGenericMethod(
            F0<? extends T1> t1,
            F0<? extends T2> t2,
            F0<? extends T3> t3,
            F0<? extends T4> t4,
            F0<? extends T5> t5,
            F0<? extends T6> t6,
            F0<? extends T7> t7,
            F0<? extends T8> t8)
            throws IllegalArgumentException, IllegalStateException { return null; }


    /**
     * Generic method with eight type args and annotation.
     *
     * @param <T1> type 1
     * @param <T2> type 2
     * @param <T3> type 3
     * @param <T4> type 4
     * @param <T5> type 5
     * @param <T6> type 6
     * @param <T7> type 7
     * @param <T8> type 8
     * @param t1 param 1
     * @param t2 param 2
     * @param t3 param 3
     * @param t4 param 4
     * @param t5 param 5
     * @param t6 param 6
     * @param t7 param 7
     * @param t8 param 8
     * @return null
     */
    @Generated(
            value = "SomeGeneratedName",
            date = "a date",
            comments = "some comment about the method below")
    public static
    <T1 extends AutoCloseable,
            T2 extends AutoCloseable,
            T3 extends AutoCloseable,
            T4 extends AutoCloseable,
            T5 extends AutoCloseable,
            T6 extends AutoCloseable,
            T7 extends AutoCloseable,
            T8 extends AutoCloseable>
    With8Types<T1, T2, T3, T4, T5, T6, T7, T8> bigGenericAnnotatedMethod(
            F0<? extends T1> t1,
            F0<? extends T2> t2,
            F0<? extends T3> t3,
            F0<? extends T4> t4,
            F0<? extends T5> t5,
            F0<? extends T6> t6,
            F0<? extends T7> t7,
            F0<? extends T8> t8)
            throws IllegalArgumentException, IllegalStateException { return null; }

    /**
     * Inner classes with type arguments in enclosing classes.
     *
     * @param i param i
     * @param j param j
     * @return return value
     */
    public Generic<Integer>.Inner nestedGeneric1(Generic<Integer>.Inner i, Generic<C>.Inner j) { return i; }

    /**
     * Inner classes with type arguments in enclosing classes.
     *
     * @param f param f
     * @param g param g
     * @return return value
     */
    public Generic<C.F0<C>>.Inner.Foo nestedGeneric2(Generic<Integer>.Inner.Foo f, Generic<C.F0<C>>.Inner.Foo g) { return g; }

    /**
     * Generic class with multiple inner classes.
     * @param <T> type parameter
     */
    public static class Generic<T> {
        public class Inner {
            T data;
            public class Foo {}
        }
    }
}
