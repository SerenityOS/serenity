/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs;

import java.io.OutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.security.*;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sun.security.provider.SHAKE256;
import sun.security.timestamp.TimestampToken;
import sun.security.util.*;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;
import sun.security.x509.KeyUsageExtension;

/**
 * A SignerInfo, as defined in PKCS#7's signedData type.
 *
 * @author Benjamin Renaud
 */
public class SignerInfo implements DerEncoder {

    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK =
            DisabledAlgorithmConstraints.jarConstraints();

    BigInteger version;
    X500Name issuerName;
    BigInteger certificateSerialNumber;
    AlgorithmId digestAlgorithmId;
    AlgorithmId digestEncryptionAlgorithmId;
    byte[] encryptedDigest;
    Timestamp timestamp;
    private boolean hasTimestamp = true;
    private static final Debug debug = Debug.getInstance("jar");

    PKCS9Attributes authenticatedAttributes;
    PKCS9Attributes unauthenticatedAttributes;

    /**
     * A map containing the algorithms in this SignerInfo. This is used to
     * avoid checking algorithms to see if they are disabled more than once.
     * The key is the AlgorithmId of the algorithm, and the value is the name of
     * the field or attribute.
     */
    private Map<AlgorithmId, String> algorithms = new HashMap<>();

    public SignerInfo(X500Name  issuerName,
                      BigInteger serial,
                      AlgorithmId digestAlgorithmId,
                      AlgorithmId digestEncryptionAlgorithmId,
                      byte[] encryptedDigest) {
        this(issuerName, serial, digestAlgorithmId, null,
                digestEncryptionAlgorithmId, encryptedDigest, null);
    }

    public SignerInfo(X500Name  issuerName,
                      BigInteger serial,
                      AlgorithmId digestAlgorithmId,
                      PKCS9Attributes authenticatedAttributes,
                      AlgorithmId digestEncryptionAlgorithmId,
                      byte[] encryptedDigest,
                      PKCS9Attributes unauthenticatedAttributes) {
        this.version = BigInteger.ONE;
        this.issuerName = issuerName;
        this.certificateSerialNumber = serial;
        this.digestAlgorithmId = digestAlgorithmId;
        this.authenticatedAttributes = authenticatedAttributes;
        this.digestEncryptionAlgorithmId = digestEncryptionAlgorithmId;
        this.encryptedDigest = encryptedDigest;
        this.unauthenticatedAttributes = unauthenticatedAttributes;
    }

    /**
     * Parses a PKCS#7 signer info.
     */
    public SignerInfo(DerInputStream derin)
        throws IOException, ParsingException
    {
        this(derin, false);
    }

    /**
     * Parses a PKCS#7 signer info.
     *
     * <p>This constructor is used only for backwards compatibility with
     * PKCS#7 blocks that were generated using JDK1.1.x.
     *
     * @param derin the ASN.1 encoding of the signer info.
     * @param oldStyle flag indicating whether or not the given signer info
     * is encoded according to JDK1.1.x.
     */
    public SignerInfo(DerInputStream derin, boolean oldStyle)
        throws IOException, ParsingException
    {
        // version
        version = derin.getBigInteger();

        // issuerAndSerialNumber
        DerValue[] issuerAndSerialNumber = derin.getSequence(2);
        if (issuerAndSerialNumber.length != 2) {
            throw new ParsingException("Invalid length for IssuerAndSerialNumber");
        }
        byte[] issuerBytes = issuerAndSerialNumber[0].toByteArray();
        issuerName = new X500Name(new DerValue(DerValue.tag_Sequence,
                                               issuerBytes));
        certificateSerialNumber = issuerAndSerialNumber[1].getBigInteger();

        // digestAlgorithmId
        DerValue tmp = derin.getDerValue();

        digestAlgorithmId = AlgorithmId.parse(tmp);

        // authenticatedAttributes
        if (oldStyle) {
            // In JDK1.1.x, the authenticatedAttributes are always present,
            // encoded as an empty Set (Set of length zero)
            derin.getSet(0);
        } else {
            // check if set of auth attributes (implicit tag) is provided
            // (auth attributes are OPTIONAL)
            if ((byte)(derin.peekByte()) == (byte)0xA0) {
                authenticatedAttributes = new PKCS9Attributes(derin);
            }
        }

        // digestEncryptionAlgorithmId - little RSA naming scheme -
        // signature == encryption...
        tmp = derin.getDerValue();

        digestEncryptionAlgorithmId = AlgorithmId.parse(tmp);

        // encryptedDigest
        encryptedDigest = derin.getOctetString();

        // unauthenticatedAttributes
        if (oldStyle) {
            // In JDK1.1.x, the unauthenticatedAttributes are always present,
            // encoded as an empty Set (Set of length zero)
            derin.getSet(0);
        } else {
            // check if set of unauth attributes (implicit tag) is provided
            // (unauth attributes are OPTIONAL)
            if (derin.available() != 0
                && (byte)(derin.peekByte()) == (byte)0xA1) {
                unauthenticatedAttributes =
                    new PKCS9Attributes(derin, true);// ignore unsupported attrs
            }
        }

        // all done
        if (derin.available() != 0) {
            throw new ParsingException("extra data at the end");
        }

        // verify CMSAlgorithmProtection
        checkCMSAlgorithmProtection();
    }

