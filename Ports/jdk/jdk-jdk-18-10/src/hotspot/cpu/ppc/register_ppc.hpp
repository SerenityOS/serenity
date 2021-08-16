/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2018 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_REGISTER_PPC_HPP
#define CPU_PPC_REGISTER_PPC_HPP

#include "asm/register.hpp"

// forward declaration
class Address;
class VMRegImpl;
typedef VMRegImpl* VMReg;

//  PPC64 registers
//
//  See "64-bit PowerPC ELF ABI Supplement 1.7", IBM Corp. (2003-10-29).
//  (http://math-atlas.sourceforge.net/devel/assembly/PPC-elf64abi-1.7.pdf)
//
//  r0        Register used in function prologs (volatile)
//  r1        Stack pointer (nonvolatile)
//  r2        TOC pointer (volatile)
//  r3        Parameter and return value (volatile)
//  r4-r10    Function parameters (volatile)
//  r11       Register used in calls by pointer and as an environment pointer for languages which require one (volatile)
//  r12       Register used for exception handling and glink code (volatile)
//  r13       Reserved for use as system thread ID
//  r14-r31   Local variables (nonvolatile)
//
//  f0        Scratch register (volatile)
//  f1-f4     Floating point parameters and return value (volatile)
//  f5-f13    Floating point parameters (volatile)
//  f14-f31   Floating point values (nonvolatile)
//
//  LR        Link register for return address (volatile)
//  CTR       Loop counter (volatile)
//  XER       Fixed point exception register (volatile)
//  FPSCR     Floating point status and control register (volatile)
//
//  CR0-CR1   Condition code fields (volatile)
//  CR2-CR4   Condition code fields (nonvolatile)
//  CR5-CR7   Condition code fields (volatile)
//
//  ----------------------------------------------
//  On processors with the VMX feature:
//  v0-v1     Volatile scratch registers
//  v2-v13    Volatile vector parameters registers
//  v14-v19   Volatile scratch registers
//  v20-v31   Non-volatile registers
//  vrsave    Non-volatile 32-bit register


// Use Register as shortcut
class RegisterImpl;
typedef RegisterImpl* Register;

inline Register as_Register(int encoding) {
  assert(encoding >= -1 && encoding < 32, "bad register encoding");
  return (Register)(intptr_t)encoding;
}

// The implementation of integer registers for the Power architecture
class RegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 32
  };

  // general construction
  inline friend Register as_Register(int encoding);

  // accessors
  int encoding() const { assert(is_valid(), "invalid register"); return value(); }
  inline VMReg as_VMReg();
  Register successor() const { return as_Register(encoding() + 1); }

  // testers
  bool is_valid()       const { return ( 0 <= (value()&0x7F) && (value()&0x7F) <  number_of_registers); }
  bool is_volatile()    const { return ( 0 <= (value()&0x7F) && (value()&0x7F) <= 13 ); }
  bool is_nonvolatile() const { return (14 <= (value()&0x7F) && (value()&0x7F) <= 31 ); }

  const char* name() const;
};

// The integer registers of the PPC architecture
CONSTANT_REGISTER_DECLARATION(Register, noreg, (-1));

CONSTANT_REGISTER_DECLARATION(Register, R0,   (0));
CONSTANT_REGISTER_DECLARATION(Register, R1,   (1));
CONSTANT_REGISTER_DECLARATION(Register, R2,   (2));
CONSTANT_REGISTER_DECLARATION(Register, R3,   (3));
CONSTANT_REGISTER_DECLARATION(Register, R4,   (4));
CONSTANT_REGISTER_DECLARATION(Register, R5,   (5));
CONSTANT_REGISTER_DECLARATION(Register, R6,   (6));
CONSTANT_REGISTER_DECLARATION(Register, R7,   (7));
CONSTANT_REGISTER_DECLARATION(Register, R8,   (8));
CONSTANT_REGISTER_DECLARATION(Register, R9,   (9));
CONSTANT_REGISTER_DECLARATION(Register, R10, (10));
CONSTANT_REGISTER_DECLARATION(Register, R11, (11));
CONSTANT_REGISTER_DECLARATION(Register, R12, (12));
CONSTANT_REGISTER_DECLARATION(Register, R13, (13));
CONSTANT_REGISTER_DECLARATION(Register, R14, (14));
CONSTANT_REGISTER_DECLARATION(Register, R15, (15));
CONSTANT_REGISTER_DECLARATION(Register, R16, (16));
CONSTANT_REGISTER_DECLARATION(Register, R17, (17));
CONSTANT_REGISTER_DECLARATION(Register, R18, (18));
CONSTANT_REGISTER_DECLARATION(Register, R19, (19));
CONSTANT_REGISTER_DECLARATION(Register, R20, (20));
CONSTANT_REGISTER_DECLARATION(Register, R21, (21));
CONSTANT_REGISTER_DECLARATION(Register, R22, (22));
CONSTANT_REGISTER_DECLARATION(Register, R23, (23));
CONSTANT_REGISTER_DECLARATION(Register, R24, (24));
CONSTANT_REGISTER_DECLARATION(Register, R25, (25));
CONSTANT_REGISTER_DECLARATION(Register, R26, (26));
CONSTANT_REGISTER_DECLARATION(Register, R27, (27));
CONSTANT_REGISTER_DECLARATION(Register, R28, (28));
CONSTANT_REGISTER_DECLARATION(Register, R29, (29));
CONSTANT_REGISTER_DECLARATION(Register, R30, (30));
CONSTANT_REGISTER_DECLARATION(Register, R31, (31));


