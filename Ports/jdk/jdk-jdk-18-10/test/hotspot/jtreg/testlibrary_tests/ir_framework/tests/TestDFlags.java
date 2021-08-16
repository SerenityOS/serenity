/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package ir_framework.tests;

import compiler.lib.ir_framework.Test;
import compiler.lib.ir_framework.TestFramework;
/*
 * @test
 * @requires vm.debug == true & vm.compMode != "Xint" & vm.compiler2.enabled & vm.flagless
 * @summary Sanity test remaining framework property flags.
 * @library /test/lib /
 * @run main/othervm -DFlipC1C2=true ir_framework.tests.TestDFlags
 * @run main/othervm -DExcludeRandom=true ir_framework.tests.TestDFlags
 * @run main/othervm -DVerifyVM=true ir_framework.tests.TestDFlags
 * @run main/othervm -DDumpReplay=true ir_framework.tests.TestDFlags
 * @run main/othervm -DVerbose=true ir_framework.tests.TestDFlags
 * @run main/othervm -DShuffleTests=false ir_framework.tests.TestDFlags
 * @run main/othervm -DReproduce=true ir_framework.tests.TestDFlags
 * @run main/othervm -DReportStdout=true ir_framework.tests.TestDFlags
 * @run main/othervm -DGCAfter=true ir_framework.tests.TestDFlags
 * @run main/othervm -DPrintTimes=true ir_framework.tests.TestDFlags
 * @run main/othervm -DVerifyIR=false ir_framework.tests.TestDFlags
 */

public class TestDFlags {
    public static void main(String[] args) {
        TestFramework.run();
    }

    @Test
    public int c1() {
        return 34;
    }


    @Test
    public void c2() {
        for (int i = 0; i < 100; i++) {
        }
    }

    @Test
    public void c2_2() {
        for (int i = 0; i < 100; i++) {
        }
    }

    @Test
    public void c2_3() {
        for (int i = 0; i < 100; i++) {
        }
    }
}

