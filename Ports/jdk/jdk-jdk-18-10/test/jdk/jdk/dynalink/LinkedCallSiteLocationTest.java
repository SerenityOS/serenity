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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.DynamicLinker;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.support.SimpleRelinkableCallSite;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @test
 * @run testng LinkedCallSiteLocationTest
 */
public class LinkedCallSiteLocationTest {
    private static final Operation GET_METHOD = StandardOperation.GET.withNamespace(StandardNamespace.METHOD);
    @Test
    public void testLinkedCallSiteLocation() throws Throwable {
        final StackTraceElement[] lastLinked = new StackTraceElement[1];

        final GuardingDynamicLinker testLinker =
                (r, s) -> { lastLinked[0] = DynamicLinker.getLinkedCallSiteLocation(); return null; };

        final DynamicLinkerFactory factory = new DynamicLinkerFactory();
        factory.setPrioritizedLinker(testLinker);
        final DynamicLinker linker = factory.createLinker();
        final SimpleRelinkableCallSite callSite = new SimpleRelinkableCallSite(
                new CallSiteDescriptor(
                        MethodHandles.lookup(),
                        GET_METHOD.named("foo"),
                        MethodType.methodType(void.class, Object.class)));
        linker.link(callSite);

        // Test initial linking
        callSite.dynamicInvoker().invoke(new TestClass1()); final int l1 = getLineNumber();
        assertLocation(lastLinked[0], l1);

        // Test relinking
        callSite.dynamicInvoker().invoke(new TestClass2()); final int l2 = getLineNumber();
        assertLocation(lastLinked[0], l2);
    }

    private void assertLocation(final StackTraceElement frame, final int lineNumber) {
        Assert.assertNotNull(frame);
        Assert.assertEquals(frame.getLineNumber(), lineNumber);
        Assert.assertEquals(frame.getClassName(), this.getClass().getName());
    }

    private static int getLineNumber() {
        return StackWalker.getInstance().walk(s -> s.skip(1).findFirst().get().getLineNumber());
    }

    public static class TestClass1 {
        public void foo() {
        }
    }

    public static class TestClass2 {
        public void foo() {
        }
    }
}
