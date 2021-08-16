/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.StringTokenizer;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.sun.imageio.plugins.tiff.TIFFFieldNode;
import com.sun.imageio.plugins.tiff.TIFFIFD;

/**
 * A class representing a field in a TIFF 6.0 Image File Directory.
 *
 * <p> A field in a TIFF Image File Directory (IFD) is defined as a
 * tag number accompanied by a sequence of values of identical data type.
 * TIFF 6.0 defines 12 data types; a 13th type {@code IFD} is
 * defined in TIFF Tech Note 1 of TIFF Specification Supplement 1. These
 * TIFF data types are referred to by Java constants and mapped internally
 * onto Java language data types and type names as follows:
 *
 * <table class="striped">
 * <caption>TIFF Data Type to Java Data Type Mapping</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">TIFF Data Type
 *     <th scope="col">Java Constant
 *     <th scope="col">Java Data Type
 *     <th scope="col">Java Type Name
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@code BYTE}
 *     <td>{@link TIFFTag#TIFF_BYTE}
 *     <td>{@code byte}
 *     <td>{@code "Byte"}
 *   <tr>
 *     <th scope="row">{@code ASCII}
 *     <td>{@link TIFFTag#TIFF_ASCII}
 *     <td>{@code String}
 *     <td>{@code "Ascii"}
 *   <tr>
 *     <th scope="row">{@code SHORT}
 *     <td>{@link TIFFTag#TIFF_SHORT}
 *     <td>{@code char}
 *     <td>{@code "Short"}
 *   <tr>
 *     <th scope="row">{@code LONG}
 *     <td>{@link TIFFTag#TIFF_LONG}
 *     <td>{@code long}
 *     <td>{@code "Long"}
 *   <tr>
 *     <th scope="row">{@code RATIONAL}
 *     <td>{@link TIFFTag#TIFF_RATIONAL}
 *     <td>{@code long[2]} {numerator, denominator}
 *     <td>{@code "Rational"}
 *   <tr>
 *     <th scope="row">{@code SBYTE}
 *     <td>{@link TIFFTag#TIFF_SBYTE}
 *     <td>{@code byte}
 *     <td>{@code "SByte"}
 *   <tr>
 *     <th scope="row">{@code UNDEFINED}
 *     <td>{@link TIFFTag#TIFF_UNDEFINED}
 *     <td>{@code byte}
 *     <td>{@code "Undefined"}
 *   <tr>
 *     <th scope="row">{@code SSHORT}
 *     <td>{@link TIFFTag#TIFF_SSHORT}
 *     <td>{@code short}
 *     <td>{@code "SShort"}
 *   <tr>
 *     <th scope="row">{@code SLONG}
 *     <td>{@link TIFFTag#TIFF_SLONG}
 *     <td>{@code int}
 *     <td>{@code "SLong"}
 *   <tr>
 *     <th scope="row">{@code SRATIONAL}
 *     <td>{@link TIFFTag#TIFF_SRATIONAL}
 *     <td>{@code int[2]} {numerator, denominator}
 *     <td>{@code "SRational"}
 *   <tr>
 *     <th scope="row">{@code FLOAT}
 *     <td>{@link TIFFTag#TIFF_FLOAT}
 *     <td>{@code float}
 *     <td>{@code "Float"}
 *   <tr>
 *     <th scope="row">{@code DOUBLE}
 *     <td>{@link TIFFTag#TIFF_DOUBLE}
 *     <td>{@code double}
 *     <td>{@code "Double"}
 *   <tr>
 *     <th scope="row">{@code IFD}
 *     <td>{@link TIFFTag#TIFF_IFD_POINTER}
 *     <td>{@code long}
 *     <td>{@code "IFDPointer"}
 *   </tr>
 * </tbody>
 * </table>
 *
 * @since 9
 * @see   TIFFDirectory
 * @see   TIFFTag
 */
public final class TIFFField implements Cloneable {

    private static final long MAX_UINT32 = 0xffffffffL;

    private static final String[] TYPE_NAMES = {
        null,
        "Byte", "Ascii", "Short", "Long", "Rational",
        "SByte", "Undefined", "SShort", "SLong", "SRational",
        "Float", "Double", "IFDPointer"
    };

    private static final boolean[] IS_INTEGRAL = {
        false,
        true, false, true, true, false,
        true, true, true, true, false,
        false, false, false
    };

    /** The tag. */
    private TIFFTag tag;

    /** The tag number. */
    private int tagNumber;

    /** The tag type. */
    private int type;

    /** The number of data items present in the field. */
    private int count;

    /** The field data. */
    private Object data;

    /** The IFD contents if available. This will usually be a TIFFIFD. */
    private TIFFDirectory dir;

    /** The default constructor. */
    private TIFFField() {}

    private static String getAttribute(Node node, String attrName) {
        NamedNodeMap attrs = node.getAttributes();
        return attrs.getNamedItem(attrName).getNodeValue();
    }

