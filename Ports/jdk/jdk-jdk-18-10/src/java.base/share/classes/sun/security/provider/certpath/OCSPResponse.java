/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider.certpath;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CRLReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

import sun.security.util.HexDumpEncoder;
import sun.security.action.GetIntegerAction;
import sun.security.x509.*;
import sun.security.util.*;

/**
 * This class is used to process an OCSP response.
 * The OCSP Response is defined
 * in RFC 2560 and the ASN.1 encoding is as follows:
 * <pre>
 *
 *  OCSPResponse ::= SEQUENCE {
 *      responseStatus         OCSPResponseStatus,
 *      responseBytes          [0] EXPLICIT ResponseBytes OPTIONAL }
 *
 *   OCSPResponseStatus ::= ENUMERATED {
 *       successful            (0),  --Response has valid confirmations
 *       malformedRequest      (1),  --Illegal confirmation request
 *       internalError         (2),  --Internal error in issuer
 *       tryLater              (3),  --Try again later
 *                                   --(4) is not used
 *       sigRequired           (5),  --Must sign the request
 *       unauthorized          (6)   --Request unauthorized
 *   }
 *
 *   ResponseBytes ::=       SEQUENCE {
 *       responseType   OBJECT IDENTIFIER,
 *       response       OCTET STRING }
 *
 *   BasicOCSPResponse       ::= SEQUENCE {
 *      tbsResponseData      ResponseData,
 *      signatureAlgorithm   AlgorithmIdentifier,
 *      signature            BIT STRING,
 *      certs                [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL }
 *
 *   The value for signature SHALL be computed on the hash of the DER
 *   encoding ResponseData.
 *
 *   ResponseData ::= SEQUENCE {
 *      version              [0] EXPLICIT Version DEFAULT v1,
 *      responderID              ResponderID,
 *      producedAt               GeneralizedTime,
 *      responses                SEQUENCE OF SingleResponse,
 *      responseExtensions   [1] EXPLICIT Extensions OPTIONAL }
 *
 *   ResponderID ::= CHOICE {
 *      byName               [1] Name,
 *      byKey                [2] KeyHash }
 *
 *   KeyHash ::= OCTET STRING -- SHA-1 hash of responder's public key
 *   (excluding the tag and length fields)
 *
 *   SingleResponse ::= SEQUENCE {
 *      certID                       CertID,
 *      certStatus                   CertStatus,
 *      thisUpdate                   GeneralizedTime,
 *      nextUpdate         [0]       EXPLICIT GeneralizedTime OPTIONAL,
 *      singleExtensions   [1]       EXPLICIT Extensions OPTIONAL }
 *
 *   CertStatus ::= CHOICE {
 *       good        [0]     IMPLICIT NULL,
 *       revoked     [1]     IMPLICIT RevokedInfo,
 *       unknown     [2]     IMPLICIT UnknownInfo }
 *
 *   RevokedInfo ::= SEQUENCE {
 *       revocationTime              GeneralizedTime,
 *       revocationReason    [0]     EXPLICIT CRLReason OPTIONAL }
 *
 *   UnknownInfo ::= NULL -- this can be replaced with an enumeration
 *
 * </pre>
 *
 * @author      Ram Marti
 */

public final class OCSPResponse {

    public enum ResponseStatus {
        SUCCESSFUL,            // Response has valid confirmations
        MALFORMED_REQUEST,     // Illegal request
        INTERNAL_ERROR,        // Internal error in responder
        TRY_LATER,             // Try again later
        UNUSED,                // is not used
        SIG_REQUIRED,          // Must sign the request
        UNAUTHORIZED           // Request unauthorized
    };
    private static final ResponseStatus[] rsvalues = ResponseStatus.values();

    private static final Debug debug = Debug.getInstance("certpath");
    private static final boolean dump = debug != null && Debug.isOn("ocsp");
    private static final ObjectIdentifier OCSP_BASIC_RESPONSE_OID =
        ObjectIdentifier.of(KnownOIDs.OCSPBasicResponse);
    private static final int CERT_STATUS_GOOD = 0;
    private static final int CERT_STATUS_REVOKED = 1;
    private static final int CERT_STATUS_UNKNOWN = 2;

    // ResponderID CHOICE tags
    private static final int NAME_TAG = 1;
    private static final int KEY_TAG = 2;

