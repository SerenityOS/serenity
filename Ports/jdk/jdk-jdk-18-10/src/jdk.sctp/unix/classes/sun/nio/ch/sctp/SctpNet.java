/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.nio.ch.sctp;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AlreadyBoundException;
import java.util.Set;
import java.util.HashSet;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.net.util.IPAddressUtil;
import sun.nio.ch.IOUtil;
import sun.nio.ch.Net;
import com.sun.nio.sctp.SctpSocketOption;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;

public class SctpNet {
    @SuppressWarnings("removal")
    private static final String osName = AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty("os.name"));

    /* -- Miscellaneous SCTP utilities -- */

    private static boolean IPv4MappedAddresses() {
        /* lksctp/linux requires Ipv4 addresses */
        return false;
    }

    static boolean throwAlreadyBoundException() throws IOException {
        throw new AlreadyBoundException();
    }

    static void listen(int fd, int backlog) throws IOException {
        listen0(fd, backlog);
    }

    static int connect(int fd, InetAddress remote, int remotePort)
            throws IOException {
        return connect0(fd, remote, remotePort);
    }

    static void close(int fd) throws IOException {
        close0(fd);
    }

    static void preClose(int fd) throws IOException {
        preClose0(fd);
    }

    /**
     * @param  oneToOne
     *         if {@code true} returns a one-to-one sctp socket, otherwise
     *         returns a one-to-many sctp socket
     */
    static FileDescriptor socket(boolean oneToOne) throws IOException {
        int nativefd = socket0(oneToOne);
        return IOUtil.newFD(nativefd);
    }

    static void bindx(int fd, InetAddress[] addrs, int port, boolean add)
            throws IOException {
        bindx(fd, addrs, port, addrs.length, add,
                IPv4MappedAddresses());
    }

    static Set<SocketAddress> getLocalAddresses(int fd)
            throws IOException {
        Set<SocketAddress> set = null;
        SocketAddress[] saa = getLocalAddresses0(fd);

        if (saa != null) {
            set = getRevealedLocalAddressSet(saa);
        }

        return set;
    }

    private static Set<SocketAddress> getRevealedLocalAddressSet(
            SocketAddress[] saa)
    {
         @SuppressWarnings("removal")
         SecurityManager sm = System.getSecurityManager();
         Set<SocketAddress> set = new HashSet<>(saa.length);
         for (SocketAddress sa : saa) {
             set.add(getRevealedLocalAddress(sa, sm));
         }
         return set;
    }

    private static SocketAddress getRevealedLocalAddress(SocketAddress sa,
                                                         @SuppressWarnings("removal") SecurityManager sm)
    {
        if (sm == null || sa == null)
            return sa;
        InetSocketAddress ia = (InetSocketAddress)sa;
        try{
            sm.checkConnect(ia.getAddress().getHostAddress(), -1);
            // Security check passed
        } catch (SecurityException e) {
            // Return loopback address
            return new InetSocketAddress(InetAddress.getLoopbackAddress(),
                                         ia.getPort());
        }
        return sa;
    }

    static Set<SocketAddress> getRemoteAddresses(int fd, int assocId)
            throws IOException {
        HashSet<SocketAddress> set = null;
        SocketAddress[] saa = getRemoteAddresses0(fd, assocId);

        if (saa != null) {
            set = new HashSet<SocketAddress>(saa.length);
            for (SocketAddress sa : saa)
                set.add(sa);
        }

        return set;
    }

    static <T> void setSocketOption(int fd,
                                    SctpSocketOption<T> name,
                                    T value,
                                    int assocId)
            throws IOException {
        if (value == null)
            throw new IllegalArgumentException("Invalid option value");

        if (name.equals(SCTP_INIT_MAXSTREAMS)) {
            InitMaxStreams maxStreamValue = (InitMaxStreams)value;
            SctpNet.setInitMsgOption0(fd,
                 maxStreamValue.maxInStreams(), maxStreamValue.maxOutStreams());
        } else if (name.equals(SCTP_PRIMARY_ADDR) ||
                   name.equals(SCTP_SET_PEER_PRIMARY_ADDR)) {

            SocketAddress addr  = (SocketAddress) value;
            if (addr == null)
                throw new IllegalArgumentException("Invalid option value");

            Net.checkAddress(addr);
            InetSocketAddress netAddr = (InetSocketAddress)addr;

            if (name.equals(SCTP_PRIMARY_ADDR)) {
                InetAddress inetAddress = netAddr.getAddress();
                if (inetAddress.isLinkLocalAddress()) {
                    inetAddress = IPAddressUtil.toScopedAddress(inetAddress);
                }
                setPrimAddrOption0(fd,
                                   assocId,
                                   inetAddress,
                                   netAddr.getPort());
            } else {
                setPeerPrimAddrOption0(fd,
                                       assocId,
                                       netAddr.getAddress(),
                                       netAddr.getPort(),
                                       IPv4MappedAddresses());
            }
        } else if (name.equals(SCTP_DISABLE_FRAGMENTS) ||
            name.equals(SCTP_EXPLICIT_COMPLETE) ||
            name.equals(SCTP_FRAGMENT_INTERLEAVE) ||
            name.equals(SCTP_NODELAY) ||
            name.equals(SO_SNDBUF) ||
            name.equals(SO_RCVBUF) ||
            name.equals(SO_LINGER)) {
            setIntOption(fd, name, value);
        } else {
            throw new AssertionError("Unknown socket option");
        }
    }

    static Object getSocketOption(int fd, SctpSocketOption<?> name, int assocId)
             throws IOException {
         if (name.equals(SCTP_SET_PEER_PRIMARY_ADDR)) {
            throw new IllegalArgumentException(
                    "SCTP_SET_PEER_PRIMARY_ADDR cannot be retrieved");
        } else if (name.equals(SCTP_INIT_MAXSTREAMS)) {
            /* container for holding maxIn/Out streams */
            int[] values = new int[2];
            SctpNet.getInitMsgOption0(fd, values);
            return InitMaxStreams.create(values[0], values[1]);
        } else if (name.equals(SCTP_PRIMARY_ADDR)) {
            return getPrimAddrOption0(fd, assocId);
        } else if (name.equals(SCTP_DISABLE_FRAGMENTS) ||
            name.equals(SCTP_EXPLICIT_COMPLETE) ||
            name.equals(SCTP_FRAGMENT_INTERLEAVE) ||
            name.equals(SCTP_NODELAY) ||
            name.equals(SO_SNDBUF) ||
            name.equals(SO_RCVBUF) ||
            name.equals(SO_LINGER)) {
            return getIntOption(fd, name);
        } else {
            throw new AssertionError("Unknown socket option");
        }
    }

    static void setIntOption(int fd, SctpSocketOption<?> name, Object value)
            throws IOException {
        if (value == null)
            throw new IllegalArgumentException("Invalid option value");

        Class<?> type = name.type();
        if (type != Integer.class && type != Boolean.class)
            throw new AssertionError("Should not reach here");

        if (name == SO_RCVBUF ||
            name == SO_SNDBUF)
        {
            int i = ((Integer)value).intValue();
            if (i < 0)
                throw new IllegalArgumentException(
                        "Invalid send/receive buffer size");
        } else if (name == SO_LINGER) {
            int i = ((Integer)value).intValue();
            if (i < 0)
                value = Integer.valueOf(-1);
            if (i > 65535)
                value = Integer.valueOf(65535);
        } else if (name.equals(SCTP_FRAGMENT_INTERLEAVE)) {
            int i = ((Integer)value).intValue();
            if (i < 0 || i > 2)
                throw new IllegalArgumentException(
                        "Invalid value for SCTP_FRAGMENT_INTERLEAVE");
        }

        int arg;
        if (type == Integer.class) {
            arg = ((Integer)value).intValue();
        } else {
            boolean b = ((Boolean)value).booleanValue();
            arg = (b) ? 1 : 0;
        }

        setIntOption0(fd, ((SctpStdSocketOption)name).constValue(), arg);
    }

    static Object getIntOption(int fd, SctpSocketOption<?> name)
            throws IOException {
        Class<?> type = name.type();

        if (type != Integer.class && type != Boolean.class)
            throw new AssertionError("Should not reach here");

        if (!(name instanceof SctpStdSocketOption))
            throw new AssertionError("Should not reach here");

        int value = getIntOption0(fd,
                ((SctpStdSocketOption)name).constValue());

        if (type == Integer.class) {
            return Integer.valueOf(value);
        } else {
            return (value == 0) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    static void shutdown(int fd, int assocId)
            throws IOException {
        shutdown0(fd, assocId);
    }

    static FileDescriptor branch(int fd, int assocId) throws IOException {
        int nativefd = branch0(fd, assocId);
        return IOUtil.newFD(nativefd);
    }

    /* Native Methods */
    static native int socket0(boolean oneToOne) throws IOException;

    static native void listen0(int fd, int backlog) throws IOException;

    static native int connect0(int fd, InetAddress remote, int remotePort)
        throws IOException;

    static native void close0(int fd) throws IOException;

    static native void preClose0(int fd) throws IOException;

    static native void bindx(int fd, InetAddress[] addrs, int port, int length,
            boolean add, boolean preferIPv6) throws IOException;

    static native int getIntOption0(int fd, int opt) throws IOException;

    static native void setIntOption0(int fd, int opt, int arg)
        throws IOException;

    static native SocketAddress[] getLocalAddresses0(int fd) throws IOException;

    static native SocketAddress[] getRemoteAddresses0(int fd, int assocId)
            throws IOException;

    static native int branch0(int fd, int assocId) throws IOException;

    static native void setPrimAddrOption0(int fd, int assocId, InetAddress ia,
            int port) throws IOException;

    static native void setPeerPrimAddrOption0(int fd, int assocId,
            InetAddress ia, int port, boolean preferIPv6) throws IOException;

    static native SocketAddress getPrimAddrOption0(int fd, int assocId)
            throws IOException;

    /* retVals [0] maxInStreams, [1] maxOutStreams */
    static native void getInitMsgOption0(int fd, int[] retVals) throws IOException;

    static native void setInitMsgOption0(int fd, int arg1, int arg2)
            throws IOException;

    static native void shutdown0(int fd, int assocId);

    static native void init();

    static {
        loadSctpLibrary();
    }

    @SuppressWarnings("removal")
    private static void loadSctpLibrary() {
        IOUtil.load();   // loads nio & net native libraries
        java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Void>() {
                    public Void run() {
                        System.loadLibrary("sctp");
                        return null;
                    }
                });
        init();
    }
}

