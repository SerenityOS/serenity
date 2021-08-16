/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.NetworkConfiguration;

/*
 * @test
 * @bug 8021372
 * @summary Tests that the MAC addresses returned by NetworkInterface.getNetworkInterfaces are unique for each adapter.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 * @run main/othervm UniqueMacAddressesTest
 */
public class UniqueMacAddressesTest {

    static PrintStream log = System.err;

    // A record pair (NetworkInterface::name,  NetworkInterface::hardwareAddress)
    record NetIfPair(String interfaceName, byte[] address) {}

    public static void main(String[] args) throws Exception {
        new UniqueMacAddressesTest().execute();
        log.println("UniqueMacAddressesTest: OK");
    }

    public UniqueMacAddressesTest() {
        log.println("UniqueMacAddressesTest: start");
    }

    public void execute() throws Exception {
        // build a list of NetworkInterface name address pairs
        // to test MAC address uniqueness
        List<NetIfPair> netIfList = createNetworkInterfaceList(NetworkConfiguration.probe());
        if (!macAddressesAreUnique(netIfList))
            throw new RuntimeException("mac address uniqueness test failed");
    }

    private boolean macAddressesAreUnique(List<NetIfPair> netIfPairs) {
        for (NetIfPair netIfPair : netIfPairs) {
            for (NetIfPair compNetIfPair : netIfPairs) {
                if (!netIfPair.interfaceName.equals(compNetIfPair.interfaceName) &&
                        testMacAddressesEqual(netIfPair, compNetIfPair))
                    return false;
            }
        }
        return true;
    }

    private boolean testMacAddressesEqual(NetIfPair if1, NetIfPair if2) {
        log.println("Compare hardware addresses of " + if1.interfaceName + " ("
                +  createMacAddressString(if1.address) + ")" + " and " + if2.interfaceName
                + " (" + createMacAddressString(if2.address) + ")");
        return (Arrays.equals(if1.address, if2.address));
    }

    private String createMacAddressString(byte[] macAddr) {
        StringBuilder sb =  new StringBuilder();
        if (macAddr != null) {
            for (int i = 0; i < macAddr.length; i++) {
                sb.append(String.format("%02X%s", macAddr[i],
                        (i < macAddr.length - 1) ? "-" : ""));
            }
        }
        return sb.toString();
    }

    private byte[] getNetworkInterfaceHardwareAddress(NetworkInterface inf) {
        try {
            return inf.getHardwareAddress();
        } catch (SocketException se) {
            throw new UncheckedIOException(se);
        }
    }

    private List<NetIfPair> createNetworkInterfaceList(NetworkConfiguration netConf) {
        return netConf.interfaces()
                .map(netIf -> new NetIfPair(netIf.getName(), getNetworkInterfaceHardwareAddress(netIf)))
                .collect(Collectors.filtering(netIfPair -> netIfPair.address != null,
                        Collectors.toCollection(ArrayList::new)));
    }
}
