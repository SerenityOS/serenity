/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_MACROASSEMBLER_ARM_HPP
#define CPU_ARM_MACROASSEMBLER_ARM_HPP

#include "code/relocInfo.hpp"
#include "utilities/powerOfTwo.hpp"

// Introduced AddressLiteral and its subclasses to ease portability from
// x86 and avoid relocation issues
class AddressLiteral {
  RelocationHolder _rspec;
  // Typically we use AddressLiterals we want to use their rval
  // However in some situations we want the lval (effect address) of the item.
  // We provide a special factory for making those lvals.
  bool _is_lval;

  address          _target;

 private:
  static relocInfo::relocType reloc_for_target(address target) {
    // Used for ExternalAddress or when the type is not specified
    // Sometimes ExternalAddress is used for values which aren't
    // exactly addresses, like the card table base.
    // external_word_type can't be used for values in the first page
    // so just skip the reloc in that case.
    return external_word_Relocation::can_be_relocated(target) ? relocInfo::external_word_type : relocInfo::none;
  }

  void set_rspec(relocInfo::relocType rtype);

 protected:
  // creation
  AddressLiteral()
    : _is_lval(false),
      _target(NULL)
  {}

  public:

  AddressLiteral(address target, relocInfo::relocType rtype) {
    _is_lval = false;
    _target = target;
    set_rspec(rtype);
  }

  AddressLiteral(address target, RelocationHolder const& rspec)
    : _rspec(rspec),
      _is_lval(false),
      _target(target)
  {}

  AddressLiteral(address target) {
    _is_lval = false;
    _target = target;
    set_rspec(reloc_for_target(target));
  }

  AddressLiteral addr() {
    AddressLiteral ret = *this;
    ret._is_lval = true;
    return ret;
  }

 private:

  address target() { return _target; }
  bool is_lval() { return _is_lval; }

  relocInfo::relocType reloc() const { return _rspec.type(); }
  const RelocationHolder& rspec() const { return _rspec; }

  friend class Assembler;
  friend class MacroAssembler;
  friend class Address;
  friend class LIR_Assembler;
  friend class InlinedAddress;
};

class ExternalAddress: public AddressLiteral {

  public:

  ExternalAddress(address target) : AddressLiteral(target) {}

};

class InternalAddress: public AddressLiteral {

  public:

  InternalAddress(address target) : AddressLiteral(target, relocInfo::internal_word_type) {}

};

// Inlined constants, for use with ldr_literal / bind_literal
// Note: InlinedInteger not supported (use move_slow(Register,int[,cond]))
class InlinedLiteral: StackObj {
 public:
  Label label; // need to be public for direct access with &
  InlinedLiteral() {
  }
};

class InlinedMetadata: public InlinedLiteral {
 private:
  Metadata *_data;

 public:
  InlinedMetadata(Metadata *data): InlinedLiteral() {
    _data = data;
  }
  Metadata *data() { return _data; }
};

// Currently unused
// class InlinedOop: public InlinedLiteral {
//  private:
//   jobject _jobject;
//
//  public:
//   InlinedOop(jobject target): InlinedLiteral() {
//     _jobject = target;
//   }
//   jobject jobject() { return _jobject; }
// };

class InlinedAddress: public InlinedLiteral {
 private:
  AddressLiteral _literal;

 public:

  InlinedAddress(jobject object): InlinedLiteral(), _literal((address)object, relocInfo::oop_type) {
    ShouldNotReachHere(); // use mov_oop (or implement InlinedOop)
  }

  InlinedAddress(Metadata *data): InlinedLiteral(), _literal((address)data, relocInfo::metadata_type) {
    ShouldNotReachHere(); // use InlinedMetadata or mov_metadata
  }

  InlinedAddress(address target, const RelocationHolder &rspec): InlinedLiteral(), _literal(target, rspec) {
    assert(rspec.type() != relocInfo::oop_type, "Do not use InlinedAddress for oops");
    assert(rspec.type() != relocInfo::metadata_type, "Do not use InlinedAddress for metadatas");
  }

  InlinedAddress(address target, relocInfo::relocType rtype): InlinedLiteral(), _literal(target, rtype) {
    assert(rtype != relocInfo::oop_type, "Do not use InlinedAddress for oops");
    assert(rtype != relocInfo::metadata_type, "Do not use InlinedAddress for metadatas");
  }

  // Note: default is relocInfo::none for InlinedAddress
  InlinedAddress(address target): InlinedLiteral(), _literal(target, relocInfo::none) {
  }

  address target() { return _literal.target(); }

  const RelocationHolder& rspec() const { return _literal.rspec(); }
};

class InlinedString: public InlinedLiteral {
 private:
  const char* _msg;

 public:
  InlinedString(const char* msg): InlinedLiteral() {
    _msg = msg;
  }
  const char* msg() { return _msg; }
};

class MacroAssembler: public Assembler {
protected:

  // Support for VM calls
  //

  // This is the base routine called by the different versions of call_VM_leaf.
  void call_VM_leaf_helper(address entry_point, int number_of_arguments);

