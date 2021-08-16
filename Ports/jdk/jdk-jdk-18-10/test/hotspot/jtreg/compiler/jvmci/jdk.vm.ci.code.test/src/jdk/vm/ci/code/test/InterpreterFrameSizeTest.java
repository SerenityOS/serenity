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
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.code.site
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 *          jdk.internal.vm.ci/jdk.vm.ci.aarch64
 *          jdk.internal.vm.ci/jdk.vm.ci.amd64
 * @compile CodeInstallationTest.java TestAssembler.java TestHotSpotVMConfig.java amd64/AMD64TestAssembler.java aarch64/AArch64TestAssembler.java
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.code.test.InterpreterFrameSizeTest
 */

package jdk.vm.ci.code.test;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.hotspot.HotSpotCodeCacheProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.junit.Assert;
import org.junit.Test;

public class InterpreterFrameSizeTest extends CodeInstallationTest {

    HotSpotCodeCacheProvider hotspotCodeCache() {
        return (HotSpotCodeCacheProvider) codeCache;
    }

    @Test
    public void testNull() {
        try {
            hotspotCodeCache().interpreterFrameSize(null);
        } catch (NullPointerException npe) {
            // Threw NPE as expected.
            return;
        }
        Assert.fail("expected NullPointerException");
    }

    @Test
    public void test() {
        ResolvedJavaMethod resolvedMethod = metaAccess.lookupJavaMethod(getMethod("testNull"));

        int bci = 0;
        int numLocals = resolvedMethod.getMaxLocals();
        int numStack = 0;
        JavaValue[] values = new JavaValue[numLocals];
        JavaKind[] slotKinds = new JavaKind[numLocals];
        BytecodeFrame frame = new BytecodeFrame(null, resolvedMethod, bci, false, false, values, slotKinds, numLocals, numStack, 0);
        int size = hotspotCodeCache().interpreterFrameSize(frame);
        if (size <= 0) {
            Assert.fail("expected non-zero result");
        }
    }
}
