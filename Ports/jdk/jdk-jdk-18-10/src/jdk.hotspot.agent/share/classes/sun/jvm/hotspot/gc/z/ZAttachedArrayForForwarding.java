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

import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.runtime.VMObjectFactory;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;

public class ZAttachedArrayForForwarding extends VMObject {
    private static CIntegerField lengthField;

    static {
        VM.registerVMInitializedObserver((o, d) -> initialize(VM.getVM().getTypeDataBase()));
    }

    static private synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("ZAttachedArrayForForwarding");

        lengthField = type.getCIntegerField("_length");
    }

    public ZAttachedArrayForForwarding(Address addr) {
        super(addr);
    }

    public long length() {
        return lengthField.getValue(addr);
    }

    // ObjectT: ZForwarding
    //  ArrayT: ZForwardingEntry
    //
    // template <typename ObjectT, typename ArrayT>
    // inline size_t ZAttachedArray<ObjectT, ArrayT>::object_size()
    private long objectSize() {
        return ZUtils.alignUp(ZForwarding.getSize(), ZForwardingEntry.getSize());
    }

    // ArrayT* operator()(const ObjectT* obj) const
    public ZForwardingEntry get(ZForwarding obj) {
        Address o = obj.getAddress().addOffsetTo(objectSize());
        return VMObjectFactory.newObject(ZForwardingEntry.class, o);
    }
}
