/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6388456
 * @summary Need adjustable TLS max record size for interoperability
 *      with non-compliant stacks
 *
 * Helper class of SSL/TLS client/server communication.
 *
 * @author Xuelei Fan
 */

import javax.net.ssl.*;

import java.io.*;
import java.security.*;
import java.nio.*;
import java.nio.channels.*;

public class SSLEngineService {

    private static String keyStoreFile = "keystore";
    private static String trustStoreFile = "truststore";
    private static char[] passphrase = "passphrase".toCharArray();

    private String pathToStores;
    private String keyFilename;
    private String trustFilename;

    protected SSLEngineService() {
        init("../etc");
    }

    protected SSLEngineService(String pathToStores) {
        init(pathToStores);
    }

    private void init(String pathToStores) {
        this.pathToStores = pathToStores;
        this.keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
        this.trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;
    }

    // deliver local application data.
    protected static void deliver(SSLEngine ssle, SocketChannel sc)
        throws Exception {

        // create buufer.
        int appBufferMax = ssle.getSession().getApplicationBufferSize();
        int netBufferMax = ssle.getSession().getPacketBufferSize();
        int length = appBufferMax * (Integer.SIZE / 8);

        // allocate more in order to check large packet
        ByteBuffer localAppData = ByteBuffer.allocate(length);

        // allocate less in order to check BUFFER_OVERFLOW/BUFFER_UNDERFLOW
        ByteBuffer localNetData = ByteBuffer.allocate(netBufferMax/2);

        // prepare local application data
        localAppData.putInt(length);
        for (int i = 1; i < appBufferMax; i++) {
            localAppData.putInt(i);
        }
        localAppData.flip();


        while (localAppData.hasRemaining()) {
            // empty the local network packet buffer.
            localNetData.clear();

            // generated local network packet.
            SSLEngineResult res = ssle.wrap(localAppData, localNetData);

            // checking status
            switch (res.getStatus()) {

            case OK :
                localNetData.flip();

                // send the network packet
                while (localNetData.hasRemaining()) {
                    if (sc.write(localNetData) < 0) {
                        throw new IOException("Unable write to socket channel");
                    }
                }

                if (res.getHandshakeStatus() ==
                        SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    Runnable runnable;
                    while ((runnable = ssle.getDelegatedTask()) != null) {
                        runnable.run();
                    }
                }

                // detect large buffer
                if (res.bytesProduced() >= Short.MAX_VALUE) {
                    System.out.println("Generate a " +
                        res.bytesProduced() + " bytes large packet ");
                }
                break;

            case BUFFER_OVERFLOW :
                // maybe need to enlarge the local network packet buffer.
                int size = ssle.getSession().getPacketBufferSize();
                if (size > localNetData.capacity()) {
                    System.out.println("resize destination buffer upto " +
                                size + " bytes for BUFFER_OVERFLOW");
                    localNetData = enlargeBuffer(localNetData, size);
                }
                break;

            default : // BUFFER_UNDERFLOW or CLOSED :
                throw new IOException("Received invalid" + res.getStatus() +
                        "during transfer application data");
            }
        }
    }


    // receive peer application data.
    protected static void receive(SSLEngine ssle, SocketChannel sc)
        throws Exception {

        // create buufers.
        int appBufferMax = ssle.getSession().getApplicationBufferSize();
        int netBufferMax = ssle.getSession().getPacketBufferSize();

        // allocate less in order to check BUFFER_OVERFLOW/BUFFER_UNDERFLOW
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferMax/2);
        ByteBuffer peerNetData = ByteBuffer.allocate(netBufferMax/2);
        int received = -1;

