/*
 * Copyright (c) 2002, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4513223
 * @summary After an instance of UnicastRemoteObject has been unexported,
 * if it gets marshalled, an InternalError should not be thrown; instead,
 * the marshalling should succeed.  Also, if its "local" LiveRef gets
 * marshalled, by way of the stub returned by RemoteObject.toStub, for
 * example, then that should also succeed, instead of throwing an
 * IOException (see fixes for bugids 4353388, 4017232).  This test
 * handles the case where the impl's RemoteRef is a UnicastServerRef2.
 * @author Peter Jones
 * @author Ann Wollrath
 *
 * @build MarshalAfterUnexport2 MarshalAfterUnexport2_Stub
 * @run main/othervm MarshalAfterUnexport2
 */

import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;

public class MarshalAfterUnexport2
    extends UnicastRemoteObject
    implements Receiver
{
    public MarshalAfterUnexport2() throws RemoteException {
        super(0, null, null);
    }

    public void receive(Remote obj) {
    }

    public static void main(String[] args) throws Exception {

        System.err.println("\nRegression test for bug 4513223\n");

        Remote impl2 = null;
        try {
            Remote impl = new MarshalAfterUnexport2();
            System.err.println(
                "created impl extending URO (with a UnicastServerRef2): " +
                impl);

            Receiver stub = (Receiver) RemoteObject.toStub(impl);
            System.err.println("stub for impl: " + stub);

            UnicastRemoteObject.unexportObject(impl, true);
            System.err.println("unexported impl");

            impl2 = new MarshalAfterUnexport2();
            Receiver stub2 = (Receiver) RemoteObject.toStub(impl2);

            System.err.println("marshalling unexported object:");
            MarshalledObject mobj = new MarshalledObject(impl);

            System.err.println("passing unexported object via RMI-JRMP:");
            stub2.receive(stub);

            System.err.println("TEST PASSED");
        } finally {
            if (impl2 != null) {
                try {
                    UnicastRemoteObject.unexportObject(impl2, true);
                } catch (Throwable t) {
                }
            }
        }
    }
}

interface Receiver extends Remote {
    void receive(Remote obj) throws RemoteException;
}
