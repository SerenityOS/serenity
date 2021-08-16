/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031111 8194486
 * @summary fix krb5 caddr
 * @library /test/lib
 * @compile -XDignore.symbol.file Addresses.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts Addresses
 */

import sun.security.krb5.Config;

import javax.security.auth.kerberos.KerberosTicket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class Addresses {

    public static void main(String[] args) throws Exception {

        KDC.saveConfig(OneKDC.KRB5_CONF, new OneKDC(null),
                "noaddresses = false",
                "extra_addresses = 10.0.0.10, 10.0.0.11 10.0.0.12");
        Config.refresh();

        KerberosTicket ticket =
                Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false)
                        .s().getPrivateCredentials(KerberosTicket.class)
                        .iterator().next();

        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetAddress extra1 = InetAddress.getByName("10.0.0.10");
        InetAddress extra2 = InetAddress.getByName("10.0.0.11");
        InetAddress extra3 = InetAddress.getByName("10.0.0.12");

        boolean loopbackFound = false;
        boolean extra1Found = false;
        boolean extra2Found = false;
        boolean extra3Found = false;
        boolean networkFound = false;

        for (InetAddress ia: ticket.getClientAddresses()) {
            System.out.println(ia);
            if (ia.equals(loopback)) {
                loopbackFound = true;
                System.out.println("  loopback found");
            } else if (ia.equals(extra1)) {
                extra1Found = true;
                System.out.println("  extra1 found");
            } else if (ia.equals(extra2)) {
                extra2Found = true;
                System.out.println("  extra2 found");
            } else if (ia.equals(extra3)) {
                extra3Found = true;
                System.out.println("  extra3 found");
            } else if (ia instanceof Inet4Address) {
                networkFound = true;
                System.out.println("  another address (" + ia +
                        "), assumed real network");
            }
        }

        if (!loopbackFound || !networkFound
                || !extra1Found || !extra2Found || !extra3Found ) {
            throw new Exception();
        }
    }
}
