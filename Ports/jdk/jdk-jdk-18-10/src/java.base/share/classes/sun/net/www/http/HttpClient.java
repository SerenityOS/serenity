/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.http;

import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import sun.net.NetworkClient;
import sun.net.ProgressSource;
import sun.net.www.MessageHeader;
import sun.net.www.HeaderParser;
import sun.net.www.MeteredStream;
import sun.net.www.ParseUtil;
import sun.net.www.protocol.http.AuthenticatorKeys;
import sun.net.www.protocol.http.HttpURLConnection;
import sun.util.logging.PlatformLogger;
import static sun.net.www.protocol.http.HttpURLConnection.TunnelState.*;
import sun.security.action.GetPropertyAction;

/**
 * @author Herb Jellinek
 * @author Dave Brown
 */
public class HttpClient extends NetworkClient {
    private final ReentrantLock clientLock = new ReentrantLock();

    // whether this httpclient comes from the cache
    protected boolean cachedHttpClient = false;

    protected boolean inCache;

    // Http requests we send
    MessageHeader requests;

    // Http data we send with the headers
    PosterOutputStream poster = null;

    // true if we are in streaming mode (fixed length or chunked)
    boolean streaming;

    // if we've had one io error
    boolean failedOnce = false;

    /** Response code for CONTINUE */
    private boolean ignoreContinue = true;
    private static final int    HTTP_CONTINUE = 100;

    /** Default port number for http daemons. REMIND: make these private */
    static final int    httpPortNumber = 80;

    /** return default port number (subclasses may override) */
    protected int getDefaultPort () { return httpPortNumber; }

    private static int getDefaultPort(String proto) {
        if ("http".equalsIgnoreCase(proto))
            return 80;
        if ("https".equalsIgnoreCase(proto))
            return 443;
        return -1;
    }

    /* All proxying (generic as well as instance-specific) may be
     * disabled through use of this flag
     */
    protected boolean proxyDisabled;

    // are we using proxy in this instance?
    public boolean usingProxy = false;
    // target host, port for the URL
    protected String host;
    protected int port;

    /* where we cache currently open, persistent connections */
    protected static KeepAliveCache kac = new KeepAliveCache();

    private static boolean keepAliveProp = true;

    // retryPostProp is true by default so as to preserve behavior
    // from previous releases.
    private static boolean retryPostProp = true;

    /* Value of the system property jdk.ntlm.cache;
       if false, then NTLM connections will not be cached.
       The default value is 'true'. */
    private static final boolean cacheNTLMProp;
    /* Value of the system property jdk.spnego.cache;
       if false, then connections authentified using the Negotiate/Kerberos
       scheme will not be cached.
       The default value is 'true'. */
    private static final boolean cacheSPNEGOProp;

    volatile boolean keepingAlive;    /* this is a keep-alive connection */
    volatile boolean disableKeepAlive;/* keep-alive has been disabled for this
                                         connection - this will be used when
                                         recomputing the value of keepingAlive */
    int keepAliveConnections = -1;    /* number of keep-alives left */

    /**Idle timeout value, in milliseconds. Zero means infinity,
     * iff keepingAlive=true.
     * Unfortunately, we can't always believe this one.  If I'm connected
     * through a Netscape proxy to a server that sent me a keep-alive
     * time of 15 sec, the proxy unilaterally terminates my connection
     * after 5 sec.  So we have to hard code our effective timeout to
     * 4 sec for the case where we're using a proxy. *SIGH*
     */
    int keepAliveTimeout = 0;

    /** whether the response is to be cached */
    private CacheRequest cacheRequest = null;

    /** Url being fetched. */
    protected URL       url;

    /* if set, the client will be reused and must not be put in cache */
    public boolean reuse = false;

    // Traffic capture tool, if configured. See HttpCapture class for info
    private HttpCapture capture = null;

