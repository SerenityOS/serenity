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

package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.*;
import javax.security.auth.x500.X500Principal;

import sun.security.util.*;
import sun.security.x509.*;

/**
 * A {@code CertSelector} that selects {@code X509Certificates} that
 * match all specified criteria. This class is particularly useful when
 * selecting certificates from a {@code CertStore} to build a
 * PKIX-compliant certification path.
 * <p>
 * When first constructed, an {@code X509CertSelector} has no criteria
 * enabled and each of the {@code get} methods return a default value
 * ({@code null}, or {@code -1} for the {@link #getBasicConstraints
 * getBasicConstraints} method). Therefore, the {@link #match match}
 * method would return {@code true} for any {@code X509Certificate}.
 * Typically, several criteria are enabled (by calling
 * {@link #setIssuer(X500Principal)} or
 * {@link #setKeyUsage setKeyUsage}, for instance) and then the
 * {@code X509CertSelector} is passed to
 * {@link CertStore#getCertificates CertStore.getCertificates} or some similar
 * method.
 * <p>
 * Several criteria can be enabled (by calling
 * {@link #setIssuer(X500Principal)}
 * and {@link #setSerialNumber setSerialNumber},
 * for example) such that the {@code match} method
 * usually uniquely matches a single {@code X509Certificate}. We say
 * usually, since it is possible for two issuing CAs to have the same
 * distinguished name and each issue a certificate with the same serial
 * number. Other unique combinations include the issuer, subject,
 * subjectKeyIdentifier and/or the subjectPublicKey criteria.
 * <p>
 * Please refer to <a href="http://tools.ietf.org/html/rfc5280">RFC 5280:
 * Internet X.509 Public Key Infrastructure Certificate and CRL Profile</a> for
 * definitions of the X.509 certificate extensions mentioned below.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertSelector
 * @see X509Certificate
 *
 * @since       1.4
 * @author      Steve Hanna
 */
public class X509CertSelector implements CertSelector {

    private static final Debug debug = Debug.getInstance("certpath");

    private static final ObjectIdentifier ANY_EXTENDED_KEY_USAGE =
        ObjectIdentifier.of(KnownOIDs.anyExtendedKeyUsage);

    static {
        CertPathHelperImpl.initialize();
    }

    private BigInteger serialNumber;
    private X500Principal issuer;
    private X500Principal subject;
    private byte[] subjectKeyID;
    private byte[] authorityKeyID;
    private Date certificateValid;
    private Date privateKeyValid;
    private ObjectIdentifier subjectPublicKeyAlgID;
    private PublicKey subjectPublicKey;
    private byte[] subjectPublicKeyBytes;
    private boolean[] keyUsage;
    private Set<String> keyPurposeSet;
    private Set<ObjectIdentifier> keyPurposeOIDSet;
    private Set<List<?>> subjectAlternativeNames;
    private Set<GeneralNameInterface> subjectAlternativeGeneralNames;
    private CertificatePolicySet policy;
    private Set<String> policySet;
    private Set<List<?>> pathToNames;
    private Set<GeneralNameInterface> pathToGeneralNames;
    private NameConstraintsExtension nc;
    private byte[] ncBytes;
    private int basicConstraints = -1;
    private X509Certificate x509Cert;
    private boolean matchAllSubjectAltNames = true;

    private static final Boolean FALSE = Boolean.FALSE;

    /* Constants representing the GeneralName types */
    static final int NAME_ANY = 0;
    static final int NAME_RFC822 = 1;
    static final int NAME_DNS = 2;
    static final int NAME_X400 = 3;
    static final int NAME_DIRECTORY = 4;
    static final int NAME_EDI = 5;
    static final int NAME_URI = 6;
    static final int NAME_IP = 7;
    static final int NAME_OID = 8;

    /**
     * Creates an {@code X509CertSelector}. Initially, no criteria are set
     * so any {@code X509Certificate} will match.
     */
    public X509CertSelector() {
        // empty
    }

    /**
     * Sets the certificateEquals criterion. The specified
     * {@code X509Certificate} must be equal to the
     * {@code X509Certificate} passed to the {@code match} method.
     * If {@code null}, then this check is not applied.
     *
     * <p>This method is particularly useful when it is necessary to
     * match a single certificate. Although other criteria can be specified
     * in conjunction with the certificateEquals criterion, it is usually not
     * practical or necessary.
     *
     * @param cert the {@code X509Certificate} to match (or
     * {@code null})
     * @see #getCertificate
     */
    public void setCertificate(X509Certificate cert) {
        x509Cert = cert;
    }

    /**
     * Sets the serialNumber criterion. The specified serial number
     * must match the certificate serial number in the
     * {@code X509Certificate}. If {@code null}, any certificate
     * serial number will do.
     *
     * @param serial the certificate serial number to match
     *        (or {@code null})
     * @see #getSerialNumber
     */
    public void setSerialNumber(BigInteger serial) {
        serialNumber = serial;
    }

    /**
     * Sets the issuer criterion. The specified distinguished name
     * must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null}, any issuer
     * distinguished name will do.
     *
     * @param issuer a distinguished name as X500Principal
     *                 (or {@code null})
     * @since 1.5
     */
    public void setIssuer(X500Principal issuer) {
        this.issuer = issuer;
    }

    /**
     * Sets the issuer criterion. The specified distinguished name
     * must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null}, any issuer
     * distinguished name will do.
     * <p>
     * If {@code issuerDN} is not {@code null}, it should contain a
     * distinguished name, in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> format.
     *
     * @param issuerDN a distinguished name in RFC 2253 format
     *                 (or {@code null})
     * @throws IOException if a parsing error occurs (incorrect form for DN)
     *
     * @deprecated Use {@link #setIssuer(X500Principal)} or
     * {@link #setIssuer(byte[])} instead. This method should not be relied on
     * as it can fail to match some certificates because of a loss of encoding
     * information in the RFC 2253 String form of some distinguished names.
     */
    @Deprecated(since="16")
    public void setIssuer(String issuerDN) throws IOException {
        if (issuerDN == null) {
            issuer = null;
        } else {
            issuer = new X500Name(issuerDN).asX500Principal();
        }
    }

