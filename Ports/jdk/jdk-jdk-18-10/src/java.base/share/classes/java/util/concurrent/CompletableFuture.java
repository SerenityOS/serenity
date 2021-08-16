/*
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

package java.util.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Objects;

/**
 * A {@link Future} that may be explicitly completed (setting its
 * value and status), and may be used as a {@link CompletionStage},
 * supporting dependent functions and actions that trigger upon its
 * completion.
 *
 * <p>When two or more threads attempt to
 * {@link #complete complete},
 * {@link #completeExceptionally completeExceptionally}, or
 * {@link #cancel cancel}
 * a CompletableFuture, only one of them succeeds.
 *
 * <p>In addition to these and related methods for directly
 * manipulating status and results, CompletableFuture implements
 * interface {@link CompletionStage} with the following policies: <ul>
 *
 * <li>Actions supplied for dependent completions of
 * <em>non-async</em> methods may be performed by the thread that
 * completes the current CompletableFuture, or by any other caller of
 * a completion method.
 *
 * <li>All <em>async</em> methods without an explicit Executor
 * argument are performed using the {@link ForkJoinPool#commonPool()}
 * (unless it does not support a parallelism level of at least two, in
 * which case, a new Thread is created to run each task).  This may be
 * overridden for non-static methods in subclasses by defining method
 * {@link #defaultExecutor()}. To simplify monitoring, debugging,
 * and tracking, all generated asynchronous tasks are instances of the
 * marker interface {@link AsynchronousCompletionTask}.  Operations
 * with time-delays can use adapter methods defined in this class, for
 * example: {@code supplyAsync(supplier, delayedExecutor(timeout,
 * timeUnit))}.  To support methods with delays and timeouts, this
 * class maintains at most one daemon thread for triggering and
 * cancelling actions, not for running them.
 *
 * <li>All CompletionStage methods are implemented independently of
 * other public methods, so the behavior of one method is not impacted
 * by overrides of others in subclasses.
 *
 * <li>All CompletionStage methods return CompletableFutures.  To
 * restrict usages to only those methods defined in interface
 * CompletionStage, use method {@link #minimalCompletionStage}. Or to
 * ensure only that clients do not themselves modify a future, use
 * method {@link #copy}.
 * </ul>
 *
 * <p>CompletableFuture also implements {@link Future} with the following
 * policies: <ul>
 *
 * <li>Since (unlike {@link FutureTask}) this class has no direct
 * control over the computation that causes it to be completed,
 * cancellation is treated as just another form of exceptional
 * completion.  Method {@link #cancel cancel} has the same effect as
 * {@code completeExceptionally(new CancellationException())}. Method
 * {@link #isCompletedExceptionally} can be used to determine if a
 * CompletableFuture completed in any exceptional fashion.
 *
 * <li>In case of exceptional completion with a CompletionException,
 * methods {@link #get()} and {@link #get(long, TimeUnit)} throw an
 * {@link ExecutionException} with the same cause as held in the
 * corresponding CompletionException.  To simplify usage in most
 * contexts, this class also defines methods {@link #join()} and
 * {@link #getNow} that instead throw the CompletionException directly
 * in these cases.
 * </ul>
 *
 * <p>Arguments used to pass a completion result (that is, for
 * parameters of type {@code T}) for methods accepting them may be
 * null, but passing a null value for any other parameter will result
 * in a {@link NullPointerException} being thrown.
 *
 * <p>Subclasses of this class should normally override the "virtual
 * constructor" method {@link #newIncompleteFuture}, which establishes
 * the concrete type returned by CompletionStage methods. For example,
 * here is a class that substitutes a different default Executor and
 * disables the {@code obtrude} methods:
 *
 * <pre> {@code
 * class MyCompletableFuture<T> extends CompletableFuture<T> {
 *   static final Executor myExecutor = ...;
 *   public MyCompletableFuture() { }
 *   public <U> CompletableFuture<U> newIncompleteFuture() {
 *     return new MyCompletableFuture<U>(); }
 *   public Executor defaultExecutor() {
 *     return myExecutor; }
 *   public void obtrudeValue(T value) {
 *     throw new UnsupportedOperationException(); }
 *   public void obtrudeException(Throwable ex) {
 *     throw new UnsupportedOperationException(); }
 * }}</pre>
 *
 * @author Doug Lea
 * @param <T> The result type returned by this future's {@code join}
 * and {@code get} methods
 * @since 1.8
 */
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    /*
     * Overview:
     *
     * A CompletableFuture may have dependent completion actions,
     * collected in a linked stack. It atomically completes by CASing
     * a result field, and then pops off and runs those actions. This
     * applies across normal vs exceptional outcomes, sync vs async
     * actions, binary triggers, and various forms of completions.
     *
     * Non-nullness of volatile field "result" indicates done.  It may
     * be set directly if known to be thread-confined, else via CAS.
     * An AltResult is used to box null as a result, as well as to
     * hold exceptions.  Using a single field makes completion simple
     * to detect and trigger.  Result encoding and decoding is
     * straightforward but tedious and adds to the sprawl of trapping
     * and associating exceptions with targets.  Minor simplifications
     * rely on (static) NIL (to box null results) being the only
     * AltResult with a null exception field, so we don't usually need
     * explicit comparisons.  Even though some of the generics casts
     * are unchecked (see SuppressWarnings annotations), they are
     * placed to be appropriate even if checked.
     *
     * Dependent actions are represented by Completion objects linked
     * as Treiber stacks headed by field "stack". There are Completion
     * classes for each kind of action, grouped into:
     * - single-input (UniCompletion),
     * - two-input (BiCompletion),
     * - projected (BiCompletions using exactly one of two inputs),
     * - shared (CoCompletion, used by the second of two sources),
     * - zero-input source actions,
     * - Signallers that unblock waiters.
     * Class Completion extends ForkJoinTask to enable async execution
     * (adding no space overhead because we exploit its "tag" methods
     * to maintain claims). It is also declared as Runnable to allow
     * usage with arbitrary executors.
     *
     * Support for each kind of CompletionStage relies on a separate
     * class, along with two CompletableFuture methods:
     *
     * * A Completion class with name X corresponding to function,
     *   prefaced with "Uni", "Bi", or "Or". Each class contains
     *   fields for source(s), actions, and dependent. They are
     *   boringly similar, differing from others only with respect to
     *   underlying functional forms. We do this so that users don't
     *   encounter layers of adapters in common usages.
     *
     * * Boolean CompletableFuture method x(...) (for example
     *   biApply) takes all of the arguments needed to check that an
     *   action is triggerable, and then either runs the action or
     *   arranges its async execution by executing its Completion
     *   argument, if present. The method returns true if known to be
     *   complete.
     *
     * * Completion method tryFire(int mode) invokes the associated x
     *   method with its held arguments, and on success cleans up.
     *   The mode argument allows tryFire to be called twice (SYNC,
     *   then ASYNC); the first to screen and trap exceptions while
     *   arranging to execute, and the second when called from a task.
     *   (A few classes are not used async so take slightly different
     *   forms.)  The claim() callback suppresses function invocation
     *   if already claimed by another thread.
     *
     * * Some classes (for example UniApply) have separate handling
     *   code for when known to be thread-confined ("now" methods) and
     *   for when shared (in tryFire), for efficiency.
     *
     * * CompletableFuture method xStage(...) is called from a public
     *   stage method of CompletableFuture f. It screens user
     *   arguments and invokes and/or creates the stage object.  If
     *   not async and already triggerable, the action is run
     *   immediately.  Otherwise a Completion c is created, and
     *   submitted to the executor if triggerable, or pushed onto f's
     *   stack if not.  Completion actions are started via c.tryFire.
     *   We recheck after pushing to a source future's stack to cover
     *   possible races if the source completes while pushing.
     *   Classes with two inputs (for example BiApply) deal with races
     *   across both while pushing actions.  The second completion is
     *   a CoCompletion pointing to the first, shared so that at most
     *   one performs the action.  The multiple-arity methods allOf
     *   does this pairwise to form trees of completions.  Method
     *   anyOf is handled differently from allOf because completion of
     *   any source should trigger a cleanStack of other sources.
     *   Each AnyOf completion can reach others via a shared array.
     *
     * Note that the generic type parameters of methods vary according
     * to whether "this" is a source, dependent, or completion.
     *
     * Method postComplete is called upon completion unless the target
     * is guaranteed not to be observable (i.e., not yet returned or
     * linked). Multiple threads can call postComplete, which
     * atomically pops each dependent action, and tries to trigger it
     * via method tryFire, in NESTED mode.  Triggering can propagate
     * recursively, so NESTED mode returns its completed dependent (if
     * one exists) for further processing by its caller (see method
     * postFire).
     *
     * Blocking methods get() and join() rely on Signaller Completions
     * that wake up waiting threads.  The mechanics are similar to
     * Treiber stack wait-nodes used in FutureTask, Phaser, and
     * SynchronousQueue. See their internal documentation for
     * algorithmic details.
     *
     * Without precautions, CompletableFutures would be prone to
     * garbage accumulation as chains of Completions build up, each
     * pointing back to its sources. So we null out fields as soon as
     * possible.  The screening checks needed anyway harmlessly ignore
     * null arguments that may have been obtained during races with
     * threads nulling out fields.  We also try to unlink non-isLive
     * (fired or cancelled) Completions from stacks that might
     * otherwise never be popped: Method cleanStack always unlinks non
     * isLive completions from the head of stack; others may
     * occasionally remain if racing with other cancellations or
     * removals.
     *
     * Completion fields need not be declared as final or volatile
     * because they are only visible to other threads upon safe
     * publication.
     */

    volatile Object result;       // Either the result or boxed AltResult
    volatile Completion stack;    // Top of Treiber stack of dependent actions

    final boolean internalComplete(Object r) { // CAS from null to r
        return RESULT.compareAndSet(this, null, r);
    }

    /** Returns true if successfully pushed c onto stack. */
    final boolean tryPushStack(Completion c) {
        Completion h = stack;
        NEXT.set(c, h);         // CAS piggyback
        return STACK.compareAndSet(this, h, c);
    }

    /** Unconditionally pushes c onto stack, retrying if necessary. */
    final void pushStack(Completion c) {
        do {} while (!tryPushStack(c));
    }

    /* ------------- Encoding and decoding outcomes -------------- */

    static final class AltResult { // See above
        final Throwable ex;        // null only for NIL
        AltResult(Throwable x) { this.ex = x; }
    }

    /** The encoding of the null value. */
    static final AltResult NIL = new AltResult(null);

    /** Completes with the null value, unless already completed. */
    final boolean completeNull() {
        return RESULT.compareAndSet(this, null, NIL);
    }

    /** Returns the encoding of the given non-exceptional value. */
    final Object encodeValue(T t) {
        return (t == null) ? NIL : t;
    }

    /** Completes with a non-exceptional result, unless already completed. */
    final boolean completeValue(T t) {
        return RESULT.compareAndSet(this, null, (t == null) ? NIL : t);
    }

    /**
     * Returns the encoding of the given (non-null) exception as a
     * wrapped CompletionException unless it is one already.
     */
    static AltResult encodeThrowable(Throwable x) {
        return new AltResult((x instanceof CompletionException) ? x :
                             new CompletionException(x));
    }

    /** Completes with an exceptional result, unless already completed. */
    final boolean completeThrowable(Throwable x) {
        return RESULT.compareAndSet(this, null, encodeThrowable(x));
    }

    /**
     * Returns the encoding of the given (non-null) exception as a
     * wrapped CompletionException unless it is one already.  May
     * return the given Object r (which must have been the result of a
     * source future) if it is equivalent, i.e. if this is a simple
     * relay of an existing CompletionException.
     */
    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException))
            x = new CompletionException(x);
        else if (r instanceof AltResult && x == ((AltResult)r).ex)
            return r;
        return new AltResult(x);
    }

    /**
     * Completes with the given (non-null) exceptional result as a
     * wrapped CompletionException unless it is one already, unless
     * already completed.  May complete with the given Object r
     * (which must have been the result of a source future) if it is
     * equivalent, i.e. if this is a simple propagation of an
     * existing CompletionException.
     */
    final boolean completeThrowable(Throwable x, Object r) {
        return RESULT.compareAndSet(this, null, encodeThrowable(x, r));
    }

    /**
     * Returns the encoding of the given arguments: if the exception
     * is non-null, encodes as AltResult.  Otherwise uses the given
     * value, boxed as NIL if null.
     */
    Object encodeOutcome(T t, Throwable x) {
        return (x == null) ? (t == null) ? NIL : t : encodeThrowable(x);
    }

    /**
     * Returns the encoding of a copied outcome; if exceptional,
     * rewraps as a CompletionException, else returns argument.
     */
    static Object encodeRelay(Object r) {
        Throwable x;
        if (r instanceof AltResult
            && (x = ((AltResult)r).ex) != null
            && !(x instanceof CompletionException))
            r = new AltResult(new CompletionException(x));
        return r;
    }

    /**
     * Completes with r or a copy of r, unless already completed.
     * If exceptional, r is first coerced to a CompletionException.
     */
    final boolean completeRelay(Object r) {
        return RESULT.compareAndSet(this, null, encodeRelay(r));
    }

    /**
     * Reports result using Future.get conventions.
     */
    private static Object reportGet(Object r)
        throws InterruptedException, ExecutionException {
        if (r == null) // by convention below, null means interrupted
            throw new InterruptedException();
        if (r instanceof AltResult) {
            Throwable x, cause;
            if ((x = ((AltResult)r).ex) == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException)x;
            if ((x instanceof CompletionException) &&
                (cause = x.getCause()) != null)
                x = cause;
            throw new ExecutionException(x);
        }
        return r;
    }

    /**
     * Decodes outcome to return result or throw unchecked exception.
     */
    private static Object reportJoin(Object r) {
        if (r instanceof AltResult) {
            Throwable x;
            if ((x = ((AltResult)r).ex) == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException)x;
            if (x instanceof CompletionException)
                throw (CompletionException)x;
            throw new CompletionException(x);
        }
        return r;
    }

    /* ------------- Async task preliminaries -------------- */

    /**
     * A marker interface identifying asynchronous tasks produced by
     * {@code async} methods. This may be useful for monitoring,
     * debugging, and tracking asynchronous activities.
     *
     * @since 1.8
     */
    public static interface AsynchronousCompletionTask {
    }

    private static final boolean USE_COMMON_POOL =
        (ForkJoinPool.getCommonPoolParallelism() > 1);

    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot
     * support parallelism.
     */
    private static final Executor ASYNC_POOL = USE_COMMON_POOL ?
        ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();

    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            Objects.requireNonNull(r);
            new Thread(r).start();
        }
    }

    /**
     * Null-checks user executor argument, and translates uses of
     * commonPool to ASYNC_POOL in case parallelism disabled.
     */
    static Executor screenExecutor(Executor e) {
        if (!USE_COMMON_POOL && e == ForkJoinPool.commonPool())
            return ASYNC_POOL;
        if (e == null) throw new NullPointerException();
        return e;
    }

    // Modes for Completion.tryFire. Signedness matters.
    static final int SYNC   =  0;
    static final int ASYNC  =  1;
    static final int NESTED = -1;

    /* ------------- Base Completion classes and operations -------------- */

    @SuppressWarnings("serial")
    abstract static class Completion extends ForkJoinTask<Void>
        implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;      // Treiber stack link

        /**
         * Performs completion action if triggered, returning a
         * dependent that may need propagation, if one exists.
         *
         * @param mode SYNC, ASYNC, or NESTED
         */
        abstract CompletableFuture<?> tryFire(int mode);

        /** Returns true if possibly still triggerable. Used by cleanStack. */
        abstract boolean isLive();

        public final void run()                { tryFire(ASYNC); }
        public final boolean exec()            { tryFire(ASYNC); return false; }
        public final Void getRawResult()       { return null; }
        public final void setRawResult(Void v) {}
    }

    /**
     * Pops and tries to trigger all reachable dependents.  Call only
     * when known to be done.
     */
    final void postComplete() {
        /*
         * On each step, variable f holds current dependents to pop
         * and run.  It is extended along only one path at a time,
         * pushing others to avoid unbounded recursion.
         */
        CompletableFuture<?> f = this; Completion h;
        while ((h = f.stack) != null ||
               (f != this && (h = (f = this).stack) != null)) {
            CompletableFuture<?> d; Completion t;
            if (STACK.compareAndSet(f, h, t = h.next)) {
                if (t != null) {
                    if (f != this) {
                        pushStack(h);
                        continue;
                    }
                    NEXT.compareAndSet(h, t, null); // try to detach
                }
                f = (d = h.tryFire(NESTED)) == null ? this : d;
            }
        }
    }

    /** Traverses stack and unlinks one or more dead Completions, if found. */
    final void cleanStack() {
        Completion p = stack;
        // ensure head of stack live
        for (boolean unlinked = false;;) {
            if (p == null)
                return;
            else if (p.isLive()) {
                if (unlinked)
                    return;
                else
                    break;
            }
            else if (STACK.weakCompareAndSet(this, p, (p = p.next)))
                unlinked = true;
            else
                p = stack;
        }
        // try to unlink first non-live
        for (Completion q = p.next; q != null;) {
            Completion s = q.next;
            if (q.isLive()) {
                p = q;
                q = s;
            } else if (NEXT.weakCompareAndSet(p, q, s))
                break;
            else
                q = p.next;
        }
    }

    /* ------------- One-input Completions -------------- */

    /** A Completion with a source, dependent, and executor. */
    @SuppressWarnings("serial")
    abstract static class UniCompletion<T,V> extends Completion {
        Executor executor;                 // executor to use (null if none)
        CompletableFuture<V> dep;          // the dependent to complete
        CompletableFuture<T> src;          // source for action

        UniCompletion(Executor executor, CompletableFuture<V> dep,
                      CompletableFuture<T> src) {
            this.executor = executor; this.dep = dep; this.src = src;
        }

        /**
         * Returns true if action can be run. Call only when known to
         * be triggerable. Uses FJ tag bit to ensure that only one
         * thread claims ownership.  If async, starts as task -- a
         * later call to tryFire will run action.
         */
        final boolean claim() {
            Executor e = executor;
            if (compareAndSetForkJoinTaskTag((short)0, (short)1)) {
                if (e == null)
                    return true;
                executor = null; // disable
                e.execute(this);
            }
            return false;
        }

        final boolean isLive() { return dep != null; }
    }

    /**
     * Pushes the given completion unless it completes while trying.
     * Caller should first check that result is null.
     */
    final void unipush(Completion c) {
        if (c != null) {
            while (!tryPushStack(c)) {
                if (result != null) {
                    NEXT.set(c, null);
                    break;
                }
            }
            if (result != null)
                c.tryFire(SYNC);
        }
    }

    /**
     * Post-processing by dependent after successful UniCompletion tryFire.
     * Tries to clean stack of source a, and then either runs postComplete
     * or returns this to caller, depending on mode.
     */
    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (a != null && a.stack != null) {
            Object r;
            if ((r = a.result) == null)
                a.cleanStack();
            if (mode >= 0 && (r != null || a.result != null))
                a.postComplete();
        }
        if (result != null && stack != null) {
            if (mode < 0)
                return this;
            else
                postComplete();
        }
        return null;
    }

    @SuppressWarnings("serial")
    static final class UniApply<T,V> extends UniCompletion<T,V> {
        Function<? super T,? extends V> fn;
        UniApply(Executor executor, CompletableFuture<V> dep,
                 CompletableFuture<T> src,
                 Function<? super T,? extends V> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d; CompletableFuture<T> a;
            Object r; Throwable x; Function<? super T,? extends V> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null)
                return null;
            tryComplete: if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult)r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    else {
                        @SuppressWarnings("unchecked") T t = (T) r;
                        d.completeValue(f.apply(t));
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    private <V> CompletableFuture<V> uniApplyStage(
        Executor e, Function<? super T,? extends V> f) {
        if (f == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null)
            return uniApplyNow(r, e, f);
        CompletableFuture<V> d = newIncompleteFuture();
        unipush(new UniApply<T,V>(e, d, this, f));
        return d;
    }

    private <V> CompletableFuture<V> uniApplyNow(
        Object r, Executor e, Function<? super T,? extends V> f) {
        Throwable x;
        CompletableFuture<V> d = newIncompleteFuture();
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                d.result = encodeThrowable(x, r);
                return d;
            }
            r = null;
        }
        try {
            if (e != null) {
                e.execute(new UniApply<T,V>(null, d, this, f));
            } else {
                @SuppressWarnings("unchecked") T t = (T) r;
                d.result = d.encodeValue(f.apply(t));
            }
        } catch (Throwable ex) {
            d.result = encodeThrowable(ex);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniAccept<T> extends UniCompletion<T,Void> {
        Consumer<? super T> fn;
        UniAccept(Executor executor, CompletableFuture<Void> dep,
                  CompletableFuture<T> src, Consumer<? super T> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d; CompletableFuture<T> a;
            Object r; Throwable x; Consumer<? super T> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null)
                return null;
            tryComplete: if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult)r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    else {
                        @SuppressWarnings("unchecked") T t = (T) r;
                        f.accept(t);
                        d.completeNull();
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    private CompletableFuture<Void> uniAcceptStage(Executor e,
                                                   Consumer<? super T> f) {
        if (f == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null)
            return uniAcceptNow(r, e, f);
        CompletableFuture<Void> d = newIncompleteFuture();
        unipush(new UniAccept<T>(e, d, this, f));
        return d;
    }

    private CompletableFuture<Void> uniAcceptNow(
        Object r, Executor e, Consumer<? super T> f) {
        Throwable x;
        CompletableFuture<Void> d = newIncompleteFuture();
        if (r instanceof AltResult) {
            if ((x = ((AltResult)r).ex) != null) {
                d.result = encodeThrowable(x, r);
                return d;
            }
            r = null;
        }
        try {
            if (e != null) {
                e.execute(new UniAccept<T>(null, d, this, f));
            } else {
                @SuppressWarnings("unchecked") T t = (T) r;
                f.accept(t);
                d.result = NIL;
            }
        } catch (Throwable ex) {
            d.result = encodeThrowable(ex);
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniRun<T> extends UniCompletion<T,Void> {
        Runnable fn;
        UniRun(Executor executor, CompletableFuture<Void> dep,
               CompletableFuture<T> src, Runnable fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d; CompletableFuture<T> a;
            Object r; Throwable x; Runnable f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null)
                return null;
            if (d.result == null) {
                if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                    d.completeThrowable(x, r);
                else
                    try {
                        if (mode <= 0 && !claim())
                            return null;
                        else {
                            f.run();
                            d.completeNull();
                        }
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
            }
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        if (f == null) throw new NullPointerException();
        Object r;
        if ((r = result) != null)
            return uniRunNow(r, e, f);
        CompletableFuture<Void> d = newIncompleteFuture();
        unipush(new UniRun<T>(e, d, this, f));
        return d;
    }

    private CompletableFuture<Void> uniRunNow(Object r, Executor e, Runnable f) {
        Throwable x;
        CompletableFuture<Void> d = newIncompleteFuture();
        if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
            d.result = encodeThrowable(x, r);
        else
            try {
                if (e != null) {
                    e.execute(new UniRun<T>(null, d, this, f));
                } else {
                    f.run();
                    d.result = NIL;
                }
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniWhenComplete<T> extends UniCompletion<T,T> {
        BiConsumer<? super T, ? super Throwable> fn;
        UniWhenComplete(Executor executor, CompletableFuture<T> dep,
                        CompletableFuture<T> src,
                        BiConsumer<? super T, ? super Throwable> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d; CompletableFuture<T> a;
            Object r; BiConsumer<? super T, ? super Throwable> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniWhenComplete(r, f, mode > 0 ? null : this))
                return null;
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniWhenComplete(Object r,
                                  BiConsumer<? super T,? super Throwable> f,
                                  UniWhenComplete<T> c) {
        T t; Throwable x = null;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    x = ((AltResult)r).ex;
                    t = null;
                } else {
                    @SuppressWarnings("unchecked") T tr = (T) r;
                    t = tr;
                }
                f.accept(t, x);
                if (x == null) {
                    internalComplete(r);
                    return true;
                }
            } catch (Throwable ex) {
                if (x == null)
                    x = ex;
                else if (x != ex)
                    x.addSuppressed(ex);
            }
            completeThrowable(x, r);
        }
        return true;
    }

    private CompletableFuture<T> uniWhenCompleteStage(
        Executor e, BiConsumer<? super T, ? super Throwable> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;
        if ((r = result) == null)
            unipush(new UniWhenComplete<T>(e, d, this, f));
        else if (e == null)
            d.uniWhenComplete(r, f, null);
        else {
            try {
                e.execute(new UniWhenComplete<T>(null, d, this, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniHandle<T,V> extends UniCompletion<T,V> {
        BiFunction<? super T, Throwable, ? extends V> fn;
        UniHandle(Executor executor, CompletableFuture<V> dep,
                  CompletableFuture<T> src,
                  BiFunction<? super T, Throwable, ? extends V> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d; CompletableFuture<T> a;
            Object r; BiFunction<? super T, Throwable, ? extends V> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniHandle(r, f, mode > 0 ? null : this))
                return null;
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final <S> boolean uniHandle(Object r,
                                BiFunction<? super S, Throwable, ? extends T> f,
                                UniHandle<S,T> c) {
        S s; Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult) {
                    x = ((AltResult)r).ex;
                    s = null;
                } else {
                    x = null;
                    @SuppressWarnings("unchecked") S ss = (S) r;
                    s = ss;
                }
                completeValue(f.apply(s, x));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <V> CompletableFuture<V> uniHandleStage(
        Executor e, BiFunction<? super T, Throwable, ? extends V> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<V> d = newIncompleteFuture();
        Object r;
        if ((r = result) == null)
            unipush(new UniHandle<T,V>(e, d, this, f));
        else if (e == null)
            d.uniHandle(r, f, null);
        else {
            try {
                e.execute(new UniHandle<T,V>(null, d, this, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniExceptionally<T> extends UniCompletion<T,T> {
        Function<? super Throwable, ? extends T> fn;
        UniExceptionally(Executor executor,
                         CompletableFuture<T> dep, CompletableFuture<T> src,
                         Function<? super Throwable, ? extends T> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d; CompletableFuture<T> a;
            Object r; Function<? super Throwable, ? extends T> f;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.uniExceptionally(r, f, mode > 0 ? null : this))
                return null;
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    final boolean uniExceptionally(Object r,
                                   Function<? super Throwable, ? extends T> f,
                                   UniExceptionally<T> c) {
        Throwable x;
        if (result == null) {
            try {
                if (c != null && !c.claim())
                    return false;
                if (r instanceof AltResult && (x = ((AltResult)r).ex) != null)
                    completeValue(f.apply(x));
                else
                    internalComplete(r);
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private CompletableFuture<T> uniExceptionallyStage(
        Executor e, Function<Throwable, ? extends T> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<T> d = newIncompleteFuture();
        Object r;
        if ((r = result) == null)
            unipush(new UniExceptionally<T>(e, d, this, f));
        else if (e == null)
            d.uniExceptionally(r, f, null);
        else {
            try {
                e.execute(new UniExceptionally<T>(null, d, this, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniComposeExceptionally<T> extends UniCompletion<T,T> {
        Function<Throwable, ? extends CompletionStage<T>> fn;
        UniComposeExceptionally(Executor executor, CompletableFuture<T> dep,
                                CompletableFuture<T> src,
                                Function<Throwable, ? extends CompletionStage<T>> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> d; CompletableFuture<T> a;
            Function<Throwable, ? extends CompletionStage<T>> f;
            Object r; Throwable x;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null)
                return null;
            if (d.result == null) {
                if ((r instanceof AltResult) &&
                    (x = ((AltResult)r).ex) != null) {
                    try {
                        if (mode <= 0 && !claim())
                            return null;
                        CompletableFuture<T> g = f.apply(x).toCompletableFuture();
                        if ((r = g.result) != null)
                            d.completeRelay(r);
                        else {
                            g.unipush(new UniRelay<T,T>(d, g));
                            if (d.result == null)
                                return null;
                        }
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                else
                    d.internalComplete(r);
            }
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    private CompletableFuture<T> uniComposeExceptionallyStage(
        Executor e, Function<Throwable, ? extends CompletionStage<T>> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<T> d = newIncompleteFuture();
        Object r, s; Throwable x;
        if ((r = result) == null)
            unipush(new UniComposeExceptionally<T>(e, d, this, f));
        else if (!(r instanceof AltResult) || (x = ((AltResult)r).ex) == null)
            d.internalComplete(r);
        else
            try {
                if (e != null)
                    e.execute(new UniComposeExceptionally<T>(null, d, this, f));
                else {
                    CompletableFuture<T> g = f.apply(x).toCompletableFuture();
                    if ((s = g.result) != null)
                        d.result = encodeRelay(s);
                    else
                        g.unipush(new UniRelay<T,T>(d, g));
                }
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniRelay<U, T extends U> extends UniCompletion<T,U> {
        UniRelay(CompletableFuture<U> dep, CompletableFuture<T> src) {
            super(null, dep, src);
        }
        final CompletableFuture<U> tryFire(int mode) {
            CompletableFuture<U> d; CompletableFuture<T> a; Object r;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null)
                return null;
            if (d.result == null)
                d.completeRelay(r);
            src = null; dep = null;
            return d.postFire(a, mode);
        }
    }

    private static <U, T extends U> CompletableFuture<U> uniCopyStage(
        CompletableFuture<T> src) {
        Object r;
        CompletableFuture<U> d = src.newIncompleteFuture();
        if ((r = src.result) != null)
            d.result = encodeRelay(r);
        else
            src.unipush(new UniRelay<U,T>(d, src));
        return d;
    }

    private MinimalStage<T> uniAsMinimalStage() {
        Object r;
        if ((r = result) != null)
            return new MinimalStage<T>(encodeRelay(r));
        MinimalStage<T> d = new MinimalStage<T>();
        unipush(new UniRelay<T,T>(d, this));
        return d;
    }

    @SuppressWarnings("serial")
    static final class UniCompose<T,V> extends UniCompletion<T,V> {
        Function<? super T, ? extends CompletionStage<V>> fn;
        UniCompose(Executor executor, CompletableFuture<V> dep,
                   CompletableFuture<T> src,
                   Function<? super T, ? extends CompletionStage<V>> fn) {
            super(executor, dep, src); this.fn = fn;
        }
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d; CompletableFuture<T> a;
            Function<? super T, ? extends CompletionStage<V>> f;
            Object r; Throwable x;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (f = fn) == null)
                return null;
            tryComplete: if (d.result == null) {
                if (r instanceof AltResult) {
                    if ((x = ((AltResult)r).ex) != null) {
                        d.completeThrowable(x, r);
                        break tryComplete;
                    }
                    r = null;
                }
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    @SuppressWarnings("unchecked") T t = (T) r;
                    CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                    if ((r = g.result) != null)
                        d.completeRelay(r);
                    else {
                        g.unipush(new UniRelay<V,V>(d, g));
                        if (d.result == null)
                            return null;
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; dep = null; fn = null;
            return d.postFire(a, mode);
        }
    }

    private <V> CompletableFuture<V> uniComposeStage(
        Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<V> d = newIncompleteFuture();
        Object r, s; Throwable x;
        if ((r = result) == null)
            unipush(new UniCompose<T,V>(e, d, this, f));
        else {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    d.result = encodeThrowable(x, r);
                    return d;
                }
                r = null;
            }
            try {
                if (e != null)
                    e.execute(new UniCompose<T,V>(null, d, this, f));
                else {
                    @SuppressWarnings("unchecked") T t = (T) r;
                    CompletableFuture<V> g = f.apply(t).toCompletableFuture();
                    if ((s = g.result) != null)
                        d.result = encodeRelay(s);
                    else
                        g.unipush(new UniRelay<V,V>(d, g));
                }
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        }
        return d;
    }

    /* ------------- Two-input Completions -------------- */

    /** A Completion for an action with two sources */
    @SuppressWarnings("serial")
    abstract static class BiCompletion<T,U,V> extends UniCompletion<T,V> {
        CompletableFuture<U> snd; // second source for action
        BiCompletion(Executor executor, CompletableFuture<V> dep,
                     CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(executor, dep, src); this.snd = snd;
        }
    }

    /** A Completion delegating to a BiCompletion */
    @SuppressWarnings("serial")
    static final class CoCompletion extends Completion {
        BiCompletion<?,?,?> base;
        CoCompletion(BiCompletion<?,?,?> base) { this.base = base; }
        final CompletableFuture<?> tryFire(int mode) {
            BiCompletion<?,?,?> c; CompletableFuture<?> d;
            if ((c = base) == null || (d = c.tryFire(mode)) == null)
                return null;
            base = null; // detach
            return d;
        }
        final boolean isLive() {
            BiCompletion<?,?,?> c;
            return (c = base) != null
                // && c.isLive()
                && c.dep != null;
        }
    }

    /**
     * Pushes completion to this and b unless both done.
     * Caller should first check that either result or b.result is null.
     */
    final void bipush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
        if (c != null) {
            while (result == null) {
                if (tryPushStack(c)) {
                    if (b.result == null)
                        b.unipush(new CoCompletion(c));
                    else if (result != null)
                        c.tryFire(SYNC);
                    return;
                }
            }
            b.unipush(c);
        }
    }

    /** Post-processing after successful BiCompletion tryFire. */
    final CompletableFuture<T> postFire(CompletableFuture<?> a,
                                        CompletableFuture<?> b, int mode) {
        if (b != null && b.stack != null) { // clean second source
            Object r;
            if ((r = b.result) == null)
                b.cleanStack();
            if (mode >= 0 && (r != null || b.result != null))
                b.postComplete();
        }
        return postFire(a, mode);
    }

    @SuppressWarnings("serial")
    static final class BiApply<T,U,V> extends BiCompletion<T,U,V> {
        BiFunction<? super T,? super U,? extends V> fn;
        BiApply(Executor executor, CompletableFuture<V> dep,
                CompletableFuture<T> src, CompletableFuture<U> snd,
                BiFunction<? super T,? super U,? extends V> fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s; BiFunction<? super T,? super U,? extends V> f;
            if (   (a = src) == null || (r = a.result) == null
                || (b = snd) == null || (s = b.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.biApply(r, s, f, mode > 0 ? null : this))
                return null;
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S> boolean biApply(Object r, Object s,
                                BiFunction<? super R,? super S,? extends T> f,
                                BiApply<R,S,T> c) {
        Throwable x;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                if ((x = ((AltResult)s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") R rr = (R) r;
                @SuppressWarnings("unchecked") S ss = (S) s;
                completeValue(f.apply(rr, ss));
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U,V> CompletableFuture<V> biApplyStage(
        Executor e, CompletionStage<U> o,
        BiFunction<? super T,? super U,? extends V> f) {
        CompletableFuture<U> b; Object r, s;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<V> d = newIncompleteFuture();
        if ((r = result) == null || (s = b.result) == null)
            bipush(b, new BiApply<T,U,V>(e, d, this, b, f));
        else if (e == null)
            d.biApply(r, s, f, null);
        else
            try {
                e.execute(new BiApply<T,U,V>(null, d, this, b, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiAccept<T,U> extends BiCompletion<T,U,Void> {
        BiConsumer<? super T,? super U> fn;
        BiAccept(Executor executor, CompletableFuture<Void> dep,
                 CompletableFuture<T> src, CompletableFuture<U> snd,
                 BiConsumer<? super T,? super U> fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s; BiConsumer<? super T,? super U> f;
            if (   (a = src) == null || (r = a.result) == null
                || (b = snd) == null || (s = b.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.biAccept(r, s, f, mode > 0 ? null : this))
                return null;
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final <R,S> boolean biAccept(Object r, Object s,
                                 BiConsumer<? super R,? super S> f,
                                 BiAccept<R,S> c) {
        Throwable x;
        tryComplete: if (result == null) {
            if (r instanceof AltResult) {
                if ((x = ((AltResult)r).ex) != null) {
                    completeThrowable(x, r);
                    break tryComplete;
                }
                r = null;
            }
            if (s instanceof AltResult) {
                if ((x = ((AltResult)s).ex) != null) {
                    completeThrowable(x, s);
                    break tryComplete;
                }
                s = null;
            }
            try {
                if (c != null && !c.claim())
                    return false;
                @SuppressWarnings("unchecked") R rr = (R) r;
                @SuppressWarnings("unchecked") S ss = (S) s;
                f.accept(rr, ss);
                completeNull();
            } catch (Throwable ex) {
                completeThrowable(ex);
            }
        }
        return true;
    }

    private <U> CompletableFuture<Void> biAcceptStage(
        Executor e, CompletionStage<U> o,
        BiConsumer<? super T,? super U> f) {
        CompletableFuture<U> b; Object r, s;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = newIncompleteFuture();
        if ((r = result) == null || (s = b.result) == null)
            bipush(b, new BiAccept<T,U>(e, d, this, b, f));
        else if (e == null)
            d.biAccept(r, s, f, null);
        else
            try {
                e.execute(new BiAccept<T,U>(null, d, this, b, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiRun<T,U> extends BiCompletion<T,U,Void> {
        Runnable fn;
        BiRun(Executor executor, CompletableFuture<Void> dep,
              CompletableFuture<T> src, CompletableFuture<U> snd,
              Runnable fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s; Runnable f;
            if (   (a = src) == null || (r = a.result) == null
                || (b = snd) == null || (s = b.result) == null
                || (d = dep) == null || (f = fn) == null
                || !d.biRun(r, s, f, mode > 0 ? null : this))
                return null;
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    final boolean biRun(Object r, Object s, Runnable f, BiRun<?,?> c) {
        Throwable x; Object z;
        if (result == null) {
            if ((r instanceof AltResult
                 && (x = ((AltResult)(z = r)).ex) != null) ||
                (s instanceof AltResult
                 && (x = ((AltResult)(z = s)).ex) != null))
                completeThrowable(x, z);
            else
                try {
                    if (c != null && !c.claim())
                        return false;
                    f.run();
                    completeNull();
                } catch (Throwable ex) {
                    completeThrowable(ex);
                }
        }
        return true;
    }

    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> o,
                                               Runnable f) {
        CompletableFuture<?> b; Object r, s;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();
        CompletableFuture<Void> d = newIncompleteFuture();
        if ((r = result) == null || (s = b.result) == null)
            bipush(b, new BiRun<>(e, d, this, b, f));
        else if (e == null)
            d.biRun(r, s, f, null);
        else
            try {
                e.execute(new BiRun<>(null, d, this, b, f));
            } catch (Throwable ex) {
                d.result = encodeThrowable(ex);
            }
        return d;
    }

    @SuppressWarnings("serial")
    static final class BiRelay<T,U> extends BiCompletion<T,U,Void> { // for And
        BiRelay(CompletableFuture<Void> dep,
                CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d;
            CompletableFuture<T> a;
            CompletableFuture<U> b;
            Object r, s, z; Throwable x;
            if (   (a = src) == null || (r = a.result) == null
                || (b = snd) == null || (s = b.result) == null
                || (d = dep) == null)
                return null;
            if (d.result == null) {
                if ((r instanceof AltResult
                     && (x = ((AltResult)(z = r)).ex) != null) ||
                    (s instanceof AltResult
                     && (x = ((AltResult)(z = s)).ex) != null))
                    d.completeThrowable(x, z);
                else
                    d.completeNull();
            }
            src = null; snd = null; dep = null;
            return d.postFire(a, b, mode);
        }
    }

    /** Recursively constructs a tree of completions. */
    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs,
                                           int lo, int hi) {
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        if (lo > hi) // empty
            d.result = NIL;
        else {
            CompletableFuture<?> a, b; Object r, s, z; Throwable x;
            int mid = (lo + hi) >>> 1;
            if ((a = (lo == mid ? cfs[lo] :
                      andTree(cfs, lo, mid))) == null ||
                (b = (lo == hi ? a : (hi == mid+1) ? cfs[hi] :
                      andTree(cfs, mid+1, hi))) == null)
                throw new NullPointerException();
            if ((r = a.result) == null || (s = b.result) == null)
                a.bipush(b, new BiRelay<>(d, a, b));
            else if ((r instanceof AltResult
                      && (x = ((AltResult)(z = r)).ex) != null) ||
                     (s instanceof AltResult
                      && (x = ((AltResult)(z = s)).ex) != null))
                d.result = encodeThrowable(x, z);
            else
                d.result = NIL;
        }
        return d;
    }

    /* ------------- Projected (Ored) BiCompletions -------------- */

    /**
     * Pushes completion to this and b unless either done.
     * Caller should first check that result and b.result are both null.
     */
    final void orpush(CompletableFuture<?> b, BiCompletion<?,?,?> c) {
        if (c != null) {
            while (!tryPushStack(c)) {
                if (result != null) {
                    NEXT.set(c, null);
                    break;
                }
            }
            if (result != null)
                c.tryFire(SYNC);
            else
                b.unipush(new CoCompletion(c));
        }
    }

    @SuppressWarnings("serial")
    static final class OrApply<T,U extends T,V> extends BiCompletion<T,U,V> {
        Function<? super T,? extends V> fn;
        OrApply(Executor executor, CompletableFuture<V> dep,
                CompletableFuture<T> src, CompletableFuture<U> snd,
                Function<? super T,? extends V> fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> d; CompletableFuture<? extends T> a, b;
            Object r; Throwable x; Function<? super T,? extends V> f;
            if ((a = src) == null || (b = snd) == null
                || ((r = a.result) == null && (r = b.result) == null)
                || (d = dep) == null || (f = fn) == null)
                return null;
            tryComplete: if (d.result == null) {
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    if (r instanceof AltResult) {
                        if ((x = ((AltResult)r).ex) != null) {
                            d.completeThrowable(x, r);
                            break tryComplete;
                        }
                        r = null;
                    }
                    @SuppressWarnings("unchecked") T t = (T) r;
                    d.completeValue(f.apply(t));
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    private <U extends T,V> CompletableFuture<V> orApplyStage(
        Executor e, CompletionStage<U> o, Function<? super T, ? extends V> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();

        Object r; CompletableFuture<? extends T> z;
        if ((r = (z = this).result) != null ||
            (r = (z = b).result) != null)
            return z.uniApplyNow(r, e, f);

        CompletableFuture<V> d = newIncompleteFuture();
        orpush(b, new OrApply<T,U,V>(e, d, this, b, f));
        return d;
    }

    @SuppressWarnings("serial")
    static final class OrAccept<T,U extends T> extends BiCompletion<T,U,Void> {
        Consumer<? super T> fn;
        OrAccept(Executor executor, CompletableFuture<Void> dep,
                 CompletableFuture<T> src, CompletableFuture<U> snd,
                 Consumer<? super T> fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d; CompletableFuture<? extends T> a, b;
            Object r; Throwable x; Consumer<? super T> f;
            if ((a = src) == null || (b = snd) == null
                || ((r = a.result) == null && (r = b.result) == null)
                || (d = dep) == null || (f = fn) == null)
                return null;
            tryComplete: if (d.result == null) {
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    if (r instanceof AltResult) {
                        if ((x = ((AltResult)r).ex) != null) {
                            d.completeThrowable(x, r);
                            break tryComplete;
                        }
                        r = null;
                    }
                    @SuppressWarnings("unchecked") T t = (T) r;
                    f.accept(t);
                    d.completeNull();
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(
        Executor e, CompletionStage<U> o, Consumer<? super T> f) {
        CompletableFuture<U> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();

        Object r; CompletableFuture<? extends T> z;
        if ((r = (z = this).result) != null ||
            (r = (z = b).result) != null)
            return z.uniAcceptNow(r, e, f);

        CompletableFuture<Void> d = newIncompleteFuture();
        orpush(b, new OrAccept<T,U>(e, d, this, b, f));
        return d;
    }

    @SuppressWarnings("serial")
    static final class OrRun<T,U> extends BiCompletion<T,U,Void> {
        Runnable fn;
        OrRun(Executor executor, CompletableFuture<Void> dep,
              CompletableFuture<T> src, CompletableFuture<U> snd,
              Runnable fn) {
            super(executor, dep, src, snd); this.fn = fn;
        }
        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> d; CompletableFuture<?> a, b;
            Object r; Throwable x; Runnable f;
            if ((a = src) == null || (b = snd) == null
                || ((r = a.result) == null && (r = b.result) == null)
                || (d = dep) == null || (f = fn) == null)
                return null;
            if (d.result == null) {
                try {
                    if (mode <= 0 && !claim())
                        return null;
                    else if (r instanceof AltResult
                        && (x = ((AltResult)r).ex) != null)
                        d.completeThrowable(x, r);
                    else {
                        f.run();
                        d.completeNull();
                    }
                } catch (Throwable ex) {
                    d.completeThrowable(ex);
                }
            }
            src = null; snd = null; dep = null; fn = null;
            return d.postFire(a, b, mode);
        }
    }

    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> o,
                                               Runnable f) {
        CompletableFuture<?> b;
        if (f == null || (b = o.toCompletableFuture()) == null)
            throw new NullPointerException();

        Object r; CompletableFuture<?> z;
        if ((r = (z = this).result) != null ||
            (r = (z = b).result) != null)
            return z.uniRunNow(r, e, f);

        CompletableFuture<Void> d = newIncompleteFuture();
        orpush(b, new OrRun<>(e, d, this, b, f));
        return d;
    }

    /** Completion for an anyOf input future. */
    @SuppressWarnings("serial")
    static class AnyOf extends Completion {
        CompletableFuture<Object> dep; CompletableFuture<?> src;
        CompletableFuture<?>[] srcs;
        AnyOf(CompletableFuture<Object> dep, CompletableFuture<?> src,
              CompletableFuture<?>[] srcs) {
            this.dep = dep; this.src = src; this.srcs = srcs;
        }
        final CompletableFuture<Object> tryFire(int mode) {
            // assert mode != ASYNC;
            CompletableFuture<Object> d; CompletableFuture<?> a;
            CompletableFuture<?>[] as;
            Object r;
            if ((a = src) == null || (r = a.result) == null
                || (d = dep) == null || (as = srcs) == null)
                return null;
            src = null; dep = null; srcs = null;
            if (d.completeRelay(r)) {
                for (CompletableFuture<?> b : as)
                    if (b != a)
                        b.cleanStack();
                if (mode < 0)
                    return d;
                else
                    d.postComplete();
            }
            return null;
        }
        final boolean isLive() {
            CompletableFuture<Object> d;
            return (d = dep) != null && d.result == null;
        }
    }

    /* ------------- Zero-input Async forms -------------- */

    @SuppressWarnings("serial")
    static final class AsyncSupply<T> extends ForkJoinTask<Void>
        implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep; Supplier<? extends T> fn;
        AsyncSupply(CompletableFuture<T> dep, Supplier<? extends T> fn) {
            this.dep = dep; this.fn = fn;
        }

        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { run(); return false; }

        public void run() {
            CompletableFuture<T> d; Supplier<? extends T> f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        d.completeValue(f.get());
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                d.postComplete();
            }
        }
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor e,
                                                     Supplier<U> f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<U> d = new CompletableFuture<U>();
        e.execute(new AsyncSupply<U>(d, f));
        return d;
    }

    @SuppressWarnings("serial")
    static final class AsyncRun extends ForkJoinTask<Void>
        implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep; Runnable fn;
        AsyncRun(CompletableFuture<Void> dep, Runnable fn) {
            this.dep = dep; this.fn = fn;
        }

        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { run(); return false; }

        public void run() {
            CompletableFuture<Void> d; Runnable f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        f.run();
                        d.completeNull();
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                d.postComplete();
            }
        }
    }

    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        if (f == null) throw new NullPointerException();
        CompletableFuture<Void> d = new CompletableFuture<Void>();
        e.execute(new AsyncRun(d, f));
        return d;
    }

    /* ------------- Signallers -------------- */

    /**
     * Completion for recording and releasing a waiting thread.  This
     * class implements ManagedBlocker to avoid starvation when
     * blocking actions pile up in ForkJoinPools.
     */
    @SuppressWarnings("serial")
    static final class Signaller extends Completion
        implements ForkJoinPool.ManagedBlocker {
        long nanos;                    // remaining wait time if timed
        final long deadline;           // non-zero if timed
        final boolean interruptible;
        boolean interrupted;
        volatile Thread thread;

        Signaller(boolean interruptible, long nanos, long deadline) {
            this.thread = Thread.currentThread();
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.deadline = deadline;
        }
        final CompletableFuture<?> tryFire(int ignore) {
            Thread w; // no need to atomically claim
            if ((w = thread) != null) {
                thread = null;
                LockSupport.unpark(w);
            }
            return null;
        }
        public boolean isReleasable() {
            if (Thread.interrupted())
                interrupted = true;
            return ((interrupted && interruptible) ||
                    (deadline != 0L &&
                     (nanos <= 0L ||
                      (nanos = deadline - System.nanoTime()) <= 0L)) ||
                    thread == null);
        }
        public boolean block() {
            while (!isReleasable()) {
                if (deadline == 0L)
                    LockSupport.park(this);
                else
                    LockSupport.parkNanos(this, nanos);
            }
            return true;
        }
        final boolean isLive() { return thread != null; }
    }

    /**
     * Returns raw result after waiting, or null if interruptible and
     * interrupted.
     */
    private Object waitingGet(boolean interruptible) {
        if (interruptible && Thread.interrupted())
            return null;
        Signaller q = null;
        boolean queued = false;
        Object r;
        while ((r = result) == null) {
            if (q == null) {
                q = new Signaller(interruptible, 0L, 0L);
                if (Thread.currentThread() instanceof ForkJoinWorkerThread)
                    ForkJoinPool.helpAsyncBlocker(defaultExecutor(), q);
            }
            else if (!queued)
                queued = tryPushStack(q);
            else if (interruptible && q.interrupted) {
                q.thread = null;
                cleanStack();
                return null;
            }
            else {
                try {
                    ForkJoinPool.managedBlock(q);
                } catch (InterruptedException ie) { // currently cannot happen
                    q.interrupted = true;
                }
            }
        }
        if (q != null) {
            q.thread = null;
            if (q.interrupted)
                Thread.currentThread().interrupt();
        }
        postComplete();
        return r;
    }

    /**
     * Returns raw result after waiting, or null if interrupted, or
     * throws TimeoutException on timeout.
     */
    private Object timedGet(long nanos) throws TimeoutException {
        long d = System.nanoTime() + nanos;
        long deadline = (d == 0L) ? 1L : d; // avoid 0
        boolean interrupted = false, queued = false;
        Signaller q = null;
        Object r = null;
        for (;;) { // order of checking interrupt, result, timeout matters
            if (interrupted || (interrupted = Thread.interrupted()))
                break;
            else if ((r = result) != null)
                break;
            else if (nanos <= 0L)
                break;
            else if (q == null) {
                q = new Signaller(true, nanos, deadline);
                if (Thread.currentThread() instanceof ForkJoinWorkerThread)
                    ForkJoinPool.helpAsyncBlocker(defaultExecutor(), q);
            }
            else if (!queued)
                queued = tryPushStack(q);
            else {
                try {
                    ForkJoinPool.managedBlock(q);
                    interrupted = q.interrupted;
                    nanos = q.nanos;
                } catch (InterruptedException ie) {
                    interrupted = true;
                }
            }
        }
        if (q != null) {
            q.thread = null;
            if (r == null)
                cleanStack();
        }
        if (r != null) {
            if (interrupted)
                Thread.currentThread().interrupt();
            postComplete();
            return r;
        } else if (interrupted)
            return null;
        else
            throw new TimeoutException();
    }

    /* ------------- public methods -------------- */

    /**
     * Creates a new incomplete CompletableFuture.
     */
    public CompletableFuture() {
    }

    /**
     * Creates a new complete CompletableFuture with given encoded result.
     */
    CompletableFuture(Object r) {
        RESULT.setRelease(this, r);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} with
     * the value obtained by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned CompletableFuture
     * @param <U> the function's return type
     * @return the new CompletableFuture
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return asyncSupplyStage(ASYNC_POOL, supplier);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor with the value obtained
     * by calling the given Supplier.
     *
     * @param supplier a function returning the value to be used
     * to complete the returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @param <U> the function's return type
     * @return the new CompletableFuture
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), supplier);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the {@link ForkJoinPool#commonPool()} after
     * it runs the given action.
     *
     * @param runnable the action to run before completing the
     * returned CompletableFuture
     * @return the new CompletableFuture
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return asyncRunStage(ASYNC_POOL, runnable);
    }

    /**
     * Returns a new CompletableFuture that is asynchronously completed
     * by a task running in the given executor after it runs the given
     * action.
     *
     * @param runnable the action to run before completing the
     * returned CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @return the new CompletableFuture
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable,
                                                   Executor executor) {
        return asyncRunStage(screenExecutor(executor), runnable);
    }

    /**
     * Returns a new CompletableFuture that is already completed with
     * the given value.
     *
     * @param value the value
     * @param <U> the type of the value
     * @return the completed CompletableFuture
     */
    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture<U>((value == null) ? NIL : value);
    }

    /**
     * Returns {@code true} if completed in any fashion: normally,
     * exceptionally, or via cancellation.
     *
     * @return {@code true} if completed
     */
    public boolean isDone() {
        return result != null;
    }

    /**
     * Waits if necessary for this future to complete, and then
     * returns its result.
     *
     * @return the result value
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    @SuppressWarnings("unchecked")
    public T get() throws InterruptedException, ExecutionException {
        Object r;
        if ((r = result) == null)
            r = waitingGet(true);
        return (T) reportGet(r);
    }

    /**
     * Waits if necessary for at most the given time for this future
     * to complete, and then returns its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the result value
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    @SuppressWarnings("unchecked")
    public T get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        Object r;
        if ((r = result) == null)
            r = timedGet(nanos);
        return (T) reportGet(r);
    }

    /**
     * Returns the result value when complete, or throws an
     * (unchecked) exception if completed exceptionally. To better
     * conform with the use of common functional forms, if a
     * computation involved in the completion of this
     * CompletableFuture threw an exception, this method throws an
     * (unchecked) {@link CompletionException} with the underlying
     * exception as its cause.
     *
     * @return the result value
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException if this future completed
     * exceptionally or a completion computation threw an exception
     */
    @SuppressWarnings("unchecked")
    public T join() {
        Object r;
        if ((r = result) == null)
            r = waitingGet(false);
        return (T) reportJoin(r);
    }

    /**
     * Returns the result value (or throws any encountered exception)
     * if completed, else returns the given valueIfAbsent.
     *
     * @param valueIfAbsent the value to return if not completed
     * @return the result value, if completed, else the given valueIfAbsent
     * @throws CancellationException if the computation was cancelled
     * @throws CompletionException if this future completed
     * exceptionally or a completion computation threw an exception
     */
    @SuppressWarnings("unchecked")
    public T getNow(T valueIfAbsent) {
        Object r;
        return ((r = result) == null) ? valueIfAbsent : (T) reportJoin(r);
    }

    /**
     * If not already completed, sets the value returned by {@link
     * #get()} and related methods to the given value.
     *
     * @param value the result value
     * @return {@code true} if this invocation caused this CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    public boolean complete(T value) {
        boolean triggered = completeValue(value);
        postComplete();
        return triggered;
    }

    /**
     * If not already completed, causes invocations of {@link #get()}
     * and related methods to throw the given exception.
     *
     * @param ex the exception
     * @return {@code true} if this invocation caused this CompletableFuture
     * to transition to a completed state, else {@code false}
     */
    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        boolean triggered = internalComplete(new AltResult(ex));
        postComplete();
        return triggered;
    }

    public <U> CompletableFuture<U> thenApply(
        Function<? super T,? extends U> fn) {
        return uniApplyStage(null, fn);
    }

    public <U> CompletableFuture<U> thenApplyAsync(
        Function<? super T,? extends U> fn) {
        return uniApplyStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> thenApplyAsync(
        Function<? super T,? extends U> fn, Executor executor) {
        return uniApplyStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(defaultExecutor(), action);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action,
                                                   Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action);
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(defaultExecutor(), action);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action,
                                                Executor executor) {
        return uniRunStage(screenExecutor(executor), action);
    }

    public <U,V> CompletableFuture<V> thenCombine(
        CompletionStage<? extends U> other,
        BiFunction<? super T,? super U,? extends V> fn) {
        return biApplyStage(null, other, fn);
    }

    public <U,V> CompletableFuture<V> thenCombineAsync(
        CompletionStage<? extends U> other,
        BiFunction<? super T,? super U,? extends V> fn) {
        return biApplyStage(defaultExecutor(), other, fn);
    }

    public <U,V> CompletableFuture<V> thenCombineAsync(
        CompletionStage<? extends U> other,
        BiFunction<? super T,? super U,? extends V> fn, Executor executor) {
        return biApplyStage(screenExecutor(executor), other, fn);
    }

    public <U> CompletableFuture<Void> thenAcceptBoth(
        CompletionStage<? extends U> other,
        BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, other, action);
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(
        CompletionStage<? extends U> other,
        BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(defaultExecutor(), other, action);
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(
        CompletionStage<? extends U> other,
        BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), other, action);
    }

    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other,
                                                Runnable action) {
        return biRunStage(null, other, action);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other,
                                                     Runnable action) {
        return biRunStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other,
                                                     Runnable action,
                                                     Executor executor) {
        return biRunStage(screenExecutor(executor), other, action);
    }

    public <U> CompletableFuture<U> applyToEither(
        CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(null, other, fn);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(
        CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(defaultExecutor(), other, fn);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(
        CompletionStage<? extends T> other, Function<? super T, U> fn,
        Executor executor) {
        return orApplyStage(screenExecutor(executor), other, fn);
    }

    public CompletableFuture<Void> acceptEither(
        CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(null, other, action);
    }

    public CompletableFuture<Void> acceptEitherAsync(
        CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> acceptEitherAsync(
        CompletionStage<? extends T> other, Consumer<? super T> action,
        Executor executor) {
        return orAcceptStage(screenExecutor(executor), other, action);
    }

    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other,
                                                  Runnable action) {
        return orRunStage(null, other, action);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                       Runnable action) {
        return orRunStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other,
                                                       Runnable action,
                                                       Executor executor) {
        return orRunStage(screenExecutor(executor), other, action);
    }

    public <U> CompletableFuture<U> thenCompose(
        Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(null, fn);
    }

    public <U> CompletableFuture<U> thenComposeAsync(
        Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> thenComposeAsync(
        Function<? super T, ? extends CompletionStage<U>> fn,
        Executor executor) {
        return uniComposeStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<T> whenComplete(
        BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }

    public CompletableFuture<T> whenCompleteAsync(
        BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(defaultExecutor(), action);
    }

    public CompletableFuture<T> whenCompleteAsync(
        BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action);
    }

    public <U> CompletableFuture<U> handle(
        BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(null, fn);
    }

    public <U> CompletableFuture<U> handleAsync(
        BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> handleAsync(
        BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return uniHandleStage(screenExecutor(executor), fn);
    }

    /**
     * Returns this CompletableFuture.
     *
     * @return this CompletableFuture
     */
    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    public CompletableFuture<T> exceptionally(
        Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(null, fn);
    }

    public CompletableFuture<T> exceptionallyAsync(
        Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(defaultExecutor(), fn);
    }

    public CompletableFuture<T> exceptionallyAsync(
        Function<Throwable, ? extends T> fn, Executor executor) {
        return uniExceptionallyStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<T> exceptionallyCompose(
        Function<Throwable, ? extends CompletionStage<T>> fn) {
        return uniComposeExceptionallyStage(null, fn);
    }

    public CompletableFuture<T> exceptionallyComposeAsync(
        Function<Throwable, ? extends CompletionStage<T>> fn) {
        return uniComposeExceptionallyStage(defaultExecutor(), fn);
    }

    public CompletableFuture<T> exceptionallyComposeAsync(
        Function<Throwable, ? extends CompletionStage<T>> fn,
        Executor executor) {
        return uniComposeExceptionallyStage(screenExecutor(executor), fn);
    }

    /* ------------- Arbitrary-arity constructions -------------- */

    /**
     * Returns a new CompletableFuture that is completed when all of
     * the given CompletableFutures complete.  If any of the given
     * CompletableFutures complete exceptionally, then the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  Otherwise, the results,
     * if any, of the given CompletableFutures are not reflected in
     * the returned CompletableFuture, but may be obtained by
     * inspecting them individually. If no CompletableFutures are
     * provided, returns a CompletableFuture completed with the value
     * {@code null}.
     *
     * <p>Among the applications of this method is to await completion
     * of a set of independent CompletableFutures before continuing a
     * program, as in: {@code CompletableFuture.allOf(c1, c2,
     * c3).join();}.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that is completed when all of the
     * given CompletableFutures complete
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    /**
     * Returns a new CompletableFuture that is completed when any of
     * the given CompletableFutures complete, with the same result.
     * Otherwise, if it completed exceptionally, the returned
     * CompletableFuture also does so, with a CompletionException
     * holding this exception as its cause.  If no CompletableFutures
     * are provided, returns an incomplete CompletableFuture.
     *
     * @param cfs the CompletableFutures
     * @return a new CompletableFuture that is completed with the
     * result or exception of any of the given CompletableFutures when
     * one completes
     * @throws NullPointerException if the array or any of its elements are
     * {@code null}
     */
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        int n; Object r;
        if ((n = cfs.length) <= 1)
            return (n == 0)
                ? new CompletableFuture<Object>()
                : uniCopyStage(cfs[0]);
        for (CompletableFuture<?> cf : cfs)
            if ((r = cf.result) != null)
                return new CompletableFuture<Object>(encodeRelay(r));
        cfs = cfs.clone();
        CompletableFuture<Object> d = new CompletableFuture<>();
        for (CompletableFuture<?> cf : cfs)
            cf.unipush(new AnyOf(d, cf, cfs));
        // If d was completed while we were adding completions, we should
        // clean the stack of any sources that may have had completions
        // pushed on their stack after d was completed.
        if (d.result != null)
            for (int i = 0, len = cfs.length; i < len; i++)
                if (cfs[i].result != null)
                    for (i++; i < len; i++)
                        if (cfs[i].result == null)
                            cfs[i].cleanStack();
        return d;
    }

    /* ------------- Control and status methods -------------- */

    /**
     * If not already completed, completes this CompletableFuture with
     * a {@link CancellationException}. Dependent CompletableFutures
     * that have not already completed will also complete
     * exceptionally, with a {@link CompletionException} caused by
     * this {@code CancellationException}.
     *
     * @param mayInterruptIfRunning this value has no effect in this
     * implementation because interrupts are not used to control
     * processing.
     *
     * @return {@code true} if this task is now cancelled
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = (result == null) &&
            internalComplete(new AltResult(new CancellationException()));
        postComplete();
        return cancelled || isCancelled();
    }

    /**
     * Returns {@code true} if this CompletableFuture was cancelled
     * before it completed normally.
     *
     * @return {@code true} if this CompletableFuture was cancelled
     * before it completed normally
     */
    public boolean isCancelled() {
        Object r;
        return ((r = result) instanceof AltResult) &&
            (((AltResult)r).ex instanceof CancellationException);
    }

    /**
     * Returns {@code true} if this CompletableFuture completed
     * exceptionally, in any way. Possible causes include
     * cancellation, explicit invocation of {@code
     * completeExceptionally}, and abrupt termination of a
     * CompletionStage action.
     *
     * @return {@code true} if this CompletableFuture completed
     * exceptionally
     */
    public boolean isCompletedExceptionally() {
        Object r;
        return ((r = result) instanceof AltResult) && r != NIL;
    }

    /**
     * Forcibly sets or resets the value subsequently returned by
     * method {@link #get()} and related methods, whether or not
     * already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param value the completion value
     */
    public void obtrudeValue(T value) {
        result = (value == null) ? NIL : value;
        postComplete();
    }

    /**
     * Forcibly causes subsequent invocations of method {@link #get()}
     * and related methods to throw the given exception, whether or
     * not already completed. This method is designed for use only in
     * error recovery actions, and even in such situations may result
     * in ongoing dependent completions using established versus
     * overwritten outcomes.
     *
     * @param ex the exception
     * @throws NullPointerException if the exception is null
     */
    public void obtrudeException(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        result = new AltResult(ex);
        postComplete();
    }

    /**
     * Returns the estimated number of CompletableFutures whose
     * completions are awaiting completion of this CompletableFuture.
     * This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return the number of dependent CompletableFutures
     */
    public int getNumberOfDependents() {
        int count = 0;
        for (Completion p = stack; p != null; p = p.next)
            ++count;
        return count;
    }

    /**
     * Returns a string identifying this CompletableFuture, as well as
     * its completion state.  The state, in brackets, contains the
     * String {@code "Completed Normally"} or the String {@code
     * "Completed Exceptionally"}, or the String {@code "Not
     * completed"} followed by the number of CompletableFutures
     * dependent upon its completion, if any.
     *
     * @return a string identifying this CompletableFuture, as well as its state
     */
    public String toString() {
        Object r = result;
        int count = 0; // avoid call to getNumberOfDependents in case disabled
        for (Completion p = stack; p != null; p = p.next)
            ++count;
        return super.toString() +
            ((r == null)
             ? ((count == 0)
                ? "[Not completed]"
                : "[Not completed, " + count + " dependents]")
             : (((r instanceof AltResult) && ((AltResult)r).ex != null)
                ? "[Completed exceptionally: " + ((AltResult)r).ex + "]"
                : "[Completed normally]"));
    }

    // jdk9 additions

    /**
     * Returns a new incomplete CompletableFuture of the type to be
     * returned by a CompletionStage method. Subclasses should
     * normally override this method to return an instance of the same
     * class as this CompletableFuture. The default implementation
     * returns an instance of class CompletableFuture.
     *
     * @param <U> the type of the value
     * @return a new CompletableFuture
     * @since 9
     */
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<U>();
    }

    /**
     * Returns the default Executor used for async methods that do not
     * specify an Executor. This class uses the {@link
     * ForkJoinPool#commonPool()} if it supports more than one
     * parallel thread, or else an Executor using one thread per async
     * task.  This method may be overridden in subclasses to return
     * an Executor that provides at least one independent thread.
     *
     * @return the executor
     * @since 9
     */
    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }

    /**
     * Returns a new CompletableFuture that is completed normally with
     * the same value as this CompletableFuture when it completes
     * normally. If this CompletableFuture completes exceptionally,
     * then the returned CompletableFuture completes exceptionally
     * with a CompletionException with this exception as cause. The
     * behavior is equivalent to {@code thenApply(x -> x)}. This
     * method may be useful as a form of "defensive copying", to
     * prevent clients from completing, while still being able to
     * arrange dependent actions.
     *
     * @return the new CompletableFuture
     * @since 9
     */
    public CompletableFuture<T> copy() {
        return uniCopyStage(this);
    }

    /**
     * Returns a new CompletionStage that is completed normally with
     * the same value as this CompletableFuture when it completes
     * normally, and cannot be independently completed or otherwise
     * used in ways not defined by the methods of interface {@link
     * CompletionStage}.  If this CompletableFuture completes
     * exceptionally, then the returned CompletionStage completes
     * exceptionally with a CompletionException with this exception as
     * cause.
     *
     * <p>Unless overridden by a subclass, a new non-minimal
     * CompletableFuture with all methods available can be obtained from
     * a minimal CompletionStage via {@link #toCompletableFuture()}.
     * For example, completion of a minimal stage can be awaited by
     *
     * <pre> {@code minimalStage.toCompletableFuture().join(); }</pre>
     *
     * @return the new CompletionStage
     * @since 9
     */
    public CompletionStage<T> minimalCompletionStage() {
        return uniAsMinimalStage();
    }

    /**
     * Completes this CompletableFuture with the result of
     * the given Supplier function invoked from an asynchronous
     * task using the given executor.
     *
     * @param supplier a function returning the value to be used
     * to complete this CompletableFuture
     * @param executor the executor to use for asynchronous execution
     * @return this CompletableFuture
     * @since 9
     */
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier,
                                              Executor executor) {
        if (supplier == null || executor == null)
            throw new NullPointerException();
        executor.execute(new AsyncSupply<T>(this, supplier));
        return this;
    }

    /**
     * Completes this CompletableFuture with the result of the given
     * Supplier function invoked from an asynchronous task using the
     * default executor.
     *
     * @param supplier a function returning the value to be used
     * to complete this CompletableFuture
     * @return this CompletableFuture
     * @since 9
     */
    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, defaultExecutor());
    }

    /**
     * Exceptionally completes this CompletableFuture with
     * a {@link TimeoutException} if not otherwise completed
     * before the given timeout.
     *
     * @param timeout how long to wait before completing exceptionally
     *        with a TimeoutException, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return this CompletableFuture
     * @since 9
     */
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (unit == null)
            throw new NullPointerException();
        if (result == null)
            whenComplete(new Canceller(Delayer.delay(new Timeout(this),
                                                     timeout, unit)));
        return this;
    }

    /**
     * Completes this CompletableFuture with the given value if not
     * otherwise completed before the given timeout.
     *
     * @param value the value to use upon timeout
     * @param timeout how long to wait before completing normally
     *        with the given value, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return this CompletableFuture
     * @since 9
     */
    public CompletableFuture<T> completeOnTimeout(T value, long timeout,
                                                  TimeUnit unit) {
        if (unit == null)
            throw new NullPointerException();
        if (result == null)
            whenComplete(new Canceller(Delayer.delay(
                                           new DelayedCompleter<T>(this, value),
                                           timeout, unit)));
        return this;
    }

    /**
     * Returns a new Executor that submits a task to the given base
     * executor after the given delay (or no delay if non-positive).
     * Each delay commences upon invocation of the returned executor's
     * {@code execute} method.
     *
     * @param delay how long to delay, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code delay} parameter
     * @param executor the base executor
     * @return the new delayed executor
     * @since 9
     */
    public static Executor delayedExecutor(long delay, TimeUnit unit,
                                           Executor executor) {
        if (unit == null || executor == null)
            throw new NullPointerException();
        return new DelayedExecutor(delay, unit, executor);
    }

    /**
     * Returns a new Executor that submits a task to the default
     * executor after the given delay (or no delay if non-positive).
     * Each delay commences upon invocation of the returned executor's
     * {@code execute} method.
     *
     * @param delay how long to delay, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code delay} parameter
     * @return the new delayed executor
     * @since 9
     */
    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        if (unit == null)
            throw new NullPointerException();
        return new DelayedExecutor(delay, unit, ASYNC_POOL);
    }

    /**
     * Returns a new CompletionStage that is already completed with
     * the given value and supports only those methods in
     * interface {@link CompletionStage}.
     *
     * @param value the value
     * @param <U> the type of the value
     * @return the completed CompletionStage
     * @since 9
     */
    public static <U> CompletionStage<U> completedStage(U value) {
        return new MinimalStage<U>((value == null) ? NIL : value);
    }

    /**
     * Returns a new CompletableFuture that is already completed
     * exceptionally with the given exception.
     *
     * @param ex the exception
     * @param <U> the type of the value
     * @return the exceptionally completed CompletableFuture
     * @since 9
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        return new CompletableFuture<U>(new AltResult(ex));
    }

    /**
     * Returns a new CompletionStage that is already completed
     * exceptionally with the given exception and supports only those
     * methods in interface {@link CompletionStage}.
     *
     * @param ex the exception
     * @param <U> the type of the value
     * @return the exceptionally completed CompletionStage
     * @since 9
     */
    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        if (ex == null) throw new NullPointerException();
        return new MinimalStage<U>(new AltResult(ex));
    }

    /**
     * Singleton delay scheduler, used only for starting and
     * cancelling tasks.
     */
    static final class Delayer {
        static ScheduledFuture<?> delay(Runnable command, long delay,
                                        TimeUnit unit) {
            return delayer.schedule(command, delay, unit);
        }

        static final class DaemonThreadFactory implements ThreadFactory {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelayScheduler");
                return t;
            }
        }

        static final ScheduledThreadPoolExecutor delayer;
        static {
            (delayer = new ScheduledThreadPoolExecutor(
                1, new DaemonThreadFactory())).
                setRemoveOnCancelPolicy(true);
        }
    }

    // Little class-ified lambdas to better support monitoring

    static final class DelayedExecutor implements Executor {
        final long delay;
        final TimeUnit unit;
        final Executor executor;
        DelayedExecutor(long delay, TimeUnit unit, Executor executor) {
            this.delay = delay; this.unit = unit; this.executor = executor;
        }
        public void execute(Runnable r) {
            Delayer.delay(new TaskSubmitter(executor, r), delay, unit);
        }
    }

    /** Action to submit user task */
    static final class TaskSubmitter implements Runnable {
        final Executor executor;
        final Runnable action;
        TaskSubmitter(Executor executor, Runnable action) {
            this.executor = executor;
            this.action = action;
        }
        public void run() { executor.execute(action); }
    }

    /** Action to completeExceptionally on timeout */
    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;
        Timeout(CompletableFuture<?> f) { this.f = f; }
        public void run() {
            if (f != null && !f.isDone())
                f.completeExceptionally(new TimeoutException());
        }
    }

    /** Action to complete on timeout */
    static final class DelayedCompleter<U> implements Runnable {
        final CompletableFuture<U> f;
        final U u;
        DelayedCompleter(CompletableFuture<U> f, U u) { this.f = f; this.u = u; }
        public void run() {
            if (f != null)
                f.complete(u);
        }
    }

    /** Action to cancel unneeded timeouts */
    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;
        Canceller(Future<?> f) { this.f = f; }
        public void accept(Object ignore, Throwable ex) {
            if (ex == null && f != null && !f.isDone())
                f.cancel(false);
        }
    }

    /**
     * A subclass that just throws UOE for most non-CompletionStage methods.
     */
    static final class MinimalStage<T> extends CompletableFuture<T> {
        MinimalStage() { }
        MinimalStage(Object r) { super(r); }
        @Override public <U> CompletableFuture<U> newIncompleteFuture() {
            return new MinimalStage<U>(); }
        @Override public T get() {
            throw new UnsupportedOperationException(); }
        @Override public T get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException(); }
        @Override public T getNow(T valueIfAbsent) {
            throw new UnsupportedOperationException(); }
        @Override public T join() {
            throw new UnsupportedOperationException(); }
        @Override public boolean complete(T value) {
            throw new UnsupportedOperationException(); }
        @Override public boolean completeExceptionally(Throwable ex) {
            throw new UnsupportedOperationException(); }
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException(); }
        @Override public void obtrudeValue(T value) {
            throw new UnsupportedOperationException(); }
        @Override public void obtrudeException(Throwable ex) {
            throw new UnsupportedOperationException(); }
        @Override public boolean isDone() {
            throw new UnsupportedOperationException(); }
        @Override public boolean isCancelled() {
            throw new UnsupportedOperationException(); }
        @Override public boolean isCompletedExceptionally() {
            throw new UnsupportedOperationException(); }
        @Override public int getNumberOfDependents() {
            throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<T> completeAsync
            (Supplier<? extends T> supplier, Executor executor) {
            throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<T> completeAsync
            (Supplier<? extends T> supplier) {
            throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<T> orTimeout
            (long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<T> completeOnTimeout
            (T value, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<T> toCompletableFuture() {
            Object r;
            if ((r = result) != null)
                return new CompletableFuture<T>(encodeRelay(r));
            else {
                CompletableFuture<T> d = new CompletableFuture<>();
                unipush(new UniRelay<T,T>(d, this));
                return d;
            }
        }
    }

    // VarHandle mechanics
    private static final VarHandle RESULT;
    private static final VarHandle STACK;
    private static final VarHandle NEXT;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            RESULT = l.findVarHandle(CompletableFuture.class, "result", Object.class);
            STACK = l.findVarHandle(CompletableFuture.class, "stack", Completion.class);
            NEXT = l.findVarHandle(Completion.class, "next", Completion.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;
    }
}
