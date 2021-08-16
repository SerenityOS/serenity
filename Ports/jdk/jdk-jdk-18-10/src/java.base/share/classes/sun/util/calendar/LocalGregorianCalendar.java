/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.calendar;

import java.security.AccessController;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.security.action.GetPropertyAction;

/**
 *
 * @author Masayoshi Okutsu
 * @since 1.6
 */

public class LocalGregorianCalendar extends BaseCalendar {
    private static final Era[] JAPANESE_ERAS = {
        new Era("Meiji",  "M", -3218832000000L, true),
        new Era("Taisho", "T", -1812153600000L, true),
        new Era("Showa",  "S", -1357603200000L, true),
        new Era("Heisei", "H",   600220800000L, true),
        new Era("Reiwa",  "R",  1556668800000L, true),
    };

    private static boolean isValidEra(Era newEra, Era[] eras) {
        Era last = eras[eras.length - 1];
        if (last.getSince(null) >= newEra.getSince(null)) {
            return false;
        }
        // The new era name should be unique. Its abbr may not.
        String newName = newEra.getName();
        for (Era era : eras) {
            if (era.getName().equals(newName)) {
                return false;
            }
        }
        return true;
    }

    private String name;
    private Era[] eras;

    public static class Date extends BaseCalendar.Date {

        protected Date() {
            super();
        }

        protected Date(TimeZone zone) {
            super(zone);
        }

        private int gregorianYear = FIELD_UNDEFINED;

        @Override
        public Date setEra(Era era) {
            if (getEra() != era) {
                super.setEra(era);
                gregorianYear = FIELD_UNDEFINED;
            }
            return this;
        }

        @Override
        public Date addYear(int localYear) {
            super.addYear(localYear);
            gregorianYear += localYear;
            return this;
        }

        @Override
        public Date setYear(int localYear) {
            if (getYear() != localYear) {
                super.setYear(localYear);
                gregorianYear = FIELD_UNDEFINED;
            }
            return this;
        }

        @Override
        public int getNormalizedYear() {
            return gregorianYear;
        }

        @Override
        public void setNormalizedYear(int normalizedYear) {
            this.gregorianYear = normalizedYear;
        }

        void setLocalEra(Era era) {
            super.setEra(era);
        }

        void setLocalYear(int year) {
            super.setYear(year);
        }

        @Override
        public String toString() {
            String time = super.toString();
            time = time.substring(time.indexOf('T'));
            StringBuffer sb = new StringBuffer();
            Era era = getEra();
            if (era != null) {
                String abbr = era.getAbbreviation();
                if (abbr != null) {
                    sb.append(abbr);
                }
            }
            sb.append(getYear()).append('.');
            CalendarUtils.sprintf0d(sb, getMonth(), 2).append('.');
            CalendarUtils.sprintf0d(sb, getDayOfMonth(), 2);
            sb.append(time);
            return sb.toString();
        }
    }

    static LocalGregorianCalendar getLocalGregorianCalendar(String name) {
        // Only the Japanese calendar is supported.
        if (!"japanese".equals(name)) {
            return null;
        }

        // Append an era to the predefined eras if it's given by the property.
        String prop = GetPropertyAction
                .privilegedGetProperty("jdk.calendar.japanese.supplemental.era");
        if (prop != null) {
            Era era = parseEraEntry(prop);
            if (era != null) {
                if (isValidEra(era, JAPANESE_ERAS)) {
                    int length = JAPANESE_ERAS.length;
                    Era[] eras = new Era[length + 1];
                    System.arraycopy(JAPANESE_ERAS, 0, eras, 0, length);
                    eras[length] = era;
                    return new LocalGregorianCalendar(name, eras);
                }
            }
        }
        return new LocalGregorianCalendar(name, JAPANESE_ERAS);
    }

    private static Era parseEraEntry(String entry) {
        String[] keyValuePairs = entry.split(",");
        String eraName = null;
        boolean localTime = true;
        long since = 0;
        String abbr = null;

        for (String item : keyValuePairs) {
            String[] keyvalue = item.split("=");
            if (keyvalue.length != 2) {
                return null;
            }
            String key = keyvalue[0].trim();
            String value = convertUnicodeEscape(keyvalue[1].trim());
            switch (key) {
            case "name":
                eraName = value;
                break;
            case "since":
                if (value.endsWith("u")) {
                    localTime = false;
                    value = value.substring(0, value.length() - 1);
                }
                try {
                    since = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return null;
                }
                break;
            case "abbr":
                abbr = value;
                break;
            default:
                return null;
            }
        }
        if (eraName == null || eraName.isEmpty()
                || abbr == null || abbr.isEmpty()) {
            return null;
        }
        return new Era(eraName, abbr, since, localTime);
    }

