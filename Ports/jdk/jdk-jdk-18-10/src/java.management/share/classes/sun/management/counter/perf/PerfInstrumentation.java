/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.management.counter.perf;

import sun.management.counter.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

public class PerfInstrumentation {
    private ByteBuffer buffer;
    private Prologue prologue;
    private long lastModificationTime;
    private long lastUsed;
    private int  nextEntry;
    private SortedMap<String, Counter>  map;

    public PerfInstrumentation(ByteBuffer b) {
        prologue = new Prologue(b);
        buffer = b;
        buffer.order(prologue.getByteOrder());

        // Check recognized versions
        int major = getMajorVersion();
        int minor = getMinorVersion();

        // Support only 2.0 version
        if (major < 2) {
            throw new InstrumentationException("Unsupported version: " +
                                               major + "." + minor);
        }
        rewind();
    }

    public int getMajorVersion() {
        return prologue.getMajorVersion();
    }

    public int getMinorVersion() {
        return prologue.getMinorVersion();
    }

    public long getModificationTimeStamp() {
        return prologue.getModificationTimeStamp();
    }

    void rewind() {
        // rewind to the first entry
        buffer.rewind();
        buffer.position(prologue.getEntryOffset());
        nextEntry = buffer.position();
        // rebuild all the counters
        map = new TreeMap<>();
    }

    boolean hasNext() {
        return (nextEntry < prologue.getUsed());
    }

    Counter getNextCounter() {
        if (! hasNext()) {
            return null;
        }

        if ((nextEntry % 4) != 0) {
            // entries are always 4 byte aligned.
            throw new InstrumentationException(
                "Entry index not properly aligned: " + nextEntry);
        }

        if (nextEntry < 0  || nextEntry > buffer.limit()) {
            // defensive check to protect against a corrupted shared memory region.
            throw new InstrumentationException(
                "Entry index out of bounds: nextEntry = " + nextEntry +
                ", limit = " + buffer.limit());
        }

        buffer.position(nextEntry);
        PerfDataEntry entry = new PerfDataEntry(buffer);
        nextEntry = nextEntry + entry.size();

        Counter counter = null;
        PerfDataType type = entry.type();
        if (type == PerfDataType.BYTE) {
            if (entry.units() == Units.STRING && entry.vectorLength() > 0) {
                counter = new PerfStringCounter(entry.name(),
                                                entry.variability(),
                                                entry.flags(),
                                                entry.vectorLength(),
                                                entry.byteData());
            } else if (entry.vectorLength() > 0) {
                counter = new PerfByteArrayCounter(entry.name(),
                                                   entry.units(),
                                                   entry.variability(),
                                                   entry.flags(),
                                                   entry.vectorLength(),
                                                   entry.byteData());
           } else {
                // ByteArrayCounter must have vectorLength > 0
                assert false;
           }
        }
        else if (type == PerfDataType.LONG) {
            if (entry.vectorLength() == 0) {
                counter = new PerfLongCounter(entry.name(),
                                              entry.units(),
                                              entry.variability(),
                                              entry.flags(),
                                              entry.longData());
            } else {
                counter = new PerfLongArrayCounter(entry.name(),
                                                   entry.units(),
                                                   entry.variability(),
                                                   entry.flags(),
                                                   entry.vectorLength(),
                                                   entry.longData());
            }
        }
        else {
            // FIXME: Should we throw an exception for unsupported type?
            // Currently skip such entry
            assert false;
        }
        return counter;
    }

    public synchronized List<Counter> getAllCounters() {
        while (hasNext()) {
            Counter c = getNextCounter();
            if (c != null) {
                map.put(c.getName(), c);
            }
        }
        return new ArrayList<>(map.values());
    }

    public synchronized List<Counter> findByPattern(String patternString) {
        while (hasNext()) {
            Counter c = getNextCounter();
            if (c != null) {
                map.put(c.getName(), c);
            }
        }

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher("");
        List<Counter> matches = new ArrayList<>();


        for (Map.Entry<String,Counter> me: map.entrySet()) {
            String name = me.getKey();

            // apply pattern to counter name
            matcher.reset(name);

            // if the pattern matches, then add Counter to list
            if (matcher.lookingAt()) {
                matches.add(me.getValue());
            }
        }
        return matches;
    }
}
