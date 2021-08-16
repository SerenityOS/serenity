/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.io.IOException;
import sun.util.logging.PlatformLogger;

/**
 * CookieManager provides a concrete implementation of {@link CookieHandler},
 * which separates the storage of cookies from the policy surrounding accepting
 * and rejecting cookies. A CookieManager is initialized with a {@link CookieStore}
 * which manages storage, and a {@link CookiePolicy} object, which makes
 * policy decisions on cookie acceptance/rejection.
 *
 * <p> The HTTP cookie management in java.net package looks like:
 * <blockquote>
 * <pre>{@code
 *                  use
 * CookieHandler <------- HttpURLConnection
 *       ^
 *       | impl
 *       |         use
 * CookieManager -------> CookiePolicy
 *             |   use
 *             |--------> HttpCookie
 *             |              ^
 *             |              | use
 *             |   use        |
 *             |--------> CookieStore
 *                            ^
 *                            | impl
 *                            |
 *                  Internal in-memory implementation
 * }</pre>
 * <ul>
 *   <li>
 *     CookieHandler is at the core of cookie management. User can call
 *     CookieHandler.setDefault to set a concrete CookieHandler implementation
 *     to be used.
 *   </li>
 *   <li>
 *     CookiePolicy.shouldAccept will be called by CookieManager.put to see whether
 *     or not one cookie should be accepted and put into cookie store. User can use
 *     any of three pre-defined CookiePolicy, namely ACCEPT_ALL, ACCEPT_NONE and
 *     ACCEPT_ORIGINAL_SERVER, or user can define his own CookiePolicy implementation
 *     and tell CookieManager to use it.
 *   </li>
 *   <li>
 *     CookieStore is the place where any accepted HTTP cookie is stored in.
 *     If not specified when created, a CookieManager instance will use an internal
 *     in-memory implementation. Or user can implements one and tell CookieManager
 *     to use it.
 *   </li>
 *   <li>
 *     Currently, only CookieStore.add(URI, HttpCookie) and CookieStore.get(URI)
 *     are used by CookieManager. Others are for completeness and might be needed
 *     by a more sophisticated CookieStore implementation, e.g. a NetscapeCookieStore.
 *   </li>
 * </ul>
 * </blockquote>
 *
 * <p>There're various ways user can hook up his own HTTP cookie management behavior, e.g.
 * <blockquote>
 * <ul>
 *   <li>Use CookieHandler.setDefault to set a brand new {@link CookieHandler} implementation
 *   <li>Let CookieManager be the default {@link CookieHandler} implementation,
 *       but implement user's own {@link CookieStore} and {@link CookiePolicy}
 *       and tell default CookieManager to use them:
 *     <blockquote><pre>
 *       // this should be done at the beginning of an HTTP session
 *       CookieHandler.setDefault(new CookieManager(new MyCookieStore(), new MyCookiePolicy()));
 *     </pre></blockquote>
 *   <li>Let CookieManager be the default {@link CookieHandler} implementation, but
 *       use customized {@link CookiePolicy}:
 *     <blockquote><pre>
 *       // this should be done at the beginning of an HTTP session
 *       CookieHandler.setDefault(new CookieManager());
 *       // this can be done at any point of an HTTP session
 *       ((CookieManager)CookieHandler.getDefault()).setCookiePolicy(new MyCookiePolicy());
 *     </pre></blockquote>
 * </ul>
 * </blockquote>
 *
 * <p>The implementation conforms to <a href="http://www.ietf.org/rfc/rfc2965.txt">RFC 2965</a>, section 3.3.
 *
 * @see CookiePolicy
 * @author Edward Wang
 * @since 1.6
 */
public class CookieManager extends CookieHandler
{
    /* ---------------- Fields -------------- */

    private CookiePolicy policyCallback;


    private CookieStore cookieJar = null;


    /* ---------------- Ctors -------------- */

    /**
     * Create a new cookie manager.
     *
     * <p>This constructor will create new cookie manager with default
     * cookie store and accept policy. The effect is same as
     * {@code CookieManager(null, null)}.
     */
    public CookieManager() {
        this(null, null);
    }


