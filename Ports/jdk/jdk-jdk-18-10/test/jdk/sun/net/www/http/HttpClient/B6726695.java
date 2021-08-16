/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6726695 6993490
 * @summary HttpURLConnection should support 'Expect: 100-contimue' headers for PUT
 * @library /test/lib
 * @run main B6726695
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6726695
*/

import java.net.*;
import java.io.*;

import jdk.test.lib.net.URIBuilder;

public class B6726695 extends Thread {
    private ServerSocket server = null;
    private int port = 0;
    private byte[] data = new byte[512];
    private String boundary = "----------------7774563516523621";

    public static void main(String[] args) throws Exception {
        B6726695 test = new B6726695();
        // Exit even if server is still running
        test.setDaemon(true);
        // start server
        test.start();
        // run test
        test.test();
    }

    public B6726695() {
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            port = server.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void test() throws Exception {
        /**
         * This is a hardcoded test. The server side expects 3 requests with a
         * Expect: 100-continue header. It will reject the 1st one and accept
         * the second one. Thus allowing us to test both scenarios.
         * The 3rd case is the simulation of a server that just plains ignore
         * the Expect: 100-Continue header. So the POST should proceed after
         * a timeout.
         */
        URL url = URIBuilder.newBuilder()
                  .scheme("http")
                  .loopback()
                  .port(port)
                  .path("/foo")
                  .toURL();

        // 1st Connection. Should be rejected. I.E. get a ProtocolException
        URLConnection con = url.openConnection(Proxy.NO_PROXY);
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST");
        http.setRequestProperty("Expect", "100-Continue");
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        http.setDoOutput(true);
        http.setFixedLengthStreamingMode(512);
        OutputStream out = null;
        int errorCode = -1;
        try {
            out = http.getOutputStream();
        } catch (ProtocolException e) {
            errorCode = http.getResponseCode();
        }
        if (errorCode != 417) {
            throw new RuntimeException("Didn't get the ProtocolException");
        }

        // 2nd connection. Should be accepted by server.
        http = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        http.setRequestMethod("POST");
        http.setRequestProperty("Expect", "100-Continue");
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        http.setDoOutput(true);
        http.setFixedLengthStreamingMode(data.length);
        out = null;
        try {
            out = http.getOutputStream();
        } catch (ProtocolException e) {
        }
        if (out == null) {
            throw new RuntimeException("Didn't get an OutputStream");
        }
        out.write(data);
        out.flush();
        errorCode = http.getResponseCode();
        if (errorCode != 200) {
            throw new RuntimeException("Response code is " + errorCode);
        }
        out.close();

        // 3rd connection. Simulate a server that doesn't implement 100-continue
        http = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        http.setRequestMethod("POST");
        http.setRequestProperty("Expect", "100-Continue");
        http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        http.setDoOutput(true);
        http.setFixedLengthStreamingMode(data.length);
        out = null;
        try {
            out = http.getOutputStream();
        } catch (ProtocolException e) {
        }
        if (out == null) {
            throw new RuntimeException("Didn't get an OutputStream");
        }
        out.write(data);
        out.flush();
        out.close();
        errorCode = http.getResponseCode();
        if (errorCode != 200) {
            throw new RuntimeException("Response code is " + errorCode);
        }
    }


    @Override
    public void run() {
        try {
            // Fist connection: don't accetpt the request
            Socket s = server.accept();
            serverReject(s);
            // Second connection: accept the request (send 100-continue)
            s = server.accept();
            serverAccept(s);
            // 3rd connection: just ignore the 'Expect:' header
            s = server.accept();
            serverIgnore(s);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { server.close(); } catch (IOException unused) {}
        }
    }

    public void serverReject(Socket s) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintStream out = new PrintStream(new BufferedOutputStream(s.getOutputStream()));
        String line = null;
        do {
            line = in.readLine();
        } while (line != null && line.length() != 0);

        out.print("HTTP/1.1 417 Expectation Failed\r\n");
        out.print("Server: Sun-Java-System-Web-Server/7.0\r\n");
        out.print("Connection: close\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
        out.close();
        in.close();
    }

    public void serverAccept(Socket s) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintStream out = new PrintStream(new BufferedOutputStream(s.getOutputStream()));
        String line = null;
        do {
            line = in.readLine();
        } while (line != null && line.length() != 0);

        // Send 100-Continue
        out.print("HTTP/1.1 100 Continue\r\n");
        out.print("\r\n");
        out.flush();
        // Then read the body
        char[] cbuf = new char[512];
        in.read(cbuf);

        /* Force the server to not respond for more that the expect 100-Continue
         * timeout set by the HTTP handler (5000 millis). This ensures the
         * timeout is correctly resets the default read timeout, infinity.
         * See 6993490. */
        System.out.println("server sleeping...");
        try {Thread.sleep(6000); } catch (InterruptedException e) {}

        // finally send the 200 OK
        out.print("HTTP/1.1 200 OK");
        out.print("Server: Sun-Java-System-Web-Server/7.0\r\n");
        out.print("Connection: close\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        out.flush();
        out.close();
        in.close();
    }

    public void serverIgnore(Socket s) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintStream out = new PrintStream(new BufferedOutputStream(s.getOutputStream()));
        String line = null;
        do {
            line = in.readLine();
        } while (line != null && line.length() != 0);

        // Then read the body
        char[] cbuf = new char[512];
        int l = in.read(cbuf);
        // finally send the 200 OK
        out.print("HTTP/1.1 200 OK");
        out.print("Server: Sun-Java-System-Web-Server/7.0\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");
        out.flush();
        out.close();
        in.close();
    }
}
