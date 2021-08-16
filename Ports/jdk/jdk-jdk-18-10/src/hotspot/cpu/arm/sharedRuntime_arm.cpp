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

#include "precompiled.hpp"
#include "asm/assembler.inline.hpp"
#include "code/debugInfoRec.hpp"
#include "code/icBuffer.hpp"
#include "code/vtableStubs.hpp"
#include "compiler/oopMap.hpp"
#include "interpreter/interpreter.hpp"
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "oops/compiledICHolder.hpp"
#include "oops/klass.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "utilities/align.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_arm.inline.hpp"
#ifdef COMPILER1
#include "c1/c1_Runtime1.hpp"
#endif
#ifdef COMPILER2
#include "opto/runtime.hpp"
#endif

#define __ masm->

class RegisterSaver {
public:

  // Special registers:
  //              32-bit ARM     64-bit ARM
  //  Rthread:       R10            R28
  //  LR:            R14            R30

  // Rthread is callee saved in the C ABI and never changed by compiled code:
  // no need to save it.

  // 2 slots for LR: the one at LR_offset and an other one at R14/R30_offset.
  // The one at LR_offset is a return address that is needed by stack walking.
  // A c2 method uses LR as a standard register so it may be live when we
  // branch to the runtime. The slot at R14/R30_offset is for the value of LR
  // in case it's live in the method we are coming from.


  enum RegisterLayout {
    fpu_save_size = FloatRegisterImpl::number_of_registers,
#ifndef __SOFTFP__
    D0_offset = 0,
#endif
    R0_offset = fpu_save_size,
    R1_offset,
    R2_offset,
    R3_offset,
    R4_offset,
    R5_offset,
    R6_offset,
#if (FP_REG_NUM != 7)
    // if not saved as FP
    R7_offset,
#endif
    R8_offset,
    R9_offset,
#if (FP_REG_NUM != 11)
    // if not saved as FP
    R11_offset,
#endif
    R12_offset,
    R14_offset,
    FP_offset,
    LR_offset,
    reg_save_size,

    Rmethod_offset = R9_offset,
    Rtemp_offset = R12_offset,
  };

  // all regs but Rthread (R10), FP (R7 or R11), SP and PC
  // (altFP_7_11 is the one amoung R7 and R11 which is not FP)
#define SAVED_BASE_REGS (RegisterSet(R0, R6) | RegisterSet(R8, R9) | RegisterSet(R12) | R14 | altFP_7_11)


  //  When LR may be live in the nmethod from which we are comming
  //  then lr_saved is true, the return address is saved before the
  //  call to save_live_register by the caller and LR contains the
  //  live value.

  static OopMap* save_live_registers(MacroAssembler* masm,
                                     int* total_frame_words,
                                     bool lr_saved = false);
  static void restore_live_registers(MacroAssembler* masm, bool restore_lr = true);

};




OopMap* RegisterSaver::save_live_registers(MacroAssembler* masm,
                                           int* total_frame_words,
                                           bool lr_saved) {
  *total_frame_words = reg_save_size;

  OopMapSet *oop_maps = new OopMapSet();
  OopMap* map = new OopMap(VMRegImpl::slots_per_word * (*total_frame_words), 0);

  if (lr_saved) {
    __ push(RegisterSet(FP));
  } else {
    __ push(RegisterSet(FP) | RegisterSet(LR));
  }
  __ push(SAVED_BASE_REGS);
  if (HaveVFP) {
    if (VM_Version::has_vfp3_32()) {
      __ fpush(FloatRegisterSet(D16, 16));
    } else {
      if (FloatRegisterImpl::number_of_registers > 32) {
        assert(FloatRegisterImpl::number_of_registers == 64, "nb fp registers should be 64");
        __ sub(SP, SP, 32 * wordSize);
      }
    }
    __ fpush(FloatRegisterSet(D0, 16));
  } else {
    __ sub(SP, SP, fpu_save_size * wordSize);
  }

  int i;
  int j=0;
  for (i = R0_offset; i <= R9_offset; i++) {
    if (j == FP_REG_NUM) {
      // skip the FP register, managed below.
      j++;
    }
    map->set_callee_saved(VMRegImpl::stack2reg(i), as_Register(j)->as_VMReg());
    j++;
  }
  assert(j == R10->encoding(), "must be");
#if (FP_REG_NUM != 11)
  // add R11, if not managed as FP
  map->set_callee_saved(VMRegImpl::stack2reg(R11_offset), R11->as_VMReg());
#endif
  map->set_callee_saved(VMRegImpl::stack2reg(R12_offset), R12->as_VMReg());
  map->set_callee_saved(VMRegImpl::stack2reg(R14_offset), R14->as_VMReg());
  if (HaveVFP) {
    for (i = 0; i < (VM_Version::has_vfp3_32() ? 64 : 32); i+=2) {
      map->set_callee_saved(VMRegImpl::stack2reg(i), as_FloatRegister(i)->as_VMReg());
      map->set_callee_saved(VMRegImpl::stack2reg(i + 1), as_FloatRegister(i)->as_VMReg()->next());
    }
  }

  return map;
}

void RegisterSaver::restore_live_registers(MacroAssembler* masm, bool restore_lr) {
  if (HaveVFP) {
    __ fpop(FloatRegisterSet(D0, 16));
    if (VM_Version::has_vfp3_32()) {
      __ fpop(FloatRegisterSet(D16, 16));
    } else {
      if (FloatRegisterImpl::number_of_registers > 32) {
        assert(FloatRegisterImpl::number_of_registers == 64, "nb fp registers should be 64");
        __ add(SP, SP, 32 * wordSize);
      }
    }
  } else {
    __ add(SP, SP, fpu_save_size * wordSize);
  }
  __ pop(SAVED_BASE_REGS);
  if (restore_lr) {
    __ pop(RegisterSet(FP) | RegisterSet(LR));
  } else {
    __ pop(RegisterSet(FP));
  }
}


static void push_result_registers(MacroAssembler* masm, BasicType ret_type) {
#ifdef __ABI_HARD__
  if (ret_type == T_DOUBLE || ret_type == T_FLOAT) {
    __ sub(SP, SP, 8);
    __ fstd(D0, Address(SP));
    return;
  }
#endif // __ABI_HARD__
  __ raw_push(R0, R1);
}

static void pop_result_registers(MacroAssembler* masm, BasicType ret_type) {
#ifdef __ABI_HARD__
  if (ret_type == T_DOUBLE || ret_type == T_FLOAT) {
    __ fldd(D0, Address(SP));
    __ add(SP, SP, 8);
    return;
  }
#endif // __ABI_HARD__
  __ raw_pop(R0, R1);
}

static void push_param_registers(MacroAssembler* masm, int fp_regs_in_arguments) {
  // R1-R3 arguments need to be saved, but we push 4 registers for 8-byte alignment
  __ push(RegisterSet(R0, R3));

  // preserve arguments
  // Likely not needed as the locking code won't probably modify volatile FP registers,
  // but there is no way to guarantee that
  if (fp_regs_in_arguments) {
    // convert fp_regs_in_arguments to a number of double registers
    int double_regs_num = (fp_regs_in_arguments + 1) >> 1;
    __ fpush_hardfp(FloatRegisterSet(D0, double_regs_num));
  }
}

static void pop_param_registers(MacroAssembler* masm, int fp_regs_in_arguments) {
  if (fp_regs_in_arguments) {
    int double_regs_num = (fp_regs_in_arguments + 1) >> 1;
    __ fpop_hardfp(FloatRegisterSet(D0, double_regs_num));
  }
  __ pop(RegisterSet(R0, R3));
}



// Is vector's size (in bytes) bigger than a size saved by default?
// All vector registers are saved by default on ARM.
bool SharedRuntime::is_wide_vector(int size) {
  return false;
}

