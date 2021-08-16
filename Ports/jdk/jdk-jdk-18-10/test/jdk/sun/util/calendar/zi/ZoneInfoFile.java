/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sun.util.calendar.*;

/**
 * <code>ZoneInfoFile</code> reads Zone information files in the
 * &lt;java.home&gt;/lib/zi directory and provides time zone
 * information in the form of a {@link ZoneInfo} object. Also, it
 * reads the ZoneInfoMappings file to obtain time zone IDs information
 * that is used by the {@link ZoneInfo} class. The directory layout
 * and data file formats are as follows.
 *
 * <p><strong>Directory layout</strong><p>
 *
 * All zone data files and ZoneInfoMappings are put under the
 * &lt;java.home&gt;/lib/zi directory. A path name for a given time
 * zone ID is a concatenation of &lt;java.home&gt;/lib/zi/ and the
 * time zone ID. (The file separator is replaced with the platform
 * dependent value. e.g., '\' for Win32.) An example layout will look
 * like as follows.
 * <blockquote>
 * <pre>
 * &lt;java.home&gt;/lib/zi/Africa/Addis_Ababa
 *                   /Africa/Dakar
 *                   /America/Los_Angeles
 *                   /Asia/Singapore
 *                   /EET
 *                   /Europe/Oslo
 *                   /GMT
 *                   /Pacific/Galapagos
 *                       ...
 *                   /ZoneInfoMappings
 * </pre>
 * </blockquote>
 *
 * A zone data file has specific information of each zone.
 * <code>ZoneInfoMappings</code> has global information of zone IDs so
 * that the information can be obtained without instantiating all time
 * zones.
 *
 * <p><strong>File format</strong><p>
 *
 * Two binary-file formats based on a simple Tag-Length-Value format are used
 * to describe TimeZone information. The generic format of a data file is:
 * <blockquote>
 * <pre>
 *    DataFile {
 *      u1              magic[7];
 *      u1              version;
 *      data_item       data[];
 *    }
 * </pre>
 * </blockquote>
 * where <code>magic</code> is a magic number identifying a file
 * format, <code>version</code> is the format version number, and
 * <code>data</code> is one or more <code>data_item</code>s. The
 * <code>data_item</code> structure is:
 * <blockquote>
 * <pre>
 *    data_item {
 *      u1              tag;
 *      u2              length;
 *      u1              value[length];
 *    }
 * </pre>
 * </blockquote>
 * where <code>tag</code> indicates the data type of the item,
 * <code>length</code> is a byte count of the following
 * <code>value</code> that is the content of item data.
 * <p>
 * All data is stored in the big-endian order. There is no boundary
 * alignment between date items.
 *
 * <p><strong>1. ZoneInfo data file</strong><p>
 *
 * Each ZoneInfo data file consists of the following members.
 * <br>
 * <blockquote>
 * <pre>
 *    ZoneInfoDataFile {
 *      u1              magic[7];
 *      u1              version;
 *      SET OF<sup>1</sup> {
 *        transition            transitions<sup>2</sup>;
 *        offset_table          offsets<sup>2</sup>;
 *        simpletimezone        stzparams<sup>2</sup>;
 *        raw_offset            rawoffset;
 *        dstsaving             dst;
 *        checksum              crc32;
 *        gmtoffsetwillchange   gmtflag<sup>2</sup>;
 *      }
 *   }
 *   1: an unordered collection of zero or one occurrences of each item
 *   2: optional item
 * </pre>
 * </blockquote>
 * <code>magic</code> is a byte-string constant identifying the
 * ZoneInfo data file.  This field must be <code>"javazi&#92;0"</code>
 * defined as {@link #JAVAZI_LABEL}.
 * <p>
 * <code>version</code> is the version number of the file format. This
 * will be used for compatibility check. This field must be
 * <code>0x01</code> in this version.
 * <p>
 * <code>transition</code>, <code>offset_table</code> and
 * <code>simpletimezone</code> have information of time transition
 * from the past to the future.  Therefore, these structures don't
 * exist if the zone didn't change zone names and haven't applied DST in
 * the past, and haven't planned to apply it.  (e.g. Asia/Tokyo zone)
 * <p>
 * <code>raw_offset</code>, <code>dstsaving</code> and <code>checksum</code>
 * exist in every zoneinfo file. They are used by TimeZone.class indirectly.
 *
 * <p><strong>1.1 <code>transition</code> structure</strong><p><a name="transition"></a>
 * <blockquote>
 * <pre>
 *    transition {
 *      u1      tag;              // 0x04 : constant
 *      u2      length;           // byte length of whole values
 *      s8      value[length/8];  // transitions in `long'
 *    }
 * </pre>
 * </blockquote>
 * See {@link ZoneInfo#transitions ZoneInfo.transitions} about the value.
 *
 * <p><strong>1.2 <code>offset_table</code> structure</strong><p>
 * <blockquote>
 * <pre>
 *    offset_table {
 *      u1      tag;              // 0x05 : constant
 *      u2      length;           // byte length of whole values
 *      s4      value[length/4];  // offset values in `int'
 *    }
 * </pre>
 * </blockquote>
 *
 * <p><strong>1.3 <code>simpletimezone</code> structure</strong><p>
 * See {@link ZoneInfo#simpleTimeZoneParams ZoneInfo.simpleTimeZoneParams}
 * about the value.
 * <blockquote>
 * <pre>
 *    simpletimezone {
 *      u1      tag;              // 0x06 : constant
 *      u2      length;           // byte length of whole values
 *      s4      value[length/4];  // SimpleTimeZone parameters
 *    }
 * </pre>
 * </blockquote>
 * See {@link ZoneInfo#offsets ZoneInfo.offsets} about the value.
 *
 * <p><strong>1.4 <code>raw_offset</code> structure</strong><p>
 * <blockquote>
 * <pre>
 *    raw_offset {
 *      u1      tag;              // 0x01 : constant
 *      u2      length;           // must be 4.
 *      s4      value;            // raw GMT offset [millisecond]
 *    }
 * </pre>
 * </blockquote>
 * See {@link ZoneInfo#rawOffset ZoneInfo.rawOffset} about the value.
 *
 * <p><strong>1.5 <code>dstsaving</code> structure</strong><p>
 * Value has dstSaving in seconds.
 * <blockquote>
 * <pre>
 *    dstsaving {
 *      u1      tag;              // 0x02 : constant
 *      u2      length;           // must be 2.
 *      s2      value;            // DST save value [second]
 *    }
 * </pre>
 * </blockquote>
 * See {@link ZoneInfo#dstSavings ZoneInfo.dstSavings} about value.
 *
 * <p><strong>1.6 <code>checksum</code> structure</strong><p>
 * <blockquote>
 * <pre>
 *    checksum {
 *      u1      tag;              // 0x03 : constant
 *      u2      length;           // must be 4.
 *      s4      value;            // CRC32 value of transitions
 *    }
 * </pre>
 * </blockquote>
 * See {@link ZoneInfo#checksum ZoneInfo.checksum}.
 *
 * <p><strong>1.7 <code>gmtoffsetwillchange</code> structure</strong><p>
 * This record has a flag value for {@link ZoneInfo#rawOffsetWillChange}.
 * If this record is not present in a zoneinfo file, 0 is assumed for
 * the value.
 * <blockquote>
 * <pre>
 *    gmtoffsetwillchange {
 *      u1      tag;             // 0x07 : constant
 *      u2      length;          // must be 1.
 *      u1      value;           // 1: if the GMT raw offset will change
 *                               // in the future, 0, otherwise.
 *     }
 * </pre>
 * </blockquote>
 *
 *
 * <p><strong>2. ZoneInfoMappings file</strong><p>
 *
 * The ZoneInfoMappings file consists of the following members.
 * <br>
 * <blockquote>
 * <pre>
 *    ZoneInfoMappings {
 *      u1      magic[7];
 *      u1      version;
 *      SET OF {
 *        versionName                   version;
 *        zone_id_table                 zoneIDs;
 *        raw_offset_table              rawoffsets;
 *        raw_offset_index_table        rawoffsetindices;
 *        alias_table                   aliases;
 *        excluded_list                 excludedList;
 *      }
 *   }
 * </pre>
 * </blockquote>
 *
 * <code>magic</code> is a byte-string constant which has the file type.
 * This field must be <code>"javazm&#92;0"</code> defined as {@link #JAVAZM_LABEL}.
 * <p>
 * <code>version</code> is the version number of this file
 * format. This will be used for compatibility check. This field must
 * be <code>0x01</code> in this version.
 * <p>
 * <code>versionName</code> shows which version of Olson's data has been used
 * to generate this ZoneInfoMappings. (e.g. <code>tzdata2000g</code>) <br>
 * This field is for trouble-shooting and isn't usually used in runtime.
 * <p>
 * <code>zone_id_table</code>, <code>raw_offset_index_table</code> and
 * <code>alias_table</code> are general information of supported
 * zones.
 *
 * <p><strong>2.1 <code>zone_id_table</code> structure</strong><p>
 * The list of zone IDs included in the zi database. The list does
 * <em>not</em> include zone IDs, if any, listed in excludedList.
 * <br>
 * <blockquote>
 * <pre>
 *    zone_id_table {
 *      u1      tag;              // 0x40 : constant
 *      u2      length;           // byte length of whole values
 *      u2      zone_id_count;
 *      zone_id value[zone_id_count];
 *    }
 *
 *    zone_id {
 *      u1      byte_length;      // byte length of id
 *      u1      id[byte_length];  // zone name string
 *    }
 * </pre>
 * </blockquote>
 *
 * <p><strong>2.2 <code>raw_offset_table</code> structure</strong><p>
 * <br>
 * <blockquote>
 * <pre>
 *    raw_offset_table {
 *      u1      tag;              // 0x41 : constant
 *      u2      length;           // byte length of whole values
 *      s4      value[length/4];  // raw GMT offset in milliseconds
 *   }
 * </pre>
 * </blockquote>
 *
 * <p><strong>2.3 <code>raw_offset_index_table</code> structure</strong><p>
 * <br>
 * <blockquote>
 * <pre>
 *    raw_offset_index_table {
 *      u1      tag;              // 0x42 : constant
 *      u2      length;           // byte length of whole values
 *      u1      value[length];
 *    }
 * </pre>
 * </blockquote>
 *
 * <p><strong>2.4 <code>alias_table</code> structure</strong><p>
 * <br>
 * <blockquote>
 * <pre>
 *   alias_table {
 *      u1      tag;              // 0x43 : constant
 *      u2      length;           // byte length of whole values
 *      u2      nentries;         // number of id-pairs
 *      id_pair value[nentries];
 *   }
 *
 *   id_pair {
 *      zone_id aliasname;
 *      zone_id ID;
 *   }
 * </pre>
 * </blockquote>
 *
 * <p><strong>2.5 <code>versionName</code> structure</strong><p>
 * <br>
 * <blockquote>
 * <pre>
 *   versionName {
 *      u1      tag;              // 0x44 : constant
 *      u2      length;           // byte length of whole values
 *      u1      value[length];
 *   }
 * </pre>
 * </blockquote>
 *
 * <p><strong>2.6 <code>excludeList</code> structure</strong><p>
 * The list of zone IDs whose zones will change their GMT offsets
 * (a.k.a. raw offsets) some time in the future. Those IDs must be
 * added to the list of zone IDs for getAvailableIDs(). Also they must
 * be examined for getAvailableIDs(int) to determine the
 * <em>current</em> GMT offsets.
 * <br>
 * <blockquote>
 * <pre>
 *   excluded_list {
 *      u1      tag;              // 0x45 : constant
 *      u2      length;           // byte length of whole values
 *      u2      nentries;         // number of zone_ids
 *      zone_id value[nentries];  // excluded zone IDs
 *   }
 * </pre>
 * </blockquote>
 *
 * @since 1.4
 */

