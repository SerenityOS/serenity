/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4696506 4942650
 * @summary Unit test for java.net.CookieHandler
 * @library /test/lib
 * @run main/othervm CookieHandlerTest
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Yingxian Wang
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;

public class CookieHandlerTest {
    static Map<String,String> cookies;
    ServerSocket ss;

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
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        SSLServerSocketFactory sslssf =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
            (SSLServerSocket) sslssf.createServerSocket();
        sslServerSocket.bind(new InetSocketAddress(loopback, serverPort));
        serverPort = sslServerSocket.getLocalPort();

        /*
         * Signal Client, we're ready for his connect.
         */
        serverReady = true;
        SSLSocket sslSocket = null;
        try {
            sslSocket = (SSLSocket) sslServerSocket.accept();

            // check request contains "Cookie"
            InputStream is = sslSocket.getInputStream ();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            boolean flag = false;
            String x;
            while ((x=r.readLine()) != null) {
                if (x.length() ==0) {
                    break;
                }
                String header = "Cookie: ";
                if (x.startsWith(header)) {
                    if (x.equals("Cookie: "+((String)cookies.get("Cookie")))) {
                        flag = true;
                    }
                }
            }
            if (!flag) {
                throw new RuntimeException("server should see cookie in request");
            }

            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    sslSocket.getOutputStream() ));

            /* send the header */
            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Set-Cookie2: "+((String)cookies.get("Set-Cookie2")+"\r\n"));
            out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.print("<HTML>");
            out.print("<HEAD><TITLE>Testing cookie</TITLE></HEAD>");
            out.print("<BODY>OK.</BODY>");
            out.print("</HTML>");
            out.flush();

            sslSocket.close();
            sslServerSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
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
        HttpsURLConnection http = null;
        /* establish http connection to server */
        URL url = URIBuilder.newBuilder()
                  .scheme("https")
                  .loopback()
                  .port(serverPort)
                  .toURL();
        HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());
        http = (HttpsURLConnection)url.openConnection();

        int respCode = http.getResponseCode();
        http.disconnect();

    }

    static class NameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String args[]) throws Exception {
        String keyFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + keyStoreFile;
        String trustFilename =
            System.getProperty("test.src", "./") + "/" + pathToStores +
                "/" + trustStoreFile;

        CookieHandler reservedCookieHandler = CookieHandler.getDefault();
        HostnameVerifier reservedHV =
            HttpsURLConnection.getDefaultHostnameVerifier();
        try {
            System.setProperty("javax.net.ssl.keyStore", keyFilename);
            System.setProperty("javax.net.ssl.keyStorePassword", passwd);
            System.setProperty("javax.net.ssl.trustStore", trustFilename);
            System.setProperty("javax.net.ssl.trustStorePassword", passwd);

            if (debug)
                System.setProperty("javax.net.debug", "all");

            /*
             * Start the tests.
             */
            cookies = new HashMap<String, String>();
            cookies.put("Cookie",
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\"; $Path=\"/acme\"");
            cookies.put("Set-Cookie2",
              "$Version=\"1\"; Part_Number=\"Riding_Rocket_0023\"; " +
              "$Path=\"/acme/ammo\"; Part_Number=\"Rocket_Launcher_0001\"; "+
              "$Path=\"/acme\"");
            CookieHandler.setDefault(new MyCookieHandler());
            new CookieHandlerTest();
        } finally {
            HttpsURLConnection.setDefaultHostnameVerifier(reservedHV);
            CookieHandler.setDefault(reservedCookieHandler);
        }
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    CookieHandlerTest() throws Exception {
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
        if (serverException != null)
            throw serverException;
        if (clientException != null)
            throw clientException;

        if (!getCalled || !putCalled) {
            throw new RuntimeException ("Either get or put method is not called");
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

    static boolean getCalled = false, putCalled = false;

    static class MyCookieHandler extends CookieHandler {
        public Map<String,List<String>>
            get(URI uri, Map<String,List<String>> requestHeaders)
            throws IOException {
            getCalled = true;
            // returns cookies[0]
            // they will be include in request
            Map<String,List<String>> map = new HashMap<>();
            List<String> l = new ArrayList<>();
            l.add(cookies.get("Cookie"));
            map.put("Cookie",l);
            return Collections.unmodifiableMap(map);
        }

        public void
            put(URI uri, Map<String,List<String>> responseHeaders)
            throws IOException {
            putCalled = true;
            // check response has cookies[1]
            List<String> l = responseHeaders.get("Set-Cookie2");
            String value = l.get(0);
            if (!value.equals((String)cookies.get("Set-Cookie2"))) {
                throw new RuntimeException("cookie should be available for handle to put into cache");
               }
        }
    }

}
