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
import java.time.LocalTime;

/**
 * <P>A thin wrapper around the {@code java.util.Date} class that allows the JDBC
 * API to identify this as an SQL {@code TIME} value. The {@code Time}
 * class adds formatting and
 * parsing operations to support the JDBC escape syntax for time
 * values.
 * <p>The date components should be set to the "zero epoch"
 * value of January 1, 1970 and should not be accessed.
 *
 * @since 1.1
 */
public class Time extends java.util.Date {

    /**
     * Constructs a {@code Time} object initialized with the
     * given values for the hour, minute, and second.
     * The driver sets the date components to January 1, 1970.
     * Any method that attempts to access the date components of a
     * {@code Time} object will throw a
     * {@code java.lang.IllegalArgumentException}.
     * <P>
     * The result is undefined if a given argument is out of bounds.
     *
     * @param hour 0 to 23
     * @param minute 0 to 59
     * @param second 0 to 59
     *
     * @deprecated Use the constructor that takes a milliseconds value
     *             in place of this constructor
     */
    @Deprecated(since="1.2")
    public Time(int hour, int minute, int second) {
        super(70, 0, 1, hour, minute, second);
    }

    /**
     * Constructs a {@code Time} object using a milliseconds time value.
     *
     * @param time milliseconds since January 1, 1970, 00:00:00 GMT;
     *             a negative number is milliseconds before
     *               January 1, 1970, 00:00:00 GMT
     */
    public Time(long time) {
        super(time);
    }

    /**
     * Sets a {@code Time} object using a milliseconds time value.
     *
     * @param time milliseconds since January 1, 1970, 00:00:00 GMT;
     *             a negative number is milliseconds before
     *               January 1, 1970, 00:00:00 GMT
     */
    public void setTime(long time) {
        super.setTime(time);
    }

    /**
     * Converts a string in JDBC time escape format to a {@code Time} value.
     *
     * @param s time in format "hh:mm:ss"
     * @return a corresponding {@code Time} object
     */
    public static Time valueOf(String s) {
        if (s == null) throw new java.lang.IllegalArgumentException();

        int hour;
        int minute;
        int second;
        int firstColon = s.indexOf(':');
        int secondColon = s.indexOf(':', firstColon + 1);
        int len = s.length();
        if (firstColon > 0 && secondColon > 0 &&
                secondColon < len - 1) {
            hour = Integer.parseInt(s, 0, firstColon, 10);
            minute = Integer.parseInt(s, firstColon + 1, secondColon, 10);
            second = Integer.parseInt(s, secondColon + 1, len, 10);
        } else {
            throw new java.lang.IllegalArgumentException();
        }

        return new Time(hour, minute, second);
    }

    /**
     * Formats a time in JDBC time escape format.
     *
     * @return a {@code String} in hh:mm:ss format
     */
    @SuppressWarnings("deprecation")
    public String toString () {
        int hour = super.getHours();
        int minute = super.getMinutes();
        int second = super.getSeconds();

        char[] buf = new char[8];
        Date.formatDecimalInt(hour, buf, 0, 2);
        buf[2] = ':';
        Date.formatDecimalInt(minute, buf, 3, 2);
        buf[5] = ':';
        Date.formatDecimalInt(second, buf, 6, 2);

        return new String(buf);
    }

    // Override all the date operations inherited from java.util.Date;

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a year component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #setYear
    */
    @Deprecated(since="1.2")
    public int getYear() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a month component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #setMonth
    */
    @Deprecated(since="1.2")
    public int getMonth() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a day component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    */
    @Deprecated(since="1.2")
    public int getDay() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a date component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #setDate
    */
    @Deprecated(since="1.2")
    public int getDate() {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a year component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #getYear
    */
    @Deprecated(since="1.2")
    public void setYear(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a month component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #getMonth
    */
    @Deprecated(since="1.2")
    public void setMonth(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * This method is deprecated and should not be used because SQL {@code TIME}
    * values do not have a date component.
    *
    * @deprecated
    * @throws java.lang.IllegalArgumentException if this
    *         method is invoked
    * @see #getDate
    */
    @Deprecated(since="1.2")
    public void setDate(int i) {
        throw new java.lang.IllegalArgumentException();
    }

   /**
    * Private serial version unique ID to ensure serialization
    * compatibility.
    */
    static final long serialVersionUID = 8397324403548013681L;

    /**
     * Obtains an instance of {@code Time} from a {@link LocalTime} object
     * with the same hour, minute and second time value as the given
     * {@code LocalTime}. The nanosecond field from {@code LocalTime} is
     * not part of the newly created {@code Time} object.
     *
     * @param time a {@code LocalTime} to convert
     * @return a {@code Time} object
     * @throws NullPointerException if {@code time} is null
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public static Time valueOf(LocalTime time) {
        return new Time(time.getHour(), time.getMinute(), time.getSecond());
    }

    /**
     * Converts this {@code Time} object to a {@code LocalTime}.
     * <p>
     * The conversion creates a {@code LocalTime} that represents the same
     * hour, minute, and second time value as this {@code Time}. The
     * nanosecond {@code LocalTime} field will be set to zero.
     *
     * @return a {@code LocalTime} object representing the same time value
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public LocalTime toLocalTime() {
        return LocalTime.of(getHours(), getMinutes(), getSeconds());
    }

   /**
    * This method always throws an UnsupportedOperationException and should
    * not be used because SQL {@code Time} values do not have a date
    * component.
    *
    * @throws java.lang.UnsupportedOperationException if this method is invoked
    */
    @Override
    public Instant toInstant() {
        throw new java.lang.UnsupportedOperationException();
    }
}