int SharedRuntime::c_calling_convention(const BasicType *sig_bt,
                                        VMRegPair *regs,
                                        VMRegPair *regs2,
                                        int total_args_passed) {
  assert(regs2 == NULL, "not needed on arm");

  int slot = 0;
  int ireg = 0;
#ifdef __ABI_HARD__
  int fp_slot = 0;
  int single_fpr_slot = 0;
#endif // __ABI_HARD__
  for (int i = 0; i < total_args_passed; i++) {
    switch (sig_bt[i]) {
    case T_SHORT:
    case T_CHAR:
    case T_BYTE:
    case T_BOOLEAN:
    case T_INT:
    case T_ARRAY:
    case T_OBJECT:
    case T_ADDRESS:
    case T_METADATA:
#ifndef __ABI_HARD__
    case T_FLOAT:
#endif // !__ABI_HARD__
      if (ireg < 4) {
        Register r = as_Register(ireg);
        regs[i].set1(r->as_VMReg());
        ireg++;
      } else {
        regs[i].set1(VMRegImpl::stack2reg(slot));
        slot++;
      }
      break;
    case T_LONG:
#ifndef __ABI_HARD__
    case T_DOUBLE:
#endif // !__ABI_HARD__
      assert((i + 1) < total_args_passed && sig_bt[i+1] == T_VOID, "missing Half" );
      if (ireg <= 2) {
#if (ALIGN_WIDE_ARGUMENTS == 1)
        if(ireg & 1) ireg++;  // Aligned location required
#endif
        Register r1 = as_Register(ireg);
        Register r2 = as_Register(ireg + 1);
        regs[i].set_pair(r2->as_VMReg(), r1->as_VMReg());
        ireg += 2;
#if (ALIGN_WIDE_ARGUMENTS == 0)
      } else if (ireg == 3) {
        // uses R3 + one stack slot
        Register r = as_Register(ireg);
        regs[i].set_pair(VMRegImpl::stack2reg(slot), r->as_VMReg());
        ireg += 1;
        slot += 1;
#endif
      } else {
        if (slot & 1) slot++; // Aligned location required
        regs[i].set_pair(VMRegImpl::stack2reg(slot+1), VMRegImpl::stack2reg(slot));
        slot += 2;
        ireg = 4;
      }
      break;
    case T_VOID:
      regs[i].set_bad();
      break;
#ifdef __ABI_HARD__
    case T_FLOAT:
      if ((fp_slot < 16)||(single_fpr_slot & 1)) {
        if ((single_fpr_slot & 1) == 0) {
          single_fpr_slot = fp_slot;
          fp_slot += 2;
        }
        FloatRegister r = as_FloatRegister(single_fpr_slot);
        single_fpr_slot++;
        regs[i].set1(r->as_VMReg());
      } else {
        regs[i].set1(VMRegImpl::stack2reg(slot));
        slot++;
      }
      break;
    case T_DOUBLE:
      assert(ALIGN_WIDE_ARGUMENTS == 1, "ABI_HARD not supported with unaligned wide arguments");
      if (fp_slot <= 14) {
        FloatRegister r1 = as_FloatRegister(fp_slot);
        FloatRegister r2 = as_FloatRegister(fp_slot+1);
        regs[i].set_pair(r2->as_VMReg(), r1->as_VMReg());
        fp_slot += 2;
      } else {
        if(slot & 1) slot++;
        regs[i].set_pair(VMRegImpl::stack2reg(slot+1), VMRegImpl::stack2reg(slot));
        slot += 2;
        single_fpr_slot = 16;
      }
      break;
#endif // __ABI_HARD__
    default:
      ShouldNotReachHere();
    }
  }
  return slot;
}

int SharedRuntime::vector_calling_convention(VMRegPair *regs,
                                             uint num_bits,
                                             uint total_args_passed) {
  Unimplemented();
  return 0;
}

int SharedRuntime::java_calling_convention(const BasicType *sig_bt,
                                           VMRegPair *regs,
                                           int total_args_passed) {
#ifdef __SOFTFP__
  // soft float is the same as the C calling convention.
  return c_calling_convention(sig_bt, regs, NULL, total_args_passed);
#endif // __SOFTFP__
  int slot = 0;
  int ireg = 0;
  int freg = 0;
  int single_fpr = 0;

  for (int i = 0; i < total_args_passed; i++) {
    switch (sig_bt[i]) {
    case T_SHORT:
    case T_CHAR:
    case T_BYTE:
    case T_BOOLEAN:
    case T_INT:
    case T_ARRAY:
    case T_OBJECT:
    case T_ADDRESS:
      if (ireg < 4) {
        Register r = as_Register(ireg++);
        regs[i].set1(r->as_VMReg());
      } else {
        regs[i].set1(VMRegImpl::stack2reg(slot++));
      }
      break;
    case T_FLOAT:
      // C2 utilizes S14/S15 for mem-mem moves
      if ((freg < 16 COMPILER2_PRESENT(-2)) || (single_fpr & 1)) {
        if ((single_fpr & 1) == 0) {
          single_fpr = freg;
          freg += 2;
        }
        FloatRegister r = as_FloatRegister(single_fpr++);
        regs[i].set1(r->as_VMReg());
      } else {
        regs[i].set1(VMRegImpl::stack2reg(slot++));
      }
      break;
    case T_DOUBLE:
      // C2 utilizes S14/S15 for mem-mem moves
      if (freg <= 14 COMPILER2_PRESENT(-2)) {
        FloatRegister r1 = as_FloatRegister(freg);
        FloatRegister r2 = as_FloatRegister(freg + 1);
        regs[i].set_pair(r2->as_VMReg(), r1->as_VMReg());
        freg += 2;
      } else {
        // Keep internally the aligned calling convention,
        // ignoring ALIGN_WIDE_ARGUMENTS
        if (slot & 1) slot++;
        regs[i].set_pair(VMRegImpl::stack2reg(slot + 1), VMRegImpl::stack2reg(slot));
        slot += 2;
        single_fpr = 16;
      }
      break;
    case T_LONG:
      // Keep internally the aligned calling convention,
      // ignoring ALIGN_WIDE_ARGUMENTS
      if (ireg <= 2) {
        if (ireg & 1) ireg++;
        Register r1 = as_Register(ireg);
        Register r2 = as_Register(ireg + 1);
        regs[i].set_pair(r2->as_VMReg(), r1->as_VMReg());
        ireg += 2;
      } else {
        if (slot & 1) slot++;
        regs[i].set_pair(VMRegImpl::stack2reg(slot + 1), VMRegImpl::stack2reg(slot));
        slot += 2;
        ireg = 4;
      }
      break;
    case T_VOID:
      regs[i].set_bad();
      break;
    default:
      ShouldNotReachHere();
    }
  }

  if (slot & 1) slot++;
  return slot;
}

static void patch_callers_callsite(MacroAssembler *masm) {
  Label skip;

  __ ldr(Rtemp, Address(Rmethod, Method::code_offset()));
  __ cbz(Rtemp, skip);

  // Pushing an even number of registers for stack alignment.
  // Selecting R9, which had to be saved anyway for some platforms.
  __ push(RegisterSet(R0, R3) | R9 | LR);
  __ fpush_hardfp(FloatRegisterSet(D0, 8));

  __ mov(R0, Rmethod);
  __ mov(R1, LR);
  __ call(CAST_FROM_FN_PTR(address, SharedRuntime::fixup_callers_callsite));

  __ fpop_hardfp(FloatRegisterSet(D0, 8));
  __ pop(RegisterSet(R0, R3) | R9 | LR);

  __ bind(skip);
}

void SharedRuntime::gen_i2c_adapter(MacroAssembler *masm,
                                    int total_args_passed, int comp_args_on_stack,
                                    const BasicType *sig_bt, const VMRegPair *regs) {
  // TODO: ARM - May be can use ldm to load arguments
  const Register tmp = Rtemp; // avoid erasing R5_mh

  // Next assert may not be needed but safer. Extra analysis required
  // if this there is not enough free registers and we need to use R5 here.
  assert_different_registers(tmp, R5_mh);

  // 6243940 We might end up in handle_wrong_method if
  // the callee is deoptimized as we race thru here. If that
  // happens we don't want to take a safepoint because the
  // caller frame will look interpreted and arguments are now
  // "compiled" so it is much better to make this transition
  // invisible to the stack walking code. Unfortunately if
  // we try and find the callee by normal means a safepoint
  // is possible. So we stash the desired callee in the thread
  // and the vm will find there should this case occur.
  Address callee_target_addr(Rthread, JavaThread::callee_target_offset());
  __ str(Rmethod, callee_target_addr);


  assert_different_registers(tmp, R0, R1, R2, R3, Rsender_sp, Rmethod);

  const Register initial_sp = Rmethod; // temporarily scratched

  // Old code was modifying R4 but this looks unsafe (particularly with JSR292)
  assert_different_registers(tmp, R0, R1, R2, R3, Rsender_sp, initial_sp);

  __ mov(initial_sp, SP);

  if (comp_args_on_stack) {
    __ sub_slow(SP, SP, comp_args_on_stack * VMRegImpl::stack_slot_size);
  }
  __ bic(SP, SP, StackAlignmentInBytes - 1);

  for (int i = 0; i < total_args_passed; i++) {
    if (sig_bt[i] == T_VOID) {
      assert(i > 0 && (sig_bt[i-1] == T_LONG || sig_bt[i-1] == T_DOUBLE), "missing half");
      continue;
    }
    assert(!regs[i].second()->is_valid() || regs[i].first()->next() == regs[i].second(), "must be ordered");
    int arg_offset = Interpreter::expr_offset_in_bytes(total_args_passed - 1 - i);

    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (r_1->is_stack()) {
      int stack_offset = r_1->reg2stack() * VMRegImpl::stack_slot_size;
      if (!r_2->is_valid()) {
        __ ldr(tmp, Address(initial_sp, arg_offset));
        __ str(tmp, Address(SP, stack_offset));
      } else {
        __ ldr(tmp, Address(initial_sp, arg_offset - Interpreter::stackElementSize));
        __ str(tmp, Address(SP, stack_offset));
        __ ldr(tmp, Address(initial_sp, arg_offset));
        __ str(tmp, Address(SP, stack_offset + wordSize));
      }
    } else if (r_1->is_Register()) {
      if (!r_2->is_valid()) {
        __ ldr(r_1->as_Register(), Address(initial_sp, arg_offset));
      } else {
        __ ldr(r_1->as_Register(), Address(initial_sp, arg_offset - Interpreter::stackElementSize));
        __ ldr(r_2->as_Register(), Address(initial_sp, arg_offset));
      }
    } else if (r_1->is_FloatRegister()) {
#ifdef __SOFTFP__
      ShouldNotReachHere();
#endif // __SOFTFP__
      if (!r_2->is_valid()) {
        __ flds(r_1->as_FloatRegister(), Address(initial_sp, arg_offset));
      } else {
        __ fldd(r_1->as_FloatRegister(), Address(initial_sp, arg_offset - Interpreter::stackElementSize));
      }
    } else {
      assert(!r_1->is_valid() && !r_2->is_valid(), "must be");
    }
  }

  // restore Rmethod (scratched for initial_sp)
  __ ldr(Rmethod, callee_target_addr);
  __ ldr(PC, Address(Rmethod, Method::from_compiled_offset()));

}

