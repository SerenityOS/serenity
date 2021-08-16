/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.consumer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jdk.jfr.consumer.MetadataEvent;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.Utils;
import jdk.jfr.internal.consumer.Dispatcher.EventDispatcher;

final class StreamConfiguration {
    final List<Runnable> closeActions = new ArrayList<>();
    final List<Runnable> flushActions = new ArrayList<>();
    final List<EventDispatcher> eventActions = new ArrayList<>();
    final List<Consumer<Throwable>> errorActions = new ArrayList<>();
    final List<Consumer<MetadataEvent>> metadataActions = new ArrayList<>();

    boolean reuse = true;
    boolean ordered = true;
    Instant startTime = null;
    Instant endTime = null;
    boolean started = false;
    long startNanos = 0;
    long endNanos = Long.MAX_VALUE;

    private volatile boolean changed = true;

    public synchronized boolean remove(Object action) {
        boolean removed = false;
        removed |= flushActions.removeIf(e -> e == action);
        removed |= closeActions.removeIf(e -> e == action);
        removed |= errorActions.removeIf(e -> e == action);
        removed |= eventActions.removeIf(e -> e.getAction() == action);
        removed |= metadataActions.removeIf(e -> e == action);
        if (removed) {
            changed = true;
        }
        return removed;
    }

    public synchronized void addEventAction(String name, Consumer<RecordedEvent> consumer) {
        eventActions.add(new EventDispatcher(name, consumer));
        changed = true;
    }

    public void addEventAction(Consumer<RecordedEvent> action) {
        addEventAction(null, action);
    }

    public synchronized void addFlushAction(Runnable action) {
        flushActions.add(action);
        changed = true;
    }

    public synchronized void addCloseAction(Runnable action) {
        closeActions.add(action);
        changed = true;
    }

    public synchronized void addErrorAction(Consumer<Throwable> action) {
        errorActions.add(action);
        changed = true;
    }

    public synchronized void addMetadataAction(Consumer<MetadataEvent> action) {
        metadataActions.add(action);
        changed = true;
    }

    public synchronized void setReuse(boolean reuse) {
        this.reuse = reuse;
        changed = true;
    }

    public synchronized void setOrdered(boolean ordered) {
        this.ordered = ordered;
        changed = true;
    }

    public synchronized void setEndTime(Instant endTime) {
        this.endTime = endTime;
        this.endNanos = Utils.timeToNanos(endTime);
        changed = true;
    }

    public synchronized void setStartTime(Instant startTime) {
        this.startTime = startTime;
        this.startNanos = Utils.timeToNanos(startTime);
        changed = true;
    }

    public synchronized void setStartNanos(long startNanos) {
        this.startNanos = startNanos;
        changed = true;
    }

    public synchronized void setStarted(boolean started) {
        this.started = started;
        changed = true;
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
