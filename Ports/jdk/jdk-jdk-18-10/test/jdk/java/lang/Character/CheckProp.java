/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7037261 7070436 7198195 8032446 8072600 8221431 8229831
 * @summary  Check j.l.Character.isLowerCase/isUppercase/isAlphabetic/isIdeographic/
 *              isUnicodeIdentifierStart/isUnicodeIdentifierPart
 * @library /lib/testlibrary/java/lang
 */

import java.util.regex.*;
import java.util.*;
import java.io.*;
import static java.lang.Character.*;

public class CheckProp {

    public static void main(String[] args) {
        Map<String, List<Integer>> propMap =  new LinkedHashMap<>();
        List.of(UCDFiles.PROP_LIST.toFile(), UCDFiles.DERIVED_PROPS.toFile()).stream()
            .forEach(f -> readPropMap(propMap, f));

        Integer[] otherLowercase = propMap.get("Other_Lowercase").toArray(new Integer[0]);
        Integer[] otherUppercase = propMap.get("Other_Uppercase").toArray(new Integer[0]);
        Integer[] otherAlphabetic = propMap.get("Other_Alphabetic").toArray(new Integer[0]);
        Integer[] ideographic = propMap.get("Ideographic").toArray(new Integer[0]);
        Integer[] IDStart = propMap.get("ID_Start").toArray(new Integer[0]);
        Integer[] IDContinue = propMap.get("ID_Continue").toArray(new Integer[0]);

        int fails = 0;
        for (int cp = MIN_CODE_POINT; cp < MAX_CODE_POINT; cp++) {
            int type = getType(cp);
            if (isLowerCase(cp) !=
                (type == LOWERCASE_LETTER ||
                 Arrays.binarySearch(otherLowercase, cp) >= 0))
            {
                fails++;
                System.err.printf("Wrong isLowerCase(U+%04x)\n", cp);
            }
            if (isUpperCase(cp) !=
                (type == UPPERCASE_LETTER ||
                 Arrays.binarySearch(otherUppercase, cp) >= 0))
            {
                fails++;
                System.err.printf("Wrong isUpperCase(U+%04x)\n", cp);
            }
            if (isAlphabetic(cp) !=
                (type == UPPERCASE_LETTER || type == LOWERCASE_LETTER ||
                 type == TITLECASE_LETTER || type == MODIFIER_LETTER  ||
                 type == OTHER_LETTER     || type == OTHER_LETTER ||
                 type == LETTER_NUMBER ||
                 Arrays.binarySearch(otherAlphabetic, cp) >=0))
            {
                fails++;
                System.err.printf("Wrong isAlphabetic(U+%04x)\n", cp);
            }
            if (isIdeographic(cp) !=
                (Arrays.binarySearch(ideographic, cp) >= 0))
            {
                fails++;
                System.err.printf("Wrong isIdeographic(U+%04x)\n", cp);
            }
            if (isUnicodeIdentifierStart(cp) !=
                (cp == 0x2E2F ||
                 Arrays.binarySearch(IDStart, cp) >= 0))
            {
                fails++;
                System.err.printf("Wrong isUnicodeIdentifierStart(U+%04x)\n", cp);
            }
            if (isUnicodeIdentifierPart(cp) !=
                (isIdentifierIgnorable(cp) ||
                 cp == 0x2E2F ||
                 Arrays.binarySearch(IDContinue, cp) >= 0))
            {
                fails++;
                System.err.printf("Wrong isUnicodeIdentifierPart(U+%04x)\n", cp);
            }
        }
        if (fails != 0)
            throw new RuntimeException("CheckProp failed=" + fails);
    }

    private static void readPropMap(Map<String, List<Integer>> propMap, File fPropList) {
        try {
            BufferedReader sbfr = new BufferedReader(new FileReader(fPropList));
            Matcher m = Pattern.compile("(\\p{XDigit}+)(?:\\.{2}(\\p{XDigit}+))?\\s*;\\s+(\\w+)\\s+#.*").matcher("");

            String line = null;
            int lineNo = 0;
            while ((line = sbfr.readLine()) != null) {
                lineNo++;
                if (line.length() <= 1 || line.charAt(0) == '#') {
                    continue;
                }
                m.reset(line);
                if (m.matches()) {
                    int start = Integer.parseInt(m.group(1), 16);
                    int end = (m.group(2)==null)?start
                              :Integer.parseInt(m.group(2), 16);
                    String name = m.group(3);

                    List<Integer> list = propMap.get(name);
                    if (list == null) {
                        list = new ArrayList<Integer>();
                        propMap.put(name, list);
                    }
                    while (start <= end)
                        list.add(start++);
                } else {
                    System.out.printf("Warning: Unrecognized line %d <%s>%n", lineNo, line);
                }
            }
            sbfr.close();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        //for (String name: propMap.keySet()) {
        //    System.out.printf("%s    %d%n", name, propMap.get(name).size());
        //}
    }
}
