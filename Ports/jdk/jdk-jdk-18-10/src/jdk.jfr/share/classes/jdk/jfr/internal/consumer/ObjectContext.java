/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.jfr.EventType;
import jdk.jfr.ValueDescriptor;

public final class ObjectContext {
    private final Map<ValueDescriptor, ObjectContext> contextLookup;
    private final TimeConverter timeConverter;

    public final EventType eventType;
    public final List<ValueDescriptor> fields;

    ObjectContext(EventType eventType, List<ValueDescriptor> fields, TimeConverter timeConverter) {
        this.contextLookup = new HashMap<>();
        this.eventType = eventType;
        this.fields = fields;
        this.timeConverter = timeConverter;
    }

    private ObjectContext(ObjectContext parent, ValueDescriptor descriptor) {
        this.eventType = parent.eventType;
        this.contextLookup = parent.contextLookup;
        this.timeConverter = parent.timeConverter;
        this.fields = descriptor.getFields();
    }

    public ObjectContext getInstance(ValueDescriptor descriptor) {
        ObjectContext context = contextLookup.get(descriptor);
        if (context == null) {
            context = new ObjectContext(this, descriptor);
            contextLookup.put(descriptor, context);
        }
        return context;
    }

    public long convertTimestamp(long ticks) {
        return timeConverter.convertTimestamp(ticks);
    }

    public long convertTimespan(long ticks) {
        return timeConverter.convertTimespan(ticks);
    }

    public ZoneId getZoneOffset() {
        return timeConverter.getZoneOffset();
    }
}
