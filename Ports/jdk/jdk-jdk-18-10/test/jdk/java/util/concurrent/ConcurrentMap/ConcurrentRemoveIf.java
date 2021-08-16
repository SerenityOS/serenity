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

/*
 * @test
 * @run testng ConcurrentRemoveIf
 * @bug 8078645
 * @summary Test removeIf on views of concurrent maps
 */

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Test
public class ConcurrentRemoveIf {
    static final int K = 100;
    static final int SIZE = 1000;
    static final int HALF_SIZE = SIZE / 2;

    @DataProvider()
    public static Object[][] concurrentMapViewRemoveIfActions() {
        List<Object[]> rows = new ArrayList<>();

        // ConcurrentMap classes to test
        Map<String, Supplier<ConcurrentMap<Integer, Integer>>> maps = new HashMap<>();
        maps.put("ConcurrentHashMap", ConcurrentHashMap::new);
        maps.put("ConcurrentSkipListMap", ConcurrentSkipListMap::new);

        // ConcurrentMap actions
        Map<String, Consumer<ConcurrentMap<Integer, Integer>>> actions = new HashMap<>();
        actions.put(".entrySet().removeIf()", m -> m.entrySet().removeIf(e -> e.getValue() == 0));
        actions.put(".values().removeIf()", m -> m.values().removeIf(v -> v == 0));

        // ConcurrentNavigableMap actions
        Map<String, Consumer<ConcurrentNavigableMap<Integer, Integer>>> navActions = new HashMap<>();
        navActions.put(".headMap()/tailMap().entrySet().removeIf()",
                       m -> {
                           ConcurrentMap<Integer, Integer> left = m.headMap(HALF_SIZE, false);
                           ConcurrentMap<Integer, Integer> right = m.tailMap(HALF_SIZE, true);
                           left.entrySet().removeIf(e -> e.getValue() == 0);
                           right.entrySet().removeIf(e -> e.getValue() == 0);
                       });
        navActions.put(".headMap()/tailMap().values().removeIf()",
                       m -> {
                           ConcurrentMap<Integer, Integer> left = m.headMap(HALF_SIZE, false);
                           ConcurrentMap<Integer, Integer> right = m.tailMap(HALF_SIZE, true);
                           left.values().removeIf(v -> v == 0);
                           right.values().removeIf(v -> v == 0);
                       });
        navActions.put(".descendingMap().entrySet().removeIf()",
                       m -> {
                           ConcurrentMap<Integer, Integer> dm = m.descendingMap();
                           dm.entrySet().removeIf(e -> e.getValue() == 0);
                       });
        navActions.put(".descendingMap().values().removeIf()",
                       m -> {
                           ConcurrentMap<Integer, Integer> dm = m.descendingMap();
                           dm.values().removeIf(v -> v == 0);
                       });

        maps.forEach((mapDescription, sm) -> {
            actions.forEach((actionDescription, action) -> {
                rows.add(new Object[] {mapDescription + actionDescription, sm, action});
            });

            if (sm.get() instanceof ConcurrentNavigableMap) {
                navActions.forEach((actionDescription, action) -> {
                    rows.add(new Object[] {mapDescription + actionDescription, sm, action});
                });
            }
        });

        return rows.toArray(new Object[0][]);
    }

    ExecutorService executorService = Executors.newCachedThreadPool();

    @AfterClass
    public void after() {
        executorService.shutdown();
    }

    @Test(dataProvider = "concurrentMapViewRemoveIfActions")
    public void testMap(String desc, Supplier<ConcurrentMap<Integer, Integer>> ms, Consumer<ConcurrentMap<Integer, Integer>> action)
            throws InterruptedException {
        for (int i = 0; i < K; i++) {
            testMap(ms.get(), action);
        }
    }

    private void testMap(ConcurrentMap<Integer, Integer> map, Consumer<ConcurrentMap<Integer, Integer>> action)
            throws InterruptedException {
        // put 0's
        fillMap(map, 0);

        // To start working simultaneously
        CyclicBarrier threadStarted = new CyclicBarrier(2);

        // This task puts 1's into map
        CompletableFuture<Void> putter = CompletableFuture.runAsync(
                awaitOn(threadStarted, () -> fillMap(map, 1)),
                executorService);

        // This task performs the map action to remove all 0's from map
        CompletableFuture<Void> remover = CompletableFuture.runAsync(
                awaitOn(threadStarted, () -> action.accept(map)),
                executorService);

        // Wait for both tasks to complete
        CompletableFuture.allOf(putter, remover).join();

        assertEquals(map.size(), SIZE, "Map size incorrect");
        map.forEach((k, v) -> assertEquals(v, (Integer)1));
    }

    static void fillMap(ConcurrentMap<Integer, Integer> map, int value) {
        for (int i = 0; i < SIZE; i++) {
            map.put(i, value);
        }
    }

    static Runnable awaitOn(CyclicBarrier threadStarted, Runnable r) {
        return () -> {
            try {
                threadStarted.await();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            r.run();
        };
    }
}
