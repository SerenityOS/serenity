/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4211906
 * @summary If the type of a parameter or return value in an RMI call can be
 * successfully downloaded by the receiving endpoint, then an array class with
 * that type as its element type should likewise be able to be successfully
 * downloaded.  This should be true regardless of how many dimensions the
 * array has.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Receiver DownloadArrayClass_Stub Foo
 * @run main/othervm/policy=security.policy DownloadArrayClass
 */

import java.lang.reflect.Array;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

public class DownloadArrayClass
    extends UnicastRemoteObject
    implements Receiver
{

    public DownloadArrayClass() throws RemoteException {
    }

    public void receive(Object obj) {
        System.err.println("+ receive(): received object " + obj);
    }

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4082868\n");

        URL remoteCodebase = null;
        try {
            remoteCodebase =
                TestLibrary.installClassInCodebase("Foo", "remote_codebase");
        } catch (MalformedURLException e) {
            TestLibrary.bomb(e);
        }

        System.err.println("Setting codebase property to: " + remoteCodebase);
        System.setProperty("java.rmi.server.codebase",
            remoteCodebase.toString());

        /*
         * Load Foo from a non-RMI class loader so that it won't be already
         * loaded by an RMI class loader in this VM (for whatever that's
         * worth), but with URLClassLoader so that they will be annotated
         * properly.
         */
        System.err.println("Creating class loader for remote codebase " +
            remoteCodebase);
        ClassLoader remoteCodebaseLoader =
            URLClassLoader.newInstance(new URL[] { remoteCodebase });

        TestLibrary.suggestSecurityManager(null);

        System.err.println("Creating remote object.");
        DownloadArrayClass obj = null;
        try {
            obj = new DownloadArrayClass();
        } catch (RemoteException e) {
            TestLibrary.bomb(e);
        }

        try {
            Receiver stub = (Receiver) RemoteObject.toStub(obj);

            /*
             * Load the downloadable class "Foo" to marshal over RMI calls
             * in various forms.
             */
            Class fooClass = remoteCodebaseLoader.loadClass("Foo");
            Object arg;

            /*
             * First, to establish that simple class downloading is working
             * properly, try marshalling a simple instance of Foo.
             */
            arg = fooClass.newInstance();
            System.err.println("Passing object of type " + arg.getClass());
            stub.receive(arg);

            /*
             * Second, try marshalling a one-dimensional array of element
             * type Foo.
             */
            arg = Array.newInstance(fooClass, 26);
            System.err.println("Passing object of type " + arg.getClass());
            stub.receive(arg);

            /*
             * Finally, try marshalling a multi-dimensional array with
             * Foo as the eventual element type.
             */
            arg = Array.newInstance(fooClass, new int[] { 1, 42, 0 });
            System.err.println("Passing object of type " + arg.getClass());
            stub.receive(arg);

            System.err.println("TEST PASSED: " +
                "arrays of downloaded classes successfully passed");

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
