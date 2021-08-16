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
import java.util.*;
import java.util.regex.*;
import java.nio.*;

/**
 * The concrete implementation of version 2.0 of the HotSpot PerfData
 * Instrumentation buffer. This class is responsible for parsing the
 * instrumentation memory and constructing the necessary objects to
 * represent and access the instrumentation objects contained in the
 * memory buffer.
 * <p>
 * The structure of the 2.0 entry is defined in struct PerfDataEnry
 * as decsribed in perfMemory.hpp. This structure looks like:
 * <pre>
 * typedef struct {
 *   jint entry_length;         // entry length in bytes
 *   jint name_offset;          // offset to entry name, relative to start
 *                              // of entry
 *   jint vector_length;        // length of the vector. If 0, then scalar.
 *   jbyte data_type;           // JNI field descriptor type
 *   jbyte flags;               // miscellaneous attribute flags
 *                              // 0x01 - supported
 *   jbyte data_units;          // unit of measure attribute
 *   jbyte data_variability;    // variability attribute
 *   jbyte data_offset;         // offset to data item, relative to start
 *                              // of entry.
 * } PerfDataEntry;
 * </pre>
 *
 * @author Brian Doherty
 * @since 1.5
 * @see AbstractPerfDataBuffer
 */
public class PerfDataBuffer extends PerfDataBufferImpl {

    // 8028357 removed old, inefficient debug logging

    private static final int syncWaitMs =
            Integer.getInteger("sun.jvmstat.perdata.syncWaitMs", 5000);
    private static final ArrayList<Monitor> EMPTY_LIST = new ArrayList<>(0);

    /*
     * These are primarily for documentary purposes and the match up
     * with the PerfDataEntry structure in perfMemory.hpp. They are
     * generally unused in this code, but they are kept consistent with
     * the data structure just in case some unforseen need arrises.
     */
    private static final int PERFDATA_ENTRYLENGTH_OFFSET=0;
    private static final int PERFDATA_ENTRYLENGTH_SIZE=4;   // sizeof(int)
    private static final int PERFDATA_NAMEOFFSET_OFFSET=4;
    private static final int PERFDATA_NAMEOFFSET_SIZE=4;    // sizeof(int)
    private static final int PERFDATA_VECTORLENGTH_OFFSET=8;
    private static final int PERFDATA_VECTORLENGTH_SIZE=4;  // sizeof(int)
    private static final int PERFDATA_DATATYPE_OFFSET=12;
    private static final int PERFDATA_DATATYPE_SIZE=1;      // sizeof(byte)
    private static final int PERFDATA_FLAGS_OFFSET=13;
    private static final int PERFDATA_FLAGS_SIZE=1;       // sizeof(byte)
    private static final int PERFDATA_DATAUNITS_OFFSET=14;
    private static final int PERFDATA_DATAUNITS_SIZE=1;     // sizeof(byte)
    private static final int PERFDATA_DATAVAR_OFFSET=15;
    private static final int PERFDATA_DATAVAR_SIZE=1;       // sizeof(byte)
    private static final int PERFDATA_DATAOFFSET_OFFSET=16;
    private static final int PERFDATA_DATAOFFSET_SIZE=4;    // sizeof(int)

    PerfDataBufferPrologue prologue;
    int nextEntry;
    long lastNumEntries;
    IntegerMonitor overflow;
    ArrayList<Monitor> insertedMonitors;

    /**
     * Construct a PerfDataBuffer instance.
     * <p>
     * This class is dynamically loaded by
     * {@link AbstractPerfDataBuffer#createPerfDataBuffer}, and this
     * constructor is called to instantiate the instance.
     *
     * @param buffer the buffer containing the instrumentation data
     * @param lvmid the Local Java Virtual Machine Identifier for this
     *              instrumentation buffer.
     */
    public PerfDataBuffer(ByteBuffer buffer, int lvmid)
           throws MonitorException {
        super(buffer, lvmid);
        prologue = new PerfDataBufferPrologue(buffer);
        this.buffer.order(prologue.getByteOrder());
    }