    /**
     * Sets the issuer criterion. The specified distinguished name
     * must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null} is specified,
     * the issuer criterion is disabled and any issuer distinguished name will
     * do.
     * <p>
     * If {@code issuerDN} is not {@code null}, it should contain a
     * single DER encoded distinguished name, as defined in X.501. The ASN.1
     * notation for this structure is as follows.
     * <pre>{@code
     * Name ::= CHOICE {
     *   RDNSequence }
     *
     * RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
     *
     * RelativeDistinguishedName ::=
     *   SET SIZE (1 .. MAX) OF AttributeTypeAndValue
     *
     * AttributeTypeAndValue ::= SEQUENCE {
     *   type     AttributeType,
     *   value    AttributeValue }
     *
     * AttributeType ::= OBJECT IDENTIFIER
     *
     * AttributeValue ::= ANY DEFINED BY AttributeType
     * ....
     * DirectoryString ::= CHOICE {
     *       teletexString           TeletexString (SIZE (1..MAX)),
     *       printableString         PrintableString (SIZE (1..MAX)),
     *       universalString         UniversalString (SIZE (1..MAX)),
     *       utf8String              UTF8String (SIZE (1.. MAX)),
     *       bmpString               BMPString (SIZE (1..MAX)) }
     * }</pre>
     * <p>
     * Note that the byte array specified here is cloned to protect against
     * subsequent modifications.
     *
     * @param issuerDN a byte array containing the distinguished name
     *                 in ASN.1 DER encoded form (or {@code null})
     * @throws IOException if an encoding error occurs (incorrect form for DN)
     */
    public void setIssuer(byte[] issuerDN) throws IOException {
        try {
            issuer = (issuerDN == null ? null : new X500Principal(issuerDN));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * Sets the subject criterion. The specified distinguished name
     * must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, any subject
     * distinguished name will do.
     *
     * @param subject a distinguished name as X500Principal
     *                  (or {@code null})
     * @since 1.5
     */
    public void setSubject(X500Principal subject) {
        this.subject = subject;
    }

    /**
     * Sets the subject criterion. The specified distinguished name
     * must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, any subject
     * distinguished name will do.
     * <p>
     * If {@code subjectDN} is not {@code null}, it should contain a
     * distinguished name, in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> format.
     *
     * @param subjectDN a distinguished name in RFC 2253 format
     *                  (or {@code null})
     * @throws IOException if a parsing error occurs (incorrect form for DN)
     *
     * @deprecated Use {@link #setSubject(X500Principal)} or
     * {@link #setSubject(byte[])} instead. This method should not be relied
     * on as it can fail to match some certificates because of a loss of
     * encoding information in the RFC 2253 String form of some distinguished
     * names.
     */
    @Deprecated(since="16")
    public void setSubject(String subjectDN) throws IOException {
        if (subjectDN == null) {
            subject = null;
        } else {
            subject = new X500Name(subjectDN).asX500Principal();
        }
    }

    /**
     * Sets the subject criterion. The specified distinguished name
     * must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, any subject
     * distinguished name will do.
     * <p>
     * If {@code subjectDN} is not {@code null}, it should contain a
     * single DER encoded distinguished name, as defined in X.501. For the ASN.1
     * notation for this structure, see {@link #setIssuer(byte[])}.
     *
     * @param subjectDN a byte array containing the distinguished name in
     *                  ASN.1 DER format (or {@code null})
     * @throws IOException if an encoding error occurs (incorrect form for DN)
     */
    public void setSubject(byte[] subjectDN) throws IOException {
        try {
            subject = (subjectDN == null ? null : new X500Principal(subjectDN));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid name", e);
        }
    }

    /**
     * Sets the subjectKeyIdentifier criterion. The
     * {@code X509Certificate} must contain a SubjectKeyIdentifier
     * extension for which the contents of the extension
     * matches the specified criterion value.
     * If the criterion value is {@code null}, no
     * subjectKeyIdentifier check will be done.
     * <p>
     * If {@code subjectKeyID} is not {@code null}, it
     * should contain a single DER encoded value corresponding to the contents
     * of the extension value (not including the object identifier,
     * criticality setting, and encapsulating OCTET STRING)
     * for a SubjectKeyIdentifier extension.
     * The ASN.1 notation for this structure follows.
     *
     * <pre>{@code
     * SubjectKeyIdentifier ::= KeyIdentifier
     *
     * KeyIdentifier ::= OCTET STRING
     * }</pre>
     * <p>
     * Since the format of subject key identifiers is not mandated by
     * any standard, subject key identifiers are not parsed by the
     * {@code X509CertSelector}. Instead, the values are compared using
     * a byte-by-byte comparison.
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param subjectKeyID the subject key identifier (or {@code null})
     * @see #getSubjectKeyIdentifier
     */
    public void setSubjectKeyIdentifier(byte[] subjectKeyID) {
        if (subjectKeyID == null) {
            this.subjectKeyID = null;
        } else {
            this.subjectKeyID = subjectKeyID.clone();
        }
    }

    /**
     * Sets the authorityKeyIdentifier criterion. The
     * {@code X509Certificate} must contain an
     * AuthorityKeyIdentifier extension for which the contents of the
     * extension value matches the specified criterion value.
     * If the criterion value is {@code null}, no
     * authorityKeyIdentifier check will be done.
     * <p>
     * If {@code authorityKeyID} is not {@code null}, it
     * should contain a single DER encoded value corresponding to the contents
     * of the extension value (not including the object identifier,
     * criticality setting, and encapsulating OCTET STRING)
     * for an AuthorityKeyIdentifier extension.
     * The ASN.1 notation for this structure follows.
     *
     * <pre>{@code
     * AuthorityKeyIdentifier ::= SEQUENCE {
     *    keyIdentifier             [0] KeyIdentifier           OPTIONAL,
     *    authorityCertIssuer       [1] GeneralNames            OPTIONAL,
     *    authorityCertSerialNumber [2] CertificateSerialNumber OPTIONAL  }
     *
     * KeyIdentifier ::= OCTET STRING
     * }</pre>
     * <p>
     * Authority key identifiers are not parsed by the
     * {@code X509CertSelector}.  Instead, the values are
     * compared using a byte-by-byte comparison.
     * <p>
     * When the {@code keyIdentifier} field of
     * {@code AuthorityKeyIdentifier} is populated, the value is
     * usually taken from the {@code SubjectKeyIdentifier} extension
     * in the issuer's certificate.  Note, however, that the result of
     * {@code X509Certificate.getExtensionValue(<SubjectKeyIdentifier Object
     * Identifier>)} on the issuer's certificate may NOT be used
     * directly as the input to {@code setAuthorityKeyIdentifier}.
     * This is because the SubjectKeyIdentifier contains
     * only a KeyIdentifier OCTET STRING, and not a SEQUENCE of
     * KeyIdentifier, GeneralNames, and CertificateSerialNumber.
     * In order to use the extension value of the issuer certificate's
     * {@code SubjectKeyIdentifier}
     * extension, it will be necessary to extract the value of the embedded
     * {@code KeyIdentifier} OCTET STRING, then DER encode this OCTET
     * STRING inside a SEQUENCE.
     * For more details on SubjectKeyIdentifier, see
     * {@link #setSubjectKeyIdentifier(byte[] subjectKeyID)}.
     * <p>
     * Note also that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param authorityKeyID the authority key identifier
     *        (or {@code null})
     * @see #getAuthorityKeyIdentifier
     */
    public void setAuthorityKeyIdentifier(byte[] authorityKeyID) {
        if (authorityKeyID == null) {
            this.authorityKeyID = null;
        } else {
            this.authorityKeyID = authorityKeyID.clone();
        }
    }

    /**
     * Sets the certificateValid criterion. The specified date must fall
     * within the certificate validity period for the
     * {@code X509Certificate}. If {@code null}, no certificateValid
     * check will be done.
     * <p>
     * Note that the {@code Date} supplied here is cloned to protect
     * against subsequent modifications.
     *
     * @param certValid the {@code Date} to check (or {@code null})
     * @see #getCertificateValid
     */
    public void setCertificateValid(Date certValid) {
        if (certValid == null) {
            certificateValid = null;
        } else {
            certificateValid = (Date)certValid.clone();
        }
    }

    /**
     * Sets the privateKeyValid criterion. The specified date must fall
     * within the private key validity period for the
     * {@code X509Certificate}. If {@code null}, no privateKeyValid
     * check will be done.
     * <p>
     * Note that the {@code Date} supplied here is cloned to protect
     * against subsequent modifications.
     *
     * @param privateKeyValid the {@code Date} to check (or
     *                        {@code null})
     * @see #getPrivateKeyValid
     */
    public void setPrivateKeyValid(Date privateKeyValid) {
        if (privateKeyValid == null) {
            this.privateKeyValid = null;
        } else {
            this.privateKeyValid = (Date)privateKeyValid.clone();
        }
    }

    /**
     * Sets the subjectPublicKeyAlgID criterion. The
     * {@code X509Certificate} must contain a subject public key
     * with the specified algorithm. If {@code null}, no
     * subjectPublicKeyAlgID check will be done.
     *
     * @param oid The object identifier (OID) of the algorithm to check
     *            for (or {@code null}). An OID is represented by a
     *            set of nonnegative integers separated by periods.
     * @throws IOException if the OID is invalid, such as
     * the first component being not 0, 1 or 2 or the second component
     * being greater than 39.
     *
     * @see #getSubjectPublicKeyAlgID
     */
    public void setSubjectPublicKeyAlgID(String oid) throws IOException {
        if (oid == null) {
            subjectPublicKeyAlgID = null;
        } else {
            subjectPublicKeyAlgID = ObjectIdentifier.of(oid);
        }
    }

    /**
     * Sets the subjectPublicKey criterion. The
     * {@code X509Certificate} must contain the specified subject public
     * key. If {@code null}, no subjectPublicKey check will be done.
     *
     * @param key the subject public key to check for (or {@code null})
     * @see #getSubjectPublicKey
     */
    public void setSubjectPublicKey(PublicKey key) {
        if (key == null) {
            subjectPublicKey = null;
            subjectPublicKeyBytes = null;
        } else {
            subjectPublicKey = key;
            subjectPublicKeyBytes = key.getEncoded();
        }
    }

    /**
     * Sets the subjectPublicKey criterion. The {@code X509Certificate}
     * must contain the specified subject public key. If {@code null},
     * no subjectPublicKey check will be done.
     * <p>
     * Because this method allows the public key to be specified as a byte
     * array, it may be used for unknown key types.
     * <p>
     * If {@code key} is not {@code null}, it should contain a
     * single DER encoded SubjectPublicKeyInfo structure, as defined in X.509.
     * The ASN.1 notation for this structure is as follows.
     * <pre>{@code
     * SubjectPublicKeyInfo  ::=  SEQUENCE  {
     *   algorithm            AlgorithmIdentifier,
     *   subjectPublicKey     BIT STRING  }
     *
     * AlgorithmIdentifier  ::=  SEQUENCE  {
     *   algorithm               OBJECT IDENTIFIER,
     *   parameters              ANY DEFINED BY algorithm OPTIONAL  }
     *                              -- contains a value of the type
     *                              -- registered for use with the
     *                              -- algorithm object identifier value
     * }</pre>
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param key a byte array containing the subject public key in ASN.1 DER
     *            form (or {@code null})
     * @throws IOException if an encoding error occurs (incorrect form for
     * subject public key)
     * @see #getSubjectPublicKey
     */
    public void setSubjectPublicKey(byte[] key) throws IOException {
        if (key == null) {
            subjectPublicKey = null;
            subjectPublicKeyBytes = null;
        } else {
            subjectPublicKeyBytes = key.clone();
            subjectPublicKey = X509Key.parse(new DerValue(subjectPublicKeyBytes));
        }
    }

    /**
     * Sets the keyUsage criterion. The {@code X509Certificate}
     * must allow the specified keyUsage values. If {@code null}, no
     * keyUsage check will be done. Note that an {@code X509Certificate}
     * that has no keyUsage extension implicitly allows all keyUsage values.
     * <p>
     * Note that the boolean array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param keyUsage a boolean array in the same format as the boolean
     *                 array returned by
     * {@link X509Certificate#getKeyUsage() X509Certificate.getKeyUsage()}.
     *                 Or {@code null}.
     * @see #getKeyUsage
     */
    public void setKeyUsage(boolean[] keyUsage) {
        if (keyUsage == null) {
            this.keyUsage = null;
        } else {
            this.keyUsage = keyUsage.clone();
        }
    }

    /**
     * Sets the extendedKeyUsage criterion. The {@code X509Certificate}
     * must allow the specified key purposes in its extended key usage
     * extension. If {@code keyPurposeSet} is empty or {@code null},
     * no extendedKeyUsage check will be done. Note that an
     * {@code X509Certificate} that has no extendedKeyUsage extension
     * implicitly allows all key purposes.
     * <p>
     * Note that the {@code Set} is cloned to protect against
     * subsequent modifications.
     *
     * @param keyPurposeSet a {@code Set} of key purpose OIDs in string
     * format (or {@code null}). Each OID is represented by a set of
     * nonnegative integers separated by periods.
     * @throws IOException if the OID is invalid, such as
     * the first component being not 0, 1 or 2 or the second component
     * being greater than 39.
     * @see #getExtendedKeyUsage
     */
    public void setExtendedKeyUsage(Set<String> keyPurposeSet) throws IOException {
        if ((keyPurposeSet == null) || keyPurposeSet.isEmpty()) {
            this.keyPurposeSet = null;
            keyPurposeOIDSet = null;
        } else {
            this.keyPurposeSet =
                Collections.unmodifiableSet(new HashSet<>(keyPurposeSet));
            keyPurposeOIDSet = new HashSet<>();
            for (String s : this.keyPurposeSet) {
                keyPurposeOIDSet.add(ObjectIdentifier.of(s));
            }
        }
    }

    /**
     * Enables/disables matching all of the subjectAlternativeNames
     * specified in the {@link #setSubjectAlternativeNames
     * setSubjectAlternativeNames} or {@link #addSubjectAlternativeName
     * addSubjectAlternativeName} methods. If enabled,
     * the {@code X509Certificate} must contain all of the
     * specified subject alternative names. If disabled, the
     * {@code X509Certificate} must contain at least one of the
     * specified subject alternative names.
     *
     * <p>The matchAllNames flag is {@code true} by default.
     *
     * @param matchAllNames if {@code true}, the flag is enabled;
     * if {@code false}, the flag is disabled.
     * @see #getMatchAllSubjectAltNames
     */
    public void setMatchAllSubjectAltNames(boolean matchAllNames) {
        this.matchAllSubjectAltNames = matchAllNames;
    }

    /**
     * Sets the subjectAlternativeNames criterion. The
     * {@code X509Certificate} must contain all or at least one of the
     * specified subjectAlternativeNames, depending on the value of
     * the matchAllNames flag (see {@link #setMatchAllSubjectAltNames
     * setMatchAllSubjectAltNames}).
     * <p>
     * This method allows the caller to specify, with a single method call,
     * the complete set of subject alternative names for the
     * subjectAlternativeNames criterion. The specified value replaces
     * the previous value for the subjectAlternativeNames criterion.
     * <p>
     * The {@code names} parameter (if not {@code null}) is a
     * {@code Collection} with one
     * entry for each name to be included in the subject alternative name
     * criterion. Each entry is a {@code List} whose first entry is an
     * {@code Integer} (the name type, 0-8) and whose second
     * entry is a {@code String} or a byte array (the name, in
     * string or ASN.1 DER encoded form, respectively).
     * There can be multiple names of the same type. If {@code null}
     * is supplied as the value for this argument, no
     * subjectAlternativeNames check will be performed.
     * <p>
     * Each subject alternative name in the {@code Collection}
     * may be specified either as a {@code String} or as an ASN.1 encoded
     * byte array. For more details about the formats used, see
     * {@link #addSubjectAlternativeName(int type, String name)
     * addSubjectAlternativeName(int type, String name)} and
     * {@link #addSubjectAlternativeName(int type, byte [] name)
     * addSubjectAlternativeName(int type, byte [] name)}.
     * <p>
     * <strong>Note:</strong> for distinguished names, specify the byte
     * array form instead of the String form. See the note in
     * {@link #addSubjectAlternativeName(int, String)} for more information.
     * <p>
     * Note that the {@code names} parameter can contain duplicate
     * names (same name and name type), but they may be removed from the
     * {@code Collection} of names returned by the
     * {@link #getSubjectAlternativeNames getSubjectAlternativeNames} method.
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @param names a {@code Collection} of names (or {@code null})
     * @throws IOException if a parsing error occurs
     * @see #getSubjectAlternativeNames
     */
    public void setSubjectAlternativeNames(Collection<List<?>> names)
            throws IOException {
        if (names == null) {
            subjectAlternativeNames = null;
            subjectAlternativeGeneralNames = null;
        } else {
            if (names.isEmpty()) {
                subjectAlternativeNames = null;
                subjectAlternativeGeneralNames = null;
                return;
            }
            Set<List<?>> tempNames = cloneAndCheckNames(names);
            // Ensure that we either set both of these or neither
            subjectAlternativeGeneralNames = parseNames(tempNames);
            subjectAlternativeNames = tempNames;
        }
    }

    /**
     * Adds a name to the subjectAlternativeNames criterion. The
     * {@code X509Certificate} must contain all or at least one
     * of the specified subjectAlternativeNames, depending on the value of
     * the matchAllNames flag (see {@link #setMatchAllSubjectAltNames
     * setMatchAllSubjectAltNames}).
     * <p>
     * This method allows the caller to add a name to the set of subject
     * alternative names.
     * The specified name is added to any previous value for the
     * subjectAlternativeNames criterion. If the specified name is a
     * duplicate, it may be ignored.
     * <p>
     * The name is provided in string format.
     * <a href="http://www.ietf.org/rfc/rfc822.txt">RFC 822</a>, DNS, and URI
     * names use the well-established string formats for those types (subject to
     * the restrictions included in RFC 5280). IPv4 address names are
     * supplied using dotted quad notation. OID address names are represented
     * as a series of nonnegative integers separated by periods. And
     * directory names (distinguished names) are supplied in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> format.
     * No standard string format is defined for otherNames, X.400 names,
     * EDI party names, IPv6 address names, or any other type of names. They
     * should be specified using the
     * {@link #addSubjectAlternativeName(int type, byte [] name)
     * addSubjectAlternativeName(int type, byte [] name)}
     * method.
     * <p>
     * <strong>Note:</strong> for distinguished names, use
     * {@linkplain #addSubjectAlternativeName(int, byte[])} instead.
     * This method should not be relied on as it can fail to match some
     * certificates because of a loss of encoding information in the RFC 2253
     * String form of some distinguished names.
     *
     * @param type the name type (0-8, as specified in
     *             RFC 5280, section 4.2.1.6)
     * @param name the name in string form (not {@code null})
     * @throws IOException if a parsing error occurs
     */
    public void addSubjectAlternativeName(int type, String name)
            throws IOException {
        addSubjectAlternativeNameInternal(type, name);
    }

    /**
     * Adds a name to the subjectAlternativeNames criterion. The
     * {@code X509Certificate} must contain all or at least one
     * of the specified subjectAlternativeNames, depending on the value of
     * the matchAllNames flag (see {@link #setMatchAllSubjectAltNames
     * setMatchAllSubjectAltNames}).
     * <p>
     * This method allows the caller to add a name to the set of subject
     * alternative names.
     * The specified name is added to any previous value for the
     * subjectAlternativeNames criterion. If the specified name is a
     * duplicate, it may be ignored.
     * <p>
     * The name is provided as a byte array. This byte array should contain
     * the DER encoded name, as it would appear in the GeneralName structure
     * defined in RFC 5280 and X.509. The encoded byte array should only contain
     * the encoded value of the name, and should not include the tag associated
     * with the name in the GeneralName structure. The ASN.1 definition of this
     * structure appears below.
     * <pre>{@code
     *  GeneralName ::= CHOICE {
     *       otherName                       [0]     OtherName,
     *       rfc822Name                      [1]     IA5String,
     *       dNSName                         [2]     IA5String,
     *       x400Address                     [3]     ORAddress,
     *       directoryName                   [4]     Name,
     *       ediPartyName                    [5]     EDIPartyName,
     *       uniformResourceIdentifier       [6]     IA5String,
     *       iPAddress                       [7]     OCTET STRING,
     *       registeredID                    [8]     OBJECT IDENTIFIER}
     * }</pre>
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param type the name type (0-8, as listed above)
     * @param name a byte array containing the name in ASN.1 DER encoded form
     * @throws IOException if a parsing error occurs
     */
    public void addSubjectAlternativeName(int type, byte[] name)
            throws IOException {
        // clone because byte arrays are modifiable
        addSubjectAlternativeNameInternal(type, name.clone());
    }

    /**
     * A private method that adds a name (String or byte array) to the
     * subjectAlternativeNames criterion. The {@code X509Certificate}
     * must contain the specified subjectAlternativeName.
     *
     * @param type the name type (0-8, as specified in
     *             RFC 5280, section 4.2.1.6)
     * @param name the name in string or byte array form
     * @throws IOException if a parsing error occurs
     */
    private void addSubjectAlternativeNameInternal(int type, Object name)
            throws IOException {
        // First, ensure that the name parses
        GeneralNameInterface tempName = makeGeneralNameInterface(type, name);
        if (subjectAlternativeNames == null) {
            subjectAlternativeNames = new HashSet<>();
        }
        if (subjectAlternativeGeneralNames == null) {
            subjectAlternativeGeneralNames = new HashSet<>();
        }
        List<Object> list = new ArrayList<>(2);
        list.add(Integer.valueOf(type));
        list.add(name);
        subjectAlternativeNames.add(list);
        subjectAlternativeGeneralNames.add(tempName);
    }

    /**
     * Parse an argument of the form passed to setSubjectAlternativeNames,
     * returning a {@code Collection} of
     * {@code GeneralNameInterface}s.
     * Throw an IllegalArgumentException or a ClassCastException
     * if the argument is malformed.
     *
     * @param names a Collection with one entry per name.
     *              Each entry is a {@code List} whose first entry
     *              is an Integer (the name type, 0-8) and whose second
     *              entry is a String or a byte array (the name, in
     *              string or ASN.1 DER encoded form, respectively).
     *              There can be multiple names of the same type. Null is
     *              not an acceptable value.
     * @return a Set of {@code GeneralNameInterface}s
     * @throws IOException if a parsing error occurs
     */
    private static Set<GeneralNameInterface> parseNames(Collection<List<?>> names) throws IOException {
        Set<GeneralNameInterface> genNames = new HashSet<>();
        for (List<?> nameList : names) {
            if (nameList.size() != 2) {
                throw new IOException("name list size not 2");
            }
            Object o =  nameList.get(0);
            if (!(o instanceof Integer nameType)) {
                throw new IOException("expected an Integer");
            }
            o = nameList.get(1);
            genNames.add(makeGeneralNameInterface(nameType, o));
        }

        return genNames;
    }

    /**
     * Compare for equality two objects of the form passed to
     * setSubjectAlternativeNames (or X509CRLSelector.setIssuerNames).
     * Throw an {@code IllegalArgumentException} or a
     * {@code ClassCastException} if one of the objects is malformed.
     *
     * @param object1 a Collection containing the first object to compare
     * @param object2 a Collection containing the second object to compare
     * @return true if the objects are equal, false otherwise
     */
    static boolean equalNames(Collection<?> object1, Collection<?> object2) {
        if ((object1 == null) || (object2 == null)) {
            return object1 == object2;
        }
        return object1.equals(object2);
    }

    /**
     * Make a {@code GeneralNameInterface} out of a name type (0-8) and an
     * Object that may be a byte array holding the ASN.1 DER encoded
     * name or a String form of the name.  Except for X.509
     * Distinguished Names, the String form of the name must not be the
     * result from calling toString on an existing GeneralNameInterface
     * implementing class.  The output of toString is not compatible
     * with the String constructors for names other than Distinguished
     * Names.
     *
     * @param type name type (0-8)
     * @param name name as ASN.1 Der-encoded byte array or String
     * @return a GeneralNameInterface name
     * @throws IOException if a parsing error occurs
     */
    static GeneralNameInterface makeGeneralNameInterface(int type, Object name)
            throws IOException {
        GeneralNameInterface result;
        if (debug != null) {
            debug.println("X509CertSelector.makeGeneralNameInterface("
                + type + ")...");
        }

        if (name instanceof String nameAsString) {
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralNameInterface() "
                    + "name is String: " + nameAsString);
            }
            result = switch (type) {
                case NAME_RFC822    -> new RFC822Name(nameAsString);
                case NAME_DNS       -> new DNSName(nameAsString);
                case NAME_DIRECTORY -> new X500Name(nameAsString);
                case NAME_URI       -> new URIName(nameAsString);
                case NAME_IP        -> new IPAddressName(nameAsString);
                case NAME_OID       -> new OIDName(nameAsString);
                default -> throw new IOException("unable to parse String names of type "
                                                 + type);
            };
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralNameInterface() "
                    + "result: " + result.toString());
            }
        } else if (name instanceof byte[]) {
            DerValue val = new DerValue((byte[]) name);
            if (debug != null) {
                debug.println
                    ("X509CertSelector.makeGeneralNameInterface() is byte[]");
            }

            result = switch (type) {
                case NAME_ANY       -> new OtherName(val);
                case NAME_RFC822    -> new RFC822Name(val);
                case NAME_DNS       -> new DNSName(val);
                case NAME_X400      -> new X400Address(val);
                case NAME_DIRECTORY -> new X500Name(val);
                case NAME_EDI       -> new EDIPartyName(val);
                case NAME_URI       -> new URIName(val);
                case NAME_IP        -> new IPAddressName(val);
                case NAME_OID       -> new OIDName(val);
                default -> throw new IOException("unable to parse byte array names of "
                                                 + "type " + type);
            };
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralNameInterface() result: "
                    + result.toString());
            }
        } else {
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralName() input name "
                    + "not String or byte array");
            }
            throw new IOException("name not String or byte array");
        }
        return result;
    }


    /**
     * Sets the name constraints criterion. The {@code X509Certificate}
     * must have subject and subject alternative names that
     * meet the specified name constraints.
     * <p>
     * The name constraints are specified as a byte array. This byte array
     * should contain the DER encoded form of the name constraints, as they
     * would appear in the NameConstraints structure defined in RFC 5280
     * and X.509. The ASN.1 definition of this structure appears below.
     *
     * <pre>{@code
     *  NameConstraints ::= SEQUENCE {
     *       permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
     *       excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
     *
     *  GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
     *
     *  GeneralSubtree ::= SEQUENCE {
     *       base                    GeneralName,
     *       minimum         [0]     BaseDistance DEFAULT 0,
     *       maximum         [1]     BaseDistance OPTIONAL }
     *
     *  BaseDistance ::= INTEGER (0..MAX)
     *
     *  GeneralName ::= CHOICE {
     *       otherName                       [0]     OtherName,
     *       rfc822Name                      [1]     IA5String,
     *       dNSName                         [2]     IA5String,
     *       x400Address                     [3]     ORAddress,
     *       directoryName                   [4]     Name,
     *       ediPartyName                    [5]     EDIPartyName,
     *       uniformResourceIdentifier       [6]     IA5String,
     *       iPAddress                       [7]     OCTET STRING,
     *       registeredID                    [8]     OBJECT IDENTIFIER}
     * }</pre>
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param bytes a byte array containing the ASN.1 DER encoding of
     *              a NameConstraints extension to be used for checking
     *              name constraints. Only the value of the extension is
     *              included, not the OID or criticality flag. Can be
     *              {@code null},
     *              in which case no name constraints check will be performed.
     * @throws IOException if a parsing error occurs
     * @see #getNameConstraints
     */
    public void setNameConstraints(byte[] bytes) throws IOException {
        if (bytes == null) {
            ncBytes = null;
            nc = null;
        } else {
            ncBytes = bytes.clone();
            nc = new NameConstraintsExtension(FALSE, bytes);
        }
    }

    /**
     * Sets the basic constraints constraint. If the value is greater than or
     * equal to zero, {@code X509Certificates} must include a
     * basicConstraints extension with
     * a pathLen of at least this value. If the value is -2, only end-entity
     * certificates are accepted. If the value is -1, no check is done.
     * <p>
     * This constraint is useful when building a certification path forward
     * (from the target toward the trust anchor. If a partial path has been
     * built, any candidate certificate must have a maxPathLen value greater
     * than or equal to the number of certificates in the partial path.
     *
     * @param minMaxPathLen the value for the basic constraints constraint
     * @throws IllegalArgumentException if the value is less than -2
     * @see #getBasicConstraints
     */
    public void setBasicConstraints(int minMaxPathLen) {
        if (minMaxPathLen < -2) {
            throw new IllegalArgumentException("basic constraints less than -2");
        }
        basicConstraints = minMaxPathLen;
    }

    /**
     * Sets the policy constraint. The {@code X509Certificate} must
     * include at least one of the specified policies in its certificate
     * policies extension. If {@code certPolicySet} is empty, then the
     * {@code X509Certificate} must include at least some specified policy
     * in its certificate policies extension. If {@code certPolicySet} is
     * {@code null}, no policy check will be performed.
     * <p>
     * Note that the {@code Set} is cloned to protect against
     * subsequent modifications.
     *
     * @param certPolicySet a {@code Set} of certificate policy OIDs in
     *                      string format (or {@code null}). Each OID is
     *                      represented by a set of nonnegative integers
     *                    separated by periods.
     * @throws IOException if a parsing error occurs on the OID such as
     * the first component is not 0, 1 or 2 or the second component is
     * greater than 39.
     * @see #getPolicy
     */
    public void setPolicy(Set<String> certPolicySet) throws IOException {
        if (certPolicySet == null) {
            policySet = null;
            policy = null;
        } else {
            // Snapshot set and parse it
            Set<String> tempSet = Collections.unmodifiableSet
                                        (new HashSet<>(certPolicySet));
            /* Convert to Vector of ObjectIdentifiers */
            Iterator<String> i = tempSet.iterator();
            Vector<CertificatePolicyId> polIdVector = new Vector<>();
            while (i.hasNext()) {
                Object o = i.next();
                if (!(o instanceof String)) {
                    throw new IOException("non String in certPolicySet");
                }
                polIdVector.add(new CertificatePolicyId
                        (ObjectIdentifier.of((String)o)));
            }
            // If everything went OK, make the changes
            policySet = tempSet;
            policy = new CertificatePolicySet(polIdVector);
        }
    }

    /**
     * Sets the pathToNames criterion. The {@code X509Certificate} must
     * not include name constraints that would prohibit building a
     * path to the specified names.
     * <p>
     * This method allows the caller to specify, with a single method call,
     * the complete set of names which the {@code X509Certificates}'s
     * name constraints must permit. The specified value replaces
     * the previous value for the pathToNames criterion.
     * <p>
     * This constraint is useful when building a certification path forward
     * (from the target toward the trust anchor. If a partial path has been
     * built, any candidate certificate must not include name constraints that
     * would prohibit building a path to any of the names in the partial path.
     * <p>
     * The {@code names} parameter (if not {@code null}) is a
     * {@code Collection} with one
     * entry for each name to be included in the pathToNames
     * criterion. Each entry is a {@code List} whose first entry is an
     * {@code Integer} (the name type, 0-8) and whose second
     * entry is a {@code String} or a byte array (the name, in
     * string or ASN.1 DER encoded form, respectively).
     * There can be multiple names of the same type. If {@code null}
     * is supplied as the value for this argument, no
     * pathToNames check will be performed.
     * <p>
     * Each name in the {@code Collection}
     * may be specified either as a {@code String} or as an ASN.1 encoded
     * byte array. For more details about the formats used, see
     * {@link #addPathToName(int type, String name)
     * addPathToName(int type, String name)} and
     * {@link #addPathToName(int type, byte [] name)
     * addPathToName(int type, byte [] name)}.
     * <p>
     * <strong>Note:</strong> for distinguished names, specify the byte
     * array form instead of the String form. See the note in
     * {@link #addPathToName(int, String)} for more information.
     * <p>
     * Note that the {@code names} parameter can contain duplicate
     * names (same name and name type), but they may be removed from the
     * {@code Collection} of names returned by the
     * {@link #getPathToNames getPathToNames} method.
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @param names a {@code Collection} with one entry per name
     *              (or {@code null})
     * @throws IOException if a parsing error occurs
     * @see #getPathToNames
     */
    public void setPathToNames(Collection<List<?>> names) throws IOException {
        if ((names == null) || names.isEmpty()) {
            pathToNames = null;
            pathToGeneralNames = null;
        } else {
            Set<List<?>> tempNames = cloneAndCheckNames(names);
            pathToGeneralNames = parseNames(tempNames);
            // Ensure that we either set both of these or neither
            pathToNames = tempNames;
        }
    }

    // called from CertPathHelper
    void setPathToNamesInternal(Set<GeneralNameInterface> names) {
        // set names to non-null dummy value
        // this breaks getPathToNames()
        pathToNames = Collections.<List<?>>emptySet();
        pathToGeneralNames = names;
    }

    /**
     * Adds a name to the pathToNames criterion. The {@code X509Certificate}
     * must not include name constraints that would prohibit building a
     * path to the specified name.
     * <p>
     * This method allows the caller to add a name to the set of names which
     * the {@code X509Certificates}'s name constraints must permit.
     * The specified name is added to any previous value for the
     * pathToNames criterion.  If the name is a duplicate, it may be ignored.
     * <p>
     * The name is provided in string format. RFC 822, DNS, and URI names
     * use the well-established string formats for those types (subject to
     * the restrictions included in RFC 5280). IPv4 address names are
     * supplied using dotted quad notation. OID address names are represented
     * as a series of nonnegative integers separated by periods. And
     * directory names (distinguished names) are supplied in RFC 2253 format.
     * No standard string format is defined for otherNames, X.400 names,
     * EDI party names, IPv6 address names, or any other type of names. They
     * should be specified using the
     * {@link #addPathToName(int type, byte [] name)
     * addPathToName(int type, byte [] name)} method.
     * <p>
     * <strong>Note:</strong> for distinguished names, use
     * {@linkplain #addPathToName(int, byte[])} instead.
     * This method should not be relied on as it can fail to match some
     * certificates because of a loss of encoding information in the RFC 2253
     * String form of some distinguished names.
     *
     * @param type the name type (0-8, as specified in
     *             RFC 5280, section 4.2.1.6)
     * @param name the name in string form
     * @throws IOException if a parsing error occurs
     */
    public void addPathToName(int type, String name) throws IOException {
        addPathToNameInternal(type, name);
    }

    /**
     * Adds a name to the pathToNames criterion. The {@code X509Certificate}
     * must not include name constraints that would prohibit building a
     * path to the specified name.
     * <p>
     * This method allows the caller to add a name to the set of names which
     * the {@code X509Certificates}'s name constraints must permit.
     * The specified name is added to any previous value for the
     * pathToNames criterion. If the name is a duplicate, it may be ignored.
     * <p>
     * The name is provided as a byte array. This byte array should contain
     * the DER encoded name, as it would appear in the GeneralName structure
     * defined in RFC 5280 and X.509. The ASN.1 definition of this structure
     * appears in the documentation for
     * {@link #addSubjectAlternativeName(int type, byte [] name)
     * addSubjectAlternativeName(int type, byte [] name)}.
     * <p>
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     *
     * @param type the name type (0-8, as specified in
     *             RFC 5280, section 4.2.1.6)
     * @param name a byte array containing the name in ASN.1 DER encoded form
     * @throws IOException if a parsing error occurs
     */
    public void addPathToName(int type, byte [] name) throws IOException {
        // clone because byte arrays are modifiable
        addPathToNameInternal(type, name.clone());
    }

    /**
     * A private method that adds a name (String or byte array) to the
     * pathToNames criterion. The {@code X509Certificate} must contain
     * the specified pathToName.
     *
     * @param type the name type (0-8, as specified in
     *             RFC 5280, section 4.2.1.6)
     * @param name the name in string or byte array form
     * @throws IOException if an encoding error occurs (incorrect form for DN)
     */
    private void addPathToNameInternal(int type, Object name)
            throws IOException {
        // First, ensure that the name parses
        GeneralNameInterface tempName = makeGeneralNameInterface(type, name);
        if (pathToGeneralNames == null) {
            pathToNames = new HashSet<>();
            pathToGeneralNames = new HashSet<>();
        }
        List<Object> list = new ArrayList<>(2);
        list.add(Integer.valueOf(type));
        list.add(name);
        pathToNames.add(list);
        pathToGeneralNames.add(tempName);
    }

    /**
     * Returns the certificateEquals criterion. The specified
     * {@code X509Certificate} must be equal to the
     * {@code X509Certificate} passed to the {@code match} method.
     * If {@code null}, this check is not applied.
     *
     * @return the {@code X509Certificate} to match (or {@code null})
     * @see #setCertificate
     */
    public X509Certificate getCertificate() {
        return x509Cert;
    }

    /**
     * Returns the serialNumber criterion. The specified serial number
     * must match the certificate serial number in the
     * {@code X509Certificate}. If {@code null}, any certificate
     * serial number will do.
     *
     * @return the certificate serial number to match
     *                (or {@code null})
     * @see #setSerialNumber
     */
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * Returns the issuer criterion as an {@code X500Principal}. This
     * distinguished name must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null}, the issuer criterion
     * is disabled and any issuer distinguished name will do.
     *
     * @return the required issuer distinguished name as X500Principal
     *         (or {@code null})
     * @since 1.5
     */
    public X500Principal getIssuer() {
        return issuer;
    }

    /**
     * Returns the issuer criterion as a {@code String}. This
     * distinguished name must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null}, the issuer criterion
     * is disabled and any issuer distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a
     * distinguished name, in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> format.
     *
     * @return the required issuer distinguished name in RFC 2253 format
     *         (or {@code null})
     *
     * @deprecated Use {@link #getIssuer()} or {@link #getIssuerAsBytes()}
     * instead. This method should not be relied on as it can fail to match
     * some certificates because of a loss of encoding information in the
     * RFC 2253 String form of some distinguished names.
     */
    @Deprecated(since="16")
    public String getIssuerAsString() {
        return (issuer == null ? null : issuer.getName());
    }

    /**
     * Returns the issuer criterion as a byte array. This distinguished name
     * must match the issuer distinguished name in the
     * {@code X509Certificate}. If {@code null}, the issuer criterion
     * is disabled and any issuer distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a byte
     * array containing a single DER encoded distinguished name, as defined in
     * X.501. The ASN.1 notation for this structure is supplied in the
     * documentation for {@link #setIssuer(byte[])}.
     * <p>
     * Note that the byte array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return a byte array containing the required issuer distinguished name
     *         in ASN.1 DER format (or {@code null})
     * @throws IOException if an encoding error occurs
     */
    public byte[] getIssuerAsBytes() throws IOException {
        return (issuer == null ? null: issuer.getEncoded());
    }

    /**
     * Returns the subject criterion as an {@code X500Principal}. This
     * distinguished name must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, the subject criterion
     * is disabled and any subject distinguished name will do.
     *
     * @return the required subject distinguished name as X500Principal
     *         (or {@code null})
     * @since 1.5
     */
    public X500Principal getSubject() {
        return subject;
    }

    /**
     * Returns the subject criterion as a {@code String}. This
     * distinguished name must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, the subject criterion
     * is disabled and any subject distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a
     * distinguished name, in
     * <a href="http://www.ietf.org/rfc/rfc2253.txt">RFC 2253</a> format.
     *
     * @return the required subject distinguished name in RFC 2253 format
     *         (or {@code null})
     *
     * @deprecated Use {@link #getSubject()} or {@link #getSubjectAsBytes()}
     * instead. This method should not be relied on as it can fail to match
     * some certificates because of a loss of encoding information in the
     * RFC 2253 String form of some distinguished names.
     */
    @Deprecated(since="16")
    public String getSubjectAsString() {
        return (subject == null ? null : subject.getName());
    }

    /**
     * Returns the subject criterion as a byte array. This distinguished name
     * must match the subject distinguished name in the
     * {@code X509Certificate}. If {@code null}, the subject criterion
     * is disabled and any subject distinguished name will do.
     * <p>
     * If the value returned is not {@code null}, it is a byte
     * array containing a single DER encoded distinguished name, as defined in
     * X.501. The ASN.1 notation for this structure is supplied in the
     * documentation for {@link #setSubject(byte[])}.
     * <p>
     * Note that the byte array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return a byte array containing the required subject distinguished name
     *         in ASN.1 DER format (or {@code null})
     * @throws IOException if an encoding error occurs
     */
    public byte[] getSubjectAsBytes() throws IOException {
        return (subject == null ? null : subject.getEncoded());
    }

    /**
     * Returns the subjectKeyIdentifier criterion. The
     * {@code X509Certificate} must contain a SubjectKeyIdentifier
     * extension with the specified value. If {@code null}, no
     * subjectKeyIdentifier check will be done.
     * <p>
     * Note that the byte array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return the key identifier (or {@code null})
     * @see #setSubjectKeyIdentifier
     */
    public byte[] getSubjectKeyIdentifier() {
        if (subjectKeyID == null) {
            return null;
        }
        return subjectKeyID.clone();
    }

    /**
     * Returns the authorityKeyIdentifier criterion. The
     * {@code X509Certificate} must contain a AuthorityKeyIdentifier
     * extension with the specified value. If {@code null}, no
     * authorityKeyIdentifier check will be done.
     * <p>
     * Note that the byte array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return the key identifier (or {@code null})
     * @see #setAuthorityKeyIdentifier
     */
    public byte[] getAuthorityKeyIdentifier() {
        if (authorityKeyID == null) {
          return null;
        }
        return authorityKeyID.clone();
    }

    /**
     * Returns the certificateValid criterion. The specified date must fall
     * within the certificate validity period for the
     * {@code X509Certificate}. If {@code null}, no certificateValid
     * check will be done.
     * <p>
     * Note that the {@code Date} returned is cloned to protect against
     * subsequent modifications.
     *
     * @return the {@code Date} to check (or {@code null})
     * @see #setCertificateValid
     */
    public Date getCertificateValid() {
        if (certificateValid == null) {
            return null;
        }
        return (Date)certificateValid.clone();
    }

    /**
     * Returns the privateKeyValid criterion. The specified date must fall
     * within the private key validity period for the
     * {@code X509Certificate}. If {@code null}, no privateKeyValid
     * check will be done.
     * <p>
     * Note that the {@code Date} returned is cloned to protect against
     * subsequent modifications.
     *
     * @return the {@code Date} to check (or {@code null})
     * @see #setPrivateKeyValid
     */
    public Date getPrivateKeyValid() {
        if (privateKeyValid == null) {
            return null;
        }
        return (Date)privateKeyValid.clone();
    }

    /**
     * Returns the subjectPublicKeyAlgID criterion. The
     * {@code X509Certificate} must contain a subject public key
     * with the specified algorithm. If {@code null}, no
     * subjectPublicKeyAlgID check will be done.
     *
     * @return the object identifier (OID) of the signature algorithm to check
     *         for (or {@code null}). An OID is represented by a set of
     *         nonnegative integers separated by periods.
     * @see #setSubjectPublicKeyAlgID
     */
    public String getSubjectPublicKeyAlgID() {
        if (subjectPublicKeyAlgID == null) {
            return null;
        }
        return subjectPublicKeyAlgID.toString();
    }

    /**
     * Returns the subjectPublicKey criterion. The
     * {@code X509Certificate} must contain the specified subject
     * public key. If {@code null}, no subjectPublicKey check will be done.
     *
     * @return the subject public key to check for (or {@code null})
     * @see #setSubjectPublicKey
     */
    public PublicKey getSubjectPublicKey() {
        return subjectPublicKey;
    }

    /**
     * Returns the keyUsage criterion. The {@code X509Certificate}
     * must allow the specified keyUsage values. If null, no keyUsage
     * check will be done.
     * <p>
     * Note that the boolean array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return a boolean array in the same format as the boolean
     *                 array returned by
     * {@link X509Certificate#getKeyUsage() X509Certificate.getKeyUsage()}.
     *                 Or {@code null}.
     * @see #setKeyUsage
     */
    public boolean[] getKeyUsage() {
        if (keyUsage == null) {
            return null;
        }
        return keyUsage.clone();
    }

    /**
     * Returns the extendedKeyUsage criterion. The {@code X509Certificate}
     * must allow the specified key purposes in its extended key usage
     * extension. If the {@code keyPurposeSet} returned is empty or
     * {@code null}, no extendedKeyUsage check will be done. Note that an
     * {@code X509Certificate} that has no extendedKeyUsage extension
     * implicitly allows all key purposes.
     *
     * @return an immutable {@code Set} of key purpose OIDs in string
     * format (or {@code null})
     * @see #setExtendedKeyUsage
     */
    public Set<String> getExtendedKeyUsage() {
        return keyPurposeSet;
    }

    /**
     * Indicates if the {@code X509Certificate} must contain all
     * or at least one of the subjectAlternativeNames
     * specified in the {@link #setSubjectAlternativeNames
     * setSubjectAlternativeNames} or {@link #addSubjectAlternativeName
     * addSubjectAlternativeName} methods. If {@code true},
     * the {@code X509Certificate} must contain all of the
     * specified subject alternative names. If {@code false}, the
     * {@code X509Certificate} must contain at least one of the
     * specified subject alternative names.
     *
     * @return {@code true} if the flag is enabled;
     * {@code false} if the flag is disabled. The flag is
     * {@code true} by default.
     * @see #setMatchAllSubjectAltNames
     */
    public boolean getMatchAllSubjectAltNames() {
        return matchAllSubjectAltNames;
    }

    /**
     * Returns a copy of the subjectAlternativeNames criterion.
     * The {@code X509Certificate} must contain all or at least one
     * of the specified subjectAlternativeNames, depending on the value
     * of the matchAllNames flag (see {@link #getMatchAllSubjectAltNames
     * getMatchAllSubjectAltNames}). If the value returned is
     * {@code null}, no subjectAlternativeNames check will be performed.
     * <p>
     * If the value returned is not {@code null}, it is a
     * {@code Collection} with
     * one entry for each name to be included in the subject alternative name
     * criterion. Each entry is a {@code List} whose first entry is an
     * {@code Integer} (the name type, 0-8) and whose second
     * entry is a {@code String} or a byte array (the name, in
     * string or ASN.1 DER encoded form, respectively).
     * There can be multiple names of the same type.  Note that the
     * {@code Collection} returned may contain duplicate names (same name
     * and name type).
     * <p>
     * Each subject alternative name in the {@code Collection}
     * may be specified either as a {@code String} or as an ASN.1 encoded
     * byte array. For more details about the formats used, see
     * {@link #addSubjectAlternativeName(int type, String name)
     * addSubjectAlternativeName(int type, String name)} and
     * {@link #addSubjectAlternativeName(int type, byte [] name)
     * addSubjectAlternativeName(int type, byte [] name)}.
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @return a {@code Collection} of names (or {@code null})
     * @see #setSubjectAlternativeNames
     */
    public Collection<List<?>> getSubjectAlternativeNames() {
        if (subjectAlternativeNames == null) {
            return null;
        }
        return cloneNames(subjectAlternativeNames);
    }

    /**
     * Clone an object of the form passed to
     * setSubjectAlternativeNames and setPathToNames.
     * Throw a {@code RuntimeException} if the argument is malformed.
     * <p>
     * This method wraps cloneAndCheckNames, changing any
     * {@code IOException} into a {@code RuntimeException}. This
     * method should be used when the object being
     * cloned has already been checked, so there should never be any exceptions.
     *
     * @param names a {@code Collection} with one entry per name.
     *              Each entry is a {@code List} whose first entry
     *              is an Integer (the name type, 0-8) and whose second
     *              entry is a String or a byte array (the name, in
     *              string or ASN.1 DER encoded form, respectively).
     *              There can be multiple names of the same type. Null
     *              is not an acceptable value.
     * @return a deep copy of the specified {@code Collection}
     * @throws RuntimeException if a parsing error occurs
     */
    private static Set<List<?>> cloneNames(Collection<List<?>> names) {
        try {
            return cloneAndCheckNames(names);
        } catch (IOException e) {
            throw new RuntimeException("cloneNames encountered IOException: " +
                                       e.getMessage());
        }
    }

    /**
     * Clone and check an argument of the form passed to
     * setSubjectAlternativeNames and setPathToNames.
     * Throw an {@code IOException} if the argument is malformed.
     *
     * @param names a {@code Collection} with one entry per name.
     *              Each entry is a {@code List} whose first entry
     *              is an Integer (the name type, 0-8) and whose second
     *              entry is a String or a byte array (the name, in
     *              string or ASN.1 DER encoded form, respectively).
     *              There can be multiple names of the same type.
     *              {@code null} is not an acceptable value.
     * @return a deep copy of the specified {@code Collection}
     * @throws IOException if a parsing error occurs
     */
    private static Set<List<?>> cloneAndCheckNames(Collection<List<?>> names) throws IOException {
        // Copy the Lists and Collection
        Set<List<?>> namesCopy = new HashSet<>();
        for (List<?> o : names)
        {
            namesCopy.add(new ArrayList<>(o));
        }

        // Check the contents of the Lists and clone any byte arrays
        for (List<?> list : namesCopy) {
            @SuppressWarnings("unchecked") // See javadoc for parameter "names".
            List<Object> nameList = (List<Object>)list;
            if (nameList.size() != 2) {
                throw new IOException("name list size not 2");
            }
            Object o = nameList.get(0);
            if (!(o instanceof Integer nameType)) {
                throw new IOException("expected an Integer");
            }
            if ((nameType < 0) || (nameType > 8)) {
                throw new IOException("name type not 0-8");
            }
            Object nameObject = nameList.get(1);
            if (!(nameObject instanceof byte[]) &&
                !(nameObject instanceof String)) {
                if (debug != null) {
                    debug.println("X509CertSelector.cloneAndCheckNames() "
                        + "name not byte array");
                }
                throw new IOException("name not byte array or String");
            }
            if (nameObject instanceof byte[]) {
                nameList.set(1, ((byte[]) nameObject).clone());
            }
        }
        return namesCopy;
    }

    /**
     * Returns the name constraints criterion. The {@code X509Certificate}
     * must have subject and subject alternative names that
     * meet the specified name constraints.
     * <p>
     * The name constraints are returned as a byte array. This byte array
     * contains the DER encoded form of the name constraints, as they
     * would appear in the NameConstraints structure defined in RFC 5280
     * and X.509. The ASN.1 notation for this structure is supplied in the
     * documentation for
     * {@link #setNameConstraints(byte [] bytes) setNameConstraints(byte [] bytes)}.
     * <p>
     * Note that the byte array returned is cloned to protect against
     * subsequent modifications.
     *
     * @return a byte array containing the ASN.1 DER encoding of
     *         a NameConstraints extension used for checking name constraints.
     *         {@code null} if no name constraints check will be performed.
     * @see #setNameConstraints
     */
    public byte[] getNameConstraints() {
        if (ncBytes == null) {
            return null;
        } else {
            return ncBytes.clone();
        }
    }

    /**
     * Returns the basic constraints constraint. If the value is greater than
     * or equal to zero, the {@code X509Certificates} must include a
     * basicConstraints extension with a pathLen of at least this value.
     * If the value is -2, only end-entity certificates are accepted. If
     * the value is -1, no basicConstraints check is done.
     *
     * @return the value for the basic constraints constraint
     * @see #setBasicConstraints
     */
    public int getBasicConstraints() {
        return basicConstraints;
    }

    /**
     * Returns the policy criterion. The {@code X509Certificate} must
     * include at least one of the specified policies in its certificate policies
     * extension. If the {@code Set} returned is empty, then the
     * {@code X509Certificate} must include at least some specified policy
     * in its certificate policies extension. If the {@code Set} returned is
     * {@code null}, no policy check will be performed.
     *
     * @return an immutable {@code Set} of certificate policy OIDs in
     *         string format (or {@code null})
     * @see #setPolicy
     */
    public Set<String> getPolicy() {
        return policySet;
    }

    /**
     * Returns a copy of the pathToNames criterion. The
     * {@code X509Certificate} must not include name constraints that would
     * prohibit building a path to the specified names. If the value
     * returned is {@code null}, no pathToNames check will be performed.
     * <p>
     * If the value returned is not {@code null}, it is a
     * {@code Collection} with one
     * entry for each name to be included in the pathToNames
     * criterion. Each entry is a {@code List} whose first entry is an
     * {@code Integer} (the name type, 0-8) and whose second
     * entry is a {@code String} or a byte array (the name, in
     * string or ASN.1 DER encoded form, respectively).
     * There can be multiple names of the same type. Note that the
     * {@code Collection} returned may contain duplicate names (same
     * name and name type).
     * <p>
     * Each name in the {@code Collection}
     * may be specified either as a {@code String} or as an ASN.1 encoded
     * byte array. For more details about the formats used, see
     * {@link #addPathToName(int type, String name)
     * addPathToName(int type, String name)} and
     * {@link #addPathToName(int type, byte [] name)
     * addPathToName(int type, byte [] name)}.
     * <p>
     * Note that a deep copy is performed on the {@code Collection} to
     * protect against subsequent modifications.
     *
     * @return a {@code Collection} of names (or {@code null})
     * @see #setPathToNames
     */
    public Collection<List<?>> getPathToNames() {
        if (pathToNames == null) {
            return null;
        }
        return cloneNames(pathToNames);
    }

    /**
     * Return a printable representation of the {@code CertSelector}.
     *
     * @return a {@code String} describing the contents of the
     *         {@code CertSelector}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X509CertSelector: [\n");
        if (x509Cert != null) {
            sb.append("  Certificate: " + x509Cert.toString() + "\n");
        }
        if (serialNumber != null) {
            sb.append("  Serial Number: " + serialNumber.toString() + "\n");
        }
        if (issuer != null) {
            sb.append("  Issuer: " + getIssuerAsString() + "\n");
        }
        if (subject != null) {
            sb.append("  Subject: " + getSubjectAsString() + "\n");
        }
        sb.append("  matchAllSubjectAltNames flag: "
                  + String.valueOf(matchAllSubjectAltNames) + "\n");
        if (subjectAlternativeNames != null) {
            sb.append("  SubjectAlternativeNames:\n");
            Iterator<List<?>> i = subjectAlternativeNames.iterator();
            while (i.hasNext()) {
                List<?> list = i.next();
                sb.append("    type " + list.get(0) +
                          ", name " + list.get(1) + "\n");
            }
        }
        if (subjectKeyID != null) {
            HexDumpEncoder enc = new HexDumpEncoder();
            sb.append("  Subject Key Identifier: " +
                      enc.encodeBuffer(subjectKeyID) + "\n");
        }
        if (authorityKeyID != null) {
            HexDumpEncoder enc = new HexDumpEncoder();
            sb.append("  Authority Key Identifier: " +
                      enc.encodeBuffer(authorityKeyID) + "\n");
        }
        if (certificateValid != null) {
            sb.append("  Certificate Valid: " +
                      certificateValid.toString() + "\n");
        }
        if (privateKeyValid != null) {
            sb.append("  Private Key Valid: " +
                      privateKeyValid.toString() + "\n");
        }
        if (subjectPublicKeyAlgID != null) {
            sb.append("  Subject Public Key AlgID: " +
                      subjectPublicKeyAlgID.toString() + "\n");
        }
        if (subjectPublicKey != null) {
            sb.append("  Subject Public Key: " +
                      subjectPublicKey.toString() + "\n");
        }
        if (keyUsage != null) {
            sb.append("  Key Usage: " + keyUsageToString(keyUsage) + "\n");
        }
        if (keyPurposeSet != null) {
            sb.append("  Extended Key Usage: " +
                      keyPurposeSet.toString() + "\n");
        }
        if (policy != null) {
            sb.append("  Policy: " + policy.toString() + "\n");
        }
        if (pathToGeneralNames != null) {
            sb.append("  Path to names:\n");
            Iterator<GeneralNameInterface> i = pathToGeneralNames.iterator();
            while (i.hasNext()) {
                sb.append("    " + i.next() + "\n");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // Copied from sun.security.x509.KeyUsageExtension
    // (without calling the superclass)
    /**
     * Returns a printable representation of the KeyUsage.
     */
    private static String keyUsageToString(boolean[] k) {
        String s = "KeyUsage [\n";
        try {
            if (k[0]) {
                s += "  DigitalSignature\n";
            }
            if (k[1]) {
                s += "  Non_repudiation\n";
            }
            if (k[2]) {
                s += "  Key_Encipherment\n";
            }
            if (k[3]) {
                s += "  Data_Encipherment\n";
            }
            if (k[4]) {
                s += "  Key_Agreement\n";
            }
            if (k[5]) {
                s += "  Key_CertSign\n";
            }
            if (k[6]) {
                s += "  Crl_Sign\n";
            }
            if (k[7]) {
                s += "  Encipher_Only\n";
            }
            if (k[8]) {
                s += "  Decipher_Only\n";
            }
        } catch (ArrayIndexOutOfBoundsException ex) {}

        s += "]\n";

        return (s);
    }

    /**
     * Returns an Extension object given any X509Certificate and extension oid.
     * Throw an {@code IOException} if the extension byte value is
     * malformed.
     *
     * @param cert a {@code X509Certificate}
     * @param extId an {@code integer} which specifies the extension index.
     * Currently, the supported extensions are as follows:
     * index 0 - PrivateKeyUsageExtension
     * index 1 - SubjectAlternativeNameExtension
     * index 2 - NameConstraintsExtension
     * index 3 - CertificatePoliciesExtension
     * index 4 - ExtendedKeyUsageExtension
     * @return an {@code Extension} object whose real type is as specified
     * by the extension oid.
     * @throws IOException if cannot construct the {@code Extension}
     * object with the extension encoding retrieved from the passed in
     * {@code X509Certificate}.
     */
    private static Extension getExtensionObject(X509Certificate cert, KnownOIDs extId)
            throws IOException {
        if (cert instanceof X509CertImpl impl) {
            return switch (extId) {
                case PrivateKeyUsage        -> impl.getPrivateKeyUsageExtension();
                case SubjectAlternativeName -> impl.getSubjectAlternativeNameExtension();
                case NameConstraints        -> impl.getNameConstraintsExtension();
                case CertificatePolicies    -> impl.getCertificatePoliciesExtension();
                case extendedKeyUsage       -> impl.getExtendedKeyUsageExtension();
                default -> null;
            };
        }
        byte[] rawExtVal = cert.getExtensionValue(extId.value());
        if (rawExtVal == null) {
            return null;
        }
        DerInputStream in = new DerInputStream(rawExtVal);
        byte[] encoded = in.getOctetString();
        switch (extId) {
            case PrivateKeyUsage:
                try {
                    return new PrivateKeyUsageExtension(FALSE, encoded);
                } catch (CertificateException ex) {
                    throw new IOException(ex.getMessage());
                }
            case SubjectAlternativeName:
                return new SubjectAlternativeNameExtension(FALSE, encoded);
            case NameConstraints:
                return new NameConstraintsExtension(FALSE, encoded);
            case CertificatePolicies:
                return new CertificatePoliciesExtension(FALSE, encoded);
            case extendedKeyUsage:
                return new ExtendedKeyUsageExtension(FALSE, encoded);
            default:
                return null;
        }
    }

    /**
     * Decides whether a {@code Certificate} should be selected.
     *
     * @param cert the {@code Certificate} to be checked
     * @return {@code true} if the {@code Certificate} should be
     *         selected, {@code false} otherwise
     */
    public boolean match(Certificate cert) {
        if (!(cert instanceof X509Certificate xcert)) {
            return false;
        }

        if (debug != null) {
            debug.println("X509CertSelector.match(SN: "
                + (xcert.getSerialNumber()).toString(16) + "\n  Issuer: "
                + xcert.getIssuerX500Principal() + "\n  Subject: " + xcert.getSubjectX500Principal()
                + ")");
        }

        /* match on X509Certificate */
        if (x509Cert != null) {
            if (!x509Cert.equals(xcert)) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "certs don't match");
                }
                return false;
            }
        }

        /* match on serial number */
        if (serialNumber != null) {
            if (!serialNumber.equals(xcert.getSerialNumber())) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "serial numbers don't match");
                }
                return false;
            }
        }

        /* match on issuer name */
        if (issuer != null) {
            if (!issuer.equals(xcert.getIssuerX500Principal())) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "issuer DNs don't match");
                }
                return false;
            }
        }

        /* match on subject name */
        if (subject != null) {
            if (!subject.equals(xcert.getSubjectX500Principal())) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "subject DNs don't match");
                }
                return false;
            }
        }

        /* match on certificate validity range */
        if (certificateValid != null) {
            try {
                xcert.checkValidity(certificateValid);
            } catch (CertificateException e) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "certificate not within validity period");
                }
                return false;
            }
        }

        /* match on subject public key */
        if (subjectPublicKeyBytes != null) {
            byte[] certKey = xcert.getPublicKey().getEncoded();
            if (!Arrays.equals(subjectPublicKeyBytes, certKey)) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "subject public keys don't match");
                }
                return false;
            }
        }

        boolean result = matchBasicConstraints(xcert)
                      && matchKeyUsage(xcert)
                      && matchExtendedKeyUsage(xcert)
                      && matchSubjectKeyID(xcert)
                      && matchAuthorityKeyID(xcert)
                      && matchPrivateKeyValid(xcert)
                      && matchSubjectPublicKeyAlgID(xcert)
                      && matchPolicy(xcert)
                      && matchSubjectAlternativeNames(xcert)
                      && matchPathToNames(xcert)
                      && matchNameConstraints(xcert);

        if (result && (debug != null)) {
            debug.println("X509CertSelector.match returning: true");
        }
        return result;
    }

    /* match on subject key identifier extension value */
    private boolean matchSubjectKeyID(X509Certificate xcert) {
        if (subjectKeyID == null) {
            return true;
        }
        try {
            byte[] extVal = xcert.getExtensionValue("2.5.29.14");
            if (extVal == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "no subject key ID extension");
                }
                return false;
            }
            DerInputStream in = new DerInputStream(extVal);
            byte[] certSubjectKeyID = in.getOctetString();
            if (certSubjectKeyID == null ||
                    !Arrays.equals(subjectKeyID, certSubjectKeyID)) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: subject key IDs " +
                        "don't match\nX509CertSelector.match: subjectKeyID: " +
                        Arrays.toString(subjectKeyID) +
                        "\nX509CertSelector.match: certSubjectKeyID: " +
                        Arrays.toString(certSubjectKeyID));
                }
                return false;
            }
        } catch (IOException ex) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "exception in subject key ID check");
            }
            return false;
        }
        return true;
    }

    /* match on authority key identifier extension value */
    private boolean matchAuthorityKeyID(X509Certificate xcert) {
        if (authorityKeyID == null) {
            return true;
        }
        try {
            byte[] extVal = xcert.getExtensionValue("2.5.29.35");
            if (extVal == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "no authority key ID extension");
                }
                return false;
            }
            DerInputStream in = new DerInputStream(extVal);
            byte[] certAuthKeyID = in.getOctetString();
            if (certAuthKeyID == null ||
                    !Arrays.equals(authorityKeyID, certAuthKeyID)) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "authority key IDs don't match");
                }
                return false;
            }
        } catch (IOException ex) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "exception in authority key ID check");
            }
            return false;
        }
        return true;
    }

    /* match on private key usage range */
    private boolean matchPrivateKeyValid(X509Certificate xcert) {
        if (privateKeyValid == null) {
            return true;
        }
        PrivateKeyUsageExtension ext = null;
        try {
            ext = (PrivateKeyUsageExtension)
                getExtensionObject(xcert, KnownOIDs.PrivateKeyUsage);
            if (ext != null) {
                ext.valid(privateKeyValid);
            }
        } catch (CertificateExpiredException e1) {
            if (debug != null) {
                String time = "n/a";
                try {
                    Date notAfter = ext.get(PrivateKeyUsageExtension.NOT_AFTER);
                    time = notAfter.toString();
                } catch (CertificateException ex) {
                    // not able to retrieve notAfter value
                }
                debug.println("X509CertSelector.match: private key usage not "
                    + "within validity date; ext.NOT_After: "
                    + time + "; X509CertSelector: "
                    + this.toString());
                e1.printStackTrace();
            }
            return false;
        } catch (CertificateNotYetValidException e2) {
            if (debug != null) {
                String time = "n/a";
                try {
                    Date notBefore = ext.get(PrivateKeyUsageExtension.NOT_BEFORE);
                    time = notBefore.toString();
                } catch (CertificateException ex) {
                    // not able to retrieve notBefore value
                }
                debug.println("X509CertSelector.match: private key usage not "
                    + "within validity date; ext.NOT_BEFORE: "
                    + time + "; X509CertSelector: "
                    + this.toString());
                e2.printStackTrace();
            }
            return false;
        } catch (IOException e4) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in "
                    + "private key usage check; X509CertSelector: "
                    + this.toString());
                e4.printStackTrace();
            }
            return false;
        }
        return true;
    }

    /* match on subject public key algorithm OID */
    private boolean matchSubjectPublicKeyAlgID(X509Certificate xcert) {
        if (subjectPublicKeyAlgID == null) {
            return true;
        }
        try {
            byte[] encodedKey = xcert.getPublicKey().getEncoded();
            DerValue val = new DerValue(encodedKey);
            if (val.tag != DerValue.tag_Sequence) {
                throw new IOException("invalid key format");
            }

            AlgorithmId algID = AlgorithmId.parse(val.data.getDerValue());
            if (debug != null) {
                debug.println("X509CertSelector.match: subjectPublicKeyAlgID = "
                    + subjectPublicKeyAlgID + ", xcert subjectPublicKeyAlgID = "
                    + algID.getOID());
            }
            if (!subjectPublicKeyAlgID.equals(algID.getOID())) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "subject public key alg IDs don't match");
                }
                return false;
            }
        } catch (IOException e5) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in subject "
                    + "public key algorithm OID check");
            }
            return false;
        }
        return true;
    }

    /* match on key usage extension value */
    private boolean matchKeyUsage(X509Certificate xcert) {
        if (keyUsage == null) {
            return true;
        }
        boolean[] certKeyUsage = xcert.getKeyUsage();
        if (certKeyUsage != null) {
            for (int keyBit = 0; keyBit < keyUsage.length; keyBit++) {
                if (keyUsage[keyBit] &&
                    ((keyBit >= certKeyUsage.length) || !certKeyUsage[keyBit])) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: "
                            + "key usage bits don't match");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    /* match on extended key usage purpose OIDs */
    private boolean matchExtendedKeyUsage(X509Certificate xcert) {
        if ((keyPurposeSet == null) || keyPurposeSet.isEmpty()) {
            return true;
        }
        try {
            ExtendedKeyUsageExtension ext =
                (ExtendedKeyUsageExtension)getExtensionObject(xcert,
                                                KnownOIDs.extendedKeyUsage);
            if (ext != null) {
                Vector<ObjectIdentifier> certKeyPurposeVector =
                    ext.get(ExtendedKeyUsageExtension.USAGES);
                if (!certKeyPurposeVector.contains(ANY_EXTENDED_KEY_USAGE)
                        && !certKeyPurposeVector.containsAll(keyPurposeOIDSet)) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: cert failed "
                            + "extendedKeyUsage criterion");
                    }
                    return false;
                }
            }
        } catch (IOException ex) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "IOException in extended key usage check");
            }
            return false;
        }
        return true;
    }

    /* match on subject alternative name extension names */
    private boolean matchSubjectAlternativeNames(X509Certificate xcert) {
        if ((subjectAlternativeNames == null) || subjectAlternativeNames.isEmpty()) {
            return true;
        }
        try {
            SubjectAlternativeNameExtension sanExt =
                (SubjectAlternativeNameExtension) getExtensionObject(
                        xcert, KnownOIDs.SubjectAlternativeName);
            if (sanExt == null) {
                if (debug != null) {
                  debug.println("X509CertSelector.match: "
                      + "no subject alternative name extension");
                }
                return false;
            }
            GeneralNames certNames =
                    sanExt.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
            Iterator<GeneralNameInterface> i =
                                subjectAlternativeGeneralNames.iterator();
            while (i.hasNext()) {
                GeneralNameInterface matchName = i.next();
                boolean found = false;
                for (Iterator<GeneralName> t = certNames.iterator();
                                                t.hasNext() && !found; ) {
                    GeneralNameInterface certName = (t.next()).getName();
                    found = certName.equals(matchName);
                }
                if (!found && (matchAllSubjectAltNames || !i.hasNext())) {
                    if (debug != null) {
                      debug.println("X509CertSelector.match: subject alternative "
                          + "name " + matchName + " not found");
                    }
                    return false;
                } else if (found && !matchAllSubjectAltNames) {
                    break;
                }
            }
        } catch (IOException ex) {
            if (debug != null)
                debug.println("X509CertSelector.match: IOException in subject "
                    + "alternative name check");
            return false;
        }
        return true;
    }

    /* match on name constraints */
    private boolean matchNameConstraints(X509Certificate xcert) {
        if (nc == null) {
            return true;
        }
        try {
            if (!nc.verify(xcert)) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: "
                        + "name constraints not satisfied");
                }
                return false;
            }
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "IOException in name constraints check");
            }
            return false;
        }
        return true;
    }

    /* match on policy OIDs */
    private boolean matchPolicy(X509Certificate xcert) {
        if (policy == null) {
            return true;
        }
        try {
            CertificatePoliciesExtension ext = (CertificatePoliciesExtension)
                getExtensionObject(xcert, KnownOIDs.CertificatePolicies);
            if (ext == null) {
                if (debug != null) {
                  debug.println("X509CertSelector.match: "
                      + "no certificate policy extension");
                }
                return false;
            }
            List<PolicyInformation> policies = ext.get(CertificatePoliciesExtension.POLICIES);
            /*
             * Convert the Vector of PolicyInformation to a Vector
             * of CertificatePolicyIds for easier comparison.
             */
            List<CertificatePolicyId> policyIDs = new ArrayList<>(policies.size());
            for (PolicyInformation info : policies) {
                policyIDs.add(info.getPolicyIdentifier());
            }
            if (policy != null) {
                boolean foundOne = false;
                /*
                 * if the user passes in an empty policy Set, then
                 * we just want to make sure that the candidate certificate
                 * has some policy OID in its CertPoliciesExtension
                 */
                if (policy.getCertPolicyIds().isEmpty()) {
                    if (policyIDs.isEmpty()) {
                        if (debug != null) {
                            debug.println("X509CertSelector.match: "
                                + "cert failed policyAny criterion");
                        }
                        return false;
                    }
                } else {
                    for (CertificatePolicyId id : policy.getCertPolicyIds()) {
                        if (policyIDs.contains(id)) {
                            foundOne = true;
                            break;
                        }
                    }
                    if (!foundOne) {
                        if (debug != null) {
                            debug.println("X509CertSelector.match: "
                                + "cert failed policyAny criterion");
                        }
                        return false;
                    }
                }
            }
        } catch (IOException ex) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "IOException in certificate policy ID check");
            }
            return false;
        }
        return true;
    }

    /* match on pathToNames */
    private boolean matchPathToNames(X509Certificate xcert) {
        if (pathToGeneralNames == null) {
            return true;
        }
        try {
            NameConstraintsExtension ext = (NameConstraintsExtension)
                getExtensionObject(xcert, KnownOIDs.NameConstraints);
            if (ext == null) {
                return true;
            }
            if ((debug != null) && Debug.isOn("certpath")) {
                debug.println("X509CertSelector.match pathToNames:\n");
                Iterator<GeneralNameInterface> i =
                                        pathToGeneralNames.iterator();
                while (i.hasNext()) {
                    debug.println("    " + i.next() + "\n");
                }
            }

            GeneralSubtrees permitted =
                    ext.get(NameConstraintsExtension.PERMITTED_SUBTREES);
            GeneralSubtrees excluded =
                    ext.get(NameConstraintsExtension.EXCLUDED_SUBTREES);
            if (excluded != null) {
                if (matchExcluded(excluded) == false) {
                    return false;
                }
            }
            if (permitted != null) {
                if (matchPermitted(permitted) == false) {
                    return false;
                }
            }
        } catch (IOException ex) {
            if (debug != null) {
                debug.println("X509CertSelector.match: "
                    + "IOException in name constraints check");
            }
            return false;
        }
        return true;
    }

    private boolean matchExcluded(GeneralSubtrees excluded) {
        /*
         * Enumerate through excluded and compare each entry
         * to all pathToNames. If any pathToName is within any of the
         * subtrees listed in excluded, return false.
         */
        for (Iterator<GeneralSubtree> t = excluded.iterator(); t.hasNext(); ) {
            GeneralSubtree tree = t.next();
            GeneralNameInterface excludedName = tree.getName().getName();
            Iterator<GeneralNameInterface> i = pathToGeneralNames.iterator();
            while (i.hasNext()) {
                GeneralNameInterface pathToName = i.next();
                if (excludedName.getType() == pathToName.getType()) {
                    switch (pathToName.constrains(excludedName)) {
                    case GeneralNameInterface.NAME_WIDENS:
                    case GeneralNameInterface.NAME_MATCH:
                        if (debug != null) {
                            debug.println("X509CertSelector.match: name constraints "
                                + "inhibit path to specified name");
                            debug.println("X509CertSelector.match: excluded name: " +
                                pathToName);
                        }
                        return false;
                    default:
                    }
                }
            }
        }
        return true;
    }

    private boolean matchPermitted(GeneralSubtrees permitted) {
        /*
         * Enumerate through pathToNames, checking that each pathToName
         * is in at least one of the subtrees listed in permitted.
         * If not, return false. However, if no subtrees of a given type
         * are listed, all names of that type are permitted.
         */
        Iterator<GeneralNameInterface> i = pathToGeneralNames.iterator();
        while (i.hasNext()) {
            GeneralNameInterface pathToName = i.next();
            Iterator<GeneralSubtree> t = permitted.iterator();
            boolean permittedNameFound = false;
            boolean nameTypeFound = false;
            String names = "";
            while (t.hasNext() && !permittedNameFound) {
                GeneralSubtree tree = t.next();
                GeneralNameInterface permittedName = tree.getName().getName();
                if (permittedName.getType() == pathToName.getType()) {
                    nameTypeFound = true;
                    names = names + "  " + permittedName;
                    switch (pathToName.constrains(permittedName)) {
                    case GeneralNameInterface.NAME_WIDENS:
                    case GeneralNameInterface.NAME_MATCH:
                        permittedNameFound = true;
                        break;
                    default:
                    }
                }
            }
            if (!permittedNameFound && nameTypeFound) {
                if (debug != null)
                  debug.println("X509CertSelector.match: " +
                            "name constraints inhibit path to specified name; " +
                            "permitted names of type " + pathToName.getType() +
                            ": " + names);
                return false;
            }
        }
        return true;
    }

    /* match on basic constraints */
    private boolean matchBasicConstraints(X509Certificate xcert) {
        if (basicConstraints == -1) {
            return true;
        }
        int maxPathLen = xcert.getBasicConstraints();
        if (basicConstraints == -2) {
            if (maxPathLen != -1) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: not an EE cert");
                }
                return false;
            }
        } else {
            if (maxPathLen < basicConstraints) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: cert's maxPathLen " +
                            "is less than the min maxPathLen set by " +
                            "basicConstraints. " +
                            "(" + maxPathLen + " < " + basicConstraints + ")");
                }
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked") // Safe casts assuming clone() works correctly
    private static <T> Set<T> cloneSet(Set<T> set) {
        if (set instanceof HashSet) {
            Object clone = ((HashSet<T>)set).clone();
            return (Set<T>)clone;
        } else {
            return new HashSet<T>(set);
        }
    }

    /**
     * Returns a copy of this object.
     *
     * @return the copy
     */
    public Object clone() {
        try {
            X509CertSelector copy = (X509CertSelector)super.clone();
            // Must clone these because addPathToName et al. modify them
            if (subjectAlternativeNames != null) {
                copy.subjectAlternativeNames =
                        cloneSet(subjectAlternativeNames);
                copy.subjectAlternativeGeneralNames =
                        cloneSet(subjectAlternativeGeneralNames);
            }
            if (pathToGeneralNames != null) {
                copy.pathToNames = cloneSet(pathToNames);
                copy.pathToGeneralNames = cloneSet(pathToGeneralNames);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            /* Cannot happen */
            throw new InternalError(e.toString(), e);
        }
    }
}
