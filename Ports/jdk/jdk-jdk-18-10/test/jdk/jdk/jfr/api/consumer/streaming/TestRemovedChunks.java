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
 * @summary Tests that a stream can gracefully handle chunk being removed
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr*=info jdk.jfr.api.consumer.streaming.TestRemovedChunks
 */
public class TestRemovedChunks {
    private final static CountDownLatch parkLatch = new CountDownLatch(1);
    private final static CountDownLatch removalLatch = new CountDownLatch(1);
    private final static CountDownLatch IFeelFineLatch = new CountDownLatch(1);

    static class DataEvent extends Event {
        double double1;
        double double2;
        double double3;
        double double4;
        double double5;
    }

    static class ParkStream extends Event {
    }

    static class IFeelFine extends Event {
    }

    public static void main(String... args) throws Exception {

        try (RecordingStream s = new RecordingStream()) {
            s.setMaxSize(5_000_000);
            s.onEvent(ParkStream.class.getName(), e -> {
                parkLatch.countDown();
                await(removalLatch);

            });
            s.onEvent(IFeelFine.class.getName(), e -> {
                IFeelFineLatch.countDown();
            });
            s.startAsync();
            // Fill first chunk with data
            emitData(1_000_000);
            // Park stream
            ParkStream ps = new ParkStream();
            ps.commit();
            await(parkLatch);
            // Rotate and emit data that exceeds maxSize
            for (int i = 0; i< 10;i++) {
                try (Recording r = new Recording()) {
                    r.start();
                    emitData(1_000_000);
                }
            }
            // Emit final event
            IFeelFine i = new IFeelFine();
            i.commit();
            // Wake up parked stream
            removalLatch.countDown();
            // Await event things gone bad
            await(IFeelFineLatch);
        }
    }

    private static void await(CountDownLatch latch) throws Error {
        try {
            latch.await();
        } catch (InterruptedException e1) {
            throw new Error("Latch interupted");
        }
    }

    private static void emitData(int amount) throws InterruptedException {
        int count = 0;
        while (amount > 0) {
            DataEvent de = new DataEvent();
            // 5 doubles are 40 bytes bytes
            // and event size, event type, thread,
            // start time, duration and stack trace about 15 bytes
            de.double1 = 0.0;
            de.double2 = 1.0;
            de.double3 = 2.0;
            de.double4 = 3.0;
            de.double5 = 4.0;
            de.commit();
            amount -= 55;
            count++;
            //
            if (count % 100_000 == 0) {
                Thread.sleep(10);
            }
        }
    }
}
