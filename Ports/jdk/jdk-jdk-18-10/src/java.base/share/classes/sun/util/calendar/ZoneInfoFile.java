/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;

/**
 * Loads TZDB time-zone rules for j.u.TimeZone
 * <p>
 * @since 1.8
 */
public final class ZoneInfoFile {

    /**
     * Gets all available IDs supported in the Java run-time.
     *
     * @return a set of time zone IDs.
     */
    public static String[] getZoneIds() {
        int len = regions.length + oldMappings.length;
        if (!USE_OLDMAPPING) {
            len += 3;    // EST/HST/MST not in tzdb.dat
        }
        String[] ids = Arrays.copyOf(regions, len);
        int i = regions.length;
        if (!USE_OLDMAPPING) {
            ids[i++] = "EST";
            ids[i++] = "HST";
            ids[i++] = "MST";
        }
        for (int j = 0; j < oldMappings.length; j++) {
            ids[i++] = oldMappings[j][0];
        }
        return ids;
    }

    /**
     * Gets all available IDs that have the same value as the
     * specified raw GMT offset.
     *
     * @param rawOffset  the GMT offset in milliseconds. This
     *                   value should not include any daylight saving time.
     * @return an array of time zone IDs.
     */
    public static String[] getZoneIds(int rawOffset) {
        List<String> ids = new ArrayList<>();
        for (String id : getZoneIds()) {
            ZoneInfo zi = getZoneInfo(id);
            if (zi.getRawOffset() == rawOffset) {
                ids.add(id);
            }
        }
        // It appears the "zi" implementation returns the
        // sorted list, though the specification does not
        // specify it. Keep the same behavior for better
        // compatibility.
        String[] list = ids.toArray(new String[ids.size()]);
        Arrays.sort(list);
        return list;
    }

    public static ZoneInfo getZoneInfo(String zoneId) {
        if (zoneId == null) {
            return null;
        }
        ZoneInfo zi = getZoneInfo0(zoneId);
        if (zi != null) {
            zi = (ZoneInfo)zi.clone();
            zi.setID(zoneId);
        }
        return zi;
    }

    private static ZoneInfo getZoneInfo0(String zoneId) {
        try {
            ZoneInfo zi = zones.get(zoneId);
            if (zi != null) {
                return zi;
            }
            String zid = zoneId;
            if (aliases.containsKey(zoneId)) {
                zid = aliases.get(zoneId);
            }
            int index = Arrays.binarySearch(regions, zid);
            if (index < 0) {
                return null;
            }
            byte[] bytes = ruleArray[indices[index]];
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
            zi = getZoneInfo(dis, zid);
            zones.put(zoneId, zi);
            return zi;
        } catch (Exception ex) {
            throw new RuntimeException("Invalid binary time-zone data: TZDB:" +
                zoneId + ", version: " + versionId, ex);
        }
    }

    /**
     * Returns a Map from alias time zone IDs to their standard
     * time zone IDs.
     *
     * @return an unmodified alias mapping
     */
    public static Map<String, String> getAliasMap() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Gets the version of this tz data.
     *
     * @return the tzdb version
     */
    public static String getVersion() {
        return versionId;
    }

    /**
     * Gets a ZoneInfo with the given GMT offset. The object
     * has its ID in the format of GMT{+|-}hh:mm.
     *
     * @param originalId  the given custom id (before normalized such as "GMT+9")
     * @param gmtOffset   GMT offset <em>in milliseconds</em>
     * @return a ZoneInfo constructed with the given GMT offset
     */
    public static ZoneInfo getCustomTimeZone(String originalId, int gmtOffset) {
        String id = toCustomID(gmtOffset);
        return new ZoneInfo(id, gmtOffset);
    }

    public static String toCustomID(int gmtOffset) {
        char sign;
        int offset = gmtOffset / 60000;
        if (offset >= 0) {
            sign = '+';
        } else {
            sign = '-';
            offset = -offset;
        }
        int hh = offset / 60;
        int mm = offset % 60;

        char[] buf = new char[] { 'G', 'M', 'T', sign, '0', '0', ':', '0', '0' };
        if (hh >= 10) {
            buf[4] += hh / 10;
        }
        buf[5] += hh % 10;
        if (mm != 0) {
            buf[7] += mm / 10;
            buf[8] += mm % 10;
        }
        return new String(buf);
    }

    ///////////////////////////////////////////////////////////
    private ZoneInfoFile() {
    }

