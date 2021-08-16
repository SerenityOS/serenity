/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.io.IOException;
import java.security.*;
import java.security.interfaces.EdECKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.RSAKey;
import java.security.spec.*;
import java.util.Locale;

import sun.security.rsa.RSAUtil;
import jdk.internal.access.SharedSecrets;
import sun.security.x509.AlgorithmId;

/**
 * Utility class for Signature related operations. Currently used by various
 * internal PKI classes such as sun.security.x509.X509CertImpl,
 * sun.security.pkcs.SignerInfo, for setting signature parameters.
 *
 * @since   11
 */
public class SignatureUtil {

    /**
     * Convert OID.1.2.3.4 or 1.2.3.4 to its matching stdName.
     *
     * @param algName input, could be in any form
     * @return the matching stdName, or {@code algName} if it is not in the
     *      form of an OID, or the OID value if no match is found.
     */
    private static String checkName(String algName) {
        if (algName.indexOf(".") == -1) {
            return algName;
        } else {
            // convert oid to String
            if (algName.startsWith("OID.")) {
                algName = algName.substring(4);
            }
            KnownOIDs ko = KnownOIDs.findMatch(algName);
            return ko != null ? ko.stdName() : algName;
        }
    }

    // Utility method of creating an AlgorithmParameters object with
    // the specified algorithm name and encoding
    private static AlgorithmParameters createAlgorithmParameters(String algName,
            byte[] paramBytes) throws ProviderException {

        try {
            algName = checkName(algName);
            AlgorithmParameters result =
                AlgorithmParameters.getInstance(algName);
            result.init(paramBytes);
            return result;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ProviderException(e);
        }
    }

    /**
     * Utility method for converting the specified AlgorithmParameters object
     * into an AlgorithmParameterSpec object.
     *
     * @param sigName signature algorithm
     * @param params (optional) parameters
     * @return an AlgorithmParameterSpec, null if {@code params} is null
     */
    public static AlgorithmParameterSpec getParamSpec(String sigName,
            AlgorithmParameters params)
            throws ProviderException {

        AlgorithmParameterSpec paramSpec = null;
        if (params != null) {
            sigName = checkName(sigName).toUpperCase(Locale.ENGLISH);
            // AlgorithmParameters.getAlgorithm() may returns oid if it's
            // created during DER decoding. Convert to use the standard name
            // before passing it to RSAUtil
            if (params.getAlgorithm().indexOf(".") != -1) {
                try {
                    params = createAlgorithmParameters(sigName,
                        params.getEncoded());
                } catch (IOException e) {
                    throw new ProviderException(e);
                }
            }

            if (sigName.indexOf("RSA") != -1) {
                paramSpec = RSAUtil.getParamSpec(params);
            } else if (sigName.indexOf("ECDSA") != -1) {
                try {
                    paramSpec = params.getParameterSpec(ECParameterSpec.class);
                } catch (Exception e) {
                    throw new ProviderException("Error handling EC parameters", e);
                }
            } else {
                throw new ProviderException
                    ("Unrecognized algorithm for signature parameters " +
                     sigName);
            }
        }
        return paramSpec;
    }

    /**
     * Utility method for converting the specified parameter bytes
     * into an AlgorithmParameterSpec object.
     *
     * @param sigName signature algorithm
     * @param paramBytes (optional) parameter bytes
     * @return an AlgorithmParameterSpec, null if {@code paramBytes} is null
     */
    public static AlgorithmParameterSpec getParamSpec(String sigName,
            byte[] paramBytes)
            throws ProviderException {
        AlgorithmParameterSpec paramSpec = null;

        if (paramBytes != null) {
            sigName = checkName(sigName).toUpperCase(Locale.ENGLISH);
            if (sigName.indexOf("RSA") != -1) {
                AlgorithmParameters params =
                    createAlgorithmParameters(sigName, paramBytes);
                paramSpec = RSAUtil.getParamSpec(params);
            } else if (sigName.indexOf("ECDSA") != -1) {
                try {
                    Provider p = Signature.getInstance(sigName).getProvider();
                    paramSpec = ECUtil.getECParameterSpec(p, paramBytes);
                } catch (Exception e) {
                    throw new ProviderException("Error handling EC parameters", e);
                }
                // ECUtil discards exception and returns null, so we need to check
                // the returned value
                if (paramSpec == null) {
                    throw new ProviderException("Error handling EC parameters");
                }
            } else {
                throw new ProviderException
                     ("Unrecognized algorithm for signature parameters " +
                      sigName);
            }
        }
        return paramSpec;
    }

