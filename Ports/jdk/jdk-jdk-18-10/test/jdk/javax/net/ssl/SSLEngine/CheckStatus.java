/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4948079
 * @summary SSLEngineResult needs updating [none yet]
 * @ignore the dependent implementation details are changed
 * @run main/othervm -Djsse.enableCBCProtection=false CheckStatus
 *
 * @author Brad Wetmore
 */

/*
 * This is a simple hack to test a bunch of conditions and check
 * their return codes.
 */
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

public class CheckStatus {

    private static boolean debug = true;

    private SSLContext sslc;
    private SSLEngine ssle1;    // client
    private SSLEngine ssle2;    // server

    private static String pathToStores = "../etc";
    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static String passwd = "passphrase";

    private static String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
    private static String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

    private ByteBuffer appOut1;         // write side of ssle1
    private ByteBuffer appIn1;          // read side of ssle1
    private ByteBuffer appOut2;         // write side of ssle2
    private ByteBuffer appIn2;          // read side of ssle2

    private ByteBuffer oneToTwo;        // "reliable" transport ssle1->ssle2
    private ByteBuffer twoToOne;        // "reliable" transport ssle2->ssle1

    /*
     * Majority of the test case is here, setup is done below.
     */

    private void createSSLEngines() throws Exception {
        ssle1 = sslc.createSSLEngine("client", 1);
        ssle1.setUseClientMode(true);

        ssle2 = sslc.createSSLEngine("server", 2);
        ssle2.setUseClientMode(false);
    }

    private boolean isHandshaking(SSLEngine e) {
        return (e.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);
    }

    private void checkResult(ByteBuffer bbIn, ByteBuffer bbOut,
            SSLEngineResult result,
            Status status, HandshakeStatus hsStatus,
            int consumed, int produced)
            throws Exception {

        if ((status != null) && (result.getStatus() != status)) {
            throw new Exception("Unexpected Status: need = " + status +
                " got = " + result.getStatus());
        }

        if ((hsStatus != null) && (result.getHandshakeStatus() != hsStatus)) {
            throw new Exception("Unexpected hsStatus: need = " + hsStatus +
                " got = " + result.getHandshakeStatus());
        }

        if ((consumed != -1) && (consumed != result.bytesConsumed())) {
            throw new Exception("Unexpected consumed: need = " + consumed +
                " got = " + result.bytesConsumed());
        }

        if ((produced != -1) && (produced != result.bytesProduced())) {
            throw new Exception("Unexpected produced: need = " + produced +
                " got = " + result.bytesProduced());
        }

        if ((consumed != -1) && (bbIn.position() != result.bytesConsumed())) {
            throw new Exception("Consumed " + bbIn.position() +
                " != " + consumed);
        }

        if ((produced != -1) && (bbOut.position() != result.bytesProduced())) {
            throw new Exception("produced " + bbOut.position() +
                " != " + produced);
        }
    }

    private void test() throws Exception {
        createSSLEngines();
        createBuffers();

        SSLEngineResult result1;        // ssle1's results from last operation
        SSLEngineResult result2;        // ssle2's results from last operation

        String [] suite1 = new String [] {
            "SSL_RSA_WITH_RC4_128_MD5" };
        String [] suite2 = new String [] {
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA" };

        ssle1.setEnabledCipherSuites(suite1);
        ssle2.setEnabledCipherSuites(suite1);

        log("================");

        log("unexpected empty unwrap");
        twoToOne.limit(0);
        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);
        twoToOne.limit(twoToOne.capacity());

        log("======================================");
        log("client hello");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);

        oneToTwo.compact();

