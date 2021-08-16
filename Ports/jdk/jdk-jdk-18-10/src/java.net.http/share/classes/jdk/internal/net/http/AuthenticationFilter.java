/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;
import static java.net.Authenticator.RequestorType.PROXY;
import static java.net.Authenticator.RequestorType.SERVER;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation of Http Basic authentication.
 */
class AuthenticationFilter implements HeaderFilter {
    volatile MultiExchange<?> exchange;
    private static final Base64.Encoder encoder = Base64.getEncoder();

    static final int DEFAULT_RETRY_LIMIT = 3;

    static final int retry_limit = Utils.getIntegerNetProperty(
            "jdk.httpclient.auth.retrylimit", DEFAULT_RETRY_LIMIT);

    static final int UNAUTHORIZED = 401;
    static final int PROXY_UNAUTHORIZED = 407;

    private static final String BASIC_DUMMY =
            "Basic " + Base64.getEncoder()
                    .encodeToString("o:o".getBytes(ISO_8859_1));

    // A public no-arg constructor is required by FilterFactory
    public AuthenticationFilter() {}

    private PasswordAuthentication getCredentials(String header,
                                                  boolean proxy,
                                                  HttpRequestImpl req)
        throws IOException
    {
        HttpClientImpl client = exchange.client();
        java.net.Authenticator auth =
                client.authenticator()
                      .orElseThrow(() -> new IOException("No authenticator set"));
        URI uri = req.uri();
        HeaderParser parser = new HeaderParser(header);
        String authscheme = parser.findKey(0);

        String realm = parser.findValue("realm");
        java.net.Authenticator.RequestorType rtype = proxy ? PROXY : SERVER;
        URL url = toURL(uri, req.method(), proxy);
        String host;
        int port;
        String protocol;
        InetSocketAddress proxyAddress;
        if (proxy && (proxyAddress = req.proxy()) != null) {
            // request sent to server through proxy
            proxyAddress = req.proxy();
            host = proxyAddress.getHostString();
            port = proxyAddress.getPort();
            protocol = "http"; // we don't support https connection to proxy
        } else {
            // direct connection to server or proxy
            host = uri.getHost();
            port = uri.getPort();
            protocol = uri.getScheme();
        }

        // needs to be instance method in Authenticator
        return auth.requestPasswordAuthenticationInstance(host,
                                                          null,
                                                          port,
                                                          protocol,
                                                          realm,
                                                          authscheme,
                                                          url,
                                                          rtype
        );
    }

    private URL toURL(URI uri, String method, boolean proxy)
            throws MalformedURLException
    {
        if (proxy && "CONNECT".equalsIgnoreCase(method)
                && "socket".equalsIgnoreCase(uri.getScheme())) {
            return null; // proxy tunneling
        }
        return uri.toURL();
    }

