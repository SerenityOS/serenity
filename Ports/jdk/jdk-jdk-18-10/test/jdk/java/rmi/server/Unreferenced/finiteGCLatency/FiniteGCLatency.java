/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4164696
 * @summary The local garbage collector needs to inspect a client VM's heap
 * often enough (even if the VM is idle) to detect unreachable live remote
 * references, so that their server VMs can be informed that the client VM
 * is no longer holding a reference; this facilitates the server VM invoking
 * the remote object's unreferenced() method (if present), garbage collecting
 * the remote object, and allowing the server VM to exit.  This test focuses
 * on the unreferenced() method being invoked: it tests that the callback
 * method will be invoked within a reasonable time after the remote object is
 * no longer remotely reachable.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary FiniteGCLatency_Stub
 * @run main/othervm/timeout=120 FiniteGCLatency
 */

import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class FiniteGCLatency implements Remote, Unreferenced {

    private final static String BINDING = "FiniteGCLatency";
    private final static long GC_INTERVAL = 6000;
    private final static long TIMEOUT = 50000;

    private Object lock = new Object();
    private boolean unreferencedInvoked = false;

    public void unreferenced() {
        System.err.println("unreferenced() method invoked");
        synchronized (lock) {
            unreferencedInvoked = true;
            lock.notify();
        }
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4164696\n");

        /*
         * Set the interval that RMI will request for GC latency (before RMI
         * gets initialized and this property is read) to an unrealistically
         * small value, so that this test shouldn't have to wait too long.
         */
        System.setProperty("sun.rmi.dgc.client.gcInterval",
            String.valueOf(GC_INTERVAL));

        FiniteGCLatency obj = new FiniteGCLatency();

        try {
            UnicastRemoteObject.exportObject(obj);
            System.err.println("exported remote object");
            Registry registry1 = TestLibrary.createRegistryOnEphemeralPort();
            int port = TestLibrary.getRegistryPort(registry1);
            System.err.println("created registry");

            Registry registry = LocateRegistry.getRegistry("", port);
            registry.bind(BINDING, obj);
            System.err.println("bound remote object in registry");

            synchronized (obj.lock) {
                registry.unbind(BINDING);
                System.err.println("unbound remote object from registry; " +
                    "waiting for unreferenced() callback...");
                obj.lock.wait(TIMEOUT);

                if (obj.unreferencedInvoked) {
                    System.err.println("TEST PASSED: unreferenced() invoked");
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unrefereced() not invoked after " +
                        ((double) TIMEOUT / 1000.0) + " seconds");
                }
            }

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(
                    "TEST FAILED: unexpected exception: " + e.toString());
            }
        } finally {
            /*
             * When all is said and done, try to unexport the remote object
             * so that the VM has a chance to exit.
             */
            try {
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (RemoteException e) {
            }
        }
    }
}
