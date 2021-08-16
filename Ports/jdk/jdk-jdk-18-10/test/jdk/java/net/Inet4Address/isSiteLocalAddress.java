/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4670299
 * @summary Inet4Address.isSiteLocalAddress() is broken against RFC 1918
*/
import java.net.*;
import java.util.Vector;
import java.util.Iterator;

public class isSiteLocalAddress {
    public static void main(String[] args) throws Exception {
        String[][] addrs =
        {{"9.255.255.255", "false"}, {"10.0.0.0", "true"},
         {"10.255.255.255", "true"}, {"11.0.0.0", "false"},
         {"172.15.255.255", "false"}, {"172.16.0.0", "true"},
         {"172.30.1.2", "true"}, {"172.31.255.255", "true"},
         {"172.32.0.0", "false"}, {"192.167.255.255", "false"},
         {"192.168.0.0", "true"}, {"192.168.255.255", "true"},
         {"192.169.0.0", "false"}};

        Vector v = new Vector();
        for (int i = 0; i < addrs.length; i++) {
            InetAddress addr = InetAddress.getByName(addrs[i][0]);
            boolean result = new Boolean(addrs[i][1]).booleanValue();
            if (addr.isSiteLocalAddress() != result) {
                v.add(addrs[i]);
            }
        }
        Iterator itr = v.iterator();
        while (itr.hasNext()) {
            String[] entry = (String[]) itr.next();
            System.out.println(entry[0] +" should return "+entry[1]
                               + " when calling isSiteLocalAddress()");
        }
        if (v.size() > 0) {
            throw new RuntimeException("InetAddress.isSiteLocalAddress() test failed");
        }
    }
}
