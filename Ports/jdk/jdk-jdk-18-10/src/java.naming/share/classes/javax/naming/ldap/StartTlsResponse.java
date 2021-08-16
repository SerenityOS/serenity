/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap;

import java.io.IOException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;

/**
 * This class implements the LDAPv3 Extended Response for StartTLS as
 * defined in
 * <a href="http://www.ietf.org/rfc/rfc2830.txt">Lightweight Directory
 * Access Protocol (v3): Extension for Transport Layer Security</a>
 *
 * The object identifier for StartTLS is 1.3.6.1.4.1.1466.20037
 * and no extended response value is defined.
 *
 *<p>
 * The Start TLS extended request and response are used to establish
 * a TLS connection over the existing LDAP connection associated with
 * the JNDI context on which {@code extendedOperation()} is invoked.
 * Typically, a JNDI program uses the StartTLS extended request and response
 * classes as follows.
 * <blockquote><pre>
 * import javax.naming.ldap.*;
 *
 * // Open an LDAP association
 * LdapContext ctx = new InitialLdapContext();
 *
 * // Perform a StartTLS extended operation
 * StartTlsResponse tls =
 *     (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
 *
 * // Open a TLS connection (over the existing LDAP association) and get details
 * // of the negotiated TLS session: cipher suite, peer certificate, ...
 * SSLSession session = tls.negotiate();
 *
 * // ... use ctx to perform protected LDAP operations
 *
 * // Close the TLS connection (revert back to the underlying LDAP association)
 * tls.close();
 *
 * // ... use ctx to perform unprotected LDAP operations
 *
 * // Close the LDAP association
 * ctx.close;
 * </pre></blockquote>
 *
 * @since 1.4
 * @see StartTlsRequest
 * @author Vincent Ryan
 */
public abstract class StartTlsResponse implements ExtendedResponse {

    // Constant

    /**
     * The StartTLS extended response's assigned object identifier
     * is 1.3.6.1.4.1.1466.20037.
     */
    public static final String OID = "1.3.6.1.4.1.1466.20037";


    // Called by subclass

    /**
     * Constructs a StartTLS extended response.
     * A concrete subclass must have a public no-arg constructor.
     */
    protected StartTlsResponse() {
    }


    // ExtendedResponse methods

    /**
     * Retrieves the StartTLS response's object identifier string.
     *
     * @return The object identifier string, "1.3.6.1.4.1.1466.20037".
     */
    public String getID() {
        return OID;
    }

    /**
     * Retrieves the StartTLS response's ASN.1 BER encoded value.
     * Since the response has no defined value, null is always
     * returned.
     *
     * @return The null value.
     */
    public byte[] getEncodedValue() {
        return null;
    }

    // StartTls-specific methods

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
    public abstract void setEnabledCipherSuites(String[] suites);

    /**
     * Sets the hostname verifier used by {@code negotiate()}
     * after the TLS handshake has completed and the default hostname
     * verification has failed.
     * {@code setHostnameVerifier()} must be called before
     * {@code negotiate()} is invoked for it to have effect.
     * If called after
     * {@code negotiate()}, this method does not do anything.
     *
     * @param verifier The non-null hostname verifier callback.
     * @see #negotiate
     */
    public abstract void setHostnameVerifier(HostnameVerifier verifier);

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
    public abstract SSLSession negotiate() throws IOException;

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
     * The default hostname verification performs a match of the server's
     * hostname against the hostname information found in the server's certificate.
     * If this verification fails and no callback has been set via
     * {@code setHostnameVerifier} then the negotiation fails.
     * If this verification fails and a callback has been set via
     * {@code setHostnameVerifier}, then the callback is used to determine whether
     * the negotiation succeeds.
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
    public abstract SSLSession negotiate(SSLSocketFactory factory)
        throws IOException;

    /**
     * Closes the TLS connection gracefully and reverts back to the underlying
     * connection.
     *
     * @throws IOException If an IO error was encountered while closing the
     * TLS connection
     */
    public abstract void close() throws IOException;

    private static final long serialVersionUID = 8372842182579276418L;
}
