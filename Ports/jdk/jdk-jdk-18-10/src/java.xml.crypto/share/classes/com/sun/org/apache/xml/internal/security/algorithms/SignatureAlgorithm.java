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
package com.sun.org.apache.xml.internal.security.algorithms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.org.apache.xml.internal.security.algorithms.implementations.IntegrityHmac;
import com.sun.org.apache.xml.internal.security.algorithms.implementations.SignatureBaseRSA;
import com.sun.org.apache.xml.internal.security.algorithms.implementations.SignatureDSA;
import com.sun.org.apache.xml.internal.security.algorithms.implementations.SignatureECDSA;
import com.sun.org.apache.xml.internal.security.exceptions.AlgorithmAlreadyRegisteredException;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignature;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureException;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.JavaUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Allows selection of digital signature's algorithm, private keys, other
 * security parameters, and algorithm's ID.
 *
 */
public class SignatureAlgorithm extends Algorithm {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(SignatureAlgorithm.class);

    /** All available algorithm classes are registered here */
    private static Map<String, Class<? extends SignatureAlgorithmSpi>> algorithmHash =
        new ConcurrentHashMap<>();

    /** Field signatureAlgorithm */
    private final SignatureAlgorithmSpi signatureAlgorithmSpi;

    private final String algorithmURI;

    /**
     * Constructor SignatureAlgorithm
     *
     * @param doc
     * @param algorithmURI
     * @throws XMLSecurityException
     */
    public SignatureAlgorithm(Document doc, String algorithmURI) throws XMLSecurityException {
        this(doc, algorithmURI, null);
    }

    public SignatureAlgorithm(Document doc, String algorithmURI, Provider provider) throws XMLSecurityException {
        this(doc, algorithmURI, provider, null);
    }

    public SignatureAlgorithm(Document doc, String algorithmURI, Provider provider, AlgorithmParameterSpec parameterSpec) throws XMLSecurityException {
        super(doc, algorithmURI);
        this.algorithmURI = algorithmURI;

        signatureAlgorithmSpi = getSignatureAlgorithmSpi(algorithmURI, provider);
        if (parameterSpec != null) {
            signatureAlgorithmSpi.engineSetParameter(parameterSpec);
            signatureAlgorithmSpi.engineAddContextToElement(getElement());
        }
    }

    /**
     * Constructor SignatureAlgorithm
     *
     * @param doc
     * @param algorithmURI
     * @param hmacOutputLength
     * @throws XMLSecurityException
     */
    public SignatureAlgorithm(
        Document doc, String algorithmURI, int hmacOutputLength
    ) throws XMLSecurityException {
        this(doc, algorithmURI, hmacOutputLength, null);
    }

    public SignatureAlgorithm(
        Document doc, String algorithmURI, int hmacOutputLength, Provider provider
    ) throws XMLSecurityException {
        super(doc, algorithmURI);
        this.algorithmURI = algorithmURI;

        signatureAlgorithmSpi = getSignatureAlgorithmSpi(algorithmURI, provider);

        signatureAlgorithmSpi.engineSetHMACOutputLength(hmacOutputLength);
        signatureAlgorithmSpi.engineAddContextToElement(getElement());
    }

    /**
     * Constructor SignatureAlgorithm
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public SignatureAlgorithm(Element element, String baseURI) throws XMLSecurityException {
        this(element, baseURI, true, null);
    }

    public SignatureAlgorithm(Element element, String baseURI, Provider provider) throws XMLSecurityException {
        this(element, baseURI, true, provider);
    }

    /**
     * Constructor SignatureAlgorithm
     *
     * @param element
     * @param baseURI
     * @param secureValidation
     * @throws XMLSecurityException
     */
    public SignatureAlgorithm(
        Element element, String baseURI, boolean secureValidation
    ) throws XMLSecurityException {
        this(element, baseURI, secureValidation, null);
    }

