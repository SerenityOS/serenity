/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @summary The test verifies JFR.dump command
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jcmd.TestJcmdDumpLimited
 */
public class TestJcmdDumpLimited {

    static class TestEvent extends Event {
        int id;
        int number;
    }

    static class TestRecording {
        Instant time;
        final Recording r;
        Path path;
        int size;
        int total;
        int id;
        Instant now;

        TestRecording(int id, int events) throws IOException, InterruptedException {
            r = new Recording();
            r.start();
            for (int i = 0; i < events; i++) {
                TestEvent event = new TestEvent();
                event.id = id;
                event.number = i;
                event.commit();
                if (i == events / 2) {
                    time = Instant.now();
                }
            }
            r.stop();
            Thread.sleep(1);
            path = Paths.get("dump-" + id + ".jfr");
            r.dump(path);
            size = (int) Files.size(path);
            this.id = id;
            this.now = Instant.now();
        }

        public void close() {
            r.close();
        }
    }

    private static long totalSize;
    private static long lastFiveSize;
    private static long firstFiveSize;
    private static long middleSize;
    private static long centerSize;
    private static long lastSize;

    private static Instant middle;
    private static Instant centerLeft;
    private static Instant centerRight;

    public static void main(String[] args) throws Exception {

        List<TestRecording> recs = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            recs.add(new TestRecording(i, 100));
        }
        int last = 0;
        List<TestRecording> reversed = new ArrayList<>(recs);
        Collections.reverse(reversed);
        for (TestRecording r : reversed) {
            r.total = r.size + last;
            last += r.size;
        }

        for (TestRecording r : recs) {
            System.out.println("Recording " + r.id + ": size=" + r.size + " (total=" + r.total + ", time=" + r.now + ")");
        }

        centerLeft = recs.get(3).time;
        middle = recs.get(4).time;
        centerRight = recs.get(5).time;

        totalSize = size(recs, 0, 9);
        lastFiveSize = size(recs, 4, 5);
        firstFiveSize = size(recs, 0, 5);
        middleSize = size(recs, 4, 1);
        centerSize = size(recs, 3, 3);
        lastSize =  size(recs, 8, 1);