    // Default maximum clock skew in milliseconds (15 minutes)
    // allowed when checking validity of OCSP responses
    private static final int DEFAULT_MAX_CLOCK_SKEW = 900000;

    /**
     * Integer value indicating the maximum allowable clock skew,
     * in milliseconds, to be used for the OCSP check.
     */
    private static final int MAX_CLOCK_SKEW = initializeClockSkew();

    /**
     * Initialize the maximum allowable clock skew by getting the OCSP
     * clock skew system property. If the property has not been set, or if its
     * value is negative, set the skew to the default.
     */
    private static int initializeClockSkew() {
        @SuppressWarnings("removal")
        Integer tmp = java.security.AccessController.doPrivileged(
                new GetIntegerAction("com.sun.security.ocsp.clockSkew"));
        if (tmp == null || tmp < 0) {
            return DEFAULT_MAX_CLOCK_SKEW;
        }
        // Convert to milliseconds, as the system property will be
        // specified in seconds
        return tmp * 1000;
    }

    // an array of all of the CRLReasons (used in SingleResponse)
    private static final CRLReason[] values = CRLReason.values();

    private final ResponseStatus responseStatus;
    private final Map<CertId, SingleResponse> singleResponseMap;
    private final AlgorithmId sigAlgId;
    private final byte[] signature;
    private final byte[] tbsResponseData;
    private final byte[] responseNonce;
    private List<X509CertImpl> certs;
    private X509CertImpl signerCert = null;
    private final ResponderId respId;
    private Date producedAtDate = null;
    private final Map<String, java.security.cert.Extension> responseExtensions;

