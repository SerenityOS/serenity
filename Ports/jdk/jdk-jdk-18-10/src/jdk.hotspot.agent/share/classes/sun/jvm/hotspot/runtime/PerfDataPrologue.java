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

public class PerfDataPrologue extends VMObject {
    private static JIntField  magicField;
    private static JByteField byteOrderField;
    private static JByteField majorVersionField;
    private static JByteField minorVersionField;
    private static JByteField accessibleField;
    private static JIntField  usedField;
    private static JIntField  overflowField;
    private static JLongField modTimeStampField;
    private static JIntField  entryOffsetField;
    private static JIntField  numEntriesField;

    static {
        VM.registerVMInitializedObserver(new Observer() {
                public void update(Observable o, Object data) {
                    initialize(VM.getVM().getTypeDataBase());
                }
            });
    }

    private static synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("PerfDataPrologue");
        magicField = type.getJIntField("magic");
        byteOrderField = type.getJByteField("byte_order");
        majorVersionField = type.getJByteField("major_version");
        minorVersionField = type.getJByteField("minor_version");
        accessibleField = type.getJByteField("accessible");
        usedField = type.getJIntField("used");
        overflowField = type.getJIntField("overflow");
        modTimeStampField = type.getJLongField("mod_time_stamp");
        entryOffsetField = type.getJIntField("entry_offset");
        numEntriesField = type.getJIntField("num_entries");
    }

    public PerfDataPrologue(Address addr) {
        super(addr);
    }

    // Accessors
    public int magic() {
        return (int) magicField.getValue(addr);
    }

    public byte byteOrder() {
        return (byte) byteOrderField.getValue(addr);
    }

    public byte majorVersion() {
        return (byte) majorVersionField.getValue(addr);
    }

    public boolean accessible() {
        return ((byte) accessibleField.getValue(addr)) != (byte)0;
    }

    public int used() {
        return (int) usedField.getValue(addr);
    }

    public int overflow() {
        return (int) overflowField.getValue(addr);
    }

    public long modTimeStamp() {
        return (long) modTimeStampField.getValue(addr);
    }

    public int entryOffset() {
        return (int) entryOffsetField.getValue(addr);
    }

    public int numEntries() {
        return (int) numEntriesField.getValue(addr);
    }
}
