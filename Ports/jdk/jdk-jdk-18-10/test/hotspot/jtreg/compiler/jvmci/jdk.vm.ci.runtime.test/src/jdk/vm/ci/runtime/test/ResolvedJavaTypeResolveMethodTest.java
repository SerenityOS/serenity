/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler jdk.vm.ci.runtime.test.ResolvedJavaTypeResolveMethodTest
 */

package jdk.vm.ci.runtime.test;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.runtime.JVMCI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class ResolvedJavaTypeResolveMethodTest {
    public final MetaAccessProvider metaAccess;

    public ResolvedJavaTypeResolveMethodTest() {
        metaAccess = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
    }

    protected abstract static class A {
        @SuppressWarnings("unused")
        private void priv() {
        }

        public void v1() {
        }

        public void v2() {
        }

        public abstract void abs();
    }

    protected static class B extends A implements I {
        @Override
        public void i() {
        }

        @Override
        public void v2() {
        }

        @Override
        public void abs() {

        }
    }

    protected static class C extends B {
        @Override
        public void d() {
        }
    }

    protected abstract static class D extends A {

    }

    protected static class E extends D {
        @Override
        public void abs() {
        }
    }

    protected interface I {
        void i();

        default void d() {
        }
    }

    @Test
    public void testDefaultMethod() {
        ResolvedJavaType i = getType(I.class);
        ResolvedJavaType b = getType(B.class);
        ResolvedJavaType c = getType(C.class);
        ResolvedJavaMethod di = getMethod(i, "d");
        ResolvedJavaMethod dc = getMethod(c, "d");

        assertEquals(null, i.resolveMethod(di, c));
        assertEquals(di, b.resolveMethod(di, c));
        assertEquals(dc, c.resolveMethod(di, c));
    }

    @Test
    public void testPrivateMethod() {
        ResolvedJavaType a = getType(A.class);
        ResolvedJavaType b = getType(B.class);
        ResolvedJavaType c = getType(C.class);
        ResolvedJavaMethod priv = getMethod(a, "priv");

        // nestmates have access to private methods
        assertNotNull(a.resolveMethod(priv, c));
        assertNotNull(b.resolveMethod(priv, c));
    }

    @Test
    public void testAbstractMethod() {
        ResolvedJavaType a = getType(A.class);
        ResolvedJavaType b = getType(B.class);
        ResolvedJavaType c = getType(C.class);
        ResolvedJavaType d = getType(D.class);
        ResolvedJavaType e = getType(E.class);
        ResolvedJavaMethod absa = getMethod(a, "abs");
        ResolvedJavaMethod absb = getMethod(b, "abs");
        ResolvedJavaMethod abse = getMethod(e, "abs");

        assertEquals(absa, a.resolveMethod(absa, c));
        assertEquals(absa, d.resolveMethod(absa, c));

        assertEquals(absb, b.resolveMethod(absa, c));
        assertEquals(absb, b.resolveMethod(absb, c));
        assertEquals(absb, c.resolveMethod(absa, c));
        assertEquals(absb, c.resolveMethod(absb, c));
        assertEquals(abse, e.resolveMethod(absa, c));
        assertNull(e.resolveMethod(absb, c));
        assertEquals(abse, e.resolveMethod(abse, c));
    }

    @Test
    public void testVirtualMethod() {
        ResolvedJavaType a = getType(A.class);
        ResolvedJavaType b = getType(B.class);
        ResolvedJavaType c = getType(C.class);
        ResolvedJavaMethod v1a = getMethod(a, "v1");
        ResolvedJavaMethod v2a = getMethod(a, "v2");
        ResolvedJavaMethod v2b = getMethod(b, "v2");

        assertEquals(v1a, a.resolveMethod(v1a, c));
        assertEquals(v1a, b.resolveMethod(v1a, c));
        assertEquals(v1a, c.resolveMethod(v1a, c));
        assertEquals(v2a, a.resolveMethod(v2a, c));
        assertEquals(v2b, b.resolveMethod(v2a, c));
        assertEquals(v2b, b.resolveMethod(v2b, c));
        assertEquals(v2b, c.resolveMethod(v2a, c));
        assertEquals(v2b, c.resolveMethod(v2b, c));

    }

    static class ClassType {
    }

    interface InterfaceType {
    }

    @Test
    public void testCloneAccessibility() {
        /*
         * The resolution machinery for clone on arrays has some hacks in that show up in odd places
         * so make sure that resolveMethod works as expected.
         */
        ResolvedJavaType interfaceType = getType(InterfaceType.class);
        ResolvedJavaType classType = getType(ClassType.class);
        ResolvedJavaType arrayType = getType(double[].class);
        ResolvedJavaMethod cloneMethod = getMethod(getType(Object.class), "clone");
        assertEquals("Can't resolve clone for class", cloneMethod, arrayType.resolveMethod(cloneMethod, classType));
        assertEquals("Can't resolve clone for interface", cloneMethod, arrayType.resolveMethod(cloneMethod, interfaceType));
    }

    static ResolvedJavaMethod getMethod(ResolvedJavaType type, String methodName) {
        for (ResolvedJavaMethod method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException();
    }

    protected ResolvedJavaType getType(Class<?> clazz) {
        ResolvedJavaType type = metaAccess.lookupJavaType(clazz);
        type.initialize();
        return type;
    }
}
