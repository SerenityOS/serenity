/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.signature;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Key;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.SecretKey;

import com.sun.org.apache.xml.internal.security.algorithms.SignatureAlgorithm;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.keys.KeyInfo;
import com.sun.org.apache.xml.internal.security.keys.content.X509Data;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.I18n;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import com.sun.org.apache.xml.internal.security.utils.SignerOutputStream;
import com.sun.org.apache.xml.internal.security.utils.UnsyncBufferedOutputStream;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Handles {@code &lt;ds:Signature&gt;} elements.
 * This is the main class that deals with creating and verifying signatures.
 *
 * <p>There are 2 types of constructors for this class. The ones that take a
 * document, baseURI and 1 or more Java Objects. This is mostly used for
 * signing purposes.
 * The other constructor is the one that takes a DOM Element and a baseURI.
 * This is used mostly with for verifying, when you have a SignatureElement.
 *
 * There are a few different types of methods:
 * <ul><li>The addDocument* methods are used to add References with optional
 * transforms during signing. </li>
 * <li>addKeyInfo* methods are to add Certificates and Keys to the
 * KeyInfo tags during signing. </li>
 * <li>appendObject allows a user to add any XML Structure as an
 * ObjectContainer during signing.</li>
 * <li>sign and checkSignatureValue methods are used to sign and validate the
 * signature. </li></ul>
 */
public final class XMLSignature extends SignatureElementProxy {

    /** MAC - Required HMAC-SHA1 */
    public static final String ALGO_ID_MAC_HMAC_SHA1 =
        Constants.SignatureSpecNS + "hmac-sha1";

    /** Signature - Required DSAwithSHA1 (DSS) */
    public static final String ALGO_ID_SIGNATURE_DSA =
        Constants.SignatureSpecNS + "dsa-sha1";

    /** Signature - Optional DSAwithSHA256 */
    public static final String ALGO_ID_SIGNATURE_DSA_SHA256 =
        Constants.SignatureSpec11NS + "dsa-sha256";

    /** Signature - Recommended RSAwithSHA1 */
    public static final String ALGO_ID_SIGNATURE_RSA =
        Constants.SignatureSpecNS + "rsa-sha1";

    /** Signature - Recommended RSAwithSHA1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA1 =
        Constants.SignatureSpecNS + "rsa-sha1";

    /** Signature - NOT Recommended RSAwithMD5 */
    public static final String ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5 =
        Constants.MoreAlgorithmsSpecNS + "rsa-md5";

    /** Signature - Optional RSAwithRIPEMD160 */
    public static final String ALGO_ID_SIGNATURE_RSA_RIPEMD160 =
        Constants.MoreAlgorithmsSpecNS + "rsa-ripemd160";

    /** Signature - Optional RSAwithSHA224 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA224 =
        Constants.MoreAlgorithmsSpecNS + "rsa-sha224";

    /** Signature - Optional RSAwithSHA256 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA256 =
        Constants.MoreAlgorithmsSpecNS + "rsa-sha256";

    /** Signature - Optional RSAwithSHA384 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA384 =
        Constants.MoreAlgorithmsSpecNS + "rsa-sha384";

    /** Signature - Optional RSAwithSHA512 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA512 =
        Constants.MoreAlgorithmsSpecNS + "rsa-sha512";

    /** Signature - Optional RSAwithSHA1andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA1_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha1-rsa-MGF1";

    /** Signature - Optional RSAwithSHA224andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA224_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha224-rsa-MGF1";

    /** Signature - Optional RSAwithSHA256andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA256_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha256-rsa-MGF1";

    /** Signature - Optional RSAwithSHA384andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA384_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha384-rsa-MGF1";

    /** Signature - Optional RSAwithSHA512andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA512_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha512-rsa-MGF1";

    /** Signature - Optional RSAwithSHA3_224andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA3_224_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha3-224-rsa-MGF1";

    /** Signature - Optional RSAwithSHA3_256andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA3_256_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha3-256-rsa-MGF1";

    /** Signature - Optional RSAwithSHA3_384andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA3_384_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha3-384-rsa-MGF1";

    /** Signature - Optional RSAwithSHA3_512andMGF1 */
    public static final String ALGO_ID_SIGNATURE_RSA_SHA3_512_MGF1 =
        Constants.XML_DSIG_NS_MORE_07_05 + "sha3-512-rsa-MGF1";

