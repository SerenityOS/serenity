/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4258644
 * @summary ObjectInputStream's default implementation of its protected
 * resolveProxyClass method is specified to pass the first non-null class
 * loader up the execution stack to the Proxy.getProxyClass method when
 * it creates the specified proxy class; this test makes sure that it does
 * that in situations where it hadn't in the past, such as if the defining
 * loaders of the interfaces were all strict ancestors of the first
 * non-null loader up the stack.
 * @author Peter Jones
 *
 * @build ResolveProxyClass
 * @run main ResolveProxyClass
 */

import java.io.*;

public class ResolveProxyClass {

    /*
     * This class is a dummy ObjectInputStream subclass that allows the
     * test code to access ObjectInputStream's protected resolveProxyClass
     * method directly.
     */
    private static class TestObjectInputStream extends ObjectInputStream {

        TestObjectInputStream() throws IOException {
            super();
        }

        protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException
        {
            return super.resolveProxyClass(interfaces);
        }
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4258644\n");

        try {

            /*
             * Set this thread's context class loader to null, so that the
             * resolveProxyClass implementation cannot cheat by guessing that
             * the context class loader is the appropriate loader to pass to
             * the Proxy.getProxyClass method.
             */
            Thread.currentThread().setContextClassLoader(null);

            /*
             * Expect the proxy class to be defined in the system class
             * loader, because that is the defining loader of this test
             * code, and it should be the first loader on the stack when
             * ObjectInputStream.resolveProxyClass gets executed.
             */
            ClassLoader expectedLoader = ResolveProxyClass.class.getClassLoader();

            TestObjectInputStream in = new TestObjectInputStream();
            Class<?> proxyClass = in.resolveProxyClass(
                new String[] { Runnable.class.getName() });
            ClassLoader proxyLoader = proxyClass.getClassLoader();
            System.err.println("proxy class \"" + proxyClass +
                "\" defined in loader: " + proxyLoader);

            if (proxyLoader != expectedLoader) {
                throw new RuntimeException(
                    "proxy class defined in loader: " + proxyLoader);
            }

            System.err.println("\nTEST PASSED");

        } catch (Throwable e) {
            System.err.println("\nTEST FAILED:");
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        }
    }
}
