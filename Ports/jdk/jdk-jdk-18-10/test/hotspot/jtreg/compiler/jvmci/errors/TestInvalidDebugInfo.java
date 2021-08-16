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
 * @requires vm.jvmci
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.code.site
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 * @compile CodeInstallerTest.java
 * @run junit/othervm -da:jdk.vm.ci... -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *              -XX:-UseJVMCICompiler compiler.jvmci.errors.TestInvalidDebugInfo
 */

package compiler.jvmci.errors;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.Location;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.VirtualObject;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotReferenceMap;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;
import org.junit.Test;

/**
 * Tests for errors in debug info.
 */
public class TestInvalidDebugInfo extends CodeInstallerTest {

    private static class UnknownJavaValue implements JavaValue {
    }

    private static class TestValueKind extends ValueKind<TestValueKind> {

        TestValueKind(Architecture arch, JavaKind kind) {
            this(arch.getPlatformKind(kind));
        }

        TestValueKind(PlatformKind kind) {
            super(kind);
        }

        @Override
        public TestValueKind changeType(PlatformKind kind) {
            return new TestValueKind(kind);
        }
    }

    private void test(JavaValue[] values, JavaKind[] slotKinds, int locals, int stack, int locks) {
        test(null, values, slotKinds, locals, stack, locks);
    }

    private void test(VirtualObject[] vobj, JavaValue[] values, JavaKind[] slotKinds, int locals, int stack, int locks) {
        test(vobj, values, slotKinds, locals, stack, locks, StackSlot.get(null, 0, true));
    }

    private void test(VirtualObject[] vobj, JavaValue[] values, JavaKind[] slotKinds, int locals, int stack, int locks, StackSlot deoptRescueSlot) {
        BytecodeFrame frame = new BytecodeFrame(null, dummyMethod, 0, false, false, values, slotKinds, locals, stack, locks);
        DebugInfo info = new DebugInfo(frame, vobj);
        info.setReferenceMap(new HotSpotReferenceMap(new Location[0], new Location[0], new int[0], 8));
        installEmptyCode(new Site[]{new Infopoint(0, info, InfopointReason.SAFEPOINT)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], deoptRescueSlot);
    }

    @Test(expected = NullPointerException.class)
    public void testNullValues() {
        test(null, new JavaKind[0], 0, 0, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSlotKinds() {
        test(new JavaValue[0], null, 0, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testMissingDeoptRescueSlot() {
        test(null, new JavaValue[0], new JavaKind[0], 0, 0, 0, null);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedScopeValuesLength() {
        test(new JavaValue[]{JavaConstant.FALSE}, new JavaKind[0], 0, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedScopeSlotKindsLength() {
        test(new JavaValue[0], new JavaKind[]{JavaKind.Boolean}, 0, 0, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullValue() {
        test(new JavaValue[]{null}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSlotKind() {
        test(new JavaValue[]{JavaConstant.INT_0}, new JavaKind[]{null}, 1, 0, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMonitor() {
        test(new JavaValue[]{null}, new JavaKind[0], 0, 0, 1);
    }

    @Test(expected = JVMCIError.class)
    public void testWrongMonitorType() {
        test(new JavaValue[]{JavaConstant.INT_0}, new JavaKind[0], 0, 0, 1);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedIllegalValue() {
        test(new JavaValue[]{Value.ILLEGAL}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedTypeInCPURegister() {
        Register reg = getRegister(arch.getPlatformKind(JavaKind.Int), 0);
        test(new JavaValue[]{reg.asValue()}, new JavaKind[]{JavaKind.Illegal}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedTypeInFloatRegister() {
        Register reg = getRegister(arch.getPlatformKind(JavaKind.Float), 0);
        test(new JavaValue[]{reg.asValue()}, new JavaKind[]{JavaKind.Illegal}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedTypeOnStack() {
        ValueKind<?> kind = new TestValueKind(codeCache.getTarget().arch, JavaKind.Int);
        StackSlot value = StackSlot.get(kind, 8, false);
        test(new JavaValue[]{value}, new JavaKind[]{JavaKind.Illegal}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testWrongConstantType() {
        test(new JavaValue[]{JavaConstant.INT_0}, new JavaKind[]{JavaKind.Object}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnsupportedConstantType() {
        test(new JavaValue[]{JavaConstant.forShort((short) 0)}, new JavaKind[]{JavaKind.Short}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedNull() {
        test(new JavaValue[]{JavaConstant.NULL_POINTER}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedObject() {
        JavaValue wrapped = constantReflection.forObject(this);
        test(new JavaValue[]{wrapped}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnknownJavaValue() {
        test(new JavaValue[]{new UnknownJavaValue()}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testMissingIllegalAfterDouble() {
        test(new JavaValue[]{JavaConstant.DOUBLE_0, JavaConstant.INT_0}, new JavaKind[]{JavaKind.Double, JavaKind.Int}, 2, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidVirtualObjectId() {
        ResolvedJavaType obj = metaAccess.lookupJavaType(Object.class);
        VirtualObject o = VirtualObject.get(obj, 5);
        o.setValues(new JavaValue[0], new JavaKind[0]);

        test(new VirtualObject[]{o}, new JavaValue[0], new JavaKind[0], 0, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testDuplicateVirtualObject() {
        ResolvedJavaType obj = metaAccess.lookupJavaType(Object.class);
        VirtualObject o1 = VirtualObject.get(obj, 0);
        o1.setValues(new JavaValue[0], new JavaKind[0]);

        VirtualObject o2 = VirtualObject.get(obj, 0);
        o2.setValues(new JavaValue[0], new JavaKind[0]);

        test(new VirtualObject[]{o1, o2}, new JavaValue[0], new JavaKind[0], 0, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUnexpectedVirtualObject() {
        ResolvedJavaType obj = metaAccess.lookupJavaType(Object.class);
        VirtualObject o = VirtualObject.get(obj, 0);
        o.setValues(new JavaValue[0], new JavaKind[0]);

        test(new VirtualObject[]{o}, new JavaValue[]{o}, new JavaKind[]{JavaKind.Int}, 1, 0, 0);
    }

    @Test(expected = JVMCIError.class)
    public void testUndefinedVirtualObject() {
        ResolvedJavaType obj = metaAccess.lookupJavaType(Object.class);
        VirtualObject o0 = VirtualObject.get(obj, 0);
        o0.setValues(new JavaValue[0], new JavaKind[0]);

        VirtualObject o1 = VirtualObject.get(obj, 1);
        o1.setValues(new JavaValue[0], new JavaKind[0]);

        test(new VirtualObject[]{o0}, new JavaValue[]{o1}, new JavaKind[]{JavaKind.Object}, 1, 0, 0);
    }
}
