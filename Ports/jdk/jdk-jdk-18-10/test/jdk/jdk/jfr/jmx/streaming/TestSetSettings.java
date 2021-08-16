/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerConnection;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.management.jfr.RemoteRecordingStream;

/**
 * @test
 * @key jfr
 * @summary Tests that a RemoteRecordingStream can be configured using
 *          setSettings
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.streaming.TestSetSettings
 */
public class TestSetSettings {

    @Enabled(false)
    @StackTrace(false)
    @Name("Event1")
    static class Event1 extends Event {
    }

    @Enabled(false)
    @StackTrace(false)
    @Name("Event2")
    static class Event2 extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        MBeanServerConnection conn = ManagementFactory.getPlatformMBeanServer();
        try (RemoteRecordingStream r = new RemoteRecordingStream(conn)) {
            r.onEvent("Event1", e -> {
                System.out.println("Found event: " + e.getEventType().getName());
                if (e.getStackTrace() == null) {
                    System.out.println("Missing strack trace");
                    return;
                }
                latch1.countDown();
            });
            r.onEvent("Event2", e -> {
                System.out.println("Found event: " + e.getEventType().getName());
                if (e.getStackTrace() == null) {
                    System.out.println("Missing strack trace");
                    return;
                }
                latch2.countDown();
            });
            // Set settings before start
            Map<String, String> settings = new HashMap<>();
            settings.put("Event1#enabled", "true");
            settings.put("Event1#stackTrace", "true");
            r.setSettings(settings);

            r.startAsync();

            Event1 e1 = new Event1();
            e1.commit();
            System.out.println("Awaiting latch 1");
            latch1.await();

            // Set settings when running
            settings = new HashMap<>();
            settings.put("Event2#enabled", "true");
            settings.put("Event2#stackTrace", "true");
            r.setSettings(settings);
            Event2 e2 = new Event2();
            e2.commit();
            System.out.println("Awaiting latch 2");
            latch2.await();
        }
    }
}
