/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary NOT A TEST. Captures the network interface configuration.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main NetworkConfigurationProbe
 */

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import jdk.test.lib.NetworkConfiguration;
import static java.util.stream.Collectors.joining;
import static java.lang.System.out;

/**
 * Not a test. Captures the network interface configuration.
 */
public class NetworkConfigurationProbe {

    public static void main(String... args) throws Exception {
        NetworkConfiguration.printSystemConfiguration(out);

        NetworkConfiguration nc = NetworkConfiguration.probe();
        String list;
        list = nc.ip4MulticastInterfaces()
                  .map(NetworkInterface::getName)
                  .collect(joining(" "));
        out.println("ip4MulticastInterfaces: " +  list);

        list = nc.ip4Addresses()
                  .map(Inet4Address::toString)
                  .collect(joining(" "));
        out.println("ip4Addresses: " +  list);

        list = nc.ip6MulticastInterfaces()
                  .map(NetworkInterface::getName)
                  .collect(joining(" "));
        out.println("ip6MulticastInterfaces: " +  list);

        list = nc.ip6Addresses()
                  .map(Inet6Address::toString)
                  .collect(joining(" "));
        out.println("ip6Addresses: " +  list);
    }
}
