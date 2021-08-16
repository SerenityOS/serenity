/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4482124
 * @summary Inet6Address.isLinkLocal() and isSiteLocal()
 *          implementation incorrect
 *
 */
import java.net.*;

public class IPv6AddressTypes {
    public static void main(String[] args) throws Exception {
        String[] goodlinklocal = {"fe80::a00:20ff:feae:45c9",
                                  "fe80::", "feb0::"};
        String[] badlinklocal = {"fec0::", "fe70::", "ff00::"};
        String[] goodsitelocal = {"fec0::a00:20ff:feae:45c9", "feff::"};
        String[] badsitelocal = {"fe80::", "fe00::", "ffc0::"};
        int i;

        for (i = 0; i < goodlinklocal.length; i++) {
            InetAddress ia = InetAddress.getByName(goodlinklocal[i]);
            if (!ia.isLinkLocalAddress()) {
                throw new RuntimeException("Link-local address check failed:"+ia);
            } else {
                System.out.println("succeed: "+ia);
            }
        }
        for (i = 0; i < badlinklocal.length; i++) {
            InetAddress ia = InetAddress.getByName(badlinklocal[i]);
            if (ia.isLinkLocalAddress()) {
                throw new RuntimeException("Link-local address check failed:"+ia);
            }
        }
        for (i = 0; i < goodsitelocal.length; i++) {
            InetAddress ia = InetAddress.getByName(goodsitelocal[i]);
            if (!ia.isSiteLocalAddress()) {
                throw new RuntimeException("Site-local address check failed:"+ia);
            }
        }
        for (i = 0; i < badsitelocal.length; i++) {
            InetAddress ia = InetAddress.getByName(badsitelocal[i]);
            if (ia.isSiteLocalAddress()) {
                throw new RuntimeException("Site-local address check failed:"+ia);
            }
        }
    }
}
