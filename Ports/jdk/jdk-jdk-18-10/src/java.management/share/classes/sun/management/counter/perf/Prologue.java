/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

class Prologue {
    // these constants should match their #define counterparts in vmdata.hpp
    private static final byte PERFDATA_BIG_ENDIAN    = 0;
    private static final byte PERFDATA_LITTLE_ENDIAN = 1;
    private static final int  PERFDATA_MAGIC         = 0xcafec0c0;

    private class PrologueFieldOffset {
        private static final int SIZEOF_BYTE = 1;
        private static final int SIZEOF_INT  = 4;
        private static final int SIZEOF_LONG = 8;

        private static final int MAGIC_SIZE            = SIZEOF_INT;
        private static final int BYTE_ORDER_SIZE       = SIZEOF_BYTE;
        private static final int MAJOR_SIZE            = SIZEOF_BYTE;
        private static final int MINOR_SIZE            = SIZEOF_BYTE;
        private static final int ACCESSIBLE_SIZE       = SIZEOF_BYTE;
        private static final int USED_SIZE             = SIZEOF_INT;
        private static final int OVERFLOW_SIZE         = SIZEOF_INT;
        private static final int MOD_TIMESTAMP_SIZE    = SIZEOF_LONG;
        private static final int ENTRY_OFFSET_SIZE     = SIZEOF_INT;
        private static final int NUM_ENTRIES_SIZE      = SIZEOF_INT;

        // these constants must match the field offsets and sizes
        // in the PerfDataPrologue structure in perfMemory.hpp
        static final int MAGIC          = 0;
        static final int BYTE_ORDER     = MAGIC + MAGIC_SIZE;
        static final int MAJOR_VERSION  = BYTE_ORDER + BYTE_ORDER_SIZE;
        static final int MINOR_VERSION  = MAJOR_VERSION + MAJOR_SIZE;
        static final int ACCESSIBLE     = MINOR_VERSION + MINOR_SIZE;
        static final int USED           = ACCESSIBLE + ACCESSIBLE_SIZE;
        static final int OVERFLOW       = USED + USED_SIZE;
        static final int MOD_TIMESTAMP  = OVERFLOW + OVERFLOW_SIZE;
        static final int ENTRY_OFFSET   = MOD_TIMESTAMP + MOD_TIMESTAMP_SIZE;
        static final int NUM_ENTRIES    = ENTRY_OFFSET + ENTRY_OFFSET_SIZE;
        static final int PROLOGUE_2_0_SIZE = NUM_ENTRIES + NUM_ENTRIES_SIZE;
    }


    private ByteBuffer header;
    private int magic;

    Prologue(ByteBuffer b) {
        this.header = b.duplicate();

        // the magic number is always stored in big-endian format
        // save and restore the buffer's initial byte order around
        // the fetch of the data.
        header.order(ByteOrder.BIG_ENDIAN);
        header.position(PrologueFieldOffset.MAGIC);
        magic = header.getInt();

        // the magic number is always stored in big-endian format
        if (magic != PERFDATA_MAGIC) {
            throw new InstrumentationException("Bad Magic: " +
                                               Integer.toHexString(getMagic()));
        }


        // set the buffer's byte order according to the value of its
        // byte order field.
        header.order(getByteOrder());

        // Check version
        int major = getMajorVersion();
        int minor = getMinorVersion();

        if (major < 2) {
            throw new InstrumentationException("Unsupported version: " +
                                               major + "." + minor);
        }

        // Currently, only support 2.0 version.
        header.limit(PrologueFieldOffset.PROLOGUE_2_0_SIZE);
    }

    public int getMagic() {
        return magic;
    }

    public int getMajorVersion() {
        header.position(PrologueFieldOffset.MAJOR_VERSION);
        return (int)header.get();
    }

    public int getMinorVersion() {
        header.position(PrologueFieldOffset.MINOR_VERSION);
        return (int)header.get();
    }

    public ByteOrder getByteOrder() {
        header.position(PrologueFieldOffset.BYTE_ORDER);

        byte byte_order = header.get();
        if (byte_order == PERFDATA_BIG_ENDIAN) {
            return ByteOrder.BIG_ENDIAN;
        }
        else {
            return ByteOrder.LITTLE_ENDIAN;
        }
    }

    public int getEntryOffset() {
        header.position(PrologueFieldOffset.ENTRY_OFFSET);
        return header.getInt();
    }

    // The following fields are updated asynchronously
    // while they are accessed by these methods.
    public int getUsed() {
        header.position(PrologueFieldOffset.USED);
        return header.getInt();
    }

    public int getOverflow() {
        header.position(PrologueFieldOffset.OVERFLOW);
        return header.getInt();
    }

    public long getModificationTimeStamp() {
        header.position(PrologueFieldOffset.MOD_TIMESTAMP);
        return header.getLong();
    }

    public int getNumEntries() {
        header.position(PrologueFieldOffset.NUM_ENTRIES);
        return header.getInt();
    }

    public boolean isAccessible() {
        header.position(PrologueFieldOffset.ACCESSIBLE);
        byte b = header.get();
        return (b == 0 ? false : true);
    }
}
