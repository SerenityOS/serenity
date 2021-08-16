/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.tools.keytool;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.Date;

import sun.security.pkcs10.PKCS10;
import sun.security.util.SignatureUtil;
import sun.security.x509.*;

/**
 * Generate a pair of keys, and provide access to them.  This class is
 * provided primarily for ease of use.
 *
 * <P>This provides some simple certificate management functionality.
 * Specifically, it allows you to create self-signed X.509 certificates
 * as well as PKCS 10 based certificate signing requests.
 *
 * <P>Keys for some public key signature algorithms have algorithm
 * parameters, such as DSS/DSA.  Some sites' Certificate Authorities
 * adopt fixed algorithm parameters, which speeds up some operations
 * including key generation and signing.  <em>At this time, this interface
 * supports initializing with a named group.</em>
 *
 * <P>Also, note that at this time only signature-capable keys may be
 * acquired through this interface.  Diffie-Hellman keys, used for secure
 * key exchange, may be supported later.
 *
 * @author David Brownell
 * @author Hemma Prafullchandra
 * @see PKCS10
 * @see X509CertImpl
 */
public final class CertAndKeyGen {
    /**
     * Creates a CertAndKeyGen object for a particular key type
     * and signature algorithm.
     *
     * @param keyType type of key, e.g. "RSA", "DSA"
     * @param sigAlg name of the signature algorithm, e.g. "MD5WithRSA",
     *          "MD2WithRSA", "SHAwithDSA". If set to null, a default
     *          algorithm matching the private key will be chosen after
     *          the first keypair is generated.
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     */
    public CertAndKeyGen (String keyType, String sigAlg)
            throws NoSuchAlgorithmException
    {
        keyGen = KeyPairGenerator.getInstance(keyType);
        this.sigAlg = sigAlg;
        this.keyType = keyType;
    }

    /**
     * @see #CertAndKeyGen(String, String, String, PrivateKey, X500Name)
     */
    public CertAndKeyGen (String keyType, String sigAlg, String providerName)
            throws NoSuchAlgorithmException, NoSuchProviderException
    {
        this(keyType, sigAlg, providerName, null, null);
    }

