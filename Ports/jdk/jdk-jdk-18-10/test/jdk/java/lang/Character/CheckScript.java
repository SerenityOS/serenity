/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6945564 6959267 7033561 7070436 7198195 8032446 8072600 8221431
 * @summary  Check that the j.l.Character.UnicodeScript
 * @library /lib/testlibrary/java/lang
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.Character.UnicodeScript;

public class CheckScript {

    public static void main(String[] args) throws Exception {
        File fScripts;
        File fAliases;
        if (args.length == 0) {
            fScripts = UCDFiles.SCRIPTS.toFile();
            fAliases = UCDFiles.PROPERTY_VALUE_ALIASES.toFile();
        } else if (args.length == 2) {
            fScripts = new File(args[0]);
            fAliases = new File(args[1]);
        } else {
            System.out.println("java CharacterScript Scripts.txt PropertyValueAliases.txt");
            throw new RuntimeException("Datafile name should be specified.");
        }

        Matcher m = Pattern.compile("(\\p{XDigit}+)(?:\\.{2}(\\p{XDigit}+))?\\s+;\\s+(\\w+)\\s+#.*").matcher("");
        String line = null;
        HashMap<String,ArrayList<Integer>> scripts = new HashMap<>();
        try (BufferedReader sbfr = new BufferedReader(new FileReader(fScripts))) {
            while ((line = sbfr.readLine()) != null) {
                if (line.length() <= 1 || line.charAt(0) == '#') {
                    continue;
                }
                m.reset(line);
                if (m.matches()) {
                    int start = Integer.parseInt(m.group(1), 16);
                    int end = (m.group(2)==null)?start
                                                :Integer.parseInt(m.group(2), 16);
                    String name = m.group(3).toLowerCase(Locale.ENGLISH);
                    ArrayList<Integer> ranges = scripts.get(name);
                    if (ranges == null) {
                        ranges = new ArrayList<Integer>();
                        scripts.put(name, ranges);
                    }
                    ranges.add(start);
                    ranges.add(end);
                }
            }
        }
        // check all defined ranges
        Integer[] ZEROSIZEARRAY = new Integer[0];
        for (String name : scripts.keySet()) {
            System.out.println("Checking " + name + "...");
            Integer[] ranges = scripts.get(name).toArray(ZEROSIZEARRAY);
            Character.UnicodeScript expected =
                Character.UnicodeScript.forName(name);

            int off = 0;
            while (off < ranges.length) {
                int start = ranges[off++];
                int end = ranges[off++];
                for (int cp = start; cp <= end; cp++) {
                    Character.UnicodeScript script =
                        Character.UnicodeScript.of(cp);
                    if (script != expected) {
                        throw new RuntimeException(
                            "UnicodeScript failed: cp=" +
                            Integer.toHexString(cp) +
                            ", of(cp)=<" + script + "> but <" +
                            expected + "> is expected");
                   }
                }
            }
        }
        // check all codepoints
        for (int cp = 0; cp < Character.MAX_CODE_POINT; cp++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.UNKNOWN) {
                if (Character.getType(cp) != Character.UNASSIGNED &&
                    Character.getType(cp) != Character.SURROGATE &&
                    Character.getType(cp) != Character.PRIVATE_USE)
                    throw new RuntimeException(
                        "UnicodeScript failed: cp=" +
                        Integer.toHexString(cp) +
                        ", of(cp)=<" + script + "> but UNKNOWN is expected");
            } else {
                Integer[] ranges =
                    scripts.get(script.name().toLowerCase(Locale.ENGLISH))
                           .toArray(ZEROSIZEARRAY);
                int off = 0;
                boolean found = false;
                while (off < ranges.length) {
                    int start = ranges[off++];
                    int end = ranges[off++];
                    if (cp >= start && cp <= end)
                        found = true;
                }
                if (!found) {
                    throw new RuntimeException(
                        "UnicodeScript failed: cp=" +
                        Integer.toHexString(cp) +
                        ", of(cp)=<" + script +
                        "> but NOT in ranges of this script");

                }
            }
        }
        // check all aliases
        m = Pattern.compile("sc\\s*;\\s*(\\p{Alpha}{4})\\s*;\\s*([\\p{Alpha}|_]+)\\s*.*").matcher("");
        line = null;
        try (BufferedReader sbfr = new BufferedReader(new FileReader(fAliases))) {
            while ((line = sbfr.readLine()) != null) {
                if (line.length() <= 1 || line.charAt(0) == '#') {
                    continue;
                }
                m.reset(line);
                if (m.matches()) {
                    String alias = m.group(1);
                    String name = m.group(2);
                    // HRKT -> Katakana_Or_Hiragana not supported
                    if ("HRKT".equals(alias.toUpperCase(Locale.ENGLISH)))
                        continue;
                    if (Character.UnicodeScript.forName(alias) !=
                        Character.UnicodeScript.forName(name)) {
                        throw new RuntimeException(
                            "UnicodeScript failed: alias<" + alias +
                            "> does not map to <" + name + ">");
                    }
                }
            }
        }
    }
}
