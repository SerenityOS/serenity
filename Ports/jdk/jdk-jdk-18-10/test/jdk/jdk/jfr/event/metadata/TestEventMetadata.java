/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.ValueDescriptor;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.metadata.TestEventMetadata
 */

public class TestEventMetadata {

    /*
     * Short guide to writing event metadata
     * =====================================

     * Name
     * ----
     *
     * Symbolic name that is used to identify an event, or a field. Referred to
     * as "id" and "field" in trace.xml-files and @Name in the Java API. If it is
     * the name of an event, the name should be prefixed "jdk.", which
     * happens automatically for native events.
     *
     * The name should be short, but not so brief that collision is likely with
     * future events or fields. It should only consist of letters and numbers.
     * Use Java naming convention , i.e. "FileRead" for an event and
     * "allocationRate" for a field. Do not use "_" and don't add the word
     * "Event" to the event name.
     *
     * Abbreviations should be avoided, but may be acceptable if the name
     * becomes long, or if it is a well established acronym. Write whole words,
     * i.e. "allocation" instead of "alloc". The name should not be a reserved
     * Java keyword, i.e "void" or "class".
     *
     * Label
     * -----
     *
     * Describes a human-readable name, typically 1-3 words. Use headline-style
     * capitalization, capitalize the first and last words, and all nouns,
     * pronouns, adjectives, verbs and adverbs. Do not include ending
     * punctuation.
     *
     * Description
     * -----------
     *
     * Describes an event with a sentence or two. It's better to omit the
     * description then copying the label. Use sentence-style
     * capitalization, capitalize the first letter of the first word, and any
     * proper names such as the word Java. If the description is one sentence,
     * period should not be included.
     *
     *
     * Do not forget to set proper units for fields, i.e "NANOS", "MILLS",
     * "TICKSPAN" ,"BYETS", "PECENTAGE" etc. in native and @Timespan, @Timespan
     * etc. in Java.
     */
    public static void main(String[] args) throws Exception {
        Set<String> types = new HashSet<>();
        // These types contains reserved keywords (unfortunately) so
        // exclude them from the check.
        types.add("jdk.types.StackTrace");
        types.add("java.lang.Class");
        List<EventType> eventTypes = FlightRecorder.getFlightRecorder().getEventTypes();
        Set<String> eventNames= new HashSet<>();
        for (EventType eventType : eventTypes) {
            verifyEventType(eventType);
            verifyValueDesscriptors(eventType.getFields(), types);
            System.out.println();
            String eventName = eventType.getName();
            if (eventNames.contains(eventName)) {
                throw new Exception("Event with name " +eventName+ " already exists");
            }
            eventNames.add(eventName);
            Set<String> fieldNames = new HashSet<>();
            for (ValueDescriptor v : eventType.getFields()) {
                String fieldName = v.getName();
                if (fieldNames.contains(fieldName)) {
                    throw new Exception("Field with name " + fieldName +" is already in use in event name " +eventName);
                }
                fieldNames.add(fieldName);
            }
        }
    }

    private static void verifyValueDesscriptors(List<ValueDescriptor> fields, Set<String> visitedTypes) {
        for (ValueDescriptor v : fields) {
            if (!visitedTypes.contains(v.getTypeName())) {
                visitedTypes.add(v.getTypeName());
                verifyValueDesscriptors(v.getFields(), visitedTypes);
            }
            verifyValueDescriptor(v);
        }
    }

    private static void verifyValueDescriptor(ValueDescriptor v) {
        verifyName(v.getName());
        verifyLabel(v.getLabel());
        verifyDescription(v.getDescription());
    }

    private static void verifyDescription(String description) {
        if (description == null) {
            return;
        }
        Asserts.assertTrue(description.length() > 10, "Description must be at least ten characters");
        Asserts.assertTrue(description.length() < 300, "Description should not exceed 300 characters. Found " + description);
        Asserts.assertTrue(description.length() == description.trim().length(), "Description should not have trim character at start or end");
        Asserts.assertFalse(description.endsWith(".") && description.indexOf(".") == description.length() - 1, "Single sentence descriptions should not use end punctuation");
    }

