/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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

#ifndef CPU_S390_REGISTER_S390_HPP
#define CPU_S390_REGISTER_S390_HPP

#include "asm/register.hpp"
#include "runtime/vm_version.hpp"

class Address;
class VMRegImpl;

typedef VMRegImpl* VMReg;


// z/Architecture registers, see "LINUX for zSeries ELF ABI Supplement", IBM March 2001
//
//   r0-r1     General purpose (volatile)
//   r2        Parameter and return value (volatile)
//   r3        TOC pointer (volatile)
//   r3-r5     Parameters (volatile)
//   r6        Parameter (nonvolatile)
//   r7-r11    Locals (nonvolatile)
//   r12       Local, often used as GOT pointer (nonvolatile)
//   r13       Local, often used as toc (nonvolatile)
//   r14       return address (volatile)
//   r15       stack pointer (nonvolatile)
//
//   f0,f2,f4,f6 Parameters (volatile)
//   f1,f3,f5,f7 General purpose (volatile)
//   f8-f15      General purpose (nonvolatile)


//===========================
//===  Integer Registers  ===
//===========================

// Use Register as shortcut.
class RegisterImpl;
typedef RegisterImpl* Register;

// The implementation of integer registers for z/Architecture.

inline Register as_Register(int encoding) {
  return (Register)(long)encoding;
}

class RegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers     = 16,
    number_of_arg_registers = 5
  };

  // general construction
  inline friend Register as_Register(int encoding);

  inline VMReg as_VMReg();

  // accessors
  int   encoding() const      { assert(is_valid(), "invalid register"); return value(); }
  const char* name() const;

  // testers
  bool is_valid() const       { return (0 <= (value()&0x7F) && (value()&0x7F) < number_of_registers); }
  bool is_even() const        { return (encoding() & 1) == 0; }
  bool is_volatile() const    { return (0 <= (value()&0x7F) && (value()&0x7F) <= 5) || (value()&0x7F)==14; }
  bool is_nonvolatile() const { return is_valid() && !is_volatile(); }

 public:
  // derived registers, offsets, and addresses
  Register predecessor() const { return as_Register((encoding()-1) & (number_of_registers-1)); }
  Register successor() const   { return as_Register((encoding() + 1) & (number_of_registers-1)); }
};

// The integer registers of the z/Architecture.

CONSTANT_REGISTER_DECLARATION(Register, noreg, (-1));

CONSTANT_REGISTER_DECLARATION(Register, Z_R0,   (0));
CONSTANT_REGISTER_DECLARATION(Register, Z_R1,   (1));
CONSTANT_REGISTER_DECLARATION(Register, Z_R2,   (2));
CONSTANT_REGISTER_DECLARATION(Register, Z_R3,   (3));
CONSTANT_REGISTER_DECLARATION(Register, Z_R4,   (4));
CONSTANT_REGISTER_DECLARATION(Register, Z_R5,   (5));
CONSTANT_REGISTER_DECLARATION(Register, Z_R6,   (6));
CONSTANT_REGISTER_DECLARATION(Register, Z_R7,   (7));
CONSTANT_REGISTER_DECLARATION(Register, Z_R8,   (8));
CONSTANT_REGISTER_DECLARATION(Register, Z_R9,   (9));
CONSTANT_REGISTER_DECLARATION(Register, Z_R10, (10));
CONSTANT_REGISTER_DECLARATION(Register, Z_R11, (11));
CONSTANT_REGISTER_DECLARATION(Register, Z_R12, (12));
CONSTANT_REGISTER_DECLARATION(Register, Z_R13, (13));
CONSTANT_REGISTER_DECLARATION(Register, Z_R14, (14));
CONSTANT_REGISTER_DECLARATION(Register, Z_R15, (15));


//=============================
//===  Condition Registers  ===
//=============================

// Use ConditionRegister as shortcut
class ConditionRegisterImpl;
typedef ConditionRegisterImpl* ConditionRegister;

// The implementation of condition register(s) for the z/Architecture.

class ConditionRegisterImpl: public AbstractRegisterImpl {
 public:

  enum {
    number_of_registers = 1
  };