//
// Because Power has many registers, #define'ing values for them is
// beneficial in code size and is worth the cost of some of the
// dangers of defines. If a particular file has a problem with these
// defines then it's possible to turn them off in that file by
// defining DONT_USE_REGISTER_DEFINES. Register_definition_ppc.cpp
// does that so that it's able to provide real definitions of these
// registers for use in debuggers and such.
//

#ifndef DONT_USE_REGISTER_DEFINES
#define noreg ((Register)(noreg_RegisterEnumValue))

#define R0 ((Register)(R0_RegisterEnumValue))
#define R1 ((Register)(R1_RegisterEnumValue))
#define R2 ((Register)(R2_RegisterEnumValue))
#define R3 ((Register)(R3_RegisterEnumValue))
#define R4 ((Register)(R4_RegisterEnumValue))
#define R5 ((Register)(R5_RegisterEnumValue))
#define R6 ((Register)(R6_RegisterEnumValue))
#define R7 ((Register)(R7_RegisterEnumValue))
#define R8 ((Register)(R8_RegisterEnumValue))
#define R9 ((Register)(R9_RegisterEnumValue))
#define R10 ((Register)(R10_RegisterEnumValue))
#define R11 ((Register)(R11_RegisterEnumValue))
#define R12 ((Register)(R12_RegisterEnumValue))
#define R13 ((Register)(R13_RegisterEnumValue))
#define R14 ((Register)(R14_RegisterEnumValue))
#define R15 ((Register)(R15_RegisterEnumValue))
#define R16 ((Register)(R16_RegisterEnumValue))
#define R17 ((Register)(R17_RegisterEnumValue))
#define R18 ((Register)(R18_RegisterEnumValue))
#define R19 ((Register)(R19_RegisterEnumValue))
#define R20 ((Register)(R20_RegisterEnumValue))
#define R21 ((Register)(R21_RegisterEnumValue))
#define R22 ((Register)(R22_RegisterEnumValue))
#define R23 ((Register)(R23_RegisterEnumValue))
#define R24 ((Register)(R24_RegisterEnumValue))
#define R25 ((Register)(R25_RegisterEnumValue))
#define R26 ((Register)(R26_RegisterEnumValue))
#define R27 ((Register)(R27_RegisterEnumValue))
#define R28 ((Register)(R28_RegisterEnumValue))
#define R29 ((Register)(R29_RegisterEnumValue))
#define R30 ((Register)(R30_RegisterEnumValue))
#define R31 ((Register)(R31_RegisterEnumValue))
#endif

// Use ConditionRegister as shortcut
class ConditionRegisterImpl;
typedef ConditionRegisterImpl* ConditionRegister;

inline ConditionRegister as_ConditionRegister(int encoding) {
  assert(encoding >= 0 && encoding < 8, "bad condition register encoding");
  return (ConditionRegister)(intptr_t)encoding;
}

// The implementation of condition register(s) for the PPC architecture
class ConditionRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 8
  };

  // construction.
  inline friend ConditionRegister as_ConditionRegister(int encoding);

  // accessors
  int encoding() const { assert(is_valid(), "invalid register"); return value(); }
  inline VMReg as_VMReg();

  // testers
  bool is_valid()       const { return  (0 <= value()        &&  value() < number_of_registers); }
  bool is_nonvolatile() const { return  (2 <= (value()&0x7F) && (value()&0x7F) <= 4 );  }

  const char* name() const;
};

// The (parts of the) condition register(s) of the PPC architecture
// sys/ioctl.h on AIX defines CR0-CR3, so I name these CCR.
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR0,   (0));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR1,   (1));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR2,   (2));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR3,   (3));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR4,   (4));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR5,   (5));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR6,   (6));
CONSTANT_REGISTER_DECLARATION(ConditionRegister, CCR7,   (7));

#ifndef DONT_USE_REGISTER_DEFINES

#define CCR0 ((ConditionRegister)(CCR0_ConditionRegisterEnumValue))
#define CCR1 ((ConditionRegister)(CCR1_ConditionRegisterEnumValue))
#define CCR2 ((ConditionRegister)(CCR2_ConditionRegisterEnumValue))
#define CCR3 ((ConditionRegister)(CCR3_ConditionRegisterEnumValue))
#define CCR4 ((ConditionRegister)(CCR4_ConditionRegisterEnumValue))
#define CCR5 ((ConditionRegister)(CCR5_ConditionRegisterEnumValue))
#define CCR6 ((ConditionRegister)(CCR6_ConditionRegisterEnumValue))
#define CCR7 ((ConditionRegister)(CCR7_ConditionRegisterEnumValue))

