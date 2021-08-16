/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute.standard;

import java.io.Serial;

import javax.print.attribute.EnumSyntax;

/**
 * Class {@code MediaSizeName} is a subclass of {@code Media}.
 * <p>
 * This attribute can be used instead of specifying {@code MediaName} or
 * {@code MediaTray}.
 * <p>
 * Class {@code MediaSizeName} currently declares a few standard media name
 * values.
 * <p>
 * <b>IPP Compatibility:</b> {@code MediaSizeName} is a representation class for
 * values of the IPP "media" attribute which names media sizes. The names of the
 * media sizes correspond to those in the IPP 1.1 RFC
 * <a href="http://www.ietf.org/rfc/rfc2911.txt">RFC 2911</a>
 */
public class MediaSizeName extends Media {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 2778798329756942747L;

    /**
     * A0 size.
     */
    public static final MediaSizeName ISO_A0 = new MediaSizeName(0);

    /**
     * A1 size.
     */
    public static final MediaSizeName ISO_A1 = new MediaSizeName(1);

    /**
     * A2 size.
     */
    public static final MediaSizeName ISO_A2 = new MediaSizeName(2);

    /**
     * A3 size.
     */
    public static final MediaSizeName ISO_A3 = new MediaSizeName(3);

    /**
     * A4 size.
     */
    public static final MediaSizeName ISO_A4 = new MediaSizeName(4);

    /**
     * A5 size.
     */
    public static final MediaSizeName ISO_A5 = new MediaSizeName(5);

    /**
     * A6 size.
     */
    public static final MediaSizeName ISO_A6 = new MediaSizeName(6);

    /**
     * A7 size.
     */
    public static final MediaSizeName ISO_A7 = new MediaSizeName(7);

    /**
     * A8 size.
     */
    public static final MediaSizeName ISO_A8 = new MediaSizeName(8);

    /**
     * A9 size.
     */
    public static final MediaSizeName ISO_A9 = new MediaSizeName(9);

    /**
     * A10 size.
     */
    public static final MediaSizeName ISO_A10 = new MediaSizeName(10);

    /**
     * ISO B0 size.
     */
    public static final MediaSizeName ISO_B0 = new MediaSizeName(11);

    /**
     * ISO B1 size.
     */
    public static final MediaSizeName ISO_B1 = new MediaSizeName(12);

    /**
     * ISO B2 size.
     */
    public static final MediaSizeName ISO_B2 = new MediaSizeName(13);

    /**
     * ISO B3 size.
     */
    public static final MediaSizeName ISO_B3 = new MediaSizeName(14);

    /**
     * ISO B4 size.
     */
    public static final MediaSizeName ISO_B4 = new MediaSizeName(15);

    /**
     * ISO B5 size.
     */
    public static final MediaSizeName ISO_B5 = new MediaSizeName(16);

    /**
     * ISO B6 size.
     */
    public static final MediaSizeName ISO_B6 = new MediaSizeName(17);

    /**
     * ISO B7 size.
     */
    public static final MediaSizeName ISO_B7 = new MediaSizeName(18);

    /**
     * ISO B8 size.
     */
    public static final MediaSizeName ISO_B8 = new MediaSizeName(19);

    /**
     * ISO B9 size.
     */
    public static final MediaSizeName ISO_B9 = new MediaSizeName(20);

    /**
     * ISO B10 size.
     */
    public static final MediaSizeName ISO_B10 = new MediaSizeName(21);

    /**
     * JIS B0 size.
     */
    public static final MediaSizeName JIS_B0 = new MediaSizeName(22);

    /**
     * JIS B1 size.
     */
    public static final MediaSizeName JIS_B1 = new MediaSizeName(23);

    /**
     * JIS B2 size.
     */
    public static final MediaSizeName JIS_B2 = new MediaSizeName(24);

    /**
     * JIS B3 size.
     */
    public static final MediaSizeName JIS_B3 = new MediaSizeName(25);

    /**
     * JIS B4 size.
     */
    public static final MediaSizeName JIS_B4 = new MediaSizeName(26);

    /**
     * JIS B5 size.
     */
    public static final MediaSizeName JIS_B5 = new MediaSizeName(27);

    /**
     * JIS B6 size.
     */
    public static final MediaSizeName JIS_B6 = new MediaSizeName(28);

    /**
     * JIS B7 size.
     */
    public static final MediaSizeName JIS_B7 = new MediaSizeName(29);

    /**
     * JIS B8 size.
     */
    public static final MediaSizeName JIS_B8 = new MediaSizeName(30);

    /**
     * JIS B9 size.
     */
    public static final MediaSizeName JIS_B9 = new MediaSizeName(31);

    /**
     * JIS B10 size.
     */
    public static final MediaSizeName JIS_B10 = new MediaSizeName(32);

