/*
 * Copyright (c) 2019, Red Hat, Inc.
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

package sun.security.krb5.internal;

import java.io.IOException;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.internal.util.KerberosFlags;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

/**
 * Implements the ASN.1 PA-PAC-OPTIONS type.
 *
 * <pre>{@code
 * PA-PAC-OPTIONS ::= SEQUENCE {
 * KerberosFlags
 *   -- Claims (0)
 *   -- Branch Aware (1)
 *   -- Forward to Full DC (2)
 * }
 * Note: KerberosFlags   ::= BIT STRING (SIZE (32..MAX))
 *         -- minimum number of bits shall be sent, but no fewer than 32
 *
 * PA-PAC-OPTIONS ::= KerberosFlags
 *   -- resource-based constrained delegation (3)
 * }</pre>
 *
 * This definition reflects MS-KILE (section 2.2.10)
 * and MS-SFU (section 2.2.5).
 */

public class PaPacOptions {

    private static final int CLAIMS = 0;
    private static final int BRANCH_AWARE = 1;
    private static final int FORWARD_TO_FULL_DC = 2;
    private static final int RESOURCE_BASED_CONSTRAINED_DELEGATION = 3;

    private KerberosFlags flags;

    public PaPacOptions() {
        this.flags = new KerberosFlags(Krb5.AP_OPTS_MAX + 1);
    }

    /**
     * Constructs a PA-PAC-OPTIONS object from a DER encoding.
     * @param encoding the ASN.1 encoded input
     * @throws Asn1Exception if invalid DER
     * @throws IOException if there is an error reading the DER value
     */
    public PaPacOptions(DerValue encoding) throws Asn1Exception, IOException {
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        DerValue der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x00) {
            flags = new KDCOptions(
                    der.getData().getDerValue());
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Setter for the claims flag
     * @param value whether the claims flag is set or not
     * @return the same PaPacOptions instance
     */
    public PaPacOptions setClaims(boolean value) {
        flags.set(CLAIMS, value);
        return this;
    }

    /**
     * Getter for the claims flag
     * @return the claims flag value
     */
    public boolean getClaims() {
        return flags.get(CLAIMS);
    }

    /**
     * Setter for the branch-aware flag
     * @param value whether the branch-aware flag is set or not
     * @return the same PaPacOptions instance
     */
    public PaPacOptions setBranchAware(boolean value) {
        flags.set(BRANCH_AWARE, value);
        return this;
    }

    /**
     * Getter for the branch-aware flag
     * @return the branch-aware flag value
     */
    public boolean getBranchAware() {
        return flags.get(BRANCH_AWARE);
    }

    /**
     * Setter for the forward-to-full-DC flag
     * @param value whether the forward-to-full-DC flag is set or not
     * @return the same PaPacOptions instance
     */
    public PaPacOptions setForwardToFullDC(boolean value) {
        flags.set(FORWARD_TO_FULL_DC, value);
        return this;
    }

    /**
     * Getter for the forward-to-full-DC flag
     * @return the forward-to-full-DC flag value
     */
    public boolean getForwardToFullDC() {
        return flags.get(FORWARD_TO_FULL_DC);
    }

    /**
     * Setter for the resource-based-constrained-delegation flag
     * @param value whether the resource-based-constrained-delegation
     *        is set or not
     * @return the same PaPacOptions instance
     */
    public PaPacOptions setResourceBasedConstrainedDelegation(boolean value) {
        flags.set(RESOURCE_BASED_CONSTRAINED_DELEGATION, value);
        return this;
    }

    /**
     * Getter for the resource-based-constrained-delegation flag
     * @return the resource-based-constrained-delegation flag value
     */
    public boolean getResourceBasedConstrainedDelegation() {
        return flags.get(RESOURCE_BASED_CONSTRAINED_DELEGATION);
    }

    /**
     * Encodes this PaPacOptions instance.
     * @return an ASN.1 encoded PaPacOptions byte array
     * @throws IOException if an I/O error occurs while encoding this
     *         PaPacOptions instance
     */
    public byte[] asn1Encode() throws IOException {
        byte[] bytes = null;
        try(DerOutputStream temp = new DerOutputStream()) {
            temp.write(
                    DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00),
                    flags.asn1Encode());
            bytes = temp.toByteArray();
        }
        try(DerOutputStream temp = new DerOutputStream()) {
            temp.write(DerValue.tag_Sequence, bytes);
            return temp.toByteArray();
        }
    }

    @Override
    public String toString() {
        return flags.toString();
    }
}
