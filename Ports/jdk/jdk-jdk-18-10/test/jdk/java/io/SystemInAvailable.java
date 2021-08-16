/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4135795
   @summary Make sure that System.in.available() works
            when System.in is the keyboard
   @run ignore This test requires console (/dev/tty) input, which is not
               supported by the current harness
 */


import java.io.*;

public class SystemInAvailable {

    public static void main(String[] args) throws Exception {
        byte[] b = new byte[1024];
        System.out.print("Press <enter>: ");
        System.out.flush();
        System.in.read(b);
        int a = System.in.available();
        if (a != 0) throw new Exception("System.in.available() ==> " + a);
    }

}
