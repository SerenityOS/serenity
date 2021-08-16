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

package sun.security.mscapi;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.math.BigInteger;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Locale;

import sun.security.rsa.RSAKeyFactory;
import sun.security.util.ECUtil;
import sun.security.util.KeyUtil;

/**
 * Signature implementation.
 *
 * Objects should be instantiated by calling Signature.getInstance() using the
 * following algorithm names:
 *
 *  . "NONEwithRSA"
 *  . "SHA1withRSA"
 *  . "SHA256withRSA"
 *  . "SHA384withRSA"
 *  . "SHA512withRSA"
 *  . "MD5withRSA"
 *  . "MD2withRSA"
 *  . "RSASSA-PSS"
 *  . "SHA1withECDSA"
 *  . "SHA224withECDSA"
 *  . "SHA256withECDSA"
 *  . "SHA384withECDSA"
 *  . "SHA512withECDSA"
 *
 * NOTE: RSA keys must be at least 512 bits long.
 *
 * NOTE: NONEwithRSA must be supplied with a pre-computed message digest.
 *       Only the following digest algorithms are supported: MD5, SHA-1,
 *       SHA-256, SHA-384, SHA-512 and a special-purpose digest
 *       algorithm which is a concatenation of SHA-1 and MD5 digests.
 *
 * @since   1.6
 * @author  Stanley Man-Kit Ho
 */
abstract class CSignature extends SignatureSpi {
    // private key algorithm name
    protected String keyAlgorithm;

    // message digest implementation we use
    protected MessageDigest messageDigest;

    // message digest name
    protected String messageDigestAlgorithm;

    // flag indicating whether the digest has been reset
    protected boolean needsReset;

    // the signing key
    protected CPrivateKey privateKey = null;

    // the verification key
    protected CPublicKey publicKey = null;

    /**
     * Constructs a new CSignature. Used by subclasses.
     */
    CSignature(String keyName, String digestName) {

        this.keyAlgorithm = keyName;
        if (digestName != null) {
            try {
                messageDigest = MessageDigest.getInstance(digestName);
                // Get the digest's canonical name
                messageDigestAlgorithm = messageDigest.getAlgorithm();
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException(e);
            }
        } else {
            messageDigest = null;
            messageDigestAlgorithm = null;
        }
        needsReset = false;
    }

    static class RSA extends CSignature {

        public RSA(String digestAlgorithm) {
            super("RSA", digestAlgorithm);
        }

        // initialize for signing. See JCA doc
        @Override
        protected void engineInitSign(PrivateKey key) throws InvalidKeyException {
            if (key == null) {
                throw new InvalidKeyException("Key cannot be null");
            }
            if ((key instanceof CPrivateKey) == false
                    || !key.getAlgorithm().equalsIgnoreCase("RSA")) {
                throw new InvalidKeyException("Key type not supported: "
                        + key.getClass() + " " + key.getAlgorithm());
            }
            privateKey = (CPrivateKey) key;

            // Check against the local and global values to make sure
            // the sizes are ok.  Round up to nearest byte.
            RSAKeyFactory.checkKeyLengths(((privateKey.length() + 7) & ~7),
                    null, CKeyPairGenerator.RSA.KEY_SIZE_MIN,
                    CKeyPairGenerator.RSA.KEY_SIZE_MAX);

            this.publicKey = null;
            resetDigest();
        }

        // initialize for signing. See JCA doc
        @Override
        protected void engineInitVerify(PublicKey key) throws InvalidKeyException {
            if (key == null) {
                throw new InvalidKeyException("Key cannot be null");
            }
            // This signature accepts only RSAPublicKey
            if ((key instanceof RSAPublicKey) == false) {
                throw new InvalidKeyException("Key type not supported: "
                        + key.getClass());
            }


            if ((key instanceof CPublicKey) == false) {

                // convert key to MSCAPI format
                java.security.interfaces.RSAPublicKey rsaKey =
                        (java.security.interfaces.RSAPublicKey) key;

                BigInteger modulus = rsaKey.getModulus();
                BigInteger exponent =  rsaKey.getPublicExponent();

                // Check against the local and global values to make sure
                // the sizes are ok.  Round up to the nearest byte.
                RSAKeyFactory.checkKeyLengths(((modulus.bitLength() + 7) & ~7),
                        exponent, -1, CKeyPairGenerator.RSA.KEY_SIZE_MAX);

                byte[] modulusBytes = modulus.toByteArray();
                byte[] exponentBytes = exponent.toByteArray();

                // Adjust key length due to sign bit
                int keyBitLength = (modulusBytes[0] == 0)
                        ? (modulusBytes.length - 1) * 8
                        : modulusBytes.length * 8;

                byte[] keyBlob = generatePublicKeyBlob(
                        keyBitLength, modulusBytes, exponentBytes);

                try {
                    publicKey = importPublicKey("RSA", keyBlob, keyBitLength);

                } catch (KeyStoreException e) {
                    throw new InvalidKeyException(e);
                }

            } else {
                publicKey = (CPublicKey) key;
            }

            this.privateKey = null;
            resetDigest();
        }

