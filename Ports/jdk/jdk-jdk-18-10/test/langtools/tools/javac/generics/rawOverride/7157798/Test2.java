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
 * @summary  Test inheritance of same-name methods from multiple interfaces
             when the methods have compatible parameter types and return types
 * @compile  Test2.java
 */

import java.util.*;

interface A { void m(Map map); }
interface B { void m(Map<Number, String> map); }

interface AB extends A, B {} //paramter type: Map<Number, String>

interface C<K, V> { List<V> getList(Map<K, V> map); }
interface D { ArrayList getList(Map map); }

interface CD<K, V> extends C<K, V>, D {} //paramter type: Map<K, V>

interface E<T> { T get(List<?> list); }
interface F<T> { T get(List list); }

interface EF<T1, T2 extends T1> extends E<T1>, F<T2> {} //parameter type: List<?>

class Test2 {

    //compatible parameter types:
    void test(AB ab) {
        ab.m(new HashMap<Number, String>());
    }

    //compatible parameter types and return types:
    void testRaw(CD cd) { //return type: ArrayList
        ArrayList al = cd.getList(new HashMap());
    }

    <K, V> void testGeneric(CD<K, V> cd) { //return type: List<V>
        V v = cd.getList(new HashMap<K, V>()).get(0);
    }

    void test(CD<Number, String> cd) { //return type: List<String>
        String s = cd.getList(new HashMap<Number, String>()).get(0);
    }

    void test(EF<Number, Integer> ef) { //return type: Number
        Number n = ef.get(new ArrayList<Integer>());
    }

    <T, U extends T> void testGeneric(EF<T, U> ef) { //return type: T
        T t = ef.get(new ArrayList<U>());
    }
}
