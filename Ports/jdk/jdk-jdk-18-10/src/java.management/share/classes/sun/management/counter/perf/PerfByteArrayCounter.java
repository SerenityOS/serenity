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
import java.nio.*;

public class PerfByteArrayCounter extends AbstractCounter
       implements ByteArrayCounter {

    @SuppressWarnings("serial") // Value indirectly copied as a byte[] in writeReplace
    ByteBuffer bb;

    PerfByteArrayCounter(String name, Units u, Variability v,
                         int flags, int vectorLength,
                         ByteBuffer bb) {

        super(name, u, v, flags, vectorLength);
        this.bb = bb;
    }

    public Object getValue() {
        return byteArrayValue();
    }

    /**
     * Get a copy of the elements of the ByteArrayCounter.
     */
    public byte[] byteArrayValue() {

        bb.position(0);
        byte[] b = new byte[bb.limit()];

        // copy the bytes
        bb.get(b);

        return b;
    }

    /**
     * Get the value of an element of the ByteArrayCounter object.
     */
    public byte byteAt(int index) {
        bb.position(index);
        return bb.get();
    }

    public String toString() {
        String result = getName() + ": " + new String(byteArrayValue()) +
                        " " + getUnits();
        if (isInternal()) {
            return result + " [INTERNAL]";
        } else {
            return result;
        }
    }

    /**
     * Serialize as a snapshot object.
     */
    protected Object writeReplace() throws java.io.ObjectStreamException {
       return new ByteArrayCounterSnapshot(getName(),
                                           getUnits(),
                                           getVariability(),
                                           getFlags(),
                                           getVectorLength(),
                                           byteArrayValue());
    }

    private static final long serialVersionUID = 2545474036937279921L;
}