        /**
         * Returns the signature bytes of all the data
         * updated so far.
         * The format of the signature depends on the underlying
         * signature scheme.
         *
         * @return the signature bytes of the signing operation's result.
         *
         * @exception SignatureException if the engine is not
         * initialized properly or if this signature algorithm is unable to
         * process the input data provided.
         */
        @Override
        protected byte[] engineSign() throws SignatureException {

            byte[] hash = getDigestValue();

            if (privateKey.getHCryptKey() == 0) {
                return signCngHash(1, hash, hash.length,
                        0,
                        this instanceof NONEwithRSA ? null : messageDigestAlgorithm,
                        privateKey.getHCryptProvider(), 0);
            } else {
                // Omit the hash OID when generating a NONEwithRSA signature
                boolean noHashOID = this instanceof NONEwithRSA;
                // Sign hash using MS Crypto APIs
                byte[] result = signHash(noHashOID, hash, hash.length,
                        messageDigestAlgorithm, privateKey.getHCryptProvider(),
                        privateKey.getHCryptKey());

                // Convert signature array from little endian to big endian
                return convertEndianArray(result);
            }
        }

        /**
         * Verifies the passed-in signature.
         *
         * @param sigBytes the signature bytes to be verified.
         *
         * @return true if the signature was verified, false if not.
         *
         * @exception SignatureException if the engine is not
         * initialized properly, the passed-in signature is improperly
         * encoded or of the wrong type, if this signature algorithm is unable to
         * process the input data provided, etc.
         */
        @Override
        protected boolean engineVerify(byte[] sigBytes)
                throws SignatureException {
            byte[] hash = getDigestValue();

            if (publicKey.getHCryptKey() == 0) {
                return verifyCngSignedHash(
                        1, hash, hash.length,
                        sigBytes, sigBytes.length,
                        0,
                        messageDigestAlgorithm,
                        publicKey.getHCryptProvider(),
                        0);
            } else {
                return verifySignedHash(hash, hash.length,
                        messageDigestAlgorithm, convertEndianArray(sigBytes),
                        sigBytes.length, publicKey.getHCryptProvider(),
                        publicKey.getHCryptKey());
            }
        }

        /**
         * Generates a public-key BLOB from a key's components.
         */
        // used by CRSACipher
        static native byte[] generatePublicKeyBlob(
                int keyBitLength, byte[] modulus, byte[] publicExponent)
                throws InvalidKeyException;

    }

    // Nested class for NONEwithRSA signatures
    public static final class NONEwithRSA extends RSA {

        // the longest supported digest is 512 bits (SHA-512)
        private static final int RAW_RSA_MAX = 64;

        private final byte[] precomputedDigest;
        private int offset = 0;

        public NONEwithRSA() {
            super(null);
            precomputedDigest = new byte[RAW_RSA_MAX];
        }

        // Stores the precomputed message digest value.
        @Override
        protected void engineUpdate(byte b) throws SignatureException {
            if (offset >= precomputedDigest.length) {
                offset = RAW_RSA_MAX + 1;
                return;
            }
            precomputedDigest[offset++] = b;
        }

        // Stores the precomputed message digest value.
        @Override
        protected void engineUpdate(byte[] b, int off, int len)
                throws SignatureException {
            if (len > (precomputedDigest.length - offset)) {
                offset = RAW_RSA_MAX + 1;
                return;
            }
            System.arraycopy(b, off, precomputedDigest, offset, len);
            offset += len;
        }

