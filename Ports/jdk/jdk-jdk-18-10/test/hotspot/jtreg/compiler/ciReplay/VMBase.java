/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.ciReplay;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

public class VMBase extends CiReplayBase {

    public static void main(String args[]) {
        new VMBase(args).runTest(/* needCoreDump = */ false);
    }

    public VMBase(String[] args) {
        super(args);
    }

    @Override
    public void testAction() {
        commonTests();
        runVmTests();
        cleanup();
        if (runServer.orElseThrow(() -> new Error("runServer must be set"))
                && WhiteBox.getWhiteBox().getBooleanVMFlag("TieredCompilation")) {
            for (int stop = 1; stop < CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION; stop++) {
                String vmOpt = "-XX:TieredStopAtLevel=" + stop;
                generateReplay(/* need coredump = */ false, vmOpt);
                int replayCompLevel = getCompLevelFromReplay();
                Asserts.assertGTE(stop, replayCompLevel, "Unexpected compLevel in replay");
                positiveTest(vmOpt);
            }
        }
    }
}

