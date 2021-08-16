/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020215
 * @summary Different execution plan when using JIT vs interpreter
 *
 * @run main compiler.escapeAnalysis.Test8020215
 */

package compiler.escapeAnalysis;

import java.util.ArrayList;
import java.util.List;

public class Test8020215 {
    public static class NamedObject {
        public int id;
        public String name;
        public NamedObject(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class NamedObjectList {
        public List<NamedObject> namedObjectList = new ArrayList<NamedObject>();

        public NamedObject getBest(int id) {
            NamedObject bestObject = null;
            for (NamedObject o : namedObjectList) {
                bestObject = id==o.id ? getBetter(bestObject, o) : bestObject;
            }
            return (bestObject != null) ? bestObject : null;
        }

        private static NamedObject getBetter(NamedObject p1, NamedObject p2) {
            return (p1 == null) ? p2 : (p2 == null) ? p1 : (p2.name.compareTo(p1.name) >= 0) ? p2 : p1;
        }
    }

    static void test(NamedObjectList b, int i) {
        NamedObject x = b.getBest(2);
        // test
        if (x == null) {
            throw new RuntimeException("x should never be null here! (i=" + i + ")");
        }
    }

    public static void main(String[] args) {
        // setup
        NamedObjectList b = new NamedObjectList();
        for (int i = 0; i < 10000; i++) {
            b.namedObjectList.add(new NamedObject(1, "2012-12-31"));
        }
        b.namedObjectList.add(new NamedObject(2, "2013-12-31"));

        // execution
        for (int i = 0; i < 12000; i++) {
            test(b, i);
        }
        System.out.println("PASSED");
    }
}
