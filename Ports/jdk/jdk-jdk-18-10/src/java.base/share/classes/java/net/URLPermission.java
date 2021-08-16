/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.security.Permission;

/**
 * Represents permission to access a resource or set of resources defined by a
 * given url, and for a given set of user-settable request methods
 * and request headers. The <i>name</i> of the permission is the url string.
 * The <i>actions</i> string is a concatenation of the request methods and headers.
 * The range of method and header names is not restricted by this class.
 * <p><b>The url</b><p>
 * The url string has the following expected structure.
 * <pre>
 *     scheme : // authority [ / path ] [ ignored-query-or-fragment ]
 * </pre>
 * <i>scheme</i> will typically be http or https, but is not restricted by this
 * class.
 * <i>authority</i> is specified as:
 * <pre>
 *     authority = [ userinfo @ ] hostrange [ : portrange ]
 *     portrange = portnumber | -portnumber | portnumber-[portnumber] | *
 *     hostrange = ([*.] dnsname) | IPv4address | IPv6address
 * </pre>
 * <i>dnsname</i> is a standard DNS host or domain name, i.e. one or more labels
 * separated by ".". <i>IPv4address</i> is a standard literal IPv4 address and
 * <i>IPv6address</i> is as defined in <a href="http://www.ietf.org/rfc/rfc2732.txt">
 * RFC 2732</a>. Literal IPv6 addresses must however, be enclosed in '[]' characters.
 * The <i>dnsname</i> specification can be preceded by "*." which means
 * the name will match any hostname whose right-most domain labels are the same as
 * this name. For example, "*.example.com" matches "foo.bar.example.com"
 * <p>
 * <i>portrange</i> is used to specify a port number, or a bounded or unbounded range of ports
 * that this permission applies to. If portrange is absent or invalid, then a default
 * port number is assumed if the scheme is {@code http} (default 80) or {@code https}
 * (default 443). No default is assumed for other schemes. A wildcard may be specified
 * which means all ports.
 * <p>
 * <i>userinfo</i> is optional. A userinfo component if present, is ignored when
 * creating a URLPermission, and has no effect on any other methods defined by this class.
 * <p>
 * The <i>path</i> component comprises a sequence of path segments,
 * separated by '/' characters. <i>path</i> may also be empty. The path is specified
 * in a similar way to the path in {@link java.io.FilePermission}. There are
 * three different ways as the following examples show:
 * <table class="striped">
 * <caption>URL Examples</caption>
 * <thead>
 * <tr><th scope="col">Example url</th><th scope="col">Description</th></tr>
 * </thead>
 * <tbody style="text-align:left">
 * <tr><th scope="row" style="white-space:nowrap;">http://www.example.com/a/b/c.html</th>
 *   <td>A url which identifies a specific (single) resource</td>
 * </tr>
 * <tr><th scope="row">http://www.example.com/a/b/*</th>
 *   <td>The '*' character refers to all resources in the same "directory" - in
 *       other words all resources with the same number of path components, and
 *       which only differ in the final path component, represented by the '*'.
 *   </td>
 * </tr>
 * <tr><th scope="row">http://www.example.com/a/b/-</th>
 *   <td>The '-' character refers to all resources recursively below the
 *       preceding path (e.g. http://www.example.com/a/b/c/d/e.html matches this
 *       example).
 *   </td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * The '*' and '-' may only be specified in the final segment of a path and must be
 * the only character in that segment. Any query or fragment components of the
 * url are ignored when constructing URLPermissions.
 * <p>
 * As a special case, urls of the form, "scheme:*" are accepted to
 * mean any url of the given scheme.
 * <p>
 * The <i>scheme</i> and <i>authority</i> components of the url string are handled
 * without regard to case. This means {@link #equals(Object)},
 * {@link #hashCode()} and {@link #implies(Permission)} are case insensitive with respect
 * to these components. If the <i>authority</i> contains a literal IP address,
 * then the address is normalized for comparison. The path component is case sensitive.
 * <p>
 * <i>ignored-query-or-fragment</i> refers to any query or fragment which appears after the
 * path component, and which is ignored by the constructors of this class. It is defined as:
 * <pre>
 *     ignored-query-or-fragment = [ ? query ] [ # fragment ]
 * </pre>
 * where <i>query</i> and <i>fragment</i> are as defined in
 * <a href="http://www.ietf.org/rfc/rfc2296.txt">RFC2396</a>. {@link #getName() getName()} therefore returns
 * only the <i>scheme</i>, <i>authority</i> and <i>path</i> components of the url string that
 * the permission was created with.
 * <p><b>The actions string</b><p>
 * The actions string of a URLPermission is a concatenation of the <i>method list</i>
 * and the <i>request headers list</i>. These are lists of the permitted request
 * methods and permitted request headers of the permission (respectively). The two lists
 * are separated by a colon ':' character and elements of each list are comma separated.
 * Some examples are:
 * <ul>
 * <li>"POST,GET,DELETE"
 * <li>"GET:X-Foo-Request,X-Bar-Request"
 * <li>"POST,GET:Header1,Header2"
 * </ul>
 * <p>
 * The first example specifies the methods: POST, GET and DELETE, but no request headers.
 * The second example specifies one request method and two headers. The third
 * example specifies two request methods, and two headers.
 * <p>
 * The colon separator need not be present if the request headers list is empty.
 * No white-space is permitted in the actions string. The action strings supplied to
 * the URLPermission constructors are case-insensitive and are normalized by converting
 * method names to upper-case and header names to the form defines in RFC2616 (lower case
 * with initial letter of each word capitalized). Either list can contain a wild-card '*'
 * character which signifies all request methods or headers respectively.
 * <p>
 * Note. Depending on the context of use, some request methods and headers may be permitted
 * at all times, and others may not be permitted at any time. For example, the
 * HTTP protocol handler might disallow certain headers such as Content-Length
 * from being set by application code, regardless of whether the security policy
 * in force, permits it.
 *
 * @since 1.8
 */
