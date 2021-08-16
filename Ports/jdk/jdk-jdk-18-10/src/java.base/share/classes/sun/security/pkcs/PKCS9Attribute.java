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

package sun.security.pkcs;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Date;
import sun.security.x509.CertificateExtensions;
import sun.security.util.*;

/**
 * Class supporting any PKCS9 attributes.
 * Supports DER decoding/encoding and access to attribute values.
 *
 * <a name="classTable"><h3>Type/Class Table</h3></a>
 * The following table shows the correspondence between
 * PKCS9 attribute types and value component classes.
 * For types not listed here, its name is the OID
 * in string form, its value is a (single-valued)
 * byte array that is the SET's encoding.
 *
 * <TABLE BORDER CELLPADDING=8 ALIGN=CENTER>
 *
 * <TR>
 * <TH>Object Identifier</TH>
 * <TH>Attribute Name</TH>
 * <TH>Type</TH>
 * <TH>Value Class</TH>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.1</TD>
 * <TD>EmailAddress</TD>
 * <TD>Multi-valued</TD>
 * <TD><code>String[]</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.2</TD>
 * <TD>UnstructuredName</TD>
 * <TD>Multi-valued</TD>
 * <TD><code>String[]</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.3</TD>
 * <TD>ContentType</TD>
 * <TD>Single-valued</TD>
 * <TD><code>ObjectIdentifier</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.4</TD>
 * <TD>MessageDigest</TD>
 * <TD>Single-valued</TD>
 * <TD><code>byte[]</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.5</TD>
 * <TD>SigningTime</TD>
 * <TD>Single-valued</TD>
 * <TD><code>Date</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.6</TD>
 * <TD>Countersignature</TD>
 * <TD>Multi-valued</TD>
 * <TD><code>SignerInfo[]</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.7</TD>
 * <TD>ChallengePassword</TD>
 * <TD>Single-valued</TD>
 * <TD><code>String</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.8</TD>
 * <TD>UnstructuredAddress</TD>
 * <TD>Single-valued</TD>
 * <TD><code>String</code></TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.9</TD>
 * <TD>ExtendedCertificateAttributes</TD>
 * <TD>Multi-valued</TD>
 * <TD>(not supported)</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.10</TD>
 * <TD>IssuerAndSerialNumber</TD>
 * <TD>Single-valued</TD>
 * <TD>(not supported)</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.{11,12}</TD>
 * <TD>RSA DSI proprietary</TD>
 * <TD>Single-valued</TD>
 * <TD>(not supported)</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.13</TD>
 * <TD>S/MIME unused assignment</TD>
 * <TD>Single-valued</TD>
 * <TD>(not supported)</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.14</TD>
 * <TD>ExtensionRequest</TD>
 * <TD>Single-valued</TD>
 * <TD>CertificateExtensions</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.15</TD>
 * <TD>SMIMECapability</TD>
 * <TD>Single-valued</TD>
 * <TD>(not supported)</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.16.2.12</TD>
 * <TD>SigningCertificate</TD>
 * <TD>Single-valued</TD>
 * <TD>SigningCertificateInfo</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.16.2.14</TD>
 * <TD>SignatureTimestampToken</TD>
 * <TD>Single-valued</TD>
 * <TD>byte[]</TD>
 * </TR>
 *
 * <TR>
 * <TD>1.2.840.113549.1.9.16.2.52</TD>
 * <TD>CMSAlgorithmProtection</TD>
 * <TD>Single-valued</TD>
 * <TD>byte[]</TD>
 * </TR>
 *
 * </TABLE>
 *
 * @author Douglas Hoover
 */
public class PKCS9Attribute implements DerEncoder {

    /* Are we debugging ? */
    private static final Debug debug = Debug.getInstance("jar");

    /**
     * Array of attribute OIDs defined in PKCS9, by number.
     */
    static final ObjectIdentifier[] PKCS9_OIDS = new ObjectIdentifier[19];

    private static final Class<?> BYTE_ARRAY_CLASS;

