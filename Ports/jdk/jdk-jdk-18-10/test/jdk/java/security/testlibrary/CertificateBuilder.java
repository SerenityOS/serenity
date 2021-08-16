/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.testlibrary;

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.Extension;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;

import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AccessDescription;
import sun.security.x509.AlgorithmId;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.DNSName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.KeyUsageExtension;
import sun.security.x509.SerialNumber;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.URIName;
import sun.security.x509.KeyIdentifier;

/**
 * Helper class that builds and signs X.509 certificates.
 *
 * A CertificateBuilder is created with a default constructor, and then
 * uses additional public methods to set the public key, desired validity
 * dates, serial number and extensions.  It is expected that the caller will
 * have generated the necessary key pairs prior to using a CertificateBuilder
 * to generate certificates.
 *
 * The following methods are mandatory before calling build():
 * <UL>
 * <LI>{@link #setSubjectName(java.lang.String)}
 * <LI>{@link #setPublicKey(java.security.PublicKey)}
 * <LI>{@link #setNotBefore(java.util.Date)} and
 * {@link #setNotAfter(java.util.Date)}, or
 * {@link #setValidity(java.util.Date, java.util.Date)}
 * <LI>{@link #setSerialNumber(java.math.BigInteger)}
 * </UL><BR>
 *
 * Additionally, the caller can either provide a {@link List} of
 * {@link Extension} objects, or use the helper classes to add specific
 * extension types.
 *
 * When all required and desired parameters are set, the
 * {@link #build(java.security.cert.X509Certificate, java.security.PrivateKey,
 * java.lang.String)} method can be used to create the {@link X509Certificate}
 * object.
 *
 * Multiple certificates may be cut from the same settings using subsequent
 * calls to the build method.  Settings may be cleared using the
 * {@link #reset()} method.
 */
public class CertificateBuilder {
    private final CertificateFactory factory;

    private X500Principal subjectName = null;
    private BigInteger serialNumber = null;
    private PublicKey publicKey = null;
    private Date notBefore = null;
    private Date notAfter = null;
    private final Map<String, Extension> extensions = new HashMap<>();
    private byte[] tbsCertBytes;
    private byte[] signatureBytes;

    /**
     * Default constructor for a {@code CertificateBuilder} object.
     *
     * @throws CertificateException if the underlying {@link CertificateFactory}
     * cannot be instantiated.
     */
    public CertificateBuilder() throws CertificateException {
        factory = CertificateFactory.getInstance("X.509");
    }

    /**
     * Set the subject name for the certificate.
     *
     * @param name An {@link X500Principal} to be used as the subject name
     * on this certificate.
     */
    public void setSubjectName(X500Principal name) {
        subjectName = name;
    }

    /**
     * Set the subject name for the certificate.
     *
     * @param name The subject name in RFC 2253 format
     */
    public void setSubjectName(String name) {
        subjectName = new X500Principal(name);
    }

    /**
     * Set the public key for this certificate.
     *
     * @param pubKey The {@link PublicKey} to be used on this certificate.
     */
    public void setPublicKey(PublicKey pubKey) {
        publicKey = Objects.requireNonNull(pubKey, "Caught null public key");
    }

    /**
     * Set the NotBefore date on the certificate.
     *
     * @param nbDate A {@link Date} object specifying the start of the
     * certificate validity period.
     */
    public void setNotBefore(Date nbDate) {
        Objects.requireNonNull(nbDate, "Caught null notBefore date");
        notBefore = (Date)nbDate.clone();
    }

    /**
     * Set the NotAfter date on the certificate.
     *
     * @param naDate A {@link Date} object specifying the end of the
     * certificate validity period.
     */
    public void setNotAfter(Date naDate) {
        Objects.requireNonNull(naDate, "Caught null notAfter date");
        notAfter = (Date)naDate.clone();
    }

    /**
     * Set the validity period for the certificate
     *
     * @param nbDate A {@link Date} object specifying the start of the
     * certificate validity period.
     * @param naDate A {@link Date} object specifying the end of the
     * certificate validity period.
     */
    public void setValidity(Date nbDate, Date naDate) {
        setNotBefore(nbDate);
        setNotAfter(naDate);
    }

