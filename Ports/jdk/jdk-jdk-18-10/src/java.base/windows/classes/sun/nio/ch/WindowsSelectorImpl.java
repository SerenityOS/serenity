/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import jdk.internal.misc.Unsafe;

/**
 * A multi-threaded implementation of Selector for Windows.
 *
 * @author Konstantin Kladko
 * @author Mark Reinhold
 */

class WindowsSelectorImpl extends SelectorImpl {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static int addressSize = unsafe.addressSize();

    private static int dependsArch(int value32, int value64) {
        return (addressSize == 4) ? value32 : value64;
    }

    // Initial capacity of the poll array
    private final int INIT_CAP = 8;
    // Maximum number of sockets for select().
    // Should be INIT_CAP times a power of 2
    private static final int MAX_SELECTABLE_FDS = 1024;

    // Size of FD_SET struct to allocate a buffer for it in SubSelector,
    // aligned to 8 bytes on 64-bit:
    // struct { unsigned int fd_count; SOCKET fd_array[MAX_SELECTABLE_FDS]; }.
    private static final long SIZEOF_FD_SET = dependsArch(
            4 + MAX_SELECTABLE_FDS * 4,      // SOCKET = unsigned int
            4 + MAX_SELECTABLE_FDS * 8 + 4); // SOCKET = unsigned __int64

    // The list of SelectableChannels serviced by this Selector. Every mod
    // MAX_SELECTABLE_FDS entry is bogus, to align this array with the poll
    // array,  where the corresponding entry is occupied by the wakeupSocket
    private SelectionKeyImpl[] channelArray = new SelectionKeyImpl[INIT_CAP];

    // The global native poll array holds file decriptors and event masks
    private PollArrayWrapper pollWrapper;

    // The number of valid entries in  poll array, including entries occupied
    // by wakeup socket handle.
    private int totalChannels = 1;

    // Number of helper threads needed for select. We need one thread per
    // each additional set of MAX_SELECTABLE_FDS - 1 channels.
    private int threadsCount = 0;

    // A list of helper threads for select.
    private final List<SelectThread> threads = new ArrayList<SelectThread>();

    //Pipe used as a wakeup object.
    private final Pipe wakeupPipe;

    // File descriptors corresponding to source and sink
    private final int wakeupSourceFd, wakeupSinkFd;

    // Maps file descriptors to their indices in  pollArray
    private static final class FdMap extends HashMap<Integer, MapEntry> {
        static final long serialVersionUID = 0L;
        private MapEntry get(int desc) {
            return get(Integer.valueOf(desc));
        }
        private MapEntry put(SelectionKeyImpl ski) {
            return put(Integer.valueOf(ski.getFDVal()), new MapEntry(ski));
        }
        private MapEntry remove(SelectionKeyImpl ski) {
            Integer fd = Integer.valueOf(ski.getFDVal());
            MapEntry x = get(fd);
            if ((x != null) && (x.ski.channel() == ski.channel()))
                return remove(fd);
            return null;
        }
    }

    // class for fdMap entries
    private static final class MapEntry {
        final SelectionKeyImpl ski;
        long updateCount = 0;
        MapEntry(SelectionKeyImpl ski) {
            this.ski = ski;
        }
    }
    private final FdMap fdMap = new FdMap();

    // SubSelector for the main thread
    private final SubSelector subSelector = new SubSelector();

    private long timeout; //timeout for poll

    // Lock for interrupt triggering and clearing
    private final Object interruptLock = new Object();
    private volatile boolean interruptTriggered;

    // pending new registrations/updates, queued by implRegister and setEventOps
    private final Object updateLock = new Object();
    private final Deque<SelectionKeyImpl> newKeys = new ArrayDeque<>();
    private final Deque<SelectionKeyImpl> updateKeys = new ArrayDeque<>();


    WindowsSelectorImpl(SelectorProvider sp) throws IOException {
        super(sp);
        pollWrapper = new PollArrayWrapper(INIT_CAP);
        wakeupPipe = new PipeImpl(sp, false);
        wakeupSourceFd = ((SelChImpl)wakeupPipe.source()).getFDVal();
        wakeupSinkFd = ((SelChImpl)wakeupPipe.sink()).getFDVal();
        pollWrapper.addWakeupSocket(wakeupSourceFd, 0);
    }

    private void ensureOpen() {
        if (!isOpen())
            throw new ClosedSelectorException();
    }

