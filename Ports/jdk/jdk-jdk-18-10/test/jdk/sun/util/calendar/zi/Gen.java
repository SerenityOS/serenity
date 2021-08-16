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

import  java.io.IOException;
import  java.io.File;
import  java.io.FileOutputStream;
import  java.io.DataOutputStream;
import  java.io.RandomAccessFile;
import  java.util.List;
import  java.util.Map;
import  java.util.Set;

/**
 * <code>Gen</code> is one of back-end classes of javazic, and generates
 * ZoneInfoMappings and zone-specific file for each zone.
 */
class Gen extends BackEnd {

    /**
     * Generates datafile in binary TLV format for each time zone.
     * Regarding contents of output files, see {@link ZoneInfoFile}.
     *
     * @param Timezone
     * @return 0 if no errors, or 1 if error occurred.
     */
    int processZoneinfo(Timezone tz) {
        try {
            int size;
            String outputDir = Main.getOutputDir();
            String zonefile = ZoneInfoFile.getFileName(tz.getName());

            /* If outputDir doesn't end with file-separator, adds it. */
            if (!outputDir.endsWith(File.separator)) {
                outputDir += File.separatorChar;
            }

            /* If zonefile includes file-separator, it's treated as part of
             * pathname. And make directory if necessary.
             */
            int index = zonefile.lastIndexOf(File.separatorChar);
            if (index != -1) {
                outputDir += zonefile.substring(0, index+1);
            }
            File outD = new File(outputDir);
            outD.mkdirs();

            FileOutputStream fos =
                new FileOutputStream(outputDir + zonefile.substring(index+1));
            DataOutputStream dos = new DataOutputStream(fos);

            /* Output Label */
            dos.write(ZoneInfoFile.JAVAZI_LABEL, 0,
                      ZoneInfoFile.JAVAZI_LABEL.length);

            /* Output Version of ZoneInfoFile */
            dos.writeByte(ZoneInfoFile.JAVAZI_VERSION);

            List<Long> transitions = tz.getTransitions();
            if (transitions != null) {
                List<Integer> dstOffsets = tz.getDstOffsets();
                List<Integer> offsets = tz.getOffsets();

                if ((dstOffsets == null && offsets != null) ||
                    (dstOffsets != null && offsets == null)) {
                    Main.panic("Data not exist. (dstOffsets or offsets)");
                    return 1;
                }

                /* Output Transition records */
                dos.writeByte(ZoneInfoFile.TAG_Transition);
                size = transitions.size();
                dos.writeShort((size * 8) & 0xFFFF);
                int dstoffset;
                for (int i = 0; i < size; i++) {
                    /* if DST offset is 0, this means DST isn't used.
                     * (NOT: offset's index is 0.)
                     */
                    if ((dstoffset = dstOffsets.get(i).intValue()) == -1) {
                        dstoffset = 0;
                    }

                    dos.writeLong((transitions.get(i).longValue() << 12)
                                  | (dstoffset << 4)
                                  | offsets.get(i).intValue());

                }

                /* Output data for GMTOffset */
                List<Integer> gmtoffset = tz.getGmtOffsets();
                dos.writeByte(ZoneInfoFile.TAG_Offset);
                size = gmtoffset.size();
                dos.writeShort((size * 4) & 0xFFFF);
                for (int i = 0; i < size; i++) {
                    dos.writeInt(gmtoffset.get(i));
                }
            }

            /* Output data for SimpleTimeZone */
            List<RuleRec> stz = tz.getLastRules();
            if (stz != null) {
                RuleRec[] rr = new RuleRec[2];
                boolean wall = true;

                rr[0] = stz.get(0);
                rr[1] = stz.get(1);

                dos.writeByte(ZoneInfoFile.TAG_SimpleTimeZone);
                wall = rr[0].getTime().isWall() && rr[1].getTime().isWall();
                if (wall) {
                    dos.writeShort(32);
                } else {
                    dos.writeShort(40);
                }

                for (int i = 0; i < 2; i++) {
                    dos.writeInt(rr[i].getMonthNum() - 1); // 0-based month number
                    dos.writeInt(rr[i].getDay().getDayForSimpleTimeZone());
                    dos.writeInt(rr[i].getDay().getDayOfWeekForSimpleTimeZoneInt());
                    dos.writeInt((int)rr[i].getTime().getTime());
                    if (!wall) {
                        dos.writeInt((rr[i].getTime().getType() & 0xFF) - 1);
                    }
                }
            }

            /* Output RawOffset */
            dos.writeByte(ZoneInfoFile.TAG_RawOffset);
            dos.writeShort(4);
            dos.writeInt(tz.getRawOffset());

            /* Output willGMTOffsetChange flag */
            if (tz.willGMTOffsetChange()) {
                dos.writeByte(ZoneInfoFile.TAG_GMTOffsetWillChange);
                dos.writeShort(1);
                dos.writeByte(1);
            }

            /* Output LastDSTSaving */
            dos.writeByte(ZoneInfoFile.TAG_LastDSTSaving);
            dos.writeShort(2);
            dos.writeShort(tz.getLastDSTSaving()/1000);

            /* Output checksum */
            dos.writeByte(ZoneInfoFile.TAG_CRC32);
            dos.writeShort(4);
            dos.writeInt(tz.getCRC32());

            fos.close();
            dos.close();
        } catch(IOException e) {
            Main.panic("IO error: "+e.getMessage());
            return 1;
        }

        return 0;
    }