static void gen_c2i_adapter(MacroAssembler *masm,
                            int total_args_passed,  int comp_args_on_stack,
                            const BasicType *sig_bt, const VMRegPair *regs,
                            Label& skip_fixup) {
  // TODO: ARM - May be can use stm to deoptimize arguments
  const Register tmp = Rtemp;

  patch_callers_callsite(masm);
  __ bind(skip_fixup);

  __ mov(Rsender_sp, SP); // not yet saved


  int extraspace = total_args_passed * Interpreter::stackElementSize;
  if (extraspace) {
    __ sub_slow(SP, SP, extraspace);
  }

  for (int i = 0; i < total_args_passed; i++) {
    if (sig_bt[i] == T_VOID) {
      assert(i > 0 && (sig_bt[i-1] == T_LONG || sig_bt[i-1] == T_DOUBLE), "missing half");
      continue;
    }
    int stack_offset = (total_args_passed - 1 - i) * Interpreter::stackElementSize;

    VMReg r_1 = regs[i].first();
    VMReg r_2 = regs[i].second();
    if (r_1->is_stack()) {
      int arg_offset = r_1->reg2stack() * VMRegImpl::stack_slot_size + extraspace;
      if (!r_2->is_valid()) {
        __ ldr(tmp, Address(SP, arg_offset));
        __ str(tmp, Address(SP, stack_offset));
      } else {
        __ ldr(tmp, Address(SP, arg_offset));
        __ str(tmp, Address(SP, stack_offset - Interpreter::stackElementSize));
        __ ldr(tmp, Address(SP, arg_offset + wordSize));
        __ str(tmp, Address(SP, stack_offset));
      }
    } else if (r_1->is_Register()) {
      if (!r_2->is_valid()) {
        __ str(r_1->as_Register(), Address(SP, stack_offset));
      } else {
        __ str(r_1->as_Register(), Address(SP, stack_offset - Interpreter::stackElementSize));
        __ str(r_2->as_Register(), Address(SP, stack_offset));
      }
    } else if (r_1->is_FloatRegister()) {
#ifdef __SOFTFP__
      ShouldNotReachHere();
#endif // __SOFTFP__
      if (!r_2->is_valid()) {
        __ fsts(r_1->as_FloatRegister(), Address(SP, stack_offset));
      } else {
        __ fstd(r_1->as_FloatRegister(), Address(SP, stack_offset - Interpreter::stackElementSize));
      }
    } else {
      assert(!r_1->is_valid() && !r_2->is_valid(), "must be");
    }
  }

  __ ldr(PC, Address(Rmethod, Method::interpreter_entry_offset()));

}

AdapterHandlerEntry* SharedRuntime::generate_i2c2i_adapters(MacroAssembler *masm,
                                                            int total_args_passed,
                                                            int comp_args_on_stack,
                                                            const BasicType *sig_bt,
                                                            const VMRegPair *regs,
                                                            AdapterFingerPrint* fingerprint) {
  address i2c_entry = __ pc();
  gen_i2c_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs);

  address c2i_unverified_entry = __ pc();
  Label skip_fixup;
  const Register receiver       = R0;
  const Register holder_klass   = Rtemp; // XXX should be OK for C2 but not 100% sure
  const Register receiver_klass = R4;

  __ load_klass(receiver_klass, receiver);
  __ ldr(holder_klass, Address(Ricklass, CompiledICHolder::holder_klass_offset()));
  __ ldr(Rmethod, Address(Ricklass, CompiledICHolder::holder_metadata_offset()));
  __ cmp(receiver_klass, holder_klass);

  __ ldr(Rtemp, Address(Rmethod, Method::code_offset()), eq);
  __ cmp(Rtemp, 0, eq);
  __ b(skip_fixup, eq);
  __ jump(SharedRuntime::get_ic_miss_stub(), relocInfo::runtime_call_type, noreg, ne);

  address c2i_entry = __ pc();
  gen_c2i_adapter(masm, total_args_passed, comp_args_on_stack, sig_bt, regs, skip_fixup);

  __ flush();
  return AdapterHandlerLibrary::new_entry(fingerprint, i2c_entry, c2i_entry, c2i_unverified_entry);
}


static int reg2offset_in(VMReg r) {
  // Account for saved FP and LR
  return r->reg2stack() * VMRegImpl::stack_slot_size + 2*wordSize;
}

static int reg2offset_out(VMReg r) {
  return (r->reg2stack() + SharedRuntime::out_preserve_stack_slots()) * VMRegImpl::stack_slot_size;
}


static void verify_oop_args(MacroAssembler* masm,
                            const methodHandle& method,
                            const BasicType* sig_bt,
                            const VMRegPair* regs) {
  Register temp_reg = Rmethod;  // not part of any compiled calling seq
  if (VerifyOops) {
    for (int i = 0; i < method->size_of_parameters(); i++) {
      if (sig_bt[i] == T_OBJECT || sig_bt[i] == T_ARRAY) {
        VMReg r = regs[i].first();
        assert(r->is_valid(), "bad oop arg");
        if (r->is_stack()) {
          __ ldr(temp_reg, Address(SP, r->reg2stack() * VMRegImpl::stack_slot_size));
          __ verify_oop(temp_reg);
        } else {
          __ verify_oop(r->as_Register());
        }
      }
    }
  }
}

static void gen_special_dispatch(MacroAssembler* masm,
                                 const methodHandle& method,
                                 const BasicType* sig_bt,
                                 const VMRegPair* regs) {
  verify_oop_args(masm, method, sig_bt, regs);
  vmIntrinsics::ID iid = method->intrinsic_id();

  // Now write the args into the outgoing interpreter space
  bool     has_receiver   = false;
  Register receiver_reg   = noreg;
  int      member_arg_pos = -1;
  Register member_reg     = noreg;
  int      ref_kind       = MethodHandles::signature_polymorphic_intrinsic_ref_kind(iid);
  if (ref_kind != 0) {
    member_arg_pos = method->size_of_parameters() - 1;  // trailing MemberName argument
    member_reg = Rmethod;  // known to be free at this point
    has_receiver = MethodHandles::ref_kind_has_receiver(ref_kind);
  } else if (iid == vmIntrinsics::_invokeBasic) {
    has_receiver = true;
  } else {
    fatal("unexpected intrinsic id %d", vmIntrinsics::as_int(iid));
  }

  if (member_reg != noreg) {
    // Load the member_arg into register, if necessary.
    SharedRuntime::check_member_name_argument_is_last_argument(method, sig_bt, regs);
    VMReg r = regs[member_arg_pos].first();
    if (r->is_stack()) {
      __ ldr(member_reg, Address(SP, r->reg2stack() * VMRegImpl::stack_slot_size));
    } else {
      // no data motion is needed
      member_reg = r->as_Register();
    }
  }

  if (has_receiver) {
    // Make sure the receiver is loaded into a register.
    assert(method->size_of_parameters() > 0, "oob");
    assert(sig_bt[0] == T_OBJECT, "receiver argument must be an object");
    VMReg r = regs[0].first();
    assert(r->is_valid(), "bad receiver arg");
    if (r->is_stack()) {
      // Porting note:  This assumes that compiled calling conventions always
      // pass the receiver oop in a register.  If this is not true on some
      // platform, pick a temp and load the receiver from stack.
      assert(false, "receiver always in a register");
      receiver_reg = j_rarg0;  // known to be free at this point
      __ ldr(receiver_reg, Address(SP, r->reg2stack() * VMRegImpl::stack_slot_size));
    } else {
      // no data motion is needed
      receiver_reg = r->as_Register();
    }
  }

  // Figure out which address we are really jumping to:
  MethodHandles::generate_method_handle_dispatch(masm, iid,
                                                 receiver_reg, member_reg, /*for_compiler_entry:*/ true);
}

