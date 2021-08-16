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

#ifndef CPU_ARM_ASSEMBLER_ARM_32_HPP
#define CPU_ARM_ASSEMBLER_ARM_32_HPP

// ARM Addressing Mode 1 - Data processing operands
class AsmOperand {
 private:
  int _encoding;

  void initialize_rotated_imm(unsigned int imm);

  void encode(int imm_8) {
    if ((imm_8 >> 8) == 0) {
      _encoding = 1 << 25 | imm_8;  // the most common case
    } else {
      initialize_rotated_imm((unsigned int)imm_8);  // slow case
    }
  }

  void encode(Register rm, AsmShift shift, int shift_imm) {
    assert((shift_imm >> 5) == 0, "encoding constraint");
    _encoding = shift_imm << 7 | shift << 5 | rm->encoding();
  }

 public:

  AsmOperand(Register reg) {
    _encoding = reg->encoding();
  }

  AsmOperand(int imm_8) {
    encode(imm_8);
  }

  AsmOperand(ByteSize bytesize_8) :
    AsmOperand(in_bytes(bytesize_8)) {}

  AsmOperand(Register rm, AsmShift shift, int shift_imm) {
    encode(rm,shift,shift_imm);
  }

  AsmOperand(Register rm, AsmShift shift, Register rs) {
    assert(rm != PC && rs != PC, "unpredictable instruction");
    _encoding = rs->encoding() << 8 | shift << 5 | 1 << 4 | rm->encoding();
  }

  AsmOperand(RegisterOrConstant offset, AsmShift shift = lsl, int shift_imm = 0) {
    if (offset.is_register()) {
      encode(offset.as_register(), shift, shift_imm);
    } else {
      assert(shift == lsl,"shift type not yet encoded");
      int imm_8 = ((int)offset.as_constant()) << shift_imm;
      encode(imm_8);
    }
  }

  int encoding() const {
    return _encoding;
  }

  bool is_immediate() const {
    return _encoding & (1 << 25) ? true : false;
  }

  Register base_register() const {
    assert(!is_immediate(), "is_immediate, no base reg");
    return as_Register(_encoding & 15);
  }

  static bool is_rotated_imm(unsigned int imm);
};


// ARM Addressing Mode 4 - Load and store multiple
class RegisterSet {
 private:
  int _encoding;

  RegisterSet(int encoding) {
    _encoding = encoding;
  }

 public:

  RegisterSet(Register reg) {
    _encoding = 1 << reg->encoding();
  }

  RegisterSet() {
    _encoding = 0;
  }

  RegisterSet(Register first, Register last) {
    assert(first < last, "encoding constraint");
    _encoding = (1 << (last->encoding() + 1)) - (1 << first->encoding());
  }

  friend RegisterSet operator | (const RegisterSet set1, const RegisterSet set2) {
    assert((set1._encoding & set2._encoding) == 0,
           "encoding constraint");
    return RegisterSet(set1._encoding | set2._encoding);
  }

  int encoding() const {
    return _encoding;
  }

  bool contains(Register reg) const {
    return (_encoding & (1 << reg->encoding())) != 0;
  }

  // number of registers in the set
  int size() const {
    int count = 0;
    unsigned int remaining = (unsigned int) _encoding;
    while (remaining != 0) {
      if ((remaining & 1) != 0) count++;
      remaining >>= 1;
    }
    return count;
  }
};

#if R9_IS_SCRATCHED
#define R9ifScratched RegisterSet(R9)
#else
#define R9ifScratched RegisterSet()
#endif

// ARM Addressing Mode 5 - Load and store multiple VFP registers
class FloatRegisterSet {
 private:
  int _encoding;

 public:

  FloatRegisterSet(FloatRegister reg) {
    if (reg->hi_bit() == 0) {
      _encoding = reg->hi_bits() << 12 | reg->lo_bit() << 22 | 1;
    } else {
      assert (reg->lo_bit() == 0, "impossible encoding");
      _encoding = reg->hi_bits() << 12 | reg->hi_bit() << 22 | 1;
    }
  }

  FloatRegisterSet(FloatRegister first, int count) {
    assert(count >= 1, "encoding constraint");
    if (first->hi_bit() == 0) {
      _encoding = first->hi_bits() << 12 | first->lo_bit() << 22 | count;
    } else {
      assert (first->lo_bit() == 0, "impossible encoding");
      _encoding = first->hi_bits() << 12 | first->hi_bit() << 22 | count;
    }
  }

  int encoding_s() const {
    return _encoding;
  }

  int encoding_d() const {
    assert((_encoding & 0xFF) <= 16, "no more than 16 double registers" );
    return (_encoding & 0xFFFFFF00) | ((_encoding & 0xFF) << 1);
  }

};


class Assembler : public AbstractAssembler  {

 public:

  static const int LogInstructionSize = 2;
  static const int InstructionSize    = 1 << LogInstructionSize;

  //---<  calculate length of instruction  >---
  // We just use the values set above.
  // instruction must start at passed address
  static unsigned int instr_len(unsigned char *instr) { return InstructionSize; }

  //---<  longest instructions  >---
  static unsigned int instr_maxlen() { return InstructionSize; }

  static inline AsmCondition inverse(AsmCondition cond) {
    assert ((cond != al) && (cond != nv), "AL and NV conditions cannot be inversed");
    return (AsmCondition)((int)cond ^ 1);
  }

  // Returns true if given value can be used as immediate in arithmetic (add/sub/cmp/cmn) instructions.
  static inline bool is_arith_imm_in_range(intx value) {
    return AsmOperand::is_rotated_imm(value);
  }

  // Arithmetic instructions

#define F(mnemonic, opcode) \
  void mnemonic(Register rd, Register rn, AsmOperand operand, AsmCondition cond = al) {    \
    emit_int32(cond << 28 | opcode << 21 | rn->encoding() << 16 |                          \
               rd->encoding() << 12 | operand.encoding());                                 \
  }                                                                                        \
  void mnemonic##s(Register rd, Register rn, AsmOperand operand, AsmCondition cond = al) { \
    emit_int32(cond << 28 | opcode << 21 | 1 << 20 | rn->encoding() << 16 |                \
               rd->encoding() << 12 | operand.encoding());                                 \
  }

  F(andr, 0)
  F(eor,  1)
  F(sub,  2)
  F(rsb,  3)
  F(add,  4)
  F(adc,  5)
  F(sbc,  6)
  F(rsc,  7)
  F(orr,  12)
  F(bic,  14)
#undef F

