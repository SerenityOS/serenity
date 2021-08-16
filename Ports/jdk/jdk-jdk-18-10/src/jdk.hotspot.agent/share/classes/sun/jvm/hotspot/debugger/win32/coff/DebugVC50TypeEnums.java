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

/** Various enumerated values used in type leaves */

public interface DebugVC50TypeEnums {
  /** LF_MODIFIER attributes */
  public static final int MODIFIER_CONST_MASK     = 0x01;
  public static final int MODIFIER_VOLATILE_MASK  = 0x02;
  public static final int MODIFIER_UNALIGNED_MASK = 0x04;

  /** LF_POINTER bitfields */
  // FIXME: verify these are correct
  // ptrtype field
  public static final int POINTER_PTRTYPE_MASK  = 0x0000001F;
  public static final int POINTER_PTRTYPE_SHIFT = 0;
  public static final int POINTER_PTRTYPE_NEAR  = 0;
  public static final int POINTER_PTRTYPE_FAR   = 1;
  public static final int POINTER_PTRTYPE_HUGE  = 2;
  /** Obsolete */
  public static final int POINTER_PTRTYPE_BASED_ON_SEGMENT = 3;
  public static final int POINTER_PTRTYPE_BASED_ON_VALUE   = 4;
  /** Obsolete */
  public static final int POINTER_PTRTYPE_BASED_ON_SEGMENT_OF_VALUE = 5;
  /** Obsolete */
  public static final int POINTER_PTRTYPE_BASED_ON_ADDRESS_OF_SYMBOL = 6;
  /** Obsolete */
  public static final int POINTER_PTRTYPE_BASED_ON_SEGMENT_OF_SYMBOL_ADDRESS = 7;
  public static final int POINTER_PTRTYPE_BASED_ON_TYPE = 8;
  /** Obsolete */
  public static final int POINTER_PTRTYPE_BASED_ON_SELF = 9;
  public static final int POINTER_PTRTYPE_NEAR_32_BIT = 10;
  public static final int POINTER_PTRTYPE_FAR_32_BIT  = 11;
  public static final int POINTER_PTRTYPE_64_BIT      = 12;

  // ptrmode field
  // FIXME: verify these are correct
  public static final int POINTER_PTRMODE_MASK  = 0x000000E0;
  public static final int POINTER_PTRMODE_SHIFT = 5;
  public static final int POINTER_PTRMODE_POINTER            = 0;
  public static final int POINTER_PTRMODE_REFERENCE          = 1;
  public static final int POINTER_PTRMODE_PTR_TO_DATA_MEMBER = 2;
  public static final int POINTER_PTRMODE_PTR_TO_METHOD      = 3;

  // FIXME: verify this is correct
  public static final int POINTER_ISFLAT32_MASK  = 0x00000100;

  // FIXME: verify this is correct
  public static final int POINTER_VOLATILE_MASK  = 0x00000200;

  // FIXME: verify this is correct
  public static final int POINTER_CONST_MASK     = 0x00000400;

  // FIXME: verify this is correct
  public static final int POINTER_UNALIGNED_MASK = 0x00000800;

  // FIXME: verify this is correct
  public static final int POINTER_RESTRICT_MASK  = 0x00001000;

  /** <p> 16:32 data for classes with or without virtual functions and
      no virtual bases. Pointer layout is: </p>

      <p>
      <table width = "15%">
      <tr> <td> 4
      <tr> <td> mdisp
      </table>
      </p>

      <p>
      <i>mdisp</i>: displacement to data
      </p>

      <p>
      NULL value is 0x80000000.
      </p>
  */
  public static final short PTR_FORMAT_DATA_NVF_NVB = (short) 3;

  /** <p> 16:32 data for class with virtual bases. Pointer layout is:</p>

      <p>
      <table width = "45%">
      <tr> <td> 4 <td> 4 <td> 4
      <tr> <td> mdisp <td> pdisp> <td> vdisp
      </table>
      </p>

      <p>
      <i>mdisp</i>: displacement to data
      </p>

      <p>
      <i>pdisp</i>: <b>this</b> pointer displacement to virtual base table pointer
      </p>

      <p>
      <i>vdisp</i>: displacement within virtual base table
      </p>

      <p>
      NULL value is (*,*,0xffffffff).
      </p>
  */
  public static final short PTR_FORMAT_DATA_VB = (short) 4;

