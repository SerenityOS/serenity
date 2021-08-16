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

package sun.nio.fs;

import jdk.internal.misc.Unsafe;
import java.util.concurrent.ExecutionException;

/**
 * Base implementation of a task (typically native) that polls a memory location
 * during execution so that it may be aborted/cancelled before completion. The
 * task is executed by invoking the {@link runInterruptibly} method defined
 * here and cancelled by invoking Thread.interrupt.
 */

abstract class Cancellable implements Runnable {
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    private final long pollingAddress;
    private final Object lock = new Object();

    // the following require lock when examining or changing
    private boolean completed;
    private Throwable exception;

    protected Cancellable() {
        pollingAddress = unsafe.allocateMemory(4);
        unsafe.putIntVolatile(null, pollingAddress, 0);
    }

    /**
     * Returns the memory address of a 4-byte int that should be polled to
     * detect cancellation.
     */
    protected long addressToPollForCancel() {
        return pollingAddress;
    }

    /**
     * The value to write to the polled memory location to indicate that the
     * task has been cancelled. If this method is not overridden then it
     * defaults to MAX_VALUE.
     */
    protected int cancelValue() {
        return Integer.MAX_VALUE;
    }

    /**
     * "cancels" the task by writing bits into memory location that it polled
     * by the task.
     */
    final void cancel() {
        synchronized (lock) {
            if (!completed) {
                unsafe.putIntVolatile(null, pollingAddress, cancelValue());
            }
        }
    }

    /**
     * Returns the exception thrown by the task or null if the task completed
     * successfully.
     */
    private Throwable exception() {
        synchronized (lock) {
            return exception;
        }
    }

    @Override
    public final void run() {
        try {
            implRun();
        } catch (Throwable t) {
            synchronized (lock) {
                exception = t;
            }
        } finally {
            synchronized (lock) {
                completed = true;
                unsafe.freeMemory(pollingAddress);
            }
        }
    }

    /**
     * The task body. This should periodically poll the memory location
     * to check for cancellation.
     */
    abstract void implRun() throws Throwable;

    /**
     * Invokes the given task in its own thread. If this (meaning the current)
     * thread is interrupted then an attempt is make to cancel the background
     * thread by writing into the memory location that it polls cooperatively.
     */
    static void runInterruptibly(Cancellable task) throws ExecutionException {
        Thread t = new Thread(null, task, "NIO-Task", 0, false);
        t.start();
        boolean cancelledByInterrupt = false;
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                cancelledByInterrupt = true;
                task.cancel();
            }
        }
        if (cancelledByInterrupt)
            Thread.currentThread().interrupt();
        Throwable exc = task.exception();
        if (exc != null)
            throw new ExecutionException(exc);
    }
}