    /**
     * ISO C0 size.
     */
    public static final MediaSizeName ISO_C0 = new MediaSizeName(33);

    /**
     * ISO C1 size.
     */
    public static final MediaSizeName ISO_C1 = new MediaSizeName(34);

    /**
     * ISO C2 size.
     */
    public static final MediaSizeName ISO_C2 = new MediaSizeName(35);

    /**
     * ISO C3 size.
     */
    public static final MediaSizeName ISO_C3 = new MediaSizeName(36);

    /**
     * ISO C4 size.
     */
    public static final MediaSizeName ISO_C4 = new MediaSizeName(37);

    /**
     * ISO C5 size.
     */
    public static final MediaSizeName ISO_C5 = new MediaSizeName(38);

    /**
     * letter size.
     */
    public static final MediaSizeName ISO_C6 = new MediaSizeName(39);

    /**
     * letter size.
     */
    public static final MediaSizeName NA_LETTER = new MediaSizeName(40);

    /**
     * legal size.
     */
    public static final MediaSizeName NA_LEGAL = new MediaSizeName(41);

    /**
     * executive size.
     */
    public static final MediaSizeName EXECUTIVE = new MediaSizeName(42);

    /**
     * ledger size.
     */
    public static final MediaSizeName LEDGER = new MediaSizeName(43);

    /**
     * tabloid size.
     */
    public static final MediaSizeName TABLOID = new MediaSizeName(44);

    /**
     * invoice size.
     */
    public static final MediaSizeName INVOICE = new MediaSizeName(45);

    /**
     * folio size.
     */
    public static final MediaSizeName FOLIO = new MediaSizeName(46);

    /**
     * quarto size.
     */
    public static final MediaSizeName QUARTO = new MediaSizeName(47);

    /**
     * Japanese Postcard size.
     */
    public static final MediaSizeName
        JAPANESE_POSTCARD = new MediaSizeName(48);

    /**
     * Japanese Double Postcard size.
     */
    public static final MediaSizeName
        JAPANESE_DOUBLE_POSTCARD = new MediaSizeName(49);

    /**
     * A size.
     */
    public static final MediaSizeName A = new MediaSizeName(50);

    /**
     * B size.
     */
    public static final MediaSizeName B = new MediaSizeName(51);

    /**
     * C size.
     */
    public static final MediaSizeName C = new MediaSizeName(52);

    /**
     * D size.
     */
    public static final MediaSizeName D = new MediaSizeName(53);

    /**
     * E size.
     */
    public static final MediaSizeName E = new MediaSizeName(54);

    /**
     * ISO designated long size.
     */
    public static final MediaSizeName
        ISO_DESIGNATED_LONG = new MediaSizeName(55);

    /**
     * Italy envelope size.
     */
    public static final MediaSizeName
        ITALY_ENVELOPE = new MediaSizeName(56);  // DESIGNATED_LONG?

    /**
     * monarch envelope size.
     */
    public static final MediaSizeName
        MONARCH_ENVELOPE = new MediaSizeName(57);

    /**
     * personal envelope size.
     */
    public static final MediaSizeName
        PERSONAL_ENVELOPE = new MediaSizeName(58);

    /**
     * number 9 envelope size.
     */
    public static final MediaSizeName
        NA_NUMBER_9_ENVELOPE = new MediaSizeName(59);

    /**
     * number 10 envelope size.
     */
    public static final MediaSizeName
        NA_NUMBER_10_ENVELOPE = new MediaSizeName(60);

    /**
     * number 11 envelope size.
     */
    public static final MediaSizeName
        NA_NUMBER_11_ENVELOPE = new MediaSizeName(61);

    /**
     * number 12 envelope size.
     */
    public static final MediaSizeName
        NA_NUMBER_12_ENVELOPE = new MediaSizeName(62);

    /**
     * number 14 envelope size.
     */
    public static final MediaSizeName
        NA_NUMBER_14_ENVELOPE = new MediaSizeName(63);

    /**
     * 6x9 North American envelope size.
     */
    public static final MediaSizeName
        NA_6X9_ENVELOPE = new MediaSizeName(64);

    /**
     * 7x9 North American envelope size.
     */
    public static final MediaSizeName
        NA_7X9_ENVELOPE = new MediaSizeName(65);

    /**
     * 9x11 North American envelope size.
     */
    public static final MediaSizeName
        NA_9X11_ENVELOPE = new MediaSizeName(66);

    /**
     * 9x12 North American envelope size.
     */
    public static final MediaSizeName
        NA_9X12_ENVELOPE = new MediaSizeName(67);

    /**
     * 10x13 North American envelope size.
     */
    public static final MediaSizeName
        NA_10X13_ENVELOPE = new MediaSizeName(68);
    /**
     * 10x14North American envelope size.
     */
    public static final MediaSizeName
        NA_10X14_ENVELOPE = new MediaSizeName(69);
    /**
     * 10x15 North American envelope size.
     */
    public static final MediaSizeName
        NA_10X15_ENVELOPE = new MediaSizeName(70);

