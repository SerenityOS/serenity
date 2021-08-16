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

import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;

import jdk.jfr.internal.LongMap;

/**
 * Holds mapping between a set of keys and their corresponding object.
 *
 * If the type is a known type, i.e. {@link jdk.jfr.consumer.RecordedThread}, an
 * {@link ObjectFactory} can be supplied which will instantiate a typed object.
 */
final class ConstantMap {

    private static final int RESOLUTION_FINISHED = 0;
    private static final int RESOLUTION_STARTED = 1;
    public static final ConstantMap EMPTY = new ConstantMap();

    // A temporary placeholder, so objects can
    // reference themselves (directly, or indirectly),
    // when making a transition from numeric id references
    // to normal Java references.
    private static final class Reference {
        private final long key;
        private final ConstantMap pool;

        Reference(ConstantMap pool, long key) {
            this.pool = pool;
            this.key = key;
        }

        Object resolve() {
            return pool.get(key);
        }

        @Override
        public String toString() {
            return "ref: " + pool.name + "[" + key + "]";
        }
    }

    final ObjectFactory<?> factory;
    final String name;

    private final LongMap<Object> objects;

    private boolean resolving;
    private boolean allResolved;

    private ConstantMap() {
        this(null, "<empty>");
        allResolved = true;
    }

    ConstantMap(ObjectFactory<?> factory, String name) {
        this.name = name;
        this.objects = new LongMap<>(2);
        this.factory = factory;
    }

    Object get(long id) {
        // fast path, all objects in pool resolved
        if (allResolved) {
            return objects.get(id);
        }
        // referenced from a pool, deal with this later
        if (!resolving) {
            return new Reference(this, id);
        }

        // should ideally always have a value
        Object value = objects.get(id);
        if (value == null) {
            // unless id is 0 which is used to represent null
            if (id != 0) {
                Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Missing object id=" + id + " in pool " + name + ". All ids should reference an object");
            }
            return null;
        }

        // id is resolved (but not the whole pool)
        if (objects.isSetId(id, RESOLUTION_FINISHED)) {
            return value;
        }

        // resolving ourself, abort to avoid infinite recursion
        if (objects.isSetId(id, RESOLUTION_STARTED)) {
            return null;
        }

        // mark id as STARTED if we should
        // come back during object resolution
        objects.setId(id, RESOLUTION_STARTED);

        // resolve object!
        Object resolved = resolve(value);

        // mark id as FINISHED
        objects.setId(id, RESOLUTION_FINISHED);

        // if a factory exists, convert to RecordedThread.
        // RecordedClass etc. and store back results
        if (factory != null) {
            Object factorized = factory.createObject(id, resolved);
            objects.put(id, factorized);
            return factorized;
        } else {
            objects.put(id, resolved);
            return resolved;
        }
    }

    private static Object resolve(Object o) {
        if (o instanceof Reference r) {
            return resolve(r.resolve());
        }
        if (o != null && o.getClass().isArray()) {
            final Object[] array = (Object[]) o;
            for (int i = 0; i < array.length; i++) {
                Object element = array[i];
                array[i] = resolve(element);
            }
            return array;
        }
        return o;
    }

    public void resolve() {
        objects.forEachKey(k -> get(k));
    }

    public void put(long key, Object value) {
        if (objects.hasKey(key)) {
            objects.clearId(key);
        }
        objects.put(key, value);
    }

    public void setResolving() {
        resolving = true;
        allResolved = false;
    }

    public void setResolved() {
        allResolved = true;
        resolving = false;
    }

    public String getName() {
        return name;
    }

    public Object getResolved(long id) {
        return objects.get(id);
    }

    public void putResolved(long id, Object object) {
        objects.put(id, object);
        objects.setId(id, RESOLUTION_FINISHED);
    }

    public void setAllResolved(boolean allResolved) {
        this.allResolved = allResolved;
    }
}
