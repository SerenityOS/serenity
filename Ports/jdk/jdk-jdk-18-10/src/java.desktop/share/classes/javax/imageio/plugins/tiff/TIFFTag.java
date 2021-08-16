/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package javax.imageio.plugins.tiff;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A class defining the notion of a TIFF tag.  A TIFF tag is a key
 * that may appear in an Image File Directory (IFD).  In the IFD
 * each tag has some data associated with it, which may consist of zero
 * or more values of a given data type. The combination of a tag and a
 * value is known as an IFD Entry or TIFF Field.
 *
 * <p> The actual tag values used in the root IFD of a standard ("baseline")
 * tiff stream are defined in the {@link BaselineTIFFTagSet
 * BaselineTIFFTagSet} class.
 *
 * @since 9
 * @see   BaselineTIFFTagSet
 * @see   TIFFField
 * @see   TIFFTagSet
 */
public class TIFFTag {

    // TIFF 6.0 + Adobe PageMaker(R) 6.0 TIFF Technical Notes 1 IFD data type

    /** Flag for 8 bit unsigned integers. */
    public static final int TIFF_BYTE        =  1;

    /** Flag for null-terminated ASCII strings. */
    public static final int TIFF_ASCII       =  2;

    /** Flag for 16 bit unsigned integers. */
    public static final int TIFF_SHORT       =  3;

    /** Flag for 32 bit unsigned integers. */
    public static final int TIFF_LONG        =  4;

    /** Flag for pairs of 32 bit unsigned integers. */
    public static final int TIFF_RATIONAL    =  5;

    /** Flag for 8 bit signed integers. */
    public static final int TIFF_SBYTE       =  6;

    /** Flag for 8 bit uninterpreted bytes. */
    public static final int TIFF_UNDEFINED   =  7;

    /** Flag for 16 bit signed integers. */
    public static final int TIFF_SSHORT      =  8;

    /** Flag for 32 bit signed integers. */
    public static final int TIFF_SLONG       =  9;

    /** Flag for pairs of 32 bit signed integers. */
    public static final int TIFF_SRATIONAL   = 10;

    /** Flag for 32 bit IEEE floats. */
    public static final int TIFF_FLOAT       = 11;

    /** Flag for 64 bit IEEE doubles. */
    public static final int TIFF_DOUBLE      = 12;

    /**
     * Flag for IFD pointer defined in TIFF Tech Note 1 in
     * TIFF Specification Supplement 1.
     */
    public static final int TIFF_IFD_POINTER = 13;

    /**
     * The numerically smallest constant representing a TIFF data type.
     */
    public static final int MIN_DATATYPE = TIFF_BYTE;

    /**
     * The numerically largest constant representing a TIFF data type.
     */
    public static final int MAX_DATATYPE = TIFF_IFD_POINTER;

    /**
     * The name assigned to a tag with an unknown tag number. Such
     * a tag may be created for example when reading an IFD and a
     * tag number is encountered which is not in any of the
     * {@code TIFFTagSet}s known to the reader.
     */
    public static final String UNKNOWN_TAG_NAME = "UnknownTag";

    /**
     * Disallowed data type mask.
     */
    private static final int DISALLOWED_DATATYPES_MASK = ~0x3fff;

    private static final int[] SIZE_OF_TYPE = {
        0, //  0 = n/a
        1, //  1 = byte
        1, //  2 = ascii
        2, //  3 = short
        4, //  4 = long
        8, //  5 = rational
        1, //  6 = sbyte
        1, //  7 = undefined
        2, //  8 = sshort
        4, //  9 = slong
        8, // 10 = srational
        4, // 11 = float
        8, // 12 = double
        4, // 13 = IFD_POINTER
    };

    private int number;
    private String name;
    private int dataTypes;
    private int count;
    private TIFFTagSet tagSet = null;

    // Mnemonic names for integral enumerated constants
    private SortedMap<Integer,String> valueNames = null;

