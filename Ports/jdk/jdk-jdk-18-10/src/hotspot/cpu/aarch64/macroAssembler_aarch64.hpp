/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2021, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP
#define CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP

#include "asm/assembler.inline.hpp"
#include "oops/compressedOops.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/powerOfTwo.hpp"

// MacroAssembler extends Assembler by frequently used macros.
//
// Instructions for which a 'better' code sequence exists depending
// on arguments should also go in here.

class MacroAssembler: public Assembler {
  friend class LIR_Assembler;

 public:
  using Assembler::mov;
  using Assembler::movi;

 protected:

  // Support for VM calls
  //
  // This is the base routine called by the different versions of call_VM_leaf. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  virtual void call_VM_leaf_base(
    address entry_point,               // the entry point
    int     number_of_arguments,        // the number of arguments to pop after the call
    Label *retaddr = NULL
  );

  virtual void call_VM_leaf_base(
    address entry_point,               // the entry point
    int     number_of_arguments,        // the number of arguments to pop after the call
    Label &retaddr) {
    call_VM_leaf_base(entry_point, number_of_arguments, &retaddr);
  }

  // This is the base routine called by the different versions of call_VM. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  //
  // If no java_thread register is specified (noreg) than rthread will be used instead. call_VM_base
  // returns the register which contains the thread upon return. If a thread register has been
  // specified, the return value will correspond to that register. If no last_java_sp is specified
  // (noreg) than rsp will be used instead.
  virtual void call_VM_base(           // returns the register containing the thread upon return
    Register oop_result,               // where an oop-result ends up if any; use noreg otherwise
    Register java_thread,              // the thread if computed before     ; use noreg otherwise
    Register last_java_sp,             // to set up last_Java_frame in stubs; use noreg otherwise
    address  entry_point,              // the entry point
    int      number_of_arguments,      // the number of arguments (w/o thread) to pop after the call
    bool     check_exceptions          // whether to check for pending exceptions after return
  );

  void call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions = true);

  enum KlassDecodeMode {
    KlassDecodeNone,
    KlassDecodeZero,
    KlassDecodeXor,
    KlassDecodeMovk
  };

  KlassDecodeMode klass_decode_mode();

 private:
  static KlassDecodeMode _klass_decode_mode;

 public:
  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

 // These routines should emit JVMTI PopFrame and ForceEarlyReturn handling code.
 // The implementation is only non-empty for the InterpreterMacroAssembler,
 // as only the interpreter handles PopFrame and ForceEarlyReturn requests.
 virtual void check_and_handle_popframe(Register java_thread);
 virtual void check_and_handle_earlyret(Register java_thread);

  void safepoint_poll(Label& slow_path, bool at_return, bool acquire, bool in_nmethod);

  // Helper functions for statistics gathering.
  // Unconditional atomic increment.
  void atomic_incw(Register counter_addr, Register tmp, Register tmp2);
  void atomic_incw(Address counter_addr, Register tmp1, Register tmp2, Register tmp3) {
    lea(tmp1, counter_addr);
    atomic_incw(tmp1, tmp2, tmp3);
  }
  // Load Effective Address
  void lea(Register r, const Address &a) {
    InstructionMark im(this);
    code_section()->relocate(inst_mark(), a.rspec());
    a.lea(this, r);
  }

  /* Sometimes we get misaligned loads and stores, usually from Unsafe
     accesses, and these can exceed the offset range. */
  Address legitimize_address(const Address &a, int size, Register scratch) {
    if (a.getMode() == Address::base_plus_offset) {
      if (! Address::offset_ok_for_immed(a.offset(), exact_log2(size))) {
        block_comment("legitimize_address {");
        lea(scratch, a);
        block_comment("} legitimize_address");
        return Address(scratch);
      }
    }
    return a;
  }

  void addmw(Address a, Register incr, Register scratch) {
    ldrw(scratch, a);
    addw(scratch, scratch, incr);
    strw(scratch, a);
  }

  // Add constant to memory word
  void addmw(Address a, int imm, Register scratch) {
    ldrw(scratch, a);
    if (imm > 0)
      addw(scratch, scratch, (unsigned)imm);
    else
      subw(scratch, scratch, (unsigned)-imm);
    strw(scratch, a);
  }

  void bind(Label& L) {
    Assembler::bind(L);
    code()->clear_last_insn();
  }

  void membar(Membar_mask_bits order_constraint);

  using Assembler::ldr;
  using Assembler::str;
  using Assembler::ldrw;
  using Assembler::strw;

  void ldr(Register Rx, const Address &adr);
  void ldrw(Register Rw, const Address &adr);
  void str(Register Rx, const Address &adr);
  void strw(Register Rx, const Address &adr);

  // Frame creation and destruction shared between JITs.
  void build_frame(int framesize);
  void remove_frame(int framesize);

  virtual void _call_Unimplemented(address call_site) {
    mov(rscratch2, call_site);
  }

// Microsoft's MSVC team thinks that the __FUNCSIG__ is approximately (sympathy for calling conventions) equivalent to __PRETTY_FUNCTION__
// Also, from Clang patch: "It is very similar to GCC's PRETTY_FUNCTION, except it prints the calling convention."
// https://reviews.llvm.org/D3311