  // accessors
  int encoding() const {
    assert(is_valid(), "invalid register"); return value();
  }

  // testers
  bool is_valid() const {
    return (0 <= value() && value() < number_of_registers);
  }
  bool is_volatile() const {
    return true;
  }
  bool is_nonvolatile() const {
    return false;
  }

  // construction.
  inline friend ConditionRegister as_ConditionRegister(int encoding);

  inline VMReg as_VMReg();
};

inline ConditionRegister as_ConditionRegister(int encoding) {
  assert(encoding >= 0 && encoding < ConditionRegisterImpl::number_of_registers, "bad condition register encoding");
  return (ConditionRegister)(long)encoding;
}

// The condition register of the z/Architecture.

CONSTANT_REGISTER_DECLARATION(ConditionRegister, Z_CR, (0));

// Because z/Architecture has so many registers, #define'ing values for them is
// beneficial in code size and is worth the cost of some of the
// dangers of defines.
// If a particular file has a problem with these defines then it's possible
// to turn them off in that file by defining
// DONT_USE_REGISTER_DEFINES. Register_definitions_s390.cpp does that
// so that it's able to provide real definitions of these registers
// for use in debuggers and such.

#ifndef DONT_USE_REGISTER_DEFINES
#define noreg ((Register)(noreg_RegisterEnumValue))

#define Z_R0  ((Register)(Z_R0_RegisterEnumValue))
#define Z_R1  ((Register)(Z_R1_RegisterEnumValue))
#define Z_R2  ((Register)(Z_R2_RegisterEnumValue))
#define Z_R3  ((Register)(Z_R3_RegisterEnumValue))
#define Z_R4  ((Register)(Z_R4_RegisterEnumValue))
#define Z_R5  ((Register)(Z_R5_RegisterEnumValue))
#define Z_R6  ((Register)(Z_R6_RegisterEnumValue))
#define Z_R7  ((Register)(Z_R7_RegisterEnumValue))
#define Z_R8  ((Register)(Z_R8_RegisterEnumValue))
#define Z_R9  ((Register)(Z_R9_RegisterEnumValue))
#define Z_R10 ((Register)(Z_R10_RegisterEnumValue))
#define Z_R11 ((Register)(Z_R11_RegisterEnumValue))
#define Z_R12 ((Register)(Z_R12_RegisterEnumValue))
#define Z_R13 ((Register)(Z_R13_RegisterEnumValue))
#define Z_R14 ((Register)(Z_R14_RegisterEnumValue))
#define Z_R15 ((Register)(Z_R15_RegisterEnumValue))

#define Z_CR ((ConditionRegister)(Z_CR_ConditionRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES


//=========================
//===  Float Registers  ===
//=========================

// Use FloatRegister as shortcut
class FloatRegisterImpl;
typedef FloatRegisterImpl* FloatRegister;

// The implementation of float registers for the z/Architecture.

inline FloatRegister as_FloatRegister(int encoding) {
  return (FloatRegister)(long)encoding;
}

class FloatRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers     = 16,
    number_of_arg_registers = 4
  };

  // construction
  inline friend FloatRegister as_FloatRegister(int encoding);

  inline VMReg as_VMReg();

  // accessors
  int encoding() const                                {
     assert(is_valid(), "invalid register"); return value();
  }

  bool  is_valid() const          { return 0 <= value() && value() < number_of_registers; }
  bool is_volatile() const        { return (0 <= (value()&0x7F) && (value()&0x7F) <= 7); }
  bool is_nonvolatile() const     { return (8 <= (value()&0x7F) && (value()&0x7F) <= 15); }

  const char* name() const;

  FloatRegister successor() const { return as_FloatRegister(encoding() + 1); }
};

// The float registers of z/Architecture.

CONSTANT_REGISTER_DECLARATION(FloatRegister, fnoreg, (-1));

CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F0,  (0));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F1,  (1));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F2,  (2));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F3,  (3));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F4,  (4));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F5,  (5));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F6,  (6));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F7,  (7));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F8,  (8));
CONSTANT_REGISTER_DECLARATION(FloatRegister,  Z_F9,  (9));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F10, (10));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F11, (11));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F12, (12));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F13, (13));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F14, (14));
CONSTANT_REGISTER_DECLARATION(FloatRegister, Z_F15, (15));