    /**
     * Create a new cookie manager with specified cookie store and cookie policy.
     *
     * @param store     a {@code CookieStore} to be used by cookie manager.
     *                  if {@code null}, cookie manager will use a default one,
     *                  which is an in-memory CookieStore implementation.
     * @param cookiePolicy      a {@code CookiePolicy} instance
     *                          to be used by cookie manager as policy callback.
     *                          if {@code null}, ACCEPT_ORIGINAL_SERVER will
     *                          be used.
     */
    public CookieManager(CookieStore store,
                         CookiePolicy cookiePolicy)
    {
        // use default cookie policy if not specify one
        policyCallback = (cookiePolicy == null) ? CookiePolicy.ACCEPT_ORIGINAL_SERVER
                                                : cookiePolicy;

        // if not specify CookieStore to use, use default one
        if (store == null) {
            cookieJar = new InMemoryCookieStore();
        } else {
            cookieJar = store;
        }
    }


    /* ---------------- Public operations -------------- */

    /**
     * To set the cookie policy of this cookie manager.
     *
     * <p> A instance of {@code CookieManager} will have
     * cookie policy ACCEPT_ORIGINAL_SERVER by default. Users always
     * can call this method to set another cookie policy.
     *
     * @param cookiePolicy      the cookie policy. Can be {@code null}, which
     *                          has no effects on current cookie policy.
     */
    public void setCookiePolicy(CookiePolicy cookiePolicy) {
        if (cookiePolicy != null) policyCallback = cookiePolicy;
    }


    /**
     * To retrieve current cookie store.
     *
     * @return  the cookie store currently used by cookie manager.
     */
    public CookieStore getCookieStore() {
        return cookieJar;
    }


    public Map<String, List<String>>
        get(URI uri, Map<String, List<String>> requestHeaders)
        throws IOException
    {
        // pre-condition check
        if (uri == null || requestHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }

        // if there's no default CookieStore, no way for us to get any cookie
        if (cookieJar == null)
            return Map.of();

        boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
        List<HttpCookie> cookies = new java.util.ArrayList<>();
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        for (HttpCookie cookie : cookieJar.get(uri)) {
            // apply path-matches rule (RFC 2965 sec. 3.3.4)
            // and check for the possible "secure" tag (i.e. don't send
            // 'secure' cookies over unsecure links)
            if (pathMatches(path, cookie.getPath()) &&
                    (secureLink || !cookie.getSecure())) {
                // Enforce httponly attribute
                if (cookie.isHttpOnly()) {
                    String s = uri.getScheme();
                    if (!"http".equalsIgnoreCase(s) && !"https".equalsIgnoreCase(s)) {
                        continue;
                    }
                }
                // Let's check the authorize port list if it exists
                String ports = cookie.getPortlist();
                if (ports != null && !ports.isEmpty()) {
                    int port = uri.getPort();
                    if (port == -1) {
                        port = "https".equals(uri.getScheme()) ? 443 : 80;
                    }
                    if (isInPortList(ports, port)) {
                        cookies.add(cookie);
                    }
                } else {
                    cookies.add(cookie);
                }
            }
        }

        // apply sort rule (RFC 2965 sec. 3.3.4)
        List<String> cookieHeader = sortByPathAndAge(cookies);

        return Map.of("Cookie", cookieHeader);
    }

