/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6267067 6351336 6389198
 * @summary unit test for javac List
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.util.*;
import com.sun.tools.javac.util.List;

public class TList {
    public static void main(String[] args) {
        new TList().run();
    }

    String[][] data = {
        { },
        { "1" },
        { "1", "2" },
        { "1", "2" }, // different but equal
        { "1", "2", "3", "4", "X", "X", "X", "8", "9", "10" } // contains duplicates
    };

    Map<java.util.List<String>,List<String>> examples;

    void run() {
        examples = new LinkedHashMap<java.util.List<String>,List<String>>();
        for (String[] values: data)
            examples.put(Arrays.asList(values), createList(values));

        // 6351336: com.sun.tools.javac.util.List shouldn't extend java.util.AbstractList
        test_AbstractList();

        // general unit tests for java.util.List methods, including...
        // 6389198: com.sun.tools.javac.util.List.equals() violates java.util.List.equals() contract
        test_add_E();
        test_add_int_E();
        test_addAll_Collection();
        test_addAll_int_Collection();
        test_clear();
        test_contains_Object();
        test_contains_All();
        test_equals_Object();
        test_get_int();
        test_hashCode();
        test_indexOf_Object();
        test_isEmpty();
        test_iterator();
        test_lastIndexOf_Object();
        test_listIterator();
        test_listIterator_int();
        test_remove_int();
        test_remove_Object();
        test_removeAll_Collection();
        test_retainAll_Collection();
        test_set_int_E();
        test_size();
        test_subList_int_int();
        test_toArray();
        test_toArray_TArray();

        // tests for additional methods
        test_prependList_List();
        test_reverse();
    }

    // 6351336
    void test_AbstractList() {
        System.err.println("test AbstractList");
        if (AbstractList.class.isAssignableFrom(List.class))
            throw new AssertionError();
    }

    void test_add_E() {
        System.err.println("test add(E)");
        for (List<String> l: examples.values()) {
            try {
                l.add("test");
                throw new AssertionError();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    void test_add_int_E() {
        System.err.println("test add(int,E)");
        for (List<String> l: examples.values()) {
            try {
                l.add(0, "test");
                throw new AssertionError();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    void test_addAll_Collection() {
        System.err.println("test addAll(Collection)");
        for (List<String> l: examples.values()) {
            int l_size = l.size();
            for (java.util.List<String> arg: examples.keySet()) {
                try {
                    boolean modified = l.addAll(arg);
                    if (modified)
                        throw new AssertionError();
                } catch (UnsupportedOperationException e) {
                }
                if (l.size() != l_size)
                    throw new AssertionError();
            }
        }
    }

    void test_addAll_int_Collection() {
        System.err.println("test addAll(int,Collection)");
        for (List<String> l: examples.values()) {
            int l_size = l.size();
            for (java.util.List<String> arg: examples.keySet()) {
                try {
                    boolean modified = l.addAll(0, arg);
                    if (modified)
                        throw new AssertionError();
                } catch (UnsupportedOperationException e) {
                }
                if (l.size() != l_size)
                    throw new AssertionError();
            }
        }
    }

    void test_clear() {
        System.err.println("test clear()");
        for (List<String> l: examples.values()) {
            int l_size = l.size();
            try {
                l.clear();
                if (l_size > 0)
                    throw new AssertionError();
            } catch (UnsupportedOperationException e) {
            }
            if (l.size() != l_size)
                throw new AssertionError();
        }
    }

    void test_contains_Object() {
        System.err.println("test contains(Object)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            boolean expect = ref.contains("1");
            boolean found = l.contains("1");
            if (expect != found)
                throw new AssertionError();
        }
    }

    void test_contains_All() {
        System.err.println("test containsAll()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (java.util.List<String> arg: examples.keySet()) {
                boolean expect = ref.containsAll(arg);
                boolean found = l.containsAll(arg);
                if (expect != found)
                    throw new AssertionError();
            }
        }
    }

    // 6389198
    void test_equals_Object() {
        System.err.println("test equals(Object)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (java.util.List<String> arg: examples.keySet()) {
                boolean expect = ref.equals(arg);
                boolean found = l.equals(arg);
                if (expect != found) {
                    System.err.println("ref: " + ref);
                    System.err.println("l: " + l);
                    System.err.println("arg: " + arg);
                    System.err.println("expect: " + expect + ", found: " + found);
                    throw new AssertionError();
                }
            }
        }
    }

    void test_get_int() {
        System.err.println("test get(int)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (int i = -1; i <= ref.size(); i++) {
                boolean expectException = i < 0 || i >= ref.size();
                String expectValue = (expectException ? null : ref.get(i));
                try {
                    String foundValue = l.get(i);
                    if (expectException || !equal(expectValue, foundValue))
                        throw new AssertionError();
                } catch (IndexOutOfBoundsException ex) {
                    if (!expectException)
                        throw new AssertionError();
                }
            }
        }
    }

    void test_hashCode() {
        System.err.println("test hashCode()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            long expect = ref.hashCode();
            long found = l.hashCode();
            if (expect != found) {
                System.err.println("ref: " + ref);
                System.err.println("l: " + l);
                System.err.println("expect: " + expect);
                System.err.println("found: " + found);
                throw new AssertionError();
            }
        }
    }

    void test_indexOf_Object() {
        System.err.println("test indexOf(Object)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (int i = -1; i < ref.size(); i++) {
                String arg = (i == -1 ? "NOT IN LIST" : ref.get(i));
                int expect = ref.indexOf(arg);
                int found = l.indexOf(arg);
                if (expect != found)
                    throw new AssertionError();
            }
        }
    }

    void test_isEmpty() {
        System.err.println("test isEmpty()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            boolean expect = ref.isEmpty();
            boolean found = l.isEmpty();
            if (expect != found)
                throw new AssertionError();
        }
    }

    void test_iterator() {
        System.err.println("test iterator()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            if (!equal(l.iterator(), ref.iterator()))
                throw new AssertionError();
        }
    }

    void test_lastIndexOf_Object() {
        System.err.println("test lastIndexOf(Object)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (int i = -1; i < ref.size(); i++) {
                String arg = (i == -1 ? "NOT IN LIST" : ref.get(i));
                int expect = ref.lastIndexOf(arg);
                int found = l.lastIndexOf(arg);
                if (expect != found)
                    throw new AssertionError();
            }
        }
    }

    void test_listIterator() {
        System.err.println("test listIterator()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            if (!equal(l.listIterator(), ref.listIterator()))
                throw new AssertionError();
        }
    }

    void test_listIterator_int() {
        System.err.println("test listIterator(int)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (int i = 0; i < ref.size(); i++) {
                if (!equal(l.listIterator(i), ref.listIterator(i)))
                    throw new AssertionError();
            }
        }
    }

