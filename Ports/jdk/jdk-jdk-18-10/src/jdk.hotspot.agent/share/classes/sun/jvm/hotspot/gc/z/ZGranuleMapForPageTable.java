/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.types.AddressField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

public class ZGranuleMapForPageTable  extends VMObject {
    private static AddressField mapField;

    static {
        VM.registerVMInitializedObserver((o, d) -> initialize(VM.getVM().getTypeDataBase()));
    }

    static private synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("ZGranuleMapForPageTable");

        mapField = type.getAddressField("_map");
    }

    public ZGranuleMapForPageTable(Address addr) {
        super(addr);
    }

    private Address map() {
        return mapField.getValue(addr);
    }

    public long size() {
        return ZGlobals.ZAddressOffsetMax >> ZGlobals.ZGranuleSizeShift;
    }

    private long index_for_addr(Address addr) {
        long index = ZAddress.offset(addr) >> ZGlobals.ZGranuleSizeShift;

        return index;
    }

    Address at(long index) {
        return map().getAddressAt(index * VM.getVM().getBytesPerLong());
    }

    Address get(Address addr) {
        long index = index_for_addr(addr);
        return at(index);
    }

    public class Iterator {
        private long next = 0;

        boolean hasNext() {
            return next < size();
        }

        Address next() {
            if (next >= size()) {
                throw new RuntimeException("OOIBE");
            }

            return at(next++);
        }
    }
}