    /**
     * {@inheritDoc}
     */
    protected void buildMonitorMap(Map<String, Monitor>  map) throws MonitorException {
        assert Thread.holdsLock(this);

        // start at the beginning of the buffer
        buffer.rewind();

        // create pseudo monitors
        buildPseudoMonitors(map);

        // wait for the target JVM to indicate that it's intrumentation
        // buffer is safely accessible
        synchWithTarget();

        // parse the currently defined entries starting at the first entry.
        nextEntry = prologue.getEntryOffset();

        // record the number of entries before parsing the structure
        int numEntries = prologue.getNumEntries();

        // start parsing
        Monitor monitor = getNextMonitorEntry();
        while (monitor != null) {
            map.put(monitor.getName(), monitor);
            monitor = getNextMonitorEntry();
        }

        /*
         * keep track of the current number of entries in the shared
         * memory for new entry detection purposes. It's possible for
         * the data structure to be modified while the Map is being
         * built and the entry count in the header might change while
         * we are parsing it. The map will contain all the counters
         * found, but the number recorded in numEntries might be small
         * than what than the number we actually parsed (due to asynchronous
         * updates). This discrepency is handled by ignoring any re-parsed
         * entries when updating the Map in getNewMonitors().
         */
        lastNumEntries = numEntries;

        // keep track of the monitors just added.
        insertedMonitors = new ArrayList<Monitor>(map.values());
    }

