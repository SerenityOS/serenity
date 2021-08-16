/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242895
 * @summary full loop unroll before EA creates phis between allocs projs and init
 * @run main/othervm -Xbatch -XX:CompileCommand=dontinline,*DataA.m1 compiler.escapeAnalysis.TestIdealAllocShape
 */

package compiler.escapeAnalysis;

public class TestIdealAllocShape {
    static volatile DataA f1;

    static class DataA {
        DataA f1;
        DataA(DataA p1) {
            this.f1 = p1;
        }
        public void m1() {}
    }

    public static DataA test1() {
        DataA l1 = new DataA(null);
        for (int i=0; i<2; i++) {
            try {
                return new DataA(l1); // l1 is a GlobalEscape. // control break.
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static DataA test2() {
        DataA l1 = new DataA(null);
        for (int i=0; i<2; i++) {
            try {
                f1 = new DataA(l1); // l1 is a GlobalEscape.
                break; // control break.
            } catch (Exception e) {
            }
        }
        synchronized(l1) { // elided sync
            l1.m1();
        }
        return null;
    }

    public static void main(String argv[]) {

        for (int i=0; i<20000; i++) {
            TestIdealAllocShape.test1();
        }

        for (int i=0; i<20000; i++) {
            TestIdealAllocShape.test2();
        }
    }
}
