/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4100814 7044282
 * @summary Make sure you can't create an array of dimension > 256.
 */

import java.lang.reflect.Array;

public class ExceedMaxDim {

    public static void main(String[] args) throws Exception {
        newInstanceOne();
        newInstanceMulti();
        zeroDimension();
    }

    private static void newInstanceOne() throws Exception {
        Object o = getArrayOf256Dimensions();
        try {
            o = Array.newInstance(o.getClass(), 1);
        } catch (IllegalArgumentException iae) {
            System.out.println("success: newInstanceOne test");
            return;
        }
        throw new Exception("NewArray allowed dimensions > MAXDIM");
    }

    private static void newInstanceMulti() throws Exception {
        Object o = getArrayOf256Dimensions();
        try {
            o = Array.newInstance(o.getClass(), new int[] { 1, 1 });
            o = Array.newInstance(o.getClass(), new int[] { 1 });
        } catch (IllegalArgumentException iae) {
            System.out.println("success: newInstanceMulti test");
            return;
        }
        throw new Exception("MultiNewArray allowed dimensions > MAXDIM");
    }

    private static void zeroDimension() throws Exception {
        try {
            Array.newInstance(Integer.TYPE, new int[0]);
        } catch (IllegalArgumentException iae) {
            System.out.println("success: zeroDimension test");
            return;
        }
        throw new Exception("MultiNewArray allowed dimension == 0");
    }

    private static Object getArrayOf256Dimensions() {
        /* 1 dimension */
        Object o = Array.newInstance(Integer.TYPE, 0);

        /* 254 more dimension. */
        for (int i = 1; i <= 254; i++) {
            o = Array.newInstance(o.getClass(), 1);
        }

        /* 255 in all. */
        return o;
    }
}