        // Stores the precomputed message digest value.
        @Override
        protected void engineUpdate(ByteBuffer byteBuffer) {
            int len = byteBuffer.remaining();
            if (len <= 0) {
                return;
            }
            if (len > (precomputedDigest.length - offset)) {
                offset = RAW_RSA_MAX + 1;
                return;
            }
            byteBuffer.get(precomputedDigest, offset, len);
            offset += len;
        }

        @Override
        protected void resetDigest(){
            offset = 0;
        }

        // Returns the precomputed message digest value.
        @Override
        protected byte[] getDigestValue() throws SignatureException {
            if (offset > RAW_RSA_MAX) {
                throw new SignatureException("Message digest is too long");
            }

            // Determine the digest algorithm from the digest length
            if (offset == 20) {
                setDigestName("SHA1");
            } else if (offset == 36) {
                setDigestName("SHA1+MD5");
            } else if (offset == 32) {
                setDigestName("SHA-256");
            } else if (offset == 48) {
                setDigestName("SHA-384");
            } else if (offset == 64) {
                setDigestName("SHA-512");
            } else if (offset == 16) {
                setDigestName("MD5");
            } else {
                throw new SignatureException(
                    "Message digest length is not supported");
            }

            byte[] result = new byte[offset];
            System.arraycopy(precomputedDigest, 0, result, 0, offset);
            offset = 0;

            return result;
        }
    }

    public static final class SHA1withRSA extends RSA {
        public SHA1withRSA() {
            super("SHA1");
        }
    }

    public static final class SHA256withRSA extends RSA {
        public SHA256withRSA() {
            super("SHA-256");
        }
    }

    public static final class SHA384withRSA extends RSA {
        public SHA384withRSA() {
            super("SHA-384");
        }
    }

    public static final class SHA512withRSA extends RSA {
        public SHA512withRSA() {
            super("SHA-512");
        }
    }

    public static final class MD5withRSA extends RSA {
        public MD5withRSA() {
            super("MD5");
        }
    }

    public static final class MD2withRSA extends RSA {
        public MD2withRSA() {
            super("MD2");
        }
    }

    public static final class SHA1withECDSA extends ECDSA {
        public SHA1withECDSA() {
            super("SHA-1");
        }
    }

    public static final class SHA224withECDSA extends ECDSA {
        public SHA224withECDSA() {
            super("SHA-224");
        }
    }

    public static final class SHA256withECDSA extends ECDSA {
        public SHA256withECDSA() {
            super("SHA-256");
        }
    }

    public static final class SHA384withECDSA extends ECDSA {
        public SHA384withECDSA() {
            super("SHA-384");
        }
    }

    public static final class SHA512withECDSA extends ECDSA {
        public SHA512withECDSA() {
            super("SHA-512");
        }
    }

    static class ECDSA extends CSignature {

        public ECDSA(String messageDigestAlgorithm) {
            super("EC", messageDigestAlgorithm);
        }

        // initialize for signing. See JCA doc
        @Override
        protected void engineInitSign(PrivateKey key) throws InvalidKeyException {
            if (key == null) {
                throw new InvalidKeyException("Key cannot be null");
            }
            if ((key instanceof CPrivateKey) == false
                    || !key.getAlgorithm().equalsIgnoreCase("EC")) {
                throw new InvalidKeyException("Key type not supported: "
                        + key.getClass() + " " + key.getAlgorithm());
            }
            privateKey = (CPrivateKey) key;

            this.publicKey = null;
            resetDigest();
        }

        // initialize for signing. See JCA doc
        @Override
        protected void engineInitVerify(PublicKey key) throws InvalidKeyException {
            if (key == null) {
                throw new InvalidKeyException("Key cannot be null");
            }
            // This signature accepts only ECPublicKey
            if ((key instanceof ECPublicKey) == false) {
                throw new InvalidKeyException("Key type not supported: "
                        + key.getClass());
            }

            if ((key instanceof CPublicKey) == false) {
                try {
                    publicKey = importECPublicKey("EC",
                            CKey.generateECBlob(key),
                            KeyUtil.getKeySize(key));
                } catch (KeyStoreException e) {
                    throw new InvalidKeyException(e);
                }
            } else {
                publicKey = (CPublicKey) key;
            }

            this.privateKey = null;
            resetDigest();
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            byte[] hash = getDigestValue();
            byte[] raw = signCngHash(0, hash, hash.length,
                    0,
                    null,
                    privateKey.getHCryptProvider(), 0);
            return ECUtil.encodeSignature(raw);
        }

