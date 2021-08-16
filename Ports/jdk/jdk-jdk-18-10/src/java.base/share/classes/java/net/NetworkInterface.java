/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class represents a Network Interface made up of a name,
 * and a list of IP addresses assigned to this interface.
 * It is used to identify the local interface on which a multicast group
 * is joined.
 *
 * Interfaces are normally known by names such as "le0".
 *
 * @since 1.4
 */
public final class NetworkInterface {
    private String name;
    private String displayName;
    private int index;
    private InetAddress addrs[];
    private InterfaceAddress bindings[];
    private NetworkInterface childs[];
    private NetworkInterface parent = null;
    private boolean virtual = false;
    private static final NetworkInterface defaultInterface;
    private static final int defaultIndex; /* index of defaultInterface */

    static {
        jdk.internal.loader.BootLoader.loadLibrary("net");

        init();
        defaultInterface = DefaultInterface.getDefault();
        if (defaultInterface != null) {
            defaultIndex = defaultInterface.getIndex();
        } else {
            defaultIndex = 0;
        }
    }

    /**
     * Returns an NetworkInterface object with index set to 0 and name to null.
     * Setting such an interface on a MulticastSocket will cause the
     * kernel to choose one interface for sending multicast packets.
     *
     */
    NetworkInterface() {
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.index = index;
        this.addrs = addrs;
    }

    /**
     * Get the name of this network interface.
     *
     * @return the name of this network interface
     */
    public String getName() {
            return name;
    }

    /**
     * Get an Enumeration with all or a subset of the InetAddresses bound to
     * this network interface.
     * <p>
     * If there is a security manager, its {@code checkConnect}
     * method is called for each InetAddress. Only InetAddresses where
     * the {@code checkConnect} doesn't throw a SecurityException
     * will be returned in the Enumeration. However, if the caller has the
     * {@link NetPermission}("getNetworkInformation") permission, then all
     * InetAddresses are returned.
     *
     * @return an Enumeration object with all or a subset of the InetAddresses
     * bound to this network interface
     * @see #inetAddresses()
     */
    public Enumeration<InetAddress> getInetAddresses() {
        return enumerationFromArray(getCheckedInetAddresses());
    }

    /**
     * Get a Stream of all or a subset of the InetAddresses bound to this
     * network interface.
     * <p>
     * If there is a security manager, its {@code checkConnect}
     * method is called for each InetAddress. Only InetAddresses where
     * the {@code checkConnect} doesn't throw a SecurityException will be
     * returned in the Stream. However, if the caller has the
     * {@link NetPermission}("getNetworkInformation") permission, then all
     * InetAddresses are returned.
     *
     * @return a Stream object with all or a subset of the InetAddresses
     * bound to this network interface
     * @since 9
     */
    public Stream<InetAddress> inetAddresses() {
        return streamFromArray(getCheckedInetAddresses());
    }

