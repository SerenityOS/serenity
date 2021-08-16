/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jmx.streaming;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.jfr.consumer.RecordingStream;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::dump(Path)
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.jmx.streaming.TestRemoteDump
 */
public class TestRemoteDump {

    @Name("RemoteDumpTest")
    static class DumpEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        testUnstarted();
        testClosed();
        testOneDump();
        testMultipleDumps();
        testEventAfterDump();
    }

    private static void testUnstarted() throws Exception {
        Path path = Path.of("recording.jfr");
        var rs = new RecordingStream();
        rs.setMaxAge(Duration.ofHours(1));
        try {
            rs.dump(path);
            throw new Exception("Should not be able to dump unstarted recording");
        } catch (IOException ise) {
            // OK, expected
        }
    }

    private static void testClosed() throws Exception {
        Path path = Path.of("recording.jfr");
        var rs = new RecordingStream();
        rs.setMaxAge(Duration.ofHours(1));
        rs.startAsync();
        rs.close();
        try {
            rs.dump(path);
            throw new Exception("Should not be able to dump closed recording");
        } catch (IOException ise) {
            // OK, expected
        }
    }

    private static void testMultipleDumps() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        var conn = ManagementFactory.getPlatformMBeanServer();
        try (var rs = new RemoteRecordingStream(conn)) {
            rs.setMaxAge(Duration.ofHours(1));
            rs.onEvent(e -> {
                latch.countDown();
            });
            rs.startAsync();
            while (latch.getCount() > 0) {
                DumpEvent e = new DumpEvent();
                e.commit();
                latch.await(10, TimeUnit.MILLISECONDS);
            }
            latch.await(); // Await first event
            AtomicInteger counter = new AtomicInteger();
            Callable<Boolean> f = () -> {
                try {
                    int id = counter.incrementAndGet();
                    Path p = Path.of("multiple-" + id + ".jfr");
                    rs.dump(p);
                    return !RecordingFile.readAllEvents(p).isEmpty();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                   return false;
                }
            };
            var service = Executors.newFixedThreadPool(3);
            var f1 = service.submit(f);
            var f2 = service.submit(f);
            var f3 = service.submit(f);
            if (!f1.get() && !f1.get() && !f3.get()) {
                throw new Exception("Failed to dump multiple recordings simultanously");
            }
            service.shutdown();
        }
    }

    private static void testOneDump() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        var conn = ManagementFactory.getPlatformMBeanServer();
        try (var rs = new RemoteRecordingStream(conn)) {
            rs.setMaxSize(5_000_000);
            rs.onEvent(e -> {
                latch.countDown();
            });
            rs.startAsync();
            while (latch.getCount() > 0) {
                DumpEvent e = new DumpEvent();
                e.commit();
                latch.await(10, TimeUnit.MILLISECONDS);
            }
            Path p = Path.of("one-dump.jfr");
            rs.dump(p);
            if (RecordingFile.readAllEvents(p).isEmpty()) {
                throw new Exception("No events in dump");
            }
        }
    }

    private static void testEventAfterDump() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try (var rs = new RecordingStream()) {
            rs.setMaxAge(Duration.ofHours(1));
            rs.onEvent(e -> {
                latch.countDown();
            });
            rs.startAsync();
            Path p = Path.of("after-dump.jfr");
            rs.dump(p);
            DumpEvent e = new DumpEvent();
            e.commit();
            latch.await();
        }
    }
}
