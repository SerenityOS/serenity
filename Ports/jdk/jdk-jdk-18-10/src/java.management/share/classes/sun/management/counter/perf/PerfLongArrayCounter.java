/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;

public class PerfLongArrayCounter extends AbstractCounter
       implements LongArrayCounter {


    @SuppressWarnings("serial") // Value indirectly copied as a long[] in writeReplace
    LongBuffer lb;

    PerfLongArrayCounter(String name, Units u, Variability v,
                         int flags, int vectorLength,
                         LongBuffer lb) {

        super(name, u, v, flags, vectorLength);
        this.lb = lb;
    }

    public Object getValue() {
        return longArrayValue();
    }

    /**
     * Get a copy of the elements of the LongArrayCounter.
     */
    public long[] longArrayValue() {

        lb.position(0);
        long[] l = new long[lb.limit()];

        // copy the bytes
        lb.get(l);

        return l;
    }

    /**
     * Get the value of an element of the LongArrayCounter object.
     */
    public long longAt(int index) {
        lb.position(index);
        return lb.get();
    }

    /**
     * Serialize as a snapshot object.
     */
    protected Object writeReplace() throws java.io.ObjectStreamException {
        return new LongArrayCounterSnapshot(getName(),
                                            getUnits(),
                                            getVariability(),
                                            getFlags(),
                                            getVectorLength(),
                                            longArrayValue());
    }

    private static final long serialVersionUID = -2733617913045487126L;
}
