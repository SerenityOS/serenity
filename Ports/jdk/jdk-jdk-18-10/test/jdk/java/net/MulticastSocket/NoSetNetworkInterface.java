/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233307
 * @library /test/lib
 * @run main/othervm NoSetNetworkInterface
 * @run main/othervm -Djava.net.preferIPv4Stack=true NoSetNetworkInterface
 * @run main/othervm -Djava.net.preferIPv6Addresses=true NoSetNetworkInterface
 * @summary Check that methods that are used to set and get the NetworkInterface
 *  for a MulticastSocket work as expected. This test also checks that getOption
 *  returns null correctly when a NetworkInterface has not been set
 */

import jdk.test.lib.NetworkConfiguration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.StandardSocketOptions;
import java.util.Optional;
import java.util.function.Predicate;

public class NoSetNetworkInterface {
    public static void main(String[] args) throws Exception {

        NetworkConfiguration nc = NetworkConfiguration.probe();

        // check set and get methods work as expected
        nc.multicastInterfaces(true).forEach(ni -> {
            checkSetInterface(ni);
            checkSetNetworkInterface(ni);
            checkSetOption(ni);
        });

        // Check that dummy NetworkInterface is returned when not set
        checkDummyNetworkInterface();
    }

    public static void checkSetInterface(NetworkInterface ni) {
        try (MulticastSocket ms = new MulticastSocket()) {
            Optional<InetAddress> iAddr = ni.inetAddresses()
                    .filter(Predicate.not(InetAddress::isAnyLocalAddress))
                    .findFirst();
            if (iAddr.isPresent()) {
                ms.setInterface(iAddr.get());
                checkForCorrectNetworkInterface("setInterface", ms, ni);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void checkSetNetworkInterface(NetworkInterface ni) {
        try (MulticastSocket ms = new MulticastSocket()) {
            ms.setNetworkInterface(ni);
            checkForCorrectNetworkInterface("setNetworkInterface", ms, ni);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void checkSetOption(NetworkInterface ni) {
        try (MulticastSocket ms = new MulticastSocket()) {
            ms.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
            checkForCorrectNetworkInterface("setOption", ms, ni);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void checkForCorrectNetworkInterface(String setterMethod,
                                                       MulticastSocket ms,
                                                       NetworkInterface ni) throws IOException {

        // getInterface
        InetAddress testAddr = ms.getInterface();
        if (!ni.inetAddresses().anyMatch(i -> i.equals(testAddr))) {
            throw new RuntimeException(setterMethod + " != getInterface");
        }

        // getNetworkInterface
        if (!ni.equals(ms.getNetworkInterface())) {
            throw new RuntimeException(setterMethod + " != getNetworkInterface");
        }

        // getOption
        if (!ni.equals(ms.getOption(StandardSocketOptions.IP_MULTICAST_IF))) {
            throw new RuntimeException(setterMethod + " != getOption");
        }
    }

    public static void checkDummyNetworkInterface() throws IOException {

        try(MulticastSocket ms = new MulticastSocket()) {

            // getOption with no Network Interface set
            NetworkInterface n0 = ms.getOption(StandardSocketOptions.IP_MULTICAST_IF);
            if (n0 != null) {
                throw new RuntimeException("NetworkInterface should be null");
            }

            // getNetworkInterface with no Network Interface set
            NetworkInterface n1 = ms.getNetworkInterface();
            if (n1 == null) {
                throw new RuntimeException("getNetworkInterface() should not return null");
            } else if (!((n1.getName().equals("0.0.0.0") || n1.getName().equals("::"))
                    && (n1.getIndex() == 0)
                    && (n1.inetAddresses().count() == 1))) {

                throw new RuntimeException("Dummy NetworkInterface not returned as expected");
            }

            // getInterface with no Network Interface set
            InetAddress iaddr = ms.getInterface();
            if (iaddr == null) {
                throw new RuntimeException("getInterface() should not return null");
            } else if (!iaddr.isAnyLocalAddress()) {
                throw new RuntimeException("getInterface() should return anyLocalAddress");
            }
        }
    }
}