    // CMSAlgorithmProtection verification as described in RFC 6211
    private void checkCMSAlgorithmProtection() throws IOException {
        if (authenticatedAttributes == null) {
            return;
        }
        PKCS9Attribute ap = authenticatedAttributes.getAttribute(
                PKCS9Attribute.CMS_ALGORITHM_PROTECTION_OID);
        if (ap == null) {
            return;
        }
        DerValue dv = new DerValue((byte[])ap.getValue());
        DerInputStream data = dv.data();
        AlgorithmId d = AlgorithmId.parse(data.getDerValue());
        DerValue ds = data.getDerValue();
        if (data.available() > 0) {
            throw new IOException("Unknown field in CMSAlgorithmProtection");
        }
        if (!ds.isContextSpecific((byte)1)) {
            throw new IOException("No signature algorithm in CMSAlgorithmProtection");
        }
        AlgorithmId s = AlgorithmId.parse(ds.withTag(DerValue.tag_Sequence));
        if (!s.equals(digestEncryptionAlgorithmId)
                || !d.equals(digestAlgorithmId)) {
            throw new IOException("CMSAlgorithmProtection check failed");
        }
    }

    public void encode(DerOutputStream out) throws IOException {

        derEncode(out);
    }

    /**
     * DER encode this object onto an output stream.
     * Implements the {@code DerEncoder} interface.
     *
     * @param out
     * the output stream on which to write the DER encoding.
     *
     * @exception IOException on encoding error.
     */
    public void derEncode(OutputStream out) throws IOException {
        DerOutputStream seq = new DerOutputStream();
        seq.putInteger(version);
        DerOutputStream issuerAndSerialNumber = new DerOutputStream();
        issuerName.encode(issuerAndSerialNumber);
        issuerAndSerialNumber.putInteger(certificateSerialNumber);
        seq.write(DerValue.tag_Sequence, issuerAndSerialNumber);

        digestAlgorithmId.encode(seq);

        // encode authenticated attributes if there are any
        if (authenticatedAttributes != null)
            authenticatedAttributes.encode((byte)0xA0, seq);

        digestEncryptionAlgorithmId.encode(seq);

        seq.putOctetString(encryptedDigest);

        // encode unauthenticated attributes if there are any
        if (unauthenticatedAttributes != null)
            unauthenticatedAttributes.encode((byte)0xA1, seq);

        DerOutputStream tmp = new DerOutputStream();
        tmp.write(DerValue.tag_Sequence, seq);

        out.write(tmp.toByteArray());
    }

    /*
     * Returns the (user) certificate pertaining to this SignerInfo.
     */
    public X509Certificate getCertificate(PKCS7 block)
        throws IOException
    {
        return block.getCertificate(certificateSerialNumber, issuerName);
    }

