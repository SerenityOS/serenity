/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.stream.Stream;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static java.net.StandardSocketOptions.*;

/*
 * @test
 * @bug 8235141
 * @summary verifies that our implementation supports the set
 *          of SocketOptions that are required by the API documentation.
 * @run testng/othervm RequiredOptions
 */
public class RequiredOptions {

    static final Set<SocketOption<?>> DATAGRAM_OPTIONS =
            Set.of(SO_BROADCAST, SO_SNDBUF, SO_RCVBUF, SO_REUSEADDR, IP_TOS);
    static final Set<SocketOption<?>> MULTICAST_OPTIONS =
            concat(DATAGRAM_OPTIONS, Set.of(IP_MULTICAST_IF, IP_MULTICAST_LOOP, IP_MULTICAST_TTL));
    static final Set<SocketOption<?>> SOCKET_OPTIONS =
            Set.of(SO_KEEPALIVE, SO_LINGER, SO_SNDBUF, SO_RCVBUF, SO_REUSEADDR, TCP_NODELAY);
    static final Set<SocketOption<?>> SERVER_OPTIONS =
            Set.of(SO_RCVBUF, SO_REUSEADDR);

    static Set<SocketOption<?>> concat(Set<SocketOption<?>> ...options) {
        return Set.of(Stream.of(options).flatMap(Set::stream).distinct().toArray(SocketOption[]::new));
    }

    @DataProvider(name = "sockets")
    static Object[][] provider() throws IOException {
        return new Object[][] {
                // UDP
                { Configurable.of(new DatagramSocket(null)), DATAGRAM_OPTIONS },
                { Configurable.of(new MulticastSocket(null)), MULTICAST_OPTIONS },
                // TCP
                { Configurable.of(new Socket()), SOCKET_OPTIONS },
                { Configurable.of(new ServerSocket()), SERVER_OPTIONS },
                // Adaptors
                { Configurable.of(DatagramChannel.open().socket()), MULTICAST_OPTIONS },
                { Configurable.of(SocketChannel.open().socket()), SOCKET_OPTIONS },
                { Configurable.of(ServerSocketChannel.open().socket()), SERVER_OPTIONS },
        };
    }

    @Test(dataProvider = "sockets")
    public <R, E extends Exception>
    void test(Configurable<R,E> socket, Set<SocketOption<?>> options) throws E {
        try (var s = socket) {
            var impl = socket.socket().getClass();
            System.out.println("Testing " + impl + " with " + options);
            Set<SocketOption<?>> supported = socket.supportedOptions();
            if (!supported.containsAll(options)) {
                for (var option : options) {
                    if (!supported.contains(option)) {
                        System.err.println("Option " + option + " not supported by " + impl);
                    }
                }
                throw new AssertionError("Not all documented options are supported by " + impl);
            }
        }
    }

    static interface Configurable<R, E extends Exception> extends AutoCloseable {
        <T> R setOption(SocketOption<T> name, T value) throws E;
        <T> T getOption(SocketOption<T> name) throws E;
        Set<SocketOption<?>> supportedOptions() throws E;
        R socket();
        void close() throws E;

        static Configurable<DatagramSocket, IOException> of(DatagramSocket socket) {
            return new ConfigurableImpl<>(socket, socket::setOption,
                    socket::getOption, socket::supportedOptions, socket::close);
        }
        static Configurable<Socket, IOException> of(Socket socket) {
            return new ConfigurableImpl<>(socket, socket::setOption,
                    socket::getOption, socket::supportedOptions, socket::close);
        }
        static Configurable<ServerSocket, IOException> of(ServerSocket socket) {
            return new ConfigurableImpl<>(socket, socket::setOption,
                    socket::getOption, socket::supportedOptions, socket::close);
        }
    }

    static final class ConfigurableImpl<R, E extends Exception> implements Configurable<R, E> {
        @FunctionalInterface
        interface SetOption<R, E extends Exception> {
            <T> R setOption(SocketOption<T> name, T value) throws E;
        }
        @FunctionalInterface
        interface GetOption<E extends Exception> {
            <T> T getOption(SocketOption<T> name) throws E;
        }
        @FunctionalInterface
        interface SupportedOption<E extends Exception> {
            Set<SocketOption<?>> supportedOptions() throws E;
        }
        @FunctionalInterface
        interface Closer<E extends Exception> {
            void close() throws E;
        }

        private final R socket;
        private final SetOption<R, E> setter;
        private final GetOption<E> getter;
        private final SupportedOption<E> support;
        private final Closer<E> closer;

        public ConfigurableImpl(R socket, SetOption<R, E> setter, GetOption<E> getter,
                                SupportedOption<E> support, Closer<E> closer) {
            this.socket = socket;
            this.setter = setter;
            this.getter = getter;
            this.support = support;
            this.closer = closer;
        }

        @Override
        public <T> R setOption(SocketOption<T> name, T value) throws E {
            return setter.setOption(name, value);
        }
        @Override
        public <T> T getOption(SocketOption<T> name) throws E {
            return getter.getOption(name);
        }
        @Override
        public Set<SocketOption<?>> supportedOptions() throws E {
            return support.supportedOptions();
        }
        @Override
        public R socket() {
            return socket;
        }
        @Override
        public void close() throws E {
            closer.close();
        }
    }


}