    /**
     * Constructs a {@code TIFFTag} with a given name, tag number, set
     * of legal data types, and value count. A negative value count signifies
     * that either an arbitrary number of values is legal or the required count
     * is determined by the values of other fields in the IFD. A non-negative
     * count specifies the number of values which an associated field must
     * contain. The tag will have no associated {@code TIFFTagSet}.
     *
     * <p> If there are mnemonic names to be associated with the legal
     * data values for the tag, {@link #addValueName(int, String)
     * addValueName()} should be called on the new instance for each name.
     * Mnemonic names apply only to tags which have integral data type.</p>
     *
     * <p> See the documentation for {@link #getDataTypes()
     * getDataTypes()} for an explanation of how the set
     * of data types is to be converted into a bit mask.</p>
     *
     * @param name the name of the tag.
     * @param number the number used to represent the tag.
     * @param dataTypes a bit mask indicating the set of legal data
     * types for this tag.
     * @param count the value count for this tag.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if number is negative or dataTypes
     * is negative or specifies an out of range type.
     */
    public TIFFTag(String name, int number, int dataTypes, int count) {
        if (name == null) {
            throw new NullPointerException("name == null");
        } else if (number < 0) {
            throw new IllegalArgumentException("number (" + number + ") < 0");
        } else if (dataTypes < 0
            || (dataTypes & DISALLOWED_DATATYPES_MASK) != 0) {
            throw new IllegalArgumentException("dataTypes out of range");
        }

        this.name = name;
        this.number = number;
        this.dataTypes = dataTypes;
        this.count = count;
    }

    /**
     * Constructs a {@code TIFFTag} with a given name, tag number and
     * {@code TIFFTagSet} to which it refers. The legal data types are
     * set to include {@link #TIFF_LONG} and {@link #TIFF_IFD_POINTER} and the
     * value count is unity. The {@code TIFFTagSet} will
     * represent the set of {@code TIFFTag}s which appear in the IFD
     * pointed to. A {@code TIFFTag} represents an IFD pointer if and
     * only if {@code tagSet} is non-{@code null} or the data
     * type {@code TIFF_IFD_POINTER} is legal.
     *
     * @param name the name of the tag.
     * @param number the number used to represent the tag.
     * @param tagSet the {@code TIFFTagSet} to which this tag belongs.
     * @throws NullPointerException if name or tagSet is null.
     * @throws IllegalArgumentException if number is negative.
     *
     * @see #TIFFTag(String, int, int, int)
     */
    public TIFFTag(String name, int number, TIFFTagSet tagSet) {
        this(name, number,
            1 << TIFFTag.TIFF_LONG | 1 << TIFFTag.TIFF_IFD_POINTER, 1);
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        this.tagSet = tagSet;
    }

    /**
     * Constructs  a  {@code TIFFTag}  with  a  given  name,  tag number,
     * and set  of  legal  data  types.  The value count of the tag will be
     * undefined and it will  have  no associated {@code TIFFTagSet}.
     *
     * @param name the name of the tag.
     * @param number the number used to represent the tag.
     * @param dataTypes a bit mask indicating the set of legal data
     * types for this tag.
     * @throws NullPointerException if name is null.
     * @throws IllegalArgumentException if number is negative or dataTypes
     * is negative or specifies an out of range type.
     *
     * @see #TIFFTag(String, int, int, int)
     */
    public TIFFTag(String name, int number, int dataTypes) {
        this(name, number, dataTypes, -1);
    }

    /**
     * Returns the number of bytes used to store a value of the given
     * data type.
     *
     * @param dataType the data type to be queried.
     *
     * @return the number of bytes used to store the given data type.
     *
     * @throws IllegalArgumentException if {@code datatype} is
     * less than {@code MIN_DATATYPE} or greater than
     * {@code MAX_DATATYPE}.
     */
    public static int getSizeOfType(int dataType) {
        if (dataType < MIN_DATATYPE ||dataType > MAX_DATATYPE) {
            throw new IllegalArgumentException("dataType out of range!");
        }

        return SIZE_OF_TYPE[dataType];
    }

    /**
     * Returns the name of the tag, as it will appear in image metadata.
     *
     * @return the tag name, as a {@code String}.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the integer used to represent the tag.
     *
     * @return the tag number, as an {@code int}.
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns a bit mask indicating the set of data types that may
     * be used to store the data associated with the tag.
     * For example, a tag that can store both SHORT and LONG values
     * would return a value of:
     *
     * <pre>
     * (1 &lt;&lt; TIFFTag.TIFF_SHORT) | (1 &lt;&lt; TIFFTag.TIFF_LONG)
     * </pre>
     *
     * @return an {@code int} containing a bitmask encoding the
     * set of valid data types.
     */
    public int getDataTypes() {
        return dataTypes;
    }

