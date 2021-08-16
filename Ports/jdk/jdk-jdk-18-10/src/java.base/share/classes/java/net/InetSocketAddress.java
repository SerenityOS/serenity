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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.ObjectStreamField;

/**
 *
 * This class implements an IP Socket Address (IP address + port number)
 * It can also be a pair (hostname + port number), in which case an attempt
 * will be made to resolve the hostname. If resolution fails then the address
 * is said to be <I>unresolved</I> but can still be used on some circumstances
 * like connecting through a proxy.
 * <p>
 * It provides an immutable object used by sockets for binding, connecting, or
 * as returned values.
 * <p>
 * The <i>wildcard</i> is a special local IP address. It usually means "any"
 * and can only be used for {@code bind} operations.
 *
 * @see java.net.Socket
 * @see java.net.ServerSocket
 * @since 1.4
 */
public class InetSocketAddress
    extends SocketAddress
{
    // Private implementation class pointed to by all public methods.
    private static class InetSocketAddressHolder {
        // The hostname of the Socket Address
        private String hostname;
        // The IP address of the Socket Address
        private InetAddress addr;
        // The port number of the Socket Address
        private int port;

        private InetSocketAddressHolder(String hostname, InetAddress addr, int port) {
            this.hostname = hostname;
            this.addr = addr;
            this.port = port;
        }

        private int getPort() {
            return port;
        }

        private InetAddress getAddress() {
            return addr;
        }

        private String getHostName() {
            if (hostname != null)
                return hostname;
            if (addr != null)
                return addr.getHostName();
            return null;
        }

        private String getHostString() {
            if (hostname != null)
                return hostname;
            if (addr != null) {
                if (addr.holder().getHostName() != null)
                    return addr.holder().getHostName();
                else
                    return addr.getHostAddress();
            }
            return null;
        }

        private boolean isUnresolved() {
            return addr == null;
        }

        @Override
        public String toString() {

            String formatted;

            if (isUnresolved()) {
                formatted = hostname + "/<unresolved>";
            } else {
                formatted = addr.toString();
                if (addr instanceof Inet6Address) {
                    int i = formatted.lastIndexOf("/");
                    formatted = formatted.substring(0, i + 1)
                            + "[" + formatted.substring(i + 1) + "]";
                }
            }
            return formatted + ":" + port;
        }

        @Override
        public final boolean equals(Object obj) {
            if (!(obj instanceof InetSocketAddressHolder that))
                return false;
            boolean sameIP;
            if (addr != null)
                sameIP = addr.equals(that.addr);
            else if (hostname != null)
                sameIP = (that.addr == null) &&
                    hostname.equalsIgnoreCase(that.hostname);
            else
                sameIP = (that.addr == null) && (that.hostname == null);
            return sameIP && (port == that.port);
        }

        @Override
        public final int hashCode() {
            if (addr != null)
                return addr.hashCode() + port;
            if (hostname != null)
                return hostname.toLowerCase().hashCode() + port;
            return port;
        }
    }

    private final transient InetSocketAddressHolder holder;

    @java.io.Serial
    private static final long serialVersionUID = 5076001401234631237L;

    private static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port out of range:" + port);
        return port;
    }

    private static String checkHost(String hostname) {
        if (hostname == null)
            throw new IllegalArgumentException("hostname can't be null");
        return hostname;
    }

    /**
     * Creates a socket address where the IP address is the wildcard address
     * and the port number a specified value.
     * <p>
     * A valid port value is between 0 and 65535.
     * A port number of {@code zero} will let the system pick up an
     * ephemeral port in a {@code bind} operation.
     *
     * @param   port    The port number
     * @throws IllegalArgumentException if the port parameter is outside the specified
     * range of valid port values.
     */
    public InetSocketAddress(int port) {
        this(InetAddress.anyLocalAddress(), port);
    }

    /**
     *
     * Creates a socket address from an IP address and a port number.
     * <p>
     * A valid port value is between 0 and 65535.
     * A port number of {@code zero} will let the system pick up an
     * ephemeral port in a {@code bind} operation.
     * <P>
     * A {@code null} address will assign the <i>wildcard</i> address.
     *
     * @param   addr    The IP address
     * @param   port    The port number
     * @throws IllegalArgumentException if the port parameter is outside the specified
     * range of valid port values.
     */
    public InetSocketAddress(InetAddress addr, int port) {
        holder = new InetSocketAddressHolder(
                        null,
                        addr == null ? InetAddress.anyLocalAddress() : addr,
                        checkPort(port));
    }

    /**
     *
     * Creates a socket address from a hostname and a port number.
     * <p>
     * An attempt will be made to resolve the hostname into an InetAddress.
     * If that attempt fails, the address will be flagged as <I>unresolved</I>.
     * <p>
     * If there is a security manager, its {@code checkConnect} method
     * is called with the host name as its argument to check the permission
     * to resolve it. This could result in a SecurityException.
     * <P>
     * A valid port value is between 0 and 65535.
     * A port number of {@code zero} will let the system pick up an
     * ephemeral port in a {@code bind} operation.
     *
     * @param   hostname the Host name
     * @param   port    The port number
     * @throws IllegalArgumentException if the port parameter is outside the range
     * of valid port values, or if the hostname parameter is {@code null}.
     * @throws SecurityException if a security manager is present and
     *                           permission to resolve the host name is
     *                           denied.
     * @see     #isUnresolved()
     */
    public InetSocketAddress(String hostname, int port) {
        checkHost(hostname);
        InetAddress addr = null;
        String host = null;
        try {
            addr = InetAddress.getByName(hostname);
        } catch(UnknownHostException e) {
            host = hostname;
        }
        holder = new InetSocketAddressHolder(host, addr, checkPort(port));
    }

    // private constructor for creating unresolved instances
    private InetSocketAddress(int port, String hostname) {
        holder = new InetSocketAddressHolder(hostname, null, port);
    }

    /**
     *
     * Creates an unresolved socket address from a hostname and a port number.
     * <p>
     * No attempt will be made to resolve the hostname into an InetAddress.
     * The address will be flagged as <I>unresolved</I>.
     * <p>
     * A valid port value is between 0 and 65535.
     * A port number of {@code zero} will let the system pick up an
     * ephemeral port in a {@code bind} operation.
     *
     * @param   host    the Host name
     * @param   port    The port number
     * @throws IllegalArgumentException if the port parameter is outside
     *                  the range of valid port values, or if the hostname
     *                  parameter is {@code null}.
     * @see     #isUnresolved()
     * @return  an {@code InetSocketAddress} representing the unresolved
     *          socket address
     * @since 1.5
     */
    public static InetSocketAddress createUnresolved(String host, int port) {
        return new InetSocketAddress(checkPort(port), checkHost(host));
    }

    /**
     * @serialField hostname String the hostname of the Socket Address
     * @serialField addr InetAddress the IP address of the Socket Address
     * @serialField port int the port number of the Socket Address
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
         new ObjectStreamField("hostname", String.class),
         new ObjectStreamField("addr", InetAddress.class),
         new ObjectStreamField("port", int.class)};

    /**
     * Writes the state of this object to the stream.
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out)
        throws IOException
    {
        // Don't call defaultWriteObject()
         ObjectOutputStream.PutField pfields = out.putFields();
         pfields.put("hostname", holder.hostname);
         pfields.put("addr", holder.addr);
         pfields.put("port", holder.port);
         out.writeFields();
     }

    /**
     * Restores the state of this object from the stream.
     *
     * @param  in the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()
        ObjectInputStream.GetField oisFields = in.readFields();
        final String oisHostname = (String)oisFields.get("hostname", null);
        final InetAddress oisAddr = (InetAddress)oisFields.get("addr", null);
        final int oisPort = oisFields.get("port", -1);

        // Check that our invariants are satisfied
        checkPort(oisPort);
        if (oisHostname == null && oisAddr == null)
            throw new InvalidObjectException("hostname and addr " +
                                             "can't both be null");

        InetSocketAddressHolder h = new InetSocketAddressHolder(oisHostname,
                                                                oisAddr,
                                                                oisPort);
        UNSAFE.putReference(this, FIELDS_OFFSET, h);
    }

    /**
     * Throws {@code InvalidObjectException}, always.
     * @throws ObjectStreamException always
     */
    @java.io.Serial
    private void readObjectNoData()
        throws ObjectStreamException
    {
        throw new InvalidObjectException("Stream data required");
    }

    private static final jdk.internal.misc.Unsafe UNSAFE
            = jdk.internal.misc.Unsafe.getUnsafe();
    private static final long FIELDS_OFFSET
            = UNSAFE.objectFieldOffset(InetSocketAddress.class, "holder");

    /**
     * Gets the port number.
     *
     * @return the port number.
     */
    public final int getPort() {
        return holder.getPort();
    }

    /**
     * Gets the {@code InetAddress}.
     *
     * @return the InetAddress or {@code null} if it is unresolved.
     */
    public final InetAddress getAddress() {
        return holder.getAddress();
    }

    /**
     * Gets the {@code hostname}.
     * Note: This method may trigger a name service reverse lookup if the
     * address was created with a literal IP address.
     *
     * @return  the hostname part of the address.
     */
    public final String getHostName() {
        return holder.getHostName();
    }

    /**
     * Returns the hostname, or the String form of the address if it
     * doesn't have a hostname (it was created using a literal).
     * This has the benefit of <b>not</b> attempting a reverse lookup.
     *
     * @return the hostname, or String representation of the address.
     * @since 1.7
     */
    public final String getHostString() {
        return holder.getHostString();
    }

    /**
     * Checks whether the address has been resolved or not.
     *
     * @return {@code true} if the hostname couldn't be resolved into
     *          an {@code InetAddress}.
     */
    public final boolean isUnresolved() {
        return holder.isUnresolved();
    }

    /**
     * Constructs a string representation of this InetSocketAddress.
     * This string is constructed by calling {@link InetAddress#toString()}
     * on the InetAddress and concatenating the port number (with a colon).
     * <p>
     * If the address is an IPv6 address, the IPv6 literal is enclosed in
     * square brackets, for example: {@code "localhost/[0:0:0:0:0:0:0:1]:80"}.
     * If the address is {@linkplain #isUnresolved() unresolved},
     * {@code <unresolved>} is displayed in place of the address literal, for
     * example {@code "foo/<unresolved>:80"}.
     * <p>
     * To retrieve a string representation of the hostname or the address, use
     * {@link #getHostString()}, rather than parsing the string returned by this
     * {@link #toString()} method.
     *
     * @return  a string representation of this object.
     */
    @Override
    public String toString() {
        return holder.toString();
    }

    /**
     * Compares this object against the specified object.
     * The result is {@code true} if and only if the argument is
     * not {@code null} and it represents the same address as
     * this object.
     * <p>
     * Two instances of {@code InetSocketAddress} represent the same
     * address if both the InetAddresses (or hostnames if it is unresolved) and port
     * numbers are equal.
     * If both addresses are unresolved, then the hostname and the port number
     * are compared.
     *
     * Note: Hostnames are case insensitive. e.g. "FooBar" and "foobar" are
     * considered equal.
     *
     * @param   obj   the object to compare against.
     * @return  {@code true} if the objects are the same;
     *          {@code false} otherwise.
     * @see java.net.InetAddress#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof InetSocketAddress addr) {
            return holder.equals(addr.holder);
        }
        return false;
    }

    /**
     * Returns a hashcode for this socket address.
     *
     * @return  a hash code value for this socket address.
     */
    @Override
    public final int hashCode() {
        return holder.hashCode();
    }
}