#define F(mnemonic, opcode) \
  void mnemonic(Register rn, AsmOperand operand, AsmCondition cond = al) {  \
    emit_int32(cond << 28 | opcode << 21 | 1 << 20 | rn->encoding() << 16 | \
              operand.encoding());                                          \
  }

  F(tst, 8)
  F(teq, 9)
  F(cmp, 10)
  F(cmn, 11)
#undef F

#define F(mnemonic, opcode) \
  void mnemonic(Register rd, AsmOperand operand, AsmCondition cond = al) {    \
    emit_int32(cond << 28 | opcode << 21 | rd->encoding() << 12 |             \
              operand.encoding());                                            \
  }                                                                           \
  void mnemonic##s(Register rd, AsmOperand operand, AsmCondition cond = al) { \
    emit_int32(cond << 28 | opcode << 21 | 1 << 20 | rd->encoding() << 12 |   \
              operand.encoding());                                            \
  }

  F(mov, 13)
  F(mvn, 15)
#undef F

  void msr(uint fields, AsmOperand operand, AsmCondition cond = al) {
    assert((operand.encoding() & (1<<25)) || ((operand.encoding() & 0xff0) == 0), "invalid addressing mode");
    emit_int32(cond << 28 | 1 << 24 | 1 << 21 | fields << 16 | 0xf << 12 | operand.encoding());
  }

  void mrs(uint fields, Register Rd, AsmCondition cond = al) {
    emit_int32(cond << 28 | 1 << 24 | (fields|0xf) << 16 | (Rd->encoding() << 12));
  }


  enum {
    CPSR = 0x00, CPSR_c = 0x01, CPSR_x = 0x02, CPSR_xc = 0x03,
    CPSR_s = 0x004, CPSR_sc = 0x05, CPSR_sx = 0x06, CPSR_sxc = 0x07,
    CPSR_f = 0x08, CPSR_fc = 0x09, CPSR_fx = 0x0a, CPSR_fxc = 0x0b,
    CPSR_fs = 0x0c, CPSR_fsc = 0x0d, CPSR_fsx = 0x0e, CPSR_fsxc = 0x0f,
    SPSR = 0x40, SPSR_c = 0x41, SPSR_x = 0x42, SPSR_xc = 0x43,
    SPSR_s = 0x44, SPSR_sc = 0x45, SPSR_sx = 0x46, SPSR_sxc = 0x47,
    SPSR_f = 0x48, SPSR_fc = 0x49, SPSR_fx = 0x4a, SPSR_fxc = 0x4b,
    SPSR_fs = 0x4c, SPSR_fsc = 0x4d, SPSR_fsx = 0x4e, SPSR_fsxc = 0x4f
  };

#define F(mnemonic, opcode) \
  void mnemonic(Register rdlo, Register rdhi, Register rm, Register rs,                  \
                AsmCondition cond = al) {                                                \
    emit_int32(cond << 28 | opcode << 21 | rdhi->encoding() << 16 |                      \
              rdlo->encoding() << 12 | rs->encoding() << 8 | 0x9 << 4 | rm->encoding()); \
  }                                                                                      \
  void mnemonic##s(Register rdlo, Register rdhi, Register rm, Register rs,               \
                   AsmCondition cond = al) {                                             \
    emit_int32(cond << 28 | opcode << 21 | 1 << 20 | rdhi->encoding() << 16 |            \
              rdlo->encoding() << 12 | rs->encoding() << 8 | 0x9 << 4 | rm->encoding()); \
  }

  F(umull, 4)
  F(umlal, 5)
  F(smull, 6)
  F(smlal, 7)
#undef F

  void mul(Register rd, Register rm, Register rs, AsmCondition cond = al) {
    emit_int32(cond << 28 | rd->encoding() << 16 |
              rs->encoding() << 8 | 0x9 << 4 | rm->encoding());
  }

  void muls(Register rd, Register rm, Register rs, AsmCondition cond = al) {
    emit_int32(cond << 28 | 1 << 20 | rd->encoding() << 16 |
              rs->encoding() << 8 | 0x9 << 4 | rm->encoding());
  }

  void mla(Register rd, Register rm, Register rs, Register rn, AsmCondition cond = al) {
    emit_int32(cond << 28 | 1 << 21 | rd->encoding() << 16 |
              rn->encoding() << 12 | rs->encoding() << 8 | 0x9 << 4 | rm->encoding());
  }

  void mlas(Register rd, Register rm, Register rs, Register rn, AsmCondition cond = al) {
    emit_int32(cond << 28 | 1 << 21 | 1 << 20 | rd->encoding() << 16 |
              rn->encoding() << 12 | rs->encoding() << 8 | 0x9 << 4 | rm->encoding());
  }

  // Loads and stores

#define F(mnemonic, l, b) \
  void mnemonic(Register rd, Address addr, AsmCondition cond = al) { \
    emit_int32(cond << 28 | 1 << 26 | b << 22 | l << 20 |            \
              rd->encoding() << 12 | addr.encoding2());              \
  }

  F(ldr,  1, 0)
  F(ldrb, 1, 1)
  F(str,  0, 0)
  F(strb, 0, 1)
#undef F

#undef F

#define F(mnemonic, l, sh, even) \
  void mnemonic(Register rd, Address addr, AsmCondition cond = al) { \
    assert(!even || (rd->encoding() & 1) == 0, "must be even");      \
    emit_int32(cond << 28 | l << 20 | rd->encoding() << 12 |         \
              1 << 7 | sh << 5 | 1 << 4 | addr.encoding3());         \
  }

  F(strh,  0, 1, false)
  F(ldrh,  1, 1, false)
  F(ldrsb, 1, 2, false)
  F(ldrsh, 1, 3, false)
  F(strd,  0, 3, true)

#undef F

  void ldrd(Register rd, Address addr, AsmCondition cond = al) {
    assert((rd->encoding() & 1) == 0, "must be even");
    assert(!addr.index()->is_valid() ||
           (addr.index()->encoding() != rd->encoding() &&
            addr.index()->encoding() != (rd->encoding()+1)), "encoding constraint");
    emit_int32(cond << 28 | rd->encoding() << 12 | 0xD /* 0b1101 */ << 4 | addr.encoding3());
  }

