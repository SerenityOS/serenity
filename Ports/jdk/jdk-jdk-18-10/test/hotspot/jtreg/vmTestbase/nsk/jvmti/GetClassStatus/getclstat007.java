/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetClassStatus;

import java.io.PrintStream;

public class getclstat007 {

    final static int JCK_STATUS_BASE = 95;

    native static void check(int i, Class cls, boolean isArray);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        check(0, getclstat007.class, false);
        check(1, byte.class, false);
        check(2, char.class, false);
        check(3, double.class, false);
        check(4, float.class, false);
        check(5, int.class, false);
        check(6, long.class, false);
        check(7, short.class, false);
        check(8, void.class, false);
        check(9, boolean.class, false);
        check(10, new byte[1].getClass(), true);
        check(11, new char[1].getClass(), true);
        check(12, new double[1].getClass(), true);
        check(13, new float[1].getClass(), true);
        check(14, new int[1].getClass(), true);
        check(15, new long[1].getClass(), true);
        check(16, new short[1].getClass(), true);
        check(17, new boolean[1].getClass(), true);
        check(18, new Object[1].getClass(), true);
        return getRes();
    }
}
