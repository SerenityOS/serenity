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
package jdk.jpackage.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class CfgFile {
    public String getValue(String section, String key) {
        Objects.requireNonNull(section);
        Objects.requireNonNull(key);

        Map<String, String> entries = data.get(section);
        TKit.assertTrue(entries != null, String.format(
                "Check section [%s] is found in [%s] cfg file", section, id));

        String value = entries.get(key);
        TKit.assertNotNull(value, String.format(
                "Check key [%s] is found in [%s] section of [%s] cfg file", key,
                section, id));

        return value;
    }

    private CfgFile(Map<String, Map<String, String>> data, String id) {
        this.data = data;
        this.id = id;
    }

    public static CfgFile readFromFile(Path path) throws IOException {
        TKit.trace(String.format("Read [%s] jpackage cfg file", path));

        final Pattern sectionBeginRegex = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*");
        final Pattern keyRegex = Pattern.compile( "\\s*([^=]*)=(.*)" );

        Map<String, Map<String, String>> result = new HashMap<>();

        String currentSectionName = null;
        Map<String, String> currentSection = new HashMap<>();
        for (String line : Files.readAllLines(path)) {
            Matcher matcher = sectionBeginRegex.matcher(line);
            if (matcher.find()) {
                if (currentSectionName != null) {
                    result.put(currentSectionName, Collections.unmodifiableMap(
                            new HashMap<>(currentSection)));
                }
                currentSectionName = matcher.group(1);
                currentSection.clear();
                continue;
            }

            matcher = keyRegex.matcher(line);
            if (matcher.find()) {
                currentSection.put(matcher.group(1), matcher.group(2));
                continue;
            }
        }

        if (!currentSection.isEmpty()) {
            result.put("", Collections.unmodifiableMap(currentSection));
        }

        return new CfgFile(Collections.unmodifiableMap(result), path.toString());
    }

    private final Map<String, Map<String, String>> data;
    private final String id;
}
