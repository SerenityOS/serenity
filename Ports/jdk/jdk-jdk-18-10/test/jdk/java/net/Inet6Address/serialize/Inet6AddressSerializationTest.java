/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * @test
 * @bug 8007373
 * @summary jdk7 backward compatibility serialization problem
 */

public class Inet6AddressSerializationTest {

    static boolean failed;

    static boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    public static final int LOOPBACK_SCOPE_ID = 0;

    public static final byte[] IN6ADDR_ANY_INIT = { (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

    public static final byte[] LOOPBACKIPV6ADDRESS = { (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    // fe80::21b:24ff:febd:f29c
    public static final byte[] E1000G0IPV6ADDRESS = { (byte) 0xfe, (byte) 0x80,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x02, (byte) 0x1b, (byte) 0x24, (byte) 0xff,
            (byte) 0xfe, (byte) 0xbd, (byte) 0xf2, (byte) 0x9c };

    public static final String E1000G0HOSTNAME = "fe80:0:0:0:21b:24ff:febd:f29c%e1000g0";

    public static final String LOCALHOSTNAME = "localhost";

    public static final String NETWORK_IF_E1000G0 = "e1000g0";

    public static final String NETWORK_IF_LO0 = "lo0";

    public static final int SCOPE_ID_E1000G0 = 2;

    public static final int SCOPE_ID_LO0 = 1;

    public static final int SCOPE_ID_ZERO = 0;

    public static void main(String[] args) throws Exception {
        // args[0] == generate-loopback generates serial data for loopback if
        // args[0] == generateAll generates serial data for interfaces with an
        // IPV6 address binding

        if (args.length != 0) {

            if (args[0].equals("generate-loopback")) {

                generateSerializedInet6AddressData(Inet6Address.getByAddress(
                        InetAddress.getLoopbackAddress().getHostName(),
                        LOOPBACKIPV6ADDRESS, LOOPBACK_SCOPE_ID), System.out,
                        true);

            } else {
                generateAllInet6AddressSerializedData();
            }
        } else {
            runTests();
        }
    }

    private static void runTests() throws UnknownHostException, Exception,
            IOException {
        byte[] thisHostIPV6Address = null;
        int scope_id = LOOPBACK_SCOPE_ID;

        System.out.println("Hostname: "
                + InetAddress.getLocalHost().getHostName());
        System.out.println("LocalHost isLoopback : "
                + InetAddress.getLocalHost().isLoopbackAddress());
        thisHostIPV6Address = getThisHostIPV6Address(InetAddress.getLocalHost()
                .getHostName());

        if (thisHostIPV6Address == null) {
            thisHostIPV6Address = IN6ADDR_ANY_INIT;
        }

        // testing JDK7 generated serialized loopback against locally generated
        // loopback address
        testInet6AddressSerialization(Inet6Address.getByAddress(InetAddress
                .getLoopbackAddress().getHostName(), LOOPBACKIPV6ADDRESS,
                scope_id), JDK7Inet6AddressSerialData);
        // testing JDK8 generated serialized loopback against locally generated
        // loopback address
        testInet6AddressSerialization(Inet6Address.getByAddress(InetAddress
                .getLoopbackAddress().getHostName(), LOOPBACKIPV6ADDRESS,
                scope_id), JDK8Inet6AddressSerialData);
        testInet6AddressSerialization(Inet6Address.getByAddress(InetAddress
                .getLocalHost().getHostName(), IN6ADDR_ANY_INIT, scope_id),
                null);
        testInet6AddressSerialization(Inet6Address.getByAddress(InetAddress
                .getLocalHost().getHostName(), thisHostIPV6Address, scope_id),
                null);
        testAllNetworkInterfaces();

        // test against lo0
        testSerializedLo0Inet6Address();

        testSerializedE1000gInet6Address();

        if (failed)
            throw new RuntimeException("Some tests failed, check output");
    }

    private static byte[] getThisHostIPV6Address(String hostName)
            throws Exception {
        InetAddress[] thisHostIPAddresses = null;
        try {
            thisHostIPAddresses = InetAddress.getAllByName(InetAddress
                    .getLocalHost().getHostName());
        } catch (UnknownHostException uhEx) {
            uhEx.printStackTrace();
            throw uhEx;
        }
        byte[] thisHostIPV6Address = null;
        for (InetAddress inetAddress : thisHostIPAddresses) {
            if (inetAddress instanceof Inet6Address) {
                if (inetAddress.getHostName().equals(hostName)) {
                    thisHostIPV6Address = inetAddress.getAddress();
                    break;
                }
            }
        }
        // System.err.println("getThisHostIPV6Address: address is "
        // + Arrays.toString(thisHostIPV6Address));
        return thisHostIPV6Address;
    }

    static void testAllNetworkInterfaces() throws Exception {
        System.err.println("\n testAllNetworkInterfaces: \n ");
        for (Enumeration<NetworkInterface> e = NetworkInterface
                .getNetworkInterfaces(); e.hasMoreElements();) {
            NetworkInterface netIF = e.nextElement();
            // Skip (Windows)Teredo Tunneling Pseudo-Interface
            if (isWindows) {
                String dName = netIF.getDisplayName();
                if (dName != null && dName.contains("Teredo")) {
                    continue;
                }
            }
            for (Enumeration<InetAddress> iadrs = netIF.getInetAddresses(); iadrs
                    .hasMoreElements();) {
                InetAddress iadr = iadrs.nextElement();
                if (iadr instanceof Inet6Address) {
                    System.err.println("Test NetworkInterface:  " + netIF);
                    Inet6Address i6adr = (Inet6Address) iadr;
                    System.err.println("Testing with " + iadr);
                    System.err.println(" scoped iface: "
                            + i6adr.getScopedInterface());
                    System.err.println(" hostname: " + i6adr.getHostName());
                    testInet6AddressSerialization(i6adr, null);
                }
            }
        }
    }

    static void displayExpectedInet6Address(Inet6Address expectedInet6Address) {

        String expectedHostName = expectedInet6Address.getHostName();
        byte[] expectedAddress = expectedInet6Address.getAddress();
        String expectedHostAddress = expectedInet6Address.getHostAddress();
        int expectedScopeId = expectedInet6Address.getScopeId();
        NetworkInterface expectedNetIf = expectedInet6Address
                .getScopedInterface();

        System.err.println("Excpected HostName: " + expectedHostName);
        System.err.println("Expected Address: "
                + Arrays.toString(expectedAddress));
        System.err.println("Expected HostAddress: " + expectedHostAddress);
        System.err.println("Expected Scope Id " + expectedScopeId);
        System.err.println("Expected NetworkInterface " + expectedNetIf);
        System.err.println("Expected Inet6Address " + expectedInet6Address);
    }

    // test serialization deserialization of Inet6Address
    static void testInet6AddressSerialization(
            Inet6Address expectedInet6Address, byte[] serializedAddress)
            throws IOException {
        System.err.println("\n testInet6AddressSerialization:  enter \n");

        // displayExpectedInet6Address(expectedInet6Address);

        byte[] serialData = serializedAddress != null ? serializedAddress
                : generateSerializedInet6AddressData(expectedInet6Address,
                        null, false);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialData);
                ObjectInputStream oin = new ObjectInputStream(bis)) {
            Inet6Address deserializedIPV6Addr = (Inet6Address) oin.readObject();
            System.err.println("Deserialized Inet6Address "
                    + deserializedIPV6Addr);
            assertHostNameEqual(expectedInet6Address.getHostName(),
                    deserializedIPV6Addr.getHostName());
            assertHostAddressEqual(expectedInet6Address.getHostAddress(),
                    deserializedIPV6Addr.getHostAddress());
            assertAddressEqual(expectedInet6Address.getAddress(),
                    deserializedIPV6Addr.getAddress());
            assertScopeIdEqual(expectedInet6Address.getScopeId(),
                    deserializedIPV6Addr.getScopeId());
            assertNetworkInterfaceEqual(
                    expectedInet6Address.getScopedInterface(),
                    deserializedIPV6Addr.getScopedInterface());
        } catch (Exception e) {
            System.err.println("Exception caught during deserialization");
            failed = true;
            e.printStackTrace();
        }
    }

    static void testSerializedE1000gInet6Address() throws IOException {
        System.err.println("\n testSerializedE1000gInet6Address:  enter \n");
        boolean testWithNetIf = true;
        boolean useMockInet6Address = false;

        NetworkInterface testNetIf = NetworkInterface
                .getByName(NETWORK_IF_E1000G0);
        Inet6Address expectedInet6Address = null;
        if (testNetIf != null) {
            System.err
                    .println("\n testSerializedE1000gInet6Address:  using netif \n");
            try {
                expectedInet6Address = Inet6Address.getByAddress(
                        E1000G0HOSTNAME, E1000G0IPV6ADDRESS, testNetIf);
            } catch (UnknownHostException ukhEx) {
                ukhEx.printStackTrace();
                testWithNetIf = true;
                useMockInet6Address = true;
            }
        } else {
            System.err
                    .println("\n testSerializedE1000gInet6Address:  using index \n");
            try {
                expectedInet6Address = Inet6Address.getByAddress(
                        E1000G0HOSTNAME, E1000G0IPV6ADDRESS, SCOPE_ID_ZERO);
            } catch (UnknownHostException ukhEx1) {
                ukhEx1.printStackTrace();
                useMockInet6Address = true;
            }
            testWithNetIf = false;
        }

        byte[] serializedAddress = SerialData_ifname_e1000g0;

        // displayExpectedInet6Address(expectedInet6Address);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(
                serializedAddress);
                ObjectInputStream oin = new ObjectInputStream(bis)) {
            Inet6Address deserializedIPV6Addr = (Inet6Address) oin.readObject();
            System.err.println("Deserialized Inet6Address "
                    + deserializedIPV6Addr);

            if (!useMockInet6Address) {
                assertHostNameEqual(expectedInet6Address.getHostName(),
                        deserializedIPV6Addr.getHostName());
                if (testWithNetIf) {
                    assertHostAddressEqual(
                            expectedInet6Address.getHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                } else {
                    assertHostAddressEqual(
                            MockE1000g0Inet6Address.getBareHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                }
                assertAddressEqual(expectedInet6Address.getAddress(),
                        deserializedIPV6Addr.getAddress());
                assertScopeIdEqual(expectedInet6Address.getScopeId(),
                        deserializedIPV6Addr.getScopeId());
                if (testWithNetIf) {
                    assertNetworkInterfaceEqual(
                            expectedInet6Address.getScopedInterface(),
                            deserializedIPV6Addr.getScopedInterface());
                } else {
                    assertNetworkInterfaceEqual(null,
                            deserializedIPV6Addr.getScopedInterface());
                }
            } else { // use MockLo0Inet6Address
                assertHostNameEqual(MockE1000g0Inet6Address.getHostName(),
                        deserializedIPV6Addr.getHostName());
                if (testWithNetIf) {
                    assertHostAddressEqual(
                            MockE1000g0Inet6Address.getHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                } else {
                    assertHostAddressEqual(
                            MockE1000g0Inet6Address.getHostAddressWithIndex(),
                            deserializedIPV6Addr.getHostAddress());
                }
                assertAddressEqual(MockE1000g0Inet6Address.getAddress(),
                        deserializedIPV6Addr.getAddress());
                if (testWithNetIf) {
                assertScopeIdEqual(MockE1000g0Inet6Address.getScopeId(),
                        deserializedIPV6Addr.getScopeId());
                } else {
                    assertScopeIdEqual(MockE1000g0Inet6Address.getScopeZero(),
                            deserializedIPV6Addr.getScopeId());
                }
                assertNetworkInterfaceNameEqual(
                        MockE1000g0Inet6Address.getScopeIfName(),
                        deserializedIPV6Addr.getScopedInterface());
            }
        } catch (Exception e) {
            System.err.println("Exception caught during deserialization");
            failed = true;
            e.printStackTrace();
        }
    }

    static void testSerializedLo0Inet6Address() throws IOException {
        System.err.println("\n testSerializedLo0Inet6Address:  enter \n");
        boolean testWithNetIf = true;
        boolean useMockInet6Address = false;

        NetworkInterface testNetIf = NetworkInterface.getByName(NETWORK_IF_LO0);
        Inet6Address expectedInet6Address = null;
        if (testNetIf != null) {
            System.err
                    .println("\n testSerializedLo0Inet6Address:  using netif \n");
            try {
                expectedInet6Address = Inet6Address.getByAddress(LOCALHOSTNAME,
                        LOOPBACKIPV6ADDRESS, testNetIf);
            } catch (UnknownHostException ukhEx) {
                ukhEx.printStackTrace();
                testWithNetIf = true;
                useMockInet6Address = true;
            }
        } else {
            System.err
                    .println("\n testSerializedLo0Inet6Address:  using index \n");
            try {
                expectedInet6Address = Inet6Address.getByAddress(LOCALHOSTNAME,
                        LOOPBACKIPV6ADDRESS, SCOPE_ID_ZERO);
            } catch (UnknownHostException ukhEx1) {
                ukhEx1.printStackTrace();
                useMockInet6Address = true;
            }
            testWithNetIf = false;
        }

        // displayExpectedInet6Address(expectedInet6Address);

        byte[] serializedAddress = SerialData_ifname_lo0;

        try (ByteArrayInputStream bis = new ByteArrayInputStream(
                serializedAddress);
                ObjectInputStream oin = new ObjectInputStream(bis)) {
            Inet6Address deserializedIPV6Addr = (Inet6Address) oin.readObject();
            System.err.println("Deserialized Inet6Address "
                    + deserializedIPV6Addr);
            if (!useMockInet6Address) {
                assertHostNameEqual(expectedInet6Address.getHostName(),
                        deserializedIPV6Addr.getHostName());
                if (testWithNetIf) {
                    assertHostAddressEqual(
                            expectedInet6Address.getHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                } else {
                    assertHostAddressEqual(
                            MockLo0Inet6Address.getBareHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                }
                assertAddressEqual(expectedInet6Address.getAddress(),
                        deserializedIPV6Addr.getAddress());
                assertScopeIdEqual(expectedInet6Address.getScopeId(),
                        deserializedIPV6Addr.getScopeId());
                if (testWithNetIf) {
                    assertNetworkInterfaceEqual(
                            expectedInet6Address.getScopedInterface(),
                            deserializedIPV6Addr.getScopedInterface());
                } else {
                    assertNetworkInterfaceEqual(null,
                            deserializedIPV6Addr.getScopedInterface());
                }
            } else { // use MockLo0Inet6Address
                assertHostNameEqual(MockLo0Inet6Address.getHostName(),
                        deserializedIPV6Addr.getHostName());
                if (testWithNetIf) {
                    assertHostAddressEqual(
                            MockLo0Inet6Address.getHostAddress(),
                            deserializedIPV6Addr.getHostAddress());
                } else {
                    assertHostAddressEqual(
                            MockLo0Inet6Address.getHostAddressWithIndex(),
                            deserializedIPV6Addr.getHostAddress());
                }
                assertAddressEqual(MockLo0Inet6Address.getAddress(),
                        deserializedIPV6Addr.getAddress());
                if (testWithNetIf) {
                assertScopeIdEqual(MockLo0Inet6Address.getScopeId(),
                        deserializedIPV6Addr.getScopeId());
                } else {
                    assertScopeIdEqual(MockLo0Inet6Address.getScopeZero(),
                            deserializedIPV6Addr.getScopeId());
                }
                assertNetworkInterfaceNameEqual(
                        MockLo0Inet6Address.getScopeIfName(),
                        deserializedIPV6Addr.getScopedInterface());
            }
        } catch (Exception e) {
            System.err.println("Exception caught during deserialization");
            failed = true;
            e.printStackTrace();
        }
    }

    static List<Inet6Address> getAllInet6Addresses() throws Exception {
        // System.err.println("\n getAllInet6Addresses: \n ");
        ArrayList<Inet6Address> inet6Addresses = new ArrayList<Inet6Address>();
        for (Enumeration<NetworkInterface> e = NetworkInterface
                .getNetworkInterfaces(); e.hasMoreElements();) {
            NetworkInterface netIF = e.nextElement();
            for (Enumeration<InetAddress> iadrs = netIF.getInetAddresses(); iadrs
                    .hasMoreElements();) {
                InetAddress iadr = iadrs.nextElement();
                if (iadr instanceof Inet6Address) {
                    System.err.println("Test NetworkInterface:  " + netIF);
                    Inet6Address i6adr = (Inet6Address) iadr;
                    System.err.println(" address " + iadr);
                    System.err.println(" scoped iface: "
                            + i6adr.getScopedInterface());
                    // using this to actually set the hostName for an
                    // InetAddress
                    // created through the NetworkInterface
                    // have found that the fabricated instances has a null
                    // hostName
                    System.err.println(" hostName: " + i6adr.getHostName());
                    inet6Addresses.add(i6adr);
                }
            }
        }
        return inet6Addresses;
    }

    static void assertHostNameEqual(String expectedHostName,
            String deserializedHostName) {
        System.err
                .println("Inet6AddressSerializationTest.assertHostNameEqual:");
        if (expectedHostName == null) {
            if (deserializedHostName == null) {
                // ok, do nothing.
            } else {
                System.err.println("Error checking " + " HostName, expected:"
                        + expectedHostName + ", got :" + deserializedHostName);
                failed = true;
            }
        } else if (!expectedHostName.equals(deserializedHostName)) {
            System.err.println("Error checking "
                    + // versionStr +
                    " HostName, expected:" + expectedHostName + ", got :"
                    + deserializedHostName);
            failed = true;
        } else {
            System.err.println("HostName equality "
                    + // versionStr +
                    " HostName, expected:" + expectedHostName + ", got :"
                    + deserializedHostName);
        }
    }

    static void assertHostAddressEqual(String expectedHostAddress,
            String deserializedHostAddress) {
        System.err
                .println("Inet6AddressSerializationTest.assertHostAddressEqual:");
        if (expectedHostAddress == null) {
            if (deserializedHostAddress == null) {
                // ok, do nothing.
            } else {
                System.err.println("Error checking "
                        + " HostAddress, expected: " + expectedHostAddress
                        + ", got: " + deserializedHostAddress);
                failed = true;
            }
        } else if (!expectedHostAddress.equals(deserializedHostAddress)) {
            System.err.println("Error checking "
                    + // versionStr +
                    " HostAddress, expected: " + expectedHostAddress
                    + ", got: " + deserializedHostAddress);
            failed = true;
        } else {
            System.err.println("HostAddress equality "
                    + // versionStr +
                    " HostAddress, expected: " + expectedHostAddress
                    + ", got: " + deserializedHostAddress);
        }
    }

    static void assertAddressEqual(byte[] expectedAddress,
            byte[] deserializedAddress) {
        System.err.println("Inet6AddressSerializationTest.assertAddressEqual:");
        if (expectedAddress == null) {
            if (deserializedAddress == null) {
                // ok, do nothing.
            } else {
                System.err.println("Error checking " + " Address, expected:"
                        + Arrays.toString(expectedAddress) + ", got: "
                        + Arrays.toString(deserializedAddress));
                failed = true;
            }
        } else if (!Arrays.equals(expectedAddress, deserializedAddress)) {
            System.err.println("Error checking "
                    + // versionStr +
                    " Address, expected: " + Arrays.toString(expectedAddress)
                    + ", got: " + Arrays.toString(deserializedAddress));
            failed = true;
        } else {
            System.err.println("Address equality "
                    + // versionStr +
                    " Address, expected: " + Arrays.toString(expectedAddress)
                    + ", got: " + Arrays.toString(deserializedAddress));
        }
    }

    static void assertScopeIdEqual(int expectedScopeId, int deserializedScopeId) {
        System.err.println("Inet6AddressSerializationTest.assertScopeIdEqual:");
        if (expectedScopeId != deserializedScopeId) {
            System.err.println("Error checking " + " ScopeId, expected:"
                    + expectedScopeId + ", got: " + deserializedScopeId);
            failed = true;
        } else {
            System.err.println("ScopeId equality "
                    + // versionStr +
                    " ScopeId, expected: " + expectedScopeId + ", got: "
                    + deserializedScopeId);
        }
    }

    static void assertNetworkInterfaceNameEqual(String expectedNetworkIfName,
            NetworkInterface deserializedNetworkInterface) {

        if (deserializedNetworkInterface != null) {
            String deserializedNetworkIfName = deserializedNetworkInterface
                    .getName();
            System.err
                    .println("Inet6AddressSerializationTest.assertHostNameEqual:");
            if (expectedNetworkIfName == null) {
                if (deserializedNetworkIfName == null) {
                    // ok, do nothing.
                } else {
                    System.err.println("Error checking "
                            + " NetworkIfName, expected: "
                            + expectedNetworkIfName + ", got: "
                            + deserializedNetworkIfName);
                    failed = true;
                }
            } else if (!expectedNetworkIfName.equals(deserializedNetworkIfName)) {
                System.err.println("Error checking "
                        + " NetworkIfName, expected: " + expectedNetworkIfName
                        + ", got: " + deserializedNetworkIfName);
                failed = true;
            } else {
                System.err.println("NetworkIfName equality "
                        + " NetworkIfName, expected: " + expectedNetworkIfName
                        + ", got: " + deserializedNetworkIfName);
            }
        } else {
            System.err
                    .println("Warning "
                            + " NetworkInterface  expected, but is null - ifname not relevant on deserializing host");
        }
    }

    static void assertNetworkInterfaceEqual(
            NetworkInterface expectedNetworkInterface,
            NetworkInterface deserializedNetworkInterface) {
        System.err
                .println("Inet6AddressSerializationTest.assertNetworkInterfaceEqual:");
        if (expectedNetworkInterface == null) {
            if (deserializedNetworkInterface == null) {
                // ok, do nothing.
                System.err.println("Network Interface equality "
                        + " NetworkInterface, expected:"
                        + expectedNetworkInterface + ", got :"
                        + deserializedNetworkInterface);
            } else {
                System.err.println("Error checking "
                        + " NetworkInterface, expected:"
                        + expectedNetworkInterface + ", got :"
                        + deserializedNetworkInterface);
                failed = true;
            }
        } else if (!expectedNetworkInterface
                .equals(deserializedNetworkInterface)) {
            System.err.println("Error checking "
                    + // versionStr +
                    " NetworkInterface, expected:" + expectedNetworkInterface
                    + ", got :" + deserializedNetworkInterface);
            failed = true;
        } else {
            System.err.println("Network Interface equality "
                    + " NetworkInterface, expected:" + expectedNetworkInterface
                    + ", got :" + deserializedNetworkInterface);
        }
    }

    static void equal(Object expected, Object got) {
        if (expected == null) {
            if (got == null) {
                // ok, do nothing.
            } else {
                System.err.println("Error checking "
                        + " serial data, expected:" + expected + ", got :"
                        + got);
                failed = true;
            }
        } else if (!expected.equals(got)) {
            System.err.println("Error checking " + // versionStr +
                    " serial data, expected:" + expected + ", got :" + got);
            failed = true;
        }
    }

    // Used to generate serialData.
    static byte[] generateSerializedInet6AddressData(Inet6Address addr,
            PrintStream out, boolean outputToFile) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(addr);
        }

        String ifname = getIfName(addr);
        byte[] ba = bos.toByteArray();
        if (out != null) {
            out.format("static final byte[] SerialData" + ifname + " = {\n");
            for (int i = 0; i < ba.length; i++) {
                out.format(" (byte)0x%02X", ba[i]);
                if (i != (ba.length - 1))
                    out.format(",");
                if (((i + 1) % 6) == 0)
                    out.format("\n");
            }
            out.format(" };\n \n");
        }
        if (outputToFile) {
            serializeInet6AddressToFile(addr);
        }
        return ba;
    }

    private static String getIfName(Inet6Address inet6Addr) {
        String ifname;
        if (inet6Addr.getScopedInterface() != null) {
            ifname = "_ifname_" + inet6Addr.getScopedInterface().getName();
        } else {
            ifname = "_ifname_"
                    + Integer.valueOf(inet6Addr.getScopeId()).toString();
        }
        return ifname;
    }

    static void generateAllInet6AddressSerializedData() throws IOException {
        // System.err.println("generateAllInet6AddressSerializedData: enter ....");

        List<Inet6Address> inet6Addresses;

        try {
            inet6Addresses = getAllInet6Addresses();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        for (Inet6Address inet6Address : inet6Addresses) {
            generateSerializedInet6AddressData(inet6Address, System.out, true);
        }
    }

    static void serializeInet6AddressToFile(Inet6Address inet6Addr) {

        // System.err
        // .println("serializeInet6AddressToIPV6AddressFile: enter ....");

        FileOutputStream fOut = null;
        String inet6AddressOutputFilename = null;
        inet6AddressOutputFilename = createOutputFileName(inet6Addr);
        try {
            fOut = new FileOutputStream(inet6AddressOutputFilename);
        } catch (FileNotFoundException fnfEx) {

            fnfEx.printStackTrace();
        }
        ObjectOutputStream ooStream = null;
        try {
            if (fOut != null) {
                ooStream = new ObjectOutputStream(fOut);
            } else {
                System.err.println("Problem initilising Object output stream ");
                System.exit(-1);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // serialise the last Inet6Address
        /*
         * System.err
         * .println("serializeInet6AddressToIPV6AddressFile scoped iface:  \n" +
         * inet6Addr.getScopedInterface());
         */
        try {
            ooStream.writeObject(inet6Addr);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        try {
            ooStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String createOutputFileName(Inet6Address inet6Addr) {
        String inet6AddressOutputFilename;
        if (inet6Addr.getScopedInterface() != null) {
            inet6AddressOutputFilename = "IPV6Address_"
                    + inet6Addr.getScopedInterface().getName() + ".out";
        } else {
            inet6AddressOutputFilename = "IPV6Address_"
                    + Integer.valueOf(inet6Addr.getScopeId()).toString()
                    + ".out";
        }
        return inet6AddressOutputFilename;
    }

    // --- Generated data ---
    // JDK7 output java Inet6AddressSerializationTest generate.

    // loopback lo0 interface on Solaris 10

    static final byte[] JDK7Inet6AddressSerialData = { (byte) 0xAC,
            (byte) 0xED, (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
            (byte) 0x00, (byte) 0x15, (byte) 0x6A, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2E, (byte) 0x6E, (byte) 0x65, (byte) 0x74,
            (byte) 0x2E, (byte) 0x49, (byte) 0x6E, (byte) 0x65, (byte) 0x74,
            (byte) 0x36, (byte) 0x41, (byte) 0x64, (byte) 0x64, (byte) 0x72,
            (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x5F, (byte) 0x7C,
            (byte) 0x20, (byte) 0x81, (byte) 0x52, (byte) 0x2C, (byte) 0x80,
            (byte) 0x21, (byte) 0x03, (byte) 0x00, (byte) 0x05, (byte) 0x49,
            (byte) 0x00, (byte) 0x08, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64,
            (byte) 0x5A, (byte) 0x00, (byte) 0x0C, (byte) 0x73, (byte) 0x63,
            (byte) 0x6F, (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69,
            (byte) 0x64, (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74,
            (byte) 0x5A, (byte) 0x00, (byte) 0x10, (byte) 0x73, (byte) 0x63,
            (byte) 0x6F, (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69,
            (byte) 0x66, (byte) 0x6E, (byte) 0x61, (byte) 0x6D, (byte) 0x65,
            (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x4C,
            (byte) 0x00, (byte) 0x06, (byte) 0x69, (byte) 0x66, (byte) 0x6E,
            (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x00,
            (byte) 0x12, (byte) 0x4C, (byte) 0x6A, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E,
            (byte) 0x67, (byte) 0x2F, (byte) 0x53, (byte) 0x74, (byte) 0x72,
            (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x3B, (byte) 0x5B,
            (byte) 0x00, (byte) 0x09, (byte) 0x69, (byte) 0x70, (byte) 0x61,
            (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73,
            (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x02, (byte) 0x5B,
            (byte) 0x42, (byte) 0x78, (byte) 0x72, (byte) 0x00, (byte) 0x14,
            (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2E,
            (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x2E, (byte) 0x49,
            (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x41, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x2D, (byte) 0x9B, (byte) 0x57, (byte) 0xAF, (byte) 0x9F,
            (byte) 0xE3, (byte) 0xEB, (byte) 0xDB, (byte) 0x02, (byte) 0x00,
            (byte) 0x03, (byte) 0x49, (byte) 0x00, (byte) 0x07, (byte) 0x61,
            (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73,
            (byte) 0x73, (byte) 0x49, (byte) 0x00, (byte) 0x06, (byte) 0x66,
            (byte) 0x61, (byte) 0x6D, (byte) 0x69, (byte) 0x6C, (byte) 0x79,
            (byte) 0x4C, (byte) 0x00, (byte) 0x08, (byte) 0x68, (byte) 0x6F,
            (byte) 0x73, (byte) 0x74, (byte) 0x4E, (byte) 0x61, (byte) 0x6D,
            (byte) 0x65, (byte) 0x71, (byte) 0x00, (byte) 0x7E, (byte) 0x00,
            (byte) 0x01, (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x74, (byte) 0x00, (byte) 0x09, (byte) 0x6C,
            (byte) 0x6F, (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x68,
            (byte) 0x6F, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x70,
            (byte) 0x75, (byte) 0x72, (byte) 0x00, (byte) 0x02, (byte) 0x5B,
            (byte) 0x42, (byte) 0xAC, (byte) 0xF3, (byte) 0x17, (byte) 0xF8,
            (byte) 0x06, (byte) 0x08, (byte) 0x54, (byte) 0xE0, (byte) 0x02,
            (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x78 };

    // JDK8 output java Inet6AddressSerializationTest generate.
    // loopback lo0 interface on Solaris 10

    static final byte[] JDK8Inet6AddressSerialData = { (byte) 0xAC,
            (byte) 0xED, (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72,
            (byte) 0x00, (byte) 0x15, (byte) 0x6A, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2E, (byte) 0x6E, (byte) 0x65, (byte) 0x74,
            (byte) 0x2E, (byte) 0x49, (byte) 0x6E, (byte) 0x65, (byte) 0x74,
            (byte) 0x36, (byte) 0x41, (byte) 0x64, (byte) 0x64, (byte) 0x72,
            (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x5F, (byte) 0x7C,
            (byte) 0x20, (byte) 0x81, (byte) 0x52, (byte) 0x2C, (byte) 0x80,
            (byte) 0x21, (byte) 0x03, (byte) 0x00, (byte) 0x05, (byte) 0x49,
            (byte) 0x00, (byte) 0x08, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64,
            (byte) 0x5A, (byte) 0x00, (byte) 0x0C, (byte) 0x73, (byte) 0x63,
            (byte) 0x6F, (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69,
            (byte) 0x64, (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74,
            (byte) 0x5A, (byte) 0x00, (byte) 0x10, (byte) 0x73, (byte) 0x63,
            (byte) 0x6F, (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69,
            (byte) 0x66, (byte) 0x6E, (byte) 0x61, (byte) 0x6D, (byte) 0x65,
            (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x4C,
            (byte) 0x00, (byte) 0x06, (byte) 0x69, (byte) 0x66, (byte) 0x6E,
            (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x00,
            (byte) 0x12, (byte) 0x4C, (byte) 0x6A, (byte) 0x61, (byte) 0x76,
            (byte) 0x61, (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E,
            (byte) 0x67, (byte) 0x2F, (byte) 0x53, (byte) 0x74, (byte) 0x72,
            (byte) 0x69, (byte) 0x6E, (byte) 0x67, (byte) 0x3B, (byte) 0x5B,
            (byte) 0x00, (byte) 0x09, (byte) 0x69, (byte) 0x70, (byte) 0x61,
            (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73,
            (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x02, (byte) 0x5B,
            (byte) 0x42, (byte) 0x78, (byte) 0x72, (byte) 0x00, (byte) 0x14,
            (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2E,
            (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x2E, (byte) 0x49,
            (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x41, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x2D, (byte) 0x9B, (byte) 0x57, (byte) 0xAF, (byte) 0x9F,
            (byte) 0xE3, (byte) 0xEB, (byte) 0xDB, (byte) 0x02, (byte) 0x00,
            (byte) 0x03, (byte) 0x49, (byte) 0x00, (byte) 0x07, (byte) 0x61,
            (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73,
            (byte) 0x73, (byte) 0x49, (byte) 0x00, (byte) 0x06, (byte) 0x66,
            (byte) 0x61, (byte) 0x6D, (byte) 0x69, (byte) 0x6C, (byte) 0x79,
            (byte) 0x4C, (byte) 0x00, (byte) 0x08, (byte) 0x68, (byte) 0x6F,
            (byte) 0x73, (byte) 0x74, (byte) 0x4E, (byte) 0x61, (byte) 0x6D,
            (byte) 0x65, (byte) 0x71, (byte) 0x00, (byte) 0x7E, (byte) 0x00,
            (byte) 0x01, (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x74, (byte) 0x00, (byte) 0x09, (byte) 0x6C,
            (byte) 0x6F, (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x68,
            (byte) 0x6F, (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x70,
            (byte) 0x75, (byte) 0x72, (byte) 0x00, (byte) 0x02, (byte) 0x5B,
            (byte) 0x42, (byte) 0xAC, (byte) 0xF3, (byte) 0x17, (byte) 0xF8,
            (byte) 0x06, (byte) 0x08, (byte) 0x54, (byte) 0xE0, (byte) 0x02,
            (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x78 };

    // java Inet6AddressSerializationTest generateAll produces this inet6address
    // serial data
    // jdk8 generated serialization of on address fe80:0:0:0:21b:24ff:febd:f29c
    // net if e1000g0

    static final byte[] SerialData_ifname_e1000g0 = { (byte) 0xAC, (byte) 0xED,
            (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72, (byte) 0x00,
            (byte) 0x15, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2E, (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x2E,
            (byte) 0x49, (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x36,
            (byte) 0x41, (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65,
            (byte) 0x73, (byte) 0x73, (byte) 0x5F, (byte) 0x7C, (byte) 0x20,
            (byte) 0x81, (byte) 0x52, (byte) 0x2C, (byte) 0x80, (byte) 0x21,
            (byte) 0x03, (byte) 0x00, (byte) 0x05, (byte) 0x49, (byte) 0x00,
            (byte) 0x08, (byte) 0x73, (byte) 0x63, (byte) 0x6F, (byte) 0x70,
            (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64, (byte) 0x5A,
            (byte) 0x00, (byte) 0x0C, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64,
            (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x5A,
            (byte) 0x00, (byte) 0x10, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x66,
            (byte) 0x6E, (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x5F,
            (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x4C, (byte) 0x00,
            (byte) 0x06, (byte) 0x69, (byte) 0x66, (byte) 0x6E, (byte) 0x61,
            (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x00, (byte) 0x12,
            (byte) 0x4C, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E, (byte) 0x67,
            (byte) 0x2F, (byte) 0x53, (byte) 0x74, (byte) 0x72, (byte) 0x69,
            (byte) 0x6E, (byte) 0x67, (byte) 0x3B, (byte) 0x5B, (byte) 0x00,
            (byte) 0x09, (byte) 0x69, (byte) 0x70, (byte) 0x61, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x74, (byte) 0x00, (byte) 0x02, (byte) 0x5B, (byte) 0x42,
            (byte) 0x78, (byte) 0x72, (byte) 0x00, (byte) 0x14, (byte) 0x6A,
            (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2E, (byte) 0x6E,
            (byte) 0x65, (byte) 0x74, (byte) 0x2E, (byte) 0x49, (byte) 0x6E,
            (byte) 0x65, (byte) 0x74, (byte) 0x41, (byte) 0x64, (byte) 0x64,
            (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x2D,
            (byte) 0x9B, (byte) 0x57, (byte) 0xAF, (byte) 0x9F, (byte) 0xE3,
            (byte) 0xEB, (byte) 0xDB, (byte) 0x02, (byte) 0x00, (byte) 0x03,
            (byte) 0x49, (byte) 0x00, (byte) 0x07, (byte) 0x61, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x49, (byte) 0x00, (byte) 0x06, (byte) 0x66, (byte) 0x61,
            (byte) 0x6D, (byte) 0x69, (byte) 0x6C, (byte) 0x79, (byte) 0x4C,
            (byte) 0x00, (byte) 0x08, (byte) 0x68, (byte) 0x6F, (byte) 0x73,
            (byte) 0x74, (byte) 0x4E, (byte) 0x61, (byte) 0x6D, (byte) 0x65,
            (byte) 0x71, (byte) 0x00, (byte) 0x7E, (byte) 0x00, (byte) 0x01,
            (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
            (byte) 0x74, (byte) 0x00, (byte) 0x25, (byte) 0x66, (byte) 0x65,
            (byte) 0x38, (byte) 0x30, (byte) 0x3A, (byte) 0x30, (byte) 0x3A,
            (byte) 0x30, (byte) 0x3A, (byte) 0x30, (byte) 0x3A, (byte) 0x32,
            (byte) 0x31, (byte) 0x62, (byte) 0x3A, (byte) 0x32, (byte) 0x34,
            (byte) 0x66, (byte) 0x66, (byte) 0x3A, (byte) 0x66, (byte) 0x65,
            (byte) 0x62, (byte) 0x64, (byte) 0x3A, (byte) 0x66, (byte) 0x32,
            (byte) 0x39, (byte) 0x63, (byte) 0x25, (byte) 0x65, (byte) 0x31,
            (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x67, (byte) 0x30,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x01,
            (byte) 0x01, (byte) 0x74, (byte) 0x00, (byte) 0x07, (byte) 0x65,
            (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x67,
            (byte) 0x30, (byte) 0x75, (byte) 0x72, (byte) 0x00, (byte) 0x02,
            (byte) 0x5B, (byte) 0x42, (byte) 0xAC, (byte) 0xF3, (byte) 0x17,
            (byte) 0xF8, (byte) 0x06, (byte) 0x08, (byte) 0x54, (byte) 0xE0,
            (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x70,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0xFE,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x1B, (byte) 0x24,
            (byte) 0xFF, (byte) 0xFE, (byte) 0xBD, (byte) 0xF2, (byte) 0x9C,
            (byte) 0x78 };

    // jdk8 generated serialization of address 0::1 on net if lo0 hostname
    // localhost scope_id 1

    static final byte[] SerialData_ifname_lo0 = { (byte) 0xAC, (byte) 0xED,
            (byte) 0x00, (byte) 0x05, (byte) 0x73, (byte) 0x72, (byte) 0x00,
            (byte) 0x15, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2E, (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x2E,
            (byte) 0x49, (byte) 0x6E, (byte) 0x65, (byte) 0x74, (byte) 0x36,
            (byte) 0x41, (byte) 0x64, (byte) 0x64, (byte) 0x72, (byte) 0x65,
            (byte) 0x73, (byte) 0x73, (byte) 0x5F, (byte) 0x7C, (byte) 0x20,
            (byte) 0x81, (byte) 0x52, (byte) 0x2C, (byte) 0x80, (byte) 0x21,
            (byte) 0x03, (byte) 0x00, (byte) 0x05, (byte) 0x49, (byte) 0x00,
            (byte) 0x08, (byte) 0x73, (byte) 0x63, (byte) 0x6F, (byte) 0x70,
            (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64, (byte) 0x5A,
            (byte) 0x00, (byte) 0x0C, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x64,
            (byte) 0x5F, (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x5A,
            (byte) 0x00, (byte) 0x10, (byte) 0x73, (byte) 0x63, (byte) 0x6F,
            (byte) 0x70, (byte) 0x65, (byte) 0x5F, (byte) 0x69, (byte) 0x66,
            (byte) 0x6E, (byte) 0x61, (byte) 0x6D, (byte) 0x65, (byte) 0x5F,
            (byte) 0x73, (byte) 0x65, (byte) 0x74, (byte) 0x4C, (byte) 0x00,
            (byte) 0x06, (byte) 0x69, (byte) 0x66, (byte) 0x6E, (byte) 0x61,
            (byte) 0x6D, (byte) 0x65, (byte) 0x74, (byte) 0x00, (byte) 0x12,
            (byte) 0x4C, (byte) 0x6A, (byte) 0x61, (byte) 0x76, (byte) 0x61,
            (byte) 0x2F, (byte) 0x6C, (byte) 0x61, (byte) 0x6E, (byte) 0x67,
            (byte) 0x2F, (byte) 0x53, (byte) 0x74, (byte) 0x72, (byte) 0x69,
            (byte) 0x6E, (byte) 0x67, (byte) 0x3B, (byte) 0x5B, (byte) 0x00,
            (byte) 0x09, (byte) 0x69, (byte) 0x70, (byte) 0x61, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x74, (byte) 0x00, (byte) 0x02, (byte) 0x5B, (byte) 0x42,
            (byte) 0x78, (byte) 0x72, (byte) 0x00, (byte) 0x14, (byte) 0x6A,
            (byte) 0x61, (byte) 0x76, (byte) 0x61, (byte) 0x2E, (byte) 0x6E,
            (byte) 0x65, (byte) 0x74, (byte) 0x2E, (byte) 0x49, (byte) 0x6E,
            (byte) 0x65, (byte) 0x74, (byte) 0x41, (byte) 0x64, (byte) 0x64,
            (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x2D,
            (byte) 0x9B, (byte) 0x57, (byte) 0xAF, (byte) 0x9F, (byte) 0xE3,
            (byte) 0xEB, (byte) 0xDB, (byte) 0x02, (byte) 0x00, (byte) 0x03,
            (byte) 0x49, (byte) 0x00, (byte) 0x07, (byte) 0x61, (byte) 0x64,
            (byte) 0x64, (byte) 0x72, (byte) 0x65, (byte) 0x73, (byte) 0x73,
            (byte) 0x49, (byte) 0x00, (byte) 0x06, (byte) 0x66, (byte) 0x61,
            (byte) 0x6D, (byte) 0x69, (byte) 0x6C, (byte) 0x79, (byte) 0x4C,
            (byte) 0x00, (byte) 0x08, (byte) 0x68, (byte) 0x6F, (byte) 0x73,
            (byte) 0x74, (byte) 0x4E, (byte) 0x61, (byte) 0x6D, (byte) 0x65,
            (byte) 0x71, (byte) 0x00, (byte) 0x7E, (byte) 0x00, (byte) 0x01,
            (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
            (byte) 0x74, (byte) 0x00, (byte) 0x09, (byte) 0x6C, (byte) 0x6F,
            (byte) 0x63, (byte) 0x61, (byte) 0x6C, (byte) 0x68, (byte) 0x6F,
            (byte) 0x73, (byte) 0x74, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x74, (byte) 0x00,
            (byte) 0x03, (byte) 0x6C, (byte) 0x6F, (byte) 0x30, (byte) 0x75,
            (byte) 0x72, (byte) 0x00, (byte) 0x02, (byte) 0x5B, (byte) 0x42,
            (byte) 0xAC, (byte) 0xF3, (byte) 0x17, (byte) 0xF8, (byte) 0x06,
            (byte) 0x08, (byte) 0x54, (byte) 0xE0, (byte) 0x02, (byte) 0x00,
            (byte) 0x00, (byte) 0x78, (byte) 0x70, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x78 };

}

class MockLo0Inet6Address {

    private static final byte[] LOOPBACKIPV6ADDRESS = { (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };

    private static final String LOCALHOSTNAME = "localhost";

    private static final String LO0HOSTADDRESS = "0:0:0:0:0:0:0:1%lo0";

    private static final String BARE_LO0HOSTADDRESS = "0:0:0:0:0:0:0:1";

    private static final String LO0HOSTADDRESS_WITHINDEX = "0:0:0:0:0:0:0:1%1";

    private static final int SCOPE_ID_LO0 = 1;

    private static final int SCOPE_ID_ZERO = 0;

    public static final String NETWORK_IF_LO0 = "lo0";

    static String getHostName() {
        return LOCALHOSTNAME;
    }

    static String getHostAddress() {
        return LO0HOSTADDRESS;
    }

    static String getBareHostAddress() {
        return BARE_LO0HOSTADDRESS;
    }

    static String getHostAddressWithIndex() {
        return LO0HOSTADDRESS_WITHINDEX;
    }

    static byte[] getAddress() {
        return LOOPBACKIPV6ADDRESS;
    }

    static int getScopeId() {
        return SCOPE_ID_LO0;
    }

    static int getScopeZero() {
        return SCOPE_ID_ZERO;
    }

    static String getScopeIfName() {
        return NETWORK_IF_LO0;
    }

}

class MockE1000g0Inet6Address {

    // fe80::21b:24ff:febd:f29c
    private static final byte[] E1000G0IPV6ADDRESS = { (byte) 0xfe,
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x1b, (byte) 0x24,
            (byte) 0xff, (byte) 0xfe, (byte) 0xbd, (byte) 0xf2, (byte) 0x9c };

    private static final String E1000G0HOSTNAME = "fe80:0:0:0:21b:24ff:febd:f29c%e1000g0";

    private static final String BARE_E1000G0HOSTADDRESS = "fe80:0:0:0:21b:24ff:febd:f29c";

    private static final String E1000G0HOSTADDRESS_WITHINDEX = "fe80:0:0:0:21b:24ff:febd:f29c%2";

    private static final String E1000G0HOSTADDRESS = "fe80:0:0:0:21b:24ff:febd:f29c%e1000g0";

    private static final String NETWORK_IF_E1000G0 = "e1000g0";

    private static final int SCOPE_ID_E1000G0 = 2;

    private static final int SCOPE_ID_ZERO = 0;

    static String getHostName() {
        return E1000G0HOSTNAME;
    }

    static String getHostAddress() {
        return E1000G0HOSTADDRESS;
    }

    static String getHostAddressWithIndex() {
        return E1000G0HOSTADDRESS_WITHINDEX;
    }

    static String getBareHostAddress() {
        return BARE_E1000G0HOSTADDRESS;
    }

    static byte[] getAddress() {
        return E1000G0IPV6ADDRESS;
    }

    static int getScopeId() {
        return SCOPE_ID_E1000G0;
    }

    static int getScopeZero() {
        return SCOPE_ID_ZERO;
    }

    static String getScopeIfName() {
        return NETWORK_IF_E1000G0;
    }

}
