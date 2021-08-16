/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.PrintStream;

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.runtime.VMObjectFactory;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

// Mirror class for ZHeap

public class ZHeap extends VMObject {

    private static long pageAllocatorFieldOffset;
    private static long pageTableFieldOffset;
    private static long forwardingTableFieldOffset;
    private static long relocateFieldOffset;

    static {
        VM.registerVMInitializedObserver((o, d) -> initialize(VM.getVM().getTypeDataBase()));
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("ZHeap");

        pageAllocatorFieldOffset = type.getAddressField("_page_allocator").getOffset();
        pageTableFieldOffset = type.getAddressField("_page_table").getOffset();
        forwardingTableFieldOffset = type.getAddressField("_forwarding_table").getOffset();
        relocateFieldOffset = type.getAddressField("_relocate").getOffset();
    }

    public ZHeap(Address addr) {
        super(addr);
    }

    private ZPageAllocator pageAllocator() {
        Address pageAllocatorAddr = addr.addOffsetTo(pageAllocatorFieldOffset);
        return (ZPageAllocator)VMObjectFactory.newObject(ZPageAllocator.class, pageAllocatorAddr);
    }

    ZPageTable pageTable() {
        return (ZPageTable)VMObjectFactory.newObject(ZPageTable.class, addr.addOffsetTo(pageTableFieldOffset));
    }

    ZForwardingTable forwardingTable() {
        return VMObjectFactory.newObject(ZForwardingTable.class, addr.addOffsetTo(forwardingTableFieldOffset));
    }

    ZRelocate relocate() {
        return VMObjectFactory.newObject(ZRelocate.class, addr.addOffsetTo(relocateFieldOffset));
    }

    public long maxCapacity() {
        return pageAllocator().maxCapacity();
    }

    public long capacity() {
        return pageAllocator().capacity();
    }

    public long used() {
        return pageAllocator().used();
    }

    boolean is_relocating(Address o) {
        return pageTable().is_relocating(o);
    }

    Address relocate_object(Address addr) {
        ZForwarding forwarding = forwardingTable().get(addr);
        if (forwarding == null) {
            return ZAddress.good(addr);
        }
        return relocate().relocateObject(forwarding, ZAddress.good(addr));
    }

    public boolean isIn(Address addr) {
        if (ZAddress.isIn(addr)) {
            ZPage page = pageTable().get(addr);
            if (page != null) {
                return page.isIn(addr);
            }
        }
        return false;
    }

    public Address remapObject(Address o) {
        ZForwarding forwarding = forwardingTable().get(addr);
        if (forwarding == null) {
            return ZAddress.good(o);
        }
        return relocate().forwardObject(forwarding, ZAddress.good(o));
    }

    public void printOn(PrintStream tty) {
        tty.print(" ZHeap          ");
        tty.print("used " + (used() / 1024 / 1024) + "M, ");
        tty.print("capacity " + (capacity() / 1024 / 1024) + "M, ");
        tty.println("max capacity " + (maxCapacity() / 1024 / 1024) + "M");
    }
}
