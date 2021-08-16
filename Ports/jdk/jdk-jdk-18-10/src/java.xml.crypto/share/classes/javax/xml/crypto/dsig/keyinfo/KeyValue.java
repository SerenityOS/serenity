/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * $Id: KeyValue.java,v 1.4 2005/05/10 16:35:35 mullan Exp $
 */
package javax.xml.crypto.dsig.keyinfo;

import java.security.KeyException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import javax.xml.crypto.XMLStructure;

/**
 * A representation of the XML <code>KeyValue</code> element as defined
 * in the <a href="http://www.w3.org/TR/xmldsig-core/">
 * W3C Recommendation for XML-Signature Syntax and Processing</a>. A
 * <code>KeyValue</code> object contains a single public key that may be
 * useful in validating the signature. The XML schema definition is defined as:
 *
 * <pre>
 *    &lt;element name="KeyValue" type="ds:KeyValueType"/&gt;
 *    &lt;complexType name="KeyValueType" mixed="true"&gt;
 *      &lt;choice&gt;
 *        &lt;element ref="ds:DSAKeyValue"/&gt;
 *        &lt;element ref="ds:RSAKeyValue"/&gt;
 *        &lt;!-- &lt;element ref="dsig11:ECKeyValue"/&gt; --&gt;
 *        &lt;!-- ECC keys (XMLDsig 1.1) will use the any element --&gt;
 *        &lt;any namespace="##other" processContents="lax"/&gt;
 *      &lt;/choice&gt;
 *    &lt;/complexType&gt;
 *
 *    &lt;element name="DSAKeyValue" type="ds:DSAKeyValueType"/&gt;
 *    &lt;complexType name="DSAKeyValueType"&gt;
 *      &lt;sequence&gt;
 *        &lt;sequence minOccurs="0"&gt;
 *          &lt;element name="P" type="ds:CryptoBinary"/&gt;
 *          &lt;element name="Q" type="ds:CryptoBinary"/&gt;
 *        &lt;/sequence&gt;
 *        &lt;element name="G" type="ds:CryptoBinary" minOccurs="0"/&gt;
 *        &lt;element name="Y" type="ds:CryptoBinary"/&gt;
 *        &lt;element name="J" type="ds:CryptoBinary" minOccurs="0"/&gt;
 *        &lt;sequence minOccurs="0"&gt;
 *          &lt;element name="Seed" type="ds:CryptoBinary"/&gt;
 *          &lt;element name="PgenCounter" type="ds:CryptoBinary"/&gt;
 *        &lt;/sequence&gt;
 *      &lt;/sequence&gt;
 *    &lt;/complexType&gt;
 *
 *    &lt;element name="RSAKeyValue" type="ds:RSAKeyValueType"/&gt;
 *    &lt;complexType name="RSAKeyValueType"&gt;
 *      &lt;sequence&gt;
 *        &lt;element name="Modulus" type="ds:CryptoBinary"/&gt;
 *        &lt;element name="Exponent" type="ds:CryptoBinary"/&gt;
 *      &lt;/sequence&gt;
 *    &lt;/complexType&gt;
 *
 *    &lt;complexType name="ECKeyValueType"&gt;
 *      &lt;sequence&gt;
 *        &lt;choice&gt;
 *          &lt;element name="ECParameters" type="dsig11:ECParametersType" /&gt;
 *          &lt;element name="NamedCurve" type="dsig11:NamedCurveType" /&gt;
 *        &lt;/choice&gt;
 *        &lt;element name="PublicKey" type="dsig11:ECPointType" /&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="Id" type="ID" use="optional" /&gt;
 *    &lt;/complexType&gt;
 *
 *    &lt;complexType name="NamedCurveType"&gt;
 *      &lt;attribute name="URI" type="anyURI" use="required" /&gt;
 *    &lt;/complexType&gt;
 *
 *    &lt;simpleType name="ECPointType"&gt;
 *      &lt;restriction base="ds:CryptoBinary" /&gt;
 *    &lt;/simpleType&gt;
 * </pre>
 * See section 4.5.2.3.1 of the W3C Recommendation for the definition
 * of ECParametersType.
 *
 * <p>A <code>KeyValue</code> instance may be created by invoking the
 * {@link KeyInfoFactory#newKeyValue newKeyValue} method of the
 * {@link KeyInfoFactory} class, and passing it a {@link
 * java.security.PublicKey} representing the value of the public key. Here is
 * an example of creating a <code>KeyValue</code> from a {@link DSAPublicKey}
 * of a {@link java.security.cert.Certificate} stored in a
 * {@link java.security.KeyStore}:
 * <pre>
 * KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
 * PublicKey dsaPublicKey = keyStore.getCertificate("myDSASigningCert").getPublicKey();
 * KeyInfoFactory factory = KeyInfoFactory.getInstance("DOM");
 * KeyValue keyValue = factory.newKeyValue(dsaPublicKey);
 * </pre>
 *
 * This class returns the <code>DSAKeyValue</code> and
 * <code>RSAKeyValue</code> elements as objects of type
 * {@link DSAPublicKey} and {@link RSAPublicKey}, respectively. Note that not
 * all of the fields in the schema are accessible as parameters of these
 * types.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @since 1.6
 * @see KeyInfoFactory#newKeyValue(PublicKey)
 */
public interface KeyValue extends XMLStructure {

    /**
     * URI identifying the DSA KeyValue KeyInfo type:
     * http://www.w3.org/2000/09/xmldsig#DSAKeyValue. This can be specified as
     * the value of the <code>type</code> parameter of the
     * {@link RetrievalMethod} class to describe a remote
     * <code>DSAKeyValue</code> structure.
     */
    static final String DSA_TYPE =
        "http://www.w3.org/2000/09/xmldsig#DSAKeyValue";

    /**
     * URI identifying the RSA KeyValue KeyInfo type:
     * http://www.w3.org/2000/09/xmldsig#RSAKeyValue. This can be specified as
     * the value of the <code>type</code> parameter of the
     * {@link RetrievalMethod} class to describe a remote
     * <code>RSAKeyValue</code> structure.
     */
    static final String RSA_TYPE =
        "http://www.w3.org/2000/09/xmldsig#RSAKeyValue";

    /**
     * URI identifying the EC KeyValue KeyInfo type:
     * http://www.w3.org/2009/xmldsig11#ECKeyValue. This can be specified as
     * the value of the <code>type</code> parameter of the
     * {@link RetrievalMethod} class to describe a remote
     * <code>ECKeyValue</code> structure.
     */
    static final String EC_TYPE =
        "http://www.w3.org/2009/xmldsig11#ECKeyValue";

    /**
     * Returns the public key of this <code>KeyValue</code>.
     *
     * @return the public key of this <code>KeyValue</code>
     * @throws KeyException if this <code>KeyValue</code> cannot be converted
     *    to a <code>PublicKey</code>
     */
    PublicKey getPublicKey() throws KeyException;
}
