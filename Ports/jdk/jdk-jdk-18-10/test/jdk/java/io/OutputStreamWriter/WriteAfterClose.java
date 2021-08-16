/*
 * Copyright (c) 1998, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4143651 5085148
 *
 * @summary Test if Writer methods will check if the stream
 *           has been closed.
 */

import java.io.*;

public class WriteAfterClose {

    static boolean failed = false;

    static void testWrite(Writer wtr) throws Exception {

        // These tests could be tighted to only check for a particular
        // kind of exception, but in 1.2beta4 StringWriter and
        // other writers throw RuntimeExceptions
        char[] cbuf = new char[2];
        wtr.close();
        System.out.println("Class " + wtr.getClass().getName());

        try {
            wtr.write('a');
            System.out.println("FAILED: Allows char write on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        try {
            wtr.write(cbuf, 0, 1);
            System.out.println("FAILED: Allows buffer write on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        try {
            wtr.write(cbuf, 0, 0);
            System.out.println("FAILED: Allows empty write on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        try {
            wtr.write("a");
            System.out.println("FAILED: Allows string write on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        try {
            wtr.write("a", 0, 1);
            System.out.println("FALIED: Allows string buf write on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        try {
            wtr.flush();
            System.out.println("FAILED: Allows flushing writer on a closed stream");
            failed = true;
        } catch (Exception e) {
        }

        wtr.close();
    }

    public static void main(String argv[]) throws Exception {
        StringWriter sw = new StringWriter();
        testWrite(new BufferedWriter(sw));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(bos);
        testWrite(osw);

        File f = new File(System.getProperty("test.dir", "."),
                          "NewFile");
        f.createNewFile();
        f.deleteOnExit();
        FileWriter fr = new FileWriter(f);
        testWrite(fr);

        if (failed) {
            throw new Exception("The test failed because one of the"
                +  " writer operation{s} failed. Check the messages");
        }
    }
}
