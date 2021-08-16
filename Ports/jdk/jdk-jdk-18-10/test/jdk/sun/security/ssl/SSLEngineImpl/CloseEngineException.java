/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4969799
 * @summary javax.net.ssl.SSLSocket.SSLSocket(InetAddress,int) shouldn't
 *              throw exception
 * @run main/othervm CloseEngineException
 */

//
// This is making sure that starting a new handshake throws the right
// exception.  There is a similar test for SSLSocket.
//

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

// Note that this test case depends on JSSE provider implementation details.
public class CloseEngineException {

    private static boolean debug = true;

    private SSLContext sslc;
    private SSLEngine ssle1;    // client
    private SSLEngine ssle2;    // server

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

        ssle2 = sslc.createSSLEngine();
        ssle2.setUseClientMode(false);
        ssle2.setNeedClientAuth(true);
    }

    private void runTest() throws Exception {
        boolean dataDone = false;

        createSSLEngines();
        createBuffers();

        SSLEngineResult result1;        // ssle1's results from last operation
        SSLEngineResult result2;        // ssle2's results from last operation

        while (!isEngineClosed(ssle1) && !isEngineClosed(ssle2)) {

            log("================");

            if (!isEngineClosed(ssle1)) {
                result1 = ssle1.wrap(appOut1, oneToTwo);
                runDelegatedTasks(result1, ssle1);

                log("wrap1:  " + result1);
                log("oneToTwo  = " + oneToTwo);
                log("");

                oneToTwo.flip();
            }
            if (!isEngineClosed(ssle2)) {
                result2 = ssle2.wrap(appOut2, twoToOne);
                runDelegatedTasks(result2, ssle2);

                log("wrap2:  " + result2);
                log("twoToOne  = " + twoToOne);

                twoToOne.flip();
            }

            log("----");

            if (!isEngineClosed(ssle1) && !dataDone) {
            log("--");
                result1 = ssle1.unwrap(twoToOne, appIn1);
                runDelegatedTasks(result1, ssle1);

                log("unwrap1: " + result1);
                log("twoToOne  = " + twoToOne);
                log("");

                twoToOne.compact();
            }
            if (!isEngineClosed(ssle2)) {
            log("---");
                result2 = ssle2.unwrap(oneToTwo, appIn2);
                runDelegatedTasks(result2, ssle2);

                log("unwrap2: " + result2);
                log("oneToTwo  = " + oneToTwo);

                oneToTwo.compact();
            }

            /*
             * If we've transfered all the data between app1 and app2,
             * we try to close and see what that gets us.
             */
            if (!dataDone && (appOut1.limit() == appIn2.position()) &&
                    (appOut2.limit() == appIn1.position())) {

                checkTransfer(appOut1, appIn2);
                checkTransfer(appOut2, appIn1);

                log("Closing ssle1's *OUTBOUND*...");
                ssle1.closeOutbound();
                dataDone = true;

                try {
                    /*
                     * Check that closed Outbound generates.
                     */
                    ssle1.beginHandshake();
                    throw new Exception(
                        "TEST FAILED:  didn't throw Exception");
                } catch (SSLException e) {
                    System.err.println("PARTIAL PASS");
                }
            }
        }

        try {
            /*
             * Check that closed Inbound generates.
             */
            ssle2.beginHandshake();
            throw new Exception(
                "TEST FAILED:  didn't throw Exception");
        } catch (SSLException e) {
            System.err.println("TEST PASSED");
        }
    }

    public static void main(String args[]) throws Exception {

        CloseEngineException test;

        test = new CloseEngineException();

        test.createSSLEngines();

        test.runTest();

        System.err.println("Test Passed.");
    }

    /*
     * **********************************************************
     * Majority of the test case is above, below is just setup stuff
     * **********************************************************
     */

    public CloseEngineException() throws Exception {
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

    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                log("running delegated task...");
                runnable.run();
            }
        }
    }

    private static boolean isEngineClosed(SSLEngine engine) {
        return (engine.isOutboundDone() && engine.isInboundDone());
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
            System.err.println(str);
        }
    }
}
