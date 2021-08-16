/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2020 SAP SE. All rights reserved.
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

#include "precompiled.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateTable.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/methodData.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/synchronizer.hpp"
#include "utilities/powerOfTwo.hpp"

#ifdef PRODUCT
#define __ _masm->
#define BLOCK_COMMENT(str)
#define BIND(label)        __ bind(label);
#else
#define __ (PRODUCT_ONLY(false&&)Verbose ? (_masm->block_comment(FILE_AND_LINE),_masm):_masm)->
#define BLOCK_COMMENT(str) __ block_comment(str)
#define BIND(label)        __ bind(label); BLOCK_COMMENT(#label ":")
#endif

// The assumed minimum size of a BranchTableBlock.
// The actual size of each block heavily depends on the CPU capabilities and,
// of course, on the logic implemented in each block.
#ifdef ASSERT
  #define BTB_MINSIZE 256
#else
  #define BTB_MINSIZE  64
#endif

#ifdef ASSERT
// Macro to open a BranchTableBlock (a piece of code that is branched to by a calculated branch).
#define BTB_BEGIN(lbl, alignment, name)                                        \
  __ align_address(alignment);                                                 \
  __ bind(lbl);                                                                \
  { unsigned int b_off = __ offset();                                          \
    uintptr_t   b_addr = (uintptr_t)__ pc();                                   \
    __ z_larl(Z_R0, (int64_t)0);     /* Check current address alignment. */    \
    __ z_slgr(Z_R0, br_tab);         /* Current Address must be equal    */    \
    __ z_slgr(Z_R0, flags);          /* to calculated branch target.     */    \
    __ z_brc(Assembler::bcondLogZero, 3); /* skip trap if ok. */               \
    __ z_illtrap(0x55);                                                        \
    guarantee(b_addr%alignment == 0, "bad alignment at begin of block" name);

// Macro to close a BranchTableBlock (a piece of code that is branched to by a calculated branch).
#define BTB_END(lbl, alignment, name)                                          \
    uintptr_t   e_addr = (uintptr_t)__ pc();                                   \
    unsigned int e_off = __ offset();                                          \
    unsigned int len   = e_off-b_off;                                          \
    if (len > alignment) {                                                     \
      tty->print_cr("%4d of %4d @ " INTPTR_FORMAT ": Block len for %s",        \
                    len, alignment, e_addr-len, name);                         \
      guarantee(len <= alignment, "block too large");                          \
    }                                                                          \
    guarantee(len == e_addr-b_addr, "block len mismatch");                     \
  }
#else
// Macro to open a BranchTableBlock (a piece of code that is branched to by a calculated branch).
#define BTB_BEGIN(lbl, alignment, name)                                        \
  __ align_address(alignment);                                                 \
  __ bind(lbl);                                                                \
  { unsigned int b_off = __ offset();                                          \
    uintptr_t   b_addr = (uintptr_t)__ pc();                                   \
    guarantee(b_addr%alignment == 0, "bad alignment at begin of block" name);

// Macro to close a BranchTableBlock (a piece of code that is branched to by a calculated branch).
#define BTB_END(lbl, alignment, name)                                          \
    uintptr_t   e_addr = (uintptr_t)__ pc();                                   \
    unsigned int e_off = __ offset();                                          \
    unsigned int len   = e_off-b_off;                                          \
    if (len > alignment) {                                                     \
      tty->print_cr("%4d of %4d @ " INTPTR_FORMAT ": Block len for %s",        \
                    len, alignment, e_addr-len, name);                         \
      guarantee(len <= alignment, "block too large");                          \
    }                                                                          \
    guarantee(len == e_addr-b_addr, "block len mismatch");                     \
  }
#endif // ASSERT

// Address computation: local variables

static inline Address iaddress(int n) {
  return Address(Z_locals, Interpreter::local_offset_in_bytes(n));
}

static inline Address laddress(int n) {
  return iaddress(n + 1);
}

static inline Address faddress(int n) {
  return iaddress(n);
}

static inline Address daddress(int n) {
  return laddress(n);
}

static inline Address aaddress(int n) {
  return iaddress(n);
}

// Pass NULL, if no shift instruction should be emitted.
static inline Address iaddress(InterpreterMacroAssembler *masm, Register r) {
  if (masm) {
    masm->z_sllg(r, r, LogBytesPerWord);  // index2bytes
  }
  return Address(Z_locals, r, Interpreter::local_offset_in_bytes(0));
}

// Pass NULL, if no shift instruction should be emitted.
static inline Address laddress(InterpreterMacroAssembler *masm, Register r) {
  if (masm) {
    masm->z_sllg(r, r, LogBytesPerWord);  // index2bytes
  }
  return Address(Z_locals, r, Interpreter::local_offset_in_bytes(1) );
}

static inline Address faddress(InterpreterMacroAssembler *masm, Register r) {
  return iaddress(masm, r);
}

static inline Address daddress(InterpreterMacroAssembler *masm, Register r) {
  return laddress(masm, r);
}

static inline Address aaddress(InterpreterMacroAssembler *masm, Register r) {
  return iaddress(masm, r);
}

// At top of Java expression stack which may be different than esp(). It
// isn't for category 1 objects.
static inline Address at_tos(int slot = 0) {
  return Address(Z_esp, Interpreter::expr_offset_in_bytes(slot));
}

// Condition conversion
static Assembler::branch_condition j_not(TemplateTable::Condition cc) {
  switch (cc) {
    case TemplateTable::equal :
      return Assembler::bcondNotEqual;
    case TemplateTable::not_equal :
      return Assembler::bcondEqual;
    case TemplateTable::less :
      return Assembler::bcondNotLow;
    case TemplateTable::less_equal :
      return Assembler::bcondHigh;
    case TemplateTable::greater :
      return Assembler::bcondNotHigh;
    case TemplateTable::greater_equal:
      return Assembler::bcondLow;
  }
  ShouldNotReachHere();
  return Assembler::bcondZero;
}

// Do an oop store like *(base + offset) = val
// offset can be a register or a constant.
static void do_oop_store(InterpreterMacroAssembler* _masm,
                         const Address&     addr,
                         Register           val,         // Noreg means always null.
                         Register           tmp1,
                         Register           tmp2,
                         Register           tmp3,
                         DecoratorSet       decorators) {
  assert_different_registers(tmp1, tmp2, tmp3, val, addr.base());
  __ store_heap_oop(val, addr, tmp1, tmp2, tmp3, decorators);
}

static void do_oop_load(InterpreterMacroAssembler* _masm,
                        const Address& addr,
                        Register dst,
                        Register tmp1,
                        Register tmp2,
                        DecoratorSet decorators) {
  assert_different_registers(addr.base(), tmp1, tmp2);
  assert_different_registers(dst, tmp1, tmp2);
  __ load_heap_oop(dst, addr, tmp1, tmp2, decorators);
}

Address TemplateTable::at_bcp(int offset) {
  assert(_desc->uses_bcp(), "inconsistent uses_bcp information");
  return Address(Z_bcp, offset);
}

void TemplateTable::patch_bytecode(Bytecodes::Code bc,
                                   Register        bc_reg,
                                   Register        temp_reg,
                                   bool            load_bc_into_bc_reg, // = true
                                   int             byte_no) {
  if (!RewriteBytecodes) { return; }

  NearLabel L_patch_done;
  BLOCK_COMMENT("patch_bytecode {");

  switch (bc) {
    case Bytecodes::_fast_aputfield:
    case Bytecodes::_fast_bputfield:
    case Bytecodes::_fast_zputfield:
    case Bytecodes::_fast_cputfield:
    case Bytecodes::_fast_dputfield:
    case Bytecodes::_fast_fputfield:
    case Bytecodes::_fast_iputfield:
    case Bytecodes::_fast_lputfield:
    case Bytecodes::_fast_sputfield:
      {
        // We skip bytecode quickening for putfield instructions when
        // the put_code written to the constant pool cache is zero.
        // This is required so that every execution of this instruction
        // calls out to InterpreterRuntime::resolve_get_put to do
        // additional, required work.
        assert(byte_no == f1_byte || byte_no == f2_byte, "byte_no out of range");
        assert(load_bc_into_bc_reg, "we use bc_reg as temp");
        __ get_cache_and_index_and_bytecode_at_bcp(Z_R1_scratch, bc_reg,
                                                   temp_reg, byte_no, 1);
        __ load_const_optimized(bc_reg, bc);
        __ compareU32_and_branch(temp_reg, (intptr_t)0,
                                 Assembler::bcondZero, L_patch_done);
      }
      break;
    default:
      assert(byte_no == -1, "sanity");
      // The pair bytecodes have already done the load.
      if (load_bc_into_bc_reg) {
        __ load_const_optimized(bc_reg, bc);
      }
      break;
  }

  if (JvmtiExport::can_post_breakpoint()) {

    Label   L_fast_patch;

    // If a breakpoint is present we can't rewrite the stream directly.
    __ z_cli(at_bcp(0), Bytecodes::_breakpoint);
    __ z_brne(L_fast_patch);
    __ get_method(temp_reg);
    // Let breakpoint table handling rewrite to quicker bytecode.
    __ call_VM_static(noreg,
                      CAST_FROM_FN_PTR(address, InterpreterRuntime::set_original_bytecode_at),
                      temp_reg, Z_R13, bc_reg);
    __ z_bru(L_patch_done);

    __ bind(L_fast_patch);
  }

#ifdef ASSERT
  NearLabel   L_okay;

  // We load into 64 bits, since this works on any CPU.
  __ z_llgc(temp_reg, at_bcp(0));
  __ compareU32_and_branch(temp_reg, Bytecodes::java_code(bc),
                            Assembler::bcondEqual, L_okay        );
  __ compareU32_and_branch(temp_reg, bc_reg, Assembler::bcondEqual, L_okay);
  __ stop_static("patching the wrong bytecode");
  __ bind(L_okay);
#endif

  // Patch bytecode.
  __ z_stc(bc_reg, at_bcp(0));

  __ bind(L_patch_done);
  BLOCK_COMMENT("} patch_bytecode");
}

// Individual instructions

void TemplateTable::nop() {
  transition(vtos, vtos);
}

void TemplateTable::shouldnotreachhere() {
  transition(vtos, vtos);
  __ stop("shouldnotreachhere bytecode");
}

void TemplateTable::aconst_null() {
  transition(vtos, atos);
  __ clear_reg(Z_tos, true, false);
}

void TemplateTable::iconst(int value) {
  transition(vtos, itos);
  // Zero extension of the iconst makes zero extension at runtime obsolete.
  __ load_const_optimized(Z_tos, ((unsigned long)(unsigned int)value));
}

void TemplateTable::lconst(int value) {
  transition(vtos, ltos);
  __ load_const_optimized(Z_tos, value);
}

// No pc-relative load/store for floats.
void TemplateTable::fconst(int value) {
  transition(vtos, ftos);
  static float   one = 1.0f, two = 2.0f;

  switch (value) {
    case 0:
      __ z_lzer(Z_ftos);
      return;
    case 1:
      __ load_absolute_address(Z_R1_scratch, (address) &one);
      __ mem2freg_opt(Z_ftos, Address(Z_R1_scratch), false);
      return;
    case 2:
      __ load_absolute_address(Z_R1_scratch, (address) &two);
      __ mem2freg_opt(Z_ftos, Address(Z_R1_scratch), false);
      return;
    default:
      ShouldNotReachHere();
      return;
  }
}

void TemplateTable::dconst(int value) {
  transition(vtos, dtos);
  static double one = 1.0;

  switch (value) {
    case 0:
      __ z_lzdr(Z_ftos);
      return;
    case 1:
      __ load_absolute_address(Z_R1_scratch, (address) &one);
      __ mem2freg_opt(Z_ftos, Address(Z_R1_scratch));
      return;
    default:
      ShouldNotReachHere();
      return;
  }
}

void TemplateTable::bipush() {
  transition(vtos, itos);
  __ z_lb(Z_tos, at_bcp(1));
}

void TemplateTable::sipush() {
  transition(vtos, itos);
  __ get_2_byte_integer_at_bcp(Z_tos, 1, InterpreterMacroAssembler::Signed);
}


void TemplateTable::ldc(bool wide) {
  transition(vtos, vtos);
  Label call_ldc, notFloat, notClass, notInt, Done;
  const Register RcpIndex = Z_tmp_1;
  const Register Rtags = Z_ARG2;

  if (wide) {
    __ get_2_byte_integer_at_bcp(RcpIndex, 1, InterpreterMacroAssembler::Unsigned);
  } else {
    __ z_llgc(RcpIndex, at_bcp(1));
  }

  __ get_cpool_and_tags(Z_tmp_2, Rtags);

  const int      base_offset = ConstantPool::header_size() * wordSize;
  const int      tags_offset = Array<u1>::base_offset_in_bytes();
  const Register Raddr_type = Rtags;

  // Get address of type.
  __ add2reg_with_index(Raddr_type, tags_offset, RcpIndex, Rtags);

  __ z_cli(0, Raddr_type, JVM_CONSTANT_UnresolvedClass);
  __ z_bre(call_ldc);    // Unresolved class - get the resolved class.

  __ z_cli(0, Raddr_type, JVM_CONSTANT_UnresolvedClassInError);
  __ z_bre(call_ldc);    // Unresolved class in error state - call into runtime
                         // to throw the error from the first resolution attempt.

  __ z_cli(0, Raddr_type, JVM_CONSTANT_Class);
  __ z_brne(notClass);   // Resolved class - need to call vm to get java
                         // mirror of the class.

  // We deal with a class. Call vm to do the appropriate.
  __ bind(call_ldc);
  __ load_const_optimized(Z_ARG2, wide);
  call_VM(Z_RET, CAST_FROM_FN_PTR(address, InterpreterRuntime::ldc), Z_ARG2);
  __ push_ptr(Z_RET);
  __ z_bru(Done);

  // Not a class.
  __ bind(notClass);
  Register RcpOffset = RcpIndex;
  __ z_sllg(RcpOffset, RcpIndex, LogBytesPerWord); // Convert index to offset.
  __ z_cli(0, Raddr_type, JVM_CONSTANT_Float);
  __ z_brne(notFloat);

  // ftos
  __ mem2freg_opt(Z_ftos, Address(Z_tmp_2, RcpOffset, base_offset), false);
  __ push_f();
  __ z_bru(Done);

  __ bind(notFloat);
  __ z_cli(0, Raddr_type, JVM_CONSTANT_Integer);
  __ z_brne(notInt);

  // itos
  __ mem2reg_opt(Z_tos, Address(Z_tmp_2, RcpOffset, base_offset), false);
  __ push_i(Z_tos);
  __ z_bru(Done);

  // assume the tag is for condy; if not, the VM runtime will tell us
  __ bind(notInt);
  condy_helper(Done);

  __ bind(Done);
}

// Fast path for caching oop constants.
// %%% We should use this to handle Class and String constants also.
// %%% It will simplify the ldc/primitive path considerably.
void TemplateTable::fast_aldc(bool wide) {
  transition(vtos, atos);

  const Register index = Z_tmp_2;
  int            index_size = wide ? sizeof(u2) : sizeof(u1);
  Label          L_do_resolve, L_resolved;

  // We are resolved if the resolved reference cache entry contains a
  // non-null object (CallSite, etc.).
  __ get_cache_index_at_bcp(index, 1, index_size);  // Load index.
  __ load_resolved_reference_at_index(Z_tos, index);
  __ z_ltgr(Z_tos, Z_tos);
  __ z_bre(L_do_resolve);

  // Convert null sentinel to NULL.
  __ load_const_optimized(Z_R1_scratch, (intptr_t)Universe::the_null_sentinel_addr());
  __ resolve_oop_handle(Z_R1_scratch);
  __ z_cg(Z_tos, Address(Z_R1_scratch));
  __ z_brne(L_resolved);
  __ clear_reg(Z_tos);
  __ z_bru(L_resolved);

  __ bind(L_do_resolve);
  // First time invocation - must resolve first.
  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc);
  __ load_const_optimized(Z_ARG1, (int)bytecode());
  __ call_VM(Z_tos, entry, Z_ARG1);

  __ bind(L_resolved);
  __ verify_oop(Z_tos);
}

void TemplateTable::ldc2_w() {
  transition(vtos, vtos);
  Label notDouble, notLong, Done;

  // Z_tmp_1 = index of cp entry
  __ get_2_byte_integer_at_bcp(Z_tmp_1, 1, InterpreterMacroAssembler::Unsigned);

  __ get_cpool_and_tags(Z_tmp_2, Z_tos);

  const int base_offset = ConstantPool::header_size() * wordSize;
  const int tags_offset = Array<u1>::base_offset_in_bytes();

  // Get address of type.
  __ add2reg_with_index(Z_tos, tags_offset, Z_tos, Z_tmp_1);

  // Index needed in both branches, so calculate here.
  __ z_sllg(Z_tmp_1, Z_tmp_1, LogBytesPerWord);  // index2bytes

  // Check type.
  __ z_cli(0, Z_tos, JVM_CONSTANT_Double);
  __ z_brne(notDouble);
  // dtos
  __ mem2freg_opt(Z_ftos, Address(Z_tmp_2, Z_tmp_1, base_offset));
  __ push_d();
  __ z_bru(Done);

  __ bind(notDouble);
  __ z_cli(0, Z_tos, JVM_CONSTANT_Long);
  __ z_brne(notLong);
  // ltos
  __ mem2reg_opt(Z_tos, Address(Z_tmp_2, Z_tmp_1, base_offset));
  __ push_l();
  __ z_bru(Done);

  __ bind(notLong);
  condy_helper(Done);

  __ bind(Done);
}

void TemplateTable::condy_helper(Label& Done) {
  const Register obj   = Z_tmp_1;
  const Register off   = Z_tmp_2;
  const Register flags = Z_ARG1;
  const Register rarg  = Z_ARG2;
  __ load_const_optimized(rarg, (int)bytecode());
  call_VM(obj, CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_ldc), rarg);
  __ get_vm_result_2(flags);

  // VMr = obj = base address to find primitive value to push
  // VMr2 = flags = (tos, off) using format of CPCE::_flags
  assert(ConstantPoolCacheEntry::field_index_mask == 0xffff, "or use other instructions");
  __ z_llghr(off, flags);
  const Address field(obj, off);

  // What sort of thing are we loading?
  __ z_srl(flags, ConstantPoolCacheEntry::tos_state_shift);
  // Make sure we don't need to mask flags for tos_state after the above shift.
  ConstantPoolCacheEntry::verify_tos_state_shift();

  switch (bytecode()) {
  case Bytecodes::_ldc:
  case Bytecodes::_ldc_w:
    {
      // tos in (itos, ftos, stos, btos, ctos, ztos)
      Label notInt, notFloat, notShort, notByte, notChar, notBool;
      __ z_cghi(flags, itos);
      __ z_brne(notInt);
      // itos
      __ z_l(Z_tos, field);
      __ push(itos);
      __ z_bru(Done);

      __ bind(notInt);
      __ z_cghi(flags, ftos);
      __ z_brne(notFloat);
      // ftos
      __ z_le(Z_ftos, field);
      __ push(ftos);
      __ z_bru(Done);

      __ bind(notFloat);
      __ z_cghi(flags, stos);
      __ z_brne(notShort);
      // stos
      __ z_lh(Z_tos, field);
      __ push(stos);
      __ z_bru(Done);

      __ bind(notShort);
      __ z_cghi(flags, btos);
      __ z_brne(notByte);
      // btos
      __ z_lb(Z_tos, field);
      __ push(btos);
      __ z_bru(Done);

      __ bind(notByte);
      __ z_cghi(flags, ctos);
      __ z_brne(notChar);
      // ctos
      __ z_llh(Z_tos, field);
      __ push(ctos);
      __ z_bru(Done);

      __ bind(notChar);
      __ z_cghi(flags, ztos);
      __ z_brne(notBool);
      // ztos
      __ z_lb(Z_tos, field);
      __ push(ztos);
      __ z_bru(Done);

      __ bind(notBool);
      break;
    }

  case Bytecodes::_ldc2_w:
    {
      Label notLong, notDouble;
      __ z_cghi(flags, ltos);
      __ z_brne(notLong);
      // ltos
      __ z_lg(Z_tos, field);
      __ push(ltos);
      __ z_bru(Done);

      __ bind(notLong);
      __ z_cghi(flags, dtos);
      __ z_brne(notDouble);
      // dtos
      __ z_ld(Z_ftos, field);
      __ push(dtos);
      __ z_bru(Done);

      __ bind(notDouble);
      break;
    }

  default:
    ShouldNotReachHere();
  }

  __ stop("bad ldc/condy");
}

