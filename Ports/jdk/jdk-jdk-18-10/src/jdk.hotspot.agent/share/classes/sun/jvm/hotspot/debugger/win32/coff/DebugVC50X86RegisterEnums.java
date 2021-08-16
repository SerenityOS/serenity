/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.win32.coff;

public interface DebugVC50X86RegisterEnums {
  /** 8-bit registers */
  public static final int NONE = 0;
  public static final int AL = 1;
  public static final int CL = 2;
  public static final int DL = 3;
  public static final int BL = 4;
  public static final int AH = 5;
  public static final int CH = 6;
  public static final int DH = 7;
  public static final int BH = 8;

  /** 16-bit registers */
  public static final int AX = 9;
  public static final int CX = 10;
  public static final int DX = 11;
  public static final int BX = 12;
  public static final int SP = 13;
  public static final int BP = 14;
  public static final int SI = 15;
  public static final int DI = 16;

  /** 32-bit registers */
  public static final int EAX = 17;
  public static final int ECX = 18;
  public static final int EDX = 19;
  public static final int EBX = 20;
  public static final int ESP = 21;
  public static final int EBP = 22;
  public static final int ESI = 23;
  public static final int EDI = 24;

  /** Segment registers */
  public static final int ES = 25;
  public static final int CS = 26;
  public static final int SS = 27;
  public static final int DS = 28;
  public static final int FS = 29;
  public static final int GS = 30;

  /** Special cases */
  public static final int IP = 31;
  public static final int FLAGS = 32;
  public static final int EIP = 33;
  public static final int EFLAGS = 34;

  /** PCODE Registers */
  public static final int TEMP = 40;
  public static final int TEMPH = 41;
  public static final int QUOTE = 42;

  /** System Registers */
  public static final int CR0 = 80;
  public static final int CR1 = 81;
  public static final int CR2 = 82;
  public static final int CR3 = 83;
  public static final int DR0 = 90;
  public static final int DR1 = 91;
  public static final int DR2 = 92;
  public static final int DR3 = 93;
  public static final int DR4 = 94;
  public static final int DR5 = 95;
  public static final int DR6 = 96;
  public static final int DR7 = 97;

  /** Register extensions for 80x87 */
  public static final int ST0 = 128;
  public static final int ST1 = 129;
  public static final int ST2 = 130;
  public static final int ST3 = 131;
  public static final int ST4 = 132;
  public static final int ST5 = 133;
  public static final int ST6 = 134;
  public static final int ST7 = 135;
  public static final int CONTROL = 136;
  public static final int STATUS = 137;
  public static final int TAG = 138;
  public static final int FPIP = 139;
  public static final int FPCS = 140;
  public static final int FPDO = 141;
  public static final int FPDS = 142;
  public static final int ISEM = 143;
  public static final int FPEIP = 144;
  public static final int FPEDO = 145;
}