    /** HMAC - NOT Recommended HMAC-MD5 */
    public static final String ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5 =
        Constants.MoreAlgorithmsSpecNS + "hmac-md5";

    /** HMAC - Optional HMAC-RIPEMD160 */
    public static final String ALGO_ID_MAC_HMAC_RIPEMD160 =
        Constants.MoreAlgorithmsSpecNS + "hmac-ripemd160";

    /** HMAC - Optional HMAC-SHA2224 */
    public static final String ALGO_ID_MAC_HMAC_SHA224 =
        Constants.MoreAlgorithmsSpecNS + "hmac-sha224";

    /** HMAC - Optional HMAC-SHA256 */
    public static final String ALGO_ID_MAC_HMAC_SHA256 =
        Constants.MoreAlgorithmsSpecNS + "hmac-sha256";

    /** HMAC - Optional HMAC-SHA284 */
    public static final String ALGO_ID_MAC_HMAC_SHA384 =
        Constants.MoreAlgorithmsSpecNS + "hmac-sha384";

    /** HMAC - Optional HMAC-SHA512 */
    public static final String ALGO_ID_MAC_HMAC_SHA512 =
        Constants.MoreAlgorithmsSpecNS + "hmac-sha512";

    /**Signature - Optional ECDSAwithSHA1 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_SHA1 =
        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";

    /**Signature - Optional ECDSAwithSHA224 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_SHA224 =
        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha224";

    /**Signature - Optional ECDSAwithSHA256 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_SHA256 =
        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256";

    /**Signature - Optional ECDSAwithSHA384 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_SHA384 =
        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";

    /**Signature - Optional ECDSAwithSHA512 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_SHA512 =
        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512";

    /**Signature - Optional ECDSAwithRIPEMD160 */
    public static final String ALGO_ID_SIGNATURE_ECDSA_RIPEMD160 =
        "http://www.w3.org/2007/05/xmldsig-more#ecdsa-ripemd160";

    /** Signature - Optional RSASSA-PSS */
    public static final String ALGO_ID_SIGNATURE_RSA_PSS =
            Constants.XML_DSIG_NS_MORE_07_05 + "rsa-pss";

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(XMLSignature.class);

    /** ds:Signature.ds:SignedInfo element */
    private final SignedInfo signedInfo;

    /** ds:Signature.ds:KeyInfo */
    private KeyInfo keyInfo;

    /**
     * Checking the digests in References in a Signature are mandatory, but for
     * References inside a Manifest it is application specific. This boolean is
     * to indicate that the References inside Manifests should be validated.
     */
    private boolean followManifestsDuringValidation = false;

    private Element signatureValueElement;

    private static final int MODE_SIGN = 0;
    private static final int MODE_VERIFY = 1;
    private int state = MODE_SIGN;

    /**
     * This creates a new {@code ds:Signature} Element and adds an empty
     * {@code ds:SignedInfo}.
     * The {@code ds:SignedInfo} is initialized with the specified Signature
     * algorithm and Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS which is REQUIRED
     * by the spec. This method's main use is for creating a new signature.
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI signature algorithm to use.
     * @throws XMLSecurityException
     */
    public XMLSignature(Document doc, String baseURI, String signatureMethodURI)
        throws XMLSecurityException {
        this(doc, baseURI, signatureMethodURI, 0, Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS, null, null);
    }

