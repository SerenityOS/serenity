/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2021 SAP SE. All rights reserved.
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

#ifndef CPU_S390_ASSEMBLER_S390_HPP
#define CPU_S390_ASSEMBLER_S390_HPP

#undef  LUCY_DBG

// Immediate is an abstraction to represent the various immediate
// operands which exist on z/Architecture. Neither this class nor
// instances hereof have an own state. It consists of methods only.
class Immediate {

 public:
    static bool is_simm(int64_t x, unsigned int nbits) {
      // nbits < 2   --> false
      // nbits >= 64 --> true
      assert(2 <= nbits && nbits < 64, "Don't call, use statically known result.");
      const int64_t min      = -(1L << (nbits-1));
      const int64_t maxplus1 =  (1L << (nbits-1));
      return min <= x && x < maxplus1;
    }
    static bool is_simm32(int64_t x) {
      return is_simm(x, 32);
    }
    static bool is_simm20(int64_t x) {
      return is_simm(x, 20);
    }
    static bool is_simm16(int64_t x) {
      return is_simm(x, 16);
    }
    static bool is_simm8(int64_t x) {
      return is_simm(x,  8);
    }

    // Test if x is within signed immediate range for nbits.
    static bool is_uimm(int64_t x, unsigned int nbits) {
      // nbits == 0  --> false
      // nbits >= 64 --> true
      assert(1 <= nbits && nbits < 64, "don't call, use statically known result");
      const uint64_t xu       = (unsigned long)x;
      const uint64_t maxplus1 = 1UL << nbits;
      return xu < maxplus1; // Unsigned comparison. Negative inputs appear to be very large.
    }
    static bool is_uimm32(int64_t x) {
      return is_uimm(x, 32);
    }
    static bool is_uimm16(int64_t x) {
      return is_uimm(x, 16);
    }
    static bool is_uimm12(int64_t x) {
      return is_uimm(x, 12);
    }
    static bool is_uimm8(int64_t x) {
      return is_uimm(x,  8);
    }
};

// Displacement is an abstraction to represent the various
// displacements which exist with addresses on z/ArchiTecture.
// Neither this class nor instances hereof have an own state. It
// consists of methods only.
class Displacement {

 public: // These tests are used outside the (Macro)Assembler world, e.g. in ad-file.

  static bool is_longDisp(int64_t x) {  // Fits in a 20-bit displacement field.
    return Immediate::is_simm20(x);
  }
  static bool is_shortDisp(int64_t x) { // Fits in a 12-bit displacement field.
    return Immediate::is_uimm12(x);
  }
  static bool is_validDisp(int64_t x) { // Is a valid displacement, regardless of length constraints.
    return is_longDisp(x);
  }
};

// RelAddr is an abstraction to represent relative addresses in the
// form they are used on z/Architecture for instructions which access
// their operand with pc-relative addresses. Neither this class nor
// instances hereof have an own state. It consists of methods only.
class RelAddr {

 private: // No public use at all. Solely for (Macro)Assembler.

  static bool is_in_range_of_RelAddr(address target, address pc, bool shortForm) {
    // Guard against illegal branch targets, e.g. -1. Occurrences in
    // CompiledStaticCall and ad-file. Do not assert (it's a test
    // function!). Just return false in case of illegal operands.
    if ((((uint64_t)target) & 0x0001L) != 0) return false;
    if ((((uint64_t)pc)     & 0x0001L) != 0) return false;

    if (shortForm) {
      return Immediate::is_simm((int64_t)(target-pc), 17); // Relative short addresses can reach +/- 2**16 bytes.
    } else {
      return Immediate::is_simm((int64_t)(target-pc), 33); // Relative long addresses can reach +/- 2**32 bytes.
    }
  }

  static bool is_in_range_of_RelAddr16(address target, address pc) {
    return is_in_range_of_RelAddr(target, pc, true);
  }
  static bool is_in_range_of_RelAddr16(ptrdiff_t distance) {
    return is_in_range_of_RelAddr((address)distance, 0, true);
  }

  static bool is_in_range_of_RelAddr32(address target, address pc) {
    return is_in_range_of_RelAddr(target, pc, false);
  }
  static bool is_in_range_of_RelAddr32(ptrdiff_t distance) {
    return is_in_range_of_RelAddr((address)distance, 0, false);
  }

  static int pcrel_off(address target, address pc, bool shortForm) {
    assert(((uint64_t)target & 0x0001L) == 0, "target of a relative address must be aligned");
    assert(((uint64_t)pc     & 0x0001L) == 0, "origin of a relative address must be aligned");

    if ((target == NULL) || (target == pc)) {
      return 0;  // Yet unknown branch destination.
    } else {
      guarantee(is_in_range_of_RelAddr(target, pc, shortForm), "target not within reach");
      return (int)((target - pc)>>1);
    }
  }

  static int pcrel_off16(address target, address pc) {
    return pcrel_off(target, pc, true);
  }
  static int pcrel_off16(ptrdiff_t distance) {
    return pcrel_off((address)distance, 0, true);
  }

  static int pcrel_off32(address target, address pc) {
    return pcrel_off(target, pc, false);
  }
  static int pcrel_off32(ptrdiff_t distance) {
    return pcrel_off((address)distance, 0, false);
  }

  static ptrdiff_t inv_pcrel_off16(int offset) {
    return ((ptrdiff_t)offset)<<1;
  }

  static ptrdiff_t inv_pcrel_off32(int offset) {
    return ((ptrdiff_t)offset)<<1;
  }

  friend class Assembler;
  friend class MacroAssembler;
  friend class NativeGeneralJump;
};

// Address is an abstraction used to represent a memory location
// as passed to Z assembler instructions.
//
// Note: A register location is represented via a Register, not
// via an address for efficiency & simplicity reasons.
class Address {
 private:
  Register _base;    // Base register.
  Register _index;   // Index register
  intptr_t _disp;    // Constant displacement.

 public:
  Address() :
    _base(noreg),
    _index(noreg),
    _disp(0) {}

  Address(Register base, Register index, intptr_t disp = 0) :
    _base(base),
    _index(index),
    _disp(disp) {}

  Address(Register base, intptr_t disp = 0) :
    _base(base),
    _index(noreg),
    _disp(disp) {}

  Address(Register base, RegisterOrConstant roc, intptr_t disp = 0) :
    _base(base),
    _index(noreg),
    _disp(disp) {
    if (roc.is_constant()) _disp += roc.as_constant(); else _index = roc.as_register();
  }

  Address(Register base, ByteSize disp) :
    Address(base, in_bytes(disp)) {}

  Address(Register base, Register index, ByteSize disp) :
    Address(base, index, in_bytes(disp)) {}

  // Aborts if disp is a register and base and index are set already.
  Address plus_disp(RegisterOrConstant disp) const {
    Address a = (*this);
    a._disp += disp.constant_or_zero();
    if (disp.is_register()) {
      if (a._index == noreg) {
        a._index = disp.as_register();
      } else {
        guarantee(_base == noreg, "can not encode"); a._base = disp.as_register();
      }
    }
    return a;
  }

  // A call to this is generated by adlc for replacement variable $xxx$$Address.
  static Address make_raw(int base, int index, int scale, int disp, relocInfo::relocType disp_reloc);

  bool is_same_address(Address a) const {
    return _base == a._base && _index == a._index && _disp == a._disp;
  }

  // testers
  bool has_base()  const { return _base  != noreg; }
  bool has_index() const { return _index != noreg; }
  bool has_disp()  const { return true; } // There is no "invalid" value.

  bool is_disp12() const { return Immediate::is_uimm12(disp()); }
  bool is_disp20() const { return Immediate::is_simm20(disp()); }
  bool is_RSform()  { return has_base() && !has_index() && is_disp12(); }
  bool is_RSYform() { return has_base() && !has_index() && is_disp20(); }
  bool is_RXform()  { return has_base() &&  has_index() && is_disp12(); }
  bool is_RXYform() { return has_base() &&  has_index() && is_disp20(); }

  bool uses(Register r) { return _base == r || _index == r; };

  // accessors
  Register base()      const { return _base; }
  Register baseOrR0()  const { assert(_base  != Z_R0, ""); return _base  == noreg ? Z_R0 : _base; }
  Register index()     const { return _index; }
  Register indexOrR0() const { assert(_index != Z_R0, ""); return _index == noreg ? Z_R0 : _index; }
  intptr_t disp() const { return _disp; }
  // Specific version for short displacement instructions.
  int      disp12() const {
    assert(is_disp12(), "displacement out of range for uimm12");
    return _disp;
  }
  // Specific version for long displacement instructions.
  int      disp20() const {
    assert(is_disp20(), "displacement out of range for simm20");
    return _disp;
  }
  intptr_t value() const { return _disp; }

  friend class Assembler;
};

class AddressLiteral {
 private:
  address          _address;
  RelocationHolder _rspec;

  RelocationHolder rspec_from_rtype(relocInfo::relocType rtype, address addr) {
    switch (rtype) {
    case relocInfo::external_word_type:
      return external_word_Relocation::spec(addr);
    case relocInfo::internal_word_type:
      return internal_word_Relocation::spec(addr);
    case relocInfo::opt_virtual_call_type:
      return opt_virtual_call_Relocation::spec();
    case relocInfo::static_call_type:
      return static_call_Relocation::spec();
    case relocInfo::runtime_call_w_cp_type:
      return runtime_call_w_cp_Relocation::spec();
    case relocInfo::none:
      return RelocationHolder();
    default:
      ShouldNotReachHere();
      return RelocationHolder();
    }
  }

 protected:
  // creation
  AddressLiteral() : _address(NULL), _rspec(NULL) {}

 public:
  AddressLiteral(address addr, RelocationHolder const& rspec)
    : _address(addr),
      _rspec(rspec) {}

  // Some constructors to avoid casting at the call site.
  AddressLiteral(jobject obj, RelocationHolder const& rspec)
    : _address((address) obj),
      _rspec(rspec) {}

  AddressLiteral(intptr_t value, RelocationHolder const& rspec)
    : _address((address) value),
      _rspec(rspec) {}

