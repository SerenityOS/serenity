/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4626311
 * @summary Protocol "rmi:" is used in contrary to spec.  The Naming.list
 * method should not append an "rmi:" scheme prefix to the URLs in the
 * array of Strings it returns.
 * @author Ann Wollrath
 *
 * @library ../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm RmiIsNoScheme
 */

import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;

public class RmiIsNoScheme implements Remote, Serializable {
    private RmiIsNoScheme() {}

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4626311\n");

        try {
            Registry registry = TestLibrary.createRegistryOnEphemeralPort();
            int registryPort = TestLibrary.getRegistryPort(registry);
            Naming.rebind("//:" + registryPort + "/RmiIsNoScheme",
                          new RmiIsNoScheme());
            String name = Naming.list("//:" + registryPort)[0];
            System.err.println("name = " + name);
            if (name.startsWith("rmi:", 0) == false) {
                System.err.println("TEST PASSED: rmi scheme not present");
            } else {
                throw new RuntimeException("TEST FAILED: rmi scheme present!");
            }
        } catch (Exception e) {
            TestLibrary.bomb(e);
        }
    }
}
