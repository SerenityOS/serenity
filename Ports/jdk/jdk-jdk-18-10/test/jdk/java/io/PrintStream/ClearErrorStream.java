/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug  4491255
 * @summary Test for a new protected method PrintStream.clearError()
 *      to reset the internal error state
 */

import java.io.*;

public class ClearErrorStream extends PrintStream {

   public ClearErrorStream(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
   }

    public static void main(String[] args) throws Exception {

        File f = new File(System.getProperty("test.dir", "."),
                          "print-stream.out");
        f.deleteOnExit();

        ClearErrorStream out = new ClearErrorStream(
                                new BufferedOutputStream(
                                new FileOutputStream(f)),
                                true);
        out.println("Hello World!");
        out.close();
        out.println("Writing after close");

        if (out.checkError()) {
            System.out.println("An error occured");
            out.clearError();

            if (!out.checkError()) {
                System.out.println("Error status cleared");
            } else {
                throw new Exception("Error Status unchanged");
            }
         }
         else {
             System.out.println(" No error occured");
         }
    }
}