    private static String versionId;
    private static final Map<String, ZoneInfo> zones = new ConcurrentHashMap<>();
    private static Map<String, String> aliases = new HashMap<>();

    private static byte[][] ruleArray;
    private static String[] regions;
    private static int[] indices;

    // Flag for supporting JDK backward compatible IDs, such as "EST".
    private static final boolean USE_OLDMAPPING;

    private static String[][] oldMappings = new String[][] {
        { "ACT", "Australia/Darwin" },
        { "AET", "Australia/Sydney" },
        { "AGT", "America/Argentina/Buenos_Aires" },
        { "ART", "Africa/Cairo" },
        { "AST", "America/Anchorage" },
        { "BET", "America/Sao_Paulo" },
        { "BST", "Asia/Dhaka" },
        { "CAT", "Africa/Harare" },
        { "CNT", "America/St_Johns" },
        { "CST", "America/Chicago" },
        { "CTT", "Asia/Shanghai" },
        { "EAT", "Africa/Addis_Ababa" },
        { "ECT", "Europe/Paris" },
        { "IET", "America/Indiana/Indianapolis" },
        { "IST", "Asia/Kolkata" },
        { "JST", "Asia/Tokyo" },
        { "MIT", "Pacific/Apia" },
        { "NET", "Asia/Yerevan" },
        { "NST", "Pacific/Auckland" },
        { "PLT", "Asia/Karachi" },
        { "PNT", "America/Phoenix" },
        { "PRT", "America/Puerto_Rico" },
        { "PST", "America/Los_Angeles" },
        { "SST", "Pacific/Guadalcanal" },
        { "VST", "Asia/Ho_Chi_Minh" },
    };

    static {
        String oldmapping = GetPropertyAction
                .privilegedGetProperty("sun.timezone.ids.oldmapping", "false")
                .toLowerCase(Locale.ROOT);
        USE_OLDMAPPING = (oldmapping.equals("yes") || oldmapping.equals("true"));
        loadTZDB();
    }

