/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7003398
 * @run main/othervm -Djava.security.manager=allow Equals
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

public class Equals {

    static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    public static void main(String args[]) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream bufferedOut = new PrintStream(baos);

        Enumeration<NetworkInterface> nifs1 = NetworkInterface.getNetworkInterfaces();
        HashMap<String,Integer> hashes = new HashMap<>();
        HashMap<String,NetworkInterface> nicMap = new HashMap<>();

        while (nifs1.hasMoreElements()) {
            NetworkInterface ni = nifs1.nextElement();
            hashes.put(ni.getName(),ni.hashCode());
            nicMap.put(ni.getName(),ni);
            displayInterfaceInformation(ni, bufferedOut);
            bufferedOut.flush();
        }

        System.setSecurityManager(new SecurityManager());

        Enumeration<NetworkInterface> nifs2 = NetworkInterface.getNetworkInterfaces();
        while (nifs2.hasMoreElements()) {
            NetworkInterface ni = nifs2.nextElement();

            // JDK-8022963, Skip (Windows)Teredo Tunneling Pseudo-Interface
            String dName = ni.getDisplayName();
            if (isWindows && dName != null && dName.contains("Teredo"))
                continue;

            NetworkInterface niOrig = nicMap.get(ni.getName());

            int h = ni.hashCode();
            if (h != hashes.get(ni.getName())) {
                System.out.printf("%nSystem information:%n");
                System.out.printf("%s", baos.toString("UTF8"));
                System.out.printf("%nni.hashCode() returned %d, expected %d, for:%n",
                                  h, hashes.get(ni.getName()));
                displayInterfaceInformation(ni, System.out);
                throw new RuntimeException("Hashcodes different for " +
                        ni.getName());
            }
            if (!ni.equals(niOrig)) {
                System.out.printf("%nSystem information:%n");
                System.out.printf("%s", baos.toString("UTF8"));
                System.out.printf("%nExpected the following interfaces to be the same:%n");
                displayInterfaceInformation(niOrig, System.out);
                displayInterfaceInformation(ni, System.out);
                throw new RuntimeException("equality different for " +
                        ni.getName());
            }
        }
    }

    static void displayInterfaceInformation(NetworkInterface netint,
                                            PrintStream out) throws SocketException {
        out.printf("Display name: %s%n", netint.getDisplayName());
        out.printf("Name: %s%n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();

        for (InetAddress inetAddress : Collections.list(inetAddresses))
            out.printf("InetAddress: %s%n", inetAddress);

        out.printf("Up? %s%n", netint.isUp());
        out.printf("Loopback? %s%n", netint.isLoopback());
        out.printf("PointToPoint? %s%n", netint.isPointToPoint());
        out.printf("Supports multicast? %s%n", netint.supportsMulticast());
        out.printf("Virtual? %s%n", netint.isVirtual());
        out.printf("Hardware address: %s%n",
                    Arrays.toString(netint.getHardwareAddress()));
        out.printf("MTU: %s%n", netint.getMTU());
        out.printf("Index: %s%n", netint.getIndex());
        out.printf("%n");
     }
}

