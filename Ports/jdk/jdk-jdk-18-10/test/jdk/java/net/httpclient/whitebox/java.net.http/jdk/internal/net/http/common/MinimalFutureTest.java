/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertThrows;

public class MinimalFutureTest {

    @Test(dataProvider = "futures")
    public void test(CompletableFuture<Object> mf) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertNoObtrusion(mf.thenApply(MinimalFutureTest::apply));
            assertNoObtrusion(mf.thenApplyAsync(MinimalFutureTest::apply));
            assertNoObtrusion(mf.thenApplyAsync(MinimalFutureTest::apply, executor));

            assertNoObtrusion(mf.thenAccept(MinimalFutureTest::accept));
            assertNoObtrusion(mf.thenAcceptAsync(MinimalFutureTest::accept));
            assertNoObtrusion(mf.thenAcceptAsync(MinimalFutureTest::accept, executor));

            assertNoObtrusion(mf.thenRun(MinimalFutureTest::run));
            assertNoObtrusion(mf.thenRunAsync(MinimalFutureTest::run));
            assertNoObtrusion(mf.thenRunAsync(MinimalFutureTest::run, executor));

            assertNoObtrusion(mf.thenCombine(otherFuture(), MinimalFutureTest::apply));
            assertNoObtrusion(mf.thenCombineAsync(otherFuture(), MinimalFutureTest::apply));
            assertNoObtrusion(mf.thenCombineAsync(otherFuture(), MinimalFutureTest::apply, executor));

            assertNoObtrusion(mf.thenAcceptBoth(otherFuture(), MinimalFutureTest::accept));
            assertNoObtrusion(mf.thenAcceptBothAsync(otherFuture(), MinimalFutureTest::accept));
            assertNoObtrusion(mf.thenAcceptBothAsync(otherFuture(), MinimalFutureTest::accept, executor));

            assertNoObtrusion(mf.runAfterBoth(otherFuture(), MinimalFutureTest::run));
            assertNoObtrusion(mf.runAfterBothAsync(otherFuture(), MinimalFutureTest::run));
            assertNoObtrusion(mf.runAfterBothAsync(otherFuture(), MinimalFutureTest::run, executor));

            // "either" methods may return something else if otherFuture() is
            // not MinimalFuture

            assertNoObtrusion(mf.applyToEither(otherFuture(), MinimalFutureTest::apply));
            assertNoObtrusion(mf.applyToEitherAsync(otherFuture(), MinimalFutureTest::apply));
            assertNoObtrusion(mf.applyToEitherAsync(otherFuture(), MinimalFutureTest::apply, executor));

            assertNoObtrusion(mf.acceptEither(otherFuture(), MinimalFutureTest::accept));
            assertNoObtrusion(mf.acceptEitherAsync(otherFuture(), MinimalFutureTest::accept));
            assertNoObtrusion(mf.acceptEitherAsync(otherFuture(), MinimalFutureTest::accept, executor));

            assertNoObtrusion(mf.runAfterEither(otherFuture(), MinimalFutureTest::run));
            assertNoObtrusion(mf.runAfterEitherAsync(otherFuture(), MinimalFutureTest::run));
            assertNoObtrusion(mf.runAfterEitherAsync(otherFuture(), MinimalFutureTest::run, executor));

            assertNoObtrusion(mf.thenCompose(MinimalFutureTest::completionStageOf));
            assertNoObtrusion(mf.thenComposeAsync(MinimalFutureTest::completionStageOf));
            assertNoObtrusion(mf.thenComposeAsync(MinimalFutureTest::completionStageOf, executor));

            assertNoObtrusion(mf.handle(MinimalFutureTest::relay));
            assertNoObtrusion(mf.handleAsync(MinimalFutureTest::relay));
            assertNoObtrusion(mf.handleAsync(MinimalFutureTest::relay, executor));

            assertNoObtrusion(mf.whenComplete(MinimalFutureTest::accept));
            assertNoObtrusion(mf.whenCompleteAsync(MinimalFutureTest::accept));
            assertNoObtrusion(mf.whenCompleteAsync(MinimalFutureTest::accept, executor));

            assertNoObtrusion(mf.toCompletableFuture());
            assertNoObtrusion(mf.exceptionally(t -> null));

            assertNoObtrusion(mf);
            assertNoObtrusion(mf.copy());
            assertNoObtrusion(mf.newIncompleteFuture());
        } finally {
            executor.shutdownNow();
        }
    }

    private static CompletableFuture<Object> otherFuture() {
        return MinimalFuture.completedFuture(new Object());
    }

    private static Object relay(Object r, Throwable e) {
        if (e != null)
            throw new CompletionException(e);
        else
            return r;
    }

    private static CompletableFuture<?> completionStageOf(Object r) {
        return new CompletableFuture<>();
    }

    private static void accept(Object arg) {
    }

    private static void accept(Object arg1, Object arg2) {
    }

    private static void run() {
    }

    private static Object apply(Object arg) {
        return new Object();
    }

    private static Object apply(Object arg1, Object arg2) {
        return new Object();
    }


    @DataProvider(name = "futures")
    public Object[][] futures() {

        MinimalFuture<Object> mf = new MinimalFuture<>();
        mf.completeExceptionally(new Throwable());

        MinimalFuture<Object> mf1 = new MinimalFuture<>();
        mf1.complete(new Object());

        return new Object[][]{
                new Object[]{new MinimalFuture<>()},
                new Object[]{MinimalFuture.failedFuture(new Throwable())},
                new Object[]{MinimalFuture.completedFuture(new Object())},
                new Object[]{mf},
                new Object[]{mf1},
        };
    }

    private void assertNoObtrusion(CompletableFuture<?> cf) {
        assertThrows(UnsupportedOperationException.class,
                     () -> cf.obtrudeValue(null));
        assertThrows(UnsupportedOperationException.class,
                     () -> cf.obtrudeException(new RuntimeException()));
    }
}
