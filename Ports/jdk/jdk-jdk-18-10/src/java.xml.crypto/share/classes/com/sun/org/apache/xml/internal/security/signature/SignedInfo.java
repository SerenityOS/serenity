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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.sun.org.apache.xml.internal.security.algorithms.SignatureAlgorithm;
import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.c14n.InvalidCanonicalizerException;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.transforms.params.InclusiveNamespaces;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Handles {@code &lt;ds:SignedInfo&gt;} elements
 * This {@code SignedInfo} element includes the canonicalization algorithm,
 * a signature algorithm, and one or more references.
 *
 */
public class SignedInfo extends Manifest {

    /** Field signatureAlgorithm */
    private final SignatureAlgorithm signatureAlgorithm;

    /** Field c14nizedBytes           */
    private byte[] c14nizedBytes;

    private Element c14nMethod;
    private Element signatureMethod;

    /**
     * Overwrites {@link Manifest#addDocument} because it creates another
     * Element.
     *
     * @param doc the {@link Document} in which {@code XMLsignature} will
     *    be placed
     * @throws XMLSecurityException
     */
    public SignedInfo(Document doc) throws XMLSecurityException {
        this(doc, XMLSignature.ALGO_ID_SIGNATURE_DSA,
             Canonicalizer.ALGO_ID_C14N_OMIT_COMMENTS);
    }

    /**
     * Constructs {@link SignedInfo} using given Canonicalization algorithm and
     * Signature algorithm.
     *
     * @param doc {@code SignedInfo} is placed in this document
     * @param signatureMethodURI URI representation of the Digest and
     *    Signature algorithm
     * @param canonicalizationMethodURI URI representation of the
     *    Canonicalization method
     * @throws XMLSecurityException
     */
    public SignedInfo(
        Document doc, String signatureMethodURI, String canonicalizationMethodURI
    ) throws XMLSecurityException {
        this(doc, signatureMethodURI, 0, canonicalizationMethodURI, null, null);
    }

    /**
     * Constructs {@link SignedInfo} using given Canonicalization algorithm and
     * Signature algorithm.
     *
     * @param doc {@code SignedInfo} is placed in this document
     * @param signatureMethodURI URI representation of the Digest and
     *    Signature algorithm
     * @param canonicalizationMethodURI URI representation of the
     *    Canonicalization method
     * @param provider security provider to use
     * @throws XMLSecurityException
     */
    public SignedInfo(
        Document doc, String signatureMethodURI, String canonicalizationMethodURI, Provider provider
    ) throws XMLSecurityException {
        this(doc, signatureMethodURI, 0, canonicalizationMethodURI, provider, null);
    }

    /**
     * Constructor SignedInfo
     *
     * @param doc {@code SignedInfo} is placed in this document
     * @param signatureMethodURI URI representation of the Digest and
     *    Signature algorithm
     * @param hMACOutputLength
     * @param canonicalizationMethodURI URI representation of the
     *    Canonicalization method
     * @throws XMLSecurityException
     */
    public SignedInfo(
        Document doc, String signatureMethodURI,
        int hMACOutputLength, String canonicalizationMethodURI
    ) throws XMLSecurityException {
        this(doc, signatureMethodURI, hMACOutputLength, canonicalizationMethodURI, null, null);
    }

    /**
     * Constructs {@link SignedInfo} using given Canonicalization algorithm and
     * Signature algorithm.
     *
     * @param doc {@code SignedInfo} is placed in this document
     * @param signatureMethodURI URI representation of the Digest and
     *    Signature algorithm
     * @param hMACOutputLength
     * @param canonicalizationMethodURI URI representation of the
     *    Canonicalization method
     * @param provider security provider to use
     * @param spec AlgorithmParameterSpec to use
     * @throws XMLSecurityException
     */
    public SignedInfo(
        Document doc, String signatureMethodURI,
        int hMACOutputLength, String canonicalizationMethodURI, Provider provider, AlgorithmParameterSpec spec
    ) throws XMLSecurityException {
        super(doc);

        c14nMethod =
            XMLUtils.createElementInSignatureSpace(getDocument(), Constants._TAG_CANONICALIZATIONMETHOD);

        c14nMethod.setAttributeNS(null, Constants._ATT_ALGORITHM, canonicalizationMethodURI);
        appendSelf(c14nMethod);
        addReturnToSelf();

        if (hMACOutputLength > 0) {
            this.signatureAlgorithm =
                new SignatureAlgorithm(getDocument(), signatureMethodURI, hMACOutputLength, provider);
        } else {
            this.signatureAlgorithm = new SignatureAlgorithm(getDocument(), signatureMethodURI, provider, spec);
        }

        signatureMethod = this.signatureAlgorithm.getElement();
        appendSelf(signatureMethod);
        addReturnToSelf();
    }

