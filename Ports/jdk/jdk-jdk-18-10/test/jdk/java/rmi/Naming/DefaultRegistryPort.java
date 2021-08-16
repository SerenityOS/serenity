/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4251878
 * @summary change in default URL port causes regression in java.rmi.Naming
 * @author Dana Burns
 * @library ../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm DefaultRegistryPort
 * @key intermittent
 */

/*
 * Ensure that the default registry port for java.rmi.Naming URLs
 * is 1099. Test creates a registry on port 1099 and then does a
 * lookup with a Naming URL that uses the default port.
 */

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DefaultRegistryPort {

    public static void main(String args[]) throws Exception {

        Registry registry = null;
        System.err.println("Starting registry on default port REGISTRY_PORT="
                           + Registry.REGISTRY_PORT);
        final int NUM = 10;
        for (int loop = 0; loop < NUM; loop++) {
            System.err.println("in loop: " + loop);
            try {
                registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                System.err.println("Created registry=" + registry);
                break;
            } catch(java.rmi.RemoteException e) {
                String err = e.getMessage();
                if (err.contains("Address already in use")
                        || err.contains("Port already in use")) {
                    try {
                        Thread.sleep((long)(TestLibrary.getTimeoutFactor() * 100));
                    } catch (InterruptedException ignore) { }
                    continue;
                }
                TestLibrary.bomb(e);
            }
        }
        if (registry == null) {
            throw new RuntimeException("can not create registry at "
                  + Registry.REGISTRY_PORT + " after trying " + NUM + "times");
        }

        registry.rebind("myself", registry);
        Remote myself = Naming.lookup("rmi://localhost/myself");
        System.err.println("Test PASSED");
    }
}
