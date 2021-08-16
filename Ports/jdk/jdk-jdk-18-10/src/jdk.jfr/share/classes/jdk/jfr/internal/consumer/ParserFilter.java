/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

final class ParserFilter {
    public static final ParserFilter ACCEPT_ALL = new ParserFilter(true, Map.of());

    private final Map<String, Long> thresholds;
    private final boolean acceptAll;

    public ParserFilter() {
        this(false, new HashMap<>());
    }

    private ParserFilter(boolean acceptAll, Map<String, Long> thresholds) {
        this.acceptAll = acceptAll;
        this.thresholds = thresholds;
    }

    public void setThreshold(String eventName, long nanos) {
        Long l = thresholds.get(eventName);
        if (l != null) {
            l = Math.min(l, nanos);
        } else {
            l = nanos;
        }
        thresholds.put(eventName, l);
    }

    public long getThreshold(String eventName) {
        if (acceptAll) {
            return 0L;
        }
        Long l = thresholds.get(eventName);
        if (l != null) {
            return l;
        }
        return -1;
    }

    @Override
    public String toString() {
        if (acceptAll) {
            return "ACCEPT ALL";
        }

        StringJoiner sb = new StringJoiner(", ");
        for (String key : thresholds.keySet().toArray(new String[0])) {
            Long value = thresholds.get(key);
            sb.add(key + " = " + value);
        }
        return sb.toString();
    }
}
