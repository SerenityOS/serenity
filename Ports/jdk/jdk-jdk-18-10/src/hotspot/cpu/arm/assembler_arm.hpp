/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_ASSEMBLER_ARM_HPP
#define CPU_ARM_ASSEMBLER_ARM_HPP

#include "utilities/macros.hpp"

enum AsmCondition {
  eq, ne, cs, cc, mi, pl, vs, vc,
  hi, ls, ge, lt, gt, le, al, nv,
  number_of_conditions,
  // alternative names
  hs = cs,
  lo = cc
};

enum AsmShift {
  lsl, lsr, asr, ror
};


enum AsmOffset {
  basic_offset = 1 << 24,
  pre_indexed  = 1 << 24 | 1 << 21,
  post_indexed = 0
};


enum AsmWriteback {
  no_writeback,
  writeback
};

enum AsmOffsetOp {
  sub_offset = 0,
  add_offset = 1
};


// ARM Addressing Modes 2 and 3 - Load and store
class Address {
 private:
  Register  _base;
  Register  _index;
  int       _disp;
  AsmOffset _mode;
  RelocationHolder   _rspec;
  int       _shift_imm;
  AsmShift  _shift;
  AsmOffsetOp _offset_op;

  static inline int abs(int x) { return x < 0 ? -x : x; }
  static inline int up (int x) { return x < 0 ?  0 : 1; }

  static const AsmShift LSL = lsl;

 public:
  Address() : _base(noreg) {}

  Address(Register rn, int offset = 0, AsmOffset mode = basic_offset) {
    _base = rn;
    _index = noreg;
    _disp = offset;
    _mode = mode;
    _shift_imm = 0;
    _shift = lsl;
    _offset_op = add_offset;
  }

  Address(Register rn, ByteSize offset, AsmOffset mode = basic_offset) :
    Address(rn, in_bytes(offset), mode) {}

  Address(Register rn, Register rm, AsmShift shift = lsl,
          int shift_imm = 0, AsmOffset mode = basic_offset,
          AsmOffsetOp offset_op = add_offset) {
    _base = rn;
    _index = rm;
    _disp = 0;
    _shift = shift;
    _shift_imm = shift_imm;
    _mode = mode;
    _offset_op = offset_op;
  }

  Address(Register rn, RegisterOrConstant offset, AsmShift shift = lsl,
          int shift_imm = 0) {
    _base = rn;
    if (offset.is_constant()) {
      _index = noreg;
      {
        int off = (int) offset.as_constant();
        if (shift_imm != 0) {
          assert(shift == lsl,"shift not yet encoded");
          off =  off << shift_imm;
        }
        _disp = off;
      }
      _shift = lsl;
      _shift_imm = 0;
    } else {
      _index = offset.as_register();
      _disp = 0;
      _shift = shift;
      _shift_imm = shift_imm;
    }
    _mode = basic_offset;
    _offset_op = add_offset;
  }

  // [base + index * wordSize]
  static Address indexed_ptr(Register base, Register index) {
    return Address(base, index, LSL, LogBytesPerWord);
  }

  // [base + index * BytesPerInt]
  static Address indexed_32(Register base, Register index) {
    return Address(base, index, LSL, LogBytesPerInt);
  }

  // [base + index * BytesPerHeapOop]
  static Address indexed_oop(Register base, Register index) {
    return Address(base, index, LSL, LogBytesPerHeapOop);
  }

  Address plus_disp(int disp) const {
    assert((disp == 0) || (_index == noreg),"can't apply an offset to a register indexed address");
    Address a = (*this);
    a._disp += disp;
    return a;
  }

  Address rebase(Register new_base) const {
    Address a = (*this);
    a._base = new_base;
    return a;
  }

  int encoding2() const {
    assert(_mode == basic_offset || _base != PC, "unpredictable instruction");
    if (_index == noreg) {
      assert(-4096 < _disp && _disp < 4096, "encoding constraint");
      return _mode | up(_disp) << 23 | _base->encoding() << 16 | abs(_disp);
    } else {
      assert(_index != PC && (_mode == basic_offset || _index != _base), "unpredictable instruction");
      assert(_disp == 0 && (_shift_imm >> 5) == 0, "encoding constraint");
      return 1 << 25 | _offset_op << 23 | _mode | _base->encoding() << 16 |
             _shift_imm << 7 | _shift << 5 | _index->encoding();
    }
  }