    /*
     * Returns the certificate chain pertaining to this SignerInfo.
     */
    public ArrayList<X509Certificate> getCertificateChain(PKCS7 block)
        throws IOException
    {
        X509Certificate userCert;
        userCert = block.getCertificate(certificateSerialNumber, issuerName);
        if (userCert == null)
            return null;

        ArrayList<X509Certificate> certList = new ArrayList<>();
        certList.add(userCert);

        X509Certificate[] pkcsCerts = block.getCertificates();
        if (pkcsCerts == null
            || userCert.getSubjectX500Principal().equals(userCert.getIssuerX500Principal())) {
            return certList;
        }

        Principal issuer = userCert.getIssuerX500Principal();
        int start = 0;
        while (true) {
            boolean match = false;
            int i = start;
            while (i < pkcsCerts.length) {
                if (issuer.equals(pkcsCerts[i].getSubjectX500Principal())) {
                    // next cert in chain found
                    certList.add(pkcsCerts[i]);
                    // if selected cert is self-signed, we're done
                    // constructing the chain
                    if (pkcsCerts[i].getSubjectX500Principal().equals(
                                            pkcsCerts[i].getIssuerX500Principal())) {
                        start = pkcsCerts.length;
                    } else {
                        issuer = pkcsCerts[i].getIssuerX500Principal();
                        X509Certificate tmpCert = pkcsCerts[start];
                        pkcsCerts[start] = pkcsCerts[i];
                        pkcsCerts[i] = tmpCert;
                        start++;
                    }
                    match = true;
                    break;
                } else {
                    i++;
                }
            }
            if (!match)
                break;
        }

        return certList;
    }