#ifndef DONT_USE_REGISTER_DEFINES
#define fnoreg ((FloatRegister)(fnoreg_FloatRegisterEnumValue))
#define Z_F0  ((FloatRegister)(   Z_F0_FloatRegisterEnumValue))
#define Z_F1  ((FloatRegister)(   Z_F1_FloatRegisterEnumValue))
#define Z_F2  ((FloatRegister)(   Z_F2_FloatRegisterEnumValue))
#define Z_F3  ((FloatRegister)(   Z_F3_FloatRegisterEnumValue))
#define Z_F4  ((FloatRegister)(   Z_F4_FloatRegisterEnumValue))
#define Z_F5  ((FloatRegister)(   Z_F5_FloatRegisterEnumValue))
#define Z_F6  ((FloatRegister)(   Z_F6_FloatRegisterEnumValue))
#define Z_F7  ((FloatRegister)(   Z_F7_FloatRegisterEnumValue))
#define Z_F8  ((FloatRegister)(   Z_F8_FloatRegisterEnumValue))
#define Z_F9  ((FloatRegister)(   Z_F9_FloatRegisterEnumValue))
#define Z_F10 ((FloatRegister)(  Z_F10_FloatRegisterEnumValue))
#define Z_F11 ((FloatRegister)(  Z_F11_FloatRegisterEnumValue))
#define Z_F12 ((FloatRegister)(  Z_F12_FloatRegisterEnumValue))
#define Z_F13 ((FloatRegister)(  Z_F13_FloatRegisterEnumValue))
#define Z_F14 ((FloatRegister)(  Z_F14_FloatRegisterEnumValue))
#define Z_F15 ((FloatRegister)(  Z_F15_FloatRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES

// Single, Double and Quad fp reg classes. These exist to map the ADLC
// encoding for a floating point register, to the FloatRegister number
// desired by the macroassembler. A FloatRegister is a number between
// 0 and 31 passed around as a pointer. For ADLC, an fp register encoding
// is the actual bit encoding used by the z/Architecture hardware. When ADLC used
// the macroassembler to generate an instruction that references, e.g., a
// double fp reg, it passed the bit encoding to the macroassembler via
// as_FloatRegister, which, for double regs > 30, returns an illegal
// register number.
//
// Therefore we provide the following classes for use by ADLC. Their
// sole purpose is to convert from z/Architecture register encodings to FloatRegisters.
// At some future time, we might replace FloatRegister with these classes,
// hence the definitions of as_xxxFloatRegister as class methods rather
// than as external inline routines.

class SingleFloatRegisterImpl;
typedef SingleFloatRegisterImpl *SingleFloatRegister;

class SingleFloatRegisterImpl {
 public:
  friend FloatRegister as_SingleFloatRegister(int encoding) {
    assert(encoding < 32, "bad single float register encoding");
    return as_FloatRegister(encoding);
  }
};

class DoubleFloatRegisterImpl;
typedef DoubleFloatRegisterImpl *DoubleFloatRegister;

class DoubleFloatRegisterImpl {
 public:
  friend FloatRegister as_DoubleFloatRegister(int encoding) {
    assert(encoding < 32, "bad double float register encoding");
    return as_FloatRegister(((encoding & 1) << 5) | (encoding & 0x1e));
  }
};

class QuadFloatRegisterImpl;
typedef QuadFloatRegisterImpl *QuadFloatRegister;

class QuadFloatRegisterImpl {
 public:
  friend FloatRegister as_QuadFloatRegister(int encoding) {
    assert(encoding < 32 && ((encoding & 2) == 0), "bad quad float register encoding");
    return as_FloatRegister(((encoding & 1) << 5) | (encoding & 0x1c));
  }
};


//==========================
//===  Vector Registers  ===
//==========================

// Use VectorRegister as shortcut
class VectorRegisterImpl;
typedef VectorRegisterImpl* VectorRegister;

// The implementation of vector registers for z/Architecture.

