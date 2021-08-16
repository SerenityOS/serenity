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

package jdk.security.logging;

import java.security.cert.CertificateFactory;
import jdk.test.lib.security.TestCertificate;

/*
 * @test
 * @bug 8148188
 * @summary Enhance the security libraries to record events of interest
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.security.logging.TestX509CertificateLog LOGGING_ENABLED
 * @run main/othervm jdk.security.logging.TestX509CertificateLog LOGGING_DISABLED
 */
public class TestX509CertificateLog {
    public static void main(String[] args) throws Exception {
        LogJvm l = new LogJvm(GenerateX509Certicate.class, args);
        l.addExpected(
            "FINE: X509Certificate: Alg:" + TestCertificate.ONE.algorithm +
            ", Serial:" + TestCertificate.ONE.serialNumber +
            ", Subject:" + TestCertificate.ONE.subject +
            ", Issuer:"  + TestCertificate.ONE.issuer +
            ", Key type:" + TestCertificate.ONE.keyType +
            ", Length:" + TestCertificate.ONE.keyLength +
            ", Cert Id:" + TestCertificate.ONE.certId);
        l.addExpected(
            "FINE: X509Certificate: Alg:" + TestCertificate.TWO.algorithm +
            ", Serial:" + TestCertificate.TWO.serialNumber +
            ", Subject:" + TestCertificate.TWO.subject +
            ", Issuer:"  + TestCertificate.TWO.issuer +
            ", Key type:" + TestCertificate.TWO.keyType +
            ", Length:" + TestCertificate.TWO.keyLength +
            ", Cert Id:" + TestCertificate.TWO.certId);
        l.testExpected();
    }

    public static class GenerateX509Certicate {
        public static void main(String[] args) throws Exception {
            TestCertificate.ONE.certificate();
            TestCertificate.TWO.certificate();
        }
    }
}
