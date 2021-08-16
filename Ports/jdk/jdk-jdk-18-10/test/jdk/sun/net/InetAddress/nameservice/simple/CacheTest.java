/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4292867
 * @summary Check that InetAddress doesn't continue to throw UHE
 *          after the name service has recovered and the negative ttl
 *          on the initial lookup has expired.
 * @run main/othervm/timeout=200 -Djdk.net.hosts.file=CacheTestHosts
 *      CacheTest
 */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class CacheTest {


    public static void main(String args[]) throws Exception {

        /*
         * First check the ttl on negative lookups is in the <15 second
         * range. If the ttl is <=0 it means we cache forever or always
         * consult the name service. For ttl > 15 the test would take
         * too long so we skip it (need to coordinate jtreg timeout
         * with negative ttl)
         */
        String ttlProp = "networkaddress.cache.negative.ttl";
        int ttl = 0;
        String policy = Security.getProperty(ttlProp);
        if (policy != null) {
            ttl = Integer.parseInt(policy);
        }
        if (ttl <= 0  || ttl > 15) {
            System.err.println("Security property " + ttlProp + " needs to " +
                " in 1-15 second range to execute this test");
            return;

        }
        String hostsFileName = System.getProperty("jdk.net.hosts.file");

        /*
         * The following outlines how the test works :-
         *
         * 1. Do a lookup via InetAddress.getByName that it guaranteed
         *    to succeed. This forces at least one entry into the cache
         *    that will not expire.
         *
         * 2. Do a lookup via InetAddress.getByName that is guarnateed
         *    to fail. This results in a negative lookup cached for
         *    for a short period to time.
         *
         * 3. Wait for the cache entry to expire.
         *
         * 4. Do a lookup (which should consult the name service) and
         *    the lookup should succeed.
         */

        // name service needs to resolve this.
        addMappingToHostsFile("theclub", "129.156.220.219", hostsFileName, false);

        // this lookup will succeed
        InetAddress.getByName("theclub");

        // lookup "luster" - this should throw UHE as name service
        // doesn't know anything about this host.

        try {
            InetAddress.getByName("luster");
            throw new RuntimeException("Test internal error " +
                " - luster is being resolved by name service");
        } catch (UnknownHostException x) {
        }

        // name service now needs to know about luster
        addMappingToHostsFile("luster", "10.5.18.21", hostsFileName, true);

        // wait for the cache entry to expire and lookup should
        // succeed.
        Thread.currentThread().sleep(ttl*1000 + 1000);
        InetAddress.getByName("luster");
    }

    private static void addMappingToHostsFile ( String host,
                                                String addr,
                                                String hostsFileName,
                                                boolean append)
                                                throws Exception {
        String mapping = addr + " " + host;
        try (PrintWriter hfPWriter = new PrintWriter(new BufferedWriter(
                new FileWriter(hostsFileName, append)))) {
            hfPWriter.println(mapping);
}
    }

}