    private URI getProxyURI(HttpRequestImpl r) {
        InetSocketAddress proxy = r.proxy();
        if (proxy == null) {
            return null;
        }

        // our own private scheme for proxy URLs
        // e.g. proxy.http://host:port/
        String scheme = "proxy." + r.uri().getScheme();
        try {
            return new URI(scheme,
                           null,
                           proxy.getHostString(),
                           proxy.getPort(),
                           "/",
                           null,
                           null);
        } catch (URISyntaxException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public void request(HttpRequestImpl r, MultiExchange<?> e) throws IOException {
        // use preemptive authentication if an entry exists.
        Cache cache = getCache(e);
        this.exchange = e;

        // Proxy
        if (exchange.proxyauth == null) {
            URI proxyURI = getProxyURI(r);
            if (proxyURI != null) {
                CacheEntry ca = cache.get(proxyURI, true);
                if (ca != null) {
                    exchange.proxyauth = new AuthInfo(true, ca.scheme, null, ca, ca.isUTF8);
                    addBasicCredentials(r, true, ca.value, ca.isUTF8);
                }
            }
        }

        // Server
        if (exchange.serverauth == null) {
            CacheEntry ca = cache.get(r.uri(), false);
            if (ca != null) {
                exchange.serverauth = new AuthInfo(true, ca.scheme, null, ca, ca.isUTF8);
                addBasicCredentials(r, false, ca.value, ca.isUTF8);
            }
        }
    }

    // TODO: refactor into per auth scheme class
    private static void addBasicCredentials(HttpRequestImpl r,
                                            boolean proxy,
                                            PasswordAuthentication pw,
                                            boolean isUTF8) {
        String hdrname = proxy ? "Proxy-Authorization" : "Authorization";
        StringBuilder sb = new StringBuilder(128);
        sb.append(pw.getUserName()).append(':').append(pw.getPassword());
        var charset = isUTF8 ? UTF_8 : ISO_8859_1;
        String s = encoder.encodeToString(sb.toString().getBytes(charset));
        String value = "Basic " + s;
        if (proxy) {
            if (r.isConnect()) {
                if (!Utils.PROXY_TUNNEL_FILTER.test(hdrname, value)) {
                    Log.logError("{0} disabled", hdrname);
                    return;
                }
            } else if (r.proxy() != null) {
                if (!Utils.PROXY_FILTER.test(hdrname, value)) {
                    Log.logError("{0} disabled", hdrname);
                    return;
                }
            }
        }
        r.setSystemHeader(hdrname, value);
    }

    // Information attached to a HttpRequestImpl relating to authentication
    static class AuthInfo {
        final boolean fromcache;
        final String scheme;
        int retries;
        PasswordAuthentication credentials; // used in request
        CacheEntry cacheEntry; // if used
        final boolean isUTF8; //

        AuthInfo(boolean fromcache,
                 String scheme,
                 PasswordAuthentication credentials, boolean isUTF8) {
            this.fromcache = fromcache;
            this.scheme = scheme;
            this.credentials = credentials;
            this.retries = 1;
            this.isUTF8 = isUTF8;
        }

        AuthInfo(boolean fromcache,
                 String scheme,
                 PasswordAuthentication credentials,
                 CacheEntry ca, boolean isUTF8) {
            this(fromcache, scheme, credentials, isUTF8);
            assert credentials == null || (ca != null && ca.value == null);
            cacheEntry = ca;
        }

        AuthInfo retryWithCredentials(PasswordAuthentication pw, boolean isUTF8) {
            // If the info was already in the cache we need to create a new
            // instance with fromCache==false so that it's put back in the
            // cache if authentication succeeds
            AuthInfo res = fromcache ? new AuthInfo(false, scheme, pw, isUTF8) : this;
            res.credentials = Objects.requireNonNull(pw);
            res.retries = retries;
            return res;
        }
    }

    @Override
    public HttpRequestImpl response(Response r) throws IOException {
        Cache cache = getCache(exchange);
        int status = r.statusCode();
        HttpHeaders hdrs = r.headers();
        HttpRequestImpl req = r.request();

        if (status != PROXY_UNAUTHORIZED) {
            if (exchange.proxyauth != null && !exchange.proxyauth.fromcache) {
                AuthInfo au = exchange.proxyauth;
                URI proxyURI = getProxyURI(req);
                if (proxyURI != null) {
                    exchange.proxyauth = null;
                    cache.store(au.scheme, proxyURI, true, au.credentials, au.isUTF8);
                }
            }
            if (status != UNAUTHORIZED) {
                // check if any authentication succeeded for first time
                if (exchange.serverauth != null && !exchange.serverauth.fromcache) {
                    AuthInfo au = exchange.serverauth;
                    cache.store(au.scheme, req.uri(), false, au.credentials, au.isUTF8);
                }
                return null;
            }
        }

        boolean proxy = status == PROXY_UNAUTHORIZED;
        String authname = proxy ? "Proxy-Authenticate" : "WWW-Authenticate";
        List<String> authvals = hdrs.allValues(authname);
        if (authvals.isEmpty() && exchange.client().authenticator().isPresent()) {
            throw new IOException(authname + " header missing for response code " + status);
        }
        String authval = null;
        boolean isUTF8 = false;
        for (String aval : authvals) {
            HeaderParser parser = new HeaderParser(aval);
            String scheme = parser.findKey(0);
            if (scheme != null && scheme.equalsIgnoreCase("Basic")) {
                authval = aval;
                var charset = parser.findValue("charset");
                isUTF8 = (charset != null && charset.equalsIgnoreCase("UTF-8"));
                break;
            }
        }
        if (authval == null) {
            return null;
        }

        if (proxy) {
            if (r.isConnectResponse) {
                if (!Utils.PROXY_TUNNEL_FILTER
                        .test("Proxy-Authorization", BASIC_DUMMY)) {
                    Log.logError("{0} disabled", "Proxy-Authorization");
                    return null;
                }
            } else if (req.proxy() != null) {
                if (!Utils.PROXY_FILTER
                        .test("Proxy-Authorization", BASIC_DUMMY)) {
                    Log.logError("{0} disabled", "Proxy-Authorization");
                    return null;
                }
            }
        }

        AuthInfo au = proxy ? exchange.proxyauth : exchange.serverauth;
        if (au == null) {
            // if no authenticator, let the user deal with 407/401
            if (!exchange.client().authenticator().isPresent()) return null;

            PasswordAuthentication pw = getCredentials(authval, proxy, req);
            if (pw == null) {
                throw new IOException("No credentials provided");
            }
            // No authentication in request. Get credentials from user
            au = new AuthInfo(false, "Basic", pw, isUTF8);
            if (proxy) {
                exchange.proxyauth = au;
            } else {
                exchange.serverauth = au;
            }
            req = HttpRequestImpl.newInstanceForAuthentication(req);
            addBasicCredentials(req, proxy, pw, isUTF8);
            return req;
        } else if (au.retries > retry_limit) {
            throw new IOException("too many authentication attempts. Limit: " +
                    Integer.toString(retry_limit));
        } else {
            // we sent credentials, but they were rejected
            if (au.fromcache) {
                cache.remove(au.cacheEntry);
            }

            // if no authenticator, let the user deal with 407/401
            if (!exchange.client().authenticator().isPresent()) return null;

            // try again
            PasswordAuthentication pw = getCredentials(authval, proxy, req);
            if (pw == null) {
                throw new IOException("No credentials provided");
            }
            au = au.retryWithCredentials(pw, isUTF8);
            if (proxy) {
                exchange.proxyauth = au;
            } else {
                exchange.serverauth = au;
            }
            req = HttpRequestImpl.newInstanceForAuthentication(req);
            addBasicCredentials(req, proxy, au.credentials, isUTF8);
            au.retries++;
            return req;
        }
    }

    // Use a WeakHashMap to make it possible for the HttpClient to
    // be garbage collected when no longer referenced.
    static final WeakHashMap<HttpClientImpl,Cache> caches = new WeakHashMap<>();

    static synchronized Cache getCache(MultiExchange<?> exchange) {
        HttpClientImpl client = exchange.client();
        Cache c = caches.get(client);
        if (c == null) {
            c = new Cache();
            caches.put(client, c);
        }
        return c;
    }

    // Note: Make sure that Cache and CacheEntry do not keep any strong
    //       reference to the HttpClient: it would prevent the client being
    //       GC'ed when no longer referenced.
    static final class Cache {
        final LinkedList<CacheEntry> entries = new LinkedList<>();

        Cache() {}

        synchronized CacheEntry get(URI uri, boolean proxy) {
            for (CacheEntry entry : entries) {
                if (entry.equalsKey(uri, proxy)) {
                    return entry;
                }
            }
            return null;
        }

        private static boolean equalsIgnoreCase(String s1, String s2) {
            return s1 == s2 || (s1 != null && s1.equalsIgnoreCase(s2));
        }

        synchronized void remove(String authscheme, URI domain, boolean proxy) {
            var iterator = entries.iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (equalsIgnoreCase(entry.scheme, authscheme)) {
                    if (entry.equalsKey(domain, proxy)) {
                        iterator.remove();
                    }
                }
            }
        }

        synchronized void remove(CacheEntry entry) {
            entries.remove(entry);
        }

        synchronized void store(String authscheme,
                                URI domain,
                                boolean proxy,
                                PasswordAuthentication value, boolean isUTF8) {
            remove(authscheme, domain, proxy);
            entries.add(new CacheEntry(authscheme, domain, proxy, value, isUTF8));
        }
    }

    static URI normalize(URI uri, boolean isPrimaryKey) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            // make sure the URI has a path, ignore query and fragment
            try {
                return new URI(uri.getScheme(), uri.getAuthority(), "/", null, null);
            } catch (URISyntaxException e) {
                throw new InternalError(e);
            }
        } else if (isPrimaryKey || !"/".equals(path)) {
            // remove extraneous components and normalize path
            return uri.resolve(".");
        } else {
            // path == "/" and the URI is not used to store
            // the primary key in the cache: nothing to do.
            return uri;
        }
    }

    static final class CacheEntry {
        final String root;
        final String scheme;
        final boolean proxy;
        final PasswordAuthentication value;
        final boolean isUTF8;

        CacheEntry(String authscheme,
                   URI uri,
                   boolean proxy,
                   PasswordAuthentication value, boolean isUTF8) {
            this.scheme = authscheme;
            this.root = normalize(uri, true).toString(); // remove extraneous components
            this.proxy = proxy;
            this.value = value;
            this.isUTF8 = isUTF8;
        }

        public PasswordAuthentication value() {
            return value;
        }

        public boolean equalsKey(URI uri, boolean proxy) {
            if (this.proxy != proxy) {
                return false;
            }
            String other = String.valueOf(normalize(uri, false));
            return other.startsWith(root);
        }
    }
}