        boolean needToReadMore = true;
        while (received != 0) {
            if (needToReadMore) {
                if (ssle.isInboundDone() || sc.read(peerNetData) < 0) {
                    break;
                }
            }

            peerNetData.flip();
            SSLEngineResult res = ssle.unwrap(peerNetData, peerAppData);
            peerNetData.compact();

            // checking status
            switch (res.getStatus()) {

            case OK :
                if (res.getHandshakeStatus() ==
                        SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    Runnable runnable;
                    while ((runnable = ssle.getDelegatedTask()) != null) {
                        runnable.run();
                    }
                }

                if (received < 0 && res.bytesProduced() < 4 ) {
                    break;
                }

                if (received < 0) {
                    received = peerAppData.getInt(0);
                }

                System.out.println("received " + peerAppData.position() +
                        " bytes client application data");
                System.out.println("\tcomsumed " + res.bytesConsumed() +
                        " byes network data");
                peerAppData.clear();

                received -= res.bytesProduced();

                // detect large buffer
                if (res.bytesConsumed() >= Short.MAX_VALUE) {
                    System.out.println("Consumes a " + res.bytesConsumed() +
                        " bytes large packet ");
                }

                needToReadMore = (peerNetData.position() > 0) ? false : true;

                break;

            case BUFFER_OVERFLOW :
                // maybe need to enlarge the peer application data buffer.
                int size = ssle.getSession().getApplicationBufferSize();
                if (size > peerAppData.capacity()) {
                    System.out.println("resize destination buffer upto " +
                        size + " bytes for BUFFER_OVERFLOW");
                    peerAppData = enlargeBuffer(peerAppData, size);
                }
                break;

            case BUFFER_UNDERFLOW :
                // maybe need to enlarge the peer network packet data buffer.
                size = ssle.getSession().getPacketBufferSize();
                if (size > peerNetData.capacity()) {
                    System.out.println("resize source buffer upto " + size +
                        " bytes for BUFFER_UNDERFLOW");
                    peerNetData = enlargeBuffer(peerNetData, size);
                }

                needToReadMore = true;
                break;

            default : // CLOSED :
                throw new IOException("Received invalid" + res.getStatus() +
                        "during transfer application data");
            }
        }
    }

    protected static void handshaking(SSLEngine ssle, SocketChannel sc,
            ByteBuffer additional) throws Exception {

        int appBufferMax = ssle.getSession().getApplicationBufferSize();
        int netBufferMax = ssle.getSession().getPacketBufferSize();

        // allocate less in order to check BUFFER_OVERFLOW/BUFFER_UNDERFLOW
        ByteBuffer localAppData = ByteBuffer.allocate(appBufferMax/10);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferMax/10);
        ByteBuffer localNetData = ByteBuffer.allocate(netBufferMax/10);
        ByteBuffer peerNetData = ByteBuffer.allocate(netBufferMax/10);

        // begin handshake
        ssle.beginHandshake();
        SSLEngineResult.HandshakeStatus hs = ssle.getHandshakeStatus();

        // start handshaking from unwrap
        byte[] buffer = new byte[0xFF];
        boolean underflow = false;
        do {
            switch (hs) {

            case NEED_UNWRAP :
                if (peerNetData.position() == 0) {
                    if (additional != null && additional.hasRemaining()) {
                        do {
                            int len = Math.min(buffer.length,
                                                peerNetData.remaining());
                            len = Math.min(len, additional.remaining());
                            if (len != 0) {
                                additional.get(buffer, 0, len);
                                peerNetData.put(buffer, 0, len);
                            }
                        } while (peerNetData.remaining() > 0 &&
                                    additional.hasRemaining());
                    } else {
                        if (sc.read(peerNetData) < 0) {
                            ssle.closeInbound();
                            return;
                        }
                    }
                }

                if (underflow) {
                    if (sc.read(peerNetData) < 0) {
                        ssle.closeInbound();
                        return;
                    }

                    underflow = false;
                }

                peerNetData.flip();
                SSLEngineResult res = ssle.unwrap(peerNetData, peerAppData);
                peerNetData.compact();
                hs = res.getHandshakeStatus();

                switch (res.getStatus()) {
                case OK :
                    break;
                case BUFFER_UNDERFLOW :
                    // maybe need to enlarge the peer network packet buffer.
                    int size = ssle.getSession().getPacketBufferSize();
                    if (size > peerNetData.capacity()) {
                        System.out.println("resize source buffer upto " +
                                size + " bytes for BUFFER_UNDERFLOW");
                        peerNetData = enlargeBuffer(peerNetData, size);
                    }

                    underflow = true;
                    break;
                case BUFFER_OVERFLOW :
                    // maybe need to enlarge the peer application data buffer.
                    size = ssle.getSession().getApplicationBufferSize();
                    if (size > peerAppData.capacity()) {
                        System.out.println("resize destination buffer upto " +
                                size + " bytes for BUFFER_OVERFLOW");
                        peerAppData = enlargeBuffer(peerAppData, size);
                    }
                    break;
                default : //CLOSED
                    throw new IOException("Received invalid" + res.getStatus() +
                        "during initial handshaking");
                }
                break;

            case NEED_WRAP :
                // empty the local network packet buffer.
                localNetData.clear();

                // generated local network packet.
                res = ssle.wrap(localAppData, localNetData);
                hs = res.getHandshakeStatus();

                // checking status
                switch (res.getStatus()) {
                case OK :
                    localNetData.flip();

                    // send the network packet
                    while (localNetData.hasRemaining()) {
                        if (sc.write(localNetData) < 0) {
                            throw new IOException(
                                "Unable write to socket channel");
                        }
                    }
                    break;

                case BUFFER_OVERFLOW :
                    // maybe need to enlarge the local network packet buffer.
                    int size = ssle.getSession().getPacketBufferSize();
                    if (size > localNetData.capacity()) {
                        System.out.println("resize destination buffer upto " +
                                size + " bytes for BUFFER_OVERFLOW");
                        localNetData = enlargeBuffer(localNetData, size);
                    }
                    break;

                default : // BUFFER_UNDERFLOW or CLOSED :
                    throw new IOException("Received invalid" + res.getStatus() +
                        "during initial handshaking");
                }
                break;

            case NEED_TASK :
                Runnable runnable;
                while ((runnable = ssle.getDelegatedTask()) != null) {
                    runnable.run();
                }
                hs = ssle.getHandshakeStatus();
                break;

            default : // FINISHED or NOT_HANDSHAKING
                // do nothing
            }
        } while (hs != SSLEngineResult.HandshakeStatus.FINISHED &&
                hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
    }

    private static ByteBuffer enlargeBuffer(ByteBuffer buffer, int size) {
        ByteBuffer bb = ByteBuffer.allocate(size);
        buffer.flip();
        bb.put(buffer);

        return bb;
    }

    /*
     * Create an initialized SSLContext to use for this test.
     */
    protected SSLEngine createSSLEngine(boolean mode) throws Exception {

        SSLEngine ssle;

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(new FileInputStream(keyFilename), passphrase);
        ts.load(new FileInputStream(trustFilename), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        ssle = sslCtx.createSSLEngine();
        ssle.setUseClientMode(mode);

        return ssle;
    }
}
