/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestCrashOnOutOfMemoryError
 * @summary Test using -XX:+CrashOnOutOfMemoryError
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestCrashOnOutOfMemoryError
 * @bug 8138745
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

public class TestCrashOnOutOfMemoryError {

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            // This should guarantee to throw:
            // java.lang.OutOfMemoryError: Requested array size exceeds VM limit
            try {
                Object[] oa = new Object[Integer.MAX_VALUE];
                throw new Error("OOME not triggered");
            } catch (OutOfMemoryError err) {
                throw new Error("OOME didn't abort JVM!");
            }
        }
        // else this is the main test
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+CrashOnOutOfMemoryError",
                 "-XX:-CreateCoredumpOnCrash", "-Xmx128m", TestCrashOnOutOfMemoryError.class.getName(),"throwOOME");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        int exitValue = output.getExitValue();
        if (0 == exitValue) {
            //expecting a non zero value
            throw new Error("Expected to get non zero exit value");
        }

        /* Output should look something like this. The actual text will depend on the OS and its core dump processing.
           Aborting due to java.lang.OutOfMemoryError: Requested array size exceeds VM limit
           # To suppress the following error report, specify this argument
           # after -XX: or in .hotspotrc:  SuppressErrorAt=/debug.cpp:303
           #
           # A fatal error has been detected by the Java Runtime Environment:
           #
           #  Internal Error (/home/cheleswer/Desktop/jdk9/dev/hotspot/src/share/vm/utilities/debug.cpp:303), pid=6212, tid=6213
           #  fatal error: OutOfMemory encountered: Requested array size exceeds VM limit
           #
           # JRE version: OpenJDK Runtime Environment (9.0) (build 1.9.0-internal-debug-cheleswer_2015_10_20_14_32-b00)
           # Java VM: OpenJDK 64-Bit Server VM (1.9.0-internal-debug-cheleswer_2015_10_20_14_32-b00, mixed mode, tiered, compressed oops, serial gc, linux-amd64)
           # Core dump will be written. Default location: Core dumps may be processed with "/usr/share/apport/apport %p %s %c %P" (or dumping to
             /home/cheleswer/Desktop/core.6212)
           #
           # An error report file with more information is saved as:
           # /home/cheleswer/Desktop/hs_err_pid6212.log
           #
           # If you would like to submit a bug report, please visit:
           #   http://bugreport.java.com/bugreport/crash.jsp
           #
           Current thread is 6213
           Dumping core ...
           Aborted (core dumped)
        */
        output.shouldContain("Aborting due to java.lang.OutOfMemoryError: Requested array size exceeds VM limit");
        // extract hs-err file
        String hs_err_file = output.firstMatch("# *(\\S*hs_err_pid\\d+\\.log)", 1);
        if (hs_err_file == null) {
            throw new Error("Did not find hs-err file in output.\n");
        }

        /*
         * Check if hs_err files exist or not
         */
        File f = new File(hs_err_file);
        if (!f.exists()) {
            throw new Error("hs-err file missing at "+ f.getAbsolutePath() + ".\n");
        }

        /*
         * Checking the completness of hs_err file. If last line of hs_err file is "END"
         * then it proves that file is complete.
         */
        try (FileInputStream fis = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            String line = null;
            String lastLine = null;
            while ((line = br.readLine()) != null) {
                lastLine = line;
            }
            if (!lastLine.equals("END.")) {
                throw new Error("hs-err file incomplete (missing END marker.)");
            } else {
                System.out.println("End marker found.");
            }
        }
        System.out.println("PASSED");
    }
}
