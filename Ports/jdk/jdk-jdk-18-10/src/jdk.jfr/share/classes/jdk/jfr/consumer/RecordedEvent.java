/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.internal.EventInstrumentation;
import jdk.jfr.internal.consumer.ObjectContext;

/**
 * A recorded event.
 *
 * @since 9
 */
public final class RecordedEvent extends RecordedObject {
    long startTimeTicks;
    long endTimeTicks;

    // package private
    RecordedEvent(ObjectContext objectContext, Object[] values, long startTimeTicks, long endTimeTicks) {
        super(objectContext, values);
        this.startTimeTicks = startTimeTicks;
        this.endTimeTicks = endTimeTicks;
    }

    /**
     * Returns the stack trace that was created when the event was committed, or
     * {@code null} if the event lacks a stack trace.
     *
     * @return stack trace, or {@code null} if doesn't exist for the event
     */
    public RecordedStackTrace getStackTrace() {
        return getTyped(EventInstrumentation.FIELD_STACK_TRACE, RecordedStackTrace.class, null);
    }

    /**
     * Returns the thread from which the event was committed, or {@code null} if
     * the thread was not recorded.
     *
     * @return thread, or {@code null} if doesn't exist for the event
     */
    public RecordedThread getThread() {
        return getTyped(EventInstrumentation.FIELD_EVENT_THREAD, RecordedThread.class, null);
    }

    /**
     * Returns the event type that describes the event.
     *
     * @return the event type, not {@code null}
     */
    public EventType getEventType() {
        return objectContext.eventType;
    }

    /**
     * Returns the start time of the event.
     * <p>
     * If the event is an instant event, then the start time and end time are the same.
     *
     * @return the start time, not {@code null}
     */
    public Instant getStartTime() {
        return Instant.ofEpochSecond(0, getStartTimeNanos());
    }

    /**
     * Returns the end time of the event.
     * <p>
     * If the event is an instant event, then the start time and end time are the same.
     *
     * @return the end time, not {@code null}
     */
    public Instant getEndTime() {
        return Instant.ofEpochSecond(0, getEndTimeNanos());
    }

    /**
     * Returns the duration of the event, measured in nanoseconds.
     *
     * @return the duration in nanoseconds, not {@code null}
     */
    public Duration getDuration() {
        return Duration.ofNanos(getEndTimeNanos() - getStartTimeNanos());
    }

    /**
     * Returns the list of descriptors that describes the fields of the event.
     *
     * @return descriptors, not {@code null}
     */
    @Override
    public List<ValueDescriptor> getFields() {
        return objectContext.fields;
    }

    @Override
    final Object objectAt(int index) {
        if (index == 0) {
            return startTimeTicks;
        }
        if (hasDuration()) {
            if (index == 1) {
                return endTimeTicks - startTimeTicks;
            }
            return objects[index - 2];
        }
        return objects[index - 1];
    }

    private boolean hasDuration() {
        return objects.length + 2 == objectContext.fields.size();
    }

    private long getStartTimeNanos() {
        return objectContext.convertTimestamp(startTimeTicks);
    }

    private long getEndTimeNanos() {
        return objectContext.convertTimestamp(endTimeTicks);
    }
}
