/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.event.gc.detailed;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @requires vm.gc == "G1" | vm.gc == null
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:NewSize=2m -XX:MaxNewSize=2m -Xmx32m -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps -XX:+G1UseAdaptiveIHOP jdk.jfr.event.gc.detailed.TestG1AIHOPEvent
 */
public class TestG1AIHOPEvent {

    private final static String EVENT_NAME = EventNames.G1AdaptiveIHOP;
    public static byte[] bytes;

    public static void main(String[] args) throws Exception {

        Recording recording = new Recording();

        // activate the event we are interested in and start recording
        recording.enable(EVENT_NAME);
        recording.start();

        // Setting NewSize and MaxNewSize will limit eden, so
        // allocating 1024 5k byte arrays should trigger at
        // least one Young GC.
        for (int i = 0; i < 1024; i++) {
            bytes = new byte[5 * 1024];
        }
        recording.stop();

        // Verify recording
        List<RecordedEvent> all = Events.fromRecording(recording);
        Events.hasEvents(all);

        for (RecordedEvent e : all) {
            Events.assertField(e, "gcId").above(0);
        }

        recording.close();
    }
}
