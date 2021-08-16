/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.util.Arrays;
import java.util.Collection;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler.Handler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;

/**
 * A collection of currently registered {@link TaskListener}s. Events passed to this TaskListener
 * will be forwarded to all the registered TaskListeners.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class MultiTaskListener implements TaskListener {
    /** The context key for the MultiTaskListener. */
    public static final Context.Key<MultiTaskListener> taskListenerKey = new Context.Key<>();

    /** Empty array of task listeners */
    private static final TaskListener[] EMPTY_LISTENERS = new TaskListener[0];

    private final DeferredCompletionFailureHandler dcfh;

    /** Get the MultiTaskListener instance for this context. */
    public static MultiTaskListener instance(Context context) {
        MultiTaskListener instance = context.get(taskListenerKey);
        if (instance == null)
            instance = new MultiTaskListener(context);
        return instance;
    }

    protected MultiTaskListener(Context context) {
        context.put(taskListenerKey, this);
        ccw = ClientCodeWrapper.instance(context);
        dcfh = DeferredCompletionFailureHandler.instance(context);
    }

    /**
     * The current set of registered listeners.
     * This is a mutable reference to an immutable array.
     */
    TaskListener[] listeners = EMPTY_LISTENERS;

    ClientCodeWrapper ccw;

    public Collection<TaskListener> getTaskListeners() {
        return Arrays.asList(listeners);
    }

    public boolean isEmpty() {
        return listeners == EMPTY_LISTENERS;
    }

    public void add(TaskListener listener) {
        for (TaskListener l: listeners) {
            if (ccw.unwrap(l) == listener)
                throw new IllegalStateException();
        }
        listeners = Arrays.copyOf(listeners, listeners.length + 1);
        listeners[listeners.length - 1] = ccw.wrap(listener);
    }

    public void remove(TaskListener listener) {
        for (int i = 0; i < listeners.length; i++) {
            if (ccw.unwrap(listeners[i]) == listener) {
                if (listeners.length == 1) {
                    listeners = EMPTY_LISTENERS;
                } else {
                    TaskListener[] newListeners = new TaskListener[listeners.length - 1];
                    System.arraycopy(listeners, 0, newListeners, 0, i);
                    System.arraycopy(listeners, i + 1, newListeners, i, newListeners.length - i);
                    listeners = newListeners;
                }
                break;
            }
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void started(TaskEvent e) {
        Handler prevDeferredHandler = dcfh.setHandler(dcfh.userCodeHandler);
        try {
            // guard against listeners being updated by a listener
            TaskListener[] ll = this.listeners;
            for (TaskListener l: ll)
                l.started(e);
        } finally {
            dcfh.setHandler(prevDeferredHandler);
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void finished(TaskEvent e) {
        Handler prevDeferredHandler = dcfh.setHandler(dcfh.userCodeHandler);
        try {
            // guard against listeners being updated by a listener
            TaskListener[] ll = this.listeners;
            for (TaskListener l: ll)
                l.finished(e);
        } finally {
            dcfh.setHandler(prevDeferredHandler);
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(listeners);
    }

    public void clear() {
        listeners = EMPTY_LISTENERS;
    }
}