inline VectorRegister as_VectorRegister(int encoding) {
  return (VectorRegister)(long)encoding;
}

class VectorRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers     = 32,
    number_of_arg_registers = 0
  };

  // construction
  inline friend VectorRegister as_VectorRegister(int encoding);

  inline VMReg as_VMReg();

  // accessors
  int encoding() const                                {
     assert(is_valid(), "invalid register"); return value();
  }

  bool is_valid() const           { return  0 <= value() && value() < number_of_registers; }
  bool is_volatile() const        { return true; }
  bool is_nonvolatile() const     { return false; }

  // Register fields in z/Architecture instructions are 4 bits wide, restricting the
  // addressable register set size to 16.
  // The vector register set size is 32, requiring an extension, by one bit, of the
  // register encoding. This is accomplished by the introduction of a RXB field in the
  // instruction. RXB = Register eXtension Bits.
  // The RXB field contains the MSBs (most significant bit) of the vector register numbers
  // used for this instruction. Assignment of MSB in RBX is by bit position of the
  // register field in the instruction.
  // Example:
  //   The register field starting at bit position 12 in the instruction is assigned RXB bit 0b0100.
  int64_t RXB_mask(int pos) {
    if (encoding() >= number_of_registers/2) {
      switch (pos) {
        case 8:   return ((int64_t)0b1000) << 8; // actual bit pos: 36
        case 12:  return ((int64_t)0b0100) << 8; // actual bit pos: 37
        case 16:  return ((int64_t)0b0010) << 8; // actual bit pos: 38
        case 32:  return ((int64_t)0b0001) << 8; // actual bit pos: 39
        default:
          ShouldNotReachHere();
      }
    }
    return 0;
  }

  const char* name() const;

  VectorRegister successor() const { return as_VectorRegister(encoding() + 1); }
};

// The Vector registers of z/Architecture.

CONSTANT_REGISTER_DECLARATION(VectorRegister, vnoreg, (-1));

CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V0,  (0));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V1,  (1));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V2,  (2));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V3,  (3));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V4,  (4));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V5,  (5));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V6,  (6));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V7,  (7));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V8,  (8));
CONSTANT_REGISTER_DECLARATION(VectorRegister,  Z_V9,  (9));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V10, (10));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V11, (11));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V12, (12));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V13, (13));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V14, (14));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V15, (15));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V16, (16));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V17, (17));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V18, (18));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V19, (19));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V20, (20));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V21, (21));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V22, (22));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V23, (23));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V24, (24));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V25, (25));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V26, (26));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V27, (27));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V28, (28));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V29, (29));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V30, (30));
CONSTANT_REGISTER_DECLARATION(VectorRegister, Z_V31, (31));

