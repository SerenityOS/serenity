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

/* @test
 * @bug 8188240
 * @summary This is a test to ensure that proxies do not try to intercept interface static methods.
 *
 * @build ProxyClashTest
 * @run main ProxyClashTest
 */

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Observer;

public class ProxyClashTest {

    public interface ClashWithRunnable {
        static int run() { return 123; }

        static void foo() {}
    }

    public static void main(String[] args) {
        System.err.println(
            "\nDynamic proxy API static method clash test\n");

        Class<?>[] interfaces =
            new Class<?>[] { ClashWithRunnable.class, Runnable.class, Observer.class };

        ClassLoader loader = ProxyClashTest.class.getClassLoader();

        /*
         * Generate a proxy class.
         */
        Class<?> proxyClass = Proxy.getProxyClass(loader, interfaces);
        System.err.println("+ generated proxy class: " + proxyClass);

        for (Method method : proxyClass.getDeclaredMethods()) {
            if (method.getName().equals("run") && method.getReturnType() == int.class) {
                throw new RuntimeException("proxy intercept a static method");
            }
            if (method.getName().equals("foo")) {
                throw new RuntimeException("proxy intercept a static method");
            }
        }

        System.err.println("\nTEST PASSED");
    }
}
