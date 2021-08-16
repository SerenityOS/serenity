/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4140804
   @summary Test if seek will throw exception given a
            negative offset.
*/



import java.io.*;

public class Seek
{
    public static void main(String argv[]) throws Exception
    {
        String dir = System.getProperty("test.src", ".");
        RandomAccessFile raf = new RandomAccessFile
            (new File(dir, "Seek.java"), "r");

        try {
            raf.seek(-10);
            throw new Exception
                ("Should have thrown an IOException when seek offset is < 0");
        } catch (IOException e) {
        } finally {
            raf.close();
        }
    }
}
