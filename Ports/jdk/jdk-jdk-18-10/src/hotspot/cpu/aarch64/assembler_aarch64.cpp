/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020 Red Hat Inc. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "asm/macroAssembler.hpp"
#include "compiler/disassembler.hpp"
#include "immediate_aarch64.hpp"
#include "memory/resourceArea.hpp"

#ifndef PRODUCT
const uintptr_t Assembler::asm_bp = 0x00007fffee09ac88;
#endif

static float unpack(unsigned value);

short Assembler::SIMD_Size_in_bytes[] = {
  // T8B, T16B, T4H, T8H, T2S, T4S, T1D, T2D, T1Q
       8,   16,   8,  16,   8,  16,   8,  16,  16
};

Assembler::SIMD_Arrangement Assembler::_esize2arrangement_table[9][2] = {
  // esize        isQ:false             isQ:true
  /*   0  */      {INVALID_ARRANGEMENT, INVALID_ARRANGEMENT},
  /*   1  */      {T8B,                 T16B},
  /*   2  */      {T4H,                 T8H},
  /*   3  */      {INVALID_ARRANGEMENT, INVALID_ARRANGEMENT},
  /*   4  */      {T2S,                 T4S},
  /*   5  */      {INVALID_ARRANGEMENT, INVALID_ARRANGEMENT},
  /*   6  */      {INVALID_ARRANGEMENT, INVALID_ARRANGEMENT},
  /*   7  */      {INVALID_ARRANGEMENT, INVALID_ARRANGEMENT},
  /*   8  */      {T1D,                 T2D}
  };


Assembler::SIMD_Arrangement Assembler::esize2arrangement(int esize, bool isQ) {
    guarantee(esize == 1 || esize == 2 || esize == 4 || esize == 8, "unsupported element size");
    return _esize2arrangement_table[esize][isQ];
}

void Assembler::emit_data64(jlong data,
                            relocInfo::relocType rtype,
                            int format) {
  if (rtype == relocInfo::none) {
    emit_int64(data);
  } else {
    emit_data64(data, Relocation::spec_simple(rtype), format);
  }
}

void Assembler::emit_data64(jlong data,
                            RelocationHolder const& rspec,
                            int format) {

  assert(inst_mark() != NULL, "must be inside InstructionMark");
  // Do not use AbstractAssembler::relocate, which is not intended for
  // embedded words.  Instead, relocate to the enclosing instruction.
  code_section()->relocate(inst_mark(), rspec, format);
  emit_int64(data);
}

extern "C" {
  void das(uint64_t start, int len) {
    ResourceMark rm;
    len <<= 2;
    if (len < 0)
      Disassembler::decode((address)start + len, (address)start);
    else
      Disassembler::decode((address)start, (address)start + len);
  }

  JNIEXPORT void das1(uintptr_t insn) {
    das(insn, 1);
  }
}

#define __ as->

void Address::lea(MacroAssembler *as, Register r) const {
  Relocation* reloc = _rspec.reloc();
  relocInfo::relocType rtype = (relocInfo::relocType) reloc->type();

  switch(_mode) {
  case base_plus_offset: {
    if (_offset == 0 && _base == r) // it's a nop
      break;
    if (_offset > 0)
      __ add(r, _base, _offset);
    else
      __ sub(r, _base, -_offset);
      break;
  }
  case base_plus_offset_reg: {
    __ add(r, _base, _index, _ext.op(), MAX2(_ext.shift(), 0));
    break;
  }
  case literal: {
    if (rtype == relocInfo::none)
      __ mov(r, target());
    else
      __ movptr(r, (uint64_t)target());
    break;
  }
  default:
    ShouldNotReachHere();
  }
}

void Assembler::adrp(Register reg1, const Address &dest, uint64_t &byte_offset) {
  ShouldNotReachHere();
}

#undef __

#define starti Instruction_aarch64 current_insn(this);