    /**
     * 5x7 North American paper.
     */
    public static final MediaSizeName
        NA_5X7 = new MediaSizeName(71);

    /**
     * 8x10 North American paper.
     */
    public static final MediaSizeName
        NA_8X10 = new MediaSizeName(72);

    /**
     * Construct a new media size enumeration value with the given integer
     * value.
     *
     * @param  value Integer value
     */
    protected MediaSizeName(int value) {
        super (value);
    }

    /**
     * The string table for class {@code MediaSizeName}.
     */
    private static final String[] myStringTable = {
                "iso-a0",
                "iso-a1",
                "iso-a2",
                "iso-a3",
                "iso-a4",
                "iso-a5",
                "iso-a6",
                "iso-a7",
                "iso-a8",
                "iso-a9",
                "iso-a10",
                "iso-b0",
                "iso-b1",
                "iso-b2",
                "iso-b3",
                "iso-b4",
                "iso-b5",
                "iso-b6",
                "iso-b7",
                "iso-b8",
                "iso-b9",
                "iso-b10",
                "jis-b0",
                "jis-b1",
                "jis-b2",
                "jis-b3",
                "jis-b4",
                "jis-b5",
                "jis-b6",
                "jis-b7",
                "jis-b8",
                "jis-b9",
                "jis-b10",
                "iso-c0",
                "iso-c1",
                "iso-c2",
                "iso-c3",
                "iso-c4",
                "iso-c5",
                "iso-c6",
                "na-letter",
                "na-legal",
                "executive",
                "ledger",
                "tabloid",
                "invoice",
                "folio",
                "quarto",
                "japanese-postcard",
                "oufuko-postcard",
                "a",
                "b",
                "c",
                "d",
                "e",
                "iso-designated-long",
                "italian-envelope",
                "monarch-envelope",
                "personal-envelope",
                "na-number-9-envelope",
                "na-number-10-envelope",
                "na-number-11-envelope",
                "na-number-12-envelope",
                "na-number-14-envelope",
                "na-6x9-envelope",
                "na-7x9-envelope",
                "na-9x11-envelope",
                "na-9x12-envelope",
                "na-10x13-envelope",
                "na-10x14-envelope",
                "na-10x15-envelope",
                "na-5x7",
                "na-8x10",
        };

    /**
     * The enumeration value table for class {@code MediaSizeName}.
     */
    private static final MediaSizeName[] myEnumValueTable = {
                ISO_A0,
                ISO_A1,
                ISO_A2,
                ISO_A3,
                ISO_A4,
                ISO_A5,
                ISO_A6,
                ISO_A7,
                ISO_A8,
                ISO_A9,
                ISO_A10,
                ISO_B0,
                ISO_B1,
                ISO_B2,
                ISO_B3,
                ISO_B4,
                ISO_B5,
                ISO_B6,
                ISO_B7,
                ISO_B8,
                ISO_B9,
                ISO_B10,
                JIS_B0,
                JIS_B1,
                JIS_B2,
                JIS_B3,
                JIS_B4,
                JIS_B5,
                JIS_B6,
                JIS_B7,
                JIS_B8,
                JIS_B9,
                JIS_B10,
                ISO_C0,
                ISO_C1,
                ISO_C2,
                ISO_C3,
                ISO_C4,
                ISO_C5,
                ISO_C6,
                NA_LETTER,
                NA_LEGAL,
                EXECUTIVE,
                LEDGER,
                TABLOID,
                INVOICE,
                FOLIO,
                QUARTO,
                JAPANESE_POSTCARD,
                JAPANESE_DOUBLE_POSTCARD,
                A,
                B,
                C,
                D,
                E,
                ISO_DESIGNATED_LONG,
                ITALY_ENVELOPE,
                MONARCH_ENVELOPE,
                PERSONAL_ENVELOPE,
                NA_NUMBER_9_ENVELOPE,
                NA_NUMBER_10_ENVELOPE,
                NA_NUMBER_11_ENVELOPE,
                NA_NUMBER_12_ENVELOPE,
                NA_NUMBER_14_ENVELOPE,
                NA_6X9_ENVELOPE,
                NA_7X9_ENVELOPE,
                NA_9X11_ENVELOPE,
                NA_9X12_ENVELOPE,
                NA_10X13_ENVELOPE,
                NA_10X14_ENVELOPE,
                NA_10X15_ENVELOPE,
                NA_5X7,
                NA_8X10,
        };

    /**
     * Returns the string table for class {@code MediaSizeName}.
     */
    protected String[] getStringTable()
    {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class {@code MediaSizeName}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }
}
