/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6265919
 */

import java.util.*;

public class Constructors {

    private static Class NPE = NullPointerException.class;

    private static int fail = 0;
    private static int pass = 0;

    private static Throwable first;

    private static void pass() {
        pass++;
    }

    private static void fail(Throwable t, Class c) {
        String s = t.getClass().getName() + " constructor did not throw " + c.getName();
         if (first == null)
            first = new RuntimeException(s);
        System.err.println("FAILED: " + s);
        fail++;
    }

    private static void nullTests() {
        IllegalFormatException ex;
        try {
            ex = new DuplicateFormatFlagsException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new FormatFlagsConversionMismatchException(null, 'a');
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new IllegalFormatConversionException('b', null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new IllegalFormatFlagsException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new MissingFormatArgumentException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new MissingFormatWidthException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new UnknownFormatConversionException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }

        try {
            ex = new UnknownFormatFlagsException(null);
            fail(ex, NPE);
        }  catch (NullPointerException x) {
            pass();
        }
    }

    public static void main(String [] args) {
        nullTests();

        if (fail != 0)
            throw new RuntimeException((fail + pass) + " tests: "
                                       + fail + " failure(s), first", first);
        else
            System.out.println("all " + (fail + pass) + " tests passed");
    }
}
