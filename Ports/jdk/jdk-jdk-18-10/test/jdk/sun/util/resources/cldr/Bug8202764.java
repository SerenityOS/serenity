/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202764
 * @modules jdk.localedata
 * @summary Checks time zone names are consistent with aliased ids,
 *      between DateFormatSymbols.getZoneStrings() and getDisplayName()
 *      of TimeZone/ZoneId classes
 * @run testng/othervm Bug8202764
 */

import static org.testng.Assert.assertEquals;

import java.time.ZoneId;
import java.time.format.TextStyle;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.testng.annotations.Test;

public class Bug8202764 {

    @Test
    public void testAliasedTZs() {
        Set<String> zoneIds = ZoneId.getAvailableZoneIds();
        Arrays.stream(DateFormatSymbols.getInstance(Locale.US).getZoneStrings())
            .forEach(zone -> {
                System.out.println(zone[0]);
                TimeZone tz = TimeZone.getTimeZone(zone[0]);
                assertEquals(zone[1], tz.getDisplayName(false, TimeZone.LONG, Locale.US));
                assertEquals(zone[2], tz.getDisplayName(false, TimeZone.SHORT, Locale.US));
                assertEquals(zone[3], tz.getDisplayName(true, TimeZone.LONG, Locale.US));
                assertEquals(zone[4], tz.getDisplayName(true, TimeZone.SHORT, Locale.US));
                if (zoneIds.contains(zone[0])) {
                    // Some of the ids, e.g. three-letter ids are not supported in ZoneId
                    ZoneId zi = tz.toZoneId();
                    assertEquals(zone[5], zi.getDisplayName(TextStyle.FULL, Locale.US));
                    assertEquals(zone[6], zi.getDisplayName(TextStyle.SHORT, Locale.US));
                }
            });
    }
}
