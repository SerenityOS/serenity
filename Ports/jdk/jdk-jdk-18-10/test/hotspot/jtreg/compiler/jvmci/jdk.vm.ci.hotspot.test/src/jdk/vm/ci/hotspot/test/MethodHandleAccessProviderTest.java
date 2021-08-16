/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8152343
 * @bug 8161068
 * @requires vm.jvmci
 * @library /test/lib /compiler/jvmci/jdk.vm.ci.hotspot.test/src
 * @modules java.base/java.lang.invoke:+open
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot:+open
 * @run testng/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI
 *      -XX:-UseJVMCICompiler jdk.vm.ci.hotspot.test.MethodHandleAccessProviderTest
 */

package jdk.vm.ci.hotspot.test;

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider.IntrinsicMethod;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class MethodHandleAccessProviderTest {
    private static final HotSpotConstantReflectionProvider CONSTANT_REFLECTION = (HotSpotConstantReflectionProvider) JVMCI.getRuntime().getHostJVMCIBackend().getConstantReflection();
    private static final MethodHandleAccessProvider PROVIDER = CONSTANT_REFLECTION.getMethodHandleAccess();
    private static final MetaAccessProvider META_ACCESS = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();

    @Test(dataProvider = "intrinsicsPositive", dataProviderClass = MethodHandleAccessProviderData.class)
    public void testLookupMethodHandleIntrinsic(ResolvedJavaMethod mtd, IntrinsicMethod expected) {
        Assert.assertEquals(expected, PROVIDER.lookupMethodHandleIntrinsic(mtd), "Unexpected intrinsic returned for " + mtd);
    }

    @Test(dataProvider = "intrinsicsNegative", dataProviderClass = MethodHandleAccessProviderData.class)
    public void testLookupMethodHandleIntrinsicNegative(ResolvedJavaMethod mtd) {
        Assert.assertNull(PROVIDER.lookupMethodHandleIntrinsic(mtd), "Expected null return for " + mtd);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testLookupMethodHandleIntrinsicNull() {
        PROVIDER.lookupMethodHandleIntrinsic(null);
    }

    @Test(dataProvider = "invokeBasicPositive", dataProviderClass = MethodHandleAccessProviderData.class)
    public void testResolveInvokeBasicTarget(JavaConstant javaConstantMethodHandle, boolean force, String expected) {
        ResolvedJavaMethod mtd = PROVIDER.resolveInvokeBasicTarget(javaConstantMethodHandle, force);
        Assert.assertTrue(mtd.getName().startsWith(expected), "Unexpected method resolved: " + mtd);
    }

    @Test(dataProvider = "invokeBasicNegative1", dataProviderClass = MethodHandleAccessProviderData.class)
    public void testResolveInvokeBasicTargetNegative1(JavaConstant javaConstantMethodHandle, boolean force) {
        Assert.assertNull(PROVIDER.resolveInvokeBasicTarget(javaConstantMethodHandle, force),
                        "Expected null return for " + javaConstantMethodHandle + " with force=" + force);
    }

    @Test(dataProvider = "invokeBasicNegative2", dataProviderClass = MethodHandleAccessProviderData.class, expectedExceptions = {NullPointerException.class})
    public void testResolveInvokeBasicTargetNegative2(JavaConstant javaConstantMethodHandle, boolean force) {
        PROVIDER.resolveInvokeBasicTarget(javaConstantMethodHandle, force);
    }

    @Test
    public void testResolveLinkToTarget() {
        Method self;
        try {
            self = getClass().getDeclaredMethod("testResolveLinkToTarget");
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: can't find method: " + e, e);
        }
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().unreflect(self);
        } catch (IllegalAccessException e) {
            throw new Error("TESTBUG: can't get MHandle: " + e, e);
        }
        Method internalMemberNameMethod;
        try {
            internalMemberNameMethod = mh.getClass().getDeclaredMethod("internalMemberName");
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: can't find method: " + e, e);
        }
        internalMemberNameMethod.setAccessible(true);
        Object memberName;
        try {
            memberName = internalMemberNameMethod.invoke(mh);
        } catch (ReflectiveOperationException e) {
            throw new Error("TESTBUG: can't invoke internalMemberName method", e);
        }
        JavaConstant jcMemberName = CONSTANT_REFLECTION.forObject(memberName);
        ResolvedJavaMethod mtd = PROVIDER.resolveLinkToTarget(jcMemberName);
        Assert.assertEquals(mtd, META_ACCESS.lookupJavaMethod(self), "Got unexpected method: " + mtd);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void testResolveLinkToTargetNegativeNull() {
        PROVIDER.resolveLinkToTarget(null);
    }

    @Test
    public void testResolveLinkToTargetNegativeNullConstant() {
        Assert.assertNull(PROVIDER.resolveLinkToTarget(JavaConstant.NULL_POINTER), "Expected null return");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testResolveLinkToTargetNegativeWrongConstant() {
        PROVIDER.resolveLinkToTarget(CONSTANT_REFLECTION.forObject("42"));
    }
}
