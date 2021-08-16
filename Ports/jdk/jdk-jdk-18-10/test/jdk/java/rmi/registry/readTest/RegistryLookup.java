/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RegistryLookup {
    public static final int EXIT_FAIL = 1;

    public static void main(String args[]) throws Exception {
        Registry registry = null;
        int exit = 0;
        try {
            int port = Integer.valueOf(args[0]);

            testPkg.Server obj = new testPkg.Server();
            testPkg.Hello stub =
                    (testPkg.Hello) UnicastRemoteObject.exportObject(obj, 0);
            // Bind the remote object's stub in the registry
            registry = LocateRegistry.getRegistry(port);
            registry.bind("Hello", stub);
            System.err.println("Server ready");

            testPkg.Client client = new testPkg.Client(port);
            String testStubReturn = client.testStub();
            if(!testStubReturn.equals(obj.hello)) {
                throw new RuntimeException("Test Fails : "
                        + "unexpected string from stub call");
            }
            registry.unbind("Hello");
            System.out.println("Test passed");
        } catch (Exception ex) {
            exit = EXIT_FAIL;
            ex.printStackTrace();
        }
        // need to exit explicitly, and parent process uses exit value
        // to tell if the test passed.
        System.exit(exit);
    }
}
