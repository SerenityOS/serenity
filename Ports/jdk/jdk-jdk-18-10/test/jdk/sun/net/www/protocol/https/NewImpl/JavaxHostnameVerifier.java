/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.

/*
 * @test
 * @bug 4474255 4484246
 * @summary When an application enables anonymous SSL cipher suite,
 *        Hostname verification is not required
 * @run main/othervm JavaxHostnameVerifier
 */

import java.io.*;
import java.net.*;
import java.security.Security;
import java.security.cert.*;
import javax.net.ssl.*;

/**
 * Use javax.net.ssl.HostnameVerifier
 */
public class JavaxHostnameVerifier {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * If the client or server is doing some kind of object creation
     * that the other side depends on, and that thread prematurely
     * exits, you may experience a hang.  The test harness will
     * terminate all hung threads after its timeout has expired,
     * currently 3 minutes by default, but you might try to be
     * smart about it....
     */

    /**
     * Returns the path to the file obtained from
     * parsing the HTML header.
     */
    private static String getPath(DataInputStream in)
        throws IOException
    {
        String line = in.readLine();
        String path = "";
        // extract class from GET line
        if (line == null)
                return null;

        if (line.startsWith("GET /")) {
            line = line.substring(5, line.length()-1).trim();
            int index = line.indexOf(' ');
            if (index != -1) {
                path = line.substring(0, index);
            }
        }

        // eat the rest of header
        do {
            line = in.readLine();
        } while ((line.length() != 0) &&
                 (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));

        if (path.length() != 0) {
            return path;
        } else {
            throw new IOException("Malformed Header");
        }
    }

    /**
     * Returns an array of bytes containing the bytes for
     * the file represented by the argument <b>path</b>.
     *
     * In our case, we just pretend to send something back.
     *
     * @return the bytes for the file
     * @exception FileNotFoundException if the file corresponding
     * to <b>path</b> could not be loaded.
     */
    private byte[] getBytes(String path)
        throws IOException
    {
        return "Hello world, I am here".getBytes();
    }

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {

        SSLServerSocketFactory sslssf =
          (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        // doClientSide() connects to "localhost"
        InetAddress localHost = InetAddress.getByName("localhost");
        InetSocketAddress address = new InetSocketAddress(localHost, serverPort);

        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket();
        sslServerSocket.bind(address);
        serverPort = sslServerSocket.getLocalPort();

        String ciphers[]= { "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA" };
        sslServerSocket.setEnabledCipherSuites(ciphers);

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;

        SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
        DataOutputStream out =
                new DataOutputStream(sslSocket.getOutputStream());

        try {
             // get path to class file from header
             DataInputStream in =
                        new DataInputStream(sslSocket.getInputStream());
             String path = getPath(in);
             // retrieve bytecodes
             byte[] bytecodes = getBytes(path);
             // send bytecodes in response (assumes HTTP/1.0 or later)
             try {
                out.writeBytes("HTTP/1.0 200 OK\r\n");
                out.writeBytes("Content-Length: " + bytecodes.length + "\r\n");
                out.writeBytes("Content-Type: text/html\r\n\r\n");
                out.write(bytecodes);
                out.flush();
             } catch (IOException ie) {
                ie.printStackTrace();
                return;
             }

        } catch (Exception e) {
             e.printStackTrace();
             // write out error response
             out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
             out.writeBytes("Content-Type: text/html\r\n\r\n");
             out.flush();
        } finally {
             // close the socket
             System.out.println("Server closing socket");
             sslSocket.close();
             serverReady = false;
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        System.setProperty("https.cipherSuites",
                "SSL_DH_anon_WITH_3DES_EDE_CBC_SHA");

        // use the default hostname verifier

        URL url = new URL("https://" + "localhost:" + serverPort +
                                "/etc/hosts");
        URLConnection urlc = url.openConnection(Proxy.NO_PROXY);

        if (!(urlc instanceof javax.net.ssl.HttpsURLConnection)) {
            throw new Exception(
                "URLConnection ! instanceof javax.net.ssl.HttpsURLConnection");
        }

        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                               urlc.getInputStream()));
            String inputLine;
            System.out.print("Client reading... ");
            while ((inputLine = in.readLine()) != null)
                System.out.println(inputLine);
            System.out.println("Cipher Suite: " +
                ((HttpsURLConnection)urlc).getCipherSuite());
            in.close();
        } catch (SSLException e) {
            if (in != null)
                in.close();
            throw e;
        }
        System.out.println("Client reports:  SUCCESS");
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String[] args) throws Exception {
        // re-enable 3DES
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new JavaxHostnameVerifier();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    JavaxHostnameVerifier() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null) {
            System.out.print("Server Exception:");
            throw serverException;
        }
        if (clientException != null) {
            System.out.print("Client Exception:");
            throw clientException;
        }
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }
}
