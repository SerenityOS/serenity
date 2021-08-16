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

/*
 * @test
 * @bug 8164879
 * @library ../../
 * @library /test/lib
 * @modules java.base/sun.security.util
 * @summary Verify AES/GCM's limits set in the jdk.tls.keyLimits property
 * @run main SSLSocketKeyLimit 0 server AES/GCM/NoPadding keyupdate 1000000
 * @run main SSLSocketKeyLimit 0 client AES/GCM/NoPadding keyupdate 1000000
 * @run main SSLSocketKeyLimit 1 client AES/GCM/NoPadding keyupdate 2^22
 */

 /**
  * Verify AES/GCM's limits set in the jdk.tls.keyLimits property
  * start a new handshake sequence to renegotiate the symmetric key with an
  * SSLSocket connection.  This test verifies the handshake method was called
  * via debugging info.  It does not verify the renegotiation was successful
  * as that is very hard.
  */

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;
import jdk.test.lib.hexdump.HexPrinter;

public class SSLSocketKeyLimit {
    SSLSocket socket;
    private InputStream in;
    private OutputStream out;

    static boolean serverReady = false;
    static int serverPort = 0;

    static String pathToStores = "../../../../javax/net/ssl/etc/";
    static String keyStoreFile = "keystore";
    static String passwd = "passphrase";
    static int dataLen = 10240;
    static byte[] data  = new byte[dataLen];
    static boolean serverwrite = true;
    int totalDataLen = 0;
    static boolean done = false;

    SSLSocketKeyLimit() {
    }

    SSLContext initContext() throws Exception {
        SSLContext sc = SSLContext.getInstance("TLSv1.3");
        KeyStore ks = KeyStore.getInstance(
                new File(System.getProperty("javax.net.ssl.keyStore")),
                passwd.toCharArray());
        KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwd.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sc;
    }

    /**
     * args should have two values:  server|client, <limit size>
     * Prepending 'p' is for internal use only.
     */
    public static void main(String args[]) throws Exception {
        if (args[0].compareTo("p") != 0) {

            boolean expectedFail = (Integer.parseInt(args[0]) == 1);
            if (expectedFail) {
                System.out.println("Test expected to not find updated msg");
            }
            //Write security property file to overwrite default
            File f = new File("keyusage."+ System.nanoTime());
            PrintWriter p = new PrintWriter(f);
            p.write("jdk.tls.keyLimits=");
            for (int i = 2; i < args.length; i++) {
                p.write(" "+ args[i]);
            }
            p.close();
            System.out.println("Keyusage path = " + f.getAbsolutePath());
            System.setProperty("test.java.opts",
                    "-Dtest.src=" + System.getProperty("test.src") +
                            " -Dtest.jdk=" + System.getProperty("test.jdk") +
                            " -Djavax.net.debug=ssl,handshake" +
                            " -Djava.security.properties=" + f.getName());

            System.out.println("test.java.opts: " +
                    System.getProperty("test.java.opts"));

            ProcessBuilder pb = ProcessTools.createTestJvm(
                    Utils.addTestJavaOpts("SSLSocketKeyLimit", "p", args[1]));

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

        if (args.length > 0 && args[0].compareToIgnoreCase("client") == 0) {
            serverwrite = false;
        }

        String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);

        Arrays.fill(data, (byte)0x0A);
        Thread ts = new Thread(new Server());

        ts.start();
        while (!serverReady) {
            Thread.sleep(100);
        }
        new Client().run();
        ts.join(10000);  // 10sec
        System.exit(0);
    }

    void write(SSLSocket s) throws Exception {
        int i = 0;
        in = s.getInputStream();
        out = s.getOutputStream();
        while (i++ < 150) {
            out.write(data, 0, dataLen);
            System.out.print("W");
            in.readNBytes(1);
            System.out.print("R");
        }
        out.write(0x0D);
        out.flush();

        // Let read side all the data
        while (!done) {
            Thread.sleep(100);
        }
        out.close();
        in.close();
    }


    void read(SSLSocket s) throws Exception {
        byte[] buf = new byte[dataLen];
        int len;
        byte i = 0;
        try {
            System.out.println("Server: connected " + s.getSession().getCipherSuite());
            in = s.getInputStream();
            out = s.getOutputStream();
            while (true) {
                len = in.read(buf, 0, dataLen);
                System.out.print("r");
                out.write(i++);
                System.out.print("w");
                for (byte b: buf) {
                    if (b == 0x0A || b == 0x0D) {
                        continue;
                    }
                    System.out.println("\nData invalid: " + HexPrinter.minimal().toString(buf));
                    break;
                }

                if (len > 0 && buf[len-1] == 0x0D) {
                    System.out.println("got end byte");
                    break;
                }
                totalDataLen += len;
            }
        } catch (Exception e) {
            System.out.println("\n"  + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tell write side that we are done reading
            out.close();
            in.close();
            done = true;
        }
        System.out.println("\nTotalDataLen = " + totalDataLen);
    }

    static class Server extends SSLSocketKeyLimit implements Runnable {
        private SSLServerSocketFactory ssf;
        private SSLServerSocket ss;
        Server() {
            super();
            try {
                ssf = initContext().getServerSocketFactory();
                ss = (SSLServerSocket) ssf.createServerSocket(serverPort);
                serverPort = ss.getLocalPort();
            } catch (Exception e) {
                System.out.println("server: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                serverReady = true;
                System.out.println("Server waiting... port: " + serverPort);
                socket = (SSLSocket) ss.accept();
                if (serverwrite) {
                    write(socket);
                } else {
                    read(socket);
                }

                socket.close();
            } catch (Exception e) {
                System.out.println("server: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Server closed");
        }
    }


    static class Client extends SSLSocketKeyLimit implements Runnable {
        private SSLSocketFactory sf;

        Client() {
            super();
        }

        public void run() {
            try {
                sf = initContext().getSocketFactory();
                System.out.println("Client: connecting... port: " + serverPort);
                socket = (SSLSocket)sf.createSocket("localhost", serverPort);
                System.out.println("Client: connected." + socket.getSession().getCipherSuite());

                // Opposite of what the server does
                if (!serverwrite) {
                    write(socket);
                } else {
                    read(socket);
                }

            } catch (Exception e) {
                System.err.println("client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
