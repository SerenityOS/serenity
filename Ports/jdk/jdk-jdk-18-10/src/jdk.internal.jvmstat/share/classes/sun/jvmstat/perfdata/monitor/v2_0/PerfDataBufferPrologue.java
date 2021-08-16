/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvmstat.perfdata.monitor.v2_0;

import sun.jvmstat.monitor.*;
import sun.jvmstat.perfdata.monitor.*;
import java.nio.*;

/**
 * Class representing the 2.0 version of the HotSpot PerfData instrumentation
 * buffer header.
 * <p>
 * The PerfDataBufferPrologue class supports parsing of the version
 * specific portions of the PerfDataPrologue C structure:
 * <pre>
 * typedef struct {
 *   ...                      // handled by superclass
 *   jint used;               // number of PerfData memory bytes used
 *   jint overflow;           // number of bytes of overflow
 *   jlong mod_time_stamp;    // time stamp of the last structural modification
 *   jint entry_offset;       // offset of the first PerfDataEntry
 *   jint num_entries;        // number of allocated PerfData entries
 * } PerfDataPrologue
 * </pre>
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class PerfDataBufferPrologue extends AbstractPerfDataBufferPrologue {

    private static final int SUPPORTED_MAJOR_VERSION = 2;
    private static final int SUPPORTED_MINOR_VERSION = 0;

    /*
     * the following constants must match the field offsets and sizes
     * in the PerfDataPrologue structure in perfMemory.hpp. offsets are
     * relative to the start of the PerfDataPrologue structure.
     *
     * note that PERFDATA_PROLOG_ACCESSIBLE_OFFSET redefines
     * PERFDATA_PROLOG_RESERVEDB1_OFFSET from AbstractPerfDataBufferPrologue.
     */
    static final int PERFDATA_PROLOG_ACCESSIBLE_OFFSET=7;
    static final int PERFDATA_PROLOG_ACCESSIBLE_SIZE=1;        // sizeof(byte)
    static final int PERFDATA_PROLOG_USED_OFFSET=8;
    static final int PERFDATA_PROLOG_USED_SIZE=4;              // sizeof(int)
    static final int PERFDATA_PROLOG_OVERFLOW_OFFSET=12;
    static final int PERFDATA_PROLOG_OVERFLOW_SIZE=4;          // sizeof(int)
    static final int PERFDATA_PROLOG_MODTIMESTAMP_OFFSET=16;
    static final int PERFDATA_PROLOG_MODTIMESTAMP_SIZE=8;      // sizeof(long)
    static final int PERFDATA_PROLOG_ENTRYOFFSET_OFFSET=24;
    static final int PERFDATA_PROLOG_ENTRYOFFSET_SIZE=4;       // sizeof(int)
    static final int PERFDATA_PROLOG_NUMENTRIES_OFFSET=28;
    static final int PERFDATA_PROLOG_NUMENTRIES_SIZE=4;        // sizeof(int)

    static final int PERFDATA_PROLOG_SIZE=32;  // sizeof(struct PerfDataProlog)

    // names for counters that expose prologue fields
    static final String PERFDATA_BUFFER_SIZE_NAME  = "sun.perfdata.size";
    static final String PERFDATA_BUFFER_USED_NAME  = "sun.perfdata.used";
    static final String PERFDATA_OVERFLOW_NAME     = "sun.perfdata.overflow";
    static final String PERFDATA_MODTIMESTAMP_NAME = "sun.perfdata.timestamp";
    static final String PERFDATA_NUMENTRIES_NAME   = "sun.perfdata.entries";

    /**
     * Create an instance of PerfDataBufferPrologue from the given
     * ByteBuffer object.
     *
     * @param byteBuffer the buffer containing the binary header data
     */
    public PerfDataBufferPrologue(ByteBuffer byteBuffer)
           throws MonitorException  {
        super(byteBuffer);
        assert ((getMajorVersion() == 2) && (getMinorVersion() == 0));
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsAccessible() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAccessible() {
        assert supportsAccessible();
        byteBuffer.position(PERFDATA_PROLOG_ACCESSIBLE_OFFSET);
        byte value = byteBuffer.get();
        return value != 0;
    }

    /**
     * Get the utilization of the instrumentation memory buffer.
     *
     * @return int - the utilization of the buffer
     */
    public int getUsed() {
        byteBuffer.position(PERFDATA_PROLOG_USED_OFFSET);
        return byteBuffer.getInt();
    }

    /**
     * Get the size of the instrumentation memory buffer.
     *
     * @return int - the size of the buffer
     */
    public int getBufferSize() {
        return byteBuffer.capacity();
    }

    /**
     * Get the buffer overflow amount. This value is non-zero if the
     * HotSpot JVM has overflowed the instrumentation memory buffer.
     * The target JVM can be restarted with -XX:PerfDataMemSize=X to
     * create a larger memory buffer.
     *
     * @return int - the size of the buffer
     */
    public int getOverflow() {
        byteBuffer.position(PERFDATA_PROLOG_OVERFLOW_OFFSET);
        return byteBuffer.getInt();
    }

    /**
     * Get the time of last modification for the instrumentation
     * memory buffer. This method returns the time, as ticks since the
     * start of the target JVM, of the last structural modification to
     * the instrumentation buffer. Structural modifications correspond to
     * the addition or deletion of instrumentation objects. Updates to
     * counter values are not structural modifications.
     */
    public long getModificationTimeStamp() {
        byteBuffer.position(PERFDATA_PROLOG_MODTIMESTAMP_OFFSET);
        return byteBuffer.getLong();
    }

    /**
     * Get the offset of the first PerfDataEntry.
     */
    public int getEntryOffset() {
        byteBuffer.position(PERFDATA_PROLOG_ENTRYOFFSET_OFFSET);
        return byteBuffer.getInt();
    }

    /**
     * Get the offset of the first PerfDataEntry.
     */
    public int getNumEntries() {
        byteBuffer.position(PERFDATA_PROLOG_NUMENTRIES_OFFSET);
        return byteBuffer.getInt();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return PERFDATA_PROLOG_SIZE;  // sizeof(struct PerfDataProlog)
    }

    /**
     * Return an IntBuffer that accesses the used value. This is used
     * to create a Monitor object for this value.
     *
     * @return IntBuffer - a ByteBuffer that accesses the used value
     *                     in the instrumentation buffer header.
     * @see #getUsed()
     */
    IntBuffer usedBuffer() {
        byteBuffer.position(PERFDATA_PROLOG_USED_OFFSET);
        IntBuffer ib = byteBuffer.asIntBuffer();
        ib.limit(1);
        return ib;
    }

    /**
     * Return an IntBuffer that accesses the size value. This is used
     * to create a Monitor object for this value.
     *
     * @return IntBuffer - a ByteBuffer that accesses the size value
     *                     in the instrumentation buffer header.
     * @see #getBufferSize()
     */
    IntBuffer sizeBuffer() {
        IntBuffer ib = IntBuffer.allocate(1);
        ib.put(byteBuffer.capacity());
        return ib;
    }

    /**
     * Return an IntBuffer that accesses the overflow value. This is used
     * to create a Monitor object for this value.
     *
     * @return IntBuffer - a ByteBuffer that accesses the overflow value
     *                     in the instrumentation buffer header.
     * @see #getOverflow()
     */
    IntBuffer overflowBuffer() {
        byteBuffer.position(PERFDATA_PROLOG_OVERFLOW_OFFSET);
        IntBuffer ib = byteBuffer.asIntBuffer();
        ib.limit(1);
        return ib;
    }

    /**
     * Return a LongBuffer that accesses the modification timestamp value.
     * This is used to create a Monitor object for this value.
     *
     * @return LongBuffer - a ByteBuffer that accesses the modification time
     *                      stamp value in the instrumentation buffer header.
     * @see #getModificationTimeStamp()
     */
    LongBuffer modificationTimeStampBuffer() {
        byteBuffer.position(PERFDATA_PROLOG_MODTIMESTAMP_OFFSET);
        LongBuffer lb = byteBuffer.asLongBuffer();
        lb.limit(1);
        return lb;
    }

    /**
     * Return an IntBuffer that accesses the number of entries value.
     * This is used to create a Monitor object for this value.
     *
     * @return LongBuffer - a ByteBuffer that accesses the num_entries
     *                      value in the instrumentation buffer header.
     * @see #getNumEntries()
     */
    IntBuffer numEntriesBuffer() {
        byteBuffer.position(PERFDATA_PROLOG_NUMENTRIES_OFFSET);
        IntBuffer ib = byteBuffer.asIntBuffer();
        ib.limit(1);
        return ib;
    }
}
