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

/*
 */

import java.rmi.*;
import sun.rmi.transport.*;
import java.io.*;
import java.lang.reflect.*;
import java.rmi.dgc.*;
import java.util.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class TestImpl extends RegistryRunner
    implements Test {
    static Thread locker = null;
    static TestImpl foo = null;
    static TestImpl bar = null;

    TestImpl() throws RemoteException {
    }

    public String echo(String msg) throws RemoteException {

        if (locker == null) {
            // hold the target if not already held
            locker = lockTargetExpireLeases(foo, DGCDeadLock.HOLD_TARGET_TIME);
        }
        return "Message received: " + msg;
    }

    static public void main(String[] args) {
        try {
            int registryPort = RegistryRunner.init(args);

            //export "Foo"
            foo = new TestImpl();
            Naming.rebind("rmi://:" +
                          registryPort
                          + "/Foo", foo);

            try {
                //export "Bar" after leases have been expired.
                bar = new TestImpl();
                Naming.rebind("rmi://localhost:" +
                              registryPort
                              + "/Bar", bar);
            } catch (Exception e) {
                throw new RemoteException(e.getMessage());
            }

            RegistryRunner.notify(registryPort);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    static Thread lockTargetExpireLeases(Remote toLock, int timeOut) {
        Thread t = new Thread((Runnable) new TargetLocker(toLock, timeOut));
        t.start();
        return t;
    }

    static class TargetLocker implements Runnable {

        Remote toLock = null;
        int timeOut = 0;

        TargetLocker(Remote toLock, int timeOut) {
            this.toLock = toLock;
            this.timeOut = timeOut;
        }

        public void run() {
            try {
                // give dgc dirty calls time to finish.
                Thread.currentThread().sleep(4000);

                java.security.AccessController.
                    doPrivileged(new LockTargetCheckLeases(toLock,
                                                           timeOut));

            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    static class LockTargetCheckLeases
        implements java.security.PrivilegedAction {

        Remote toLock = null;
        int timeOut = 0;

        LockTargetCheckLeases(Remote toLock, int timeOut) {
            this.toLock = toLock;
            this.timeOut = timeOut;
        }

        public Object run() {
            try {

                Class args[] = new Class[1];

                Class objTableClass = Class.forName
                    ("sun.rmi.transport.ObjectTable");

                /* get the Target that corresponds to toLock from the
                 * ObjectTable
                 */
                args[0] = Class.forName("java.rmi.Remote");
                Method objTableGetTarget =
                    objTableClass.getDeclaredMethod("getTarget", args );
                objTableGetTarget.setAccessible(true);

                Target lockTarget =
                    ((Target) objTableGetTarget.invoke
                     (null , new Object [] {toLock} ));

                // make sure the lease on this object has expired.
                expireLeases(lockTarget);

                // stop other threads from using the target for toLock.
                synchronized (lockTarget) {
                    System.err.println("Locked the relevant target, sleeping " +
                                       timeOut/1000 + " seconds");
                    Thread.currentThread().sleep(timeOut);
                    System.err.println("Target unlocked");
                }

            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            return null;
        }
    }

    /* leases have long values, so no dirty calls which would lock out
     * a clean call, but the leases need to expire anyway, so we do it
     * explicitly.
     */
    static void expireLeases(Target t) throws Exception {

        final Target target = t;

        java.security.AccessController.doPrivileged(

            //  put this into another class?
            new java.security.PrivilegedAction() {
            public Object run() {
                try {

                    Class DGCClass = Class.forName("sun.rmi.transport.DGCImpl");
                    Method getDGCImpl =
                        DGCClass.getDeclaredMethod("getDGCImpl", null );
                    getDGCImpl.setAccessible(true);

                    // make sure the lease on this object has expired.
                    DGC dgcImpl = ((DGC) getDGCImpl.invoke(null, null));

                    /* Get the lease table from the DGCImpl. */
                    Field reflectedLeaseTable =
                        dgcImpl.getClass().getDeclaredField("leaseTable");
                    reflectedLeaseTable.setAccessible(true);

                    Map leaseTable = (Map) reflectedLeaseTable.get(dgcImpl);

                    // dont really need this synchronization...
                    synchronized (leaseTable) {
                        Iterator en = leaseTable.values().iterator();
                        while (en.hasNext()) {
                            Object info = en.next();

                            /* Get the notifySet in the leaseInfo object. */
                            Field notifySetField =
                                info.getClass().getDeclaredField("notifySet");
                            notifySetField.setAccessible(true);
                            HashSet notifySet = (HashSet) notifySetField.get(info);

                            Iterator iter = notifySet.iterator();
                            while (iter.hasNext()) {
                                Target notified = (Target) iter.next();

                                if (notified == target) {

                                /* Get and set the expiration field from the info object. */
                                    Field expirationField = info.getClass().
                                        getDeclaredField("expiration");
                                    expirationField.setAccessible(true);
                                    expirationField.setLong(info, 0);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();
                    System.exit(1);
                }
                // no interesting return value for this privileged action
                return null;
            }
        });
    }
}