    /* Returns null if verify fails, this signerInfo if
       verify succeeds. */
    SignerInfo verify(PKCS7 block, byte[] data)
    throws NoSuchAlgorithmException, SignatureException {

        try {
            Timestamp timestamp = null;
            try {
                timestamp = getTimestamp();
            } catch (Exception e) {
                // Log exception and continue. This allows for the case
                // where, if there are no other errors, the code is
                // signed but w/o a timestamp.
                if (debug != null) {
                    debug.println("Unexpected exception while getting" +
                                  " timestamp: " + e);
                }
            }

            ContentInfo content = block.getContentInfo();
            if (data == null) {
                data = content.getContentBytes();
            }

            String digestAlgName = digestAlgorithmId.getName();
            algorithms.put(digestAlgorithmId, "SignerInfo digestAlgorithm field");

            byte[] dataSigned;

            // if there are authenticate attributes, get the message
            // digest and compare it with the digest of data
            if (authenticatedAttributes == null) {
                dataSigned = data;
            } else {

                // first, check content type
                ObjectIdentifier contentType = (ObjectIdentifier)
                       authenticatedAttributes.getAttributeValue(
                         PKCS9Attribute.CONTENT_TYPE_OID);
                if (contentType == null ||
                    !contentType.equals(content.contentType))
                    return null;  // contentType does not match, bad SignerInfo

                // now, check message digest
                byte[] messageDigest = (byte[])
                    authenticatedAttributes.getAttributeValue(
                         PKCS9Attribute.MESSAGE_DIGEST_OID);

                if (messageDigest == null) // fail if there is no message digest
                    return null;

                byte[] computedMessageDigest;
                if (digestAlgName.equals("SHAKE256")
                        || digestAlgName.equals("SHAKE256-LEN")) {
                    if (digestAlgName.equals("SHAKE256-LEN")) {
                        int v = new DerValue(digestAlgorithmId
                                .getEncodedParams()).getInteger();
                        if (v != 512) {
                            throw new SignatureException(
                                    "Unsupported id-shake256-" + v);
                        }
                    }
                    var md = new SHAKE256(64);
                    md.update(data, 0, data.length);
                    computedMessageDigest = md.digest();
                } else {
                    MessageDigest md = MessageDigest.getInstance(digestAlgName);
                    computedMessageDigest = md.digest(data);
                }

                if (!MessageDigest.isEqual(messageDigest, computedMessageDigest)) {
                    return null;
                }

                // message digest attribute matched
                // digest of original data

                // the data actually signed is the DER encoding of
                // the authenticated attributes (tagged with
                // the "SET OF" tag, not 0xA0).
                dataSigned = authenticatedAttributes.getDerEncoding();
            }

            // put together digest algorithm and encryption algorithm
            // to form signing algorithm. See makeSigAlg for details.
            String sigAlgName = makeSigAlg(
                    digestAlgorithmId,
                    digestEncryptionAlgorithmId,
                    authenticatedAttributes == null);

            KnownOIDs oid = KnownOIDs.findMatch(sigAlgName);
            if (oid != null) {
                AlgorithmId sigAlgId =
                    new AlgorithmId(ObjectIdentifier.of(oid),
                            digestEncryptionAlgorithmId.getParameters());
                algorithms.put(sigAlgId,
                    "SignerInfo digestEncryptionAlgorithm field");
            }

            X509Certificate cert = getCertificate(block);
            if (cert == null) {
                return null;
            }
            PublicKey key = cert.getPublicKey();

            if (cert.hasUnsupportedCriticalExtension()) {
                throw new SignatureException("Certificate has unsupported "
                                             + "critical extension(s)");
            }

            // Make sure that if the usage of the key in the certificate is
            // restricted, it can be used for digital signatures.
            // XXX We may want to check for additional extensions in the
            // future.
            boolean[] keyUsageBits = cert.getKeyUsage();
            if (keyUsageBits != null) {
                KeyUsageExtension keyUsage;
                try {
                    // We don't care whether or not this extension was marked
                    // critical in the certificate.
                    // We're interested only in its value (i.e., the bits set)
                    // and treat the extension as critical.
                    keyUsage = new KeyUsageExtension(keyUsageBits);
                } catch (IOException ioe) {
                    throw new SignatureException("Failed to parse keyUsage "
                                                 + "extension");
                }

                boolean digSigAllowed
                        = keyUsage.get(KeyUsageExtension.DIGITAL_SIGNATURE);

                boolean nonRepuAllowed
                        = keyUsage.get(KeyUsageExtension.NON_REPUDIATION);

                if (!digSigAllowed && !nonRepuAllowed) {
                    throw new SignatureException("Key usage restricted: "
                                                 + "cannot be used for "
                                                 + "digital signatures");
                }
            }

            Signature sig = Signature.getInstance(sigAlgName);

            AlgorithmParameters ap =
                digestEncryptionAlgorithmId.getParameters();
            try {
                SignatureUtil.initVerifyWithParam(sig, key,
                    SignatureUtil.getParamSpec(sigAlgName, ap));
            } catch (ProviderException | InvalidAlgorithmParameterException |
                     InvalidKeyException e) {
                throw new SignatureException(e.getMessage(), e);
            }

            sig.update(dataSigned);
            if (sig.verify(encryptedDigest)) {
                return this;
            }
        } catch (IOException e) {
            throw new SignatureException("Error verifying signature", e);
        }
        return null;
    }