void TemplateTable::locals_index(Register reg, int offset) {
  __ z_llgc(reg, at_bcp(offset));
  __ z_lcgr(reg);
}

void TemplateTable::iload() {
  iload_internal();
}

void TemplateTable::nofast_iload() {
  iload_internal(may_not_rewrite);
}

void TemplateTable::iload_internal(RewriteControl rc) {
  transition(vtos, itos);

  if (RewriteFrequentPairs && rc == may_rewrite) {
    NearLabel rewrite, done;
    const Register bc = Z_ARG4;

    assert(Z_R1_scratch != bc, "register damaged");

    // Get next byte.
    __ z_llgc(Z_R1_scratch, at_bcp(Bytecodes::length_for (Bytecodes::_iload)));

    // If _iload, wait to rewrite to iload2. We only want to rewrite the
    // last two iloads in a pair. Comparing against fast_iload means that
    // the next bytecode is neither an iload or a caload, and therefore
    // an iload pair.
    __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_iload,
                             Assembler::bcondEqual, done);

    __ load_const_optimized(bc, Bytecodes::_fast_iload2);
    __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_fast_iload,
                             Assembler::bcondEqual, rewrite);

    // If _caload, rewrite to fast_icaload.
    __ load_const_optimized(bc, Bytecodes::_fast_icaload);
    __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_caload,
                             Assembler::bcondEqual, rewrite);

    // Rewrite so iload doesn't check again.
    __ load_const_optimized(bc, Bytecodes::_fast_iload);

    // rewrite
    // bc: fast bytecode
    __ bind(rewrite);
    patch_bytecode(Bytecodes::_iload, bc, Z_R1_scratch, false);

    __ bind(done);

  }

  // Get the local value into tos.
  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_tos, iaddress(_masm, Z_R1_scratch), false);
}

void TemplateTable::fast_iload2() {
  transition(vtos, itos);

  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_tos, iaddress(_masm, Z_R1_scratch), false);
  __ push_i(Z_tos);
  locals_index(Z_R1_scratch, 3);
  __ mem2reg_opt(Z_tos, iaddress(_masm, Z_R1_scratch), false);
}

void TemplateTable::fast_iload() {
  transition(vtos, itos);

  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_tos, iaddress(_masm, Z_R1_scratch), false);
}

void TemplateTable::lload() {
  transition(vtos, ltos);

  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_tos, laddress(_masm, Z_R1_scratch));
}

void TemplateTable::fload() {
  transition(vtos, ftos);

  locals_index(Z_R1_scratch);
  __ mem2freg_opt(Z_ftos, faddress(_masm, Z_R1_scratch), false);
}

void TemplateTable::dload() {
  transition(vtos, dtos);

  locals_index(Z_R1_scratch);
  __ mem2freg_opt(Z_ftos, daddress(_masm, Z_R1_scratch));
}

void TemplateTable::aload() {
  transition(vtos, atos);

  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_tos, aaddress(_masm, Z_R1_scratch));
}

void TemplateTable::locals_index_wide(Register reg) {
  __ get_2_byte_integer_at_bcp(reg, 2, InterpreterMacroAssembler::Unsigned);
  __ z_lcgr(reg);
}

void TemplateTable::wide_iload() {
  transition(vtos, itos);

  locals_index_wide(Z_tmp_1);
  __ mem2reg_opt(Z_tos, iaddress(_masm, Z_tmp_1), false);
}

void TemplateTable::wide_lload() {
  transition(vtos, ltos);

  locals_index_wide(Z_tmp_1);
  __ mem2reg_opt(Z_tos, laddress(_masm, Z_tmp_1));
}

void TemplateTable::wide_fload() {
  transition(vtos, ftos);

  locals_index_wide(Z_tmp_1);
  __ mem2freg_opt(Z_ftos, faddress(_masm, Z_tmp_1), false);
}

void TemplateTable::wide_dload() {
  transition(vtos, dtos);

  locals_index_wide(Z_tmp_1);
  __ mem2freg_opt(Z_ftos, daddress(_masm, Z_tmp_1));
}

void TemplateTable::wide_aload() {
  transition(vtos, atos);

  locals_index_wide(Z_tmp_1);
  __ mem2reg_opt(Z_tos, aaddress(_masm, Z_tmp_1));
}

void TemplateTable::index_check(Register array, Register index, unsigned int shift) {
  assert_different_registers(Z_R1_scratch, array, index);

  // Check array.
  __ null_check(array, Z_R0_scratch, arrayOopDesc::length_offset_in_bytes());

  // Sign extend index for use by indexed load.
  __ z_lgfr(index, index);

  // Check index.
  Label index_ok;
  __ z_cl(index, Address(array, arrayOopDesc::length_offset_in_bytes()));
  __ z_brl(index_ok);
  __ lgr_if_needed(Z_ARG3, index); // See generate_ArrayIndexOutOfBounds_handler().
  // Pass the array to create more detailed exceptions.
  __ lgr_if_needed(Z_ARG2, array); // See generate_ArrayIndexOutOfBounds_handler().
  __ load_absolute_address(Z_R1_scratch,
                           Interpreter::_throw_ArrayIndexOutOfBoundsException_entry);
  __ z_bcr(Assembler::bcondAlways, Z_R1_scratch);
  __ bind(index_ok);

  if (shift > 0)
    __ z_sllg(index, index, shift);
}

void TemplateTable::iaload() {
  transition(itos, itos);

  __ pop_ptr(Z_tmp_1);  // array
  // Index is in Z_tos.
  Register index = Z_tos;
  index_check(Z_tmp_1, index, LogBytesPerInt); // Kills Z_ARG3.
  // Load the value.
  __ mem2reg_opt(Z_tos,
                 Address(Z_tmp_1, index, arrayOopDesc::base_offset_in_bytes(T_INT)),
                 false);
}

void TemplateTable::laload() {
  transition(itos, ltos);

  __ pop_ptr(Z_tmp_2);
  // Z_tos   : index
  // Z_tmp_2 : array
  Register index = Z_tos;
  index_check(Z_tmp_2, index, LogBytesPerLong);
  __ mem2reg_opt(Z_tos,
                 Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_LONG)));
}

void TemplateTable::faload() {
  transition(itos, ftos);

  __ pop_ptr(Z_tmp_2);
  // Z_tos   : index
  // Z_tmp_2 : array
  Register index = Z_tos;
  index_check(Z_tmp_2, index, LogBytesPerInt);
  __ mem2freg_opt(Z_ftos,
                  Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_FLOAT)),
                  false);
}

void TemplateTable::daload() {
  transition(itos, dtos);

  __ pop_ptr(Z_tmp_2);
  // Z_tos   : index
  // Z_tmp_2 : array
  Register index = Z_tos;
  index_check(Z_tmp_2, index, LogBytesPerLong);
  __ mem2freg_opt(Z_ftos,
                  Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_DOUBLE)));
}

void TemplateTable::aaload() {
  transition(itos, atos);

  unsigned const int shift = LogBytesPerHeapOop;
  __ pop_ptr(Z_tmp_1);  // array
  // Index is in Z_tos.
  Register index = Z_tos;
  index_check(Z_tmp_1, index, shift);
  // Now load array element.
  do_oop_load(_masm, Address(Z_tmp_1, index, arrayOopDesc::base_offset_in_bytes(T_OBJECT)), Z_tos,
              Z_tmp_2, Z_tmp_3, IS_ARRAY);
  __ verify_oop(Z_tos);
}

void TemplateTable::baload() {
  transition(itos, itos);

  __ pop_ptr(Z_tmp_1);
  // Z_tos   : index
  // Z_tmp_1 : array
  Register index = Z_tos;
  index_check(Z_tmp_1, index, 0);
  __ z_lb(Z_tos,
          Address(Z_tmp_1, index, arrayOopDesc::base_offset_in_bytes(T_BYTE)));
}

void TemplateTable::caload() {
  transition(itos, itos);

  __ pop_ptr(Z_tmp_2);
  // Z_tos   : index
  // Z_tmp_2 : array
  Register index = Z_tos;
  index_check(Z_tmp_2, index, LogBytesPerShort);
  // Load into 64 bits, works on all CPUs.
  __ z_llgh(Z_tos,
            Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_CHAR)));
}

// Iload followed by caload frequent pair.
void TemplateTable::fast_icaload() {
  transition(vtos, itos);

  // Load index out of locals.
  locals_index(Z_R1_scratch);
  __ mem2reg_opt(Z_ARG3, iaddress(_masm, Z_R1_scratch), false);
  // Z_ARG3  : index
  // Z_tmp_2 : array
  __ pop_ptr(Z_tmp_2);
  index_check(Z_tmp_2, Z_ARG3, LogBytesPerShort);
  // Load into 64 bits, works on all CPUs.
  __ z_llgh(Z_tos,
            Address(Z_tmp_2, Z_ARG3, arrayOopDesc::base_offset_in_bytes(T_CHAR)));
}

void TemplateTable::saload() {
  transition(itos, itos);

  __ pop_ptr(Z_tmp_2);
  // Z_tos   : index
  // Z_tmp_2 : array
  Register index = Z_tos;
  index_check(Z_tmp_2, index, LogBytesPerShort);
  __ z_lh(Z_tos,
          Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_SHORT)));
}

void TemplateTable::iload(int n) {
  transition(vtos, itos);
  __ z_ly(Z_tos, iaddress(n));
}

void TemplateTable::lload(int n) {
  transition(vtos, ltos);
  __ z_lg(Z_tos, laddress(n));
}

void TemplateTable::fload(int n) {
  transition(vtos, ftos);
  __ mem2freg_opt(Z_ftos, faddress(n), false);
}

void TemplateTable::dload(int n) {
  transition(vtos, dtos);
  __ mem2freg_opt(Z_ftos, daddress(n));
}

void TemplateTable::aload(int n) {
  transition(vtos, atos);
  __ mem2reg_opt(Z_tos, aaddress(n));
}

void TemplateTable::aload_0() {
  aload_0_internal();
}

void TemplateTable::nofast_aload_0() {
  aload_0_internal(may_not_rewrite);
}

void TemplateTable::aload_0_internal(RewriteControl rc) {
  transition(vtos, atos);

  // According to bytecode histograms, the pairs:
  //
  // _aload_0, _fast_igetfield
  // _aload_0, _fast_agetfield
  // _aload_0, _fast_fgetfield
  //
  // occur frequently. If RewriteFrequentPairs is set, the (slow)
  // _aload_0 bytecode checks if the next bytecode is either
  // _fast_igetfield, _fast_agetfield or _fast_fgetfield and then
  // rewrites the current bytecode into a pair bytecode; otherwise it
  // rewrites the current bytecode into _fast_aload_0 that doesn't do
  // the pair check anymore.
  //
  // Note: If the next bytecode is _getfield, the rewrite must be
  //       delayed, otherwise we may miss an opportunity for a pair.
  //
  // Also rewrite frequent pairs
  //   aload_0, aload_1
  //   aload_0, iload_1
  // These bytecodes with a small amount of code are most profitable
  // to rewrite.
  if (!(RewriteFrequentPairs && (rc == may_rewrite))) {
    aload(0);
    return;
  }

  NearLabel rewrite, done;
  const Register bc = Z_ARG4;

  assert(Z_R1_scratch != bc, "register damaged");
  // Get next byte.
  __ z_llgc(Z_R1_scratch, at_bcp(Bytecodes::length_for (Bytecodes::_aload_0)));

  // Do actual aload_0.
  aload(0);

  // If _getfield then wait with rewrite.
  __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_getfield,
                           Assembler::bcondEqual, done);

  // If _igetfield then rewrite to _fast_iaccess_0.
  assert(Bytecodes::java_code(Bytecodes::_fast_iaccess_0)
            == Bytecodes::_aload_0, "fix bytecode definition");

  __ load_const_optimized(bc, Bytecodes::_fast_iaccess_0);
  __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_fast_igetfield,
                           Assembler::bcondEqual, rewrite);

  // If _agetfield then rewrite to _fast_aaccess_0.
  assert(Bytecodes::java_code(Bytecodes::_fast_aaccess_0)
            == Bytecodes::_aload_0, "fix bytecode definition");

  __ load_const_optimized(bc, Bytecodes::_fast_aaccess_0);
  __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_fast_agetfield,
                           Assembler::bcondEqual, rewrite);

  // If _fgetfield then rewrite to _fast_faccess_0.
  assert(Bytecodes::java_code(Bytecodes::_fast_faccess_0)
            == Bytecodes::_aload_0, "fix bytecode definition");

  __ load_const_optimized(bc, Bytecodes::_fast_faccess_0);
  __ compareU32_and_branch(Z_R1_scratch, Bytecodes::_fast_fgetfield,
                           Assembler::bcondEqual, rewrite);

  // Else rewrite to _fast_aload0.
  assert(Bytecodes::java_code(Bytecodes::_fast_aload_0)
            == Bytecodes::_aload_0, "fix bytecode definition");
  __ load_const_optimized(bc, Bytecodes::_fast_aload_0);

  // rewrite
  // bc: fast bytecode
  __ bind(rewrite);

  patch_bytecode(Bytecodes::_aload_0, bc, Z_R1_scratch, false);
  // Reload local 0 because of VM call inside patch_bytecode().
  // this may trigger GC and thus change the oop.
  aload(0);

  __ bind(done);
}

void TemplateTable::istore() {
  transition(itos, vtos);
  locals_index(Z_R1_scratch);
  __ reg2mem_opt(Z_tos, iaddress(_masm, Z_R1_scratch), false);
}

void TemplateTable::lstore() {
  transition(ltos, vtos);
  locals_index(Z_R1_scratch);
  __ reg2mem_opt(Z_tos, laddress(_masm, Z_R1_scratch));
}

void TemplateTable::fstore() {
  transition(ftos, vtos);
  locals_index(Z_R1_scratch);
  __ freg2mem_opt(Z_ftos, faddress(_masm, Z_R1_scratch));
}

void TemplateTable::dstore() {
  transition(dtos, vtos);
  locals_index(Z_R1_scratch);
  __ freg2mem_opt(Z_ftos, daddress(_masm, Z_R1_scratch));
}

void TemplateTable::astore() {
  transition(vtos, vtos);
  __ pop_ptr(Z_tos);
  locals_index(Z_R1_scratch);
  __ reg2mem_opt(Z_tos, aaddress(_masm, Z_R1_scratch));
}

void TemplateTable::wide_istore() {
  transition(vtos, vtos);
  __ pop_i(Z_tos);
  locals_index_wide(Z_tmp_1);
  __ reg2mem_opt(Z_tos, iaddress(_masm, Z_tmp_1), false);
}

void TemplateTable::wide_lstore() {
  transition(vtos, vtos);
  __ pop_l(Z_tos);
  locals_index_wide(Z_tmp_1);
  __ reg2mem_opt(Z_tos, laddress(_masm, Z_tmp_1));
}

void TemplateTable::wide_fstore() {
  transition(vtos, vtos);
  __ pop_f(Z_ftos);
  locals_index_wide(Z_tmp_1);
  __ freg2mem_opt(Z_ftos, faddress(_masm, Z_tmp_1), false);
}

void TemplateTable::wide_dstore() {
  transition(vtos, vtos);
  __ pop_d(Z_ftos);
  locals_index_wide(Z_tmp_1);
  __ freg2mem_opt(Z_ftos, daddress(_masm, Z_tmp_1));
}

void TemplateTable::wide_astore() {
  transition(vtos, vtos);
  __ pop_ptr(Z_tos);
  locals_index_wide(Z_tmp_1);
  __ reg2mem_opt(Z_tos, aaddress(_masm, Z_tmp_1));
}

void TemplateTable::iastore() {
  transition(itos, vtos);

  Register index = Z_ARG3; // Index_check expects index in Z_ARG3.
  // Value is in Z_tos ...
  __ pop_i(index);        // index
  __ pop_ptr(Z_tmp_1);    // array
  index_check(Z_tmp_1, index, LogBytesPerInt);
  // ... and then move the value.
  __ reg2mem_opt(Z_tos,
                 Address(Z_tmp_1, index, arrayOopDesc::base_offset_in_bytes(T_INT)),
                 false);
}

void TemplateTable::lastore() {
  transition(ltos, vtos);

  __ pop_i(Z_ARG3);
  __ pop_ptr(Z_tmp_2);
  // Z_tos   : value
  // Z_ARG3  : index
  // Z_tmp_2 : array
 index_check(Z_tmp_2, Z_ARG3, LogBytesPerLong); // Prefer index in Z_ARG3.
  __ reg2mem_opt(Z_tos,
                 Address(Z_tmp_2, Z_ARG3, arrayOopDesc::base_offset_in_bytes(T_LONG)));
}

void TemplateTable::fastore() {
  transition(ftos, vtos);

  __ pop_i(Z_ARG3);
  __ pop_ptr(Z_tmp_2);
  // Z_ftos  : value
  // Z_ARG3  : index
  // Z_tmp_2 : array
  index_check(Z_tmp_2, Z_ARG3, LogBytesPerInt); // Prefer index in Z_ARG3.
  __ freg2mem_opt(Z_ftos,
                  Address(Z_tmp_2, Z_ARG3, arrayOopDesc::base_offset_in_bytes(T_FLOAT)),
                  false);
}

void TemplateTable::dastore() {
  transition(dtos, vtos);

  __ pop_i(Z_ARG3);
  __ pop_ptr(Z_tmp_2);
  // Z_ftos  : value
  // Z_ARG3  : index
  // Z_tmp_2 : array
  index_check(Z_tmp_2, Z_ARG3, LogBytesPerLong); // Prefer index in Z_ARG3.
  __ freg2mem_opt(Z_ftos,
                  Address(Z_tmp_2, Z_ARG3, arrayOopDesc::base_offset_in_bytes(T_DOUBLE)));
}