    private static String convertUnicodeEscape(String src) {
        Matcher m = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(src);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb,
                Character.toString((char)Integer.parseUnsignedInt(m.group(1), 16)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private LocalGregorianCalendar(String name, Era[] eras) {
        this.name = name;
        this.eras = eras;
        setEras(eras);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Date getCalendarDate() {
        return getCalendarDate(System.currentTimeMillis(), newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long millis) {
        return getCalendarDate(millis, newCalendarDate());
    }

    @Override
    public Date getCalendarDate(long millis, TimeZone zone) {
        return getCalendarDate(millis, newCalendarDate(zone));
    }

    @Override
    public Date getCalendarDate(long millis, CalendarDate date) {
        Date ldate = (Date) super.getCalendarDate(millis, date);
        return adjustYear(ldate, millis, ldate.getZoneOffset());
    }

    private Date adjustYear(Date ldate, long millis, int zoneOffset) {
        int i;
        for (i = eras.length - 1; i >= 0; --i) {
            Era era = eras[i];
            long since = era.getSince(null);
            if (era.isLocalTime()) {
                since -= zoneOffset;
            }
            if (millis >= since) {
                ldate.setLocalEra(era);
                int y = ldate.getNormalizedYear() - era.getSinceDate().getYear() + 1;
                ldate.setLocalYear(y);
                break;
            }
        }
        if (i < 0) {
            ldate.setLocalEra(null);
            ldate.setLocalYear(ldate.getNormalizedYear());
        }
        ldate.setNormalized(true);
        return ldate;
    }

    @Override
    public Date newCalendarDate() {
        return new Date();
    }

    @Override
    public Date newCalendarDate(TimeZone zone) {
        return new Date(zone);
    }

    @Override
    public boolean validate(CalendarDate date) {
        Date ldate = (Date) date;
        Era era = ldate.getEra();
        if (era != null) {
            if (!validateEra(era)) {
                return false;
            }
            ldate.setNormalizedYear(era.getSinceDate().getYear() + ldate.getYear() - 1);
            Date tmp = newCalendarDate(date.getZone());
            tmp.setEra(era).setDate(date.getYear(), date.getMonth(), date.getDayOfMonth());
            normalize(tmp);
            if (tmp.getEra() != era) {
                return false;
            }
        } else {
            if (date.getYear() >= eras[0].getSinceDate().getYear()) {
                return false;
            }
            ldate.setNormalizedYear(ldate.getYear());
        }
        return super.validate(ldate);
    }

    private boolean validateEra(Era era) {
        for (Era era1 : eras) {
            if (era == era1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean normalize(CalendarDate date) {
        if (date.isNormalized()) {
            return true;
        }

        normalizeYear(date);
        Date ldate = (Date) date;

        // Normalize it as a Gregorian date and get its millisecond value
        super.normalize(ldate);

        boolean hasMillis = false;
        long millis = 0;
        int year = ldate.getNormalizedYear();
        int i;
        Era era = null;
        for (i = eras.length - 1; i >= 0; --i) {
            era = eras[i];
            if (era.isLocalTime()) {
                CalendarDate sinceDate = era.getSinceDate();
                int sinceYear = sinceDate.getYear();
                if (year > sinceYear) {
                    break;
                }
                if (year == sinceYear) {
                    int month = ldate.getMonth();
                    int sinceMonth = sinceDate.getMonth();
                    if (month > sinceMonth) {
                        break;
                    }
                    if (month == sinceMonth) {
                        int day = ldate.getDayOfMonth();
                        int sinceDay = sinceDate.getDayOfMonth();
                        if (day > sinceDay) {
                            break;
                        }
                        if (day == sinceDay) {
                            long timeOfDay = ldate.getTimeOfDay();
                            long sinceTimeOfDay = sinceDate.getTimeOfDay();
                            if (timeOfDay >= sinceTimeOfDay) {
                                break;
                            }
                            --i;
                            break;
                        }
                    }
                }
            } else {
                if (!hasMillis) {
                    millis  = super.getTime(date);
                    hasMillis = true;
                }

                long since = era.getSince(date.getZone());
                if (millis >= since) {
                    break;
                }
            }
        }
        if (i >= 0) {
            ldate.setLocalEra(era);
            @SuppressWarnings("null")
            int y = ldate.getNormalizedYear() - era.getSinceDate().getYear() + 1;
            ldate.setLocalYear(y);
        } else {
            // Set Gregorian year with no era
            ldate.setEra(null);
            ldate.setLocalYear(year);
            ldate.setNormalizedYear(year);
        }
        ldate.setNormalized(true);
        return true;
    }

    @Override
    void normalizeMonth(CalendarDate date) {
        normalizeYear(date);
        super.normalizeMonth(date);
    }

    void normalizeYear(CalendarDate date) {
        Date ldate = (Date) date;
        // Set the supposed-to-be-correct Gregorian year first
        // e.g., Showa 90 becomes 2015 (1926 + 90 - 1).
        Era era = ldate.getEra();
        if (era == null || !validateEra(era)) {
            ldate.setNormalizedYear(ldate.getYear());
        } else {
            ldate.setNormalizedYear(era.getSinceDate().getYear() + ldate.getYear() - 1);
        }
    }

    /**
     * Returns whether the specified Gregorian year is a leap year.
     * @see #isLeapYear(Era, int)
     */
    @Override
    public boolean isLeapYear(int gregorianYear) {
        return CalendarUtils.isGregorianLeapYear(gregorianYear);
    }

    public boolean isLeapYear(Era era, int year) {
        if (era == null) {
            return isLeapYear(year);
        }
        int gyear = era.getSinceDate().getYear() + year - 1;
        return isLeapYear(gyear);
    }

    @Override
    public void getCalendarDateFromFixedDate(CalendarDate date, long fixedDate) {
        Date ldate = (Date) date;
        super.getCalendarDateFromFixedDate(ldate, fixedDate);
        adjustYear(ldate, (fixedDate - EPOCH_OFFSET) * DAY_IN_MILLIS, 0);
    }
}
