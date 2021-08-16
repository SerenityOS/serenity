/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6442073
 * @summary Check getXXX methods for local/remote port/address/socketaddress
 *          match socket spec for unbound case
 */
import java.net.*;
import java.nio.channels.*;

public class UnboundSocketTests {

    static int failures = 0;

    static void check(String msg, Object actual, Object expected) {
        System.out.format("%s expected: %s, actual: %s", msg, expected, actual);
        if (actual == expected) {
            System.out.println(" [PASS]");
        } else {
            System.out.println(" [FAIL]");
            failures++;
        }
    }

    static void checkIsAnyLocalAddress(String msg, InetAddress actual) {
        System.out.format("%s actual: %s", msg, actual);
        if (actual.isAnyLocalAddress()) {
            System.out.println(" [PASS]");
        } else {
            System.out.println(" [FAIL]");
            failures++;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n-- SocketChannel --");

        SocketChannel sc = SocketChannel.open();
        try {
            check("getLocalPort()", sc.socket().getLocalPort(), -1);
            checkIsAnyLocalAddress("getLocalAddress()",
                sc.socket().getLocalAddress());
            check("getLocalSocketAddress()", sc.socket().getLocalSocketAddress(), null);

            check("getPort()", sc.socket().getPort(), 0);
            check("getInetAddress()", sc.socket().getInetAddress(), null);
            check("getRemoteSocketAddress()", sc.socket().getRemoteSocketAddress(), null);
        } finally {
            sc.close();
        }

        System.out.println("\n-- ServerSocketChannel --");

        ServerSocketChannel ssc = ServerSocketChannel.open();
        try {
            check("getLocalPort()", ssc.socket().getLocalPort(), -1);
            check("getInetAddress()", ssc.socket().getInetAddress(), null);
            check("getLocalSocketAddress()", ssc.socket().getLocalSocketAddress(), null);
        } finally {
            ssc.close();
        }

        System.out.println("\n-- DatagramChannel --");

        DatagramChannel dc = DatagramChannel.open();
        try {
            // not specified
            check("getLocalPort()", dc.socket().getLocalPort(), 0);

            checkIsAnyLocalAddress("getLocalAddress()",
                dc.socket().getLocalAddress());
            check("getLocalSocketAddress()", dc.socket().getLocalSocketAddress(), null);

            check("getPort()", dc.socket().getPort(), -1);
            check("getInetAddress()", dc.socket().getInetAddress(), null);
            check("getRemoteSocketAddress()", dc.socket().getRemoteSocketAddress(), null);
        } finally {
            dc.close();
        }

        if (failures > 0) {
            throw new RuntimeException(failures + " sub-tests(s) failed.");
        }

    }
}
