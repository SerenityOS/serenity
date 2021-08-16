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
 * @summary Test ServiceLoader with two iterators, interleaving their use
 *   to test that they don't interfere with each other
 * @run testng TwoIterators
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TwoIterators {

    // service type
    public static interface S { }

    // service provider implementations
    public static class S1 implements S { }
    public static class S2 implements S { }

    private ClassLoader testClassLoader;

    // creates the services configuration file and sets the ClassLoader
    @BeforeClass
    void setup() throws Exception {
        String classes = System.getProperty("test.classes");
        Path dir = Paths.get(classes, "META-INF", "services");
        Files.createDirectories(dir);
        Path config = dir.resolve(S.class.getName());
        Files.write(config, Arrays.asList(S1.class.getName(), S2.class.getName()));

        this.testClassLoader = TwoIterators.class.getClassLoader();
    }

    @Test
    public void testSequentialUse1() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        Iterator<S> iterator1 = sl.iterator();
        iterator1.next();
        iterator1.next();
        assertFalse(iterator1.hasNext());

        Iterator<S> iterator2 = sl.iterator();
        iterator2.next();
        iterator2.next();
        assertFalse(iterator2.hasNext());
    }

    @Test
    public void testSequentialUse2() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        Iterator<S> iterator1 = sl.iterator();
        Iterator<S> iterator2 = sl.iterator();

        iterator1.next();
        iterator1.next();
        assertFalse(iterator1.hasNext());

        iterator2.next();
        iterator2.next();
        assertFalse(iterator2.hasNext());
    }

    @Test
    public void testInterleaved1() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        Iterator<S> iterator1 = sl.iterator();
        Iterator<S> iterator2 = sl.iterator();

        iterator1.next();
        iterator2.next();
        iterator1.next();
        iterator2.next();
        assertFalse(iterator1.hasNext());
        assertFalse(iterator2.hasNext());
    }

    @Test
    public void testInterleaved2() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        Iterator<S> iterator1 = sl.iterator();
        iterator1.next();

        Iterator<S> iterator2 = sl.iterator();

        assertTrue(iterator1.hasNext());
        assertTrue(iterator2.hasNext());

        iterator1.next();
        iterator2.next();
        iterator2.next();

        assertFalse(iterator1.hasNext());
        assertFalse(iterator2.hasNext());
    }

    @Test
    public void testInterleaved3() {
        ServiceLoader<S> sl = ServiceLoader.load(S.class, testClassLoader);

        Iterator<S> iterator1 = sl.iterator();
        iterator1.next();

        Iterator<S> iterator2 = sl.iterator();

        assertTrue(iterator2.hasNext());
        assertTrue(iterator1.hasNext());

        iterator2.next();
        iterator2.next();
        iterator1.next();

        assertFalse(iterator1.hasNext());
        assertFalse(iterator2.hasNext());
    }
}

