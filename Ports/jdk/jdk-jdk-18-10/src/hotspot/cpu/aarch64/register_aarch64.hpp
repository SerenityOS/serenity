/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_REGISTER_AARCH64_HPP
#define CPU_AARCH64_REGISTER_AARCH64_HPP

#include "asm/register.hpp"
#include "utilities/powerOfTwo.hpp"

class VMRegImpl;
typedef VMRegImpl* VMReg;

// Use Register as shortcut
class RegisterImpl;
typedef RegisterImpl* Register;

inline const Register as_Register(int encoding) {
  return (Register)(intptr_t) encoding;
}

class RegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers         =   32,
    number_of_byte_registers      = 32,
    number_of_registers_for_jvmci = 34,  // Including SP and ZR.
    max_slots_per_register = 2
  };

  // derived registers, offsets, and addresses
  Register successor() const                          { return as_Register(encoding() + 1); }

  // construction
  inline friend const Register as_Register(int encoding);

  VMReg as_VMReg();

  // accessors
  int   encoding() const                         { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  bool  is_valid() const                         { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  bool  has_byte_register() const                { return 0 <= (intptr_t)this && (intptr_t)this < number_of_byte_registers; }
  const char* name() const;
  int   encoding_nocheck() const                 { return (intptr_t)this; }
};

// The integer registers of the aarch64 architecture

CONSTANT_REGISTER_DECLARATION(Register, noreg, (-1));


CONSTANT_REGISTER_DECLARATION(Register, r0,    (0));
CONSTANT_REGISTER_DECLARATION(Register, r1,    (1));
CONSTANT_REGISTER_DECLARATION(Register, r2,    (2));
CONSTANT_REGISTER_DECLARATION(Register, r3,    (3));
CONSTANT_REGISTER_DECLARATION(Register, r4,    (4));
CONSTANT_REGISTER_DECLARATION(Register, r5,    (5));
CONSTANT_REGISTER_DECLARATION(Register, r6,    (6));
CONSTANT_REGISTER_DECLARATION(Register, r7,    (7));
CONSTANT_REGISTER_DECLARATION(Register, r8,    (8));
CONSTANT_REGISTER_DECLARATION(Register, r9,    (9));
CONSTANT_REGISTER_DECLARATION(Register, r10,  (10));
CONSTANT_REGISTER_DECLARATION(Register, r11,  (11));
CONSTANT_REGISTER_DECLARATION(Register, r12,  (12));
CONSTANT_REGISTER_DECLARATION(Register, r13,  (13));
CONSTANT_REGISTER_DECLARATION(Register, r14,  (14));
CONSTANT_REGISTER_DECLARATION(Register, r15,  (15));
CONSTANT_REGISTER_DECLARATION(Register, r16,  (16));
CONSTANT_REGISTER_DECLARATION(Register, r17,  (17));

// In the ABI for Windows+AArch64 the register r18 is used to store the pointer
// to the current thread's TEB (where TLS variables are stored). We could
// carefully save and restore r18 at key places, however Win32 Structured
// Exception Handling (SEH) is using TLS to unwind the stack. If r18 is used
// for any other purpose at the time of an exception happening, SEH would not
// be able to unwind the stack properly and most likely crash.
//
// It's easier to avoid allocating r18 altogether.
//
// See https://docs.microsoft.com/en-us/cpp/build/arm64-windows-abi-conventions?view=vs-2019#integer-registers
CONSTANT_REGISTER_DECLARATION(Register, r18_tls,  (18));
CONSTANT_REGISTER_DECLARATION(Register, r19,  (19));
CONSTANT_REGISTER_DECLARATION(Register, r20,  (20));
CONSTANT_REGISTER_DECLARATION(Register, r21,  (21));
CONSTANT_REGISTER_DECLARATION(Register, r22,  (22));
CONSTANT_REGISTER_DECLARATION(Register, r23,  (23));
CONSTANT_REGISTER_DECLARATION(Register, r24,  (24));
CONSTANT_REGISTER_DECLARATION(Register, r25,  (25));
CONSTANT_REGISTER_DECLARATION(Register, r26,  (26));
CONSTANT_REGISTER_DECLARATION(Register, r27,  (27));
CONSTANT_REGISTER_DECLARATION(Register, r28,  (28));
CONSTANT_REGISTER_DECLARATION(Register, r29,  (29));
CONSTANT_REGISTER_DECLARATION(Register, r30,  (30));


// r31 is not a general purpose register, but represents either the
// stack pointer or the zero/discard register depending on the
// instruction.
CONSTANT_REGISTER_DECLARATION(Register, r31_sp, (31));
CONSTANT_REGISTER_DECLARATION(Register, zr,  (32));
CONSTANT_REGISTER_DECLARATION(Register, sp,  (33));

// Used as a filler in instructions where a register field is unused.
const Register dummy_reg = r31_sp;

// Use FloatRegister as shortcut
class FloatRegisterImpl;
typedef FloatRegisterImpl* FloatRegister;

inline FloatRegister as_FloatRegister(int encoding) {
  return (FloatRegister)(intptr_t) encoding;
}

// The implementation of floating point registers for the architecture
class FloatRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 32,
    max_slots_per_register = 8,
    save_slots_per_register = 2,
    slots_per_neon_register = 4,
    extra_save_slots_per_neon_register = slots_per_neon_register - save_slots_per_register
  };

  // construction
  inline friend FloatRegister as_FloatRegister(int encoding);

  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  FloatRegister successor() const                          { return as_FloatRegister((encoding() + 1) % 32); }

  // accessors
  int   encoding() const                          { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  int   encoding_nocheck() const                         { return (intptr_t)this; }
  bool  is_valid() const                          { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  const char* name() const;

};

// The float registers of the AARCH64 architecture

CONSTANT_REGISTER_DECLARATION(FloatRegister, fnoreg , (-1));

CONSTANT_REGISTER_DECLARATION(FloatRegister, v0     , ( 0));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v1     , ( 1));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v2     , ( 2));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v3     , ( 3));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v4     , ( 4));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v5     , ( 5));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v6     , ( 6));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v7     , ( 7));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v8     , ( 8));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v9     , ( 9));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v10    , (10));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v11    , (11));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v12    , (12));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v13    , (13));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v14    , (14));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v15    , (15));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v16    , (16));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v17    , (17));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v18    , (18));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v19    , (19));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v20    , (20));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v21    , (21));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v22    , (22));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v23    , (23));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v24    , (24));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v25    , (25));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v26    , (26));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v27    , (27));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v28    , (28));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v29    , (29));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v30    , (30));
CONSTANT_REGISTER_DECLARATION(FloatRegister, v31    , (31));

