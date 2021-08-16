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

/*
 * @test
 * @bug 8152341
 * @requires vm.jvmci
 * @library /compiler/jvmci/common/patches /test/lib /compiler/jvmci/jdk.vm.ci.hotspot.test/src
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @build jdk.internal.vm.ci/jdk.vm.ci.hotspot.CompilerToVMHelper
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *      -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *      -XX:-UseJVMCICompiler jdk.vm.ci.hotspot.test.MemoryAccessProviderTest
 */

package jdk.vm.ci.hotspot.test;

import sun.hotspot.WhiteBox;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.runtime.JVMCI;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MemoryAccessProviderTest {
    private static final MemoryAccessProvider PROVIDER = JVMCI.getRuntime().getHostJVMCIBackend().getConstantReflection().getMemoryAccessProvider();

    @Test(dataProvider = "positivePrimitive", dataProviderClass = MemoryAccessProviderData.class)
    public void testPositiveReadPrimitiveConstant(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertEquals(PROVIDER.readPrimitiveConstant(kind, base, offset, bitsCount), expected, "Failed to read constant");
    }

    @Test(dataProvider = "positivePrimitive", dataProviderClass = MemoryAccessProviderData.class, expectedExceptions = {IllegalArgumentException.class})
    public void testReadPrimitiveConstantNullBase(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertNull(PROVIDER.readPrimitiveConstant(kind, null, offset, bitsCount), "Unexpected value for null base");
    }

    @Test(dataProvider = "negative", dataProviderClass = MemoryAccessProviderData.class, expectedExceptions = {IllegalArgumentException.class})
    public void testNegativeReadPrimitiveConstant(JavaKind kind, Constant base) {
        PROVIDER.readPrimitiveConstant(kind, base, 0L, kind == null ? 0 : kind.getByteCount() / 8);
    }

    @Test(dataProvider = "outOfBoundsInstanceFields", dataProviderClass = MemoryAccessProviderData.class)
    public void testReadPrimitiveInstanceFieldOutOfBounds(JavaKind kind, Constant base, Long offset, boolean isOutOfBounds) {
        try {
            PROVIDER.readPrimitiveConstant(kind, base, offset, kind.getByteCount() * 8);
            Assert.assertFalse(isOutOfBounds);
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue(isOutOfBounds, iae.getMessage());
        }
    }

    @Test(dataProvider = "outOfBoundsStaticFields", dataProviderClass = MemoryAccessProviderData.class)
    public void testReadPrimitiveStaticFieldOutOFBounds(JavaKind kind, Constant base, Long offset, boolean isOutOfBounds) {
        try {
            PROVIDER.readPrimitiveConstant(kind, base, offset, kind.getByteCount() * 8);
            Assert.assertFalse(isOutOfBounds);
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue(isOutOfBounds, iae.getMessage());
        }
    }

    @Test(dataProvider = "outOfBoundsObjectArray", dataProviderClass = MemoryAccessProviderData.class)
    public void testReadObjectOutOfBoundsObjectArray(JavaKind kind, Constant base, Long offset, boolean isOutOfBounds) {
        try {
            PROVIDER.readObjectConstant(base, offset);
            Assert.assertFalse(isOutOfBounds);
        } catch (IllegalArgumentException iae) {
            Assert.assertTrue(isOutOfBounds, iae.getMessage());
        }
    }

    @Test(dataProvider = "positiveObject", dataProviderClass = MemoryAccessProviderData.class, expectedExceptions = {IllegalArgumentException.class})
    public void testObjectReadPrimitiveConstant(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        PROVIDER.readPrimitiveConstant(kind, base, 0L, bitsCount);
    }

    @Test(dataProvider = "positivePrimitive", dataProviderClass = MemoryAccessProviderData.class, expectedExceptions = {IllegalArgumentException.class})
    public void testReadPrimitiveConstantLessBits(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        PROVIDER.readPrimitiveConstant(kind, base, offset, bitsCount - 1);
    }

    @Test(dataProvider = "positiveObject", dataProviderClass = MemoryAccessProviderData.class)
    public void testPositiveReadObjectConstant(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertEquals(PROVIDER.readObjectConstant(base, offset), expected, "Unexpected result");
    }

    @Test(dataProvider = "positiveObject", dataProviderClass = MemoryAccessProviderData.class)
    public void testNegativeReadObjectConstantNullBase(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertNull(PROVIDER.readObjectConstant(null, offset), "Unexpected return");
    }

    @Test(dataProvider = "positiveObject", dataProviderClass = MemoryAccessProviderData.class)
    public void testNegativeReadObjectConstantWrongOffset(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertNull(PROVIDER.readObjectConstant(base, offset + 1), "Expected null");
    }

    @Test(dataProvider = "positivePrimitive", dataProviderClass = MemoryAccessProviderData.class, expectedExceptions = {IllegalArgumentException.class})
    public void testNegativeReadObjectConstantPrimitiveBase(JavaKind kind, Constant base, Long offset, Object expected, int bitsCount) {
        Assert.assertNull(PROVIDER.readObjectConstant(base, offset), "Expected null");
    }
}
