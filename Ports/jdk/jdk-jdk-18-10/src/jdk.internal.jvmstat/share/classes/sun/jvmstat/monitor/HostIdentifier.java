/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.monitor;

import java.net.*;

/**
 * An abstraction that identifies a target host and communications
 * protocol. The HostIdentifier, or hostid, provides a convenient string
 * representation of the information needed to locate and communicate with
 * a target host. The string, based on a {@link URI}, may specify the
 * the communications protocol, host name, and protocol specific information
 * for a target host. The format for a HostIdentifier string is:
 * <pre>
 *       [<I>protocol</I>:][[<I>//</I>]<I>hostname</I>][<I>:port</I>][<I>/servername</I>]
 * </pre>
 * There are actually no required components of this string, as a null string
 * is interpreted to mean a local connection to the local host and is equivalent
 * to the string <em>local://localhost</em>. The components of the
 * HostIdentifier are:
 * <ul>
 *   <li><p>{@code protocol} - The communications protocol. If omitted,
 *          and a hostname is not specified, then default local protocol,
 *          <em>local:</em>, is assumed. If the protocol is omitted and a
 *          hostname is specified then the default remote protocol,
 *          <em>rmi:</em> is assumed.
 *       </p></li>
 *   <li><p>{@code hostname} - The hostname. If omitted, then
 *          <em>localhost</em> is assumed. If the protocol is also omitted,
 *          then default local protocol <em>local:</em> is also assumed.
 *          If the hostname is not omitted but the protocol is omitted,
 *          then the default remote protocol, <em>rmi:</em> is assumed.
 *       </p></li>
 *   <li><p>{@code port} - The port for the communications protocol.
 *          Treatment of the {@code port} parameter is implementation
 *          (protocol) specific. It is unused by the default local protocol,
 *          <em>local:</em>. For the default remote protocol, <em>rmi:</em>,
 *          {@code port} indicates the port number of the <em>rmiregistry</em>
 *          on the target host and defaults to port 1099.
 *       </p></li>
 *   <li><p>{@code servername} - The treatment of the Path, Query, and
 *          Fragment components of the HostIdentifier are implementation
 *          (protocol) dependent. These components are ignored by the
 *          default local protocol, <em>local:</em>. For the default remote
 *          protocol, <em>rmi</em>, the Path component is interpreted as
 *          the name of the RMI remote object. The Query component may
 *          contain an access mode specifier <em>?mode=</em> specifying
 *          <em>"r"</em> or <em>"rw"</em> access (write access currently
 *          ignored). The Fragment part is ignored.
 *       </p></li>
 * </ul>
 * <p>
 * All HostIdentifier objects are represented as absolute, hierarchical URIs.
 * The constructors accept relative URIs, but these will generally be
 * transformed into an absolute URI specifying a default protocol. A
 * HostIdentifier differs from a URI in that certain contractions and
 * illicit syntactical constructions are allowed. The following are all
 * valid HostIdentifier strings:
 *
 * <ul>
 *   <li>{@code <null>} - transformed into "//localhost"</li>
 *   <li>localhost - transformed into "//localhost"</li>
 *   <li>hostname - transformed into "//hostname"</li>
 *   <li>hostname:port - transformed into "//hostname:port"</li>
 *   <li>proto:hostname - transformed into "proto://hostname"</li>
 *   <li>proto:hostname:port - transformed into
 *          "proto://hostname:port"</li>
 *   <li>proto://hostname:port</li>
 * </ul>
 *
 * @see URI
 * @see VmIdentifier
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class HostIdentifier {
    private URI uri;

    /**
     * creates a canonical representation of the uriString. This method
     * performs certain translations depending on the type of URI generated
     * by the string.
     */
    private URI canonicalize(String uriString) throws URISyntaxException {
        if ((uriString == null) || (uriString.compareTo("localhost") == 0)) {
            uriString = "//localhost";
            return new URI(uriString);
        }

        if (Character.isDigit(uriString.charAt(0))) {
            // may be hostname or hostname:port since it starts with digits
            uriString = "//" + uriString;
        }

        URI u = new URI(uriString);

        if (u.isAbsolute()) {
            if (u.isOpaque()) {
                /*
                 * this code is here to deal with a special case. For ease of
                 * use, we'd like to be able to handle the case where the user
                 * specifies hostname:port, not requiring the scheme part.
                 * This introduces some subtleties.
                 *     hostname:port - scheme = hostname
                 *                   - schemespecificpart = port
                 *                   - hostname = null
                 *                   - userinfo=null
                 * however, someone could also enter scheme:hostname:port and
                 * get into this code. the strategy is to consider this
                 * syntax illegal and provide some code to defend against it.
                 * Basically, we test that the string contains only one ":"
                 * and that the ssp is numeric. If we get two colons, we will
                 * attempt to insert the "//" after the first colon and then
                 * try to create a URI from the resulting string.
                 */
                String scheme = u.getScheme();
                String ssp = u.getSchemeSpecificPart();
                String frag = u.getFragment();
                URI u2 = null;

                int c1index = uriString.indexOf(':');
                int c2index = uriString.lastIndexOf(':');
                if (c2index != c1index) {
                    /*
                     * this is the scheme:hostname:port case. Attempt to
                     * transform this to scheme://hostname:port. If a path
                     * part is part of the original strings, it will be
                     * included in the SchemeSpecificPart. however, the
                     * fragment part must be handled separately.
                     */
                    if (frag == null) {
                        u2 = new URI(scheme + "://" + ssp);
                    } else {
                        u2 = new URI(scheme + "://" + ssp + "#" + frag);
                    }
                    return u2;
                }
                /*
                 * here we have the <string>:<string> case, possibly with
                 * optional path and fragment components. we assume that
                 * the part following the colon is a number. we don't check
                 * this condition here as it will get detected later anyway.
                 */
                u2 = new URI("//" + uriString);
                return u2;
            } else {
                return u;
            }
        } else {
            /*
             * This is the case where we were given a hostname followed
             * by a path part, fragment part, or both a path and fragment
             * part. The key here is that no scheme part was specified.
             * For this case, if the scheme specific part does not begin
             * with "//", then we prefix the "//" to the given string and
             * attempt to create a URI from the resulting string.
             */
            String ssp = u.getSchemeSpecificPart();
            if (ssp.startsWith("//")) {
                return u;
            } else {
                return new URI("//" + uriString);
            }
        }
    }

    /**
     * Create a HostIdentifier instance from a string value.
     *
     * @param uriString a string representing a target host. The syntax of
     *                  the string must conform to the rules specified in the
     *                  class documentation.
     *
     * @throws URISyntaxException Thrown when the uriString or its canonical
     *                            form is poorly formed. This exception may
     *                            get encapsulated into a MonitorException in
     *                            a future version.
     *
     */
    public HostIdentifier(String uriString) throws URISyntaxException {
        uri = canonicalize(uriString);
    }

    /**
     * Create a HostIdentifier instance from component parts of a URI.
     *
     * @param scheme the {@link URI#getScheme} component of a URI.
     * @param authority the {@link URI#getAuthority} component of a URI.
     * @param path the {@link URI#getPath} component of a URI.
     * @param query the {@link URI#getQuery} component of a URI.
     * @param fragment the {@link URI#getFragment} component of a URI.
     *
     * @throws URISyntaxException Thrown when the uriString or its canonical
     *                            form is poorly formed. This exception may
     *                            get encapsulated into a MonitorException in
     *                            a future version.
     * @see URI
     */
    public HostIdentifier(String scheme, String authority, String path,
                          String query, String fragment)
           throws URISyntaxException {
        uri = new URI(scheme, authority, path, query, fragment);
    }

    /**
     * Create a HostIdentifier instance from a VmIdentifier.
     *
     * The necessary components of the VmIdentifier are extracted and
     * reassembled into a HostIdentifier. If a "file:" scheme (protocol)
     * is specified, the returned HostIdentifier will always be
     * equivalent to HostIdentifier("file://localhost").
     *
     * @param vmid the VmIdentifier use to construct the HostIdentifier.
     */
    public HostIdentifier(VmIdentifier vmid) {
        /*
         * Extract all components of the VmIdentifier URI except the
         * user-info part of the authority (the lvmid).
         */
        StringBuilder sb = new StringBuilder();
        String scheme = vmid.getScheme();
        String host = vmid.getHost();
        String authority = vmid.getAuthority();

        // check for 'file:' VmIdentifiers and handled as a special case.
        if ((scheme != null) && (scheme.compareTo("file") == 0)) {
            try {
                uri = new URI("file://localhost");
            } catch (URISyntaxException e) { };
            return;
        }

        if ((host != null) && (host.compareTo(authority) == 0)) {
            /*
             * this condition occurs when the VmIdentifier specifies only
             * the authority (i.e. the lvmid ), and not a host name.
             */
            host = null;
        }

        if (scheme == null) {
            if (host == null) {
                scheme = "local";            // default local scheme
            } else {
                /*
                 * rmi is the default remote scheme. if the VmIdentifier
                 * specifies some other protocol, this default is overridden.
                 */
                scheme = "rmi";
            }
        }

        sb.append(scheme).append("://");

        if (host == null) {
            sb.append("localhost");          // default host name
        } else {
            sb.append(host);
        }

        int port = vmid.getPort();
        if (port != -1) {
            sb.append(":").append(port);
        }

        String path = vmid.getPath();
        if ((path != null) && (path.length() != 0)) {
            sb.append(path);
        }

        String query = vmid.getQuery();
        if (query != null) {
            sb.append("?").append(query);
        }

        String frag = vmid.getFragment();
        if (frag != null) {
            sb.append("#").append(frag);
        }

        try {
           uri = new URI(sb.toString());
        } catch (URISyntaxException e) {
           // shouldn't happen, as we were passed a valid VmIdentifier
           throw new RuntimeException("Internal Error", e);
        }
    }

    /**
     * Resolve a VmIdentifier with this HostIdentifier. A VmIdentifier, such
     * as <em>1234</em> or <em>1234@hostname</em> or any other string that
     * omits certain components of the URI string may be valid, but is certainly
     * incomplete. They are missing critical information for identifying the
     * the communications protocol, target host, or other parameters. A
     * VmIdentifier of this form is considered <em>unresolved</em>. This method
     * uses components of the HostIdentifier to resolve the missing components
     * of the VmIdentifier.
     * <p>
     * Specified components of the unresolved VmIdentifier take precedence
     * over their HostIdentifier counterparts. For example, if the VmIdentifier
     * indicates <em>1234@hostname:2099</em> and the HostIdentifier indicates
     * <em>rmi://hostname:1099/</em>, then the resolved VmIdentifier will
     * be <em>rmi://1234@hostname:2099</em>. Any component not explicitly
     * specified or assumed by the HostIdentifier, will remain unresolved in
     * resolved VmIdentifier.
     *  <p>
     * A VmIdentifier specifying a <em>file:</em> scheme (protocol), is
     * not changed in any way by this method.
     *
     * @param vmid the unresolved VmIdentifier.
     * @return VmIdentifier - the resolved VmIdentifier. If vmid was resolved
     *                        on entry to this method, then the returned
     *                        VmIdentifier will be equal, but not identical, to
     *                        vmid.
     */
    public VmIdentifier resolve(VmIdentifier vmid)
           throws URISyntaxException, MonitorException {
        String scheme = vmid.getScheme();
        String host = vmid.getHost();
        String authority = vmid.getAuthority();

        if ((scheme != null) && (scheme.compareTo("file") == 0)) {
            // don't attempt to resolve a file based VmIdentifier.
            return vmid;
        }

        if ((host != null) && (host.compareTo(authority) == 0)) {
            /*
             * this condition occurs when the VmIdentifier specifies only
             * the authority (i.e. an lvmid), and not a host name.
             */
            host = null;
        }

        if (scheme == null) {
            scheme = getScheme();
        }

        URI nuri = null;

        StringBuilder sb = new StringBuilder();

        sb.append(scheme).append("://");

        String userInfo = vmid.getUserInfo();
        if (userInfo != null) {
            sb.append(userInfo);
        } else {
            sb.append(vmid.getAuthority());
        }

        if (host == null) {
            host = getHost();
        }
        sb.append("@").append(host);

        int port = vmid.getPort();
        if (port == -1) {
            port = getPort();
        }

        if (port != -1) {
            sb.append(":").append(port);
        }

        String path = vmid.getPath();
        if ((path == null) || (path.length() == 0)) {
            path = getPath();
        }

        if ((path != null) && (path.length() > 0)) {
            sb.append(path);
        }

        String query = vmid.getQuery();
        if (query == null) {
            query = getQuery();
        }
        if (query != null) {
            sb.append("?").append(query);
        }

        String fragment = vmid.getFragment();
        if (fragment == null) {
            fragment = getFragment();
        }
        if (fragment != null) {
            sb.append("#").append(fragment);
        }

        String s = sb.toString();
        return new VmIdentifier(s);
    }

    /**
     * Return the Scheme, or protocol, portion of this HostIdentifier.
     *
     * @return String - the scheme for this HostIdentifier.
     * @see URI#getScheme()
     */
    public String getScheme() {
        return uri.isAbsolute() ? uri.getScheme() : null;
    }

    /**
     * Return the Scheme Specific Part of this HostIdentifier.
     *
     * @return String - the scheme specific part for this HostIdentifier.
     * @see URI#getSchemeSpecificPart()
     */
    public String getSchemeSpecificPart() {
        return  uri.getSchemeSpecificPart();
    }

    /**
     * Return the User Info part of this HostIdentifier.
     *
     * @return String - the user info part for this HostIdentifier.
     * @see URI#getUserInfo()
     */
    public String getUserInfo() {
        return uri.getUserInfo();
    }

    /**
     * Return the Host part of this HostIdentifier.
     *
     * @return String - the host part for this HostIdentifier, or
     *                  "localhost" if the URI.getHost() returns null.
     * @see URI#getUserInfo()
     */
    public String getHost() {
        return (uri.getHost() == null) ? "localhost" : uri.getHost();
    }

    /**
     * Return the Port for of this HostIdentifier.
     *
     * @return String - the port for this HostIdentifier
     * @see URI#getPort()
     */
    public int getPort() {
        return uri.getPort();
    }

    /**
     * Return the Path part of this HostIdentifier.
     *
     * @return String - the path part for this HostIdentifier.
     * @see URI#getPath()
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * Return the Query part of this HostIdentifier.
     *
     * @return String - the query part for this HostIdentifier.
     * @see URI#getQuery()
     */
    public String getQuery() {
        return uri.getQuery();
    }

    /**
     * Return the Fragment part of this HostIdentifier.
     *
     * @return String - the fragment part for this HostIdentifier.
     * @see URI#getFragment()
     */
    public String getFragment() {
        return uri.getFragment();
    }

    /**
     * Return the mode indicated in this HostIdentifier.
     *
     * @return String - the mode string. If no mode is specified, then "r"
     *                  is returned. otherwise, the specified mode is returned.
     */
    public String getMode() {
        String query = getQuery();
        if (query != null) {
            String[] queryArgs = query.split("\\+");
            for (int i = 0; i < queryArgs.length; i++) {
                if (queryArgs[i].startsWith("mode=")) {
                    int index = queryArgs[i].indexOf('=');
                    return queryArgs[i].substring(index+1);
                }
            }
        }
        return "r";
    }

    /**
     * Return the URI associated with the HostIdentifier.
     *
     * @return URI - the URI.
     * @see URI
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Return the hash code for this HostIdentifier. The hash code is
     * identical to the hash code for the contained URI.
     *
     * @return int - the hashcode.
     * @see URI#hashCode()
     */
    public int hashCode() {
        return uri.hashCode();
    }

    /**
     * Test for quality with other objects.
     *
     * @param object the object to be test for equality.
     * @return boolean - returns true if the given object is of type
     *                   HostIdentifier and its URI field is equal to this
     *                   object's URI field. Otherwise, returns false.
     *
     * @see URI#equals(Object)
     */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof HostIdentifier)) {
            return false;
        }
        return uri.equals(((HostIdentifier)object).uri);
    }


    /**
     * Convert to a string representation. Conversion is identical to
     * calling getURI().toString(). This may change in a future release.
     *
     * @return String - a String representation of the HostIdentifier.
     *
     * @see URI#toString()
     */
    public String toString() {
        return uri.toString();
    }
}
