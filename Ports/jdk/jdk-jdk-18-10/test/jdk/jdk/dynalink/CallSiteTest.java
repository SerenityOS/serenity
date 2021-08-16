/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import static jdk.dynalink.StandardNamespace.PROPERTY;
import static jdk.dynalink.StandardOperation.GET;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.Operation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.support.SimpleRelinkableCallSite;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @run testng CallSiteTest
 */
public class CallSiteTest {
    private static final Operation GET_PROPERTY = GET.withNamespace(PROPERTY);

    @Test
    public void testInitialize() {
        final DynamicLinkerFactory factory = new DynamicLinkerFactory();
        final DynamicLinker linker = factory.createLinker();
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final boolean[] initializeCalled = { Boolean.FALSE };
        linker.link(new SimpleRelinkableCallSite(new CallSiteDescriptor(
            MethodHandles.publicLookup(), GET_PROPERTY.named("DO_NOT_CARE"), mt)) {
                @Override
                public void initialize(final MethodHandle relinkAndInvoke) {
                    initializeCalled[0] = Boolean.TRUE;
                    super.initialize(relinkAndInvoke);
                }
            });

        Assert.assertTrue(initializeCalled[0]);
    }

    @Test
    public void testRelink() {
        final DynamicLinkerFactory factory = new DynamicLinkerFactory();
        final DynamicLinker linker = factory.createLinker();
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final boolean[] relinkCalled = { Boolean.FALSE };
        final CallSite cs = linker.link(new SimpleRelinkableCallSite(new CallSiteDescriptor(
            MethodHandles.publicLookup(), GET_PROPERTY.named("class"), mt)) {
                @Override
                public void relink(final GuardedInvocation guardedInvocation, final MethodHandle relinkAndInvoke) {
                    relinkCalled[0] = Boolean.TRUE;
                    super.relink(guardedInvocation, relinkAndInvoke);
                }
            });

        Assert.assertFalse(relinkCalled[0]);
        try {
            cs.getTarget().invoke(new Object());
        } catch (final Throwable th) {}

        Assert.assertTrue(relinkCalled[0]);
    }

    @Test
    public void testResetAndRelink() {
        final DynamicLinkerFactory factory = new DynamicLinkerFactory();
        factory.setUnstableRelinkThreshold(1);
        final DynamicLinker linker = factory.createLinker();
        final MethodType mt = MethodType.methodType(Object.class, Object.class);
        final boolean[] resetAndRelinkCalled = { Boolean.FALSE };
        final CallSite cs = linker.link(new SimpleRelinkableCallSite(new CallSiteDescriptor(
            MethodHandles.publicLookup(), GET_PROPERTY.named("length"), mt)) {
                @Override
                public void resetAndRelink(final GuardedInvocation guardedInvocation, final MethodHandle relinkAndInvoke) {
                    resetAndRelinkCalled[0] = Boolean.TRUE;
                    super.resetAndRelink(guardedInvocation, relinkAndInvoke);
                }
            });

        Assert.assertFalse(resetAndRelinkCalled[0]);
        try {
            cs.getTarget().invoke(new Object[] {});
        } catch (final Throwable th) {}

        Assert.assertFalse(resetAndRelinkCalled[0]);
        try {
            cs.getTarget().invoke(new ArrayList<Object>());
        } catch (final Throwable th) {}

        Assert.assertTrue(resetAndRelinkCalled[0]);
    }
}
