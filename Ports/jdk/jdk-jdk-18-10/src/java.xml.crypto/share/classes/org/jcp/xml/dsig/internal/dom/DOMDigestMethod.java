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
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.spec.DigestMethodParameterSpec;

import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * DOM-based abstract implementation of DigestMethod.
 *
 */
public abstract class DOMDigestMethod extends DOMStructure
    implements DigestMethod {

    static final String SHA224 =
        "http://www.w3.org/2001/04/xmldsig-more#sha224"; // see RFC 4051
    static final String SHA384 =
        "http://www.w3.org/2001/04/xmldsig-more#sha384"; // see RFC 4051
    static final String WHIRLPOOL =
        "http://www.w3.org/2007/05/xmldsig-more#whirlpool"; // see RFC 6931
    static final String SHA3_224 =
        "http://www.w3.org/2007/05/xmldsig-more#sha3-224"; // see RFC 6931
    static final String SHA3_256 =
        "http://www.w3.org/2007/05/xmldsig-more#sha3-256"; // see RFC 6931
    static final String SHA3_384 =
        "http://www.w3.org/2007/05/xmldsig-more#sha3-384"; // see RFC 6931
    static final String SHA3_512 =
        "http://www.w3.org/2007/05/xmldsig-more#sha3-512"; // see RFC 6931

    private DigestMethodParameterSpec params;

    /**
     * Creates a {@code DOMDigestMethod}.
     *
     * @param params the algorithm-specific params (may be {@code null})
     * @throws InvalidAlgorithmParameterException if the parameters are not
     *    appropriate for this digest method
     */
    DOMDigestMethod(AlgorithmParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params != null && !(params instanceof DigestMethodParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("params must be of type DigestMethodParameterSpec");
        }
        checkParams((DigestMethodParameterSpec)params);
        this.params = (DigestMethodParameterSpec)params;
    }

    /**
     * Creates a {@code DOMDigestMethod} from an element. This constructor
     * invokes the abstract {@link #unmarshalParams unmarshalParams} method to
     * unmarshal any algorithm-specific input parameters.
     *
     * @param dmElem a DigestMethod element
     */
    DOMDigestMethod(Element dmElem) throws MarshalException {
        Element paramsElem = DOMUtils.getFirstChildElement(dmElem);
        if (paramsElem != null) {
            params = unmarshalParams(paramsElem);
        }
        try {
            checkParams(params);
        } catch (InvalidAlgorithmParameterException iape) {
            throw new MarshalException(iape);
        }
    }

    static DigestMethod unmarshal(Element dmElem) throws MarshalException {
        String alg = DOMUtils.getAttributeValue(dmElem, "Algorithm");
        if (alg.equals(DigestMethod.SHA1)) {
            return new SHA1(dmElem);
        } else if (alg.equals(SHA224)) {
            return new SHA224(dmElem);
        } else if (alg.equals(DigestMethod.SHA256)) {
            return new SHA256(dmElem);
        } else if (alg.equals(SHA384)) {
            return new SHA384(dmElem);
        } else if (alg.equals(DigestMethod.SHA512)) {
            return new SHA512(dmElem);
        } else if (alg.equals(DigestMethod.RIPEMD160)) {
            return new RIPEMD160(dmElem);
        } else if (alg.equals(WHIRLPOOL)) {
            return new WHIRLPOOL(dmElem);
        } else if (alg.equals(SHA3_224)) {
            return new SHA3_224(dmElem);
        } else if (alg.equals(SHA3_256)) {
            return new SHA3_256(dmElem);
        } else if (alg.equals(SHA3_384)) {
            return new SHA3_384(dmElem);
        } else if (alg.equals(SHA3_512)) {
            return new SHA3_512(dmElem);
        } else {
            throw new MarshalException("unsupported DigestMethod algorithm: " +
                                       alg);
        }
    }

    /**
     * Checks if the specified parameters are valid for this algorithm. By
     * default, this method throws an exception if parameters are specified
     * since most DigestMethod algorithms do not have parameters. Subclasses
     * should override it if they have parameters.
     *
     * @param params the algorithm-specific params (may be {@code null})
     * @throws InvalidAlgorithmParameterException if the parameters are not
     *    appropriate for this digest method
     */
    void checkParams(DigestMethodParameterSpec params)
        throws InvalidAlgorithmParameterException
    {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("no parameters " +
                "should be specified for the " + getMessageDigestAlgorithm() +
                " DigestMethod algorithm");
        }
    }

    public final AlgorithmParameterSpec getParameterSpec() {
        return params;
    }

    /**
     * Unmarshals {@code DigestMethodParameterSpec} from the specified
     * {@code Element}.  By default, this method throws an exception since
     * most DigestMethod algorithms do not have parameters. Subclasses should
     * override it if they have parameters.
     *
     * @param paramsElem the {@code Element} holding the input params
     * @return the algorithm-specific {@code DigestMethodParameterSpec}
     * @throws MarshalException if the parameters cannot be unmarshalled
     */
    DigestMethodParameterSpec unmarshalParams(Element paramsElem)
        throws MarshalException
    {
        throw new MarshalException("no parameters should " +
                                   "be specified for the " +
                                   getMessageDigestAlgorithm() +
                                   " DigestMethod algorithm");
    }

    /**
     * This method invokes the abstract {@link #marshalParams marshalParams}
     * method to marshal any algorithm-specific parameters.
     */
    @Override
    public void marshal(Node parent, String prefix, DOMCryptoContext context)
        throws MarshalException
    {
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);

        Element dmElem = DOMUtils.createElement(ownerDoc, "DigestMethod",
                                                XMLSignature.XMLNS, prefix);
        DOMUtils.setAttribute(dmElem, "Algorithm", getAlgorithm());

        if (params != null) {
            marshalParams(dmElem, prefix);
        }

        parent.appendChild(dmElem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DigestMethod)) {
            return false;
        }
        DigestMethod odm = (DigestMethod)o;

        boolean paramsEqual = params == null ? odm.getParameterSpec() == null :
            params.equals(odm.getParameterSpec());

        return getAlgorithm().equals(odm.getAlgorithm()) && paramsEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (params != null) {
            result = 31 * result + params.hashCode();
        }
        result = 31 * result + getAlgorithm().hashCode();

        return result;
    }

    /**
     * Marshals the algorithm-specific parameters to an Element and
     * appends it to the specified parent element. By default, this method
     * throws an exception since most DigestMethod algorithms do not have
     * parameters. Subclasses should override it if they have parameters.
     *
     * @param parent the parent element to append the parameters to
     * @param prefix the namespace prefix to use
     * @throws MarshalException if the parameters cannot be marshalled
     */
    void marshalParams(Element parent, String prefix)
        throws MarshalException
    {
        throw new MarshalException("no parameters should " +
                                   "be specified for the " +
                                   getMessageDigestAlgorithm() +
                                   " DigestMethod algorithm");
    }

    /**
     * Returns the MessageDigest standard algorithm name.
     */
    abstract String getMessageDigestAlgorithm();

    static final class SHA1 extends DOMDigestMethod {
        SHA1(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA1(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        public String getAlgorithm() {
            return DigestMethod.SHA1;
        }
        String getMessageDigestAlgorithm() {
            return "SHA-1";
        }
    }

    static final class SHA224 extends DOMDigestMethod {
        SHA224(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA224(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA224;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA-224";
        }
    }

    static final class SHA256 extends DOMDigestMethod {
        SHA256(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA256(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        public String getAlgorithm() {
            return DigestMethod.SHA256;
        }
        String getMessageDigestAlgorithm() {
            return "SHA-256";
        }
    }

    static final class SHA384 extends DOMDigestMethod {
        SHA384(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA384(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        public String getAlgorithm() {
            return SHA384;
        }
        String getMessageDigestAlgorithm() {
            return "SHA-384";
        }
    }

    static final class SHA512 extends DOMDigestMethod {
        SHA512(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA512(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        public String getAlgorithm() {
            return DigestMethod.SHA512;
        }
        String getMessageDigestAlgorithm() {
            return "SHA-512";
        }
    }

    static final class RIPEMD160 extends DOMDigestMethod {
        RIPEMD160(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        RIPEMD160(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return DigestMethod.RIPEMD160;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "RIPEMD160";
        }
    }

    static final class WHIRLPOOL extends DOMDigestMethod {
        WHIRLPOOL(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        WHIRLPOOL(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return WHIRLPOOL;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "WHIRLPOOL";
        }
    }

    static final class SHA3_224 extends DOMDigestMethod {
        SHA3_224(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA3_224(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA3_224;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA3-224";
        }
    }

    static final class SHA3_256 extends DOMDigestMethod {
        SHA3_256(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA3_256(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA3_256;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA3-256";
        }
    }

    static final class SHA3_384 extends DOMDigestMethod {
        SHA3_384(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA3_384(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA3_384;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA3-384";
        }
    }

    static final class SHA3_512 extends DOMDigestMethod {
        SHA3_512(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
            super(params);
        }
        SHA3_512(Element dmElem) throws MarshalException {
            super(dmElem);
        }
        @Override
        public String getAlgorithm() {
            return SHA3_512;
        }
        @Override
        String getMessageDigestAlgorithm() {
            return "SHA3-512";
        }
    }
}
