/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4868820
 * @summary IPv6 support for Windows XP and 2003 server
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main ScopeTests
 */

import java.net.*;
import java.util.*;

public class ScopeTests extends Tests {

    public static void main (String[] args) throws Exception {
        checkDebug (args);
        simpleTests();
        complexTests();
        System.out.println ("Tests passed: OK");
    }

    static void sassert (boolean condition, String msg) throws Exception {
        if (!condition) {
            throw new Exception (msg);
        }
    }

    static void checkAddress (String a, int numeric) throws Exception {
        Inet6Address addr = (Inet6Address) InetAddress.getByName (a);
        if (addr.getScopeId () != numeric) {
            throw new Exception ("wroing numeric scopeid");
        }
    }

    static void checkAddress (String a, String str) throws Exception {
        Inet6Address addr = (Inet6Address) InetAddress.getByName (a);
        if (!addr.getScopedInterface().getName().equals (str)) {
            throw new Exception ("wroing scoped interface name");
        }
    }

    /* These tests check generic API functionality that is not
     * dependent on scoping being implemented in the platform
     */
    static void simpleTests () throws Exception {
        checkAddress ("fe80::%1", 1);
        checkAddress ("fe80::1%1", 1);
        checkAddress ("fec0::1:a00:20ff:feed:b08d%0", 0);
        checkAddress ("fec0::1:a00:20ff:feed:b08d%1", 1);
        checkAddress ("fec0::1:a00:20ff:feed:b08d%99", 99);
        checkAddress ("fe80::", 0);
        checkAddress ("fec0::1:a00:20ff:feed:b08d", 0);
        checkAddress ("fec0::1:a00:20ff:feed:b08d", 0);
        checkAddress ("fec0::1:a00:20ff:feed:b08d", 0);
    }

    /* These tests check the NetworkInterfaces for the actual scopeids
     * configured on the system and do tests using that information
     */
    static void complexTests () throws Exception {
        dprintln ("ComplexTests");
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface nif = (NetworkInterface)e.nextElement();
            String name = nif.getName();
            Enumeration addrs = nif.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = (InetAddress) addrs.nextElement();
                dprintln ("ComplexTests: "+addr);
                if (addr instanceof Inet6Address) {
                    Inet6Address ia6 = (Inet6Address) addr;
                    if (ia6.getScopeId() != 0) {
                        System.out.println ("Testing interface: " + name +
                                            " and address: " + ia6);
                        ctest1 (name, ia6);
                        ctest2 (name, ia6);
                    } else {
                        System.out.println ("Interface: " + name +
                                            " Address: "+ ia6 +
                                            " does not support scope");
                    }
                }
            }
        }
    }


   /* check the scoped name on the Inet6Address is the same as
    * the interface it is attached to
    */
    static void ctest1 (String ifname, Inet6Address ia6) throws Exception {
        System.out.println ("ctest1:" + ia6);
        String s = ia6.getScopedInterface().getName();
        int scope = ia6.getScopeId();
        sassert (ifname.equals (s), "ctest1:"+ifname+":"+s);

    }

    /* create an Inet6Adddress by all three methods using the ifname
     * compare the numeric scope id with that of the Inet6Address passed in
     */
    static void ctest2 (String ifname, Inet6Address ia6) throws Exception {
        System.out.println ("ctest2:" + ia6);
        String s = ia6.getScopedInterface().getName();
        int scope = ia6.getScopeId();
        String s1 = ia6.getHostAddress();
        if (s1.indexOf('%') != -1) {
            s1 = s1.substring (0, s1.indexOf ('%'));
        }
        Inet6Address add = (Inet6Address) InetAddress.getByName (s1+"%"+ifname);
        sassert (add.getScopeId() == scope, "ctest2:1:" +scope);
    }
}