        @Override
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            byte[] hash = getDigestValue();
            sigBytes = ECUtil.decodeSignature(sigBytes);
            return verifyCngSignedHash(
                    0,
                    hash, hash.length,
                    sigBytes, sigBytes.length,
                    0,
                    null,
                    publicKey.getHCryptProvider(),
                    0
            );
        }
    }

    public static final class PSS extends RSA {

        private PSSParameterSpec pssParams = null;

        // Workaround: Cannot import raw public key to CNG. This signature
        // will be used for verification if key is not from MSCAPI.
        private Signature fallbackSignature;

        public PSS() {
            super(null);
        }

        @Override
        protected void engineInitSign(PrivateKey key) throws InvalidKeyException {
            super.engineInitSign(key);
            fallbackSignature = null;
        }

        @Override
        protected void engineInitVerify(PublicKey key) throws InvalidKeyException {
            if (key == null) {
                throw new InvalidKeyException("Key cannot be null");
            }
            // This signature accepts only RSAPublicKey
            if ((key instanceof java.security.interfaces.RSAPublicKey) == false) {
                throw new InvalidKeyException("Key type not supported: "
                        + key.getClass());
            }

            this.privateKey = null;

            if (key instanceof CPublicKey) {
                fallbackSignature = null;
                publicKey = (CPublicKey) key;
            } else {
                if (fallbackSignature == null) {
                    try {
                        fallbackSignature = Signature.getInstance(
                                "RSASSA-PSS", "SunRsaSign");
                    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                        throw new InvalidKeyException("Invalid key", e);
                    }
                }
                fallbackSignature.initVerify(key);
                if (pssParams != null) {
                    try {
                        fallbackSignature.setParameter(pssParams);
                    } catch (InvalidAlgorithmParameterException e) {
                        throw new InvalidKeyException("Invalid params", e);
                    }
                }
                publicKey = null;
            }
            resetDigest();
        }

        @Override
        protected void engineUpdate(byte b) throws SignatureException {
            ensureInit();
            if (fallbackSignature != null) {
                fallbackSignature.update(b);
            } else {
                messageDigest.update(b);
            }
            needsReset = true;
        }

        @Override
        protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
            ensureInit();
            if (fallbackSignature != null) {
                fallbackSignature.update(b, off, len);
            } else {
                messageDigest.update(b, off, len);
            }
            needsReset = true;
        }

        @Override
        protected void engineUpdate(ByteBuffer input) {
            try {
                ensureInit();
            } catch (SignatureException se) {
                // workaround for API bug
                throw new RuntimeException(se.getMessage());
            }
            if (fallbackSignature != null) {
                try {
                    fallbackSignature.update(input);
                } catch (SignatureException se) {
                    // workaround for API bug
                    throw new RuntimeException(se.getMessage());
                }
            } else {
                messageDigest.update(input);
            }
            needsReset = true;
        }

        @Override
        protected byte[] engineSign() throws SignatureException {
            ensureInit();
            byte[] hash = getDigestValue();
            return signCngHash(2, hash, hash.length,
                    pssParams.getSaltLength(),
                    ((MGF1ParameterSpec)
                            pssParams.getMGFParameters()).getDigestAlgorithm(),
                    privateKey.getHCryptProvider(), privateKey.getHCryptKey());
        }

        @Override
        protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
            ensureInit();
            if (fallbackSignature != null) {
                needsReset = false;
                return fallbackSignature.verify(sigBytes);
            } else {
                byte[] hash = getDigestValue();
                return verifyCngSignedHash(
                        2, hash, hash.length,
                        sigBytes, sigBytes.length,
                        pssParams.getSaltLength(),
                        ((MGF1ParameterSpec)
                                pssParams.getMGFParameters()).getDigestAlgorithm(),
                        publicKey.getHCryptProvider(),
                        publicKey.getHCryptKey()
                );
            }
        }

        @Override
        protected void engineSetParameter(AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            if (needsReset) {
                throw new ProviderException
                        ("Cannot set parameters during operations");
            }
            this.pssParams = validateSigParams(params);
            if (fallbackSignature != null) {
                fallbackSignature.setParameter(params);
            }
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            AlgorithmParameters ap = null;
            if (this.pssParams != null) {
                try {
                    ap = AlgorithmParameters.getInstance("RSASSA-PSS");
                    ap.init(this.pssParams);
                } catch (GeneralSecurityException gse) {
                    throw new ProviderException(gse.getMessage());
                }
            }
            return ap;
        }

        private void ensureInit() throws SignatureException {
            if (this.privateKey == null && this.publicKey == null
                    && fallbackSignature == null) {
                throw new SignatureException("Missing key");
            }
            if (this.pssParams == null) {
                // Parameters are required for signature verification
                throw new SignatureException
                        ("Parameters required for RSASSA-PSS signatures");
            }
            if (fallbackSignature == null && messageDigest == null) {
                // This could happen if initVerify(softKey), setParameter(),
                // and initSign() were called. No messageDigest. Create it.
                try {
                    messageDigest = MessageDigest
                            .getInstance(pssParams.getDigestAlgorithm());
                } catch (NoSuchAlgorithmException e) {
                    throw new SignatureException(e);
                }
            }
        }

        /**
         * Validate the specified Signature PSS parameters.
         */
        private PSSParameterSpec validateSigParams(AlgorithmParameterSpec p)
                throws InvalidAlgorithmParameterException {

            if (p == null) {
                throw new InvalidAlgorithmParameterException
                        ("Parameters cannot be null");
            }

            if (!(p instanceof PSSParameterSpec)) {
                throw new InvalidAlgorithmParameterException
                        ("parameters must be type PSSParameterSpec");
            }

            // no need to validate again if same as current signature parameters
            PSSParameterSpec params = (PSSParameterSpec) p;
            if (params == this.pssParams) return params;

            // now sanity check the parameter values
            if (!(params.getMGFAlgorithm().equalsIgnoreCase("MGF1"))) {
                throw new InvalidAlgorithmParameterException("Only supports MGF1");

            }

            if (params.getTrailerField() != PSSParameterSpec.TRAILER_FIELD_BC) {
                throw new InvalidAlgorithmParameterException
                        ("Only supports TrailerFieldBC(1)");
            }

            AlgorithmParameterSpec algSpec = params.getMGFParameters();
            if (!(algSpec instanceof MGF1ParameterSpec)) {
                throw new InvalidAlgorithmParameterException
                        ("Only support MGF1ParameterSpec");
            }

            MGF1ParameterSpec mgfSpec = (MGF1ParameterSpec)algSpec;

            String msgHashAlg = params.getDigestAlgorithm()
                    .toLowerCase(Locale.ROOT).replaceAll("-", "");
            if (msgHashAlg.equals("sha")) {
                msgHashAlg = "sha1";
            }
            String mgf1HashAlg = mgfSpec.getDigestAlgorithm()
                    .toLowerCase(Locale.ROOT).replaceAll("-", "");
            if (mgf1HashAlg.equals("sha")) {
                mgf1HashAlg = "sha1";
            }

            if (!mgf1HashAlg.equals(msgHashAlg)) {
                throw new InvalidAlgorithmParameterException
                        ("MGF1 hash must be the same as message hash");
            }

            return params;
        }
    }

    /**
     * Sign hash using CNG API with HCRYPTKEY.
     * @param type 0 no padding, 1, pkcs1, 2, pss
     */
    native static byte[] signCngHash(
            int type, byte[] hash,
            int hashSize, int saltLength, String hashAlgorithm,
            long hCryptProv, long nCryptKey)
            throws SignatureException;

    /**
     * Verify a signed hash using CNG API with HCRYPTKEY.
     * @param type 0 no padding, 1, pkcs1, 2, pss
     */
    private native static boolean verifyCngSignedHash(
            int type, byte[] hash, int hashSize,
            byte[] signature, int signatureSize,
            int saltLength, String hashAlgorithm,
            long hCryptProv, long hKey) throws SignatureException;

    /**
     * Resets the message digest if needed.
     */
    protected void resetDigest() {
        if (needsReset) {
            if (messageDigest != null) {
                messageDigest.reset();
            }
            needsReset = false;
        }
    }

    protected byte[] getDigestValue() throws SignatureException {
        needsReset = false;
        return messageDigest.digest();
    }

    protected void setDigestName(String name) {
        messageDigestAlgorithm = name;
    }

    /**
     * Updates the data to be signed or verified
     * using the specified byte.
     *
     * @param b the byte to use for the update.
     *
     * @exception SignatureException if the engine is not initialized
     * properly.
     */
    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        messageDigest.update(b);
        needsReset = true;
    }

    /**
     * Updates the data to be signed or verified, using the
     * specified array of bytes, starting at the specified offset.
     *
     * @param b the array of bytes
     * @param off the offset to start from in the array of bytes
     * @param len the number of bytes to use, starting at offset
     *
     * @exception SignatureException if the engine is not initialized
     * properly
     */
    @Override
    protected void engineUpdate(byte[] b, int off, int len)
            throws SignatureException {
        messageDigest.update(b, off, len);
        needsReset = true;
    }

    /**
     * Updates the data to be signed or verified, using the
     * specified ByteBuffer.
     *
     * @param input the ByteBuffer
     */
    @Override
    protected void engineUpdate(ByteBuffer input) {
        messageDigest.update(input);
        needsReset = true;
    }

    /**
     * Convert array from big endian to little endian, or vice versa.
     */
    private static byte[] convertEndianArray(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0)
            return byteArray;

        byte [] retval = new byte[byteArray.length];

        // make it big endian
        for (int i=0;i < byteArray.length;i++)
            retval[i] = byteArray[byteArray.length - i - 1];

        return retval;
    }

    /**
     * Sign hash using Microsoft Crypto API with HCRYPTKEY.
     * The returned data is in little-endian.
     */
    private native static byte[] signHash(boolean noHashOID, byte[] hash,
        int hashSize, String hashAlgorithm, long hCryptProv, long hCryptKey)
            throws SignatureException;

    /**
     * Verify a signed hash using Microsoft Crypto API with HCRYPTKEY.
     */
    private native static boolean verifySignedHash(byte[] hash, int hashSize,
        String hashAlgorithm, byte[] signature, int signatureSize,
        long hCryptProv, long hCryptKey) throws SignatureException;

    /**
     * Sets the specified algorithm parameter to the specified
     * value. This method supplies a general-purpose mechanism through
     * which it is possible to set the various parameters of this object.
     * A parameter may be any settable parameter for the algorithm, such as
     * a parameter size, or a source of random bits for signature generation
     * (if appropriate), or an indication of whether or not to perform
     * a specific but optional computation. A uniform algorithm-specific
     * naming scheme for each parameter is desirable but left unspecified
     * at this time.
     *
     * @param param the string identifier of the parameter.
     *
     * @param value the parameter value.
     *
     * @exception InvalidParameterException if <code>param</code> is an
     * invalid parameter for this signature algorithm engine,
     * the parameter is already set
     * and cannot be set again, a security exception occurs, and so on.
     *
     * @deprecated Replaced by {@link
     * #engineSetParameter(java.security.spec.AlgorithmParameterSpec)
     * engineSetParameter}.
     */
    @Override
    @Deprecated
    protected void engineSetParameter(String param, Object value)
            throws InvalidParameterException {
        throw new InvalidParameterException("Parameter not supported");
    }

    /**
     * Sets this signature engine with the specified algorithm parameter.
     *
     * @param params the parameters
     *
     * @exception InvalidAlgorithmParameterException if the given
     * parameter is invalid
     */
    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException("No parameter accepted");
        }
    }

    /**
     * Gets the value of the specified algorithm parameter.
     * This method supplies a general-purpose mechanism through which it
     * is possible to get the various parameters of this object. A parameter
     * may be any settable parameter for the algorithm, such as a parameter
     * size, or  a source of random bits for signature generation (if
     * appropriate), or an indication of whether or not to perform a
     * specific but optional computation. A uniform algorithm-specific
     * naming scheme for each parameter is desirable but left unspecified
     * at this time.
     *
     * @param param the string name of the parameter.
     *
     * @return the object that represents the parameter value, or null if
     * there is none.
     *
     * @exception InvalidParameterException if <code>param</code> is an
     * invalid parameter for this engine, or another exception occurs while
     * trying to get this parameter.
     *
     * @deprecated
     */
    @Override
    @Deprecated
    protected Object engineGetParameter(String param)
           throws InvalidParameterException {
        throw new InvalidParameterException("Parameter not supported");
    }

    /**
     * Gets the algorithm parameter from this signature engine.
     *
     * @return the parameter, or null if no parameter is used.
     */
    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    /**
     * Imports a public-key BLOB.
     */
    // used by CRSACipher
    static native CPublicKey importPublicKey(
            String alg, byte[] keyBlob, int keySize) throws KeyStoreException;

    static native CPublicKey importECPublicKey(
            String alg, byte[] keyBlob, int keySize) throws KeyStoreException;
}
