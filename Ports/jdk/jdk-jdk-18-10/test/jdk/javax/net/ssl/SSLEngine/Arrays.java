/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5019096
 * @summary Add scatter/gather APIs for SSLEngine
 * @library /test/lib
 * @run main/othervm Arrays SSL
 * @run main/othervm Arrays TLS
 * @run main/othervm Arrays SSLv3
 * @run main/othervm Arrays TLSv1
 * @run main/othervm Arrays TLSv1.1
 * @run main/othervm Arrays TLSv1.2
 * @run main/othervm Arrays TLSv1.3
 * @run main/othervm -Djdk.tls.acknowledgeCloseNotify=true Arrays TLSv1.3
 */

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.security.*;
import java.nio.*;

import jdk.test.lib.security.SecurityUtils;

public class Arrays {

    private static boolean debug = false;
    private static boolean acknowledgeCloseNotify =
        "true".equals(System.getProperty("jdk.tls.acknowledgeCloseNotify"));

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

    private ByteBuffer [] appOutArray1;
    private ByteBuffer [] appInArray1;

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

        while (!isEngineClosed(ssle1) || !isEngineClosed(ssle2)) {

            log("================");

            result1 = ssle1.wrap(appOutArray1, oneToTwo);
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

            result1 = ssle1.unwrap(twoToOne, appInArray1);
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
            if (!dataDone) {
                boolean done = true;

                for (int i = 0; i < appOutArray1.length; i++) {
                    if (appOutArray1[i].remaining() != 0) {
                        log("1st out not done");
                        done = false;
                    }
                }

                if (appOut2.remaining() != 0) {
                    log("2nd out not done");
                    done = false;
                }

                if (done) {
                    log("Closing ssle1's *OUTBOUND*...");
                    for (int i = 0; i < appOutArray1.length; i++) {
                        appOutArray1[i].rewind();
                    }
                    ssle1.closeOutbound();
                    String protocol = ssle2.getSession().getProtocol();
                    if (!acknowledgeCloseNotify) {
                        switch (ssle2.getSession().getProtocol()) {
                            case "SSLv3":
                            case "TLSv1":
                            case "TLSv1.1":
                            case "TLSv1.2":
                                break;
                            default:    // TLSv1.3
                                // TLS 1.3, half-close only.
                                ssle2.closeOutbound();
                        }
                    }
                    dataDone = true;
                }
            }
        }
        checkTransfer(appOutArray1,  appIn2);
        appInArray1[appInArray1.length - 1].limit(
            appInArray1[appInArray1.length - 1].position());
        checkTransfer(appInArray1, appOut2);
    }

    private static String contextVersion;
    public static void main(String args[]) throws Exception {
        contextVersion = args[0];
        // Re-enable context version if it is disabled.
        // If context version is SSLv3, TLSv1 needs to be re-enabled.
        if (contextVersion.equals("SSLv3")) {
            SecurityUtils.removeFromDisabledTlsAlgs("TLSv1");
        } else if (contextVersion.equals("TLSv1") ||
                   contextVersion.equals("TLSv1.1")) {
            SecurityUtils.removeFromDisabledTlsAlgs(contextVersion);
        }

        Arrays test;

        test = new Arrays();

        test.createSSLEngines();

        test.runTest();

        System.err.println("Test Passed.");
    }

    /*
     * **********************************************************
     * Majority of the test case is above, below is just setup stuff
     * **********************************************************
     */

    public Arrays() throws Exception {
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

        SSLContext sslCtx = SSLContext.getInstance(contextVersion);

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslCtx;
    }

    private void createBuffers() {
        // Size the buffers as appropriate.

        SSLSession session = ssle1.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        appIn2 = ByteBuffer.allocateDirect(appBufferMax + 50);

        oneToTwo = ByteBuffer.allocateDirect(netBufferMax);
        twoToOne = ByteBuffer.allocateDirect(netBufferMax);

        ByteBuffer strBB = ByteBuffer.wrap(
            "Hi Engine2, I'm SSLEngine1, So Be it" .getBytes());

        strBB.position(0);
        strBB.limit(5);
        ByteBuffer appOut1a = strBB.slice();

        strBB.position(5);
        strBB.limit(15);
        ByteBuffer appOut1b = strBB.slice();

        strBB.position(15);
        strBB.limit(strBB.capacity());
        ByteBuffer appOut1c = strBB.slice();

        strBB.rewind();

        appOutArray1 = new ByteBuffer [] { appOut1a, appOut1b, appOut1c };

        appOut2 = ByteBuffer.wrap("Hello Engine1, I'm SSLEngine2".getBytes());

        ByteBuffer appIn1a = ByteBuffer.allocateDirect(5);
        ByteBuffer appIn1b = ByteBuffer.allocateDirect(10);
        ByteBuffer appIn1c = ByteBuffer.allocateDirect(appBufferMax + 50);
        appInArray1 = new ByteBuffer [] { appIn1a, appIn1b, appIn1c };

        log("AppOut1a = " + appOut1a);
        log("AppOut1a = " + appOut1b);
        log("AppOut1a = " + appOut1c);
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

    private static void checkTransfer(ByteBuffer [] a, ByteBuffer b)
            throws Exception {

        b.flip();

        for (int i = 0; i < a.length; i++) {
            a[i].rewind();

            b.limit(b.position() + a[i].remaining());

            if (!a[i].equals(b)) {
                throw new Exception("Data didn't transfer cleanly");
            }

            b.position(b.limit());
        }

        log("Data transferred cleanly");
    }

    private static void log(String str) {
        if (debug) {
            System.err.println(str);
        }
    }
}
