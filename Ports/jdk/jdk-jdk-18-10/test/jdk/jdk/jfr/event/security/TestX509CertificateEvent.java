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

import java.security.cert.CertificateFactory;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.security.TestCertificate;

/*
 * @test
 * @bug 8148188
 * @summary Enhance the security libraries to record events of interest
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.security.TestX509CertificateEvent
 */
public class TestX509CertificateEvent {
    public static void main(String[] args) throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.X509Certificate);
            recording.start();

            TestCertificate.ONE.certificate();
            TestCertificate.TWO.certificate();
            // Generate twice to make sure only one event per certificate is generated
            TestCertificate.ONE.certificate();
            TestCertificate.TWO.certificate();

            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            Asserts.assertEquals(events.size(), 2, "Incorrect number of X509Certificate events");
            assertEvent(events, TestCertificate.ONE);
            assertEvent(events, TestCertificate.TWO);
        }
    }

    private static void assertEvent(List<RecordedEvent> events, TestCertificate cert) throws Exception {
        for (RecordedEvent e : events) {
            if (e.getLong("certificateId") == cert.certId) {
                Events.assertField(e, "algorithm").equal(cert.algorithm);
                Events.assertField(e, "subject").equal(cert.subject);
                Events.assertField(e, "issuer").equal(cert.issuer);
                Events.assertField(e, "keyType").equal(cert.keyType);
                Events.assertField(e, "keyLength").equal(cert.keyLength);
                Events.assertField(e, "serialNumber").equal(cert.serialNumber);
                return;
            }
        }
        System.out.println(events);
        throw new Exception("Could not find event with cert Id: " + cert.certId);
    }
}
