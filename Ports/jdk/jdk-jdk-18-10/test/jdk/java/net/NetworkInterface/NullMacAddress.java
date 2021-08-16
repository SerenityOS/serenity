/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8059309
 * @summary Test that querrying the mac address of the loopback interface
 *          returns null and doesn't throw a SocketException.
 * @library /test/lib
 * @run testng/othervm NullMacAddress
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true NullMacAddress
 * @run testng/othervm -Djava.net.preferIPv4Stack=true NullMacAddress
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Locale;

import jdk.test.lib.net.IPSupport;

public class NullMacAddress {

    @BeforeTest
    void setup() {
        IPSupport.throwSkippedExceptionIfNonOperational();
    }

    @Test
    public void testNetworkInterfaces() throws SocketException {
        NetworkInterface.networkInterfaces().forEach(this::testMacAddress);
    }

    private void testMacAddress(NetworkInterface ni) {
        try {
            var name = ni.getDisplayName();
            System.out.println("Testing: " + name);
            var loopback = ni.isLoopback();
            var macAddress = ni.getHardwareAddress();
            var hdr = macAddress == null ? "null"
                    : "0x" + new BigInteger(1, macAddress)
                    .toString(16).toUpperCase(Locale.ROOT);
            System.out.println("   MAC address: " + hdr + (loopback ? " (loopback)" : ""));
            if (loopback) {
                assertNull(macAddress, "Loopback interface \""
                        + name + "\" doesn't have a null MAC Address");
            }
        } catch (SocketException ex) {
            throw new UncheckedIOException(ex);
        }
    }
 }

