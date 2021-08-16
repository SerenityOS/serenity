/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 8005696
 * @summary Basic tests for CompletableFuture
 * @library /test/lib
 * @run main Basic
 * @run main/othervm -Djava.util.concurrent.ForkJoinPool.common.parallelism=0 Basic
 * @author Chris Hegarty
 */

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.reflect.Array;
import java.util.concurrent.Phaser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.test.lib.Utils;

public class Basic {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static void checkCompletedNormally(CompletableFuture<?> cf, Object value) {
        checkCompletedNormally(cf, value == null ? null : new Object[] { value });
    }

    static void checkCompletedNormally(CompletableFuture<?> cf, Object[] values) {
        try { equalAnyOf(cf.join(), values); } catch (Throwable x) { unexpected(x); }
        try { equalAnyOf(cf.getNow(null), values); } catch (Throwable x) { unexpected(x); }
        try { equalAnyOf(cf.get(), values); } catch (Throwable x) { unexpected(x); }
        try { equalAnyOf(cf.get(0L, SECONDS), values); } catch (Throwable x) { unexpected(x); }
        check(cf.isDone(), "Expected isDone to be true, got:" + cf);
        check(!cf.isCompletedExceptionally(), "Expected isCompletedExceptionally to return false");
        check(!cf.isCancelled(), "Expected isCancelled to be false");
        check(!cf.cancel(true), "Expected cancel to return false");
        check(cf.toString().matches(".*\\[.*Completed normally.*\\]"));
        check(cf.complete(null) == false, "Expected complete() to fail");
        check(cf.completeExceptionally(new Throwable()) == false,
              "Expected completeExceptionally() to fail");
    }

    static <T> void checkCompletedExceptionally(CompletableFuture<T> cf)
        throws Exception
    {
        checkCompletedExceptionally(cf, false);
    }

    @SuppressWarnings("unchecked")
    static <T> void checkCompletedExceptionally(CompletableFuture<T> cf, boolean cancelled)
        throws Exception
    {
        try { cf.join(); fail("Excepted exception to be thrown"); }
        catch (CompletionException x) { if (cancelled) fail(); else pass(); }
        catch (CancellationException x) { if (cancelled) pass(); else fail(); }
        try { cf.getNow(null); fail("Excepted exception to be thrown"); }
        catch (CompletionException x) { if (cancelled) fail(); else pass(); }
        catch (CancellationException x) { if (cancelled) pass(); else fail(); }
        try { cf.get(); fail("Excepted exception to be thrown");}
        catch (CancellationException x) { if (cancelled) pass(); else fail(); }
        catch (ExecutionException x) { if (cancelled) check(x.getCause() instanceof CancellationException); else pass(); }
        try { cf.get(0L, SECONDS); fail("Excepted exception to be thrown");}
        catch (CancellationException x) { if (cancelled) pass(); else fail(); }
        catch (ExecutionException x) { if (cancelled) check(x.getCause() instanceof CancellationException); else pass(); }
        check(cf.isDone(), "Expected isDone to be true, got:" + cf);
        check(cf.isCompletedExceptionally(), "Expected isCompletedExceptionally");
        check(cf.isCancelled() == cancelled, "Expected isCancelled: " + cancelled + ", got:"  + cf.isCancelled());
        check(cf.cancel(true) == cancelled, "Expected cancel: " + cancelled + ", got:"  + cf.cancel(true));
        check(cf.toString().matches(".*\\[.*Completed exceptionally.*\\]"));  // ## TODO: 'E'xceptionally
        check(cf.complete((T)new Object()) == false, "Expected complete() to fail");
        check(cf.completeExceptionally(new Throwable()) == false,
              "Expected completeExceptionally() to fail, already completed");
    }

