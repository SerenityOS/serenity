/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;

// Each stream has one of these for input. Each Http2Connection has one
// for output. Can be used blocking or asynchronously.

public class Queue<T> implements ExceptionallyCloseable {

    private final LinkedList<T> q = new LinkedList<>();
    private boolean closed = false;
    private boolean closing = false;
    private Throwable exception = null;
    private int waiters; // true if someone waiting
    private final T closeSentinel;

    Queue(T closeSentinel) {
        this.closeSentinel = Objects.requireNonNull(closeSentinel);
    }

    public synchronized int size() {
        return q.size();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public synchronized boolean isClosing() {
        return closing;
    }

    public synchronized void put(T obj) throws IOException {
        Objects.requireNonNull(obj);
        if (closed || closing) {
            throw new IOException("stream closed");
        }

        q.add(obj);

        if (waiters > 0) {
            notifyAll();
        }
    }

    // Other close() variants are immediate and abortive
    // This allows whatever is on Q to be processed first.

    public synchronized void orderlyClose() {
        if (closing || closed)
            return;

        try {
            put(closeSentinel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        closing = true;
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        notifyAll();
    }

    @Override
    public synchronized void closeExceptionally(Throwable t) {
        if (exception == null) exception = t;
        else if (t != null && t != exception) {
            if (!Stream.of(exception.getSuppressed())
                .filter(x -> x == t)
                .findFirst()
                .isPresent())
            {
                exception.addSuppressed(t);
            }
        }
        close();
    }

    public synchronized T take() throws IOException {
        if (closed) {
            throw newIOException("stream closed");
        }
        try {
            while (q.size() == 0) {
                waiters++;
                wait();
                if (closed) {
                    throw newIOException("Queue closed");
                }
                waiters--;
            }
            T item = q.removeFirst();
            if (item.equals(closeSentinel)) {
                closed = true;
                assert q.isEmpty();
                return null;
            }
            return item;
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    public synchronized T poll() throws IOException {
        if (closed) {
            throw newIOException("stream closed");
        }

        if (q.isEmpty()) {
            return null;
        }
        return take();
    }

    private IOException newIOException(String msg) {
        if (exception == null) {
            return new IOException(msg);
        } else {
            return new IOException(msg, exception);
        }
    }
}
