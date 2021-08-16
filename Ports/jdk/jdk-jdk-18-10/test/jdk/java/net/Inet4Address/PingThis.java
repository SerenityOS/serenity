/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/* @test
 * @bug 7163874 8133015
 * @library /test/lib
 * @summary InetAddress.isReachable is returning false
 *          for InetAdress 0.0.0.0 and ::0
 * @run main PingThis
 * @run main/othervm -Djava.net.preferIPv4Stack=true PingThis
 */

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import jdk.test.lib.net.IPSupport;

public class PingThis {
    public static void main(String args[]) throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;
        }
        IPSupport.throwSkippedExceptionIfNonOperational();

        List<String> addrs = new ArrayList<String>();

        if (IPSupport.hasIPv4()) {
            addrs.add("0.0.0.0");
        }
        if (IPSupport.hasIPv6()) {
            addrs.add("::0");
        }

        for (String addr : addrs) {
            InetAddress inetAddress = InetAddress.getByName(addr);
            System.out.println("The target ip is "
                    + inetAddress.getHostAddress());
            boolean isReachable = inetAddress.isReachable(3000);
            System.out.println("the target is reachable: " + isReachable);
            if (isReachable) {
                System.out.println("Test passed ");
            } else {
                System.out.println("Test failed ");
                throw new Exception("address " + inetAddress.getHostAddress()
                        + " can not be reachable!");
            }
        }
    }
}
