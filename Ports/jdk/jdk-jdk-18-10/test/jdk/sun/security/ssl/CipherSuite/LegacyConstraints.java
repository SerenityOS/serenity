/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8238560
 * @library /javax/net/ssl/templates
 * @summary Make sure that legacy suites are not selected if stronger choices
 *          are available
 * @run main/othervm LegacyConstraints
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLSocket;

public class LegacyConstraints extends SSLSocketTemplate {

    // none of the legacy suites are supported in TLS 1.3, so don't
    // test TLS 1.3
    private final static List<String> TLS_PROTOCOLS = List.of(
        "TLSv1", "TLSv1.1", "TLSv1.2");

    // cipher suites that contain legacy algorithms
    private final static List<String> LEGACY_SUITES = List.of(
        "TLS_RSA_WITH_NULL_SHA256",
        "TLS_DH_anon_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
        "TLS_RSA_WITH_DES_CBC_SHA",
        "TLS_RSA_WITH_3DES_EDE_CBC_SHA");

    private static String protocol;

    public static void main(String[] args) throws Exception {
        // Clear disabled algorithms so that it doesn't interfere
        // with legacy suites
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        for (String tlsProtocol : TLS_PROTOCOLS) {
            protocol = tlsProtocol;
            System.out.println("Testing " + protocol);
            new LegacyConstraints().run();
        }
    }

    /**
     * Prepends legacy suites to the array of enabled suites.
     */
    private static void configureSocket(SSLSocket socket) {
        socket.setEnabledProtocols(new String[] {protocol});
        List<String> newSuites = new ArrayList(LEGACY_SUITES);
        Arrays.stream(socket.getEnabledCipherSuites())
                            .forEachOrdered(suite -> newSuites.add(suite));
        socket.setEnabledCipherSuites(newSuites.toArray(new String[0]));
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        configureSocket(socket);

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();

        String negotiatedSuite = socket.getSession().getCipherSuite();
        if (LEGACY_SUITES.contains(negotiatedSuite)) {
            throw new Exception("Test FAILED: the negotiated suite " +
                    negotiatedSuite + " is a legacy suite");
        }
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        configureSocket(socket);

        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();
    }
}
