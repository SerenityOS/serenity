/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.internal.jfc.model;

import java.util.StringJoiner;

public final class Utilities {
    private static final String[] UNITS = new String[] {
        "ns", "us", "ns", "ms", "s", "m", "h", "d" // order matters
    };

    static XmlElement instantiate(Class<? extends XmlElement> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InternalError("Unable to instantiate " + type, e);
        }
    }

    static String elementName(Class<? extends XmlElement> type) {
        String name = type.getSimpleName();
        if (name.startsWith("Xml") && name.length() > 3) {
            return name.substring(3).toLowerCase();
        }
        throw new InternalError("Unexpected class " + type);
    }

    static String escapeAll(String text) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            addCharacter(s, text.charAt(i));
        }
        return s.toString();
    }

    private static void addCharacter(StringBuilder s, char c) {
        if (c == 34) {
            s.append("&quot;");
            return;
        }
        if (c == 38) {
            s.append("&amp;");
            return;
        }
        if (c == 39) {
            s.append("&apos;");
            return;
        }
        if (c == 60) {
            s.append("&lt;");
            return;
        }
        if (c == 62) {
            s.append("&gt;");
            return;
        }
        if (c > 0x7F) {
            s.append("&#");
            s.append((int) c);
            s.append(';');
            return;
        }
        s.append(c);
    }

    static void checkValid(String value, Object... valid) {
        StringJoiner sj = new StringJoiner(", ");
        for (Object v : valid) {
            if (v.equals(value)) {
                return;
            }
            sj.add("'" + v + "'");
        }
        String msg = "Incorrect value '" + value + "'. Valid values are " + sj.toString() + ".";
        int index = msg.lastIndexOf(",");
        if (index != -1) {
            msg = msg.substring(0, index) + " and" + msg.substring(index + 1);
        }
        throw new IllegalArgumentException(msg);
    }

    static String parseTimespan(String s) {
        StringBuilder sb = new StringBuilder();
        try {
            for (String unit : UNITS) {
                if (s.endsWith(unit)) {
                    return parseForUnit(s, unit);
                }
            }
            Long.parseLong(s);
            sb.append("Timespan '" + s + "' is missing unit.");
        } catch (NumberFormatException nfe) {
            sb.append("'" + s + "' is not a valid timespan." + System.lineSeparator());
            sb.append("Should be numeric value followed by a unit, i.e. 20 ms.");
        }
        sb.append(" Valid units are ns, us, ms, s, m, h and d.");
        throw new IllegalArgumentException(sb.toString());
    }

    private static String parseForUnit(String s, String unit) {
        String number = s.trim().substring(0, s.length() - unit.length());
        return Long.parseLong(number.trim()) + " " + unit;
    }
}
