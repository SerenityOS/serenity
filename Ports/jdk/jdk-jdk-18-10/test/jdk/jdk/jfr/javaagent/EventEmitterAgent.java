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

package jdk.jfr.javaagent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;

// Java agent that emits events
public class EventEmitterAgent {

    private static final long EVENTS = 150_000;
    private static final Path DUMP_PATH = Paths.get("dump.jfr").toAbsolutePath();

    // Called when agent is loaded from command line
    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        agentWork();
    }

    // Called when agent is dynamically loaded
    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        agentWork();
    }

    private static void agentWork() throws Exception {
        try (Recording r = new Recording(Configuration.getConfiguration("default"))) {
            r.enable(EventNames.JavaExceptionThrow);
            r.setDestination(DUMP_PATH);
            r.start();
            emitEvents();
            r.stop();
        }
    }

    public static void emitEvents() {
        for (int i = 0; i < EVENTS; i++) {
            TestEvent e = new TestEvent();
            e.msg = "Long message that puts pressure on the string pool " + i % 100;
            e.count = i;
            e.thread = Thread.currentThread();
            e.clazz = String.class;
            e.commit();
            if (i % 10000 == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    @Name("Test")
    static class TestEvent extends Event {
        String msg;
        int count;
        Thread thread;
        Class<?> clazz;
    }

    public static void validateRecording() throws Exception {
        long testEventCount = RecordingFile.readAllEvents(DUMP_PATH)
                .stream()
                .filter(e -> e.getEventType().getName().equals("Test"))
                .count();
        Asserts.assertEquals(testEventCount, EVENTS, "Mismatch in TestEvent count");
    }
}