void TemplateTable::aastore() {
  NearLabel is_null, ok_is_subtype, done;
  transition(vtos, vtos);

  // stack: ..., array, index, value

  Register Rvalue = Z_tos;
  Register Rarray = Z_ARG2;
  Register Rindex = Z_ARG3; // Convention for index_check().

  __ load_ptr(0, Rvalue);
  __ z_l(Rindex, Address(Z_esp, Interpreter::expr_offset_in_bytes(1)));
  __ load_ptr(2, Rarray);

  unsigned const int shift = LogBytesPerHeapOop;
  index_check(Rarray, Rindex, shift); // side effect: Rindex = Rindex << shift
  Register Rstore_addr  = Rindex;
  // Address where the store goes to, i.e. &(Rarry[index])
  __ load_address(Rstore_addr, Address(Rarray, Rindex, arrayOopDesc::base_offset_in_bytes(T_OBJECT)));

  // do array store check - check for NULL value first.
  __ compareU64_and_branch(Rvalue, (intptr_t)0, Assembler::bcondEqual, is_null);

  Register Rsub_klass   = Z_ARG4;
  Register Rsuper_klass = Z_ARG5;
  __ load_klass(Rsub_klass, Rvalue);
  // Load superklass.
  __ load_klass(Rsuper_klass, Rarray);
  __ z_lg(Rsuper_klass, Address(Rsuper_klass, ObjArrayKlass::element_klass_offset()));

  // Generate a fast subtype check.  Branch to ok_is_subtype if no failure.
  // Throw if failure.
  Register tmp1 = Z_tmp_1;
  Register tmp2 = Z_tmp_2;
  __ gen_subtype_check(Rsub_klass, Rsuper_klass, tmp1, tmp2, ok_is_subtype);

  // Fall through on failure.
  // Object is in Rvalue == Z_tos.
  assert(Rvalue == Z_tos, "that's the expected location");
  __ load_absolute_address(tmp1, Interpreter::_throw_ArrayStoreException_entry);
  __ z_br(tmp1);

  Register tmp3 = Rsub_klass;

  // Have a NULL in Rvalue.
  __ bind(is_null);
  __ profile_null_seen(tmp1);

  // Store a NULL.
  do_oop_store(_masm, Address(Rstore_addr, (intptr_t)0), noreg,
               tmp3, tmp2, tmp1, IS_ARRAY);
  __ z_bru(done);

  // Come here on success.
  __ bind(ok_is_subtype);

  // Now store using the appropriate barrier.
  do_oop_store(_masm, Address(Rstore_addr, (intptr_t)0), Rvalue,
               tmp3, tmp2, tmp1, IS_ARRAY | IS_NOT_NULL);

  // Pop stack arguments.
  __ bind(done);
  __ add2reg(Z_esp, 3 * Interpreter::stackElementSize);
}


void TemplateTable::bastore() {
  transition(itos, vtos);

  __ pop_i(Z_ARG3);
  __ pop_ptr(Z_tmp_2);
  // Z_tos   : value
  // Z_ARG3  : index
  // Z_tmp_2 : array

  // Need to check whether array is boolean or byte
  // since both types share the bastore bytecode.
  __ load_klass(Z_tmp_1, Z_tmp_2);
  __ z_llgf(Z_tmp_1, Address(Z_tmp_1, Klass::layout_helper_offset()));
  __ z_tmll(Z_tmp_1, Klass::layout_helper_boolean_diffbit());
  Label L_skip;
  __ z_bfalse(L_skip);
  // if it is a T_BOOLEAN array, mask the stored value to 0/1
  __ z_nilf(Z_tos, 0x1);
  __ bind(L_skip);

  // No index shift necessary - pass 0.
  index_check(Z_tmp_2, Z_ARG3, 0); // Prefer index in Z_ARG3.
  __ z_stc(Z_tos,
           Address(Z_tmp_2, Z_ARG3, arrayOopDesc::base_offset_in_bytes(T_BYTE)));
}

void TemplateTable::castore() {
  transition(itos, vtos);

  __ pop_i(Z_ARG3);
  __ pop_ptr(Z_tmp_2);
  // Z_tos   : value
  // Z_ARG3  : index
  // Z_tmp_2 : array
  Register index = Z_ARG3; // prefer index in Z_ARG3
  index_check(Z_tmp_2, index, LogBytesPerShort);
  __ z_sth(Z_tos,
           Address(Z_tmp_2, index, arrayOopDesc::base_offset_in_bytes(T_CHAR)));
}

void TemplateTable::sastore() {
  castore();
}

void TemplateTable::istore(int n) {
  transition(itos, vtos);
  __ reg2mem_opt(Z_tos, iaddress(n), false);
}

void TemplateTable::lstore(int n) {
  transition(ltos, vtos);
  __ reg2mem_opt(Z_tos, laddress(n));
}

void TemplateTable::fstore(int n) {
  transition(ftos, vtos);
  __ freg2mem_opt(Z_ftos, faddress(n), false);
}

void TemplateTable::dstore(int n) {
  transition(dtos, vtos);
  __ freg2mem_opt(Z_ftos, daddress(n));
}

void TemplateTable::astore(int n) {
  transition(vtos, vtos);
  __ pop_ptr(Z_tos);
  __ reg2mem_opt(Z_tos, aaddress(n));
}

void TemplateTable::pop() {
  transition(vtos, vtos);
  __ add2reg(Z_esp, Interpreter::stackElementSize);
}

void TemplateTable::pop2() {
  transition(vtos, vtos);
  __ add2reg(Z_esp, 2 * Interpreter::stackElementSize);
}

void TemplateTable::dup() {
  transition(vtos, vtos);
  __ load_ptr(0, Z_tos);
  __ push_ptr(Z_tos);
  // stack: ..., a, a
}

void TemplateTable::dup_x1() {
  transition(vtos, vtos);

  // stack: ..., a, b
  __ load_ptr(0, Z_tos);          // load b
  __ load_ptr(1, Z_R0_scratch);   // load a
  __ store_ptr(1, Z_tos);         // store b
  __ store_ptr(0, Z_R0_scratch);  // store a
  __ push_ptr(Z_tos);             // push b
  // stack: ..., b, a, b
}

void TemplateTable::dup_x2() {
  transition(vtos, vtos);

  // stack: ..., a, b, c
  __ load_ptr(0, Z_R0_scratch);   // load c
  __ load_ptr(2, Z_R1_scratch);   // load a
  __ store_ptr(2, Z_R0_scratch);  // store c in a
  __ push_ptr(Z_R0_scratch);      // push c
  // stack: ..., c, b, c, c
  __ load_ptr(2, Z_R0_scratch);   // load b
  __ store_ptr(2, Z_R1_scratch);  // store a in b
  // stack: ..., c, a, c, c
  __ store_ptr(1, Z_R0_scratch);  // store b in c
  // stack: ..., c, a, b, c
}

void TemplateTable::dup2() {
  transition(vtos, vtos);

  // stack: ..., a, b
  __ load_ptr(1, Z_R0_scratch);  // load a
  __ push_ptr(Z_R0_scratch);     // push a
  __ load_ptr(1, Z_R0_scratch);  // load b
  __ push_ptr(Z_R0_scratch);     // push b
  // stack: ..., a, b, a, b
}

void TemplateTable::dup2_x1() {
  transition(vtos, vtos);

  // stack: ..., a, b, c
  __ load_ptr(0, Z_R0_scratch);  // load c
  __ load_ptr(1, Z_R1_scratch);  // load b
  __ push_ptr(Z_R1_scratch);     // push b
  __ push_ptr(Z_R0_scratch);     // push c
  // stack: ..., a, b, c, b, c
  __ store_ptr(3, Z_R0_scratch); // store c in b
  // stack: ..., a, c, c, b, c
  __ load_ptr( 4, Z_R0_scratch); // load a
  __ store_ptr(2, Z_R0_scratch); // store a in 2nd c
  // stack: ..., a, c, a, b, c
  __ store_ptr(4, Z_R1_scratch); // store b in a
  // stack: ..., b, c, a, b, c
}

void TemplateTable::dup2_x2() {
  transition(vtos, vtos);

  // stack: ..., a, b, c, d
  __ load_ptr(0, Z_R0_scratch);   // load d
  __ load_ptr(1, Z_R1_scratch);   // load c
  __ push_ptr(Z_R1_scratch);      // push c
  __ push_ptr(Z_R0_scratch);      // push d
  // stack: ..., a, b, c, d, c, d
  __ load_ptr(4, Z_R1_scratch);   // load b
  __ store_ptr(2, Z_R1_scratch);  // store b in d
  __ store_ptr(4, Z_R0_scratch);  // store d in b
  // stack: ..., a, d, c, b, c, d
  __ load_ptr(5, Z_R0_scratch);   // load a
  __ load_ptr(3, Z_R1_scratch);   // load c
  __ store_ptr(3, Z_R0_scratch);  // store a in c
  __ store_ptr(5, Z_R1_scratch);  // store c in a
  // stack: ..., c, d, a, b, c, d
}

void TemplateTable::swap() {
  transition(vtos, vtos);

  // stack: ..., a, b
  __ load_ptr(1, Z_R0_scratch);  // load a
  __ load_ptr(0, Z_R1_scratch);  // load b
  __ store_ptr(0, Z_R0_scratch);  // store a in b
  __ store_ptr(1, Z_R1_scratch);  // store b in a
  // stack: ..., b, a
}

void TemplateTable::iop2(Operation op) {
  transition(itos, itos);
  switch (op) {
    case add  :                           __ z_ay(Z_tos,  __ stackTop()); __ pop_i(); break;
    case sub  :                           __ z_sy(Z_tos,  __ stackTop()); __ pop_i(); __ z_lcr(Z_tos, Z_tos); break;
    case mul  :                           __ z_msy(Z_tos, __ stackTop()); __ pop_i(); break;
    case _and :                           __ z_ny(Z_tos,  __ stackTop()); __ pop_i(); break;
    case _or  :                           __ z_oy(Z_tos,  __ stackTop()); __ pop_i(); break;
    case _xor :                           __ z_xy(Z_tos,  __ stackTop()); __ pop_i(); break;
    case shl  : __ z_lr(Z_tmp_1, Z_tos);
                __ z_nill(Z_tmp_1, 31);  // Lowest 5 bits are shiftamount.
                                          __ pop_i(Z_tos);   __ z_sll(Z_tos, 0,  Z_tmp_1); break;
    case shr  : __ z_lr(Z_tmp_1, Z_tos);
                __ z_nill(Z_tmp_1, 31);  // Lowest 5 bits are shiftamount.
                                          __ pop_i(Z_tos);   __ z_sra(Z_tos, 0,  Z_tmp_1); break;
    case ushr : __ z_lr(Z_tmp_1, Z_tos);
                __ z_nill(Z_tmp_1, 31);  // Lowest 5 bits are shiftamount.
                                          __ pop_i(Z_tos);   __ z_srl(Z_tos, 0,  Z_tmp_1); break;
    default   : ShouldNotReachHere(); break;
  }
  return;
}

void TemplateTable::lop2(Operation op) {
  transition(ltos, ltos);

  switch (op) {
    case add  :  __ z_ag(Z_tos,  __ stackTop()); __ pop_l(); break;
    case sub  :  __ z_sg(Z_tos,  __ stackTop()); __ pop_l(); __ z_lcgr(Z_tos, Z_tos); break;
    case mul  :  __ z_msg(Z_tos, __ stackTop()); __ pop_l(); break;
    case _and :  __ z_ng(Z_tos,  __ stackTop()); __ pop_l(); break;
    case _or  :  __ z_og(Z_tos,  __ stackTop()); __ pop_l(); break;
    case _xor :  __ z_xg(Z_tos,  __ stackTop()); __ pop_l(); break;
    default   : ShouldNotReachHere(); break;
  }
  return;
}

// Common part of idiv/irem.
static void idiv_helper(InterpreterMacroAssembler * _masm, address exception) {
  NearLabel not_null;

  // Use register pair Z_tmp_1, Z_tmp_2 for DIVIDE SINGLE.
  assert(Z_tmp_1->successor() == Z_tmp_2, " need even/odd register pair for idiv/irem");

  // Get dividend.
  __ pop_i(Z_tmp_2);

  // If divisor == 0 throw exception.
  __ compare32_and_branch(Z_tos, (intptr_t) 0,
                          Assembler::bcondNotEqual, not_null   );
  __ load_absolute_address(Z_R1_scratch, exception);
  __ z_br(Z_R1_scratch);

  __ bind(not_null);

  __ z_lgfr(Z_tmp_2, Z_tmp_2);   // Sign extend dividend.
  __ z_dsgfr(Z_tmp_1, Z_tos);    // Do it.
}

void TemplateTable::idiv() {
  transition(itos, itos);

  idiv_helper(_masm, Interpreter::_throw_ArithmeticException_entry);
  __ z_llgfr(Z_tos, Z_tmp_2);     // Result is in Z_tmp_2.
}

void TemplateTable::irem() {
  transition(itos, itos);

  idiv_helper(_masm, Interpreter::_throw_ArithmeticException_entry);
  __ z_llgfr(Z_tos, Z_tmp_1);     // Result is in Z_tmp_1.
}

void TemplateTable::lmul() {
  transition(ltos, ltos);

  // Multiply with memory operand.
  __ z_msg(Z_tos, __ stackTop());
  __ pop_l();  // Pop operand.
}

// Common part of ldiv/lrem.
//
// Input:
//     Z_tos := the divisor (dividend still on stack)
//
// Updated registers:
//     Z_tmp_1 := pop_l() % Z_tos     ; if is_ldiv == false
//     Z_tmp_2 := pop_l() / Z_tos     ; if is_ldiv == true
//
static void ldiv_helper(InterpreterMacroAssembler * _masm, address exception, bool is_ldiv) {
  NearLabel not_null, done;

  // Use register pair Z_tmp_1, Z_tmp_2 for DIVIDE SINGLE.
  assert(Z_tmp_1->successor() == Z_tmp_2,
         " need even/odd register pair for idiv/irem");

  // Get dividend.
  __ pop_l(Z_tmp_2);

  // If divisor == 0 throw exception.
  __ compare64_and_branch(Z_tos, (intptr_t)0, Assembler::bcondNotEqual, not_null);
  __ load_absolute_address(Z_R1_scratch, exception);
  __ z_br(Z_R1_scratch);

  __ bind(not_null);
  // Special case for dividend == 0x8000 and divisor == -1.
  if (is_ldiv) {
    // result := Z_tmp_2 := - dividend
    __ z_lcgr(Z_tmp_2, Z_tmp_2);
  } else {
    // result remainder := Z_tmp_1 := 0
    __ clear_reg(Z_tmp_1, true, false);  // Don't set CC.
  }

  // if divisor == -1 goto done
  __ compare64_and_branch(Z_tos, -1, Assembler::bcondEqual, done);
  if (is_ldiv)
    // Restore sign, because divisor != -1.
    __ z_lcgr(Z_tmp_2, Z_tmp_2);
  __ z_dsgr(Z_tmp_1, Z_tos);    // Do it.
  __ bind(done);
}

void TemplateTable::ldiv() {
  transition(ltos, ltos);

  ldiv_helper(_masm, Interpreter::_throw_ArithmeticException_entry, true /*is_ldiv*/);
  __ z_lgr(Z_tos, Z_tmp_2);     // Result is in Z_tmp_2.
}

void TemplateTable::lrem() {
  transition(ltos, ltos);

  ldiv_helper(_masm, Interpreter::_throw_ArithmeticException_entry, false /*is_ldiv*/);
  __ z_lgr(Z_tos, Z_tmp_1);     // Result is in Z_tmp_1.
}

void TemplateTable::lshl() {
  transition(itos, ltos);

  // Z_tos: shift amount
  __ pop_l(Z_tmp_1);              // Get shift value.
  __ z_sllg(Z_tos, Z_tmp_1, 0, Z_tos);
}

void TemplateTable::lshr() {
  transition(itos, ltos);

  // Z_tos: shift amount
  __ pop_l(Z_tmp_1);              // Get shift value.
  __ z_srag(Z_tos, Z_tmp_1, 0, Z_tos);
}

void TemplateTable::lushr() {
  transition(itos, ltos);

  // Z_tos: shift amount
  __ pop_l(Z_tmp_1);              // Get shift value.
  __ z_srlg(Z_tos, Z_tmp_1, 0, Z_tos);
}

void TemplateTable::fop2(Operation op) {
  transition(ftos, ftos);

  switch (op) {
    case add:
      // Add memory operand.
      __ z_aeb(Z_ftos, __ stackTop()); __ pop_f(); return;
    case sub:
      // Sub memory operand.
      __ z_ler(Z_F1, Z_ftos);    // first operand
      __ pop_f(Z_ftos);          // second operand from stack
      __ z_sebr(Z_ftos, Z_F1);
      return;
    case mul:
      // Multiply with memory operand.
      __ z_meeb(Z_ftos, __ stackTop()); __ pop_f(); return;
    case div:
      __ z_ler(Z_F1, Z_ftos);    // first operand
      __ pop_f(Z_ftos);          // second operand from stack
      __ z_debr(Z_ftos, Z_F1);
      return;
    case rem:
      // Do runtime call.
      __ z_ler(Z_FARG2, Z_ftos);  // divisor
      __ pop_f(Z_FARG1);          // dividend
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::frem));
      // Result should be in the right place (Z_ftos == Z_FRET).
      return;
    default:
      ShouldNotReachHere();
      return;
  }
}

void TemplateTable::dop2(Operation op) {
  transition(dtos, dtos);

  switch (op) {
    case add:
      // Add memory operand.
      __ z_adb(Z_ftos, __ stackTop()); __ pop_d(); return;
    case sub:
      // Sub memory operand.
      __ z_ldr(Z_F1, Z_ftos);    // first operand
      __ pop_d(Z_ftos);          // second operand from stack
      __ z_sdbr(Z_ftos, Z_F1);
      return;
    case mul:
      // Multiply with memory operand.
      __ z_mdb(Z_ftos, __ stackTop()); __ pop_d(); return;
    case div:
      __ z_ldr(Z_F1, Z_ftos);    // first operand
      __ pop_d(Z_ftos);          // second operand from stack
      __ z_ddbr(Z_ftos, Z_F1);
      return;
    case rem:
      // Do runtime call.
      __ z_ldr(Z_FARG2, Z_ftos);  // divisor
      __ pop_d(Z_FARG1);          // dividend
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::drem));
      // Result should be in the right place (Z_ftos == Z_FRET).
      return;
    default:
      ShouldNotReachHere();
      return;
  }
}

void TemplateTable::ineg() {
  transition(itos, itos);
  __ z_lcr(Z_tos);
}

void TemplateTable::lneg() {
  transition(ltos, ltos);
  __ z_lcgr(Z_tos);
}

void TemplateTable::fneg() {
  transition(ftos, ftos);
  __ z_lcebr(Z_ftos, Z_ftos);
}

void TemplateTable::dneg() {
  transition(dtos, dtos);
  __ z_lcdbr(Z_ftos, Z_ftos);
}

