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

package jdk.jfr.event.os;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Platform;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.os.TestProcessStart
 */
public class TestProcessStart {
    private final static String EVENT_NAME = EventNames.ProcessStart;

    public static void main(String[] args) throws Throwable {

        try (Recording recording = new Recording()) {
            recording.enable(EVENT_NAME);
            recording.start();
            List<String> commandList = new ArrayList<>();
            if (Platform.isWindows()) {
                commandList.add("dir");
            } else {
                commandList.add("ls");
            }
            commandList.add("*.jfr");
            ProcessBuilder pb = new ProcessBuilder(commandList);
            pb.directory(new File(".").getAbsoluteFile());
            Process p = pb.start();
            StringJoiner command = new StringJoiner(" ");
            for (String cmd : commandList) {
                command.add(cmd);
            }
            System.out.println(p.pid());
            System.out.println(pb.directory());
            System.out.println(commandList);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            for (RecordedEvent event : events) {
                System.out.println(event);
                Events.assertField(event, "pid").equal(p.pid());
                Events.assertField(event, "directory").equal(pb.directory().toString());
                Events.assertField(event, "command").equal(command.toString());
            }
        }
    }
}
