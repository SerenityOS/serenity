/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.plab.lib;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlabInfo {

    private final Map<String, Long> plabInfo;

    public PlabInfo() {
        plabInfo = new HashMap<>();
    }

    private PlabInfo(Map<String, Long> map) {
        plabInfo = new HashMap<>(map);
    }

    /**
     * Add key and value to underlying Map.
     * @param key   PLAB info field name
     * @param value PLAB info value for field
     */
    public void put(String key, long value) {
        plabInfo.put(key, value);
    }

    /**
     * Get stream of Map.Entry representing underlying Map with PLAB information.
     */
    public Stream<Map.Entry<String, Long>> entryStream() {
        return plabInfo.entrySet().stream();
    }

    /**
     * Returns the PlabInfo narrowed for the given fields only
     * @param fields
     * @return PlabInfo
     */
    public PlabInfo filter(List<String> fields) {
        return new PlabInfo(entryStream()
                .filter(field -> fields.contains(field.getKey()))
                .collect(Collectors.toMap(
                        item -> item.getKey(),
                        item -> item.getValue())
                )
        );
    }

    /**
     * Checks if statistic contains expected fields.
     * @param fields fields which should be in statistic
     * @return true if all fields are in statistic, false otherwise
     */
    public boolean checkFields(List<String> fields) {
        for (String key : fields) {
            if (!plabInfo.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return a collection of the values.
     * @return collection of values
     */
    public Collection<Long> values() {
        return plabInfo.values();
    }

    /**
     * Get value for specified field.
     * @param field
     * @return long value which is contained in specified field
     */
    public long get(String field) {
        return plabInfo.get(field);
    }
}
