/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;

import java.security.cert.*;
import java.util.*;

import sun.security.util.*;
import sun.security.util.HexDumpEncoder;


/**
 * The X509CertInfo class represents X.509 certificate information.
 *
 * <P>X.509 certificates have several base data elements, including:
 *
 * <UL>
 * <LI>The <em>Subject Name</em>, an X.500 Distinguished Name for
 *      the entity (subject) for which the certificate was issued.
 *
 * <LI>The <em>Subject Public Key</em>, the public key of the subject.
 *      This is one of the most important parts of the certificate.
 *
 * <LI>The <em>Validity Period</em>, a time period (e.g. six months)
 *      within which the certificate is valid (unless revoked).
 *
 * <LI>The <em>Issuer Name</em>, an X.500 Distinguished Name for the
 *      Certificate Authority (CA) which issued the certificate.
 *
 * <LI>A <em>Serial Number</em> assigned by the CA, for use in
 *      certificate revocation and other applications.
 * </UL>
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see CertAttrSet
 * @see X509CertImpl
 */
public class X509CertInfo implements CertAttrSet<String> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info";
    // Certificate attribute names
    public static final String NAME = "info";
    public static final String DN_NAME = "dname";
    public static final String VERSION = CertificateVersion.NAME;
    public static final String SERIAL_NUMBER = CertificateSerialNumber.NAME;
    public static final String ALGORITHM_ID = CertificateAlgorithmId.NAME;
    public static final String ISSUER = "issuer";
    public static final String SUBJECT = "subject";
    public static final String VALIDITY = CertificateValidity.NAME;
    public static final String KEY = CertificateX509Key.NAME;
    public static final String ISSUER_ID = "issuerID";
    public static final String SUBJECT_ID = "subjectID";
    public static final String EXTENSIONS = CertificateExtensions.NAME;

    // X509.v1 data
    protected CertificateVersion version = new CertificateVersion();
    protected CertificateSerialNumber   serialNum = null;
    protected CertificateAlgorithmId    algId = null;
    protected X500Name                  issuer = null;
    protected X500Name                  subject = null;
    protected CertificateValidity       interval = null;
    protected CertificateX509Key        pubKey = null;

    // X509.v2 & v3 extensions
    protected UniqueIdentity   issuerUniqueId = null;
    protected UniqueIdentity  subjectUniqueId = null;

    // X509.v3 extensions
    protected CertificateExtensions     extensions = null;

    // Attribute numbers for internal manipulation
    private static final int ATTR_VERSION = 1;
    private static final int ATTR_SERIAL = 2;
    private static final int ATTR_ALGORITHM = 3;
    private static final int ATTR_ISSUER = 4;
    private static final int ATTR_VALIDITY = 5;
    private static final int ATTR_SUBJECT = 6;
    private static final int ATTR_KEY = 7;
    private static final int ATTR_ISSUER_ID = 8;
    private static final int ATTR_SUBJECT_ID = 9;
    private static final int ATTR_EXTENSIONS = 10;

    // DER encoded CertificateInfo data
    private byte[]      rawCertInfo = null;

    // The certificate attribute name to integer mapping stored here
    private static final Map<String,Integer> map = new HashMap<String,Integer>();
    static {
        map.put(VERSION, Integer.valueOf(ATTR_VERSION));
        map.put(SERIAL_NUMBER, Integer.valueOf(ATTR_SERIAL));
        map.put(ALGORITHM_ID, Integer.valueOf(ATTR_ALGORITHM));
        map.put(ISSUER, Integer.valueOf(ATTR_ISSUER));
        map.put(VALIDITY, Integer.valueOf(ATTR_VALIDITY));
        map.put(SUBJECT, Integer.valueOf(ATTR_SUBJECT));
        map.put(KEY, Integer.valueOf(ATTR_KEY));
        map.put(ISSUER_ID, Integer.valueOf(ATTR_ISSUER_ID));
        map.put(SUBJECT_ID, Integer.valueOf(ATTR_SUBJECT_ID));
        map.put(EXTENSIONS, Integer.valueOf(ATTR_EXTENSIONS));
    }

    /**
     * Construct an uninitialized X509CertInfo on which <a href="#decode">
     * decode</a> must later be called (or which may be deserialized).
     */
    public X509CertInfo() { }

    /**
     * Unmarshals a certificate from its encoded form, parsing the
     * encoded bytes.  This form of constructor is used by agents which
     * need to examine and use certificate contents.  That is, this is
     * one of the more commonly used constructors.  Note that the buffer
     * must include only a certificate, and no "garbage" may be left at
     * the end.  If you need to ignore data at the end of a certificate,
     * use another constructor.
     *
     * @param cert the encoded bytes, with no trailing data.
     * @exception CertificateParsingException on parsing errors.
     */
    public X509CertInfo(byte[] cert) throws CertificateParsingException {
        try {
            DerValue    in = new DerValue(cert);

            parse(in);
        } catch (IOException e) {
            throw new CertificateParsingException(e);
        }
    }

    /**
     * Unmarshal a certificate from its encoded form, parsing a DER value.
     * This form of constructor is used by agents which need to examine
     * and use certificate contents.
     *
     * @param derVal the der value containing the encoded cert.
     * @exception CertificateParsingException on parsing errors.
     */
    public X509CertInfo(DerValue derVal) throws CertificateParsingException {
        try {
            parse(derVal);
        } catch (IOException e) {
            throw new CertificateParsingException(e);
        }
    }

    /**
     * Appends the certificate to an output stream.
     *
     * @param out an output stream to which the certificate is appended.
     * @exception CertificateException on encoding errors.
     * @exception IOException on other errors.
     */
    public void encode(OutputStream out)
    throws CertificateException, IOException {
        if (rawCertInfo == null) {
            DerOutputStream tmp = new DerOutputStream();
            emit(tmp);
            rawCertInfo = tmp.toByteArray();
        }
        out.write(rawCertInfo.clone());
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(VERSION);
        elements.addElement(SERIAL_NUMBER);
        elements.addElement(ALGORITHM_ID);
        elements.addElement(ISSUER);
        elements.addElement(VALIDITY);
        elements.addElement(SUBJECT);
        elements.addElement(KEY);
        elements.addElement(ISSUER_ID);
        elements.addElement(SUBJECT_ID);
        elements.addElement(EXTENSIONS);

        return elements.elements();
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return(NAME);
    }

    /**
     * Returns the encoded certificate info.
     *
     * @exception CertificateEncodingException on encoding information errors.
     */
    public byte[] getEncodedInfo() throws CertificateEncodingException {
        try {
            if (rawCertInfo == null) {
                DerOutputStream tmp = new DerOutputStream();
                emit(tmp);
                rawCertInfo = tmp.toByteArray();
            }
            return rawCertInfo.clone();
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        } catch (CertificateException e) {
            throw new CertificateEncodingException(e.toString());
        }
    }

    /**
     * Compares two X509CertInfo objects.  This is false if the
     * certificates are not both X.509 certs, otherwise it
     * compares them as binary data.
     *
     * @param other the object being compared with this one
     * @return true iff the certificates are equivalent
     */
    public boolean equals(Object other) {
        if (other instanceof X509CertInfo) {
            return equals((X509CertInfo) other);
        } else {
            return false;
        }
    }

    /**
     * Compares two certificates, returning false if any data
     * differs between the two.
     *
     * @param other the object being compared with this one
     * @return true iff the certificates are equivalent
     */
    public boolean equals(X509CertInfo other) {
        if (this == other) {
            return(true);
        } else if (rawCertInfo == null || other.rawCertInfo == null) {
            return(false);
        } else if (rawCertInfo.length != other.rawCertInfo.length) {
            return(false);
        }
        for (int i = 0; i < rawCertInfo.length; i++) {
            if (rawCertInfo[i] != other.rawCertInfo[i]) {
                return(false);
            }
        }
        return(true);
    }

    /**
     * Calculates a hash code value for the object.  Objects
     * which are equal will also have the same hashcode.
     */
    public int hashCode() {
        int     retval = 0;

        for (int i = 1; i < rawCertInfo.length; i++) {
            retval += rawCertInfo[i] * i;
        }
        return(retval);
    }

    /**
     * Returns a printable representation of the certificate.
     */
    public String toString() {

        if (subject == null || pubKey == null || interval == null
            || issuer == null || algId == null || serialNum == null) {
                throw new NullPointerException("X.509 cert is incomplete");
        }
        StringBuilder sb = new StringBuilder();

        sb.append("[\n")
            .append("  ").append(version).append('\n')
            .append("  Subject: ").append(subject).append('\n')
            .append("  Signature Algorithm: ").append(algId).append('\n')
            .append("  Key:  ").append(pubKey).append('\n')
            .append("  ").append(interval).append('\n')
            .append("  Issuer: ").append(issuer).append('\n')
            .append("  ").append(serialNum).append('\n');

        // optional v2, v3 extras
        if (issuerUniqueId != null) {
            sb.append("  Issuer Id:\n").append(issuerUniqueId).append('\n');
        }
        if (subjectUniqueId != null) {
            sb.append("  Subject Id:\n").append(subjectUniqueId).append('\n');
        }
        if (extensions != null) {
            Collection<Extension> allExts = extensions.getAllExtensions();
            Extension[] exts = allExts.toArray(new Extension[0]);
            sb.append("\nCertificate Extensions: ").append(exts.length);
            for (int i = 0; i < exts.length; i++) {
                sb.append("\n[").append(i+1).append("]: ");
                Extension ext = exts[i];
                try {
                    if (OIDMap.getClass(ext.getExtensionId()) == null) {
                        sb.append(ext);
                        byte[] extValue = ext.getExtensionValue();
                        if (extValue != null) {
                            DerOutputStream out = new DerOutputStream();
                            out.putOctetString(extValue);
                            extValue = out.toByteArray();
                            HexDumpEncoder enc = new HexDumpEncoder();
                            sb.append("Extension unknown: ")
                                .append("DER encoded OCTET string =\n")
                                .append(enc.encodeBuffer(extValue))
                                .append('\n');
                        }
                    } else {
                        sb.append(ext); //sub-class exists
                    }
                } catch (Exception e) {
                    sb.append(", Error parsing this extension");
                }
            }
            Map<String,Extension> invalid = extensions.getUnparseableExtensions();
            if (invalid.isEmpty() == false) {
                sb.append("\nUnparseable certificate extensions: ")
                    .append(invalid.size());
                int i = 1;
                for (Extension ext : invalid.values()) {
                    sb.append("\n[")
                        .append(i++)
                        .append("]: ")
                        .append(ext);
                }
            }
        }
        sb.append("\n]");
        return sb.toString();
    }

    /**
     * Set the certificate attribute.
     *
     * @param name the name of the Certificate attribute.
     * @param val the value of the Certificate attribute.
     * @exception CertificateException on invalid attributes.
     * @exception IOException on other errors.
     */
    public void set(String name, Object val)
    throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);

        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateException("Attribute name not recognized: "
                                           + name);
        }
        // set rawCertInfo to null, so that we are forced to re-encode
        rawCertInfo = null;
        String suffix = attrName.getSuffix();

        switch (attr) {
        case ATTR_VERSION:
            if (suffix == null) {
                setVersion(val);
            } else {
                version.set(suffix, val);
            }
            break;

        case ATTR_SERIAL:
            if (suffix == null) {
                setSerialNumber(val);
            } else {
                serialNum.set(suffix, val);
            }
            break;

        case ATTR_ALGORITHM:
            if (suffix == null) {
                setAlgorithmId(val);
            } else {
                algId.set(suffix, val);
            }
            break;

        case ATTR_ISSUER:
            setIssuer(val);
            break;

        case ATTR_VALIDITY:
            if (suffix == null) {
                setValidity(val);
            } else {
                interval.set(suffix, val);
            }
            break;

        case ATTR_SUBJECT:
            setSubject(val);
            break;

        case ATTR_KEY:
            if (suffix == null) {
                setKey(val);
            } else {
                pubKey.set(suffix, val);
            }
            break;

        case ATTR_ISSUER_ID:
            setIssuerUniqueId(val);
            break;

        case ATTR_SUBJECT_ID:
            setSubjectUniqueId(val);
            break;

        case ATTR_EXTENSIONS:
            if (suffix == null) {
                setExtensions(val);
            } else {
                if (extensions == null)
                    extensions = new CertificateExtensions();
                extensions.set(suffix, val);
            }
            break;
        }
    }

    /**
     * Delete the certificate attribute.
     *
     * @param name the name of the Certificate attribute.
     * @exception CertificateException on invalid attributes.
     * @exception IOException on other errors.
     */
    public void delete(String name)
    throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);

        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateException("Attribute name not recognized: "
                                           + name);
        }
        // set rawCertInfo to null, so that we are forced to re-encode
        rawCertInfo = null;
        String suffix = attrName.getSuffix();

        switch (attr) {
        case ATTR_VERSION:
            if (suffix == null) {
                version = null;
            } else {
                version.delete(suffix);
            }
            break;
        case (ATTR_SERIAL):
            if (suffix == null) {
                serialNum = null;
            } else {
                serialNum.delete(suffix);
            }
            break;
        case (ATTR_ALGORITHM):
            if (suffix == null) {
                algId = null;
            } else {
                algId.delete(suffix);
            }
            break;
        case (ATTR_ISSUER):
            issuer = null;
            break;
        case (ATTR_VALIDITY):
            if (suffix == null) {
                interval = null;
            } else {
                interval.delete(suffix);
            }
            break;
        case (ATTR_SUBJECT):
            subject = null;
            break;
        case (ATTR_KEY):
            if (suffix == null) {
                pubKey = null;
            } else {
                pubKey.delete(suffix);
            }
            break;
        case (ATTR_ISSUER_ID):
            issuerUniqueId = null;
            break;
        case (ATTR_SUBJECT_ID):
            subjectUniqueId = null;
            break;
        case (ATTR_EXTENSIONS):
            if (suffix == null) {
                extensions = null;
            } else {
                if (extensions != null)
                   extensions.delete(suffix);
            }
            break;
        }
    }

    /**
     * Get the certificate attribute.
     *
     * @param name the name of the Certificate attribute.
     *
     * @exception CertificateException on invalid attributes.
     * @exception IOException on other errors.
     */
    public Object get(String name)
    throws CertificateException, IOException {
        X509AttributeName attrName = new X509AttributeName(name);

        int attr = attributeMap(attrName.getPrefix());
        if (attr == 0) {
            throw new CertificateParsingException(
                          "Attribute name not recognized: " + name);
        }
        String suffix = attrName.getSuffix();

        switch (attr) { // frequently used attributes first
        case (ATTR_EXTENSIONS):
            if (suffix == null) {
                return(extensions);
            } else {
                if (extensions == null) {
                    return null;
                } else {
                    return(extensions.get(suffix));
                }
            }
        case (ATTR_SUBJECT):
            if (suffix == null) {
                return(subject);
            } else {
                return(getX500Name(suffix, false));
            }
        case (ATTR_ISSUER):
            if (suffix == null) {
                return(issuer);
            } else {
                return(getX500Name(suffix, true));
            }
        case (ATTR_KEY):
            if (suffix == null) {
                return(pubKey);
            } else {
                return(pubKey.get(suffix));
            }
        case (ATTR_ALGORITHM):
            if (suffix == null) {
                return(algId);
            } else {
                return(algId.get(suffix));
            }
        case (ATTR_VALIDITY):
            if (suffix == null) {
                return(interval);
            } else {
                return(interval.get(suffix));
            }
        case (ATTR_VERSION):
            if (suffix == null) {
                return(version);
            } else {
                return(version.get(suffix));
            }
        case (ATTR_SERIAL):
            if (suffix == null) {
                return(serialNum);
            } else {
                return(serialNum.get(suffix));
            }
        case (ATTR_ISSUER_ID):
            return(issuerUniqueId);
        case (ATTR_SUBJECT_ID):
            return(subjectUniqueId);
        }
        return null;
    }

    /*
     * Get the Issuer or Subject name
     */
    private Object getX500Name(String name, boolean getIssuer)
        throws IOException {
        if (name.equalsIgnoreCase(X509CertInfo.DN_NAME)) {
            return getIssuer ? issuer : subject;
        } else if (name.equalsIgnoreCase("x500principal")) {
            return getIssuer ? issuer.asX500Principal()
                             : subject.asX500Principal();
        } else {
            throw new IOException("Attribute name not recognized.");
        }
    }

    /*
     * This routine unmarshals the certificate information.
     */
    private void parse(DerValue val)
    throws CertificateParsingException, IOException {
        DerInputStream  in;
        DerValue        tmp;

        if (val.tag != DerValue.tag_Sequence) {
            throw new CertificateParsingException("signed fields invalid");
        }
        rawCertInfo = val.toByteArray();

        in = val.data;

        // Version
        tmp = in.getDerValue();
        if (tmp.isContextSpecific((byte)0)) {
            version = new CertificateVersion(tmp);
            tmp = in.getDerValue();
        }

        // Serial number ... an integer
        serialNum = new CertificateSerialNumber(tmp);

        // Algorithm Identifier
        algId = new CertificateAlgorithmId(in);

        // Issuer name
        issuer = new X500Name(in);
        if (issuer.isEmpty()) {
            throw new CertificateParsingException(
                "Empty issuer DN not allowed in X509Certificates");
        }

        // validity:  SEQUENCE { start date, end date }
        interval = new CertificateValidity(in);

        // subject name
        subject = new X500Name(in);
        if ((version.compare(CertificateVersion.V1) == 0) &&
                subject.isEmpty()) {
            throw new CertificateParsingException(
                      "Empty subject DN not allowed in v1 certificate");
        }

        // public key
        pubKey = new CertificateX509Key(in);

        // If more data available, make sure version is not v1.
        if (in.available() != 0) {
            if (version.compare(CertificateVersion.V1) == 0) {
                throw new CertificateParsingException(
                          "no more data allowed for version 1 certificate");
            }
        } else {
            return;
        }

        // Get the issuerUniqueId if present
        tmp = in.getDerValue();
        if (tmp.isContextSpecific((byte)1)) {
            issuerUniqueId = new UniqueIdentity(tmp);
            if (in.available() == 0)
                return;
            tmp = in.getDerValue();
        }

        // Get the subjectUniqueId if present.
        if (tmp.isContextSpecific((byte)2)) {
            subjectUniqueId = new UniqueIdentity(tmp);
            if (in.available() == 0)
                return;
            tmp = in.getDerValue();
        }

        // Get the extensions.
        if (version.compare(CertificateVersion.V3) != 0) {
            throw new CertificateParsingException(
                      "Extensions not allowed in v2 certificate");
        }
        if (tmp.isConstructed() && tmp.isContextSpecific((byte)3)) {
            extensions = new CertificateExtensions(tmp.data);
        }

        // verify X.509 V3 Certificate
        verifyCert(subject, extensions);

    }

    /*
     * Verify if X.509 V3 Certificate is compliant with RFC 5280.
     */
    private void verifyCert(X500Name subject,
        CertificateExtensions extensions)
        throws CertificateParsingException, IOException {

        // if SubjectName is empty, check for SubjectAlternativeNameExtension
        if (subject.isEmpty()) {
            if (extensions == null) {
                throw new CertificateParsingException("X.509 Certificate is " +
                        "incomplete: subject field is empty, and certificate " +
                        "has no extensions");
            }
            SubjectAlternativeNameExtension subjectAltNameExt = null;
            SubjectAlternativeNameExtension extValue = null;
            GeneralNames names = null;
            try {
                subjectAltNameExt = (SubjectAlternativeNameExtension)
                        extensions.get(SubjectAlternativeNameExtension.NAME);
                names = subjectAltNameExt.get(
                        SubjectAlternativeNameExtension.SUBJECT_NAME);
            } catch (IOException e) {
                throw new CertificateParsingException("X.509 Certificate is " +
                        "incomplete: subject field is empty, and " +
                        "SubjectAlternativeName extension is absent");
            }

            // SubjectAlternativeName extension is empty or not marked critical
            if (names == null || names.isEmpty()) {
                throw new CertificateParsingException("X.509 Certificate is " +
                        "incomplete: subject field is empty, and " +
                        "SubjectAlternativeName extension is empty");
            } else if (subjectAltNameExt.isCritical() == false) {
                throw new CertificateParsingException("X.509 Certificate is " +
                        "incomplete: SubjectAlternativeName extension MUST " +
                        "be marked critical when subject field is empty");
            }
        }
    }

    /*
     * Marshal the contents of a "raw" certificate into a DER sequence.
     */
    private void emit(DerOutputStream out)
    throws CertificateException, IOException {
        DerOutputStream tmp = new DerOutputStream();

        // version number, iff not V1
        version.encode(tmp);

        // Encode serial number, issuer signing algorithm, issuer name
        // and validity
        serialNum.encode(tmp);
        algId.encode(tmp);

        if ((version.compare(CertificateVersion.V1) == 0) &&
            (issuer.toString() == null))
            throw new CertificateParsingException(
                      "Null issuer DN not allowed in v1 certificate");

        issuer.encode(tmp);
        interval.encode(tmp);

        // Encode subject (principal) and associated key
        if ((version.compare(CertificateVersion.V1) == 0) &&
            (subject.toString() == null))
            throw new CertificateParsingException(
                      "Null subject DN not allowed in v1 certificate");
        subject.encode(tmp);
        pubKey.encode(tmp);

        // Encode issuerUniqueId & subjectUniqueId.
        if (issuerUniqueId != null) {
            issuerUniqueId.encode(tmp, DerValue.createTag(DerValue.TAG_CONTEXT,
                                                          false,(byte)1));
        }
        if (subjectUniqueId != null) {
            subjectUniqueId.encode(tmp, DerValue.createTag(DerValue.TAG_CONTEXT,
                                                           false,(byte)2));
        }

        // Write all the extensions.
        if (extensions != null) {
            extensions.encode(tmp);
        }

        // Wrap the data; encoding of the "raw" cert is now complete.
        out.write(DerValue.tag_Sequence, tmp);
    }

    /**
     * Returns the integer attribute number for the passed attribute name.
     */
    private int attributeMap(String name) {
        Integer num = map.get(name);
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    /**
     * Set the version number of the certificate.
     *
     * @param val the Object class value for the Extensions
     * @exception CertificateException on invalid data.
     */
    private void setVersion(Object val) throws CertificateException {
        if (!(val instanceof CertificateVersion)) {
            throw new CertificateException("Version class type invalid.");
        }
        version = (CertificateVersion)val;
    }

    /**
     * Set the serial number of the certificate.
     *
     * @param val the Object class value for the CertificateSerialNumber
     * @exception CertificateException on invalid data.
     */
    private void setSerialNumber(Object val) throws CertificateException {
        if (!(val instanceof CertificateSerialNumber)) {
            throw new CertificateException("SerialNumber class type invalid.");
        }
        serialNum = (CertificateSerialNumber)val;
    }

    /**
     * Set the algorithm id of the certificate.
     *
     * @param val the Object class value for the AlgorithmId
     * @exception CertificateException on invalid data.
     */
    private void setAlgorithmId(Object val) throws CertificateException {
        if (!(val instanceof CertificateAlgorithmId)) {
            throw new CertificateException(
                             "AlgorithmId class type invalid.");
        }
        algId = (CertificateAlgorithmId)val;
    }

    /**
     * Set the issuer name of the certificate.
     *
     * @param val the Object class value for the issuer
     * @exception CertificateException on invalid data.
     */
    private void setIssuer(Object val) throws CertificateException {
        if (!(val instanceof X500Name)) {
            throw new CertificateException(
                             "Issuer class type invalid.");
        }
        issuer = (X500Name)val;
    }

    /**
     * Set the validity interval of the certificate.
     *
     * @param val the Object class value for the CertificateValidity
     * @exception CertificateException on invalid data.
     */
    private void setValidity(Object val) throws CertificateException {
        if (!(val instanceof CertificateValidity)) {
            throw new CertificateException(
                             "CertificateValidity class type invalid.");
        }
        interval = (CertificateValidity)val;
    }

    /**
     * Set the subject name of the certificate.
     *
     * @param val the Object class value for the Subject
     * @exception CertificateException on invalid data.
     */
    private void setSubject(Object val) throws CertificateException {
        if (!(val instanceof X500Name)) {
            throw new CertificateException(
                             "Subject class type invalid.");
        }
        subject = (X500Name)val;
    }

    /**
     * Set the public key in the certificate.
     *
     * @param val the Object class value for the PublicKey
     * @exception CertificateException on invalid data.
     */
    private void setKey(Object val) throws CertificateException {
        if (!(val instanceof CertificateX509Key)) {
            throw new CertificateException(
                             "Key class type invalid.");
        }
        pubKey = (CertificateX509Key)val;
    }

    /**
     * Set the Issuer Unique Identity in the certificate.
     *
     * @param val the Object class value for the IssuerUniqueId
     * @exception CertificateException
     */
    private void setIssuerUniqueId(Object val) throws CertificateException {
        if (version.compare(CertificateVersion.V2) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(val instanceof UniqueIdentity)) {
            throw new CertificateException(
                             "IssuerUniqueId class type invalid.");
        }
        issuerUniqueId = (UniqueIdentity)val;
    }

    /**
     * Set the Subject Unique Identity in the certificate.
     *
     * @param val the Object class value for the SubjectUniqueId
     * @exception CertificateException
     */
    private void setSubjectUniqueId(Object val) throws CertificateException {
        if (version.compare(CertificateVersion.V2) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(val instanceof UniqueIdentity)) {
            throw new CertificateException(
                             "SubjectUniqueId class type invalid.");
        }
        subjectUniqueId = (UniqueIdentity)val;
    }

    /**
     * Set the extensions in the certificate.
     *
     * @param val the Object class value for the Extensions
     * @exception CertificateException
     */
    private void setExtensions(Object val) throws CertificateException {
        if (version.compare(CertificateVersion.V3) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(val instanceof CertificateExtensions)) {
          throw new CertificateException(
                             "Extensions class type invalid.");
        }
        extensions = (CertificateExtensions)val;
    }
}
