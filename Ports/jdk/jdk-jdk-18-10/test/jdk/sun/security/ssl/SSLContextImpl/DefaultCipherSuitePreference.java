/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168261
 * @summary Use server cipher suites preference by default
 * @run main/othervm DefaultCipherSuitePreference
 */

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class DefaultCipherSuitePreference {
    private static final String[] contextAlgorithms = {
            "Default", "SSL", "TLS", "SSLv3", "TLSv1",
            "TLSv1.1", "TLSv1.2", "TLSv1.3"
        };

    public static void main(String[] args) throws Exception {
        for (String algorithm : contextAlgorithms) {
            System.out.println("Checking SSLContext of " + algorithm);
            SSLContext sslContext = SSLContext.getInstance(algorithm);

            // Default SSLContext is initialized automatically.
            if (!algorithm.equals("Default")) {
                // Use default TK, KM and random.
                sslContext.init((KeyManager[])null, (TrustManager[])null, null);
            }

            //
            // Check SSLContext
            //
            // Check default SSLParameters of SSLContext
            checkDefaultCipherSuitePreference(
                    sslContext.getDefaultSSLParameters(),
                    "SSLContext.getDefaultSSLParameters()");

            // Check supported SSLParameters of SSLContext
            checkDefaultCipherSuitePreference(
                    sslContext.getSupportedSSLParameters(),
                    "SSLContext.getSupportedSSLParameters()");

            //
            // Check SSLEngine
            //
            // Check SSLParameters of SSLEngine
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(true);
            checkDefaultCipherSuitePreference(
                    engine.getSSLParameters(),
                    "client mode SSLEngine.getSSLParameters()");

            engine.setUseClientMode(false);
            checkDefaultCipherSuitePreference(
                    engine.getSSLParameters(),
                    "server mode SSLEngine.getSSLParameters()");

            //
            // Check SSLSocket
            //
            // Check SSLParameters of SSLSocket
            SocketFactory fac = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket)fac.createSocket();
            checkDefaultCipherSuitePreference(
                    socket.getSSLParameters(),
                    "SSLSocket.getSSLParameters()");

            //
            // Check SSLServerSocket
            //
            // Check SSLParameters of SSLServerSocket
            SSLServerSocketFactory sf = sslContext.getServerSocketFactory();
            SSLServerSocket ssocket = (SSLServerSocket)sf.createServerSocket();
            checkDefaultCipherSuitePreference(
                    ssocket.getSSLParameters(),
                    "SSLServerSocket.getSSLParameters()");
        }
    }

    private static void checkDefaultCipherSuitePreference(
            SSLParameters parameters, String context) throws Exception {
        if (!parameters.getUseCipherSuitesOrder()) {
            throw new Exception(
                    "The local cipher suite preference is not honored " +
                    "in the connection populated SSLParameters object (" +
                    context + ")");
        }
    }
}