#define f current_insn.f
#define sf current_insn.sf
#define rf current_insn.rf
#define srf current_insn.srf
#define zrf current_insn.zrf
#define prf current_insn.prf
#define pgrf current_insn.pgrf
#define fixed current_insn.fixed

  void Assembler::adr(Register Rd, address adr) {
    intptr_t offset = adr - pc();
    int offset_lo = offset & 3;
    offset >>= 2;
    starti;
    f(0, 31), f(offset_lo, 30, 29), f(0b10000, 28, 24), sf(offset, 23, 5);
    rf(Rd, 0);
  }

  void Assembler::_adrp(Register Rd, address adr) {
    uint64_t pc_page = (uint64_t)pc() >> 12;
    uint64_t adr_page = (uint64_t)adr >> 12;
    intptr_t offset = adr_page - pc_page;
    int offset_lo = offset & 3;
    offset >>= 2;
    starti;
    f(1, 31), f(offset_lo, 30, 29), f(0b10000, 28, 24), sf(offset, 23, 5);
    rf(Rd, 0);
  }

// An "all-purpose" add/subtract immediate, per ARM documentation:
// A "programmer-friendly" assembler may accept a negative immediate
// between -(2^24 -1) and -1 inclusive, causing it to convert a
// requested ADD operation to a SUB, or vice versa, and then encode
// the absolute value of the immediate as for uimm24.
void Assembler::add_sub_immediate(Instruction_aarch64 &current_insn,
                                  Register Rd, Register Rn, unsigned uimm, int op,
                                  int negated_op) {
  bool sets_flags = op & 1;   // this op sets flags
  union {
    unsigned u;
    int imm;
  };
  u = uimm;
  bool shift = false;
  bool neg = imm < 0;
  if (neg) {
    imm = -imm;
    op = negated_op;
  }
  assert(Rd != sp || imm % 16 == 0, "misaligned stack");
  if (imm >= (1 << 11)
      && ((imm >> 12) << 12 == imm)) {
    imm >>= 12;
    shift = true;
  }
  f(op, 31, 29), f(0b10001, 28, 24), f(shift, 23, 22), f(imm, 21, 10);

  // add/subtract immediate ops with the S bit set treat r31 as zr;
  // with S unset they use sp.
  if (sets_flags)
    zrf(Rd, 0);
  else
    srf(Rd, 0);

  srf(Rn, 5);
}

#undef f
#undef sf
#undef rf
#undef srf
#undef zrf
#undef prf
#undef pgrf
#undef fixed

#undef starti

Address::Address(address target, relocInfo::relocType rtype) : _mode(literal){
  _is_lval = false;
  _target = target;
  switch (rtype) {
  case relocInfo::oop_type:
  case relocInfo::metadata_type:
    // Oops are a special case. Normally they would be their own section
    // but in cases like icBuffer they are literals in the code stream that
    // we don't have a section for. We use none so that we get a literal address
    // which is always patchable.
    break;
  case relocInfo::external_word_type:
    _rspec = external_word_Relocation::spec(target);
    break;
  case relocInfo::internal_word_type:
    _rspec = internal_word_Relocation::spec(target);
    break;
  case relocInfo::opt_virtual_call_type:
    _rspec = opt_virtual_call_Relocation::spec();
    break;
  case relocInfo::static_call_type:
    _rspec = static_call_Relocation::spec();
    break;
  case relocInfo::runtime_call_type:
    _rspec = runtime_call_Relocation::spec();
    break;
  case relocInfo::poll_type:
  case relocInfo::poll_return_type:
    _rspec = Relocation::spec_simple(rtype);
    break;
  case relocInfo::none:
    _rspec = RelocationHolder::none;
    break;
  default:
    ShouldNotReachHere();
    break;
  }
}

void Assembler::b(const Address &dest) {
  code_section()->relocate(pc(), dest.rspec());
  b(dest.target());
}

void Assembler::bl(const Address &dest) {
  code_section()->relocate(pc(), dest.rspec());
  bl(dest.target());
}

