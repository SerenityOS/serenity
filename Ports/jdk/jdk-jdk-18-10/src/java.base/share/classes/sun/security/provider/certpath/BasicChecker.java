/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.X500Name;
import sun.security.util.Debug;

/**
 * BasicChecker is a PKIXCertPathChecker that checks the basic information
 * on a PKIX certificate, namely the signature, validity, and subject/issuer
 * name chaining.
 *
 * @since       1.4
 * @author      Yassir Elley
 */
class BasicChecker extends PKIXCertPathChecker {

    private static final Debug debug = Debug.getInstance("certpath");
    private final PublicKey trustedPubKey;
    private final X500Principal caName;
    private final Date date;
    private final String sigProvider;
    private final boolean sigOnly;
    private X500Principal prevSubject;
    private PublicKey prevPubKey;

    /**
     * Constructor that initializes the input parameters.
     *
     * @param anchor the anchor selected to validate the target certificate
     * @param date the time for which the validity of the certificate
     *        should be determined
     * @param sigProvider the name of the signature provider
     * @param sigOnly true if only signature checking is to be done;
     *        if false, all checks are done
     */
    BasicChecker(TrustAnchor anchor, Date date, String sigProvider,
                 boolean sigOnly) {
        if (anchor.getTrustedCert() != null) {
            this.trustedPubKey = anchor.getTrustedCert().getPublicKey();
            this.caName = anchor.getTrustedCert().getSubjectX500Principal();
        } else {
            this.trustedPubKey = anchor.getCAPublicKey();
            this.caName = anchor.getCA();
        }
        this.date = date;
        this.sigProvider = sigProvider;
        this.sigOnly = sigOnly;
        this.prevPubKey = trustedPubKey;
    }

