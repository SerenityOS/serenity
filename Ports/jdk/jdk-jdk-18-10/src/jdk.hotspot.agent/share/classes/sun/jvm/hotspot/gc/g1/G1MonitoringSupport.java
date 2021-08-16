/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.gc.g1;

import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

// Mirror class for G1MonitoringSupport.

public class G1MonitoringSupport extends VMObject {
    // size_t _eden_space_committed;
    static private CIntegerField edenSpaceCommittedField;
    // size_t _eden_space_used;
    static private CIntegerField edenSpaceUsedField;
    // size_t _survivor_space_committed;
    static private CIntegerField survivorSpaceCommittedField;
    // size_t _survivor_space_used;
    static private CIntegerField survivorSpaceUsedField;
    // size_t _old_gen_committed;
    static private CIntegerField oldGenCommittedField;
    // size_t _old_gen_used;
    static private CIntegerField oldGenUsedField;

    static {
        VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
            });
    }

    static private synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("G1MonitoringSupport");

        edenSpaceCommittedField = type.getCIntegerField("_eden_space_committed");
        edenSpaceUsedField = type.getCIntegerField("_eden_space_used");
        survivorSpaceCommittedField = type.getCIntegerField("_survivor_space_committed");
        survivorSpaceUsedField = type.getCIntegerField("_survivor_space_used");
        oldGenCommittedField = type.getCIntegerField("_old_gen_committed");
        oldGenUsedField = type.getCIntegerField("_old_gen_used");
    }

    public long edenSpaceCommitted() {
        return edenSpaceCommittedField.getValue(addr);
    }

    public long edenSpaceUsed() {
        return edenSpaceUsedField.getValue(addr);
    }

    public long edenSpaceRegionNum() {
        return edenSpaceUsed() / HeapRegion.grainBytes();
    }

    public long survivorSpaceCommitted() {
        return survivorSpaceCommittedField.getValue(addr);
    }

    public long survivorSpaceUsed() {
        return survivorSpaceUsedField.getValue(addr);
    }

    public long survivorSpaceRegionNum() {
        return survivorSpaceUsed() / HeapRegion.grainBytes();
    }

    public long oldGenCommitted() {
        return oldGenCommittedField.getValue(addr);
    }

    public long oldGenUsed() {
        return oldGenUsedField.getValue(addr);
    }

    public G1MonitoringSupport(Address addr) {
        super(addr);
    }
}
