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

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8159746
 * @summary Test invoking a default method in a non-public proxy interface
 * @build p.Foo p.Bar p.ProxyMaker
 * @run testng DefaultMethodProxy
 */
public class DefaultMethodProxy {
    public interface I {
        default String m() { return "I"; }
    }

    @Test
    public static void publicInterface() throws ReflectiveOperationException {
        // create a proxy instance of a public proxy interface should succeed
        Proxy proxy = (Proxy)Proxy.newProxyInstance(DefaultMethodProxy.class.getClassLoader(),
                new Class<?>[] { I.class }, IH);

        testDefaultMethod(proxy, "I");

        // can get the invocation handler
        assertTrue(Proxy.getInvocationHandler(proxy) == IH);
    }


    @DataProvider(name = "nonPublicIntfs")
    private static Object[][] nonPublicIntfs() throws ClassNotFoundException {
        Class<?> fooClass = Class.forName("p.Foo");
        Class<?> barClass = Class.forName("p.Bar");
        return new Object[][]{
                new Object[]{new Class<?>[]{ fooClass }, "foo"},
                new Object[]{new Class<?>[]{ barClass, fooClass }, "bar"},
                new Object[]{new Class<?>[]{ barClass }, "bar"},
        };
    }

    @Test(dataProvider = "nonPublicIntfs")
    public static void hasPackageAccess(Class<?>[] intfs, String expected) throws ReflectiveOperationException {
        Proxy proxy = (Proxy)Proxy.newProxyInstance(DefaultMethodProxy.class.getClassLoader(), intfs, IH);
        testDefaultMethod(proxy, expected);

        // proxy instance is created successfully even invocation handler has no access
        Proxy.newProxyInstance(DefaultMethodProxy.class.getClassLoader(), intfs, IH_NO_ACCESS);
    }

    // IAE thrown at invocation time
    @Test(dataProvider = "nonPublicIntfs", expectedExceptions = {IllegalAccessException.class})
    public static void noPackageAccess(Class<?>[] intfs, String ignored) throws Throwable {
        Proxy proxy = (Proxy)Proxy.newProxyInstance(DefaultMethodProxy.class.getClassLoader(), intfs, IH_NO_ACCESS);
        try {
            testDefaultMethod(proxy, "dummy");
        } catch (InvocationTargetException e) {
            // unwrap the exception
            if (e.getCause() instanceof UndeclaredThrowableException) {
                Throwable cause = e.getCause();
                throw cause.getCause();
            }
            throw e;
        }
    }

    /*
     * Verify if a default method "m" can be invoked successfully
     */
    static void testDefaultMethod(Proxy proxy, String expected) throws ReflectiveOperationException {
        Method m = proxy.getClass().getDeclaredMethod("m");
        m.setAccessible(true);
        String name = (String) m.invoke(proxy);
        if (!expected.equals(name)) {
            throw new RuntimeException("return value: " + name + " expected: " + expected);
        }
    }

    // invocation handler with access to the non-public interface in package p
    private static final InvocationHandler IH = (proxy, method, params) -> {
        System.out.format("Proxy for %s: invoking %s%n",
                Arrays.stream(proxy.getClass().getInterfaces())
                      .map(Class::getName)
                      .collect(Collectors.joining(", ")), method.getName());
        if (method.isDefault()) {
            return p.ProxyMaker.invoke(proxy, method, params);
        }
        throw new UnsupportedOperationException(method.toString());
    };

    // invocation handler with no access to the non-public interface in package p
    // expect IllegalAccessException thrown
    private static final InvocationHandler IH_NO_ACCESS = (proxy, method, params) -> {
        System.out.format("Proxy for %s: invoking %s%n",
                Arrays.stream(proxy.getClass().getInterfaces())
                        .map(Class::getName)
                        .collect(Collectors.joining(", ")), method.getName());
        if (method.isDefault()) {
            InvocationHandler.invokeDefault(proxy, method, params);
        }
        throw new UnsupportedOperationException(method.toString());
    };
}