    /*
     * Create an OCSP response from its ASN.1 DER encoding.
     *
     * @param bytes The DER-encoded bytes for an OCSP response
     */
    public OCSPResponse(byte[] bytes) throws IOException {
        if (dump) {
            HexDumpEncoder hexEnc = new HexDumpEncoder();
            debug.println("OCSPResponse bytes...\n\n" +
                hexEnc.encode(bytes) + "\n");
        }
        DerValue der = new DerValue(bytes);
        if (der.tag != DerValue.tag_Sequence) {
            throw new IOException("Bad encoding in OCSP response: " +
                "expected ASN.1 SEQUENCE tag.");
        }
        DerInputStream derIn = der.getData();

        // responseStatus
        int status = derIn.getEnumerated();
        if (status >= 0 && status < rsvalues.length) {
            responseStatus = rsvalues[status];
        } else {
            // unspecified responseStatus
            throw new IOException("Unknown OCSPResponse status: " + status);
        }
        if (debug != null) {
            debug.println("OCSP response status: " + responseStatus);
        }
        if (responseStatus != ResponseStatus.SUCCESSFUL) {
            // no need to continue, responseBytes are not set.
            singleResponseMap = Collections.emptyMap();
            certs = new ArrayList<X509CertImpl>();
            sigAlgId = null;
            signature = null;
            tbsResponseData = null;
            responseNonce = null;
            responseExtensions = Collections.emptyMap();
            respId = null;
            return;
        }

        // responseBytes
        der = derIn.getDerValue();
        if (!der.isContextSpecific((byte)0)) {
            throw new IOException("Bad encoding in responseBytes element " +
                "of OCSP response: expected ASN.1 context specific tag 0.");
        }
        DerValue tmp = der.data.getDerValue();
        if (tmp.tag != DerValue.tag_Sequence) {
            throw new IOException("Bad encoding in responseBytes element " +
                "of OCSP response: expected ASN.1 SEQUENCE tag.");
        }

        // responseType
        derIn = tmp.data;
        ObjectIdentifier responseType = derIn.getOID();
        if (responseType.equals((Object)OCSP_BASIC_RESPONSE_OID)) {
            if (debug != null) {
                debug.println("OCSP response type: basic");
            }
        } else {
            if (debug != null) {
                debug.println("OCSP response type: " + responseType);
            }
            throw new IOException("Unsupported OCSP response type: " +
                                  responseType);
        }

        // BasicOCSPResponse
        DerInputStream basicOCSPResponse =
            new DerInputStream(derIn.getOctetString());

        DerValue[] seqTmp = basicOCSPResponse.getSequence(3);
        if (seqTmp.length < 3) {
            throw new IOException("Unexpected BasicOCSPResponse value");
        }

        DerValue responseData = seqTmp[0];

        // Need the DER encoded ResponseData to verify the signature later
        tbsResponseData = seqTmp[0].toByteArray();

        // tbsResponseData
        if (responseData.tag != DerValue.tag_Sequence) {
            throw new IOException("Bad encoding in tbsResponseData " +
                "element of OCSP response: expected ASN.1 SEQUENCE tag.");
        }
        DerInputStream seqDerIn = responseData.data;
        DerValue seq = seqDerIn.getDerValue();

        // version
        if (seq.isContextSpecific((byte)0)) {
            // seq[0] is version
            if (seq.isConstructed() && seq.isContextSpecific()) {
                //System.out.println ("version is available");
                seq = seq.data.getDerValue();
                int version = seq.getInteger();
                if (seq.data.available() != 0) {
                    throw new IOException("Bad encoding in version " +
                        " element of OCSP response: bad format");
                }
                seq = seqDerIn.getDerValue();
            }
        }

        // responderID
        respId = new ResponderId(seq.toByteArray());
        if (debug != null) {
            debug.println("Responder ID: " + respId);
        }

        // producedAt
        seq = seqDerIn.getDerValue();
        producedAtDate = seq.getGeneralizedTime();
        if (debug != null) {
            debug.println("OCSP response produced at: " + producedAtDate);
        }

        // responses
        DerValue[] singleResponseDer = seqDerIn.getSequence(1);
        singleResponseMap = new HashMap<>(singleResponseDer.length);
        if (debug != null) {
            debug.println("OCSP number of SingleResponses: "
                          + singleResponseDer.length);
        }
        for (DerValue srDer : singleResponseDer) {
            SingleResponse singleResponse = new SingleResponse(srDer);
            singleResponseMap.put(singleResponse.getCertId(), singleResponse);
        }

        // responseExtensions
        Map<String, java.security.cert.Extension> tmpExtMap = new HashMap<>();
        if (seqDerIn.available() > 0) {
            seq = seqDerIn.getDerValue();
            if (seq.isContextSpecific((byte)1)) {
                tmpExtMap = parseExtensions(seq);
            }
        }
        responseExtensions = tmpExtMap;

        // Attach the nonce value if found in the extension map
        Extension nonceExt = (Extension)tmpExtMap.get(
                PKIXExtensions.OCSPNonce_Id.toString());
        responseNonce = (nonceExt != null) ?
                nonceExt.getExtensionValue() : null;
        if (debug != null && responseNonce != null) {
            debug.println("Response nonce: " + Arrays.toString(responseNonce));
        }

        // signatureAlgorithmId
        sigAlgId = AlgorithmId.parse(seqTmp[1]);

        // signature
        signature = seqTmp[2].getBitString();

        // if seq[3] is available , then it is a sequence of certificates
        if (seqTmp.length > 3) {
            // certs are available
            DerValue seqCert = seqTmp[3];
            if (!seqCert.isContextSpecific((byte)0)) {
                throw new IOException("Bad encoding in certs element of " +
                    "OCSP response: expected ASN.1 context specific tag 0.");
            }
            DerValue[] derCerts = seqCert.getData().getSequence(3);
            certs = new ArrayList<X509CertImpl>(derCerts.length);
            try {
                for (int i = 0; i < derCerts.length; i++) {
                    X509CertImpl cert =
                        new X509CertImpl(derCerts[i].toByteArray());
                    certs.add(cert);

                    if (debug != null) {
                        debug.println("OCSP response cert #" + (i + 1) + ": " +
                            cert.getSubjectX500Principal());
                    }
                }
            } catch (CertificateException ce) {
                throw new IOException("Bad encoding in X509 Certificate", ce);
            }
        } else {
            certs = new ArrayList<X509CertImpl>();
        }
    }