#endif // DONT_USE_REGISTER_DEFINES

// Forward declaration
// Use VectorSRegister as a shortcut.
class VectorSRegisterImpl;
typedef VectorSRegisterImpl* VectorSRegister;

// Use FloatRegister as shortcut
class FloatRegisterImpl;
typedef FloatRegisterImpl* FloatRegister;

inline FloatRegister as_FloatRegister(int encoding) {
  assert(encoding >= -1 && encoding < 32, "bad float register encoding");
  return (FloatRegister)(intptr_t)encoding;
}

// The implementation of float registers for the PPC architecture
class FloatRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 32
  };

  // construction
  inline friend FloatRegister as_FloatRegister(int encoding);

  // accessors
  int           encoding() const { assert(is_valid(), "invalid register"); return value(); }
  inline VMReg  as_VMReg();
  FloatRegister successor() const { return as_FloatRegister(encoding() + 1); }

  // testers
  bool is_valid() const { return (0 <= value() && value() < number_of_registers); }

  const char* name() const;

  // convert to VSR
  VectorSRegister to_vsr() const;
};

// The float registers of the PPC architecture
CONSTANT_REGISTER_DECLARATION(FloatRegister, fnoreg, (-1));

CONSTANT_REGISTER_DECLARATION(FloatRegister, F0,  ( 0));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F1,  ( 1));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F2,  ( 2));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F3,  ( 3));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F4,  ( 4));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F5,  ( 5));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F6,  ( 6));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F7,  ( 7));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F8,  ( 8));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F9,  ( 9));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F10, (10));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F11, (11));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F12, (12));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F13, (13));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F14, (14));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F15, (15));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F16, (16));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F17, (17));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F18, (18));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F19, (19));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F20, (20));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F21, (21));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F22, (22));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F23, (23));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F24, (24));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F25, (25));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F26, (26));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F27, (27));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F28, (28));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F29, (29));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F30, (30));
CONSTANT_REGISTER_DECLARATION(FloatRegister, F31, (31));

#ifndef DONT_USE_REGISTER_DEFINES
#define fnoreg ((FloatRegister)(fnoreg_FloatRegisterEnumValue))
#define F0     ((FloatRegister)(    F0_FloatRegisterEnumValue))
#define F1     ((FloatRegister)(    F1_FloatRegisterEnumValue))
#define F2     ((FloatRegister)(    F2_FloatRegisterEnumValue))
#define F3     ((FloatRegister)(    F3_FloatRegisterEnumValue))
#define F4     ((FloatRegister)(    F4_FloatRegisterEnumValue))
#define F5     ((FloatRegister)(    F5_FloatRegisterEnumValue))
#define F6     ((FloatRegister)(    F6_FloatRegisterEnumValue))
#define F7     ((FloatRegister)(    F7_FloatRegisterEnumValue))
#define F8     ((FloatRegister)(    F8_FloatRegisterEnumValue))
#define F9     ((FloatRegister)(    F9_FloatRegisterEnumValue))
#define F10    ((FloatRegister)(   F10_FloatRegisterEnumValue))
#define F11    ((FloatRegister)(   F11_FloatRegisterEnumValue))
#define F12    ((FloatRegister)(   F12_FloatRegisterEnumValue))
#define F13    ((FloatRegister)(   F13_FloatRegisterEnumValue))
#define F14    ((FloatRegister)(   F14_FloatRegisterEnumValue))
#define F15    ((FloatRegister)(   F15_FloatRegisterEnumValue))
#define F16    ((FloatRegister)(   F16_FloatRegisterEnumValue))
#define F17    ((FloatRegister)(   F17_FloatRegisterEnumValue))
#define F18    ((FloatRegister)(   F18_FloatRegisterEnumValue))
#define F19    ((FloatRegister)(   F19_FloatRegisterEnumValue))
#define F20    ((FloatRegister)(   F20_FloatRegisterEnumValue))
#define F21    ((FloatRegister)(   F21_FloatRegisterEnumValue))
#define F22    ((FloatRegister)(   F22_FloatRegisterEnumValue))
#define F23    ((FloatRegister)(   F23_FloatRegisterEnumValue))
#define F24    ((FloatRegister)(   F24_FloatRegisterEnumValue))
#define F25    ((FloatRegister)(   F25_FloatRegisterEnumValue))
#define F26    ((FloatRegister)(   F26_FloatRegisterEnumValue))
#define F27    ((FloatRegister)(   F27_FloatRegisterEnumValue))
#define F28    ((FloatRegister)(   F28_FloatRegisterEnumValue))
#define F29    ((FloatRegister)(   F29_FloatRegisterEnumValue))
#define F30    ((FloatRegister)(   F30_FloatRegisterEnumValue))
#define F31    ((FloatRegister)(   F31_FloatRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES

