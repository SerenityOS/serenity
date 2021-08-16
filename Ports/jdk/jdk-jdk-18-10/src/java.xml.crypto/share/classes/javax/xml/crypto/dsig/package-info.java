/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Classes for generating and validating XML digital
 * signatures. This package includes classes that represent the core elements
 * defined in the W3C XML digital signature specification:
 * {@link javax.xml.crypto.dsig.XMLSignature XMLSignature},
 * {@link javax.xml.crypto.dsig.SignedInfo SignedInfo},
 * {@link javax.xml.crypto.dsig.CanonicalizationMethod CanonicalizationMethod},
 * {@link javax.xml.crypto.dsig.SignatureMethod SignatureMethod},
 * {@link javax.xml.crypto.dsig.Reference Reference},
 * {@link javax.xml.crypto.dsig.DigestMethod DigestMethod},
 * {@link javax.xml.crypto.dsig.XMLObject XMLObject},
 * {@link javax.xml.crypto.dsig.Manifest Manifest},
 * {@link javax.xml.crypto.dsig.SignatureProperties SignatureProperties}, and
 * {@link javax.xml.crypto.dsig.SignatureProperty SignatureProperty}.
 * {@code KeyInfo} types are defined in the
 * {@link javax.xml.crypto.dsig.keyinfo} subpackage.
 * {@link javax.xml.crypto.dsig.XMLSignatureFactory XMLSignatureFactory}
 * is an abstract factory that creates
 * {@link javax.xml.crypto.dsig.XMLSignature XMLSignature} objects from scratch
 * or from a pre-existing XML representation, such as a DOM node.
 * {@link javax.xml.crypto.dsig.TransformService} is a service provider
 * interface for creating and plugging in implementations of
 * transform and canonicalization algorithms.
 *
 * <p>Of primary significance in this package is the
 * {@link javax.xml.crypto.dsig.XMLSignature XMLSignature} class,
 * which allows you to sign and validate an XML digital signature.
 *
 * <h2><a id="service_providers"></a>Service Providers</h2>
 * A service provider is a concrete implementation of the abstract
 * {@link javax.xml.crypto.dsig.XMLSignatureFactory XMLSignatureFactory} and
 * {@link javax.xml.crypto.dsig.keyinfo.KeyInfoFactory KeyInfoFactory} classes
 * and is responsible for creating objects and algorithms that parse, generate
 * and validate XML Signatures and KeyInfo structures. A concrete implementation
 * of {@code XMLSignatureFactory} MUST provide support for each of the REQUIRED
 * algorithms as specified by the W3C recommendation for XML Signatures. It MAY
 * support other algorithms as defined by the W3C recommendation or other
 * specifications.
 *
 * <p>The API leverages the JCA provider model (see
 * {@link java.security.Provider the Provider class}) for registering and
 * loading {@code XMLSignatureFactory} and {@code KeyInfoFactory}
 * implementations.
 *
 * <p>Each concrete {@code XMLSignatureFactory} or {@code KeyInfoFactory}
 * implementation supports a specific XML mechanism type that identifies the XML
 * processing mechanism that an implementation uses internally to parse and
 * generate XML signature and KeyInfo structures.
 *
 * <p>A service provider implementation SHOULD use underlying JCA engine
 * classes, such as {@link java.security.Signature} and
 * {@link java.security.MessageDigest} to perform cryptographic operations.
 *
 * <p>In addition to the {@code XMLSignatureFactory} and {@code KeyInfoFactory}
 * classes, the API supports a service provider interface for transform and
 * canonicalization algorithms. The {@link
 * javax.xml.crypto.dsig.TransformService TransformService} class allows you to
 * develop and plug in an implementation of a specific transform or
 * canonicalization algorithm for a particular XML mechanism type. The {@code
 * TransformService} class uses the standard JCA provider model for registering
 * and loading implementations. Each service provider implementation SHOULD use
 * the {@code TransformService} class to find a provider that supports transform
 * and canonicalization algorithms in XML Signatures that it is generating or
 * validating.
 *
 * <h3><a id="dom_req"></a>DOM Mechanism Requirements</h3>
 * The following requirements MUST be abided by when implementing a DOM-based
 * {@code XMLSignatureFactory}, {@code KeyInfoFactory} or {@code
 * TransformService} in order to minimize interoperability problems:
 * <ol>
 * <li>The {@code unmarshalXMLSignature} method of {@code XMLSignatureFactory}
 * MUST support {@code DOMValidateContext} types. If the type is
 * {@code DOMValidateContext}, it SHOULD contain an {@code Element} of type
 * Signature. Additionally, the {@code unmarshalXMLSignature} method MAY
 * populate the Id/Element mappings of the passed-in {@code DOMValidateContext}.
 * </li>
 *
 * <li>The {@code sign} method of {@code XMLSignature}s produced by
 * {@code XMLSignatureFactory} MUST support {@code DOMSignContext} types and the
 * {@code validate} method MUST support {@code DOMValidateContext} types. This
 * requirement also applies to the {@code validate} method of {@code
 * SignatureValue} and the {@code validate} method of {@code Reference}.</li>
 *
 * <li>The implementation MUST support {@code DOMStructure}s as the mechanism
 * for the application to specify extensible content (any elements or mixed
 * content).</li>
 *
 * <li>If the {@code dereference} method of user-specified {@code
 * URIDereferencer}s returns {@code NodeSetData} objects, the {@code iterator}
 * method MUST return an iteration over objects of type {@code
 * org.w3c.dom.Node}.</li>
 *
 * <li>{@code URIReference} objects passed to the {@code dereference} method of
 * user-specified {@code URIDereferencer}s MUST be of type {@code
 * DOMURIReference} and {@code XMLCryptoContext} objects MUST implement {@code
 * DOMCryptoContext}.</li>
 *
 * <li>The previous 2 requirements also apply to {@code URIDereferencer}s
 * returned by the {@code getURIDereferencer} method of {@code
 * XMLSignatureFactory} and {@code KeyInfoFactory}.</li>
 *
 * <li>The {@code unmarshalKeyInfo} method of {@code KeyInfoFactory} MUST
 * support {@code DOMStructure} types. If the type is {@code DOMStructure}, it
 * SHOULD contain an {@code Element} of type {@code KeyInfo}.</li>
 *
 * <li>The {@code transform} method of {@code Transform} MUST support
 * {@code DOMCryptoContext} context parameter types.</li>
 *
 * <li>The {@code newtransform} and {@code newCanonicalizationMethod} methods of
 * {@code XMLSignatureFactory} MUST support {@code DOMStructure} parameter
 * types.</li>
 *
 * <li>The {@code init}, and {@code marshalParams} methods of
 * {@code TransformService} MUST support {@code DOMStructure} and
 * {@code DOMCryptoContext} types.</li>
 *
 * <li>The {@code unmarshalXMLSignature} method of {@code XMLSignatureFactory}
 * MUST support {@code DOMStructure} types. If the type is {@code DOMStructure},
 * it SHOULD contain an {@code Element} of type {@code Signature}.</li>
 *
 * <li>The {@code marshal} method of {@code KeyInfo} MUST support
 * {@code DOMStructure} and {@code DOMCryptoContext} parameter types.</li>
 * </ol>
 *
 * <p>Note that a DOM implementation MAY internally use other XML parsing APIs
 * other than DOM as long as it doesn't affect interoperability. For example, a
 * DOM implementation of {@code XMLSignatureFactory} might use a SAX parser
 * internally to canonicalize data.
 *
 * <h2>Package Specification</h2>
 *
 * <ul>
 * <li>
 * <a href="http://www.w3.org/TR/xmldsig-core/">
 * XML-Signature Syntax and Processing: W3C Recommendation</a>
 * <li>
 * <a href="http://www.ietf.org/rfc/rfc3275.txt">
 * RFC 3275: XML-Signature Syntax and Processing</a>
 * </ul>
 *
 * @since 1.6
 */

package javax.xml.crypto.dsig;