    /**
     * Set the serial number on the certificate.
     *
     * @param serial A serial number in {@link BigInteger} form.
     */
    public void setSerialNumber(BigInteger serial) {
        Objects.requireNonNull(serial, "Caught null serial number");
        serialNumber = serial;
    }


    /**
     * Add a single extension to the certificate.
     *
     * @param ext The extension to be added.
     */
    public void addExtension(Extension ext) {
        Objects.requireNonNull(ext, "Caught null extension");
        extensions.put(ext.getId(), ext);
    }

    /**
     * Add multiple extensions contained in a {@code List}.
     *
     * @param extList The {@link List} of extensions to be added to
     * the certificate.
     */
    public void addExtensions(List<Extension> extList) {
        Objects.requireNonNull(extList, "Caught null extension list");
        for (Extension ext : extList) {
            extensions.put(ext.getId(), ext);
        }
    }

    /**
     * Helper method to add DNSName types for the SAN extension
     *
     * @param dnsNames A {@code List} of names to add as DNSName types
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addSubjectAltNameDNSExt(List<String> dnsNames) throws IOException {
        if (!dnsNames.isEmpty()) {
            GeneralNames gNames = new GeneralNames();
            for (String name : dnsNames) {
                gNames.add(new GeneralName(new DNSName(name)));
            }
            addExtension(new SubjectAlternativeNameExtension(false,
                    gNames));
        }
    }

    /**
     * Helper method to add one or more OCSP URIs to the Authority Info Access
     * certificate extension.
     *
     * @param locations A list of one or more OCSP responder URIs as strings
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addAIAExt(List<String> locations)
            throws IOException {
        if (!locations.isEmpty()) {
            List<AccessDescription> acDescList = new ArrayList<>();
            for (String ocspUri : locations) {
                acDescList.add(new AccessDescription(
                        AccessDescription.Ad_OCSP_Id,
                        new GeneralName(new URIName(ocspUri))));
            }
            addExtension(new AuthorityInfoAccessExtension(acDescList));
        }
    }

    /**
     * Set a Key Usage extension for the certificate.  The extension will
     * be marked critical.
     *
     * @param bitSettings Boolean array for all nine bit settings in the order
     * documented in RFC 5280 section 4.2.1.3.
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addKeyUsageExt(boolean[] bitSettings) throws IOException {
        addExtension(new KeyUsageExtension(bitSettings));
    }

    /**
     * Set the Basic Constraints Extension for a certificate.
     *
     * @param crit {@code true} if critical, {@code false} otherwise
     * @param isCA {@code true} if the extension will be on a CA certificate,
     * {@code false} otherwise
     * @param maxPathLen The maximum path length issued by this CA.  Values
     * less than zero will omit this field from the resulting extension and
     * no path length constraint will be asserted.
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addBasicConstraintsExt(boolean crit, boolean isCA,
            int maxPathLen) throws IOException {
        addExtension(new BasicConstraintsExtension(crit, isCA, maxPathLen));
    }

    /**
     * Add the Authority Key Identifier extension.
     *
     * @param authorityCert The certificate of the issuing authority.
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addAuthorityKeyIdExt(X509Certificate authorityCert)
            throws IOException {
        addAuthorityKeyIdExt(authorityCert.getPublicKey());
    }

    /**
     * Add the Authority Key Identifier extension.
     *
     * @param authorityKey The public key of the issuing authority.
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addAuthorityKeyIdExt(PublicKey authorityKey) throws IOException {
        KeyIdentifier kid = new KeyIdentifier(authorityKey);
        addExtension(new AuthorityKeyIdentifierExtension(kid, null, null));
    }

    /**
     * Add the Subject Key Identifier extension.
     *
     * @param subjectKey The public key to be used in the resulting certificate
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addSubjectKeyIdExt(PublicKey subjectKey) throws IOException {
        byte[] keyIdBytes = new KeyIdentifier(subjectKey).getIdentifier();
        addExtension(new SubjectKeyIdentifierExtension(keyIdBytes));
    }

    /**
     * Add the Extended Key Usage extension.
     *
     * @param ekuOids A {@link List} of object identifiers in string form.
     *
     * @throws IOException if an encoding error occurs.
     */
    public void addExtendedKeyUsageExt(List<String> ekuOids)
            throws IOException {
        if (!ekuOids.isEmpty()) {
            Vector<ObjectIdentifier> oidVector = new Vector<>();
            for (String oid : ekuOids) {
                oidVector.add(ObjectIdentifier.of(oid));
            }
            addExtension(new ExtendedKeyUsageExtension(oidVector));
        }
    }