#ifdef _WIN64
#define call_Unimplemented() _call_Unimplemented((address)__FUNCSIG__)
#else
#define call_Unimplemented() _call_Unimplemented((address)__PRETTY_FUNCTION__)
#endif

  // aliases defined in AARCH64 spec

  template<class T>
  inline void cmpw(Register Rd, T imm)  { subsw(zr, Rd, imm); }

  inline void cmp(Register Rd, unsigned char imm8)  { subs(zr, Rd, imm8); }
  inline void cmp(Register Rd, unsigned imm) = delete;

  inline void cmnw(Register Rd, unsigned imm) { addsw(zr, Rd, imm); }
  inline void cmn(Register Rd, unsigned imm) { adds(zr, Rd, imm); }

  void cset(Register Rd, Assembler::Condition cond) {
    csinc(Rd, zr, zr, ~cond);
  }
  void csetw(Register Rd, Assembler::Condition cond) {
    csincw(Rd, zr, zr, ~cond);
  }

  void cneg(Register Rd, Register Rn, Assembler::Condition cond) {
    csneg(Rd, Rn, Rn, ~cond);
  }
  void cnegw(Register Rd, Register Rn, Assembler::Condition cond) {
    csnegw(Rd, Rn, Rn, ~cond);
  }

  inline void movw(Register Rd, Register Rn) {
    if (Rd == sp || Rn == sp) {
      addw(Rd, Rn, 0U);
    } else {
      orrw(Rd, zr, Rn);
    }
  }
  inline void mov(Register Rd, Register Rn) {
    assert(Rd != r31_sp && Rn != r31_sp, "should be");
    if (Rd == Rn) {
    } else if (Rd == sp || Rn == sp) {
      add(Rd, Rn, 0U);
    } else {
      orr(Rd, zr, Rn);
    }
  }

  inline void moviw(Register Rd, unsigned imm) { orrw(Rd, zr, imm); }
  inline void movi(Register Rd, unsigned imm) { orr(Rd, zr, imm); }

  inline void tstw(Register Rd, Register Rn) { andsw(zr, Rd, Rn); }
  inline void tst(Register Rd, Register Rn) { ands(zr, Rd, Rn); }

  inline void tstw(Register Rd, uint64_t imm) { andsw(zr, Rd, imm); }
  inline void tst(Register Rd, uint64_t imm) { ands(zr, Rd, imm); }

  inline void bfiw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void bfi(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void bfxilw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void bfxil(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    bfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void sbfizw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void sbfiz(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void sbfxw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void sbfx(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    sbfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void ubfizw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfmw(Rd, Rn, ((32 - lsb) & 31), (width - 1));
  }
  inline void ubfiz(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfm(Rd, Rn, ((64 - lsb) & 63), (width - 1));
  }

  inline void ubfxw(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfmw(Rd, Rn, lsb, (lsb + width - 1));
  }
  inline void ubfx(Register Rd, Register Rn, unsigned lsb, unsigned width) {
    ubfm(Rd, Rn, lsb , (lsb + width - 1));
  }

  inline void asrw(Register Rd, Register Rn, unsigned imm) {
    sbfmw(Rd, Rn, imm, 31);
  }

  inline void asr(Register Rd, Register Rn, unsigned imm) {
    sbfm(Rd, Rn, imm, 63);
  }

  inline void lslw(Register Rd, Register Rn, unsigned imm) {
    ubfmw(Rd, Rn, ((32 - imm) & 31), (31 - imm));
  }

  inline void lsl(Register Rd, Register Rn, unsigned imm) {
    ubfm(Rd, Rn, ((64 - imm) & 63), (63 - imm));
  }

  inline void lsrw(Register Rd, Register Rn, unsigned imm) {
    ubfmw(Rd, Rn, imm, 31);
  }

  inline void lsr(Register Rd, Register Rn, unsigned imm) {
    ubfm(Rd, Rn, imm, 63);
  }

  inline void rorw(Register Rd, Register Rn, unsigned imm) {
    extrw(Rd, Rn, Rn, imm);
  }

  inline void ror(Register Rd, Register Rn, unsigned imm) {
    extr(Rd, Rn, Rn, imm);
  }

  inline void sxtbw(Register Rd, Register Rn) {
    sbfmw(Rd, Rn, 0, 7);
  }
  inline void sxthw(Register Rd, Register Rn) {
    sbfmw(Rd, Rn, 0, 15);
  }
  inline void sxtb(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 7);
  }
  inline void sxth(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 15);
  }
  inline void sxtw(Register Rd, Register Rn) {
    sbfm(Rd, Rn, 0, 31);
  }

  inline void uxtbw(Register Rd, Register Rn) {
    ubfmw(Rd, Rn, 0, 7);
  }
  inline void uxthw(Register Rd, Register Rn) {
    ubfmw(Rd, Rn, 0, 15);
  }
  inline void uxtb(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 7);
  }
  inline void uxth(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 15);
  }
  inline void uxtw(Register Rd, Register Rn) {
    ubfm(Rd, Rn, 0, 31);
  }

  inline void cmnw(Register Rn, Register Rm) {
    addsw(zr, Rn, Rm);
  }
  inline void cmn(Register Rn, Register Rm) {
    adds(zr, Rn, Rm);
  }

  inline void cmpw(Register Rn, Register Rm) {
    subsw(zr, Rn, Rm);
  }
  inline void cmp(Register Rn, Register Rm) {
    subs(zr, Rn, Rm);
  }

  inline void negw(Register Rd, Register Rn) {
    subw(Rd, zr, Rn);
  }

  inline void neg(Register Rd, Register Rn) {
    sub(Rd, zr, Rn);
  }

  inline void negsw(Register Rd, Register Rn) {
    subsw(Rd, zr, Rn);
  }

  inline void negs(Register Rd, Register Rn) {
    subs(Rd, zr, Rn);
  }

  inline void cmnw(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    addsw(zr, Rn, Rm, kind, shift);
  }
  inline void cmn(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    adds(zr, Rn, Rm, kind, shift);
  }

  inline void cmpw(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    subsw(zr, Rn, Rm, kind, shift);
  }
  inline void cmp(Register Rn, Register Rm, enum shift_kind kind, unsigned shift = 0) {
    subs(zr, Rn, Rm, kind, shift);
  }

  inline void negw(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subw(Rd, zr, Rn, kind, shift);
  }

  inline void neg(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    sub(Rd, zr, Rn, kind, shift);
  }

  inline void negsw(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subsw(Rd, zr, Rn, kind, shift);
  }

  inline void negs(Register Rd, Register Rn, enum shift_kind kind, unsigned shift = 0) {
    subs(Rd, zr, Rn, kind, shift);
  }

  inline void mnegw(Register Rd, Register Rn, Register Rm) {
    msubw(Rd, Rn, Rm, zr);
  }
  inline void mneg(Register Rd, Register Rn, Register Rm) {
    msub(Rd, Rn, Rm, zr);
  }

  inline void mulw(Register Rd, Register Rn, Register Rm) {
    maddw(Rd, Rn, Rm, zr);
  }
  inline void mul(Register Rd, Register Rn, Register Rm) {
    madd(Rd, Rn, Rm, zr);
  }

  inline void smnegl(Register Rd, Register Rn, Register Rm) {
    smsubl(Rd, Rn, Rm, zr);
  }
  inline void smull(Register Rd, Register Rn, Register Rm) {
    smaddl(Rd, Rn, Rm, zr);
  }

  inline void umnegl(Register Rd, Register Rn, Register Rm) {
    umsubl(Rd, Rn, Rm, zr);
  }
  inline void umull(Register Rd, Register Rn, Register Rm) {
    umaddl(Rd, Rn, Rm, zr);
  }