// SVE vector registers, shared with the SIMD&FP v0-v31. Vn maps to Zn[127:0].
CONSTANT_REGISTER_DECLARATION(FloatRegister, z0     , ( 0));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z1     , ( 1));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z2     , ( 2));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z3     , ( 3));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z4     , ( 4));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z5     , ( 5));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z6     , ( 6));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z7     , ( 7));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z8     , ( 8));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z9     , ( 9));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z10    , (10));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z11    , (11));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z12    , (12));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z13    , (13));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z14    , (14));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z15    , (15));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z16    , (16));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z17    , (17));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z18    , (18));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z19    , (19));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z20    , (20));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z21    , (21));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z22    , (22));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z23    , (23));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z24    , (24));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z25    , (25));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z26    , (26));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z27    , (27));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z28    , (28));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z29    , (29));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z30    , (30));
CONSTANT_REGISTER_DECLARATION(FloatRegister, z31    , (31));


class PRegisterImpl;
typedef PRegisterImpl* PRegister;
inline PRegister as_PRegister(int encoding) {
  return (PRegister)(intptr_t)encoding;
}

// The implementation of predicate registers for the architecture
class PRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 16,
    number_of_governing_registers = 8,
    max_slots_per_register = 1
  };

  // construction
  inline friend PRegister as_PRegister(int encoding);

  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  PRegister successor() const     { return as_PRegister(encoding() + 1); }

  // accessors
  int   encoding() const          { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  int   encoding_nocheck() const  { return (intptr_t)this; }
  bool  is_valid() const          { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  bool  is_governing() const      { return 0 <= (intptr_t)this && (intptr_t)this < number_of_governing_registers; }
  const char* name() const;
};

// The predicate registers of SVE.
CONSTANT_REGISTER_DECLARATION(PRegister, p0,  ( 0));
CONSTANT_REGISTER_DECLARATION(PRegister, p1,  ( 1));
CONSTANT_REGISTER_DECLARATION(PRegister, p2,  ( 2));
CONSTANT_REGISTER_DECLARATION(PRegister, p3,  ( 3));
CONSTANT_REGISTER_DECLARATION(PRegister, p4,  ( 4));
CONSTANT_REGISTER_DECLARATION(PRegister, p5,  ( 5));
CONSTANT_REGISTER_DECLARATION(PRegister, p6,  ( 6));
CONSTANT_REGISTER_DECLARATION(PRegister, p7,  ( 7));
CONSTANT_REGISTER_DECLARATION(PRegister, p8,  ( 8));
CONSTANT_REGISTER_DECLARATION(PRegister, p9,  ( 9));
CONSTANT_REGISTER_DECLARATION(PRegister, p10, (10));
CONSTANT_REGISTER_DECLARATION(PRegister, p11, (11));
CONSTANT_REGISTER_DECLARATION(PRegister, p12, (12));
CONSTANT_REGISTER_DECLARATION(PRegister, p13, (13));
CONSTANT_REGISTER_DECLARATION(PRegister, p14, (14));
CONSTANT_REGISTER_DECLARATION(PRegister, p15, (15));

// Need to know the total number of registers of all sorts for SharedInfo.
// Define a class that exports it.
class ConcreteRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
  // A big enough number for C2: all the registers plus flags
  // This number must be large enough to cover REG_COUNT (defined by c2) registers.
  // There is no requirement that any ordering here matches any ordering c2 gives
  // it's optoregs.

    number_of_registers = (RegisterImpl::max_slots_per_register * RegisterImpl::number_of_registers +
                           FloatRegisterImpl::max_slots_per_register * FloatRegisterImpl::number_of_registers +
                           PRegisterImpl::max_slots_per_register * PRegisterImpl::number_of_registers +
                           1) // flags
  };

  // added to make it compile
  static const int max_gpr;
  static const int max_fpr;
  static const int max_pr;
};

