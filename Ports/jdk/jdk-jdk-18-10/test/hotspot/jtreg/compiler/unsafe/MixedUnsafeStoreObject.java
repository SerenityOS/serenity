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
 * @bug 8155635
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:-TieredCompilation compiler.unsafe.MixedUnsafeStoreObject
 * @run main/othervm -Xbatch compiler.unsafe.MixedUnsafeStoreObject
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;

public class MixedUnsafeStoreObject {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static final long F_OFFSET;

    static {
        try {
            F_OFFSET = UNSAFE.objectFieldOffset(T.class.getDeclaredField("f"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static class T {
        Object f;
    }

    public static void testFieldInstanceObject(Object t) {
        for (int c = 0; c < 20000; c++) { // trigger OSR compilation
            // java/lang/Object+12 *
            // _base = InstPtr, _ptr = BotPTR, _field = NULL, mismatched = true
            UNSAFE.putReference(t, F_OFFSET, "foo");
        }
    }

    public static void testFieldInstanceT(T t) {
        for (int c = 0; c < 20000; c++) { // trigger OSR compilation
            // ...$T+12 *
            // _base = InstPtr, _ptr = BotPTR, _field = T.f, mismatched = false
            UNSAFE.putReference(t, F_OFFSET, "foo");
        }
    }
    public static void main(String[] args) {
        testFieldInstanceObject(new T());
        testFieldInstanceT(new T());
    }
}