#ifndef DONT_USE_REGISTER_DEFINES
#define vnoreg ((VectorRegister)(vnoreg_VectorRegisterEnumValue))
#define Z_V0  ((VectorRegister)(   Z_V0_VectorRegisterEnumValue))
#define Z_V1  ((VectorRegister)(   Z_V1_VectorRegisterEnumValue))
#define Z_V2  ((VectorRegister)(   Z_V2_VectorRegisterEnumValue))
#define Z_V3  ((VectorRegister)(   Z_V3_VectorRegisterEnumValue))
#define Z_V4  ((VectorRegister)(   Z_V4_VectorRegisterEnumValue))
#define Z_V5  ((VectorRegister)(   Z_V5_VectorRegisterEnumValue))
#define Z_V6  ((VectorRegister)(   Z_V6_VectorRegisterEnumValue))
#define Z_V7  ((VectorRegister)(   Z_V7_VectorRegisterEnumValue))
#define Z_V8  ((VectorRegister)(   Z_V8_VectorRegisterEnumValue))
#define Z_V9  ((VectorRegister)(   Z_V9_VectorRegisterEnumValue))
#define Z_V10 ((VectorRegister)(  Z_V10_VectorRegisterEnumValue))
#define Z_V11 ((VectorRegister)(  Z_V11_VectorRegisterEnumValue))
#define Z_V12 ((VectorRegister)(  Z_V12_VectorRegisterEnumValue))
#define Z_V13 ((VectorRegister)(  Z_V13_VectorRegisterEnumValue))
#define Z_V14 ((VectorRegister)(  Z_V14_VectorRegisterEnumValue))
#define Z_V15 ((VectorRegister)(  Z_V15_VectorRegisterEnumValue))
#define Z_V16 ((VectorRegister)(  Z_V16_VectorRegisterEnumValue))
#define Z_V17 ((VectorRegister)(  Z_V17_VectorRegisterEnumValue))
#define Z_V18 ((VectorRegister)(  Z_V18_VectorRegisterEnumValue))
#define Z_V19 ((VectorRegister)(  Z_V19_VectorRegisterEnumValue))
#define Z_V20 ((VectorRegister)(  Z_V20_VectorRegisterEnumValue))
#define Z_V21 ((VectorRegister)(  Z_V21_VectorRegisterEnumValue))
#define Z_V22 ((VectorRegister)(  Z_V22_VectorRegisterEnumValue))
#define Z_V23 ((VectorRegister)(  Z_V23_VectorRegisterEnumValue))
#define Z_V24 ((VectorRegister)(  Z_V24_VectorRegisterEnumValue))
#define Z_V25 ((VectorRegister)(  Z_V25_VectorRegisterEnumValue))
#define Z_V26 ((VectorRegister)(  Z_V26_VectorRegisterEnumValue))
#define Z_V27 ((VectorRegister)(  Z_V27_VectorRegisterEnumValue))
#define Z_V28 ((VectorRegister)(  Z_V28_VectorRegisterEnumValue))
#define Z_V29 ((VectorRegister)(  Z_V29_VectorRegisterEnumValue))
#define Z_V30 ((VectorRegister)(  Z_V30_VectorRegisterEnumValue))
#define Z_V31 ((VectorRegister)(  Z_V31_VectorRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES


// Need to know the total number of registers of all sorts for SharedInfo.
// Define a class that exports it.

class ConcreteRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers =
      (RegisterImpl::number_of_registers +
      FloatRegisterImpl::number_of_registers)
      * 2 // register halves
      + 1 // condition code register
  };
  static const int max_gpr;
  static const int max_fpr;
};


// Common register declarations used in assembler code.
REGISTER_DECLARATION(Register,      Z_EXC_OOP, Z_R2);
REGISTER_DECLARATION(Register,      Z_EXC_PC,  Z_R3);
REGISTER_DECLARATION(Register,      Z_RET,     Z_R2);
REGISTER_DECLARATION(Register,      Z_ARG1,    Z_R2);
REGISTER_DECLARATION(Register,      Z_ARG2,    Z_R3);
REGISTER_DECLARATION(Register,      Z_ARG3,    Z_R4);
REGISTER_DECLARATION(Register,      Z_ARG4,    Z_R5);
REGISTER_DECLARATION(Register,      Z_ARG5,    Z_R6);
REGISTER_DECLARATION(Register,      Z_SP,     Z_R15);
REGISTER_DECLARATION(FloatRegister, Z_FRET,    Z_F0);
REGISTER_DECLARATION(FloatRegister, Z_FARG1,   Z_F0);
REGISTER_DECLARATION(FloatRegister, Z_FARG2,   Z_F2);
REGISTER_DECLARATION(FloatRegister, Z_FARG3,   Z_F4);
REGISTER_DECLARATION(FloatRegister, Z_FARG4,   Z_F6);

#ifndef DONT_USE_REGISTER_DEFINES
#define Z_EXC_OOP         AS_REGISTER(Register,  Z_R2)
#define Z_EXC_PC          AS_REGISTER(Register,  Z_R3)
#define Z_RET             AS_REGISTER(Register,  Z_R2)
#define Z_ARG1            AS_REGISTER(Register,  Z_R2)
#define Z_ARG2            AS_REGISTER(Register,  Z_R3)
#define Z_ARG3            AS_REGISTER(Register,  Z_R4)
#define Z_ARG4            AS_REGISTER(Register,  Z_R5)
#define Z_ARG5            AS_REGISTER(Register,  Z_R6)
#define Z_SP              AS_REGISTER(Register, Z_R15)
#define Z_FRET            AS_REGISTER(FloatRegister, Z_F0)
#define Z_FARG1           AS_REGISTER(FloatRegister, Z_F0)
#define Z_FARG2           AS_REGISTER(FloatRegister, Z_F2)
#define Z_FARG3           AS_REGISTER(FloatRegister, Z_F4)
#define Z_FARG4           AS_REGISTER(FloatRegister, Z_F6)
#endif

