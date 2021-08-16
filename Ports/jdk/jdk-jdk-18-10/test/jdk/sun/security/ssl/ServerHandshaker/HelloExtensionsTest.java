/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173783
 * @summary 6u141 IllegalArgumentException: jdk.tls.namedGroups
 * run main/othervm HelloExtensionsTest
 * run main/othervm HelloExtensionsTest -Djdk.tls.namedGroups="bug, bug"
 * run main/othervm HelloExtensionsTest -Djdk.tls.namedGroups="secp521r1"
 *
 */
import javax.crypto.*;
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.nio.*;
import java.security.*;

public class HelloExtensionsTest {

    private static boolean debug = false;
    private static boolean proceed = true;
    private static boolean EcAvailable = isEcAvailable();

    static String pathToStores = "../../../../javax/net/ssl/etc";
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

         /*

         A client hello message captured via wireshark by selecting
         a TLSv1.2 Client Hello record and clicking through to the
         TLSv1.2 Record Layer line and then selecting the hex stream
         via "copy -> bytes -> hex stream".

         For Record purposes, here's the ClientHello :

         *** ClientHello, TLSv1.2
         RandomCookie:  GMT: 1469560450 bytes = { 108, 140, 12, 202,
         2, 213, 10, 236, 143, 223, 58, 162, 228, 155, 239, 3, 98,
         232, 89, 41, 116, 120, 13, 37, 105, 153, 97, 241 }
         Session ID:  {}
         Cipher Suites: [TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
         TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TLS_RSA_WITH_AES_128_CBC_SHA256,
         TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,
         TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,
         TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,
         TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,
         TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
         TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
         TLS_RSA_WITH_AES_128_CBC_SHA,
         TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,
         TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,
         TLS_DHE_RSA_WITH_AES_128_CBC_SHA,
         TLS_DHE_DSS_WITH_AES_128_CBC_SHA,
         TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
         TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
         TLS_RSA_WITH_AES_128_GCM_SHA256,
         TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256,
         TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256,
         TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
         TLS_DHE_DSS_WITH_AES_128_GCM_SHA256,
         TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,
         TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,
         SSL_RSA_WITH_3DES_EDE_CBC_SHA,
         TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,
         TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,
         SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,
         SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,
         TLS_EMPTY_RENEGOTIATION_INFO_SCSV]
         Compression Methods:  { 0 }
         Extension elliptic_curves, curve names: {secp256r1,
         sect163k1, sect163r2, secp192r1, secp224r1, sect233k1, sect233r1,
         sect283k1, sect283r1, secp384r1, sect409k1, sect409r1, secp521r1,
         sect571k1, sect571r1, secp160k1, secp160r1, secp160r2, sect163r1,
         secp192k1, sect193r1, sect193r2, secp224k1, sect239k1, secp256k1}
         Extension ec_point_formats, formats: [uncompressed]
         Extension signature_algorithms, signature_algorithms:
         SHA512withECDSA, SHA512withRSA, SHA384withECDSA, SHA384withRSA,
         SHA256withECDSA, SHA256withRSA, Unknown (hash:0x3, signature:0x3),
         Unknown (hash:0x3, signature:0x1), SHA1withECDSA,
         SHA1withRSA, SHA1withDSA
         Extension server_name, server_name:
         [host_name: bugs.openjdk.java.net]
         */

        String hello = "16030300df010000db03035898b7826c8c0cc" +
            "a02d50aec8fdf3aa2e49bef0362e8592974780d25699961f" +
            "100003ac023c027003cc025c02900670040c009c013002fc" +
            "004c00e00330032c02bc02f009cc02dc031009e00a2c008c" +
            "012000ac003c00d0016001300ff01000078000a003400320" +
            "0170001000300130015000600070009000a0018000b000c0" +
            "019000d000e000f001000110002001200040005001400080" +
            "016000b00020100000d00180016060306010503050104030" +
            "401030303010203020102020000001a00180000156275677" +
            "32e6f70656e6a646b2e6a6176612e6e6574";

        byte[] msg_clihello = hexStringToByteArray(hello);
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

        if (!proceed) {
            //expected exception occurred. Don't process anymore
            return;
        }

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
            try {
                while ((runnable = engine.getDelegatedTask()) != null) {
                    log("\trunning delegated task...");
                    runnable.run();
                }
            } catch (ExceptionInInitializerError e) {
                String v = System.getProperty("jdk.tls.namedGroups");
                if (!EcAvailable || v == null) {
                    // we weren't expecting this if no EC providers
                    throw new RuntimeException("Unexpected Error :" + e);
                }
                if (v != null && v.contains("bug")) {
                    // OK - we were expecting this Error
                    log("got expected error for bad jdk.tls.namedGroups");
                    proceed = false;
                    return;
                } else {
                    System.out.println("Unexpected error. " +
                        "jdk.tls.namedGroups value: " + v);
                    throw e;
                }
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            log("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static boolean isEcAvailable() {
        try {
            Signature.getInstance("SHA1withECDSA");
            Signature.getInstance("NONEwithECDSA");
            KeyAgreement.getInstance("ECDH");
            KeyFactory.getInstance("EC");
            KeyPairGenerator.getInstance("EC");
            AlgorithmParameters.getInstance("EC");
        } catch (Exception e) {
            log("EC not available. Received: " + e);
            return false;
        }
        return true;
    }

    public static void main(String args[]) throws Exception {
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
