/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8209824
 * @summary per latest JDK code coverage report, 2 methods replaceStaleEntry
 *          and prevIndex in ThreadLocal.ThreadLocalMap are not touched
 *          by any JDK regression tests, this is to trigger the code paths.
 * @modules java.base/java.lang:+open
 * @run testng ReplaceStaleEntry
 */

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ReplaceStaleEntry {

    public static int INITIAL_CAPACITY;

    @BeforeClass
    public static void setup() throws Exception {
        Class<?> clazz = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
        Field f = clazz.getDeclaredField("INITIAL_CAPACITY");
        f.setAccessible(true);
        INITIAL_CAPACITY = f.getInt(null);
        System.out.println("INITIAL_CAPACITY: " + INITIAL_CAPACITY);
    }

    /**
     * This test triggers the code path to replaceStaleEntry, so as prevIndex is
     * triggered too as it's called by replaceStaleEntry.
     * The code paths must be triggered by reusing an already used entry in the
     * map's entry table. The code below
     *  1. first occupies the entries
     *  2. then expunges the entries by removing strong references to thread locals
     *  3. then reuse some of entries by adding more thread locals to the thread
     * and, the above steps are run for several times by adding more and more
     * thread locals, also trigger rehash of the map.
     */
    @Test
    public static void test() {
        Map<Integer, ThreadLocal> locals = new HashMap<>();
        for (int j = 1; j < 32; j *= 2) {
            int loop = INITIAL_CAPACITY * j;
            for (int i = 0; i < loop; i++) {
                ThreadLocal l = new ThreadLocal<Integer>();
                l.set(i);
                locals.put(i, l);
            }
            locals.keySet().stream().forEach(i -> {
                assertEquals((int)locals.get(i).get(), (int)i, "Unexpected thread local value!");
            });
            locals.clear();
            System.gc();
        }
    }
}