  // This is the base routine called by the different versions of call_VM. The interpreter
  // may customize this version by overriding it for its purposes (e.g., to save/restore
  // additional registers when doing a VM call).
  virtual void call_VM_helper(Register oop_result, address entry_point, int number_of_arguments, bool check_exceptions);
public:

  MacroAssembler(CodeBuffer* code) : Assembler(code) {}

  // These routines should emit JVMTI PopFrame and ForceEarlyReturn handling code.
  // The implementation is only non-empty for the InterpreterMacroAssembler,
  // as only the interpreter handles PopFrame and ForceEarlyReturn requests.
  virtual void check_and_handle_popframe() {}
  virtual void check_and_handle_earlyret() {}

  // By default, we do not need relocation information for non
  // patchable absolute addresses. However, when needed by some
  // extensions, ignore_non_patchable_relocations can be modified,
  // returning false to preserve all relocation information.
  inline bool ignore_non_patchable_relocations() { return true; }

  void align(int modulus);

  // Support for VM calls
  //
  // It is imperative that all calls into the VM are handled via the call_VM methods.
  // They make sure that the stack linkage is setup correctly. call_VM's correspond
  // to ENTRY/ENTRY_X entry points while call_VM_leaf's correspond to LEAF entry points.

  void call_VM(Register oop_result, address entry_point, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);

  // The following methods are required by templateTable.cpp,
  // but not used on ARM.
  void call_VM(Register oop_result, Register last_java_sp, address entry_point, int number_of_arguments = 0, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, bool check_exceptions = true);
  void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3, bool check_exceptions = true);

  // Note: The super_call_VM calls are not used on ARM

  // Raw call, without saving/restoring registers, exception handling, etc.
  // Mainly used from various stubs.
  // Note: if 'save_R9_if_scratched' is true, call_VM may on some
  // platforms save values on the stack. Set it to false (and handle
  // R9 in the callers) if the top of the stack must not be modified
  // by call_VM.
  void call_VM(address entry_point, bool save_R9_if_scratched);

  void call_VM_leaf(address entry_point);
  void call_VM_leaf(address entry_point, Register arg_1);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3);
  void call_VM_leaf(address entry_point, Register arg_1, Register arg_2, Register arg_3, Register arg_4);

  void get_vm_result(Register oop_result, Register tmp);
  void get_vm_result_2(Register metadata_result, Register tmp);

  // Always sets/resets sp, which default to SP if (last_sp == noreg)
  // Optionally sets/resets fp (use noreg to avoid setting it)
  // Optionally sets/resets pc depending on save_last_java_pc flag
  // Note: when saving PC, set_last_Java_frame returns PC's offset in the code section
  //       (for oop_maps offset computation)
  int set_last_Java_frame(Register last_sp, Register last_fp, bool save_last_java_pc, Register tmp);
  void reset_last_Java_frame(Register tmp);
  // status set in set_last_Java_frame for reset_last_Java_frame
  bool _fp_saved;
  bool _pc_saved;

#ifdef PRODUCT
#define BLOCK_COMMENT(str) /* nothing */
#define STOP(error) __ stop(error)
#else
#define BLOCK_COMMENT(str) __ block_comment(str)
#define STOP(error) __ block_comment(error); __ stop(error)
#endif

  void lookup_virtual_method(Register recv_klass,
                             Register vtable_index,
                             Register method_result);

  // Test sub_klass against super_klass, with fast and slow paths.

  // The fast path produces a tri-state answer: yes / no / maybe-slow.
  // One of the three labels can be NULL, meaning take the fall-through.
  // No registers are killed, except temp_regs.
  void check_klass_subtype_fast_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Register temp_reg2,
                                     Label* L_success,
                                     Label* L_failure,
                                     Label* L_slow_path);

  // The rest of the type check; must be wired to a corresponding fast path.
  // It does not repeat the fast path logic, so don't use it standalone.
  // temp_reg3 can be noreg, if no temps are available.
  // Updates the sub's secondary super cache as necessary.
  // If set_cond_codes:
  // - condition codes will be Z on success, NZ on failure.
  // - temp_reg will be 0 on success, non-0 on failure
  void check_klass_subtype_slow_path(Register sub_klass,
                                     Register super_klass,
                                     Register temp_reg,
                                     Register temp_reg2,
                                     Register temp_reg3, // auto assigned if noreg
                                     Label* L_success,
                                     Label* L_failure,
                                     bool set_cond_codes = false);

  // Simplified, combined version, good for typical uses.
  // temp_reg3 can be noreg, if no temps are available. It is used only on slow path.
  // Falls through on failure.
  void check_klass_subtype(Register sub_klass,
                           Register super_klass,
                           Register temp_reg,
                           Register temp_reg2,
                           Register temp_reg3, // auto assigned on slow path if noreg
                           Label& L_success);

  // Returns address of receiver parameter, using tmp as base register. tmp and params_count can be the same.
  Address receiver_argument_address(Register params_base, Register params_count, Register tmp);

  void _verify_oop(Register reg, const char* s, const char* file, int line);
  void _verify_oop_addr(Address addr, const char * s, const char* file, int line);

  // TODO: verify method and klass metadata (compare against vptr?)
  void _verify_method_ptr(Register reg, const char * msg, const char * file, int line) {}
  void _verify_klass_ptr(Register reg, const char * msg, const char * file, int line) {}