    private static void initData(Node node,
                                 int[] otype, int[] ocount, Object[] odata) {
        int type;
        int count;
        Object data = null;

        String typeName = node.getNodeName();
        typeName = typeName.substring(4);
        typeName = typeName.substring(0, typeName.length() - 1);
        type = TIFFField.getTypeByName(typeName);
        if (type == -1) {
            throw new IllegalArgumentException("typeName = " + typeName);
        }

        Node child = node.getFirstChild();

        count = 0;
        while (child != null) {
            String childTypeName = child.getNodeName().substring(4);
            if (!typeName.equals(childTypeName)) {
                // warning
            }

            ++count;
            child = child.getNextSibling();
        }

        if (count > 0) {
            data = createArrayForType(type, count);
            child = node.getFirstChild();
            int idx = 0;
            while (child != null) {
                String value = getAttribute(child, "value");

                String numerator, denominator;
                int slashPos;

                switch (type) {
                case TIFFTag.TIFF_ASCII:
                    ((String[])data)[idx] = value;
                    break;
                case TIFFTag.TIFF_BYTE:
                case TIFFTag.TIFF_SBYTE:
                    ((byte[])data)[idx] =
                        (byte)Integer.parseInt(value);
                    break;
                case TIFFTag.TIFF_SHORT:
                    ((char[])data)[idx] =
                        (char)Integer.parseInt(value);
                    break;
                case TIFFTag.TIFF_SSHORT:
                    ((short[])data)[idx] =
                        (short)Integer.parseInt(value);
                    break;
                case TIFFTag.TIFF_SLONG:
                    ((int[])data)[idx] =
                        Integer.parseInt(value);
                    break;
                case TIFFTag.TIFF_LONG:
                case TIFFTag.TIFF_IFD_POINTER:
                    ((long[])data)[idx] =
                        Long.parseLong(value);
                    break;
                case TIFFTag.TIFF_FLOAT:
                    ((float[])data)[idx] =
                        Float.parseFloat(value);
                    break;
                case TIFFTag.TIFF_DOUBLE:
                    ((double[])data)[idx] =
                        Double.parseDouble(value);
                    break;
                case TIFFTag.TIFF_SRATIONAL:
                    slashPos = value.indexOf("/");
                    numerator = value.substring(0, slashPos);
                    denominator = value.substring(slashPos + 1);

                    ((int[][])data)[idx] = new int[2];
                    ((int[][])data)[idx][0] =
                        Integer.parseInt(numerator);
                    ((int[][])data)[idx][1] =
                        Integer.parseInt(denominator);
                    break;
                case TIFFTag.TIFF_RATIONAL:
                    slashPos = value.indexOf("/");
                    numerator = value.substring(0, slashPos);
                    denominator = value.substring(slashPos + 1);

                    ((long[][])data)[idx] = new long[2];
                    ((long[][])data)[idx][0] =
                        Long.parseLong(numerator);
                    ((long[][])data)[idx][1] =
                        Long.parseLong(denominator);
                    break;
                default:
                    // error
                }

                idx++;
                child = child.getNextSibling();
            }
        }

        otype[0] = type;
        ocount[0] = count;
        odata[0] = data;
    }

    /**
     * Creates a {@code TIFFField} from a TIFF native image
     * metadata node. If the value of the {@code "number"} attribute
     * of the node is not found in {@code tagSet} then a new
     * {@code TIFFTag} with name {@code TIFFTag.UNKNOWN_TAG_NAME}
     * will be created and assigned to the field.
     *
     * @param tagSet The {@code TIFFTagSet} to which the
     * {@code TIFFTag} of the field belongs.
     * @param node A native TIFF image metadata {@code TIFFField} node.
     * @throws IllegalArgumentException If the {@code Node} parameter content
     * does not adhere to the {@code TIFFField} element structure defined by
     * the <a href="../../metadata/doc-files/tiff_metadata.html#ImageMetadata">
     * TIFF native image metadata format specification</a>, or if the
     * combination of node attributes and data is not legal per the
     * {@link #TIFFField(TIFFTag,int,int,Object)} constructor specification.
     * Note that a cause might be set on such an exception.
     * @return A new {@code TIFFField}.
     */
    public static TIFFField createFromMetadataNode(TIFFTagSet tagSet,
                                                   Node node) {
        if (node == null) {
            // This method is specified to throw only IllegalArgumentExceptions
            // so we create an IAE with a NullPointerException as its cause.
            throw new IllegalArgumentException(new NullPointerException
                ("node == null!"));
        }
        String name = node.getNodeName();
        if (!name.equals("TIFFField")) {
            throw new IllegalArgumentException("!name.equals(\"TIFFField\")");
        }

        int tagNumber = Integer.parseInt(getAttribute(node, "number"));
        TIFFTag tag = null;
        if (tagSet != null) {
            tag = tagSet.getTag(tagNumber);
        }

        int type = TIFFTag.TIFF_UNDEFINED;
        int count = 0;
        Object data = null;

        Node child = node.getFirstChild();
        if (child != null) {
            String typeName = child.getNodeName();
            if (typeName.equals("TIFFUndefined")) {
                String values = getAttribute(child, "value");
                StringTokenizer st = new StringTokenizer(values, ",");
                count = st.countTokens();

                byte[] bdata = new byte[count];
                for (int i = 0; i < count; i++) {
                    bdata[i] = (byte)Integer.parseInt(st.nextToken());
                }

                type = TIFFTag.TIFF_UNDEFINED;
                data = bdata;
            } else {
                int[] otype = new int[1];
                int[] ocount = new int[1];
                Object[] odata = new Object[1];

                initData(node.getFirstChild(), otype, ocount, odata);
                type = otype[0];
                count = ocount[0];
                data = odata[0];
            }
        } else if (tag != null) {
            int t = TIFFTag.MAX_DATATYPE;
            while(t >= TIFFTag.MIN_DATATYPE && !tag.isDataTypeOK(t)) {
                t--;
            }
            type = t;
        }

        if (tag == null) {
            tag = new TIFFTag(TIFFTag.UNKNOWN_TAG_NAME, tagNumber, 1 << type);
        }

        TIFFField field;
        try {
            field = new TIFFField(tag, type, count, data);
        } catch (NullPointerException npe) {
            // This method is specified to throw only IllegalArgumentExceptions
            // so we catch the NullPointerException and set it as the cause of
            // the IAE which is thrown.
            throw new IllegalArgumentException(npe);
        }

        return field;
    }

