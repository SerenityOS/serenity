/*
 * Copyright (c) 2019, Google Inc. All rights reserved.
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.ProcessBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class T {
    public static boolean run() {
        boolean res = false;
        String echoInput = "foo";
        ProcessBuilder pb = new ProcessBuilder("echo", echoInput);

        try {
            // Starting a ProcessBuilder causes the process reaper thread to be
            // created. The process reaper thread has small stack size. In JDK
            // 13, the REAPER_DEFAULT_STACKSIZE is 128K. With JDK 8, it is 32K.
            // Using the process reaper thread can demonstrate the TLS problem.
            // The reaper thread can fail with StackOverflow in one of the
            // failure mode with certain TLS sizes. In another observed
            // failure mode the VM fails to create a thread with error message
            // 'Failed to start thread - pthread_create failed'.
            System.out.println("Starting a new process ...");
            Process process = pb.start();
            process.waitFor();
            String echoOutput = output(process.getInputStream());
            System.out.println("Echo Output: " + echoOutput);
            if (echoOutput.equals(echoInput)) {
                res = true;
            } else {
                // 'res' is false, fail
                System.out.println("Unexpected Echo output: " + echoOutput +
                                   ", expects: " + echoInput);
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return res;
    }

    private static String output(InputStream inputStream) throws IOException {
        String s = "";
        try (BufferedReader br =
                 new BufferedReader(new InputStreamReader(inputStream))) {
            s = br.readLine();
        }
        return s;
    }
}

