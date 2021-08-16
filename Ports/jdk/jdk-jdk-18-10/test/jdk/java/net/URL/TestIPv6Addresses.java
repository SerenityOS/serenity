/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4451522 4460484
 * @run main/othervm -Djava.security.manager=allow TestIPv6Addresses
 * @summary URI and URL getHost() methods don't comform to RFC 2732
 */

// Run in othervm -Djava.security.manager=allow because the tests sets a SecurityManager

import java.net.*;

public class TestIPv6Addresses {
    public static void main(String[] args) {
        try {
        // testing InetAddress static constructors
        InetAddress ia1 = InetAddress.getByName("fe80::a00:20ff:feae:45c9");
        InetAddress ia2 = InetAddress.getByName("[fe80::a00:20ff:feae:45c9]");
        System.out.println("InetAddress: "+ia1+" , "+ia2);
        if (!ia1.equals(ia2)) {
            throw new RuntimeException("InetAddress.getByName failed for"+
                                       "literal IPv6 addresses");
        }
        // testing URL constructor, getHost and getAuthority
        URL u1 = new URL("http", "fe80::a00:20ff:feae:45c9", 80, "/index.html");
        URL u2 = new URL("http", "[fe80::a00:20ff:feae:45c9]", 80, "/index.html");
        if (!u1.equals(u2)) {
            throw new RuntimeException("URL constructor failed for"+
                                       "literal IPv6 addresses");
        }
        if (!u1.getHost().equals(u2.getHost()) ||
            !u1.getHost().equals("[fe80::a00:20ff:feae:45c9]")) {
            throw new RuntimeException("URL.getHost() failed for"+
                                       "literal IPv6 addresses");
        }
        if (!u1.getAuthority().equals("[fe80::a00:20ff:feae:45c9]:80")) {
            throw new RuntimeException("URL.getAuthority() failed for"+
                                       "literal IPv6 addresses");
        }

        // need to test URI as well

        // testing SocketPermission to see whether it handles unambiguous cases
        // testing getIP and getHost etc
        SocketPermission sp1 =
            new SocketPermission(u1.getHost()+":80-", "resolve");
        SocketPermission sp2 =
            new SocketPermission(ia1.getHostAddress()+":8080", "resolve");

        if (!sp1.implies(sp2)) {
            throw new RuntimeException("SocketPermission implies doesn't work"+
                                       " for literal IPv6 addresses");
        }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        // bug 4460484

        SecurityManager sm = new SecurityManager();
        String strAddr = "::FFFF:127.0.0.1.2";

        try {
            InetAddress addr = InetAddress.getByName(strAddr);
        } catch (UnknownHostException e) {
            // expected
        }
        System.setSecurityManager(sm);

        try {
            InetAddress addr = InetAddress.getByName(strAddr);
        } catch (java.security.AccessControlException e) {
            // expected
        } catch (UnknownHostException e) {
            // expected
        }

    }
}
