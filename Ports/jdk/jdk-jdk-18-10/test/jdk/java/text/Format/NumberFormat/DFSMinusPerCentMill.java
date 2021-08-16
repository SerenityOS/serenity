/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug 8220309 8230284
 * @library /java/text/testlib
 * @summary Test String representation of MinusSign/Percent/PerMill symbols.
 *          This test assumes CLDR has numbering systems for "arab" and
 *          "arabext", and their minus/percent representations include
 *          BiDi formatting control characters.
 * @run testng/othervm DFSMinusPerCentMill
 */

import java.io.*;
import java.util.*;
import java.text.*;

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DFSMinusPerCentMill {
    private enum Type {
        NUMBER, PERCENT, CURRENCY, INTEGER, COMPACT, PERMILL
    }

    private static final Locale US_ARAB = Locale.forLanguageTag("en-US-u-nu-arab");
    private static final Locale US_ARABEXT = Locale.forLanguageTag("en-US-u-nu-arabext");
    private static final double SRC_NUM = -1234.56;

    @DataProvider
    Object[][] formatData() {
        return new Object[][] {
            // Locale, FormatStyle, expected format, expected single char symbol
            {US_ARAB, Type.NUMBER, "\u061c-\u0661\u066c\u0662\u0663\u0664\u066b\u0665\u0666"},
            {US_ARAB, Type.PERCENT, "\u061c-\u0661\u0662\u0663\u066c\u0664\u0665\u0666\u066a\u061c"},
            {US_ARAB, Type.CURRENCY, "\u061c-\u0661\u066c\u0662\u0663\u0664\u066b\u0665\u0666\u00a0$"},
            {US_ARAB, Type.INTEGER, "\u061c-\u0661\u066c\u0662\u0663\u0665"},
            {US_ARAB, Type.COMPACT, "\u061c-\u0661K"},
            {US_ARAB, Type.PERMILL, "\u061c-\u0661\u0662\u0663\u0664\u0665\u0666\u0660\u0609"},

            {US_ARABEXT, Type.NUMBER, "\u200e-\u200e\u06f1\u066c\u06f2\u06f3\u06f4\u066b\u06f5\u06f6"},
            {US_ARABEXT, Type.PERCENT, "\u200e-\u200e\u06f1\u06f2\u06f3\u066c\u06f4\u06f5\u06f6\u066a"},
            {US_ARABEXT, Type.CURRENCY, "\u200e-\u200e$\u00a0\u06f1\u066c\u06f2\u06f3\u06f4\u066b\u06f5\u06f6"},
            {US_ARABEXT, Type.INTEGER, "\u200e-\u200e\u06f1\u066c\u06f2\u06f3\u06f5"},
            {US_ARABEXT, Type.COMPACT, "\u200e-\u200e\u06f1K"},
            {US_ARABEXT, Type.PERMILL, "\u200e-\u200e\u06f1\u06f2\u06f3\u06f4\u06f5\u06f6\u06f0\u0609"},
        };
    }

    @DataProvider
    Object[][] charSymbols() {
        return new Object[][]{
            // Locale, percent, per mille, minus sign
            {US_ARAB, '\u066a', '\u0609', '-'},
            {US_ARABEXT, '\u066a', '\u0609', '-'},
        };
    }

    @Test(dataProvider="formatData")
    public void testFormatData(Locale l, Type style, String expected) {
        NumberFormat nf = null;
        switch (style) {
            case NUMBER:
                nf = NumberFormat.getNumberInstance(l);
                break;
            case PERCENT:
                nf = NumberFormat.getPercentInstance(l);
                break;
            case CURRENCY:
                nf = NumberFormat.getCurrencyInstance(l);
                break;
            case INTEGER:
                nf = NumberFormat.getIntegerInstance(l);
                break;
            case COMPACT:
                nf = NumberFormat.getCompactNumberInstance(l, NumberFormat.Style.SHORT);
                break;
            case PERMILL:
                nf = new DecimalFormat("#.#\u2030", DecimalFormatSymbols.getInstance(l));
                break;
        }

        assertEquals(nf.format(SRC_NUM), expected);
    }

    @Test(dataProvider="charSymbols")
    public void testCharSymbols(Locale l, char percent, char permill, char minus) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(l);
        assertEquals(dfs.getPercent(), percent);
        assertEquals(dfs.getPerMill(), permill);
        assertEquals(dfs.getMinusSign(), minus);
    }

    @Test
    public void testSerialization() throws Exception {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new ObjectOutputStream(bos).writeObject(dfs);
        DecimalFormatSymbols dfsSerialized = (DecimalFormatSymbols)new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray())
        ).readObject();

        assertEquals(dfs, dfsSerialized);

        // set minus/percent/permille
        dfs.setMinusSign('a');
        dfs.setPercent('b');
        dfs.setPerMill('c');
        bos = new ByteArrayOutputStream();
        new ObjectOutputStream(bos).writeObject(dfs);
        dfsSerialized = (DecimalFormatSymbols)new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray())
        ).readObject();

        assertEquals(dfs, dfsSerialized);
    }
}
