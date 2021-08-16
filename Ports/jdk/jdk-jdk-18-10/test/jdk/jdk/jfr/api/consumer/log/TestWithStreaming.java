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
package jdk.jfr.api.consumer.log;

import java.io.Closeable;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Checks that it is possible to stream together with log stream
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build jdk.jfr.api.consumer.log.LogAnalyzer
 * @run main/othervm -Xlog:jfr+event*=debug:file=with-streaming.log
 *      jdk.jfr.api.consumer.log.TestWithStreaming
 */
public class TestWithStreaming {

    @Name("TwoStreams")
    static class TwoStreams extends Event {
        String message;
    }

    public static void main(String... args) throws Exception {
        LogAnalyzer la = new LogAnalyzer("with-streaming.log");
        CountDownLatch latch = new CountDownLatch(2);
        try (RecordingStream rs = new RecordingStream()) {
            rs.enable(TwoStreams.class);
            rs.onEvent("TwoStreams", e -> {
                latch.countDown();
            });
            rs.setStartTime(Instant.MIN);
            rs.startAsync();
            TwoStreams e1 = new TwoStreams();
            e1.commit();
            TwoStreams e2 = new TwoStreams();
            e2.commit();
            latch.await();
            var scheduler = Executors.newScheduledThreadPool(1);
            try (Closeable close = scheduler::shutdown) {
                scheduler.scheduleAtFixedRate(() -> {
                    TwoStreams e = new TwoStreams();
                    e.message = "hello";
                    e.commit();
                }, 0, 10, TimeUnit.MILLISECONDS);
                la.await("hello");
            }
        }
    }
}
