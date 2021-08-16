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

package compiler.jvmci.common.testcases;

import java.util.HashMap;
import java.util.Map;

public interface MultipleImplementersInterface {

    int INT_CONSTANT = Integer.MAX_VALUE;
    long LONG_CONSTANT = Long.MAX_VALUE;
    float FLOAT_CONSTANT = Float.MAX_VALUE;
    double DOUBLE_CONSTANT = Double.MAX_VALUE;
    String STRING_CONSTANT = "Hello";
    Object OBJECT_CONSTANT = new Object();

    default void defaultMethod() {
        // empty
    }

    void testMethod();

    default void finalize() throws Throwable {
        // empty
    }

    default void lambdaUsingMethod() {
        Thread t = new Thread(this::defaultMethod);
        t.start();
    }

    default void printFields() {
        System.out.println(OBJECT_CONSTANT);
        String s = "";
        System.out.println(s);
    }

    static void staticMethod() {
        System.getProperties(); // calling some static method
        Map map = new HashMap(); // calling some constructor
        map.put(OBJECT_CONSTANT, OBJECT_CONSTANT); // calling some interface method
        map.remove(OBJECT_CONSTANT); // calling some default interface method
    }

    default void instanceMethod() {
        toString(); // calling some virtual method
    }

    default void anonClassMethod() {
        new Runnable() {
            @Override
            public void run() {
                System.out.println("Running");
            }
        }.run();
    }
}