// Use SpecialRegister as shortcut
class SpecialRegisterImpl;
typedef SpecialRegisterImpl* SpecialRegister;

inline SpecialRegister as_SpecialRegister(int encoding) {
  return (SpecialRegister)(intptr_t)encoding;
}

// The implementation of special registers for the Power architecture (LR, CTR and friends)
class SpecialRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 6
  };

  // construction
  inline friend SpecialRegister as_SpecialRegister(int encoding);

  // accessors
  int             encoding()  const { assert(is_valid(), "invalid register"); return value(); }
  inline VMReg    as_VMReg();

  // testers
  bool is_valid()       const { return 0 <= value() && value() < number_of_registers; }

  const char* name() const;
};

// The special registers of the PPC architecture
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_XER,     (0));
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_LR,      (1));
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_CTR,     (2));
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_VRSAVE,  (3));
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_SPEFSCR, (4));
CONSTANT_REGISTER_DECLARATION(SpecialRegister, SR_PPR,     (5));

#ifndef DONT_USE_REGISTER_DEFINES
#define SR_XER     ((SpecialRegister)(SR_XER_SpecialRegisterEnumValue))
#define SR_LR      ((SpecialRegister)(SR_LR_SpecialRegisterEnumValue))
#define SR_CTR     ((SpecialRegister)(SR_CTR_SpecialRegisterEnumValue))
#define SR_VRSAVE  ((SpecialRegister)(SR_VRSAVE_SpecialRegisterEnumValue))
#define SR_SPEFSCR ((SpecialRegister)(SR_SPEFSCR_SpecialRegisterEnumValue))
#define SR_PPR     ((SpecialRegister)(SR_PPR_SpecialRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES


// Use VectorRegister as shortcut
class VectorRegisterImpl;
typedef VectorRegisterImpl* VectorRegister;

inline VectorRegister as_VectorRegister(int encoding) {
  return (VectorRegister)(intptr_t)encoding;
}

// The implementation of vector registers for the Power architecture
class VectorRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 32
  };

  // construction
  inline friend VectorRegister as_VectorRegister(int encoding);

  // accessors
  int            encoding()  const { assert(is_valid(), "invalid register"); return value(); }

  // testers
  bool is_valid()       const { return   0 <=  value()       &&  value() < number_of_registers; }

  const char* name() const;

  // convert to VSR
  VectorSRegister to_vsr() const;
};

// The Vector registers of the Power architecture

CONSTANT_REGISTER_DECLARATION(VectorRegister, vnoreg, (-1));

CONSTANT_REGISTER_DECLARATION(VectorRegister, VR0,  ( 0));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR1,  ( 1));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR2,  ( 2));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR3,  ( 3));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR4,  ( 4));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR5,  ( 5));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR6,  ( 6));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR7,  ( 7));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR8,  ( 8));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR9,  ( 9));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR10, (10));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR11, (11));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR12, (12));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR13, (13));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR14, (14));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR15, (15));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR16, (16));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR17, (17));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR18, (18));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR19, (19));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR20, (20));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR21, (21));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR22, (22));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR23, (23));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR24, (24));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR25, (25));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR26, (26));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR27, (27));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR28, (28));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR29, (29));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR30, (30));
CONSTANT_REGISTER_DECLARATION(VectorRegister, VR31, (31));

