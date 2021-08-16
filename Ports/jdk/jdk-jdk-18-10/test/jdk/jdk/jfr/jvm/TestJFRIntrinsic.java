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

/**
 * @test
 * @summary Intrinsic for JFR
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @modules jdk.jfr/jdk.jfr.internal
 *          java.base/jdk.internal.misc
 *          java.management
 *
 * @build sun.hotspot.WhiteBox
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -ea -Xmixed -Xbatch -XX:TieredStopAtLevel=4 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      jdk.jfr.jvm.TestJFRIntrinsic
 *
  * @run main/othervm -Xbootclasspath/a:. -ea -Xmixed -Xbatch -XX:TieredStopAtLevel=1 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      jdk.jfr.jvm.TestJFRIntrinsic
 */

package jdk.jfr.jvm;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;
import jdk.jfr.internal.JVM;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.NMethod;

public class TestJFRIntrinsic {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    public Object eventWriter;

    public static void main(String... args) throws Exception {
        JVM.getJVM().createNativeJFR();
        TestJFRIntrinsic ti = new TestJFRIntrinsic();
        Method classid = TestJFRIntrinsic.class.getDeclaredMethod("getClassIdIntrinsic",  Class.class);
        ti.runIntrinsicTest(classid);
        Method eventWriterMethod = TestJFRIntrinsic.class.getDeclaredMethod("getEventWriterIntrinsic", Class.class);
        ti.runIntrinsicTest(eventWriterMethod);
    }

    public void getClassIdIntrinsic(Class<?> cls) {
        long exp = JVM.getClassId(cls);
        if (exp == 0) {
            throw new RuntimeException("Class id is zero");
        }
    }

    public void getEventWriterIntrinsic(Class<?> cls) {
        Object o = JVM.getEventWriter();
        if (o != null) {
            eventWriter = o;
        }
    }

    void runIntrinsicTest(Method method) throws Exception {
        if (getMaxCompilationLevel() < 1) {
            /* no compiler */
            return;
        }
        /* load it */
        try {
            method.invoke(this, TestJFRIntrinsic.class);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        int[] lvls = getAvailableCompilationLevels();
        for (int i : lvls) {
            if (!WHITE_BOX.enqueueMethodForCompilation(method, i)) {
                throw new RuntimeException("Failed to enqueue method on level: " + i);
            }

            if (WHITE_BOX.isMethodCompiled(method)) {
                NMethod nm = NMethod.get(method, false);
                if (nm.comp_level != i) {
                    throw new RuntimeException("Failed to compile on correct level: " + i);
                }
                System.out.println("Compiled " + method + " on level "  + i);
            }
        }
    }

    /* below is copied from CompilerUtil in hotspot test lib, removed this when it's moved */

    /**
     * Returns available compilation levels
     *
     * @return int array with compilation levels
     */
    public static int[] getAvailableCompilationLevels() {
        if (!WhiteBox.getWhiteBox().getBooleanVMFlag("UseCompiler")) {
            return new int[0];
        }
        if (WhiteBox.getWhiteBox().getBooleanVMFlag("TieredCompilation")) {
            Long flagValue = WhiteBox.getWhiteBox()
                    .getIntxVMFlag("TieredStopAtLevel");
            int maxLevel = flagValue.intValue();
            return IntStream.rangeClosed(1, maxLevel).toArray();
        } else {
            if (Platform.isServer() && !Platform.isEmulatedClient()) {
                return new int[]{4};
            }
            if (Platform.isClient() || Platform.isMinimal() || Platform.isEmulatedClient()) {
                return new int[]{1};
            }
        }
        return new int[0];
    }

    /**
     * Returns maximum compilation level available
     * @return an int value representing maximum compilation level available
     */
    public static int getMaxCompilationLevel() {
        return Arrays.stream(getAvailableCompilationLevels())
                .max()
                .getAsInt();
    }
}
