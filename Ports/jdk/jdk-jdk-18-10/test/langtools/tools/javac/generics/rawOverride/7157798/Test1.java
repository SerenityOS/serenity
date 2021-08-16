/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug      7062745 7157798
 * @summary  Test inheritance of same-name methods from mulitple interfaces
             when the methods have compatible return types
 * @compile  Test1.java
 */

import java.util.*;

interface A { List<Number> getList(); }
interface B { List getList(); }

interface AB extends A, B {} //return type: List<Number>

interface C<T> { List<T> getList(); }

interface BC<T> extends B, C<T> {} //return type: List<T>

interface D { Number m(); }
interface E { Double m(); }

interface DE extends D, E {} //return type: Double

interface F { ArrayList getList(); }
interface G { Collection getList(); }

interface AG extends A, G{}; //return type: List<Number>

interface CF<T> extends C<T>, F {} //return type: ArrayList

interface CG<T> extends C<T>, G {} //return type: List<T>

interface H<T> { Iterable<T> getList(); }

interface CH<T> extends C<T>, H<T> {} //return type: List<T>

interface CFGH<T> extends C<T>, F, G, H<T> {} //return type: ArrayList


class Test1 {

    //raw and typed return types:
    void test(AB ab) {
        Number n = ab.getList().get(1);
    }

    void test(BC<String> bc) {
        String s = bc.getList().get(1);
    }

    void testRaw(BC bc) {
        List list = bc.getList();
    }

    void testWildCard(BC<?> bc) {
        List<?> list = bc.getList();
    }

    <T> void testGeneric(BC<T> bc) {
        T t = bc.getList().get(1);
    }

    //covariant return:
    void test(DE de) {
        Double d = de.m();
    }

    //mixed:
    void test(AG ag) {
        Number n = ag.getList().get(0);
    }

    void test(CF<Integer> cf) {
        ArrayList list = cf.getList();
    }

    void test(CG<String> cg) {
        String s = cg.getList().get(0);
    }

    void test(CH<String> ch) {
        String s = ch.getList().get(0);
    }

    void test(CFGH<Double> cfgh) {
        ArrayList list = cfgh.getList();
    }

    void testWildCard(CFGH<?> cfgh) {
        ArrayList list = cfgh.getList();
    }
}
