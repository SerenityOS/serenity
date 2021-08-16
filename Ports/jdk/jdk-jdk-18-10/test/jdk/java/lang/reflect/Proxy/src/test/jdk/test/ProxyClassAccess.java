/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.internal.*;
import jdk.test.internal.foo.*;
import p.two.Bar;
import p.three.P;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Test proxy class to have access to types referenced in the public methods.
 */
public class ProxyClassAccess {
    public static void main(String... args) throws Exception {
        testImplClass();
        testProxyClass1();
        testProxyClass2();
        testNonPublicProxy();
    }

    /*
     * Invoke methods from implementation class
     */
    static void testImplClass() {
        R impl = new RImpl();
        impl.foo();
        Bar[][] bars = new Bar[0][0];
        impl.setBarArray(bars);
        try {
            impl.throwException();
            throw new RuntimeException("FooException not thrown");
        } catch (FooException e) { }
    }

    /*
     * Invoke methods via proxy
     */
    static void testProxyClass1() {
        R proxy = (R) Proxy.newProxyInstance(R.class.getClassLoader(),
                                             new Class<?>[] { R.class }, handler);
        proxy.foo();
        Bar[][] bars = new Bar[0][0];
        proxy.setBarArray(bars);
    }

    /*
     * Invoke methods via proxy defined with a custom class loader
     */
    static void testProxyClass2() {
        URLClassLoader loader = new URLClassLoader(new URL[0]);
        P proxy = (P) Proxy.newProxyInstance(loader,
                new Class<?>[] { R.class, P.class }, handler);
        proxy.bar();
        proxy.barArrays();
    }

    static void testNonPublicProxy() {
        NP proxy = (NP) Proxy.newProxyInstance(NP.class.getClassLoader(),
                                               new Class<?>[]{NP.class}, handler);
        proxy.test();

        try {
            URLClassLoader loader = new URLClassLoader(new URL[0]);
            proxy = (NP) Proxy.newProxyInstance(loader,
                    new Class<?>[]{NP.class}, handler);
            throw new RuntimeException("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    static InvocationHandler handler = new InvocationHandler() {
        final R impl = new RImpl();
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == R.class) {
                return method.invoke(impl, args);
            } else {
                return null;
            }
        }
    };
}
