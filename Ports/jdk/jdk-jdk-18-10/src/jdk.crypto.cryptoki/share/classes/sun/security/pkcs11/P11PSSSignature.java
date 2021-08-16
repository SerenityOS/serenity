/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import sun.nio.ch.DirectBuffer;

import java.util.Hashtable;
import java.util.Arrays;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.interfaces.*;
import sun.security.pkcs11.wrapper.*;
import sun.security.util.KnownOIDs;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.*;

/**
 * RSASSA-PSS Signature implementation class. This class currently supports the
 * following algorithms:
 *
 * . RSA-PSS:
 *   . RSASSA-PSS
 *   . SHA1withRSASSA-PSS
 *   . SHA224withRSASSA-PSS
 *   . SHA256withRSASSA-PSS
 *   . SHA384withRSASSA-PSS
 *   . SHA512withRSASSA-PSS
 *   . SHA3-224withRSASSA-PSS
 *   . SHA3-256withRSASSA-PSS
 *   . SHA3-384withRSASSA-PSS
 *   . SHA3-512withRSASSA-PSS
 *
 * Note that the underlying PKCS#11 token may support complete signature
 * algorithm (e.g. CKM_<md>_RSA_PKCS_PSS), or it may just
 * implement the signature algorithm without hashing (i.e. CKM_RSA_PKCS_PSS).
 * This class uses what is available and adds whatever extra processing
 * is needed.
 *
 * @since   13
 */
final class P11PSSSignature extends SignatureSpi {

    private static final boolean DEBUG = false;

    // mappings of digest algorithms and their output length in bytes
    private static final Hashtable<String, Integer> DIGEST_LENGTHS =
        new Hashtable<String, Integer>();

    static {
        DIGEST_LENGTHS.put("SHA-1", 20);
        DIGEST_LENGTHS.put("SHA-224", 28);
        DIGEST_LENGTHS.put("SHA-256", 32);
        DIGEST_LENGTHS.put("SHA-384", 48);
        DIGEST_LENGTHS.put("SHA-512", 64);
        DIGEST_LENGTHS.put("SHA-512/224", 28);
        DIGEST_LENGTHS.put("SHA-512/256", 32);
        DIGEST_LENGTHS.put("SHA3-224", 28);
        DIGEST_LENGTHS.put("SHA3-256", 32);
        DIGEST_LENGTHS.put("SHA3-384", 48);
        DIGEST_LENGTHS.put("SHA3-512", 64);
    }

    // utility method for looking up the std digest algorithms
    private static String toStdName(String givenDigestAlg) {
        if (givenDigestAlg == null) return null;

        KnownOIDs given2 = KnownOIDs.findMatch(givenDigestAlg);
        if (given2 == null) {
            return givenDigestAlg;
        } else {
            return given2.stdName();
        }
    }

    // utility method for comparing digest algorithms
    // NOTE that first argument is assumed to be standard digest name
    private static boolean isDigestEqual(String stdAlg, String givenAlg) {
        if (stdAlg == null || givenAlg == null) return false;

        givenAlg = toStdName(givenAlg);
        return stdAlg.equalsIgnoreCase(givenAlg);
    }

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // name of the key algorithm, currently just RSA
    private static final String KEY_ALGO = "RSA";

    // mechanism id
    private final CK_MECHANISM mechanism;

    // type, one of T_* below
    private final int type;

    // key instance used, if init*() was called
    private P11Key p11Key = null;

    // PSS parameters and the flag controlling its access
    private PSSParameterSpec sigParams = null;
    private boolean isActive = false;

    // message digest alg, if implied by the algorithm name
    private final String mdAlg;

    // message digest, if we do the digesting ourselves
    private MessageDigest md = null;

    // associated session, if any
    private Session session;

    // mode, one of M_* below
    private int mode;

    // flag indicating whether an operation is initialized
    private boolean initialized = false;

    // buffer, for update(byte)
    private final byte[] buffer = new byte[1];

    // total number of bytes processed in current operation
    private int bytesProcessed = 0;

