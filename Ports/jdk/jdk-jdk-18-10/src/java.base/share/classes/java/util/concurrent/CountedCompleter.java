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

/**
 * A {@link ForkJoinTask} with a completion action performed when
 * triggered and there are no remaining pending actions.
 * CountedCompleters are in general more robust in the
 * presence of subtask stalls and blockage than are other forms of
 * ForkJoinTasks, but are less intuitive to program.  Uses of
 * CountedCompleter are similar to those of other completion based
 * components (such as {@link java.nio.channels.CompletionHandler})
 * except that multiple <em>pending</em> completions may be necessary
 * to trigger the completion action {@link #onCompletion(CountedCompleter)},
 * not just one.
 * Unless initialized otherwise, the {@linkplain #getPendingCount pending
 * count} starts at zero, but may be (atomically) changed using
 * methods {@link #setPendingCount}, {@link #addToPendingCount}, and
 * {@link #compareAndSetPendingCount}. Upon invocation of {@link
 * #tryComplete}, if the pending action count is nonzero, it is
 * decremented; otherwise, the completion action is performed, and if
 * this completer itself has a completer, the process is continued
 * with its completer.  As is the case with related synchronization
 * components such as {@link Phaser} and {@link Semaphore}, these methods
 * affect only internal counts; they do not establish any further
 * internal bookkeeping. In particular, the identities of pending
 * tasks are not maintained. As illustrated below, you can create
 * subclasses that do record some or all pending tasks or their
 * results when needed.  As illustrated below, utility methods
 * supporting customization of completion traversals are also
 * provided. However, because CountedCompleters provide only basic
 * synchronization mechanisms, it may be useful to create further
 * abstract subclasses that maintain linkages, fields, and additional
 * support methods appropriate for a set of related usages.
 *
 * <p>A concrete CountedCompleter class must define method {@link
 * #compute}, that should in most cases (as illustrated below), invoke
 * {@code tryComplete()} once before returning. The class may also
 * optionally override method {@link #onCompletion(CountedCompleter)}
 * to perform an action upon normal completion, and method
 * {@link #onExceptionalCompletion(Throwable, CountedCompleter)} to
 * perform an action upon any exception.
 *
 * <p>CountedCompleters most often do not bear results, in which case
 * they are normally declared as {@code CountedCompleter<Void>}, and
 * will always return {@code null} as a result value.  In other cases,
 * you should override method {@link #getRawResult} to provide a
 * result from {@code join(), invoke()}, and related methods.  In
 * general, this method should return the value of a field (or a
 * function of one or more fields) of the CountedCompleter object that
 * holds the result upon completion. Method {@link #setRawResult} by
 * default plays no role in CountedCompleters.  It is possible, but
 * rarely applicable, to override this method to maintain other
 * objects or fields holding result data.
 *
 * <p>A CountedCompleter that does not itself have a completer (i.e.,
 * one for which {@link #getCompleter} returns {@code null}) can be
 * used as a regular ForkJoinTask with this added functionality.
 * However, any completer that in turn has another completer serves
 * only as an internal helper for other computations, so its own task
 * status (as reported in methods such as {@link ForkJoinTask#isDone})
 * is arbitrary; this status changes only upon explicit invocations of
 * {@link #complete}, {@link ForkJoinTask#cancel},
 * {@link ForkJoinTask#completeExceptionally(Throwable)} or upon
 * exceptional completion of method {@code compute}. Upon any
 * exceptional completion, the exception may be relayed to a task's
 * completer (and its completer, and so on), if one exists and it has
 * not otherwise already completed. Similarly, cancelling an internal
 * CountedCompleter has only a local effect on that completer, so is
 * not often useful.
 *
 * <p><b>Sample Usages.</b>
 *
 * <p><b>Parallel recursive decomposition.</b> CountedCompleters may
 * be arranged in trees similar to those often used with {@link
 * RecursiveAction}s, although the constructions involved in setting
 * them up typically vary. Here, the completer of each task is its
 * parent in the computation tree. Even though they entail a bit more
 * bookkeeping, CountedCompleters may be better choices when applying
 * a possibly time-consuming operation (that cannot be further
 * subdivided) to each element of an array or collection; especially
 * when the operation takes a significantly different amount of time
 * to complete for some elements than others, either because of
 * intrinsic variation (for example I/O) or auxiliary effects such as
 * garbage collection.  Because CountedCompleters provide their own
 * continuations, other tasks need not block waiting to perform them.
 *
 * <p>For example, here is an initial version of a utility method that
 * uses divide-by-two recursive decomposition to divide work into
 * single pieces (leaf tasks). Even when work is split into individual
 * calls, tree-based techniques are usually preferable to directly
 * forking leaf tasks, because they reduce inter-thread communication
 * and improve load balancing. In the recursive case, the second of
 * each pair of subtasks to finish triggers completion of their parent
 * (because no result combination is performed, the default no-op
 * implementation of method {@code onCompletion} is not overridden).
 * The utility method sets up the root task and invokes it (here,
 * implicitly using the {@link ForkJoinPool#commonPool()}).  It is
 * straightforward and reliable (but not optimal) to always set the
 * pending count to the number of child tasks and call {@code
 * tryComplete()} immediately before returning.
 *
 * <pre> {@code
 * public static <E> void forEach(E[] array, Consumer<E> action) {
 *   class Task extends CountedCompleter<Void> {
 *     final int lo, hi;
 *     Task(Task parent, int lo, int hi) {
 *       super(parent); this.lo = lo; this.hi = hi;
 *     }
 *
 *     public void compute() {
 *       if (hi - lo >= 2) {
 *         int mid = (lo + hi) >>> 1;
 *         // must set pending count before fork
 *         setPendingCount(2);
 *         new Task(this, mid, hi).fork(); // right child
 *         new Task(this, lo, mid).fork(); // left child
 *       }
 *       else if (hi > lo)
 *         action.accept(array[lo]);
 *       tryComplete();
 *     }
 *   }
 *   new Task(null, 0, array.length).invoke();
 * }}</pre>
 *
 * This design can be improved by noticing that in the recursive case,
 * the task has nothing to do after forking its right task, so can
 * directly invoke its left task before returning. (This is an analog
 * of tail recursion removal.)  Also, when the last action in a task
 * is to fork or invoke a subtask (a "tail call"), the call to {@code
 * tryComplete()} can be optimized away, at the cost of making the
 * pending count look "off by one".
 *
 * <pre> {@code
 *     public void compute() {
 *       if (hi - lo >= 2) {
 *         int mid = (lo + hi) >>> 1;
 *         setPendingCount(1); // looks off by one, but correct!
 *         new Task(this, mid, hi).fork(); // right child
 *         new Task(this, lo, mid).compute(); // direct invoke
 *       } else {
 *         if (hi > lo)
 *           action.accept(array[lo]);
 *         tryComplete();
 *       }
 *     }}</pre>
 *
 * As a further optimization, notice that the left task need not even exist.
 * Instead of creating a new one, we can continue using the original task,
 * and add a pending count for each fork.  Additionally, because no task
 * in this tree implements an {@link #onCompletion(CountedCompleter)} method,
 * {@code tryComplete} can be replaced with {@link #propagateCompletion}.
 *
 * <pre> {@code
 *     public void compute() {
 *       int n = hi - lo;
 *       for (; n >= 2; n /= 2) {
 *         addToPendingCount(1);
 *         new Task(this, lo + n/2, lo + n).fork();
 *       }
 *       if (n > 0)
 *         action.accept(array[lo]);
 *       propagateCompletion();
 *     }}</pre>
 *
 * When pending counts can be precomputed, they can be established in
 * the constructor:
 *
 * <pre> {@code
 * public static <E> void forEach(E[] array, Consumer<E> action) {
 *   class Task extends CountedCompleter<Void> {
 *     final int lo, hi;
 *     Task(Task parent, int lo, int hi) {
 *       super(parent, 31 - Integer.numberOfLeadingZeros(hi - lo));
 *       this.lo = lo; this.hi = hi;
 *     }
 *
 *     public void compute() {
 *       for (int n = hi - lo; n >= 2; n /= 2)
 *         new Task(this, lo + n/2, lo + n).fork();
 *       action.accept(array[lo]);
 *       propagateCompletion();
 *     }
 *   }
 *   if (array.length > 0)
 *     new Task(null, 0, array.length).invoke();
 * }}</pre>
 *
 * Additional optimizations of such classes might entail specializing
 * classes for leaf steps, subdividing by say, four, instead of two
 * per iteration, and using an adaptive threshold instead of always
 * subdividing down to single elements.
 *
 * <p><b>Searching.</b> A tree of CountedCompleters can search for a
 * value or property in different parts of a data structure, and
 * report a result in an {@link
 * java.util.concurrent.atomic.AtomicReference AtomicReference} as
 * soon as one is found. The others can poll the result to avoid
 * unnecessary work. (You could additionally {@linkplain #cancel
 * cancel} other tasks, but it is usually simpler and more efficient
 * to just let them notice that the result is set and if so skip
 * further processing.)  Illustrating again with an array using full
 * partitioning (again, in practice, leaf tasks will almost always
 * process more than one element):
 *
 * <pre> {@code
 * class Searcher<E> extends CountedCompleter<E> {
 *   final E[] array; final AtomicReference<E> result; final int lo, hi;
 *   Searcher(CountedCompleter<?> p, E[] array, AtomicReference<E> result, int lo, int hi) {
 *     super(p);
 *     this.array = array; this.result = result; this.lo = lo; this.hi = hi;
 *   }
 *   public E getRawResult() { return result.get(); }
 *   public void compute() { // similar to ForEach version 3
 *     int l = lo, h = hi;
 *     while (result.get() == null && h >= l) {
 *       if (h - l >= 2) {
 *         int mid = (l + h) >>> 1;
 *         addToPendingCount(1);
 *         new Searcher(this, array, result, mid, h).fork();
 *         h = mid;
 *       }
 *       else {
 *         E x = array[l];
 *         if (matches(x) && result.compareAndSet(null, x))
 *           quietlyCompleteRoot(); // root task is now joinable
 *         break;
 *       }
 *     }
 *     tryComplete(); // normally complete whether or not found
 *   }
 *   boolean matches(E e) { ... } // return true if found
 *
 *   public static <E> E search(E[] array) {
 *       return new Searcher<E>(null, array, new AtomicReference<E>(), 0, array.length).invoke();
 *   }
 * }}</pre>
 *
 * In this example, as well as others in which tasks have no other
 * effects except to {@code compareAndSet} a common result, the
 * trailing unconditional invocation of {@code tryComplete} could be
 * made conditional ({@code if (result.get() == null) tryComplete();})
 * because no further bookkeeping is required to manage completions
 * once the root task completes.
 *
 * <p><b>Recording subtasks.</b> CountedCompleter tasks that combine
 * results of multiple subtasks usually need to access these results
 * in method {@link #onCompletion(CountedCompleter)}. As illustrated in the following
 * class (that performs a simplified form of map-reduce where mappings
 * and reductions are all of type {@code E}), one way to do this in
 * divide and conquer designs is to have each subtask record its
 * sibling, so that it can be accessed in method {@code onCompletion}.
 * This technique applies to reductions in which the order of
 * combining left and right results does not matter; ordered
 * reductions require explicit left/right designations.  Variants of
 * other streamlinings seen in the above examples may also apply.
 *
 * <pre> {@code
 * class MyMapper<E> { E apply(E v) {  ...  } }
 * class MyReducer<E> { E apply(E x, E y) {  ...  } }
 * class MapReducer<E> extends CountedCompleter<E> {
 *   final E[] array; final MyMapper<E> mapper;
 *   final MyReducer<E> reducer; final int lo, hi;
 *   MapReducer<E> sibling;
 *   E result;
 *   MapReducer(CountedCompleter<?> p, E[] array, MyMapper<E> mapper,
 *              MyReducer<E> reducer, int lo, int hi) {
 *     super(p);
 *     this.array = array; this.mapper = mapper;
 *     this.reducer = reducer; this.lo = lo; this.hi = hi;
 *   }
 *   public void compute() {
 *     if (hi - lo >= 2) {
 *       int mid = (lo + hi) >>> 1;
 *       MapReducer<E> left = new MapReducer(this, array, mapper, reducer, lo, mid);
 *       MapReducer<E> right = new MapReducer(this, array, mapper, reducer, mid, hi);
 *       left.sibling = right;
 *       right.sibling = left;
 *       setPendingCount(1); // only right is pending
 *       right.fork();
 *       left.compute();     // directly execute left
 *     }
 *     else {
 *       if (hi > lo)
 *           result = mapper.apply(array[lo]);
 *       tryComplete();
 *     }
 *   }
 *   public void onCompletion(CountedCompleter<?> caller) {
 *     if (caller != this) {
 *       MapReducer<E> child = (MapReducer<E>)caller;
 *       MapReducer<E> sib = child.sibling;
 *       if (sib == null || sib.result == null)
 *         result = child.result;
 *       else
 *         result = reducer.apply(child.result, sib.result);
 *     }
 *   }
 *   public E getRawResult() { return result; }
 *
 *   public static <E> E mapReduce(E[] array, MyMapper<E> mapper, MyReducer<E> reducer) {
 *     return new MapReducer<E>(null, array, mapper, reducer,
 *                              0, array.length).invoke();
 *   }
 * }}</pre>
 *
 * Here, method {@code onCompletion} takes a form common to many
 * completion designs that combine results. This callback-style method
 * is triggered once per task, in either of the two different contexts
 * in which the pending count is, or becomes, zero: (1) by a task
 * itself, if its pending count is zero upon invocation of {@code
 * tryComplete}, or (2) by any of its subtasks when they complete and
 * decrement the pending count to zero. The {@code caller} argument
 * distinguishes cases.  Most often, when the caller is {@code this},
 * no action is necessary. Otherwise the caller argument can be used
 * (usually via a cast) to supply a value (and/or links to other
 * values) to be combined.  Assuming proper use of pending counts, the
 * actions inside {@code onCompletion} occur (once) upon completion of
 * a task and its subtasks. No additional synchronization is required
 * within this method to ensure thread safety of accesses to fields of
 * this task or other completed tasks.
 *
 * <p><b>Completion Traversals.</b> If using {@code onCompletion} to
 * process completions is inapplicable or inconvenient, you can use
 * methods {@link #firstComplete} and {@link #nextComplete} to create
 * custom traversals.  For example, to define a MapReducer that only
 * splits out right-hand tasks in the form of the third ForEach
 * example, the completions must cooperatively reduce along
 * unexhausted subtask links, which can be done as follows:
 *
 * <pre> {@code
 * class MapReducer<E> extends CountedCompleter<E> { // version 2
 *   final E[] array; final MyMapper<E> mapper;
 *   final MyReducer<E> reducer; final int lo, hi;
 *   MapReducer<E> forks, next; // record subtask forks in list
 *   E result;
 *   MapReducer(CountedCompleter<?> p, E[] array, MyMapper<E> mapper,
 *              MyReducer<E> reducer, int lo, int hi, MapReducer<E> next) {
 *     super(p);
 *     this.array = array; this.mapper = mapper;
 *     this.reducer = reducer; this.lo = lo; this.hi = hi;
 *     this.next = next;
 *   }
 *   public void compute() {
 *     int l = lo, h = hi;
 *     while (h - l >= 2) {
 *       int mid = (l + h) >>> 1;
 *       addToPendingCount(1);
 *       (forks = new MapReducer(this, array, mapper, reducer, mid, h, forks)).fork();
 *       h = mid;
 *     }
 *     if (h > l)
 *       result = mapper.apply(array[l]);
 *     // process completions by reducing along and advancing subtask links
 *     for (CountedCompleter<?> c = firstComplete(); c != null; c = c.nextComplete()) {
 *       for (MapReducer t = (MapReducer)c, s = t.forks; s != null; s = t.forks = s.next)
 *         t.result = reducer.apply(t.result, s.result);
 *     }
 *   }
 *   public E getRawResult() { return result; }
 *
 *   public static <E> E mapReduce(E[] array, MyMapper<E> mapper, MyReducer<E> reducer) {
 *     return new MapReducer<E>(null, array, mapper, reducer,
 *                              0, array.length, null).invoke();
 *   }
 * }}</pre>
 *
 * <p><b>Triggers.</b> Some CountedCompleters are themselves never
 * forked, but instead serve as bits of plumbing in other designs;
 * including those in which the completion of one or more async tasks
 * triggers another async task. For example:
 *
 * <pre> {@code
 * class HeaderBuilder extends CountedCompleter<...> { ... }
 * class BodyBuilder extends CountedCompleter<...> { ... }
 * class PacketSender extends CountedCompleter<...> {
 *   PacketSender(...) { super(null, 1); ... } // trigger on second completion
 *   public void compute() { } // never called
 *   public void onCompletion(CountedCompleter<?> caller) { sendPacket(); }
 * }
 * // sample use:
 * PacketSender p = new PacketSender();
 * new HeaderBuilder(p, ...).fork();
 * new BodyBuilder(p, ...).fork();}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public abstract class CountedCompleter<T> extends ForkJoinTask<T> {
    private static final long serialVersionUID = 5232453752276485070L;

    /** This task's completer, or null if none */
    final CountedCompleter<?> completer;
    /** The number of pending tasks until completion */
    volatile int pending;

    /**
     * Creates a new CountedCompleter with the given completer
     * and initial pending count.
     *
     * @param completer this task's completer, or {@code null} if none
     * @param initialPendingCount the initial pending count
     */
    protected CountedCompleter(CountedCompleter<?> completer,
                               int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    /**
     * Creates a new CountedCompleter with the given completer
     * and an initial pending count of zero.
     *
     * @param completer this task's completer, or {@code null} if none
     */
    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    /**
     * Creates a new CountedCompleter with no completer
     * and an initial pending count of zero.
     */
    protected CountedCompleter() {
        this.completer = null;
    }

    /**
     * The main computation performed by this task.
     */
    public abstract void compute();

    /**
     * Performs an action when method {@link #tryComplete} is invoked
     * and the pending count is zero, or when the unconditional
     * method {@link #complete} is invoked.  By default, this method
     * does nothing. You can distinguish cases by checking the
     * identity of the given caller argument. If not equal to {@code
     * this}, then it is typically a subtask that may contain results
     * (and/or links to other results) to combine.
     *
     * @param caller the task invoking this method (which may
     * be this task itself)
     */
    public void onCompletion(CountedCompleter<?> caller) {
    }

    /**
     * Performs an action when method {@link
     * #completeExceptionally(Throwable)} is invoked or method {@link
     * #compute} throws an exception, and this task has not already
     * otherwise completed normally. On entry to this method, this task
     * {@link ForkJoinTask#isCompletedAbnormally}.  The return value
     * of this method controls further propagation: If {@code true}
     * and this task has a completer that has not completed, then that
     * completer is also completed exceptionally, with the same
     * exception as this completer.  The default implementation of
     * this method does nothing except return {@code true}.
     *
     * @param ex the exception
     * @param caller the task invoking this method (which may
     * be this task itself)
     * @return {@code true} if this exception should be propagated to this
     * task's completer, if one exists
     */
    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
        return true;
    }

    /**
     * Returns the completer established in this task's constructor,
     * or {@code null} if none.
     *
     * @return the completer
     */
    public final CountedCompleter<?> getCompleter() {
        return completer;
    }

    /**
     * Returns the current pending count.
     *
     * @return the current pending count
     */
    public final int getPendingCount() {
        return pending;
    }

    /**
     * Sets the pending count to the given value.
     *
     * @param count the count
     */
    public final void setPendingCount(int count) {
        pending = count;
    }

    /**
     * Adds (atomically) the given value to the pending count.
     *
     * @param delta the value to add
     */
    public final void addToPendingCount(int delta) {
        PENDING.getAndAdd(this, delta);
    }

    /**
     * Sets (atomically) the pending count to the given count only if
     * it currently holds the given expected value.
     *
     * @param expected the expected value
     * @param count the new value
     * @return {@code true} if successful
     */
    public final boolean compareAndSetPendingCount(int expected, int count) {
        return PENDING.compareAndSet(this, expected, count);
    }

    // internal-only weak version
    final boolean weakCompareAndSetPendingCount(int expected, int count) {
        return PENDING.weakCompareAndSet(this, expected, count);
    }

    /**
     * If the pending count is nonzero, (atomically) decrements it.
     *
     * @return the initial (undecremented) pending count holding on entry
     * to this method
     */
    public final int decrementPendingCountUnlessZero() {
        int c;
        do {} while ((c = pending) != 0 &&
                     !weakCompareAndSetPendingCount(c, c - 1));
        return c;
    }

    /**
     * Returns the root of the current computation; i.e., this
     * task if it has no completer, else its completer's root.
     *
     * @return the root of the current computation
     */
    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> a = this, p;
        while ((p = a.completer) != null)
            a = p;
        return a;
    }

    /**
     * If the pending count is nonzero, decrements the count;
     * otherwise invokes {@link #onCompletion(CountedCompleter)}
     * and then similarly tries to complete this task's completer,
     * if one exists, else marks this task as complete.
     */
    public final void tryComplete() {
        CountedCompleter<?> a = this, s = a;
        for (int c;;) {
            if ((c = a.pending) == 0) {
                a.onCompletion(s);
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            }
            else if (a.weakCompareAndSetPendingCount(c, c - 1))
                return;
        }
    }

    /**
     * Equivalent to {@link #tryComplete} but does not invoke {@link
     * #onCompletion(CountedCompleter)} along the completion path:
     * If the pending count is nonzero, decrements the count;
     * otherwise, similarly tries to complete this task's completer, if
     * one exists, else marks this task as complete. This method may be
     * useful in cases where {@code onCompletion} should not, or need
     * not, be invoked for each completer in a computation.
     */
    public final void propagateCompletion() {
        CountedCompleter<?> a = this, s;
        for (int c;;) {
            if ((c = a.pending) == 0) {
                if ((a = (s = a).completer) == null) {
                    s.quietlyComplete();
                    return;
                }
            }
            else if (a.weakCompareAndSetPendingCount(c, c - 1))
                return;
        }
    }

    /**
     * Regardless of pending count, invokes
     * {@link #onCompletion(CountedCompleter)}, marks this task as
     * complete and further triggers {@link #tryComplete} on this
     * task's completer, if one exists.  The given rawResult is
     * used as an argument to {@link #setRawResult} before invoking
     * {@link #onCompletion(CountedCompleter)} or marking this task
     * as complete; its value is meaningful only for classes
     * overriding {@code setRawResult}.  This method does not modify
     * the pending count.
     *
     * <p>This method may be useful when forcing completion as soon as
     * any one (versus all) of several subtask results are obtained.
     * However, in the common (and recommended) case in which {@code
     * setRawResult} is not overridden, this effect can be obtained
     * more simply using {@link #quietlyCompleteRoot()}.
     *
     * @param rawResult the raw result
     */
    public void complete(T rawResult) {
        CountedCompleter<?> p;
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        if ((p = completer) != null)
            p.tryComplete();
    }

    /**
     * If this task's pending count is zero, returns this task;
     * otherwise decrements its pending count and returns {@code null}.
     * This method is designed to be used with {@link #nextComplete} in
     * completion traversal loops.
     *
     * @return this task, if pending count was zero, else {@code null}
     */
    public final CountedCompleter<?> firstComplete() {
        for (int c;;) {
            if ((c = pending) == 0)
                return this;
            else if (weakCompareAndSetPendingCount(c, c - 1))
                return null;
        }
    }

    /**
     * If this task does not have a completer, invokes {@link
     * ForkJoinTask#quietlyComplete} and returns {@code null}.  Or, if
     * the completer's pending count is non-zero, decrements that
     * pending count and returns {@code null}.  Otherwise, returns the
     * completer.  This method can be used as part of a completion
     * traversal loop for homogeneous task hierarchies:
     *
     * <pre> {@code
     * for (CountedCompleter<?> c = firstComplete();
     *      c != null;
     *      c = c.nextComplete()) {
     *   // ... process c ...
     * }}</pre>
     *
     * @return the completer, or {@code null} if none
     */
    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> p;
        if ((p = completer) != null)
            return p.firstComplete();
        else {
            quietlyComplete();
            return null;
        }
    }

    /**
     * Equivalent to {@code getRoot().quietlyComplete()}.
     */
    public final void quietlyCompleteRoot() {
        for (CountedCompleter<?> a = this, p;;) {
            if ((p = a.completer) == null) {
                a.quietlyComplete();
                return;
            }
            a = p;
        }
    }

    /**
     * If this task has not completed, attempts to process at most the
     * given number of other unprocessed tasks for which this task is
     * on the completion path, if any are known to exist.
     *
     * @param maxTasks the maximum number of tasks to process.  If
     *                 less than or equal to zero, then no tasks are
     *                 processed.
     */
    public final void helpComplete(int maxTasks) {
        ForkJoinPool.WorkQueue q; Thread t; boolean owned;
        if (owned = (t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            q = ((ForkJoinWorkerThread)t).workQueue;
        else
            q = ForkJoinPool.commonQueue();
        if (q != null && maxTasks > 0)
            q.helpComplete(this, owned, maxTasks);
    }

    // ForkJoinTask overrides

    /**
     * Supports ForkJoinTask exception propagation.
     */
    @Override
    final int trySetException(Throwable ex) {
        CountedCompleter<?> a = this, p = a;
        do {} while (isExceptionalStatus(a.trySetThrown(ex)) &&
                     a.onExceptionalCompletion(ex, p) &&
                     (a = (p = a).completer) != null && a.status >= 0);
        return status;
    }

    /**
     * Implements execution conventions for CountedCompleters.
     */
    @Override
    protected final boolean exec() {
        compute();
        return false;
    }

    /**
     * Returns the result of the computation.  By default,
     * returns {@code null}, which is appropriate for {@code Void}
     * actions, but in other cases should be overridden, almost
     * always to return a field or function of a field that
     * holds the result upon completion.
     *
     * @return the result of the computation
     */
    @Override
    public T getRawResult() { return null; }

    /**
     * A method that result-bearing CountedCompleters may optionally
     * use to help maintain result data.  By default, does nothing.
     * Overrides are not recommended. However, if this method is
     * overridden to update existing objects or fields, then it must
     * in general be defined to be thread-safe.
     */
    @Override
    protected void setRawResult(T t) { }

    // VarHandle mechanics
    private static final VarHandle PENDING;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            PENDING = l.findVarHandle(CountedCompleter.class, "pending", int.class);

        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
