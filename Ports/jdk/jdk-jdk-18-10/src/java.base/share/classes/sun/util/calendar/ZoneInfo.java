/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * <code>ZoneInfo</code> is an implementation subclass of {@link
 * java.util.TimeZone TimeZone} that represents GMT offsets and
 * daylight saving time transitions of a time zone.
 * <p>
 * The daylight saving time transitions are described in the {@link
 * #transitions transitions} table consisting of a chronological
 * sequence of transitions of GMT offset and/or daylight saving time
 * changes. Since all transitions are represented in UTC, in theory,
 * <code>ZoneInfo</code> can be used with any calendar systems except
 * for the {@link #getOffset(int,int,int,int,int,int) getOffset}
 * method that takes Gregorian calendar date fields.
 * <p>
 * This table covers transitions from 1900 until 2037 (as of version
 * 1.4), Before 1900, it assumes that there was no daylight saving
 * time and the <code>getOffset</code> methods always return the
 * {@link #getRawOffset} value. No Local Mean Time is supported. If a
 * specified date is beyond the transition table and this time zone is
 * supposed to observe daylight saving time in 2037, it delegates
 * operations to a {@link java.util.SimpleTimeZone SimpleTimeZone}
 * object created using the daylight saving time schedule as of 2037.
 * <p>
 * The date items, transitions, GMT offset(s), etc. are read from a database
 * file. See {@link ZoneInfoFile} for details.
 * @see java.util.SimpleTimeZone
 * @since 1.4
 */

public class ZoneInfo extends TimeZone {

    private static final int UTC_TIME = 0;
    private static final int STANDARD_TIME = 1;
    private static final int WALL_TIME = 2;

    private static final long OFFSET_MASK = 0x0fL;
    private static final long DST_MASK = 0xf0L;
    private static final int DST_NSHIFT = 4;
    // this bit field is reserved for abbreviation support
    private static final long ABBR_MASK = 0xf00L;
    private static final int TRANSITION_NSHIFT = 12;

    /**
     * The raw GMT offset in milliseconds between this zone and GMT.
     * Negative offsets are to the west of Greenwich.  To obtain local
     * <em>standard</em> time, add the offset to GMT time.
     * @serial
     */
    private int rawOffset;

    /**
     * Difference in milliseconds from the original GMT offset in case
     * the raw offset value has been modified by calling {@link
     * #setRawOffset}. The initial value is 0.
     * @serial
     */
    private int rawOffsetDiff = 0;

    /**
     * A CRC32 value of all pairs of transition time (in milliseconds
     * in <code>long</code>) in local time and its GMT offset (in
     * seconds in <code>int</code>) in the chronological order. Byte
     * values of each <code>long</code> and <code>int</code> are taken
     * in the big endian order (i.e., MSB to LSB).
     * @serial
     */
    private int checksum;

    /**
     * The amount of time in milliseconds saved during daylight saving
     * time. If <code>useDaylight</code> is false, this value is 0.
     * @serial
     */
    private int dstSavings;

    /**
     * This array describes transitions of GMT offsets of this time
     * zone, including both raw offset changes and daylight saving
     * time changes.
     * A long integer consists of four bit fields.
     * <ul>
     * <li>The most significant 52-bit field represents transition
     * time in milliseconds from Gregorian January 1 1970, 00:00:00
     * GMT.</li>
     * <li>The next 4-bit field is reserved and must be 0.</li>
     * <li>The next 4-bit field is an index value to {@link #offsets
     * offsets[]} for the amount of daylight saving at the
     * transition. If this value is zero, it means that no daylight
     * saving, not the index value zero.</li>
     * <li>The least significant 4-bit field is an index value to
     * {@link #offsets offsets[]} for <em>total</em> GMT offset at the
     * transition.</li>
     * </ul>
     * If this time zone doesn't observe daylight saving time and has
     * never changed any GMT offsets in the past, this value is null.
     * @serial
     */
    private long[] transitions;

    /**
     * This array holds all unique offset values in
     * milliseconds. Index values to this array are stored in the
     * transitions array elements.
     * @serial
     */
    private int[] offsets;

    /**
     * SimpleTimeZone parameter values. It has to have either 8 for
     * {@link java.util.SimpleTimeZone#SimpleTimeZone(int, String,
     * int, int , int , int , int , int , int , int , int) the
     * 11-argument SimpleTimeZone constructor} or 10 for {@link
     * java.util.SimpleTimeZone#SimpleTimeZone(int, String, int, int,
     * int , int , int , int , int , int , int, int, int) the
     * 13-argument SimpleTimeZone constructor} parameters.
     * @serial
     */
    private int[] simpleTimeZoneParams;

    /**
     * True if the raw GMT offset value would change after the time
     * zone data has been generated; false, otherwise. The default
     * value is false.
     * @serial
     */
    private boolean willGMTOffsetChange = false;

    /**
     * True if the object has been modified after its instantiation.
     */
    private transient boolean dirty = false;

    @java.io.Serial
    private static final long serialVersionUID = 2653134537216586139L;

    /**
     * A constructor.
     */
    public ZoneInfo() {
    }

    /**
     * A Constructor for CustomID.
     */
    public ZoneInfo(String ID, int rawOffset) {
        this(ID, rawOffset, 0, 0, null, null, null, false);
    }

    /**
     * Constructs a ZoneInfo instance.
     *
     * @param ID time zone name
     * @param rawOffset GMT offset in milliseconds
     * @param dstSavings daylight saving value in milliseconds or 0
     * (zero) if this time zone doesn't observe Daylight Saving Time.
     * @param checksum CRC32 value with all transitions table entry
     * values
     * @param transitions transition table
     * @param offsets offset value table
     * @param simpleTimeZoneParams parameter values for constructing
     * SimpleTimeZone
     * @param willGMTOffsetChange the value of willGMTOffsetChange
     */
    ZoneInfo(String ID,
             int rawOffset,
             int dstSavings,
             int checksum,
             long[] transitions,
             int[] offsets,
             int[] simpleTimeZoneParams,
             boolean willGMTOffsetChange) {
        setID(ID);
        this.rawOffset = rawOffset;
        this.dstSavings = dstSavings;
        this.checksum = checksum;
        this.transitions = transitions;
        this.offsets = offsets;
        this.simpleTimeZoneParams = simpleTimeZoneParams;
        this.willGMTOffsetChange = willGMTOffsetChange;
    }

    /**
     * Returns the difference in milliseconds between local time and UTC
     * of given time, taking into account both the raw offset and the
     * effect of daylight savings.
     *
     * @param date the milliseconds in UTC
     * @return the milliseconds to add to UTC to get local wall time
     */
    public int getOffset(long date) {
        return getOffsets(date, null, UTC_TIME);
    }

    public int getOffsets(long utc, int[] offsets) {
        return getOffsets(utc, offsets, UTC_TIME);
    }

    public int getOffsetsByStandard(long standard, int[] offsets) {
        return getOffsets(standard, offsets, STANDARD_TIME);
    }

    public int getOffsetsByWall(long wall, int[] offsets) {
        return getOffsets(wall, offsets, WALL_TIME);
    }

    private int getOffsets(long date, int[] offsets, int type) {
        // if dst is never observed, there is no transition.
        if (transitions == null) {
            int offset = getLastRawOffset();
            if (offsets != null) {
                offsets[0] = offset;
                offsets[1] = 0;
            }
            return offset;
        }

        date -= rawOffsetDiff;
        int index = getTransitionIndex(date, type);

        // prior to the transition table, returns the raw offset.
        // FIXME: should support LMT.
        if (index < 0) {
            int offset = getLastRawOffset();
            if (offsets != null) {
                offsets[0] = offset;
                offsets[1] = 0;
            }
            return offset;
        }

        if (index < transitions.length) {
            long val = transitions[index];
            int offset = this.offsets[(int)(val & OFFSET_MASK)] + rawOffsetDiff;
            if (offsets != null) {
                int dst = (int)((val >>> DST_NSHIFT) & 0xfL);
                int save = (dst == 0) ? 0 : this.offsets[dst];
                offsets[0] = offset - save;
                offsets[1] = save;
            }
            return offset;
        }

        // beyond the transitions, delegate to SimpleTimeZone if there
        // is a rule; otherwise, return the offset of the last transition.
        SimpleTimeZone tz = getLastRule();
        if (tz != null) {
            int rawoffset = tz.getRawOffset();
            long msec = date;
            if (type != UTC_TIME) {
                msec -= rawOffset;
            }
            int dstoffset = tz.getOffset(msec) - rawOffset;

            // Check if it's in a standard-to-daylight transition.
            if (dstoffset > 0 && tz.getOffset(msec - dstoffset) == rawoffset && type == WALL_TIME) {
                dstoffset = 0;
            }

            if (offsets != null) {
                offsets[0] = rawoffset;
                offsets[1] = dstoffset;
            }
            return rawoffset + dstoffset;
        } else {
            // use the last transition
            long val = transitions[transitions.length - 1];
            int offset = this.offsets[(int)(val & OFFSET_MASK)] + rawOffsetDiff;
            if (offsets != null) {
                int dst = (int)((val >>> DST_NSHIFT) & 0xfL);
                int save = (dst == 0) ? 0 : this.offsets[dst];
                offsets[0] = offset - save;
                offsets[1] = save;
            }
            return offset;
        }
    }

    private int getTransitionIndex(long date, int type) {
        int low = 0;
        int high = transitions.length - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            long val = transitions[mid];
            long midVal = val >> TRANSITION_NSHIFT; // sign extended
            if (type != UTC_TIME) {
                midVal += offsets[(int)(val & OFFSET_MASK)]; // wall time
            }
            if (type == STANDARD_TIME) {
                int dstIndex = (int)((val >>> DST_NSHIFT) & 0xfL);
                if (dstIndex != 0) {
                    midVal -= offsets[dstIndex]; // make it standard time
                }
            }

            if (midVal < date) {
                low = mid + 1;
            } else if (midVal > date) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        // if beyond the transitions, returns that index.
        if (low >= transitions.length) {
            return low;
        }
        return low - 1;
    }

    /**
     * Returns the difference in milliseconds between local time and
     * UTC, taking into account both the raw offset and the effect of
     * daylight savings, for the specified date and time.  This method
     * assumes that the start and end month are distinct.  This method
     * assumes a Gregorian calendar for calculations.
     * <p>
     * <em>Note: In general, clients should use
     * {@link Calendar#ZONE_OFFSET Calendar.get(ZONE_OFFSET)} +
     * {@link Calendar#DST_OFFSET Calendar.get(DST_OFFSET)}
     * instead of calling this method.</em>
     *
     * @param era       The era of the given date. The value must be either
     *                  GregorianCalendar.AD or GregorianCalendar.BC.
     * @param year      The year in the given date.
     * @param month     The month in the given date. Month is 0-based. e.g.,
     *                  0 for January.
     * @param day       The day-in-month of the given date.
     * @param dayOfWeek The day-of-week of the given date.
     * @param milliseconds The milliseconds in day in <em>standard</em> local time.
     * @return The milliseconds to add to UTC to get local time.
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int milliseconds) {
        if (milliseconds < 0 || milliseconds >= AbstractCalendar.DAY_IN_MILLIS) {
            throw new IllegalArgumentException();
        }

        if (era == java.util.GregorianCalendar.BC) { // BC
            year = 1 - year;
        } else if (era != java.util.GregorianCalendar.AD) {
            throw new IllegalArgumentException();
        }

        Gregorian gcal = CalendarSystem.getGregorianCalendar();
        CalendarDate date = gcal.newCalendarDate(null);
        date.setDate(year, month + 1, day);
        if (gcal.validate(date) == false) {
            throw new IllegalArgumentException();
        }

        // bug-for-bug compatible argument checking
        if (dayOfWeek < java.util.GregorianCalendar.SUNDAY
            || dayOfWeek > java.util.GregorianCalendar.SATURDAY) {
            throw new IllegalArgumentException();
        }

        if (transitions == null) {
            return getLastRawOffset();
        }

        long dateInMillis = gcal.getTime(date) + milliseconds;
        dateInMillis -= (long) rawOffset; // make it UTC
        return getOffsets(dateInMillis, null, UTC_TIME);
    }

    /**
     * Sets the base time zone offset from GMT. This operation
     * modifies all the transitions of this ZoneInfo object, including
     * historical ones, if applicable.
     *
     * @param offsetMillis the base time zone offset to GMT.
     * @see getRawOffset
     */
    public synchronized void setRawOffset(int offsetMillis) {
        if (offsetMillis == rawOffset + rawOffsetDiff) {
            return;
        }
        rawOffsetDiff = offsetMillis - rawOffset;
        if (lastRule != null) {
            lastRule.setRawOffset(offsetMillis);
        }
        dirty = true;
    }

    /**
     * Returns the GMT offset of the current date. This GMT offset
     * value is not modified during Daylight Saving Time.
     *
     * @return the GMT offset value in milliseconds to add to UTC time
     * to get local standard time
     */
    public int getRawOffset() {
        if (!willGMTOffsetChange) {
            return rawOffset + rawOffsetDiff;
        }

        int[] offsets = new int[2];
        getOffsets(System.currentTimeMillis(), offsets, UTC_TIME);
        return offsets[0];
    }

    public boolean isDirty() {
        return dirty;
    }

    private int getLastRawOffset() {
        return rawOffset + rawOffsetDiff;
    }

    /**
     * Queries if this time zone uses Daylight Saving Time in the last known rule.
     */
    public boolean useDaylightTime() {
        return (simpleTimeZoneParams != null);
    }

    @Override
    public boolean observesDaylightTime() {
        if (simpleTimeZoneParams != null) {
            return true;
        }
        if (transitions == null) {
            return false;
        }

        // Look up the transition table to see if it's in DST right
        // now or if there's any standard-to-daylight transition at
        // any future.
        long utc = System.currentTimeMillis() - rawOffsetDiff;
        int index = getTransitionIndex(utc, UTC_TIME);

        // before transitions in the transition table
        if (index < 0) {
            return false;
        }

        // the time is in the table range.
        for (int i = index; i < transitions.length; i++) {
            if ((transitions[i] & DST_MASK) != 0) {
                return true;
            }
        }
        // No further DST is observed.
        return false;
    }

    /**
     * Queries if the specified date is in Daylight Saving Time.
     */
    public boolean inDaylightTime(Date date) {
        if (date == null) {
            throw new NullPointerException();
        }

        if (transitions == null) {
            return false;
        }

        long utc = date.getTime() - rawOffsetDiff;
        int index = getTransitionIndex(utc, UTC_TIME);

        // before transitions in the transition table
        if (index < 0) {
            return false;
        }

        // the time is in the table range.
        if (index < transitions.length) {
            return (transitions[index] & DST_MASK) != 0;
        }

        // beyond the transition table
        SimpleTimeZone tz = getLastRule();
        if (tz != null) {
            return tz.inDaylightTime(date);
        } else {
            // use the last transition
            return (transitions[transitions.length - 1] & DST_MASK) != 0;
        }
    }

    /**
     * Returns the amount of time in milliseconds that the clock is advanced
     * during daylight saving time is in effect in its last daylight saving time rule.
     *
     * @return the number of milliseconds the time is advanced with respect to
     * standard time when daylight saving time is in effect.
     */
    public int getDSTSavings() {
        return dstSavings;
    }

//    /**
//     * @return the last year in the transition table or -1 if this
//     * time zone doesn't observe any daylight saving time.
//     */
//    public int getMaxTransitionYear() {
//      if (transitions == null) {
//          return -1;
//      }
//      long val = transitions[transitions.length - 1];
//      int offset = this.offsets[(int)(val & OFFSET_MASK)] + rawOffsetDiff;
//      val = (val >> TRANSITION_NSHIFT) + offset;
//      CalendarDate lastDate = Gregorian.getCalendarDate(val);
//      return lastDate.getYear();
//    }

    /**
     * Returns a string representation of this time zone.
     * @return the string
     */
    public String toString() {
        return getClass().getName() +
            "[id=\"" + getID() + "\"" +
            ",offset=" + getLastRawOffset() +
            ",dstSavings=" + dstSavings +
            ",useDaylight=" + useDaylightTime() +
            ",transitions=" + ((transitions != null) ? transitions.length : 0) +
            ",lastRule=" + (lastRule == null ? getLastRuleInstance() : lastRule) +
            "]";
    }

    /**
     * Gets all available IDs supported in the Java run-time.
     *
     * @return an array of time zone IDs.
     */
    public static String[] getAvailableIDs() {
        return ZoneInfoFile.getZoneIds();
    }

    /**
     * Gets all available IDs that have the same value as the
     * specified raw GMT offset.
     *
     * @param rawOffset the GMT offset in milliseconds. This
     * value should not include any daylight saving time.
     *
     * @return an array of time zone IDs.
     */
    public static String[] getAvailableIDs(int rawOffset) {
        return ZoneInfoFile.getZoneIds(rawOffset);
    }

    /**
     * Gets the ZoneInfo for the given ID.
     *
     * @param ID the ID for a ZoneInfo. See TimeZone for detail.
     *
     * @return the specified ZoneInfo object, or null if there is no
     * time zone of the ID.
     */
    public static TimeZone getTimeZone(String ID) {
        return ZoneInfoFile.getZoneInfo(ID);
    }

    private transient SimpleTimeZone lastRule;

    /**
     * Returns a SimpleTimeZone object representing the last GMT
     * offset and DST schedule or null if this time zone doesn't
     * observe DST.
     */
    private synchronized SimpleTimeZone getLastRule() {
        if (lastRule == null) {
            lastRule = getLastRuleInstance();
        }
        return lastRule;
    }

    /**
     * Returns a SimpleTimeZone object that represents the last
     * known daylight saving time rules.
     *
     * @return a SimpleTimeZone object or null if this time zone
     * doesn't observe DST.
     */
    public SimpleTimeZone getLastRuleInstance() {
        if (simpleTimeZoneParams == null) {
            return null;
        }
        if (simpleTimeZoneParams.length == 10) {
            return new SimpleTimeZone(getLastRawOffset(), getID(),
                                      simpleTimeZoneParams[0],
                                      simpleTimeZoneParams[1],
                                      simpleTimeZoneParams[2],
                                      simpleTimeZoneParams[3],
                                      simpleTimeZoneParams[4],
                                      simpleTimeZoneParams[5],
                                      simpleTimeZoneParams[6],
                                      simpleTimeZoneParams[7],
                                      simpleTimeZoneParams[8],
                                      simpleTimeZoneParams[9],
                                      dstSavings);
        }
        return new SimpleTimeZone(getLastRawOffset(), getID(),
                                  simpleTimeZoneParams[0],
                                  simpleTimeZoneParams[1],
                                  simpleTimeZoneParams[2],
                                  simpleTimeZoneParams[3],
                                  simpleTimeZoneParams[4],
                                  simpleTimeZoneParams[5],
                                  simpleTimeZoneParams[6],
                                  simpleTimeZoneParams[7],
                                  dstSavings);
    }

    /**
     * Returns a copy of this <code>ZoneInfo</code>.
     */
    public Object clone() {
        ZoneInfo zi = (ZoneInfo) super.clone();
        zi.lastRule = null;
        return zi;
    }

    /**
     * Returns a hash code value calculated from the GMT offset and
     * transitions.
     * @return a hash code of this time zone
     */
    public int hashCode() {
        return getLastRawOffset() ^ checksum;
    }

    /**
     * Compares the equity of two ZoneInfo objects.
     *
     * @param obj the object to be compared with
     * @return true if given object is same as this ZoneInfo object,
     * false otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo that = (ZoneInfo) obj;
        return (getID().equals(that.getID())
                && (getLastRawOffset() == that.getLastRawOffset())
                && (checksum == that.checksum));
    }

    /**
     * Returns true if this zone has the same raw GMT offset value and
     * transition table as another zone info. If the specified
     * TimeZone object is not a ZoneInfo instance, this method returns
     * true if the specified TimeZone object has the same raw GMT
     * offset value with no daylight saving time.
     *
     * @param other the ZoneInfo object to be compared with
     * @return true if the given <code>TimeZone</code> has the same
     * GMT offset and transition information; false, otherwise.
     */
    public boolean hasSameRules(TimeZone other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof ZoneInfo)) {
            if (getRawOffset() != other.getRawOffset()) {
                return false;
            }
            // if both have the same raw offset and neither observes
            // DST, they have the same rule.
            if ((transitions == null)
                && (useDaylightTime() == false)
                && (other.useDaylightTime() == false)) {
                return true;
            }
            return false;
        }
        if (getLastRawOffset() != ((ZoneInfo)other).getLastRawOffset()) {
            return false;
        }
        return (checksum == ((ZoneInfo)other).checksum);
    }

    /**
     * Returns a Map from alias time zone IDs to their standard
     * time zone IDs.
     *
     * @return the Map that holds the mappings from alias time zone IDs
     *    to their standard time zone IDs, or null if
     *    <code>ZoneInfoMappings</code> file is not available.
     */
    public static Map<String, String> getAliasTable() {
         return ZoneInfoFile.getAliasMap();
    }

    @java.io.Serial
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // We don't know how this object from 1.4.x or earlier has
        // been mutated. So it should always be marked as `dirty'.
        dirty = true;
    }
}
