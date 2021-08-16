/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.ThreadGroupReference;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.StackFrame;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

class ThreadInfo {
    // This is a list of all known ThreadInfo objects. It survives
    // ThreadInfo.invalidateAll, unlike the other static fields below.
    private static List<ThreadInfo> threads = Collections.synchronizedList(new ArrayList<ThreadInfo>());
    private static boolean gotInitialThreads = false;

    private static ThreadInfo current = null;
    private static ThreadGroupReference group = null;

    private final ThreadReference thread;
    private int currentFrameIndex = 0;

    private ThreadInfo(ThreadReference thread) {
        this.thread = thread;
        if (thread == null) {
            MessageOutput.fatalError("Internal error: null ThreadInfo created");
        }
    }

    private static void initThreads() {
        if (!gotInitialThreads) {
            for (ThreadReference thread : Env.vm().allThreads()) {
                threads.add(new ThreadInfo(thread));
            }
            gotInitialThreads = true;
        }
    }

    static void addThread(ThreadReference thread) {
        synchronized (threads) {
            initThreads();
            ThreadInfo ti = new ThreadInfo(thread);
            // Guard against duplicates. Duplicates can happen during
            // initialization when a particular thread might be added both
            // by a thread start event and by the initial call to threads()
            if (getThreadInfo(thread) == null) {
                threads.add(ti);
            }
        }
    }

    static void removeThread(ThreadReference thread) {
        if (thread.equals(ThreadInfo.current)) {
            // Current thread has died.

            // Be careful getting the thread name. If its death happens
            // as part of VM termination, it may be too late to get the
            // information, and an exception will be thrown.
            String currentThreadName;
            try {
               currentThreadName = "\"" + thread.name() + "\"";
            } catch (Exception e) {
               currentThreadName = "";
            }

            setCurrentThread(null);

            MessageOutput.println();
            MessageOutput.println("Current thread died. Execution continuing...",
                                  currentThreadName);
        }
        threads.remove(getThreadInfo(thread));
    }

    static List<ThreadInfo> threads() {
        synchronized(threads) {
            initThreads();
            // Make a copy to allow iteration without synchronization
            return new ArrayList<ThreadInfo>(threads);
        }
    }

    static void invalidateAll() {
        current = null;
        group = null;
        synchronized (threads) {
            for (ThreadInfo ti : threads()) {
                ti.invalidate();
            }
        }
    }

    static void setThreadGroup(ThreadGroupReference tg) {
        group = tg;
    }

    static void setCurrentThread(ThreadReference tr) {
        if (tr == null) {
            setCurrentThreadInfo(null);
        } else {
            ThreadInfo tinfo = getThreadInfo(tr);
            setCurrentThreadInfo(tinfo);
        }
    }

    static void setCurrentThreadInfo(ThreadInfo tinfo) {
        current = tinfo;
        if (current != null) {
            current.invalidate();
        }
    }

    /**
     * Get the current ThreadInfo object.
     *
     * @return the ThreadInfo for the current thread.
     */
    static ThreadInfo getCurrentThreadInfo() {
        return current;
    }

    /**
     * Get the thread from this ThreadInfo object.
     *
     * @return the Thread wrapped by this ThreadInfo.
     */
    ThreadReference getThread() {
        return thread;
    }

    static ThreadGroupReference group() {
        if (group == null) {
            // Current thread group defaults to the first top level
            // thread group.
            setThreadGroup(Env.vm().topLevelThreadGroups().get(0));
        }
        return group;
    }

    static ThreadInfo getThreadInfo(long id) {
        ThreadInfo retInfo = null;

        synchronized (threads) {
            for (ThreadInfo ti : threads()) {
                if (ti.thread.uniqueID() == id) {
                   retInfo = ti;
                   break;
                }
            }
        }
        return retInfo;
    }

    static ThreadInfo getThreadInfo(ThreadReference tr) {
        return getThreadInfo(tr.uniqueID());
    }

    static ThreadInfo getThreadInfo(String idToken) {
        ThreadInfo tinfo = null;
        if (idToken.startsWith("t@")) {
            idToken = idToken.substring(2);
        }
        try {
            long threadId = Long.decode(idToken).longValue();
            tinfo = getThreadInfo(threadId);
        } catch (NumberFormatException e) {
            tinfo = null;
        }
        return tinfo;
    }

    /**
     * Get the thread stack frames.
     *
     * @return a <code>List</code> of the stack frames.
     */
    List<StackFrame> getStack() throws IncompatibleThreadStateException {
        return thread.frames();
    }

    /**
     * Get the current stackframe.
     *
     * @return the current stackframe.
     */
    StackFrame getCurrentFrame() throws IncompatibleThreadStateException {
        if (thread.frameCount() == 0) {
            return null;
        }
        return thread.frame(currentFrameIndex);
    }

    /**
     * Invalidate the current stackframe index.
     */
    void invalidate() {
        currentFrameIndex = 0;
    }

    /* Throw IncompatibleThreadStateException if not suspended */
    private void assureSuspended() throws IncompatibleThreadStateException {
        if (!thread.isSuspended()) {
            throw new IncompatibleThreadStateException();
        }
    }

    /**
     * Get the current stackframe index.
     *
     * @return the number of the current stackframe.  Frame zero is the
     * closest to the current program counter
     */
    int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    /**
     * Set the current stackframe to a specific frame.
     *
     * @param nFrame    the number of the desired stackframe.  Frame zero is the
     * closest to the current program counter
     * @exception IllegalAccessError when the thread isn't
     * suspended or waiting at a breakpoint
     * @exception ArrayIndexOutOfBoundsException when the
     * requested frame is beyond the stack boundary
     */
    void setCurrentFrameIndex(int nFrame) throws IncompatibleThreadStateException {
        assureSuspended();
        if ((nFrame < 0) || (nFrame >= thread.frameCount())) {
            throw new ArrayIndexOutOfBoundsException();
        }
        currentFrameIndex = nFrame;
    }

    /**
     * Change the current stackframe to be one or more frames higher
     * (as in, away from the current program counter).
     *
     * @param nFrames   the number of stackframes
     * @exception IllegalAccessError when the thread isn't
     * suspended or waiting at a breakpoint
     * @exception ArrayIndexOutOfBoundsException when the
     * requested frame is beyond the stack boundary
     */
    void up(int nFrames) throws IncompatibleThreadStateException {
        setCurrentFrameIndex(currentFrameIndex + nFrames);
    }

    /**
     * Change the current stackframe to be one or more frames lower
     * (as in, toward the current program counter).     *
     * @param nFrames   the number of stackframes
     * @exception IllegalAccessError when the thread isn't
     * suspended or waiting at a breakpoint
     * @exception ArrayIndexOutOfBoundsException when the
     * requested frame is beyond the stack boundary
     */
    void down(int nFrames) throws IncompatibleThreadStateException {
        setCurrentFrameIndex(currentFrameIndex - nFrames);
    }

}