    @SuppressWarnings("removal")
    private static void loadTZDB() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    String libDir = StaticProperty.javaHome() + File.separator + "lib";
                    try (DataInputStream dis = new DataInputStream(
                             new BufferedInputStream(new FileInputStream(
                                 new File(libDir, "tzdb.dat"))))) {
                        load(dis);
                    }
                } catch (Exception x) {
                    throw new Error(x);
                }
                return null;
            }
        });
    }

    private static void addOldMapping() {
        for (String[] alias : oldMappings) {
            aliases.put(alias[0], alias[1]);
        }
        if (USE_OLDMAPPING) {
            aliases.put("EST", "America/New_York");
            aliases.put("MST", "America/Denver");
            aliases.put("HST", "Pacific/Honolulu");
        } else {
            zones.put("EST", new ZoneInfo("EST", -18000000));
            zones.put("MST", new ZoneInfo("MST", -25200000));
            zones.put("HST", new ZoneInfo("HST", -36000000));
        }
    }

    public static boolean useOldMapping() {
       return USE_OLDMAPPING;
    }

    /**
     * Loads the rules from a DateInputStream
     *
     * @param dis  the DateInputStream to load, not null
     * @throws Exception if an error occurs
     */
    private static void load(DataInputStream dis) throws ClassNotFoundException, IOException {
        if (dis.readByte() != 1) {
            throw new StreamCorruptedException("File format not recognised");
        }
        // group
        String groupId = dis.readUTF();
        if ("TZDB".equals(groupId) == false) {
            throw new StreamCorruptedException("File format not recognised");
        }
        // versions, only keep the last one
        int versionCount = dis.readShort();
        for (int i = 0; i < versionCount; i++) {
            versionId = dis.readUTF();

        }
        // regions
        int regionCount = dis.readShort();
        String[] regionArray = new String[regionCount];
        for (int i = 0; i < regionCount; i++) {
            regionArray[i] = dis.readUTF();
        }
        // rules
        int ruleCount = dis.readShort();
        ruleArray = new byte[ruleCount][];
        for (int i = 0; i < ruleCount; i++) {
            byte[] bytes = new byte[dis.readShort()];
            dis.readFully(bytes);
            ruleArray[i] = bytes;
        }
        // link version-region-rules, only keep the last version, if more than one
        for (int i = 0; i < versionCount; i++) {
            regionCount = dis.readShort();
            regions = new String[regionCount];
            indices = new int[regionCount];
            for (int j = 0; j < regionCount; j++) {
                regions[j] = regionArray[dis.readShort()];
                indices[j] = dis.readShort();
            }
        }
        // remove the following ids from the map, they
        // are exclued from the "old" ZoneInfo
        zones.remove("ROC");
        for (int i = 0; i < versionCount; i++) {
            int aliasCount = dis.readShort();
            aliases.clear();
            for (int j = 0; j < aliasCount; j++) {
                String alias = regionArray[dis.readShort()];
                String region = regionArray[dis.readShort()];
                aliases.put(alias, region);
            }
        }
        // old us time-zone names
        addOldMapping();
    }

    /////////////////////////Ser/////////////////////////////////
    public static ZoneInfo getZoneInfo(DataInput in, String zoneId) throws Exception {
        byte type = in.readByte();
        // TBD: assert ZRULES:
        int stdSize = in.readInt();
        long[] stdTrans = new long[stdSize];
        for (int i = 0; i < stdSize; i++) {
            stdTrans[i] = readEpochSec(in);
        }
        int [] stdOffsets = new int[stdSize + 1];
        for (int i = 0; i < stdOffsets.length; i++) {
            stdOffsets[i] = readOffset(in);
        }
        int savSize = in.readInt();
        long[] savTrans = new long[savSize];
        for (int i = 0; i < savSize; i++) {
            savTrans[i] = readEpochSec(in);
        }
        int[] savOffsets = new int[savSize + 1];
        for (int i = 0; i < savOffsets.length; i++) {
            savOffsets[i] = readOffset(in);
        }
        int ruleSize = in.readByte();
        ZoneOffsetTransitionRule[] rules = new ZoneOffsetTransitionRule[ruleSize];
        for (int i = 0; i < ruleSize; i++) {
            rules[i] = new ZoneOffsetTransitionRule(in);
        }
        return getZoneInfo(zoneId, stdTrans, stdOffsets, savTrans, savOffsets, rules);
    }

    public static int readOffset(DataInput in) throws IOException {
        int offsetByte = in.readByte();
        return offsetByte == 127 ? in.readInt() : offsetByte * 900;
    }

    static long readEpochSec(DataInput in) throws IOException {
        int hiByte = in.readByte() & 255;
        if (hiByte == 255) {
            return in.readLong();
        } else {
            int midByte = in.readByte() & 255;
            int loByte = in.readByte() & 255;
            long tot = ((hiByte << 16) + (midByte << 8) + loByte);
            return (tot * 900) - 4575744000L;
        }
    }

    /////////////////////////ZoneRules --> ZoneInfo/////////////////////////////////

    // ZoneInfo starts with UTC1900
    private static final long UTC1900 = -2208988800L;

    // ZoneInfo ends with   UTC2037
    // LocalDateTime.of(2038, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.UTC) - 1;
    private static final long UTC2037 = 2145916799L;

    // ZoneInfo has an ending entry for 2037, this need to be offset by
    // a "rawOffset"
    // LocalDateTime.of(2037, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.UTC));
    private static final long LDT2037 = 2114380800L;

    //Current time. Used to determine future GMToffset transitions
    private static final long CURRT = System.currentTimeMillis()/1000;

    /* Get a ZoneInfo instance.
     *
     * @param standardTransitions  the standard transitions, not null
     * @param standardOffsets  the standard offsets, not null
     * @param savingsInstantTransitions  the standard transitions, not null
     * @param wallOffsets  the wall offsets, not null
     * @param lastRules  the recurring last rules, size 15 or less, not null
     */
    private static ZoneInfo getZoneInfo(String zoneId,
                                        long[] standardTransitions,
                                        int[] standardOffsets,
                                        long[] savingsInstantTransitions,
                                        int[] wallOffsets,
                                        ZoneOffsetTransitionRule[] lastRules) {
        int rawOffset = 0;
        int dstSavings = 0;
        int checksum = 0;
        int[] params = null;
        boolean willGMTOffsetChange = false;

        // rawOffset, pick the last one
        if (standardTransitions.length > 0) {
            rawOffset = standardOffsets[standardOffsets.length - 1] * 1000;
            willGMTOffsetChange = standardTransitions[standardTransitions.length - 1] > CURRT;
        }
        else
            rawOffset = standardOffsets[0] * 1000;

        // transitions, offsets;
        long[] transitions = null;
        int[]  offsets = null;
        int    nOffsets = 0;
        int    nTrans = 0;

        if (savingsInstantTransitions.length != 0) {
            transitions = new long[250];
            offsets = new int[100];    // TBD: ZoneInfo actually can't handle
                                       // offsets.length > 16 (4-bit index limit)
            // last year in trans table
            // It should not matter to use before or after offset for year
            int lastyear = getYear(savingsInstantTransitions[savingsInstantTransitions.length - 1],
                                   wallOffsets[savingsInstantTransitions.length - 1]);
            int i = 0, k = 1;
            while (i < savingsInstantTransitions.length &&
                   savingsInstantTransitions[i] < UTC1900) {
                i++;     // skip any date before UTC1900
            }
            if (i < savingsInstantTransitions.length) {
                // javazic writes the last GMT offset into index 0!
                if (i < savingsInstantTransitions.length) {
                    offsets[0] = standardOffsets[standardOffsets.length - 1] * 1000;
                    nOffsets = 1;
                }
                // ZoneInfo has a beginning entry for 1900.
                // Only add it if this is not the only one in table
                nOffsets = addTrans(transitions, nTrans++,
                                    offsets, nOffsets,
                                    UTC1900,
                                    wallOffsets[i],
                                    getStandardOffset(standardTransitions, standardOffsets, UTC1900));
            }

            for (; i < savingsInstantTransitions.length; i++) {
                long trans = savingsInstantTransitions[i];
                if (trans > UTC2037) {
                    // no trans beyond LASTYEAR
                    lastyear = LASTYEAR;
                    break;
                }
                while (k < standardTransitions.length) {
                    // some standard offset transitions don't exist in
                    // savingInstantTrans, if the offset "change" doesn't
                    // really change the "effectiveWallOffset". For example
                    // the 1999/2000 pair in Zone Arg/Buenos_Aires, in which
                    // the daylightsaving "happened" but it actually does
                    // not result in the timezone switch. ZoneInfo however
                    // needs them in its transitions table
                    long trans_s = standardTransitions[k];
                    if (trans_s >= UTC1900) {
                        if (trans_s > trans)
                            break;
                        if (trans_s < trans) {
                            if (nOffsets + 2 >= offsets.length) {
                                offsets = Arrays.copyOf(offsets, offsets.length + 100);
                            }
                            if (nTrans + 1 >= transitions.length) {
                                transitions = Arrays.copyOf(transitions, transitions.length + 100);
                            }
                            nOffsets = addTrans(transitions, nTrans++, offsets, nOffsets,
                                                trans_s,
                                                wallOffsets[i],
                                                standardOffsets[k+1]);

                        }
                    }
                    k++;
                }
                if (nOffsets + 2 >= offsets.length) {
                    offsets = Arrays.copyOf(offsets, offsets.length + 100);
                }
                if (nTrans + 1 >= transitions.length) {
                    transitions = Arrays.copyOf(transitions, transitions.length + 100);
                }
                nOffsets = addTrans(transitions, nTrans++, offsets, nOffsets,
                                    trans,
                                    wallOffsets[i + 1],
                                    getStandardOffset(standardTransitions, standardOffsets, trans));

            }
            // append any leftover standard trans
            while (k < standardTransitions.length) {
                long trans = standardTransitions[k];
                if (trans >= UTC1900) {
                    int offset = wallOffsets[i];
                    int offsetIndex = indexOf(offsets, 0, nOffsets, offset);
                    if (offsetIndex == nOffsets)
                        nOffsets++;
                    transitions[nTrans++] = ((trans * 1000) << TRANSITION_NSHIFT) |
                                            (offsetIndex & OFFSET_MASK);
                }
                k++;
            }
            if (lastRules.length > 1) {
                // fill the gap between the last trans until LASTYEAR
                while (lastyear++ < LASTYEAR) {
                    for (ZoneOffsetTransitionRule zotr : lastRules) {
                        long trans = zotr.getTransitionEpochSecond(lastyear);
                        if (nOffsets + 2 >= offsets.length) {
                            offsets = Arrays.copyOf(offsets, offsets.length + 100);
                        }
                        if (nTrans + 1 >= transitions.length) {
                            transitions = Arrays.copyOf(transitions, transitions.length + 100);
                        }
                        nOffsets = addTrans(transitions, nTrans++,
                                            offsets, nOffsets,
                                            trans,
                                            zotr.offsetAfter,
                                            zotr.standardOffset);
                    }
                }
                ZoneOffsetTransitionRule startRule =  lastRules[lastRules.length - 2];
                ZoneOffsetTransitionRule endRule =  lastRules[lastRules.length - 1];
                params = new int[10];
                if (startRule.offsetAfter - startRule.offsetBefore < 0 &&
                    endRule.offsetAfter - endRule.offsetBefore > 0) {
                    ZoneOffsetTransitionRule tmp;
                    tmp = startRule;
                    startRule = endRule;
                    endRule = tmp;
                }
                params[0] = startRule.month - 1;
                int dom = startRule.dom;
                int dow = startRule.dow;
                if (dow == -1) {
                    params[1] = dom;
                    params[2] = 0;
                } else {
                    // ZoneRulesBuilder adjusts < 0 case (-1, for last, don't have
                    // "<=" case yet) to positive value if not February (it appears
                    // we don't have February cutoff in tzdata table yet)
                    // Ideally, if JSR310 can just pass in the nagative and
                    // we can then pass in the dom = -1, dow > 0 into ZoneInfo
                    //
                    // hacking, assume the >=24 is the result of ZRB optimization for
                    // "last", it works for now. From tzdata2020d this hacking
                    // will not work for Asia/Gaza and Asia/Hebron which follow
                    // Palestine DST rules.
                    if (dom < 0 || dom >= 24 &&
                                   !(zoneId.equals("Asia/Gaza") ||
                                     zoneId.equals("Asia/Hebron"))) {
                        params[1] = -1;
                        params[2] = toCalendarDOW[dow];
                    } else {
                        params[1] = dom;
                        // To specify a day of week on or after an exact day of month,
                        // set the month to an exact month value, day-of-month to the
                        // day on or after which the rule is applied, and day-of-week
                        // to a negative Calendar.DAY_OF_WEEK DAY_OF_WEEK field value.
                        params[2] = -toCalendarDOW[dow];
                    }
                }
                params[3] = startRule.secondOfDay * 1000;
                params[4] = toSTZTime[startRule.timeDefinition];
                params[5] = endRule.month - 1;
                dom = endRule.dom;
                dow = endRule.dow;
                if (dow == -1) {
                    params[6] = dom;
                    params[7] = 0;
                } else {
                    // hacking: see comment above
                    if (dom < 0 || dom >= 24 &&
                                   !(zoneId.equals("Asia/Gaza") ||
                                     zoneId.equals("Asia/Hebron"))) {
                        params[6] = -1;
                        params[7] = toCalendarDOW[dow];
                    } else {
                        params[6] = dom;
                        params[7] = -toCalendarDOW[dow];
                    }
                }
                params[8] = endRule.secondOfDay * 1000;
                params[9] = toSTZTime[endRule.timeDefinition];
                dstSavings = (startRule.offsetAfter - startRule.offsetBefore) * 1000;

                // Note: known mismatching -> Asia/Amman
                // ZoneInfo :      startDayOfWeek=5     <= Thursday
                //                 startTime=86400000   <= 24 hours
                // This:           startDayOfWeek=6
                //                 startTime=0
                // Similar workaround needs to be applied to Africa/Cairo and
                // its endDayOfWeek and endTime
                // Below is the workarounds, it probably slows down everyone a little
                if (params[2] == 6 && params[3] == 0 &&
                    (zoneId.equals("Asia/Amman"))) {
                    params[2] = 5;
                    params[3] = 86400000;
                }
                // Additional check for startDayOfWeek=6 and starTime=86400000
                // is needed for Asia/Amman;
                if (params[2] == 7 && params[3] == 0 &&
                     (zoneId.equals("Asia/Amman"))) {
                    params[2] = 6;        // Friday
                    params[3] = 86400000; // 24h
                }
                //endDayOfWeek and endTime workaround
                if (params[7] == 6 && params[8] == 0 &&
                    (zoneId.equals("Africa/Cairo"))) {
                    params[7] = 5;
                    params[8] = 86400000;
                }

            } else if (nTrans > 0) {  // only do this if there is something in table already
                if (lastyear < LASTYEAR) {
                    // ZoneInfo has an ending entry for 2037
                    //long trans = OffsetDateTime.of(LASTYEAR, 1, 1, 0, 0, 0, 0,
                    //                               ZoneOffset.ofTotalSeconds(rawOffset/1000))
                    //                           .toEpochSecond();
                    long trans = LDT2037 - rawOffset/1000;

                    int offsetIndex = indexOf(offsets, 0, nOffsets, rawOffset/1000);
                    if (offsetIndex == nOffsets)
                        nOffsets++;
                    transitions[nTrans++] = (trans * 1000) << TRANSITION_NSHIFT |
                                       (offsetIndex & OFFSET_MASK);

                } else if (savingsInstantTransitions.length > 2) {
                    // Workaround: create the params based on the last pair for
                    // zones like Israel and Iran which have trans defined
                    // up until 2037, but no "transition rule" defined
                    //
                    // Note: Known mismatching for Israel, Asia/Jerusalem/Tel Aviv
                    // ZoneInfo:        startMode=3
                    //                  startMonth=2
                    //                  startDay=26
                    //                  startDayOfWeek=6
                    //
                    // This:            startMode=1
                    //                  startMonth=2
                    //                  startDay=27
                    //                  startDayOfWeek=0
                    // these two are actually the same for 2037, the SimpleTimeZone
                    // for the last "known" year
                    int m = savingsInstantTransitions.length;
                    long startTrans = savingsInstantTransitions[m - 2];
                    int startOffset = wallOffsets[m - 2 + 1];
                    int startStd = getStandardOffset(standardTransitions, standardOffsets, startTrans);
                    long endTrans =  savingsInstantTransitions[m - 1];
                    int endOffset = wallOffsets[m - 1 + 1];
                    int endStd = getStandardOffset(standardTransitions, standardOffsets, endTrans);
                    if (startOffset > startStd && endOffset == endStd) {
                        // last - 1 trans
                        m = savingsInstantTransitions.length - 2;
                        ZoneOffset before = ZoneOffset.ofTotalSeconds(wallOffsets[m]);
                        ZoneOffset after = ZoneOffset.ofTotalSeconds(wallOffsets[m + 1]);
                        LocalDateTime ldt = LocalDateTime.ofEpochSecond(savingsInstantTransitions[m], 0, before);
                        LocalDateTime startLDT;
                        if (after.getTotalSeconds() > before.getTotalSeconds()) {  // isGap()
                            startLDT = ldt;
                        } else {
                            startLDT = ldt.plusSeconds(wallOffsets[m + 1] - wallOffsets[m]);
                        }
                        // last trans
                        m = savingsInstantTransitions.length - 1;
                        before = ZoneOffset.ofTotalSeconds(wallOffsets[m]);
                        after = ZoneOffset.ofTotalSeconds(wallOffsets[m + 1]);
                        ldt = LocalDateTime.ofEpochSecond(savingsInstantTransitions[m], 0, before);
                        LocalDateTime endLDT;
                        if (after.getTotalSeconds() > before.getTotalSeconds()) {  // isGap()
                            endLDT = ldt.plusSeconds(wallOffsets[m + 1] - wallOffsets[m]);
                        } else {
                            endLDT = ldt;
                        }
                        params = new int[10];
                        params[0] = startLDT.getMonthValue() - 1;
                        params[1] = startLDT.getDayOfMonth();
                        params[2] = 0;
                        params[3] = startLDT.toLocalTime().toSecondOfDay() * 1000;
                        params[4] = SimpleTimeZone.WALL_TIME;
                        params[5] = endLDT.getMonthValue() - 1;
                        params[6] = endLDT.getDayOfMonth();
                        params[7] = 0;
                        params[8] = endLDT.toLocalTime().toSecondOfDay() * 1000;
                        params[9] = SimpleTimeZone.WALL_TIME;
                        dstSavings = (startOffset - startStd) * 1000;
                    }
                }
            }
            if (transitions != null && transitions.length != nTrans) {
                if (nTrans == 0) {
                    transitions = null;
                } else {
                    transitions = Arrays.copyOf(transitions, nTrans);
                }
            }
            if (offsets != null && offsets.length != nOffsets) {
                if (nOffsets == 0) {
                    offsets = null;
                } else {
                    offsets = Arrays.copyOf(offsets, nOffsets);
                }
            }
            if (transitions != null) {
                Checksum sum = new Checksum();
                for (i = 0; i < transitions.length; i++) {
                    long val = transitions[i];
                    int dst = (int)((val >>> DST_NSHIFT) & 0xfL);
                    int saving = (dst == 0) ? 0 : offsets[dst];
                    int index = (int)(val & OFFSET_MASK);
                    int offset = offsets[index];
                    long second = (val >> TRANSITION_NSHIFT);
                    // javazic uses "index of the offset in offsets",
                    // instead of the real offset value itself to
                    // calculate the checksum. Have to keep doing
                    // the same thing, checksum is part of the
                    // ZoneInfo serialization form.
                    sum.update(second + index);
                    sum.update(index);
                    sum.update(dst == 0 ? -1 : dst);
                }
                checksum = (int)sum.getValue();
            }
        }
        return new ZoneInfo(zoneId, rawOffset, dstSavings, checksum, transitions,
                            offsets, params, willGMTOffsetChange);
    }

    private static int getStandardOffset(long[] standardTransitions,
                                         int[] standardOffsets,
                                         long epochSec) {
        // The size of stdOffsets is [0..9], with most are
        // [1..4] entries , simple loop search is faster
        //
        // int index  = Arrays.binarySearch(standardTransitions, epochSec);
        // if (index < 0) {
        //    // switch negative insert position to start of matched range
        //    index = -index - 2;
        // }
        // return standardOffsets[index + 1];
        int index = 0;
        for (; index < standardTransitions.length; index++) {
            if (epochSec < standardTransitions[index]) {
                break;
            }
        }
        return standardOffsets[index];
    }

    static final int SECONDS_PER_DAY = 86400;
    static final int DAYS_PER_CYCLE = 146097;
    static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

    private static int getYear(long epochSecond, int offset) {
        long second = epochSecond + offset;  // overflow caught later
        long epochDay = Math.floorDiv(second, SECONDS_PER_DAY);
        long zeroDay = epochDay + DAYS_0000_TO_1970;
        // find the march-based year
        zeroDay -= 60;  // adjust to 0000-03-01 so leap day is at end of four year cycle
        long adjust = 0;
        if (zeroDay < 0) {
            // adjust negative years to positive for calculation
            long adjustCycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1;
            adjust = adjustCycles * 400;
            zeroDay += -adjustCycles * DAYS_PER_CYCLE;
        }
        long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
        long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        if (doyEst < 0) {
            // fix estimate
            yearEst--;
            doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
        }
        yearEst += adjust;  // reset any negative year
        int marchDoy0 = (int) doyEst;
        // convert march-based values back to january-based
        int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
        int month = (marchMonth0 + 2) % 12 + 1;
        int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
        yearEst += marchMonth0 / 10;
        return (int)yearEst;
    }

    private static final int toCalendarDOW[] = new int[] {
        -1,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY
    };

    private static final int toSTZTime[] = new int[] {
        SimpleTimeZone.UTC_TIME,
        SimpleTimeZone.WALL_TIME,
        SimpleTimeZone.STANDARD_TIME,
    };

    private static final long OFFSET_MASK = 0x0fL;
    private static final long DST_MASK = 0xf0L;
    private static final int  DST_NSHIFT = 4;
    private static final int  TRANSITION_NSHIFT = 12;
    private static final int  LASTYEAR = 2037;

    // from: 0 for offset lookup, 1 for dstsvings lookup
    private static int indexOf(int[] offsets, int from, int nOffsets, int offset) {
        offset *= 1000;
        for (; from < nOffsets; from++) {
            if (offsets[from] == offset)
                return from;
        }
        offsets[from] = offset;
        return from;
    }

    // return updated nOffsets
    private static int addTrans(long transitions[], int nTrans,
                                int offsets[], int nOffsets,
                                long trans, int offset, int stdOffset) {
        int offsetIndex = indexOf(offsets, 0, nOffsets, offset);
        if (offsetIndex == nOffsets)
            nOffsets++;
        int dstIndex = 0;
        if (offset != stdOffset) {
            dstIndex = indexOf(offsets, 1, nOffsets, offset - stdOffset);
            if (dstIndex == nOffsets)
                nOffsets++;
        }
        transitions[nTrans] = ((trans * 1000) << TRANSITION_NSHIFT) |
                              ((dstIndex << DST_NSHIFT) & DST_MASK) |
                              (offsetIndex & OFFSET_MASK);
        return nOffsets;
    }

    // ZoneInfo checksum, copy/pasted from javazic
    private static class Checksum extends CRC32 {
        public void update(int val) {
            byte[] b = new byte[4];
            b[0] = (byte)(val >>> 24);
            b[1] = (byte)(val >>> 16);
            b[2] = (byte)(val >>> 8);
            b[3] = (byte)(val);
            update(b);
        }
        void update(long val) {
            byte[] b = new byte[8];
            b[0] = (byte)(val >>> 56);
            b[1] = (byte)(val >>> 48);
            b[2] = (byte)(val >>> 40);
            b[3] = (byte)(val >>> 32);
            b[4] = (byte)(val >>> 24);
            b[5] = (byte)(val >>> 16);
            b[6] = (byte)(val >>> 8);
            b[7] = (byte)(val);
            update(b);
        }
    }

    // A simple/raw version of j.t.ZoneOffsetTransitionRule
    private static class ZoneOffsetTransitionRule {
        private final int month;
        private final byte dom;
        private final int dow;
        private final int secondOfDay;
        private final boolean timeEndOfDay;
        private final int timeDefinition;
        private final int standardOffset;
        private final int offsetBefore;
        private final int offsetAfter;

        ZoneOffsetTransitionRule(DataInput in) throws IOException {
            int data = in.readInt();
            int dowByte = (data & (7 << 19)) >>> 19;
            int timeByte = (data & (31 << 14)) >>> 14;
            int stdByte = (data & (255 << 4)) >>> 4;
            int beforeByte = (data & (3 << 2)) >>> 2;
            int afterByte = (data & 3);

            this.month = data >>> 28;
            this.dom = (byte)(((data & (63 << 22)) >>> 22) - 32);
            this.dow = dowByte == 0 ? -1 : dowByte;
            this.secondOfDay = timeByte == 31 ? in.readInt() : timeByte * 3600;
            this.timeEndOfDay = timeByte == 24;
            this.timeDefinition = (data & (3 << 12)) >>> 12;

            this.standardOffset = stdByte == 255 ? in.readInt() : (stdByte - 128) * 900;
            this.offsetBefore = beforeByte == 3 ? in.readInt() : standardOffset + beforeByte * 1800;
            this.offsetAfter = afterByte == 3 ? in.readInt() : standardOffset + afterByte * 1800;
        }

        long getTransitionEpochSecond(int year) {
            long epochDay = 0;
            if (dom < 0) {
                epochDay = toEpochDay(year, month, lengthOfMonth(year, month) + 1 + dom);
                if (dow != -1) {
                    epochDay = previousOrSame(epochDay, dow);
                }
            } else {
                epochDay = toEpochDay(year, month, dom);
                if (dow != -1) {
                    epochDay = nextOrSame(epochDay, dow);
                }
            }
            if (timeEndOfDay) {
                epochDay += 1;
            }
            int difference = 0;
            switch (timeDefinition) {
                case 0:    // UTC
                    difference = 0;
                    break;
                case 1:    // WALL
                    difference = -offsetBefore;
                    break;
                case 2:    //STANDARD
                    difference = -standardOffset;
                    break;
            }
            return epochDay * 86400 + secondOfDay + difference;
        }

        static final boolean isLeapYear(int year) {
            return ((year & 3) == 0) && ((year % 100) != 0 || (year % 400) == 0);
        }

        static final int lengthOfMonth(int year, int month) {
            switch (month) {
                case 2:        //FEBRUARY:
                    return isLeapYear(year)? 29 : 28;
                case 4:        //APRIL:
                case 6:        //JUNE:
                case 9:        //SEPTEMBER:
                case 11:       //NOVEMBER:
                    return 30;
                default:
                    return 31;
            }
        }

        static final long toEpochDay(int year, int month, int day) {
            long y = year;
            long m = month;
            long total = 0;
            total += 365 * y;
            if (y >= 0) {
                total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
            } else {
                total -= y / -4 - y / -100 + y / -400;
            }
            total += ((367 * m - 362) / 12);
            total += day - 1;
            if (m > 2) {
                total--;
                if (!isLeapYear(year)) {
                    total--;
                }
            }
            return total - DAYS_0000_TO_1970;
        }

        static final long previousOrSame(long epochDay, int dayOfWeek) {
            return adjust(epochDay, dayOfWeek, 1);
        }

        static final long nextOrSame(long epochDay, int dayOfWeek) {
           return adjust(epochDay, dayOfWeek, 0);
        }

        static final long adjust(long epochDay, int dow, int relative) {
            int calDow = (int)Math.floorMod(epochDay + 3, 7L) + 1;
            if (relative < 2 && calDow == dow) {
                return epochDay;
            }
            if ((relative & 1) == 0) {
                int daysDiff = calDow - dow;
                return epochDay + (daysDiff >= 0 ? 7 - daysDiff : -daysDiff);
            } else {
                int daysDiff = dow - calDow;
                return epochDay - (daysDiff >= 0 ? 7 - daysDiff : -daysDiff);
            }
        }
    }
}
