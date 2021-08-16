/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * This class represents the Basic Constraints Extension.
 *
 * <p>The basic constraints extension identifies whether the subject of the
 * certificate is a CA and how deep a certification path may exist
 * through that CA.
 *
 * <pre>
 * The ASN.1 syntax for this extension is:
 * BasicConstraints ::= SEQUENCE {
 *     cA                BOOLEAN DEFAULT FALSE,
 *     pathLenConstraint INTEGER (0..MAX) OPTIONAL
 * }
 * </pre>
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see CertAttrSet
 * @see Extension
 */
public class BasicConstraintsExtension extends Extension
implements CertAttrSet<String> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.BasicConstraints";
    /**
     * Attribute names.
     */
    public static final String NAME = "BasicConstraints";
    public static final String IS_CA = "is_ca";
    public static final String PATH_LEN = "path_len";

    // Private data members
    private boolean     ca = false;
    private int pathLen = -1;

    // Encode this extension value
    private void encodeThis() throws IOException {
        DerOutputStream out = new DerOutputStream();
        DerOutputStream tmp = new DerOutputStream();

        if (ca) {
            tmp.putBoolean(ca);
            // Only encode pathLen when ca == true
            if (pathLen >= 0) {
                tmp.putInteger(pathLen);
            }
        }
        out.write(DerValue.tag_Sequence, tmp);
        this.extensionValue = out.toByteArray();
    }

    /**
     * Default constructor for this object. The extension is marked
     * critical if the ca flag is true, false otherwise.
     *
     * @param ca true, if the subject of the Certificate is a CA.
     * @param len specifies the depth of the certification path.
     */
    public BasicConstraintsExtension(boolean ca, int len) throws IOException {
        this(Boolean.valueOf(ca), ca, len);
    }

    /**
     * Constructor for this object with specified criticality.
     *
     * @param critical true, if the extension should be marked critical
     * @param ca true, if the subject of the Certificate is a CA.
     * @param len specifies the depth of the certification path.
     */
    public BasicConstraintsExtension(Boolean critical, boolean ca, int len)
    throws IOException {
        this.ca = ca;
        this.pathLen = len;
        this.extensionId = PKIXExtensions.BasicConstraints_Id;
        this.critical = critical.booleanValue();
        encodeThis();
    }

    /**
     * Create the extension from the passed DER encoded value of the same.
     *
     * @param critical flag indicating if extension is critical or not
     * @param value an array containing the DER encoded bytes of the extension.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
     public BasicConstraintsExtension(Boolean critical, Object value)
         throws IOException
    {
         this.extensionId = PKIXExtensions.BasicConstraints_Id;
         this.critical = critical.booleanValue();

         this.extensionValue = (byte[]) value;
         DerValue val = new DerValue(this.extensionValue);
         if (val.tag != DerValue.tag_Sequence) {
             throw new IOException("Invalid encoding of BasicConstraints");
         }

         if (val.data == null || val.data.available() == 0) {
             // non-CA cert ("cA" field is FALSE by default), return -1
             return;
         }
         DerValue opt = val.data.getDerValue();
         if (opt.tag != DerValue.tag_Boolean) {
             // non-CA cert ("cA" field is FALSE by default), return -1
             return;
         }

         this.ca = opt.getBoolean();
         if (val.data.available() == 0) {
             // From PKIX profile:
             // Where pathLenConstraint does not appear, there is no
             // limit to the allowed length of the certification path.
             this.pathLen = Integer.MAX_VALUE;
             return;
         }

         opt = val.data.getDerValue();
         if (opt.tag != DerValue.tag_Integer) {
             throw new IOException("Invalid encoding of BasicConstraints");
         }
         this.pathLen = opt.getInteger();
         /*
          * Activate this check once again after PKIX profiling
          * is a standard and this check no longer imposes an
          * interoperability barrier.
          * if (ca) {
          *   if (!this.critical) {
          *   throw new IOException("Criticality cannot be false for CA.");
          *   }
          * }
          */
     }

     /**
      * Return user readable form of extension.
      */
     public String toString() {
         String pathLenAsString;
         if (pathLen < 0) {
             pathLenAsString = " undefined";
         } else if (pathLen == Integer.MAX_VALUE) {
             pathLenAsString = " no limit";
         } else {
             pathLenAsString = String.valueOf(pathLen);
         }
         return super.toString() +
             "BasicConstraints:[\n  CA:" + ca +
             "\n  PathLen:" + pathLenAsString +
             "\n]\n";
     }

     /**
      * Encode this extension value to the output stream.
      *
      * @param out the DerOutputStream to encode the extension to.
      */
     public void encode(OutputStream out) throws IOException {
         DerOutputStream tmp = new DerOutputStream();
         if (extensionValue == null) {
             this.extensionId = PKIXExtensions.BasicConstraints_Id;
             if (ca) {
                 critical = true;
             } else {
                 critical = false;
             }
             encodeThis();
         }
         super.encode(tmp);

         out.write(tmp.toByteArray());
     }

    /**
     * Set the attribute value.
     */
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(IS_CA)) {
            if (!(obj instanceof Boolean)) {
              throw new IOException("Attribute value should be of type Boolean.");
            }
            ca = ((Boolean)obj).booleanValue();
        } else if (name.equalsIgnoreCase(PATH_LEN)) {
            if (!(obj instanceof Integer)) {
              throw new IOException("Attribute value should be of type Integer.");
            }
            pathLen = ((Integer)obj).intValue();
        } else {
          throw new IOException("Attribute name not recognized by " +
                                "CertAttrSet:BasicConstraints.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public Object get(String name) throws IOException {
        if (name.equalsIgnoreCase(IS_CA)) {
            return (Boolean.valueOf(ca));
        } else if (name.equalsIgnoreCase(PATH_LEN)) {
            return (Integer.valueOf(pathLen));
        } else {
          throw new IOException("Attribute name not recognized by " +
                                "CertAttrSet:BasicConstraints.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(IS_CA)) {
            ca = false;
        } else if (name.equalsIgnoreCase(PATH_LEN)) {
            pathLen = -1;
        } else {
          throw new IOException("Attribute name not recognized by " +
                                "CertAttrSet:BasicConstraints.");
        }
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(IS_CA);
        elements.addElement(PATH_LEN);

        return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return (NAME);
    }
}
