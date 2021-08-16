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
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.security.TestCertificate;

/*
 * @test
 * @bug 8148188
 * @summary Enhance the security libraries to record events of interest
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.events
 * @run main/othervm jdk.jfr.event.security.TestX509ValidationEvent
 */
public class TestX509ValidationEvent {
    public static void main(String[] args) throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.X509Validation);
            recording.start();
            // intermediate certificate test
            TestCertificate.generateChain(false, true);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertEquals(events.size(), 3, "Incorrect number of events");
            assertEvent1(events);
        }

        try (Recording recording = new Recording()) {
            recording.enable(EventNames.X509Validation);
            recording.start();
            // self signed certificate test
            TestCertificate.generateChain(true, true);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertEquals(events.size(), 2, "Incorrect number of events");
            assertEvent2(events);
        }

        try (Recording recording = new Recording()) {
            recording.enable(EventNames.X509Validation);
            recording.start();
            // intermediate certificate test, with no Cert for trust anchor
            TestCertificate.generateChain(true, false);
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertEquals(events.size(), 2, "Incorrect number of events");
            assertEvent3(events);
        }
    }

    private static void assertEvent1(List<RecordedEvent> events) throws Exception {
        for (RecordedEvent e : events) {
            int pos = e.getInt("certificatePosition");
            switch (pos) {
                case 1:
                    Events.assertField(e, "certificateId")
                            .equal(TestCertificate.ROOT_CA.certId);
                    break;
                case 2:
                    Events.assertField(e, "certificateId")
                            .equal(TestCertificate.TWO.certId);
                    break;
                case 3:
                    Events.assertField(e, "certificateId")
                            .equal(TestCertificate.ONE.certId);
                    break;
                default:
                    System.out.println(events);
                    throw new Exception("Unexpected position:" + pos);
            }
        }
    }

    /*
     * Self signed certificate test
     */
    private static void assertEvent2(List<RecordedEvent> events) throws Exception {
        for (RecordedEvent e : events) {
            int pos = e.getInt("certificatePosition");
            switch (pos) {
                case 1:
                case 2:
                    Events.assertField(e, "certificateId")
                            .equal(TestCertificate.ROOT_CA.certId);
                    break;
                default:
                    System.out.println(events);
                    throw new Exception("Unexpected position:" + pos);
            }
        }
    }
    /*
     * Self signed certificate test
     */
    private static void assertEvent3(List<RecordedEvent> events) throws Exception {
        for (RecordedEvent e : events) {
            int pos = e.getInt("certificatePosition");
            switch (pos) {
                // use public key of cert provided in TrustAnchor
                case 1:
                    Asserts.assertEquals(e.getLong("certificateId"),
                        Long.valueOf(TestCertificate.ROOT_CA.certificate().getPublicKey().hashCode()));
                    break;
                case 2:
                    Events.assertField(e, "certificateId")
                            .equal(TestCertificate.ROOT_CA.certId);
                    break;
                default:
                    System.out.println(events);
                    throw new Exception("Unexpected position:" + pos);
            }
        }
    }
}
