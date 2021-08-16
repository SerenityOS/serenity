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
 * @test jdk.vm.ci.hotspot.test.HotSpotConstantReflectionProviderTest
 * @requires vm.jvmci
 * @modules jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.misc
 * @library /test/lib /compiler/jvmci/jdk.vm.ci.hotspot.test/src
 * @build jdk.vm.ci.hotspot.test.DummyClass
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.vm.ci.hotspot.test.DummyClass
 * @run testng/othervm/timeout=300 -Xbootclasspath/a:.
 *      -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *      -XX:-UseJVMCICompiler jdk.vm.ci.hotspot.test.HotSpotConstantReflectionProviderTest
 */

package jdk.vm.ci.hotspot.test;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MemoryAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.testng.Assert;
import org.testng.annotations.Test;

import static jdk.vm.ci.hotspot.test.TestHelper.CONSTANT_REFLECTION_PROVIDER;

public class HotSpotConstantReflectionProviderTest {

    @Test(dataProvider = "forObjectDataProvider", dataProviderClass = ForObjectDataProvider.class)
    public void testForObject(Object obj, String expected) {
        JavaConstant jConst = TestHelper.CONSTANT_REFLECTION_PROVIDER.forObject(obj);
        Assert.assertNotNull(jConst,
                        "An instance of JavaConstant returned by" + " \"forObject\" method should not be null");
        Assert.assertEquals(jConst.toString(), expected, "Unexpected result:");
    }

    @Test(dataProvider = "forStringDataProvider", dataProviderClass = ForStringDataProvider.class)
    public void testForString(String string, String expected) {
        JavaConstant jConst = CONSTANT_REFLECTION_PROVIDER.forString(string);
        Assert.assertNotNull(jConst,
                        "An instance of JavaConstant returned by" + " \"forString\" method should not be null");
        Assert.assertEquals(jConst.toString(), expected, "Unexpected result:");
    }

    @Test(dataProvider = "constantEqualsDataProvider", dataProviderClass = ConstantEqualsDataProvider.class)
    public void testConstantEquals(Constant const1, Constant const2, Boolean expected) {
        Assert.assertEquals(CONSTANT_REFLECTION_PROVIDER.constantEquals(const1, const2), expected,
                        "Unexpected result:");
    }

    @Test(dataProvider = "readArrayLengthDataProvider", dataProviderClass = ReadArrayLengthDataProvider.class)
    public void testReadArrayLength(JavaConstant array, Integer expected) {
        Assert.assertEquals(CONSTANT_REFLECTION_PROVIDER.readArrayLength(array), expected,
                        "Unexpected result:");
    }

    @Test(dataProvider = "readArrayElementDataProvider", dataProviderClass = ReadArrayElementDataProvider.class)
    public void testReadArrayElement(JavaConstant array, int index, Object expected) {
        Assert.assertEquals(CONSTANT_REFLECTION_PROVIDER.readArrayElement(array, index), expected,
                        "Unexpected result:");
    }

    @Test(dataProvider = "readFieldValueDataProvider", dataProviderClass = ReadFieldValueDataProvider.class)
    public void testReadFieldValue(ResolvedJavaField field, JavaConstant receiver, JavaConstant expected) {
        JavaConstant actual = CONSTANT_REFLECTION_PROVIDER.readFieldValue(field, receiver);
        Assert.assertEquals(actual == null ? "null" : actual.toString(),
                        expected == null ? "null" : expected.toString(), "Unexpected result:");
    }

    @Test(dataProvider = "readFieldValueNegativeDataProvider", dataProviderClass = ReadFieldValueDataProvider.class, expectedExceptions = {NullPointerException.class})
    public void testNegativeReadFieldValue(ResolvedJavaField field, JavaConstant receiver) {
        CONSTANT_REFLECTION_PROVIDER.readFieldValue(field, receiver);
    }

    @Test(dataProvider = "asJavaTypeDataProvider", dataProviderClass = AsJavaTypeDataProvider.class)
    public void testAsJavaType(JavaConstant constant, String expected) {
        ResolvedJavaType actual = CONSTANT_REFLECTION_PROVIDER.asJavaType(constant);
        Assert.assertEquals(actual == null ? "null" : actual.toJavaName(),
                        expected == null ? "null" : expected,
                        "Unexpected result, wrong type returned:");
    }

    @Test(dataProvider = "boxPrimitiveDataProvider", dataProviderClass = BoxPrimitiveDataProvider.class)
    public void testBoxPrimitive(JavaConstant constant, JavaConstant expected) {
        JavaConstant actual = CONSTANT_REFLECTION_PROVIDER.boxPrimitive(constant);
        Assert.assertEquals(actual, expected, "Unexpected result:");
    }

    @Test(dataProvider = "unboxPrimitiveDataProvider", dataProviderClass = UnboxPrimitiveDataProvider.class)
    public void testUnboxPrimitive(JavaConstant constant, JavaConstant expected) {
        JavaConstant actual = CONSTANT_REFLECTION_PROVIDER.unboxPrimitive(constant);
        Assert.assertEquals(actual, expected, "Unexpected result:");
    }

    @Test
    public void testGetMemoryAccessProvider() {
        MemoryAccessProvider actual = CONSTANT_REFLECTION_PROVIDER.getMemoryAccessProvider();
        Assert.assertNotNull(actual, "Returned MemoryAccessProvider instance should not be null");
    }

    @Test
    public void testGetMethodHandleAccess() {
        MethodHandleAccessProvider actual = CONSTANT_REFLECTION_PROVIDER.getMethodHandleAccess();
        Assert.assertNotNull(actual,
                        "Returned MethodHandleAccessProvider instance should not be null");
    }
}