#define verify_oop(reg) _verify_oop(reg, "broken oop " #reg, __FILE__, __LINE__)
#define verify_oop_addr(addr) _verify_oop_addr(addr, "broken oop ", __FILE__, __LINE__)
#define verify_method_ptr(reg) _verify_method_ptr(reg, "broken method " #reg, __FILE__, __LINE__)
#define verify_klass_ptr(reg) _verify_klass_ptr(reg, "broken klass " #reg, __FILE__, __LINE__)

  void null_check(Register reg, Register tmp, int offset = -1);
  inline void null_check(Register reg) { null_check(reg, noreg, -1); } // for C1 lir_null_check

  // Puts address of allocated object into register `obj` and end of allocated object into register `obj_end`.
  void eden_allocate(Register obj, Register obj_end, Register tmp1, Register tmp2,
                     RegisterOrConstant size_expression, Label& slow_case);
  void tlab_allocate(Register obj, Register obj_end, Register tmp1,
                     RegisterOrConstant size_expression, Label& slow_case);

  void zero_memory(Register start, Register end, Register tmp);

  static bool needs_explicit_null_check(intptr_t offset);
  static bool uses_implicit_null_check(void* address);

  void arm_stack_overflow_check(int frame_size_in_bytes, Register tmp);
  void arm_stack_overflow_check(Register Rsize, Register tmp);

  void bang_stack_with_offset(int offset) {
    ShouldNotReachHere();
  }

  void resolve_jobject(Register value, Register tmp1, Register tmp2);

  void nop() {
    mov(R0, R0);
  }

  void push(Register rd, AsmCondition cond = al) {
    assert(rd != SP, "unpredictable instruction");
    str(rd, Address(SP, -wordSize, pre_indexed), cond);
  }

  void push(RegisterSet reg_set, AsmCondition cond = al) {
    assert(!reg_set.contains(SP), "unpredictable instruction");
    stmdb(SP, reg_set, writeback, cond);
  }

  void pop(Register rd, AsmCondition cond = al) {
    assert(rd != SP, "unpredictable instruction");
    ldr(rd, Address(SP, wordSize, post_indexed), cond);
  }

  void pop(RegisterSet reg_set, AsmCondition cond = al) {
    assert(!reg_set.contains(SP), "unpredictable instruction");
    ldmia(SP, reg_set, writeback, cond);
  }

  void fpushd(FloatRegister fd, AsmCondition cond = al) {
    fstmdbd(SP, FloatRegisterSet(fd), writeback, cond);
  }

  void fpushs(FloatRegister fd, AsmCondition cond = al) {
    fstmdbs(SP, FloatRegisterSet(fd), writeback, cond);
  }

  void fpopd(FloatRegister fd, AsmCondition cond = al) {
    fldmiad(SP, FloatRegisterSet(fd), writeback, cond);
  }

  void fpops(FloatRegister fd, AsmCondition cond = al) {
    fldmias(SP, FloatRegisterSet(fd), writeback, cond);
  }

  void fpush(FloatRegisterSet reg_set) {
    fstmdbd(SP, reg_set, writeback);
  }

  void fpop(FloatRegisterSet reg_set) {
    fldmiad(SP, reg_set, writeback);
  }

  void fpush_hardfp(FloatRegisterSet reg_set) {
#ifndef __SOFTFP__
    fpush(reg_set);
#endif
  }

  void fpop_hardfp(FloatRegisterSet reg_set) {
#ifndef __SOFTFP__
    fpop(reg_set);
#endif
  }

  // Order access primitives
  enum Membar_mask_bits {
    StoreStore = 1 << 3,
    LoadStore  = 1 << 2,
    StoreLoad  = 1 << 1,
    LoadLoad   = 1 << 0
  };

  void membar(Membar_mask_bits mask,
              Register tmp,
              bool preserve_flags = true,
              Register load_tgt = noreg);

  void breakpoint(AsmCondition cond = al);
  void stop(const char* msg);
  // prints msg and continues
  void warn(const char* msg);
  void unimplemented(const char* what = "");
  void should_not_reach_here()                   { stop("should not reach here"); }
  static void debug(const char* msg, const intx* registers);

  // Create a walkable frame to help tracking down who called this code.
  // Returns the frame size in words.
  int should_not_call_this() {
    raw_push(FP, LR);
    should_not_reach_here();
    flush();
    return 2; // frame_size_in_words (FP+LR)
  }

  int save_all_registers();
  void restore_all_registers();
  int save_caller_save_registers();
  void restore_caller_save_registers();

  void add_rc(Register dst, Register arg1, RegisterOrConstant arg2);

  // add_slow and mov_slow are used to manipulate offsets larger than 1024,
  // these functions are not expected to handle all possible constants,
  // only those that can really occur during compilation
  void add_slow(Register rd, Register rn, int c);
  void sub_slow(Register rd, Register rn, int c);


  void mov_slow(Register rd, intptr_t c, AsmCondition cond = al);
  void mov_slow(Register rd, const char *string);
  void mov_slow(Register rd, address addr);

  void patchable_mov_oop(Register rd, jobject o, int oop_index) {
    mov_oop(rd, o, oop_index);
  }
  void mov_oop(Register rd, jobject o, int index = 0, AsmCondition cond = al);

  void patchable_mov_metadata(Register rd, Metadata* o, int index) {
    mov_metadata(rd, o, index);
  }
  void mov_metadata(Register rd, Metadata* o, int index = 0);

  void mov_float(FloatRegister fd, jfloat c, AsmCondition cond = al);
  void mov_double(FloatRegister fd, jdouble c, AsmCondition cond = al);


  // Note: this variant of mov_address assumes the address moves with
  // the code. Do *not* implement it with non-relocated instructions,
  // unless PC-relative.
  void mov_relative_address(Register rd, address addr, AsmCondition cond = al) {
    int offset = addr - pc() - 8;
    assert((offset & 3) == 0, "bad alignment");
    if (offset >= 0) {
      assert(AsmOperand::is_rotated_imm(offset), "addr too far");
      add(rd, PC, offset, cond);
    } else {
      assert(AsmOperand::is_rotated_imm(-offset), "addr too far");
      sub(rd, PC, -offset, cond);
    }
  }

  // Runtime address that may vary from one execution to another.
  // Warning: do not implement as a PC relative address.
  void mov_address(Register rd, address addr) {
    mov_address(rd, addr, RelocationHolder::none);
  }

  // rspec can be RelocationHolder::none (for ignored symbolic Relocation).
  // In that case, the address is absolute and the generated code need
  // not be relocable.
  void mov_address(Register rd, address addr, RelocationHolder const& rspec) {
    assert(rspec.type() != relocInfo::runtime_call_type, "do not use mov_address for runtime calls");
    assert(rspec.type() != relocInfo::static_call_type, "do not use mov_address for relocable calls");
    if (rspec.type() == relocInfo::none) {
      // absolute address, relocation not needed
      mov_slow(rd, (intptr_t)addr);
      return;
    }
    if (VM_Version::supports_movw()) {
      relocate(rspec);
      int c = (int)addr;
      movw(rd, c & 0xffff);
      if ((unsigned int)c >> 16) {
        movt(rd, (unsigned int)c >> 16);
      }
      return;
    }
    Label skip_literal;
    InlinedAddress addr_literal(addr, rspec);
    ldr_literal(rd, addr_literal);
    b(skip_literal);
    bind_literal(addr_literal);
    bind(skip_literal);
  }

  // Note: Do not define mov_address for a Label
  //
  // Load from addresses potentially within the code are now handled
  // InlinedLiteral subclasses (to allow more flexibility on how the
  // ldr_literal is performed).

  void ldr_literal(Register rd, InlinedAddress& L) {
    assert(L.rspec().type() != relocInfo::runtime_call_type, "avoid ldr_literal for calls");
    assert(L.rspec().type() != relocInfo::static_call_type, "avoid ldr_literal for calls");
    relocate(L.rspec());
    ldr(rd, Address(PC, target(L.label) - pc() - 8));
  }

  void ldr_literal(Register rd, InlinedString& L) {
    const char* msg = L.msg();
    if (code()->consts()->contains((address)msg)) {
      // string address moves with the code
      ldr(rd, Address(PC, ((address)msg) - pc() - 8));
      return;
    }
    // Warning: use external strings with care. They are not relocated
    // if the code moves. If needed, use code_string to move them
    // to the consts section.
    ldr(rd, Address(PC, target(L.label) - pc() - 8));
  }

  void ldr_literal(Register rd, InlinedMetadata& L) {
    // relocation done in the bind_literal for metadatas
    ldr(rd, Address(PC, target(L.label) - pc() - 8));
  }

  void bind_literal(InlinedAddress& L) {
    bind(L.label);
    assert(L.rspec().type() != relocInfo::metadata_type, "Must use InlinedMetadata");
    // We currently do not use oop 'bound' literals.
    // If the code evolves and the following assert is triggered,
    // we need to implement InlinedOop (see InlinedMetadata).
    assert(L.rspec().type() != relocInfo::oop_type, "Inlined oops not supported");
    // Note: relocation is handled by relocate calls in ldr_literal
    AbstractAssembler::emit_address((address)L.target());
  }

  void bind_literal(InlinedString& L) {
    const char* msg = L.msg();
    if (code()->consts()->contains((address)msg)) {
      // The Label should not be used; avoid binding it
      // to detect errors.
      return;
    }
    bind(L.label);
    AbstractAssembler::emit_address((address)L.msg());
  }

  void bind_literal(InlinedMetadata& L) {
    bind(L.label);
    relocate(metadata_Relocation::spec_for_immediate());
    AbstractAssembler::emit_address((address)L.data());
  }

  void resolve_oop_handle(Register result);
  void load_mirror(Register mirror, Register method, Register tmp);

