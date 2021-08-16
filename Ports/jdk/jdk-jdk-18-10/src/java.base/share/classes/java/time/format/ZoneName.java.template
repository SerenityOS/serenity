/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package java.time.format;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A helper class to map a zone name to metazone and back to the
 * appropriate zone id for the particular locale.
 * <p>
 * The zid<->metazone mappings are based on CLDR metaZones.xml.
 * The alias mappings are based on Link entries in tzdb data files and
 * CLDR's supplementalMetadata.xml.
 */
class ZoneName {

    public static String toZid(String zid, Locale locale) {
        String mzone = zidToMzone.get(zid);
        if (mzone == null && aliases.containsKey(zid)) {
            zid = aliases.get(zid);
            mzone = zidToMzone.get(zid);
        }
        if (mzone != null) {
            Map<String, String> map = mzoneToZidL.get(mzone);
            if (map != null && map.containsKey(locale.getCountry())) {
                zid = map.get(locale.getCountry());
            } else {
                zid = mzoneToZid.get(mzone);
            }
        }
        return toZid(zid);
    }

    public static String toZid(String zid) {
        if (aliases.containsKey(zid)) {
            return aliases.get(zid);
        }
        return zid;
    }

    private static final String[] zidMap = new String[] {
        // From metaZones.xml
%%%%ZIDMAP%%%%

        // From tzdb
        "Africa/Khartoum", "Africa_Central", "Africa/Maputo", // tzdata2017c
        "Africa/Windhoek", "Africa_Central", "Africa/Maputo", // tzdata2017c
        "Africa/Sao_Tome", "Africa_Western", "Africa/Lagos",  // tzdata2018c
    };
    private static final String[] mzoneMap = new String[] {
        // From metaZones.xml
%%%%MZONEMAP%%%%

        // From tzdb
        "Africa_Western", "ST", "Africa/Sao_Tome", // tzdata2018c
    };
    private static final String[] aliasMap = new String[] {
        // From supplementalMetadata.xml
%%%%DEPRECATED%%%%

        // From tzdb
%%%%TZDATALINK%%%%
    };

    private static final Map<String, String> zidToMzone = new HashMap<>();
    private static final Map<String, String> mzoneToZid = new HashMap<>();
    private static final Map<String, Map<String, String>> mzoneToZidL = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();

    static {
        for (int i = 0; i < zidMap.length; i += 3) {
            zidToMzone.put(zidMap[i], zidMap[i + 1]);
            mzoneToZid.put(zidMap[i + 1], zidMap[i + 2]);
        }

        for (int i = 0; i < mzoneMap.length; i += 3) {
            String mzone = mzoneMap[i];
            Map<String, String> map = mzoneToZidL.get(mzone);
            if (map == null) {
                map = new HashMap<>();
                mzoneToZidL.put(mzone, map);
            }
            map.put(mzoneMap[i + 1], mzoneMap[i + 2]);
        }

        for (int i = 0; i < aliasMap.length; i += 2) {
            aliases.put(aliasMap[i], aliasMap[i + 1]);
        }
    }
}
