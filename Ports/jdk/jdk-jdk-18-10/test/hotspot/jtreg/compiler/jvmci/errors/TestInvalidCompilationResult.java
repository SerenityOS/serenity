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
 *                   -XX:-UseJVMCICompiler compiler.jvmci.errors.TestInvalidCompilationResult
 */

package compiler.jvmci.errors;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.ConstantReference;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Mark;
import jdk.vm.ci.code.site.Reference;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotConstant;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.VMConstant;
import org.junit.Test;

/**
 * Tests for errors in the code installer.
 */
public class TestInvalidCompilationResult extends CodeInstallerTest {

    private static class InvalidAssumption extends Assumption {
    }

    private static class InvalidVMConstant implements VMConstant {

        public boolean isDefaultForKind() {
            return false;
        }

        public String toValueString() {
            return null;
        }
    }

    private static class InvalidReference extends Reference {

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidAssumption() {
        installEmptyCode(new Site[0], new Assumption[]{new InvalidAssumption()}, new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidAlignment() {
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 7, new DataPatch[0], null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDataPatchInDataSection() {
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{null}, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceInDataSection() {
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{new DataPatch(0, null)}, null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidDataSectionReference() {
        DataSectionReference ref = new DataSectionReference();
        ref.setOffset(0);
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{new DataPatch(0, ref)}, null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidNarrowMethodInDataSection() {
        HotSpotConstant c = (HotSpotConstant) dummyMethod.getEncoding();
        ConstantReference ref = new ConstantReference((VMConstant) c.compress());
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{new DataPatch(0, ref)}, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullConstantInDataSection() {
        ConstantReference ref = new ConstantReference(null);
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{new DataPatch(0, ref)}, null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidConstantInDataSection() {
        ConstantReference ref = new ConstantReference(new InvalidVMConstant());
        installEmptyCode(new Site[0], new Assumption[0], new Comment[0], 16, new DataPatch[]{new DataPatch(0, ref)}, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceInCode() {
        installEmptyCode(new Site[]{new DataPatch(0, null)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullConstantInCode() {
        ConstantReference ref = new ConstantReference(null);
        installEmptyCode(new Site[]{new DataPatch(0, ref)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidConstantInCode() {
        ConstantReference ref = new ConstantReference(new InvalidVMConstant());
        installEmptyCode(new Site[]{new DataPatch(0, ref)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidReference() {
        InvalidReference ref = new InvalidReference();
        installEmptyCode(new Site[]{new DataPatch(0, ref)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testOutOfBoundsDataSectionReference() {
        DataSectionReference ref = new DataSectionReference();
        ref.setOffset(0x1000);
        installEmptyCode(new Site[]{new DataPatch(0, ref)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidMark() {
        installEmptyCode(new Site[]{new Mark(0, new Object())}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidMarkInt() {
        installEmptyCode(new Site[]{new Mark(0, -1)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSite() {
        installEmptyCode(new Site[]{null}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testInfopointMissingDebugInfo() {
        Infopoint info = new Infopoint(0, null, InfopointReason.METHOD_START);
        installEmptyCode(new Site[]{info}, new Assumption[0], new Comment[0], 16, new DataPatch[0], null);
    }

    @Test(expected = JVMCIError.class)
    public void testSafepointMissingDebugInfo() {
        Infopoint info = new Infopoint(0, null, InfopointReason.SAFEPOINT);
        StackSlot deoptRescueSlot = StackSlot.get(null, 0, true);
        installEmptyCode(new Site[]{info}, new Assumption[0], new Comment[0], 16, new DataPatch[0], deoptRescueSlot);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidDeoptRescueSlot() {
        StackSlot deoptRescueSlot = StackSlot.get(null, -1, false);
        installEmptyCode(new Site[]{}, new Assumption[0], new Comment[0], 16, new DataPatch[0], deoptRescueSlot);
    }
}
