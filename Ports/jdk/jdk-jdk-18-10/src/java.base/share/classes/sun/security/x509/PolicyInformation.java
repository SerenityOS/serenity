/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.PolicyQualifierInfo;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import sun.security.util.DerValue;
import sun.security.util.DerOutputStream;
/**
 * PolicyInformation is the class that contains a specific certificate policy
 * that is part of the CertificatePoliciesExtension. A
 * CertificatePolicyExtension value consists of a vector of these objects.
 * <p>
 * The ASN.1 syntax for PolicyInformation (IMPLICIT tagging is defined in the
 * module definition):
 * <pre>
 *
 * PolicyInformation ::= SEQUENCE {
 *      policyIdentifier   CertPolicyId,
 *      policyQualifiers   SEQUENCE SIZE (1..MAX) OF
 *                              PolicyQualifierInfo OPTIONAL }
 *
 * CertPolicyId ::= OBJECT IDENTIFIER
 *
 * PolicyQualifierInfo ::= SEQUENCE {
 *      policyQualifierId  PolicyQualifierId,
 *      qualifier          ANY DEFINED BY policyQualifierId }
 * </pre>
 *
 * @author Sean Mullan
 * @author Anne Anderson
 * @since       1.4
 */
public class PolicyInformation {

    // Attribute names
    public static final String NAME       = "PolicyInformation";
    public static final String ID         = "id";
    public static final String QUALIFIERS = "qualifiers";

    /* The policy OID */
    private CertificatePolicyId policyIdentifier;

    /* A Set of java.security.cert.PolicyQualifierInfo objects */
    private Set<PolicyQualifierInfo> policyQualifiers;

    /**
     * Create an instance of PolicyInformation
     *
     * @param policyIdentifier the policyIdentifier as a
     *          CertificatePolicyId
     * @param policyQualifiers a Set of PolicyQualifierInfo objects.
     *          Must not be NULL. Specify an empty Set for no qualifiers.
     * @exception IOException on decoding errors.
     */
    public PolicyInformation(CertificatePolicyId policyIdentifier,
            Set<PolicyQualifierInfo> policyQualifiers) throws IOException {
        if (policyQualifiers == null) {
            throw new NullPointerException("policyQualifiers is null");
        }
        this.policyQualifiers =
            new LinkedHashSet<PolicyQualifierInfo>(policyQualifiers);
        this.policyIdentifier = policyIdentifier;
    }

    /**
     * Create an instance of PolicyInformation, decoding from
     * the passed DerValue.
     *
     * @param val the DerValue to construct the PolicyInformation from.
     * @exception IOException on decoding errors.
     */
    public PolicyInformation(DerValue val) throws IOException {
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding of PolicyInformation");
        }
        policyIdentifier = new CertificatePolicyId(val.data.getDerValue());
        if (val.data.available() != 0) {
            policyQualifiers = new LinkedHashSet<PolicyQualifierInfo>();
            DerValue opt = val.data.getDerValue();
            if (opt.tag != DerValue.tag_Sequence)
                throw new IOException("Invalid encoding of PolicyInformation");
            if (opt.data.available() == 0)
                throw new IOException("No data available in policyQualifiers");
            while (opt.data.available() != 0)
                policyQualifiers.add(new PolicyQualifierInfo
                        (opt.data.getDerValue().toByteArray()));
        } else {
            policyQualifiers = Collections.emptySet();
        }
    }

    /**
     * Compare this PolicyInformation with another object for equality
     *
     * @param other object to be compared with this
     * @return true iff the PolicyInformation objects match
     */
    public boolean equals(Object other) {
        if (!(other instanceof PolicyInformation))
            return false;
        PolicyInformation piOther = (PolicyInformation)other;

        if (!policyIdentifier.equals(piOther.getPolicyIdentifier()))
            return false;

        return policyQualifiers.equals(piOther.getPolicyQualifiers());
    }

    /**
     * Returns the hash code for this PolicyInformation.
     *
     * @return a hash code value.
     */
    public int hashCode() {
        int myhash = 37 + policyIdentifier.hashCode();
        myhash = 37 * myhash + policyQualifiers.hashCode();
        return myhash;
    }

    /**
     * Return the policyIdentifier value
     *
     * @return The CertificatePolicyId object containing
     *     the policyIdentifier (not a copy).
     */
    public CertificatePolicyId getPolicyIdentifier() {
        return policyIdentifier;
    }

    /**
     * Return the policyQualifiers value
     *
     * @return a Set of PolicyQualifierInfo objects associated
     *    with this certificate policy (not a copy).
     *    Returns an empty Set if there are no qualifiers.
     *    Never returns null.
     */
    public Set<PolicyQualifierInfo> getPolicyQualifiers() {
        return policyQualifiers;
    }

    /**
     * Get the attribute value.
     */
    public Object get(String name) throws IOException {
        if (name.equalsIgnoreCase(ID)) {
            return policyIdentifier;
        } else if (name.equalsIgnoreCase(QUALIFIERS)) {
            return policyQualifiers;
        } else {
            throw new IOException("Attribute name [" + name +
                "] not recognized by PolicyInformation.");
        }
    }

    /**
     * Set the attribute value.
     */
    @SuppressWarnings("unchecked") // Checked with instanceof
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(ID)) {
            if (obj instanceof CertificatePolicyId)
                policyIdentifier = (CertificatePolicyId)obj;
            else
                throw new IOException("Attribute value must be instance " +
                    "of CertificatePolicyId.");
        } else if (name.equalsIgnoreCase(QUALIFIERS)) {
            if (policyIdentifier == null) {
                throw new IOException("Attribute must have a " +
                    "CertificatePolicyIdentifier value before " +
                    "PolicyQualifierInfo can be set.");
            }
            if (obj instanceof Set) {
                Iterator<?> i = ((Set<?>)obj).iterator();
                while (i.hasNext()) {
                    Object obj1 = i.next();
                    if (!(obj1 instanceof PolicyQualifierInfo)) {
                        throw new IOException("Attribute value must be a" +
                                    "Set of PolicyQualifierInfo objects.");
                    }
                }
                policyQualifiers = (Set<PolicyQualifierInfo>) obj;
            } else {
                throw new IOException("Attribute value must be of type Set.");
            }
        } else {
            throw new IOException("Attribute name [" + name +
                "] not recognized by PolicyInformation");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(QUALIFIERS)) {
            policyQualifiers = Collections.emptySet();
        } else if (name.equalsIgnoreCase(ID)) {
            throw new IOException("Attribute ID may not be deleted from " +
                "PolicyInformation.");
        } else {
            //ID may not be deleted
            throw new IOException("Attribute name [" + name +
                "] not recognized by PolicyInformation.");
        }
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(ID);
        elements.addElement(QUALIFIERS);

        return elements.elements();
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return NAME;
    }

    /**
     * Return a printable representation of the PolicyInformation.
     */
    public String toString() {
        return "  [" + policyIdentifier + policyQualifiers + "  ]\n";
    }

    /**
     * Write the PolicyInformation to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        policyIdentifier.encode(tmp);
        if (!policyQualifiers.isEmpty()) {
            DerOutputStream tmp2 = new DerOutputStream();
            for (PolicyQualifierInfo pq : policyQualifiers) {
                tmp2.write(pq.getEncoded());
            }
            tmp.write(DerValue.tag_Sequence, tmp2);
        }
        out.write(DerValue.tag_Sequence, tmp);
    }
}
