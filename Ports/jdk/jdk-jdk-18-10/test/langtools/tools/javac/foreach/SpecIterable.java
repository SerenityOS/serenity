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
 * @bug 4911157
 * @summary Runtime error because of missing method in iterator
 * @author gafter
 */

import java.util.Iterator;

/* The problem occurs when we have a class that extends a specialized
 * iterator */
public class SpecIterable implements Iterable<String> {
    public static void main(String[] args) {
        int i = 0;
        for (String s : new SpecIterable()) {
            if (i++ == 4) break;
            System.out.println(s);
        }
        System.out.println("passed");
    }

    public Iterator<String> iterator() {
        return new Iterator<String>() {
                public void remove() {}
                public boolean hasNext() { return true; }
                public String next() { return "a"; }
            };
    }
}
