/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test SSLEngine.begineHandshake() triggers a KeyUpdate handshake
 * in TLSv1.3
 * @run main/othervm TLS13BeginHandshake
 */

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;

public class TLS13BeginHandshake {
    static String pathToStores =
            System.getProperty("test.src") + "/../../../../javax/net/ssl/etc/";
    static String keyStoreFile = "keystore";
    static String passwd = "passphrase";

    private SSLEngine serverEngine, clientEngine;
    SSLEngineResult clientResult, serverResult;
    private ByteBuffer clientOut, clientIn;
    private ByteBuffer serverOut, serverIn;
    private ByteBuffer cTOs,sTOc;

    public static void main(String args[]) throws Exception{
        new TLS13BeginHandshake().runDemo();
    }

    private void runDemo() throws Exception {
        int done = 0;

        createSSLEngines();
        createBuffers();

        while (!isEngineClosed(clientEngine) || !isEngineClosed(serverEngine)) {

            System.out.println("================");
            clientResult = clientEngine.wrap(clientOut, cTOs);
            System.out.println("client wrap: " + clientResult);
            runDelegatedTasks(clientResult, clientEngine);
            serverResult = serverEngine.wrap(serverOut, sTOc);
            System.out.println("server wrap: " + serverResult);
            runDelegatedTasks(serverResult, serverEngine);

            cTOs.flip();
            sTOc.flip();

            System.out.println("----");
            clientResult = clientEngine.unwrap(sTOc, clientIn);
            System.out.println("client unwrap: " + clientResult);
            if (clientResult.getStatus() == SSLEngineResult.Status.CLOSED) {
                break;
            }            runDelegatedTasks(clientResult, clientEngine);
            serverResult = serverEngine.unwrap(cTOs, serverIn);
            System.out.println("server unwrap: " + serverResult);
            runDelegatedTasks(serverResult, serverEngine);

            cTOs.compact();
            sTOc.compact();

            //System.err.println("so limit="+serverOut.limit()+" so pos="+serverOut.position());
            //System.out.println("bf ctos limit="+cTOs.limit()+" pos="+cTOs.position()+" cap="+cTOs.capacity());
            //System.out.println("bf stoc limit="+sTOc.limit()+" pos="+sTOc.position()+" cap="+sTOc.capacity());
            if (done < 2  && (clientOut.limit() == serverIn.position()) &&
                    (serverOut.limit() == clientIn.position())) {

                if (done == 0) {
                    checkTransfer(serverOut, clientIn);
                    checkTransfer(clientOut, serverIn);
                    clientEngine.beginHandshake();
                    done++;
                    continue;
                }

                checkTransfer(serverOut, clientIn);
                checkTransfer(clientOut, serverIn);
                System.out.println("\tClosing...");
                clientEngine.closeOutbound();
                serverEngine.closeOutbound();
                done++;
                continue;
            }
        }
    }

    private static boolean isEngineClosed(SSLEngine engine) {
        if (engine.isInboundDone())
            System.out.println("inbound closed");
        if (engine.isOutboundDone())
            System.out.println("outbound closed");
        return (engine.isOutboundDone() && engine.isInboundDone());
    }

    private static void checkTransfer(ByteBuffer a, ByteBuffer b)
            throws Exception {
        a.flip();
        b.flip();

        if (!a.equals(b)) {
            throw new Exception("Data didn't transfer cleanly");
        } else {
            System.out.println("\tData transferred cleanly");
        }

        a.compact();
        b.compact();

    }
    private void createBuffers() {
        SSLSession session = clientEngine.getSession();
        int appBufferMax = session.getApplicationBufferSize();
        int netBufferMax = session.getPacketBufferSize();

        clientIn = ByteBuffer.allocate(appBufferMax + 50);
        serverIn = ByteBuffer.allocate(appBufferMax + 50);

        cTOs = ByteBuffer.allocateDirect(netBufferMax);
        sTOc = ByteBuffer.allocateDirect(netBufferMax);

        clientOut = ByteBuffer.wrap("client".getBytes());
        serverOut = ByteBuffer.wrap("server".getBytes());
    }

    private void createSSLEngines() throws Exception {
        serverEngine = initContext().createSSLEngine();
        serverEngine.setUseClientMode(false);
        serverEngine.setNeedClientAuth(true);

        clientEngine = initContext().createSSLEngine("client", 80);
        clientEngine.setUseClientMode(true);
    }

    private SSLContext initContext() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLSv1.3");
        KeyStore ks = KeyStore.getInstance(new File(pathToStores + keyStoreFile),
                passwd.toCharArray());
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sc;
    }

    private static void runDelegatedTasks(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                runnable.run();
            }
            HandshakeStatus hsStatus = engine.getHandshakeStatus();
            if (hsStatus == HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
        }
    }
}
