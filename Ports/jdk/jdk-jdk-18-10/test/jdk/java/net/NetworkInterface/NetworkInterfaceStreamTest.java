/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8081678 8131155
 * @summary Tests for stream returning methods
 * @library /lib/testlibrary/bootlib /test/lib
 * @build java.base/java.util.stream.OpTestCase
 * @run testng/othervm NetworkInterfaceStreamTest
 * @run testng/othervm -Djava.net.preferIPv4Stack=true NetworkInterfaceStreamTest
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;

import jdk.test.lib.net.IPSupport;

public class NetworkInterfaceStreamTest extends OpTestCase {

    private final static boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    @BeforeTest
    void setup() {
        IPSupport.throwSkippedExceptionIfNonOperational();
    }

    @Test
    public void testNetworkInterfaces() throws SocketException {
        Supplier<Stream<NetworkInterface>> ss = () -> {
            try {
                return NetworkInterface.networkInterfaces()
                        .filter(ni -> isIncluded(ni));
            }
            catch (SocketException e) {
                throw new RuntimeException(e);
            }
        };

        Collection<NetworkInterface> enums = Collections.list(NetworkInterface.getNetworkInterfaces());
        Collection<NetworkInterface> expected = new ArrayList<>();
        enums.forEach(ni -> {
            if (isIncluded(ni)) {
                expected.add(ni);
            }
        });
        withData(TestData.Factory.ofSupplier("Top-level network interfaces", ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();
    }

    private Collection<NetworkInterface> getAllNetworkInterfaces() throws SocketException {
        Collection<NetworkInterface> anis = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            getAllSubNetworkInterfaces(ni, anis);
        }
        return anis;
    }

    private void getAllSubNetworkInterfaces(NetworkInterface ni, Collection<NetworkInterface> result) {
        if (isIncluded(ni)) {
            result.add(ni);
        }

        for (NetworkInterface sni : Collections.list(ni.getSubInterfaces())) {
            getAllSubNetworkInterfaces(sni, result);
        }
    }

    private Stream<NetworkInterface> allNetworkInterfaces() throws SocketException {
        return NetworkInterface.networkInterfaces()
                .filter(ni -> isIncluded(ni))
                .flatMap(this::allSubNetworkInterfaces);
    }

    private Stream<NetworkInterface> allSubNetworkInterfaces(NetworkInterface ni) {
        return Stream.concat(
                Stream.of(ni),
                ni.subInterfaces().filter(sni -> isIncluded(sni)).flatMap(this::allSubNetworkInterfaces));
    }

    @Test
    public void testSubNetworkInterfaces() throws SocketException {
        Supplier<Stream<NetworkInterface>> ss = () -> {
            try {
                return allNetworkInterfaces();
            }
            catch (SocketException e) {
                throw new RuntimeException(e);
            }
        };

        Collection<NetworkInterface> expected = getAllNetworkInterfaces();
        withData(TestData.Factory.ofSupplier("All network interfaces", ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();
    }


    @Test
    public void testInetAddresses() throws SocketException {
        Supplier<Stream<InetAddress>> ss = () -> {
            try {
                return NetworkInterface.networkInterfaces()
                        .filter(ni -> isIncluded(ni))
                        .flatMap(NetworkInterface::inetAddresses);
            }
            catch (SocketException e) {
                throw new RuntimeException(e);
            }
        };

        Collection<NetworkInterface> nis = Collections.list(NetworkInterface.getNetworkInterfaces());
        Collection<InetAddress> expected = new ArrayList<>();
        for (NetworkInterface ni : nis) {
            if (isIncluded(ni)) {
                expected.addAll(Collections.list(ni.getInetAddresses()));
            }
        }
        withData(TestData.Factory.ofSupplier("All inet addresses", ss))
                .stream(s -> s)
                .expectedResult(expected)
                .exercise();
    }

    /**
     * Check if the input network interface should be included in the test. It is necessary to exclude
     * "Teredo Tunneling Pseudo-Interface" whose configuration can be variable during a test run.
     *
     * @param ni a network interace
     * @return false if it is a "Teredo Tunneling Pseudo-Interface", otherwise true.
     */
    private boolean isIncluded(NetworkInterface ni) {
        if (!IS_WINDOWS) {
            return true;
        }

        String dName = ni.getDisplayName();
        return dName == null || !dName.contains("Teredo");
    }

}