void TemplateTable::iinc() {
  transition(vtos, vtos);

  Address local;
  __ z_lb(Z_R0_scratch, at_bcp(2)); // Get constant.
  locals_index(Z_R1_scratch);
  local = iaddress(_masm, Z_R1_scratch);
  __ z_a(Z_R0_scratch, local);
  __ reg2mem_opt(Z_R0_scratch, local, false);
}

void TemplateTable::wide_iinc() {
  transition(vtos, vtos);

  // Z_tmp_1 := increment
  __ get_2_byte_integer_at_bcp(Z_tmp_1, 4, InterpreterMacroAssembler::Signed);
  // Z_R1_scratch := index of local to increment
  locals_index_wide(Z_tmp_2);
  // Load, increment, and store.
  __ access_local_int(Z_tmp_2, Z_tos);
  __ z_agr(Z_tos,  Z_tmp_1);
  // Shifted index is still in Z_tmp_2.
  __ reg2mem_opt(Z_tos, Address(Z_locals, Z_tmp_2), false);
}


void TemplateTable::convert() {
  // Checking
#ifdef ASSERT
  TosState   tos_in  = ilgl;
  TosState   tos_out = ilgl;

  switch (bytecode()) {
    case Bytecodes::_i2l:
    case Bytecodes::_i2f:
    case Bytecodes::_i2d:
    case Bytecodes::_i2b:
    case Bytecodes::_i2c:
    case Bytecodes::_i2s:
      tos_in = itos;
      break;
    case Bytecodes::_l2i:
    case Bytecodes::_l2f:
    case Bytecodes::_l2d:
      tos_in = ltos;
      break;
    case Bytecodes::_f2i:
    case Bytecodes::_f2l:
    case Bytecodes::_f2d:
      tos_in = ftos;
      break;
    case Bytecodes::_d2i:
    case Bytecodes::_d2l:
    case Bytecodes::_d2f:
      tos_in = dtos;
      break;
    default :
      ShouldNotReachHere();
  }
  switch (bytecode()) {
    case Bytecodes::_l2i:
    case Bytecodes::_f2i:
    case Bytecodes::_d2i:
    case Bytecodes::_i2b:
    case Bytecodes::_i2c:
    case Bytecodes::_i2s:
      tos_out = itos;
      break;
    case Bytecodes::_i2l:
    case Bytecodes::_f2l:
    case Bytecodes::_d2l:
      tos_out = ltos;
      break;
    case Bytecodes::_i2f:
    case Bytecodes::_l2f:
    case Bytecodes::_d2f:
      tos_out = ftos;
      break;
    case Bytecodes::_i2d:
    case Bytecodes::_l2d:
    case Bytecodes::_f2d:
      tos_out = dtos;
      break;
    default :
      ShouldNotReachHere();
  }

  transition(tos_in, tos_out);
#endif // ASSERT

  // Conversion
  Label done;
  switch (bytecode()) {
    case Bytecodes::_i2l:
      __ z_lgfr(Z_tos, Z_tos);
      return;
    case Bytecodes::_i2f:
      __ z_cefbr(Z_ftos, Z_tos);
      return;
    case Bytecodes::_i2d:
      __ z_cdfbr(Z_ftos, Z_tos);
      return;
    case Bytecodes::_i2b:
      // Sign extend least significant byte.
      __ move_reg_if_needed(Z_tos, T_BYTE, Z_tos, T_INT);
      return;
    case Bytecodes::_i2c:
      // Zero extend 2 least significant bytes.
      __ move_reg_if_needed(Z_tos, T_CHAR, Z_tos, T_INT);
      return;
    case Bytecodes::_i2s:
      // Sign extend 2 least significant bytes.
      __ move_reg_if_needed(Z_tos, T_SHORT, Z_tos, T_INT);
      return;
    case Bytecodes::_l2i:
      // Sign-extend not needed here, upper 4 bytes of int value in register are ignored.
      return;
    case Bytecodes::_l2f:
      __ z_cegbr(Z_ftos, Z_tos);
      return;
    case Bytecodes::_l2d:
      __ z_cdgbr(Z_ftos, Z_tos);
      return;
    case Bytecodes::_f2i:
    case Bytecodes::_f2l:
      __ clear_reg(Z_tos, true, false);  // Don't set CC.
      __ z_cebr(Z_ftos, Z_ftos);
      __ z_brno(done); // NaN -> 0
      if (bytecode() == Bytecodes::_f2i)
        __ z_cfebr(Z_tos, Z_ftos, Assembler::to_zero);
      else // bytecode() == Bytecodes::_f2l
        __ z_cgebr(Z_tos, Z_ftos, Assembler::to_zero);
      break;
    case Bytecodes::_f2d:
      __ move_freg_if_needed(Z_ftos, T_DOUBLE, Z_ftos, T_FLOAT);
      return;
    case Bytecodes::_d2i:
    case Bytecodes::_d2l:
      __ clear_reg(Z_tos, true, false);  // Ddon't set CC.
      __ z_cdbr(Z_ftos, Z_ftos);
      __ z_brno(done); // NaN -> 0
      if (bytecode() == Bytecodes::_d2i)
        __ z_cfdbr(Z_tos, Z_ftos, Assembler::to_zero);
      else // Bytecodes::_d2l
        __ z_cgdbr(Z_tos, Z_ftos, Assembler::to_zero);
      break;
    case Bytecodes::_d2f:
      __ move_freg_if_needed(Z_ftos, T_FLOAT, Z_ftos, T_DOUBLE);
      return;
    default:
      ShouldNotReachHere();
  }
  __ bind(done);
}

void TemplateTable::lcmp() {
  transition(ltos, itos);

  Label   done;
  Register val1 = Z_R0_scratch;
  Register val2 = Z_R1_scratch;

  if (VM_Version::has_LoadStoreConditional()) {
    __ pop_l(val1);           // pop value 1.
    __ z_lghi(val2,  -1);     // lt value
    __ z_cgr(val1, Z_tos);    // Compare with Z_tos (value 2). Protect CC under all circumstances.
    __ z_lghi(val1,   1);     // gt value
    __ z_lghi(Z_tos,  0);     // eq value

    __ z_locgr(Z_tos, val1, Assembler::bcondHigh);
    __ z_locgr(Z_tos, val2, Assembler::bcondLow);
  } else {
    __ pop_l(val1);           // Pop value 1.
    __ z_cgr(val1, Z_tos);    // Compare with Z_tos (value 2). Protect CC under all circumstances.

    __ z_lghi(Z_tos,  0);     // eq value
    __ z_bre(done);

    __ z_lghi(Z_tos,  1);     // gt value
    __ z_brh(done);

    __ z_lghi(Z_tos, -1);     // lt value
  }

  __ bind(done);
}


void TemplateTable::float_cmp(bool is_float, int unordered_result) {
  Label done;

  if (is_float) {
    __ pop_f(Z_FARG2);
    __ z_cebr(Z_FARG2, Z_ftos);
  } else {
    __ pop_d(Z_FARG2);
    __ z_cdbr(Z_FARG2, Z_ftos);
  }

  if (VM_Version::has_LoadStoreConditional()) {
    Register one       = Z_R0_scratch;
    Register minus_one = Z_R1_scratch;
    __ z_lghi(minus_one,  -1);
    __ z_lghi(one,  1);
    __ z_lghi(Z_tos, 0);
    __ z_locgr(Z_tos, one,       unordered_result == 1 ? Assembler::bcondHighOrNotOrdered : Assembler::bcondHigh);
    __ z_locgr(Z_tos, minus_one, unordered_result == 1 ? Assembler::bcondLow              : Assembler::bcondLowOrNotOrdered);
  } else {
    // Z_FARG2 == Z_ftos
    __ clear_reg(Z_tos, false, false);
    __ z_bre(done);

    // F_ARG2 > Z_Ftos, or unordered
    __ z_lhi(Z_tos, 1);
    __ z_brc(unordered_result == 1 ? Assembler::bcondHighOrNotOrdered : Assembler::bcondHigh, done);

    // F_ARG2 < Z_FTOS, or unordered
    __ z_lhi(Z_tos, -1);

    __ bind(done);
  }
}

void TemplateTable::branch(bool is_jsr, bool is_wide) {
  const Register   bumped_count = Z_tmp_1;
  const Register   method       = Z_tmp_2;
  const Register   m_counters   = Z_R1_scratch;
  const Register   mdo          = Z_tos;

  BLOCK_COMMENT("TemplateTable::branch {");
  __ get_method(method);
  __ profile_taken_branch(mdo, bumped_count);

  const ByteSize ctr_offset = InvocationCounter::counter_offset();
  const ByteSize be_offset  = MethodCounters::backedge_counter_offset()   + ctr_offset;
  const ByteSize inv_offset = MethodCounters::invocation_counter_offset() + ctr_offset;

  // Get (wide) offset to disp.
  const Register disp = Z_ARG5;
  if (is_wide) {
    __ get_4_byte_integer_at_bcp(disp, 1);
  } else {
    __ get_2_byte_integer_at_bcp(disp, 1, InterpreterMacroAssembler::Signed);
  }

  // Handle all the JSR stuff here, then exit.
  // It's much shorter and cleaner than intermingling with the
  // non-JSR normal-branch stuff occurring below.
  if (is_jsr) {
    // Compute return address as bci in Z_tos.
    __ z_lgr(Z_R1_scratch, Z_bcp);
    __ z_sg(Z_R1_scratch, Address(method, Method::const_offset()));
    __ add2reg(Z_tos, (is_wide ? 5 : 3) - in_bytes(ConstMethod::codes_offset()), Z_R1_scratch);

    // Bump bcp to target of JSR.
    __ z_agr(Z_bcp, disp);
    // Push return address for "ret" on stack.
    __ push_ptr(Z_tos);
    // And away we go!
    __ dispatch_next(vtos, 0 , true);
    return;
  }

  // Normal (non-jsr) branch handling.

  // Bump bytecode pointer by displacement (take the branch).
  __ z_agr(Z_bcp, disp);

  assert(UseLoopCounter || !UseOnStackReplacement,
         "on-stack-replacement requires loop counters");

  NearLabel backedge_counter_overflow;
  NearLabel dispatch;
  int       increment = InvocationCounter::count_increment;

  if (UseLoopCounter) {
    // Increment backedge counter for backward branches.
    // disp: target offset
    // Z_bcp: target bcp
    // Z_locals: locals pointer
    //
    // Count only if backward branch.
    __ compare32_and_branch(disp, (intptr_t)0, Assembler::bcondHigh, dispatch);


    if (ProfileInterpreter) {
      NearLabel   no_mdo;

      // Are we profiling?
      __ load_and_test_long(mdo, Address(method, Method::method_data_offset()));
      __ branch_optimized(Assembler::bcondZero, no_mdo);

      // Increment the MDO backedge counter.
      const Address mdo_backedge_counter(mdo, MethodData::backedge_counter_offset() + InvocationCounter::counter_offset());

      const Address mask(mdo, MethodData::backedge_mask_offset());
      __ increment_mask_and_jump(mdo_backedge_counter, increment, mask,
                                 Z_ARG2, false, Assembler::bcondZero,
                                 UseOnStackReplacement ? &backedge_counter_overflow : NULL);
      __ z_bru(dispatch);
      __ bind(no_mdo);
    }

    // Increment backedge counter in MethodCounters*.
    __ get_method_counters(method, m_counters, dispatch);
    const Address mask(m_counters, MethodCounters::backedge_mask_offset());
    __ increment_mask_and_jump(Address(m_counters, be_offset),
                               increment, mask,
                               Z_ARG2, false, Assembler::bcondZero,
                               UseOnStackReplacement ? &backedge_counter_overflow : NULL);
    __ bind(dispatch);
  }

  // Pre-load the next target bytecode into rbx.
  __ z_llgc(Z_bytecode, Address(Z_bcp, (intptr_t) 0));

  // Continue with the bytecode @ target.
  // Z_tos: Return bci for jsr's, unused otherwise.
  // Z_bytecode: target bytecode
  // Z_bcp: target bcp
  __ dispatch_only(vtos, true);

  // Out-of-line code runtime calls.
  if (UseLoopCounter && UseOnStackReplacement) {
    // invocation counter overflow
    __ bind(backedge_counter_overflow);

    __ z_lcgr(Z_ARG2, disp); // Z_ARG2 := -disp
    __ z_agr(Z_ARG2, Z_bcp); // Z_ARG2 := branch target bcp - disp == branch bcp
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::frequency_counter_overflow),
               Z_ARG2);

    // Z_RET: osr nmethod (osr ok) or NULL (osr not possible).
    __ compare64_and_branch(Z_RET, (intptr_t) 0, Assembler::bcondEqual, dispatch);

    // Nmethod may have been invalidated (VM may block upon call_VM return).
    __ z_cliy(nmethod::state_offset(), Z_RET, nmethod::in_use);
    __ z_brne(dispatch);

    // Migrate the interpreter frame off of the stack.

    __ z_lgr(Z_tmp_1, Z_RET); // Save the nmethod.

    call_VM(noreg,
            CAST_FROM_FN_PTR(address, SharedRuntime::OSR_migration_begin));

    // Z_RET is OSR buffer, move it to expected parameter location.
    __ lgr_if_needed(Z_ARG1, Z_RET);

    // Pop the interpreter frame ...
    __ pop_interpreter_frame(Z_R14, Z_ARG2/*tmp1*/, Z_ARG3/*tmp2*/);

    // ... and begin the OSR nmethod.
    __ z_lg(Z_R1_scratch, Address(Z_tmp_1, nmethod::osr_entry_point_offset()));
    __ z_br(Z_R1_scratch);
  }
  BLOCK_COMMENT("} TemplateTable::branch");
}

