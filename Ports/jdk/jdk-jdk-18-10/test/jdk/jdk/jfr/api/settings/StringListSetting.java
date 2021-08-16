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

package jdk.jfr.api.settings;
import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.SettingControl;

@Label("String List")
@Description("Accepts set of strings, such as \"text\" or \"text1\", \"text2\", or nothing to reject all strings")
@MetadataDefinition
public final class StringListSetting extends SettingControl {

    private Set<String> acceptedStrings = new HashSet<>();

    @Override
    public void setValue(String s) {
        acceptedStrings = parseSetting(s);
    }

    private Set<String> parseSetting(String s) {
        Set<String> stringSet = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int index = 0; index < s.length(); index++) {
            char c = s.charAt(index);
            if (c != '"') {
                if (inString) {
                    // escape double quotes
                    if (c == '\\' && index + 1 < s.length()) {
                        if (s.charAt(index + 1) == '"') {
                            index++;
                            c = '"';
                        }
                    }
                    sb.append(c);
                }
            } else {
                if (inString) {
                    stringSet.add(sb.toString());
                    sb.setLength(0);
                }
                inString = !inString;
            }
        }
        return stringSet;
    }

    @Override
    public String getValue() {
        StringJoiner sj = new StringJoiner(", ", "\"", "\"");
        for (String s : acceptedStrings) {
            sj.add(s);
        }
        return sj.toString();
    }

    @Override
    public String combine(Set<String> values) {
        Set<String> nameSet = new HashSet<>();
        for (String s : values) {
            nameSet.addAll(parseSetting(s));
        }
        if (nameSet.isEmpty()) {
            return "";
        }
        StringJoiner sj = new StringJoiner(", ");
        for (String s : nameSet) {
            s = s.replace("\"", "\\\""); // escape quotes
            sj.add("\"" + s + "\"");
        }
        return sj.toString();
    }

    public boolean accept(String string) {
        return acceptedStrings.contains(string);
    }
}