void Assembler::adr(Register r, const Address &dest) {
  code_section()->relocate(pc(), dest.rspec());
  adr(r, dest.target());
}

void Assembler::br(Condition cc, Label &L) {
  if (L.is_bound()) {
    br(cc, target(L));
  } else {
    L.add_patch_at(code(), locator());
    br(cc, pc());
  }
}

void Assembler::wrap_label(Label &L,
                                 Assembler::uncond_branch_insn insn) {
  if (L.is_bound()) {
    (this->*insn)(target(L));
  } else {
    L.add_patch_at(code(), locator());
    (this->*insn)(pc());
  }
}

void Assembler::wrap_label(Register r, Label &L,
                                 compare_and_branch_insn insn) {
  if (L.is_bound()) {
    (this->*insn)(r, target(L));
  } else {
    L.add_patch_at(code(), locator());
    (this->*insn)(r, pc());
  }
}

void Assembler::wrap_label(Register r, int bitpos, Label &L,
                                 test_and_branch_insn insn) {
  if (L.is_bound()) {
    (this->*insn)(r, bitpos, target(L));
  } else {
    L.add_patch_at(code(), locator());
    (this->*insn)(r, bitpos, pc());
  }
}

void Assembler::wrap_label(Label &L, prfop op, prefetch_insn insn) {
  if (L.is_bound()) {
    (this->*insn)(target(L), op);
  } else {
    L.add_patch_at(code(), locator());
    (this->*insn)(pc(), op);
  }
}

bool Assembler::operand_valid_for_add_sub_immediate(int64_t imm) {
  bool shift = false;
  uint64_t uimm = (uint64_t)uabs((jlong)imm);
  if (uimm < (1 << 12))
    return true;
  if (uimm < (1 << 24)
      && ((uimm >> 12) << 12 == uimm)) {
    return true;
  }
  return false;
}

bool Assembler::operand_valid_for_logical_immediate(bool is32, uint64_t imm) {
  return encode_logical_immediate(is32, imm) != 0xffffffff;
}

static uint64_t doubleTo64Bits(jdouble d) {
  union {
    jdouble double_value;
    uint64_t double_bits;
  };

  double_value = d;
  return double_bits;
}

bool Assembler::operand_valid_for_float_immediate(double imm) {
  // If imm is all zero bits we can use ZR as the source of a
  // floating-point value.
  if (doubleTo64Bits(imm) == 0)
    return true;

  // Otherwise try to encode imm then convert the encoded value back
  // and make sure it's the exact same bit pattern.
  unsigned result = encoding_for_fp_immediate(imm);
  return doubleTo64Bits(imm) == fp_immediate_for_encoding(result, true);
}

int AbstractAssembler::code_fill_byte() {
  return 0;
}

// n.b. this is implemented in subclass MacroAssembler
void Assembler::bang_stack_with_offset(int offset) { Unimplemented(); }


// and now the routines called by the assembler which encapsulate the
// above encode and decode functions

uint32_t
asm_util::encode_logical_immediate(bool is32, uint64_t imm)
{
  if (is32) {
    /* Allow all zeros or all ones in top 32-bits, so that
       constant expressions like ~1 are permitted. */
    if (imm >> 32 != 0 && imm >> 32 != 0xffffffff)
      return 0xffffffff;
    /* Replicate the 32 lower bits to the 32 upper bits.  */
    imm &= 0xffffffff;
    imm |= imm << 32;
  }

  return encoding_for_logical_immediate(imm);
}

unsigned Assembler::pack(double value) {
  float val = (float)value;
  unsigned result = encoding_for_fp_immediate(val);
  guarantee(unpack(result) == value,
            "Invalid floating-point immediate operand");
  return result;
}

// Packed operands for  Floating-point Move (immediate)

static float unpack(unsigned value) {
  union {
    unsigned ival;
    float val;
  };
  ival = fp_immediate_for_encoding(value, 0);
  return val;
}

address Assembler::locate_next_instruction(address inst) {
  return inst + Assembler::instruction_size;
}
