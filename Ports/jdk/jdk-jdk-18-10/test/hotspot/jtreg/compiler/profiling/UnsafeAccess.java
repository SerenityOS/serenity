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
 * @bug 8134918
 * @modules java.base/jdk.internal.misc
 *
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:TypeProfileLevel=222 -XX:+UseTypeSpeculation -Xbatch
 *                                 -XX:CompileCommand=dontinline,compiler.profiling.UnsafeAccess::test*
 *                                 compiler.profiling.UnsafeAccess
 */

package compiler.profiling;

import jdk.internal.misc.Unsafe;

public class UnsafeAccess {
    private static final Unsafe U = Unsafe.getUnsafe();

    static Class cls = Object.class;
    static long off = U.ARRAY_OBJECT_BASE_OFFSET;

    static Object testUnsafeAccess(Object o, boolean isObjArray) {
        if (o != null && cls.isInstance(o)) { // speculates "o" type to int[]
            return helperUnsafeAccess(o, isObjArray);
        }
        return null;
    }

    static Object helperUnsafeAccess(Object o, boolean isObjArray) {
        if (isObjArray) {
            U.putReference(o, off, new Object());
        }
        return o;
    }

    static Object testUnsafeLoadStore(Object o, boolean isObjArray) {
        if (o != null && cls.isInstance(o)) { // speculates "o" type to int[]
            return helperUnsafeLoadStore(o, isObjArray);
        }
        return null;
    }

    static Object helperUnsafeLoadStore(Object o, boolean isObjArray) {
        if (isObjArray) {
            Object o1 = U.getReference(o, off);
            U.compareAndSetReference(o, off, o1, new Object());
        }
        return o;
    }

    public static void main(String[] args) {
        Object[] objArray = new Object[10];
        int[]    intArray = new    int[10];

        for (int i = 0; i < 20_000; i++) {
            helperUnsafeAccess(objArray, true);
        }
        for (int i = 0; i < 20_000; i++) {
            testUnsafeAccess(intArray, false);
        }

        for (int i = 0; i < 20_000; i++) {
            helperUnsafeLoadStore(objArray, true);
        }
        for (int i = 0; i < 20_000; i++) {
            testUnsafeLoadStore(intArray, false);
        }

        System.out.println("TEST PASSED");
    }
}
