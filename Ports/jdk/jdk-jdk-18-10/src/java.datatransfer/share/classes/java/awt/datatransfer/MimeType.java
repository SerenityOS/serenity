/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.util.Locale;

/**
 * A Multipurpose Internet Mail Extension (MIME) type, as defined in RFC 2045
 * and 2046.
 * <p>
 * THIS IS *NOT* - REPEAT *NOT* - A PUBLIC CLASS! DataFlavor IS THE PUBLIC
 * INTERFACE, AND THIS IS PROVIDED AS A ***PRIVATE*** (THAT IS AS IN *NOT*
 * PUBLIC) HELPER CLASS!
 */
class MimeType implements Externalizable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.2 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -6568722458793895906L;

    /**
     * Constructor for externalization; this constructor should not be called
     * directly by an application, since the result will be an uninitialized,
     * immutable {@code MimeType} object.
     */
    public MimeType() {
    }

    /**
     * Builds a {@code MimeType} from a {@code String}.
     *
     * @param  rawdata text used to initialize the {@code MimeType}
     * @throws NullPointerException if {@code rawdata} is {@code null}
     */
    public MimeType(String rawdata) throws MimeTypeParseException {
        parse(rawdata);
    }

    /**
     * Builds a {@code MimeType} with the given primary and sub type but has an
     * empty parameter list.
     *
     * @param  primary the primary type of this {@code MimeType}
     * @param  sub the subtype of this {@code MimeType}
     * @throws NullPointerException if either {@code primary} or {@code sub} is
     *         {@code null}
     */
    public MimeType(String primary, String sub) throws MimeTypeParseException {
        this(primary, sub, new MimeTypeParameterList());
    }

    /**
     * Builds a {@code MimeType} with a pre-defined and valid (or empty)
     * parameter list.
     *
     * @param  primary the primary type of this {@code MimeType}
     * @param  sub the subtype of this {@code MimeType}
     * @param  mtpl the requested parameter list
     * @throws NullPointerException if either {@code primary}, {@code sub} or
     *         {@code mtpl} is {@code null}
     */
    public MimeType(String primary, String sub, MimeTypeParameterList mtpl) throws