    public SignatureAlgorithm(
        Element element, String baseURI, boolean secureValidation, Provider provider
    ) throws XMLSecurityException {
        super(element, baseURI);
        algorithmURI = this.getURI();

        Attr attr = element.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            element.setIdAttributeNode(attr, true);
        }

        if (secureValidation && (XMLSignature.ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5.equals(algorithmURI)
            || XMLSignature.ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5.equals(algorithmURI))) {
            Object[] exArgs = { algorithmURI };

            throw new XMLSecurityException("signature.signatureAlgorithm", exArgs);
        }

        signatureAlgorithmSpi = getSignatureAlgorithmSpi(algorithmURI, provider);
        signatureAlgorithmSpi.engineGetContextFromElement(getElement());
    }

    /**
     * Get a SignatureAlgorithmSpi object corresponding to the algorithmURI argument
     */
    private static SignatureAlgorithmSpi getSignatureAlgorithmSpi(String algorithmURI, Provider provider)
        throws XMLSignatureException {
        try {
            Class<? extends SignatureAlgorithmSpi> implementingClass = algorithmHash.get(algorithmURI);
            LOG.debug("Create URI \"{}\" class \"{}\"", algorithmURI, implementingClass);
            if (implementingClass == null) {
                Object[] exArgs = { algorithmURI };
                throw new XMLSignatureException("algorithms.NoSuchAlgorithmNoEx", exArgs);
            }

            if (provider != null) {
                try {
                    Constructor<? extends SignatureAlgorithmSpi> constructor = implementingClass.getConstructor(Provider.class);
                    return constructor.newInstance(provider);

                } catch (NoSuchMethodException e) {
                    LOG.warn("Class \"{}\" does not have a constructor with Provider", implementingClass);
                }
            }

            return JavaUtils.newInstanceWithEmptyConstructor(implementingClass);

        }  catch (IllegalAccessException | InstantiationException | InvocationTargetException | NullPointerException ex) {
            Object[] exArgs = { algorithmURI, ex.getMessage() };
            throw new XMLSignatureException(ex, "algorithms.NoSuchAlgorithm", exArgs);
        }
    }


    /**
     * Proxy method for {@link java.security.Signature#sign()}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @return the result of the {@link java.security.Signature#sign()} method
     * @throws XMLSignatureException
     */
    public byte[] sign() throws XMLSignatureException {
        return signatureAlgorithmSpi.engineSign();
    }

    /**
     * Proxy method for {@link java.security.Signature#getAlgorithm}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @return the result of the {@link java.security.Signature#getAlgorithm} method
     */
    public String getJCEAlgorithmString() {
        return signatureAlgorithmSpi.engineGetJCEAlgorithmString();
    }

    /**
     * Method getJCEProviderName
     *
     * @return The Provider of this Signature Algorithm
     */
    public String getJCEProviderName() {
        return signatureAlgorithmSpi.engineGetJCEProviderName();
    }

    /**
     * Proxy method for {@link java.security.Signature#update(byte[])}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param input
     * @throws XMLSignatureException
     */
    public void update(byte[] input) throws XMLSignatureException {
        signatureAlgorithmSpi.engineUpdate(input);
    }

    /**
     * Proxy method for {@link java.security.Signature#update(byte)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param input
     * @throws XMLSignatureException
     */
    public void update(byte input) throws XMLSignatureException {
        signatureAlgorithmSpi.engineUpdate(input);
    }

    /**
     * Proxy method for {@link java.security.Signature#update(byte[], int, int)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param buf
     * @param offset
     * @param len
     * @throws XMLSignatureException
     */
    public void update(byte[] buf, int offset, int len) throws XMLSignatureException {
        signatureAlgorithmSpi.engineUpdate(buf, offset, len);
    }

    /**
     * Proxy method for {@link java.security.Signature#initSign(java.security.PrivateKey)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param signingKey
     * @throws XMLSignatureException
     */
    public void initSign(Key signingKey) throws XMLSignatureException {
        signatureAlgorithmSpi.engineInitSign(signingKey);
    }

    /**
     * Proxy method for {@link java.security.Signature#initSign(java.security.PrivateKey,
     * java.security.SecureRandom)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param signingKey
     * @param secureRandom
     * @throws XMLSignatureException
     */
    public void initSign(Key signingKey, SecureRandom secureRandom) throws XMLSignatureException {
        signatureAlgorithmSpi.engineInitSign(signingKey, secureRandom);
    }

    /**
     * Proxy method for {@link java.security.Signature#initSign(java.security.PrivateKey)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param signingKey
     * @param algorithmParameterSpec
     * @throws XMLSignatureException
     */
    public void initSign(
        Key signingKey, AlgorithmParameterSpec algorithmParameterSpec
    ) throws XMLSignatureException {
        signatureAlgorithmSpi.engineInitSign(signingKey, algorithmParameterSpec);
    }

    /**
     * Proxy method for {@link java.security.Signature#setParameter(
     * java.security.spec.AlgorithmParameterSpec)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param params
     * @throws XMLSignatureException
     */
    public void setParameter(AlgorithmParameterSpec params) throws XMLSignatureException {
        signatureAlgorithmSpi.engineSetParameter(params);
    }

    /**
     * Proxy method for {@link java.security.Signature#initVerify(java.security.PublicKey)}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param verificationKey
     * @throws XMLSignatureException
     */
    public void initVerify(Key verificationKey) throws XMLSignatureException {
        signatureAlgorithmSpi.engineInitVerify(verificationKey);
    }

    /**
     * Proxy method for {@link java.security.Signature#verify(byte[])}
     * which is executed on the internal {@link java.security.Signature} object.
     *
     * @param signature
     * @return true if the signature is valid.
     *
     * @throws XMLSignatureException
     */
    public boolean verify(byte[] signature) throws XMLSignatureException {
        return signatureAlgorithmSpi.engineVerify(signature);
    }

    /**
     * Returns the URI representation of Transformation algorithm
     *
     * @return the URI representation of Transformation algorithm
     */
    public final String getURI() {
        return getLocalAttribute(Constants._ATT_ALGORITHM);
    }

    /**
     * Registers implementing class of the SignatureAlgorithm with algorithmURI
     *
     * @param algorithmURI algorithmURI URI representation of {@code SignatureAlgorithm}.
     * @param implementingClass {@code implementingClass} the implementing class of
     * {@link SignatureAlgorithmSpi}
     * @throws AlgorithmAlreadyRegisteredException if specified algorithmURI is already registered
     * @throws XMLSignatureException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the signature algorithm
     */
    @SuppressWarnings("unchecked")
    public static void register(String algorithmURI, String implementingClass)
       throws AlgorithmAlreadyRegisteredException, ClassNotFoundException,
           XMLSignatureException {
        JavaUtils.checkRegisterPermission();
        LOG.debug("Try to register {} {}", algorithmURI, implementingClass);

        // are we already registered?
        Class<? extends SignatureAlgorithmSpi> registeredClass = algorithmHash.get(algorithmURI);
        if (registeredClass != null) {
            Object[] exArgs = { algorithmURI, registeredClass };
            throw new AlgorithmAlreadyRegisteredException(
                "algorithm.alreadyRegistered", exArgs
            );
        }
        try {
            Class<? extends SignatureAlgorithmSpi> clazz =
                (Class<? extends SignatureAlgorithmSpi>)
                    ClassLoaderUtils.loadClass(implementingClass, SignatureAlgorithm.class);
            algorithmHash.put(algorithmURI, clazz);
        } catch (NullPointerException ex) {
            Object[] exArgs = { algorithmURI, ex.getMessage() };
            throw new XMLSignatureException(ex, "algorithms.NoSuchAlgorithm", exArgs);
        }
    }

    /**
     * Registers implementing class of the SignatureAlgorithm with algorithmURI
     *
     * @param algorithmURI algorithmURI URI representation of {@code SignatureAlgorithm}.
     * @param implementingClass {@code implementingClass} the implementing class of
     * {@link SignatureAlgorithmSpi}
     * @throws AlgorithmAlreadyRegisteredException if specified algorithmURI is already registered
     * @throws XMLSignatureException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the signature algorithm
     */
    public static void register(String algorithmURI, Class<? extends SignatureAlgorithmSpi> implementingClass)
       throws AlgorithmAlreadyRegisteredException, ClassNotFoundException,
           XMLSignatureException {
        JavaUtils.checkRegisterPermission();
        LOG.debug("Try to register {} {}", algorithmURI, implementingClass);

        // are we already registered?
        Class<? extends SignatureAlgorithmSpi> registeredClass = algorithmHash.get(algorithmURI);
        if (registeredClass != null) {
            Object[] exArgs = { algorithmURI, registeredClass };
            throw new AlgorithmAlreadyRegisteredException(
                "algorithm.alreadyRegistered", exArgs
            );
        }
        algorithmHash.put(algorithmURI, implementingClass);
    }

    /**
     * This method registers the default algorithms.
     */
    public static void registerDefaultAlgorithms() {
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_DSA, SignatureDSA.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_DSA_SHA256, SignatureDSA.SHA256.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, SignatureBaseRSA.SignatureRSASHA1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_SHA1, IntegrityHmac.IntegrityHmacSHA1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_NOT_RECOMMENDED_RSA_MD5,
            SignatureBaseRSA.SignatureRSAMD5.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_RIPEMD160,
            SignatureBaseRSA.SignatureRSARIPEMD160.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA224, SignatureBaseRSA.SignatureRSASHA224.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256, SignatureBaseRSA.SignatureRSASHA256.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384, SignatureBaseRSA.SignatureRSASHA384.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512, SignatureBaseRSA.SignatureRSASHA512.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1_MGF1, SignatureBaseRSA.SignatureRSASHA1MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA224_MGF1, SignatureBaseRSA.SignatureRSASHA224MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256_MGF1, SignatureBaseRSA.SignatureRSASHA256MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384_MGF1, SignatureBaseRSA.SignatureRSASHA384MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512_MGF1, SignatureBaseRSA.SignatureRSASHA512MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_PSS, SignatureBaseRSA.SignatureRSASSAPSS.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA3_224_MGF1, SignatureBaseRSA.SignatureRSASHA3_224MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA3_256_MGF1, SignatureBaseRSA.SignatureRSASHA3_256MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA3_384_MGF1, SignatureBaseRSA.SignatureRSASHA3_384MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA3_512_MGF1, SignatureBaseRSA.SignatureRSASHA3_512MGF1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA1, SignatureECDSA.SignatureECDSASHA1.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA224, SignatureECDSA.SignatureECDSASHA224.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA256, SignatureECDSA.SignatureECDSASHA256.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA384, SignatureECDSA.SignatureECDSASHA384.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512, SignatureECDSA.SignatureECDSASHA512.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_RIPEMD160, SignatureECDSA.SignatureECDSARIPEMD160.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_NOT_RECOMMENDED_MD5, IntegrityHmac.IntegrityHmacMD5.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_RIPEMD160, IntegrityHmac.IntegrityHmacRIPEMD160.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_SHA224, IntegrityHmac.IntegrityHmacSHA224.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_SHA256, IntegrityHmac.IntegrityHmacSHA256.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_SHA384, IntegrityHmac.IntegrityHmacSHA384.class
        );
        algorithmHash.put(
            XMLSignature.ALGO_ID_MAC_HMAC_SHA512, IntegrityHmac.IntegrityHmacSHA512.class
        );
    }

    /**
     * Method getBaseNamespace
     *
     * @return URI of this element
     */
    public String getBaseNamespace() {
        return Constants.SignatureSpecNS;
    }

    /**
     * Method getBaseLocalName
     *
     * @return Local name
     */
    public String getBaseLocalName() {
        return Constants._TAG_SIGNATUREMETHOD;
    }
}
