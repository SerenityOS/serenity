/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.internal.Repository;
import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary Test jfr reconstruct
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.jfr/jdk.jfr.internal
 * @run main/othervm jdk.jfr.tool.TestAssemble
 */
public class TestAssemble {

    @Name("Correlation")
    static class CorrelationEvent extends Event {
        int id;
    }
    private static int RECORDING_COUNT = 5;

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Throwable {
        // Create some disk recordings
        Recording[] recordings = new Recording[5];
        for (int i = 0; i < RECORDING_COUNT; i++) {
            Recording r = new Recording();
            r.setToDisk(true);
            r.start();
            CorrelationEvent ce = new CorrelationEvent();
            ce.id = i;
            ce.commit();
            r.stop();
            recordings[i] = r;
        }
        Path dir = Paths.get("reconstruction-parts");
        Files.createDirectories(dir);

        long expectedCount = 0;
        for (int i = 0; i < RECORDING_COUNT; i++) {
            Path tmp = dir.resolve("chunk-part-" + i + ".jfr");
            recordings[i].dump(tmp);
            expectedCount += countEventInRecording(tmp);
        }

        SafePath repository = Repository.getRepository().getRepositoryPath();
        Path destinationPath = Paths.get("reconstructed.jfr");

        String directory = repository.toString();
        String destination = destinationPath.toAbsolutePath().toString();

        // Test failure
        OutputAnalyzer output = ExecuteHelper.jfr("assemble");
        output.shouldContain("too few arguments");

        output = ExecuteHelper.jfr("assemble", directory);
        output.shouldContain("too few arguments");

        output = ExecuteHelper.jfr("assemble", "not-a-directory", destination);
        output.shouldContain("directory does not exist, not-a-directory");

        output = ExecuteHelper.jfr("assemble", directory, "not-a-destination");
        output.shouldContain("filename must end with '.jfr'");

        output = ExecuteHelper.jfr("assemble", "--wrongOption", directory, destination);
        output.shouldContain("too many arguments");

        FileWriter fw = new FileWriter(destination);
        fw.write('d');
        fw.close();
        output = ExecuteHelper.jfr("assemble", directory, destination);
        output.shouldContain("already exists");
        Files.delete(destinationPath);

        // test success
        output = ExecuteHelper.jfr("assemble", directory, destination);
        System.out.println(output.getOutput());
        output.shouldContain("Finished.");

        long reconstructedCount = countEventInRecording(destinationPath);
        Asserts.assertEquals(expectedCount, reconstructedCount);
        // Cleanup
        for (int i = 0; i < RECORDING_COUNT; i++) {
            recordings[i].close();
        }
    }

    private static long countEventInRecording(Path file) throws IOException {
        Integer lastId = -1;
        try (RecordingFile rf = new RecordingFile(file)) {
            long count = 0;
            while (rf.hasMoreEvents()) {
                RecordedEvent re = rf.readEvent();
                if (re.getEventType().getName().equals("Correlation")) {
                    Integer id = re.getValue("id");
                    if (id < lastId) {
                        Asserts.fail("Expected chunk number to increase");
                    }
                    lastId = id;
                }
                count++;
            }
            return count;
        }
    }
}
