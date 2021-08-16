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

package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import sun.security.util.*;

/**
 * This class represents the Inhibit Any-Policy Extension.
 *
 * <p>The inhibit any-policy extension can be used in certificates issued
 * to CAs. The inhibit any-policy indicates that the special any-policy
 * OID, with the value {2 5 29 32 0}, is not considered an explicit
 * match for other certificate policies.  The value indicates the number
 * of additional certificates that may appear in the path before any-
 * policy is no longer permitted.  For example, a value of one indicates
 * that any-policy may be processed in certificates issued by the sub-
 * ject of this certificate, but not in additional certificates in the
 * path.
 * <p>
 * This extension MUST be critical.
 * <p>
 * The ASN.1 syntax for this extension is:
 * <pre>{@code
 * id-ce-inhibitAnyPolicy OBJECT IDENTIFIER ::=  { id-ce 54 }
 *
 * InhibitAnyPolicy ::= SkipCerts
 *
 * SkipCerts ::= INTEGER (0..MAX)
 * }</pre>
 * @author Anne Anderson
 * @see CertAttrSet
 * @see Extension
 */
public class InhibitAnyPolicyExtension extends Extension
implements CertAttrSet<String> {

    private static final Debug debug = Debug.getInstance("certpath");

    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.InhibitAnyPolicy";

    /**
     * Object identifier for "any-policy"
     */
    public static ObjectIdentifier AnyPolicy_Id =
            ObjectIdentifier.of(KnownOIDs.CE_CERT_POLICIES_ANY);

    /**
     * Attribute names.
     */
    public static final String NAME = "InhibitAnyPolicy";
    public static final String SKIP_CERTS = "skip_certs";

    // Private data members
    private int skipCerts = Integer.MAX_VALUE;

    // Encode this extension value
    private void encodeThis() throws IOException {
        DerOutputStream out = new DerOutputStream();
        out.putInteger(skipCerts);
        this.extensionValue = out.toByteArray();
    }

    /**
     * Default constructor for this object.
     *
     * @param skipCerts specifies the depth of the certification path.
     *                  Use value of -1 to request unlimited depth.
     */
    public InhibitAnyPolicyExtension(int skipCerts) throws IOException {
        if (skipCerts < -1)
            throw new IOException("Invalid value for skipCerts");
        if (skipCerts == -1)
            this.skipCerts = Integer.MAX_VALUE;
        else
            this.skipCerts = skipCerts;
        this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;
        critical = true;
        encodeThis();
    }

    /**
     * Create the extension from the passed DER encoded value of the same.
     *
     * @param critical criticality flag to use.  Must be true for this
     *                 extension.
     * @param value a byte array holding the DER-encoded extension value.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public InhibitAnyPolicyExtension(Boolean critical, Object value)
        throws IOException {

        this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;

        if (!critical.booleanValue())
            throw new IOException("Criticality cannot be false for " +
                                  "InhibitAnyPolicy");
        this.critical = critical.booleanValue();

        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        if (val.tag != DerValue.tag_Integer)
            throw new IOException("Invalid encoding of InhibitAnyPolicy: "
                                  + "data not integer");

        if (val.data == null)
            throw new IOException("Invalid encoding of InhibitAnyPolicy: "
                                  + "null data");
        int skipCertsValue = val.getInteger();
        if (skipCertsValue < -1)
            throw new IOException("Invalid value for skipCerts");
        if (skipCertsValue == -1) {
            this.skipCerts = Integer.MAX_VALUE;
        } else {
            this.skipCerts = skipCertsValue;
        }
    }

     /**
      * Return user readable form of extension.
      */
     public String toString() {
         String s = super.toString() + "InhibitAnyPolicy: " + skipCerts + "\n";
         return s;
     }

     /**
      * Encode this extension value to the output stream.
      *
      * @param out the DerOutputStream to encode the extension to.
      */
     public void encode(OutputStream out) throws IOException {
         DerOutputStream tmp = new DerOutputStream();
         if (extensionValue == null) {
             this.extensionId = PKIXExtensions.InhibitAnyPolicy_Id;
             critical = true;
             encodeThis();
         }
         super.encode(tmp);

         out.write(tmp.toByteArray());
     }

    /**
     * Set the attribute value.
     *
     * @param name name of attribute to set. Must be SKIP_CERTS.
     * @param obj  value to which attribute is to be set.  Must be Integer
     *             type.
     * @throws IOException on error
     */
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(SKIP_CERTS)) {
            if (!(obj instanceof Integer))
                throw new IOException("Attribute value should be of type Integer.");
            int skipCertsValue = ((Integer)obj).intValue();
            if (skipCertsValue < -1)
                throw new IOException("Invalid value for skipCerts");
            if (skipCertsValue == -1) {
                skipCerts = Integer.MAX_VALUE;
            } else {
                skipCerts = skipCertsValue;
            }
        } else
            throw new IOException("Attribute name not recognized by " +
                                  "CertAttrSet:InhibitAnyPolicy.");
        encodeThis();
    }

    /**
     * Get the attribute value.
     *
     * @param name name of attribute to get.  Must be SKIP_CERTS.
     * @return value of the attribute.  In this case it will be of type
     *          Integer.
     * @throws IOException on error
     */
    public Integer get(String name) throws IOException {
        if (name.equalsIgnoreCase(SKIP_CERTS))
            return (skipCerts);
        else
            throw new IOException("Attribute name not recognized by " +
                                  "CertAttrSet:InhibitAnyPolicy.");
    }

    /**
     * Delete the attribute value.
     *
     * @param name name of attribute to delete. Must be SKIP_CERTS.
     * @throws IOException on error.  In this case, IOException will always be
     *                     thrown, because the only attribute, SKIP_CERTS, is
     *                     required.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(SKIP_CERTS))
            throw new IOException("Attribute " + SKIP_CERTS +
                                  " may not be deleted.");
        else
            throw new IOException("Attribute name not recognized by " +
                                  "CertAttrSet:InhibitAnyPolicy.");
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     *
     * @return enumeration of elements
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(SKIP_CERTS);
        return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     *
     * @return name of attribute.
     */
    public String getName() {
        return (NAME);
    }
}
