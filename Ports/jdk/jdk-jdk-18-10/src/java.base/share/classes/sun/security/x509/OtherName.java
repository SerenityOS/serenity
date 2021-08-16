/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Constructor;
import java.util.Arrays;

import sun.security.util.*;

/**
 * This class represents the OtherName as required by the GeneralNames
 * ASN.1 object. It supplies the generic framework to allow specific
 * Other Name types, and also provides minimal support for unrecognized
 * Other Name types.
 *
 * The ASN.1 definition for OtherName is:
 * <pre>
 * OtherName ::= SEQUENCE {
 *     type-id    OBJECT IDENTIFIER,
 *     value      [0] EXPLICIT ANY DEFINED BY type-id
 * }
 * </pre>
 * @author Hemma Prafullchandra
 */
public class OtherName implements GeneralNameInterface {

    private String name;
    private ObjectIdentifier oid;
    private byte[] nameValue = null;
    private GeneralNameInterface gni = null;

    private static final byte TAG_VALUE = 0;

    private int myhash = -1;

    /**
     * Create the OtherName object from a passed ObjectIdentfier and
     * byte array name value
     *
     * @param oid ObjectIdentifier of this OtherName object
     * @param value the DER-encoded value of the OtherName
     * @throws IOException on error
     */
    public OtherName(ObjectIdentifier oid, byte[] value) throws IOException {
        if (oid == null || value == null) {
            throw new NullPointerException("parameters may not be null");
        }
        this.oid = oid;
        this.nameValue = value;
        gni = getGNI(oid, value);
        if (gni != null) {
            name = gni.toString();
        } else {
            name = "Unrecognized ObjectIdentifier: " + oid.toString();
        }
    }

    /**
     * Create the OtherName object from the passed encoded Der value.
     *
     * @param derValue the encoded DER OtherName.
     * @exception IOException on error.
     */
    public OtherName(DerValue derValue) throws IOException {
        DerInputStream in = derValue.toDerInputStream();

        oid = in.getOID();
        DerValue val = in.getDerValue();
        nameValue = val.toByteArray();
        gni = getGNI(oid, nameValue);
        if (gni != null) {
            name = gni.toString();
        } else {
            name = "Unrecognized ObjectIdentifier: " + oid.toString();
        }
    }

    /**
     * Get ObjectIdentifier
     */
    public ObjectIdentifier getOID() {
        //XXXX May want to consider cloning this
        return oid;
    }

    /**
     * Get name value
     */
    public byte[] getNameValue() {
        return nameValue.clone();
    }

    /**
     * Get GeneralNameInterface
     */
    private GeneralNameInterface getGNI(ObjectIdentifier oid, byte[] nameValue)
            throws IOException {
        try {
            Class<?> extClass = OIDMap.getClass(oid);
            if (extClass == null) {   // Unsupported OtherName
                return null;
            }
            Class<?>[] params = { Object.class };
            Constructor<?> cons = extClass.getConstructor(params);

            Object[] passed = new Object[] { nameValue };
            GeneralNameInterface gni =
                       (GeneralNameInterface)cons.newInstance(passed);
            return gni;
        } catch (Exception e) {
            throw new IOException("Instantiation error: " + e, e);
        }
    }

    /**
     * Return the type of the GeneralName.
     */
    public int getType() {
        return GeneralNameInterface.NAME_ANY;
    }

    /**
     * Encode the Other name into the DerOutputStream.
     *
     * @param out the DER stream to encode the Other-Name to.
     * @exception IOException on encoding errors.
     */
    public void encode(DerOutputStream out) throws IOException {
        if (gni != null) {
            // This OtherName has a supported class
            gni.encode(out);
            return;
        } else {
            // This OtherName has no supporting class
            DerOutputStream tmp = new DerOutputStream();
            tmp.putOID(oid);
            tmp.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, TAG_VALUE), nameValue);
            out.write(DerValue.tag_Sequence, tmp);
        }
    }

    /**
     * Compares this name with another, for equality.
     *
     * @return true iff the names are identical.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OtherName)) {
            return false;
        }
        OtherName otherOther = (OtherName)other;
        if (!(otherOther.oid.equals(oid))) {
            return false;
        }
        GeneralNameInterface otherGNI = null;
        try {
            otherGNI = getGNI(otherOther.oid, otherOther.nameValue);
        } catch (IOException ioe) {
            return false;
        }

        boolean result;
        if (otherGNI != null) {
            try {
                result = (otherGNI.constrains(this) == NAME_MATCH);
            } catch (UnsupportedOperationException ioe) {
                result = false;
            }
        } else {
            result = Arrays.equals(nameValue, otherOther.nameValue);
        }

        return result;
    }

    /**
     * Returns the hash code for this OtherName.
     *
     * @return a hash code value.
     */
    public int hashCode() {
        if (myhash == -1) {
            myhash = 37 + oid.hashCode();
            for (int i = 0; i < nameValue.length; i++) {
                myhash = 37 * myhash + nameValue[i];
            }
        }
        return myhash;
    }

    /**
     * Convert the name into user readable string.
     */
    public String toString() {
        return "Other-Name: " + name;
    }

    /**
     * Return type of constraint inputName places on this name:<ul>
     *   <li>NAME_DIFF_TYPE = -1: input name is different type from name
     *       (i.e. does not constrain).
     *   <li>NAME_MATCH = 0: input name matches name.
     *   <li>NAME_NARROWS = 1: input name narrows name (is lower in the
     *       naming subtree)
     *   <li>NAME_WIDENS = 2: input name widens name (is higher in the
     *       naming subtree)
     *   <li>NAME_SAME_TYPE = 3: input name does not match or narrow name,
     *       but is same type.
     * </ul>.  These results are used in checking NameConstraints during
     * certification path verification.
     *
     * @param inputName to be checked for being constrained
     * @return constraint type above
     * @throws UnsupportedOperationException if name is same type, but
     *         comparison operations are not supported for this name type.
     */
    public int constrains(GeneralNameInterface inputName) {
        int constraintType;
        if (inputName == null) {
            constraintType = NAME_DIFF_TYPE;
        } else if (inputName.getType() != NAME_ANY) {
            constraintType = NAME_DIFF_TYPE;
        } else {
            throw new UnsupportedOperationException("Narrowing, widening, "
                + "and matching are not supported for OtherName.");
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
    public int subtreeDepth() {
        throw new UnsupportedOperationException
            ("subtreeDepth() not supported for generic OtherName");
    }

}