    public void
        put(URI uri, Map<String, List<String>> responseHeaders)
        throws IOException
    {
        // pre-condition check
        if (uri == null || responseHeaders == null) {
            throw new IllegalArgumentException("Argument is null");
        }


        // if there's no default CookieStore, no need to remember any cookie
        if (cookieJar == null)
            return;

    PlatformLogger logger = PlatformLogger.getLogger("java.net.CookieManager");
        for (String headerKey : responseHeaders.keySet()) {
            // RFC 2965 3.2.2, key must be 'Set-Cookie2'
            // we also accept 'Set-Cookie' here for backward compatibility
            if (headerKey == null
                || !(headerKey.equalsIgnoreCase("Set-Cookie2")
                     || headerKey.equalsIgnoreCase("Set-Cookie")
                    )
                )
            {
                continue;
            }

            for (String headerValue : responseHeaders.get(headerKey)) {
                try {
                    List<HttpCookie> cookies;
                    try {
                        cookies = HttpCookie.parse(headerValue);
                    } catch (IllegalArgumentException e) {
                        // Bogus header, make an empty list and log the error
                        cookies = java.util.Collections.emptyList();
                        if (logger.isLoggable(PlatformLogger.Level.SEVERE)) {
                            logger.severe("Invalid cookie for " + uri + ": " + headerValue);
                        }
                    }
                    for (HttpCookie cookie : cookies) {
                        if (cookie.getPath() == null) {
                            // If no path is specified, then by default
                            // the path is the directory of the page/doc
                            String path = uri.getPath();
                            if (!path.endsWith("/")) {
                                int i = path.lastIndexOf('/');
                                if (i > 0) {
                                    path = path.substring(0, i + 1);
                                } else {
                                    path = "/";
                                }
                            }
                            cookie.setPath(path);
                        }

                        // As per RFC 2965, section 3.3.1:
                        // Domain  Defaults to the effective request-host.  (Note that because
                        // there is no dot at the beginning of effective request-host,
                        // the default Domain can only domain-match itself.)
                        if (cookie.getDomain() == null) {
                            String host = uri.getHost();
                            if (host != null && !host.contains("."))
                                host += ".local";
                            cookie.setDomain(host);
                        }
                        String ports = cookie.getPortlist();
                        if (ports != null) {
                            int port = uri.getPort();
                            if (port == -1) {
                                port = "https".equals(uri.getScheme()) ? 443 : 80;
                            }
                            if (ports.isEmpty()) {
                                // Empty port list means this should be restricted
                                // to the incoming URI port
                                cookie.setPortlist("" + port );
                                if (shouldAcceptInternal(uri, cookie)) {
                                    cookieJar.add(uri, cookie);
                                }
                            } else {
                                // Only store cookies with a port list
                                // IF the URI port is in that list, as per
                                // RFC 2965 section 3.3.2
                                if (isInPortList(ports, port) &&
                                        shouldAcceptInternal(uri, cookie)) {
                                    cookieJar.add(uri, cookie);
                                }
                            }
                        } else {
                            if (shouldAcceptInternal(uri, cookie)) {
                                cookieJar.add(uri, cookie);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // invalid set-cookie header string
                    // no-op
                }
            }
        }
    }


    /* ---------------- Private operations -------------- */

    // to determine whether or not accept this cookie
    private boolean shouldAcceptInternal(URI uri, HttpCookie cookie) {
        try {
            return policyCallback.shouldAccept(uri, cookie);
        } catch (Exception ignored) { // protect against malicious callback
            return false;
        }
    }


    private static boolean isInPortList(String lst, int port) {
        int i = lst.indexOf(',');
        int val = -1;
        while (i > 0) {
            try {
                val = Integer.parseInt(lst, 0, i, 10);
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException numberFormatException) {
            }
            lst = lst.substring(i+1);
            i = lst.indexOf(',');
        }
        if (!lst.isEmpty()) {
            try {
                val = Integer.parseInt(lst);
                if (val == port) {
                    return true;
                }
            } catch (NumberFormatException numberFormatException) {
            }
        }
        return false;
    }

    /*
     * path-matches algorithm, as defined by RFC 2965
     */
    private boolean pathMatches(String path, String pathToMatchWith) {
        if (path == pathToMatchWith)
            return true;
        if (path == null || pathToMatchWith == null)
            return false;
        if (path.startsWith(pathToMatchWith))
            return true;

        return false;
    }


    /*
     * sort cookies with respect to their path and age: those with more longer Path attributes
     * precede those with shorter, as defined in RFC 6265. Cookies with the same length
     * path are distinguished by creation time (older first). Method made PP to enable testing.
     */
    static List<String> sortByPathAndAge(List<HttpCookie> cookies) {
        Collections.sort(cookies, new CookieComparator());

        List<String> cookieHeader = new java.util.ArrayList<>();
        for (HttpCookie cookie : cookies) {
            // Netscape cookie spec and RFC 2965 have different format of Cookie
            // header; RFC 2965 requires a leading $Version="1" string while Netscape
            // does not.
            // The workaround here is to add a $Version="1" string in advance
            if (cookies.indexOf(cookie) == 0 && cookie.getVersion() > 0) {
                cookieHeader.add("$Version=\"1\"");
            }

            cookieHeader.add(cookie.toString());
        }
        return cookieHeader;
    }


    // Comparator compares the length of the path. Longer paths should precede shorter ones.
    // As per rfc6265 cookies with equal path lengths sort on creation time.

    static class CookieComparator implements Comparator<HttpCookie> {
        public int compare(HttpCookie c1, HttpCookie c2) {
            if (c1 == c2) return 0;
            if (c1 == null) return -1;
            if (c2 == null) return 1;

            String p1 = c1.getPath();
            String p2 = c2.getPath();
            p1 = (p1 == null) ? "" : p1;
            p2 = (p2 == null) ? "" : p2;
            int len1 = p1.length();
            int len2 = p2.length();
            if (len1 > len2)
                return -1;
            if (len2 > len1)
                return 1;

            // Check creation time. Sort older first
            long creation1 = c1.getCreationTime();
            long creation2 = c2.getCreationTime();
            if (creation1 < creation2) {
                return -1;
            }
            if (creation1 > creation2) {
                return 1;
            }
            return 0;
        }
    }
}
