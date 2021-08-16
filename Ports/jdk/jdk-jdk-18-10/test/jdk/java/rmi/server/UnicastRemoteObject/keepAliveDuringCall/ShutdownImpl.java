/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ShutdownImpl implements Shutdown {

    private static Remote impl;         // rooted here to prevent GC

    private final ShutdownMonitor monitor;

    private ShutdownImpl(ShutdownMonitor monitor) {
        this.monitor = monitor;
    }

    public void shutdown() {
        try {
            System.err.println(
                "(ShutdownImpl.shutdown) shutdown method invoked:");

            UnicastRemoteObject.unexportObject(this, true);
            System.err.println(
                "(ShutdownImpl.shutdown) shutdown object unexported");

            Thread.sleep(500);
            System.err.println("(ShutDownImpl.shutdown) FEE");
            Thread.sleep(500);
            System.err.println("(ShutDownImpl.shutdown) FIE");
            Thread.sleep(500);
            System.err.println("(ShutDownImpl.shutdown) FOE");
            Thread.sleep(500);
            System.err.println("(ShutDownImpl.shutdown) FOO");

            monitor.declareStillAlive();
            System.err.println("(ShutDownImpl.shutdown) still alive!");
        } catch (Exception e) {
            throw new RuntimeException(
                "unexpected exception occurred in shutdown method", e);
        }
    }

    public static void main(String[] args) {
        try {
            int registryPort = Integer.parseInt(System.getProperty("rmi.registry.port"));
            Registry registry =
                LocateRegistry.getRegistry("", registryPort);
            ShutdownMonitor monitor = (ShutdownMonitor)
                registry.lookup(KeepAliveDuringCall.BINDING);
            System.err.println("(ShutdownImpl) retrieved shutdown monitor");

            impl = new ShutdownImpl(monitor);
            Shutdown stub = (Shutdown) UnicastRemoteObject.exportObject(impl);
            System.err.println("(ShutdownImpl) exported shutdown object");

            monitor.submitShutdown(stub);
            System.err.println("(ShutdownImpl) submitted shutdown object");

        } catch (Exception e) {
            System.err.println("(ShutdownImpl) TEST SUBPROCESS FAILURE:");
            e.printStackTrace();
        }
    }
}