#ifndef DONT_USE_REGISTER_DEFINES
#define vnoreg ((VectorRegister)(vnoreg_VectorRegisterEnumValue))
#define VR0    ((VectorRegister)(   VR0_VectorRegisterEnumValue))
#define VR1    ((VectorRegister)(   VR1_VectorRegisterEnumValue))
#define VR2    ((VectorRegister)(   VR2_VectorRegisterEnumValue))
#define VR3    ((VectorRegister)(   VR3_VectorRegisterEnumValue))
#define VR4    ((VectorRegister)(   VR4_VectorRegisterEnumValue))
#define VR5    ((VectorRegister)(   VR5_VectorRegisterEnumValue))
#define VR6    ((VectorRegister)(   VR6_VectorRegisterEnumValue))
#define VR7    ((VectorRegister)(   VR7_VectorRegisterEnumValue))
#define VR8    ((VectorRegister)(   VR8_VectorRegisterEnumValue))
#define VR9    ((VectorRegister)(   VR9_VectorRegisterEnumValue))
#define VR10   ((VectorRegister)(  VR10_VectorRegisterEnumValue))
#define VR11   ((VectorRegister)(  VR11_VectorRegisterEnumValue))
#define VR12   ((VectorRegister)(  VR12_VectorRegisterEnumValue))
#define VR13   ((VectorRegister)(  VR13_VectorRegisterEnumValue))
#define VR14   ((VectorRegister)(  VR14_VectorRegisterEnumValue))
#define VR15   ((VectorRegister)(  VR15_VectorRegisterEnumValue))
#define VR16   ((VectorRegister)(  VR16_VectorRegisterEnumValue))
#define VR17   ((VectorRegister)(  VR17_VectorRegisterEnumValue))
#define VR18   ((VectorRegister)(  VR18_VectorRegisterEnumValue))
#define VR19   ((VectorRegister)(  VR19_VectorRegisterEnumValue))
#define VR20   ((VectorRegister)(  VR20_VectorRegisterEnumValue))
#define VR21   ((VectorRegister)(  VR21_VectorRegisterEnumValue))
#define VR22   ((VectorRegister)(  VR22_VectorRegisterEnumValue))
#define VR23   ((VectorRegister)(  VR23_VectorRegisterEnumValue))
#define VR24   ((VectorRegister)(  VR24_VectorRegisterEnumValue))
#define VR25   ((VectorRegister)(  VR25_VectorRegisterEnumValue))
#define VR26   ((VectorRegister)(  VR26_VectorRegisterEnumValue))
#define VR27   ((VectorRegister)(  VR27_VectorRegisterEnumValue))
#define VR28   ((VectorRegister)(  VR28_VectorRegisterEnumValue))
#define VR29   ((VectorRegister)(  VR29_VectorRegisterEnumValue))
#define VR30   ((VectorRegister)(  VR30_VectorRegisterEnumValue))
#define VR31   ((VectorRegister)(  VR31_VectorRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES


inline VectorSRegister as_VectorSRegister(int encoding) {
  return (VectorSRegister)(intptr_t)encoding;
}

// The implementation of Vector-Scalar (VSX) registers on POWER architecture.
class VectorSRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 64
  };

  // construction
  inline friend VectorSRegister as_VectorSRegister(int encoding);

  // accessors
  int encoding() const { assert(is_valid(), "invalid register"); return value(); }
  inline VMReg as_VMReg();

  // testers
  bool is_valid() const { return 0 <=  value() &&  value() < number_of_registers; }

  const char* name() const;

  // convert to VR
  VectorRegister to_vr() const;
};

// The Vector-Scalar (VSX) registers of the POWER architecture.

CONSTANT_REGISTER_DECLARATION(VectorSRegister, vsnoreg, (-1));

CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR0,  ( 0));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR1,  ( 1));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR2,  ( 2));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR3,  ( 3));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR4,  ( 4));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR5,  ( 5));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR6,  ( 6));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR7,  ( 7));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR8,  ( 8));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR9,  ( 9));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR10, (10));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR11, (11));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR12, (12));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR13, (13));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR14, (14));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR15, (15));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR16, (16));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR17, (17));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR18, (18));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR19, (19));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR20, (20));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR21, (21));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR22, (22));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR23, (23));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR24, (24));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR25, (25));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR26, (26));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR27, (27));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR28, (28));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR29, (29));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR30, (30));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR31, (31));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR32, (32));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR33, (33));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR34, (34));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR35, (35));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR36, (36));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR37, (37));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR38, (38));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR39, (39));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR40, (40));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR41, (41));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR42, (42));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR43, (43));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR44, (44));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR45, (45));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR46, (46));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR47, (47));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR48, (48));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR49, (49));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR50, (50));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR51, (51));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR52, (52));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR53, (53));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR54, (54));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR55, (55));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR56, (56));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR57, (57));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR58, (58));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR59, (59));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR60, (60));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR61, (61));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR62, (62));
CONSTANT_REGISTER_DECLARATION(VectorSRegister, VSR63, (63));

