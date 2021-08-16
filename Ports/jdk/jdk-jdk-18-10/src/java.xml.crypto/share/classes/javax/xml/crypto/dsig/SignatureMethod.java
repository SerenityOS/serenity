/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
/*
 * $Id: SignatureMethod.java,v 1.5 2005/05/10 16:03:46 mullan Exp $
 */
package javax.xml.crypto.dsig;

import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

/**
 * A representation of the XML <code>SignatureMethod</code> element
 * as defined in the <a href="http://www.w3.org/TR/xmldsig-core/">
 * W3C Recommendation for XML-Signature Syntax and Processing</a>.
 * The XML Schema Definition is defined as:
 * <pre>
 *   &lt;element name="SignatureMethod" type="ds:SignatureMethodType"/&gt;
 *     &lt;complexType name="SignatureMethodType" mixed="true"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="HMACOutputLength" minOccurs="0" type="ds:HMACOutputLengthType"/&gt;
 *         &lt;any namespace="##any" minOccurs="0" maxOccurs="unbounded"/&gt;
 *           &lt;!-- (0,unbounded) elements from (1,1) namespace --&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="Algorithm" type="anyURI" use="required"/&gt;
 *     &lt;/complexType&gt;
 * </pre>
 *
 * A <code>SignatureMethod</code> instance may be created by invoking the
 * {@link XMLSignatureFactory#newSignatureMethod newSignatureMethod} method
 * of the {@link XMLSignatureFactory} class.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see XMLSignatureFactory#newSignatureMethod(String, SignatureMethodParameterSpec)
 */
public interface SignatureMethod extends XMLStructure, AlgorithmMethod {

    // All methods can be found in RFC 6931.

    /**
     * The <a href="http://www.w3.org/2000/09/xmldsig#dsa-sha1">DSA-SHA1</a>
     * (DSS) signature method algorithm URI.
     */
    String DSA_SHA1 =
        "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

    /**
     * The <a href="http://www.w3.org/2009/xmldsig11#dsa-sha256">DSA-SHA256</a>
     * (DSS) signature method algorithm URI.
     *
     * @since 11
     */
    String DSA_SHA256 = "http://www.w3.org/2009/xmldsig11#dsa-sha256";

    /**
     * The <a href="http://www.w3.org/2000/09/xmldsig#rsa-sha1">RSA-SHA1</a>
     * (PKCS #1) signature method algorithm URI.
     */
    String RSA_SHA1 =
        "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#rsa-sha224">
     * RSA-SHA224</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha224";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256">
     * RSA-SHA256</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#rsa-sha384">
     * RSA-SHA384</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#rsa-sha512">
     * RSA-SHA512</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1">
     * SHA1-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA1_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha1-rsa-MGF1";

    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1">
     * SHA224-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA224_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha224-rsa-MGF1";

    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1">
     * SHA256-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA256_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha256-rsa-MGF1";

    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1">
     * SHA384-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA384_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha384-rsa-MGF1";

    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1">
     * SHA512-RSA-MGF1</a> (PKCS #1) signature method algorithm URI.
     *
     * @since 11
     */
    String SHA512_RSA_MGF1 = "http://www.w3.org/2007/05/xmldsig-more#sha512-rsa-MGF1";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1">
     * ECDSA-SHA1</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA1 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224">
     * ECDSA-SHA224</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256">
     * ECDSA-SHA256</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384">
     * ECDSA-SHA384</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512">
     * ECDSA-SHA512</a> (FIPS 180-4) signature method algorithm URI.
     *
     * @since 11
     */
    String ECDSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512";

    /**
     * The <a href="http://www.w3.org/2000/09/xmldsig#hmac-sha1">HMAC-SHA1</a>
     * MAC signature method algorithm URI
     */
    String HMAC_SHA1 =
        "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#hmac-sha224">
     * HMAC-SHA224</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA224 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha224";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#hmac-sha256">
     * HMAC-SHA256</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha256";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#hmac-sha384">
     * HMAC-SHA384</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha384";

    /**
     * The <a href="http://www.w3.org/2001/04/xmldsig-more#hmac-sha512">
     * HMAC-SHA512</a> MAC signature method algorithm URI.
     *
     * @since 11
     */
    String HMAC_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#hmac-sha512";


    /**
     * The <a href="http://www.w3.org/2007/05/xmldsig-more#rsa-pss">
     * RSASSA-PSS</a> signature method algorithm URI.
     * <p>
     * Calling {@link XMLSignatureFactory#newSignatureMethod
     * XMLSignatureFactory.newSignatureMethod(RSA_PSS, null)} returns a
     * {@code SignatureMethod} object that uses the default parameter as defined in
     * <a href="https://tools.ietf.org/html/rfc6931#section-2.3.9">RFC 6931 Section 2.3.9</a>,
     * which uses SHA-256 as the {@code DigestMethod}, MGF1 with SHA-256 as the
     * {@code MaskGenerationFunction}, 32 as {@code SaltLength}, and 1 as
     * {@code TrailerField}. This default parameter is represented as an
     * {@link javax.xml.crypto.dsig.spec.RSAPSSParameterSpec RSAPSSParameterSpec}
     * type and returned by the {@link #getParameterSpec()} method
     * of the {@code SignatureMethod} object.
     *
     * @since 17
     */
    String RSA_PSS = "http://www.w3.org/2007/05/xmldsig-more#rsa-pss";

    /**
     * Returns the algorithm-specific input parameters of this
     * <code>SignatureMethod</code>.
     *
     * <p>The returned parameters can be typecast to a {@link
     * SignatureMethodParameterSpec} object.
     *
     * @return the algorithm-specific input parameters of this
     *    <code>SignatureMethod</code> (may be <code>null</code> if not
     *    specified)
     */
    AlgorithmParameterSpec getParameterSpec();
}