        log("Check for unwrap when wrap needed");
        result2 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result2,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);

        log("======================================");
        log("ServerHello");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_TASK, result2.bytesProduced(), 0);
        twoToOne.compact();

        runDelegatedTasks(ssle1);

        log("======================================");
        log("Key Exchange");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);

        oneToTwo.compact();

        log("======================================");
        log("CCS");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_UNWRAP,
             result1.bytesProduced(), 0);

        oneToTwo.compact();

        log("======================================");
        log("Finished");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_WRAP, result1.bytesProduced(), 0);

        oneToTwo.compact();

        log("======================================");
        log("CCS");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("======================================");
        log("FINISHED");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.FINISHED, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.FINISHED, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("======================================");
        log("Check Session/Ciphers");

        String suite = ssle1.getSession().getCipherSuite();
        if (!suite.equals(suite1[0])) {
            throw new Exception("suites not equal: " + suite + "/" +
                suite1[0]);
        }

        suite = ssle2.getSession().getCipherSuite();
        if (!suite.equals(suite1[0])) {
            throw new Exception("suites not equal: " + suite + "/" +
                suite1[0]);
        }

        log("======================================");
        log("DATA");

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut1.capacity(), -1);
        oneToTwo.flip();

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut2.capacity(), -1);
        twoToOne.flip();

        SSLEngineResult result3 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result3,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            result2.bytesProduced(), result2.bytesConsumed());
        twoToOne.compact();

        SSLEngineResult result4 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result4,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            result1.bytesProduced(), result1.bytesConsumed());
        oneToTwo.compact();

        appIn1.clear();
        appIn2.clear();
        appOut1.rewind();
        appOut2.rewind();

        log("======================================");
        log("RENEGOTIATE");

        ssle2.getSession().invalidate();
        ssle2.setNeedClientAuth(true);

        ssle1.setEnabledCipherSuites(suite2);
        ssle2.setEnabledCipherSuites(suite2);

        ssle2.beginHandshake();

        log("======================================");
        log("HelloRequest");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_TASK, result2.bytesProduced(), 0);
        twoToOne.compact();

        runDelegatedTasks(ssle1);

        log("======================================");
        log("ClientHello");

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);

        oneToTwo.compact();

        log("======================================");
        log("CLIENT->SERVER DATA IN MIDDLE OF HANDSHAKE");

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP,
            appOut1.capacity(), -1);
        oneToTwo.flip();

        result4 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result4,
            Status.OK, HandshakeStatus.NEED_WRAP,
            result1.bytesProduced(), result1.bytesConsumed());
        oneToTwo.compact();

        appIn2.clear();
        appOut1.rewind();

        log("======================================");
        log("ServerHello");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_TASK, result2.bytesProduced(), 0);
        twoToOne.compact();

        runDelegatedTasks(ssle1);

        log("======================================");
        log("SERVER->CLIENT DATA IN MIDDLE OF HANDSHAKE");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_UNWRAP,
            appOut2.capacity(), -1);
        twoToOne.flip();

        result3 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result3,
            Status.OK, HandshakeStatus.NEED_WRAP,
            result2.bytesProduced(), result2.bytesConsumed());
        twoToOne.compact();

        appIn1.clear();
        appOut2.rewind();

        log("======================================");
        log("Client Cert and Key Exchange");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_TASK, result1.bytesProduced(), 0);
        runDelegatedTasks(ssle2);

        oneToTwo.compact();

        log("======================================");
        log("CCS");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_UNWRAP,
             result1.bytesProduced(), 0);

        oneToTwo.compact();

        log("======================================");
        log("Finished");
        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.OK, HandshakeStatus.NEED_UNWRAP, 0, -1);

        oneToTwo.flip();
        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.OK, HandshakeStatus.NEED_WRAP, result1.bytesProduced(), 0);

        oneToTwo.compact();

        log("======================================");
        log("CCS");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NEED_WRAP, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.NEED_UNWRAP, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("======================================");
        log("FINISHED");

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.FINISHED, 0, -1);
        twoToOne.flip();

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.OK, HandshakeStatus.FINISHED, result2.bytesProduced(), 0);
        twoToOne.compact();

        log("======================================");
        log("Check Session/Ciphers");

        suite = ssle1.getSession().getCipherSuite();
        if (!suite.equals(suite2[0])) {
            throw new Exception("suites not equal: " + suite + "/" +
                suite2[0]);
        }

        suite = ssle2.getSession().getCipherSuite();
        if (!suite.equals(suite2[0])) {
            throw new Exception("suites not equal: " + suite + "/" +
                suite2[0]);
        }

        log("======================================");
        log("DATA USING NEW SESSION");

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut1.capacity(), -1);
        oneToTwo.flip();

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            appOut2.capacity(), -1);
        twoToOne.flip();

        result3 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result3,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            result2.bytesProduced(), result2.bytesConsumed());
        twoToOne.compact();

        result4 = ssle2.unwrap(oneToTwo, appIn2);
        checkResult(oneToTwo, appIn2, result4,
            Status.OK, HandshakeStatus.NOT_HANDSHAKING,
            result1.bytesProduced(), result1.bytesConsumed());
        oneToTwo.compact();

        appIn1.clear();
        appIn2.clear();
        appOut1.rewind();
        appOut2.rewind();

        log("======================================");
        log("CN");

        if (isHandshaking(ssle1)) {
            throw new Exception("ssle1 IS handshaking");
        }

        if (isHandshaking(ssle2)) {
            throw new Exception("ssle2 IS handshaking");
        }

        ssle2.closeOutbound();

        if (!isHandshaking(ssle2)) {
            throw new Exception("ssle1 IS NOT handshaking");
        }

        appOut1.rewind();
        appOut2.rewind();

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.CLOSED, HandshakeStatus.NEED_UNWRAP, 0, -1);
        twoToOne.flip();

        if (ssle1.isInboundDone()) {
            throw new Exception("ssle1 inboundDone");
        }

        result1 = ssle1.unwrap(twoToOne, appIn1);
        checkResult(twoToOne, appIn1, result1,
            Status.CLOSED, HandshakeStatus.NEED_WRAP,
            result2.bytesProduced(), 0);
        twoToOne.compact();

        if (!ssle1.isInboundDone()) {
            throw new Exception("ssle1 inboundDone");
        }

        if (!isHandshaking(ssle1)) {
            throw new Exception("ssle1 IS NOT handshaking");
        }

        result2 = ssle2.wrap(appOut2, twoToOne);
        checkResult(appOut2, twoToOne, result2,
            Status.CLOSED, HandshakeStatus.NEED_UNWRAP, 0, 0);
        twoToOne.flip();

        log("======================================");
        log("CN response");

        if (ssle1.isOutboundDone()) {
            throw new Exception("ssle1 outboundDone");
        }

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(appOut1, oneToTwo, result1,
             Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, -1);

        if (!ssle1.isOutboundDone()) {
            throw new Exception("ssle1 outboundDone is NOT done");
        }

        if (isHandshaking(ssle1)) {
            throw new Exception("ssle1 IS handshaking");
        }

        oneToTwo.flip();

        if (!ssle2.isOutboundDone()) {
            throw new Exception("ssle1 outboundDone");
        }

        if (ssle2.isInboundDone()) {
            throw new Exception("ssle1 inboundDone");
        }

        result2 = ssle2.unwrap(oneToTwo, appIn2);

        checkResult(oneToTwo, appIn2, result2,
             Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING,
             result1.bytesProduced(), 0);

        if (!ssle2.isOutboundDone()) {
            throw new Exception("ssle1 outboundDone is NOT done");
        }

        if (!ssle2.isInboundDone()) {
            throw new Exception("ssle1 inboundDone is NOT done");
        }

        if (isHandshaking(ssle2)) {
            throw new Exception("ssle1 IS handshaking");
        }

        oneToTwo.compact();
    }

    public static void main(String args[]) throws Exception {
        // reset the security property to make sure that the algorithms
        // and keys used in this test are not disabled.
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        CheckStatus cs;

        cs = new CheckStatus();

        cs.createSSLEngines();

        cs.test();

        System.out.println("Test Passed.");
    }

    /*
     * **********************************************************
     * Majority of the test case is above, below is just setup stuff
     * **********************************************************
     */

    public CheckStatus() throws Exception {
        sslc = getSSLContext(keyFilename, trustFilename);
    }

    /*
     * Create an initialized SSLContext to use for this test.
     */
    private SSLContext getSSLContext(String keyFile, String trustFile)
            throws Exception {

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

        return sslCtx;
    }

    private void createBuffers() {
        // Size the buffers as appropriate.

        SSLSession session = ssle1.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        appIn1 = ByteBuffer.allocateDirect(appBufferMax + 50);
        appIn2 = ByteBuffer.allocateDirect(appBufferMax + 50);

        oneToTwo = ByteBuffer.allocateDirect(netBufferMax);
        twoToOne = ByteBuffer.allocateDirect(netBufferMax);

        appOut1 = ByteBuffer.wrap("Hi Engine2, I'm SSLEngine1".getBytes());
        appOut2 = ByteBuffer.wrap("Hello Engine1, I'm SSLEngine2".getBytes());

        log("AppOut1 = " + appOut1);
        log("AppOut2 = " + appOut2);
        log("");
    }

    private static void runDelegatedTasks(SSLEngine engine) throws Exception {

        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            log("running delegated task...");
            runnable.run();
        }
    }

    private static void checkTransfer(ByteBuffer a, ByteBuffer b)
            throws Exception {
        a.flip();
        b.flip();

        if (!a.equals(b)) {
            throw new Exception("Data didn't transfer cleanly");
        } else {
            log("Data transferred cleanly");
        }

        a.position(a.limit());
        b.position(b.limit());
        a.limit(a.capacity());
        b.limit(b.capacity());
    }

    private static void log(String str) {
        if (debug) {
            System.out.println(str);
        }
    }
}
