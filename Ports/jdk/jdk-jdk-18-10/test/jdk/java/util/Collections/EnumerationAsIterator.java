/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072726
 * @summary Tests for Enumeration-to-Iterator conversion.
 * @run testng EnumerationAsIterator
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.testng.Assert.*;

@Test
public class EnumerationAsIterator {
    static Object[] of(String description, Supplier<Enumeration<?>> s, Collection<?> exp) {
        return new Object[]{description, s, exp};
    }

    static Object[] of(String description, Collection<?> c, Collection<?> exp) {
        return of(description, () -> Collections.enumeration(c), exp);
    }

    /**
     * A wrapper Enumeration that doesn't override the
     * default method on Enumeration.
     */
    static <T> Enumeration<T> wrapInDefault(Enumeration<T> e) {
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return e.hasMoreElements();
            }

            @Override
            public T nextElement() {
                return e.nextElement();
            }
        };
    }

    @DataProvider
    public static Iterator<Object[]> unmodifiable() {
        return Arrays.asList(
            of("Default-wrapped ArrayList",
               () -> wrapInDefault(
                   Collections.enumeration(new ArrayList<>(Arrays.asList("a")))),
               Arrays.asList("a")),

            of("Unmodifiable ArrayList",
               Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a"))),
               Arrays.asList("a")),

            of("Modifiable ArrayList",
               new ArrayList<>(Arrays.asList("a")),
               Arrays.asList("a"))
        ).iterator();
    }

    @DataProvider
    public static Iterator<Object[]> others() {
        return Arrays.asList(
            of("Default Collections.emptyEnumeration()",
               () -> wrapInDefault(Collections.emptyEnumeration()),
               Collections.emptyList()),

            of("Collections.emptyEnumeration()",
               Collections::emptyEnumeration,
               Collections.emptyList()),

            of("Collections.emptyList()",
               Collections.emptyList(),
               Collections.emptyList()),

            of("Collections.singletonList()",
               Collections.singletonList("a"),
               Collections.singletonList("a")),

            of("Arrays.asList(...)",
               Arrays.asList("a", "b", "c"),
               Arrays.asList("a", "b", "c"))
        ).iterator();
    }

    @DataProvider
    public static Iterator<Object[]> all() {
        List<Object[]> all = new ArrayList<>();
        unmodifiable().forEachRemaining(all::add);
        others().forEachRemaining(all::add);
        return all.iterator();
    }

    @Test(dataProvider = "all")
    public void consumeByNext(String description, Supplier<Enumeration<?>> s, Collection<?> exp) {
        Iterator<?> i = s.get().asIterator();
        int count = 0;
        while (i.hasNext()) {
            assertTrue(i.hasNext());

            i.next();
            count++;
        }
        assertEquals(count, exp.size());

        assertFalse(i.hasNext());

        try {
            i.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test(dataProvider = "all")
    public void consumeByForEachRemaining(String description,
                                          Supplier<Enumeration<?>> s,
                                          Collection<?> exp) {
        Iterator<?> i = s.get().asIterator();
        AtomicInteger ai = new AtomicInteger();
        i.forEachRemaining(e -> ai.getAndIncrement());
        assertEquals(ai.get(), exp.size());
        i.forEachRemaining(e -> ai.getAndIncrement());
        assertEquals(ai.get(), exp.size());

        assertFalse(i.hasNext());

        try {
            i.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test(dataProvider = "all")
    public void consumeByNextThenForEachRemaining(String description,
                                                  Supplier<Enumeration<?>> s,
                                                  Collection<?> exp) {
        Iterator<?> i = s.get().asIterator();
        AtomicInteger ai = new AtomicInteger();
        if (i.hasNext()) {
            i.next();
            ai.getAndIncrement();
        }
        i.forEachRemaining(e -> ai.getAndIncrement());
        assertEquals(ai.get(), exp.size());
        i.forEachRemaining(e -> ai.getAndIncrement());
        assertEquals(ai.get(), exp.size());

        assertFalse(i.hasNext());

        try {
            i.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test(dataProvider = "all")
    public void contents(String description, Supplier<Enumeration<?>> s, Collection<?> exp) {
        assertEquals(copy(s.get()), exp);
    }

    private List<?> copy(Enumeration<?> input) {
        List<Object> output = new ArrayList<>();
        input.asIterator().forEachRemaining(output::add);
        return output;
    }

    @Test(dataProvider = "unmodifiable",
          expectedExceptions=UnsupportedOperationException.class)
    public void removeThrowsAfterAdvancingE(String description,
                                            Supplier<Enumeration<?>> s,
                                            Collection<?> exp) {
        Enumeration<?> e = s.get();
        e.nextElement();
        e.asIterator().remove();
    }

    @Test(dataProvider = "unmodifiable",
          expectedExceptions=UnsupportedOperationException.class)
    public void removeThrowsAfterAdvancingI(String description,
                                            Supplier<Enumeration<?>> s,
                                            Collection<?> exp) {
        Iterator<?> i = s.get().asIterator();
        i.next();
        i.remove();
    }
}
