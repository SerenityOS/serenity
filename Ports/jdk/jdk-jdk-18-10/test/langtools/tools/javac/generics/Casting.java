/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4468840
 * @summary generics problem with casting
 * @author gafter
 *
 * @compile  Casting.java
 */

package Casting;

class Test {}

class List<T> {
    <T> T[] toArray(T[] a) { return null; }
}

public class Casting {
    public static void main(String[] args) {
        List<Test> l1 = null;
        List<Test> l2 = (List<Test>)l1;
    }
}

interface Map<K,V> {
    void put(K k, V v);
    V get(Object o);
}
interface Set<T> {
    <T> T[] toArray(T[] a);
}

class Casting2 {
     public static void main(String[] args){
         Map<Integer,Set<Integer>> map = null;
         if (map.get(new Integer(1)) == map.get(new Integer(2))) ;
     }
}
