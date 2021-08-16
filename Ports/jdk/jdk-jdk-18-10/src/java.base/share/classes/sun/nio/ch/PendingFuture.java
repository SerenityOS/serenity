/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.nio.channels.*;
import java.util.concurrent.*;
import java.io.IOException;

/**
 * A Future for a pending I/O operation. A PendingFuture allows for the
 * attachment of an additional arbitrary context object and a timer task.
 */

final class PendingFuture<V,A> implements Future<V> {

    private final AsynchronousChannel channel;
    private final CompletionHandler<V,? super A> handler;
    private final A attachment;

    // true if result (or exception) is available
    private volatile boolean haveResult;
    private volatile V result;
    private volatile Throwable exc;

    // latch for waiting (created lazily if needed)
    private CountDownLatch latch;

    // optional timer task that is cancelled when result becomes available
    private Future<?> timeoutTask;

    // optional context object
    private volatile Object context;

    PendingFuture(AsynchronousChannel channel,
                  CompletionHandler<V,? super A> handler,
                  A attachment,
                  Object context)
    {
        this.channel = channel;
        this.handler = handler;
        this.attachment = attachment;
        this.context = context;
    }

    PendingFuture(AsynchronousChannel channel,
                  CompletionHandler<V,? super A> handler,
                  A attachment)
    {
        this.channel = channel;
        this.handler = handler;
        this.attachment = attachment;
    }

    PendingFuture(AsynchronousChannel channel) {
        this(channel, null, null);
    }

    PendingFuture(AsynchronousChannel channel, Object context) {
        this(channel, null, null, context);
    }

    AsynchronousChannel channel() {
        return channel;
    }

    CompletionHandler<V,? super A> handler() {
        return handler;
    }

    A attachment() {
        return attachment;
    }

    void setContext(Object context) {
        this.context = context;
    }

    Object getContext() {
        return context;
    }

    void setTimeoutTask(Future<?> task) {
        synchronized (this) {
            if (haveResult) {
                task.cancel(false);
            } else {
                this.timeoutTask = task;
            }
        }
    }

    // creates latch if required; return true if caller needs to wait
    private boolean prepareForWait() {
        synchronized (this) {
            if (haveResult) {
                return false;
            } else {
                if (latch == null)
                    latch = new CountDownLatch(1);
                return true;
            }
        }
    }

    /**
     * Sets the result, or a no-op if the result or exception is already set.
     */
    void setResult(V res) {
        synchronized (this) {
            if (haveResult)
                return;
            result = res;
            haveResult = true;
            if (timeoutTask != null)
                timeoutTask.cancel(false);
            if (latch != null)
                latch.countDown();
        }
    }

    /**
     * Sets the result, or a no-op if the result or exception is already set.
     */
    void setFailure(Throwable x) {
        if (!(x instanceof IOException) && !(x instanceof SecurityException))
            x = new IOException(x);
        synchronized (this) {
            if (haveResult)
                return;
            exc = x;
            haveResult = true;
            if (timeoutTask != null)
                timeoutTask.cancel(false);
            if (latch != null)
                latch.countDown();
        }
    }

    /**
     * Sets the result
     */
    void setResult(V res, Throwable x) {
        if (x == null) {
            setResult(res);
        } else {
            setFailure(x);
        }
    }

    @Override
    public V get() throws ExecutionException, InterruptedException {
        if (!haveResult) {
            boolean needToWait = prepareForWait();
            if (needToWait)
                latch.await();
        }
        if (exc != null) {
            if (exc instanceof CancellationException)
                throw new CancellationException();
            throw new ExecutionException(exc);
        }
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit)
        throws ExecutionException, InterruptedException, TimeoutException
    {
        if (!haveResult) {
            boolean needToWait = prepareForWait();
            if (needToWait)
                if (!latch.await(timeout, unit)) throw new TimeoutException();
        }
        if (exc != null) {
            if (exc instanceof CancellationException)
                throw new CancellationException();
            throw new ExecutionException(exc);
        }
        return result;
    }

    Throwable exception() {
        return (exc instanceof CancellationException) ? null : exc;
    }

    V value() {
        return result;
    }

    @Override
    public boolean isCancelled() {
        return (exc instanceof CancellationException);
    }

    @Override
    public boolean isDone() {
        return haveResult;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        synchronized (this) {
            if (haveResult)
                return false;    // already completed

            // notify channel
            if (channel() instanceof Cancellable)
                ((Cancellable)channel()).onCancel(this);

            // set result and cancel timer
            exc = new CancellationException();
            haveResult = true;
            if (timeoutTask != null)
                timeoutTask.cancel(false);
        }

        // close channel if forceful cancel
        if (mayInterruptIfRunning) {
            try {
                channel().close();
            } catch (IOException ignore) { }
        }

        // release waiters
        if (latch != null)
            latch.countDown();
        return true;
    }
}
