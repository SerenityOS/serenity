/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static jdk.internal.net.http.common.Utils.pow2Size;

/*
 * A FIFO message storage facility.
 *
 * The queue supports at most one consumer and an arbitrary number of producers.
 * Methods `peek`, `remove` and `isEmpty` must not be invoked concurrently.
 * Methods `addText`, `addBinary`, `addPing`, `addPong` and `addClose` may be
 * invoked concurrently.
 *
 * This queue is of a bounded size. The queue pre-allocates array of the said
 * size and fills it with `Message` elements. The resulting structure never
 * changes. This allows to avoid re-allocation and garbage collection of
 * elements and arrays thereof. For this reason `Message` elements are never
 * returned from the `peek` method. Instead their components passed to the
 * provided callback.
 *
 * The queue consists of:
 *
 *   - a ring array of n + 1 `Message` elements
 *   - indexes H and T denoting the head and the tail elements of the queue
 *     respectively
 *
 * Each `Message` element contains a boolean flag. This flag is an auxiliary
 * communication between the producers and the consumer. The flag shows
 * whether or not the element is ready to be consumed (peeked at, removed). The
 * flag is required since updating an element involves many fields and thus is
 * not an atomic action. An addition to the queue happens in two steps:
 *
 * # Step 1
 *
 * Producers race with each other to secure an index for the element they add.
 * T is atomically advanced [1] only if the advanced value doesn't equal to H
 * (a producer doesn't bump into the head of the queue).
 *
 * # Step 2
 *
 * Once T is advanced in the previous step, the producer updates the message
 * fields of the element at the previous value of T and then sets the flag of
 * this element.
 *
 * A removal happens in a single step. The consumer gets the element at index H.
 * If the flag of this element is set, the consumer clears the fields of the
 * element, clears the flag and finally advances H.
 *
 * ----------------------------------------------------------------------------
 * [1] To advance the index is to change it from i to (i + 1) % (n + 1).
 */
public class MessageQueue {

    private final Message[] elements;

    private final AtomicInteger tail = new AtomicInteger();
    private volatile int head;

