/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Util;
import com.sun.tools.sjavac.options.Options;
import com.sun.tools.sjavac.server.PortFile;
import com.sun.tools.sjavac.server.Sjavac;
import com.sun.tools.sjavac.server.SjavacServer;

/**
 * Sjavac implementation that delegates requests to a SjavacServer.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class SjavacClient implements Sjavac {

    private PortFile portFile;

    // The servercmd option specifies how the server part of sjavac is spawned.
    // It should point to a com.sun.tools.sjavac.Main that supports --startserver
    private String serverCommand;

    // Accept 120 seconds of inactivity before quitting.
    private static final int KEEPALIVE = 120;
    private static final int POOLSIZE = Runtime.getRuntime().availableProcessors();
    // Wait 2 seconds for response, before giving up on javac server.
    private static final int CONNECTION_TIMEOUT = 2000;
    private static final int MAX_CONNECT_ATTEMPTS = 3;
    private static final int WAIT_BETWEEN_CONNECT_ATTEMPTS = 2000;

    public SjavacClient(Options options) {
        String serverConf = options.getServerConf();
        String configFile = Util.extractStringOption("conf", serverConf, "");

        try {
            List<String> configFileLines = Files.readAllLines(Path.of(configFile), StandardCharsets.UTF_8);
            String configFileContent = String.join("\n", configFileLines);

            String portfileName = Util.extractStringOptionLine("portfile", configFileContent, "");
            if (portfileName.isEmpty()) {
                Log.error("Configuration file missing value for 'portfile'");
                portFile = null;
            } else  {
                portFile = SjavacServer.getPortFile(portfileName);
            }

            String serverCommandString = Util.extractStringOptionLine("servercmd", configFileContent, "");
            if (serverCommandString.isEmpty()) {
                Log.error("Configuration file missing value for 'servercmd'");
                serverCommand = null;
            } else  {
                serverCommand = serverCommandString;
            }
        } catch (IOException e) {
            Log.error("Cannot read configuration file " + configFile);
            Log.debug(e);
            portFile = null;
            serverCommand = null;
        }
    }

    @Override
    public Result compile(String[] args) {
        if (portFile == null || serverCommand == null) {
            Log.error("Incorrect configuration, portfile and/or servercmd missing");
            return Result.ERROR;
        }

        Result result = null;
        try (Socket socket = tryConnect()) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send args array to server
            out.println(args.length);
            for (String arg : args)
                out.println(arg);
            out.flush();

            // Read server response line by line
            String line;
            while (null != (line = in.readLine())) {
                if (!line.contains(":")) {
                    throw new AssertionError("Could not parse protocol line: >>\"" + line + "\"<<");
                }
                String[] typeAndContent = line.split(":", 2);
                String type = typeAndContent[0];
                String content = typeAndContent[1];

                try {
                    if (Log.isDebugging()) {
                        // Distinguish server generated output if debugging.
                        content = "[sjavac-server] " + content;
                    }
                    Log.log(Log.Level.valueOf(type), content);
                    continue;
                } catch (IllegalArgumentException e) {
                    // Parsing of 'type' as log level failed.
                }

                if (type.equals(SjavacServer.LINE_TYPE_RC)) {
                    result = Main.Result.valueOf(content);
                }
            }
        } catch (PortFileInaccessibleException e) {
            Log.error("Port file inaccessible.");
            result = Result.ERROR;
        } catch (IOException ioe) {
            Log.error("IOException caught during compilation: " + ioe.getMessage());
            Log.debug(ioe);
            result = Result.ERROR;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // Restore interrupt
            Log.error("Compilation interrupted.");
            Log.debug(ie);
            result = Result.ERROR;
        }

        if (result == null) {
            // No LINE_TYPE_RC was found.
            result = Result.ERROR;
        }

        return result;
    }

    /*
     * Makes MAX_CONNECT_ATTEMPTS attempts to connect to server.
     */
    private Socket tryConnect() throws IOException, InterruptedException {
        makeSureServerIsRunning();
        int attempt = 0;
        while (true) {
            Log.debug("Trying to connect. Attempt " + (++attempt) + " of " + MAX_CONNECT_ATTEMPTS);
            try {
                return makeConnectionAttempt();
            } catch (IOException ex) {
                Log.error("Connection attempt failed: " + ex.getMessage());
                if (attempt >= MAX_CONNECT_ATTEMPTS) {
                    Log.error("Giving up");
                    throw new IOException("Could not connect to server", ex);
                }
            }
            Thread.sleep(WAIT_BETWEEN_CONNECT_ATTEMPTS);
        }
    }

    private Socket makeConnectionAttempt() throws IOException {
        Socket socket = new Socket();
        InetAddress localhost = InetAddress.getByName(null);
        InetSocketAddress address = new InetSocketAddress(localhost, portFile.getPort());
        socket.connect(address, CONNECTION_TIMEOUT);
        Log.debug("Connected");
        return socket;
    }

    /*
     * Will return immediately if a server already seems to be running,
     * otherwise fork a new server and block until it seems to be running.
     */
    private void makeSureServerIsRunning()
            throws IOException, InterruptedException {

        if (portFile.exists()) {
            portFile.lock();
            portFile.getValues();
            portFile.unlock();

            if (portFile.containsPortInfo()) {
                // Server seems to already be running
                return;
            }
        }

        // Fork a new server and wait for it to start
        startNewServer();
    }

    @Override
    public void shutdown() {
        // Nothing to clean up
    }

    /*
     * Fork a server process process and wait for server to come around
     */
    public void startNewServer()
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(serverCommand.split(" ")));
        cmd.add("--startserver:"
              + "portfile=" + portFile.getFilename()
              + ",poolsize=" + POOLSIZE
              + ",keepalive="+ KEEPALIVE);

        Process serverProcess;
        Log.debug("Starting server. Command: " + String.join(" ", cmd));
        try {
            // If the cmd for some reason can't be executed (file is not found,
            // or is not executable for instance) this will throw an
            // IOException and p == null.
            serverProcess = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ex) {
            // Message is typically something like:
            // Cannot run program "xyz": error=2, No such file or directory
            Log.error("Failed to create server process: " + ex.getMessage());
            Log.debug(ex);
            throw new IOException(ex);
        }

        // serverProcess != null at this point.
        try {
            // Throws an IOException if no valid values materialize
            portFile.waitForValidValues();
        } catch (IOException ex) {
            // Process was started, but server failed to initialize. This could
            // for instance be due to the JVM not finding the server class,
            // or the server running in to some exception early on.
            Log.error("Sjavac server failed to initialize: " + ex.getMessage());
            Log.error("Process output:");
            Reader serverStdoutStderr = new InputStreamReader(serverProcess.getInputStream());
            try (BufferedReader br = new BufferedReader(serverStdoutStderr)) {
                br.lines().forEach(Log::error);
            }
            Log.error("<End of process output>");
            try {
                Log.error("Process exit code: " + serverProcess.exitValue());
            } catch (IllegalThreadStateException e) {
                // Server is presumably still running.
            }
            throw new IOException("Server failed to initialize: " + ex.getMessage(), ex);
        }
    }
}
