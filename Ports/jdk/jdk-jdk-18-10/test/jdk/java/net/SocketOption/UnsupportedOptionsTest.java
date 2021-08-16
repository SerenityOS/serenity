/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.net.IPSupport;

/*
 * @test
 * @bug 8143554 8044773
 * @library /test/lib
 * @summary Test checks that UnsupportedOperationException for unsupported
 *          SOCKET_OPTIONS is thrown by both getOption() and setOption() methods.
 * @requires !vm.graal.enabled
 * @run main UnsupportedOptionsTest
 * @run main/othervm -Djava.net.preferIPv4Stack=true UnsupportedOptionsTest
 * @run main/othervm --limit-modules=java.base UnsupportedOptionsTest
 */

public class UnsupportedOptionsTest {

    private static final List<SocketOption<?>> socketOptions = new ArrayList<>();

    static {
        socketOptions.add(StandardSocketOptions.IP_MULTICAST_IF);
        socketOptions.add(StandardSocketOptions.IP_MULTICAST_LOOP);
        socketOptions.add(StandardSocketOptions.IP_MULTICAST_TTL);
        socketOptions.add(StandardSocketOptions.IP_TOS);
        socketOptions.add(StandardSocketOptions.SO_BROADCAST);
        socketOptions.add(StandardSocketOptions.SO_KEEPALIVE);
        socketOptions.add(StandardSocketOptions.SO_LINGER);
        socketOptions.add(StandardSocketOptions.SO_RCVBUF);
        socketOptions.add(StandardSocketOptions.SO_REUSEADDR);
        socketOptions.add(StandardSocketOptions.SO_SNDBUF);
        socketOptions.add(StandardSocketOptions.TCP_NODELAY);

        try {
            Class<?> c = Class.forName("jdk.net.ExtendedSocketOptions");
            Field field = c.getField("TCP_QUICKACK");
            socketOptions.add((SocketOption<?>)field.get(null));
            field = c.getField("TCP_KEEPIDLE");
            socketOptions.add((SocketOption<?>)field.get(null));
            field = c.getField("TCP_KEEPINTERVAL");
            socketOptions.add((SocketOption<?>)field.get(null));
            field = c.getField("TCP_KEEPCOUNT");
            socketOptions.add((SocketOption<?>)field.get(null));

        } catch (ClassNotFoundException e) {
            // ignore, jdk.net module not present
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        Socket s = new Socket();
        ServerSocket ss = new ServerSocket();
        DatagramSocket ds = new DatagramSocket();
        MulticastSocket ms = new MulticastSocket();

        for (SocketOption option : socketOptions) {
            if (!s.supportedOptions().contains(option)) {
                testUnsupportedSocketOption(s, option);
            }

            if (!ss.supportedOptions().contains(option)) {
                testUnsupportedSocketOption(ss, option);
            }

            if (!ms.supportedOptions().contains(option)) {
                testUnsupportedSocketOption(ms, option);
            }

            if (!ds.supportedOptions().contains(option)) {
                testUnsupportedSocketOption(ds, option);
            }
        }
    }

    /*
     * Check that UnsupportedOperationException for unsupported option is
     * thrown from both getOption() and setOption() methods.
     */
    private static void testUnsupportedSocketOption(Object socket,
                                                    SocketOption option) {
        testSet(socket, option);
        testGet(socket, option);
    }

    private static void testSet(Object socket, SocketOption option) {
        try {
            setOption(socket, option);
        } catch (UnsupportedOperationException e) {
            System.out.println("UnsupportedOperationException was throw " +
                    "as expected. Socket: " + socket + " Option: " + option);
            return;
        } catch (Exception e) {
            throw new RuntimeException("FAIL. Unexpected exception.", e);
        }
        throw new RuntimeException("FAIL. UnsupportedOperationException " +
                "hasn't been thrown. Socket: " + socket + " Option: " + option);
    }

    private static void testGet(Object socket, SocketOption option) {
        try {
            getOption(socket, option);
        } catch (UnsupportedOperationException e) {
            System.out.println("UnsupportedOperationException was throw " +
                    "as expected. Socket: " + socket + " Option: " + option);
            return;
        } catch (Exception e) {
            throw new RuntimeException("FAIL. Unexpected exception.", e);
        }
        throw new RuntimeException("FAIL. UnsupportedOperationException " +
                "hasn't been thrown. Socket: " + socket + " Option: " + option);
    }

    private static void getOption(Object socket,
                                  SocketOption option) throws IOException {
        if (socket instanceof Socket) {
            ((Socket) socket).getOption(option);
        } else if (socket instanceof ServerSocket) {
            ((ServerSocket) socket).getOption(option);
        } else if (socket instanceof DatagramSocket) {
            ((DatagramSocket) socket).getOption(option);
        } else {
            throw new RuntimeException("Unsupported socket type");
        }
    }

    private static void setOption(Object socket,
                                  SocketOption option) throws IOException {
        if (socket instanceof Socket) {
            ((Socket) socket).setOption(option, null);
        } else if (socket instanceof ServerSocket) {
            ((ServerSocket) socket).setOption(option, null);
        } else if (socket instanceof DatagramSocket) {
            ((DatagramSocket) socket).setOption(option, null);
        } else {
            throw new RuntimeException("Unsupported socket type");
        }
    }
}