    /**
     * @param doc
     * @param signatureMethodElem
     * @param canonicalizationMethodElem
     * @throws XMLSecurityException
     */
    public SignedInfo(
        Document doc, Element signatureMethodElem, Element canonicalizationMethodElem
    ) throws XMLSecurityException {
        this(doc, signatureMethodElem, canonicalizationMethodElem, null);
    }

    public SignedInfo(
        Document doc, Element signatureMethodElem, Element canonicalizationMethodElem, Provider provider
    ) throws XMLSecurityException {
        super(doc);
        // Check this?
        this.c14nMethod = canonicalizationMethodElem;
        appendSelf(c14nMethod);
        addReturnToSelf();

        this.signatureAlgorithm =
            new SignatureAlgorithm(signatureMethodElem, null, provider);

        signatureMethod = this.signatureAlgorithm.getElement();
        appendSelf(signatureMethod);

        addReturnToSelf();
    }

    /**
     * Build a {@link SignedInfo} from an {@link Element}
     *
     * @param element {@code SignedInfo}
     * @param baseURI the URI of the resource where the XML instance was stored
     * @throws XMLSecurityException
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0033.html">
     * Question</A>
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0054.html">
     * Answer</A>
     */
    public SignedInfo(Element element, String baseURI) throws XMLSecurityException {
        this(element, baseURI, true, null);
    }

    /**
     * Build a {@link SignedInfo} from an {@link Element}
     *
     * @param element {@code SignedInfo}
     * @param baseURI the URI of the resource where the XML instance was stored
     * @param secureValidation whether secure validation is enabled or not
     * @throws XMLSecurityException
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0033.html">
     * Question</A>
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0054.html">
     * Answer</A>
     */
    public SignedInfo(
        Element element, String baseURI, boolean secureValidation
    ) throws XMLSecurityException {
        this(element, baseURI, secureValidation, null);
    }

    /**
     * Build a {@link SignedInfo} from an {@link Element}
     *
     * @param element {@code SignedInfo}
     * @param baseURI the URI of the resource where the XML instance was stored
     * @param secureValidation whether secure validation is enabled or not
     * @param provider security provider to use
     * @throws XMLSecurityException
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0033.html">
     * Question</A>
     * @see <A HREF="http://lists.w3.org/Archives/Public/w3c-ietf-xmldsig/2001OctDec/0054.html">
     * Answer</A>
     */
    public SignedInfo(
        Element element, String baseURI, boolean secureValidation, Provider provider
    ) throws XMLSecurityException {
        super(element, baseURI, secureValidation);

        c14nMethod = XMLUtils.getNextElement(element.getFirstChild());
        if (c14nMethod == null ||
            !(Constants.SignatureSpecNS.equals(c14nMethod.getNamespaceURI())
                && Constants._TAG_CANONICALIZATIONMETHOD.equals(c14nMethod.getLocalName()))) {
            Object[] exArgs = { Constants._TAG_CANONICALIZATIONMETHOD, Constants._TAG_SIGNEDINFO };
            throw new XMLSignatureException("xml.WrongContent", exArgs);
        }

        signatureMethod = XMLUtils.getNextElement(c14nMethod.getNextSibling());
        if (signatureMethod == null ||
            !(Constants.SignatureSpecNS.equals(signatureMethod.getNamespaceURI())
                && Constants._TAG_SIGNATUREMETHOD.equals(signatureMethod.getLocalName()))) {
            Object[] exArgs = { Constants._TAG_SIGNATUREMETHOD, Constants._TAG_SIGNEDINFO };
            throw new XMLSignatureException("xml.WrongContent", exArgs);
        }

        this.signatureAlgorithm =
            new SignatureAlgorithm(signatureMethod, this.getBaseURI(), secureValidation, provider);
    }

    /**
     * Tests core validation process
     *
     * @return true if verification was successful
     * @throws MissingResourceFailureException
     * @throws XMLSecurityException
     */
    public boolean verify()
        throws MissingResourceFailureException, XMLSecurityException {
        return super.verifyReferences(false);
    }