#ifndef DONT_USE_REGISTER_DEFINES
#define vsnoreg ((VectorSRegister)(vsnoreg_VectorSRegisterEnumValue))
#define VSR0    ((VectorSRegister)(   VSR0_VectorSRegisterEnumValue))
#define VSR1    ((VectorSRegister)(   VSR1_VectorSRegisterEnumValue))
#define VSR2    ((VectorSRegister)(   VSR2_VectorSRegisterEnumValue))
#define VSR3    ((VectorSRegister)(   VSR3_VectorSRegisterEnumValue))
#define VSR4    ((VectorSRegister)(   VSR4_VectorSRegisterEnumValue))
#define VSR5    ((VectorSRegister)(   VSR5_VectorSRegisterEnumValue))
#define VSR6    ((VectorSRegister)(   VSR6_VectorSRegisterEnumValue))
#define VSR7    ((VectorSRegister)(   VSR7_VectorSRegisterEnumValue))
#define VSR8    ((VectorSRegister)(   VSR8_VectorSRegisterEnumValue))
#define VSR9    ((VectorSRegister)(   VSR9_VectorSRegisterEnumValue))
#define VSR10   ((VectorSRegister)(  VSR10_VectorSRegisterEnumValue))
#define VSR11   ((VectorSRegister)(  VSR11_VectorSRegisterEnumValue))
#define VSR12   ((VectorSRegister)(  VSR12_VectorSRegisterEnumValue))
#define VSR13   ((VectorSRegister)(  VSR13_VectorSRegisterEnumValue))
#define VSR14   ((VectorSRegister)(  VSR14_VectorSRegisterEnumValue))
#define VSR15   ((VectorSRegister)(  VSR15_VectorSRegisterEnumValue))
#define VSR16   ((VectorSRegister)(  VSR16_VectorSRegisterEnumValue))
#define VSR17   ((VectorSRegister)(  VSR17_VectorSRegisterEnumValue))
#define VSR18   ((VectorSRegister)(  VSR18_VectorSRegisterEnumValue))
#define VSR19   ((VectorSRegister)(  VSR19_VectorSRegisterEnumValue))
#define VSR20   ((VectorSRegister)(  VSR20_VectorSRegisterEnumValue))
#define VSR21   ((VectorSRegister)(  VSR21_VectorSRegisterEnumValue))
#define VSR22   ((VectorSRegister)(  VSR22_VectorSRegisterEnumValue))
#define VSR23   ((VectorSRegister)(  VSR23_VectorSRegisterEnumValue))
#define VSR24   ((VectorSRegister)(  VSR24_VectorSRegisterEnumValue))
#define VSR25   ((VectorSRegister)(  VSR25_VectorSRegisterEnumValue))
#define VSR26   ((VectorSRegister)(  VSR26_VectorSRegisterEnumValue))
#define VSR27   ((VectorSRegister)(  VSR27_VectorSRegisterEnumValue))
#define VSR28   ((VectorSRegister)(  VSR28_VectorSRegisterEnumValue))
#define VSR29   ((VectorSRegister)(  VSR29_VectorSRegisterEnumValue))
#define VSR30   ((VectorSRegister)(  VSR30_VectorSRegisterEnumValue))
#define VSR31   ((VectorSRegister)(  VSR31_VectorSRegisterEnumValue))
#define VSR32   ((VectorSRegister)(  VSR32_VectorSRegisterEnumValue))
#define VSR33   ((VectorSRegister)(  VSR33_VectorSRegisterEnumValue))
#define VSR34   ((VectorSRegister)(  VSR34_VectorSRegisterEnumValue))
#define VSR35   ((VectorSRegister)(  VSR35_VectorSRegisterEnumValue))
#define VSR36   ((VectorSRegister)(  VSR36_VectorSRegisterEnumValue))
#define VSR37   ((VectorSRegister)(  VSR37_VectorSRegisterEnumValue))
#define VSR38   ((VectorSRegister)(  VSR38_VectorSRegisterEnumValue))
#define VSR39   ((VectorSRegister)(  VSR39_VectorSRegisterEnumValue))
#define VSR40   ((VectorSRegister)(  VSR40_VectorSRegisterEnumValue))
#define VSR41   ((VectorSRegister)(  VSR41_VectorSRegisterEnumValue))
#define VSR42   ((VectorSRegister)(  VSR42_VectorSRegisterEnumValue))
#define VSR43   ((VectorSRegister)(  VSR43_VectorSRegisterEnumValue))
#define VSR44   ((VectorSRegister)(  VSR44_VectorSRegisterEnumValue))
#define VSR45   ((VectorSRegister)(  VSR45_VectorSRegisterEnumValue))
#define VSR46   ((VectorSRegister)(  VSR46_VectorSRegisterEnumValue))
#define VSR47   ((VectorSRegister)(  VSR47_VectorSRegisterEnumValue))
#define VSR48   ((VectorSRegister)(  VSR48_VectorSRegisterEnumValue))
#define VSR49   ((VectorSRegister)(  VSR49_VectorSRegisterEnumValue))
#define VSR50   ((VectorSRegister)(  VSR50_VectorSRegisterEnumValue))
#define VSR51   ((VectorSRegister)(  VSR51_VectorSRegisterEnumValue))
#define VSR52   ((VectorSRegister)(  VSR52_VectorSRegisterEnumValue))
#define VSR53   ((VectorSRegister)(  VSR53_VectorSRegisterEnumValue))
#define VSR54   ((VectorSRegister)(  VSR54_VectorSRegisterEnumValue))
#define VSR55   ((VectorSRegister)(  VSR55_VectorSRegisterEnumValue))
#define VSR56   ((VectorSRegister)(  VSR56_VectorSRegisterEnumValue))
#define VSR57   ((VectorSRegister)(  VSR57_VectorSRegisterEnumValue))
#define VSR58   ((VectorSRegister)(  VSR58_VectorSRegisterEnumValue))
#define VSR59   ((VectorSRegister)(  VSR59_VectorSRegisterEnumValue))
#define VSR60   ((VectorSRegister)(  VSR60_VectorSRegisterEnumValue))
#define VSR61   ((VectorSRegister)(  VSR61_VectorSRegisterEnumValue))
#define VSR62   ((VectorSRegister)(  VSR62_VectorSRegisterEnumValue))
#define VSR63   ((VectorSRegister)(  VSR63_VectorSRegisterEnumValue))
#endif // DONT_USE_REGISTER_DEFINES

