/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.net.ssl;

import java.net.URL;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * <code>HttpsURLConnection</code> extends <code>HttpURLConnection</code>
 * with support for https-specific features.
 * <P>
 * See <A HREF="http://www.w3.org/pub/WWW/Protocols/">
 * http://www.w3.org/pub/WWW/Protocols/</A> and
 * <A HREF="http://www.ietf.org/"> RFC 2818 </A>
 * for more details on the
 * https specification.
 * <P>
 * This class uses <code>HostnameVerifier</code> and
 * <code>SSLSocketFactory</code>.
 * There are default implementations defined for both classes.
 * However, the implementations can be replaced on a per-class (static) or
 * per-instance basis.  All new <code>HttpsURLConnection</code>s instances
 * will be assigned
 * the "default" static values at instance creation, but they can be overridden
 * by calling the appropriate per-instance set method(s) before
 * <code>connect</code>ing.
 *
 * @since 1.4
 */
public abstract class HttpsURLConnection extends HttpURLConnection {
    /**
     * Creates an <code>HttpsURLConnection</code> using the
     * URL specified.
     *
     * @param url the URL
     */
    protected HttpsURLConnection(URL url) {
        super(url);
    }

    /**
     * Returns the cipher suite in use on this connection.
     *
     * @return the cipher suite
     * @throws IllegalStateException if this method is called before
     *          the connection has been established.
     */
    public abstract String getCipherSuite();

    /**
     * Returns the certificate(s) that were sent to the server during
     * handshaking.
     * <P>
     * Note: This method is useful only when using certificate-based
     * cipher suites.
     * <P>
     * When multiple certificates are available for use in a
     * handshake, the implementation chooses what it considers the
     * "best" certificate chain available, and transmits that to
     * the other side.  This method allows the caller to know
     * which certificate chain was actually sent.
     *
     * @return an ordered array of certificates,
     *          with the client's own certificate first followed by any
     *          certificate authorities.  If no certificates were sent,
     *          then null is returned.
     * @throws IllegalStateException if this method is called before
     *          the connection has been established.
     * @see #getLocalPrincipal()
     */
    public abstract java.security.cert.Certificate [] getLocalCertificates();

    /**
     * Returns the server's certificate chain which was established
     * as part of defining the session.
     * <P>
     * Note: This method can be used only when using certificate-based
     * cipher suites; using it with non-certificate-based cipher suites,
     * such as Kerberos, will throw an SSLPeerUnverifiedException.
     * <P>
     * Note: The returned value may not be a valid certificate chain
     * and should not be relied on for trust decisions.
     *
     * @return an ordered array of server certificates,
     *          with the peer's own certificate first followed by
     *          any certificate authorities.
     * @throws SSLPeerUnverifiedException if the peer is not verified.
     * @throws IllegalStateException if this method is called before
     *          the connection has been established.
     * @see #getPeerPrincipal()
     */
    public abstract java.security.cert.Certificate [] getServerCertificates()
            throws SSLPeerUnverifiedException;

    /**
     * Returns the server's principal which was established as part of
     * defining the session.
     * <P>
     * Note: Subclasses should override this method. If not overridden, it
     * will default to returning the X500Principal of the server's end-entity
     * certificate for certificate-based ciphersuites, or throw an
     * SSLPeerUnverifiedException for non-certificate based ciphersuites,
     * such as Kerberos.
     *
     * @return the server's principal. Returns an X500Principal of the
     * end-entity certiticate for X509-based cipher suites, and
     * KerberosPrincipal for Kerberos cipher suites.
     *
     * @throws SSLPeerUnverifiedException if the peer was not verified
     * @throws IllegalStateException if this method is called before
     *          the connection has been established.
     *
     * @see #getServerCertificates()
     * @see #getLocalPrincipal()
     *
     * @since 1.5
     */
    public Principal getPeerPrincipal()
            throws SSLPeerUnverifiedException {

        java.security.cert.Certificate[] certs = getServerCertificates();
        return ((X509Certificate)certs[0]).getSubjectX500Principal();
    }