    private InetAddress[] getCheckedInetAddresses() {
        InetAddress[] local_addrs = new InetAddress[addrs.length];
        boolean trusted = true;

        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            try {
                sec.checkPermission(new NetPermission("getNetworkInformation"));
            } catch (SecurityException e) {
                trusted = false;
            }
        }
        int i = 0;
        for (int j = 0; j < addrs.length; j++) {
            try {
                if (!trusted) {
                    sec.checkConnect(addrs[j].getHostAddress(), -1);
                }
                local_addrs[i++] = addrs[j];
            } catch (SecurityException e) { }
        }
        return Arrays.copyOf(local_addrs, i);
    }

    /**
     * Get a List of all or a subset of the {@code InterfaceAddresses}
     * of this network interface.
     * <p>
     * If there is a security manager, its {@code checkConnect}
     * method is called with the InetAddress for each InterfaceAddress.
     * Only InterfaceAddresses where the {@code checkConnect} doesn't throw
     * a SecurityException will be returned in the List.
     *
     * @return a {@code List} object with all or a subset of the
     *         InterfaceAddress of this network interface
     * @since 1.6
     */
    public java.util.List<InterfaceAddress> getInterfaceAddresses() {
        java.util.List<InterfaceAddress> lst = new java.util.ArrayList<>(1);
        if (bindings != null) {
            @SuppressWarnings("removal")
            SecurityManager sec = System.getSecurityManager();
            for (int j=0; j<bindings.length; j++) {
                try {
                    if (sec != null) {
                        sec.checkConnect(bindings[j].getAddress().getHostAddress(), -1);
                    }
                    lst.add(bindings[j]);
                } catch (SecurityException e) { }
            }
        }
        return lst;
    }

    /**
     * Get an Enumeration with all the subinterfaces (also known as virtual
     * interfaces) attached to this network interface.
     * <p>
     * For instance eth0:1 will be a subinterface to eth0.
     *
     * @return an Enumeration object with all of the subinterfaces
     * of this network interface
     * @see #subInterfaces()
     * @since 1.6
     */
    public Enumeration<NetworkInterface> getSubInterfaces() {
        return enumerationFromArray(childs);
    }

    /**
     * Get a Stream of all subinterfaces (also known as virtual
     * interfaces) attached to this network interface.
     *
     * @return a Stream object with all of the subinterfaces
     * of this network interface
     * @since 9
     */
    public Stream<NetworkInterface> subInterfaces() {
        return streamFromArray(childs);
    }

    /**
     * Returns the parent NetworkInterface of this interface if this is
     * a subinterface, or {@code null} if it is a physical
     * (non virtual) interface or has no parent.
     *
     * @return The {@code NetworkInterface} this interface is attached to.
     * @since 1.6
     */
    public NetworkInterface getParent() {
        return parent;
    }

    /**
     * Returns the index of this network interface. The index is an integer greater
     * or equal to zero, or {@code -1} for unknown. This is a system specific value
     * and interfaces with the same name can have different indexes on different
     * machines.
     *
     * @return the index of this network interface or {@code -1} if the index is
     *         unknown
     * @see #getByIndex(int)
     * @since 1.7
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the display name of this network interface.
     * A display name is a human readable String describing the network
     * device.
     *
     * @return a non-empty string representing the display name of this network
     *         interface, or null if no display name is available.
     */
    public String getDisplayName() {
        /* strict TCK conformance */
        return "".equals(displayName) ? null : displayName;
    }

    /**
     * Searches for the network interface with the specified name.
     *
     * @param   name
     *          The name of the network interface.
     *
     * @return  A {@code NetworkInterface} with the specified name,
     *          or {@code null} if there is no network interface
     *          with the specified name.
     *
     * @throws  SocketException
     *          If an I/O error occurs.
     *
     * @throws  NullPointerException
     *          If the specified name is {@code null}.
     */
    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null)
            throw new NullPointerException();
        return getByName0(name);
    }

    /**
     * Get a network interface given its index.
     *
     * @param index an integer, the index of the interface
     * @return the NetworkInterface obtained from its index, or {@code null} if
     *         there is no interface with such an index on the system
     * @throws  SocketException  if an I/O error occurs.
     * @throws  IllegalArgumentException if index has a negative value
     * @see #getIndex()
     * @since 1.7
     */
    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0)
            throw new IllegalArgumentException("Interface index can't be negative");
        return getByIndex0(index);
    }

    /**
     * Convenience method to search for a network interface that
     * has the specified Internet Protocol (IP) address bound to
     * it.
     * <p>
     * If the specified IP address is bound to multiple network
     * interfaces it is not defined which network interface is
     * returned.
     *
     * @param   addr
     *          The {@code InetAddress} to search with.
     *
     * @return  A {@code NetworkInterface}
     *          or {@code null} if there is no network interface
     *          with the specified IP address.
     *
     * @throws  SocketException
     *          If an I/O error occurs.
     *
     * @throws  NullPointerException
     *          If the specified address is {@code null}.
     */
    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        }

        if (addr.holder.family == InetAddress.IPv4) {
            if (!(addr instanceof Inet4Address)) {
                throw new IllegalArgumentException("invalid family type: "
                        + addr.holder.family);
            }
        } else if (addr.holder.family == InetAddress.IPv6) {
            if (!(addr instanceof Inet6Address)) {
                throw new IllegalArgumentException("invalid family type: "
                        + addr.holder.family);
            }
        } else {
            throw new IllegalArgumentException("invalid address type: " + addr);
        }
        return getByInetAddress0(addr);
    }

    /**
     * Returns an {@code Enumeration} of all the interfaces on this machine. The
     * {@code Enumeration} contains at least one element, possibly representing
     * a loopback interface that only supports communication between entities on
     * this machine.
     *
     * @apiNote this method can be used in combination with
     * {@link #getInetAddresses()} to obtain all IP addresses for this node
     *
     * @return an Enumeration of NetworkInterfaces found on this machine
     * @throws     SocketException  if an I/O error occurs,
     *             or if the platform does not have at least one configured
     *             network interface.
     * @see #networkInterfaces()
     */
    public static Enumeration<NetworkInterface> getNetworkInterfaces()
        throws SocketException {
        NetworkInterface[] netifs = getAll();
        if (netifs != null && netifs.length > 0) {
            return enumerationFromArray(netifs);
        } else {
            throw new SocketException("No network interfaces configured");
        }
    }

    /**
     * Returns a {@code Stream} of all the interfaces on this machine.  The
     * {@code Stream} contains at least one interface, possibly representing a
     * loopback interface that only supports communication between entities on
     * this machine.
     *
     * @apiNote this method can be used in combination with
     * {@link #inetAddresses()}} to obtain a stream of all IP addresses for
     * this node, for example:
     * <pre> {@code
     * Stream<InetAddress> addrs = NetworkInterface.networkInterfaces()
     *     .flatMap(NetworkInterface::inetAddresses);
     * }</pre>
     *
     * @return a Stream of NetworkInterfaces found on this machine
     * @throws     SocketException  if an I/O error occurs,
     *             or if the platform does not have at least one configured
     *             network interface.
     * @since 9
     */
    public static Stream<NetworkInterface> networkInterfaces()
        throws SocketException {
        NetworkInterface[] netifs = getAll();
        if (netifs != null && netifs.length > 0) {
            return streamFromArray(netifs);
        }  else {
            throw new SocketException("No network interfaces configured");
        }
    }

    /**
     * Checks if the given address is bound to any of the interfaces on this
     * machine.
     *
     * @param   addr
     *          The {@code InetAddress} to search with.
     * @return  true iff the addr parameter is currently bound to one of
     *          the interfaces on this machine.
     *
     * @throws  SocketException
     *          If an I/O error occurs.
     */
    /* package-private */ static boolean isBoundInetAddress(InetAddress addr)
        throws SocketException {
        return boundInetAddress0(addr);
    }

    private static <T> Enumeration<T> enumerationFromArray(T[] a) {
        return new Enumeration<>() {
            int i = 0;

            @Override
            public T nextElement() {
                if (i < a.length) {
                    return a[i++];
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public boolean hasMoreElements() {
                return i < a.length;
            }
        };
    }

    private static <T> Stream<T> streamFromArray(T[] a) {
        return StreamSupport.stream(
                Spliterators.spliterator(
                        a,
                        Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL),
                false);
    }

    private static native NetworkInterface[] getAll()
        throws SocketException;

    private static native NetworkInterface getByName0(String name)
        throws SocketException;

    private static native NetworkInterface getByIndex0(int index)
        throws SocketException;

    private static native boolean boundInetAddress0(InetAddress addr)
            throws SocketException;

    private static native NetworkInterface getByInetAddress0(InetAddress addr)
        throws SocketException;

    /**
     * Returns whether a network interface is up and running.
     *
     * @return  {@code true} if the interface is up and running.
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */

    public boolean isUp() throws SocketException {
        return isUp0(name, index);
    }

    /**
     * Returns whether a network interface is a loopback interface.
     *
     * @return  {@code true} if the interface is a loopback interface.
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */

    public boolean isLoopback() throws SocketException {
        return isLoopback0(name, index);
    }

    /**
     * Returns whether a network interface is a point to point interface.
     * A typical point to point interface would be a PPP connection through
     * a modem.
     *
     * @return  {@code true} if the interface is a point to point
     *          interface.
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */

    public boolean isPointToPoint() throws SocketException {
        return isP2P0(name, index);
    }

    /**
     * Returns whether a network interface supports multicasting or not.
     *
     * @return  {@code true} if the interface supports Multicasting.
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */

    public boolean supportsMulticast() throws SocketException {
        return supportsMulticast0(name, index);
    }

    /**
     * Returns the hardware address (usually MAC) of the interface if it
     * has one and if it can be accessed given the current privileges.
     * If a security manager is set, then the caller must have
     * the permission {@link NetPermission}("getNetworkInformation").
     *
     * @return  a byte array containing the address, or {@code null} if
     *          the address doesn't exist, is not accessible or a security
     *          manager is set and the caller does not have the permission
     *          NetPermission("getNetworkInformation")
     *
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */
    public byte[] getHardwareAddress() throws SocketException {
        @SuppressWarnings("removal")
        SecurityManager sec = System.getSecurityManager();
        if (sec != null) {
            try {
                sec.checkPermission(new NetPermission("getNetworkInformation"));
            } catch (SecurityException e) {
                if (!getInetAddresses().hasMoreElements()) {
                    // don't have connect permission to any local address
                    return null;
                }
            }
        }
        if (isLoopback0(name, index)) {
            return null;
        }
        for (InetAddress addr : addrs) {
            if (addr instanceof Inet4Address) {
                return getMacAddr0(((Inet4Address)addr).getAddress(), name, index);
            }
        }
        return getMacAddr0(null, name, index);
    }

    /**
     * Returns the Maximum Transmission Unit (MTU) of this interface.
     *
     * @return the value of the MTU for that interface.
     * @throws          SocketException if an I/O error occurs.
     * @since 1.6
     */
    public int getMTU() throws SocketException {
        return getMTU0(name, index);
    }

    /**
     * Returns whether this interface is a virtual interface (also called
     * subinterface).
     * Virtual interfaces are, on some systems, interfaces created as a child
     * of a physical interface and given different settings (like address or
     * MTU). Usually the name of the interface will the name of the parent
     * followed by a colon (:) and a number identifying the child since there
     * can be several virtual interfaces attached to a single physical
     * interface.
     *
     * @return {@code true} if this interface is a virtual interface.
     * @since 1.6
     */
    public boolean isVirtual() {
        return virtual;
    }

    private static native boolean isUp0(String name, int ind) throws SocketException;
    private static native boolean isLoopback0(String name, int ind) throws SocketException;
    private static native boolean supportsMulticast0(String name, int ind) throws SocketException;
    private static native boolean isP2P0(String name, int ind) throws SocketException;
    private static native byte[] getMacAddr0(byte[] inAddr, String name, int ind) throws SocketException;
    private static native int getMTU0(String name, int ind) throws SocketException;

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same NetworkInterface
     * as this object.
     * <p>
     * Two instances of {@code NetworkInterface} represent the same
     * NetworkInterface if both the name and the set of {@code InetAddress}es
     * bound to the interfaces are equal.
     *
     * @apiNote two {@code NetworkInterface} objects referring to the same
     * underlying interface may not compare equal if the addresses
     * of the underlying interface are being dynamically updated by
     * the system.
     *
     * @param   obj   the object to compare against.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     * @see     java.net.InetAddress#getAddress()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkInterface that)) {
            return false;
        }
        if (this.name != null ) {
            if (!this.name.equals(that.name)) {
                return false;
            }
        } else {
            if (that.name != null) {
                return false;
            }
        }

        if (this.addrs == null) {
            return that.addrs == null;
        } else if (that.addrs == null) {
            return false;
        }

        /* Both addrs not null. Compare number of addresses */

        if (this.addrs.length != that.addrs.length) {
            return false;
        }

        InetAddress[] thatAddrs = that.addrs;
        int count = thatAddrs.length;

        for (int i=0; i<count; i++) {
            boolean found = false;
            for (int j=0; j<count; j++) {
                if (addrs[i].equals(thatAddrs[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return name == null? 0: name.hashCode();
    }

    public String toString() {
        String result = "name:";
        result += name == null? "null": name;
        if (displayName != null) {
            result += " (" + displayName + ")";
        }
        return result;
    }

    private static native void init();

    /**
     * Returns the default network interface of this system
     *
     * @return the default interface
     */
    static NetworkInterface getDefault() {
        return defaultInterface;
    }
}