    /**
     * Generates ZoneInfoMappings in binary TLV format for each zone.
     * Regarding contents of output files, see {@link ZoneInfoFile}.
     *
     * @param Mappings
     * @return 0 if no errors, or 1 if error occurred.
     */
    int generateSrc(Mappings map) {
        try {
            int index;
            int block_size;
            int roi_size;
            long fp;
            String outputDir = Main.getOutputDir();

            /* If outputDir doesn't end with file-separator, adds it. */
            if (!outputDir.endsWith(File.separator)) {
                outputDir += File.separatorChar;
            }

            File outD = new File(outputDir);
            outD.mkdirs();

            /* Open ZoneInfoMapping file to write. */
            RandomAccessFile raf =
                new RandomAccessFile(outputDir + ZoneInfoFile.JAVAZM_FILE_NAME, "rw");

            /* Whether rawOffsetIndex list exists or not. */
            List<Integer> roi = map.getRawOffsetsIndex();
            if (roi == null) {
                Main.panic("Data not exist. (rawOffsetsIndex)");
                return 1;
            }
            roi_size = roi.size();

            /* Whether rawOffsetIndexTable list exists or not. */
            List<Set<String>> roit = map.getRawOffsetsIndexTable();
            if (roit == null || roit.size() != roi_size) {
                Main.panic("Data not exist. (rawOffsetsIndexTable) Otherwise, Invalid size");
                return 1;
            }

            /* Output Label */
            raf.write(ZoneInfoFile.JAVAZM_LABEL, 0,
                      ZoneInfoFile.JAVAZM_LABEL.length);

            /* Output Version */
            raf.writeByte(ZoneInfoFile.JAVAZM_VERSION);

            index = ZoneInfoFile.JAVAZM_LABEL.length + 2;

            /* Output Version of Olson's tzdata */
            byte[] b = Main.getVersionName().getBytes("UTF-8");
            raf.writeByte(ZoneInfoFile.TAG_TZDataVersion);
            raf.writeShort((b.length+1) & 0xFFFF);
            raf.write(b);
            raf.writeByte(0x00);
            index += b.length + 4;

            /* Output ID list. */
            raf.writeByte(ZoneInfoFile.TAG_ZoneIDs);
            block_size = 2;
            raf.writeShort(block_size & 0xFFFF);
            short nID = 0;
            raf.writeShort(nID & 0xFFFF);
            for (int i = 0; i < roi_size; i++) {
                for (String key : roit.get(i)) {
                    byte size = (byte)key.getBytes("UTF-8").length;
                    raf.writeByte(size & 0xFF);
                    raf.write(key.getBytes("UTF-8"), 0, size);
                    block_size += 1 + size;
                    nID++;
                }
            }
            fp = raf.getFilePointer();
            raf.seek(index);
            raf.writeShort((block_size) & 0xFFFF);
            raf.writeShort(nID & 0xFFFF);
            raf.seek(fp);

            /* Output sorted rawOffset list. */
            raf.writeByte(ZoneInfoFile.TAG_RawOffsets);
            index += 3 + block_size;
            block_size = roi_size * 4;
            raf.writeShort(block_size & 0xFFFF);
            for (int i = 0; i < roi_size; i++) {
                raf.writeInt(Integer.parseInt(roi.get(i).toString()));
            }

            /* Output sorted rawOffsetIndex list. */
            raf.writeByte(ZoneInfoFile.TAG_RawOffsetIndices);
            index += 3 + block_size;
            block_size = 0;
            raf.writeShort(block_size & 0xFFFF);
            int num;
            for (int i = 0; i < roi_size; i++) {
                num = roit.get(i).size();
                block_size += num;
                for (int j = 0; j < num; j++) {
                    raf.writeByte(i);
                }
            }
            fp = raf.getFilePointer();
            raf.seek(index);
            raf.writeShort((block_size) & 0xFFFF);
            raf.seek(fp);

            /* Whether alias list exists or not. */
            Map<String,String> a = map.getAliases();
            if (a == null) {
                Main.panic("Data not exist. (aliases)");
                return 0;
            }

            /* Output ID list. */
            raf.writeByte(ZoneInfoFile.TAG_ZoneAliases);
            index += 3 + block_size;
            block_size = 2;
            raf.writeShort(block_size & 0xFFFF);
            raf.writeShort(a.size() & 0xFFFF);
            for (String key : a.keySet()) {
                String alias = a.get(key);
                byte key_size = (byte)key.length();
                byte alias_size = (byte)alias.length();
                raf.writeByte(key_size & 0xFF);
                raf.write(key.getBytes("UTF-8"), 0, key_size);
                raf.writeByte(alias_size & 0xFF);
                raf.write(alias.getBytes("UTF-8"), 0, alias_size);
                block_size += 2 + key_size + alias_size;
            }
            fp = raf.getFilePointer();
            raf.seek(index);
            raf.writeShort((block_size) & 0xFFFF);
            raf.seek(fp);

            /* Output the exclude list if it exists. */
            List<String> excludedZones = map.getExcludeList();
            if (excludedZones != null) {
                raf.writeByte(ZoneInfoFile.TAG_ExcludedZones);
                index += 3 + block_size;
                block_size = 2;
                raf.writeShort(block_size & 0xFFFF);  // place holder
                raf.writeShort(excludedZones.size()); // the number of excluded zones
                for (String name : excludedZones) {
                    byte size = (byte) name.length();
                    raf.writeByte(size);                 // byte length
                    raf.write(name.getBytes("UTF-8"), 0, size); // zone name
                    block_size += 1 + size;
                }
                fp = raf.getFilePointer();
                raf.seek(index);
                raf.writeShort(block_size & 0xFFFF);
                raf.seek(fp);
            }

            /* Close ZoneInfoMapping file. */
            raf.close();
        } catch(IOException e) {
            Main.panic("IO error: "+e.getMessage());
            return 1;
        }

        return 0;
    }
}