    /**
     * Initializes the internal state of the checker from parameters
     * specified in the constructor.
     */
    @Override
    public void init(boolean forward) throws CertPathValidatorException {
        if (!forward) {
            prevPubKey = trustedPubKey;
            if (PKIX.isDSAPublicKeyWithoutParams(prevPubKey)) {
                // If TrustAnchor is a DSA public key and it has no params, it
                // cannot be used to verify the signature of the first cert,
                // so throw exception
                throw new CertPathValidatorException("Key parameters missing");
            }
            prevSubject = caName;
        } else {
            throw new
                CertPathValidatorException("forward checking not supported");
        }
    }

    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return null;
    }

    /**
     * Performs the signature, validity, and subject/issuer name chaining
     * checks on the certificate using its internal state. This method does
     * not remove any critical extensions from the Collection.
     *
     * @param cert the Certificate
     * @param unresolvedCritExts a Collection of the unresolved critical
     * extensions
     * @throws CertPathValidatorException if certificate does not verify
     */
    @Override
    public void check(Certificate cert, Collection<String> unresolvedCritExts)
        throws CertPathValidatorException
    {
        X509Certificate currCert = (X509Certificate)cert;

        if (!sigOnly) {
            verifyValidity(currCert);
            verifyNameChaining(currCert);
        }
        verifySignature(currCert);

        updateState(currCert);
    }

    /**
     * Verifies the signature on the certificate using the previous public key.
     *
     * @param cert the X509Certificate
     * @throws CertPathValidatorException if certificate does not verify
     */
    private void verifySignature(X509Certificate cert)
        throws CertPathValidatorException
    {
        String msg = "signature";
        if (debug != null)
            debug.println("---checking " + msg + "...");

        try {
            cert.verify(prevPubKey, sigProvider);
        } catch (SignatureException e) {
            throw new CertPathValidatorException
                (msg + " check failed", e, null, -1,
                 BasicReason.INVALID_SIGNATURE);
        } catch (GeneralSecurityException e) {
            throw new CertPathValidatorException(msg + " check failed", e);
        }

        if (debug != null)
            debug.println(msg + " verified.");
    }

    /**
     * Internal method to verify the validity on a certificate
     */
    private void verifyValidity(X509Certificate cert)
        throws CertPathValidatorException
    {
        String msg = "validity";
        if (debug != null)
            debug.println("---checking " + msg + ":" + date.toString() + "...");

        try {
            cert.checkValidity(date);
        } catch (CertificateExpiredException e) {
            throw new CertPathValidatorException
                (msg + " check failed", e, null, -1, BasicReason.EXPIRED);
        } catch (CertificateNotYetValidException e) {
            throw new CertPathValidatorException
                (msg + " check failed", e, null, -1, BasicReason.NOT_YET_VALID);
        }

        if (debug != null)
            debug.println(msg + " verified.");
    }

    /**
     * Internal method to check that cert has a valid DN to be next in a chain
     */
    private void verifyNameChaining(X509Certificate cert)
        throws CertPathValidatorException
    {
        if (prevSubject != null) {

            String msg = "subject/issuer name chaining";
            if (debug != null)
                debug.println("---checking " + msg + "...");

            X500Principal currIssuer = cert.getIssuerX500Principal();

            // reject null or empty issuer DNs
            if (X500Name.asX500Name(currIssuer).isEmpty()) {
                throw new CertPathValidatorException
                    (msg + " check failed: " +
                     "empty/null issuer DN in certificate is invalid", null,
                     null, -1, PKIXReason.NAME_CHAINING);
            }

            if (!(currIssuer.equals(prevSubject))) {
                throw new CertPathValidatorException
                    (msg + " check failed", null, null, -1,
                     PKIXReason.NAME_CHAINING);
            }

            if (debug != null)
                debug.println(msg + " verified.");
        }
    }

    /**
     * Internal method to manage state information at each iteration
     */
    private void updateState(X509Certificate currCert)
        throws CertPathValidatorException
    {
        PublicKey cKey = currCert.getPublicKey();
        if (debug != null) {
            debug.println("BasicChecker.updateState issuer: " +
                currCert.getIssuerX500Principal().toString() + "; subject: " +
                currCert.getSubjectX500Principal() + "; serial#: " +
                currCert.getSerialNumber().toString());
        }
        if (PKIX.isDSAPublicKeyWithoutParams(cKey)) {
            // cKey needs to inherit DSA parameters from prev key
            cKey = makeInheritedParamsKey(cKey, prevPubKey);
            if (debug != null) debug.println("BasicChecker.updateState Made " +
                                             "key with inherited params");
        }
        prevPubKey = cKey;
        prevSubject = currCert.getSubjectX500Principal();
    }

    /**
     * Internal method to create a new key with inherited key parameters.
     *
     * @param keyValueKey key from which to obtain key value
     * @param keyParamsKey key from which to obtain key parameters
     * @return new public key having value and parameters
     * @throws CertPathValidatorException if keys are not appropriate types
     * for this operation
     */
    static PublicKey makeInheritedParamsKey(PublicKey keyValueKey,
        PublicKey keyParamsKey) throws CertPathValidatorException
    {
        if (!(keyValueKey instanceof DSAPublicKey) ||
            !(keyParamsKey instanceof DSAPublicKey))
            throw new CertPathValidatorException("Input key is not " +
                                                 "appropriate type for " +
                                                 "inheriting parameters");
        DSAParams params = ((DSAPublicKey)keyParamsKey).getParams();
        if (params == null)
            throw new CertPathValidatorException("Key parameters missing");
        try {
            BigInteger y = ((DSAPublicKey)keyValueKey).getY();
            KeyFactory kf = KeyFactory.getInstance("DSA");
            DSAPublicKeySpec ks = new DSAPublicKeySpec(y,
                                                       params.getP(),
                                                       params.getQ(),
                                                       params.getG());
            return kf.generatePublic(ks);
        } catch (GeneralSecurityException e) {
            throw new CertPathValidatorException("Unable to generate key with" +
                                                 " inherited parameters: " +
                                                 e.getMessage(), e);
        }
    }

    /**
     * return the public key associated with the last certificate processed
     *
     * @return PublicKey the last public key processed
     */
    PublicKey getPublicKey() {
        return prevPubKey;
    }
}
