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
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that it is possible to start a stream when there are
 *          already chunk in the repository
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestRecordingBefore
 */
public class TestRecordingBefore {

    static class SnakeEvent extends Event {
        int id;
    }

    public static void main(String... args) throws Exception {

        try (Recording r1 = new Recording()) {
            r1.start();
            emitSnakeEvent(1);
            emitSnakeEvent(2);
            emitSnakeEvent(3);
            // Force a chunk rotation
            try (Recording r2 = new Recording()) {
                r2.start();
                emitSnakeEvent(4);
                emitSnakeEvent(5);
                emitSnakeEvent(6);
                r2.stop();
            }
            r1.stop();
            // Two chunks should now exist in the repository
            AtomicBoolean fail = new AtomicBoolean(false);
            CountDownLatch lastEvent = new CountDownLatch(1);
            try (RecordingStream rs = new RecordingStream()) {
                rs.onEvent(e -> {
                    long id = e.getLong("id");
                    if (id < 7) {
                        System.out.println("Found unexpected id " + id);
                        fail.set(true);
                    }
                    if (id == 9) {
                        lastEvent.countDown();
                    }
                });
                rs.startAsync();
                emitSnakeEvent(7);
                emitSnakeEvent(8);
                emitSnakeEvent(9);
                lastEvent.await();
                if (fail.get()) {
                    throw new Exception("Found events from a previous recording");
                }
            }
        }
    }

    static void emitSnakeEvent(int id) {
        SnakeEvent e = new SnakeEvent();
        e.id = id;
        e.commit();
    }

}
