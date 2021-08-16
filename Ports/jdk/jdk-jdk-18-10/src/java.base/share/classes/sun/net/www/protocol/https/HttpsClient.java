/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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


package sun.net.www.protocol.https;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.PrintStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.Principal;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import javax.net.ssl.*;
import sun.net.www.http.HttpClient;
import sun.net.www.protocol.http.AuthenticatorKeys;
import sun.net.www.protocol.http.HttpURLConnection;
import sun.security.action.*;

import sun.security.util.HostnameChecker;
import sun.security.ssl.SSLSocketImpl;

import sun.util.logging.PlatformLogger;
import static sun.net.www.protocol.http.HttpURLConnection.TunnelState.*;


/**
 * This class provides HTTPS client URL support, building on the standard
 * "sun.net.www" HTTP protocol handler.  HTTPS is the same protocol as HTTP,
 * but differs in the transport layer which it uses:  <UL>
 *
 *      <LI>There's a <em>Secure Sockets Layer</em> between TCP
 *      and the HTTP protocol code.
 *
 *      <LI>It uses a different default TCP port.
 *
 *      <LI>It doesn't use application level proxies, which can see and
 *      manipulate HTTP user level data, compromising privacy.  It uses
 *      low level tunneling instead, which hides HTTP protocol and data
 *      from all third parties.  (Traffic analysis is still possible).
 *
 *      <LI>It does basic server authentication, to protect
 *      against "URL spoofing" attacks.  This involves deciding
 *      whether the X.509 certificate chain identifying the server
 *      is trusted, and verifying that the name of the server is
 *      found in the certificate.  (The application may enable an
 *      anonymous SSL cipher suite, and such checks are not done
 *      for anonymous ciphers.)
 *
 *      <LI>It exposes key SSL session attributes, specifically the
 *      cipher suite in use and the server's X509 certificates, to
 *      application software which knows about this protocol handler.
 *
 *      </UL>
 *
 * <P> System properties used include:  <UL>
 *
 *      <LI><em>https.proxyHost</em> ... the host supporting SSL
 *      tunneling using the conventional CONNECT syntax
 *
 *      <LI><em>https.proxyPort</em> ... port to use on proxyHost
 *
 *      <LI><em>https.cipherSuites</em> ... comma separated list of
 *      SSL cipher suite names to enable.
 *
 *      <LI><em>http.nonProxyHosts</em> ...
 *
 *      </UL>
 *
 * @author David Brownell
 * @author Bill Foote
 */

