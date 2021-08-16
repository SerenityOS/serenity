/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/*
 * @test
 * @bug 8048330
 * @summary Test convenience static factory methods on Set.
 * @run testng SetFactories
 */


public class SetFactories {

    static final int NUM_STRINGS = 20; // should be larger than the largest fixed-arg overload
    static final String[] stringArray;
    static {
        String[] sa = new String[NUM_STRINGS];
        for (int i = 0; i < NUM_STRINGS; i++) {
            sa[i] = String.valueOf((char)('a' + i));
        }
        stringArray = sa;
    }

    static Object[] a(Set<String> act, Set<String> exp) {
        return new Object[] { act, exp };
    }

    static Set<String> hashSetOf(String... args) {
        return new HashSet<>(Arrays.asList(args));
    }

    @DataProvider(name="empty")
    public Iterator<Object[]> empty() {
        return Collections.singletonList(
            // actual, expected
            a(Set.of(), Collections.emptySet())
        ).iterator();
    }

    @DataProvider(name="nonempty")
    public Iterator<Object[]> nonempty() {
        return Arrays.asList(
            // actual, expected
            a(   Set.of("a"),
              hashSetOf("a")),
            a(   Set.of("a", "b"),
              hashSetOf("a", "b")),
            a(   Set.of("a", "b", "c"),
              hashSetOf("a", "b", "c")),
            a(   Set.of("a", "b", "c", "d"),
              hashSetOf("a", "b", "c", "d")),
            a(   Set.of("a", "b", "c", "d", "e"),
              hashSetOf("a", "b", "c", "d", "e")),
            a(   Set.of("a", "b", "c", "d", "e", "f"),
              hashSetOf("a", "b", "c", "d", "e", "f")),
            a(   Set.of("a", "b", "c", "d", "e", "f", "g"),
              hashSetOf("a", "b", "c", "d", "e", "f", "g")),
            a(   Set.of("a", "b", "c", "d", "e", "f", "g", "h"),
              hashSetOf("a", "b", "c", "d", "e", "f", "g", "h")),
            a(   Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i"),
              hashSetOf("a", "b", "c", "d", "e", "f", "g", "h", "i")),
            a(   Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
              hashSetOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")),
            a(   Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
                 Set.of("j", "i", "h", "g", "f", "e", "d", "c", "b", "a")),
            a(   Set.of(stringArray),
              hashSetOf(stringArray))
        ).iterator();
    }

    @DataProvider(name="all")
    public Iterator<Object[]> all() {
        List<Object[]> all = new ArrayList<>();
        empty().forEachRemaining(all::add);
        nonempty().forEachRemaining(all::add);
        return all.iterator();
    }

    @Test(dataProvider="all", expectedExceptions=UnsupportedOperationException.class)
    public void cannotAdd(Set<String> act, Set<String> exp) {
        act.add("x");
    }

    @Test(dataProvider="nonempty", expectedExceptions=UnsupportedOperationException.class)
    public void cannotRemove(Set<String> act, Set<String> exp) {
        act.remove(act.iterator().next());
    }

    @Test(dataProvider="all")
    public void contentsMatch(Set<String> act, Set<String> exp) {
        assertEquals(act, exp);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed2() {
        Set<String> set = Set.of("a", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed3() {
        Set<String> set = Set.of("a", "b", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed4() {
        Set<String> set = Set.of("a", "b", "c", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed5() {
        Set<String> set = Set.of("a", "b", "c", "d", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed6() {
        Set<String> set = Set.of("a", "b", "c", "d", "e", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed7() {
        Set<String> set = Set.of("a", "b", "c", "d", "e", "f", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed8() {
        Set<String> set = Set.of("a", "b", "c", "d", "e", "f", "g", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed9() {
        Set<String> set = Set.of("a", "b", "c", "d", "e", "f", "g", "h", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowed10() {
        Set<String> set = Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "a");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void dupsDisallowedN() {
        String[] array = stringArray.clone();
        array[0] = array[1];
        Set<String> set = Set.of(array);
    }

    @Test(dataProvider="all")
    public void hashCodeEqual(Set<String> act, Set<String> exp) {
        assertEquals(act.hashCode(), exp.hashCode());
    }

    @Test(dataProvider="all")
    public void containsAll(Set<String> act, Set<String> exp) {
        assertTrue(act.containsAll(exp));
        assertTrue(exp.containsAll(act));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed1() {
        Set.of((String)null); // force one-arg overload
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed2a() {
        Set.of("a", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed2b() {
        Set.of(null, "b");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed3() {
        Set.of("a", "b", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed4() {
        Set.of("a", "b", "c", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed5() {
        Set.of("a", "b", "c", "d", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed6() {
        Set.of("a", "b", "c", "d", "e", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed7() {
        Set.of("a", "b", "c", "d", "e", "f", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed8() {
        Set.of("a", "b", "c", "d", "e", "f", "g", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed9() {
        Set.of("a", "b", "c", "d", "e", "f", "g", "h", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowed10() {
        Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullDisallowedN() {
        String[] array = stringArray.clone();
        array[0] = null;
        Set.of(array);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void nullArrayDisallowed() {
        Set.of((Object[])null);
    }

    @Test(dataProvider="all", expectedExceptions=NullPointerException.class)
    public void containsNullShouldThrowNPE(Set<String> act, Set<String> exp) {
        act.contains(null);
    }

    @Test(dataProvider="all")
    public void serialEquality(Set<String> act, Set<String> exp) {
        // assume that act.equals(exp) tested elsewhere
        Set<String> copy = serialClone(act);
        assertEquals(act, copy);
        assertEquals(copy, exp);
    }

    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(obj);
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    Set<Integer> genSet() {
        return new HashSet<>(Arrays.asList(1, 2, 3));
    }

    @Test
    public void copyOfResultsEqual() {
        Set<Integer> orig = genSet();
        Set<Integer> copy = Set.copyOf(orig);

        assertEquals(orig, copy);
        assertEquals(copy, orig);
    }

    @Test
    public void copyOfModifiedUnequal() {
        Set<Integer> orig = genSet();
        Set<Integer> copy = Set.copyOf(orig);
        orig.add(4);

        assertNotEquals(orig, copy);
        assertNotEquals(copy, orig);
    }

    @Test
    public void copyOfIdentity() {
        Set<Integer> orig = genSet();
        Set<Integer> copy1 = Set.copyOf(orig);
        Set<Integer> copy2 = Set.copyOf(copy1);

        assertNotSame(orig, copy1);
        assertSame(copy1, copy2);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void copyOfRejectsNullCollection() {
        Set<Integer> set = Set.copyOf(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void copyOfRejectsNullElements() {
        Set<Integer> set = Set.copyOf(Arrays.asList(1, null, 3));
    }

    @Test
    public void copyOfAcceptsDuplicates() {
        Set<Integer> set = Set.copyOf(Arrays.asList(1, 1, 2, 3, 3, 3));
        assertEquals(set, Set.of(1, 2, 3));
    }
}
