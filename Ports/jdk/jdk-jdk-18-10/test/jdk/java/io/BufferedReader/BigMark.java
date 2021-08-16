/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @summary BufferedReader should throw an OutOfMemoryError when the
            read-ahead limit is very large
   @bug 6350733
   @build BigMark
   @run main/othervm BigMark
*/

import java.io.*;


public class BigMark {

    public static void main(String[] args) throws IOException {
        String line;
        int i = 0;
        String dir = System.getProperty("test.src", ".");
        BufferedReader br
            = new BufferedReader(new FileReader(new File(dir, "BigMark.java")), 100);

        br.mark(200);
        line = br.readLine();
        System.err.println(i + ": " + line);
        i++;

        try {
            // BR.fill() call to new char[Integer.MAX_VALUE] should succeed
            br.mark(Integer.MAX_VALUE);
            line = br.readLine();
        } catch (OutOfMemoryError x) {
            x.printStackTrace();
            throw x;
        }
        System.out.println("OutOfMemoryError not thrown as expected");
    }

}
