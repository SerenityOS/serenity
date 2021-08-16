/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.InternalException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;

abstract class AbstractLauncher extends ConnectorImpl
                                implements LaunchingConnector
{
    abstract public VirtualMachine
        launch(Map<String, ? extends Connector.Argument> arguments)
        throws IOException, IllegalConnectorArgumentsException,
               VMStartException;

    abstract public String name();

    abstract public String description();

    ThreadGroup grp;

    AbstractLauncher() {
        super();

        grp = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = null;
        while ((parent = grp.getParent()) != null) {
            grp = parent;
        }
    }

    String[] tokenizeCommand(String command, char quote) {
        String quoteStr = String.valueOf(quote); // easier to deal with

        /*
         * Tokenize the command, respecting the given quote character.
         */
        StringTokenizer tokenizer = new StringTokenizer(command,
                                                        quote + " \t\r\n\f",
                                                        true);
        String quoted = null;
        String pending = null;
        List<String> tokenList = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (quoted != null) {
                if (token.equals(quoteStr)) {
                    tokenList.add(quoted);
                    quoted = null;
                } else {
                    quoted += token;
                }
            } else if (pending != null) {
                if (token.equals(quoteStr)) {
                    quoted = pending;
                } else if ((token.length() == 1) &&
                           Character.isWhitespace(token.charAt(0))) {
                    tokenList.add(pending);
                } else {
                    throw new InternalException("Unexpected token: " + token);
                }
                pending = null;
            } else {
                if (token.equals(quoteStr)) {
                    quoted = "";
                } else if ((token.length() == 1) &&
                           Character.isWhitespace(token.charAt(0))) {
                    // continue
                } else {
                    pending = token;
                }
            }
        }

        /*
         * Add final token.
         */
        if (pending != null) {
            tokenList.add(pending);
        }

        /*
         * An unclosed quote at the end of the command. Do an
         * implicit end quote.
         */
        if (quoted != null) {
            tokenList.add(quoted);
        }

        String[] tokenArray = new String[tokenList.size()];
        for (int i = 0; i < tokenList.size(); i++) {
            tokenArray[i] = tokenList.get(i);
        }
        return tokenArray;
    }

    protected VirtualMachine launch(String[] commandArray, String address,
                                    TransportService.ListenKey listenKey,
                                    TransportService ts)
                                    throws IOException, VMStartException {
        Helper helper = new Helper(commandArray, address, listenKey, ts);
        helper.launchAndAccept();

        VirtualMachineManager manager =
            Bootstrap.virtualMachineManager();

        return manager.createVirtualMachine(helper.connection(),
                                            helper.process());
    }

    /**
     * This class simply provides a context for a single launch and
     * accept. It provides instance fields that can be used by
     * all threads involved. This stuff can't be in the Connector proper
     * because the connector is a singleton and is not specific to any
     * one launch.
     */
    private class Helper {
        @SuppressWarnings("unused")
        private final String address;
        private TransportService.ListenKey listenKey;
        private TransportService ts;
        private final String[] commandArray;
        private Process process = null;
        private Connection connection = null;
        private IOException acceptException = null;
        private boolean exited = false;

        Helper(String[] commandArray, String address, TransportService.ListenKey listenKey,
            TransportService ts) {
            this.commandArray = commandArray;
            this.address = address;
            this.listenKey = listenKey;
            this.ts = ts;
        }

        String commandString() {
            String str = "";
            for (int i = 0; i < commandArray.length; i++) {
                if (i > 0) {
                    str += " ";
                }
                str += commandArray[i];
            }
            return str;
        }

        synchronized void launchAndAccept() throws
                                IOException, VMStartException {

            process = Runtime.getRuntime().exec(commandArray);

            Thread acceptingThread = acceptConnection();
            Thread monitoringThread = monitorTarget();
            try {
                while ((connection == null) &&
                       (acceptException == null) &&
                       !exited) {
                    wait();
                }

                if (exited) {
                    throw new VMStartException(
                        "VM initialization failed for: " + commandString(), process);
                }
                if (acceptException != null) {
                    // Rethrow the exception in this thread
                    throw acceptException;
                }
            } catch (InterruptedException e) {
                throw new InterruptedIOException("Interrupted during accept");
            } finally {
                acceptingThread.interrupt();
                monitoringThread.interrupt();
            }
        }

        Process process() {
            return process;
        }

        Connection connection() {
            return connection;
        }

        synchronized void notifyOfExit() {
            exited = true;
            notify();
        }

        synchronized void notifyOfConnection(Connection connection) {
            this.connection = connection;
            notify();
        }

        synchronized void notifyOfAcceptException(IOException acceptException) {
            this.acceptException = acceptException;
            notify();
        }

        Thread monitorTarget() {
            Thread thread = new Thread(grp, "launched target monitor") {
                public void run() {
                    try {
                        process.waitFor();
                        /*
                         * Notify waiting thread of VM error termination
                         */
                        notifyOfExit();
                    } catch (InterruptedException e) {
                        // Connection has been established, stop monitoring
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
            return thread;
        }

        Thread acceptConnection() {
            Thread thread = new Thread(grp, "connection acceptor") {
                public void run() {
                    try {
                        Connection connection = ts.accept(listenKey, 0, 0);
                        /*
                         * Notify waiting thread of connection
                         */
                        notifyOfConnection(connection);
                    } catch (InterruptedIOException e) {
                        // VM terminated, stop accepting
                    } catch (IOException e) {
                        // Report any other exception to waiting thread
                        notifyOfAcceptException(e);
                    }
                }
            };
            thread.setDaemon(true);
            thread.start();
            return thread;
        }
    }
}
