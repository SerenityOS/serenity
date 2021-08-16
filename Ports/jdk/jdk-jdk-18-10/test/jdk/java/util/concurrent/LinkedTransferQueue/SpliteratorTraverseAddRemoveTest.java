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
 * @run testng SpliteratorTraverseAddRemoveTest
 * @bug 8085978
 * @summary repeatedly traverse the queue using the spliterator while
 *          concurrently adding and removing an element to test that self-linked
 *          nodes are never erroneously reported on traversal
 */

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Test
public class SpliteratorTraverseAddRemoveTest {

    static Object[] of(String desc, Consumer<Queue<?>> c) {
        return new Object[]{desc, c};
    }

    static void assertIsString(Object e) {
        Assert.assertTrue(e instanceof String,
                          String.format("Object instanceof %s (actual: instanceof %s)",
                                        String.class.getName(),
                                        e.getClass().getName()));
    }

    @DataProvider()
    public static Object[][] spliteratorTraversers() {
        return new Object[][]{
                of("forEachRemaining", q -> {
                    q.spliterator().forEachRemaining(SpliteratorTraverseAddRemoveTest::assertIsString);
                }),
                of("tryAdvance", q -> {
                    Spliterator<?> s = q.spliterator();
                    while (s.tryAdvance(SpliteratorTraverseAddRemoveTest::assertIsString))
                        ;
                }),
                of("trySplit then forEachRemaining", q -> {
                    Spliterator<?> r = q.spliterator();

                    List<Spliterator<?>> ss = new ArrayList<>();
                    Spliterator<?> l;
                    while ((l = r.trySplit()) != null) {
                        ss.add(l);
                    }
                    ss.add(r);

                    ss.forEach(s -> s.forEachRemaining(SpliteratorTraverseAddRemoveTest::assertIsString));
                }),
        };
    }

    @Test(dataProvider = "spliteratorTraversers")
    public void testQueue(String desc, Consumer<Queue<String>> c)
            throws InterruptedException {
        AtomicBoolean done = new AtomicBoolean(false);
        Queue<String> msgs = new LinkedTransferQueue<>();

        CompletableFuture<Void> traversalTask = CompletableFuture.runAsync(() -> {
            while (!done.get()) {
                // Traversal will fail if self-linked nodes of
                // LinkedTransferQueue are erroneously reported
                c.accept(msgs);
            }
        });
        CompletableFuture<Void> addAndRemoveTask = CompletableFuture.runAsync(() -> {
            while (!traversalTask.isDone()) {
                msgs.add("msg");
                msgs.remove("msg");
            }
        });

        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        done.set(true);

        addAndRemoveTask.join();
        Assert.assertTrue(traversalTask.isDone());
        traversalTask.join();
    }
}
