/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8016474
 * @summary The bug only happens with C1 and G1 using a different ObjectAlignmentInBytes than KlassAlignmentInBytes (which is 8)
 *
 * @modules java.base/jdk.internal.misc:+open
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:ObjectAlignmentInBytes=32
 *                   compiler.unsafe.GetUnsafeObjectG1PreBarrier
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class GetUnsafeObjectG1PreBarrier {
    private static final Unsafe unsafe;
    private static final int N = 100_000;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object a;

    public static void main(String[] args) throws Throwable {
        new GetUnsafeObjectG1PreBarrier();
    }

    public GetUnsafeObjectG1PreBarrier() throws Throwable {
        doit();
    }

    private void doit() throws Throwable {
        Field field = GetUnsafeObjectG1PreBarrier.class.getField("a");
        long fieldOffset = unsafe.objectFieldOffset(field);

        for (int i = 0; i < N; i++) {
            readField(this, fieldOffset);
        }
    }

    private void readField(Object o, long fieldOffset) {
        unsafe.getReference(o, fieldOffset);
    }
}
