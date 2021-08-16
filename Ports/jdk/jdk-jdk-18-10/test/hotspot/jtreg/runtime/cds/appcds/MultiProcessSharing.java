/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Run multiple processes with the same archive, ensure they share
 *
 * @requires vm.cds
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile test-classes/MultiProcClass.java
 * @run driver MultiProcessSharing
 */

import java.io.File;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;


public class MultiProcessSharing {
    static String useWbJar;
    static String sharedClass1Jar;
    static boolean checkPmap = false;

    public static void main(String[] args) throws Exception {
        String wbJar = JarBuilder.build(true, "WhiteBox", "sun/hotspot/WhiteBox");
        useWbJar = "-Xbootclasspath/a:" + wbJar;
        sharedClass1Jar = JarBuilder.build("shared_class1", "MultiProcClass");

        // create an archive
        OutputAnalyzer out = TestCommon.dump(sharedClass1Jar,
            TestCommon.list("MultiProcClass"), useWbJar);
        TestCommon.checkDump(out);

        // determine whether OK to use pmap for extra test verification
        long myPid = ProcessHandle.current().pid();
        checkPmap = (Platform.isLinux() && (MultiProcClass.runPmap(myPid, false) == 0));
        System.out.println("MultiProcessSharing: checkPmap is " + checkPmap);

        // use an archive in several processes concurrently
        int numProcesses = 3;
        Thread[] threads = new Thread[numProcesses];
        ProcessHandler[] processHandlers = new ProcessHandler[numProcesses];
        for (int i = 0; i < numProcesses; i++) {
            processHandlers[i] = new ProcessHandler(i);
            threads[i] = new Thread(processHandlers[i]);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ie) {
                throw ie;
            }
        }

        // check results
        for (ProcessHandler ph : processHandlers) {
            TestCommon.checkExec(ph.out);
            if (checkPmap && !TestCommon.isUnableToMap(ph.out)) {
                checkPmapOutput(ph.out.getOutput());
            }
        }
    }


    static class ProcessHandler implements Runnable {
        int processNumber;
        OutputAnalyzer out;

        ProcessHandler(int processNumber) {
            this.processNumber = processNumber;
        }

        @Override
        public void run() {
            try {
                out = TestCommon.exec(sharedClass1Jar,
                   "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI", useWbJar,
                   "MultiProcClass", "" + processNumber, "" + checkPmap);
            } catch (Exception e) {
                throw new RuntimeException("Error occurred when using archive, exec()" + e);
            }
        }
    }


    private static void checkPmapOutput(String stdio) {
        System.out.println("Checking pmap output ...");
        String[] lines = stdio.split("\n");

        boolean foundJsa = false;
        boolean foundReadOnlyJsaSection = false;

        for (String line : lines) {
            if (line.contains(TestCommon.getCurrentArchiveName()))
                System.out.println(line);
                foundJsa = true;
                if (line.contains("r--")) {
                    foundReadOnlyJsaSection = true;
                }

                // On certain ARM platforms system maps r/o memory mapped files
                // as r/x; see JDK-8145694 for details
                if ( (Platform.isARM() || Platform.isAArch64()) && line.contains("r-x") ) {
                    foundReadOnlyJsaSection = true;
                }
        }

        Asserts.assertTrue(foundJsa && foundReadOnlyJsaSection);
    }

}
