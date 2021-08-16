/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186576 8194486
 * @summary KerberosTicket does not properly handle renewable tickets
 *          at the end of their lifetime
 * @library /test/lib
 * @compile -XDignore.symbol.file NullRenewUntil.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Dtest.set.null.renew NullRenewUntil
 */

import jdk.test.lib.Asserts;
import sun.security.krb5.Config;

import javax.security.auth.kerberos.KerberosTicket;

public class NullRenewUntil {

    public static void main(String[] args) throws Exception {

        OneKDC kdc = new OneKDC(null);

        KDC.saveConfig(OneKDC.KRB5_CONF, kdc,
                "ticket_lifetime = 10s",
                "renew_lifetime = 11s");
        Config.refresh();

        KerberosTicket ticket = Context
                .fromUserPass(OneKDC.USER, OneKDC.PASS, false).s()
                .getPrivateCredentials(KerberosTicket.class).iterator().next();

        System.out.println(ticket);
        Asserts.assertTrue(ticket.getRenewTill() != null, ticket.toString());

        Thread.sleep(2000);

        ticket.refresh();
        System.out.println(ticket);
        Asserts.assertTrue(ticket.getRenewTill() == null, ticket.toString());

        Thread.sleep(2000);
        ticket.refresh();
        System.out.println(ticket);
    }
}