public class ZoneInfoFile {

    /**
     * The magic number for the ZoneInfo data file format.
     */
    public static final byte[]  JAVAZI_LABEL = {
        (byte)'j', (byte)'a', (byte)'v', (byte)'a', (byte)'z', (byte)'i', (byte)'\0'
    };
    private static final int    JAVAZI_LABEL_LENGTH = JAVAZI_LABEL.length;

    /**
     * The ZoneInfo data file format version number. Must increase
     * one when any incompatible change has been made.
     */
    public static final byte    JAVAZI_VERSION = 0x01;

    /**
     * Raw offset data item tag.
     */
    public static final byte    TAG_RawOffset = 1;

    /**
     * Known last Daylight Saving Time save value data item tag.
     */
    public static final byte    TAG_LastDSTSaving = 2;

    /**
     * Checksum data item tag.
     */
    public static final byte    TAG_CRC32 = 3;

    /**
     * Transition data item tag.
     */
    public static final byte    TAG_Transition = 4;

    /**
     * Offset table data item tag.
     */
    public static final byte    TAG_Offset = 5;

    /**
     * SimpleTimeZone parameters data item tag.
     */
    public static final byte    TAG_SimpleTimeZone = 6;

    /**
     * Raw GMT offset will change in the future.
     */
    public static final byte    TAG_GMTOffsetWillChange = 7;


