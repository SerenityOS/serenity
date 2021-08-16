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
 * @bug 4116437
 * @summary Distributed Garbage Collector Memory Leak
 *
 * @author Laird Dornin
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport:open
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary CheckLeaseLeak_Stub LeaseLeakClient LeaseLeak
 * @run main/othervm/timeout=240 CheckLeaseLeak
 *
 */

/**
 * A bug in sun.rmi.transport.DGCImp.checkLeases() results in memory
 * leak of LeaseInfo objects.
 *
 * In order to verify that this problem no longer exists, we create a
 * remote object and a serveral clients in different VMs. The clients
 * call a remote method on an exported object. This will cause the rmi
 * runtime to create several references (all with different vmids) to
 * the remote object.  Each vmid needs a seperate LeaseInfo object in
 * the object table target DGCImpl.leaseTable.  If the leak is fixed,
 * the leaseTable field will contain no objects.  We use reflection to
 * find the number of objects contained in this table.
 */

import java.rmi.*;
import java.rmi.server.*;
import sun.rmi.transport.*;
import sun.rmi.*;
import java.util.Map;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.registry.*;

public class CheckLeaseLeak extends UnicastRemoteObject implements LeaseLeak {
    public CheckLeaseLeak() throws RemoteException { }
    public void ping () throws RemoteException { }

    /**
     * Id to fake the DGC_ID, so we can later get a reference to the
     * DGCImpl in the object table.
     */
    private final static int DGC_ID = 2;

    private final static int ITERATIONS = 10;
    private final static int numberPingCalls = 0;
    private final static int CHECK_INTERVAL = 400;
    private final static int LEASE_VALUE = 20;

    public static void main (String[] args) {
        CheckLeaseLeak leakServer = null;
        int numLeft =0;

        /*
         * we want DGC to collect leases *quickly*
         * decrease the lease check interval
         */
        TestLibrary.setInteger("sun.rmi.dgc.checkInterval",
                               CHECK_INTERVAL);
        TestLibrary.setInteger("java.rmi.dgc.leaseValue",
                               LEASE_VALUE);

        try {
            Registry registry =
                TestLibrary.createRegistryOnEphemeralPort();
            int registryPort = TestLibrary.getRegistryPort(registry);

            leakServer = new CheckLeaseLeak();
            registry.rebind("/LeaseLeak", leakServer);

            /* create a bunch of clients in a *different* vm */
            for (int i = 0 ; i < ITERATIONS ; i ++ ) {
                System.err.println("Created client: " + i);

                JavaVM jvm = new JavaVM("LeaseLeakClient",
                                        " -Djava.security.policy=" +
                                        TestParams.defaultPolicy +
                                        " -Drmi.registry.port=" +
                                        registryPort,
                                        "");

                try {
                    if (jvm.execute() != 0) {
                        TestLibrary.bomb("Client process failed");
                    }
                } finally {
                    jvm.destroy();
                }
            }
            numLeft = getDGCLeaseTableSize();
            Thread.sleep(3000);

        } catch(Exception e) {
            TestLibrary.bomb("CheckLeaseLeak Error: ", e);
        } finally {
            if (leakServer != null) {
                TestLibrary.unexport(leakServer);
                leakServer = null;
            }
        }

        /* numLeft should be 2 - if 11 there is a problem. */
        if (numLeft > 2) {
            TestLibrary.bomb("Too many objects in DGCImpl.leaseTable: "+
                            numLeft);
        } else {
            System.err.println("Check leaseInfo leak passed with " +
                               numLeft
                                   + " object(s) in the leaseTable");
        }
    }

    /**
     * Obtain a reference to the main DGCImpl via reflection.  Extract
     * the DGCImpl using the ObjectTable and the well known ID of the
     * DGCImpl.
     */
    private static int getDGCLeaseTableSize () {
        int numLeaseInfosLeft = 0;

        /**
         * Will eventually be set to point at the leaseTable inside
         * DGCImpl.
         */
        Map leaseTable = null;
        final Remote[] dgcImpl = new Remote[1];
        Field f;

        try {
            f = (Field) java.security.AccessController.doPrivileged
                (new java.security.PrivilegedExceptionAction() {
                    public Object run() throws Exception {

                        ObjID dgcID = new ObjID(DGC_ID);

                        /*
                         * Construct an ObjectEndpoint containing DGC's
                         * ObjID.
                         */
                        Class oeClass =
                            Class.forName("sun.rmi.transport.ObjectEndpoint");
                        Class[] constrParams =
                            new Class[]{ ObjID.class, Transport.class };
                        Constructor oeConstructor =
                            oeClass.getDeclaredConstructor(constrParams);
                        oeConstructor.setAccessible(true);
                        Object oe =
                            oeConstructor.newInstance(
                                new Object[]{ dgcID, null });

                        /*
                         * Get Target that contains DGCImpl in ObjectTable
                         */
                        Class objTableClass =
                            Class.forName("sun.rmi.transport.ObjectTable");
                        Class getTargetParams[] = new Class[] { oeClass };
                        Method objTableGetTarget =
                            objTableClass.getDeclaredMethod("getTarget",
                                                            getTargetParams);
                        objTableGetTarget.setAccessible(true);
                        Target dgcTarget = (Target)
                            objTableGetTarget.invoke(null, new Object[]{ oe });

                        /* get the DGCImpl from its Target */
                        Method targetGetImpl =
                            dgcTarget.getClass().getDeclaredMethod
                            ("getImpl", null);
                        targetGetImpl.setAccessible(true);
                        dgcImpl[0] =
                            (Remote) targetGetImpl.invoke(dgcTarget, null);

                        /* Get the lease table from the DGCImpl. */
                        Field reflectedLeaseTable =
                            dgcImpl[0].getClass().getDeclaredField
                            ("leaseTable");
                        reflectedLeaseTable.setAccessible(true);

                        return reflectedLeaseTable;
                    }
            });

            /**
             * This is the leaseTable that will fill up with LeaseInfo
             * objects if the LeaseInfo memory leak is not fixed.
             */
            leaseTable = (Map) f.get(dgcImpl[0]);

            numLeaseInfosLeft = leaseTable.size();

        } catch(Exception e) {
            if (e instanceof java.security.PrivilegedActionException)
                e = ((java.security.PrivilegedActionException) e).
                    getException();
            TestLibrary.bomb(e);
        }

        return numLeaseInfosLeft;
    }
}