  /** <p> 16:32 method nonvirtual bases with single address point.
      Pointer layout is: </p>

      <p>
      <table width = "15%">
      <tr> <td> 4
      <tr> <td> off
      </table>
      </p>

      <p>
      <i>off</i>: offset of function
      </p>

      <p>
      NULL value is 0L.
      </p>
  */
  public static final short PTR_FORMAT_METHOD_NVB_SAP = (short) 11;

  /** <p> 16:32 method nonvirtual bases with multiple address points.
      Pointer layout is: </p>

      <p>
      <table width = "30%">
      <tr> <td> 4 <td> 4
      <tr> <td> off <td> disp
      </table>
      </p>

      <p>
      <i>off</i>: offset of function
      </p>

      <p>
      <i>disp</i>: displacement of address point.
      </p>

      <p>
      NULL value is (0L : 0L).
      </p>
  */
  public static final short PTR_FORMAT_METHOD_NVB_MAP = (short) 12;

  /** <p> 16:32 method with virtual bases. Pointer layout is: </p>

      <p>
      <table width = "60%">
      <tr> <td> 4 <td> 4 <td> 4 <td> 4
      <tr> <td> off <td> mdisp <td> pdisp <td> vdisp
      </table>
      </p>

      <p>
      <i>off</i>: offset of function
      </p>

      <p>
      <i>mdisp</i>: displacement to data
      </p>

      <p>
      <i>pdisp</i>: <b>this</b> pointer displacement to virtual base
      table pointer
      </p>

      <p>
      <i>vdisp</i>: displacement within virtual base table
      </p>

      NULL value is (0L, *, *, *).
  */
  public static final short PTR_FORMAT_METHOD_VB = (short) 13;

  /** Class, structure, union, and enum properties */
  // FIXME: verify these are correct
  /** Structure is packed */
  public static final short PROPERTY_PACKED   = (short) 0x001;
  /** Class has constructors and/or destructors */
  public static final short PROPERTY_CTOR     = (short) 0x002;
  /** Class has overloaded operators */
  public static final short PROPERTY_OVEROPS  = (short) 0x004;
  /** Class is a nested class */
  public static final short PROPERTY_ISNESTED = (short) 0x008;
  /** Class contains nested classes */
  public static final short PROPERTY_CNESTED  = (short) 0x010;
  /** Class has overloaded assignment */
  public static final short PROPERTY_OPASSIGN = (short) 0x020;
  /** Class has casting methods */
  public static final short PROPERTY_OPCAST   = (short) 0x040;
  /** Class/structure is a forward (incomplete) reference */
  public static final short PROPERTY_FWDREF   = (short) 0x080;
  /** This is a scoped definition */
  public static final short PROPERTY_SCOPED   = (short) 0x100;

  /** Calling conventions */
  /** Arguments pushed right to left, caller pops arguments. */
  public static final byte CALLCONV_NEAR_C        = (byte)  0;
  public static final byte CALLCONV_FAR_C         = (byte)  1;
  public static final byte CALLCONV_NEAR_PASCAL   = (byte)  2;
  /** Arguments pushed left to right, callee pops arguments. */
  public static final byte CALLCONV_FAR_PASCAL    = (byte)  3;
  public static final byte CALLCONV_NEAR_FASTCALL = (byte)  4;
  public static final byte CALLCONV_FAR_FASTCALL  = (byte)  5;
  public static final byte CALLCONV_RESERVED      = (byte)  6;
  public static final byte CALLCONV_NEAR_STDCALL  = (byte)  7;
  public static final byte CALLCONV_FAR_STDCALL   = (byte)  8;
  public static final byte CALLCONV_NEAR_SYSCALL  = (byte)  9;
  public static final byte CALLCONV_FAR_SYSCALL   = (byte) 10;
  public static final byte CALLCONV_THIS_CALL     = (byte) 11;
  public static final byte CALLCONV_MIPS_CALL     = (byte) 12;
  public static final byte CALLCONV_GENERIC       = (byte) 13;

  /** vtable entry descriptors */
  public static final int VTENTRY_NEAR                 = 0;
  public static final int VTENTRY_FAR                  = 1;
  public static final int VTENTRY_THIN                 = 2;
  /** Address point displacement to outermost class. This is at
      entry[-1] from table address. */
  public static final int VTENTRY_ADDRESS_PT_DISP      = 3;
  /** Far pointer to metaclass descriptor. This is at entry[-2] from
      table address. */
  public static final int VTENTRY_FAR_PTR_TO_METACLASS = 4;
  public static final int VTENTRY_NEAR_32              = 5;
  public static final int VTENTRY_FAR_32               = 6;