#define WRAP(INSN)                                                            \
  void INSN(Register Rd, Register Rn, Register Rm, Register Ra) {             \
    if ((VM_Version::features() & VM_Version::CPU_A53MAC) && Ra != zr)        \
      nop();                                                                  \
    Assembler::INSN(Rd, Rn, Rm, Ra);                                          \
  }

  WRAP(madd) WRAP(msub) WRAP(maddw) WRAP(msubw)
  WRAP(smaddl) WRAP(smsubl) WRAP(umaddl) WRAP(umsubl)
#undef WRAP


  // macro assembly operations needed for aarch64

  // first two private routines for loading 32 bit or 64 bit constants
private:

  void mov_immediate64(Register dst, uint64_t imm64);
  void mov_immediate32(Register dst, uint32_t imm32);

  int push(unsigned int bitset, Register stack);
  int pop(unsigned int bitset, Register stack);

  int push_fp(unsigned int bitset, Register stack);
  int pop_fp(unsigned int bitset, Register stack);

  void mov(Register dst, Address a);

public:
  void push(RegSet regs, Register stack) { if (regs.bits()) push(regs.bits(), stack); }
  void pop(RegSet regs, Register stack) { if (regs.bits()) pop(regs.bits(), stack); }

  void push_fp(FloatRegSet regs, Register stack) { if (regs.bits()) push_fp(regs.bits(), stack); }
  void pop_fp(FloatRegSet regs, Register stack) { if (regs.bits()) pop_fp(regs.bits(), stack); }

  static RegSet call_clobbered_registers();

  // Push and pop everything that might be clobbered by a native
  // runtime call except rscratch1 and rscratch2.  (They are always
  // scratch, so we don't have to protect them.)  Only save the lower
  // 64 bits of each vector register. Additonal registers can be excluded
  // in a passed RegSet.
  void push_call_clobbered_registers_except(RegSet exclude);
  void pop_call_clobbered_registers_except(RegSet exclude);

  void push_call_clobbered_registers() {
    push_call_clobbered_registers_except(RegSet());
  }
  void pop_call_clobbered_registers() {
    pop_call_clobbered_registers_except(RegSet());
  }


  // now mov instructions for loading absolute addresses and 32 or
  // 64 bit integers

  inline void mov(Register dst, address addr)             { mov_immediate64(dst, (uint64_t)addr); }

  inline void mov(Register dst, int imm64)                { mov_immediate64(dst, (uint64_t)imm64); }
  inline void mov(Register dst, long imm64)               { mov_immediate64(dst, (uint64_t)imm64); }
  inline void mov(Register dst, long long imm64)          { mov_immediate64(dst, (uint64_t)imm64); }
  inline void mov(Register dst, unsigned int imm64)       { mov_immediate64(dst, (uint64_t)imm64); }
  inline void mov(Register dst, unsigned long imm64)      { mov_immediate64(dst, (uint64_t)imm64); }
  inline void mov(Register dst, unsigned long long imm64) { mov_immediate64(dst, (uint64_t)imm64); }

  inline void movw(Register dst, uint32_t imm32)
  {
    mov_immediate32(dst, imm32);
  }

  void mov(Register dst, RegisterOrConstant src) {
    if (src.is_register())
      mov(dst, src.as_register());
    else
      mov(dst, src.as_constant());
  }

  void movptr(Register r, uintptr_t imm64);

  void mov(FloatRegister Vd, SIMD_Arrangement T, uint32_t imm32);

  void mov(FloatRegister Vd, SIMD_Arrangement T, FloatRegister Vn) {
    orr(Vd, T, Vn, Vn);
  }


