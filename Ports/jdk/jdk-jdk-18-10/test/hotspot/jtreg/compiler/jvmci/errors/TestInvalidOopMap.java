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
 *             -XX:-UseJVMCICompiler compiler.jvmci.errors.TestInvalidOopMap
 */

package compiler.jvmci.errors;

import jdk.vm.ci.code.BytecodePosition;
import jdk.vm.ci.code.DebugInfo;
import jdk.vm.ci.code.Location;
import jdk.vm.ci.code.ReferenceMap;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Infopoint;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotReferenceMap;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import org.junit.Test;

/**
 * Tests for errors in oop maps.
 */
public class TestInvalidOopMap extends CodeInstallerTest {

    private static class InvalidReferenceMap extends ReferenceMap {
    }

    private void test(ReferenceMap refMap) {
        BytecodePosition pos = new BytecodePosition(null, dummyMethod, 0);
        DebugInfo info = new DebugInfo(pos);
        info.setReferenceMap(refMap);
        installEmptyCode(new Site[]{new Infopoint(0, info, InfopointReason.SAFEPOINT)}, new Assumption[0], new Comment[0], 16, new DataPatch[0], StackSlot.get(null, 0, true));
    }

    @Test(expected = NullPointerException.class)
    public void testMissingReferenceMap() {
        test(null);
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidReferenceMap() {
        test(new InvalidReferenceMap());
    }

    @Test(expected = NullPointerException.class)
    public void testNullOops() {
        test(new HotSpotReferenceMap(null, new Location[0], new int[0], 8));
    }

    @Test(expected = NullPointerException.class)
    public void testNullBase() {
        test(new HotSpotReferenceMap(new Location[0], null, new int[0], 8));
    }

    @Test(expected = NullPointerException.class)
    public void testNullSize() {
        test(new HotSpotReferenceMap(new Location[0], new Location[0], null, 8));
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidLength() {
        test(new HotSpotReferenceMap(new Location[1], new Location[2], new int[3], 8));
    }

    @Test(expected = JVMCIError.class)
    public void testInvalidShortOop() {
        PlatformKind kind = arch.getPlatformKind(JavaKind.Short);
        Register reg = getRegister(kind, 0);

        Location[] oops = new Location[]{Location.register(reg)};
        Location[] base = new Location[]{null};
        int[] size = new int[]{kind.getSizeInBytes()};

        test(new HotSpotReferenceMap(oops, base, size, 8));
    }
}
