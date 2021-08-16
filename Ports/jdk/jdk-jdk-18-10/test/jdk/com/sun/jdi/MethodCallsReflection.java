/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Debuggee which exercises various types of method calls which throw
 * Exceptions, including some done via Reflection,
 * This was written while investigating Bug ID 4359606
 */

class MethodCallsReflection {

    public static void main(String args[]) throws Exception {
        (new MethodCallsReflection()).go();
    }

    static void staticCaller1(MethodCallsReflection mc) throws Exception {
        System.out.println("Called staticCaller1");
        staticExceptionCallee();
    }
    static void staticCaller2(MethodCallsReflection mc) throws Exception {
        System.out.println("Called staticCaller2");
        mc.instanceExceptionCallee();
    }
    static void staticCaller3(MethodCallsReflection mc) throws Exception {
        System.out.println("Called staticCaller3");
        /*
         * Invocation by reflection. This also exercises native method calls
         * since Method.invoke is a native method.
         */
        Method m = MethodCallsReflection.class.getDeclaredMethod("staticExceptionCallee", new Class[0]);
        m.invoke(mc, new Object[0]);
    }

    void instanceCaller1() throws Exception {
        System.out.println("Called instanceCaller1");
        staticExceptionCallee();
    }

    void instanceCaller2() throws Exception {
        System.out.println("Called instanceCaller2");
        instanceExceptionCallee();
    }

    void instanceCaller3() throws Exception {
        System.out.println("Called instanceCaller3");

        /*
         * Invocation by reflection. This also exercises native method calls
         * since Method.invoke is a native method.
         */
        Method m = getClass().getDeclaredMethod("instanceExceptionCallee", new Class[0]);
        m.invoke(this, new Object[0]);
    }

   static  void staticExceptionCallee() throws Exception {
        System.out.println("Called staticExceptionCallee");
        throw new IndexOutOfBoundsException ("staticExceptionCallee");
    }

    void instanceExceptionCallee() throws Exception {
        System.out.println("Called instanceExceptionCallee");
        throw new IndexOutOfBoundsException ("instanceExceptionCallee");
    }

    void go() throws Exception {
        try {
            instanceCaller1();
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Caught expected IndexOutOfBoundsException from instanceCaller1()");
        }

        try {
            instanceCaller2();
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Caught expected IndexOutOfBoundsException from instanceCaller2()");
        }

        try {
            instanceCaller3();
        } catch (InvocationTargetException ex) {
            System.out.println("Caught expected InvocationTargetException from instanceCaller3()");
        }

        try {
            staticCaller1(this);
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Caught expected IndexOutOfBoundsException from staticCaller1()");
        }

        try {
            staticCaller2(this);
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("Caught expected IndexOutOfBoundsException from staticCaller2()");
        }
        try {
            staticCaller3(this);
        } catch (InvocationTargetException ex) {
            System.out.println("Caught expected InvocationTargetException from staticCaller3()");
        }
    }
}
