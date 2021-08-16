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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 4323990 4413069 8160838
 * @summary HttpsURLConnection doesn't send Proxy-Authorization on CONNECT
 *     Incorrect checking of proxy server response
 * @modules jdk.crypto.ec
 *          java.base/sun.net.www
 * @library /javax/net/ssl/templates
 * @run main/othervm ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=Basic
 *      ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=Basic,
 *      ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=BAsIc
 *      ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=Basic,Digest
 *      ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=Unknown,bAsIc
 *      ProxyAuthTest fail
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=
 *      ProxyAuthTest succeed
 * @run main/othervm
 *      -Djdk.http.auth.tunneling.disabledSchemes=Digest,NTLM,Negotiate
 *      ProxyAuthTest succeed
 * @run main/othervm -Djdk.http.auth.tunneling.disabledSchemes=UNKNOWN,notKnown
 *      ProxyAuthTest succeed
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLContext;
import static java.nio.charset.StandardCharsets.US_ASCII;

/*
 * ProxyAuthTest.java -- includes a simple server that can serve
 * Http get request in both clear and secure channel, and a client
 * that makes https requests behind the firewall through an
 * authentication proxy
 */

public class ProxyAuthTest extends SSLSocketTemplate {
    private static boolean expectSuccess;

    ProxyAuthTest() {
        serverAddress = InetAddress.getLoopbackAddress();
    }

    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        // Get the customized arguments.
        parseArguments(args);

        (new ProxyAuthTest()).run();
    }

    @Override
    protected boolean isCustomizedClientConnection() {
        return true;
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        String response = "Proxy authentication for tunneling succeeded ..";
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // read the request
            readRequest(in);

            // retrieve bytecodes
            byte[] bytecodes = response.getBytes(US_ASCII);

            // send bytecodes in response (assumes HTTP/1.0 or later)
            out.writeBytes("HTTP/1.0 200 OK\r\n");
            out.writeBytes("Content-Length: " + bytecodes.length + "\r\n");
            out.writeBytes("Content-Type: text/html\r\n\r\n");
            out.write(bytecodes);
            out.flush();
        } catch (IOException e) {
            // write out error response
            out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
            out.writeBytes("Content-Type: text/html\r\n\r\n");
            out.flush();
        }
    }

    @Override
    protected void runClientApplication(int serverPort) throws Exception {
        /*
         * Set the default SSLSocketFactory.
         */
        SSLContext context = createClientSSLContext();
        HttpsURLConnection.setDefaultSSLSocketFactory(
                context.getSocketFactory());

        /*
         * setup up a proxy with authentication information
         */
        ProxyTunnelServer ps = setupProxy();

        /*
         * we want to avoid URLspoofCheck failures in cases where the cert
         * DN name does not match the hostname in the URL.
         */
        HttpsURLConnection.setDefaultHostnameVerifier(new NameVerifier());

        InetSocketAddress paddr = InetSocketAddress
            .createUnresolved(ps.getInetAddress().getHostAddress(),
                              ps.getPort());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, paddr);

        InetAddress serverAddress = this.serverAddress;
        String host = serverAddress == null
                ? "localhost"
                : serverAddress.getHostAddress();
        if (host.indexOf(':') > -1) host = "[" + host + "]";
        URL url = new URL(
                "https://" + host + ":" + serverPort + "/index.html");
        System.out.println("URL: " + url);
        BufferedReader in = null;
        HttpsURLConnection uc = (HttpsURLConnection) url.openConnection(proxy);
        try {
            in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
            String inputLine;
            System.out.print("Client received from the server: ");
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
            }
            if (!expectSuccess) {
                throw new RuntimeException(
                    "Expected exception/failure to connect, but succeeded.");
            }
        } catch (IOException e) {
            if (expectSuccess) {
                System.out.println("Client side failed: " + e.getMessage());
                throw e;
            }

            // Assert that the error stream is not accessible from the failed
            // tunnel setup.
            if (uc.getErrorStream() != null) {
                throw new RuntimeException("Unexpected error stream.");
            }

            if (!e.getMessage().contains("Unable to tunnel through proxy") ||
                !e.getMessage().contains("407")) {

                throw new RuntimeException(
                        "Expected exception about cannot tunnel, " +
                        "407, etc, but got", e);
            } else {
                // Informative
                System.out.println(
                        "Caught expected exception: " + e.getMessage());
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }


    private static void parseArguments(String[] args) {
        if (args[0].equals("succeed")) {
            expectSuccess = true;
        } else {
            expectSuccess = false;
        }
    }

    /**
     * read the response, don't care for the syntax of the request-line
     * for this testing
     */
    private static void readRequest(BufferedReader in) throws IOException {
        String line = null;
        System.out.println("Server received: ");
        do {
            if (line != null) {
                System.out.println(line);
            }
            line = in.readLine();
        } while ((line.length() != 0) &&
                (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
    }

    private static class NameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static ProxyTunnelServer setupProxy() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ProxyTunnelServer pserver = new ProxyTunnelServer(loopback);

        /*
         * register a system wide authenticator and setup the proxy for
         * authentication
         */
        Authenticator.setDefault(new TestAuthenticator());

        // register with the username and password
        pserver.needUserAuth(true);
        pserver.setUserAuth("Test", "test123");

        pserver.start();
        return pserver;
    }

    private static class TestAuthenticator extends Authenticator {

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("Test", "test123".toCharArray());
        }
    }
}
