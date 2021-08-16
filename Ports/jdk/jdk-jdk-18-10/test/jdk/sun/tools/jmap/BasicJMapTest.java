/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.fail;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test id=Serial
 * @requires vm.gc.Serial
 * @summary Unit test for jmap utility (Serial GC)
 * @key intermittent
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main/othervm/timeout=240 -XX:+UseSerialGC BasicJMapTest
 */

/*
 * @test id=Parallel
 * @requires vm.gc.Parallel
 * @summary Unit test for jmap utility (Parallel GC)
 * @key intermittent
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main/othervm/timeout=240 -XX:+UseParallelGC BasicJMapTest
 */

/*
 * @test id=G1
 * @requires vm.gc.G1
 * @summary Unit test for jmap utility (G1 GC)
 * @key intermittent
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main/othervm/timeout=240 -XX:+UseG1GC BasicJMapTest
 */

/*
 * @test id=Shenandoah
 * @requires vm.gc.Shenandoah
 * @summary Unit test for jmap utility (Shenandoah GC)
 * @key intermittent
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main/othervm/timeout=240 -XX:+UseShenandoahGC BasicJMapTest
 */

/*
 * @test id=Z
 * @requires vm.gc.Z
 * @summary Unit test for jmap utility (Z GC)
 * @key intermittent
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main/othervm/timeout=240 -XX:+UseZGC BasicJMapTest
 */

public class BasicJMapTest {

    private static ProcessBuilder processBuilder = new ProcessBuilder();

    public static void main(String[] args) throws Exception {
        testHisto();
        testHistoLive();
        testHistoAll();
        testHistoParallelZero();
        testHistoParallel();
        testHistoNonParallel();
        testHistoToFile();
        testHistoLiveToFile();
        testHistoAllToFile();
        testFinalizerInfo();
        testClstats();
        testDump();
        testDumpLive();
        testDumpAll();
        testDumpCompressed();
        testDumpIllegalCompressedArgs();
    }

    private static void testHisto() throws Exception {
        OutputAnalyzer output = jmap("-histo:");
        output.shouldHaveExitValue(0);
        OutputAnalyzer output1 = jmap("-histo");
        output1.shouldHaveExitValue(0);
    }

    private static void testHistoLive() throws Exception {
        OutputAnalyzer output = jmap("-histo:live");
        output.shouldHaveExitValue(0);
    }

    private static void testHistoAll() throws Exception {
        OutputAnalyzer output = jmap("-histo:all");
        output.shouldHaveExitValue(0);
    }

    private static void testHistoParallelZero() throws Exception {
        OutputAnalyzer output = jmap("-histo:parallel=0");
        output.shouldHaveExitValue(0);
    }

    private static void testHistoParallel() throws Exception {
        OutputAnalyzer output = jmap("-histo:parallel=2");
        output.shouldHaveExitValue(0);
    }

    private static void testHistoNonParallel() throws Exception {
        OutputAnalyzer output = jmap("-histo:parallel=1");
        output.shouldHaveExitValue(0);
    }

    private static void testHistoToFile() throws Exception {
        histoToFile(false, false, 1);
    }

    private static void testHistoLiveToFile() throws Exception {
        histoToFile(true, false, 1);
    }

    private static void testHistoAllToFile() throws Exception {
        histoToFile(false, true, 1);
    }

    private static void testHistoFileParallelZero() throws Exception {
        histoToFile(false, false, 0);
    }

    private static void testHistoFileParallel() throws Exception {
        histoToFile(false, false, 2);
    }

