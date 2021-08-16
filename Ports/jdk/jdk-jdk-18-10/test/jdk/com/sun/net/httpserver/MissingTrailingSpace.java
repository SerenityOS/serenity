/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8068795
 * @summary HttpServer missing tailing space for some response codes
 * @run main MissingTrailingSpace
 * @run main/othervm -Djava.net.preferIPv6Addresses=true MissingTrailingSpace
 * @author lev.priima@oracle.com
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MissingTrailingSpace {

    private static final int noMsgCode = 207;
    private static final String someContext = "/context";

    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        try {
            server.setExecutor(Executors.newFixedThreadPool(1));
            server.createContext(someContext, new HttpHandler() {
                @Override
                public void handle(HttpExchange msg) {
                    try {
                        try {
                            msg.sendResponseHeaders(noMsgCode, -1);
                        } catch(IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } finally {
                        msg.close();
                    }
                }
            });
            server.start();
            System.out.println("Server started at port "
                               + server.getAddress().getPort());

            runRawSocketHttpClient(loopback, server.getAddress().getPort());
        } finally {
            ((ExecutorService)server.getExecutor()).shutdown();
            server.stop(0);
        }
        System.out.println("Server finished.");
    }

    static void runRawSocketHttpClient(InetAddress address, int port)
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

            writer.print("GET " + someContext + "/ HTTP/1.1" + CRLF);
            writer.print("User-Agent: Java/"
                + System.getProperty("java.version")
                + CRLF);
            writer.print("Host: " + address.getHostName() + CRLF);
            writer.print("Accept: */*" + CRLF);
            writer.print("Connection: keep-alive" + CRLF);
            writer.print(CRLF); // Important, else the server will expect that
            // there's more into the request.
            writer.flush();
            System.out.println("Client wrote rquest to socket: " + socket);

            reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
            System.out.println("Client start reading from server:"  );
            String line = reader.readLine();
            if ( !line.endsWith(" ") ) {
                throw new RuntimeException("respond to unknown code "
                    + noMsgCode
                    + " doesn't return space at the end of the first header.\n"
                    + "Should be: " + "\"" + line + " \""
                    + ", but returns: " + "\"" + line + "\".");
            }
            for (; line != null; line = reader.readLine()) {
                if (line.isEmpty()) {
                    break;
                }
                System.out.println("\""  + line + "\"");
            }
            System.out.println("Client finished reading from server"  );
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                    logOrIgnore.printStackTrace();
                }
            if (writer != null) {
                writer.close();
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
