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

 /**
  * @test
  * @bug 8189439
  * @summary Parameters type profiling is not performed from aarch64 interpreter
  *
  * @requires os.arch != "arm"
  * @requires vm.flavor == "server" & vm.compMode == "Xmixed" & !vm.emulatedClient & !vm.graal.enabled
  *
  * @comment the test can't be run w/ TieredStopAtLevel < 4
  * @requires vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel == 4
  *
  * @library /test/lib /
  * @build sun.hotspot.WhiteBox
  * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
  * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
  *                   -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
  *                   -XX:CompileThreshold=10000
  *                   -server -XX:-TieredCompilation -XX:TypeProfileLevel=020
  *                    compiler.profiling.TestTypeProfiling
  * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
  *                   -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
  *                   -XX:CompileThreshold=10000
  *                   -server -XX:-TieredCompilation -XX:TypeProfileLevel=200
  *                    compiler.profiling.TestTypeProfiling
  */

package compiler.profiling;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;
import java.lang.reflect.Method;

public class TestTypeProfiling {

    public static int[] mParamTypeCheck(Object in) {
        try {
            return (int[]) in;
        } catch (ClassCastException cce) {
            return null;
        }
    }

    static Object x2(Object src) {
        return src;
    }

    public static int[] mRetTypeCheck(Object in) {
        try {
            Object out = x2(in);
            return (int[]) out;
        } catch (ClassCastException cce) {
            return null;
        }
    }

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int TIERED_STOP_AT_LEVEL = WHITE_BOX.getIntxVMFlag("TieredStopAtLevel").intValue();

    static boolean deoptimize(Method method, Object src_obj) throws Exception {
        for (int i = 0; i < 10; i++) {
            method.invoke(null, src_obj);
            if (!WHITE_BOX.isMethodCompiled(method)) {
                return true;
            }
        }
        return false;
    }

    static public void main(String[] args) throws Exception {
        if (!Platform.isServer() || Platform.isEmulatedClient()) {
            throw new Error("TESTBUG: Not server mode");
        }
        // Only execute if C2 is available
        if (TIERED_STOP_AT_LEVEL != CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION) {
            throw new RuntimeException("please enable C2");
        }

        Method method;
        if (WHITE_BOX.getUintxVMFlag("TypeProfileLevel") == 20) {
            method = TestTypeProfiling.class.getMethod("mRetTypeCheck", Object.class);
        } else
        if (WHITE_BOX.getUintxVMFlag("TypeProfileLevel") == 200) {
            method = TestTypeProfiling.class.getMethod("mParamTypeCheck", Object.class);
        } else {
            throw new RuntimeException("please setup method return/params type profilation: -XX:TypeProfileLevel=020/200");
        }

        int[] src = new int[10];
        Object src_obj = new Object();

        // Warm up & make sure we collect type profiling
        for (int i = 0; i < 20000; i++) {
            mParamTypeCheck(src);
            mRetTypeCheck(src);
        }

        // And make sure the method is compiled by C2
        WHITE_BOX.enqueueMethodForCompilation(method, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(method)) {
            throw new RuntimeException(method.getName() + " is not compiled");
        }

        // should deoptimize for speculative type check
        // Intepreter will also add actual type check trap information into MDO
        // when it throw ClassCastException
        if (!deoptimize(method, src_obj)) {
            throw new RuntimeException(method.getName() + " is not deoptimized");
        }

        // compile again
        // c2 will generate throw instead of uncommon trap because
        // actual type check trap information is present in MDO
        WHITE_BOX.enqueueMethodForCompilation(method, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        if (!WHITE_BOX.isMethodCompiled(method)) {
            throw new RuntimeException(method.getName() + " is not recompiled");
        }

        // this time new parameter type should not force deoptimization
        if (deoptimize(method, src_obj)) {
            throw new RuntimeException(method.getName() + " is deoptimized again");
        }
    }
}
