/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.consumer.streaming;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.EventStream;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.StreamingUtils;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @summary Test scenario where JFR event producer is in a different process
 *          with respect to the JFR event stream consumer.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules jdk.attach
 *          jdk.jfr
 * @run main jdk.jfr.api.consumer.streaming.TestCrossProcessStreaming
 */

// Test Sequence:
// 1. Main starts a child process "Event-Producer"
// 2. Producer process produces TestEvent1 (first batch).
// 3. Main process consumes the event stream until pre-defined number of events is consumed.
// 4. Main process signals to child process to start producing the 2nd batch of events (TestEvent2).
// 5. The second batch is produced for pre-defined number of flush intervals.
// 6. Once the main process detects the pre-defined number of flush intervals, it signals
//    to the producer process to exit.
// 7. Producer process communicates the number of events in 2nd batch then exits.
// 8. Main process verifies that number of events in 2nd batch arrived as expected, and that
//    producer process exited w/o error.
//
//    The sequence of steps 2-5 ensures that the stream can be consumed simultaneously
//    as the producer process is producing events.
public class TestCrossProcessStreaming {
    @Name("Batch1")
    public static class TestEvent1 extends Event {
    }
    @Name("Batch2")
    public static class TestEvent2 extends Event {
    }
    @Name("Result")
    public static class ResultEvent extends Event {
        int batch1Count;
        int batch2Count;
    }

    public static void main(String... args) throws Exception {
        Process process = EventProducer.start();
        Path repo = StreamingUtils.getJfrRepository(process);

        // Consume 1000 events in a first batch
        CountDownLatch consumed = new CountDownLatch(1000);
        try (EventStream es = EventStream.openRepository(repo)) {
            es.onEvent("Batch1", e -> consumed.countDown());
            es.setStartTime(Instant.EPOCH); // read from start
            es.startAsync();
            consumed.await();
        }

        signal("second-batch");

        // Consume events until 'exit' signal.
        AtomicInteger total = new AtomicInteger();
        AtomicInteger produced = new AtomicInteger(-1);
        AtomicReference<Exception> exception = new AtomicReference<>();
        try (EventStream es = EventStream.openRepository(repo)) {
            es.onEvent("Batch2", e -> {
                    try {
                        if (total.incrementAndGet() == 1000)  {
                            signal("exit");
                        }
                    } catch (Exception exc) {
                        exception.set(exc);
                    }
            });
            es.onEvent("Result",e -> {
                produced.set(e.getInt("batch2Count"));
                es.close();
            });
            es.setStartTime(Instant.EPOCH);
            es.start();
        }
        process.waitFor();

        if (exception.get() != null) {
            throw exception.get();
        }
        Asserts.assertEquals(process.exitValue(), 0, "Incorrect exit value");
        Asserts.assertEquals(total.get(), produced.get(), "Missing events");
    }

    static class EventProducer {
        private static final String MAIN_STARTED = "MAIN_STARTED";

        static Process start() throws Exception {
            String[] args = {"-XX:StartFlightRecording", EventProducer.class.getName()};
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
            return ProcessTools.startProcess("Event-Producer", pb,
                                             line -> line.contains(MAIN_STARTED),
                                             0, TimeUnit.SECONDS);
        }

        public static void main(String... args) throws Exception {
            System.out.println(MAIN_STARTED);
            ResultEvent rs = new ResultEvent();
            rs.batch1Count = emit(TestEvent1.class, "second-batch");
            rs.batch2Count = emit(TestEvent2.class, "exit");
            rs.commit();
        }

        static int emit(Class<? extends Event> eventClass, String termination) throws Exception {
            int count = 0;
            while (true) {
                Event event = eventClass.getConstructor().newInstance();
                event.commit();
                count++;
                if (count % 1000 == 0) {
                    Thread.sleep(100);
                    if (signalCheck(termination)) {
                        System.out.println("Events generated: " + count);
                        return count;
                    }
                }
            }
        }
    }

    static void signal(String name) throws Exception {
        Files.createFile(Paths.get(".", name));
    }

    static boolean signalCheck(String name) throws Exception {
        return Files.exists(Paths.get(".", name));
    }
}
