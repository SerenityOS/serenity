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
 * A class encapsulating a single JPEG quantization table.
 * The elements appear in natural order (as opposed to zig-zag order).
 * Static variables are provided for the "standard" tables taken from
 *  Annex K of the JPEG specification, as well as the default tables
 * conventionally used for visually lossless encoding.
 * <p>
 * For more information about the operation of the standard JPEG plug-in,
 * see the <A HREF="../../metadata/doc-files/jpeg_metadata.html">JPEG
 * metadata format specification and usage notes</A>
 */

public class JPEGQTable {

    private static final int[] k1 = {
        16,  11,  10,  16,  24,  40,  51,  61,
        12,  12,  14,  19,  26,  58,  60,  55,
        14,  13,  16,  24,  40,  57,  69,  56,
        14,  17,  22,  29,  51,  87,  80,  62,
        18,  22,  37,  56,  68,  109, 103, 77,
        24,  35,  55,  64,  81,  104, 113, 92,
        49,  64,  78,  87,  103, 121, 120, 101,
        72,  92,  95,  98,  112, 100, 103, 99,
    };

    private static final int[] k1div2 = {
        8,   6,   5,   8,   12,  20,  26,  31,
        6,   6,   7,   10,  13,  29,  30,  28,
        7,   7,   8,   12,  20,  29,  35,  28,
        7,   9,   11,  15,  26,  44,  40,  31,
        9,   11,  19,  28,  34,  55,  52,  39,
        12,  18,  28,  32,  41,  52,  57,  46,
        25,  32,  39,  44,  52,  61,  60,  51,
        36,  46,  48,  49,  56,  50,  52,  50,
    };

    private static final int[] k2 = {
        17,  18,  24,  47,  99,  99,  99,  99,
        18,  21,  26,  66,  99,  99,  99,  99,
        24,  26,  56,  99,  99,  99,  99,  99,
        47,  66,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
        99,  99,  99,  99,  99,  99,  99,  99,
    };

    private static final int[] k2div2 = {
        9,   9,   12,  24,  50,  50,  50,  50,
        9,   11,  13,  33,  50,  50,  50,  50,
        12,  13,  28,  50,  50,  50,  50,  50,
        24,  33,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
        50,  50,  50,  50,  50,  50,  50,  50,
    };

    /**
     * The sample luminance quantization table given in the JPEG
     * specification, table K.1. According to the specification,
     * these values produce "good" quality output.
     * @see #K1Div2Luminance
     */
    public static final JPEGQTable
        K1Luminance = new JPEGQTable(k1, false);

    /**
     * The sample luminance quantization table given in the JPEG
     * specification, table K.1, with all elements divided by 2.
     * According to the specification, these values produce "very good"
     * quality output. This is the table usually used for "visually lossless"
     * encoding, and is the default luminance table used if the default
     * tables and quality settings are used.
     * @see #K1Luminance
     */
    public static final JPEGQTable
        K1Div2Luminance = new JPEGQTable(k1div2, false);

    /**
     * The sample chrominance quantization table given in the JPEG
     * specification, table K.2. According to the specification,
     * these values produce "good" quality output.
     * @see #K2Div2Chrominance
     */
    public static final JPEGQTable K2Chrominance =
        new JPEGQTable(k2, false);

    /**
     * The sample chrominance quantization table given in the JPEG
     * specification, table K.1, with all elements divided by 2.
     * According to the specification, these values produce "very good"
     * quality output. This is the table usually used for "visually lossless"
     * encoding, and is the default chrominance table used if the default
     * tables and quality settings are used.
     * @see #K2Chrominance
     */
    public static final JPEGQTable K2Div2Chrominance =
        new JPEGQTable(k2div2, false);

    private int[] qTable;

    private JPEGQTable(int[] table, boolean copy) {
        qTable = (copy) ? Arrays.copyOf(table, table.length) : table;
    }

    /**
     * Constructs a quantization table from the argument, which must
     * contain 64 elements in natural order (not zig-zag order).
     * A copy is made of the input array.
     * @param table the quantization table, as an {@code int} array.
     * @throws IllegalArgumentException if {@code table} is
     * {@code null} or {@code table.length} is not equal to 64.
     */
    public JPEGQTable(int[] table) {
        if (table == null) {
            throw new IllegalArgumentException("table must not be null.");
        }
        if (table.length != 64) {
            throw new IllegalArgumentException("table.length != 64");
        }
        qTable = Arrays.copyOf(table, table.length);
    }

    /**
     * Returns a copy of the current quantization table as an array
     * of {@code int}s in natural (not zig-zag) order.
     * @return A copy of the current quantization table.
     */
    public int[] getTable() {
        return Arrays.copyOf(qTable, qTable.length);
    }

    /**
     * Returns a new quantization table where the values are multiplied
     * by {@code scaleFactor} and then clamped to the range 1..32767
     * (or to 1..255 if {@code forceBaseline} is true).
     * <p>
     * Values of {@code scaleFactor} less than 1 tend to improve
     * the quality level of the table, and values greater than 1.0
     * degrade the quality level of the table.
     * @param scaleFactor multiplication factor for the table.
     * @param forceBaseline if {@code true},
     * the values will be clamped to the range 1..255
     * @return a new quantization table that is a linear multiple
     * of the current table.
     */
    public JPEGQTable getScaledInstance(float scaleFactor,
                                        boolean forceBaseline) {
        int max = (forceBaseline) ? 255 : 32767;
        int[] scaledTable = new int[qTable.length];
        for (int i=0; i<qTable.length; i++) {
            int sv = (int)((qTable[i] * scaleFactor)+0.5f);
            if (sv < 1) {
                sv = 1;
            }
            if (sv > max) {
                sv = max;
            }
            scaledTable[i] = sv;
        }
        return new JPEGQTable(scaledTable);
    }

    /**
     * Returns a {@code String} representing this quantization table.
     * @return a {@code String} representing this quantization table.
     */
    public String toString() {
        String ls = System.getProperty("line.separator", "\n");
        StringBuilder sb = new StringBuilder("JPEGQTable:"+ls);
        for (int i=0; i < qTable.length; i++) {
            if (i % 8 == 0) {
                sb.append('\t');
            }
            sb.append(qTable[i]);
            sb.append(((i % 8) == 7) ? ls : ' ');
        }
        return sb.toString();
    }
}
