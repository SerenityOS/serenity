/*
 * Copyright (c) 1997, 2004, Oracle and/or its affiliates. All rights reserved.
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
import sun.security.util.*;

/**
 * This class defines the EDIPartyName of the GeneralName choice.
 * The ASN.1 syntax for this is:
 * <pre>
 * EDIPartyName ::= SEQUENCE {
 *     nameAssigner  [0]  DirectoryString OPTIONAL,
 *     partyName     [1]  DirectoryString }
 * </pre>
 *
 * @author Hemma Prafullchandra
 * @see GeneralName
 * @see GeneralNames
 * @see GeneralNameInterface
 */
public class EDIPartyName implements GeneralNameInterface {

    // Private data members
    private static final byte TAG_ASSIGNER = 0;
    private static final byte TAG_PARTYNAME = 1;

    private String assigner = null;
    private String party = null;

    private int myhash = -1;

    /**
     * Create the EDIPartyName object from the specified names.
     *
     * @param assignerName the name of the assigner
     * @param partyName the name of the EDI party.
     */
    public EDIPartyName(String assignerName, String partyName) {
        this.assigner = assignerName;
        this.party = partyName;
    }

    /**
     * Create the EDIPartyName object from the specified name.
     *
     * @param partyName the name of the EDI party.
     */
    public EDIPartyName(String partyName) {
        this.party = partyName;
    }

    /**
     * Create the EDIPartyName object from the passed encoded Der value.
     *
     * @param derValue the encoded DER EDIPartyName.
     * @exception IOException on error.
     */
    public EDIPartyName(DerValue derValue) throws IOException {
        DerInputStream in = new DerInputStream(derValue.toByteArray());
        DerValue[] seq = in.getSequence(2);

        int len = seq.length;
        if (len < 1 || len > 2)
            throw new IOException("Invalid encoding of EDIPartyName");

        for (int i = 0; i < len; i++) {
            DerValue opt = seq[i];
            if (opt.isContextSpecific(TAG_ASSIGNER) &&
                !opt.isConstructed()) {
                if (assigner != null)
                    throw new IOException("Duplicate nameAssigner found in"
                                          + " EDIPartyName");
                opt = opt.data.getDerValue();
                assigner = opt.getAsString();
            }
            if (opt.isContextSpecific(TAG_PARTYNAME) &&
                !opt.isConstructed()) {
                if (party != null)
                    throw new IOException("Duplicate partyName found in"
                                          + " EDIPartyName");
                opt = opt.data.getDerValue();
                party = opt.getAsString();
            }
        }
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return (GeneralNameInterface.NAME_EDI);
    }

    /**
     * Encode the EDI party name into the DerOutputStream.
     *
     * @param out the DER stream to encode the EDIPartyName to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tagged = new DerOutputStream();
        DerOutputStream tmp = new DerOutputStream();

        if (assigner != null) {
            DerOutputStream tmp2 = new DerOutputStream();
            // XXX - shd check is chars fit into PrintableString
            tmp2.putPrintableString(assigner);
            tagged.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                 false, TAG_ASSIGNER), tmp2);
        }
        if (party == null)
            throw  new IOException("Cannot have null partyName");

        // XXX - shd check is chars fit into PrintableString
        tmp.putPrintableString(party);
        tagged.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                 false, TAG_PARTYNAME), tmp);

        out.write(DerValue.tag_Sequence, tagged);
    }

    /**
     * Return the assignerName
     *
     * @return String assignerName
     */
    public String getAssignerName() {
        return assigner;
    }

    /**
     * Return the partyName
     *
     * @return String partyName
     */
    public String getPartyName() {
        return party;
    }

    /**
     * Compare this EDIPartyName with another.  Does a byte-string
     * comparison without regard to type of the partyName and
     * the assignerName.
     *
     * @return true if the two names match
     */
    public boolean equals(Object other) {
        if (!(other instanceof EDIPartyName))
            return false;
        String otherAssigner = ((EDIPartyName)other).assigner;
        if (this.assigner == null) {
            if (otherAssigner != null)
                return false;
        } else {
            if (!(this.assigner.equals(otherAssigner)))
                return false;
        }
        String otherParty = ((EDIPartyName)other).party;
        if (this.party == null) {
            if (otherParty != null)
                return false;
        } else {
            if (!(this.party.equals(otherParty)))
                return false;
        }
        return true;
    }

    /**
     * Returns the hash code value for this EDIPartyName.
     *
     * @return a hash code value.
     */
    public int hashCode() {
        if (myhash == -1) {
            myhash = 37 + (party == null ? 1 : party.hashCode());
            if (assigner != null) {
                myhash = 37 * myhash + assigner.hashCode();
            }
        }
        return myhash;
    }

    /**
     * Return the printable string.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("EDIPartyName: ");
        if (assigner != null) {
            sb.append("  nameAssigner = ")
              .append(assigner)
              .append(',');
        }
        sb.append("  partyName = ")
          .append(party);
        return sb.toString();
    }

    /**
     * Return constraint type:<ul>
     *   <li>NAME_DIFF_TYPE = -1: input name is different type from name (i.e. does not constrain)
     *   <li>NAME_MATCH = 0: input name matches name
     *   <li>NAME_NARROWS = 1: input name narrows name
     *   <li>NAME_WIDENS = 2: input name widens name
     *   <li>NAME_SAME_TYPE = 3: input name does not match or narrow name, but is same type
     * </ul>.  These results are used in checking NameConstraints during
     * certification path verification.
     *
     * @param inputName to be checked for being constrained
     * @return constraint type above
     * @throws UnsupportedOperationException if name is same type, but comparison operations are
     *          not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
        int constraintType;
        if (inputName == null)
            constraintType = NAME_DIFF_TYPE;
        else if (inputName.getType() != NAME_EDI)
            constraintType = NAME_DIFF_TYPE;
        else {
            throw new UnsupportedOperationException("Narrowing, widening, and matching of names not supported for EDIPartyName");
        }
        return constraintType;
    }

    /**
     * Return subtree depth of this name for purposes of determining
     * NameConstraints minimum and maximum bounds and for calculating
     * path lengths in name subtrees.
     *
     * @return distance of name from root
     * @throws UnsupportedOperationException if not supported for this name type
     */
    public int subtreeDepth() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("subtreeDepth() not supported for EDIPartyName");
    }

}
