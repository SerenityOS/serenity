/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;

import org.testng.annotations.Test;

import static java.lang.constant.ConstantDescs.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

/**
 * @test
 * @compile IndyDescTest.java
 * @run testng IndyDescTest
 * @summary unit tests for java.lang.constant.IndyDescTest
 */
@Test
public class IndyDescTest {
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type,
                                     Object... args) {
        if (args.length == 0)
            return new ConstantCallSite(MethodHandles.constant(String.class, "Foo"));
        else
            return new ConstantCallSite(MethodHandles.constant(String.class, (String) args[0]));
    }

    public void testIndyDesc() throws Throwable {
        ClassDesc c = ClassDesc.of("IndyDescTest");
        MethodTypeDesc mt = MethodTypeDesc.of(CD_CallSite, CD_MethodHandles_Lookup, CD_String, CD_MethodType, CD_Object.arrayType());
        DirectMethodHandleDesc mh = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, c, "bootstrap", mt);
        DynamicCallSiteDesc csd = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CD_String));
        CallSite cs = csd.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target = cs.getTarget();
        assertEquals("Foo", target.invoke());
        assertEquals("wooga", csd.invocationName());

        mh = ofCallsiteBootstrap(c, "bootstrap", CD_CallSite, CD_Object.arrayType());
        csd = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CD_String));
        cs = csd.resolveCallSiteDesc(MethodHandles.lookup());
        target = cs.getTarget();
        assertEquals("Foo", target.invoke());
        assertEquals("wooga", csd.invocationName());

        DynamicCallSiteDesc csd2 = DynamicCallSiteDesc.of(mh, "foo", MethodTypeDesc.of(CD_String), "Bar");
        CallSite cs2 = csd2.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target2 = cs2.getTarget();
        assertEquals("Bar", target2.invoke());
        assertEquals("foo", csd2.invocationName());

        DynamicCallSiteDesc csd3 = DynamicCallSiteDesc.of(mh, MethodTypeDesc.of(CD_String));
        CallSite cs3 = csd.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target3 = cs3.getTarget();
        assertEquals("Foo", target3.invoke());
        assertEquals("_", csd3.invocationName());

        DynamicCallSiteDesc csd4 = DynamicCallSiteDesc.of(mh, "foo", MethodTypeDesc.of(CD_String)).withArgs("Bar");
        CallSite cs4 = csd4.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target4 = cs4.getTarget();
        assertEquals("Bar", target4.invoke());

        DynamicCallSiteDesc csd5 = DynamicCallSiteDesc.of(mh, MethodTypeDesc.of(CD_String, CD_String))
                .withNameAndType("foo", MethodTypeDesc.of(CD_String)).withArgs("Bar");
        CallSite cs5 = csd5.resolveCallSiteDesc(MethodHandles.lookup());
        MethodHandle target5 = cs5.getTarget();
        assertEquals("Bar", target5.invoke());
        assertEquals("foo", csd5.invocationName());
    }

    public void testEqualsHashToString() throws Throwable {
        ClassDesc c = ClassDesc.of("IndyDescTest");
        MethodTypeDesc mt = MethodTypeDesc.of(CD_CallSite, CD_MethodHandles_Lookup, CD_String, CD_MethodType, CD_Object.arrayType());
        DirectMethodHandleDesc mh = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, c, "bootstrap", mt);

        DynamicCallSiteDesc csd1 = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CD_String));
        DynamicCallSiteDesc csd2 = DynamicCallSiteDesc.of(mh, "wooga", MethodTypeDesc.of(CD_String));
        DynamicCallSiteDesc csd3 = DynamicCallSiteDesc.of(mh, "foo", MethodTypeDesc.of(CD_String));
        assertEquals(csd1, csd2);
        assertEquals(csd1.hashCode(), csd2.hashCode());
        assertNotEquals(csd1, csd3);
        assertNotEquals(csd1.hashCode(), csd3.hashCode());

        assertEquals(csd1.toString(), "DynamicCallSiteDesc[IndyDescTest::bootstrap(wooga/):()String]");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyInvocationName() throws Throwable {
        ClassDesc c = ClassDesc.of("IndyDescTest");
        MethodTypeDesc mt = MethodTypeDesc.of(CD_CallSite, CD_MethodHandles_Lookup, CD_String, CD_MethodType, CD_Object.arrayType());
        DirectMethodHandleDesc mh = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, c, "bootstrap", mt);
        DynamicCallSiteDesc csd1 = DynamicCallSiteDesc.of(mh, "", MethodTypeDesc.of(CD_String));
    }
}
