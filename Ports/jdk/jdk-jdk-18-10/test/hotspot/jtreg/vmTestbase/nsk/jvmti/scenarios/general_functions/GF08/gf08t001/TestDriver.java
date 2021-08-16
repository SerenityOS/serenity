/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/scenarios/general_functions/GF08/gf08t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *    This test implements GF08 scenario of test plan for General
 *    Functions:
 *        Do the following:
 *        Run simple java apllication with VM option '-verbose:gc'.
 *        Run the same application with JVMTI agent. The agent should
 *        set JVMTI_VERBOSE_GC with SetVerboseFlag. Check that outputs
 *        in stderr in both runs are equal.
 *    The test agent has a special input parameter 'setVerboseMode'.
 *    When VM runs the test class 'gf08t001' with
 *      '-agentlib:gf08t001=setVerboseMode=yes'
 *    option, then the agent calls SetVerboseFlag with
 *    JVMTI_VERBOSE_GC flag in Onload phase.
 *    The test's script wrapper runs the 'gf08t001' class twice.
 *    First time, with "setVerboseMode=yes" agent mode. Second
 *    time, with "setVerboseMode=no" agent mode and with
 *    "-verbose:gc" VM option. In both cases the output is
 *    searched for 'Pause Full' string, unless ExplicitGCInvokesConcurrent
 *    is enabled and G1 is enabled. If ExplicitGCInvokesConcurrent and
 *    G1 is enabled the test searches for 'GC' string in output.
 *    The test fails if this string is not found in the output.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/native
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI
 *      TestDriver
 */

import sun.hotspot.code.Compiler;
import sun.hotspot.WhiteBox;
import sun.hotspot.gc.GC;

public class TestDriver {
    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        Boolean isExplicitGCInvokesConcurrentOn = wb.getBooleanVMFlag("ExplicitGCInvokesConcurrent");
        boolean isUseG1GCon = GC.G1.isSelected();
        boolean isUseZGCon = GC.Z.isSelected();
        boolean isShenandoahGCon = GC.Shenandoah.isSelected();
        boolean isUseEpsilonGCon = GC.Epsilon.isSelected();

        if (Compiler.isGraalEnabled() &&
            (isUseZGCon || isUseEpsilonGCon || isShenandoahGCon)) {
            return; // Graal does not support these GCs
        }

        String keyPhrase;
        if ((isExplicitGCInvokesConcurrentOn && isUseG1GCon) || isUseZGCon || isShenandoahGCon) {
            keyPhrase = "GC";
        } else {
            keyPhrase = "Pause Full";
        }

        nsk.jvmti.scenarios.general_functions.GF08.gf08t.main(new String[] {
                "gf08t001",
                nsk.jvmti.scenarios.general_functions.GF08.gf08t001.class.getName(),
                "gc",
                keyPhrase});
    }
}

