/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148108
 * @summary Disable Diffie-Hellman keys less than 1024 bits
 * @library /javax/net/ssl/templates
 * @run main/othervm -Djdk.tls.ephemeralDHKeySize=legacy LegacyDHEKeyExchange
 */

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import java.util.concurrent.CountDownLatch;

public class LegacyDHEKeyExchange extends SSLSocketTemplate{

    private final CountDownLatch connDoneLatch = new CountDownLatch(2);

    private static final int LINGER_TIMEOUT = 30; // in seconds

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        try {
            super.runServerApplication(socket);
            throw new Exception("Legacy DH keys (< 1024) should be restricted");
        } catch (SSLHandshakeException she) {
            String expectedExMsg = "Received fatal alert: insufficient_security";
            if (!expectedExMsg.equals(she.getMessage())) {
                throw she;
            }
            System.out.println("Expected exception thrown in server");
        } finally {
            connDoneLatch.countDown();
            connDoneLatch.await();
        }
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        String[] suites = new String [] {"TLS_DHE_RSA_WITH_AES_128_CBC_SHA"};
        socket.setEnabledCipherSuites(suites);
        socket.setSoLinger(true, LINGER_TIMEOUT);

        try {
            super.runClientApplication(socket);
            throw new Exception("Legacy DH keys (< 1024) should be restricted");
        } catch (SSLHandshakeException she) {
            String expectedExMsg = "DH ServerKeyExchange does not comply to" +
                    " algorithm constraints";
            if (!expectedExMsg.equals(she.getMessage())) {
                throw she;
            }
            System.out.println("Expected exception thrown in client");
        } finally {
            connDoneLatch.countDown();
            connDoneLatch.await();
        }
    }

    public static void main(String[] args) throws Exception {
        new LegacyDHEKeyExchange().run();
    }
}
