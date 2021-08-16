/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.scripting
 * @library modules classpath/pearscript
 * @build ReloadTest org.pear.PearScript org.pear.PearScriptEngineFactory bananascript/*
 * @run testng/othervm ReloadTest
 * @summary Basic test of ServiceLoader.reload
 */

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.ServiceLoader.*;
import javax.script.ScriptEngineFactory;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ReloadTest {

    public void testReload() {
        ServiceLoader<ScriptEngineFactory> sl = load(ScriptEngineFactory.class);
        List<String> names1 = sl.stream()
                .map(Provider::get)
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toList());
        assertFalse(names1.isEmpty());
        sl.reload();
        List<String> names2 = sl.stream()
                .map(Provider::get)
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toList());
        assertEquals(names1, names2);
    }

    @Test(expectedExceptions = { ConcurrentModificationException.class })
    public void testIteratorHasNext() {
        ServiceLoader<ScriptEngineFactory> sl = load(ScriptEngineFactory.class);
        Iterator<ScriptEngineFactory> iterator = sl.iterator();
        sl.reload();
        iterator.hasNext();
    }

    @Test(expectedExceptions = { ConcurrentModificationException.class })
    public void testIteratorNext() {
        ServiceLoader<ScriptEngineFactory> sl = load(ScriptEngineFactory.class);
        Iterator<ScriptEngineFactory> iterator = sl.iterator();
        assertTrue(iterator.hasNext());
        sl.reload();
        iterator.next();
    }

    @Test(expectedExceptions = { ConcurrentModificationException.class })
    public void testStreamFindAny() {
        ServiceLoader<ScriptEngineFactory> sl = load(ScriptEngineFactory.class);
        Stream<Provider<ScriptEngineFactory>> stream = sl.stream();
        sl.reload();
        stream.findAny();
    }

    @Test(expectedExceptions = { ConcurrentModificationException.class })
    public void testSpliteratorTryAdvance() {
        ServiceLoader<ScriptEngineFactory> sl = load(ScriptEngineFactory.class);
        Stream<Provider<ScriptEngineFactory>> stream = sl.stream();
        Spliterator<Provider<ScriptEngineFactory>> spliterator = stream.spliterator();
        sl.reload();
        spliterator.tryAdvance(System.out::println);
    }

}
