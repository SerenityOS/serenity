/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NonBlockingPumpReader extends NonBlockingReader {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private final char[] buffer;
    private int read;
    private int write;
    private int count;

    /** Main lock guarding all access */
    final ReentrantLock lock;
    /** Condition for waiting takes */
    private final Condition notEmpty;
    /** Condition for waiting puts */
    private final Condition notFull;

    private final Writer writer;

    private boolean closed;

    public NonBlockingPumpReader() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public NonBlockingPumpReader(int bufferSize) {
        this.buffer = new char[bufferSize];
        this.writer = new NbpWriter();
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.notFull = lock.newCondition();
    }

    public Writer getWriter() {
        return this.writer;
    }

    @Override
    public boolean ready() {
        return available() > 0;
    }

    public int available() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected int read(long timeout, boolean isPeek) throws IOException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // Blocks until more input is available or the reader is closed.
            if (!closed && count == 0) {
                try {
                    notEmpty.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw (IOException) new InterruptedIOException().initCause(e);
                }
            }
            if (closed) {
                return EOF;
            } else if (count == 0) {
                return READ_EXPIRED;
            } else {
                if (isPeek) {
                    return buffer[read];
                } else {
                    int res = buffer[read];
                    if (++read == buffer.length) {
                        read = 0;
                    }
                    --count;
                    notFull.signal();
                    return res;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int readBuffered(char[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (b.length == 0) {
            return 0;
        } else {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if (!closed && count == 0) {
                    try {
                        notEmpty.await();
                    } catch (InterruptedException e) {
                        throw (IOException) new InterruptedIOException().initCause(e);
                    }
                }
                if (closed) {
                    return EOF;
                } else if (count == 0) {
                    return READ_EXPIRED;
                } else {
                    int r = Math.min(b.length, count);
                    for (int i = 0; i < r; i++) {
                        b[i] = buffer[read++];
                        if (read == buffer.length) {
                            read = 0;
                        }
                    }
                    count -= r;
                    notFull.signal();
                    return r;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    void write(char[] cbuf, int off, int len) throws IOException {
        if (len > 0) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                while (len > 0) {
                    // Blocks until there is new space available for buffering or the
                    // reader is closed.
                    if (!closed && count == buffer.length) {
                        try {
                            notFull.await();
                        } catch (InterruptedException e) {
                            throw (IOException) new InterruptedIOException().initCause(e);
                        }
                    }
                    if (closed) {
                        throw new IOException("Closed");
                    }
                    while (len > 0 && count < buffer.length) {
                        buffer[write++] = cbuf[off++];
                        count++;
                        len--;
                        if (write == buffer.length) {
                            write = 0;
                        }
                    }
                    notEmpty.signal();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void close() throws IOException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            this.closed = true;
            this.notEmpty.signalAll();
            this.notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private class NbpWriter extends Writer {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            NonBlockingPumpReader.this.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
            NonBlockingPumpReader.this.close();
        }

    }

}
