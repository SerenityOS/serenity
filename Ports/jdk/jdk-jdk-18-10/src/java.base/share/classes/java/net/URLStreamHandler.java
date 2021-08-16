/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;
import sun.net.util.IPAddressUtil;

/**
 * The abstract class {@code URLStreamHandler} is the common
 * superclass for all stream protocol handlers. A stream protocol
 * handler knows how to make a connection for a particular protocol
 * type, such as {@code http} or {@code https}.
 * <p>
 * In most cases, an instance of a {@code URLStreamHandler}
 * subclass is not created directly by an application. Rather, the
 * first time a protocol name is encountered when constructing a
 * {@code URL}, the appropriate stream protocol handler is
 * automatically loaded.
 *
 * @author  James Gosling
 * @see     java.net.URL#URL(java.lang.String, java.lang.String, int, java.lang.String)
 * @since   1.0
 */
public abstract class URLStreamHandler {
    /**
     * Constructor for subclasses to call.
     */
    public URLStreamHandler() {}

    /**
     * Opens a connection to the object referenced by the
     * {@code URL} argument.
     * This method should be overridden by a subclass.
     *
     * <p>If for the handler's protocol (such as HTTP or JAR), there
     * exists a public, specialized URLConnection subclass belonging
     * to one of the following packages or one of their subpackages:
     * java.lang, java.io, java.util, java.net, the connection
     * returned will be of that subclass. For example, for HTTP an
     * HttpURLConnection will be returned, and for JAR a
     * JarURLConnection will be returned.
     *
     * @param      u   the URL that this connects to.
     * @return     a {@code URLConnection} object for the {@code URL}.
     * @throws     IOException  if an I/O error occurs while opening the
     *               connection.
     */
    protected abstract URLConnection openConnection(URL u) throws IOException;

