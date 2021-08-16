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

/*
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *  check overload resolution and target type inference w.r.t. generic methods
 * @author  Maurizio Cimadamore
 * @run main TargetType03
 */
import java.util.*;

public class TargetType03 {

    interface Mapper<X,Y> {
        Y myMap(X a);
    }

    static class MapperList<A> extends ArrayList<A> {
        public <B> List<B> myMap(Mapper<A, B> mapper) {
            ArrayList<B> mappedList = new ArrayList<>();
            for (A elem : this) {
                mappedList.add(mapper.myMap(elem));
            }
            return mappedList;
        };
    }

    public static void main(String[] args) {
        MapperList<Integer> numbers = new MapperList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        numbers.add(4);
        numbers.add(5);
        List<Integer> sqNumbers = numbers.myMap(a -> a * a);
        //check invariants
        if (numbers.size() != sqNumbers.size()) {
            throw new AssertionError();
        }
        for (int i = 0; i < numbers.size() ; i ++) {
            if (sqNumbers.get(i) != Math.pow(numbers.get(i), 2)) {
                throw new AssertionError();
            }
        }
    }
}
