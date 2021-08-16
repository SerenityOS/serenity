/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;

/**
 * Event that contains information about event types and configurations.
 *
 * @since 16
 */
public final class MetadataEvent {
    private final List<EventType> current;
    private final List<EventType> previous;
    private final List<Configuration> configurations;
    private List<EventType> added;
    private List<EventType> removed;

    /* package private */
    MetadataEvent(List<EventType> previous, List<EventType> current, List<Configuration> configs) {
        this.previous = previous;
        this.current = current;
        this.configurations = configs;
    }

    /**
     * Returns a list of the current event types being used.
     *
     * @return an immutable list of event types, not {@code null}
     */
    public final List<EventType> getEventTypes() {
        return Collections.unmodifiableList(current);
    }

    /**
     * Returns a list of added event types since the last metadata event.
     * <p>
     * The delta will be from the last metadata event. If no metadata event has been
     * emitted earlier, all known event types will be in the list.
     *
     * @return an immutable list of added event types, not {@code null}
     */
    public final List<EventType> getAddedEventTypes() {
        if (added == null) {
            calculateDelta();
        }
        return added;
    }

    /**
     * Returns a list of removed event types since the last metadata event.
     * <p>
     * The delta will be from the last metadata event. If no metadata event has been
     * emitted earlier, the list will be empty.
     *
     * @return an immutable list of added event types, not {@code null}
     */
    public final List<EventType> getRemovedEventTypes() {
        if (removed == null) {
            calculateDelta();
        }
        return removed;
    }

    /**
     * Returns a list of configurations.
     *
     * @return an immutable list of configurations, not {@code null}
     */
    public List<Configuration> getConfigurations() {
        return configurations;
    }

    private void calculateDelta() {
        List<EventType> added = new ArrayList<>();
        Map<Long, EventType> previousSet = new HashMap<>(previous.size());
        for (EventType eventType : previous) {
            previousSet.put(eventType.getId(), eventType);
        }
        for (EventType eventType : current) {
            EventType t = previousSet.remove(eventType.getId());
            if (t == null) {
                added.add(eventType);
            }
        }
        this.removed = List.copyOf(previousSet.values());
        this.added = List.copyOf(added);
    }
}