    void verify(List<CertId> certIds, IssuerInfo issuerInfo,
            X509Certificate responderCert, Date date, byte[] nonce,
            String variant)
        throws CertPathValidatorException
    {
        switch (responseStatus) {
            case SUCCESSFUL:
                break;
            case TRY_LATER:
            case INTERNAL_ERROR:
                throw new CertPathValidatorException(
                    "OCSP response error: " + responseStatus, null, null, -1,
                    BasicReason.UNDETERMINED_REVOCATION_STATUS);
            case UNAUTHORIZED:
            default:
                throw new CertPathValidatorException("OCSP response error: " +
                                                     responseStatus);
        }

        // Check that the response includes a response for all of the
        // certs that were supplied in the request
        for (CertId certId : certIds) {
            SingleResponse sr = getSingleResponse(certId);
            if (sr == null) {
                if (debug != null) {
                    debug.println("No response found for CertId: " + certId);
                }
                throw new CertPathValidatorException(
                    "OCSP response does not include a response for a " +
                    "certificate supplied in the OCSP request");
            }
            if (debug != null) {
                debug.println("Status of certificate (with serial number " +
                    certId.getSerialNumber() + ") is: " + sr.getCertStatus());
            }
        }

        // Locate the signer cert
        if (signerCert == null) {
            // Add the Issuing CA cert and/or Trusted Responder cert to the list
            // of certs from the OCSP response
            try {
                if (issuerInfo.getCertificate() != null) {
                    certs.add(X509CertImpl.toImpl(issuerInfo.getCertificate()));
                }
                if (responderCert != null) {
                    certs.add(X509CertImpl.toImpl(responderCert));
                }
            } catch (CertificateException ce) {
                throw new CertPathValidatorException(
                    "Invalid issuer or trusted responder certificate", ce);
            }

            if (respId.getType() == ResponderId.Type.BY_NAME) {
                X500Principal rName = respId.getResponderName();
                for (X509CertImpl cert : certs) {
                    if (cert.getSubjectX500Principal().equals(rName)) {
                        signerCert = cert;
                        break;
                    }
                }
            } else if (respId.getType() == ResponderId.Type.BY_KEY) {
                KeyIdentifier ridKeyId = respId.getKeyIdentifier();
                for (X509CertImpl cert : certs) {
                    // Match responder's key identifier against the cert's SKID
                    // This will match if the SKID is encoded using the 160-bit
                    // SHA-1 hash method as defined in RFC 5280.
                    KeyIdentifier certKeyId = cert.getSubjectKeyId();
                    if (certKeyId != null && ridKeyId.equals(certKeyId)) {
                        signerCert = cert;
                        break;
                    } else {
                        // The certificate does not have a SKID or may have
                        // been using a different algorithm (ex: see RFC 7093).
                        // Check if the responder's key identifier matches
                        // against a newly generated key identifier of the
                        // cert's public key using the 160-bit SHA-1 method.
                        try {
                            certKeyId = new KeyIdentifier(cert.getPublicKey());
                        } catch (IOException e) {
                            // ignore
                        }
                        if (ridKeyId.equals(certKeyId)) {
                            signerCert = cert;
                            break;
                        }
                    }
                }
            }
        }

        // Check whether the signer cert returned by the responder is trusted
        boolean signedByTrustedResponder = false;
        if (signerCert != null) {
            // Check if the response is signed by the issuing CA
            if (signerCert.getSubjectX500Principal().equals(
                    issuerInfo.getName()) &&
                    signerCert.getPublicKey().equals(
                            issuerInfo.getPublicKey())) {
                if (debug != null) {
                    debug.println("OCSP response is signed by the target's " +
                        "Issuing CA");
                }
                // cert is trusted, now verify the signed response

            // Check if the response is signed by a trusted responder
            } else if (signerCert.equals(responderCert)) {
                signedByTrustedResponder = true;
                if (debug != null) {
                    debug.println("OCSP response is signed by a Trusted " +
                        "Responder");
                }
                // cert is trusted, now verify the signed response

            // Check if the response is signed by an authorized responder
            } else if (signerCert.getIssuerX500Principal().equals(
                    issuerInfo.getName())) {

                // Check for the OCSPSigning key purpose
                try {
                    List<String> keyPurposes = signerCert.getExtendedKeyUsage();
                    if (keyPurposes == null ||
                        !keyPurposes.contains(KnownOIDs.OCSPSigning.value())) {
                        throw new CertPathValidatorException(
                            "Responder's certificate not valid for signing " +
                            "OCSP responses");
                    }
                } catch (CertificateParsingException cpe) {
                    // assume cert is not valid for signing
                    throw new CertPathValidatorException(
                        "Responder's certificate not valid for signing " +
                        "OCSP responses", cpe);
                }

                // Check algorithm constraints specified in security property
                // "jdk.certpath.disabledAlgorithms".
                AlgorithmChecker algChecker =
                        new AlgorithmChecker(issuerInfo.getAnchor(), date,
                                variant);
                algChecker.init(false);
                algChecker.check(signerCert, Collections.<String>emptySet());

                // check the validity
                try {
                    if (date == null) {
                        signerCert.checkValidity();
                    } else {
                        signerCert.checkValidity(date);
                    }
                } catch (CertificateException e) {
                    throw new CertPathValidatorException(
                        "Responder's certificate not within the " +
                        "validity period", e);
                }

                // check for revocation
                //
                // A CA may specify that an OCSP client can trust a
                // responder for the lifetime of the responder's
                // certificate. The CA does so by including the
                // extension id-pkix-ocsp-nocheck.
                //
                Extension noCheck =
                    signerCert.getExtension(PKIXExtensions.OCSPNoCheck_Id);
                if (noCheck != null) {
                    if (debug != null) {
                        debug.println("Responder's certificate includes " +
                            "the extension id-pkix-ocsp-nocheck.");
                    }
                } else {
                    // we should do the revocation checking of the
                    // authorized responder in a future update.
                }

                // verify the signature
                try {
                    signerCert.verify(issuerInfo.getPublicKey());
                    if (debug != null) {
                        debug.println("OCSP response is signed by an " +
                            "Authorized Responder");
                    }
                    // cert is trusted, now verify the signed response

                } catch (GeneralSecurityException e) {
                    signerCert = null;
                }
            } else {
                throw new CertPathValidatorException(
                    "Responder's certificate is not authorized to sign " +
                    "OCSP responses");
            }
        }

        // Confirm that the signed response was generated using the public
        // key from the trusted responder cert
        if (signerCert != null) {
            // Check algorithm constraints specified in security property
            // "jdk.certpath.disabledAlgorithms".
            AlgorithmChecker.check(signerCert.getPublicKey(), sigAlgId, variant,
                    signedByTrustedResponder
                        ? new TrustAnchor(responderCert, null)
                        : issuerInfo.getAnchor());

            if (!verifySignature(signerCert)) {
                throw new CertPathValidatorException(
                    "Error verifying OCSP Response's signature");
            }
        } else {
            // Need responder's cert in order to verify the signature
            throw new CertPathValidatorException(
                "Unable to verify OCSP Response's signature");
        }

        if (nonce != null) {
            if (responseNonce != null && !Arrays.equals(nonce, responseNonce)) {
                throw new CertPathValidatorException("Nonces don't match");
            }
        }

        // Check freshness of OCSPResponse
        long now = (date == null) ? System.currentTimeMillis() : date.getTime();
        Date nowPlusSkew = new Date(now + MAX_CLOCK_SKEW);
        Date nowMinusSkew = new Date(now - MAX_CLOCK_SKEW);
        for (SingleResponse sr : singleResponseMap.values()) {
            if (debug != null) {
                String until = "";
                if (sr.nextUpdate != null) {
                    until = " until " + sr.nextUpdate;
                }
                debug.println("OCSP response validity interval is from " +
                        sr.thisUpdate + until);
                debug.println("Checking validity of OCSP response on " +
                        new Date(now) + " with allowed interval between " +
                        nowMinusSkew + " and " + nowPlusSkew);
            }

            // Check that the test date is within the validity interval:
            //   [ thisUpdate - MAX_CLOCK_SKEW,
            //     MAX(thisUpdate, nextUpdate) + MAX_CLOCK_SKEW ]
            if (nowPlusSkew.before(sr.thisUpdate) ||
                    nowMinusSkew.after(
                    sr.nextUpdate != null ? sr.nextUpdate : sr.thisUpdate))
            {
                throw new CertPathValidatorException(
                                      "Response is unreliable: its validity " +
                                      "interval is out-of-date");
            }
        }
    }

