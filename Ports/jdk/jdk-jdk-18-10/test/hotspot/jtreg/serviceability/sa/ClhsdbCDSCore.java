/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174994 8200613
 * @summary Test the clhsdb commands 'printmdo', 'printall', 'jstack' on a CDS enabled corefile.
 * @requires vm.cds
 * @requires vm.hasSA
 * @requires vm.flavor == "server"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver/timeout=2400 ClhsdbCDSCore
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.internal.misc.Unsafe;

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.CoreUtils;
import jdk.test.lib.Utils;

import jtreg.SkippedException;

class CrashApp {
    public static void main(String[] args) {
        Unsafe.getUnsafe().putInt(0L, 0);
    }
}

public class ClhsdbCDSCore {
    private static final String SHARED_ARCHIVE_NAME = "ArchiveForClhsdbCDSCore.jsa";
    private static String coreFileName;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbCDSCore test");
        cleanup();

        try {
            CDSOptions opts = (new CDSOptions()).setArchiveName(SHARED_ARCHIVE_NAME);
            CDSTestUtils.createArchiveAndCheck(opts);

            String[] jArgs = {
                "-Xmx512m",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:SharedArchiveFile=" + SHARED_ARCHIVE_NAME,
                "-XX:+CreateCoredumpOnCrash",
                "-Xshare:auto",
                "-XX:+ProfileInterpreter",
                "--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED",
                CrashApp.class.getName()
            };

            OutputAnalyzer crashOutput;
            try {
               List<String> options = new ArrayList<>();
               options.addAll(Arrays.asList(jArgs));
               ProcessBuilder pb = ProcessTools.createTestJvm(options);
               // Add "ulimit -c unlimited" if we can since we are generating a core file.
               pb = CoreUtils.addCoreUlimitCommand(pb);
               crashOutput = ProcessTools.executeProcess(pb);
            } catch (Throwable t) {
               throw new Error("Can't execute the java cds process.", t);
            }

            try {
                coreFileName = CoreUtils.getCoreFileLocation(crashOutput.getStdout(), crashOutput.pid());
            } catch (Exception e) {
                cleanup();
                throw e;
            }

            ClhsdbLauncher test = new ClhsdbLauncher();

            // Ensure that UseSharedSpaces is turned on.
            List<String> cmds = List.of("flags UseSharedSpaces");

            String useSharedSpacesOutput = test.runOnCore(coreFileName, cmds, null, null);

            if (useSharedSpacesOutput == null) {
                // Output could be null due to attach permission issues.
                cleanup();
                throw new SkippedException("Could not determine the UseSharedSpaces value");
            }

            if (useSharedSpacesOutput.contains("UseSharedSpaces = false")) {
                // CDS archive is not mapped. Skip the rest of the test.
                cleanup();
                throw new SkippedException("The CDS archive is not mapped");
            }

            List testJavaOpts = Arrays.asList(Utils.getTestJavaOpts());

            if (testJavaOpts.contains("-XX:TieredStopAtLevel=1")) {
                // No MDOs are allocated in -XX:TieredStopAtLevel=1
                // The reason is methods being compiled aren't hot enough
                // Let's not call printmdo in such scenario
                cmds = List.of("printall", "jstack -v");
            } else {
                cmds = List.of("printmdo -a", "printall", "jstack -v");
            }

            Map<String, List<String>> expStrMap = new HashMap<>();
            Map<String, List<String>> unExpStrMap = new HashMap<>();
            expStrMap.put("printmdo -a", List.of(
                "CounterData",
                "BranchData"));
            unExpStrMap.put("printmdo -a", List.of(
                "No suitable match for type of address"));
            expStrMap.put("printall", List.of(
                "aload_0",
                "_nofast_aload_0",
                "_nofast_getfield",
                "_nofast_putfield",
                "Constant Pool of",
                "public static void main\\(java.lang.String\\[\\]\\)",
                "Bytecode",
                "invokevirtual",
                "checkcast",
                "Exception Table",
                "invokedynamic"));
            unExpStrMap.put("printall", List.of(
                "sun.jvm.hotspot.types.WrongTypeException",
                "illegal code",
                "Failure occurred at bci",
                "No suitable match for type of address"));
            expStrMap.put("jstack -v", List.of(
                "Common-Cleaner",
                "Method*"));
            unExpStrMap.put("jstack -v", List.of(
                "sun.jvm.hotspot.debugger.UnmappedAddressException"));
            test.runOnCore(coreFileName, cmds, expStrMap, unExpStrMap);
        } catch (SkippedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }
        cleanup();
        System.out.println("Test PASSED");
    }

    private static void cleanup() {
        remove(SHARED_ARCHIVE_NAME);
    }

    private static void remove(String item) {
        File toDelete = new File(item);
        toDelete.delete();
    }
}