    /**
     * Clear all settings and return the {@code CertificateBuilder} to
     * its default state.
     */
    public void reset() {
        extensions.clear();
        subjectName = null;
        notBefore = null;
        notAfter = null;
        serialNumber = null;
        publicKey = null;
        signatureBytes = null;
        tbsCertBytes = null;
    }

    /**
     * Build the certificate.
     *
     * @param issuerCert The certificate of the issuing authority, or
     * {@code null} if the resulting certificate is self-signed.
     * @param issuerKey The private key of the issuing authority
     * @param algName The signature algorithm name
     *
     * @return The resulting {@link X509Certificate}
     *
     * @throws IOException if an encoding error occurs.
     * @throws CertificateException If the certificate cannot be generated
     * by the underlying {@link CertificateFactory}
     * @throws NoSuchAlgorithmException If an invalid signature algorithm
     * is provided.
     */
    public X509Certificate build(X509Certificate issuerCert,
            PrivateKey issuerKey, String algName)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        // TODO: add some basic checks (key usage, basic constraints maybe)

        AlgorithmId signAlg = AlgorithmId.get(algName);
        byte[] encodedCert = encodeTopLevel(issuerCert, issuerKey, signAlg);
        ByteArrayInputStream bais = new ByteArrayInputStream(encodedCert);
        return (X509Certificate)factory.generateCertificate(bais);
    }

    /**
     * Encode the contents of the outer-most ASN.1 SEQUENCE:
     *
     * <PRE>
     *  Certificate  ::=  SEQUENCE  {
     *      tbsCertificate       TBSCertificate,
     *      signatureAlgorithm   AlgorithmIdentifier,
     *      signatureValue       BIT STRING  }
     * </PRE>
     *
     * @param issuerCert The certificate of the issuing authority, or
     * {@code null} if the resulting certificate is self-signed.
     * @param issuerKey The private key of the issuing authority
     * @param signAlg The signature algorithm object
     *
     * @return The DER-encoded X.509 certificate
     *
     * @throws CertificateException If an error occurs during the
     * signing process.
     * @throws IOException if an encoding error occurs.
     */
    private byte[] encodeTopLevel(X509Certificate issuerCert,
            PrivateKey issuerKey, AlgorithmId signAlg)
            throws CertificateException, IOException {
        DerOutputStream outerSeq = new DerOutputStream();
        DerOutputStream topLevelItems = new DerOutputStream();

        tbsCertBytes = encodeTbsCert(issuerCert, signAlg);
        topLevelItems.write(tbsCertBytes);
        try {
            signatureBytes = signCert(issuerKey, signAlg);
        } catch (GeneralSecurityException ge) {
            throw new CertificateException(ge);
        }
        signAlg.derEncode(topLevelItems);
        topLevelItems.putBitString(signatureBytes);
        outerSeq.write(DerValue.tag_Sequence, topLevelItems);

        return outerSeq.toByteArray();
    }

    /**
     * Encode the bytes for the TBSCertificate structure:
     * <PRE>
     *  TBSCertificate  ::=  SEQUENCE  {
     *      version         [0]  EXPLICIT Version DEFAULT v1,
     *      serialNumber         CertificateSerialNumber,
     *      signature            AlgorithmIdentifier,
     *      issuer               Name,
     *      validity             Validity,
     *      subject              Name,
     *      subjectPublicKeyInfo SubjectPublicKeyInfo,
     *      issuerUniqueID  [1]  IMPLICIT UniqueIdentifier OPTIONAL,
     *                        -- If present, version MUST be v2 or v3
     *      subjectUniqueID [2]  IMPLICIT UniqueIdentifier OPTIONAL,
     *                        -- If present, version MUST be v2 or v3
     *      extensions      [3]  EXPLICIT Extensions OPTIONAL
     *                        -- If present, version MUST be v3
     *      }
     *
     * @param issuerCert The certificate of the issuing authority, or
     * {@code null} if the resulting certificate is self-signed.
     * @param signAlg The signature algorithm object
     *
     * @return The DER-encoded bytes for the TBSCertificate structure
     *
     * @throws IOException if an encoding error occurs.
     */
    private byte[] encodeTbsCert(X509Certificate issuerCert,
            AlgorithmId signAlg) throws IOException {
        DerOutputStream tbsCertSeq = new DerOutputStream();
        DerOutputStream tbsCertItems = new DerOutputStream();

        // Hardcode to V3
        byte[] v3int = {0x02, 0x01, 0x02};
        tbsCertItems.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                (byte)0), v3int);

        // Serial Number
        SerialNumber sn = new SerialNumber(serialNumber);
        sn.encode(tbsCertItems);

        // Algorithm ID
        signAlg.derEncode(tbsCertItems);

        // Issuer Name
        if (issuerCert != null) {
            tbsCertItems.write(
                    issuerCert.getSubjectX500Principal().getEncoded());
        } else {
            // Self-signed
            tbsCertItems.write(subjectName.getEncoded());
        }

        // Validity period (set as UTCTime)
        DerOutputStream valSeq = new DerOutputStream();
        valSeq.putUTCTime(notBefore);
        valSeq.putUTCTime(notAfter);
        tbsCertItems.write(DerValue.tag_Sequence, valSeq);

        // Subject Name
        tbsCertItems.write(subjectName.getEncoded());

        // SubjectPublicKeyInfo
        tbsCertItems.write(publicKey.getEncoded());

        // TODO: Extensions!
        encodeExtensions(tbsCertItems);

        // Wrap it all up in a SEQUENCE and return the bytes
        tbsCertSeq.write(DerValue.tag_Sequence, tbsCertItems);
        return tbsCertSeq.toByteArray();
    }

    /**
     * Encode the extensions segment for an X.509 Certificate:
     *
     * <PRE>
     *  Extensions  ::=  SEQUENCE SIZE (1..MAX) OF Extension
     *
     *  Extension  ::=  SEQUENCE  {
     *      extnID      OBJECT IDENTIFIER,
     *      critical    BOOLEAN DEFAULT FALSE,
     *      extnValue   OCTET STRING
     *                  -- contains the DER encoding of an ASN.1 value
     *                  -- corresponding to the extension type identified
     *                  -- by extnID
     *      }
     * </PRE>
     *
     * @param tbsStream The {@code DerOutputStream} that holds the
     * TBSCertificate contents.
     *
     * @throws IOException if an encoding error occurs.
     */
    private void encodeExtensions(DerOutputStream tbsStream)
            throws IOException {
        DerOutputStream extSequence = new DerOutputStream();
        DerOutputStream extItems = new DerOutputStream();

        for (Extension ext : extensions.values()) {
            ext.encode(extItems);
        }
        extSequence.write(DerValue.tag_Sequence, extItems);
        tbsStream.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                (byte)3), extSequence);
    }

    /**
     * Digitally sign the X.509 certificate.
     *
     * @param issuerKey The private key of the issuing authority
     * @param signAlg The signature algorithm object
     *
     * @return The digital signature bytes.
     *
     * @throws GeneralSecurityException If any errors occur during the
     * digital signature process.
     */
    private byte[] signCert(PrivateKey issuerKey, AlgorithmId signAlg)
            throws GeneralSecurityException {
        Signature sig = Signature.getInstance(signAlg.getName());
        sig.initSign(issuerKey);
        sig.update(tbsCertBytes);
        return sig.sign();
    }
 }