  /** Label addressing modes */
  public static final short LABEL_ADDR_MODE_NEAR = (short) 0;
  public static final short LABEL_ADDR_MODE_FAR  = (short) 4;

  //
  // Primitive/reserved type enumerations
  //

  // FIXME: verify these are correct
  // Type field
  public static final int RESERVED_TYPE_MASK         = 0x070;
  public static final int RESERVED_TYPE_SPECIAL      = 0x000;
  public static final int RESERVED_TYPE_SIGNED_INT   = 0x010;
  public static final int RESERVED_TYPE_UNSIGNED_INT = 0x020;
  public static final int RESERVED_TYPE_BOOLEAN      = 0x030;
  public static final int RESERVED_TYPE_REAL         = 0x040;
  public static final int RESERVED_TYPE_COMPLEX      = 0x050;
  public static final int RESERVED_TYPE_SPECIAL2     = 0x060;
  public static final int RESERVED_TYPE_REALLY_INT   = 0x070;

  // Mode field
  public static final int RESERVED_MODE_MASK         = 0x700;
  public static final int RESERVED_MODE_DIRECT       = 0x000;
  public static final int RESERVED_MODE_NEAR_PTR     = 0x100;
  public static final int RESERVED_MODE_FAR_PTR      = 0x200;
  public static final int RESERVED_MODE_HUGE_PTR     = 0x300;
  public static final int RESERVED_MODE_NEAR_32_PTR  = 0x400;
  public static final int RESERVED_MODE_FAR_32_PTR   = 0x500;
  public static final int RESERVED_MODE_NEAR_64_PTR  = 0x600;

  // Size field for each of the types above.
  // Has different meanings based on type.
  public static final int RESERVED_SIZE_MASK                      = 0x7;
  // Special type
  public static final int RESERVED_SIZE_SPECIAL_NO_TYPE           = 0x0;
  public static final int RESERVED_SIZE_SPECIAL_ABSOLUTE_SYMBOL   = 0x1;
  public static final int RESERVED_SIZE_SPECIAL_SEGMENT           = 0x2;
  public static final int RESERVED_SIZE_SPECIAL_VOID              = 0x3;
  public static final int RESERVED_SIZE_SPECIAL_BASIC_8_BYTE      = 0x4;
  public static final int RESERVED_SIZE_SPECIAL_NEAR_BASIC_STRING = 0x5;
  public static final int RESERVED_SIZE_SPECIAL_FAR_BASIC_STRING  = 0x6;
  public static final int RESERVED_SIZE_SPECIAL_UNTRANSLATED      = 0x7;
  // Signed, unsigned and boolean types
  public static final int RESERVED_SIZE_INT_1_BYTE                = 0x0;
  public static final int RESERVED_SIZE_INT_2_BYTE                = 0x1;
  public static final int RESERVED_SIZE_INT_4_BYTE                = 0x2;
  public static final int RESERVED_SIZE_INT_8_BYTE                = 0x3;
  // Real and complex types
  public static final int RESERVED_SIZE_REAL_32_BIT               = 0x0;
  public static final int RESERVED_SIZE_REAL_64_BIT               = 0x1;
  public static final int RESERVED_SIZE_REAL_80_BIT               = 0x2;
  public static final int RESERVED_SIZE_REAL_128_BIT              = 0x3;
  public static final int RESERVED_SIZE_REAL_48_BIT               = 0x4;
  // Special2 type
  public static final int RESERVED_SIZE_SPECIAL2_BIT              = 0x0;
  public static final int RESERVED_SIZE_SPECIAL2_PASCAL_CHAR      = 0x1;
  // Really int type
  public static final int RESERVED_SIZE_REALLY_INT_CHAR           = 0x0;
  public static final int RESERVED_SIZE_REALLY_INT_WCHAR          = 0x1;
  public static final int RESERVED_SIZE_REALLY_INT_2_BYTE         = 0x2; // 2 byte signed integer
  public static final int RESERVED_SIZE_REALLY_INT_2_BYTE_U       = 0x3; // 2 byte unsigned integer
  public static final int RESERVED_SIZE_REALLY_INT_4_BYTE         = 0x4; // 4 byte signed integer
  public static final int RESERVED_SIZE_REALLY_INT_4_BYTE_U       = 0x5; // 4 byte unsigned integer
  public static final int RESERVED_SIZE_REALLY_INT_8_BYTE         = 0x6; // 8 byte signed integer
  public static final int RESERVED_SIZE_REALLY_INT_8_BYTE_U       = 0x7; // 8 byte unsigned integer
}
