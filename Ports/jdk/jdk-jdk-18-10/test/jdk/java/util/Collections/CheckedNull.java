/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6409434
 * @summary Test behavior of nulls in checked collections
 */

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

@SuppressWarnings({"unchecked","serial"})
public class CheckedNull {

    void test(String[] args) throws Throwable {
        testCollection(Collections.checkedCollection(
                           new ArrayList<String>(), String.class));
        testCollection(Collections.checkedList(
                           new ArrayList<String>(), String.class));
        testCollection(Collections.checkedSet(
                           new HashSet<String>(), String.class));

        final Comparator nullLow = new Comparator() {
                public int compare(Object x, Object y) {
                    return x == y ?  0 :
                        x == null ? -1 :
                        y == null ?  1 :
                        ((Comparable)x).compareTo(y); }};
        testCollection(Collections.checkedSortedSet(
                           new TreeSet<String>(nullLow), String.class));

        testMap(Collections.checkedMap(
                    new HashMap<String, String>(),
                    String.class, String.class));
    }

    ClassCastException cce(F f) {
        try { f.f(); fail(); return null; }
        catch (ClassCastException cce) { pass(); return cce; }
        catch (Throwable t) { unexpected(t); return null; }
    }

    void equalCCE(F ... fs) {
        String detailMessage = null;
        for (F f : fs)
            if (detailMessage == null)
                detailMessage = cce(f).getMessage();
            else
                equal(detailMessage, cce(f).getMessage());
    }

    void add(Collection c, Object o) {
        int s = c.size();
        check(! c.contains(o));
        check(c.add(o));
        check(c.contains(o));
        equal(c.size(), s+1);
        check(c.remove(o));
        check(! c.contains(o));
        check(c.addAll(singleton(o)));
        check(c.contains(o));
        equal(c.size(), s+1);
        check(c.remove(o));
        equal(c.size(), s);
    }

    void testCollection(final Collection c) {
        try {
            check(c.isEmpty());
            add(c, null);
            add(c, "foo");

            check(c.add("bar"));
            add(c, null);
            add(c, "foo");

            equalCCE(
                new F(){void f(){ c.add(1); }},
                new F(){void f(){ c.addAll(singleton(1)); }});

        } catch (Throwable t) { unexpected(t); }
    }

    void put(Map m, Object k, Object v) {
        int s = m.size();
        check(! m.containsKey(k));
        check(! m.containsValue(v));
        equal(null, m.put(k, v));
        check(m.containsKey(k));
        check(m.containsValue(v));
        equal(m.size(), s+1);
        equal(v, m.remove(k));
        check(! m.containsKey(k));
        check(! m.containsValue(v));
        m.putAll(singletonMap(k,v));
        check(m.containsKey(k));
        check(m.containsValue(v));
        equal(m.size(), s+1);
        equal(v,m.remove(k));
        equal(m.size(), s);
    }

    void testMap(final Map m) {
        try {
            check(m.isEmpty());

            put(m, "foo", null);
            put(m, null, "foo");
            put(m, null, null);
            put(m, "foo", "bar");

            m.put("a", "b");

            put(m, "foo", null);
            put(m, null, "foo");
            put(m, null, null);
            put(m, "foo", "bar");

            equalCCE(
                new F(){void f(){ m.put(1, "foo"); }},
                new F(){void f(){ m.putAll(singletonMap(1, "foo")); }});

            final Collection cheater = new ArrayList() {
                    public boolean contains(Object o) {
                        if (o instanceof Map.Entry)
                            ((Map.Entry)o).setValue(1);
                        return false; }};

            equalCCE(
                new F(){void f(){ m.put("foo", 1); }},
                new F(){void f(){ m.putAll(singletonMap("foo", 1)); }},
                new F(){void f(){
                    ((Map.Entry)m.entrySet().iterator().next()).setValue(1); }},
                new F(){void f(){
                    m.entrySet().removeAll(cheater);}},
                new F(){void f(){
                    m.entrySet().retainAll(cheater);}});

            equalCCE(
                new F(){void f(){ m.put(3, 1); }},
                new F(){void f(){ m.putAll(singletonMap(3, 1)); }});

            equal(m.size(), 1);
            equal(m.keySet(), singleton("a"));
            equal(m.entrySet(),
                  singleton(new AbstractMap.SimpleImmutableEntry("a","b")));

        } catch (Throwable t) { unexpected(t); }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new CheckedNull().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
}
