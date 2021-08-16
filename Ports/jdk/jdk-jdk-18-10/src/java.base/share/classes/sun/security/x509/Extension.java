/*
 * Copyright (c) 1997, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import sun.security.util.*;

/**
 * Represent a X509 Extension Attribute.
 *
 * <p>Extensions are additional attributes which can be inserted in a X509
 * v3 certificate. For example a "Driving License Certificate" could have
 * the driving license number as a extension.
 *
 * <p>Extensions are represented as a sequence of the extension identifier
 * (Object Identifier), a boolean flag stating whether the extension is to
 * be treated as being critical and the extension value itself (this is again
 * a DER encoding of the extension value).
 * <pre>
 * ASN.1 definition of Extension:
 * Extension ::= SEQUENCE {
 *      ExtensionId     OBJECT IDENTIFIER,
 *      critical        BOOLEAN DEFAULT FALSE,
 *      extensionValue  OCTET STRING
 * }
 * </pre>
 * All subclasses need to implement a constructor of the form
 * <pre>{@code
 *     <subclass> (Boolean, Object)
 * }</pre>
 * where the Object is typically an array of DER encoded bytes.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class Extension implements java.security.cert.Extension {

    protected ObjectIdentifier  extensionId = null;
    protected boolean           critical = false;
    protected byte[]            extensionValue = null;

    /**
     * Default constructor.  Used only by sub-classes.
     */
    public Extension() { }

    /**
     * Constructs an extension from a DER encoded array of bytes.
     */
    public Extension(DerValue derVal) throws IOException {

        DerInputStream in = derVal.toDerInputStream();

        // Object identifier
        extensionId = in.getOID();

        // If the criticality flag was false, it will not have been encoded.
        DerValue val = in.getDerValue();
        if (val.tag == DerValue.tag_Boolean) {
            critical = val.getBoolean();

            // Extension value (DER encoded)
            val = in.getDerValue();
            extensionValue = val.getOctetString();
        } else {
            critical = false;
            extensionValue = val.getOctetString();
        }
    }

    /**
     * Constructs an Extension from individual components of ObjectIdentifier,
     * criticality and the DER encoded OctetString.
     *
     * @param extensionId the ObjectIdentifier of the extension
     * @param critical the boolean indicating if the extension is critical
     * @param extensionValue the DER encoded octet string of the value.
     */
    public Extension(ObjectIdentifier extensionId, boolean critical,
                     byte[] extensionValue) throws IOException {
        this.extensionId = extensionId;
        this.critical = critical;
        // passed in a DER encoded octet string, strip off the tag
        // and length
        DerValue inDerVal = new DerValue(extensionValue);
        this.extensionValue = inDerVal.getOctetString();
    }

    /**
     * Constructs an Extension from another extension. To be used for
     * creating decoded subclasses.
     *
     * @param ext the extension to create from.
     */
    public Extension(Extension ext) {
        this.extensionId = ext.extensionId;
        this.critical = ext.critical;
        this.extensionValue = ext.extensionValue;
    }

    /**
     * Constructs an Extension from individual components of ObjectIdentifier,
     * criticality and the raw encoded extension value.
     *
     * @param extensionId the ObjectIdentifier of the extension
     * @param critical the boolean indicating if the extension is critical
     * @param rawExtensionValue the raw DER-encoded extension value (this
     * is not the encoded OctetString).
     */
    public static Extension newExtension(ObjectIdentifier extensionId,
        boolean critical, byte[] rawExtensionValue) throws IOException {
        Extension ext = new Extension();
        ext.extensionId = extensionId;
        ext.critical = critical;
        ext.extensionValue = rawExtensionValue;
        return ext;
    }

    public void encode(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }

        DerOutputStream dos1 = new DerOutputStream();
        DerOutputStream dos2 = new DerOutputStream();

        dos1.putOID(extensionId);
        if (critical) {
            dos1.putBoolean(critical);
        }
        dos1.putOctetString(extensionValue);

        dos2.write(DerValue.tag_Sequence, dos1);
        out.write(dos2.toByteArray());
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors
     */
    public void encode(DerOutputStream out) throws IOException {

        if (extensionId == null)
            throw new IOException("Null OID to encode for the extension!");
        if (extensionValue == null)
            throw new IOException("No value to encode for the extension!");

        DerOutputStream dos = new DerOutputStream();

        dos.putOID(extensionId);
        if (critical)
            dos.putBoolean(critical);
        dos.putOctetString(extensionValue);

        out.write(DerValue.tag_Sequence, dos);
    }

    /**
     * Returns true if extension is critical.
     */
    public boolean isCritical() {
        return critical;
    }

    /**
     * Returns the ObjectIdentifier of the extension.
     */
    public ObjectIdentifier getExtensionId() {
        return extensionId;
    }

    public byte[] getValue() {
        return extensionValue.clone();
    }

    /**
     * Returns the extension value as an byte array for further processing.
     * Note, this is the raw DER value of the extension, not the DER
     * encoded octet string which is in the certificate.
     * This method does not return a clone; it is the responsibility of the
     * caller to clone the array if necessary.
     */
    public byte[] getExtensionValue() {
        return extensionValue;
    }

    public String getId() {
        return extensionId.toString();
    }

    /**
     * Returns the Extension in user readable form.
     */
    public String toString() {
        return "ObjectId: " + extensionId +
                " Criticality=" + critical + '\n';
    }

    // Value to mix up the hash
    private static final int hashMagic = 31;

    /**
     * Returns a hashcode value for this Extension.
     *
     * @return the hashcode value.
     */
    public int hashCode() {
        int h = 0;
        if (extensionValue != null) {
            byte[] val = extensionValue;
            int len = val.length;
            while (len > 0)
                h += len * val[--len];
        }
        h = h * hashMagic + extensionId.hashCode();
        h = h * hashMagic + (critical?1231:1237);
        return h;
    }

    /**
     * Compares this Extension for equality with the specified
     * object. If the <code>other</code> object is an
     * <code>instanceof</code> <code>Extension</code>, then
     * its encoded form is retrieved and compared with the
     * encoded form of this Extension.
     *
     * @param other the object to test for equality with this Extension.
     * @return true iff the other object is of type Extension, and the
     * criticality flag, object identifier and encoded extension value of
     * the two Extensions match, false otherwise.
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Extension))
            return false;
        Extension otherExt = (Extension) other;
        if (critical != otherExt.critical)
            return false;
        if (!extensionId.equals(otherExt.extensionId))
            return false;
        return Arrays.equals(extensionValue, otherExt.extensionValue);
    }
}
