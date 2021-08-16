/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008577 8024141
 * @summary Test for cache support of sun.util.locale.provider.LocaleResources.getTimeZoneNames
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug8024141
 */

import java.time.ZoneId;
import static java.util.Locale.ENGLISH;
import static java.time.format.TextStyle.FULL;
import static java.time.format.TextStyle.SHORT;

public class Bug8024141 {
    // This test assumes that the two time zones are in GMT. If
    // they become different zones, need to pick up another zones.
    private static final String[] ZONES = {
        "Africa/Abidjan",
        "Africa/Bamako"
    };

    public static void main(String[] args) {
        ZoneId gmt = ZoneId.of("GMT");
        String gmtName = gmt.getDisplayName(FULL, ENGLISH);
        String gmtAbbr = gmt.getDisplayName(SHORT, ENGLISH);

        for (String zone : ZONES) {
            ZoneId id = ZoneId.of(zone);
            String name = id.getDisplayName(FULL, ENGLISH);
            String abbr = id.getDisplayName(SHORT, ENGLISH);

            if (!name.equals(gmtName) || !abbr.equals(gmtAbbr)) {
                throw new RuntimeException("inconsistent name/abbr for " + zone + ":\n"
                                           + "name=" + name + ", abbr=" + abbr);
            }
        }
    }
}
