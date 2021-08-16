/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 * @requires vm.jvmci
 * @library /test/lib /
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:-UseJVMCICompiler
 *                   compiler.jvmci.compilerToVM.GetExceptionTableTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import jdk.test.lib.Asserts;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GetExceptionTableTest {

    public static final int TRY_CATCH_COUNT = 3;
    public static final int TRY_CATCH_FINALLY_COUNT = 8;
    public static final int TRY_WITH_RESOURCES_COUNT = 5;
    public static final int EMPTY_COUNT = 0;

    public static void main(String[] args) {
        Map<Executable, Integer> testCases = createTestCases();
        testCases.forEach(GetExceptionTableTest::runSanityTest);
    }

    private static Map<Executable, Integer> createTestCases() {
        HashMap<Executable, Integer> methods = new HashMap<>();
        try {
            Class<?> aClass = GetExceptionTableTest.DummyClass.class;
            methods.put(aClass.getMethod("tryCatchDummy"), TRY_CATCH_COUNT);
            methods.put(aClass.getMethod("tryCatchFinallyDummy"),
                    TRY_CATCH_FINALLY_COUNT);
            methods.put(aClass.getMethod("tryWithResourcesDummy"),
                    TRY_WITH_RESOURCES_COUNT);
            methods.put(aClass.getMethod("emptyFunction"), EMPTY_COUNT);
        } catch (NoSuchMethodException e) {
            throw new Error("TEST BUG", e);
        }
        return methods;
    }

    private static void runSanityTest(Executable aMethod,
                                      Integer expectedTableLength) {
        HotSpotResolvedJavaMethod method = CTVMUtilities
                .getResolvedMethod(aMethod);
        int tableLength = CompilerToVMHelper.getExceptionTableLength(method);
        Asserts.assertEQ(tableLength, expectedTableLength, aMethod
                + " incorrect exception table length.");

        long tableStart = CompilerToVMHelper.getExceptionTableStart(method);
        if (tableLength > 0) {
            Asserts.assertNE(tableStart, 0L, aMethod + " exception table starts "
                    + "at 0.");
        }
    }

    private static class DummyClass {
        public static void emptyFunction() {}
        public static void tryCatchDummy() throws Throwable {
            try {
                throw new Exception("Dummy exception");
            } catch (ArithmeticException ex) {
                throw new IOException(ex.getMessage());
            } catch (IOException ex) {
                throw new Exception(ex);
            } catch (Exception ex) {
                throw new Exception(ex);
            }
        }

        public int tryCatchFinallyDummy() {
            // 4 times catch/finally = 8 catch-blocks and finally-blocks
            try {
                throw new Exception("Dummy exception");
            } catch (IndexOutOfBoundsException ex) {
                return 1;
            } catch (ArithmeticException ex) {
                return 2;
            } catch (IOException ex) {
                return 3;
            } catch (Exception ex) {
                return 4;
            } finally {
                return 0;
            }
        }

        public static int tryWithResourcesDummy() throws Throwable {
            try (Socket socket = new Socket()) {
                throw new Exception("Dummy exception");
            } catch (ArithmeticException ex) {
                return 1;
            } catch (IOException ex) {
                return 2;
            } catch (Exception ex) {
                return 3;
            }
        }
    }
}
