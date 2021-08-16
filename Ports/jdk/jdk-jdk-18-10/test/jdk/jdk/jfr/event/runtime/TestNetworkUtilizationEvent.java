/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.event.runtime.TestNetworkUtilizationEvent
 */
public class TestNetworkUtilizationEvent {

    private static final long packetSendCount = 100;

    public static void main(String[] args) throws Throwable {

        Recording recording = new Recording();
        recording.enable(EventNames.NetworkUtilization).with("period", "endChunk");
        recording.start();

        DatagramSocket socket = new DatagramSocket();
        String msg = "hello!";
        byte[] buf = msg.getBytes();
        forceEndChunk();
        // Send a few packets both to the loopback address as well to an
        // external
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLoopbackAddress(), 12345);
        for (int i = 0; i < packetSendCount; ++i) {
            socket.send(packet);
        }
        packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("10.0.0.0"), 12345);
        for (int i = 0; i < packetSendCount; ++i) {
            socket.send(packet);
        }
        forceEndChunk();
        socket.close();
        // Now there should have been traffic on at least two different
        // interfaces
        recording.stop();

        Set<String> networkInterfaces = new HashSet<>();
        List<RecordedEvent> events = Events.fromRecording(recording);
        Events.hasEvents(events);
        for (RecordedEvent event : events) {
            System.out.println(event);
            Events.assertField(event, "writeRate").atLeast(0L).atMost(1000L * Integer.MAX_VALUE);
            Events.assertField(event, "readRate").atLeast(0L).atMost(1000L * Integer.MAX_VALUE);
            Events.assertField(event, "networkInterface").notNull();
            if (event.getLong("writeRate") > 0) {
                networkInterfaces.add(event.getString("networkInterface"));
            }
        }

        if (Platform.isWindows()) {
            // Windows does not track statistics for the loopback
            // interface
            Asserts.assertGreaterThanOrEqual(networkInterfaces.size(), 1);
        } else {
            Asserts.assertGreaterThanOrEqual(networkInterfaces.size(), 2);
        }
    }

    private static void forceEndChunk() {
       try(Recording r = new Recording()) {
           r.start();
           r.stop();
       }
    }
}
