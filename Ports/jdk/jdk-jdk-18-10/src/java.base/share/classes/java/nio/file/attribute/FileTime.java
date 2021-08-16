/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents the value of a file's time stamp attribute. For example, it may
 * represent the time that the file was last
 * {@link BasicFileAttributes#lastModifiedTime() modified},
 * {@link BasicFileAttributes#lastAccessTime() accessed},
 * or {@link BasicFileAttributes#creationTime() created}.
 *
 * <p> Instances of this class are immutable.
 *
 * @since 1.7
 * @see java.nio.file.Files#setLastModifiedTime
 * @see java.nio.file.Files#getLastModifiedTime
 */

public final class FileTime
    implements Comparable<FileTime>
{
    /**
     * The unit of granularity to interpret the value. Null if
     * this {@code FileTime} is converted from an {@code Instant},
     * the {@code value} and {@code unit} pair will not be used
     * in this scenario.
     */
    private final TimeUnit unit;

    /**
     * The value since the epoch; can be negative.
     */
    private final long value;

    /**
     * The value as Instant (created lazily, if not from an instant)
     */
    private Instant instant;

    /**
     * The value return by toString (created lazily)
     */
    private String valueAsString;

    /**
     * Initializes a new instance of this class.
     */
    private FileTime(long value, TimeUnit unit, Instant instant) {
        this.value = value;
        this.unit = unit;
        this.instant = instant;
    }

    /**
     * Returns a {@code FileTime} representing a value at the given unit of
     * granularity.
     *
     * @param   value
     *          the value since the epoch (1970-01-01T00:00:00Z); can be
     *          negative
     * @param   unit
     *          the unit of granularity to interpret the value
     *
     * @return  a {@code FileTime} representing the given value
     */
    public static FileTime from(long value, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return new FileTime(value, unit, null);
    }

    /**
     * Returns a {@code FileTime} representing the given value in milliseconds.
     *
     * @param   value
     *          the value, in milliseconds, since the epoch
     *          (1970-01-01T00:00:00Z); can be negative
     *
     * @return  a {@code FileTime} representing the given value
     */
    public static FileTime fromMillis(long value) {
        return new FileTime(value, TimeUnit.MILLISECONDS, null);
    }

    /**
     * Returns a {@code FileTime} representing the same point of time value
     * on the time-line as the provided {@code Instant} object.
     *
     * @param   instant
     *          the instant to convert
     * @return  a {@code FileTime} representing the same point on the time-line
     *          as the provided instant
     * @since 1.8
     */
    public static FileTime from(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return new FileTime(0, null, instant);
    }

    /**
     * Returns the value at the given unit of granularity.
     *
     * <p> Conversion from a coarser granularity that would numerically overflow
     * saturate to {@code Long.MIN_VALUE} if negative or {@code Long.MAX_VALUE}
     * if positive.
     *
     * @param   unit
     *          the unit of granularity for the return value
     *
     * @return  value in the given unit of granularity, since the epoch
     *          since the epoch (1970-01-01T00:00:00Z); can be negative
     */
    public long to(TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        if (this.unit != null) {
            return unit.convert(this.value, this.unit);
        } else {
            long secs = unit.convert(instant.getEpochSecond(), TimeUnit.SECONDS);
            if (secs == Long.MIN_VALUE || secs == Long.MAX_VALUE) {
                return secs;
            }
            long nanos = unit.convert(instant.getNano(), TimeUnit.NANOSECONDS);
            long r = secs + nanos;
            // Math.addExact() variant
            if (((secs ^ r) & (nanos ^ r)) < 0) {
                return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
            }
            return r;
        }
    }

    /**
     * Returns the value in milliseconds.
     *
     * <p> Conversion from a coarser granularity that would numerically overflow
     * saturate to {@code Long.MIN_VALUE} if negative or {@code Long.MAX_VALUE}
     * if positive.
     *
     * @return  the value in milliseconds, since the epoch (1970-01-01T00:00:00Z)
     */
    public long toMillis() {
        if (unit != null) {
            return unit.toMillis(value);
        } else {
            long secs = instant.getEpochSecond();
            int  nanos = instant.getNano();
            // Math.multiplyExact() variant
            long r = secs * 1000;
            long ax = Math.abs(secs);
            if (((ax | 1000) >>> 31 != 0)) {
                if ((r / 1000) != secs) {
                    return (secs < 0) ? Long.MIN_VALUE : Long.MAX_VALUE;
                }
            }
            return r + nanos / 1000_000;
        }
    }

    /**
     * Time unit constants for conversion.
     */
    private static final long HOURS_PER_DAY      = 24L;
    private static final long MINUTES_PER_HOUR   = 60L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR   = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    private static final long SECONDS_PER_DAY    = SECONDS_PER_HOUR * HOURS_PER_DAY;
    private static final long MILLIS_PER_SECOND  = 1000L;
    private static final long MICROS_PER_SECOND  = 1000_000L;
    private static final long NANOS_PER_SECOND   = 1000_000_000L;
    private static final int  NANOS_PER_MILLI    = 1000_000;
    private static final int  NANOS_PER_MICRO    = 1000;
    // The epoch second of Instant.MIN.
    private static final long MIN_SECOND = -31557014167219200L;
    // The epoch second of Instant.MAX.
    private static final long MAX_SECOND = 31556889864403199L;

    /*
     * Scale d by m, checking for overflow.
     */
    private static long scale(long d, long m, long over) {
        if (d >  over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }

    /**
     * Converts this {@code FileTime} object to an {@code Instant}.
     *
     * <p> The conversion creates an {@code Instant} that represents the
     * same point on the time-line as this {@code FileTime}.
     *
     * <p> {@code FileTime} can store points on the time-line further in the
     * future and further in the past than {@code Instant}. Conversion
     * from such further time points saturates to {@link Instant#MIN} if
     * earlier than {@code Instant.MIN} or {@link Instant#MAX} if later
     * than {@code Instant.MAX}.
     *
     * @return  an instant representing the same point on the time-line as
     *          this {@code FileTime} object
     * @since 1.8
     */
    public Instant toInstant() {
        if (instant == null) {
            long secs = 0L;
            int nanos = 0;
            switch (unit) {
                case DAYS:
                    secs = scale(value, SECONDS_PER_DAY,
                                 Long.MAX_VALUE/SECONDS_PER_DAY);
                    break;
                case HOURS:
                    secs = scale(value, SECONDS_PER_HOUR,
                                 Long.MAX_VALUE/SECONDS_PER_HOUR);
                    break;
                case MINUTES:
                    secs = scale(value, SECONDS_PER_MINUTE,
                                 Long.MAX_VALUE/SECONDS_PER_MINUTE);
                    break;
                case SECONDS:
                    secs = value;
                    break;
                case MILLISECONDS:
                    secs = Math.floorDiv(value, MILLIS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, MILLIS_PER_SECOND)
                            * NANOS_PER_MILLI;
                    break;
                case MICROSECONDS:
                    secs = Math.floorDiv(value, MICROS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, MICROS_PER_SECOND)
                            * NANOS_PER_MICRO;
                    break;
                case NANOSECONDS:
                    secs = Math.floorDiv(value, NANOS_PER_SECOND);
                    nanos = (int)Math.floorMod(value, NANOS_PER_SECOND);
                    break;
                default : throw new AssertionError("Unit not handled");
            }
            if (secs <= MIN_SECOND)
                instant = Instant.MIN;
            else if (secs >= MAX_SECOND)
                instant = Instant.MAX;
            else
                instant = Instant.ofEpochSecond(secs, nanos);
        }
        return instant;
    }

    /**
     * Tests this {@code FileTime} for equality with the given object.
     *
     * <p> The result is {@code true} if and only if the argument is not {@code
     * null} and is a {@code FileTime} that represents the same time. This
     * method satisfies the general contract of the {@code Object.equals} method.
     *
     * @param   obj
     *          the object to compare with
     *
     * @return  {@code true} if, and only if, the given object is a {@code
     *          FileTime} that represents the same time
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FileTime) ? compareTo((FileTime)obj) == 0 : false;
    }

    /**
     * Computes a hash code for this file time.
     *
     * <p> The hash code is based upon the value represented, and satisfies the
     * general contract of the {@link Object#hashCode} method.
     *
     * @return  the hash-code value
     */
    @Override
    public int hashCode() {
        // hashcode of instant representation to satisfy contract with equals
        return toInstant().hashCode();
    }

    private long toDays() {
        if (unit != null) {
            return unit.toDays(value);
        } else {
            return TimeUnit.SECONDS.toDays(toInstant().getEpochSecond());
        }
    }

    private long toExcessNanos(long days) {
        if (unit != null) {
            return unit.toNanos(value - unit.convert(days, TimeUnit.DAYS));
        } else {
            return TimeUnit.SECONDS.toNanos(toInstant().getEpochSecond()
                                            - TimeUnit.DAYS.toSeconds(days));
        }
    }

    /**
     * Compares the value of two {@code FileTime} objects for order.
     *
     * @param   other
     *          the other {@code FileTime} to be compared
     *
     * @return  {@code 0} if this {@code FileTime} is equal to {@code other}, a
     *          value less than 0 if this {@code FileTime} represents a time
     *          that is before {@code other}, and a value greater than 0 if this
     *          {@code FileTime} represents a time that is after {@code other}
     */
    @Override
    public int compareTo(FileTime other) {
        // same granularity
        if (unit != null && unit == other.unit) {
            return Long.compare(value, other.value);
        } else {
            // compare using instant representation when unit differs
            long secs = toInstant().getEpochSecond();
            long secsOther = other.toInstant().getEpochSecond();
            int cmp = Long.compare(secs, secsOther);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Long.compare(toInstant().getNano(), other.toInstant().getNano());
            if (cmp != 0) {
                return cmp;
            }
            if (secs != MAX_SECOND && secs != MIN_SECOND) {
                return 0;
            }
            // if both this and other's Instant reps are MIN/MAX,
            // use daysSinceEpoch and nanosOfDays, which will not
            // saturate during calculation.
            long days = toDays();
            long daysOther = other.toDays();
            if (days == daysOther) {
                return Long.compare(toExcessNanos(days), other.toExcessNanos(daysOther));
            }
            return Long.compare(days, daysOther);
        }
    }

    // days in a 400 year cycle = 146097
    // days in a 10,000 year cycle = 146097 * 25
    // seconds per day = 86400
    private static final long DAYS_PER_10000_YEARS = 146097L * 25L;
    private static final long SECONDS_PER_10000_YEARS = 146097L * 25L * 86400L;
    private static final long SECONDS_0000_TO_1970 = ((146097L * 5L) - (30L * 365L + 7L)) * 86400L;

    // append year/month/day/hour/minute/second/nano with width and 0 padding
    private StringBuilder append(StringBuilder sb, int w, int d) {
        while (w > 0) {
            sb.append((char)(d/w + '0'));
            d = d % w;
            w /= 10;
        }
        return sb;
    }

    /**
     * Returns the string representation of this {@code FileTime}. The string
     * is returned in the <a
     * href="http://www.w3.org/TR/NOTE-datetime">ISO&nbsp;8601</a> format:
     * <pre>
     *     YYYY-MM-DDThh:mm:ss[.s+]Z
     * </pre>
     * where "{@code [.s+]}" represents a dot followed by one of more digits
     * for the decimal fraction of a second. It is only present when the decimal
     * fraction of a second is not zero. For example, {@code
     * FileTime.fromMillis(1234567890000L).toString()} yields {@code
     * "2009-02-13T23:31:30Z"}, and {@code FileTime.fromMillis(1234567890123L).toString()}
     * yields {@code "2009-02-13T23:31:30.123Z"}.
     *
     * <p> A {@code FileTime} is primarily intended to represent the value of a
     * file's time stamp. Where used to represent <i>extreme values</i>, where
     * the year is less than "{@code 0001}" or greater than "{@code 9999}" then
     * this method deviates from ISO 8601 in the same manner as the
     * <a href="http://www.w3.org/TR/xmlschema-2/#deviantformats">XML Schema
     * language</a>. That is, the year may be expanded to more than four digits
     * and may be negative-signed. If more than four digits then leading zeros
     * are not present. The year before "{@code 0001}" is "{@code -0001}".
     *
     * @return  the string representation of this file time
     */
    @Override
    public String toString() {
        if (valueAsString == null) {
            long secs = 0L;
            int  nanos = 0;
            if (instant == null && unit.compareTo(TimeUnit.SECONDS) >= 0) {
                secs = unit.toSeconds(value);
            } else {
                secs = toInstant().getEpochSecond();
                nanos = toInstant().getNano();
            }
            LocalDateTime ldt;
            int year = 0;
            if (secs >= -SECONDS_0000_TO_1970) {
                // current era
                long zeroSecs = secs - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
                long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
                long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
                ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
                year = ldt.getYear() +  (int)hi * 10000;
            } else {
                // before current era
                long zeroSecs = secs + SECONDS_0000_TO_1970;
                long hi = zeroSecs / SECONDS_PER_10000_YEARS;
                long lo = zeroSecs % SECONDS_PER_10000_YEARS;
                ldt = LocalDateTime.ofEpochSecond(lo - SECONDS_0000_TO_1970, nanos, ZoneOffset.UTC);
                year = ldt.getYear() + (int)hi * 10000;
            }
            if (year <= 0) {
                year = year - 1;
            }
            int fraction = ldt.getNano();
            StringBuilder sb = new StringBuilder(64);
            sb.append(year < 0 ? "-" : "");
            year = Math.abs(year);
            if (year < 10000) {
                append(sb, 1000, Math.abs(year));
            } else {
                sb.append(String.valueOf(year));
            }
            sb.append('-');
            append(sb, 10, ldt.getMonthValue());
            sb.append('-');
            append(sb, 10, ldt.getDayOfMonth());
            sb.append('T');
            append(sb, 10, ldt.getHour());
            sb.append(':');
            append(sb, 10, ldt.getMinute());
            sb.append(':');
            append(sb, 10, ldt.getSecond());
            if (fraction != 0) {
                sb.append('.');
                // adding leading zeros and stripping any trailing zeros
                int w = 100_000_000;
                while (fraction % 10 == 0) {
                    fraction /= 10;
                    w /= 10;
                }
                append(sb, w, fraction);
            }
            sb.append('Z');
            valueAsString = sb.toString();
        }
        return valueAsString;
    }
}
