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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Month enum handles month related manipulation.
 *
 * @since 1.4
 */
enum Month {
    JANUARY("Jan"),
    FEBRUARY("Feb"),
    MARCH("Mar"),
    APRIL("Apr"),
    MAY("May"),
    JUNE("Jun"),
    JULY("Jul"),
    AUGUST("Aug"),
    SEPTEMBER("Sep"),
    OCTOBER("Oct"),
    NOVEMBER("Nov"),
    DECEMBER("Dec");

    private final String abbr;

    private static final Map<String,Month> abbreviations
                                = new HashMap<String,Month>(12);

    static {
        for (Month m : Month.values()) {
            abbreviations.put(m.abbr, m);
        }
    }

    private Month(String abbr) {
        this.abbr = abbr;
    }

    int value() {
        return ordinal() + 1;
    }

    /**
     * Parses the specified string as a month abbreviation.
     * @param name the month abbreviation
     * @return the Month value
     */
    static Month parse(String name) {
        Month m = abbreviations.get(name);
        if (m != null) {
            return m;
        }
        return null;
    }

    /**
     * @param month the nunmth number (1-based)
     * @return the month name in uppercase of the specified month
     */
    static String toString(int month) {
        if (month >= JANUARY.value() && month <= DECEMBER.value()) {
            return "Calendar." + Month.values()[month - 1];
        }
        throw new IllegalArgumentException("wrong month number: " + month);
    }
}
