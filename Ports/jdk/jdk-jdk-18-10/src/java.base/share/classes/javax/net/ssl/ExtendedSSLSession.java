/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * Extends the {@code SSLSession} interface to support additional
 * session attributes.
 *
 * @since 1.7
 */
public abstract class ExtendedSSLSession implements SSLSession {
    /**
     * Constructor for subclasses to call.
     */
    public ExtendedSSLSession() {}

    /**
     * Obtains an array of supported signature algorithms that the local side
     * is willing to use.
     * <p>
     * Note: this method is used to indicate to the peer which signature
     * algorithms may be used for digital signatures in TLS/DTLS 1.2. It is
     * not meaningful for TLS/DTLS versions prior to 1.2.
     * <p>
     * The signature algorithm name must be a standard Java Security
     * name (such as "SHA1withRSA", "SHA256withECDSA", and so on).
     * See the <a href=
     * "{@docRoot}/../specs/security/standard-names.html">
     * Java Security Standard Algorithm Names</a> document
     * for information about standard algorithm names.
     * <p>
     * Note: the local supported signature algorithms should conform to
     * the algorithm constraints specified by
     * {@link SSLParameters#getAlgorithmConstraints getAlgorithmConstraints()}
     * method in {@code SSLParameters}.
     *
     * @return An array of supported signature algorithms, in descending
     *     order of preference.  The return value is an empty array if
     *     no signature algorithm is supported.
     *
     * @see SSLParameters#getAlgorithmConstraints
     */
    public abstract String[] getLocalSupportedSignatureAlgorithms();

    /**
     * Obtains an array of supported signature algorithms that the peer is
     * able to use.
     * <p>
     * Note: this method is used to indicate to the local side which signature
     * algorithms may be used for digital signatures in TLS/DTLS 1.2. It is
     * not meaningful for TLS/DTLS versions prior to 1.2.
     * <p>
     * The signature algorithm name must be a standard Java Security
     * name (such as "SHA1withRSA", "SHA256withECDSA", and so on).
     * See the <a href=
     * "{@docRoot}/../specs/security/standard-names.html">
     * Java Security Standard Algorithm Names</a> document
     * for information about standard algorithm names.
     *
     * @return An array of supported signature algorithms, in descending
     *     order of preference.  The return value is an empty array if
     *     the peer has not sent the supported signature algorithms.
     *
     * @see X509KeyManager
     * @see X509ExtendedKeyManager
     */
    public abstract String[] getPeerSupportedSignatureAlgorithms();

    /**
     * Obtains a {@link List} containing all {@link SNIServerName}s
     * of the requested Server Name Indication (SNI) extension.
     * <P>
     * In server mode, unless the return {@link List} is empty,
     * the server should use the requested server names to guide its
     * selection of an appropriate authentication certificate, and/or
     * other aspects of security policy.
     * <P>
     * In client mode, unless the return {@link List} is empty,
     * the client should use the requested server names to guide its
     * endpoint identification of the peer's identity, and/or
     * other aspects of security policy.
     *
     * @return a non-null immutable list of {@link SNIServerName}s of the
     *         requested server name indications. The returned list may be
     *         empty if no server name indications were requested.
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation
     *
     * @see SNIServerName
     * @see X509ExtendedTrustManager
     * @see X509ExtendedKeyManager
     *
     * @since 1.8
     */
    public List<SNIServerName> getRequestedServerNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@link List} containing DER-encoded OCSP responses
     * (using the ASN.1 type OCSPResponse defined in RFC 6960) for
     * the client to verify status of the server's certificate during
     * handshaking.
     *
     * <P>
     * This method only applies to certificate-based server
     * authentication.  An {@link X509ExtendedTrustManager} will use the
     * returned value for server certificate validation.
     *
     * @implSpec This method throws UnsupportedOperationException by default.
     *         Classes derived from ExtendedSSLSession must implement
     *         this method.
     *
     * @return a non-null unmodifiable list of byte arrays, each entry
     *         containing a DER-encoded OCSP response (using the
     *         ASN.1 type OCSPResponse defined in RFC 6960).  The order
     *         of the responses must match the order of the certificates
     *         presented by the server in its Certificate message (See
     *         {@link SSLSession#getLocalCertificates()} for server mode,
     *         and {@link SSLSession#getPeerCertificates()} for client mode).
     *         It is possible that fewer response entries may be returned than
     *         the number of presented certificates.  If an entry in the list
     *         is a zero-length byte array, it should be treated by the
     *         caller as if the OCSP entry for the corresponding certificate
     *         is missing.  The returned list may be empty if no OCSP responses
     *         were presented during handshaking or if OCSP stapling is not
     *         supported by either endpoint for this handshake.
     *
     * @throws UnsupportedOperationException if the underlying provider
     *         does not implement the operation
     *
     * @see X509ExtendedTrustManager
     *
     * @since 9
     */
    public List<byte[]> getStatusResponses() {
        throw new UnsupportedOperationException();
    }
}
