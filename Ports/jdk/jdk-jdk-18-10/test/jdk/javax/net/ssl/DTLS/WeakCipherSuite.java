/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 8043758
 * @summary Datagram Transport Layer Security (DTLS)
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @build DTLSOverDatagram
 * @run main/othervm WeakCipherSuite TLS_DH_anon_WITH_AES_128_GCM_SHA256
 * @run main/othervm WeakCipherSuite SSL_DH_anon_WITH_DES_CBC_SHA
 * @run main/othervm WeakCipherSuite SSL_RSA_WITH_DES_CBC_SHA
 * @run main/othervm WeakCipherSuite SSL_DHE_RSA_WITH_DES_CBC_SHA
 * @run main/othervm WeakCipherSuite SSL_DHE_DSS_WITH_DES_CBC_SHA
 */

import javax.net.ssl.SSLEngine;
import java.security.Security;

/**
 * Test common DTLS weak cipher suites.
 */
public class WeakCipherSuite extends DTLSOverDatagram {

    // use the specific cipher suite
    volatile static String cipherSuite;

    public static void main(String[] args) throws Exception {
        // reset security properties to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        cipherSuite = args[0];

        WeakCipherSuite testCase = new WeakCipherSuite();
        testCase.runTest(testCase);
    }

    @Override
    SSLEngine createSSLEngine(boolean isClient) throws Exception {
        SSLEngine engine = super.createSSLEngine(isClient);
        engine.setEnabledCipherSuites(new String[]{cipherSuite});

        return engine;
    }
}
