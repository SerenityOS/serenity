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
 * @bug 8164879
 * @library ../../
 * @library /test/lib
 * @summary Verify AES/GCM's limits set in the jdk.tls.keyLimits property
 * start a new handshake sequence to renegotiate the symmetric key with an
 * SSLSocket connection.  This test verifies the handshake method was called
 * via debugging info.  It does not verify the renegotiation was successful
 * as that is very hard.
 *
 * @run main SSLEngineKeyLimit 0 server AES/GCM/NoPadding keyupdate 1050000
 * @run main SSLEngineKeyLimit 1 client AES/GCM/NoPadding keyupdate 2^22
 */

/*
 * This test runs in another process so we can monitor the debug
 * results.  The OutputAnalyzer must see correct debug output to return a
 * success.
 */

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Utils;

public class SSLEngineKeyLimit {

    SSLEngine eng;
    static ByteBuffer cTos;
    static ByteBuffer sToc;
    static ByteBuffer outdata;
    ByteBuffer buf;
    static boolean ready = false;

    static String pathToStores = "../../../../javax/net/ssl/etc/";
    static String keyStoreFile = "keystore";
    static String passwd = "passphrase";
    static String keyFilename;
    static int dataLen = 10240;
    static boolean serverwrite = true;
    int totalDataLen = 0;
    static boolean sc = true;
    int delay = 1;
    static boolean readdone = false;

    // Turn on debugging
    static boolean debug = false;

    SSLEngineKeyLimit() {
        buf = ByteBuffer.allocate(dataLen*4);
    }

    /**
     * args should have two values:  server|client, <limit size>
     * Prepending 'p' is for internal use only.
     */
    public static void main(String args[]) throws Exception {

        for (int i = 0; i < args.length; i++) {
            System.out.print(" " + args[i]);
        }
        System.out.println();
        if (args[0].compareTo("p") != 0) {
            boolean expectedFail = (Integer.parseInt(args[0]) == 1);
            if (expectedFail) {
                System.out.println("Test expected to not find updated msg");
            }

            // Write security property file to overwrite default
            File f = new File("keyusage."+ System.nanoTime());
            PrintWriter p = new PrintWriter(f);
            p.write("jdk.tls.keyLimits=");
            for (int i = 2; i < args.length; i++) {
                p.write(" "+ args[i]);
            }
            p.close();

            System.setProperty("test.java.opts",
                    "-Dtest.src=" + System.getProperty("test.src") +
                            " -Dtest.jdk=" + System.getProperty("test.jdk") +
                            " -Djavax.net.debug=ssl,handshake" +
                            " -Djava.security.properties=" + f.getName());

            System.out.println("test.java.opts: " +
                    System.getProperty("test.java.opts"));

            ProcessBuilder pb = ProcessTools.createTestJvm(
                    Utils.addTestJavaOpts("SSLEngineKeyLimit", "p", args[1]));

            OutputAnalyzer output = ProcessTools.executeProcess(pb);
            try {
                if (expectedFail) {
                    output.shouldNotContain("KeyUpdate: write key updated");
                    output.shouldNotContain("KeyUpdate: read key updated");
                } else {
                    output.shouldContain("trigger key update");
                    output.shouldContain("KeyUpdate: write key updated");
                    output.shouldContain("KeyUpdate: read key updated");
                }
            } catch (Exception e) {
                throw e;
            } finally {
                System.out.println("-- BEGIN Stdout:");
                System.out.println(output.getStdout());
                System.out.println("-- END Stdout");
                System.out.println("-- BEGIN Stderr:");
                System.out.println(output.getStderr());
                System.out.println("-- END Stderr");
            }
            return;
        }

        if (args[0].compareTo("p") != 0) {
            throw new Exception ("Tried to run outside of a spawned process");
        }

        if (args[1].compareTo("client") == 0) {
            serverwrite = false;
        }

        cTos = ByteBuffer.allocateDirect(dataLen*4);
        keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);

