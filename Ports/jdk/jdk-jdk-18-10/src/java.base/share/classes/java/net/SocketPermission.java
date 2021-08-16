/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import sun.net.util.IPAddressUtil;
import sun.net.PortConfig;
import sun.security.util.RegisteredDomain;
import sun.security.util.SecurityConstants;
import sun.security.util.Debug;


/**
 * This class represents access to a network via sockets.
 * A SocketPermission consists of a
 * host specification and a set of "actions" specifying ways to
 * connect to that host. The host is specified as
 * <pre>
 *    host = (hostname | IPv4address | iPv6reference) [:portrange]
 *    portrange = portnumber | -portnumber | portnumber-[portnumber]
 * </pre>
 * The host is expressed as a DNS name, as a numerical IP address,
 * or as "localhost" (for the local machine).
 * The wildcard "*" may be included once in a DNS name host
 * specification. If it is included, it must be in the leftmost
 * position, as in "*.example.com".
 * <p>
 * The format of the IPv6reference should follow that specified in <a
 * href="http://www.ietf.org/rfc/rfc2732.txt"><i>RFC&nbsp;2732: Format
 * for Literal IPv6 Addresses in URLs</i></a>:
 * <pre>
 *    ipv6reference = "[" IPv6address "]"
 *</pre>
 * For example, you can construct a SocketPermission instance
 * as the following:
 * <pre>
 *    String hostAddress = inetaddress.getHostAddress();
 *    if (inetaddress instanceof Inet6Address) {
 *        sp = new SocketPermission("[" + hostAddress + "]:" + port, action);
 *    } else {
 *        sp = new SocketPermission(hostAddress + ":" + port, action);
 *    }
 * </pre>
 * or
 * <pre>
 *    String host = url.getHost();
 *    sp = new SocketPermission(host + ":" + port, action);
 * </pre>
 * <p>
 * The <A HREF="Inet6Address.html#lform">full uncompressed form</A> of
 * an IPv6 literal address is also valid.
 * <p>
 * The port or portrange is optional. A port specification of the
 * form "N-", where <i>N</i> is a port number, signifies all ports
 * numbered <i>N</i> and above, while a specification of the
 * form "-N" indicates all ports numbered <i>N</i> and below.
 * The special port value {@code 0} refers to the entire <i>ephemeral</i>
 * port range. This is a fixed range of ports a system may use to
 * allocate dynamic ports from. The actual range may be system dependent.
 * <p>
 * The possible ways to connect to the host are
 * <pre>
 * accept
 * connect
 * listen
 * resolve
 * </pre>
 * The "listen" action is only meaningful when used with "localhost" and
 * means the ability to bind to a specified port.
 * The "resolve" action is implied when any of the other actions are present.
 * The action "resolve" refers to host/ip name service lookups.
 * <P>
 * The actions string is converted to lowercase before processing.
 * <p>As an example of the creation and meaning of SocketPermissions,
 * note that if the following permission:
 *
 * <pre>
 *   p1 = new SocketPermission("foo.example.com:7777", "connect,accept");
 * </pre>
 *
 * is granted to some code, it allows that code to connect to port 7777 on
 * {@code foo.example.com}, and to accept connections on that port.
 *
 * <p>Similarly, if the following permission:
 *
 * <pre>
 *   p2 = new SocketPermission("localhost:1024-", "accept,connect,listen");
 * </pre>
 *
 * is granted to some code, it allows that code to
 * accept connections on, connect to, or listen on any port between
 * 1024 and 65535 on the local host.
 *
 * <p>Note: Granting code permission to accept or make connections to remote
 * hosts may be dangerous because malevolent code can then more easily
 * transfer and share confidential data among parties who may not
 * otherwise have access to the data.
 *
 * @see java.security.Permissions
 * @see SocketPermission
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 *
 * @serial exclude
 */

