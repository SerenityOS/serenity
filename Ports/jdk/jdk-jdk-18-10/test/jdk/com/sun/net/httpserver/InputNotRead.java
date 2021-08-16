/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8266761
 * @summary HttpServer can fail with an assertion error when a handler doesn't fully
 *          read the request body and sends back a reply with no content, or when
 *          the client closes its outputstream while the server tries to drains
 *          its content.
 * @run testng/othervm InputNotRead
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true InputNotRead
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.*;

public class InputNotRead {

    private static final int msgCode = 200;
    private static final String someContext = "/context";

    static class ServerThreadFactory implements ThreadFactory {
        static final AtomicLong tokens = new AtomicLong();
        @Override
        public Thread newThread(Runnable r) {
            var thread = new Thread(r, "Server-" + tokens.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    static {
        Logger.getLogger("").setLevel(Level.ALL);
        Logger.getLogger("").getHandlers()[0].setLevel(Level.ALL);
    }

    @Test
    public void testSendResponse() throws Exception {
        System.out.println("testSendResponse()");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        ExecutorService executor = Executors.newCachedThreadPool(new ServerThreadFactory());
        server.setExecutor(executor);
        try {
            server.createContext(someContext, new HttpHandler() {
                @Override
                public void handle(HttpExchange msg) throws IOException {
                    System.err.println("Handling request: " + msg.getRequestURI());
                    byte[] reply = new byte[0];
                    try {
                        msg.getRequestBody().read();
                        try {
                            msg.sendResponseHeaders(msgCode, reply.length == 0 ? -1 : reply.length);
                        } catch(IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } finally {
                        // don't close the exchange and don't close any stream
                        // to trigger the assertion.
                        System.err.println("Request handled: " + msg.getRequestURI());
                    }
                }
            });
            server.start();
            System.out.println("Server started at port "
                               + server.getAddress().getPort());

            runRawSocketHttpClient(loopback, server.getAddress().getPort(), -1);
        } finally {
            System.out.println("shutting server down");
            executor.shutdown();
            server.stop(0);
        }
        System.out.println("Server finished.");
    }

    @Test
    public void testCloseOutputStream() throws Exception {
        System.out.println("testCloseOutputStream()");
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        ExecutorService executor = Executors.newCachedThreadPool(new ServerThreadFactory());
        server.setExecutor(executor);
        try {
            server.createContext(someContext, new HttpHandler() {
                @Override
                public void handle(HttpExchange msg) throws IOException {
                    System.err.println("Handling request: " + msg.getRequestURI());
                    byte[] reply = "Here is my reply!".getBytes(UTF_8);
                    try {
                        BufferedReader r = new BufferedReader(new InputStreamReader(msg.getRequestBody()));
                        r.read();
                        try {
                            msg.sendResponseHeaders(msgCode, reply.length == 0 ? -1 : reply.length);
                            msg.getResponseBody().write(reply);
                            msg.getResponseBody().close();
                            Thread.sleep(50);
                        } catch(IOException | InterruptedException ie) {
                            ie.printStackTrace();
                        }
                    } finally {
                        System.err.println("Request handled: " + msg.getRequestURI());
                    }
                }
            });
            server.start();
            System.out.println("Server started at port "
                    + server.getAddress().getPort());

            runRawSocketHttpClient(loopback, server.getAddress().getPort(), 64 * 1024 + 16);
        } finally {
            System.out.println("shutting server down");
            executor.shutdown();
            server.stop(0);
        }
        System.out.println("Server finished.");
    }

    static void runRawSocketHttpClient(InetAddress address, int port, int contentLength)
        throws Exception
    {
        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader reader = null;
        final String CRLF = "\r\n";
        try {
            socket = new Socket(address, port);
            writer = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream()));
            System.out.println("Client connected by socket: " + socket);
            String body = "I will send all the data.";
            if (contentLength <= 0)
                contentLength = body.getBytes(UTF_8).length;

            writer.print("GET " + someContext + "/ HTTP/1.1" + CRLF);
            writer.print("User-Agent: Java/"
                + System.getProperty("java.version")
                + CRLF);
            writer.print("Host: " + address.getHostName() + CRLF);
            writer.print("Accept: */*" + CRLF);
            writer.print("Content-Length: " + contentLength + CRLF);
            writer.print("Connection: keep-alive" + CRLF);
            writer.print(CRLF); // Important, else the server will expect that
            // there's more into the request.
            writer.flush();
            System.out.println("Client wrote request to socket: " + socket);
            writer.print(body);
            writer.flush();

            reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
            System.out.println("Client start reading from server:"  );
            String line = reader.readLine();
            for (; line != null; line = reader.readLine()) {
                if (line.isEmpty()) {
                    break;
                }
                System.out.println("\""  + line + "\"");
            }
            System.out.println("Client finished reading from server"  );
        } finally {
            // give time to the server to try & drain its input stream
            Thread.sleep(500);
            // closes the client outputstream while the server is draining
            // it
            if (writer != null) {
                writer.close();
            }
            // give time to the server to trigger its assertion
            // error before closing the connection
            Thread.sleep(500);
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                    logOrIgnore.printStackTrace();
                }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException logOrIgnore) {
                    logOrIgnore.printStackTrace();
                }
            }
        }
        System.out.println("Client finished." );
    }

}
