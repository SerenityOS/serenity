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

/*
 * @test
 * @bug 8269592
 *
 * @requires vm.jvmci
 *
 * @library / /test/lib
 * @library ../common/patches
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.org.objectweb.asm.tree
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.code.stack
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbatch -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   compiler.jvmci.compilerToVM.IterateFramesNative
 * @run main/othervm -Xcomp -Xbootclasspath/a:.
 *                   -XX:CompileOnly=compiler.jvmci.compilerToVM.IterateFramesNative::callerNative
 *                   -XX:CompileOnly=jdk.vm.ci.hotspot.CompilerToVM::iterateFrames
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -Dcompiler.jvmci.compilerToVM.IterateFramesNative.checkCompiled=true
 *                   compiler.jvmci.compilerToVM.IterateFramesNative
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import jdk.vm.ci.code.stack.InspectedFrameVisitor;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotStackFrameReference;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class IterateFramesNative {
    private static final WhiteBox WB;
    private static final Method NATIVE_METHOD;
    private static final Method ITERATE_FRAMES_METHOD;
    private static final ResolvedJavaMethod NATIVE_METHOD_RESOLVED;
    private static final ResolvedJavaMethod NATIVE_CALLBACK_METHOD_RESOLVED;
    private static final boolean CHECK_COMPILED;

    static {
        Method nativeCallbackMethod;
        WB = WhiteBox.getWhiteBox();
        try {
            NATIVE_METHOD = IterateFramesNative.class.getDeclaredMethod("callerNative",
                    Runnable.class);
            nativeCallbackMethod = IterateFramesNative.class.getDeclaredMethod("testNativeFrameCallback",
                    Helper.class, int.class);
            ITERATE_FRAMES_METHOD = CompilerToVMHelper.CompilerToVMClass().getDeclaredMethod(
                    "iterateFrames",
                    ResolvedJavaMethod[].class,
                    ResolvedJavaMethod[].class,
                    int.class,
                    InspectedFrameVisitor.class);
        } catch (NoSuchMethodException e) {
            throw new Error("Can't get executable for test method", e);
        }
        NATIVE_METHOD_RESOLVED = CTVMUtilities.getResolvedMethod(NATIVE_METHOD);
        NATIVE_CALLBACK_METHOD_RESOLVED = CTVMUtilities.getResolvedMethod(nativeCallbackMethod);
        CHECK_COMPILED = Boolean.getBoolean(
                "compiler.jvmci.compilerToVM.IterateFramesNative.checkCompiled");

        loadNativeLibrary();
    }

    public static void main(String[] args) {
        new IterateFramesNative().test();
    }

    private void test() {
        for (int i = 0; i < CompilerWhiteBoxTest.THRESHOLD + 1; i++) {
            testNativeFrame("someString", i);
        }
    }

    /**
    * Loads native library(libIterateFramesNative.so)
    */
    protected static void loadNativeLibrary() {
        System.loadLibrary("IterateFramesNative");
    }

    public static native void callerNative(Runnable runnable);

    private void testNativeFrame(String str, int iteration) {
        Helper innerHelper = new Helper(str);

        callerNative(() -> testNativeFrameCallback(innerHelper, iteration));

        Asserts.assertEQ(innerHelper.string, NATIVE_METHOD_RESOLVED.getName(),
            "Native frame not found?: " + NATIVE_METHOD_RESOLVED.getName());

        if (CHECK_COMPILED) {
            Asserts.assertTrue(WB.isMethodCompiled(ITERATE_FRAMES_METHOD),
                "Expected native method to be compiled: " + ITERATE_FRAMES_METHOD);
            Asserts.assertTrue(WB.isMethodCompiled(NATIVE_METHOD),
                "Expected native method to be compiled: " + NATIVE_METHOD);
        }
    }

    private void testNativeFrameCallback(Helper helper, int iteration) {
        HotSpotStackFrameReference initialFrame = CompilerToVMHelper.iterateFrames(
            null,
            null,
            0,
            f -> {
                HotSpotStackFrameReference frame = (HotSpotStackFrameReference) f;
                Asserts.assertNotNull(frame, "got null frame for native method");
                return frame;
            });
        Asserts.assertNotNull(initialFrame, "frame must not be null");
        Asserts.assertEQ(initialFrame.getMethod().getName(), "iterateFrames",
            "Expected initial frame method to be CompilerToVM.iterateFrames");

        AtomicInteger frameCounter = new AtomicInteger();
        ResolvedJavaMethod[] methods = new ResolvedJavaMethod[] {NATIVE_METHOD_RESOLVED, NATIVE_CALLBACK_METHOD_RESOLVED};
        CompilerToVMHelper.iterateFrames(
            methods,
            methods,
            0,
            f -> {
                HotSpotStackFrameReference frame = (HotSpotStackFrameReference) f;
                Asserts.assertNotNull(frame, "got null frame for native method");
                int index = frameCounter.getAndIncrement();
                if (index == 0) {
                    Asserts.assertTrue(frame.isMethod(NATIVE_CALLBACK_METHOD_RESOLVED),
                        "unexpected method: " + frame.getMethod().getName());
                } else if (index == 1) {
                    Asserts.assertTrue(frame.isMethod(NATIVE_METHOD_RESOLVED),
                        "unexpected method: " + frame.getMethod().getName());
                    helper.string = frame.getMethod().getName();
                    Asserts.assertFalse(frame.hasVirtualObjects(),
                        "native frames do not have virtual objects");
                    return frame; // stop
                }
                return null;
            });
    }

    private class Helper {
        public String string;

        public Helper(String s) {
            this.string = s;
        }
    }
}
