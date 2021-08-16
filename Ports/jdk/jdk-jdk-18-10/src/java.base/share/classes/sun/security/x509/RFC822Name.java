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
import java.util.Locale;

import sun.security.util.*;

/**
 * This class implements the RFC822Name as required by the GeneralNames
 * ASN.1 object.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 * @see GeneralName
 * @see GeneralNames
 * @see GeneralNameInterface
 */
public class RFC822Name implements GeneralNameInterface
{
    private String name;

    /**
     * Create the RFC822Name object from the passed encoded Der value.
     *
     * @param derValue the encoded DER RFC822Name.
     * @exception IOException on error.
     */
    public RFC822Name(DerValue derValue) throws IOException {
        name = derValue.getIA5String();
        parseName(name);
    }

    /**
     * Create the RFC822Name object with the specified name.
     *
     * @param name the RFC822Name.
     * @throws IOException on invalid input name
     */
    public RFC822Name(String name) throws IOException {
        parseName(name);
        this.name = name;
    }

    /**
     * Parse an RFC822Name string to see if it is a valid
     * addr-spec according to IETF RFC 822 and RFC 5280:
     * [local-part@]domain
     * <p>
     * local-part@ could be empty for an RFC822Name NameConstraint,
     * but the domain at least must be non-empty.  Case is not
     * significant.
     *
     * @param name the RFC822Name string
     * @throws IOException if name is not valid
     */
    public void parseName(String name) throws IOException {
        if (name == null || name.isEmpty()) {
            throw new IOException("RFC822Name may not be null or empty");
        }
        // See if domain is a valid domain name
        String domain = name.substring(name.indexOf('@')+1);
        if (domain.isEmpty()) {
            throw new IOException("RFC822Name may not end with @");
        } else {
            //An RFC822 NameConstraint could start with a ., although
            //a DNSName may not
            if (domain.startsWith(".")) {
                if (domain.length() == 1)
                    throw new IOException("RFC822Name domain may not be just .");
            }
        }
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return (GeneralNameInterface.NAME_RFC822);
    }

    /**
     * Return the actual name value of the GeneralName.
     */
    public String getName() {
        return name;
    }

    /**
     * Encode the RFC822 name into the DerOutputStream.
     *
     * @param out the DER stream to encode the RFC822Name to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        out.putIA5String(name);
    }

    /**
     * Convert the name into user readable string.
     */
    public String toString() {
        return ("RFC822Name: " + name);
    }

    /**
     * Compares this name with another, for equality.
     *
     * @return true iff the names are equivalent
     * according to RFC 5280.
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof RFC822Name))
            return false;

        RFC822Name other = (RFC822Name)obj;

        // RFC 5280 mandates that these names are
        // not case-sensitive
        return name.equalsIgnoreCase(other.name);
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return name.toUpperCase(Locale.ENGLISH).hashCode();
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
     * <p>
     *
     * [RFC 5280]:
     * When the subjectAltName extension contains an Internet mail address,
     * the address MUST be stored in the rfc822Name.  The format of an
     * rfc822Name is a "Mailbox" as defined in Section 4.1.2 of [RFC2821].
     * A Mailbox has the form "Local-part@Domain".  Note that a Mailbox has
     * no phrase (such as a common name) before it, has no comment (text
     * surrounded in parentheses) after it, and is not surrounded by "&lt;" and
     * "&gt;".
     *
     * @param inputName to be checked for being constrained
     * @return constraint type above
     * @throws UnsupportedOperationException if name is not exact match, but narrowing and widening are
     *          not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName) throws UnsupportedOperationException {
        int constraintType;
        if (inputName == null)
            constraintType = NAME_DIFF_TYPE;
        else if (inputName.getType() != (GeneralNameInterface.NAME_RFC822)) {
            constraintType = NAME_DIFF_TYPE;
        } else {
            //RFC 5280 specifies that case is not significant in RFC822Names
            String inName =
                (((RFC822Name)inputName).getName()).toLowerCase(Locale.ENGLISH);
            String thisName = name.toLowerCase(Locale.ENGLISH);
            if (inName.equals(thisName)) {
                constraintType = NAME_MATCH;
            } else if (thisName.endsWith(inName)) {
                /* if both names contain @, then they had to match exactly */
                if (inName.indexOf('@') != -1) {
                    constraintType = NAME_SAME_TYPE;
                } else if (inName.startsWith(".")) {
                    constraintType = NAME_WIDENS;
                } else {
                    int inNdx = thisName.lastIndexOf(inName);
                    if (thisName.charAt(inNdx-1) == '@' ) {
                        constraintType = NAME_WIDENS;
                    } else {
                        constraintType = NAME_SAME_TYPE;
                    }
                }
            } else if (inName.endsWith(thisName)) {
                /* if thisName contains @, then they had to match exactly */
                if (thisName.indexOf('@') != -1) {
                    constraintType = NAME_SAME_TYPE;
                } else if (thisName.startsWith(".")) {
                    constraintType = NAME_NARROWS;
                } else {
                    int ndx = inName.lastIndexOf(thisName);
                    if (inName.charAt(ndx-1) == '@') {
                        constraintType = NAME_NARROWS;
                    } else {
                        constraintType = NAME_SAME_TYPE;
                    }
                }
            } else {
                constraintType = NAME_SAME_TYPE;
            }
        }
        return constraintType;
    }

    /**
     * Return subtree depth of this name for purposes of determining
     * NameConstraints minimum and maximum bounds.
     *
     * @return distance of name from root
     * @throws UnsupportedOperationException if not supported for this name type
     */
    public int subtreeDepth() throws UnsupportedOperationException {
        String subtree=name;
        int i=1;

        /* strip off name@ portion */
        int atNdx = subtree.lastIndexOf('@');
        if (atNdx >= 0) {
            i++;
            subtree=subtree.substring(atNdx+1);
        }

        /* count dots in DNSName, adding one if DNSName preceded by @ */
        for (; subtree.lastIndexOf('.') >= 0; i++) {
            subtree=subtree.substring(0,subtree.lastIndexOf('.'));
        }

        return i;
    }
}
