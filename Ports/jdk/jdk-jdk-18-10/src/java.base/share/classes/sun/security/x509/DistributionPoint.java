/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * Represent the DistributionPoint sequence used in the CRL
 * Distribution Points Extension (OID = 2.5.29.31).
 * <p>
 * The ASN.1 definition for this is:
 * <pre>
 * DistributionPoint ::= SEQUENCE {
 *      distributionPoint       [0]     DistributionPointName OPTIONAL,
 *      reasons                 [1]     ReasonFlags OPTIONAL,
 *      cRLIssuer               [2]     GeneralNames OPTIONAL }
 *
 * DistributionPointName ::= CHOICE {
 *      fullName                [0]     GeneralNames,
 *      nameRelativeToCRLIssuer [1]     RelativeDistinguishedName }
 *
 * ReasonFlags ::= BIT STRING {
 *      unused                  (0),
 *      keyCompromise           (1),
 *      cACompromise            (2),
 *      affiliationChanged      (3),
 *      superseded              (4),
 *      cessationOfOperation    (5),
 *      certificateHold         (6),
 *      privilegeWithdrawn      (7),
 *      aACompromise            (8) }
 *
 * GeneralNames ::= SEQUENCE SIZE (1..MAX) OF GeneralName
 *
 * GeneralName ::= CHOICE {
 *         otherName                   [0] INSTANCE OF OTHER-NAME,
 *         rfc822Name                  [1] IA5String,
 *         dNSName                     [2] IA5String,
 *         x400Address                 [3] ORAddress,
 *         directoryName               [4] Name,
 *         ediPartyName                [5] EDIPartyName,
 *         uniformResourceIdentifier   [6] IA5String,
 *         iPAddress                   [7] OCTET STRING,
 *         registeredID                [8] OBJECT IDENTIFIER }
 *
 * RelativeDistinguishedName ::=
 *   SET OF AttributeTypeAndValue
 *
 * AttributeTypeAndValue ::= SEQUENCE {
 *   type     AttributeType,
 *   value    AttributeValue }
 *
 * AttributeType ::= OBJECT IDENTIFIER
 *
 * AttributeValue ::= ANY DEFINED BY AttributeType
 * </pre>
 * <p>
 * Instances of this class are designed to be immutable. However, since this
 * is an internal API we do not use defensive cloning for values for
 * performance reasons. It is the responsibility of the consumer to ensure
 * that no mutable elements are modified.
 *
 * @author Anne Anderson
 * @author Andreas Sterbenz
 * @since 1.4.2
 * @see CRLDistributionPointsExtension
 */
public class DistributionPoint {

    // reason flag bits
    // NOTE that these are NOT quite the same as the CRL reason code extension
    public static final int KEY_COMPROMISE         = 1;
    public static final int CA_COMPROMISE          = 2;
    public static final int AFFILIATION_CHANGED    = 3;
    public static final int SUPERSEDED             = 4;
    public static final int CESSATION_OF_OPERATION = 5;
    public static final int CERTIFICATE_HOLD       = 6;
    public static final int PRIVILEGE_WITHDRAWN    = 7;
    public static final int AA_COMPROMISE          = 8;

    private static final String[] REASON_STRINGS = {
        null,
        "key compromise",
        "CA compromise",
        "affiliation changed",
        "superseded",
        "cessation of operation",
        "certificate hold",
        "privilege withdrawn",
        "AA compromise",
    };

    // context specific tag values
    private static final byte TAG_DIST_PT = 0;
    private static final byte TAG_REASONS = 1;
    private static final byte TAG_ISSUER = 2;

    private static final byte TAG_FULL_NAME = 0;
    private static final byte TAG_REL_NAME = 1;

    // only one of fullName and relativeName can be set
    private GeneralNames fullName;
    private RDN relativeName;

    // reasonFlags or null
    private boolean[] reasonFlags;

    // crlIssuer or null
    private GeneralNames crlIssuer;

    // cached hashCode value
    private volatile int hashCode;

