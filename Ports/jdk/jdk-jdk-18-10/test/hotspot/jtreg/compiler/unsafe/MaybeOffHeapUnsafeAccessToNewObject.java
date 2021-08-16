/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8219335
 * @summary "failed: unexpected type" assert failure in ConnectionGraph::split_unique_types() with unsafe accesses
 *
 * @modules java.base/jdk.internal.misc
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation -XX:-TieredCompilation -Xcomp -XX:CompileOnly=MaybeOffHeapUnsafeAccessToNewObject::test1 -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline MaybeOffHeapUnsafeAccessToNewObject
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

public class MaybeOffHeapUnsafeAccessToNewObject {
    public volatile int f_int = -1;


    public static Unsafe unsafe = Unsafe.getUnsafe();
    public static final long f_int_off;

    static {
        Field f_int_field = null;
        try {
            f_int_field = MaybeOffHeapUnsafeAccessToNewObject.class.getField("f_int");
        } catch (Exception e) {
            System.out.println("reflection failed " + e);
            e.printStackTrace();
        }
        f_int_off = unsafe.objectFieldOffset(f_int_field);
    }

    static public void main(String[] args) {
        MaybeOffHeapUnsafeAccessToNewObject o = new MaybeOffHeapUnsafeAccessToNewObject();
        test1();
    }

    static Object test1_helper1(Object t) {
        return t;
    }

    static long test1_helper2(long off) {
        return off;
    }

    static int test1() {
        MaybeOffHeapUnsafeAccessToNewObject t = new MaybeOffHeapUnsafeAccessToNewObject();
        Object o = test1_helper1(t);
        long off = test1_helper2(f_int_off);
        return unsafe.getInt(o, off);
    }

}
