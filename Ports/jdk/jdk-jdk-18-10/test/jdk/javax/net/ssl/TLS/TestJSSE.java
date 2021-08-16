/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Provider;
import java.security.Security;

public class TestJSSE {

    private static final String LOCAL_IP = "127.0.0.1";

    public static void main(String... args) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        // enable debug output
        System.setProperty("javax.net.debug", "ssl,record");

        String srvProtocol = System.getProperty("SERVER_PROTOCOL");
        String clnProtocol = System.getProperty("CLIENT_PROTOCOL");
        String cipher = System.getProperty("CIPHER");
        if (srvProtocol == null || clnProtocol == null || cipher == null) {
            throw new IllegalArgumentException("Incorrect parameters");
        }

        System.out.println("ServerProtocol = " + srvProtocol);
        System.out.println("ClientProtocol = " + clnProtocol);
        System.out.println("Cipher         = " + cipher);

        try (CipherTestUtils.Server srv = server(srvProtocol, cipher, args)) {
            client(srv.getPort(), clnProtocol, cipher, args);
        }
    }

    public static void client(int port, String protocols, String cipher,
            String... exceptions) throws Exception {

        String expectedExcp = exceptions.length >= 1 ? exceptions[0] : null;

        System.out.println("This is client");
        System.out.println("Testing protocol: " + protocols);
        System.out.println("Testing cipher  : " + cipher);

        CipherTestUtils.mainClient(
            new JSSEFactory(LOCAL_IP, protocols, cipher, "Client JSSE"),
            port, expectedExcp);
    }

    public static CipherTestUtils.Server server(String protocol,
                String cipher, String... exceptions) throws Exception {

        String expectedExcp = exceptions.length >= 1 ? exceptions[0] : null;

        System.out.println("This is server");
        System.out.println("Testing protocol: " + protocol);
        System.out.println("Testing cipher  : " + cipher);

        return CipherTestUtils.mainServer(
            new JSSEFactory(null, protocol, cipher, "Server JSSE"),
            expectedExcp);
    }

    private static class JSSEFactory extends CipherTestUtils.PeerFactory {

        private final String cipher;
        private final String protocol;
        private final String host;
        private final String name;

        JSSEFactory(String host, String protocol, String cipher, String name) {
            this.cipher = cipher;
            this.protocol = protocol;
            this.host = host;
            this.name = name;
        }

        @Override
        String getName() {
            return name;
        }

        @Override
        String getTestedCipher() {
            return cipher;
        }

        @Override
        String getTestedProtocol() {
            return protocol;
        }

        @Override
        CipherTestUtils.Client newClient(CipherTestUtils cipherTest, int port)
                throws Exception {
            return new JSSEClient(cipherTest, host, port, protocol, cipher);
        }

        @Override
        CipherTestUtils.Server newServer(CipherTestUtils cipherTest, int port)
                throws Exception {
            return new JSSEServer(cipherTest, port, protocol, cipher);
        }
    }
}
