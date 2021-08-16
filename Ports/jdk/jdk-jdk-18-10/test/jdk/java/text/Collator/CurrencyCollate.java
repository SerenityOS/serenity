/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4114080 4150807
 * @summary Test currency collation.  For bug 4114080, make sure the collation
 * of the euro sign behaves properly. For bug 4150807, make sure the collation
 * order of ALL the current symbols is correct.  This means they sort
 * in English alphabetical order of their English names.  This test can be
 * easily extended to do simple spot checking.  See other collation tests for
 * more sophisticated testing.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.text.CollationKey;

/* Author: Alan Liu
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 */
public class CurrencyCollate {
    static Collator myCollation = Collator.getInstance(Locale.US);

    public static void main(String[] args) {
        String[] DATA = {
            "\u20AC", ">", "$",     // euro > dollar
            "\u20AC", "<", "\u00A3", // euro < pound

            // additional tests for general currency sort order (bug #4150807)
            "\u00a4", "<", "\u0e3f", // generic currency < baht
            "\u0e3f", "<", "\u00a2", // baht < cent
            "\u00a2", "<", "\u20a1", // cent < colon
            "\u20a1", "<", "\u20a2", // colon < cruzeiro
            "\u20a2", "<", "\u0024", // cruzeiro < dollar
            "\u0024", "<", "\u20ab", // dollar < dong
            "\u20ab", "<", "\u20a3", // dong < franc
            "\u20a3", "<", "\u20a4", // franc < lira
            "\u20a4", "<", "\u20a5", // lira < mill
            "\u20a5", "<", "\u20a6", // mill < naira
            "\u20a6", "<", "\u20a7", // naira < peseta
            "\u20a7", "<", "\u00a3", // peseta < pound
            "\u00a3", "<", "\u20a8", // pound < rupee
            "\u20a8", "<", "\u20aa", // rupee < shekel
            "\u20aa", "<", "\u20a9", // shekel < won
            "\u20a9", "<", "\u00a5"  // won < yen
        };
        for (int i=0; i<DATA.length; i+=3) {
            int expected = DATA[i+1].equals(">") ? 1 : (DATA[i+1].equals("<") ? -1 : 0);
            int actual = myCollation.compare(DATA[i], DATA[i+2]);
            if (actual != expected) {
                throw new RuntimeException("Collation of " +
                                           DATA[i] + " vs. " +
                                           DATA[i+2] + " yields " + actual +
                                           "; expected " + expected);
            }
        }
        System.out.println("Ok");
    }
}

//eof
