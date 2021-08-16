/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.net;

import jdk.net.ExtendedSocketOptions.PlatformSocketOptions;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines static methods to set and get socket options defined by the
 * {@link java.net.SocketOption} interface. All of the standard options defined
 * by {@link java.net.Socket}, {@link java.net.ServerSocket}, and
 * {@link java.net.DatagramSocket} can be set this way, as well as additional
 * or platform specific options supported by each socket type.
 * <p>
 * The {@link #supportedOptions(Class)} method can be called to determine
 * the complete set of options available (per socket type) on the
 * current system.
 * <p>
 * When a security manager is installed, some non-standard socket options
 * may require a security permission before being set or get.
 * The details are specified in {@link ExtendedSocketOptions}. No permission
 * is required for {@link java.net.StandardSocketOptions}.
 *
 * @deprecated
 * Java SE 9 added standard methods to set/get socket options, and retrieve the per-Socket
 * supported options effectively rendering this API redundant. Please refer to the corresponding
 * socket's class for the equivalent method to set/get a socket option or retrieve available socket options.
 *
 * @see java.nio.channels.NetworkChannel
 */
@Deprecated(since = "16")
public class Sockets {

    private static final Map<Class<?>,Set<SocketOption<?>>>
            options = optionSets();

    private Sockets() {}

    /**
     * Sets the value of a socket option on a {@link java.net.Socket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option. May be null for some
     *              options.
     * @param <T> The type of the socket option
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs, or socket is closed.
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @throws NullPointerException if name is null
     *
     * @deprecated use {@link java.net.Socket#setOption(SocketOption, Object)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> void setOption(Socket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a {@link java.net.Socket}
     *
     * @param s the socket
     * @param name The socket option
     * @param <T> The type of the socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @throws NullPointerException if name is null
     *
     * @deprecated use {@link java.net.Socket#getOption(SocketOption)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> T getOption(Socket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Sets the value of a socket option on a {@link java.net.ServerSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option
     * @param <T> The type of the socket option
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @deprecated use {@link java.net.ServerSocket#setOption(SocketOption, Object)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> void setOption(ServerSocket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a {@link java.net.ServerSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param <T> The type of the socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @deprecated use {@link java.net.ServerSocket#getOption(SocketOption)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> T getOption(ServerSocket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Sets the value of a socket option on a {@link java.net.DatagramSocket}
     * or {@link java.net.MulticastSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param value The value of the socket option
     * @param <T> The type of the socket option
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IllegalArgumentException if the value is not valid for
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @deprecated use {@link java.net.DatagramSocket#setOption(SocketOption, Object)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> void setOption(DatagramSocket s, SocketOption<T> name, T value) throws IOException
    {
        s.setOption(name, value);
    }

    /**
     * Returns the value of a socket option from a
     * {@link java.net.DatagramSocket} or {@link java.net.MulticastSocket}
     *
     * @param s the socket
     * @param name The socket option
     * @param <T> The type of the socket option
     *
     * @return The value of the socket option.
     *
     * @throws UnsupportedOperationException if the socket does not support
     *         the option.
     *
     * @throws IOException if an I/O error occurs
     *
     * @throws NullPointerException if name is null
     *
     * @throws SecurityException if a security manager is set and the
     *         caller does not have any required permission.
     *
     * @deprecated use {@link java.net.DatagramSocket#getOption(SocketOption)} instead.
     *
     * @see java.net.StandardSocketOptions
     */
    @Deprecated(since = "16")
    public static <T> T getOption(DatagramSocket s, SocketOption<T> name) throws IOException
    {
        return s.getOption(name);
    }

    /**
     * Returns a set of {@link java.net.SocketOption}s supported by the
     * given socket type. This set may include standard options and also
     * non standard extended options.
     *
     * @param socketType the type of java.net socket
     *
     * @return A set of socket options
     *
     * @throws IllegalArgumentException if socketType is not a valid
     *         socket type from the java.net package.
     *
     * @deprecated use {@link Socket#supportedOptions()}, {@link ServerSocket#supportedOptions()},
     *             or {@link DatagramSocket#supportedOptions()} instead.
     */
    @Deprecated(since = "16", forRemoval=true)
    public static Set<SocketOption<?>> supportedOptions(Class<?> socketType) {
        Set<SocketOption<?>> set = options.get(socketType);
        if (set == null) {
            throw new IllegalArgumentException("unknown socket type");
        }
        return set;
    }

    private static void checkValueType(Object value, Class<?> type) {
        if (!type.isAssignableFrom(value.getClass())) {
            String s = "Found: " + value.getClass().toString() + " Expected: "
                        + type.toString();
            throw new IllegalArgumentException(s);
        }
    }

    private static volatile boolean checkedReusePort;
    private static volatile boolean isReusePortAvailable;

    /**
     * Tells whether SO_REUSEPORT is supported.
     */
    static boolean isReusePortAvailable() {
        if (!checkedReusePort) {
            Set<SocketOption<?>> s = new Socket().supportedOptions();
            isReusePortAvailable = s.contains(StandardSocketOptions.SO_REUSEPORT);
            checkedReusePort = true;
        }
        return isReusePortAvailable;
    }

    @SuppressWarnings("removal")
    private static Map<Class<?>,Set<SocketOption<?>>> optionSets() {
        Map<Class<?>,Set<SocketOption<?>>> options = new HashMap<>();
        boolean incomingNapiIdsupported = PlatformSocketOptions.get().incomingNapiIdSupported();

        boolean reuseportsupported = isReusePortAvailable();
        // Socket

        Set<SocketOption<?>> set = new HashSet<>();
        set.add(StandardSocketOptions.SO_KEEPALIVE);
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        if (reuseportsupported) {
            set.add(StandardSocketOptions.SO_REUSEPORT);
        }
        set.add(StandardSocketOptions.SO_LINGER);
        set.add(StandardSocketOptions.IP_TOS);
        set.add(StandardSocketOptions.TCP_NODELAY);
        if (QuickAck.available) {
            set.add(ExtendedSocketOptions.TCP_QUICKACK);
        }
        if (KeepAliveOptions.AVAILABLE) {
            set.addAll(Set.of(ExtendedSocketOptions.TCP_KEEPCOUNT,
                    ExtendedSocketOptions.TCP_KEEPIDLE,
                    ExtendedSocketOptions.TCP_KEEPINTERVAL));
        }
        if (incomingNapiIdsupported) {
            set.add(ExtendedSocketOptions.SO_INCOMING_NAPI_ID);
        }
        set = Collections.unmodifiableSet(set);
        options.put(Socket.class, set);

        // ServerSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        if (reuseportsupported) {
            set.add(StandardSocketOptions.SO_REUSEPORT);
        }
        if (QuickAck.available) {
            set.add(ExtendedSocketOptions.TCP_QUICKACK);
        }
        if (KeepAliveOptions.AVAILABLE) {
            set.addAll(Set.of(ExtendedSocketOptions.TCP_KEEPCOUNT,
                    ExtendedSocketOptions.TCP_KEEPIDLE,
                    ExtendedSocketOptions.TCP_KEEPINTERVAL));
        }
        set.add(StandardSocketOptions.IP_TOS);
        if (incomingNapiIdsupported) {
            set.add(ExtendedSocketOptions.SO_INCOMING_NAPI_ID);
        }
        set = Collections.unmodifiableSet(set);
        options.put(ServerSocket.class, set);

        // DatagramSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        if (reuseportsupported) {
            set.add(StandardSocketOptions.SO_REUSEPORT);
        }
        set.add(StandardSocketOptions.IP_TOS);
        if (incomingNapiIdsupported) {
            set.add(ExtendedSocketOptions.SO_INCOMING_NAPI_ID);
        }
        set = Collections.unmodifiableSet(set);
        options.put(DatagramSocket.class, set);

        // MulticastSocket

        set = new HashSet<>();
        set.add(StandardSocketOptions.SO_SNDBUF);
        set.add(StandardSocketOptions.SO_RCVBUF);
        set.add(StandardSocketOptions.SO_REUSEADDR);
        if (reuseportsupported) {
            set.add(StandardSocketOptions.SO_REUSEPORT);
        }
        set.add(StandardSocketOptions.IP_TOS);
        set.add(StandardSocketOptions.IP_MULTICAST_IF);
        set.add(StandardSocketOptions.IP_MULTICAST_TTL);
        set.add(StandardSocketOptions.IP_MULTICAST_LOOP);
        set = Collections.unmodifiableSet(set);
        options.put(MulticastSocket.class, set);

        return Collections.unmodifiableMap(options);
    }

    /**
     * Tells whether TCP_QUICKACK is supported.
     */
    static class QuickAck {

        static final boolean available;

        static {
            Set<SocketOption<?>> s = new Socket().supportedOptions();
            available = s.contains(ExtendedSocketOptions.TCP_QUICKACK);
        }
    }

    /**
     * Tells whether TCP_KEEPALIVE options are supported.
     */
    static class KeepAliveOptions {

        static final boolean AVAILABLE;

        static {
            Set<SocketOption<?>> s = new Socket().supportedOptions();
            AVAILABLE = s.containsAll(Set.of(ExtendedSocketOptions.TCP_KEEPCOUNT,
                                            ExtendedSocketOptions.TCP_KEEPIDLE,
                                            ExtendedSocketOptions.TCP_KEEPINTERVAL));
        }
    }
}
