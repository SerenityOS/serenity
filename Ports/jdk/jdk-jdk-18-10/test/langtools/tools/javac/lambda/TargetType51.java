/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8005244
 * @summary Implement overload resolution as per latest spec EDR
 *          smoke test for combinator-like stuck analysis
 * @author  Maurizio Cimadamore
 * @compile TargetType51.java
 */

import java.util.Comparator;

class TargetType51 {

    interface SimpleMapper<T, U> {
       T map(U t);
    }

    interface SimpleList<X> {
        SimpleList<X> sort(Comparator<? super X> c);
    }

    static class Person {
        String getName() { return ""; }
    }

    <T, U extends Comparable<? super U>> Comparator<T> comparing(SimpleMapper<U, T> mapper) {  return null; }

    static class F<U extends Comparable<? super U>, T> {
        F(SimpleMapper<U, T> f) { }
    }

    void testAssignmentContext(SimpleList<Person> list, boolean cond) {
        SimpleList<Person> p1 = list.sort(comparing(Person::getName));
        SimpleList<Person> p2 = list.sort(comparing(x->x.getName()));
        SimpleList<Person> p3 = list.sort(cond ? comparing(Person::getName) : comparing(x->x.getName()));
        SimpleList<Person> p4 = list.sort((cond ? comparing(Person::getName) : comparing(x->x.getName())));
    }

    void testMethodContext(SimpleList<Person> list, boolean cond) {
        testMethodContext(list.sort(comparing(Person::getName)), true);
        testMethodContext(list.sort(comparing(x->x.getName())), true);
        testMethodContext(list.sort(cond ? comparing(Person::getName) : comparing(x->x.getName())), true);
        testMethodContext(list.sort((cond ? comparing(Person::getName) : comparing(x->x.getName()))), true);
    }
}
