/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.jvmci.common.testcases;

import java.util.HashMap;
import java.util.Map;

public class MultipleImplementer2 implements MultipleImplementersInterface {

    // Different access levels on the fields of this class are used on purpose.
    // It is needed to verify jdk.vm.ci.CompilerToVM constant pool related
    // methods, e.g. resolveFieldInPool.

    private static int intStaticField = INT_CONSTANT;
    final static long longStaticField = LONG_CONSTANT;
    volatile static float floatStaticField = FLOAT_CONSTANT;
    static double doubleStaticField = DOUBLE_CONSTANT;
    public static String stringStaticField = STRING_CONSTANT;
    protected static Object objectStaticField = OBJECT_CONSTANT;

    public int intField = INT_CONSTANT;
    private long longField = LONG_CONSTANT;
    protected float floatField = FLOAT_CONSTANT;
    transient double doubleField = DOUBLE_CONSTANT;
    volatile String stringField = STRING_CONSTANT;
    String stringFieldEmpty = "";
    final Object objectField;

    public MultipleImplementer2() {
        intField = Integer.MAX_VALUE;
        longField = Long.MAX_VALUE;
        floatField = Float.MAX_VALUE;
        doubleField = Double.MAX_VALUE;
        stringField = "Message";
        objectField = new Object();
    }

    @Override
    public void testMethod() {
        // empty
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }

    public void lambdaUsingMethod2() {
        Thread t = new Thread(this::testMethod);
        t.start();
    }

    /**
     * This method is needed to have "getstatic" and "getfield" instructions
     * in the class. These instructions are needed to test
     * resolveFieldInPool method, because it needs a bytecode as one of its arguments.
     */
    public void printFileds() {
        System.out.println(intStaticField);
        System.out.println(longStaticField);
        System.out.println(floatStaticField);
        System.out.println(doubleStaticField);
        System.out.println(stringStaticField);
        System.out.println(objectStaticField);
        System.out.println(intField);
        System.out.println(longField);
        System.out.println(floatField);
        System.out.println(doubleField);
        System.out.println(stringField);
        System.out.println(stringFieldEmpty);
        System.out.println(objectField);
    }

    public static void staticMethod() {
        System.getProperties(); // calling some static method
        Map map = new HashMap(); // calling some constructor
        map.put(OBJECT_CONSTANT, OBJECT_CONSTANT); // calling some interface method
        map.remove(OBJECT_CONSTANT); // calling some default interface method
    }

    @Override
    public void instanceMethod() {
        toString(); // calling some virtual method
        super.toString(); // calling some special method
    }

    @Override
    public void anonClassMethod() {
        new Runnable() {
            @Override
            public void run() {
                System.out.println("Running");
            }
        }.run();
    }
}
