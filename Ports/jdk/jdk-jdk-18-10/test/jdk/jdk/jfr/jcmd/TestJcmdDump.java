/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jcmd;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary The test verifies JFR.dump command
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:FlightRecorderOptions:maxchunksize=1M jdk.jfr.jcmd.TestJcmdDump
 */
public class TestJcmdDump {

    static class StoppedEvent extends Event {
    }
    static class RunningEvent extends Event {
    }

    private static final String[] names = { null, "r1" };
    private static final boolean booleanValues[] = { true, false };
    private static final long timeoutMillis = 50000;

    public static void main(String[] args) throws Exception {
        // Create a stopped recording in the repository to complicate things
        Recording r = new Recording();
        r.start();
        StoppedEvent de = new StoppedEvent();
        de.commit();
        r.stop();

        // The implementation of JFR.dump touch code that can't be executed using the
        // Java API. It is therefore important to try all combinations. The
        // implementation is non-trivial and depends on the combination
        for (String name : names) {
            for (boolean disk : booleanValues) {
                try (Recording r1 = new Recording(); Recording r2 = new Recording()) {
                    System.out.println();
                    System.out.println();
                    System.out.println("Starting recordings with disk=" + disk);
                    r1.setToDisk(disk);
                    // To complicate things, only enable OldObjectSample for one recording
                    r1.enable(EventNames.OldObjectSample).withoutStackTrace();
                    r1.setName("r1");
                    r2.setToDisk(disk);
                    r2.setName("r2");
                    r1.start();
                    r2.start();

                    // Expect no path to GC roots
                    jfrDump(Boolean.FALSE, name, disk, rootCount -> rootCount == 0);
                    // Expect path to GC roots
                    jfrDump(null, name, disk, rootCount -> rootCount == 0);
                    // Expect at least one path to a GC root
                    jfrDump(Boolean.TRUE, name, disk, rootCount -> rootCount > 0);
                }
            }
        }
        r.close(); // release recording data from the stopped recording
    }

    private static void jfrDump(Boolean pathToGCRoots, String name, boolean disk, Predicate<Integer> successPredicate) throws Exception {
        List<Object> leakList = new ArrayList<>();
        leakList.add(new Object[1000_0000]);
        System.gc();
        while (true) {
            RunningEvent re = new RunningEvent();
            re.commit();
            leakList.add(new Object[1000_0000]);
            leakList.add(new Object[1000_0000]);
            leakList.add(new Object[1000_0000]);
            System.gc(); // This will shorten time for object to be emitted.
            File recording = new File("TestJCMdDump.jfr");
            List<String> params = buildParameters(pathToGCRoots, name, recording);
            System.out.println(params);
            OutputAnalyzer output = ProcessTools.executeProcess(new ProcessBuilder(params));
            output.reportDiagnosticSummary();
            JcmdAsserts.assertRecordingDumpedToFile(output, recording);
            int rootCount = 0;
            int oldObjectCount = 0;
            int stoppedEventCount = 0;
            int runningEventCount = 0;
            for (RecordedEvent e : RecordingFile.readAllEvents(recording.toPath())) {
                if (e.getEventType().getName().equals(EventNames.OldObjectSample)) {
                    if (e.getValue("root") != null) {
                        rootCount++;
                    }
                    oldObjectCount++;
                }
                if (e.getEventType().getName().equals(StoppedEvent.class.getName())) {
                    stoppedEventCount++;
                }
                if (e.getEventType().getName().equals(RunningEvent.class.getName())) {
                    runningEventCount++;
                }
            }
            System.out.println("Name: " + name);
            System.out.println("Disk: " + disk);
            System.out.println("Path to GC roots: " + pathToGCRoots);
            System.out.println("Old Objects: " + oldObjectCount);
            System.out.println("Root objects: "+ rootCount);
            System.out.println("Stopped events: "+ stoppedEventCount);
            System.out.println("Running events: "+ runningEventCount);

            System.out.println();
            if (runningEventCount == 0) {
                throw new Exception("Missing event from running recording");
            }
            if (name == null && stoppedEventCount == 0) {
                throw new Exception("Missing event from stopped recording");
            }
            if (name != null && stoppedEventCount > 0) {
                throw new Exception("Stopped event should not be part of dump");
            }
            if (oldObjectCount != 0 && successPredicate.test(rootCount)) {
                return;
            }
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println("************* Retrying! **************");
            Files.delete(recording.toPath());
        }
    }

    private static List<String> buildParameters(Boolean pathToGCRoots, String name, File recording) {
        List<String> params = new ArrayList<>();
        params.add(JDKToolFinder.getJDKTool("jcmd"));
        params.add("-J-Dsun.tools.attach.attachTimeout=" + timeoutMillis);
        params.add(String.valueOf(ProcessHandle.current().pid()));
        params.add("JFR.dump");
        params.add("filename=" + recording.getAbsolutePath());
        if (pathToGCRoots != null) { // if path-to-gc-roots is omitted, default is used (disabled).
            params.add("path-to-gc-roots=" + pathToGCRoots);
        }
        if (name != null) { // if name is omitted, all recordings will be dumped
            params.add("name=" + name);
        }
        return params;
    }
}
