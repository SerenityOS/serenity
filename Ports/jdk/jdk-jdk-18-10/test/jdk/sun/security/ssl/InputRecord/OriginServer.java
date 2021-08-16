/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 *
 * This is a HTTP test server used by the ClientHelloRead.java
 */

import java.io.*;
import java.net.*;
import javax.net.*;

/*
 * OriginServer.java -- a simple server that can serve
 * Http get request in both clear and secure channel
 */

public abstract class OriginServer implements Runnable {

    private ServerSocket server = null;
    Exception serverException = null;
    /**
     * Constructs a OriginServer based on ss and
     * obtains a response data's bytecodes using the method
     * getBytes.
     */
    protected OriginServer(ServerSocket ss) throws Exception
    {
        server = ss;
        newListener();
        if (serverException != null)
            throw serverException;
    }

    /**
     * Returns an array of bytes containing the bytes for
     * data sent in the response.
     *
     * @return the bytes for the information that is being sent
     */
    public abstract byte[] getBytes();

    /**
     * The "listen" thread that accepts a connection to the
     * server, parses header and sends back the response
     */
    public void run()
    {
        Socket socket;

        // accept a connection
        try {
            socket = server.accept();
        } catch (IOException e) {
            System.out.println("Class Server died: " + e.getMessage());
            serverException = e;
            return;
        }
        try {
            DataOutputStream out =
                new DataOutputStream(socket.getOutputStream());
            try {
                BufferedReader in =
                    new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));
                // read the request
                readRequest(in);
                // retrieve bytecodes
                byte[] bytecodes = getBytes();
                // send bytecodes in response (assumes HTTP/1.0 or later)
                try {
                    out.writeBytes("HTTP/1.0 200 OK\r\n");
                    out.writeBytes("Content-Length: " + bytecodes.length +
                                   "\r\n");
                    out.writeBytes("Content-Type: text/html\r\n\r\n");
                    out.write(bytecodes);
                    out.flush();
                } catch (IOException ie) {
                    serverException = ie;
                    return;
                }

            } catch (Exception e) {
                // write out error response
                out.writeBytes("HTTP/1.0 400 " + e.getMessage() + "\r\n");
                out.writeBytes("Content-Type: text/html\r\n\r\n");
                out.flush();
            }

        } catch (IOException ex) {
            System.out.println("Server: Error writing response: "
                                 + ex.getMessage());
            serverException = ex;

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                serverException = e;
            }
        }
    }

    /**
     * Create a new thread to listen.
     */
    private void newListener()
    {
        (new Thread(this)).start();
    }

    /**
     * read the response, don't care for the syntax of the request-line
     */
    private static void readRequest(BufferedReader in)
        throws IOException
    {
        String line = null;
        do {
            line = in.readLine();
        } while ((line.length() != 0) &&
                (line.charAt(0) != '\r') && (line.charAt(0) != '\n'));
    }
}
