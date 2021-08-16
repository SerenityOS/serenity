/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import jdk.internal.net.http.hpack.HPACK.Logger;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/*
 * Adds reverse lookup to SimpleHeaderTable. Separated from SimpleHeaderTable
 * for performance reasons. Decoder does not need this functionality. On the
 * other hand, Encoder does.
 */
final class HeaderTable extends SimpleHeaderTable {

    //
    // To quickly find an index of an entry in the dynamic table with the given
    // contents an effective inverse mapping is needed. Here's a simple idea
    // behind such a mapping.
    //
    // # The problem:
    //
    // We have a queue with an O(1) lookup by index:
    //
    //     get: index -> x
    //
    // What we want is an O(1) reverse lookup:
    //
    //     indexOf: x -> index
    //
    // # Solution:
    //
    // Let's store an inverse mapping in a Map<x, Integer>. This have a problem
    // that when a new element is added to the queue, all indexes in the map
    // become invalid. Namely, the new element is assigned with an index of 1,
    // and each index i, i > 1 becomes shifted by 1 to the left:
    //
    //     1, 1, 2, 3, ... , n-1, n
    //
    // Re-establishing the invariant would seem to require a pass through the
    // map incrementing all indexes (map values) by 1, which is O(n).
    //
    // The good news is we can do much better then this!
    //
    // Let's create a single field of type long, called 'counter'. Then each
    // time a new element 'x' is added to the queue, a value of this field gets
    // incremented. Then the resulting value of the 'counter_x' is then put as a
    // value under key 'x' to the map:
    //
    //    map.put(x, counter_x)
    //
    // It gives us a map that maps an element to a value the counter had at the
    // time the element had been added.
    //
    // In order to retrieve an index of any element 'x' in the queue (at any
    // given time) we simply need to subtract the value (the snapshot of the
    // counter at the time when the 'x' was added) from the current value of the
    // counter. This operation basically answers the question:
    //
    //     How many elements ago 'x' was the tail of the queue?
    //
    // Which is the same as its index in the queue now. Given, of course, it's
    // still in the queue.
    //
    // I'm pretty sure in a real life long overflow will never happen, so it's
    // not too practical to add recalibrating code, but a pedantic person might
    // want to do so:
    //
    //     if (counter == Long.MAX_VALUE) {
    //         recalibrate();
    //     }
    //
    // Where 'recalibrate()' goes through the table doing this:
    //
    //     value -= counter
    //
    // That's given, of course, the size of the table itself is less than
    // Long.MAX_VALUE :-)
    //

    /* An immutable map of static header fields' indexes */
    private static final Map<String, Map<String, Integer>> staticIndexes;

    static {
        Map<String, Map<String, Integer>> map
                = new HashMap<>(STATIC_TABLE_LENGTH);
        for (int i = 1; i <= STATIC_TABLE_LENGTH; i++) {
            HeaderField f = staticTable.get(i);
            Map<String, Integer> values
                    = map.computeIfAbsent(f.name, k -> new HashMap<>());
            values.put(f.value, i);
        }
        // create an immutable deep copy
        Map<String, Map<String, Integer>> copy = new HashMap<>(map.size());
        for (Map.Entry<String, Map<String, Integer>> e : map.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        staticIndexes = Map.copyOf(copy);
    }

    //                name  ->    (value ->    [index])
    private final Map<String, Map<String, Deque<Long>>> map;
    private long counter = 1;

    public HeaderTable(int maxSize, Logger logger) {
        super(maxSize, logger);
        map = new HashMap<>();
    }

    //
    // This method returns:
    //
    // * a positive integer i where i (i = [1..Integer.MAX_VALUE]) is an
    // index of an entry with a header (n, v), where n.equals(name) &&
    // v.equals(value)
    //
    // * a negative integer j where j (j = [-Integer.MAX_VALUE..-1]) is an
    // index of an entry with a header (n, v), where n.equals(name)
    //
    // * 0 if there's no entry e such that e.getName().equals(name)
    //
    // The rationale behind this design is to allow to pack more useful data
    // into a single invocation, facilitating a single pass where possible
    // (the idea is the same as in java.util.Arrays.binarySearch(int[], int)).
    //
    public int indexOf(CharSequence name, CharSequence value) {
        // Invoking toString() will possibly allocate Strings for the sake of
        // the search, which doesn't feel right.
        String n = name.toString();
        String v = value.toString();

        // 1. Try exact match in the static region
        Map<String, Integer> values = staticIndexes.get(n);
        if (values != null) {
            Integer idx = values.get(v);
            if (idx != null) {
                return idx;
            }
        }
        // 2. Try exact match in the dynamic region
        int didx = search(n, v);
        if (didx > 0) {
            return STATIC_TABLE_LENGTH + didx;
        } else if (didx < 0) {
            if (values != null) {
                // 3. Return name match from the static region
                return -values.values().iterator().next(); // Iterator allocation
            } else {
                // 4. Return name match from the dynamic region
                return -STATIC_TABLE_LENGTH + didx;
            }
        } else {
            if (values != null) {
                // 3. Return name match from the static region
                return -values.values().iterator().next(); // Iterator allocation
            } else {
                return 0;
            }
        }
    }

    @Override
    protected void add(HeaderField f) {
        super.add(f);
        Map<String, Deque<Long>> values = map.computeIfAbsent(f.name, k -> new HashMap<>());
        Deque<Long> indexes = values.computeIfAbsent(f.value, k -> new LinkedList<>());
        long counterSnapshot = counter++;
        indexes.add(counterSnapshot);
        assert indexesUniqueAndOrdered(indexes);
    }

    private boolean indexesUniqueAndOrdered(Deque<Long> indexes) {
        long maxIndexSoFar = -1;
        for (long l : indexes) {
            if (l <= maxIndexSoFar) {
                return false;
            } else {
                maxIndexSoFar = l;
            }
        }
        return true;
    }

    int search(String name, String value) {
        Map<String, Deque<Long>> values = map.get(name);
        if (values == null) {
            return 0;
        }
        Deque<Long> indexes = values.get(value);
        if (indexes != null) {
            return (int) (counter - indexes.peekLast());
        } else {
            assert !values.isEmpty();
            Long any = values.values().iterator().next().peekLast(); // Iterator allocation
            return -(int) (counter - any);
        }
    }

    @Override
    protected HeaderField remove() {
        HeaderField f = super.remove();
        Map<String, Deque<Long>> values = map.get(f.name);
        Deque<Long> indexes = values.get(f.value);
        Long index = indexes.pollFirst();
        if (indexes.isEmpty()) {
            values.remove(f.value);
        }
        assert index != null;
        if (values.isEmpty()) {
            map.remove(f.name);
        }
        return f;
    }
}
