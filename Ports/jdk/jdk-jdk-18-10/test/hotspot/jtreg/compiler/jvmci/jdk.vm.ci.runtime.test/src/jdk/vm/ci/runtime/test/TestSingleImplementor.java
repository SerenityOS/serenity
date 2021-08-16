/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8238190
 * @summary Verify single interface implementor recording supports diamond-shaped class hierarchies
 * @requires vm.jvmci
 * @library ../../../../../
 * @modules java.base/jdk.internal.reflect
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 *          java.base/jdk.internal.misc
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.TestSingleImplementor
 */

package jdk.vm.ci.runtime.test;

import org.junit.Assert;
import org.junit.Test;

import jdk.vm.ci.meta.ResolvedJavaType;


public class TestSingleImplementor extends TypeUniverse {

    static int SideEffect;

    interface I {
        void foo();
    }

    interface I1 extends I {
    }

    interface I2 extends I {
    }

    static class Impl implements I1, I2 {
        @Override
        public void foo() {
            SideEffect = 42;
        }
    }

    public static void snippetDiamond(I i) {
        i.foo();
    }

    @Test
    public void testDiamondShape() throws Throwable {
        snippetDiamond(new Impl());
        ResolvedJavaType interfaceType = metaAccess.lookupJavaType(I.class);
        ResolvedJavaType implementationType = metaAccess.lookupJavaType(Impl.class);
        Assert.assertEquals(implementationType, interfaceType.getSingleImplementor());
    }

    interface IF1 {
        void foo();
    }

    static class Impl1 implements IF1 {
        @Override
        public void foo() {
            SideEffect = 43;
        }
    }

    public static void snippetRegular(IF1 i) {
        i.foo();
    }

    @Test
    public void testRegularShape() throws Throwable {
        snippetRegular(new Impl1());
        ResolvedJavaType interfaceType = metaAccess.lookupJavaType(IF1.class);
        ResolvedJavaType implementationType = metaAccess.lookupJavaType(Impl1.class);
        Assert.assertEquals(implementationType, interfaceType.getSingleImplementor());
    }

}
