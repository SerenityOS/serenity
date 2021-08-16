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
/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.spec.RSAPSSParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;

import java.io.IOException;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.jcp.xml.dsig.internal.SignerOutputStream;
import com.sun.org.apache.xml.internal.security.algorithms.implementations.SignatureBaseRSA.SignatureRSASSAPSS.DigestAlgorithm;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;

/**
 * DOM-based abstract implementation of SignatureMethod for RSA-PSS.
 */
public abstract class DOMRSAPSSSignatureMethod extends AbstractDOMSignatureMethod {

    private static final String DOM_SIGNATURE_PROVIDER = "org.jcp.xml.dsig.internal.dom.SignatureProvider";

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(DOMRSAPSSSignatureMethod.class);

    private final SignatureMethodParameterSpec params;
    private Signature signature;

    // see RFC 6931 for these algorithm definitions
    static final String RSA_PSS =
        "http://www.w3.org/2007/05/xmldsig-more#rsa-pss";

    private static final RSAPSSParameterSpec DEFAULT_PSS_SPEC
            = new RSAPSSParameterSpec(new PSSParameterSpec(
                "SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"),
                32, PSSParameterSpec.TRAILER_FIELD_BC));

    private PSSParameterSpec spec;

    /**
     * Creates a {@code DOMSignatureMethod}.
     *
     * @param params the algorithm-specific params (may be {@code null})
     * @throws InvalidAlgorithmParameterException if the parameters are not
     *    appropriate for this signature method
     */
    DOMRSAPSSSignatureMethod(AlgorithmParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params != null &&
            !(params instanceof SignatureMethodParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("params must be of type SignatureMethodParameterSpec");
        }
        if (params == null) {
            params = DEFAULT_PSS_SPEC;
        }
        checkParams((SignatureMethodParameterSpec)params);
        this.params = (SignatureMethodParameterSpec)params;
    }

    /**
     * Creates a {@code DOMSignatureMethod} from an element. This ctor
     * invokes the {@link #unmarshalParams unmarshalParams} method to
     * unmarshal any algorithm-specific input parameters.
     *
     * @param smElem a SignatureMethod element
     */
    DOMRSAPSSSignatureMethod(Element smElem) throws MarshalException {
        Element paramsElem = DOMUtils.getFirstChildElement(smElem);
        if (paramsElem != null) {
            params = unmarshalParams(paramsElem);
        } else {
            params = DEFAULT_PSS_SPEC;
        }
        try {
            checkParams(params);
        } catch (InvalidAlgorithmParameterException iape) {
            throw new MarshalException(iape);
        }
    }

    @Override
    void checkParams(SignatureMethodParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (!(params instanceof RSAPSSParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("params must be of type RSAPSSParameterSpec");
        }

        spec = ((RSAPSSParameterSpec) params).getPSSParameterSpec();
        LOG.debug("Setting RSAPSSParameterSpec to: {}", params.toString());
    }

    public final AlgorithmParameterSpec getParameterSpec() {
        return params;
    }

    void marshalParams(Element parent, String prefix)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);

        Element rsaPssParamsElement = ownerDoc.createElementNS(Constants.XML_DSIG_NS_MORE_07_05, "pss" + ":" + Constants._TAG_RSAPSSPARAMS);
        rsaPssParamsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:" + "pss", Constants.XML_DSIG_NS_MORE_07_05);

        DigestAlgorithm digestAlgorithm;
        try {
            digestAlgorithm = DigestAlgorithm.fromDigestAlgorithm(spec.getDigestAlgorithm());
            String xmlDigestAlgorithm = digestAlgorithm.getXmlDigestAlgorithm();
            if (!xmlDigestAlgorithm.equals(DigestMethod.SHA256)) {
                Element digestMethodElement = DOMUtils.createElement(rsaPssParamsElement.getOwnerDocument(), Constants._TAG_DIGESTMETHOD,
                        XMLSignature.XMLNS, prefix);
                digestMethodElement.setAttributeNS(null, Constants._ATT_ALGORITHM, xmlDigestAlgorithm);
                rsaPssParamsElement.appendChild(digestMethodElement);
            }
            if (spec.getSaltLength() != digestAlgorithm.getSaltLength()) {
                Element saltLengthElement = rsaPssParamsElement.getOwnerDocument().createElementNS(Constants.XML_DSIG_NS_MORE_07_05, "pss" + ":" + Constants._TAG_SALTLENGTH);
                Text saltLengthText = rsaPssParamsElement.getOwnerDocument().createTextNode(String.valueOf(spec.getSaltLength()));
                saltLengthElement.appendChild(saltLengthText);
                rsaPssParamsElement.appendChild(saltLengthElement);
            }
        } catch (DOMException | com.sun.org.apache.xml.internal.security.signature.XMLSignatureException e) {
            throw new MarshalException("Invalid digest name supplied: " + spec.getDigestAlgorithm());
        }

        if (!spec.getMGFAlgorithm().equals("MGF1")) {
            throw new MarshalException("Unsupported MGF algorithm supplied: " + spec.getMGFAlgorithm());
        }

        MGF1ParameterSpec mgfSpec = (MGF1ParameterSpec)spec.getMGFParameters();
        try {
            DigestAlgorithm mgfDigestAlgorithm = DigestAlgorithm.fromDigestAlgorithm(mgfSpec.getDigestAlgorithm());
            if (mgfDigestAlgorithm != digestAlgorithm) {
                Element mgfElement = rsaPssParamsElement.getOwnerDocument().createElementNS(Constants.XML_DSIG_NS_MORE_07_05, "pss" + ":" + Constants._TAG_MGF);
                try {
                    mgfElement.setAttributeNS(null, Constants._ATT_ALGORITHM, "http://www.w3.org/2007/05/xmldsig-more#MGF1");
                } catch (DOMException e) {
                    throw new MarshalException("Should not happen");
                }
                Element mgfDigestMethodElement = DOMUtils.createElement(rsaPssParamsElement.getOwnerDocument(), Constants._TAG_DIGESTMETHOD,
                        XMLSignature.XMLNS, prefix);
                String xmlDigestAlgorithm = mgfDigestAlgorithm.getXmlDigestAlgorithm();
                mgfDigestMethodElement.setAttributeNS(null, Constants._ATT_ALGORITHM, xmlDigestAlgorithm);
                mgfElement.appendChild(mgfDigestMethodElement);
                rsaPssParamsElement.appendChild(mgfElement);
            }
        } catch (DOMException | com.sun.org.apache.xml.internal.security.signature.XMLSignatureException e) {
            throw new MarshalException("Invalid digest name supplied: " + mgfSpec.getDigestAlgorithm());
        }

        if (spec.getTrailerField() != 1) {
            Element trailerFieldElement = rsaPssParamsElement.getOwnerDocument().createElementNS(Constants.XML_DSIG_NS_MORE_07_05, "pss" + ":" + Constants._TAG_TRAILERFIELD);
            Text trailerFieldText = rsaPssParamsElement.getOwnerDocument().createTextNode(String.valueOf(spec.getTrailerField()));
            trailerFieldElement.appendChild(trailerFieldText);
            rsaPssParamsElement.appendChild(trailerFieldElement);
        }

        if (rsaPssParamsElement.hasChildNodes()) {
            parent.appendChild(rsaPssParamsElement);
        }
    }

    private static DigestAlgorithm validateDigestAlgorithm(String input)
            throws MarshalException {
        try {
            return DigestAlgorithm.fromXmlDigestAlgorithm(input);
        } catch (com.sun.org.apache.xml.internal.security.signature.XMLSignatureException e) {
            throw new MarshalException("Invalid digest algorithm supplied: " + input);
        }
    }

    SignatureMethodParameterSpec unmarshalParams(Element paramsElem)
        throws MarshalException
    {
        if (paramsElem != null) {
            Element saltLengthNode = XMLUtils.selectNode(paramsElem.getFirstChild(), Constants.XML_DSIG_NS_MORE_07_05, Constants._TAG_SALTLENGTH, 0);
            Element trailerFieldNode = XMLUtils.selectNode(paramsElem.getFirstChild(), Constants.XML_DSIG_NS_MORE_07_05, Constants._TAG_TRAILERFIELD, 0);
            Element digestAlgorithmNode = XMLUtils.selectDsNode(paramsElem.getFirstChild(), Constants._TAG_DIGESTMETHOD, 0);
            Element mgfNode = XMLUtils.selectNode(paramsElem.getFirstChild(), Constants.XML_DSIG_NS_MORE_07_05, Constants._TAG_MGF, 0);

            DigestAlgorithm digestAlgorithm = digestAlgorithmNode != null
                    ? validateDigestAlgorithm(digestAlgorithmNode.getAttribute(Constants._ATT_ALGORITHM))
                    : DigestAlgorithm.SHA256;

            DigestAlgorithm mgfDigestAlgorithm = digestAlgorithm;
            if (mgfNode != null) {
                String mgfAlgorithm = mgfNode.getAttribute(Constants._ATT_ALGORITHM);
                if (!mgfAlgorithm.equals("http://www.w3.org/2007/05/xmldsig-more#MGF1")) {
                    throw new MarshalException("Unknown MGF algorithm: " + mgfAlgorithm);
                }
                Element mgfDigestAlgorithmNode = XMLUtils.selectDsNode(mgfNode.getFirstChild(), Constants._TAG_DIGESTMETHOD, 0);
                if (mgfDigestAlgorithmNode != null) {
                    mgfDigestAlgorithm = validateDigestAlgorithm(mgfDigestAlgorithmNode.getAttribute(Constants._ATT_ALGORITHM));
                }
            }

            int saltLength;
            try {
                saltLength = saltLengthNode == null ? digestAlgorithm.getSaltLength() : Integer.parseUnsignedInt(saltLengthNode.getTextContent());
            } catch (NumberFormatException ex) {
                throw new MarshalException("Invalid salt length supplied: " + saltLengthNode.getTextContent());
            }

            int trailerField;
            try {
                trailerField = trailerFieldNode == null ? 1 : Integer.parseUnsignedInt(trailerFieldNode.getTextContent());
            } catch (NumberFormatException ex) {
                throw new MarshalException("Invalid trailer field supplied: " + trailerFieldNode.getTextContent());
            }

            return new RSAPSSParameterSpec(new PSSParameterSpec(
                    digestAlgorithm.getDigestAlgorithm(),
                    "MGF1", new MGF1ParameterSpec(mgfDigestAlgorithm.getDigestAlgorithm()),
                    saltLength, trailerField));
        }
        return DEFAULT_PSS_SPEC;
    }

    boolean verify(Key key, SignedInfo si, byte[] sig,
                   XMLValidateContext context)
        throws InvalidKeyException, SignatureException, XMLSignatureException
    {
        if (key == null || si == null || sig == null) {
            throw new NullPointerException();
        }

        if (!(key instanceof PublicKey)) {
            throw new InvalidKeyException("key must be PublicKey");
        }
        if (signature == null) {
            try {
                Provider p = (Provider)context.getProperty(DOM_SIGNATURE_PROVIDER);
                signature = (p == null)
                    ? Signature.getInstance(getJCAAlgorithm())
                    : Signature.getInstance(getJCAAlgorithm(), p);
            } catch (NoSuchAlgorithmException nsae) {
                throw new XMLSignatureException(nsae);
            }
        }
        signature.initVerify((PublicKey)key);
        try {
            signature.setParameter(spec);
        } catch (InvalidAlgorithmParameterException e) {
            throw new XMLSignatureException(e);
        }
        LOG.debug("Signature provider: {}", signature.getProvider());
        LOG.debug("Verifying with key: {}", key);
        LOG.debug("JCA Algorithm: {}", getJCAAlgorithm());
        LOG.debug("Signature Bytes length: {}", sig.length);

        try (SignerOutputStream outputStream = new SignerOutputStream(signature)) {
            ((DOMSignedInfo)si).canonicalize(context, outputStream);

            return signature.verify(sig);
        } catch (IOException ioe) {
            throw new XMLSignatureException(ioe);
        }
    }

    byte[] sign(Key key, SignedInfo si, XMLSignContext context)
        throws InvalidKeyException, XMLSignatureException
    {
        if (key == null || si == null) {
            throw new NullPointerException();
        }

        if (!(key instanceof PrivateKey)) {
            throw new InvalidKeyException("key must be PrivateKey");
        }
        if (signature == null) {
            try {
                Provider p = (Provider)context.getProperty(DOM_SIGNATURE_PROVIDER);
                signature = (p == null)
                    ? Signature.getInstance(getJCAAlgorithm())
                    : Signature.getInstance(getJCAAlgorithm(), p);
            } catch (NoSuchAlgorithmException nsae) {
                throw new XMLSignatureException(nsae);
            }
        }
        signature.initSign((PrivateKey)key);
        try {
            signature.setParameter(spec);
        } catch (InvalidAlgorithmParameterException e) {
            throw new XMLSignatureException(e);
        }
        LOG.debug("Signature provider: {}", signature.getProvider());
        LOG.debug("Signing with key: {}", key);
        LOG.debug("JCA Algorithm: {}", getJCAAlgorithm());

        try (SignerOutputStream outputStream = new SignerOutputStream(signature)) {
            ((DOMSignedInfo)si).canonicalize(context, outputStream);

            return signature.sign();
        } catch (SignatureException | IOException e) {
            throw new XMLSignatureException(e);
        }
    }

    @Override
    boolean paramsEqual(AlgorithmParameterSpec spec) {
        return getParameterSpec().equals(spec);
    }

    static final class RSAPSS extends DOMRSAPSSSignatureMethod {
        RSAPSS(AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            super(params);
        }
        RSAPSS(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return RSA_PSS;
        }
        @Override
        String getJCAAlgorithm() {
            return "RSASSA-PSS";
        }
        @Override
        Type getAlgorithmType() {
            return Type.RSA;
        }
    }

}
