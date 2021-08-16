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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Class contains representation of GC PLAB log.
 */
public class PlabReport {

    private final Map<Long, PlabGCStatistics> report = new HashMap<>();

    public PlabReport() {
    }

    /**
     * Checks if underlying Map contains requested GC ID.
     */
    public boolean containsKey(Long gcId) {
        return report.containsKey(gcId);
    }

    /**
     * Puts GC ID and PlabGCStatistics to underlying Map.
     */
    public void put(Long gcId, PlabGCStatistics plabStat) {
        report.put(gcId, plabStat);
    }

    /**
     * Returns PlabGCStatistics for specified GC ID.
     */
    public PlabGCStatistics get(Long gcId) {
        return report.get(gcId);
    }

    /**
     * Returns Stream of Map.Entry of underlying Map.
     */
    public Stream<Map.Entry<Long, PlabGCStatistics>> entryStream() {
        return report.entrySet().stream();
    }
}
