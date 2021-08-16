/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import java.util.Spliterator;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Abstract base class for most fork-join tasks used to implement stream ops.
 * Manages splitting logic, tracking of child tasks, and intermediate results.
 * Each task is associated with a {@link Spliterator} that describes the portion
 * of the input associated with the subtree rooted at this task.
 * Tasks may be leaf nodes (which will traverse the elements of
 * the {@code Spliterator}) or internal nodes (which split the
 * {@code Spliterator} into multiple child tasks).
 *
 * @implNote
 * <p>This class is based on {@link CountedCompleter}, a form of fork-join task
 * where each task has a semaphore-like count of uncompleted children, and the
 * task is implicitly completed and notified when its last child completes.
 * Internal node tasks will likely override the {@code onCompletion} method from
 * {@code CountedCompleter} to merge the results from child tasks into the
 * current task's result.
 *
 * <p>Splitting and setting up the child task links is done by {@code compute()}
 * for internal nodes.  At {@code compute()} time for leaf nodes, it is
 * guaranteed that the parent's child-related fields (including sibling links
 * for the parent's children) will be set up for all children.
 *
 * <p>For example, a task that performs a reduce would override {@code doLeaf()}
 * to perform a reduction on that leaf node's chunk using the
 * {@code Spliterator}, and override {@code onCompletion()} to merge the results
 * of the child tasks for internal nodes:
 *
 * <pre>{@code
 *     protected S doLeaf() {
 *         spliterator.forEach(...);
 *         return localReductionResult;
 *     }
 *
 *     public void onCompletion(CountedCompleter caller) {
 *         if (!isLeaf()) {
 *             ReduceTask<P_IN, P_OUT, T, R> child = children;
 *             R result = child.getLocalResult();
 *             child = child.nextSibling;
 *             for (; child != null; child = child.nextSibling)
 *                 result = combine(result, child.getLocalResult());
 *             setLocalResult(result);
 *         }
 *     }
 * }</pre>
 *
 * <p>Serialization is not supported as there is no intention to serialize
 * tasks managed by stream ops.
 *
 * @param <P_IN> Type of elements input to the pipeline
 * @param <P_OUT> Type of elements output from the pipeline
 * @param <R> Type of intermediate result, which may be different from operation
 *        result type
 * @param <K> Type of parent, child and sibling tasks
 * @since 1.8
 */
