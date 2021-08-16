/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4180429
   @summary Lossage in dup2 if System.in is closed.
   @run main/othervm Duped
 */

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.File;

public class Duped {

    public static class Echo {
        public static void main(String args[]) throws Exception {
            StringBuffer s = new StringBuffer();
            int c;
            while ((c = System.in.read()) != -1)
                s.append((char)c);
            System.out.println(s);
        }
    }

    public static void main(String[] args) throws Exception {

        String command =
            System.getProperty("java.home") +
            File.separator +
            "bin" +
            File.separator +
            "java -classpath " +
            System.getProperty("java.class.path") +
            " Duped$Echo";

        if (args.length == 1 && args[0].equals("-dont")) {
            /*
             * To quickly check that this test is working when it is
             * supposed to, just run it with -dont and it shouldn't
             * complain at all.
             */
        } else {
            /*
             * In normal runs we just close in, and that causes
             * lossage on fork.
             */
            System.in.close();
        }

        Process p = Runtime.getRuntime().exec(command);
        PrintStream out = new PrintStream(p.getOutputStream());
        out.println(HELLO);
        out.close();

        BufferedReader in =
            new BufferedReader(new InputStreamReader(p.getInputStream()));
        String read = in.readLine();

        if (!HELLO.equals(read)) {
            throw new Exception("Failed, read ``" + read + "''");
        }
    }

    static final String HELLO = "Hello, world!";

}