    @Override
    protected int doSelect(Consumer<SelectionKey> action, long timeout)
        throws IOException
    {
        assert Thread.holdsLock(this);
        this.timeout = timeout; // set selector timeout
        processUpdateQueue();
        processDeregisterQueue();
        if (interruptTriggered) {
            resetWakeupSocket();
            return 0;
        }
        // Calculate number of helper threads needed for poll. If necessary
        // threads are created here and start waiting on startLock
        adjustThreadsCount();
        finishLock.reset(); // reset finishLock
        // Wakeup helper threads, waiting on startLock, so they start polling.
        // Redundant threads will exit here after wakeup.
        startLock.startThreads();
        // do polling in the main thread. Main thread is responsible for
        // first MAX_SELECTABLE_FDS entries in pollArray.
        try {
            begin();
            try {
                subSelector.poll();
            } catch (IOException e) {
                finishLock.setException(e); // Save this exception
            }
            // Main thread is out of poll(). Wakeup others and wait for them
            if (threads.size() > 0)
                finishLock.waitForHelperThreads();
          } finally {
              end();
          }
        // Done with poll(). Set wakeupSocket to nonsignaled  for the next run.
        finishLock.checkForException();
        processDeregisterQueue();
        int updated = updateSelectedKeys(action);
        // Done with poll(). Set wakeupSocket to nonsignaled  for the next run.
        resetWakeupSocket();
        return updated;
    }

    /**
     * Process new registrations and changes to the interest ops.
     */
    private void processUpdateQueue() {
        assert Thread.holdsLock(this);

        synchronized (updateLock) {
            SelectionKeyImpl ski;

            // new registrations
            while ((ski = newKeys.pollFirst()) != null) {
                if (ski.isValid()) {
                    growIfNeeded();
                    channelArray[totalChannels] = ski;
                    ski.setIndex(totalChannels);
                    pollWrapper.putEntry(totalChannels, ski);
                    totalChannels++;
                    MapEntry previous = fdMap.put(ski);
                    assert previous == null;
                }
            }

            // changes to interest ops
            while ((ski = updateKeys.pollFirst()) != null) {
                int events = ski.translateInterestOps();
                int fd = ski.getFDVal();
                if (ski.isValid() && fdMap.containsKey(fd)) {
                    int index = ski.getIndex();
                    assert index >= 0 && index < totalChannels;
                    pollWrapper.putEventOps(index, events);
                }
            }
        }
    }

    // Helper threads wait on this lock for the next poll.
    private final StartLock startLock = new StartLock();

