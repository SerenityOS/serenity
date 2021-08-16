/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * @test
 * @bug 8029164
 * @run testng ThenComposeAsyncTest
 * @run testng/othervm -Djava.util.concurrent.ForkJoinPool.common.parallelism=0 ThenComposeAsyncTest
 * @summary Test that CompletableFuture.thenCompose works correctly if the
 * composing task is complete before composition
 */
@Test
public class ThenComposeAsyncTest {

    public void testThenComposeAsync() throws Exception {
        // Composing CompletableFuture is complete
        CompletableFuture<String> cf1 = CompletableFuture.completedFuture("one");

        // Composing function returns a CompletableFuture executed asynchronously
        CountDownLatch cdl = new CountDownLatch(1);
        CompletableFuture<String> cf2 = cf1.thenCompose(str -> CompletableFuture.supplyAsync(() -> {
            while (true) {
                try {
                    cdl.await();
                    break;
                }
                catch (InterruptedException e) {
                }
            }
            return str + ", two";
        }));

        // Ensure returned CompletableFuture completes after call to thenCompose
        // This guarantees that any premature internal completion will be
        // detected
        cdl.countDown();

        String val = cf2.get();
        Assert.assertNotNull(val);
        Assert.assertEquals(val, "one, two");
    }
}
