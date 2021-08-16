/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Locale;
import java.util.TimeZone;

/**
 * The class <code>Era</code> represents a calendar era that defines a
 * period of time in which the same year numbering is used. For
 * example, Gregorian year 2004 is <I>Heisei</I> 16 in the Japanese
 * calendar system. An era starts at any point of time (Gregorian) that is
 * represented by <code>CalendarDate</code>.
 *
 * <p><code>Era</code>s that are applicable to a particular calendar
 * system can be obtained by calling {@link CalendarSystem#getEras}
 * one of which can be used to specify a date in
 * <code>CalendarDate</code>.
 *
 * <p>The following era names are defined in this release.
 * <pre>{@code
 *   Calendar system         Era name         Since (in Gregorian)
 *   -----------------------------------------------------------------------
 *   Japanese calendar       Meiji            1868-01-01T00:00:00 local time
 *                           Taisho           1912-07-30T00:00:00 local time
 *                           Showa            1926-12-25T00:00:00 local time
 *                           Heisei           1989-01-08T00:00:00 local time
 *                           Reiwa            2019-05-01T00:00:00 local time
 *   -----------------------------------------------------------------------
 * }</pre>
 *
 * @author Masayoshi Okutsu
 * @since 1.5
 */

public final class Era {
    private final String name;
    private final String abbr;
    private final long since;
    private final CalendarDate sinceDate;
    private final boolean localTime;

    /**
     * Constructs an <code>Era</code> instance.
     *
     * @param name the era name (e.g., "BeforeCommonEra" for the Julian calendar system)
     * @param abbr the abbreviation of the era name (e.g., "B.C.E." for "BeforeCommonEra")
     * @param since the time (millisecond offset from January 1, 1970
     * (Gregorian) UTC or local time) when the era starts, inclusive.
     * @param localTime <code>true</code> if <code>since</code>
     * specifies a local time; <code>false</code> if
     * <code>since</code> specifies UTC
     */
    public Era(String name, String abbr, long since, boolean localTime) {
        this.name = name;
        this.abbr = abbr;
        this.since = since;
        this.localTime = localTime;
        Gregorian gcal = CalendarSystem.getGregorianCalendar();
        BaseCalendar.Date d = (BaseCalendar.Date) gcal.newCalendarDate(null);
        gcal.getCalendarDate(since, d);
        sinceDate = new ImmutableGregorianDate(d);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName(Locale locale) {
        return name;
    }

    public String getAbbreviation() {
        return abbr;
    }

    public String getDiaplayAbbreviation(Locale locale) {
        return abbr;
    }

    public long getSince(TimeZone zone) {
        if (zone == null || !localTime) {
            return since;
        }
        int offset = zone.getOffset(since);
        return since - offset;
    }

    public CalendarDate getSinceDate() {
        return sinceDate;
    }

    public boolean isLocalTime() {
        return localTime;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Era)) {
            return false;
        }
        Era that = (Era) o;
        return name.equals(that.name)
            && abbr.equals(that.abbr)
            && since == that.since
            && localTime == that.localTime;
    }

    private int hash = 0;

    public int hashCode() {
        if (hash == 0) {
            hash = name.hashCode() ^ abbr.hashCode() ^ (int)since ^ (int)(since >> 32)
                ^ (localTime ? 1 : 0);
        }
        return hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(getName()).append(" (");
        sb.append(getAbbreviation()).append(')');
        sb.append(" since ").append(getSinceDate());
        if (localTime) {
            sb.setLength(sb.length() - 1); // remove 'Z'
            sb.append(" local time");
        }
        sb.append(']');
        return sb.toString();
    }
}
