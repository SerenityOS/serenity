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
 *   Test bridge methods in certain SAM conversion
 * @compile BridgeMethod.java
 * @run main BridgeMethod
 */

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class BridgeMethod {

    interface H {Object m();}

    interface K<T> {void m(T t);}

    interface L extends K<String> {} //generic substitution

    interface M {void m(String s);}

    interface KM extends K<String>, M{} //generic substitution

    interface N extends H {String m();} //covariant return

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    static void bar(String s) {
        System.out.println("BridgeMethod.bar(String) " + s);
    }

    String moo() {
        return "moo";
    }

    private static Set<String> setOfStringObject() {
        Set<String> s = new HashSet<>();
        s.add("java.lang.String");
        s.add("java.lang.Object");
        return s;
    }

    public static void main(String[] args) {
        L la = BridgeMethod::bar; //static reference
        la.m("hi");
        Class<? extends L> c1 = la.getClass();
        Method[] methods = c1.getDeclaredMethods();
        Set<String> types = setOfStringObject();
        System.out.println("methods in SAM conversion of L:");
        for(Method m : methods) {
            assertTrue(m.getName().equals("m"));
            System.out.println(m.toGenericString());
            Class[] parameterTypes = m.getParameterTypes();
            assertTrue(parameterTypes.length == 1);
            assertTrue(types.remove(parameterTypes[0].getName()));
        }
        assertTrue(types.isEmpty() || (types.size() == 1 && types.contains("java.lang.String")));

        KM km = BridgeMethod::bar;
        //km.m("hi"); //will be uncommented when CR7028808 fixed
        Class<? extends KM> c2 = km.getClass();
        methods = c2.getDeclaredMethods();
        types = setOfStringObject();
        System.out.println("methods in SAM conversion of KM:");
        for(Method m : methods) {
            assertTrue(m.getName().equals("m"));
            System.out.println(m.toGenericString());
            Class<?>[] parameterTypes = m.getParameterTypes();
            assertTrue(parameterTypes.length == 1);
            assertTrue(types.remove(parameterTypes[0].getName()));
        }
        assertTrue(types.isEmpty());

        N n = new BridgeMethod()::moo; //instance reference
        assertTrue( n.m().equals("moo") );
        assertTrue( ((H)n).m().equals("moo") );
        Class<? extends N> c3 = n.getClass();
        methods = c3.getDeclaredMethods();
        types = setOfStringObject();
        System.out.println("methods in SAM conversion of N:");
        for(Method m : methods) {
            System.out.println(m.toGenericString());
            if (m.getName().equals("m")) {
                Class<?> returnType = m.getReturnType();
                assertTrue(types.remove(returnType.getName()));
            }
        }
        assertTrue(types.size() == 1); //there's a bridge
    }
}
