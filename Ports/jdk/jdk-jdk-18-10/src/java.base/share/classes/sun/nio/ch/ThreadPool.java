/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import sun.security.action.GetPropertyAction;
import sun.security.action.GetIntegerAction;
import jdk.internal.misc.InnocuousThread;

/**
 * Encapsulates a thread pool associated with a channel group.
 */

public class ThreadPool {
    private static final String DEFAULT_THREAD_POOL_THREAD_FACTORY =
        "java.nio.channels.DefaultThreadPool.threadFactory";
    private static final String DEFAULT_THREAD_POOL_INITIAL_SIZE =
        "java.nio.channels.DefaultThreadPool.initialSize";

    private final ExecutorService executor;

    // indicates if thread pool is fixed size
    private final boolean isFixed;

    // indicates the pool size (for a fixed thread pool configuratin this is
    // the maximum pool size; for other thread pools it is the initial size)
    private final int poolSize;

    private ThreadPool(ExecutorService executor,
                       boolean isFixed,
                       int poolSize)
    {
        this.executor = executor;
        this.isFixed = isFixed;
        this.poolSize = poolSize;
    }

    ExecutorService executor() {
        return executor;
    }

    boolean isFixedThreadPool() {
        return isFixed;
    }

    int poolSize() {
        return poolSize;
    }

    @SuppressWarnings("removal")
    static ThreadFactory defaultThreadFactory() {
        if (System.getSecurityManager() == null) {
            return (Runnable r) -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            };
        } else {
            return (Runnable r) -> {
                PrivilegedAction<Thread> action = () -> {
                    Thread t = InnocuousThread.newThread(r);
                    t.setDaemon(true);
                    return t;
               };
               return AccessController.doPrivileged(action);
           };
        }
    }

    private static class DefaultThreadPoolHolder {
        static final ThreadPool defaultThreadPool = createDefault();
    }

    // return the default (system-wide) thread pool
    static ThreadPool getDefault() {
        return DefaultThreadPoolHolder.defaultThreadPool;
    }

    // create thread using default settings (configured by system properties)
    static ThreadPool createDefault() {
        // default the number of fixed threads to the hardware core count
        int initialSize = getDefaultThreadPoolInitialSize();
        if (initialSize < 0)
            initialSize = Runtime.getRuntime().availableProcessors();
        // default to thread factory that creates daemon threads
        ThreadFactory threadFactory = getDefaultThreadPoolThreadFactory();
        if (threadFactory == null)
            threadFactory = defaultThreadFactory();
        // create thread pool
        ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
        return new ThreadPool(executor, false, initialSize);
    }

    // create using given parameters
    static ThreadPool create(int nThreads, ThreadFactory factory) {
        if (nThreads <= 0)
            throw new IllegalArgumentException("'nThreads' must be > 0");
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, factory);
        return new ThreadPool(executor, true, nThreads);
    }

    // wrap a user-supplied executor
    public static ThreadPool wrap(ExecutorService executor, int initialSize) {
        if (executor == null)
            throw new NullPointerException("'executor' is null");
        // attempt to check if cached thread pool
        if (executor instanceof ThreadPoolExecutor) {
            int max = ((ThreadPoolExecutor)executor).getMaximumPoolSize();
            if (max == Integer.MAX_VALUE) {
                if (initialSize < 0) {
                    initialSize = Runtime.getRuntime().availableProcessors();
                } else {
                   // not a cached thread pool so ignore initial size
                    initialSize = 0;
                }
            }
        } else {
            // some other type of thread pool
            if (initialSize < 0)
                initialSize = 0;
        }
        return new ThreadPool(executor, false, initialSize);
    }

    private static int getDefaultThreadPoolInitialSize() {
        @SuppressWarnings("removal")
        String propValue = AccessController.doPrivileged(new
            GetPropertyAction(DEFAULT_THREAD_POOL_INITIAL_SIZE));
        if (propValue != null) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException x) {
                throw new Error("Value of property '" + DEFAULT_THREAD_POOL_INITIAL_SIZE +
                    "' is invalid: " + x);
            }
        }
        return -1;
    }

    private static ThreadFactory getDefaultThreadPoolThreadFactory() {
        @SuppressWarnings("removal")
        String propValue = AccessController.doPrivileged(new
            GetPropertyAction(DEFAULT_THREAD_POOL_THREAD_FACTORY));
        if (propValue != null) {
            try {
                @SuppressWarnings("deprecation")
                Object tmp = Class
                    .forName(propValue, true, ClassLoader.getSystemClassLoader()).newInstance();
                return (ThreadFactory)tmp;
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException x) {
                throw new Error(x);
            }
        }
        return null;
    }
}