    /**
     * The ZoneInfoMappings file name.
     */
    public static final String  JAVAZM_FILE_NAME = "ZoneInfoMappings";

    /**
     * The magic number for the ZoneInfoMappings file format.
     */
    public static final byte[]  JAVAZM_LABEL = {
        (byte)'j', (byte)'a', (byte)'v', (byte)'a', (byte)'z', (byte)'m', (byte)'\0'
    };
    private static final int    JAVAZM_LABEL_LENGTH = JAVAZM_LABEL.length;

    /**
     * The ZoneInfoMappings file format version number. Must increase
     * one when any incompatible change has been made.
     */
    public static final byte    JAVAZM_VERSION = 0x01;

    /**
     * Time zone IDs data item tag.
     */
    public static final byte    TAG_ZoneIDs = 64;

    /**
     * Raw GMT offsets table data item tag.
     */
    public static final byte    TAG_RawOffsets = 65;

    /**
     * Indices to the raw GMT offset table data item tag.
     */
    public static final byte    TAG_RawOffsetIndices = 66;

    /**
     * Time zone aliases table data item tag.
     */
    public static final byte    TAG_ZoneAliases = 67;

    /**
     * Olson's public zone information version tag.
     */
    public static final byte    TAG_TZDataVersion = 68;

    /**
     * Excluded zones item tag. (Added in Mustang)
     */
    public static final byte    TAG_ExcludedZones = 69;

