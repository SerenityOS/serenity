/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8141370
 * @key intermittent
 * @library /test/lib
 * @build DeadSSLSocketFactory
 * @run main/othervm DeadSSLLdapTimeoutTest
 */

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.SSLHandshakeException;

import jdk.test.lib.net.URIBuilder;

import static java.util.concurrent.TimeUnit.NANOSECONDS;


class DeadServerTimeoutSSLTest implements Callable<Boolean> {

    Hashtable<Object, Object> env;
    DeadSSLServer server;
    boolean passed = false;

    public DeadServerTimeoutSSLTest(Hashtable<Object, Object> env) throws IOException {
        SocketAddress sockAddr = new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0);
        this.server = new DeadSSLServer(sockAddr);
        this.env = env;
    }

    public void handleNamingException(NamingException e) {
        if (e.getCause() instanceof SocketTimeoutException
                || e.getCause().getCause() instanceof SocketTimeoutException) {
            // SSL connect will timeout via readReply using
            // SocketTimeoutException
            System.out.println("PASS: Observed expected SocketTimeoutException");
            pass();
        } else if (e.getCause() instanceof SSLHandshakeException
                && e.getCause().getCause() instanceof EOFException) {
            // test seems to be failing intermittently on some
            // platforms.
            System.out.println("PASS: Observed expected SSLHandshakeException/EOFException");
            pass();
        } else {
            fail(e);
        }
    }

    public void pass() {
        this.passed = true;
    }

    public void fail() {
        throw new RuntimeException("Test failed");
    }

    public void fail(Exception e) {
        System.err.println("FAIL: Unexpected exception was observed:" + e.getMessage());
        throw new RuntimeException("Test failed", e);
    }

    boolean shutItDown(InitialContext ctx) {
        try {
            if (ctx != null) ctx.close();
            return true;
        } catch (NamingException ex) {
            return false;
        }
    }

    public Boolean call() {
        InitialContext ctx = null;

        try {
            server.serverStarted.await(); // Wait for the server to start-up
            Thread.sleep(200); // to be sure

            env.put(Context.PROVIDER_URL,
                    URIBuilder.newBuilder()
                            .scheme("ldap")
                            .loopback()
                            .port(server.getLocalPort())
                            .buildUnchecked().toString()
            );

            long start = System.nanoTime();
            try {
                ctx = new InitialDirContext(env);
                fail();
            } catch (NamingException e) {
                long end = System.nanoTime();
                System.out.println(this.getClass().toString() + " - elapsed: "
                        + NANOSECONDS.toMillis(end - start));
                handleNamingException(e);
            } finally {
                // Stop the server side thread
                server.testDone.countDown();
                shutItDown(ctx);
                server.close();
            }
            return passed;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

class DeadSSLServer extends Thread {
    ServerSocket serverSock;
    // Latch to be used by client to wait for server to start
    CountDownLatch serverStarted = new CountDownLatch(1);

    // Latch to be used by server thread to wait for client to finish testing
    CountDownLatch testDone = new CountDownLatch(1);

    public DeadSSLServer(SocketAddress socketAddress) throws IOException {
        // create unbound server socket
        var srvSock = new ServerSocket();
        // bind it to the address provided
        srvSock.bind(socketAddress);
        this.serverSock = srvSock;
        start();
    }

    public void run() {
        // Signal client to proceed with the test
        serverStarted.countDown();
        while (true) {
            try (var acceptedSocket = serverSock.accept()) {
                System.err.println("Accepted connection:" + acceptedSocket);
                int iteration = 0;
                // Wait for socket to get opened by DeadSSLSocketFactory and connected to the test server
                while (iteration++ < 20) {
                    if (DeadSSLSocketFactory.firstCreatedSocket.get() != null &&
                        DeadSSLSocketFactory.firstCreatedSocket.get().isConnected()) {
                        break;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException ie) {
                    }
                }
                Socket clientSideSocket = DeadSSLSocketFactory.firstCreatedSocket.get();
                System.err.printf("Got SSLSocketFactory connection after %d iterations: %s%n",
                        iteration, clientSideSocket);

                if (clientSideSocket == null || !clientSideSocket.isConnected()) {
                    // If after 1000 ms client side connection is not opened - probably other local process
                    // tried to connect to the test server socket. Close current connection and retry accept.
                    continue;
                } else {
                    // Check if accepted socket is connected to the LDAP client
                    if (acceptedSocket.getLocalPort() == clientSideSocket.getPort() &&
                            acceptedSocket.getPort() == clientSideSocket.getLocalPort() &&
                            acceptedSocket.getInetAddress().equals(clientSideSocket.getLocalAddress())) {
                        System.err.println("Accepted connection is originated from LDAP client:" + acceptedSocket);
                        try {
                            // Give LDAP client time to fully establish the connection.
                            // When client is done - the accepted socket will be closed
                            testDone.await();
                        } catch (InterruptedException e) {
                        }
                        break;
                    } else {
                        // If accepted socket is not from the LDAP client - the accepted connection will be closed and new
                        // one will be accepted
                        System.err.println("SSLSocketFactory connection has been established, but originated not from" +
                                " the test's LDAP client:" + acceptedSocket);
                    }
                }
            } catch (Exception e) {
                System.err.println("Server socket. Failure to accept connection:" + e.getMessage());
            }
        }
    }

    public int getLocalPort() {
        return serverSock.getLocalPort();
    }

    public void close() throws IOException {
        serverSock.close();
    }
}

public class DeadSSLLdapTimeoutTest {
    // com.sun.jndi.ldap.connect.timeout value to set
    static final String CONNECT_TIMEOUT_MS = "10";

    // com.sun.jndi.ldap.read.timeout value to set
    static final String READ_TIMEOUT_MS = "3000";

    static Hashtable<Object, Object> createEnv() {
        Hashtable<Object, Object> env = new Hashtable<>(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        return env;
    }

    public static void main(String[] args) throws Exception {
        //
        // Running this test serially as it seems to tickle a problem
        // on older kernels
        //
        // run the DeadServerTest with connect / read timeouts set
        // and ssl enabled
        // this should exit with a SocketTimeoutException as the root cause
        // it should also use the connect timeout instead of the read timeout
        System.out.printf("Running connect timeout test with %sms connect timeout," +
                          " %sms read timeout & SSL%n",
                          CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);

        Hashtable<Object, Object> sslenv = createEnv();
        // Setup connect timeout environment property
        sslenv.put("com.sun.jndi.ldap.connect.timeout", CONNECT_TIMEOUT_MS);
        // Setup read timeout environment property
        sslenv.put("com.sun.jndi.ldap.read.timeout", READ_TIMEOUT_MS);
        // Setup DeadSSLSocketFactory to track the client's first LDAP connection
        sslenv.put("java.naming.ldap.factory.socket", "DeadSSLSocketFactory");
        // Use SSL protocol
        sslenv.put(Context.SECURITY_PROTOCOL, "ssl");

        boolean testFailed = !new DeadServerTimeoutSSLTest(sslenv).call();
        if (testFailed) {
            throw new AssertionError("some tests failed");
        }
    }
}

