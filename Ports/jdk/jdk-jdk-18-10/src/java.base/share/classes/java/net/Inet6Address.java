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
import java.io.ObjectStreamField;
import java.util.Enumeration;
import java.util.Arrays;

/**
 * This class represents an Internet Protocol version 6 (IPv6) address.
 * Defined by <a href="http://www.ietf.org/rfc/rfc2373.txt">
 * <i>RFC&nbsp;2373: IP Version 6 Addressing Architecture</i></a>.
 *
 * <h2> <a id="format">Textual representation of IP addresses</a> </h2>
 *
 * Textual representation of IPv6 address used as input to methods
 * takes one of the following forms:
 *
 * <ol>
 *   <li><p> <a id="lform">The preferred form</a> is x:x:x:x:x:x:x:x,
 *   where the 'x's are
 *   the hexadecimal values of the eight 16-bit pieces of the
 *   address. This is the full form.  For example,
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code 1080:0:0:0:8:800:200C:417A}</li>
 *   </ul></blockquote>
 *
 *   <p> Note that it is not necessary to write the leading zeros in
 *   an individual field. However, there must be at least one numeral
 *   in every field, except as described below.</li>
 *
 *   <li><p> Due to some methods of allocating certain styles of IPv6
 *   addresses, it will be common for addresses to contain long
 *   strings of zero bits. In order to make writing addresses
 *   containing zero bits easier, a special syntax is available to
 *   compress the zeros. The use of "::" indicates multiple groups
 *   of 16-bits of zeros. The "::" can only appear once in an address.
 *   The "::" can also be used to compress the leading and/or trailing
 *   zeros in an address. For example,
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code 1080::8:800:200C:417A}</li>
 *   </ul></blockquote>
 *
 *   <li><p> An alternative form that is sometimes more convenient
 *   when dealing with a mixed environment of IPv4 and IPv6 nodes is
 *   x:x:x:x:x:x:d.d.d.d, where the 'x's are the hexadecimal values
 *   of the six high-order 16-bit pieces of the address, and the 'd's
 *   are the decimal values of the four low-order 8-bit pieces of the
 *   standard IPv4 representation address, for example,
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code ::FFFF:129.144.52.38}</li>
 *   <li>{@code ::129.144.52.38}</li>
 *   </ul></blockquote>
 *
 *   <p> where "::FFFF:d.d.d.d" and "::d.d.d.d" are, respectively, the
 *   general forms of an IPv4-mapped IPv6 address and an
 *   IPv4-compatible IPv6 address. Note that the IPv4 portion must be
 *   in the "d.d.d.d" form. The following forms are invalid:
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code ::FFFF:d.d.d}</li>
 *   <li>{@code ::FFFF:d.d}</li>
 *   <li>{@code ::d.d.d}</li>
 *   <li>{@code ::d.d}</li>
 *   </ul></blockquote>
 *
 *   <p> The following form:
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code ::FFFF:d}</li>
 *   </ul></blockquote>
 *
 *   <p> is valid, however it is an unconventional representation of
 *   the IPv4-compatible IPv6 address,
 *
 *   <blockquote><ul style="list-style-type:none">
 *   <li>{@code ::255.255.0.d}</li>
 *   </ul></blockquote>
 *
 *   <p> while "::d" corresponds to the general IPv6 address
 *   "0:0:0:0:0:0:0:d".</li>
 * </ol>
 *
 * <p> For methods that return a textual representation as output
 * value, the full form is used. Inet6Address will return the full
 * form because it is unambiguous when used in combination with other
 * textual data.
 *
 * <h3> Special IPv6 address </h3>
 *
 * <blockquote>
 * <table class="borderless">
 * <caption style="display:none">Description of IPv4-mapped address</caption>
 * <tr><th style="vertical-align:top; padding-right:2px"><i>IPv4-mapped address</i></th>
 *         <td>Of the form ::ffff:w.x.y.z, this IPv6 address is used to
 *         represent an IPv4 address. It allows the native program to
 *         use the same address data structure and also the same
 *         socket when communicating with both IPv4 and IPv6 nodes.
 *
 *         <p>In InetAddress and Inet6Address, it is used for internal
 *         representation; it has no functional role. Java will never
 *         return an IPv4-mapped address.  These classes can take an
 *         IPv4-mapped address as input, both in byte array and text
 *         representation. However, it will be converted into an IPv4
 *         address.</td></tr>
 * </table></blockquote>
 *
 * <h3><a id="scoped">Textual representation of IPv6 scoped addresses</a></h3>
 *
 * <p> The textual representation of IPv6 addresses as described above can be
 * extended to specify IPv6 scoped addresses. This extension to the basic
 * addressing architecture is described in [draft-ietf-ipngwg-scoping-arch-04.txt].
 *
 * <p> Because link-local and site-local addresses are non-global, it is possible
 * that different hosts may have the same destination address and may be
 * reachable through different interfaces on the same originating system. In
 * this case, the originating system is said to be connected to multiple zones
 * of the same scope. In order to disambiguate which is the intended destination
 * zone, it is possible to append a zone identifier (or <i>scope_id</i>) to an
 * IPv6 address.
 *
 * <p> The general format for specifying the <i>scope_id</i> is the following:
 *
 * <blockquote><i>IPv6-address</i>%<i>scope_id</i></blockquote>
 * <p> The IPv6-address is a literal IPv6 address as described above.
 * The <i>scope_id</i> refers to an interface on the local system, and it can be
 * specified in two ways.
 * <ol><li><i>As a numeric identifier.</i> This must be a positive integer
 * that identifies the particular interface and scope as understood by the
 * system. Usually, the numeric values can be determined through administration
 * tools on the system. Each interface may have multiple values, one for each
 * scope. If the scope is unspecified, then the default value used is zero.</li>
 * <li><i>As a string.</i> This must be the exact string that is returned by
 * {@link java.net.NetworkInterface#getName()} for the particular interface in
 * question. When an Inet6Address is created in this way, the numeric scope-id
 * is determined at the time the object is created by querying the relevant
 * NetworkInterface.</li></ol>
 *
 * <p> Note also, that the numeric <i>scope_id</i> can be retrieved from
 * Inet6Address instances returned from the NetworkInterface class. This can be
 * used to find out the current scope ids configured on the system.
 * @since 1.4
 */

