/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap.ext;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.HostnameVerifier;
import sun.security.util.HostnameChecker;

import javax.naming.ldap.*;
import com.sun.jndi.ldap.Connection;

/**
 * This class implements the LDAPv3 Extended Response for StartTLS as
 * defined in
 * <a href="http://www.ietf.org/rfc/rfc2830.txt">Lightweight Directory
 * Access Protocol (v3): Extension for Transport Layer Security</a>
 *
 * The object identifier for StartTLS is 1.3.6.1.4.1.1466.20037
 * and no extended response value is defined.
 *
 * <p>
 * The Start TLS extended request and response are used to establish
 * a TLS connection over the existing LDAP connection associated with
 * the JNDI context on which {@code extendedOperation()} is invoked.
 *
 * @see StartTlsRequest
 * @author Vincent Ryan
 */
public final class StartTlsResponseImpl extends StartTlsResponse {

    private static final boolean debug = false;

    /*
     * The dNSName type in a subjectAltName extension of an X.509 certificate
     */
    private static final int DNSNAME_TYPE = 2;

    /*
     * The server's hostname.
     */
    private transient String hostname = null;

    /*
     * The LDAP socket.
     */
    private transient Connection ldapConnection = null;

    /*
     * The original input stream.
     */
    private transient InputStream originalInputStream = null;

    /*
     * The original output stream.
     */
    private transient OutputStream originalOutputStream = null;

    /*
     * The SSL socket.
     */
    private transient SSLSocket sslSocket = null;

    /*
     * The SSL socket factories.
     */
    private transient SSLSocketFactory defaultFactory = null;
    private transient SSLSocketFactory currentFactory = null;

    /*
     * The list of cipher suites to be enabled.
     */
    private transient String[] suites = null;

    /*
     * The hostname verifier callback.
     */
    private transient HostnameVerifier verifier = null;

    /*
     * The flag to indicate that the TLS connection is closed.
     */
    private transient boolean isClosed = true;

    private static final long serialVersionUID = -1126624615143411328L;

    // public no-arg constructor required by JDK's Service Provider API.

    public StartTlsResponseImpl() {}

    /**
     * Overrides the default list of cipher suites enabled for use on the
     * TLS connection. The cipher suites must have already been listed by
     * {@code SSLSocketFactory.getSupportedCipherSuites()} as being supported.
     * Even if a suite has been enabled, it still might not be used because
     * the peer does not support it, or because the requisite certificates
     * (and private keys) are not available.
     *
     * @param suites The non-null list of names of all the cipher suites to
     * enable.
     * @see #negotiate
     */
    public void setEnabledCipherSuites(String[] suites) {
        // The impl does accept null suites, although the spec requires
        // a non-null list.
        this.suites = suites == null ? null : suites.clone();
    }

