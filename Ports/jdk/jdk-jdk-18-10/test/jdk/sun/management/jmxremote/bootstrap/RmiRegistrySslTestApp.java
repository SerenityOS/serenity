/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import jdk.test.lib.Utils;


public class RmiRegistrySslTestApp {

    static final String ok = "OK: Found jmxrmi entry in RMIRegistry!";
    static final String ko = "KO: Did not find jmxrmi entry in RMIRegistry!";
    static final String ko2 = "KO: Did not get expected exception!";
    static final String okException = "OK: Got expected exception!";
    static final String koException = "KO: Got unexpected exception!";

    public static void main(String args[]) throws Exception {

        System.out.println("RmiRegistry lookup...");

        String testID = System.getProperty("testID");
        int port = Integer.parseInt(System.getProperty("test.rmi.port"));

        if ("Test1".equals(testID)) {
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                String[] list = registry.list();
                if ("jmxrmi".equals(list[0])) {
                    System.out.println(ok);
                } else {
                    System.out.println(ko);
                    throw new IllegalArgumentException(ko);
                }
            } catch (Exception e) {
                System.out.println(koException);
                e.printStackTrace(System.out);
                throw e;
            }
        }

        if ("Test2".equals(testID)) {
            try {
                Registry registry = LocateRegistry.getRegistry(port);
                String[] list = registry.list();
                throw new IllegalArgumentException(ko2);
            } catch (Exception e) {
                System.out.println(okException);
                e.printStackTrace(System.out);
                return;
            }
        }

        if ("Test3".equals(testID)) {
            try {
                Registry registry = LocateRegistry.getRegistry(
                    null, port, new SslRMIClientSocketFactory());
                String[] list = registry.list();
                if ("jmxrmi".equals(list[0])) {
                    System.out.println(ok);
                } else {
                    System.out.println(ko);
                    throw new IllegalArgumentException(ko);
                }
            } catch (Exception e) {
                System.out.println(koException);
                e.printStackTrace(System.out);
                throw e;
            }
        }
    }
}