    // Utility method for initializing the specified Signature object
    // for verification with the specified key and params (may be null)
    public static void initVerifyWithParam(Signature s, PublicKey key,
            AlgorithmParameterSpec params)
            throws ProviderException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        SharedSecrets.getJavaSecuritySignatureAccess().initVerify(s, key, params);
    }

    // Utility method for initializing the specified Signature object
    // for verification with the specified Certificate and params (may be null)
    public static void initVerifyWithParam(Signature s,
            java.security.cert.Certificate cert,
            AlgorithmParameterSpec params)
            throws ProviderException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        SharedSecrets.getJavaSecuritySignatureAccess().initVerify(s, cert, params);
    }

    // Utility method for initializing the specified Signature object
    // for signing with the specified key and params (may be null)
    public static void initSignWithParam(Signature s, PrivateKey key,
            AlgorithmParameterSpec params, SecureRandom sr)
            throws ProviderException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        SharedSecrets.getJavaSecuritySignatureAccess().initSign(s, key, params, sr);
    }

    public static class EdDSADigestAlgHolder {
        public static final AlgorithmId sha512;
        public static final AlgorithmId shake256;
        public static final AlgorithmId shake256$512;

        static {
            try {
                sha512 = new AlgorithmId(ObjectIdentifier.of(KnownOIDs.SHA_512));
                shake256 = new AlgorithmId(ObjectIdentifier.of(KnownOIDs.SHAKE256));
                shake256$512 = new AlgorithmId(
                        ObjectIdentifier.of(KnownOIDs.SHAKE256_LEN),
                        new DerValue((byte) 2, new byte[]{2, 0})); // int 512
            } catch (IOException e) {
                throw new AssertionError("Should not happen", e);
            }
        }
    }
    /**
     * Determines the digestEncryptionAlgorithmId in PKCS7 SignerInfo.
     *
     * @param signer Signature object that tells you RSASSA-PSS params
     * @param sigalg Signature algorithm
     * @param privateKey key tells you EdDSA params
     * @param directsign Ed448 uses different digest algs depending on this
     * @return the digest algId
     * @throws NoSuchAlgorithmException
     */
    public static AlgorithmId getDigestAlgInPkcs7SignerInfo(
            Signature signer, String sigalg, PrivateKey privateKey, boolean directsign)
            throws NoSuchAlgorithmException {
        AlgorithmId digAlgID;
        String kAlg = privateKey.getAlgorithm();
        if (privateKey instanceof EdECPrivateKey
                || kAlg.equalsIgnoreCase("Ed25519")
                || kAlg.equalsIgnoreCase("Ed448")) {
            if (privateKey instanceof EdECPrivateKey) {
                // Note: SunEC's kAlg is EdDSA, find out the real one
                kAlg = ((EdECPrivateKey) privateKey).getParams().getName();
            }
            // https://www.rfc-editor.org/rfc/rfc8419.html#section-3
            switch (kAlg.toUpperCase(Locale.ENGLISH)) {
                case "ED25519":
                    digAlgID = EdDSADigestAlgHolder.sha512;
                    break;
                case "ED448":
                    if (directsign) {
                        digAlgID = EdDSADigestAlgHolder.shake256;
                    } else {
                        digAlgID = EdDSADigestAlgHolder.shake256$512;
                    }
                    break;
                default:
                    throw new AssertionError("Unknown curve name: " + kAlg);
            }
        } else {
            if (sigalg.equalsIgnoreCase("RSASSA-PSS")) {
                try {
                    digAlgID = AlgorithmId.get(signer.getParameters()
                            .getParameterSpec(PSSParameterSpec.class)
                            .getDigestAlgorithm());
                } catch (InvalidParameterSpecException e) {
                    throw new AssertionError("Should not happen", e);
                }
            } else {
                digAlgID = AlgorithmId.get(extractDigestAlgFromDwithE(sigalg));
            }
        }
        return digAlgID;
    }

    /**
     * Extracts the digest algorithm name from a signature
     * algorithm name in either the "DIGESTwithENCRYPTION" or the
     * "DIGESTwithENCRYPTIONandWHATEVER" format.
     *
     * It's OK to return "SHA1" instead of "SHA-1".
     */
    public static String extractDigestAlgFromDwithE(String signatureAlgorithm) {
        signatureAlgorithm = signatureAlgorithm.toUpperCase(Locale.ENGLISH);
        int with = signatureAlgorithm.indexOf("WITH");
        if (with > 0) {
            return signatureAlgorithm.substring(0, with);
        } else {
            throw new IllegalArgumentException(
                    "Unknown algorithm: " + signatureAlgorithm);
        }
    }

    /**
     * Extracts the key algorithm name from a signature
     * algorithm name in either the "DIGESTwithENCRYPTION" or the
     * "DIGESTwithENCRYPTIONandWHATEVER" format.
     *
     * @return the key algorithm name, or null if the input
     *      is not in either of the formats.
     */
    public static String extractKeyAlgFromDwithE(String signatureAlgorithm) {
        signatureAlgorithm = signatureAlgorithm.toUpperCase(Locale.ENGLISH);
        int with = signatureAlgorithm.indexOf("WITH");
        String keyAlgorithm = null;
        if (with > 0) {
            int and = signatureAlgorithm.indexOf("AND", with + 4);
            if (and > 0) {
                keyAlgorithm = signatureAlgorithm.substring(with + 4, and);
            } else {
                keyAlgorithm = signatureAlgorithm.substring(with + 4);
            }
            if (keyAlgorithm.equalsIgnoreCase("ECDSA")) {
                keyAlgorithm = "EC";
            }
        }
        return keyAlgorithm;
    }

    /**
     * Returns default AlgorithmParameterSpec for a key used in a signature.
     * This is only useful for RSASSA-PSS now, which is the only algorithm
     * that must be initialized with a AlgorithmParameterSpec now.
     */
    public static AlgorithmParameterSpec getDefaultParamSpec(
            String sigAlg, Key k) {
        sigAlg = checkName(sigAlg);
        if (sigAlg.equalsIgnoreCase("RSASSA-PSS")) {
            if (k instanceof RSAKey) {
                AlgorithmParameterSpec spec = ((RSAKey) k).getParams();
                if (spec instanceof PSSParameterSpec) {
                    return spec;
                }
            }
            switch (ifcFfcStrength(KeyUtil.getKeySize(k))) {
                case "SHA256":
                    return PSSParamsHolder.PSS_256_SPEC;
                case "SHA384":
                    return PSSParamsHolder.PSS_384_SPEC;
                case "SHA512":
                    return PSSParamsHolder.PSS_512_SPEC;
                default:
                    throw new AssertionError("Should not happen");
            }
        } else {
            return null;
        }
    }

    /**
     * Create a Signature that has been initialized with proper key and params.
     *
     * @param sigAlg signature algorithms
     * @param key public or private key
     * @param provider (optional) provider
     */
    public static Signature fromKey(String sigAlg, Key key, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException,
                   InvalidKeyException{
        Signature sigEngine = (provider == null || provider.isEmpty())
                ? Signature.getInstance(sigAlg)
                : Signature.getInstance(sigAlg, provider);
        return autoInitInternal(sigAlg, key, sigEngine);
    }

    /**
     * Create a Signature that has been initialized with proper key and params.
     *
     * @param sigAlg signature algorithms
     * @param key public or private key
     * @param provider (optional) provider
     */
    public static Signature fromKey(String sigAlg, Key key, Provider provider)
            throws NoSuchAlgorithmException, InvalidKeyException{
        Signature sigEngine = (provider == null)
                ? Signature.getInstance(sigAlg)
                : Signature.getInstance(sigAlg, provider);
        return autoInitInternal(sigAlg, key, sigEngine);
    }

    private static Signature autoInitInternal(String alg, Key key, Signature s)
            throws InvalidKeyException {
        AlgorithmParameterSpec params = SignatureUtil
                .getDefaultParamSpec(alg, key);
        try {
            if (key instanceof PrivateKey) {
                SignatureUtil.initSignWithParam(s, (PrivateKey) key, params,
                        null);
            } else {
                SignatureUtil.initVerifyWithParam(s, (PublicKey) key, params);
            }
        } catch (InvalidAlgorithmParameterException e) {
            throw new AssertionError("Should not happen", e);
        }
        return s;
    }

    /**
     * Derives AlgorithmId from a signature object and a key.
     * @param sigEngine the signature object
     * @param key the private key
     * @return the AlgorithmId, not null
     * @throws SignatureException if cannot find one
     */
    public static AlgorithmId fromSignature(Signature sigEngine, PrivateKey key)
            throws SignatureException {
        try {
            if (key instanceof EdECKey) {
                return AlgorithmId.get(((EdECKey) key).getParams().getName());
            }

            AlgorithmParameters params = null;
            try {
                params = sigEngine.getParameters();
            } catch (UnsupportedOperationException e) {
                // some provider does not support it
            }
            if (params != null) {
                return AlgorithmId.get(sigEngine.getParameters());
            } else {
                String sigAlg = sigEngine.getAlgorithm();
                if (sigAlg.equalsIgnoreCase("EdDSA")) {
                    // Hopefully key knows if it's Ed25519 or Ed448
                    sigAlg = key.getAlgorithm();
                }
                return AlgorithmId.get(sigAlg);
            }
        } catch (NoSuchAlgorithmException e) {
            // This could happen if both sig alg and key alg is EdDSA,
            // we don't know which provider does this.
            throw new SignatureException("Cannot derive AlgorithmIdentifier", e);
        }
    }

    /**
     * Checks if a signature algorithm matches a key, i.e. if this
     * signature can be initialized with this key. Currently used
     * in {@link jdk.security.jarsigner.JarSigner} to fail early.
     *
     * Note: Unknown signature algorithms are allowed.
     *
     * @param key must not be null
     * @param sAlg must not be null
     * @throws IllegalArgumentException if they are known to not match
     */
    public static void checkKeyAndSigAlgMatch(PrivateKey key, String sAlg) {
        String kAlg = key.getAlgorithm().toUpperCase(Locale.ENGLISH);
        sAlg = checkName(sAlg).toUpperCase(Locale.ENGLISH);
        switch (sAlg) {
            case "RSASSA-PSS" -> {
                if (!kAlg.equals("RSASSA-PSS")
                        && !kAlg.equals("RSA")) {
                    throw new IllegalArgumentException(
                            "key algorithm not compatible with signature algorithm");
                }
            }
            case "EDDSA" -> {
                // General EdDSA, any EDDSA name variance is OK
                if (!kAlg.equals("EDDSA") && !kAlg.equals("ED448")
                        && !kAlg.equals("ED25519")) {
                    throw new IllegalArgumentException(
                            "key algorithm not compatible with signature algorithm");
                }
            }
            case "ED25519", "ED448" -> {
                // fix-size EdDSA
                if (key instanceof EdECKey) {
                    // SunEC's key alg is fix-size. Must match.
                    String groupName = ((EdECKey) key).getParams()
                            .getName().toUpperCase(Locale.US);
                    if (!sAlg.equals(groupName)) {
                        throw new IllegalArgumentException(
                                "key algorithm not compatible with signature algorithm");
                    }
                } else {
                    // Other vendor might be generalized or fix-size
                    if (!kAlg.equals("EDDSA") && !kAlg.equals(sAlg)) {
                        throw new IllegalArgumentException(
                                "key algorithm not compatible with signature algorithm");
                    }
                }
            }
            default -> {
                if (sAlg.contains("WITH")) {
                    if ((sAlg.endsWith("WITHRSA") && !kAlg.equals("RSA")) ||
                            (sAlg.endsWith("WITHECDSA") && !kAlg.equals("EC")) ||
                            (sAlg.endsWith("WITHDSA") && !kAlg.equals("DSA"))) {
                        throw new IllegalArgumentException(
                                "key algorithm not compatible with signature algorithm");
                    }
                }
                // Do not fail now. Maybe new algorithm we don't know.
            }
        }
    }

    /**
     * Returns the default signature algorithm for a private key.
     *
     * @param k cannot be null
     * @return the default alg, might be null if unsupported
     */
    public static String getDefaultSigAlgForKey(PrivateKey k) {
        String kAlg = k.getAlgorithm();
        return switch (kAlg.toUpperCase(Locale.ENGLISH)) {
            case "DSA", "RSA" -> ifcFfcStrength(KeyUtil.getKeySize(k))
                    + "with" + kAlg;
            case "EC" -> ecStrength(KeyUtil.getKeySize(k))
                    + "withECDSA";
            case "EDDSA" -> k instanceof EdECPrivateKey
                    ? ((EdECPrivateKey) k).getParams().getName()
                    : kAlg;
            case "RSASSA-PSS", "ED25519", "ED448" -> kAlg;
            default -> null;
        };
    }

    // Useful PSSParameterSpec objects
    private static class PSSParamsHolder {
        static final PSSParameterSpec PSS_256_SPEC = new PSSParameterSpec(
                "SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256,
                32, PSSParameterSpec.TRAILER_FIELD_BC);
        static final PSSParameterSpec PSS_384_SPEC = new PSSParameterSpec(
                "SHA-384", "MGF1",
                MGF1ParameterSpec.SHA384,
                48, PSSParameterSpec.TRAILER_FIELD_BC);
        static final PSSParameterSpec PSS_512_SPEC = new PSSParameterSpec(
                "SHA-512", "MGF1",
                MGF1ParameterSpec.SHA512,
                64, PSSParameterSpec.TRAILER_FIELD_BC);
    }

    // The following values are from SP800-57 part 1 rev 4 tables 2 and 3

    /**
     * Return the default message digest algorithm with the same security
     * strength as the specified EC key size.
     *
     * Attention: sync with the @implNote inside
     * {@link jdk.security.jarsigner.JarSigner.Builder#getDefaultSignatureAlgorithm}.
     */
    private static String ecStrength (int bitLength) {
        if (bitLength >= 512) { // 256 bits of strength
            return "SHA512";
        } else if (bitLength >= 384) {  // 192 bits of strength
            return "SHA384";
        } else { // 128 bits of strength and less
            return "SHA256";
        }
    }

    /**
     * Return the default message digest algorithm with the same security
     * strength as the specified IFC/FFC key size.
     *
     * Attention: sync with the @implNote inside
     * {@link jdk.security.jarsigner.JarSigner.Builder#getDefaultSignatureAlgorithm}.
     */
    private static String ifcFfcStrength (int bitLength) {
        if (bitLength > 7680) { // 256 bits
            return "SHA512";
        } else if (bitLength > 3072) {  // 192 bits
            return "SHA384";
        } else  { // 128 bits and less
            return "SHA256";
        }
    }
}