public final class URLPermission extends Permission {

    @java.io.Serial
    private static final long serialVersionUID = -2702463814894478682L;

    private transient String scheme;
    private transient String ssp;                 // scheme specific part
    private transient String path;
    private transient List<String> methods;
    private transient List<String> requestHeaders;
    private transient Authority authority;

    // serialized field
    /**
     * The actions string
     */
    private String actions;

    /**
     * Creates a new URLPermission from a url string and which permits the given
     * request methods and user-settable request headers.
     * The name of the permission is the url string it was created with. Only the scheme,
     * authority and path components of the url are used internally. Any fragment or query
     * components are ignored. The permissions action string is as specified above.
     *
     * @param url the url string
     *
     * @param actions the actions string
     *
     * @throws    IllegalArgumentException if url is invalid or if actions contains white-space.
     */
    public URLPermission(String url, String actions) {
        super(normalize(url));
        init(actions);
    }

    /**
     * Remove any query or fragment from url string
     */
    private static String normalize(String url) {
        int index = url.indexOf('?');
        if (index >= 0) {
            url = url.substring(0, index);
        } else {
            index = url.indexOf('#');
            if (index >= 0) {
                url = url.substring(0, index);
            }
        }
        return url;
    }

    private void init(String actions) {
        parseURI(getName());
        int colon = actions.indexOf(':');
        if (actions.lastIndexOf(':') != colon) {
            throw new IllegalArgumentException(
                "Invalid actions string: \"" + actions + "\"");
        }

        String methods, headers;
        if (colon == -1) {
            methods = actions;
            headers = "";
        } else {
            methods = actions.substring(0, colon);
            headers = actions.substring(colon+1);
        }

        List<String> l = normalizeMethods(methods);
        Collections.sort(l);
        this.methods = Collections.unmodifiableList(l);

        l = normalizeHeaders(headers);
        Collections.sort(l);
        this.requestHeaders = Collections.unmodifiableList(l);

        this.actions = actions();
    }

    /**
     * Creates a URLPermission with the given url string and unrestricted
     * methods and request headers by invoking the two argument
     * constructor as follows: URLPermission(url, "*:*")
     *
     * @param url the url string
     *
     * @throws    IllegalArgumentException if url does not result in a valid {@link URI}
     */
    public URLPermission(String url) {
        this(url, "*:*");
    }

    /**
     * Returns the normalized method list and request
     * header list, in the form:
     * <pre>
     *      "method-names : header-names"
     * </pre>
     * <p>
     * where method-names is the list of methods separated by commas
     * and header-names is the list of permitted headers separated by commas.
     * There is no white space in the returned String. If header-names is empty
     * then the colon separator may not be present.
     */
    public String getActions() {
        return actions;
    }