    private static void realMain(String[] args) throws Throwable {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            test(pool);
        } finally {
            pool.shutdown();
            if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
                throw new Error();
        }
    }

    static AtomicInteger atomicInt = new AtomicInteger(0);

    private static void test(ExecutorService executor) throws Throwable {

        Thread.currentThread().setName("mainThread");

        //----------------------------------------------------------------
        // supplyAsync tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<String> cf = supplyAsync(() -> "a test string");
            checkCompletedNormally(cf, cf.join());
            cf = supplyAsync(() -> "a test string", commonPool());
            checkCompletedNormally(cf, cf.join());
            cf = supplyAsync(() -> "a test string", executor);
            checkCompletedNormally(cf, cf.join());
            cf = supplyAsync(() -> { throw new RuntimeException(); });
            checkCompletedExceptionally(cf);
            cf = supplyAsync(() -> { throw new RuntimeException(); }, commonPool());
            checkCompletedExceptionally(cf);
            cf = supplyAsync(() -> { throw new RuntimeException(); }, executor);
            checkCompletedExceptionally(cf);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // runAsync tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf = runAsync(() -> {});
            checkCompletedNormally(cf, cf.join());
            cf = runAsync(() -> {}, commonPool());
            checkCompletedNormally(cf, cf.join());
            cf = runAsync(() -> {}, executor);
            checkCompletedNormally(cf, cf.join());
            cf = runAsync(() -> { throw new RuntimeException(); });
            checkCompletedExceptionally(cf);
            cf = runAsync(() -> { throw new RuntimeException(); }, commonPool());
            checkCompletedExceptionally(cf);
            cf = runAsync(() -> { throw new RuntimeException(); }, executor);
            checkCompletedExceptionally(cf);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // explicit completion
        //----------------------------------------------------------------
        try {
            final Phaser phaser = new Phaser(1);
            final int phase = phaser.getPhase();
            CompletableFuture<Integer> cf;
            cf = supplyAsync(() -> { phaser.awaitAdvance(phase); return 1; });
            cf.complete(2);
            phaser.arrive();
            checkCompletedNormally(cf, 2);

            cf = supplyAsync(() -> { phaser.awaitAdvance(phase+1); return 1; });
            cf.completeExceptionally(new Throwable());
            phaser.arrive();
            checkCompletedExceptionally(cf);

            cf = supplyAsync(() -> { phaser.awaitAdvance(phase+2); return 1; });
            cf.cancel(true);
            phaser.arrive();
            checkCompletedExceptionally(cf, true);

            cf = supplyAsync(() -> { phaser.awaitAdvance(phase+3); return 1; });
            check(cf.getNow(2) == 2);
            phaser.arrive();
            checkCompletedNormally(cf, 1);
            check(cf.getNow(2) == 1);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenApplyXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf2;
            CompletableFuture<String> cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenApply(x -> x.equals("a test string") ? 1 : 0);
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, 1);

            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenApplyAsync(x -> x.equals("a test string") ? 1 : 0);
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, 1);

            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenApplyAsync(x -> x.equals("a test string") ? 1 : 0, executor);
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, 1);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenApply(x -> 0);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenApplyAsync(x -> 0);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenApplyAsync(x -> 0, executor);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenAcceptXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf2;
            int before = atomicInt.get();
            CompletableFuture<String> cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenAccept(x -> { if (x.equals("a test string")) { atomicInt.incrementAndGet(); return; } throw new RuntimeException(); });
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenAcceptAsync(x -> { if (x.equals("a test string")) { atomicInt.incrementAndGet(); return; } throw new RuntimeException(); });
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenAcceptAsync(x -> { if (x.equals("a test string")) { atomicInt.incrementAndGet(); return; } throw new RuntimeException(); }, executor);
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenAccept(x -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenAcceptAsync(x -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenAcceptAsync(x -> atomicInt.incrementAndGet(), executor );
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenRunXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf2;
            int before = atomicInt.get();
            CompletableFuture<String> cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenRun(() -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenRunAsync(() -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> "a test string");
            cf2 = cf1.thenRunAsync(() -> atomicInt.incrementAndGet(), executor);
            checkCompletedNormally(cf1, "a test string");
            checkCompletedNormally(cf2, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenRun(() -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenRunAsync(() -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenRunAsync(() -> atomicInt.incrementAndGet(), executor);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenCombineXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf3;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            CompletableFuture<Integer> cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenCombine(cf2, (x, y) -> x + y);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenCombineAsync(cf2, (x, y) -> x + y);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenCombineAsync(cf2, (x, y) -> x + y, executor);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, 2);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenCombine(cf2, (x, y) -> 0);
            checkCompletedExceptionally(cf1);
            checkCompletedNormally(cf2, 1);
            checkCompletedExceptionally(cf3);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.thenCombineAsync(cf2, (x, y) -> 0);
            checkCompletedNormally(cf1, 1);
            checkCompletedExceptionally(cf2);
            checkCompletedExceptionally(cf3);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.thenCombineAsync(cf2, (x, y) -> 0, executor);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            checkCompletedExceptionally(cf3);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenAcceptBothXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf3;
            int before = atomicInt.get();
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            CompletableFuture<Integer> cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenAcceptBoth(cf2, (x, y) -> { check(x + y == 2); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenAcceptBothAsync(cf2, (x, y) -> { check(x + y == 2); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenAcceptBothAsync(cf2, (x, y) -> { check(x + y == 2); atomicInt.incrementAndGet(); }, executor);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> 1);
            cf3 = cf1.thenAcceptBoth(cf2, (x, y) -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf1);
            checkCompletedNormally(cf2, 1);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.thenAcceptBothAsync(cf2, (x, y) -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf1, 1);
            checkCompletedExceptionally(cf2);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.thenAcceptBothAsync(cf2, (x, y) -> atomicInt.incrementAndGet(), executor);
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // runAfterBothXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf3;
            int before = atomicInt.get();
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            CompletableFuture<Integer> cf2 = supplyAsync(() -> 1);
            cf3 = cf1.runAfterBoth(cf2, () -> { check(cf1.isDone()); check(cf2.isDone()); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            CompletableFuture<Integer> cfa = supplyAsync(() -> 1);
            CompletableFuture<Integer> cfb = supplyAsync(() -> 1);
            cf3 = cfa.runAfterBothAsync(cfb, () -> { check(cfa.isDone()); check(cfb.isDone()); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cfa, 1);
            checkCompletedNormally(cfb, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            CompletableFuture<Integer> cfx = supplyAsync(() -> 1);
            CompletableFuture<Integer> cfy = supplyAsync(() -> 1);
            cf3 = cfy.runAfterBothAsync(cfx, () -> { check(cfx.isDone()); check(cfy.isDone()); atomicInt.incrementAndGet(); }, executor);
            checkCompletedNormally(cfx, 1);
            checkCompletedNormally(cfy, 1);
            checkCompletedNormally(cf3, null);
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            CompletableFuture<Integer> cf4 = supplyAsync(() -> { throw new RuntimeException(); });
            CompletableFuture<Integer> cf5 = supplyAsync(() -> 1);
            cf3 = cf5.runAfterBothAsync(cf4, () -> atomicInt.incrementAndGet(), executor);
            checkCompletedExceptionally(cf4);
            checkCompletedNormally(cf5, 1);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);

            before = atomicInt.get();
            cf4 = supplyAsync(() -> 1);
            cf5 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf5.runAfterBothAsync(cf4, () -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf4, 1);
            checkCompletedExceptionally(cf5);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);

            before = atomicInt.get();
            cf4 = supplyAsync(() -> { throw new RuntimeException(); });
            cf5 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf5.runAfterBoth(cf4, () -> atomicInt.incrementAndGet());
            checkCompletedExceptionally(cf4);
            checkCompletedExceptionally(cf5);
            checkCompletedExceptionally(cf3);
            check(atomicInt.get() == before);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // applyToEitherXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf3;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            CompletableFuture<Integer> cf2 = supplyAsync(() -> 2);
            cf3 = cf1.applyToEither(cf2, x -> { check(x == 1 || x == 2); return x; });
            checkCompletedNormally(cf3, new Object[] {1, 2});
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.applyToEitherAsync(cf2, x -> { check(x == 1 || x == 2); return x; });
            checkCompletedNormally(cf3, new Object[] {1, 2});
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.applyToEitherAsync(cf2, x -> { check(x == 1 || x == 2); return x; }, executor);
            checkCompletedNormally(cf3, new Object[] {1, 2});
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.applyToEither(cf2, x -> { check(x == 2); return x; });
            try { check(cf3.join() == 2); } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.applyToEitherAsync(cf2, x -> { check(x == 1); return x; });
            try { check(cf3.join() == 1); } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.applyToEitherAsync(cf2, x -> { fail(); return x; });
            checkCompletedExceptionally(cf3);
            check(cf1.isDone() || cf2.isDone());

            final Phaser cf3Done = new Phaser(2);
            cf1 = supplyAsync(() -> { cf3Done.arriveAndAwaitAdvance(); return 1; });
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.applyToEither(cf2, x -> { check(x == 2); return x; });
            checkCompletedNormally(cf3, 2);
            checkCompletedNormally(cf2, 2);
            check(!cf1.isDone());
            cf3Done.arrive();
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf3, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { cf3Done.arriveAndAwaitAdvance(); return 2; });
            cf3 = cf1.applyToEitherAsync(cf2, x -> { check(x == 1); return x; });
            checkCompletedNormally(cf3, 1);
            checkCompletedNormally(cf1, 1);
            check(!cf2.isDone());
            cf3Done.arrive();
            checkCompletedNormally(cf2, 2);
            checkCompletedNormally(cf3, 1);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // acceptEitherXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf3;
            int before = atomicInt.get();
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            CompletableFuture<Integer> cf2 = supplyAsync(() -> 2);
            cf3 = cf1.acceptEither(cf2, x -> { check(x == 1 || x == 2); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.acceptEitherAsync(cf2, x -> { check(x == 1 || x == 2); atomicInt.incrementAndGet(); });
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> 2);
            cf3 = cf2.acceptEitherAsync(cf1, x -> { check(x == 1 || x == 2); atomicInt.incrementAndGet(); }, executor);
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> 2);
            cf3 = cf2.acceptEitherAsync(cf1, x -> { check(x == 2); }, executor);
            try { check(cf3.join() == null); } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf2.acceptEitherAsync(cf1, x -> { check(x == 1); });
            try { check(cf3.join() == null); } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = supplyAsync(() -> { throw new RuntimeException(); });
            cf3 = cf2.acceptEitherAsync(cf1, x -> { fail(); });
            checkCompletedExceptionally(cf3);
            check(cf1.isDone() || cf2.isDone());

            final Phaser cf3Done = new Phaser(2);
            cf1 = supplyAsync(() -> { cf3Done.arriveAndAwaitAdvance(); return 1; });
            cf2 = supplyAsync(() -> 2);
            cf3 = cf1.acceptEither(cf2, x -> { check(x == 2); });
            checkCompletedNormally(cf3, null);
            checkCompletedNormally(cf2, 2);
            check(!cf1.isDone());
            cf3Done.arrive();
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf3, null);

            cf1 = supplyAsync(() -> 1);
            cf2 = supplyAsync(() -> { cf3Done.arriveAndAwaitAdvance(); return 2; });
            cf3 = cf1.acceptEitherAsync(cf2, x -> { check(x == 1); });
            checkCompletedNormally(cf3, null);
            checkCompletedNormally(cf1, 1);
            check(!cf2.isDone());
            cf3Done.arrive();
            checkCompletedNormally(cf2, 2);
            checkCompletedNormally(cf3, null);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // runAfterEitherXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Void> cf3;
            int before = atomicInt.get();
            CompletableFuture<Void> cf1 = runAsync(() -> {});
            CompletableFuture<Void> cf2 = runAsync(() -> {});
            cf3 = cf1.runAfterEither(cf2, () -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = runAsync(() -> {});
            cf2 = runAsync(() -> {});
            cf3 = cf1.runAfterEitherAsync(cf2, () -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = runAsync(() -> {});
            cf2 = runAsync(() -> {});
            cf3 = cf2.runAfterEitherAsync(cf1, () -> atomicInt.incrementAndGet(), executor);
            checkCompletedNormally(cf3, null);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == (before + 1));

            before = atomicInt.get();
            cf1 = runAsync(() -> { throw new RuntimeException(); });
            cf2 = runAsync(() -> {});
            cf3 = cf2.runAfterEither(cf1, () -> atomicInt.incrementAndGet());
            try {
                check(cf3.join() == null);
                check(atomicInt.get() == (before + 1));
            } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            before = atomicInt.get();
            cf1 = runAsync(() -> {});
            cf2 = runAsync(() -> { throw new RuntimeException(); });
            cf3 = cf1.runAfterEitherAsync(cf2, () -> atomicInt.incrementAndGet());
            try {
                check(cf3.join() == null);
                check(atomicInt.get() == (before + 1));
            } catch (CompletionException x) { pass(); }
            check(cf3.isDone());
            check(cf1.isDone() || cf2.isDone());

            before = atomicInt.get();
            cf1 = runAsync(() -> { throw new RuntimeException(); });
            cf2 = runAsync(() -> { throw new RuntimeException(); });
            cf3 = cf2.runAfterEitherAsync(cf1, () -> atomicInt.incrementAndGet(), executor);
            checkCompletedExceptionally(cf3);
            check(cf1.isDone() || cf2.isDone());
            check(atomicInt.get() == before);

            final Phaser cf3Done = new Phaser(2);
            before = atomicInt.get();
            cf1 = runAsync(() -> cf3Done.arriveAndAwaitAdvance());
            cf2 = runAsync(() -> {});
            cf3 = cf1.runAfterEither(cf2, () -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf3, null);
            checkCompletedNormally(cf2, null);
            check(!cf1.isDone());
            check(atomicInt.get() == (before + 1));
            cf3Done.arrive();
            checkCompletedNormally(cf1, null);
            checkCompletedNormally(cf3, null);

            before = atomicInt.get();
            cf1 = runAsync(() -> {});
            cf2 = runAsync(() -> cf3Done.arriveAndAwaitAdvance());
            cf3 = cf1.runAfterEitherAsync(cf2, () -> atomicInt.incrementAndGet());
            checkCompletedNormally(cf3, null);
            checkCompletedNormally(cf1, null);
            check(!cf2.isDone());
            check(atomicInt.get() == (before + 1));
            cf3Done.arrive();
            checkCompletedNormally(cf2, null);
            checkCompletedNormally(cf3, null);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // thenComposeXXX tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf2;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            cf2 = cf1.thenCompose(x -> { check(x == 1); return CompletableFuture.completedFuture(2); });
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = cf1.thenComposeAsync(x -> { check(x == 1); return CompletableFuture.completedFuture(2); });
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = cf1.thenComposeAsync(x -> { check(x == 1); return CompletableFuture.completedFuture(2); }, executor);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 2);

            int before = atomicInt.get();
            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenCompose(x -> { atomicInt.incrementAndGet(); return CompletableFuture.completedFuture(2); });
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> { throw new RuntimeException(); });
            cf2 = cf1.thenComposeAsync(x -> { atomicInt.incrementAndGet(); return CompletableFuture.completedFuture(2); });
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(atomicInt.get() == before);

            cf1 = supplyAsync(() -> 1);
            cf2 = cf1.thenComposeAsync(x -> { throw new RuntimeException(); }, executor);
            checkCompletedNormally(cf1, 1);
            checkCompletedExceptionally(cf2);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // anyOf tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Object> cf3;
            for (int k=0; k < 10; k++){
                CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
                CompletableFuture<Integer> cf2 = supplyAsync(() -> 2);
                cf3 = CompletableFuture.anyOf(cf1, cf2);
                checkCompletedNormally(cf3, new Object[] {1, 2});
                check(cf1.isDone() || cf2.isDone());
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // allOf tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<?> cf3;
            for (int k=0; k < 10; k++){
                CompletableFuture<Integer>[] cfs = (CompletableFuture<Integer>[])
                        Array.newInstance(CompletableFuture.class, 10);
                for (int j=0; j < 10; j++) {
                    final int v = j;
                    cfs[j] = supplyAsync(() -> v);
                }
                cf3 = CompletableFuture.allOf(cfs);
                for (int j=0; j < 10; j++)
                    checkCompletedNormally(cfs[j], j);
                checkCompletedNormally(cf3, null);
            }
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // exceptionally tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf2;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            cf2 = cf1.exceptionally(t -> { fail("function should never be called"); return 2;});
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);

            final RuntimeException t = new RuntimeException();
            cf1 = supplyAsync(() -> { throw t; });
            cf2 = cf1.exceptionally(x -> { check(x.getCause() == t); return 2;});
            checkCompletedExceptionally(cf1);
            checkCompletedNormally(cf2, 2);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // handle tests
        //----------------------------------------------------------------
        try {
            CompletableFuture<Integer> cf2;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            cf2 = cf1.handle((x,t) -> x+1);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 2);

            final RuntimeException ex = new RuntimeException();
            cf1 = supplyAsync(() -> { throw ex; });
            cf2 = cf1.handle((x,t) -> { check(t.getCause() == ex); return 2;});
            checkCompletedExceptionally(cf1);
            checkCompletedNormally(cf2, 2);

            cf1 = supplyAsync(() -> 1);
            cf2 = cf1.handleAsync((x,t) -> x+1);
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 2);

            cf1 = supplyAsync(() -> { throw ex; });
            cf2 = cf1.handleAsync((x,t) -> { check(t.getCause() == ex); return 2;});
            checkCompletedExceptionally(cf1);
            checkCompletedNormally(cf2, 2);
        } catch (Throwable t) { unexpected(t); }

        //----------------------------------------------------------------
        // whenComplete tests
        //----------------------------------------------------------------
        try {
            AtomicInteger count = new AtomicInteger();
            CompletableFuture<Integer> cf2;
            CompletableFuture<Integer> cf1 = supplyAsync(() -> 1);
            cf2 = cf1.whenComplete((x,t) -> count.getAndIncrement());
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            check(count.get() == 1, "action count should be incremented");

            final RuntimeException ex = new RuntimeException();
            cf1 = supplyAsync(() -> { throw ex; });
            cf2 = cf1.whenComplete((x,t) -> count.getAndIncrement());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(count.get() == 2, "action count should be incremented");

            cf1 = supplyAsync(() -> 1);
            cf2 = cf1.whenCompleteAsync((x,t) -> count.getAndIncrement());
            checkCompletedNormally(cf1, 1);
            checkCompletedNormally(cf2, 1);
            check(count.get() == 3, "action count should be incremented");

            cf1 = supplyAsync(() -> { throw ex; });
            cf2 = cf1.whenCompleteAsync((x,t) -> count.getAndIncrement());
            checkCompletedExceptionally(cf1);
            checkCompletedExceptionally(cf2);
            check(count.get() == 4, "action count should be incremented");

        } catch (Throwable t) { unexpected(t); }

    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void check(boolean cond, String msg) {if (cond) pass(); else fail(msg);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    static void equalAnyOf(Object x, Object[] y) {
        if (x == null && y == null) { pass(); return; }
        for (Object z : y) { if (x.equals(z)) { pass(); return; } }
        StringBuilder sb = new StringBuilder();
        for (Object o : y)
            sb.append(o).append(" ");
        fail(x + " not equal to one of [" + sb + "]");}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
