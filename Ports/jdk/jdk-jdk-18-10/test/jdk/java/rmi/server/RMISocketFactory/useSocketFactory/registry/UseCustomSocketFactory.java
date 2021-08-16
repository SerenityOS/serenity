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

/* @test
 * @bug 4148850
 *
 * @summary synopsis: need to obtain remote references securely
 *
 * @author Laird Dornin; code borrowed from Ann Wollrath
 *
 * @library ../../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Compress Hello HelloImpl HelloImpl_Stub
 * @run main/othervm/policy=security.policy/timeout=240 UseCustomSocketFactory
 */

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/**
 * Test ensures that the rmiregistry is capable of running over customx
 * (i.e. compression) client and server socket factories.
 */
public class UseCustomSocketFactory {

    Hello hello = null;

    public static void main(String[] args) {

        Registry registry = null;
        HelloImpl impl = null;

        System.out.println("\nRegression test for bug 4148850\n");

        TestLibrary.suggestSecurityManager("java.rmi.RMISecurityManager");
        int registryPort = -1;

        try {
            impl = new HelloImpl();

            /* Make sure that the rmiregistry can communicate over a
             * custom socket.  Ensure that the functionality exists to
             * allow the rmiregistry to be secure.
             */
            registry = LocateRegistry.
                createRegistry(0,
                               new Compress.CompressRMIClientSocketFactory(),
                               new Compress.CompressRMIServerSocketFactory());
            registryPort = TestLibrary.getRegistryPort(registry);
            registry.rebind("/HelloServer", impl);
            checkStub(registry, "RMIServerSocket");

        } catch (Exception e) {
            TestLibrary.bomb("creating registry", e);
        }

        JavaVM serverVM = new JavaVM("HelloImpl",
                                     "-Djava.security.manager=allow" +
                                     " -Djava.security.policy=" +
                                     TestParams.defaultPolicy +
                                     " -Drmi.registry.port=" +
                                     registryPort,
                                     "");

        try {

            /*
             * spawn VM for HelloServer which will download a client socket
             * factory
             */
            serverVM.start();

            synchronized (impl) {

                System.out.println("waiting for remote notification");

                if (!HelloImpl.clientCalledSuccessfully) {
                    impl.wait(75 * 1000);
                }

                if (!HelloImpl.clientCalledSuccessfully) {
                    throw new RuntimeException("Client did not execute call in time...");
                }
            }

            System.err.println("\nRegression test for bug 4148850 passed.\n ");

        } catch (Exception e) {
            TestLibrary.bomb("test failed", e);

        } finally {
            serverVM.destroy();
            try {
                registry.unbind("/HelloServer");
            } catch (Exception e) {
                TestLibrary.bomb("unbinding HelloServer", e);
            }
            TestLibrary.unexport(registry);
            TestLibrary.unexport(impl);
            impl = null;
            registry = null;
        }
    }

    static void checkStub(Object stub, String toCheck) throws RemoteException {
        System.err.println("Ensuring that the stub contains a socket factory string: " +
                           toCheck);
        System.err.println(stub);
        if (stub.toString().indexOf(toCheck) < 0) {
            throw new RemoteException("RemoteStub.toString() did not contain instance of "
                                      + toCheck);
        }
    }
}
