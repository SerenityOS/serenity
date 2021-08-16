/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4033560
   @summary 4033560 limited args of exec to 198 on Solaris. We check
            that we can actually exec more args than that.
   @author Anand Palaniswamy
   @run main/othervm ExecWithLotsOfArgs
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;

public class ExecWithLotsOfArgs {

    public static class EchoingHelper {
        public static void main(String[] args) {
            for (int i = 0; i < args.length; i++) {
                System.out.println(args[i]);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String[] command = new String[300];
        int n = 0;

        /*
         * The Java program to exec. This is slightly fragile. Works
         * on Solaris and Win32.
         */
        command[n++] = System.getProperty("java.home") + File.separator +
            "bin" + File.separator + "java";
        if (System.getProperty("java.class.path") != null) {
            command[n++] = "-classpath";
            command[n++] = System.getProperty("java.class.path");
        }

        /*
         * The class with main() that the exec'd VM will run.
         */
        command[n++] = "ExecWithLotsOfArgs$EchoingHelper";

        /*
         * Make a long set of args n, n + 1, ... , 300.
         */
        for (int i = n; i < command.length; i++) {
            command[i] = new String(new Integer(i).toString());
        }

        /*
         * Do the exec.
         */
        Process p = null;
        p = Runtime.getRuntime().exec(command);
        BufferedReader in = new BufferedReader
            (new InputStreamReader(p.getInputStream()));

        /*
         * Read back all the strings and that the same were returned.
         */
        String s;
        int count = n;
        while ((s = in.readLine()) != null) {
            if (count >= command.length) {
                failed("Was expecting " + (command.length - 2) +
                       " strings to be echo'ed back, but got " +
                       (count - 1) + " instead");
            }
            if (!s.equals(command[count])) {
                failed("Exec'd process returned \"" +
                       s + "\", was expecting \""  +
                       command[count] + "\"");
            }
            count++;
        }

        /*
         * Did we read anything at all?
         */
        if (count == n) {
            /* Try reading the error stream to see if we got any diagnostics */
            in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = in.readLine()) != null) {
                System.err.println("Error output: " + s);
            }
            failed("Exec'd process didn't writing anything to its stdout");
        }
    }

    private static void failed(String s) {
        throw new RuntimeException("Failed: " + s);
    }
}
