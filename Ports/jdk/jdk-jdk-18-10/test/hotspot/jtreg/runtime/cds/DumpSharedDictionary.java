/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8130072
 * @summary Check that Shared Dictionary is printed out with jcmd
 * @requires vm.cds
 * @library /test/lib
 * @run driver DumpSharedDictionary
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.JDKToolFinder;

public class DumpSharedDictionary {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Start this process
            CDSOptions opts = (new CDSOptions())
                .setArchiveName("./DumpSharedDictionary.jsa");
            CDSTestUtils.createArchiveAndCheck(opts);

            String testjdkPath = System.getProperty("test.jdk");

            opts = (new CDSOptions())
                .setUseVersion(false)
                .addSuffix("-XX:SharedArchiveFile=./DumpSharedDictionary.jsa",
                           "-Dtest.jdk=" + testjdkPath,
                           "DumpSharedDictionary", "test");
            CDSTestUtils.run(opts)
                        .assertNormalExit();
        } else {
            // Grab my own PID
            String pid = Long.toString(ProcessTools.getProcessId());

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(new String[] {JDKToolFinder.getJDKTool("jcmd"), pid, "VM.systemdictionary"});
            OutputAnalyzer output = CDSTestUtils.executeAndLog(pb, "jcmd-systemdictionary");
            try {
                output.shouldContain("Shared Dictionary statistics:");
                output.shouldContain("Number of buckets");
                output.shouldContain("Number of entries");
                output.shouldContain("Maximum bucket size");
            } catch (RuntimeException e) {
                output.shouldContain("Unknown diagnostic command");
            }

            pb.command(new String[] {JDKToolFinder.getJDKTool("jcmd"), pid, "VM.systemdictionary", "-verbose"});
            output = CDSTestUtils.executeAndLog(pb, "jcmd-systemdictionary-verbose");
            try {
                output.shouldContain("Shared Dictionary");
                output.shouldContain("Dictionary for loader data: 0x");
                output.shouldContain("^java.lang.String");
            } catch (RuntimeException e) {
                output.shouldContain("Unknown diagnostic command");
            }
        }
    }
}
