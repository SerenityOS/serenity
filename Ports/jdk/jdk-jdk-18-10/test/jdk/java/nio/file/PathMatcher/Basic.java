/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4313887 6866397 8073445
 * @summary Unit test for java.nio.file.PathMatcher
 */

import java.nio.file.*;
import java.util.regex.PatternSyntaxException;

public class Basic {
    static int failures;

    static void match(String name, String pattern, boolean expectedToMatch) {
        System.out.format("%s -> %s", name, pattern);
        Path file = Paths.get(name);
        boolean matched =  file.getFileSystem()
            .getPathMatcher("glob:" + pattern).matches(file);
        if (matched)
            System.out.print(" (matched)");
        else
            System.out.print(" (no match)");
        if (matched != expectedToMatch) {
            System.out.println(" ==> UNEXPECTED RESULT!");
            failures++;
        } else {
            System.out.println(" OKAY");
        }
    }

    static void assertMatch(String path, String pattern) {
        match(path, pattern, true);
    }

    static void assertNotMatch(String path, String pattern) {
        match(path, pattern, false);
    }

    static void assertBadPattern(String path, String pattern) {
        System.out.format("Compile bad pattern %s\t", pattern);
        try {
            FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            System.out.println("Compiled ==> UNEXPECTED RESULT!");
            failures++;
        } catch (PatternSyntaxException e) {
            System.out.println("Failed to compile ==> OKAY");
        }
    }

    static void assertRegExMatch(String path, String pattern) {
        System.out.format("Test regex pattern: %s", pattern);
        Path file = Paths.get(path);
        boolean matched =  file.getFileSystem()
                               .getPathMatcher("regex:" + pattern).matches(file);
        if (matched) {
            System.out.println(" OKAY");
        } else {
            System.out.println(" ==> UNEXPECTED RESULT!");
            failures++;
        }
    }


    public static void main(String[] args) {
        // basic
        assertMatch("foo.html", "foo.html");
        assertNotMatch("foo.html", "foo.htm");
        assertNotMatch("foo.html", "bar.html");

        // match zero or more characters
        assertMatch("foo.html", "f*");
        assertMatch("foo.html", "*.html");
        assertMatch("foo.html", "foo.html*");
        assertMatch("foo.html", "*foo.html");
        assertMatch("foo.html", "*foo.html*");
        assertNotMatch("foo.html", "*.htm");
        assertNotMatch("foo.html", "f.*");

        // match one character
        assertMatch("foo.html", "?oo.html");
        assertMatch("foo.html", "??o.html");
        assertMatch("foo.html", "???.html");
        assertMatch("foo.html", "???.htm?");
        assertNotMatch("foo.html", "foo.???");

        // group of subpatterns
        assertMatch("foo.html", "foo{.html,.class}");
        assertMatch("foo.html", "foo.{class,html}");
        assertNotMatch("foo.html", "foo{.htm,.class}");

        // bracket expressions
        assertMatch("foo.html", "[f]oo.html");
        assertMatch("foo.html", "[e-g]oo.html");
        assertMatch("foo.html", "[abcde-g]oo.html");
        assertMatch("foo.html", "[abcdefx-z]oo.html");
        assertMatch("foo.html", "[!a]oo.html");
        assertMatch("foo.html", "[!a-e]oo.html");
        assertMatch("foo-bar", "foo[-a-z]bar");     // match dash
        assertMatch("foo.html", "foo[!-]html");     // match !dash

        // groups of subpattern with bracket expressions
        assertMatch("foo.html", "[f]oo.{[h]tml,class}");
        assertMatch("foo.html", "foo.{[a-z]tml,class}");
        assertMatch("foo.html", "foo.{[!a-e]tml,.class}");

        // assume special characters are allowed in file names
        assertMatch("{foo}.html", "\\{foo*");
        assertMatch("{foo}.html", "*\\}.html");
        assertMatch("[foo].html", "\\[foo*");
        assertMatch("[foo].html", "*\\].html");

        // errors
        assertBadPattern("foo.html", "*[a--z]");            // bad range
        assertBadPattern("foo.html", "*[a--]");             // bad range
        assertBadPattern("foo.html", "*[a-z");              // missing ]
        assertBadPattern("foo.html", "*{class,java");       // missing }
        assertBadPattern("foo.html", "*.{class,{.java}}");  // nested group
        assertBadPattern("foo.html", "*.html\\");           // nothing to escape

        // platform specific
        if (System.getProperty("os.name").startsWith("Windows")) {
            assertMatch("C:\\foo", "C:\\\\f*");
            assertMatch("C:\\FOO", "c:\\\\f*");
            assertMatch("C:\\foo\\bar\\gus", "C:\\\\**\\\\gus");
            assertMatch("C:\\foo\\bar\\gus", "C:\\\\**");
        } else {
            assertMatch("/tmp/foo", "/tmp/*");
            assertMatch("/tmp/foo/bar", "/tmp/**");

            // some special characters not allowed on Windows
            assertMatch("myfile?", "myfile\\?");
            assertMatch("one\\two", "one\\\\two");
            assertMatch("one*two", "one\\*two");
        }

        // regex syntax
        assertRegExMatch("foo.html", ".*\\.html");

        if (System.getProperty("os.name").startsWith("Windows")) {
            assertRegExMatch("foo012", "foo\\d+");
            assertRegExMatch("fo o", "fo\\so");
            assertRegExMatch("foo", "\\w+");
        }

        // unknown syntax
        try {
            System.out.format("Test unknown syntax");
            FileSystems.getDefault().getPathMatcher("grep:foo");
            System.out.println(" ==> NOT EXPECTED TO COMPILE");
            failures++;
        } catch (UnsupportedOperationException e) {
            System.out.println(" OKAY");
        }

        // GLOB_SYNTAX case sensitivity of getPathMatcher: should not throw UOE
        try {
            FileSystems.getDefault().getPathMatcher("glob:java");
            FileSystems.getDefault().getPathMatcher("Glob:java");
            FileSystems.getDefault().getPathMatcher("GLOB:java");
            System.out.println("Test GLOB_SYNTAX case sensitivity OKAY");
        } catch (UnsupportedOperationException e) {
            System.err.println("getPathMatcher GLOB_SYNTAX case sensitivity");
            e.printStackTrace();
            failures++;
        }

        // REGEX_SYNTAX case sensitivity of getPathMatcher: should not throw UOE
        try {
            FileSystems.getDefault().getPathMatcher("regex:java");
            FileSystems.getDefault().getPathMatcher("Regex:java");
            FileSystems.getDefault().getPathMatcher("RegEx:java");
            FileSystems.getDefault().getPathMatcher("REGEX:java");
            System.out.println("Test REGEX_SYNTAX case sensitivity OKAY");
        } catch (UnsupportedOperationException e) {
            System.err.println("getPathMatcher REGEX_SYNTAX case sensitivity");
            e.printStackTrace();
            failures++;
        }

        if (failures > 0)
            throw new RuntimeException(failures +
                " sub-test(s) failed - see log for details");
    }
}
