/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4486049 6282555 6318622
 * @summary toString method fails if size changes in between a call to size
 *           and an attempt to iterate.
 * @author Josh Bloch, Martin Buchholz
 */

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class ToString {
    private static void realMain(String[] args) {
        testCollection(new LinkedHashSet<Object>() {
                public int size() {
                    return super.size() + 1; // Lies, lies, all lies!
                }});
        testCollection(new ArrayList<Object>());
        testCollection(new Vector<Object>());
        testCollection(new CopyOnWriteArrayList<Object>());
        testCollection(new CopyOnWriteArraySet<Object>());
    }

    private static void testCollection(Collection<Object> c) {
        System.out.println(c.getClass());
        equal(c.toString(), "[]");
        check(c.add("x"));
        equal(c.toString(), "[x]");
        check(c.add("y"));
        equal(c.toString(), "[x, y]");
        check(c.add(null));
        equal(c.toString(), "[x, y, null]");
        if (c instanceof AbstractCollection) {
            check(c.add(c));
            equal(c.toString(), "[x, y, null, (this Collection)]");
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() { passed++; }
    static void fail() { failed++; Thread.dumpStack(); }
    static void fail(String msg) { System.out.println(msg); fail(); }
    static void unexpected(Throwable t) { failed++; t.printStackTrace(); }
    static void check(boolean cond) { if (cond) pass(); else fail(); }
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y); fail(); }}

    public static void main(String[] args) throws Throwable {
        try { realMain(args); } catch (Throwable t) { unexpected(t); }

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
}
