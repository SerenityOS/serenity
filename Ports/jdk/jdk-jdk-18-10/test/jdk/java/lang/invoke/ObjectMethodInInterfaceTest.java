/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031502
 * @summary JSR292: IncompatibleClassChangeError in LambdaForm for CharSequence.toString() method handle type converter
 * @compile ObjectMethodInInterfaceTest.java
 * @run main/othervm -Djava.lang.invoke.MethodHandle.COMPILE_THRESHOLD=0 test.java.lang.invoke.ObjectMethodInInterfaceTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ObjectMethodInInterfaceTest {
    public static void main(String[] args) throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findVirtual(CharSequence.class, "toString", MethodType.methodType(String.class));
        MethodType mt = MethodType.methodType(Object.class, CharSequence.class);
        mh = mh.asType(mt);

        Object res = mh.invokeExact((CharSequence)"123");

        System.out.println("TEST PASSED");
    }
}
