/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import jdk.test.lib.security.SecurityUtils;

/*
 * @test
 * @bug 8234728
 * @library /javax/net/ssl/templates
 *          /javax/net/ssl/TLSCommon
 *          /test/lib
 * @summary Test TLS ciphersuites order.
 *      Parameter order: <protocol> <client cipher order> <server cipher order>
 * @run main/othervm TLSCipherSuitesOrder TLSv13 ORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv13 UNORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv13 UNORDERED UNORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv13 ORDERED ORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv12 ORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv12 UNORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv12 UNORDERED UNORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv12 ORDERED ORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv11 ORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv11 UNORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv11 UNORDERED UNORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv11 ORDERED ORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv1 ORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv1 UNORDERED default
 * @run main/othervm TLSCipherSuitesOrder TLSv1 UNORDERED UNORDERED
 * @run main/othervm TLSCipherSuitesOrder TLSv1 ORDERED ORDERED
 */
public class TLSCipherSuitesOrder extends SSLSocketTemplate {

    private final String protocol;
    private final String[] servercipherSuites;
    private final String[] clientcipherSuites;

    public static void main(String[] args) {
        PROTOCOL protocol = PROTOCOL.valueOf(args[0]);
        try {
            new TLSCipherSuitesOrder(protocol.getProtocol(),
                    protocol.getCipherSuite(args[1]),
                    protocol.getCipherSuite(args[2])).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TLSCipherSuitesOrder(String protocol, String[] clientcipherSuites,
            String[] servercipherSuites) {
        // Re-enable protocol if it is disabled.
        if (protocol.equals("TLSv1") || protocol.equals("TLSv1.1")) {
            SecurityUtils.removeFromDisabledTlsAlgs(protocol);
        }
        this.protocol = protocol;
        this.clientcipherSuites = clientcipherSuites;
        this.servercipherSuites = servercipherSuites;
    }

    // Servers are configured before clients, increment test case after.
    @Override
    protected void configureClientSocket(SSLSocket socket) {
        socket.setEnabledProtocols(new String[]{protocol});
        if (clientcipherSuites != null) {
            socket.setEnabledCipherSuites(clientcipherSuites);
        }
    }

    @Override
    protected void configureServerSocket(SSLServerSocket serverSocket) {
        serverSocket.setEnabledProtocols(new String[]{protocol});
        if (servercipherSuites != null) {
            serverSocket.setEnabledCipherSuites(servercipherSuites);
        }
    }

    protected void runServerApplication(SSLSocket socket) throws Exception {
        if (servercipherSuites != null) {
            System.out.printf("SERVER: setEnabledCipherSuites:%s - "
                    + "getEnabledCipherSuites:%s%n",
                    Arrays.deepToString(servercipherSuites),
                    Arrays.deepToString(socket.getEnabledCipherSuites()));
        }
        if (servercipherSuites != null && !Arrays.equals(servercipherSuites,
                socket.getEnabledCipherSuites())) {
            throw new RuntimeException("Unmatched server side CipherSuite order");
        }
        super.runServerApplication(socket);
    }

    protected void runClientApplication(SSLSocket socket) throws Exception {
        if (clientcipherSuites != null) {
            System.out.printf("CLIENT: setEnabledCipherSuites:%s - "
                    + "getEnabledCipherSuites:%s%n",
                    Arrays.deepToString(clientcipherSuites),
                    Arrays.deepToString(socket.getEnabledCipherSuites()));
        }
        if (clientcipherSuites != null && !Arrays.equals(
                clientcipherSuites, socket.getEnabledCipherSuites())) {
            throw new RuntimeException("Unmatched client side CipherSuite order");
        }
        super.runClientApplication(socket);
    }

    enum PROTOCOL {
        TLSv13("TLSv1.3",
                new String[]{
                    "TLS_AES_256_GCM_SHA384",
                    "TLS_AES_128_GCM_SHA256",
                    "TLS_CHACHA20_POLY1305_SHA256"},
                new String[]{"TLS_CHACHA20_POLY1305_SHA256",
                    "TLS_AES_128_GCM_SHA256",
                    "TLS_AES_256_GCM_SHA384"}),
        TLSv12("TLSv1.2",
                new String[]{
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"},
                new String[]{
                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"}),
        TLSv11("TLSv1.1",
                new String[]{
                    "TLS_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA"},
                new String[]{
                    "TLS_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_RSA_WITH_AES_256_CBC_SHA"}),
        TLSv1("TLSv1",
                new String[]{
                    "TLS_RSA_WITH_AES_256_CBC_SHA",
                    "TLS_RSA_WITH_AES_128_CBC_SHA"},
                new String[]{
                    "TLS_RSA_WITH_AES_128_CBC_SHA",
                    "TLS_RSA_WITH_AES_256_CBC_SHA"});

        String protocol;
        String[] orderedCiphers;
        String[] unOrderedCiphers;

        private PROTOCOL(String protocol, String[] orderedCiphers,
                String[] unOrderedCiphers) {
            this.protocol = protocol;
            this.orderedCiphers = orderedCiphers;
            this.unOrderedCiphers = unOrderedCiphers;
        }

        public String getProtocol() {
            return protocol;
        }

        public String[] getOrderedCiphers() {
            return orderedCiphers;
        }

        public String[] getUnOrderedCiphers() {
            return unOrderedCiphers;
        }

        public String[] getCipherSuite(String order) {
            switch (order) {
                case "ORDERED":
                    return getOrderedCiphers();
                case "UNORDERED":
                    return getUnOrderedCiphers();
                default:
                    return null;
            }
        }
    }
}