    /**
     * Returns the OCSP ResponseStatus.
     *
     * @return the {@code ResponseStatus} for this OCSP response
     */
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    /*
     * Verify the signature of the OCSP response.
     */
    private boolean verifySignature(X509Certificate cert)
        throws CertPathValidatorException {

        try {
            Signature respSignature = Signature.getInstance(sigAlgId.getName());
            respSignature.initVerify(cert.getPublicKey());
            respSignature.update(tbsResponseData);

            if (respSignature.verify(signature)) {
                if (debug != null) {
                    debug.println("Verified signature of OCSP Response");
                }
                return true;

            } else {
                if (debug != null) {
                    debug.println(
                        "Error verifying signature of OCSP Response");
                }
                return false;
            }
        } catch (InvalidKeyException | NoSuchAlgorithmException |
                 SignatureException e)
        {
            throw new CertPathValidatorException(e);
        }
    }

    /**
     * Returns the SingleResponse of the specified CertId, or null if
     * there is no response for that CertId.
     *
     * @param certId the {@code CertId} for a {@code SingleResponse} to be
     * searched for in the OCSP response.
     *
     * @return the {@code SingleResponse} for the provided {@code CertId},
     * or {@code null} if it is not found.
     */
    public SingleResponse getSingleResponse(CertId certId) {
        return singleResponseMap.get(certId);
    }