// Register declarations to be used in frame manager assembly code.
// Use only non-volatile registers in order to keep values across C-calls.

// Register to cache the integer value on top of the operand stack.
REGISTER_DECLARATION(Register, Z_tos,         Z_R2);
// Register to cache the fp value on top of the operand stack.
REGISTER_DECLARATION(FloatRegister, Z_ftos,   Z_F0);
// Expression stack pointer in interpreted java frame.
REGISTER_DECLARATION(Register, Z_esp,         Z_R7);
// Address of current thread.
REGISTER_DECLARATION(Register, Z_thread,      Z_R8);
// Address of current method. only valid in interpreter_entry.
REGISTER_DECLARATION(Register, Z_method,      Z_R9);
// Inline cache register. used by c1 and c2.
REGISTER_DECLARATION(Register, Z_inline_cache,Z_R9);
// Frame pointer of current interpreter frame. only valid while
// executing bytecodes.
REGISTER_DECLARATION(Register, Z_fp,          Z_R9);
// Address of the locals array in an interpreted java frame.
REGISTER_DECLARATION(Register, Z_locals,      Z_R12);
// Bytecode pointer.
REGISTER_DECLARATION(Register, Z_bcp,         Z_R13);
// Bytecode which is dispatched (short lived!).
REGISTER_DECLARATION(Register, Z_bytecode,    Z_R14);
#ifndef DONT_USE_REGISTER_DEFINES
#define Z_tos             AS_REGISTER(Register, Z_R2)
#define Z_ftos            AS_REGISTER(FloatRegister, Z_F0)
#define Z_esp             AS_REGISTER(Register, Z_R7)
#define Z_thread          AS_REGISTER(Register, Z_R8)
#define Z_method          AS_REGISTER(Register, Z_R9)
#define Z_inline_cache    AS_REGISTER(Register, Z_R9)
#define Z_fp              AS_REGISTER(Register, Z_R9)
#define Z_locals          AS_REGISTER(Register, Z_R12)
#define Z_bcp             AS_REGISTER(Register, Z_R13)
#define Z_bytecode        AS_REGISTER(Register, Z_R14)
#endif

// Temporary registers to be used within frame manager. We can use
// the nonvolatiles because the call stub has saved them.
// Use only non-volatile registers in order to keep values across C-calls.
REGISTER_DECLARATION(Register, Z_tmp_1,  Z_R10);
REGISTER_DECLARATION(Register, Z_tmp_2,  Z_R11);
REGISTER_DECLARATION(Register, Z_tmp_3,  Z_R12);
REGISTER_DECLARATION(Register, Z_tmp_4,  Z_R13);
#ifndef DONT_USE_REGISTER_DEFINES
#define Z_tmp_1      AS_REGISTER(Register, Z_R10)
#define Z_tmp_2      AS_REGISTER(Register, Z_R11)
#define Z_tmp_3      AS_REGISTER(Register, Z_R12)
#define Z_tmp_4      AS_REGISTER(Register, Z_R13)
#endif

// Scratch registers are volatile.
REGISTER_DECLARATION(Register, Z_R0_scratch, Z_R0);
REGISTER_DECLARATION(Register, Z_R1_scratch, Z_R1);
REGISTER_DECLARATION(FloatRegister, Z_fscratch_1, Z_F1);
#ifndef DONT_USE_REGISTER_DEFINES
#define Z_R0_scratch  AS_REGISTER(Register, Z_R0)
#define Z_R1_scratch  AS_REGISTER(Register, Z_R1)
#define Z_fscratch_1  AS_REGISTER(FloatRegister, Z_F1)
#endif


#endif // CPU_S390_REGISTER_S390_HPP
