/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7151414 4745761
 * @summary Unit test for calendar types
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;

public class CalendarTypeTest {

    // Calendar types supported in JRE
    static Locale[] locales = new Locale[] {
        Locale.US,
        Locale.forLanguageTag("th-TH-u-ca-gregory"),
        new Locale("th", "TH"),
        Locale.forLanguageTag("en-US-u-ca-buddhist"),
        new Locale("ja", "JP", "JP"),
        Locale.forLanguageTag("en-US-u-ca-japanese")};
    static final String[] TYPES = new String[] {
        "gregory",
        "buddhist",
        "japanese"};
    static final String[] ALIASES = new String[] {
        "gregorian",
        "iso8601"};

    public static void main(String[] args) {
        for (int i = 0; i < locales.length; i++) {
            Calendar cal = Calendar.getInstance(locales[i]);
            String type = cal.getCalendarType();
            checkValue("bad calendar type", type, TYPES[i / 2]);
        }

        GregorianCalendar gcal = new GregorianCalendar();
        checkValue("bad GregorianCalendar type", gcal.getCalendarType(), "gregory");

        Gregorian g = new Gregorian();
        checkValue("bad GregorianCalendar subclass type", g.getCalendarType(), "gregory");

        Calendar k = new Koyomi();
        checkValue("bad class name", k.getCalendarType(), k.getClass().getName());

        Set<String> types = Calendar.getAvailableCalendarTypes();
        if (types.size() != 3) {
            throw new RuntimeException("size != 3");
        }
        for (String s : TYPES) {
            if (!types.contains(s)) {
                throw new RuntimeException(s + " not contained");
            }
        }
        for (String s : ALIASES) {
            if (types.contains(s)) {
                throw new RuntimeException("alias " + s + " contained");
            }
        }
    }

    private static void checkValue(String msg, String got, String expected) {
        if (!expected.equals(got)) {
            String str = String.format("%s: got '%s', expected '%s'", msg, got, expected);
            throw new RuntimeException(str);
        }
    }

    @SuppressWarnings("serial")
    private static class Gregorian extends GregorianCalendar {
    }

    @SuppressWarnings("serial")
    private static class Koyomi extends Calendar {

        @Override
        protected void computeTime() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected void computeFields() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void add(int field, int amount) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void roll(int field, boolean up) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMinimum(int field) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getMaximum(int field) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getGreatestMinimum(int field) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getLeastMaximum(int field) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