    /**
     * Constructor for the class using GeneralNames for DistributionPointName
     *
     * @param fullName the GeneralNames of the distribution point; may be null
     * @param reasonFlags the CRL reasons included in the CRL at this distribution
     *        point; may be null
     * @param crlIssuer the name(s) of the CRL issuer for the CRL at this
     *        distribution point; may be null
     */
    public DistributionPoint(GeneralNames fullName, boolean[] reasonFlags,
            GeneralNames crlIssuer) {
        if ((fullName == null) && (crlIssuer == null)) {
            throw new IllegalArgumentException
                        ("fullName and crlIssuer may not both be null");
        }
        this.fullName = fullName;
        this.reasonFlags = reasonFlags;
        this.crlIssuer = crlIssuer;
    }

    /**
     * Constructor for the class using RelativeDistinguishedName for
     * DistributionPointName
     *
     * @param relativeName the RelativeDistinguishedName of the distribution
     *        point; may not be null
     * @param reasonFlags the CRL reasons included in the CRL at this distribution
     *        point; may be null
     * @param crlIssuer the name(s) of the CRL issuer for the CRL at this
     *        distribution point; may not be null or empty.
     */
    public DistributionPoint(RDN relativeName, boolean[] reasonFlags,
            GeneralNames crlIssuer) {
        if ((relativeName == null) && (crlIssuer == null)) {
            throw new IllegalArgumentException
                        ("relativeName and crlIssuer may not both be null");
        }
        this.relativeName = relativeName;
        this.reasonFlags = reasonFlags;
        this.crlIssuer = crlIssuer;
    }

    /**
     * Create the object from the passed DER encoded form.
     *
     * @param val the DER encoded form of the DistributionPoint
     * @throws IOException on error
     */
    public DistributionPoint(DerValue val) throws IOException {
        if (val.tag != DerValue.tag_Sequence) {
            throw new IOException("Invalid encoding of DistributionPoint.");
        }

        // Note that all the fields in DistributionPoint are defined as
        // being OPTIONAL, i.e., there could be an empty SEQUENCE, resulting
        // in val.data being null.
        while ((val.data != null) && (val.data.available() != 0)) {
            DerValue opt = val.data.getDerValue();

            if (opt.isContextSpecific(TAG_DIST_PT) && opt.isConstructed()) {
                if ((fullName != null) || (relativeName != null)) {
                    throw new IOException("Duplicate DistributionPointName in "
                                          + "DistributionPoint.");
                }
                DerValue distPnt = opt.data.getDerValue();
                if (distPnt.isContextSpecific(TAG_FULL_NAME)
                        && distPnt.isConstructed()) {
                    distPnt.resetTag(DerValue.tag_Sequence);
                    fullName = new GeneralNames(distPnt);
                } else if (distPnt.isContextSpecific(TAG_REL_NAME)
                        && distPnt.isConstructed()) {
                    distPnt.resetTag(DerValue.tag_Set);
                    relativeName = new RDN(distPnt);
                } else {
                    throw new IOException("Invalid DistributionPointName in "
                                          + "DistributionPoint");
                }
            } else if (opt.isContextSpecific(TAG_REASONS)
                                                && !opt.isConstructed()) {
                if (reasonFlags != null) {
                    throw new IOException("Duplicate Reasons in " +
                                          "DistributionPoint.");
                }
                opt.resetTag(DerValue.tag_BitString);
                reasonFlags = (opt.getUnalignedBitString()).toBooleanArray();
            } else if (opt.isContextSpecific(TAG_ISSUER)
                                                && opt.isConstructed()) {
                if (crlIssuer != null) {
                    throw new IOException("Duplicate CRLIssuer in " +
                                          "DistributionPoint.");
                }
                opt.resetTag(DerValue.tag_Sequence);
                crlIssuer = new GeneralNames(opt);
            } else {
                throw new IOException("Invalid encoding of " +
                                      "DistributionPoint.");
            }
        }
        if ((crlIssuer == null) && (fullName == null) && (relativeName == null)) {
            throw new IOException("One of fullName, relativeName, "
                + " and crlIssuer has to be set");
        }
    }

