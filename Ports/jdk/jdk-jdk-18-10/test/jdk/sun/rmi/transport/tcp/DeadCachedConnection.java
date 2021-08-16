/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4094891 8157777
 * @summary unable to retry call if cached connection to server is used
 * @library ../../../../java/rmi/testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary RegistryVM RegistryRunner
 * @run main/othervm DeadCachedConnection
 */

/* Fault: Cached connections used for remote invocations exhibited
 * failure (sudden EOF or a TCP-related exception) immediately on
 * sending a new request.  It was then impossible to tell whether the
 * connection had managed to transport the request before dying; even
 * deserialization of request arguments is non-idempotent in general.
 *
 * In fact, this problem cannot be solved generally without rewriting
 * the protocol.  For now, the common case is the closing of an idle
 * connection by a loaded/bored/dead server host.
 *
 * The fix is/was to trivially attempt to execute a non-blocking read
 * on the connection before reusing it, to see if an exception/EOF is
 * waiting for delivery.  This is a 99%/1% solution, but until the
 * great protocol rewrite, it's the best option.
 *
 * Reproducing is by establishing a connection to a registry and
 * killing/restarting that registry (this forces the TCP connection
 * to close).  The next call to the registry will use the (stale)
 * cached connection, and will fail without the bugfix.
 */

import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class DeadCachedConnection {

    static public void main(String[] argv)
        throws Exception {
        try {
            Registry reg = null;
            int port = makeRegistry(0);

            // Get a handle to the registry
            System.err.println ("Locating just-started registry...");
            try {
                reg = LocateRegistry.getRegistry(port);
            } catch (RemoteException e) {
                throw new InternalError ("Can't find registry after starting it.");
            }

            // Contact the registry by invoking something on it.
            System.err.println ("Connecting to registry...");
            String[] junk = reg.list();

            // Kill and restart the registry
            System.err.println("Killing registry...");
            killRegistry();
            System.err.println("Restarting registry...");
            makeRegistry(port);

            // Try again (this is the test)
            System.err.println("Trying to use registry in spite of stale cache...");
            junk = reg.list();

            System.err.println("Test succeeded.");
        } catch (Exception e) {
            TestLibrary.bomb(e);
        } finally {
            // dont leave the registry around to affect other tests.
            killRegistry();
        }
    }

    public static int makeRegistry(int port) {
        try {
            subreg = RegistryVM.createRegistryVM(System.out, System.err, "", port);
            subreg.start();
            int regPort = subreg.getPort();
            System.out.println("Starting registry on port " + regPort);
            return regPort;
        } catch (IOException e) {
            // one of these is summarily dropped, can't remember which one
            System.out.println ("Test setup failed - cannot run rmiregistry");
            TestLibrary.bomb("Test setup failed - cannot run test", e);
        }
        return -1;
    }

    private static RegistryVM subreg = null;

    public static void killRegistry() throws InterruptedException {
        if (subreg != null) {
            subreg.cleanup();
            subreg = null;
        }
    }
}
