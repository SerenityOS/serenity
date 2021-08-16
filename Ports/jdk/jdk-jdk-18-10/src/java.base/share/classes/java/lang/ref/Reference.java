/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import jdk.internal.access.JavaLangRefAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.ref.Cleaner;

/**
 * Abstract base class for reference objects.  This class defines the
 * operations common to all reference objects.  Because reference objects are
 * implemented in close cooperation with the garbage collector, this class may
 * not be subclassed directly.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public abstract class Reference<T> {

    /* The state of a Reference object is characterized by two attributes.  It
     * may be either "active", "pending", or "inactive".  It may also be
     * either "registered", "enqueued", "dequeued", or "unregistered".
     *
     *   Active: Subject to special treatment by the garbage collector.  Some
     *   time after the collector detects that the reachability of the
     *   referent has changed to the appropriate state, the collector
     *   "notifies" the reference, changing the state to either "pending" or
     *   "inactive".
     *   referent != null; discovered = null, or in GC discovered list.
     *
     *   Pending: An element of the pending-Reference list, waiting to be
     *   processed by the ReferenceHandler thread.  The pending-Reference
     *   list is linked through the discovered fields of references in the
     *   list.
     *   referent = null; discovered = next element in pending-Reference list.
     *
     *   Inactive: Neither Active nor Pending.
     *   referent = null.
     *
     *   Registered: Associated with a queue when created, and not yet added
     *   to the queue.
     *   queue = the associated queue.
     *
     *   Enqueued: Added to the associated queue, and not yet removed.
     *   queue = ReferenceQueue.ENQUEUE; next = next entry in list, or this to
     *   indicate end of list.
     *
     *   Dequeued: Added to the associated queue and then removed.
     *   queue = ReferenceQueue.NULL; next = this.
     *
     *   Unregistered: Not associated with a queue when created.
     *   queue = ReferenceQueue.NULL.
     *
     * The collector only needs to examine the referent field and the
     * discovered field to determine whether a (non-FinalReference) Reference
     * object needs special treatment.  If the referent is non-null and not
     * known to be live, then it may need to be discovered for possible later
     * notification.  But if the discovered field is non-null, then it has
     * already been discovered.
     *
     * FinalReference (which exists to support finalization) differs from
     * other references, because a FinalReference is not cleared when
     * notified.  The referent being null or not cannot be used to distinguish
     * between the active state and pending or inactive states.  However,
     * FinalReferences do not support enqueue().  Instead, the next field of a
     * FinalReference object is set to "this" when it is added to the
     * pending-Reference list.  The use of "this" as the value of next in the
     * enqueued and dequeued states maintains the non-active state.  An
     * additional check that the next field is null is required to determine
     * that a FinalReference object is active.
     *
     * Initial states:
     *   [active/registered]
     *   [active/unregistered] [1]
     *
     * Transitions:
     *                            clear [2]
     *   [active/registered]     ------->   [inactive/registered]
     *          |                                 |
     *          |                                 | enqueue
     *          | GC              enqueue [2]     |
     *          |                -----------------|
     *          |                                 |
     *          v                                 |
     *   [pending/registered]    ---              v
     *          |                   | ReferenceHandler
     *          | enqueue [2]       |--->   [inactive/enqueued]
     *          v                   |             |
     *   [pending/enqueued]      ---              |
     *          |                                 | poll/remove
     *          | poll/remove                     | + clear [4]
     *          |                                 |
     *          v            ReferenceHandler     v
     *   [pending/dequeued]      ------>    [inactive/dequeued]
     *
     *
     *                           clear/enqueue/GC [3]
     *   [active/unregistered]   ------
     *          |                      |
     *          | GC                   |
     *          |                      |--> [inactive/unregistered]
     *          v                      |
     *   [pending/unregistered]  ------
     *                           ReferenceHandler
     *
     * Terminal states:
     *   [inactive/dequeued]
     *   [inactive/unregistered]
     *
     * Unreachable states (because enqueue also clears):
     *   [active/enqeued]
     *   [active/dequeued]
     *
     * [1] Unregistered is not permitted for FinalReferences.
     *
     * [2] These transitions are not possible for FinalReferences, making
     * [pending/enqueued], [pending/dequeued], and [inactive/registered]
     * unreachable.
     *
     * [3] The garbage collector may directly transition a Reference
     * from [active/unregistered] to [inactive/unregistered],
     * bypassing the pending-Reference list.
     *
     * [4] The queue handler for FinalReferences also clears the reference.
     */

    private T referent;         /* Treated specially by GC */

    /* The queue this reference gets enqueued to by GC notification or by
     * calling enqueue().
     *
     * When registered: the queue with which this reference is registered.
     *        enqueued: ReferenceQueue.ENQUEUE
     *        dequeued: ReferenceQueue.NULL
     *    unregistered: ReferenceQueue.NULL
     */
    volatile ReferenceQueue<? super T> queue;

    /* The link in a ReferenceQueue's list of Reference objects.
     *
     * When registered: null
     *        enqueued: next element in queue (or this if last)
     *        dequeued: this (marking FinalReferences as inactive)
     *    unregistered: null
     */
    @SuppressWarnings("rawtypes")
    volatile Reference next;

    /* Used by the garbage collector to accumulate Reference objects that need
     * to be revisited in order to decide whether they should be notified.
     * Also used as the link in the pending-Reference list.  The discovered
     * field and the next field are distinct to allow the enqueue() method to
     * be applied to a Reference object while it is either in the
     * pending-Reference list or in the garbage collector's discovered set.
     *
     * When active: null or next element in a discovered reference list
     *              maintained by the GC (or this if last)
     *     pending: next element in the pending-Reference list (null if last)
     *    inactive: null
     */
    private transient Reference<?> discovered;


    /* High-priority thread to enqueue pending References
     */
    private static class ReferenceHandler extends Thread {

        private static void ensureClassInitialized(Class<?> clazz) {
            try {
                Class.forName(clazz.getName(), true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw (Error) new NoClassDefFoundError(e.getMessage()).initCause(e);
            }
        }

        static {
            // pre-load and initialize Cleaner class so that we don't
            // get into trouble later in the run loop if there's
            // memory shortage while loading/initializing it lazily.
            ensureClassInitialized(Cleaner.class);
        }

        ReferenceHandler(ThreadGroup g, String name) {
            super(g, null, name, 0, false);
        }

        public void run() {
            while (true) {
                processPendingReferences();
            }
        }
    }

    /*
     * Atomically get and clear (set to null) the VM's pending-Reference list.
     */
    private static native Reference<?> getAndClearReferencePendingList();

    /*
     * Test whether the VM's pending-Reference list contains any entries.
     */
    private static native boolean hasReferencePendingList();

    /*
     * Wait until the VM's pending-Reference list may be non-null.
     */
    private static native void waitForReferencePendingList();

    /*
     * Enqueue a Reference taken from the pending list.  Calling this method
     * takes us from the Reference<?> domain of the pending list elements to
     * having a Reference<T> with a correspondingly typed queue.
     */
    private void enqueueFromPending() {
        var q = queue;
        if (q != ReferenceQueue.NULL) q.enqueue(this);
    }

    private static final Object processPendingLock = new Object();
    private static boolean processPendingActive = false;

    private static void processPendingReferences() {
        // Only the singleton reference processing thread calls
        // waitForReferencePendingList() and getAndClearReferencePendingList().
        // These are separate operations to avoid a race with other threads
        // that are calling waitForReferenceProcessing().
        waitForReferencePendingList();
        Reference<?> pendingList;
        synchronized (processPendingLock) {
            pendingList = getAndClearReferencePendingList();
            processPendingActive = true;
        }
        while (pendingList != null) {
            Reference<?> ref = pendingList;
            pendingList = ref.discovered;
            ref.discovered = null;

            if (ref instanceof Cleaner) {
                ((Cleaner)ref).clean();
                // Notify any waiters that progress has been made.
                // This improves latency for nio.Bits waiters, which
                // are the only important ones.
                synchronized (processPendingLock) {
                    processPendingLock.notifyAll();
                }
            } else {
                ref.enqueueFromPending();
            }
        }
        // Notify any waiters of completion of current round.
        synchronized (processPendingLock) {
            processPendingActive = false;
            processPendingLock.notifyAll();
        }
    }

    // Wait for progress in reference processing.
    //
    // Returns true after waiting (for notification from the reference
    // processing thread) if either (1) the VM has any pending
    // references, or (2) the reference processing thread is
    // processing references. Otherwise, returns false immediately.
    private static boolean waitForReferenceProcessing()
        throws InterruptedException
    {
        synchronized (processPendingLock) {
            if (processPendingActive || hasReferencePendingList()) {
                // Wait for progress, not necessarily completion.
                processPendingLock.wait();
                return true;
            } else {
                return false;
            }
        }
    }

    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent());
        Thread handler = new ReferenceHandler(tg, "Reference Handler");
        /* If there were a special system-only priority greater than
         * MAX_PRIORITY, it would be used here
         */
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);
        handler.start();

        // provide access in SharedSecrets
        SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
            @Override
            public boolean waitForReferenceProcessing()
                throws InterruptedException
            {
                return Reference.waitForReferenceProcessing();
            }

            @Override
            public void runFinalization() {
                Finalizer.runFinalization();
            }
        });
    }

    /* -- Referent accessor and setters -- */

    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, either by the program or by the garbage collector, then
     * this method returns {@code null}.
     *
     * @apiNote
     * This method returns a strong reference to the referent. This may cause
     * the garbage collector to treat it as strongly reachable until some later
     * collection cycle.  The {@link #refersTo(Object) refersTo} method can be
     * used to avoid such strengthening when testing whether some object is
     * the referent of a reference object; that is, use {@code ref.refersTo(obj)}
     * rather than {@code ref.get() == obj}.
     *
     * @return   The object to which this reference refers, or
     *           {@code null} if this reference object has been cleared
     * @see #refersTo
     */
    @IntrinsicCandidate
    public T get() {
        return this.referent;
    }

    /**
     * Tests if the referent of this reference object is {@code obj}.
     * Using a {@code null} {@code obj} returns {@code true} if the
     * reference object has been cleared.
     *
     * @param  obj the object to compare with this reference object's referent
     * @return {@code true} if {@code obj} is the referent of this reference object
     * @since 16
     */
    public final boolean refersTo(T obj) {
        return refersTo0(obj);
    }

    /* Implementation of refersTo(), overridden for phantom references.
     */
    @IntrinsicCandidate
    native boolean refersTo0(Object o);

    /**
     * Clears this reference object.  Invoking this method will not cause this
     * object to be enqueued.
     *
     * <p> This method is invoked only by Java code; when the garbage collector
     * clears references it does so directly, without invoking this method.
     */
    public void clear() {
        clear0();
    }

    /* Implementation of clear(), also used by enqueue().  A simple
     * assignment of the referent field won't do for some garbage
     * collectors.
     */
    private native void clear0();

    /* -- Operations on inactive FinalReferences -- */

    /* These functions are only used by FinalReference, and must only be
     * called after the reference becomes inactive. While active, a
     * FinalReference is considered weak but the referent is not normally
     * accessed. Once a FinalReference becomes inactive it is considered a
     * strong reference. These functions are used to bypass the
     * corresponding weak implementations, directly accessing the referent
     * field with strong semantics.
     */

    /**
     * Load referent with strong semantics.
     */
    T getFromInactiveFinalReference() {
        assert this instanceof FinalReference;
        assert next != null; // I.e. FinalReference is inactive
        return this.referent;
    }

    /**
     * Clear referent with strong semantics.
     */
    void clearInactiveFinalReference() {
        assert this instanceof FinalReference;
        assert next != null; // I.e. FinalReference is inactive
        this.referent = null;
    }

    /* -- Queue operations -- */

    /**
     * Tests if this reference object is in its associated queue, if any.
     * This method returns {@code true} only if all of the following conditions
     * are met:
     * <ul>
     * <li>this reference object was registered with a queue when it was created; and
     * <li>the garbage collector has added this reference object to the queue
     *     or {@link #enqueue()} is called; and
     * <li>this reference object is not yet removed from the queue.
     * </ul>
     * Otherwise, this method returns {@code false}.
     * This method may return {@code false} if this reference object has been cleared
     * but not enqueued due to the race condition.
     *
     * @deprecated
     * This method was originally specified to test if a reference object has
     * been cleared and enqueued but was never implemented to do this test.
     * This method could be misused due to the inherent race condition
     * or without an associated {@code ReferenceQueue}.
     * An application relying on this method to release critical resources
     * could cause serious performance issue.
     * An application should use {@link ReferenceQueue} to reliably determine
     * what reference objects that have been enqueued or
     * {@link #refersTo(Object) refersTo(null)} to determine if this reference
     * object has been cleared.
     *
     * @return   {@code true} if and only if this reference object is
     *           in its associated queue (if any).
     */
    @Deprecated(since="16")
    public boolean isEnqueued() {
        return (this.queue == ReferenceQueue.ENQUEUED);
    }

    /**
     * Clears this reference object and adds it to the queue with which
     * it is registered, if any.
     *
     * <p> This method is invoked only by Java code; when the garbage collector
     * enqueues references it does so directly, without invoking this method.
     *
     * @return   {@code true} if this reference object was successfully
     *           enqueued; {@code false} if it was already enqueued or if
     *           it was not registered with a queue when it was created
     */
    public boolean enqueue() {
        clear0();               // Intentionally clear0() rather than clear()
        return this.queue.enqueue(this);
    }

    /**
     * Throws {@link CloneNotSupportedException}. A {@code Reference} cannot be
     * meaningfully cloned. Construct a new {@code Reference} instead.
     *
     * @return never returns normally
     * @throws  CloneNotSupportedException always
     *
     * @since 11
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /* -- Constructors -- */

    Reference(T referent) {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }

    /**
     * Ensures that the object referenced by the given reference remains
     * <a href="package-summary.html#reachability"><em>strongly reachable</em></a>,
     * regardless of any prior actions of the program that might otherwise cause
     * the object to become unreachable; thus, the referenced object is not
     * reclaimable by garbage collection at least until after the invocation of
     * this method.  Invocation of this method does not itself initiate garbage
     * collection or finalization.
     *
     * <p> This method establishes an ordering for
     * <a href="package-summary.html#reachability"><em>strong reachability</em></a>
     * with respect to garbage collection.  It controls relations that are
     * otherwise only implicit in a program -- the reachability conditions
     * triggering garbage collection.  This method is designed for use in
     * uncommon situations of premature finalization where using
     * {@code synchronized} blocks or methods, or using other synchronization
     * facilities are not possible or do not provide the desired control.  This
     * method is applicable only when reclamation may have visible effects,
     * which is possible for objects with finalizers (See Section {@jls 12.6}
     * of <cite>The Java Language Specification</cite>) that
     * are implemented in ways that rely on ordering control for
     * correctness.
     *
     * @apiNote
     * Finalization may occur whenever the virtual machine detects that no
     * reference to an object will ever be stored in the heap: The garbage
     * collector may reclaim an object even if the fields of that object are
     * still in use, so long as the object has otherwise become unreachable.
     * This may have surprising and undesirable effects in cases such as the
     * following example in which the bookkeeping associated with a class is
     * managed through array indices.  Here, method {@code action} uses a
     * {@code reachabilityFence} to ensure that the {@code Resource} object is
     * not reclaimed before bookkeeping on an associated
     * {@code ExternalResource} has been performed; in particular here, to
     * ensure that the array slot holding the {@code ExternalResource} is not
     * nulled out in method {@link Object#finalize}, which may otherwise run
     * concurrently.
     *
     * <pre> {@code
     * class Resource {
     *   private static ExternalResource[] externalResourceArray = ...
     *
     *   int myIndex;
     *   Resource(...) {
     *     myIndex = ...
     *     externalResourceArray[myIndex] = ...;
     *     ...
     *   }
     *   protected void finalize() {
     *     externalResourceArray[myIndex] = null;
     *     ...
     *   }
     *   public void action() {
     *     try {
     *       // ...
     *       int i = myIndex;
     *       Resource.update(externalResourceArray[i]);
     *     } finally {
     *       Reference.reachabilityFence(this);
     *     }
     *   }
     *   private static void update(ExternalResource ext) {
     *     ext.status = ...;
     *   }
     * }}</pre>
     *
     * Here, the invocation of {@code reachabilityFence} is nonintuitively
     * placed <em>after</em> the call to {@code update}, to ensure that the
     * array slot is not nulled out by {@link Object#finalize} before the
     * update, even if the call to {@code action} was the last use of this
     * object.  This might be the case if, for example a usage in a user program
     * had the form {@code new Resource().action();} which retains no other
     * reference to this {@code Resource}.  While probably overkill here,
     * {@code reachabilityFence} is placed in a {@code finally} block to ensure
     * that it is invoked across all paths in the method.  In a method with more
     * complex control paths, you might need further precautions to ensure that
     * {@code reachabilityFence} is encountered along all of them.
     *
     * <p> It is sometimes possible to better encapsulate use of
     * {@code reachabilityFence}.  Continuing the above example, if it were
     * acceptable for the call to method {@code update} to proceed even if the
     * finalizer had already executed (nulling out slot), then you could
     * localize use of {@code reachabilityFence}:
     *
     * <pre> {@code
     * public void action2() {
     *   // ...
     *   Resource.update(getExternalResource());
     * }
     * private ExternalResource getExternalResource() {
     *   ExternalResource ext = externalResourceArray[myIndex];
     *   Reference.reachabilityFence(this);
     *   return ext;
     * }}</pre>
     *
     * <p> Method {@code reachabilityFence} is not required in constructions
     * that themselves ensure reachability.  For example, because objects that
     * are locked cannot, in general, be reclaimed, it would suffice if all
     * accesses of the object, in all methods of class {@code Resource}
     * (including {@code finalize}) were enclosed in {@code synchronized (this)}
     * blocks.  (Further, such blocks must not include infinite loops, or
     * themselves be unreachable, which fall into the corner case exceptions to
     * the "in general" disclaimer.)  However, method {@code reachabilityFence}
     * remains a better option in cases where this approach is not as efficient,
     * desirable, or possible; for example because it would encounter deadlock.
     *
     * @param ref the reference. If {@code null}, this method has no effect.
     * @since 9
     * @jls 12.6 Finalization of Class Instances
     */
    @ForceInline
    public static void reachabilityFence(Object ref) {
        // Does nothing. This method is annotated with @ForceInline to eliminate
        // most of the overhead that using @DontInline would cause with the
        // HotSpot JVM, when this fence is used in a wide variety of situations.
        // HotSpot JVM retains the ref and does not GC it before a call to
        // this method, because the JIT-compilers do not have GC-only safepoints.
    }
}