    private static final PlatformLogger logger = HttpURLConnection.getHttpLogger();
    private static void logFinest(String msg) {
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest(msg);
        }
    }

    protected volatile String authenticatorKey;

    /**
     * A NOP method kept for backwards binary compatibility
     * @deprecated -- system properties are no longer cached.
     */
    @Deprecated
    public static synchronized void resetProperties() {
    }

    int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    static {
        Properties props = GetPropertyAction.privilegedGetProperties();
        String keepAlive = props.getProperty("http.keepAlive");
        String retryPost = props.getProperty("sun.net.http.retryPost");
        String cacheNTLM = props.getProperty("jdk.ntlm.cache");
        String cacheSPNEGO = props.getProperty("jdk.spnego.cache");

        if (keepAlive != null) {
            keepAliveProp = Boolean.parseBoolean(keepAlive);
        } else {
            keepAliveProp = true;
        }

        if (retryPost != null) {
            retryPostProp = Boolean.parseBoolean(retryPost);
        } else {
            retryPostProp = true;
        }

        if (cacheNTLM != null) {
            cacheNTLMProp = Boolean.parseBoolean(cacheNTLM);
        } else {
            cacheNTLMProp = true;
        }

        if (cacheSPNEGO != null) {
            cacheSPNEGOProp = Boolean.parseBoolean(cacheSPNEGO);
        } else {
            cacheSPNEGOProp = true;
        }
    }

    /**
     * @return true iff http keep alive is set (i.e. enabled).  Defaults
     *          to true if the system property http.keepAlive isn't set.
     */
    public boolean getHttpKeepAliveSet() {
        return keepAliveProp;
    }


    protected HttpClient() {
    }

    private HttpClient(URL url)
    throws IOException {
        this(url, (String)null, -1, false);
    }

    protected HttpClient(URL url,
                         boolean proxyDisabled) throws IOException {
        this(url, null, -1, proxyDisabled);
    }

    /* This package-only CTOR should only be used for FTP piggy-backed on HTTP
     * URL's that use this won't take advantage of keep-alive.
     * Additionally, this constructor may be used as a last resort when the
     * first HttpClient gotten through New() failed (probably b/c of a
     * Keep-Alive mismatch).
     *
     * XXX That documentation is wrong ... it's not package-private any more
     */
    public HttpClient(URL url, String proxyHost, int proxyPort)
    throws IOException {
        this(url, proxyHost, proxyPort, false);
    }

    protected HttpClient(URL url, Proxy p, int to) throws IOException {
        proxy = (p == null) ? Proxy.NO_PROXY : p;
        this.host = url.getHost();
        this.url = url;
        port = url.getPort();
        if (port == -1) {
            port = getDefaultPort();
        }
        setConnectTimeout(to);

        capture = HttpCapture.getCapture(url);
        openServer();
    }

    protected static Proxy newHttpProxy(String proxyHost, int proxyPort,
                                      String proto) {
        if (proxyHost == null || proto == null)
            return Proxy.NO_PROXY;
        int pport = proxyPort < 0 ? getDefaultPort(proto) : proxyPort;
        InetSocketAddress saddr = InetSocketAddress.createUnresolved(proxyHost, pport);
        return new Proxy(Proxy.Type.HTTP, saddr);
    }

    /*
     * This constructor gives "ultimate" flexibility, including the ability
     * to bypass implicit proxying.  Sometimes we need to be using tunneling
     * (transport or network level) instead of proxying (application level),
     * for example when we don't want the application level data to become
     * visible to third parties.
     *
     * @param url               the URL to which we're connecting
     * @param proxy             proxy to use for this URL (e.g. forwarding)
     * @param proxyPort         proxy port to use for this URL
     * @param proxyDisabled     true to disable default proxying
     */
    private HttpClient(URL url, String proxyHost, int proxyPort,
                       boolean proxyDisabled)
        throws IOException {
        this(url, proxyDisabled ? Proxy.NO_PROXY :
             newHttpProxy(proxyHost, proxyPort, "http"), -1);
    }

    public HttpClient(URL url, String proxyHost, int proxyPort,
                       boolean proxyDisabled, int to)
        throws IOException {
        this(url, proxyDisabled ? Proxy.NO_PROXY :
             newHttpProxy(proxyHost, proxyPort, "http"), to);
    }

    /* This class has no public constructor for HTTP.  This method is used to
     * get an HttpClient to the specified URL.  If there's currently an
     * active HttpClient to that server/port, you'll get that one.
     */
    public static HttpClient New(URL url)
    throws IOException {
        return HttpClient.New(url, Proxy.NO_PROXY, -1, true, null);
    }

    public static HttpClient New(URL url, boolean useCache)
        throws IOException {
        return HttpClient.New(url, Proxy.NO_PROXY, -1, useCache, null);
    }

    public static HttpClient New(URL url, Proxy p, int to, boolean useCache,
        HttpURLConnection httpuc) throws IOException
    {
        if (p == null) {
            p = Proxy.NO_PROXY;
        }
        HttpClient ret = null;
        /* see if one's already around */
        if (useCache) {
            ret = kac.get(url, null);
            if (ret != null && httpuc != null &&
                httpuc.streaming() &&
                httpuc.getRequestMethod() == "POST") {
                if (!ret.available()) {
                    ret.inCache = false;
                    ret.closeServer();
                    ret = null;
                }
            }
            if (ret != null) {
                String ak = httpuc == null ? AuthenticatorKeys.DEFAULT
                     : httpuc.getAuthenticatorKey();
                boolean compatible = Objects.equals(ret.proxy, p)
                     && Objects.equals(ret.getAuthenticatorKey(), ak);
                if (compatible) {
                    ret.lock();
                    try {
                        ret.cachedHttpClient = true;
                        assert ret.inCache;
                        ret.inCache = false;
                        if (httpuc != null && ret.needsTunneling())
                            httpuc.setTunnelState(TUNNELING);
                        logFinest("KeepAlive stream retrieved from the cache, " + ret);
                    } finally {
                        ret.unlock();
                    }
                } else {
                    // We cannot return this connection to the cache as it's
                    // KeepAliveTimeout will get reset. We simply close the connection.
                    // This should be fine as it is very rare that a connection
                    // to the same host will not use the same proxy.
                    ret.lock();
                    try  {
                        ret.inCache = false;
                        ret.closeServer();
                    } finally {
                        ret.unlock();
                    }
                    ret = null;
                }
            }
        }
        if (ret == null) {
            ret = new HttpClient(url, p, to);
            if (httpuc != null) {
                ret.authenticatorKey = httpuc.getAuthenticatorKey();
            }
        } else {
            @SuppressWarnings("removal")
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                if (ret.proxy == Proxy.NO_PROXY || ret.proxy == null) {
                    security.checkConnect(InetAddress.getByName(url.getHost()).getHostAddress(), url.getPort());
                } else {
                    security.checkConnect(url.getHost(), url.getPort());
                }
            }
            ret.url = url;
        }
        return ret;
    }

    public static HttpClient New(URL url, Proxy p, int to,
        HttpURLConnection httpuc) throws IOException
    {
        return New(url, p, to, true, httpuc);
    }

    public static HttpClient New(URL url, String proxyHost, int proxyPort,
                                 boolean useCache)
        throws IOException {
        return New(url, newHttpProxy(proxyHost, proxyPort, "http"),
            -1, useCache, null);
    }

    public static HttpClient New(URL url, String proxyHost, int proxyPort,
                                 boolean useCache, int to,
                                 HttpURLConnection httpuc)
        throws IOException {
        return New(url, newHttpProxy(proxyHost, proxyPort, "http"),
            to, useCache, httpuc);
    }

    public final String getAuthenticatorKey() {
        String k = authenticatorKey;
        if (k == null) return AuthenticatorKeys.DEFAULT;
        return k;
    }

    /* return it to the cache as still usable, if:
     * 1) It's keeping alive, AND
     * 2) It still has some connections left, AND
     * 3) It hasn't had a error (PrintStream.checkError())
     * 4) It hasn't timed out
     *
     * If this client is not keepingAlive, it should have been
     * removed from the cache in the parseHeaders() method.
     */

    public void finished() {
        if (reuse) /* will be reused */
            return;
        keepAliveConnections--;
        poster = null;
        if (keepAliveConnections > 0 && isKeepingAlive() &&
               !(serverOutput.checkError())) {
            /* This connection is keepingAlive && still valid.
             * Return it to the cache.
             */
            putInKeepAliveCache();
        } else {
            closeServer();
        }
    }

    protected boolean available() {
        boolean available = true;
        int old = -1;

        lock();
        try {
            try {
                old = serverSocket.getSoTimeout();
                serverSocket.setSoTimeout(1);
                BufferedInputStream tmpbuf =
                        new BufferedInputStream(serverSocket.getInputStream());
                int r = tmpbuf.read();
                if (r == -1) {
                    logFinest("HttpClient.available(): " +
                            "read returned -1: not available");
                    available = false;
                }
            } catch (SocketTimeoutException e) {
                logFinest("HttpClient.available(): " +
                        "SocketTimeout: its available");
            } finally {
                if (old != -1)
                    serverSocket.setSoTimeout(old);
            }
        } catch (IOException e) {
            logFinest("HttpClient.available(): " +
                        "SocketException: not available");
            available = false;
        } finally {
            unlock();
        }
        return available;
    }

    protected void putInKeepAliveCache() {
        lock();
        try {
            if (inCache) {
                assert false : "Duplicate put to keep alive cache";
                return;
            }
            inCache = true;
            kac.put(url, null, this);
        } finally {
            unlock();
        }
    }

    protected boolean isInKeepAliveCache() {
        lock();
        try {
            return inCache;
        } finally {
            unlock();
        }
    }

    /*
     * Close an idle connection to this URL (if it exists in the
     * cache).
     */
    public void closeIdleConnection() {
        HttpClient http = kac.get(url, null);
        if (http != null) {
            http.closeServer();
        }
    }

    /* We're very particular here about what our InputStream to the server
     * looks like for reasons that are apparent if you can decipher the
     * method parseHTTP().  That's why this method is overidden from the
     * superclass.
     */
    @Override
    public void openServer(String server, int port) throws IOException {
        serverSocket = doConnect(server, port);
        try {
            OutputStream out = serverSocket.getOutputStream();
            if (capture != null) {
                out = new HttpCaptureOutputStream(out, capture);
            }
            serverOutput = new PrintStream(
                new BufferedOutputStream(out),
                                         false, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding+" encoding not found", e);
        }
        serverSocket.setTcpNoDelay(true);
    }

    /*
     * Returns true if the http request should be tunneled through proxy.
     * An example where this is the case is Https.
     */
    public boolean needsTunneling() {
        return false;
    }

    /*
     * Returns true if this httpclient is from cache
     */
    public boolean isCachedConnection() {
        lock();
        try {
            return cachedHttpClient;
        } finally {
            unlock();
        }
    }

    /*
     * Finish any work left after the socket connection is
     * established.  In the normal http case, it's a NO-OP. Subclass
     * may need to override this. An example is Https, where for
     * direct connection to the origin server, ssl handshake needs to
     * be done; for proxy tunneling, the socket needs to be converted
     * into an SSL socket before ssl handshake can take place.
     */
    public void afterConnect() throws IOException, UnknownHostException {
        // NO-OP. Needs to be overwritten by HttpsClient
    }

    /*
     * call openServer in a privileged block
     */
    @SuppressWarnings("removal")
    private void privilegedOpenServer(final InetSocketAddress server)
         throws IOException
    {
        assert clientLock.isHeldByCurrentThread();
        try {
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedExceptionAction<>() {
                    public Void run() throws IOException {
                    openServer(server.getHostString(), server.getPort());
                    return null;
                }
            });
        } catch (java.security.PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
    }

    /*
     * call super.openServer
     */
    private void superOpenServer(final String proxyHost,
                                 final int proxyPort)
        throws IOException, UnknownHostException
    {
        super.openServer(proxyHost, proxyPort);
    }

    /*
     */
    protected void openServer() throws IOException {

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();

        lock();
        try {
            if (security != null) {
                security.checkConnect(host, port);
            }

            if (keepingAlive) { // already opened
                return;
            }

            if (url.getProtocol().equals("http") ||
                    url.getProtocol().equals("https")) {

                if ((proxy != null) && (proxy.type() == Proxy.Type.HTTP)) {
                    sun.net.www.URLConnection.setProxiedHost(host);
                    privilegedOpenServer((InetSocketAddress) proxy.address());
                    usingProxy = true;
                    return;
                } else {
                    // make direct connection
                    openServer(host, port);
                    usingProxy = false;
                    return;
                }

            } else {
                /* we're opening some other kind of url, most likely an
                 * ftp url.
                 */
                if ((proxy != null) && (proxy.type() == Proxy.Type.HTTP)) {
                    sun.net.www.URLConnection.setProxiedHost(host);
                    privilegedOpenServer((InetSocketAddress) proxy.address());
                    usingProxy = true;
                    return;
                } else {
                    // make direct connection
                    super.openServer(host, port);
                    usingProxy = false;
                    return;
                }
            }
        } finally {
            unlock();
        }
    }

    public String getURLFile() throws IOException {

        String fileName;

        /**
         * proxyDisabled is set by subclass HttpsClient!
         */
        if (usingProxy && !proxyDisabled) {
            // Do not use URLStreamHandler.toExternalForm as the fragment
            // should not be part of the RequestURI. It should be an
            // absolute URI which does not have a fragment part.
            StringBuilder result = new StringBuilder(128);
            result.append(url.getProtocol());
            result.append(":");
            if (url.getAuthority() != null && !url.getAuthority().isEmpty()) {
                result.append("//");
                result.append(url.getAuthority());
            }
            if (url.getPath() != null) {
                result.append(url.getPath());
            }
            if (url.getQuery() != null) {
                result.append('?');
                result.append(url.getQuery());
            }

            fileName = result.toString();
        } else {
            fileName = url.getFile();

            if ((fileName == null) || (fileName.isEmpty())) {
                fileName = "/";
            } else if (fileName.charAt(0) == '?') {
                /* HTTP/1.1 spec says in 5.1.2. about Request-URI:
                 * "Note that the absolute path cannot be empty; if
                 * none is present in the original URI, it MUST be
                 * given as "/" (the server root)."  So if the file
                 * name here has only a query string, the path is
                 * empty and we also have to add a "/".
                 */
                fileName = "/" + fileName;
            }
        }

        if (fileName.indexOf('\n') == -1)
            return fileName;
        else
            throw new java.net.MalformedURLException("Illegal character in URL");
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void writeRequests(MessageHeader head) {
        requests = head;
        requests.print(serverOutput);
        serverOutput.flush();
    }

    public void writeRequests(MessageHeader head,
                              PosterOutputStream pos) throws IOException {
        requests = head;
        requests.print(serverOutput);
        poster = pos;
        if (poster != null)
            poster.writeTo(serverOutput);
        serverOutput.flush();
    }

    public void writeRequests(MessageHeader head,
                              PosterOutputStream pos,
                              boolean streaming) throws IOException {
        this.streaming = streaming;
        writeRequests(head, pos);
    }

    /** Parse the first line of the HTTP request.  It usually looks
        something like: {@literal "HTTP/1.0 <number> comment\r\n"}. */

    public boolean parseHTTP(MessageHeader responses, ProgressSource pi, HttpURLConnection httpuc)
    throws IOException {
        /* If "HTTP/*" is found in the beginning, return true.  Let
         * HttpURLConnection parse the mime header itself.
         *
         * If this isn't valid HTTP, then we don't try to parse a header
         * out of the beginning of the response into the responses,
         * and instead just queue up the output stream to it's very beginning.
         * This seems most reasonable, and is what the NN browser does.
         */

        try {
            serverInput = serverSocket.getInputStream();
            if (capture != null) {
                serverInput = new HttpCaptureInputStream(serverInput, capture);
            }
            serverInput = new BufferedInputStream(serverInput);
            return (parseHTTPHeader(responses, pi, httpuc));
        } catch (SocketTimeoutException stex) {
            // We don't want to retry the request when the app. sets a timeout
            // but don't close the server if timeout while waiting for 100-continue
            if (ignoreContinue) {
                closeServer();
            }
            throw stex;
        } catch (IOException e) {
            closeServer();
            cachedHttpClient = false;
            if (!failedOnce && requests != null) {
                failedOnce = true;
                if (getRequestMethod().equals("CONNECT")
                    || streaming
                    || (httpuc.getRequestMethod().equals("POST")
                        && !retryPostProp)) {
                    // do not retry the request
                }  else {
                    // try once more
                    openServer();
                    checkTunneling(httpuc);
                    afterConnect();
                    writeRequests(requests, poster);
                    return parseHTTP(responses, pi, httpuc);
                }
            }
            throw e;
        }

    }

    // Check whether tunnel must be open and open it if necessary
    // (in the case of HTTPS with proxy)
    private void checkTunneling(HttpURLConnection httpuc) throws IOException {
        if (needsTunneling()) {
            MessageHeader origRequests = requests;
            PosterOutputStream origPoster = poster;
            httpuc.doTunneling();
            requests = origRequests;
            poster = origPoster;
        }
    }

    private boolean parseHTTPHeader(MessageHeader responses, ProgressSource pi, HttpURLConnection httpuc)
    throws IOException {
        /* If "HTTP/*" is found in the beginning, return true.  Let
         * HttpURLConnection parse the mime header itself.
         *
         * If this isn't valid HTTP, then we don't try to parse a header
         * out of the beginning of the response into the responses,
         * and instead just queue up the output stream to it's very beginning.
         * This seems most reasonable, and is what the NN browser does.
         */

        keepAliveConnections = -1;
        keepAliveTimeout = 0;

        boolean ret = false;
        byte[] b = new byte[8];

        try {
            int nread = 0;
            serverInput.mark(10);
            while (nread < 8) {
                int r = serverInput.read(b, nread, 8 - nread);
                if (r < 0) {
                    break;
                }
                nread += r;
            }
            String keep=null;
            String authenticate=null;
            ret = b[0] == 'H' && b[1] == 'T'
                    && b[2] == 'T' && b[3] == 'P' && b[4] == '/' &&
                b[5] == '1' && b[6] == '.';
            serverInput.reset();
            if (ret) { // is valid HTTP - response started w/ "HTTP/1."
                responses.parseHeader(serverInput);

                // we've finished parsing http headers
                // check if there are any applicable cookies to set (in cache)
                CookieHandler cookieHandler = httpuc.getCookieHandler();
                if (cookieHandler != null) {
                    URI uri = ParseUtil.toURI(url);
                    // NOTE: That cast from Map shouldn't be necessary but
                    // a bug in javac is triggered under certain circumstances
                    // So we do put the cast in as a workaround until
                    // it is resolved.
                    if (uri != null)
                        cookieHandler.put(uri, responses.getHeaders());
                }

                /* decide if we're keeping alive:
                 * This is a bit tricky.  There's a spec, but most current
                 * servers (10/1/96) that support this differ in dialects.
                 * If the server/client misunderstand each other, the
                 * protocol should fall back onto HTTP/1.0, no keep-alive.
                 */
                if (usingProxy) { // not likely a proxy will return this
                    keep = responses.findValue("Proxy-Connection");
                    authenticate = responses.findValue("Proxy-Authenticate");
                }
                if (keep == null) {
                    keep = responses.findValue("Connection");
                    authenticate = responses.findValue("WWW-Authenticate");
                }

                // 'disableKeepAlive' starts with the value false.
                // It can transition from false to true, but once true
                // it stays true.
                // If cacheNTLMProp is false, and disableKeepAlive is false,
                // then we need to examine the response headers to figure out
                // whether we are doing NTLM authentication. If we do NTLM,
                // and cacheNTLMProp is false, than we can't keep this connection
                // alive: we will switch disableKeepAlive to true.
                boolean canKeepAlive = !disableKeepAlive;
                if (canKeepAlive && (cacheNTLMProp == false || cacheSPNEGOProp == false)
                        && authenticate != null) {
                    authenticate = authenticate.toLowerCase(Locale.US);
                    if (cacheNTLMProp == false) {
                        canKeepAlive &= !authenticate.startsWith("ntlm ");
                    }
                    if (cacheSPNEGOProp == false) {
                        canKeepAlive &= !authenticate.startsWith("negotiate ");
                        canKeepAlive &= !authenticate.startsWith("kerberos ");
                    }
                }
                disableKeepAlive |= !canKeepAlive;

                if (keep != null && keep.toLowerCase(Locale.US).equals("keep-alive")) {
                    /* some servers, notably Apache1.1, send something like:
                     * "Keep-Alive: timeout=15, max=1" which we should respect.
                     */
                    if (disableKeepAlive) {
                        keepAliveConnections = 1;
                    } else {
                        HeaderParser p = new HeaderParser(
                            responses.findValue("Keep-Alive"));
                        /* default should be larger in case of proxy */
                        keepAliveConnections = p.findInt("max", usingProxy?50:5);
                        keepAliveTimeout = p.findInt("timeout", usingProxy?60:5);
                    }
                } else if (b[7] != '0') {
                    /*
                     * We're talking 1.1 or later. Keep persistent until
                     * the server says to close.
                     */
                    if (keep != null || disableKeepAlive) {
                        /*
                         * The only Connection token we understand is close.
                         * Paranoia: if there is any Connection header then
                         * treat as non-persistent.
                         */
                        keepAliveConnections = 1;
                    } else {
                        keepAliveConnections = 5;
                    }
                }
            } else if (nread != 8) {
                if (!failedOnce && requests != null) {
                    failedOnce = true;
                    if (getRequestMethod().equals("CONNECT")
                        || streaming
                        || (httpuc.getRequestMethod().equals("POST")
                            && !retryPostProp)) {
                        // do not retry the request
                    } else {
                        closeServer();
                        cachedHttpClient = false;
                        openServer();
                        checkTunneling(httpuc);
                        afterConnect();
                        writeRequests(requests, poster);
                        return parseHTTP(responses, pi, httpuc);
                    }
                }
                throw new SocketException("Unexpected end of file from server");
            } else {
                // we can't vouche for what this is....
                responses.set("Content-type", "unknown/unknown");
            }
        } catch (IOException e) {
            throw e;
        }

        int code = -1;
        try {
            String resp;
            resp = responses.getValue(0);
            /* should have no leading/trailing LWS
             * expedite the typical case by assuming it has
             * form "HTTP/1.x <WS> 2XX <mumble>"
             */
            int ind;
            ind = resp.indexOf(' ');
            while(resp.charAt(ind) == ' ')
                ind++;
            code = Integer.parseInt(resp, ind, ind + 3, 10);
        } catch (Exception e) {}

        if (code == HTTP_CONTINUE && ignoreContinue) {
            responses.reset();
            return parseHTTPHeader(responses, pi, httpuc);
        }

        long cl = -1;

        /*
         * Set things up to parse the entity body of the reply.
         * We should be smarter about avoid pointless work when
         * the HTTP method and response code indicate there will be
         * no entity body to parse.
         */
        String te = responses.findValue("Transfer-Encoding");
        if (te != null && te.equalsIgnoreCase("chunked")) {
            serverInput = new ChunkedInputStream(serverInput, this, responses);

            /*
             * If keep alive not specified then close after the stream
             * has completed.
             */
            if (keepAliveConnections <= 1) {
                keepAliveConnections = 1;
                keepingAlive = false;
            } else {
                keepingAlive = !disableKeepAlive;
            }
            failedOnce = false;
        } else {

            /*
             * If it's a keep alive connection then we will keep
             * (alive if :-
             * 1. content-length is specified, or
             * 2. "Not-Modified" or "No-Content" responses - RFC 2616 states that
             *    204 or 304 response must not include a message body.
             */
            String cls = responses.findValue("content-length");
            if (cls != null) {
                try {
                    cl = Long.parseLong(cls);
                } catch (NumberFormatException e) {
                    cl = -1;
                }
            }
            String requestLine = requests.getKey(0);

            if ((requestLine != null &&
                 (requestLine.startsWith("HEAD"))) ||
                code == HttpURLConnection.HTTP_NOT_MODIFIED ||
                code == HttpURLConnection.HTTP_NO_CONTENT) {
                cl = 0;
            }

            if (keepAliveConnections > 1 &&
                (cl >= 0 ||
                 code == HttpURLConnection.HTTP_NOT_MODIFIED ||
                 code == HttpURLConnection.HTTP_NO_CONTENT)) {
                keepingAlive = !disableKeepAlive;
                failedOnce = false;
            } else if (keepingAlive) {
                /* Previously we were keeping alive, and now we're not.  Remove
                 * this from the cache (but only here, once) - otherwise we get
                 * multiple removes and the cache count gets messed up.
                 */
                keepingAlive=false;
            }
        }

        /* wrap a KeepAliveStream/MeteredStream around it if appropriate */

        if (cl > 0) {
            // In this case, content length is well known, so it is okay
            // to wrap the input stream with KeepAliveStream/MeteredStream.

            if (pi != null) {
                // Progress monitor is enabled
                pi.setContentType(responses.findValue("content-type"));
            }

            // If disableKeepAlive == true, the client will not be returned
            // to the cache. But we still need to use a keepalive stream to
            // allow the multi-message authentication exchange on the connection
            boolean useKeepAliveStream = isKeepingAlive() || disableKeepAlive;
            if (useKeepAliveStream)   {
                // Wrap KeepAliveStream if keep alive is enabled.
                logFinest("KeepAlive stream used: " + url);
                serverInput = new KeepAliveStream(serverInput, pi, cl, this);
                failedOnce = false;
            }
            else        {
                serverInput = new MeteredStream(serverInput, pi, cl);
            }
        }
        else if (cl == -1)  {
            // In this case, content length is unknown - the input
            // stream would simply be a regular InputStream or
            // ChunkedInputStream.

            if (pi != null) {
                // Progress monitoring is enabled.

                pi.setContentType(responses.findValue("content-type"));

                // Wrap MeteredStream for tracking indeterministic
                // progress, even if the input stream is ChunkedInputStream.
                serverInput = new MeteredStream(serverInput, pi, cl);
            }
            else    {
                // Progress monitoring is disabled, and there is no
                // need to wrap an unknown length input stream.

                // ** This is an no-op **
            }
        }
        else    {
            if (pi != null)
                pi.finishTracking();
        }

        return ret;
    }

    public InputStream getInputStream() {
        lock();
        try {
            return serverInput;
        } finally {
            unlock();
        }
    }

    public OutputStream getOutputStream() {
        return serverOutput;
    }

    @Override
    public String toString() {
        return getClass().getName()+"("+url+")";
    }

    public final boolean isKeepingAlive() {
        return getHttpKeepAliveSet() && keepingAlive;
    }

    public void setCacheRequest(CacheRequest cacheRequest) {
        this.cacheRequest = cacheRequest;
    }

    CacheRequest getCacheRequest() {
        return cacheRequest;
    }

    String getRequestMethod() {
        if (requests != null) {
            String requestLine = requests.getKey(0);
            if (requestLine != null) {
               return requestLine.split("\\s+")[0];
            }
        }
        return "";
    }

    public void setDoNotRetry(boolean value) {
        // failedOnce is used to determine if a request should be retried.
        failedOnce = value;
    }

    public void setIgnoreContinue(boolean value) {
        ignoreContinue = value;
    }

    /* Use only on connections in error. */
    @Override
    public void closeServer() {
        try {
            keepingAlive = false;
            serverSocket.close();
        } catch (Exception e) {}
    }

    /**
     * @return the proxy host being used for this client, or null
     *          if we're not going through a proxy
     */
    public String getProxyHostUsed() {
        if (!usingProxy) {
            return null;
        } else {
            return ((InetSocketAddress)proxy.address()).getHostString();
        }
    }

    /**
     * @return the proxy port being used for this client.  Meaningless
     *          if getProxyHostUsed() gives null.
     */
    public int getProxyPortUsed() {
        if (usingProxy)
            return ((InetSocketAddress)proxy.address()).getPort();
        return -1;
    }

    public final void lock() {
        clientLock.lock();
    }

    public final void unlock() {
        clientLock.unlock();
    }
}
