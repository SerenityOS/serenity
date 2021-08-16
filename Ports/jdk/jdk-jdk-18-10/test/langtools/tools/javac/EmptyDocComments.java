/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160204 4234488
 * @summary Create empty javadoc comments and verify that javac does not crash.
 * @compile EmptyDocComments.java
 */

// WARNING: This file intentionally contains whitespace characters at the end of
// some lines.  Do not delete them!

// If this file fails to compile, then the test has failed.  The test does not
// need to be run.

public class EmptyDocComments
{
    public static void Main(String [] args)
    {
    }

    // Verify that empty doc comments don't cause a crash.

    /**
     *  */
    public static void emptyDocComment0()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains whitespace");
        System.out.println("- ends on same line as potential comment");
    }

    /**
     ***/
    public static void emptyDocComment1()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains nothing");
        System.out.println("- ends on same line as potential comment");
    }

    /** */
    public static void emptyDocComment2()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains whitespace");
        System.out.println("- ends on same line as comment start");
    }

    /**
      *
      */
    public static void emptyDocComment3()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains whitespace");
        System.out.println("- ends on new line ");
    }

    /***/
    public static void emptyDocComment4()
    {
        System.out.println("minimal javadoc comment");
        System.out.println("- contains nothing");
        System.out.println("- ends on same line as comment start");
    }

    /**/
    public static void emptyDocComment5()
    {
        System.out.println("minimal _java_ comment");
        System.out.println("- contains nothing");
        System.out.println("- ends on same line as comment start");
    }

    /** **** */
    public static void emptyDocComment6()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains \"*\"");
        System.out.println("- ends on same line as comment start");
    }

    // Verify that we properly handle very small, non-empty comments.

    /** a */
    public static void singleChar0()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains a single character");
        System.out.println("- ends on same line as comment start");
    }

    /**
     * a      */
    public static void singleChar1()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains a single character and trailing whitespace");
        System.out.println("- ends on same line as potential comment");
    }

    /**
     * a
     */
    public static void singleChar2()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains a single character, no trailing whitespace");
        System.out.println("- ends on new line ");
    }

    /**
       a
     */
    public static void singleChar3()
    {
        System.out.println("javadoc comment");
        System.out.println("- contains a single character and trailing whitespace");
        System.out.println("- ends on new line ");
    }
}