    /**
     * Returns the value count of this tag. If this value is positive, it
     * represents the required number of values for a {@code TIFFField}
     * which has this tag. If the value is negative, the count is undefined.
     * In the latter case the count may be derived, e.g., the number of values
     * of the {@code BitsPerSample} field is {@code SamplesPerPixel},
     * or it may be variable as in the case of most {@code US-ASCII}
     * fields.
     *
     * @return the value count of this tag.
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns {@code true} if the given data type
     * may be used for the data associated with this tag.
     *
     * @param dataType the data type to be queried, one of
     * {@code TIFF_BYTE}, {@code TIFF_SHORT}, etc.
     *
     * @return a {@code boolean} indicating whether the given
     * data type may be used with this tag.
     *
     * @throws IllegalArgumentException if {@code datatype} is
     * less than {@code MIN_DATATYPE} or greater than
     * {@code MAX_DATATYPE}.
     */
    public boolean isDataTypeOK(int dataType) {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE) {
            throw new IllegalArgumentException("datatype not in range!");
        }
        return (dataTypes & (1 << dataType)) != 0;
    }

    /**
     * Returns the {@code TIFFTagSet} of which this tag is a part.
     *
     * @return the containing {@code TIFFTagSet}.
     */
    public TIFFTagSet getTagSet() {
        return tagSet;
    }

    /**
     * Returns {@code true} if this tag is used to point to an IFD
     * structure containing additional tags. A {@code TIFFTag} represents
     * an IFD pointer if and only if its {@code TIFFTagSet} is
     * non-{@code null} or the data type {@code TIFF_IFD_POINTER} is
     * legal. This condition will be satisfied if and only if either
     * {@code getTagSet() != null} or
     * {@code isDataTypeOK(TIFF_IFD_POINTER) == true}.
     *
     * <p>Many TIFF extensions use the IFD mechanism in order to limit the
     * number of new tags that may appear in the root IFD.</p>
     *
     * @return {@code true} if this tag points to an IFD.
     */
    public boolean isIFDPointer() {
        return tagSet != null || isDataTypeOK(TIFF_IFD_POINTER);
    }

    /**
     * Returns {@code true} if there are mnemonic names associated with
     * the set of legal values for the data associated with this tag.  Mnemonic
     * names apply only to tags which have integral data type.
     *
     * @return {@code true} if mnemonic value names are available.
     */
    public boolean hasValueNames() {
        return valueNames != null;
    }

    /**
     * Adds a mnemonic name for a particular value that this tag's data may take
     * on.  Mnemonic names apply only to tags which have integral data type.
     *
     * @param value the data value.
     * @param name the name to associate with the value.
     */
    protected void addValueName(int value, String name) {
        if (valueNames == null) {
            valueNames = new TreeMap<Integer,String>();
        }
        valueNames.put(Integer.valueOf(value), name);
    }

    /**
     * Returns the mnemonic name associated with a particular value
     * that this tag's data may take on, or {@code null} if
     * no name is present.  Mnemonic names apply only to tags which have
     * integral data type.
     *
     * @param value the data value.
     *
     * @return the mnemonic name associated with the value, as a
     * {@code String}.
     */
    public String getValueName(int value) {
        if (valueNames == null) {
            return null;
        }
        return valueNames.get(Integer.valueOf(value));
    }

    /**
     * Returns an array of values for which mnemonic names are defined.  The
     * method {@link #getValueName(int) getValueName()} will return
     * non-{@code null} only for values contained in the returned array.
     * Mnemonic names apply only to tags which have integral data type.
     *
     * @return the values for which there is a mnemonic name.
     */
    public int[] getNamedValues() {
        int[] intValues = null;
        if (valueNames != null) {
            Set<Integer> values = valueNames.keySet();
            Iterator<Integer> iter = values.iterator();
            intValues = new int[values.size()];
            int i = 0;
            while (iter.hasNext()) {
                intValues[i++] = iter.next();
            }
        }
        return intValues;
    }
}