    /**
     * Return a set of all CertIds in this {@code OCSPResponse}
     *
     * @return an unmodifiable set containing every {@code CertId} in this
     *      response.
     */
    public Set<CertId> getCertIds() {
        return Collections.unmodifiableSet(singleResponseMap.keySet());
    }

    /*
     * Returns the certificate for the authority that signed the OCSP response.
     */
    X509Certificate getSignerCertificate() {
        return signerCert; // set in verify()
    }

    /**
     * Get the {@code ResponderId} from this {@code OCSPResponse}
     *
     * @return the {@code ResponderId} from this response or {@code null}
     *      if no responder ID is in the body of the response (e.g. a
     *      response with a status other than SUCCESS.
     */
    public ResponderId getResponderId() {
        return respId;
    }

    /**
     * Provide a String representation of an OCSPResponse
     *
     * @return a human-readable representation of the OCSPResponse
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OCSP Response:\n");
        sb.append("Response Status: ").append(responseStatus).append("\n");
        sb.append("Responder ID: ").append(respId).append("\n");
        sb.append("Produced at: ").append(producedAtDate).append("\n");
        int count = singleResponseMap.size();
        sb.append(count).append(count == 1 ?
                " response:\n" : " responses:\n");
        for (SingleResponse sr : singleResponseMap.values()) {
            sb.append(sr).append("\n");
        }
        if (responseExtensions != null && responseExtensions.size() > 0) {
            count = responseExtensions.size();
            sb.append(count).append(count == 1 ?
                    " extension:\n" : " extensions:\n");
            for (String extId : responseExtensions.keySet()) {
                sb.append(responseExtensions.get(extId)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Build a String-Extension map from DER encoded data.
     * @param derVal A {@code DerValue} object built from a SEQUENCE of
     *      extensions
     *
     * @return a {@code Map} using the OID in string form as the keys.  If no
     *      extensions are found or an empty SEQUENCE is passed in, then
     *      an empty {@code Map} will be returned.
     *
     * @throws IOException if any decoding errors occur.
     */
    private static Map<String, java.security.cert.Extension>
        parseExtensions(DerValue derVal) throws IOException {
        DerValue[] extDer = derVal.data.getSequence(3);
        Map<String, java.security.cert.Extension> extMap =
                new HashMap<>(extDer.length);

        for (DerValue extDerVal : extDer) {
            Extension ext = new Extension(extDerVal);
            if (debug != null) {
                debug.println("Extension: " + ext);
            }
            // We don't support any extensions yet. Therefore, if it
            // is critical we must throw an exception because we
            // don't know how to process it.
            if (ext.isCritical()) {
                throw new IOException("Unsupported OCSP critical extension: " +
                        ext.getExtensionId());
            }
            extMap.put(ext.getId(), ext);
        }

        return extMap;
    }

    /*
     * A class representing a single OCSP response.
     */
    public static final class SingleResponse implements OCSP.RevocationStatus {
        private final CertId certId;
        private final CertStatus certStatus;
        private final Date thisUpdate;
        private final Date nextUpdate;
        private final Date revocationTime;
        private final CRLReason revocationReason;
        private final Map<String, java.security.cert.Extension> singleExtensions;

