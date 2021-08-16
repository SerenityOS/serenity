/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136421
 *
 * @requires vm.jvmci & vm.compMode == "Xmixed"
 * @requires vm.opt.final.EliminateAllocations == true
 *
 * @comment no "-Xcomp -XX:-TieredCompilation" combination allowed until JDK-8140018 is resolved
 * @requires vm.opt.TieredCompilation == null | vm.opt.TieredCompilation == true
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
 *                   -XX:CompileCommand=exclude,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::check
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame2
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::recurse
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame3
 *                   -XX:+DoEscapeAnalysis -XX:-UseCounterDecay
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.materializeFirst=true
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.invalidate=false
 *                   compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest
 * @run main/othervm -Xbatch -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:CompileCommand=exclude,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::check
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame2
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::recurse
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame3
 *                   -XX:+DoEscapeAnalysis -XX:-UseCounterDecay
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.materializeFirst=false
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.invalidate=false
 *                   compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest
 * @run main/othervm -Xbatch -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:CompileCommand=exclude,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::check
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame2
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::recurse
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame3
 *                   -XX:+DoEscapeAnalysis -XX:-UseCounterDecay
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.materializeFirst=true
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.invalidate=true
 *                   compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest
 * @run main/othervm -Xbatch -Xbootclasspath/a:.
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *                   -XX:CompileCommand=exclude,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::check
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame
 *                   -XX:CompileCommand=dontinline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame2
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::recurse
 *                   -XX:CompileCommand=inline,compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest::testFrame3
 *                   -XX:+DoEscapeAnalysis -XX:-UseCounterDecay
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.materializeFirst=false
 *                   -Dcompiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.invalidate=true
 *                   compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest
 */

package compiler.jvmci.compilerToVM;

