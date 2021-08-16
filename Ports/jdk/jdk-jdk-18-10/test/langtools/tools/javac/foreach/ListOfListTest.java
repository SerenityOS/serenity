/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4915435
 * @summary NullPointerError in Resolve.findMethod() with foreach vs generics
 * @author gafter
 */

import java.util.List;
import java.util.ArrayList;

public class ListOfListTest {

    public static void main(String[] argv) {
        List<List<Integer>> ll = new ArrayList<List<Integer>>();
        List<Integer> il = new ArrayList<Integer>();
        ll.add(il);
        il.add(1);
        int sum = 0;
        for (Integer i : ll.get(0)) {
            sum = identity(sum) + i;
        }

        if (sum != 1) throw new Error(""+sum);

        enumsToo();
    }

    static int identity(Integer i) {
        switch (i) {
        case 0: return 0;
        default: return i;
        }
    }

    enum E { a, b, c, d, e };

    static class Box<T> {
        Box(T t) { this.t = t; }
        T t;
        T get() { return t; }
    }

    static void enumsToo() {
        Box<E> box = new Box<E>(E.c);
        switch (box.get()) {
        case a: throw new Error();
        case c: break;
        default: throw new Error();
        }
        Box<Integer> boxi = new Box<Integer>(12);
        switch (boxi.get()) {
        case 0: throw new Error();
        case 12: break;
        default: throw new Error();
        }
    }
}