    /**
     * Derives the signature algorithm name from the digest algorithm
     * and the encryption algorithm inside a PKCS7 SignerInfo.
     *
     * The digest algorithm is in the form "DIG", and the encryption
     * algorithm can be in any of the 3 forms:
     *
     * 1. Old style key algorithm like RSA, DSA, EC, this method returns
     *    DIGwithKEY.
     * 2. New style signature algorithm in the form of HASHwithKEY, this
     *    method returns DIGwithKEY. Please note this is not HASHwithKEY.
     * 3. Modern signature algorithm like RSASSA-PSS and EdDSA, this method
     *    returns the signature algorithm itself but ensures digAlgId is
     *    compatible with the algorithm as described in RFC 4056 and 8419.
     *
     * @param digAlgId the digest algorithm
     * @param encAlgId the encryption algorithm
     * @param directSign whether the signature is calculated on the content
     *                   directly. This makes difference for Ed448.
     */
    public static String makeSigAlg(AlgorithmId digAlgId, AlgorithmId encAlgId,
            boolean directSign) throws NoSuchAlgorithmException {
        String encAlg = encAlgId.getName();
        switch (encAlg) {
            case "RSASSA-PSS":
                PSSParameterSpec spec = (PSSParameterSpec)
                        SignatureUtil.getParamSpec(encAlg, encAlgId.getParameters());
                if (!AlgorithmId.get(spec.getDigestAlgorithm()).equals(digAlgId)) {
                    throw new NoSuchAlgorithmException("Incompatible digest algorithm");
                }
                return encAlg;
            case "Ed25519":
                if (!digAlgId.equals(SignatureUtil.EdDSADigestAlgHolder.sha512)) {
                    throw new NoSuchAlgorithmException("Incompatible digest algorithm");
                }
                return encAlg;
            case "Ed448":
                if (directSign) {
                    if (!digAlgId.equals(SignatureUtil.EdDSADigestAlgHolder.shake256)) {
                        throw new NoSuchAlgorithmException("Incompatible digest algorithm");
                    }
                } else {
                    if (!digAlgId.equals(SignatureUtil.EdDSADigestAlgHolder.shake256$512)) {
                        throw new NoSuchAlgorithmException("Incompatible digest algorithm");
                    }
                }
                return encAlg;
            default:
                String digAlg = digAlgId.getName();
                String keyAlg = SignatureUtil.extractKeyAlgFromDwithE(encAlg);
                if (keyAlg == null) {
                    // The encAlg used to be only the key alg
                    keyAlg = encAlg;
                }
                if (digAlg.startsWith("SHA-")) {
                    digAlg = "SHA" + digAlg.substring(4);
                }
                if (keyAlg.equals("EC")) keyAlg = "ECDSA";
                return digAlg + "with" + keyAlg;
        }
    }

    /* Verify the content of the pkcs7 block. */
    SignerInfo verify(PKCS7 block)
        throws NoSuchAlgorithmException, SignatureException {
        return verify(block, null);
    }

    public BigInteger getVersion() {
            return version;
    }

    public X500Name getIssuerName() {
        return issuerName;
    }

    public BigInteger getCertificateSerialNumber() {
        return certificateSerialNumber;
    }

    public AlgorithmId getDigestAlgorithmId() {
        return digestAlgorithmId;
    }

    public PKCS9Attributes getAuthenticatedAttributes() {
        return authenticatedAttributes;
    }

    public AlgorithmId getDigestEncryptionAlgorithmId() {
        return digestEncryptionAlgorithmId;
    }

    public byte[] getEncryptedDigest() {
        return encryptedDigest;
    }

    public PKCS9Attributes getUnauthenticatedAttributes() {
        return unauthenticatedAttributes;
    }

    /**
     * Returns the timestamp PKCS7 data unverified.
     * @return a PKCS7 object
     */
    public PKCS7 getTsToken() throws IOException {
        if (unauthenticatedAttributes == null) {
            return null;
        }
        PKCS9Attribute tsTokenAttr =
                unauthenticatedAttributes.getAttribute(
                        PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_OID);
        if (tsTokenAttr == null) {
            return null;
        }
        return new PKCS7((byte[])tsTokenAttr.getValue());
    }

    /*
     * Extracts a timestamp from a PKCS7 SignerInfo.
     *
     * Examines the signer's unsigned attributes for a
     * {@code signatureTimestampToken} attribute. If present,
     * then it is parsed to extract the date and time at which the
     * timestamp was generated.
     *
     * @param info A signer information element of a PKCS 7 block.
     *
     * @return A timestamp token or null if none is present.
     * @throws IOException if an error is encountered while parsing the
     *         PKCS7 data.
     * @throws NoSuchAlgorithmException if an error is encountered while
     *         verifying the PKCS7 object.
     * @throws SignatureException if an error is encountered while
     *         verifying the PKCS7 object.
     * @throws CertificateException if an error is encountered while generating
     *         the TSA's certpath.
     */
    public Timestamp getTimestamp()
        throws IOException, NoSuchAlgorithmException, SignatureException,
               CertificateException
    {
        if (timestamp != null || !hasTimestamp)
            return timestamp;

        PKCS7 tsToken = getTsToken();
        if (tsToken == null) {
            hasTimestamp = false;
            return null;
        }

        // Extract the content (an encoded timestamp token info)
        byte[] encTsTokenInfo = tsToken.getContentInfo().getData();
        // Extract the signer (the Timestamping Authority)
        // while verifying the content
        SignerInfo[] tsa = tsToken.verify(encTsTokenInfo);
        if (tsa == null || tsa.length == 0) {
            throw new SignatureException("Unable to verify timestamp");
        }
        // Expect only one signer
        ArrayList<X509Certificate> chain = tsa[0].getCertificateChain(tsToken);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath tsaChain = cf.generateCertPath(chain);
        // Create a timestamp token info object
        TimestampToken tsTokenInfo = new TimestampToken(encTsTokenInfo);
        // Check that the signature timestamp applies to this signature
        verifyTimestamp(tsTokenInfo);
        algorithms.putAll(tsa[0].algorithms);
        // Create a timestamp object
        timestamp = new Timestamp(tsTokenInfo.getDate(), tsaChain);
        return timestamp;
    }

