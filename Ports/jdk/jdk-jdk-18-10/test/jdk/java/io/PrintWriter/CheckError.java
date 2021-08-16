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
 * @bug 4213822
 * @summary Test that checkError() returns a correct value
 *      when a PrintWriter is wrapped with another
 *      PrintWriter.
 */

import java.io.*;

public class CheckError {
    public static void main(String[] args) throws Exception {

        boolean passTest1 = false;
        File file = new File(System.getProperty("test.dir", "."),
                          "junkie.out");

        FileWriter fw = new FileWriter(file);

        PrintWriter ppw  = new PrintWriter(
                           new PrintWriter(fw));

        fw.close();
        ppw.println("Hello World!");

        file.deleteOnExit();

        if (ppw.checkError()) {
            System.out.println("Correct: An error occured in the" +
                " underlying writer");
            passTest1 = true;
        }
        ppw.close();

        // Test when the underlying stream is a PrintStream
        FileOutputStream fos = new FileOutputStream(file);
        PrintWriter pps  = new PrintWriter(
                            new PrintStream(fos));

        fos.close();
        pps.println("Hello World!");

        if (pps.checkError()) {
            System.out.println("Correct: An error occured in the" +
                " underlying Stream");
        } else {
            if (!passTest1) {
                throw new Exception("CheckError() returned an incorrect value" +
                    " when error occured in the underlying Stream" +
                        " and when error occured in the underlying writer");
            } else {
                throw new Exception("CheckError() returned an incorrect value" +
                    " when the error has occured in the underlying Stream");
            }
        }
        if (!passTest1) {
                throw new Exception("CheckError() returned an incorrect value" +
                    " when the error has occured in the underlying Writer");
        }
        pps.close();
    }
}
