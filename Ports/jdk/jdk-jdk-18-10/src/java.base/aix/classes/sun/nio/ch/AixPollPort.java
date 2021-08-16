/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012 SAP SE. All rights reserved.
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

import java.nio.channels.spi.AsynchronousChannelProvider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import jdk.internal.misc.Unsafe;

/**
 * AsynchronousChannelGroup implementation based on the AIX pollset framework.
 */
final class AixPollPort
    extends Port
{
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    static {
        IOUtil.load();
        init();
    }

    /**
     * struct pollfd {
     *     int fd;
     *     short events;
     *     short revents;
     * }
     */
    private static final int SIZEOF_POLLFD    = eventSize();
    private static final int OFFSETOF_EVENTS  = eventsOffset();
    private static final int OFFSETOF_REVENTS = reventsOffset();
    private static final int OFFSETOF_FD      = fdOffset();

    // opcodes
    private static final int PS_ADD     = 0x0;
    private static final int PS_MOD     = 0x1;
    private static final int PS_DELETE  = 0x2;

    // maximum number of events to poll at a time
    private static final int MAX_POLL_EVENTS = 512;

    // pollset ID
    private final int pollset;

    // true if port is closed
    private boolean closed;

    // socket pair used for wakeup
    private final int sp[];

    // socket pair used to indicate pending pollsetCtl calls
    // Background info: pollsetCtl blocks when another thread is in a pollsetPoll call.
    private final int ctlSp[];

    // number of wakeups pending
    private final AtomicInteger wakeupCount = new AtomicInteger();

    // address of the poll array passed to pollset_poll
    private final long address;

    // encapsulates an event for a channel
    static class Event {
        final PollableChannel channel;
        final int events;

        Event(PollableChannel channel, int events) {
            this.channel = channel;
            this.events = events;
        }

        PollableChannel channel()   { return channel; }
        int events()                { return events; }
    }

    // queue of events for cases that a polling thread dequeues more than one
    // event
    private final ArrayBlockingQueue<Event> queue;
    private final Event NEED_TO_POLL = new Event(null, 0);
    private final Event EXECUTE_TASK_OR_SHUTDOWN = new Event(null, 0);
    private final Event CONTINUE_AFTER_CTL_EVENT = new Event(null, 0);

    // encapsulates a pollset control event for a file descriptor
    static class ControlEvent {
        final int fd;
        final int events;
        final boolean removeOnly;
        int error = 0;

        ControlEvent(int fd, int events, boolean removeOnly) {
            this.fd = fd;
            this.events = events;
            this.removeOnly = removeOnly;
        }

        int fd()                 { return fd; }
        int events()             { return events; }
        boolean removeOnly()     { return removeOnly; }
        int error()              { return error; }
        void setError(int error) { this.error = error; }
    }

    // queue of control events that need to be processed
    // (this object is also used for synchronization)
    private final HashSet<ControlEvent> controlQueue = new HashSet<ControlEvent>();

    // lock used to check whether a poll operation is ongoing
    private final ReentrantLock controlLock = new ReentrantLock();

    AixPollPort(AsynchronousChannelProvider provider, ThreadPool pool)
        throws IOException
    {
        super(provider, pool);

        // open pollset
        this.pollset = pollsetCreate();

        // create socket pair for wakeup mechanism
        int[] sv = new int[2];
        try {
            socketpair(sv);
            // register one end with pollset
            pollsetCtl(pollset, PS_ADD, sv[0], Net.POLLIN);
        } catch (IOException x) {
            pollsetDestroy(pollset);
            throw x;
        }
        this.sp = sv;

        // create socket pair for pollset control mechanism
        sv = new int[2];
        try {
            socketpair(sv);
            // register one end with pollset
            pollsetCtl(pollset, PS_ADD, sv[0], Net.POLLIN);
        } catch (IOException x) {
            pollsetDestroy(pollset);
            throw x;
        }
        this.ctlSp = sv;

        // allocate the poll array
        this.address = allocatePollArray(MAX_POLL_EVENTS);

        // create the queue and offer the special event to ensure that the first
        // threads polls
        this.queue = new ArrayBlockingQueue<Event>(MAX_POLL_EVENTS);
        this.queue.offer(NEED_TO_POLL);
    }

    AixPollPort start() {
        startThreads(new EventHandlerTask());
        return this;
    }

    /**
     * Release all resources
     */
    private void implClose() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
        }
        freePollArray(address);
        close0(sp[0]);
        close0(sp[1]);
        close0(ctlSp[0]);
        close0(ctlSp[1]);
        pollsetDestroy(pollset);
    }

    private void wakeup() {
        if (wakeupCount.incrementAndGet() == 1) {
            // write byte to socketpair to force wakeup
            try {
                interrupt(sp[1]);
            } catch (IOException x) {
                throw new AssertionError(x);
            }
        }
    }

    @Override
    void executeOnHandlerTask(Runnable task) {
        synchronized (this) {
            if (closed)
                throw new RejectedExecutionException();
            offerTask(task);
            wakeup();
        }
    }

    @Override
    void shutdownHandlerTasks() {
        /*
         * If no tasks are running then just release resources; otherwise
         * write to the one end of the socketpair to wakeup any polling threads.
         */
        int nThreads = threadCount();
        if (nThreads == 0) {
            implClose();
        } else {
            // send interrupt to each thread
            while (nThreads-- > 0) {
                wakeup();
            }
        }
    }

    // invoke by clients to register a file descriptor
    @Override
    void startPoll(int fd, int events) {
        queueControlEvent(new ControlEvent(fd, events, false));
    }

    // Callback method for implementations that need special handling when fd is removed
    @Override
    protected void preUnregister(int fd) {
        queueControlEvent(new ControlEvent(fd, 0, true));
    }

    // Add control event into queue and wait for completion.
    // In case the control lock is free, this method also tries to apply the control change directly.
    private void queueControlEvent(ControlEvent ev) {
        // pollsetCtl blocks when a poll call is ongoing. This is very probable.
        // Therefore we let the polling thread do the pollsetCtl call.
        synchronized (controlQueue) {
            controlQueue.add(ev);
            // write byte to socketpair to force wakeup
            try {
                interrupt(ctlSp[1]);
            } catch (IOException x) {
                throw new AssertionError(x);
            }
            do {
                // Directly empty queue if no poll call is ongoing.
                if (controlLock.tryLock()) {
                    try {
                        processControlQueue();
                    } finally {
                        controlLock.unlock();
                    }
                } else {
                    try {
                        // Do not starve in case the polling thread returned before
                        // we could write to ctlSp[1] but the polling thread did not
                        // release the control lock until we checked. Therefore, use
                        // a timed wait for the time being.
                        controlQueue.wait(100);
                    } catch (InterruptedException e) {
                        // ignore exception and try again
                    }
                }
            } while (controlQueue.contains(ev));
        }
        if (ev.error() != 0) {
            throw new AssertionError();
        }
    }

    // Process all events currently stored in the control queue.
    private void processControlQueue() {
        synchronized (controlQueue) {
            // On Aix it is only possible to set the event
            // bits on the first call of pollsetCtl. Later
            // calls only add bits, but cannot remove them.
            // Therefore, we always remove the file
            // descriptor ignoring the error and then add it.
            Iterator<ControlEvent> iter = controlQueue.iterator();
            while (iter.hasNext()) {
                ControlEvent ev = iter.next();
                pollsetCtl(pollset, PS_DELETE, ev.fd(), 0);
                if (!ev.removeOnly()) {
                    ev.setError(pollsetCtl(pollset, PS_MOD, ev.fd(), ev.events()));
                }
                iter.remove();
            }
            controlQueue.notifyAll();
        }
    }

    /*
     * Task to process events from pollset and dispatch to the channel's
     * onEvent handler.
     *
     * Events are retreived from pollset in batch and offered to a BlockingQueue
     * where they are consumed by handler threads. A special "NEED_TO_POLL"
     * event is used to signal one consumer to re-poll when all events have
     * been consumed.
     */
    private class EventHandlerTask implements Runnable {
        private Event poll() throws IOException {
            try {
                for (;;) {
                    int n;
                    controlLock.lock();
                    try {
                        n = pollsetPoll(pollset, address, MAX_POLL_EVENTS);
                    } finally {
                        controlLock.unlock();
                    }
                    /*
                     * 'n' events have been read. Here we map them to their
                     * corresponding channel in batch and queue n-1 so that
                     * they can be handled by other handler threads. The last
                     * event is handled by this thread (and so is not queued).
                     */
                    fdToChannelLock.readLock().lock();
                    try {
                        while (n-- > 0) {
                            long eventAddress = getEvent(address, n);
                            int fd = getDescriptor(eventAddress);

                            // To emulate one shot semantic we need to remove
                            // the file descriptor here.
                            if (fd != sp[0] && fd != ctlSp[0]) {
                                synchronized (controlQueue) {
                                    pollsetCtl(pollset, PS_DELETE, fd, 0);
                                }
                            }

                            // wakeup
                            if (fd == sp[0]) {
                                if (wakeupCount.decrementAndGet() == 0) {
                                    // no more wakeups so drain pipe
                                    drain1(sp[0]);
                                }

                                // queue special event if there are more events
                                // to handle.
                                if (n > 0) {
                                    queue.offer(EXECUTE_TASK_OR_SHUTDOWN);
                                    continue;
                                }
                                return EXECUTE_TASK_OR_SHUTDOWN;
                            }

                            // wakeup to process control event
                            if (fd == ctlSp[0]) {
                                synchronized (controlQueue) {
                                    drain1(ctlSp[0]);
                                    processControlQueue();
                                }
                                if (n > 0) {
                                    continue;
                                }
                                return CONTINUE_AFTER_CTL_EVENT;
                            }

                            PollableChannel channel = fdToChannel.get(fd);
                            if (channel != null) {
                                int events = getRevents(eventAddress);
                                Event ev = new Event(channel, events);

                                // n-1 events are queued; This thread handles
                                // the last one except for the wakeup
                                if (n > 0) {
                                    queue.offer(ev);
                                } else {
                                    return ev;
                                }
                            }
                        }
                    } finally {
                        fdToChannelLock.readLock().unlock();
                    }
                }
            } finally {
                // to ensure that some thread will poll when all events have
                // been consumed
                queue.offer(NEED_TO_POLL);
            }
        }

        public void run() {
            Invoker.GroupAndInvokeCount myGroupAndInvokeCount =
                Invoker.getGroupAndInvokeCount();
            final boolean isPooledThread = (myGroupAndInvokeCount != null);
            boolean replaceMe = false;
            Event ev;
            try {
                for (;;) {
                    // reset invoke count
                    if (isPooledThread)
                        myGroupAndInvokeCount.resetInvokeCount();

                    try {
                        replaceMe = false;
                        ev = queue.take();

                        // no events and this thread has been "selected" to
                        // poll for more.
                        if (ev == NEED_TO_POLL) {
                            try {
                                ev = poll();
                            } catch (IOException x) {
                                x.printStackTrace();
                                return;
                            }
                        }
                    } catch (InterruptedException x) {
                        continue;
                    }

                    // contine after we processed a control event
                    if (ev == CONTINUE_AFTER_CTL_EVENT) {
                        continue;
                    }

                    // handle wakeup to execute task or shutdown
                    if (ev == EXECUTE_TASK_OR_SHUTDOWN) {
                        Runnable task = pollTask();
                        if (task == null) {
                            // shutdown request
                            return;
                        }
                        // run task (may throw error/exception)
                        replaceMe = true;
                        task.run();
                        continue;
                    }

                    // process event
                    try {
                        ev.channel().onEvent(ev.events(), isPooledThread);
                    } catch (Error x) {
                        replaceMe = true; throw x;
                    } catch (RuntimeException x) {
                        replaceMe = true; throw x;
                    }
                }
            } finally {
                // last handler to exit when shutdown releases resources
                int remaining = threadExit(this, replaceMe);
                if (remaining == 0 && isShutdown()) {
                    implClose();
                }
            }
        }
    }

    /**
     * Allocates a poll array to handle up to {@code count} events.
     */
    private static long allocatePollArray(int count) {
        return unsafe.allocateMemory(count * SIZEOF_POLLFD);
    }

    /**
     * Free a poll array
     */
    private static void freePollArray(long address) {
        unsafe.freeMemory(address);
    }

    /**
     * Returns event[i];
     */
    private static long getEvent(long address, int i) {
        return address + (SIZEOF_POLLFD*i);
    }

    /**
     * Returns event->fd
     */
    private static int getDescriptor(long eventAddress) {
        return unsafe.getInt(eventAddress + OFFSETOF_FD);
    }

    /**
     * Returns event->events
     */
    private static int getEvents(long eventAddress) {
        return unsafe.getChar(eventAddress + OFFSETOF_EVENTS);
    }

    /**
     * Returns event->revents
     */
    private static int getRevents(long eventAddress) {
        return unsafe.getChar(eventAddress + OFFSETOF_REVENTS);
    }

    // -- Native methods --

    private static native void init();

    private static native int eventSize();

    private static native int eventsOffset();

    private static native int reventsOffset();

    private static native int fdOffset();

    private static native int pollsetCreate() throws IOException;

    private static native int pollsetCtl(int pollset, int opcode, int fd, int events);

    private static native int pollsetPoll(int pollset, long pollAddress, int numfds)
        throws IOException;

    private static native void pollsetDestroy(int pollset);

    private static native void socketpair(int[] sv) throws IOException;

    private static native void interrupt(int fd) throws IOException;

    private static native void drain1(int fd) throws IOException;

    private static native void close0(int fd);
}
