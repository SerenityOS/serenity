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

package compiler.lib.ir_framework.shared;

import compiler.lib.ir_framework.TestFramework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Dedicated socket to send data from the flag and test VM back to the driver VM.
 */
public class TestFrameworkSocket implements AutoCloseable {
    public static final String STDOUT_PREFIX = "[STDOUT]";
    public static final String TESTLIST_TAG = "[TESTLIST]";
    public static final String DEFAULT_REGEX_TAG = "[DEFAULT_REGEX]";

    // Static fields used for test VM only.
    private static final String SERVER_PORT_PROPERTY = "ir.framework.server.port";
    private static final int SERVER_PORT = Integer.getInteger(SERVER_PORT_PROPERTY, -1);

    private static final boolean REPRODUCE = Boolean.getBoolean("Reproduce");
    private static final String HOSTNAME = null;
    private static Socket clientSocket = null;
    private static PrintWriter clientWriter = null;

    private final String serverPortPropertyFlag;
    private FutureTask<String> socketTask;
    private final ServerSocket serverSocket;
    private boolean receivedStdOut = false;

    public TestFrameworkSocket() {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new TestFrameworkException("Failed to create TestFramework server socket", e);
        }
        int port = serverSocket.getLocalPort();
        if (TestFramework.VERBOSE) {
            System.out.println("TestFramework server socket uses port " + port);
        }
        serverPortPropertyFlag = "-D" + SERVER_PORT_PROPERTY + "=" + port;
        start();
    }

    public String getPortPropertyFlag() {
        return serverPortPropertyFlag;
    }

    private void start() {
        socketTask = initSocketTask();
        Thread socketThread = new Thread(socketTask);
        socketThread.start();
    }

    /**
     * Waits for a client (created by flag or test VM) to connect. Return the messages received from the client.
     */
    private FutureTask<String> initSocketTask() {
        return new FutureTask<>(() -> {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                StringBuilder builder = new StringBuilder();
                String next;
                while ((next = in.readLine()) != null) {
                    builder.append(next).append(System.lineSeparator());
                    if (next.startsWith(STDOUT_PREFIX)) {
                        receivedStdOut = true;
                    }
                }
                return builder.toString();
            } catch (IOException e) {
                throw new TestFrameworkException("Server socket error", e);
            }
        });
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new TestFrameworkException("Could not close socket", e);
        }
    }

    /**
     * Only called by test VM to write to server socket.
     */
    public static void write(String msg, String tag) {
        write(msg, tag, false);
    }

    /**
     * Only called by test VM to write to server socket.
     */
    public static void write(String msg, String tag, boolean stdout) {
        if (REPRODUCE) {
            System.out.println("Debugging Test VM: Skip writing due to -DReproduce");
            return;
        }
        TestFramework.check(SERVER_PORT != -1, "Server port was not set correctly for flag and/or test VM "
                                               + "or method not called from flag or test VM");
        try {
            // Keep the client socket open until the test VM terminates (calls closeClientSocket before exiting main()).
            if (clientSocket == null) {
                clientSocket = new Socket(HOSTNAME, SERVER_PORT);
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            }
            if (stdout) {
                msg = STDOUT_PREFIX + tag + " " + msg;
            }
            clientWriter.println(msg);
        } catch (Exception e) {
            // When the test VM is directly run, we should ignore all messages that would normally be sent to the
            // driver VM.
            String failMsg = System.lineSeparator() + System.lineSeparator() + """
                             ###########################################################
                              Did you directly run the test VM (TestVM class)
                              to reproduce a bug?
                              => Append the flag -DReproduce=true and try again!
                             ###########################################################
                             """;
            throw new TestRunException(failMsg, e);
        }
        if (TestFramework.VERBOSE) {
            System.out.println("Written " + tag + " to socket:");
            System.out.println(msg);
        }
    }

    /**
     * Closes (and flushes) the printer to the socket and the socket itself. Is called as last thing before exiting
     * the main() method of the flag and the test VM.
     */
    public static void closeClientSocket() {
        if (clientSocket != null) {
            try {
                clientWriter.close();
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close TestVM socket", e);
            }
        }
    }

    /**
     * Get the socket output of the flag VM.
     */
    public String getOutput() {
        try {
            return socketTask.get();
        } catch (ExecutionException e) {
            // Thrown when socket task was not finished, yet (i.e. no client sent data) but socket was already closed.
            return "";
        } catch (Exception e) {
            throw new TestFrameworkException("Could not read from socket task", e);
        }
    }

    /**
     * Return whether test VM sent messages to be put on stdout (starting with {@link ::STDOUT_PREFIX}).
     */
    public boolean hasStdOut() {
        return receivedStdOut;
    }
}
