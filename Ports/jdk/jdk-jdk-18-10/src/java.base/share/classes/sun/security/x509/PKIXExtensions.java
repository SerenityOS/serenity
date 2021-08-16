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

package sun.security.x509;


import sun.security.util.*;

/**
 * Lists all the object identifiers of the X509 extensions of the PKIX profile.
 *
 * <p>Extensions are addiitonal attributes which can be inserted in a X509
 * v3 certificate. For example a "Driving License Certificate" could have
 * the driving license number as a extension.
 *
 * <p>Extensions are represented as a sequence of the extension identifier
 * (Object Identifier), a boolean flag stating whether the extension is to
 * be treated as being critical and the extension value itself (this is again
 * a DER encoding of the extension value).
 *
 * @see Extension
 *
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class PKIXExtensions {
    /**
     * Identifies the particular public key used to sign the certificate.
     */
    public static final ObjectIdentifier AuthorityKey_Id =
            ObjectIdentifier.of(KnownOIDs.AuthorityKeyID);

    /**
     * Identifies the particular public key used in an application.
     */
    public static final ObjectIdentifier SubjectKey_Id =
            ObjectIdentifier.of(KnownOIDs.SubjectKeyID);

    /**
     * Defines the purpose of the key contained in the certificate.
     */
    public static final ObjectIdentifier KeyUsage_Id =
            ObjectIdentifier.of(KnownOIDs.KeyUsage);

    /**
     * Allows the certificate issuer to specify a different validity period
     * for the private key than the certificate.
     */
    public static final ObjectIdentifier PrivateKeyUsage_Id =
            ObjectIdentifier.of(KnownOIDs.PrivateKeyUsage);

    /**
     * Contains the sequence of policy information terms.
     */
    public static final ObjectIdentifier CertificatePolicies_Id =
            ObjectIdentifier.of(KnownOIDs.CertificatePolicies);

    /**
     * Lists pairs of object identifiers of policies considered equivalent by
     * the issuing CA to the subject CA.
     */
    public static final ObjectIdentifier PolicyMappings_Id =
            ObjectIdentifier.of(KnownOIDs.PolicyMappings);

    /**
     * Allows additional identities to be bound to the subject of the
     * certificate.
     */
    public static final ObjectIdentifier SubjectAlternativeName_Id =
            ObjectIdentifier.of(KnownOIDs.SubjectAlternativeName);

    /**
     * Allows additional identities to be associated with the certificate
     * issuer.
     */
    public static final ObjectIdentifier IssuerAlternativeName_Id =
            ObjectIdentifier.of(KnownOIDs.IssuerAlternativeName);

    /**
     * Identifies additional directory attributes.
     * This extension is always non-critical.
     */
    public static final ObjectIdentifier SubjectDirectoryAttributes_Id =
            ObjectIdentifier.of(KnownOIDs.SubjectDirectoryAttributes);

    /**
     * Identifies whether the subject of the certificate is a CA and how deep
     * a certification path may exist through that CA.
     */
    public static final ObjectIdentifier BasicConstraints_Id =
            ObjectIdentifier.of(KnownOIDs.BasicConstraints);

    /**
     * Provides for permitted and excluded subtrees that place restrictions
     * on names that may be included within a certificate issued by a given CA.
     */
    public static final ObjectIdentifier NameConstraints_Id =
            ObjectIdentifier.of(KnownOIDs.NameConstraints);

    /**
     * Used to either prohibit policy mapping or limit the set of policies
     * that can be in subsequent certificates.
     */
    public static final ObjectIdentifier PolicyConstraints_Id =
            ObjectIdentifier.of(KnownOIDs.PolicyConstraints);

    /**
     * Identifies how CRL information is obtained.
     */
    public static final ObjectIdentifier CRLDistributionPoints_Id =
            ObjectIdentifier.of(KnownOIDs.CRLDistributionPoints);

    /**
     * Conveys a monotonically increasing sequence number for each CRL
     * issued by a given CA.
     */
    public static final ObjectIdentifier CRLNumber_Id =
            ObjectIdentifier.of(KnownOIDs.CRLNumber);

    /**
     * Identifies the CRL distribution point for a particular CRL.
     */
    public static final ObjectIdentifier IssuingDistributionPoint_Id =
            ObjectIdentifier.of(KnownOIDs.IssuingDistributionPoint);

    /**
     * Identifies the delta CRL.
     */
    public static final ObjectIdentifier DeltaCRLIndicator_Id =
            ObjectIdentifier.of(KnownOIDs.DeltaCRLIndicator);

    /**
     * Identifies the reason for the certificate revocation.
     */
    public static final ObjectIdentifier ReasonCode_Id =
            ObjectIdentifier.of(KnownOIDs.ReasonCode);

    /**
     * This extension provides a registered instruction identifier indicating
     * the action to be taken, after encountering a certificate that has been
     * placed on hold.
     */
    public static final ObjectIdentifier HoldInstructionCode_Id =
            ObjectIdentifier.of(KnownOIDs.HoldInstructionCode);

    /**
     * Identifies the date on which it is known or suspected that the private
     * key was compromised or that the certificate otherwise became invalid.
     */
    public static final ObjectIdentifier InvalidityDate_Id =
            ObjectIdentifier.of(KnownOIDs.InvalidityDate);
    /**
     * Identifies one or more purposes for which the certified public key
     * may be used, in addition to or in place of the basic purposes
     * indicated in the key usage extension field.
     */
    public static final ObjectIdentifier ExtendedKeyUsage_Id =
            ObjectIdentifier.of(KnownOIDs.extendedKeyUsage);

    /**
     * Specifies whether any-policy policy OID is permitted
     */
    public static final ObjectIdentifier InhibitAnyPolicy_Id =
            ObjectIdentifier.of(KnownOIDs.InhibitAnyPolicy);

    /**
     * Identifies the certificate issuer associated with an entry in an
     * indirect CRL.
     */
    public static final ObjectIdentifier CertificateIssuer_Id =
            ObjectIdentifier.of(KnownOIDs.CertificateIssuer);

    /**
     * This extension indicates how to access CA information and services for
     * the issuer of the certificate in which the extension appears.
     * This information may be used for on-line certification validation
     * services.
     */
    public static final ObjectIdentifier AuthInfoAccess_Id =
            ObjectIdentifier.of(KnownOIDs.AuthInfoAccess);

    /**
     * This extension indicates how to access CA information and services for
     * the subject of the certificate in which the extension appears.
     */
    public static final ObjectIdentifier SubjectInfoAccess_Id =
            ObjectIdentifier.of(KnownOIDs.SubjectInfoAccess);

    /**
     * Identifies how delta CRL information is obtained.
     */
    public static final ObjectIdentifier FreshestCRL_Id =
            ObjectIdentifier.of(KnownOIDs.FreshestCRL);

    /**
     * Identifies the OCSP client can trust the responder for the
     * lifetime of the responder's certificate.
     */
    public static final ObjectIdentifier OCSPNoCheck_Id =
            ObjectIdentifier.of(KnownOIDs.OCSPNoCheck);

    /**
     * This extension is used to provide nonce data for OCSP requests
     * or responses.
     */
    public static final ObjectIdentifier OCSPNonce_Id =
            ObjectIdentifier.of(KnownOIDs.OCSPNonceExt);
}
