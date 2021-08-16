/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.proc;

import java.util.List;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

/** An extension of the JVMDebugger interface with a few additions to
    support 32-bit vs. 64-bit debugging as well as features required
    by the architecture-specific subpackages. */

public interface ProcDebugger extends JVMDebugger {
  public MachineDescription getMachineDescription() throws DebuggerException;
  public String       addressValueToString(long address) throws DebuggerException;
  public boolean      readJBoolean(long address) throws DebuggerException;
  public byte         readJByte(long address) throws DebuggerException;
  public char         readJChar(long address) throws DebuggerException;
  public double       readJDouble(long address) throws DebuggerException;
  public float        readJFloat(long address) throws DebuggerException;
  public int          readJInt(long address) throws DebuggerException;
  public long         readJLong(long address) throws DebuggerException;
  public short        readJShort(long address) throws DebuggerException;
  public long         readCInteger(long address, long numBytes, boolean isUnsigned)
    throws DebuggerException;
  public ProcAddress   readAddress(long address) throws DebuggerException;
  public ProcAddress   readCompOopAddress(long address) throws DebuggerException;
  public ProcAddress   readCompKlassAddress(long address) throws DebuggerException;
  public ProcOopHandle readOopHandle(long address) throws DebuggerException;
  public ProcOopHandle readCompOopHandle(long address) throws DebuggerException;
  public long[]       getThreadIntegerRegisterSet(int tid) throws DebuggerException;
  public long         getAddressValue(Address addr) throws DebuggerException;

  // for ProcCDebugger, ProcCFrame and SharedObject
  public List<ThreadProxy> getThreadList() throws DebuggerException;
  public List<LoadObject> getLoadObjectList() throws DebuggerException;
  public CFrame        topFrameForThread(ThreadProxy thread) throws DebuggerException;
  public ClosestSymbol lookup(long address) throws DebuggerException;
  public String        demangle(String name);
}