#define F(mnemonic, l, pu) \
  void mnemonic(Register rn, RegisterSet reg_set,                        \
                AsmWriteback w = no_writeback, AsmCondition cond = al) { \
    assert(reg_set.encoding() != 0 && (w == no_writeback ||              \
           (reg_set.encoding() & (1 << rn->encoding())) == 0),           \
           "unpredictable instruction");                                 \
    emit_int32(cond << 28 | 4 << 25 | pu << 23 | w << 21 | l << 20 |     \
              rn->encoding() << 16 | reg_set.encoding());                \
  }

  F(ldmda, 1, 0)    F(ldmfa, 1, 0)
  F(ldmia, 1, 1)    F(ldmfd, 1, 1)
  F(ldmdb, 1, 2)    F(ldmea, 1, 2)
  F(ldmib, 1, 3)    F(ldmed, 1, 3)
  F(stmda, 0, 0)    F(stmed, 0, 0)
  F(stmia, 0, 1)    F(stmea, 0, 1)
  F(stmdb, 0, 2)    F(stmfd, 0, 2)
  F(stmib, 0, 3)    F(stmfa, 0, 3)
#undef F

  void ldrex(Register rd, Address addr, AsmCondition cond = al) {
    assert(rd != PC, "unpredictable instruction");
    emit_int32(cond << 28 | 0x19 << 20 | addr.encoding_ex() |
              rd->encoding()  << 12 | 0xf9f);
  }

  void strex(Register rs, Register rd, Address addr, AsmCondition cond = al) {
    assert(rd != PC && rs != PC &&
           rs != rd && rs != addr.base(), "unpredictable instruction");
    emit_int32(cond << 28 | 0x18 << 20 | addr.encoding_ex() |
              rs->encoding()  << 12 | 0xf90 | rd->encoding());
  }

  void ldrexd(Register rd, Address addr, AsmCondition cond = al) {
    assert(rd != PC, "unpredictable instruction");
    emit_int32(cond << 28 | 0x1B << 20 | addr.encoding_ex() |
              rd->encoding()  << 12 | 0xf9f);
  }

  void strexd(Register rs, Register rd, Address addr, AsmCondition cond = al) {
    assert(rd != PC && rs != PC &&
           rs != rd && rs != addr.base(), "unpredictable instruction");
    emit_int32(cond << 28 | 0x1A << 20 | addr.encoding_ex() |
              rs->encoding()  << 12 | 0xf90 | rd->encoding());
  }

  void clrex() {
    emit_int32(0xF << 28 | 0x57 << 20 | 0xFF  << 12 | 0x01f);
  }

  // Miscellaneous instructions

  void clz(Register rd, Register rm, AsmCondition cond = al) {
    emit_int32(cond << 28 | 0x016f0f10 | rd->encoding() << 12 | rm->encoding());
  }

  void rev(Register rd, Register rm, AsmCondition cond = al) {
    emit_int32(cond << 28 | 0x06bf0f30 | rd->encoding() << 12 | rm->encoding());
  }

  void rev16(Register rd, Register rm, AsmCondition cond = al) {
    emit_int32(cond << 28 | 0x6bf0fb0 | rd->encoding() << 12 | rm->encoding());
  }

  void revsh(Register rd, Register rm, AsmCondition cond = al) {
    emit_int32(cond << 28 | 0x6ff0fb0 | rd->encoding() << 12 | rm->encoding());
  }

  void rbit(Register rd, Register rm, AsmCondition cond = al) {
    emit_int32(cond << 28 | 0x6ff0f30 | rd->encoding() << 12 | rm->encoding());
  }

  void pld(Address addr) {
    emit_int32(0xf550f000 | addr.encoding2());
  }

  void pldw(Address addr) {
    assert(!VM_Version::is_initialized() ||
           (VM_Version::arm_arch() >= 7 && VM_Version::has_multiprocessing_extensions()),
           "PLDW is available on ARMv7 with Multiprocessing Extensions only");
    emit_int32(0xf510f000 | addr.encoding2());
  }

  void svc(int imm_24, AsmCondition cond = al) {
    assert((imm_24 >> 24) == 0, "encoding constraint");
    emit_int32(cond << 28 | 0xf << 24 | imm_24);
  }

  void ubfx(Register rd, Register rn, unsigned int lsb, unsigned int width, AsmCondition cond = al) {
    assert(VM_Version::arm_arch() >= 7, "no ubfx on this processor");
    assert(width > 0, "must be");
    assert(lsb < 32, "must be");
    emit_int32(cond << 28 | 0x3f << 21 | (width - 1)  << 16 | rd->encoding() << 12 |
              lsb << 7 | 0x5 << 4 | rn->encoding());
  }

  void uxtb(Register rd, Register rm, unsigned int rotation = 0, AsmCondition cond = al) {
    assert(VM_Version::arm_arch() >= 7, "no uxtb on this processor");
    assert((rotation % 8) == 0 && (rotation <= 24), "encoding constraint");
    emit_int32(cond << 28 | 0x6e << 20 | 0xf << 16 | rd->encoding() << 12 |
              (rotation >> 3) << 10 | 0x7 << 4 | rm->encoding());
  }

  // ARM Memory Barriers
  //
  // There are two types of memory barriers defined for the ARM processor
  // DataSynchronizationBarrier and DataMemoryBarrier
  //
  // The Linux kernel uses the DataMemoryBarrier for all of it's
  // memory barrier operations (smp_mb, smp_rmb, smp_wmb)
  //
  // There are two forms of each barrier instruction.
  // The mcr forms are supported on armv5 and newer architectures
  //
  // The dmb, dsb instructions were added in armv7
  // architectures and are compatible with their mcr
  // predecessors.
  //
  // Here are the encodings for future reference:
  //
  // DataSynchronizationBarrier (dsb)
  // on ARMv7 - emit_int32(0xF57FF04F)
  //
  // on ARMv5+ - mcr p15, 0, Rtmp, c7, c10, 4  on earlier processors
  //             emit_int32(0xe << 28 | 0xe << 24 | 0x7 << 16 | Rtmp->encoding() << 12  |
  //                       0xf << 8  | 0x9 << 4  | 0xa);
  //
  // DataMemoryBarrier (dmb)
  // on ARMv7 - emit_int32(0xF57FF05F)
  //
  // on ARMv5+ - mcr p15, 0, Rtmp, c7, c10, 5 on earlier processors
  //             emit_int32(0xe << 28 | 0xe << 24 | 0x7 << 16 | Rtmp->encoding() << 12  |
  //                       0xf << 8  | 0xb << 4  | 0xa);
  //

  enum DMB_Opt {
    DMB_all = 0xf,
    DMB_st  = 0xe,
  };

  void dmb(DMB_Opt opt, Register reg) {
    if (VM_Version::arm_arch() >= 7) {
      emit_int32(0xF57FF050 | opt);
    } else if (VM_Version::arm_arch() == 6) {
      bool preserve_tmp = (reg == noreg);
      if(preserve_tmp) {
        reg = Rtemp;
        str(reg, Address(SP, -wordSize, pre_indexed));
      }
      mov(reg, 0);
      // DataMemoryBarrier
      emit_int32(0xe << 28 |
                0xe << 24 |
                0x7 << 16 |
                reg->encoding() << 12  |
                0xf << 8  |
                0xb << 4  |
                0xa);
      if(preserve_tmp) {
        ldr(reg, Address(SP, wordSize, post_indexed));
      }
    }
  }

  void dsb(Register reg) {
    if (VM_Version::arm_arch() >= 7) {
      emit_int32(0xF57FF04F);
    } else {
      bool preserve_tmp = (reg == noreg);
      if(preserve_tmp) {
        reg = Rtemp;
        str(reg, Address(SP, -wordSize, pre_indexed));
      }
      mov(reg, 0);
      // DataSynchronizationBarrier
      emit_int32(0xe << 28 |
                0xe << 24 |
                0x7 << 16 |
                reg->encoding() << 12  |
                0xf << 8  |
                0x9 << 4  |
                0xa);
      if(preserve_tmp) {
        ldr(reg, Address(SP, wordSize, post_indexed));
      }
    }
  }