@SuppressWarnings("serial")
abstract class AbstractTask<P_IN, P_OUT, R,
                            K extends AbstractTask<P_IN, P_OUT, R, K>>
        extends CountedCompleter<R> {

    private static final int LEAF_TARGET = ForkJoinPool.getCommonPoolParallelism() << 2;

    /** The pipeline helper, common to all tasks in a computation */
    protected final PipelineHelper<P_OUT> helper;

    /**
     * The spliterator for the portion of the input associated with the subtree
     * rooted at this task
     */
    protected Spliterator<P_IN> spliterator;

    /** Target leaf size, common to all tasks in a computation */
    protected long targetSize; // may be lazily initialized

    /**
     * The left child.
     * null if no children
     * if non-null rightChild is non-null
     */
    protected K leftChild;

    /**
     * The right child.
     * null if no children
     * if non-null leftChild is non-null
     */
    protected K rightChild;

    /** The result of this node, if completed */
    private R localResult;

    /**
     * Constructor for root nodes.
     *
     * @param helper The {@code PipelineHelper} describing the stream pipeline
     *               up to this operation
     * @param spliterator The {@code Spliterator} describing the source for this
     *                    pipeline
     */
    protected AbstractTask(PipelineHelper<P_OUT> helper,
                           Spliterator<P_IN> spliterator) {
        super(null);
        this.helper = helper;
        this.spliterator = spliterator;
        this.targetSize = 0L;
    }

    /**
     * Constructor for non-root nodes.
     *
     * @param parent this node's parent task
     * @param spliterator {@code Spliterator} describing the subtree rooted at
     *        this node, obtained by splitting the parent {@code Spliterator}
     */
    protected AbstractTask(K parent,
                           Spliterator<P_IN> spliterator) {
        super(parent);
        this.spliterator = spliterator;
        this.helper = parent.helper;
        this.targetSize = parent.targetSize;
    }

    /**
     * Default target of leaf tasks for parallel decomposition.
     * To allow load balancing, we over-partition, currently to approximately
     * four tasks per processor, which enables others to help out
     * if leaf tasks are uneven or some processors are otherwise busy.
     */
    public static int getLeafTarget() {
        Thread t = Thread.currentThread();
        if (t instanceof ForkJoinWorkerThread) {
            return ((ForkJoinWorkerThread) t).getPool().getParallelism() << 2;
        }
        else {
            return LEAF_TARGET;
        }
    }

    /**
     * Constructs a new node of type T whose parent is the receiver; must call
     * the AbstractTask(T, Spliterator) constructor with the receiver and the
     * provided Spliterator.
     *
     * @param spliterator {@code Spliterator} describing the subtree rooted at
     *        this node, obtained by splitting the parent {@code Spliterator}
     * @return newly constructed child node
     */
    protected abstract K makeChild(Spliterator<P_IN> spliterator);

    /**
     * Computes the result associated with a leaf node.  Will be called by
     * {@code compute()} and the result passed to @{code setLocalResult()}
     *
     * @return the computed result of a leaf node
     */
    protected abstract R doLeaf();

    /**
     * Returns a suggested target leaf size based on the initial size estimate.
     *
     * @return suggested target leaf size
     */
    public static long suggestTargetSize(long sizeEstimate) {
        long est = sizeEstimate / getLeafTarget();
        return est > 0L ? est : 1L;
    }

    /**
     * Returns the targetSize, initializing it via the supplied
     * size estimate if not already initialized.
     */
    protected final long getTargetSize(long sizeEstimate) {
        long s;
        return ((s = targetSize) != 0 ? s :
                (targetSize = suggestTargetSize(sizeEstimate)));
    }

    /**
     * Returns the local result, if any. Subclasses should use
     * {@link #setLocalResult(Object)} and {@link #getLocalResult()} to manage
     * results.  This returns the local result so that calls from within the
     * fork-join framework will return the correct result.
     *
     * @return local result for this node previously stored with
     * {@link #setLocalResult}
     */
    @Override
    public R getRawResult() {
        return localResult;
    }

    /**
     * Does nothing; instead, subclasses should use
     * {@link #setLocalResult(Object)}} to manage results.
     *
     * @param result must be null, or an exception is thrown (this is a safety
     *        tripwire to detect when {@code setRawResult()} is being used
     *        instead of {@code setLocalResult()}
     */
    @Override
    protected void setRawResult(R result) {
        if (result != null)
            throw new IllegalStateException();
    }

    /**
     * Retrieves a result previously stored with {@link #setLocalResult}
     *
     * @return local result for this node previously stored with
     * {@link #setLocalResult}
     */
    protected R getLocalResult() {
        return localResult;
    }

    /**
     * Associates the result with the task, can be retrieved with
     * {@link #getLocalResult}
     *
     * @param localResult local result for this node
     */
    protected void setLocalResult(R localResult) {
        this.localResult = localResult;
    }

    /**
     * Indicates whether this task is a leaf node.  (Only valid after
     * {@link #compute} has been called on this node).  If the node is not a
     * leaf node, then children will be non-null and numChildren will be
     * positive.
     *
     * @return {@code true} if this task is a leaf node
     */
    protected boolean isLeaf() {
        return leftChild == null;
    }

    /**
     * Indicates whether this task is the root node
     *
     * @return {@code true} if this task is the root node.
     */
    protected boolean isRoot() {
        return getParent() == null;
    }

    /**
     * Returns the parent of this task, or null if this task is the root
     *
     * @return the parent of this task, or null if this task is the root
     */
    @SuppressWarnings("unchecked")
    protected K getParent() {
        return (K) getCompleter();
    }

    /**
     * Decides whether or not to split a task further or compute it
     * directly. If computing directly, calls {@code doLeaf} and pass
     * the result to {@code setRawResult}. Otherwise splits off
     * subtasks, forking one and continuing as the other.
     *
     * <p> The method is structured to conserve resources across a
     * range of uses.  The loop continues with one of the child tasks
     * when split, to avoid deep recursion. To cope with spliterators
     * that may be systematically biased toward left-heavy or
     * right-heavy splits, we alternate which child is forked versus
     * continued in the loop.
     */
    @Override
    public void compute() {
        Spliterator<P_IN> rs = spliterator, ls; // right, left spliterators
        long sizeEstimate = rs.estimateSize();
        long sizeThreshold = getTargetSize(sizeEstimate);
        boolean forkRight = false;
        @SuppressWarnings("unchecked") K task = (K) this;
        while (sizeEstimate > sizeThreshold && (ls = rs.trySplit()) != null) {
            K leftChild, rightChild, taskToFork;
            task.leftChild  = leftChild = task.makeChild(ls);
            task.rightChild = rightChild = task.makeChild(rs);
            task.setPendingCount(1);
            if (forkRight) {
                forkRight = false;
                rs = ls;
                task = leftChild;
                taskToFork = rightChild;
            }
            else {
                forkRight = true;
                task = rightChild;
                taskToFork = leftChild;
            }
            taskToFork.fork();
            sizeEstimate = rs.estimateSize();
        }
        task.setLocalResult(task.doLeaf());
        task.tryComplete();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * Clears spliterator and children fields.  Overriders MUST call
     * {@code super.onCompletion} as the last thing they do if they want these
     * cleared.
     */
    @Override
    public void onCompletion(CountedCompleter<?> caller) {
        spliterator = null;
        leftChild = rightChild = null;
    }

    /**
     * Returns whether this node is a "leftmost" node -- whether the path from
     * the root to this node involves only traversing leftmost child links.  For
     * a leaf node, this means it is the first leaf node in the encounter order.
     *
     * @return {@code true} if this node is a "leftmost" node
     */
    protected boolean isLeftmostNode() {
        @SuppressWarnings("unchecked")
        K node = (K) this;
        while (node != null) {
            K parent = node.getParent();
            if (parent != null && parent.leftChild != node)
                return false;
            node = parent;
        }
        return true;
    }
}
