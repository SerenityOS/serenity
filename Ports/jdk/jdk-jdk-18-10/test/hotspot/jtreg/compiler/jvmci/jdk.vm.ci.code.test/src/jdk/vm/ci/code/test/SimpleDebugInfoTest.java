/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmci
 * @requires vm.simpleArch == "x64" | vm.simpleArch == "aarch64"
 * @library /
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.code.site
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.aarch64
 *          jdk.internal.vm.ci/jdk.vm.ci.amd64
 * @compile CodeInstallationTest.java DebugInfoTest.java TestAssembler.java TestHotSpotVMConfig.java amd64/AMD64TestAssembler.java aarch64/AArch64TestAssembler.java
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.code.test.SimpleDebugInfoTest
 */

package jdk.vm.ci.code.test;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;
import org.junit.Assume;
import org.junit.Test;

public class SimpleDebugInfoTest extends DebugInfoTest {

    public static int intOnStack() {
        return 42;
    }

    private void testIntOnStack(DebugInfoCompiler compiler) {
        test(compiler, getMethod("intOnStack"), 2, JavaKind.Int);
    }

    public static int intInLocal() {
        int local = 42;
        return local;
    }

    public void testIntInLocal(DebugInfoCompiler compiler) {
        test(compiler, getMethod("intInLocal"), 3, JavaKind.Int);
    }

    @Test
    public void testConstInt() {
        DebugInfoCompiler compiler = (asm, values) -> {
            values[0] = JavaConstant.forInt(42);
            return null;
        };
        testIntOnStack(compiler);
        testIntInLocal(compiler);
    }

    @Test
    public void testRegInt() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadInt(42);
            values[0] = reg.asValue(asm.getValueKind(JavaKind.Int));
            return null;
        };
        testIntOnStack(compiler);
        testIntInLocal(compiler);
    }

    @Test
    public void testStackInt() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadInt(42);
            values[0] = asm.emitIntToStack(reg);
            return null;
        };
        testIntOnStack(compiler);
        testIntInLocal(compiler);
    }

    public static float floatOnStack() {
        return 42.0f;
    }

    private void testFloatOnStack(DebugInfoCompiler compiler) {
        test(compiler, getMethod("floatOnStack"), 2, JavaKind.Float);
    }

    public static float floatInLocal() {
        float local = 42.0f;
        return local;
    }

    private void testFloatInLocal(DebugInfoCompiler compiler) {
        test(compiler, getMethod("floatInLocal"), 3, JavaKind.Float);
    }

    @Test
    public void testConstFloat() {
        DebugInfoCompiler compiler = (asm, values) -> {
            values[0] = JavaConstant.forFloat(42.0f);
            return null;
        };
        testFloatOnStack(compiler);
        testFloatInLocal(compiler);
    }

    @Test
    public void testRegFloat() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadFloat(42.0f);
            values[0] = reg.asValue(asm.getValueKind(JavaKind.Float));
            return null;
        };
        testFloatOnStack(compiler);
        testFloatInLocal(compiler);
    }

    @Test
    public void testStackFloat() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadFloat(42.0f);
            values[0] = asm.emitFloatToStack(reg);
            return null;
        };
        testFloatOnStack(compiler);
        testFloatInLocal(compiler);
    }

    public static long longOnStack() {
        return 42;
    }

    private void testLongOnStack(DebugInfoCompiler compiler) {
        test(compiler, getMethod("longOnStack"), 3, JavaKind.Long, JavaKind.Illegal);
    }

    public static long longInLocal() {
        long local = 42;
        return local;
    }

    private void testLongInLocal(DebugInfoCompiler compiler) {
        test(compiler, getMethod("longInLocal"), 4, JavaKind.Long, JavaKind.Illegal);
    }

    @Test
    public void testConstLong() {
        DebugInfoCompiler compiler = (asm, values) -> {
            values[0] = JavaConstant.forLong(42);
            values[1] = Value.ILLEGAL;
            return null;
        };
        testLongOnStack(compiler);
        testLongInLocal(compiler);
    }

    @Test
    public void testRegLong() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadLong(42);
            values[0] = reg.asValue(asm.getValueKind(JavaKind.Long));
            values[1] = Value.ILLEGAL;
            return null;
        };
        testLongOnStack(compiler);
        testLongInLocal(compiler);
    }

    @Test
    public void testStackLong() {
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadLong(42);
            values[0] = asm.emitLongToStack(reg);
            values[1] = Value.ILLEGAL;
            return null;
        };
        testLongOnStack(compiler);
        testLongInLocal(compiler);
    }

    public static Class<?> objectOnStack() {
        return SimpleDebugInfoTest.class;
    }

    private void testObjectOnStack(DebugInfoCompiler compiler) {
        test(compiler, getMethod("objectOnStack"), 2, JavaKind.Object);
    }

    public static Class<?> objectInLocal() {
        Class<?> local = SimpleDebugInfoTest.class;
        return local;
    }

    private void testObjectInLocal(DebugInfoCompiler compiler) {
        test(compiler, getMethod("objectInLocal"), 3, JavaKind.Object);
    }

    @Test
    public void testConstObject() {
        ResolvedJavaType type = metaAccess.lookupJavaType(objectOnStack());
        DebugInfoCompiler compiler = (asm, values) -> {
            values[0] = constantReflection.asJavaClass(type);
            return null;
        };
        testObjectOnStack(compiler);
        testObjectInLocal(compiler);
    }

    @Test
    public void testRegObject() {
        ResolvedJavaType type = metaAccess.lookupJavaType(objectOnStack());
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadPointer((HotSpotConstant) constantReflection.asJavaClass(type));
            values[0] = reg.asValue(asm.getValueKind(JavaKind.Object));
            return null;
        };
        testObjectOnStack(compiler);
        testObjectInLocal(compiler);
    }

    @Test
    public void testStackObject() {
        ResolvedJavaType type = metaAccess.lookupJavaType(objectOnStack());
        DebugInfoCompiler compiler = (asm, values) -> {
            Register reg = asm.emitLoadPointer((HotSpotConstant) constantReflection.asJavaClass(type));
            values[0] = asm.emitPointerToStack(reg);
            return null;
        };
        testObjectOnStack(compiler);
        testObjectInLocal(compiler);
    }

    @Test
    public void testRegNarrowObject() {
        Assume.assumeTrue(config.useCompressedOops);
        ResolvedJavaType type = metaAccess.lookupJavaType(objectOnStack());
        DebugInfoCompiler compiler = (asm, values) -> {
            HotSpotConstant wide = (HotSpotConstant) constantReflection.asJavaClass(type);
            Register reg = asm.emitLoadPointer((HotSpotConstant) wide.compress());
            values[0] = reg.asValue(asm.narrowOopKind);
            return null;
        };
        testObjectOnStack(compiler);
        testObjectInLocal(compiler);
    }

    @Test
    public void testStackNarrowObject() {
        Assume.assumeTrue(config.useCompressedOops);
        ResolvedJavaType type = metaAccess.lookupJavaType(objectOnStack());
        DebugInfoCompiler compiler = (asm, values) -> {
            HotSpotConstant wide = (HotSpotConstant) constantReflection.asJavaClass(type);
            Register reg = asm.emitLoadPointer((HotSpotConstant) wide.compress());
            values[0] = asm.emitNarrowPointerToStack(reg);
            return null;
        };
        testObjectOnStack(compiler);
        testObjectInLocal(compiler);
    }
}
