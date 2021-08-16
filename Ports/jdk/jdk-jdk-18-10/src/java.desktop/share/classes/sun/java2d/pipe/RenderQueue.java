/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.util.HashSet;
import java.util.Set;
import sun.awt.SunToolkit;

/**
 * The RenderQueue class encapsulates a RenderBuffer on which rendering
 * operations are enqueued.  Note that the RenderQueue lock must be acquired
 * before performing any operations on the queue (e.g. enqueuing an operation
 * or flushing the queue).  A sample usage scenario follows:
 *
 *     public void drawSomething(...) {
 *         rq.lock();
 *         try {
 *             ctx.validate(...);
 *             rq.ensureCapacity(4);
 *             rq.getBuffer().putInt(DRAW_SOMETHING);
 *             ...
 *         } finally {
 *             rq.unlock();
 *         }
 *     }
 *
 * If you are enqueuing an operation that involves 8-byte parameters (i.e.
 * long or double values), it is imperative that you ensure proper
 * alignment of the underlying RenderBuffer.  This can be accomplished
 * simply by providing an offset to the first 8-byte parameter in your
 * operation to the ensureCapacityAndAlignment() method.  For example:
 *
 *     public void drawStuff(...) {
 *         rq.lock();
 *         try {
 *             RenderBuffer buf = rq.getBuffer();
 *             ctx.validate(...);
 *             // 28 total bytes in the operation, 12 bytes to the first long
 *             rq.ensureCapacityAndAlignment(28, 12);
 *             buf.putInt(DRAW_STUFF);
 *             buf.putInt(x).putInt(y);
 *             buf.putLong(addr1);
 *             buf.putLong(addr2);
 *         } finally {
 *             rq.unlock();
 *         }
 *     }
 */
public abstract class RenderQueue {

    /** The size of the underlying buffer, in bytes. */
    private static final int BUFFER_SIZE = 32000;

    /** The underlying buffer for this queue. */
    protected RenderBuffer buf;

    /**
     * A Set containing hard references to Objects that must stay alive until
     * the queue has been completely flushed.
     */
    protected Set<Object> refSet;

    protected RenderQueue() {
        refSet = new HashSet<>();
        buf = RenderBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Locks the queue for read/write access.
     */
    public final void lock() {
        /*
         * Implementation note: In theory we should have two separate locks:
         * one lock to synchronize access to the RenderQueue, and then a
         * separate lock (the AWT lock) that only needs to be acquired when
         * we are about to flush the queue (using native windowing system
         * operations).  In practice it has been difficult to enforce the
         * correct lock ordering; sometimes AWT will have already acquired
         * the AWT lock before grabbing the RQ lock (see 6253009), while the
         * expected order should be RQ lock and then AWT lock.  Due to this
         * issue, using two separate locks is prone to deadlocks.  Therefore,
         * to solve this issue we have decided to eliminate the separate RQ
         * lock and instead just acquire the AWT lock here.  (Someday it might
         * be nice to go back to the old two-lock system, but that would
         * require potentially risky changes to AWT to ensure that it never
         * acquires the AWT lock before calling into 2D code that wants to
         * acquire the RQ lock.)
         */
        SunToolkit.awtLock();
    }

    /**
     * Attempts to lock the queue.  If successful, this method returns true,
     * indicating that the caller is responsible for calling
     * {@code unlock}; otherwise this method returns false.
     */
    public final boolean tryLock() {
        return SunToolkit.awtTryLock();
    }

    /**
     * Unlocks the queue.
     */
    public final void unlock() {
        SunToolkit.awtUnlock();
    }

    /**
     * Adds the given Object to the set of hard references, which will
     * prevent that Object from being disposed until the queue has been
     * flushed completely.  This is useful in cases where some enqueued
     * data could become invalid if the reference Object were garbage
     * collected before the queue could be processed.  (For example, keeping
     * a hard reference to a FontStrike will prevent any enqueued glyph
     * images associated with that strike from becoming invalid before the
     * queue is flushed.)  The reference set will be cleared immediately
     * after the queue is flushed each time.
     */
    public final void addReference(Object ref) {
        refSet.add(ref);
    }

    /**
     * Returns the encapsulated RenderBuffer object.
     */
    public final RenderBuffer getBuffer() {
        return buf;
    }

    /**
     * Ensures that there will be enough room on the underlying buffer
     * for the following operation.  If the operation will not fit given
     * the remaining space, the buffer will be flushed immediately, leaving
     * an empty buffer for the impending operation.
     *
     * @param opsize size (in bytes) of the following operation
     */
    public final void ensureCapacity(int opsize) {
        if (buf.remaining() < opsize) {
            flushNow();
        }
    }

    /**
     * Convenience method that is equivalent to calling ensureCapacity()
     * followed by ensureAlignment().  The ensureCapacity() call allows for an
     * extra 4 bytes of space in case the ensureAlignment() method needs to
     * insert a NOOP token on the buffer.
     *
     * @param opsize size (in bytes) of the following operation
     * @param first8ByteValueOffset offset (in bytes) from the current
     * position to the first 8-byte value used in the following operation
     */
    public final void ensureCapacityAndAlignment(int opsize,
                                                 int first8ByteValueOffset)
    {
        ensureCapacity(opsize + 4);
        ensureAlignment(first8ByteValueOffset);
    }

    /**
     * Inserts a 4-byte NOOP token when necessary to ensure that all 8-byte
     * parameters for the following operation are added to the underlying
     * buffer with an 8-byte memory alignment.
     *
     * @param first8ByteValueOffset offset (in bytes) from the current
     * position to the first 8-byte value used in the following operation
     */
    public final void ensureAlignment(int first8ByteValueOffset) {
        int first8ByteValuePosition = buf.position() + first8ByteValueOffset;
        if ((first8ByteValuePosition & 7) != 0) {
            buf.putInt(BufferedOpCodes.NOOP);
        }
    }

    /**
     * Immediately processes each operation currently pending on the buffer.
     * This method will block until the entire buffer has been flushed.  The
     * queue lock must be acquired before calling this method.
     */
    public abstract void flushNow();

    /**
     * Immediately processes each operation currently pending on the buffer,
     * and then invokes the provided task.  This method will block until the
     * entire buffer has been flushed and the provided task has been executed.
     * The queue lock must be acquired before calling this method.
     */
    public abstract void flushAndInvokeNow(Runnable task);

    /**
     * Updates the current position of the underlying buffer, and then
     * flushes the queue immediately.  This method is useful when native code
     * has added data to the queue and needs to flush immediately.
     */
    public void flushNow(int position) {
        buf.position(position);
        flushNow();
    }
}
