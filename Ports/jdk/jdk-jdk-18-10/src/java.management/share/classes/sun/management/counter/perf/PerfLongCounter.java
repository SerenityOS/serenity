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

public class PerfLongCounter extends AbstractCounter
       implements LongCounter {

    @SuppressWarnings("serial") // Value indirectly copied as a long[] in writeReplace
    LongBuffer lb;

    // package private
    PerfLongCounter(String name, Units u, Variability v, int flags,
                    LongBuffer lb) {
        super(name, u, v, flags);
        this.lb = lb;
    }

    public Object getValue() {
        return Long.valueOf(lb.get(0));
    }

    /**
     * Get the value of this Long performance counter
     */
    public long longValue() {
        return lb.get(0);
    }

    /**
     * Serialize as a snapshot object.
     */
    protected Object writeReplace() throws java.io.ObjectStreamException {
        return new LongCounterSnapshot(getName(),
                                       getUnits(),
                                       getVariability(),
                                       getFlags(),
                                       longValue());
    }

    private static final long serialVersionUID = 857711729279242948L;
}
