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

package sun.net.www.protocol.http;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.net.URL;
import java.net.URLConnection;
import java.net.ProtocolException;
import java.net.HttpRetryException;
import java.net.PasswordAuthentication;
import java.net.Authenticator;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.SocketPermission;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.CookieHandler;
import java.net.ResponseCache;
import java.net.CacheResponse;
import java.net.SecureCacheResponse;
import java.net.CacheRequest;
import java.net.URLPermission;
import java.net.Authenticator.RequestorType;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.StringJoiner;
import jdk.internal.access.JavaNetHttpCookieAccess;
import jdk.internal.access.SharedSecrets;
import sun.net.*;
import sun.net.util.IPAddressUtil;
import sun.net.www.*;
import sun.net.www.http.HttpClient;
import sun.net.www.http.PosterOutputStream;
import sun.net.www.http.ChunkedInputStream;
import sun.net.www.http.ChunkedOutputStream;
import sun.util.logging.PlatformLogger;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import static sun.net.www.protocol.http.AuthScheme.BASIC;
import static sun.net.www.protocol.http.AuthScheme.DIGEST;
import static sun.net.www.protocol.http.AuthScheme.NTLM;
import static sun.net.www.protocol.http.AuthScheme.NEGOTIATE;
import static sun.net.www.protocol.http.AuthScheme.KERBEROS;
import static sun.net.www.protocol.http.AuthScheme.UNKNOWN;
import sun.security.action.GetIntegerAction;
import sun.security.action.GetPropertyAction;

/**
 * A class to represent an HTTP connection to a remote object.
 */


public class HttpURLConnection extends java.net.HttpURLConnection {

    static final String HTTP_CONNECT = "CONNECT";

    static final String version;
    public static final String userAgent;

    /* max # of allowed re-directs */
    static final int defaultmaxRedirects = 20;
    static final int maxRedirects;

    /* Not all servers support the (Proxy)-Authentication-Info headers.
     * By default, we don't require them to be sent
     */
    static final boolean validateProxy;
    static final boolean validateServer;

    /** A, possibly empty, set of authentication schemes that are disabled
     *  when proxying plain HTTP ( not HTTPS ). */
    static final Set<String> disabledProxyingSchemes;

    /** A, possibly empty, set of authentication schemes that are disabled
     *  when setting up a tunnel for HTTPS ( HTTP CONNECT ). */
    static final Set<String> disabledTunnelingSchemes;

    private StreamingOutputStream strOutputStream;
    private static final String RETRY_MSG1 =
        "cannot retry due to proxy authentication, in streaming mode";
    private static final String RETRY_MSG2 =
        "cannot retry due to server authentication, in streaming mode";
    private static final String RETRY_MSG3 =
        "cannot retry due to redirection, in streaming mode";

    /*
     * System properties related to error stream handling:
     *
     * sun.net.http.errorstream.enableBuffering = <boolean>
     *
     * With the above system property set to true (default is false),
     * when the response code is >=400, the HTTP handler will try to
     * buffer the response body (up to a certain amount and within a
     * time limit). Thus freeing up the underlying socket connection
     * for reuse. The rationale behind this is that usually when the
     * server responds with a >=400 error (client error or server
     * error, such as 404 file not found), the server will send a
     * small response body to explain who to contact and what to do to
     * recover. With this property set to true, even if the
     * application doesn't call getErrorStream(), read the response
     * body, and then call close(), the underlying socket connection
     * can still be kept-alive and reused. The following two system
     * properties provide further control to the error stream
     * buffering behaviour.
     *
     * sun.net.http.errorstream.timeout = <int>
     *     the timeout (in millisec) waiting the error stream
     *     to be buffered; default is 300 ms
     *
     * sun.net.http.errorstream.bufferSize = <int>
     *     the size (in bytes) to use for the buffering the error stream;
     *     default is 4k
     */


    /* Should we enable buffering of error streams? */
    private static boolean enableESBuffer = false;

    /* timeout waiting for read for buffered error stream;
     */
    private static int timeout4ESBuffer = 0;

    /* buffer size for buffered error stream;
     */
    private static int bufSize4ES = 0;

    /*
     * Restrict setting of request headers through the public api
     * consistent with JavaScript XMLHttpRequest2 with a few
     * exceptions. Disallowed headers are silently ignored for
     * backwards compatibility reasons rather than throwing a
     * SecurityException. For example, some applets set the
     * Host header since old JREs did not implement HTTP 1.1.
     * Additionally, any header starting with Sec- is
     * disallowed.
     *
     * The following headers are allowed for historical reasons:
     *
     * Accept-Charset, Accept-Encoding, Cookie, Cookie2, Date,
     * Referer, TE, User-Agent, headers beginning with Proxy-.
     *
     * The following headers are allowed in a limited form:
     *
     * Connection: close
     *
     * See http://www.w3.org/TR/XMLHttpRequest2.
     */
    private static final boolean allowRestrictedHeaders;
    private static final Set<String> restrictedHeaderSet;
    private static final String[] restrictedHeaders = {
        /* Restricted by XMLHttpRequest2 */
        //"Accept-Charset",
        //"Accept-Encoding",
        "Access-Control-Request-Headers",
        "Access-Control-Request-Method",
        "Connection", /* close is allowed */
        "Content-Length",
        //"Cookie",
        //"Cookie2",
        "Content-Transfer-Encoding",
        //"Date",
        //"Expect",
        "Host",
        "Keep-Alive",
        "Origin",
        // "Referer",
        // "TE",
        "Trailer",
        "Transfer-Encoding",
        "Upgrade",
        //"User-Agent",
        "Via"
    };

    @SuppressWarnings("removal")
    private static String getNetProperty(String name) {
        PrivilegedAction<String> pa = () -> NetProperties.get(name);
        return AccessController.doPrivileged(pa);
    }

    private static Set<String> schemesListToSet(String list) {
        if (list == null || list.isEmpty())
            return Collections.emptySet();

        Set<String> s = new HashSet<>();
        String[] parts = list.split("\\s*,\\s*");
        for (String part : parts)
            s.add(part.toLowerCase(Locale.ROOT));
        return s;
    }

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();
        maxRedirects = GetIntegerAction.privilegedGetProperty(
                "http.maxRedirects", defaultmaxRedirects);
        version = props.getProperty("java.version");
        String agent = props.getProperty("http.agent");
        if (agent == null) {
            agent = "Java/"+version;
        } else {
            agent = agent + " Java/"+version;
        }
        userAgent = agent;

        // A set of net properties to control the use of authentication schemes
        // when proxying/tunneling.
        String p = getNetProperty("jdk.http.auth.tunneling.disabledSchemes");
        disabledTunnelingSchemes = schemesListToSet(p);
        p = getNetProperty("jdk.http.auth.proxying.disabledSchemes");
        disabledProxyingSchemes = schemesListToSet(p);

        validateProxy = Boolean.parseBoolean(
                props.getProperty("http.auth.digest.validateProxy"));
        validateServer = Boolean.parseBoolean(
                props.getProperty("http.auth.digest.validateServer"));

        enableESBuffer = Boolean.parseBoolean(
                props.getProperty("sun.net.http.errorstream.enableBuffering"));
        timeout4ESBuffer = GetIntegerAction.privilegedGetProperty(
                "sun.net.http.errorstream.timeout", 300);
        if (timeout4ESBuffer <= 0) {
            timeout4ESBuffer = 300; // use the default
        }

        bufSize4ES = GetIntegerAction.privilegedGetProperty(
                "sun.net.http.errorstream.bufferSize", 4096);
        if (bufSize4ES <= 0) {
            bufSize4ES = 4096; // use the default
        }