        private SingleResponse(DerValue der) throws IOException {
            if (der.tag != DerValue.tag_Sequence) {
                throw new IOException("Bad ASN.1 encoding in SingleResponse");
            }
            DerInputStream tmp = der.data;

            certId = new CertId(tmp.getDerValue().data);
            DerValue derVal = tmp.getDerValue();
            short tag = (byte)(derVal.tag & 0x1f);
            if (tag ==  CERT_STATUS_REVOKED) {
                certStatus = CertStatus.REVOKED;
                revocationTime = derVal.data.getGeneralizedTime();
                if (derVal.data.available() != 0) {
                    DerValue dv = derVal.data.getDerValue();
                    tag = (byte)(dv.tag & 0x1f);
                    if (tag == 0) {
                        int reason = dv.data.getEnumerated();
                        // if reason out-of-range just leave as UNSPECIFIED
                        if (reason >= 0 && reason < values.length) {
                            revocationReason = values[reason];
                        } else {
                            revocationReason = CRLReason.UNSPECIFIED;
                        }
                    } else {
                        revocationReason = CRLReason.UNSPECIFIED;
                    }
                } else {
                    revocationReason = CRLReason.UNSPECIFIED;
                }
                // RevokedInfo
                if (debug != null) {
                    debug.println("Revocation time: " + revocationTime);
                    debug.println("Revocation reason: " + revocationReason);
                }
            } else {
                revocationTime = null;
                revocationReason = null;
                if (tag == CERT_STATUS_GOOD) {
                    certStatus = CertStatus.GOOD;
                } else if (tag == CERT_STATUS_UNKNOWN) {
                    certStatus = CertStatus.UNKNOWN;
                } else {
                    throw new IOException("Invalid certificate status");
                }
            }

            thisUpdate = tmp.getGeneralizedTime();
            if (debug != null) {
                debug.println("thisUpdate: " + thisUpdate);
            }

            // Parse optional fields like nextUpdate and singleExtensions
            Date tmpNextUpdate = null;
            Map<String, java.security.cert.Extension> tmpMap = null;

            // Check for the first optional item, it could be nextUpdate
            // [CONTEXT 0] or singleExtensions [CONTEXT 1]
            if (tmp.available() > 0) {
                derVal = tmp.getDerValue();

                // nextUpdate processing
                if (derVal.isContextSpecific((byte)0)) {
                    tmpNextUpdate = derVal.data.getGeneralizedTime();
                    if (debug != null) {
                        debug.println("nextUpdate: " + tmpNextUpdate);
                    }

                    // If more data exists in the singleResponse, it
                    // can only be singleExtensions.  Get this DER value
                    // for processing in the next block
                    derVal = tmp.available() > 0 ? tmp.getDerValue() : null;
                }

                // singleExtensions processing
                if (derVal != null) {
                    if (derVal.isContextSpecific((byte)1)) {
                        tmpMap = parseExtensions(derVal);

                        // There should not be any other items in the
                        // singleResponse at this point.
                        if (tmp.available() > 0) {
                            throw new IOException(tmp.available() +
                                " bytes of additional data in singleResponse");
                        }
                    } else {
                        // Unknown item in the singleResponse
                        throw new IOException("Unsupported singleResponse " +
                            "item, tag = " + String.format("%02X", derVal.tag));
                    }
                }
            }

            nextUpdate = tmpNextUpdate;
            singleExtensions = (tmpMap != null) ? tmpMap :
                    Collections.emptyMap();
            if (debug != null) {
                for (java.security.cert.Extension ext :
                        singleExtensions.values()) {
                   debug.println("singleExtension: " + ext);
                }
            }
        }

        /*
         * Return the certificate's revocation status code
         */
        @Override
        public CertStatus getCertStatus() {
            return certStatus;
        }

        /**
         * Get the Cert ID that this SingleResponse is for.
         *
         * @return the {@code CertId} for this {@code SingleResponse}
         */
        public CertId getCertId() {
            return certId;
        }

        /**
         * Get the {@code thisUpdate} field from this {@code SingleResponse}.
         *
         * @return a {@link Date} object containing the thisUpdate date
         */
        public Date getThisUpdate() {
            return (thisUpdate != null ? (Date) thisUpdate.clone() : null);
        }

        /**
         * Get the {@code nextUpdate} field from this {@code SingleResponse}.
         *
         * @return a {@link Date} object containing the nexUpdate date or
         * {@code null} if a nextUpdate field is not present in the response.
         */
        public Date getNextUpdate() {
            return (nextUpdate != null ? (Date) nextUpdate.clone() : null);
        }