    private final class StartLock {
        // A variable which distinguishes the current run of doSelect from the
        // previous one. Incrementing runsCounter and notifying threads will
        // trigger another round of poll.
        private long runsCounter;
       // Triggers threads, waiting on this lock to start polling.
        private synchronized void startThreads() {
            runsCounter++; // next run
            notifyAll(); // wake up threads.
        }
        // This function is called by a helper thread to wait for the
        // next round of poll(). It also checks, if this thread became
        // redundant. If yes, it returns true, notifying the thread
        // that it should exit.
        private synchronized boolean waitForStart(SelectThread thread) {
            while (true) {
                while (runsCounter == thread.lastRun) {
                    try {
                        startLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (thread.isZombie()) { // redundant thread
                    return true; // will cause run() to exit.
                } else {
                    thread.lastRun = runsCounter; // update lastRun
                    return false; //   will cause run() to poll.
                }
            }
        }
    }

    // Main thread waits on this lock, until all helper threads are done
    // with poll().
    private final FinishLock finishLock = new FinishLock();

    private final class FinishLock  {
        // Number of helper threads, that did not finish yet.
        private int threadsToFinish;

        // IOException which occurred during the last run.
        IOException exception = null;

        // Called before polling.
        private void reset() {
            threadsToFinish = threads.size(); // helper threads
        }

        // Each helper thread invokes this function on finishLock, when
        // the thread is done with poll().
        private synchronized void threadFinished() {
            if (threadsToFinish == threads.size()) { // finished poll() first
                // if finished first, wakeup others
                wakeup();
            }
            threadsToFinish--;
            if (threadsToFinish == 0) // all helper threads finished poll().
                notify();             // notify the main thread
        }

        // The main thread invokes this function on finishLock to wait
        // for helper threads to finish poll().
        private synchronized void waitForHelperThreads() {
            if (threadsToFinish == threads.size()) {
                // no helper threads finished yet. Wakeup them up.
                wakeup();
            }
            while (threadsToFinish != 0) {
                try {
                    finishLock.wait();
                } catch (InterruptedException e) {
                    // Interrupted - set interrupted state.
                    Thread.currentThread().interrupt();
                }
            }
        }

        // sets IOException for this run
        private synchronized void setException(IOException e) {
            exception = e;
        }

        // Checks if there was any exception during the last run.
        // If yes, throws it
        private void checkForException() throws IOException {
            if (exception == null)
                return;
            String message = "An exception occurred" +
                    " during the execution of select(): \n" +
                    exception + '\n';
            exception = null;
            throw new IOException(message);
        }
    }

    private final class SubSelector {
        private final int pollArrayIndex; // starting index in pollArray to poll
        // These arrays will hold result of native select().
        // The first element of each array is the number of selected sockets.
        // Other elements are file descriptors of selected sockets.
        private final int[] readFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] writeFds = new int [MAX_SELECTABLE_FDS + 1];
        private final int[] exceptFds = new int [MAX_SELECTABLE_FDS + 1];
        // Buffer for readfds, writefds and exceptfds structs that are passed
        // to native select().
        private final long fdsBuffer = unsafe.allocateMemory(SIZEOF_FD_SET * 3);

        private SubSelector() {
            this.pollArrayIndex = 0; // main thread
        }

        private SubSelector(int threadIndex) { // helper threads
            this.pollArrayIndex = (threadIndex + 1) * MAX_SELECTABLE_FDS;
        }

        private int poll() throws IOException{ // poll for the main thread
            return poll0(pollWrapper.pollArrayAddress,
                         Math.min(totalChannels, MAX_SELECTABLE_FDS),
                         readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }

        private int poll(int index) throws IOException {
            // poll for helper threads
            return  poll0(pollWrapper.pollArrayAddress +
                     (pollArrayIndex * PollArrayWrapper.SIZE_POLLFD),
                     Math.min(MAX_SELECTABLE_FDS,
                             totalChannels - (index + 1) * MAX_SELECTABLE_FDS),
                     readFds, writeFds, exceptFds, timeout, fdsBuffer);
        }

        private native int poll0(long pollAddress, int numfds,
             int[] readFds, int[] writeFds, int[] exceptFds, long timeout, long fdsBuffer);

        private int processSelectedKeys(long updateCount, Consumer<SelectionKey> action)
            throws IOException
        {
            int numKeysUpdated = 0;
            numKeysUpdated += processFDSet(updateCount, action, readFds,
                                           Net.POLLIN,
                                           false);
            numKeysUpdated += processFDSet(updateCount, action, writeFds,
                                           Net.POLLCONN |
                                           Net.POLLOUT,
                                           false);
            numKeysUpdated += processFDSet(updateCount, action, exceptFds,
                                           Net.POLLIN |
                                           Net.POLLCONN |
                                           Net.POLLOUT,
                                           true);
            return numKeysUpdated;
        }

        /**
         * updateCount is used to tell if a key has been counted as updated
         * in this select operation.
         *
         * me.updateCount <= updateCount
         */
        private int processFDSet(long updateCount,
                                 Consumer<SelectionKey> action,
                                 int[] fds, int rOps,
                                 boolean isExceptFds)
            throws IOException
        {
            int numKeysUpdated = 0;
            for (int i = 1; i <= fds[0]; i++) {
                int desc = fds[i];
                if (desc == wakeupSourceFd) {
                    synchronized (interruptLock) {
                        interruptTriggered = true;
                    }
                    continue;
                }
                MapEntry me = fdMap.get(desc);
                // If me is null, the key was deregistered in the previous
                // processDeregisterQueue.
                if (me == null)
                    continue;
                SelectionKeyImpl ski = me.ski;

                // The descriptor may be in the exceptfds set because there is
                // OOB data queued to the socket. If there is OOB data then it
                // is discarded and the key is not added to the selected set.
                SelectableChannel sc = ski.channel();
                if (isExceptFds && (sc instanceof SocketChannelImpl)
                        && ((SocketChannelImpl) sc).isNetSocket()
                        && Net.discardOOB(ski.getFD())) {
                    continue;
                }

                int updated = processReadyEvents(rOps, ski, action);
                if (updated > 0 && me.updateCount != updateCount) {
                    me.updateCount = updateCount;
                    numKeysUpdated++;
                }
            }
            return numKeysUpdated;
        }

        private void freeFDSetBuffer() {
            unsafe.freeMemory(fdsBuffer);
        }
    }

    // Represents a helper thread used for select.
    private final class SelectThread extends Thread {
        private final int index; // index of this thread
        final SubSelector subSelector;
        private long lastRun = 0; // last run number
        private volatile boolean zombie;
        // Creates a new thread
        private SelectThread(int i) {
            super(null, null, "SelectorHelper", 0, false);
            this.index = i;
            this.subSelector = new SubSelector(i);
            //make sure we wait for next round of poll
            this.lastRun = startLock.runsCounter;
        }
        void makeZombie() {
            zombie = true;
        }
        boolean isZombie() {
            return zombie;
        }
        public void run() {
            while (true) { // poll loop
                // wait for the start of poll. If this thread has become
                // redundant, then exit.
                if (startLock.waitForStart(this)) {
                    subSelector.freeFDSetBuffer();
                    return;
                }
                // call poll()
                try {
                    subSelector.poll(index);
                } catch (IOException e) {
                    // Save this exception and let other threads finish.
                    finishLock.setException(e);
                }
                // notify main thread, that this thread has finished, and
                // wakeup others, if this thread is the first to finish.
                finishLock.threadFinished();
            }
        }
    }

    // After some channels registered/deregistered, the number of required
    // helper threads may have changed. Adjust this number.
    private void adjustThreadsCount() {
        if (threadsCount > threads.size()) {
            // More threads needed. Start more threads.
            for (int i = threads.size(); i < threadsCount; i++) {
                SelectThread newThread = new SelectThread(i);
                threads.add(newThread);
                newThread.setDaemon(true);
                newThread.start();
            }
        } else if (threadsCount < threads.size()) {
            // Some threads become redundant. Remove them from the threads List.
            for (int i = threads.size() - 1 ; i >= threadsCount; i--)
                threads.remove(i).makeZombie();
        }
    }

    // Sets Windows wakeup socket to a signaled state.
    private void setWakeupSocket() {
        setWakeupSocket0(wakeupSinkFd);
    }
    private native void setWakeupSocket0(int wakeupSinkFd);

    // Sets Windows wakeup socket to a non-signaled state.
    private void resetWakeupSocket() {
        synchronized (interruptLock) {
            if (interruptTriggered == false)
                return;
            resetWakeupSocket0(wakeupSourceFd);
            interruptTriggered = false;
        }
    }

    private native void resetWakeupSocket0(int wakeupSourceFd);

    // We increment this counter on each call to updateSelectedKeys()
    // each entry in  SubSelector.fdsMap has a memorized value of
    // updateCount. When we increment numKeysUpdated we set updateCount
    // for the corresponding entry to its current value. This is used to
    // avoid counting the same key more than once - the same key can
    // appear in readfds and writefds.
    private long updateCount = 0;

    // Update ops of the corresponding Channels. Add the ready keys to the
    // ready queue.
    private int updateSelectedKeys(Consumer<SelectionKey> action) throws IOException {
        updateCount++;
        int numKeysUpdated = 0;
        numKeysUpdated += subSelector.processSelectedKeys(updateCount, action);
        for (SelectThread t: threads) {
            numKeysUpdated += t.subSelector.processSelectedKeys(updateCount, action);
        }
        return numKeysUpdated;
    }

    @Override
    protected void implClose() throws IOException {
        assert !isOpen();
        assert Thread.holdsLock(this);

        // prevent further wakeup
        synchronized (interruptLock) {
            interruptTriggered = true;
        }

        wakeupPipe.sink().close();
        wakeupPipe.source().close();
        pollWrapper.free();

        // Make all remaining helper threads exit
        for (SelectThread t: threads)
             t.makeZombie();
        startLock.startThreads();
        subSelector.freeFDSetBuffer();
    }

    @Override
    protected void implRegister(SelectionKeyImpl ski) {
        ensureOpen();
        synchronized (updateLock) {
            newKeys.addLast(ski);
        }
    }

    private void growIfNeeded() {
        if (channelArray.length == totalChannels) {
            int newSize = totalChannels * 2; // Make a larger array
            SelectionKeyImpl temp[] = new SelectionKeyImpl[newSize];
            System.arraycopy(channelArray, 1, temp, 1, totalChannels - 1);
            channelArray = temp;
            pollWrapper.grow(newSize);
        }
        if (totalChannels % MAX_SELECTABLE_FDS == 0) { // more threads needed
            pollWrapper.addWakeupSocket(wakeupSourceFd, totalChannels);
            totalChannels++;
            threadsCount++;
        }
    }

    @Override
    protected void implDereg(SelectionKeyImpl ski) {
        assert !ski.isValid();
        assert Thread.holdsLock(this);

        if (fdMap.remove(ski) != null) {
            int i = ski.getIndex();
            assert (i >= 0);

            if (i != totalChannels - 1) {
                // Copy end one over it
                SelectionKeyImpl endChannel = channelArray[totalChannels-1];
                channelArray[i] = endChannel;
                endChannel.setIndex(i);
                pollWrapper.replaceEntry(pollWrapper, totalChannels-1, pollWrapper, i);
            }
            ski.setIndex(-1);

            channelArray[totalChannels - 1] = null;
            totalChannels--;
            if (totalChannels != 1 && totalChannels % MAX_SELECTABLE_FDS == 1) {
                totalChannels--;
                threadsCount--; // The last thread has become redundant.
            }
        }
    }

    @Override
    public void setEventOps(SelectionKeyImpl ski) {
        ensureOpen();
        synchronized (updateLock) {
            updateKeys.addLast(ski);
        }
    }

    @Override
    public Selector wakeup() {
        synchronized (interruptLock) {
            if (!interruptTriggered) {
                setWakeupSocket();
                interruptTriggered = true;
            }
        }
        return this;
    }

    static {
        IOUtil.load();
    }
}
