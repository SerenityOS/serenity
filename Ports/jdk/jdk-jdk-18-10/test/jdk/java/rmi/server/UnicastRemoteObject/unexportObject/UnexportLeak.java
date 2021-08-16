/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4331349
 * @summary synopsis: unexporting doesn't guarantee that DGC will
 * let go of remote object
 *
 * @author Ann Wollrath
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary UnexportLeak_Stub Ping
 * @run main/othervm UnexportLeak
 */

import java.lang.ref.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

public class UnexportLeak implements Ping {
    public void ping() {
    }

    public static void main(String[] args) {
        try {
            System.err.println("\nRegression test for bug 4331349\n");
            Registry registry = TestLibrary.createRegistryOnEphemeralPort();
            int registryPort = TestLibrary.getRegistryPort(registry);
            Remote obj = new UnexportLeak();
            WeakReference wr = new WeakReference(obj);
            UnicastRemoteObject.exportObject(obj);
            LocateRegistry.getRegistry(registryPort).rebind("UnexportLeak", obj);
            UnicastRemoteObject.unexportObject(obj, true);
            obj = null;
            flushRefs();
            if (wr.get() != null) {
                System.err.println("FAILED: unexported object not collected");
                throw new RuntimeException(
                    "FAILED: unexported object not collected");
            } else {
                System.err.println("PASSED: unexported object collected");
            }
        } catch (RemoteException e) {
            System.err.println(
                "FAILED: RemoteException encountered: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("FAILED: RemoteException encountered");
        }
    }

    /**
     * Force desparate garbage collection so that all WeakReference instances
     * will be cleared.
     */
    private static void flushRefs() {
        java.util.Vector chain = new java.util.Vector();
        try {
            while (true) {
                int[] hungry = new int[65536];
                chain.addElement(hungry);
            }
        } catch (OutOfMemoryError e) {
        }
    }
}