    void test_remove_int() {
        System.err.println("test remove(int)");
        for (List<String> l: examples.values()) {
            try {
                l.remove(0);
                throw new AssertionError();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    void test_remove_Object() {
        System.err.println("test remove(Object)");
        for (List<String> l: examples.values()) {
            boolean hasX = l.contains("X");
            try {
                l.remove("X");
                if (hasX)
                    throw new AssertionError();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    void test_removeAll_Collection() {
        System.err.println("test removeAll(Collection)");
        for (List<String> l: examples.values()) {
            int l_size = l.size();
            for (java.util.List<String> arg: examples.keySet()) {
                try {
                    boolean modified = l.removeAll(arg);
                    if (modified)
                        throw new AssertionError();
                } catch (UnsupportedOperationException e) {
                }
                if (l.size() != l_size)
                    throw new AssertionError();
            }
        }
    }

    void test_retainAll_Collection() {
        System.err.println("test retainAll(Collection)");
        for (List<String> l: examples.values()) {
            int l_size = l.size();
            for (java.util.List<String> arg: examples.keySet()) {
                try {
                    boolean modified = l.retainAll(arg);
                    if (modified)
                        throw new AssertionError();
                } catch (UnsupportedOperationException e) {
                }
                if (l.size() != l_size)
                    throw new AssertionError();
            }
        }
    }

    void test_set_int_E() {
        System.err.println("test set(int,E)");
        for (List<String> l: examples.values()) {
            try {
                l.set(0, "X");
                throw new AssertionError();
            } catch (UnsupportedOperationException ex) {
            }
        }
    }

    void test_size() {
        System.err.println("test size()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            int  expect = ref.size();
            int found = l.size();
            if (expect != found)
                throw new AssertionError();
        }
    }

    void test_subList_int_int() {
        System.err.println("test subList(int,int)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (int lwb = 0; lwb < ref.size(); lwb++) {
                for (int upb = lwb; upb <= ref.size(); upb++) {
                    if (!equal(l.subList(lwb, upb), ref.subList(lwb,upb)))
                    throw new AssertionError();
                }
            }
        }
    }

    void test_toArray() {
        System.err.println("test toArray()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            if (!equal(l.toArray(), ref.toArray()))
                throw new AssertionError();
        }
    }

    void test_toArray_TArray() {
        System.err.println("test toArray(E[])");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            if (!equal(l.toArray(new String[0]), ref.toArray(new String[0])))
                throw new AssertionError();
        }
    }

    void test_prependList_List() {
        System.err.println("test prependList(List<E>)");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            for (Map.Entry<java.util.List<String>, List<String>> arg_e: examples.entrySet()) {
                java.util.List<String> arg_ref = arg_e.getKey();
                List<String> arg = arg_e.getValue();
                java.util.List<String> expect = join(arg, ref);
                List<String> found = l.prependList(arg);
                // verify results, and that original and arg lists are unchanged
                if (!equal(expect, found)) {
                    System.err.println("ref: " + ref);
                    System.err.println("l: " + l);
                    System.err.println("arg: " + arg);
                    System.err.println("expect: " + expect);
                    System.err.println("found: " + found);
                    throw new AssertionError();
                }
                if (!equal(l, ref))
                    throw new AssertionError();
                if (!equal(arg, arg_ref))
                    throw new AssertionError();
            }
        }
    }

    void test_reverse() {
        System.err.println("test reverse()");
        for (Map.Entry<java.util.List<String>,List<String>> e: examples.entrySet()) {
            java.util.List<String> ref = e.getKey();
            List<String> l = e.getValue();
            java.util.List<String> expect = reverse(ref);
            List<String> found = l.reverse();
            if (l.size() < 2 && found != l)  // reverse of empty or singleton list is itself
                throw new AssertionError();
            if (!equal(l, ref)) // orginal should be unchanged
                throw new AssertionError();
            if (!equal(expect, found))
                throw new AssertionError();
        }
    }

    static <T> com.sun.tools.javac.util.List<T> createList(List<T> d) {
        com.sun.tools.javac.util.List<T> l = com.sun.tools.javac.util.List.nil();
        for (ListIterator<T> iter = d.listIterator(d.size()); iter.hasPrevious(); )
            l = l.prepend(iter.previous());
        return l;
    }

    static <T> com.sun.tools.javac.util.List<T> createList(T... d) {
        com.sun.tools.javac.util.List<T> l = com.sun.tools.javac.util.List.nil();
        for (int i = d.length - 1; i >= 0; i--)
            l = l.prepend(d[i]);
        return l;
    }

    static <T> boolean equal(T t1, T t2) {
        return (t1 == null ? t2 == null : t1.equals(t2));
    }

    static <T> boolean equal(Iterator<T> iter1, Iterator<T> iter2) {
        if (iter1 == null || iter2 == null)
            return (iter1 == iter2);

        while (iter1.hasNext() && iter2.hasNext()) {
            if (!equal(iter1.next(), iter2.next()))
                return false;
        }

        return (!iter1.hasNext() && !iter2.hasNext());
    }

    static <T> boolean equal(ListIterator<T> iter1, ListIterator<T> iter2) {
        if (iter1 == null || iter2 == null)
            return (iter1 == iter2);

        if (iter1.previousIndex() != iter2.previousIndex())
            return false;

        while (iter1.hasPrevious() && iter2.hasPrevious()) {
            iter1.previous();
            iter2.previous();
        }

        return equal((Iterator<T>) iter1, (Iterator<T>) iter2);
    }

    static <T> boolean equal(T[] a1, T[] a2) {
        if (a1 == null || a2 == null)
            return (a1 == a2);

        if (a1.length != a2.length)
            return false;

        for (int i = 0; i < a1.length; i++) {
            if (!equal(a1[i], a2[i]))
                return false;
        }

        return true;
    }

    static <T> java.util.List<T> join(java.util.List<T>... lists) {
        java.util.List<T> r = new ArrayList<T>();
        for (java.util.List<T> l: lists)
            r.addAll(l);
        return r;
    }

    static <T> java.util.List<T> reverse(java.util.List<T> l) {
        java.util.List<T> r = new ArrayList<T>(l.size());
        for (T t: l)
            r.add(0, t);
        return r;
    }
}
