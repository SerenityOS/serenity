/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8134389
 *
 * @run main/othervm -Xbatch compiler.jsr292.TestArrayReturnType
 * @run main/othervm -Xbatch -XX:-Inline compiler.jsr292.TestArrayReturnType
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=exclude,compiler.jsr292.TestArrayReturnType::testArrayReturnType
 *      compiler.jsr292.TestArrayReturnType
 */

package compiler.jsr292;


import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class TestArrayReturnType {

    static final MethodHandle mh;
    static int[] testArray = new int[1];
    static {
        try {
            mh = MethodHandles.lookup().findStatic(TestArrayReturnType.class, "testArrayReturnType", MethodType.methodType(int[].class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static int[] testArrayReturnType() {
        return testArray;
    }

    public static void test()  throws Throwable {
        int a[] = (int[])mh.invokeExact();
        for (int i=0; i<a.length; i++) {
            a[i] = 1;
        }
    }

    public static void main(String[] args) throws Throwable {
        for (int i=0; i<15000; i++) {
            test();
        }
        System.out.println("TEST PASSED");
    }
}
