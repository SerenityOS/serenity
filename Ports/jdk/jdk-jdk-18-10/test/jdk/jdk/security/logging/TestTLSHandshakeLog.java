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

import jdk.test.lib.security.TestTLSHandshake;

/*
 * @test
 * @bug 8148188
 * @summary Enhance the security libraries to record events of interest
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.security.logging.TestTLSHandshakeLog LOGGING_ENABLED
 * @run main/othervm jdk.security.logging.TestTLSHandshakeLog LOGGING_DISABLED
 */
public class TestTLSHandshakeLog {
    public static void main(String[] args) throws Exception {
        LogJvm l = new LogJvm(TLSHandshake.class, args);
        l.addExpected("FINE: X509Certificate: Alg:SHA256withRSA, Serial:" + TestTLSHandshake.CERT_SERIAL);
        l.addExpected("Subject:CN=Regression Test");
        l.addExpected("Key type:EC, Length:256");
        l.addExpected("FINE: ValidationChain: " +
                TestTLSHandshake.ANCHOR_HASHCODE +
                ", " + TestTLSHandshake.HASHCODE);
        l.addExpected("SunJSSE Test Serivce");
        l.addExpected("TLSHandshake:");
        l.addExpected("TLSv1.2");
        l.addExpected(TestTLSHandshake.CIPHER_SUITE +", " + TestTLSHandshake.HASHCODE);
        l.testExpected();
    }

    public static class TLSHandshake {
        public static void main(String[] args) throws Exception {
            TestTLSHandshake handshake = new TestTLSHandshake();
            handshake.run();
        }
    }
}