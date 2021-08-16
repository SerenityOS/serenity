/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test 1.1 05/01/05
 * @bug 6206527
 * @summary "cannot assign address" when binding ServerSocket on Suse 9
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main B6206527
 */

import java.net.*;
import java.util.*;
import jdk.test.lib.NetworkConfiguration;

public class B6206527 {

    public static void main (String[] args) throws Exception {
        Inet6Address addr = getLocalAddr();
        if (addr == null) {
            System.out.println("Could not find a link-local address");
            return;
        }

        try (ServerSocket ss = new ServerSocket()) {
            System.out.println("trying LL addr: " + addr);
            ss.bind(new InetSocketAddress(addr, 0));
        }

        // need to remove the %scope suffix
        addr = (Inet6Address) InetAddress.getByAddress (
            addr.getAddress()
        );

        try (ServerSocket ss = new ServerSocket()) {
            System.out.println("trying LL addr: " + addr);
            ss.bind(new InetSocketAddress(addr, 0));
        }
    }

    public static Inet6Address getLocalAddr() throws Exception {
        Optional<Inet6Address> oaddr = NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(a -> a.isLinkLocalAddress())
                .findFirst();

        return oaddr.orElseGet(() -> null);
    }
}
