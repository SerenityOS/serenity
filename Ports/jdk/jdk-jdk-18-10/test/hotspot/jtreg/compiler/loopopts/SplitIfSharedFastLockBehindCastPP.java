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
 * @bug 8231620
 * @summary assert(bol->is_Bool()) crash during split if due to FastLockNode
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement SplitIfSharedFastLockBehindCastPP
 */


public class SplitIfSharedFastLockBehindCastPP {
    private static boolean field;
    private static A obj_field;

    public static void main(String[] args) {
        A lock = new A();
        obj_field = lock;
        for (int i = 0; i < 20_000; i++) {
            test1(true, lock);
            test1(false, lock);
            test2(true);
            test2(false);
        }
    }

    private static void test1(boolean flag, Object obj) {
        if (obj == null) {
        }

        boolean flag2;
        if (flag) {
            flag2 = true;
        } else {
            flag2 = false;
            obj = obj_field;
        }

        // This loop will be unswitched. The condition becomes candidate for split if
        for (int i = 0; i < 100; i++) {
            if (flag2) {
                field = true;
            } else {
                field = false;
            }
            synchronized (obj) {
                field = true;
            }
        }
    }

    private static Object test2(boolean flag) {
        int integer;
        if (flag) {
            field = true;
            integer = 1;
        } else {
            field = false;
            integer = 2;
        }

        Object obj = integer;

        // This loop will be unswitched. The condition becomes candidate for split if
        for (int i = 0; i < 100; i++) {
            if (integer == 1) {
                field = true;
            } else {
                field = false;
            }
            synchronized (obj) {
                field = true;
            }
        }
        return obj;
    }

    private static final class A {
    }
}
