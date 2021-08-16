/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.settings;

import java.util.Set;

/**
 * Helper class for settings that use boolean numbers
 *
 */
final class BooleanValue  {
    private String value = "false";
    private boolean booleanValue;

    private BooleanValue(boolean b) {
        booleanValue = b;
        value = b ? "true" : "false";
    }

    public String union(Set<String> values) {
        for (String v : values) {
            if ("true".equals(v)) {
                return "true";
            }
        }
        return "false";
    }

    public void setValue(String value) {
        this.value = value;
        this.booleanValue = Boolean.valueOf(value);
    }

    public final String getValue() {
        return this.value;
    }

    public boolean getBoolean() {
        return booleanValue;
    }

    public static BooleanValue valueOf(String defaultValue) {
        if ("true".equals(defaultValue)) {
            return new BooleanValue(true);
        }
        if ("false".equals(defaultValue)) {
            return new BooleanValue(false);
        }
        throw new InternalError("Unknown default value for settings '" + defaultValue + "'");
    }
}