// Maximum number of incoming arguments that can be passed in i registers.
const int PPC_ARGS_IN_REGS_NUM = 8;


// Need to know the total number of registers of all sorts for SharedInfo.
// Define a class that exports it.
class ConcreteRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
    max_gpr = RegisterImpl::number_of_registers * 2,
    max_fpr = max_gpr + FloatRegisterImpl::number_of_registers * 2,
    max_vsr = max_fpr + VectorSRegisterImpl::number_of_registers,
    max_cnd = max_vsr + ConditionRegisterImpl::number_of_registers,
    max_spr = max_cnd + SpecialRegisterImpl::number_of_registers,
    // This number must be large enough to cover REG_COUNT (defined by c2) registers.
    // There is no requirement that any ordering here matches any ordering c2 gives
    // it's optoregs.
    number_of_registers = max_spr
  };
};

// Common register declarations used in assembler code.
REGISTER_DECLARATION(Register,      R0_SCRATCH, R0);  // volatile
REGISTER_DECLARATION(Register,      R1_SP,      R1);  // non-volatile
REGISTER_DECLARATION(Register,      R2_TOC,     R2);  // volatile
REGISTER_DECLARATION(Register,      R3_RET,     R3);  // volatile
REGISTER_DECLARATION(Register,      R3_ARG1,    R3);  // volatile
REGISTER_DECLARATION(Register,      R4_ARG2,    R4);  // volatile
REGISTER_DECLARATION(Register,      R5_ARG3,    R5);  // volatile
REGISTER_DECLARATION(Register,      R6_ARG4,    R6);  // volatile
REGISTER_DECLARATION(Register,      R7_ARG5,    R7);  // volatile
REGISTER_DECLARATION(Register,      R8_ARG6,    R8);  // volatile
REGISTER_DECLARATION(Register,      R9_ARG7,    R9);  // volatile
REGISTER_DECLARATION(Register,      R10_ARG8,   R10); // volatile
REGISTER_DECLARATION(FloatRegister, F0_SCRATCH, F0);  // volatile
REGISTER_DECLARATION(FloatRegister, F1_RET,     F1);  // volatile
REGISTER_DECLARATION(FloatRegister, F1_ARG1,    F1);  // volatile
REGISTER_DECLARATION(FloatRegister, F2_ARG2,    F2);  // volatile
REGISTER_DECLARATION(FloatRegister, F3_ARG3,    F3);  // volatile
REGISTER_DECLARATION(FloatRegister, F4_ARG4,    F4);  // volatile
REGISTER_DECLARATION(FloatRegister, F5_ARG5,    F5);  // volatile
REGISTER_DECLARATION(FloatRegister, F6_ARG6,    F6);  // volatile
REGISTER_DECLARATION(FloatRegister, F7_ARG7,    F7);  // volatile
REGISTER_DECLARATION(FloatRegister, F8_ARG8,    F8);  // volatile
REGISTER_DECLARATION(FloatRegister, F9_ARG9,    F9);  // volatile
REGISTER_DECLARATION(FloatRegister, F10_ARG10,  F10); // volatile
REGISTER_DECLARATION(FloatRegister, F11_ARG11,  F11); // volatile
REGISTER_DECLARATION(FloatRegister, F12_ARG12,  F12); // volatile
REGISTER_DECLARATION(FloatRegister, F13_ARG13,  F13); // volatile

#ifndef DONT_USE_REGISTER_DEFINES
#define R0_SCRATCH         AS_REGISTER(Register, R0)
#define R1_SP              AS_REGISTER(Register, R1)
#define R2_TOC             AS_REGISTER(Register, R2)
#define R3_RET             AS_REGISTER(Register, R3)
#define R3_ARG1            AS_REGISTER(Register, R3)
#define R4_ARG2            AS_REGISTER(Register, R4)
#define R5_ARG3            AS_REGISTER(Register, R5)
#define R6_ARG4            AS_REGISTER(Register, R6)
#define R7_ARG5            AS_REGISTER(Register, R7)
#define R8_ARG6            AS_REGISTER(Register, R8)
#define R9_ARG7            AS_REGISTER(Register, R9)
#define R10_ARG8           AS_REGISTER(Register, R10)
#define F0_SCRATCH         AS_REGISTER(FloatRegister, F0)
#define F1_RET             AS_REGISTER(FloatRegister, F1)
#define F1_ARG1            AS_REGISTER(FloatRegister, F1)
#define F2_ARG2            AS_REGISTER(FloatRegister, F2)
#define F3_ARG3            AS_REGISTER(FloatRegister, F3)
#define F4_ARG4            AS_REGISTER(FloatRegister, F4)
#define F5_ARG5            AS_REGISTER(FloatRegister, F5)
#define F6_ARG6            AS_REGISTER(FloatRegister, F6)
#define F7_ARG7            AS_REGISTER(FloatRegister, F7)
#define F8_ARG8            AS_REGISTER(FloatRegister, F8)
#define F9_ARG9            AS_REGISTER(FloatRegister, F9)
#define F10_ARG10          AS_REGISTER(FloatRegister, F10)
#define F11_ARG11          AS_REGISTER(FloatRegister, F11)
#define F12_ARG12          AS_REGISTER(FloatRegister, F12)
#define F13_ARG13          AS_REGISTER(FloatRegister, F13)
#endif