    // constant for signing mode
    private static final int M_SIGN   = 1;
    // constant for verification mode
    private static final int M_VERIFY = 2;

    // constant for type digesting, we do the hashing ourselves
    private static final int T_DIGEST = 1;
    // constant for type update, token does everything
    private static final int T_UPDATE = 2;

    P11PSSSignature(Token token, String algorithm, long mechId)
            throws NoSuchAlgorithmException, PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = new CK_MECHANISM(mechId);
        int idx = algorithm.indexOf("with");
        // convert to stdName
        this.mdAlg = (idx == -1?
                null : toStdName(algorithm.substring(0, idx)));

        switch ((int)mechId) {
        case (int)CKM_SHA1_RSA_PKCS_PSS:
        case (int)CKM_SHA224_RSA_PKCS_PSS:
        case (int)CKM_SHA256_RSA_PKCS_PSS:
        case (int)CKM_SHA384_RSA_PKCS_PSS:
        case (int)CKM_SHA512_RSA_PKCS_PSS:
        case (int)CKM_SHA3_224_RSA_PKCS_PSS:
        case (int)CKM_SHA3_256_RSA_PKCS_PSS:
        case (int)CKM_SHA3_384_RSA_PKCS_PSS:
        case (int)CKM_SHA3_512_RSA_PKCS_PSS:
            type = T_UPDATE;
            this.md = null;
            break;
        case (int)CKM_RSA_PKCS_PSS:
            // check if the digest algo is supported by underlying PKCS11 lib
            if (this.mdAlg != null && token.getMechanismInfo
                    (Functions.getHashMechId(this.mdAlg)) == null) {
                throw new NoSuchAlgorithmException("Unsupported algorithm: " +
                        algorithm);
            }
            this.md = (this.mdAlg == null? null :
                    MessageDigest.getInstance(this.mdAlg));
            type = T_DIGEST;
            break;
        default:
            throw new ProviderException("Unsupported mechanism: " + mechId);
        }
    }

    private static PSSParameterSpec genDefaultParams(String digestAlg,
            P11Key key) throws SignatureException {
        int mdLen;
        try {
            mdLen = DIGEST_LENGTHS.get(digestAlg);
        } catch (NullPointerException npe) {
            throw new SignatureException("Unsupported digest: " +
                    digestAlg);
        }
        int saltLen = Integer.min(mdLen, (key.length() >> 3) - mdLen -2);
        return new PSSParameterSpec(digestAlg,
                "MGF1", new MGF1ParameterSpec(digestAlg),
                saltLen, PSSParameterSpec.TRAILER_FIELD_BC);
    }

    private void ensureInitialized() throws SignatureException {
        token.ensureValid();

        if (this.p11Key == null) {
            throw new SignatureException("Missing key");
        }
        if (this.sigParams == null) {
            if (this.mdAlg == null) {
                // PSS Parameters are required for signature verification
                throw new SignatureException
                    ("Parameters required for RSASSA-PSS signature");
            }
            // generate default params for both sign and verify?
            this.sigParams = genDefaultParams(this.mdAlg, this.p11Key);
            this.mechanism.setParameter(new CK_RSA_PKCS_PSS_PARAMS(
                    this.mdAlg, "MGF1", this.mdAlg, sigParams.getSaltLength()));
        }

        if (initialized == false) {
            try {
                initialize();
            } catch (ProviderException pe) {
                throw new SignatureException(pe);
            }
        }
    }

    // reset the states to the pre-initialized values
    private void reset(boolean doCancel) {
        if (!initialized) {
            return;
        }
        initialized = false;

        try {
            if (session == null) {
                return;
            }

            if (doCancel && token.explicitCancel) {
                cancelOperation();
            }
        } finally {
            p11Key.releaseKeyID();
            mechanism.freeHandle();
            session = token.releaseSession(session);
            isActive = false;
        }
    }

    private void cancelOperation() {
        token.ensureValid();
        if (DEBUG) System.out.print("Cancelling operation");

        // cancel operation by finishing it; avoid killSession as some
        // hardware vendors may require re-login
        try {
            if (mode == M_SIGN) {
                if (type == T_UPDATE) {
                    if (DEBUG) System.out.println(" by C_SignFinal");
                    token.p11.C_SignFinal(session.id(), 0);
                } else {
                    byte[] digest =
                        (md == null? new byte[0] : md.digest());
                    if (DEBUG) System.out.println(" by C_Sign");
                    token.p11.C_Sign(session.id(), digest);
                }
            } else { // M_VERIFY
                byte[] signature =
                    new byte[(p11Key.length() + 7) >> 3];
                if (type == T_UPDATE) {
                    if (DEBUG) System.out.println(" by C_VerifyFinal");
                    token.p11.C_VerifyFinal(session.id(), signature);
                } else {
                    byte[] digest =
                        (md == null? new byte[0] : md.digest());
                    if (DEBUG) System.out.println(" by C_Verify");
                    token.p11.C_Verify(session.id(), digest, signature);
                }
            }
        } catch (PKCS11Exception e) {
            if (e.getErrorCode() == CKR_OPERATION_NOT_INITIALIZED) {
                // Cancel Operation may be invoked after an error on a PKCS#11
                // call. If the operation inside the token was already cancelled,
                // do not fail here. This is part of a defensive mechanism for
                // PKCS#11 libraries that do not strictly follow the standard.
                return;
            }
            if (mode == M_SIGN) {
                throw new ProviderException("cancel failed", e);
            }
            // ignore failure for verification
        }
    }

    // assumes current state is initialized == false
    private void initialize() throws ProviderException {
        if (DEBUG) System.out.println("Initializing");

        if (p11Key == null) {
            throw new ProviderException(
                    "No Key found, call initSign/initVerify first");
        }

        long keyID = p11Key.getKeyID();
        try {
            if (session == null) {
                session = token.getOpSession();
            }
            if (mode == M_SIGN) {
                token.p11.C_SignInit(session.id(), mechanism, keyID);
            } else {
                token.p11.C_VerifyInit(session.id(), mechanism, keyID);
            }
        } catch (PKCS11Exception e) {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
            throw new ProviderException("Initialization failed", e);
        }
        if (bytesProcessed != 0) {
            bytesProcessed = 0;
            if (md != null) {
                md.reset();
            }
        }
        initialized = true;
        isActive = false;
        if (DEBUG) System.out.println("Initialized");
    }

    private void checkKeySize(Key key) throws InvalidKeyException {
        if (DEBUG) System.out.print("Checking Key");

        if (!key.getAlgorithm().equals(KEY_ALGO)) {
            throw new InvalidKeyException("Only " + KEY_ALGO +
                " keys are supported");
        }

        CK_MECHANISM_INFO mechInfo = null;
        try {
            mechInfo = token.getMechanismInfo(mechanism.mechanism);
        } catch (PKCS11Exception e) {
            // should not happen, ignore for now
            if (DEBUG) {
                System.out.println("Unexpected exception");
                e.printStackTrace();
            }
        }

        int keySize = 0; // in bytes
        if (mechInfo != null) {
            if (key instanceof P11Key) {
                keySize = (((P11Key) key).length() + 7) >> 3;
            } else if (key instanceof RSAKey) {
                keySize = ((RSAKey) key).getModulus().bitLength() >> 3;
            } else {
                throw new InvalidKeyException("Unrecognized key type " + key);
            }
            // check against available native info which are in bits
            if ((mechInfo.iMinKeySize != 0) &&
                    (keySize < (mechInfo.iMinKeySize >> 3))) {
                throw new InvalidKeyException(KEY_ALGO +
                    " key must be at least " + mechInfo.iMinKeySize + " bits");
            }
            if ((mechInfo.iMaxKeySize != Integer.MAX_VALUE) &&
                    (keySize > (mechInfo.iMaxKeySize >> 3))) {
                throw new InvalidKeyException(KEY_ALGO +
                    " key must be at most " + mechInfo.iMaxKeySize + " bits");
            }
        }
        if (this.sigParams != null) {
            String digestAlg = this.sigParams.getDigestAlgorithm();
            int sLen = this.sigParams.getSaltLength();

            int hLen = DIGEST_LENGTHS.get(toStdName(digestAlg)).intValue();
            int minKeyLen = Math.addExact(Math.addExact(sLen, hLen), 2);

            if (keySize < minKeyLen) {
                throw new InvalidKeyException
                    ("Key is too short for current params, need min " + minKeyLen);
            }
        }
    }

    private void setSigParams(AlgorithmParameterSpec p)
            throws InvalidAlgorithmParameterException {
        if (p == null) {
            throw new InvalidAlgorithmParameterException("PSS Parameter required");
        }
        if (!(p instanceof PSSParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("Only PSSParameterSpec is supported");
        }
        // no need to validate again if same as current signature parameters
        PSSParameterSpec params = (PSSParameterSpec) p;
        if (params == this.sigParams) return;

        String digestAlgorithm = params.getDigestAlgorithm();
        if (this.mdAlg != null && !isDigestEqual(this.mdAlg, digestAlgorithm)) {
            throw new InvalidAlgorithmParameterException
                    ("Digest algorithm in Signature parameters must be " +
                     this.mdAlg);
        }

        try {
            if (token.getMechanismInfo(Functions.getHashMechId
                    (digestAlgorithm)) == null) {
                throw new InvalidAlgorithmParameterException
                        ("Unsupported digest algorithm: " + digestAlgorithm);
            }
        } catch (PKCS11Exception pe) {
            // should not happen
            throw new InvalidAlgorithmParameterException(pe);
        }

        Integer digestLen = DIGEST_LENGTHS.get(toStdName(digestAlgorithm));
        if (digestLen == null) {
            throw new InvalidAlgorithmParameterException
                ("Unsupported digest algorithm in Signature parameters: " +
                 digestAlgorithm);
        }

        if (!(params.getMGFAlgorithm().equalsIgnoreCase("MGF1"))) {
            throw new InvalidAlgorithmParameterException("Only supports MGF1");
        }

        // defaults to the digest algorithm unless overridden
        String mgfDigestAlgo = digestAlgorithm;
        AlgorithmParameterSpec mgfParams = params.getMGFParameters();
        if (mgfParams != null) {
            if (!(mgfParams instanceof MGF1ParameterSpec)) {
                throw new InvalidAlgorithmParameterException
                        ("Only MGF1ParameterSpec is supported");
            }
            mgfDigestAlgo = ((MGF1ParameterSpec)mgfParams).getDigestAlgorithm();
        }

        if (params.getTrailerField() != PSSParameterSpec.TRAILER_FIELD_BC) {
            throw new InvalidAlgorithmParameterException
                ("Only supports TrailerFieldBC(1)");
        }

        int saltLen = params.getSaltLength();
        if (this.p11Key != null) {
            int maxSaltLen = ((this.p11Key.length() + 7) >> 3) -
                    digestLen.intValue() - 2;

            if (DEBUG) {
                System.out.println("Max saltLen = " + maxSaltLen);
                System.out.println("Curr saltLen = " + saltLen);
            }
            if (maxSaltLen < 0 || saltLen > maxSaltLen) {
                throw new InvalidAlgorithmParameterException
                        ("Invalid with current key size");
            }
        } else if (DEBUG) {
            System.out.println("No key available for validating saltLen");
        }

        // validated, now try to store the parameter internally
        try {
            this.mechanism.setParameter(
                    new CK_RSA_PKCS_PSS_PARAMS(digestAlgorithm, "MGF1",
                            mgfDigestAlgo, saltLen));
            this.sigParams = params;
        } catch (IllegalArgumentException iae) {
            throw new InvalidAlgorithmParameterException(iae);
        }
    }

    // see JCA spec
    @Override
    protected void engineInitVerify(PublicKey publicKey)
            throws InvalidKeyException {

        if (publicKey == null) {
            throw new InvalidKeyException("Key must not be null");
        }

        // Need to check key length whenever a new key is set
        if (publicKey != p11Key) {
            checkKeySize(publicKey);
        }

        reset(true);
        mode = M_VERIFY;
        p11Key = P11KeyFactory.convertKey(token, publicKey, KEY_ALGO);

        // attempt initialization when key and params are both available
        if (this.p11Key != null && this.sigParams != null) {
            try {
                initialize();
            } catch (ProviderException pe) {
                throw new InvalidKeyException(pe);
            }
        }
    }

    // see JCA spec
    @Override
    protected void engineInitSign(PrivateKey privateKey)
            throws InvalidKeyException {

        if (privateKey == null) {
            throw new InvalidKeyException("Key must not be null");
        }

        // Need to check RSA key length whenever a new key is set
        if (privateKey != p11Key) {
            checkKeySize(privateKey);
        }

        reset(true);
        mode = M_SIGN;
        p11Key = P11KeyFactory.convertKey(token, privateKey, KEY_ALGO);

        // attempt initialization when key and params are both available
        if (this.p11Key != null && this.sigParams != null) {
            try {
                initialize();
            } catch (ProviderException pe) {
                throw new InvalidKeyException(pe);
            }
        }
    }

    // see JCA spec
    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        ensureInitialized();
        isActive = true;
        buffer[0] = b;
        engineUpdate(buffer, 0, 1);
    }

    // see JCA spec
    @Override
    protected void engineUpdate(byte[] b, int ofs, int len)
            throws SignatureException {
        ensureInitialized();
        if (len == 0) {
            return;
        }
        // check for overflow
        if (len + bytesProcessed < 0) {
            throw new ProviderException("Processed bytes limits exceeded.");
        }
        isActive = true;
        switch (type) {
        case T_UPDATE:
            try {
                if (mode == M_SIGN) {
                    System.out.println(this + ": Calling C_SignUpdate");
                    token.p11.C_SignUpdate(session.id(), 0, b, ofs, len);
                } else {
                    System.out.println(this + ": Calling C_VerfifyUpdate");
                    token.p11.C_VerifyUpdate(session.id(), 0, b, ofs, len);
                }
                bytesProcessed += len;
            } catch (PKCS11Exception e) {
                reset(false);
                throw new ProviderException(e);
            }
            break;
        case T_DIGEST:
            // should not happen as this should be covered by earlier checks
            if (md == null) {
                throw new ProviderException("PSS Parameters required");
            }
            md.update(b, ofs, len);
            bytesProcessed += len;
            break;
        default:
            throw new ProviderException("Internal error");
        }
    }

    // see JCA spec
    @Override
    protected void engineUpdate(ByteBuffer byteBuffer) {
        try {
            ensureInitialized();
        } catch (SignatureException se) {
            throw new ProviderException(se);
        }
        int len = byteBuffer.remaining();
        if (len <= 0) {
            return;
        }
        isActive = true;
        switch (type) {
        case T_UPDATE:
            if (byteBuffer instanceof DirectBuffer == false) {
                // cannot do better than default impl
                super.engineUpdate(byteBuffer);
                return;
            }
            long addr = ((DirectBuffer)byteBuffer).address();
            int ofs = byteBuffer.position();
            try {
                if (mode == M_SIGN) {
                    System.out.println(this + ": Calling C_SignUpdate");
                    token.p11.C_SignUpdate
                        (session.id(), addr + ofs, null, 0, len);
                } else {
                    System.out.println(this + ": Calling C_VerifyUpdate");
                    token.p11.C_VerifyUpdate
                        (session.id(), addr + ofs, null, 0, len);
                }
                bytesProcessed += len;
                byteBuffer.position(ofs + len);
            } catch (PKCS11Exception e) {
                reset(false);
                throw new ProviderException("Update failed", e);
            }
            break;
        case T_DIGEST:
            // should not happen as this should be covered by earlier checks
            if (md == null) {
                throw new ProviderException("PSS Parameters required");
            }
            md.update(byteBuffer);
            bytesProcessed += len;
            break;
        default:
            reset(false);
            throw new ProviderException("Internal error");
        }
    }

    // see JCA spec
    @Override
    protected byte[] engineSign() throws SignatureException {
        ensureInitialized();
        boolean doCancel = true;
        if (DEBUG) System.out.print("Generating signature");
        try {
            byte[] signature;
            if (type == T_UPDATE) {
                if (DEBUG) System.out.println(" by C_SignFinal");
                signature = token.p11.C_SignFinal(session.id(), 0);
            } else {
                if (md == null) {
                    throw new ProviderException("PSS Parameters required");
                }
                byte[] digest = md.digest();
                if (DEBUG) System.out.println(" by C_Sign");
                signature = token.p11.C_Sign(session.id(), digest);
            }
            doCancel = false;
            return signature;
        } catch (PKCS11Exception pe) {
            // As per the PKCS#11 standard, C_Sign and C_SignFinal may only
            // keep the operation active on CKR_BUFFER_TOO_SMALL errors or
            // successful calls to determine the output length. However,
            // these cases are handled at OpenJDK's libj2pkcs11 native
            // library. Thus, doCancel can safely be 'false' here.
            doCancel = false;
            throw new ProviderException(pe);
        } catch (ProviderException e) {
            throw e;
        } finally {
            reset(doCancel);
        }
    }

    // see JCA spec
    @Override
    protected boolean engineVerify(byte[] signature) throws SignatureException {
        ensureInitialized();
        boolean doCancel = true;
        if (DEBUG) System.out.print("Verifying signature");
        try {
            if (type == T_UPDATE) {
                if (DEBUG) System.out.println(" by C_VerifyFinal");
                token.p11.C_VerifyFinal(session.id(), signature);
            } else {
                if (md == null) {
                    throw new ProviderException("PSS Parameters required");
                }
                byte[] digest = md.digest();
                if (DEBUG) System.out.println(" by C_Verify");
                token.p11.C_Verify(session.id(), digest, signature);
            }
            doCancel = false;
            return true;
        } catch (PKCS11Exception pe) {
            doCancel = false;
            long errorCode = pe.getErrorCode();
            if (errorCode == CKR_SIGNATURE_INVALID) {
                return false;
            }
            if (errorCode == CKR_SIGNATURE_LEN_RANGE) {
                // return false rather than throwing an exception
                return false;
            }
            // ECF bug?
            if (errorCode == CKR_DATA_LEN_RANGE) {
                return false;
            }
            throw new ProviderException(pe);
        }  catch (ProviderException e) {
            throw e;
        } finally {
            reset(doCancel);
        }
    }

    // see JCA spec
    @SuppressWarnings("deprecation")
    @Override
    protected void engineSetParameter(String param, Object value)
            throws InvalidParameterException {
        throw new UnsupportedOperationException("setParameter() not supported");
    }

    // see JCA spec
    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        // disallow changing parameters when update has been called
        if (isActive) {
            throw new ProviderException
                ("Cannot set parameters during operations");
        }
        setSigParams(params);
        if (type == T_DIGEST) {
            try {
                this.md = MessageDigest.getInstance(sigParams.getDigestAlgorithm());
            } catch (NoSuchAlgorithmException nsae) {
                throw new InvalidAlgorithmParameterException(nsae);
            }
        }

        // attempt initialization when key and params are both available
        if (this.p11Key != null && this.sigParams != null) {
            try {
                initialize();
            } catch (ProviderException pe) {
                throw new InvalidAlgorithmParameterException(pe);
            }
        }
    }

    // see JCA spec
    @SuppressWarnings("deprecation")
    @Override
    protected Object engineGetParameter(String param)
            throws InvalidParameterException {
        throw new UnsupportedOperationException("getParameter() not supported");
    }

    // see JCA spec
    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (this.sigParams != null) {
            try {
                AlgorithmParameters ap = AlgorithmParameters.getInstance("RSASSA-PSS");
                ap.init(this.sigParams);
                return ap;
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
