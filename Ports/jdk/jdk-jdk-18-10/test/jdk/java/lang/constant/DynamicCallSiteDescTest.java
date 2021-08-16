/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodType;
import java.lang.constant.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static java.lang.constant.ConstantDescs.CD_int;
import static java.lang.constant.ConstantDescs.CD_void;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @test
 * @compile DynamicCallSiteDescTest.java
 * @run testng DynamicCallSiteDescTest
 * @summary unit tests for java.lang.constant.DynamicCallSiteDesc
 */

@Test
public class DynamicCallSiteDescTest extends SymbolicDescTest {
    /* note there is no unit test for method resolveCallSiteDesc as it is being tested in another test in this
     * suite, IndyDescTest
     */

    public void testOf() throws ReflectiveOperationException {
        DirectMethodHandleDesc dmh = ConstantDescs.ofCallsiteBootstrap(
                ClassDesc.of("BootstrapAndTarget"),
                "bootstrap",
                ClassDesc.of("java.lang.invoke.CallSite")
        );
        try {
            DynamicCallSiteDesc.of(
                    dmh,
                    "",
                    MethodTypeDesc.ofDescriptor("()I")
            );
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // good
        }

        try {
            DynamicCallSiteDesc.of(
                    null,
                    "getTarget",
                    MethodTypeDesc.ofDescriptor("()I")
            );
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            DynamicCallSiteDesc.of(
                    dmh,
                    null,
                    MethodTypeDesc.ofDescriptor("()I")
            );
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            DynamicCallSiteDesc.of(
                    dmh,
                    "getTarget",
                    null
            );
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            DynamicCallSiteDesc.of(
                    dmh,
                    "getTarget",
                    MethodTypeDesc.ofDescriptor("()I"),
                    null
            );
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }
        try {
            DynamicCallSiteDesc.of(
                    dmh,
                    "getTarget",
                    MethodTypeDesc.ofDescriptor("()I"),
                    new ConstantDesc[]{ null }
            );
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }
    }

    public void testWithArgs() throws ReflectiveOperationException {
        DynamicCallSiteDesc desc = DynamicCallSiteDesc.of(ConstantDescs.ofCallsiteBootstrap(
                ClassDesc.of("BootstrapAndTarget"),
                "bootstrap",
                ClassDesc.of("java.lang.invoke.CallSite")
            ),
            "getTarget",
            MethodTypeDesc.ofDescriptor("()I")
        );

        try {
            desc.withArgs(null);
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            desc.withArgs(new ConstantDesc[]{ null });
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }
    }

    public void testWithNameAndType() throws ReflectiveOperationException {
        DynamicCallSiteDesc desc = DynamicCallSiteDesc.of(ConstantDescs.ofCallsiteBootstrap(
                ClassDesc.of("BootstrapAndTarget"),
                "bootstrap",
                ClassDesc.of("java.lang.invoke.CallSite")
                ),
                "getTarget",
                MethodTypeDesc.ofDescriptor("()I")
        );

        try {
            desc.withNameAndType(null, MethodTypeDesc.ofDescriptor("()I"));
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }

        try {
            desc.withNameAndType("bootstrap", null);
            fail("NullPointerException expected");
        } catch (NullPointerException npe) {
            // good
        }
    }

    public void testAccessorsAndFactories() throws ReflectiveOperationException {
        DynamicCallSiteDesc desc = DynamicCallSiteDesc.of(ConstantDescs.ofCallsiteBootstrap(
                ClassDesc.of("BootstrapAndTarget"),
                "bootstrap",
                ClassDesc.of("java.lang.invoke.CallSite")
                ),
                "_",
                MethodTypeDesc.ofDescriptor("()I")
        );
        assertEquals(desc, DynamicCallSiteDesc.of((DirectMethodHandleDesc)desc.bootstrapMethod(), desc.invocationType()));
        assertEquals(desc, DynamicCallSiteDesc.of((DirectMethodHandleDesc)desc.bootstrapMethod(),
                desc.invocationName(), desc.invocationType()));
        assertEquals(desc, DynamicCallSiteDesc.of((DirectMethodHandleDesc)desc.bootstrapMethod(),
                desc.invocationName(), desc.invocationType(), desc.bootstrapArgs()));
    }
}
