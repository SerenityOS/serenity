/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug   6964714
 * @library /test/lib
 * @run main/othervm -Djava.net.preferIPv4Stack=true IPv4Only
 * @summary Test the networkinterface listing with java.net.preferIPv4Stack=true.
 */


import java.net.*;
import java.util.*;
import jdk.test.lib.net.IPSupport;

public class IPv4Only {
    public static void main(String[] args) throws Exception {
        if (IPSupport.hasIPv4()) {
            System.out.println("Testing IPv4");
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                   InetAddress hostAddr = addrs.nextElement();
                   if ( hostAddr instanceof Inet6Address ){
                        throw new RuntimeException( "NetworkInterfaceV6List failed - found v6 address " + hostAddr.getHostAddress() );
                   }
                }
            }
        } else {
            try {
                NetworkInterface.getNetworkInterfaces();
                throw new RuntimeException("NetworkInterface.getNetworkInterfaces() should throw SocketException");
            } catch (SocketException expected) {
                System.out.println("caught expected exception: " + expected);
            }

            try {
                NetworkInterface.networkInterfaces();
                throw new RuntimeException("NetworkInterface.networkInterfaces() should throw SocketException");
            } catch (SocketException expected) {
                System.out.println("caught expected exception: " + expected);
            }
        }
    }
}