    /**
     * Checks if this URLPermission implies the given permission.
     * Specifically, the following checks are done as if in the
     * following sequence:
     * <ul>
     * <li>if 'p' is not an instance of URLPermission return false</li>
     * <li>if any of p's methods are not in this's method list, and if
     *     this's method list is not equal to "*", then return false.</li>
     * <li>if any of p's headers are not in this's request header list, and if
     *     this's request header list is not equal to "*", then return false.</li>
     * <li>if this's url scheme is not equal to p's url scheme return false</li>
     * <li>if the scheme specific part of this's url is '*' return true</li>
     * <li>if the set of hosts defined by p's url hostrange is not a subset of
     *     this's url hostrange then return false. For example, "*.foo.example.com"
     *     is a subset of "*.example.com". "foo.bar.example.com" is not
     *     a subset of "*.foo.example.com"</li>
     * <li>if the portrange defined by p's url is not a subset of the
     *     portrange defined by this's url then return false.
     * <li>if the path or paths specified by p's url are contained in the
     *     set of paths specified by this's url, then return true
     * <li>otherwise, return false</li>
     * </ul>
     * <p>Some examples of how paths are matched are shown below:
     * <table class="plain">
     * <caption>Examples of Path Matching</caption>
     * <thead>
     * <tr><th scope="col">this's path</th><th scope="col">p's path</th><th>match</th></tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr><th scope="row">/a/b</th><th scope="row">/a/b</th><td>yes</td></tr>
     * <tr><th scope="row" rowspan="3">/a/b/*</th><th scope="row">/a/b/c</th><td>yes</td></tr>
     * <tr>  <th scope="row">/a/b/c/d</th><td>no</td></tr>
     * <tr>  <th scope="row">/a/b/c/-</th><td>no</td></tr>
     * <tr><th scope="row" rowspan="3">/a/b/-</th><th scope="row">/a/b/c/d</th><td>yes</td></tr>
     * <tr>  <th scope="row">/a/b/c/d/e</th><td>yes</td></tr>
     * <tr>  <th scope="row">/a/b/c/*</th><td>yes</td></tr>
     * </tbody>
     * </table>
     */
    public boolean implies(Permission p) {
        if (! (p instanceof URLPermission that)) {
            return false;
        }

        if (this.methods.isEmpty() && !that.methods.isEmpty()) {
            return false;
        }

        if (!this.methods.isEmpty() &&
            !this.methods.get(0).equals("*") &&
            Collections.indexOfSubList(this.methods,
                                       that.methods) == -1) {
            return false;
        }

        if (this.requestHeaders.isEmpty() && !that.requestHeaders.isEmpty()) {
            return false;
        }

        if (!this.requestHeaders.isEmpty() &&
            !this.requestHeaders.get(0).equals("*") &&
             Collections.indexOfSubList(this.requestHeaders,
                                        that.requestHeaders) == -1) {
            return false;
        }

        if (!this.scheme.equals(that.scheme)) {
            return false;
        }

        if (this.ssp.equals("*")) {
            return true;
        }

        if (!this.authority.implies(that.authority)) {
            return false;
        }

        if (this.path == null) {
            return that.path == null;
        }
        if (that.path == null) {
            return false;
        }

        if (this.path.endsWith("/-")) {
            String thisprefix = this.path.substring(0, this.path.length() - 1);
            return that.path.startsWith(thisprefix);
            }

        if (this.path.endsWith("/*")) {
            String thisprefix = this.path.substring(0, this.path.length() - 1);
            if (!that.path.startsWith(thisprefix)) {
                return false;
            }
            String thatsuffix = that.path.substring(thisprefix.length());
            // suffix must not contain '/' chars
            if (thatsuffix.indexOf('/') != -1) {
                return false;
            }
            if (thatsuffix.equals("-")) {
                return false;
            }
            return true;
        }
        return this.path.equals(that.path);
    }


    /**
     * Returns true if, this.getActions().equals(p.getActions())
     * and p's url equals this's url.  Returns false otherwise.
     */
    public boolean equals(Object p) {
        if (!(p instanceof URLPermission that)) {
            return false;
        }
        if (!this.scheme.equals(that.scheme)) {
            return false;
        }
        if (!this.getActions().equals(that.getActions())) {
            return false;
        }
        if (!this.authority.equals(that.authority)) {
            return false;
        }
        if (this.path != null) {
            return this.path.equals(that.path);
        } else {
            return that.path == null;
        }
    }

    /**
     * Returns a hashcode calculated from the hashcode of the
     * actions String and the url string.
     */
    public int hashCode() {
        return getActions().hashCode()
            + scheme.hashCode()
            + authority.hashCode()
            + (path == null ? 0 : path.hashCode());
    }


