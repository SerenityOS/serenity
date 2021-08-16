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

package compiler.lib.ir_framework.flag;

import compiler.lib.ir_framework.CompLevel;
import compiler.lib.ir_framework.TestFramework;
import compiler.lib.ir_framework.shared.TestFrameworkException;
import compiler.lib.ir_framework.shared.TestRunException;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class' main method is called from {@link TestFramework} and represents the so-called "flag VM". It uses the
 * Whitebox API to determine the necessary additional flags to run the test VM (e.g. to do IR matching). It returns
 * the flags over the dedicated TestFramework socket.
 */
public class FlagVM {
    public static final String TEST_VM_FLAGS_FILE_PREFIX = "test-vm-flags-pid-";
    public static final String TEST_VM_FLAGS_FILE_POSTFIX = ".log";
    public static final String TEST_VM_FLAGS_DELIMITER = " ";

    private static final String TEST_VM_FLAGS_FILE;
    private static final WhiteBox WHITE_BOX;

    static {
        try {
            WHITE_BOX = WhiteBox.getWhiteBox();
            TEST_VM_FLAGS_FILE = TEST_VM_FLAGS_FILE_PREFIX + ProcessTools.getProcessId() + TEST_VM_FLAGS_FILE_POSTFIX;
        } catch (UnsatisfiedLinkError e) {
            throw new TestFrameworkException("Could not load WhiteBox", e);
        } catch (Exception e) {
            throw new TestFrameworkException("Could not get process id", e);
        }
    }


    private static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    private static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL =
            CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    private static final boolean TEST_C1 = TIERED_COMPILATION
                                           && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < CompLevel.C2.getValue();

    private static final boolean VERBOSE = Boolean.getBoolean("Verbose");
    private static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    private static final boolean EXCLUDE_RANDOM = Boolean.getBoolean("ExcludeRandom");
    private static final boolean FLIP_C1_C2 = Boolean.getBoolean("FlipC1C2");
    private static final boolean REQUESTED_VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"));
    private static final boolean VERIFY_IR = REQUESTED_VERIFY_IR && USE_COMPILER && !EXCLUDE_RANDOM && !FLIP_C1_C2 && !TEST_C1;

    private static String[] getPrintFlags() {
        return new String[] {"-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions"};
    }

    /**
     * Main entry point of the flag VM.
     */
    public static void main(String[] args) {
        String testClassName = args[0];
        if (VERBOSE) {
            System.out.println("FlagVM main() called. Prepare test VM flags to run class " + testClassName);
        }
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (Exception e) {
            throw new TestRunException("Could not find test class " + testClassName, e);
        }
        emitTestVMFlags(prepareTestVmFlags(testClass));
    }

    /**
     * Emit test VM flags to the dedicated test VM flags file to parse them from the TestFramework "driver" VM again
     * which adds them to the test VM.
     */
    private static void emitTestVMFlags(ArrayList<String> flags) {
        try (var bw = Files.newBufferedWriter(Paths.get(TEST_VM_FLAGS_FILE))) {
            bw.write(String.join(TEST_VM_FLAGS_DELIMITER, flags));
        } catch (IOException e) {
            throw new TestFrameworkException("Error while writing to file " + TEST_VM_FLAGS_FILE, e);
        }
    }

    private static ArrayList<String> prepareTestVmFlags(Class<?> testClass) {
        return setupIrVerificationFlags(testClass);
    }

    private static ArrayList<String> setupIrVerificationFlags(Class<?> testClass) {
        ArrayList<String> cmds = new ArrayList<>();
        if (VERIFY_IR) {
            // Add print flags for IR verification
            cmds.addAll(Arrays.asList(getPrintFlags()));
            cmds.add("-XX:+LogCompilation");
            cmds.add("-XX:CompileCommand=log," + testClass.getCanonicalName() + "::*");
            addBoolOptionForClass(cmds, testClass, "PrintIdeal");
            addBoolOptionForClass(cmds, testClass, "PrintOptoAssembly");
            // Always trap for exception throwing to not confuse IR verification
            cmds.add("-XX:-OmitStackTraceInFastThrow");
            cmds.add("-DShouldDoIRVerification=true");
        } else {
            cmds.add("-DShouldDoIRVerification=false");
        }
        return cmds;
    }

    private static void addBoolOptionForClass(ArrayList<String> cmds, Class<?> testClass, String option) {
        cmds.add("-XX:CompileCommand=option," + testClass.getCanonicalName() + "::*,bool," + option + ",true");
    }
}