    /**
     * Constructs a {@code TIFFField} with arbitrary data. The
     * {@code type} parameter must be a value for which
     * {@link TIFFTag#isDataTypeOK tag.isDataTypeOK()}
     * returns {@code true}. The {@code data} parameter must
     * be an array of a Java type appropriate for the type of the TIFF
     * field.
     *
     * <p>Note that the value (data) of the {@code TIFFField}
     * will always be the actual field value regardless of the number of
     * bytes required for that value. This is the case despite the fact
     * that the TIFF <i>IFD Entry</i> corresponding to the field may
     * actually contain the offset to the value of the field rather than
     * the value itself (the latter occurring if and only if the
     * value fits into 4 bytes). In other words, the value of the
     * field will already have been read from the TIFF stream. (An exception
     * to this case may occur when the field represents the contents of a
     * non-baseline IFD. In that case the data will be a {@code long[]}
     * containing the offset to the IFD and the {@code TIFFDirectory}
     * returned by {@link #getDirectory()} will be its contents.)
     *
     * @param tag The tag to associated with this field.
     * @param type One of the {@code TIFFTag.TIFF_*} constants
     * indicating the data type of the field as written to the TIFF stream.
     * @param count The number of data values.
     * @param data The actual data content of the field.
     *
     * @throws NullPointerException if {@code tag == null}.
     * @throws IllegalArgumentException if {@code type} is not
     * one of the {@code TIFFTag.TIFF_*} data type constants.
     * @throws IllegalArgumentException if {@code type} is an unacceptable
     * data type for the supplied {@code TIFFTag}.
     * @throws IllegalArgumentException if {@code count < 0}.
     * @throws IllegalArgumentException if {@code count < 1}
     * and {@code type} is {@code TIFF_RATIONAL} or
     * {@code TIFF_SRATIONAL}.
     * @throws IllegalArgumentException if {@code count != 1}
     * and {@code type} is {@code TIFF_IFD_POINTER}.
     * @throws NullPointerException if {@code data == null}.
     * @throws IllegalArgumentException if {@code data} is an instance of
     * a class incompatible with the specified type.
     * @throws IllegalArgumentException if the size of the data array is wrong.
     * @throws IllegalArgumentException if the type of the data array is
     * {@code TIFF_LONG}, {@code TIFF_RATIONAL}, or {@code TIFF_IFD_POINTER}
     * and any of the elements is negative or greater than {@code 0xffffffff}.
     */
    public TIFFField(TIFFTag tag, int type, int count, Object data) {
        if(tag == null) {
            throw new NullPointerException("tag == null!");
        } else if(type < TIFFTag.MIN_DATATYPE || type > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type "+type);
        } else if(!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type
                + " for " + tag.getName() + " tag");
        } else if(count < 0) {
            throw new IllegalArgumentException("count < 0!");
        } else if((type == TIFFTag.TIFF_RATIONAL
                   || type == TIFFTag.TIFF_SRATIONAL)
                  && count < 1) {
            throw new IllegalArgumentException
                ("Type is TIFF_RATIONAL or TIFF_SRATIONAL and count < 1");
        } else if (type == TIFFTag.TIFF_IFD_POINTER && count != 1) {
            throw new IllegalArgumentException
                ("Type is TIFF_IFD_POINTER and count != 1");
        } else if(data == null) {
            throw new NullPointerException("data == null!");
        }