    private static void verifyName(String name) {
        System.out.println("Verifying name: " + name);
        Asserts.assertNotEquals(name, null, "Name not allowed to be null");
        Asserts.assertTrue(name.length() > 1, "Name must be at least two characters");
        Asserts.assertTrue(name.length() < 32, "Name should not exceed 32 characters");
        Asserts.assertFalse(isReservedKeyword(name),"Name must not be reserved keyword in the Java language (" + name + ")");
        char firstChar = name.charAt(0);
        Asserts.assertTrue(Character.isAlphabetic(firstChar), "Name must start with a character");
        Asserts.assertTrue(Character.isLowerCase(firstChar), "Name must start with lower case letter");
        Asserts.assertTrue(Character.isJavaIdentifierStart(firstChar), "Not valid first character for Java identifier");
        for (int i = 1; i < name.length(); i++) {
            Asserts.assertTrue(Character.isJavaIdentifierPart(name.charAt(i)), "Not valid character for a Java identifier");
            Asserts.assertTrue(Character.isAlphabetic(name.charAt(i)), "Name must consists of characters, found '" + name.charAt(i) + "'");
        }
        Asserts.assertFalse(name.contains("ID"), "'ID' should not be used in name, consider using 'Id'");
        checkCommonAbbreviations(name);
    }

    private static void verifyLabel(String label) {
        Asserts.assertNotEquals(label, null, "Label not allowed to be null");
        Asserts.assertTrue(label.length() > 1, "Name must be at least two characters");
        Asserts.assertTrue(label.length() < 45, "Label should not exceed 45 characters, use description to explain " + label);
        Asserts.assertTrue(label.length() == label.trim().length(), "Label should not have trim character at start and end");
        Asserts.assertTrue(Character.isUpperCase(label.charAt(0)), "Label should start with upper case letter");
        for (int i = 0; i < label.length(); i++) {
            char c = label.charAt(i);
            Asserts.assertTrue(Character.isDigit(c) || Character.isAlphabetic(label.charAt(i)) || c == ' ' || c == '(' || c == ')' || c == '-', "Label should only consist of letters or space, found '" + label.charAt(i)
                    + "'");
        }
    }

    private static void verifyEventType(EventType eventType) {
        System.out.println("Verifying event: " + eventType.getName());
        verifyDescription(eventType.getDescription());
        verifyLabel(eventType.getLabel());
        Asserts.assertNotEquals(eventType.getName(), null, "Name not allowed to be null");
        Asserts.assertTrue(eventType.getName().startsWith(EventNames.PREFIX), "OpenJDK events must start with " + EventNames.PREFIX);
        String name = eventType.getName().substring(EventNames.PREFIX.length());
        Asserts.assertFalse(isReservedKeyword(name),"Name must not be reserved keyword in the Java language (" + name + ")");
        checkCommonAbbreviations(name);
          char firstChar = name.charAt(0);
        Asserts.assertFalse(name.contains("ID"), "'ID' should not be used in name, consider using 'Id'");
        Asserts.assertTrue(Character.isAlphabetic(firstChar), "Name " + name + " must start with a character");
        Asserts.assertTrue(Character.isUpperCase(firstChar), "Name " + name + " must start with upper case letter");
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            Asserts.assertTrue(Character.isAlphabetic(c) || Character.isDigit(c), "Name " + name + " must consists of characters or numbers, found '" + name.charAt(i) + "'");
        }
    }

    static boolean isReservedKeyword(String s) {
        String[] keywords = new String[] {
                // "module", "requires", "exports", "to", "uses", "provides", "with", module-info.java
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while" };
        for (int i = 0; i < keywords.length; i++) {
            if (s.equals(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    private static void checkCommonAbbreviations(String name) {
        String lowerCased = name.toLowerCase();
        Asserts.assertFalse(lowerCased.contains("info") && !lowerCased.contains("information"), "Use 'information' instead 'info' in name");
        Asserts.assertFalse(lowerCased.contains("alloc") && !lowerCased.contains("alloca"), "Use 'allocation' instead 'alloc' in name");
        Asserts.assertFalse(lowerCased.contains("config") && !(lowerCased.contains("configuration") || lowerCased.contains("filterconfigured")), "Use 'configuration' instead of 'config' in name");
        Asserts.assertFalse(lowerCased.contains("evac") && !lowerCased.contains("evacu"), "Use 'evacuation' instead of 'evac' in name");
        Asserts.assertFalse(lowerCased.contains("stat") && !(lowerCased.contains("state") ||lowerCased.contains("statistic") ||lowerCased.contains("filterstatus")) , "Use 'statistics' instead of 'stat' in name");
        Asserts.assertFalse(name.contains("ID") , "Use 'id' or 'Id' instead of 'ID' in name");
    }
}
