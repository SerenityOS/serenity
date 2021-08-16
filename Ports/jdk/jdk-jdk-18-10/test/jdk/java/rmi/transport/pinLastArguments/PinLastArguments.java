/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6332349
 * @summary Passing live remote references as part of the arguments of
 * a remote invocation should not cause an AssertionError (on the
 * second and subsequent attempts) when system assertions are enabled,
 * nor should it cause the references to be pinned until a subsequent
 * such remote invocation occurs (if the argument stream was not
 * released cleanly because of a marshalling failure).
 * @author Peter Jones
 *
 * @run main/othervm -esa PinLastArguments
 */

import java.io.NotSerializableException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.rmi.MarshalException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PinLastArguments {

    public interface Ping extends Remote {
        void ping(Object first, Object second) throws RemoteException;
    }

    private static class PingImpl implements Ping {
        PingImpl() { }
        public void ping(Object first, Object second) {
            System.err.println("ping invoked: " + first + ", " + second);
        }
    }

    public static void main(String[] args) throws Exception {
        System.err.println("\nRegression test for bug 6332349\n");

        Ping impl = new PingImpl();
        Reference<?> ref = new WeakReference<Ping>(impl);
        try {
            Ping stub = (Ping) UnicastRemoteObject.exportObject(impl, 0);
            Object notSerializable = new Object();
            stub.ping(impl, null);
            try {
                stub.ping(impl, notSerializable);
            } catch (MarshalException e) {
                if (e.getCause() instanceof NotSerializableException) {
                    System.err.println("ping invocation failed as expected");
                } else {
                    throw e;
                }
            }
        } finally {
            UnicastRemoteObject.unexportObject(impl, true);
        }
        impl = null;

        // Might require multiple calls to System.gc() for weak-references
        // processing to be complete. If the weak-reference is not cleared as
        // expected we will hang here until timed out by the test harness.
        while (true) {
            System.gc();
            Thread.sleep(20);
            if (ref.get() == null) {
                break;
            }
        }

        System.err.println("TEST PASSED");
    }
}