// ---------------------------------------------------------------------------
// Generate a native wrapper for a given method.  The method takes arguments
// in the Java compiled code convention, marshals them to the native
// convention (handlizes oops, etc), transitions to native, makes the call,
// returns to java state (possibly blocking), unhandlizes any result and
// returns.
nmethod* SharedRuntime::generate_native_wrapper(MacroAssembler* masm,
                                                const methodHandle& method,
                                                int compile_id,
                                                BasicType* in_sig_bt,
                                                VMRegPair* in_regs,
                                                BasicType ret_type,
                                                address critical_entry) {
  if (method->is_method_handle_intrinsic()) {
    vmIntrinsics::ID iid = method->intrinsic_id();
    intptr_t start = (intptr_t)__ pc();
    int vep_offset = ((intptr_t)__ pc()) - start;
    gen_special_dispatch(masm,
                         method,
                         in_sig_bt,
                         in_regs);
    int frame_complete = ((intptr_t)__ pc()) - start;  // not complete, period
    __ flush();
    int stack_slots = SharedRuntime::out_preserve_stack_slots();  // no out slots at all, actually
    return nmethod::new_native_nmethod(method,
                                       compile_id,
                                       masm->code(),
                                       vep_offset,
                                       frame_complete,
                                       stack_slots / VMRegImpl::slots_per_word,
                                       in_ByteSize(-1),
                                       in_ByteSize(-1),
                                       (OopMapSet*)NULL);
  }
  // Arguments for JNI method include JNIEnv and Class if static

  // Usage of Rtemp should be OK since scratched by native call

  bool is_static = method->is_static();

  const int total_in_args = method->size_of_parameters();
  int total_c_args = total_in_args + 1;
  if (is_static) {
    total_c_args++;
  }

  BasicType* out_sig_bt = NEW_RESOURCE_ARRAY(BasicType, total_c_args);
  VMRegPair* out_regs   = NEW_RESOURCE_ARRAY(VMRegPair, total_c_args);

  int argc = 0;
  out_sig_bt[argc++] = T_ADDRESS;
  if (is_static) {
    out_sig_bt[argc++] = T_OBJECT;
  }

  int i;
  for (i = 0; i < total_in_args; i++) {
    out_sig_bt[argc++] = in_sig_bt[i];
  }

  int out_arg_slots = c_calling_convention(out_sig_bt, out_regs, NULL, total_c_args);
  int stack_slots = SharedRuntime::out_preserve_stack_slots() + out_arg_slots;
  // Since object arguments need to be wrapped, we must preserve space
  // for those object arguments which come in registers (GPR_PARAMS maximum)
  // plus one more slot for Klass handle (for static methods)
  int oop_handle_offset = stack_slots;
  stack_slots += (GPR_PARAMS + 1) * VMRegImpl::slots_per_word;

  // Plus a lock if needed
  int lock_slot_offset = 0;
  if (method->is_synchronized()) {
    lock_slot_offset = stack_slots;
    assert(sizeof(BasicLock) == wordSize, "adjust this code");
    stack_slots += VMRegImpl::slots_per_word;
  }

  // Space to save return address and FP
  stack_slots += 2 * VMRegImpl::slots_per_word;

  // Calculate the final stack size taking account of alignment
  stack_slots = align_up(stack_slots, StackAlignmentInBytes / VMRegImpl::stack_slot_size);
  int stack_size = stack_slots * VMRegImpl::stack_slot_size;
  int lock_slot_fp_offset = stack_size - 2 * wordSize -
    lock_slot_offset * VMRegImpl::stack_slot_size;

  // Unverified entry point
  address start = __ pc();

  // Inline cache check, same as in C1_MacroAssembler::inline_cache_check()
  const Register receiver = R0; // see receiverOpr()
  __ load_klass(Rtemp, receiver);
  __ cmp(Rtemp, Ricklass);
  Label verified;

  __ b(verified, eq); // jump over alignment no-ops too
  __ jump(SharedRuntime::get_ic_miss_stub(), relocInfo::runtime_call_type, Rtemp);
  __ align(CodeEntryAlignment);

  // Verified entry point
  __ bind(verified);
  int vep_offset = __ pc() - start;


  if ((InlineObjectHash && method->intrinsic_id() == vmIntrinsics::_hashCode) || (method->intrinsic_id() == vmIntrinsics::_identityHashCode)) {
    // Object.hashCode, System.identityHashCode can pull the hashCode from the header word
    // instead of doing a full VM transition once it's been computed.
    Label slow_case;
    const Register obj_reg = R0;

    // Unlike for Object.hashCode, System.identityHashCode is static method and
    // gets object as argument instead of the receiver.
    if (method->intrinsic_id() == vmIntrinsics::_identityHashCode) {
      assert(method->is_static(), "method should be static");
      // return 0 for null reference input, return val = R0 = obj_reg = 0
      __ cmp(obj_reg, 0);
      __ bx(LR, eq);
    }

    __ ldr(Rtemp, Address(obj_reg, oopDesc::mark_offset_in_bytes()));

    assert(markWord::unlocked_value == 1, "adjust this code");
    __ tbz(Rtemp, exact_log2(markWord::unlocked_value), slow_case);

    __ bics(Rtemp, Rtemp, ~markWord::hash_mask_in_place);
    __ mov(R0, AsmOperand(Rtemp, lsr, markWord::hash_shift), ne);
    __ bx(LR, ne);

    __ bind(slow_case);
  }

  // Bang stack pages
  __ arm_stack_overflow_check(stack_size, Rtemp);

  // Setup frame linkage
  __ raw_push(FP, LR);
  __ mov(FP, SP);
  __ sub_slow(SP, SP, stack_size - 2*wordSize);

  int frame_complete = __ pc() - start;

  OopMapSet* oop_maps = new OopMapSet();
  OopMap* map = new OopMap(stack_slots * 2, 0 /* arg_slots*/);
  const int extra_args = is_static ? 2 : 1;
  int receiver_offset = -1;
  int fp_regs_in_arguments = 0;

  for (i = total_in_args; --i >= 0; ) {
    switch (in_sig_bt[i]) {
    case T_ARRAY:
    case T_OBJECT: {
      VMReg src = in_regs[i].first();
      VMReg dst = out_regs[i + extra_args].first();
      if (src->is_stack()) {
        assert(dst->is_stack(), "must be");
        assert(i != 0, "Incoming receiver is always in a register");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src)));
        __ cmp(Rtemp, 0);
        __ add(Rtemp, FP, reg2offset_in(src), ne);
        __ str(Rtemp, Address(SP, reg2offset_out(dst)));
        int offset_in_older_frame = src->reg2stack() + SharedRuntime::out_preserve_stack_slots();
        map->set_oop(VMRegImpl::stack2reg(offset_in_older_frame + stack_slots));
      } else {
        int offset = oop_handle_offset * VMRegImpl::stack_slot_size;
        __ str(src->as_Register(), Address(SP, offset));
        map->set_oop(VMRegImpl::stack2reg(oop_handle_offset));
        if ((i == 0) && (!is_static)) {
          receiver_offset = offset;
        }
        oop_handle_offset += VMRegImpl::slots_per_word;

        if (dst->is_stack()) {
          __ movs(Rtemp, src->as_Register());
          __ add(Rtemp, SP, offset, ne);
          __ str(Rtemp, Address(SP, reg2offset_out(dst)));
        } else {
          __ movs(dst->as_Register(), src->as_Register());
          __ add(dst->as_Register(), SP, offset, ne);
        }
      }
    }

    case T_VOID:
      break;


#ifdef __SOFTFP__
    case T_DOUBLE:
#endif
    case T_LONG: {
      VMReg src_1 = in_regs[i].first();
      VMReg src_2 = in_regs[i].second();
      VMReg dst_1 = out_regs[i + extra_args].first();
      VMReg dst_2 = out_regs[i + extra_args].second();
#if (ALIGN_WIDE_ARGUMENTS == 0)
      // C convention can mix a register and a stack slot for a
      // 64-bits native argument.

      // Note: following code should work independently of whether
      // the Java calling convention follows C convention or whether
      // it aligns 64-bit values.
      if (dst_2->is_Register()) {
        if (src_1->as_Register() != dst_1->as_Register()) {
          assert(src_1->as_Register() != dst_2->as_Register() &&
                 src_2->as_Register() != dst_2->as_Register(), "must be");
          __ mov(dst_2->as_Register(), src_2->as_Register());
          __ mov(dst_1->as_Register(), src_1->as_Register());
        } else {
          assert(src_2->as_Register() == dst_2->as_Register(), "must be");
        }
      } else if (src_2->is_Register()) {
        if (dst_1->is_Register()) {
          // dst mixes a register and a stack slot
          assert(dst_2->is_stack() && src_1->is_Register() && src_2->is_Register(), "must be");
          assert(src_1->as_Register() != dst_1->as_Register(), "must be");
          __ str(src_2->as_Register(), Address(SP, reg2offset_out(dst_2)));
          __ mov(dst_1->as_Register(), src_1->as_Register());
        } else {
          // registers to stack slots
          assert(dst_2->is_stack() && src_1->is_Register() && src_2->is_Register(), "must be");
          __ str(src_1->as_Register(), Address(SP, reg2offset_out(dst_1)));
          __ str(src_2->as_Register(), Address(SP, reg2offset_out(dst_2)));
        }
      } else if (src_1->is_Register()) {
        if (dst_1->is_Register()) {
          // src and dst must be R3 + stack slot
          assert(dst_1->as_Register() == src_1->as_Register(), "must be");
          __ ldr(Rtemp,    Address(FP, reg2offset_in(src_2)));
          __ str(Rtemp,    Address(SP, reg2offset_out(dst_2)));
        } else {
          // <R3,stack> -> <stack,stack>
          assert(dst_2->is_stack() && src_2->is_stack(), "must be");
          __ ldr(LR, Address(FP, reg2offset_in(src_2)));
          __ str(src_1->as_Register(), Address(SP, reg2offset_out(dst_1)));
          __ str(LR, Address(SP, reg2offset_out(dst_2)));
        }
      } else {
        assert(src_2->is_stack() && dst_1->is_stack() && dst_2->is_stack(), "must be");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src_1)));
        __ ldr(LR,    Address(FP, reg2offset_in(src_2)));
        __ str(Rtemp, Address(SP, reg2offset_out(dst_1)));
        __ str(LR,    Address(SP, reg2offset_out(dst_2)));
      }
#else // ALIGN_WIDE_ARGUMENTS
      if (src_1->is_stack()) {
        assert(src_2->is_stack() && dst_1->is_stack() && dst_2->is_stack(), "must be");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src_1)));
        __ ldr(LR,    Address(FP, reg2offset_in(src_2)));
        __ str(Rtemp, Address(SP, reg2offset_out(dst_1)));
        __ str(LR,    Address(SP, reg2offset_out(dst_2)));
      } else if (dst_1->is_stack()) {
        assert(dst_2->is_stack() && src_1->is_Register() && src_2->is_Register(), "must be");
        __ str(src_1->as_Register(), Address(SP, reg2offset_out(dst_1)));
        __ str(src_2->as_Register(), Address(SP, reg2offset_out(dst_2)));
      } else if (src_1->as_Register() == dst_1->as_Register()) {
        assert(src_2->as_Register() == dst_2->as_Register(), "must be");
      } else {
        assert(src_1->as_Register() != dst_2->as_Register() &&
               src_2->as_Register() != dst_2->as_Register(), "must be");
        __ mov(dst_2->as_Register(), src_2->as_Register());
        __ mov(dst_1->as_Register(), src_1->as_Register());
      }
