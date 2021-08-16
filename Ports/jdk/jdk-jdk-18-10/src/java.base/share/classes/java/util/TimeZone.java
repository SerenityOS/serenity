/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.util;

import java.io.Serializable;
import java.time.ZoneId;

import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;
import sun.util.calendar.ZoneInfo;
import sun.util.calendar.ZoneInfoFile;
import sun.util.locale.provider.TimeZoneNameUtility;

/**
 * {@code TimeZone} represents a time zone offset, and also figures out daylight
 * savings.
 *
 * <p>
 * Typically, you get a {@code TimeZone} using {@code getDefault}
 * which creates a {@code TimeZone} based on the time zone where the program
 * is running. For example, for a program running in Japan, {@code getDefault}
 * creates a {@code TimeZone} object based on Japanese Standard Time.
 *
 * <p>
 * You can also get a {@code TimeZone} using {@code getTimeZone}
 * along with a time zone ID. For instance, the time zone ID for the
 * U.S. Pacific Time zone is "America/Los_Angeles". So, you can get a
 * U.S. Pacific Time {@code TimeZone} object with:
 * <blockquote><pre>
 * TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
 * </pre></blockquote>
 * You can use the {@code getAvailableIDs} method to iterate through
 * all the supported time zone IDs. You can then choose a
 * supported ID to get a {@code TimeZone}.
 * If the time zone you want is not represented by one of the
 * supported IDs, then a custom time zone ID can be specified to
 * produce a TimeZone. The syntax of a custom time zone ID is:
 *
 * <blockquote><pre>
 * <a id="CustomID"><i>CustomID:</i></a>
 *         {@code GMT} <i>Sign</i> <i>Hours</i> {@code :} <i>Minutes</i>
 *         {@code GMT} <i>Sign</i> <i>Hours</i> <i>Minutes</i>
 *         {@code GMT} <i>Sign</i> <i>Hours</i>
 * <i>Sign:</i> one of
 *         {@code + -}
 * <i>Hours:</i>
 *         <i>Digit</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         {@code 0 1 2 3 4 5 6 7 8 9}
 * </pre></blockquote>
 *
 * <i>Hours</i> must be between 0 to 23 and <i>Minutes</i> must be
 * between 00 to 59.  For example, "GMT+10" and "GMT+0010" mean ten
 * hours and ten minutes ahead of GMT, respectively.
 * <p>
 * The format is locale independent and digits must be taken from the
 * Basic Latin block of the Unicode standard. No daylight saving time
 * transition schedule can be specified with a custom time zone ID. If
 * the specified string doesn't match the syntax, {@code "GMT"}
 * is used.
 * <p>
 * When creating a {@code TimeZone}, the specified custom time
 * zone ID is normalized in the following syntax:
 * <blockquote><pre>
 * <a id="NormalizedCustomID"><i>NormalizedCustomID:</i></a>
 *         {@code GMT} <i>Sign</i> <i>TwoDigitHours</i> {@code :} <i>Minutes</i>
 * <i>Sign:</i> one of
 *         {@code + -}
 * <i>TwoDigitHours:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Minutes:</i>
 *         <i>Digit</i> <i>Digit</i>
 * <i>Digit:</i> one of
 *         {@code 0 1 2 3 4 5 6 7 8 9}
 * </pre></blockquote>
 * For example, TimeZone.getTimeZone("GMT-8").getID() returns "GMT-08:00".
 *
 * <h2>Three-letter time zone IDs</h2>
 *
 * For compatibility with JDK 1.1.x, some other three-letter time zone IDs
 * (such as "PST", "CTT", "AST") are also supported. However, <strong>their
 * use is deprecated</strong> because the same abbreviation is often used
 * for multiple time zones (for example, "CST" could be U.S. "Central Standard
 * Time" and "China Standard Time"), and the Java platform can then only
 * recognize one of them.
 *
 *
 * @see          Calendar
 * @see          GregorianCalendar
 * @see          SimpleTimeZone
 * @author       Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu
 * @since        1.1
 */
public abstract class TimeZone implements Serializable, Cloneable {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    public TimeZone() {
    }

    /**
     * A style specifier for {@code getDisplayName()} indicating
     * a short name, such as "PST."
     * @see #LONG
     * @since 1.2
     */
    public static final int SHORT = 0;

    /**
     * A style specifier for {@code getDisplayName()} indicating
     * a long name, such as "Pacific Standard Time."
     * @see #SHORT
     * @since 1.2
     */
    public static final int LONG  = 1;

