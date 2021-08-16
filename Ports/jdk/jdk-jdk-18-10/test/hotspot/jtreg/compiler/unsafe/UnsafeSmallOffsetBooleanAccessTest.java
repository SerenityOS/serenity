/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8161720
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:-TieredCompilation UnsafeSmallOffsetBooleanAccessTest
 * @run main/othervm -Xbatch UnsafeSmallOffsetBooleanAccessTest
 */

import java.util.Random;
import jdk.internal.misc.Unsafe;

public class UnsafeSmallOffsetBooleanAccessTest {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static final long F_OFFSET;
    static final Random random = new Random(42);

    static {
        try {
            F_OFFSET = UNSAFE.objectFieldOffset(T.class.getDeclaredField("f"));
            System.out.println("The offset is: " + F_OFFSET);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static class T {
        boolean f;
    }

    // Always return false in a way that is not obvious to the compiler.
    public static boolean myRandom() {
        if (random.nextInt(101) > 134) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean test(T t) {
        boolean result = false;
        for (int i = 0; i < 20000; i++) {
            boolean random = myRandom();
            // If myRandom() returns false, access t.f.
            //
            // If myRandom() returns true, access virtual address
            // F_OFFSET. That address is most likely not mapped,
            // therefore the access will most likely cause a
            // crash. We're not concerned about that, though, because
            // myRandom() always returns false. However, the C2
            // compiler avoids normalization of the value returned by
            // getBoolean in this case.
            result = UNSAFE.getBoolean(myRandom() ? null : t, F_OFFSET);
        }
        return result;
    }

    public static void main(String[] args) {
        T t = new T();
        UNSAFE.putBoolean(t, F_OFFSET, true);
        System.out.println("The result for t is: " + test(t));
    }
}