    private List<String> normalizeMethods(String methods) {
        List<String> l = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        for (int i=0; i<methods.length(); i++) {
            char c = methods.charAt(i);
            if (c == ',') {
                String s = b.toString();
                if (!s.isEmpty())
                    l.add(s);
                b = new StringBuilder();
            } else if (c == ' ' || c == '\t') {
                throw new IllegalArgumentException(
                    "White space not allowed in methods: \"" + methods + "\"");
            } else {
                if (c >= 'a' && c <= 'z') {
                    c += 'A' - 'a';
                }
                b.append(c);
            }
        }
        String s = b.toString();
        if (!s.isEmpty())
            l.add(s);
        return l;
    }

    private List<String> normalizeHeaders(String headers) {
        List<String> l = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i=0; i<headers.length(); i++) {
            char c = headers.charAt(i);
            if (c >= 'a' && c <= 'z') {
                if (capitalizeNext) {
                    c += 'A' - 'a';
                    capitalizeNext = false;
                }
                b.append(c);
            } else if (c == ' ' || c == '\t') {
                throw new IllegalArgumentException(
                    "White space not allowed in headers: \"" + headers + "\"");
            } else if (c == '-') {
                    capitalizeNext = true;
                b.append(c);
            } else if (c == ',') {
                String s = b.toString();
                if (!s.isEmpty())
                    l.add(s);
                b = new StringBuilder();
                capitalizeNext = true;
            } else {
                capitalizeNext = false;
                b.append(c);
            }
        }
        String s = b.toString();
        if (!s.isEmpty())
            l.add(s);
        return l;
    }

    private void parseURI(String url) {
        int len = url.length();
        int delim = url.indexOf(':');
        if (delim == -1 || delim + 1 == len) {
            throw new IllegalArgumentException(
                "Invalid URL string: \"" + url + "\"");
        }
        scheme = url.substring(0, delim).toLowerCase();
        this.ssp = url.substring(delim + 1);

        if (!ssp.startsWith("//")) {
            if (!ssp.equals("*")) {
                throw new IllegalArgumentException(
                    "Invalid URL string: \"" + url + "\"");
            }
            this.authority = new Authority(scheme, "*");
            return;
        }
        String authpath = ssp.substring(2);

        delim = authpath.indexOf('/');
        String auth;
        if (delim == -1) {
            this.path = "";
            auth = authpath;
        } else {
            auth = authpath.substring(0, delim);
            this.path = authpath.substring(delim);
        }
        this.authority = new Authority(scheme, auth.toLowerCase());
    }

    private String actions() {
        // The colon separator is optional when the request headers list is
        // empty.This implementation chooses to include it even when the request
        // headers list is empty.
        return String.join(",", methods) + ":" + String.join(",", requestHeaders);
    }

    /**
     * Restores the state of this object from stream.
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        String actions = (String)fields.get("actions", null);

        init(actions);
    }

    static class Authority {
        HostPortrange p;

        Authority(String scheme, String authority) {
            int at = authority.indexOf('@');
            if (at == -1) {
                    p = new HostPortrange(scheme, authority);
            } else {
                    p = new HostPortrange(scheme, authority.substring(at+1));
            }
        }

        boolean implies(Authority other) {
            return impliesHostrange(other) && impliesPortrange(other);
        }

        private boolean impliesHostrange(Authority that) {
            String thishost = this.p.hostname();
            String thathost = that.p.hostname();

            if (p.wildcard() && thishost.isEmpty()) {
                // this "*" implies all others
                return true;
            }
            if (that.p.wildcard() && thathost.isEmpty()) {
                // that "*" can only be implied by this "*"
                return false;
            }
            if (thishost.equals(thathost)) {
                // covers all cases of literal IP addresses and fixed
                // domain names.
                return true;
            }
            if (this.p.wildcard()) {
                // this "*.foo.com" implies "bub.bar.foo.com"
                return thathost.endsWith(thishost);
            }
            return false;
        }

        private boolean impliesPortrange(Authority that) {
            int[] thisrange = this.p.portrange();
            int[] thatrange = that.p.portrange();
            if (thisrange[0] == -1) {
                /* port not specified non http/s URL */
                return true;
            }
            return thisrange[0] <= thatrange[0] &&
                        thisrange[1] >= thatrange[1];
        }

        boolean equals(Authority that) {
            return this.p.equals(that.p);
        }

        public int hashCode() {
            return p.hashCode();
        }
    }
}
