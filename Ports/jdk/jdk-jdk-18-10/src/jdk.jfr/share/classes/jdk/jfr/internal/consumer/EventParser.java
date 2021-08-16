/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.jfr.internal.EventInstrumentation.FIELD_DURATION;

import java.io.IOException;
import java.util.List;

import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;

/**
 * Parses an event and returns a {@link RecordedEvent}.
 *
 */
final class EventParser extends Parser {

    private static final JdkJfrConsumer PRIVATE_ACCESS = JdkJfrConsumer.instance();

    private final Parser[] parsers;
    private final EventType eventType;
    private final TimeConverter timeConverter;
    private final boolean hasDuration;
    private final List<ValueDescriptor> valueDescriptors;
    private final int startIndex;
    private final int length;
    private final RecordedEvent unorderedEvent;
    private final ObjectContext objectContext;

    private RecordedEvent[] cached;
    private int cacheIndex;

    private boolean enabled = true;
    private boolean ordered;
    private long filterStart;
    private long filterEnd = Long.MAX_VALUE;
    private long thresholdNanos = -1;

    EventParser(TimeConverter timeConverter, EventType type, Parser[] parsers) {
        this.timeConverter = timeConverter;
        this.parsers = parsers;
        this.eventType = type;
        this.hasDuration = type.getField(FIELD_DURATION) != null;
        this.startIndex = hasDuration ? 2 : 1;
        this.length = parsers.length - startIndex;
        this.valueDescriptors = type.getFields();
        this.objectContext = new ObjectContext(type, valueDescriptors, timeConverter);
        this.unorderedEvent = PRIVATE_ACCESS.newRecordedEvent(objectContext, new Object[length], 0L, 0L);
    }

    private RecordedEvent cachedEvent() {
        if (ordered) {
            if (cacheIndex == cached.length) {
                RecordedEvent[] old = cached;
                cached = new RecordedEvent[cached.length * 2];
                System.arraycopy(old, 0, cached, 0, old.length);
            }
            RecordedEvent event = cached[cacheIndex];
            if (event == null) {
                event = PRIVATE_ACCESS.newRecordedEvent(objectContext, new Object[length], 0L, 0L);
                cached[cacheIndex] = event;
            }
            cacheIndex++;
            return event;
        } else {
            return unorderedEvent;
        }
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setThresholdNanos(long thresholdNanos) {
        this.thresholdNanos = thresholdNanos;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public RecordedEvent parse(RecordingInput input) throws IOException {
        if (!enabled) {
            return null;
        }

        long startTicks = input.readLong();
        long endTicks = startTicks;
        if (hasDuration) {
            long durationTicks = input.readLong();
            if (thresholdNanos > 0L) {
                if (timeConverter.convertTimespan(durationTicks) < thresholdNanos) {
                    return null;
                }
            }
            endTicks += durationTicks;
        }
        if (filterStart != 0L || filterEnd != Long.MAX_VALUE) {
            long eventEnd = timeConverter.convertTimestamp(endTicks);
            if (eventEnd < filterStart) {
                return null;
            }
            if (eventEnd > filterEnd) {
                return null;
            }
        }

        if (cached != null) {
            RecordedEvent event = cachedEvent();
            JdkJfrConsumer access = PRIVATE_ACCESS;
            access.setStartTicks(event, startTicks);
            access.setEndTicks(event, endTicks);
            Object[] values = access.eventValues(event);
            for (int i = 0; i < values.length; i++) {
                values[i] = parsers[startIndex + i].parse(input);
            }
            return event;
        }

        Object[] values = new Object[length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parsers[startIndex + i].parse(input);
        }
        return PRIVATE_ACCESS.newRecordedEvent(objectContext, values, startTicks, endTicks);
    }

    @Override
    public void skip(RecordingInput input) throws IOException {
        throw new InternalError("Should not call this method. More efficient to read event size and skip ahead");
    }

    public void resetCache() {
        cacheIndex = 0;
    }

    private boolean hasReuse() {
        return cached != null;
    }

    public void setReuse(boolean reuse) {
        if (reuse == hasReuse()) {
            return;
        }
        if (reuse) {
            cached = new RecordedEvent[2];
            cacheIndex = 0;
        } else {
            cached = null;
        }
    }

    public void setFilterStart(long filterStart) {
        this.filterStart = filterStart;
    }

    public void setFilterEnd(long filterEnd) {
        this.filterEnd = filterEnd;
    }

    public void setOrdered(boolean ordered) {
        if (this.ordered == ordered) {
            return;
        }
        this.ordered = ordered;
        this.cacheIndex = 0;
    }
}
