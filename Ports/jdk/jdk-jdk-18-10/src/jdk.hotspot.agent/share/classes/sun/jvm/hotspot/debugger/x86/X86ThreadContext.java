/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.x86;

import java.lang.annotation.Native;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

/** Specifies the thread context on x86 platforms; only a sub-portion
    of the context is guaranteed to be present on all operating
    systems. */

public abstract class X86ThreadContext implements ThreadContext {
  // Taken from /usr/include/ia32/sys/reg.h on Solaris/x86

  // NOTE: the indices for the various registers must be maintained as
  // listed across various operating systems. However, only a small
  // subset of the registers' values are guaranteed to be present (and
  // must be present for the SA's stack walking to work): EAX, EBX,
  // ECX, EDX, ESI, EDI, EBP, ESP, and EIP.

  // One instance of the Native annotation is enough to trigger header generation
  // for this file.
  @Native
  public static final int GS = 0;
  public static final int FS = 1;
  public static final int ES = 2;
  public static final int DS = 3;
  public static final int EDI = 4;
  public static final int ESI = 5;
  public static final int EBP = 6;
  public static final int ESP = 7;
  public static final int EBX = 8;
  public static final int EDX = 9;
  public static final int ECX = 10;
  public static final int EAX = 11;
  public static final int TRAPNO = 12;
  public static final int ERR = 13;
  public static final int EIP = 14;
  public static final int CS = 15;
  public static final int EFL = 16;
  public static final int UESP = 17;
  public static final int SS = 18;
  // Additional state (not in reg.h) for debug registers
  public static final int DR0 = 19;
  public static final int DR1 = 20;
  public static final int DR2 = 21;
  public static final int DR3 = 22;
  public static final int DR6 = 23;
  public static final int DR7 = 24;


  public static final int PC = EIP;
  public static final int FP = EBP;
  public static final int SP = UESP;
  public static final int PS = EFL;
  public static final int R0 = EAX;
  public static final int R1 = EDX;

  public static final int NPRGREG = 25;

  private static final String[] regNames = {
    "GS",     "FS",    "ES",    "DS",
    "EDI",    "ESI",   "EBP",   "ESP",
    "EBX",    "EDX",   "ECX",   "EAX",
    "TRAPNO", "ERR",   "EIP",   "CS",
    "EFLAGS", "UESP",  "SS",
    "DR0",    "DR1",   "DR2",   "DR3",
    "DR6",    "DR7"
  };

  // Ought to be int on x86 but we're stuck
  private long[] data;

  public X86ThreadContext() {
    data = new long[NPRGREG];
  }

  public int getNumRegisters() {
    return NPRGREG;
  }

  public String getRegisterName(int index) {
    return regNames[index];
  }

  public void setRegister(int index, long value) {
    data[index] = value;
  }

  public long getRegister(int index) {
    return data[index];
  }

  public CFrame getTopFrame(Debugger dbg) {
    return null;
  }

  /** This can't be implemented in this class since we would have to
      tie the implementation to, for example, the debugging system */
  public abstract void setRegisterAsAddress(int index, Address value);

  /** This can't be implemented in this class since we would have to
      tie the implementation to, for example, the debugging system */
  public abstract Address getRegisterAsAddress(int index);
}