    // Constants used internally; unit is milliseconds
    private static final int ONE_MINUTE = 60*1000;
    private static final int ONE_HOUR   = 60*ONE_MINUTE;
    private static final int ONE_DAY    = 24*ONE_HOUR;

    // Proclaim serialization compatibility with JDK 1.1
    @java.io.Serial
    static final long serialVersionUID = 3581463369166924961L;

    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add to UTC to get local time.
     * <p>
     * This method returns a historically correct offset if an
     * underlying {@code TimeZone} implementation subclass
     * supports historical Daylight Saving Time schedule and GMT
     * offset changes.
     *
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the milliseconds in day in <em>standard</em>
     * local time.
     *
     * @return the offset in milliseconds to add to GMT to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    public abstract int getOffset(int era, int year, int month, int day,
                                  int dayOfWeek, int milliseconds);

    /**
     * Returns the offset of this time zone from UTC at the specified
     * date. If Daylight Saving Time is in effect at the specified
     * date, the offset value is adjusted with the amount of daylight
     * saving.
     * <p>
     * This method returns a historically correct offset value if an
     * underlying TimeZone implementation subclass supports historical
     * Daylight Saving Time schedule and GMT offset changes.
     *
     * @param date the date represented in milliseconds since January 1, 1970 00:00:00 GMT
     * @return the amount of time in milliseconds to add to UTC to get local time.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     * @since 1.4
     */
    public int getOffset(long date) {
        if (inDaylightTime(new Date(date))) {
            return getRawOffset() + getDSTSavings();
        }
        return getRawOffset();
    }

    /**
     * Gets the raw GMT offset and the amount of daylight saving of this
     * time zone at the given time.
     * @param date the milliseconds (since January 1, 1970,
     * 00:00:00.000 GMT) at which the time zone offset and daylight
     * saving amount are found
     * @param offsets an array of int where the raw GMT offset
     * (offset[0]) and daylight saving amount (offset[1]) are stored,
     * or null if those values are not needed. The method assumes that
     * the length of the given array is two or larger.
     * @return the total amount of the raw GMT offset and daylight
     * saving at the specified date.
     *
     * @see Calendar#ZONE_OFFSET
     * @see Calendar#DST_OFFSET
     */
    int getOffsets(long date, int[] offsets) {
        int rawoffset = getRawOffset();
        int dstoffset = 0;
        if (inDaylightTime(new Date(date))) {
            dstoffset = getDSTSavings();
        }
        if (offsets != null) {
            offsets[0] = rawoffset;
            offsets[1] = dstoffset;
        }
        return rawoffset + dstoffset;
    }

    /**
     * Sets the base time zone offset to GMT.
     * This is the offset to add to UTC to get local time.
     * <p>
     * If an underlying {@code TimeZone} implementation subclass
     * supports historical GMT offset changes, the specified GMT
     * offset is set as the latest GMT offset and the difference from
     * the known latest GMT offset value is used to adjust all
     * historical GMT offset values.
     *
     * @param offsetMillis the given base time zone offset to GMT.
     */
    public abstract void setRawOffset(int offsetMillis);

    /**
     * Returns the amount of time in milliseconds to add to UTC to get
     * standard time in this time zone. Because this value is not
     * affected by daylight saving time, it is called <I>raw
     * offset</I>.
     * <p>
     * If an underlying {@code TimeZone} implementation subclass
     * supports historical GMT offset changes, the method returns the
     * raw offset value of the current date. In Honolulu, for example,
     * its raw offset changed from GMT-10:30 to GMT-10:00 in 1947, and
     * this method always returns -36000000 milliseconds (i.e., -10
     * hours).
     *
     * @return the amount of raw offset time in milliseconds to add to UTC.
     * @see Calendar#ZONE_OFFSET
     */
    public abstract int getRawOffset();

