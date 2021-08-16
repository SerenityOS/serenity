/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6215962
 * @summary Confirm that replacing Utility.arayEquals methods have with
 * Arrays.equals introduces no problem.
 */
import java.text.*;
import java.util.*;

public class Bug6215962 {

    public static void main(String[] args) {
        testMessageFormat();
        testChoiceFormat();
        testDateFormatSymbols();
    }

    /**
     * Test cases for MessageFormat
     */
    static void testMessageFormat() {
        MessageFormat mf1 = new MessageFormat("{0}", null);
        MessageFormat mf2 = new MessageFormat("{0}", null);
        check(mf1, mf2, true);

        mf1.setLocale(null);
        check(mf1, mf2, true);

        mf1 = new MessageFormat("{0}", Locale.US);
        check(mf1, mf2, false);

        mf2 = new MessageFormat("{0}", Locale.JAPAN);
        check(mf1, mf2, false);

        mf1 = new MessageFormat("{0}", new Locale("ja", "JP"));
        check(mf1, mf2, true);

        mf1.setLocale(null);
        check(mf1, mf2, false);

        mf1 = new MessageFormat("{0}", new Locale("ja", "JP", "FOO"));
        check(mf1, mf2, false);

        mf2 = new MessageFormat("{1}", new Locale("ja", "JP", "FOO"));
        check(mf1, mf2, false);

        mf1 = new MessageFormat("{1}", new Locale("ja", "JP", "FOO"));
        check(mf1, mf2, true);

        mf1 = new MessageFormat("{1, date}", new Locale("ja", "JP", "FOO"));
        check(mf1, mf2, false);

        mf2 = new MessageFormat("{1, date}", new Locale("ja", "JP", "FOO"));
        check(mf1, mf2, true);
    }

    static void check(MessageFormat f1, MessageFormat f2, boolean expected) {
        boolean got = f1.equals(f2);
        if (got != expected) {
            throw new RuntimeException("Test failed for MessageFormat.equals(). Got: " + got + ", Expected: " + expected);
        }
    }

    /**
     * Test cases for MessageFormat
     */
    static void testChoiceFormat() {
        double[] limits0 = {0,1,2,3,4,5,6};
        double[] limits1 = {1,2,3,4,5,6,7};
        String[] monthNames0 = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        String[] monthNames1 = {"Sun","Mon","Tue","Wed","Thur","Fri","Sat"};

        ChoiceFormat cf1 = new ChoiceFormat(limits1, monthNames0);
        ChoiceFormat cf2 = new ChoiceFormat(limits1, monthNames0);
        check(cf1, cf2, true);

        cf2 = new ChoiceFormat(limits0, monthNames0);
        check(cf1, cf2, false);

        cf2 = new ChoiceFormat(limits1, monthNames1);
        check(cf1, cf2, false);
    }

    static void check(ChoiceFormat f1, ChoiceFormat f2, boolean expected) {
        boolean got = f1.equals(f2);
        if (got != expected) {
            throw new RuntimeException("Test failed for ChoiceFormat.equals(). Got: " + got + ", Expected: " + expected);
        }
    }

    /**
     * Test cases for DateFormatSymbols
     */
    static void testDateFormatSymbols() {
        DateFormatSymbols dfs1 = new DateFormatSymbols();
        DateFormatSymbols dfs2 = new DateFormatSymbols();
        check(dfs1, dfs2, true);

        // Becase eras, months, shortmonths, weekdays, shortweekdays, ampms are
        // the same data type (String[]) and are treated in the same way, here
        // I test only Months.
        String[] tmp = dfs1.getMonths();
        String saved = tmp[0];
        tmp[0] = "Foo";
        dfs1.setMonths(tmp);
        check(dfs1, dfs2, false);

        tmp[0] = saved;
        dfs1.setMonths(tmp);
        check(dfs1, dfs2, true);

        // Test LocalizedpatternChars (String)
        String pattern = dfs2.getLocalPatternChars();
        dfs2.setLocalPatternChars("Bar");
        check(dfs1, dfs2, false);

        dfs2.setLocalPatternChars(pattern);
        check(dfs1, dfs2, true);

        // Test TimeZone strings (String[][])
        String[][] zones = dfs1.getZoneStrings();
        saved = zones[0][1];
        zones[0][1] = "Yokohama Summer Time";
        dfs1.setZoneStrings(zones);
        check(dfs1, dfs2, false);

        zones[0][1] = saved;
        dfs1.setZoneStrings(zones);
        check(dfs1, dfs2, true);
    }

    static void check(DateFormatSymbols dfs1, DateFormatSymbols dfs2, boolean expected) {
        boolean got = dfs1.equals(dfs2);
        if (got != expected) {
            throw new RuntimeException("Test failed for DateFormatSymbols.equals(). Got: " + got + ", Expected: " + expected);
        }
    }
}