  int encoding3() const {
    assert(_mode == basic_offset || _base != PC, "unpredictable instruction");
    if (_index == noreg) {
      assert(-256 < _disp && _disp < 256, "encoding constraint");
      return _mode | up(_disp) << 23 | 1 << 22 | _base->encoding() << 16 |
             (abs(_disp) & 0xf0) << 4 | abs(_disp) & 0x0f;
    } else {
      assert(_index != PC && (_mode == basic_offset || _index != _base), "unpredictable instruction");
      assert(_disp == 0 && _shift == lsl && _shift_imm == 0, "encoding constraint");
      return _mode | _offset_op << 23 | _base->encoding() << 16 | _index->encoding();
    }
  }

  int encoding_ex() const {
    assert(_index == noreg && _disp == 0 && _mode == basic_offset &&
           _base != PC, "encoding constraint");
    return _base->encoding() << 16;
  }

  int encoding_vfp() const {
    assert(_index == noreg && _mode == basic_offset, "encoding constraint");
    assert(-1024 < _disp && _disp < 1024 && (_disp & 3) == 0, "encoding constraint");
    return _base->encoding() << 16 | up(_disp) << 23 | abs(_disp) >> 2;
  }

  int encoding_simd() const {
    assert(_base != PC, "encoding constraint");
    assert(_index != PC && _index != SP, "encoding constraint");
    assert(_disp == 0, "encoding constraint");
    assert(_shift == 0, "encoding constraint");
    assert(_index == noreg || _mode == basic_offset, "encoding constraint");
    assert(_mode == basic_offset || _mode == post_indexed, "encoding constraint");
    int index;
    if (_index == noreg) {
      if (_mode == post_indexed)
        index = 13;
      else
        index = 15;
    } else {
      index = _index->encoding();
    }

    return _base->encoding() << 16 | index;
  }

  Register base() const {
    return _base;
  }

  Register index() const {
    return _index;
  }

  int disp() const {
    return _disp;
  }

  AsmOffset mode() const {
    return _mode;
  }

  int shift_imm() const {
    return _shift_imm;
  }

  AsmShift shift() const {
    return _shift;
  }

  AsmOffsetOp offset_op() const {
    return _offset_op;
  }

  bool uses(Register reg) const { return _base == reg || _index == reg; }

  const relocInfo::relocType rtype() { return _rspec.type(); }
  const RelocationHolder&    rspec() { return _rspec; }

  // Convert the raw encoding form into the form expected by the
  // constructor for Address.
  static Address make_raw(int base, int index, int scale, int disp, relocInfo::relocType disp_reloc);
};

#ifdef COMPILER2
class VFP {
  // Helper classes to detect whether a floating point constant can be
  // encoded in a fconstd or fconsts instruction
  // The conversion from the imm8, 8 bit constant, to the floating
  // point value encoding is done with either:
  // for single precision: imm8<7>:NOT(imm8<6>):Replicate(imm8<6>,5):imm8<5:0>:Zeros(19)
  // or
  // for double precision: imm8<7>:NOT(imm8<6>):Replicate(imm8<6>,8):imm8<5:0>:Zeros(48)

 private:
  class fpnum {
   public:
    virtual unsigned int f_hi4() const = 0;
    virtual bool f_lo_is_null() const = 0;
    virtual int e() const = 0;
    virtual unsigned int s() const = 0;

    inline bool can_be_imm8() const { return e() >= -3 && e() <= 4 && f_lo_is_null(); }
    inline unsigned char imm8() const { int v = (s() << 7) | (((e() - 1) & 0x7) << 4) | f_hi4(); assert((v >> 8) == 0, "overflow"); return v; }
  };

 public:
  class float_num : public fpnum {
   public:
    float_num(float v) {
      _num.val = v;
    }

    virtual unsigned int f_hi4() const { return (_num.bits << 9) >> (19+9); }
    virtual bool f_lo_is_null() const { return (_num.bits & ((1 << 19) - 1)) == 0; }
    virtual int e() const { return ((_num.bits << 1) >> (23+1)) - 127; }
    virtual unsigned int s() const { return _num.bits >> 31; }

   private:
    union {
      float val;
      unsigned int bits;
    } _num;
  };

  class double_num : public fpnum {
   public:
    double_num(double v) {
      _num.val = v;
    }

    virtual unsigned int f_hi4() const { return (_num.bits << 12) >> (48+12); }
    virtual bool f_lo_is_null() const { return (_num.bits & ((1LL << 48) - 1)) == 0; }
    virtual int e() const { return ((_num.bits << 1) >> (52+1)) - 1023; }
    virtual unsigned int s() const { return _num.bits >> 63; }

   private:
    union {
      double val;
      unsigned long long bits;
    } _num;
  };
};
#endif

#include "assembler_arm_32.hpp"


#endif // CPU_ARM_ASSEMBLER_ARM_HPP
