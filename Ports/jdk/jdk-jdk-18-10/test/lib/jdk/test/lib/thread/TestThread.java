/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.thread;

import java.util.concurrent.TimeoutException;

/**
 * Thread which catches exceptions thrown during the execution
 * and stores them for later analysis.
 *
 * <pre>
 * {@code
 * TestThread thread = new TestThread(new XRun() {
 *      public void run() {
 *      // do something
 *      }
 * });
 * thread.start();
 * // do something
 * Throwable uncaught = thread.getUncaught();
 * }
 * </pre>
 */
public class TestThread extends Thread {

    private final Runnable runnable;
    private volatile Throwable uncaught;

    /**
     * Returns {@link Runnable} the thread has been created with.
     *
     * @return The object whose {@code run} method is called
     */
    public Runnable getRunnable() {
        return runnable;
    }

    /**
     * Creates a new {@code TestThread} object.
     *
     * @param target The object whose {@code run} method is called
     * @param name The thread name
     */
    public TestThread(Runnable target, String name) {
        super(target, name);
        this.runnable = target;
    }

    /**
     * Creates a new {@code TestThread} object.
     *
     * @param target The object whose {@code run} method is called
     */
    public TestThread(Runnable target) {
        super(target);
        this.runnable = target;
    }

    /**
     * Creates a new {@code TestThread} object.
     *
     * @param group The thread group
     * @param target The object whose {@code run} method is called
     * @param name The thread name
     * @param stackSize Stack size
     */
    public TestThread(ThreadGroup group, Runnable target, String name,
            long stackSize) {
        super(group, target, name, stackSize);
        this.runnable = target;
    }

    /**
     * Creates a new {@code TestThread} object.
     *
     * @param group The thread group
     * @param target The object whose {@code run} method is called
     * @param name The thread name
     */
    public TestThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
        this.runnable = target;
    }

    /**
     * Creates a new {@code TestThread} object.
     *
     * @param group The thread group
     * @param target The object whose {@code run} method is called
     */
    public TestThread(ThreadGroup group, Runnable target) {
        super(group, target);
        this.runnable = target;
    }

    /**
     * The thread executor.
     */
    @Override
    public void run() {
        try {
            super.run();
        } catch (Throwable t) {
            uncaught = t;
        }
    }

    /**
     * Returns exception caught during the execution.
     *
     * @return {@link Throwable}
     */
    public Throwable getUncaught() {
        return uncaught;
    }

    /**
     * Waits for {@link TestThread} to die
     * and throws exception caught during the execution.
     *
     * @throws InterruptedException
     * @throws Throwable
     */
    public void joinAndThrow() throws InterruptedException, Throwable {
        join();
        if (uncaught != null) {
            throw uncaught;
        }
    }

    /**
     * Waits during {@code timeout} for {@link TestThread} to die
     * and throws exception caught during the execution.
     *
     * @param timeout The time to wait in milliseconds
     * @throws InterruptedException
     * @throws Throwable
     */
    public void joinAndThrow(long timeout) throws InterruptedException,
            Throwable {
        join(timeout);
        if (isAlive()) {
            throw new TimeoutException();
        }
        if (uncaught != null) {
            throw uncaught;
        }
    }

    /**
     * Waits for {@link TestThread} to die
     * and returns exception caught during the execution.
     *
     * @return Exception caught during the execution
     * @throws InterruptedException
     */
    public Throwable joinAndReturn() throws InterruptedException {
        join();
        if (uncaught != null) {
            return uncaught;
        }
        return null;
    }

    /**
     * Waits during {@code timeout} for {@link TestThread} to die
     * and returns exception caught during the execution.
     *
     * @param timeout The time to wait in milliseconds
     * @return Exception caught during the execution
     * @throws InterruptedException
     */
    public Throwable joinAndReturn(long timeout) throws InterruptedException {
        join(timeout);
        if (isAlive()) {
            return new TimeoutException();
        }
        if (uncaught != null) {
            return uncaught;
        }
        return null;
    }
}