#endif // ALIGN_WIDE_ARGUMENTS
      break;
    }

#if (!defined __SOFTFP__ && !defined __ABI_HARD__)
    case T_FLOAT: {
      VMReg src = in_regs[i].first();
      VMReg dst = out_regs[i + extra_args].first();
      if (src->is_stack()) {
        assert(dst->is_stack(), "must be");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src)));
        __ str(Rtemp, Address(SP, reg2offset_out(dst)));
      } else if (dst->is_stack()) {
        __ fsts(src->as_FloatRegister(), Address(SP, reg2offset_out(dst)));
      } else {
        assert(src->is_FloatRegister() && dst->is_Register(), "must be");
        __ fmrs(dst->as_Register(), src->as_FloatRegister());
      }
      break;
    }

    case T_DOUBLE: {
      VMReg src_1 = in_regs[i].first();
      VMReg src_2 = in_regs[i].second();
      VMReg dst_1 = out_regs[i + extra_args].first();
      VMReg dst_2 = out_regs[i + extra_args].second();
      if (src_1->is_stack()) {
        assert(src_2->is_stack() && dst_1->is_stack() && dst_2->is_stack(), "must be");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src_1)));
        __ ldr(LR,    Address(FP, reg2offset_in(src_2)));
        __ str(Rtemp, Address(SP, reg2offset_out(dst_1)));
        __ str(LR,    Address(SP, reg2offset_out(dst_2)));
      } else if (dst_1->is_stack()) {
        assert(dst_2->is_stack() && src_1->is_FloatRegister(), "must be");
        __ fstd(src_1->as_FloatRegister(), Address(SP, reg2offset_out(dst_1)));
#if (ALIGN_WIDE_ARGUMENTS == 0)
      } else if (dst_2->is_stack()) {
        assert(! src_2->is_stack(), "must be"); // assuming internal java convention is aligned
        // double register must go into R3 + one stack slot
        __ fmrrd(dst_1->as_Register(), Rtemp, src_1->as_FloatRegister());
        __ str(Rtemp, Address(SP, reg2offset_out(dst_2)));
#endif
      } else {
        assert(src_1->is_FloatRegister() && dst_1->is_Register() && dst_2->is_Register(), "must be");
        __ fmrrd(dst_1->as_Register(), dst_2->as_Register(), src_1->as_FloatRegister());
      }
      break;
    }
#endif // __SOFTFP__

#ifdef __ABI_HARD__
    case T_FLOAT: {
      VMReg src = in_regs[i].first();
      VMReg dst = out_regs[i + extra_args].first();
      if (src->is_stack()) {
        if (dst->is_stack()) {
          __ ldr(Rtemp, Address(FP, reg2offset_in(src)));
          __ str(Rtemp, Address(SP, reg2offset_out(dst)));
        } else {
          // C2 Java calling convention does not populate S14 and S15, therefore
          // those need to be loaded from stack here
          __ flds(dst->as_FloatRegister(), Address(FP, reg2offset_in(src)));
          fp_regs_in_arguments++;
        }
      } else {
        assert(src->is_FloatRegister(), "must be");
        fp_regs_in_arguments++;
      }
      break;
    }
    case T_DOUBLE: {
      VMReg src_1 = in_regs[i].first();
      VMReg src_2 = in_regs[i].second();
      VMReg dst_1 = out_regs[i + extra_args].first();
      VMReg dst_2 = out_regs[i + extra_args].second();
      if (src_1->is_stack()) {
        if (dst_1->is_stack()) {
          assert(dst_2->is_stack(), "must be");
          __ ldr(Rtemp, Address(FP, reg2offset_in(src_1)));
          __ ldr(LR,    Address(FP, reg2offset_in(src_2)));
          __ str(Rtemp, Address(SP, reg2offset_out(dst_1)));
          __ str(LR,    Address(SP, reg2offset_out(dst_2)));
        } else {
          // C2 Java calling convention does not populate S14 and S15, therefore
          // those need to be loaded from stack here
          __ fldd(dst_1->as_FloatRegister(), Address(FP, reg2offset_in(src_1)));
          fp_regs_in_arguments += 2;
        }
      } else {
        assert(src_1->is_FloatRegister() && src_2->is_FloatRegister(), "must be");
        fp_regs_in_arguments += 2;
      }
      break;
    }
