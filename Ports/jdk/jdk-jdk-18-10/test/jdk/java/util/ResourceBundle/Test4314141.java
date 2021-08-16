/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
    @test
    @summary Verify a few assertions of the new specification of ResourceBundle.getBundle
    @build Test4314141B Test4314141B_fr_CH Test4314141B_es_ES
    @run main Test4314141
    @bug 4314141
*/

import java.util.ResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;

public class Test4314141 {

    public static void main(String[] args) {
        Locale reservedLocale = Locale.getDefault();
        try {
            testCandidateOmission();
            testExample();
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    /**
     * Tests that candidate bundle names where the final component is an empty string are omitted.
     * Previous versions of ResourceBundle might attempt to load bundles with a trailing
     * underscore (e.g., "Test4314141_") resulting from concatenation with an empty string.
     * This is no longer permitted.
     */
    static void testCandidateOmission() {
        Locale.setDefault(Locale.US);
        doTestCandidateOmission("de", "DE", "EURO", new String[] {"_de", ""});
        doTestCandidateOmission("de", "DE", "", new String[] {"_de", ""});
        doTestCandidateOmission("de", "", "EURO", new String[] {"_de", ""});
        doTestCandidateOmission("de", "", "", new String[] {"_de", ""});
        doTestCandidateOmission("", "DE", "EURO", new String[] {"__DE", ""});
        doTestCandidateOmission("", "DE", "", new String[] {"__DE", ""});
        doTestCandidateOmission("", "", "EURO", new String[] {"___EURO", ""});
        doTestCandidateOmission("", "", "", new String[] {""});
    }

    static void doTestCandidateOmission(String language, String country, String variant,
            String[] expectedSuffixes) {
        doTest("Test4314141A", language, country, variant, expectedSuffixes);
    }

    /**
     * Verifies the example from the getBundle specification.
     */
    static void testExample() {
        Locale.setDefault(new Locale("en", "UK"));
        doTestExample("fr", "CH", new String[] {"_fr_CH.class", "_fr.properties", ".class"});
        doTestExample("fr", "FR", new String[] {"_fr.properties", ".class"});
        doTestExample("de", "DE", new String[] {"_en.properties", ".class"});
        doTestExample("en", "US", new String[] {"_en.properties", ".class"});
        doTestExample("es", "ES", new String[] {"_es_ES.class", ".class"});
    }

    static void doTestExample(String language, String country, String[] expectedSuffixes) {
        doTest("Test4314141B", language, country, "", expectedSuffixes);
    }

    static void doTest(String baseName, String language, String country, String variant,
            String[] expectedSuffixes) {
        System.out.print("Looking for " + baseName + " \"" + language + "\", \"" + country + "\", \"" + variant + "\"");
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, new Locale(language, country, variant));
        System.out.print(" => got ");
        String previousName = null;
        int nameCount = 0;
        for (int i = 3; i >= 0; i--) {
            String name = bundle.getString("name" + i);
            if (!name.equals(previousName)) {
                if (previousName != null) {
                    System.out.print(", ");
                }
                System.out.print(name);
                if (!name.equals(baseName + expectedSuffixes[nameCount++])) {
                    System.out.println();
                    throw new RuntimeException("Error: got unexpected resource bundle");
                }
                previousName = name;
            }
        }
        System.out.println();
        if (nameCount != expectedSuffixes.length) {
            throw new RuntimeException("Error: parent chain too short");
        }
    }
}
