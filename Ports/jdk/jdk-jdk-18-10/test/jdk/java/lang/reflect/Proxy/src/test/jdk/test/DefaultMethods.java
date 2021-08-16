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

package jdk.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Tests invocation of default methods in exported types and inaccessible types
 * in a named module
 */
public class DefaultMethods {
    private final static Module TEST_MODULE = DefaultMethods.class.getModule();
    private final static InvocationHandler IH = (proxy, method, params) -> {
        return InvocationHandler.invokeDefault(proxy, method, params);
    };

    public static void main(String... args) throws Throwable {
        // exported types from m1
        testDefaultMethod(new Class<?>[] { p.one.I.class, p.two.A.class}, 1);
        // qualified-exported type from m2
        testDefaultMethod(new Class<?>[] { p.two.internal.C.class, p.two.A.class }, 2);
        // module-private type from test module
        testDefaultMethod(new Class<?>[] { jdk.test.internal.R.class }, 10);
        // non-public interface in the same runtime package
        testDefaultMethod(new Class<?>[] { Class.forName("jdk.test.NP") }, 100);

        // inaccessible type - not exported to test module
        Class<?> qType = Class.forName("p.three.internal.Q");
        inaccessibleDefaultMethod(qType);
        // non-public interface in the same runtime package
        Class<?> nonPublicType = Class.forName("jdk.test.internal.NP");
        inaccessibleDefaultMethod(nonPublicType);
    }

    static void testDefaultMethod(Class<?>[] intfs, int expected) throws Exception {
        Object proxy = Proxy.newProxyInstance(TEST_MODULE.getClassLoader(), intfs, IH);
        if (!proxy.getClass().getModule().isNamed()) {
            throw new RuntimeException(proxy.getClass() + " expected to be in a named module");
        }
        Method m = intfs[0].getMethod("m");
        int result = (int)m.invoke(proxy);
        if (result != expected) {
            throw new RuntimeException("return value: " + result + " expected: " + expected);
        }
    }

    static void inaccessibleDefaultMethod(Class<?> intf) throws Throwable {
        Object proxy = Proxy.newProxyInstance(TEST_MODULE.getClassLoader(), new Class<?>[] { intf }, IH);
        if (!proxy.getClass().getModule().isNamed()) {
            throw new RuntimeException(proxy.getClass() + " expected to be in a named module");
        }
        Method m = intf.getMethod("m");
        try {
            InvocationHandler.invokeDefault(proxy, m, null);
            throw new RuntimeException("IAE not thrown invoking: " + m);
        } catch (IllegalAccessException e) {}

        if (m.trySetAccessible()) {
            try {
                m.invoke(proxy);
                throw new RuntimeException("IAE not thrown invoking: " + m);
            } catch (InvocationTargetException e) {
                // IAE wrapped by InvocationHandler::invoke with UndeclaredThrowableException
                // then wrapped by Method::invoke with InvocationTargetException
                assert e.getCause() instanceof UndeclaredThrowableException;
                Throwable cause = e.getCause().getCause();
                if (!(cause instanceof IllegalAccessException))
                    throw e;
            }
        }
    }
}