void TemplateTable::if_0cmp(Condition cc) {
  transition(itos, vtos);

  // Assume branch is more often taken than not (loops use backward branches).
  NearLabel not_taken;
  __ compare32_and_branch(Z_tos, (intptr_t) 0, j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(Z_tos);
}

void TemplateTable::if_icmp(Condition cc) {
  transition(itos, vtos);

  // Assume branch is more often taken than not (loops use backward branches).
  NearLabel not_taken;
  __ pop_i(Z_R0_scratch);
  __ compare32_and_branch(Z_R0_scratch, Z_tos, j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(Z_tos);
}

void TemplateTable::if_nullcmp(Condition cc) {
  transition(atos, vtos);

  // Assume branch is more often taken than not (loops use backward branches) .
  NearLabel not_taken;
  __ compare64_and_branch(Z_tos, (intptr_t) 0, j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(Z_tos);
}

void TemplateTable::if_acmp(Condition cc) {
  transition(atos, vtos);
  // Assume branch is more often taken than not (loops use backward branches).
  NearLabel not_taken;
  __ pop_ptr(Z_ARG2);
  __ verify_oop(Z_ARG2);
  __ verify_oop(Z_tos);
  __ compareU64_and_branch(Z_tos, Z_ARG2, j_not(cc), not_taken);
  branch(false, false);
  __ bind(not_taken);
  __ profile_not_taken_branch(Z_ARG3);
}

void TemplateTable::ret() {
  transition(vtos, vtos);

  locals_index(Z_tmp_1);
  // Get return bci, compute return bcp. Must load 64 bits.
  __ mem2reg_opt(Z_tmp_1, iaddress(_masm, Z_tmp_1));
  __ profile_ret(Z_tmp_1, Z_tmp_2);
  __ get_method(Z_tos);
  __ mem2reg_opt(Z_R1_scratch, Address(Z_tos, Method::const_offset()));
  __ load_address(Z_bcp, Address(Z_R1_scratch, Z_tmp_1, ConstMethod::codes_offset()));
  __ dispatch_next(vtos, 0 , true);
}

void TemplateTable::wide_ret() {
  transition(vtos, vtos);

  locals_index_wide(Z_tmp_1);
  // Get return bci, compute return bcp.
  __ mem2reg_opt(Z_tmp_1, aaddress(_masm, Z_tmp_1));
  __ profile_ret(Z_tmp_1, Z_tmp_2);
  __ get_method(Z_tos);
  __ mem2reg_opt(Z_R1_scratch, Address(Z_tos, Method::const_offset()));
  __ load_address(Z_bcp, Address(Z_R1_scratch, Z_tmp_1, ConstMethod::codes_offset()));
  __ dispatch_next(vtos, 0, true);
}

void TemplateTable::tableswitch () {
  transition(itos, vtos);

  NearLabel default_case, continue_execution;
  Register  bcp = Z_ARG5;
  // Align bcp.
  __ load_address(bcp, at_bcp(BytesPerInt));
  __ z_nill(bcp, (-BytesPerInt) & 0xffff);

  // Load lo & hi.
  Register low  = Z_tmp_1;
  Register high = Z_tmp_2;

  // Load low into 64 bits, since used for address calculation.
  __ mem2reg_signed_opt(low, Address(bcp, BytesPerInt));
  __ mem2reg_opt(high, Address(bcp, 2 * BytesPerInt), false);
  // Sign extend "label" value for address calculation.
  __ z_lgfr(Z_tos, Z_tos);

  // Check against lo & hi.
  __ compare32_and_branch(Z_tos, low, Assembler::bcondLow, default_case);
  __ compare32_and_branch(Z_tos, high, Assembler::bcondHigh, default_case);

  // Lookup dispatch offset.
  __ z_sgr(Z_tos, low);
  Register jump_table_offset = Z_ARG3;
  // Index2offset; index in Z_tos is killed by profile_switch_case.
  __ z_sllg(jump_table_offset, Z_tos, LogBytesPerInt);
  __ profile_switch_case(Z_tos, Z_ARG4 /*tmp for mdp*/, low/*tmp*/, Z_bytecode/*tmp*/);

  Register index = Z_tmp_2;

  // Load index sign extended for addressing.
  __ mem2reg_signed_opt(index, Address(bcp, jump_table_offset, 3 * BytesPerInt));

  // Continue execution.
  __ bind(continue_execution);

  // Load next bytecode.
  __ z_llgc(Z_bytecode, Address(Z_bcp, index));
  __ z_agr(Z_bcp, index); // Advance bcp.
  __ dispatch_only(vtos, true);

  // Handle default.
  __ bind(default_case);

  __ profile_switch_default(Z_tos);
  __ mem2reg_signed_opt(index, Address(bcp));
  __ z_bru(continue_execution);
}

void TemplateTable::lookupswitch () {
  transition(itos, itos);
  __ stop("lookupswitch bytecode should have been rewritten");
}

void TemplateTable::fast_linearswitch () {
  transition(itos, vtos);

  Label    loop_entry, loop, found, continue_execution;
  Register bcp = Z_ARG5;

  // Align bcp.
  __ load_address(bcp, at_bcp(BytesPerInt));
  __ z_nill(bcp, (-BytesPerInt) & 0xffff);

  // Start search with last case.
  Register current_case_offset = Z_tmp_1;

  __ mem2reg_signed_opt(current_case_offset, Address(bcp, BytesPerInt));
  __ z_sllg(current_case_offset, current_case_offset, LogBytesPerWord);   // index2bytes
  __ z_bru(loop_entry);

  // table search
  __ bind(loop);

  __ z_c(Z_tos, Address(bcp, current_case_offset, 2 * BytesPerInt));
  __ z_bre(found);

  __ bind(loop_entry);
  __ z_aghi(current_case_offset, -2 * BytesPerInt);  // Decrement.
  __ z_brnl(loop);

  // default case
  Register   offset = Z_tmp_2;

  __ profile_switch_default(Z_tos);
  // Load offset sign extended for addressing.
  __ mem2reg_signed_opt(offset, Address(bcp));
  __ z_bru(continue_execution);

  // Entry found -> get offset.
  __ bind(found);
  __ mem2reg_signed_opt(offset, Address(bcp, current_case_offset, 3 * BytesPerInt));
  // Profile that this case was taken.
  Register current_case_idx = Z_ARG4;
  __ z_srlg(current_case_idx, current_case_offset, LogBytesPerWord); // bytes2index
  __ profile_switch_case(current_case_idx, Z_tos, bcp, Z_bytecode);

  // Continue execution.
  __ bind(continue_execution);

  // Load next bytecode.
  __ z_llgc(Z_bytecode, Address(Z_bcp, offset, 0));
  __ z_agr(Z_bcp, offset); // Advance bcp.
  __ dispatch_only(vtos, true);
}


void TemplateTable::fast_binaryswitch() {

  transition(itos, vtos);

  // Implementation using the following core algorithm:
  //
  // int binary_search(int key, LookupswitchPair* array, int n) {
  //   // Binary search according to "Methodik des Programmierens" by
  //   // Edsger W. Dijkstra and W.H.J. Feijen, Addison Wesley Germany 1985.
  //   int i = 0;
  //   int j = n;
  //   while (i+1 < j) {
  //     // invariant P: 0 <= i < j <= n and (a[i] <= key < a[j] or Q)
  //     // with      Q: for all i: 0 <= i < n: key < a[i]
  //     // where a stands for the array and assuming that the (inexisting)
  //     // element a[n] is infinitely big.
  //     int h = (i + j) >> 1;
  //     // i < h < j
  //     if (key < array[h].fast_match()) {
  //       j = h;
  //     } else {
  //       i = h;
  //     }
  //   }
  //   // R: a[i] <= key < a[i+1] or Q
  //   // (i.e., if key is within array, i is the correct index)
  //   return i;
  // }

  // Register allocation
  // Note: Since we use the indices in address operands, we do all the
  // computation in 64 bits.
  const Register key   = Z_tos; // Already set (tosca).
  const Register array = Z_tmp_1;
  const Register i     = Z_tmp_2;
  const Register j     = Z_ARG5;
  const Register h     = Z_ARG4;
  const Register temp  = Z_R1_scratch;

  // Find array start.
  __ load_address(array, at_bcp(3 * BytesPerInt));
  __ z_nill(array, (-BytesPerInt) & 0xffff);   // align

  // Initialize i & j.
  __ clear_reg(i, true, false);  // i = 0;  Don't set CC.
  __ mem2reg_signed_opt(j, Address(array, -BytesPerInt)); // j = length(array);

  // And start.
  Label entry;
  __ z_bru(entry);

  // binary search loop
  {
    NearLabel   loop;

    __ bind(loop);

    // int h = (i + j) >> 1;
    __ add2reg_with_index(h, 0, i, j); // h = i + j;
    __ z_srag(h, h, 1);                // h = (i + j) >> 1;

    // if (key < array[h].fast_match()) {
    //   j = h;
    // } else {
    //   i = h;
    // }

    // Convert array[h].match to native byte-ordering before compare.
    __ z_sllg(temp, h, LogBytesPerWord);   // index2bytes
    __ mem2reg_opt(temp, Address(array, temp), false);

    NearLabel  else_;

    __ compare32_and_branch(key, temp, Assembler::bcondNotLow, else_);
    // j = h if (key <  array[h].fast_match())
    __ z_lgr(j, h);
    __ z_bru(entry); // continue

    __ bind(else_);

    // i = h if (key >= array[h].fast_match())
    __ z_lgr(i, h);  // and fallthrough

    // while (i+1 < j)
    __ bind(entry);

    // if (i + 1 < j) continue search
    __ add2reg(h, 1, i);
    __ compare64_and_branch(h, j, Assembler::bcondLow, loop);
  }

  // End of binary search, result index is i (must check again!).
  NearLabel default_case;

  // h is no longer needed, so use it to hold the byte offset.
  __ z_sllg(h, i, LogBytesPerWord);   // index2bytes
  __ mem2reg_opt(temp, Address(array, h), false);
  __ compare32_and_branch(key, temp, Assembler::bcondNotEqual, default_case);

  // entry found -> j = offset
  __ mem2reg_signed_opt(j, Address(array, h, BytesPerInt));
  __ profile_switch_case(i, key, array, Z_bytecode);
  // Load next bytecode.
  __ z_llgc(Z_bytecode, Address(Z_bcp, j));
  __ z_agr(Z_bcp, j);       // Advance bcp.
  __ dispatch_only(vtos, true);

  // default case -> j = default offset
  __ bind(default_case);

  __ profile_switch_default(i);
  __ mem2reg_signed_opt(j, Address(array, -2 * BytesPerInt));
  // Load next bytecode.
  __ z_llgc(Z_bytecode, Address(Z_bcp, j));
  __ z_agr(Z_bcp, j);       // Advance bcp.
  __ dispatch_only(vtos, true);
}

void TemplateTable::_return(TosState state) {
  transition(state, state);
  assert(_desc->calls_vm(),
         "inconsistent calls_vm information"); // call in remove_activation

  if (_desc->bytecode() == Bytecodes::_return_register_finalizer) {
    Register Rthis  = Z_ARG2;
    Register Rklass = Z_ARG5;
    Label skip_register_finalizer;
    assert(state == vtos, "only valid state");
    __ z_lg(Rthis, aaddress(0));
    __ load_klass(Rklass, Rthis);
    __ testbit(Address(Rklass, Klass::access_flags_offset()), exact_log2(JVM_ACC_HAS_FINALIZER));
    __ z_bfalse(skip_register_finalizer);
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::register_finalizer), Rthis);
    __ bind(skip_register_finalizer);
  }

  if (_desc->bytecode() != Bytecodes::_return_register_finalizer) {
    Label no_safepoint;
    const Address poll_byte_addr(Z_thread, in_bytes(JavaThread::polling_word_offset()) + 7 /* Big Endian */);
    __ z_tm(poll_byte_addr, SafepointMechanism::poll_bit());
    __ z_braz(no_safepoint);
    __ push(state);
    __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint));
    __ pop(state);
    __ bind(no_safepoint);
  }

  if (state == itos) {
    // Narrow result if state is itos but result type is smaller.
    // Need to narrow in the return bytecode rather than in generate_return_entry
    // since compiled code callers expect the result to already be narrowed.
    __ narrow(Z_tos, Z_tmp_1); /* fall through */
  }

  __ remove_activation(state, Z_R14);
  __ z_br(Z_R14);
}

// ----------------------------------------------------------------------------
// NOTE: Cpe_offset is already computed as byte offset, so we must not
// shift it afterwards!
void TemplateTable::resolve_cache_and_index(int byte_no,
                                            Register cache,
                                            Register cpe_offset,
                                            size_t index_size) {
  BLOCK_COMMENT("resolve_cache_and_index {");
  NearLabel      resolved, clinit_barrier_slow;
  const Register bytecode_in_cpcache = Z_R1_scratch;
  const int      total_f1_offset = in_bytes(ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::f1_offset());
  assert_different_registers(cache, cpe_offset, bytecode_in_cpcache);

  Bytecodes::Code code = bytecode();
  switch (code) {
    case Bytecodes::_nofast_getfield: code = Bytecodes::_getfield; break;
    case Bytecodes::_nofast_putfield: code = Bytecodes::_putfield; break;
    default:
      break;
  }

  {
    assert(byte_no == f1_byte || byte_no == f2_byte, "byte_no out of range");
    __ get_cache_and_index_and_bytecode_at_bcp(cache, cpe_offset, bytecode_in_cpcache, byte_no, 1, index_size);
    // Have we resolved this bytecode?
    __ compare32_and_branch(bytecode_in_cpcache, (int)code, Assembler::bcondEqual, resolved);
  }

  // Resolve first time through.
  // Class initialization barrier slow path lands here as well.
  __ bind(clinit_barrier_slow);
  address entry = CAST_FROM_FN_PTR(address, InterpreterRuntime::resolve_from_cache);
  __ load_const_optimized(Z_ARG2, (int) code);
  __ call_VM(noreg, entry, Z_ARG2);

  // Update registers with resolved info.
  __ get_cache_and_index_at_bcp(cache, cpe_offset, 1, index_size);
  __ bind(resolved);

  // Class initialization barrier for static methods
  if (VM_Version::supports_fast_class_init_checks() && bytecode() == Bytecodes::_invokestatic) {
    const Register method = Z_R1_scratch;
    const Register klass  = Z_R1_scratch;

    __ load_resolved_method_at_index(byte_no, cache, cpe_offset, method);
    __ load_method_holder(klass, method);
    __ clinit_barrier(klass, Z_thread, NULL /*L_fast_path*/, &clinit_barrier_slow);
  }

  BLOCK_COMMENT("} resolve_cache_and_index");
}

// The Rcache and index registers must be set before call.
// Index is already a byte offset, don't shift!
void TemplateTable::load_field_cp_cache_entry(Register obj,
                                              Register cache,
                                              Register index,
                                              Register off,
                                              Register flags,
                                              bool is_static = false) {
  assert_different_registers(cache, index, flags, off);
  ByteSize cp_base_offset = ConstantPoolCache::base_offset();

  // Field offset
  __ mem2reg_opt(off, Address(cache, index, cp_base_offset + ConstantPoolCacheEntry::f2_offset()));
  // Flags. Must load 64 bits.
  __ mem2reg_opt(flags, Address(cache, index, cp_base_offset + ConstantPoolCacheEntry::flags_offset()));

  // klass overwrite register
  if (is_static) {
    __ mem2reg_opt(obj, Address(cache, index, cp_base_offset + ConstantPoolCacheEntry::f1_offset()));
    __ mem2reg_opt(obj, Address(obj, Klass::java_mirror_offset()));
    __ resolve_oop_handle(obj);
  }
}

void TemplateTable::load_invoke_cp_cache_entry(int byte_no,
                                               Register method,
                                               Register itable_index,
                                               Register flags,
                                               bool is_invokevirtual,
                                               bool is_invokevfinal, // unused
                                               bool is_invokedynamic) {
  BLOCK_COMMENT("load_invoke_cp_cache_entry {");
  // Setup registers.
  const Register cache     = Z_ARG1;
  const Register cpe_offset= flags;
  const ByteSize base_off  = ConstantPoolCache::base_offset();
  const ByteSize f1_off    = ConstantPoolCacheEntry::f1_offset();
  const ByteSize f2_off    = ConstantPoolCacheEntry::f2_offset();
  const ByteSize flags_off = ConstantPoolCacheEntry::flags_offset();
  const int method_offset  = in_bytes(base_off + ((byte_no == f2_byte) ? f2_off : f1_off));
  const int flags_offset   = in_bytes(base_off + flags_off);
  // Access constant pool cache fields.
  const int index_offset   = in_bytes(base_off + f2_off);

  assert_different_registers(method, itable_index, flags, cache);
  assert(is_invokevirtual == (byte_no == f2_byte), "is_invokevirtual flag redundant");

  if (is_invokevfinal) {
    // Already resolved.
     assert(itable_index == noreg, "register not used");
     __ get_cache_and_index_at_bcp(cache, cpe_offset, 1);
  } else {
    // Need to resolve.
    resolve_cache_and_index(byte_no, cache, cpe_offset, is_invokedynamic ? sizeof(u4) : sizeof(u2));
  }
  __ z_lg(method, Address(cache, cpe_offset, method_offset));

  if (itable_index != noreg) {
    __ z_lg(itable_index, Address(cache, cpe_offset, index_offset));
  }

  // Only load the lower 4 bytes and fill high bytes of flags with zeros.
  // Callers depend on this zero-extension!!!
  // Attention: overwrites cpe_offset == flags
  __ z_llgf(flags, Address(cache, cpe_offset, flags_offset + (BytesPerLong-BytesPerInt)));

  BLOCK_COMMENT("} load_invoke_cp_cache_entry");
}

// The registers cache and index expected to be set before call.
// Correct values of the cache and index registers are preserved.
void TemplateTable::jvmti_post_field_access(Register cache, Register index,
                                            bool is_static, bool has_tos) {

  // Do the JVMTI work here to avoid disturbing the register state below.
  // We use c_rarg registers here because we want to use the register used in
  // the call to the VM
  if (!JvmtiExport::can_post_field_access()) {
    return;
  }

  // Check to see if a field access watch has been set before we
  // take the time to call into the VM.
  Label exit;
  assert_different_registers(cache, index, Z_tos);
  __ load_absolute_address(Z_tos, (address)JvmtiExport::get_field_access_count_addr());
  __ load_and_test_int(Z_R0, Address(Z_tos));
  __ z_brz(exit);

  // Index is returned as byte offset, do not shift!
  __ get_cache_and_index_at_bcp(Z_ARG3, Z_R1_scratch, 1);

  // cache entry pointer
  __ add2reg_with_index(Z_ARG3,
                        in_bytes(ConstantPoolCache::base_offset()),
                        Z_ARG3, Z_R1_scratch);

  if (is_static) {
    __ clear_reg(Z_ARG2, true, false); // NULL object reference. Don't set CC.
  } else {
    __ mem2reg_opt(Z_ARG2, at_tos());  // Get object pointer without popping it.
    __ verify_oop(Z_ARG2);
  }
  // Z_ARG2: object pointer or NULL
  // Z_ARG3: cache entry pointer
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_access),
             Z_ARG2, Z_ARG3);
  __ get_cache_and_index_at_bcp(cache, index, 1);

  __ bind(exit);
}

void TemplateTable::pop_and_check_object(Register r) {
  __ pop_ptr(r);
  __ null_check(r);  // for field access must check obj.
  __ verify_oop(r);
}

void TemplateTable::getfield_or_static(int byte_no, bool is_static, RewriteControl rc) {
  transition(vtos, vtos);

  const Register cache = Z_tmp_1;
  const Register index = Z_tmp_2;
  const Register obj   = Z_tmp_1;
  const Register off   = Z_ARG2;
  const Register flags = Z_ARG1;
  const Register bc    = Z_tmp_1;  // Uses same reg as obj, so don't mix them.

  resolve_cache_and_index(byte_no, cache, index, sizeof(u2));
  jvmti_post_field_access(cache, index, is_static, false);
  load_field_cp_cache_entry(obj, cache, index, off, flags, is_static);

  if (!is_static) {
    // Obj is on the stack.
    pop_and_check_object(obj);
  }

  // Displacement is 0, so any store instruction will be fine on any CPU.
  const Address field(obj, off);

  Label    is_Byte, is_Bool, is_Int, is_Short, is_Char,
           is_Long, is_Float, is_Object, is_Double;
  Label    is_badState8, is_badState9, is_badStateA, is_badStateB,
           is_badStateC, is_badStateD, is_badStateE, is_badStateF,
           is_badState;
  Label    branchTable, atosHandler,  Done;
  Register br_tab       = Z_R1_scratch;
  bool     do_rewrite   = !is_static && (rc == may_rewrite);
  bool     dont_rewrite = (is_static || (rc == may_not_rewrite));

  assert(do_rewrite == !dont_rewrite, "Oops, code is not fit for that");
  assert(btos == 0, "change code, btos != 0");

  // Calculate branch table size. Generated code size depends on ASSERT and on bytecode rewriting.
#ifdef ASSERT
  const unsigned int bsize = dont_rewrite ? BTB_MINSIZE*1 : BTB_MINSIZE*4;
#else
  const unsigned int bsize = dont_rewrite ? BTB_MINSIZE*1 : BTB_MINSIZE*4;
#endif

  // Calculate address of branch table entry and branch there.
  {
    const int bit_shift = exact_log2(bsize); // Size of each branch table entry.
    const int r_bitpos  = 63 - bit_shift;
    const int l_bitpos  = r_bitpos - ConstantPoolCacheEntry::tos_state_bits + 1;
    const int n_rotate  = (bit_shift-ConstantPoolCacheEntry::tos_state_shift);
    __ z_larl(br_tab, branchTable);
    __ rotate_then_insert(flags, flags, l_bitpos, r_bitpos, n_rotate, true);
  }
  __ z_bc(Assembler::bcondAlways, 0, flags, br_tab);

  __ align_address(bsize);
  BIND(branchTable);

  // btos
  BTB_BEGIN(is_Byte, bsize, "getfield_or_static:is_Byte");
  __ z_lb(Z_tos, field);
  __ push(btos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_bgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Byte, bsize, "getfield_or_static:is_Byte");

  // ztos
  BTB_BEGIN(is_Bool, bsize, "getfield_or_static:is_Bool");
  __ z_lb(Z_tos, field);
  __ push(ztos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    // Use btos rewriting, no truncating to t/f bit is needed for getfield.
    patch_bytecode(Bytecodes::_fast_bgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Bool, bsize, "getfield_or_static:is_Bool");

  // ctos
  BTB_BEGIN(is_Char, bsize, "getfield_or_static:is_Char");
  // Load into 64 bits, works on all CPUs.
  __ z_llgh(Z_tos, field);
  __ push(ctos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_cgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Char, bsize, "getfield_or_static:is_Char");

  // stos
  BTB_BEGIN(is_Short, bsize, "getfield_or_static:is_Short");
  __ z_lh(Z_tos, field);
  __ push(stos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_sgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Short, bsize, "getfield_or_static:is_Short");

  // itos
  BTB_BEGIN(is_Int, bsize, "getfield_or_static:is_Int");
  __ mem2reg_opt(Z_tos, field, false);
  __ push(itos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_igetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Int, bsize, "getfield_or_static:is_Int");

  // ltos
  BTB_BEGIN(is_Long, bsize, "getfield_or_static:is_Long");
  __ mem2reg_opt(Z_tos, field);
  __ push(ltos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_lgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Long, bsize, "getfield_or_static:is_Long");

  // ftos
  BTB_BEGIN(is_Float, bsize, "getfield_or_static:is_Float");
  __ mem2freg_opt(Z_ftos, field, false);
  __ push(ftos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_fgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Float, bsize, "getfield_or_static:is_Float");

  // dtos
  BTB_BEGIN(is_Double, bsize, "getfield_or_static:is_Double");
  __ mem2freg_opt(Z_ftos, field);
  __ push(dtos);
  // Rewrite bytecode to be faster.
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_dgetfield, bc, Z_ARG5);
  }
  __ z_bru(Done);
  BTB_END(is_Double, bsize, "getfield_or_static:is_Double");

  // atos
  BTB_BEGIN(is_Object, bsize, "getfield_or_static:is_Object");
  __ z_bru(atosHandler);
  BTB_END(is_Object, bsize, "getfield_or_static:is_Object");

  // Bad state detection comes at no extra runtime cost.
  BTB_BEGIN(is_badState8, bsize, "getfield_or_static:is_badState8");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badState8, bsize, "getfield_or_static:is_badState8");
  BTB_BEGIN(is_badState9, bsize, "getfield_or_static:is_badState9");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badState9, bsize, "getfield_or_static:is_badState9");
  BTB_BEGIN(is_badStateA, bsize, "getfield_or_static:is_badStateA");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateA, bsize, "getfield_or_static:is_badStateA");
  BTB_BEGIN(is_badStateB, bsize, "getfield_or_static:is_badStateB");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateB, bsize, "getfield_or_static:is_badStateB");
  BTB_BEGIN(is_badStateC, bsize, "getfield_or_static:is_badStateC");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateC, bsize, "getfield_or_static:is_badStateC");
  BTB_BEGIN(is_badStateD, bsize, "getfield_or_static:is_badStateD");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateD, bsize, "getfield_or_static:is_badStateD");
  BTB_BEGIN(is_badStateE, bsize, "getfield_or_static:is_badStateE");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateE, bsize, "getfield_or_static:is_badStateE");
  BTB_BEGIN(is_badStateF, bsize, "getfield_or_static:is_badStateF");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateF, bsize, "getfield_or_static:is_badStateF");

  __ align_address(64);
  BIND(is_badState);  // Do this outside branch table. Needs a lot of space.
  {
    unsigned int b_off = __ offset();
    if (is_static) {
      __ stop_static("Bad state in getstatic");
    } else {
      __ stop_static("Bad state in getfield");
    }
    unsigned int e_off = __ offset();
  }

  __ align_address(64);
  BIND(atosHandler);  // Oops are really complicated to handle.
                      // There is a lot of code generated.
                      // Therefore: generate the handler outside of branch table.
                      // There is no performance penalty. The additional branch
                      // to here is compensated for by the fallthru to "Done".
  {
    unsigned int b_off = __ offset();
    do_oop_load(_masm, field, Z_tos, Z_tmp_2, Z_tmp_3, IN_HEAP);
    __ verify_oop(Z_tos);
    __ push(atos);
    if (do_rewrite) {
      patch_bytecode(Bytecodes::_fast_agetfield, bc, Z_ARG5);
    }
    unsigned int e_off = __ offset();
  }

  BIND(Done);
}

