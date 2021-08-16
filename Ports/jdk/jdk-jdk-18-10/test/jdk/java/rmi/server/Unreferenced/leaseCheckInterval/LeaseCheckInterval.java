/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4285878
 * @summary When the "java.rmi.dgc.leaseValue" system property is set to a
 * value much lower than its default (10 minutes), then the server-side
 * user-visible detection of DGC lease expiration-- in the form of
 * Unreferenced.unreferenced() invocations and possibly even local garbage
 * collection (including weak reference notification, finalization, etc.)--
 * may be delayed longer than expected.  While this is not a spec violation
 * (because there are no timeliness guarantees for any of these garbage
 * collection-related events), the user might expect that an unreferenced()
 * invocation for an object whose last client has terminated abnorally
 * should occur on relatively the same time order as the lease value
 * granted.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary JavaVM LeaseCheckInterval_Stub SelfTerminator
 * @run main/othervm LeaseCheckInterval
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

public class LeaseCheckInterval implements Remote, Unreferenced {

    public static final String BINDING = "LeaseCheckInterval";
    private static final long LEASE_VALUE = 10000;
    private static final long TIMEOUT = 20000;

    private Object lock = new Object();
    private boolean unreferencedInvoked = false;

    public void unreferenced() {
        System.err.println("unreferenced() method invoked");
        synchronized (lock) {
            unreferencedInvoked = true;
            lock.notify();
        }
    }

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for bug 4285878\n");

        /*
         * Set the duration of leases granted to a very small value, so that
         * we can test if expirations are detected in a roughly comparable
         * time.
         */
        System.setProperty("java.rmi.dgc.leaseValue",
                           String.valueOf(LEASE_VALUE));

        LeaseCheckInterval obj = new LeaseCheckInterval();
        JavaVM jvm = null;

        try {
            UnicastRemoteObject.exportObject(obj);
            System.err.println("exported remote object");

            Registry localRegistry = TestLibrary.createRegistryOnEphemeralPort();
            int registryPort = TestLibrary.getRegistryPort(localRegistry);
            System.err.println("created local registry");

            localRegistry.bind(BINDING, obj);
            System.err.println("bound remote object in local registry");

            synchronized (obj.lock) {
                System.err.println("starting remote client VM...");
                jvm = new JavaVM("SelfTerminator", "-Drmi.registry.port=" +
                            registryPort, "");
                jvm.start();

                System.err.println("waiting for unreferenced() callback...");
                obj.lock.wait(TIMEOUT);

                if (obj.unreferencedInvoked) {
                    System.err.println("TEST PASSED: " +
                        "unreferenced() invoked in timely fashion");
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unreferenced() not invoked after " +
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
            if (jvm != null) {
                jvm.destroy();
            }
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
