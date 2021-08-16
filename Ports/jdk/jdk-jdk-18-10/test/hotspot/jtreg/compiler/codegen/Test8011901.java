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
 * @bug 8011901
 * @summary instruct xaddL_no_res shouldn't allow 64 bit constants.
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:-BackgroundCompilation compiler.codegen.Test8011901
 */

package compiler.codegen;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class Test8011901 {

    private long ctl;

    private static final Unsafe U;
    private static final long CTL;

    static {
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
            CTL = U.objectFieldOffset(Test8011901.class.getDeclaredField("ctl"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static void main(String[] args) {
        for(int c = 0; c < 20000; c++) {
            new Test8011901().makeTest();
        }
        System.out.println("Test Passed");
    }

    public static final long EXPECTED = 1L << 42;

    public void makeTest() {
        U.getAndAddLong(this, CTL, EXPECTED);
        if (ctl != EXPECTED) {
            throw new RuntimeException("Test failed. Expected: " + EXPECTED + ", but got = " + ctl);
        }
    }
}