void TemplateTable::getfield(int byte_no) {
  BLOCK_COMMENT("getfield  {");
  getfield_or_static(byte_no, false);
  BLOCK_COMMENT("} getfield");
}

void TemplateTable::nofast_getfield(int byte_no) {
  getfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::getstatic(int byte_no) {
  BLOCK_COMMENT("getstatic {");
  getfield_or_static(byte_no, true);
  BLOCK_COMMENT("} getstatic");
}

// The registers cache and index expected to be set before call.  The
// function may destroy various registers, just not the cache and
// index registers.
void TemplateTable::jvmti_post_field_mod(Register cache,
                                         Register index, bool is_static) {
  transition(vtos, vtos);

  if (!JvmtiExport::can_post_field_modification()) {
    return;
  }

  BLOCK_COMMENT("jvmti_post_field_mod {");

  // Check to see if a field modification watch has been set before
  // we take the time to call into the VM.
  Label    L1;
  ByteSize cp_base_offset = ConstantPoolCache::base_offset();
  assert_different_registers(cache, index, Z_tos);

  __ load_absolute_address(Z_tos, (address)JvmtiExport::get_field_modification_count_addr());
  __ load_and_test_int(Z_R0, Address(Z_tos));
  __ z_brz(L1);

  // Index is returned as byte offset, do not shift!
  __ get_cache_and_index_at_bcp(Z_ARG3, Z_R1_scratch, 1);

  if (is_static) {
    // Life is simple. Null out the object pointer.
    __ clear_reg(Z_ARG2, true, false);   // Don't set CC.
  } else {
    // Life is harder. The stack holds the value on top, followed by
    // the object. We don't know the size of the value, though. It
    // could be one or two words depending on its type. As a result,
    // we must find the type to determine where the object is.
    __ mem2reg_opt(Z_ARG4,
                   Address(Z_ARG3, Z_R1_scratch,
                           in_bytes(cp_base_offset + ConstantPoolCacheEntry::flags_offset()) +
                           (BytesPerLong - BytesPerInt)),
                   false);
    __ z_srl(Z_ARG4, ConstantPoolCacheEntry::tos_state_shift);
    // Make sure we don't need to mask Z_ARG4 for tos_state after the above shift.
    ConstantPoolCacheEntry::verify_tos_state_shift();
    __ mem2reg_opt(Z_ARG2, at_tos(1));  // Initially assume a one word jvalue.

    NearLabel   load_dtos, cont;

    __ compareU32_and_branch(Z_ARG4, (intptr_t) ltos,
                              Assembler::bcondNotEqual, load_dtos);
    __ mem2reg_opt(Z_ARG2, at_tos(2)); // ltos (two word jvalue)
    __ z_bru(cont);

    __ bind(load_dtos);
    __ compareU32_and_branch(Z_ARG4, (intptr_t)dtos, Assembler::bcondNotEqual, cont);
    __ mem2reg_opt(Z_ARG2, at_tos(2)); // dtos (two word jvalue)

    __ bind(cont);
  }
  // cache entry pointer

  __ add2reg_with_index(Z_ARG3, in_bytes(cp_base_offset), Z_ARG3, Z_R1_scratch);

  // object(tos)
  __ load_address(Z_ARG4, Address(Z_esp, Interpreter::stackElementSize));
  // Z_ARG2: object pointer set up above (NULL if static)
  // Z_ARG3: cache entry pointer
  // Z_ARG4: jvalue object on the stack
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_modification),
             Z_ARG2, Z_ARG3, Z_ARG4);
  __ get_cache_and_index_at_bcp(cache, index, 1);

  __ bind(L1);
  BLOCK_COMMENT("} jvmti_post_field_mod");
}


void TemplateTable::putfield_or_static(int byte_no, bool is_static, RewriteControl rc) {
  transition(vtos, vtos);

  const Register cache         = Z_tmp_1;
  const Register index         = Z_ARG5;
  const Register obj           = Z_tmp_1;
  const Register off           = Z_tmp_2;
  const Register flags         = Z_R1_scratch;
  const Register br_tab        = Z_ARG5;
  const Register bc            = Z_tmp_1;
  const Register oopStore_tmp1 = Z_R1_scratch;
  const Register oopStore_tmp2 = Z_ARG5;
  const Register oopStore_tmp3 = Z_R0_scratch;

  resolve_cache_and_index(byte_no, cache, index, sizeof(u2));
  jvmti_post_field_mod(cache, index, is_static);
  load_field_cp_cache_entry(obj, cache, index, off, flags, is_static);
  // begin of life for:
  //   obj, off   long life range
  //   flags      short life range, up to branch into branch table
  // end of life for:
  //   cache, index

  const Address field(obj, off);
  Label is_Byte, is_Bool, is_Int, is_Short, is_Char,
        is_Long, is_Float, is_Object, is_Double;
  Label is_badState8, is_badState9, is_badStateA, is_badStateB,
        is_badStateC, is_badStateD, is_badStateE, is_badStateF,
        is_badState;
  Label branchTable, atosHandler, Done;
  bool  do_rewrite   = !is_static && (rc == may_rewrite);
  bool  dont_rewrite = (is_static || (rc == may_not_rewrite));

  assert(do_rewrite == !dont_rewrite, "Oops, code is not fit for that");

  assert(btos == 0, "change code, btos != 0");

#ifdef ASSERT
  const unsigned int bsize = is_static ? BTB_MINSIZE*1 : BTB_MINSIZE*4;
#else
  const unsigned int bsize = is_static ? BTB_MINSIZE*1 : BTB_MINSIZE*8;
#endif

  // Calculate address of branch table entry and branch there.
  {
    const int bit_shift = exact_log2(bsize); // Size of each branch table entry.
    const int r_bitpos  = 63 - bit_shift;
    const int l_bitpos  = r_bitpos - ConstantPoolCacheEntry::tos_state_bits + 1;
    const int n_rotate  = (bit_shift-ConstantPoolCacheEntry::tos_state_shift);
    __ z_larl(br_tab, branchTable);
    __ rotate_then_insert(flags, flags, l_bitpos, r_bitpos, n_rotate, true);
    __ z_bc(Assembler::bcondAlways, 0, flags, br_tab);
  }
  // end of life for:
  //   flags, br_tab

  __ align_address(bsize);
  BIND(branchTable);

  // btos
  BTB_BEGIN(is_Byte, bsize, "putfield_or_static:is_Byte");
  __ pop(btos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ z_stc(Z_tos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_bputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Byte, bsize, "putfield_or_static:is_Byte");

  // ztos
  BTB_BEGIN(is_Bool, bsize, "putfield_or_static:is_Bool");
  __ pop(ztos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ z_nilf(Z_tos, 0x1);
  __ z_stc(Z_tos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_zputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END(is_Bool, bsize, "putfield_or_static:is_Bool");

  // ctos
  BTB_BEGIN(is_Char, bsize, "putfield_or_static:is_Char");
  __ pop(ctos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ z_sth(Z_tos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_cputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Char, bsize, "putfield_or_static:is_Char");

  // stos
  BTB_BEGIN(is_Short, bsize, "putfield_or_static:is_Short");
  __ pop(stos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ z_sth(Z_tos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_sputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Short, bsize, "putfield_or_static:is_Short");

  // itos
  BTB_BEGIN(is_Int, bsize, "putfield_or_static:is_Int");
  __ pop(itos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ reg2mem_opt(Z_tos, field, false);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_iputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Int, bsize, "putfield_or_static:is_Int");

  // ltos
  BTB_BEGIN(is_Long, bsize, "putfield_or_static:is_Long");
  __ pop(ltos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ reg2mem_opt(Z_tos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_lputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Long, bsize, "putfield_or_static:is_Long");

  // ftos
  BTB_BEGIN(is_Float, bsize, "putfield_or_static:is_Float");
  __ pop(ftos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ freg2mem_opt(Z_ftos, field, false);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_fputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Float, bsize, "putfield_or_static:is_Float");

  // dtos
  BTB_BEGIN(is_Double, bsize, "putfield_or_static:is_Double");
  __ pop(dtos);
  if (!is_static) {
    pop_and_check_object(obj);
  }
  __ freg2mem_opt(Z_ftos, field);
  if (do_rewrite) {
    patch_bytecode(Bytecodes::_fast_dputfield, bc, Z_ARG5, true, byte_no);
  }
  __ z_bru(Done);
  BTB_END( is_Double, bsize, "putfield_or_static:is_Double");

  // atos
  BTB_BEGIN(is_Object, bsize, "putfield_or_static:is_Object");
  __ z_bru(atosHandler);
  BTB_END( is_Object, bsize, "putfield_or_static:is_Object");

  // Bad state detection comes at no extra runtime cost.
  BTB_BEGIN(is_badState8, bsize, "putfield_or_static:is_badState8");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badState8, bsize, "putfield_or_static:is_badState8");
  BTB_BEGIN(is_badState9, bsize, "putfield_or_static:is_badState9");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badState9, bsize, "putfield_or_static:is_badState9");
  BTB_BEGIN(is_badStateA, bsize, "putfield_or_static:is_badStateA");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateA, bsize, "putfield_or_static:is_badStateA");
  BTB_BEGIN(is_badStateB, bsize, "putfield_or_static:is_badStateB");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateB, bsize, "putfield_or_static:is_badStateB");
  BTB_BEGIN(is_badStateC, bsize, "putfield_or_static:is_badStateC");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateC, bsize, "putfield_or_static:is_badStateC");
  BTB_BEGIN(is_badStateD, bsize, "putfield_or_static:is_badStateD");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateD, bsize, "putfield_or_static:is_badStateD");
  BTB_BEGIN(is_badStateE, bsize, "putfield_or_static:is_badStateE");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateE, bsize, "putfield_or_static:is_badStateE");
  BTB_BEGIN(is_badStateF, bsize, "putfield_or_static:is_badStateF");
  __ z_illtrap();
  __ z_bru(is_badState);
  BTB_END( is_badStateF, bsize, "putfield_or_static:is_badStateF");

  __ align_address(64);
  BIND(is_badState);  // Do this outside branch table. Needs a lot of space.
  {
    unsigned int b_off = __ offset();
    if (is_static) __ stop_static("Bad state in putstatic");
    else            __ stop_static("Bad state in putfield");
    unsigned int e_off = __ offset();
  }

  __ align_address(64);
  BIND(atosHandler);  // Oops are really complicated to handle.
                      // There is a lot of code generated.
                      // Therefore: generate the handler outside of branch table.
                      // There is no performance penalty. The additional branch
                      // to here is compensated for by the fallthru to "Done".
  {
    unsigned int b_off = __ offset();
    __ pop(atos);
    if (!is_static) {
      pop_and_check_object(obj);
    }
    // Store into the field
    do_oop_store(_masm, Address(obj, off), Z_tos,
                 oopStore_tmp1, oopStore_tmp2, oopStore_tmp3, IN_HEAP);
    if (do_rewrite) {
      patch_bytecode(Bytecodes::_fast_aputfield, bc, Z_ARG5, true, byte_no);
    }
    // __ z_bru(Done); // fallthru
    unsigned int e_off = __ offset();
  }

  BIND(Done);

  // Check for volatile store.
  Label notVolatile;

  __ testbit(Z_ARG4, ConstantPoolCacheEntry::is_volatile_shift);
  __ z_brz(notVolatile);
  __ z_fence();

  BIND(notVolatile);
}

void TemplateTable::putfield(int byte_no) {
  BLOCK_COMMENT("putfield  {");
  putfield_or_static(byte_no, false);
  BLOCK_COMMENT("} putfield");
}

void TemplateTable::nofast_putfield(int byte_no) {
  putfield_or_static(byte_no, false, may_not_rewrite);
}

void TemplateTable::putstatic(int byte_no) {
  BLOCK_COMMENT("putstatic {");
  putfield_or_static(byte_no, true);
  BLOCK_COMMENT("} putstatic");
}

// Push the tos value back to the stack.
// gc will find oops there and update.
void TemplateTable::jvmti_post_fast_field_mod() {

  if (!JvmtiExport::can_post_field_modification()) {
    return;
  }

  // Check to see if a field modification watch has been set before
  // we take the time to call into the VM.
  Label   exit;

  BLOCK_COMMENT("jvmti_post_fast_field_mod {");

  __ load_absolute_address(Z_R1_scratch,
                           (address) JvmtiExport::get_field_modification_count_addr());
  __ load_and_test_int(Z_R0_scratch, Address(Z_R1_scratch));
  __ z_brz(exit);

  Register obj = Z_tmp_1;

  __ pop_ptr(obj);                  // Copy the object pointer from tos.
  __ verify_oop(obj);
  __ push_ptr(obj);                 // Put the object pointer back on tos.

  // Save tos values before call_VM() clobbers them. Since we have
  // to do it for every data type, we use the saved values as the
  // jvalue object.
  switch (bytecode()) {          // Load values into the jvalue object.
    case Bytecodes::_fast_aputfield:
      __ push_ptr(Z_tos);
      break;
    case Bytecodes::_fast_bputfield:
    case Bytecodes::_fast_zputfield:
    case Bytecodes::_fast_sputfield:
    case Bytecodes::_fast_cputfield:
    case Bytecodes::_fast_iputfield:
      __ push_i(Z_tos);
      break;
    case Bytecodes::_fast_dputfield:
      __ push_d();
      break;
    case Bytecodes::_fast_fputfield:
      __ push_f();
      break;
    case Bytecodes::_fast_lputfield:
      __ push_l(Z_tos);
      break;

    default:
      ShouldNotReachHere();
  }

  // jvalue on the stack
  __ load_address(Z_ARG4, Address(Z_esp, Interpreter::stackElementSize));
  // Access constant pool cache entry.
  __ get_cache_entry_pointer_at_bcp(Z_ARG3, Z_tos, 1);
  __ verify_oop(obj);

  // obj   : object pointer copied above
  // Z_ARG3: cache entry pointer
  // Z_ARG4: jvalue object on the stack
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_modification),
             obj, Z_ARG3, Z_ARG4);

  switch (bytecode()) {             // Restore tos values.
    case Bytecodes::_fast_aputfield:
      __ pop_ptr(Z_tos);
      break;
    case Bytecodes::_fast_bputfield:
    case Bytecodes::_fast_zputfield:
    case Bytecodes::_fast_sputfield:
    case Bytecodes::_fast_cputfield:
    case Bytecodes::_fast_iputfield:
      __ pop_i(Z_tos);
      break;
    case Bytecodes::_fast_dputfield:
      __ pop_d(Z_ftos);
      break;
    case Bytecodes::_fast_fputfield:
      __ pop_f(Z_ftos);
      break;
    case Bytecodes::_fast_lputfield:
      __ pop_l(Z_tos);
      break;
    default:
      break;
  }

  __ bind(exit);
  BLOCK_COMMENT("} jvmti_post_fast_field_mod");
}

void TemplateTable::fast_storefield(TosState state) {
  transition(state, vtos);

  ByteSize base = ConstantPoolCache::base_offset();
  jvmti_post_fast_field_mod();

  // Access constant pool cache.
  Register cache = Z_tmp_1;
  Register index = Z_tmp_2;
  Register flags = Z_ARG5;

  // Index comes in bytes, don't shift afterwards!
  __ get_cache_and_index_at_bcp(cache, index, 1);

  // Test for volatile.
  assert(!flags->is_volatile(), "do_oop_store could perform leaf RT call");
  __ z_lg(flags, Address(cache, index, base + ConstantPoolCacheEntry::flags_offset()));

  // Replace index with field offset from cache entry.
  Register field_offset = index;
  __ z_lg(field_offset, Address(cache, index, base + ConstantPoolCacheEntry::f2_offset()));

  // Get object from stack.
  Register   obj = cache;

  pop_and_check_object(obj);

  // field address
  const Address   field(obj, field_offset);

  // access field
  switch (bytecode()) {
    case Bytecodes::_fast_aputfield:
      do_oop_store(_masm, Address(obj, field_offset), Z_tos,
                   Z_ARG2, Z_ARG3, Z_ARG4, IN_HEAP);
      break;
    case Bytecodes::_fast_lputfield:
      __ reg2mem_opt(Z_tos, field);
      break;
    case Bytecodes::_fast_iputfield:
      __ reg2mem_opt(Z_tos, field, false);
      break;
    case Bytecodes::_fast_zputfield:
      __ z_nilf(Z_tos, 0x1);
      // fall through to bputfield
    case Bytecodes::_fast_bputfield:
      __ z_stc(Z_tos, field);
      break;
    case Bytecodes::_fast_sputfield:
      // fall through
    case Bytecodes::_fast_cputfield:
      __ z_sth(Z_tos, field);
      break;
    case Bytecodes::_fast_fputfield:
      __ freg2mem_opt(Z_ftos, field, false);
      break;
    case Bytecodes::_fast_dputfield:
      __ freg2mem_opt(Z_ftos, field);
      break;
    default:
      ShouldNotReachHere();
  }

  //  Check for volatile store.
  Label notVolatile;

  __ testbit(flags, ConstantPoolCacheEntry::is_volatile_shift);
  __ z_brz(notVolatile);
  __ z_fence();

  __ bind(notVolatile);
}

