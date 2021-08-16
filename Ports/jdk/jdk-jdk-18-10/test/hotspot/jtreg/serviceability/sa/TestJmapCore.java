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
 * @test TestJmapCore
 * @summary Test verifies that jhsdb jmap could generate heap dump from core when heap is full
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver/timeout=240 TestJmapCore run heap
 */

import java.io.File;

import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.classloader.GeneratingClassLoader;
import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.CoreUtils;
import jtreg.SkippedException;

public class TestJmapCore {
    public static final String HEAP_OOME = "heap";
    public static final String METASPACE_OOME = "metaspace";


    public static void main(String[] args) throws Throwable {
        if (args.length == 1) {
            try {
                if (args[0].equals(HEAP_OOME)) {
                    Object[] oa = new Object[Integer.MAX_VALUE / 2];
                    for(int i = 0; i < oa.length; i++) {
                        oa[i] = new Object[Integer.MAX_VALUE / 2];
                    }
                } else {
                    GeneratingClassLoader loader = new GeneratingClassLoader();
                    for (int i = 0; ; i++) {
                        loader.loadClass(loader.getClassName(i));
                    }
                }
                throw new Error("OOME not triggered");
            } catch (OutOfMemoryError err) {
                return;
            }
        }
        test(args[1]);
    }

    static void test(String type) throws Throwable {
        ProcessBuilder pb = ProcessTools.createTestJvm("-XX:+CreateCoredumpOnCrash",
                "-Xmx512m", "-XX:MaxMetaspaceSize=64m", "-XX:+CrashOnOutOfMemoryError",
                TestJmapCore.class.getName(), type);

        // If we are going to force a core dump, apply "ulimit -c unlimited" if we can.
        pb = CoreUtils.addCoreUlimitCommand(pb);
        OutputAnalyzer output = ProcessTools.executeProcess(pb);

        String coreFileName = CoreUtils.getCoreFileLocation(output.getStdout(), output.pid());
        File core = new File(coreFileName);
        File dumpFile = new File("heap.hprof");
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        launcher.addToolArg("jmap");
        launcher.addToolArg("--binaryheap");
        launcher.addToolArg("--dumpfile=" + dumpFile);
        launcher.addToolArg("--exe");
        launcher.addToolArg(JDKToolFinder.getTestJDKTool("java"));
        launcher.addToolArg("--core");
        launcher.addToolArg(core.getPath());

        ProcessBuilder jhsdpb = new ProcessBuilder();
        jhsdpb.command(launcher.getCommand());
        Process jhsdb = jhsdpb.start();
        OutputAnalyzer out = new OutputAnalyzer(jhsdb);

        jhsdb.waitFor();

        System.out.println(out.getStdout());
        System.err.println(out.getStderr());

        if (dumpFile.exists() && dumpFile.isFile()) {
            HprofParser.parse(dumpFile);
        } else {
          throw new RuntimeException(
            "Could not find dump file " + dumpFile.getAbsolutePath());
        }

        System.out.println("PASSED");
    }
}