    /**
     * Gets the ID of this time zone.
     * @return the ID of this time zone.
     */
    public String getID()
    {
        return ID;
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     */
    public void setID(String ID)
    {
        if (ID == null) {
            throw new NullPointerException();
        }
        this.ID = ID;
        this.zoneId = null;   // invalidate cache
    }

    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the default locale.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(false, {@link #LONG},
     *                Locale.getDefault({@link Locale.Category#DISPLAY}))
     * </pre></blockquote>
     *
     * @return the human-readable name of this time zone in the default locale.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     */
    public final String getDisplayName() {
        return getDisplayName(false, LONG,
                              Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Returns a long standard time name of this {@code TimeZone} suitable for
     * presentation to the user in the specified {@code locale}.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(false, {@link #LONG}, locale)
     * </pre></blockquote>
     *
     * @param locale the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale.
     * @throws    NullPointerException if {@code locale} is {@code null}.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     */
    public final String getDisplayName(Locale locale) {
        return getDisplayName(false, LONG, locale);
    }

    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the default locale. If the
     * specified {@code daylight} is {@code true}, a Daylight Saving Time name
     * is returned (even if this {@code TimeZone} doesn't observe Daylight Saving
     * Time). Otherwise, a Standard Time name is returned.
     *
     * <p>This method is equivalent to:
     * <blockquote><pre>
     * getDisplayName(daylight, style,
     *                Locale.getDefault({@link Locale.Category#DISPLAY}))
     * </pre></blockquote>
     *
     * @param daylight {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style either {@link #LONG} or {@link #SHORT}
     * @return the human-readable name of this time zone in the default locale.
     * @throws    IllegalArgumentException if {@code style} is invalid.
     * @since 1.2
     * @see #getDisplayName(boolean, int, Locale)
     * @see Locale#getDefault(Locale.Category)
     * @see Locale.Category
     * @see java.text.DateFormatSymbols#getZoneStrings()
     */
    public final String getDisplayName(boolean daylight, int style) {
        return getDisplayName(daylight, style,
                              Locale.getDefault(Locale.Category.DISPLAY));
    }

    /**
     * Returns a name in the specified {@code style} of this {@code TimeZone}
     * suitable for presentation to the user in the specified {@code
     * locale}. If the specified {@code daylight} is {@code true}, a Daylight
     * Saving Time name is returned (even if this {@code TimeZone} doesn't
     * observe Daylight Saving Time). Otherwise, a Standard Time name is
     * returned.
     *
     * <p>When looking up a time zone name, the {@linkplain
     * ResourceBundle.Control#getCandidateLocales(String,Locale) default
     * {@code Locale} search path of {@code ResourceBundle}} derived
     * from the specified {@code locale} is used. (No {@linkplain
     * ResourceBundle.Control#getFallbackLocale(String,Locale) fallback
     * {@code Locale}} search is performed.) If a time zone name in any
     * {@code Locale} of the search path, including {@link Locale#ROOT}, is
     * found, the name is returned. Otherwise, a string in the
     * <a href="#NormalizedCustomID">normalized custom ID format</a> is returned.
     *
     * @param daylight {@code true} specifying a Daylight Saving Time name, or
     *                 {@code false} specifying a Standard Time name
     * @param style either {@link #LONG} or {@link #SHORT}
     * @param locale   the locale in which to supply the display name.
     * @return the human-readable name of this time zone in the given locale.
     * @throws    IllegalArgumentException if {@code style} is invalid.
     * @throws    NullPointerException if {@code locale} is {@code null}.
     * @since 1.2
     * @see java.text.DateFormatSymbols#getZoneStrings()
     */
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        if (style != SHORT && style != LONG) {
            throw new IllegalArgumentException("Illegal style: " + style);
        }
        String id = getID();
        String name = TimeZoneNameUtility.retrieveDisplayName(id, daylight, style, locale);
        if (name != null) {
            return name;
        }

        if (id.startsWith("GMT") && id.length() > 3) {
            char sign = id.charAt(3);
            if (sign == '+' || sign == '-') {
                return id;
            }
        }
        int offset = getRawOffset();
        if (daylight) {
            offset += getDSTSavings();
        }
        return ZoneInfoFile.toCustomID(offset);
    }

    private static String[] getDisplayNames(String id, Locale locale) {
        return TimeZoneNameUtility.retrieveDisplayNames(id, locale);
    }

    /**
     * Returns the amount of time to be added to local standard time
     * to get local wall clock time.
     *
     * <p>The default implementation returns 3600000 milliseconds
     * (i.e., one hour) if a call to {@link #useDaylightTime()}
     * returns {@code true}. Otherwise, 0 (zero) is returned.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method returns the amount of saving time of the
     * last known Daylight Saving Time rule that can be a future
     * prediction.
     *
     * <p>If the amount of saving time at any given time stamp is
     * required, construct a {@link Calendar} with this {@code
     * TimeZone} and the time stamp, and call {@link Calendar#get(int)
     * Calendar.get}{@code (}{@link Calendar#DST_OFFSET}{@code )}.
     *
     * @return the amount of saving time in milliseconds
     * @since 1.4
     * @see #inDaylightTime(Date)
     * @see #getOffset(long)
     * @see #getOffset(int,int,int,int,int,int)
     * @see Calendar#ZONE_OFFSET
     */
    public int getDSTSavings() {
        if (useDaylightTime()) {
            return 3600000;
        }
        return 0;
    }

    /**
     * Queries if this {@code TimeZone} uses Daylight Saving Time.
     *
     * <p>If an underlying {@code TimeZone} implementation subclass
     * supports historical and future Daylight Saving Time schedule
     * changes, this method refers to the last known Daylight Saving Time
     * rule that can be a future prediction and may not be the same as
     * the current rule. Consider calling {@link #observesDaylightTime()}
     * if the current rule should also be taken into account.
     *
     * @return {@code true} if this {@code TimeZone} uses Daylight Saving Time,
     *         {@code false}, otherwise.
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     */
    public abstract boolean useDaylightTime();

    /**
     * Returns {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time.
     *
     * <p>The default implementation returns {@code true} if
     * {@code useDaylightTime()} or {@code inDaylightTime(new Date())}
     * returns {@code true}.
     *
     * @return {@code true} if this {@code TimeZone} is currently in
     * Daylight Saving Time, or if a transition from Standard Time to
     * Daylight Saving Time occurs at any future time; {@code false}
     * otherwise.
     * @since 1.7
     * @see #useDaylightTime()
     * @see #inDaylightTime(Date)
     * @see Calendar#DST_OFFSET
     */
    public boolean observesDaylightTime() {
        return useDaylightTime() || inDaylightTime(new Date());
    }

    /**
     * Queries if the given {@code date} is in Daylight Saving Time in
     * this time zone.
     *
     * @param date the given Date.
     * @return {@code true} if the given date is in Daylight Saving Time,
     *         {@code false}, otherwise.
     */
    public abstract boolean inDaylightTime(Date date);

    /**
     * Gets the {@code TimeZone} for the given ID.
     *
     * @param ID the ID for a {@code TimeZone}, either an abbreviation
     * such as "PST", a full name such as "America/Los_Angeles", or a custom
     * ID such as "GMT-8:00". Note that the support of abbreviations is
     * for JDK 1.1.x compatibility only and full names should be used.
     *
     * @return the specified {@code TimeZone}, or the GMT zone if the given ID
     * cannot be understood.
     */
    public static synchronized TimeZone getTimeZone(String ID) {
        return getTimeZone(ID, true);
    }

    /**
     * Gets the {@code TimeZone} for the given {@code zoneId}.
     *
     * @param zoneId a {@link ZoneId} from which the time zone ID is obtained
     * @return the specified {@code TimeZone}, or the GMT zone if the given ID
     *         cannot be understood.
     * @throws NullPointerException if {@code zoneId} is {@code null}
     * @since 1.8
     */
    public static TimeZone getTimeZone(ZoneId zoneId) {
        String tzid = zoneId.getId(); // throws an NPE if null
        char c = tzid.charAt(0);
        if (c == '+' || c == '-') {
            tzid = "GMT" + tzid;
        } else if (c == 'Z' && tzid.length() == 1) {
            tzid = "UTC";
        }
        return getTimeZone(tzid, true);
    }

    /**
     * Converts this {@code TimeZone} object to a {@code ZoneId}.
     *
     * @return a {@code ZoneId} representing the same time zone as this
     *         {@code TimeZone}
     * @since 1.8
     */
    public ZoneId toZoneId() {
        ZoneId zId = zoneId;
        if (zId == null) {
            zoneId = zId = toZoneId0();
        }
        return zId;
    }

    private ZoneId toZoneId0() {
        String id = getID();
        TimeZone defaultZone = defaultTimeZone;
        // are we not defaultTimeZone but our id is equal to default's?
        if (defaultZone != this &&
            defaultZone != null && id.equals(defaultZone.getID())) {
            // delegate to default TZ which is effectively immutable
            return defaultZone.toZoneId();
        }
        // derive it ourselves
        if (ZoneInfoFile.useOldMapping() && id.length() == 3) {
            if ("EST".equals(id))
                return ZoneId.of("America/New_York");
            if ("MST".equals(id))
                return ZoneId.of("America/Denver");
            if ("HST".equals(id))
                return ZoneId.of("America/Honolulu");
        }
        return ZoneId.of(id, ZoneId.SHORT_IDS);
    }

    private static TimeZone getTimeZone(String ID, boolean fallback) {
        TimeZone tz = ZoneInfo.getTimeZone(ID);
        if (tz == null) {
            tz = parseCustomTimeZone(ID);
            if (tz == null && fallback) {
                tz = new ZoneInfo(GMT_ID, 0);
            }
        }
        return tz;
    }

    /**
     * Gets the available IDs according to the given time zone offset in milliseconds.
     *
     * @param rawOffset the given time zone GMT offset in milliseconds.
     * @return an array of IDs, where the time zone for that ID has
     * the specified GMT offset. For example, "America/Phoenix" and "America/Denver"
     * both have GMT-07:00, but differ in daylight saving behavior.
     * @see #getRawOffset()
     */
    public static synchronized String[] getAvailableIDs(int rawOffset) {
        return ZoneInfo.getAvailableIDs(rawOffset);
    }

    /**
     * Gets all the available IDs supported.
     * @return an array of IDs.
     */
    public static synchronized String[] getAvailableIDs() {
        return ZoneInfo.getAvailableIDs();
    }

    /**
     * Gets the platform defined TimeZone ID.
     **/
    private static native String getSystemTimeZoneID(String javaHome);

    /**
     * Gets the custom time zone ID based on the GMT offset of the
     * platform. (e.g., "GMT+08:00")
     */
    private static native String getSystemGMTOffsetID();

    /**
     * Gets the default {@code TimeZone} of the Java virtual machine. If the
     * cached default {@code TimeZone} is available, its clone is returned.
     * Otherwise, the method takes the following steps to determine the default
     * time zone.
     *
     * <ul>
     * <li>Use the {@code user.timezone} property value as the default
     * time zone ID if it's available.</li>
     * <li>Detect the platform time zone ID. The source of the
     * platform time zone and ID mapping may vary with implementation.</li>
     * <li>Use {@code GMT} as the last resort if the given or detected
     * time zone ID is unknown.</li>
     * </ul>
     *
     * <p>The default {@code TimeZone} created from the ID is cached,
     * and its clone is returned. The {@code user.timezone} property
     * value is set to the ID upon return.
     *
     * @return the default {@code TimeZone}
     * @see #setDefault(TimeZone)
     */
    public static TimeZone getDefault() {
        return (TimeZone) getDefaultRef().clone();
    }

    /**
     * Returns the reference to the default TimeZone object. This
     * method doesn't create a clone.
     */
    static TimeZone getDefaultRef() {
        TimeZone defaultZone = defaultTimeZone;
        if (defaultZone == null) {
            // Need to initialize the default time zone.
            defaultZone = setDefaultZone();
            assert defaultZone != null;
        }
        // Don't clone here.
        return defaultZone;
    }

    private static synchronized TimeZone setDefaultZone() {
        TimeZone tz;
        // get the time zone ID from the system properties
        Properties props = GetPropertyAction.privilegedGetProperties();
        String zoneID = props.getProperty("user.timezone");

        // if the time zone ID is not set (yet), perform the
        // platform to Java time zone ID mapping.
        if (zoneID == null || zoneID.isEmpty()) {
            String javaHome = StaticProperty.javaHome();
            try {
                zoneID = getSystemTimeZoneID(javaHome);
                if (zoneID == null) {
                    zoneID = GMT_ID;
                }
            } catch (NullPointerException e) {
                zoneID = GMT_ID;
            }
        }

        // Get the time zone for zoneID. But not fall back to
        // "GMT" here.
        tz = getTimeZone(zoneID, false);

        if (tz == null) {
            // If the given zone ID is unknown in Java, try to
            // get the GMT-offset-based time zone ID,
            // a.k.a. custom time zone ID (e.g., "GMT-08:00").
            String gmtOffsetID = getSystemGMTOffsetID();
            if (gmtOffsetID != null) {
                zoneID = gmtOffsetID;
            }
            tz = getTimeZone(zoneID, true);
        }
        assert tz != null;

        final String id = zoneID;
        props.setProperty("user.timezone", id);

        defaultTimeZone = tz;
        return tz;
    }

    /**
     * Sets the {@code TimeZone} that is returned by the {@code getDefault}
     * method. {@code zone} is cached. If {@code zone} is null, the cached
     * default {@code TimeZone} is cleared. This method doesn't change the value
     * of the {@code user.timezone} property.
     *
     * @param zone the new default {@code TimeZone}, or null
     * @throws SecurityException if the security manager's {@code checkPermission}
     *                           denies {@code PropertyPermission("user.timezone",
     *                           "write")}
     * @see #getDefault
     * @see PropertyPermission
     */
    public static void setDefault(TimeZone zone)
    {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new PropertyPermission
                               ("user.timezone", "write"));
        }
        // by saving a defensive clone and returning a clone in getDefault() too,
        // the defaultTimeZone instance is isolated from user code which makes it
        // effectively immutable. This is important to avoid races when the
        // following is evaluated in ZoneId.systemDefault():
        // TimeZone.getDefault().toZoneId().
        defaultTimeZone = (zone == null) ? null : (TimeZone) zone.clone();
    }

    /**
     * Returns true if this zone has the same rule and offset as another zone.
     * That is, if this zone differs only in ID, if at all.  Returns false
     * if the other zone is null.
     * @param other the {@code TimeZone} object to be compared with
     * @return true if the other zone is not null and is the same as this one,
     * with the possible exception of the ID
     * @since 1.2
     */
    public boolean hasSameRules(TimeZone other) {
        return other != null && getRawOffset() == other.getRawOffset() &&
            useDaylightTime() == other.useDaylightTime();
    }

    /**
     * Creates a copy of this {@code TimeZone}.
     *
     * @return a clone of this {@code TimeZone}
     */
    public Object clone()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * The null constant as a TimeZone.
     */
    static final TimeZone NO_TIMEZONE = null;

    // =======================privates===============================

    /**
     * The string identifier of this {@code TimeZone}.  This is a
     * programmatic identifier used internally to look up {@code TimeZone}
     * objects from the system table and also to map them to their localized
     * display names.  {@code ID} values are unique in the system
     * table but may not be for dynamically created zones.
     * @serial
     */
    private String           ID;

    /**
     * Cached {@link ZoneId} for this TimeZone
     */
    private transient ZoneId zoneId;

    private static volatile TimeZone defaultTimeZone;

    static final String         GMT_ID        = "GMT";
    private static final int    GMT_ID_LENGTH = 3;

    /**
     * Parses a custom time zone identifier and returns a corresponding zone.
     * This method doesn't support the RFC 822 time zone format. (e.g., +hhmm)
     *
     * @param id a string of the <a href="#CustomID">custom ID form</a>.
     * @return a newly created TimeZone with the given offset and
     * no daylight saving time, or null if the id cannot be parsed.
     */
    private static final TimeZone parseCustomTimeZone(String id) {
        int length;

        // Error if the length of id isn't long enough or id doesn't
        // start with "GMT".
        if ((length = id.length()) < (GMT_ID_LENGTH + 2) ||
            id.indexOf(GMT_ID) != 0) {
            return null;
        }

        ZoneInfo zi;

        // First, we try to find it in the cache with the given
        // id. Even the id is not normalized, the returned ZoneInfo
        // should have its normalized id.
        zi = ZoneInfoFile.getZoneInfo(id);
        if (zi != null) {
            return zi;
        }

        int index = GMT_ID_LENGTH;
        boolean negative = false;
        char c = id.charAt(index++);
        if (c == '-') {
            negative = true;
        } else if (c != '+') {
            return null;
        }

        int hours = 0;
        int num = 0;
        int countDelim = 0;
        int len = 0;
        while (index < length) {
            c = id.charAt(index++);
            if (c == ':') {
                if (countDelim > 0) {
                    return null;
                }
                if (len > 2) {
                    return null;
                }
                hours = num;
                countDelim++;
                num = 0;
                len = 0;
                continue;
            }
            if (c < '0' || c > '9') {
                return null;
            }
            num = num * 10 + (c - '0');
            len++;
        }
        if (index != length) {
            return null;
        }
        if (countDelim == 0) {
            if (len <= 2) {
                hours = num;
                num = 0;
            } else {
                hours = num / 100;
                num %= 100;
            }
        } else {
            if (len != 2) {
                return null;
            }
        }
        if (hours > 23 || num > 59) {
            return null;
        }
        int gmtOffset =  (hours * 60 + num) * 60 * 1000;

        if (gmtOffset == 0) {
            zi = ZoneInfoFile.getZoneInfo(GMT_ID);
            if (negative) {
                zi.setID("GMT-00:00");
            } else {
                zi.setID("GMT+00:00");
            }
        } else {
            zi = ZoneInfoFile.getCustomTimeZone(id, negative ? -gmtOffset : gmtOffset);
        }
        return zi;
    }
}