void TemplateTable::fast_accessfield(TosState state) {
  transition(atos, state);

  Register obj = Z_tos;

  // Do the JVMTI work here to avoid disturbing the register state below
  if (JvmtiExport::can_post_field_access()) {
    // Check to see if a field access watch has been set before we
    // take the time to call into the VM.
    Label cont;

    __ load_absolute_address(Z_R1_scratch,
                             (address)JvmtiExport::get_field_access_count_addr());
    __ load_and_test_int(Z_R0_scratch, Address(Z_R1_scratch));
    __ z_brz(cont);

    // Access constant pool cache entry.

    __ get_cache_entry_pointer_at_bcp(Z_ARG3, Z_tmp_1, 1);
    __ verify_oop(obj);
    __ push_ptr(obj);  // Save object pointer before call_VM() clobbers it.
    __ z_lgr(Z_ARG2, obj);

    // Z_ARG2: object pointer copied above
    // Z_ARG3: cache entry pointer
    __ call_VM(noreg,
               CAST_FROM_FN_PTR(address, InterpreterRuntime::post_field_access),
               Z_ARG2, Z_ARG3);
    __ pop_ptr(obj); // Restore object pointer.

    __ bind(cont);
  }

  // Access constant pool cache.
  Register   cache = Z_tmp_1;
  Register   index = Z_tmp_2;

  // Index comes in bytes, don't shift afterwards!
  __ get_cache_and_index_at_bcp(cache, index, 1);
  // Replace index with field offset from cache entry.
  __ mem2reg_opt(index,
                 Address(cache, index,
                         ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::f2_offset()));

  __ verify_oop(obj);
  __ null_check(obj);

  Address field(obj, index);

  // access field
  switch (bytecode()) {
    case Bytecodes::_fast_agetfield:
      do_oop_load(_masm, field, Z_tos, Z_tmp_1, Z_tmp_2, IN_HEAP);
      __ verify_oop(Z_tos);
      return;
    case Bytecodes::_fast_lgetfield:
      __ mem2reg_opt(Z_tos, field);
      return;
    case Bytecodes::_fast_igetfield:
      __ mem2reg_opt(Z_tos, field, false);
      return;
    case Bytecodes::_fast_bgetfield:
      __ z_lb(Z_tos, field);
      return;
    case Bytecodes::_fast_sgetfield:
      __ z_lh(Z_tos, field);
      return;
    case Bytecodes::_fast_cgetfield:
      __ z_llgh(Z_tos, field);   // Load into 64 bits, works on all CPUs.
      return;
    case Bytecodes::_fast_fgetfield:
      __ mem2freg_opt(Z_ftos, field, false);
      return;
    case Bytecodes::_fast_dgetfield:
      __ mem2freg_opt(Z_ftos, field);
      return;
    default:
      ShouldNotReachHere();
  }
}

void TemplateTable::fast_xaccess(TosState state) {
  transition(vtos, state);

  Register receiver = Z_tos;
  // Get receiver.
  __ mem2reg_opt(Z_tos, aaddress(0));

  // Access constant pool cache.
  Register cache = Z_tmp_1;
  Register index = Z_tmp_2;

  // Index comes in bytes, don't shift afterwards!
  __ get_cache_and_index_at_bcp(cache, index, 2);
  // Replace index with field offset from cache entry.
  __ mem2reg_opt(index,
                 Address(cache, index,
                         ConstantPoolCache::base_offset() + ConstantPoolCacheEntry::f2_offset()));

  // Make sure exception is reported in correct bcp range (getfield is
  // next instruction).
  __ add2reg(Z_bcp, 1);
  __ null_check(receiver);
  switch (state) {
    case itos:
      __ mem2reg_opt(Z_tos, Address(receiver, index), false);
      break;
    case atos:
      do_oop_load(_masm, Address(receiver, index), Z_tos, Z_tmp_1, Z_tmp_2, IN_HEAP);
      __ verify_oop(Z_tos);
      break;
    case ftos:
      __ mem2freg_opt(Z_ftos, Address(receiver, index));
      break;
    default:
      ShouldNotReachHere();
  }

  // Reset bcp to original position.
  __ add2reg(Z_bcp, -1);
}

//-----------------------------------------------------------------------------
// Calls

void TemplateTable::prepare_invoke(int byte_no,
                                   Register method,  // linked method (or i-klass)
                                   Register index,   // itable index, MethodType, etc.
                                   Register recv,    // If caller wants to see it.
                                   Register flags) { // If caller wants to test it.
  // Determine flags.
  const Bytecodes::Code code = bytecode();
  const bool is_invokeinterface  = code == Bytecodes::_invokeinterface;
  const bool is_invokedynamic    = code == Bytecodes::_invokedynamic;
  const bool is_invokehandle     = code == Bytecodes::_invokehandle;
  const bool is_invokevirtual    = code == Bytecodes::_invokevirtual;
  const bool is_invokespecial    = code == Bytecodes::_invokespecial;
  const bool load_receiver       = (recv != noreg);
  assert(load_receiver == (code != Bytecodes::_invokestatic && code != Bytecodes::_invokedynamic), "");

  // Setup registers & access constant pool cache.
  if (recv  == noreg) { recv  = Z_ARG1; }
  if (flags == noreg) { flags = Z_ARG2; }
  assert_different_registers(method, Z_R14, index, recv, flags);

  BLOCK_COMMENT("prepare_invoke {");

  load_invoke_cp_cache_entry(byte_no, method, index, flags, is_invokevirtual, false, is_invokedynamic);

  // Maybe push appendix to arguments.
  if (is_invokedynamic || is_invokehandle) {
    Label L_no_push;
    Register resolved_reference = Z_R1_scratch;
    __ testbit(flags, ConstantPoolCacheEntry::has_appendix_shift);
    __ z_bfalse(L_no_push);
    // Push the appendix as a trailing parameter.
    // This must be done before we get the receiver,
    // since the parameter_size includes it.
    __ load_resolved_reference_at_index(resolved_reference, index);
    __ verify_oop(resolved_reference);
    __ push_ptr(resolved_reference);  // Push appendix (MethodType, CallSite, etc.).
    __ bind(L_no_push);
  }

  // Load receiver if needed (after appendix is pushed so parameter size is correct).
  if (load_receiver) {
    assert(!is_invokedynamic, "");
    // recv := int2long(flags & ConstantPoolCacheEntry::parameter_size_mask) << 3
    // Flags is zero-extended int2long when loaded during load_invoke_cp_cache_entry().
    // Only the least significant byte (psize) of flags is used.
    {
      const unsigned int logSES = Interpreter::logStackElementSize;
      const int bit_shift = logSES;
      const int r_bitpos  = 63 - bit_shift;
      const int l_bitpos  = r_bitpos - ConstantPoolCacheEntry::parameter_size_bits + 1;
      const int n_rotate  = bit_shift;
      assert(ConstantPoolCacheEntry::parameter_size_mask == 255, "adapt bitpositions");
      __ rotate_then_insert(recv, flags, l_bitpos, r_bitpos, n_rotate, true);
    }
    // Recv now contains #arguments * StackElementSize.

    Address recv_addr(Z_esp, recv);
    __ z_lg(recv, recv_addr);
    __ verify_oop(recv);
  }

  // Compute return type.
  // ret_type is used by callers (invokespecial, invokestatic) at least.
  Register ret_type = Z_R1_scratch;
  assert_different_registers(ret_type, method);

  const address table_addr = (address)Interpreter::invoke_return_entry_table_for(code);
  __ load_absolute_address(Z_R14, table_addr);

  {
    const int bit_shift = LogBytesPerWord;           // Size of each table entry.
    const int r_bitpos  = 63 - bit_shift;
    const int l_bitpos  = r_bitpos - ConstantPoolCacheEntry::tos_state_bits + 1;
    const int n_rotate  = bit_shift-ConstantPoolCacheEntry::tos_state_shift;
    __ rotate_then_insert(ret_type, flags, l_bitpos, r_bitpos, n_rotate, true);
    // Make sure we don't need to mask flags for tos_state after the above shift.
    ConstantPoolCacheEntry::verify_tos_state_shift();
  }

    __ z_lg(Z_R14, Address(Z_R14, ret_type)); // Load return address.
  BLOCK_COMMENT("} prepare_invoke");
}


void TemplateTable::invokevirtual_helper(Register index,
                                         Register recv,
                                         Register flags) {
  // Uses temporary registers Z_tmp_2, Z_ARG4.
  assert_different_registers(index, recv, Z_tmp_2, Z_ARG4);

  // Test for an invoke of a final method.
  Label notFinal;

  BLOCK_COMMENT("invokevirtual_helper {");

  __ testbit(flags, ConstantPoolCacheEntry::is_vfinal_shift);
  __ z_brz(notFinal);

  const Register method = index;  // Method must be Z_ARG3.
  assert(method == Z_ARG3, "method must be second argument for interpreter calling convention");

  // Do the call - the index is actually the method to call.
  // That is, f2 is a vtable index if !is_vfinal, else f2 is a method.

  // It's final, need a null check here!
  __ null_check(recv);

  // Profile this call.
  __ profile_final_call(Z_tmp_2);
  __ profile_arguments_type(Z_tmp_2, method, Z_ARG5, true); // Argument type profiling.
  __ jump_from_interpreted(method, Z_tmp_2);

  __ bind(notFinal);

  // Get receiver klass.
  __ null_check(recv, Z_R0_scratch, oopDesc::klass_offset_in_bytes());
  __ load_klass(Z_tmp_2, recv);

  // Profile this call.
  __ profile_virtual_call(Z_tmp_2, Z_ARG4, Z_ARG5);

  // Get target method & entry point.
  __ z_sllg(index, index, exact_log2(vtableEntry::size_in_bytes()));
  __ mem2reg_opt(method,
                 Address(Z_tmp_2, index,
                         Klass::vtable_start_offset() + in_ByteSize(vtableEntry::method_offset_in_bytes())));
  __ profile_arguments_type(Z_ARG4, method, Z_ARG5, true);
  __ jump_from_interpreted(method, Z_ARG4);
  BLOCK_COMMENT("} invokevirtual_helper");
}

void TemplateTable::invokevirtual(int byte_no) {
  transition(vtos, vtos);

  assert(byte_no == f2_byte, "use this argument");
  prepare_invoke(byte_no,
                 Z_ARG3,  // method or vtable index
                 noreg,   // unused itable index
                 Z_ARG1,  // recv
                 Z_ARG2); // flags

  // Z_ARG3 : index
  // Z_ARG1 : receiver
  // Z_ARG2 : flags
  invokevirtual_helper(Z_ARG3, Z_ARG1, Z_ARG2);
}

void TemplateTable::invokespecial(int byte_no) {
  transition(vtos, vtos);

  assert(byte_no == f1_byte, "use this argument");
  Register Rmethod = Z_tmp_2;
  prepare_invoke(byte_no, Rmethod, noreg, // Get f1 method.
                 Z_ARG3);   // Get receiver also for null check.
  __ verify_oop(Z_ARG3);
  __ null_check(Z_ARG3);
  // Do the call.
  __ profile_call(Z_ARG2);
  __ profile_arguments_type(Z_ARG2, Rmethod, Z_ARG5, false);
  __ jump_from_interpreted(Rmethod, Z_R1_scratch);
}

void TemplateTable::invokestatic(int byte_no) {
  transition(vtos, vtos);

  assert(byte_no == f1_byte, "use this argument");
  Register Rmethod = Z_tmp_2;
  prepare_invoke(byte_no, Rmethod);   // Get f1 method.
  // Do the call.
  __ profile_call(Z_ARG2);
  __ profile_arguments_type(Z_ARG2, Rmethod, Z_ARG5, false);
  __ jump_from_interpreted(Rmethod, Z_R1_scratch);
}

// Outdated feature, and we don't support it.
void TemplateTable::fast_invokevfinal(int byte_no) {
  transition(vtos, vtos);
  assert(byte_no == f2_byte, "use this argument");
  __ stop("fast_invokevfinal not used on linuxs390x");
}

void TemplateTable::invokeinterface(int byte_no) {
  transition(vtos, vtos);

  assert(byte_no == f1_byte, "use this argument");
  Register klass     = Z_ARG2,
           method    = Z_ARG3,
           interface = Z_ARG4,
           flags     = Z_ARG5,
           receiver  = Z_tmp_1;

  BLOCK_COMMENT("invokeinterface {");

  prepare_invoke(byte_no, interface, method,  // Get f1 klassOop, f2 Method*.
                 receiver, flags);

  // Z_R14 (== Z_bytecode) : return entry

  // First check for Object case, then private interface method,
  // then regular interface method.

  // Special case of invokeinterface called for virtual method of
  // java.lang.Object. See cpCache.cpp for details.
  NearLabel notObjectMethod, no_such_method;
  __ testbit(flags, ConstantPoolCacheEntry::is_forced_virtual_shift);
  __ z_brz(notObjectMethod);
  invokevirtual_helper(method, receiver, flags);
  __ bind(notObjectMethod);

  // Check for private method invocation - indicated by vfinal
  NearLabel notVFinal;
  __ testbit(flags, ConstantPoolCacheEntry::is_vfinal_shift);
  __ z_brz(notVFinal);

  // Get receiver klass into klass - also a null check.
  __ load_klass(klass, receiver);

  NearLabel subtype, no_such_interface;

  __ check_klass_subtype(klass, interface, Z_tmp_2, flags/*scratch*/, subtype);
  // If we get here the typecheck failed
  __ z_bru(no_such_interface);
  __ bind(subtype);

  // do the call
  __ profile_final_call(Z_tmp_2);
  __ profile_arguments_type(Z_tmp_2, method, Z_ARG5, true);
  __ jump_from_interpreted(method, Z_tmp_2);

  __ bind(notVFinal);

  // Get receiver klass into klass - also a null check.
  __ load_klass(klass, receiver);

  __ lookup_interface_method(klass, interface, noreg, noreg, /*temp*/Z_ARG1,
                             no_such_interface, /*return_method=*/false);

  // Profile this call.
  __ profile_virtual_call(klass, Z_ARG1/*mdp*/, flags/*scratch*/);

  // Find entry point to call.

  // Get declaring interface class from method
  __ load_method_holder(interface, method);

  // Get itable index from method
  Register index   = receiver,
           method2 = flags;
  __ z_lgf(index, Address(method, Method::itable_index_offset()));
  __ z_aghi(index, -Method::itable_index_max);
  __ z_lcgr(index, index);

  __ lookup_interface_method(klass, interface, index, method2, Z_tmp_2,
                             no_such_interface);

  // Check for abstract method error.
  // Note: This should be done more efficiently via a throw_abstract_method_error
  // interpreter entry point and a conditional jump to it in case of a null
  // method.
  __ compareU64_and_branch(method2, (intptr_t) 0,
                           Assembler::bcondZero, no_such_method);

  __ profile_arguments_type(Z_tmp_1, method2, Z_tmp_2, true);

  // Do the call.
  __ jump_from_interpreted(method2, Z_tmp_2);
  __ should_not_reach_here();

  // exception handling code follows...
  // Note: Must restore interpreter registers to canonical
  // state for exception handling to work correctly!

  __ bind(no_such_method);

  // Throw exception.
  // Pass arguments for generating a verbose error message.
  __ z_lgr(Z_tmp_1, method); // Prevent register clash.
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::throw_AbstractMethodErrorVerbose),
                              klass, Z_tmp_1);
  // The call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();

  __ bind(no_such_interface);

  // Throw exception.
  // Pass arguments for generating a verbose error message.
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address,
                              InterpreterRuntime::throw_IncompatibleClassChangeErrorVerbose),
                              klass, interface);
  // The call_VM checks for exception, so we should never return here.
  __ should_not_reach_here();

  BLOCK_COMMENT("} invokeinterface");
  return;
}

void TemplateTable::invokehandle(int byte_no) {
  transition(vtos, vtos);

  const Register method = Z_tmp_2;
  const Register recv   = Z_ARG5;
  const Register mtype  = Z_tmp_1;
  prepare_invoke(byte_no,
                 method, mtype,   // Get f2 method, f1 MethodType.
                 recv);
  __ verify_method_ptr(method);
  __ verify_oop(recv);
  __ null_check(recv);

  // Note: Mtype is already pushed (if necessary) by prepare_invoke.

  // FIXME: profile the LambdaForm also.
  __ profile_final_call(Z_ARG2);
  __ profile_arguments_type(Z_ARG3, method, Z_ARG5, true);

  __ jump_from_interpreted(method, Z_ARG3);
}

void TemplateTable::invokedynamic(int byte_no) {
  transition(vtos, vtos);

  const Register Rmethod   = Z_tmp_2;
  const Register Rcallsite = Z_tmp_1;

  prepare_invoke(byte_no, Rmethod, Rcallsite);

  // Rmethod: CallSite object (from f1)
  // Rcallsite: MH.linkToCallSite method (from f2)

  // Note: Callsite is already pushed by prepare_invoke.

  // TODO: should make a type profile for any invokedynamic that takes a ref argument.
  // Profile this call.
  __ profile_call(Z_ARG2);
  __ profile_arguments_type(Z_ARG2, Rmethod, Z_ARG5, false);
  __ jump_from_interpreted(Rmethod, Z_ARG2);
}

//-----------------------------------------------------------------------------
// Allocation

