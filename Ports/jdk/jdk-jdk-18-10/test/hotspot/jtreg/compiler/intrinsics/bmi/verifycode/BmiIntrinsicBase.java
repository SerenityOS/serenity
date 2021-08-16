/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics.bmi.verifycode;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import sun.hotspot.code.NMethod;
import sun.hotspot.cpuinfo.CPUInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class BmiIntrinsicBase extends CompilerWhiteBoxTest {

    protected BmiIntrinsicBase(BmiTestCase testCase) {
        super(testCase);
    }

    public static void verifyTestCase(Function<Method, BmiTestCase> constructor, Method... methods) throws Exception {
        for (Method method : methods) {
            new BmiIntrinsicBase(constructor.apply(method)).test();
        }
    }

    @Override
    protected void test() throws Exception {
        BmiTestCase bmiTestCase;
        if (((BmiTestCase) testCase).getTestCaseX64()) {
            bmiTestCase = (BmiTestCase_x64) testCase;
        } else {
            bmiTestCase = (BmiTestCase) testCase;
        }

        if (!(Platform.isX86() || Platform.isX64())) {
            System.out.println("Unsupported platform, test SKIPPED");
            return;
        }

        if (!Platform.isServer()) {
            throw new Error("TESTBUG: Not server VM");
        }

        if (Platform.isInt()) {
            throw new Error("TESTBUG: test can not be run in interpreter");
        }

        if (!CPUInfo.hasFeature(bmiTestCase.getCpuFlag())) {
            System.out.println("Unsupported hardware, no required CPU flag " + bmiTestCase.getCpuFlag() + " , test SKIPPED");
            return;
        }

        if (!Boolean.valueOf(getVMOption(bmiTestCase.getVMFlag()))) {
            System.out.println("VM flag " + bmiTestCase.getVMFlag() + " disabled, test SKIPPED");
            return;
        }

        System.out.println(testCase.name());

        if (TIERED_COMPILATION && TIERED_STOP_AT_LEVEL != CompilerWhiteBoxTest.COMP_LEVEL_MAX || Platform.isEmulatedClient()) {
            System.out.println("TieredStopAtLevel value (" + TIERED_STOP_AT_LEVEL + ") is too low, test SKIPPED");
            return;
        }
        deoptimize();
        compileAtLevelAndCheck(CompilerWhiteBoxTest.COMP_LEVEL_MAX);
    }

    protected void compileAtLevelAndCheck(int level) {
        WHITE_BOX.enqueueMethodForCompilation(method, level);
        waitBackgroundCompilation();
        checkCompilation(method, level);
        checkEmittedCode(method);
    }

    protected void checkCompilation(Executable executable, int level) {
        if (!WHITE_BOX.isMethodCompiled(executable)) {
            throw new AssertionError("Test bug, expected compilation (level): " + level + ", but not compiled" + WHITE_BOX.isMethodCompilable(executable, level));
        }
        final int compilationLevel = WHITE_BOX.getMethodCompilationLevel(executable);
        if (compilationLevel != level) {
            throw new AssertionError("Test bug, expected compilation (level): " + level + ", but level: " + compilationLevel);
        }
    }

    protected void checkEmittedCode(Executable executable) {
        final byte[] nativeCode = NMethod.get(executable, false).insts;
        final byte[] matchInstrPattern = (((BmiTestCase) testCase).getTestCaseX64() && Platform.isX64()) ? ((BmiTestCase_x64) testCase).getInstrPattern_x64() : ((BmiTestCase) testCase).getInstrPattern();
        if (!((BmiTestCase) testCase).verifyPositive(nativeCode)) {
            throw new AssertionError(testCase.name() + " " + "CPU instructions expected not found in nativeCode: " + Utils.toHexString(nativeCode) + " ---- Expected instrPattern: " +
            Utils.toHexString(matchInstrPattern));
        } else {
            System.out.println("CPU instructions found, PASSED, nativeCode: " + Utils.toHexString(nativeCode) + " ---- Expected instrPattern: " +
            Utils.toHexString(matchInstrPattern));
        }
    }

    abstract static class BmiTestCase implements CompilerWhiteBoxTest.TestCase {
        private final Method method;
        protected byte[] instrMask;
        protected byte[] instrPattern;
        protected boolean isLongOperation;
        protected String cpuFlag = "bmi1";
        protected String vmFlag = "UseBMI1Instructions";

        public BmiTestCase(Method method) {
            this.method = method;
        }

        @Override
        public String name() {
            return method.toGenericString();
        }

        @Override
        public Executable getExecutable() {
            return method;
        }

        @Override
        public Callable<Integer> getCallable() {
            return null;
        }

        @Override
        public boolean isOsr() {
            return false;
        }

        public byte[] getInstrPattern() {
            return instrPattern;
        }

        protected int countCpuInstructions(byte[] nativeCode) {
            return countCpuInstructions(nativeCode, instrMask, instrPattern);
        }

        public static int countCpuInstructions(byte[] nativeCode, byte[] instrMask, byte[] instrPattern) {
            int count = 0;
            int patternSize = Math.min(instrMask.length, instrPattern.length);
            boolean found;
            Asserts.assertGreaterThan(patternSize, 0);
            for (int i = 0, n = nativeCode.length - patternSize; i < n; i++) {
                found = true;
                for (int j = 0; j < patternSize; j++) {
                    if ((nativeCode[i + j] & instrMask[j]) != instrPattern[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    ++count;
                    i += patternSize - 1;
                }
            }
            return count;
        }

        public boolean verifyPositive(byte[] nativeCode) {
            final int cnt = countCpuInstructions(nativeCode);
            if (Platform.isX86()) {
                return cnt >= (isLongOperation ? 2 : 1);
            } else {
                return Platform.isX64() && cnt >= 1;
            }
        }

        protected String getCpuFlag() {
            return cpuFlag;
        }

        protected String getVMFlag() {
            return vmFlag;
        }

        protected boolean getTestCaseX64() {
            return false;
        }
    }

    abstract static class BmiTestCase_x64 extends BmiTestCase {
        protected byte[] instrMask_x64;
        protected byte[] instrPattern_x64;

        protected BmiTestCase_x64(Method method) {
            super(method);
        }

        public byte[] getInstrPattern_x64() {
            return instrPattern_x64;
        }

        protected boolean getTestCaseX64() {
            return true;
        }

        protected int countCpuInstructions(byte[] nativeCode) {
            int cnt = super.countCpuInstructions(nativeCode);
            if (Platform.isX64()) { // on x64 platform the instruction we search for can be encoded in 2 different ways
                cnt += countCpuInstructions(nativeCode, instrMask_x64, instrPattern_x64);
            }
            return cnt;
        }
    }
}
