/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

/**
 * @test
 * @bug 4091811 4148753 4102731
 * @summary Test java.net.MulticastSocket joinGroup and leaveGroup
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main JoinLeave
 * @run main/othervm -Djava.net.preferIPv4Stack=true JoinLeave
 */
public class JoinLeave {

    public static void main(String args[]) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();
        InetAddress ip4Group = InetAddress.getByName("224.80.80.80");
        InetAddress ip6Group = InetAddress.getByName("ff02::a");

        NetworkConfiguration nc = NetworkConfiguration.probe();
        nc.ip4MulticastInterfaces().forEach(nic -> joinLeave(ip4Group, nic));
        nc.ip6MulticastInterfaces().forEach(nic -> joinLeave(ip6Group, nic));
    }

    static void joinLeave(InetAddress group, NetworkInterface nif) {
        System.out.println("Joining:" + group + " on " + nif);
        try (MulticastSocket soc = new MulticastSocket()) {
            soc.setNetworkInterface(nif);
            soc.joinGroup(group);
            soc.leaveGroup(group);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