    /**
     * Returns the principal that was sent to the server during handshaking.
     * <P>
     * Note: Subclasses should override this method. If not overridden, it
     * will default to returning the X500Principal of the end-entity certificate
     * that was sent to the server for certificate-based ciphersuites or,
     * return null for non-certificate based ciphersuites, such as Kerberos.
     *
     * @return the principal sent to the server. Returns an X500Principal
     * of the end-entity certificate for X509-based cipher suites, and
     * KerberosPrincipal for Kerberos cipher suites. If no principal was
     * sent, then null is returned.
     *
     * @throws IllegalStateException if this method is called before
     *          the connection has been established.
     *
     * @see #getLocalCertificates()
     * @see #getPeerPrincipal()
     *
     * @since 1.5
     */
    public Principal getLocalPrincipal() {

        java.security.cert.Certificate[] certs = getLocalCertificates();
        if (certs != null) {
            return ((X509Certificate)certs[0]).getSubjectX500Principal();
        } else {
            return null;
        }
    }

    /**
     * <code>HostnameVerifier</code> provides a callback mechanism so that
     * implementers of this interface can supply a policy for
     * handling the case where the host to connect to and
     * the server name from the certificate mismatch.
     * <p>
     * The default implementation will deny such connections.
     */
    private static HostnameVerifier defaultHostnameVerifier =
                                        new DefaultHostnameVerifier();

