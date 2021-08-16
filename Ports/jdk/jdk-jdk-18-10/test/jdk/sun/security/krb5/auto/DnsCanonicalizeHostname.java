/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import sun.security.krb5.PrincipalName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * @test
 * @bug 8210821
 * @summary Support dns_canonicalize_hostname in krb5.conf
 * @library /test/lib
 * @compile -XDignore.symbol.file DnsCanonicalizeHostname.java
 * @run main jdk.test.lib.FileInstaller dns_canonicalize_hostname.hosts hosts
 * @run main/othervm -Djdk.net.hosts.file=hosts DnsCanonicalizeHostname none
 * @run main/othervm -Djdk.net.hosts.file=hosts DnsCanonicalizeHostname true
 * @run main/othervm -Djdk.net.hosts.file=hosts DnsCanonicalizeHostname false
 */
public class DnsCanonicalizeHostname {

    // In dns_canonicalize_hostname.hosts, all "dummy.example.com", "dummy",
    // and "bogus" are resolved to 127.0.0.1. Since "dummy.example.com" is on
    // the first line, it is returned at the reverse lookup.

    public static void main(String[] args) throws Exception {

        Files.write(Path.of("krb5.conf"), List.of(
                "[libdefaults]",
                "default_realm = R",
                args[0].equals("none")
                        ? "# empty line"
                        : "dns_canonicalize_hostname = " + args[0],
                "",
                "[realms]",
                "R = {",
                "    kdc = 127.0.0.1",
                "}"
        ));
        System.setProperty("java.security.krb5.conf", "krb5.conf");

        String n1 = new PrincipalName("host/dummy", PrincipalName.KRB_NT_SRV_HST)
                .getNameStrings()[1];
        String n2 = new PrincipalName("host/bogus", PrincipalName.KRB_NT_SRV_HST)
                .getNameStrings()[1];

        switch (args[0]) {
            case "none":
            case "true":
                Asserts.assertEQ(n1, "dummy.example.com");
                Asserts.assertEQ(n2, "bogus");
                break;
            case "false":
                Asserts.assertEQ(n1, "dummy");
                Asserts.assertEQ(n2, "bogus");
                break;
        }
    }
}
