/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8134577
 * @summary Test the internal NameService implementation which is enabled via
 *          the system property jdk.net.hosts.file. This property specifies
 *          a file name that contains address host mappings, similar to those in
 *          /etc/hosts file. TestHosts-II file doesn't exist, so a UHE should be
 *          thrown
 * @run main/othervm -Djdk.net.hosts.file=TestHosts-II -Dsun.net.inetaddr.ttl=0
 *          InternalNameServiceWithNoHostsFileTest
 */



import java.net.InetAddress;
import java.net.UnknownHostException;


public class InternalNameServiceWithNoHostsFileTest {
    public static void main(String args[]) throws Exception {
        InetAddress testAddress = null;

        try {
            testAddress = InetAddress.getByName("host.sample-domain");
            throw new RuntimeException ("UnknownHostException expected");
        } catch (UnknownHostException uhEx) {
            System.out.println("UHE caught as expected == " + uhEx.getMessage());
        }
    }
}