#define F(mnemonic, b) \
  void mnemonic(Register rd, Register rm, Register rn, AsmCondition cond = al) { \
    assert(rn != rm && rn != rd, "unpredictable instruction");                   \
    emit_int32(cond << 28 | 0x2 << 23 | b << 22 | rn->encoding() << 16 |         \
              rd->encoding() << 12 | 9 << 4 | rm->encoding());                   \
  }

  F(swp,  0)
  F(swpb, 1)
#undef F

  // Branches

#define F(mnemonic, l) \
  void mnemonic(Register rm, AsmCondition cond = al) {            \
    emit_int32(cond << 28 | 0x012fff10 | l << 5 | rm->encoding()); \
  }

  F(bx,  0)
  F(blx, 1)
#undef F

#define F(mnemonic, l)                                                  \
  void mnemonic(address target, AsmCondition cond = al) {               \
    unsigned int offset = (unsigned int)(target - pc() - 8);            \
    assert((offset & 3) == 0, "bad alignment");                         \
    assert((offset >> 25) == 0 || ((int)offset >> 25) == -1, "offset is too large"); \
    emit_int32(cond << 28 | l << 24 | offset << 6 >> 8);                \
  }

  F(b,  0xa)
  F(bl, 0xb)
#undef F

  void udf(int imm_16) {
    assert((imm_16 >> 16) == 0, "encoding constraint");
    emit_int32(0xe7f000f0 | (imm_16 & 0xfff0) << 8 | (imm_16 & 0xf));
  }

  // ARMv7 instructions

#define F(mnemonic, wt) \
  void mnemonic(Register rd, int imm_16, AsmCondition cond = al) { \
    assert((imm_16 >> 16) == 0, "encoding constraint");            \
    emit_int32(cond << 28 | wt << 20 | rd->encoding() << 12 |      \
              (imm_16 & 0xf000) << 4 | (imm_16 & 0xfff));          \
  }

  F(movw, 0x30)
  F(movt, 0x34)
#undef F

  // VFP Support

// Checks that VFP instructions are not used in SOFTFP mode.
#ifdef __SOFTFP__
#define CHECK_VFP_PRESENT ShouldNotReachHere()
#else
#define CHECK_VFP_PRESENT
#endif // __SOFTFP__

  static const int single_cp_num = 0xa00;
  static const int double_cp_num = 0xb00;

  // Bits P, Q, R, S collectively form the opcode
#define F(mnemonic, P, Q, R, S) \
  void mnemonic##d(FloatRegister fd, FloatRegister fn, FloatRegister fm, \
                   AsmCondition cond = al) {                             \
    CHECK_VFP_PRESENT;                                                   \
    assert(fn->lo_bit() == 0 && fd->lo_bit() == 0 && fm->lo_bit() == 0, "single precision register?"); \
    emit_int32(cond << 28 | 0x7 << 25 | double_cp_num |                  \
              P << 23 | Q << 21 | R << 20 | S << 6 |                     \
              fn->hi_bits() << 16 | fn->hi_bit() << 7 |                  \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                 \
              fm->hi_bits()       | fm->hi_bit() << 5);                  \
  }                                                                      \
  void mnemonic##s(FloatRegister fd, FloatRegister fn, FloatRegister fm, \
                   AsmCondition cond = al) {                             \
    assert(fn->hi_bit() == 0 && fd->hi_bit() == 0 && fm->hi_bit() == 0, "double precision register?"); \
    CHECK_VFP_PRESENT;                                                   \
    emit_int32(cond << 28 | 0x7 << 25 | single_cp_num |                  \
              P << 23 | Q << 21 | R << 20 | S << 6 |                     \
              fn->hi_bits() << 16 | fn->lo_bit() << 7 |                  \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                 \
              fm->hi_bits()       | fm->lo_bit() << 5);                  \
  }

  F(fmac,  0, 0, 0, 0)  // Fd = Fd + (Fn * Fm)
  F(fnmac, 0, 0, 0, 1)  // Fd = Fd - (Fn * Fm)
  F(fmsc,  0, 0, 1, 0)  // Fd = -Fd + (Fn * Fm)
  F(fnmsc, 0, 0, 1, 1)  // Fd = -Fd - (Fn * Fm)

  F(fmul,  0, 1, 0, 0)  // Fd = Fn * Fm
  F(fnmul, 0, 1, 0, 1)  // Fd = -(Fn * Fm)
  F(fadd,  0, 1, 1, 0)  // Fd = Fn + Fm
  F(fsub,  0, 1, 1, 1)  // Fd = Fn - Fm
  F(fdiv,  1, 0, 0, 0)  // Fd = Fn / Fm
#undef F

  enum VElem_Size {
    VELEM_SIZE_8  = 0x00,
    VELEM_SIZE_16 = 0x01,
    VELEM_SIZE_32 = 0x02,
    VELEM_SIZE_64 = 0x03
  };

  enum VLD_Type {
    VLD1_TYPE_1_REG  = 0x7 /* 0b0111 */,
    VLD1_TYPE_2_REGS = 0xA /* 0b1010 */,
    VLD1_TYPE_3_REGS = 0x6 /* 0b0110 */,
    VLD1_TYPE_4_REGS = 0x2 /* 0b0010 */
  };

  enum VFloat_Arith_Size {
    VFA_SIZE_F32 = 0x0 /* 0b0 */,
  };

  // Bits P, Q, R, S collectively form the opcode