#define ARM_INSTR_1(common_mnemonic, arm32_mnemonic, arg_type) \
  void common_mnemonic(arg_type arg) { \
      arm32_mnemonic(arg); \
  }

#define ARM_INSTR_2(common_mnemonic, arm32_mnemonic, arg1_type, arg2_type) \
  void common_mnemonic(arg1_type arg1, arg2_type arg2) { \
      arm32_mnemonic(arg1, arg2); \
  }

#define ARM_INSTR_3(common_mnemonic, arm32_mnemonic, arg1_type, arg2_type, arg3_type) \
  void common_mnemonic(arg1_type arg1, arg2_type arg2, arg3_type arg3) { \
      arm32_mnemonic(arg1, arg2, arg3); \
  }

  ARM_INSTR_1(jump, bx,  Register)
  ARM_INSTR_1(call, blx, Register)

  ARM_INSTR_2(cbz_32,  cbz,  Register, Label&)
  ARM_INSTR_2(cbnz_32, cbnz, Register, Label&)

  ARM_INSTR_2(ldr_u32, ldr,  Register, Address)
  ARM_INSTR_2(ldr_s32, ldr,  Register, Address)
  ARM_INSTR_2(str_32,  str,  Register, Address)

  ARM_INSTR_2(mvn_32,  mvn,  Register, Register)
  ARM_INSTR_2(cmp_32,  cmp,  Register, Register)
  ARM_INSTR_2(neg_32,  neg,  Register, Register)
  ARM_INSTR_2(clz_32,  clz,  Register, Register)
  ARM_INSTR_2(rbit_32, rbit, Register, Register)

  ARM_INSTR_2(cmp_32,  cmp,  Register, int)
  ARM_INSTR_2(cmn_32,  cmn,  Register, int)

  ARM_INSTR_3(add_32,  add,  Register, Register, Register)
  ARM_INSTR_3(sub_32,  sub,  Register, Register, Register)
  ARM_INSTR_3(subs_32, subs, Register, Register, Register)
  ARM_INSTR_3(mul_32,  mul,  Register, Register, Register)
  ARM_INSTR_3(and_32,  andr, Register, Register, Register)
  ARM_INSTR_3(orr_32,  orr,  Register, Register, Register)
  ARM_INSTR_3(eor_32,  eor,  Register, Register, Register)

  ARM_INSTR_3(add_32,  add,  Register, Register, AsmOperand)
  ARM_INSTR_3(sub_32,  sub,  Register, Register, AsmOperand)
  ARM_INSTR_3(orr_32,  orr,  Register, Register, AsmOperand)
  ARM_INSTR_3(eor_32,  eor,  Register, Register, AsmOperand)
  ARM_INSTR_3(and_32,  andr, Register, Register, AsmOperand)


  ARM_INSTR_3(add_32,  add,  Register, Register, int)
  ARM_INSTR_3(adds_32, adds, Register, Register, int)
  ARM_INSTR_3(sub_32,  sub,  Register, Register, int)
  ARM_INSTR_3(subs_32, subs, Register, Register, int)

  ARM_INSTR_2(tst_32,  tst,  Register, unsigned int)
  ARM_INSTR_2(tst_32,  tst,  Register, AsmOperand)

  ARM_INSTR_3(and_32,  andr, Register, Register, uint)
  ARM_INSTR_3(orr_32,  orr,  Register, Register, uint)
  ARM_INSTR_3(eor_32,  eor,  Register, Register, uint)

  ARM_INSTR_1(cmp_zero_float,  fcmpzs, FloatRegister)
  ARM_INSTR_1(cmp_zero_double, fcmpzd, FloatRegister)

  ARM_INSTR_2(ldr_float,   flds,   FloatRegister, Address)
  ARM_INSTR_2(str_float,   fsts,   FloatRegister, Address)
  ARM_INSTR_2(mov_float,   fcpys,  FloatRegister, FloatRegister)
  ARM_INSTR_2(neg_float,   fnegs,  FloatRegister, FloatRegister)
  ARM_INSTR_2(abs_float,   fabss,  FloatRegister, FloatRegister)
  ARM_INSTR_2(sqrt_float,  fsqrts, FloatRegister, FloatRegister)
  ARM_INSTR_2(cmp_float,   fcmps,  FloatRegister, FloatRegister)

  ARM_INSTR_3(add_float,   fadds,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(sub_float,   fsubs,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(mul_float,   fmuls,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(div_float,   fdivs,  FloatRegister, FloatRegister, FloatRegister)

  ARM_INSTR_2(ldr_double,  fldd,   FloatRegister, Address)
  ARM_INSTR_2(str_double,  fstd,   FloatRegister, Address)
  ARM_INSTR_2(mov_double,  fcpyd,  FloatRegister, FloatRegister)
  ARM_INSTR_2(neg_double,  fnegd,  FloatRegister, FloatRegister)
  ARM_INSTR_2(cmp_double,  fcmpd,  FloatRegister, FloatRegister)
  ARM_INSTR_2(abs_double,  fabsd,  FloatRegister, FloatRegister)
  ARM_INSTR_2(sqrt_double, fsqrtd, FloatRegister, FloatRegister)

  ARM_INSTR_3(add_double,  faddd,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(sub_double,  fsubd,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(mul_double,  fmuld,  FloatRegister, FloatRegister, FloatRegister)
  ARM_INSTR_3(div_double,  fdivd,  FloatRegister, FloatRegister, FloatRegister)

  ARM_INSTR_2(convert_f2d, fcvtds, FloatRegister, FloatRegister)
  ARM_INSTR_2(convert_d2f, fcvtsd, FloatRegister, FloatRegister)

  ARM_INSTR_2(mov_fpr2gpr_float, fmrs, Register, FloatRegister)

#undef ARM_INSTR_1
#undef ARM_INSTR_2
#undef ARM_INSTR_3



  void tbz(Register rt, int bit, Label& L) {
    assert(0 <= bit && bit < BitsPerWord, "bit number is out of range");
    tst(rt, 1 << bit);
    b(L, eq);
  }

  void tbnz(Register rt, int bit, Label& L) {
    assert(0 <= bit && bit < BitsPerWord, "bit number is out of range");
    tst(rt, 1 << bit);
    b(L, ne);
  }

  void cbz(Register rt, Label& L) {
    cmp(rt, 0);
    b(L, eq);
  }

  void cbz(Register rt, address target) {
    cmp(rt, 0);
    b(target, eq);
  }

  void cbnz(Register rt, Label& L) {
    cmp(rt, 0);
    b(L, ne);
  }

  void ret(Register dst = LR) {
    bx(dst);
  }


  Register zero_register(Register tmp) {
    mov(tmp, 0);
    return tmp;
  }

  void logical_shift_left(Register dst, Register src, int shift) {
    mov(dst, AsmOperand(src, lsl, shift));
  }

  void logical_shift_left_32(Register dst, Register src, int shift) {
    mov(dst, AsmOperand(src, lsl, shift));
  }

  void logical_shift_right(Register dst, Register src, int shift) {
    mov(dst, AsmOperand(src, lsr, shift));
  }

  void arith_shift_right(Register dst, Register src, int shift) {
    mov(dst, AsmOperand(src, asr, shift));
  }

  void asr_32(Register dst, Register src, int shift) {
    mov(dst, AsmOperand(src, asr, shift));
  }

  // If <cond> holds, compares r1 and r2. Otherwise, flags are set so that <cond> does not hold.
  void cond_cmp(Register r1, Register r2, AsmCondition cond) {
    cmp(r1, r2, cond);
  }

  // If <cond> holds, compares r and imm. Otherwise, flags are set so that <cond> does not hold.
  void cond_cmp(Register r, int imm, AsmCondition cond) {
    cmp(r, imm, cond);
  }

  void align_reg(Register dst, Register src, int align) {
    assert (is_power_of_2(align), "should be");
    bic(dst, src, align-1);
  }

  void prefetch_read(Address addr) {
    pld(addr);
  }

  void raw_push(Register r1, Register r2) {
    assert(r1->encoding() < r2->encoding(), "should be ordered");
    push(RegisterSet(r1) | RegisterSet(r2));
  }

  void raw_pop(Register r1, Register r2) {
    assert(r1->encoding() < r2->encoding(), "should be ordered");
    pop(RegisterSet(r1) | RegisterSet(r2));
  }

  void raw_push(Register r1, Register r2, Register r3) {
    assert(r1->encoding() < r2->encoding() && r2->encoding() < r3->encoding(), "should be ordered");
    push(RegisterSet(r1) | RegisterSet(r2) | RegisterSet(r3));
  }

  void raw_pop(Register r1, Register r2, Register r3) {
    assert(r1->encoding() < r2->encoding() && r2->encoding() < r3->encoding(), "should be ordered");
    pop(RegisterSet(r1) | RegisterSet(r2) | RegisterSet(r3));
  }

  // Restores registers r1 and r2 previously saved by raw_push(r1, r2, ret_addr) and returns by ret_addr. Clobbers LR.
  void raw_pop_and_ret(Register r1, Register r2) {
    raw_pop(r1, r2, PC);
  }

  void indirect_jump(Address addr, Register scratch) {
    ldr(PC, addr);
  }

  void indirect_jump(InlinedAddress& literal, Register scratch) {
    ldr_literal(PC, literal);
  }

  void neg(Register dst, Register src) {
    rsb(dst, src, 0);
  }

  void branch_if_negative_32(Register r, Label& L) {
    // TODO: This function and branch_if_any_negative_32 could possibly
    // be revised after the aarch64 removal.
    // tbnz is not used instead of tst & b.mi because destination may be out of tbnz range (+-32KB)
    // since these methods are used in LIR_Assembler::emit_arraycopy() to jump to stub entry.
    tst_32(r, r);
    b(L, mi);
  }

  void branch_if_any_negative_32(Register r1, Register r2, Register tmp, Label& L) {
    orrs(tmp, r1, r2);
    b(L, mi);
  }

  void branch_if_any_negative_32(Register r1, Register r2, Register r3, Register tmp, Label& L) {
    orr_32(tmp, r1, r2);
    orrs(tmp, tmp, r3);
    b(L, mi);
  }

  void add_ptr_scaled_int32(Register dst, Register r1, Register r2, int shift) {
      add(dst, r1, AsmOperand(r2, lsl, shift));
  }

  void sub_ptr_scaled_int32(Register dst, Register r1, Register r2, int shift) {
    sub(dst, r1, AsmOperand(r2, lsl, shift));
  }

  // C 'boolean' to Java boolean: x == 0 ? 0 : 1
  void c2bool(Register x);

    // klass oop manipulations if compressed

  void load_klass(Register dst_klass, Register src_oop, AsmCondition cond = al);

  void store_klass(Register src_klass, Register dst_oop);


    // oop manipulations

  void load_heap_oop(Register dst, Address src, Register tmp1 = noreg, Register tmp2 = noreg, Register tmp3 = noreg, DecoratorSet decorators = 0);
  void store_heap_oop(Address obj, Register new_val, Register tmp1 = noreg, Register tmp2 = noreg, Register tmp3 = noreg, DecoratorSet decorators = 0);
  void store_heap_oop_null(Address obj, Register new_val, Register tmp1 = noreg, Register tmp2 = noreg, Register tmp3 = noreg, DecoratorSet decorators = 0);

  void access_load_at(BasicType type, DecoratorSet decorators, Address src, Register dst, Register tmp1, Register tmp2, Register tmp3);
  void access_store_at(BasicType type, DecoratorSet decorators, Address obj, Register new_val, Register tmp1, Register tmp2, Register tmp3, bool is_null);

  void ldr_global_ptr(Register reg, address address_of_global);
  void ldr_global_s32(Register reg, address address_of_global);
  void ldrb_global(Register reg, address address_of_global);

  // address_placeholder_instruction is invalid instruction and is used
  // as placeholder in code for address of label
  enum { address_placeholder_instruction = 0xFFFFFFFF };

  void emit_address(Label& L) {
    assert(!L.is_bound(), "otherwise address will not be patched");
    target(L);       // creates relocation which will be patched later

    assert ((offset() & (wordSize-1)) == 0, "should be aligned by word size");

    AbstractAssembler::emit_address((address)address_placeholder_instruction);
  }

  void b(address target, AsmCondition cond = al) {
    Assembler::b(target, cond);                 \
  }
  void b(Label& L, AsmCondition cond = al) {
    // internal jumps
    Assembler::b(target(L), cond);
  }

  void bl(address target, AsmCondition cond = al) {
    Assembler::bl(target, cond);
  }
  void bl(Label& L, AsmCondition cond = al) {
    // internal calls
    Assembler::bl(target(L), cond);
  }

  void adr(Register dest, Label& L, AsmCondition cond = al) {
    int delta = target(L) - pc() - 8;
    if (delta >= 0) {
      add(dest, PC, delta, cond);
    } else {
      sub(dest, PC, -delta, cond);
    }
  }

  // Variable-length jump and calls. We now distinguish only the
  // patchable case from the other cases. Patchable must be
  // distinguised from relocable. Relocable means the generated code
  // containing the jump/call may move. Patchable means that the
  // targeted address may be changed later.

  // Non patchable versions.
  // - used only for relocInfo::runtime_call_type and relocInfo::none
  // - may use relative or absolute format (do not use relocInfo::none
  //   if the generated code may move)
  // - the implementation takes into account switch to THUMB mode if the
  //   destination is a THUMB address
  // - the implementation supports far targets
  //
  // To reduce regression risk, scratch still defaults to noreg on
  // arm32. This results in patchable instructions. However, if
  // patching really matters, the call sites should be modified and
  // use patchable_call or patchable_jump. If patching is not required
  // and if a register can be cloberred, it should be explicitly
  // specified to allow future optimizations.
  void jump(address target,
            relocInfo::relocType rtype = relocInfo::runtime_call_type,
            Register scratch = noreg, AsmCondition cond = al);

  void call(address target,
            RelocationHolder rspec, AsmCondition cond = al);

  void call(address target,
            relocInfo::relocType rtype = relocInfo::runtime_call_type,
            AsmCondition cond = al) {
    call(target, Relocation::spec_simple(rtype), cond);
  }

  void jump(AddressLiteral dest) {
    jump(dest.target(), dest.reloc());
  }
  void jump(address dest, relocInfo::relocType rtype, AsmCondition cond) {
    jump(dest, rtype, Rtemp, cond);
  }

  void call(AddressLiteral dest) {
    call(dest.target(), dest.reloc());
  }

  // Patchable version:
  // - set_destination can be used to atomically change the target
  //
  // The targets for patchable_jump and patchable_call must be in the
  // code cache.
  // [ including possible extensions of the code cache, like AOT code ]
  //
  // To reduce regression risk, scratch still defaults to noreg on
  // arm32. If a register can be cloberred, it should be explicitly
  // specified to allow future optimizations.
  void patchable_jump(address target,
                      relocInfo::relocType rtype = relocInfo::runtime_call_type,
                      Register scratch = noreg, AsmCondition cond = al
                      );

  // patchable_call may scratch Rtemp
  int patchable_call(address target,
                     RelocationHolder const& rspec,
                     bool c2 = false);

  int patchable_call(address target,
                     relocInfo::relocType rtype,
                     bool c2 = false) {
    return patchable_call(target, Relocation::spec_simple(rtype), c2);
  }


  static bool _reachable_from_cache(address target);
  static bool _cache_fully_reachable();
  bool cache_fully_reachable();
  bool reachable_from_cache(address target);

  void zero_extend(Register rd, Register rn, int bits);
  void sign_extend(Register rd, Register rn, int bits);

  inline void zap_high_non_significant_bits(Register r) {
  }

  void cmpoop(Register obj1, Register obj2);

  void long_move(Register rd_lo, Register rd_hi,
                 Register rn_lo, Register rn_hi,
                 AsmCondition cond = al);
  void long_shift(Register rd_lo, Register rd_hi,
                  Register rn_lo, Register rn_hi,
                  AsmShift shift, Register count);
  void long_shift(Register rd_lo, Register rd_hi,
                  Register rn_lo, Register rn_hi,
                  AsmShift shift, int count);

  void atomic_cas(Register tmpreg1, Register tmpreg2, Register oldval, Register newval, Register base, int offset);
  void atomic_cas_bool(Register oldval, Register newval, Register base, int offset, Register tmpreg);
  void atomic_cas64(Register temp_lo, Register temp_hi, Register temp_result, Register oldval_lo, Register oldval_hi, Register newval_lo, Register newval_hi, Register base, int offset);

  void cas_for_lock_acquire(Register oldval, Register newval, Register base, Register tmp, Label &slow_case, bool allow_fallthrough_on_failure = false, bool one_shot = false);
  void cas_for_lock_release(Register oldval, Register newval, Register base, Register tmp, Label &slow_case, bool allow_fallthrough_on_failure = false, bool one_shot = false);

#ifndef PRODUCT
  // Preserves flags and all registers.
  // On SMP the updated value might not be visible to external observers without a sychronization barrier
  void cond_atomic_inc32(AsmCondition cond, int* counter_addr);
#endif // !PRODUCT

  // unconditional non-atomic increment
  void inc_counter(address counter_addr, Register tmpreg1, Register tmpreg2);
  void inc_counter(int* counter_addr, Register tmpreg1, Register tmpreg2) {
    inc_counter((address) counter_addr, tmpreg1, tmpreg2);
  }

  void pd_patch_instruction(address branch, address target, const char* file, int line);

  // Loading and storing values by size and signed-ness;
  // size must not exceed wordSize (i.e. 8-byte values are not supported on 32-bit ARM);
  // each of these calls generates exactly one load or store instruction,
  // so src can be pre- or post-indexed address.
  // 32-bit ARM variants also support conditional execution
  void load_sized_value(Register dst, Address src, size_t size_in_bytes, bool is_signed, AsmCondition cond = al);
  void store_sized_value(Register src, Address dst, size_t size_in_bytes, AsmCondition cond = al);

  void lookup_interface_method(Register recv_klass,
                               Register intf_klass,
                               RegisterOrConstant itable_index,
                               Register method_result,
                               Register temp_reg1,
                               Register temp_reg2,
                               Label& L_no_such_interface);


  void floating_cmp(Register dst);

  // improved x86 portability (minimizing source code changes)

  void ldr_literal(Register rd, AddressLiteral addr) {
    relocate(addr.rspec());
    ldr(rd, Address(PC, addr.target() - pc() - 8));
  }

  void lea(Register Rd, AddressLiteral addr) {
    // Never dereferenced, as on x86 (lval status ignored)
    mov_address(Rd, addr.target(), addr.rspec());
  }

  void restore_default_fp_mode();

  void safepoint_poll(Register tmp1, Label& slow_path);
  void get_polling_page(Register dest);
  void read_polling_page(Register dest, relocInfo::relocType rtype);
};


// The purpose of this class is to build several code fragments of the same size
// in order to allow fast table branch.

class FixedSizeCodeBlock {
public:
  FixedSizeCodeBlock(MacroAssembler* masm, int size_in_instrs, bool enabled);
  ~FixedSizeCodeBlock();

private:
  MacroAssembler* _masm;
  address _start;
  int _size_in_instrs;
  bool _enabled;
};


#endif // CPU_ARM_MACROASSEMBLER_ARM_HPP