    /*
     * Check that the signature timestamp applies to this signature.
     * Match the hash present in the signature timestamp token against the hash
     * of this signature.
     */
    private void verifyTimestamp(TimestampToken token)
        throws NoSuchAlgorithmException, SignatureException {

        AlgorithmId digestAlgId = token.getHashAlgorithm();
        algorithms.put(digestAlgId, "TimestampToken digestAlgorithm field");

        MessageDigest md = MessageDigest.getInstance(digestAlgId.getName());

        if (!MessageDigest.isEqual(token.getHashedMessage(),
            md.digest(encryptedDigest))) {

            throw new SignatureException("Signature timestamp (#" +
                token.getSerialNumber() + ") generated on " + token.getDate() +
                " is inapplicable");
        }

        if (debug != null) {
            debug.println();
            debug.println("Detected signature timestamp (#" +
                token.getSerialNumber() + ") generated on " + token.getDate());
            debug.println();
        }
    }

    public String toString() {
        HexDumpEncoder hexDump = new HexDumpEncoder();

        String out = "";

        out += "Signer Info for (issuer): " + issuerName + "\n";
        out += "\tversion: " + Debug.toHexString(version) + "\n";
        out += "\tcertificateSerialNumber: " +
               Debug.toHexString(certificateSerialNumber) + "\n";
        out += "\tdigestAlgorithmId: " + digestAlgorithmId + "\n";
        if (authenticatedAttributes != null) {
            out += "\tauthenticatedAttributes: " + authenticatedAttributes +
                   "\n";
        }
        out += "\tdigestEncryptionAlgorithmId: " + digestEncryptionAlgorithmId +
            "\n";

        out += "\tencryptedDigest: " + "\n" +
            hexDump.encodeBuffer(encryptedDigest) + "\n";
        if (unauthenticatedAttributes != null) {
            out += "\tunauthenticatedAttributes: " +
                   unauthenticatedAttributes + "\n";
        }
        return out;
    }

    /**
     * Verify all of the algorithms in the array of SignerInfos against the
     * constraints in the jdk.jar.disabledAlgorithms security property.
     *
     * @param infos array of SignerInfos
     * @param params constraint parameters
     * @param name the name of the signer's PKCS7 file
     * @return a set of algorithms that passed the checks and are not disabled
     */
    public static Set<String> verifyAlgorithms(SignerInfo[] infos,
        JarConstraintsParameters params, String name) throws SignatureException {
        Map<AlgorithmId, String> algorithms = new HashMap<>();
        for (SignerInfo info : infos) {
            algorithms.putAll(info.algorithms);
        }

        Set<String> enabledAlgorithms = new HashSet<>();
        try {
            for (Map.Entry<AlgorithmId, String> algorithm : algorithms.entrySet()) {
                params.setExtendedExceptionMsg(name, algorithm.getValue());
                AlgorithmId algId = algorithm.getKey();
                JAR_DISABLED_CHECK.permits(algId.getName(),
                    algId.getParameters(), params);
                enabledAlgorithms.add(algId.getName());
            }
        } catch (CertPathValidatorException e) {
            throw new SignatureException(e);
        }
        return enabledAlgorithms;
    }
}
