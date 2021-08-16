/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.security.*;
import java.security.spec.*;

import javax.security.auth.x500.X500Principal;

import java.math.BigInteger;
import java.util.Date;
import java.util.Set;
import java.util.Arrays;

import sun.security.x509.X509CRLImpl;
import sun.security.util.SignatureUtil;

/**
 * <p>
 * Abstract class for an X.509 Certificate Revocation List (CRL).
 * A CRL is a time-stamped list identifying revoked certificates.
 * It is signed by a Certificate Authority (CA) and made freely
 * available in a public repository.
 *
 * <p>Each revoked certificate is
 * identified in a CRL by its certificate serial number. When a
 * certificate-using system uses a certificate (e.g., for verifying a
 * remote user's digital signature), that system not only checks the
 * certificate signature and validity but also acquires a suitably-
 * recent CRL and checks that the certificate serial number is not on
 * that CRL.  The meaning of "suitably-recent" may vary with local
 * policy, but it usually means the most recently-issued CRL.  A CA
 * issues a new CRL on a regular periodic basis (e.g., hourly, daily, or
 * weekly).  Entries are added to CRLs as revocations occur, and an
 * entry may be removed when the certificate expiration date is reached.
 * <p>
 * The X.509 v2 CRL format is described below in ASN.1:
 * <pre>
 * CertificateList  ::=  SEQUENCE  {
 *     tbsCertList          TBSCertList,
 *     signatureAlgorithm   AlgorithmIdentifier,
 *     signature            BIT STRING  }
 * </pre>
 * <p>
 * More information can be found in
 * <a href="http://tools.ietf.org/html/rfc5280">RFC 5280: Internet X.509
 * Public Key Infrastructure Certificate and CRL Profile</a>.
 * <p>
 * The ASN.1 definition of {@code tbsCertList} is:
 * <pre>
 * TBSCertList  ::=  SEQUENCE  {
 *     version                 Version OPTIONAL,
 *                             -- if present, must be v2
 *     signature               AlgorithmIdentifier,
 *     issuer                  Name,
 *     thisUpdate              ChoiceOfTime,
 *     nextUpdate              ChoiceOfTime OPTIONAL,
 *     revokedCertificates     SEQUENCE OF SEQUENCE  {
 *         userCertificate         CertificateSerialNumber,
 *         revocationDate          ChoiceOfTime,
 *         crlEntryExtensions      Extensions OPTIONAL
 *                                 -- if present, must be v2
 *         }  OPTIONAL,
 *     crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                  -- if present, must be v2
 *     }
 * </pre>
 * <p>
 * CRLs are instantiated using a certificate factory. The following is an
 * example of how to instantiate an X.509 CRL:
 * <pre>{@code
 * try (InputStream inStream = new FileInputStream("fileName-of-crl")) {
 *     CertificateFactory cf = CertificateFactory.getInstance("X.509");
 *     X509CRL crl = (X509CRL)cf.generateCRL(inStream);
 * }
 * }</pre>
 *
 * @author Hemma Prafullchandra
 * @since 1.2
 *
 *
 * @see CRL
 * @see CertificateFactory
 * @see X509Extension
 */

public abstract class X509CRL extends CRL implements X509Extension {

    private transient X500Principal issuerPrincipal;

    /**
     * Constructor for X.509 CRLs.
     */
    protected X509CRL() {
        super("X.509");
    }

    /**
     * Compares this CRL for equality with the given
     * object. If the {@code other} object is an
     * {@code instanceof} {@code X509CRL}, then
     * its encoded form is retrieved and compared with the
     * encoded form of this CRL.
     *
     * @param other the object to test for equality with this CRL.
     *
     * @return true iff the encoded forms of the two CRLs
     * match, false otherwise.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof X509CRL)) {
            return false;
        }
        try {
            byte[] thisCRL = X509CRLImpl.getEncodedInternal(this);
            byte[] otherCRL = X509CRLImpl.getEncodedInternal((X509CRL)other);

            return Arrays.equals(thisCRL, otherCRL);
        } catch (CRLException e) {
            return false;
        }
    }

    /**
     * Returns a hashcode value for this CRL from its
     * encoded form.
     *
     * @return the hashcode value.
     */
    public int hashCode() {
        int retval = 0;
        try {
            byte[] crlData = X509CRLImpl.getEncodedInternal(this);
            for (int i = 1; i < crlData.length; i++) {
                 retval += crlData[i] * i;
            }
            return retval;
        } catch (CRLException e) {
            return retval;
        }
    }

    /**
     * Returns the ASN.1 DER-encoded form of this CRL.
     *
     * @return the encoded form of this certificate
     * @throws    CRLException if an encoding error occurs.
     */
    public abstract byte[] getEncoded()
        throws CRLException;

    /**
     * Verifies that this CRL was signed using the
     * private key that corresponds to the given public key.
     *
     * @param key the PublicKey used to carry out the verification.
     *
     * @throws    NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @throws    InvalidKeyException on incorrect key.
     * @throws    NoSuchProviderException if there's no default provider.
     * @throws    SignatureException on signature errors.
     * @throws    CRLException on encoding errors.
     */
    public abstract void verify(PublicKey key)
        throws CRLException,  NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException,
        SignatureException;

