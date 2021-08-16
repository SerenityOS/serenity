/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.util.Vector;
import sun.awt.AppContext;

/**
 * A queue of text layout tasks.
 *
 * @author  Timothy Prinzing
 * @see     AsyncBoxView
 * @since   1.3
 */
public class LayoutQueue {

    private static final Object DEFAULT_QUEUE = new Object();

    private Vector<Runnable> tasks;
    private Thread worker;

    /**
     * Construct a layout queue.
     */
    public LayoutQueue() {
        tasks = new Vector<Runnable>();
    }

    /**
     * Fetch the default layout queue.
     * @return the default layout queue
     */
    public static LayoutQueue getDefaultQueue() {
        AppContext ac = AppContext.getAppContext();
        synchronized (DEFAULT_QUEUE) {
            LayoutQueue defaultQueue = (LayoutQueue) ac.get(DEFAULT_QUEUE);
            if (defaultQueue == null) {
                defaultQueue = new LayoutQueue();
                ac.put(DEFAULT_QUEUE, defaultQueue);
            }
            return defaultQueue;
        }
    }

    /**
     * Set the default layout queue.
     *
     * @param q the new queue.
     */
    public static void setDefaultQueue(LayoutQueue q) {
        synchronized (DEFAULT_QUEUE) {
            AppContext.getAppContext().put(DEFAULT_QUEUE, q);
        }
    }

    /**
     * Add a task that is not needed immediately because
     * the results are not believed to be visible.
     * @param task the task to add to the queue
     */
    public synchronized void addTask(Runnable task) {
        if (worker == null) {
            Runnable workerRunnable = () -> {
                Runnable work;
                do {
                    work = waitForWork();
                    if (work != null) {
                        work.run();
                    }
                } while (work != null);
            };
            worker = new Thread(null, workerRunnable, "text-layout", 0, false);
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        }
        tasks.addElement(task);
        notifyAll();
    }

    /**
     * Used by the worker thread to get a new task to execute.
     * @return a task from the queue
     */
    protected synchronized Runnable waitForWork() {
        while (tasks.size() == 0) {
            try {
                wait();
            } catch (InterruptedException ie) {
                return null;
            }
        }
        Runnable work = tasks.firstElement();
        tasks.removeElementAt(0);
        return work;
    }
}