#define F(mnemonic, P, Q, R, S) \
  void mnemonic(FloatRegister fd, FloatRegister fn, FloatRegister fm,    \
                int size, int quad) {                                    \
    CHECK_VFP_PRESENT;                                                   \
    assert(VM_Version::has_simd(), "simd instruction");                  \
    assert(fn->lo_bit() == 0 && fd->lo_bit() == 0 && fm->lo_bit() == 0,  \
           "single precision register?");                                \
    assert(!quad || ((fn->hi_bits() | fd->hi_bits() | fm->hi_bits()) & 1) == 0, \
           "quad precision register?");                                  \
    emit_int32(0xf << 28 | P << 23 | Q << 8 | R << 4 |                   \
              S << 21 | size << 20 | quad << 6 |                         \
              fn->hi_bits() << 16 | fn->hi_bit() << 7 |                  \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                 \
              fm->hi_bits()       | fm->hi_bit() << 5);                  \
  }

  F(vmulI,  0x4 /* 0b0100 */, 0x9 /* 0b1001 */, 1, 0)  // Vd = Vn * Vm (int)
  F(vaddI,  0x4 /* 0b0100 */, 0x8 /* 0b1000 */, 0, 0)  // Vd = Vn + Vm (int)
  F(vsubI,  0x6 /* 0b0110 */, 0x8 /* 0b1000 */, 0, 0)  // Vd = Vn - Vm (int)
  F(vaddF,  0x4 /* 0b0100 */, 0xD /* 0b1101 */, 0, 0)  // Vd = Vn + Vm (float)
  F(vsubF,  0x4 /* 0b0100 */, 0xD /* 0b1101 */, 0, 1)  // Vd = Vn - Vm (float)
  F(vmulF,  0x6 /* 0b0110 */, 0xD /* 0b1101 */, 1, 0)  // Vd = Vn * Vm (float)
  F(vshlSI, 0x4 /* 0b0100 */, 0x4 /* 0b0100 */, 0, 0)  // Vd = ashift(Vm,Vn) (int)
  F(vshlUI, 0x6 /* 0b0110 */, 0x4 /* 0b0100 */, 0, 0)  // Vd = lshift(Vm,Vn) (int)
  F(_vandI, 0x4 /* 0b0100 */, 0x1 /* 0b0001 */, 1, 0)  // Vd = Vn & Vm (int)
  F(_vorI,  0x4 /* 0b0100 */, 0x1 /* 0b0001 */, 1, 1)  // Vd = Vn | Vm (int)
  F(_vxorI, 0x6 /* 0b0110 */, 0x1 /* 0b0001 */, 1, 0)  // Vd = Vn ^ Vm (int)
#undef F

  void vandI(FloatRegister fd, FloatRegister fn, FloatRegister fm, int quad) {
    _vandI(fd, fn, fm, 0, quad);
  }
  void vorI(FloatRegister fd, FloatRegister fn, FloatRegister fm, int quad) {
    _vorI(fd, fn, fm, 0, quad);
  }
  void vxorI(FloatRegister fd, FloatRegister fn, FloatRegister fm, int quad) {
    _vxorI(fd, fn, fm, 0, quad);
  }

  void vneg(FloatRegister fd, FloatRegister fm, int size, int flt, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(fd->lo_bit() == 0 && fm->lo_bit() == 0,
           "single precision register?");
    assert(!quad || ((fd->hi_bits() | fm->hi_bits()) & 1) == 0,
           "quad precision register?");
    emit_int32(0xf << 28 | 0x3B /* 0b00111011 */ << 20 | 0x1 /* 0b01 */ << 16 | 0x7 /* 0b111 */ << 7 |
               size << 18 | quad << 6 | flt << 10 |
               fd->hi_bits() << 12 | fd->hi_bit() << 22 |
               fm->hi_bits() <<  0 | fm->hi_bit() << 5);
  }

  void vnegI(FloatRegister fd, FloatRegister fm, int size, int quad) {
    int flt = 0;
    vneg(fd, fm, size, flt, quad);
  }

  void vshli(FloatRegister fd, FloatRegister fm, int size, int imm, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(fd->lo_bit() == 0 && fm->lo_bit() == 0,
           "single precision register?");
    assert(!quad || ((fd->hi_bits() | fm->hi_bits()) & 1) == 0,
           "quad precision register?");

    if (imm >= size) {
      // maximum shift gives all zeroes, direction doesn't matter,
      // but only available for shift right
      vshri(fd, fm, size, size, true /* unsigned */, quad);
      return;
    }
    assert(imm >= 0 && imm < size, "out of range");

    int imm6 = 0;
    int L = 0;
    switch (size) {
    case 8:
    case 16:
    case 32:
      imm6 = size + imm ;
      break;
    case 64:
      L = 1;
      imm6 = imm ;
      break;
    default:
      ShouldNotReachHere();
    }
    emit_int32(0xf << 28 | 0x5 /* 0b00101 */ << 23 | 0x51 /* 0b01010001 */ << 4 |
               imm6 << 16 | L << 7 | quad << 6 |
               fd->hi_bits() << 12 | fd->hi_bit() << 22 |
               fm->hi_bits() <<  0 | fm->hi_bit() << 5);
  }

  void vshri(FloatRegister fd, FloatRegister fm, int size, int imm,
             bool U /* unsigned */, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(fd->lo_bit() == 0 && fm->lo_bit() == 0,
           "single precision register?");
    assert(!quad || ((fd->hi_bits() | fm->hi_bits()) & 1) == 0,
           "quad precision register?");
    assert(imm > 0, "out of range");
    if (imm >= size) {
      // maximum shift (all zeroes)
      imm = size;
    }
    int imm6 = 0;
    int L = 0;
    switch (size) {
    case 8:
    case 16:
    case 32:
      imm6 = 2 * size - imm ;
      break;
    case 64:
      L = 1;
      imm6 = 64 - imm ;
      break;
    default:
      ShouldNotReachHere();
    }
    emit_int32(0xf << 28 | 0x5 /* 0b00101 */ << 23 | 0x1 /* 0b00000001 */ << 4 |
               imm6 << 16 | L << 7 | quad << 6 | U << 24 |
               fd->hi_bits() << 12 | fd->hi_bit() << 22 |
               fm->hi_bits() <<  0 | fm->hi_bit() << 5);
  }
  void vshrUI(FloatRegister fd, FloatRegister fm, int size, int imm, int quad) {
    vshri(fd, fm, size, imm, true /* unsigned */, quad);
  }
  void vshrSI(FloatRegister fd, FloatRegister fm, int size, int imm, int quad) {
    vshri(fd, fm, size, imm, false /* signed */, quad);
  }

  // Extension opcodes where P,Q,R,S = 1 opcode is in Fn
