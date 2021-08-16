/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jfr.event.gc.collection;

import java.lang.management.ManagementFactory;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+ExplicitGCInvokesConcurrent jdk.jfr.event.gc.collection.TestSystemGC true
 * @run main/othervm -XX:-ExplicitGCInvokesConcurrent jdk.jfr.event.gc.collection.TestSystemGC false
 */
public class TestSystemGC {
    public static void main(String[] args) throws Exception {
        boolean concurrent = Boolean.valueOf(args[0]);
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.SystemGC);
            recording.start();

            // Trigger 3 System GC
            System.gc();
            ManagementFactory.getMemoryMXBean().gc();
            Runtime.getRuntime().gc();

            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            System.out.println(events);

            Asserts.assertEquals(3, events.size(), "Expected 3 SystemGC events");

            RecordedEvent event1 = events.get(0);
            Events.assertFrame(event1, System.class, "gc");
            Events.assertEventThread(event1, Thread.currentThread());
            Events.assertField(event1, "invokedConcurrent").isEqual(concurrent);

            RecordedEvent event2 = events.get(1);
            Events.assertFrame(event2, Runtime.class, "gc");
            Events.assertEventThread(event2, Thread.currentThread());
            Events.assertField(event1, "invokedConcurrent").isEqual(concurrent);

            RecordedEvent event3 = events.get(2);
            // MemoryMXBean.class is an interface so can't assertFrame on it
            Events.assertEventThread(event3, Thread.currentThread());
            Events.assertField(event1, "invokedConcurrent").isEqual(concurrent);
        }
     }
}
