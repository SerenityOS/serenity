/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test socket-channel connection-state transitions
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main ConnectState
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;


public class ConnectState {

    static PrintStream log = System.err;

    static InetSocketAddress remote;

    final static int ST_UNCONNECTED = 0;
    final static int ST_PENDING = 1;
    final static int ST_CONNECTED = 2;
    final static int ST_CLOSED = 3;
    final static int ST_PENDING_OR_CONNECTED = 4;
    // NO exceptions expected
    final static Collection<Class<?>> NONE = Collections.emptySet();

    // make a set of expected exception.
    static Collection<Class<?>> expectedExceptions(Class<?>... expected) {
        final Collection<Class<?>> exceptions;
        if (expected.length == 0) {
            exceptions = NONE;
        } else if (expected.length == 1) {
            assert expected[0] != null;
            exceptions = Collections.<Class<?>>singleton(expected[0]);
        } else {
            exceptions = new HashSet<>(Arrays.asList(expected));
        }
        return exceptions;
    }

    static abstract class Test {

        abstract String go(SocketChannel sc) throws Exception;

        static void check(boolean test, String desc) throws Exception {
            if (!test)
                throw new Exception("Incorrect state: " + desc);
        }

        static void check(SocketChannel sc, int state) throws Exception {
            switch (state) {
            case ST_UNCONNECTED:
                check(!sc.isConnected(), "!isConnected");
                check(!sc.isConnectionPending(), "!isConnectionPending");
                check(sc.isOpen(), "isOpen");
                break;
            case ST_PENDING:
                check(!sc.isConnected(), "!isConnected");
                check(sc.isConnectionPending(), "isConnectionPending");
                check(sc.isOpen(), "isOpen");
                break;
            case ST_CONNECTED:
                check(sc.isConnected(), "isConnected");
                check(!sc.isConnectionPending(), "!isConnectionPending");
                check(sc.isOpen(), "isOpen");
                break;
            case ST_CLOSED:
                check(sc.isConnected(), "isConnected");
                check(!sc.isConnectionPending(), "!isConnectionPending");
                check(sc.isOpen(), "isOpen");
                break;
            case ST_PENDING_OR_CONNECTED:
                check(sc.isConnected() || sc.isConnectionPending(),
                        "isConnected || isConnectionPending");
                check(sc.isOpen(), "isOpen");
                break;
            }
        }

        Test(String name, Class<?> exception, int state) throws Exception {
            this(name, expectedExceptions(exception), state);
        }

        // On some architecture we may need to accept several exceptions.
        // For instance on Solaris, when using a server colocated on the
        // machine we cannot guarantee that we will get a
        // ConnectionPendingException when connecting twice on the same
        // non-blocking socket. We may instead get a an
        // AlreadyConnectedException, which is also valid: it simply means
        // that the first connection has been immediately accepted.
        Test(String name, Collection<Class<?>> exceptions, int state)
                throws Exception {
            SocketChannel sc = SocketChannel.open();
            String note;
            try {
                try {
                    note = go(sc);
                } catch (Exception x) {
                    Class<?> expectedExceptionClass = null;
                    for (Class<?> exception : exceptions) {
                        if (exception.isInstance(x)) {
                            log.println(name + ": As expected: "
                                        + x);
                            expectedExceptionClass = exception;
                            check(sc, state);
                            break;
                        }
                    }
                    if (expectedExceptionClass == null
                            && !exceptions.isEmpty()) {
                        // we had an exception, but it's not of the set of
                        // exceptions we expected.
                        throw new Exception(name
                                                + ": Incorrect exception",
                                                x);
                    } else if (exceptions.isEmpty()) {
                        // we didn't expect any exception
                        throw new Exception(name
                                            + ": Unexpected exception",
                                            x);
                    }
                    // if we reach here, we have our expected exception
                    assert expectedExceptionClass != null;
                    return;
                }
                if (!exceptions.isEmpty()) {
                    throw new Exception(name
                                        + ": Expected exception not thrown: "
                                        + exceptions.iterator().next());
                }
                check(sc, state);
                log.println(name + ": Returned normally"
                            + ((note != null) ? ": " + note : ""));
            } finally {
                if (sc.isOpen())
                    sc.close();
            }
        }

    }

    static void tests() throws Exception {
        log.println(remote);

        new Test("Read unconnected", NotYetConnectedException.class,
                 ST_UNCONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    ByteBuffer b = ByteBuffer.allocateDirect(1024);
                    sc.read(b);
                    return null;
                }};

        new Test("Write unconnected", NotYetConnectedException.class,
                 ST_UNCONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    ByteBuffer b = ByteBuffer.allocateDirect(1024);
                    sc.write(b);
                    return null;
                }};

        new Test("Simple connect", NONE, ST_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.connect(remote);
                    return null;
                }};

        new Test("Simple connect & finish", NONE, ST_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.connect(remote);
                    if (!sc.finishConnect())
                        throw new Exception("finishConnect returned false");
                    return null;
                }};

        new Test("Double connect",
                 AlreadyConnectedException.class, ST_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.connect(remote);
                    sc.connect(remote);
                    return null;
                }};

        new Test("Finish w/o start",
                 NoConnectionPendingException.class, ST_UNCONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.finishConnect();
                    return null;
                }};

        // Note: using our local EchoServer rather than echo on a distant
        //       host - we see that Tries to finish = 0 (instead of ~ 18).
        new Test("NB simple connect", NONE, ST_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.configureBlocking(false);
                    sc.connect(remote);
                    int n = 0;
                    while (!sc.finishConnect()) {
                        Thread.sleep(10);
                        n++;
                    }
                    sc.finishConnect();         // Check redundant invocation
                    return ("Tries to finish = " + n);
                }};

        // Note: using our local EchoServer rather than echo on a distant
        //       host - we cannot guarantee that this test will get a
        //       a ConnectionPendingException: it may get an
        //       AlreadyConnectedException, so we should allow for both.
        new Test("NB double connect",
                 expectedExceptions(ConnectionPendingException.class,
                                    AlreadyConnectedException.class),
                 ST_PENDING_OR_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.configureBlocking(false);
                    sc.connect(remote);
                    sc.connect(remote);
                    return null;
                }};

        new Test("NB finish w/o start",
                 NoConnectionPendingException.class, ST_UNCONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.configureBlocking(false);
                    sc.finishConnect();
                    return null;
                }};

        new Test("NB connect, B finish", NONE, ST_CONNECTED) {
                @Override
                String go(SocketChannel sc) throws Exception {
                    sc.configureBlocking(false);
                    sc.connect(remote);
                    sc.configureBlocking(true);
                    sc.finishConnect();
                    return null;
                }};

    }

    public static void main(String[] args) throws Exception {
        try (TestServers.EchoServer echoServer
                = TestServers.EchoServer.startNewServer(500)) {
            remote = new InetSocketAddress(echoServer.getAddress(),
                                           echoServer.getPort());
            tests();
        }
    }

}
