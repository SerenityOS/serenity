/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.security.auth.x500.X500Principal;

import sun.net.util.IPAddressUtil;
import sun.security.util.*;
import sun.security.pkcs.PKCS9Attribute;

/**
 * This class defines the Name Constraints Extension.
 * <p>
 * The name constraints extension provides permitted and excluded
 * subtrees that place restrictions on names that may be included within
 * a certificate issued by a given CA.  Restrictions may apply to the
 * subject distinguished name or subject alternative names.  Any name
 * matching a restriction in the excluded subtrees field is invalid
 * regardless of information appearing in the permitted subtrees.
 * <p>
 * The ASN.1 syntax for this is:
 * <pre>
 * NameConstraints ::= SEQUENCE {
 *    permittedSubtrees [0]  GeneralSubtrees OPTIONAL,
 *    excludedSubtrees  [1]  GeneralSubtrees OPTIONAL
 * }
 * GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
 * </pre>
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see Extension
 * @see CertAttrSet
 */
public class NameConstraintsExtension extends Extension
implements CertAttrSet<String>, Cloneable {
    /**
     * Identifier for this attribute, to be used with the
     * get, set, delete methods of Certificate, x509 type.
     */
    public static final String IDENT = "x509.info.extensions.NameConstraints";
    /**
     * Attribute names.
     */
    public static final String NAME = "NameConstraints";
    public static final String PERMITTED_SUBTREES = "permitted_subtrees";
    public static final String EXCLUDED_SUBTREES = "excluded_subtrees";

    // Private data members
    private static final byte TAG_PERMITTED = 0;
    private static final byte TAG_EXCLUDED = 1;

    private GeneralSubtrees     permitted = null;
    private GeneralSubtrees     excluded = null;

    private boolean hasMin;
    private boolean hasMax;
    private boolean minMaxValid = false;

    // Recalculate hasMin and hasMax flags.
    private void calcMinMax() throws IOException {
        hasMin = false;
        hasMax = false;
        if (excluded != null) {
            for (int i = 0; i < excluded.size(); i++) {
                GeneralSubtree subtree = excluded.get(i);
                if (subtree.getMinimum() != 0)
                    hasMin = true;
                if (subtree.getMaximum() != -1)
                    hasMax = true;
            }
        }

        if (permitted != null) {
            for (int i = 0; i < permitted.size(); i++) {
                GeneralSubtree subtree = permitted.get(i);
                if (subtree.getMinimum() != 0)
                    hasMin = true;
                if (subtree.getMaximum() != -1)
                    hasMax = true;
            }
        }
        minMaxValid = true;
    }

    // Encode this extension value.
    private void encodeThis() throws IOException {
        minMaxValid = false;
        if (permitted == null && excluded == null) {
            this.extensionValue = null;
            return;
        }
        DerOutputStream seq = new DerOutputStream();

        DerOutputStream tagged = new DerOutputStream();
        if (permitted != null) {
            DerOutputStream tmp = new DerOutputStream();
            permitted.encode(tmp);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT,
                                 true, TAG_PERMITTED), tmp);
        }
        if (excluded != null) {
            DerOutputStream tmp = new DerOutputStream();
            excluded.encode(tmp);
            tagged.writeImplicit(DerValue.createTag(DerValue.TAG_CONTEXT,
                                 true, TAG_EXCLUDED), tmp);
        }
        seq.write(DerValue.tag_Sequence, tagged);
        this.extensionValue = seq.toByteArray();
    }

    /**
     * The default constructor for this class. Both parameters
     * are optional and can be set to null.  The extension criticality
     * is set to true.
     *
     * @param permitted the permitted GeneralSubtrees (null for optional).
     * @param excluded the excluded GeneralSubtrees (null for optional).
     */
    public NameConstraintsExtension(GeneralSubtrees permitted,
                                    GeneralSubtrees excluded)
    throws IOException {
        this.permitted = permitted;
        this.excluded = excluded;

        this.extensionId = PKIXExtensions.NameConstraints_Id;
        this.critical = true;
        encodeThis();
    }

    /**
     * Create the extension from the passed DER encoded value.
     *
     * @param critical true if the extension is to be treated as critical.
     * @param value an array of DER encoded bytes of the actual value.
     * @exception ClassCastException if value is not an array of bytes
     * @exception IOException on error.
     */
    public NameConstraintsExtension(Boolean critical, Object value)
    throws IOException {
        this.extensionId = PKIXExtensions.NameConstraints_Id;
        this.critical = critical.booleanValue();

        this.extensionValue = (byte[]) value;
        DerValue val = new DerValue(this.extensionValue);
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding for" +
                                  " NameConstraintsExtension.");
        }

        // NB. this is always encoded with the IMPLICIT tag
        // The checks only make sense if we assume implicit tagging,
        // with explicit tagging the form is always constructed.
        // Note that all the fields in NameConstraints are defined as
        // being OPTIONAL, i.e., there could be an empty SEQUENCE, resulting
        // in val.data being null.
        if (val.data == null)
            return;
        while (val.data.available() != 0) {
            DerValue opt = val.data.getDerValue();

            if (opt.isContextSpecific(TAG_PERMITTED) && opt.isConstructed()) {
                if (permitted != null) {
                    throw new IOException("Duplicate permitted " +
                         "GeneralSubtrees in NameConstraintsExtension.");
                }
                opt.resetTag(DerValue.tag_Sequence);
                permitted = new GeneralSubtrees(opt);

            } else if (opt.isContextSpecific(TAG_EXCLUDED) &&
                       opt.isConstructed()) {
                if (excluded != null) {
                    throw new IOException("Duplicate excluded " +
                             "GeneralSubtrees in NameConstraintsExtension.");
                }
                opt.resetTag(DerValue.tag_Sequence);
                excluded = new GeneralSubtrees(opt);
            } else
                throw new IOException("Invalid encoding of " +
                                      "NameConstraintsExtension.");
        }
        minMaxValid = false;
    }

    /**
     * Return the printable string.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString())
            .append("NameConstraints: [");
        if (permitted != null) {
            sb.append("\n    Permitted:")
                .append(permitted);
        }
        if (excluded != null) {
            sb.append("\n    Excluded:")
                .append(excluded);
        }
        sb.append("   ]\n");
        return sb.toString();
    }

    /**
     * Write the extension to the OutputStream.
     *
     * @param out the OutputStream to write the extension to.
     * @exception IOException on encoding errors.
     */
    public void encode(OutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        if (this.extensionValue == null) {
            this.extensionId = PKIXExtensions.NameConstraints_Id;
            this.critical = true;
            encodeThis();
        }
        super.encode(tmp);
        out.write(tmp.toByteArray());
    }

    /**
     * Set the attribute value.
     */
    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(PERMITTED_SUBTREES)) {
            if (!(obj instanceof GeneralSubtrees)) {
                throw new IOException("Attribute value should be"
                                    + " of type GeneralSubtrees.");
            }
            permitted = (GeneralSubtrees)obj;
        } else if (name.equalsIgnoreCase(EXCLUDED_SUBTREES)) {
            if (!(obj instanceof GeneralSubtrees)) {
                throw new IOException("Attribute value should be "
                                    + "of type GeneralSubtrees.");
            }
            excluded = (GeneralSubtrees)obj;
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:NameConstraintsExtension.");
        }
        encodeThis();
    }

    /**
     * Get the attribute value.
     */
    public GeneralSubtrees get(String name) throws IOException {
        if (name.equalsIgnoreCase(PERMITTED_SUBTREES)) {
            return (permitted);
        } else if (name.equalsIgnoreCase(EXCLUDED_SUBTREES)) {
            return (excluded);
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:NameConstraintsExtension.");
        }
    }

    /**
     * Delete the attribute value.
     */
    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(PERMITTED_SUBTREES)) {
            permitted = null;
        } else if (name.equalsIgnoreCase(EXCLUDED_SUBTREES)) {
            excluded = null;
        } else {
          throw new IOException("Attribute name not recognized by " +
                        "CertAttrSet:NameConstraintsExtension.");
        }
        encodeThis();
    }

    /**
     * Return an enumeration of names of attributes existing within this
     * attribute.
     */
    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(PERMITTED_SUBTREES);
        elements.addElement(EXCLUDED_SUBTREES);

        return (elements.elements());
    }

    /**
     * Return the name of this attribute.
     */
    public String getName() {
        return (NAME);
    }

    /**
     * Merge additional name constraints with existing ones.
     * This function is used in certification path processing
     * to accumulate name constraints from successive certificates
     * in the path.  Note that NameConstraints can never be
     * expanded by a merge, just remain constant or become more
     * limiting.
     * <p>
     * IETF RFC 5280 specifies the processing of Name Constraints as
     * follows:
     * <p>
     * (j)  If permittedSubtrees is present in the certificate, set the
     * constrained subtrees state variable to the intersection of its
     * previous value and the value indicated in the extension field.
     * <p>
     * (k)  If excludedSubtrees is present in the certificate, set the
     * excluded subtrees state variable to the union of its previous
     * value and the value indicated in the extension field.
     *
     * @param newConstraints additional NameConstraints to be applied
     * @throws IOException on error
     */
    public void merge(NameConstraintsExtension newConstraints)
            throws IOException {

        if (newConstraints == null) {
            // absence of any explicit constraints implies unconstrained
            return;
        }

        /*
         * If excludedSubtrees is present in the certificate, set the
         * excluded subtrees state variable to the union of its previous
         * value and the value indicated in the extension field.
         */

        GeneralSubtrees newExcluded = newConstraints.get(EXCLUDED_SUBTREES);
        if (excluded == null) {
            excluded = (newExcluded != null) ?
                        (GeneralSubtrees)newExcluded.clone() : null;
        } else {
            if (newExcluded != null) {
                // Merge new excluded with current excluded (union)
                excluded.union(newExcluded);
            }
        }

        /*
         * If permittedSubtrees is present in the certificate, set the
         * constrained subtrees state variable to the intersection of its
         * previous value and the value indicated in the extension field.
         */

        GeneralSubtrees newPermitted = newConstraints.get(PERMITTED_SUBTREES);
        if (permitted == null) {
            permitted = (newPermitted != null) ?
                        (GeneralSubtrees)newPermitted.clone() : null;
        } else {
            if (newPermitted != null) {
                // Merge new permitted with current permitted (intersection)
                newExcluded = permitted.intersect(newPermitted);

                // Merge new excluded subtrees to current excluded (union)
                if (newExcluded != null) {
                    if (excluded != null) {
                        excluded.union(newExcluded);
                    } else {
                        excluded = (GeneralSubtrees)newExcluded.clone();
                    }
                }
            }
        }

        // Optional optimization: remove permitted subtrees that are excluded.
        // This is not necessary for algorithm correctness, but it makes
        // subsequent operations on the NameConstraints faster and require
        // less space.
        if (permitted != null) {
            permitted.reduce(excluded);
        }

        // The NameConstraints have been changed, so re-encode them.  Methods in
        // this class assume that the encodings have already been done.
        encodeThis();

    }

    /**
     * check whether a certificate conforms to these NameConstraints.
     * This involves verifying that the subject name and subjectAltName
     * extension (critical or noncritical) is consistent with the permitted
     * subtrees state variables.  Also verify that the subject name and
     * subjectAltName extension (critical or noncritical) is consistent with
     * the excluded subtrees state variables.
     *
     * @param cert X509Certificate to be verified
     * @return true if certificate verifies successfully
     * @throws IOException on error
     */
    public boolean verify(X509Certificate cert) throws IOException {

        if (cert == null) {
            throw new IOException("Certificate is null");
        }

        // Calculate hasMin and hasMax booleans (if necessary)
        if (!minMaxValid) {
            calcMinMax();
        }

        if (hasMin) {
            throw new IOException("Non-zero minimum BaseDistance in"
                                + " name constraints not supported");
        }

        if (hasMax) {
            throw new IOException("Maximum BaseDistance in"
                                + " name constraints not supported");
        }

        X500Principal subjectPrincipal = cert.getSubjectX500Principal();
        X500Name subject = X500Name.asX500Name(subjectPrincipal);

        // Check subject as an X500Name
        if (subject.isEmpty() == false) {
            if (verify(subject) == false) {
                return false;
            }
        }

        GeneralNames altNames = null;
        // extract altNames
        try {
            // extract extensions, if any, from certInfo
            // following returns null if certificate contains no extensions
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            SubjectAlternativeNameExtension altNameExt =
                certImpl.getSubjectAlternativeNameExtension();
            if (altNameExt != null) {
                // extract altNames from extension; this call does not
                // return an IOException on null altnames
                altNames = altNameExt.get(
                        SubjectAlternativeNameExtension.SUBJECT_NAME);
            }
        } catch (CertificateException ce) {
            throw new IOException("Unable to extract extensions from " +
                        "certificate: " + ce.getMessage());
        }

        if (altNames == null) {
            altNames = new GeneralNames();

            // RFC 5280 4.2.1.10:
            // When constraints are imposed on the rfc822Name name form,
            // but the certificate does not include a subject alternative name,
            // the rfc822Name constraint MUST be applied to the attribute of
            // type emailAddress in the subject distinguished name.
            for (AVA ava : subject.allAvas()) {
                ObjectIdentifier attrOID = ava.getObjectIdentifier();
                if (attrOID.equals(PKCS9Attribute.EMAIL_ADDRESS_OID)) {
                    String attrValue = ava.getValueString();
                    if (attrValue != null) {
                        try {
                            altNames.add(new GeneralName(
                                    new RFC822Name(attrValue)));
                        } catch (IOException ioe) {
                            continue;
                        }
                    }
                }
            }
        }

        // If there is no IPAddressName or DNSName in subjectAlternativeNames,
        // see if the last CN inside subjectName can be used instead.
        DerValue derValue = subject.findMostSpecificAttribute
                (X500Name.commonName_oid);
        String cn = derValue == null ? null : derValue.getAsString();

        if (cn != null) {
            try {
                if (IPAddressUtil.isIPv4LiteralAddress(cn) ||
                        IPAddressUtil.isIPv6LiteralAddress(cn)) {
                    if (!hasNameType(altNames, GeneralNameInterface.NAME_IP)) {
                        altNames.add(new GeneralName(new IPAddressName(cn)));
                    }
                } else {
                    if (!hasNameType(altNames, GeneralNameInterface.NAME_DNS)) {
                        altNames.add(new GeneralName(new DNSName(cn)));
                    }
                }
            } catch (IOException ioe) {
                // OK, cn is neither IP nor DNS
            }
        }

        // verify each subjectAltName
        for (int i = 0; i < altNames.size(); i++) {
            GeneralNameInterface altGNI = altNames.get(i).getName();
            if (!verify(altGNI)) {
                return false;
            }
        }

        // All tests passed.
        return true;
    }

    private static boolean hasNameType(GeneralNames names, int type) {
        for (GeneralName name : names.names()) {
            if (name.getType() == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * check whether a name conforms to these NameConstraints.
     * This involves verifying that the name is consistent with the
     * permitted and excluded subtrees variables.
     *
     * @param name GeneralNameInterface name to be verified
     * @return true if certificate verifies successfully
     * @throws IOException on error
     */
    public boolean verify(GeneralNameInterface name) throws IOException {
        if (name == null) {
            throw new IOException("name is null");
        }

        // Verify that the name is consistent with the excluded subtrees
        if (excluded != null && excluded.size() > 0) {

            for (int i = 0; i < excluded.size(); i++) {
                GeneralSubtree gs = excluded.get(i);
                if (gs == null)
                    continue;
                GeneralName gn = gs.getName();
                if (gn == null)
                    continue;
                GeneralNameInterface exName = gn.getName();
                if (exName == null)
                    continue;

                // if name matches or narrows any excluded subtree,
                // return false
                switch (exName.constrains(name)) {
                case GeneralNameInterface.NAME_DIFF_TYPE:
                case GeneralNameInterface.NAME_WIDENS: // name widens excluded
                case GeneralNameInterface.NAME_SAME_TYPE:
                    break;
                case GeneralNameInterface.NAME_MATCH:
                case GeneralNameInterface.NAME_NARROWS: // subject name excluded
                    return false;
                }
            }
        }

        // Verify that the name is consistent with the permitted subtrees
        if (permitted != null && permitted.size() > 0) {

            boolean sameType = false;

            for (int i = 0; i < permitted.size(); i++) {
                GeneralSubtree gs = permitted.get(i);
                if (gs == null)
                    continue;
                GeneralName gn = gs.getName();
                if (gn == null)
                    continue;
                GeneralNameInterface perName = gn.getName();
                if (perName == null)
                    continue;

                // if Name matches any type in permitted,
                // and Name does not match or narrow some permitted subtree,
                // return false
                switch (perName.constrains(name)) {
                case GeneralNameInterface.NAME_DIFF_TYPE:
                    continue; // continue checking other permitted names
                case GeneralNameInterface.NAME_WIDENS: // name widens permitted
                case GeneralNameInterface.NAME_SAME_TYPE:
                    sameType = true;
                    continue; // continue to look for a match or narrow
                case GeneralNameInterface.NAME_MATCH:
                case GeneralNameInterface.NAME_NARROWS:
                    // name narrows permitted
                    return true; // name is definitely OK, so break out of loop
                }
            }
            if (sameType) {
                return false;
            }
        }
        return true;
    }

    /**
     * Clone all objects that may be modified during certificate validation.
     */
    public Object clone() {
        try {
            NameConstraintsExtension newNCE =
                (NameConstraintsExtension) super.clone();

            if (permitted != null) {
                newNCE.permitted = (GeneralSubtrees) permitted.clone();
            }
            if (excluded != null) {
                newNCE.excluded = (GeneralSubtrees) excluded.clone();
            }
            return newNCE;
        } catch (CloneNotSupportedException cnsee) {
            throw new RuntimeException("CloneNotSupportedException while " +
                "cloning NameConstraintsException. This should never happen.");
        }
    }
}
