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
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that it is possible to stream contents of ongoing
 *          recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr+system+streaming=trace
 *      jdk.jfr.api.consumer.streaming.TestStartMultiChunk
 */
public class TestStartMultiChunk {

    @Period("10 s")
    @Name("Zebra")
    static class ZebraEvent extends Event {
    }

    @Name("Cat")
    static class CatEvent extends Event {
    }

    @Name("Dog")
    static class DogEvent extends Event {
    }

    @Name("Mouse")
    static class MouseEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch dogLatch = new CountDownLatch(1);
        CountDownLatch catLatch = new CountDownLatch(1);
        CountDownLatch mouseLatch = new CountDownLatch(1);
        CountDownLatch zebraLatch = new CountDownLatch(3);

        FlightRecorder.addPeriodicEvent(ZebraEvent.class, () -> {
            ZebraEvent ze = new ZebraEvent();
            ze.commit();
            System.out.println("Zebra emitted");
        });

        try (RecordingStream s = new RecordingStream()) {
            s.onEvent("Cat", e -> {
                System.out.println("Found cat!");
                catLatch.countDown();
            });
            s.onEvent("Dog", e -> {
                System.out.println("Found dog!");
                dogLatch.countDown();
            });
            s.onEvent("Zebra", e -> {
                System.out.println("Found zebra!");
                zebraLatch.countDown();
            });
            s.onEvent("Mouse", e -> {
                System.out.println("Found mouse!");
                mouseLatch.countDown();
            });
            s.startAsync();
            System.out.println("Stream recoding started");

            try (Recording r1 = new Recording()) {
                r1.start();
                System.out.println("r1.start()");
                MouseEvent me = new MouseEvent();
                me.commit();
                System.out.println("Mouse emitted");
                mouseLatch.await();
                try (Recording r2 = new Recording()) { // force chunk rotation
                                                       // in stream
                    r2.start();
                    System.out.println("r2.start()");
                    DogEvent de = new DogEvent();
                    de.commit();
                    System.out.println("Dog emitted");
                    dogLatch.await();
                    CatEvent ce = new CatEvent();
                    ce.commit();
                    System.out.println("Cat emitted");
                    catLatch.await();
                    zebraLatch.await();
                }
            }
        }
    }
}
