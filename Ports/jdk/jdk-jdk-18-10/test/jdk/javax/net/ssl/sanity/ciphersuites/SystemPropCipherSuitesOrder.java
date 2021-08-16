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
 * @summary Test TLS ciphersuites order set through System properties
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_CHACHA20_POLY1305_SHA256,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
 *      -Djdk.tls.server.cipherSuites=TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256,TLS_AES_128_GCM_SHA256
 *      SystemPropCipherSuitesOrder TLSv1.3
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_CHACHA20_POLY1305_SHA256,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
 *      SystemPropCipherSuitesOrder TLSv1.3
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites=TLS_AES_128_GCM_SHA256,TLS_CHACHA20_POLY1305_SHA256,TLS_AES_256_GCM_SHA384
 *      SystemPropCipherSuitesOrder TLSv1.3
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
 *      -Djdk.tls.server.cipherSuites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
 *      SystemPropCipherSuitesOrder TLSv1.2
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
 *      SystemPropCipherSuitesOrder TLSv1.2
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
 *      SystemPropCipherSuitesOrder TLSv1.2
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      -Djdk.tls.server.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1.1
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1.1
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1.1
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      -Djdk.tls.server.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1
 * @run main/othervm
 *      -Djdk.tls.client.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1
 * @run main/othervm
 *      -Djdk.tls.server.cipherSuites=TLS_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA
 *      SystemPropCipherSuitesOrder TLSv1
 */
public class SystemPropCipherSuitesOrder extends SSLSocketTemplate {

    private final String protocol;
    private static String[] servercipherSuites;
    private static String[] clientcipherSuites;

    public static void main(String[] args) {
        servercipherSuites
                = toArray(System.getProperty("jdk.tls.server.cipherSuites"));
        clientcipherSuites
                = toArray(System.getProperty("jdk.tls.client.cipherSuites"));
        System.out.printf("SYSTEM PROPERTIES: ServerProp:%s - ClientProp:%s%n",
                Arrays.deepToString(servercipherSuites),
                Arrays.deepToString(clientcipherSuites));

        try {
            new SystemPropCipherSuitesOrder(args[0]).run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SystemPropCipherSuitesOrder(String protocol) {
        this.protocol = protocol;
        // Re-enable protocol if disabled.
        if (protocol.equals("TLSv1") || protocol.equals("TLSv1.1")) {
            SecurityUtils.removeFromDisabledTlsAlgs(protocol);
        }
    }

    // Servers are configured before clients, increment test case after.
    @Override
    protected void configureClientSocket(SSLSocket socket) {
        socket.setEnabledProtocols(new String[]{protocol});
    }

    @Override
    protected void configureServerSocket(SSLServerSocket serverSocket) {
        serverSocket.setEnabledProtocols(new String[]{protocol});
    }

    protected void runServerApplication(SSLSocket socket) throws Exception {
        if (servercipherSuites != null) {
            System.out.printf("SERVER: SystemProperty:%s - "
                    + "getEnabledCipherSuites:%s%n",
                    Arrays.deepToString(servercipherSuites),
                    Arrays.deepToString(socket.getEnabledCipherSuites()));
        }
        if (servercipherSuites != null && !Arrays.equals(
                servercipherSuites, socket.getEnabledCipherSuites())) {
            throw new RuntimeException("Unmatched server side CipherSuite order");
        }
        super.runServerApplication(socket);
    }

    protected void runClientApplication(SSLSocket socket) throws Exception {
        if (clientcipherSuites != null) {
            System.out.printf("CLIENT: SystemProperty:%s - "
                    + "getEnabledCipherSuites:%s%n",
                    Arrays.deepToString(clientcipherSuites),
                    Arrays.deepToString(socket.getEnabledCipherSuites()));
        }
        if (clientcipherSuites != null && !Arrays.equals(clientcipherSuites,
                socket.getEnabledCipherSuites())) {
            throw new RuntimeException("Unmatched client side CipherSuite order");
        }
        super.runClientApplication(socket);
    }

    private static String[] toArray(String prop) {
        return (prop != null) ? prop.split(",") : null;
    }
}