// final for export control reasons (access to APIs); remove with care
final class HttpsClient extends HttpClient
    implements HandshakeCompletedListener
{
    // STATIC STATE and ACCESSORS THERETO

    // HTTPS uses a different default port number than HTTP.
    private static final int    httpsPortNumber = 443;

    // default HostnameVerifier class canonical name
    private static final String defaultHVCanonicalName =
            "javax.net.ssl.HttpsURLConnection.DefaultHostnameVerifier";

    /** Returns the default HTTPS port (443) */
    @Override
    protected int getDefaultPort() { return httpsPortNumber; }

    private HostnameVerifier hv;
    private SSLSocketFactory sslSocketFactory;

    // HttpClient.proxyDisabled will always be false, because we don't
    // use an application-level HTTP proxy.  We might tunnel through
    // our http proxy, though.


    // INSTANCE DATA

    // last negotiated SSL session
    private SSLSession  session;

    private String [] getCipherSuites() {
        //
        // If ciphers are assigned, sort them into an array.
        //
        String ciphers [];
        String cipherString =
                GetPropertyAction.privilegedGetProperty("https.cipherSuites");

        if (cipherString == null || cipherString.isEmpty()) {
            ciphers = null;
        } else {
            StringTokenizer     tokenizer;
            ArrayList<String>   v = new ArrayList<>();

            tokenizer = new StringTokenizer(cipherString, ",");
            while (tokenizer.hasMoreTokens())
                v.add(tokenizer.nextToken());
            ciphers = new String [v.size()];
            for (int i = 0; i < ciphers.length; i++)
                ciphers [i] = v.get(i);
        }
        return ciphers;
    }

    private String [] getProtocols() {
        //
        // If protocols are assigned, sort them into an array.
        //
        String protocols [];
        String protocolString =
                GetPropertyAction.privilegedGetProperty("https.protocols");

        if (protocolString == null || protocolString.isEmpty()) {
            protocols = null;
        } else {
            StringTokenizer     tokenizer;
            ArrayList<String>   v = new ArrayList<>();

            tokenizer = new StringTokenizer(protocolString, ",");
            while (tokenizer.hasMoreTokens())
                v.add(tokenizer.nextToken());
            protocols = new String [v.size()];
            for (int i = 0; i < protocols.length; i++) {
                protocols [i] = v.get(i);
            }
        }
        return protocols;
    }

    private String getUserAgent() {
        String userAgent =
                GetPropertyAction.privilegedGetProperty("https.agent");
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "JSSE";
        }
        return userAgent;
    }

    // CONSTRUCTOR, FACTORY


    /**
     * Create an HTTPS client URL.  Traffic will be tunneled through any
     * intermediate nodes rather than proxied, so that confidentiality
     * of data exchanged can be preserved.  However, note that all the
     * anonymous SSL flavors are subject to "person-in-the-middle"
     * attacks against confidentiality.  If you enable use of those
     * flavors, you may be giving up the protection you get through
     * SSL tunneling.
     *
     * Use New to get new HttpsClient. This constructor is meant to be
     * used only by New method. New properly checks for URL spoofing.
     *
     * @param url https URL with which a connection must be established
     */
    private HttpsClient(SSLSocketFactory sf, URL url)
    throws IOException
    {
        // HttpClient-level proxying is always disabled,
        // because we override doConnect to do tunneling instead.
        this(sf, url, (String)null, -1);
    }

    /**
     *  Create an HTTPS client URL.  Traffic will be tunneled through
     * the specified proxy server.
     */
    HttpsClient(SSLSocketFactory sf, URL url, String proxyHost, int proxyPort)
        throws IOException {
        this(sf, url, proxyHost, proxyPort, -1);
    }

    /**
     *  Create an HTTPS client URL.  Traffic will be tunneled through
     * the specified proxy server, with a connect timeout
     */
    HttpsClient(SSLSocketFactory sf, URL url, String proxyHost, int proxyPort,
                int connectTimeout)
        throws IOException {
        this(sf, url,
             (proxyHost == null? null:
                HttpClient.newHttpProxy(proxyHost, proxyPort, "https")),
                connectTimeout);
    }

    /**
     *  Same as previous constructor except using a Proxy
     */
    HttpsClient(SSLSocketFactory sf, URL url, Proxy proxy,
                int connectTimeout)
        throws IOException {
        PlatformLogger logger = HttpURLConnection.getHttpLogger();
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
             logger.finest("Creating new HttpsClient with url:" + url + " and proxy:" + proxy +
             " with connect timeout:" + connectTimeout);
        }
        this.proxy = proxy;
        setSSLSocketFactory(sf);
        this.proxyDisabled = true;

        this.host = url.getHost();
        this.url = url;
        port = url.getPort();
        if (port == -1) {
            port = getDefaultPort();
        }
        setConnectTimeout(connectTimeout);
        openServer();
    }


    // This code largely ripped off from HttpClient.New, and
    // it uses the same keepalive cache.

    static HttpClient New(SSLSocketFactory sf, URL url, HostnameVerifier hv,
                          HttpURLConnection httpuc)
            throws IOException {
        return HttpsClient.New(sf, url, hv, true, httpuc);
    }

    /** See HttpClient for the model for this method. */
    static HttpClient New(SSLSocketFactory sf, URL url,
            HostnameVerifier hv, boolean useCache,
            HttpURLConnection httpuc) throws IOException {
        return HttpsClient.New(sf, url, hv, (String)null, -1, useCache, httpuc);
    }

    /**
     * Get a HTTPS client to the URL.  Traffic will be tunneled through
     * the specified proxy server.
     */
    static HttpClient New(SSLSocketFactory sf, URL url, HostnameVerifier hv,
                           String proxyHost, int proxyPort,
                           HttpURLConnection httpuc) throws IOException {
        return HttpsClient.New(sf, url, hv, proxyHost, proxyPort, true, httpuc);
    }

    static HttpClient New(SSLSocketFactory sf, URL url, HostnameVerifier hv,
                           String proxyHost, int proxyPort, boolean useCache,
                           HttpURLConnection httpuc)
        throws IOException {
        return HttpsClient.New(sf, url, hv, proxyHost, proxyPort, useCache, -1,
                               httpuc);
    }

    static HttpClient New(SSLSocketFactory sf, URL url, HostnameVerifier hv,
                          String proxyHost, int proxyPort, boolean useCache,
                          int connectTimeout, HttpURLConnection httpuc)
        throws IOException {

        return HttpsClient.New(sf, url, hv,
                               (proxyHost == null? null :
                                HttpClient.newHttpProxy(proxyHost, proxyPort, "https")),
                               useCache, connectTimeout, httpuc);
    }

    static HttpClient New(SSLSocketFactory sf, URL url, HostnameVerifier hv,
                          Proxy p, boolean useCache,
                          int connectTimeout, HttpURLConnection httpuc)
        throws IOException
    {
        if (p == null) {
            p = Proxy.NO_PROXY;
        }
        PlatformLogger logger = HttpURLConnection.getHttpLogger();
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest("Looking for HttpClient for URL " + url +
                " and proxy value of " + p);
        }
        HttpsClient ret = null;
        if (useCache) {
            /* see if one's already around */
            ret = (HttpsClient) kac.get(url, sf);
            if (ret != null && httpuc != null &&
                httpuc.streaming() &&
                httpuc.getRequestMethod() == "POST") {
                if (!ret.available())
                    ret = null;
            }

            if (ret != null) {
                String ak = httpuc == null ? AuthenticatorKeys.DEFAULT
                     : httpuc.getAuthenticatorKey();
                boolean compatible = ((ret.proxy != null && ret.proxy.equals(p)) ||
                    (ret.proxy == null && p == Proxy.NO_PROXY))
                     && Objects.equals(ret.getAuthenticatorKey(), ak);

                if (compatible) {
                    ret.lock();
                    try {
                        ret.cachedHttpClient = true;
                        assert ret.inCache;
                        ret.inCache = false;
                        if (httpuc != null && ret.needsTunneling())
                            httpuc.setTunnelState(TUNNELING);
                        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                            logger.finest("KeepAlive stream retrieved from the cache, " + ret);
                        }
                    } finally {
                        ret.unlock();
                    }
                } else {
                    // We cannot return this connection to the cache as it's
                    // KeepAliveTimeout will get reset. We simply close the connection.
                    // This should be fine as it is very rare that a connection
                    // to the same host will not use the same proxy.
                    ret.lock();
                    try {
                        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                            logger.finest("Not returning this connection to cache: " + ret);
                        }
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
            ret = new HttpsClient(sf, url, p, connectTimeout);
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
        ret.setHostnameVerifier(hv);

        return ret;
    }

    // METHODS
    void setHostnameVerifier(HostnameVerifier hv) {
        this.hv = hv;
    }

    void setSSLSocketFactory(SSLSocketFactory sf) {
        sslSocketFactory = sf;
    }

    SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * The following method, createSocket, is defined in NetworkClient
     * and overridden here so that the socket facroty is used to create
     * new sockets.
     */
    @Override
    protected Socket createSocket() throws IOException {
        try {
            return sslSocketFactory.createSocket();
        } catch (SocketException se) {
            //
            // bug 6771432
            // javax.net.SocketFactory throws a SocketException with an
            // UnsupportedOperationException as its cause to indicate that
            // unconnected sockets have not been implemented.
            //
            Throwable t = se.getCause();
            if (t instanceof UnsupportedOperationException) {
                return super.createSocket();
            } else {
                throw se;
            }
        }
    }


    @Override
    public boolean needsTunneling() {
        return (proxy != null && proxy.type() != Proxy.Type.DIRECT
                && proxy.type() != Proxy.Type.SOCKS);
    }

    @Override
    public void afterConnect() throws IOException, UnknownHostException {
        if (!isCachedConnection()) {
            SSLSocket s = null;
            SSLSocketFactory factory = sslSocketFactory;
            try {
                if (!(serverSocket instanceof SSLSocket)) {
                    s = (SSLSocket)factory.createSocket(serverSocket,
                                                        host, port, true);
                } else {
                    s = (SSLSocket)serverSocket;
                    if (s instanceof SSLSocketImpl) {
                        ((SSLSocketImpl)s).setHost(host);
                    }
                }
            } catch (IOException ex) {
                // If we fail to connect through the tunnel, try it
                // locally, as a last resort.  If this doesn't work,
                // throw the original exception.
                try {
                    s = (SSLSocket)factory.createSocket(host, port);
                } catch (IOException ignored) {
                    throw ex;
                }
            }

            //
            // Force handshaking, so that we get any authentication.
            // Register a handshake callback so our session state tracks any
            // later session renegotiations.
            //
            String [] protocols = getProtocols();
            String [] ciphers = getCipherSuites();
            if (protocols != null) {
                s.setEnabledProtocols(protocols);
            }
            if (ciphers != null) {
                s.setEnabledCipherSuites(ciphers);
            }
            s.addHandshakeCompletedListener(this);

            // We have two hostname verification approaches. One is in
            // SSL/TLS socket layer, where the algorithm is configured with
            // SSLParameters.setEndpointIdentificationAlgorithm(), and the
            // hostname verification is done by X509ExtendedTrustManager when
            // the algorithm is "HTTPS". The other one is in HTTPS layer,
            // where the algorithm is customized by
            // HttpsURLConnection.setHostnameVerifier(), and the hostname
            // verification is done by HostnameVerifier when the default
            // rules for hostname verification fail.
            //
            // The relationship between two hostname verification approaches
            // likes the following:
            //
            //               |             EIA algorithm
            //               +----------------------------------------------
            //               |     null      |   HTTPS    |   LDAP/other   |
            // -------------------------------------------------------------
            //     |         |1              |2           |3               |
            // HNV | default | Set HTTPS EIA | use EIA    | HTTPS          |
            //     |--------------------------------------------------------
            //     | non -   |4              |5           |6               |
            //     | default | HTTPS/HNV     | use EIA    | HTTPS/HNV      |
            // -------------------------------------------------------------
            //
            // Abbreviation:
            //     EIA: the endpoint identification algorithm in SSL/TLS
            //           socket layer
            //     HNV: the hostname verification object in HTTPS layer
            // Notes:
            //     case 1. default HNV and EIA is null
            //           Set EIA as HTTPS, hostname check done in SSL/TLS
            //           layer.
            //     case 2. default HNV and EIA is HTTPS
            //           Use existing EIA, hostname check done in SSL/TLS
            //           layer.
            //     case 3. default HNV and EIA is other than HTTPS
            //           Use existing EIA, EIA check done in SSL/TLS
            //           layer, then do HTTPS check in HTTPS layer.
            //     case 4. non-default HNV and EIA is null
            //           No EIA, no EIA check done in SSL/TLS layer, then do
            //           HTTPS check in HTTPS layer using HNV as override.
            //     case 5. non-default HNV and EIA is HTTPS
            //           Use existing EIA, hostname check done in SSL/TLS
            //           layer. No HNV override possible. We will review this
            //           decision and may update the architecture for JDK 7.
            //     case 6. non-default HNV and EIA is other than HTTPS
            //           Use existing EIA, EIA check done in SSL/TLS layer,
            //           then do HTTPS check in HTTPS layer as override.
            boolean needToCheckSpoofing = true;
            String identification =
                s.getSSLParameters().getEndpointIdentificationAlgorithm();
            if (identification != null && identification.length() != 0) {
                if (identification.equalsIgnoreCase("HTTPS")) {
                    // Do not check server identity again out of SSLSocket,
                    // the endpoint will be identified during TLS handshaking
                    // in SSLSocket.
                    needToCheckSpoofing = false;
                }   // else, we don't understand the identification algorithm,
                    // need to check URL spoofing here.
            } else {
                boolean isDefaultHostnameVerifier = false;

                // We prefer to let the SSLSocket do the spoof checks, but if
                // the application has specified a HostnameVerifier (HNV),
                // we will always use that.
                if (hv != null) {
                    String canonicalName = hv.getClass().getCanonicalName();
                    if (canonicalName != null &&
                    canonicalName.equalsIgnoreCase(defaultHVCanonicalName)) {
                        isDefaultHostnameVerifier = true;
                    }
                } else {
                    // Unlikely to happen! As the behavior is the same as the
                    // default hostname verifier, so we prefer to let the
                    // SSLSocket do the spoof checks.
                    isDefaultHostnameVerifier = true;
                }

                if (isDefaultHostnameVerifier) {
                    // If the HNV is the default from HttpsURLConnection, we
                    // will do the spoof checks in SSLSocket.
                    SSLParameters paramaters = s.getSSLParameters();
                    paramaters.setEndpointIdentificationAlgorithm("HTTPS");
                    // host has been set previously for SSLSocketImpl
                    if (!(s instanceof SSLSocketImpl)) {
                        paramaters.setServerNames(List.of(new SNIHostName(host)));
                    }
                    s.setSSLParameters(paramaters);

                    needToCheckSpoofing = false;
                }
            }

            s.startHandshake();
            session = s.getSession();
            // change the serverSocket and serverOutput
            serverSocket = s;
            try {
                serverOutput = new PrintStream(
                    new BufferedOutputStream(serverSocket.getOutputStream()),
                    false, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new InternalError(encoding+" encoding not found");
            }

            // check URL spoofing if it has not been checked under handshaking
            if (needToCheckSpoofing) {
                checkURLSpoofing(hv);
            }
        } else {
            // if we are reusing a cached https session,
            // we don't need to do handshaking etc. But we do need to
            // set the ssl session
            session = ((SSLSocket)serverSocket).getSession();
        }
    }

    // Server identity checking is done according to RFC 2818: HTTP over TLS
    // Section 3.1 Server Identity
    private void checkURLSpoofing(HostnameVerifier hostnameVerifier)
            throws IOException {
        //
        // Get authenticated server name, if any
        //
        String host = url.getHost();

        // if IPv6 strip off the "[]"
        if (host != null && host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length()-1);
        }

        Certificate[] peerCerts = null;
        String cipher = session.getCipherSuite();
        try {
            HostnameChecker checker = HostnameChecker.getInstance(
                                                HostnameChecker.TYPE_TLS);

            // get the subject's certificate
            peerCerts = session.getPeerCertificates();

            X509Certificate peerCert;
            if (peerCerts[0] instanceof
                    java.security.cert.X509Certificate) {
                peerCert = (java.security.cert.X509Certificate)peerCerts[0];
            } else {
                throw new SSLPeerUnverifiedException("");
            }
            checker.match(host, peerCert);

            // if it doesn't throw an exception, we passed. Return.
            return;

        } catch (SSLPeerUnverifiedException e) {

            //
            // client explicitly changed default policy and enabled
            // anonymous ciphers; we can't check the standard policy
            //
            // ignore
        } catch (java.security.cert.CertificateException cpe) {
            // ignore
        }

        if ((cipher != null) && (cipher.indexOf("_anon_") != -1)) {
            return;
        } else if ((hostnameVerifier != null) &&
                   (hostnameVerifier.verify(host, session))) {
            return;
        }

        serverSocket.close();
        session.invalidate();

        throw new IOException("HTTPS hostname wrong:  should be <"
                              + url.getHost() + ">");
    }

    @Override
    protected void putInKeepAliveCache() {
        if (inCache) {
            assert false : "Duplicate put to keep alive cache";
            return;
        }
        inCache = true;
        kac.put(url, sslSocketFactory, this);
    }

    /*
     * Close an idle connection to this URL (if it exists in the cache).
     */
    @Override
    public void closeIdleConnection() {
        HttpClient http = kac.get(url, sslSocketFactory);
        if (http != null) {
            http.closeServer();
        }
    }

    /**
     * Returns the cipher suite in use on this connection.
     */
    String getCipherSuite() {
        return session.getCipherSuite();
    }

    /**
     * Returns the certificate chain the client sent to the
     * server, or null if the client did not authenticate.
     */
    public java.security.cert.Certificate [] getLocalCertificates() {
        return session.getLocalCertificates();
    }

    /**
     * Returns the certificate chain with which the server
     * authenticated itself, or throw a SSLPeerUnverifiedException
     * if the server did not authenticate.
     */
    java.security.cert.Certificate [] getServerCertificates()
            throws SSLPeerUnverifiedException
    {
        return session.getPeerCertificates();
    }

    /**
     * Returns the principal with which the server authenticated
     * itself, or throw a SSLPeerUnverifiedException if the
     * server did not authenticate.
     */
    Principal getPeerPrincipal()
            throws SSLPeerUnverifiedException
    {
        Principal principal;
        try {
            principal = session.getPeerPrincipal();
        } catch (AbstractMethodError e) {
            // if the provider does not support it, fallback to peer certs.
            // return the X500Principal of the end-entity cert.
            java.security.cert.Certificate[] certs =
                        session.getPeerCertificates();
            principal = ((X509Certificate)certs[0]).getSubjectX500Principal();
        }
        return principal;
    }

    /**
     * Returns the principal the client sent to the
     * server, or null if the client did not authenticate.
     */
    Principal getLocalPrincipal()
    {
        Principal principal;
        try {
            principal = session.getLocalPrincipal();
        } catch (AbstractMethodError e) {
            principal = null;
            // if the provider does not support it, fallback to local certs.
            // return the X500Principal of the end-entity cert.
            java.security.cert.Certificate[] certs =
                        session.getLocalCertificates();
            if (certs != null) {
                principal = ((X509Certificate)certs[0]).getSubjectX500Principal();
            }
        }
        return principal;
    }

    /**
     * Returns the {@code SSLSession} in use on this connection.
     */
    SSLSession getSSLSession() {
        return session;
    }

    /**
     * This method implements the SSL HandshakeCompleted callback,
     * remembering the resulting session so that it may be queried
     * for the current cipher suite and peer certificates.  Servers
     * sometimes re-initiate handshaking, so the session in use on
     * a given connection may change.  When sessions change, so may
     * peer identities and cipher suites.
     */
    public void handshakeCompleted(HandshakeCompletedEvent event)
    {
        session = event.getSession();
    }

    /**
     * @return the proxy host being used for this client, or null
     *          if we're not going through a proxy
     */
    @Override
    public String getProxyHostUsed() {
        if (!needsTunneling()) {
            return null;
        } else {
            return super.getProxyHostUsed();
        }
    }

    /**
     * @return the proxy port being used for this client.  Meaningless
     *          if getProxyHostUsed() gives null.
     */
    @Override
    public int getProxyPortUsed() {
        return (proxy == null || proxy.type() == Proxy.Type.DIRECT ||
                proxy.type() == Proxy.Type.SOCKS)? -1:
            ((InetSocketAddress)proxy.address()).getPort();
    }
}
