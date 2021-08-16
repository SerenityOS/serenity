/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import sun.security.util.DerValue;
import sun.security.util.DerOutputStream;

/**
 * This class defines the certificate policies extension which specifies the
 * policies under which the certificate has been issued
 * and the purposes for which the certificate may be used.
 * <p>
 * Applications with specific policy requirements are expected to have a
 * list of those policies which they will accept and to compare the
 * policy OIDs in the certificate to that list.  If this extension is
 * critical, the path validation software MUST be able to interpret this
 * extension (including the optional qualifier), or MUST reject the
 * certificate.
 * <p>
 * Optional qualifiers are not supported in this implementation, as they are
 * not recommended by RFC 5280.
 *
 * The ASN.1 syntax for this is (IMPLICIT tagging is defined in the
 * module definition):
 * <pre>
 * id-ce-certificatePolicies OBJECT IDENTIFIER ::=  { id-ce 32 }
 *
 * certificatePolicies ::= SEQUENCE SIZE (1..MAX) OF PolicyInformation
 *
 * PolicyInformation ::= SEQUENCE {
 *      policyIdentifier   CertPolicyId,
 *      policyQualifiers   SEQUENCE SIZE (1..MAX) OF
 *                              PolicyQualifierInfo OPTIONAL }
 *
 * CertPolicyId ::= OBJECT IDENTIFIER
 * </pre>
 * @author Anne Anderson
 * @since       1.4
 * @see Extension
 * @see CertAttrSet
 */
public class CertificatePoliciesExtension extends Extension
implements CertAttrSet<String> {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.CertificatePolicies";
    /**
     * Attribute names.
     */
    public static final String NAME = "CertificatePolicies";
    public static final String POLICIES = "policies";

    /**
     * List of PolicyInformation for this object.
     */
    private List<PolicyInformation> certPolicies;

    // Encode this extension value.
    private void encodeThis() throws IOException {
        if (certPolicies == null || certPolicies.isEmpty()) {
            this.extensionValue = null;
        } else {
            DerOutputStream os = new DerOutputStream();
            DerOutputStream tmp = new DerOutputStream();

            for (PolicyInformation info : certPolicies) {
                info.encode(tmp);
            }

            os.write(DerValue.tag_Sequence, tmp);
            this.extensionValue = os.toByteArray();
        }
    }

    /**
     * Create a CertificatePoliciesExtension object from
     * a List of PolicyInformation; the criticality is set to false.
     *
     * @param certPolicies the List of PolicyInformation.
     */
    public CertificatePoliciesExtension(List<PolicyInformation> certPolicies)
    throws IOException {
        this(Boolean.FALSE, certPolicies);
    }

    /**
     * Create a CertificatePoliciesExtension object from
     * a List of PolicyInformation with specified criticality.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param certPolicies the List of PolicyInformation.
     */
    public CertificatePoliciesExtension(Boolean critical,
            List<PolicyInformation> certPolicies) throws IOException {
        this.certPolicies = certPolicies;
        this.extensionId = PKIXExtensions.CertificatePolicies_Id;
        this.critical = critical.booleanValue();
        encodeThis();
    }

    /**
     * Create the extension from its DER encoded value and criticality.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public CertificatePoliciesExtension(Boolean critical, Object value)
    throws IOException {
        this.extensionId = PKIXExtensions.CertificatePolicies_Id;
        this.critical = critical.booleanValue();
        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding for " +
                                   "CertificatePoliciesExtension.");
        }
        certPolicies = new ArrayList<PolicyInformation>();
        while (val.data.available() != 0) {
            DerValue seq = val.data.getDerValue();
            PolicyInformation policy = new PolicyInformation(seq);
            certPolicies.add(policy);
        }
    }

    /**
     * Return the extension as user readable string.
     */
    public String toString() {
        if (certPolicies == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(super.toString())
            .append("CertificatePolicies [\n");
        for (PolicyInformation info : certPolicies) {
            sb.append(info);
        }
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Write the extension to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (extensionValue == null) {
          extensionId = PKIXExtensions.CertificatePolicies_Id;
          critical = false;
          encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    @SuppressWarnings("unchecked") // Checked with an instanceof check
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(POLICIES)) {
            if (!(obj instanceof List)) {
                throw new IOException("Attribute value should be of type List.");
            }
            certPolicies = (List<PolicyInformation>)obj;
        } else {
          throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:CertificatePoliciesExtension.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public List<PolicyInformation> get(String name) throws IOException {
        if (name.equalsIgnoreCase(POLICIES)) {
            //XXXX May want to consider cloning this
            return certPolicies;
        } else {
          throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:CertificatePoliciesExtension.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(POLICIES)) {
            certPolicies = null;
        } else {
          throw new IOException("Attribute name [" + name +
                                "] not recognized by " +
                                "CertAttrSet:CertificatePoliciesExtension.");
        }
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(POLICIES);

        return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return (NAME);
    }
}
