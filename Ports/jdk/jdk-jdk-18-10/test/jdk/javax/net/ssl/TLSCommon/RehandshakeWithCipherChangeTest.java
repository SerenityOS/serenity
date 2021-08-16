/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.util.Random;
import jdk.test.lib.RandomFactory;

/**
 * Testing SSLEngines re-handshaking with cipher change. New cipher is taken
 * randomly from the supported ciphers list.
 */
public class RehandshakeWithCipherChangeTest extends SSLEngineTestCase {

    public static void main(String[] s) {
        RehandshakeWithCipherChangeTest test
                = new RehandshakeWithCipherChangeTest();
        test.runTests(Ciphers.ENABLED_NON_KRB_NOT_ANON_CIPHERS);
    }

    @Override
    protected void testOneCipher(String cipher) throws SSLException {
        SSLContext context = getContext();
        int maxPacketSize = getMaxPacketSize();
        SSLEngine clientEngine = context.createSSLEngine();
        clientEngine.setUseClientMode(true);
        SSLEngine serverEngine = context.createSSLEngine();
        serverEngine.setUseClientMode(false);
        clientEngine.setEnabledCipherSuites(new String[]{cipher});
        serverEngine.setEnabledCipherSuites(
                Ciphers.ENABLED_NON_KRB_NOT_ANON_CIPHERS.ciphers);
        String randomCipher;
        serverEngine.setNeedClientAuth(true);
        long initialEpoch = 0;
        long secondEpoch = 0;
        SSLEngineResult r;
        doHandshake(clientEngine, serverEngine, maxPacketSize,
                HandshakeMode.INITIAL_HANDSHAKE);
        sendApplicationData(clientEngine, serverEngine);
        r = sendApplicationData(serverEngine, clientEngine);
        if (TESTED_SECURITY_PROTOCOL.contains("DTLS")) {
            initialEpoch = r.sequenceNumber() >> 48;
        }
        final Random RNG = RandomFactory.getRandom();
        randomCipher = Ciphers.ENABLED_NON_KRB_NOT_ANON_CIPHERS.ciphers[RNG
                .nextInt(Ciphers.ENABLED_NON_KRB_NOT_ANON_CIPHERS.ciphers.length)];
        clientEngine.setEnabledCipherSuites(new String[]{randomCipher});
        doHandshake(clientEngine, serverEngine, maxPacketSize,
                HandshakeMode.REHANDSHAKE_BEGIN_CLIENT);
        sendApplicationData(clientEngine, serverEngine);
        r = sendApplicationData(serverEngine, clientEngine);
        if (TESTED_SECURITY_PROTOCOL.contains("DTLS")) {
            secondEpoch = r.sequenceNumber() >> 48;
            AssertionError epochError = new AssertionError("Epoch number"
                    + " did not grow after re-handshake! "
                    + " Was " + initialEpoch + ", now " + secondEpoch + ".");
            if (Long.compareUnsigned(secondEpoch, initialEpoch) <= 0) {
                throw epochError;
            }
        }
        closeEngines(clientEngine, serverEngine);
    }
}