        /**
         * Get the {@code revocationTime} field from this
         * {@code SingleResponse}.
         *
         * @return a {@link Date} object containing the revocationTime date or
         * {@code null} if the {@code SingleResponse} does not have a status
         * of {@code REVOKED}.
         */
        @Override
        public Date getRevocationTime() {
            return (revocationTime != null ? (Date) revocationTime.clone() :
                    null);
        }

        /**
         * Get the {@code revocationReason} field for the
         * {@code SingleResponse}.
         *
         * @return a {@link CRLReason} containing the revocation reason, or
         * {@code null} if a revocation reason was not provided or the
         * response status is not {@code REVOKED}.
         */
        @Override
        public CRLReason getRevocationReason() {
            return revocationReason;
        }

        /**
         * Get the {@code singleExtensions} for this {@code SingleResponse}.
         *
         * @return a {@link Map} of {@link Extension} objects, keyed by
         * their OID value in string form.
         */
        @Override
        public Map<String, java.security.cert.Extension> getSingleExtensions() {
            return Collections.unmodifiableMap(singleExtensions);
        }

        /**
         * Construct a string representation of a single OCSP response.
         */
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("SingleResponse:\n");
            sb.append(certId);
            sb.append("\nCertStatus: ").append(certStatus).append("\n");
            if (certStatus == CertStatus.REVOKED) {
                sb.append("revocationTime is ");
                sb.append(revocationTime).append("\n");
                sb.append("revocationReason is ");
                sb.append(revocationReason).append("\n");
            }
            sb.append("thisUpdate is ").append(thisUpdate).append("\n");
            if (nextUpdate != null) {
                sb.append("nextUpdate is ").append(nextUpdate).append("\n");
            }
            for (java.security.cert.Extension ext : singleExtensions.values()) {
                sb.append("singleExtension: ");
                sb.append(ext.toString()).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Helper class that allows consumers to pass in issuer information.  This
     * will always consist of the issuer's name and public key, but may also
     * contain a certificate if the originating data is in that form.  The
     * trust anchor for the certificate chain will be included for certpath
     * disabled algorithm checking.
     */
    static final class IssuerInfo {
        private final TrustAnchor anchor;
        private final X509Certificate certificate;
        private final X500Principal name;
        private final PublicKey pubKey;

        IssuerInfo(TrustAnchor anchor) {
            this(anchor, (anchor != null) ? anchor.getTrustedCert() : null);
        }

        IssuerInfo(X509Certificate issuerCert) {
            this(null, issuerCert);
        }

        IssuerInfo(TrustAnchor anchor, X509Certificate issuerCert) {
            if (anchor == null && issuerCert == null) {
                throw new NullPointerException("TrustAnchor and issuerCert " +
                        "cannot be null");
            }
            this.anchor = anchor;
            if (issuerCert != null) {
                name = issuerCert.getSubjectX500Principal();
                pubKey = issuerCert.getPublicKey();
                certificate = issuerCert;
            } else {
                name = anchor.getCA();
                pubKey = anchor.getCAPublicKey();
                certificate = anchor.getTrustedCert();
            }
        }

        /**
         * Get the certificate in this IssuerInfo if present.
         *
         * @return the {@code X509Certificate} used to create this IssuerInfo
         * object, or {@code null} if a certificate was not used in its
         * creation.
         */
        X509Certificate getCertificate() {
            return certificate;
        }

        /**
         * Get the name of this issuer.
         *
         * @return an {@code X500Principal} corresponding to this issuer's
         * name.  If derived from an issuer's {@code X509Certificate} this
         * would be equivalent to the certificate subject name.
         */
        X500Principal getName() {
            return name;
        }

        /**
         * Get the public key for this issuer.
         *
         * @return a {@code PublicKey} for this issuer.
         */
        PublicKey getPublicKey() {
            return pubKey;
        }

        /**
         * Get the TrustAnchor for the certificate chain.
         *
         * @return a {@code TrustAnchor}.
         */
        TrustAnchor getAnchor() {
            return anchor;
        }

        /**
         * Create a string representation of this IssuerInfo.
         *
         * @return a {@code String} form of this IssuerInfo object.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Issuer Info:\n");
            sb.append("Name: ").append(name.toString()).append("\n");
            sb.append("Public Key:\n").append(pubKey.toString()).append("\n");
            return sb.toString();
        }
    }
}
