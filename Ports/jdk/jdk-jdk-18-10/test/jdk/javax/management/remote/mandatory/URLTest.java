/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5057532
 * @summary Tests that host names are parsed correctly in URLs
 * @author Eamonn McManus
 *
 * @run clean URLTest
 * @run build URLTest
 * @run main URLTest
 */

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URI;
import javax.management.remote.JMXServiceURL;

public class URLTest {
    private static final String[] good = {
        "",
        "a",
        "a.b",
        "a.b.c.d.e.f.g",
        "aaa.bbb",
        "a-a.b-b",
        "a-a",
        "a--b",
        "1.2.3.4",
        "1.2.3.x",
        "111.222.222.111",
        "1",
        "23skiddoo",
        "23skiddoo.sfbay",
        "a1.b2",
        "1234.sfbay",
        "[::]",
        "[ffff::ffff]",
    };
    private static final String[] bad = {
        "-a",
        "a-",
        "-",
        "_",
        "a_b",
        "a_b.sfbay",
        ".",
        "..",
        ".a",
        "a.",
        "a..",
        "a..b",
        ".a.b",
        "a.b.",
        "a.b..",
        "1.2",
        "111.222.333.444",
        "a.23skiddoo",
        "[:::]",
        "[:]",
    };

    public static void main(String[] args) throws Exception {
        System.out.println("Testing that JMXServiceURL accepts the same " +
                           "hosts as java.net.URI");
        System.out.println("(Except that it allows empty host names and " +
                           "forbids a trailing \".\")");
        System.out.println();

        int failures = 0;

        for (int pass = 1; pass <= 2; pass++) {
            final boolean accept = (pass == 1);
            System.out.println("  Hosts that should " +
                               (accept ? "" : "not ") + "work");
            String[] hosts = accept ? good : bad;

            for (int i = 0; i < hosts.length; i++) {
                final String host = hosts[i];
                System.out.print("    " + host + ": ");

                boolean jmxAccept = true;
                try {
                    new JMXServiceURL("rmi", hosts[i], 0);
                } catch (MalformedURLException e) {
                    jmxAccept = false;
                }

                boolean uriAccept;
                try {
                    final URI uri = new URI("http://" + host + "/");
                    uriAccept = (uri.getHost() != null);
                } catch (URISyntaxException e) {
                    uriAccept = false;
                }

                final int len = host.length();
                if (accept != uriAccept && len != 0 &&
                    !(len > 1 && host.charAt(len - 1) == '.'
                      && host.charAt(len - 2) != '.')) {
                    // JMXServiceURL allows empty host name; also
                    // java.net.URI allows trailing dot in hostname,
                    // following RFC 2396, but JMXServiceURL doesn't,
                    // following RFC 2609
                    System.out.println("TEST BUG: URI accept=" + uriAccept);
                    failures++;
                } else {
                    if (jmxAccept == accept)
                        System.out.println("OK");
                    else {
                        System.out.println("FAILED");
                        failures++;
                    }
                }
            }

            System.out.println();
        }

        if (failures == 0)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILURES: " + failures);
            System.exit(1);
        }
    }
}
