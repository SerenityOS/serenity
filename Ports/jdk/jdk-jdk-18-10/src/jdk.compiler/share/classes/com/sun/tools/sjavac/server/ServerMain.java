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

import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Log.Level;
import com.sun.tools.sjavac.server.log.LazyInitFileLog;
import com.sun.tools.sjavac.server.log.LoggingOutputStream;

import static com.sun.tools.sjavac.Log.Level.ERROR;
import static com.sun.tools.sjavac.Log.Level.INFO;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ServerMain {

    // For logging server internal (non request specific) errors.
    private static LazyInitFileLog errorLog;

    public static int run(String[] args) {

        // Under normal operation, all logging messages generated server-side
        // are due to compilation requests. These logging messages should
        // be relayed back to the requesting client rather than written to the
        // server log. The only messages that should be written to the server
        // log (in production mode) should be errors,
        Log.setLogForCurrentThread(errorLog = new LazyInitFileLog("server.log"));
        Log.setLogLevel(ERROR); // should be set to ERROR.

        // Make sure no exceptions go under the radar
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.setLogForCurrentThread(errorLog);
            Log.error(e);
        });

        // Inevitably someone will try to print messages using System.{out,err}.
        // Make sure this output also ends up in the log.
        System.setOut(new PrintStream(new LoggingOutputStream(System.out, INFO, "[stdout] ")));
        System.setErr(new PrintStream(new LoggingOutputStream(System.err, ERROR, "[stderr] ")));

        // Any options other than --startserver?
        if (args.length > 1) {
            Log.error("When spawning a background server, only a single --startserver argument is allowed.");
            return Result.CMDERR.exitCode;
        }

        int exitCode;
        try {
            SjavacServer server = new SjavacServer(args[0]);
            exitCode = server.startServer();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            exitCode = Result.ERROR.exitCode;
        }

        return exitCode;
    }

    public static LazyInitFileLog getErrorLog() {
        return errorLog;
    }
}
