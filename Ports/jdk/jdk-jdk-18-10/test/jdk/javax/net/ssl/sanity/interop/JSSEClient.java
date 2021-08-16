/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

class JSSEClient extends CipherTest.Client {

    private final SSLContext sslContext;
    private final MyX509KeyManager keyManager;

    JSSEClient(CipherTest cipherTest) throws Exception {
        super(cipherTest);
        this.keyManager = new MyX509KeyManager(CipherTest.keyManager);
        sslContext = SSLContext.getInstance("TLS");
    }

    void runTest(CipherTest.TestParameters params) throws Exception {
        SSLSocket socket = null;
        try {
            keyManager.setAuthType(params.clientAuth);
            sslContext.init(
                    new KeyManager[] { keyManager },
                    new TrustManager[] { CipherTest.trustManager },
                    CipherTest.secureRandom);
            SSLSocketFactory factory = (SSLSocketFactory)sslContext.getSocketFactory();
            socket = (SSLSocket)factory.createSocket("127.0.0.1", CipherTest.serverPort);
            socket.setSoTimeout(CipherTest.TIMEOUT);
            socket.setEnabledCipherSuites(new String[] { params.cipherSuite.name() });
            socket.setEnabledProtocols(new String[] { params.protocol.name });
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            sendRequest(in, out);
            socket.close();
            SSLSession session = socket.getSession();
            session.invalidate();
            String cipherSuite = session.getCipherSuite();
            if (!params.cipherSuite.name().equals(cipherSuite)) {
                throw new Exception("Negotiated ciphersuite mismatch: "
                        + cipherSuite + " != " + params.cipherSuite);
            }
            String protocol = session.getProtocol();
            if (!params.protocol.name.equals(protocol)) {
                throw new Exception("Negotiated protocol mismatch: " + protocol
                        + " != " + params.protocol);
            }
            if (cipherSuite.indexOf("DH_anon") == -1) {
                session.getPeerCertificates();
            }
            Certificate[] certificates = session.getLocalCertificates();
            if (params.clientAuth == null) {
                if (certificates != null) {
                    throw new Exception("Local certificates should be null");
                }
            } else {
                if ((certificates == null) || (certificates.length == 0)) {
                    throw new Exception("Certificates missing");
                }
                String keyAlg = certificates[0].getPublicKey().getAlgorithm();
                if (params.clientAuth != keyAlg) {
                    throw new Exception("Certificate type mismatch: " + keyAlg + " != " + params.clientAuth);
                }
            }
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

}
