/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Constructor;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/*
 * @test
 * @bug 8068373 8075110 8075156
 * @summary Ensure a code point U+0000 null control character is detected.
 * @run main/othervm -Djava.util.prefs.userRoot=. CodePointZeroPrefsTest
 */
public class CodePointZeroPrefsTest
{
    public static void main(String[] args) throws Exception
    {
        int failures = 0;

        Preferences node = Preferences.userRoot().node("com/acme/testing");

        // --- put() ---

        // legal key and value
        try {
            node.put("a", "1");
        } catch (IllegalArgumentException iae) {
            System.err.println("Unexpected IllegalArgumentException for legal put() key");
            failures++;
        }

        // illegal key only
        try {
            node.put("a\u0000b", "1");
            System.err.println("IllegalArgumentException not thrown for illegal put() key");
            failures++;
        } catch (IllegalArgumentException iae) {
            // do nothing
        }

        // illegal value only
        try {
            node.put("ab", "2\u00003");
            System.err.println("IllegalArgumentException not thrown for illegal put() value");
            failures++;
        } catch (IllegalArgumentException iae) {
            // do nothing
        }

        // illegal key and value
        try {
            node.put("a\u0000b", "2\u00003");
            System.err.println("IllegalArgumentException not thrown for illegal put() entry");
            failures++;
        } catch (IllegalArgumentException iae) {
            // do nothing
        }

        // --- get ---

        // illegal key only
        try {
            String theDefault = "default";
            String value = node.get("a\u0000b", theDefault);
            System.err.println("IllegalArgumentException not thrown for illegal get() key");
            failures++;
        } catch (IllegalArgumentException iae) {
            // do nothing
        }

        // --- remove ---

        // illegal key only
        try {
            node.remove("a\u0000b");
            System.err.println("IllegalArgumentException not thrown for illegal remove() key");
            failures++;
        } catch (IllegalArgumentException iae) {
            // do nothing
        }

        node.removeNode();

        if (failures != 0) {
            throw new RuntimeException("CodePointZeroPrefsTest failed with "
                + failures + " errors!");
        }
    }
}