        testDump();
        testDumpMaxSize();
        testDumpMaxSizeSmall();
        testDumpBegin();
        testDumpEnd();
        testDumpBeginEndInstant();
        testDumpBeginEndLocalDateTime();
        testDumpBeginEndLocalTime();
        testDumpBeginEndSame();
        testDumpMaxAge();
        testDumpBeginEndRelative();
        testDumpTooEarly();
        testDumpTooLate();
        testDumpBeginMaxAge();
        TestDumpEndMaxage();
        testDumpEndBegin();
        testDumpInvalidTime();
    }

    private static int size(List<TestRecording> recs, int skip, int limit) {
        return recs.stream().skip(skip).limit(limit).mapToInt(r -> r.size).sum();
    }

    private static void testDumpEndBegin() throws Exception {
        Path testEndBegin = Paths.get("testEndBegin.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + testEndBegin.toFile().getAbsolutePath(), "begin=" + Instant.now(), "end=" + Instant.now().minusSeconds(200));
        output.shouldContain("Dump failed, begin must precede end.");
        assertMissingFile(testEndBegin);
    }

    private static void TestDumpEndMaxage() throws Exception {
        Path testEndMaxAge = Paths.get("testEndMaxAge.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + testEndMaxAge.toFile().getAbsolutePath(), "end=" + Instant.now(), "maxage=2h");
        output.shouldContain("Dump failed, maxage can't be combined with begin or end.");
        assertMissingFile(testEndMaxAge);
    }

    private static Path testDumpBeginMaxAge() throws Exception {
        Path testBeginMaxAge = Paths.get("testBeginMaxAge.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginMaxAge.toFile().getAbsolutePath(), "begin=" + Instant.now().minusSeconds(100), "maxage=2h");
        output.shouldContain("Dump failed, maxage can't be combined with begin or end.");
        assertMissingFile(testBeginMaxAge);
        return testBeginMaxAge;
    }

    private static void testDumpTooLate() throws Exception {
        Path missing = Paths.get("missing2.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + missing.toFile().getAbsolutePath(), "begin=" + Instant.now().plus(Duration.ofHours(1)),
                "end=" + Instant.now().plus(Duration.ofHours(2)));
        output.shouldContain("Dump failed. No data found in the specified interval.");
        assertMissingFile(missing);
    }

    private static void testDumpTooEarly() throws Exception {
        Path missing = Paths.get("missing.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + missing.toFile().getAbsolutePath(), "end=" + Instant.now().minus(Duration.ofHours(1)));
        output.shouldContain("Dump failed. No data found in the specified interval.");
        assertMissingFile(missing);
    }

    private static void testDumpBeginEndRelative() throws IOException {
        Path testBeginEndRelative = Paths.get("testBeginEndRelative.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginEndRelative.toFile().getAbsolutePath(), "begin=-3h", "end=-0s");
        Asserts.assertEquals(totalSize, Files.size(testBeginEndRelative), "Expected dump with begin=-3h end=0s to contain data from all recordings");
        Files.delete(testBeginEndRelative);
    }

    private static void testDumpMaxAge() throws IOException {
        Path testMaxAge = Paths.get("testMaxAge.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testMaxAge.toFile().getAbsolutePath(), "maxage=2h");
        Asserts.assertEquals(totalSize, Files.size(testMaxAge), "Expected dump with maxage=2h  to contain data from all recordings");
        Files.delete(testMaxAge);
    }

    private static void testDumpBeginEndSame() throws IOException {
        Path testBeginEnd = Paths.get("testBeginEndSame.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginEnd.toFile().getAbsolutePath(), "begin=" + middle, "end=" + middle);
        Asserts.assertEquals(middleSize, Files.size(testBeginEnd), "Expected dump with begin=" + middle + "end=" + middle + " contain data from middle recording");
        Files.delete(testBeginEnd);
    }

    private static void testDumpBeginEndInstant() throws IOException {
        Path testBeginEnd = Paths.get("testBeginEndInstant.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginEnd.toFile().getAbsolutePath(), "begin=" + centerLeft, "end=" + centerRight);
        Asserts.assertEquals(centerSize, Files.size(testBeginEnd), "Expected dump with begin=" + centerLeft + " end=" + centerRight + " contain data from the 'center'-recordings");
        Files.delete(testBeginEnd);
    }

    private static void testDumpBeginEndLocalDateTime() throws IOException {
        LocalDateTime centerLeftLocal = LocalDateTime.ofInstant(centerLeft, ZoneOffset.systemDefault());
        LocalDateTime centerRightLocal = LocalDateTime.ofInstant(centerRight, ZoneOffset.systemDefault());
        Path testBeginEnd = Paths.get("testBeginEndLocalDateTime.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginEnd.toFile().getAbsolutePath(), "begin=" + centerLeftLocal, "end=" + centerRightLocal);
        Asserts.assertEquals(centerSize, Files.size(testBeginEnd), "Expected dump with begin=" + centerLeftLocal + " end=" + centerRightLocal + " contain data from the 'center'-recordings");
        Files.delete(testBeginEnd);
    }

    private static void testDumpBeginEndLocalTime() throws IOException {
        LocalTime centerLeftLocal = LocalTime.ofInstant(centerLeft, ZoneOffset.systemDefault());
        LocalTime centerRightLocal = LocalTime.ofInstant(centerRight, ZoneOffset.systemDefault());
        Path testBeginEnd = Paths.get("testBeginEndLocalTime.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBeginEnd.toFile().getAbsolutePath(), "begin=" + centerLeftLocal, "end=" + centerRightLocal);
        Asserts.assertEquals(centerSize, Files.size(testBeginEnd), "Expected dump with begin=" + centerLeftLocal + " end=" + centerRightLocal + " contain data from the 'center'-recordings");
        Files.delete(testBeginEnd);
    }

    private static void testDumpEnd() throws IOException {
        Path testEnd = Paths.get("testEnd.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testEnd.toFile().getAbsolutePath(), "end=" + middle);
        Asserts.assertEquals(firstFiveSize, Files.size(testEnd), "Expected dump with end=" + middle + " to contain data from the five first recordings");
        Files.delete(testEnd);
    }

    private static void testDumpBegin() throws IOException {
        Path testBegin = Paths.get("testBegin.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testBegin.toFile().getAbsolutePath(), "begin=" + middle);
        Asserts.assertEquals(lastFiveSize, Files.size(testBegin), "Expected dump with begin=" + middle + " to contain data from the last five recordings");
        Files.delete(testBegin);
    }

    private static void testDumpMaxSize() throws IOException {
        Path testMaxSize = Paths.get("testMaxSize.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testMaxSize.toFile().getAbsolutePath(), "maxsize=" + lastFiveSize);
        Asserts.assertEquals(lastFiveSize, Files.size(testMaxSize), "Expected dump with maxsize=" + lastFiveSize + " to contain data from the last five recordings");
        Files.delete(testMaxSize);
    }

    private static void testDumpMaxSizeSmall() throws IOException {
        Path testMaxSizeSmall = Paths.get("testMaxSizeSmall.jfr");
        JcmdHelper.jcmd("JFR.dump", "filename=" + testMaxSizeSmall.toFile().getAbsolutePath(), "maxsize=1k");
        Asserts.assertEquals(lastSize, Files.size(testMaxSizeSmall), "Expected dump with maxsize=1k to contain data from the last recording");
        Files.delete(testMaxSizeSmall);
    }

    private static void testDump() throws IOException {
        Path all = Paths.get("all.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + all.toFile().getAbsolutePath());
        JcmdAsserts.assertRecordingDumpedToFile(output, all.toFile());
        Asserts.assertEquals(totalSize, Files.size(all), "Expected dump to be sum of all recordings");
        Files.delete(all);
    }

    private static void testDumpInvalidTime() throws Exception {
        Path invalidTime = Paths.get("invalidTime.jfr");
        OutputAnalyzer output = JcmdHelper.jcmd("JFR.dump", "filename=" + invalidTime.toFile().getAbsolutePath(), "begin=4711");
        output.shouldContain("Dump failed, not a valid begin time.");
        assertMissingFile(invalidTime);
    }

    private static void assertMissingFile(Path missing) throws Exception {
        if (Files.exists(missing)) {
            throw new Exception("Unexpected dumpfile found");
        }
    }

}
