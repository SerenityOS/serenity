/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047031
 * @key intermittent
 * @summary SocketPermission tests for legacy socket types.
 *          This test needs to bind its servers to the wildcard
 *          address and as such may fail intermittently.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run testng/othervm -Djava.security.manager=allow SocketPermissionTest
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Optional;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import static jdk.test.lib.NetworkConfiguration.probe;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SocketPermissionTest {

    @BeforeMethod
    public void setupSecurityManager() throws Exception {
        // All permissions, a specific ACC will be used to when testing
        // with a reduced permission set.
        Policy.setPolicy(new Policy() {
             final PermissionCollection perms = new Permissions();
             { perms.add(new java.security.AllPermission()); }
             public PermissionCollection getPermissions(ProtectionDomain domain) {
                 return perms;
             }
             public PermissionCollection getPermissions(CodeSource codesource) {
                 return perms;
             }
             public boolean implies(ProtectionDomain domain, Permission perm) {
                 return perms.implies(perm);
             }
        } );
        System.setSecurityManager(new SecurityManager());
    }

    static final AccessControlContext RESTRICTED_ACC = getAccessControlContext();

    @Test
    public void connectSocketTest() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();

            String addr = "localhost:" + port;
            AccessControlContext acc = getAccessControlContext(
                    new SocketPermission(addr, "listen,connect,resolve"));

            // Positive
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                try (Socket client = new Socket(InetAddress.getLocalHost(), port)) {
                }
                return null;
            }, acc);

            //Negative
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    Socket client = new Socket(InetAddress.getLocalHost(), port);
                    fail("Expected SecurityException");
                    return null;
                }, RESTRICTED_ACC);
            } catch (SecurityException expected) { }
        }
    }

    @Test
    public void connectDatagramSocketTest() throws Exception {
        byte[] msg = "Hello".getBytes(UTF_8);
        InetAddress lh = InetAddress.getLocalHost();

        try (DatagramSocket ds = new DatagramSocket(0)) {
            int port = ds.getLocalPort();

            String addr = lh.getHostAddress() + ":" + port;
            AccessControlContext acc = getAccessControlContext(
                    new SocketPermission(addr, "connect,resolve"));

            // Positive
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                DatagramPacket dp = new DatagramPacket(msg, msg.length, lh, port);
                ds.send(dp);
                return null;
            }, acc);

            // Negative
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    DatagramPacket dp = new DatagramPacket(msg, msg.length, lh, port);
                    ds.send(dp);
                    fail("Expected SecurityException");
                    return null;
                }, RESTRICTED_ACC);
            } catch (SecurityException expected) { }
        }
    }

    @Test
    public void acceptServerSocketTest() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();

            String addr = "localhost:" + port;
            AccessControlContext acc = getAccessControlContext(
                    new SocketPermission(addr, "listen,connect,resolve"),
                    new SocketPermission("localhost:1024-", "accept"));

            // Positive
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                InetAddress me = InetAddress.getLocalHost();
                try (Socket client = new Socket(me, port)) {
                    ss.accept();
                }
                return null;
            }, acc);

            // Negative
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    InetAddress me = InetAddress.getLocalHost();
                    try (Socket client = new Socket(me, port)) {
                        ss.accept();
                    }
                    fail("Expected SecurityException");
                    return null;
                }, RESTRICTED_ACC);
            } catch (SecurityException expected) { }
        }
    }

    @Test
    public void sendDatagramPacketTest() throws Exception {
        byte[] msg = "Hello".getBytes(UTF_8);
        InetAddress group = InetAddress.getByName("229.227.226.221");

        try (DatagramSocket ds = new DatagramSocket(0)) {
            int port = ds.getLocalPort();

            String addr = "localhost:" + port;
            //test for SocketPermission "229.227.226.221", "connect,accept"
            AccessControlContext acc = getAccessControlContext(
                    new SocketPermission(addr, "listen,resolve"),
                    new SocketPermission("229.227.226.221", "connect,accept"));

            // Positive
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                DatagramPacket hi = new DatagramPacket(msg, msg.length, group, port);
                ds.send(hi);
                return null;
            }, acc);

            // Negative
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    DatagramPacket hi = new DatagramPacket(msg, msg.length, group, port);
                    ds.send(hi);
                    fail("Expected SecurityException");
                    return null;
                }, RESTRICTED_ACC);
            } catch (SecurityException expected) { }
        }
    }

    @Test
    public void joinGroupMulticastTest() throws Exception {
        InetAddress group = InetAddress.getByName("229.227.226.221");
        try (MulticastSocket s = new MulticastSocket(0)) {
            int port = s.getLocalPort();

            String addr = "localhost:" + port;
            AccessControlContext acc = getAccessControlContext(
                    new SocketPermission(addr, "listen,resolve"),
                    new SocketPermission("229.227.226.221", "connect,accept"));

            // Positive ( requires a functional network interface )
            Optional<NetworkInterface> onif = probe().ip4MulticastInterfaces().findFirst();
            if (!onif.isPresent()) {
                s.setNetworkInterface(onif.get());

                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    s.joinGroup(group);
                    s.leaveGroup(group);
                    return null;
                }, acc);
            }

            // Negative
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                    s.joinGroup(group);
                    s.leaveGroup(group);
                    fail("Expected SecurityException");
                    return null;
                }, RESTRICTED_ACC);
            } catch (SecurityException expected) { }
        }

    }

    @Test
    public void listenDatagramSocketTest() throws Exception {
        // the hardcoded port number doesn't really matter since we expect the
        // security permission to be checked before the underlying operation.
        int port = 8899;
        String addr = "localhost:" + port;
        AccessControlContext acc = getAccessControlContext(
                new SocketPermission(addr, "listen"));

        // Positive
        AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
            try (DatagramSocket ds = new DatagramSocket(port)) { }
            catch (IOException intermittentlyExpected) { /* ignore */ }
            return null;
        }, acc);

        // Negative
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                try (DatagramSocket ds = new DatagramSocket(port)) { }
                catch (IOException intermittentlyExpected) { /* ignore */ }
                fail("Expected SecurityException");
                return null;
            }, RESTRICTED_ACC);
        } catch (SecurityException expected) { }
    }

    @Test
    public void listenMulticastSocketTest() throws Exception {
        // the hardcoded port number doesn't really matter since we expect the
        // security permission to be checked before the underlying operation.
        int port = 8899;
        String addr = "localhost:" + port;
        AccessControlContext acc = getAccessControlContext(
                new SocketPermission(addr, "listen"));

        // Positive
        AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
            try (MulticastSocket ms = new MulticastSocket(port)) { }
            catch (IOException intermittentlyExpected) { /* ignore */ }
            return null;
        }, acc);

        // Negative
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                try (MulticastSocket ms = new MulticastSocket(port)) { }
                catch (IOException intermittentlyExpected) { /* ignore */ }
                fail("Expected SecurityException");
                return null;
            }, RESTRICTED_ACC);
        } catch (SecurityException expected) { }
    }

    @Test
    public void listenServerSocketTest() throws Exception {
        // the hardcoded port number doesn't really matter since we expect the
        // security permission to be checked before the underlying operation.
        int port = 8899;
        String addr = "localhost:" + port;
        AccessControlContext acc = getAccessControlContext(
                new SocketPermission(addr, "listen"));

        // Positive
        AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
            try (ServerSocket ss = new ServerSocket(port)) { }
            catch (IOException intermittentlyExpected) { /* ignore */ }
            return null;
        }, acc);

        // Negative
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                try (ServerSocket ss = new ServerSocket(port)) { }
                catch (IOException intermittentlyExpected) { /* ignore */ }
                fail("Expected SecurityException");
                return null;
            }, RESTRICTED_ACC);
        } catch (SecurityException expected) { }

    }

    private static AccessControlContext getAccessControlContext(Permission... ps) {
        Permissions perms = new Permissions();
        for (Permission p : ps) {
            perms.add(p);
        }
        /*
         *Create an AccessControlContext that consist a single protection domain
         * with only the permissions calculated above
         */
        ProtectionDomain pd = new ProtectionDomain(null, perms);
        return new AccessControlContext(new ProtectionDomain[]{pd});
    }

    // Standalone entry point for running with, possibly older, JDKs.
    public static void main(String[] args) throws Throwable {
        SocketPermissionTest test = new SocketPermissionTest();
        test.setupSecurityManager();
        for (java.lang.reflect.Method m : SocketPermissionTest.class.getDeclaredMethods()) {
            if (m.getAnnotation(Test.class) != null) {
                System.out.println("Invoking " + m.getName());
                m.invoke(test);
            }
        }
    }
}
