/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.imageio.plugins.jpeg;

import java.util.Arrays;

/**
 * A class encapsulating a single JPEG Huffman table.
 * Fields are provided for the "standard" tables taken
 * from Annex K of the JPEG specification.
 * These are the tables used as defaults.
 * <p>
 * For more information about the operation of the standard JPEG plug-in,
 * see the <A HREF="../../metadata/doc-files/jpeg_metadata.html">JPEG
 * metadata format specification and usage notes</A>
 */

public class JPEGHuffmanTable {

    /* The data for the publically defined tables, as specified in ITU T.81
     * JPEG specification section K3.3 and used in the IJG library.
     */
    private static final short[] StdDCLuminanceLengths = {
        0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    };

    private static final short[] StdDCLuminanceValues = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b,
    };

    private static final short[] StdDCChrominanceLengths = {
        0x00, 0x03, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
        0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
    };

    private static final short[] StdDCChrominanceValues = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b,
    };

    private static final short[] StdACLuminanceLengths = {
        0x00, 0x02, 0x01, 0x03, 0x03, 0x02, 0x04, 0x03,
        0x05, 0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7d,
    };

    private static final short[] StdACLuminanceValues = {
        0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
        0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
        0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
        0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
        0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
        0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
        0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
        0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
        0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
        0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
        0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
        0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
        0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
        0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
        0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
        0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa,
    };

    private static final short[] StdACChrominanceLengths = {
        0x00, 0x02, 0x01, 0x02, 0x04, 0x04, 0x03, 0x04,
        0x07, 0x05, 0x04, 0x04, 0x00, 0x01, 0x02, 0x77,
    };

    private static final short[] StdACChrominanceValues = {
        0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
        0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
        0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
        0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
        0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
        0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
        0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
        0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
        0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
        0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
        0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
        0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
        0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
        0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
        0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
        0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa,
    };

    /**
     * The standard DC luminance Huffman table.
     */
    public static final JPEGHuffmanTable
        StdDCLuminance = new JPEGHuffmanTable(StdDCLuminanceLengths,
                                              StdDCLuminanceValues, false);

    /**
     * The standard DC chrominance Huffman table.
     */
    public static final JPEGHuffmanTable
        StdDCChrominance = new JPEGHuffmanTable(StdDCChrominanceLengths,
                                                StdDCChrominanceValues, false);

    /**
     * The standard AC luminance Huffman table.
     */
    public static final JPEGHuffmanTable
        StdACLuminance = new JPEGHuffmanTable(StdACLuminanceLengths,
                                              StdACLuminanceValues, false);

    /**
     * The standard AC chrominance Huffman table.
     */
    public static final JPEGHuffmanTable
        StdACChrominance = new JPEGHuffmanTable(StdACChrominanceLengths,
                                                StdACChrominanceValues, false);

    private short[] lengths;
    private short[] values;

    /**
     * Creates a Huffman table and initializes it. The input arrays are copied.
     * The arrays must describe a possible Huffman table.
     * For example, 3 codes cannot be expressed with a single bit.
     *
     * @param lengths an array of {@code short}s where {@code lengths[k]}
     * is equal to the number of values with corresponding codes of
     * length {@code k + 1} bits.
     * @param values an array of shorts containing the values in
     * order of increasing code length.
     * @throws IllegalArgumentException if {@code lengths} or
     * {@code values} are null, the length of {@code lengths} is
     * greater than 16, the length of {@code values} is greater than 256,
     * if any value in {@code lengths} or {@code values} is less
     * than zero, or if the arrays do not describe a valid Huffman table.
     */
    public JPEGHuffmanTable(short[] lengths, short[] values) {
        if (lengths == null || values == null ||
            lengths.length == 0 || values.length == 0 ||
            lengths.length > 16 || values.length > 256) {
            throw new IllegalArgumentException("Illegal lengths or values");
        }
        for (int i = 0; i<lengths.length; i++) {
            if (lengths[i] < 0) {
                throw new IllegalArgumentException("lengths["+i+"] < 0");
            }
        }
        for (int i = 0; i<values.length; i++) {
            if (values[i] < 0) {
                throw new IllegalArgumentException("values["+i+"] < 0");
            }
        }
        this.lengths = Arrays.copyOf(lengths, lengths.length);
        this.values = Arrays.copyOf(values, values.length);
        validate();
    }

    private void validate() {
        int sumOfLengths = 0;
        for (int i=0; i<lengths.length; i++) {
            sumOfLengths += lengths[i];
        }
        if (sumOfLengths != values.length) {
            throw new IllegalArgumentException("lengths do not correspond " +
                                               "to length of value table");
        }
    }

    /* Internal version which avoids the overhead of copying and checking */
    private JPEGHuffmanTable(short[] lengths, short[] values, boolean copy) {
        if (copy) {
            this.lengths = Arrays.copyOf(lengths, lengths.length);
            this.values = Arrays.copyOf(values, values.length);
        } else {
            this.lengths = lengths;
            this.values = values;
        }
    }

    /**
     * Returns an array of {@code short}s containing the number of values
     * for each length in the Huffman table. The returned array is a copy.
     *
     * @return a {@code short} array where {@code array[k-1]}
     * is equal to the number of values in the table of length {@code k}.
     * @see #getValues
     */
    public short[] getLengths() {
        return Arrays.copyOf(lengths, lengths.length);
    }

    /**
     * Returns an array of {@code short}s containing the values arranged
     * by increasing length of their corresponding codes.
     * The interpretation of the array is dependent on the values returned
     * from {@code getLengths}. The returned array is a copy.
     *
     * @return a {@code short} array of values.
     * @see #getLengths
     */
    public short[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Returns a {@code String} representing this Huffman table.
     * @return a {@code String} representing this Huffman table.
     */
    public String toString() {
        String ls = System.getProperty("line.separator", "\n");
        StringBuilder sb = new StringBuilder("JPEGHuffmanTable");
        sb.append(ls).append("lengths:");
        for (int i=0; i<lengths.length; i++) {
            sb.append(" ").append(lengths[i]);
        }
        sb.append(ls).append("values:");
        for (int i=0; i<values.length; i++) {
            sb.append(" ").append(values[i]);
        }
        return sb.toString();
    }
}
