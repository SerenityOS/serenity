/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test       OnThrowTest.java
 * @bug        6263814
 * @summary    Test for -agentlib::[onthrow,launch]
 * @author     Kelly O'Hair
 *
 * @run compile -g OnThrowTest.java
 * @run compile -g OnThrowTarget.java
 * @run compile -g VMConnection.java
 * @run driver OnThrowTest
 */

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedInputStream;
import java.io.IOException;

public class OnThrowTest extends Object {

    /* Full pathname for file to be touched or created by the launch */
    private String touchFile;

    OnThrowTest() throws Exception {
        /* Name of touch file */
        touchFile = System.getProperty("test.classes") +
                    File.separator + "OnThrowLaunchTouchFile";
        /* Make sure it's gone when we start */
        File f = new File(touchFile);
        f.delete();
        if ( f.exists() ) {
            throw new Exception("Test failed: Cannot remove old touch file: " +
                  touchFile);
        }
    }

    /* Used to see if touch file exists */
    private boolean touchFileExists() {
        File f = new File(touchFile);
        return f.exists();
    }

    /**
     * Run an arbitrary command
     *
     */
    public void run(String[] cmdStrings) throws Exception {
        StringBuffer stdoutBuffer = new StringBuffer();
        StringBuffer stderrBuffer = new StringBuffer();
        String CR = System.getProperty("line.separator");
        int subprocessStatus = 1;

        System.out.print(CR + "runCommand method about to execute: ");
        for (int iNdx = 0; iNdx < cmdStrings.length; iNdx++) {
            System.out.print(" ");
            System.out.print(cmdStrings[iNdx]);
        }
        System.out.println(CR);

        try {
            Process process = Runtime.getRuntime().
                exec(VMConnection.insertDebuggeeVMOptions(cmdStrings));
            int BUFFERSIZE = 4096;
            /*
             * Gather up the output of the subprocess using non-blocking
             * reads so we can get both the subprocess stdout and the
             * subprocess stderr without overfilling any buffers.
             */
            BufferedInputStream is =
                new BufferedInputStream(process.getInputStream());
            int isLen = 0;
            byte[] isBuf = new byte[BUFFERSIZE];

            BufferedInputStream es =
                new BufferedInputStream(process.getErrorStream());
            int esLen = 0;
            byte[] esBuf = new byte[BUFFERSIZE];

            do {
                isLen = is.read(isBuf);
                if (isLen > 0) {
                    stdoutBuffer.append(new String(isBuf, 0, isLen));
                }
                esLen = es.read(esBuf);
                if (esLen > 0) {
                    stderrBuffer.append(new String(esBuf, 0, esLen));
                }
            } while ((isLen > -1) || (esLen > -1));

            try {
                process.waitFor();
                subprocessStatus = process.exitValue();
                process = null;
            } catch(java.lang.InterruptedException e) {
                System.err.println("InterruptedException: " + e);
                throw new Exception("Test failed: process interrupted");
            }

        } catch(IOException ex) {
            System.err.println("IO error: " + ex);
            throw new Exception("Test failed: IO error running process");
        }

        System.out.println(CR + "--- Return code was: " +
                           CR + Integer.toString(subprocessStatus));
        System.out.println(CR + "--- Return stdout was: " +
                           CR + stdoutBuffer.toString());
        System.out.println(CR + "--- Return stderr was: " +
                           CR + stderrBuffer.toString());

    }

    public static void main(String[] args) throws Exception {

        OnThrowTest myTest = new OnThrowTest();

        String launch = System.getProperty("test.classes") +
                        File.separator + "OnThrowLaunch.sh";
        File f = new File(launch);
        f.delete();
        FileWriter fw = new FileWriter(f);
        fw.write("#!/bin/sh\n echo OK $* > " +
                 myTest.touchFile.replace('\\','/') + "\n exit 0\n");
        fw.flush();
        fw.close();
        if ( ! f.exists() ) {
            throw new Exception("Test failed: sh file not created: " + launch);
        }

        String javaExe = System.getProperty("java.home") +
                         File.separator + "bin" + File.separator + "java";
        String targetClass = "OnThrowTarget";
        String cmds [] = {javaExe,
                          "-agentlib:jdwp=transport=dt_socket," +
                          "onthrow=OnThrowException,server=y,suspend=n," +
                          "launch=" + "sh " + launch.replace('\\','/'),
                          targetClass};

        /* Run the target app, which will launch the launch script */
        myTest.run(cmds);
        if ( !myTest.touchFileExists() ) {
            throw new Exception("Test failed: touch file not found: " +
                  myTest.touchFile);
        }

        System.out.println("Test passed: launch create file");
    }

}