#endif // __ABI_HARD__

    default: {
      assert(in_sig_bt[i] != T_ADDRESS, "found T_ADDRESS in java args");
      VMReg src = in_regs[i].first();
      VMReg dst = out_regs[i + extra_args].first();
      if (src->is_stack()) {
        assert(dst->is_stack(), "must be");
        __ ldr(Rtemp, Address(FP, reg2offset_in(src)));
        __ str(Rtemp, Address(SP, reg2offset_out(dst)));
      } else if (dst->is_stack()) {
        __ str(src->as_Register(), Address(SP, reg2offset_out(dst)));
      } else {
        assert(src->is_Register() && dst->is_Register(), "must be");
        __ mov(dst->as_Register(), src->as_Register());
      }
    }
    }
  }

  // Get Klass mirror
  int klass_offset = -1;
  if (is_static) {
    klass_offset = oop_handle_offset * VMRegImpl::stack_slot_size;
    __ mov_oop(Rtemp, JNIHandles::make_local(method->method_holder()->java_mirror()));
    __ add(c_rarg1, SP, klass_offset);
    __ str(Rtemp, Address(SP, klass_offset));
    map->set_oop(VMRegImpl::stack2reg(oop_handle_offset));
  }

  // the PC offset given to add_gc_map must match the PC saved in set_last_Java_frame
  int pc_offset = __ set_last_Java_frame(SP, FP, true, Rtemp);
  assert(((__ pc()) - start) == __ offset(), "warning: start differs from code_begin");
  oop_maps->add_gc_map(pc_offset, map);

  // Order last_Java_pc store with the thread state transition (to _thread_in_native)
  __ membar(MacroAssembler::StoreStore, Rtemp);

  // RedefineClasses() tracing support for obsolete method entry
  if (log_is_enabled(Trace, redefine, class, obsolete)) {
    __ save_caller_save_registers();
    __ mov(R0, Rthread);
    __ mov_metadata(R1, method());
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::rc_trace_method_entry), R0, R1);
    __ restore_caller_save_registers();
  }

  const Register sync_handle = R5;
  const Register sync_obj    = R6;
  const Register disp_hdr    = altFP_7_11;
  const Register tmp         = R8;

  Label slow_lock, lock_done, fast_lock;
  if (method->is_synchronized()) {
    // The first argument is a handle to sync object (a class or an instance)
    __ ldr(sync_obj, Address(R1));
    // Remember the handle for the unlocking code
    __ mov(sync_handle, R1);

    const Register mark = tmp;
    // On MP platforms the next load could return a 'stale' value if the memory location has been modified by another thread.
    // That would be acceptable as either CAS or slow case path is taken in that case

    __ ldr(mark, Address(sync_obj, oopDesc::mark_offset_in_bytes()));
    __ sub(disp_hdr, FP, lock_slot_fp_offset);
    __ tst(mark, markWord::unlocked_value);
    __ b(fast_lock, ne);

    // Check for recursive lock
    // See comments in InterpreterMacroAssembler::lock_object for
    // explanations on the fast recursive locking check.
    // Check independently the low bits and the distance to SP
    // -1- test low 2 bits
    __ movs(Rtemp, AsmOperand(mark, lsl, 30));
    // -2- test (hdr - SP) if the low two bits are 0
    __ sub(Rtemp, mark, SP, eq);
    __ movs(Rtemp, AsmOperand(Rtemp, lsr, exact_log2(os::vm_page_size())), eq);
    // If still 'eq' then recursive locking OK
    // set to zero if recursive lock, set to non zero otherwise (see discussion in JDK-8267042)
    __ str(Rtemp, Address(disp_hdr, BasicLock::displaced_header_offset_in_bytes()));
    __ b(lock_done, eq);
    __ b(slow_lock);

    __ bind(fast_lock);
    __ str(mark, Address(disp_hdr, BasicLock::displaced_header_offset_in_bytes()));

    __ cas_for_lock_acquire(mark, disp_hdr, sync_obj, Rtemp, slow_lock);

    __ bind(lock_done);
  }

  // Get JNIEnv*
  __ add(c_rarg0, Rthread, in_bytes(JavaThread::jni_environment_offset()));

  // Perform thread state transition
  __ mov(Rtemp, _thread_in_native);
  __ str(Rtemp, Address(Rthread, JavaThread::thread_state_offset()));

  // Finally, call the native method
  __ call(method->native_function());

  // Set FPSCR/FPCR to a known state
  if (AlwaysRestoreFPU) {
    __ restore_default_fp_mode();
  }

  // Ensure a Boolean result is mapped to 0..1
  if (ret_type == T_BOOLEAN) {
    __ c2bool(R0);
  }

  // Do a safepoint check while thread is in transition state
  Label call_safepoint_runtime, return_to_java;
  __ mov(Rtemp, _thread_in_native_trans);
  __ str_32(Rtemp, Address(Rthread, JavaThread::thread_state_offset()));

  // make sure the store is observed before reading the SafepointSynchronize state and further mem refs
  __ membar(MacroAssembler::Membar_mask_bits(MacroAssembler::StoreLoad | MacroAssembler::StoreStore), Rtemp);

  __ safepoint_poll(R2, call_safepoint_runtime);
  __ ldr_u32(R3, Address(Rthread, JavaThread::suspend_flags_offset()));
  __ cmp(R3, 0);
  __ b(call_safepoint_runtime, ne);

  __ bind(return_to_java);

  // Perform thread state transition and reguard stack yellow pages if needed
  Label reguard, reguard_done;
  __ mov(Rtemp, _thread_in_Java);
  __ ldr_s32(R2, Address(Rthread, JavaThread::stack_guard_state_offset()));
  __ str_32(Rtemp, Address(Rthread, JavaThread::thread_state_offset()));

  __ cmp(R2, StackOverflow::stack_guard_yellow_reserved_disabled);
  __ b(reguard, eq);
  __ bind(reguard_done);

  Label slow_unlock, unlock_done;
  if (method->is_synchronized()) {
    __ ldr(sync_obj, Address(sync_handle));

    // See C1_MacroAssembler::unlock_object() for more comments
    __ ldr(R2, Address(disp_hdr, BasicLock::displaced_header_offset_in_bytes()));
    __ cbz(R2, unlock_done);

    __ cas_for_lock_release(disp_hdr, R2, sync_obj, Rtemp, slow_unlock);

    __ bind(unlock_done);
  }

  // Set last java frame and handle block to zero
  __ ldr(LR, Address(Rthread, JavaThread::active_handles_offset()));
  __ reset_last_Java_frame(Rtemp); // sets Rtemp to 0 on 32-bit ARM

  __ str_32(Rtemp, Address(LR, JNIHandleBlock::top_offset_in_bytes()));
  if (CheckJNICalls) {
    __ str(__ zero_register(Rtemp), Address(Rthread, JavaThread::pending_jni_exception_check_fn_offset()));
  }

  // Unbox oop result, e.g. JNIHandles::resolve value in R0.
  if (ret_type == T_OBJECT || ret_type == T_ARRAY) {
    __ resolve_jobject(R0,      // value
                       Rtemp,   // tmp1
                       R1_tmp); // tmp2
  }

  // Any exception pending?
  __ ldr(Rtemp, Address(Rthread, Thread::pending_exception_offset()));
  __ mov(SP, FP);

  __ cmp(Rtemp, 0);
  // Pop the frame and return if no exception pending
  __ pop(RegisterSet(FP) | RegisterSet(PC), eq);
  // Pop the frame and forward the exception. Rexception_pc contains return address.
  __ ldr(FP, Address(SP, wordSize, post_indexed), ne);
  __ ldr(Rexception_pc, Address(SP, wordSize, post_indexed), ne);
  __ jump(StubRoutines::forward_exception_entry(), relocInfo::runtime_call_type, Rtemp);

  // Safepoint operation and/or pending suspend request is in progress.
  // Save the return values and call the runtime function by hand.
  __ bind(call_safepoint_runtime);
  push_result_registers(masm, ret_type);
  __ mov(R0, Rthread);
  __ call(CAST_FROM_FN_PTR(address, JavaThread::check_special_condition_for_native_trans));
  pop_result_registers(masm, ret_type);
  __ b(return_to_java);

  // Reguard stack pages. Save native results around a call to C runtime.
  __ bind(reguard);
  push_result_registers(masm, ret_type);
  __ call(CAST_FROM_FN_PTR(address, SharedRuntime::reguard_yellow_pages));
  pop_result_registers(masm, ret_type);
  __ b(reguard_done);

  if (method->is_synchronized()) {
    // Locking slow case
    __ bind(slow_lock);

    push_param_registers(masm, fp_regs_in_arguments);

    // last_Java_frame is already set, so do call_VM manually; no exception can occur
    __ mov(R0, sync_obj);
    __ mov(R1, disp_hdr);
    __ mov(R2, Rthread);
    __ call(CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_locking_C));

    pop_param_registers(masm, fp_regs_in_arguments);

    __ b(lock_done);

    // Unlocking slow case
    __ bind(slow_unlock);

    push_result_registers(masm, ret_type);

    // Clear pending exception before reentering VM.
    // Can store the oop in register since it is a leaf call.
    assert_different_registers(Rtmp_save1, sync_obj, disp_hdr);
    __ ldr(Rtmp_save1, Address(Rthread, Thread::pending_exception_offset()));
    Register zero = __ zero_register(Rtemp);
    __ str(zero, Address(Rthread, Thread::pending_exception_offset()));
    __ mov(R0, sync_obj);
    __ mov(R1, disp_hdr);
    __ mov(R2, Rthread);
    __ call(CAST_FROM_FN_PTR(address, SharedRuntime::complete_monitor_unlocking_C));
    __ str(Rtmp_save1, Address(Rthread, Thread::pending_exception_offset()));

    pop_result_registers(masm, ret_type);

    __ b(unlock_done);
  }

  __ flush();
  return nmethod::new_native_nmethod(method,
                                     compile_id,
                                     masm->code(),
                                     vep_offset,
                                     frame_complete,
                                     stack_slots / VMRegImpl::slots_per_word,
                                     in_ByteSize(is_static ? klass_offset : receiver_offset),
                                     in_ByteSize(lock_slot_offset * VMRegImpl::stack_slot_size),
                                     oop_maps);
}

// this function returns the adjust size (in number of words) to a c2i adapter
// activation for use during deoptimization
int Deoptimization::last_frame_adjust(int callee_parameters, int callee_locals) {
  int extra_locals_size = (callee_locals - callee_parameters) * Interpreter::stackElementWords;
  return extra_locals_size;
}


// Number of stack slots between incoming argument block and the start of
// a new frame.  The PROLOG must add this many slots to the stack.  The
// EPILOG must remove this many slots.
// FP + LR
uint SharedRuntime::in_preserve_stack_slots() {
  return 2 * VMRegImpl::slots_per_word;
}

uint SharedRuntime::out_preserve_stack_slots() {
  return 0;
}

//------------------------------generate_deopt_blob----------------------------
void SharedRuntime::generate_deopt_blob() {
  ResourceMark rm;
  CodeBuffer buffer("deopt_blob", 1024, 1024);
  int frame_size_in_words;
  OopMapSet* oop_maps;
  int reexecute_offset;
  int exception_in_tls_offset;
  int exception_offset;

  MacroAssembler* masm = new MacroAssembler(&buffer);
  Label cont;
  const Register Rkind   = R9; // caller-saved
  const Register Rublock = R6;
  const Register Rsender = altFP_7_11;
  assert_different_registers(Rkind, Rublock, Rsender, Rexception_obj, Rexception_pc, R0, R1, R2, R3, R8, Rtemp);

  address start = __ pc();

  oop_maps = new OopMapSet();
  // LR saved by caller (can be live in c2 method)

  // A deopt is a case where LR may be live in the c2 nmethod. So it's
  // not possible to call the deopt blob from the nmethod and pass the
  // address of the deopt handler of the nmethod in LR. What happens
  // now is that the caller of the deopt blob pushes the current
  // address so the deopt blob doesn't have to do it. This way LR can
  // be preserved, contains the live value from the nmethod and is
  // saved at R14/R30_offset here.
  OopMap* map = RegisterSaver::save_live_registers(masm, &frame_size_in_words, true);
  __ mov(Rkind, Deoptimization::Unpack_deopt);
  __ b(cont);

  exception_offset = __ pc() - start;

  // Transfer Rexception_obj & Rexception_pc in TLS and fall thru to the
  // exception_in_tls_offset entry point.
  __ str(Rexception_obj, Address(Rthread, JavaThread::exception_oop_offset()));
  __ str(Rexception_pc, Address(Rthread, JavaThread::exception_pc_offset()));
  // Force return value to NULL to avoid confusing the escape analysis
  // logic. Everything is dead here anyway.
  __ mov(R0, 0);

  exception_in_tls_offset = __ pc() - start;

  // Exception data is in JavaThread structure
  // Patch the return address of the current frame
  __ ldr(LR, Address(Rthread, JavaThread::exception_pc_offset()));
  (void) RegisterSaver::save_live_registers(masm, &frame_size_in_words);
  {
    const Register Rzero = __ zero_register(Rtemp); // XXX should be OK for C2 but not 100% sure
    __ str(Rzero, Address(Rthread, JavaThread::exception_pc_offset()));
  }
  __ mov(Rkind, Deoptimization::Unpack_exception);
  __ b(cont);

  reexecute_offset = __ pc() - start;

  (void) RegisterSaver::save_live_registers(masm, &frame_size_in_words);
  __ mov(Rkind, Deoptimization::Unpack_reexecute);

  // Calculate UnrollBlock and save the result in Rublock
  __ bind(cont);
  __ mov(R0, Rthread);
  __ mov(R1, Rkind);

  int pc_offset = __ set_last_Java_frame(SP, FP, false, Rtemp); // note: FP may not need to be saved (not on x86)
  assert(((__ pc()) - start) == __ offset(), "warning: start differs from code_begin");
  __ call(CAST_FROM_FN_PTR(address, Deoptimization::fetch_unroll_info));
  if (pc_offset == -1) {
    pc_offset = __ offset();
  }
  oop_maps->add_gc_map(pc_offset, map);
  __ reset_last_Java_frame(Rtemp); // Rtemp free since scratched by far call

  __ mov(Rublock, R0);

  // Reload Rkind from the UnrollBlock (might have changed)
  __ ldr_s32(Rkind, Address(Rublock, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()));
  Label noException;
  __ cmp_32(Rkind, Deoptimization::Unpack_exception);   // Was exception pending?
  __ b(noException, ne);
  // handle exception case
#ifdef ASSERT
  // assert that exception_pc is zero in tls
  { Label L;
    __ ldr(Rexception_pc, Address(Rthread, JavaThread::exception_pc_offset()));
    __ cbz(Rexception_pc, L);
    __ stop("exception pc should be null");
    __ bind(L);
  }
#endif
  __ ldr(Rexception_obj, Address(Rthread, JavaThread::exception_oop_offset()));
  __ verify_oop(Rexception_obj);
  {
    const Register Rzero = __ zero_register(Rtemp);
    __ str(Rzero, Address(Rthread, JavaThread::exception_oop_offset()));
  }

  __ bind(noException);

  // This frame is going away.  Fetch return value, so we can move it to
  // a new frame.
  __ ldr(R0, Address(SP, RegisterSaver::R0_offset * wordSize));
  __ ldr(R1, Address(SP, RegisterSaver::R1_offset * wordSize));
#ifndef __SOFTFP__
  __ ldr_double(D0, Address(SP, RegisterSaver::D0_offset * wordSize));
#endif
  // pop frame
  __ add(SP, SP, RegisterSaver::reg_save_size * wordSize);

  // Set initial stack state before pushing interpreter frames
  __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::size_of_deoptimized_frame_offset_in_bytes()));
  __ ldr(R2, Address(Rublock, Deoptimization::UnrollBlock::frame_pcs_offset_in_bytes()));
  __ ldr(R3, Address(Rublock, Deoptimization::UnrollBlock::frame_sizes_offset_in_bytes()));

  __ add(SP, SP, Rtemp);

