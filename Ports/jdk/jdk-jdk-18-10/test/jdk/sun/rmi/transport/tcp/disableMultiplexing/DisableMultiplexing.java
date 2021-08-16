/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4183204
 * @summary The RMI runtime should fail to export a remote object on a TCP
 * port for an applet or application that does not have permission to listen
 * on that port, rather than engage in the deprecated "multiplexing protocol".
 * @author Peter Jones
 *
 * @build DisableMultiplexing_Stub
 * @run main/othervm -Djava.security.manager=allow DisableMultiplexing
 */

import java.rmi.*;
import java.rmi.server.*;

public class DisableMultiplexing implements Remote {

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4183204\n");

        System.err.println("Setting draconian security manager.");
        System.setSecurityManager(new SecurityManager() {
            public void checkListen(int port) {
                throw new SecurityException("THOU SHALT NOT LISTEN");
            }
        });

        System.err.println("Creating remote object.");
        DisableMultiplexing obj = new DisableMultiplexing();

        try {
            System.err.println("Attempting to export remote object.");
            UnicastRemoteObject.exportObject(obj);
            try {
                UnicastRemoteObject.unexportObject(obj, true);
            } catch (NoSuchObjectException e) {
            }
            throw new RuntimeException(
                "TEST FAILED: remote object successfully exported");
        } catch (SecurityException e) {
            System.err.println("TEST PASSED: ");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("TEST FAILED: " + e.toString());
        }
    }
}
