/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CountDownLatch;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Test that it is possible to iterate over chunk without normal events
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestEmptyChunks
 */
public class TestEmptyChunks {
    static class EndEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch end = new CountDownLatch(1);
        CountDownLatch firstFlush = new CountDownLatch(1);
        try (RecordingStream es = new RecordingStream()) {
            es.onEvent(EndEvent.class.getName(), e -> {
                end.countDown();
            });
            es.onFlush(() -> {
                firstFlush.countDown();
            });
            es.startAsync();
            System.out.println("Invoked startAsync()");
            // Wait for stream thread to start
            firstFlush.await();
            System.out.println("Stream thread active");
            Recording r1 = new Recording();
            r1.start();
            System.out.println("Chunk 1 started");
            Recording r2 = new Recording();
            r2.start();
            System.out.println("Chunk 2 started");
            Recording r3 = new Recording();
            r3.start();
            System.out.println("Chunk 3 started");
            r2.stop();
            System.out.println("Chunk 4 started");
            r3.stop();
            System.out.println("Chunk 5 started");
            EndEvent e = new EndEvent();
            e.commit();
            end.await();
            r1.stop();
            System.out.println("Chunk 5 ended");
            r1.close();
            r2.close();
            r3.close();
        }
    }
}
