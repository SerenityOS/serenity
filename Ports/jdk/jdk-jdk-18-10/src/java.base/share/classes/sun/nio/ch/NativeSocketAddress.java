/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.channels.UnsupportedAddressTypeException;

import jdk.internal.access.JavaNetInetAddressAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.util.ArraysSupport;

/**
 * A native socket address that is the union of struct sockaddr, struct sockaddr_in,
 * and struct sockaddr_in6.
 *
 * This class is not thread safe.
 */
class NativeSocketAddress {
    private static final JavaNetInetAddressAccess JNINA = SharedSecrets.getJavaNetInetAddressAccess();
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final long ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    private static final int AF_INET  = AFINET();
    private static final int AF_INET6 = AFINET6();

    private static final int SIZEOF_SOCKADDR4     = sizeofSockAddr4();
    private static final int SIZEOF_SOCKADDR6     = sizeofSockAddr6();
    private static final int SIZEOF_SOCKETADDRESS = Math.max(SIZEOF_SOCKADDR4, SIZEOF_SOCKADDR6);
    private static final int SIZEOF_FAMILY        = sizeofFamily();
    private static final int OFFSET_FAMILY        = offsetFamily();
    private static final int OFFSET_SIN4_PORT     = offsetSin4Port();
    private static final int OFFSET_SIN4_ADDR     = offsetSin4Addr();
    private static final int OFFSET_SIN6_PORT     = offsetSin6Port();
    private static final int OFFSET_SIN6_ADDR     = offsetSin6Addr();
    private static final int OFFSET_SIN6_SCOPE_ID = offsetSin6ScopeId();
    private static final int OFFSET_SIN6_FLOWINFO = offsetSin6FlowInfo();

    // SOCKETADDRESS
    private final long address;

    long address() {
        return address;
    }

    NativeSocketAddress() {
        long base = UNSAFE.allocateMemory(SIZEOF_SOCKETADDRESS);
        UNSAFE.setMemory(base, SIZEOF_SOCKETADDRESS, (byte) 0);
        this.address = base;
    }

    /**
     * Allocate an array of native socket addresses.
     */
    static NativeSocketAddress[] allocate(int count) {
        NativeSocketAddress[] array = new NativeSocketAddress[count];
        for (int i = 0; i < count; i++) {
            try {
                array[i] = new NativeSocketAddress();
            } catch (OutOfMemoryError e) {
                freeAll(array);
                throw e;
            }
        }
        return array;
    }

    /**
     * Free all non-null native socket addresses in the given array.
     */
    static void freeAll(NativeSocketAddress[] array) {
        for (int i = 0; i < array.length; i++) {
            NativeSocketAddress sa = array[i];
            if (sa != null) {
                UNSAFE.freeMemory(sa.address);
            }
        }
    }

    /**
     * Encodes the given InetSocketAddress into this socket address.
     * @param protocolFamily protocol family
     * @param isa the InetSocketAddress to encode
     * @return the size of the socket address (sizeof sockaddr or sockaddr6)
     * @throws UnsupportedAddressTypeException if the address type is not supported
     */
    int encode(ProtocolFamily protocolFamily, InetSocketAddress isa) {
        if (protocolFamily == StandardProtocolFamily.INET) {
            // struct sockaddr
            InetAddress ia = isa.getAddress();
            if (!(ia instanceof Inet4Address))
                throw new UnsupportedAddressTypeException();
            putFamily(AF_INET);
            putAddress(AF_INET, ia);
            putPort(AF_INET, isa.getPort());
            return SIZEOF_SOCKADDR4;
        } else {
            // struct sockaddr6
            putFamily(AF_INET6);
            putAddress(AF_INET6, isa.getAddress());
            putPort(AF_INET6, isa.getPort());
            UNSAFE.putInt(address + OFFSET_SIN6_FLOWINFO, 0);
            return SIZEOF_SOCKADDR6;
        }
    }

    /**
     * Return an InetSocketAddress to represent the socket address in this buffer.
     * @throws SocketException if the socket address is not AF_INET or AF_INET6
     */
    InetSocketAddress decode() throws SocketException {
        int family = family();
        if (family != AF_INET && family != AF_INET6)
            throw new SocketException("Socket family not recognized");
        return new InetSocketAddress(address(family), port(family));
    }

