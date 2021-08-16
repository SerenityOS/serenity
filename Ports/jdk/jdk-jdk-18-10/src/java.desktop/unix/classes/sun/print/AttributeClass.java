/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AttributeClass {
    private String myName;
    private int myType;
    private int nameLen;
    private Object myValue;

    public static final int TAG_UNSUPPORTED_VALUE = 0x10;
    public static final int TAG_INT = 0x21;
    public static final int TAG_BOOL = 0x22;
    public static final int TAG_ENUM = 0x23;
    public static final int TAG_OCTET = 0x30;
    public static final int TAG_DATE = 0x31;
    public static final int TAG_RESOLUTION = 0x32;
    public static final int TAG_RANGE_INTEGER = 0x33;

    public static final int TAG_TEXT_LANGUAGE = 0x35;
    public static final int TAG_NAME_LANGUAGE = 0x36;

    public static final int TAG_TEXT_WO_LANGUAGE = 0x41;
    public static final int TAG_NAME_WO_LANGUAGE = 0x42;
    public static final int TAG_KEYWORD = 0x44;
    public static final int TAG_URI = 0x45;
    public static final int TAG_CHARSET = 0x47;
    public static final int TAG_NATURALLANGUAGE = 0x48;
    public static final int TAG_MIME_MEDIATYPE = 0x49;
    public static final int TAG_MEMBER_ATTRNAME = 0x4A;


    public static final AttributeClass ATTRIBUTES_CHARSET =
        new AttributeClass("attributes-charset",
                           TAG_CHARSET, "utf-8");
    public static final AttributeClass ATTRIBUTES_NATURAL_LANGUAGE =
        new AttributeClass("attributes-natural-language",
                           TAG_NATURALLANGUAGE, "en");

    /*
     * value passed in by IPPPrintService.readIPPResponse is a sequence
     * of bytes with this format
     * | length1 | byte1 | byte 2 | ... byten | length2 | byte1 ... byten |
     *      :
     * | lengthN | byte1 ... byten | total number of values|
     */
    protected AttributeClass(String name, int type, Object value) {
        myName = name;
        myType = type;
        nameLen = name.length();
        myValue = value;
    }

    public byte getType() {
        return (byte)myType;
    }

    public char[] getLenChars() {
        char[] chars = new char[2];
        chars[0] = 0;
        chars[1] = (char)nameLen;
        return chars;
    }

    /**
     * Returns raw data.
     */
    public Object getObjectValue() {
        return myValue;
    }

    /**
     * Returns single int value.
     */
    public int getIntValue() {
        byte[] bufArray = (byte[])myValue;

        if (bufArray != null) {
            byte[] buf = new byte[4];
            for (int i=0; i<4; i++) {
                buf[i] = bufArray[i+1];
            }

            return convertToInt(buf);
        }
        return 0;
    }

    /**
     * Returns array of int values.
     */
    public int[] getArrayOfIntValues() {

        byte[] bufArray = (byte[])myValue;
        if (bufArray != null) {

            //ArrayList valList = new ArrayList();
            ByteArrayInputStream bufStream =
                new ByteArrayInputStream(bufArray);
            int available = bufStream.available();

            // total number of values is at the end of the stream
            bufStream.mark(available);
            bufStream.skip(available-1);
            int length = bufStream.read();
            bufStream.reset();

            int[] valueArray = new int[length];
            for (int i = 0; i < length; i++) {
                // read length
                int valLength = bufStream.read();
                if (valLength != 4) {
                    // invalid data
                    return null;
                }

                byte[] bufBytes = new byte[valLength];
                bufStream.read(bufBytes, 0, valLength);
                valueArray[i] = convertToInt(bufBytes);

            }
            return valueArray;
        }
        return null;
    }

    /**
     * Returns 2 int values.
     */
    public int[] getIntRangeValue() {
        int[] range = {0, 0};
        byte[] bufArray = (byte[])myValue;
        if (bufArray != null) {
            int nBytes = 4; // 32-bit signed integer
            for (int j=0; j<2; j++) { // 2 set of integers
                byte[] intBytes = new byte[nBytes];
                // REMIND: # bytes should be 8
                for (int i=0; i< nBytes; i++) {
                    //+ 1 because the 1st byte is length
                    intBytes[i] = bufArray[i+(4*j)+1];
                }
                range[j] = convertToInt(intBytes);
            }
        }
        return range;

    }

    /**
     * Returns String value.
     */
    public String getStringValue() {
        //assumes only 1 attribute value.  Will get the first value
        // if > 1.
        String strVal = null;
        byte[] bufArray = (byte[])myValue;
        if (bufArray != null) {
            ByteArrayInputStream bufStream =
                new ByteArrayInputStream(bufArray);

            int valLength = bufStream.read();

            byte[] strBytes = new byte[valLength];
            bufStream.read(strBytes, 0, valLength);
            strVal = new String(strBytes, UTF_8);
        }
        return strVal;
    }


    /**
     * Returns array of String values.
     */
    public String[] getArrayOfStringValues() {

        byte[] bufArray = (byte[])myValue;
        if (bufArray != null) {
            ByteArrayInputStream bufStream =
                new ByteArrayInputStream(bufArray);
            int available = bufStream.available();

            // total number of values is at the end of the stream
            bufStream.mark(available);
            bufStream.skip(available-1);
            int length = bufStream.read();
            bufStream.reset();

            String[] valueArray = new String[length];
            for (int i = 0; i < length; i++) {
                // read length
                int valLength = bufStream.read();
                byte[] bufBytes = new byte[valLength];
                bufStream.read(bufBytes, 0, valLength);
                valueArray[i] = new String(bufBytes, UTF_8);
            }
            return valueArray;
        }
        return null;
    }


    /**
     * Returns single byte value.
     */
    public byte getByteValue() {
        byte[] bufArray = (byte[])myValue;

        if ((bufArray != null) && (bufArray.length>=2)) {
            return bufArray[1];
        }
        return 0;
    }

    /**
     * Returns attribute name.
     */
    public String getName() {
        return myName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AttributeClass)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        AttributeClass acObj = (AttributeClass) obj;
        return myType == acObj.getType() &&
               Objects.equals(myName, acObj.getName()) &&
               Objects.equals(myValue, acObj.getObjectValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myType, myName, myValue);
    }

    public String toString() {
        return myName;
    }

    private int unsignedByteToInt(byte b) {
        return (b & 0xff);
    }

    private int convertToInt(byte[] buf) {
        int intVal = 0;
        int pos = 0;
        intVal+= unsignedByteToInt(buf[pos++]) << 24;
        intVal+= unsignedByteToInt(buf[pos++]) << 16;
        intVal+= unsignedByteToInt(buf[pos++]) << 8;
        intVal+= unsignedByteToInt(buf[pos++]) << 0;
        return intVal;
    }
}