public:

  // Generalized Test Bit And Branch, including a "far" variety which
  // spans more than 32KiB.
  void tbr(Condition cond, Register Rt, int bitpos, Label &dest, bool isfar = false) {
    assert(cond == EQ || cond == NE, "must be");

    if (isfar)
      cond = ~cond;

    void (Assembler::* branch)(Register Rt, int bitpos, Label &L);
    if (cond == Assembler::EQ)
      branch = &Assembler::tbz;
    else
      branch = &Assembler::tbnz;

    if (isfar) {
      Label L;
      (this->*branch)(Rt, bitpos, L);
      b(dest);
      bind(L);
    } else {
      (this->*branch)(Rt, bitpos, dest);
    }
  }

  // macro instructions for accessing and updating floating point
  // status register
  //
  // FPSR : op1 == 011
  //        CRn == 0100
  //        CRm == 0100
  //        op2 == 001

  inline void get_fpsr(Register reg)
  {
    mrs(0b11, 0b0100, 0b0100, 0b001, reg);
  }

  inline void set_fpsr(Register reg)
  {
    msr(0b011, 0b0100, 0b0100, 0b001, reg);
  }

  inline void clear_fpsr()
  {
    msr(0b011, 0b0100, 0b0100, 0b001, zr);
  }

  // DCZID_EL0: op1 == 011
  //            CRn == 0000
  //            CRm == 0000
  //            op2 == 111
  inline void get_dczid_el0(Register reg)
  {
    mrs(0b011, 0b0000, 0b0000, 0b111, reg);
  }

  // CTR_EL0:   op1 == 011
  //            CRn == 0000
  //            CRm == 0000
  //            op2 == 001
  inline void get_ctr_el0(Register reg)
  {
    mrs(0b011, 0b0000, 0b0000, 0b001, reg);
  }

  // idiv variant which deals with MINLONG as dividend and -1 as divisor
  int corrected_idivl(Register result, Register ra, Register rb,
                      bool want_remainder, Register tmp = rscratch1);
  int corrected_idivq(Register result, Register ra, Register rb,
                      bool want_remainder, Register tmp = rscratch1);

  // Support for NULL-checks
  //
  // Generates code that causes a NULL OS exception if the content of reg is NULL.
  // If the accessed location is M[reg + offset] and the offset is known, provide the
  // offset. No explicit code generation is needed if the offset is within a certain
  // range (0 <= offset <= page_size).

  virtual void null_check(Register reg, int offset = -1);
  static bool needs_explicit_null_check(intptr_t offset);
  static bool uses_implicit_null_check(void* address);

  static address target_addr_for_insn(address insn_addr, unsigned insn);
  static address target_addr_for_insn(address insn_addr) {
    unsigned insn = *(unsigned*)insn_addr;
    return target_addr_for_insn(insn_addr, insn);
  }

  // Required platform-specific helpers for Label::patch_instructions.
  // They _shadow_ the declarations in AbstractAssembler, which are undefined.
  static int pd_patch_instruction_size(address branch, address target);
  static void pd_patch_instruction(address branch, address target, const char* file = NULL, int line = 0) {
    pd_patch_instruction_size(branch, target);
  }
  static address pd_call_destination(address branch) {
    return target_addr_for_insn(branch);
  }
#ifndef PRODUCT
  static void pd_print_patched_instruction(address branch);
