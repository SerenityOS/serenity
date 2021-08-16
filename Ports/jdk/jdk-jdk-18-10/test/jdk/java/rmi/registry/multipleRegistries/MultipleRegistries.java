/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4267864
 * @summary Can't run multiple registries in the same VM
 * @author Ann Wollrath
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm/timeout=240 MultipleRegistries
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MultipleRegistries implements RemoteInterface {

    private static final String NAME = "MultipleRegistries";

    public Object passObject(Object obj) {
        return obj;
    }

    public static void main(String[] args) throws Exception {

        RemoteInterface server = null;
        RemoteInterface proxy = null;

        try {
            System.err.println("export object");
            server = new MultipleRegistries();
            proxy =
                (RemoteInterface) UnicastRemoteObject.exportObject(server, 0);

            System.err.println("proxy = " + proxy);

            System.err.println("export registries");
            Registry registryImpl1 = TestLibrary.createRegistryOnUnusedPort();
            int port1 = TestLibrary.getRegistryPort(registryImpl1);
            Registry registryImpl2 = TestLibrary.createRegistryOnUnusedPort();
            int port2 = TestLibrary.getRegistryPort(registryImpl2);
            System.err.println("bind remote object in registries");
            Registry registry1 = LocateRegistry.getRegistry(port1);
            Registry registry2 = LocateRegistry.getRegistry(port2);

            registry1.bind(NAME, proxy);
            registry2.bind(NAME, proxy);

            System.err.println("lookup remote object in registries");

            RemoteInterface remote1 = (RemoteInterface) registry1.lookup(NAME);
            RemoteInterface remote2 = (RemoteInterface) registry2.lookup(NAME);

            System.err.println("invoke methods on remote objects");
            remote1.passObject(remote1);
            remote2.passObject(remote2);

            System.err.println("TEST PASSED");

        } finally {
            if (proxy != null) {
                UnicastRemoteObject.unexportObject(server, true);
            }
        }
    }
}


interface RemoteInterface extends Remote {
    Object passObject(Object obj) throws RemoteException;
}