    public MessageQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException();
        }
        int s = pow2Size(capacity + 1);
        assert s % 2 == 0 : s;
        Message[] array = new Message[s];
        for (int i = 0; i < array.length; i++) {
            array[i] = new Message();
        }
        elements = array;
    }

    /* Exposed for testing purposes */
    protected static int effectiveCapacityOf(int n) {
        return pow2Size(n + 1) - 1;
    }

    public <T> void addText(CharBuffer message,
                            boolean isLast,
                            T attachment,
                            BiConsumer<? super T, ? super Throwable> action,
                            CompletableFuture<T> future)
            throws IOException
    {
        add(MessageQueue.Type.TEXT, null, null, message, isLast, -1, attachment,
            action, future);
    }

    private <T> void add(Type type,
                         Supplier<? extends ByteBuffer> binarySupplier,
                         ByteBuffer binary,
                         CharBuffer text,
                         boolean isLast,
                         int statusCode,
                         T attachment,
                         BiConsumer<? super T, ? super Throwable> action,
                         CompletableFuture<? super T> future)
            throws IOException
    {
        // Pong "subtype" is determined by whichever field (data carrier)
        // is not null. Both fields cannot be null or non-null simultaneously.
        assert type != Type.PONG || (binary == null ^ binarySupplier == null);
        int h, currentTail, newTail;
        do {
            h = head;
            currentTail = tail.get();
            newTail = (currentTail + 1) & (elements.length - 1);
            if (newTail == h) {
                throw new IOException("Queue full");
            }
        } while (!tail.compareAndSet(currentTail, newTail));
        Message t = elements[currentTail];
        if (t.ready) {
            throw new InternalError();
        }
        t.type = type;
        t.binarySupplier = binarySupplier;
        t.binary = binary;
        t.text = text;
        t.isLast = isLast;
        t.statusCode = statusCode;
        t.attachment = attachment;
        t.action = action;
        t.future = future;
        t.ready = true;
    }

    public <T> void addBinary(ByteBuffer message,
                              boolean isLast,
                              T attachment,
                              BiConsumer<? super T, ? super Throwable> action,
                              CompletableFuture<? super T> future)
            throws IOException
    {
        add(MessageQueue.Type.BINARY, null, message, null, isLast, -1, attachment,
            action, future);
    }

    public <T> void addPing(ByteBuffer message,
                            T attachment,
                            BiConsumer<? super T, ? super Throwable> action,
                            CompletableFuture<? super T> future)
            throws IOException
    {
        add(MessageQueue.Type.PING, null, message, null, false, -1, attachment,
            action, future);
    }

    public <T> void addPong(ByteBuffer message,
                            T attachment,
                            BiConsumer<? super T, ? super Throwable> action,
                            CompletableFuture<? super T> future)
            throws IOException
    {
        add(MessageQueue.Type.PONG, null, message, null, false, -1, attachment,
            action, future);
    }

    public <T> void addPong(Supplier<? extends ByteBuffer> message,
                            T attachment,
                            BiConsumer<? super T, ? super Throwable> action,
                            CompletableFuture<? super T> future)
            throws IOException
    {
        add(MessageQueue.Type.PONG, message, null, null, false, -1, attachment,
            action, future);
    }

    public <T> void addClose(int statusCode,
                             CharBuffer reason,
                             T attachment,
                             BiConsumer<? super T, ? super Throwable> action,
                             CompletableFuture<? super T> future)
            throws IOException
    {
        add(MessageQueue.Type.CLOSE, null, null, reason, false, statusCode,
            attachment, action, future);
    }

    @SuppressWarnings("unchecked")
    public <R, E extends Throwable> R peek(QueueCallback<R, E> callback)
            throws E
    {
        Message h = elements[head];
        if (!h.ready) {
            return callback.onEmpty();
        }
        Type type = h.type;
        switch (type) {
            case TEXT:
                try {
                    return (R) callback.onText(h.text, h.isLast, h.attachment,
                                               h.action, h.future);
                } catch (Throwable t) {
                    // Something unpleasant is going on here with the compiler.
                    // If this seemingly useless catch is omitted, the compiler
                    // reports an error:
                    //
                    //   java: unreported exception java.lang.Throwable;
                    //   must be caught or declared to be thrown
                    //
                    // My guess is there is a problem with both the type
                    // inference for the method AND @SuppressWarnings("unchecked")
                    // being working at the same time.
                    throw (E) t;
                }
            case BINARY:
                try {
                    return (R) callback.onBinary(h.binary, h.isLast, h.attachment,
                                                 h.action, h.future);
                } catch (Throwable t) {
                    throw (E) t;
                }
            case PING:
                try {
                    return (R) callback.onPing(h.binary, h.attachment, h.action,
                                               h.future);
                } catch (Throwable t) {
                    throw (E) t;
                }
            case PONG:
                try {
                    if (h.binarySupplier != null) {
                        return (R) callback.onPong(h.binarySupplier, h.attachment,
                                                   h.action, h.future);
                    } else {
                        return (R) callback.onPong(h.binary, h.attachment, h.action,
                                                   h.future);
                    }
                } catch (Throwable t) {
                    throw (E) t;
                }
            case CLOSE:
                try {
                    return (R) callback.onClose(h.statusCode, h.text, h.attachment,
                                                h.action, h.future);
                } catch (Throwable t) {
                    throw (E) t;
                }
            default:
                throw new InternalError(String.valueOf(type));
        }
    }

    public boolean isEmpty() {
        return !elements[head].ready;
    }

    public void remove() {
        int currentHead = head;
        Message h = elements[currentHead];
        if (!h.ready) {
            throw new InternalError("Queue empty");
        }
        h.type = null;
        h.binarySupplier = null;
        h.binary = null;
        h.text = null;
        h.attachment = null;
        h.action = null;
        h.future = null;
        h.ready = false;
        head = (currentHead + 1) & (elements.length - 1);
    }

    private enum Type {

        TEXT,
        BINARY,
        PING,
        PONG,
        CLOSE
    }

    /*
     * A callback for consuming a queue element's fields. Can return a result of
     * type T or throw an exception of type E. This design allows to avoid
     * "returning" results or "throwing" errors by updating some objects from
     * the outside of the methods.
     */
    public interface QueueCallback<R, E extends Throwable> {

        <T> R onText(CharBuffer message,
                     boolean isLast,
                     T attachment,
                     BiConsumer<? super T, ? super Throwable> action,
                     CompletableFuture<? super T> future) throws E;

        <T> R onBinary(ByteBuffer message,
                       boolean isLast,
                       T attachment,
                       BiConsumer<? super T, ? super Throwable> action,
                       CompletableFuture<? super T> future) throws E;

        <T> R onPing(ByteBuffer message,
                     T attachment,
                     BiConsumer<? super T, ? super Throwable> action,
                     CompletableFuture<? super T> future) throws E;

        <T> R onPong(ByteBuffer message,
                     T attachment,
                     BiConsumer<? super T, ? super Throwable> action,
                     CompletableFuture<? super T> future) throws E;

        <T> R onPong(Supplier<? extends ByteBuffer> message,
                     T attachment,
                     BiConsumer<? super T, ? super Throwable> action,
                     CompletableFuture<? super T> future) throws E;

        <T> R onClose(int statusCode,
                      CharBuffer reason,
                      T attachment,
                      BiConsumer<? super T, ? super Throwable> action,
                      CompletableFuture<? super T> future) throws E;

        /* The queue is empty*/
        R onEmpty() throws E;
    }

    /*
     * A union of components of all WebSocket message types; also a node in a
     * queue.
     *
     * A `Message` never leaves the context of the queue, thus the reference to
     * it cannot be retained by anyone other than the queue.
     */
    private static class Message {

        private volatile boolean ready;

        // -- The source message fields --

        private Type type;
        private Supplier<? extends ByteBuffer> binarySupplier;
        private ByteBuffer binary;
        private CharBuffer text;
        private boolean isLast;
        private int statusCode;
        private Object attachment;
        @SuppressWarnings("rawtypes")
        private BiConsumer action;
        @SuppressWarnings("rawtypes")
        private CompletableFuture future;
    }
}
