/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4495742
 * @ignore JSSE supported cipher suites are changed with CR 6916074,
 *     need to update this test case in JDK 7 soon
 * @run main/timeout=180 TestAllSuites
 * @summary Add non-blocking SSL/TLS functionality, usable with any
 *      I/O abstraction
 *
 * Iterate through all the suites using both TLS and SSLv3, and turn
 * SSLv2Hello off and on.  Exchange some bytes and shutdown.
 *
 * @author Brad Wetmore
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;
import java.util.*;

public class TestAllSuites {

    private static boolean debug = false;

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

    String [][] protocols = new String [][] {
        { "SSLv3" },
        { "TLSv1" },
        { "SSLv3", "SSLv2Hello"},
        { "TLSv1", "SSLv2Hello"}
    };

    /*
     * Majority of the test case is here, setup is done below.
     */

    private void createSSLEngines() throws Exception {
        ssle1 = sslc.createSSLEngine("client", 1);
        ssle1.setUseClientMode(true);

        ssle2 = sslc.createSSLEngine("server", 2);
        ssle2.setUseClientMode(false);
    }

    private void test() throws Exception {

        createSSLEngines();
        String [] suites = ssle1.getSupportedCipherSuites();

        for (int i = 0; i < suites.length; i++) {
            for (int j = 0; j < protocols.length; j++) {
                createSSLEngines();
                runTest(suites[i], protocols[j]);
            }
        }
    }

    private void runTest(String suite, String [] protocols) throws Exception {

        boolean dataDone = false;

        System.out.println("======================================");
        System.out.println("Testing: " + suite);
        for (int i = 0; i < protocols.length; i++) {
            System.out.print(protocols[i] + " ");
        }

        /*
         * Don't run the Kerberized suites for now.
         */
        if (suite.startsWith("TLS_KRB5")) {
            System.out.println("Ignoring Kerberized suite");
            return;
        }

        /*
         * Don't run the SCSV suite
         */
        if (suite.equals("TLS_EMPTY_RENEGOTIATION_INFO_SCSV")) {
            System.out.println("Ignoring SCSV suite");
            return;
        }


        if (!suite.contains("DH_anon")) {
            ssle2.setNeedClientAuth(true);
        }

        String [] suites = new String [] { suite };

        ssle1.setEnabledCipherSuites(suites);
        ssle2.setEnabledCipherSuites(suites);

        ssle1.setEnabledProtocols(protocols);
        ssle2.setEnabledProtocols(protocols);

        createBuffers();

        SSLEngineResult result1;        // ssle1's results from last operation
        SSLEngineResult result2;        // ssle2's results from last operation

        Date start = new Date();
        while (!isEngineClosed(ssle1) || !isEngineClosed(ssle2)) {

            log("----------------");

            result1 = ssle1.wrap(appOut1, oneToTwo);
            result2 = ssle2.wrap(appOut2, twoToOne);

            log("wrap1:  " + result1);
            log("oneToTwo  = " + oneToTwo);
            log("");

            log("wrap2:  " + result2);
            log("twoToOne  = " + twoToOne);

            runDelegatedTasks(result1, ssle1);
            runDelegatedTasks(result2, ssle2);

            oneToTwo.flip();
            twoToOne.flip();

            log("----");

            result1 = ssle1.unwrap(twoToOne, appIn1);
            result2 = ssle2.unwrap(oneToTwo, appIn2);

            log("unwrap1: " + result1);
            log("twoToOne  = " + twoToOne);
            log("");

            log("unwrap2: " + result2);
            log("oneToTwo  = " + oneToTwo);

            runDelegatedTasks(result1, ssle1);
            runDelegatedTasks(result2, ssle2);

            oneToTwo.compact();
            twoToOne.compact();

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
            }
        }

        /*
         * Just for grins, try closing again, make sure nothing
         * strange is happening after we're closed.
         */
        ssle1.closeInbound();
        ssle1.closeOutbound();

        ssle2.closeInbound();
        ssle2.closeOutbound();

        appOut1.rewind();
        appIn1.clear();
        oneToTwo.clear();

        result1 = ssle1.wrap(appOut1, oneToTwo);
        checkResult(result1);

        result1 = ssle1.unwrap(oneToTwo, appIn1);
        checkResult(result1);

        System.out.println("Test Passed.");
        System.out.println("\n======================================");

        Date end = new Date();
        elapsed += end.getTime() - start.getTime();

    }

    static long elapsed = 0;

    private static void checkResult(SSLEngineResult result) throws Exception {
        if ((result.getStatus() != Status.CLOSED) ||
                (result.getHandshakeStatus() !=
                    HandshakeStatus.NOT_HANDSHAKING) ||
                (result.bytesConsumed() != 0) ||
                (result.bytesProduced() != 0)) {
            throw new Exception("Unexpected close status");
        }
    }

    public static void main(String args[]) throws Exception {

        TestAllSuites tas;

        tas = new TestAllSuites();

        tas.createSSLEngines();

        tas.test();

        System.out.println("All Tests Passed.");
        System.out.println("Elapsed time: " + elapsed / 1000.0);
    }

    /*
     * **********************************************************
     * Majority of the test case is above, below is just setup stuff
     * **********************************************************
     */

    public TestAllSuites() throws Exception {
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
            System.out.println(str);
        }
    }
}