template <class RegImpl = Register> class RegSetIterator;

// A set of registers
template <class RegImpl>
class AbstractRegSet {
  uint32_t _bitset;

  AbstractRegSet(uint32_t bitset) : _bitset(bitset) { }

public:

  AbstractRegSet() : _bitset(0) { }

  AbstractRegSet(RegImpl r1) : _bitset(1 << r1->encoding()) { }

  AbstractRegSet operator+(const AbstractRegSet aSet) const {
    AbstractRegSet result(_bitset | aSet._bitset);
    return result;
  }

  AbstractRegSet operator-(const AbstractRegSet aSet) const {
    AbstractRegSet result(_bitset & ~aSet._bitset);
    return result;
  }

  AbstractRegSet &operator+=(const AbstractRegSet aSet) {
    *this = *this + aSet;
    return *this;
  }

  AbstractRegSet &operator-=(const AbstractRegSet aSet) {
    *this = *this - aSet;
    return *this;
  }

  static AbstractRegSet of(RegImpl r1) {
    return AbstractRegSet(r1);
  }

  static AbstractRegSet of(RegImpl r1, RegImpl r2) {
    return of(r1) + r2;
  }

  static AbstractRegSet of(RegImpl r1, RegImpl r2, RegImpl r3) {
    return of(r1, r2) + r3;
  }

  static AbstractRegSet of(RegImpl r1, RegImpl r2, RegImpl r3, RegImpl r4) {
    return of(r1, r2, r3) + r4;
  }

  static AbstractRegSet range(RegImpl start, RegImpl end) {
    uint32_t bits = ~0;
    bits <<= start->encoding();
    bits <<= 31 - end->encoding();
    bits >>= 31 - end->encoding();

    return AbstractRegSet(bits);
  }

  uint32_t bits() const { return _bitset; }

private:

  RegImpl first();

public:

  friend class RegSetIterator<RegImpl>;

  RegSetIterator<RegImpl> begin();
};

typedef AbstractRegSet<Register> RegSet;
typedef AbstractRegSet<FloatRegister> FloatRegSet;

template <class RegImpl>
class RegSetIterator {
  AbstractRegSet<RegImpl> _regs;

public:
  RegSetIterator(AbstractRegSet<RegImpl> x): _regs(x) {}
  RegSetIterator(const RegSetIterator& mit) : _regs(mit._regs) {}

  RegSetIterator& operator++() {
    RegImpl r = _regs.first();
    if (r->is_valid())
      _regs -= r;
    return *this;
  }

  bool operator==(const RegSetIterator& rhs) const {
    return _regs.bits() == rhs._regs.bits();
  }
  bool operator!=(const RegSetIterator& rhs) const {
    return ! (rhs == *this);
  }

  RegImpl operator*() {
    return _regs.first();
  }
};

template <class RegImpl>
inline RegSetIterator<RegImpl> AbstractRegSet<RegImpl>::begin() {
  return RegSetIterator<RegImpl>(*this);
}

template <>
inline Register AbstractRegSet<Register>::first() {
  uint32_t first = _bitset & -_bitset;
  return first ? as_Register(exact_log2(first)) : noreg;
}

template <>
inline FloatRegister AbstractRegSet<FloatRegister>::first() {
  uint32_t first = _bitset & -_bitset;
  return first ? as_FloatRegister(exact_log2(first)) : fnoreg;
}

inline Register as_Register(FloatRegister reg) {
  return as_Register(reg->encoding());
}

#endif // CPU_AARCH64_REGISTER_AARCH64_HPP
