/*
 * Copyright (c) 2000, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger;

import sun.jvm.hotspot.debugger.cdbg.*;

/** This is a placeholder interface for a thread's context, containing
    only integer registers (no floating-point ones). What it contains
    is platform-dependent. Not all registers are guaranteed to be
    present in the context or read from the target process in all
    situations. However, the routines in it are exposed to allow
    platform-independent iteration. */

public interface ThreadContext {
  /** Number of integer registers in the context */
  public int getNumRegisters();

  /** Get the name of the specified register (0..getNumRegisters() -
      1) */
  public String getRegisterName(int i);

  /** Get the value of the specified register (0..getNumRegisters() -
      1) */
  public long getRegister(int index);

  /** Set the value of the specified register (0..getNumRegisters() -
      1) */
  public void setRegister(int index, long value);

  /** Get the value of the specified register (0..getNumRegisters() -
      1) as an Address */
  public Address getRegisterAsAddress(int index);

  /** Set the value of the specified register (0..getNumRegisters() -
      1) as an Address */
  public void setRegisterAsAddress(int index, Address value);

  public CFrame getTopFrame(Debugger dbg);
}