#endif

  static int patch_oop(address insn_addr, address o);
  static int patch_narrow_klass(address insn_addr, narrowKlass n);

  address emit_trampoline_stub(int insts_call_instruction_offset, address target);
  void emit_static_call_stub();

  // The following 4 methods return the offset of the appropriate move instruction

  // Support for fast byte/short loading with zero extension (depending on particular CPU)
  int load_unsigned_byte(Register dst, Address src);
  int load_unsigned_short(Register dst, Address src);

  // Support for fast byte/short loading with sign extension (depending on particular CPU)
  int load_signed_byte(Register dst, Address src);
  int load_signed_short(Register dst, Address src);

  int load_signed_byte32(Register dst, Address src);
  int load_signed_short32(Register dst, Address src);

  // Support for sign-extension (hi:lo = extend_sign(lo))
  void extend_sign(Register hi, Register lo);

  // Load and store values by size and signed-ness
  void load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed, Register dst2 = noreg);
  void store_sized_value(Address dst, Register src, size_t size_in_bytes, Register src2 = noreg);

  // Support for inc/dec with optimal instruction selection depending on value

  // x86_64 aliases an unqualified register/address increment and
  // decrement to call incrementq and decrementq but also supports
  // explicitly sized calls to incrementq/decrementq or
  // incrementl/decrementl

  // for aarch64 the proper convention would be to use
  // increment/decrement for 64 bit operatons and
  // incrementw/decrementw for 32 bit operations. so when porting
  // x86_64 code we can leave calls to increment/decrement as is,
  // replace incrementq/decrementq with increment/decrement and
  // replace incrementl/decrementl with incrementw/decrementw.

  // n.b. increment/decrement calls with an Address destination will
  // need to use a scratch register to load the value to be
  // incremented. increment/decrement calls which add or subtract a
  // constant value greater than 2^12 will need to use a 2nd scratch
  // register to hold the constant. so, a register increment/decrement
  // may trash rscratch2 and an address increment/decrement trash
  // rscratch and rscratch2

  void decrementw(Address dst, int value = 1);
  void decrementw(Register reg, int value = 1);

  void decrement(Register reg, int value = 1);
  void decrement(Address dst, int value = 1);

  void incrementw(Address dst, int value = 1);
  void incrementw(Register reg, int value = 1);

  void increment(Register reg, int value = 1);
  void increment(Address dst, int value = 1);


  // Alignment
  void align(int modulus);

  // Stack frame creation/removal
  void enter()
  {
    stp(rfp, lr, Address(pre(sp, -2 * wordSize)));
    mov(rfp, sp);
  }
  void leave()
  {
    mov(sp, rfp);
    ldp(rfp, lr, Address(post(sp, 2 * wordSize)));
  }

  // Support for getting the JavaThread pointer (i.e.; a reference to thread-local information)
  // The pointer will be loaded into the thread register.
  void get_thread(Register thread);


  // Support for VM calls
  //
  // It is imperative that all calls into the VM are handled via the call_VM macros.
  // They make sure that the stack linkage is setup correctly. call_VM's correspond
  // to ENTRY/ENTRY_X entry points while call_VM_leaf's correspond to LEAF entry points.


  void call_VM(Register oop_result,
               address entry_point,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  // Overloadings with last_Java_sp
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               int number_of_arguments = 0,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, bool
               check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2,
               bool check_exceptions = true);
  void call_VM(Register oop_result,
               Register last_java_sp,
               address entry_point,
               Register arg_1, Register arg_2, Register arg_3,
               bool check_exceptions = true);

  void get_vm_result  (Register oop_result, Register thread);
  void get_vm_result_2(Register metadata_result, Register thread);

  // These always tightly bind to MacroAssembler::call_VM_base
  // bypassing the virtual implementation
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, int number_of_arguments = 0, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);
  void super_call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4, bool check_exceptions = true);

  void call_VM_leaf(address entry_point,
                    int number_of_arguments = 0);
  void call_VM_leaf(address entry_point,
                    Register arg_1);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point,
                    Register arg_1, Register arg_2, Register arg_3);

  // These always tightly bind to MacroAssembler::call_VM_leaf_base
  // bypassing the virtual implementation
  void super_call_VM_leaf(address entry_point);
  void super_call_VM_leaf(address entry_point, Register arg_1);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);
  void super_call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4);

  // last Java Frame (fills frame anchor)
  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           address last_java_pc,
                           Register scratch);

  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           Label &last_java_pc,
                           Register scratch);

  void set_last_Java_frame(Register last_java_sp,
                           Register last_java_fp,
                           Register last_java_pc,
                           Register scratch);

  void reset_last_Java_frame(Register thread);

  // thread in the default location (rthread)
  void reset_last_Java_frame(bool clear_fp);

  // Stores
  void store_check(Register obj);                // store check for obj - register is destroyed afterwards
  void store_check(Register obj, Address dst);   // same as above, dst is exact store location (reg. is destroyed)

  void resolve_jobject(Register value, Register thread, Register tmp);

  // C 'boolean' to Java boolean: x == 0 ? 0 : 1
  void c2bool(Register x);

  void load_method_holder_cld(Register rresult, Register rmethod);
  void load_method_holder(Register holder, Register method);

  // oop manipulations
  void load_klass(Register dst, Register src);
  void store_klass(Register dst, Register src);
  void cmp_klass(Register oop, Register trial_klass, Register tmp);

  void resolve_weak_handle(Register result, Register tmp);
  void resolve_oop_handle(Register result, Register tmp = r5);
  void load_mirror(Register dst, Register method, Register tmp = r5);

  void access_load_at(BasicType type, DecoratorSet decorators, Register dst, Address src,
                      Register tmp1, Register tmp_thread);

  void access_store_at(BasicType type, DecoratorSet decorators, Address dst, Register src,
                       Register tmp1, Register tmp_thread);

  void load_heap_oop(Register dst, Address src, Register tmp1 = noreg,
                     Register thread_tmp = noreg, DecoratorSet decorators = 0);

  void load_heap_oop_not_null(Register dst, Address src, Register tmp1 = noreg,
                              Register thread_tmp = noreg, DecoratorSet decorators = 0);
  void store_heap_oop(Address dst, Register src, Register tmp1 = noreg,
                      Register tmp_thread = noreg, DecoratorSet decorators = 0);

  // currently unimplemented
  // Used for storing NULL. All other oop constants should be
  // stored using routines that take a jobject.
  void store_heap_oop_null(Address dst);

  void store_klass_gap(Register dst, Register src);

  // This dummy is to prevent a call to store_heap_oop from
  // converting a zero (like NULL) into a Register by giving
  // the compiler two choices it can't resolve

  void store_heap_oop(Address dst, void* dummy);

  void encode_heap_oop(Register d, Register s);
  void encode_heap_oop(Register r) { encode_heap_oop(r, r); }
  void decode_heap_oop(Register d, Register s);
  void decode_heap_oop(Register r) { decode_heap_oop(r, r); }
  void encode_heap_oop_not_null(Register r);
  void decode_heap_oop_not_null(Register r);
  void encode_heap_oop_not_null(Register dst, Register src);
  void decode_heap_oop_not_null(Register dst, Register src);

  void set_narrow_oop(Register dst, jobject obj);

  void encode_klass_not_null(Register r);
  void decode_klass_not_null(Register r);
  void encode_klass_not_null(Register dst, Register src);
  void decode_klass_not_null(Register dst, Register src);

  void set_narrow_klass(Register dst, Klass* k);

  // if heap base register is used - reinit it with the correct value
  void reinit_heapbase();

  DEBUG_ONLY(void verify_heapbase(const char* msg);)

  void push_CPU_state(bool save_vectors = false, bool use_sve = false,
                      int sve_vector_size_in_bytes = 0);
  void pop_CPU_state(bool restore_vectors = false, bool use_sve = false,
                      int sve_vector_size_in_bytes = 0);

  // Round up to a power of two
  void round_to(Register reg, int modulus);

  // allocation
  void eden_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void tlab_allocate(
    Register obj,                      // result: pointer to object after successful allocation
    Register var_size_in_bytes,        // object size in bytes if unknown at compile time; invalid otherwise
    int      con_size_in_bytes,        // object size in bytes if   known at compile time
    Register t1,                       // temp register
    Register t2,                       // temp register
    Label&   slow_case                 // continuation point if fast allocation fails
  );
  void verify_tlab();

  // interface method calling
  void lookup_interface_method(Register recv_klass,
                               Register intf_klass,
                               RegisterOrConstant itable_index,
                               Register method_result,
                               Register scan_temp,
                               Label& no_such_interface,
                   bool return_method = true);

  // virtual method calling
  // n.b. x86 allows RegisterOrConstant for vtable_index
  void lookup_virtual_method(Register recv_klass,
                             RegisterOrConstant vtable_index,
                             Register method_result);

  // Test sub_klass against super_klass, with fast and slow paths.

  // The fast path produces a tri-state answer: yes / no / maybe-slow.
  // One of the three labels can be NULL, meaning take the fall-through.
  // If super_check_offset is -1, the value is loaded up from super_klass.
  // No registers are killed, except temp_reg.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     Label* L_slow_path,
                RegisterOrConstant super_check_offset = RegisterOrConstant(-1));

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // The temp_reg and temp2_reg can be noreg, if no temps are available.
  // Updates the sub's secondary super cache as necessary.
  // If set_cond_codes, condition codes will be Z on success, NZ on failure.
  void check_klass_subtype_slow_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Register temp2_reg,
                                     Label* L_success,
                                     Label* L_failure,
                                     bool set_cond_codes = false);

  // Simplified, combined version, good for typical uses.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp_reg,
                           Label& L_success);

  void clinit_barrier(Register klass,
                      Register thread,
                      Label* L_fast_path = NULL,
                      Label* L_slow_path = NULL);

  Address argument_address(RegisterOrConstant arg_slot, int extra_slot_offset = 0);

  void verify_sve_vector_length();
  void reinitialize_ptrue() {
    if (UseSVE > 0) {
      sve_ptrue(ptrue, B);
    }
  }
  void verify_ptrue();

  // Debugging

  // only if +VerifyOops
  void verify_oop(Register reg, const char* s = "broken oop");
  void verify_oop_addr(Address addr, const char * s = "broken oop addr");

