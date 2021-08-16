/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

/**
 * @test
 * @bug 6458027
 * @summary Disabling IPv6 on a specific network interface causes problems.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main SetGetNetworkInterfaceTest
 * @run main/othervm -Djava.net.preferIPv4Stack=true SetGetNetworkInterfaceTest
*/
public class SetGetNetworkInterfaceTest {

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();
        NetworkConfiguration nc = NetworkConfiguration.probe();
        try (MulticastSocket ms = new MulticastSocket()) {
            nc.multicastInterfaces(true).forEach(nif -> setGetNetworkInterface(ms, nif));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Test passed.");
    }

    static void setGetNetworkInterface(MulticastSocket ms, NetworkInterface nif) {
        try {
            System.out.println(NetworkConfiguration.interfaceInformation(nif));
            ms.setNetworkInterface(nif);
            NetworkInterface msNetIf = ms.getNetworkInterface();
            if (nif.equals(msNetIf)) {
                System.out.println(" OK");
            } else {
                System.out.println("FAILED!!!");
                System.out.println(NetworkConfiguration.interfaceInformation(msNetIf));
                throw new RuntimeException("Test Fail");
            }
            System.out.println("------------------");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
