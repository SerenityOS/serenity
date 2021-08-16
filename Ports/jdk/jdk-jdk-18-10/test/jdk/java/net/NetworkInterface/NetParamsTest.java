/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4691932
 * @summary Programmatic access to network parameters
 */

import java.net.*;
import java.util.Enumeration;

public class NetParamsTest {
    private static void printIF(NetworkInterface netif) throws SocketException {
        System.out.println(netif.getName() + " : ");
        System.out.println("\tStatus: " + (netif.isUp() ? " UP" : "DOWN"));
        byte[] mac = netif.getHardwareAddress();
        if (mac != null) {
            System.out.print("\tHardware Address: ");
            for (byte b : mac) {
                System.out.print(Integer.toHexString(b) + ":");
            }
            System.out.println();
        }
        System.out.println("\tLoopback: " + netif.isLoopback());
        System.out.println("\tPoint to Point: " + netif.isPointToPoint());
        System.out.println("\tVirtual: " + netif.isVirtual());
        if (netif.isVirtual()) {
            NetworkInterface parent = netif.getParent();
            String parentName = parent == null ? "null" : parent.getName();
            System.out.println("\tParent Interface: " + parentName);
        }
        System.out.println("\tMulticast: " + netif.supportsMulticast());

        System.out.println("\tMTU: " + netif.getMTU());
        System.out.println("\tBindings:");
        java.util.List<InterfaceAddress> binds = netif.getInterfaceAddresses();
        for (InterfaceAddress b : binds) {
            System.out.println("\t\t" + b);
        }
        Enumeration<NetworkInterface> ifs = netif.getSubInterfaces();
        while(ifs.hasMoreElements()) {
            NetworkInterface subif = ifs.nextElement();
            printIF(subif);
        }
    }

    public static void main(String[] args) throws Exception {
        Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
        while (ifs.hasMoreElements()) {
            NetworkInterface netif = ifs.nextElement();
            printIF(netif);
        }
     }
}