    static {
        // set unused PKCS9_OIDS entries to null
        // rest are initialized with public constants
        PKCS9_OIDS[0] = PKCS9_OIDS[11] = PKCS9_OIDS[12] = PKCS9_OIDS[13] =
        PKCS9_OIDS[15] = null;
        try {
            BYTE_ARRAY_CLASS = Class.forName("[B");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e.toString());
        }
    }

    public static final ObjectIdentifier EMAIL_ADDRESS_OID = PKCS9_OIDS[1] =
            ObjectIdentifier.of(KnownOIDs.EmailAddress);
    public static final ObjectIdentifier UNSTRUCTURED_NAME_OID = PKCS9_OIDS[2] =
            ObjectIdentifier.of(KnownOIDs.UnstructuredName);
    public static final ObjectIdentifier CONTENT_TYPE_OID = PKCS9_OIDS[3] =
            ObjectIdentifier.of(KnownOIDs.ContentType);
    public static final ObjectIdentifier MESSAGE_DIGEST_OID = PKCS9_OIDS[4] =
            ObjectIdentifier.of(KnownOIDs.MessageDigest);
    public static final ObjectIdentifier SIGNING_TIME_OID = PKCS9_OIDS[5] =
            ObjectIdentifier.of(KnownOIDs.SigningTime);
    public static final ObjectIdentifier COUNTERSIGNATURE_OID = PKCS9_OIDS[6] =
            ObjectIdentifier.of(KnownOIDs.CounterSignature);
    public static final ObjectIdentifier CHALLENGE_PASSWORD_OID =
            PKCS9_OIDS[7] = ObjectIdentifier.of(KnownOIDs.ChallengePassword);
    public static final ObjectIdentifier UNSTRUCTURED_ADDRESS_OID =
            PKCS9_OIDS[8] = ObjectIdentifier.of(KnownOIDs.UnstructuredAddress);
    public static final ObjectIdentifier EXTENDED_CERTIFICATE_ATTRIBUTES_OID =
            PKCS9_OIDS[9] =
            ObjectIdentifier.of(KnownOIDs.ExtendedCertificateAttributes);
    public static final ObjectIdentifier ISSUER_SERIALNUMBER_OID =
            PKCS9_OIDS[10] =
            ObjectIdentifier.of(KnownOIDs.IssuerAndSerialNumber);
    // [11], [12] are RSA DSI proprietary
    // [13] ==> signingDescription, S/MIME, not used anymore
    public static final ObjectIdentifier EXTENSION_REQUEST_OID =
            PKCS9_OIDS[14] = ObjectIdentifier.of(KnownOIDs.ExtensionRequest);
    public static final ObjectIdentifier SIGNING_CERTIFICATE_OID =
            PKCS9_OIDS[16] = ObjectIdentifier.of(KnownOIDs.SigningCertificate);
    public static final ObjectIdentifier SIGNATURE_TIMESTAMP_TOKEN_OID =
            PKCS9_OIDS[17] =
            ObjectIdentifier.of(KnownOIDs.SignatureTimestampToken);
    public static final ObjectIdentifier CMS_ALGORITHM_PROTECTION_OID =
            PKCS9_OIDS[18] =
            ObjectIdentifier.of(KnownOIDs.CMSAlgorithmProtection);

    /**
     * Acceptable ASN.1 tags for DER encodings of values of PKCS9
     * attributes, by index in <code>PKCS9_OIDS</code>.
     * Sets of acceptable tags are represented as arrays.
     */
    private static final Byte[][] PKCS9_VALUE_TAGS = {
        null,
        {DerValue.tag_IA5String},   // EMailAddress
        {DerValue.tag_IA5String,
         DerValue.tag_PrintableString,
         DerValue.tag_T61String,
         DerValue.tag_BMPString,
         DerValue.tag_UniversalString,
         DerValue.tag_UTF8String},  // UnstructuredName
        {DerValue.tag_ObjectId},    // ContentType
        {DerValue.tag_OctetString}, // MessageDigest
        {DerValue.tag_UtcTime,
         DerValue.tag_GeneralizedTime}, // SigningTime
        {DerValue.tag_Sequence},    // Countersignature
        {DerValue.tag_PrintableString,
         DerValue.tag_T61String,
         DerValue.tag_BMPString,
         DerValue.tag_UniversalString,
         DerValue.tag_UTF8String},   // ChallengePassword
        {DerValue.tag_PrintableString,
         DerValue.tag_T61String,
         DerValue.tag_BMPString,
         DerValue.tag_UniversalString,
         DerValue.tag_UTF8String},   // UnstructuredAddress
        {DerValue.tag_SetOf},       // ExtendedCertificateAttributes
        {DerValue.tag_Sequence},    // issuerAndSerialNumber
        null,
        null,
        null,
        {DerValue.tag_Sequence},    // extensionRequest
        {DerValue.tag_Sequence},    // SMIMECapability
        {DerValue.tag_Sequence},    // SigningCertificate
        {DerValue.tag_Sequence},    // SignatureTimestampToken
        {DerValue.tag_Sequence}     // CMSAlgorithmProtection
    };

    private static final Class<?>[] VALUE_CLASSES = new Class<?>[19];

    static {
        try {
            Class<?> str = Class.forName("[Ljava.lang.String;");

            VALUE_CLASSES[0] = null;  // not used
            VALUE_CLASSES[1] = str;   // EMailAddress
            VALUE_CLASSES[2] = str;   // UnstructuredName
            VALUE_CLASSES[3] =        // ContentType
                Class.forName("sun.security.util.ObjectIdentifier");
            VALUE_CLASSES[4] = BYTE_ARRAY_CLASS; // MessageDigest (byte[])
            VALUE_CLASSES[5] = Class.forName("java.util.Date"); // SigningTime
            VALUE_CLASSES[6] =        // Countersignature
                Class.forName("[Lsun.security.pkcs.SignerInfo;");
            VALUE_CLASSES[7] =        // ChallengePassword
                Class.forName("java.lang.String");
            VALUE_CLASSES[8] = str;   // UnstructuredAddress
            VALUE_CLASSES[9] = null;  // ExtendedCertificateAttributes
            VALUE_CLASSES[10] = null;  // IssuerAndSerialNumber
            VALUE_CLASSES[11] = null;  // not used
            VALUE_CLASSES[12] = null;  // not used
            VALUE_CLASSES[13] = null;  // not used
            VALUE_CLASSES[14] =        // ExtensionRequest
                Class.forName("sun.security.x509.CertificateExtensions");
            VALUE_CLASSES[15] = null;  // not supported yet
            VALUE_CLASSES[16] = null;  // not supported yet
            VALUE_CLASSES[17] = BYTE_ARRAY_CLASS;  // SignatureTimestampToken
            VALUE_CLASSES[18] = BYTE_ARRAY_CLASS;  // CMSAlgorithmProtection
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e.toString());
        }
    }

    /**
     * Array indicating which PKCS9 attributes are single-valued,
     * by index in <code>PKCS9_OIDS</code>.
     */
    private static final boolean[] SINGLE_VALUED = {
      false,
      false,   // EMailAddress
      false,   // UnstructuredName
      true,    // ContentType
      true,    // MessageDigest
      true,    // SigningTime
      false,   // Countersignature
      true,    // ChallengePassword
      false,   // UnstructuredAddress
      false,   // ExtendedCertificateAttributes
      true,    // IssuerAndSerialNumber - not supported yet
      false,   // not used
      false,   // not used
      false,   // not used
      true,    // ExtensionRequest
      true,    // SMIMECapability - not supported yet
      true,    // SigningCertificate
      true,    // SignatureTimestampToken
      true,    // CMSAlgorithmProtection
    };

    /**
     * The OID of this attribute.
     */
    private ObjectIdentifier oid;

    /**
     * The index of the OID of this attribute in <code>PKCS9_OIDS</code>,
     * or -1 if it's unknown.
     */
    private int index;

    /**
     * Value set of this attribute.  Its class is given by
     * <code>VALUE_CLASSES[index]</code>. The SET itself
     * as byte[] if unknown.
     */
    private Object value;

    /**
     * Construct an attribute object from the attribute's OID and
     * value.  If the attribute is single-valued, provide only one
     * value.  If the attribute is multi-valued, provide an array
     * containing all the values.
     * Arrays of length zero are accepted, though probably useless.
     *
     * <P> The
     * <a href=#classTable>table</a> gives the class that <code>value</code>
     * must have for a given attribute.
     *
     * @exception IllegalArgumentException
     * if the <code>value</code> has the wrong type.
     */
    public PKCS9Attribute(ObjectIdentifier oid, Object value)
    throws IllegalArgumentException {
        init(oid, value);
    }

    private void init(ObjectIdentifier oid, Object value)
        throws IllegalArgumentException {

        this.oid = oid;
        index = indexOf(oid, PKCS9_OIDS, 1);
        Class<?> clazz = index == -1 ? BYTE_ARRAY_CLASS: VALUE_CLASSES[index];
        if (!clazz.isInstance(value)) {
                throw new IllegalArgumentException(
                           "Wrong value class " +
                           " for attribute " + oid +
                           " constructing PKCS9Attribute; was " +
                           value.getClass().toString() + ", should be " +
                           clazz.toString());
        }
        this.value = value;
    }


    /**
     * Construct a PKCS9Attribute from its encoding on an input
     * stream.
     *
     * @param derVal the DerValue representing the DER encoding of the attribute.
     * @exception IOException on parsing error.
     */
    public PKCS9Attribute(DerValue derVal) throws IOException {

        DerInputStream derIn = new DerInputStream(derVal.toByteArray());
        DerValue[] val =  derIn.getSequence(2);

        if (derIn.available() != 0)
            throw new IOException("Excess data parsing PKCS9Attribute");

        if (val.length != 2)
            throw new IOException("PKCS9Attribute doesn't have two components");

        // get the oid
        oid = val[0].getOID();
        byte[] content = val[1].toByteArray();
        DerValue[] elems = new DerInputStream(content).getSet(1);

        index = indexOf(oid, PKCS9_OIDS, 1);
        if (index == -1) {
            if (debug != null) {
                debug.println("Unsupported signer attribute: " + oid);
            }
            value = content;
            return;
        }

        // check single valued have only one value
        if (SINGLE_VALUED[index] && elems.length > 1)
            throwSingleValuedException();

        // check for illegal element tags
        Byte tag;
        for (DerValue elem : elems) {
            tag = elem.tag;
            if (indexOf(tag, PKCS9_VALUE_TAGS[index], 0) == -1)
                throwTagException(tag);
        }

        switch (index) {
        case 1:     // email address
        case 2:     // unstructured name
        case 8:     // unstructured address
            { // open scope
                String[] values = new String[elems.length];

                for (int i=0; i < elems.length; i++)
                    values[i] = elems[i].getAsString();
                value = values;
            } // close scope
            break;

        case 3:     // content type
            value = elems[0].getOID();
            break;

        case 4:     // message digest
            value = elems[0].getOctetString();
            break;

        case 5:     // signing time
            byte elemTag = elems[0].getTag();
            DerInputStream dis = new DerInputStream(elems[0].toByteArray());
            value = (elemTag == DerValue.tag_GeneralizedTime) ?
                    dis.getGeneralizedTime() : dis.getUTCTime();
            break;

        case 6:     // countersignature
            { // open scope
                SignerInfo[] values = new SignerInfo[elems.length];
                for (int i=0; i < elems.length; i++)
                    values[i] =
                        new SignerInfo(elems[i].toDerInputStream());
                value = values;
            } // close scope
            break;

        case 7:     // challenge password
            value = elems[0].getAsString();
            break;

        case 9:     // extended-certificate attribute -- not supported
            throw new IOException("PKCS9 extended-certificate " +
                                  "attribute not supported.");
            // break unnecessary
        case 10:    // issuerAndserialNumber attribute -- not supported
            throw new IOException("PKCS9 IssuerAndSerialNumber" +
                                  "attribute not supported.");
            // break unnecessary
        case 11:    // RSA DSI proprietary
        case 12:    // RSA DSI proprietary
            throw new IOException("PKCS9 RSA DSI attributes" +
                                  "11 and 12, not supported.");
            // break unnecessary
        case 13:    // S/MIME unused attribute
            throw new IOException("PKCS9 attribute #13 not supported.");
            // break unnecessary

        case 14:     // ExtensionRequest
            value = new CertificateExtensions(
                       new DerInputStream(elems[0].toByteArray()));
            break;

        case 15:     // SMIME-capability attribute -- not supported
            throw new IOException("PKCS9 SMIMECapability " +
                                  "attribute not supported.");
            // break unnecessary
        case 16:     // SigningCertificate attribute
            value = new SigningCertificateInfo(elems[0].toByteArray());
            break;

        case 17:     // SignatureTimestampToken attribute
            value = elems[0].toByteArray();
            break;

        case 18:    // CMSAlgorithmProtection
            value = elems[0].toByteArray();
            break;

        default: // can't happen
        }
    }

    /**
     * Write the DER encoding of this attribute to an output stream.
     *
     * <P> N.B.: This method always encodes values of
     * ChallengePassword and UnstructuredAddress attributes as ASN.1
     * <code>PrintableString</code>s, without checking whether they
     * should be encoded as <code>T61String</code>s.
     */
    @Override
    public void derEncode(OutputStream out) throws IOException {
        DerOutputStream temp = new DerOutputStream();
        temp.putOID(oid);
        switch (index) {
        case -1:    // Unknown
            temp.write((byte[])value);
            break;
        case 1:     // email address
        case 2:     // unstructured name
            { // open scope
                String[] values = (String[]) value;
                DerOutputStream[] temps = new
                    DerOutputStream[values.length];

                for (int i=0; i < values.length; i++) {
                    temps[i] = new DerOutputStream();
                    temps[i].putIA5String( values[i]);
                }
                temp.putOrderedSetOf(DerValue.tag_Set, temps);
            } // close scope
            break;

        case 3:     // content type
            {
                DerOutputStream temp2 = new DerOutputStream();
                temp2.putOID((ObjectIdentifier) value);
                temp.write(DerValue.tag_Set, temp2.toByteArray());
            }
            break;

        case 4:     // message digest
            {
                DerOutputStream temp2 = new DerOutputStream();
                temp2.putOctetString((byte[]) value);
                temp.write(DerValue.tag_Set, temp2.toByteArray());
            }
            break;

        case 5:     // signing time
            {
                DerOutputStream temp2 = new DerOutputStream();
                temp2.putUTCTime((Date) value);
                temp.write(DerValue.tag_Set, temp2.toByteArray());
            }
            break;

        case 6:     // countersignature
            temp.putOrderedSetOf(DerValue.tag_Set, (DerEncoder[]) value);
            break;

        case 7:     // challenge password
            {
                DerOutputStream temp2 = new DerOutputStream();
                temp2.putPrintableString((String) value);
                temp.write(DerValue.tag_Set, temp2.toByteArray());
            }
            break;

        case 8:     // unstructured address
            { // open scope
                String[] values = (String[]) value;
                DerOutputStream[] temps = new
                    DerOutputStream[values.length];

                for (int i=0; i < values.length; i++) {
                    temps[i] = new DerOutputStream();
                    temps[i].putPrintableString(values[i]);
                }
                temp.putOrderedSetOf(DerValue.tag_Set, temps);
            } // close scope
            break;

        case 9:     // extended-certificate attribute -- not supported
            throw new IOException("PKCS9 extended-certificate " +
                                  "attribute not supported.");
            // break unnecessary
        case 10:    // issuerAndserialNumber attribute -- not supported
            throw new IOException("PKCS9 IssuerAndSerialNumber" +
                                  "attribute not supported.");
            // break unnecessary
        case 11:    // RSA DSI proprietary
        case 12:    // RSA DSI proprietary
            throw new IOException("PKCS9 RSA DSI attributes" +
                                  "11 and 12, not supported.");
            // break unnecessary
        case 13:    // S/MIME unused attribute
            throw new IOException("PKCS9 attribute #13 not supported.");
            // break unnecessary

        case 14:     // ExtensionRequest
            {
                DerOutputStream temp2 = new DerOutputStream();
                CertificateExtensions exts = (CertificateExtensions)value;
                try {
                    exts.encode(temp2, true);
                } catch (CertificateException ex) {
                    throw new IOException(ex.toString());
                }
                temp.write(DerValue.tag_Set, temp2.toByteArray());
            }
            break;
        case 15:    // SMIMECapability
            throw new IOException("PKCS9 attribute #15 not supported.");
            // break unnecessary

        case 16:    // SigningCertificate
            throw new IOException(
                "PKCS9 SigningCertificate attribute not supported.");
            // break unnecessary

        case 17:    // SignatureTimestampToken
            temp.write(DerValue.tag_Set, (byte[])value);
            break;

        case 18:    // CMSAlgorithmProtection
            temp.write(DerValue.tag_Set, (byte[])value);
            break;

        default: // can't happen
        }

        DerOutputStream derOut = new DerOutputStream();
        derOut.write(DerValue.tag_Sequence, temp.toByteArray());

        out.write(derOut.toByteArray());

    }

    /**
     * Returns if the attribute is known. Unknown attributes can be created
     * from DER encoding with unknown OIDs.
     */
    public boolean isKnown() {
        return index != -1;
    }

    /**
     * Get the value of this attribute.  If the attribute is
     * single-valued, return just the one value.  If the attribute is
     * multi-valued, return an array containing all the values.
     * It is possible for this array to be of length 0.
     *
     * <P> The
     * <a href=#classTable>table</a> gives the class of the value returned,
     * depending on the type of this attribute.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Show whether this attribute is single-valued.
     */
    public boolean isSingleValued() {
        return index == -1 || SINGLE_VALUED[index];
    }

    /**
     *  Return the OID of this attribute.
     */
    public ObjectIdentifier getOID() {
        return oid;
    }

    /**
     *  Return the name of this attribute.
     */
    public String getName() {
        String n = oid.toString();
        KnownOIDs os = KnownOIDs.findMatch(n);
        return (os == null? n : os.stdName());
    }

    /**
     * Return the OID for a given attribute name or null if we don't recognize
     * the name.
     */
    public static ObjectIdentifier getOID(String name) {
        KnownOIDs o = KnownOIDs.findMatch(name);
        if (o != null) {
            return ObjectIdentifier.of(o);
        } else {
            return null;
        }
    }

    /**
     * Return the attribute name for a given OID or null if we don't recognize
     * the oid.
     */
    public static String getName(ObjectIdentifier oid) {
        return KnownOIDs.findMatch(oid.toString()).stdName();
    }

    /**
     * Returns a string representation of this attribute.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);

        sb.append("[");

        if (index == -1) {
            sb.append(oid.toString());
        } else {
            sb.append(getName(oid));
        }
        sb.append(": ");

        if (index == -1 || SINGLE_VALUED[index]) {
            if (value instanceof byte[]) { // special case for octet string
                HexDumpEncoder hexDump = new HexDumpEncoder();
                sb.append(hexDump.encodeBuffer((byte[]) value));
            } else {
                sb.append(value.toString());
            }
            sb.append("]");
            return sb.toString();
        } else { // multi-valued
            boolean first = true;
            Object[] values = (Object[]) value;

            for (Object curVal : values) {
                if (first)
                    first = false;
                else
                    sb.append(", ");
                sb.append(curVal.toString());
            }
            return sb.toString();
        }
    }

    /**
     * Beginning the search at <code>start</code>, find the first
     * index <code>i</code> such that <code>a[i] = obj</code>.
     *
     * @return the index, if found, and -1 otherwise.
     */
    static int indexOf(Object obj, Object[] a, int start) {
        for (int i=start; i < a.length; i++) {
            if (obj.equals(a[i])) return i;
        }
        return -1;
    }

    /**
     * Throw an exception when there are multiple values for
     * a single-valued attribute.
     */
    private void throwSingleValuedException() throws IOException {
        throw new IOException("Single-value attribute " +
                              oid + " (" + getName() + ")" +
                              " has multiple values.");
    }

    /**
     * Throw an exception when the tag on a value encoding is
     * wrong for the attribute whose value it is. This method
     * will only be called for known tags.
     */
    private void throwTagException(Byte tag)
    throws IOException {
        Byte[] expectedTags = PKCS9_VALUE_TAGS[index];
        StringBuilder msg = new StringBuilder(100);
        msg.append("Value of attribute ");
        msg.append(oid.toString());
        msg.append(" (");
        msg.append(getName());
        msg.append(") has wrong tag: ");
        msg.append(tag.toString());
        msg.append(".  Expected tags: ");

        msg.append(expectedTags[0].toString());

        for (int i = 1; i < expectedTags.length; i++) {
            msg.append(", ");
            msg.append(expectedTags[i].toString());
        }
        msg.append(".");
        throw new IOException(msg.toString());
    }
}
