/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4457683
 * @summary After all of the remote objects (including a registry, if
 * applicable) that had been exported with a given
 * RMIServerSocketFactory value (including null) have been unexported,
 * the server socket created for the exports should be closed (so that
 * the local port is released).
 * @author Peter Jones
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm CloseServerSocket
 * @key intermittent
 */

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

public class CloseServerSocket implements Remote {
    private static final int PORT = TestLibrary.getUnusedRandomPort();

    private CloseServerSocket() { }

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4457683\n");

        verifyPortFree(PORT);
        Registry registry = LocateRegistry.createRegistry(PORT);
        System.err.println("- exported registry: " + registry);
        verifyPortInUse(PORT);
        UnicastRemoteObject.unexportObject(registry, true);
        System.err.println("- unexported registry");
        int tries = (int)TestLibrary.getTimeoutFactor();
        tries = Math.max(tries, 1);
        while (tries-- > 0) {
            Thread.sleep(1000);
            try {
                verifyPortFree(PORT);
                break;
            } catch (IOException ignore) { }
        }
        if (tries < 0) {
            throw new RuntimeException("time out after tries: " + tries);
        }


        /*
         * The follow portion of this test is disabled temporarily
         * because 4457683 was partially backed out because of
         * 6269166; for now, only server sockets originally opened for
         * exports on non-anonymous ports will be closed when all of
         * the corresponding remote objects have been exported.  A
         * separate bug will be filed to represent the remainder of
         * 4457683 for anonymous-port exports.
         */

//      SSF ssf = new SSF();
//      Remote impl = new CloseServerSocket();
//      Remote stub = UnicastRemoteObject.exportObject(impl, 0, null, ssf);
//      System.err.println("- exported object: " + stub);
//      UnicastRemoteObject.unexportObject(impl, true);
//      System.err.println("- unexported object");
//      synchronized (ssf) {
//          if (!ssf.serverSocketClosed) {
//              throw new RuntimeException("TEST FAILED: " +
//                                         "server socket not closed");
//          }
//      }

        System.err.println("TEST PASSED");
    }

    private static void verifyPortFree(int port) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        ss.close();
        System.err.println("- port " + port + " is free");
    }

    private static void verifyPortInUse(int port) throws IOException {
        try {
            verifyPortFree(port);
            throw new RuntimeException("port is not in use: " + port);
        } catch (BindException e) {
            System.err.println("- port " + port + " is in use");
            return;
        }
    }

    private static class SSF implements RMIServerSocketFactory {
        boolean serverSocketClosed = false;
        SSF() { };

        public ServerSocket createServerSocket(int port) throws IOException {
            return new SS(port);
        }

        private class SS extends ServerSocket {
            SS(int port) throws IOException {
                super(port);
                System.err.println("- created server socket: " + this);
            };

            public void close() throws IOException {
                synchronized (SSF.this) {
                    serverSocketClosed = true;
                    SSF.this.notifyAll();
                }
                System.err.println("- closing server socket: " + this);
                super.close();
            }
        }
    }
}
