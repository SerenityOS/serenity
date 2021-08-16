/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class PerfMemory {
    private static AddressField  startField;
    private static AddressField  endField;
    private static AddressField  topField;
    private static CIntegerField capacityField;
    private static AddressField  prologueField;
    private static CIntegerField initializedField;

    static {
        VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
            });
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("PerfMemory");
        startField = type.getAddressField("_start");
        endField = type.getAddressField("_end");
        topField = type.getAddressField("_top");
        capacityField = type.getCIntegerField("_capacity");
        prologueField = type.getAddressField("_prologue");
        initializedField = type.getCIntegerField("_initialized");
    }

    // Accessors
    public static Address start() {
        return startField.getValue();
    }

    public static Address end() {
        return endField.getValue();
    }

    public static Address top() {
        return topField.getValue();
    }

    public static long capacity() {
        return capacityField.getValue();
    }

    public static boolean initialized() {
        return (initializedField.getValue()) != 0;
    }

    public static PerfDataPrologue prologue() {
        return (PerfDataPrologue) VMObjectFactory.newObject(
                   PerfDataPrologue.class, prologueField.getValue());
    }

    public static boolean contains(Address addr) {
        return start() != null &&
            addr.minus(start()) >= 0 &&
            end().minus(addr) > 0;
    }

    // an interface supplied to iterate PerfDataEntries
    public static interface PerfDataEntryVisitor {
        // returns false to stop the iteration
        public boolean visit(PerfDataEntry pde);
    }

    public static void iterate(PerfDataEntryVisitor visitor) {
        PerfDataPrologue header = prologue();
        int off = header.entryOffset();
        int num = header.numEntries();
        Address addr = header.getAddress();

        for (int i = 0; i < num; i++) {
            PerfDataEntry pde = (PerfDataEntry) VMObjectFactory.newObject(
                               PerfDataEntry.class, addr.addOffsetTo(off));
            off += pde.entryLength();
            if (visitor.visit(pde) == false) return;
        }
    }
}
