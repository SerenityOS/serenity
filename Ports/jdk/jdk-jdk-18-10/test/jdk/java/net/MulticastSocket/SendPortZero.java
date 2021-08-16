/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketPermission;
import java.nio.channels.DatagramChannel;
import java.security.AccessControlException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;

import static org.testng.Assert.assertThrows;

/*
 * @test
 * @bug 8243408
 * @summary Check that MulticastSocket throws expected
 *          Exception when sending a DatagramPacket with port 0
 * @run testng/othervm -Djava.security.manager=allow SendPortZero
 */

public class SendPortZero {
    private InetAddress loopbackAddr, wildcardAddr;
    private MulticastSocket multicastSocket;
    private DatagramPacket loopbackZeroPkt, wildcardZeroPkt, wildcardValidPkt;

    private static final Class<SocketException> SE = SocketException.class;
    private static final Class<AccessControlException> ACE =
            AccessControlException.class;

    @BeforeTest
    public void setUp() throws IOException {
        multicastSocket = new MulticastSocket();

        byte[] buf = "test".getBytes();

        // Addresses
        loopbackAddr = InetAddress.getLoopbackAddress();
        //wildcardAddr = new InetSocketAddress(0).getAddress();

        // Packets
        // loopback w/port 0
        loopbackZeroPkt = new DatagramPacket(buf, 0, buf.length);
        loopbackZeroPkt.setAddress(loopbackAddr);
        loopbackZeroPkt.setPort(0);

        /*
        //Commented until JDK-8236852 is fixed

        // wildcard w/port 0
        wildcardZeroPkt = new DatagramPacket(buf, 0, buf.length);
        wildcardZeroPkt.setAddress(wildcardAddr);
        wildcardZeroPkt.setPort(0);

        //Commented until JDK-8236807 is fixed

        // wildcard addr w/valid port
        wildcardValidPkt = new DatagramPacket(buf, 0, buf.length);
        var addr = socket.getAddress();
        wildcardValidPkt.setAddress(addr);
        wildcardValidPkt.setPort(socket.getLocalPort());
      */
    }

    @DataProvider(name = "data")
    public Object[][] variants() {
        return new Object[][]{
                { multicastSocket,       loopbackZeroPkt }
        };
    }

    @Test(dataProvider = "data")
    public void testSend(MulticastSocket ms, DatagramPacket pkt) {
        assertThrows(SE, () -> ms.send(pkt));
        assertThrows(SE, () -> ms.send(pkt, (byte) 0));
    }

    // Check that 0 port check doesn't override security manager check
    @Test(dataProvider = "data")
    public void testSendWithSecurityManager(MulticastSocket ms,
                                            DatagramPacket pkt) {
        Policy defaultPolicy = Policy.getPolicy();
        try {
            Policy.setPolicy(new NoSendPolicy());
            System.setSecurityManager(new SecurityManager());

            assertThrows(ACE, () -> ms.send(pkt));
            assertThrows(ACE, () -> ms.send(pkt, (byte) 0));
        } finally {
            System.setSecurityManager(null);
            Policy.setPolicy(defaultPolicy);
        }
    }

    static class NoSendPolicy extends Policy {
        final PermissionCollection perms = new Permissions();
        { perms.add(
                new SocketPermission("*:0", "connect")); }

        public boolean implies(ProtectionDomain domain, Permission perm) {
            return !perms.implies(perm);
        }
    }

    @AfterTest
    public void tearDown() {
        multicastSocket.close();
    }
}