    private static void histoToFile(boolean live,
                                    boolean explicitAll,
                                    int parallelThreadNum) throws Exception {
        String liveArg = "";
        String fileArg = "";
        String parArg = "parallel=" + parallelThreadNum;
        String allArgs = "-histo:";

        if (live && explicitAll) {
            fail("Illegal argument setting for jmap -histo");
        }
        if (live) {
            liveArg = "live,";
        }
        if (explicitAll) {
            liveArg = "all,";
        }

        File file = new File("jmap.histo.file" + System.currentTimeMillis() + ".histo");
        if (file.exists()) {
            file.delete();
        }
        fileArg = "file=" + file.getName();

        OutputAnalyzer output;
        allArgs = allArgs + liveArg + fileArg + ',' + parArg;
        output = jmap(allArgs);
        output.shouldHaveExitValue(0);
        output.shouldContain("Heap inspection file created");
        file.delete();
    }

    private static void testFinalizerInfo() throws Exception {
        OutputAnalyzer output = jmap("-finalizerinfo");
        output.shouldHaveExitValue(0);
    }

    private static void testClstats() throws Exception {
        OutputAnalyzer output = jmap("-clstats");
        output.shouldHaveExitValue(0);
    }

    private static void testDump() throws Exception {
        dump(false, false, false);
    }

    private static void testDumpLive() throws Exception {
        dump(true, false, false);
    }

    private static void testDumpAll() throws Exception {
        dump(false, true, false);
    }

    private static void testDumpCompressed() throws Exception {
        dump(true, false, true);
    }

    private static void testDumpIllegalCompressedArgs() throws Exception{
        dump(true, false, true, "0", 1, "Compression level out of range");
        dump(true, false, true, "100", 1, "Compression level out of range");
        dump(true, false, true, "abc", 1, "Invalid compress level");
        dump(true, false, true, "", 1, "Fail: no number provided in option:");
    }

    private static void dump(boolean live, boolean explicitAll, boolean compressed) throws Exception {
        dump(live, explicitAll, compressed, "1", 0, "Heap dump file created");
    }

    private static void dump(boolean live,
                             boolean explicitAll,
                             boolean compressed,
                             String compressLevel,
                             int expExitValue,
                             String expOutput) throws Exception {
        String liveArg = "";
        String fileArg = "";
        String compressArg = "";
        String allArgs = "-dump:";

        if (live && explicitAll) {
            fail("Illegal argument setting for jmap -dump");
        }
        if (live) {
            liveArg = "live,";
        }
        if (explicitAll) {
            liveArg = "all,";
        }

        String filePath = "jmap.dump" + System.currentTimeMillis() + ".hprof";
        if (compressed) {
            compressArg = "gz=" + compressLevel;
            filePath = filePath + ".gz";
        }

        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        fileArg = "file=" + file.getName() + ",";

        OutputAnalyzer output;
        allArgs = allArgs + liveArg + "format=b," + fileArg + compressArg;
        output = jmap(allArgs);
        output.shouldHaveExitValue(expExitValue);
        output.shouldContain(expOutput);
        if (expExitValue == 0) {
            verifyDumpFile(file);
        }
        file.delete();
    }

    private static void verifyDumpFile(File dump) {
        assertTrue(dump.exists() && dump.isFile(), "Could not create dump file " + dump.getAbsolutePath());
        try {
            File out = HprofParser.parse(dump);

            assertTrue(out != null && out.exists() && out.isFile(),
                       "Could not find hprof parser output file");
            List<String> lines = Files.readAllLines(out.toPath());
            assertTrue(lines.size() > 0, "hprof parser output file is empty");
            for (String line : lines) {
                assertFalse(line.matches(".*WARNING(?!.*Failed to resolve " +
                                         "object.*constantPoolOop.*).*"));
            }

            out.delete();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not parse dump file " + dump.getAbsolutePath());
        }
    }

    private static OutputAnalyzer jmap(String... toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jmap");
        launcher.addVMArgs(Utils.getTestJavaOpts());
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }
        launcher.addToolArg(Long.toString(ProcessTools.getProcessId()));

        processBuilder.command(launcher.getCommand());
        System.out.println(Arrays.toString(processBuilder.command().toArray()));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }
}
