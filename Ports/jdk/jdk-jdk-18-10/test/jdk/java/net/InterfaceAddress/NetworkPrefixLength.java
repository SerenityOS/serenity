/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6707289 7107883
 * @summary InterfaceAddress.getNetworkPrefixLength() does not conform to Javadoc
 */

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import static java.lang.System.out;

public class NetworkPrefixLength {
    static boolean passed = true;

    public static void main(String[] args) throws Exception {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();

        while (nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();
            for (InterfaceAddress iaddr : nic.getInterfaceAddresses()) {
                boolean valid = checkPrefix(iaddr);
                if (!valid) {
                    passed = false;
                    debug(nic.getName(), iaddr);
                }
                InetAddress ia = iaddr.getAddress();
                if (ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                    // assumption: prefix length will always be 8
                    if (iaddr.getNetworkPrefixLength() != 8) {
                        out.println("Expected prefix of 8, got " + iaddr);
                        passed = false;
                    }
                }
            }
        }

        if (!passed)
            throw new RuntimeException("Failed: some interfaces have invalid prefix lengths");
    }

    static boolean checkPrefix(InterfaceAddress iaddr) {
        InetAddress addr = iaddr.getAddress();

        if (addr instanceof Inet4Address)
            return checkIPv4PrefixLength(iaddr.getNetworkPrefixLength());
        else
            return checkIPv6PrefixLength(iaddr.getNetworkPrefixLength());
    }

    static boolean checkIPv4PrefixLength(int prefix) {
        if (prefix >=0 && prefix <= 32)
            return true;

        return false;
    }

    static boolean checkIPv6PrefixLength(int prefix) {
        if (prefix >=0 && prefix <= 128)
            return true;

        return false;
    }

    static void debug(String nicName, InterfaceAddress iaddr) {
        out.println("NIC " + nicName + " has an address with an invalid prefix length:\n" + iaddr);
    }
}

