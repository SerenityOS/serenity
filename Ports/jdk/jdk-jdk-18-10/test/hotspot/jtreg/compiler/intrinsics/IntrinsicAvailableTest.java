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

/*
 * @test
 * @bug 8130832
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:+UseCRC32Intrinsics
 *                   compiler.intrinsics.IntrinsicAvailableTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:-UseCRC32Intrinsics
 *                   compiler.intrinsics.IntrinsicAvailableTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:ControlIntrinsic=+_updateCRC32
 *                   -XX:-UseCRC32Intrinsics
 *                   compiler.intrinsics.IntrinsicAvailableTest
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:ControlIntrinsic=-_updateCRC32
 *                   -XX:+UseCRC32Intrinsics
 *                   compiler.intrinsics.IntrinsicAvailableTest
 *
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   -XX:ControlIntrinsic=+_updateCRC32
 *                   -XX:+UseCRC32Intrinsics
 *                   compiler.intrinsics.IntrinsicAvailableTest
 */


package compiler.intrinsics;

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Platform;

import java.lang.reflect.Executable;
import java.util.concurrent.Callable;

public class IntrinsicAvailableTest extends CompilerWhiteBoxTest {

    public IntrinsicAvailableTest(IntrinsicAvailableTestTestCase testCase) {
        super(testCase);
    }

    public static class IntrinsicAvailableTestTestCase implements TestCase {

        public String name() {
            return "IntrinsicAvailableTestTestCase";
        }

        public Executable getExecutable() {
            // Using a single method to test the
            // WhiteBox.isIntrinsicAvailable(Executable method, int compLevel)
            // call for the compilation level corresponding to both the C1 and C2
            // compiler keeps the current test simple.
            //
            // The tested method is java.util.zip.CRC32.update(int, int) because
            // both C1 and C2 define an intrinsic for the method and
            // the UseCRC32Intrinsics flag can be used to enable/disable
            // intrinsification of the method in both product and fastdebug
            // builds.
            try {
                return Class.forName("java.util.zip.CRC32").getDeclaredMethod("update", int.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Test bug, method unavailable. " + e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Test bug, class unavailable. " + e);
            }
        }

        public Callable<Integer> getCallable() {
            return null;
        }

        public boolean isOsr() {
            return false;
        }

    }

    protected void checkIntrinsicForCompilationLevel(Executable method, int compLevel) throws Exception {
        boolean intrinsicEnabled = true;
        String controlIntrinsic = getVMOption("ControlIntrinsic", "");

        if (controlIntrinsic.contains("+_updateCRC32")) {
          intrinsicEnabled = true;
        } else if (controlIntrinsic.contains("-_updateCRC32")) {
          intrinsicEnabled = false;
        }

        intrinsicEnabled &= Boolean.valueOf(getVMOption("UseCRC32Intrinsics"));

        boolean intrinsicAvailable = WHITE_BOX.isIntrinsicAvailable(method,
                                                                    compLevel);

        String intrinsicEnabledMessage = intrinsicEnabled ? "enabled" : "disabled";
        String intrinsicAvailableMessage = intrinsicAvailable ? "available" : "not available";

        if (intrinsicEnabled == intrinsicAvailable) {
            System.out.println("Expected result: intrinsic for java.util.zip.CRC32.update() is " +
                               intrinsicEnabledMessage + " and intrinsic is " + intrinsicAvailableMessage +
                               " at compilation level " + compLevel);
        } else {
            throw new RuntimeException("Unexpected result: intrinsic for java.util.zip.CRC32.update() is " +
                                       intrinsicEnabledMessage + " but intrinsic is " + intrinsicAvailableMessage +
                                       " at compilation level " + compLevel);
        }
    }

    public void test() throws Exception {
        Executable intrinsicMethod = testCase.getExecutable();
        if (Platform.isServer() && !Platform.isEmulatedClient() && (TIERED_STOP_AT_LEVEL == COMP_LEVEL_FULL_OPTIMIZATION)) {
            if (TIERED_COMPILATION) {
                checkIntrinsicForCompilationLevel(intrinsicMethod, COMP_LEVEL_SIMPLE);
            }
            // Dont bother check JVMCI compiler - returns false on all intrinsics.
            if (!Boolean.valueOf(getVMOption("UseJVMCICompiler"))) {
                checkIntrinsicForCompilationLevel(intrinsicMethod, COMP_LEVEL_FULL_OPTIMIZATION);
            }
        } else {
            checkIntrinsicForCompilationLevel(intrinsicMethod, COMP_LEVEL_SIMPLE);
        }
    }

    public static void main(String args[]) throws Exception {
        new IntrinsicAvailableTest(new IntrinsicAvailableTestTestCase()).test();
    }
}
