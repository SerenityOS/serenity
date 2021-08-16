/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4057345 4120016 4120014
 * @summary try-catch 1:  Verify that overzealous dead-code elimination no
 * longer removes live code.
 * @author dps
 *
 * @run clean DeadCode1
 * @run compile -O DeadCode1.java
 * @run main DeadCode1
 */

public class DeadCode1
{
    public static int test() {
        Object[] arrayref = null;
        try {
            Object obj = arrayref[0];
            return 2;
        } catch (NullPointerException e)  {
            return 0;
        }
    }


    public static void main(String[] args) {
        int ret = test();
        if (ret == 2)
            throw new RuntimeException("test() = 2; accidental removal of live code");
        else
            System.out.println("correct dead-code elimination");
    }
}
