/*
 * Copyright (c) 1999, 2002, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import java.net.MalformedURLException;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;
import com.sun.jndi.toolkit.url.Uri;
import com.sun.jndi.toolkit.url.UrlUtil;

/*
 * Extract components of an LDAP URL.
 *
 * The format of an LDAP URL is defined in RFC 2255 as follows:
 *
 *     ldapurl    = scheme "://" [hostport] ["/"
 *                  [dn ["?" [attributes] ["?" [scope]
 *                  ["?" [filter] ["?" extensions]]]]]]
 *     scheme     = "ldap"
 *     attributes = attrdesc *("," attrdesc)
 *     scope      = "base" / "one" / "sub"
 *     dn         = distinguishedName from Section 3 of [1]
 *     hostport   = hostport from Section 5 of RFC 1738 [5]
 *     attrdesc   = AttributeDescription from Section 4.1.5 of [2]
 *     filter     = filter from Section 4 of [4]
 *     extensions = extension *("," extension)
 *     extension  = ["!"] extype ["=" exvalue]
 *     extype     = token / xtoken
 *     exvalue    = LDAPString from section 4.1.2 of [2]
 *     token      = oid from section 4.1 of [3]
 *     xtoken     = ("X-" / "x-") token
 *
 * For example,
 *
 *     ldap://ldap.itd.umich.edu/o=University%20of%20Michigan,c=US
 *     ldap://host.com:6666/o=IMC,c=US??sub?(cn=Babs%20Jensen)
 *
 * This class also supports ldaps URLs.
 */

public final class LdapURL extends Uri {

    private boolean useSsl = false;
    private String DN = null;
    private String attributes = null;
    private String scope = null;
    private String filter = null;
    private String extensions = null;

    /**
     * Creates an LdapURL object from an LDAP URL string.
     */
    public LdapURL(String url) throws NamingException {

        super();

        try {
            init(url); // scheme, host, port, path, query
            useSsl = scheme.equalsIgnoreCase("ldaps");

            if (! (scheme.equalsIgnoreCase("ldap") || useSsl)) {
                throw new MalformedURLException("Not an LDAP URL: " + url);
            }

            parsePathAndQuery(); // DN, attributes, scope, filter, extensions

        } catch (MalformedURLException e) {
            NamingException ne = new NamingException("Cannot parse url: " + url);
            ne.setRootCause(e);
            throw ne;
        } catch (UnsupportedEncodingException e) {
            NamingException ne = new NamingException("Cannot parse url: " + url);
            ne.setRootCause(e);
            throw ne;
        }
    }

    /**
     * Returns true if the URL is an LDAPS URL.
     */
    public boolean useSsl() {
        return useSsl;
    }

    /**
     * Returns the LDAP URL's distinguished name.
     */
    public String getDN() {
        return DN;
    }

    /**
     * Returns the LDAP URL's attributes.
     */
    public String getAttributes() {
        return attributes;
    }

    /**
     * Returns the LDAP URL's scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns the LDAP URL's filter.
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Returns the LDAP URL's extensions.
     */
    public String getExtensions() {
        return extensions;
    }

    /**
     * Given a space-separated list of LDAP URLs, returns an array of strings.
     */
    public static String[] fromList(String urlList) throws NamingException {

        String[] urls = new String[(urlList.length() + 1) / 2];
        int i = 0;              // next available index in urls
        StringTokenizer st = new StringTokenizer(urlList, " ");

        while (st.hasMoreTokens()) {
            urls[i++] = st.nextToken();
        }
        String[] trimmed = new String[i];
        System.arraycopy(urls, 0, trimmed, 0, i);
        return trimmed;
    }

    /**
     * Determines whether an LDAP URL has query components.
     */
    public static boolean hasQueryComponents(String url) {
        return (url.lastIndexOf('?') != -1);
    }

    /*
     * Assembles an LDAP or LDAPS URL string from its components.
     * If "host" is an IPv6 literal, it may optionally include delimiting
     * brackets.
     */
    static String toUrlString(String host, int port, String dn, boolean useSsl)
        {

        try {
            String h = (host != null) ? host : "";
            if ((h.indexOf(':') != -1) && (h.charAt(0) != '[')) {
                h = "[" + h + "]";          // IPv6 literal
            }
            String p = (port != -1) ? (":" + port) : "";
            String d = (dn != null) ? ("/" + UrlUtil.encode(dn, "UTF8")) : "";

            return useSsl ? "ldaps://" + h + p + d : "ldap://" + h + p + d;
        } catch (UnsupportedEncodingException e) {
            // UTF8 should always be supported
            throw new IllegalStateException("UTF-8 encoding unavailable");
        }
    }

    /*
     * Parses the path and query components of an URL and sets this
     * object's fields accordingly.
     */
    private void parsePathAndQuery() throws MalformedURLException,
        UnsupportedEncodingException {

        // path begins with a '/' or is empty

        if (path.isEmpty()) {
            return;
        }

        DN = path.startsWith("/") ? path.substring(1) : path;
        if (DN.length() > 0) {
            DN = UrlUtil.decode(DN, "UTF8");
        }

        // query begins with a '?' or is null

        if (query == null || query.length() < 2) {
            return;
        }

        int currentIndex = 1;
        int nextQmark;
        int endIndex;

        // attributes:
        nextQmark = query.indexOf('?', currentIndex);
        endIndex = nextQmark == -1 ? query.length() : nextQmark;
        if (endIndex - currentIndex > 0) {
            attributes = query.substring(currentIndex, endIndex);
        }
        currentIndex = endIndex + 1;
        if (currentIndex >= query.length()) {
            return;
        }

        // scope:
        nextQmark = query.indexOf('?', currentIndex);
        endIndex = nextQmark == -1 ? query.length() : nextQmark;
        if (endIndex - currentIndex > 0) {
            scope = query.substring(currentIndex, endIndex);
        }
        currentIndex = endIndex + 1;
        if (currentIndex >= query.length()) {
            return;
        }

        // filter:
        nextQmark = query.indexOf('?', currentIndex);
        endIndex = nextQmark == -1 ? query.length() : nextQmark;
        if (endIndex - currentIndex > 0) {
            filter = query.substring(currentIndex, endIndex);
            filter = UrlUtil.decode(filter, "UTF8");
        }
        currentIndex = endIndex + 1;
        if (currentIndex >= query.length()) {
            return;
        }

        // extensions:
        if (query.length() - currentIndex > 0) {
            extensions = query.substring(currentIndex);
            extensions = UrlUtil.decode(extensions, "UTF8");
        }
    }

/*
    public static void main(String[] args) throws Exception {

        LdapURL url = new LdapURL(args[0]);

        System.out.println("Example LDAP URL: " + url.toString());
        System.out.println("  scheme: " + url.getScheme());
        System.out.println("    host: " + url.getHost());
        System.out.println("    port: " + url.getPort());
        System.out.println("      DN: " + url.getDN());
        System.out.println("   attrs: " + url.getAttributes());
        System.out.println("   scope: " + url.getScope());
        System.out.println("  filter: " + url.getFilter());
        System.out.println("  extens: " + url.getExtensions());
        System.out.println("");
    }
*/
}