    /**
     * Same as openConnection(URL), except that the connection will be
     * made through the specified proxy; Protocol handlers that do not
     * support proxying will ignore the proxy parameter and make a
     * normal connection.
     *
     * <p> Calling this method preempts the system's default
     * {@link java.net.ProxySelector ProxySelector} settings.
     *
     * @implSpec
     * The default implementation of this method first checks that the given
     * {@code URL} and {@code Proxy} are not null, then throws {@code
     * UnsupportedOperationException}. Subclasses should override this method
     * with an appropriate implementation.
     *
     * @param      u   the URL that this connects to.
     * @param      p   the proxy through which the connection will be made.
     *                 If direct connection is desired, Proxy.NO_PROXY
     *                 should be specified.
     * @return     a {@code URLConnection} object for the {@code URL}.
     * @throws     IOException  if an I/O error occurs while opening the
     *               connection.
     * @throws     IllegalArgumentException if either u or p is null,
     *               or p has the wrong type.
     * @throws     UnsupportedOperationException if the subclass that
     *               implements the protocol doesn't support this method.
     * @since      1.5
     */
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        if (u == null || p == null)
            throw new IllegalArgumentException("null " + (u == null ? "url" : "proxy"));
        throw new UnsupportedOperationException("Method not implemented.");
    }

    /**
     * Parses the string representation of a {@code URL} into a
     * {@code URL} object.
     * <p>
     * If there is any inherited context, then it has already been
     * copied into the {@code URL} argument.
     * <p>
     * The {@code parseURL} method of {@code URLStreamHandler}
     * parses the string representation as if it were an
     * {@code http} specification. Most URL protocol families have a
     * similar parsing. A stream protocol handler for a protocol that has
     * a different syntax must override this routine.
     *
     * @param   u       the {@code URL} to receive the result of parsing
     *                  the spec.
     * @param   spec    the {@code String} representing the URL that
     *                  must be parsed.
     * @param   start   the character index at which to begin parsing. This is
     *                  just past the '{@code :}' (if there is one) that
     *                  specifies the determination of the protocol name.
     * @param   limit   the character position to stop parsing at. This is the
     *                  end of the string or the position of the
     *                  "{@code #}" character, if present. All information
     *                  after the sharp sign indicates an anchor.
     */
    protected void parseURL(URL u, String spec, int start, int limit) {
        // These fields may receive context content if this was relative URL
        String protocol = u.getProtocol();
        String authority = u.getAuthority();
        String userInfo = u.getUserInfo();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        String query = u.getQuery();

        // This field has already been parsed
        String ref = u.getRef();

        boolean isRelPath = false;
        boolean queryOnly = false;

// FIX: should not assume query if opaque
        // Strip off the query part
        if (start < limit) {
            int queryStart = spec.indexOf('?');
            queryOnly = queryStart == start;
            if ((queryStart != -1) && (queryStart < limit)) {
                query = spec.substring(queryStart+1, limit);
                limit = queryStart;
                spec = spec.substring(0, queryStart);
            }
        }

        int i;
        // Parse the authority part if any
        boolean isUNCName = (start <= limit - 4) &&
                        (spec.charAt(start) == '/') &&
                        (spec.charAt(start + 1) == '/') &&
                        (spec.charAt(start + 2) == '/') &&
                        (spec.charAt(start + 3) == '/');
        if (!isUNCName && (start <= limit - 2) && (spec.charAt(start) == '/') &&
            (spec.charAt(start + 1) == '/')) {
            start += 2;
            i = spec.indexOf('/', start);
            if (i < 0 || i > limit) {
                i = spec.indexOf('?', start);
                if (i < 0 || i > limit)
                    i = limit;
            }

            host = authority = spec.substring(start, i);

            int ind = authority.indexOf('@');
            if (ind != -1) {
                if (ind != authority.lastIndexOf('@')) {
                    // more than one '@' in authority. This is not server based
                    userInfo = null;
                    host = null;
                } else {
                    userInfo = authority.substring(0, ind);
                    host = authority.substring(ind+1);
                }
            } else {
                userInfo = null;
            }
            if (host != null) {
                // If the host is surrounded by [ and ] then its an IPv6
                // literal address as specified in RFC2732
                if (host.length()>0 && (host.charAt(0) == '[')) {
                    if ((ind = host.indexOf(']')) > 2) {

                        String nhost = host ;
                        host = nhost.substring(0,ind+1);
                        if (!IPAddressUtil.
                            isIPv6LiteralAddress(host.substring(1, ind))) {
                            throw new IllegalArgumentException(
                                "Invalid host: "+ host);
                        }

                        port = -1 ;
                        if (nhost.length() > ind+1) {
                            if (nhost.charAt(ind+1) == ':') {
                                ++ind ;
                                // port can be null according to RFC2396
                                if (nhost.length() > (ind + 1)) {
                                    port = Integer.parseInt(nhost, ind + 1,
                                        nhost.length(), 10);
                                }
                            } else {
                                throw new IllegalArgumentException(
                                    "Invalid authority field: " + authority);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException(
                            "Invalid authority field: " + authority);
                    }
                } else {
                    ind = host.indexOf(':');
                    port = -1;
                    if (ind >= 0) {
                        // port can be null according to RFC2396
                        if (host.length() > (ind + 1)) {
                            port = Integer.parseInt(host, ind + 1,
                                    host.length(), 10);
                        }
                        host = host.substring(0, ind);
                    }
                }
            } else {
                host = "";
            }
            if (port < -1)
                throw new IllegalArgumentException("Invalid port number :" +
                                                   port);
            start = i;
            // If the authority is defined then the path is defined by the
            // spec only; See RFC 2396 Section 5.2.4.
            if (!authority.isEmpty())
                path = "";
        }

        if (host == null) {
            host = "";
        }

        // Parse the file path if any
        if (start < limit) {
            String specStr = spec.substring(start, limit);
            if (spec.charAt(start) == '/') {
                path = specStr;
            } else if (path != null && !path.isEmpty()) {
                isRelPath = true;
                int ind = path.lastIndexOf('/');
                if (ind == -1 && authority != null) {
                    path = "/".concat(specStr);
                } else {
                    path = path.substring(0, ind + 1).concat(specStr);
                }
            } else {
                path = (authority != null) ? "/".concat(specStr) : specStr;
            }
        } else if (queryOnly && path != null) {
            int ind = path.lastIndexOf('/');
            if (ind < 0) {
                path = "/";
            } else {
                path = path.substring(0, ind + 1);
            }
        }
        if (path == null)
            path = "";

        if (isRelPath) {
            // Remove embedded /./
            while ((i = path.indexOf("/./")) >= 0) {
                path = path.substring(0, i) + path.substring(i + 2);
            }
            // Remove embedded /../ if possible
            i = 0;
            while ((i = path.indexOf("/../", i)) >= 0) {
                /*
                 * A "/../" will cancel the previous segment and itself,
                 * unless that segment is a "/../" itself
                 * i.e. "/a/b/../c" becomes "/a/c"
                 * but "/../../a" should stay unchanged
                 */
                if (i > 0 && (limit = path.lastIndexOf('/', i - 1)) >= 0 &&
                    (path.indexOf("/../", limit) != 0)) {
                    path = path.substring(0, limit).concat(path.substring(i + 3));
                    i = 0;
                } else {
                    i = i + 3;
                }
            }
            // Remove trailing .. if possible
            while (path.endsWith("/..")) {
                i = path.indexOf("/..");
                if ((limit = path.lastIndexOf('/', i - 1)) >= 0) {
                    path = path.substring(0, limit+1);
                } else {
                    break;
                }
            }
            // Remove starting .
            if (path.startsWith("./") && path.length() > 2)
                path = path.substring(2);

            // Remove trailing .
            if (path.endsWith("/."))
                path = path.substring(0, path.length() -1);
        }

        setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }

    /**
     * Returns the default port for a URL parsed by this handler. This method
     * is meant to be overridden by handlers with default port numbers.
     * @return the default port for a {@code URL} parsed by this handler.
     * @since 1.3
     */
    protected int getDefaultPort() {
        return -1;
    }

    /**
     * Provides the default equals calculation. May be overridden by handlers
     * for other protocols that have different requirements for equals().
     * This method requires that none of its arguments is null. This is
     * guaranteed by the fact that it is only called by java.net.URL class.
     * @param u1 a URL object
     * @param u2 a URL object
     * @return {@code true} if the two urls are
     * considered equal, i.e. they refer to the same
     * fragment in the same file.
     * @since 1.3
     */
    protected boolean equals(URL u1, URL u2) {
        return Objects.equals(u1.getRef(), u2.getRef()) && sameFile(u1, u2);
    }

    /**
     * Provides the default hash calculation. May be overridden by handlers for
     * other protocols that have different requirements for hashCode
     * calculation.
     * @param u a URL object
     * @return an {@code int} suitable for hash table indexing
     * @since 1.3
     */
    protected int hashCode(URL u) {
        int h = 0;

        // Generate the protocol part.
        String protocol = u.getProtocol();
        if (protocol != null)
            h += protocol.hashCode();

        // Generate the host part.
        InetAddress addr = getHostAddress(u);
        if (addr != null) {
            h += addr.hashCode();
        } else {
            String host = u.getHost();
            if (host != null)
                h += host.toLowerCase().hashCode();
        }

        // Generate the file part.
        String file = u.getFile();
        if (file != null)
            h += file.hashCode();

        // Generate the port part.
        if (u.getPort() == -1)
            h += getDefaultPort();
        else
            h += u.getPort();

        // Generate the ref part.
        String ref = u.getRef();
        if (ref != null)
            h += ref.hashCode();

        return h;
    }

    /**
     * Compare two urls to see whether they refer to the same file,
     * i.e., having the same protocol, host, port, and path.
     * This method requires that none of its arguments is null. This is
     * guaranteed by the fact that it is only called indirectly
     * by java.net.URL class.
     * @param u1 a URL object
     * @param u2 a URL object
     * @return true if u1 and u2 refer to the same file
     * @since 1.3
     */
    protected boolean sameFile(URL u1, URL u2) {
        // Compare the protocols.
        if (!((u1.getProtocol() == u2.getProtocol()) ||
              (u1.getProtocol() != null &&
               u1.getProtocol().equalsIgnoreCase(u2.getProtocol()))))
            return false;

        // Compare the files.
        if (!(u1.getFile() == u2.getFile() ||
              (u1.getFile() != null && u1.getFile().equals(u2.getFile()))))
            return false;

        // Compare the ports.
        int port1, port2;
        port1 = (u1.getPort() != -1) ? u1.getPort() : u1.handler.getDefaultPort();
        port2 = (u2.getPort() != -1) ? u2.getPort() : u2.handler.getDefaultPort();
        if (port1 != port2)
            return false;

        // Compare the hosts.
        if (!hostsEqual(u1, u2))
            return false;

        return true;
    }

    /**
     * Get the IP address of our host. An empty host field or a DNS failure
     * will result in a null return.
     *
     * @param u a URL object
     * @return an {@code InetAddress} representing the host
     * IP address.
     * @since 1.3
     */
    protected InetAddress getHostAddress(URL u) {
        return u.getHostAddress();
    }

    /**
     * Compares the host components of two URLs.
     * @param u1 the URL of the first host to compare
     * @param u2 the URL of the second host to compare
     * @return  {@code true} if and only if they
     * are equal, {@code false} otherwise.
     * @since 1.3
     */
    protected boolean hostsEqual(URL u1, URL u2) {
        InetAddress a1 = getHostAddress(u1);
        InetAddress a2 = getHostAddress(u2);
        // if we have internet address for both, compare them
        if (a1 != null && a2 != null) {
            return a1.equals(a2);
        // else, if both have host names, compare them
        } else if (u1.getHost() != null && u2.getHost() != null)
            return u1.getHost().equalsIgnoreCase(u2.getHost());
         else
            return u1.getHost() == null && u2.getHost() == null;
    }

    /**
     * Converts a {@code URL} of a specific protocol to a
     * {@code String}.
     *
     * @param   u   the URL.
     * @return  a string representation of the {@code URL} argument.
     */
    protected String toExternalForm(URL u) {
        String s;
        return u.getProtocol()
            + ':'
            + ((s = u.getAuthority()) != null && !s.isEmpty()
               ? "//" + s : "")
            + ((s = u.getPath()) != null ? s : "")
            + ((s = u.getQuery()) != null ? '?' + s : "")
            + ((s = u.getRef()) != null ? '#' + s : "");
    }

    /**
     * Sets the fields of the {@code URL} argument to the indicated values.
     * Only classes derived from URLStreamHandler are able
     * to use this method to set the values of the URL fields.
     *
     * @param   u         the URL to modify.
     * @param   protocol  the protocol name.
     * @param   host      the remote host value for the URL.
     * @param   port      the port on the remote machine.
     * @param   authority the authority part for the URL.
     * @param   userInfo the userInfo part of the URL.
     * @param   path      the path component of the URL.
     * @param   query     the query part for the URL.
     * @param   ref       the reference.
     * @throws          SecurityException       if the protocol handler of the URL is
     *                                  different from this one
     * @since 1.3
     */
    protected void setURL(URL u, String protocol, String host, int port,
                             String authority, String userInfo, String path,
                             String query, String ref) {
        if (this != u.handler) {
            throw new SecurityException("handler for url different from " +
                                        "this handler");
        } else if (host != null && u.isBuiltinStreamHandler(this)) {
            String s = IPAddressUtil.checkHostString(host);
            if (s != null) throw new IllegalArgumentException(s);
        }
        // ensure that no one can reset the protocol on a given URL.
        u.set(u.getProtocol(), host, port, authority, userInfo, path, query, ref);
    }

    /**
     * Sets the fields of the {@code URL} argument to the indicated values.
     * Only classes derived from URLStreamHandler are able
     * to use this method to set the values of the URL fields.
     *
     * @param   u         the URL to modify.
     * @param   protocol  the protocol name. This value is ignored since 1.2.
     * @param   host      the remote host value for the URL.
     * @param   port      the port on the remote machine.
     * @param   file      the file.
     * @param   ref       the reference.
     * @throws          SecurityException       if the protocol handler of the URL is
     *                                  different from this one
     * @deprecated Use setURL(URL, String, String, int, String, String, String,
     *             String);
     */
    @Deprecated
    protected void setURL(URL u, String protocol, String host, int port,
                          String file, String ref) {
        /*
         * Only old URL handlers call this, so assume that the host
         * field might contain "user:passwd@host". Fix as necessary.
         */
        String authority = null;
        String userInfo = null;
        if (host != null && !host.isEmpty()) {
            authority = (port == -1) ? host : host + ":" + port;
            int at = host.lastIndexOf('@');
            if (at != -1) {
                userInfo = host.substring(0, at);
                host = host.substring(at+1);
            }
        }

        /*
         * Assume file might contain query part. Fix as necessary.
         */
        String path = null;
        String query = null;
        if (file != null) {
            int q = file.lastIndexOf('?');
            if (q != -1) {
                query = file.substring(q+1);
                path = file.substring(0, q);
            } else
                path = file;
        }
        setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }
}
