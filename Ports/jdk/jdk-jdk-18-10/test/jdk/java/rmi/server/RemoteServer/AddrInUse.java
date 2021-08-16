/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4111507
 * @summary retryServerSocket should not retry on BindException
 * @author Ann Wollrath
 *
 * @run main/othervm AddrInUse
 */

import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;

public class AddrInUse implements Runnable {

    private static int port = -1;
    private static final long TIMEOUT = 10000;

    private boolean exportSucceeded = false;
    private Throwable exportException = null;

    public void run() {

        /*
         * Attempt to create (i.e. export) a registry on the port that
         * has already been bound, and record the result.
         */
        try {
            LocateRegistry.createRegistry(port);
            synchronized (this) {
                exportSucceeded = true;
                notifyAll();
            }
        } catch (Throwable t) {
            synchronized (this) {
                exportException = t;
                notifyAll();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 4111507\n");

        /*
         * Bind a server socket to a port.
         */
        ServerSocket server = new ServerSocket(0);
        port = server.getLocalPort();
        System.err.println("Created a ServerSocket on port " + port + "...");

        /*
         * Start a thread that creates a registry on the same port,
         * and analyze the result.
         */
        System.err.println("create a registry on the same port...");
        System.err.println("(should cause an ExportException)");
        AddrInUse obj = new AddrInUse();
        synchronized (obj) {
            (new Thread(obj, "AddrInUse")).start();

            /*
             * Don't wait forever (original bug is that the export
             * hangs).
             */
            obj.wait(TIMEOUT);

            if (obj.exportSucceeded) {
                throw new RuntimeException(
                    "TEST FAILED: export on already-bound port succeeded");
            } else if (obj.exportException != null) {
                obj.exportException.printStackTrace();
                if (obj.exportException instanceof ExportException) {
                    System.err.println("TEST PASSED");
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unexpected exception occurred",
                        obj.exportException);
                }
            } else {
                throw new RuntimeException("TEST FAILED: export timed out");
            }
        }
    }
}
