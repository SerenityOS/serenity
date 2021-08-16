/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.mathexact.sanity;

import compiler.testlibrary.intrinsics.Verifier;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Platform;

import java.io.FileOutputStream;
import java.lang.reflect.Executable;
import java.util.Properties;

public abstract class IntrinsicBase extends CompilerWhiteBoxTest {
    protected String useMathExactIntrinsics;

    protected IntrinsicBase(TestCase testCase) {
        super(testCase);
        useMathExactIntrinsics = getVMOption("UseMathExactIntrinsics");
    }

    @Override
    protected void test() throws Exception {
        //java.lang.Math should be loaded to allow a compilation of the methods that use Math's method
        System.out.println("class java.lang.Math should be loaded. Proof: " + Math.class);
        printEnvironmentInfo();
        if (Platform.isInt()) {
            throw new Error("TESTBUG: test can not be run in interpreter");
        }

        int expectedIntrinsicCount = 0;

        if (Platform.isServer() && !Platform.isEmulatedClient()) {
            if (TIERED_COMPILATION) {
                int max_level = TIERED_STOP_AT_LEVEL;
                expectedIntrinsicCount = (max_level == COMP_LEVEL_MAX) ? 1 : 0;
                for (int i = COMP_LEVEL_SIMPLE; i <= max_level; ++i) {
                    deoptimize();
                    compileAtLevel(i);
                }
            } else {
                expectedIntrinsicCount = 1;
                deoptimize();
                compileAtLevel(COMP_LEVEL_MAX);
            }
        } else {
            deoptimize();
            compileAtLevel(COMP_LEVEL_SIMPLE);
        }

        if (!isIntrinsicAvailable()) {
            expectedIntrinsicCount = 0;
        }

        System.out.println("Expected intrinsic count is " + expectedIntrinsicCount + " name " + getIntrinsicId());

        final FileOutputStream out = new FileOutputStream(getVMOption("LogFile") + Verifier.PROPERTY_FILE_SUFFIX);
        Properties expectedProps = new Properties();
        expectedProps.setProperty(Verifier.INTRINSIC_NAME_PROPERTY, getIntrinsicId());
        expectedProps.setProperty(Verifier.INTRINSIC_EXPECTED_COUNT_PROPERTY, String.valueOf(expectedIntrinsicCount));
        expectedProps.store(out, null);

        out.close();
    }

    protected void printEnvironmentInfo() {
        System.out.println("os.arch=" + Platform.getOsArch());
        System.out.println("java.vm.info=" + Platform.vmInfo);
        System.out.println("useMathExactIntrinsics=" + useMathExactIntrinsics);
    }

    protected void compileAtLevel(int level) {
        WHITE_BOX.enqueueMethodForCompilation(method, level);
        waitBackgroundCompilation();
        checkCompilation(method, level);
    }

    protected void checkCompilation(Executable executable, int level) {
        if (!WHITE_BOX.isMethodCompiled(executable)) {
            throw new RuntimeException("Test bug, expected compilation (level): " + level + ", but not compiled");
        }
        final int compilationLevel = WHITE_BOX.getMethodCompilationLevel(executable);
        if (compilationLevel != level) {
            if (!(TIERED_COMPILATION && level == COMP_LEVEL_FULL_PROFILE && compilationLevel == COMP_LEVEL_LIMITED_PROFILE)) { //possible case
                throw new RuntimeException("Test bug, expected compilation (level): " + level + ", but level: " + compilationLevel);
            }
        }
    }

    // An intrinsic is available if:
    // - the intrinsic is enabled (by using the appropriate command-line flag) and
    // - the intrinsic is supported by the VM (i.e., the platform on which the VM is
    //   running provides the instructions necessary for the VM to generate the intrinsic).
    protected abstract boolean isIntrinsicAvailable();

    protected abstract String getIntrinsicId();

    static class IntTest extends IntrinsicBase {

        protected boolean isIntrinsicAvailable; // The tested intrinsic is available on the current platform.

        protected IntTest(MathIntrinsic.IntIntrinsic testCase) {
            super(testCase);
            // Only the C2 compiler intrinsifies exact math methods
            // so check if the intrinsics are available with C2.
            isIntrinsicAvailable = WHITE_BOX.isIntrinsicAvailable(testCase.getTestMethod(),
                                                                  COMP_LEVEL_FULL_OPTIMIZATION);
        }

        @Override
        protected boolean isIntrinsicAvailable() {
            return isIntrinsicAvailable;
        }

        @Override
        protected String getIntrinsicId() {
            return "_" + testCase.name().toLowerCase() + "ExactI";
        }
    }

    static class LongTest extends IntrinsicBase {

        protected boolean isIntrinsicAvailable; // The tested intrinsic is available on the current platform.

        protected LongTest(MathIntrinsic.LongIntrinsic testCase) {
            super(testCase);
            // Only the C2 compiler intrinsifies exact math methods
            // so check if the intrinsics are available with C2.
            isIntrinsicAvailable = WHITE_BOX.isIntrinsicAvailable(testCase.getTestMethod(),
                                                                  COMP_LEVEL_FULL_OPTIMIZATION);
        }

        @Override
        protected boolean isIntrinsicAvailable() {
            return isIntrinsicAvailable;
        }

        @Override
        protected String getIntrinsicId() {
            return "_" + testCase.name().toLowerCase() + "ExactL";
        }
    }
}