public final
class Inet6Address extends InetAddress {
    static final int INADDRSZ = 16;

    private static class Inet6AddressHolder {

        private Inet6AddressHolder() {
            ipaddress = new byte[INADDRSZ];
        }

        private Inet6AddressHolder(
            byte[] ipaddress, int scope_id, boolean scope_id_set,
            NetworkInterface ifname, boolean scope_ifname_set)
        {
            this.ipaddress = ipaddress;
            this.scope_id = scope_id;
            this.scope_id_set = scope_id_set;
            this.scope_ifname_set = scope_ifname_set;
            this.scope_ifname = ifname;
        }

        /**
         * Holds a 128-bit (16 bytes) IPv6 address.
         */
        byte[] ipaddress;

        /**
         * scope_id. The scope specified when the object is created. If the object
         * is created with an interface name, then the scope_id is not determined
         * until the time it is needed.
         */
        int scope_id;  // 0

        /**
         * This will be set to true when the scope_id field contains a valid
         * integer scope_id.
         */
        boolean scope_id_set;  // false

        /**
         * scoped interface. scope_id is derived from this as the scope_id of the first
         * address whose scope is the same as this address for the named interface.
         */
        NetworkInterface scope_ifname;  // null

        /**
         * set if the object is constructed with a scoped
         * interface instead of a numeric scope id.
         */
        boolean scope_ifname_set; // false;

        void setAddr(byte addr[]) {
            if (addr.length == INADDRSZ) { // normal IPv6 address
                System.arraycopy(addr, 0, ipaddress, 0, INADDRSZ);
            }
        }

        void init(byte addr[], int scope_id) {
            setAddr(addr);

            if (scope_id >= 0) {
                this.scope_id = scope_id;
                this.scope_id_set = true;
            }
        }

