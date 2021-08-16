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
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NonBlockingPumpInputStream extends NonBlockingInputStream {

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    // Read and write buffer are backed by the same array
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    private final OutputStream output;

    private boolean closed;

    private IOException ioException;

    public NonBlockingPumpInputStream() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public NonBlockingPumpInputStream(int bufferSize) {
        byte[] buf = new byte[bufferSize];
        this.readBuffer = ByteBuffer.wrap(buf);
        this.writeBuffer = ByteBuffer.wrap(buf);
        this.output = new NbpOutputStream();
        // There are no bytes available to read after initialization
        readBuffer.limit(0);
    }

    public OutputStream getOutputStream() {
        return this.output;
    }

    private int wait(ByteBuffer buffer, long timeout) throws IOException {
        boolean isInfinite = (timeout <= 0L);
        long end = 0;
        if (!isInfinite) {
            end = System.currentTimeMillis() + timeout;
        }
        while (!closed && !buffer.hasRemaining() && (isInfinite || timeout > 0L)) {
            // Wake up waiting readers/writers
            notifyAll();
            try {
                wait(timeout);
                checkIoException();
            } catch (InterruptedException e) {
                checkIoException();
                throw new InterruptedIOException();
            }
            if (!isInfinite) {
                timeout = end - System.currentTimeMillis();
            }
        }
        return buffer.hasRemaining()
                ? 0
                : closed
                    ? EOF
                    : READ_EXPIRED;
    }

    private static boolean rewind(ByteBuffer buffer, ByteBuffer other) {
        // Extend limit of other buffer if there is additional input/output available
        if (buffer.position() > other.position()) {
            other.limit(buffer.position());
        }
        // If we have reached the end of the buffer, rewind and set the new limit
        if (buffer.position() == buffer.capacity()) {
            buffer.rewind();
            buffer.limit(other.position());
            return true;
        } else {
            return false;
        }
    }

    public synchronized int available() {
        int count = readBuffer.remaining();
        if (writeBuffer.position() < readBuffer.position()) {
            count += writeBuffer.position();
        }
        return count;
    }

    @Override
    public synchronized int read(long timeout, boolean isPeek) throws IOException {
        checkIoException();
        // Blocks until more input is available or the reader is closed.
        int res = wait(readBuffer, timeout);
        if (res >= 0) {
            res = readBuffer.get() & 0x00FF;
        }
        rewind(readBuffer, writeBuffer);
        return res;
    }

    public synchronized void setIoException(IOException exception) {
        this.ioException = exception;
        notifyAll();
    }

    protected synchronized void checkIoException() throws IOException {
        if (ioException != null) {
            throw ioException;
        }
    }

    synchronized void write(byte[] cbuf, int off, int len) throws IOException {
        while (len > 0) {
            // Blocks until there is new space available for buffering or the
            // reader is closed.
            if (wait(writeBuffer, 0L) == EOF) {
                throw new ClosedException();
            }
            // Copy as much characters as we can
            int count = Math.min(len, writeBuffer.remaining());
            writeBuffer.put(cbuf, off, count);
            off += count;
            len -= count;
            // Update buffer states and rewind if necessary
            rewind(writeBuffer, readBuffer);
        }
    }

    synchronized void flush() {
        // Avoid waking up readers when there is nothing to read
        if (readBuffer.hasRemaining()) {
            // Notify readers
            notifyAll();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        this.closed = true;
        notifyAll();
    }

    private class NbpOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            NonBlockingPumpInputStream.this.write(new byte[] { (byte) b }, 0, 1);
        }

        @Override
        public void write(byte[] cbuf, int off, int len) throws IOException {
            NonBlockingPumpInputStream.this.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            NonBlockingPumpInputStream.this.flush();
        }

        @Override
        public void close() throws IOException {
            NonBlockingPumpInputStream.this.close();
        }

    }

}
