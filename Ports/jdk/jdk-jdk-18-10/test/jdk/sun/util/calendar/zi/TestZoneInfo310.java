/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8007572 8008161 8157792 8212970 8224560
 * @summary Test whether the TimeZone generated from JSR310 tzdb is the same
 * as the one from the tz data from javazic
 * @modules java.base/sun.util.calendar:+open
 * @build BackEnd Checksum DayOfWeek Gen GenDoc Main Mappings Month
 *        Rule RuleDay RuleRec Simple TestZoneInfo310 Time Timezone
 *        TzIDOldMapping Zone ZoneInfoFile ZoneInfoOld ZoneRec Zoneinfo
 * @run main TestZoneInfo310
 */

import java.io.File;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.time.zone.*;
import java.time.ZoneId;

public class TestZoneInfo310 {

    public static void main(String[] args) throws Throwable {

        String TESTDIR = System.getProperty("test.dir", ".");
        Path tzdir = Paths.get(System.getProperty("test.root"),
            "..", "..", "make", "data", "tzdata");
        String tzfiles = "africa antarctica asia australasia europe northamerica southamerica backward etcetera gmt";
        Path jdk_tzdir = Paths.get(System.getProperty("test.src"), "tzdata_jdk");
        String jdk_tzfiles = "jdk11_backward";
        String zidir = TESTDIR + File.separator + "zi";
        File fZidir = new File(zidir);
        if (!fZidir.exists()) {
            fZidir.mkdirs();
        }
        Matcher m = Pattern.compile("tzdata(?<ver>[0-9]{4}[A-z])")
                           .matcher(new String(Files.readAllBytes(tzdir.resolve("VERSION")), "ascii"));
        String ver = m.find() ? m.group("ver") : "NULL";

        ArrayList<String> alist = new ArrayList<>();
        alist.add("-V");
        alist.add(ver);
        alist.add("-d");
        alist.add(zidir);
        for (String f : tzfiles.split(" ")) {
            alist.add(tzdir.resolve(f).toString());
        }
        for (String f : jdk_tzfiles.split(" ")) {
            alist.add(jdk_tzdir.resolve(f).toString());
        }
        System.out.println("Compiling tz files!");
        Main.main(alist.toArray(new String[alist.size()]));

        //////////////////////////////////
        System.out.println("testing!");
        ZoneInfoFile.ziDir = zidir;
        long t0, t1;

        t0 = System.nanoTime();
        ZoneInfoOld.getTimeZone("America/Los_Angeles");
        t1 = System.nanoTime();
        System.out.printf("OLD.getZoneInfoOld()[1]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        ZoneInfoOld.getTimeZone("America/New_York");
        t1 = System.nanoTime();
        System.out.printf("OLD.getZoneInfoOld()[2]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        ZoneInfoOld.getTimeZone("America/Denver");
        t1 = System.nanoTime();
        System.out.printf("OLD.getZoneInfoOld()[3]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        String[] zids_old = ZoneInfoOld.getAvailableIDs();
        t1 = System.nanoTime();
        System.out.printf("OLD.getAvailableIDs()=%d, total=%d%n",
                          (t1 - t0) / 1000, zids_old.length);
        Arrays.sort(zids_old);

        t0 = System.nanoTime();
        String[] alias_old = ZoneInfoOld.getAliasTable()
                                 .keySet().toArray(new String[0]);
        t1 = System.nanoTime();
        System.out.printf("OLD.getAliasTable()=%d, total=%d%n",
                          (t1 - t0) / 1000, alias_old.length);
        Arrays.sort(alias_old);

        t0 = System.currentTimeMillis();
        for (String zid : zids_old) {
            ZoneInfoOld.getTimeZone(zid);
        }
        t1 = System.currentTimeMillis();
        System.out.printf("OLD.TotalTZ()=%d (ms)%n", t1 - t0);

/*
        t0 = System.nanoTime();
        ZoneId.of("America/Los_Angeles").getRules();
        t1 = System.nanoTime();
        System.out.printf("NEW.ZoneId.of()[1]=%d%n", (t1 - t0) / 1000);
*/
        t0 = System.nanoTime();
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        t1 = System.nanoTime();
        System.out.printf("NEW.getTimeZone()[1]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        tz = TimeZone.getTimeZone("America/New_York");
        t1 = System.nanoTime();
        System.out.printf("NEW.getTimeZone()[2]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        tz = TimeZone.getTimeZone("America/Denver");
        t1 = System.nanoTime();
        System.out.printf("NEW.getTimeZone()[3]=%d%n", (t1 - t0) / 1000);

        t0 = System.nanoTime();
        String[] zids_new = TimeZone.getAvailableIDs();
        t1 = System.nanoTime();
        System.out.printf("NEW.getAvailableIDs()=%d, total=%d%n",
                          (t1 - t0) / 1000, zids_new.length);
        Arrays.sort(zids_new);

        t0 = System.nanoTime();
        String[] alias_new = sun.util.calendar.ZoneInfo.getAliasTable()
                                 .keySet().toArray(new String[0]);
        t1 = System.nanoTime();
        System.out.printf("NEW.getAliasTable()=%d, total=%d%n",
                          (t1 - t0) / 1000, alias_new.length);
        Arrays.sort(alias_new);

        t0 = System.currentTimeMillis();
        for (String zid : zids_new) {
            TimeZone.getTimeZone(zid);
        }
        t1 = System.currentTimeMillis();
        System.out.printf("NEW.TotalTZ()=%d (ms)%n", t1 - t0);

        if (!Arrays.equals(zids_old, zids_new)) {
            throw new RuntimeException("  FAILED:  availableIds don't match");
        }

        if (!Arrays.equals(alias_old, alias_new)) {
            throw new RuntimeException("  FAILED:  aliases don't match");
        }

        for (String zid : zids_new) {
            ZoneInfoOld zi = toZoneInfoOld(TimeZone.getTimeZone(zid));
            ZoneInfoOld ziOLD = (ZoneInfoOld)ZoneInfoOld.getTimeZone(zid);
            /*
             * Temporary ignoring the failing TimeZones which are having zone
             * rules defined till year 2037 and/or above and have negative DST
             * save time in IANA tzdata. This bug is tracked via JDK-8223388.
             *
             * These are the zones/rules that employ negative DST in vanguard
             * format (as of 2019a):
             *
             *  - Rule "Eire"
             *  - Rule "Morocco"
             *  - Rule "Namibia"
             *  - Zone "Europe/Prague"
             *
             * Tehran/Iran rule has rules beyond 2037, in which javazic assumes
             * to be the last year. Thus javazic's rule is based on year 2037
             * (Mar 20th/Sep 20th are the cutover dates), while the real rule
             * has year 2087 where Mar 21st/Sep 21st are the cutover dates.
             */
            if (zid.equals("Africa/Casablanca") || // uses "Morocco" rule
                zid.equals("Africa/El_Aaiun") || // uses "Morocco" rule
                zid.equals("Africa/Windhoek") || // uses "Namibia" rule
                zid.equals("Eire") ||
                zid.equals("Europe/Bratislava") || // link to "Europe/Prague"
                zid.equals("Europe/Dublin") || // uses "Eire" rule
                zid.equals("Europe/Prague") ||
                zid.equals("Asia/Tehran") || // last rule mismatch
                zid.equals("Iran")) { // last rule mismatch
                    continue;
            }
            if (! zi.equalsTo(ziOLD)) {
                System.out.println(zi.diffsTo(ziOLD));
                throw new RuntimeException("  FAILED:  " + zid);
            }
        }
        delete(fZidir);

        // test tzdb version
        if (!ver.equals(sun.util.calendar.ZoneInfoFile.getVersion())) {
            System.out.printf("  FAILED:  ver=%s, expected=%s%n",
                              sun.util.calendar.ZoneInfoFile.getVersion(), ver);
            throw new RuntimeException("Version test failed");
        }

        // test getAvailableIDs(raw);
        zids_new = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        Arrays.sort(zids_new);
        zids_old = ZoneInfoOld.getAvailableIDs(-8 * 60 * 60 * 1000);
        Arrays.sort(zids_old);
        if (!Arrays.equals(zids_new, zids_old)) {
            System.out.println("------------------------");
            System.out.println("NEW.getAvailableIDs(-8:00)");
            for (String zid : zids_new) {
                System.out.println(zid);
            }
            System.out.println("------------------------");
            System.out.println("OLD.getAvailableIDs(-8:00)");
            for (String zid : zids_old) {
                System.out.println(zid);
            }
            throw new RuntimeException("  FAILED:  availableIds(offset) don't match");
        }
    }

    private static void delete(File f) {
        if (f.isDirectory()) {
            for (File f0 : f.listFiles()) {
               delete(f0);
            }
        }
        f.delete();
     }

    // to access sun.util.calendar.ZoneInfo's private fields
    static Class<?> ziClz;
    static Field rawOffset;
    static Field checksum;
    static Field dstSavings;
    static Field transitions;
    static Field offsets;
    static Field simpleTimeZoneParams;
    static Field willGMTOffsetChange;
    static {
        try {
            ziClz = Class.forName("sun.util.calendar.ZoneInfo");
            rawOffset = ziClz.getDeclaredField("rawOffset");
            checksum = ziClz.getDeclaredField("checksum");
            dstSavings = ziClz.getDeclaredField("dstSavings");
            transitions = ziClz.getDeclaredField("transitions");
            offsets = ziClz.getDeclaredField("offsets");
            simpleTimeZoneParams = ziClz.getDeclaredField("simpleTimeZoneParams");
            willGMTOffsetChange = ziClz.getDeclaredField("willGMTOffsetChange");
            rawOffset.setAccessible(true);
            checksum.setAccessible(true);
            dstSavings.setAccessible(true);
            transitions.setAccessible(true);
            offsets.setAccessible(true);
            simpleTimeZoneParams.setAccessible(true);
            willGMTOffsetChange.setAccessible(true);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static ZoneInfoOld toZoneInfoOld(TimeZone tz) throws Exception {
        return new ZoneInfoOld(tz.getID(),
                               rawOffset.getInt(tz),
                               dstSavings.getInt(tz),
                               checksum.getInt(tz),
                               (long[])transitions.get(tz),
                               (int[])offsets.get(tz),
                               (int[])simpleTimeZoneParams.get(tz),
                               willGMTOffsetChange.getBoolean(tz));
    }


}
