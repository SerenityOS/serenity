/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdeprscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.ElementKind;

/**
 * A database of deprecations (APIs declared to be deprecated),
 * loaded from a JDK or from a class library.
 */
public class DeprDB {
    /**
     * Deprecated types.
     * A map from deprecated type names to DeprData values.
     * Types include classes, interfaces, enums, and annotation types.
     */
    final Map<String, DeprData> types = new HashMap<>();

    /**
     * Deprecated methods. Key is type name, value is map from method
     * signatures in the form "methodname(parms)ret" to DeprData value.
     */
    final Map<String, Map<String, DeprData>> methods = new HashMap<>();

    /**
     * Deprecated fields. Key is type name, value is map from field name
     * to forRemoval value.
     */
    final Map<String, Map<String, DeprData>> fields = new HashMap<>();

    /**
     * Set of valid ElementKind strings.
     */
    static final Set<String> validElementKinds =
        Set.of(Arrays.stream(ElementKind.values())
                     .map(ElementKind::toString)
                     .toArray(String[]::new));


    private DeprDB() { }

    public static List<DeprData> loadFromFile(String filename) throws IOException {
        List<DeprData> list = new ArrayList<>();

        exit:
        try (final BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line = br.readLine();
            if (line == null || !line.equals("#jdepr1")) {
                System.out.printf("ERROR: invalid first line %s%n", line);
                break exit;
            }
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> tokens = CSV.split(line);
                if (tokens.size() != 5) {
                    System.out.printf("ERROR: %s%n", line);
                    continue;
                }
                // kind,typeName,descOrName,since,forRemoval
                String kindStr = tokens.get(0);
                String type = tokens.get(1);
                String detail = tokens.get(2);
                String since = tokens.get(3);
                boolean forRemoval = Boolean.parseBoolean(tokens.get(4));
                ElementKind kind;

                if (validElementKinds.contains(kindStr)) {
                    kind = ElementKind.valueOf(kindStr);
                } else {
                    System.out.printf("ERROR: invalid element kind %s%n", kindStr);
                    continue;
                }

                DeprData data = new DeprData(kind, /*TypeElement*/null, type, detail, since, forRemoval);
                list.add(data);
            }
        }
        return list;
    }

    public static DeprDB loadFromList(List<DeprData> deprList) {
        DeprDB db = new DeprDB();

        for (DeprData dd : deprList) {
            switch (dd.kind) {
                case CLASS:
                case INTERFACE:
                case ENUM:
                case ANNOTATION_TYPE:
                    db.types.put(dd.typeName, dd);
                    break;
                case METHOD:
                case CONSTRUCTOR:
                    db.methods.computeIfAbsent(dd.typeName, k -> new HashMap<>())
                              .put(dd.nameSig, dd);
                    break;
                case ENUM_CONSTANT:
                case FIELD:
                    db.fields.computeIfAbsent(dd.typeName, k -> new HashMap<>())
                             .put(dd.nameSig, dd);
                    break;
            }
        }

        return db;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb, Locale.US);
        f.format("=== Types ===%n");
        f.format("%s%n", types.toString());
        f.format("=== Methods ===%n");
        f.format("%s%n", methods.toString());
        f.format("=== Fields ===%n");
        f.format("%s%n", fields.toString());
        return sb.toString();
    }

    public DeprData getTypeDeprecated(String typeName) {
        return types.get(typeName);
    }

    public DeprData getMethodDeprecated(String typeName, String methodName, String type) {
        Map<String, DeprData> m = methods.get(typeName);
        if (m == null) {
            return null;
        }
        return m.get(methodName + type);
    }

    public DeprData getFieldDeprecated(String typeName, String fieldName) {
        Map<String, DeprData> f = fields.get(typeName);
        if (f == null) {
            return null;
        }
        return f.get(fieldName);
    }
}
