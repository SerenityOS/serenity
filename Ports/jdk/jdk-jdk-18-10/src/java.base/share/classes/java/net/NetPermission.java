/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * This class is for various network permissions.
 * A NetPermission contains a name (also referred to as a "target name") but
 * no actions list; you either have the named permission
 * or you don't.
 * <P>
 * The target name is the name of the network permission (see below). The naming
 * convention follows the  hierarchical property naming convention.
 * Also, an asterisk
 * may appear at the end of the name, following a ".", or by itself, to
 * signify a wildcard match. For example: "foo.*" and "*" signify a wildcard
 * match, while "*foo" and "a*b" do not.
 * <P>
 * The following table lists the standard NetPermission target names,
 * and for each provides a description of what the permission allows
 * and a discussion of the risks of granting code the permission.
 *
 * <table class="striped">
 * <caption style="display:none">Permission target name, what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 *   <th scope="row">allowHttpTrace</th>
 *   <td>The ability to use the HTTP TRACE method in HttpURLConnection.</td>
 *   <td>Malicious code using HTTP TRACE could get access to security sensitive
 *   information in the HTTP headers (such as cookies) that it might not
 *   otherwise have access to.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">accessUnixDomainSocket</th>
 *   <td>The ability to accept, bind, connect or get the local address
 *   of a <i>Unix Domain</i> socket.
 *   </td>
 *   <td>Malicious code could connect to local processes using Unix domain sockets
 *    or impersonate local processes, by binding to the same pathnames (assuming they
 *    have the required Operating System permissions.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getCookieHandler</th>
 *   <td>The ability to get the cookie handler that processes highly
 *   security sensitive cookie information for an Http session.</td>
 *   <td>Malicious code can get a cookie handler to obtain access to
 *   highly security sensitive cookie information. Some web servers
 *   use cookies to save user private information such as access
 *   control information, or to track user browsing habit.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">getNetworkInformation</th>
 *   <td>The ability to retrieve all information about local network interfaces.</td>
 *   <td>Malicious code can read information about network hardware such as
 *   MAC addresses, which could be used to construct local IPv6 addresses.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getProxySelector</th>
 *   <td>The ability to get the proxy selector used to make decisions
 *   on which proxies to use when making network connections.</td>
 *   <td>Malicious code can get a ProxySelector to discover proxy
 *   hosts and ports on internal networks, which could then become
 *   targets for attack.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getResponseCache</th>
 *   <td>The ability to get the response cache that provides
 *   access to a local response cache.</td>
 *   <td>Malicious code getting access to the local response cache
 *   could access security sensitive information.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">requestPasswordAuthentication</th>
 *   <td>The ability
 *   to ask the authenticator registered with the system for
 *   a password</td>
 *   <td>Malicious code may steal this password.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setCookieHandler</th>
 *   <td>The ability to set the cookie handler that processes highly
 *   security sensitive cookie information for an Http session.</td>
 *   <td>Malicious code can set a cookie handler to obtain access to
 *   highly security sensitive cookie information. Some web servers
 *   use cookies to save user private information such as access
 *   control information, or to track user browsing habit.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">setDefaultAuthenticator</th>
 *   <td>The ability to set the
 *   way authentication information is retrieved when
 *   a proxy or HTTP server asks for authentication</td>
 *   <td>Malicious
 *   code can set an authenticator that monitors and steals user
 *   authentication input as it retrieves the input from the user.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setProxySelector</th>
 *   <td>The ability to set the proxy selector used to make decisions
 *   on which proxies to use when making network connections.</td>
 *   <td>Malicious code can set a ProxySelector that directs network
 *   traffic to an arbitrary network host.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setResponseCache</th>
 *   <td>The ability to set the response cache that provides access to
 *   a local response cache.</td>
 *   <td>Malicious code getting access to the local response cache
 *   could access security sensitive information, or create false
 *   entries in the response cache.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">setSocketImpl</th>
 *   <td>The ability to create a sub-class of Socket or ServerSocket with a
 *   user specified SocketImpl.</td>
 *   <td>Malicious user-defined SocketImpls can change the behavior of
 *   Socket and ServerSocket in surprising ways, by virtue of their
 *   ability to access the protected fields of SocketImpl.</td>
 *   </tr>
 *
 * <tr>
 *   <th scope="row">specifyStreamHandler</th>
 *   <td>The ability
 *   to specify a stream handler when constructing a URL</td>
 *   <td>Malicious code may create a URL with resources that it would
 *   normally not have access to (like file:/foo/fum/), specifying a
 *   stream handler that gets the actual bytes from someplace it does
 *   have access to. Thus it might be able to trick the system into
 *   creating a ProtectionDomain/CodeSource for a class even though
 *   that class really didn't come from that location.</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @implNote
 * Implementations may define additional target names, but should use naming
 * conventions such as reverse domain name notation to avoid name clashes.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 */

public final class NetPermission extends BasicPermission {
    @java.io.Serial
    private static final long serialVersionUID = -8343910153355041693L;

    /**
     * Creates a new NetPermission with the specified name.
     * The name is the symbolic name of the NetPermission, such as
     * "setDefaultAuthenticator", etc. An asterisk
     * may appear at the end of the name, following a ".", or by itself, to
     * signify a wildcard match.
     *
     * @param name the name of the NetPermission.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public NetPermission(String name)
    {
        super(name);
    }

    /**
     * Creates a new NetPermission object with the specified name.
     * The name is the symbolic name of the NetPermission, and the
     * actions String is currently unused and should be null.
     *
     * @param name the name of the NetPermission.
     * @param actions should be null.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public NetPermission(String name, String actions)
    {
        super(name, actions);
    }
}