// Register declarations to be used in frame manager assembly code.
// Use only non-volatile registers in order to keep values across C-calls.
REGISTER_DECLARATION(Register, R14_bcp,        R14);
REGISTER_DECLARATION(Register, R15_esp,        R15);
REGISTER_DECLARATION(FloatRegister, F15_ftos,  F15);
REGISTER_DECLARATION(Register, R16_thread,     R16);      // address of current thread
REGISTER_DECLARATION(Register, R17_tos,        R17);      // address of Java tos (prepushed).
REGISTER_DECLARATION(Register, R18_locals,     R18);      // address of first param slot (receiver).
REGISTER_DECLARATION(Register, R19_method,     R19);      // address of current method
#ifndef DONT_USE_REGISTER_DEFINES
#define R14_bcp           AS_REGISTER(Register, R14)
#define R15_esp           AS_REGISTER(Register, R15)
#define F15_ftos          AS_REGISTER(FloatRegister, F15)
#define R16_thread        AS_REGISTER(Register, R16)
#define R17_tos           AS_REGISTER(Register, R17)
#define R18_locals        AS_REGISTER(Register, R18)
#define R19_method        AS_REGISTER(Register, R19)
#define R21_sender_SP     AS_REGISTER(Register, R21)
#define R23_method_handle AS_REGISTER(Register, R23)
#endif

// Temporary registers to be used within frame manager. We can use
// the non-volatiles because the call stub has saved them.
// Use only non-volatile registers in order to keep values across C-calls.
REGISTER_DECLARATION(Register, R21_tmp1, R21);
REGISTER_DECLARATION(Register, R22_tmp2, R22);
REGISTER_DECLARATION(Register, R23_tmp3, R23);
REGISTER_DECLARATION(Register, R24_tmp4, R24);
REGISTER_DECLARATION(Register, R25_tmp5, R25);
REGISTER_DECLARATION(Register, R26_tmp6, R26);
REGISTER_DECLARATION(Register, R27_tmp7, R27);
REGISTER_DECLARATION(Register, R28_tmp8, R28);
REGISTER_DECLARATION(Register, R29_tmp9, R29);
REGISTER_DECLARATION(Register, R24_dispatch_addr,     R24);
REGISTER_DECLARATION(Register, R25_templateTableBase, R25);
REGISTER_DECLARATION(Register, R26_monitor,           R26);
REGISTER_DECLARATION(Register, R27_constPoolCache,    R27);
REGISTER_DECLARATION(Register, R28_mdx,               R28);

REGISTER_DECLARATION(Register, R19_inline_cache_reg, R19);
REGISTER_DECLARATION(Register, R29_TOC, R29);

#ifndef DONT_USE_REGISTER_DEFINES
#define R21_tmp1         AS_REGISTER(Register, R21)
#define R22_tmp2         AS_REGISTER(Register, R22)
#define R23_tmp3         AS_REGISTER(Register, R23)
#define R24_tmp4         AS_REGISTER(Register, R24)
#define R25_tmp5         AS_REGISTER(Register, R25)
#define R26_tmp6         AS_REGISTER(Register, R26)
#define R27_tmp7         AS_REGISTER(Register, R27)
#define R28_tmp8         AS_REGISTER(Register, R28)
#define R29_tmp9         AS_REGISTER(Register, R29)
//    Lmonitors  : monitor pointer
//    LcpoolCache: constant pool cache
//    mdx: method data index
#define R24_dispatch_addr     AS_REGISTER(Register, R24)
#define R25_templateTableBase AS_REGISTER(Register, R25)
#define R26_monitor           AS_REGISTER(Register, R26)
#define R27_constPoolCache    AS_REGISTER(Register, R27)
#define R28_mdx               AS_REGISTER(Register, R28)

#define R19_inline_cache_reg AS_REGISTER(Register, R19)
#define R29_TOC AS_REGISTER(Register, R29)
#endif

// Scratch registers are volatile.
REGISTER_DECLARATION(Register, R11_scratch1, R11);
REGISTER_DECLARATION(Register, R12_scratch2, R12);
#ifndef DONT_USE_REGISTER_DEFINES
#define R11_scratch1   AS_REGISTER(Register, R11)
#define R12_scratch2   AS_REGISTER(Register, R12)
#endif

#endif // CPU_PPC_REGISTER_PPC_HPP
