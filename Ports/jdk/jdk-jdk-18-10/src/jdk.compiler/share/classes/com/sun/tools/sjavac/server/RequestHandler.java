/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.sjavac.server;

import com.sun.tools.javac.main.Main;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;

import static com.sun.tools.sjavac.server.SjavacServer.LINE_TYPE_RC;


/**
 * A RequestHandler handles requests performed over a socket. Specifically it
 *  - Reads the command string specifying which method is to be invoked
 *  - Reads the appropriate arguments
 *  - Delegates the actual invocation to the given sjavac implementation
 *  - Writes the result back to the socket output stream
 *
 * None of the work performed by this class is really bound by the CPU. It
 * should be completely fine to have a large number of RequestHandlers active.
 * To limit the number of concurrent compilations, use PooledSjavac.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class RequestHandler extends Thread {

    private final Socket socket;
    private final Sjavac sjavac;

    public RequestHandler(Socket socket, Sjavac sjavac) {
        this.socket = socket;
        this.sjavac = sjavac;
    }

    @Override
    public void run() {

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Set up logging for this thread. Stream back logging messages to
            // client on the format format "level:msg".
            Log.setLogForCurrentThread(new Log(out, out) {
                @Override
                protected boolean isLevelLogged(Level l) {
                    // Make sure it is up to the client to decide whether or
                    // not this message should be displayed.
                    return true;
                }

                @Override
                protected void printLogMsg(Level msgLevel, String msg) {
                    // Follow sjavac server/client protocol: Send one line
                    // at a time and prefix with message with "level:".
                    Util.getLines(msg)
                        .map(line -> msgLevel + ":" + line)
                        .forEach(line -> super.printLogMsg(msgLevel, line));
                }
            });

            // Read argument array
            int n = Integer.parseInt(in.readLine());
            String[] args = new String[n];
            for (int i = 0; i < n; i++) {
                args[i] = in.readLine();
            }

            // If there has been any internal errors, notify client
            checkInternalErrorLog();

            // Perform compilation
            Main.Result rc = sjavac.compile(args);

            // Send return code back to client
            out.println(LINE_TYPE_RC + ":" + rc.name());

            // Check for internal errors again.
            checkInternalErrorLog();
        } catch (Exception ex) {
            // Not much to be done at this point. The client side request
            // code will most likely throw an IOException and the
            // compilation will fail.
            Log.error(ex);
        } finally {
            Log.setLogForCurrentThread(null);
        }
    }

    private void checkInternalErrorLog() {
        Path errorLog = ServerMain.getErrorLog().getLogDestination();
        if (errorLog != null) {
            Log.error("Server has encountered an internal error. See " + errorLog.toAbsolutePath()
                    + " for details.");
        }
    }
}
