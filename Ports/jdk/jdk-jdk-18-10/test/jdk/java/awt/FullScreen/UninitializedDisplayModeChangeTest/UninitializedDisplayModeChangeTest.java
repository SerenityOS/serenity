/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6358034 6568560 8198613 8198335
 * @key headful
 * @summary Tests that no exception is thrown when display mode is changed
 *          externally
 * @compile UninitializedDisplayModeChangeTest.java DisplayModeChanger.java
 * @run main/othervm UninitializedDisplayModeChangeTest
 * @run main/othervm -Djava.awt.headless=true UninitializedDisplayModeChangeTest
 */

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

public class UninitializedDisplayModeChangeTest {
    public static volatile boolean failed = false;
    public static void main(String[] args) {
        Toolkit.getDefaultToolkit();
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    Thread.currentThread().setDefaultUncaughtExceptionHandler(
                        new Thread.UncaughtExceptionHandler() {
                            public void uncaughtException(Thread t,
                                                          Throwable e)
                            {
                                System.err.println("Exception Detected:");
                                e.printStackTrace();
                                failed = true;
                            }
                        }
                    );
                }
            });
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }

        Process childProc;
        String classPath = System.getProperty("java.class.path" , ".");
        String cmd = new String(System.getProperty("java.home") +
                File.separator +
                "bin" +
                File.separator +
                "java -cp " + classPath +
                " DisplayModeChanger");

        System.out.println("Launching the display mode changer process");
        System.out.println("cmd="+cmd);
        try {
            childProc = Runtime.getRuntime().exec(cmd);
            StreamProcessor err =
                new StreamProcessor("stderr", childProc.getErrorStream());
            StreamProcessor out =
                new StreamProcessor("stdout", childProc.getInputStream());
            err.start();
            out.start();

            childProc.waitFor();
        } catch (Exception e) {
            failed = true;
            e.printStackTrace();
        }

        if (failed) {
            throw new RuntimeException("Test Failed: exception detected");
        }
        System.out.println("Test Passed.");
    }

    static class StreamProcessor extends Thread {
        InputStream is;
        String inputType;
        StreamProcessor(String inputType, InputStream is) {
            this.inputType = inputType;
            this.is = is;
        }
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ( (line = br.readLine()) != null) {
                    System.out.println("Display Changer "+inputType+
                                       " output > " + line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
