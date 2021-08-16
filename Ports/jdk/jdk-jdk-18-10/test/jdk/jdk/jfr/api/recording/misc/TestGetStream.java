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
package jdk.jfr.api.recording.misc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary A simple test for Recording.getStream()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.misc.TestGetStream
 */
public class TestGetStream {

    private final static Instant offset = Instant.now();
    private static Instant previous;

    public static void main(String[] args) throws Exception {

        Recording recording = new Recording();
        Instant t0 = newTimestamp();
        recording.start();
        Instant t1 = newTimestamp();
        createChunkWithId(1);
        Instant t2 = newTimestamp();
        createChunkWithId(2);
        Instant t3 = newTimestamp();
        createChunkWithId(3);
        Instant t4 = newTimestamp();
        recording.stop();
        Instant t5 = newTimestamp();
        printTimeStamp("t0", t0);
        printTimeStamp("t1", t1);
        printTimeStamp("t2", t2);
        printTimeStamp("t3", t3);
        printTimeStamp("t4", t4);
        printTimeStamp("t5", t5);

        assertContainsId(recording, "t1-t4", t1, t4, 1, 2, 3);
        assertContainsId(recording, "t1-t3", t1, t3, 1, 2);
        assertContainsId(recording, "t2-t4", t2, t4, 2, 3);
        assertContainsId(recording, "t1-t2", t1, t2, 1);
        assertContainsId(recording, "t2-t3", t2, t3, 2);
        assertContainsId(recording, "t3-t4", t3, t4, 3);
        assertContainsId(recording, "t0-t1", t0, t1);
        assertContainsId(recording, "t4-t5", t4, t5);

        assertEndBeforeBegin();
    }

    private static void printTimeStamp(String name, Instant t) {
        Duration s = Duration.between(offset, t);
        System.out.println(name + ": " + (s.getSeconds() * 1_000_000_000L + s.getNano()));
    }

    private static void createChunkWithId(int id) throws InterruptedException {
        newTimestamp(); // ensure every recording gets a unique start time
        try (Recording r = new Recording()) {
            r.start();
            SimpleEvent s = new SimpleEvent();
            s.id = id;
            s.commit();
            r.stop();
            newTimestamp(); // ensure that start time is not equal to stop time
        }
        newTimestamp(); // ensure every recording gets a unique stop time
    }

    // Create a timestamp that is not same as the previous one
    private static Instant newTimestamp() throws InterruptedException {
        while (true) {
            Instant now = Instant.now();
            if (!now.equals(previous)) {
                previous = now;
                return now;
            }
            Thread.sleep(1);
        }
    }

    private static void assertContainsId(Recording r, String interval, Instant start, Instant end, Integer... ids) throws IOException {
        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        long time = System.currentTimeMillis();
        String fileName = idList.stream().map(x -> x.toString()).collect(Collectors.joining("_", "recording-get-stream_" + time + "_", ".jfr"));
        Path file = Paths.get(fileName);
        try (InputStream is = r.getStream(start, end)) {
            Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
        }
        try (RecordingFile rf = new RecordingFile(file)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                Integer id = event.getValue("id");
                Asserts.assertTrue(idList.contains(id), "Unexpected id " + id + " found in interval " + interval);
                idList.remove(id);
            }
            Asserts.assertTrue(idList.isEmpty(), "Expected events with ids " + idList);
        }
    }

    private static void assertEndBeforeBegin() throws IOException {
        try (Recording recording = new Recording()) {
            recording.start();
            recording.stop();
            Instant begin = Instant.now();
            Instant end = begin.minusNanos(1);
            recording.getStream(begin, end);
            Asserts.fail("Expected IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException x) {
            // Caught the expected exception
        }
    }

}
