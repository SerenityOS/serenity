/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */
package com.sun.hotspot.igv.data;

import java.util.Comparator;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputEdge {

    public enum State {
        IMMUTABLE,
        SAME,
        NEW,
        DELETED
    }

    public static final Comparator<InputEdge> OUTGOING_COMPARATOR = new Comparator<InputEdge>(){

        @Override
            public int compare(InputEdge o1, InputEdge o2) {
                if(o1.getFromIndex() == o2.getFromIndex()) {
                    return o1.getTo() - o2.getTo();
                }
                return o1.getFromIndex() - o2.getFromIndex();
            }
    };

    public static final Comparator<InputEdge> INGOING_COMPARATOR = new Comparator<InputEdge>(){

        @Override
            public int compare(InputEdge o1, InputEdge o2) {
                if(o1.getToIndex() == o2.getToIndex()) {
                    return o1.getFrom() - o2.getFrom();
                }
                return o1.getToIndex() - o2.getToIndex();
            }
    };

    private final char toIndex;
    private final char fromIndex;
    private final int from;
    private final int to;
    private final String label;
    private final String type;
    private State state;

    public InputEdge(char toIndex, int from, int to) {
        this((char) 0, toIndex, from, to, null, null);
    }

    public InputEdge(char fromIndex, char toIndex, int from, int to) {
        this(fromIndex, toIndex, from, to, null, null);
    }

    public InputEdge(char fromIndex, char toIndex, int from, int to, String label, String type) {
        this.toIndex = toIndex;
        this.fromIndex = fromIndex;
        this.from = from;
        this.to = to;
        this.state = State.SAME;
        this.label = label;
        this.type = type.intern();
    }

    static WeakHashMap<InputEdge, WeakReference<InputEdge>> immutableCache = new WeakHashMap<>();

    public static synchronized InputEdge createImmutable(char fromIndex, char toIndex, int from, int to, String label, String type) {
        InputEdge edge = new InputEdge(fromIndex, toIndex, from, to, label, type, State.IMMUTABLE);
        WeakReference<InputEdge> result = immutableCache.get(edge);
        if (result != null) {
            InputEdge edge2 = result.get();
            if (edge2 != null) {
                return edge2;
            }
        }
        immutableCache.put(edge, new WeakReference<>(edge));
        return edge;
    }

    public InputEdge(char fromIndex, char toIndex, int from, int to, String label, String type, State state) {
        this.toIndex = toIndex;
        this.fromIndex = fromIndex;
        this.from = from;
        this.to = to;
        this.state = state;
        this.label = label;
        this.type = type;
    }

    public State getState() {
        return state;
    }

    public void setState(State x) {
        if (state == State.IMMUTABLE) {
            throw new InternalError("Can't change immutable instances");
        }
        this.state = x;
    }

    public char getToIndex() {
        return toIndex;
    }

    public char getFromIndex() {
        return fromIndex;
    }

    public String getName() {
        return "in" + toIndex;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof InputEdge)) {
            return false;
        }
        InputEdge conn2 = (InputEdge) o;
        boolean result = conn2.fromIndex == fromIndex && conn2.toIndex == toIndex && conn2.from == from && conn2.to == to;
        if (result && (state == State.IMMUTABLE || conn2.state == State.IMMUTABLE)) {
            // Immutable instances must be exactly the same
            return conn2.label == label && conn2.state == state;
        }
        return result;
    }

    @Override
    public String toString() {
        return "Edge from " + from + " to " + to + "(" + (int) fromIndex + ", " + (int) toIndex + ") ";
    }

    @Override
    public int hashCode() {
        return (from << 20 | to << 8 | toIndex << 4 | fromIndex);
    }
}
