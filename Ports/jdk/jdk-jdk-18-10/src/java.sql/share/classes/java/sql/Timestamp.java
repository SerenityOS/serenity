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
import java.time.LocalDateTime;

/**
 * <P>A thin wrapper around {@code java.util.Date} that allows
 * the JDBC API to identify this as an SQL {@code TIMESTAMP} value.
 * It adds the ability
 * to hold the SQL {@code TIMESTAMP} fractional seconds value, by allowing
 * the specification of fractional seconds to a precision of nanoseconds.
 * A Timestamp also provides formatting and
 * parsing operations to support the JDBC escape syntax for timestamp values.
 *
 * <p>The precision of a Timestamp object is calculated to be either:
 * <ul>
 * <li>{@code 19 }, which is the number of characters in yyyy-mm-dd hh:mm:ss
 * <li> {@code  20 + s }, which is the number
 * of characters in the yyyy-mm-dd hh:mm:ss.[fff...] and {@code s} represents  the scale of the given Timestamp,
 * its fractional seconds precision.
 *</ul>
 *
 * <P><B>Note:</B> This type is a composite of a {@code java.util.Date} and a
 * separate nanoseconds value. Only integral seconds are stored in the
 * {@code java.util.Date} component. The fractional seconds - the nanos - are
 * separate.  The {@code Timestamp.equals(Object)} method never returns
 * {@code true} when passed an object
 * that isn't an instance of {@code java.sql.Timestamp},
 * because the nanos component of a date is unknown.
 * As a result, the {@code Timestamp.equals(Object)}
 * method is not symmetric with respect to the
 * {@code java.util.Date.equals(Object)}
 * method.  Also, the {@code hashCode} method uses the underlying
 * {@code java.util.Date}
 * implementation and therefore does not include nanos in its computation.
 * <P>
 * Due to the differences between the {@code Timestamp} class
 * and the {@code java.util.Date}
 * class mentioned above, it is recommended that code not view
 * {@code Timestamp} values generically as an instance of
 * {@code java.util.Date}.  The
 * inheritance relationship between {@code Timestamp}
 * and {@code java.util.Date} really
 * denotes implementation inheritance, and not type inheritance.
 *
 * @since 1.1
 */
public class Timestamp extends java.util.Date {

