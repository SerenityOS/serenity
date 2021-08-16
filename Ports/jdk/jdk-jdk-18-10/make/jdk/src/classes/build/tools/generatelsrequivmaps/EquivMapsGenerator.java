/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.generatelsrequivmaps;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This tool reads the IANA Language Subtag Registry data file downloaded from
 * http://www.iana.org/assignments/language-subtag-registry, which is specified
 * in the command line and generates a .java source file as specified in
 * command line. The generated .java source file contains equivalent language
 * maps. These equivalent language maps are used by LocaleMatcher.java
 * for the locale matching mechanism specified in RFC 4647 "Matching of Language
 * Tags".
 */
public class EquivMapsGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java EquivMapsGenerator"
                    + " language-subtag-registry.txt LocaleEquivalentMaps.java");
            System.exit(1);
        }
        readLSRfile(args[0]);
        generateEquivalentMap();
        generateSourceCode(args[1]);
    }

    private static String LSRrevisionDate;
    private static Map<String, StringBuilder> initialLanguageMap =
        new TreeMap<>();
    private static Map<String, StringBuilder> initialRegionVariantMap =
        new TreeMap<>();

    private static Map<String, String> sortedLanguageMap1 = new TreeMap<>();
    private static Map<String, String[]> sortedLanguageMap2 = new TreeMap<>();
    private static Map<String, String> sortedRegionVariantMap =
        new TreeMap<>();

    private static void readLSRfile(String filename) throws Exception {
        String type = null;
        String tag = null;
        String preferred = null;
        String prefix = null;

        for (String line : Files.readAllLines(Paths.get(filename),
                                              Charset.forName("UTF-8"))) {
            line = line.toLowerCase(Locale.ROOT);
            int index = line.indexOf(' ') + 1;
            if (line.startsWith("file-date:")) {
                LSRrevisionDate = line.substring(index);
            } else if (line.startsWith("type:")) {
                type = line.substring(index);
            } else if (line.startsWith("tag:") || line.startsWith("subtag:")) {
                tag = line.substring(index);
            } else if (line.startsWith("preferred-value:")) {
                preferred = line.substring(index);
            } else if (line.startsWith("prefix:")) {
                prefix = line.substring(index);
            } else if (line.equals("%%")) {
                processDeprecatedData(type, tag, preferred, prefix);
                type = null;
                tag = null;
                preferred = null;
                prefix = null;
            }
        }

        // Last entry
        processDeprecatedData(type, tag, preferred, prefix);
    }

    private static void processDeprecatedData(String type,
                                              String tag,
                                              String preferred,
                                              String prefix) {
        StringBuilder sb;

        if (type == null || tag == null || preferred == null) {
            return;
        }

        if (type.equals("extlang") && prefix != null) {
            tag = prefix + "-" + tag;
        }

        if (type.equals("region") || type.equals("variant")) {
            if (!initialRegionVariantMap.containsKey(preferred)) {
                sb = new StringBuilder("-");
                sb.append(preferred);
                sb.append(",-");
                sb.append(tag);
                initialRegionVariantMap.put("-"+preferred, sb);
            } else {
                throw new RuntimeException("New case, need implementation."
                    + " A region/variant subtag \"" + preferred
                    + "\" is registered for more than one subtags.");
            }
        } else { // language, extlang, legacy, and redundant
            if (!initialLanguageMap.containsKey(preferred)) {
                sb = new StringBuilder(preferred);
                sb.append(',');
                sb.append(tag);
                initialLanguageMap.put(preferred, sb);
            } else {
                sb = initialLanguageMap.get(preferred);
                sb.append(',');
                sb.append(tag);
                initialLanguageMap.put(preferred, sb);
            }
        }
    }

    private static void generateEquivalentMap() {
        String[] subtags;
        for (String preferred : initialLanguageMap.keySet()) {
            // There are cases where the same tag may appear in two entries, e.g.,
            // "yue" is defined both as extlang and redundant. Remove the dup.
            subtags = Arrays.stream(initialLanguageMap.get(preferred).toString().split(","))
                    .distinct()
                    .collect(Collectors.toList())
                    .toArray(new String[0]);

            if (subtags.length == 2) {
                sortedLanguageMap1.put(subtags[0], subtags[1]);
                sortedLanguageMap1.put(subtags[1], subtags[0]);
            } else if (subtags.length > 2) {
                for (int i = 0; i < subtags.length; i++) {
                    sortedLanguageMap2.put(subtags[i], createLangArray(i, subtags));
                }
            } else {
                    throw new RuntimeException("New case, need implementation."
                        + " A language subtag \"" + preferred
                        + "\" is registered for more than two subtags. ");
            }
        }

        for (String preferred : initialRegionVariantMap.keySet()) {
            subtags =
                initialRegionVariantMap.get(preferred).toString().split(",");

            sortedRegionVariantMap.put(subtags[0], subtags[1]);
            sortedRegionVariantMap.put(subtags[1], subtags[0]);
        }

    }

    /* create the array of subtags excluding the subtag at index location */
    private static String[] createLangArray(int index, String[] subtags) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < subtags.length; i++) {
            if (i != index) {
                list.add(subtags[i]);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private static String generateValuesString(String[] values) {
        String outputStr = "";
        for (int i = 0; i < values.length; i++) {
            if (i != values.length - 1) {
                outputStr = outputStr + "\"" + values[i] + "\", ";
            } else {
                outputStr = outputStr + "\"" + values[i] + "\"";
            }

        }
        return outputStr;
    }

    private static final String COPYRIGHT = "/*\n"
        + " * Copyright (c) 2012, %d, Oracle and/or its affiliates. All rights reserved.\n"
        + " * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n"
        + " *\n"
        + " * This code is free software; you can redistribute it and/or modify it\n"
        + " * under the terms of the GNU General Public License version 2 only, as\n"
        + " * published by the Free Software Foundation.  Oracle designates this\n"
        + " * particular file as subject to the \"Classpath\" exception as provided\n"
        + " * by Oracle in the LICENSE file that accompanied this code.\n"
        + " *\n"
        + " * This code is distributed in the hope that it will be useful, but WITHOUT\n"
        + " * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n"
        + " * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n"
        + " * version 2 for more details (a copy is included in the LICENSE file that\n"
        + " * accompanied this code).\n"
        + " *\n"
        + " * You should have received a copy of the GNU General Public License version\n"
        + " * 2 along with this work; if not, write to the Free Software Foundation,\n"
        + " * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.\n"
        + " *\n"
        + " * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA\n"
        + " * or visit www.oracle.com if you need additional information or have any\n"
        + " * questions.\n"
        + "*/\n\n";

    private static final String headerText =
        "package sun.util.locale;\n\n"
        + "import java.util.HashMap;\n"
        + "import java.util.Map;\n\n"
        + "final class LocaleEquivalentMaps {\n\n"
        + "    static final Map<String, String> singleEquivMap;\n"
        + "    static final Map<String, String[]> multiEquivsMap;\n"
        + "    static final Map<String, String> regionVariantEquivMap;\n\n"
        + "    static {\n"
        + "        singleEquivMap = new HashMap<>(";

    private static final String footerText =
        "    }\n\n"
        + "}";

    private static String getOpenJDKCopyright() {
        int year = ZonedDateTime.now(ZoneId
                .of("America/Los_Angeles")).getYear();
        return String.format(Locale.US, COPYRIGHT, year);
    }

    /**
     * The input lsr data file is in UTF-8, so theoretically for the characters
     * beyond US-ASCII, the generated Java String literals need to be Unicode
     * escaped (\\uXXXX) while writing to a file. But as of now, there is not
     * the case since we don't use "description", "comment" or alike.
     */
    private static void generateSourceCode(String fileName) {

        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(fileName))) {
            writer.write(getOpenJDKCopyright());
            writer.write(headerText
                + (int)(sortedLanguageMap1.size() / 0.75f + 1) + ");\n"
                + "        multiEquivsMap = new HashMap<>("
                + (int)(sortedLanguageMap2.size() / 0.75f + 1) + ");\n"
                + "        regionVariantEquivMap = new HashMap<>("
                + (int)(sortedRegionVariantMap.size() / 0.75f + 1) + ");\n\n"
                + "        // This is an auto-generated file and should not be manually edited.\n"
                + "        //   LSR Revision: " + LSRrevisionDate);
            writer.newLine();

            for (String key : sortedLanguageMap1.keySet()) {
                String value = sortedLanguageMap1.get(key);
                writer.write("        singleEquivMap.put(\""
                    + key + "\", \"" + value + "\");");
                writer.newLine();
            }

            writer.newLine();
            for (String key : sortedLanguageMap2.keySet()) {
                String[] values = sortedLanguageMap2.get(key);

                if (values.length >= 2) {
                    writer.write("        multiEquivsMap.put(\""
                        + key + "\", new String[] {"
                        + generateValuesString(values) + "});");
                    writer.newLine();
                }
            }

            writer.newLine();
            for (String key : sortedRegionVariantMap.keySet()) {
                String value = sortedRegionVariantMap.get(key);
                writer.write("        regionVariantEquivMap.put(\""
                    + key + "\", \"" + value + "\");");
                writer.newLine();
            }

            writer.write(footerText);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
        }

    }

}
