/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7068321
 * @summary Support TLS Server Name Indication (SNI) Extension in JSSE Server
 * @library ../SSLEngine ../templates
 * @build SSLEngineService SSLCapabilities SSLExplorer
 * @run main/othervm SSLEngineExplorerMatchedSNI www.example.com
 *     www\.example\.com
 * @run main/othervm SSLEngineExplorerMatchedSNI www.example.com
 *     www\.example\.(com|org)
 * @run main/othervm SSLEngineExplorerMatchedSNI example.com
 *     (.*\.)*example\.(com|org)
 * @run main/othervm SSLEngineExplorerMatchedSNI www.example.com
 *     (.*\.)*example\.(com|org)
 * @run main/othervm SSLEngineExplorerMatchedSNI www.us.example.com
 *     (.*\.)*example\.(com|org)
 */

import javax.net.ssl.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.nio.channels.*;

public class SSLEngineExplorerMatchedSNI extends SSLEngineService {

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
    static boolean separateServerThread = false;

    // Is the server ready to serve?
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = true;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {

        // create SSLEngine.
        SSLEngine ssle = createSSLEngine(false);

        // Create a server socket channel.
        InetSocketAddress isa =
                new InetSocketAddress(InetAddress.getLocalHost(), serverPort);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(isa);
        serverPort = ssc.socket().getLocalPort();

        // Signal Client, we're ready for his connect.
        serverReady = true;

        // Accept a socket channel.
        SocketChannel sc = ssc.accept();

        // Complete connection.
        while (!sc.finishConnect()) {
            Thread.sleep(50);
            // waiting for the connection completed.
        }

        ByteBuffer buffer = ByteBuffer.allocate(0xFF);
        int position = 0;
        SSLCapabilities capabilities = null;

        // Read the header of TLS record
        buffer.limit(SSLExplorer.RECORD_HEADER_SIZE);
        while (position < SSLExplorer.RECORD_HEADER_SIZE) {
            int n = sc.read(buffer);
            if (n < 0) {
                throw new Exception("unexpected end of stream!");
            }
            position += n;
        }
        buffer.flip();

        int recordLength = SSLExplorer.getRequiredSize(buffer);
        if (buffer.capacity() < recordLength) {
            ByteBuffer oldBuffer = buffer;
            buffer = ByteBuffer.allocate(recordLength);
            buffer.put(oldBuffer);
        }

        buffer.position(SSLExplorer.RECORD_HEADER_SIZE);
        buffer.limit(buffer.capacity());
        while (position < recordLength) {
            int n = sc.read(buffer);
            if (n < 0) {
                throw new Exception("unexpected end of stream!");
            }
            position += n;
        }
        buffer.flip();

        capabilities = SSLExplorer.explore(buffer);
        if (capabilities != null) {
            System.out.println("Record version: " +
                    capabilities.getRecordVersion());
            System.out.println("Hello version: " +
                    capabilities.getHelloVersion());
        }

        // enable server name indication checking
        SNIMatcher matcher = SNIHostName.createSNIMatcher(
                                                serverAcceptableHostname);
        Collection<SNIMatcher> matchers = new ArrayList<>(1);
        matchers.add(matcher);
        SSLParameters params = ssle.getSSLParameters();
        params.setSNIMatchers(matchers);
        ssle.setSSLParameters(params);

        // handshaking
        handshaking(ssle, sc, buffer);

        // receive application data
        receive(ssle, sc);

        // send out application data
        deliver(ssle, sc);

        // check server name indication
        ExtendedSSLSession session = (ExtendedSSLSession)ssle.getSession();
        checkCapabilities(capabilities, session);

        // close the socket channel.
        sc.close();
        ssc.close();
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        // create SSLEngine.
        SSLEngine ssle = createSSLEngine(true);

        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        // Create a non-blocking socket channel.
        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        InetSocketAddress isa =
                new InetSocketAddress(InetAddress.getLocalHost(), serverPort);
        sc.connect(isa);

        // Complete connection.
        while (!sc.finishConnect() ) {
            Thread.sleep(50);
            // waiting for the connection completed.
        }

        SNIHostName serverName = new SNIHostName(clientRequestedHostname);
        List<SNIServerName> serverNames = new ArrayList<>(1);
        serverNames.add(serverName);
        SSLParameters params = ssle.getSSLParameters();
        params.setServerNames(serverNames);
        ssle.setSSLParameters(params);

        // handshaking
        handshaking(ssle, sc, null);

        // send out application data
        deliver(ssle, sc);

        // receive application data
        receive(ssle, sc);

        // check server name indication
        ExtendedSSLSession session = (ExtendedSSLSession)ssle.getSession();
        checkSNIInSession(session);

        // close the socket channel.
        sc.close();
    }

    void checkCapabilities(SSLCapabilities capabilities,
            ExtendedSSLSession session) throws Exception {
        List<SNIServerName> sessionSNI = session.getRequestedServerNames();
        if (!sessionSNI.equals(capabilities.getServerNames())) {
            for (SNIServerName sni : sessionSNI) {
                System.out.println("SNI in session is " + sni);
            }

            List<SNIServerName> capaSNI = capabilities.getServerNames();
            for (SNIServerName sni : capaSNI) {
                System.out.println("SNI in session is " + sni);
            }

            throw new Exception(
                    "server name indication does not match capabilities");
        }

        checkSNIInSession(session);
    }

    void checkSNIInSession(ExtendedSSLSession session) throws Exception {
        List<SNIServerName> sessionSNI = session.getRequestedServerNames();
        if (sessionSNI.isEmpty()) {
            throw new Exception(
                    "unexpected empty request server name indication");
        }

        if (sessionSNI.size() != 1) {
            throw new Exception(
                    "unexpected request server name indication");
        }

        SNIServerName serverName = sessionSNI.get(0);
        if (!(serverName instanceof SNIHostName)) {
            throw new Exception(
                    "unexpected instance of request server name indication");
        }

        String hostname = ((SNIHostName)serverName).getAsciiName();
        if (!clientRequestedHostname.equalsIgnoreCase(hostname)) {
            throw new Exception(
                    "unexpected request server name indication value");
        }
    }

    private static String clientRequestedHostname;
    private static String serverAcceptableHostname;

    private static void parseArguments(String[] args) {
        clientRequestedHostname = args[0];
        serverAcceptableHostname = args[1];
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */
    volatile Exception serverException = null;
    volatile Exception clientException = null;

    // use any free port by default
    volatile int serverPort = 0;

    public static void main(String args[]) throws Exception {
        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        new SSLEngineExplorerMatchedSNI();
    }

    Thread clientThread = null;
    Thread serverThread = null;

    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    SSLEngineExplorerMatchedSNI() throws Exception {
        super("../etc");

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
                        System.err.println(e);
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
