/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.security;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.security.TestTLSHandshake;

/*
 * @test
 * @bug 8148188
 * @summary Enhance the security libraries to record events of interest
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.security.TestTLSHandshakeEvent
 */
public class TestTLSHandshakeEvent {
    public static void main(String[] args) throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.TLSHandshake);
            recording.start();
            TestTLSHandshake handshake = new TestTLSHandshake();
            handshake.run();
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            Events.hasEvents(events);
            assertEvent(events, handshake);
        }
    }

    private static void assertEvent(List<RecordedEvent> events, TestTLSHandshake handshake) throws Exception {
        System.out.println(events);
        for (RecordedEvent e : events) {
            if (handshake.peerHost.equals(e.getString("peerHost"))) {
                Events.assertField(e, "peerPort").equal(handshake.peerPort);
                Events.assertField(e, "protocolVersion").equal(handshake.protocolVersion);
                Events.assertField(e, "certificateId").equal(TestTLSHandshake.HASHCODE);
                Events.assertField(e, "cipherSuite").equal(TestTLSHandshake.CIPHER_SUITE);
                return;
            }
        }
        System.out.println(events);
        throw new Exception("Could not find event with hostname: " + handshake.peerHost);
    }
}
