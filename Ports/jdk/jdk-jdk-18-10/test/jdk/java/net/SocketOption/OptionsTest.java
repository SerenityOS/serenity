/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8036979 8072384 8044773 8225214 8233296 8234083
 * @library /test/lib
 * @requires !vm.graal.enabled
 * @run main/othervm -Xcheck:jni OptionsTest
 * @run main/othervm -Xcheck:jni -Djava.net.preferIPv4Stack=true OptionsTest
 * @run main/othervm --limit-modules=java.base OptionsTest
 * @run main/othervm/policy=options.policy OptionsTest
 */

import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import jdk.test.lib.net.IPSupport;

public class OptionsTest {

    static class Test<T> {
        final SocketOption<T> option;
        final T value;
        Test(SocketOption<T> option, T value) {
            this.option = option;
            this.value = value;
        }
        static <T> Test<T> create(SocketOption<T> option, T value) {
            return new Test<T>(option, value);
        }

    }

    // The tests set the option using the new API, read back the set value
    // which could be different, and then use the legacy get API to check
    // these values are the same

    static Test<?>[] socketTests = new Test<?>[] {
        Test.create(StandardSocketOptions.SO_KEEPALIVE, Boolean.TRUE),
        Test.create(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(10 * 100)),
        Test.create(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(8 * 100)),
        Test.create(StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_REUSEPORT, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_LINGER, Integer.valueOf(-1)),
        Test.create(StandardSocketOptions.SO_LINGER, Integer.valueOf(0)),
        Test.create(StandardSocketOptions.SO_LINGER, Integer.valueOf(80)),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(0)),  // lower-bound
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(100)),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(255))  //upper-bound
    };

    static Test<?>[] serverSocketTests = new Test<?>[] {
        Test.create(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(8 * 100)),
        Test.create(StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_REUSEPORT, Boolean.FALSE),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(0)),  // lower-bound
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(100)),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(255))  //upper-bound
    };

    static Test<?>[] datagramSocketTests = new Test<?>[] {
        Test.create(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(10 * 100)),
        Test.create(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(8 * 100)),
        Test.create(StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_REUSEPORT, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_BROADCAST, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_BROADCAST, Boolean.TRUE),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(0)),  // lower-bound
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(100)),
        Test.create(StandardSocketOptions.IP_TOS, Integer.valueOf(255))  //upper-bound
    };

    static Test<?>[] multicastSocketTests = new Test<?>[] {
        Test.create(StandardSocketOptions.SO_BROADCAST, Boolean.FALSE),
        Test.create(StandardSocketOptions.SO_BROADCAST, Boolean.TRUE),
        Test.create(StandardSocketOptions.IP_MULTICAST_IF, getNetworkInterface()),
        Test.create(StandardSocketOptions.IP_MULTICAST_TTL, Integer.valueOf(0)),   // lower-bound
        Test.create(StandardSocketOptions.IP_MULTICAST_TTL, Integer.valueOf(10)),
        Test.create(StandardSocketOptions.IP_MULTICAST_TTL, Integer.valueOf(255)), //upper-bound
        Test.create(StandardSocketOptions.IP_MULTICAST_LOOP, Boolean.TRUE)
    };

    static NetworkInterface getNetworkInterface() {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface ni = nifs.nextElement();
                if (ni.supportsMulticast()) {
                    return ni;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    static boolean okayToTest(Socket s, SocketOption<?> option) {
        if (option == StandardSocketOptions.SO_REUSEPORT) {
            // skip SO_REUSEPORT if option is not supported
            return s.supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT);
        }
        if (option == StandardSocketOptions.IP_TOS && s.isConnected()) {
            // skip IP_TOS if connected
            return false;
        }
        return true;
    }

    static <T> void testEqual(SocketOption<T> option, T value1, T value2) {
        if (!value1.equals(value2)) {
            throw new RuntimeException("Test of " + option.name() + " failed: "
                    + value1 + " != " + value2);
        }
    }

    static <T> void test(Socket s, Test<T> test) throws Exception {
        SocketOption<T> option = test.option;
        s.setOption(option, test.value);
        T value1 = s.getOption(test.option);
        T value2 = (T) legacyGetOption(Socket.class, s, test.option);
        testEqual(option, value1, value2);
    }

    static <T> void test(ServerSocket ss, Test<T> test) throws Exception {
        SocketOption<T> option = test.option;
        ss.setOption(option, test.value);
        T value1 = ss.getOption(test.option);
        T value2 = (T) legacyGetOption(ServerSocket.class, ss, test.option);
        testEqual(option, value1, value2);
    }

    static <T> void test(DatagramSocket ds, Test<T> test) throws Exception {
        SocketOption<T> option = test.option;
        ds.setOption(option, test.value);
        T value1 = ds.getOption(test.option);
        T value2 = (T) legacyGetOption(ds.getClass(), ds, test.option);
        testEqual(option, value1, value2);
    }

    // Tests default and negative values of SO_LINGER. All negative values should
    // retrieve as -1.
    static void testSoLingerValues() throws Exception {
        try (Socket s = new Socket()) {
            // retrieve without set
            int defaultValue = s.getOption(StandardSocketOptions.SO_LINGER);
            testEqual(StandardSocketOptions.SO_LINGER, -1, defaultValue);

            for (int v : List.of(-1, -2, -100, -65534, -65535, -65536, -100000)) {
                System.out.println("Testing SO_LINGER with:" + v);
                s.setOption(StandardSocketOptions.SO_LINGER, v);
                int value = s.getOption(StandardSocketOptions.SO_LINGER);
                testEqual(StandardSocketOptions.SO_LINGER, -1, value);
            }
        }
    }

    @SuppressWarnings("try")
    static void doSocketTests() throws Exception {
        // unconnected socket
        try (Socket s = new Socket()) {
            for (Test<?> test : socketTests) {
                if (okayToTest(s, test.option)) {
                    test(s, test);
                }
            }
        }

        // connected socket
        try (ServerSocket ss = new ServerSocket()) {
            var loopback = InetAddress.getLoopbackAddress();
            ss.bind(new InetSocketAddress(loopback, 0));
            try (Socket s1 = new Socket()) {
                s1.connect(ss.getLocalSocketAddress());
                try (Socket s2 = ss.accept()) {
                    for (Test<?> test : socketTests) {
                        if (okayToTest(s1, test.option)) {
                            test(s1, test);
                        }
                    }
                }
            }
        }

        testSoLingerValues();
    }

    static void doServerSocketTests() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            Set<SocketOption<?>> options = ss.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);
            for (Test<?> test : serverSocketTests) {
                if (!(test.option == StandardSocketOptions.SO_REUSEPORT && !reuseport)) {
                    test(ss, test);
                }
            }
        }
    }

    static void doDatagramSocketTests() throws Exception {
        try (DatagramSocket ds = new DatagramSocket(0)) {
            Set<SocketOption<?>> options = ds.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);
            for (Test<?> test : datagramSocketTests) {
                if (!(test.option == StandardSocketOptions.SO_REUSEPORT && !reuseport)) {
                    test(ds, test);
                }
            }
        }
    }

    static void doMulticastSocketTests() throws Exception {
        try (MulticastSocket ms = new MulticastSocket(0)) {
            for (Test<?> test : multicastSocketTests) {
                test(ms, test);
            }
        }
    }

    static Object legacyGetOption(Class<?> type, Object s, Object option) throws Exception {
        if (type.equals(Socket.class)) {
            Socket socket = (Socket)s;
            Set<SocketOption<?>> options = socket.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);

            if (option.equals(StandardSocketOptions.SO_KEEPALIVE)) {
                return Boolean.valueOf(socket.getKeepAlive());
            } else if (option.equals(StandardSocketOptions.SO_SNDBUF)) {
                return Integer.valueOf(socket.getSendBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_RCVBUF)) {
                return Integer.valueOf(socket.getReceiveBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_REUSEADDR)) {
                return Boolean.valueOf(socket.getReuseAddress());
            } else if (option.equals(StandardSocketOptions.SO_REUSEPORT) && reuseport) {
                return Boolean.valueOf(socket.getOption(StandardSocketOptions.SO_REUSEPORT));
            } else if (option.equals(StandardSocketOptions.SO_LINGER)) {
                return Integer.valueOf(socket.getSoLinger());
            } else if (option.equals(StandardSocketOptions.IP_TOS)) {
                return Integer.valueOf(socket.getTrafficClass());
            } else if (option.equals(StandardSocketOptions.TCP_NODELAY)) {
                return Boolean.valueOf(socket.getTcpNoDelay());
            } else {
                throw new RuntimeException("unexpected socket option");
            }
        } else if  (type.equals(ServerSocket.class)) {
            ServerSocket socket = (ServerSocket)s;
            Set<SocketOption<?>> options = socket.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);

            if (option.equals(StandardSocketOptions.SO_RCVBUF)) {
                return Integer.valueOf(socket.getReceiveBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_REUSEADDR)) {
                return Boolean.valueOf(socket.getReuseAddress());
            } else if (option.equals(StandardSocketOptions.SO_REUSEPORT) && reuseport) {
                return Boolean.valueOf(socket.getOption(StandardSocketOptions.SO_REUSEPORT));
            } else if (option.equals(StandardSocketOptions.IP_TOS)) {
                return getServerSocketTrafficClass(socket);
            } else {
                throw new RuntimeException("unexpected socket option");
            }
        } else if  (type.equals(DatagramSocket.class)) {
            DatagramSocket socket = (DatagramSocket)s;
            Set<SocketOption<?>> options = socket.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);

            if (option.equals(StandardSocketOptions.SO_SNDBUF)) {
                return Integer.valueOf(socket.getSendBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_RCVBUF)) {
                return Integer.valueOf(socket.getReceiveBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_REUSEADDR)) {
                return Boolean.valueOf(socket.getReuseAddress());
            } else if (option.equals(StandardSocketOptions.SO_BROADCAST)) {
                return Boolean.valueOf(socket.getBroadcast());
            } else if (option.equals(StandardSocketOptions.SO_REUSEPORT) && reuseport) {
                return Boolean.valueOf(socket.getOption(StandardSocketOptions.SO_REUSEPORT));
            } else if (option.equals(StandardSocketOptions.IP_TOS)) {
                return Integer.valueOf(socket.getTrafficClass());
            } else {
                throw new RuntimeException("unexpected socket option");
            }

        } else if  (type.equals(MulticastSocket.class)) {
            MulticastSocket socket = (MulticastSocket)s;
            Set<SocketOption<?>> options = socket.supportedOptions();
            boolean reuseport = options.contains(StandardSocketOptions.SO_REUSEPORT);

            if (option.equals(StandardSocketOptions.SO_SNDBUF)) {
                return Integer.valueOf(socket.getSendBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_RCVBUF)) {
                return Integer.valueOf(socket.getReceiveBufferSize());
            } else if (option.equals(StandardSocketOptions.SO_REUSEADDR)) {
                return Boolean.valueOf(socket.getReuseAddress());
            } else if (option.equals(StandardSocketOptions.SO_BROADCAST)) {
                return Boolean.valueOf(socket.getBroadcast());
            } else if (option.equals(StandardSocketOptions.SO_REUSEPORT) && reuseport) {
                return Boolean.valueOf(socket.getOption(StandardSocketOptions.SO_REUSEPORT));
            } else if (option.equals(StandardSocketOptions.IP_TOS)) {
                return Integer.valueOf(socket.getTrafficClass());
            } else if (option.equals(StandardSocketOptions.IP_MULTICAST_IF)) {
                return socket.getNetworkInterface();
            } else if (option.equals(StandardSocketOptions.IP_MULTICAST_TTL)) {
                return Integer.valueOf(socket.getTimeToLive());
            } else if (option.equals(StandardSocketOptions.IP_MULTICAST_LOOP)) {
                return !Boolean.valueOf(socket.getLoopbackMode());
            } else {
                throw new RuntimeException("unexpected socket option");
            }
        }
        throw new RuntimeException("unexpected socket type");
    }

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();
        doSocketTests();
        doServerSocketTests();
        doDatagramSocketTests();
        doMulticastSocketTests();
    }

    // Reflectively access jdk.net.Sockets.getOption so that the test can run
    // without the jdk.net module.
    static Object getServerSocketTrafficClass(ServerSocket ss) throws Exception {
        try {
            Class<?> c = Class.forName("jdk.net.Sockets");
            Method m = c.getMethod("getOption", ServerSocket.class, SocketOption.class);
            return m.invoke(null, ss, StandardSocketOptions.IP_TOS);
        } catch (ClassNotFoundException e) {
            // Ok, jdk.net module not present, just fall back
            System.out.println("jdk.net module not present, falling back.");
            return Integer.valueOf(ss.getOption(StandardSocketOptions.IP_TOS));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
