/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * This class wraps a regular input stream and allows it to appear as if it
 * is non-blocking; that is, reads can be performed against it that timeout
 * if no data is seen for a period of time.  This effect is achieved by having
 * a separate thread perform all non-blocking read requests and then
 * waiting on the thread to complete.
 *
 * <p>VERY IMPORTANT NOTES
 * <ul>
 *   <li> This class is not thread safe. It expects at most one reader.
 *   <li> The {@link #shutdown()} method must be called in order to shut down
 *          the thread that handles blocking I/O.
 * </ul>
 */
public class NonBlockingInputStreamImpl
    extends NonBlockingInputStream
{
    private InputStream in;                  // The actual input stream
    private int         b = READ_EXPIRED;    // Recently read byte

    private String      name;
    private boolean     threadIsReading      = false;
    private IOException exception            = null;
    private long        threadDelay          = 60 * 1000;
    private Thread      thread;

    /**
     * Creates a <code>NonBlockingReader</code> out of a normal blocking
     * reader. Note that this call also spawn a separate thread to perform the
     * blocking I/O on behalf of the thread that is using this class. The
     * {@link #shutdown()} method must be called in order to shut this thread down.
     * @param name The stream name
     * @param in The reader to wrap
     */
    public NonBlockingInputStreamImpl(String name, InputStream in) {
        this.in = in;
        this.name = name;
    }

    private synchronized void startReadingThreadIfNeeded() {
        if (thread == null) {
            thread = new Thread(this::run);
            thread.setName(name + " non blocking reader thread");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Shuts down the thread that is handling blocking I/O. Note that if the
     * thread is currently blocked waiting for I/O it will not actually
     * shut down until the I/O is received.
     */
    public synchronized void shutdown() {
        if (thread != null) {
            notify();
        }
    }

    @Override
    public void close() throws IOException {
        /*
         * The underlying input stream is closed first. This means that if the
         * I/O thread was blocked waiting on input, it will be woken for us.
         */
        in.close();
        shutdown();
    }

    /**
     * Attempts to read a byte from the input stream for a specific
     * period of time.
     * @param timeout The amount of time to wait for the character
     * @param isPeek <code>true</code>if the byte read must not be consumed
     * @return The byte read, -1 if EOF is reached, or -2 if the
     *   read timed out.
     * @throws IOException if anything wrong happens
     */
    public synchronized int read(long timeout, boolean isPeek) throws IOException {
        /*
         * If the thread hit an IOException, we report it.
         */
        if (exception != null) {
            assert b == READ_EXPIRED;
            IOException toBeThrown = exception;
            if (!isPeek)
                exception = null;
            throw toBeThrown;
        }

        /*
         * If there was a pending character from the thread, then
         * we send it. If the timeout is 0L or the thread was shut down
         * then do a local read.
         */
        if (b >= -1) {
            assert exception == null;
        }
        else if (!isPeek && timeout <= 0L && !threadIsReading) {
            b = in.read();
        }
        else {
            /*
             * If the thread isn't reading already, then ask it to do so.
             */
            if (!threadIsReading) {
                threadIsReading = true;
                startReadingThreadIfNeeded();
                notifyAll();
            }

            boolean isInfinite = (timeout <= 0L);

            /*
             * So the thread is currently doing the reading for us. So
             * now we play the waiting game.
             */
            while (isInfinite || timeout > 0L)  {
                long start = System.currentTimeMillis ();

                try {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    wait(timeout);
                }
                catch (InterruptedException e) {
                    exception = (IOException) new InterruptedIOException().initCause(e);
                }

                if (exception != null) {
                    assert b == READ_EXPIRED;

                    IOException toBeThrown = exception;
                    if (!isPeek)
                        exception = null;
                    throw toBeThrown;
                }

                if (b >= -1) {
                    assert exception == null;
                    break;
                }

                if (!isInfinite) {
                    timeout -= System.currentTimeMillis() - start;
                }
            }
        }

        /*
         * b is the character that was just read. Either we set it because
         * a local read was performed or the read thread set it (or failed to
         * change it).  We will return it's value, but if this was a peek
         * operation, then we leave it in place.
         */
        int ret = b;
        if (!isPeek) {
            b = READ_EXPIRED;
        }
        return ret;
    }

    private void run () {
        Log.debug("NonBlockingInputStream start");
        boolean needToRead;

        try {
            while (true) {

                /*
                 * Synchronize to grab variables accessed by both this thread
                 * and the accessing thread.
                 */
                synchronized (this) {
                    needToRead = this.threadIsReading;

                    try {
                        /*
                         * Nothing to do? Then wait.
                         */
                        if (!needToRead) {
                            wait(threadDelay);
                        }
                    } catch (InterruptedException e) {
                        /* IGNORED */
                    }

                    needToRead = this.threadIsReading;
                    if (!needToRead) {
                        return;
                    }
                }

                /*
                 * We're not shutting down, but we need to read. This cannot
                 * happen while we are holding the lock (which we aren't now).
                 */
                int byteRead = READ_EXPIRED;
                IOException failure = null;
                try {
                    byteRead = in.read();
                } catch (IOException e) {
                    failure = e;
                }

                /*
                 * Re-grab the lock to update the state.
                 */
                synchronized (this) {
                    exception = failure;
                    b = byteRead;
                    threadIsReading = false;
                    notify();
                }

                // If end of stream, exit the loop thread
                if (byteRead < 0) {
                    return;
                }
            }
        } catch (Throwable t) {
            Log.warn("Error in NonBlockingInputStream thread", t);
        } finally {
            Log.debug("NonBlockingInputStream shutdown");
            synchronized (this) {
                thread = null;
                threadIsReading = false;
            }
        }
    }

}
