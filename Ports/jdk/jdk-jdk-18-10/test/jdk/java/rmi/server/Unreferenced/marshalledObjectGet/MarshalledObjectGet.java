/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @summary When an object is retrieved from a MarshalledObject, callbacks
 * that were registered by objects in the graph for execution when the
 * unmarshalling is done should get executed.  This is verified by way of
 * an exported object's stub getting unmarshalled, and then garbage
 * collected, in which case the impl's unreferenced() method should get
 * invoked.
 * @author Peter Jones
 *
 * @build MarshalledObjectGet_Stub
 * @run main/othervm/timeout=120 MarshalledObjectGet
 */

import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

public class MarshalledObjectGet implements Remote, Unreferenced {

    private static final String BINDING = "MarshalledObjectGet";
    private static final long GC_INTERVAL = 6000;
    private static final long TIMEOUT = 50000;

    private Object lock = new Object();
    private boolean unreferencedInvoked;

    public void unreferenced() {
        System.err.println("unreferenced() method invoked");
        synchronized (lock) {
            unreferencedInvoked = true;
            lock.notify();
        }
    }

    public static void main(String[] args) {

        System.err.println(
            "\nTest to verify correction interaction of " +
            "MarshalledObject.get and DGC registration\n");

        /*
         * Set the interval that RMI will request for GC latency (before RMI
         * gets initialized and this property is read) to an unrealistically
         * small value, so that this test shouldn't have to wait too long.
         */
        System.setProperty("sun.rmi.dgc.client.gcInterval",
            String.valueOf(GC_INTERVAL));

        MarshalledObjectGet obj = new MarshalledObjectGet();

        try {
            Remote stub = UnicastRemoteObject.exportObject(obj);
            System.err.println("exported remote object");

            MarshalledObject mobj = new MarshalledObject(stub);
            Remote unmarshalledStub = (Remote) mobj.get();
            System.err.println("unmarshalled stub from marshalled object");

            synchronized (obj.lock) {
                obj.unreferencedInvoked = false;

                unmarshalledStub = null;
                System.gc();
                System.err.println("cleared unmarshalled stub");
                System.err.println("waiting for unreferenced() callback " +
                                   "(SHOULD happen)...");
                obj.lock.wait(TIMEOUT);

                if (obj.unreferencedInvoked) {
                    // TEST PASSED
                } else {
                    throw new RuntimeException(
                        "TEST FAILED: unrefereced() not invoked after " +
                        ((double) TIMEOUT / 1000.0) + " seconds");
                }
            }

            System.err.println("TEST PASSED");

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(
                    "TEST FAILED: unexpected exception: " + e.toString());
            }
        } finally {
            if (obj != null) {
                try {
                    UnicastRemoteObject.unexportObject(obj, true);
                } catch (Exception e) {
                }
            }
        }
    }
}
