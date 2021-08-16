/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketPermission;
import java.nio.channels.ServerSocketChannel;
import java.security.AccessControlContext;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static java.security.AccessController.*;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8224730
 * @summary Check local address access with a security manager
 * @run testng/othervm -Djava.security.manager=allow TestLocalAddress
 */

public class TestLocalAddress {

    InetAddress localHost;
    ExposedSecurityManager exposedSecurityManager;

    @BeforeTest
    public void setup() throws Exception {
        localHost = InetAddress.getLocalHost();
        out.println("localHost: " + localHost);

        Policy.setPolicy(new AllPermissionsPolicy());
        exposedSecurityManager = new ExposedSecurityManager();
        System.setSecurityManager(exposedSecurityManager);
        out.println("Security manager set");
    }

    @Test
    public void serverSocketNoSecurityManager() throws Exception {
        out.println("\n\n--- serverSocketNoSecurityManager ---");
        try (ServerSocket ss = new ServerSocket()) {
            testWithNoSecurityManager(ss);
        }
    }

    @Test
    public void serverSocketAdapterNoSecurityManager() throws Exception {
        out.println("\n\n--- serverSocketAdapterNoSecurityManager ---");
        try (ServerSocket ss = ServerSocketChannel.open().socket()) {
            testWithNoSecurityManager(ss);
        }
    }

    void testWithNoSecurityManager(ServerSocket ss) throws Exception {
        final SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(null);
        try {
            ss.bind(new InetSocketAddress(localHost, 0));

            var localSocketAddr = ((InetSocketAddress)ss.getLocalSocketAddress());
            var localInetAddress = ss.getInetAddress();
            assertEquals(localInetAddress, localSocketAddr.getAddress());
            if (!(localHost.equals(InetAddress.getLoopbackAddress())))
                assertNotEquals(localInetAddress, InetAddress.getLoopbackAddress());

            // toString
            String s = ss.toString();
            out.println("toString returned:" + s);
            assertTrue(s.contains(localInetAddress.toString()),
                    "Expected [" + localInetAddress + "] in " + s);

        } finally {
            System.setSecurityManager(sm);
        }
    }

    @Test
    public void serverSocketNoPermissions() throws Exception {
        out.println("\n\n--- serverSocketNoPermissions ---");
        try (ServerSocket ss = new ServerSocket()) {
            testWithNoPermissions(ss);
        }
    }

    @Test
    public void serverSocketAdapterNoPermissions() throws Exception {
        out.println("\n\n--- serverSocketAdapterNoPermissions ---");
        try (ServerSocket ss = ServerSocketChannel.open().socket()) {
            testWithNoPermissions(ss);
        }
    }

    void testWithNoPermissions(ServerSocket ss) throws Exception {
        ss.bind(new InetSocketAddress(localHost, 0));

        PrivilegedExceptionAction<SocketAddress> pa = ss::getLocalSocketAddress;
        var localSocketAddr = (InetSocketAddress) doPrivileged(pa, noPermissions());
        assertSecurityManagerCalled();
        PrivilegedExceptionAction<InetAddress> pa1 = ss::getInetAddress;
        var localInetAddress = doPrivileged(pa1, noPermissions());
        assertSecurityManagerCalled();

        assertEquals(localInetAddress, localSocketAddr.getAddress());
        assertEquals(localInetAddress, InetAddress.getLoopbackAddress());

        // toString
        PrivilegedExceptionAction<String> pa2 = ss::toString;
        String s = doPrivileged(pa2, noPermissions());
        assertSecurityManagerCalled();
        out.println("toString returned:" + s);
        assertTrue(s.contains(localInetAddress.toString()),
                "Expected [" + localInetAddress + "] in " + s);
    }


    @Test
    public void serverSocketFineGrainPermissions() throws Exception {
        out.println("\n\n--- serverSocketFineGrainPermissions ---");
        try (ServerSocket ss = new ServerSocket()) {
            testWithFineGrainPermissions(ss);
        }
    }

    @Test
    public void serverSocketAdapterFineGrainPermissions() throws Exception {
        out.println("\n\n--- serverSocketAdapterFineGrainPermissions ---");
        try (ServerSocket ss = ServerSocketChannel.open().socket()) {
            testWithFineGrainPermissions(ss);
        }
    }