#define F(mnemonic, N, opcode) \
  void mnemonic##d(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->lo_bit() == 0 && fm->hi_bit() == 0, "incorrect register?");        \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              double_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                          \
              fm->hi_bits()       | fm->lo_bit() << 5);                           \
  }                                                                               \
  void mnemonic##s(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->hi_bit() == 0 && fm->hi_bit() == 0, "double precision register?"); \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              single_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                          \
              fm->hi_bits()       | fm->lo_bit() << 5);                           \
  }

  F(fuito,  0, 0x8)  // Unsigned integer to floating point conversion
  F(fsito,  1, 0x8)  // Signed integer to floating point conversion
#undef F

#define F(mnemonic, N, opcode) \
  void mnemonic##d(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->hi_bit() == 0 && fm->lo_bit() == 0, "incorrect register?");        \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              double_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                          \
              fm->hi_bits()       | fm->hi_bit() << 5);                           \
  }                                                                               \
  void mnemonic##s(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->hi_bit() == 0 && fm->hi_bit() == 0, "double precision register?"); \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              single_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                          \
              fm->hi_bits()       | fm->lo_bit() << 5);                           \
  }

  F(ftoui,  0, 0xc)  // Float to unsigned int conversion
  F(ftouiz, 1, 0xc)  // Float to unsigned int conversion, RZ mode
  F(ftosi,  0, 0xd)  // Float to signed int conversion
  F(ftosiz, 1, 0xd)  // Float to signed int conversion, RZ mode
#undef F

#define F(mnemonic, N, opcode) \
  void mnemonic##d(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->hi_bit() == 0 && fm->lo_bit() == 0, "incorrect register?");        \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              double_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                          \
              fm->hi_bits()       | fm->hi_bit() << 5);                           \
  }                                                                               \
  void mnemonic##s(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->lo_bit() == 0 && fm->hi_bit() == 0, "incorrect register?");        \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              single_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                          \
              fm->hi_bits()       | fm->lo_bit() << 5);                           \
  }

  F(fcvtd,  1, 0x7)  // Single->Double conversion
  F(fcvts,  1, 0x7)  // Double->Single conversion
#undef F

#define F(mnemonic, N, opcode) \
  void mnemonic##d(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->lo_bit() == 0 && fm->lo_bit() == 0, "single precision register?"); \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              double_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                          \
              fm->hi_bits()       | fm->hi_bit() << 5);                           \
  }                                                                               \
  void mnemonic##s(FloatRegister fd, FloatRegister fm, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                            \
    assert(fd->hi_bit() == 0 && fm->hi_bit() == 0, "double precision register?"); \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |         \
              single_cp_num |                                                     \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                          \
              fm->hi_bits()       | fm->lo_bit() << 5);                           \
  }

  F(fcpy,   0, 0x0)  // Fd = Fm
  F(fabs,   1, 0x0)  // Fd = abs(Fm)
  F(fneg,   0, 0x1)  // Fd = -Fm
  F(fsqrt,  1, 0x1)  // Fd = sqrt(Fm)
  F(fcmp,   0, 0x4)  // Compare Fd with Fm no exceptions on quiet NANs
  F(fcmpe,  1, 0x4)  // Compare Fd with Fm with exceptions on quiet NANs
#undef F

  // Opcodes with one operand only
#define F(mnemonic, N, opcode) \
  void mnemonic##d(FloatRegister fd, AsmCondition cond = al) {               \
    CHECK_VFP_PRESENT;                                                       \
    assert(fd->lo_bit() == 0, "single precision register?");                 \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |    \
              double_cp_num | fd->hi_bits() << 12 | fd->hi_bit() << 22);     \
  }                                                                          \
  void mnemonic##s(FloatRegister fd, AsmCondition cond = al) {               \
    CHECK_VFP_PRESENT;                                                       \
    assert(fd->hi_bit() == 0, "double precision register?");                 \
    emit_int32(cond << 28 | 0xeb << 20 | opcode << 16 | N << 7 | 1 << 6 |    \
              single_cp_num | fd->hi_bits() << 12 | fd->lo_bit() << 22);     \
  }

  F(fcmpz,  0, 0x5)  // Compare Fd with 0, no exceptions quiet NANs
  F(fcmpez, 1, 0x5)  // Compare Fd with 0, with exceptions quiet NANs
#undef F

  // Float loads (L==1) and stores (L==0)
#define F(mnemonic, L) \
  void mnemonic##d(FloatRegister fd, Address addr, AsmCondition cond = al) { \
    CHECK_VFP_PRESENT;                                                       \
    assert(fd->lo_bit() == 0, "single precision register?");                 \
    emit_int32(cond << 28 | 0xd << 24 | L << 20 |                            \
              fd->hi_bits() << 12 | fd->hi_bit() << 22 |                     \
              double_cp_num | addr.encoding_vfp());                          \
  }                                                                          \
  void mnemonic##s(FloatRegister fd, Address addr, AsmCondition cond = al) { \
    CHECK_VFP_PRESENT;                                                       \
    assert(fd->hi_bit() == 0, "double precision register?");                 \
    emit_int32(cond << 28 | 0xd << 24 | L << 20 |                            \
              fd->hi_bits() << 12 | fd->lo_bit() << 22 |                     \
              single_cp_num | addr.encoding_vfp());                          \
  }

  F(fst, 0)  // Store 1 register
  F(fld, 1)  // Load 1 register
#undef F

  // Float load and store multiple
#define F(mnemonic, l, pu) \
  void mnemonic##d(Register rn, FloatRegisterSet reg_set,                    \
                   AsmWriteback w = no_writeback, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                       \
    assert(w == no_writeback || rn != PC, "unpredictable instruction");      \
    assert(!(w == no_writeback && pu == 2), "encoding constraint");          \
    assert((reg_set.encoding_d() & 1) == 0, "encoding constraint");          \
    emit_int32(cond << 28 | 6 << 25 | pu << 23 | w << 21 | l << 20 |         \
              rn->encoding() << 16 | reg_set.encoding_d() | double_cp_num);  \
  }                                                                          \
  void mnemonic##s(Register rn, FloatRegisterSet reg_set,                    \
                   AsmWriteback w = no_writeback, AsmCondition cond = al) {  \
    CHECK_VFP_PRESENT;                                                       \
    assert(w == no_writeback || rn != PC, "unpredictable instruction");      \
    assert(!(w == no_writeback && pu == 2), "encoding constraint");          \
    emit_int32(cond << 28 | 6 << 25 | pu << 23 | w << 21 | l << 20 |         \
              rn->encoding() << 16 | reg_set.encoding_s() | single_cp_num);  \
  }

  F(fldmia, 1, 1)    F(fldmfd, 1, 1)
  F(fldmdb, 1, 2)    F(fldmea, 1, 2)
  F(fstmia, 0, 1)    F(fstmea, 0, 1)
  F(fstmdb, 0, 2)    F(fstmfd, 0, 2)
