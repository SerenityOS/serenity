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

/** Miscellaneous integer enumerations for symbol parsing */

public interface DebugVC50SymbolEnums {

  /** Machine types (S_COMPILE) */
  public static final byte MACHTYPE_INTEL_8080 = (byte) 0x00;
  public static final byte MACHTYPE_INTEL_8086 = (byte) 0x01;
  public static final byte MACHTYPE_INTEL_80286 = (byte) 0x02;
  public static final byte MACHTYPE_INTEL_80386 = (byte) 0x03;
  public static final byte MACHTYPE_INTEL_80486 = (byte) 0x04;
  public static final byte MACHTYPE_INTEL_PENTIUM = (byte) 0x05;
  public static final byte MACHTYPE_INTEL_PENTIUM_PRO = (byte) 0x06;
  public static final byte MACHTYPE_MIPS_R4000 = (byte) 0x10;
  public static final byte MACHTYPE_MIPS_RESERVED = (byte) 0x11;
  public static final byte MACHTYPE_MIPS_RESERVED2 = (byte) 0x12;
  public static final byte MACHTYPE_MC68000 = (byte) 0x20;
  public static final byte MACHTYPE_MC68010 = (byte) 0x21;
  public static final byte MACHTYPE_MC68020 = (byte) 0x22;
  public static final byte MACHTYPE_MC68030 = (byte) 0x23;
  public static final byte MACHTYPE_MC68040 = (byte) 0x24;
  public static final byte MACHTYPE_ALPHA = (byte) 0x30;
  public static final byte MACHTYPE_PPC601 = (byte) 0x40;
  public static final byte MACHTYPE_PPC603 = (byte) 0x41;
  public static final byte MACHTYPE_PPC604 = (byte) 0x42;
  public static final byte MACHTYPE_PPC620 = (byte) 0x43;

  /** Compile flags (S_COMPILE). All have masks and shifts because the
      data is bit-packed into three bytes in the file. (FIXME: test
      these masks and shifts to make sure they are correct.) */
  public static final int  COMPFLAG_LANGUAGE_MASK    = 0x00FF0000;
  public static final int  COMPFLAG_LANGUAGE_SHIFT   = 16;
  public static final int  COMPFLAG_LANGUAGE_C       = 0;
  public static final int  COMPFLAG_LANGUAGE_CPP     = 1; // C++
  public static final int  COMPFLAG_LANGUAGE_FORTRAN = 2;
  public static final int  COMPFLAG_LANGUAGE_MASM    = 3;
  public static final int  COMPFLAG_LANGUAGE_PASCAL  = 4;
  public static final int  COMPFLAG_LANGUAGE_BASIC   = 5;
  public static final int  COMPFLAG_LANGUAGE_COBOL   = 6;

  public static final int  COMPFLAG_PCODE_PRESENT_MASK  = 0x00008000;

  // Float precision enumeration
  public static final int  COMPFLAG_FLOAT_PRECISION_MASK   = 0x00006000;
  public static final int  COMPFLAG_FLOAT_PRECISION_SHIFT  = 13;
  public static final int  COMPFLAG_FLOAT_PRECISION_ANSI_C = 1;

  // Floating package enumeration
  public static final int  COMPFLAG_FLOAT_PACKAGE_MASK  = 0x00001800;
  public static final int  COMPFLAG_FLOAT_PACKAGE_SHIFT = 11;
  public static final int  COMPFLAG_FLOAT_PACKAGE_HARDWARE = 0;
  public static final int  COMPFLAG_FLOAT_PACKAGE_EMULATOR = 1;
  public static final int  COMPFLAG_FLOAT_PACKAGE_ALTMATH  = 2;

  // Ambient code/data memory model flags
  public static final int  COMPFLAG_AMBIENT_DATA_MASK  = 0x00000700;
  public static final int  COMPFLAG_AMBIENT_DATA_SHIFT = 12;
  public static final int  COMPFLAG_AMBIENT_CODE_MASK  = 0x000000E0;
  public static final int  COMPFLAG_AMBIENT_CODE_SHIFT = 8;
  public static final int  COMPFLAG_AMBIENT_MODEL_NEAR = 0;
  public static final int  COMPFLAG_AMBIENT_MODEL_FAR  = 1;
  public static final int  COMPFLAG_AMBIENT_MODEL_HUGE = 2;

  // Indicates whether program is compiled for 32-bit addresses
  public static final int  COMPFLAG_MODE32_MASK        = 0x00000010;

  /** Function return flags (S_RETURN) */
  // FIXME: verify these are correct
  public static final short FUNCRET_VARARGS_LEFT_TO_RIGHT_MASK  = (short) 0x0001;
  public static final short FUNCRET_RETURNEE_STACK_CLEANUP_MASK = (short) 0x0002;

  // Return styles
  public static final byte FUNCRET_VOID                   = (byte) 0x00;
  public static final byte FUNCRET_IN_REGISTERS           = (byte) 0x01;
  public static final byte FUNCRET_INDIRECT_CALLER_NEAR   = (byte) 0x02;
  public static final byte FUNCRET_INDIRECT_CALLER_FAR    = (byte) 0x03;
  public static final byte FUNCRET_INDIRECT_RETURNEE_NEAR = (byte) 0x04;
  public static final byte FUNCRET_INDIRECT_RETURNEE_FAR  = (byte) 0x05;

  /** Procedure flags */
  // FIXME: verify these are correct
  public static final byte PROCFLAGS_FRAME_POINTER_OMITTED = (byte) 0x01;
  public static final byte PROCFLAGS_INTERRUPT_ROUTINE     = (byte) 0x02;
  public static final byte PROCFLAGS_FAR_RETURN            = (byte) 0x04;
  public static final byte PROCFLAGS_NEVER_RETURN          = (byte) 0x08;

  /** Thunk types (S_THUNK32) */
  public static final byte THUNK_NO_TYPE  = (byte) 0;
  public static final byte THUNK_ADJUSTOR = (byte) 1;
  public static final byte THUNK_VCALL    = (byte) 2;
  public static final byte THUNK_PCODE    = (byte) 3;

  /** Execution models (S_CEXMODEL32) */
  public static final short EXMODEL_NOT_CODE              = (short) 0x00;
  public static final short EXMODEL_JUMP_TABLE            = (short) 0x01;
  public static final short EXMODEL_PADDING               = (short) 0x02;
  public static final short EXMODEL_NATIVE                = (short) 0x20;
  public static final short EXMODEL_MICROFOCUS_COBOL      = (short) 0x21;
  public static final short EXMODEL_PADDING_FOR_ALIGNMENT = (short) 0x22;
  public static final short EXMODEL_CODE                  = (short) 0x23;
  public static final short EXMODEL_PCODE                 = (short) 0x40;
}