    void testWithFineGrainPermissions(ServerSocket ss) throws Exception {
        AccessControlContext connectPermission = withPermissions(
                new SocketPermission(localHost.getHostName(), "connect")
        );
        ss.bind(new InetSocketAddress(localHost, 0));

        PrivilegedExceptionAction<SocketAddress> pa = ss::getLocalSocketAddress;
        var localSocketAddr = (InetSocketAddress) doPrivileged(pa, connectPermission);
        assertSecurityManagerCalled();
        PrivilegedExceptionAction<InetAddress> pa1 = ss::getInetAddress;
        var localInetAddress = doPrivileged(pa1, connectPermission);
        assertSecurityManagerCalled();

        assertEquals(localInetAddress, localSocketAddr.getAddress());
        assertEquals(localInetAddress, localHost);

        // toString
        PrivilegedExceptionAction<String> pa2 = ss::toString;
        String s = doPrivileged(pa2, connectPermission);
        assertSecurityManagerCalled();
        out.println("toString returned:" + s);
        assertTrue(s.contains(localInetAddress.toString()),
                "Expected [" + localInetAddress + "] in " + s);
    }


    @Test
    public void serverSocketUnbound() throws Exception {
        out.println("\n\n--- serverSocketUnbound ---");
        try (ServerSocket ss = new ServerSocket()) {
            testUnbound(ss);
        }
    }

    @Test
    public void serverSocketAdapterUnbound() throws Exception {
        out.println("\n\n--- serverSocketAdapterUnbound ---");
        try (ServerSocket ss = ServerSocketChannel.open().socket()) {
            testUnbound(ss);
        }
    }

    void testUnbound(ServerSocket ss) {
        assert !ss.isBound();
        exposedSecurityManager.reset();
        assertEquals(ss.getLocalSocketAddress(), null);
        assertEquals(exposedSecurityManager.port, -999);
        assertEquals(ss.getInetAddress(), null);
        assertEquals(exposedSecurityManager.port, -999);
        String s = ss.toString();
        assertEquals(exposedSecurityManager.port, -999);
        out.println("toString returned:" + s);
        assertTrue(s.contains("unbound"), "Expected [unbound] in " + s);
    }

    // A security manager that allows inspection of checkConnect's host/port.
    static class ExposedSecurityManager extends SecurityManager {
        volatile String host;
        volatile int port;
        ExposedSecurityManager() {
            reset();
        }
        @Override
        public void checkConnect(String host, int port) {
            this.host = host;
            this.port = port;
            super.checkConnect(host, port);
        }
        void reset() {
            host = "reset";
            port = -999;
        }
    }

    void assertSecurityManagerCalled() {
        assertEquals(exposedSecurityManager.port, -1);
        assertEquals(exposedSecurityManager.host, localHost.getHostAddress());
        exposedSecurityManager.reset();
    }

    @Test
    // Ensures that the test machinery is operating as expected.
    public void sanity() {
        PrivilegedAction<?> connectAction = () -> {
            System.getSecurityManager().checkConnect("example.com", 80);
            return null;
        };

        try {
            doPrivileged(connectAction, allPermissions());
        } catch (SecurityException unexpected) {
            throw unexpected;
        }
        try {
            doPrivileged(connectAction, noPermissions());
            fail("Expected exception not thrown");
        } catch (SecurityException expected) { }
        try {
            doPrivileged(connectAction,
                    withPermissions(new SocketPermission("example.com:80", "connect")));
        } catch (SecurityException unexpected) {
            throw unexpected;
        }
    }

    static AccessControlContext withPermissions(Permission... perms) {
        Permissions p = new Permissions();
        for (Permission perm : perms) {
            p.add(perm);
        }
        ProtectionDomain pd = new ProtectionDomain(null, p);
        return new AccessControlContext(new ProtectionDomain[]{ pd });
    }

    static AccessControlContext allPermissions() {
        return withPermissions(new AllPermission());
    }

    static AccessControlContext noPermissions() {
        return withPermissions(/*empty*/);
    }

    // A Policy that implies all permissions.
    static class AllPermissionsPolicy extends Policy {
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return true;
        }
    }
}
