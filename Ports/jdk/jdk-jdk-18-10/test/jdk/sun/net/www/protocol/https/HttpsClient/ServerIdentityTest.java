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
 * @bug 4328195
 * @summary Need to include the alternate subject DN for certs,
 *          https should check for this
 * @library /javax/net/ssl/templates
 * @run main/othervm ServerIdentityTest dnsstore localhost
 * @run main/othervm ServerIdentityTest ipstore 127.0.0.1
 *
 * @author Yingxian Wang
 */

import java.io.InputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public final class ServerIdentityTest extends SSLSocketTemplate {

    private static String keystore;
    private static String hostname;
    private static SSLContext context;

    /*
     * Run the test case.
     */
    public static void main(String[] args) throws Exception {
        // Get the customized arguments.
        initialize(args);

        (new ServerIdentityTest()).run();
    }

    ServerIdentityTest() throws UnknownHostException {
        serverAddress = InetAddress.getByName(hostname);
    }

    @Override
    protected boolean isCustomizedClientConnection() {
        return true;
    }

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        sslIS.read();
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
        bw.write("HTTP/1.1 200 OK\r\n\r\n\r\n");
        bw.flush();
        socket.getSession().invalidate();
    }

    @Override
    protected void runClientApplication(int serverPort) throws Exception {
        URL url = new URL(
                "https://" + hostname + ":" + serverPort + "/index.html");

        HttpURLConnection urlc = null;
        InputStream is = null;
        try {
            urlc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
            is = urlc.getInputStream();
        } finally {
            if (is != null) {
                is.close();
            }
            if (urlc != null) {
                urlc.disconnect();
            }
        }
    }

    @Override
    protected SSLContext createServerSSLContext() throws Exception {
        return context;
    }

    @Override
    protected SSLContext createClientSSLContext() throws Exception {
        return context;
    }

    private static void initialize(String[] args) throws Exception {
        keystore = args[0];
        hostname = args[1];

        String password = "changeit";
        String keyFilename =
                System.getProperty("test.src", ".") + "/" + keystore;
        String trustFilename =
                System.getProperty("test.src", ".") + "/" + keystore;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", password);

        context = SSLContext.getDefault();
        HttpsURLConnection.setDefaultSSLSocketFactory(
                context.getSocketFactory());
    }
}
