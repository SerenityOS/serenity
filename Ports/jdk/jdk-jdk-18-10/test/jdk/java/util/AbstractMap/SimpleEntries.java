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

/*
 * @test
 * @bug 4904074 6328220 6330389
 * @summary Basic tests for SimpleEntry, SimpleImmutableEntry
 * @author Martin Buchholz
 */

import java.util.Map;

import static java.util.AbstractMap.SimpleEntry;
import static java.util.AbstractMap.SimpleImmutableEntry;

public class SimpleEntries {
    private static String k = "foo";
    private static Long v = 1L;
    private static Long v2 = 2L;
    private static void realMain(String[] args) throws Throwable {
        testEntry(new SimpleEntry<String,Long>(k,v));
        testEntry(new SimpleImmutableEntry<String,Long>(k,v));
        testNullEntry(new SimpleEntry<String,Long>(null,null));
        testNullEntry(new SimpleImmutableEntry<String,Long>(null,null));
    }

    private static void testEntry(Map.Entry<String,Long> e) {
        equal(e.getKey(), k);
        equal(e.getValue(), v);
        equal(e, new SimpleEntry<String,Long>(k,v));
        check(! e.equals(new SimpleEntry<String,Long>(k,v2)));
        check(! e.equals(null));
        equal(e, new SimpleImmutableEntry<String,Long>(k,v));
        equal(e.toString(), k+"="+v);
        if (e instanceof SimpleEntry) {
            equal(e.setValue(v2), v);
            equal(e.getValue(), v2);
            equal(e.setValue(null), v2);
            equal(e.getValue(), null);
        } else {
            try { e.setValue(v2); fail(); }
            catch (UnsupportedOperationException t) {}
            catch (Throwable t) { unexpected(t); }
        }
    }

    private static void testNullEntry(Map.Entry<String,Long> e) {
        equal(e.getKey(), null);
        equal(e.getValue(), null);
        equal(e, new SimpleEntry<String,Long>(null, null));
        equal(e, new SimpleImmutableEntry<String,Long>(null, null));
        equal(e.toString(), "null=null");
        if (e instanceof SimpleEntry) {
            equal(e.setValue(v), null);
            equal(e.getValue(), v);
        } else {
            try { e.setValue(null); fail(); }
            catch (UnsupportedOperationException t) {}
            catch (Throwable t) { unexpected(t); }
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
