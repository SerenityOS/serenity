/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4463802
 * @summary generics: extension of raw not treated as raw.
 * @author gafter
 *
 * @compile  ExtendedRaw2.java
 */

// from library
interface Comparable<T> { }
interface List<E> { }
class Comparator<T> { }

class Collections {
    public static <T extends Object & Comparable<T>>
    int binarySearch(List<T> list, T key) {
        throw new Error();
    }
    public static <T>
    int binarySearch(List<T> list, T key, Comparator<T> c) {
        throw new Error();
    }
}


// user code below
class Record implements Comparable { }
class T {
    public static void main(String[] arg) {
        List records = null;
        Record x = null;
        int result = Collections.binarySearch(records, x);
    }
}