    /**
     * Creates a CertAndKeyGen object for a particular key type,
     * signature algorithm, and provider. The newly generated cert will
     * be signed by the signer's private key when it is provided.
     *
     * @param keyType type of key, e.g. "RSA", "DSA", "X25519", "DH", etc.
     * @param sigAlg name of the signature algorithm, e.g. "SHA384WithRSA",
     *          "SHA256withDSA", etc. If set to null, a default
     *          algorithm matching the private key or signer's private
     *          key will be chosen after the first keypair is generated.
     * @param providerName name of the provider
     * @param signerPrivateKey (optional) signer's private key
     * @param signerSubjectName (optional) signer's subject name
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
     */
    public CertAndKeyGen(String keyType, String sigAlg, String providerName,
            PrivateKey signerPrivateKey, X500Name signerSubjectName)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        if (providerName == null) {
            keyGen = KeyPairGenerator.getInstance(keyType);
        } else {
            try {
                keyGen = KeyPairGenerator.getInstance(keyType, providerName);
            } catch (Exception e) {
                // try first available provider instead
                keyGen = KeyPairGenerator.getInstance(keyType);
            }
        }
        this.sigAlg = sigAlg;
        this.keyType = keyType;
        this.signerPrivateKey = signerPrivateKey;
        this.signerSubjectName = signerSubjectName;
        this.signerFlag = signerPrivateKey != null;
    }

    /**
     * Sets the source of random numbers used when generating keys.
     * If you do not provide one, a system default facility is used.
     * You may wish to provide your own source of random numbers
     * to get a reproducible sequence of keys and signatures, or
     * because you may be able to take advantage of strong sources
     * of randomness/entropy in your environment.
     */
    public void         setRandom (SecureRandom generator)
    {
        prng = generator;
    }

    public void generate(String name) {
        try {
            if (prng == null) {
                prng = new SecureRandom();
            }
            try {
                keyGen.initialize(new NamedParameterSpec(name), prng);
            } catch (InvalidAlgorithmParameterException e) {
                if (keyType.equalsIgnoreCase("EC")) {
                    // EC has another NamedParameterSpec
                    keyGen.initialize(new ECGenParameterSpec(name), prng);
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        generateInternal();
    }

    // want "public void generate (X509Certificate)" ... inherit DSA/D-H param

    public void generate(int keyBits) {
        if (keyBits != -1) {
            try {
                if (prng == null) {
                    prng = new SecureRandom();
                }
                keyGen.initialize(keyBits, prng);

            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        generateInternal();
    }

    /**
     * Generates a random public/private key pair.
     *
     * <P>Note that not all public key algorithms are currently
     * supported for use in X.509 certificates.  If the algorithm
     * you specified does not produce X.509 compatible keys, an
     * invalid key exception is thrown.
     *
     * @exception IllegalArgumentException if the environment does not
     *  provide X.509 public keys for this signature algorithm.
     */
    private void generateInternal() {
        KeyPair pair = keyGen.generateKeyPair();
        publicKey = pair.getPublic();
        privateKey = pair.getPrivate();

        // publicKey's format must be X.509 otherwise
        // the whole CertGen part of this class is broken.
        if (!"X.509".equalsIgnoreCase(publicKey.getFormat())) {
            throw new IllegalArgumentException("Public key format is "
                + publicKey.getFormat() + ", must be X.509");
        }

        if (sigAlg == null) {
            if (signerFlag) {
                sigAlg = SignatureUtil.getDefaultSigAlgForKey(signerPrivateKey);
                if (sigAlg == null) {
                    throw new IllegalArgumentException(
                            "Cannot derive signature algorithm from "
                                    + signerPrivateKey.getAlgorithm());
                }
            } else {
                sigAlg = SignatureUtil.getDefaultSigAlgForKey(privateKey);
                if (sigAlg == null) {
                    throw new IllegalArgumentException(
                            "Cannot derive signature algorithm from "
                                    + privateKey.getAlgorithm());
                }
            }
        }
    }

    /**
     * Returns the public key of the generated key pair if it is of type
     * <code>X509Key</code>, or null if the public key is of a different type.
     *
     * XXX Note: This behaviour is needed for backwards compatibility.
     * What this method really should return is the public key of the
     * generated key pair, regardless of whether or not it is an instance of
     * <code>X509Key</code>. Accordingly, the return type of this method
     * should be <code>PublicKey</code>.
     */
    public X509Key getPublicKey()
    {
        if (!(publicKey instanceof X509Key)) {
            return null;
        }
        return (X509Key)publicKey;
    }

    /**
     * Always returns the public key of the generated key pair. Used
     * by KeyTool only.
     *
     * The publicKey is not necessarily to be an instance of
     * X509Key in some JCA/JCE providers, for example SunPKCS11.
     */
    public PublicKey getPublicKeyAnyway() {
        return publicKey;
    }

    /**
     * Returns the private key of the generated key pair.
     *
     * <P><STRONG><em>Be extremely careful when handling private keys.
     * When private keys are not kept secret, they lose their ability
     * to securely authenticate specific entities ... that is a huge
     * security risk!</em></STRONG>
     */
    public PrivateKey getPrivateKey ()
    {
        return privateKey;
    }

    /**
     * Returns a self-signed X.509v3 certificate for the public key.
     * The certificate is immediately valid. No extensions.
     *
     * <P>Such certificates normally are used to identify a "Certificate
     * Authority" (CA).  Accordingly, they will not always be accepted by
     * other parties.  However, such certificates are also useful when
     * you are bootstrapping your security infrastructure, or deploying
     * system prototypes.
     *
     * @param myname X.500 name of the subject (who is also the issuer)
     * @param firstDate the issue time of the certificate
     * @param validity how long the certificate should be valid, in seconds
     * @exception CertificateException on certificate handling errors.
     * @exception InvalidKeyException on key handling errors.
     * @exception SignatureException on signature handling errors.
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
     */
    public X509Certificate getSelfCertificate (
            X500Name myname, Date firstDate, long validity)
    throws CertificateException, InvalidKeyException, SignatureException,
        NoSuchAlgorithmException, NoSuchProviderException
    {
        return getSelfCertificate(myname, firstDate, validity, null);
    }

    // Like above, plus a CertificateExtensions argument, which can be null.
    // Create a self-signed certificate, or a certificate that is signed by
    // a signer when the signer's private key is provided.
    public X509Certificate getSelfCertificate (X500Name myname, Date firstDate,
            long validity, CertificateExtensions ext)
    throws CertificateException, InvalidKeyException, SignatureException,
        NoSuchAlgorithmException, NoSuchProviderException
    {
        X509CertImpl    cert;
        Date            lastDate;

        try {
            lastDate = new Date ();
            lastDate.setTime (firstDate.getTime () + validity * 1000);

            CertificateValidity interval =
                                   new CertificateValidity(firstDate,lastDate);

            X509CertInfo info = new X509CertInfo();
            // Add all mandatory attributes
            info.set(X509CertInfo.VERSION,
                     new CertificateVersion(CertificateVersion.V3));
            if (prng == null) {
                prng = new SecureRandom();
            }
            info.set(X509CertInfo.SERIAL_NUMBER,
                    CertificateSerialNumber.newRandom64bit(prng));
            info.set(X509CertInfo.SUBJECT, myname);
            info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
            info.set(X509CertInfo.VALIDITY, interval);
            if (signerFlag) {
                // use signer's subject name to set the issuer name
                info.set(X509CertInfo.ISSUER, signerSubjectName);
            } else {
                info.set(X509CertInfo.ISSUER, myname);
            }
            if (ext != null) info.set(X509CertInfo.EXTENSIONS, ext);

            cert = new X509CertImpl(info);
            if (signerFlag) {
                // use signer's private key to sign
                cert.sign(signerPrivateKey, sigAlg);
            } else {
                cert.sign(privateKey, sigAlg);
            }

            return cert;

        } catch (IOException e) {
             throw new CertificateEncodingException("getSelfCert: " +
                                                    e.getMessage());
        }
    }

    // Keep the old method
    public X509Certificate getSelfCertificate (X500Name myname, long validity)
    throws CertificateException, InvalidKeyException, SignatureException,
        NoSuchAlgorithmException, NoSuchProviderException
    {
        return getSelfCertificate(myname, new Date(), validity);
    }

    private SecureRandom        prng;
    private String              keyType;
    private String              sigAlg;
    private KeyPairGenerator    keyGen;
    private PublicKey           publicKey;
    private PrivateKey          privateKey;
    private boolean             signerFlag;
    private PrivateKey          signerPrivateKey;
    private X500Name            signerSubjectName;
}
