/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8240676
 * @summary Meet not symmetric failure when running lucene on jdk8
 *
 * @run main/othervm -XX:-BackgroundCompilation TestArrayMeetNotSymmetrical
 *
 */

public class TestArrayMeetNotSymmetrical {
    private static final Object field = new Object[0];
    private static final Object field2 = new A[0];

    public static void main(String[] args) {
        Object array = new A[10];
        for (int i = 0; i < 20_000; i++) {
            test1(true, 10);
            test1(false, 10);
            test2(true);
            test2(false);
        }
    }

    private static Object test1(boolean flag, int len) {
        Object o;
        if (flag) {
            o = field;
        } else {
            o = new A[len];
        }
        return o;
    }

    private static Object test2(boolean flag) {
        Object o;
        if (flag) {
            o = field;
        } else {
            o = field2;
        }
        return o;
    }


    private static class A {
    }
}