    /**
     * Constructs a {@code Timestamp} object initialized
     * with the given values.
     *
     * @param year the year minus 1900
     * @param month 0 to 11
     * @param date 1 to 31
     * @param hour 0 to 23
     * @param minute 0 to 59
     * @param second 0 to 59
     * @param nano 0 to 999,999,999
     * @deprecated instead use the constructor {@code Timestamp(long millis)}
     * @throws IllegalArgumentException if the nano argument is out of bounds
     */
    @Deprecated(since="1.2")
    public Timestamp(int year, int month, int date,
                     int hour, int minute, int second, int nano) {
        super(year, month, date, hour, minute, second);
        if (nano > 999999999 || nano < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        nanos = nano;
    }

    /**
     * Constructs a {@code Timestamp} object
     * using a milliseconds time value. The
     * integral seconds are stored in the underlying date value; the
     * fractional seconds are stored in the {@code nanos} field of
     * the {@code Timestamp} object.
     *
     * @param time milliseconds since January 1, 1970, 00:00:00 GMT.
     *        A negative number is the number of milliseconds before
     *         January 1, 1970, 00:00:00 GMT.
     * @see java.util.Calendar
     */
    public Timestamp(long time) {
        super((time/1000)*1000);
        nanos = (int)((time%1000) * 1000000);
        if (nanos < 0) {
            nanos = 1000000000 + nanos;
            super.setTime(((time/1000)-1)*1000);
        }
    }

    /**
     * Sets this {@code Timestamp} object to represent a point in time that is
     * {@code time} milliseconds after January 1, 1970 00:00:00 GMT.
     *
     * @param time   the number of milliseconds.
     * @see #getTime
     * @see #Timestamp(long time)
     * @see java.util.Calendar
     */
    public void setTime(long time) {
        super.setTime((time/1000)*1000);
        nanos = (int)((time%1000) * 1000000);
        if (nanos < 0) {
            nanos = 1000000000 + nanos;
            super.setTime(((time/1000)-1)*1000);
        }
    }

    /**
     * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
     * represented by this {@code Timestamp} object.
     *
     * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
     *          represented by this date.
     * @see #setTime
     */
    public long getTime() {
        long time = super.getTime();
        return (time + (nanos / 1000000));
    }


    /**
     * @serial
     */
    private int nanos;

    /**
     * Converts a {@code String} object in JDBC timestamp escape format to a
     * {@code Timestamp} value.
     *
     * @param s timestamp in format {@code yyyy-[m]m-[d]d hh:mm:ss[.f...]}.  The
     * fractional seconds may be omitted. The leading zero for {@code mm}
     * and {@code dd} may also be omitted.
     *
     * @return corresponding {@code Timestamp} value
     * @throws java.lang.IllegalArgumentException if the given argument
     * does not have the format {@code yyyy-[m]m-[d]d hh:mm:ss[.f...]}
     */
    public static Timestamp valueOf(String s) {
        final int YEAR_LENGTH = 4;
        final int MONTH_LENGTH = 2;
        final int DAY_LENGTH = 2;
        final int MAX_MONTH = 12;
        final int MAX_DAY = 31;
        int year = 0;
        int month = 0;
        int day = 0;
        int hour;
        int minute;
        int second;
        int a_nanos = 0;
        int firstDash;
        int secondDash;
        int dividingSpace;
        int firstColon;
        int secondColon;
        int period;
        String formatError = "Timestamp format must be yyyy-mm-dd hh:mm:ss[.fffffffff]";

        if (s == null) throw new java.lang.IllegalArgumentException("null string");

        // Split the string into date and time components
        s = s.trim();
        dividingSpace = s.indexOf(' ');
        if (dividingSpace < 0) {
            throw new java.lang.IllegalArgumentException(formatError);
        }

        // Parse the date
        firstDash = s.indexOf('-');
        secondDash = s.indexOf('-', firstDash+1);

        // Parse the time
        firstColon = s.indexOf(':', dividingSpace + 1);
        secondColon = s.indexOf(':', firstColon + 1);
        period = s.indexOf('.', secondColon + 1);

        // Convert the date
        boolean parsedDate = false;
        if (firstDash > 0 && secondDash > 0 && secondDash < dividingSpace - 1) {
            if (firstDash == YEAR_LENGTH &&
                    (secondDash - firstDash > 1 && secondDash - firstDash <= MONTH_LENGTH + 1) &&
                    (dividingSpace - secondDash > 1 && dividingSpace - secondDash <= DAY_LENGTH + 1)) {
                 year = Integer.parseInt(s, 0, firstDash, 10);
                 month = Integer.parseInt(s, firstDash + 1, secondDash, 10);
                 day = Integer.parseInt(s, secondDash + 1, dividingSpace, 10);

                if ((month >= 1 && month <= MAX_MONTH) && (day >= 1 && day <= MAX_DAY)) {
                    parsedDate = true;
                }
            }
        }
        if (! parsedDate) {
            throw new java.lang.IllegalArgumentException(formatError);
        }

        // Convert the time; default missing nanos
        int len = s.length();
        if (firstColon > 0 && secondColon > 0 && secondColon < len - 1) {
            hour = Integer.parseInt(s, dividingSpace + 1, firstColon, 10);
            minute = Integer.parseInt(s, firstColon + 1, secondColon, 10);
            if (period > 0 && period < len - 1) {
                second = Integer.parseInt(s, secondColon + 1, period, 10);
                int nanoPrecision = len - (period + 1);
                if (nanoPrecision > 9)
                    throw new java.lang.IllegalArgumentException(formatError);
                if (!Character.isDigit(s.charAt(period + 1)))
                    throw new java.lang.IllegalArgumentException(formatError);
                int tmpNanos = Integer.parseInt(s, period + 1, len, 10);
                while (nanoPrecision < 9) {
                    tmpNanos *= 10;
                    nanoPrecision++;
                }
                a_nanos = tmpNanos;
            } else if (period > 0) {
                throw new java.lang.IllegalArgumentException(formatError);
            } else {
                second = Integer.parseInt(s, secondColon + 1, len, 10);
            }
        } else {
            throw new java.lang.IllegalArgumentException(formatError);
        }

        return new Timestamp(year - 1900, month - 1, day, hour, minute, second, a_nanos);
    }

    /**
     * Formats a timestamp in JDBC timestamp escape format.
     *         {@code yyyy-mm-dd hh:mm:ss.fffffffff},
     * where {@code fffffffff} indicates nanoseconds.
     *
     * @return a {@code String} object in
     *           {@code yyyy-mm-dd hh:mm:ss.fffffffff} format
     */
    @SuppressWarnings("deprecation")
    public String toString() {
        int year = super.getYear() + 1900;
        int month = super.getMonth() + 1;
        int day = super.getDate();
        int hour = super.getHours();
        int minute = super.getMinutes();
        int second = super.getSeconds();

        int trailingZeros = 0;
        int tmpNanos = nanos;
        if (tmpNanos == 0) {
            trailingZeros = 8;
        } else {
            while (tmpNanos % 10 == 0) {
                tmpNanos /= 10;
                trailingZeros++;
            }
        }

        // 8058429: To comply with current JCK tests, we need to deal with year
        // being any number between 0 and 292278995
        int count = 10000;
        int yearSize = 4;
        do {
            if (year < count) {
                break;
            }
            yearSize++;
            count *= 10;
        } while (count < 1000000000);

        char[] buf = new char[25 + yearSize - trailingZeros];
        Date.formatDecimalInt(year, buf, 0, yearSize);
        buf[yearSize] = '-';
        Date.formatDecimalInt(month, buf, yearSize + 1, 2);
        buf[yearSize + 3] = '-';
        Date.formatDecimalInt(day, buf, yearSize + 4, 2);
        buf[yearSize + 6] = ' ';
        Date.formatDecimalInt(hour, buf, yearSize + 7, 2);
        buf[yearSize + 9] = ':';
        Date.formatDecimalInt(minute, buf, yearSize + 10, 2);
        buf[yearSize + 12] = ':';
        Date.formatDecimalInt(second, buf, yearSize + 13, 2);
        buf[yearSize + 15] = '.';
        Date.formatDecimalInt(tmpNanos, buf, yearSize + 16, 9 - trailingZeros);

        return new String(buf);
    }

    /**
     * Gets this {@code Timestamp} object's {@code nanos} value.
     *
     * @return this {@code Timestamp} object's fractional seconds component
     * @see #setNanos
     */
    public int getNanos() {
        return nanos;
    }

    /**
     * Sets this {@code Timestamp} object's {@code nanos} field
     * to the given value.
     *
     * @param n the new fractional seconds component
     * @throws java.lang.IllegalArgumentException if the given argument
     *         is greater than 999999999 or less than 0
     * @see #getNanos
     */
    public void setNanos(int n) {
        if (n > 999999999 || n < 0) {
            throw new IllegalArgumentException("nanos > 999999999 or < 0");
        }
        nanos = n;
    }

    /**
     * Tests to see if this {@code Timestamp} object is
     * equal to the given {@code Timestamp} object.
     *
     * @param ts the {@code Timestamp} value to compare with
     * @return {@code true} if the given {@code Timestamp}
     *         object is equal to this {@code Timestamp} object;
     *         {@code false} otherwise
     */
    public boolean equals(Timestamp ts) {
        if (super.equals(ts)) {
            if  (nanos == ts.nanos) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Tests to see if this {@code Timestamp} object is
     * equal to the given object.
     *
     * This version of the method {@code equals} has been added
     * to fix the incorrect
     * signature of {@code Timestamp.equals(Timestamp)} and to preserve backward
     * compatibility with existing class files.
     *
     * Note: This method is not symmetric with respect to the
     * {@code equals(Object)} method in the base class.
     *
     * @param ts the {@code Object} value to compare with
     * @return {@code true} if the given {@code Object} is an instance
     *         of a {@code Timestamp} that
     *         is equal to this {@code Timestamp} object;
     *         {@code false} otherwise
     */
    public boolean equals(java.lang.Object ts) {
      if (ts instanceof Timestamp) {
        return this.equals((Timestamp)ts);
      } else {
        return false;
      }
    }

    /**
     * Indicates whether this {@code Timestamp} object is
     * earlier than the given {@code Timestamp} object.
     *
     * @param ts the {@code Timestamp} value to compare with
     * @return {@code true} if this {@code Timestamp} object is earlier;
     *        {@code false} otherwise
     */
    public boolean before(Timestamp ts) {
        return compareTo(ts) < 0;
    }

    /**
     * Indicates whether this {@code Timestamp} object is
     * later than the given {@code Timestamp} object.
     *
     * @param ts the {@code Timestamp} value to compare with
     * @return {@code true} if this {@code Timestamp} object is later;
     *        {@code false} otherwise
     */
    public boolean after(Timestamp ts) {
        return compareTo(ts) > 0;
    }

    /**
     * Compares this {@code Timestamp} object to the given
     * {@code Timestamp} object.
     *
     * @param   ts   the {@code Timestamp} object to be compared to
     *                this {@code Timestamp} object
     * @return  the value {@code 0} if the two {@code Timestamp}
     *          objects are equal; a value less than {@code 0} if this
     *          {@code Timestamp} object is before the given argument;
     *          and a value greater than {@code 0} if this
     *          {@code Timestamp} object is after the given argument.
     * @since   1.4
     */
    public int compareTo(Timestamp ts) {
        long thisTime = this.getTime();
        long anotherTime = ts.getTime();
        int i = (thisTime<anotherTime ? -1 :(thisTime==anotherTime?0 :1));
        if (i == 0) {
            if (nanos > ts.nanos) {
                    return 1;
            } else if (nanos < ts.nanos) {
                return -1;
            }
        }
        return i;
    }

    /**
     * Compares this {@code Timestamp} object to the given
     * {@code Date} object.
     *
     * @param o the {@code Date} to be compared to
     *          this {@code Timestamp} object
     * @return  the value {@code 0} if this {@code Timestamp} object
     *          and the given object are equal; a value less than {@code 0}
     *          if this  {@code Timestamp} object is before the given argument;
     *          and a value greater than {@code 0} if this
     *          {@code Timestamp} object is after the given argument.
     *
     * @since   1.5
     */
    public int compareTo(java.util.Date o) {
       if(o instanceof Timestamp) {
            // When Timestamp instance compare it with a Timestamp
            // Hence it is basically calling this.compareTo((Timestamp))o);
            // Note typecasting is safe because o is instance of Timestamp
           return compareTo((Timestamp)o);
      } else {
            // When Date doing a o.compareTo(this)
            // will give wrong results.
          Timestamp ts = new Timestamp(o.getTime());
          return this.compareTo(ts);
      }
    }

    /**
     * {@inheritDoc}
     *
     * The {@code hashCode} method uses the underlying {@code java.util.Date}
     * implementation and therefore does not include nanos in its computation.
     *
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    static final long serialVersionUID = 2745179027874758501L;

    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * Obtains an instance of {@code Timestamp} from a {@code LocalDateTime}
     * object, with the same year, month, day of month, hours, minutes,
     * seconds and nanos date-time value as the provided {@code LocalDateTime}.
     * <p>
     * The provided {@code LocalDateTime} is interpreted as the local
     * date-time in the local time zone.
     *
     * @param dateTime a {@code LocalDateTime} to convert
     * @return a {@code Timestamp} object
     * @throws NullPointerException if {@code dateTime} is null.
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public static Timestamp valueOf(LocalDateTime dateTime) {
        return new Timestamp(dateTime.getYear() - 1900,
                             dateTime.getMonthValue() - 1,
                             dateTime.getDayOfMonth(),
                             dateTime.getHour(),
                             dateTime.getMinute(),
                             dateTime.getSecond(),
                             dateTime.getNano());
    }

    /**
     * Converts this {@code Timestamp} object to a {@code LocalDateTime}.
     * <p>
     * The conversion creates a {@code LocalDateTime} that represents the
     * same year, month, day of month, hours, minutes, seconds and nanos
     * date-time value as this {@code Timestamp} in the local time zone.
     *
     * @return a {@code LocalDateTime} object representing the same date-time value
     * @since 1.8
     */
    @SuppressWarnings("deprecation")
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.of(getYear() + 1900,
                                getMonth() + 1,
                                getDate(),
                                getHours(),
                                getMinutes(),
                                getSeconds(),
                                getNanos());
    }

    /**
     * Obtains an instance of {@code Timestamp} from an {@link Instant} object.
     * <p>
     * {@code Instant} can store points on the time-line further in the future
     * and further in the past than {@code Date}. In this scenario, this method
     * will throw an exception.
     *
     * @param instant  the instant to convert
     * @return an {@code Timestamp} representing the same point on the time-line as
     *  the provided instant
     * @throws NullPointerException if {@code instant} is null.
     * @throws IllegalArgumentException if the instant is too large to
     *  represent as a {@code Timestamp}
     * @since 1.8
     */
    public static Timestamp from(Instant instant) {
        try {
            Timestamp stamp = new Timestamp(instant.getEpochSecond() * MILLIS_PER_SECOND);
            stamp.nanos = instant.getNano();
            return stamp;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Converts this {@code Timestamp} object to an {@code Instant}.
     * <p>
     * The conversion creates an {@code Instant} that represents the same
     * point on the time-line as this {@code Timestamp}.
     *
     * @return an instant representing the same point on the time-line
     * @since 1.8
     */
    @Override
    public Instant toInstant() {
        return Instant.ofEpochSecond(super.getTime() / MILLIS_PER_SECOND, nanos);
    }
}
