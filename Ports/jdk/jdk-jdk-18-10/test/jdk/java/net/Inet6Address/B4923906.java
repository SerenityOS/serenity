/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4923906
 * @summary  Inet6Address.getScopedInterface() returns a String instead of a NetworkInterface
 */

import java.net.*;
import java.util.*;

public class B4923906 {
    public static void main (String[] args) throws Exception {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface ifc = (NetworkInterface) e.nextElement();
            Enumeration addrs = ifc.getInetAddresses();
            System.out.println (ifc.getName() +": " + ifc.getDisplayName());
            while (addrs.hasMoreElements()) {
                InetAddress a = (InetAddress)addrs.nextElement();
                System.out.println ("\t"+a);
                if (a instanceof Inet6Address) {
                    Inet6Address ia6 = (Inet6Address) a;
                    Object o = ia6.getScopedInterface();
                    if (o instanceof String) {
                        throw new RuntimeException (
                           "Oops! This should be a NetworkInterface"
                        );
                    }
                }
            }
        }
    }
}