        allowRestrictedHeaders = Boolean.parseBoolean(
                props.getProperty("sun.net.http.allowRestrictedHeaders"));
        if (!allowRestrictedHeaders) {
            restrictedHeaderSet = new HashSet<>(restrictedHeaders.length);
            for (int i=0; i < restrictedHeaders.length; i++) {
                restrictedHeaderSet.add(restrictedHeaders[i].toLowerCase());
            }
        } else {
            restrictedHeaderSet = null;
        }
    }

    static final String httpVersion = "HTTP/1.1";
    static final String acceptString =
        "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2";

    // the following http request headers should NOT have their values
    // returned for security reasons.
    private static final String[] EXCLUDE_HEADERS = {
            "Proxy-Authorization",
            "Authorization"
    };

    // also exclude system cookies when any might be set
    private static final String[] EXCLUDE_HEADERS2= {
            "Proxy-Authorization",
            "Authorization",
            "Cookie",
            "Cookie2"
    };

    protected HttpClient http;
    protected Handler handler;
    protected Proxy instProxy;
    protected volatile Authenticator authenticator;
    protected volatile String authenticatorKey;

    private CookieHandler cookieHandler;
    private final ResponseCache cacheHandler;

    private volatile boolean usingProxy;

    // the cached response, and cached response headers and body
    protected CacheResponse cachedResponse;
    private MessageHeader cachedHeaders;
    private InputStream cachedInputStream;

    /* output stream to server */
    protected PrintStream ps = null;

    /* buffered error stream */
    private InputStream errorStream = null;

    /* User set Cookies */
    private boolean setUserCookies = true;
    private String userCookies = null;
    private String userCookies2 = null;

    /* We only have a single static authenticator for now.
     * REMIND:  backwards compatibility with JDK 1.1.  Should be
     * eliminated for JDK 2.0.
     */
    @Deprecated
    private static HttpAuthenticator defaultAuth;

    /* all the headers we send
     * NOTE: do *NOT* dump out the content of 'requests' in the
     * output or stacktrace since it may contain security-sensitive
     * headers such as those defined in EXCLUDE_HEADERS.
     */
    private MessageHeader requests;

    /* The headers actually set by the user are recorded here also
     */
    private MessageHeader userHeaders;

    /* Headers and request method cannot be changed
     * once this flag is set in :-
     *     - getOutputStream()
     *     - getInputStream())
     *     - connect()
     * Access is protected by connectionLock.
     */
    private boolean connecting = false;

    /* The following two fields are only used with Digest Authentication */
    String domain;      /* The list of authentication domains */
    DigestAuthentication.Parameters digestparams;

    /* Current credentials in use */
    AuthenticationInfo  currentProxyCredentials = null;
    AuthenticationInfo  currentServerCredentials = null;
    boolean             needToCheck = true;
    private boolean doingNTLM2ndStage = false; /* doing the 2nd stage of an NTLM server authentication */
    private boolean doingNTLMp2ndStage = false; /* doing the 2nd stage of an NTLM proxy authentication */

    /* try auth without calling Authenticator. Used for transparent NTLM authentication */
    private boolean tryTransparentNTLMServer = true;
    private boolean tryTransparentNTLMProxy = true;
    private boolean useProxyResponseCode = false;

    /* Used by Windows specific code */
    private Object authObj;

    /* Set if the user is manually setting the Authorization or Proxy-Authorization headers */
    boolean isUserServerAuth;
    boolean isUserProxyAuth;

    String serverAuthKey, proxyAuthKey;

    /* Progress source */
    protected ProgressSource pi;

    /* all the response headers we get back */
    private MessageHeader responses;
    /* the stream _from_ the server */
    private InputStream inputStream = null;
    /* post stream _to_ the server, if any */
    private PosterOutputStream poster = null;

    /* Indicates if the std. request headers have been set in requests. */
    private boolean setRequests=false;

    /* Indicates whether a request has already failed or not */
    private boolean failedOnce=false;

    /* Remembered Exception, we will throw it again if somebody
       calls getInputStream after disconnect */
    private Exception rememberedException = null;

    /* If we decide we want to reuse a client, we put it here */
    private HttpClient reuseClient = null;

    /* Tunnel states */
    public enum TunnelState {
        /* No tunnel */
        NONE,

        /* Setting up a tunnel */
        SETUP,

        /* Tunnel has been successfully setup */
        TUNNELING
    }

    private TunnelState tunnelState = TunnelState.NONE;

    /* Redefine timeouts from java.net.URLConnection as we need -1 to mean
     * not set. This is to ensure backward compatibility.
     */
    private int connectTimeout = NetworkClient.DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = NetworkClient.DEFAULT_READ_TIMEOUT;

    /* A permission converted from a URLPermission */
    private SocketPermission socketPermission;

    /* Logging support */
    private static final PlatformLogger logger =
            PlatformLogger.getLogger("sun.net.www.protocol.http.HttpURLConnection");

    /* Lock */
    private final ReentrantLock connectionLock = new ReentrantLock();

    private final void lock() {
        connectionLock.lock();
    }

    private final void unlock() {
        connectionLock.unlock();
    }

    public final boolean isLockHeldByCurrentThread() {
        return connectionLock.isHeldByCurrentThread();
    }


    /*
     * privileged request password authentication
     *
     */
    @SuppressWarnings("removal")
    private static PasswordAuthentication
    privilegedRequestPasswordAuthentication(
                            final Authenticator authenticator,
                            final String host,
                            final InetAddress addr,
                            final int port,
                            final String protocol,
                            final String prompt,
                            final String scheme,
                            final URL url,
                            final RequestorType authType) {
        return java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<>() {
                public PasswordAuthentication run() {
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Requesting Authentication: host =" + host + " url = " + url);
                    }
                    PasswordAuthentication pass = Authenticator.requestPasswordAuthentication(
                        authenticator, host, addr, port, protocol,
                        prompt, scheme, url, authType);
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Authentication returned: " + (pass != null ? pass.toString() : "null"));
                    }
                    return pass;
                }
            });
    }

    private boolean isRestrictedHeader(String key, String value) {
        if (allowRestrictedHeaders) {
            return false;
        }

        key = key.toLowerCase();
        if (restrictedHeaderSet.contains(key)) {
            /*
             * Exceptions to restricted headers:
             *
             * Allow "Connection: close".
             */
            if (key.equals("connection") && value.equalsIgnoreCase("close")) {
                return false;
            }
            return true;
        } else if (key.startsWith("sec-")) {
            return true;
        }
        return false;
    }

    /*
     * Checks the validity of http message header and whether the header
     * is restricted and throws IllegalArgumentException if invalid or
     * restricted.
     */
    private boolean isExternalMessageHeaderAllowed(String key, String value) {
        checkMessageHeader(key, value);
        if (!isRestrictedHeader(key, value)) {
            return true;
        }
        return false;
    }

    /* Logging support */
    public static PlatformLogger getHttpLogger() {
        return logger;
    }

    /* Used for Windows NTLM implementation */
    public Object authObj() {
        return authObj;
    }

    public void authObj(Object authObj) {
        this.authObj = authObj;
    }

    @Override
    public void setAuthenticator(Authenticator auth) {
        lock();
        try {
            if (connecting || connected) {
                throw new IllegalStateException(
                        "Authenticator must be set before connecting");
            }
            authenticator = Objects.requireNonNull(auth);
            authenticatorKey = AuthenticatorKeys.getKey(authenticator);
        } finally {
            unlock();
        }
    }

    public String getAuthenticatorKey() {
        String k = authenticatorKey;
        if (k == null) return AuthenticatorKeys.getKey(authenticator);
        return k;
    }

    /*
     * checks the validity of http message header and throws
     * IllegalArgumentException if invalid.
     */
    private void checkMessageHeader(String key, String value) {
        char LF = '\n';
        int index = key.indexOf(LF);
        int index1 = key.indexOf(':');
        if (index != -1 || index1 != -1) {
            throw new IllegalArgumentException(
                "Illegal character(s) in message header field: " + key);
        }
        else {
            if (value == null) {
                return;
            }

            index = value.indexOf(LF);
            while (index != -1) {
                index++;
                if (index < value.length()) {
                    char c = value.charAt(index);
                    if ((c==' ') || (c=='\t')) {
                        // ok, check the next occurrence
                        index = value.indexOf(LF, index);
                        continue;
                    }
                }
                throw new IllegalArgumentException(
                    "Illegal character(s) in message header value: " + value);
            }
        }
    }

    public void setRequestMethod(String method)
                        throws ProtocolException {
        lock();
        try {
            if (connecting) {
                throw new IllegalStateException("connect in progress");
            }
            super.setRequestMethod(method);
        } finally {
            unlock();
        }
    }

    /* adds the standard key/val pairs to reqests if necessary & write to
     * given PrintStream
     */
    private void writeRequests() throws IOException {
        assert isLockHeldByCurrentThread();

        /* print all message headers in the MessageHeader
         * onto the wire - all the ones we've set and any
         * others that have been set
         */
        // send any pre-emptive authentication
        if (http.usingProxy && tunnelState() != TunnelState.TUNNELING) {
            setPreemptiveProxyAuthentication(requests);
        }
        if (!setRequests) {

            /* We're very particular about the order in which we
             * set the request headers here.  The order should not
             * matter, but some careless CGI programs have been
             * written to expect a very particular order of the
             * standard headers.  To name names, the order in which
             * Navigator3.0 sends them.  In particular, we make *sure*
             * to send Content-type: <> and Content-length:<> second
             * to last and last, respectively, in the case of a POST
             * request.
             */
            if (!failedOnce) {
                checkURLFile();
                requests.prepend(method + " " + getRequestURI()+" "  +
                                 httpVersion, null);
            }
            if (!getUseCaches()) {
                requests.setIfNotSet ("Cache-Control", "no-cache");
                requests.setIfNotSet ("Pragma", "no-cache");
            }
            requests.setIfNotSet("User-Agent", userAgent);
            int port = url.getPort();
            String host = stripIPv6ZoneId(url.getHost());
            if (port != -1 && port != url.getDefaultPort()) {
                host += ":" + String.valueOf(port);
            }
            String reqHost = requests.findValue("Host");
            if (reqHost == null ||
                (!reqHost.equalsIgnoreCase(host) && !checkSetHost()))
            {
                requests.set("Host", host);
            }
            requests.setIfNotSet("Accept", acceptString);

            /*
             * For HTTP/1.1 the default behavior is to keep connections alive.
             * However, we may be talking to a 1.0 server so we should set
             * keep-alive just in case, except if we have encountered an error
             * or if keep alive is disabled via a system property
             */

            // Try keep-alive only on first attempt
            if (!failedOnce && http.getHttpKeepAliveSet()) {
                if (http.usingProxy && tunnelState() != TunnelState.TUNNELING) {
                    requests.setIfNotSet("Proxy-Connection", "keep-alive");
                } else {
                    requests.setIfNotSet("Connection", "keep-alive");
                }
            } else {
                /*
                 * RFC 2616 HTTP/1.1 section 14.10 says:
                 * HTTP/1.1 applications that do not support persistent
                 * connections MUST include the "close" connection option
                 * in every message
                 */
                requests.setIfNotSet("Connection", "close");
            }
            // Set modified since if necessary
            long modTime = getIfModifiedSince();
            if (modTime != 0 ) {
                Date date = new Date(modTime);
                //use the preferred date format according to RFC 2068(HTTP1.1),
                // RFC 822 and RFC 1123
                SimpleDateFormat fo =
                  new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                fo.setTimeZone(TimeZone.getTimeZone("GMT"));
                requests.setIfNotSet("If-Modified-Since", fo.format(date));
            }
            // check for preemptive authorization
            AuthenticationInfo sauth = AuthenticationInfo.getServerAuth(url,
                                             getAuthenticatorKey());
            if (sauth != null && sauth.supportsPreemptiveAuthorization() ) {
                // Sets "Authorization"
                requests.setIfNotSet(sauth.getHeaderName(), sauth.getHeaderValue(url,method));
                currentServerCredentials = sauth;
            }

            if (!method.equals("PUT") && (poster != null || streaming())) {
                requests.setIfNotSet ("Content-type",
                        "application/x-www-form-urlencoded");
            }

            boolean chunked = false;

            if (streaming()) {
                if (chunkLength != -1) {
                    requests.set ("Transfer-Encoding", "chunked");
                    chunked = true;
                } else { /* fixed content length */
                    if (fixedContentLengthLong != -1) {
                        requests.set ("Content-Length",
                                      String.valueOf(fixedContentLengthLong));
                    } else if (fixedContentLength != -1) {
                        requests.set ("Content-Length",
                                      String.valueOf(fixedContentLength));
                    }
                }
            } else if (poster != null) {
                /* add Content-Length & POST/PUT data */
                // safe to synchronize on poster: this is
                // a simple subclass of ByteArrayOutputStream
                synchronized (poster) {
                    /* close it, so no more data can be added */
                    poster.close();
                    requests.set("Content-Length",
                                 String.valueOf(poster.size()));
                }
            }

            if (!chunked) {
                if (requests.findValue("Transfer-Encoding") != null) {
                    requests.remove("Transfer-Encoding");
                    if (logger.isLoggable(PlatformLogger.Level.WARNING)) {
                        logger.warning(
                            "use streaming mode for chunked encoding");
                    }
                }
            }

            // get applicable cookies based on the uri and request headers
            // add them to the existing request headers
            setCookieHeader();

            setRequests=true;
        }
        if (logger.isLoggable(PlatformLogger.Level.FINE)) {
            logger.fine(requests.toString());
        }
        http.writeRequests(requests, poster, streaming());
        if (ps.checkError()) {
            String proxyHost = http.getProxyHostUsed();
            int proxyPort = http.getProxyPortUsed();
            disconnectInternal();
            if (failedOnce) {
                throw new IOException("Error writing to server");
            } else { // try once more
                failedOnce=true;
                if (proxyHost != null) {
                    setProxiedClient(url, proxyHost, proxyPort);
                } else {
                    setNewClient (url);
                }
                ps = (PrintStream) http.getOutputStream();
                connected=true;
                responses = new MessageHeader();
                setRequests=false;
                writeRequests();
            }
        }
    }

    private boolean checkSetHost() {
        @SuppressWarnings("removal")
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            String name = s.getClass().getName();
            if (name.equals("sun.plugin2.applet.AWTAppletSecurityManager") ||
                name.equals("sun.plugin2.applet.FXAppletSecurityManager") ||
                name.equals("com.sun.javaws.security.JavaWebStartSecurity") ||
                name.equals("sun.plugin.security.ActivatorSecurityManager"))
            {
                int CHECK_SET_HOST = -2;
                try {
                    s.checkConnect(url.toExternalForm(), CHECK_SET_HOST);
                } catch (SecurityException ex) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkURLFile() {
        @SuppressWarnings("removal")
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            String name = s.getClass().getName();
            if (name.equals("sun.plugin2.applet.AWTAppletSecurityManager") ||
                name.equals("sun.plugin2.applet.FXAppletSecurityManager") ||
                name.equals("com.sun.javaws.security.JavaWebStartSecurity") ||
                name.equals("sun.plugin.security.ActivatorSecurityManager"))
            {
                int CHECK_SUBPATH = -3;
                try {
                    s.checkConnect(url.toExternalForm(), CHECK_SUBPATH);
                } catch (SecurityException ex) {
                    throw new SecurityException("denied access outside a permitted URL subpath", ex);
                }
            }
        }
    }

    /**
     * Create a new HttpClient object, bypassing the cache of
     * HTTP client objects/connections.
     *
     * @param url       the URL being accessed
     */
    protected void setNewClient (URL url)
    throws IOException {
        setNewClient(url, false);
    }

    /**
     * Obtain a HttpsClient object. Use the cached copy if specified.
     *
     * @param url       the URL being accessed
     * @param useCache  whether the cached connection should be used
     *        if present
     */
    protected void setNewClient (URL url, boolean useCache)
        throws IOException {
        http = HttpClient.New(url, null, -1, useCache, connectTimeout, this);
        http.setReadTimeout(readTimeout);
    }


    /**
     * Create a new HttpClient object, set up so that it uses
     * per-instance proxying to the given HTTP proxy.  This
     * bypasses the cache of HTTP client objects/connections.
     *
     * @param url       the URL being accessed
     * @param proxyHost the proxy host to use
     * @param proxyPort the proxy port to use
     */
    protected void setProxiedClient (URL url, String proxyHost, int proxyPort)
    throws IOException {
        setProxiedClient(url, proxyHost, proxyPort, false);
    }

    /**
     * Obtain a HttpClient object, set up so that it uses per-instance
     * proxying to the given HTTP proxy. Use the cached copy of HTTP
     * client objects/connections if specified.
     *
     * @param url       the URL being accessed
     * @param proxyHost the proxy host to use
     * @param proxyPort the proxy port to use
     * @param useCache  whether the cached connection should be used
     *        if present
     */
    protected void setProxiedClient (URL url,
                                     String proxyHost, int proxyPort,
                                     boolean useCache)
        throws IOException {
        proxiedConnect(url, proxyHost, proxyPort, useCache);
    }

    protected void proxiedConnect(URL url,
                                  String proxyHost, int proxyPort,
                                  boolean useCache)
        throws IOException {
        http = HttpClient.New (url, proxyHost, proxyPort, useCache,
            connectTimeout, this);
        http.setReadTimeout(readTimeout);
    }

    protected HttpURLConnection(URL u, Handler handler)
    throws IOException {
        // we set proxy == null to distinguish this case with the case
        // when per connection proxy is set
        this(u, null, handler);
    }

    private static String checkHost(String h) throws IOException {
        if (h != null) {
            if (h.indexOf('\n') > -1) {
                throw new MalformedURLException("Illegal character in host");
            }
        }
        return h;
    }
    public HttpURLConnection(URL u, String host, int port) throws IOException {
        this(u, new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved(checkHost(host), port)));
    }

    /** this constructor is used by other protocol handlers such as ftp
        that want to use http to fetch urls on their behalf.*/
    public HttpURLConnection(URL u, Proxy p) throws IOException {
        this(u, p, new Handler());
    }

    private static URL checkURL(URL u) throws IOException {
        if (u != null) {
            if (u.toExternalForm().indexOf('\n') > -1) {
                throw new MalformedURLException("Illegal character in URL");
            }
        }
        String s = IPAddressUtil.checkAuthority(u);
        if (s != null) {
            throw new MalformedURLException(s);
        }
        return u;
    }

    @SuppressWarnings("removal")
    protected HttpURLConnection(URL u, Proxy p, Handler handler)
            throws IOException {
        super(checkURL(u));
        requests = new MessageHeader();
        responses = new MessageHeader();
        userHeaders = new MessageHeader();
        this.handler = handler;
        instProxy = p;
        if (instProxy instanceof sun.net.ApplicationProxy) {
            /* Application set Proxies should not have access to cookies
             * in a secure environment unless explicitly allowed. */
            try {
                cookieHandler = CookieHandler.getDefault();
            } catch (SecurityException se) { /* swallow exception */ }
        } else {
            cookieHandler = java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<>() {
                public CookieHandler run() {
                    return CookieHandler.getDefault();
                }
            });
        }
        cacheHandler = java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<>() {
                public ResponseCache run() {
                return ResponseCache.getDefault();
            }
        });
    }

    /**
     * @deprecated.  Use java.net.Authenticator.setDefault() instead.
     */
    @Deprecated
    public static void setDefaultAuthenticator(HttpAuthenticator a) {
        defaultAuth = a;
    }

    /**
     * opens a stream allowing redirects only to the same host.
     */
    public static InputStream openConnectionCheckRedirects(URLConnection c)
        throws IOException
    {
        boolean redir;
        int redirects = 0;
        InputStream in;
        Authenticator a = null;

        do {
            if (c instanceof HttpURLConnection) {
                ((HttpURLConnection) c).setInstanceFollowRedirects(false);
                if (a == null) {
                    a = ((HttpURLConnection) c).authenticator;
                }
            }

            // We want to open the input stream before
            // getting headers, because getHeaderField()
            // et al swallow IOExceptions.
            in = c.getInputStream();
            redir = false;

            if (c instanceof HttpURLConnection) {
                HttpURLConnection http = (HttpURLConnection) c;
                int stat = http.getResponseCode();
                if (stat >= 300 && stat <= 307 && stat != 306 &&
                        stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    URL base = http.getURL();
                    String loc = http.getHeaderField("Location");
                    URL target = null;
                    if (loc != null) {
                        target = new URL(base, loc);
                    }
                    http.disconnect();
                    if (target == null
                        || !base.getProtocol().equals(target.getProtocol())
                        || base.getPort() != target.getPort()
                        || !hostsEqual(base, target)
                        || redirects >= 5)
                    {
                        throw new SecurityException("illegal URL redirect");
                    }
                    redir = true;
                    c = target.openConnection();
                    if (a != null && c instanceof HttpURLConnection) {
                        ((HttpURLConnection)c).setAuthenticator(a);
                    }
                    redirects++;
                }
            }
        } while (redir);
        return in;
    }


    //
    // Same as java.net.URL.hostsEqual
    //
    @SuppressWarnings("removal")
    private static boolean hostsEqual(URL u1, URL u2) {
        final String h1 = u1.getHost();
        final String h2 = u2.getHost();

        if (h1 == null) {
            return h2 == null;
        } else if (h2 == null) {
            return false;
        } else if (h1.equalsIgnoreCase(h2)) {
            return true;
        }
        // Have to resolve addresses before comparing, otherwise
        // names like tachyon and tachyon.eng would compare different
        final boolean result[] = {false};

        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<>() {
                public Void run() {
                try {
                    InetAddress a1 = InetAddress.getByName(h1);
                    InetAddress a2 = InetAddress.getByName(h2);
                    result[0] = a1.equals(a2);
                } catch(UnknownHostException | SecurityException e) {
                }
                return null;
            }
        });

        return result[0];
    }

    // overridden in HTTPS subclass

    public void connect() throws IOException {
        lock();
        try {
            connecting = true;
        } finally {
            unlock();
        }
        plainConnect();
    }

    private boolean checkReuseConnection () {
        if (connected) {
            return true;
        }
        if (reuseClient != null) {
            http = reuseClient;
            http.setReadTimeout(getReadTimeout());
            http.reuse = false;
            reuseClient = null;
            connected = true;
            return true;
        }
        return false;
    }

    @SuppressWarnings("removal")
    private String getHostAndPort(URL url) {
        String host = url.getHost();
        final String hostarg = host;
        try {
            // lookup hostname and use IP address if available
            host = AccessController.doPrivileged(
                new PrivilegedExceptionAction<>() {
                    public String run() throws IOException {
                            InetAddress addr = InetAddress.getByName(hostarg);
                            return addr.getHostAddress();
                    }
                }
            );
        } catch (PrivilegedActionException e) {}
        int port = url.getPort();
        if (port == -1) {
            String scheme = url.getProtocol();
            if ("http".equals(scheme)) {
                return host + ":80";
            } else { // scheme must be https
                return host + ":443";
            }
        }
        return host + ":" + Integer.toString(port);
    }

    @SuppressWarnings("removal")
    protected void plainConnect() throws IOException {
        lock();
        try {
            if (connected) {
                return;
            }
        } finally {
            unlock();
        }
        SocketPermission p = URLtoSocketPermission(this.url);
        if (p != null) {
            try {
                AccessController.doPrivilegedWithCombiner(
                    new PrivilegedExceptionAction<>() {
                        public Void run() throws IOException {
                            plainConnect0();
                            return null;
                        }
                    }, null, p
                );
            } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
            }
        } else {
            // run without additional permission
            plainConnect0();
        }
    }

    /**
     *  if the caller has a URLPermission for connecting to the
     *  given URL, then return a SocketPermission which permits
     *  access to that destination. Return null otherwise. The permission
     *  is cached in a field (which can only be changed by redirects)
     */
    SocketPermission URLtoSocketPermission(URL url) throws IOException {

        if (socketPermission != null) {
            return socketPermission;
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();

        if (sm == null) {
            return null;
        }

        // the permission, which we might grant

        SocketPermission newPerm = new SocketPermission(
            getHostAndPort(url), "connect"
        );

        String actions = getRequestMethod()+":" +
                getUserSetHeaders().getHeaderNamesInList();

        String urlstring = url.getProtocol() + "://" + url.getAuthority()
                + url.getPath();

        URLPermission p = new URLPermission(urlstring, actions);
        try {
            sm.checkPermission(p);
            socketPermission = newPerm;
            return socketPermission;
        } catch (SecurityException e) {
            // fall thru
        }
        return null;
    }

    protected void plainConnect0()  throws IOException {
        // try to see if request can be served from local cache
        if (cacheHandler != null && getUseCaches()) {
            try {
                URI uri = ParseUtil.toURI(url);
                if (uri != null) {
                    cachedResponse = cacheHandler.get(uri, getRequestMethod(), getUserSetHeaders().getHeaders());
                    if ("https".equalsIgnoreCase(uri.getScheme())
                        && !(cachedResponse instanceof SecureCacheResponse)) {
                        cachedResponse = null;
                    }
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Cache Request for " + uri + " / " + getRequestMethod());
                        logger.finest("From cache: " + (cachedResponse != null ? cachedResponse.toString() : "null"));
                    }
                    if (cachedResponse != null) {
                        cachedHeaders = mapToMessageHeader(cachedResponse.getHeaders());
                        cachedInputStream = cachedResponse.getBody();
                    }
                }
            } catch (IOException ioex) {
                // ignore and commence normal connection
            }
            if (cachedHeaders != null && cachedInputStream != null) {
                connected = true;
                return;
            } else {
                cachedResponse = null;
            }
        }
        try {
            /* Try to open connections using the following scheme,
             * return on the first one that's successful:
             * 1) if (instProxy != null)
             *        connect to instProxy; raise exception if failed
             * 2) else use system default ProxySelector
             * 3) else make a direct connection if ProxySelector is not present
             */

            if (instProxy == null) { // no instance Proxy is set
                /**
                 * Do we have to use a proxy?
                 */
                @SuppressWarnings("removal")
                ProxySelector sel =
                    java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction<>() {
                            public ProxySelector run() {
                                     return ProxySelector.getDefault();
                                 }
                             });
                if (sel != null) {
                    URI uri = sun.net.www.ParseUtil.toURI(url);
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("ProxySelector Request for " + uri);
                    }
                    final List<Proxy> proxies;
                    try {
                        proxies = sel.select(uri);
                    } catch (IllegalArgumentException iae) {
                        throw new IOException("Failed to select a proxy", iae);
                    }
                    final Iterator<Proxy> it = proxies.iterator();
                    Proxy p;
                    while (it.hasNext()) {
                        p = it.next();
                        try {
                            if (!failedOnce) {
                                http = getNewHttpClient(url, p, connectTimeout);
                                http.setReadTimeout(readTimeout);
                            } else {
                                // make sure to construct new connection if first
                                // attempt failed
                                http = getNewHttpClient(url, p, connectTimeout, false);
                                http.setReadTimeout(readTimeout);
                            }
                            if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                                if (p != null) {
                                    logger.finest("Proxy used: " + p.toString());
                                }
                            }
                            break;
                        } catch (IOException ioex) {
                            if (p != Proxy.NO_PROXY) {
                                sel.connectFailed(uri, p.address(), ioex);
                                if (!it.hasNext()) {
                                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                                        logger.finest("Retrying with proxy: " + p.toString());
                                    }
                                    http = getNewHttpClient(url, p, connectTimeout, false);
                                    http.setReadTimeout(readTimeout);
                                    break;
                                }
                            } else {
                                throw ioex;
                            }
                            continue;
                        }
                    }
                } else {
                    // No proxy selector, create http client with no proxy
                    if (!failedOnce) {
                        http = getNewHttpClient(url, null, connectTimeout);
                        http.setReadTimeout(readTimeout);
                    } else {
                        // make sure to construct new connection if first
                        // attempt failed
                        http = getNewHttpClient(url, null, connectTimeout, false);
                        http.setReadTimeout(readTimeout);
                    }
                }
            } else {
                if (!failedOnce) {
                    http = getNewHttpClient(url, instProxy, connectTimeout);
                    http.setReadTimeout(readTimeout);
                } else {
                    // make sure to construct new connection if first
                    // attempt failed
                    http = getNewHttpClient(url, instProxy, connectTimeout, false);
                    http.setReadTimeout(readTimeout);
                }
            }

            usingProxy = usingProxy || usingProxyInternal();
            ps = (PrintStream)http.getOutputStream();
        } catch (IOException e) {
            throw e;
        }
        // constructor to HTTP client calls openserver
        connected = true;
    }

    // subclass HttpsClient will overwrite & return an instance of HttpsClient
    protected HttpClient getNewHttpClient(URL url, Proxy p, int connectTimeout)
        throws IOException {
        return HttpClient.New(url, p, connectTimeout, this);
    }

    // subclass HttpsClient will overwrite & return an instance of HttpsClient
    protected HttpClient getNewHttpClient(URL url, Proxy p,
                                          int connectTimeout, boolean useCache)
        throws IOException {
        return HttpClient.New(url, p, connectTimeout, useCache, this);
    }

    private void expect100Continue() throws IOException {
            // Expect: 100-Continue was set, so check the return code for
            // Acceptance
            int oldTimeout = http.getReadTimeout();
            boolean enforceTimeOut = false;
            boolean timedOut = false;
            if (oldTimeout <= 0) {
                // 5s read timeout in case the server doesn't understand
                // Expect: 100-Continue
                http.setReadTimeout(5000);
                enforceTimeOut = true;
            }

            try {
                http.parseHTTP(responses, pi, this);
            } catch (SocketTimeoutException se) {
                if (!enforceTimeOut) {
                    throw se;
                }
                timedOut = true;
                http.setIgnoreContinue(true);
            }
            if (!timedOut) {
                // Can't use getResponseCode() yet
                String resp = responses.getValue(0);
                // Parse the response which is of the form:
                // HTTP/1.1 417 Expectation Failed
                // HTTP/1.1 100 Continue
                if (resp != null && resp.startsWith("HTTP/")) {
                    String[] sa = resp.split("\\s+");
                    responseCode = -1;
                    try {
                        // Response code is 2nd token on the line
                        if (sa.length > 1)
                            responseCode = Integer.parseInt(sa[1]);
                    } catch (NumberFormatException numberFormatException) {
                    }
                }
                if (responseCode != 100) {
                    throw new ProtocolException("Server rejected operation");
                }
            }

            http.setReadTimeout(oldTimeout);

            responseCode = -1;
            responses.reset();
            // Proceed
    }

    /*
     * Allowable input/output sequences:
     * [interpreted as request entity]
     * - get output, [write output,] get input, [read input]
     * - get output, [write output]
     * [interpreted as GET]
     * - get input, [read input]
     * Disallowed:
     * - get input, [read input,] get output, [write output]
     */

    @SuppressWarnings("removal")
    @Override
    public OutputStream getOutputStream() throws IOException {
        lock();
        try {
            connecting = true;
            SocketPermission p = URLtoSocketPermission(this.url);

            if (p != null) {
                try {
                    return AccessController.doPrivilegedWithCombiner(
                            new PrivilegedExceptionAction<>() {
                                public OutputStream run() throws IOException {
                                    return getOutputStream0();
                                }
                            }, null, p
                    );
                } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
                }
            } else {
                return getOutputStream0();
            }
        } finally {
            unlock();
        }
    }

    private OutputStream getOutputStream0() throws IOException {
        assert isLockHeldByCurrentThread();
        try {
            if (!doOutput) {
                throw new ProtocolException("cannot write to a URLConnection"
                               + " if doOutput=false - call setDoOutput(true)");
            }

            if (method.equals("GET")) {
                method = "POST"; // Backward compatibility
            }
            if ("TRACE".equals(method) && "http".equals(url.getProtocol())) {
                throw new ProtocolException("HTTP method TRACE" +
                                            " doesn't support output");
            }

            // if there's already an input stream open, throw an exception
            if (inputStream != null) {
                throw new ProtocolException("Cannot write output after reading input.");
            }

            if (!checkReuseConnection())
                connect();

            boolean expectContinue = false;
            String expects = requests.findValue("Expect");
            if ("100-Continue".equalsIgnoreCase(expects) && streaming()) {
                http.setIgnoreContinue(false);
                expectContinue = true;
            }

            if (streaming() && strOutputStream == null) {
                writeRequests();
            }

            if (expectContinue) {
                expect100Continue();
            }
            ps = (PrintStream)http.getOutputStream();
            if (streaming()) {
                if (strOutputStream == null) {
                    if (chunkLength != -1) { /* chunked */
                         strOutputStream = new StreamingOutputStream(
                               new ChunkedOutputStream(ps, chunkLength), -1L);
                    } else { /* must be fixed content length */
                        long length = 0L;
                        if (fixedContentLengthLong != -1) {
                            length = fixedContentLengthLong;
                        } else if (fixedContentLength != -1) {
                            length = fixedContentLength;
                        }
                        strOutputStream = new StreamingOutputStream(ps, length);
                    }
                }
                return strOutputStream;
            } else {
                if (poster == null) {
                    poster = new PosterOutputStream();
                }
                return poster;
            }
        } catch (RuntimeException e) {
            disconnectInternal();
            throw e;
        } catch (ProtocolException e) {
            // Save the response code which may have been set while enforcing
            // the 100-continue. disconnectInternal() forces it to -1
            int i = responseCode;
            disconnectInternal();
            responseCode = i;
            throw e;
        } catch (IOException e) {
            disconnectInternal();
            throw e;
        }
    }

    public boolean streaming () {
        return (fixedContentLength != -1) || (fixedContentLengthLong != -1) ||
               (chunkLength != -1);
    }

    /*
     * get applicable cookies based on the uri and request headers
     * add them to the existing request headers
     */
    private void setCookieHeader() throws IOException {
        if (cookieHandler != null) {
            // we only want to capture the user defined Cookies once, as
            // they cannot be changed by user code after we are connected,
            // only internally.

            // we should only reach here when called from
            // writeRequest, which in turn is only called by
            // getInputStream0
            assert isLockHeldByCurrentThread();
            if (setUserCookies) {
                int k = requests.getKey("Cookie");
                if (k != -1)
                    userCookies = requests.getValue(k);
                k = requests.getKey("Cookie2");
                if (k != -1)
                    userCookies2 = requests.getValue(k);
                setUserCookies = false;
            }

            // remove old Cookie header before setting new one.
            requests.remove("Cookie");
            requests.remove("Cookie2");

            URI uri = ParseUtil.toURI(url);
            if (uri != null) {
                if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                    logger.finest("CookieHandler request for " + uri);
                }
                Map<String, List<String>> cookies
                    = cookieHandler.get(
                        uri, requests.getHeaders(EXCLUDE_HEADERS));
                if (!cookies.isEmpty()) {
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Cookies retrieved: " + cookies.toString());
                    }
                    for (Map.Entry<String, List<String>> entry :
                             cookies.entrySet()) {
                        String key = entry.getKey();
                        // ignore all entries that don't have "Cookie"
                        // or "Cookie2" as keys
                        if (!"Cookie".equalsIgnoreCase(key) &&
                            !"Cookie2".equalsIgnoreCase(key)) {
                            continue;
                        }
                        List<String> l = entry.getValue();
                        if (l != null && !l.isEmpty()) {
                            StringJoiner cookieValue = new StringJoiner("; ");
                            for (String value : l) {
                                cookieValue.add(value);
                            }
                            requests.add(key, cookieValue.toString());
                        }
                    }
                }
            }
            if (userCookies != null) {
                int k;
                if ((k = requests.getKey("Cookie")) != -1)
                    requests.set("Cookie", requests.getValue(k) + ";" + userCookies);
                else
                    requests.set("Cookie", userCookies);
            }
            if (userCookies2 != null) {
                int k;
                if ((k = requests.getKey("Cookie2")) != -1)
                    requests.set("Cookie2", requests.getValue(k) + ";" + userCookies2);
                else
                    requests.set("Cookie2", userCookies2);
            }

        } // end of getting cookies
    }

    @SuppressWarnings("removal")
    @Override
    public InputStream getInputStream() throws IOException {
        lock();
        try {
            connecting = true;
            SocketPermission p = URLtoSocketPermission(this.url);

            if (p != null) {
                try {
                    return AccessController.doPrivilegedWithCombiner(
                            new PrivilegedExceptionAction<>() {
                                public InputStream run() throws IOException {
                                    return getInputStream0();
                                }
                            }, null, p
                    );
                } catch (PrivilegedActionException e) {
                    throw (IOException) e.getException();
                }
            } else {
                return getInputStream0();
            }
        } finally {
            unlock();
        }
    }

    @SuppressWarnings("empty-statement")
    private InputStream getInputStream0() throws IOException {

        assert isLockHeldByCurrentThread();
        if (!doInput) {
            throw new ProtocolException("Cannot read from URLConnection"
                   + " if doInput=false (call setDoInput(true))");
        }

        if (rememberedException != null) {
            if (rememberedException instanceof RuntimeException)
                throw new RuntimeException(rememberedException);
            else {
                throw getChainedException((IOException)rememberedException);
            }
        }

        if (inputStream != null) {
            return inputStream;
        }

        if (streaming() ) {
            if (strOutputStream == null) {
                getOutputStream();
            }
            /* make sure stream is closed */
            strOutputStream.close ();
            if (!strOutputStream.writtenOK()) {
                throw new IOException ("Incomplete output stream");
            }
        }

        int redirects = 0;
        int respCode = 0;
        long cl = -1;
        AuthenticationInfo serverAuthentication = null;
        AuthenticationInfo proxyAuthentication = null;
        AuthenticationHeader srvHdr = null;

        /**
         * Failed Negotiate
         *
         * In some cases, the Negotiate auth is supported for the
         * remote host but the negotiate process still fails (For
         * example, if the web page is located on a backend server
         * and delegation is needed but fails). The authentication
         * process will start again, and we need to detect this
         * kind of failure and do proper fallback (say, to NTLM).
         *
         * In order to achieve this, the inNegotiate flag is set
         * when the first negotiate challenge is met (and reset
         * if authentication is finished). If a fresh new negotiate
         * challenge (no parameter) is found while inNegotiate is
         * set, we know there's a failed auth attempt recently.
         * Here we'll ignore the header line so that fallback
         * can be practiced.
         *
         * inNegotiateProxy is for proxy authentication.
         */
        boolean inNegotiate = false;
        boolean inNegotiateProxy = false;

        // If the user has set either of these headers then do not remove them
        isUserServerAuth = requests.getKey("Authorization") != -1;
        isUserProxyAuth = requests.getKey("Proxy-Authorization") != -1;

        try {
            do {
                if (!checkReuseConnection())
                    connect();

                if (cachedInputStream != null) {
                    return cachedInputStream;
                }

                // Check if URL should be metered
                boolean meteredInput = ProgressMonitor.getDefault().shouldMeterInput(url, method);

                if (meteredInput)   {
                    pi = new ProgressSource(url, method);
                    pi.beginTracking();
                }

                /* REMIND: This exists to fix the HttpsURLConnection subclass.
                 * Hotjava needs to run on JDK1.1FCS.  Do proper fix once a
                 * proper solution for SSL can be found.
                 */
                ps = (PrintStream)http.getOutputStream();

                if (!streaming()) {
                    writeRequests();
                }
                http.parseHTTP(responses, pi, this);
                if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                    logger.fine(responses.toString());
                }

                boolean b1 = responses.filterNTLMResponses("WWW-Authenticate");
                boolean b2 = responses.filterNTLMResponses("Proxy-Authenticate");
                if (b1 || b2) {
                    if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                        logger.fine(">>>> Headers are filtered");
                        logger.fine(responses.toString());
                    }
                }

                inputStream = http.getInputStream();

                respCode = getResponseCode();
                if (respCode == -1) {
                    disconnectInternal();
                    throw new IOException ("Invalid Http response");
                }
                if (respCode == HTTP_PROXY_AUTH) {
                    if (streaming()) {
                        disconnectInternal();
                        throw new HttpRetryException (
                            RETRY_MSG1, HTTP_PROXY_AUTH);
                    }

                    // Read comments labeled "Failed Negotiate" for details.
                    boolean dontUseNegotiate = false;
                    Iterator<String> iter = responses.multiValueIterator("Proxy-Authenticate");
                    while (iter.hasNext()) {
                        String value = iter.next().trim();
                        if (value.equalsIgnoreCase("Negotiate") ||
                                value.equalsIgnoreCase("Kerberos")) {
                            if (!inNegotiateProxy) {
                                inNegotiateProxy = true;
                            } else {
                                dontUseNegotiate = true;
                                doingNTLMp2ndStage = false;
                                proxyAuthentication = null;
                            }
                            break;
                        }
                    }

                    // changes: add a 3rd parameter to the constructor of
                    // AuthenticationHeader, so that NegotiateAuthentication.
                    // isSupported can be tested.
                    // The other 2 appearances of "new AuthenticationHeader" is
                    // altered in similar ways.

                    AuthenticationHeader authhdr = new AuthenticationHeader (
                            "Proxy-Authenticate",
                            responses,
                            new HttpCallerInfo(url,
                                               http.getProxyHostUsed(),
                                               http.getProxyPortUsed(),
                                               authenticator),
                            dontUseNegotiate,
                            disabledProxyingSchemes
                    );

                    if (!doingNTLMp2ndStage) {
                        proxyAuthentication =
                            resetProxyAuthentication(proxyAuthentication, authhdr);
                        if (proxyAuthentication != null) {
                            redirects++;
                            disconnectInternal();
                            continue;
                        }
                    } else {
                        /* in this case, only one header field will be present */
                        String raw = responses.findValue ("Proxy-Authenticate");
                        reset ();
                        if (!proxyAuthentication.setHeaders(this,
                                                        authhdr.headerParser(), raw)) {
                            disconnectInternal();
                            throw new IOException ("Authentication failure");
                        }
                        if (serverAuthentication != null && srvHdr != null &&
                                !serverAuthentication.setHeaders(this,
                                                        srvHdr.headerParser(), raw)) {
                            disconnectInternal ();
                            throw new IOException ("Authentication failure");
                        }
                        authObj = null;
                        doingNTLMp2ndStage = false;
                        continue;
                    }
                } else {
                    inNegotiateProxy = false;
                    doingNTLMp2ndStage = false;
                    if (!isUserProxyAuth)
                        requests.remove("Proxy-Authorization");
                }

                // cache proxy authentication info
                if (proxyAuthentication != null) {
                    // cache auth info on success, domain header not relevant.
                    proxyAuthentication.addToCache();
                }

                if (respCode == HTTP_UNAUTHORIZED) {
                    if (streaming()) {
                        disconnectInternal();
                        throw new HttpRetryException (
                            RETRY_MSG2, HTTP_UNAUTHORIZED);
                    }

                    // Read comments labeled "Failed Negotiate" for details.
                    boolean dontUseNegotiate = false;
                    Iterator<String> iter = responses.multiValueIterator("WWW-Authenticate");
                    while (iter.hasNext()) {
                        String value = iter.next().trim();
                        if (value.equalsIgnoreCase("Negotiate") ||
                                value.equalsIgnoreCase("Kerberos")) {
                            if (!inNegotiate) {
                                inNegotiate = true;
                            } else {
                                dontUseNegotiate = true;
                                doingNTLM2ndStage = false;
                                serverAuthentication = null;
                            }
                            break;
                        }
                    }

                    srvHdr = new AuthenticationHeader (
                            "WWW-Authenticate", responses,
                            new HttpCallerInfo(url, authenticator),
                            dontUseNegotiate
                    );

                    String raw = srvHdr.raw();
                    if (!doingNTLM2ndStage) {
                        if ((serverAuthentication != null)&&
                            serverAuthentication.getAuthScheme() != NTLM) {
                            if (serverAuthentication.isAuthorizationStale (raw)) {
                                /* we can retry with the current credentials */
                                disconnectWeb();
                                redirects++;
                                requests.set(serverAuthentication.getHeaderName(),
                                            serverAuthentication.getHeaderValue(url, method));
                                currentServerCredentials = serverAuthentication;
                                setCookieHeader();
                                continue;
                            } else {
                                serverAuthentication.removeFromCache();
                            }
                        }
                        serverAuthentication = getServerAuthentication(srvHdr);
                        currentServerCredentials = serverAuthentication;

                        if (serverAuthentication != null) {
                            disconnectWeb();
                            redirects++; // don't let things loop ad nauseum
                            setCookieHeader();
                            continue;
                        }
                    } else {
                        reset ();
                        /* header not used for ntlm */
                        if (!serverAuthentication.setHeaders(this, null, raw)) {
                            disconnectWeb();
                            throw new IOException ("Authentication failure");
                        }
                        doingNTLM2ndStage = false;
                        authObj = null;
                        setCookieHeader();
                        continue;
                    }
                }
                // cache server authentication info
                if (serverAuthentication != null) {
                    // cache auth info on success
                    if (!(serverAuthentication instanceof DigestAuthentication) ||
                        (domain == null)) {
                        if (serverAuthentication instanceof BasicAuthentication) {
                            // check if the path is shorter than the existing entry
                            String npath = AuthenticationInfo.reducePath (url.getPath());
                            String opath = serverAuthentication.path;
                            if (!opath.startsWith (npath) || npath.length() >= opath.length()) {
                                /* npath is longer, there must be a common root */
                                npath = BasicAuthentication.getRootPath (opath, npath);
                            }
                            // remove the entry and create a new one
                            BasicAuthentication a =
                                (BasicAuthentication) serverAuthentication.clone();
                            serverAuthentication.removeFromCache();
                            a.path = npath;
                            serverAuthentication = a;
                        }
                        serverAuthentication.addToCache();
                    } else {
                        // what we cache is based on the domain list in the request
                        DigestAuthentication srv = (DigestAuthentication)
                            serverAuthentication;
                        StringTokenizer tok = new StringTokenizer (domain," ");
                        String realm = srv.realm;
                        PasswordAuthentication pw = srv.pw;
                        digestparams = srv.params;
                        while (tok.hasMoreTokens()) {
                            String path = tok.nextToken();
                            try {
                                /* path could be an abs_path or a complete URI */
                                URL u = new URL (url, path);
                                DigestAuthentication d = new DigestAuthentication (
                                                   false, u, realm, "Digest", pw,
                                                   digestparams, srv.authenticatorKey);
                                d.addToCache ();
                            } catch (Exception e) {}
                        }
                    }
                }

                // some flags should be reset to its initialized form so that
                // even after a redirect the necessary checks can still be
                // preformed.
                inNegotiate = false;
                inNegotiateProxy = false;

                //serverAuthentication = null;
                doingNTLMp2ndStage = false;
                doingNTLM2ndStage = false;
                if (!isUserServerAuth)
                    requests.remove("Authorization");
                if (!isUserProxyAuth)
                    requests.remove("Proxy-Authorization");

                if (respCode == HTTP_OK) {
                    checkResponseCredentials (false);
                } else {
                    needToCheck = false;
                }

                // a flag need to clean
                needToCheck = true;

                if (followRedirect()) {
                    /* if we should follow a redirect, then the followRedirects()
                     * method will disconnect() and re-connect us to the new
                     * location
                     */
                    redirects++;

                    // redirecting HTTP response may have set cookie, so
                    // need to re-generate request header
                    setCookieHeader();

                    continue;
                }

                try {
                    cl = Long.parseLong(responses.findValue("content-length"));
                } catch (Exception exc) { };

                if (method.equals("HEAD") || cl == 0 ||
                    respCode == HTTP_NOT_MODIFIED ||
                    respCode == HTTP_NO_CONTENT) {

                    if (pi != null) {
                        pi.finishTracking();
                        pi = null;
                    }
                    http.finished();
                    http = null;
                    inputStream = new EmptyInputStream();
                    connected = false;
                }

                if (respCode == 200 || respCode == 203 || respCode == 206 ||
                    respCode == 300 || respCode == 301 || respCode == 410) {
                    if (cacheHandler != null && getUseCaches()) {
                        // give cache a chance to save response in cache
                        URI uri = ParseUtil.toURI(url);
                        if (uri != null) {
                            URLConnection uconn = this;
                            if ("https".equalsIgnoreCase(uri.getScheme())) {
                                try {
                                // use reflection to get to the public
                                // HttpsURLConnection instance saved in
                                // DelegateHttpsURLConnection
                                uconn = (URLConnection)this.getClass().getField("httpsURLConnection").get(this);
                                } catch (IllegalAccessException |
                                         NoSuchFieldException e) {
                                    // ignored; use 'this'
                                }
                            }
                            CacheRequest cacheRequest =
                                cacheHandler.put(uri, uconn);
                            if (cacheRequest != null && http != null) {
                                http.setCacheRequest(cacheRequest);
                                inputStream = new HttpInputStream(inputStream, cacheRequest);
                            }
                        }
                    }
                }

                if (!(inputStream instanceof HttpInputStream)) {
                    inputStream = new HttpInputStream(inputStream);
                }

                if (respCode >= 400) {
                    if (respCode == 404 || respCode == 410) {
                        throw new FileNotFoundException(url.toString());
                    } else {
                        throw new java.io.IOException("Server returned HTTP" +
                              " response code: " + respCode + " for URL: " +
                              url.toString());
                    }
                }
                poster = null;
                strOutputStream = null;
                return inputStream;
            } while (redirects < maxRedirects);

            throw new ProtocolException("Server redirected too many " +
                                        " times ("+ redirects + ")");
        } catch (RuntimeException e) {
            disconnectInternal();
            rememberedException = e;
            throw e;
        } catch (IOException e) {
            rememberedException = e;

            // buffer the error stream if bytes < 4k
            // and it can be buffered within 1 second
            String te = responses.findValue("Transfer-Encoding");
            if (http != null && http.isKeepingAlive() && enableESBuffer &&
                (cl > 0 || (te != null && te.equalsIgnoreCase("chunked")))) {
                errorStream = ErrorStream.getErrorStream(inputStream, cl, http);
            }
            throw e;
        } finally {
            if (proxyAuthKey != null) {
                AuthenticationInfo.endAuthRequest(proxyAuthKey);
            }
            if (serverAuthKey != null) {
                AuthenticationInfo.endAuthRequest(serverAuthKey);
            }
        }
    }

    /*
     * Creates a chained exception that has the same type as
     * original exception and with the same message. Right now,
     * there is no convenient APIs for doing so.
     */
    private IOException getChainedException(final IOException rememberedException) {
        try {
            final Object[] args = { rememberedException.getMessage() };
            @SuppressWarnings("removal")
            IOException chainedException =
                java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction<>() {
                        public IOException run() throws Exception {
                            return (IOException)
                                rememberedException.getClass()
                                .getConstructor(new Class<?>[] { String.class })
                                .newInstance(args);
                        }
                    });
            chainedException.initCause(rememberedException);
            return chainedException;
        } catch (Exception ignored) {
            return rememberedException;
        }
    }

    @Override
    public InputStream getErrorStream() {
        if (connected && responseCode >= 400) {
            // Client Error 4xx and Server Error 5xx
            if (errorStream != null) {
                return errorStream;
            } else if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    /**
     * set or reset proxy authentication info in request headers
     * after receiving a 407 error. In the case of NTLM however,
     * receiving a 407 is normal and we just skip the stale check
     * because ntlm does not support this feature.
     */
    private AuthenticationInfo
        resetProxyAuthentication(AuthenticationInfo proxyAuthentication, AuthenticationHeader auth) throws IOException {

        // Only called from getInputStream0 and doTunneling0
        assert isLockHeldByCurrentThread();

        if ((proxyAuthentication != null )&&
             proxyAuthentication.getAuthScheme() != NTLM) {
            String raw = auth.raw();
            if (proxyAuthentication.isAuthorizationStale (raw)) {
                /* we can retry with the current credentials */
                String value;
                if (proxyAuthentication instanceof DigestAuthentication) {
                    DigestAuthentication digestProxy = (DigestAuthentication)
                        proxyAuthentication;
                    if (tunnelState() == TunnelState.SETUP) {
                        value = digestProxy.getHeaderValue(connectRequestURI(url), HTTP_CONNECT);
                    } else {
                        value = digestProxy.getHeaderValue(getRequestURI(), method);
                    }
                } else {
                    value = proxyAuthentication.getHeaderValue(url, method);
                }
                requests.set(proxyAuthentication.getHeaderName(), value);
                currentProxyCredentials = proxyAuthentication;
                return proxyAuthentication;
            } else {
                proxyAuthentication.removeFromCache();
            }
        }
        proxyAuthentication = getHttpProxyAuthentication(auth);
        currentProxyCredentials = proxyAuthentication;
        return  proxyAuthentication;
    }

    /**
     * Returns the tunnel state.
     *
     * @return  the state
     */
    TunnelState tunnelState() {
        return tunnelState;
    }

    /**
     * Set the tunneling status.
     *
     * @param tunnelState the state
     */
    public void setTunnelState(TunnelState tunnelState) {
        this.tunnelState = tunnelState;
    }

    /**
     * establish a tunnel through proxy server
     */
    public void doTunneling() throws IOException {
        lock();
        try {
            doTunneling0();
        } finally{
            unlock();
        }
    }

    private void doTunneling0() throws IOException {
        int retryTunnel = 0;
        String statusLine = "";
        int respCode = 0;
        AuthenticationInfo proxyAuthentication = null;
        String proxyHost = null;
        int proxyPort = -1;

        assert isLockHeldByCurrentThread();

        // save current requests so that they can be restored after tunnel is setup.
        MessageHeader savedRequests = requests;
        requests = new MessageHeader();

        // Read comments labeled "Failed Negotiate" for details.
        boolean inNegotiateProxy = false;

        try {
            /* Actively setting up a tunnel */
            setTunnelState(TunnelState.SETUP);

            do {
                if (!checkReuseConnection()) {
                    proxiedConnect(url, proxyHost, proxyPort, false);
                }
                // send the "CONNECT" request to establish a tunnel
                // through proxy server
                sendCONNECTRequest();
                responses.reset();

                // There is no need to track progress in HTTP Tunneling,
                // so ProgressSource is null.
                http.parseHTTP(responses, null, this);

                /* Log the response to the CONNECT */
                if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                    logger.fine(responses.toString());
                }

                if (responses.filterNTLMResponses("Proxy-Authenticate")) {
                    if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                        logger.fine(">>>> Headers are filtered");
                        logger.fine(responses.toString());
                    }
                }

                statusLine = responses.getValue(0);
                StringTokenizer st = new StringTokenizer(statusLine);
                st.nextToken();
                respCode = Integer.parseInt(st.nextToken().trim());
                if (respCode == HTTP_PROXY_AUTH) {
                    // Read comments labeled "Failed Negotiate" for details.
                    boolean dontUseNegotiate = false;
                    Iterator<String> iter = responses.multiValueIterator("Proxy-Authenticate");
                    while (iter.hasNext()) {
                        String value = iter.next().trim();
                        if (value.equalsIgnoreCase("Negotiate") ||
                                value.equalsIgnoreCase("Kerberos")) {
                            if (!inNegotiateProxy) {
                                inNegotiateProxy = true;
                            } else {
                                dontUseNegotiate = true;
                                doingNTLMp2ndStage = false;
                                proxyAuthentication = null;
                            }
                            break;
                        }
                    }

                    AuthenticationHeader authhdr = new AuthenticationHeader(
                            "Proxy-Authenticate",
                            responses,
                            new HttpCallerInfo(url,
                                               http.getProxyHostUsed(),
                                               http.getProxyPortUsed(),
                                               authenticator),
                            dontUseNegotiate,
                            disabledTunnelingSchemes
                    );
                    if (!doingNTLMp2ndStage) {
                        proxyAuthentication =
                            resetProxyAuthentication(proxyAuthentication, authhdr);
                        if (proxyAuthentication != null) {
                            proxyHost = http.getProxyHostUsed();
                            proxyPort = http.getProxyPortUsed();
                            disconnectInternal();
                            retryTunnel++;
                            continue;
                        }
                    } else {
                        String raw = responses.findValue ("Proxy-Authenticate");
                        reset ();
                        if (!proxyAuthentication.setHeaders(this,
                                                authhdr.headerParser(), raw)) {
                            disconnectInternal();
                            throw new IOException ("Authentication failure");
                        }
                        authObj = null;
                        doingNTLMp2ndStage = false;
                        continue;
                    }
                }
                // cache proxy authentication info
                if (proxyAuthentication != null) {
                    // cache auth info on success, domain header not relevant.
                    proxyAuthentication.addToCache();
                }

                if (respCode == HTTP_OK) {
                    setTunnelState(TunnelState.TUNNELING);
                    break;
                }
                // we don't know how to deal with other response code
                // so disconnect and report error
                disconnectInternal();
                setTunnelState(TunnelState.NONE);
                break;
            } while (retryTunnel < maxRedirects);

            if (retryTunnel >= maxRedirects || (respCode != HTTP_OK)) {
                if (respCode != HTTP_PROXY_AUTH) {
                    // remove all but authenticate responses
                    responses.reset();
                }
                throw new IOException("Unable to tunnel through proxy."+
                                      " Proxy returns \"" +
                                      statusLine + "\"");
            }
        } finally  {
            if (proxyAuthKey != null) {
                AuthenticationInfo.endAuthRequest(proxyAuthKey);
            }
        }

        // restore original request headers
        requests = savedRequests;

        // reset responses
        responses.reset();
    }

    static String connectRequestURI(URL url) {
        String host = url.getHost();
        int port = url.getPort();
        port = port != -1 ? port : url.getDefaultPort();

        return host + ":" + port;
    }

    /**
     * send a CONNECT request for establishing a tunnel to proxy server
     */
    private void sendCONNECTRequest() throws IOException {
        int port = url.getPort();

        requests.set(0, HTTP_CONNECT + " " + connectRequestURI(url)
                         + " " + httpVersion, null);
        requests.setIfNotSet("User-Agent", userAgent);

        String host = url.getHost();
        if (port != -1 && port != url.getDefaultPort()) {
            host += ":" + String.valueOf(port);
        }
        requests.setIfNotSet("Host", host);

        // Not really necessary for a tunnel, but can't hurt
        requests.setIfNotSet("Accept", acceptString);

        if (http.getHttpKeepAliveSet()) {
            requests.setIfNotSet("Proxy-Connection", "keep-alive");
        }

        setPreemptiveProxyAuthentication(requests);

         /* Log the CONNECT request */
        if (logger.isLoggable(PlatformLogger.Level.FINE)) {
            logger.fine(requests.toString());
        }

        http.writeRequests(requests, null);
    }

    /**
     * Sets pre-emptive proxy authentication in header
     */
    private void setPreemptiveProxyAuthentication(MessageHeader requests) throws IOException {
        AuthenticationInfo pauth
            = AuthenticationInfo.getProxyAuth(http.getProxyHostUsed(),
                                              http.getProxyPortUsed(),
                                              getAuthenticatorKey());
        if (pauth != null && pauth.supportsPreemptiveAuthorization()) {
            String value;
            if (pauth instanceof DigestAuthentication) {
                DigestAuthentication digestProxy = (DigestAuthentication) pauth;
                if (tunnelState() == TunnelState.SETUP) {
                    value = digestProxy
                        .getHeaderValue(connectRequestURI(url), HTTP_CONNECT);
                } else {
                    value = digestProxy.getHeaderValue(getRequestURI(), method);
                }
            } else {
                value = pauth.getHeaderValue(url, method);
            }

            // Sets "Proxy-authorization"
            requests.set(pauth.getHeaderName(), value);
            currentProxyCredentials = pauth;
        }
    }

    /**
     * Gets the authentication for an HTTP proxy, and applies it to
     * the connection.
     */
    @SuppressWarnings({"removal","fallthrough"})
    private AuthenticationInfo getHttpProxyAuthentication(AuthenticationHeader authhdr) {

        assert isLockHeldByCurrentThread();

        /* get authorization from authenticator */
        AuthenticationInfo ret = null;
        String raw = authhdr.raw();
        String host = http.getProxyHostUsed();
        int port = http.getProxyPortUsed();
        if (host != null && authhdr.isPresent()) {
            HeaderParser p = authhdr.headerParser();
            String realm = p.findValue("realm");
            String charset = p.findValue("charset");
            boolean isUTF8 = (charset != null && charset.equalsIgnoreCase("UTF-8"));
            String scheme = authhdr.scheme();
            AuthScheme authScheme = UNKNOWN;
            if ("basic".equalsIgnoreCase(scheme)) {
                authScheme = BASIC;
            } else if ("digest".equalsIgnoreCase(scheme)) {
                authScheme = DIGEST;
            } else if ("ntlm".equalsIgnoreCase(scheme)) {
                authScheme = NTLM;
                doingNTLMp2ndStage = true;
            } else if ("Kerberos".equalsIgnoreCase(scheme)) {
                authScheme = KERBEROS;
                doingNTLMp2ndStage = true;
            } else if ("Negotiate".equalsIgnoreCase(scheme)) {
                authScheme = NEGOTIATE;
                doingNTLMp2ndStage = true;
            }

            if (realm == null)
                realm = "";
            proxyAuthKey = AuthenticationInfo.getProxyAuthKey(host, port, realm,
                                authScheme, getAuthenticatorKey());
            ret = AuthenticationInfo.getProxyAuth(proxyAuthKey);
            if (ret == null) {
                switch (authScheme) {
                case BASIC:
                    InetAddress addr = null;
                    try {
                        final String finalHost = host;
                        addr = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedExceptionAction<>() {
                                public InetAddress run()
                                    throws java.net.UnknownHostException {
                                    return InetAddress.getByName(finalHost);
                                }
                            });
                    } catch (java.security.PrivilegedActionException ignored) {
                        // User will have an unknown host.
                    }
                    PasswordAuthentication a =
                        privilegedRequestPasswordAuthentication(
                                    authenticator,
                                    host, addr, port, "http",
                                    realm, scheme, url, RequestorType.PROXY);
                    if (a != null) {
                        ret = new BasicAuthentication(true, host, port, realm, a,
                                             isUTF8, getAuthenticatorKey());
                    }
                    break;
                case DIGEST:
                    a = privilegedRequestPasswordAuthentication(
                                    authenticator,
                                    host, null, port, url.getProtocol(),
                                    realm, scheme, url, RequestorType.PROXY);
                    if (a != null) {
                        DigestAuthentication.Parameters params =
                            new DigestAuthentication.Parameters();
                        ret = new DigestAuthentication(true, host, port, realm,
                                             scheme, a, params,
                                             getAuthenticatorKey());
                    }
                    break;
                case NTLM:
                    if (NTLMAuthenticationProxy.supported) {
                        /* tryTransparentNTLMProxy will always be true the first
                         * time around, but verify that the platform supports it
                         * otherwise don't try. */
                        if (tryTransparentNTLMProxy) {
                            tryTransparentNTLMProxy =
                                    NTLMAuthenticationProxy.supportsTransparentAuth;
                            /* If the platform supports transparent authentication
                             * then normally it's ok to do transparent auth to a proxy
                             * because we generally trust proxies (chosen by the user)
                             * But not in the case of 305 response where the server
                             * chose it. */
                            if (tryTransparentNTLMProxy && useProxyResponseCode) {
                                tryTransparentNTLMProxy = false;
                            }

                        }
                        a = null;
                        if (tryTransparentNTLMProxy) {
                            logger.finest("Trying Transparent NTLM authentication");
                        } else {
                            a = privilegedRequestPasswordAuthentication(
                                                authenticator,
                                                host, null, port, url.getProtocol(),
                                                "", scheme, url, RequestorType.PROXY);
                        }
                        /* If we are not trying transparent authentication then
                         * we need to have a PasswordAuthentication instance. For
                         * transparent authentication (Windows only) the username
                         * and password will be picked up from the current logged
                         * on users credentials.
                         */
                        if (tryTransparentNTLMProxy ||
                              (!tryTransparentNTLMProxy && a != null)) {
                            ret = NTLMAuthenticationProxy.proxy.create(true, host,
                                    port, a, getAuthenticatorKey());
                        }

                        /* set to false so that we do not try again */
                        tryTransparentNTLMProxy = false;
                    }
                    break;
                case NEGOTIATE:
                    ret = new NegotiateAuthentication(new HttpCallerInfo(authhdr.getHttpCallerInfo(), "Negotiate"));
                    break;
                case KERBEROS:
                    ret = new NegotiateAuthentication(new HttpCallerInfo(authhdr.getHttpCallerInfo(), "Kerberos"));
                    break;
                case UNKNOWN:
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Unknown/Unsupported authentication scheme: " + scheme);
                    }
                /*fall through*/
                default:
                    throw new AssertionError("should not reach here");
                }
            }
            // For backwards compatibility, we also try defaultAuth
            // REMIND:  Get rid of this for JDK2.0.

            if (ret == null && defaultAuth != null
                && defaultAuth.schemeSupported(scheme)) {
                try {
                    URL u = new URL("http", host, port, "/");
                    String a = defaultAuth.authString(u, scheme, realm);
                    if (a != null) {
                        ret = new BasicAuthentication (true, host, port, realm, a,
                                  getAuthenticatorKey());
                        // not in cache by default - cache on success
                    }
                } catch (java.net.MalformedURLException ignored) {
                }
            }
            if (ret != null) {
                if (!ret.setHeaders(this, p, raw)) {
                    ret = null;
                }
            }
        }
        if (logger.isLoggable(PlatformLogger.Level.FINER)) {
            logger.finer("Proxy Authentication for " + authhdr.toString() +" returned " + (ret != null ? ret.toString() : "null"));
        }
        return ret;
    }

    /**
     * Gets the authentication for an HTTP server, and applies it to
     * the connection.
     * @param authhdr the AuthenticationHeader which tells what auth scheme is
     * preferred.
     */
    @SuppressWarnings("fallthrough")
    private AuthenticationInfo getServerAuthentication(AuthenticationHeader authhdr) {

        // Only called from getInputStream0
        assert isLockHeldByCurrentThread();

        /* get authorization from authenticator */
        AuthenticationInfo ret = null;
        String raw = authhdr.raw();
        /* When we get an NTLM auth from cache, don't set any special headers */
        if (authhdr.isPresent()) {
            HeaderParser p = authhdr.headerParser();
            String realm = p.findValue("realm");
            String scheme = authhdr.scheme();
            String charset = p.findValue("charset");
            boolean isUTF8 = (charset != null && charset.equalsIgnoreCase("UTF-8"));
            AuthScheme authScheme = UNKNOWN;
            if ("basic".equalsIgnoreCase(scheme)) {
                authScheme = BASIC;
            } else if ("digest".equalsIgnoreCase(scheme)) {
                authScheme = DIGEST;
            } else if ("ntlm".equalsIgnoreCase(scheme)) {
                authScheme = NTLM;
                doingNTLM2ndStage = true;
            } else if ("Kerberos".equalsIgnoreCase(scheme)) {
                authScheme = KERBEROS;
                doingNTLM2ndStage = true;
            } else if ("Negotiate".equalsIgnoreCase(scheme)) {
                authScheme = NEGOTIATE;
                doingNTLM2ndStage = true;
            }

            domain = p.findValue ("domain");
            if (realm == null)
                realm = "";
            serverAuthKey = AuthenticationInfo.getServerAuthKey(url, realm, authScheme,
                                               getAuthenticatorKey());
            ret = AuthenticationInfo.getServerAuth(serverAuthKey);
            InetAddress addr = null;
            if (ret == null) {
                try {
                    addr = InetAddress.getByName(url.getHost());
                } catch (java.net.UnknownHostException ignored) {
                    // User will have addr = null
                }
            }
            // replacing -1 with default port for a protocol
            int port = url.getPort();
            if (port == -1) {
                port = url.getDefaultPort();
            }
            if (ret == null) {
                switch(authScheme) {
                case KERBEROS:
                    ret = new NegotiateAuthentication(new HttpCallerInfo(authhdr.getHttpCallerInfo(), "Kerberos"));
                    break;
                case NEGOTIATE:
                    ret = new NegotiateAuthentication(new HttpCallerInfo(authhdr.getHttpCallerInfo(), "Negotiate"));
                    break;
                case BASIC:
                    PasswordAuthentication a =
                        privilegedRequestPasswordAuthentication(
                            authenticator,
                            url.getHost(), addr, port, url.getProtocol(),
                            realm, scheme, url, RequestorType.SERVER);
                    if (a != null) {
                        ret = new BasicAuthentication(false, url, realm, a,
                                    isUTF8, getAuthenticatorKey());
                    }
                    break;
                case DIGEST:
                    a = privilegedRequestPasswordAuthentication(
                            authenticator,
                            url.getHost(), addr, port, url.getProtocol(),
                            realm, scheme, url, RequestorType.SERVER);
                    if (a != null) {
                        digestparams = new DigestAuthentication.Parameters();
                        ret = new DigestAuthentication(false, url, realm, scheme,
                                    a, digestparams,
                                    getAuthenticatorKey());
                    }
                    break;
                case NTLM:
                    if (NTLMAuthenticationProxy.supported) {
                        URL url1;
                        try {
                            url1 = new URL (url, "/"); /* truncate the path */
                        } catch (Exception e) {
                            url1 = url;
                        }

                        /* tryTransparentNTLMServer will always be true the first
                         * time around, but verify that the platform supports it
                         * otherwise don't try. */
                        if (tryTransparentNTLMServer) {
                            tryTransparentNTLMServer =
                                    NTLMAuthenticationProxy.supportsTransparentAuth;
                            /* If the platform supports transparent authentication
                             * then check if we are in a secure environment
                             * whether, or not, we should try transparent authentication.*/
                            if (tryTransparentNTLMServer) {
                                tryTransparentNTLMServer =
                                        NTLMAuthenticationProxy.isTrustedSite(url);
                            }
                        }
                        a = null;
                        if (tryTransparentNTLMServer) {
                            logger.finest("Trying Transparent NTLM authentication");
                        } else {
                            a = privilegedRequestPasswordAuthentication(
                                authenticator,
                                url.getHost(), addr, port, url.getProtocol(),
                                "", scheme, url, RequestorType.SERVER);
                        }

                        /* If we are not trying transparent authentication then
                         * we need to have a PasswordAuthentication instance. For
                         * transparent authentication (Windows only) the username
                         * and password will be picked up from the current logged
                         * on users credentials.
                         */
                        if (tryTransparentNTLMServer ||
                              (!tryTransparentNTLMServer && a != null)) {
                            ret = NTLMAuthenticationProxy.proxy.create(false,
                                     url1, a, getAuthenticatorKey());
                        }

                        /* set to false so that we do not try again */
                        tryTransparentNTLMServer = false;
                    }
                    break;
                case UNKNOWN:
                    if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                        logger.finest("Unknown/Unsupported authentication scheme: " + scheme);
                    }
                /*fall through*/
                default:
                    throw new AssertionError("should not reach here");
                }
            }

            // For backwards compatibility, we also try defaultAuth
            // REMIND:  Get rid of this for JDK2.0.

            if (ret == null && defaultAuth != null
                && defaultAuth.schemeSupported(scheme)) {
                String a = defaultAuth.authString(url, scheme, realm);
                if (a != null) {
                    ret = new BasicAuthentication (false, url, realm, a,
                                    getAuthenticatorKey());
                    // not in cache by default - cache on success
                }
            }

            if (ret != null ) {
                if (!ret.setHeaders(this, p, raw)) {
                    ret = null;
                }
            }
        }
        if (logger.isLoggable(PlatformLogger.Level.FINER)) {
            logger.finer("Server Authentication for " + authhdr.toString() +" returned " + (ret != null ? ret.toString() : "null"));
        }
        return ret;
    }

    /* inclose will be true if called from close(), in which case we
     * force the call to check because this is the last chance to do so.
     * If not in close(), then the authentication info could arrive in a trailer
     * field, which we have not read yet.
     */
    private void checkResponseCredentials (boolean inClose) throws IOException {
        try {
            if (!needToCheck)
                return;
            if ((validateProxy && currentProxyCredentials != null) &&
                (currentProxyCredentials instanceof DigestAuthentication)) {
                String raw = responses.findValue ("Proxy-Authentication-Info");
                if (inClose || (raw != null)) {
                    DigestAuthentication da = (DigestAuthentication)
                        currentProxyCredentials;
                    da.checkResponse (raw, method, getRequestURI());
                    currentProxyCredentials = null;
                }
            }
            if ((validateServer && currentServerCredentials != null) &&
                (currentServerCredentials instanceof DigestAuthentication)) {
                String raw = responses.findValue ("Authentication-Info");
                if (inClose || (raw != null)) {
                    DigestAuthentication da = (DigestAuthentication)
                        currentServerCredentials;
                    da.checkResponse (raw, method, url);
                    currentServerCredentials = null;
                }
            }
            if ((currentServerCredentials==null) && (currentProxyCredentials == null)) {
                needToCheck = false;
            }
        } catch (IOException e) {
            disconnectInternal();
            connected = false;
            throw e;
        }
    }

   /* The request URI used in the request line for this request.
    * Also, needed for digest authentication
    */

    String requestURI = null;

    String getRequestURI() throws IOException {
        if (requestURI == null) {
            requestURI = http.getURLFile();
        }
        return requestURI;
    }

    /* Tells us whether to follow a redirect.  If so, it
     * closes the connection (break any keep-alive) and
     * resets the url, re-connects, and resets the request
     * property.
     */
    @SuppressWarnings("removal")
    private boolean followRedirect() throws IOException {
        if (!getInstanceFollowRedirects()) {
            return false;
        }

        final int stat = getResponseCode();
        if (stat < 300 || stat > 307 || stat == 306
                                || stat == HTTP_NOT_MODIFIED) {
            return false;
        }
        final String loc = getHeaderField("Location");
        if (loc == null) {
            /* this should be present - if not, we have no choice
             * but to go forward w/ the response we got
             */
            return false;
        }

        URL locUrl;
        try {
            locUrl = new URL(loc);
            if (!url.getProtocol().equalsIgnoreCase(locUrl.getProtocol())) {
                return false;
            }

        } catch (MalformedURLException mue) {
          // treat loc as a relative URI to conform to popular browsers
          locUrl = new URL(url, loc);
        }

        final URL locUrl0 = locUrl;
        socketPermission = null; // force recalculation
        SocketPermission p = URLtoSocketPermission(locUrl);

        if (p != null) {
            try {
                return AccessController.doPrivilegedWithCombiner(
                    new PrivilegedExceptionAction<>() {
                        public Boolean run() throws IOException {
                            return followRedirect0(loc, stat, locUrl0);
                        }
                    }, null, p
                );
            } catch (PrivilegedActionException e) {
                throw (IOException) e.getException();
            }
        } else {
            // run without additional permission
            return followRedirect0(loc, stat, locUrl);
        }
    }

    /* Tells us whether to follow a redirect.  If so, it
     * closes the connection (break any keep-alive) and
     * resets the url, re-connects, and resets the request
     * property.
     */
    private boolean followRedirect0(String loc, int stat, URL locUrl)
        throws IOException
    {
        assert isLockHeldByCurrentThread();

        disconnectInternal();
        if (streaming()) {
            throw new HttpRetryException (RETRY_MSG3, stat, loc);
        }
        if (logger.isLoggable(PlatformLogger.Level.FINE)) {
            logger.fine("Redirected from " + url + " to " + locUrl);
        }

        // clear out old response headers!!!!
        responses = new MessageHeader();
        if (stat == HTTP_USE_PROXY) {
            /* This means we must re-request the resource through the
             * proxy denoted in the "Location:" field of the response.
             * Judging by the spec, the string in the Location header
             * _should_ denote a URL - let's hope for "http://my.proxy.org"
             * Make a new HttpClient to the proxy, using HttpClient's
             * Instance-specific proxy fields, but note we're still fetching
             * the same URL.
             */
            String proxyHost = locUrl.getHost();
            int proxyPort = locUrl.getPort();

            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(proxyHost, proxyPort);
            }

            setProxiedClient (url, proxyHost, proxyPort);
            requests.set(0, method + " " + getRequestURI()+" "  +
                             httpVersion, null);
            connected = true;
            // need to remember this in case NTLM proxy authentication gets
            // used. We can't use transparent authentication when user
            // doesn't know about proxy.
            useProxyResponseCode = true;
        } else {
            final URL prevURL = url;

            // maintain previous headers, just change the name
            // of the file we're getting
            url = locUrl;
            requestURI = null; // force it to be recalculated
            if (method.equals("POST") && !Boolean.getBoolean("http.strictPostRedirect") && (stat!=307)) {
                /* The HTTP/1.1 spec says that a redirect from a POST
                 * *should not* be immediately turned into a GET, and
                 * that some HTTP/1.0 clients incorrectly did this.
                 * Correct behavior redirects a POST to another POST.
                 * Unfortunately, since most browsers have this incorrect
                 * behavior, the web works this way now.  Typical usage
                 * seems to be:
                 *   POST a login code or passwd to a web page.
                 *   after validation, the server redirects to another
                 *     (welcome) page
                 *   The second request is (erroneously) expected to be GET
                 *
                 * We will do the incorrect thing (POST-->GET) by default.
                 * We will provide the capability to do the "right" thing
                 * (POST-->POST) by a system property, "http.strictPostRedirect=true"
                 */

                requests = new MessageHeader();
                setRequests = false;
                super.setRequestMethod("GET"); // avoid the connecting check
                poster = null;
                if (!checkReuseConnection())
                    connect();

                if (!sameDestination(prevURL, url)) {
                    // Ensures pre-redirect user-set cookie will not be reset.
                    // CookieHandler, if any, will be queried to determine
                    // cookies for redirected URL, if any.
                    userCookies = null;
                    userCookies2 = null;
                }
            } else {
                if (!checkReuseConnection())
                    connect();
                /* Even after a connect() call, http variable still can be
                 * null, if a ResponseCache has been installed and it returns
                 * a non-null CacheResponse instance. So check nullity before using it.
                 *
                 * And further, if http is null, there's no need to do anything
                 * about request headers because successive http session will use
                 * cachedInputStream/cachedHeaders anyway, which is returned by
                 * CacheResponse.
                 */
                if (http != null) {
                    requests.set(0, method + " " + getRequestURI()+" "  +
                                 httpVersion, null);
                    int port = url.getPort();
                    String host = stripIPv6ZoneId(url.getHost());
                    if (port != -1 && port != url.getDefaultPort()) {
                        host += ":" + String.valueOf(port);
                    }
                    requests.set("Host", host);
                }

                if (!sameDestination(prevURL, url)) {
                    // Redirecting to a different destination will drop any
                    // security-sensitive headers, regardless of whether
                    // they are user-set or not. CookieHandler, if any, will be
                    // queried to determine cookies for redirected URL, if any.
                    userCookies = null;
                    userCookies2 = null;
                    requests.remove("Cookie");
                    requests.remove("Cookie2");
                    requests.remove("Authorization");

                    // check for preemptive authorization
                    AuthenticationInfo sauth =
                            AuthenticationInfo.getServerAuth(url, getAuthenticatorKey());
                    if (sauth != null && sauth.supportsPreemptiveAuthorization() ) {
                        // Sets "Authorization"
                        requests.setIfNotSet(sauth.getHeaderName(), sauth.getHeaderValue(url,method));
                        currentServerCredentials = sauth;
                    }
                }
            }
        }
        return true;
    }

    /* Returns true iff the given URLs have the same host and effective port. */
    private static boolean sameDestination(URL firstURL, URL secondURL) {
        assert firstURL.getProtocol().equalsIgnoreCase(secondURL.getProtocol()):
               "protocols not equal: " + firstURL +  " - " + secondURL;

        if (!firstURL.getHost().equalsIgnoreCase(secondURL.getHost()))
            return false;

        int firstPort = firstURL.getPort();
        if (firstPort == -1)
            firstPort = firstURL.getDefaultPort();
        int secondPort = secondURL.getPort();
        if (secondPort == -1)
            secondPort = secondURL.getDefaultPort();
        if (firstPort != secondPort)
            return false;

        return true;
    }

    /* dummy byte buffer for reading off socket prior to closing */
    byte[] cdata = new byte [128];

    /**
     * Reset (without disconnecting the TCP conn) in order to do another transaction with this instance
     */
    private void reset() throws IOException {
        http.reuse = true;
        /* must save before calling close */
        reuseClient = http;
        InputStream is = http.getInputStream();
        if (!method.equals("HEAD")) {
            try {
                /* we want to read the rest of the response without using the
                 * hurry mechanism, because that would close the connection
                 * if everything is not available immediately
                 */
                if ((is instanceof ChunkedInputStream) ||
                    (is instanceof MeteredStream)) {
                    /* reading until eof will not block */
                    while (is.read (cdata) > 0) {}
                } else {
                    /* raw stream, which will block on read, so only read
                     * the expected number of bytes, probably 0
                     */
                    long cl = 0;
                    int n = 0;
                    String cls = responses.findValue ("Content-Length");
                    if (cls != null) {
                        try {
                            cl = Long.parseLong (cls);
                        } catch (NumberFormatException e) {
                            cl = 0;
                        }
                    }
                    for (long i=0; i<cl; ) {
                        if ((n = is.read (cdata)) == -1) {
                            break;
                        } else {
                            i+= n;
                        }
                    }
                }
            } catch (IOException e) {
                http.reuse = false;
                reuseClient = null;
                disconnectInternal();
                return;
            }
            try {
                if (is instanceof MeteredStream) {
                    is.close();
                }
            } catch (IOException e) { }
        }
        responseCode = -1;
        responses = new MessageHeader();
        connected = false;
    }

    /**
     * Disconnect from the web server at the first 401 error. Do not
     * disconnect when using a proxy, a good proxy should have already
     * closed the connection to the web server.
     */
    private void disconnectWeb() throws IOException {
        if (usingProxyInternal() && http.isKeepingAlive()) {
            responseCode = -1;
            // clean up, particularly, skip the content part
            // of a 401 error response
            reset();
        } else {
            disconnectInternal();
        }
    }

    /**
     * Disconnect from the server (for internal use)
     */
    private void disconnectInternal() {
        responseCode = -1;
        inputStream = null;
        if (pi != null) {
            pi.finishTracking();
            pi = null;
        }
        if (http != null) {
            http.closeServer();
            http = null;
            connected = false;
        }
    }

    /**
     * Disconnect from the server (public API)
     */
    public void disconnect() {

        responseCode = -1;
        if (pi != null) {
            pi.finishTracking();
            pi = null;
        }

        if (http != null) {
            /*
             * If we have an input stream this means we received a response
             * from the server. That stream may have been read to EOF and
             * depending on the stream type may already be closed or
             * the http client may be returned to the keep-alive cache.
             * If the http client has been returned to the keep-alive cache
             * it may be closed (idle timeout) or may be allocated to
             * another request.
             *
             * In other to avoid timing issues we close the input stream
             * which will either close the underlying connection or return
             * the client to the cache. If there's a possibility that the
             * client has been returned to the cache (ie: stream is a keep
             * alive stream or a chunked input stream) then we remove an
             * idle connection to the server. Note that this approach
             * can be considered an approximation in that we may close a
             * different idle connection to that used by the request.
             * Additionally it's possible that we close two connections
             * - the first becuase it wasn't an EOF (and couldn't be
             * hurried) - the second, another idle connection to the
             * same server. The is okay because "disconnect" is an
             * indication that the application doesn't intend to access
             * this http server for a while.
             */

            if (inputStream != null) {
                HttpClient hc = http;

                // un-synchronized
                boolean ka = hc.isKeepingAlive();

                try {
                    inputStream.close();
                } catch (IOException ioe) { }

                // if the connection is persistent it may have been closed
                // or returned to the keep-alive cache. If it's been returned
                // to the keep-alive cache then we would like to close it
                // but it may have been allocated

                if (ka) {
                    hc.closeIdleConnection();
                }


            } else {
                // We are deliberatly being disconnected so HttpClient
                // should not try to resend the request no matter what stage
                // of the connection we are in.
                http.setDoNotRetry(true);

                http.closeServer();
            }

            //      poster = null;
            http = null;
            connected = false;
        }
        cachedInputStream = null;
        if (cachedHeaders != null) {
            cachedHeaders.reset();
        }
    }

    /**
     * Returns true only if the established connection is using a proxy
     */
    boolean usingProxyInternal() {
        if (http != null) {
            return (http.getProxyHostUsed() != null);
        }
        return false;
    }

    /**
     * Returns true if the established connection is using a proxy
     * or if a proxy is specified for the inactive connection
     */
    @Override
    public boolean usingProxy() {
        if (usingProxy || usingProxyInternal())
            return true;

        if (instProxy != null)
            return instProxy.type().equals(Proxy.Type.HTTP);

        return false;
    }

    // constant strings represent set-cookie header names
    private static final String SET_COOKIE = "set-cookie";
    private static final String SET_COOKIE2 = "set-cookie2";

    /**
     * Returns a filtered version of the given headers value.
     *
     * Note: The implementation currently only filters out HttpOnly cookies
     *       from Set-Cookie and Set-Cookie2 headers.
     */
    private String filterHeaderField(String name, String value) {
        if (value == null)
            return null;

        if (SET_COOKIE.equalsIgnoreCase(name) ||
            SET_COOKIE2.equalsIgnoreCase(name)) {

            // Filtering only if there is a cookie handler. [Assumption: the
            // cookie handler will store/retrieve the HttpOnly cookies]
            if (cookieHandler == null || value.isEmpty())
                return value;

            JavaNetHttpCookieAccess access =
                    SharedSecrets.getJavaNetHttpCookieAccess();
            StringJoiner retValue = new StringJoiner(",");  // RFC 2965, comma separated
            List<HttpCookie> cookies = access.parse(value);
            for (HttpCookie cookie : cookies) {
                // skip HttpOnly cookies
                if (!cookie.isHttpOnly())
                    retValue.add(access.header(cookie));
            }
            return retValue.toString();
        }

        return value;
    }

    // Cache the filtered response headers so that they don't need
    // to be generated for every getHeaderFields() call.
    private Map<String, List<String>> filteredHeaders;  // null

    private Map<String, List<String>> getFilteredHeaderFields() {
        if (filteredHeaders != null)
            return filteredHeaders;

        Map<String, List<String>> headers, tmpMap = new HashMap<>();

        if (cachedHeaders != null)
            headers = cachedHeaders.getHeaders();
        else
            headers = responses.getHeaders();

        for (Map.Entry<String, List<String>> e: headers.entrySet()) {
            String key = e.getKey();
            List<String> values = e.getValue(), filteredVals = new ArrayList<>();
            for (String value : values) {
                String fVal = filterHeaderField(key, value);
                if (fVal != null)
                    filteredVals.add(fVal);
            }
            if (!filteredVals.isEmpty())
                tmpMap.put(key, Collections.unmodifiableList(filteredVals));
        }

        return filteredHeaders = Collections.unmodifiableMap(tmpMap);
    }

    /**
     * Gets a header field by name. Returns null if not known.
     * @param name the name of the header field
     */
    @Override
    public String getHeaderField(String name) {
        try {
            getInputStream();
        } catch (IOException e) {}

        if (cachedHeaders != null) {
            return filterHeaderField(name, cachedHeaders.findValue(name));
        }

        return filterHeaderField(name, responses.findValue(name));
    }

    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents
     * the corresponding field values.
     *
     * @return a Map of header fields
     * @since 1.4
     */
    @Override
    public Map<String, List<String>> getHeaderFields() {
        try {
            getInputStream();
        } catch (IOException e) {}

        return getFilteredHeaderFields();
    }

    /**
     * Gets a header field by index. Returns null if not known.
     * @param n the index of the header field
     */
    @Override
    public String getHeaderField(int n) {
        try {
            getInputStream();
        } catch (IOException e) {}

        if (cachedHeaders != null) {
           return filterHeaderField(cachedHeaders.getKey(n),
                                    cachedHeaders.getValue(n));
        }
        return filterHeaderField(responses.getKey(n), responses.getValue(n));
    }

    /**
     * Gets a header field by index. Returns null if not known.
     * @param n the index of the header field
     */
    @Override
    public String getHeaderFieldKey(int n) {
        try {
            getInputStream();
        } catch (IOException e) {}

        if (cachedHeaders != null) {
            return cachedHeaders.getKey(n);
        }

        return responses.getKey(n);
    }

    /**
     * Sets request property. If a property with the key already
     * exists, overwrite its value with the new value.
     * @param value the value to be set
     */
    @Override
    public void setRequestProperty(String key, String value) {
        lock();
        try {
            if (connected || connecting)
                throw new IllegalStateException("Already connected");
            if (key == null)
                throw new NullPointerException("key is null");

            if (isExternalMessageHeaderAllowed(key, value)) {
                requests.set(key, value);
                if (!key.equalsIgnoreCase("Content-Type")) {
                    userHeaders.set(key, value);
                }
            }
        } finally {
            unlock();
        }
    }

    MessageHeader getUserSetHeaders() {
        return userHeaders;
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param   key     the keyword by which the request is known
     *                  (e.g., "<code>accept</code>").
     * @param   value  the value associated with it.
     * @see #getRequestProperty(java.lang.String)
     * @since 1.4
     */
    @Override
    public void addRequestProperty(String key, String value) {
        lock();
        try {
            if (connected || connecting)
                throw new IllegalStateException("Already connected");
            if (key == null)
                throw new NullPointerException("key is null");

            if (isExternalMessageHeaderAllowed(key, value)) {
                requests.add(key, value);
                if (!key.equalsIgnoreCase("Content-Type")) {
                    userHeaders.add(key, value);
                }
            }
        } finally {
            unlock();
        }
    }

    //
    // Set a property for authentication.  This can safely disregard
    // the connected test.
    //
    public void setAuthenticationProperty(String key, String value) {
        // Only called by the implementation of AuthenticationInfo::setHeaders(...)
        // in AuthenticationInfo subclasses, which is only called from
        // methods from HttpURLConnection protected by the connectionLock.
        assert isLockHeldByCurrentThread();

        checkMessageHeader(key, value);
        requests.set(key, value);
    }

    @Override
    public String getRequestProperty (String key) {
        lock();
        try {
            if (key == null) {
                return null;
            }

            // don't return headers containing security sensitive information
            for (int i = 0; i < EXCLUDE_HEADERS.length; i++) {
                if (key.equalsIgnoreCase(EXCLUDE_HEADERS[i])) {
                    return null;
                }
            }
            if (!setUserCookies) {
                if (key.equalsIgnoreCase("Cookie")) {
                    return userCookies;
                }
                if (key.equalsIgnoreCase("Cookie2")) {
                    return userCookies2;
                }
            }
            return requests.findValue(key);
        } finally {
            unlock();
        }
    }

    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List
     * of Strings that represents the corresponding
     * field values.
     *
     * @return  a Map of the general request properties for this connection.
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    @Override
    public Map<String, List<String>> getRequestProperties() {
        lock();
        try {
            if (connected)
                throw new IllegalStateException("Already connected");

            // exclude headers containing security-sensitive info
            if (setUserCookies) {
                return requests.getHeaders(EXCLUDE_HEADERS);
            }
            /*
             * The cookies in the requests message headers may have
             * been modified. Use the saved user cookies instead.
             */
            Map<String, List<String>> userCookiesMap = null;
            if (userCookies != null || userCookies2 != null) {
                userCookiesMap = new HashMap<>();
                if (userCookies != null) {
                    userCookiesMap.put("Cookie", Arrays.asList(userCookies));
                }
                if (userCookies2 != null) {
                    userCookiesMap.put("Cookie2", Arrays.asList(userCookies2));
                }
            }
            return requests.filterAndAddHeaders(EXCLUDE_HEADERS2, userCookiesMap);
        } finally {
            unlock();
        }
    }

    @Override
    public void setConnectTimeout(int timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeouts can't be negative");
        connectTimeout = timeout;
    }


    /**
     * Returns setting for connect timeout.
     * <p>
     * 0 return implies that the option is disabled
     * (i.e., timeout of infinity).
     *
     * @return an <code>int</code> that indicates the connect timeout
     *         value in milliseconds
     * @see java.net.URLConnection#setConnectTimeout(int)
     * @see java.net.URLConnection#connect()
     * @since 1.5
     */
    @Override
    public int getConnectTimeout() {
        return (connectTimeout < 0 ? 0 : connectTimeout);
    }

    /**
     * Sets the read timeout to a specified timeout, in
     * milliseconds. A non-zero value specifies the timeout when
     * reading from Input stream when a connection is established to a
     * resource. If the timeout expires before there is data available
     * for read, a java.net.SocketTimeoutException is raised. A
     * timeout of zero is interpreted as an infinite timeout.
     *
     * <p> Some non-standard implementation of this method ignores the
     * specified timeout. To see the read timeout set, please call
     * getReadTimeout().
     *
     * @param timeout an <code>int</code> that specifies the timeout
     * value to be used in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     *
     * @see java.net.URLConnection#getReadTimeout()
     * @see java.io.InputStream#read()
     * @since 1.5
     */
    @Override
    public void setReadTimeout(int timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeouts can't be negative");
        readTimeout = timeout;
    }

    /**
     * Returns setting for read timeout. 0 return implies that the
     * option is disabled (i.e., timeout of infinity).
     *
     * @return an <code>int</code> that indicates the read timeout
     *         value in milliseconds
     *
     * @see java.net.URLConnection#setReadTimeout(int)
     * @see java.io.InputStream#read()
     * @since 1.5
     */
    @Override
    public int getReadTimeout() {
        return readTimeout < 0 ? 0 : readTimeout;
    }

    public CookieHandler getCookieHandler() {
        return cookieHandler;
    }

    String getMethod() {
        return method;
    }

    private MessageHeader mapToMessageHeader(Map<String, List<String>> map) {
        MessageHeader headers = new MessageHeader();
        if (map == null || map.isEmpty()) {
            return headers;
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                if (key == null) {
                    headers.prepend(key, value);
                } else {
                    headers.add(key, value);
                }
            }
        }
        return headers;
    }

    /**
     * Returns the given host, without the IPv6 Zone Id, if present.
     * (e.g. [fe80::a00:27ff:aaaa:aaaa%eth0] -> [fe80::a00:27ff:aaaa:aaaa])
     *
     * @param host host address (not null, not empty)
     * @return host address without Zone Id
     */
    static String stripIPv6ZoneId(String host) {
        if (host.charAt(0) != '[') { // not an IPv6-literal
            return host;
        }
        int i = host.lastIndexOf('%');
        if (i == -1) { // doesn't contain zone_id
            return host;
        }
        return host.substring(0, i) + "]";
    }

    /* The purpose of this wrapper is just to capture the close() call
     * so we can check authentication information that may have
     * arrived in a Trailer field
     */
    class HttpInputStream extends FilterInputStream {
        private CacheRequest cacheRequest;
        private OutputStream outputStream;
        private boolean marked = false;
        private int inCache = 0;
        private int markCount = 0;
        private boolean closed;  // false

        public HttpInputStream (InputStream is) {
            super (is);
            this.cacheRequest = null;
            this.outputStream = null;
        }

        public HttpInputStream (InputStream is, CacheRequest cacheRequest) {
            super (is);
            this.cacheRequest = cacheRequest;
            try {
                this.outputStream = cacheRequest.getBody();
            } catch (IOException ioex) {
                this.cacheRequest.abort();
                this.cacheRequest = null;
                this.outputStream = null;
            }
        }

        /**
         * Marks the current position in this input stream. A subsequent
         * call to the <code>reset</code> method repositions this stream at
         * the last marked position so that subsequent reads re-read the same
         * bytes.
         * <p>
         * The <code>readlimit</code> argument tells this input stream to
         * allow that many bytes to be read before the mark position gets
         * invalidated.
         * <p>
         * This method simply performs <code>in.mark(readlimit)</code>.
         *
         * @param   readlimit   the maximum limit of bytes that can be read before
         *                      the mark position becomes invalid.
         * @see     java.io.FilterInputStream#in
         * @see     java.io.FilterInputStream#reset()
         */
        // safe to use synchronized here: super method is synchronized too
        // and involves no blocking operation; only mark & reset are
        // synchronized in the super class hierarchy.
        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
            if (cacheRequest != null) {
                marked = true;
                markCount = 0;
            }
        }

        /**
         * Repositions this stream to the position at the time the
         * <code>mark</code> method was last called on this input stream.
         * <p>
         * This method
         * simply performs <code>in.reset()</code>.
         * <p>
         * Stream marks are intended to be used in
         * situations where you need to read ahead a little to see what's in
         * the stream. Often this is most easily done by invoking some
         * general parser. If the stream is of the type handled by the
         * parse, it just chugs along happily. If the stream is not of
         * that type, the parser should toss an exception when it fails.
         * If this happens within readlimit bytes, it allows the outer
         * code to reset the stream and try another parser.
         *
         * @exception  IOException  if the stream has not been marked or if the
         *               mark has been invalidated.
         * @see        java.io.FilterInputStream#in
         * @see        java.io.FilterInputStream#mark(int)
         */
        // safe to use synchronized here: super method is synchronized too
        // and involves no blocking operation; only mark & reset are
        // synchronized in the super class hierarchy.
        @Override
        public synchronized void reset() throws IOException {
            super.reset();
            if (cacheRequest != null) {
                marked = false;
                inCache += markCount;
            }
        }

        private void ensureOpen() throws IOException {
            if (closed)
                throw new IOException("stream is closed");
        }

        @Override
        public int read() throws IOException {
            ensureOpen();
            try {
                byte[] b = new byte[1];
                int ret = read(b);
                return (ret == -1? ret : (b[0] & 0x00FF));
            } catch (IOException ioex) {
                if (cacheRequest != null) {
                    cacheRequest.abort();
                }
                throw ioex;
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ensureOpen();
            try {
                int newLen = super.read(b, off, len);
                int nWrite;
                // write to cache
                if (inCache > 0) {
                    if (inCache >= newLen) {
                        inCache -= newLen;
                        nWrite = 0;
                    } else {
                        nWrite = newLen - inCache;
                        inCache = 0;
                    }
                } else {
                    nWrite = newLen;
                }
                if (nWrite > 0 && outputStream != null)
                    outputStream.write(b, off + (newLen-nWrite), nWrite);
                if (marked) {
                    markCount += newLen;
                }
                return newLen;
            } catch (IOException ioex) {
                if (cacheRequest != null) {
                    cacheRequest.abort();
                }
                throw ioex;
            }
        }

        /* skip() calls read() in order to ensure that entire response gets
         * cached. same implementation as InputStream.skip */

        private byte[] skipBuffer;
        private static final int SKIP_BUFFER_SIZE = 8096;

        @Override
        public long skip (long n) throws IOException {
            ensureOpen();
            long remaining = n;
            int nr;
            if (skipBuffer == null)
                skipBuffer = new byte[SKIP_BUFFER_SIZE];

            byte[] localSkipBuffer = skipBuffer;

            if (n <= 0) {
                return 0;
            }

            while (remaining > 0) {
                nr = read(localSkipBuffer, 0,
                          (int) Math.min(SKIP_BUFFER_SIZE, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
        }

        @Override
        public void close () throws IOException {
            if (closed)
                return;

            try {
                if (outputStream != null) {
                    if (read() != -1) {
                        cacheRequest.abort();
                    } else {
                        outputStream.close();
                    }
                }
                super.close ();
            } catch (IOException ioex) {
                if (cacheRequest != null) {
                    cacheRequest.abort();
                }
                throw ioex;
            } finally {
                closed = true;
                HttpURLConnection.this.http = null;
                checkResponseCredentials (true);
            }
        }
    }

    class StreamingOutputStream extends FilterOutputStream {

        long expected;
        long written;
        boolean closed;
        boolean error;
        IOException errorExcp;

        /**
         * expectedLength == -1 if the stream is chunked
         * expectedLength > 0 if the stream is fixed content-length
         *    In the 2nd case, we make sure the expected number
         *    of bytes are actually written
         */
        StreamingOutputStream (OutputStream os, long expectedLength) {
            super (os);
            expected = expectedLength;
            written = 0L;
            closed = false;
            error = false;
        }

        @Override
        public void write (int b) throws IOException {
            checkError();
            written ++;
            if (expected != -1L && written > expected) {
                throw new IOException ("too many bytes written");
            }
            out.write (b);
        }

        @Override
        public void write (byte[] b) throws IOException {
            write (b, 0, b.length);
        }

        @Override
        public void write (byte[] b, int off, int len) throws IOException {
            checkError();
            written += len;
            if (expected != -1L && written > expected) {
                out.close ();
                throw new IOException ("too many bytes written");
            }
            out.write (b, off, len);
        }

        void checkError () throws IOException {
            if (closed) {
                throw new IOException ("Stream is closed");
            }
            if (error) {
                throw errorExcp;
            }
            if (out instanceof PrintStream) {
                if (((PrintStream) out).checkError()) {
                    throw new IOException("Error writing request body to server");
                }
            } else if (out instanceof ChunkedOutputStream) {
                if (((ChunkedOutputStream) out).checkError()) {
                    throw new IOException("Error writing request body to server");
                }
            }
        }

        /* this is called to check that all the bytes
         * that were supposed to be written were written
         * and that the stream is now closed().
         */
        boolean writtenOK () {
            return closed && ! error;
        }

        @Override
        public void close () throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            if (expected != -1L) {
                /* not chunked */
                if (written != expected) {
                    error = true;
                    errorExcp = new IOException ("insufficient data written");
                    out.close ();
                    throw errorExcp;
                }
                super.flush(); /* can't close the socket */
            } else {
                /* chunked */
                super.close (); /* force final chunk to be written */
                /* trailing \r\n */
                OutputStream o = http.getOutputStream();
                o.write ('\r');
                o.write ('\n');
                o.flush();
            }
        }
    }


    static class ErrorStream extends InputStream {
        ByteBuffer buffer;
        InputStream is;

        private ErrorStream(ByteBuffer buf) {
            buffer = buf;
            is = null;
        }

        private ErrorStream(ByteBuffer buf, InputStream is) {
            buffer = buf;
            this.is = is;
        }

        // when this method is called, it's either the case that cl > 0, or
        // if chunk-encoded, cl = -1; in other words, cl can't be 0
        public static InputStream getErrorStream(InputStream is, long cl, HttpClient http) {

            // cl can't be 0; this following is here for extra precaution
            if (cl == 0) {
                return null;
            }

            try {
                // set SO_TIMEOUT to 1/5th of the total timeout
                // remember the old timeout value so that we can restore it
                int oldTimeout = http.getReadTimeout();
                http.setReadTimeout(timeout4ESBuffer/5);

                long expected = 0;
                boolean isChunked = false;
                // the chunked case
                if (cl < 0) {
                    expected = bufSize4ES;
                    isChunked = true;
                } else {
                    expected = cl;
                }
                if (expected <= bufSize4ES) {
                    int exp = (int) expected;
                    byte[] buffer = new byte[exp];
                    int count = 0, time = 0, len = 0;
                    do {
                        try {
                            len = is.read(buffer, count,
                                             buffer.length - count);
                            if (len < 0) {
                                if (isChunked) {
                                    // chunked ended
                                    // if chunked ended prematurely,
                                    // an IOException would be thrown
                                    break;
                                }
                                // the server sends less than cl bytes of data
                                throw new IOException("the server closes"+
                                                      " before sending "+cl+
                                                      " bytes of data");
                            }
                            count += len;
                        } catch (SocketTimeoutException ex) {
                            time += timeout4ESBuffer/5;
                        }
                    } while (count < exp && time < timeout4ESBuffer);

                    // reset SO_TIMEOUT to old value
                    http.setReadTimeout(oldTimeout);

                    // if count < cl at this point, we will not try to reuse
                    // the connection
                    if (count == 0) {
                        // since we haven't read anything,
                        // we will return the underlying
                        // inputstream back to the application
                        return null;
                    }  else if ((count == expected && !(isChunked)) || (isChunked && len <0)) {
                        // put the connection into keep-alive cache
                        // the inputstream will try to do the right thing
                        is.close();
                        return new ErrorStream(ByteBuffer.wrap(buffer, 0, count));
                    } else {
                        // we read part of the response body
                        return new ErrorStream(
                                      ByteBuffer.wrap(buffer, 0, count), is);
                    }
                }
                return null;
            } catch (IOException ioex) {
                // ioex.printStackTrace();
                return null;
            }
        }

        @Override
        public int available() throws IOException {
            if (is == null) {
                return buffer.remaining();
            } else {
                return buffer.remaining()+is.available();
            }
        }

        public int read() throws IOException {
            byte[] b = new byte[1];
            int ret = read(b);
            return (ret == -1? ret : (b[0] & 0x00FF));
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int rem = buffer.remaining();
            if (rem > 0) {
                int ret = rem < len? rem : len;
                buffer.get(b, off, ret);
                return ret;
            } else {
                if (is == null) {
                    return -1;
                } else {
                    return is.read(b, off, len);
                }
            }
        }

        @Override
        public void close() throws IOException {
            buffer = null;
            if (is != null) {
                is.close();
            }
        }
    }
}

/** An input stream that just returns EOF.  This is for
 * HTTP URLConnections that are KeepAlive && use the
 * HEAD method - i.e., stream not dead, but nothing to be read.
 */

class EmptyInputStream extends InputStream {

    @Override
    public int available() {
        return 0;
    }

    public int read() {
        return -1;
    }
}
