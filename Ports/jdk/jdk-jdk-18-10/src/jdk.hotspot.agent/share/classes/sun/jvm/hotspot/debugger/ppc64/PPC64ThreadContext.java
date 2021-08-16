/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.ppc64;

import java.lang.annotation.Native;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

/** Specifies the thread context on ppc64 platforms; only a sub-portion
 * of the context is guaranteed to be present on all operating
 * systems. */

public abstract class PPC64ThreadContext implements ThreadContext {

  // NOTE: The indices for the various registers must be maintained as
  // listed across various operating systems. However, only a small
  // subset of the registers' values are guaranteed to be present (and
  // must be present for the SA's stack walking to work).

  // One instance of the Native annotation is enough to trigger header generation
  // for this file.
  @Native
  public static final int R31 = 0;
  public static final int R30 = 1;
  public static final int R29 = 2;
  public static final int R28 = 3;
  public static final int R27 = 4;
  public static final int R26 = 5;
  public static final int R25 = 6;
  public static final int R24 = 7;
  public static final int R23 = 8;
  public static final int R22 = 9;
  public static final int R21 = 10;
  public static final int R20 = 11;
  public static final int R19 = 12;
  public static final int R18 = 13;
  public static final int R17 = 14;
  public static final int R16 = 15;
  public static final int R15 = 16;
  public static final int R14 = 17;
  public static final int R13 = 18;
  public static final int R12 = 19;
  public static final int R11 = 20;
  public static final int R10 = 21;
  public static final int R9 = 22;
  public static final int R8 = 23;
  public static final int R7 = 24;
  public static final int R6 = 25;
  public static final int R5 = 26;
  public static final int R4 = 27;
  public static final int R3 = 28;
  public static final int R2 = 29;
  public static final int R1 = 30;
  public static final int R0 = 31;
  public static final int NIP = 32;
  public static final int LR = 33;

  public static final int NPRGREG = 34;

  private static final String[] regNames = {
    "r31", "r30", "r29", "r28", "r27", "r26", "r25", "r24",
    "r23", "r22", "r21", "r20", "r19", "r18", "r17", "r16",
    "r15", "r14", "r13", "r12", "r11", "r10", "r9",  "r8",
    "r7",  "r6",  "r5",  "r4",  "r3",   "r2", "r1",  "r0",
    "nip", "link"
  };

  public static final int PC = NIP;
  public static final int SP = R1;

  private long[] data;

  public PPC64ThreadContext() {
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
   * tie the implementation to, for example, the debugging system */
  public abstract void setRegisterAsAddress(int index, Address value);

  /** This can't be implemented in this class since we would have to
   * tie the implementation to, for example, the debugging system */
  public abstract Address getRegisterAsAddress(int index);

}
