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

package jdk.jfr.event.diagnostics;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules java.management
 * @run main/othervm jdk.jfr.event.diagnostics.TestHeapDump
 */
public class TestHeapDump {
    private final static String EVENT_NAME = EventNames.HeapDump;

    public static void main(String[] args) throws Exception {

        Path path = Paths.get("dump.hprof").toAbsolutePath();
        try (Recording r = new Recording()) {
            r.enable(EVENT_NAME);
            r.start();
            heapDump(path);
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            if (events.size() != 1) {
                throw new Exception("Expected one event, got " + events.size());
            }
            RecordedEvent e = events.get(0);
            Events.assertField(e, "destination").equal(path.toString());
            Events.assertField(e, "gcBeforeDump").equal(true);
            Events.assertField(e, "onOutOfMemoryError").equal(false);
            Events.assertField(e, "size").equal(Files.size(path));
            System.out.println(e);
        }
    }

    private static void heapDump(Path path) throws Exception {
        ObjectName objectName = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        Object[] parameters = new Object[2];
        parameters[0] = path.toString();
        parameters[1] = true;
        String[] signature = new String[] { String.class.getName(), boolean.class.toString() };
        mbeanServer.invoke(objectName, "dumpHeap", parameters, signature);
    }
}