    /**
     * Tests core validation process
     *
     * @param followManifests defines whether the verification process has to verify referenced {@code ds:Manifest}s, too
     * @return true if verification was successful
     * @throws MissingResourceFailureException
     * @throws XMLSecurityException
     */
    public boolean verify(boolean followManifests)
        throws MissingResourceFailureException, XMLSecurityException {
        return super.verifyReferences(followManifests);
    }

    /**
     * Returns getCanonicalizedOctetStream
     *
     * @return the canonicalization result octet stream of {@code SignedInfo} element
     * @throws CanonicalizationException
     * @throws InvalidCanonicalizerException
     * @throws XMLSecurityException
     * @throws IOException
     */
    public byte[] getCanonicalizedOctetStream()
        throws CanonicalizationException, InvalidCanonicalizerException, XMLSecurityException, IOException {
        if (this.c14nizedBytes == null) {
            Canonicalizer c14nizer =
                Canonicalizer.getInstance(this.getCanonicalizationMethodURI());

            String inclusiveNamespaces = this.getInclusiveNamespaces();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (inclusiveNamespaces == null) {
                    c14nizer.canonicalizeSubtree(getElement(), baos);
                } else {
                    c14nizer.canonicalizeSubtree(getElement(), inclusiveNamespaces, baos);
                }
                this.c14nizedBytes = baos.toByteArray();
            }
        }

        // make defensive copy
        return this.c14nizedBytes.clone();
    }

    /**
     * Output the C14n stream to the given OutputStream.
     * @param os
     * @throws CanonicalizationException
     * @throws InvalidCanonicalizerException
     * @throws XMLSecurityException
     */
    public void signInOctetStream(OutputStream os)
        throws CanonicalizationException, InvalidCanonicalizerException, XMLSecurityException {
        if (this.c14nizedBytes == null) {
            Canonicalizer c14nizer =
                Canonicalizer.getInstance(this.getCanonicalizationMethodURI());
            String inclusiveNamespaces = this.getInclusiveNamespaces();

            if (inclusiveNamespaces == null) {
                c14nizer.canonicalizeSubtree(getElement(), os);
            } else {
                c14nizer.canonicalizeSubtree(getElement(), inclusiveNamespaces, os);
            }
        } else {
            try {
                os.write(this.c14nizedBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns the Canonicalization method URI
     *
     * @return the Canonicalization method URI
     */
    public String getCanonicalizationMethodURI() {
        return c14nMethod.getAttributeNS(null, Constants._ATT_ALGORITHM);
    }

    /**
     * Returns the Signature method URI
     *
     * @return the Signature method URI
     */
    public String getSignatureMethodURI() {
        Element signatureElement = this.getSignatureMethodElement();

        if (signatureElement != null) {
            return signatureElement.getAttributeNS(null, Constants._ATT_ALGORITHM);
        }

        return null;
    }

    /**
     * Method getSignatureMethodElement
     * @return returns the SignatureMethod Element
     *
     */
    public Element getSignatureMethodElement() {
        return signatureMethod;
    }

    /**
     * Creates a SecretKey for the appropriate Mac algorithm based on a
     * byte[] array password.
     *
     * @param secretKeyBytes
     * @return the secret key for the SignedInfo element.
     */
    public SecretKey createSecretKey(byte[] secretKeyBytes) {
        return new SecretKeySpec(secretKeyBytes, this.signatureAlgorithm.getJCEAlgorithmString());
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Method getBaseLocalName
     * {@inheritDoc}
     *
     */
    public String getBaseLocalName() {
        return Constants._TAG_SIGNEDINFO;
    }

    public String getInclusiveNamespaces() {
        String c14nMethodURI = getCanonicalizationMethodURI();
        if (!("http://www.w3.org/2001/10/xml-exc-c14n#".equals(c14nMethodURI) ||
            "http://www.w3.org/2001/10/xml-exc-c14n#WithComments".equals(c14nMethodURI))) {
            return null;
        }

        Element inclusiveElement = XMLUtils.getNextElement(c14nMethod.getFirstChild());

        if (inclusiveElement != null) {
            try {
                String inclusiveNamespaces =
                    new InclusiveNamespaces(
                        inclusiveElement,
                        InclusiveNamespaces.ExclusiveCanonicalizationNamespace
                    ).getInclusiveNamespaces();
                return inclusiveNamespaces;
            } catch (XMLSecurityException e) {
                return null;
            }
        }
        return null;
    }
}
