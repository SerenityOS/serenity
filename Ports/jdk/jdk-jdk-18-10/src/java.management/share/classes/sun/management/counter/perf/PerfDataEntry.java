/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UnsupportedEncodingException;

class PerfDataEntry {
    private class EntryFieldOffset {
        private static final int SIZEOF_BYTE = 1;
        private static final int SIZEOF_INT  = 4;
        private static final int SIZEOF_LONG = 8;

        private static final int ENTRY_LENGTH_SIZE    = SIZEOF_INT;
        private static final int NAME_OFFSET_SIZE     = SIZEOF_INT;
        private static final int VECTOR_LENGTH_SIZE   = SIZEOF_INT;
        private static final int DATA_TYPE_SIZE       = SIZEOF_BYTE;
        private static final int FLAGS_SIZE           = SIZEOF_BYTE;
        private static final int DATA_UNIT_SIZE       = SIZEOF_BYTE;
        private static final int DATA_VAR_SIZE        = SIZEOF_BYTE;
        private static final int DATA_OFFSET_SIZE     = SIZEOF_INT;

        static final int ENTRY_LENGTH  = 0;
        static final int NAME_OFFSET   = ENTRY_LENGTH + ENTRY_LENGTH_SIZE;
        static final int VECTOR_LENGTH = NAME_OFFSET + NAME_OFFSET_SIZE;;
        static final int DATA_TYPE     = VECTOR_LENGTH + VECTOR_LENGTH_SIZE;
        static final int FLAGS         = DATA_TYPE + DATA_TYPE_SIZE;
        static final int DATA_UNIT     = FLAGS + FLAGS_SIZE;
        static final int DATA_VAR      = DATA_UNIT + DATA_UNIT_SIZE;
        static final int DATA_OFFSET   = DATA_VAR + DATA_VAR_SIZE;
    }

    private String       name;
    private int          entryStart;
    private int          entryLength;
    private int          vectorLength;
    private PerfDataType dataType;
    private int          flags;
    private Units        unit;
    private Variability  variability;
    private int          dataOffset;
    private int          dataSize;
    private ByteBuffer   data;

    PerfDataEntry(ByteBuffer b) {
        entryStart = b.position();
        entryLength = b.getInt();

        // check for valid entry length
        if (entryLength <= 0 || entryLength > b.limit()) {
            throw new InstrumentationException("Invalid entry length: " +
                                               " entryLength = " + entryLength);
        }
        // check if last entry occurs before the eof.
        if ((entryStart + entryLength) > b.limit()) {
            throw new InstrumentationException("Entry extends beyond end of buffer: " +
                                               " entryStart = " + entryStart +
                                               " entryLength = " + entryLength +
                                               " buffer limit = " + b.limit());
        }

        b.position(entryStart + EntryFieldOffset.NAME_OFFSET);
        int nameOffset = b.getInt();

        if ((entryStart + nameOffset) > b.limit()) {
            throw new InstrumentationException("Invalid name offset: " +
                                               " entryStart = " + entryStart +
                                               " nameOffset = " + nameOffset +
                                               " buffer limit = " + b.limit());
        }


        b.position(entryStart + EntryFieldOffset.VECTOR_LENGTH);
        vectorLength = b.getInt();

        b.position(entryStart + EntryFieldOffset.DATA_TYPE);
        dataType = PerfDataType.toPerfDataType(b.get());

        b.position(entryStart + EntryFieldOffset.FLAGS);
        flags = b.get();

        b.position(entryStart + EntryFieldOffset.DATA_UNIT);
        unit = Units.toUnits(b.get());

        b.position(entryStart + EntryFieldOffset.DATA_VAR);
        variability = Variability.toVariability(b.get());

        b.position(entryStart + EntryFieldOffset.DATA_OFFSET);
        dataOffset = b.getInt();

        // read in the perfData item name, casting bytes to chars. skip the
        // null terminator
        b.position(entryStart + nameOffset);
        // calculate the length of the name
        int nameLength = 0;
        byte c;
        for (; (c = b.get()) != (byte)0; nameLength++);

        byte[] symbolBytes = new byte[nameLength];
        b.position(entryStart + nameOffset);
        for (int i = 0; i < nameLength; i++) {
            symbolBytes[i] = b.get();
        }

        // convert name into a String
        try {
            name = new String(symbolBytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            // should not reach here
            // "UTF-8" is always a known encoding
            throw new InternalError(e.getMessage(), e);
        }

        if (variability == Variability.INVALID) {
            throw new InstrumentationException("Invalid variability attribute:" +
                                               " name = " + name);
        }
        if (unit == Units.INVALID) {
            throw new InstrumentationException("Invalid units attribute: " +
                                               " name = " + name);
        }

        if (vectorLength > 0) {
            dataSize = vectorLength * dataType.size();
        } else {
            dataSize = dataType.size();
        }

        // check if data beyond the eof.
        if ((entryStart + dataOffset + dataSize) > b.limit()) {
            throw new InstrumentationException("Data extends beyond end of buffer: " +
                                               " entryStart = " + entryStart +
                                               " dataOffset = " + dataOffset+
                                               " dataSize = " + dataSize +
                                               " buffer limit = " + b.limit());
        }
        // Construct a ByteBuffer for the data
        b.position(entryStart + dataOffset);
        data = b.slice();
        data.order(b.order());
        data.limit(dataSize);
    }


    public int size() {
        return entryLength;
    }

    public String name() {
        return name;
    }

    public PerfDataType type() {
        return dataType;
    }

    public Units units() {
        return unit;
    }

    public int flags() {
        return flags;
    }

    /**
     * Returns the number of elements in the data.
     */
    public int vectorLength() {
        return vectorLength;
    }

    public Variability variability() {
        return variability;
    }

    public ByteBuffer byteData() {
        data.position(0);
        assert data.remaining() == vectorLength();
        return data.duplicate();
    }

    public LongBuffer longData() {
        LongBuffer lb = data.asLongBuffer();
        return lb;
    }
}
