/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010303 8062373
 * @summary Graph inference: missing incorporation step causes spurious inference error
 * @compile TargetType69.java
 */
import java.util.*;

class TargetType69 {

    interface Function<X,Y> {
        Y m(X x);
    }

    abstract class TabulationAssertion<T, U> { }

    class GroupedMapAssertion<K, M1 extends Map<K, ?>> extends TabulationAssertion<Integer, M1> {
        GroupedMapAssertion(Function<Integer, K> classifier) { }
    }


    <T, M2 extends Map> void exerciseMapTabulation(Function<T, ? extends M2> collector,
                                                             TabulationAssertion<T, M2> assertion)  { }

    void test(Function<Integer, Integer> classifier, Function<Integer, Map<Integer, List<Integer>>> coll) {
        exerciseMapTabulation(coll, new GroupedMapAssertion<>(classifier));
        exerciseMapTabulation(coll, new GroupedMapAssertion<>(classifier) {});
    }
}