// Original comment on "allow_shared_alloc":
// Always go the slow path.
//  + Eliminated optimization within the template-based interpreter:
//    If an allocation is done within the interpreter without using
//    tlabs, the interpreter tries to do the allocation directly
//    on the heap.
//  + That means the profiling hooks are not considered and allocations
//    get lost for the profiling framework.
//  + However, we do not think that this optimization is really needed,
//    so we always go now the slow path through the VM in this case --
//    spec jbb2005 shows no measurable performance degradation.
void TemplateTable::_new() {
  transition(vtos, atos);
  address prev_instr_address = NULL;
  Register tags  = Z_tmp_1;
  Register RallocatedObject   = Z_tos;
  Register cpool = Z_ARG2;
  Register tmp = Z_ARG3; // RobjectFields==tmp and Rsize==offset must be a register pair.
  Register offset = Z_ARG4;
  Label slow_case;
  Label done;
  Label initialize_header;

  BLOCK_COMMENT("TemplateTable::_new {");
  __ get_2_byte_integer_at_bcp(offset/*dest*/, 1, InterpreterMacroAssembler::Unsigned);
  __ get_cpool_and_tags(cpool, tags);
  // Make sure the class we're about to instantiate has been resolved.
  // This is done before loading InstanceKlass to be consistent with the order
  // how Constant Pool is updated (see ConstantPool::klass_at_put).
  const int tags_offset = Array<u1>::base_offset_in_bytes();
  __ load_address(tmp, Address(tags, offset, tags_offset));
  __ z_cli(0, tmp, JVM_CONSTANT_Class);
  __ z_brne(slow_case);

  __ z_sllg(offset, offset, LogBytesPerWord); // Convert to to offset.
  // Get InstanceKlass.
  Register iklass = cpool;
  __ load_resolved_klass_at_offset(cpool, offset, iklass);

  // Make sure klass is initialized & doesn't have finalizer.
  // Make sure klass is fully initialized.
  const int state_offset = in_bytes(InstanceKlass::init_state_offset());
  if (Immediate::is_uimm12(state_offset)) {
    __ z_cli(state_offset, iklass, InstanceKlass::fully_initialized);
  } else {
    __ z_cliy(state_offset, iklass, InstanceKlass::fully_initialized);
  }
  __ z_brne(slow_case);

  // Get instance_size in InstanceKlass (scaled to a count of bytes).
  Register Rsize = offset;
  __ z_llgf(Rsize, Address(iklass, Klass::layout_helper_offset()));
  __ z_tmll(Rsize, Klass::_lh_instance_slow_path_bit);
  __ z_btrue(slow_case);

  // Allocate the instance
  // 1) Try to allocate in the TLAB.
  // 2) If the above fails (or is not applicable), go to a slow case
  // (creates a new TLAB, etc.).
  // Note: compared to other architectures, s390's implementation always goes
  // to the slow path if TLAB is used and fails.
  if (UseTLAB) {
    Register RoldTopValue = RallocatedObject;
    Register RnewTopValue = tmp;
    __ z_lg(RoldTopValue, Address(Z_thread, JavaThread::tlab_top_offset()));
    __ load_address(RnewTopValue, Address(RoldTopValue, Rsize));
    __ z_cg(RnewTopValue, Address(Z_thread, JavaThread::tlab_end_offset()));
    __ z_brh(slow_case);
    __ z_stg(RnewTopValue, Address(Z_thread, JavaThread::tlab_top_offset()));

    Register RobjectFields = tmp;
    Register Rzero = Z_R1_scratch;
    __ clear_reg(Rzero, true /*whole reg*/, false); // Load 0L into Rzero. Don't set CC.

    if (!ZeroTLAB) {
      // The object is initialized before the header. If the object size is
      // zero, go directly to the header initialization.
      __ z_aghi(Rsize, (int)-sizeof(oopDesc)); // Subtract header size, set CC.
      __ z_bre(initialize_header);             // Jump if size of fields is zero.

      // Initialize object fields.
      // See documentation for MVCLE instruction!!!
      assert(RobjectFields->encoding() % 2 == 0, "RobjectFields must be an even register");
      assert(Rsize->encoding() == (RobjectFields->encoding()+1),
             "RobjectFields and Rsize must be a register pair");
      assert(Rzero->encoding() % 2 == 1, "Rzero must be an odd register");

      // Set Rzero to 0 and use it as src length, then mvcle will copy nothing
      // and fill the object with the padding value 0.
      __ add2reg(RobjectFields, sizeof(oopDesc), RallocatedObject);
      __ move_long_ext(RobjectFields, as_Register(Rzero->encoding() - 1), 0);
    }

    // Initialize object header only.
    __ bind(initialize_header);
    __ store_const(Address(RallocatedObject, oopDesc::mark_offset_in_bytes()),
                   (long)markWord::prototype().value());

    __ store_klass_gap(Rzero, RallocatedObject);  // Zero klass gap for compressed oops.
    __ store_klass(iklass, RallocatedObject);     // Store klass last.

    {
      SkipIfEqual skip(_masm, &DTraceAllocProbes, false, Z_ARG5 /*scratch*/);
      // Trigger dtrace event for fastpath.
      __ push(atos); // Save the return value.
      __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_object_alloc), RallocatedObject);
      __ pop(atos); // Restore the return value.
    }
    __ z_bru(done);
  }

  // slow case
  __ bind(slow_case);
  __ get_constant_pool(Z_ARG2);
  __ get_2_byte_integer_at_bcp(Z_ARG3/*dest*/, 1, InterpreterMacroAssembler::Unsigned);
  call_VM(Z_tos, CAST_FROM_FN_PTR(address, InterpreterRuntime::_new), Z_ARG2, Z_ARG3);
  __ verify_oop(Z_tos);

  // continue
  __ bind(done);

  BLOCK_COMMENT("} TemplateTable::_new");
}

void TemplateTable::newarray() {
  transition(itos, atos);

  // Call runtime.
  __ z_llgc(Z_ARG2, at_bcp(1));   // type
  __ z_lgfr(Z_ARG3, Z_tos);       // size
  call_VM(Z_RET,
          CAST_FROM_FN_PTR(address, InterpreterRuntime::newarray),
          Z_ARG2, Z_ARG3);
}

void TemplateTable::anewarray() {
  transition(itos, atos);
  __ get_2_byte_integer_at_bcp(Z_ARG3, 1, InterpreterMacroAssembler::Unsigned);
  __ get_constant_pool(Z_ARG2);
  __ z_lgfr(Z_ARG4, Z_tos);
  call_VM(Z_tos, CAST_FROM_FN_PTR(address, InterpreterRuntime::anewarray),
          Z_ARG2, Z_ARG3, Z_ARG4);
}

void TemplateTable::arraylength() {
  transition(atos, itos);

  int offset = arrayOopDesc::length_offset_in_bytes();

  __ null_check(Z_tos, Z_R0_scratch, offset);
  __ mem2reg_opt(Z_tos, Address(Z_tos, offset), false);
}

void TemplateTable::checkcast() {
  transition(atos, atos);

  NearLabel done, is_null, ok_is_subtype, quicked, resolved;

  BLOCK_COMMENT("checkcast {");
  // If object is NULL, we are almost done.
  __ compareU64_and_branch(Z_tos, (intptr_t) 0, Assembler::bcondZero, is_null);

  // Get cpool & tags index.
  Register cpool = Z_tmp_1;
  Register tags = Z_tmp_2;
  Register index = Z_ARG5;

  __ get_cpool_and_tags(cpool, tags);
  __ get_2_byte_integer_at_bcp(index, 1, InterpreterMacroAssembler::Unsigned);
  // See if bytecode has already been quicked.
  // Note: For CLI, we would have to add the index to the tags pointer first,
  // thus load and compare in a "classic" manner.
  __ z_llgc(Z_R0_scratch,
            Address(tags, index, Array<u1>::base_offset_in_bytes()));
  __ compareU64_and_branch(Z_R0_scratch, JVM_CONSTANT_Class,
                           Assembler::bcondEqual, quicked);

  __ push(atos); // Save receiver for result, and for GC.
  call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  __ get_vm_result_2(Z_tos);

  Register   receiver = Z_ARG4;
  Register   klass = Z_tos;
  Register   subklass = Z_ARG5;

  __ pop_ptr(receiver); // restore receiver
  __ z_bru(resolved);

  // Get superklass in klass and subklass in subklass.
  __ bind(quicked);

  __ z_lgr(Z_ARG4, Z_tos);  // Save receiver.
  __ z_sllg(index, index, LogBytesPerWord);  // index2bytes for addressing
  __ load_resolved_klass_at_offset(cpool, index, klass);

  __ bind(resolved);

  __ load_klass(subklass, receiver);

  // Generate subtype check. Object in receiver.
  // Superklass in klass. Subklass in subklass.
  __ gen_subtype_check(subklass, klass, Z_ARG3, Z_tmp_1, ok_is_subtype);

  // Come here on failure.
  __ push_ptr(receiver);
  // Object is at TOS, target klass oop expected in rax by convention.
  __ z_brul((address) Interpreter::_throw_ClassCastException_entry);

  // Come here on success.
  __ bind(ok_is_subtype);

  __ z_lgr(Z_tos, receiver); // Restore object.

  // Collect counts on whether this test sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ z_bru(done);
    __ bind(is_null);
    __ profile_null_seen(Z_tmp_1);
  } else {
    __ bind(is_null);   // Same as 'done'.
  }

  __ bind(done);
  BLOCK_COMMENT("} checkcast");
}

void TemplateTable::instanceof() {
  transition(atos, itos);

  NearLabel done, is_null, ok_is_subtype, quicked, resolved;

  BLOCK_COMMENT("instanceof {");
  // If object is NULL, we are almost done.
  __ compareU64_and_branch(Z_tos, (intptr_t) 0, Assembler::bcondZero, is_null);

  // Get cpool & tags index.
  Register cpool = Z_tmp_1;
  Register tags = Z_tmp_2;
  Register index = Z_ARG5;

  __ get_cpool_and_tags(cpool, tags);
  __ get_2_byte_integer_at_bcp(index, 1, InterpreterMacroAssembler::Unsigned);
  // See if bytecode has already been quicked.
  // Note: For CLI, we would have to add the index to the tags pointer first,
  // thus load and compare in a "classic" manner.
  __ z_llgc(Z_R0_scratch,
            Address(tags, index, Array<u1>::base_offset_in_bytes()));
  __ compareU64_and_branch(Z_R0_scratch, JVM_CONSTANT_Class, Assembler::bcondEqual, quicked);

  __ push(atos); // Save receiver for result, and for GC.
  call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::quicken_io_cc));
  __ get_vm_result_2(Z_tos);

  Register receiver = Z_tmp_2;
  Register klass = Z_tos;
  Register subklass = Z_tmp_2;

  __ pop_ptr(receiver); // Restore receiver.
  __ verify_oop(receiver);
  __ load_klass(subklass, subklass);
  __ z_bru(resolved);

  // Get superklass in klass and subklass in subklass.
  __ bind(quicked);

  __ load_klass(subklass, Z_tos);
  __ z_sllg(index, index, LogBytesPerWord);  // index2bytes for addressing
  __ load_resolved_klass_at_offset(cpool, index, klass);

  __ bind(resolved);

  // Generate subtype check.
  // Superklass in klass. Subklass in subklass.
  __ gen_subtype_check(subklass, klass, Z_ARG4, Z_ARG5, ok_is_subtype);

  // Come here on failure.
  __ clear_reg(Z_tos, true, false);
  __ z_bru(done);

  // Come here on success.
  __ bind(ok_is_subtype);
  __ load_const_optimized(Z_tos, 1);

  // Collect counts on whether this test sees NULLs a lot or not.
  if (ProfileInterpreter) {
    __ z_bru(done);
    __ bind(is_null);
    __ profile_null_seen(Z_tmp_1);
  } else {
    __ bind(is_null);   // same as 'done'
  }

  __ bind(done);
  // tos = 0: obj == NULL or  obj is not an instanceof the specified klass
  // tos = 1: obj != NULL and obj is     an instanceof the specified klass
  BLOCK_COMMENT("} instanceof");
}

//-----------------------------------------------------------------------------
// Breakpoints
void TemplateTable::_breakpoint() {

  // Note: We get here even if we are single stepping.
  // Jbug insists on setting breakpoints at every bytecode
  // even if we are in single step mode.

  transition(vtos, vtos);

  // Get the unpatched byte code.
  __ get_method(Z_ARG2);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::get_original_bytecode_at),
             Z_ARG2, Z_bcp);
  // Save the result to a register that is preserved over C-function calls.
  __ z_lgr(Z_tmp_1, Z_RET);

  // Post the breakpoint event.
  __ get_method(Z_ARG2);
  __ call_VM(noreg,
             CAST_FROM_FN_PTR(address, InterpreterRuntime::_breakpoint),
             Z_ARG2, Z_bcp);

  // Must restore the bytecode, because call_VM destroys Z_bytecode.
  __ z_lgr(Z_bytecode, Z_tmp_1);

  // Complete the execution of original bytecode.
  __ dispatch_only_normal(vtos);
}


// Exceptions

void TemplateTable::athrow() {
  transition(atos, vtos);
  __ null_check(Z_tos);
  __ load_absolute_address(Z_ARG2, Interpreter::throw_exception_entry());
  __ z_br(Z_ARG2);
}

// Synchronization
//
// Note: monitorenter & exit are symmetric routines; which is reflected
//       in the assembly code structure as well
//
// Stack layout:
//
//               callers_sp        <- Z_SP (callers_sp == Z_fp (own fp))
//               return_pc
//               [rest of ABI_160]
//              /slot o:   free
//             / ...       free
//       oper. | slot n+1: free    <- Z_esp points to first free slot
//       stack | slot n:   val                      caches IJAVA_STATE.esp
//             | ...
//              \slot 0:   val
//              /slot m            <- IJAVA_STATE.monitors = monitor block top
//             | ...
//     monitors| slot 2
//             | slot 1
//              \slot 0
//              /slot l            <- monitor block bot
// ijava_state | ...
//             | slot 2
//              \slot 0
//                                 <- Z_fp
void TemplateTable::monitorenter() {
  transition(atos, vtos);

  BLOCK_COMMENT("monitorenter {");

  // Check for NULL object.
  __ null_check(Z_tos);
  const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;
  NearLabel allocated;
  // Initialize entry pointer.
  const Register Rfree_slot = Z_tmp_1;
  __ clear_reg(Rfree_slot, true, false); // Points to free slot or NULL. Don't set CC.

  // Find a free slot in the monitor block from top to bot (result in Rfree_slot).
  {
    const Register Rcurr_monitor = Z_ARG2;
    const Register Rbot = Z_ARG3; // Points to word under bottom of monitor block.
    const Register Rlocked_obj = Z_ARG4;
    NearLabel loop, exit, not_free;
    // Starting with top-most entry.
    __ get_monitors(Rcurr_monitor); // Rcur_monitor = IJAVA_STATE.monitors
    __ add2reg(Rbot, -frame::z_ijava_state_size, Z_fp);

#ifdef ASSERT
    address reentry = NULL;
    { NearLabel ok;
      __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondNotHigh, ok);
      reentry = __ stop_chain_static(reentry, "IJAVA_STATE.monitors points below monitor block bottom");
      __ bind(ok);
    }
    { NearLabel ok;
      __ compareU64_and_branch(Rcurr_monitor, Z_esp, Assembler::bcondHigh, ok);
      reentry = __ stop_chain_static(reentry, "IJAVA_STATE.monitors above Z_esp");
      __ bind(ok);
    }
#endif

    // Check if bottom reached, i.e. if there is at least one monitor.
    __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondEqual, exit);

    __ bind(loop);
    // Check if current entry is used.
    __ load_and_test_long(Rlocked_obj, Address(Rcurr_monitor, BasicObjectLock::obj_offset_in_bytes()));
    __ z_brne(not_free);
    // If not used then remember entry in Rfree_slot.
    __ z_lgr(Rfree_slot, Rcurr_monitor);
    __ bind(not_free);
    // Exit if current entry is for same object; this guarantees, that new monitor
    // used for recursive lock is above the older one.
    __ compareU64_and_branch(Rlocked_obj, Z_tos, Assembler::bcondEqual, exit);
    // otherwise advance to next entry
    __ add2reg(Rcurr_monitor, entry_size);
    // Check if bottom reached, if not at bottom then check this entry.
    __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondNotEqual, loop);
    __ bind(exit);
  }

  // Rfree_slot != NULL -> found one
  __ compareU64_and_branch(Rfree_slot, (intptr_t)0L, Assembler::bcondNotEqual, allocated);

  // Allocate one if there's no free slot.
  __ add_monitor_to_stack(false, Z_ARG3, Z_ARG4, Z_ARG5);
  __ get_monitors(Rfree_slot);

  // Rfree_slot: points to monitor entry.
  __ bind(allocated);

  // Increment bcp to point to the next bytecode, so exception
  // handling for async. exceptions work correctly.
  // The object has already been poped from the stack, so the
  // expression stack looks correct.
  __ add2reg(Z_bcp, 1, Z_bcp);

  // Store object.
  __ z_stg(Z_tos, BasicObjectLock::obj_offset_in_bytes(), Rfree_slot);
  __ lock_object(Rfree_slot, Z_tos);

  // Check to make sure this monitor doesn't cause stack overflow after locking.
  __ save_bcp();  // in case of exception
  __ generate_stack_overflow_check(0);

  // The bcp has already been incremented. Just need to dispatch to
  // next instruction.
  __ dispatch_next(vtos);

  BLOCK_COMMENT("} monitorenter");
}


void TemplateTable::monitorexit() {
  transition(atos, vtos);

  BLOCK_COMMENT("monitorexit {");

  // Check for NULL object.
  __ null_check(Z_tos);

  NearLabel found, not_found;
  const Register Rcurr_monitor = Z_ARG2;

  // Find matching slot.
  {
    const int entry_size = frame::interpreter_frame_monitor_size() * wordSize;
    NearLabel entry, loop;

    const Register Rbot = Z_ARG3; // Points to word under bottom of monitor block.
    const Register Rlocked_obj = Z_ARG4;
    // Starting with top-most entry.
    __ get_monitors(Rcurr_monitor); // Rcur_monitor = IJAVA_STATE.monitors
    __ add2reg(Rbot, -frame::z_ijava_state_size, Z_fp);

#ifdef ASSERT
    address reentry = NULL;
    { NearLabel ok;
      __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondNotHigh, ok);
      reentry = __ stop_chain_static(reentry, "IJAVA_STATE.monitors points below monitor block bottom");
      __ bind(ok);
    }
    { NearLabel ok;
      __ compareU64_and_branch(Rcurr_monitor, Z_esp, Assembler::bcondHigh, ok);
      reentry = __ stop_chain_static(reentry, "IJAVA_STATE.monitors above Z_esp");
      __ bind(ok);
    }
#endif

    // Check if bottom reached, i.e. if there is at least one monitor.
    __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondEqual, not_found);

    __ bind(loop);
    // Check if current entry is for same object.
    __ z_lg(Rlocked_obj, Address(Rcurr_monitor, BasicObjectLock::obj_offset_in_bytes()));
    // If same object then stop searching.
    __ compareU64_and_branch(Rlocked_obj, Z_tos, Assembler::bcondEqual, found);
    // Otherwise advance to next entry.
    __ add2reg(Rcurr_monitor, entry_size);
    // Check if bottom reached, if not at bottom then check this entry.
    __ compareU64_and_branch(Rcurr_monitor, Rbot, Assembler::bcondNotEqual, loop);
  }

  __ bind(not_found);
  // Error handling. Unlocking was not block-structured.
  __ call_VM(noreg, CAST_FROM_FN_PTR(address,
                   InterpreterRuntime::throw_illegal_monitor_state_exception));
  __ should_not_reach_here();

  __ bind(found);
  __ push_ptr(Z_tos); // Make sure object is on stack (contract with oopMaps).
  __ unlock_object(Rcurr_monitor, Z_tos);
  __ pop_ptr(Z_tos); // Discard object.
  BLOCK_COMMENT("} monitorexit");
}

// Wide instructions
void TemplateTable::wide() {
  transition(vtos, vtos);

  __ z_llgc(Z_R1_scratch, at_bcp(1));
  __ z_sllg(Z_R1_scratch, Z_R1_scratch, LogBytesPerWord);
  __ load_absolute_address(Z_tmp_1, (address) Interpreter::_wentry_point);
  __ mem2reg_opt(Z_tmp_1, Address(Z_tmp_1, Z_R1_scratch));
  __ z_br(Z_tmp_1);
  // Note: the bcp increment step is part of the individual wide
  // bytecode implementations.
}

// Multi arrays
void TemplateTable::multianewarray() {
  transition(vtos, atos);

  __ z_llgc(Z_tmp_1, at_bcp(3)); // Get number of dimensions.
  // Slot count to byte offset.
  __ z_sllg(Z_tmp_1, Z_tmp_1, Interpreter::logStackElementSize);
  // Z_esp points past last_dim, so set to Z_ARG2 to first_dim address.
  __ load_address(Z_ARG2, Address(Z_esp, Z_tmp_1));
  call_VM(Z_RET,
          CAST_FROM_FN_PTR(address, InterpreterRuntime::multianewarray),
          Z_ARG2);
  // Pop dimensions from expression stack.
  __ z_agr(Z_esp, Z_tmp_1);
}