#ifdef ASSERT
  // Compilers generate code that bang the stack by as much as the
  // interpreter would need. So this stack banging should never
  // trigger a fault. Verify that it does not on non product builds.
  // See if it is enough stack to push deoptimized frames.
  //
  // The compiled method that we are deoptimizing was popped from the stack.
  // If the stack bang results in a stack overflow, we don't return to the
  // method that is being deoptimized. The stack overflow exception is
  // propagated to the caller of the deoptimized method. Need to get the pc
  // from the caller in LR and restore FP.
  __ ldr(LR, Address(R2, 0));
  __ ldr(FP, Address(Rublock, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));
  __ ldr_s32(R8, Address(Rublock, Deoptimization::UnrollBlock::total_frame_sizes_offset_in_bytes()));
  __ arm_stack_overflow_check(R8, Rtemp);
#endif
  __ ldr_s32(R8, Address(Rublock, Deoptimization::UnrollBlock::number_of_frames_offset_in_bytes()));

  // Pick up the initial fp we should save
  // XXX Note: was ldr(FP, Address(FP));

  // The compiler no longer uses FP as a frame pointer for the
  // compiled code. It can be used by the allocator in C2 or to
  // memorize the original SP for JSR292 call sites.

  // Hence, ldr(FP, Address(FP)) is probably not correct. For x86,
  // Deoptimization::fetch_unroll_info computes the right FP value and
  // stores it in Rublock.initial_info. This has been activated for ARM.
  __ ldr(FP, Address(Rublock, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));

  __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::caller_adjustment_offset_in_bytes()));
  __ mov(Rsender, SP);
  __ sub(SP, SP, Rtemp);

  // Push interpreter frames in a loop
  Label loop;
  __ bind(loop);
  __ ldr(LR, Address(R2, wordSize, post_indexed));         // load frame pc
  __ ldr(Rtemp, Address(R3, wordSize, post_indexed));      // load frame size

  __ raw_push(FP, LR);                                     // create new frame
  __ mov(FP, SP);
  __ sub(Rtemp, Rtemp, 2*wordSize);

  __ sub(SP, SP, Rtemp);

  __ str(Rsender, Address(FP, frame::interpreter_frame_sender_sp_offset * wordSize));
  __ mov(LR, 0);
  __ str(LR, Address(FP, frame::interpreter_frame_last_sp_offset * wordSize));

  __ subs(R8, R8, 1);                               // decrement counter
  __ mov(Rsender, SP);
  __ b(loop, ne);

  // Re-push self-frame
  __ ldr(LR, Address(R2));
  __ raw_push(FP, LR);
  __ mov(FP, SP);
  __ sub(SP, SP, (frame_size_in_words - 2) * wordSize);

  // Restore frame locals after moving the frame
  __ str(R0, Address(SP, RegisterSaver::R0_offset * wordSize));
  __ str(R1, Address(SP, RegisterSaver::R1_offset * wordSize));

#ifndef __SOFTFP__
  __ str_double(D0, Address(SP, RegisterSaver::D0_offset * wordSize));
#endif // !__SOFTFP__

#ifdef ASSERT
  // Reload Rkind from the UnrollBlock and check that it was not overwritten (Rkind is not callee-saved)
  { Label L;
    __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()));
    __ cmp_32(Rkind, Rtemp);
    __ b(L, eq);
    __ stop("Rkind was overwritten");
    __ bind(L);
  }
#endif

  // Call unpack_frames with proper arguments
  __ mov(R0, Rthread);
  __ mov(R1, Rkind);

  pc_offset = __ set_last_Java_frame(SP, FP, true, Rtemp);
  assert(((__ pc()) - start) == __ offset(), "warning: start differs from code_begin");
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames));
  if (pc_offset == -1) {
    pc_offset = __ offset();
  }
  oop_maps->add_gc_map(pc_offset, new OopMap(frame_size_in_words * VMRegImpl::slots_per_word, 0));
  __ reset_last_Java_frame(Rtemp); // Rtemp free since scratched by far call

  // Collect return values, pop self-frame and jump to interpreter
  __ ldr(R0, Address(SP, RegisterSaver::R0_offset * wordSize));
  __ ldr(R1, Address(SP, RegisterSaver::R1_offset * wordSize));
  // Interpreter floats controlled by __SOFTFP__, but compiler
  // float return value registers controlled by __ABI_HARD__
  // This matters for vfp-sflt builds.
#ifndef __SOFTFP__
  // Interpreter hard float
#ifdef __ABI_HARD__
  // Compiler float return value in FP registers
  __ ldr_double(D0, Address(SP, RegisterSaver::D0_offset * wordSize));
#else
  // Compiler float return value in integer registers,
  // copy to D0 for interpreter (S0 <-- R0)
  __ fmdrr(D0_tos, R0, R1);
#endif
#endif // !__SOFTFP__
  __ mov(SP, FP);

  __ pop(RegisterSet(FP) | RegisterSet(PC));

  __ flush();

  _deopt_blob = DeoptimizationBlob::create(&buffer, oop_maps, 0, exception_offset,
                                           reexecute_offset, frame_size_in_words);
  _deopt_blob->set_unpack_with_exception_in_tls_offset(exception_in_tls_offset);
}

#ifdef COMPILER2

//------------------------------generate_uncommon_trap_blob--------------------
// Ought to generate an ideal graph & compile, but here's some ASM
// instead.
void SharedRuntime::generate_uncommon_trap_blob() {
  // allocate space for the code
  ResourceMark rm;

  // setup code generation tools
  int pad = VerifyThread ? 512 : 0;
#ifdef _LP64
  CodeBuffer buffer("uncommon_trap_blob", 2700+pad, 512);
#else
  // Measured 8/7/03 at 660 in 32bit debug build (no VerifyThread)
  // Measured 8/7/03 at 1028 in 32bit debug build (VerifyThread)
  CodeBuffer buffer("uncommon_trap_blob", 2000+pad, 512);
#endif
  // bypassed when code generation useless
  MacroAssembler* masm               = new MacroAssembler(&buffer);
  const Register Rublock = R6;
  const Register Rsender = altFP_7_11;
  assert_different_registers(Rublock, Rsender, Rexception_obj, R0, R1, R2, R3, R8, Rtemp);

  //
  // This is the entry point for all traps the compiler takes when it thinks
  // it cannot handle further execution of compilation code. The frame is
  // deoptimized in these cases and converted into interpreter frames for
  // execution
  // The steps taken by this frame are as follows:
  //   - push a fake "unpack_frame"
  //   - call the C routine Deoptimization::uncommon_trap (this function
  //     packs the current compiled frame into vframe arrays and returns
  //     information about the number and size of interpreter frames which
  //     are equivalent to the frame which is being deoptimized)
  //   - deallocate the "unpack_frame"
  //   - deallocate the deoptimization frame
  //   - in a loop using the information returned in the previous step
  //     push interpreter frames;
  //   - create a dummy "unpack_frame"
  //   - call the C routine: Deoptimization::unpack_frames (this function
  //     lays out values on the interpreter frame which was just created)
  //   - deallocate the dummy unpack_frame
  //   - return to the interpreter entry point
  //
  //  Refer to the following methods for more information:
  //   - Deoptimization::uncommon_trap
  //   - Deoptimization::unpack_frame

  // the unloaded class index is in R0 (first parameter to this blob)

  __ raw_push(FP, LR);
  __ set_last_Java_frame(SP, FP, false, Rtemp);
  __ mov(R2, Deoptimization::Unpack_uncommon_trap);
  __ mov(R1, R0);
  __ mov(R0, Rthread);
  __ call(CAST_FROM_FN_PTR(address, Deoptimization::uncommon_trap));
  __ mov(Rublock, R0);
  __ reset_last_Java_frame(Rtemp);
  __ raw_pop(FP, LR);

#ifdef ASSERT
  { Label L;
    __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::unpack_kind_offset_in_bytes()));
    __ cmp_32(Rtemp, Deoptimization::Unpack_uncommon_trap);
    __ b(L, eq);
    __ stop("SharedRuntime::generate_uncommon_trap_blob: expected Unpack_uncommon_trap");
    __ bind(L);
  }
