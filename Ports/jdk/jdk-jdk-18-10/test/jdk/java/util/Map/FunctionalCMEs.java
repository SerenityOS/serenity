/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/**
 * @test
 * @bug 8071667
 * @summary Ensure that ConcurrentModificationExceptions are thrown as specified from Map methods that accept Functions
 * @author bchristi
 * @build Defaults
 * @run testng FunctionalCMEs
 */
public class FunctionalCMEs {
    static final String KEY = "key";

    @DataProvider(name = "Maps", parallel = true)
    private static Iterator<Object[]> makeMaps() {
        return Arrays.asList(
                // Test maps that CME
                new Object[]{new HashMap<>(), true},
                new Object[]{new Hashtable<>(), true},
                new Object[]{new LinkedHashMap<>(), true},
                new Object[]{new TreeMap<>(), true},
                // Test default Map methods - no CME
                new Object[]{new Defaults.ExtendsAbstractMap<>(), false}
        ).iterator();
    }

    @Test(dataProvider = "Maps")
    public void testComputeIfAbsent(Map<String,String> map, boolean expectCME) {
        checkCME(() -> {
            map.computeIfAbsent(KEY, k -> {
                putToForceRehash(map);
                return "computedValue";
            });
        }, expectCME);
    }

    @Test(dataProvider = "Maps")
    public void testCompute(Map<String,String> map, boolean expectCME) {
        checkCME(() -> {
            map.compute(KEY, mkBiFunc(map));
        }, expectCME);
    }

    @Test(dataProvider = "Maps")
    public void testComputeWithKeyMapped(Map<String,String> map, boolean expectCME) {
        map.put(KEY, "firstValue");
        checkCME(() -> {
            map.compute(KEY, mkBiFunc(map));
        }, expectCME);
    }

    @Test(dataProvider = "Maps")
    public void testComputeIfPresent(Map<String,String> map, boolean expectCME) {
        map.put(KEY, "firstValue");
        checkCME(() -> {
           map.computeIfPresent(KEY, mkBiFunc(map));
        }, expectCME);
    }

    @Test(dataProvider = "Maps")
    public void testMerge(Map<String,String> map, boolean expectCME) {
        map.put(KEY, "firstValue");
        checkCME(() -> {
            map.merge(KEY, "nextValue", mkBiFunc(map));
        }, expectCME);
    }

    @Test(dataProvider = "Maps")
    public void testForEach(Map<String,String> map, boolean ignored) {
        checkCME(() -> {
            map.put(KEY, "firstValue");
            putToForceRehash(map);
            map.forEach((k,v) -> {
                map.remove(KEY);
            });
        }, true);
    }

    @Test(dataProvider = "Maps")
    public void testReplaceAll(Map<String,String> map, boolean ignored) {
        checkCME(() -> {
            map.put(KEY, "firstValue");
            putToForceRehash(map);
            map.replaceAll((k,v) -> {
                map.remove(KEY);
                return "computedValue";
            });
        },true);
    }

    private static void checkCME(Runnable code, boolean expectCME) {
        try {
            code.run();
        } catch (ConcurrentModificationException cme) {
            if (expectCME) { return; } else { throw cme; }
        }
        if (expectCME) {
            throw new RuntimeException("Expected CME, but wasn't thrown");
        }
    }

    private static BiFunction<String,String,String> mkBiFunc(Map<String,String> map) {
        return (k,v) -> {
            putToForceRehash(map);
            return "computedValue";
        };
    }

    private static void putToForceRehash(Map<String,String> map) {
        for (int i = 0; i < 64; ++i) {
            map.put(i + "", "value");
        }
    }
}
