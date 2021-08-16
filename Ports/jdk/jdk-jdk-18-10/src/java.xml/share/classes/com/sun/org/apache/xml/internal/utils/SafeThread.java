/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.org.apache.xml.internal.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a safe thread that does not inherit thread-locals and runs only
 * once.
 */
public class SafeThread extends Thread {
    private volatile boolean ran = false;

    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    private static String threadName() {
        return "SafeThread-" + threadNumber.getAndIncrement();
    }

    public SafeThread(Runnable target) {
        this(null, target, threadName());
    }

    public SafeThread(Runnable target, String name) {
        this(null, target, name);
    }

    public SafeThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name, 0, false);
    }

    public final void run() {
        if (Thread.currentThread() != this) {
            throw new IllegalStateException("The run() method in a"
                    + " SafeThread cannot be called from another thread.");
        }
        synchronized (this) {
            if (!ran) {
                ran = true;
            } else {
                throw new IllegalStateException("The run() method in a"
                        + " SafeThread cannot be called more than once.");
            }
        }
        super.run();
    }
}
