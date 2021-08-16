/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
package sun.jvm.hotspot.debugger.posix;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.utilities.memo.*;

/** Provides a simple wrapper around the ELF library which handles
    relocation. */
public abstract class DSO implements LoadObject {

    private String         filename;
    private Address        addr;
    private long           size;

    public DSO(String filename, long size, Address relocation) {
        this.filename = filename;
        this.size = size;
        this.addr = relocation;
    }

    public String getName() {
        return filename;
    }

    public Address getBase() {
        return addr;
    }

    public long getSize() {
        return size;
    }

    public CDebugInfoDataBase getDebugInfoDataBase() throws DebuggerException {
        // FIXME: after stabs parser
        return null;
    }

    public BlockSym debugInfoForPC(Address pc) throws DebuggerException  {
        // FIXME: after stabs parser
        return null;
    }

    public LineNumberInfo lineNumberForPC(Address pc) throws DebuggerException {
        // FIXME: after stabs parser
        return null;
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof DSO)) {
           return false;
        }
        DSO other = (DSO)o;
        return getBase().equals(other.getBase());
    }

    public int hashCode() {
        return getBase().hashCode();
    }

    protected abstract Address newAddress(long addr);
    protected abstract long getAddressValue(Address addr);
}
