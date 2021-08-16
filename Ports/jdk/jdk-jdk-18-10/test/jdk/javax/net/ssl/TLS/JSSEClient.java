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

import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

class JSSEClient extends CipherTestUtils.Client {

    private static final String DEFAULT = "DEFAULT";
    private static final String TLS = "TLS";

    private final SSLContext context;
    private final MyX509KeyManager keyManager;
    private final int port;
    private final String host;
    private final String protocol;

    JSSEClient(CipherTestUtils cipherTest, String host, int port,
            String protocols, String ciphersuite) throws Exception {
        super(cipherTest, ciphersuite);
        this.host = host;
        this.port = port;
        this.protocol = protocols;
        this.keyManager = new MyX509KeyManager(
                                    cipherTest.getClientKeyManager());
        context = SSLContext.getInstance(TLS);
    }

    @Override
    void runTest(CipherTestUtils.TestParameters params) throws Exception {
        keyManager.setAuthType(params.clientAuth);
        context.init(
                new KeyManager[]{ keyManager },
                new TrustManager[]{ cipherTest.getClientTrustManager() },
                CipherTestUtils.secureRandom);
        SSLSocketFactory factory = (SSLSocketFactory)context.getSocketFactory();

        System.out.println("Connecting to server...");
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setSoTimeout(CipherTestUtils.TIMEOUT);
            socket.setEnabledCipherSuites(params.cipherSuite.split(","));
            if (params.protocol != null && !params.protocol.trim().isEmpty()
                    && !params.protocol.trim().equals(DEFAULT)) {
                socket.setEnabledProtocols(params.protocol.split(","));
            }
            CipherTestUtils.printInfo(socket);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            sendRequest(in, out);
            SSLSession session = socket.getSession();
            session.invalidate();
            String cipherSuite = session.getCipherSuite();
            if (params.cipherSuite.equals(cipherSuite) == false) {
                throw new RuntimeException("Negotiated ciphersuite mismatch: "
                        + cipherSuite + " != " + params.cipherSuite);
            }
            String protocol = session.getProtocol();
            if (!DEFAULT.equals(params.protocol)
                    && !params.protocol.contains(protocol)) {
                throw new RuntimeException("Negotiated protocol mismatch: "
                        + protocol + " != " + params.protocol);
            }
            if (!cipherSuite.contains("DH_anon")) {
                session.getPeerCertificates();
            }
            Certificate[] certificates = session.getLocalCertificates();
            if (params.clientAuth == null) {
                if (certificates != null) {
                    throw new RuntimeException("Local certificates "
                            + "should be null");
                }
            } else {
                if ((certificates == null) || (certificates.length == 0)) {
                    throw new RuntimeException("Certificates missing");
                }
                String keyAlg = certificates[0].getPublicKey().getAlgorithm();
                if ("EC".equals(keyAlg)) {
                    keyAlg = "ECDSA";
                }
                if (!params.clientAuth.equals(keyAlg)) {
                    throw new RuntimeException("Certificate type mismatch: "
                            + keyAlg + " != " + params.clientAuth);
                }
            }
        }
    }
}