    /**
     * {@inheritDoc}
     */
    protected void getNewMonitors(Map<String, Monitor> map) throws MonitorException {
        assert Thread.holdsLock(this);

        int numEntries = prologue.getNumEntries();

        if (numEntries > lastNumEntries) {
            lastNumEntries = numEntries;
            Monitor monitor = getNextMonitorEntry();

            while (monitor != null) {
                String name = monitor.getName();

                // guard against re-parsed entries
                if (!map.containsKey(name)) {
                    map.put(name, monitor);
                    if (insertedMonitors != null) {
                        insertedMonitors.add(monitor);
                    }
                }
                monitor = getNextMonitorEntry();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected MonitorStatus getMonitorStatus(Map<String, Monitor> map) throws MonitorException {
        assert Thread.holdsLock(this);
        assert insertedMonitors != null;

        // load any new monitors
        getNewMonitors(map);

        // current implementation doesn't support deletion of reuse of entries
        ArrayList<Monitor> removed = EMPTY_LIST;
        ArrayList<Monitor> inserted = insertedMonitors;

        insertedMonitors = new ArrayList<>();
        return new MonitorStatus(inserted, removed);
    }

    /**
     * Build the pseudo monitors used to map the prolog data into counters.
     */
    protected void buildPseudoMonitors(Map<String, Monitor> map) {
        Monitor monitor = null;
        String name = null;
        IntBuffer ib = null;

        name = PerfDataBufferPrologue.PERFDATA_MAJOR_NAME;
        ib = prologue.majorVersionBuffer();
        monitor = new PerfIntegerMonitor(name, Units.NONE,
                                         Variability.CONSTANT, false, ib);
        map.put(name, monitor);

        name = PerfDataBufferPrologue.PERFDATA_MINOR_NAME;
        ib = prologue.minorVersionBuffer();
        monitor = new PerfIntegerMonitor(name, Units.NONE,
                                         Variability.CONSTANT, false, ib);
        map.put(name, monitor);

        name = PerfDataBufferPrologue.PERFDATA_BUFFER_SIZE_NAME;
        ib = prologue.sizeBuffer();
        monitor = new PerfIntegerMonitor(name, Units.BYTES,
                                         Variability.MONOTONIC, false, ib);
        map.put(name, monitor);

        name = PerfDataBufferPrologue.PERFDATA_BUFFER_USED_NAME;
        ib = prologue.usedBuffer();
        monitor = new PerfIntegerMonitor(name, Units.BYTES,
                                         Variability.MONOTONIC, false, ib);
        map.put(name, monitor);

        name = PerfDataBufferPrologue.PERFDATA_OVERFLOW_NAME;
        ib = prologue.overflowBuffer();
        monitor = new PerfIntegerMonitor(name, Units.BYTES,
                                         Variability.MONOTONIC, false, ib);
        map.put(name, monitor);
        this.overflow = (IntegerMonitor)monitor;

        name = PerfDataBufferPrologue.PERFDATA_MODTIMESTAMP_NAME;
        LongBuffer lb = prologue.modificationTimeStampBuffer();
        monitor = new PerfLongMonitor(name, Units.TICKS,
                                      Variability.MONOTONIC, false, lb);
        map.put(name, monitor);
    }

    /**
     * Method that waits until the target jvm indicates that
     * its shared memory is safe to access.
     */
    protected void synchWithTarget() throws MonitorException {
        /*
         * synch must happen with syncWaitMs from now. Default is 5 seconds,
         * which is reasonabally generous and should provide for extreme
         * situations like startup delays due to allocation of large ISM heaps.
         */
        long timeLimit = System.currentTimeMillis() + syncWaitMs;

        // loop waiting for the accessible indicater to be non-zero
        while (!prologue.isAccessible()) {

            // give the target jvm a chance to complete initializatoin
            try { Thread.sleep(20); } catch (InterruptedException e) { }

            if (System.currentTimeMillis() > timeLimit) {
                throw new MonitorException("Could not synchronize with target");
            }
        }
    }

    /**
     * method to extract the next monitor entry from the instrumentation memory.
     * assumes that nextEntry is the offset into the byte array
     * at which to start the search for the next entry. method leaves
     * next entry pointing to the next entry or to the end of data.
     */
    protected Monitor getNextMonitorEntry() throws MonitorException {
        Monitor monitor = null;

        // entries are always 4 byte aligned.
        if ((nextEntry % 4) != 0) {
            throw new MonitorStructureException(
                    "Misaligned entry index: "
                    + Integer.toHexString(nextEntry));
        }

        // protect againt a corrupted shard memory region.
        if ((nextEntry < 0)  || (nextEntry > buffer.limit())) {
            throw new MonitorStructureException(
                    "Entry index out of bounds: "
                    + Integer.toHexString(nextEntry)
                    + ", limit = " + Integer.toHexString(buffer.limit()));
        }

        // check for end of the buffer
        if (nextEntry == buffer.limit()) {
            return null;
        }

        buffer.position(nextEntry);

        int entryStart = buffer.position();
        int entryLength = buffer.getInt();

        // check for valid entry length
        if ((entryLength < 0) || (entryLength > buffer.limit())) {
            throw new MonitorStructureException(
                    "Invalid entry length: entryLength = " + entryLength
                    + " (0x" + Integer.toHexString(entryLength) + ")");
        }

        // check if last entry occurs before the eof.
        if ((entryStart + entryLength) > buffer.limit()) {
            throw new MonitorStructureException(
                    "Entry extends beyond end of buffer: "
                    + " entryStart = 0x" + Integer.toHexString(entryStart)
                    + " entryLength = 0x" + Integer.toHexString(entryLength)
                    + " buffer limit = 0x" + Integer.toHexString(buffer.limit()));
        }

        if (entryLength == 0) {
            // end of data
            return null;
        }

        // we can safely read this entry
        int nameOffset = buffer.getInt();
        int vectorLength = buffer.getInt();
        byte typeCodeByte = buffer.get();
        byte flags = buffer.get();
        byte unitsByte = buffer.get();
        byte varByte = buffer.get();
        int dataOffset = buffer.getInt();

        // convert common attributes to their object types
        Units units = Units.toUnits(unitsByte);
        Variability variability = Variability.toVariability(varByte);
        TypeCode typeCode = null;
        boolean supported = (flags & 0x01) != 0;

        try {
            typeCode = TypeCode.toTypeCode(typeCodeByte);

        } catch (IllegalArgumentException e) {
            throw new MonitorStructureException(
                    "Illegal type code encountered:"
                    + " entry_offset = 0x" + Integer.toHexString(nextEntry)
                    + ", type_code = " + Integer.toHexString(typeCodeByte));
        }

        // verify that the name_offset is contained within the entry bounds
        if (nameOffset > entryLength) {
            throw new MonitorStructureException(
                    "Field extends beyond entry bounds"
                    + " entry_offset = 0x" + Integer.toHexString(nextEntry)
                    + ", name_offset = 0x" + Integer.toHexString(nameOffset));
        }

        // verify that the data_offset is contained within the entry bounds
        if (dataOffset > entryLength) {
            throw new MonitorStructureException(
                    "Field extends beyond entry bounds:"
                    + " entry_offset = 0x" + Integer.toHexString(nextEntry)
                    + ", data_offset = 0x" + Integer.toHexString(dataOffset));
        }

        // validate the variability and units fields
        if (variability == Variability.INVALID) {
            throw new MonitorDataException(
                    "Invalid variability attribute:"
                    + " entry_offset = 0x" + Integer.toHexString(nextEntry)
                    + ", variability = 0x" + Integer.toHexString(varByte));
        }

        if (units == Units.INVALID) {
            throw new MonitorDataException(
                    "Invalid units attribute: entry_offset = 0x"
                    + Integer.toHexString(nextEntry)
                    + ", units = 0x" + Integer.toHexString(unitsByte));
        }

        // the entry looks good - parse the variable length components

        /*
         * The name starts at nameOffset and continues up to the first null
         * byte. however, we don't know the length, but we can approximate it
         * without searching for the null by using the offset for the data
         * field, which follows the name field.
         */
        assert (buffer.position() == (entryStart + nameOffset));
        assert (dataOffset > nameOffset);

        // include possible pad space
        int maxNameLength = dataOffset-nameOffset;

        // maxNameLength better be less than the total entry length
        assert (maxNameLength < entryLength);

        // collect the characters, but do not collect the null byte,
        // as the String(byte[]) constructor does not ignore it!
        byte[] nameBytes = new byte[maxNameLength];
        int nameLength = 0;
        byte b;
        while (((b = buffer.get()) != 0) && (nameLength < maxNameLength)) {
             nameBytes[nameLength++] = b;
        }

        assert (nameLength < maxNameLength);

        // we should before or at the start of the data field
        assert (buffer.position() <= (entryStart + dataOffset));

        // convert the name bytes into a String
        String name = new String(nameBytes, 0, nameLength);

        /*
         * compute the size of the data item - this includes pad
         * characters used to align the next entry.
         */
        int dataSize = entryLength - dataOffset;

        // set the position to the start of the data item
        buffer.position(entryStart + dataOffset);

        if (vectorLength == 0) {
            // create a scalar Monitor object
            if (typeCode == TypeCode.LONG) {
                LongBuffer lb = buffer.asLongBuffer();
                lb.limit(1);  // limit buffer size to one long value.
                monitor = new PerfLongMonitor(name, units, variability,
                                              supported, lb);
            } else {
                /*
                 * unexpected type code - coding error or uncoordinated
                 * JVM change
                 */
                throw new MonitorTypeException(
                        "Unexpected type code encountered:"
                        + " entry_offset = 0x" + Integer.toHexString(nextEntry)
                        + ", name = " + name
                        + ", type_code = " + typeCode
                        + " (0x" + Integer.toHexString(typeCodeByte) + ")");
            }
        } else {
            // create a vector Monitor object
            if (typeCode == TypeCode.BYTE) {
                if (units != Units.STRING) {
                    // only byte arrays of type STRING are currently supported
                    throw new MonitorTypeException(
                            "Unexpected vector type encounterd:"
                            + " entry_offset = "
                            + Integer.toHexString(nextEntry)
                            + ", name = " + name
                            + ", type_code = " + typeCode + " (0x"
                            + Integer.toHexString(typeCodeByte) + ")"
                            + ", units = " + units + " (0x"
                            + Integer.toHexString(unitsByte) + ")");
                }

                ByteBuffer bb = buffer.slice();
                bb.limit(vectorLength); // limit buffer length to # of chars

                if (variability == Variability.CONSTANT) {
                    monitor = new PerfStringConstantMonitor(name, supported,
                                                            bb);
                } else if (variability == Variability.VARIABLE) {
                    monitor = new PerfStringVariableMonitor(name, supported,
                                                            bb, vectorLength-1);
                } else if (variability == Variability.MONOTONIC) {
                    // Monotonically increasing byte arrays are not supported
                    throw new MonitorDataException(
                            "Unexpected variability attribute:"
                            + " entry_offset = 0x"
                            + Integer.toHexString(nextEntry)
                            + " name = " + name
                            + ", variability = " + variability + " (0x"
                            + Integer.toHexString(varByte) + ")");
                } else {
                    // variability was validated above, so this unexpected
                    assert false;
                }
            } else {
                // coding error or uncoordinated JVM change
                throw new MonitorTypeException(
                        "Unexpected type code encountered:"
                        + " entry_offset = 0x"
                        + Integer.toHexString(nextEntry)
                        + ", name = " + name
                        + ", type_code = " + typeCode + " (0x"
                        + Integer.toHexString(typeCodeByte) + ")");
            }
        }

        // setup index to next entry for next iteration of the loop.
        nextEntry = entryStart + entryLength;
        return monitor;
    }
}