MimeTypeParseException {
        //    check to see if primary is valid
        if(isValidToken(primary)) {
            primaryType = primary.toLowerCase(Locale.ENGLISH);
        } else {
            throw new MimeTypeParseException("Primary type is invalid.");
        }

        //    check to see if sub is valid
        if(isValidToken(sub)) {
            subType = sub.toLowerCase(Locale.ENGLISH);
        } else {
            throw new MimeTypeParseException("Sub type is invalid.");
        }

        parameters = (MimeTypeParameterList)mtpl.clone();
    }

    public int hashCode() {

        // We sum up the hash codes for all of the strings. This
        // way, the order of the strings is irrelevant
        int code = 0;
        code += primaryType.hashCode();
        code += subType.hashCode();
        code += parameters.hashCode();
        return code;
    } // hashCode()

    /**
     * {@code MimeType}s are equal if their primary types, subtypes, and
     * parameters are all equal. No default values are taken into account.
     *
     * @param  thatObject the object to be evaluated as a {@code MimeType}
     * @return {@code true} if {@code thatObject} is a {@code MimeType};
     *         otherwise returns {@code false}
     */
    public boolean equals(Object thatObject) {
        if (!(thatObject instanceof MimeType)) {
            return false;
        }
        MimeType that = (MimeType)thatObject;
        boolean isIt =
            ((this.primaryType.equals(that.primaryType)) &&
             (this.subType.equals(that.subType)) &&
             (this.parameters.equals(that.parameters)));
        return isIt;
    } // equals()

    /**
     * A routine for parsing the MIME type out of a String.
     *
     * @throws NullPointerException if {@code rawdata} is {@code null}
     */
    private void parse(String rawdata) throws MimeTypeParseException {
        int slashIndex = rawdata.indexOf('/');
        int semIndex = rawdata.indexOf(';');
        if((slashIndex < 0) && (semIndex < 0)) {
            //    neither character is present, so treat it
            //    as an error
            throw new MimeTypeParseException("Unable to find a sub type.");
        } else if((slashIndex < 0) && (semIndex >= 0)) {
            //    we have a ';' (and therefore a parameter list),
            //    but no '/' indicating a sub type is present
            throw new MimeTypeParseException("Unable to find a sub type.");
        } else if((slashIndex >= 0) && (semIndex < 0)) {
            //    we have a primary and sub type but no parameter list
            primaryType = rawdata.substring(0,slashIndex).
                trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1).
                trim().toLowerCase(Locale.ENGLISH);
            parameters = new MimeTypeParameterList();
        } else if (slashIndex < semIndex) {
            //    we have all three items in the proper sequence
            primaryType = rawdata.substring(0, slashIndex).
                trim().toLowerCase(Locale.ENGLISH);
            subType = rawdata.substring(slashIndex + 1,
                semIndex).trim().toLowerCase(Locale.ENGLISH);
            parameters = new
MimeTypeParameterList(rawdata.substring(semIndex));
        } else {
            //    we have a ';' lexically before a '/' which means we have a primary type
            //    & a parameter list but no sub type
            throw new MimeTypeParseException("Unable to find a sub type.");
        }

        //    now validate the primary and sub types

        //    check to see if primary is valid
        if(!isValidToken(primaryType)) {
            throw new MimeTypeParseException("Primary type is invalid.");
        }

        //    check to see if sub is valid
        if(!isValidToken(subType)) {
            throw new MimeTypeParseException("Sub type is invalid.");
        }
    }

    /**
     * Retrieve the primary type of this object.
     */
    public String getPrimaryType() {
        return primaryType;
    }

    /**
     * Retrieve the sub type of this object.
     */
    public String getSubType() {
        return subType;
    }

    /**
     * Retrieve a copy of this object's parameter list.
     */
    public MimeTypeParameterList getParameters() {
        return (MimeTypeParameterList)parameters.clone();
    }

    /**
     * Retrieve the value associated with the given name, or {@code null} if
     * there is no current association.
     */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Set the value to be associated with the given name, replacing
     * any previous association.
     *
     * @throws IllegalArgumentException if parameter or value is illegal
     */
    public void setParameter(String name, String value) {
        parameters.set(name, value);
    }

    /**
     * Remove any value associated with the given name.
     *
     * @throws IllegalArgumentException if parameter may not be deleted
     */
    public void removeParameter(String name) {
        parameters.remove(name);
    }

    /**
     * Return the String representation of this object.
     */
    public String toString() {
        return getBaseType() + parameters.toString();
    }

    /**
     * Return a String representation of this object without the parameter list.
     */
    public String getBaseType() {
        return primaryType + "/" + subType;
    }

    /**
     * Returns {@code true} if the primary type and the subtype of this object
     * are the same as the specified {@code type}; otherwise returns
     * {@code false}.
     *
     * @param  type the type to compare to {@code this}'s type
     * @return {@code true} if the primary type and the subtype of this object
     *         are the same as the specified {@code type}; otherwise returns
     *         {@code false}
     */
    public boolean match(MimeType type) {
        if (type == null)
            return false;
        return primaryType.equals(type.getPrimaryType())
                    && (subType.equals("*")
                            || type.getSubType().equals("*")
                            || (subType.equals(type.getSubType())));
    }

    /**
     * Returns {@code true} if the primary type and the subtype of this object
     * are the same as the content type described in {@code rawdata}; otherwise
     * returns {@code false}.
     *
     * @param  rawdata the raw data to be examined
     * @return {@code true} if the primary type and the subtype of this object
     *         are the same as the content type described in {@code rawdata};
     *         otherwise returns {@code false}; if {@code rawdata} is
     *         {@code null}, returns {@code false}
     */
    public boolean match(String rawdata) throws MimeTypeParseException {
        if (rawdata == null)
            return false;
        return match(new MimeType(rawdata));
    }

    /**
     * The object implements the writeExternal method to save its contents by
     * calling the methods of DataOutput for its primitive values or calling the
     * writeObject method of ObjectOutput for objects, strings and arrays.
     *
     * @throws IOException Includes any I/O exceptions that may occur
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        String s = toString(); // contains ASCII chars only
        // one-to-one correspondence between ASCII char and byte in UTF string
        if (s.length() <= 65535) { // 65535 is max length of UTF string
            out.writeUTF(s);
        } else {
            out.writeByte(0);
            out.writeByte(0);
            out.writeInt(s.length());
            out.write(s.getBytes());
        }
    }

    /**
     * The object implements the readExternal method to restore its contents by
     * calling the methods of DataInput for primitive types and readObject for
     * objects, strings and arrays. The readExternal method must read the values
     * in the same sequence and with the same types as were written by
     * writeExternal.
     *
     * @throws ClassNotFoundException If the class for an object being restored
     *         cannot be found
     */
    public void readExternal(ObjectInput in) throws IOException,
ClassNotFoundException {
        String s = in.readUTF();
        if (s == null || s.length() == 0) { // long mime type
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = in.readInt();
            while (len-- > 0) {
                baos.write(in.readByte());
            }
            s = baos.toString();
        }
        try {
            parse(s);
        } catch(MimeTypeParseException e) {
            throw new IOException(e.toString());
        }
    }

    /**
     * Returns a clone of this object.
     *
     * @return a clone of this object
     */
    public Object clone() {
        MimeType newObj = null;
        try {
            newObj = (MimeType)super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
        }
        newObj.parameters = (MimeTypeParameterList)parameters.clone();
        return newObj;
    }

    private transient String    primaryType;
    private transient String    subType;
    private transient MimeTypeParameterList parameters;

    //    below here be scary parsing related things

    /**
     * Determines whether or not a given character belongs to a legal token.
     */
    private static boolean isTokenChar(char c) {
        return ((c > 040) && (c < 0177)) && (TSPECIALS.indexOf(c) < 0);
    }

    /**
     * Determines whether or not a given string is a legal token.
     *
     * @throws NullPointerException if {@code s} is {@code null}
     */
    private boolean isValidToken(String s) {
        int len = s.length();
        if(len > 0) {
            for (int i = 0; i < len; ++i) {
                char c = s.charAt(i);
                if (!isTokenChar(c)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * A string that holds all the special chars.
     */
    private static final String TSPECIALS = "()<>@,;:\\\"/[]?=";
} // class MimeType
