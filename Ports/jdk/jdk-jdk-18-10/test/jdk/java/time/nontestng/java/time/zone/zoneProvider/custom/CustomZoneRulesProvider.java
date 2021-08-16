/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package custom;

import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesProvider;
import java.util.Set;
import java.util.NavigableMap;
import java.util.TreeMap;

public class CustomZoneRulesProvider extends ZoneRulesProvider {
    @Override
    protected Set<String> provideZoneIds() {
        return Set.of("Custom/Timezone");
    }

    @Override
    protected ZoneRules provideRules(String zoneId, boolean forCaching) {
        return ZoneId.of("UTC").getRules();
    }

    @Override
    protected NavigableMap<String, ZoneRules> provideVersions(String zoneId) {
        var map = new TreeMap<String, ZoneRules>();
        map.put("bogusVersion", getRules(zoneId, false));
        return map;
    }
}