    private static Map<String, ZoneInfoOld> zoneInfoObjects = null;

    private static final ZoneInfoOld GMT = new ZoneInfoOld("GMT", 0);

    static String ziDir;

    /**
     * Converts the given time zone ID to a platform dependent path
     * name. For example, "America/Los_Angeles" is converted to
     * "America\Los_Angeles" on Win32.
     * @return a modified ID replacing '/' with {@link
     * java.io.File#separatorChar File.separatorChar} if needed.
     */
    public static String getFileName(String ID) {
        if (File.separatorChar == '/') {
            return ID;
        }
        return ID.replace('/', File.separatorChar);
    }

    /**
     * Gets a ZoneInfo with the given GMT offset. The object
     * has its ID in the format of GMT{+|-}hh:mm.
     *
     * @param originalId the given custom id (before normalized such as "GMT+9")
     * @param gmtOffset GMT offset <em>in milliseconds</em>
     * @return a ZoneInfo constructed with the given GMT offset
     */
    public static ZoneInfoOld getCustomTimeZone(String originalId, int gmtOffset) {
        String id = toCustomID(gmtOffset);

        ZoneInfoOld zi = getFromCache(id);
        if (zi == null) {
            zi = new ZoneInfoOld(id, gmtOffset);
            zi = addToCache(id, zi);
            if (!id.equals(originalId)) {
                zi = addToCache(originalId, zi);
            }
        }
        return (ZoneInfoOld) zi.clone();
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

    /**
     * @return a ZoneInfo instance created for the specified id, or
     * null if there is no time zone data file found for the specified
     * id.
     */
    public static ZoneInfoOld getZoneInfoOld(String id) {
        //treat GMT zone as special
        if ("GMT".equals(id))
            return (ZoneInfoOld) GMT.clone();
        ZoneInfoOld zi = getFromCache(id);
        if (zi == null) {
            Map<String, String> aliases = ZoneInfoOld.getCachedAliasTable();
            if (aliases != null && aliases.get(id) != null) {
                return null;
            }
            zi = createZoneInfoOld(id);
            if (zi == null) {
                return null;
            }
            zi = addToCache(id, zi);
        }
        return (ZoneInfoOld) zi.clone();
    }

    synchronized static ZoneInfoOld getFromCache(String id) {
        if (zoneInfoObjects == null) {
            return null;
        }
        return zoneInfoObjects.get(id);
    }

    synchronized static ZoneInfoOld addToCache(String id, ZoneInfoOld zi) {
        if (zoneInfoObjects == null) {
            zoneInfoObjects = new HashMap<>();
        } else {
            ZoneInfoOld zone = zoneInfoObjects.get(id);
            if (zone != null) {
                return zone;
            }
        }
        zoneInfoObjects.put(id, zi);
        return zi;
    }

    private static ZoneInfoOld createZoneInfoOld(String id) {
        byte[] buf = readZoneInfoFile(getFileName(id));
        if (buf == null) {
            return null;
        }

        int index = 0;
        int filesize = buf.length;
        int rawOffset = 0;
        int dstSavings = 0;
        int checksum = 0;
        boolean willGMTOffsetChange = false;
        long[] transitions = null;
        int[] offsets = null;
        int[] simpleTimeZoneParams = null;

        try {
            for (index = 0; index < JAVAZI_LABEL.length; index++) {
                if (buf[index] != JAVAZI_LABEL[index]) {
                    System.err.println("ZoneInfoOld: wrong magic number: " + id);
                    return null;
                }
            }
            if (buf[index++] > JAVAZI_VERSION) {
                System.err.println("ZoneInfo: incompatible version ("
                                   + buf[index - 1] + "): " + id);
                return null;
            }

            while (index < filesize) {
                byte tag = buf[index++];
                int  len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                if (filesize < index+len) {
                    break;
                }

                switch (tag) {
                case TAG_CRC32:
                    {
                        int val = buf[index++] & 0xff;
                        val = (val << 8) + (buf[index++] & 0xff);
                        val = (val << 8) + (buf[index++] & 0xff);
                        val = (val << 8) + (buf[index++] & 0xff);
                        checksum = val;
                    }
                    break;

                case TAG_LastDSTSaving:
                    {
                        short val = (short)(buf[index++] & 0xff);
                        val = (short)((val << 8) + (buf[index++] & 0xff));
                        dstSavings = val * 1000;
                    }
                    break;

                case TAG_RawOffset:
                    {
                        int val = buf[index++] & 0xff;
                        val = (val << 8) + (buf[index++] & 0xff);
                        val = (val << 8) + (buf[index++] & 0xff);
                        val = (val << 8) + (buf[index++] & 0xff);
                        rawOffset = val;
                    }
                    break;

                case TAG_Transition:
                    {
                        int n = len / 8;
                        transitions = new long[n];
                        for (int i = 0; i < n; i ++) {
                            long val = buf[index++] & 0xff;
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            transitions[i] = val;
                        }
                    }
                    break;

                case TAG_Offset:
                    {
                        int n = len / 4;
                        offsets = new int[n];
                        for (int i = 0; i < n; i ++) {
                            int val = buf[index++] & 0xff;
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            offsets[i] = val;
                        }
                    }
                    break;

                case TAG_SimpleTimeZone:
                    {
                        if (len != 32 && len != 40) {
                            System.err.println("ZoneInfo: wrong SimpleTimeZone parameter size");
                            return null;
                        }
                        int n = len / 4;
                        simpleTimeZoneParams = new int[n];
                        for (int i = 0; i < n; i++) {
                            int val = buf[index++] & 0xff;
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            simpleTimeZoneParams[i] = val;
                        }
                    }
                    break;

                case TAG_GMTOffsetWillChange:
                    {
                        if (len != 1) {
                            System.err.println("ZoneInfo: wrong byte length for TAG_GMTOffsetWillChange");
                        }
                        willGMTOffsetChange = buf[index++] == 1;
                    }
                    break;

                default:
                    System.err.println("ZoneInfo: unknown tag < " + tag + ">. ignored.");
                    index += len;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("ZoneInfo: corrupted zoneinfo file: " + id);
            return null;
        }

        if (index != filesize) {
            System.err.println("ZoneInfo: wrong file size: " + id);
            return null;
        }

        return new ZoneInfoOld(id, rawOffset, dstSavings, checksum,
                            transitions, offsets, simpleTimeZoneParams,
                            willGMTOffsetChange);
    }

    private volatile static SoftReference<List<String>> zoneIDs = null;

    static List<String> getZoneIDs() {
        List<String> ids = null;
        SoftReference<List<String>> cache = zoneIDs;
        if (cache != null) {
            ids = cache.get();
            if (ids != null) {
                return ids;
            }
        }
        byte[] buf = null;
        buf = getZoneInfoOldMappings();
        int index = JAVAZM_LABEL_LENGTH + 1;
        int filesize = buf.length;
        try {
        loop:
            while (index < filesize) {
                byte tag = buf[index++];
                int     len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                switch (tag) {
                case TAG_ZoneIDs:
                    {
                        int n = (buf[index++] << 8) + (buf[index++] & 0xFF);
                        ids = new ArrayList<>(n);

                        for (int i = 0; i < n; i++) {
                            byte m = buf[index++];
                            ids.add(new String(buf, index, m, "UTF-8"));
                            index += m;
                        }
                    }
                    break loop;

                default:
                    index += len;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("ZoneInfoOld: corrupted " + JAVAZM_FILE_NAME);
        }

        zoneIDs = new SoftReference<>(ids);
        return ids;
    }

    /**
     * @return an alias table in HashMap where a key is an alias ID
     * (e.g., "PST") and its value is a real time zone ID (e.g.,
     * "America/Los_Angeles").
     */
    static Map<String, String> getZoneAliases() {
        byte[] buf = getZoneInfoOldMappings();
        int index = JAVAZM_LABEL_LENGTH + 1;
        int filesize = buf.length;
        Map<String, String> aliases = null;

        try {
        loop:
            while (index < filesize) {
                byte tag = buf[index++];
                int     len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                switch (tag) {
                case TAG_ZoneAliases:
                    {
                        int n = (buf[index++] << 8) + (buf[index++] & 0xFF);
                        aliases = new HashMap<>(n);
                        for (int i = 0; i < n; i++) {
                            byte m = buf[index++];
                            String name = new String(buf, index, m, "UTF-8");
                            index += m;
                            m = buf[index++];
                            String realName = new String(buf, index, m, "UTF-8");
                            index += m;
                            aliases.put(name, realName);
                        }
                    }
                    break loop;

                default:
                    index += len;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("ZoneInfoOld: corrupted " + JAVAZM_FILE_NAME);
            return null;
        }
        return aliases;
    }

    private volatile static SoftReference<List<String>> excludedIDs = null;
    private volatile static boolean hasNoExcludeList = false;

    /**
     * @return a List of zone IDs for zones that will change their GMT
     * offsets in some future time.
     *
     * @since 1.6
     */
    static List<String> getExcludedZones() {
        if (hasNoExcludeList) {
            return null;
        }

        List<String> excludeList = null;

        SoftReference<List<String>> cache = excludedIDs;
        if (cache != null) {
            excludeList = cache.get();
            if (excludeList != null) {
                return excludeList;
            }
        }

        byte[] buf = getZoneInfoOldMappings();
        int index = JAVAZM_LABEL_LENGTH + 1;
        int filesize = buf.length;

        try {
          loop:
            while (index < filesize) {
                byte tag = buf[index++];
                int     len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                switch (tag) {
                case TAG_ExcludedZones:
                    {
                        int n = (buf[index++] << 8) + (buf[index++] & 0xFF);
                        excludeList = new ArrayList<>();
                        for (int i = 0; i < n; i++) {
                            byte m = buf[index++];
                            String name = new String(buf, index, m, "UTF-8");
                            index += m;
                            excludeList.add(name);
                        }
                    }
                    break loop;

                default:
                    index += len;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("ZoneInfoOld: corrupted " + JAVAZM_FILE_NAME);
            return null;
        }

        if (excludeList != null) {
            excludedIDs = new SoftReference<>(excludeList);
        } else {
            hasNoExcludeList = true;
        }
        return excludeList;
    }

    private volatile static SoftReference<byte[]> rawOffsetIndices = null;

    static byte[] getRawOffsetIndices() {
        byte[] indices = null;

        SoftReference<byte[]> cache = rawOffsetIndices;
        if (cache != null) {
            indices = cache.get();
            if (indices != null) {
                return indices;
            }
        }

        byte[] buf = getZoneInfoOldMappings();
        int index = JAVAZM_LABEL_LENGTH + 1;
        int filesize = buf.length;

        try {
        loop:
            while (index < filesize) {
                byte tag = buf[index++];
                int     len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                switch (tag) {
                case TAG_RawOffsetIndices:
                    {
                        indices = new byte[len];
                        for (int i = 0; i < len; i++) {
                            indices[i] = buf[index++];
                        }
                    }
                    break loop;

                default:
                    index += len;
                    break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("ZoneInfoOld: corrupted " + JAVAZM_FILE_NAME);
        }

        rawOffsetIndices = new SoftReference<>(indices);
        return indices;
    }

    private volatile static SoftReference<int[]> rawOffsets = null;

    static int[] getRawOffsets() {
        int[] offsets = null;

        SoftReference<int[]> cache = rawOffsets;
        if (cache != null) {
            offsets = cache.get();
            if (offsets != null) {
                return offsets;
            }
        }

        byte[] buf = getZoneInfoOldMappings();
        int index = JAVAZM_LABEL_LENGTH + 1;
        int filesize = buf.length;

        try {
        loop:
            while (index < filesize) {
                byte tag = buf[index++];
                int     len = ((buf[index++] & 0xFF) << 8) + (buf[index++] & 0xFF);

                switch (tag) {
                case TAG_RawOffsets:
                    {
                        int n = len/4;
                        offsets = new int[n];
                        for (int i = 0; i < n; i++) {
                            int val = buf[index++] & 0xff;
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            val = (val << 8) + (buf[index++] & 0xff);
                            offsets[i] = val;
                        }
                    }
                    break loop;

                default:
                    index += len;
                    break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("ZoneInfoOld: corrupted " + JAVAZM_FILE_NAME);
        }

        rawOffsets = new SoftReference<>(offsets);
        return offsets;
    }

    private volatile static SoftReference<byte[]> zoneInfoMappings = null;

    private static byte[] getZoneInfoOldMappings() {
        byte[] data;
        SoftReference<byte[]> cache = zoneInfoMappings;
        if (cache != null) {
            data = cache.get();
            if (data != null) {
                return data;
            }
        }
        data = readZoneInfoFile(JAVAZM_FILE_NAME);
        if (data == null) {
            throw new RuntimeException("ZoneInfoOldMapping " +
                JAVAZM_FILE_NAME + " either doesn't exist or doesn't have data");
        }

        int index;
        for (index = 0; index < JAVAZM_LABEL.length; index++) {
            if (data[index] != JAVAZM_LABEL[index]) {
                System.err.println("ZoneInfoOld: wrong magic number: " + JAVAZM_FILE_NAME);
                return null;
            }
        }
        if (data[index++] > JAVAZM_VERSION) {
            System.err.println("ZoneInfoOld: incompatible version ("
                               + data[index - 1] + "): " + JAVAZM_FILE_NAME);
            return null;
        }

        zoneInfoMappings = new SoftReference<>(data);
        return data;
    }

    /**
     * Reads the specified file under &lt;java.home&gt;/lib/zi into a buffer.
     * @return the buffer, or null if any I/O error occurred.
     */
    private static byte[] readZoneInfoFile(final String fileName) {
        if (fileName.indexOf("..") >= 0) {
            return null;
        }
        byte[] buffer = null;
        File file = new File(ziDir, fileName);
        try {
            int filesize = (int)file.length();
            if (filesize > 0) {
                FileInputStream fis = new FileInputStream(file);
                buffer = new byte[filesize];
                try {
                    if (fis.read(buffer) != filesize) {
                        throw new IOException("read error on " + fileName);
                    }
                } finally {
                    fis.close();
                }
            }
        } catch (Exception ex) {
            if (!(ex instanceof FileNotFoundException) || JAVAZM_FILE_NAME.equals(fileName)) {
                System.err.println("ZoneInfoOld: " + ex.getMessage());
            }
        }
        return buffer;
    }

    private ZoneInfoFile() {
    }
}
