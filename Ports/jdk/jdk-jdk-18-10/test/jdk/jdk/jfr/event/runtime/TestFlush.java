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

package jdk.jfr.event.runtime;

import java.util.concurrent.CountDownLatch;

import java.util.HashMap;
import java.util.Map;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Period;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingStream;
import jdk.jfr.consumer.RecordedEvent;

import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @summary Verifies at the metalevel that stream contents are written to ongoing recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr+system+streaming=trace jdk.jfr.event.runtime.TestFlush
 */
public class TestFlush {
    private static boolean flushEventAck = false;

    @Period("2 s")
    static class ZebraEvent extends Event {
    }
    static class CatEvent extends Event {
    }
    static class DogEvent extends Event {
    }
    static class MouseEvent extends Event {
    }

    public static void main(String... args) throws InterruptedException {
        CountDownLatch dogLatch = new CountDownLatch(1);
        CountDownLatch catLatch = new CountDownLatch(1);
        CountDownLatch mouseLatch = new CountDownLatch(1);
        CountDownLatch zebraLatch = new CountDownLatch(3);

        FlightRecorder.addPeriodicEvent(ZebraEvent.class, () -> {
            ZebraEvent ze = new ZebraEvent();
            ze.commit();
        });

        try (RecordingStream rs = new RecordingStream()) {
            rs.enable(EventNames.Flush);
            rs.onEvent(e -> {
                if (e.getEventType().getName().equals(EventNames.Flush)) {
                    flushEventAck = true;
                    validateFlushEvent(e);
                    return;
                }
                if (e.getEventType().getName().equals(CatEvent.class.getName())) {
                    System.out.println("Found cat!");
                    catLatch.countDown();
                    return;
                }
                if (e.getEventType().getName().equals(DogEvent.class.getName())) {
                    System.out.println("Found dog!");
                    dogLatch.countDown();
                    return;
                }
                if (e.getEventType().getName().equals(ZebraEvent.class.getName())) {
                    System.out.println("Found zebra!");
                    zebraLatch.countDown();
                    return;
                }
                if (e.getEventType().getName().equals(MouseEvent.class.getName())) {
                    System.out.println("Found mouse!");
                    mouseLatch.countDown();
                    return;
                }
                System.out.println("Unexpected event: " + e.getEventType().getName());
            });

            rs.startAsync();

            try (Recording r1 = new Recording()) {
                r1.start();
                MouseEvent me = new MouseEvent();
                me.commit();
                System.out.println("Mouse emitted");
                mouseLatch.await();
                try (Recording r2 = new Recording()) { // force chunk rotation in stream
                    r2.start();
                    DogEvent de = new DogEvent();
                    de.commit();
                    System.out.println("Dog emitted");
                    dogLatch.await();
                    CatEvent ce = new CatEvent();
                    ce.commit();
                    System.out.println("Cat emitted");
                    catLatch.await();
                    zebraLatch.await();
                    acknowledgeFlushEvent();
                }
            }
        }
    }

    private static void printEvent(RecordedEvent re) {
        System.out.println(re.getEventType().getName());
        System.out.println(re.getStartTime().toEpochMilli());
        System.out.println(re.getEndTime().toEpochMilli());
    }

    private static void printFlushEvent(RecordedEvent re) {
        printEvent(re);
        System.out.println("flushID: " + (long) re.getValue("flushId"));
        System.out.println("elements: " + (long) re.getValue("elements"));
        System.out.println("size: " + (long) re.getValue("size"));
    }

    private static void validateFlushEvent(RecordedEvent re) {
        printFlushEvent(re);
        Asserts.assertTrue(re.getEventType().getName().contains("Flush"), "invalid Event type");
        Asserts.assertGT((long) re.getValue("flushId"), 0L, "Invalid flush ID");
        Asserts.assertGT((long) re.getValue("elements"), 0L, "No elements");
        Asserts.assertGT((long) re.getValue("size"), 0L, "Empty size");
    }

    private static void acknowledgeFlushEvent() {
        Asserts.assertTrue(flushEventAck, "No Flush event");
    }
}