public final class SocketPermission extends Permission
    implements java.io.Serializable
{
    @java.io.Serial
    private static final long serialVersionUID = -7204263841984476862L;

    /**
     * Connect to host:port
     */
    private static final int CONNECT    = 0x1;

    /**
     * Listen on host:port
     */
    private static final int LISTEN     = 0x2;

    /**
     * Accept a connection from host:port
     */
    private static final int ACCEPT     = 0x4;

    /**
     * Resolve DNS queries
     */
    private static final int RESOLVE    = 0x8;

    /**
     * No actions
     */
    private static final int NONE               = 0x0;

    /**
     * All actions
     */
    private static final int ALL        = CONNECT|LISTEN|ACCEPT|RESOLVE;

    // various port constants
    private static final int PORT_MIN = 0;
    private static final int PORT_MAX = 65535;
    private static final int PRIV_PORT_MAX = 1023;
    private static final int DEF_EPH_LOW = 49152;

    // the actions mask
    private transient int mask;

    /**
     * the actions string.
     *
     * @serial
     */

    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    // hostname part as it is passed
    private transient String hostname;

    // the canonical name of the host
    // in the case of "*.foo.com", cname is ".foo.com".

    private transient String cname;

    // all the IP addresses of the host
    private transient InetAddress[] addresses;

    // true if the hostname is a wildcard (e.g. "*.example.com")
    private transient boolean wildcard;

    // true if we were initialized with a single numeric IP address
    private transient boolean init_with_ip;

    // true if this SocketPermission represents an invalid/unknown host
    // used for implies when the delayed lookup has already failed
    private transient boolean invalid;

    // port range on host
    private transient int[] portrange;

    private transient boolean defaultDeny = false;

    // true if this SocketPermission represents a hostname
    // that failed our reverse mapping heuristic test
    private transient boolean untrusted;
    private transient boolean trusted;

    // true if the sun.net.trustNameService system property is set
    private static boolean trustNameService;

    private static Debug debug = null;
    private static boolean debugInit = false;

    // lazy initializer
    private static class EphemeralRange {
        static final int low = initEphemeralPorts("low", DEF_EPH_LOW);
            static final int high = initEphemeralPorts("high", PORT_MAX);
    };

    static {
        @SuppressWarnings("removal")
        Boolean tmp = java.security.AccessController.doPrivileged(
                new sun.security.action.GetBooleanAction("sun.net.trustNameService"));
        trustNameService = tmp.booleanValue();
    }

    private static synchronized Debug getDebug() {
        if (!debugInit) {
            debug = Debug.getInstance("access");
            debugInit = true;
        }
        return debug;
    }

    /**
     * Creates a new SocketPermission object with the specified actions.
     * The host is expressed as a DNS name, or as a numerical IP address.
     * Optionally, a port or a portrange may be supplied (separated
     * from the DNS name or IP address by a colon).
     * <p>
     * To specify the local machine, use "localhost" as the <i>host</i>.
     * Also note: An empty <i>host</i> String ("") is equivalent to "localhost".
     * <p>
     * The <i>actions</i> parameter contains a comma-separated list of the
     * actions granted for the specified host (and port(s)). Possible actions are
     * "connect", "listen", "accept", "resolve", or
     * any combination of those. "resolve" is automatically added
     * when any of the other three are specified.
     * <p>
     * Examples of SocketPermission instantiation are the following:
     * <pre>
     *    nr = new SocketPermission("www.example.com", "connect");
     *    nr = new SocketPermission("www.example.com:80", "connect");
     *    nr = new SocketPermission("*.example.com", "connect");
     *    nr = new SocketPermission("*.edu", "resolve");
     *    nr = new SocketPermission("204.160.241.0", "connect");
     *    nr = new SocketPermission("localhost:1024-65535", "listen");
     *    nr = new SocketPermission("204.160.241.0:1024-65535", "connect");
     * </pre>
     *
     * @param host the hostname or IP address of the computer, optionally
     * including a colon followed by a port or port range.
     * @param action the action string.
     *
     * @throws NullPointerException if any parameters are null
     * @throws IllegalArgumentException if the format of {@code host} is
     *         invalid, or if the {@code action} string is empty, malformed, or
     *         contains an action other than the specified possible actions
     */
    public SocketPermission(String host, String action) {
        super(getHost(host));
        // name initialized to getHost(host); NPE detected in getHost()
        init(getName(), getMask(action));
    }


    SocketPermission(String host, int mask) {
        super(getHost(host));
        // name initialized to getHost(host); NPE detected in getHost()
        init(getName(), mask);
    }

    private void setDeny() {
        defaultDeny = true;
    }

    private static String getHost(String host) {
        if (host.isEmpty()) {
            return "localhost";
        } else {
            /* IPv6 literal address used in this context should follow
             * the format specified in RFC 2732;
             * if not, we try to solve the unambiguous case
             */
            int ind;
            if (host.charAt(0) != '[') {
                if ((ind = host.indexOf(':')) != host.lastIndexOf(':')) {
                    /* More than one ":", meaning IPv6 address is not
                     * in RFC 2732 format;
                     * We will rectify user errors for all unambiguous cases
                     */
                    StringTokenizer st = new StringTokenizer(host, ":");
                    int tokens = st.countTokens();
                    if (tokens == 9) {
                        // IPv6 address followed by port
                        ind = host.lastIndexOf(':');
                        host = "[" + host.substring(0, ind) + "]" +
                            host.substring(ind);
                    } else if (tokens == 8 && host.indexOf("::") == -1) {
                        // IPv6 address only, not followed by port
                        host = "[" + host + "]";
                    } else {
                        // could be ambiguous
                        throw new IllegalArgumentException("Ambiguous"+
                                                           " hostport part");
                    }
                }
            }
            return host;
        }
    }

    private int[] parsePort(String port)
        throws Exception
    {

        if (port == null || port.isEmpty() || port.equals("*")) {
            return new int[] {PORT_MIN, PORT_MAX};
        }

        int dash = port.indexOf('-');

        if (dash == -1) {
            int p = Integer.parseInt(port);
            return new int[] {p, p};
        } else {
            String low = port.substring(0, dash);
            String high = port.substring(dash+1);
            int l,h;

            if (low.isEmpty()) {
                l = PORT_MIN;
            } else {
                l = Integer.parseInt(low);
            }

            if (high.isEmpty()) {
                h = PORT_MAX;
            } else {
                h = Integer.parseInt(high);
            }
            if (l < 0 || h < 0 || h<l)
                throw new IllegalArgumentException("invalid port range");

            return new int[] {l, h};
        }
    }

    /**
     * Returns true if the permission has specified zero
     * as its value (or lower bound) signifying the ephemeral range
     */
    private boolean includesEphemerals() {
        return portrange[0] == 0;
    }

    /**
     * Initialize the SocketPermission object. We don't do any DNS lookups
     * as this point, instead we hold off until the implies method is
     * called.
     */
    private void init(String host, int mask) {
        // Set the integer mask that represents the actions

        if ((mask & ALL) != mask)
            throw new IllegalArgumentException("invalid actions mask");

        // always OR in RESOLVE if we allow any of the others
        this.mask = mask | RESOLVE;

        // Parse the host name.  A name has up to three components, the
        // hostname, a port number, or two numbers representing a port
        // range.   "www.example.com:8080-9090" is a valid host name.

        // With IPv6 an address can be 2010:836B:4179::836B:4179
        // An IPv6 address needs to be enclose in []
        // For ex: [2010:836B:4179::836B:4179]:8080-9090
        // Refer to RFC 2732 for more information.

        int rb = 0 ;
        int start = 0, end = 0;
        int sep = -1;
        String hostport = host;
        if (host.charAt(0) == '[') {
            start = 1;
            rb = host.indexOf(']');
            if (rb != -1) {
                host = host.substring(start, rb);
            } else {
                throw new
                    IllegalArgumentException("invalid host/port: "+host);
            }
            sep = hostport.indexOf(':', rb+1);
        } else {
            start = 0;
            sep = host.indexOf(':', rb);
            end = sep;
            if (sep != -1) {
                host = host.substring(start, end);
            }
        }

        if (sep != -1) {
            String port = hostport.substring(sep+1);
            try {
                portrange = parsePort(port);
            } catch (Exception e) {
                throw new
                    IllegalArgumentException("invalid port range: "+port);
            }
        } else {
            portrange = new int[] { PORT_MIN, PORT_MAX };
        }

        hostname = host;

        // is this a domain wildcard specification
        if (host.lastIndexOf('*') > 0) {
            throw new
               IllegalArgumentException("invalid host wildcard specification");
        } else if (host.startsWith("*")) {
            wildcard = true;
            if (host.equals("*")) {
                cname = "";
            } else if (host.startsWith("*.")) {
                cname = host.substring(1).toLowerCase();
            } else {
              throw new
               IllegalArgumentException("invalid host wildcard specification");
            }
            return;
        } else {
            if (!host.isEmpty()) {
                // see if we are being initialized with an IP address.
                char ch = host.charAt(0);
                if (ch == ':' || Character.digit(ch, 16) != -1) {
                    byte ip[] = IPAddressUtil.textToNumericFormatV4(host);
                    if (ip == null) {
                        ip = IPAddressUtil.textToNumericFormatV6(host);
                    }
                    if (ip != null) {
                        try {
                            addresses =
                                new InetAddress[]
                                {InetAddress.getByAddress(ip) };
                            init_with_ip = true;
                        } catch (UnknownHostException uhe) {
                            // this shouldn't happen
                            invalid = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Convert an action string to an integer actions mask.
     *
     * @param action the action string
     * @return the action mask
     */
    private static int getMask(String action) {

        if (action == null) {
            throw new NullPointerException("action can't be null");
        }

        if (action.isEmpty()) {
            throw new IllegalArgumentException("action can't be empty");
        }

        int mask = NONE;

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (action == SecurityConstants.SOCKET_RESOLVE_ACTION) {
            return RESOLVE;
        } else if (action == SecurityConstants.SOCKET_CONNECT_ACTION) {
            return CONNECT;
        } else if (action == SecurityConstants.SOCKET_LISTEN_ACTION) {
            return LISTEN;
        } else if (action == SecurityConstants.SOCKET_ACCEPT_ACTION) {
            return ACCEPT;
        } else if (action == SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION) {
            return CONNECT|ACCEPT;
        }

        char[] a = action.toCharArray();

        int i = a.length - 1;
        if (i < 0)
            return mask;

        while (i != -1) {
            char c;

            // skip whitespace
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                               c == '\r' ||
                               c == '\n' ||
                               c == '\f' ||
                               c == '\t'))
                i--;

            // check for the known strings
            int matchlen;

            if (i >= 6 && (a[i-6] == 'c' || a[i-6] == 'C') &&
                          (a[i-5] == 'o' || a[i-5] == 'O') &&
                          (a[i-4] == 'n' || a[i-4] == 'N') &&
                          (a[i-3] == 'n' || a[i-3] == 'N') &&
                          (a[i-2] == 'e' || a[i-2] == 'E') &&
                          (a[i-1] == 'c' || a[i-1] == 'C') &&
                          (a[i] == 't' || a[i] == 'T'))
            {
                matchlen = 7;
                mask |= CONNECT;

            } else if (i >= 6 && (a[i-6] == 'r' || a[i-6] == 'R') &&
                                 (a[i-5] == 'e' || a[i-5] == 'E') &&
                                 (a[i-4] == 's' || a[i-4] == 'S') &&
                                 (a[i-3] == 'o' || a[i-3] == 'O') &&
                                 (a[i-2] == 'l' || a[i-2] == 'L') &&
                                 (a[i-1] == 'v' || a[i-1] == 'V') &&
                                 (a[i] == 'e' || a[i] == 'E'))
            {
                matchlen = 7;
                mask |= RESOLVE;

            } else if (i >= 5 && (a[i-5] == 'l' || a[i-5] == 'L') &&
                                 (a[i-4] == 'i' || a[i-4] == 'I') &&
                                 (a[i-3] == 's' || a[i-3] == 'S') &&
                                 (a[i-2] == 't' || a[i-2] == 'T') &&
                                 (a[i-1] == 'e' || a[i-1] == 'E') &&
                                 (a[i] == 'n' || a[i] == 'N'))
            {
                matchlen = 6;
                mask |= LISTEN;

            } else if (i >= 5 && (a[i-5] == 'a' || a[i-5] == 'A') &&
                                 (a[i-4] == 'c' || a[i-4] == 'C') &&
                                 (a[i-3] == 'c' || a[i-3] == 'C') &&
                                 (a[i-2] == 'e' || a[i-2] == 'E') &&
                                 (a[i-1] == 'p' || a[i-1] == 'P') &&
                                 (a[i] == 't' || a[i] == 'T'))
            {
                matchlen = 6;
                mask |= ACCEPT;

            } else {
                // parse error
                throw new IllegalArgumentException(
                        "invalid permission: " + action);
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            boolean seencomma = false;
            while (i >= matchlen && !seencomma) {
                switch (c = a[i-matchlen]) {
                case ' ': case '\r': case '\n':
                case '\f': case '\t':
                    break;
                default:
                    if (c == ',' && i > matchlen) {
                        seencomma = true;
                        break;
                    }
                    throw new IllegalArgumentException(
                            "invalid permission: " + action);
                }
                i--;
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen;
        }

        return mask;
    }

    private boolean isUntrusted()
        throws UnknownHostException
    {
        if (trusted) return false;
        if (invalid || untrusted) return true;
        try {
            if (!trustNameService && (defaultDeny ||
                sun.net.www.URLConnection.isProxiedHost(hostname))) {
                if (this.cname == null) {
                    this.getCanonName();
                }
                if (!match(cname, hostname)) {
                    // Last chance
                    if (!authorized(hostname, addresses[0].getAddress())) {
                        untrusted = true;
                        Debug debug = getDebug();
                        if (debug != null && Debug.isOn("failure")) {
                            debug.println("socket access restriction: proxied host " + "(" + addresses[0] + ")" + " does not match " + cname + " from reverse lookup");
                        }
                        return true;
                    }
                }
                trusted = true;
            }
        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }
        return false;
    }

    /**
     * attempt to get the fully qualified domain name
     *
     */
    void getCanonName()
        throws UnknownHostException
    {
        if (cname != null || invalid || untrusted) return;

        // attempt to get the canonical name

        try {
            // first get the IP addresses if we don't have them yet
            // this is because we need the IP address to then get
            // FQDN.
            if (addresses == null) {
                getIP();
            }

            // we have to do this check, otherwise we might not
            // get the fully qualified domain name
            if (init_with_ip) {
                cname = addresses[0].getHostName(false).toLowerCase();
            } else {
             cname = InetAddress.getByName(addresses[0].getHostAddress()).
                                              getHostName(false).toLowerCase();
            }
        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }
    }

    private transient String cdomain, hdomain;

    /**
     * previously we allowed domain names to be specified in IDN ACE form
     * Need to check for that and convert to Unicode
     */
    private static String checkForIDN(String name) {
        if (name.startsWith("xn--") || name.contains(".xn--")) {
            return IDN.toUnicode(name);
        } else {
            return name;
        }
    }

    private boolean match(String cname, String hname) {
        String a = checkForIDN(cname.toLowerCase());
        String b = checkForIDN(hname.toLowerCase());
        if (a.startsWith(b)  &&
            ((a.length() == b.length()) || (a.charAt(b.length()) == '.'))) {
            return true;
        }
        if (cdomain == null) {
            cdomain = RegisteredDomain.from(a)
                                      .map(RegisteredDomain::name)
                                      .orElse(a);
        }
        if (hdomain == null) {
            hdomain = RegisteredDomain.from(b)
                                      .map(RegisteredDomain::name)
                                      .orElse(b);
        }

        return !cdomain.isEmpty() && !hdomain.isEmpty() && cdomain.equals(hdomain);
    }

    private boolean authorized(String cname, byte[] addr) {
        if (addr.length == 4)
            return authorizedIPv4(cname, addr);
        else if (addr.length == 16)
            return authorizedIPv6(cname, addr);
        else
            return false;
    }

    private boolean authorizedIPv4(String cname, byte[] addr) {
        String authHost = "";
        InetAddress auth;

        try {
            authHost = "auth." +
                        (addr[3] & 0xff) + "." + (addr[2] & 0xff) + "." +
                        (addr[1] & 0xff) + "." + (addr[0] & 0xff) +
                        ".in-addr.arpa";
            // Following check seems unnecessary
            // auth = InetAddress.getAllByName0(authHost, false)[0];
            authHost = hostname + '.' + authHost;
            auth = InetAddress.getAllByName0(authHost, false)[0];
            if (auth.equals(InetAddress.getByAddress(addr))) {
                return true;
            }
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: IP address of " + auth + " != " + InetAddress.getByAddress(addr));
            }
        } catch (UnknownHostException uhe) {
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: forward lookup failed for " + authHost);
            }
        }
        return false;
    }

    private boolean authorizedIPv6(String cname, byte[] addr) {
        String authHost = "";
        InetAddress auth;

        try {
            StringBuilder sb = new StringBuilder(39);

            for (int i = 15; i >= 0; i--) {
                sb.append(Integer.toHexString(((addr[i]) & 0x0f)));
                sb.append('.');
                sb.append(Integer.toHexString(((addr[i] >> 4) & 0x0f)));
                sb.append('.');
            }
            authHost = "auth." + sb.toString() + "IP6.ARPA";
            //auth = InetAddress.getAllByName0(authHost, false)[0];
            authHost = hostname + '.' + authHost;
            auth = InetAddress.getAllByName0(authHost, false)[0];
            if (auth.equals(InetAddress.getByAddress(addr)))
                return true;
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: IP address of " + auth + " != " + InetAddress.getByAddress(addr));
            }
        } catch (UnknownHostException uhe) {
            Debug debug = getDebug();
            if (debug != null && Debug.isOn("failure")) {
                debug.println("socket access restriction: forward lookup failed for " + authHost);
            }
        }
        return false;
    }


    /**
     * get IP addresses. Sets invalid to true if we can't get them.
     *
     */
    void getIP()
        throws UnknownHostException
    {
        if (addresses != null || wildcard || invalid) return;

        try {
            // now get all the IP addresses
            String host;
            if (getName().charAt(0) == '[') {
                // Literal IPv6 address
                host = getName().substring(1, getName().indexOf(']'));
            } else {
                int i = getName().indexOf(':');
                if (i == -1)
                    host = getName();
                else {
                    host = getName().substring(0,i);
                }
            }

            addresses =
                new InetAddress[] {InetAddress.getAllByName0(host, false)[0]};

        } catch (UnknownHostException uhe) {
            invalid = true;
            throw uhe;
        }  catch (IndexOutOfBoundsException iobe) {
            invalid = true;
            throw new UnknownHostException(getName());
        }
    }

    /**
     * Checks if this socket permission object "implies" the
     * specified permission.
     * <P>
     * More specifically, this method first ensures that all of the following
     * are true (and returns false if any of them are not):
     * <ul>
     * <li> <i>p</i> is an instanceof SocketPermission,
     * <li> <i>p</i>'s actions are a proper subset of this
     * object's actions, and
     * <li> <i>p</i>'s port range is included in this port range. Note:
     * port range is ignored when p only contains the action, 'resolve'.
     * </ul>
     *
     * Then {@code implies} checks each of the following, in order,
     * and for each returns true if the stated condition is true:
     * <ul>
     * <li> If this object was initialized with a single IP address and one of <i>p</i>'s
     * IP addresses is equal to this object's IP address.
     * <li>If this object is a wildcard domain (such as *.example.com), and
     * <i>p</i>'s canonical name (the name without any preceding *)
     * ends with this object's canonical host name. For example, *.example.com
     * implies *.foo.example.com.
     * <li>If this object was not initialized with a single IP address, and one of this
     * object's IP addresses equals one of <i>p</i>'s IP addresses.
     * <li>If this canonical name equals <i>p</i>'s canonical name.
     * </ul>
     *
     * If none of the above are true, {@code implies} returns false.
     * @param p the permission to check against.
     *
     * @return true if the specified permission is implied by this object,
     * false if not.
     */
    @Override
    public boolean implies(Permission p) {
        int i,j;

        if (!(p instanceof SocketPermission that))
            return false;

        if (p == this)
            return true;

        return ((this.mask & that.mask) == that.mask) &&
                                        impliesIgnoreMask(that);
    }

    /**
     * Checks if the incoming Permission's action are a proper subset of
     * the this object's actions.
     * <P>
     * Check, in the following order:
     * <ul>
     * <li> Checks that "p" is an instanceof a SocketPermission
     * <li> Checks that "p"'s actions are a proper subset of the
     * current object's actions.
     * <li> Checks that "p"'s port range is included in this port range
     * <li> If this object was initialized with an IP address, checks that
     *      one of "p"'s IP addresses is equal to this object's IP address.
     * <li> If either object is a wildcard domain (i.e., "*.example.com"),
     *      attempt to match based on the wildcard.
     * <li> If this object was not initialized with an IP address, attempt
     *      to find a match based on the IP addresses in both objects.
     * <li> Attempt to match on the canonical hostnames of both objects.
     * </ul>
     * @param that the incoming permission request
     *
     * @return true if "permission" is a proper subset of the current object,
     * false if not.
     */
    boolean impliesIgnoreMask(SocketPermission that) {
        int i,j;

        if ((that.mask & RESOLVE) != that.mask) {

            // check simple port range
            if ((that.portrange[0] < this.portrange[0]) ||
                    (that.portrange[1] > this.portrange[1])) {

                // if either includes the ephemeral range, do full check
                if (this.includesEphemerals() || that.includesEphemerals()) {
                    if (!inRange(this.portrange[0], this.portrange[1],
                                     that.portrange[0], that.portrange[1]))
                    {
                                return false;
                    }
                } else {
                    return false;
                }
            }
        }

        // allow a "*" wildcard to always match anything
        if (this.wildcard && "".equals(this.cname))
            return true;

        // return if either one of these NetPerm objects are invalid...
        if (this.invalid || that.invalid) {
            return compareHostnames(that);
        }

        try {
            if (this.init_with_ip) { // we only check IP addresses
                if (that.wildcard)
                    return false;

                if (that.init_with_ip) {
                    return (this.addresses[0].equals(that.addresses[0]));
                } else {
                    if (that.addresses == null) {
                        that.getIP();
                    }
                    for (i=0; i < that.addresses.length; i++) {
                        if (this.addresses[0].equals(that.addresses[i]))
                            return true;
                    }
                }
                // since "this" was initialized with an IP address, we
                // don't check any other cases
                return false;
            }

            // check and see if we have any wildcards...
            if (this.wildcard || that.wildcard) {
                // if they are both wildcards, return true iff
                // that's cname ends with this cname (i.e., *.example.com
                // implies *.foo.example.com)
                if (this.wildcard && that.wildcard)
                    return (that.cname.endsWith(this.cname));

                // a non-wildcard can't imply a wildcard
                if (that.wildcard)
                    return false;

                // this is a wildcard, lets see if that's cname ends with
                // it...
                if (that.cname == null) {
                    that.getCanonName();
                }
                return (that.cname.endsWith(this.cname));
            }

            // compare IP addresses
            if (this.addresses == null) {
                this.getIP();
            }

            if (that.addresses == null) {
                that.getIP();
            }

            if (!(that.init_with_ip && this.isUntrusted())) {
                for (j = 0; j < this.addresses.length; j++) {
                    for (i=0; i < that.addresses.length; i++) {
                        if (this.addresses[j].equals(that.addresses[i]))
                            return true;
                    }
                }

                // XXX: if all else fails, compare hostnames?
                // Do we really want this?
                if (this.cname == null) {
                    this.getCanonName();
                }

                if (that.cname == null) {
                    that.getCanonName();
                }

                return (this.cname.equalsIgnoreCase(that.cname));
            }

        } catch (UnknownHostException uhe) {
            return compareHostnames(that);
        }

        // make sure the first thing that is done here is to return
        // false. If not, uncomment the return false in the above catch.

        return false;
    }

    private boolean compareHostnames(SocketPermission that) {
        // we see if the original names/IPs passed in were equal.

        String thisHost = hostname;
        String thatHost = that.hostname;

        if (thisHost == null) {
            return false;
        } else if (this.wildcard) {
            final int cnameLength = this.cname.length();
            return thatHost.regionMatches(true,
                                          (thatHost.length() - cnameLength),
                                          this.cname, 0, cnameLength);
        } else {
            return thisHost.equalsIgnoreCase(thatHost);
        }
    }

    /**
     * Checks two SocketPermission objects for equality.
     *
     * @param obj the object to test for equality with this object.
     *
     * @return true if <i>obj</i> is a SocketPermission, and has the
     *  same hostname, port range, and actions as this
     *  SocketPermission object. However, port range will be ignored
     *  in the comparison if <i>obj</i> only contains the action, 'resolve'.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof SocketPermission that))
            return false;

        //this is (overly?) complex!!!

        // check the mask first
        if (this.mask != that.mask) return false;

        if ((that.mask & RESOLVE) != that.mask) {
            // now check the port range...
            if ((this.portrange[0] != that.portrange[0]) ||
                (this.portrange[1] != that.portrange[1])) {
                return false;
            }
        }

        // short cut. This catches:
        //  "crypto" equal to "crypto", or
        // "1.2.3.4" equal to "1.2.3.4.", or
        //  "*.edu" equal to "*.edu", but it
        //  does not catch "crypto" equal to
        // "crypto.foo.example.com".

        if (this.getName().equalsIgnoreCase(that.getName())) {
            return true;
        }

        // we now attempt to get the Canonical (FQDN) name and
        // compare that. If this fails, about all we can do is return
        // false.

        try {
            this.getCanonName();
            that.getCanonName();
        } catch (UnknownHostException uhe) {
            return false;
        }

        if (this.invalid || that.invalid)
            return false;

        if (this.cname != null) {
            return this.cname.equalsIgnoreCase(that.cname);
        }

        return false;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        /*
         * If this SocketPermission was initialized with an IP address
         * or a wildcard, use getName().hashCode(), otherwise use
         * the hashCode() of the host name returned from
         * java.net.InetAddress.getHostName method.
         */

        if (init_with_ip || wildcard) {
            return this.getName().hashCode();
        }

        try {
            getCanonName();
        } catch (UnknownHostException uhe) {

        }

        if (invalid || cname == null)
            return this.getName().hashCode();
        else
            return this.cname.hashCode();
    }

    /**
     * Return the current action mask.
     *
     * @return the actions mask.
     */

    int getMask() {
        return mask;
    }

    /**
     * Returns the "canonical string representation" of the actions in the
     * specified mask.
     * Always returns present actions in the following order:
     * connect, listen, accept, resolve.
     *
     * @param mask a specific integer action mask to translate into a string
     * @return the canonical string representation of the actions
     */
    private static String getActions(int mask) {
        StringJoiner sj = new StringJoiner(",");
        if ((mask & CONNECT) == CONNECT) {
            sj.add("connect");
        }
        if ((mask & LISTEN) == LISTEN) {
            sj.add("listen");
        }
        if ((mask & ACCEPT) == ACCEPT) {
            sj.add("accept");
        }
        if ((mask & RESOLVE) == RESOLVE) {
            sj.add("resolve");
        }
        return sj.toString();
    }

    /**
     * Returns the canonical string representation of the actions.
     * Always returns present actions in the following order:
     * connect, listen, accept, resolve.
     *
     * @return the canonical string representation of the actions.
     */
    @Override
    public String getActions()
    {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Returns a new PermissionCollection object for storing SocketPermission
     * objects.
     * <p>
     * SocketPermission objects must be stored in a manner that allows them
     * to be inserted into the collection in any order, but that also enables the
     * PermissionCollection {@code implies}
     * method to be implemented in an efficient (and consistent) manner.
     *
     * @return a new PermissionCollection object suitable for storing SocketPermissions.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new SocketPermissionCollection();
    }

    /**
     * {@code writeObject} is called to save the state of the
     * {@code SocketPermission} to a stream. The actions are serialized,
     * and the superclass takes care of the name.
     *
     * @param  s the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }

    /**
     * {@code readObject} is called to restore the state of the
     * {@code SocketPermission} from a stream.
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private synchronized void readObject(java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the action, then initialize the rest
        s.defaultReadObject();
        init(getName(),getMask(actions));
    }

    /**
     * Check the system/security property for the ephemeral port range
     * for this system. The suffix is either "high" or "low"
     */
    @SuppressWarnings("removal")
    private static int initEphemeralPorts(String suffix, int defval) {
        return AccessController.doPrivileged(
            new PrivilegedAction<>(){
                public Integer run() {
                    int val = Integer.getInteger(
                            "jdk.net.ephemeralPortRange."+suffix, -1
                    );
                    if (val != -1) {
                        return val;
                    } else {
                        return suffix.equals("low") ?
                            PortConfig.getLower() : PortConfig.getUpper();
                    }
                }
            }
        );
    }

    /**
     * Check if the target range is within the policy range
     * together with the ephemeral range for this platform
     * (if policy includes ephemeral range)
     */
    private static boolean inRange(
        int policyLow, int policyHigh, int targetLow, int targetHigh
    )
    {
        final int ephemeralLow = EphemeralRange.low;
        final int ephemeralHigh = EphemeralRange.high;

        if (targetLow == 0) {
            // check policy includes ephemeral range
            if (!inRange(policyLow, policyHigh, ephemeralLow, ephemeralHigh)) {
                return false;
            }
            if (targetHigh == 0) {
                // nothing left to do
                return true;
            }
            // continue check with first real port number
            targetLow = 1;
        }

        if (policyLow == 0 && policyHigh == 0) {
            // ephemeral range only
            return targetLow >= ephemeralLow && targetHigh <= ephemeralHigh;
        }

        if (policyLow != 0) {
            // simple check of policy only
            return targetLow >= policyLow && targetHigh <= policyHigh;
        }

        // policyLow == 0 which means possibly two ranges to check

        // first check if policy and ephem range overlap/contiguous

        if (policyHigh >= ephemeralLow - 1) {
            return targetHigh <= ephemeralHigh;
        }

        // policy and ephem range do not overlap

        // target range must lie entirely inside policy range or eph range

        return  (targetLow <= policyHigh && targetHigh <= policyHigh) ||
                (targetLow >= ephemeralLow && targetHigh <= ephemeralHigh);
    }
    /*
    public String toString()
    {
        StringBuffer s = new StringBuffer(super.toString() + "\n" +
            "cname = " + cname + "\n" +
            "wildcard = " + wildcard + "\n" +
            "invalid = " + invalid + "\n" +
            "portrange = " + portrange[0] + "," + portrange[1] + "\n");
        if (addresses != null) for (int i=0; i<addresses.length; i++) {
            s.append( addresses[i].getHostAddress());
            s.append("\n");
        } else {
            s.append("(no addresses)\n");
        }

        return s.toString();
    }

    public static void main(String args[]) throws Exception {
        SocketPermission this_ = new SocketPermission(args[0], "connect");
        SocketPermission that_ = new SocketPermission(args[1], "connect");
        System.out.println("-----\n");
        System.out.println("this.implies(that) = " + this_.implies(that_));
        System.out.println("-----\n");
        System.out.println("this = "+this_);
        System.out.println("-----\n");
        System.out.println("that = "+that_);
        System.out.println("-----\n");

        SocketPermissionCollection nps = new SocketPermissionCollection();
        nps.add(this_);
        nps.add(new SocketPermission("www-leland.stanford.edu","connect"));
        nps.add(new SocketPermission("www-example.com","connect"));
        System.out.println("nps.implies(that) = " + nps.implies(that_));
        System.out.println("-----\n");
    }
    */
}

/**

if (init'd with IP, key is IP as string)
if wildcard, its the wild card
else its the cname?

 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Roland Schemers
 *
 * @serial include
 */

final class SocketPermissionCollection extends PermissionCollection
    implements Serializable
{
    // Not serialized; see serialization section at end of class
    private transient Map<String, SocketPermission> perms;

    /**
     * Create an empty SocketPermissionCollection object.
     */
    public SocketPermissionCollection() {
        perms = new ConcurrentHashMap<>();
    }

    /**
     * Adds a permission to the SocketPermissions. The key for the hash is
     * the name in the case of wildcards, or all the IP addresses.
     *
     * @param permission the Permission object to add.
     *
     * @throws    IllegalArgumentException   if the permission is not a
     *                                       SocketPermission
     *
     * @throws    SecurityException   if this SocketPermissionCollection object
     *                                has been marked readonly
     */
    @Override
    public void add(Permission permission) {
        if (! (permission instanceof SocketPermission sp))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        // Add permission to map if it is absent, or replace with new
        // permission if applicable. NOTE: cannot use lambda for
        // remappingFunction parameter until JDK-8076596 is fixed.
        perms.merge(sp.getName(), sp,
            new java.util.function.BiFunction<>() {
                @Override
                public SocketPermission apply(SocketPermission existingVal,
                                              SocketPermission newVal) {
                    int oldMask = existingVal.getMask();
                    int newMask = newVal.getMask();
                    if (oldMask != newMask) {
                        int effective = oldMask | newMask;
                        if (effective == newMask) {
                            return newVal;
                        }
                        if (effective != oldMask) {
                            return new SocketPermission(sp.getName(),
                                                        effective);
                        }
                    }
                    return existingVal;
                }
            }
        );
    }

    /**
     * Check and see if this collection of permissions implies the permissions
     * expressed in "permission".
     *
     * @param permission the Permission object to compare
     *
     * @return true if "permission" is a proper subset of a permission in
     * the collection, false if not.
     */
    @Override
    public boolean implies(Permission permission)
    {
        if (! (permission instanceof SocketPermission np))
                return false;

        int desired = np.getMask();
        int effective = 0;
        int needed = desired;

        var hit = perms.get(np.getName());
        if (hit != null) {
            // fastpath, if the host was explicitly listed
            if (((needed & hit.getMask()) != 0) && hit.impliesIgnoreMask(np)) {
                effective |= hit.getMask();
                if ((effective & desired) == desired) {
                    return true;
                }
                needed = (desired & ~effective);
            }
        }

        //System.out.println("implies "+np);
        for (SocketPermission x : perms.values()) {
            //System.out.println("  trying "+x);
            if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(np)) {
                effective |=  x.getMask();
                if ((effective & desired) == desired) {
                    return true;
                }
                needed = (desired & ~effective);
            }
        }
        return false;
    }

    /**
     * Returns an enumeration of all the SocketPermission objects in the
     * container.
     *
     * @return an enumeration of all the SocketPermission objects.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<Permission> elements() {
        return (Enumeration)Collections.enumeration(perms.values());
    }

    @java.io.Serial
    private static final long serialVersionUID = 2787186408602843674L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:

    //
    // The SocketPermissions for this set.
    // @serial
    //
    // private Vector permissions;

    /**
     * @serialField permissions java.util.Vector
     *     A list of the SocketPermissions for this set.
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Vector.class),
    };

    /**
     * Writes the state of this object to the stream.
     * @serialData "permissions" field (a Vector containing the SocketPermissions).
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    /*
     * Writes the contents of the perms field out as a Vector for
     * serialization compatibility with earlier releases.
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Write out Vector
        Vector<SocketPermission> permissions = new Vector<>(perms.values());

        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /**
     * Reads in a {@code Vector} of {@code SocketPermission} and saves
     * them in the perms field.
     *
     * @param  in the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call in.defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get the one we want
        @SuppressWarnings("unchecked")
        Vector<SocketPermission> permissions = (Vector<SocketPermission>)gfields.get("permissions", null);
        perms = new ConcurrentHashMap<>(permissions.size());
        for (SocketPermission sp : permissions) {
            perms.put(sp.getName(), sp);
        }
    }
}
