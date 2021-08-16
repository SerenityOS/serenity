/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7192246
 * @summary basic test for default methods
 * @author  Maurizio Cimadamore
 */

import java.util.*;

public class Pos01 {

    interface Mapper<T> {
        T map(T in);
    }

    interface ExtendedList<T> extends List<T> {
        default List<T> testMap(Mapper<T> r) {
            return Pos01.<T>listMapper(this, r);
        }
    }

    static class MyList<E> extends ArrayList<E> implements ExtendedList<E> {}

    public static void main(String[] args) {
       MyList<Integer> l = new MyList<Integer>();
       l.add(1); l.add(2); l.add(3);
       l.testMap((Integer x) -> x * x );
    }

    static <T> List<T> listMapper(List<T> l, Mapper<T> mapper) {
        MyList<T> new_list = new MyList<T>();
        for (T el : l) {
            new_list.add(mapper.map(el));
        }
        return new_list;
    }
}
