/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4268258
 * @summary When a DGC dirty call fails, RMI's client-side DGC implementation
 * should attempt to retry the same dirty call a few times, at least until the
 * known lease for that endpoint has expired, instead of just giving up
 * renewing that lease at all after the first failure.
 * @author Peter Jones (inspired by Adrian Colley's test case in 4268258)
 *
 * @build RetryDirtyCalls RetryDirtyCalls_Stub
 * @run main/othervm RetryDirtyCalls
 */

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

interface Self extends Remote {
    Self getSelf() throws RemoteException;
}

public class RetryDirtyCalls implements Self, Unreferenced {

    /** how long we wait before declaring that this test has passed */
    private final static long TIMEOUT = 20000;

    /** true if this object's unreferenced method has been called */
    private boolean unreferenced = false;

    /**
     * Return this object.  The need for this method is explained below.
     */
    public Self getSelf() {
        return this;
    }

    public void unreferenced() {
        synchronized (this) {
            unreferenced = true;
            notifyAll();
        }
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4268258\n");

        /*
         * Set properties to tweak DGC behavior so that this test will execute
         * quickly: set the granted lease duration to 10 seconds, the interval
         * that leases are checked to 3 seconds.
         */
        System.setProperty("java.rmi.dgc.leaseValue", "10000");
        System.setProperty("sun.rmi.dgc.checkInterval", "3000");

        /*
         * Make idle connections time out almost instantly (0.1 seconds) so
         * that the DGC implementation will have to make a new connection for
         * each dirty call, thus going through the socket factory, where we
         * can easily cause the operation to fail.
         */
        System.setProperty("sun.rmi.transport.connectionTimeout", "100");

        RetryDirtyCalls impl = new RetryDirtyCalls();

        try {
            TestSF sf = new TestSF();
            RMISocketFactory.setSocketFactory(sf);

            /*
             * The stub returned by UnicastRemoteObject.exportObject() does
             * not participate in DGC, but it does allow us to invoke a method
             * on the remote object through RMI.  Therefore, we invoke the
             * getSelf() method through RMI, which returns an equivalent stub
             * that does participate in DGC.
             */
            Self stub = (Self) UnicastRemoteObject.exportObject(impl);
            Self dgcStub = stub.getSelf();
            stub = null;                // in case 4114579 has been fixed

            /*
             * Set the socket factory to cause 3 connections attempts in a row
             * to fail before allowing a connection to succeed, expecting the
             * client-side DGC implementation to make at least four attempts.
             */
            final int FLAKE_FACTOR = 3;
            sf.setFlakeFactor(FLAKE_FACTOR);

            long deadline = System.currentTimeMillis() + TIMEOUT;
            boolean unreferenced;

            synchronized (impl) {
                while (!(unreferenced = impl.unreferenced)) {
                    long timeToWait = deadline - System.currentTimeMillis();
                    if (timeToWait > 0) {
                        impl.wait(timeToWait);
                    } else {
                        break;
                    }
                }
            }

            if (unreferenced) {
                throw new RuntimeException("remote object unreferenced");
            }

            int createCount = sf.getCreateCount();
            if (createCount == 0) {
                throw new RuntimeException("test socket factory never used");
            } else if (createCount < (FLAKE_FACTOR + 3)) {
                /*
                 * The unreferenced method was not invoked for some reason,
                 * but the dirty calls were clearly not retried well enough.
                 */
                throw new RuntimeException(
                    "test failed because dirty calls not retried enough, " +
                    "but remote object not unreferenced");
            }

            System.err.println(
                "TEST PASSED: remote object not unreferenced");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        } finally {
            /*
             * When all is said and done, try to unexport the remote object
             * so that the VM has a chance to exit.
             */
            try {
                UnicastRemoteObject.unexportObject(impl, true);
            } catch (Exception e) {
            }
        }
    }
}

class TestSF extends RMISocketFactory {

    private int flakeFactor = 0;

    private int flakeState = 0;

    private int createCount = 0;

    public synchronized void setFlakeFactor(int newFlakeFactor) {
        flakeFactor = newFlakeFactor;
    }

    public synchronized int getCreateCount() {
        return createCount;
    }

    public synchronized Socket createSocket(String host, int port)
        throws IOException
    {
        createCount++;

        if (++flakeState > flakeFactor) {
            flakeState = 0;
        }

        if (flakeState == 0) {
            return new Socket(host, port);
        } else {
            throw new IOException("random network failure");
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
}
