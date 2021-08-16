/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 5045147
 * @summary Test handling of null with empty Map
 * @author Mike Duigou
 */

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.reflect.*;

public class EmptyMapAndNulls {

    @SuppressWarnings("rawtypes")
    static void realMain(String[] args) throws Throwable {
        // No comparator
        Map<String,String> comparable = new TreeMap<>();

        // insert null into empty map (5045147 failure)
        try {
            comparable.put(null, "anything");
            fail("null shouldn't be accepted");
        } catch (NullPointerException failed) {
            pass();
        }

        // insert non-null into empty map
        try {
            comparable.put("test", "anything");
            pass();
        } catch (NullPointerException failed) {
            fail();
        }

        // insert null into non-empty map
        try {
            comparable.put(null, "anything");
            fail("null shouldn't be accepted");
        } catch (NullPointerException failed) {
            pass();
        }

        // Comparator (String.CASE_INSENSITIVE_ORDER). Intentionally a raw type.
        Map comparator = new TreeMap(String.CASE_INSENSITIVE_ORDER);

        // insert null into empty map (5045147 failure)
        try {
            comparator.put(null, "anything");
            fail("null shouldn't be accepted");
        } catch (NullPointerException failed) {
            pass();
        }

        // insert non-null into empty map
        try {
            comparator.put("test", "anything");
            pass();
        } catch (NullPointerException failed) {
            fail();
        }

        // insert null into non-empty map
        try {
            comparator.put(null, "anything");
            fail("null shouldn't be accepted");
        } catch (NullPointerException failed) {
            pass();
        }

        comparator.clear();

        // insert non-String into empty map (5045147 failure)
        try {
            comparator.put(new Object(), "anything");
            fail("Object shouldn't be accepted");
        } catch (ClassCastException failed) {
            pass();
        }

    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
