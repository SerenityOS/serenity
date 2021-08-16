/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test bridge methods for certain SAM conversions
 *   Test the set of generated methods
 * @compile LambdaTest6.java
 * @run main LambdaTest6
 */

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class LambdaTest6<T> {

    interface H {Object m();}

    interface K<U> {void m(U element);}

    interface L extends K<String> {} //generic substitution

    interface M {void m(String s);}

    interface KM extends K<String>, M{} //generic substitution

    interface N extends H {String m();} //covariant return

    private static void assertTrue(boolean b) {
        if(!b)
            throw new AssertionError();
    }

    private Set<String> setOfStringObject() {
        Set<String> s = new HashSet<>();
        s.add("java.lang.String");
        s.add("java.lang.Object");
        return s;
    }

    private static Set<String> allowedMethods() {
        Set<String> s = new HashSet<>();
        s.add("m");
        return s;
    }

    private static boolean matchingMethodNames(Method[] methods) {
        Set<String> methodNames = new HashSet<>();
        for (Method m : methods) {
            methodNames.add(m.getName());
        }
        return methodNames.equals(allowedMethods());
    }

    private void test1()
    {
        L la = s -> { };
        la.m("hi");
        Class<? extends L> c1 = la.getClass();
        Method[] methods = c1.getDeclaredMethods();
        assertTrue(matchingMethodNames(methods));
        Set<String> types = setOfStringObject();
        for(Method m : methods) {
            if ("m".equals(m.getName())) {
                Class[] parameterTypes = m.getParameterTypes();
                assertTrue(parameterTypes.length == 1);
                assertTrue(types.remove(parameterTypes[0].getName()));
            }
        }
        assertTrue(types.isEmpty() || (types.size() == 1 && types.contains("java.lang.String")));
    }

    private void test2()
    {
        KM km = s -> { };
        //km.m("hi");
        Class<? extends KM> c2 = km.getClass();
        Method[] methods = c2.getDeclaredMethods();
        assertTrue(matchingMethodNames(methods));
        Set<String> types = setOfStringObject();
        for(Method m : methods) {
            if ("m".equals(m.getName())) {
                Class[] parameterTypes = m.getParameterTypes();
                assertTrue(parameterTypes.length == 1);
                assertTrue(types.remove(parameterTypes[0].getName()));
            }
        }
        assertTrue(types.isEmpty());
    }

    private void test3()
    {
        N na = ()-> "hi";
        assertTrue( na.m().equals("hi") );
        assertTrue( ((H)na).m().equals("hi") );
        Class<? extends N> c3 = na.getClass();
        Method[] methods = c3.getDeclaredMethods();
        assertTrue(matchingMethodNames(methods));
        Set<String> types = setOfStringObject();
        for(Method m : methods) {
            if ("m".equals(m.getName())) {
                Class returnType = m.getReturnType();
                assertTrue(types.remove(returnType.getName()));
            }
        }
        assertTrue(types.size() == 1); //there's a bridge
    }


    public static void main(String[] args) {
        LambdaTest6 test = new LambdaTest6();
        test.test1();
        test.test2();
        test.test3();
    }
}
