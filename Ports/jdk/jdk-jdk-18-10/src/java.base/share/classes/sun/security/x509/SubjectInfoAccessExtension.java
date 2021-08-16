/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.*;

import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * The Subject Information Access Extension (OID = 1.3.6.1.5.5.7.1.11).
 * <p>
 * The subject information access extension indicates how to access
 * information and services for the subject of the certificate in which
 * the extension appears.  When the subject is a CA, information and
 * services may include certificate validation services and CA policy
 * data.  When the subject is an end entity, the information describes
 * the type of services offered and how to access them.  In this case,
 * the contents of this extension are defined in the protocol
 * specifications for the supported services.  This extension may be
 * included in end entity or CA certificates.  Conforming CAs MUST mark
 * this extension as non-critical.
 * <p>
 * This extension is defined in <a href="http://tools.ietf.org/html/rfc5280">
 * Internet X.509 PKI Certificate and Certificate Revocation List
 * (CRL) Profile</a>. The profile permits
 * the extension to be included in end-entity or CA certificates,
 * and it must be marked as non-critical. Its ASN.1 definition is as follows:
 * <pre>
 *   id-pe-subjectInfoAccess OBJECT IDENTIFIER ::= { id-pe 11 }
 *
 *   SubjectInfoAccessSyntax  ::=
 *          SEQUENCE SIZE (1..MAX) OF AccessDescription
 *
 *   AccessDescription  ::=  SEQUENCE {
 *          accessMethod          OBJECT IDENTIFIER,
 *          accessLocation        GeneralName  }
 * </pre>
 *
 * @see Extension
 * @see CertAttrSet
 */

public class SubjectInfoAccessExtension extends Extension
        implements CertAttrSet<String> {

    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT =
                                "x509.info.extensions.SubjectInfoAccess";

    /**
     * Attribute name.
     */
    public static final String NAME = "SubjectInfoAccess";
    public static final String DESCRIPTIONS = "descriptions";

    /**
     * The List of AccessDescription objects.
     */
    private List<AccessDescription> accessDescriptions;

    /**
     * Create an SubjectInfoAccessExtension from a List of
     * AccessDescription; the criticality is set to false.
     *
     * @param accessDescriptions the List of AccessDescription
     * @throws IOException on error
     */
    public SubjectInfoAccessExtension(
            List<AccessDescription> accessDescriptions) throws IOException {
        this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
        this.critical = false;
        this.accessDescriptions = accessDescriptions;
        encodeThis();
    }

    /**
     * Create the extension from the passed DER encoded value of the same.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value Array of DER encoded bytes of the actual value.
     * @exception IOException on error.
     */
    public SubjectInfoAccessExtension(Boolean critical, Object value)
            throws IOException {
        this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
        this.critical = critical.booleanValue();

        if (!(value instanceof byte[])) {
            throw new IOException("Illegal argument type");
        }

        extensionValue = (byte[])value;
        DerValue val = new DerValue(extensionValue);
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding for " +
                                  "SubjectInfoAccessExtension.");
        }
        accessDescriptions = new ArrayList<AccessDescription>();
        while (val.data.available() != 0) {
            DerValue seq = val.data.getDerValue();
            AccessDescription accessDescription = new AccessDescription(seq);
            accessDescriptions.add(accessDescription);
        }
    }

    /**
     * Return the list of AccessDescription objects.
     */
    public List<AccessDescription> getAccessDescriptions() {
        return accessDescriptions;
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.SubjectInfoAccess_Id;
            this.critical = false;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    @SuppressWarnings("unchecked") // Checked with instanceof
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(DESCRIPTIONS)) {
            if (!(obj instanceof List)) {
                throw new IOException("Attribute value should be of type List.");
            }
            accessDescriptions = (List<AccessDescription>)obj;
        } else {
            throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:SubjectInfoAccessExtension.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public List<AccessDescription> get(String name) throws IOException {
        if (name.equalsIgnoreCase(DESCRIPTIONS)) {
            return accessDescriptions;
        } else {
            throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:SubjectInfoAccessExtension.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(DESCRIPTIONS)) {
            accessDescriptions =
                Collections.<AccessDescription>emptyList();
        } else {
            throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:SubjectInfoAccessExtension.");
        }
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(DESCRIPTIONS);
        return elements.elements();
    }

     // Encode this extension value
    private void encodeThis() throws IOException {
        if (accessDescriptions.isEmpty()) {
            this.extensionValue = null;
        } else {
            DerOutputStream ads = new DerOutputStream();
            for (AccessDescription accessDescription : accessDescriptions) {
                accessDescription.encode(ads);
            }
            DerOutputStream seq = new DerOutputStream();
            seq.write(DerValue.tag_Sequence, ads);
            this.extensionValue = seq.toByteArray();
        }
    }

    /**
     * Return the extension as user readable string.
     */
    public String toString() {
        return super.toString() +
            "SubjectInfoAccess [\n  " + accessDescriptions + "\n]\n";
    }

}