        boolean isDataArrayCorrect = false;

        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_SBYTE:
        case TIFFTag.TIFF_UNDEFINED:
            isDataArrayCorrect = data instanceof byte[]
                && ((byte[])data).length == count;
            break;
        case TIFFTag.TIFF_ASCII:
            isDataArrayCorrect = data instanceof String[]
                && ((String[])data).length == count;
            break;
        case TIFFTag.TIFF_SHORT:
            isDataArrayCorrect = data instanceof char[]
                && ((char[])data).length == count;
            break;
        case TIFFTag.TIFF_LONG:
            isDataArrayCorrect = data instanceof long[]
                && ((long[])data).length == count;
            if (isDataArrayCorrect) {
                for (long datum : (long[])data) {
                    if (datum < 0) {
                        throw new IllegalArgumentException
                            ("Negative value supplied for TIFF_LONG");
                    }
                    if (datum > MAX_UINT32) {
                        throw new IllegalArgumentException
                            ("Too large value supplied for TIFF_LONG");
                    }
                }
            }
            break;
        case TIFFTag.TIFF_IFD_POINTER:
            isDataArrayCorrect = data instanceof long[]
                && ((long[])data).length == 1;
            if (((long[])data)[0] < 0) {
                throw new IllegalArgumentException
                    ("Negative value supplied for TIFF_IFD_POINTER");
            }
            if (((long[])data)[0] > MAX_UINT32) {
                throw new IllegalArgumentException
                    ("Too large value supplied for TIFF_IFD_POINTER");
            }
            break;
        case TIFFTag.TIFF_RATIONAL:
            isDataArrayCorrect = data instanceof long[][]
                && ((long[][])data).length == count;
            if (isDataArrayCorrect) {
                for (long[] datum : (long[][])data) {
                    if (datum.length != 2) {
                        isDataArrayCorrect = false;
                        break;
                    }
                    if (datum[0] < 0 || datum[1] < 0) {
                        throw new IllegalArgumentException
                            ("Negative value supplied for TIFF_RATIONAL");
                    }
                    if (datum[0] > MAX_UINT32 || datum[1] > MAX_UINT32) {
                        throw new IllegalArgumentException
                            ("Too large value supplied for TIFF_RATIONAL");
                    }
                }
            }
            break;
        case TIFFTag.TIFF_SSHORT:
            isDataArrayCorrect = data instanceof short[]
                && ((short[])data).length == count;
            break;
        case TIFFTag.TIFF_SLONG:
            isDataArrayCorrect = data instanceof int[]
                && ((int[])data).length == count;
            break;
        case TIFFTag.TIFF_SRATIONAL:
            isDataArrayCorrect = data instanceof int[][]
                && ((int[][])data).length == count;
            if (isDataArrayCorrect) {
                for (int[] datum : (int[][])data) {
                    if (datum.length != 2) {
                        isDataArrayCorrect = false;
                        break;
                    }
                }
            }
            break;
        case TIFFTag.TIFF_FLOAT:
            isDataArrayCorrect = data instanceof float[]
                && ((float[])data).length == count;
            break;
        case TIFFTag.TIFF_DOUBLE:
            isDataArrayCorrect = data instanceof double[]
                && ((double[])data).length == count;
            break;
        default:
            throw new IllegalArgumentException("Unknown data type "+type);
        }

        if (!isDataArrayCorrect) {
            throw new IllegalArgumentException
                ("Illegal class or length for data array");
        }

        this.tag = tag;
        this.tagNumber = tag.getNumber();
        this.type = type;
        this.count = count;
        this.data = data;
    }

    /**
     * Constructs a data array using {@link #createArrayForType
     * createArrayForType()} and invokes
     * {@link #TIFFField(TIFFTag,int,int,Object)} with the supplied
     * parameters and the created array.
     *
     * @param tag The tag to associated with this field.
     * @param type One of the {@code TIFFTag.TIFF_*} constants
     * indicating the data type of the field as written to the TIFF stream.
     * @param count The number of data values.
     * @throws NullPointerException if {@code tag == null}.
     * @throws IllegalArgumentException if {@code type} is not
     * one of the {@code TIFFTag.TIFF_*} data type constants.
     * @throws IllegalArgumentException if {@code type} is an unacceptable
     * data type for the supplied {@code TIFFTag}.
     * @throws IllegalArgumentException if {@code count < 0}.
     * @see #TIFFField(TIFFTag,int,int,Object)
     * @throws IllegalArgumentException if {@code count < 1}
     * and {@code type} is {@code TIFF_RATIONAL} or
     * {@code TIFF_SRATIONAL}.
     * @throws IllegalArgumentException if {@code count != 1}
     * and {@code type} is {@code TIFF_IFD_POINTER}.
     */
    public TIFFField(TIFFTag tag, int type, int count) {
        this(tag, type, count, createArrayForType(type, count));
    }

    /**
     * Constructs a {@code TIFFField} with a single non-negative integral
     * value. The field will have type {@link TIFFTag#TIFF_SHORT TIFF_SHORT}
     * if {@code value} is in {@code [0,0xffff]}, and type
     * {@link TIFFTag#TIFF_LONG TIFF_LONG} if {@code value} is in
     * {@code [0x10000,0xffffffff]}. The count of the field will be unity.
     *
     * @param tag The tag to associate with this field.
     * @param value The value to associate with this field.
     * @throws NullPointerException if {@code tag == null}.
     * @throws IllegalArgumentException if {@code value} is not in
     * {@code [0,0xffffffff]}.
     * @throws IllegalArgumentException if {@code value} is in
     * {@code [0,0xffff]} and {@code TIFF_SHORT} is an unacceptable type
     * for the {@code TIFFTag}, or if {@code value} is in
     * {@code [0x10000,0xffffffff]} and {@code TIFF_LONG} is an unacceptable
     * type for the {@code TIFFTag}.
     */
    public TIFFField(TIFFTag tag, long value) {
        if(tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value < 0!");
        }
        if (value > MAX_UINT32) {
            throw new IllegalArgumentException("value > 0xffffffff!");
        }

        this.tag = tag;
        this.tagNumber = tag.getNumber();
        this.count = 1;

        if (value < 65536) {
            if (!tag.isDataTypeOK(TIFFTag.TIFF_SHORT)) {
                throw new IllegalArgumentException("Illegal data type "
                    + getTypeName(TIFFTag.TIFF_SHORT) + " for tag "
                    + "\"" + tag.getName() + "\"");
            }
            this.type = TIFFTag.TIFF_SHORT;
            char[] cdata = new char[1];
            cdata[0] = (char)value;
            this.data = cdata;
        } else {
            if (!tag.isDataTypeOK(TIFFTag.TIFF_LONG)) {
                throw new IllegalArgumentException("Illegal data type "
                    + getTypeName(TIFFTag.TIFF_LONG) + " for tag "
                    + "\"" + tag.getName() + "\"");
            }
            this.type = TIFFTag.TIFF_LONG;
            long[] ldata = new long[1];
            ldata[0] = value;
            this.data = ldata;
        }
    }

    /**
     * Constructs a {@code TIFFField} with an IFD offset and contents.
     * The offset will be stored as the data of this field as
     * {@code long[] {offset}}. The directory will not be cloned. The count
     * of the field will be unity.
     *
     * @param tag The tag to associated with this field.
     * @param type One of the constants {@code TIFFTag.TIFF_LONG} or
     * {@code TIFFTag.TIFF_IFD_POINTER}.
     * @param offset The IFD offset.
     * @param dir The directory.
     *
     * @throws NullPointerException if {@code tag == null}.
     * @throws IllegalArgumentException if {@code type} is an unacceptable
     * data type for the supplied {@code TIFFTag}.
     * @throws IllegalArgumentException if {@code type} is neither
     * {@code TIFFTag.TIFF_LONG} nor {@code TIFFTag.TIFF_IFD_POINTER}.
     * @throws IllegalArgumentException if {@code offset <= 0}.
     * @throws NullPointerException if {@code dir == null}.
     *
     * @see #TIFFField(TIFFTag,int,int,Object)
     */
    public TIFFField(TIFFTag tag, int type, long offset, TIFFDirectory dir) {
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        } else if (type < TIFFTag.MIN_DATATYPE || type > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type "+type);
        } else if (!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type
                + " for " + tag.getName() + " tag");
        } else if (type != TIFFTag.TIFF_LONG
                   && type != TIFFTag.TIFF_IFD_POINTER) {
            throw new IllegalArgumentException("type " + type
                + " is neither TIFFTag.TIFF_LONG nor TIFFTag.TIFF_IFD_POINTER");
        } else if (offset <= 0) {
            throw new IllegalArgumentException("offset " + offset
                + " is non-positive");
        } else if (dir == null) {
            throw new NullPointerException("dir == null");
        }

        this.tag = tag;
        this.tagNumber = tag.getNumber();
        this.type = type;
        this.count = 1;
        this.data = new long[] {offset};

        this.dir = dir;
    }

    /**
     * Retrieves the tag associated with this field.
     *
     * @return The associated {@code TIFFTag}.
     */
    public TIFFTag getTag() {
        return tag;
    }

    /**
     * Retrieves the tag number in the range {@code [0,65535]}.
     *
     * @return The tag number.
     */
    public int getTagNumber() {
        return tagNumber;
    }

    /**
     * Returns the type of the data stored in the field.  For a TIFF 6.0
     * stream, the value will equal one of the {@code TIFFTag.TIFF_*}
     * constants. For future revisions of TIFF, higher values are possible.
     *
     * @return The data type of the field value.
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the name of the supplied data type constant.
     *
     * @param dataType One of the {@code TIFFTag.TIFF_*} constants
     * indicating the data type of the field as written to the TIFF stream.
     * @return The type name corresponding to the supplied type constant.
     * @throws IllegalArgumentException if {@code dataType} is not
     * one of the {@code TIFFTag.TIFF_*} data type constants.
     */
    public static String getTypeName(int dataType) {
        if (dataType < TIFFTag.MIN_DATATYPE ||
            dataType > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type "+dataType);
        }

        return TYPE_NAMES[dataType];
    }

    /**
     * Returns the data type constant corresponding to the supplied data
     * type name. If the name is unknown {@code -1} will be returned.
     *
     * @param typeName The type name.
     * @return One of the {@code TIFFTag.TIFF_*} constants or
     * {@code -1} if the name is not recognized.
     */
    public static int getTypeByName(String typeName) {
        for (int i = TIFFTag.MIN_DATATYPE; i <= TIFFTag.MAX_DATATYPE; i++) {
            if (typeName.equals(TYPE_NAMES[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Creates an array appropriate for the indicated data type.
     *
     * @param dataType One of the {@code TIFFTag.TIFF_*} data type
     * constants.
     * @param count The number of values in the array.
     * @return An array appropriate for the specified data type.
     *
     * @throws IllegalArgumentException if {@code dataType} is not
     * one of the {@code TIFFTag.TIFF_*} data type constants.
     * @throws IllegalArgumentException if {@code count < 0}.
     * @throws IllegalArgumentException if {@code count < 1}
     * and {@code type} is {@code TIFF_RATIONAL} or
     * {@code TIFF_SRATIONAL}.
     * @throws IllegalArgumentException if {@code count != 1}
     * and {@code type} is {@code TIFF_IFD_POINTER}.
     */
    public static Object createArrayForType(int dataType, int count) {

        if(count < 0) {
            throw new IllegalArgumentException("count < 0!");
        } else if ((dataType == TIFFTag.TIFF_RATIONAL
                   || dataType == TIFFTag.TIFF_SRATIONAL)
                  && count < 1) {
            throw new IllegalArgumentException
                ("Type is TIFF_RATIONAL or TIFF_SRATIONAL and count < 1");
        } else if (dataType == TIFFTag.TIFF_IFD_POINTER && count != 1) {
            throw new IllegalArgumentException
                ("Type is TIFF_IFD_POINTER and count != 1");
        }

        switch (dataType) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_SBYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return new byte[count];
        case TIFFTag.TIFF_ASCII:
            return new String[count];
        case TIFFTag.TIFF_SHORT:
            return new char[count];
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return new long[count];
        case TIFFTag.TIFF_RATIONAL:
            return new long[count][2];
        case TIFFTag.TIFF_SSHORT:
            return new short[count];
        case TIFFTag.TIFF_SLONG:
            return new int[count];
        case TIFFTag.TIFF_SRATIONAL:
            return new int[count][2];
        case TIFFTag.TIFF_FLOAT:
            return new float[count];
        case TIFFTag.TIFF_DOUBLE:
            return new double[count];
        default:
            throw new IllegalArgumentException("Unknown data type "+dataType);
        }
    }

    /**
     * Returns the {@code TIFFField} as a node named either
     * {@code "TIFFField"} or {@code "TIFFIFD"} as described in the
     * TIFF native image metadata specification. The node will be named
     * {@code "TIFFIFD"} if and only if {@link #hasDirectory()} returns
     * {@code true} and the field's type is either {@link TIFFTag#TIFF_LONG}
     * or {@link TIFFTag#TIFF_IFD_POINTER}.
     *
     * @return a {@code Node} named {@code "TIFFField"} or
     * {@code "TIFFIFD"}.
     */
    public Node getAsNativeNode() {
        return new TIFFFieldNode(this);
    }

    /**
     * Indicates whether the value associated with the field is of
     * integral data type.
     *
     * @return Whether the field type is integral.
     */
    public boolean isIntegral() {
        return IS_INTEGRAL[type];
    }

    /**
     * Returns the number of data items present in the field.  For
     * {@code TIFFTag.TIFF_ASCII} fields, the value returned is the
     * number of {@code String}s, not the total length of the
     * data as in the file representation.
     *
     * @return The number of data items present in the field.
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns a reference to the data object associated with the field.
     *
     * @return The data object of the field.
     */
    public Object getData() {
        return data;
    }

    /**
     * Returns the data as an uninterpreted array of
     * {@code byte}s.  The type of the field must be one of
     * {@code TIFFTag.TIFF_BYTE}, {@code TIFF_SBYTE}, or
     * {@code TIFF_UNDEFINED}.
     *
     * <p> For data in {@code TIFFTag.TIFF_BYTE} format, the application
     * must take care when promoting the data to longer integral types
     * to avoid sign extension.
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_BYTE}, {@code TIFF_SBYTE}, or
     * {@code TIFF_UNDEFINED}.
     * @return The data as an uninterpreted array of bytes.
     */
    public byte[] getAsBytes() {
        return (byte[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_SHORT} data as an array of
     * {@code char}s (unsigned 16-bit integers).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_SHORT}.
     * @return The data as an array of {@code char}s.
     */
    public char[] getAsChars() {
        return (char[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_SSHORT} data as an array of
     * {@code short}s (signed 16-bit integers).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_SSHORT}.
     * @return The data as an array of {@code short}s.
     */
    public short[] getAsShorts() {
        return (short[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_SLONG} data as an array of
     * {@code int}s (signed 32-bit integers).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_SHORT}, {@code TIFF_SSHORT}, or
     * {@code TIFF_SLONG}.
     * @return The data as an array of {@code int}s.
     */
    public int[] getAsInts() {
        if (data instanceof int[]) {
            return (int[])data;
        } else if (data instanceof char[]){
            char[] cdata = (char[])data;
            int[] idata = new int[cdata.length];
            for (int i = 0; i < cdata.length; i++) {
                idata[i] = cdata[i] & 0xffff;
            }
            return idata;
        } else if (data instanceof short[]){
            short[] sdata = (short[])data;
            int[] idata = new int[sdata.length];
            for (int i = 0; i < sdata.length; i++) {
                idata[i] = (int)sdata[i];
            }
            return idata;
        } else {
            throw new ClassCastException("Data not char[], short[], or int[]!");
        }
    }

    /**
     * Returns {@code TIFFTag.TIFF_LONG} or
     * {@code TIFF_IFD_POINTER} data as an array of
     * {@code long}s (signed 64-bit integers).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_LONG} or {@code TIFF_IFD_POINTER}.
     * @return The data as an array of {@code long}s.
     */
    public long[] getAsLongs() {
        return (long[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_FLOAT} data as an array of
     * {@code float}s (32-bit floating-point values).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_FLOAT}.
     * @return The data as an array of {@code float}s.
     */
    public float[] getAsFloats() {
        return (float[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_DOUBLE} data as an array of
     * {@code double}s (64-bit floating-point values).
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_DOUBLE}.
     * @return The data as an array of {@code double}s.
     */
    public double[] getAsDoubles() {
        return (double[])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_SRATIONAL} data as an array of
     * 2-element arrays of {@code int}s.
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_SRATIONAL}.
     * @return The data as an array of signed rationals.
     */
    public int[][] getAsSRationals() {
        return (int[][])data;
    }

    /**
     * Returns {@code TIFFTag.TIFF_RATIONAL} data as an array of
     * 2-element arrays of {@code long}s.
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_RATIONAL}.
     * @return The data as an array of unsigned rationals.
     */
    public long[][] getAsRationals() {
        return (long[][])data;
    }

    /**
     * Returns data in any format as an {@code int}.
     *
     * <p> {@code TIFFTag.TIFF_BYTE} values are treated as unsigned; that
     * is, no sign extension will take place and the returned value
     * will be in the range [0, 255].  {@code TIFF_SBYTE} data
     * will be returned in the range [-128, 127].
     *
     * <p> A {@code TIFF_UNDEFINED} value is treated as though
     * it were a {@code TIFF_BYTE}.
     *
     * <p> Data in {@code TIFF_SLONG}, {@code TIFF_LONG},
     * {@code TIFF_FLOAT}, {@code TIFF_DOUBLE} or
     * {@code TIFF_IFD_POINTER} format are simply cast to
     * {@code int} and may suffer from truncation.
     *
     * <p> Data in {@code TIFF_SRATIONAL} or
     * {@code TIFF_RATIONAL} format are evaluated by dividing the
     * numerator into the denominator using double-precision
     * arithmetic and then casting to {@code int}.  Loss of
     * precision and truncation may occur.
     *
     * <p> Data in {@code TIFF_ASCII} format will be parsed as by
     * the {@code Double.parseDouble} method, with the result
     * case to {@code int}.
     *
     * @param index The index of the data.
     * @return The data at the given index as an {@code int}.
     */
    public int getAsInt(int index) {
        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFFTag.TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFFTag.TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFFTag.TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFFTag.TIFF_SLONG:
            return ((int[])data)[index];
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return (int)((long[])data)[index];
        case TIFFTag.TIFF_FLOAT:
            return (int)((float[])data)[index];
        case TIFFTag.TIFF_DOUBLE:
            return (int)((double[])data)[index];
        case TIFFTag.TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (int)((double)ivalue[0]/ivalue[1]);
        case TIFFTag.TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (int)((double)lvalue[0]/lvalue[1]);
        case TIFFTag.TIFF_ASCII:
             String s = ((String[])data)[index];
             return (int)Double.parseDouble(s);
        default:
            throw new ClassCastException(); // should never happen
        }
    }

    /**
     * Returns data in any format as a {@code long}.
     *
     * <p> {@code TIFFTag.TIFF_BYTE} and {@code TIFF_UNDEFINED} data
     * are treated as unsigned; that is, no sign extension will take
     * place and the returned value will be in the range [0, 255].
     * {@code TIFF_SBYTE} data will be returned in the range
     * [-128, 127].
     *
     * <p> Data in {@code TIFF_FLOAT} and {@code TIFF_DOUBLE} are
     * simply cast to {@code long} and may suffer from truncation.
     *
     * <p> Data in {@code TIFF_SRATIONAL} or
     * {@code TIFF_RATIONAL} format are evaluated by dividing the
     * numerator into the denominator using double-precision
     * arithmetic and then casting to {@code long}.  Loss of
     * precision and truncation may occur.
     *
     * <p> Data in {@code TIFF_ASCII} format will be parsed as by
     * the {@code Double.parseDouble} method, with the result
     * cast to {@code long}.
     *
     * @param index The index of the data.
     * @return The data at the given index as a {@code long}.
     */
    public long getAsLong(int index) {
        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFFTag.TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFFTag.TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFFTag.TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFFTag.TIFF_SLONG:
            return ((int[])data)[index];
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return ((long[])data)[index];
        case TIFFTag.TIFF_FLOAT:
            return (long)((float[])data)[index];
        case TIFFTag.TIFF_DOUBLE:
            return (long)((double[])data)[index];
        case TIFFTag.TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (long)((double)ivalue[0]/ivalue[1]);
        case TIFFTag.TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (long)((double)lvalue[0]/lvalue[1]);
        case TIFFTag.TIFF_ASCII:
             String s = ((String[])data)[index];
             return (long)Double.parseDouble(s);
        default:
            throw new ClassCastException(); // should never happen
        }
    }

    /**
     * Returns data in any format as a {@code float}.
     *
     * <p> {@code TIFFTag.TIFF_BYTE} and {@code TIFF_UNDEFINED} data
     * are treated as unsigned; that is, no sign extension will take
     * place and the returned value will be in the range [0, 255].
     * {@code TIFF_SBYTE} data will be returned in the range
     * [-128, 127].
     *
     * <p> Data in {@code TIFF_SLONG}, {@code TIFF_LONG},
     * {@code TIFF_DOUBLE}, or {@code TIFF_IFD_POINTER} format are
     * simply cast to {@code float} and may suffer from
     * truncation.
     *
     * <p> Data in {@code TIFF_SRATIONAL} or
     * {@code TIFF_RATIONAL} format are evaluated by dividing the
     * numerator into the denominator using double-precision
     * arithmetic and then casting to {@code float}.
     *
     * <p> Data in {@code TIFF_ASCII} format will be parsed as by
     * the {@code Double.parseDouble} method, with the result
     * cast to {@code float}.
     *
     * @param index The index of the data.
     * @return The data at the given index as a {@code float}.
     */
    public float getAsFloat(int index) {
        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFFTag.TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFFTag.TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFFTag.TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFFTag.TIFF_SLONG:
            return ((int[])data)[index];
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return ((long[])data)[index];
        case TIFFTag.TIFF_FLOAT:
            return ((float[])data)[index];
        case TIFFTag.TIFF_DOUBLE:
            return (float)((double[])data)[index];
        case TIFFTag.TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (float)((double)ivalue[0]/ivalue[1]);
        case TIFFTag.TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (float)((double)lvalue[0]/lvalue[1]);
        case TIFFTag.TIFF_ASCII:
             String s = ((String[])data)[index];
             return (float)Double.parseDouble(s);
        default:
            throw new ClassCastException(); // should never happen
        }
    }

    /**
     * Returns data in any format as a {@code double}.
     *
     * <p> {@code TIFFTag.TIFF_BYTE} and {@code TIFF_UNDEFINED} data
     * are treated as unsigned; that is, no sign extension will take
     * place and the returned value will be in the range [0, 255].
     * {@code TIFF_SBYTE} data will be returned in the range
     * [-128, 127].
     *
     * <p> Data in {@code TIFF_SRATIONAL} or
     * {@code TIFF_RATIONAL} format are evaluated by dividing the
     * numerator into the denominator using double-precision
     * arithmetic.
     *
     * <p> Data in {@code TIFF_ASCII} format will be parsed as by
     * the {@code Double.parseDouble} method.
     *
     * @param index The index of the data.
     * @return The data at the given index as a {@code double}.
     */
    public double getAsDouble(int index) {
        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFFTag.TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFFTag.TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFFTag.TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFFTag.TIFF_SLONG:
            return ((int[])data)[index];
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return ((long[])data)[index];
        case TIFFTag.TIFF_FLOAT:
            return ((float[])data)[index];
        case TIFFTag.TIFF_DOUBLE:
            return ((double[])data)[index];
        case TIFFTag.TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (double)ivalue[0]/ivalue[1];
        case TIFFTag.TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (double)lvalue[0]/lvalue[1];
        case TIFFTag.TIFF_ASCII:
             String s = ((String[])data)[index];
             return Double.parseDouble(s);
        default:
            throw new ClassCastException(); // should never happen
        }
    }

    /**
     * Returns a {@code TIFFTag.TIFF_ASCII} value as a
     * {@code String}.
     *
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_ASCII}.
     *
     * @param index The index of the data.
     * @return The data at the given index as a {@code String}.
     */
    public String getAsString(int index) {
        return ((String[])data)[index];
    }

    /**
     * Returns a {@code TIFFTag.TIFF_SRATIONAL} data item as a
     * two-element array of {@code int}s.
     *
     * @param index The index of the data.
     * @return The data at the given index as a signed rational.
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_SRATIONAL}.
     */
    public int[] getAsSRational(int index) {
        return ((int[][])data)[index];
    }

    /**
     * Returns a TIFFTag.TIFF_RATIONAL data item as a two-element array
     * of ints.
     *
     * @param index The index of the data.
     * @return The data at the given index as an unsigned rational.
     * @throws ClassCastException if the field is not of type
     * {@code TIFF_RATIONAL}.
     */
    public long[] getAsRational(int index) {
        return ((long[][])data)[index];
    }


    /**
     * Returns a {@code String} containing a human-readable
     * version of the data item.  Data of type
     * {@code TIFFTag.TIFF_RATIONAL} or {@code TIFF_SRATIONAL} are
     * represented as a pair of integers separated by a
     * {@code '/'} character.  If the numerator of a
     * {@code TIFFTag.TIFF_RATIONAL} or {@code TIFF_SRATIONAL} is an integral
     * multiple of the denominator, then the value is represented as
     * {@code "q/1"} where {@code q} is the quotient of the numerator and
     * denominator.
     *
     * @param index The index of the data.
     * @return The data at the given index as a {@code String}.
     * @throws ClassCastException if the field is not of one of the
     * legal field types.
     */
    public String getValueAsString(int index) {
        switch (type) {
        case TIFFTag.TIFF_ASCII:
            return ((String[])data)[index];
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
            return Integer.toString(((byte[])data)[index] & 0xff);
        case TIFFTag.TIFF_SBYTE:
            return Integer.toString(((byte[])data)[index]);
        case TIFFTag.TIFF_SHORT:
            return Integer.toString(((char[])data)[index] & 0xffff);
        case TIFFTag.TIFF_SSHORT:
            return Integer.toString(((short[])data)[index]);
        case TIFFTag.TIFF_SLONG:
            return Integer.toString(((int[])data)[index]);
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            return Long.toString(((long[])data)[index]);
        case TIFFTag.TIFF_FLOAT:
            return Float.toString(((float[])data)[index]);
        case TIFFTag.TIFF_DOUBLE:
            return Double.toString(((double[])data)[index]);
        case TIFFTag.TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            String srationalString;
            if(ivalue[1] != 0 && ivalue[0] % ivalue[1] == 0) {
                // If the denominator is a non-zero integral divisor
                // of the numerator then convert the fraction to be
                // with respect to a unity denominator.
                srationalString =
                    Integer.toString(ivalue[0] / ivalue[1]) + "/1";
            } else {
                // Use the values directly.
                srationalString =
                    Integer.toString(ivalue[0]) +
                    "/" +
                    Integer.toString(ivalue[1]);
            }
            return srationalString;
        case TIFFTag.TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            String rationalString;
            if(lvalue[1] != 0L && lvalue[0] % lvalue[1] == 0) {
                // If the denominator is a non-zero integral divisor
                // of the numerator then convert the fraction to be
                // with respect to a unity denominator.
                rationalString =
                    Long.toString(lvalue[0] / lvalue[1]) + "/1";
            } else {
                // Use the values directly.
                rationalString =
                    Long.toString(lvalue[0]) +
                    "/" +
                    Long.toString(lvalue[1]);
            }
            return rationalString;
        default:
            throw new ClassCastException(); // should never happen
        }
    }

    /**
     * Returns whether the field has a {@code TIFFDirectory}.
     *
     * @return true if and only if getDirectory() returns non-null.
     */
    public boolean hasDirectory() {
        return getDirectory() != null;
    }

    /**
     * Returns the associated {@code TIFFDirectory}, if available. If no
     * directory is set, then {@code null} will be returned.
     *
     * @return the TIFFDirectory instance or null.
     */
    public TIFFDirectory getDirectory() {
        return dir;
    }

    /**
     * Clones the field and all the information contained therein.
     *
     * @return A clone of this {@code TIFFField}.
     * @throws CloneNotSupportedException if the instance cannot be cloned.
     */
    @Override
    public TIFFField clone() throws CloneNotSupportedException {
        TIFFField field = (TIFFField)super.clone();

        Object fieldData;
        switch (type) {
        case TIFFTag.TIFF_BYTE:
        case TIFFTag.TIFF_UNDEFINED:
        case TIFFTag.TIFF_SBYTE:
            fieldData = ((byte[])data).clone();
            break;
        case TIFFTag.TIFF_SHORT:
            fieldData = ((char[])data).clone();
            break;
        case TIFFTag.TIFF_SSHORT:
            fieldData = ((short[])data).clone();
            break;
        case TIFFTag.TIFF_SLONG:
            fieldData = ((int[])data).clone();
            break;
        case TIFFTag.TIFF_LONG:
        case TIFFTag.TIFF_IFD_POINTER:
            fieldData = ((long[])data).clone();
            break;
        case TIFFTag.TIFF_FLOAT:
            fieldData = ((float[])data).clone();
            break;
        case TIFFTag.TIFF_DOUBLE:
            fieldData = ((double[])data).clone();
            break;
        case TIFFTag.TIFF_SRATIONAL:
            fieldData = ((int[][])data).clone();
            break;
        case TIFFTag.TIFF_RATIONAL:
            fieldData = ((long[][])data).clone();
            break;
        case TIFFTag.TIFF_ASCII:
            fieldData = ((String[])data).clone();
            break;
        default:
            throw new ClassCastException(); // should never happen
        }

        field.tag = tag;
        field.tagNumber = tagNumber;
        field.type = type;
        field.count = count;
        field.data = fieldData;
        field.dir = dir != null ? dir.clone() : null;

        return field;
    }
}
