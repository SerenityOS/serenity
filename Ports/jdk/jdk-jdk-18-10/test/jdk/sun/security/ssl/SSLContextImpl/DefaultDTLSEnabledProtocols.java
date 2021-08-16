/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8237474
 * @summary Test jdk.tls.client.protocols with DTLS
 * @run main/othervm DefaultDTLSEnabledProtocols
 */

import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class DefaultDTLSEnabledProtocols {
    static enum ContextVersion {
        TLS_CV_01("DTLS",
                new String[] {"DTLSv1.0", "DTLSv1.2"}),
        TLS_CV_02("DTLSv1.0",
                new String[] {"DTLSv1.0"}),
        TLS_CV_03("DTLSv1.2",
                new String[] {"DTLSv1.0", "DTLSv1.2"});

        final String contextVersion;
        final String[] enabledProtocols;
        final static String[] supportedProtocols = new String[] {
                "DTLSv1.0", "DTLSv1.2"};

        ContextVersion(String contextVersion, String[] enabledProtocols) {
            this.contextVersion = contextVersion;
            this.enabledProtocols = enabledProtocols;
        }
    }

    private static boolean checkProtocols(String[] target, String[] expected) {
        boolean success = true;
        if (target.length == 0) {
            System.out.println("\tError: No protocols");
            success = false;
        }

        if (!protocolEquals(target, expected)) {
            System.out.println("\tError: Expected to get protocols " +
                    Arrays.toString(expected));
            success = false;
        }
        System.out.println("\t  Protocols found " + Arrays.toString(target));

        return success;
    }

    private static boolean protocolEquals(
            String[] actualProtocols,
            String[] expectedProtocols) {
        if (actualProtocols.length != expectedProtocols.length) {
            return false;
        }

        Set<String> set = new HashSet<>(Arrays.asList(expectedProtocols));
        for (String actual : actualProtocols) {
            if (set.add(actual)) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkCipherSuites(String[] target) {
        boolean success = true;
        if (target.length == 0) {
            System.out.println("\tError: No cipher suites");
            success = false;
        }

        return success;
    }

    public static void main(String[] args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        boolean failed = false;
        for (ContextVersion cv : ContextVersion.values()) {
            System.out.println("Checking SSLContext of " + cv.contextVersion);
            SSLContext context = SSLContext.getInstance(cv.contextVersion);

            // Default SSLContext is initialized automatically.
            if (!cv.contextVersion.equals("Default")) {
                // Use default TK, KM and random.
                context.init((KeyManager[])null, (TrustManager[])null, null);
            }

            //
            // Check SSLContext
            //
            // Check default SSLParameters of SSLContext
            System.out.println("\tChecking default SSLParameters");
            SSLParameters parameters = context.getDefaultSSLParameters();

            String[] protocols = parameters.getProtocols();
            failed |= !checkProtocols(protocols, cv.enabledProtocols);

            String[] ciphers = parameters.getCipherSuites();
            failed |= !checkCipherSuites(ciphers);

            // Check supported SSLParameters of SSLContext
            System.out.println("\tChecking supported SSLParameters");
            parameters = context.getSupportedSSLParameters();

            protocols = parameters.getProtocols();
            failed |= !checkProtocols(protocols, cv.supportedProtocols);

            ciphers = parameters.getCipherSuites();
            failed |= !checkCipherSuites(ciphers);

            //
            // Check SSLEngine
            //
            // Check SSLParameters of SSLEngine
            System.out.println();
            System.out.println("\tChecking SSLEngine of this SSLContext");
            System.out.println("\tChecking SSLEngine.getSSLParameters()");
            SSLEngine engine = context.createSSLEngine();
            engine.setUseClientMode(true);
            parameters = engine.getSSLParameters();

            protocols = parameters.getProtocols();
            failed |= !checkProtocols(protocols, cv.enabledProtocols);

            ciphers = parameters.getCipherSuites();
            failed |= !checkCipherSuites(ciphers);

            System.out.println("\tChecking SSLEngine.getEnabledProtocols()");
            protocols = engine.getEnabledProtocols();
            failed |= !checkProtocols(protocols, cv.enabledProtocols);

            System.out.println("\tChecking SSLEngine.getEnabledCipherSuites()");
            ciphers = engine.getEnabledCipherSuites();
            failed |= !checkCipherSuites(ciphers);

            System.out.println("\tChecking SSLEngine.getSupportedProtocols()");
            protocols = engine.getSupportedProtocols();
            failed |= !checkProtocols(protocols, cv.supportedProtocols);

            System.out.println(
                    "\tChecking SSLEngine.getSupportedCipherSuites()");
            ciphers = engine.getSupportedCipherSuites();
            failed |= !checkCipherSuites(ciphers);

            //
            // Check SSLSocket
            //
            // Check SSLParameters of SSLSocket
            System.out.println();
            System.out.println("\tChecking SSLSocket of this SSLContext");
            try {
                context.getSocketFactory();
                failed = true;
                System.out.println("SSLSocket returned a socket for DTLS");
            } catch (UnsupportedOperationException e) {
                System.out.println("\t  " + e.getMessage());
            }

            //
            // Check SSLServerSocket
            //
            // Check SSLParameters of SSLServerSocket
            System.out.println();
            System.out.println("\tChecking SSLServerSocket of this SSLContext");
            try {
                context.getServerSocketFactory();
                failed = true;
                System.out.println("SSLServerSocket returned a socket for DTLS");
            } catch (UnsupportedOperationException e) {
                System.out.println("\t  " + e.getMessage());
            }
        }

        if (failed) {
            throw new Exception("Run into problems, see log for more details");
        } else {
            System.out.println("\t... Success");
        }
    }
}
