/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @summary Test J2Ddemo.jar
 * @run main/timeout=200 J2DdemoTest
 */

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.PrintStream;

public class J2DdemoTest {

    public static void main(String args[]) throws Exception {
        DemoRun test;

        /* Run the J2Ddemo.jar with the -runs=1 option */
        test = new DemoRun("jfc/J2Ddemo/J2Ddemo.jar", "-runs=1");
        test.runit();

        /* Make sure patterns in output look ok */
        if (test.output_contains("ERROR")) {
            throw new RuntimeException("Test failed - ERROR seen in output");
        }
        if (test.output_contains("Exception")) {
            throw new RuntimeException("Test failed - Exception seen in output");
        }

        /* Must be a pass. */
        System.out.println("Test passed - cleanly terminated");
    }

    /*
     * Helper class to direct process output to a StringBuffer
     */
    static class MyInputStream implements Runnable {
        private String              name;
        private BufferedInputStream in;
        private StringBuffer        buffer;

        /* Create MyInputStream that saves all output to a StringBuffer */
        MyInputStream(String name, InputStream in) {
            this.name = name;
            this.in = new BufferedInputStream(in);
            buffer = new StringBuffer(4096);
            Thread thr = new Thread(this);
            thr.setDaemon(true);
            thr.start();
        }

        /* Dump the buffer */
        void dump(PrintStream x) {
            String str = buffer.toString();
            x.println("<beginning of " + name + " buffer>");
            x.println(str);
            x.println("<end of buffer>");
        }

        /* Check to see if a pattern is inside the output. */
        boolean contains(String pattern) {
            String str = buffer.toString();
            return str.contains(pattern);
        }

        /* Runs as a separate thread capturing all output in a StringBuffer */
        public void run() {
            try {
                byte b[] = new byte[100];
                for (;;) {
                    int n = in.read(b);
                    String str;
                    if (n < 0) {
                        break;
                    }
                    str = new String(b, 0, n);
                    buffer.append(str);
                    System.out.print(str);
                }
            } catch (IOException ioe) { /* skip */ }
        }
    }

    /*
     * Generic run of a demo jar file.
     */
    static class DemoRun {

        private String        demo_name;
        private String        demo_options;
        private MyInputStream output;
        private MyInputStream error;

        /* Create a Demo run process */
        public DemoRun(String name, String options)
        {
            demo_name    = name;
            demo_options = options;
        }

        /*
         * Execute the demo
         */
        public void runit()
        {
            String jre_home  = System.getProperty("java.home");
            String sdk_home  = (jre_home.endsWith("jre") ?
                                (jre_home + File.separator + "..") :
                                jre_home );
            String java      = sdk_home
                                 + File.separator + "bin"
                                 + File.separator + "java";

            /* VM options */
            String vm_opts[] = new String[0];
            String vopts = System.getProperty("test.vm.opts");
            if ( vopts != null && vopts.length()>0 ) {
                vm_opts = vopts.split("\\p{Space}+");
            } else {
                vm_opts = new String[0];
            }

            /* Command line */
            String cmd[] = new String[1 + vm_opts.length + 3];
            String cmdLine;
            int i;

            i = 0;
            cmdLine = "";
            cmdLine += (cmd[i++] = java);
            cmdLine += " ";
            for ( String vopt : vm_opts ) {
                cmdLine += (cmd[i++] = vopt);
                cmdLine += " ";
            }
            cmdLine += (cmd[i++] = "-jar");
            cmdLine += " ";
            String demo_path;
            String test_dir = System.getenv("TEST_IMAGE_DIR");
            System.out.println("TEST_IMAGE_DIR="+test_dir);
            if (test_dir != null) {
                demo_path = test_dir + File.separator + "jdk" + File.separator +
                            "demos" + File.separator + demo_name;
            } else {
                demo_path = sdk_home + File.separator +
                            "demo" + File.separator + demo_name;
            }
            System.out.println("demo_path="+demo_path);
            cmdLine += cmd[i++] = demo_path;
            cmdLine += " ";
            cmdLine += (cmd[i++] = demo_options);

            /* Begin process */
            Process p;

            System.out.println("Starting: " + cmdLine);
            try {
                p = Runtime.getRuntime().exec(cmd);
            } catch ( IOException e ) {
                throw new RuntimeException("Test failed - exec got IO exception");
            }

            /* Save process output in StringBuffers */
            output = new MyInputStream("Input Stream", p.getInputStream());
            error  = new MyInputStream("Error Stream", p.getErrorStream());

            /* Wait for process to complete, and if exit code is non-zero we fail */
            int exitStatus;
            try {
                exitStatus = p.waitFor();
                if ( exitStatus != 0) {
                    System.out.println("Exit code is " + exitStatus);
                    error.dump(System.out);
                    output.dump(System.out);
                    throw new RuntimeException("Test failed - " +
                                        "exit return code non-zero " +
                                        "(exitStatus==" + exitStatus + ")");
                }
            } catch ( InterruptedException e ) {
                throw new RuntimeException("Test failed - process interrupted");
            }
            System.out.println("Completed: " + cmdLine);
        }

        /* Does the pattern appear in the output of this process */
        public boolean output_contains(String pattern)
        {
            return output.contains(pattern) || error.contains(pattern);
        }
    }

}