    /**
     * This creates a new {@code ds:Signature} Element and adds an empty
     * {@code ds:SignedInfo}.
     * The {@code ds:SignedInfo} is initialized with the specified Signature
     * algorithm and Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS which is REQUIRED
     * by the spec. This method's main use is for creating a new signature.
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI signature algorithm to use.
     * @param provider security provider to use.
     * @throws XMLSecurityException
     */
    public XMLSignature(Document doc, String baseURI, String signatureMethodURI, Provider provider)
        throws XMLSecurityException {
        this(doc, baseURI, signatureMethodURI, 0, Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS, provider, null);
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param hmacOutputLength
     * @throws XMLSecurityException
     */
    public XMLSignature(Document doc, String baseURI, String signatureMethodURI,
                        int hmacOutputLength) throws XMLSecurityException {
        this(
            doc, baseURI, signatureMethodURI, hmacOutputLength,
            Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS, null, null
        );
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param hmacOutputLength
     * @param provider security provider to use.
     * @throws XMLSecurityException
     */
    public XMLSignature(Document doc, String baseURI, String signatureMethodURI,
                        int hmacOutputLength, Provider provider) throws XMLSecurityException {
        this(
            doc, baseURI, signatureMethodURI, hmacOutputLength,
            Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS, provider, null
        );
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param canonicalizationMethodURI the canonicalization algorithm to be used to c14nize the SignedInfo element.
     * @throws XMLSecurityException
     */
    public XMLSignature(
        Document doc,
        String baseURI,
        String signatureMethodURI,
        String canonicalizationMethodURI
    ) throws XMLSecurityException {
        this(doc, baseURI, signatureMethodURI, 0, canonicalizationMethodURI, null, null);
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param canonicalizationMethodURI the canonicalization algorithm to be used to c14nize the SignedInfo element.
     * @param provider security provider to use.
     * @throws XMLSecurityException
     */
    public XMLSignature(
        Document doc,
        String baseURI,
        String signatureMethodURI,
        String canonicalizationMethodURI,
        Provider provider
    ) throws XMLSecurityException {
        this(doc, baseURI, signatureMethodURI, 0, canonicalizationMethodURI, provider, null);
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param hmacOutputLength
     * @param canonicalizationMethodURI the canonicalization algorithm to be used to c14nize the SignedInfo element.
     * @throws XMLSecurityException
     */
    public XMLSignature(
        Document doc,
        String baseURI,
        String signatureMethodURI,
        int hmacOutputLength,
        String canonicalizationMethodURI
    ) throws XMLSecurityException {
        this(doc, baseURI, signatureMethodURI, hmacOutputLength, canonicalizationMethodURI, null, null);
    }

    /**
     * Constructor XMLSignature
     *
     * @param doc Document in which the signature will be appended after creation.
     * @param baseURI URI to be used as context for all relative URIs.
     * @param signatureMethodURI the Signature method to be used.
     * @param hmacOutputLength
     * @param canonicalizationMethodURI the canonicalization algorithm to be used to c14nize the SignedInfo element.
     * @param provider security provider to use.
     * @param spec
     * @throws XMLSecurityException
     */
    public XMLSignature(
        Document doc,
        String baseURI,
        String signatureMethodURI,
        int hmacOutputLength,
        String canonicalizationMethodURI,
        Provider provider,
        AlgorithmParameterSpec spec
    ) throws XMLSecurityException {
        super(doc);

        String xmlnsDsPrefix = getDefaultPrefix(Constants.SignatureSpecNS);
        if (xmlnsDsPrefix == null || xmlnsDsPrefix.length() == 0) {
            getElement().setAttributeNS(
                Constants.NamespaceSpecNS, "xmlns", Constants.SignatureSpecNS
            );
        } else {
            getElement().setAttributeNS(
                Constants.NamespaceSpecNS, "xmlns:" + xmlnsDsPrefix, Constants.SignatureSpecNS
            );
        }
        addReturnToSelf();

        this.baseURI = baseURI;
        this.signedInfo =
            new SignedInfo(
                getDocument(), signatureMethodURI, hmacOutputLength, canonicalizationMethodURI, provider, spec
            );

        appendSelf(this.signedInfo);
        addReturnToSelf();

        // create an empty SignatureValue; this is filled by setSignatureValueElement
        signatureValueElement =
            XMLUtils.createElementInSignatureSpace(getDocument(), Constants._TAG_SIGNATUREVALUE);

        appendSelf(signatureValueElement);
        addReturnToSelf();
    }

    /**
     *  Creates a XMLSignature in a Document
     * @param doc
     * @param baseURI
     * @param signatureMethodElem
     * @param canonicalizationMethodElem
     * @throws XMLSecurityException
     */
    public XMLSignature(
        Document doc,
        String baseURI,
        Element signatureMethodElem,
        Element canonicalizationMethodElem
    ) throws XMLSecurityException {
        this(doc, baseURI, signatureMethodElem, canonicalizationMethodElem, null);
    }

    public XMLSignature(
        Document doc,
        String baseURI,
        Element signatureMethodElem,
        Element canonicalizationMethodElem,
        Provider provider
    ) throws XMLSecurityException {
        super(doc);

        String xmlnsDsPrefix = getDefaultPrefix(Constants.SignatureSpecNS);
        if (xmlnsDsPrefix == null || xmlnsDsPrefix.length() == 0) {
            getElement().setAttributeNS(
                Constants.NamespaceSpecNS, "xmlns", Constants.SignatureSpecNS
            );
        } else {
            getElement().setAttributeNS(
                Constants.NamespaceSpecNS, "xmlns:" + xmlnsDsPrefix, Constants.SignatureSpecNS
            );
        }
        addReturnToSelf();

        this.baseURI = baseURI;
        this.signedInfo =
            new SignedInfo(getDocument(), signatureMethodElem, canonicalizationMethodElem, provider);

        appendSelf(this.signedInfo);
        addReturnToSelf();

        // create an empty SignatureValue; this is filled by setSignatureValueElement
        signatureValueElement =
            XMLUtils.createElementInSignatureSpace(getDocument(), Constants._TAG_SIGNATUREVALUE);

        appendSelf(signatureValueElement);
        addReturnToSelf();
    }

    /**
     * This will parse the element and construct the Java Objects.
     * That will allow a user to validate the signature.
     *
     * @param element ds:Signature element that contains the whole signature
     * @param baseURI URI to be prepended to all relative URIs
     * @throws XMLSecurityException
     * @throws XMLSignatureException if the signature is badly formatted
     */
    public XMLSignature(Element element, String baseURI)
        throws XMLSignatureException, XMLSecurityException {
        this(element, baseURI, true, null);
    }

    /**
     * This will parse the element and construct the Java Objects.
     * That will allow a user to validate the signature.
     *
     * @param element ds:Signature element that contains the whole signature
     * @param baseURI URI to be prepended to all relative URIs
     * @param provider security provider to use
     * @throws XMLSecurityException
     * @throws XMLSignatureException if the signature is badly formatted
     */
    public XMLSignature(Element element, String baseURI, Provider provider)
        throws XMLSignatureException, XMLSecurityException {
        this(element, baseURI, true, provider);
    }

    /**
     * This will parse the element and construct the Java Objects.
     * That will allow a user to validate the signature.
     *
     * @param element ds:Signature element that contains the whole signature
     * @param baseURI URI to be prepended to all relative URIs
     * @param secureValidation whether secure secureValidation is enabled or not
     * @throws XMLSecurityException
     * @throws XMLSignatureException if the signature is badly formatted
     */
    public XMLSignature(Element element, String baseURI, boolean secureValidation)
        throws XMLSignatureException, XMLSecurityException {
        this(element, baseURI, secureValidation, null);
    }

    /**
     * This will parse the element and construct the Java Objects.
     * That will allow a user to validate the signature.
     *
     * @param element ds:Signature element that contains the whole signature
     * @param baseURI URI to be prepended to all relative URIs
     * @param secureValidation whether secure secureValidation is enabled or not
     * @param provider security provider to use
     * @throws XMLSecurityException
     * @throws XMLSignatureException if the signature is badly formatted
     */
    public XMLSignature(Element element, String baseURI, boolean secureValidation, Provider provider)
        throws XMLSignatureException, XMLSecurityException {
        super(element, baseURI);

        if (!(Constants.SignatureSpecNS.equals(element.getNamespaceURI())
            && Constants._TAG_SIGNATURE.equals(element.getLocalName()))) {
            Object[] exArgs = { element.getLocalName() };
            throw new XMLSignatureException("signature.Verification.InvalidElement", exArgs);
        }

        // check out SignedInfo child
        Element signedInfoElem = XMLUtils.getNextElement(element.getFirstChild());

        // check to see if it is there
        if (signedInfoElem == null ||
            !(Constants.SignatureSpecNS.equals(signedInfoElem.getNamespaceURI())
                && Constants._TAG_SIGNEDINFO.equals(signedInfoElem.getLocalName()))) {
            Object[] exArgs = { Constants._TAG_SIGNEDINFO, Constants._TAG_SIGNATURE };
            throw new XMLSignatureException("xml.WrongContent", exArgs);
        }

        // create a SignedInfo object from that element
        this.signedInfo = new SignedInfo(signedInfoElem, baseURI, secureValidation, provider);
        // get signedInfoElem again in case it has changed
        signedInfoElem = XMLUtils.getNextElement(element.getFirstChild());

        // check out SignatureValue child
        this.signatureValueElement =
            XMLUtils.getNextElement(signedInfoElem.getNextSibling());

        // check to see if it exists
        if (signatureValueElement == null ||
            !(Constants.SignatureSpecNS.equals(signatureValueElement.getNamespaceURI())
                && Constants._TAG_SIGNATUREVALUE.equals(signatureValueElement.getLocalName()))) {
            Object[] exArgs = { Constants._TAG_SIGNATUREVALUE, Constants._TAG_SIGNATURE };
            throw new XMLSignatureException("xml.WrongContent", exArgs);
        }
        Attr signatureValueAttr = signatureValueElement.getAttributeNodeNS(null, "Id");
        if (signatureValueAttr != null) {
            signatureValueElement.setIdAttributeNode(signatureValueAttr, true);
        }

        // <element ref="ds:KeyInfo" minOccurs="0"/>
        Element keyInfoElem =
            XMLUtils.getNextElement(signatureValueElement.getNextSibling());

        // If it exists use it, but it's not mandatory
        Element objectElem = null;
        if (keyInfoElem != null
            && Constants.SignatureSpecNS.equals(keyInfoElem.getNamespaceURI())
            && Constants._TAG_KEYINFO.equals(keyInfoElem.getLocalName())) {
            this.keyInfo = new KeyInfo(keyInfoElem, baseURI);
            this.keyInfo.setSecureValidation(secureValidation);
            objectElem = XMLUtils.getNextElement(keyInfoElem.getNextSibling());
        } else {
            // If we have no KeyInfo
            objectElem = keyInfoElem;
        }

        // <element ref="ds:Object" minOccurs="0" maxOccurs="unbounded"/>
        while (objectElem != null) {
            // Make sure it actually is an Object
            if (!(Constants.SignatureSpecNS.equals(objectElem.getNamespaceURI())
                && Constants._TAG_OBJECT.equals(objectElem.getLocalName()))) {
                Object[] exArgs = { objectElem.getLocalName() };
                throw new XMLSignatureException("signature.Verification.InvalidElement", exArgs);
            }

            Attr objectAttr = objectElem.getAttributeNodeNS(null, "Id");
            if (objectAttr != null) {
                objectElem.setIdAttributeNode(objectAttr, true);
            }

            Node firstChild = objectElem.getFirstChild();
            // Register Ids of the Object child elements
            while (firstChild != null) {
                if (firstChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElem = (Element)firstChild;
                    String tag = childElem.getLocalName();
                    if ("Manifest".equals(tag)) {
                        new Manifest(childElem, baseURI);
                    } else if ("SignatureProperties".equals(tag)) {
                        new SignatureProperties(childElem, baseURI);
                    }
                }
                firstChild = firstChild.getNextSibling();
            }

            objectElem = XMLUtils.getNextElement(objectElem.getNextSibling());
        }

        this.state = MODE_VERIFY;
    }

    /**
     * Sets the {@code Id} attribute
     *
     * @param id Id value for the id attribute on the Signature Element
     */
    public void setId(String id) {
        if (id != null) {
            setLocalIdAttribute(Constants._ATT_ID, id);
        }
    }

    /**
     * Returns the {@code Id} attribute
     *
     * @return the {@code Id} attribute
     */
    public String getId() {
        return getLocalAttribute(Constants._ATT_ID);
    }

    /**
     * Returns the completely parsed {@code SignedInfo} object.
     *
     * @return the completely parsed {@code SignedInfo} object.
     */
    public SignedInfo getSignedInfo() {
        return this.signedInfo;
    }

    /**
     * Returns the octet value of the SignatureValue element.
     * Throws an XMLSignatureException if it has no or wrong content.
     *
     * @return the value of the SignatureValue element.
     * @throws XMLSignatureException If there is no content
     */
    public byte[] getSignatureValue() throws XMLSignatureException {
        String content = XMLUtils.getFullTextChildrenFromNode(signatureValueElement);
        return XMLUtils.decode(content);
    }

    /**
     * Base64 encodes and sets the bytes as the content of the SignatureValue
     * Node.
     *
     * @param bytes bytes to be used by SignatureValue before Base64 encoding
     */
    private void setSignatureValueElement(byte[] bytes) {

        while (signatureValueElement.hasChildNodes()) {
            signatureValueElement.removeChild(signatureValueElement.getFirstChild());
        }

        String base64codedValue = XMLUtils.encodeToString(bytes);

        if (base64codedValue.length() > 76 && !XMLUtils.ignoreLineBreaks()) {
            base64codedValue = "\n" + base64codedValue + "\n";
        }

        Text t = createText(base64codedValue);
        signatureValueElement.appendChild(t);
    }

    /**
     * Returns the KeyInfo child. If we are in signing mode and the KeyInfo
     * does not exist yet, it is created on demand and added to the Signature.
     * <br>
     * This allows to add arbitrary content to the KeyInfo during signing.
     *
     * @return the KeyInfo object
     */
    public KeyInfo getKeyInfo() {
        // check to see if we are signing and if we have to create a keyinfo
        if (this.state == MODE_SIGN && this.keyInfo == null) {

            // create the KeyInfo
            this.keyInfo = new KeyInfo(getDocument());

            // get the Element from KeyInfo
            Element keyInfoElement = this.keyInfo.getElement();
            Element firstObject =
                XMLUtils.selectDsNode(
                    getElement().getFirstChild(), Constants._TAG_OBJECT, 0
                );

            if (firstObject != null) {
                // add it before the object
                getElement().insertBefore(keyInfoElement, firstObject);
                XMLUtils.addReturnBeforeChild(getElement(), firstObject);
            } else {
                // add it as the last element to the signature
                appendSelf(keyInfoElement);
                addReturnToSelf();
            }
        }

        return this.keyInfo;
    }

    /**
     * Appends an Object (not a {@code java.lang.Object} but an Object
     * element) to the Signature. Please note that this is only possible
     * when signing.
     *
     * @param object ds:Object to be appended.
     * @throws XMLSignatureException When this object is used to verify.
     */
    public void appendObject(ObjectContainer object) throws XMLSignatureException {
        //try {
        //if (this.state != MODE_SIGN) {
        // throw new XMLSignatureException(
        //  "signature.operationOnlyBeforeSign");
        //}

        appendSelf(object);
        addReturnToSelf();
        //} catch (XMLSecurityException ex) {
        // throw new XMLSignatureException(ex);
        //}
    }

    /**
     * Returns the {@code i}th {@code ds:Object} child of the signature
     * or null if no such {@code ds:Object} element exists.
     *
     * @param i
     * @return the {@code i}th {@code ds:Object} child of the signature
     * or null if no such {@code ds:Object} element exists.
     */
    public ObjectContainer getObjectItem(int i) {
        Element objElem =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_OBJECT, i
            );

        try {
            return new ObjectContainer(objElem, this.baseURI);
        } catch (XMLSecurityException ex) {
            return null;
        }
    }

    /**
     * Returns the number of all {@code ds:Object} elements.
     *
     * @return the number of all {@code ds:Object} elements.
     */
    public int getObjectLength() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_OBJECT);
    }

    /**
     * Digests all References in the SignedInfo, calculates the signature value
     * and sets it in the SignatureValue Element.
     *
     * @param signingKey the {@link java.security.PrivateKey} or
     * {@link javax.crypto.SecretKey} that is used to sign.
     * @throws XMLSignatureException
     */
    public void sign(Key signingKey) throws XMLSignatureException {

        if (signingKey instanceof PublicKey) {
            throw new IllegalArgumentException(
                I18n.translate("algorithms.operationOnlyVerification")
            );
        }

        //Create a SignatureAlgorithm object
        SignedInfo si = this.getSignedInfo();
        SignatureAlgorithm sa = si.getSignatureAlgorithm();
        try (SignerOutputStream output = new SignerOutputStream(sa);
            OutputStream so = new UnsyncBufferedOutputStream(output)) {

            // generate digest values for all References in this SignedInfo
            si.generateDigestValues();

            // initialize SignatureAlgorithm for signing
            sa.initSign(signingKey);

            // get the canonicalized bytes from SignedInfo
            si.signInOctetStream(so);

            // set them on the SignatureValue element
            this.setSignatureValueElement(sa.sign());
        } catch (XMLSignatureException ex) {
            throw ex;
        } catch (XMLSecurityException | IOException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * Adds a {@link ResourceResolverSpi} to enable the retrieval of resources.
     *
     * @param resolver
     */
    public void addResourceResolver(ResourceResolverSpi resolver) {
        this.getSignedInfo().addResourceResolver(resolver);
    }

    /**
     * Extracts the public key from the certificate and verifies if the signature
     * is valid by re-digesting all References, comparing those against the
     * stored DigestValues and then checking to see if the Signatures match on
     * the SignedInfo.
     *
     * @param cert Certificate that contains the public key part of the keypair
     * that was used to sign.
     * @return true if the signature is valid, false otherwise
     * @throws XMLSignatureException
     */
    public boolean checkSignatureValue(X509Certificate cert)
        throws XMLSignatureException {
        // see if cert is null
        if (cert != null) {
            // check the values with the public key from the cert
            return this.checkSignatureValue(cert.getPublicKey());
        }

        Object[] exArgs = { "Didn't get a certificate" };
        throw new XMLSignatureException("empty", exArgs);
    }

    /**
     * Verifies if the signature is valid by redigesting all References,
     * comparing those against the stored DigestValues and then checking to see
     * if the Signatures match on the SignedInfo.
     *
     * @param pk {@link java.security.PublicKey} part of the keypair or
     * {@link javax.crypto.SecretKey} that was used to sign
     * @return true if the signature is valid, false otherwise
     * @throws XMLSignatureException
     */
    public boolean checkSignatureValue(Key pk) throws XMLSignatureException {
        //COMMENT: pk suggests it can only be a public key?
        //check to see if the key is not null
        if (pk == null) {
            Object[] exArgs = { "Didn't get a key" };
            throw new XMLSignatureException("empty", exArgs);
        }
        // all references inside the signedinfo need to be dereferenced and
        // digested again to see if the outcome matches the stored value in the
        // SignedInfo.
        // If followManifestsDuringValidation is true it will do the same for
        // References inside a Manifest.
        try {
            SignedInfo si = this.getSignedInfo();
            //create a SignatureAlgorithms from the SignatureMethod inside
            //SignedInfo. This is used to validate the signature.
            SignatureAlgorithm sa = si.getSignatureAlgorithm();
            LOG.debug("signatureMethodURI = {}", sa.getAlgorithmURI());
            LOG.debug("jceSigAlgorithm = {}", sa.getJCEAlgorithmString());
            LOG.debug("PublicKey = {}", pk);

            byte[] sigBytes = null;
            try (SignerOutputStream so = new SignerOutputStream(sa);
                OutputStream bos = new UnsyncBufferedOutputStream(so)) {

                sa.initVerify(pk);
                LOG.debug("jceSigProvider = {}", sa.getJCEProviderName());

                // Get the canonicalized (normalized) SignedInfo
                si.signInOctetStream(bos);
                // retrieve the byte[] from the stored signature
                sigBytes = this.getSignatureValue();
            } catch (IOException ex) {
                LOG.debug(ex.getMessage(), ex);
                // Impossible...
            } catch (XMLSecurityException ex) {
                throw ex;
            }

            // have SignatureAlgorithm sign the input bytes and compare them to
            // the bytes that were stored in the signature.
            if (!sa.verify(sigBytes)) {
                LOG.warn("Signature verification failed.");
                return false;
            }

            return si.verify(this.followManifestsDuringValidation);
        } catch (XMLSignatureException ex) {
            throw ex;
        } catch (XMLSecurityException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * Add a Reference with full parameters to this Signature
     *
     * @param referenceURI URI of the resource to be signed. Can be null in
     * which case the dereferencing is application specific. Can be "" in which
     * it's the parent node (or parent document?). There can only be one "" in
     * each signature.
     * @param trans Optional list of transformations to be done before digesting
     * @param digestURI Mandatory URI of the digesting algorithm to use.
     * @param referenceId Optional id attribute for this Reference
     * @param referenceType Optional mimetype for the URI
     * @throws XMLSignatureException
     */
    public void addDocument(
        String referenceURI,
        Transforms trans,
        String digestURI,
        String referenceId,
        String referenceType
    ) throws XMLSignatureException {
        this.signedInfo.addDocument(
            this.baseURI, referenceURI, trans, digestURI, referenceId, referenceType
        );
    }

    /**
     * This method is a proxy method for the {@link Manifest#addDocument} method.
     *
     * @param referenceURI URI according to the XML Signature specification.
     * @param trans List of transformations to be applied.
     * @param digestURI URI of the digest algorithm to be used.
     * @see Manifest#addDocument
     * @throws XMLSignatureException
     */
    public void addDocument(
        String referenceURI,
        Transforms trans,
        String digestURI
    ) throws XMLSignatureException {
        this.signedInfo.addDocument(this.baseURI, referenceURI, trans, digestURI, null, null);
    }

    /**
     * Adds a Reference with just the URI and the transforms. This used the
     * SHA1 algorithm as a default digest algorithm.
     *
     * @param referenceURI URI according to the XML Signature specification.
     * @param trans List of transformations to be applied.
     * @throws XMLSignatureException
     */
    public void addDocument(String referenceURI, Transforms trans)
        throws XMLSignatureException {
        this.signedInfo.addDocument(
            this.baseURI, referenceURI, trans, Constants.ALGO_ID_DIGEST_SHA1, null, null
        );
    }

    /**
     * Add a Reference with just this URI. It uses SHA1 by default as the digest
     * algorithm
     *
     * @param referenceURI URI according to the XML Signature specification.
     * @throws XMLSignatureException
     */
    public void addDocument(String referenceURI) throws XMLSignatureException {
        this.signedInfo.addDocument(
            this.baseURI, referenceURI, null, Constants.ALGO_ID_DIGEST_SHA1, null, null
        );
    }

    /**
     * Add an X509 Certificate to the KeyInfo. This will include the whole cert
     * inside X509Data/X509Certificate tags.
     *
     * @param cert Certificate to be included. This should be the certificate of
     * the key that was used to sign.
     * @throws XMLSecurityException
     */
    public void addKeyInfo(X509Certificate cert) throws XMLSecurityException {
        X509Data x509data = new X509Data(getDocument());

        x509data.addCertificate(cert);
        this.getKeyInfo().add(x509data);
    }

    /**
     * Add this public key to the KeyInfo. This will include the complete key in
     * the KeyInfo structure.
     *
     * @param pk
     */
    public void addKeyInfo(PublicKey pk) {
        this.getKeyInfo().add(pk);
    }

    /**
     * Proxy method for {@link SignedInfo#createSecretKey(byte[])}. If you want
     * to create a MAC, this method helps you to obtain the
     * {@link javax.crypto.SecretKey} from octets.
     *
     * @param secretKeyBytes
     * @return the secret key created.
     * @see SignedInfo#createSecretKey(byte[])
     */
    public SecretKey createSecretKey(byte[] secretKeyBytes) {
        return this.getSignedInfo().createSecretKey(secretKeyBytes);
    }

    /**
     * Signal whether Manifest should be automatically validated.
     * Checking the digests in References in a Signature are mandatory, but for
     * References inside a Manifest it is application specific. This boolean is
     * to indicate that the References inside Manifests should be validated.
     *
     * @param followManifests
     * @see <a href="http://www.w3.org/TR/xmldsig-core/#sec-CoreValidation">
     * Core validation section in the XML Signature Rec.</a>
     */
    public void setFollowNestedManifests(boolean followManifests) {
        this.followManifestsDuringValidation = followManifests;
    }

    /**
     * Get the local name of this element
     *
     * @return Constants._TAG_SIGNATURE
     */
    public String getBaseLocalName() {
        return Constants._TAG_SIGNATURE;
    }
}
