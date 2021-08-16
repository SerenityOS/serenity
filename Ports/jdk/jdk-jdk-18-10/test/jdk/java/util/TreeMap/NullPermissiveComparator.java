/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6467933
 * @summary Test proper handling of comparators permitting nulls
 * @author Martin Buchholz
 */

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("unchecked")
public class NullPermissiveComparator {

    static void equal(Map m, String s) {
        equal(m.toString(), s);
    }

    static void realMain(String[] args) throws Throwable {
        final Comparator nullLow = new Comparator() {
                public int compare(Object x, Object y) {
                    return x == y ?  0 :
                        x == null ? -1 :
                        y == null ?  1 :
                        ((Comparable)x).compareTo(y); }};

        final Comparator nullHigh = new Comparator() {
                public int compare(Object x, Object y) {
                    return x == y ?  0 :
                        x == null ?  1 :
                        y == null ? -1 :
                        ((Comparable)x).compareTo(y); }};

        TreeMap m = new TreeMap(nullLow);

        m.put("a", "A");
        m.put("b", "B");
        m.put("c", "C");

        equal(m, "{a=A, b=B, c=C}");
        equal(m.headMap("b"), "{a=A}");
        equal(m.tailMap("b"), "{b=B, c=C}");
        equal(m.headMap(null), "{}");
        equal(m.tailMap(null), "{a=A, b=B, c=C}");

        m.put(null, "NULL");

        equal(m, "{null=NULL, a=A, b=B, c=C}");
        equal(m.headMap("b"), "{null=NULL, a=A}");
        equal(m.tailMap("b"), "{b=B, c=C}");
        equal(m.headMap(null), "{}");
        equal(m.tailMap(null), "{null=NULL, a=A, b=B, c=C}");

        m = new TreeMap(nullHigh);

        m.put("a", "A");
        m.put("b", "B");
        m.put("c", "C");

        equal(m, "{a=A, b=B, c=C}");
        equal(m.headMap("b"), "{a=A}");
        equal(m.tailMap("b"), "{b=B, c=C}");
        equal(m.headMap(null), "{a=A, b=B, c=C}");
        equal(m.tailMap(null), "{}");

        m.put(null, "NULL");

        equal(m, "{a=A, b=B, c=C, null=NULL}");
        equal(m.headMap("b"), "{a=A}");
        equal(m.tailMap("b"), "{b=B, c=C, null=NULL}");
        equal(m.headMap(null), "{a=A, b=B, c=C}");
        equal(m.tailMap(null), "{null=NULL}");
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
