/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4322806
 * @summary When an RMI (JRMP) connection is made to a TCP address that is
 * listening, so the connection is accepted, but the server never responds
 * to the initial JRMP handshake (nor does it terminate the connection),
 * the client should not hang forever; instead, it should throw an exception
 * after a reasonable timeout interval.  The exception should be a
 * java.rmi.ConnectException or ConnectIOException, not a MarshalException,
 * because it should be clear that no partial call execution has occurred at
 * this point (because no data for the invocation has yet been written).
 * @author Peter Jones
 *
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @run main/othervm HandshakeTimeout
 */

import java.net.ServerSocket;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class HandshakeTimeout {

    private static final int TIMEOUT = 10000;

    public static void main(String[] args) throws Exception {

        System.setProperty("sun.rmi.transport.tcp.handshakeTimeout",
                           String.valueOf(TIMEOUT / 2));

        /*
         * Listen on port, but never process connections made to it.
         */
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        /*
         * Attempt RMI call to port in separate thread.
         */
        Registry registry = LocateRegistry.getRegistry(port);
        Connector connector = new Connector(registry);
        Thread t = new Thread(connector);
        t.setDaemon(true);
        t.start();

        /*
         * Wait for call attempt to finished, and analyze result.
         */
        t.join(TIMEOUT);
        synchronized (connector) {
            if (connector.success) {
                throw new RuntimeException(
                    "TEST FAILED: remote call succeeded??");
            }
            if (connector.exception == null) {
                throw new RuntimeException(
                    "TEST FAILED: remote call did not time out");
            } else {
                System.err.println("remote call failed with exception:");
                connector.exception.printStackTrace();
                System.err.println();

                if (connector.exception instanceof MarshalException) {
                    throw new RuntimeException(
                        "TEST FAILED: MarshalException thrown, expecting " +
                        "java.rmi.ConnectException or ConnectIOException");
                } else if (connector.exception instanceof ConnectException ||
                           connector.exception instanceof ConnectIOException)
                {
                    System.err.println(
                        "TEST PASSED: java.rmi.ConnectException or " +
                        "ConnectIOException thrown");
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unexpected Exception thrown",
                        connector.exception);
                }
            }
        }
    }

    private static class Connector implements Runnable {

        private final Registry registry;

        boolean success = false;
        Exception exception = null;

        Connector(Registry registry) {
            this.registry = registry;
        }

        public void run() {
            try {
                registry.lookup("Dale Cooper");
                synchronized (this) {
                    success = true;
                }
            } catch (Exception e) {
                synchronized (this) {
                    exception = e;
                }
            }
        }
    }
}