        sToc = ByteBuffer.allocateDirect(dataLen*4);
        outdata = ByteBuffer.allocateDirect(dataLen);

        byte[] data  = new byte[dataLen];
        Arrays.fill(data, (byte)0x0A);
        outdata.put(data);
        outdata.flip();
        cTos.clear();
        sToc.clear();

        Thread ts = new Thread(serverwrite ? new Client() : new Server());
        ts.start();
        (serverwrite ? new Server() : new Client()).run();
        ts.interrupt();
        ts.join();
    }

    private static void doTask(SSLEngineResult result,
            SSLEngine engine) throws Exception {

        if (result.getHandshakeStatus() ==
                SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                print("\trunning delegated task...");
                runnable.run();
            }
            SSLEngineResult.HandshakeStatus hsStatus =
                    engine.getHandshakeStatus();
            if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new Exception(
                    "handshake shouldn't need additional tasks");
            }
            print("\tnew HandshakeStatus: " + hsStatus);
        }
    }

    static void print(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    static void log(String s, SSLEngineResult r) {
        if (!debug) {
            return;
        }
        System.out.println(s + ": " +
                r.getStatus() + "/" + r.getHandshakeStatus()+ " " +
                r.bytesConsumed() + "/" + r.bytesProduced() + " ");

    }

    void write() throws Exception {
        int i = 0;
        SSLEngineResult r;
        boolean again = true;

        while (!ready) {
            Thread.sleep(delay);
        }
        print("Write-side. ");

        while (i++ < 150) {
            while (sc) {
                if (readdone) {
                    return;
                }
                Thread.sleep(delay);
            }

            outdata.rewind();
            print("write wrap");

            while (true) {
                r = eng.wrap(outdata, getWriteBuf());
                log("write wrap", r);
                if (debug && r.getStatus() != SSLEngineResult.Status.OK) {
                    print("outdata pos: " + outdata.position() +
                            " rem: " + outdata.remaining() +
                            " lim: " + outdata.limit() +
                            " cap: " + outdata.capacity());
                    print("writebuf pos: " + getWriteBuf().position() +
                            " rem: " + getWriteBuf().remaining() +
                            " lim: " + getWriteBuf().limit() +
                            " cap: " + getWriteBuf().capacity());
                }
                if (again && r.getStatus() == SSLEngineResult.Status.OK &&
                        r.getHandshakeStatus() ==
                                SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    print("again");
                    again = false;
                    continue;
                }
                break;
            }
            doTask(r, eng);
            getWriteBuf().flip();
            sc = true;
            while (sc) {
                if (readdone) {
                    return;
                }
                Thread.sleep(delay);
            }

            while (true) {
                buf.clear();
                r = eng.unwrap(getReadBuf(), buf);
                log("write unwrap", r);
                if (debug && r.getStatus() != SSLEngineResult.Status.OK) {
                    print("buf pos: " + buf.position() +
                            " rem: " + buf.remaining() +
                            " lim: " + buf.limit() +
                            " cap: " + buf.capacity());
                    print("readbuf pos: " + getReadBuf().position() +
                            " rem: " + getReadBuf().remaining() +
                            " lim: " + getReadBuf().limit() +
                            " cap:"  + getReadBuf().capacity());
                }
                break;
            }
            doTask(r, eng);
            getReadBuf().compact();
            print("compacted readbuf pos: " + getReadBuf().position() +
                    " rem: " + getReadBuf().remaining() +
                    " lim: " + getReadBuf().limit() +
                    " cap: " + getReadBuf().capacity());
            sc = true;
        }
    }

    void read() throws Exception {
        byte b = 0x0B;
        ByteBuffer buf2 = ByteBuffer.allocateDirect(dataLen);
        SSLEngineResult r = null;
        boolean exit, again = true;

        while (eng == null) {
            Thread.sleep(delay);
        }

        try {
            System.out.println("connected");
            print("entering read loop");
            ready = true;
            while (true) {

                while (!sc) {
                    Thread.sleep(delay);
                }

                print("read wrap");
                exit = false;
                while (!exit) {
                    buf2.put(b);
                    buf2.flip();
                    r = eng.wrap(buf2, getWriteBuf());
                    log("read wrap", r);
                    if (debug) {
                             // && r.getStatus() != SSLEngineResult.Status.OK) {
                        print("buf2 pos: " + buf2.position() +
                                " rem: " + buf2.remaining() +
                                " cap: " + buf2.capacity());
                        print("writebuf pos: " + getWriteBuf().position() +
                                " rem: " + getWriteBuf().remaining() +
                                " cap: " + getWriteBuf().capacity());
                    }
                    if (again && r.getStatus() == SSLEngineResult.Status.OK &&
                            r.getHandshakeStatus() ==
                                SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                        buf2.compact();
                        again = false;
                        continue;
                    }
                    exit = true;
                }
                doTask(r, eng);
                buf2.clear();
                getWriteBuf().flip();

                sc = false;

                while (!sc) {
                    Thread.sleep(delay);
                }

                while (true) {
                        buf.clear();
                        r = eng.unwrap(getReadBuf(), buf);
                        log("read unwrap", r);
                        if (debug &&
                                r.getStatus() != SSLEngineResult.Status.OK) {
                            print("buf pos " + buf.position() +
                                    " rem: " + buf.remaining() +
                                    " lim: " + buf.limit() +
                                    " cap: " + buf.capacity());
                            print("readbuf pos: " + getReadBuf().position() +
                                    " rem: " + getReadBuf().remaining() +
                                    " lim: " + getReadBuf().limit() +
                                    " cap: " + getReadBuf().capacity());
                            doTask(r, eng);
                        }

                    if (again && r.getStatus() == SSLEngineResult.Status.OK &&
                            r.getHandshakeStatus() ==
                                SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                        buf.clear();
                        print("again");
                        again = false;
                        continue;

                    }
                    break;
                }
                buf.clear();
                getReadBuf().compact();

                totalDataLen += r.bytesProduced();
                sc = false;
            }
        } catch (Exception e) {
            sc = false;
            readdone = true;
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("Total data read = " + totalDataLen);
        }
    }

    ByteBuffer getReadBuf() {
        return null;
    }

    ByteBuffer getWriteBuf() {
        return null;
    }


    SSLContext initContext() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLSv1.3");
        KeyStore ks = KeyStore.getInstance(
                new File(System.getProperty("javax.net.ssl.keyStore")),
                passwd.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        sc.init(kmf.getKeyManagers(),
                tmf.getTrustManagers(), new SecureRandom());
        return sc;
    }

    static class Server extends SSLEngineKeyLimit implements Runnable {
        Server() throws Exception {
            super();
            eng = initContext().createSSLEngine();
            eng.setUseClientMode(false);
            eng.setNeedClientAuth(true);
        }

        public void run() {
            try {
                if (serverwrite) {
                    write();
                } else {
                    read();
                }

            } catch (Exception e) {
                System.out.println("server: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Server closed");
        }

        @Override
        ByteBuffer getWriteBuf() {
            return sToc;
        }
        @Override
        ByteBuffer getReadBuf() {
            return cTos;
        }
    }


    static class Client extends SSLEngineKeyLimit implements Runnable {
        Client() throws Exception {
            super();
            eng = initContext().createSSLEngine();
            eng.setUseClientMode(true);
        }

        public void run() {
            try {
                if (!serverwrite) {
                    write();
                } else {
                    read();
                }
            } catch (Exception e) {
                System.out.println("client: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Client closed");
        }
        @Override
        ByteBuffer getWriteBuf() {
            return cTos;
        }
        @Override
        ByteBuffer getReadBuf() {
            return sToc;
        }
    }
}
