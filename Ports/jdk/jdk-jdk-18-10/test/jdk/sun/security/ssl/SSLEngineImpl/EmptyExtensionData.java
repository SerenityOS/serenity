/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 6728126
 * @summary Parsing Extensions in Client Hello message is done in a wrong way
 * @library /test/lib
 * @run main/othervm EmptyExtensionData
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

import jdk.test.lib.security.SecurityUtils;

public class EmptyExtensionData {

    private static boolean debug = false;

    private static String pathToStores = "../../../../javax/net/ssl/etc";
    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static String passwd = "passphrase";

    private static String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

    private static void checkDone(SSLEngine ssle) throws Exception {
        if (!ssle.isInboundDone()) {
            throw new Exception("isInboundDone isn't done");
        }
        if (!ssle.isOutboundDone()) {
            throw new Exception("isOutboundDone isn't done");
        }
    }

    private static void runTest(SSLEngine ssle) throws Exception {
        // a client hello message with an empty extension data
        byte[] msg_clihello = {
                (byte)0x16, (byte)0x03, (byte)0x01, (byte)0x00,
                (byte)0x6f, (byte)0x01, (byte)0x00, (byte)0x00,
                (byte)0x6b, (byte)0x03, (byte)0x01, (byte)0x48,
                (byte)0x90, (byte)0x71, (byte)0xfc, (byte)0xf9,
                (byte)0xa2, (byte)0x3a, (byte)0xd7, (byte)0xa8,
                (byte)0x0b, (byte)0x25, (byte)0xf1, (byte)0x2b,
                (byte)0x88, (byte)0x80, (byte)0x66, (byte)0xca,
                (byte)0x07, (byte)0x78, (byte)0x2a, (byte)0x08,
                (byte)0x9d, (byte)0x62, (byte)0x1d, (byte)0x89,
                (byte)0xc9, (byte)0x1e, (byte)0x1f, (byte)0xe5,
                (byte)0x92, (byte)0xfe, (byte)0x8d, (byte)0x00,
                (byte)0x00, (byte)0x24, (byte)0x00, (byte)0x88,
                (byte)0x00, (byte)0x87, (byte)0x00, (byte)0x39,
                (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x84,
                (byte)0x00, (byte)0x35, (byte)0x00, (byte)0x45,
                (byte)0x00, (byte)0x44, (byte)0x00, (byte)0x33,
                (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x41,
                (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x05,
                (byte)0x00, (byte)0x2f, (byte)0x00, (byte)0x16,
                (byte)0x00, (byte)0x13, (byte)0xfe, (byte)0xff,
                (byte)0x00, (byte)0x0a, (byte)0x01, (byte)0x00,
                (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x16, (byte)0x00, (byte)0x14,
                (byte)0x00, (byte)0x00, (byte)0x11, (byte)0x6a,
                (byte)0x75, (byte)0x73, (byte)0x74, (byte)0x69,
                (byte)0x6e, (byte)0x2e, (byte)0x75, (byte)0x6b,
                (byte)0x2e, (byte)0x73, (byte)0x75, (byte)0x6e,
                (byte)0x2e, (byte)0x63, (byte)0x6f, (byte)0x6d,
                (byte)0x00, (byte)0x23, (byte)0x00, (byte)0x00
            };
        ByteBuffer bf_clihello = ByteBuffer.wrap(msg_clihello);

        SSLSession session = ssle.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        ByteBuffer serverIn = ByteBuffer.allocate(appBufferMax + 50);
        ByteBuffer serverOut = ByteBuffer.wrap("I'm Server".getBytes());
        ByteBuffer sTOc = ByteBuffer.allocate(netBufferMax);

        ssle.beginHandshake();

        // unwrap the clientHello message.
        SSLEngineResult result = ssle.unwrap(bf_clihello, serverIn);
        System.out.println("server unwrap " + result);
        runDelegatedTasks(result, ssle);

        // one more step, ensure the clientHello message is parsed.
        SSLEngineResult.HandshakeStatus status = ssle.getHandshakeStatus();
        if ( status == HandshakeStatus.NEED_UNWRAP) {
            result = ssle.unwrap(bf_clihello, serverIn);
            System.out.println("server unwrap " + result);
            runDelegatedTasks(result, ssle);
        } else if ( status == HandshakeStatus.NEED_WRAP) {
            result = ssle.wrap(serverOut, sTOc);
            System.out.println("server wrap " + result);
            runDelegatedTasks(result, ssle);
        } else {
            throw new Exception("unexpected handshake status " + status);
        }

        // enough, stop
    }

    /*
     * If the result indicates that we have outstanding tasks to do,
     * go ahead and run them in this thread.
     */
    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("\trunning delegated task...");
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            log("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    public static void main(String args[]) throws Exception {
        // Re-enable TLSv1 since test depends on it.
        SecurityUtils.removeFromDisabledTlsAlgs("TLSv1");

        SSLEngine ssle = createSSLEngine(keyFilename, trustFilename);
        runTest(ssle);

        System.out.println("Test Passed.");
    }

    /*
     * Create an initialized SSLContext to use for this test.
     */
    static private SSLEngine createSSLEngine(String keyFile, String trustFile)
            throws Exception {

        SSLEngine ssle;

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "passphrase".toCharArray();

        ks.load(new FileInputStream(keyFile), passphrase);
        ts.load(new FileInputStream(trustFile), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        ssle = sslCtx.createSSLEngine();
        ssle.setUseClientMode(false);

        return ssle;
    }


    private static void log(String str) {
        if (debug) {
            System.out.println(str);
        }
    }
}
