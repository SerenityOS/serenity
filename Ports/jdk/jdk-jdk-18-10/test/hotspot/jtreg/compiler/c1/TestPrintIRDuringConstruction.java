/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary load/store elimination will print out instructions without bcis.
 * @bug 8235383
 * @requires vm.debug == true & vm.compiler1.enabled
 * @run main/othervm -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xcomp -XX:+PrintIRDuringConstruction -XX:+Verbose compiler.c1.TestPrintIRDuringConstruction
 */

package compiler.c1;

public class TestPrintIRDuringConstruction {
    static class Dummy {
        public int value;
    }

    static int foo() {
        Dummy obj = new Dummy();       // c1 doesn't have Escape Analysis

        obj.value = 0;                 // dummy store an initial value.
        return obj.value + obj.value;  // redundant load
    }

    public static void main(String[] args) {
        for (int i=0; i<5_000; ++i) {
            foo();
        }
    }
 }