    /**
     * Overrides the default hostname verifier used by {@code negotiate()}
     * after the TLS handshake has completed. If
     * {@code setHostnameVerifier()} has not been called before
     * {@code negotiate()} is invoked, {@code negotiate()}
     * will perform a simple case ignore match. If called after
     * {@code negotiate()}, this method does not do anything.
     *
     * @param verifier The non-null hostname verifier callback.
     * @see #negotiate
     */
    public void setHostnameVerifier(HostnameVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * Negotiates a TLS session using the default SSL socket factory.
     * <p>
     * This method is equivalent to {@code negotiate(null)}.
     *
     * @return The negotiated SSL session
     * @throws IOException If an IO error was encountered while establishing
     * the TLS session.
     * @see #setEnabledCipherSuites
     * @see #setHostnameVerifier
     */
    public SSLSession negotiate() throws IOException {

        return negotiate(null);
    }

    /**
     * Negotiates a TLS session using an SSL socket factory.
     * <p>
     * Creates an SSL socket using the supplied SSL socket factory and
     * attaches it to the existing connection. Performs the TLS handshake
     * and returns the negotiated session information.
     * <p>
     * If cipher suites have been set via {@code setEnabledCipherSuites}
     * then they are enabled before the TLS handshake begins.
     * <p>
     * Hostname verification is performed after the TLS handshake completes.
     * The default check performs a case insensitive match of the server's
     * hostname against that in the server's certificate. The server's
     * hostname is extracted from the subjectAltName in the server's
     * certificate (if present). Otherwise the value of the common name
     * attribute of the subject name is used. If a callback has
     * been set via {@code setHostnameVerifier} then that verifier is used if
     * the default check fails.
     * <p>
     * If an error occurs then the SSL socket is closed and an IOException
     * is thrown. The underlying connection remains intact.
     *
     * @param factory The possibly null SSL socket factory to use.
     * If null, the default SSL socket factory is used.
     * @return The negotiated SSL session
     * @throws IOException If an IO error was encountered while establishing
     * the TLS session.
     * @see #setEnabledCipherSuites
     * @see #setHostnameVerifier
     */
    public SSLSession negotiate(SSLSocketFactory factory) throws IOException {

        if (isClosed && sslSocket != null) {
            throw new IOException("TLS connection is closed.");
        }

        if (factory == null) {
            factory = getDefaultFactory();
        }

        if (debug) {
            System.out.println("StartTLS: About to start handshake");
        }

        SSLSession sslSession = startHandshake(factory).getSession();

        if (debug) {
            System.out.println("StartTLS: Completed handshake");
        }

        SSLPeerUnverifiedException verifExcep = null;
        try {
            if (verify(hostname, sslSession)) {
                isClosed = false;
                return sslSession;
            }
        } catch (SSLPeerUnverifiedException e) {
            // Save to return the cause
            verifExcep = e;
        }
        if ((verifier != null) &&
                verifier.verify(hostname, sslSession)) {
            isClosed = false;
            return sslSession;
        }

        // Verification failed
        close();
        sslSession.invalidate();
        if (verifExcep == null) {
            verifExcep = new SSLPeerUnverifiedException(
                        "hostname of the server '" + hostname +
                        "' does not match the hostname in the " +
                        "server's certificate.");
        }
        throw verifExcep;
    }

    /**
     * Closes the TLS connection gracefully and reverts back to the underlying
     * connection.
     *
     * @throws IOException If an IO error was encountered while closing the
     * TLS connection
     */
    public void close() throws IOException {

        if (isClosed) {
            return;
        }

        if (debug) {
            System.out.println("StartTLS: replacing SSL " +
                                "streams with originals");
        }

        // Replace SSL streams with the original streams
        ldapConnection.replaceStreams(
                originalInputStream, originalOutputStream, false);

        if (debug) {
            System.out.println("StartTLS: closing SSL Socket");
        }
        sslSocket.close();

        isClosed = true;
    }

    /**
     * Sets the connection for TLS to use. The TLS connection will be attached
     * to this connection.
     *
     * @param ldapConnection The non-null connection to use.
     * @param hostname The server's hostname. If null, the hostname used to
     * open the connection will be used instead.
     */
    public void setConnection(Connection ldapConnection, String hostname) {
        this.ldapConnection = ldapConnection;
        this.hostname = (hostname == null || hostname.isEmpty())
            ? ldapConnection.host : hostname;
        originalInputStream = ldapConnection.inStream;
        originalOutputStream = ldapConnection.outStream;
    }

    /*
     * Returns the default SSL socket factory.
     *
     * @return The default SSL socket factory.
     * @throws IOException If TLS is not supported.
     */
    private SSLSocketFactory getDefaultFactory() throws IOException {

        if (defaultFactory != null) {
            return defaultFactory;
        }

        return (defaultFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault());
    }

    /*
     * Start the TLS handshake and manipulate the input and output streams.
     *
     * @param factory The SSL socket factory to use.
     * @return The SSL socket.
     * @throws IOException If an exception occurred while performing the
     * TLS handshake.
     */
    private SSLSocket startHandshake(SSLSocketFactory factory)
        throws IOException {

        if (ldapConnection == null) {
            throw new IllegalStateException("LDAP connection has not been set."
                + " TLS requires an existing LDAP connection.");
        }

        if (factory != currentFactory) {
            // Create SSL socket layered over the existing connection
            sslSocket = (SSLSocket) factory.createSocket(ldapConnection.sock,
                ldapConnection.host, ldapConnection.port, false);
            currentFactory = factory;

            if (debug) {
                System.out.println("StartTLS: Created socket : " + sslSocket);
            }
        }

        if (suites != null) {
            sslSocket.setEnabledCipherSuites(suites);
            if (debug) {
                System.out.println("StartTLS: Enabled cipher suites");
            }
        }

        // Connection must be quite for handshake to proceed

        try {
            if (debug) {
                System.out.println(
                        "StartTLS: Calling sslSocket.startHandshake");
            }
            ldapConnection.setHandshakeCompletedListener(sslSocket);
            sslSocket.startHandshake();
            if (debug) {
                System.out.println(
                        "StartTLS: + Finished sslSocket.startHandshake");
            }

            // Replace original streams with the new SSL streams
            ldapConnection.replaceStreams(sslSocket.getInputStream(),
                    sslSocket.getOutputStream(), true);
            if (debug) {
                System.out.println("StartTLS: Replaced IO Streams");
            }

        } catch (IOException e) {
            if (debug) {
                System.out.println("StartTLS: Got IO error during handshake");
                e.printStackTrace();
            }

            sslSocket.close();
            isClosed = true;
            throw e;   // pass up exception
        }

        return sslSocket;
    }

    /*
     * Verifies that the hostname in the server's certificate matches the
     * hostname of the server.
     * The server's first certificate is examined. If it has a subjectAltName
     * that contains a dNSName then that is used as the server's hostname.
     * The server's hostname may contain a wildcard for its left-most name part.
     * Otherwise, if the certificate has no subjectAltName then the value of
     * the common name attribute of the subject name is used.
     *
     * @param hostname The hostname of the server.
     * @param session the SSLSession used on the connection to host.
     * @return true if the hostname is verified, false otherwise.
     */

    private boolean verify(String hostname, SSLSession session)
        throws SSLPeerUnverifiedException {

        java.security.cert.Certificate[] certs = null;

        // if IPv6 strip off the "[]"
        if (hostname != null && hostname.startsWith("[") &&
                hostname.endsWith("]")) {
            hostname = hostname.substring(1, hostname.length() - 1);
        }
        try {
            HostnameChecker checker = HostnameChecker.getInstance(
                                                HostnameChecker.TYPE_LDAP);
            // get the subject's certificate
            certs = session.getPeerCertificates();
            X509Certificate peerCert;
            if (certs[0] instanceof java.security.cert.X509Certificate) {
                peerCert = (java.security.cert.X509Certificate) certs[0];
            } else {
                throw new SSLPeerUnverifiedException(
                        "Received a non X509Certificate from the server");
            }
            checker.match(hostname, peerCert);

            // no exception means verification passed
            return true;
        } catch (SSLPeerUnverifiedException e) {

            /*
             * The application may enable an anonymous SSL cipher suite, and
             * hostname verification is not done for anonymous ciphers
             */
            String cipher = session.getCipherSuite();
            if (cipher != null && (cipher.indexOf("_anon_") != -1)) {
                return true;
            }
            throw e;
        } catch (CertificateException e) {

            /*
             * Pass up the cause of the failure
             */
            throw(SSLPeerUnverifiedException)
                new SSLPeerUnverifiedException("hostname of the server '" +
                                hostname +
                                "' does not match the hostname in the " +
                                "server's certificate.").initCause(e);
        }
    }

    /*
     * Get the peer principal from the session
     */
    private static Principal getPeerPrincipal(SSLSession session)
            throws SSLPeerUnverifiedException {
        Principal principal;
        try {
            principal = session.getPeerPrincipal();
        } catch (AbstractMethodError e) {
            // if the JSSE provider does not support it, return null, since
            // we need it only for Kerberos.
            principal = null;
        }
        return principal;
    }
}
