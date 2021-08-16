/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.util.DerValue;
import sun.security.util.DerOutputStream;

/**
 * Represents the CRL Certificate Issuer Extension (OID = 2.5.29.29).
 * <p>
 * The CRL certificate issuer extension identifies the certificate issuer
 * associated with an entry in an indirect CRL, i.e. a CRL that has the
 * indirectCRL indicator set in its issuing distribution point extension. If
 * this extension is not present on the first entry in an indirect CRL, the
 * certificate issuer defaults to the CRL issuer. On subsequent entries
 * in an indirect CRL, if this extension is not present, the certificate
 * issuer for the entry is the same as that for the preceding entry.
 * <p>
 * If used by conforming CRL issuers, this extension is always
 * critical.  If an implementation ignored this extension it could not
 * correctly attribute CRL entries to certificates.  PKIX (RFC 5280)
 * RECOMMENDS that implementations recognize this extension.
 * <p>
 * The ASN.1 definition for this is:
 * <pre>
 * id-ce-certificateIssuer   OBJECT IDENTIFIER ::= { id-ce 29 }
 *
 * certificateIssuer ::=     GeneralNames
 * </pre>
 *
 * @author Anne Anderson
 * @author Sean Mullan
 * @since 1.5
 * @see Extension
 * @see CertAttrSet
 */
public class CertificateIssuerExtension extends Extension
    implements CertAttrSet<String> {

    /**
     * Attribute names.
     */
    public static final String NAME = "CertificateIssuer";
    public static final String ISSUER = "issuer";

    private GeneralNames names;

    /**
     * Encode this extension
     */
    private void encodeThis() throws IOException {
        if (names == null || names.isEmpty()) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream os = new DerOutputStream();
        names.encode(os);
        this.extensionValue = os.toByteArray();
    }

    /**
     * Create a CertificateIssuerExtension containing the specified issuer name.
     * Criticality is automatically set to true.
     *
     * @param issuer the certificate issuer
     * @throws IOException on error
     */
    public CertificateIssuerExtension(GeneralNames issuer) throws IOException {
        this.extensionId = PKIXExtensions.CertificateIssuer_Id;
        this.critical = true;
        this.names = issuer;
        encodeThis();
    }

    /**
     * Create a CertificateIssuerExtension from the specified DER encoded
     * value of the same.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value
     * @throws ClassCastException if value is not an array of bytes
     * @throws IOException on error
     */
    public CertificateIssuerExtension(Boolean critical, Object value)
        throws IOException {
        this.extensionId = PKIXExtensions.CertificateIssuer_Id;
        this.critical = critical.booleanValue();

        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        this.names = new GeneralNames(val);
    }

    /**
     * Set the attribute value.
     *
     * @throws IOException on error
     */
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(ISSUER)) {
            if (!(obj instanceof GeneralNames)) {
                throw new IOException("Attribute value must be of type " +
                    "GeneralNames");
            }
            this.names = (GeneralNames)obj;
        } else {
            throw new IOException("Attribute name not recognized by " +
                "CertAttrSet:CertificateIssuer");
        }
        encodeThis();
    }

    /**
     * Gets the attribute value.
     *
     * @throws IOException on error
     */
    public GeneralNames get(String name) throws IOException {
        if (name.equalsIgnoreCase(ISSUER)) {
            return names;
        } else {
            throw new IOException("Attribute name not recognized by " +
                "CertAttrSet:CertificateIssuer");
        }
    }

    /**
     * Deletes the attribute value.
     *
     * @throws IOException on error
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(ISSUER)) {
            names = null;
        } else {
            throw new IOException("Attribute name not recognized by " +
                "CertAttrSet:CertificateIssuer");
        }
        encodeThis();
    }

    /**
     * Returns a printable representation of the certificate issuer.
     */
    public String toString() {
        return super.toString() + "Certificate Issuer [\n" +
            String.valueOf(names) + "]\n";
    }

    /**
     * Write the extension to the OutputStream.
     *
     * @param out the OutputStream to write the extension to
     * @exception IOException on encoding errors
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream  tmp = new DerOutputStream();
        if (extensionValue == null) {
            extensionId = PKIXExtensions.CertificateIssuer_Id;
            critical = true;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(ISSUER);
        return elements.elements();
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return NAME;
    }
}
