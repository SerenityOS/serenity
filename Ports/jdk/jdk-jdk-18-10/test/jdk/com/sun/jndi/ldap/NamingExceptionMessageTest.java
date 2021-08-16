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

/*
 * @test
 * @bug 8062947
 * @summary Test that NamingException message text matches the failure reason
 * @library /test/lib lib
 * @run testng NamingExceptionMessageTest
 */

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;
import org.testng.Assert;
import jdk.test.lib.net.URIBuilder;

public class NamingExceptionMessageTest {

    @Test
    public void timeoutMessageTest() throws Exception {
        try (var ldapServer = TestLdapServer.newInstance(false)) {
            ldapServer.start();
            ldapServer.awaitStartup();
            var env = ldapServer.getInitialLdapCtxEnvironment(TIMEOUT_VALUE);
            var namingException = Assert.expectThrows(NamingException.class, () -> new InitialDirContext(env));
            System.out.println("Got naming exception:" + namingException);
            Assert.assertEquals(namingException.getMessage(), EXPECTED_TIMEOUT_MESSAGE);
        }
    }

    @Test
    public void connectionClosureMessageTest() throws Exception {
        try (var ldapServer = TestLdapServer.newInstance(true)) {
            ldapServer.start();
            ldapServer.awaitStartup();
            var env = ldapServer.getInitialLdapCtxEnvironment(0);
            var namingException = Assert.expectThrows(NamingException.class, () -> new InitialDirContext(env));
            if (namingException instanceof ServiceUnavailableException) {
                // If naming exception is ServiceUnavailableException it could mean
                // that the connection was closed on test server-side before LDAP client starts
                // read-out of the reply message. For such cases test run is considered as successful.
                System.out.println("Got ServiceUnavailableException: Test PASSED");
            } else {
                // If exception is not ServiceUnavailableException - check the exception message
                System.out.println("Got NamingException:" + namingException);
                Assert.assertEquals(namingException.getMessage(), EXPECTED_CLOSURE_MESSAGE);
            }
        }
    }

    // Test LDAP server
    private static class TestLdapServer extends BaseLdapServer {

        private final boolean closeConnections;
        private final CountDownLatch startupLatch = new CountDownLatch(1);

        public Hashtable<Object, Object> getInitialLdapCtxEnvironment(int readTimeoutValue) {
            // Create environment for initial LDAP context
            Hashtable<Object, Object> env = new Hashtable<>();

            // Activate LDAPv3
            env.put("java.naming.ldap.version", "3");

            // De-activate the ManageDsaIT control
            env.put(Context.REFERRAL, "follow");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, getUrlString());
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "name");
            env.put(Context.SECURITY_CREDENTIALS, "pwd");

            if (readTimeoutValue > 0) {
                env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(readTimeoutValue));
                env.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(readTimeoutValue));
            }

            return env;
        }

        private String getUrlString() {
            String url = URIBuilder.newBuilder()
                    .scheme("ldap")
                    .loopback()
                    .port(getPort())
                    .buildUnchecked()
                    .toString();
            return url;
        }

        public static TestLdapServer newInstance(boolean closeConnections) throws IOException {
            ServerSocket srvSock = new ServerSocket();
            srvSock.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            return new TestLdapServer(srvSock, closeConnections);
        }

        void awaitStartup() throws InterruptedException {
            startupLatch.await();
        }

        private TestLdapServer(ServerSocket serverSocket, boolean closeConnections) {
            super(serverSocket);
            this.closeConnections = closeConnections;

        }

        @Override
        protected void beforeAcceptingConnections() {
            startupLatch.countDown();
        }

        @Override
        protected void handleRequest(Socket socket,
                                     LdapMessage msg,
                                     OutputStream out)
                throws IOException {
            switch (msg.getOperation()) {
                case BIND_REQUEST:
                    if (closeConnections) {
                        // Give some time for LDAP client to start-up
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        // Close the socket
                        closeSilently(socket);
                    } else {
                        try {
                            TimeUnit.DAYS.sleep(Integer.MAX_VALUE);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                default:
                    break;
            }
        }
    }

    // Expected message for case when connection is closed on server side
    private static final String EXPECTED_CLOSURE_MESSAGE = "LDAP connection has been closed";
    // read and connect timeouts value
    private static final int TIMEOUT_VALUE = 129;
    // Expected message text when connection is timed-out
    private static final String EXPECTED_TIMEOUT_MESSAGE = String.format(
            "LDAP response read timed out, timeout used: %d ms.", TIMEOUT_VALUE);
}