    /**
     * Verifies that this CRL was signed using the
     * private key that corresponds to the given public key.
     * This method uses the signature verification engine
     * supplied by the given provider.
     *
     * @param key the PublicKey used to carry out the verification.
     * @param sigProvider the name of the signature provider.
     *
     * @throws    NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @throws    InvalidKeyException on incorrect key.
     * @throws    NoSuchProviderException on incorrect provider.
     * @throws    SignatureException on signature errors.
     * @throws    CRLException on encoding errors.
     */
    public abstract void verify(PublicKey key, String sigProvider)
        throws CRLException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException,
        SignatureException;

    /**
     * Verifies that this CRL was signed using the
     * private key that corresponds to the given public key.
     * This method uses the signature verification engine
     * supplied by the given provider. Note that the specified Provider object
     * does not have to be registered in the provider list.
     *
     * This method was added to version 1.8 of the Java Platform Standard
     * Edition. In order to maintain backwards compatibility with existing
     * service providers, this method is not {@code abstract}
     * and it provides a default implementation.
     *
     * @param key the PublicKey used to carry out the verification.
     * @param sigProvider the signature provider.
     *
     * @throws    NoSuchAlgorithmException on unsupported signature
     * algorithms.
     * @throws    InvalidKeyException on incorrect key.
     * @throws    SignatureException on signature errors.
     * @throws    CRLException on encoding errors.
     * @since 1.8
     */
    public void verify(PublicKey key, Provider sigProvider)
        throws CRLException, NoSuchAlgorithmException,
        InvalidKeyException, SignatureException {
        String sigAlgName = getSigAlgName();
        Signature sig = (sigProvider == null)
            ? Signature.getInstance(sigAlgName)
            : Signature.getInstance(sigAlgName, sigProvider);

        try {
            byte[] paramBytes = getSigAlgParams();
            SignatureUtil.initVerifyWithParam(sig, key,
                SignatureUtil.getParamSpec(sigAlgName, paramBytes));
        } catch (ProviderException e) {
            throw new CRLException(e.getMessage(), e.getCause());
        } catch (InvalidAlgorithmParameterException e) {
            throw new CRLException(e);
        }

        byte[] tbsCRL = getTBSCertList();
        sig.update(tbsCRL, 0, tbsCRL.length);

        if (sig.verify(getSignature()) == false) {
            throw new SignatureException("Signature does not match.");
        }
    }

    /**
     * Gets the {@code version} (version number) value from the CRL.
     * The ASN.1 definition for this is:
     * <pre>
     * version    Version OPTIONAL,
     *             -- if present, must be v2
     *
     * Version  ::=  INTEGER  {  v1(0), v2(1), v3(2)  }
     *             -- v3 does not apply to CRLs but appears for consistency
     *             -- with definition of Version for certs
     * </pre>
     *
     * @return the version number, i.e. 1 or 2.
     */
    public abstract int getVersion();

    /**
     * Gets the {@code issuer} (issuer distinguished name) value from
     * the CRL. The issuer name identifies the entity that signed (and
     * issued) the CRL.
     *
     * <p>The issuer name field contains an
     * X.500 distinguished name (DN).
     * The ASN.1 definition for this is:
     * <pre>
     * issuer    Name
     *
     * Name ::= CHOICE { RDNSequence }
     * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
     * RelativeDistinguishedName ::=
     *     SET OF AttributeValueAssertion
     *
     * AttributeValueAssertion ::= SEQUENCE {
     *                               AttributeType,
     *                               AttributeValue }
     * AttributeType ::= OBJECT IDENTIFIER
     * AttributeValue ::= ANY
     * </pre>
     * The {@code Name} describes a hierarchical name composed of
     * attributes,
     * such as country name, and corresponding values, such as US.
     * The type of the {@code AttributeValue} component is determined by
     * the {@code AttributeType}; in general it will be a
     * {@code directoryString}. A {@code directoryString} is usually
     * one of {@code PrintableString},
     * {@code TeletexString} or {@code UniversalString}.
     *
     * @return a Principal whose name is the issuer distinguished name.
     *
     * @deprecated Use {@link #getIssuerX500Principal} instead. This method
     * returns the {@code issuer} as an implementation specific
     * {@code Principal} object, which should not be relied upon by portable
     * code.
     */
    @Deprecated(since="16")
    public abstract Principal getIssuerDN();

    /**
     * Returns the issuer (issuer distinguished name) value from the
     * CRL as an {@code X500Principal}.
     * <p>
     * It is recommended that subclasses override this method.
     *
     * @return an {@code X500Principal} representing the issuer
     *          distinguished name
     * @since 1.4
     */
    public X500Principal getIssuerX500Principal() {
        if (issuerPrincipal == null) {
            issuerPrincipal = X509CRLImpl.getIssuerX500Principal(this);
        }
        return issuerPrincipal;
    }