        void init(byte addr[], NetworkInterface nif)
            throws UnknownHostException
        {
            setAddr(addr);

            if (nif != null) {
                this.scope_id = deriveNumericScope(ipaddress, nif);
                this.scope_id_set = true;
                this.scope_ifname = nif;
                this.scope_ifname_set = true;
            }
        }

        String getHostAddress() {
            String s = numericToTextFormat(ipaddress);
            if (scope_ifname != null) { /* must check this first */
                s = s + "%" + scope_ifname.getName();
            } else if (scope_id_set) {
                s = s + "%" + scope_id;
            }
            return s;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Inet6AddressHolder that)) {
                return false;
            }

            return Arrays.equals(this.ipaddress, that.ipaddress);
        }

        public int hashCode() {
            if (ipaddress != null) {

                int hash = 0;
                int i=0;
                while (i<INADDRSZ) {
                    int j=0;
                    int component=0;
                    while (j<4 && i<INADDRSZ) {
                        component = (component << 8) + ipaddress[i];
                        j++;
                        i++;
                    }
                    hash += component;
                }
                return hash;

            } else {
                return 0;
            }
        }

        boolean isIPv4CompatibleAddress() {
            if ((ipaddress[0] == 0x00) && (ipaddress[1] == 0x00) &&
                (ipaddress[2] == 0x00) && (ipaddress[3] == 0x00) &&
                (ipaddress[4] == 0x00) && (ipaddress[5] == 0x00) &&
                (ipaddress[6] == 0x00) && (ipaddress[7] == 0x00) &&
                (ipaddress[8] == 0x00) && (ipaddress[9] == 0x00) &&
                (ipaddress[10] == 0x00) && (ipaddress[11] == 0x00))  {
                return true;
            }
            return false;
        }

        boolean isMulticastAddress() {
            return ((ipaddress[0] & 0xff) == 0xff);
        }

        boolean isAnyLocalAddress() {
            byte test = 0x00;
            for (int i = 0; i < INADDRSZ; i++) {
                test |= ipaddress[i];
            }
            return (test == 0x00);
        }

        boolean isLoopbackAddress() {
            byte test = 0x00;
            for (int i = 0; i < 15; i++) {
                test |= ipaddress[i];
            }
            return (test == 0x00) && (ipaddress[15] == 0x01);
        }

        boolean isLinkLocalAddress() {
            return ((ipaddress[0] & 0xff) == 0xfe
                    && (ipaddress[1] & 0xc0) == 0x80);
        }


        boolean isSiteLocalAddress() {
            return ((ipaddress[0] & 0xff) == 0xfe
                    && (ipaddress[1] & 0xc0) == 0xc0);
        }

        boolean isMCGlobal() {
            return ((ipaddress[0] & 0xff) == 0xff
                    && (ipaddress[1] & 0x0f) == 0x0e);
        }

        boolean isMCNodeLocal() {
            return ((ipaddress[0] & 0xff) == 0xff
                    && (ipaddress[1] & 0x0f) == 0x01);
        }

        boolean isMCLinkLocal() {
            return ((ipaddress[0] & 0xff) == 0xff
                    && (ipaddress[1] & 0x0f) == 0x02);
        }

        boolean isMCSiteLocal() {
            return ((ipaddress[0] & 0xff) == 0xff
                    && (ipaddress[1] & 0x0f) == 0x05);
        }

        boolean isMCOrgLocal() {
            return ((ipaddress[0] & 0xff) == 0xff
                    && (ipaddress[1] & 0x0f) == 0x08);
        }
    }

    private final transient Inet6AddressHolder holder6;

    @java.io.Serial
    private static final long serialVersionUID = 6880410070516793377L;

    // Perform native initialization
    static { init(); }

    Inet6Address() {
        super();
        holder.init(null, IPv6);
        holder6 = new Inet6AddressHolder();
    }

    /* checking of value for scope_id should be done by caller
     * scope_id must be >= 0, or -1 to indicate not being set
     */
    Inet6Address(String hostName, byte addr[], int scope_id) {
        holder.init(hostName, IPv6);
        holder6 = new Inet6AddressHolder();
        holder6.init(addr, scope_id);
    }

    Inet6Address(String hostName, byte addr[]) {
        holder6 = new Inet6AddressHolder();
        try {
            initif (hostName, addr, null);
        } catch (UnknownHostException e) {} /* cant happen if ifname is null */
    }

    Inet6Address (String hostName, byte addr[], NetworkInterface nif)
        throws UnknownHostException
    {
        holder6 = new Inet6AddressHolder();
        initif (hostName, addr, nif);
    }

    Inet6Address (String hostName, byte addr[], String ifname)
        throws UnknownHostException
    {
        holder6 = new Inet6AddressHolder();
        initstr (hostName, addr, ifname);
    }

    /**
     * Create an Inet6Address in the exact manner of {@link
     * InetAddress#getByAddress(String,byte[])} except that the IPv6 scope_id is
     * set to the value corresponding to the given interface for the address
     * type specified in {@code addr}. The call will fail with an
     * UnknownHostException if the given interface does not have a numeric
     * scope_id assigned for the given address type (e.g. link-local or site-local).
     * See <a href="Inet6Address.html#scoped">here</a> for a description of IPv6
     * scoped addresses.
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     * @param nif an interface this address must be associated with.
     * @return  an Inet6Address object created from the raw IP address.
     * @throws  UnknownHostException
     *          if IP address is of illegal length, or if the interface does not
     *          have a numeric scope_id assigned for the given address type.
     *
     * @since 1.5
     */
    public static Inet6Address getByAddress(String host, byte[] addr,
                                            NetworkInterface nif)
        throws UnknownHostException
    {
        if (host != null && !host.isEmpty() && host.charAt(0) == '[') {
            if (host.charAt(host.length()-1) == ']') {
                host = host.substring(1, host.length() -1);
            }
        }
        if (addr != null) {
            if (addr.length == Inet6Address.INADDRSZ) {
                return new Inet6Address(host, addr, nif);
            }
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    /**
     * Create an Inet6Address in the exact manner of {@link
     * InetAddress#getByAddress(String,byte[])} except that the IPv6 scope_id is
     * set to the given numeric value. The scope_id is not checked to determine
     * if it corresponds to any interface on the system.
     * See <a href="Inet6Address.html#scoped">here</a> for a description of IPv6
     * scoped addresses.
     *
     * @param host the specified host
     * @param addr the raw IP address in network byte order
     * @param scope_id the numeric scope_id for the address.
     * @return  an Inet6Address object created from the raw IP address.
     * @throws  UnknownHostException  if IP address is of illegal length.
     *
     * @since 1.5
     */
    public static Inet6Address getByAddress(String host, byte[] addr,
                                            int scope_id)
        throws UnknownHostException
    {
        if (host != null && !host.isEmpty() && host.charAt(0) == '[') {
            if (host.charAt(host.length()-1) == ']') {
                host = host.substring(1, host.length() -1);
            }
        }
        if (addr != null) {
            if (addr.length == Inet6Address.INADDRSZ) {
                return new Inet6Address(host, addr, scope_id);
            }
        }
        throw new UnknownHostException("addr is of illegal length");
    }

    private void initstr(String hostName, byte addr[], String ifname)
        throws UnknownHostException
    {
        try {
            NetworkInterface nif = NetworkInterface.getByName (ifname);
            if (nif == null) {
                throw new UnknownHostException ("no such interface " + ifname);
            }
            initif (hostName, addr, nif);
        } catch (SocketException e) {
            throw new UnknownHostException ("SocketException thrown" + ifname);
        }
    }

    private void initif(String hostName, byte addr[], NetworkInterface nif)
        throws UnknownHostException
    {
        int family = -1;
        holder6.init(addr, nif);

        if (addr.length == INADDRSZ) { // normal IPv6 address
            family = IPv6;
        }
        holder.init(hostName, family);
    }

    /* check the two Ipv6 addresses and return false if they are both
     * non global address types, but not the same.
     * (i.e. one is site-local and the other link-local)
     * return true otherwise.
     */

    private static boolean isDifferentLocalAddressType(
        byte[] thisAddr, byte[] otherAddr) {

        if (Inet6Address.isLinkLocalAddress(thisAddr) &&
                !Inet6Address.isLinkLocalAddress(otherAddr)) {
            return false;
        }
        if (Inet6Address.isSiteLocalAddress(thisAddr) &&
                !Inet6Address.isSiteLocalAddress(otherAddr)) {
            return false;
        }
        return true;
    }

    private static int deriveNumericScope (byte[] thisAddr, NetworkInterface ifc) throws UnknownHostException {
        Enumeration<InetAddress> addresses = ifc.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            if (!(addr instanceof Inet6Address ia6_addr)) {
                continue;
            }
            /* check if site or link local prefixes match */
            if (!isDifferentLocalAddressType(thisAddr, ia6_addr.getAddress())){
                /* type not the same, so carry on searching */
                continue;
            }
            /* found a matching address - return its scope_id */
            return ia6_addr.getScopeId();
        }
        throw new UnknownHostException ("no scope_id found");
    }

    private int deriveNumericScope (String ifname) throws UnknownHostException {
        Enumeration<NetworkInterface> en;
        try {
            en = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new UnknownHostException ("could not enumerate local network interfaces");
        }
        while (en.hasMoreElements()) {
            NetworkInterface ifc = en.nextElement();
            if (ifc.getName().equals (ifname)) {
                return deriveNumericScope(holder6.ipaddress, ifc);
            }
        }
        throw new UnknownHostException ("No matching address found for interface : " +ifname);
    }

    /**
     * @serialField ipaddress byte[] holds a 128-bit (16 bytes) IPv6 address
     * @serialField scope_id int the address scope id. {@code 0} if undefined
     * @serialField scope_id_set boolean {@code true} when the scope_id field
     *              contains  a valid integer scope_id
     * @serialField scope_ifname_set boolean {@code true} if the object is
     *              constructed with a scoped interface instead of a numeric
     *              scope id
     * @serialField ifname String the name of the scoped network interface.
     *              {@code null} if undefined
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
         new ObjectStreamField("ipaddress", byte[].class),
         new ObjectStreamField("scope_id", int.class),
         new ObjectStreamField("scope_id_set", boolean.class),
         new ObjectStreamField("scope_ifname_set", boolean.class),
         new ObjectStreamField("ifname", String.class)
    };

    private static final jdk.internal.misc.Unsafe UNSAFE
            = jdk.internal.misc.Unsafe.getUnsafe();
    private static final long FIELDS_OFFSET = UNSAFE.objectFieldOffset(
                Inet6Address.class, "holder6");

    /**
     * Restores the state of this object from the stream.
     * This includes the scope information, but only if the
     * scoped interface name is valid on this system.
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        NetworkInterface scope_ifname = null;

        if (getClass().getClassLoader() != null) {
            throw new SecurityException ("invalid address type");
        }

        ObjectInputStream.GetField gf = s.readFields();
        byte[] ipaddress = (byte[])gf.get("ipaddress", new byte[0]);
        int scope_id = gf.get("scope_id", -1);
        boolean scope_id_set = gf.get("scope_id_set", false);
        boolean scope_ifname_set = gf.get("scope_ifname_set", false);
        String ifname = (String)gf.get("ifname", null);

        if (ifname != null && !ifname.isEmpty()) {
            try {
                scope_ifname = NetworkInterface.getByName(ifname);
                if (scope_ifname == null) {
                    /* the interface does not exist on this system, so we clear
                     * the scope information completely */
                    scope_id_set = false;
                    scope_ifname_set = false;
                    scope_id = 0;
                } else {
                    scope_ifname_set = true;
                    try {
                        scope_id = deriveNumericScope (ipaddress, scope_ifname);
                    } catch (UnknownHostException e) {
                        // typically should not happen, but it may be that
                        // the machine being used for deserialization has
                        // the same interface name but without IPv6 configured.
                    }
                }
            } catch (SocketException e) {}
        }

        /* if ifname was not supplied, then the numeric info is used */

        ipaddress = ipaddress.clone();

        // Check that our invariants are satisfied
        if (ipaddress.length != INADDRSZ) {
            throw new InvalidObjectException("invalid address length: "+
                                             ipaddress.length);
        }

        if (holder.getFamily() != IPv6) {
            throw new InvalidObjectException("invalid address family type");
        }

        Inet6AddressHolder h = new Inet6AddressHolder(
            ipaddress, scope_id, scope_id_set, scope_ifname, scope_ifname_set
        );

        UNSAFE.putReference(this, FIELDS_OFFSET, h);
    }

    /**
     * The default behavior of this method is overridden in order to
     * write the scope_ifname field as a {@code String}, rather than a
     * {@code NetworkInterface} which is not serializable.
     *
     * @param  s the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private synchronized void writeObject(ObjectOutputStream s)
        throws IOException
    {
            String ifname = null;

        if (holder6.scope_ifname != null) {
            ifname = holder6.scope_ifname.getName();
            holder6.scope_ifname_set = true;
        }
        ObjectOutputStream.PutField pfields = s.putFields();
        pfields.put("ipaddress", holder6.ipaddress);
        pfields.put("scope_id", holder6.scope_id);
        pfields.put("scope_id_set", holder6.scope_id_set);
        pfields.put("scope_ifname_set", holder6.scope_ifname_set);
        pfields.put("ifname", ifname);
        s.writeFields();
    }

    /**
     * Utility routine to check if the InetAddress is an IP multicast
     * address. 11111111 at the start of the address identifies the
     * address as being a multicast address.
     *
     * @return a {@code boolean} indicating if the InetAddress is an IP
     *         multicast address
     */
    @Override
    public boolean isMulticastAddress() {
        return holder6.isMulticastAddress();
    }

    /**
     * Utility routine to check if the InetAddress is a wildcard address.
     *
     * @return a {@code boolean} indicating if the InetAddress is
     *         a wildcard address.
     */
    @Override
    public boolean isAnyLocalAddress() {
        return holder6.isAnyLocalAddress();
    }

    /**
     * Utility routine to check if the InetAddress is a loopback address.
     *
     * @return a {@code boolean} indicating if the InetAddress is a loopback
     *         address; or false otherwise.
     */
    @Override
    public boolean isLoopbackAddress() {
        return holder6.isLoopbackAddress();
    }

    /**
     * Utility routine to check if the InetAddress is an link local address.
     *
     * @return a {@code boolean} indicating if the InetAddress is a link local
     *         address; or false if address is not a link local unicast address.
     */
    @Override
    public boolean isLinkLocalAddress() {
        return holder6.isLinkLocalAddress();
    }

    /* static version of above */
    static boolean isLinkLocalAddress(byte[] ipaddress) {
        return ((ipaddress[0] & 0xff) == 0xfe
                && (ipaddress[1] & 0xc0) == 0x80);
    }

    /**
     * Utility routine to check if the InetAddress is a site local address.
     *
     * @return a {@code boolean} indicating if the InetAddress is a site local
     *         address; or false if address is not a site local unicast address.
     */
    @Override
    public boolean isSiteLocalAddress() {
        return holder6.isSiteLocalAddress();
    }

    /* static version of above */
    static boolean isSiteLocalAddress(byte[] ipaddress) {
        return ((ipaddress[0] & 0xff) == 0xfe
                && (ipaddress[1] & 0xc0) == 0xc0);
    }

    /**
     * Utility routine to check if the multicast address has global scope.
     *
     * @return a {@code boolean} indicating if the address has is a multicast
     *         address of global scope, false if it is not of global scope or
     *         it is not a multicast address
     */
    @Override
    public boolean isMCGlobal() {
        return holder6.isMCGlobal();
    }

    /**
     * Utility routine to check if the multicast address has node scope.
     *
     * @return a {@code boolean} indicating if the address has is a multicast
     *         address of node-local scope, false if it is not of node-local
     *         scope or it is not a multicast address
     */
    @Override
    public boolean isMCNodeLocal() {
        return holder6.isMCNodeLocal();
    }

    /**
     * Utility routine to check if the multicast address has link scope.
     *
     * @return a {@code boolean} indicating if the address has is a multicast
     *         address of link-local scope, false if it is not of link-local
     *         scope or it is not a multicast address
     */
    @Override
    public boolean isMCLinkLocal() {
        return holder6.isMCLinkLocal();
    }

    /**
     * Utility routine to check if the multicast address has site scope.
     *
     * @return a {@code boolean} indicating if the address has is a multicast
     *         address of site-local scope, false if it is not  of site-local
     *         scope or it is not a multicast address
     */
    @Override
    public boolean isMCSiteLocal() {
        return holder6.isMCSiteLocal();
    }

    /**
     * Utility routine to check if the multicast address has organization scope.
     *
     * @return a {@code boolean} indicating if the address has is a multicast
     *         address of organization-local scope, false if it is not of
     *         organization-local scope or it is not a multicast address
     */
    @Override
    public boolean isMCOrgLocal() {
        return holder6.isMCOrgLocal();
    }

    /**
     * Returns the raw IP address of this {@code InetAddress} object. The result
     * is in network byte order: the highest order byte of the address is in
     * {@code getAddress()[0]}.
     *
     * @return  the raw IP address of this object.
     */
    @Override
    public byte[] getAddress() {
        return holder6.ipaddress.clone();
    }

    /**
     * Returns a reference to the byte[] with the IPv6 address.
     */
    byte[] addressBytes() {
        return holder6.ipaddress;
    }

    /**
     * Returns the numeric scopeId, if this instance is associated with
     * an interface. If no scoped_id is set, the returned value is zero.
     *
     * @return the scopeId, or zero if not set.
     *
     * @since 1.5
     */
     public int getScopeId() {
         return holder6.scope_id;
     }

    /**
     * Returns the scoped interface, if this instance was created with
     * a scoped interface.
     *
     * @return the scoped interface, or null if not set.
     * @since 1.5
     */
     public NetworkInterface getScopedInterface() {
         return holder6.scope_ifname;
     }

    /**
     * Returns the IP address string in textual presentation. If the instance
     * was created specifying a scope identifier then the scope id is appended
     * to the IP address preceded by a "%" (per-cent) character. This can be
     * either a numeric value or a string, depending on which was used to create
     * the instance.
     *
     * @return  the raw IP address in a string format.
     */
    @Override
    public String getHostAddress() {
        return holder6.getHostAddress();
    }

    /**
     * Returns a hashcode for this IP address.
     *
     * @return  a hash code value for this IP address.
     */
    @Override
    public int hashCode() {
        return holder6.hashCode();
    }

    /**
     * Compares this object against the specified object. The result is {@code
     * true} if and only if the argument is not {@code null} and it represents
     * the same IP address as this object.
     *
     * <p> Two instances of {@code InetAddress} represent the same IP address
     * if the length of the byte arrays returned by {@code getAddress} is the
     * same for both, and each of the array components is the same for the byte
     * arrays.
     *
     * @param   obj   the object to compare against.
     *
     * @return  {@code true} if the objects are the same; {@code false} otherwise.
     *
     * @see     java.net.InetAddress#getAddress()
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Inet6Address inetAddr) {
            return holder6.equals(inetAddr.holder6);
        }
        return false;
    }

    /**
     * Utility routine to check if the InetAddress is an
     * IPv4 compatible IPv6 address.
     *
     * @return a {@code boolean} indicating if the InetAddress is an IPv4
     *         compatible IPv6 address; or false if address is IPv4 address.
     */
    public boolean isIPv4CompatibleAddress() {
        return holder6.isIPv4CompatibleAddress();
    }

    // Utilities

    private static final int INT16SZ = 2;

    /**
     * Convert IPv6 binary address into presentation (printable) format.
     *
     * @param src a byte array representing the IPv6 numeric address
     * @return a String representing an IPv6 address in
     *         textual representation format
     */
    static String numericToTextFormat(byte[] src) {
        StringBuilder sb = new StringBuilder(39);
        for (int i = 0; i < (INADDRSZ / INT16SZ); i++) {
            sb.append(Integer.toHexString(((src[i<<1]<<8) & 0xff00)
                                          | (src[(i<<1)+1] & 0xff)));
            if (i < (INADDRSZ / INT16SZ) -1 ) {
               sb.append(":");
            }
        }
        return sb.toString();
    }

    /**
     * Perform class load-time initializations.
     */
    private static native void init();
}