  AddressLiteral(address addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
    _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  // Some constructors to avoid casting at the call site.
  AddressLiteral(address* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
    _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(bool* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(const bool* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(signed char* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(int* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(intptr_t addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(intptr_t* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(float* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  AddressLiteral(double* addr, relocInfo::relocType rtype = relocInfo::none)
    : _address((address) addr),
      _rspec(rspec_from_rtype(rtype, (address) addr)) {}

  intptr_t value() const { return (intptr_t) _address; }

  const relocInfo::relocType rtype() const { return _rspec.type(); }
  const RelocationHolder&    rspec() const { return _rspec; }

  RelocationHolder rspec(int offset) const {
    return offset == 0 ? _rspec : _rspec.plus(offset);
  }
};

// Convenience classes
class ExternalAddress: public AddressLiteral {
 private:
  static relocInfo::relocType reloc_for_target(address target) {
    // Sometimes ExternalAddress is used for values which aren't
    // exactly addresses, like the card table base.
    // External_word_type can't be used for values in the first page
    // so just skip the reloc in that case.
    return external_word_Relocation::can_be_relocated(target) ? relocInfo::external_word_type : relocInfo::none;
  }

 public:
  ExternalAddress(address target) : AddressLiteral(target, reloc_for_target(          target)) {}
};

// Argument is an abstraction used to represent an outgoing actual
// argument or an incoming formal parameter, whether it resides in
// memory or in a register, in a manner consistent with the
// z/Architecture Application Binary Interface, or ABI. This is often
// referred to as the native or C calling convention.
class Argument {
 private:
  int _number;
  bool _is_in;

 public:
  enum {
    // Only 5 registers may contain integer parameters.
    n_register_parameters = 5,
    // Can have up to 4 floating registers.
    n_float_register_parameters = 4
  };

  // creation
  Argument(int number, bool is_in) : _number(number), _is_in(is_in) {}
  Argument(int number) : _number(number) {}

  int number() const { return _number; }

  Argument successor() const { return Argument(number() + 1); }

  // Locating register-based arguments:
  bool is_register() const { return _number < n_register_parameters; }

  // Locating Floating Point register-based arguments:
  bool is_float_register() const { return _number < n_float_register_parameters; }

  FloatRegister as_float_register() const {
    assert(is_float_register(), "must be a register argument");
    return as_FloatRegister((number() *2) + 1);
  }

  FloatRegister as_double_register() const {
    assert(is_float_register(), "must be a register argument");
    return as_FloatRegister((number() *2));
  }

  Register as_register() const {
    assert(is_register(), "must be a register argument");
    return as_Register(number() + Z_ARG1->encoding());
  }

  // debugging
  const char* name() const;

  friend class Assembler;
};


// The z/Architecture Assembler: Pure assembler doing NO optimizations
// on the instruction level; i.e., what you write is what you get. The
// Assembler is generating code into a CodeBuffer.
class Assembler : public AbstractAssembler {
 protected:

  friend class AbstractAssembler;
  friend class AddressLiteral;

  // Code patchers need various routines like inv_wdisp().
  friend class NativeInstruction;
#ifndef COMPILER2
  friend class NativeGeneralJump;
#endif
  friend class Relocation;

 public:

// Addressing

// address calculation
#define LA_ZOPC     (unsigned  int)(0x41  << 24)
#define LAY_ZOPC    (unsigned long)(0xe3L << 40 | 0x71L)
#define LARL_ZOPC   (unsigned long)(0xc0L << 40 | 0x00L << 32)


// Data Transfer

// register to register transfer
#define LR_ZOPC     (unsigned  int)(24 << 8)
#define LBR_ZOPC    (unsigned  int)(0xb926 << 16)
#define LHR_ZOPC    (unsigned  int)(0xb927 << 16)
#define LGBR_ZOPC   (unsigned  int)(0xb906 << 16)
#define LGHR_ZOPC   (unsigned  int)(0xb907 << 16)
#define LGFR_ZOPC   (unsigned  int)(0xb914 << 16)
#define LGR_ZOPC    (unsigned  int)(0xb904 << 16)

#define LLHR_ZOPC   (unsigned  int)(0xb995 << 16)
#define LLGCR_ZOPC  (unsigned  int)(0xb984 << 16)
#define LLGHR_ZOPC  (unsigned  int)(0xb985 << 16)
#define LLGTR_ZOPC  (unsigned  int)(185 << 24 | 23 << 16)
#define LLGFR_ZOPC  (unsigned  int)(185 << 24 | 22 << 16)

#define LTR_ZOPC    (unsigned  int)(18 << 8)
#define LTGFR_ZOPC  (unsigned  int)(185 << 24 | 18 << 16)
#define LTGR_ZOPC   (unsigned  int)(185 << 24 | 2 << 16)

#define LER_ZOPC    (unsigned  int)(56 << 8)
#define LEDBR_ZOPC  (unsigned  int)(179 << 24 | 68 << 16)
#define LEXBR_ZOPC  (unsigned  int)(179 << 24 | 70 << 16)
#define LDEBR_ZOPC  (unsigned  int)(179 << 24 | 4 << 16)
#define LDR_ZOPC    (unsigned  int)(40 << 8)
#define LDXBR_ZOPC  (unsigned  int)(179 << 24 | 69 << 16)
#define LXEBR_ZOPC  (unsigned  int)(179 << 24 | 6 << 16)
#define LXDBR_ZOPC  (unsigned  int)(179 << 24 | 5 << 16)
#define LXR_ZOPC    (unsigned  int)(179 << 24 | 101 << 16)
#define LTEBR_ZOPC  (unsigned  int)(179 << 24 | 2 << 16)
#define LTDBR_ZOPC  (unsigned  int)(179 << 24 | 18 << 16)
#define LTXBR_ZOPC  (unsigned  int)(179 << 24 | 66 << 16)

#define LRVR_ZOPC   (unsigned  int)(0xb91f << 16)
#define LRVGR_ZOPC  (unsigned  int)(0xb90f << 16)

#define LDGR_ZOPC   (unsigned  int)(0xb3c1 << 16)                // z10
#define LGDR_ZOPC   (unsigned  int)(0xb3cd << 16)                // z10

#define LOCR_ZOPC   (unsigned  int)(0xb9f2 << 16)                // z196
#define LOCGR_ZOPC  (unsigned  int)(0xb9e2 << 16)                // z196

// immediate to register transfer
#define IIHH_ZOPC   (unsigned  int)(165 << 24)
#define IIHL_ZOPC   (unsigned  int)(165 << 24 | 1 << 16)
#define IILH_ZOPC   (unsigned  int)(165 << 24 | 2 << 16)
#define IILL_ZOPC   (unsigned  int)(165 << 24 | 3 << 16)
#define IIHF_ZOPC   (unsigned long)(0xc0L << 40 | 8L << 32)
#define IILF_ZOPC   (unsigned long)(0xc0L << 40 | 9L << 32)
#define LLIHH_ZOPC  (unsigned  int)(165 << 24 | 12 << 16)
#define LLIHL_ZOPC  (unsigned  int)(165 << 24 | 13 << 16)
#define LLILH_ZOPC  (unsigned  int)(165 << 24 | 14 << 16)
#define LLILL_ZOPC  (unsigned  int)(165 << 24 | 15 << 16)
#define LLIHF_ZOPC  (unsigned long)(0xc0L << 40 | 14L << 32)
#define LLILF_ZOPC  (unsigned long)(0xc0L << 40 | 15L << 32)
#define LHI_ZOPC    (unsigned  int)(167 << 24 | 8 << 16)
#define LGHI_ZOPC   (unsigned  int)(167 << 24 | 9 << 16)
#define LGFI_ZOPC   (unsigned long)(0xc0L << 40 | 1L << 32)

#define LZER_ZOPC   (unsigned  int)(0xb374 << 16)
#define LZDR_ZOPC   (unsigned  int)(0xb375 << 16)

// LOAD: memory to register transfer
#define LB_ZOPC     (unsigned long)(227L << 40 | 118L)
#define LH_ZOPC     (unsigned  int)(72 << 24)
#define LHY_ZOPC    (unsigned long)(227L << 40 | 120L)
#define L_ZOPC      (unsigned  int)(88 << 24)
#define LY_ZOPC     (unsigned long)(227L << 40 | 88L)
#define LT_ZOPC     (unsigned long)(0xe3L << 40 | 0x12L)
#define LGB_ZOPC    (unsigned long)(227L << 40 | 119L)
#define LGH_ZOPC    (unsigned long)(227L << 40 | 21L)
#define LGF_ZOPC    (unsigned long)(227L << 40 | 20L)
#define LG_ZOPC     (unsigned long)(227L << 40 | 4L)
#define LTG_ZOPC    (unsigned long)(0xe3L << 40 | 0x02L)
#define LTGF_ZOPC   (unsigned long)(0xe3L << 40 | 0x32L)

#define LLC_ZOPC    (unsigned long)(0xe3L << 40 | 0x94L)
#define LLH_ZOPC    (unsigned long)(0xe3L << 40 | 0x95L)
#define LLGT_ZOPC   (unsigned long)(227L << 40 | 23L)
#define LLGC_ZOPC   (unsigned long)(227L << 40 | 144L)
#define LLGH_ZOPC   (unsigned long)(227L << 40 | 145L)
#define LLGF_ZOPC   (unsigned long)(227L << 40 | 22L)

#define IC_ZOPC     (unsigned  int)(0x43  << 24)
#define ICY_ZOPC    (unsigned long)(0xe3L << 40 | 0x73L)
#define ICM_ZOPC    (unsigned  int)(0xbf  << 24)
#define ICMY_ZOPC   (unsigned long)(0xebL << 40 | 0x81L)
#define ICMH_ZOPC   (unsigned long)(0xebL << 40 | 0x80L)

#define LRVH_ZOPC   (unsigned long)(0xe3L << 40 | 0x1fL)
#define LRV_ZOPC    (unsigned long)(0xe3L << 40 | 0x1eL)
#define LRVG_ZOPC   (unsigned long)(0xe3L << 40 | 0x0fL)


// LOAD relative: memory to register transfer
#define LHRL_ZOPC   (unsigned long)(0xc4L << 40 | 0x05L << 32)  // z10
#define LRL_ZOPC    (unsigned long)(0xc4L << 40 | 0x0dL << 32)  // z10
#define LGHRL_ZOPC  (unsigned long)(0xc4L << 40 | 0x04L << 32)  // z10
#define LGFRL_ZOPC  (unsigned long)(0xc4L << 40 | 0x0cL << 32)  // z10
#define LGRL_ZOPC   (unsigned long)(0xc4L << 40 | 0x08L << 32)  // z10

#define LLHRL_ZOPC  (unsigned long)(0xc4L << 40 | 0x02L << 32)  // z10
#define LLGHRL_ZOPC (unsigned long)(0xc4L << 40 | 0x06L << 32)  // z10
#define LLGFRL_ZOPC (unsigned long)(0xc4L << 40 | 0x0eL << 32)  // z10

#define LOC_ZOPC    (unsigned long)(0xebL << 40 | 0xf2L)        // z196
#define LOCG_ZOPC   (unsigned long)(0xebL << 40 | 0xe2L)        // z196


// LOAD multiple registers at once
#define LM_ZOPC     (unsigned  int)(0x98  << 24)
#define LMY_ZOPC    (unsigned long)(0xebL << 40 | 0x98L)
#define LMG_ZOPC    (unsigned long)(0xebL << 40 | 0x04L)

#define LE_ZOPC     (unsigned  int)(0x78 << 24)
#define LEY_ZOPC    (unsigned long)(237L << 40 | 100L)
#define LDEB_ZOPC   (unsigned long)(237L << 40 | 4)
#define LD_ZOPC     (unsigned  int)(0x68 << 24)
#define LDY_ZOPC    (unsigned long)(237L << 40 | 101L)
#define LXEB_ZOPC   (unsigned long)(237L << 40 | 6)
#define LXDB_ZOPC   (unsigned long)(237L << 40 | 5)

// STORE: register to memory transfer
#define STC_ZOPC    (unsigned  int)(0x42 << 24)
#define STCY_ZOPC   (unsigned long)(227L << 40 | 114L)
#define STH_ZOPC    (unsigned  int)(64 << 24)
#define STHY_ZOPC   (unsigned long)(227L << 40 | 112L)
#define ST_ZOPC     (unsigned  int)(80 << 24)
#define STY_ZOPC    (unsigned long)(227L << 40 | 80L)
#define STG_ZOPC    (unsigned long)(227L << 40 | 36L)

#define STCM_ZOPC   (unsigned long)(0xbeL << 24)
#define STCMY_ZOPC  (unsigned long)(0xebL << 40 | 0x2dL)
#define STCMH_ZOPC  (unsigned long)(0xebL << 40 | 0x2cL)

// STORE relative: memory to register transfer
#define STHRL_ZOPC  (unsigned long)(0xc4L << 40 | 0x07L << 32)  // z10
#define STRL_ZOPC   (unsigned long)(0xc4L << 40 | 0x0fL << 32)  // z10
#define STGRL_ZOPC  (unsigned long)(0xc4L << 40 | 0x0bL << 32)  // z10

#define STOC_ZOPC   (unsigned long)(0xebL << 40 | 0xf3L)        // z196
#define STOCG_ZOPC  (unsigned long)(0xebL << 40 | 0xe3L)        // z196

// STORE multiple registers at once
#define STM_ZOPC    (unsigned  int)(0x90  << 24)
#define STMY_ZOPC   (unsigned long)(0xebL << 40 | 0x90L)
#define STMG_ZOPC   (unsigned long)(0xebL << 40 | 0x24L)

#define STE_ZOPC    (unsigned  int)(0x70 << 24)
#define STEY_ZOPC   (unsigned long)(237L << 40 | 102L)
#define STD_ZOPC    (unsigned  int)(0x60 << 24)
#define STDY_ZOPC   (unsigned long)(237L << 40 | 103L)

// MOVE: immediate to memory transfer
#define MVHHI_ZOPC  (unsigned long)(0xe5L << 40 | 0x44L << 32)   // z10
#define MVHI_ZOPC   (unsigned long)(0xe5L << 40 | 0x4cL << 32)   // z10
#define MVGHI_ZOPC  (unsigned long)(0xe5L << 40 | 0x48L << 32)   // z10


//  ALU operations

// Load Positive
#define LPR_ZOPC    (unsigned  int)(16 << 8)
#define LPGFR_ZOPC  (unsigned  int)(185 << 24 | 16 << 16)
#define LPGR_ZOPC   (unsigned  int)(185 << 24)
#define LPEBR_ZOPC  (unsigned  int)(179 << 24)
#define LPDBR_ZOPC  (unsigned  int)(179 << 24 | 16 << 16)
#define LPXBR_ZOPC  (unsigned  int)(179 << 24 | 64 << 16)

// Load Negative
#define LNR_ZOPC    (unsigned  int)(17 << 8)
#define LNGFR_ZOPC  (unsigned  int)(185 << 24 | 17 << 16)
#define LNGR_ZOPC   (unsigned  int)(185 << 24 | 1 << 16)
#define LNEBR_ZOPC  (unsigned  int)(179 << 24 | 1 << 16)
#define LNDBR_ZOPC  (unsigned  int)(179 << 24 | 17 << 16)
#define LNXBR_ZOPC  (unsigned  int)(179 << 24 | 65 << 16)

// Load Complement
#define LCR_ZOPC    (unsigned  int)(19 << 8)
#define LCGFR_ZOPC  (unsigned  int)(185 << 24 | 19 << 16)
#define LCGR_ZOPC   (unsigned  int)(185 << 24 | 3 << 16)
#define LCEBR_ZOPC  (unsigned  int)(179 << 24 | 3 << 16)
#define LCDBR_ZOPC  (unsigned  int)(179 << 24 | 19 << 16)
#define LCXBR_ZOPC  (unsigned  int)(179 << 24 | 67 << 16)

// Add
// RR, signed
#define AR_ZOPC     (unsigned  int)(26 << 8)
#define AGFR_ZOPC   (unsigned  int)(0xb9 << 24 | 0x18 << 16)
#define AGR_ZOPC    (unsigned  int)(0xb9 << 24 | 0x08 << 16)
// RRF, signed
#define ARK_ZOPC    (unsigned  int)(0xb9 << 24 | 0x00f8 << 16)
#define AGRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00e8 << 16)
// RI, signed
#define AHI_ZOPC    (unsigned  int)(167 << 24 | 10 << 16)
#define AFI_ZOPC    (unsigned long)(0xc2L << 40 | 9L << 32)
#define AGHI_ZOPC   (unsigned  int)(167 << 24 | 11 << 16)
#define AGFI_ZOPC   (unsigned long)(0xc2L << 40 | 8L << 32)
// RIE, signed
#define AHIK_ZOPC   (unsigned long)(0xecL << 40 | 0x00d8L)
#define AGHIK_ZOPC  (unsigned long)(0xecL << 40 | 0x00d9L)
#define AIH_ZOPC    (unsigned long)(0xccL << 40 | 0x08L << 32)
// RM, signed
#define AHY_ZOPC    (unsigned long)(227L << 40 | 122L)
#define A_ZOPC      (unsigned  int)(90 << 24)
#define AY_ZOPC     (unsigned long)(227L << 40 | 90L)
#define AGF_ZOPC    (unsigned long)(227L << 40 | 24L)
#define AG_ZOPC     (unsigned long)(227L << 40 | 8L)
// In-memory arithmetic (add signed, add logical with signed immediate).
// MI, signed
#define ASI_ZOPC    (unsigned long)(0xebL << 40 | 0x6aL)
#define AGSI_ZOPC   (unsigned long)(0xebL << 40 | 0x7aL)

// RR, Logical
#define ALR_ZOPC    (unsigned  int)(30 << 8)
#define ALGFR_ZOPC  (unsigned  int)(185 << 24 | 26 << 16)
#define ALGR_ZOPC   (unsigned  int)(185 << 24 | 10 << 16)
#define ALCGR_ZOPC  (unsigned  int)(185 << 24 | 136 << 16)
// RRF, Logical
#define ALRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00fa << 16)
#define ALGRK_ZOPC  (unsigned  int)(0xb9 << 24 | 0x00ea << 16)
// RI, Logical
#define ALFI_ZOPC   (unsigned long)(0xc2L << 40 | 0x0bL << 32)
#define ALGFI_ZOPC  (unsigned long)(0xc2L << 40 | 0x0aL << 32)
// RIE, Logical
#define ALHSIK_ZOPC (unsigned long)(0xecL << 40 | 0x00daL)
#define ALGHSIK_ZOPC (unsigned long)(0xecL << 40 | 0x00dbL)
// RM, Logical
#define AL_ZOPC     (unsigned  int)(0x5e << 24)
#define ALY_ZOPC    (unsigned long)(227L << 40 | 94L)
#define ALGF_ZOPC   (unsigned long)(227L << 40 | 26L)
#define ALG_ZOPC    (unsigned long)(227L << 40 | 10L)
// In-memory arithmetic (add signed, add logical with signed immediate).
// MI, Logical
#define ALSI_ZOPC   (unsigned long)(0xebL << 40 | 0x6eL)
#define ALGSI_ZOPC  (unsigned long)(0xebL << 40 | 0x7eL)

// RR, BFP
#define AEBR_ZOPC   (unsigned  int)(179 << 24 | 10 << 16)
#define ADBR_ZOPC   (unsigned  int)(179 << 24 | 26 << 16)
#define AXBR_ZOPC   (unsigned  int)(179 << 24 | 74 << 16)
// RM, BFP
#define AEB_ZOPC    (unsigned long)(237L << 40 | 10)
#define ADB_ZOPC    (unsigned long)(237L << 40 | 26)

// Subtract
// RR, signed
#define SR_ZOPC     (unsigned  int)(27 << 8)
#define SGFR_ZOPC   (unsigned  int)(185 << 24 | 25 << 16)
#define SGR_ZOPC    (unsigned  int)(185 << 24 | 9 << 16)
// RRF, signed
#define SRK_ZOPC    (unsigned  int)(0xb9 << 24 | 0x00f9 << 16)
#define SGRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00e9 << 16)
//   RM, signed
#define SH_ZOPC     (unsigned  int)(0x4b << 24)
#define SHY_ZOPC    (unsigned long)(227L << 40 | 123L)
#define S_ZOPC      (unsigned  int)(0x5B << 24)
#define SY_ZOPC     (unsigned long)(227L << 40 | 91L)
#define SGF_ZOPC    (unsigned long)(227L << 40 | 25)
#define SG_ZOPC     (unsigned long)(227L << 40 | 9)
// RR, Logical
#define SLR_ZOPC    (unsigned  int)(31 << 8)
#define SLGFR_ZOPC  (unsigned  int)(185 << 24 | 27 << 16)
#define SLGR_ZOPC   (unsigned  int)(185 << 24 | 11 << 16)
// RIL, Logical
#define SLFI_ZOPC   (unsigned long)(0xc2L << 40 | 0x05L << 32)
#define SLGFI_ZOPC  (unsigned long)(0xc2L << 40 | 0x04L << 32)
// RRF, Logical
#define SLRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00fb << 16)
#define SLGRK_ZOPC  (unsigned  int)(0xb9 << 24 | 0x00eb << 16)
// RM, Logical
#define SLY_ZOPC    (unsigned long)(227L << 40 | 95L)
#define SLGF_ZOPC   (unsigned long)(227L << 40 | 27L)
#define SLG_ZOPC    (unsigned long)(227L << 40 | 11L)

// RR, BFP
#define SEBR_ZOPC   (unsigned  int)(179 << 24 | 11 << 16)
#define SDBR_ZOPC   (unsigned  int)(179 << 24 | 27 << 16)
#define SXBR_ZOPC   (unsigned  int)(179 << 24 | 75 << 16)
// RM, BFP
#define SEB_ZOPC    (unsigned long)(237L << 40 | 11)
#define SDB_ZOPC    (unsigned long)(237L << 40 | 27)

// Multiply
// RR, signed
#define MR_ZOPC     (unsigned  int)(28 << 8)
#define MSR_ZOPC    (unsigned  int)(178 << 24 | 82 << 16)
#define MSGFR_ZOPC  (unsigned  int)(185 << 24 | 28 << 16)
#define MSGR_ZOPC   (unsigned  int)(185 << 24 | 12 << 16)
// RI, signed
#define MHI_ZOPC    (unsigned  int)(167 << 24 | 12 << 16)
#define MGHI_ZOPC   (unsigned  int)(167 << 24 | 13 << 16)
#define MSFI_ZOPC   (unsigned long)(0xc2L << 40 | 0x01L << 32)   // z10
#define MSGFI_ZOPC  (unsigned long)(0xc2L << 40 | 0x00L << 32)   // z10
// RM, signed
#define M_ZOPC      (unsigned  int)(92 << 24)
#define MS_ZOPC     (unsigned  int)(0x71 << 24)
#define MHY_ZOPC    (unsigned long)(0xe3L<< 40 | 0x7cL)
#define MSY_ZOPC    (unsigned long)(227L << 40 | 81L)
#define MSGF_ZOPC   (unsigned long)(227L << 40 | 28L)
#define MSG_ZOPC    (unsigned long)(227L << 40 | 12L)
// RR, unsigned
#define MLR_ZOPC    (unsigned  int)(185 << 24 | 150 << 16)
#define MLGR_ZOPC   (unsigned  int)(185 << 24 | 134 << 16)
// RM, unsigned
#define ML_ZOPC     (unsigned long)(227L << 40 | 150L)
#define MLG_ZOPC    (unsigned long)(227L << 40 | 134L)

// RR, BFP
#define MEEBR_ZOPC  (unsigned  int)(179 << 24 | 23 << 16)
#define MDEBR_ZOPC  (unsigned  int)(179 << 24 | 12 << 16)
#define MDBR_ZOPC   (unsigned  int)(179 << 24 | 28 << 16)
#define MXDBR_ZOPC  (unsigned  int)(179 << 24 | 7 << 16)
#define MXBR_ZOPC   (unsigned  int)(179 << 24 | 76 << 16)
// RM, BFP
#define MEEB_ZOPC   (unsigned long)(237L << 40 | 23)
#define MDEB_ZOPC   (unsigned long)(237L << 40 | 12)
#define MDB_ZOPC    (unsigned long)(237L << 40 | 28)
#define MXDB_ZOPC   (unsigned long)(237L << 40 | 7)

// Multiply-Add
#define MAEBR_ZOPC  (unsigned  int)(179 << 24 | 14 << 16)
#define MADBR_ZOPC  (unsigned  int)(179 << 24 | 30 << 16)
#define MSEBR_ZOPC  (unsigned  int)(179 << 24 | 15 << 16)
#define MSDBR_ZOPC  (unsigned  int)(179 << 24 | 31 << 16)
#define MAEB_ZOPC   (unsigned long)(237L << 40 | 14)
#define MADB_ZOPC   (unsigned long)(237L << 40 | 30)
#define MSEB_ZOPC   (unsigned long)(237L << 40 | 15)
#define MSDB_ZOPC   (unsigned long)(237L << 40 | 31)

// Divide
// RR, signed
#define DSGFR_ZOPC  (unsigned  int)(0xb91d << 16)
#define DSGR_ZOPC   (unsigned  int)(0xb90d << 16)
// RM, signed
#define D_ZOPC      (unsigned  int)(93 << 24)
#define DSGF_ZOPC   (unsigned long)(227L << 40 | 29L)
#define DSG_ZOPC    (unsigned long)(227L << 40 | 13L)
// RR, unsigned
#define DLR_ZOPC    (unsigned  int)(185 << 24 | 151 << 16)
#define DLGR_ZOPC   (unsigned  int)(185 << 24 | 135 << 16)
// RM, unsigned
#define DL_ZOPC     (unsigned long)(227L << 40 | 151L)
#define DLG_ZOPC    (unsigned long)(227L << 40 | 135L)

// RR, BFP
#define DEBR_ZOPC   (unsigned  int)(179 << 24 | 13 << 16)
#define DDBR_ZOPC   (unsigned  int)(179 << 24 | 29 << 16)
#define DXBR_ZOPC   (unsigned  int)(179 << 24 | 77 << 16)
// RM, BFP
#define DEB_ZOPC    (unsigned long)(237L << 40 | 13)
#define DDB_ZOPC    (unsigned long)(237L << 40 | 29)

// Square Root
// RR, BFP
#define SQEBR_ZOPC  (unsigned  int)(0xb314 << 16)
#define SQDBR_ZOPC  (unsigned  int)(0xb315 << 16)
#define SQXBR_ZOPC  (unsigned  int)(0xb316 << 16)
// RM, BFP
#define SQEB_ZOPC   (unsigned long)(237L << 40 | 20)
#define SQDB_ZOPC   (unsigned long)(237L << 40 | 21)

// Compare and Test
// RR, signed
#define CR_ZOPC     (unsigned  int)(25 << 8)
#define CGFR_ZOPC   (unsigned  int)(185 << 24 | 48 << 16)
#define CGR_ZOPC    (unsigned  int)(185 << 24 | 32 << 16)
// RI, signed
#define CHI_ZOPC    (unsigned  int)(167 << 24 | 14 << 16)
#define CFI_ZOPC    (unsigned long)(0xc2L << 40 | 0xdL << 32)
#define CGHI_ZOPC   (unsigned  int)(167 << 24 | 15 << 16)
#define CGFI_ZOPC   (unsigned long)(0xc2L << 40 | 0xcL << 32)
// RM, signed
#define CH_ZOPC     (unsigned  int)(0x49 << 24)
#define CHY_ZOPC    (unsigned long)(227L << 40 | 121L)
#define C_ZOPC      (unsigned  int)(0x59 << 24)
#define CY_ZOPC     (unsigned long)(227L << 40 | 89L)
#define CGF_ZOPC    (unsigned long)(227L << 40 | 48L)
#define CG_ZOPC     (unsigned long)(227L << 40 | 32L)
// RR, unsigned
#define CLR_ZOPC    (unsigned  int)(21 << 8)
#define CLGFR_ZOPC  (unsigned  int)(185 << 24 | 49 << 16)
#define CLGR_ZOPC   (unsigned  int)(185 << 24 | 33 << 16)
// RIL, unsigned
#define CLFI_ZOPC   (unsigned long)(0xc2L << 40 | 0xfL << 32)
#define CLGFI_ZOPC  (unsigned long)(0xc2L << 40 | 0xeL << 32)
// RM, unsigned
#define CL_ZOPC     (unsigned  int)(0x55 << 24)
#define CLY_ZOPC    (unsigned long)(227L << 40 | 85L)
#define CLGF_ZOPC   (unsigned long)(227L << 40 | 49L)
#define CLG_ZOPC    (unsigned long)(227L << 40 | 33L)
// RI, unsigned
#define TMHH_ZOPC   (unsigned  int)(167 << 24 | 2 << 16)
#define TMHL_ZOPC   (unsigned  int)(167 << 24 | 3 << 16)
#define TMLH_ZOPC   (unsigned  int)(167 << 24)
#define TMLL_ZOPC   (unsigned  int)(167 << 24 | 1 << 16)

// RR, BFP
#define CEBR_ZOPC   (unsigned  int)(179 << 24 | 9 << 16)
#define CDBR_ZOPC   (unsigned  int)(179 << 24 | 25 << 16)
#define CXBR_ZOPC   (unsigned  int)(179 << 24 | 73 << 16)
// RM, BFP
#define CEB_ZOPC    (unsigned long)(237L << 40 | 9)
#define CDB_ZOPC    (unsigned long)(237L << 40 | 25)

// Shift
// arithmetic
#define SLA_ZOPC    (unsigned  int)(0x8b  << 24)
#define SLAK_ZOPC   (unsigned long)(0xebL << 40 | 0xddL)
#define SLAG_ZOPC   (unsigned long)(0xebL << 40 | 0x0bL)
#define SRA_ZOPC    (unsigned  int)(0x8a  << 24)
#define SRAK_ZOPC   (unsigned long)(0xebL << 40 | 0xdcL)
#define SRAG_ZOPC   (unsigned long)(0xebL << 40 | 0x0aL)
// logical
#define SLL_ZOPC    (unsigned  int)(0x89  << 24)
#define SLLK_ZOPC   (unsigned long)(0xebL << 40 | 0xdfL)
#define SLLG_ZOPC   (unsigned long)(0xebL << 40 | 0x0dL)
#define SRL_ZOPC    (unsigned  int)(0x88  << 24)
#define SRLK_ZOPC   (unsigned long)(0xebL << 40 | 0xdeL)
#define SRLG_ZOPC   (unsigned long)(0xebL << 40 | 0x0cL)

// Rotate, then AND/XOR/OR/insert
// rotate
#define RLL_ZOPC    (unsigned long)(0xebL << 40 | 0x1dL)         // z10
#define RLLG_ZOPC   (unsigned long)(0xebL << 40 | 0x1cL)         // z10
// rotate and {AND|XOR|OR|INS}
#define RNSBG_ZOPC  (unsigned long)(0xecL << 40 | 0x54L)         // z196
#define RXSBG_ZOPC  (unsigned long)(0xecL << 40 | 0x57L)         // z196
#define ROSBG_ZOPC  (unsigned long)(0xecL << 40 | 0x56L)         // z196
#define RISBG_ZOPC  (unsigned long)(0xecL << 40 | 0x55L)         // z196

// AND
// RR, signed
#define NR_ZOPC     (unsigned  int)(20 << 8)
#define NGR_ZOPC    (unsigned  int)(185 << 24 | 128 << 16)
// RRF, signed
#define NRK_ZOPC    (unsigned  int)(0xb9 << 24 | 0x00f4 << 16)
#define NGRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00e4 << 16)
// RI, signed
#define NIHH_ZOPC   (unsigned  int)(165 << 24 | 4 << 16)
#define NIHL_ZOPC   (unsigned  int)(165 << 24 | 5 << 16)
#define NILH_ZOPC   (unsigned  int)(165 << 24 | 6 << 16)
#define NILL_ZOPC   (unsigned  int)(165 << 24 | 7 << 16)
#define NIHF_ZOPC   (unsigned long)(0xc0L << 40 | 10L << 32)
#define NILF_ZOPC   (unsigned long)(0xc0L << 40 | 11L << 32)
// RM, signed
#define N_ZOPC      (unsigned  int)(0x54 << 24)
#define NY_ZOPC     (unsigned long)(227L << 40 | 84L)
#define NG_ZOPC     (unsigned long)(227L << 40 | 128L)

// OR
// RR, signed
#define OR_ZOPC     (unsigned  int)(22 << 8)
#define OGR_ZOPC    (unsigned  int)(185 << 24 | 129 << 16)
// RRF, signed
#define ORK_ZOPC    (unsigned  int)(0xb9 << 24 | 0x00f6 << 16)
#define OGRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00e6 << 16)
// RI, signed
#define OIHH_ZOPC   (unsigned  int)(165 << 24 | 8 << 16)
#define OIHL_ZOPC   (unsigned  int)(165 << 24 | 9 << 16)
#define OILH_ZOPC   (unsigned  int)(165 << 24 | 10 << 16)
#define OILL_ZOPC   (unsigned  int)(165 << 24 | 11 << 16)
#define OIHF_ZOPC   (unsigned long)(0xc0L << 40 | 12L << 32)
#define OILF_ZOPC   (unsigned long)(0xc0L << 40 | 13L << 32)
// RM, signed
#define O_ZOPC      (unsigned  int)(0x56 << 24)
#define OY_ZOPC     (unsigned long)(227L << 40 | 86L)
#define OG_ZOPC     (unsigned long)(227L << 40 | 129L)

// XOR
// RR, signed
#define XR_ZOPC     (unsigned  int)(23 << 8)
#define XGR_ZOPC    (unsigned  int)(185 << 24 | 130 << 16)
// RRF, signed
#define XRK_ZOPC    (unsigned  int)(0xb9 << 24 | 0x00f7 << 16)
#define XGRK_ZOPC   (unsigned  int)(0xb9 << 24 | 0x00e7 << 16)
// RI, signed
#define XIHF_ZOPC   (unsigned long)(0xc0L << 40 | 6L << 32)
#define XILF_ZOPC   (unsigned long)(0xc0L << 40 | 7L << 32)
// RM, signed
#define X_ZOPC      (unsigned  int)(0x57 << 24)
#define XY_ZOPC     (unsigned long)(227L << 40 | 87L)
#define XG_ZOPC     (unsigned long)(227L << 40 | 130L)


// Data Conversion

// INT to BFP
#define CEFBR_ZOPC  (unsigned  int)(179 << 24 | 148 << 16)
#define CDFBR_ZOPC  (unsigned  int)(179 << 24 | 149 << 16)
#define CXFBR_ZOPC  (unsigned  int)(179 << 24 | 150 << 16)
#define CEGBR_ZOPC  (unsigned  int)(179 << 24 | 164 << 16)
#define CDGBR_ZOPC  (unsigned  int)(179 << 24 | 165 << 16)
#define CXGBR_ZOPC  (unsigned  int)(179 << 24 | 166 << 16)
// BFP to INT
#define CFEBR_ZOPC  (unsigned  int)(179 << 24 | 152 << 16)
#define CFDBR_ZOPC  (unsigned  int)(179 << 24 | 153 << 16)
#define CFXBR_ZOPC  (unsigned  int)(179 << 24 | 154 << 16)
#define CGEBR_ZOPC  (unsigned  int)(179 << 24 | 168 << 16)
#define CGDBR_ZOPC  (unsigned  int)(179 << 24 | 169 << 16)
#define CGXBR_ZOPC  (unsigned  int)(179 << 24 | 170 << 16)
// INT to DEC
#define CVD_ZOPC    (unsigned  int)(0x4e << 24)
#define CVDY_ZOPC   (unsigned long)(0xe3L << 40 | 0x26L)
#define CVDG_ZOPC   (unsigned long)(0xe3L << 40 | 0x2eL)


// BFP Control

#define SRNM_ZOPC   (unsigned  int)(178 << 24 | 153 << 16)
#define EFPC_ZOPC   (unsigned  int)(179 << 24 | 140 << 16)
#define SFPC_ZOPC   (unsigned  int)(179 << 24 | 132 << 16)
#define STFPC_ZOPC  (unsigned  int)(178 << 24 | 156 << 16)
#define LFPC_ZOPC   (unsigned  int)(178 << 24 | 157 << 16)


// Branch Instructions

// Register
#define BCR_ZOPC    (unsigned  int)(7 << 8)
#define BALR_ZOPC   (unsigned  int)(5 << 8)
#define BASR_ZOPC   (unsigned  int)(13 << 8)
#define BCTGR_ZOPC  (unsigned long)(0xb946 << 16)
// Absolute
#define BC_ZOPC     (unsigned  int)(71 << 24)
#define BAL_ZOPC    (unsigned  int)(69 << 24)
#define BAS_ZOPC    (unsigned  int)(77 << 24)
#define BXH_ZOPC    (unsigned  int)(134 << 24)
#define BXHG_ZOPC   (unsigned long)(235L << 40 | 68)
// Relative
#define BRC_ZOPC    (unsigned  int)(167 << 24 | 4 << 16)
#define BRCL_ZOPC   (unsigned long)(192L << 40 | 4L << 32)
#define BRAS_ZOPC   (unsigned  int)(167 << 24 | 5 << 16)
#define BRASL_ZOPC  (unsigned long)(192L << 40 | 5L << 32)
#define BRCT_ZOPC   (unsigned  int)(167 << 24 | 6 << 16)
#define BRCTG_ZOPC  (unsigned  int)(167 << 24 | 7 << 16)
#define BRXH_ZOPC   (unsigned  int)(132 << 24)
#define BRXHG_ZOPC  (unsigned long)(236L << 40 | 68)
#define BRXLE_ZOPC  (unsigned  int)(133 << 24)
#define BRXLG_ZOPC  (unsigned long)(236L << 40 | 69)


// Compare and Branch Instructions

// signed comp reg/reg, branch Absolute
#define CRB_ZOPC    (unsigned long)(0xecL << 40 | 0xf6L)         // z10
#define CGRB_ZOPC   (unsigned long)(0xecL << 40 | 0xe4L)         // z10
// signed comp reg/reg, branch Relative
#define CRJ_ZOPC    (unsigned long)(0xecL << 40 | 0x76L)         // z10
#define CGRJ_ZOPC   (unsigned long)(0xecL << 40 | 0x64L)         // z10
// signed comp reg/imm, branch absolute
#define CIB_ZOPC    (unsigned long)(0xecL << 40 | 0xfeL)         // z10
#define CGIB_ZOPC   (unsigned long)(0xecL << 40 | 0xfcL)         // z10
// signed comp reg/imm, branch relative
#define CIJ_ZOPC    (unsigned long)(0xecL << 40 | 0x7eL)         // z10
#define CGIJ_ZOPC   (unsigned long)(0xecL << 40 | 0x7cL)         // z10

// unsigned comp reg/reg, branch Absolute
#define CLRB_ZOPC   (unsigned long)(0xecL << 40 | 0xf7L)         // z10
#define CLGRB_ZOPC  (unsigned long)(0xecL << 40 | 0xe5L)         // z10
// unsigned comp reg/reg, branch Relative
#define CLRJ_ZOPC   (unsigned long)(0xecL << 40 | 0x77L)         // z10
#define CLGRJ_ZOPC  (unsigned long)(0xecL << 40 | 0x65L)         // z10
// unsigned comp reg/imm, branch absolute
#define CLIB_ZOPC   (unsigned long)(0xecL << 40 | 0xffL)         // z10
#define CLGIB_ZOPC  (unsigned long)(0xecL << 40 | 0xfdL)         // z10
// unsigned comp reg/imm, branch relative
#define CLIJ_ZOPC   (unsigned long)(0xecL << 40 | 0x7fL)         // z10
#define CLGIJ_ZOPC  (unsigned long)(0xecL << 40 | 0x7dL)         // z10

// comp reg/reg, trap
#define CRT_ZOPC    (unsigned  int)(0xb972 << 16)                // z10
#define CGRT_ZOPC   (unsigned  int)(0xb960 << 16)                // z10
#define CLRT_ZOPC   (unsigned  int)(0xb973 << 16)                // z10
#define CLGRT_ZOPC  (unsigned  int)(0xb961 << 16)                // z10
// comp reg/imm, trap
#define CIT_ZOPC    (unsigned long)(0xecL << 40 | 0x72L)         // z10
#define CGIT_ZOPC   (unsigned long)(0xecL << 40 | 0x70L)         // z10
#define CLFIT_ZOPC  (unsigned long)(0xecL << 40 | 0x73L)         // z10
#define CLGIT_ZOPC  (unsigned long)(0xecL << 40 | 0x71L)         // z10


// Direct Memory Operations

// Compare
#define CLI_ZOPC    (unsigned  int)(0x95  << 24)
#define CLIY_ZOPC   (unsigned long)(0xebL << 40 | 0x55L)
#define CLC_ZOPC    (unsigned long)(0xd5L << 40)
#define CLCL_ZOPC   (unsigned  int)(0x0f  <<  8)
#define CLCLE_ZOPC  (unsigned  int)(0xa9  << 24)
#define CLCLU_ZOPC  (unsigned long)(0xebL << 40 | 0x8fL)

// Move
#define MVI_ZOPC    (unsigned  int)(0x92  << 24)
#define MVIY_ZOPC   (unsigned long)(0xebL << 40 | 0x52L)
#define MVC_ZOPC    (unsigned long)(0xd2L << 40)
#define MVCL_ZOPC   (unsigned  int)(0x0e  <<  8)
#define MVCLE_ZOPC  (unsigned  int)(0xa8  << 24)

// Test
#define TM_ZOPC     (unsigned  int)(0x91  << 24)
#define TMY_ZOPC    (unsigned long)(0xebL << 40 | 0x51L)

// AND
#define NI_ZOPC     (unsigned  int)(0x94  << 24)
#define NIY_ZOPC    (unsigned long)(0xebL << 40 | 0x54L)
#define NC_ZOPC     (unsigned long)(0xd4L << 40)

// OR
#define OI_ZOPC     (unsigned  int)(0x96  << 24)
#define OIY_ZOPC    (unsigned long)(0xebL << 40 | 0x56L)
#define OC_ZOPC     (unsigned long)(0xd6L << 40)

// XOR
#define XI_ZOPC     (unsigned  int)(0x97  << 24)
#define XIY_ZOPC    (unsigned long)(0xebL << 40 | 0x57L)
#define XC_ZOPC     (unsigned long)(0xd7L << 40)

// Search String
#define SRST_ZOPC   (unsigned  int)(178 << 24 | 94 << 16)
#define SRSTU_ZOPC  (unsigned  int)(185 << 24 | 190 << 16)

// Translate characters
#define TROO_ZOPC   (unsigned  int)(0xb9 << 24 | 0x93 << 16)
#define TROT_ZOPC   (unsigned  int)(0xb9 << 24 | 0x92 << 16)
#define TRTO_ZOPC   (unsigned  int)(0xb9 << 24 | 0x91 << 16)
#define TRTT_ZOPC   (unsigned  int)(0xb9 << 24 | 0x90 << 16)


//---------------------------
//--  Vector Instructions  --
//---------------------------

//---<  Vector Support Instructions  >---

//---  Load (memory)  ---

#define VLM_ZOPC    (unsigned long)(0xe7L << 40 | 0x36L << 0)   // load full vreg range (n * 128 bit)
#define VL_ZOPC     (unsigned long)(0xe7L << 40 | 0x06L << 0)   // load full vreg (128 bit)
#define VLEB_ZOPC   (unsigned long)(0xe7L << 40 | 0x00L << 0)   // load vreg element (8 bit)
#define VLEH_ZOPC   (unsigned long)(0xe7L << 40 | 0x01L << 0)   // load vreg element (16 bit)
#define VLEF_ZOPC   (unsigned long)(0xe7L << 40 | 0x03L << 0)   // load vreg element (32 bit)
#define VLEG_ZOPC   (unsigned long)(0xe7L << 40 | 0x02L << 0)   // load vreg element (64 bit)

#define VLREP_ZOPC  (unsigned long)(0xe7L << 40 | 0x05L << 0)   // load and replicate into all vector elements
#define VLLEZ_ZOPC  (unsigned long)(0xe7L << 40 | 0x04L << 0)   // load logical element and zero.

// vector register gather
#define VGEF_ZOPC   (unsigned long)(0xe7L << 40 | 0x13L << 0)   // gather element (32 bit), V1(M3) = [D2(V2(M3),B2)]
#define VGEG_ZOPC   (unsigned long)(0xe7L << 40 | 0x12L << 0)   // gather element (64 bit), V1(M3) = [D2(V2(M3),B2)]
// vector register scatter
#define VSCEF_ZOPC  (unsigned long)(0xe7L << 40 | 0x1bL << 0)   // vector scatter element FW
#define VSCEG_ZOPC  (unsigned long)(0xe7L << 40 | 0x1aL << 0)   // vector scatter element DW

#define VLBB_ZOPC   (unsigned long)(0xe7L << 40 | 0x07L << 0)   // load vreg to block boundary (load to alignment).
#define VLL_ZOPC    (unsigned long)(0xe7L << 40 | 0x37L << 0)   // load vreg with length.

//---  Load (register)  ---

#define VLR_ZOPC    (unsigned long)(0xe7L << 40 | 0x56L << 0)   // copy full vreg (128 bit)
#define VLGV_ZOPC   (unsigned long)(0xe7L << 40 | 0x21L << 0)   // copy vreg element -> GR
#define VLVG_ZOPC   (unsigned long)(0xe7L << 40 | 0x22L << 0)   // copy GR -> vreg element
#define VLVGP_ZOPC  (unsigned long)(0xe7L << 40 | 0x62L << 0)   // copy GR2, GR3 (disjoint pair) -> vreg

// vector register pack: cut in half the size the source vector elements
#define VPK_ZOPC    (unsigned long)(0xe7L << 40 | 0x94L << 0)   // just cut
#define VPKS_ZOPC   (unsigned long)(0xe7L << 40 | 0x97L << 0)   // saturate as signed values
#define VPKLS_ZOPC  (unsigned long)(0xe7L << 40 | 0x95L << 0)   // saturate as unsigned values

// vector register unpack: double in size the source vector elements
#define VUPH_ZOPC   (unsigned long)(0xe7L << 40 | 0xd7L << 0)   // signed, left half of the source vector elements
#define VUPLH_ZOPC  (unsigned long)(0xe7L << 40 | 0xd5L << 0)   // unsigned, left half of the source vector elements
#define VUPL_ZOPC   (unsigned long)(0xe7L << 40 | 0xd6L << 0)   // signed, right half of the source vector elements
#define VUPLL_ZOPC  (unsigned long)(0xe7L << 40 | 0xd4L << 0)   // unsigned, right half of the source vector element

// vector register merge
#define VMRH_ZOPC   (unsigned long)(0xe7L << 40 | 0x61L << 0)   // register merge high (left half of source registers)
#define VMRL_ZOPC   (unsigned long)(0xe7L << 40 | 0x60L << 0)   // register merge low (right half of source registers)

// vector register permute
#define VPERM_ZOPC  (unsigned long)(0xe7L << 40 | 0x8cL << 0)   // vector permute
#define VPDI_ZOPC   (unsigned long)(0xe7L << 40 | 0x84L << 0)   // vector permute DW immediate

// vector register replicate
#define VREP_ZOPC   (unsigned long)(0xe7L << 40 | 0x4dL << 0)   // vector replicate
#define VREPI_ZOPC  (unsigned long)(0xe7L << 40 | 0x45L << 0)   // vector replicate immediate
#define VSEL_ZOPC   (unsigned long)(0xe7L << 40 | 0x8dL << 0)   // vector select

#define VSEG_ZOPC   (unsigned long)(0xe7L << 40 | 0x5fL << 0)   // vector sign-extend to DW (rightmost element in each DW).

//---  Load (immediate)  ---

#define VLEIB_ZOPC  (unsigned long)(0xe7L << 40 | 0x40L << 0)   // load vreg element (16 bit imm to 8 bit)
#define VLEIH_ZOPC  (unsigned long)(0xe7L << 40 | 0x41L << 0)   // load vreg element (16 bit imm to 16 bit)
#define VLEIF_ZOPC  (unsigned long)(0xe7L << 40 | 0x43L << 0)   // load vreg element (16 bit imm to 32 bit)
#define VLEIG_ZOPC  (unsigned long)(0xe7L << 40 | 0x42L << 0)   // load vreg element (16 bit imm to 64 bit)

//---  Store  ---

#define VSTM_ZOPC   (unsigned long)(0xe7L << 40 | 0x3eL << 0)   // store full vreg range (n * 128 bit)
#define VST_ZOPC    (unsigned long)(0xe7L << 40 | 0x0eL << 0)   // store full vreg (128 bit)
#define VSTEB_ZOPC  (unsigned long)(0xe7L << 40 | 0x08L << 0)   // store vreg element (8 bit)
#define VSTEH_ZOPC  (unsigned long)(0xe7L << 40 | 0x09L << 0)   // store vreg element (16 bit)
#define VSTEF_ZOPC  (unsigned long)(0xe7L << 40 | 0x0bL << 0)   // store vreg element (32 bit)
#define VSTEG_ZOPC  (unsigned long)(0xe7L << 40 | 0x0aL << 0)   // store vreg element (64 bit)
#define VSTL_ZOPC   (unsigned long)(0xe7L << 40 | 0x3fL << 0)   // store vreg with length.

//---  Misc  ---

#define VGM_ZOPC    (unsigned long)(0xe7L << 40 | 0x46L << 0)   // generate bit  mask, [start..end] = '1', else '0'
#define VGBM_ZOPC   (unsigned long)(0xe7L << 40 | 0x44L << 0)   // generate byte mask, bits(imm16) -> bytes

//---<  Vector Arithmetic Instructions  >---

// Load
#define VLC_ZOPC    (unsigned long)(0xe7L << 40 | 0xdeL << 0)   // V1 := -V2,   element size = 2**m
#define VLP_ZOPC    (unsigned long)(0xe7L << 40 | 0xdfL << 0)   // V1 := |V2|,  element size = 2**m

// ADD
#define VA_ZOPC     (unsigned long)(0xe7L << 40 | 0xf3L << 0)   // V1 := V2 + V3, element size = 2**m
#define VACC_ZOPC   (unsigned long)(0xe7L << 40 | 0xf1L << 0)   // V1 := carry(V2 + V3), element size = 2**m

// SUB
#define VS_ZOPC     (unsigned long)(0xe7L << 40 | 0xf7L << 0)   // V1 := V2 - V3, element size = 2**m
#define VSCBI_ZOPC  (unsigned long)(0xe7L << 40 | 0xf5L << 0)   // V1 := borrow(V2 - V3), element size = 2**m

// MUL
#define VML_ZOPC    (unsigned long)(0xe7L << 40 | 0xa2L << 0)   // V1 := V2 * V3, element size = 2**m
#define VMH_ZOPC    (unsigned long)(0xe7L << 40 | 0xa3L << 0)   // V1 := V2 * V3, element size = 2**m
#define VMLH_ZOPC   (unsigned long)(0xe7L << 40 | 0xa1L << 0)   // V1 := V2 * V3, element size = 2**m, unsigned
#define VME_ZOPC    (unsigned long)(0xe7L << 40 | 0xa6L << 0)   // V1 := V2 * V3, element size = 2**m
#define VMLE_ZOPC   (unsigned long)(0xe7L << 40 | 0xa4L << 0)   // V1 := V2 * V3, element size = 2**m, unsigned
#define VMO_ZOPC    (unsigned long)(0xe7L << 40 | 0xa7L << 0)   // V1 := V2 * V3, element size = 2**m
#define VMLO_ZOPC   (unsigned long)(0xe7L << 40 | 0xa5L << 0)   // V1 := V2 * V3, element size = 2**m, unsigned

// MUL & ADD
#define VMAL_ZOPC   (unsigned long)(0xe7L << 40 | 0xaaL << 0)   // V1 := V2 * V3 + V4, element size = 2**m
#define VMAH_ZOPC   (unsigned long)(0xe7L << 40 | 0xabL << 0)   // V1 := V2 * V3 + V4, element size = 2**m
#define VMALH_ZOPC  (unsigned long)(0xe7L << 40 | 0xa9L << 0)   // V1 := V2 * V3 + V4, element size = 2**m, unsigned
#define VMAE_ZOPC   (unsigned long)(0xe7L << 40 | 0xaeL << 0)   // V1 := V2 * V3 + V4, element size = 2**m
#define VMALE_ZOPC  (unsigned long)(0xe7L << 40 | 0xacL << 0)   // V1 := V2 * V3 + V4, element size = 2**m, unsigned
#define VMAO_ZOPC   (unsigned long)(0xe7L << 40 | 0xafL << 0)   // V1 := V2 * V3 + V4, element size = 2**m
#define VMALO_ZOPC  (unsigned long)(0xe7L << 40 | 0xadL << 0)   // V1 := V2 * V3 + V4, element size = 2**m, unsigned

// Vector SUM
#define VSUM_ZOPC   (unsigned long)(0xe7L << 40 | 0x64L << 0)   // V1[j] := toFW(sum(V2[i]) + V3[j]), subelements: byte or HW
#define VSUMG_ZOPC  (unsigned long)(0xe7L << 40 | 0x65L << 0)   // V1[j] := toDW(sum(V2[i]) + V3[j]), subelements: HW or FW
#define VSUMQ_ZOPC  (unsigned long)(0xe7L << 40 | 0x67L << 0)   // V1[j] := toQW(sum(V2[i]) + V3[j]), subelements: FW or DW

// Average
#define VAVG_ZOPC   (unsigned long)(0xe7L << 40 | 0xf2L << 0)   // V1 := (V2+V3+1)/2, signed,   element size = 2**m
#define VAVGL_ZOPC  (unsigned long)(0xe7L << 40 | 0xf0L << 0)   // V1 := (V2+V3+1)/2, unsigned, element size = 2**m

// VECTOR Galois Field Multiply Sum
#define VGFM_ZOPC   (unsigned long)(0xe7L << 40 | 0xb4L << 0)
#define VGFMA_ZOPC  (unsigned long)(0xe7L << 40 | 0xbcL << 0)

//---<  Vector Logical Instructions  >---

// AND
#define VN_ZOPC     (unsigned long)(0xe7L << 40 | 0x68L << 0)   // V1 := V2 & V3,  element size = 2**m
#define VNC_ZOPC    (unsigned long)(0xe7L << 40 | 0x69L << 0)   // V1 := V2 & ~V3, element size = 2**m

// XOR
#define VX_ZOPC     (unsigned long)(0xe7L << 40 | 0x6dL << 0)   // V1 := V2 ^ V3,  element size = 2**m

// NOR
#define VNO_ZOPC    (unsigned long)(0xe7L << 40 | 0x6bL << 0)   // V1 := !(V2 | V3),  element size = 2**m

// OR
#define VO_ZOPC     (unsigned long)(0xe7L << 40 | 0x6aL << 0)   // V1 := V2 | V3,  element size = 2**m

// Comparison (element-wise)
#define VCEQ_ZOPC   (unsigned long)(0xe7L << 40 | 0xf8L << 0)   // V1 := (V2 == V3) ? 0xffff : 0x0000, element size = 2**m
#define VCH_ZOPC    (unsigned long)(0xe7L << 40 | 0xfbL << 0)   // V1 := (V2  > V3) ? 0xffff : 0x0000, element size = 2**m, signed
#define VCHL_ZOPC   (unsigned long)(0xe7L << 40 | 0xf9L << 0)   // V1 := (V2  > V3) ? 0xffff : 0x0000, element size = 2**m, unsigned

// Max/Min (element-wise)
#define VMX_ZOPC    (unsigned long)(0xe7L << 40 | 0xffL << 0)   // V1 := (V2 > V3) ? V2 : V3, element size = 2**m, signed
#define VMXL_ZOPC   (unsigned long)(0xe7L << 40 | 0xfdL << 0)   // V1 := (V2 > V3) ? V2 : V3, element size = 2**m, unsigned
#define VMN_ZOPC    (unsigned long)(0xe7L << 40 | 0xfeL << 0)   // V1 := (V2 < V3) ? V2 : V3, element size = 2**m, signed
#define VMNL_ZOPC   (unsigned long)(0xe7L << 40 | 0xfcL << 0)   // V1 := (V2 < V3) ? V2 : V3, element size = 2**m, unsigned

// Leading/Trailing Zeros, population count
#define VCLZ_ZOPC   (unsigned long)(0xe7L << 40 | 0x53L << 0)   // V1 := leadingzeros(V2),  element size = 2**m
#define VCTZ_ZOPC   (unsigned long)(0xe7L << 40 | 0x52L << 0)   // V1 := trailingzeros(V2), element size = 2**m
#define VPOPCT_ZOPC (unsigned long)(0xe7L << 40 | 0x50L << 0)   // V1 := popcount(V2), bytewise!!

// Rotate/Shift
#define VERLLV_ZOPC (unsigned long)(0xe7L << 40 | 0x73L << 0)   // V1 := rotateleft(V2), rotate count in V3 element
#define VERLL_ZOPC  (unsigned long)(0xe7L << 40 | 0x33L << 0)   // V1 := rotateleft(V3), rotate count from d2(b2).
#define VERIM_ZOPC  (unsigned long)(0xe7L << 40 | 0x72L << 0)   // Rotate then insert under mask. Read Principles of Operation!!

#define VESLV_ZOPC  (unsigned long)(0xe7L << 40 | 0x70L << 0)   // V1 := SLL(V2, V3), unsigned, element-wise
#define VESL_ZOPC   (unsigned long)(0xe7L << 40 | 0x30L << 0)   // V1 := SLL(V3), unsigned, shift count from d2(b2).

#define VESRAV_ZOPC (unsigned long)(0xe7L << 40 | 0x7AL << 0)   // V1 := SRA(V2, V3), signed, element-wise
#define VESRA_ZOPC  (unsigned long)(0xe7L << 40 | 0x3AL << 0)   // V1 := SRA(V3), signed, shift count from d2(b2).
#define VESRLV_ZOPC (unsigned long)(0xe7L << 40 | 0x78L << 0)   // V1 := SRL(V2, V3), unsigned, element-wise
#define VESRL_ZOPC  (unsigned long)(0xe7L << 40 | 0x38L << 0)   // V1 := SRL(V3), unsigned, shift count from d2(b2).

#define VSL_ZOPC    (unsigned long)(0xe7L << 40 | 0x74L << 0)   // V1 := SLL(V2), unsigned, bit-count
#define VSLB_ZOPC   (unsigned long)(0xe7L << 40 | 0x75L << 0)   // V1 := SLL(V2), unsigned, byte-count
#define VSLDB_ZOPC  (unsigned long)(0xe7L << 40 | 0x77L << 0)   // V1 := SLL((V2,V3)), unsigned, byte-count

#define VSRA_ZOPC   (unsigned long)(0xe7L << 40 | 0x7eL << 0)   // V1 := SRA(V2), signed, bit-count
#define VSRAB_ZOPC  (unsigned long)(0xe7L << 40 | 0x7fL << 0)   // V1 := SRA(V2), signed, byte-count
#define VSRL_ZOPC   (unsigned long)(0xe7L << 40 | 0x7cL << 0)   // V1 := SRL(V2), unsigned, bit-count
#define VSRLB_ZOPC  (unsigned long)(0xe7L << 40 | 0x7dL << 0)   // V1 := SRL(V2), unsigned, byte-count

// Test under Mask
#define VTM_ZOPC    (unsigned long)(0xe7L << 40 | 0xd8L << 0)   // Like TM, set CC according to state of selected bits.

//---<  Vector String Instructions  >---
#define VFAE_ZOPC   (unsigned long)(0xe7L << 40 | 0x82L << 0)   // Find any element
#define VFEE_ZOPC   (unsigned long)(0xe7L << 40 | 0x80L << 0)   // Find element equal
#define VFENE_ZOPC  (unsigned long)(0xe7L << 40 | 0x81L << 0)   // Find element not equal
#define VSTRC_ZOPC  (unsigned long)(0xe7L << 40 | 0x8aL << 0)   // String range compare
#define VISTR_ZOPC  (unsigned long)(0xe7L << 40 | 0x5cL << 0)   // Isolate String


//--------------------------------
//--  Miscellaneous Operations  --
//--------------------------------

// Execute
#define EX_ZOPC     (unsigned  int)(68L << 24)
#define EXRL_ZOPC   (unsigned long)(0xc6L << 40 | 0x00L << 32)  // z10

// Compare and Swap
#define CS_ZOPC     (unsigned  int)(0xba << 24)
#define CSY_ZOPC    (unsigned long)(0xebL << 40 | 0x14L)
#define CSG_ZOPC    (unsigned long)(0xebL << 40 | 0x30L)

// Interlocked-Update
#define LAA_ZOPC    (unsigned long)(0xebL << 40 | 0xf8L)         // z196
#define LAAG_ZOPC   (unsigned long)(0xebL << 40 | 0xe8L)         // z196
#define LAAL_ZOPC   (unsigned long)(0xebL << 40 | 0xfaL)         // z196
#define LAALG_ZOPC  (unsigned long)(0xebL << 40 | 0xeaL)         // z196
#define LAN_ZOPC    (unsigned long)(0xebL << 40 | 0xf4L)         // z196
#define LANG_ZOPC   (unsigned long)(0xebL << 40 | 0xe4L)         // z196
#define LAX_ZOPC    (unsigned long)(0xebL << 40 | 0xf7L)         // z196
#define LAXG_ZOPC   (unsigned long)(0xebL << 40 | 0xe7L)         // z196
#define LAO_ZOPC    (unsigned long)(0xebL << 40 | 0xf6L)         // z196
#define LAOG_ZOPC   (unsigned long)(0xebL << 40 | 0xe6L)         // z196

// System Functions
#define STCKF_ZOPC  (unsigned  int)(0xb2 << 24 | 0x7c << 16)
#define STFLE_ZOPC  (unsigned  int)(0xb2 << 24 | 0xb0 << 16)
#define ECTG_ZOPC   (unsigned long)(0xc8L <<40 | 0x01L << 32)    // z10
#define ECAG_ZOPC   (unsigned long)(0xebL <<40 | 0x4cL)          // z10

// Execution Prediction
#define PFD_ZOPC    (unsigned long)(0xe3L <<40 | 0x36L)          // z10
#define PFDRL_ZOPC  (unsigned long)(0xc6L <<40 | 0x02L << 32)    // z10
#define BPP_ZOPC    (unsigned long)(0xc7L <<40)                  // branch prediction preload  -- EC12
#define BPRP_ZOPC   (unsigned long)(0xc5L <<40)                  // branch prediction preload  -- EC12

// Transaction Control
#define TBEGIN_ZOPC  (unsigned long)(0xe560L << 32)              // tx begin                   -- EC12
#define TBEGINC_ZOPC (unsigned long)(0xe561L << 32)              // tx begin (constrained)     -- EC12
#define TEND_ZOPC    (unsigned  int)(0xb2f8  << 16)              // tx end                     -- EC12
#define TABORT_ZOPC  (unsigned  int)(0xb2fc  << 16)              // tx abort                   -- EC12
#define ETND_ZOPC    (unsigned  int)(0xb2ec  << 16)              // tx nesting depth           -- EC12
#define PPA_ZOPC     (unsigned  int)(0xb2e8  << 16)              // tx processor assist        -- EC12

// Crypto and Checksum
#define CKSM_ZOPC   (unsigned  int)(0xb2 << 24 | 0x41 << 16)     // checksum. This is NOT CRC32
#define KM_ZOPC     (unsigned  int)(0xb9 << 24 | 0x2e << 16)     // cipher
#define KMC_ZOPC    (unsigned  int)(0xb9 << 24 | 0x2f << 16)     // cipher
#define KMA_ZOPC    (unsigned  int)(0xb9 << 24 | 0x29 << 16)     // cipher
#define KMF_ZOPC    (unsigned  int)(0xb9 << 24 | 0x2a << 16)     // cipher
#define KMCTR_ZOPC  (unsigned  int)(0xb9 << 24 | 0x2d << 16)     // cipher
#define KMO_ZOPC    (unsigned  int)(0xb9 << 24 | 0x2b << 16)     // cipher
#define KIMD_ZOPC   (unsigned  int)(0xb9 << 24 | 0x3e << 16)     // SHA (msg digest)
#define KLMD_ZOPC   (unsigned  int)(0xb9 << 24 | 0x3f << 16)     // SHA (msg digest)
#define KMAC_ZOPC   (unsigned  int)(0xb9 << 24 | 0x1e << 16)     // Message Authentication Code

// Various
#define TCEB_ZOPC   (unsigned long)(237L << 40 | 16)
#define TCDB_ZOPC   (unsigned long)(237L << 40 | 17)
#define TAM_ZOPC    (unsigned long)(267)

#define FLOGR_ZOPC  (unsigned  int)(0xb9 << 24 | 0x83 << 16)
#define POPCNT_ZOPC (unsigned  int)(0xb9e1 << 16)
#define AHHHR_ZOPC  (unsigned  int)(0xb9c8 << 16)
#define AHHLR_ZOPC  (unsigned  int)(0xb9d8 << 16)


// OpCode field masks

#define RI_MASK     (unsigned  int)(0xff  << 24 | 0x0f << 16)
#define RRE_MASK    (unsigned  int)(0xff  << 24 | 0xff << 16)
#define RSI_MASK    (unsigned  int)(0xff  << 24)
#define RIE_MASK    (unsigned long)(0xffL << 40 | 0xffL)
#define RIL_MASK    (unsigned long)(0xffL << 40 | 0x0fL << 32)

#define BASR_MASK   (unsigned  int)(0xff << 8)
#define BCR_MASK    (unsigned  int)(0xff << 8)
#define BRC_MASK    (unsigned  int)(0xff << 24 | 0x0f << 16)
#define LGHI_MASK   (unsigned  int)(0xff << 24 | 0x0f << 16)
#define LLI_MASK    (unsigned  int)(0xff << 24 | 0x0f << 16)
#define II_MASK     (unsigned  int)(0xff << 24 | 0x0f << 16)
#define LLIF_MASK   (unsigned long)(0xffL << 40 | 0x0fL << 32)
#define IIF_MASK    (unsigned long)(0xffL << 40 | 0x0fL << 32)
#define BRASL_MASK  (unsigned long)(0xffL << 40 | 0x0fL << 32)
#define TM_MASK     (unsigned  int)(0xff << 24)
#define TMY_MASK    (unsigned long)(0xffL << 40 | 0xffL)
#define LB_MASK     (unsigned long)(0xffL << 40 | 0xffL)
#define LH_MASK     (unsigned int)(0xff << 24)
#define L_MASK      (unsigned int)(0xff << 24)
#define LY_MASK     (unsigned long)(0xffL << 40 | 0xffL)
#define LG_MASK     (unsigned long)(0xffL << 40 | 0xffL)
#define LLGH_MASK   (unsigned long)(0xffL << 40 | 0xffL)
#define LLGF_MASK   (unsigned long)(0xffL << 40 | 0xffL)
#define SLAG_MASK   (unsigned long)(0xffL << 40 | 0xffL)
#define LARL_MASK   (unsigned long)(0xff0fL << 32)
#define LGRL_MASK   (unsigned long)(0xff0fL << 32)
#define LE_MASK     (unsigned int)(0xff << 24)
#define LD_MASK     (unsigned int)(0xff << 24)
#define ST_MASK     (unsigned int)(0xff << 24)
#define STC_MASK    (unsigned int)(0xff << 24)
#define STG_MASK    (unsigned long)(0xffL << 40 | 0xffL)
#define STH_MASK    (unsigned int)(0xff << 24)
#define STE_MASK    (unsigned int)(0xff << 24)
#define STD_MASK    (unsigned int)(0xff << 24)
#define CMPBRANCH_MASK (unsigned long)(0xffL << 40 | 0xffL)
#define REL_LONG_MASK  (unsigned long)(0xff0fL << 32)

 public:
  // Condition code masks. Details:
  // - Mask bit#3 must be zero for all compare and branch/trap instructions to ensure
  //   future compatibility.
  // - For all arithmetic instructions which set the condition code, mask bit#3
  //   indicates overflow ("unordered" in float operations).
  // - "unordered" float comparison results have to be treated as low.
  // - When overflow/unordered is detected, none of the branch conditions is true,
  //   except for bcondOverflow/bcondNotOrdered and bcondAlways.
  // - For INT comparisons, the inverse condition can be calculated as (14-cond).
  // - For FLOAT comparisons, the inverse condition can be calculated as (15-cond).
  enum branch_condition {
    bcondNever       =  0,
    bcondAlways      = 15,

    // Specific names. Make use of lightweight sync.
    // Full and lightweight sync operation.
    bcondFullSync    = 15,
    bcondLightSync   = 14,
    bcondNop         =  0,

    // arithmetic compare instructions
    // arithmetic load and test, insert instructions
    // Mask bit#3 must be zero for future compatibility.
    bcondEqual       =  8,
    bcondNotEqual    =  6,
    bcondLow         =  4,
    bcondNotLow      = 10,
    bcondHigh        =  2,
    bcondNotHigh     = 12,
    // arithmetic calculation instructions
    // Mask bit#3 indicates overflow if detected by instr.
    // Mask bit#3 = 0 (overflow is not handled by compiler).
    bcondOverflow    =  1,
    bcondNotOverflow = 14,
    bcondZero        =  bcondEqual,
    bcondNotZero     =  bcondNotEqual,
    bcondNegative    =  bcondLow,
    bcondNotNegative =  bcondNotLow,
    bcondPositive    =  bcondHigh,
    bcondNotPositive =  bcondNotHigh,
    bcondNotOrdered  =  1,  // float comparisons
    bcondOrdered     = 14,  // float comparisons
    bcondLowOrNotOrdered  =  bcondLow  | bcondNotOrdered,  // float comparisons
    bcondHighOrNotOrdered =  bcondHigh | bcondNotOrdered,  // float comparisons
    bcondNotLowOrNotOrdered   =  bcondNotLow   | bcondNotOrdered,  // float comparisons
    bcondNotHighOrNotOrdered  =  bcondNotHigh  | bcondNotOrdered,  // float comparisons
    bcondNotEqualOrNotOrdered =  bcondNotEqual | bcondNotOrdered,  // float comparisons
    // unsigned arithmetic calculation instructions
    // Mask bit#0 is not used by these instructions.
    // There is no indication of overflow for these instr.
    bcondLogZero_NoCarry     =  8,
    bcondLogZero_Carry       =  2,
    // bcondLogZero_Borrow      =  8,  // This CC is never generated.
    bcondLogZero_NoBorrow    =  2,
    bcondLogZero             =  bcondLogZero_Carry | bcondLogZero_NoCarry,
    bcondLogNotZero_NoCarry  =  4,
    bcondLogNotZero_Carry    =  1,
    bcondLogNotZero_Borrow   =  4,
    bcondLogNotZero_NoBorrow =  1,
    bcondLogNotZero          =  bcondLogNotZero_Carry | bcondLogNotZero_NoCarry,
    bcondLogCarry            =  bcondLogZero_Carry | bcondLogNotZero_Carry,
    bcondLogBorrow           =  /* bcondLogZero_Borrow | */ bcondLogNotZero_Borrow,
    // Vector compare instructions
    bcondVAlltrue    =  8,  // All  vector elements evaluate true
    bcondVMixed      =  4,  // Some vector elements evaluate true, some false
    bcondVAllfalse   =  1,  // All  vector elements evaluate false
    // string search instructions
    bcondFound       =  4,
    bcondNotFound    =  2,
    bcondInterrupted =  1,
    // bit test instructions
    bcondAllZero     =  8,
    bcondMixed       =  6,
    bcondAllOne      =  1,
    bcondNotAllZero  =  7 // for tmll
  };

  enum Condition {
    // z/Architecture
    negative         = 0,
    less             = 0,
    positive         = 1,
    greater          = 1,
    zero             = 2,
    equal            = 2,
    summary_overflow = 3,
  };

  // Rounding mode for float-2-int conversions.
  enum RoundingMode {
    current_mode      = 0,   // Mode taken from FPC register.
    biased_to_nearest = 1,
    to_nearest        = 4,
    to_zero           = 5,
    to_plus_infinity  = 6,
    to_minus_infinity = 7
  };

  // Vector Register Element Type.
  enum VRegElemType {
    VRET_BYTE   = 0,
    VRET_HW     = 1,
    VRET_FW     = 2,
    VRET_DW     = 3,
    VRET_QW     = 4
  };

  // Vector Operation Result Control.
  //   This is a set of flags used in some vector instructions to control
  //   the result (side) effects of instruction execution.
  enum VOpRC {
    VOPRC_CCSET    = 0b0001, // set the CC.
    VOPRC_CCIGN    = 0b0000, // ignore, don't set CC.
    VOPRC_ZS       = 0b0010, // Zero Search. Additional, elementwise, comparison against zero.
    VOPRC_NOZS     = 0b0000, // No Zero Search.
    VOPRC_RTBYTEIX = 0b0100, // generate byte index to lowest element with true comparison.
    VOPRC_RTBITVEC = 0b0000, // generate bit vector, all 1s for true, all 0s for false element comparisons.
    VOPRC_INVERT   = 0b1000, // invert comparison results.
    VOPRC_NOINVERT = 0b0000  // use comparison results as is, do not invert.
  };

  // Inverse condition code, i.e. determine "15 - cc" for a given condition code cc.
  static branch_condition inverse_condition(branch_condition cc);
  static branch_condition inverse_float_condition(branch_condition cc);


  //-----------------------------------------------
  // instruction property getter methods
  //-----------------------------------------------

  // Calculate length of instruction.
  static unsigned int instr_len(unsigned char *instr);

  // Longest instructions are 6 bytes on z/Architecture.
  static unsigned int instr_maxlen() { return 6; }

  // Average instruction is 4 bytes on z/Architecture (just a guess).
  static unsigned int instr_avglen() { return 4; }

  // Shortest instructions are 2 bytes on z/Architecture.
  static unsigned int instr_minlen() { return 2; }

  // Move instruction at pc right-justified into passed long int.
  // Return instr len in bytes as function result.
  static unsigned int get_instruction(unsigned char *pc, unsigned long *instr);

  // Move instruction in passed (long int) into storage at pc.
  // This code is _NOT_ MT-safe!!
  static void set_instruction(unsigned char *pc, unsigned long instr, unsigned int len) {
    memcpy(pc, ((unsigned char *)&instr)+sizeof(unsigned long)-len, len);
  }


  //------------------------------------------
  // instruction field test methods
  //------------------------------------------

  // Only used once in s390.ad to implement Matcher::is_short_branch_offset().
  static bool is_within_range_of_RelAddr16(address target, address origin) {
    return RelAddr::is_in_range_of_RelAddr16(target, origin);
  }


  //----------------------------------
  // some diagnostic output
  //----------------------------------

  static void print_dbg_msg(outputStream* out, unsigned long inst, const char* msg, int ilen) PRODUCT_RETURN;
  static void dump_code_range(outputStream* out, address pc, const unsigned int range, const char* msg = " ") PRODUCT_RETURN;

 protected:

  //-------------------------------------------------------
  // instruction field helper methods (internal)
  //-------------------------------------------------------

  // Return a mask of 1s between hi_bit and lo_bit (inclusive).
  static long fmask(unsigned int hi_bit, unsigned int lo_bit) {
    assert(hi_bit >= lo_bit && hi_bit < 48, "bad bits");
    return ((1L<<(hi_bit-lo_bit+1)) - 1) << lo_bit;
  }

  // extract u_field
  // unsigned value
  static long inv_u_field(long x, int hi_bit, int lo_bit) {
    return (x & fmask(hi_bit, lo_bit)) >> lo_bit;
  }

  // extract s_field
  // Signed value, may need sign extension.
  static long inv_s_field(long x, int hi_bit, int lo_bit) {
    x = inv_u_field(x, hi_bit, lo_bit);
    // Highest extracted bit set -> sign extension.
    return (x >= (1L<<(hi_bit-lo_bit)) ? x | ((-1L)<<(hi_bit-lo_bit)) : x);
  }

  // Extract primary opcode from instruction.
  static int z_inv_op(int  x) { return inv_u_field(x, 31, 24); }
  static int z_inv_op(long x) { return inv_u_field(x, 47, 40); }

  static int inv_reg( long x, int s, int len) { return inv_u_field(x, (len-s)-1, (len-s)-4); }  // Regs are encoded in 4 bits.
  static int inv_mask(long x, int s, int len) { return inv_u_field(x, (len-s)-1, (len-s)-8); }  // Mask is 8 bits long.
  static int inv_simm16_48(long x) { return (inv_s_field(x, 31, 16)); }                         // 6-byte instructions only
  static int inv_simm16(long x)    { return (inv_s_field(x, 15,  0)); }                         // 4-byte instructions only
  static int inv_simm20(long x)    { return (inv_u_field(x, 27, 16) |                           // 6-byte instructions only
                                             inv_s_field(x, 15, 8)<<12); }
  static int inv_simm32(long x)    { return (inv_s_field(x, 31,  0)); }                         // 6-byte instructions only
  static int inv_uimm12(long x)    { return (inv_u_field(x, 11,  0)); }                         // 4-byte instructions only

  // Encode u_field from long value.
  static long u_field(long x, int hi_bit, int lo_bit) {
    long r = x << lo_bit;
    assert((r & ~fmask(hi_bit, lo_bit))   == 0, "value out of range");
    assert(inv_u_field(r, hi_bit, lo_bit) == x, "just checking");
    return r;
  }

  static int64_t rsmask_48( Address a) { assert(a.is_RSform(),  "bad address format"); return rsmask_48( a.disp12(), a.base()); }
  static int64_t rxmask_48( Address a) {      if (a.is_RXform())  { return rxmask_48( a.disp12(), a.index(), a.base()); }
                                         else if (a.is_RSform())  { return rsmask_48( a.disp12(),            a.base()); }
                                         else                     { guarantee(false, "bad address format");  return 0;  }
                                       }
  static int64_t rsymask_48(Address a) { assert(a.is_RSYform(), "bad address format"); return rsymask_48(a.disp20(), a.base()); }
  static int64_t rxymask_48(Address a) {      if (a.is_RXYform()) { return rxymask_48( a.disp20(), a.index(), a.base()); }
                                         else if (a.is_RSYform()) { return rsymask_48( a.disp20(),            a.base()); }
                                         else                     { guarantee(false, "bad address format");  return 0;   }
                                       }

  static int64_t rsmask_48( int64_t d2, Register b2)              { return uimm12(d2, 20, 48)                   | regz(b2, 16, 48); }
  static int64_t rxmask_48( int64_t d2, Register x2, Register b2) { return uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48); }
  static int64_t rsymask_48(int64_t d2, Register b2)              { return simm20(d2)                           | regz(b2, 16, 48); }
  static int64_t rxymask_48(int64_t d2, Register x2, Register b2) { return simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48); }

  // Address calculated from d12(vx,b) - vx is vector index register.
  static int64_t rvmask_48( int64_t d2, VectorRegister x2, Register b2) { return uimm12(d2, 20, 48) | vreg(x2, 12) | regz(b2, 16, 48); }

  static int64_t vreg_mask(VectorRegister v, int pos) {
    return vreg(v, pos) | v->RXB_mask(pos);
  }

  // Vector Element Size Control. 4-bit field which indicates the size of the vector elements.
  static int64_t vesc_mask(int64_t size, int min_size, int max_size, int pos) {
    // min_size - minimum element size. Not all instructions support element sizes beginning with "byte".
    // max_size - maximum element size. Not all instructions support element sizes up to "QW".
    assert((min_size <= size) && (size <= max_size), "element size control out of range");
    return uimm4(size, pos, 48);
  }

  // Vector Element IndeX. 4-bit field which indexes the target vector element.
  static int64_t veix_mask(int64_t ix, int el_size, int pos) {
    // el_size - size of the vector element. This is a VRegElemType enum value.
    // ix      - vector element index.
    int max_ix = -1;
    switch (el_size) {
      case VRET_BYTE: max_ix = 15; break;
      case VRET_HW:   max_ix =  7; break;
      case VRET_FW:   max_ix =  3; break;
      case VRET_DW:   max_ix =  1; break;
      case VRET_QW:   max_ix =  0; break;
      default:        guarantee(false, "bad vector element size %d", el_size); break;
    }
    assert((0 <= ix) && (ix <= max_ix), "element size out of range (0 <= %ld <= %d)", ix, max_ix);
    return uimm4(ix, pos, 48);
  }

  // Vector Operation Result Control. 4-bit field.
  static int64_t voprc_any(int64_t flags, int pos, int64_t allowed_flags = 0b1111) {
    assert((flags & allowed_flags) == flags, "Invalid VOPRC_* flag combination: %d", (int)flags);
    return uimm4(flags, pos, 48);
  }

  // Vector Operation Result Control. Condition code setting.
  static int64_t voprc_ccmask(int64_t flags, int pos) {
    return voprc_any(flags, pos, VOPRC_CCIGN | VOPRC_CCSET);
  }

 public:

  //--------------------------------------------------
  // instruction field construction methods
  //--------------------------------------------------

  // Compute relative address (32 bit) for branch.
  // Only used once in nativeInst_s390.cpp.
  static intptr_t z_pcrel_off(address dest, address pc) {
    return RelAddr::pcrel_off32(dest, pc);
  }

  // Extract 20-bit signed displacement.
  // Only used in disassembler_s390.cpp for temp enhancements.
  static int inv_simm20_xx(address iLoc) {
    unsigned long instr = 0;
    unsigned long iLen  = get_instruction(iLoc, &instr);
    return inv_simm20(instr);
  }

  // unsigned immediate, in low bits, nbits long
  static long uimm(long x, int nbits) {
    assert(Immediate::is_uimm(x, nbits), "unsigned constant out of range");
    return x & fmask(nbits - 1, 0);
  }

  // Cast '1' to long to avoid sign extension if nbits = 32.
  // signed immediate, in low bits, nbits long
  static long simm(long x, int nbits) {
    assert(Immediate::is_simm(x, nbits), "value out of range");
    return x & fmask(nbits - 1, 0);
  }

  static long imm(int64_t x, int nbits) {
    // Assert that x can be represented with nbits bits ignoring the sign bits,
    // i.e. the more higher bits should all be 0 or 1.
    assert((x >> nbits) == 0 || (x >> nbits) == -1, "value out of range");
    return x & fmask(nbits-1, 0);
  }

  // A 20-bit displacement is only in instructions of the
  // RSY, RXY, or SIY format. In these instructions, the D
  // field consists of a DL (low) field in bit positions 20-31
  // and of a DH (high) field in bit positions 32-39. The
  // value of the displacement is formed by appending the
  // contents of the DH field to the left of the contents of
  // the DL field.
  static long simm20(int64_t ui20) {
    assert(Immediate::is_simm(ui20, 20), "value out of range");
    return ( ((ui20        & 0xfffL) << (48-32)) |  // DL
            (((ui20 >> 12) &  0xffL) << (48-40)));  // DH
  }

  static long reg(Register r, int s, int len)  { return u_field(r->encoding(), (len-s)-1, (len-s)-4); }
  static long reg(int r, int s, int len)       { return u_field(r,             (len-s)-1, (len-s)-4); }
  static long regt(Register r, int s, int len) { return reg(r, s, len); }
  static long regz(Register r, int s, int len) { assert(r != Z_R0, "cannot use register R0 in memory access"); return reg(r, s, len); }

  static long uimm4( int64_t ui4,  int s, int len) { return uimm(ui4,   4) << (len-s-4);  }
  static long uimm6( int64_t ui6,  int s, int len) { return uimm(ui6,   6) << (len-s-6);  }
  static long uimm8( int64_t ui8,  int s, int len) { return uimm(ui8,   8) << (len-s-8);  }
  static long uimm12(int64_t ui12, int s, int len) { return uimm(ui12, 12) << (len-s-12); }
  static long uimm16(int64_t ui16, int s, int len) { return uimm(ui16, 16) << (len-s-16); }
  static long uimm32(int64_t ui32, int s, int len) { return uimm((unsigned)ui32, 32) << (len-s-32); } // prevent sign extension

  static long simm8( int64_t si8,  int s, int len) { return simm(si8,   8) << (len-s-8);  }
  static long simm12(int64_t si12, int s, int len) { return simm(si12, 12) << (len-s-12); }
  static long simm16(int64_t si16, int s, int len) { return simm(si16, 16) << (len-s-16); }
  static long simm24(int64_t si24, int s, int len) { return simm(si24, 24) << (len-s-24); }
  static long simm32(int64_t si32, int s, int len) { return simm(si32, 32) << (len-s-32); }

  static long imm8( int64_t i8,  int s, int len)   { return imm(i8,   8) << (len-s-8);  }
  static long imm12(int64_t i12, int s, int len)   { return imm(i12, 12) << (len-s-12); }
  static long imm16(int64_t i16, int s, int len)   { return imm(i16, 16) << (len-s-16); }
  static long imm24(int64_t i24, int s, int len)   { return imm(i24, 24) << (len-s-24); }
  static long imm32(int64_t i32, int s, int len)   { return imm(i32, 32) << (len-s-32); }

  static long vreg(VectorRegister v, int pos)      { const int len = 48; return u_field(v->encoding()&0x0f, (len-pos)-1, (len-pos)-4) | v->RXB_mask(pos); }

  static long fregt(FloatRegister r, int s, int len) { return freg(r,s,len); }
  static long freg( FloatRegister r, int s, int len) { return u_field(r->encoding(), (len-s)-1, (len-s)-4); }

  // Rounding mode for float-2-int conversions.
  static long rounding_mode(RoundingMode m, int s, int len) {
    assert(m != 2 && m != 3, "invalid mode");
    return uimm(m, 4) << (len-s-4);
  }

  //--------------------------------------------
  // instruction field getter methods
  //--------------------------------------------

  static int get_imm32(address a, int instruction_number) {
    int imm;
    int *p =((int *)(a + 2 + 6 * instruction_number));
    imm = *p;
    return imm;
  }

  static short get_imm16(address a, int instruction_number) {
    short imm;
    short *p =((short *)a) + 2 * instruction_number + 1;
    imm = *p;
    return imm;
  }


  //--------------------------------------------
  // instruction field setter methods
  //--------------------------------------------

  static void set_imm32(address a, int64_t s) {
    assert(Immediate::is_simm32(s) || Immediate::is_uimm32(s), "to big");
    int* p = (int *) (a + 2);
    *p = s;
  }

  static void set_imm16(int* instr, int64_t s) {
    assert(Immediate::is_simm16(s) || Immediate::is_uimm16(s), "to big");
    short* p = ((short *)instr) + 1;
    *p = s;
  }

 public:

  static unsigned int align(unsigned int x, unsigned int a) { return ((x + (a - 1)) & ~(a - 1)); }
  static bool    is_aligned(unsigned int x, unsigned int a) { return (0 == x % a); }

  inline void emit_16(int x);
  inline void emit_32(int x);
  inline void emit_48(long x);

  // Compare and control flow instructions
  // =====================================

  // See also commodity routines compare64_and_branch(), compare32_and_branch().

  // compare instructions
  // compare register
  inline void z_cr(  Register r1, Register r2);                          // compare (r1, r2)        ; int32
  inline void z_cgr( Register r1, Register r2);                          // compare (r1, r2)        ; int64
  inline void z_cgfr(Register r1, Register r2);                          // compare (r1, r2)        ; int64 <--> int32
   // compare immediate
  inline void z_chi( Register r1, int64_t i2);                           // compare (r1, i2_imm16)  ; int32
  inline void z_cfi( Register r1, int64_t i2);                           // compare (r1, i2_imm32)  ; int32
  inline void z_cghi(Register r1, int64_t i2);                           // compare (r1, i2_imm16)  ; int64
  inline void z_cgfi(Register r1, int64_t i2);                           // compare (r1, i2_imm32)  ; int64
   // compare memory
  inline void z_ch(  Register r1, const Address &a);                     // compare (r1, *(a))               ; int32 <--> int16
  inline void z_ch(  Register r1, int64_t d2, Register x2, Register b2); // compare (r1, *(d2_uimm12+x2+b2)) ; int32 <--> int16
  inline void z_c(   Register r1, const Address &a);                     // compare (r1, *(a))               ; int32
  inline void z_c(   Register r1, int64_t d2, Register x2, Register b2); // compare (r1, *(d2_uimm12+x2+b2)) ; int32
  inline void z_cy(  Register r1, int64_t d2, Register x2, Register b2); // compare (r1, *(d2_uimm20+x2+b2)) ; int32
  inline void z_cy(  Register r1, int64_t d2, Register b2);              // compare (r1, *(d2_uimm20+x2+b2)) ; int32
  inline void z_cy(  Register r1, const Address& a);                     // compare (r1, *(a))               ; int32
   //inline void z_cgf(Register r1,const Address &a);                    // compare (r1, *(a))               ; int64 <--> int32
   //inline void z_cgf(Register r1,int64_t d2, Register x2, Register b2);// compare (r1, *(d2_uimm12+x2+b2)) ; int64 <--> int32
  inline void z_cg(  Register r1, const Address &a);                     // compare (r1, *(a))               ; int64
  inline void z_cg(  Register r1, int64_t d2, Register x2, Register b2); // compare (r1, *(d2_imm20+x2+b2))  ; int64

   // compare logical instructions
   // compare register
  inline void z_clr(  Register r1, Register r2);                         // compare (r1, r2)                 ; uint32
  inline void z_clgr( Register r1, Register r2);                         // compare (r1, r2)                 ; uint64
   // compare immediate
  inline void z_clfi( Register r1, int64_t i2);                          // compare (r1, i2_uimm32)          ; uint32
  inline void z_clgfi(Register r1, int64_t i2);                          // compare (r1, i2_uimm32)          ; uint64
  inline void z_cl(   Register r1, const Address &a);                    // compare (r1, *(a)                ; uint32
  inline void z_cl(   Register r1, int64_t d2, Register x2, Register b2);// compare (r1, *(d2_uimm12+x2+b2)  ; uint32
  inline void z_cly(  Register r1, int64_t d2, Register x2, Register b2);// compare (r1, *(d2_uimm20+x2+b2)) ; uint32
  inline void z_cly(  Register r1, int64_t d2, Register b2);             // compare (r1, *(d2_uimm20+x2+b2)) ; uint32
  inline void z_cly(  Register r1, const Address& a);                    // compare (r1, *(a))               ; uint32
  inline void z_clg(  Register r1, const Address &a);                    // compare (r1, *(a)                ; uint64
  inline void z_clg(  Register r1, int64_t d2, Register x2, Register b2);// compare (r1, *(d2_imm20+x2+b2)   ; uint64

  // test under mask
  inline void z_tmll(Register r1, int64_t i2);           // test under mask, see docu
  inline void z_tmlh(Register r1, int64_t i2);           // test under mask, see docu
  inline void z_tmhl(Register r1, int64_t i2);           // test under mask, see docu
  inline void z_tmhh(Register r1, int64_t i2);           // test under mask, see docu

  // branch instructions
  inline void z_bc(  branch_condition m1, int64_t d2, Register x2, Register b2);// branch  m1 ? pc = (d2_uimm12+x2+b2)
  inline void z_bcr( branch_condition m1, Register r2);                         // branch (m1 && r2!=R0) ? pc = r2
  inline void z_brc( branch_condition i1, int64_t i2);                          // branch  i1 ? pc = pc + i2_imm16
  inline void z_brc( branch_condition i1, address a);                           // branch  i1 ? pc = a
  inline void z_brc( branch_condition i1, Label& L);                            // branch  i1 ? pc = Label
  //inline void z_brcl(branch_condition i1, int64_t i2);                        // branch  i1 ? pc = pc + i2_imm32
  inline void z_brcl(branch_condition i1, address a);                           // branch  i1 ? pc = a
  inline void z_brcl(branch_condition i1, Label& L);                            // branch  i1 ? pc = Label
  inline void z_bctgr(Register r1, Register r2);         // branch on count r1 -= 1; (r1!=0) ? pc = r2  ; r1 is int64

  // branch unconditional / always
  inline void z_br(Register r2);                         // branch to r2, nop if r2 == Z_R0


  // See also commodity routines compare64_and_branch(), compare32_and_branch().
  // signed comparison and branch
  inline void z_crb( Register r1, Register r2, branch_condition m3, int64_t d4, Register b4); // (r1 m3 r2) ? goto b4+d4      ; int32  -- z10
  inline void z_cgrb(Register r1, Register r2, branch_condition m3, int64_t d4, Register b4); // (r1 m3 r2) ? goto b4+d4      ; int64  -- z10
  inline void z_crj( Register r1, Register r2, branch_condition m3, Label& L);                // (r1 m3 r2) ? goto L          ; int32  -- z10
  inline void z_crj( Register r1, Register r2, branch_condition m3, address a4);              // (r1 m3 r2) ? goto (pc+a4<<1) ; int32  -- z10
  inline void z_cgrj(Register r1, Register r2, branch_condition m3, Label& L);                // (r1 m3 r2) ? goto L          ; int64  -- z10
  inline void z_cgrj(Register r1, Register r2, branch_condition m3, address a4);              // (r1 m3 r2) ? goto (pc+a4<<1) ; int64  -- z10
  inline void z_cib( Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4);  // (r1 m3 i2_imm8) ? goto b4+d4      ; int32  -- z10
  inline void z_cgib(Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4);  // (r1 m3 i2_imm8) ? goto b4+d4      ; int64  -- z10
  inline void z_cij( Register r1, int64_t i2, branch_condition m3, Label& L);                 // (r1 m3 i2_imm8) ? goto L          ; int32  -- z10
  inline void z_cij( Register r1, int64_t i2, branch_condition m3, address a4);               // (r1 m3 i2_imm8) ? goto (pc+a4<<1) ; int32  -- z10
  inline void z_cgij(Register r1, int64_t i2, branch_condition m3, Label& L);                 // (r1 m3 i2_imm8) ? goto L          ; int64  -- z10
  inline void z_cgij(Register r1, int64_t i2, branch_condition m3, address a4);               // (r1 m3 i2_imm8) ? goto (pc+a4<<1) ; int64  -- z10
  // unsigned comparison and branch
  inline void z_clrb( Register r1, Register r2, branch_condition m3, int64_t d4, Register b4);// (r1 m3 r2) ? goto b4+d4      ; uint32  -- z10
  inline void z_clgrb(Register r1, Register r2, branch_condition m3, int64_t d4, Register b4);// (r1 m3 r2) ? goto b4+d4      ; uint64  -- z10
  inline void z_clrj( Register r1, Register r2, branch_condition m3, Label& L);               // (r1 m3 r2) ? goto L          ; uint32  -- z10
  inline void z_clrj( Register r1, Register r2, branch_condition m3, address a4);             // (r1 m3 r2) ? goto (pc+a4<<1) ; uint32  -- z10
  inline void z_clgrj(Register r1, Register r2, branch_condition m3, Label& L);               // (r1 m3 r2) ? goto L          ; uint64  -- z10
  inline void z_clgrj(Register r1, Register r2, branch_condition m3, address a4);             // (r1 m3 r2) ? goto (pc+a4<<1) ; uint64  -- z10
  inline void z_clib( Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4); // (r1 m3 i2_uimm8) ? goto b4+d4      ; uint32  -- z10
  inline void z_clgib(Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4); // (r1 m3 i2_uimm8) ? goto b4+d4      ; uint64  -- z10
  inline void z_clij( Register r1, int64_t i2, branch_condition m3, Label& L);                // (r1 m3 i2_uimm8) ? goto L          ; uint32  -- z10
  inline void z_clij( Register r1, int64_t i2, branch_condition m3, address a4);              // (r1 m3 i2_uimm8) ? goto (pc+a4<<1) ; uint32  -- z10
  inline void z_clgij(Register r1, int64_t i2, branch_condition m3, Label& L);                // (r1 m3 i2_uimm8) ? goto L          ; uint64  -- z10
  inline void z_clgij(Register r1, int64_t i2, branch_condition m3, address a4);              // (r1 m3 i2_uimm8) ? goto (pc+a4<<1) ; uint64  -- z10

  // Compare and trap instructions.
  // signed comparison
  inline void z_crt(Register r1,  Register r2, int64_t m3);  // (r1 m3 r2)        ? trap ; int32  -- z10
  inline void z_cgrt(Register r1, Register r2, int64_t m3);  // (r1 m3 r2)        ? trap ; int64  -- z10
  inline void z_cit(Register r1,  int64_t i2, int64_t m3);   // (r1 m3 i2_imm16)  ? trap ; int32  -- z10
  inline void z_cgit(Register r1, int64_t i2, int64_t m3);   // (r1 m3 i2_imm16)  ? trap ; int64  -- z10
  // unsigned comparison
  inline void z_clrt(Register r1,  Register r2, int64_t m3); // (r1 m3 r2)        ? trap ; uint32 -- z10
  inline void z_clgrt(Register r1, Register r2, int64_t m3); // (r1 m3 r2)        ? trap ; uint64 -- z10
  inline void z_clfit(Register r1,  int64_t i2, int64_t m3); // (r1 m3 i2_uimm16) ? trap ; uint32 -- z10
  inline void z_clgit(Register r1, int64_t i2, int64_t m3);  // (r1 m3 i2_uimm16) ? trap ; uint64 -- z10

  inline void z_illtrap();
  inline void z_illtrap(int id);
  inline void z_illtrap_eyecatcher(unsigned short xpattern, unsigned short pattern);


  // load address, add for addresses
  // ===============================

  // The versions without suffix z assert that the base reg is != Z_R0.
  // Z_R0 is interpreted as constant '0'. The variants with Address operand
  // check this automatically, so no two versions are needed.
  inline void z_layz(Register r1, int64_t d2, Register x2, Register b2); // Special version. Allows Z_R0 as base reg.
  inline void z_lay(Register r1, const Address &a);                      // r1 = a
  inline void z_lay(Register r1, int64_t d2, Register x2, Register b2);  // r1 = d2_imm20+x2+b2
  inline void z_laz(Register r1, int64_t d2, Register x2, Register b2);  // Special version. Allows Z_R0 as base reg.
  inline void z_la(Register r1, const Address &a);                       // r1 = a                ; unsigned immediate!
  inline void z_la(Register r1, int64_t d2, Register x2, Register b2);   // r1 = d2_uimm12+x2+b2  ; unsigned immediate!
  inline void z_larl(Register r1, int64_t i2);                           // r1 = pc + i2_imm32<<1;
  inline void z_larl(Register r1, address a2);                           // r1 = pc + i2_imm32<<1;

  // Load instructions for integers
  // ==============================

  // Address as base + index + offset
  inline void z_lb( Register r1, const Address &a);                     // load r1 = *(a)              ; int32 <- int8
  inline void z_lb( Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int32 <- int8
  inline void z_lh( Register r1, const Address &a);                     // load r1 = *(a)              ; int32 <- int16
  inline void z_lh( Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_uimm12+x2+b2); int32 <- int16
  inline void z_lhy(Register r1, const Address &a);                     // load r1 = *(a)              ; int32 <- int16
  inline void z_lhy(Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int32 <- int16
  inline void z_l(  Register r1, const Address& a);                     // load r1 = *(a)              ; int32
  inline void z_l(  Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_uimm12+x2+b2); int32
  inline void z_ly( Register r1, const Address& a);                     // load r1 = *(a)              ; int32
  inline void z_ly( Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int32

  inline void z_lgb(Register r1, const Address &a);                     // load r1 = *(a)              ; int64 <- int8
  inline void z_lgb(Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int64 <- int8
  inline void z_lgh(Register r1, const Address &a);                     // load r1 = *(a)              ; int64 <- int16
  inline void z_lgh(Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm12+x2+b2) ; int64 <- int16
  inline void z_lgf(Register r1, const Address &a);                     // load r1 = *(a)              ; int64 <- int32
  inline void z_lgf(Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int64 <- int32
  inline void z_lg( Register r1, const Address& a);                     // load r1 = *(a)              ; int64 <- int64
  inline void z_lg( Register r1, int64_t d2, Register x2, Register b2); // load r1 = *(d2_imm20+x2+b2) ; int64 <- int64

  // load and test
  inline void z_lt(  Register r1, const Address &a);                    // load and test r1 = *(a)              ; int32
  inline void z_lt(  Register r1, int64_t d2, Register x2, Register b2);// load and test r1 = *(d2_imm20+x2+b2) ; int32
  inline void z_ltg( Register r1, const Address &a);                    // load and test r1 = *(a)              ; int64
  inline void z_ltg( Register r1, int64_t d2, Register x2, Register b2);// load and test r1 = *(d2_imm20+x2+b2) ; int64
  inline void z_ltgf(Register r1, const Address &a);                    // load and test r1 = *(a)              ; int64 <- int32
  inline void z_ltgf(Register r1, int64_t d2, Register x2, Register b2);// load and test r1 = *(d2_imm20+x2+b2) ; int64 <- int32

  // load unsigned integer - zero extended
  inline void z_llc( Register r1, const Address& a);                    // load r1 = *(a)              ; uint32 <- uint8
  inline void z_llc( Register r1, int64_t d2, Register x2, Register b2);// load r1 = *(d2_imm20+x2+b2) ; uint32 <- uint8
  inline void z_llh( Register r1, const Address& a);                    // load r1 = *(a)              ; uint32 <- uint16
  inline void z_llh( Register r1, int64_t d2, Register x2, Register b2);// load r1 = *(d2_imm20+x2+b2) ; uint32 <- uint16
  inline void z_llgc(Register r1, const Address& a);                    // load r1 = *(a)              ; uint64 <- uint8
  inline void z_llgc(Register r1, int64_t d2, Register x2, Register b2);// load r1 = *(d2_imm20+x2+b2) ; uint64 <- uint8
  inline void z_llgc( Register r1, int64_t d2, Register b2);            // load r1 = *(d2_imm20+b2)    ; uint64 <- uint8
  inline void z_llgh(Register r1, const Address& a);                    // load r1 = *(a)              ; uint64 <- uint16
  inline void z_llgh(Register r1, int64_t d2, Register x2, Register b2);// load r1 = *(d2_imm20+x2+b2) ; uint64 <- uint16
  inline void z_llgf(Register r1, const Address& a);                    // load r1 = *(a)              ; uint64 <- uint32
  inline void z_llgf(Register r1, int64_t d2, Register x2, Register b2);// load r1 = *(d2_imm20+x2+b2) ; uint64 <- uint32

  // pc relative addressing
  inline void z_lhrl( Register r1, int64_t i2);   // load r1 = *(pc + i2_imm32<<1) ; int32 <- int16    -- z10
  inline void z_lrl(  Register r1, int64_t i2);   // load r1 = *(pc + i2_imm32<<1) ; int32             -- z10
  inline void z_lghrl(Register r1, int64_t i2);   // load r1 = *(pc + i2_imm32<<1) ; int64 <- int16    -- z10
  inline void z_lgfrl(Register r1, int64_t i2);   // load r1 = *(pc + i2_imm32<<1) ; int64 <- int32    -- z10
  inline void z_lgrl( Register r1, int64_t i2);   // load r1 = *(pc + i2_imm32<<1) ; int64             -- z10

  inline void z_llhrl( Register r1, int64_t i2);  // load r1 = *(pc + i2_imm32<<1) ; uint32 <- uint16  -- z10
  inline void z_llghrl(Register r1, int64_t i2);  // load r1 = *(pc + i2_imm32<<1) ; uint64 <- uint16  -- z10
  inline void z_llgfrl(Register r1, int64_t i2);  // load r1 = *(pc + i2_imm32<<1) ; uint64 <- uint32  -- z10

  // Store instructions for integers
  // ===============================

  // Address as base + index + offset
  inline void z_stc( Register r1, const Address &d);                     // store *(a)               = r1  ; int8
  inline void z_stc( Register r1, int64_t d2, Register x2, Register b2); // store *(d2_uimm12+x2+b2) = r1  ; int8
  inline void z_stcy(Register r1, const Address &d);                     // store *(a)               = r1  ; int8
  inline void z_stcy(Register r1, int64_t d2, Register x2, Register b2); // store *(d2_imm20+x2+b2)  = r1  ; int8
  inline void z_sth( Register r1, const Address &d);                     // store *(a)               = r1  ; int16
  inline void z_sth( Register r1, int64_t d2, Register x2, Register b2); // store *(d2_uimm12+x2+b2) = r1  ; int16
  inline void z_sthy(Register r1, const Address &d);                     // store *(a)               = r1  ; int16
  inline void z_sthy(Register r1, int64_t d2, Register x2, Register b2); // store *(d2_imm20+x2+b2)  = r1  ; int16
  inline void z_st(  Register r1, const Address &d);                     // store *(a)               = r1  ; int32
  inline void z_st(  Register r1, int64_t d2, Register x2, Register b2); // store *(d2_uimm12+x2+b2) = r1  ; int32
  inline void z_sty( Register r1, const Address &d);                     // store *(a)               = r1  ; int32
  inline void z_sty( Register r1, int64_t d2, Register x2, Register b2); // store *(d2_imm20+x2+b2)  = r1  ; int32
  inline void z_stg( Register r1, const Address &d);                     // store *(a)               = r1  ; int64
  inline void z_stg( Register r1, int64_t d2, Register x2, Register b2); // store *(d2_uimm12+x2+b2) = r1  ; int64

  inline void z_stcm( Register r1, int64_t m3, int64_t d2, Register b2); // store character under mask
  inline void z_stcmy(Register r1, int64_t m3, int64_t d2, Register b2); // store character under mask
  inline void z_stcmh(Register r1, int64_t m3, int64_t d2, Register b2); // store character under mask

  // pc relative addressing
  inline void z_sthrl(Register r1, int64_t i2);   // store *(pc + i2_imm32<<1) = r1 ; int16  -- z10
  inline void z_strl( Register r1, int64_t i2);   // store *(pc + i2_imm32<<1) = r1 ; int32  -- z10
  inline void z_stgrl(Register r1, int64_t i2);   // store *(pc + i2_imm32<<1) = r1 ; int64  -- z10


  // Load and store immediates
  // =========================

  // load immediate
  inline void z_lhi( Register r1, int64_t i2);                  // r1 = i2_imm16    ; int32 <- int16
  inline void z_lghi(Register r1, int64_t i2);                  // r1 = i2_imm16    ; int64 <- int16
  inline void z_lgfi(Register r1, int64_t i2);                  // r1 = i2_imm32    ; int64 <- int32

  inline void z_llihf(Register r1, int64_t i2);                 // r1 = i2_imm32    ; uint64 <- (uint32<<32)
  inline void z_llilf(Register r1, int64_t i2);                 // r1 = i2_imm32    ; uint64 <- uint32
  inline void z_llihh(Register r1, int64_t i2);                 // r1 = i2_imm16    ; uint64 <- (uint16<<48)
  inline void z_llihl(Register r1, int64_t i2);                 // r1 = i2_imm16    ; uint64 <- (uint16<<32)
  inline void z_llilh(Register r1, int64_t i2);                 // r1 = i2_imm16    ; uint64 <- (uint16<<16)
  inline void z_llill(Register r1, int64_t i2);                 // r1 = i2_imm16    ; uint64 <- uint16

  // insert immediate
  inline void z_ic(  Register r1, int64_t d2, Register x2, Register b2); // insert character
  inline void z_icy( Register r1, int64_t d2, Register x2, Register b2); // insert character
  inline void z_icm( Register r1, int64_t m3, int64_t d2, Register b2);  // insert character under mask
  inline void z_icmy(Register r1, int64_t m3, int64_t d2, Register b2);  // insert character under mask
  inline void z_icmh(Register r1, int64_t m3, int64_t d2, Register b2);  // insert character under mask

  inline void z_iihh(Register r1, int64_t i2);                  // insert immediate  r1[ 0-15] = i2_imm16
  inline void z_iihl(Register r1, int64_t i2);                  // insert immediate  r1[16-31] = i2_imm16
  inline void z_iilh(Register r1, int64_t i2);                  // insert immediate  r1[32-47] = i2_imm16
  inline void z_iill(Register r1, int64_t i2);                  // insert immediate  r1[48-63] = i2_imm16
  inline void z_iihf(Register r1, int64_t i2);                  // insert immediate  r1[32-63] = i2_imm32
  inline void z_iilf(Register r1, int64_t i2);                  // insert immediate  r1[ 0-31] = i2_imm32

  // store immediate
  inline void z_mvhhi(const Address &d, int64_t i2);            // store *(d)           = i2_imm16 ; int16
  inline void z_mvhhi(int64_t d1, Register b1, int64_t i2);     // store *(d1_imm12+b1) = i2_imm16 ; int16
  inline void z_mvhi( const Address &d, int64_t i2);            // store *(d)           = i2_imm16 ; int32
  inline void z_mvhi( int64_t d1, Register b1, int64_t i2);     // store *(d1_imm12+b1) = i2_imm16 ; int32
  inline void z_mvghi(const Address &d, int64_t i2);            // store *(d)           = i2_imm16 ; int64
  inline void z_mvghi(int64_t d1, Register b1, int64_t i2);     // store *(d1_imm12+b1) = i2_imm16 ; int64

  // Move and Convert instructions
  // =============================

  // move, sign extend
  inline void z_lbr(Register r1, Register r2);             // move r1 = r2 ; int32  <- int8
  inline void z_lhr( Register r1, Register r2);            // move r1 = r2 ; int32  <- int16
  inline void z_lr(Register r1, Register r2);              // move r1 = r2 ; int32, no sign extension
  inline void z_lgbr(Register r1, Register r2);            // move r1 = r2 ; int64  <- int8
  inline void z_lghr(Register r1, Register r2);            // move r1 = r2 ; int64  <- int16
  inline void z_lgfr(Register r1, Register r2);            // move r1 = r2 ; int64  <- int32
  inline void z_lgr(Register r1, Register r2);             // move r1 = r2 ; int64
  // move, zero extend
  inline void z_llhr( Register r1, Register r2);           // move r1 = r2 ; uint32 <- uint16
  inline void z_llgcr(Register r1, Register r2);           // move r1 = r2 ; uint64 <- uint8
  inline void z_llghr(Register r1, Register r2);           // move r1 = r2 ; uint64 <- uint16
  inline void z_llgfr(Register r1, Register r2);           // move r1 = r2 ; uint64 <- uint32

  // move and test register
  inline void z_ltr(Register r1, Register r2);             // load/move and test r1 = r2; int32
  inline void z_ltgr(Register r1, Register r2);            // load/move and test r1 = r2; int64
  inline void z_ltgfr(Register r1, Register r2);           // load/move and test r1 = r2; int64 <-- int32

  // move and byte-reverse
  inline void z_lrvr( Register r1, Register r2);           // move and reverse byte order r1 = r2; int32
  inline void z_lrvgr(Register r1, Register r2);           // move and reverse byte order r1 = r2; int64


  // Arithmetic instructions (Integer only)
  // ======================================
  // For float arithmetic instructions scroll further down
  // Add logical differs in the condition codes set!

  // add registers
  inline void z_ar(   Register r1, Register r2);                      // add         r1 = r1 + r2  ; int32
  inline void z_agr(  Register r1, Register r2);                      // add         r1 = r1 + r2  ; int64
  inline void z_agfr( Register r1, Register r2);                      // add         r1 = r1 + r2  ; int64 <- int32
  inline void z_ark(  Register r1, Register r2, Register r3);         // add         r1 = r2 + r3  ; int32
  inline void z_agrk( Register r1, Register r2, Register r3);         // add         r1 = r2 + r3  ; int64

  inline void z_alr(  Register r1, Register r2);                      // add logical r1 = r1 + r2  ; int32
  inline void z_algr( Register r1, Register r2);                      // add logical r1 = r1 + r2  ; int64
  inline void z_algfr(Register r1, Register r2);                      // add logical r1 = r1 + r2  ; int64 <- int32
  inline void z_alrk( Register r1, Register r2, Register r3);         // add logical r1 = r2 + r3  ; int32
  inline void z_algrk(Register r1, Register r2, Register r3);         // add logical r1 = r2 + r3  ; int64
  inline void z_alcgr(Register r1, Register r2);                      // add logical with carry r1 = r1 + r2 + c  ; int64

  // add immediate
  inline void z_ahi(  Register r1, int64_t i2);                       // add         r1 = r1 + i2_imm16 ; int32
  inline void z_afi(  Register r1, int64_t i2);                       // add         r1 = r1 + i2_imm32 ; int32
  inline void z_alfi( Register r1, int64_t i2);                       // add         r1 = r1 + i2_imm32 ; int32
  inline void z_aghi( Register r1, int64_t i2);                       // add logical r1 = r1 + i2_imm16 ; int64
  inline void z_agfi( Register r1, int64_t i2);                       // add         r1 = r1 + i2_imm32 ; int64
  inline void z_algfi(Register r1, int64_t i2);                       // add logical r1 = r1 + i2_imm32 ; int64
  inline void z_ahik( Register r1, Register r3, int64_t i2);          // add         r1 = r3 + i2_imm16 ; int32
  inline void z_aghik(Register r1, Register r3, int64_t i2);          // add         r1 = r3 + i2_imm16 ; int64
  inline void z_aih(  Register r1, int64_t i2);                       // add         r1 = r1 + i2_imm32 ; int32 (HiWord)

  // add memory
  inline void z_a( Register r1, int64_t d2, Register x2, Register b2);  // add r1 = r1 + *(d2_uimm12+s2+b2) ; int32
  inline void z_ay(  Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+s2+b2)  ; int32
  inline void z_ag(  Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+s2+b2)  ; int64
  inline void z_agf( Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+x2+b2)  ; int64 <- int32
  inline void z_al(  Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_uimm12+x2+b2) ; int32
  inline void z_aly( Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+x2+b2)  ; int32
  inline void z_alg( Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+x2+b2)  ; int64
  inline void z_algf(Register r1, int64_t d2, Register x2, Register b2);// add r1 = r1 + *(d2_imm20+x2+b2)  ; int64 <- int32
  inline void z_a(   Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int32
  inline void z_ay(  Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int32
  inline void z_al(  Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int32
  inline void z_aly( Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int32
  inline void z_ag(  Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int64
  inline void z_agf( Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int64 <- int32
  inline void z_alg( Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int64
  inline void z_algf(Register r1, const Address& a);                  // add r1 = r1 + *(a)               ; int64 <- int32


  inline void z_alhsik( Register r1, Register r3, int64_t i2);    // add logical r1 = r3 + i2_imm16   ; int32
  inline void z_alghsik(Register r1, Register r3, int64_t i2);    // add logical r1 = r3 + i2_imm16   ; int64

  inline void z_asi(  int64_t d1, Register b1, int64_t i2);       // add           *(d1_imm20+b1) += i2_imm8 ; int32   -- z10
  inline void z_agsi( int64_t d1, Register b1, int64_t i2);       // add           *(d1_imm20+b1) += i2_imm8 ; int64   -- z10
  inline void z_alsi( int64_t d1, Register b1, int64_t i2);       // add logical   *(d1_imm20+b1) += i2_imm8 ; uint32  -- z10
  inline void z_algsi(int64_t d1, Register b1, int64_t i2);       // add logical   *(d1_imm20+b1) += i2_imm8 ; uint64  -- z10
  inline void z_asi(  const Address& d, int64_t i2);              // add           *(d) += i2_imm8           ; int32   -- z10
  inline void z_agsi( const Address& d, int64_t i2);              // add           *(d) += i2_imm8           ; int64   -- z10
  inline void z_alsi( const Address& d, int64_t i2);              // add logical   *(d) += i2_imm8           ; uint32  -- z10
  inline void z_algsi(const Address& d, int64_t i2);              // add logical   *(d) += i2_imm8           ; uint64  -- z10

  // sign adjustment
  inline void z_lcr(  Register r1, Register r2 = noreg);              // neg r1 = -r2   ; int32
  inline void z_lcgr( Register r1, Register r2 = noreg);              // neg r1 = -r2   ; int64
  inline void z_lcgfr(Register r1, Register r2);                      // neg r1 = -r2   ; int64 <- int32
  inline void z_lnr(  Register r1, Register r2 = noreg);              // neg r1 = -|r2| ; int32
  inline void z_lngr( Register r1, Register r2 = noreg);              // neg r1 = -|r2| ; int64
  inline void z_lngfr(Register r1, Register r2);                      // neg r1 = -|r2| ; int64 <- int32
  inline void z_lpr(  Register r1, Register r2 = noreg);              //     r1 =  |r2| ; int32
  inline void z_lpgr( Register r1, Register r2 = noreg);              //     r1 =  |r2| ; int64
  inline void z_lpgfr(Register r1, Register r2);                      //     r1 =  |r2| ; int64 <- int32

  // subtract intstructions
  // sub registers
  inline void z_sr(   Register r1, Register r2);                      // sub         r1 = r1 - r2                ; int32
  inline void z_sgr(  Register r1, Register r2);                      // sub         r1 = r1 - r2                ; int64
  inline void z_sgfr( Register r1, Register r2);                      // sub         r1 = r1 - r2                ; int64 <- int32
  inline void z_srk(  Register r1, Register r2, Register r3);         // sub         r1 = r2 - r3                ; int32
  inline void z_sgrk( Register r1, Register r2, Register r3);         // sub         r1 = r2 - r3                ; int64

  inline void z_slr(  Register r1, Register r2);                      // sub logical r1 = r1 - r2                ; int32
  inline void z_slgr( Register r1, Register r2);                      // sub logical r1 = r1 - r2                ; int64
  inline void z_slgfr(Register r1, Register r2);                      // sub logical r1 = r1 - r2                ; int64 <- int32
  inline void z_slrk( Register r1, Register r2, Register r3);         // sub logical r1 = r2 - r3                ; int32
  inline void z_slgrk(Register r1, Register r2, Register r3);         // sub logical r1 = r2 - r3                ; int64
  inline void z_slfi( Register r1, int64_t i2);                       // sub logical r1 = r1 - i2_uimm32         ; int32
  inline void z_slgfi(Register r1, int64_t i2);                       // add logical r1 = r1 - i2_uimm32         ; int64

  // sub memory
  inline void z_s(   Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 - *(d2_imm12+x2+b2) ; int32
  inline void z_sy(  Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 + *(d2_imm20+s2+b2) ; int32
  inline void z_sg(  Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 - *(d2_imm12+x2+b2) ; int64
  inline void z_sgf( Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 - *(d2_imm12+x2+b2) ; int64 - int32
  inline void z_slg( Register r1, int64_t d2, Register x2, Register b2);  // sub logical r1 = r1 - *(d2_imm20+x2+b2) ; uint64
  inline void z_slgf(Register r1, int64_t d2, Register x2, Register b2);  // sub logical r1 = r1 - *(d2_imm20+x2+b2) ; uint64 - uint32
  inline void z_s(   Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; int32
  inline void z_sy(  Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; int32
  inline void z_sg(  Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; int64
  inline void z_sgf( Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; int64 - int32
  inline void z_slg( Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; uint64
  inline void z_slgf(Register r1, const Address& a);                      // sub         r1 = r1 - *(a)              ; uint64 - uint32

  inline void z_sh(  Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 - *(d2_imm12+x2+b2) ; int32 - int16
  inline void z_shy( Register r1, int64_t d2, Register x2, Register b2);  // sub         r1 = r1 - *(d2_imm20+x2+b2) ; int32 - int16
  inline void z_sh(  Register r1, const Address &a);                      // sub         r1 = r1 - *(d2_imm12+x2+b2) ; int32 - int16
  inline void z_shy( Register r1, const Address &a);                      // sub         r1 = r1 - *(d2_imm20+x2+b2) ; int32 - int16

  // Multiplication instructions
  // mul registers
  inline void z_msr(  Register r1, Register r2);                          // mul r1 = r1 * r2          ; int32
  inline void z_msgr( Register r1, Register r2);                          // mul r1 = r1 * r2          ; int64
  inline void z_msgfr(Register r1, Register r2);                          // mul r1 = r1 * r2          ; int64 <- int32
  inline void z_mlr(  Register r1, Register r2);                          // mul r1 = r1 * r2          ; int32 unsigned
  inline void z_mlgr( Register r1, Register r2);                          // mul r1 = r1 * r2          ; int64 unsigned
  // mul register - memory
  inline void z_mhy( Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_msy( Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_msg( Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_msgf(Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_ml(  Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_mlg( Register r1, int64_t d2, Register x2, Register b2);  // mul r1 = r1 * *(d2+x2+b2)
  inline void z_mhy( Register r1, const Address& a);                      // mul r1 = r1 * *(a)
  inline void z_msy( Register r1, const Address& a);                      // mul r1 = r1 * *(a)
  inline void z_msg( Register r1, const Address& a);                      // mul r1 = r1 * *(a)
  inline void z_msgf(Register r1, const Address& a);                      // mul r1 = r1 * *(a)
  inline void z_ml(  Register r1, const Address& a);                      // mul r1 = r1 * *(a)
  inline void z_mlg( Register r1, const Address& a);                      // mul r1 = r1 * *(a)

  inline void z_msfi( Register r1, int64_t i2);   // mult r1 = r1 * i2_imm32;   int32  -- z10
  inline void z_msgfi(Register r1, int64_t i2);   // mult r1 = r1 * i2_imm32;   int64  -- z10
  inline void z_mhi(  Register r1, int64_t i2);   // mult r1 = r1 * i2_imm16;   int32
  inline void z_mghi( Register r1, int64_t i2);   // mult r1 = r1 * i2_imm16;   int64

  // Division instructions
  inline void z_dsgr( Register r1, Register r2);      // div  r1 = r1 / r2               ; int64/int32 needs reg pair!
  inline void z_dsgfr(Register r1, Register r2);      // div  r1 = r1 / r2               ; int64/int32 needs reg pair!


  // Logic instructions
  // ===================

  // and
  inline void z_n(   Register r1, int64_t d2, Register x2, Register b2);
  inline void z_ny(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_ng(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_n(   Register r1, const Address& a);
  inline void z_ny(  Register r1, const Address& a);
  inline void z_ng(  Register r1, const Address& a);

  inline void z_nr(  Register r1, Register r2);               // and r1 = r1 & r2         ; int32
  inline void z_ngr( Register r1, Register r2);               // and r1 = r1 & r2         ; int64
  inline void z_nrk( Register r1, Register r2, Register r3);  // and r1 = r2 & r3         ; int32
  inline void z_ngrk(Register r1, Register r2, Register r3);  // and r1 = r2 & r3         ; int64

  inline void z_nihh(Register r1, int64_t i2);                // and r1 = r1 & i2_imm16   ; and only for bits  0-15
  inline void z_nihl(Register r1, int64_t i2);                // and r1 = r1 & i2_imm16   ; and only for bits 16-31
  inline void z_nilh(Register r1, int64_t i2);                // and r1 = r1 & i2_imm16   ; and only for bits 32-47
  inline void z_nill(Register r1, int64_t i2);                // and r1 = r1 & i2_imm16   ; and only for bits 48-63
  inline void z_nihf(Register r1, int64_t i2);                // and r1 = r1 & i2_imm32   ; and only for bits  0-31
  inline void z_nilf(Register r1, int64_t i2);                // and r1 = r1 & i2_imm32   ; and only for bits 32-63  see also MacroAssembler::nilf.

  // or
  inline void z_o(   Register r1, int64_t d2, Register x2, Register b2);
  inline void z_oy(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_og(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_o(   Register r1, const Address& a);
  inline void z_oy(  Register r1, const Address& a);
  inline void z_og(  Register r1, const Address& a);

  inline void z_or(  Register r1, Register r2);               // or r1 = r1 | r2; int32
  inline void z_ogr( Register r1, Register r2);               // or r1 = r1 | r2; int64
  inline void z_ork( Register r1, Register r2, Register r3);  // or r1 = r2 | r3         ; int32
  inline void z_ogrk(Register r1, Register r2, Register r3);  // or r1 = r2 | r3         ; int64

  inline void z_oihh(Register r1, int64_t i2);                // or r1 = r1 | i2_imm16   ; or only for bits  0-15
  inline void z_oihl(Register r1, int64_t i2);                // or r1 = r1 | i2_imm16   ; or only for bits 16-31
  inline void z_oilh(Register r1, int64_t i2);                // or r1 = r1 | i2_imm16   ; or only for bits 32-47
  inline void z_oill(Register r1, int64_t i2);                // or r1 = r1 | i2_imm16   ; or only for bits 48-63
  inline void z_oihf(Register r1, int64_t i2);                // or r1 = r1 | i2_imm32   ; or only for bits  0-31
  inline void z_oilf(Register r1, int64_t i2);                // or r1 = r1 | i2_imm32   ; or only for bits 32-63

  // xor
  inline void z_x(   Register r1, int64_t d2, Register x2, Register b2);
  inline void z_xy(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_xg(  Register r1, int64_t d2, Register x2, Register b2);
  inline void z_x(   Register r1, const Address& a);
  inline void z_xy(  Register r1, const Address& a);
  inline void z_xg(  Register r1, const Address& a);

  inline void z_xr(  Register r1, Register r2);               // xor r1 = r1 ^ r2         ; int32
  inline void z_xgr( Register r1, Register r2);               // xor r1 = r1 ^ r2         ; int64
  inline void z_xrk( Register r1, Register r2, Register r3);  // xor r1 = r2 ^ r3         ; int32
  inline void z_xgrk(Register r1, Register r2, Register r3);  // xor r1 = r2 ^ r3         ; int64

  inline void z_xihf(Register r1, int64_t i2);                // xor r1 = r1 ^ i2_imm32   ; or only for bits  0-31
  inline void z_xilf(Register r1, int64_t i2);                // xor r1 = r1 ^ i2_imm32   ; or only for bits 32-63

  // shift
  inline void z_sla( Register r1,              int64_t d2, Register b2=Z_R0); // shift left  r1 = r1 << ((d2+b2)&0x3f) ; int32, only 31 bits shifted, sign preserved!
  inline void z_slak(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift left  r1 = r3 << ((d2+b2)&0x3f) ; int32, only 31 bits shifted, sign preserved!
  inline void z_slag(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift left  r1 = r3 << ((d2+b2)&0x3f) ; int64, only 63 bits shifted, sign preserved!
  inline void z_sra( Register r1,              int64_t d2, Register b2=Z_R0); // shift right r1 = r1 >> ((d2+b2)&0x3f) ; int32, sign extended
  inline void z_srak(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift right r1 = r3 >> ((d2+b2)&0x3f) ; int32, sign extended
  inline void z_srag(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift right r1 = r3 >> ((d2+b2)&0x3f) ; int64, sign extended
  inline void z_sll( Register r1,              int64_t d2, Register b2=Z_R0); // shift left  r1 = r1 << ((d2+b2)&0x3f) ; int32, zeros added
  inline void z_sllk(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift left  r1 = r3 << ((d2+b2)&0x3f) ; int32, zeros added
  inline void z_sllg(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift left  r1 = r3 << ((d2+b2)&0x3f) ; int64, zeros added
  inline void z_srl( Register r1,              int64_t d2, Register b2=Z_R0); // shift right r1 = r1 >> ((d2+b2)&0x3f) ; int32, zero extended
  inline void z_srlk(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift right r1 = r3 >> ((d2+b2)&0x3f) ; int32, zero extended
  inline void z_srlg(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // shift right r1 = r3 >> ((d2+b2)&0x3f) ; int64, zero extended

  // rotate
  inline void z_rll( Register r1, Register r3, int64_t d2, Register b2=Z_R0); // rot r1 = r3 << (d2+b2 & 0x3f) ; int32  -- z10
  inline void z_rllg(Register r1, Register r3, int64_t d2, Register b2=Z_R0); // rot r1 = r3 << (d2+b2 & 0x3f) ; int64  -- z10

  // rotate the AND/XOR/OR/insert
  inline void z_rnsbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only = false); // rotate then AND selected bits  -- z196
  inline void z_rxsbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only = false); // rotate then XOR selected bits  -- z196
  inline void z_rosbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only = false); // rotate then OR  selected bits  -- z196
  inline void z_risbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool zero_rest = false); // rotate then INS selected bits  -- z196


  // memory-immediate instructions (8-bit immediate)
  // ===============================================

  inline void z_cli( int64_t d1, Register b1, int64_t i2); // compare *(d1_imm12+b1) ^= i2_imm8           ; int8
  inline void z_mvi( int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1)  = i2_imm8           ; int8
  inline void z_tm(  int64_t d1, Register b1, int64_t i2); // test    *(d1_imm12+b1) against mask i2_imm8 ; int8
  inline void z_ni(  int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) &= i2_imm8           ; int8
  inline void z_oi(  int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) |= i2_imm8           ; int8
  inline void z_xi(  int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) ^= i2_imm8           ; int8
  inline void z_cliy(int64_t d1, Register b1, int64_t i2); // compare *(d1_imm12+b1) ^= i2_imm8           ; int8
  inline void z_mviy(int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1)  = i2_imm8           ; int8
  inline void z_tmy( int64_t d1, Register b1, int64_t i2); // test    *(d1_imm12+b1) against mask i2_imm8 ; int8
  inline void z_niy( int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) &= i2_imm8           ; int8
  inline void z_oiy( int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) |= i2_imm8           ; int8
  inline void z_xiy( int64_t d1, Register b1, int64_t i2); // store   *(d1_imm12+b1) ^= i2_imm8           ; int8
  inline void z_cli( const Address& a, int64_t imm8);      // compare *(a)           ^= imm8              ; int8
  inline void z_mvi( const Address& a, int64_t imm8);      // store   *(a)            = imm8              ; int8
  inline void z_tm(  const Address& a, int64_t imm8);      // test    *(a)           against mask imm8    ; int8
  inline void z_ni(  const Address& a, int64_t imm8);      // store   *(a)           &= imm8              ; int8
  inline void z_oi(  const Address& a, int64_t imm8);      // store   *(a)           |= imm8              ; int8
  inline void z_xi(  const Address& a, int64_t imm8);      // store   *(a)           ^= imm8              ; int8
  inline void z_cliy(const Address& a, int64_t imm8);      // compare *(a)           ^= imm8              ; int8
  inline void z_mviy(const Address& a, int64_t imm8);      // store   *(a)            = imm8              ; int8
  inline void z_tmy( const Address& a, int64_t imm8);      // test    *(a)           against mask imm8    ; int8
  inline void z_niy( const Address& a, int64_t imm8);      // store   *(a)           &= imm8              ; int8
  inline void z_oiy( const Address& a, int64_t imm8);      // store   *(a)           |= imm8              ; int8
  inline void z_xiy( const Address& a, int64_t imm8);      // store   *(a)           ^= imm8              ; int8


  //------------------------------
  // Interlocked-Update
  //------------------------------
  inline void z_laa(  Register r1, Register r3, int64_t d2, Register b2);   // load and add    int32, signed   -- z196
  inline void z_laag( Register r1, Register r3, int64_t d2, Register b2);   // load and add    int64, signed   -- z196
  inline void z_laal( Register r1, Register r3, int64_t d2, Register b2);   // load and add    int32, unsigned -- z196
  inline void z_laalg(Register r1, Register r3, int64_t d2, Register b2);   // load and add    int64, unsigned -- z196
  inline void z_lan(  Register r1, Register r3, int64_t d2, Register b2);   // load and and    int32           -- z196
  inline void z_lang( Register r1, Register r3, int64_t d2, Register b2);   // load and and    int64           -- z196
  inline void z_lax(  Register r1, Register r3, int64_t d2, Register b2);   // load and xor    int32           -- z196
  inline void z_laxg( Register r1, Register r3, int64_t d2, Register b2);   // load and xor    int64           -- z196
  inline void z_lao(  Register r1, Register r3, int64_t d2, Register b2);   // load and or     int32           -- z196
  inline void z_laog( Register r1, Register r3, int64_t d2, Register b2);   // load and or     int64           -- z196

  inline void z_laa(  Register r1, Register r3, const Address& a);          // load and add    int32, signed   -- z196
  inline void z_laag( Register r1, Register r3, const Address& a);          // load and add    int64, signed   -- z196
  inline void z_laal( Register r1, Register r3, const Address& a);          // load and add    int32, unsigned -- z196
  inline void z_laalg(Register r1, Register r3, const Address& a);          // load and add    int64, unsigned -- z196
  inline void z_lan(  Register r1, Register r3, const Address& a);          // load and and    int32           -- z196
  inline void z_lang( Register r1, Register r3, const Address& a);          // load and and    int64           -- z196
  inline void z_lax(  Register r1, Register r3, const Address& a);          // load and xor    int32           -- z196
  inline void z_laxg( Register r1, Register r3, const Address& a);          // load and xor    int64           -- z196
  inline void z_lao(  Register r1, Register r3, const Address& a);          // load and or     int32           -- z196
  inline void z_laog( Register r1, Register r3, const Address& a);          // load and or     int64           -- z196

  //--------------------------------
  // Execution Prediction
  //--------------------------------
  inline void z_pfd(  int64_t m1, int64_t d2, Register x2, Register b2);  // prefetch
  inline void z_pfd(  int64_t m1, Address a);
  inline void z_pfdrl(int64_t m1, int64_t i2);                            // prefetch
  inline void z_bpp(  int64_t m1, int64_t i2, int64_t d3, Register b3);   // branch prediction    -- EC12
  inline void z_bprp( int64_t m1, int64_t i2, int64_t i3);                // branch prediction    -- EC12

  //-------------------------------
  // Transaction Control
  //-------------------------------
  inline void z_tbegin(int64_t d1, Register b1, int64_t i2);          // begin transaction               -- EC12
  inline void z_tbeginc(int64_t d1, Register b1, int64_t i2);         // begin transaction (constrained) -- EC12
  inline void z_tend();                                               // end transaction                 -- EC12
  inline void z_tabort(int64_t d2, Register b2);                      // abort transaction               -- EC12
  inline void z_etnd(Register r1);                                    // extract tx nesting depth        -- EC12
  inline void z_ppa(Register r1, Register r2, int64_t m3);            // perform processor assist        -- EC12

  //---------------------------------
  // Conditional Execution
  //---------------------------------
  inline void z_locr( Register r1, Register r2, branch_condition cc);             // if (cc) load r1 = r2               ; int32 -- z196
  inline void z_locgr(Register r1, Register r2, branch_condition cc);             // if (cc) load r1 = r2               ; int64 -- z196
  inline void z_loc(  Register r1, int64_t d2, Register b2, branch_condition cc); // if (cc) load r1 = *(d2_simm20+b2)  ; int32 -- z196
  inline void z_locg( Register r1, int64_t d2, Register b2, branch_condition cc); // if (cc) load r1 = *(d2_simm20+b2)  ; int64 -- z196
  inline void z_loc(  Register r1, const Address& a, branch_condition cc);        // if (cc) load r1 = *(a)             ; int32 -- z196
  inline void z_locg( Register r1, const Address& a, branch_condition cc);        // if (cc) load r1 = *(a)             ; int64 -- z196
  inline void z_stoc( Register r1, int64_t d2, Register b2, branch_condition cc); // if (cc) store *(d2_simm20+b2) = r1 ; int32 -- z196
  inline void z_stocg(Register r1, int64_t d2, Register b2, branch_condition cc); // if (cc) store *(d2_simm20+b2) = r1 ; int64 -- z196


  // Complex CISC instructions
  // ==========================

  inline void z_cksm( Register r1, Register r2);                       // checksum. This is NOT CRC32
  inline void z_km(   Register r1, Register r2);                       // cipher message
  inline void z_kmc(  Register r1, Register r2);                       // cipher message with chaining
  inline void z_kma(  Register r1, Register r3, Register r2);          // cipher message with authentication
  inline void z_kmf(  Register r1, Register r2);                       // cipher message with cipher feedback
  inline void z_kmctr(Register r1, Register r3, Register r2);          // cipher message with counter
  inline void z_kmo(  Register r1, Register r2);                       // cipher message with output feedback
  inline void z_kimd( Register r1, Register r2);                       // msg digest (SHA)
  inline void z_klmd( Register r1, Register r2);                       // msg digest (SHA)
  inline void z_kmac( Register r1, Register r2);                       // msg authentication code

  inline void z_ex(Register r1, int64_t d2, Register x2, Register b2);// execute
  inline void z_exrl(Register r1, int64_t i2);                        // execute relative long         -- z10
  inline void z_exrl(Register r1, address a2);                        // execute relative long         -- z10

  inline void z_ectg(int64_t d1, Register b1, int64_t d2, Register b2, Register r3);  // extract cpu time
  inline void z_ecag(Register r1, Register r3, int64_t d2, Register b2);              // extract CPU attribute

  inline void z_srst(Register r1, Register r2);                       // search string
  inline void z_srstu(Register r1, Register r2);                      // search string unicode

  inline void z_mvc(const Address& d, const Address& s, int64_t l);               // move l bytes
  inline void z_mvc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2); // move l+1 bytes
  inline void z_mvcle(Register r1, Register r3, int64_t d2, Register b2=Z_R0);    // move region of memory

  inline void z_stfle(int64_t d2, Register b2);                            // store facility list extended

  inline void z_nc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2);// and *(d1+b1) = *(d1+l+b1) & *(d2+b2) ; d1, d2: uimm12, ands l+1 bytes
  inline void z_oc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2);//  or *(d1+b1) = *(d1+l+b1) | *(d2+b2) ; d1, d2: uimm12,  ors l+1 bytes
  inline void z_xc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2);// xor *(d1+b1) = *(d1+l+b1) ^ *(d2+b2) ; d1, d2: uimm12, xors l+1 bytes
  inline void z_nc(Address dst, int64_t len, Address src2);                     // and *dst = *dst & *src2, ands len bytes in memory
  inline void z_oc(Address dst, int64_t len, Address src2);                     //  or *dst = *dst | *src2,  ors len bytes in memory
  inline void z_xc(Address dst, int64_t len, Address src2);                     // xor *dst = *dst ^ *src2, xors len bytes in memory

  // compare instructions
  inline void z_clc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2);  // compare (*(d1_uimm12+b1), *(d1_uimm12+b1)) ; compare l bytes
  inline void z_clcle(Register r1, Register r3, int64_t d2, Register b2);  // compare logical long extended, see docu
  inline void z_clclu(Register r1, Register r3, int64_t d2, Register b2);  // compare logical long unicode, see docu

  // Translate characters
  inline void z_troo(Register r1, Register r2, int64_t m3);
  inline void z_trot(Register r1, Register r2, int64_t m3);
  inline void z_trto(Register r1, Register r2, int64_t m3);
  inline void z_trtt(Register r1, Register r2, int64_t m3);


  //---------------------------
  //--  Vector Instructions  --
  //---------------------------

  //---<  Vector Support Instructions  >---

  // Load (transfer from memory)
  inline void z_vlm(   VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vl(    VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vleb(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vleh(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vlef(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vleg(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);

  // Gather/Scatter
  inline void z_vgef(  VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3);
  inline void z_vgeg(  VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3);

  inline void z_vscef( VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3);
  inline void z_vsceg( VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3);

  // load and replicate
  inline void z_vlrep( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vlrepb(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vlreph(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vlrepf(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vlrepg(VectorRegister v1, int64_t d2, Register x2, Register b2);

  inline void z_vllez( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vllezb(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vllezh(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vllezf(VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vllezg(VectorRegister v1, int64_t d2, Register x2, Register b2);

  inline void z_vlbb(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vll(   VectorRegister v1, Register r3, int64_t d2, Register b2);

  // Load (register to register)
  inline void z_vlr(   VectorRegister v1, VectorRegister v2);

  inline void z_vlgv(  Register r1, VectorRegister v3, int64_t d2, Register b2, int64_t m4);
  inline void z_vlgvb( Register r1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vlgvh( Register r1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vlgvf( Register r1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vlgvg( Register r1, VectorRegister v3, int64_t d2, Register b2);

  inline void z_vlvg(  VectorRegister v1, Register r3, int64_t d2, Register b2, int64_t m4);
  inline void z_vlvgb( VectorRegister v1, Register r3, int64_t d2, Register b2);
  inline void z_vlvgh( VectorRegister v1, Register r3, int64_t d2, Register b2);
  inline void z_vlvgf( VectorRegister v1, Register r3, int64_t d2, Register b2);
  inline void z_vlvgg( VectorRegister v1, Register r3, int64_t d2, Register b2);

  inline void z_vlvgp( VectorRegister v1, Register r2, Register r3);

  // vector register pack
  inline void z_vpk(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vpkh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpkf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpkg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);

  inline void z_vpks(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5);
  inline void z_vpksh( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpksf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpksg( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpkshs(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpksfs(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpksgs(VectorRegister v1, VectorRegister v2, VectorRegister v3);

  inline void z_vpkls(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5);
  inline void z_vpklsh( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpklsf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpklsg( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpklshs(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpklsfs(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vpklsgs(VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // vector register unpack (sign-extended)
  inline void z_vuph(   VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vuphb(  VectorRegister v1, VectorRegister v2);
  inline void z_vuphh(  VectorRegister v1, VectorRegister v2);
  inline void z_vuphf(  VectorRegister v1, VectorRegister v2);
  inline void z_vupl(   VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vuplb(  VectorRegister v1, VectorRegister v2);
  inline void z_vuplh(  VectorRegister v1, VectorRegister v2);
  inline void z_vuplf(  VectorRegister v1, VectorRegister v2);

  // vector register unpack (zero-extended)
  inline void z_vuplh(  VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vuplhb( VectorRegister v1, VectorRegister v2);
  inline void z_vuplhh( VectorRegister v1, VectorRegister v2);
  inline void z_vuplhf( VectorRegister v1, VectorRegister v2);
  inline void z_vupll(  VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vupllb( VectorRegister v1, VectorRegister v2);
  inline void z_vupllh( VectorRegister v1, VectorRegister v2);
  inline void z_vupllf( VectorRegister v1, VectorRegister v2);

  // vector register merge high/low
  inline void z_vmrh( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmrhb(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrhh(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrhf(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrhg(VectorRegister v1, VectorRegister v2, VectorRegister v3);

  inline void z_vmrl( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmrlb(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrlh(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrlf(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmrlg(VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // vector register permute
  inline void z_vperm( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);
  inline void z_vpdi(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t        m4);

  // vector register replicate
  inline void z_vrep(  VectorRegister v1, VectorRegister v3, int64_t imm2, int64_t m4);
  inline void z_vrepb( VectorRegister v1, VectorRegister v3, int64_t imm2);
  inline void z_vreph( VectorRegister v1, VectorRegister v3, int64_t imm2);
  inline void z_vrepf( VectorRegister v1, VectorRegister v3, int64_t imm2);
  inline void z_vrepg( VectorRegister v1, VectorRegister v3, int64_t imm2);
  inline void z_vrepi( VectorRegister v1, int64_t imm2,      int64_t m3);
  inline void z_vrepib(VectorRegister v1, int64_t imm2);
  inline void z_vrepih(VectorRegister v1, int64_t imm2);
  inline void z_vrepif(VectorRegister v1, int64_t imm2);
  inline void z_vrepig(VectorRegister v1, int64_t imm2);

  inline void z_vsel(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);
  inline void z_vseg(  VectorRegister v1, VectorRegister v2, int64_t imm3);

  // Load (immediate)
  inline void z_vleib( VectorRegister v1, int64_t imm2, int64_t m3);
  inline void z_vleih( VectorRegister v1, int64_t imm2, int64_t m3);
  inline void z_vleif( VectorRegister v1, int64_t imm2, int64_t m3);
  inline void z_vleig( VectorRegister v1, int64_t imm2, int64_t m3);

  // Store
  inline void z_vstm(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vst(   VectorRegister v1, int64_t d2, Register x2, Register b2);
  inline void z_vsteb( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vsteh( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vstef( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vsteg( VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3);
  inline void z_vstl(  VectorRegister v1, Register r3, int64_t d2, Register b2);

  // Misc
  inline void z_vgm(   VectorRegister v1, int64_t imm2, int64_t imm3, int64_t m4);
  inline void z_vgmb(  VectorRegister v1, int64_t imm2, int64_t imm3);
  inline void z_vgmh(  VectorRegister v1, int64_t imm2, int64_t imm3);
  inline void z_vgmf(  VectorRegister v1, int64_t imm2, int64_t imm3);
  inline void z_vgmg(  VectorRegister v1, int64_t imm2, int64_t imm3);

  inline void z_vgbm(  VectorRegister v1, int64_t imm2);
  inline void z_vzero( VectorRegister v1); // preferred method to set vreg to all zeroes
  inline void z_vone(  VectorRegister v1); // preferred method to set vreg to all ones

  //---<  Vector Arithmetic Instructions  >---

  // Load
  inline void z_vlc(    VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vlcb(   VectorRegister v1, VectorRegister v2);
  inline void z_vlch(   VectorRegister v1, VectorRegister v2);
  inline void z_vlcf(   VectorRegister v1, VectorRegister v2);
  inline void z_vlcg(   VectorRegister v1, VectorRegister v2);
  inline void z_vlp(    VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vlpb(   VectorRegister v1, VectorRegister v2);
  inline void z_vlph(   VectorRegister v1, VectorRegister v2);
  inline void z_vlpf(   VectorRegister v1, VectorRegister v2);
  inline void z_vlpg(   VectorRegister v1, VectorRegister v2);

  // ADD
  inline void z_va(     VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vab(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vah(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vaf(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vag(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vaq(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vacc(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vaccb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vacch(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vaccf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vaccg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vaccq(  VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // SUB
  inline void z_vs(     VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vsb(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsh(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsf(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsg(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsq(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vscbi(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vscbib( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vscbih( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vscbif( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vscbig( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vscbiq( VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // MULTIPLY
  inline void z_vml(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmh(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmlh(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vme(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmle(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmo(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmlo(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);

  // MULTIPLY & ADD
  inline void z_vmal(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmah(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmalh(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmae(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmale(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmao(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vmalo(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);

  // VECTOR SUM
  inline void z_vsum(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vsumb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsumh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsumg(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vsumgh( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsumgf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsumq(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vsumqf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsumqg( VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // Average
  inline void z_vavg(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vavgb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavgh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavgf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavgg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavgl(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vavglb( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavglh( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavglf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vavglg( VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // VECTOR Galois Field Multiply Sum
  inline void z_vgfm(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vgfmb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vgfmh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vgfmf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vgfmg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  // VECTOR Galois Field Multiply Sum and Accumulate
  inline void z_vgfma(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5);
  inline void z_vgfmab( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);
  inline void z_vgfmah( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);
  inline void z_vgfmaf( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);
  inline void z_vgfmag( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4);

  //---<  Vector Logical Instructions  >---

  // AND
  inline void z_vn(     VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vnc(    VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // XOR
  inline void z_vx(     VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // NOR
  inline void z_vno(    VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // OR
  inline void z_vo(     VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // Comparison (element-wise)
  inline void z_vceq(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5);
  inline void z_vceqb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqbs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqhs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqfs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vceqgs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vch(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5);
  inline void z_vchb(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchh(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchf(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchg(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchbs(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchhs(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchfs(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchgs(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5);
  inline void z_vchlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlbs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlhs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlfs( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vchlgs( VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // Max/Min (element-wise)
  inline void z_vmx(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmxb(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxh(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxf(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxg(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmxlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmxlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmn(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmnb(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnh(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnf(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmng(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4);
  inline void z_vmnlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vmnlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // Leading/Trailing Zeros, population count
  inline void z_vclz(   VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vclzb(  VectorRegister v1, VectorRegister v2);
  inline void z_vclzh(  VectorRegister v1, VectorRegister v2);
  inline void z_vclzf(  VectorRegister v1, VectorRegister v2);
  inline void z_vclzg(  VectorRegister v1, VectorRegister v2);
  inline void z_vctz(   VectorRegister v1, VectorRegister v2, int64_t m3);
  inline void z_vctzb(  VectorRegister v1, VectorRegister v2);
  inline void z_vctzh(  VectorRegister v1, VectorRegister v2);
  inline void z_vctzf(  VectorRegister v1, VectorRegister v2);
  inline void z_vctzg(  VectorRegister v1, VectorRegister v2);
  inline void z_vpopct( VectorRegister v1, VectorRegister v2, int64_t m3);

  // Rotate/Shift
  inline void z_verllv( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4);
  inline void z_verllvb(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_verllvh(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_verllvf(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_verllvg(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_verll(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4);
  inline void z_verllb( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_verllh( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_verllf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_verllg( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_verim(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t m5);
  inline void z_verimb( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4);
  inline void z_verimh( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4);
  inline void z_verimf( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4);
  inline void z_verimg( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4);

  inline void z_veslv(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4);
  inline void z_veslvb( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_veslvh( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_veslvf( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_veslvg( VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesl(   VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4);
  inline void z_veslb(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_veslh(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_veslf(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_veslg(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);

  inline void z_vesrav( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4);
  inline void z_vesravb(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesravh(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesravf(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesravg(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesra(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4);
  inline void z_vesrab( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrah( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesraf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrag( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrlv( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4);
  inline void z_vesrlvb(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesrlvh(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesrlvf(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesrlvg(VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vesrl(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4);
  inline void z_vesrlb( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrlh( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrlf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);
  inline void z_vesrlg( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2);

  inline void z_vsl(    VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vslb(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsldb(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4);

  inline void z_vsra(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsrab(  VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsrl(   VectorRegister v1, VectorRegister v2, VectorRegister v3);
  inline void z_vsrlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3);

  // Test under Mask
  inline void z_vtm(    VectorRegister v1, VectorRegister v2);

  //---<  Vector String Instructions  >---
  inline void z_vfae(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5);   // Find any element
  inline void z_vfaeb(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfaeh(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfaef(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfee(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5);   // Find element equal
  inline void z_vfeeb(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfeeh(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfeef(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfene(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5);   // Find element not equal
  inline void z_vfeneb( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfeneh( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vfenef( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t cc5);
  inline void z_vstrc(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t imm5, int64_t cc6);   // String range compare
  inline void z_vstrcb( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t cc6);
  inline void z_vstrch( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t cc6);
  inline void z_vstrcf( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t cc6);
  inline void z_vistr(  VectorRegister v1, VectorRegister v2, int64_t imm3, int64_t cc5);                      // Isolate String
  inline void z_vistrb( VectorRegister v1, VectorRegister v2, int64_t cc5);
  inline void z_vistrh( VectorRegister v1, VectorRegister v2, int64_t cc5);
  inline void z_vistrf( VectorRegister v1, VectorRegister v2, int64_t cc5);
  inline void z_vistrbs(VectorRegister v1, VectorRegister v2);
  inline void z_vistrhs(VectorRegister v1, VectorRegister v2);
  inline void z_vistrfs(VectorRegister v1, VectorRegister v2);


  // Floatingpoint instructions
  // ==========================

  // compare instructions
  inline void z_cebr(FloatRegister r1, FloatRegister r2);                     // compare (r1, r2)                ; float
  inline void z_ceb(FloatRegister r1, int64_t d2, Register x2, Register b2);  // compare (r1, *(d2_imm12+x2+b2)) ; float
  inline void z_ceb(FloatRegister r1, const Address &a);                      // compare (r1, *(d2_imm12+x2+b2)) ; float
  inline void z_cdbr(FloatRegister r1, FloatRegister r2);                     // compare (r1, r2)                ; double
  inline void z_cdb(FloatRegister r1, int64_t d2, Register x2, Register b2);  // compare (r1, *(d2_imm12+x2+b2)) ; double
  inline void z_cdb(FloatRegister r1, const Address &a);                      // compare (r1, *(d2_imm12+x2+b2)) ; double

  // load instructions
  inline void z_le( FloatRegister r1, int64_t d2, Register x2, Register b2);   // load r1 = *(d2_uimm12+x2+b2) ; float
  inline void z_ley(FloatRegister r1, int64_t d2, Register x2, Register b2);   // load r1 = *(d2_imm20+x2+b2)  ; float
  inline void z_ld( FloatRegister r1, int64_t d2, Register x2, Register b2);   // load r1 = *(d2_uimm12+x2+b2) ; double
  inline void z_ldy(FloatRegister r1, int64_t d2, Register x2, Register b2);   // load r1 = *(d2_imm20+x2+b2)  ; double
  inline void z_le( FloatRegister r1, const Address &a);                       // load r1 = *(a)               ; float
  inline void z_ley(FloatRegister r1, const Address &a);                       // load r1 = *(a)               ; float
  inline void z_ld( FloatRegister r1, const Address &a);                       // load r1 = *(a)               ; double
  inline void z_ldy(FloatRegister r1, const Address &a);                       // load r1 = *(a)               ; double

  // store instructions
  inline void z_ste( FloatRegister r1, int64_t d2, Register x2, Register b2);  // store *(d2_uimm12+x2+b2) = r1  ; float
  inline void z_stey(FloatRegister r1, int64_t d2, Register x2, Register b2);  // store *(d2_imm20+x2+b2)  = r1  ; float
  inline void z_std( FloatRegister r1, int64_t d2, Register x2, Register b2);  // store *(d2_uimm12+x2+b2) = r1  ; double
  inline void z_stdy(FloatRegister r1, int64_t d2, Register x2, Register b2);  // store *(d2_imm20+x2+b2)  = r1  ; double
  inline void z_ste( FloatRegister r1, const Address &a);                      // store *(a)               = r1  ; float
  inline void z_stey(FloatRegister r1, const Address &a);                      // store *(a)               = r1  ; float
  inline void z_std( FloatRegister r1, const Address &a);                      // store *(a)               = r1  ; double
  inline void z_stdy(FloatRegister r1, const Address &a);                      // store *(a)               = r1  ; double

  // load and store immediates
  inline void z_lzer(FloatRegister r1);                                 // r1 = 0     ; single
  inline void z_lzdr(FloatRegister r1);                                 // r1 = 0     ; double

  // Move and Convert instructions
  inline void z_ler(FloatRegister r1, FloatRegister r2);                // move         r1 = r2 ; float
  inline void z_ldr(FloatRegister r1, FloatRegister r2);                // move         r1 = r2 ; double
  inline void z_ledbr(FloatRegister r1, FloatRegister r2);              // conv / round r1 = r2 ; float <- double
  inline void z_ldebr(FloatRegister r1, FloatRegister r2);              // conv         r1 = r2 ; double <- float

  // move between integer and float registers
  inline void z_cefbr( FloatRegister r1, Register r2);                  // r1 = r2; float  <-- int32
  inline void z_cdfbr( FloatRegister r1, Register r2);                  // r1 = r2; double <-- int32
  inline void z_cegbr( FloatRegister r1, Register r2);                  // r1 = r2; float  <-- int64
  inline void z_cdgbr( FloatRegister r1, Register r2);                  // r1 = r2; double <-- int64

  // rounding mode for float-2-int conversions
  inline void z_cfebr(Register r1, FloatRegister r2, RoundingMode m);   // conv r1 = r2  ; int32 <-- float
  inline void z_cfdbr(Register r1, FloatRegister r2, RoundingMode m);   // conv r1 = r2  ; int32 <-- double
  inline void z_cgebr(Register r1, FloatRegister r2, RoundingMode m);   // conv r1 = r2  ; int64 <-- float
  inline void z_cgdbr(Register r1, FloatRegister r2, RoundingMode m);   // conv r1 = r2  ; int64 <-- double

  inline void z_ldgr(FloatRegister r1, Register r2);   // fr1 = r2  ; what kind of conversion?  -- z10
  inline void z_lgdr(Register r1, FloatRegister r2);   // r1  = fr2 ; what kind of conversion?  -- z10


  // ADD
  inline void z_aebr(FloatRegister f1, FloatRegister f2);                      // f1 = f1 + f2               ; float
  inline void z_adbr(FloatRegister f1, FloatRegister f2);                      // f1 = f1 + f2               ; double
  inline void z_aeb( FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 + *(d2+x2+b2)      ; float
  inline void z_adb( FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 + *(d2+x2+b2)      ; double
  inline void z_aeb( FloatRegister f1, const Address& a);                      // f1 = f1 + *(a)             ; float
  inline void z_adb( FloatRegister f1, const Address& a);                      // f1 = f1 + *(a)             ; double

  // SUB
  inline void z_sebr(FloatRegister f1, FloatRegister f2);                      // f1 = f1 - f2               ; float
  inline void z_sdbr(FloatRegister f1, FloatRegister f2);                      // f1 = f1 - f2               ; double
  inline void z_seb( FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 - *(d2+x2+b2)      ; float
  inline void z_sdb( FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 - *(d2+x2+b2)      ; double
  inline void z_seb( FloatRegister f1, const Address& a);                      // f1 = f1 - *(a)             ; float
  inline void z_sdb( FloatRegister f1, const Address& a);                      // f1 = f1 - *(a)             ; double
  // negate
  inline void z_lcebr(FloatRegister r1, FloatRegister r2);                     // neg r1 = -r2   ; float
  inline void z_lcdbr(FloatRegister r1, FloatRegister r2);                     // neg r1 = -r2   ; double

  // Absolute value, monadic if fr2 == noreg.
  inline void z_lpdbr( FloatRegister fr1, FloatRegister fr2 = fnoreg);         // fr1 = |fr2|


  // MUL
  inline void z_meebr(FloatRegister f1, FloatRegister f2);                      // f1 = f1 * f2               ; float
  inline void z_mdbr( FloatRegister f1, FloatRegister f2);                      // f1 = f1 * f2               ; double
  inline void z_meeb( FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 * *(d2+x2+b2)      ; float
  inline void z_mdb(  FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 * *(d2+x2+b2)      ; double
  inline void z_meeb( FloatRegister f1, const Address& a);
  inline void z_mdb(  FloatRegister f1, const Address& a);

  // MUL-ADD
  inline void z_maebr(FloatRegister f1, FloatRegister f3, FloatRegister f2);    // f1 = f3 * f2 + f1          ; float
  inline void z_madbr(FloatRegister f1, FloatRegister f3, FloatRegister f2);    // f1 = f3 * f2 + f1          ; double
  inline void z_msebr(FloatRegister f1, FloatRegister f3, FloatRegister f2);    // f1 = f3 * f2 - f1          ; float
  inline void z_msdbr(FloatRegister f1, FloatRegister f3, FloatRegister f2);    // f1 = f3 * f2 - f1          ; double
  inline void z_maeb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2); // f1 = f3 * *(d2+x2+b2) + f1 ; float
  inline void z_madb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2); // f1 = f3 * *(d2+x2+b2) + f1 ; double
  inline void z_mseb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2); // f1 = f3 * *(d2+x2+b2) - f1 ; float
  inline void z_msdb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2); // f1 = f3 * *(d2+x2+b2) - f1 ; double
  inline void z_maeb(FloatRegister f1, FloatRegister f3, const Address& a);
  inline void z_madb(FloatRegister f1, FloatRegister f3, const Address& a);
  inline void z_mseb(FloatRegister f1, FloatRegister f3, const Address& a);
  inline void z_msdb(FloatRegister f1, FloatRegister f3, const Address& a);

  // DIV
  inline void z_debr( FloatRegister f1, FloatRegister f2);                      // f1 = f1 / f2               ; float
  inline void z_ddbr( FloatRegister f1, FloatRegister f2);                      // f1 = f1 / f2               ; double
  inline void z_deb(  FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 / *(d2+x2+b2)      ; float
  inline void z_ddb(  FloatRegister f1, int64_t d2, Register x2, Register b2);  // f1 = f1 / *(d2+x2+b2)      ; double
  inline void z_deb(  FloatRegister f1, const Address& a);                      // f1 = f1 / *(a)             ; float
  inline void z_ddb(  FloatRegister f1, const Address& a);                      // f1 = f1 / *(a)             ; double

  // square root
  inline void z_sqdbr(FloatRegister fr1, FloatRegister fr2);                    // fr1 = sqrt(fr2)            ; double
  inline void z_sqdb( FloatRegister fr1, int64_t d2, Register x2, Register b2); // fr1 = srqt( *(d2+x2+b2)
  inline void z_sqdb( FloatRegister fr1, int64_t d2, Register b2);              // fr1 = srqt( *(d2+b2)

  // Nop instruction
  // ===============

  // branch never (nop)
  inline void z_nop();
  inline void nop(); // Used by shared code.

  // ===============================================================================================

  // Simplified emitters:
  // ====================


  // Some memory instructions without index register (just convenience).
  inline void z_layz(Register r1, int64_t d2, Register b2 = Z_R0);
  inline void z_lay(Register r1, int64_t d2, Register b2);
  inline void z_laz(Register r1, int64_t d2, Register b2);
  inline void z_la(Register r1, int64_t d2, Register b2);
  inline void z_l(Register r1, int64_t d2, Register b2);
  inline void z_ly(Register r1, int64_t d2, Register b2);
  inline void z_lg(Register r1, int64_t d2, Register b2);
  inline void z_st(Register r1, int64_t d2, Register b2);
  inline void z_sty(Register r1, int64_t d2, Register b2);
  inline void z_stg(Register r1, int64_t d2, Register b2);
  inline void z_lgf(Register r1, int64_t d2, Register b2);
  inline void z_lgh(Register r1, int64_t d2, Register b2);
  inline void z_llgh(Register r1, int64_t d2, Register b2);
  inline void z_llgf(Register r1, int64_t d2, Register b2);
  inline void z_lgb(Register r1, int64_t d2, Register b2);
  inline void z_cl( Register r1, int64_t d2, Register b2);
  inline void z_c(Register r1, int64_t d2, Register b2);
  inline void z_cg(Register r1, int64_t d2, Register b2);
  inline void z_sh(Register r1, int64_t d2, Register b2);
  inline void z_shy(Register r1, int64_t d2, Register b2);
  inline void z_ste(FloatRegister r1, int64_t d2, Register b2);
  inline void z_std(FloatRegister r1, int64_t d2, Register b2);
  inline void z_stdy(FloatRegister r1, int64_t d2, Register b2);
  inline void z_stey(FloatRegister r1, int64_t d2, Register b2);
  inline void z_ld(FloatRegister r1, int64_t d2, Register b2);
  inline void z_ldy(FloatRegister r1, int64_t d2, Register b2);
  inline void z_le(FloatRegister r1, int64_t d2, Register b2);
  inline void z_ley(FloatRegister r1, int64_t d2, Register b2);

  inline void z_agf(Register r1, int64_t d2, Register b2);

  inline void z_exrl(Register r1, Label& L);
  inline void z_larl(Register r1, Label& L);
  inline void z_bru( Label& L);
  inline void z_brul(Label& L);
  inline void z_brul(address a);
  inline void z_brh( Label& L);
  inline void z_brl( Label& L);
  inline void z_bre( Label& L);
  inline void z_brnh(Label& L);
  inline void z_brnl(Label& L);
  inline void z_brne(Label& L);
  inline void z_brz( Label& L);
  inline void z_brnz(Label& L);
  inline void z_brnaz(Label& L);
  inline void z_braz(Label& L);
  inline void z_brnp(Label& L);

  inline void z_btrue( Label& L);
  inline void z_bfalse(Label& L);

  inline void z_bvat(Label& L);   // all true
  inline void z_bvnt(Label& L);   // not all true (mixed or all false)
  inline void z_bvmix(Label& L);  // mixed true and false
  inline void z_bvnf(Label& L);   // not all false (mixed or all true)
  inline void z_bvaf(Label& L);   // all false

  inline void z_brno( Label& L);


  inline void z_basr(Register r1, Register r2);
  inline void z_brasl(Register r1, address a);
  inline void z_brct(Register r1, address a);
  inline void z_brct(Register r1, Label& L);

  inline void z_brxh(Register r1, Register r3, address a);
  inline void z_brxh(Register r1, Register r3, Label& L);

  inline void z_brxle(Register r1, Register r3, address a);
  inline void z_brxle(Register r1, Register r3, Label& L);

  inline void z_brxhg(Register r1, Register r3, address a);
  inline void z_brxhg(Register r1, Register r3, Label& L);

  inline void z_brxlg(Register r1, Register r3, address a);
  inline void z_brxlg(Register r1, Register r3, Label& L);

  // Ppopulation count intrinsics.
  inline void z_flogr(Register r1, Register r2);    // find leftmost one
  inline void z_popcnt(Register r1, Register r2);   // population count
  inline void z_ahhhr(Register r1, Register r2, Register r3);   // ADD halfword high high
  inline void z_ahhlr(Register r1, Register r2, Register r3);   // ADD halfword high low

  inline void z_tam();
  inline void z_stckf(int64_t d2, Register b2);
  inline void z_stm( Register r1, Register r3, int64_t d2, Register b2);
  inline void z_stmy(Register r1, Register r3, int64_t d2, Register b2);
  inline void z_stmg(Register r1, Register r3, int64_t d2, Register b2);
  inline void z_lm( Register r1, Register r3, int64_t d2, Register b2);
  inline void z_lmy(Register r1, Register r3, int64_t d2, Register b2);
  inline void z_lmg(Register r1, Register r3, int64_t d2, Register b2);

  inline void z_cs( Register r1, Register r3, int64_t d2, Register b2);
  inline void z_csy(Register r1, Register r3, int64_t d2, Register b2);
  inline void z_csg(Register r1, Register r3, int64_t d2, Register b2);
  inline void z_cs( Register r1, Register r3, const Address& a);
  inline void z_csy(Register r1, Register r3, const Address& a);
  inline void z_csg(Register r1, Register r3, const Address& a);

  inline void z_cvd(Register r1, int64_t d2, Register x2, Register b2);
  inline void z_cvdg(Register r1, int64_t d2, Register x2, Register b2);
  inline void z_cvd(Register r1, int64_t d2, Register b2);
  inline void z_cvdg(Register r1, int64_t d2, Register b2);

  // Instruction queries:
  // instruction properties and recognize emitted instructions
  // ===========================================================

  static int nop_size() { return 2; }

  static int z_brul_size() { return 6; }

  static bool is_z_basr(short x) {
    return (BASR_ZOPC == (x & BASR_MASK));
  }
  static bool is_z_algr(long x) {
    return (ALGR_ZOPC == (x & RRE_MASK));
  }
  static bool is_z_lb(long x) {
    return (LB_ZOPC == (x & LB_MASK));
  }
  static bool is_z_lh(int x) {
    return (LH_ZOPC == (x & LH_MASK));
  }
  static bool is_z_l(int x) {
    return (L_ZOPC == (x & L_MASK));
  }
  static bool is_z_lgr(long x) {
    return (LGR_ZOPC == (x & RRE_MASK));
  }
  static bool is_z_ly(long x) {
    return (LY_ZOPC == (x & LY_MASK));
  }
  static bool is_z_lg(long x) {
    return (LG_ZOPC == (x & LG_MASK));
  }
  static bool is_z_llgh(long x) {
    return (LLGH_ZOPC == (x & LLGH_MASK));
  }
  static bool is_z_llgf(long x) {
    return (LLGF_ZOPC == (x & LLGF_MASK));
  }
  static bool is_z_le(int x) {
    return (LE_ZOPC == (x & LE_MASK));
  }
  static bool is_z_ld(int x) {
    return (LD_ZOPC == (x & LD_MASK));
  }
  static bool is_z_st(int x) {
    return (ST_ZOPC == (x & ST_MASK));
  }
  static bool is_z_stc(int x) {
    return (STC_ZOPC == (x & STC_MASK));
  }
  static bool is_z_stg(long x) {
    return (STG_ZOPC == (x & STG_MASK));
  }
  static bool is_z_sth(int x) {
    return (STH_ZOPC == (x & STH_MASK));
  }
  static bool is_z_ste(int x) {
    return (STE_ZOPC == (x & STE_MASK));
  }
  static bool is_z_std(int x) {
    return (STD_ZOPC == (x & STD_MASK));
  }
  static bool is_z_slag(long x) {
    return (SLAG_ZOPC == (x & SLAG_MASK));
  }
  static bool is_z_tmy(long x) {
    return (TMY_ZOPC == (x & TMY_MASK));
  }
  static bool is_z_tm(long x) {
    return ((unsigned int)TM_ZOPC == (x & (unsigned int)TM_MASK));
  }
  static bool is_z_bcr(long x) {
    return (BCR_ZOPC == (x & BCR_MASK));
  }
  static bool is_z_nop(long x) {
    return is_z_bcr(x) && ((x & 0x00ff) == 0);
  }
  static bool is_z_nop(address x) {
    return is_z_nop(* (short *) x);
  }
  static bool is_z_br(long x) {
    return is_z_bcr(x) && ((x & 0x00f0) == 0x00f0);
  }
  static bool is_z_brc(long x, int cond) {
    return ((unsigned int)BRC_ZOPC == (x & BRC_MASK)) && ((cond<<20) == (x & 0x00f00000U));
  }
  // Make use of lightweight sync.
  static bool is_z_sync_full(long x) {
    return is_z_bcr(x) && (((x & 0x00f0)>>4)==bcondFullSync) && ((x & 0x000f)==0x0000);
  }
  static bool is_z_sync_light(long x) {
    return is_z_bcr(x) && (((x & 0x00f0)>>4)==bcondLightSync) && ((x & 0x000f)==0x0000);
  }
  static bool is_z_sync(long x) {
    return is_z_sync_full(x) || is_z_sync_light(x);
  }

  static bool is_z_brasl(long x) {
    return (BRASL_ZOPC == (x & BRASL_MASK));
  }
  static bool is_z_brasl(address a) {
  long x = (*((long *)a))>>16;
   return is_z_brasl(x);
  }
  static bool is_z_larl(long x) {
    return (LARL_ZOPC == (x & LARL_MASK));
  }
  static bool is_z_lgrl(long x) {
    return (LGRL_ZOPC == (x & LGRL_MASK));
  }
  static bool is_z_lgrl(address a) {
  long x = (*((long *)a))>>16;
   return is_z_lgrl(x);
  }

  static bool is_z_lghi(unsigned long x) {
    return (unsigned int)LGHI_ZOPC == (x & (unsigned int)LGHI_MASK);
  }

  static bool is_z_llill(unsigned long x) {
    return (unsigned int)LLILL_ZOPC == (x & (unsigned int)LLI_MASK);
  }
  static bool is_z_llilh(unsigned long x) {
    return (unsigned int)LLILH_ZOPC == (x & (unsigned int)LLI_MASK);
  }
  static bool is_z_llihl(unsigned long x) {
    return (unsigned int)LLIHL_ZOPC == (x & (unsigned int)LLI_MASK);
  }
  static bool is_z_llihh(unsigned long x) {
    return (unsigned int)LLIHH_ZOPC == (x & (unsigned int)LLI_MASK);
  }
  static bool is_z_llilf(unsigned long x) {
    return LLILF_ZOPC == (x & LLIF_MASK);
  }
  static bool is_z_llihf(unsigned long x) {
    return LLIHF_ZOPC == (x & LLIF_MASK);
  }

  static bool is_z_iill(unsigned long x) {
    return (unsigned int)IILL_ZOPC == (x & (unsigned int)II_MASK);
  }
  static bool is_z_iilh(unsigned long x) {
    return (unsigned int)IILH_ZOPC == (x & (unsigned int)II_MASK);
  }
  static bool is_z_iihl(unsigned long x) {
    return (unsigned int)IIHL_ZOPC == (x & (unsigned int)II_MASK);
  }
  static bool is_z_iihh(unsigned long x) {
    return (unsigned int)IIHH_ZOPC == (x & (unsigned int)II_MASK);
  }
  static bool is_z_iilf(unsigned long x) {
    return IILF_ZOPC == (x & IIF_MASK);
  }
  static bool is_z_iihf(unsigned long x) {
    return IIHF_ZOPC == (x & IIF_MASK);
  }

  static inline bool is_equal(unsigned long inst, unsigned long idef);
  static inline bool is_equal(unsigned long inst, unsigned long idef, unsigned long imask);
  static inline bool is_equal(address iloc, unsigned long idef);
  static inline bool is_equal(address iloc, unsigned long idef, unsigned long imask);

  static inline bool is_sigtrap_range_check(address pc);
  static inline bool is_sigtrap_zero_check(address pc);

  //-----------------
  // memory barriers
  //-----------------
  // machine barrier instructions:
  //
  // - z_sync            Two-way memory barrier, aka fence.
  //                     Only load-after-store-order is not guaranteed in the
  //                     z/Architecture memory model, i.e. only 'fence' is needed.
  //
  // semantic barrier instructions:
  // (as defined in orderAccess.hpp)
  //
  // - z_release         orders Store|Store,   empty implementation
  //                            Load|Store
  // - z_acquire         orders Load|Store,    empty implementation
  //                            Load|Load
  // - z_fence           orders Store|Store,   implemented as z_sync.
  //                            Load|Store,
  //                            Load|Load,
  //                            Store|Load
  //
  // For this implementation to be correct, we need H/W fixes on (very) old H/W:
  //          For z990, it is Driver-55:  MCL232 in the J13484 (i390/ML) Stream.
  //          For z9,   it is Driver-67:  MCL065 in the G40963 (i390/ML) Stream.
  // These drivers are a prereq. Otherwise, memory synchronization will not work.

  inline void z_sync();
  inline void z_release();
  inline void z_acquire();
  inline void z_fence();

  // Creation
  Assembler(CodeBuffer* code) : AbstractAssembler(code) { }

};

#endif // CPU_S390_ASSEMBLER_S390_HPP