    /**
     * Gets the {@code thisUpdate} date from the CRL.
     * The ASN.1 definition for this is:
     * <pre>
     * thisUpdate   ChoiceOfTime
     * ChoiceOfTime ::= CHOICE {
     *     utcTime        UTCTime,
     *     generalTime    GeneralizedTime }
     * </pre>
     *
     * @return the {@code thisUpdate} date from the CRL.
     */
    public abstract Date getThisUpdate();

    /**
     * Gets the {@code nextUpdate} date from the CRL.
     *
     * @return the {@code nextUpdate} date from the CRL, or null if
     * not present.
     */
    public abstract Date getNextUpdate();

    /**
     * Gets the CRL entry, if any, with the given certificate serialNumber.
     *
     * @param serialNumber the serial number of the certificate for which a CRL entry
     * is to be looked up
     * @return the entry with the given serial number, or null if no such entry
     * exists in this CRL.
     * @see X509CRLEntry
     */
    public abstract X509CRLEntry
        getRevokedCertificate(BigInteger serialNumber);

    /**
     * Get the CRL entry, if any, for the given certificate.
     *
     * <p>This method can be used to lookup CRL entries in indirect CRLs,
     * that means CRLs that contain entries from issuers other than the CRL
     * issuer. The default implementation will only return entries for
     * certificates issued by the CRL issuer. Subclasses that wish to
     * support indirect CRLs should override this method.
     *
     * @param certificate the certificate for which a CRL entry is to be looked
     *   up
     * @return the entry for the given certificate, or null if no such entry
     *   exists in this CRL.
     * @throws    NullPointerException if certificate is null
     *
     * @since 1.5
     */
    public X509CRLEntry getRevokedCertificate(X509Certificate certificate) {
        X500Principal certIssuer = certificate.getIssuerX500Principal();
        X500Principal crlIssuer = getIssuerX500Principal();
        if (certIssuer.equals(crlIssuer) == false) {
            return null;
        }
        return getRevokedCertificate(certificate.getSerialNumber());
    }

    /**
     * Gets all the entries from this CRL.
     * This returns a Set of X509CRLEntry objects.
     *
     * @return all the entries or null if there are none present.
     * @see X509CRLEntry
     */
    public abstract Set<? extends X509CRLEntry> getRevokedCertificates();

    /**
     * Gets the DER-encoded CRL information, the
     * {@code tbsCertList} from this CRL.
     * This can be used to verify the signature independently.
     *
     * @return the DER-encoded CRL information.
     * @throws    CRLException if an encoding error occurs.
     */
    public abstract byte[] getTBSCertList() throws CRLException;

    /**
     * Gets the {@code signature} value (the raw signature bits) from
     * the CRL.
     * The ASN.1 definition for this is:
     * <pre>
     * signature     BIT STRING
     * </pre>
     *
     * @return the signature.
     */
    public abstract byte[] getSignature();

    /**
     * Gets the signature algorithm name for the CRL
     * signature algorithm. An example is the string "SHA256withRSA".
     * The ASN.1 definition for this is:
     * <pre>
     * signatureAlgorithm   AlgorithmIdentifier
     *
     * AlgorithmIdentifier  ::=  SEQUENCE  {
     *     algorithm               OBJECT IDENTIFIER,
     *     parameters              ANY DEFINED BY algorithm OPTIONAL  }
     *                             -- contains a value of the type
     *                             -- registered for use with the
     *                             -- algorithm object identifier value
     * </pre>
     *
     * <p>The algorithm name is determined from the {@code algorithm}
     * OID string.
     *
     * @return the signature algorithm name.
     */
    public abstract String getSigAlgName();

    /**
     * Gets the signature algorithm OID string from the CRL.
     * An OID is represented by a set of nonnegative whole numbers separated
     * by periods.
     * For example, the string "1.2.840.10040.4.3" identifies the SHA-1
     * with DSA signature algorithm defined in
     * <a href="http://www.ietf.org/rfc/rfc3279.txt">RFC 3279: Algorithms and
     * Identifiers for the Internet X.509 Public Key Infrastructure Certificate
     * and CRL Profile</a>.
     *
     * <p>See {@link #getSigAlgName() getSigAlgName} for
     * relevant ASN.1 definitions.
     *
     * @return the signature algorithm OID string.
     */
    public abstract String getSigAlgOID();

    /**
     * Gets the DER-encoded signature algorithm parameters from this
     * CRL's signature algorithm. In most cases, the signature
     * algorithm parameters are null; the parameters are usually
     * supplied with the public key.
     * If access to individual parameter values is needed then use
     * {@link java.security.AlgorithmParameters AlgorithmParameters}
     * and instantiate with the name returned by
     * {@link #getSigAlgName() getSigAlgName}.
     *
     * <p>See {@link #getSigAlgName() getSigAlgName} for
     * relevant ASN.1 definitions.
     *
     * @return the DER-encoded signature algorithm parameters, or
     *         null if no parameters are present.
     */
    public abstract byte[] getSigAlgParams();
}
