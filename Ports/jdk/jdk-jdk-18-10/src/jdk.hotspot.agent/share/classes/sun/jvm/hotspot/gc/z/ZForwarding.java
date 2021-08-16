/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, NTT DATA.
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

package sun.jvm.hotspot.gc.z;

import java.util.Iterator;

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.runtime.VMObjectFactory;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

public class ZForwarding extends VMObject {
    private static Type type;
    private static long virtualFieldOffset;
    private static long entriesFieldOffset;
    private static CIntegerField objectAlignmentShiftField;
    private static CIntegerField refCountField;

    static {
        VM.registerVMInitializedObserver((o, d) -> initialize(VM.getVM().getTypeDataBase()));
    }

    static private synchronized void initialize(TypeDataBase db) {
        type = db.lookupType("ZForwarding");

        virtualFieldOffset = type.getField("_virtual").getOffset();
        entriesFieldOffset = type.getField("_entries").getOffset();
        objectAlignmentShiftField = type.getCIntegerField("_object_alignment_shift");
        refCountField = type.getCIntegerField("_ref_count");
    }

    public ZForwarding(Address addr) {
        super(addr);
    }

    public static long getSize() {
        return type.getSize();
    }

    private ZVirtualMemory virtual() {
        return VMObjectFactory.newObject(ZVirtualMemory.class, addr.addOffsetTo(virtualFieldOffset));
    }

    private ZAttachedArrayForForwarding entries() {
        return VMObjectFactory.newObject(ZAttachedArrayForForwarding.class, addr.addOffsetTo(entriesFieldOffset));
    }

    public long start() {
        return virtual().start();
    }

    public int objectAlignmentShift() {
        return (int)objectAlignmentShiftField.getValue(addr);
    }

    public boolean retainPage() {
        return refCountField.getValue(addr) > 0;
    }

    private ZForwardingEntry at(long cursor) {
        long offset = ZForwardingEntry.getSize() * cursor;
        Address entryAddress = entries().get(this).getAddress().addOffsetTo(offset);
        return VMObjectFactory.newObject(ZForwardingEntry.class, entryAddress);
    }

    private class ZForwardEntryIterator implements Iterator<ZForwardingEntry> {

        private long cursor;

        private ZForwardingEntry nextEntry;

        public ZForwardEntryIterator(long fromIndex) {
            long mask = entries().length() - 1;
            long hash = ZHash.uint32_to_uint32(fromIndex);
            cursor = hash & mask;
            nextEntry = at(cursor);
        }

        @Override
        public boolean hasNext() {
            return nextEntry.populated();
        }

        @Override
        public ZForwardingEntry next() {
            ZForwardingEntry entry = nextEntry;

            long mask = entries().length() - 1;
            cursor = (cursor + 1) & mask;
            nextEntry = at(cursor);

            return entry;
        }

        public ZForwardingEntry peak() {
            return nextEntry;
        }
    }

    public ZForwardingEntry find(long fromIndex) {
        ZForwardEntryIterator itr = new ZForwardEntryIterator(fromIndex);
        while (itr.hasNext()) {
            ZForwardingEntry entry = itr.next();
            if (entry.fromIndex() == fromIndex) {
                return entry;
            }
        }
        return itr.peak();
    }
}
