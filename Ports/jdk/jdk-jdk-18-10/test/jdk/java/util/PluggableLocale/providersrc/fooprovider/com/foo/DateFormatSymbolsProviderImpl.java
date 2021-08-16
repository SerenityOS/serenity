/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package com.foo;

import java.text.*;
import java.text.spi.*;
import java.util.*;

import com.foobar.Utils;

public class DateFormatSymbolsProviderImpl extends DateFormatSymbolsProvider {

    static Locale[] avail = {
        new Locale("ja", "JP", "osaka"),
        new Locale("ja", "JP", "kyoto"),
        Locale.JAPAN,
        new Locale("yy", "ZZ")
    };
    static List<Locale> availList = Arrays.asList(avail);

    static String[] dialect = {
        "\u3084\u3002",
        "\u3069\u3059\u3002",
        "\u3067\u3059\u3002",
        "-yy-ZZ"
    };

    static Map<Locale, FooDateFormatSymbols> symbols = new HashMap<Locale, FooDateFormatSymbols>(4);

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public DateFormatSymbols getInstance(Locale locale) {
        if (!Utils.supportsLocale(availList, locale)) {
            throw new IllegalArgumentException("locale is not supported: "+locale);
        }

        FooDateFormatSymbols fdfs = symbols.get(locale);
        if (fdfs == null) {
            for (int index = 0; index < avail.length; index ++) {
                if (Utils.supportsLocale(avail[index], locale)) {
                    fdfs = new FooDateFormatSymbols(index);
                    symbols.put(locale, fdfs);
                    break;
                }
            }
        }
        return fdfs;
    }

    class FooDateFormatSymbols extends DateFormatSymbols {
        String dialect = "";

        String[] eras = null;
        String[] months = null;
        String[] shortMonths = null;
        String[] weekdays = null;
        String[] shortWeekdays = null;
        String[] ampms = null;

        public FooDateFormatSymbols(int index) {
            super(DateFormatSymbolsProviderImpl.this.avail[index]);
            dialect = DateFormatSymbolsProviderImpl.this.dialect[index];
        }

        public String[] getEras() {
            if (eras == null) {
                eras = super.getEras();
                for (int i = 0; i < eras.length; i++) {
                    eras[i] = eras[i]+dialect;
                }
            }
            return eras;
        }

        /**
         * Sets era strings. For example: "AD" and "BC".
         * @param newEras the new era strings.
         */
        public void setEras(String[] newEras) {
            eras = newEras;
        }

        /**
         * Gets month strings. For example: "January", "February", etc.
         * @return the month strings.
         */
        public String[] getMonths() {
            if (months == null) {
                months = super.getMonths();
                for (int i = 0; i < months.length; i++) {
                    months[i] = months[i]+dialect;
                }
            }
            return months;
        }

        /**
         * Sets month strings. For example: "January", "February", etc.
         * @param newMonths the new month strings.
         */
        public void setMonths(String[] newMonths) {
            months = newMonths;
        }

        /**
         * Gets short month strings. For example: "Jan", "Feb", etc.
         * @return the short month strings.
         */
        public String[] getShortMonths() {
            if (shortMonths == null) {
                shortMonths = super.getShortMonths();
                for (int i = 0; i < shortMonths.length; i++) {
                    shortMonths[i] = shortMonths[i]+dialect;
                }
            }
            return shortMonths;
        }

        /**
         * Sets short month strings. For example: "Jan", "Feb", etc.
         * @param newShortMonths the new short month strings.
         */
        public void setShortMonths(String[] newShortMonths) {
            shortMonths = newShortMonths;
        }

        /**
         * Gets weekday strings. For example: "Sunday", "Monday", etc.
         * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
         * <code>Calendar.MONDAY</code>, etc. to index the result array.
         */
        public String[] getWeekdays() {
            if (weekdays == null) {
                weekdays = super.getWeekdays();
                for (int i = 0; i < weekdays.length; i++) {
                    weekdays[i] = weekdays[i]+dialect;
                }
            }
            return weekdays;
        }

        /**
         * Sets weekday strings. For example: "Sunday", "Monday", etc.
         * @param newWeekdays the new weekday strings. The array should
         * be indexed by <code>Calendar.SUNDAY</code>,
         * <code>Calendar.MONDAY</code>, etc.
         */
        public void setWeekdays(String[] newWeekdays) {
            weekdays = newWeekdays;
        }

        /**
         * Gets short weekday strings. For example: "Sun", "Mon", etc.
         * @return the short weekday strings. Use <code>Calendar.SUNDAY</code>,
         * <code>Calendar.MONDAY</code>, etc. to index the result array.
         */
        public String[] getShortWeekdays() {
            if (shortWeekdays == null) {
                shortWeekdays = super.getShortWeekdays();
                for (int i = 0; i < shortWeekdays.length; i++) {
                    shortWeekdays[i] = shortWeekdays[i]+dialect;
                }
            }
            return shortWeekdays;
        }

        /**
         * Sets short weekday strings. For example: "Sun", "Mon", etc.
         * @param newShortWeekdays the new short weekday strings. The array should
         * be indexed by <code>Calendar.SUNDAY</code>,
         * <code>Calendar.MONDAY</code>, etc.
         */
        public void setShortWeekdays(String[] newShortWeekdays) {
            shortWeekdays = newShortWeekdays;
        }

        /**
         * Gets ampm strings. For example: "AM" and "PM".
         * @return the ampm strings.
         */
        public String[] getAmPmStrings() {
            if (ampms == null) {
                ampms = super.getAmPmStrings();
                for (int i = 0; i < ampms.length; i++) {
                    ampms[i] = ampms[i]+dialect;
                }
            }
            return ampms;
        }

        /**
         * Sets ampm strings. For example: "AM" and "PM".
         * @param newAmpms the new ampm strings.
         */
        public void setAmPmStrings(String[] newAmpms) {
            ampms = newAmpms;
        }

        @Override
        public String[][] getZoneStrings() {
            return new String[0][0];
        }
    }
}
