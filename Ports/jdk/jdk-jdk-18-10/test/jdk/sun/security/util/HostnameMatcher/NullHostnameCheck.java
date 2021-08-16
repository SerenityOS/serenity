/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jdk.test.lib.security.KeyStoreUtils;
import jdk.test.lib.security.SecurityUtils;
import jdk.test.lib.security.SSLContextBuilder;

/*
 * @test
 * @bug 8211339 8234728
 * @summary Verify hostname returns an exception instead of null pointer when
 * creating a new engine
 * @library /test/lib
 * @run main/othervm NullHostnameCheck TLSv1
 * @run main/othervm NullHostnameCheck TLSv1.1
 * @run main/othervm NullHostnameCheck TLSv1.2
 * @run main/othervm NullHostnameCheck TLSv1.3
 */

public final class NullHostnameCheck {

    public static void main(String[] args) throws Exception {
        String protocol = args[0];
        String password = "123456";

        // Re-enable TLSv1 or TLSv1.1 when test depends on it.
        if (protocol.equals("TLSv1") || protocol.equals("TLSv1.1")) {
            SecurityUtils.removeFromDisabledTlsAlgs(protocol);
        }

        SSLContext serverCtx = SSLContextBuilder.builder()
                .keyStore(KeyStoreUtils.loadKeyStoreBase64(
                        keystoreB64, password))
                .kmfPassphrase(password)
                .protocol(protocol)
                .build();
        SSLEngine serverEngine = serverCtx.createSSLEngine("localhost", -1);
        serverEngine.setUseClientMode(false);

        SSLContext clientCtx = SSLContext.getInstance(protocol);
        clientCtx.init(null, new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(
                        X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public void checkServerTrusted(
                        X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        }, null);

        SSLEngine clientEngine = clientCtx.createSSLEngine();
        clientEngine.setUseClientMode(true);

        SSLParameters sslParameters = clientEngine.getSSLParameters();
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        clientEngine.setSSLParameters(sslParameters);
        try {
            handshake(clientEngine, serverEngine);
            throw new Exception("Value was not null.  Unexpected.");
        } catch (SSLHandshakeException e) {
            if (e.getCause() instanceof CertificateException) {
                System.out.println("Correct Exception class thrown:\n\t" +
                        e.getMessage());
                return;
            }
            throw e;
        }
    }

    private static void handshake(SSLEngine clientEngine,
            SSLEngine serverEngine) throws SSLException{
        ByteBuffer cTOs = ByteBuffer.allocate(
                clientEngine.getSession().getPacketBufferSize());
        ByteBuffer sTOc = ByteBuffer.allocate(
                serverEngine.getSession().getPacketBufferSize());

        ByteBuffer serverAppReadBuffer = ByteBuffer.allocate(
                serverEngine.getSession().getApplicationBufferSize());
        ByteBuffer clientAppReadBuffer = ByteBuffer.allocate(
                clientEngine.getSession().getApplicationBufferSize());

        clientEngine.beginHandshake();
        serverEngine.beginHandshake();

        ByteBuffer empty = ByteBuffer.allocate(0);

        SSLEngineResult clientResult;
        SSLEngineResult serverResult;

        boolean clientHandshakeFinished = false;
        boolean serverHandshakeFinished = false;

        do {
            if (!clientHandshakeFinished) {
                clientResult = clientEngine.wrap(empty, cTOs);
                runDelegatedTasks(clientResult, clientEngine);

                if (isHandshakeFinished(clientResult)) {
                    clientHandshakeFinished = true;
                }
            }

            if (!serverHandshakeFinished) {
                serverResult = serverEngine.wrap(empty, sTOc);
                runDelegatedTasks(serverResult, serverEngine);

                if (isHandshakeFinished(serverResult)) {
                    serverHandshakeFinished = true;
                }
            }

            cTOs.flip();
            sTOc.flip();

            if (!clientHandshakeFinished) {
                clientResult = clientEngine.unwrap(sTOc, clientAppReadBuffer);

                runDelegatedTasks(clientResult, clientEngine);

                if (isHandshakeFinished(clientResult)) {
                    clientHandshakeFinished = true;
                }
            }

            if (!serverHandshakeFinished) {
                serverResult = serverEngine.unwrap(cTOs, serverAppReadBuffer);
                runDelegatedTasks(serverResult, serverEngine);

                if (isHandshakeFinished(serverResult)) {
                    serverHandshakeFinished = true;
                }
            }

            sTOc.compact();
            cTOs.compact();
        } while (!clientHandshakeFinished || !serverHandshakeFinished);
    }

    private static boolean isHandshakeFinished(SSLEngineResult result) {
        return result.getHandshakeStatus() ==
                SSLEngineResult.HandshakeStatus.FINISHED;
    }

    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) {
        if (result.getHandshakeStatus() ==
                SSLEngineResult.HandshakeStatus.NEED_TASK) {
            for (;;) {
                Runnable task = engine.getDelegatedTask();
                if (task == null) {
                    break;
                }
                task.run();
            }
        }
    }

    // Base64 of PKCS12 Keystore
    /*
     * Certificate
     * "signature algorithm": "SHA384withRSA",
     * "issuer"             : "CN=test, OU=test, O=test, L=test, ST=test, C=test",
     * "not before"         : "2019-12-05 12:43:23.000 IST",
     * "not  after"         : "2049-11-27 12:43:23.000 IST",
     * "subject"            : "CN=test, OU=test, O=test, L=test, ST=test, C=test",
     * "subject public key" : "RSA",
     */
    static final String keystoreB64 =
        "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"
        + "y3daZV4AIDAYag";
}