// TODO: verify method and klass metadata (compare against vptr?)
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line){}

#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

  // only if +VerifyFPU
  void verify_FPU(int stack_depth, const char* s = "illegal FPU state");

  // prints msg, dumps registers and stops execution
  void stop(const char* msg);

  static void debug64(char* msg, int64_t pc, int64_t regs[]);

  void untested()                                { stop("untested"); }

  void unimplemented(const char* what = "");

  void should_not_reach_here()                   { stop("should not reach here"); }

  // Stack overflow checking
  void bang_stack_with_offset(int offset) {
    // stack grows down, caller passes positive offset
    assert(offset > 0, "must bang with negative offset");
    sub(rscratch2, sp, offset);
    str(zr, Address(rscratch2));
  }

  // Writes to stack successive pages until offset reached to check for
  // stack overflow + shadow pages.  Also, clobbers tmp
  void bang_stack_size(Register size, Register tmp);

  // Check for reserved stack access in method being exited (for JIT)
  void reserved_stack_check();

  // Arithmetics

  void addptr(const Address &dst, int32_t src);
  void cmpptr(Register src1, Address src2);

  void cmpoop(Register obj1, Register obj2);

  // Various forms of CAS

  void cmpxchg_obj_header(Register oldv, Register newv, Register obj, Register tmp,
                          Label &suceed, Label *fail);
  void cmpxchgptr(Register oldv, Register newv, Register addr, Register tmp,
                  Label &suceed, Label *fail);

  void cmpxchgw(Register oldv, Register newv, Register addr, Register tmp,
                  Label &suceed, Label *fail);

  void atomic_add(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addw(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addal(Register prev, RegisterOrConstant incr, Register addr);
  void atomic_addalw(Register prev, RegisterOrConstant incr, Register addr);

  void atomic_xchg(Register prev, Register newv, Register addr);
  void atomic_xchgw(Register prev, Register newv, Register addr);
  void atomic_xchgl(Register prev, Register newv, Register addr);
  void atomic_xchglw(Register prev, Register newv, Register addr);
  void atomic_xchgal(Register prev, Register newv, Register addr);
  void atomic_xchgalw(Register prev, Register newv, Register addr);

  void orptr(Address adr, RegisterOrConstant src) {
    ldr(rscratch1, adr);
    if (src.is_register())
      orr(rscratch1, rscratch1, src.as_register());
    else
      orr(rscratch1, rscratch1, src.as_constant());
    str(rscratch1, adr);
  }

  // A generic CAS; success or failure is in the EQ flag.
  // Clobbers rscratch1
  void cmpxchg(Register addr, Register expected, Register new_val,
               enum operand_size size,
               bool acquire, bool release, bool weak,
               Register result);

private:
  void compare_eq(Register rn, Register rm, enum operand_size size);

#ifdef ASSERT
  // Template short-hand support to clean-up after a failed call to trampoline
  // call generation (see trampoline_call() below),  when a set of Labels must
  // be reset (before returning).
  template<typename Label, typename... More>
  void reset_labels(Label &lbl, More&... more) {
    lbl.reset(); reset_labels(more...);
  }
  template<typename Label>
  void reset_labels(Label &lbl) {
    lbl.reset();
  }
#endif

public:
  // Calls

  address trampoline_call(Address entry, CodeBuffer* cbuf = NULL);

  static bool far_branches() {
    return ReservedCodeCacheSize > branch_range;
  }

  // Jumps that can reach anywhere in the code cache.
  // Trashes tmp.
  void far_call(Address entry, CodeBuffer *cbuf = NULL, Register tmp = rscratch1);
  void far_jump(Address entry, CodeBuffer *cbuf = NULL, Register tmp = rscratch1);

  static int far_branch_size() {
    if (far_branches()) {
      return 3 * 4;  // adrp, add, br
    } else {
      return 4;
    }
  }

  // Emit the CompiledIC call idiom
  address ic_call(address entry, jint method_index = 0);

public:

  // Data

  void mov_metadata(Register dst, Metadata* obj);
  Address allocate_metadata_address(Metadata* obj);
  Address constant_oop_address(jobject obj);

  void movoop(Register dst, jobject obj, bool immediate = false);

  // CRC32 code for java.util.zip.CRC32::updateBytes() instrinsic.
  void kernel_crc32(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3);
  // CRC32 code for java.util.zip.CRC32C::updateBytes() instrinsic.
  void kernel_crc32c(Register crc, Register buf, Register len,
        Register table0, Register table1, Register table2, Register table3,
        Register tmp, Register tmp2, Register tmp3);

  // Stack push and pop individual 64 bit registers
  void push(Register src);
  void pop(Register dst);

  // push all registers onto the stack
  void pusha();
  void popa();

  void repne_scan(Register addr, Register value, Register count,
                  Register scratch);
  void repne_scanw(Register addr, Register value, Register count,
                   Register scratch);

  typedef void (MacroAssembler::* add_sub_imm_insn)(Register Rd, Register Rn, unsigned imm);
  typedef void (MacroAssembler::* add_sub_reg_insn)(Register Rd, Register Rn, Register Rm, enum shift_kind kind, unsigned shift);

  // If a constant does not fit in an immediate field, generate some
  // number of MOV instructions and then perform the operation
  void wrap_add_sub_imm_insn(Register Rd, Register Rn, unsigned imm,
                             add_sub_imm_insn insn1,
                             add_sub_reg_insn insn2);
  // Seperate vsn which sets the flags
  void wrap_adds_subs_imm_insn(Register Rd, Register Rn, unsigned imm,
                             add_sub_imm_insn insn1,
                             add_sub_reg_insn insn2);

#define WRAP(INSN)                                                      \
  void INSN(Register Rd, Register Rn, unsigned imm) {                   \
    wrap_add_sub_imm_insn(Rd, Rn, imm, &Assembler::INSN, &Assembler::INSN); \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
             enum shift_kind kind, unsigned shift = 0) {                \
    Assembler::INSN(Rd, Rn, Rm, kind, shift);                           \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm) {                    \
    Assembler::INSN(Rd, Rn, Rm);                                        \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
           ext::operation option, int amount = 0) {                     \
    Assembler::INSN(Rd, Rn, Rm, option, amount);                        \
  }

  WRAP(add) WRAP(addw) WRAP(sub) WRAP(subw)

#undef WRAP
#define WRAP(INSN)                                                      \
  void INSN(Register Rd, Register Rn, unsigned imm) {                   \
    wrap_adds_subs_imm_insn(Rd, Rn, imm, &Assembler::INSN, &Assembler::INSN); \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
             enum shift_kind kind, unsigned shift = 0) {                \
    Assembler::INSN(Rd, Rn, Rm, kind, shift);                           \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm) {                    \
    Assembler::INSN(Rd, Rn, Rm);                                        \
  }                                                                     \
                                                                        \
  void INSN(Register Rd, Register Rn, Register Rm,                      \
           ext::operation option, int amount = 0) {                     \
    Assembler::INSN(Rd, Rn, Rm, option, amount);                        \
  }

  WRAP(adds) WRAP(addsw) WRAP(subs) WRAP(subsw)

  void add(Register Rd, Register Rn, RegisterOrConstant increment);
  void addw(Register Rd, Register Rn, RegisterOrConstant increment);
  void sub(Register Rd, Register Rn, RegisterOrConstant decrement);
  void subw(Register Rd, Register Rn, RegisterOrConstant decrement);

  void adrp(Register reg1, const Address &dest, uint64_t &byte_offset);

  void tableswitch(Register index, jint lowbound, jint highbound,
                   Label &jumptable, Label &jumptable_end, int stride = 1) {
    adr(rscratch1, jumptable);
    subsw(rscratch2, index, lowbound);
    subsw(zr, rscratch2, highbound - lowbound);
    br(Assembler::HS, jumptable_end);
    add(rscratch1, rscratch1, rscratch2,
        ext::sxtw, exact_log2(stride * Assembler::instruction_size));
    br(rscratch1);
  }

  // Form an address from base + offset in Rd.  Rd may or may not
  // actually be used: you must use the Address that is returned.  It
  // is up to you to ensure that the shift provided matches the size
  // of your data.
  Address form_address(Register Rd, Register base, int64_t byte_offset, int shift);

  // Return true iff an address is within the 48-bit AArch64 address
  // space.
  bool is_valid_AArch64_address(address a) {
    return ((uint64_t)a >> 48) == 0;
  }

  // Load the base of the cardtable byte map into reg.
  void load_byte_map_base(Register reg);

  // Prolog generator routines to support switch between x86 code and
  // generated ARM code

  // routine to generate an x86 prolog for a stub function which
  // bootstraps into the generated ARM code which directly follows the
  // stub
  //

  public:

  void ldr_constant(Register dest, const Address &const_addr) {
    if (NearCpool) {
      ldr(dest, const_addr);
    } else {
      uint64_t offset;
      adrp(dest, InternalAddress(const_addr.target()), offset);
      ldr(dest, Address(dest, offset));
    }
  }

  address read_polling_page(Register r, relocInfo::relocType rtype);
  void get_polling_page(Register dest, relocInfo::relocType rtype);

  // CRC32 code for java.util.zip.CRC32::updateBytes() instrinsic.
  void update_byte_crc32(Register crc, Register val, Register table);
  void update_word_crc32(Register crc, Register v, Register tmp,
        Register table0, Register table1, Register table2, Register table3,
        bool upper = false);

  address has_negatives(Register ary1, Register len, Register result);

  address arrays_equals(Register a1, Register a2, Register result, Register cnt1,
                        Register tmp1, Register tmp2, Register tmp3, int elem_size);

  void string_equals(Register a1, Register a2, Register result, Register cnt1,
                     int elem_size);

  void fill_words(Register base, Register cnt, Register value);
  void zero_words(Register base, uint64_t cnt);
  address zero_words(Register ptr, Register cnt);
  void zero_dcache_blocks(Register base, Register cnt);

  static const int zero_words_block_size;

  address byte_array_inflate(Register src, Register dst, Register len,
                             FloatRegister vtmp1, FloatRegister vtmp2,
                             FloatRegister vtmp3, Register tmp4);

  void char_array_compress(Register src, Register dst, Register len,
                           FloatRegister tmp1Reg, FloatRegister tmp2Reg,
                           FloatRegister tmp3Reg, FloatRegister tmp4Reg,
                           Register result);

  void encode_iso_array(Register src, Register dst,
                        Register len, Register result,
                        FloatRegister Vtmp1, FloatRegister Vtmp2,
                        FloatRegister Vtmp3, FloatRegister Vtmp4);
  void fast_log(FloatRegister vtmp0, FloatRegister vtmp1, FloatRegister vtmp2,
                FloatRegister vtmp3, FloatRegister vtmp4, FloatRegister vtmp5,
                FloatRegister tmpC1, FloatRegister tmpC2, FloatRegister tmpC3,
                FloatRegister tmpC4, Register tmp1, Register tmp2,
                Register tmp3, Register tmp4, Register tmp5);
  void generate_dsin_dcos(bool isCos, address npio2_hw, address two_over_pi,
      address pio2, address dsin_coef, address dcos_coef);
 private:
  // begin trigonometric functions support block
  void generate__ieee754_rem_pio2(address npio2_hw, address two_over_pi, address pio2);
  void generate__kernel_rem_pio2(address two_over_pi, address pio2);
  void generate_kernel_sin(FloatRegister x, bool iyIsOne, address dsin_coef);
  void generate_kernel_cos(FloatRegister x, address dcos_coef);
  // end trigonometric functions support block
  void add2_with_carry(Register final_dest_hi, Register dest_hi, Register dest_lo,
                       Register src1, Register src2);
  void add2_with_carry(Register dest_hi, Register dest_lo, Register src1, Register src2) {
    add2_with_carry(dest_hi, dest_hi, dest_lo, src1, src2);
  }
  void multiply_64_x_64_loop(Register x, Register xstart, Register x_xstart,
                             Register y, Register y_idx, Register z,
                             Register carry, Register product,
                             Register idx, Register kdx);
  void multiply_128_x_128_loop(Register y, Register z,
                               Register carry, Register carry2,
                               Register idx, Register jdx,
                               Register yz_idx1, Register yz_idx2,
                               Register tmp, Register tmp3, Register tmp4,
                               Register tmp7, Register product_hi);
  void kernel_crc32_using_crc32(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
  void kernel_crc32c_using_crc32c(Register crc, Register buf,
        Register len, Register tmp0, Register tmp1, Register tmp2,
        Register tmp3);
public:
  void multiply_to_len(Register x, Register xlen, Register y, Register ylen, Register z,
                       Register zlen, Register tmp1, Register tmp2, Register tmp3,
                       Register tmp4, Register tmp5, Register tmp6, Register tmp7);
  void mul_add(Register out, Register in, Register offs, Register len, Register k);

  // Place an ISB after code may have been modified due to a safepoint.
  void safepoint_isb();

private:
  // Return the effective address r + (r1 << ext) + offset.
  // Uses rscratch2.
  Address offsetted_address(Register r, Register r1, Address::extend ext,
                            int offset, int size);

private:
  // Returns an address on the stack which is reachable with a ldr/str of size
  // Uses rscratch2 if the address is not directly reachable
  Address spill_address(int size, int offset, Register tmp=rscratch2);
  Address sve_spill_address(int sve_reg_size_in_bytes, int offset, Register tmp=rscratch2);

  bool merge_alignment_check(Register base, size_t size, int64_t cur_offset, int64_t prev_offset) const;

  // Check whether two loads/stores can be merged into ldp/stp.
  bool ldst_can_merge(Register rx, const Address &adr, size_t cur_size_in_bytes, bool is_store) const;

  // Merge current load/store with previous load/store into ldp/stp.
  void merge_ldst(Register rx, const Address &adr, size_t cur_size_in_bytes, bool is_store);

  // Try to merge two loads/stores into ldp/stp. If success, returns true else false.
  bool try_merge_ldst(Register rt, const Address &adr, size_t cur_size_in_bytes, bool is_store);

public:
  void spill(Register Rx, bool is64, int offset) {
    if (is64) {
      str(Rx, spill_address(8, offset));
    } else {
      strw(Rx, spill_address(4, offset));
    }
  }
  void spill(FloatRegister Vx, SIMD_RegVariant T, int offset) {
    str(Vx, T, spill_address(1 << (int)T, offset));
  }
  void spill_sve_vector(FloatRegister Zx, int offset, int vector_reg_size_in_bytes) {
    sve_str(Zx, sve_spill_address(vector_reg_size_in_bytes, offset));
  }
  void unspill(Register Rx, bool is64, int offset) {
    if (is64) {
      ldr(Rx, spill_address(8, offset));
    } else {
      ldrw(Rx, spill_address(4, offset));
    }
  }
  void unspill(FloatRegister Vx, SIMD_RegVariant T, int offset) {
    ldr(Vx, T, spill_address(1 << (int)T, offset));
  }
  void unspill_sve_vector(FloatRegister Zx, int offset, int vector_reg_size_in_bytes) {
    sve_ldr(Zx, sve_spill_address(vector_reg_size_in_bytes, offset));
  }
  void spill_copy128(int src_offset, int dst_offset,
                     Register tmp1=rscratch1, Register tmp2=rscratch2) {
    if (src_offset < 512 && (src_offset & 7) == 0 &&
        dst_offset < 512 && (dst_offset & 7) == 0) {
      ldp(tmp1, tmp2, Address(sp, src_offset));
      stp(tmp1, tmp2, Address(sp, dst_offset));
    } else {
      unspill(tmp1, true, src_offset);
      spill(tmp1, true, dst_offset);
      unspill(tmp1, true, src_offset+8);
      spill(tmp1, true, dst_offset+8);
    }
  }
  void spill_copy_sve_vector_stack_to_stack(int src_offset, int dst_offset,
                                            int sve_vec_reg_size_in_bytes) {
    assert(sve_vec_reg_size_in_bytes % 16 == 0, "unexpected sve vector reg size");
    for (int i = 0; i < sve_vec_reg_size_in_bytes / 16; i++) {
      spill_copy128(src_offset, dst_offset);
      src_offset += 16;
      dst_offset += 16;
    }
  }
  void cache_wb(Address line);
  void cache_wbsync(bool is_pre);

private:
  // Check the current thread doesn't need a cross modify fence.
  void verify_cross_modify_fence_not_required() PRODUCT_RETURN;

};

#ifdef ASSERT
inline bool AbstractAssembler::pd_check_instruction_mark() { return false; }
#endif

/**
 * class SkipIfEqual:
 *
 * Instantiating this class will result in assembly code being output that will
 * jump around any code emitted between the creation of the instance and it's
 * automatic destruction at the end of a scope block, depending on the value of
 * the flag passed to the constructor, which will be checked at run-time.
 */
class SkipIfEqual {
 private:
  MacroAssembler* _masm;
  Label _label;

 public:
   SkipIfEqual(MacroAssembler*, const bool* flag_addr, bool value);
   ~SkipIfEqual();
};

struct tableswitch {
  Register _reg;
  int _insn_index; jint _first_key; jint _last_key;
  Label _after;
  Label _branches;
};

#endif // CPU_AARCH64_MACROASSEMBLER_AARCH64_HPP