    /**
     * Find a mismatch between this and another socket address
     * @return the byte offset of the first mismatch or -1 if no mismatch
     */
    private int mismatch(NativeSocketAddress other) {
        int i = ArraysSupport.vectorizedMismatch(null,
                this.address,
                null,
                other.address,
                SIZEOF_SOCKETADDRESS,
                ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE);
        if (i >= 0)
            return i;
        i = SIZEOF_SOCKETADDRESS - ~i;
        for (; i < SIZEOF_SOCKETADDRESS; i++) {
            if (UNSAFE.getByte(this.address + i) != UNSAFE.getByte(other.address + i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NativeSocketAddress) {
            return mismatch((NativeSocketAddress) other) < 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int offset = 0; offset < SIZEOF_SOCKETADDRESS; offset++) {
            h = 31 * h + UNSAFE.getByte(address + offset);
        }
        return h;
    }

    @Override
    public String toString() {
        int family = family();
        if (family == AF_INET || family == AF_INET6) {
            return ((family == AF_INET) ? "AF_INET" : "AF_INET6")
                    + ", address=" + address(family) + ", port=" + port(family);
        } else {
            return "<unknown>";
        }
    }

    /**
     * Return the value of the sa_family field.
     */
    private int family() {
        if (SIZEOF_FAMILY == 1) {
            return UNSAFE.getByte(address + OFFSET_FAMILY);
        } else if (SIZEOF_FAMILY == 2) {
            return UNSAFE.getShort(address + OFFSET_FAMILY);
        } else {
            throw new InternalError();
        }
    }

    /**
     * Stores the given family in the sa_family field.
     */
    private void putFamily(int family) {
        if (SIZEOF_FAMILY == 1) {
            UNSAFE.putByte(address + OFFSET_FAMILY, (byte) family);
        } else if (SIZEOF_FAMILY == 2) {
            UNSAFE.putShort(address + OFFSET_FAMILY, (short) family);
        } else {
            throw new InternalError();
        }
    }

    /**
     * Return the value of the sin_port or sin6_port field. These fields are
     * stored in network order.
     */
    private int port(int family) {
        byte b1, b2;
        if (family == AF_INET) {
            b1 = UNSAFE.getByte(address + OFFSET_SIN4_PORT);
            b2 = UNSAFE.getByte(address + OFFSET_SIN4_PORT + 1);
        } else {
            b1 = UNSAFE.getByte(address + OFFSET_SIN6_PORT);
            b2 = UNSAFE.getByte(address + OFFSET_SIN6_PORT + 1);
        }
        return (Byte.toUnsignedInt(b1) << 8) + Byte.toUnsignedInt(b2);
    }

    /**
     * Stores the given port number in the sin_port or sin6_port field. The
     * port is stored in network order.
     */
    private void putPort(int family, int port) {
        byte b1 = (byte) ((port >> 8) & 0xff);
        byte b2 = (byte) ((port >> 0) & 0xff);
        if (family == AF_INET) {
            UNSAFE.putByte(address + OFFSET_SIN4_PORT, b1);
            UNSAFE.putByte(address + OFFSET_SIN4_PORT + 1, b2);
        } else {
            UNSAFE.putByte(address + OFFSET_SIN6_PORT, b1);
            UNSAFE.putByte(address + OFFSET_SIN6_PORT + 1, b2);
        }
    }

    /**
     * Return an InetAddress to represent the value of the address in the
     * sin4_addr or sin6_addr fields. For IPv6 addresses, the Inet6Address is
     * created with the sin6_scope_id in the sockaddr_in6 structure.
     */
    private InetAddress address(int family) {
        int len;
        int offset;
        int scope_id;
        if (family == AF_INET) {
            len = 4;
            offset = OFFSET_SIN4_ADDR;
            scope_id = 0;
        } else {
            len = 16;
            offset = OFFSET_SIN6_ADDR;
            scope_id = UNSAFE.getInt(address + OFFSET_SIN6_SCOPE_ID);
        }
        byte[] bytes = new byte[len];
        UNSAFE.copyMemory(null, address + offset, bytes, ARRAY_BASE_OFFSET, len);
        try {
            if (scope_id == 0) {
                return InetAddress.getByAddress(bytes);
            } else {
                return Inet6Address.getByAddress(null, bytes, scope_id);
            }
        } catch (UnknownHostException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Stores the given InetAddress in the sin_addr or sin6_addr/sin6_scope_id
     * fields. For IPv6 addresses, the sin6_addr will be popluated with an
     * IPv4-mapped IPv6 address when the given InetAddress is an IPv4 address.
     */
    private void putAddress(int family, InetAddress ia) {
        if (family == AF_INET) {
            // IPv4 address
            putAddress(address + OFFSET_SIN4_ADDR, (Inet4Address) ia);
        } else {
            int scope_id;
            if (ia instanceof Inet4Address) {
                // IPv4-mapped IPv6 address
                UNSAFE.setMemory(address + OFFSET_SIN6_ADDR, 10, (byte) 0);
                UNSAFE.putByte(address + OFFSET_SIN6_ADDR + 10, (byte) 0xff);
                UNSAFE.putByte(address + OFFSET_SIN6_ADDR + 11, (byte) 0xff);
                putAddress(address + OFFSET_SIN6_ADDR + 12, (Inet4Address) ia);
                scope_id = 0;
            } else {
                // IPv6 address
                var inet6Address = (Inet6Address) ia;
                putAddress(address + OFFSET_SIN6_ADDR, inet6Address);
                scope_id = inet6Address.getScopeId();
            }
            UNSAFE.putInt(address + OFFSET_SIN6_SCOPE_ID, scope_id);
        }
    }

    private static void putAddress(long address, Inet4Address ia) {
        int ipAddress = JNINA.addressValue(ia);
        // network order
        UNSAFE.putByte(address + 0, (byte) ((ipAddress >>> 24) & 0xFF));
        UNSAFE.putByte(address + 1, (byte) ((ipAddress >>> 16) & 0xFF));
        UNSAFE.putByte(address + 2, (byte) ((ipAddress >>> 8) & 0xFF));
        UNSAFE.putByte(address + 3, (byte) (ipAddress & 0xFF));
    }

    private static void putAddress(long address, Inet6Address ia) {
        byte[] bytes = JNINA.addressBytes(ia);
        UNSAFE.copyMemory(bytes, ARRAY_BASE_OFFSET, null, address, 16);
    }

    private static native int AFINET();
    private static native int AFINET6();
    private static native int sizeofSockAddr4();
    private static native int sizeofSockAddr6();
    private static native int sizeofFamily();
    private static native int offsetFamily();
    private static native int offsetSin4Port();
    private static native int offsetSin4Addr();
    private static native int offsetSin6Port();
    private static native int offsetSin6Addr();
    private static native int offsetSin6ScopeId();
    private static native int offsetSin6FlowInfo();

    static {
        IOUtil.load();
    }
}