#undef F

  // fconst{s,d} encoding:
  //  31  28 27   23 22  21 20 19   16 15 12 10  9  8   7    4 3     0
  // | cond | 11101 | D | 11  | imm4H | Vd  | 101 | sz | 0000 | imm4L |
  // sz = 0 for single precision, 1 otherwise
  // Register number is Vd:D for single precision, D:Vd otherwise
  // immediate value is imm4H:imm4L

  void fconsts(FloatRegister fd, unsigned char imm_8, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->hi_bit() == 0, "double precision register?");
    emit_int32(cond << 28 | 0xeb << 20 | single_cp_num |
              fd->hi_bits() << 12 | fd->lo_bit() << 22 | (imm_8 & 0xf) | (imm_8 >> 4) << 16);
  }

  void fconstd(FloatRegister fd, unsigned char imm_8, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->lo_bit() == 0, "double precision register?");
    emit_int32(cond << 28 | 0xeb << 20 | double_cp_num |
              fd->hi_bits() << 12 | fd->hi_bit() << 22 | (imm_8 & 0xf) | (imm_8 >> 4) << 16);
  }

  // GPR <-> FPR transfers
  void fmsr(FloatRegister fd, Register rd, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->hi_bit() == 0, "double precision register?");
    emit_int32(cond << 28 | 0xe0 << 20 | single_cp_num | 1 << 4 |
              fd->hi_bits() << 16 | fd->lo_bit() << 7 | rd->encoding() << 12);
  }

  void fmrs(Register rd, FloatRegister fd, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->hi_bit() == 0, "double precision register?");
    emit_int32(cond << 28 | 0xe1 << 20 | single_cp_num | 1 << 4 |
              fd->hi_bits() << 16 | fd->lo_bit() << 7 | rd->encoding() << 12);
  }

  void fmdrr(FloatRegister fd, Register rd, Register rn, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->lo_bit() == 0, "single precision register?");
    emit_int32(cond << 28 | 0xc4 << 20 | double_cp_num | 1 << 4 |
              fd->hi_bits() | fd->hi_bit() << 5 |
              rn->encoding() << 16 | rd->encoding() << 12);
  }

  void fmrrd(Register rd, Register rn, FloatRegister fd, AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(fd->lo_bit() == 0, "single precision register?");
    emit_int32(cond << 28 | 0xc5 << 20 | double_cp_num | 1 << 4 |
              fd->hi_bits() | fd->hi_bit() << 5 |
              rn->encoding() << 16 | rd->encoding() << 12);
  }

  void fmstat(AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    emit_int32(cond << 28 | 0xef1fa10);
  }

  void vmrs(Register rt, VFPSystemRegister sr, AsmCondition cond = al) {
    assert((sr->encoding() & (~0xf)) == 0, "what system register is that?");
    emit_int32(cond << 28 | rt->encoding() << 12 | sr->encoding() << 16 | 0xef00a10);
  }

  void vmsr(VFPSystemRegister sr, Register rt, AsmCondition cond = al) {
    assert((sr->encoding() & (~0xf)) == 0, "what system register is that?");
    emit_int32(cond << 28 | rt->encoding() << 12 | sr->encoding() << 16 | 0xee00a10);
  }

  void vcnt(FloatRegister Dd, FloatRegister Dm) {
    CHECK_VFP_PRESENT;
    // emitted at VM startup to detect whether the instruction is available
    assert(!VM_Version::is_initialized() || VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0 && Dm->lo_bit() == 0, "single precision registers?");
    emit_int32(0xf3b00500 | Dd->hi_bit() << 22 | Dd->hi_bits() << 12 | Dm->hi_bit() << 5 | Dm->hi_bits());
  }

  void vpaddl(FloatRegister Dd, FloatRegister Dm, int size, bool s) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0 && Dm->lo_bit() == 0, "single precision registers?");
    assert(size == 8 || size == 16 || size == 32, "unexpected size");
    emit_int32(0xf3b00200 | Dd->hi_bit() << 22 | (size >> 4) << 18 | Dd->hi_bits() << 12 | (s ? 0 : 1) << 7 | Dm->hi_bit() << 5 | Dm->hi_bits());
  }

  void vld1(FloatRegister Dd, Address addr, VElem_Size size, int bits) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision registers?");
    int align = 0;
    assert(bits == 128, "code assumption");
    VLD_Type type = VLD1_TYPE_2_REGS; // 2x64
    emit_int32(0xf4200000 | Dd->hi_bit() << 22 | Dd->hi_bits() << 12 | type << 8 | size << 6 | align << 4 | addr.encoding_simd());
  }

  void vst1(FloatRegister Dd, Address addr, VElem_Size size, int bits) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision registers?");
    int align = 0;
    assert(bits == 128, "code assumption");
    VLD_Type type = VLD1_TYPE_2_REGS; // 2x64
    emit_int32(0xf4000000 | Dd->hi_bit() << 22 | Dd->hi_bits() << 12 | type << 8 | size << 6 | align << 4 | addr.encoding_simd());
  }

  void vmovI(FloatRegister Dd, int imm8, VElem_Size size, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision register?");
    assert(!quad || (Dd->hi_bits() & 1) == 0, "quad precision register?");
    assert(imm8 >= 0 && imm8 < 256, "out of range");
    int op;
    int cmode;
    switch (size) {
    case VELEM_SIZE_8:
      op = 0;
      cmode = 0xE /* 0b1110 */;
      break;
    case VELEM_SIZE_16:
      op = 0;
      cmode = 0x8 /* 0b1000 */;
      break;
    case VELEM_SIZE_32:
      op = 0;
      cmode = 0x0 /* 0b0000 */;
      break;
    default:
      ShouldNotReachHere();
      return;
    }
    emit_int32(0xf << 28 | 0x1 << 25 | 0x1 << 23 | 0x1 << 4 |
              (imm8 >> 7) << 24 | ((imm8 & 0x70) >> 4) << 16 | (imm8 & 0xf) |
              quad << 6 | op << 5 | cmode << 8 |
              Dd->hi_bits() << 12 | Dd->hi_bit() << 22);
  }

  void vdupI(FloatRegister Dd, Register Rs, VElem_Size size, int quad,
             AsmCondition cond = al) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision register?");
    assert(!quad || (Dd->hi_bits() & 1) == 0, "quad precision register?");
    int b;
    int e;
    switch (size) {
    case VELEM_SIZE_8:
      b = 1;
      e = 0;
      break;
    case VELEM_SIZE_16:
      b = 0;
      e = 1;
      break;
    case VELEM_SIZE_32:
      b = 0;
      e = 0;
      break;
    default:
      ShouldNotReachHere();
      return;
    }
    emit_int32(cond << 28 | 0x1D /* 0b11101 */ << 23 | 0xB /* 0b1011 */ << 8 | 0x1 << 4 |
              quad << 21 | b << 22 |  e << 5 | Rs->encoding() << 12 |
              Dd->hi_bits() << 16 | Dd->hi_bit() << 7);
  }

  void vdup(FloatRegister Dd, FloatRegister Ds, int index, int size, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision register?");
    assert(Ds->lo_bit() == 0, "single precision register?");
    assert(!quad || (Dd->hi_bits() & 1) == 0, "quad precision register?");
    int range = 64 / size;
    assert(index < range, "overflow");
    int imm4;
    switch (size) {
    case 8:
      assert((index & 0x7 /* 0b111 */) == index, "overflow");
      imm4 = index << 1 | 0x1 /* 0b0001 */;
      break;
    case 16:
      assert((index & 0x3 /* 0b11 */) == index, "overflow");
      imm4 = index << 2 | 0x2 /* 0b0010 */;
      break;
    case 32:
      assert((index & 0x1 /* 0b1 */) == index, "overflow");
      imm4 = index << 3 | 0x4 /* 0b0100 */;
      break;
    default:
      ShouldNotReachHere();
      return;
    }
    emit_int32(0xF /* 0b1111 */ << 28 | 0x3B /* 0b00111011 */ << 20 | 0x6 /* 0b110 */ << 9 |
               quad << 6 | imm4 << 16 |
               Dd->hi_bits() << 12 | Dd->hi_bit() << 22 |
               Ds->hi_bits() << 00 | Ds->hi_bit() << 5);
  }

  void vdupF(FloatRegister Dd, FloatRegister Ss, int quad) {
    int index = 0;
    FloatRegister Ds = as_FloatRegister(Ss->encoding() & ~1);
    if (Ss->lo_bit() != 0) {
      /* odd S register */
      assert(Ds->successor() == Ss, "bad reg");
      index = 1;
    } else {
      /* even S register */
      assert(Ds == Ss, "bad reg");
    }
    vdup(Dd, Ds, index, 32, quad);
  }

  void vrev(FloatRegister Dd, FloatRegister Dm, int quad, int region_size, VElem_Size size) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision register?");
    assert(Dm->lo_bit() == 0, "single precision register?");
    assert(!quad || ((Dd->hi_bits() | Dm->hi_bits()) & 1) == 0,
           "quad precision register?");
    unsigned int op = 0;
    switch (region_size) {
      case 16: op = 0x2; /*0b10*/ break;
      case 32: op = 0x1; /*0b01*/ break;
      case 64: op = 0x0; /*0b00*/ break;
      default: assert(false, "encoding constraint");
    }
    emit_int32(0xf << 28 | 0x7 << 23 | Dd->hi_bit() << 22 | 0x3 << 20 |
               size << 18 | Dd->hi_bits() << 12 | op  << 7 | quad << 6 | Dm->hi_bit() << 5 |
               Dm->hi_bits());
  }

  void veor(FloatRegister Dd, FloatRegister Dn, FloatRegister Dm, int quad) {
    CHECK_VFP_PRESENT;
    assert(VM_Version::has_simd(), "simd instruction");
    assert(Dd->lo_bit() == 0, "single precision register?");
    assert(Dm->lo_bit() == 0, "single precision register?");
    assert(Dn->lo_bit() == 0, "single precision register?");
    assert(!quad || ((Dd->hi_bits() | Dm->hi_bits() | Dn->hi_bits()) & 1) == 0,
           "quad precision register?");

    emit_int32(0xf << 28 | 0x3 << 24 | Dd->hi_bit() << 22 | Dn->hi_bits() << 16 |
               Dd->hi_bits() << 12 | 0x1 << 8 | Dn->hi_bit() << 7 | quad << 6 |
               Dm->hi_bit() << 5 | 0x1 << 4 | Dm->hi_bits());
  }


  Assembler(CodeBuffer* code) : AbstractAssembler(code) {}

