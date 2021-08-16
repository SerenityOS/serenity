/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8225499 4464064
 * @library /test/lib
 * @summary InetSocketAddress::toString not friendly to IPv6 literal addresses
 * @run testng/othervm ToString
 * @run testng/othervm -Djava.net.preferIPv4Stack=true ToString
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true ToString
 */

import java.net.*;

import jdk.test.lib.net.IPSupport;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ToString {

    private static final String loopbackAddr;
    private static final String wildcardAddr;
    private static final String localAddr;

    static {
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            String addr = loopback.getHostAddress();
            if (loopback instanceof Inet6Address) {
                addr = "[" + addr + "]";
            }
            loopbackAddr = addr;

            InetSocketAddress isa = new InetSocketAddress((InetAddress) null, 80);
            addr = isa.getAddress().toString();
            if (isa.getAddress() instanceof Inet6Address) {
                addr = "::/[0:0:0:0:0:0:0:0]";
            }
            wildcardAddr = addr;

            InetAddress ia = InetAddress.getLocalHost();
            addr = ia.toString();
            if (ia instanceof Inet6Address) {
                addr = ia.getHostName() + "/[" + ia.getHostAddress() + "]";
            }
            localAddr = addr;

        } catch (UnknownHostException uhe) {
            throw new RuntimeException(uhe);
        }
    }

    @BeforeTest
    public void setup() {
        IPSupport.throwSkippedExceptionIfNonOperational();
    }

    @Test
    // InetSocketAddress.toString() throws NPE with unresolved address
    public static void NPETest() {
        System.out.println(new InetSocketAddress("unresolved", 12345));
    }

    @DataProvider(name = "hostPortArgs")
    public Object[][] createArgs1() {
        return new Object[][]{
                // hostname, port number, expected string in format
                // <hostname>/<IP literal>:<port> or
                // <hostname>/<unresolved>:<port> if address is unresolved
                {"::1", 80, "/[0:0:0:0:0:0:0:1]:80"},
                {"fedc:ba98:7654:3210:fedc:ba98:7654:3210", 80, "/[fedc:ba98:7654:3210:fedc:ba98:7654:3210]:80"},
                {"::192.9.5.5", 80, "/[0:0:0:0:0:0:c009:505]:80"},
                {"127.0.0.1", 80, "/127.0.0.1:80"},
                {"::ffff:192.0.2.128", 80, "/192.0.2.128:80"},
                {"0", 80, "/0.0.0.0:80"},
                {":", 80, ":/<unresolved>:80"},
                {":1", 80, ":1/<unresolved>:80"}
        };
    }

    @Test(dataProvider = "hostPortArgs")
    public static void testConstructor(String host, int port, String string) {
        String received = new InetSocketAddress(host, port).toString();

        if (!string.equals(received)) {
            throw new RuntimeException("Expected: " + string + " Received: " + received);
        }
    }

    @DataProvider(name = "addrPortArgs")
    public Object[][] createArgs2() {
        InetAddress nullAddr = null;
        try {
            return new Object[][]{
                    // InetAddress, port number, expected string
                    {InetAddress.getLoopbackAddress(), 80, "localhost/" + loopbackAddr + ":80"},
                    {InetAddress.getLocalHost(), 80, localAddr + ":80"},
                    {InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), 80, "/1.1.1.1:80"},
                    {InetAddress.getByAddress(new byte[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}), 80, "/[101:101:101:101:101:101:101:101]:80"},
                    {InetAddress.getByName("225.225.225.0"), 80, "/225.225.225.0:80"},
                    {nullAddr, 80, wildcardAddr + ":80"}
            };
        } catch (UnknownHostException uhe) {
            throw new RuntimeException("Data provider creation failed: " + uhe, uhe);
        }
    }

    @Test(dataProvider = "addrPortArgs")
    public static void testConstructor(InetAddress addr, int port, String string) {
        String received = new InetSocketAddress(addr, port).toString();

        if (!string.equals(received)) {
            throw new RuntimeException("Expected: " + string + " Received: " + received);
        }
    }

    @DataProvider(name = "unresolved")
    public Object[][] createArgs3() {
        return new Object[][]{
                // hostname, port number, expected string
                {"::1", 80, "::1/<unresolved>:80"},
                {"fedc:ba98:7654:3210:fedc:ba98:7654:3210", 80, "fedc:ba98:7654:3210:fedc:ba98:7654:3210/<unresolved>:80"},
                {"::192.9.5.5", 80, "::192.9.5.5/<unresolved>:80"},
                {"127.0.0.1", 80, "127.0.0.1/<unresolved>:80"},
                {"::ffff:192.0.2.128", 80, "::ffff:192.0.2.128/<unresolved>:80"},
                {"0", 80, "0/<unresolved>:80"},
                {"foo", 80, "foo/<unresolved>:80"},
                {":", 80, ":/<unresolved>:80"},
                {":1", 80, ":1/<unresolved>:80"}
        };
    }

    @Test(dataProvider = "unresolved")
    public static void testCreateUnresolved(String host, int port, String string) {
        String received = InetSocketAddress.createUnresolved(host, port).toString();

        if (!string.equals(received)) {
            throw new RuntimeException("Expected: " + string + " Received: " + received);
        }
    }
}
