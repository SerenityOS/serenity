/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import sun.util.locale.provider.CalendarDataUtility;

public class BuddhistCalendar extends GregorianCalendar {

//////////////////
// Class Variables
//////////////////

    @java.io.Serial
    private static final long serialVersionUID = -8527488697350388578L;

    private static final int BUDDHIST_YEAR_OFFSET = 543;

///////////////
// Constructors
///////////////

    /**
     * Constructs a default BuddhistCalendar using the current time
     * in the default time zone with the default locale.
     */
    public BuddhistCalendar() {
        super();
    }

    /**
     * Constructs a BuddhistCalendar based on the current time
     * in the given time zone with the default locale.
     * @param zone the given time zone.
     */
    public BuddhistCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a BuddhistCalendar based on the current time
     * in the default time zone with the given locale.
     * @param aLocale the given locale.
     */
    public BuddhistCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a BuddhistCalendar based on the current time
     * in the given time zone with the given locale.
     * @param zone the given time zone.
     * @param aLocale the given locale.
     */
    public BuddhistCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

/////////////////
// Public methods
/////////////////

    /**
     * Returns {@code "buddhist"} as the calendar type of this Calendar.
     */
    @Override
    public String getCalendarType() {
        return "buddhist";
    }

    /**
     * Compares this BuddhistCalendar to an object reference.
     * @param obj the object reference with which to compare
     * @return true if this object is equal to <code>obj</code>; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof BuddhistCalendar
            && super.equals(obj);
    }

    /**
     * Override hashCode.
     * Generates the hash code for the BuddhistCalendar object
     */
    @Override
    public int hashCode() {
        return super.hashCode() ^ BUDDHIST_YEAR_OFFSET;
    }

    /**
     * Gets the value for a given time field.
     * @param field the given time field.
     * @return the value for the given time field.
     */
    @Override
    public int get(int field)
    {
        if (field == YEAR) {
            return super.get(field) + yearOffset;
        }
        return super.get(field);
    }

    /**
     * Sets the time field with the given value.
     * @param field the given time field.
     * @param value the value to be set for the given time field.
     */
    @Override
    public void set(int field, int value)
    {
        if (field == YEAR) {
            super.set(field, value - yearOffset);
        } else {
            super.set(field, value);
        }
    }

    /**
     * Adds the specified (signed) amount of time to the given time field.
     * @param field the time field.
     * @param amount the amount of date or time to be added to the field.
     */
    @Override
    public void add(int field, int amount)
    {
        int savedYearOffset = yearOffset;
        // To let the superclass calculate date-time values correctly,
        // temporarily make this GregorianCalendar.
        yearOffset = 0;
        try {
            super.add(field, amount);
        } finally {
            yearOffset = savedYearOffset;
        }
    }

    /**
     * Add to field a signed amount without changing larger fields.
     * A negative roll amount means to subtract from field without changing
     * larger fields.
     * @param field the time field.
     * @param amount the signed amount to add to <code>field</code>.
     */
    @Override
    public void roll(int field, int amount)
    {
        int savedYearOffset = yearOffset;
        // To let the superclass calculate date-time values correctly,
        // temporarily make this GregorianCalendar.
        yearOffset = 0;
        try {
            super.roll(field, amount);
        } finally {
            yearOffset = savedYearOffset;
        }
    }

    @Override
    public String getDisplayName(int field, int style, Locale locale) {
        if (field != ERA) {
            return super.getDisplayName(field, style, locale);
        }

        return CalendarDataUtility.retrieveFieldValueName("buddhist", field, get(field), style, locale);
    }

    @Override
    public Map<String,Integer> getDisplayNames(int field, int style, Locale locale) {
        if (field != ERA) {
            return super.getDisplayNames(field, style, locale);
        }
        return CalendarDataUtility.retrieveFieldValueNames("buddhist", field, style, locale);
    }

    /**
     * Returns the maximum value that this field could have, given the
     * current date.  For example, with the date "Feb 3, 2540" and the
     * <code>DAY_OF_MONTH</code> field, the actual maximum is 28; for
     * "Feb 3, 2539" it is 29.
     *
     * @param field the field to determine the maximum of
     * @return the maximum of the given field for the current date of this Calendar
     */
    @Override
    public int getActualMaximum(int field) {
        int savedYearOffset = yearOffset;
        // To let the superclass calculate date-time values correctly,
        // temporarily make this GregorianCalendar.
        yearOffset = 0;
        try {
            return super.getActualMaximum(field);
        } finally {
            yearOffset = savedYearOffset;
        }
    }

    @Override
    @SuppressWarnings("empty-statement")
    public String toString() {
        // The super class produces a String with the Gregorian year
        // value (or '?')
        String s = super.toString();
        // If the YEAR field is UNSET, then return the Gregorian string.
        if (!isSet(YEAR)) {
            return s;
        }

        final String yearField = "YEAR=";
        int p = s.indexOf(yearField);
        // If the string doesn't include the year value for some
        // reason, then return the Gregorian string.
        if (p == -1) {
            return s;
        }
        p += yearField.length();
        StringBuilder sb = new StringBuilder(s.length() + 10);
        sb.append(s, 0, p);
        // Skip the year number
        while (Character.isDigit(s.charAt(p++)))
            ;
        int year = internalGet(YEAR) + BUDDHIST_YEAR_OFFSET;
        sb.append(year).append(s, p - 1, s.length());
        return sb.toString();
    }

    private transient int yearOffset = BUDDHIST_YEAR_OFFSET;

    @java.io.Serial
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        yearOffset = BUDDHIST_YEAR_OFFSET;
    }
}
