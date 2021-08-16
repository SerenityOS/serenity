/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4010355
 * @summary RemoteException should have server's stack trace
 *
 * @author Laird Dornin
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary ClientStackTrace MyRemoteObject_Stub
 * @run main/othervm/policy=security.policy/timeout=120 ClientStackTrace
 */

/*
 * This test ensures that the stack trace in a caught server side
 * remote exception contains the string "exceptionReceivedFromServer".
 */

import java.rmi.*;
import java.rmi.server.*;
import sun.rmi.transport.StreamRemoteCall;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

interface MyRemoteInterface extends Remote {
    void ping() throws RemoteException;
}

class MyRemoteObject extends UnicastRemoteObject
    implements MyRemoteInterface {

    public MyRemoteObject () throws RemoteException {}

    public void ping () throws RemoteException {
        throw new RemoteException("This is a test remote exception");
    }
}

public class ClientStackTrace {
    Object dummy = new Object();

    public static void main(String[] args) {

        TestLibrary.suggestSecurityManager("java.rmi.RMISecurityManager");

        Object dummy = new Object();
        MyRemoteObject myRobj = null;
        MyRemoteInterface myStub = null;

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(bos);

            System.err.println("\nRegression test for bug 4010355\n");

            myRobj = new MyRemoteObject();

            /* cause a remote exception to occur. */
            try {
                myStub = (MyRemoteInterface) RemoteObject.toStub(myRobj);
                myStub.ping();

            } catch (RemoteException re) {
                re.printStackTrace(ps);
                String trace = bos.toString();

                if (trace.indexOf("exceptionReceivedFromServer") <0 ) {
                    throw new RuntimeException("No client stack trace on " +
                                               "thrown remote exception");
                } else {
                    System.err.println("test passed with stack trace: " +
                                       trace);
                }
            }

            deactivate(myRobj);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("test failed");
            throw new RuntimeException(e.getMessage());
        } finally {
            myRobj = null;
            myStub = null;
        }
    }

    // make sure that the remote object goes away.
    static void deactivate(RemoteServer r) {
        // make sure that the object goes away
        try {
            System.err.println("deactivating object.");
            UnicastRemoteObject.unexportObject(r, true);
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
    }
}
