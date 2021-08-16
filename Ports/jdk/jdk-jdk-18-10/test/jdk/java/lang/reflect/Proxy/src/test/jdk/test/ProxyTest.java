/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class ProxyTest {
    public static class Data {
        private static int count = 0;
        final int testcase;
        final ClassLoader loader;
        final Module module;
        final Class<?>[] interfaces;
        // Expected the proxy class in the specified module
        public Data(Module m, ClassLoader loader, Class<?>... interfaces) {
            this.module = m;
            this.loader = loader;
            this.interfaces = interfaces;
            this.testcase = ++count;
        }
        // Expected the proxy class in a dynamic module
        public Data(ClassLoader loader, Class<?>... interfaces) {
            this(null, loader, interfaces);
        }

        @Override
        public String toString() {
            String expected = module != null
                    ? (module.isNamed() ? module.getName() : "unnamed")
                    : "dynamic";
            return String.format("%2d: Expected: %s %s loader: %s", testcase, expected,
                    Arrays.toString(interfaces), loader);
        }
    }

    public void test(Data d) {
        System.out.println(d);

        if (d.module != null) {
            testProxyClass(d.module, d.loader, d.interfaces);
        } else {
            testDynamicModule(d);
        }
    }

    private void testDynamicModule(Data d) {
        Class<?> proxyClass = Proxy.getProxyClass(d.loader, d.interfaces);
        assertDynamicModule(proxyClass.getModule(), d.loader, proxyClass);

        Object proxy = Proxy.newProxyInstance(d.loader, d.interfaces, handler);
        assertDynamicModule(proxy.getClass().getModule(), d.loader, proxy.getClass());
    }

    private static void testProxyClass(Module module, ClassLoader ld, Class<?>... interfaces) {
        Class<?> proxyClass = Proxy.getProxyClass(ld, interfaces);
        assertEquals(proxyClass.getModule(), module);

        Object proxy = Proxy.newProxyInstance(ld, interfaces, handler);
        assertEquals(proxy.getClass().getModule(), module);
    }

    public static void assertDynamicModule(Module m, ClassLoader ld, Class<?> proxyClass) {
        if (!m.isNamed() || !m.getName().startsWith("jdk.proxy")) {
            throw new RuntimeException(m.getName() + " not dynamic module");
        }

        if (ld != m.getClassLoader() || proxyClass.getClassLoader() != ld) {
            throw new RuntimeException("unexpected class loader");
        }

        try {
            Constructor<?> cons = proxyClass.getConstructor(InvocationHandler.class);
            cons.newInstance(handler);
            // the exported package has the same name as the dynamic module
            if (!proxyClass.getPackageName().equals(m.getName())) {
                throw new RuntimeException("Expected IllegalAccessException: " + proxyClass);
            }
        } catch (IllegalAccessException e) {
            // expected
        } catch (NoSuchMethodException|InstantiationException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertEquals(Object o1, Object o2) {
        if (o1 != o2) {
            throw new RuntimeException(o1 + " != " + o2);
        }
    }
    private final static InvocationHandler handler =
        (proxy, m, params) -> { throw new RuntimeException(m.toString()); };
}