    /*
     * The initial default <code>HostnameVerifier</code>.  Should be
     * updated for another other type of <code>HostnameVerifier</code>
     * that are created.
     */
    private static class DefaultHostnameVerifier
            implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return false;
        }
    }

    /**
     * The <code>hostnameVerifier</code> for this object.
     */
    protected HostnameVerifier hostnameVerifier = defaultHostnameVerifier;

    /**
     * Sets the default <code>HostnameVerifier</code> inherited by a
     * new instance of this class.
     * <P>
     * If this method is not called, the default
     * <code>HostnameVerifier</code> assumes the connection should not
     * be permitted.
     *
     * @param v the default host name verifier
     * @throws IllegalArgumentException if the <code>HostnameVerifier</code>
     *          parameter is null.
     * @throws SecurityException if a security manager exists and its
     *         <code>checkPermission</code> method does not allow
     *         <code>SSLPermission("setHostnameVerifier")</code>
     * @see #getDefaultHostnameVerifier()
     */
    public static void setDefaultHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException(
                "no default HostnameVerifier specified");
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SSLPermission("setHostnameVerifier"));
        }
        defaultHostnameVerifier = v;
    }

    /**
     * Gets the default <code>HostnameVerifier</code> that is inherited
     * by new instances of this class.
     *
     * @return the default host name verifier
     * @see #setDefaultHostnameVerifier(HostnameVerifier)
     */
    public static HostnameVerifier getDefaultHostnameVerifier() {
        return defaultHostnameVerifier;
    }

    /**
     * Sets the <code>HostnameVerifier</code> for this instance.
     * <P>
     * New instances of this class inherit the default static hostname
     * verifier set by {@link #setDefaultHostnameVerifier(HostnameVerifier)
     * setDefaultHostnameVerifier}.  Calls to this method replace
     * this object's <code>HostnameVerifier</code>.
     *
     * @param v the host name verifier
     * @throws IllegalArgumentException if the <code>HostnameVerifier</code>
     *  parameter is null.
     * @see #getHostnameVerifier()
     * @see #setDefaultHostnameVerifier(HostnameVerifier)
     */
    public void setHostnameVerifier(HostnameVerifier v) {
        if (v == null) {
            throw new IllegalArgumentException(
                "no HostnameVerifier specified");
        }

        hostnameVerifier = v;
    }

    /**
     * Gets the <code>HostnameVerifier</code> in place on this instance.
     *
     * @return the host name verifier
     * @see #setHostnameVerifier(HostnameVerifier)
     * @see #setDefaultHostnameVerifier(HostnameVerifier)
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    private static SSLSocketFactory defaultSSLSocketFactory = null;

    /**
     * The <code>SSLSocketFactory</code> inherited when an instance
     * of this class is created.
     */
    private SSLSocketFactory sslSocketFactory = getDefaultSSLSocketFactory();

    /**
     * Sets the default <code>SSLSocketFactory</code> inherited by new
     * instances of this class.
     * <P>
     * The socket factories are used when creating sockets for secure
     * https URL connections.
     *
     * @param sf the default SSL socket factory
     * @throws IllegalArgumentException if the SSLSocketFactory
     *          parameter is null.
     * @throws SecurityException if a security manager exists and its
     *         <code>checkSetFactory</code> method does not allow
     *         a socket factory to be specified.
     * @see #getDefaultSSLSocketFactory()
     */
    public static void setDefaultSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException(
                "no default SSLSocketFactory specified");
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkSetFactory();
        }
        defaultSSLSocketFactory = sf;
    }

    /**
     * Gets the default static <code>SSLSocketFactory</code> that is
     * inherited by new instances of this class.
     * <P>
     * The socket factories are used when creating sockets for secure
     * https URL connections.
     *
     * @return the default <code>SSLSocketFactory</code>
     * @see #setDefaultSSLSocketFactory(SSLSocketFactory)
     */
    public static SSLSocketFactory getDefaultSSLSocketFactory() {
        if (defaultSSLSocketFactory == null) {
            defaultSSLSocketFactory =
                (SSLSocketFactory)SSLSocketFactory.getDefault();
        }
        return defaultSSLSocketFactory;
    }

    /**
     * Sets the <code>SSLSocketFactory</code> to be used when this instance
     * creates sockets for secure https URL connections.
     * <P>
     * New instances of this class inherit the default static
     * <code>SSLSocketFactory</code> set by
     * {@link #setDefaultSSLSocketFactory(SSLSocketFactory)
     * setDefaultSSLSocketFactory}.  Calls to this method replace
     * this object's <code>SSLSocketFactory</code>.
     *
     * @param sf the SSL socket factory
     * @throws IllegalArgumentException if the <code>SSLSocketFactory</code>
     *          parameter is null.
     * @throws SecurityException if a security manager exists and its
     *         <code>checkSetFactory</code> method does not allow
     *         a socket factory to be specified.
     * @see #getSSLSocketFactory()
     */
    public void setSSLSocketFactory(SSLSocketFactory sf) {
        if (sf == null) {
            throw new IllegalArgumentException(
                "no SSLSocketFactory specified");
        }

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkSetFactory();
        }
        sslSocketFactory = sf;
    }

    /**
     * Gets the SSL socket factory to be used when creating sockets
     * for secure https URL connections.
     *
     * @return the <code>SSLSocketFactory</code>
     * @see #setSSLSocketFactory(SSLSocketFactory)
     */
    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Returns an {@link Optional} containing the {@code SSLSession} in
     * use on this connection.  Returns an empty {@code Optional} if the
     * underlying implementation does not support this method.
     *
     * @implSpec For compatibility, the default implementation of this
     *           method returns an empty {@code Optional}.  Subclasses
     *           should override this method with an appropriate
     *           implementation since an application may need to access
     *           additional parameters associated with the SSL session.
     *
     * @return   an {@link Optional} containing the {@code SSLSession} in
     *           use on this connection.
     *
     * @throws   IllegalStateException if this method is called before
     *           the connection has been established
     *
     * @see SSLSession
     *
     * @since 12
     */
    public Optional<SSLSession> getSSLSession() {
        return Optional.empty();
    }
}