#ifdef COMPILER2
  typedef VFP::double_num double_num;
  typedef VFP::float_num  float_num;
#endif
};

#ifdef __SOFTFP__
// Soft float function declarations
extern "C" {
extern float  __aeabi_fadd(float, float);
extern float  __aeabi_fmul(float, float);
extern float  __aeabi_fsub(float, float);
extern float  __aeabi_fdiv(float, float);

extern double __aeabi_dadd(double, double);
extern double __aeabi_dmul(double, double);
extern double __aeabi_dsub(double, double);
extern double __aeabi_ddiv(double, double);

extern double __aeabi_f2d(float);
extern float  __aeabi_d2f(double);
extern float  __aeabi_i2f(int);
extern double __aeabi_i2d(int);
extern int    __aeabi_f2iz(float);

extern int  __aeabi_fcmpeq(float, float);
extern int  __aeabi_fcmplt(float, float);
extern int  __aeabi_fcmple(float, float);
extern int  __aeabi_fcmpge(float, float);
extern int  __aeabi_fcmpgt(float, float);

extern int  __aeabi_dcmpeq(double, double);
extern int  __aeabi_dcmplt(double, double);
extern int  __aeabi_dcmple(double, double);
extern int  __aeabi_dcmpge(double, double);
extern int  __aeabi_dcmpgt(double, double);

// Imported code from glibc soft-fp bundle for
// calculation accuracy improvement. See CR 6757269.
extern double __aeabi_fadd_glibc(float, float);
extern double __aeabi_fsub_glibc(float, float);
extern double __aeabi_dadd_glibc(double, double);
extern double __aeabi_dsub_glibc(double, double);
};
#endif // __SOFTFP__


#endif // CPU_ARM_ASSEMBLER_ARM_32_HPP
