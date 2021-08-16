/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @test
 * @key headful
 * @bug 8081019
 * @summary Check peer to null in CPlatformWindow.checkZoom() method
 * @author Alexandr Scherbatiy
 */
public class bug8081019 {

    private static final String RUN_PROCESS = "RUN_PROCESS";
    private static final String RUN_TEST = "RUN_TEST";

    public static void main(String[] args) throws Exception {
        String command = RUN_PROCESS;

        if (0 < args.length) {
            command = args[0];
        }

        switch (command) {
            case RUN_PROCESS:
                runProcess();
                break;
            case RUN_TEST:
                runTest();
                break;
            default:
                throw new RuntimeException("Unknown command: " + command);
        }
    }

    private static void runTest() throws Exception {
        System.setSecurityManager(new SecurityManager());
        Frame f = new Frame("Test frame");
        f.setVisible(true);
        f.setVisible(false);
        f.dispose();
    }

    private static void runProcess() throws Exception {
        String javaPath = System.getProperty("java.home", "");
        String command = javaPath + File.separator + "bin" + File.separator + "java"
                + " -Djava.security.manager=allow " + bug8081019.class.getName() + " " + RUN_TEST;

        Process process = Runtime.getRuntime().exec(command);
        boolean processExit = process.waitFor(20, TimeUnit.SECONDS);

        dumpStream(process.getErrorStream(), "error stream");
        dumpStream(process.getInputStream(), "input stream");

        if (!processExit) {
            process.destroy();
            throw new RuntimeException(""
                    + "The sub process has not exited!");
        }
    }

    public static void dumpStream(InputStream in, String name) throws IOException {
        System.out.println("--- dump " + name + " ---");
        String tempString;
        int count = in.available();
        boolean exception = false;
        while (count > 0) {
            byte[] b = new byte[count];
            in.read(b);
            tempString = new String(b);
            if (!exception) {
                exception = tempString.indexOf("Exception") != -1
                        || tempString.indexOf("Error") != -1;
            }
            System.out.println(tempString);
            count = in.available();
        }

        if (exception) {
            throw new RuntimeException("Exception in the output!");
        }
    }
}