import compiler.jvmci.common.CTVMUtilities;
import compiler.testlibrary.CompilerUtils;
import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import jdk.vm.ci.code.stack.InspectedFrame;
import jdk.vm.ci.hotspot.CompilerToVMHelper;
import jdk.vm.ci.hotspot.HotSpotStackFrameReference;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jtreg.SkippedException;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class MaterializeVirtualObjectTest {
    private static final WhiteBox WB;
    private static final boolean INVALIDATE;
    private static final int COMPILE_THRESHOLD;
    private static final Method MATERIALIZED_METHOD;
    private static final Method NOT_MATERIALIZED_METHOD;
    private static final Method FRAME3_METHOD;
    private static final ResolvedJavaMethod MATERIALIZED_RESOLVED;
    private static final ResolvedJavaMethod NOT_MATERIALIZED_RESOLVED;
    private static final ResolvedJavaMethod FRAME2_RESOLVED;
    private static final ResolvedJavaMethod FRAME3_RESOLVED;
    private static final boolean MATERIALIZE_FIRST;

    static {
        Method method1;
        Method method2;
        WB = WhiteBox.getWhiteBox();
        try {
            method1 = MaterializeVirtualObjectTest.class.getDeclaredMethod("testFrame",
                    String.class, int.class);
            method2 = MaterializeVirtualObjectTest.class.getDeclaredMethod("testFrame2",
                    String.class, int.class);
            FRAME3_METHOD = MaterializeVirtualObjectTest.class.getDeclaredMethod("testFrame3",
                    Helper.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new Error("Can't get executable for test method", e);
        }
        ResolvedJavaMethod resolved1;
        resolved1 = CTVMUtilities.getResolvedMethod(method1);
        FRAME2_RESOLVED = CTVMUtilities.getResolvedMethod(method2);
        FRAME3_RESOLVED = CTVMUtilities.getResolvedMethod(FRAME3_METHOD);
        INVALIDATE = Boolean.getBoolean(
                "compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.invalidate");
        COMPILE_THRESHOLD = CompilerWhiteBoxTest.THRESHOLD;
        MATERIALIZE_FIRST = Boolean.getBoolean(
                "compiler.jvmci.compilerToVM.MaterializeVirtualObjectTest.materializeFirst");
        MATERIALIZED_RESOLVED = MATERIALIZE_FIRST ? resolved1 : FRAME2_RESOLVED;
        NOT_MATERIALIZED_RESOLVED = MATERIALIZE_FIRST ? FRAME2_RESOLVED : resolved1;
        MATERIALIZED_METHOD = MATERIALIZE_FIRST ? method1 : method2;
        NOT_MATERIALIZED_METHOD = MATERIALIZE_FIRST ? method2 : method1;
    }

    public static void main(String[] args) {
        int levels[] = CompilerUtils.getAvailableCompilationLevels();
        // we need compilation level 4 to use EscapeAnalysis
        if (levels.length < 1 || levels[levels.length - 1] != 4) {
            throw new SkippedException("Test needs compilation level 4");
        }

        new MaterializeVirtualObjectTest().test();
    }

    private static String getName() {
        return "CASE: invalidate=" + INVALIDATE + ", materializedMethod="
                + (MATERIALIZE_FIRST ? "testFrame" : "testFrame2")
                + ", notMaterializedMethod="
                + (MATERIALIZE_FIRST ? "testFrame2" : "testFrame");
    }

    private void test() {
        System.out.println(getName());
        Asserts.assertFalse(WB.isMethodCompiled(MATERIALIZED_METHOD),
                getName() + " : materialized method is compiled");
        Asserts.assertFalse(WB.isMethodCompiled(NOT_MATERIALIZED_METHOD),
                getName() + " : not materialized method is compiled");
        for (int i = 0; i < CompilerWhiteBoxTest.THRESHOLD; i++) {
            testFrame("someString", i);
        }
        Asserts.assertTrue(WB.isMethodCompiled(MATERIALIZED_METHOD), getName()
                + " : materialized method not compiled");
        Asserts.assertTrue(WB.isMethodCompiled(NOT_MATERIALIZED_METHOD),
                getName() + " : not materialized method not compiled");
        testFrame("someString", /* materialize */ CompilerWhiteBoxTest.THRESHOLD);

        // run second test types
        for (int i = 0; i < CompilerWhiteBoxTest.THRESHOLD; i++) {
            testFrame("someString", i);
        }
        Asserts.assertTrue(WB.isMethodCompiled(MATERIALIZED_METHOD), getName()
                + " : materialized method not compiled");
        Asserts.assertTrue(WB.isMethodCompiled(NOT_MATERIALIZED_METHOD),
                getName() + " : not materialized method not compiled");
        testFrame("someString", /* materialize */ CompilerWhiteBoxTest.THRESHOLD + 1);
    }

    private void testFrame(String str, int iteration) {
        Helper helper = new Helper(str);
        testFrame2(str, iteration);
        Asserts.assertTrue((helper.string != null) && (this != null)
                && (helper != null), String.format("%s : some locals are null", getName()));
    }

    private void testFrame2(String str, int iteration) {
        Helper helper = new Helper(str);
        Helper helper2 = new Helper("bar");
        testFrame3(helper, iteration);
        Asserts.assertTrue((helper.string != null) && (this != null) && helper.string == str
                && (helper != null), String.format("%s : some locals are null", getName()));
        Asserts.assertTrue((helper2.string != null) && (this != null)
                && (helper2 != null), String.format("%s : some locals are null", getName()));
    }

    private void testFrame3(Helper outerHelper, int iteration) {
        Helper innerHelper = new Helper("foo");
        recurse(2, iteration);
        Asserts.assertTrue((innerHelper.string != null) && (this != null)
                && (innerHelper != null), String.format("%s : some locals are null", getName()));
        Asserts.assertTrue((outerHelper.string != null) && (this != null)
                && (outerHelper != null), String.format("%s : some locals are null", getName()));
    }

    private void recurse(int depth, int iteration) {
        if (depth == 0) {
            check(iteration);
        } else {
            Integer s = new Integer(depth);
            recurse(depth - 1, iteration);
            Asserts.assertEQ(s.intValue(), depth,
                    String.format("different values: %s != %s", s.intValue(), depth));
        }
    }

    private void checkStructure(boolean materialize) {
        boolean[] framesSeen = new boolean[2];
        Object[] helpers = new Object[1];
        CompilerToVMHelper.iterateFrames(
            new ResolvedJavaMethod[] {FRAME3_RESOLVED},
            null, /* any */
            0,
            f -> {
                if (!framesSeen[1]) {
                    Asserts.assertTrue(f.isMethod(FRAME3_RESOLVED),
                            "Expected testFrame3 first");
                    framesSeen[1] = true;
                    Asserts.assertTrue(f.getLocal(0) != null, "this should not be null");
                    Asserts.assertTrue(f.getLocal(1) != null, "outerHelper should not be null");
                    Asserts.assertTrue(f.getLocal(3) != null, "innerHelper should not be null");
                    Asserts.assertEQ(((Helper) f.getLocal(3)).string, "foo", "innerHelper.string should be foo");
                    helpers[0] = f.getLocal(1);
                    if (materialize) {
                        f.materializeVirtualObjects(false);
                    }
                    return null; //continue
                } else {
                    Asserts.assertFalse(framesSeen[0], "frame3 can not have been seen");
                    Asserts.assertTrue(f.isMethod(FRAME2_RESOLVED),
                            "Expected testFrame2 second");
                    framesSeen[0] = true;
                    Asserts.assertTrue(f.getLocal(0) != null, "this should not be null");
                    Asserts.assertTrue(f.getLocal(1) != null, "str should not be null");
                    Asserts.assertTrue(f.getLocal(3) != null, "helper should not be null");
                    Asserts.assertTrue(f.getLocal(4) != null, "helper2 should not be null");
                    Asserts.assertEQ(((Helper) f.getLocal(3)).string, f.getLocal(1), "helper.string should be the same as str");
                    Asserts.assertEQ(((Helper) f.getLocal(4)).string, "bar", "helper2.string should be foo");
                    if (!materialize) {
                        Asserts.assertEQ(f.getLocal(3), helpers[0], "helper should be the same as frame3's outerHelper");
                    }
                    return f; // stop
                }
            });
        Asserts.assertTrue(framesSeen[1], "frame3 should have been seen");
        Asserts.assertTrue(framesSeen[0], "frame2 should have been seen");
    }

    private void check(int iteration) {
        // Materialize virtual objects on last invocation
        if (iteration == COMPILE_THRESHOLD) {
            // get frames and check not-null
            HotSpotStackFrameReference materialized = CompilerToVMHelper.iterateFrames(
                new ResolvedJavaMethod[] {MATERIALIZED_RESOLVED},
                null /* any */,
                0,
                f -> (HotSpotStackFrameReference) f);
            Asserts.assertNotNull(materialized, getName()
                    + " : got null frame for materialized method");
            Asserts.assertTrue(materialized.isMethod(MATERIALIZED_RESOLVED),
                "Expected materialized method but got " + materialized);
            InspectedFrame notMaterialized = CompilerToVMHelper.iterateFrames(
                new ResolvedJavaMethod[] {NOT_MATERIALIZED_RESOLVED},
                null /* any */,
                0,
                f -> f);
            Asserts.assertNE(materialized, notMaterialized,
                    "Got same frame pointer for both tested frames");
            Asserts.assertTrue(notMaterialized.isMethod(NOT_MATERIALIZED_RESOLVED),
                "Expected notMaterialized method but got " + notMaterialized);
            Asserts.assertNotNull(notMaterialized, getName()
                    + " : got null frame for not materialized method");
            Asserts.assertTrue(WB.isMethodCompiled(MATERIALIZED_METHOD), getName()
                + " : materialized method not compiled");
            Asserts.assertTrue(WB.isMethodCompiled(NOT_MATERIALIZED_METHOD),
                getName() + " : not materialized method not compiled");
            // check that frames has virtual objects before materialization stage
            Asserts.assertTrue(materialized.hasVirtualObjects(), getName()
                    + ": materialized frame has no virtual object before materialization");
            Asserts.assertTrue(notMaterialized.hasVirtualObjects(), getName()
                    + ": notMaterialized frame has no virtual object before materialization");
            // materialize
            CompilerToVMHelper.materializeVirtualObjects(materialized, INVALIDATE);
            // check that only not materialized frame has virtual objects
            Asserts.assertFalse(materialized.hasVirtualObjects(), getName()
                    + " : materialized has virtual object after materialization");
            Asserts.assertTrue(notMaterialized.hasVirtualObjects(), getName()
                    + " : notMaterialized has no virtual object after materialization");
            // check that materialized frame was deoptimized in case invalidate=true
            Asserts.assertEQ(WB.isMethodCompiled(MATERIALIZED_METHOD), !INVALIDATE, getName()
                    + " : materialized method has unexpected compiled status");
            // check that not materialized frame wasn't deoptimized
            Asserts.assertTrue(WB.isMethodCompiled(NOT_MATERIALIZED_METHOD), getName()
                    + " : not materialized method has unexpected compiled status");
        } else if (iteration == COMPILE_THRESHOLD + 1) {
            checkStructure(false);
            checkStructure(true);
        }
    }

    private class Helper {
        public String string;

        public Helper(String s) {
            this.string = s;
        }
    }
}
