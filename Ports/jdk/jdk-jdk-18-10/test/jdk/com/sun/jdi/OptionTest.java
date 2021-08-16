/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test       OptionTest
 * @bug        5095072
 * @summary    Test for misc jdwp options, just that the option is parsed
 * @author     Kelly O'Hair (copied from Tim Bell's NoLaunchOptionTest)
 *
 * @run compile -g OptionTest.java
 * @run compile -g HelloWorld.java
 * @run compile -g VMConnection.java
 * @run driver OptionTest
 */

import java.util.regex.Pattern;

public class OptionTest extends Object {
    private static final Pattern TRANSPORT_ERROR_PTRN = Pattern.compile("^ERROR: transport error .+$", Pattern.MULTILINE);
    private int subprocessStatus;
    private static final String CR = System.getProperty("line.separator");
    private static final int BUFFERSIZE = 4096;
    public static final int RETSTAT = 0;
    public static final int STDOUT = 1;
    public static final int STDERR = 2;

    /**
     * Run an arbitrary command and return the results to caller.
     *
     * @param an array of String containing the command
     *        to run and any flags or parameters to the command.
     *
     * @return completion status, stderr and stdout as array of String
     *  Look for:
     *    return status in result[OptionTest.RETSTAT]
     *    standard out in result[OptionTest.STDOUT]
     *    standard err in result[OptionTest.STDERR]
     *
     */
    public String[] run (String[] cmdStrings) {
        StringBuffer stdoutBuffer = new StringBuffer();
        StringBuffer stderrBuffer = new StringBuffer();

        System.out.print(CR + "runCommand method about to execute: ");
        for (int iNdx = 0; iNdx < cmdStrings.length; iNdx++) {
            System.out.print(" ");
            System.out.print(cmdStrings[iNdx]);
        }
        System.out.println(CR);
        try {
            Process process = Runtime.getRuntime().exec(cmdStrings);
            /*
             * Gather up the output of the subprocess using non-blocking
             * reads so we can get both the subprocess stdout and the
             * subprocess stderr without overfilling any buffers.
             */
            java.io.BufferedInputStream is =
                new java.io.BufferedInputStream(process.getInputStream());
            int isLen = 0;
            byte[] isBuf = new byte[BUFFERSIZE];

            java.io.BufferedInputStream es =
                new java.io.BufferedInputStream(process.getErrorStream());
            int esLen = 0;
            byte[] esBuf = new byte[BUFFERSIZE];

            do {
                isLen = is.read(isBuf);
                if (isLen > 0) {
                    stdoutBuffer.append(
                                        new String(isBuf, 0, isLen));
                }
                esLen = es.read(esBuf);
                if (esLen > 0) {
                    stderrBuffer.append(
                                        new String(esBuf, 0, esLen));
                }
            } while ((isLen > -1) || (esLen > -1));
            try {
                process.waitFor();
                subprocessStatus = process.exitValue();
                process = null;
            } catch(java.lang.InterruptedException e) {
                System.err.println("InterruptedException: " + e);
            }

        } catch(java.io.IOException ex) {
            System.err.println("IO error: " + ex);
        }
        String[] result =
            new String[] {
                Integer.toString(subprocessStatus),
                stdoutBuffer.toString(),
                stderrBuffer.toString()
        };

        System.out.println(CR + "--- Return code was: " +
                           CR + result[RETSTAT]);
        System.out.println(CR + "--- Return stdout was: " +
                           CR + result[STDOUT]);
        System.out.println(CR + "--- Return stderr was: " +
                           CR + result[STDERR]);

        return result;
    }

    public static void main(String[] args) throws Exception {
        String javaExe = System.getProperty("java.home") +
            java.io.File.separator + "bin" +
            java.io.File.separator + "java";
        String targetClass = "HelloWorld";
        String baseOptions = "transport=dt_socket" +
                              ",address=0" +
                              ",server=y" +
                              ",suspend=n";

        /* Option combinations to try (combos faster, fewer exec's) */
        String options[] =  {
                "timeout=0,mutf8=y,quiet=y,stdalloc=y,strict=n",
                "timeout=200000,mutf8=n,quiet=n,stdalloc=n,strict=y"
                };

        for ( String option : options) {
            String cmds [] = {javaExe,
                              "-agentlib:jdwp=" + baseOptions + "," + option,
                              targetClass};
            OptionTest myTest = new OptionTest();
            String results [] = myTest.run(VMConnection.insertDebuggeeVMOptions(cmds));
            if (!(results[RETSTAT].equals("0")) ||
                (TRANSPORT_ERROR_PTRN.matcher(results[STDERR]).find())) {
                throw new Exception("Test failed: jdwp doesn't like " + cmds[1]);
            }
        }

        System.out.println("Testing invalid address string");

        // Test invalid addresses
        String badAddresses[] = {
            ":",
            "localhost:",
            "localhost:abc",
            "localhost:65536",
            "localhost:65F"
        };

        for (String badAddress : badAddresses) {

            String badOptions = "transport=dt_socket" +
                              ",address=" + badAddress +
                              ",server=y" +
                              ",suspend=n";
            String cmds[] = {javaExe, "-agentlib:jdwp=" + badOptions, targetClass};
            OptionTest myTest = new OptionTest();
            String results[] = myTest.run(VMConnection.insertDebuggeeVMOptions(cmds));

            if (!results[RETSTAT].equals("0") && TRANSPORT_ERROR_PTRN.matcher(results[STDERR]).find()) {
                // We got expected error, test passed
            }
            else {
                throw new Exception("Test failed: jdwp accept invalid address '" + badAddress + "'");
            }
        }

        System.out.println("Test passed: status = 0");
    }
}