#endif


  // Set initial stack state before pushing interpreter frames
  __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::size_of_deoptimized_frame_offset_in_bytes()));
  __ ldr(R2, Address(Rublock, Deoptimization::UnrollBlock::frame_pcs_offset_in_bytes()));
  __ ldr(R3, Address(Rublock, Deoptimization::UnrollBlock::frame_sizes_offset_in_bytes()));

  __ add(SP, SP, Rtemp);

  // See if it is enough stack to push deoptimized frames.
#ifdef ASSERT
  // Compilers generate code that bang the stack by as much as the
  // interpreter would need. So this stack banging should never
  // trigger a fault. Verify that it does not on non product builds.
  //
  // The compiled method that we are deoptimizing was popped from the stack.
  // If the stack bang results in a stack overflow, we don't return to the
  // method that is being deoptimized. The stack overflow exception is
  // propagated to the caller of the deoptimized method. Need to get the pc
  // from the caller in LR and restore FP.
  __ ldr(LR, Address(R2, 0));
  __ ldr(FP, Address(Rublock, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));
  __ ldr_s32(R8, Address(Rublock, Deoptimization::UnrollBlock::total_frame_sizes_offset_in_bytes()));
  __ arm_stack_overflow_check(R8, Rtemp);
#endif
  __ ldr_s32(R8, Address(Rublock, Deoptimization::UnrollBlock::number_of_frames_offset_in_bytes()));
  __ ldr_s32(Rtemp, Address(Rublock, Deoptimization::UnrollBlock::caller_adjustment_offset_in_bytes()));
  __ mov(Rsender, SP);
  __ sub(SP, SP, Rtemp);
  //  __ ldr(FP, Address(FP));
  __ ldr(FP, Address(Rublock, Deoptimization::UnrollBlock::initial_info_offset_in_bytes()));

  // Push interpreter frames in a loop
  Label loop;
  __ bind(loop);
  __ ldr(LR, Address(R2, wordSize, post_indexed));         // load frame pc
  __ ldr(Rtemp, Address(R3, wordSize, post_indexed));      // load frame size

  __ raw_push(FP, LR);                                     // create new frame
  __ mov(FP, SP);
  __ sub(Rtemp, Rtemp, 2*wordSize);

  __ sub(SP, SP, Rtemp);

  __ str(Rsender, Address(FP, frame::interpreter_frame_sender_sp_offset * wordSize));
  __ mov(LR, 0);
  __ str(LR, Address(FP, frame::interpreter_frame_last_sp_offset * wordSize));
  __ subs(R8, R8, 1);                               // decrement counter
  __ mov(Rsender, SP);
  __ b(loop, ne);

  // Re-push self-frame
  __ ldr(LR, Address(R2));
  __ raw_push(FP, LR);
  __ mov(FP, SP);

  // Call unpack_frames with proper arguments
  __ mov(R0, Rthread);
  __ mov(R1, Deoptimization::Unpack_uncommon_trap);
  __ set_last_Java_frame(SP, FP, true, Rtemp);
  __ call_VM_leaf(CAST_FROM_FN_PTR(address, Deoptimization::unpack_frames));
  //  oop_maps->add_gc_map(__ pc() - start, new OopMap(frame_size_in_words, 0));
  __ reset_last_Java_frame(Rtemp);

  __ mov(SP, FP);
  __ pop(RegisterSet(FP) | RegisterSet(PC));

  masm->flush();
  _uncommon_trap_blob = UncommonTrapBlob::create(&buffer, NULL, 2 /* LR+FP */);
}

#endif // COMPILER2

//------------------------------generate_handler_blob------
//
// Generate a special Compile2Runtime blob that saves all registers,
// setup oopmap, and calls safepoint code to stop the compiled code for
// a safepoint.
//
SafepointBlob* SharedRuntime::generate_handler_blob(address call_ptr, int poll_type) {
  assert(StubRoutines::forward_exception_entry() != NULL, "must be generated before");

  ResourceMark rm;
  CodeBuffer buffer("handler_blob", 256, 256);
  int frame_size_words;
  OopMapSet* oop_maps;

  bool cause_return = (poll_type == POLL_AT_RETURN);

  MacroAssembler* masm = new MacroAssembler(&buffer);
  address start = __ pc();
  oop_maps = new OopMapSet();

  if (!cause_return) {
    __ sub(SP, SP, 4); // make room for LR which may still be live
                       // here if we are coming from a c2 method
  }

  OopMap* map = RegisterSaver::save_live_registers(masm, &frame_size_words, !cause_return);
  if (!cause_return) {
    // update saved PC with correct value
    // need 2 steps because LR can be live in c2 method
    __ ldr(LR, Address(Rthread, JavaThread::saved_exception_pc_offset()));
    __ str(LR, Address(SP, RegisterSaver::LR_offset * wordSize));
  }

  __ mov(R0, Rthread);
  int pc_offset = __ set_last_Java_frame(SP, FP, false, Rtemp); // note: FP may not need to be saved (not on x86)
  assert(((__ pc()) - start) == __ offset(), "warning: start differs from code_begin");
  __ call(call_ptr);
  if (pc_offset == -1) {
    pc_offset = __ offset();
  }
  oop_maps->add_gc_map(pc_offset, map);
  __ reset_last_Java_frame(Rtemp); // Rtemp free since scratched by far call

  if (!cause_return) {
    // If our stashed return pc was modified by the runtime we avoid touching it
    __ ldr(R3_tmp, Address(Rthread, JavaThread::saved_exception_pc_offset()));
    __ ldr(R2_tmp, Address(SP, RegisterSaver::LR_offset * wordSize));
    __ cmp(R2_tmp, R3_tmp);
    // Adjust return pc forward to step over the safepoint poll instruction
    __ add(R2_tmp, R2_tmp, 4, eq);
    __ str(R2_tmp, Address(SP, RegisterSaver::LR_offset * wordSize), eq);

    // Check for pending exception
    __ ldr(Rtemp, Address(Rthread, Thread::pending_exception_offset()));
    __ cmp(Rtemp, 0);

    RegisterSaver::restore_live_registers(masm, false);
    __ pop(PC, eq);
    __ pop(Rexception_pc);
  } else {
    // Check for pending exception
    __ ldr(Rtemp, Address(Rthread, Thread::pending_exception_offset()));
    __ cmp(Rtemp, 0);

    RegisterSaver::restore_live_registers(masm);
    __ bx(LR, eq);
    __ mov(Rexception_pc, LR);
  }

  __ jump(StubRoutines::forward_exception_entry(), relocInfo::runtime_call_type, Rtemp);

  __ flush();

  return SafepointBlob::create(&buffer, oop_maps, frame_size_words);
}

RuntimeStub* SharedRuntime::generate_resolve_blob(address destination, const char* name) {
  assert(StubRoutines::forward_exception_entry() != NULL, "must be generated before");

  ResourceMark rm;
  CodeBuffer buffer(name, 1000, 512);
  int frame_size_words;
  OopMapSet *oop_maps;
  int frame_complete;

  MacroAssembler* masm = new MacroAssembler(&buffer);
  Label pending_exception;

  int start = __ offset();

  oop_maps = new OopMapSet();
  OopMap* map = RegisterSaver::save_live_registers(masm, &frame_size_words);

  frame_complete = __ offset();

  __ mov(R0, Rthread);

  int pc_offset = __ set_last_Java_frame(SP, FP, false, Rtemp);
  assert(start == 0, "warning: start differs from code_begin");
  __ call(destination);
  if (pc_offset == -1) {
    pc_offset = __ offset();
  }
  oop_maps->add_gc_map(pc_offset, map);
  __ reset_last_Java_frame(Rtemp); // Rtemp free since scratched by far call

  __ ldr(R1, Address(Rthread, Thread::pending_exception_offset()));
  __ cbnz(R1, pending_exception);

  // Overwrite saved register values

  // Place metadata result of VM call into Rmethod
  __ get_vm_result_2(R1, Rtemp);
  __ str(R1, Address(SP, RegisterSaver::Rmethod_offset * wordSize));

  // Place target address (VM call result) into Rtemp
  __ str(R0, Address(SP, RegisterSaver::Rtemp_offset * wordSize));

  RegisterSaver::restore_live_registers(masm);
  __ jump(Rtemp);

  __ bind(pending_exception);

  RegisterSaver::restore_live_registers(masm);
  const Register Rzero = __ zero_register(Rtemp);
  __ str(Rzero, Address(Rthread, JavaThread::vm_result_2_offset()));
  __ mov(Rexception_pc, LR);
  __ jump(StubRoutines::forward_exception_entry(), relocInfo::runtime_call_type, Rtemp);

  __ flush();

  return RuntimeStub::new_runtime_stub(name, &buffer, frame_complete, frame_size_words, oop_maps, true);
}

#ifdef COMPILER2
RuntimeStub* SharedRuntime::make_native_invoker(address call_target,
                                                int shadow_space_bytes,
                                                const GrowableArray<VMReg>& input_registers,
                                                const GrowableArray<VMReg>& output_registers) {
  Unimplemented();
  return nullptr;
}
#endif
