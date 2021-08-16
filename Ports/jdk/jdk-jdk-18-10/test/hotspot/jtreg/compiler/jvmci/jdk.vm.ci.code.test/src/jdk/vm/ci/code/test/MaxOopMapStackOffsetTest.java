/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.aarch64
 *          jdk.internal.vm.ci/jdk.vm.ci.amd64
 * @compile CodeInstallationTest.java DebugInfoTest.java TestAssembler.java TestHotSpotVMConfig.java amd64/AMD64TestAssembler.java aarch64/AArch64TestAssembler.java
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.code.test.MaxOopMapStackOffsetTest
 */

package jdk.vm.ci.code.test;

import jdk.vm.ci.code.Location;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.common.JVMCIError;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import org.junit.Test;

public class MaxOopMapStackOffsetTest extends DebugInfoTest {

    public static int pass() {
        return 42;
    }

    public static int fail() {
        return 42;
    }

    private void test(String name, int offset) {
        Location location = Location.stack(offset);
               DebugInfoCompiler compiler = (asm, values) -> {
            asm.growFrame(offset);
            Register v = asm.emitLoadInt(0);
            asm.emitIntToStack(v);
            values[0] = JavaConstant.forInt(42);
            return null;
        };
        test(compiler, getMethod(name), 2, new Location[]{location}, new Location[1], new int[]{4}, JavaKind.Int);
    }

    private int maxOffset() {
        return config.maxOopMapStackOffset;
    }

    private int wordSize() {
        return config.heapWordSize;
    }

    @Test(expected = JVMCIError.class)
    public void failTooLargeOffset() {
        // This should throw a JVMCIError during installation because the offset is too large.
        test("fail", maxOffset() + wordSize());
    }

    @Test
    public void passWithLargeOffset() {
        test("pass", maxOffset());
    }
}
