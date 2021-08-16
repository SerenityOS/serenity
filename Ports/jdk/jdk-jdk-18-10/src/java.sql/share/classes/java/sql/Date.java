/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.time.Instant;
import java.time.LocalDate;

/**
 * <P>A thin wrapper around a millisecond value that allows
 * JDBC to identify this as an SQL {@code DATE} value.  A
 * milliseconds value represents the number of milliseconds that
 * have passed since January 1, 1970 00:00:00.000 GMT.
 * <p>
 * To conform with the definition of SQL {@code DATE}, the
 * millisecond values wrapped by a {@code java.sql.Date} instance
 * must be 'normalized' by setting the
 * hours, minutes, seconds, and milliseconds to zero in the particular
 * time zone with which the instance is associated.
 *
 * @since 1.1
 */
public class Date extends java.util.Date {

    /**
     * Constructs a {@code Date} object initialized with the given
     * year, month, and day.
     * <P>
     * The result is undefined if a given argument is out of bounds.
     *
     * @param year the year minus 1900; must be 0 to 8099. (Note that
     *        8099 is 9999 minus 1900.)
     * @param month 0 to 11
     * @param day 1 to 31
     * @deprecated instead use the constructor {@code Date(long date)}
     */
    @Deprecated(since="1.2")
    public Date(int year, int month, int day) {
        super(year, month, day);
    }

    /**
     * Constructs a {@code Date} object using the given milliseconds
     * time value.  If the given milliseconds value contains time
     * information, the driver will set the time components to the
     * time in the default time zone (the time zone of the Java virtual
     * machine running the application) that corresponds to zero GMT.
     *
     * @param date milliseconds since January 1, 1970, 00:00:00 GMT not
     *        to exceed the milliseconds representation for the year 8099.
     *        A negative number indicates the number of milliseconds
     *        before January 1, 1970, 00:00:00 GMT.
     */
    public Date(long date) {
        // If the millisecond date value contains time info, mask it out.
        super(date);

    }

    /**
     * Sets an existing {@code Date} object
     * using the given milliseconds time value.
     * If the given milliseconds value contains time information,
     * the driver will set the time components to the
     * time in the default time zone (the time zone of the Java virtual
     * machine running the application) that corresponds to zero GMT.
     *
     * @param date milliseconds since January 1, 1970, 00:00:00 GMT not
     *        to exceed the milliseconds representation for the year 8099.
     *        A negative number indicates the number of milliseconds
     *        before January 1, 1970, 00:00:00 GMT.
     */
    public void setTime(long date) {
        // If the millisecond date value contains time info, mask it out.
        super.setTime(date);
    }

    /**
     * Converts a string in JDBC date escape format to
     * a {@code Date} value.
     *
     * @param s a {@code String} object representing a date in
     *        in the format "yyyy-[m]m-[d]d". The leading zero for {@code mm}
     * and {@code dd} may also be omitted.
     * @return a {@code java.sql.Date} object representing the
     *         given date
     * @throws IllegalArgumentException if the date given is not in the
     *         JDBC date escape format (yyyy-[m]m-[d]d)
     */
    public static Date valueOf(String s) {
        if (s == null) {
            throw new java.lang.IllegalArgumentException();
        }
        final int YEAR_LENGTH = 4;
        final int MONTH_LENGTH = 2;
        final int DAY_LENGTH = 2;
        final int MAX_MONTH = 12;
        final int MAX_DAY = 31;
        Date d = null;

        int firstDash = s.indexOf('-');
        int secondDash = s.indexOf('-', firstDash + 1);
        int len = s.length();

        if ((firstDash > 0) && (secondDash > 0) && (secondDash < len - 1)) {
            if (firstDash == YEAR_LENGTH &&
                    (secondDash - firstDash > 1 && secondDash - firstDash <= MONTH_LENGTH + 1) &&
                    (len - secondDash > 1 && len - secondDash <= DAY_LENGTH + 1)) {
                int year = Integer.parseInt(s, 0, firstDash, 10);
                int month = Integer.parseInt(s, firstDash + 1, secondDash, 10);
                int day = Integer.parseInt(s, secondDash + 1, len, 10);

                if ((month >= 1 && month <= MAX_MONTH) && (day >= 1 && day <= MAX_DAY)) {
                    d = new Date(year - 1900, month - 1, day);
                }
            }
        }
        if (d == null) {
            throw new java.lang.IllegalArgumentException();
        }

        return d;

    }


    /**
     * Formats a date in the date escape format yyyy-mm-dd.
     *
     * @return a String in yyyy-mm-dd format
     */
    @SuppressWarnings("deprecation")
    public String toString () {
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int day = super.getDate();

        char buf[] = new char[10];
        formatDecimalInt(year, buf, 0, 4);
        buf[4] = '-';
        Date.formatDecimalInt(month, buf, 5, 2);
        buf[7] = '-';
        Date.formatDecimalInt(day, buf, 8, 2);

        return new String(buf);
    }

    /**
     * Formats an unsigned integer into a char array in decimal output format.
     * Numbers will be zero-padded or truncated if the string representation
     * of the integer is smaller than or exceeds len, respectively.
     *
     * Should consider moving this to Integer and expose it through
     * JavaLangAccess similar to Integer::formatUnsignedInt
     * @param val  Value to convert
     * @param buf  Array containing converted value
     * @param offset Starting pos in buf
     * @param len  length of output value
     */
    static void formatDecimalInt(int val, char[] buf, int offset, int len) {
        int charPos = offset + len;
        do {
            buf[--charPos] = (char)('0' + (val % 10));
            val /= 10;
        } while (charPos > offset);
    }

    // Override all the time operations inherited from java.util.Date;

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #setHours
    */
    @Deprecated(since="1.2")
    public int getHours() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #setMinutes
    */
    @Deprecated(since="1.2")
    public int getMinutes() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #setSeconds
    */
    @Deprecated(since="1.2")
    public int getSeconds() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #getHours
    */
    @Deprecated(since="1.2")
    public void setHours(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #getMinutes
    */
    @Deprecated(since="1.2")
    public void setMinutes(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL Date
    * values do not have a time component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this method is invoked
    * @see #getSeconds
    */
    @Deprecated(since="1.2")
    public void setSeconds(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * Private serial version unique ID to ensure serialization
    * compatibility.
    */
    static final long serialVersionUID = 1511598038487230103L;

    /**
     * Obtains an instance of {@code Date} from a {@link LocalDate} object
     * with the same year, month and day of month value as the given
     * {@code LocalDate}.
     * <p>
     * The provided {@code LocalDate} is interpreted as the local date
     * in the local time zone.
     *
     * @param date a {@code LocalDate} to convert
     * @return a {@code Date} object
     * @throws NullPointerException if {@code date} is null
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public static Date valueOf(LocalDate date) {
        return new Date(date.getYear() - 1900, date.getMonthValue() -1,
                        date.getDayOfMonth());
    }

    /**
     * Creates a {@code LocalDate} instance using the year, month and day
     * from this {@code Date} object.
     * @return a {@code LocalDate} object representing the same date value
     *
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public LocalDate toLocalDate() {
        return LocalDate.of(getYear() + 1900, getMonth() + 1, getDate());
    }

   /**
    * This method always throws an UnsupportedOperationException and should
    * not be used because SQL {@code Date} values do not have a time
    * component.
    *
    * @throws java.lang.UnsupportedOperationException if this method is invoked
    */
    @Override
    public Instant toInstant() {
        throw new java.lang.UnsupportedOperationException();
    }
}
