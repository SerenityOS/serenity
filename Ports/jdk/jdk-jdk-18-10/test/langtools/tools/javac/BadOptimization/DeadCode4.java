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
 * @summary static init:  Verify that overzealous dead-code elimination no
 * longer removes live code.
 * @author dps
 *
 * @run clean DeadCode4
 * @run compile -O DeadCode4.java
 * @run main DeadCode4
 */

class cls
{
    static int arr[];

    static {
        arr = new int[2];
        arr[0] = 0;
        arr[1] = 2;
    }
}

public class DeadCode4
{
    final int x = 9;

    private final void fun1() {
        try {
            int i = cls.arr[3];
            // if we got here, then there must be a problem
            throw new RuntimeException("accidental removal of live code");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("ArrayIndexOutOfBoundsException correctly thrown");
            e.printStackTrace();
        }
    }

    public static void main( String[] args )
    {
        DeadCode4 r1 = new DeadCode4();
        r1.fun1();
    }
}
