/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/*SAM types:
         1. An interface that has a single abstract method
         2. Having more than one distinct methods, but only one is "real", the others are overriden public methods in Object - example: Comparator<T>
         3. Having more than one methods due to inheritance, but they have the same signature
         4. Having more than one methods due to inheritance, but one of them has a subsignature of all other methods
                a) parameter types compatible
                b) return type substitutable
                c) thrown type not conflicting with the thrown clause of any other method
                d) mixed up
         5. Type-dependent SAM types
  non-SAM types:
         6. An interface that has a single abstract method, which is also public method in Object
         7. Having more than one methods due to inheritance, and none of them has a subsignature of all other methods
*/

import java.util.List;
import java.util.Collection;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.concurrent.TimeoutException;
import java.io.*;

interface A {int getOldest(List<Number> list);}
interface B {int getOldest(List list);}
interface C {int getOldest(List<?> list);}
interface D {int getOldest(List<Integer> list);}
interface E {int getOldest(Collection<?> collection);}
//Not SAM type, case #7
interface DE extends D, E {}

interface Foo {int getAge(Number n);}
interface Bar {int getAge(Integer i);}
//Not SAM type, case #7
interface FooBar extends Foo, Bar {}

//Not SAM type, case #6
interface Planet {boolean equals(Object o);}

// SAM type interfaces:
// type #2:
//only one abstract non-Ojbect method getAge()
interface Mars<T> extends Planet {int getAge(T t);}
//only one abstract non-Ojbect method increment()
interface Jupiter {
    boolean equals(Object o);
    String toString();
    int increment(int i);
}

// type #3:
interface X {int getTotal(List<String> arg);}
interface Y {int getTotal(List<String> strs);}
//SAM type ([List<String>], int, {})
interface XY extends X, Y {}
//SAM type ([List<String>], int, {})
interface XYZ extends X, Y, XY {}

// type #4 a):
//SAM type ([List], int, {})
interface AB extends A, B {}

// type #4 b):
interface F {Number getValue(String str);}
interface G {Integer getValue(String str);}
interface H {Serializable getValue(String str);}
interface I {Object getValue(String str);}
//SAM type ([String], Integer, {})
interface FGHI extends F, G, H, I {}

interface J {List<Number> getAll(String str);}
interface K {List<?> getAll(String str);}
interface L {List getAll(String str);}
interface M {Collection getAll(String str);}
//SAM type ([String], List<Number>/List, {}) - the return type is flexible to some degree
interface JK extends J, K {}
//SAM type ([String], List<Number>/List, {})
interface JL extends J, L {}
//SAM type ([String], List<Number>/List, {})
interface JKL extends J, K, L {}
//SAM type ([String], List<Number>/List, {})
interface JKLM extends J, K, L, M {}

// type #4 c):
interface N {String getText(File f) throws IOException;}
interface O {String getText(File f) throws FileNotFoundException;}
interface P {String getText(File f) throws NullPointerException;}
//SAM type ([File], String, {FileNotFoundException})
interface NO extends N, O {}
//SAM type ([File], String, {})
interface NOP extends N, O, P {}

interface Boo {int getAge(String s) throws IOException;}
interface Doo {int getAge(String s) throws SQLException;}
//SAM type ([String], int, {})
interface BooDoo extends Boo, Doo {}

// type #4 d):
interface Q {Iterable m(Iterable<String> arg);}
interface R {Iterable<String> m(Iterable arg);}
//SAM type ([Iterable], Iterable<String>/Iterable, {})
interface QR extends Q, R {}

interface U {Collection foo(List<String> arg) throws IOException, SQLTransientException;}
interface V {List<?> foo(List<String> arg) throws EOFException, SQLException, TimeoutException;}
interface W {List<String> foo(List arg) throws Exception;}
//SAM type ([List<String>], List<String>/List, {EOFException, SQLTransientException})
interface UV extends U, V {}
// SAM type ([List], List<String>/List, {EOFException, SQLTransientException})
interface UVW extends U, V, W {}

// type #5:
// Not a SAM because sam-ness depends on instantiation of type-variables
interface Qoo<T> {void m(T arg);}
interface Roo<S extends Number> {void m(S arg);}
interface QooRoo<T1, T2 extends Number, T3> extends Qoo<T1>, Roo<T2> {}
