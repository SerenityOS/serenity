/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4742177 8019834
 * @summary Re-test IPv6 (and specifically MulticastSocket) with latest Linux & USAGI code
 */
import java.net.*;
import java.util.*;


public class BadIPv6Addresses {
    public static void main(String[] args) throws Exception {
        String[] badAddresses = new String[] {
            "0:1:2:3:4:5:6:7:8",        // too many :
            "0:1:2:3:4:5:6",            // not enough :
            "0:1:2:3:4:5:6:x",          // bad digits
            "0:1:2:3:4:5:6::7",         // adjacent :
            "0:1:2:3:4:5:6:789abcdef",  // too many digits
            "0:1:2:3::x",               // compressed, bad digits
            "0:1:2:::3",                // compressed, too many adjacent :
            "0:1:2:3::abcde",           // compressed, too many digits
            "0:1",                      // compressed, not enough :
            "0:0:0:0:0:x:10.0.0.1",     // with embeded ipv4, bad ipv6 digits
            "0:0:0:0:0:0:10.0.0.x",     // with embeded ipv4, bad ipv4 digits
            "0:0:0:0:0::0:10.0.0.1",    // with embeded ipv4, adjacent :
            "0:0:0:0:0:fffff:10.0.0.1", // with embeded ipv4, too many ipv6 digits
            "0:0:0:0:0:0:0:10.0.0.1",   // with embeded ipv4, too many :
            "0:0:0:0:0:10.0.0.1",       // with embeded ipv4, not enough :
            "0:0:0:0:0:0:10.0.0.0.1",   // with embeded ipv4, too many .
            "0:0:0:0:0:0:10.0.1",       // with embeded ipv4, not enough .
            "0:0:0:0:0:0:10..0.0.1",    // with embeded ipv4, adjacent .
            "::fffx:192.168.0.1",       // with compressed ipv4, bad ipv6 digits
            "::ffff:192.168.0.x",       // with compressed ipv4, bad ipv4 digits
            ":::ffff:192.168.0.1",      // with compressed ipv4, too many adjacent :
            "::fffff:192.168.0.1",      // with compressed ipv4, too many ipv6 digits
            "::ffff:1923.168.0.1",      // with compressed ipv4, too many ipv4 digits
            ":ffff:192.168.0.1",        // with compressed ipv4, not enough :
            "::ffff:192.168.0.1.2",     // with compressed ipv4, too many .
            "::ffff:192.168.0",         // with compressed ipv4, not enough .
            "::ffff:192.168..0.1"       // with compressed ipv4, adjacent .
        };

        List<String> failedAddrs = new ArrayList<String>();
        for (String addrStr : badAddresses) {
            try {
                InetAddress addr = InetAddress.getByName(addrStr);

                // it is an error if no exception
                failedAddrs.add(addrStr);
            } catch (UnknownHostException e) {
                // expected
            }
        }

        if (failedAddrs.size() > 0) {
            System.out.println("We should reject following ipv6 addresses, but we didn't:");
            for (String addr : failedAddrs) {
                System.out.println("\t" + addr);
            }
            throw new RuntimeException("Test failed.");
        }
    }
}
