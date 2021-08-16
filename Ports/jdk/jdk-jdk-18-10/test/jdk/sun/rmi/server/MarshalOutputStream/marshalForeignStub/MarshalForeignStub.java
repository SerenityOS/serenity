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
 * @bug 4117427
 * @summary When marshalling an object that implements java.rmi.Remote,
 * but is not a RemoteStub and its corresponding stub class is not known
 * by the RMI runtime, RMI's MarshalOutputStream should assume that the
 * object is a proxy of some sort (like an RMI/IIOP stub) and simply
 * allow the object to be serialized.
 * @author Ann Wollrath
 *
 * @library ../../../../../java/rmi/testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Receiver MarshalForeignStub_Stub
 * @run main/othervm/policy=security.policy MarshalForeignStub
 */

import java.io.Serializable;
import java.rmi.*;
import java.rmi.server.*;

public class MarshalForeignStub
    extends UnicastRemoteObject
    implements Receiver
{

    public static class ForeignStub implements Remote, Serializable {
    }

    public MarshalForeignStub() throws RemoteException {
    }

    public void receive(Object obj) {
        System.err.println("+ receive(): received object " + obj);
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4117427\n");

        TestLibrary.suggestSecurityManager(null);

        System.err.println("Creating remote object.");
        MarshalForeignStub obj = null;
        try {
            obj = new MarshalForeignStub();
        } catch (RemoteException e) {
            TestLibrary.bomb(e);
        }

        try {
            Receiver stub = (Receiver) RemoteObject.toStub(obj);

            /*
             * Pass an instance of ForeignStub to the remote object.
             * This should succeed, because MarshalOutputStream now
             * allows objects that implement Remote, but not RemoteStub
             * to be serialized in an RMI call.
             */
            System.err.println(
                "Passing a foreign stub to remote object.");
            stub.receive(new ForeignStub());

            System.err.println("TEST SUCCEEDED");
        } catch (Exception e) {
            System.err.println("TEST FAILED: ");
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        } finally {
            try {
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (Throwable e) {
            }
        }
    }
}
