/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.gc == "Serial" | vm.gc == null
 * @requires vm.hasJFR
 * @library /test/lib
 * @run driver jdk.jfr.event.runtime.TestVMInfoEvent generateFlagsFile
 * @run main/othervm -XX:Flags=TestVMInfoEvent.flags -Xmx500m jdk.jfr.event.runtime.TestVMInfoEvent arg1 arg2
 */
public class TestVMInfoEvent {
    private final static String EVENT_NAME = EventNames.JVMInformation;

    public static void main(String[] args) throws Exception {
        if( (args.length > 0) && ("generateFlagsFile".equals(args[0])) ) {
            generateFlagsFile();
            return;
        }
        RuntimeMXBean mbean = ManagementFactory.getRuntimeMXBean();
        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
            Events.assertField(event, "jvmName").equal(mbean.getVmName());
            String jvmVersion = Events.assertField(event, "jvmVersion").notEmpty().getValue();
            if (!jvmVersion.contains(mbean.getVmVersion())) {
                Asserts.fail(String.format("%s does not contain %s", jvmVersion, mbean.getVmVersion()));
            }

            String jvmArgs = Events.assertField(event, "jvmArguments").notNull().getValue();
            String jvmFlags = Events.assertField(event, "jvmFlags").notNull().getValue();
            Long pid = Events.assertField(event, "pid").atLeast(0L).getValue();
            Asserts.assertEquals(pid, ProcessHandle.current().pid());
            String eventArgs = (jvmFlags.trim() + " " + jvmArgs).trim();
            String beanArgs = mbean.getInputArguments().stream().collect(Collectors.joining(" "));
            Asserts.assertEquals(eventArgs, beanArgs, "Wrong inputArgs");

            final String javaCommand = mbean.getSystemProperties().get("sun.java.command");
            Events.assertField(event, "javaArguments").equal(javaCommand);
            Events.assertField(event, "jvmStartTime").equal(mbean.getStartTime());
        }
    }

    public static void generateFlagsFile() throws Exception {
        Files.writeString(Paths.get("", "TestVMInfoEvent.flags"), "+UseSerialGC");
    }
}