    /**
     * Return the full distribution point name or null if not set.
     */
    public GeneralNames getFullName() {
        return fullName;
    }

    /**
     * Return the relative distribution point name or null if not set.
     */
    public RDN getRelativeName() {
        return relativeName;
    }

    /**
     * Return the reason flags or null if not set.
     */
    public boolean[] getReasonFlags() {
        return reasonFlags;
    }

    /**
     * Return the CRL issuer name or null if not set.
     */
    public GeneralNames getCRLIssuer() {
        return crlIssuer;
    }

    /**
     * Write the DistributionPoint value to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the extension to.
     * @exception IOException on error.
     */
    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tagged = new DerOutputStream();

        // NOTE: only one of pointNames and pointRDN can be set
        if ((fullName != null) || (relativeName != null)) {
            DerOutputStream distributionPoint = new DerOutputStream();
            if (fullName != null) {
                DerOutputStream derOut = new DerOutputStream();
                fullName.encode(derOut);
                distributionPoint.writeImplicit(
                    DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_FULL_NAME),
                    derOut);
            } else if (relativeName != null) {
                DerOutputStream derOut = new DerOutputStream();
                relativeName.encode(derOut);
                distributionPoint.writeImplicit(
                    DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_REL_NAME),
                    derOut);
            }
            tagged.write(
                DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_DIST_PT),
                distributionPoint);
        }
        if (reasonFlags != null) {
            DerOutputStream reasons = new DerOutputStream();
            BitArray rf = new BitArray(reasonFlags);
            reasons.putTruncatedUnalignedBitString(rf);
            tagged.writeImplicit(
                DerValue.createTag(DerValue.TAG_CONTEXT, false, TAG_REASONS),
                reasons);
        }
        if (crlIssuer != null) {
            DerOutputStream issuer = new DerOutputStream();
            crlIssuer.encode(issuer);
            tagged.writeImplicit(
                DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_ISSUER),
                issuer);
        }
        out.write(DerValue.tag_Sequence, tagged);
    }

    /**
     * Compare an object to this DistributionPoint for equality.
     *
     * @param obj Object to be compared to this
     * @return true if objects match; false otherwise
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DistributionPoint == false) {
            return false;
        }
        DistributionPoint other = (DistributionPoint)obj;

        boolean equal = Objects.equals(this.fullName, other.fullName)
                     && Objects.equals(this.relativeName, other.relativeName)
                     && Objects.equals(this.crlIssuer, other.crlIssuer)
                     && Arrays.equals(this.reasonFlags, other.reasonFlags);
        return equal;
    }

    public int hashCode() {
        int hash = hashCode;
        if (hash == 0) {
            hash = 1;
            if (fullName != null) {
                hash += fullName.hashCode();
            }
            if (relativeName != null) {
                hash += relativeName.hashCode();
            }
            if (crlIssuer != null) {
                hash += crlIssuer.hashCode();
            }
            if (reasonFlags != null) {
                for (int i = 0; i < reasonFlags.length; i++) {
                    if (reasonFlags[i]) {
                        hash += i;
                    }
                }
            }
            hashCode = hash;
        }
        return hash;
    }

    /**
     * Return a string representation for reasonFlag bit 'reason'.
     */
    private static String reasonToString(int reason) {
        if ((reason > 0) && (reason < REASON_STRINGS.length)) {
            return REASON_STRINGS[reason];
        }
        return "Unknown reason " + reason;
    }

    /**
     * Return a printable string of the Distribution Point.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DistributionPoint:\n     ");
        if (fullName != null) {
            sb.append(fullName);
        }
        if (relativeName != null) {
            sb.append(relativeName);
        }
        sb.append('\n');

        if (reasonFlags != null) {
            sb.append("   ReasonFlags:\n");
            for (int i = 0; i < reasonFlags.length; i++) {
                if (reasonFlags[i]) {
                    sb.append("    ")
                        .append(reasonToString(i))
                        .append('\n');
                }
            }
        }
        if (crlIssuer != null) {
            sb.append("   CRLIssuer:")
                .append(crlIssuer)
                .append('\n');
        }
        return sb.toString();
    }

}
