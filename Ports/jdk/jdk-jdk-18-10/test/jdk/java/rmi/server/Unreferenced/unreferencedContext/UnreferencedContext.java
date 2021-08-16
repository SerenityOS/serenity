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
 * @bug 4171278
 * @summary A remote object's unreferenced() method should be invoked with the
 * context class loader set to the same context class loader that would be set
 * when remote calls for that object are being executed: the object's class's
 * class loader, or the context class loader set when the remote object was
 * exported, if it is a child of the remote object's class loader.
 * @author Peter Jones
 *
 * @bug 4214123
 * @summary Unreferenced.unreferenced(...) threads should run in the nonSystem group.
 *          To complete the fix for, 4182104, RMI unreferenced threads should also
 *          run in the nonSystem so that they do not need permissions to modify the
 *          system thread group.
 *
 * @author Laird Dornin
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary UnreferencedContext_Stub
 * @run main/othervm/timeout=120 -Djava.security.manager=allow UnreferencedContext
 */

import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class UnreferencedContext implements Remote, Unreferenced, Runnable {

    private final static String BINDING = "UnreferencedContext";
    private final static long GC_INTERVAL = 6000;
    private final static long TIMEOUT = 60000;

    private Object lock = new Object();
    private boolean unreferencedInvoked = false;
    private ClassLoader unreferencedContext;

    public void run() {
        System.err.println("unreferenced method created thread succesfully");
    }

    public void unreferenced() {
        // turn on security to ensure that the action below will not
        // require extra permissions
        System.setSecurityManager(new java.rmi.RMISecurityManager());

        // exercise functionality prohibited by 4214123
        (new Thread(this)).start();

        System.err.println("unreferenced() method invoked");
        synchronized (lock) {
            unreferencedInvoked = true;
            unreferencedContext =
                Thread.currentThread().getContextClassLoader();
            lock.notify();
        }
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4171278\n");

        /*
         * Set the interval that RMI will request for GC latency (before RMI
         * gets initialized and this property is read) to an unrealistically
         * small value, so that this test shouldn't have to wait too long.
         */
        System.setProperty("sun.rmi.dgc.client.gcInterval",
            String.valueOf(GC_INTERVAL));

        UnreferencedContext obj = new UnreferencedContext();

        try {
            /*
             * This little trick is necessary to make sure that the RMI server
             * threads for objects created on the default port get created
             * before we set our special context class loader, so that they
             * don't *accidentally* inherit it when making the unreferenced()
             * callback.
             */
            UnicastRemoteObject.exportObject(obj);
            UnicastRemoteObject.unexportObject(obj, true);

            /*
             * Now create special context class loader before exporting the
             * remote object for real, so that it should be set when the
             * object's unreferenced() method is called.
             */
            ClassLoader intendedContext = new URLClassLoader(new URL[0]);
            Thread.currentThread().setContextClassLoader(intendedContext);
            System.err.println(
                "created and set intended context class loader: " +
                intendedContext);

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
                /*
                 * This incantation seems sufficient to work around the
                 * ramifications of 4164696, so that this test will actually
                 * prove something useful about 1.2Beta4 or 1.2FCS before
                 * 4171278 was fixed.
                 */
                for (int i = 0; i < 10; i++) {
                    System.gc();
                    obj.lock.wait(TIMEOUT / 10);
                    if (obj.unreferencedInvoked) {
                        break;
                    }
                }

                if (obj.unreferencedInvoked) {
                    System.err.println(
                        "invoked with context class loader: " +
                        obj.unreferencedContext);

                    if (obj.unreferencedContext == intendedContext) {
                        System.err.println(
                            "TEST PASSED: unreferenced() invoked" +
                            " with intended context class loader");
                    } else {
                        throw new RuntimeException(
                            "TEST FAILED: unreferenced() invoked" +
                            " with incorrect context class loader");
                    }
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unreferenced() not invoked after " +
                        ((double) TIMEOUT / 1000.0) + " seconds or unreferenced failed to create a thread");
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
