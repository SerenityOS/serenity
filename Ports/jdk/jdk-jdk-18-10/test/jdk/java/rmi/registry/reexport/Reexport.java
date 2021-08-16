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

/* @test
 * @bug 4120329
 * @summary RMI registry creation is impossible if first attempt fails.
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm Reexport
 */

/*
 * If a VM could not create an RMI registry because the registry port
 * was already occupied by this or other processes, the next
 * time the VM tried to create a registry (after the other registry
 * was brought down) the attempt would fail.  The second try to create
 * a registry would fail because the registry ObjID would still be in
 * use when it should never have been allocated.
 *
 * The test creates this conflict starting a dummy tcp server and ensures
 * that a registry can still be created after the conflict is resolved.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Reexport {
    static public void main(String[] argv) throws IOException {

        for (int loop = 0; loop < 10; loop++) {
            System.err.println("\nat loop: " + loop);
            int port = -1;
            try (ServerSocketChannel server = ServerSocketChannel.open();) {
                server.bind(null);
                InetSocketAddress addr = (InetSocketAddress)server.getLocalAddress();
                port = addr.getPort();

                System.err.println("Creating duplicate registry, this should fail...");
                createReg(port, true);
            }
            try {
                if (createReg(port, false) == null) {
                    TestLibrary.bomb("Could not create registry on second try");
                }
                System.err.println("Test passed");
                return;
            } catch (Exception e) {
                String err = e.getMessage();
                if (err.contains("Address already in use")
                        || err.contains("Port already in use")) {
                    continue;
                }
                TestLibrary.bomb(e);
            }
        }
        TestLibrary.bomb("Test failed");
    }

    static Registry createReg(int port, boolean expectException) {
        Registry reg = null;

        try {
            reg = LocateRegistry.createRegistry(port);
            if (expectException) {
                TestLibrary.bomb("Registry is up, an Exception is expected!");
            }
        } catch (Throwable e) {
            if (expectException) {
                System.err.println("EXPECTING PORT IN USE EXCEPTION:");
                System.err.println(e.getMessage());
                e.printStackTrace();
            } else {
                TestLibrary.bomb((Exception) e);
            }
        }
        return reg;
    }
}
