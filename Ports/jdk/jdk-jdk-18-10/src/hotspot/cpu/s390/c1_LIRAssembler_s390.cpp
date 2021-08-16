/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2019 SAP SE. All rights reserved.
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
#include "c1/c1_Compilation.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArrayKlass.hpp"
#include "ci/ciInstance.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/universe.hpp"
#include "nativeInst_s390.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_s390.inline.hpp"

#define __ _masm->

#ifndef PRODUCT
#undef __
#define __ (Verbose ? (_masm->block_comment(FILE_AND_LINE),_masm) : _masm)->
#endif

//------------------------------------------------------------

bool LIR_Assembler::is_small_constant(LIR_Opr opr) {
  // Not used on ZARCH_64
  ShouldNotCallThis();
  return false;
}

LIR_Opr LIR_Assembler::receiverOpr() {
  return FrameMap::Z_R2_oop_opr;
}

LIR_Opr LIR_Assembler::osrBufferPointer() {
  return FrameMap::Z_R2_opr;
}

int LIR_Assembler::initial_frame_size_in_bytes() const {
  return in_bytes(frame_map()->framesize_in_bytes());
}

// Inline cache check: done before the frame is built.
// The inline cached class is in Z_inline_cache(Z_R9).
// We fetch the class of the receiver and compare it with the cached class.
// If they do not match we jump to the slow case.
int LIR_Assembler::check_icache() {
  Register receiver = receiverOpr()->as_register();
  int offset = __ offset();
  __ inline_cache_check(receiver, Z_inline_cache);
  return offset;
}

void LIR_Assembler::clinit_barrier(ciMethod* method) {
  assert(!method->holder()->is_not_initialized(), "initialization should have been started");

  Label L_skip_barrier;
  Register klass = Z_R1_scratch;

  metadata2reg(method->holder()->constant_encoding(), klass);
  __ clinit_barrier(klass, Z_thread, &L_skip_barrier /*L_fast_path*/);

  __ load_const_optimized(klass, SharedRuntime::get_handle_wrong_method_stub());
  __ z_br(klass);

  __ bind(L_skip_barrier);
}

void LIR_Assembler::osr_entry() {
  // On-stack-replacement entry sequence (interpreter frame layout described in frame_s390.hpp):
  //
  //   1. Create a new compiled activation.
  //   2. Initialize local variables in the compiled activation. The expression stack must be empty
  //      at the osr_bci; it is not initialized.
  //   3. Jump to the continuation address in compiled code to resume execution.

  // OSR entry point
  offsets()->set_value(CodeOffsets::OSR_Entry, code_offset());
  BlockBegin* osr_entry = compilation()->hir()->osr_entry();
  ValueStack* entry_state = osr_entry->end()->state();
  int number_of_locks = entry_state->locks_size();

  // Create a frame for the compiled activation.
  __ build_frame(initial_frame_size_in_bytes(), bang_size_in_bytes());

  // OSR buffer is
  //
  // locals[nlocals-1..0]
  // monitors[number_of_locks-1..0]
  //
  // Locals is a direct copy of the interpreter frame so in the osr buffer
  // the first slot in the local array is the last local from the interpreter
  // and the last slot is local[0] (receiver) from the interpreter
  //
  // Similarly with locks. The first lock slot in the osr buffer is the nth lock
  // from the interpreter frame, the nth lock slot in the osr buffer is 0th lock
  // in the interpreter frame (the method lock if a sync method)

  // Initialize monitors in the compiled activation.
  //   I0: pointer to osr buffer
  //
  // All other registers are dead at this point and the locals will be
  // copied into place by code emitted in the IR.

  Register OSR_buf = osrBufferPointer()->as_register();
  { assert(frame::interpreter_frame_monitor_size() == BasicObjectLock::size(), "adjust code below");
    int monitor_offset = BytesPerWord * method()->max_locals() +
      (2 * BytesPerWord) * (number_of_locks - 1);
    // SharedRuntime::OSR_migration_begin() packs BasicObjectLocks in
    // the OSR buffer using 2 word entries: first the lock and then
    // the oop.
    for (int i = 0; i < number_of_locks; i++) {
      int slot_offset = monitor_offset - ((i * 2) * BytesPerWord);
      // Verify the interpreter's monitor has a non-null object.
      __ asm_assert_mem8_isnot_zero(slot_offset + 1*BytesPerWord, OSR_buf, "locked object is NULL", __LINE__);
      // Copy the lock field into the compiled activation.
      __ z_lg(Z_R1_scratch, slot_offset + 0, OSR_buf);
      __ z_stg(Z_R1_scratch, frame_map()->address_for_monitor_lock(i));
      __ z_lg(Z_R1_scratch, slot_offset + 1*BytesPerWord, OSR_buf);
      __ z_stg(Z_R1_scratch, frame_map()->address_for_monitor_object(i));
    }
  }
}

// --------------------------------------------------------------------------------------------

address LIR_Assembler::emit_call_c(address a) {
  __ align_call_far_patchable(__ pc());
  address call_addr = __ call_c_opt(a);
  if (call_addr == NULL) {
    bailout("const section overflow");
  }
  return call_addr;
}

int LIR_Assembler::emit_exception_handler() {
  // If the last instruction is a call (typically to do a throw which
  // is coming at the end after block reordering) the return address
  // must still point into the code area in order to avoid assertion
  // failures when searching for the corresponding bci. => Add a nop.
  // (was bug 5/14/1999 - gri)
  __ nop();

  // Generate code for exception handler.
  address handler_base = __ start_a_stub(exception_handler_size());
  if (handler_base == NULL) {
    // Not enough space left for the handler.
    bailout("exception handler overflow");
    return -1;
  }

  int offset = code_offset();

  address a = Runtime1::entry_for (Runtime1::handle_exception_from_callee_id);
  address call_addr = emit_call_c(a);
  CHECK_BAILOUT_(-1);
  __ should_not_reach_here();
  guarantee(code_offset() - offset <= exception_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}

// Emit the code to remove the frame from the stack in the exception
// unwind path.
int LIR_Assembler::emit_unwind_handler() {
#ifndef PRODUCT
  if (CommentedAssembly) {
    _masm->block_comment("Unwind handler");
  }
#endif

  int offset = code_offset();
  Register exception_oop_callee_saved = Z_R10; // Z_R10 is callee-saved.
  Register Rtmp1                      = Z_R11;
  Register Rtmp2                      = Z_R12;

  // Fetch the exception from TLS and clear out exception related thread state.
  Address exc_oop_addr = Address(Z_thread, JavaThread::exception_oop_offset());
  Address exc_pc_addr  = Address(Z_thread, JavaThread::exception_pc_offset());
  __ z_lg(Z_EXC_OOP, exc_oop_addr);
  __ clear_mem(exc_oop_addr, sizeof(oop));
  __ clear_mem(exc_pc_addr, sizeof(intptr_t));

  __ bind(_unwind_handler_entry);
  __ verify_not_null_oop(Z_EXC_OOP);
  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ lgr_if_needed(exception_oop_callee_saved, Z_EXC_OOP); // Preserve the exception.
  }

  // Preform needed unlocking.
  MonitorExitStub* stub = NULL;
  if (method()->is_synchronized()) {
    // Runtime1::monitorexit_id expects lock address in Z_R1_scratch.
    LIR_Opr lock = FrameMap::as_opr(Z_R1_scratch);
    monitor_address(0, lock);
    stub = new MonitorExitStub(lock, true, 0);
    __ unlock_object(Rtmp1, Rtmp2, lock->as_register(), *stub->entry());
    __ bind(*stub->continuation());
  }

  if (compilation()->env()->dtrace_method_probes()) {
    ShouldNotReachHere(); // Not supported.
#if 0
    __ mov(rdi, r15_thread);
    __ mov_metadata(rsi, method()->constant_encoding());
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit)));
#endif
  }

  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ lgr_if_needed(Z_EXC_OOP, exception_oop_callee_saved);  // Restore the exception.
  }

  // Remove the activation and dispatch to the unwind handler.
  __ pop_frame();
  __ z_lg(Z_EXC_PC, _z_abi16(return_pc), Z_SP);

  // Z_EXC_OOP: exception oop
  // Z_EXC_PC: exception pc

  // Dispatch to the unwind logic.
  __ load_const_optimized(Z_R5, Runtime1::entry_for (Runtime1::unwind_exception_id));
  __ z_br(Z_R5);

  // Emit the slow path assembly.
  if (stub != NULL) {
    stub->emit_code(this);
  }

  return offset;
}

int LIR_Assembler::emit_deopt_handler() {
  // If the last instruction is a call (typically to do a throw which
  // is coming at the end after block reordering) the return address
  // must still point into the code area in order to avoid assertion
  // failures when searching for the corresponding bci. => Add a nop.
  // (was bug 5/14/1999 - gri)
  __ nop();

  // Generate code for exception handler.
  address handler_base = __ start_a_stub(deopt_handler_size());
  if (handler_base == NULL) {
    // Not enough space left for the handler.
    bailout("deopt handler overflow");
    return -1;
  }  int offset = code_offset();
  // Size must be constant (see HandlerImpl::emit_deopt_handler).
  __ load_const(Z_R1_scratch, SharedRuntime::deopt_blob()->unpack());
  __ call(Z_R1_scratch);
  guarantee(code_offset() - offset <= deopt_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}

void LIR_Assembler::jobject2reg(jobject o, Register reg) {
  if (o == NULL) {
    __ clear_reg(reg, true/*64bit*/, false/*set cc*/); // Must not kill cc set by cmove.
  } else {
    AddressLiteral a = __ allocate_oop_address(o);
    bool success = __ load_oop_from_toc(reg, a, reg);
    if (!success) {
      bailout("const section overflow");
    }
  }
}

void LIR_Assembler::jobject2reg_with_patching(Register reg, CodeEmitInfo *info) {
  // Allocate a new index in table to hold the object once it's been patched.
  int oop_index = __ oop_recorder()->allocate_oop_index(NULL);
  PatchingStub* patch = new PatchingStub(_masm, patching_id(info), oop_index);

  AddressLiteral addrlit((intptr_t)0, oop_Relocation::spec(oop_index));
  assert(addrlit.rspec().type() == relocInfo::oop_type, "must be an oop reloc");
  // The NULL will be dynamically patched later so the sequence to
  // load the address literal must not be optimized.
  __ load_const(reg, addrlit);

  patching_epilog(patch, lir_patch_normal, reg, info);
}

void LIR_Assembler::metadata2reg(Metadata* md, Register reg) {
  bool success = __ set_metadata_constant(md, reg);
  if (!success) {
    bailout("const section overflow");
    return;
  }
}

void LIR_Assembler::klass2reg_with_patching(Register reg, CodeEmitInfo *info) {
  // Allocate a new index in table to hold the klass once it's been patched.
  int index = __ oop_recorder()->allocate_metadata_index(NULL);
  PatchingStub* patch = new PatchingStub(_masm, PatchingStub::load_klass_id, index);
  AddressLiteral addrlit((intptr_t)0, metadata_Relocation::spec(index));
  assert(addrlit.rspec().type() == relocInfo::metadata_type, "must be an metadata reloc");
  // The NULL will be dynamically patched later so the sequence to
  // load the address literal must not be optimized.
  __ load_const(reg, addrlit);

  patching_epilog(patch, lir_patch_normal, reg, info);
}

void LIR_Assembler::emit_op3(LIR_Op3* op) {
  switch (op->code()) {
    case lir_idiv:
    case lir_irem:
      arithmetic_idiv(op->code(),
                      op->in_opr1(),
                      op->in_opr2(),
                      op->in_opr3(),
                      op->result_opr(),
                      op->info());
      break;
    case lir_fmad: {
      const FloatRegister opr1 = op->in_opr1()->as_double_reg(),
                          opr2 = op->in_opr2()->as_double_reg(),
                          opr3 = op->in_opr3()->as_double_reg(),
                          res  = op->result_opr()->as_double_reg();
      __ z_madbr(opr3, opr1, opr2);
      if (res != opr3) { __ z_ldr(res, opr3); }
    } break;
    case lir_fmaf: {
      const FloatRegister opr1 = op->in_opr1()->as_float_reg(),
                          opr2 = op->in_opr2()->as_float_reg(),
                          opr3 = op->in_opr3()->as_float_reg(),
                          res  = op->result_opr()->as_float_reg();
      __ z_maebr(opr3, opr1, opr2);
      if (res != opr3) { __ z_ler(res, opr3); }
    } break;
    default: ShouldNotReachHere(); break;
  }
}


void LIR_Assembler::emit_opBranch(LIR_OpBranch* op) {
#ifdef ASSERT
  assert(op->block() == NULL || op->block()->label() == op->label(), "wrong label");
  if (op->block() != NULL)  { _branch_target_blocks.append(op->block()); }
  if (op->ublock() != NULL) { _branch_target_blocks.append(op->ublock()); }
#endif

  if (op->cond() == lir_cond_always) {
    if (op->info() != NULL) { add_debug_info_for_branch(op->info()); }
    __ branch_optimized(Assembler::bcondAlways, *(op->label()));
  } else {
    Assembler::branch_condition acond = Assembler::bcondZero;
    if (op->code() == lir_cond_float_branch) {
      assert(op->ublock() != NULL, "must have unordered successor");
      __ branch_optimized(Assembler::bcondNotOrdered, *(op->ublock()->label()));
    }
    switch (op->cond()) {
      case lir_cond_equal:        acond = Assembler::bcondEqual;     break;
      case lir_cond_notEqual:     acond = Assembler::bcondNotEqual;  break;
      case lir_cond_less:         acond = Assembler::bcondLow;       break;
      case lir_cond_lessEqual:    acond = Assembler::bcondNotHigh;   break;
      case lir_cond_greaterEqual: acond = Assembler::bcondNotLow;    break;
      case lir_cond_greater:      acond = Assembler::bcondHigh;      break;
      case lir_cond_belowEqual:   acond = Assembler::bcondNotHigh;   break;
      case lir_cond_aboveEqual:   acond = Assembler::bcondNotLow;    break;
      default:                         ShouldNotReachHere();
    }
    __ branch_optimized(acond,*(op->label()));
  }
}


void LIR_Assembler::emit_opConvert(LIR_OpConvert* op) {
  LIR_Opr src  = op->in_opr();
  LIR_Opr dest = op->result_opr();

  switch (op->bytecode()) {
    case Bytecodes::_i2l:
      __ move_reg_if_needed(dest->as_register_lo(), T_LONG, src->as_register(), T_INT);
      break;

    case Bytecodes::_l2i:
      __ move_reg_if_needed(dest->as_register(), T_INT, src->as_register_lo(), T_LONG);
      break;

    case Bytecodes::_i2b:
      __ move_reg_if_needed(dest->as_register(), T_BYTE, src->as_register(), T_INT);
      break;

    case Bytecodes::_i2c:
      __ move_reg_if_needed(dest->as_register(), T_CHAR, src->as_register(), T_INT);
      break;

    case Bytecodes::_i2s:
      __ move_reg_if_needed(dest->as_register(), T_SHORT, src->as_register(), T_INT);
      break;

    case Bytecodes::_f2d:
      assert(dest->is_double_fpu(), "check");
      __ move_freg_if_needed(dest->as_double_reg(), T_DOUBLE, src->as_float_reg(), T_FLOAT);
      break;

    case Bytecodes::_d2f:
      assert(dest->is_single_fpu(), "check");
      __ move_freg_if_needed(dest->as_float_reg(), T_FLOAT, src->as_double_reg(), T_DOUBLE);
      break;

    case Bytecodes::_i2f:
      __ z_cefbr(dest->as_float_reg(), src->as_register());
      break;

    case Bytecodes::_i2d:
      __ z_cdfbr(dest->as_double_reg(), src->as_register());
      break;

    case Bytecodes::_l2f:
      __ z_cegbr(dest->as_float_reg(), src->as_register_lo());
      break;
    case Bytecodes::_l2d:
      __ z_cdgbr(dest->as_double_reg(), src->as_register_lo());
      break;

    case Bytecodes::_f2i:
    case Bytecodes::_f2l: {
      Label done;
      FloatRegister Rsrc = src->as_float_reg();
      Register Rdst = (op->bytecode() == Bytecodes::_f2i ? dest->as_register() : dest->as_register_lo());
      __ clear_reg(Rdst, true, false);
      __ z_cebr(Rsrc, Rsrc);
      __ z_brno(done); // NaN -> 0
      if (op->bytecode() == Bytecodes::_f2i) {
        __ z_cfebr(Rdst, Rsrc, Assembler::to_zero);
      } else { // op->bytecode() == Bytecodes::_f2l
        __ z_cgebr(Rdst, Rsrc, Assembler::to_zero);
      }
      __ bind(done);
    }
    break;

    case Bytecodes::_d2i:
    case Bytecodes::_d2l: {
      Label done;
      FloatRegister Rsrc = src->as_double_reg();
      Register Rdst = (op->bytecode() == Bytecodes::_d2i ? dest->as_register() : dest->as_register_lo());
      __ clear_reg(Rdst, true, false);  // Don't set CC.
      __ z_cdbr(Rsrc, Rsrc);
      __ z_brno(done); // NaN -> 0
      if (op->bytecode() == Bytecodes::_d2i) {
        __ z_cfdbr(Rdst, Rsrc, Assembler::to_zero);
      } else { // Bytecodes::_d2l
        __ z_cgdbr(Rdst, Rsrc, Assembler::to_zero);
      }
      __ bind(done);
    }
    break;

    default: ShouldNotReachHere();
  }
}

void LIR_Assembler::align_call(LIR_Code code) {
  // End of call instruction must be 4 byte aligned.
  int offset = __ offset();
  switch (code) {
    case lir_icvirtual_call:
      offset += MacroAssembler::load_const_from_toc_size();
      // no break
    case lir_static_call:
    case lir_optvirtual_call:
    case lir_dynamic_call:
      offset += NativeCall::call_far_pcrelative_displacement_offset;
      break;
    default: ShouldNotReachHere();
  }
  if ((offset & (NativeCall::call_far_pcrelative_displacement_alignment-1)) != 0) {
    __ nop();
  }
}

void LIR_Assembler::call(LIR_OpJavaCall* op, relocInfo::relocType rtype) {
  assert((__ offset() + NativeCall::call_far_pcrelative_displacement_offset) % NativeCall::call_far_pcrelative_displacement_alignment == 0,
         "must be aligned (offset=%d)", __ offset());
  assert(rtype == relocInfo::none ||
         rtype == relocInfo::opt_virtual_call_type ||
         rtype == relocInfo::static_call_type, "unexpected rtype");
  // Prepend each BRASL with a nop.
  __ relocate(rtype);
  __ z_nop();
  __ z_brasl(Z_R14, op->addr());
  add_call_info(code_offset(), op->info());
}

void LIR_Assembler::ic_call(LIR_OpJavaCall* op) {
  address virtual_call_oop_addr = NULL;
  AddressLiteral empty_ic((address) Universe::non_oop_word());
  virtual_call_oop_addr = __ pc();
  bool success = __ load_const_from_toc(Z_inline_cache, empty_ic);
  if (!success) {
    bailout("const section overflow");
    return;
  }

  // CALL to fixup routine. Fixup routine uses ScopeDesc info
  // to determine who we intended to call.
  __ relocate(virtual_call_Relocation::spec(virtual_call_oop_addr));
  call(op, relocInfo::none);
}

void LIR_Assembler::move_regs(Register from_reg, Register to_reg) {
  if (from_reg != to_reg) __ z_lgr(to_reg, from_reg);
}

void LIR_Assembler::const2stack(LIR_Opr src, LIR_Opr dest) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_stack(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();

  unsigned int lmem = 0;
  unsigned int lcon = 0;
  int64_t cbits = 0;
  Address dest_addr;
  switch (c->type()) {
    case T_INT:  // fall through
    case T_FLOAT:
      dest_addr = frame_map()->address_for_slot(dest->single_stack_ix());
      lmem = 4; lcon = 4; cbits = c->as_jint_bits();
      break;

    case T_ADDRESS:
      dest_addr = frame_map()->address_for_slot(dest->single_stack_ix());
      lmem = 8; lcon = 4; cbits = c->as_jint_bits();
      break;

    case T_OBJECT:
      dest_addr = frame_map()->address_for_slot(dest->single_stack_ix());
      if (c->as_jobject() == NULL) {
        __ store_const(dest_addr, (int64_t)NULL_WORD, 8, 8);
      } else {
        jobject2reg(c->as_jobject(), Z_R1_scratch);
        __ reg2mem_opt(Z_R1_scratch, dest_addr, true);
      }
      return;

    case T_LONG:  // fall through
    case T_DOUBLE:
      dest_addr = frame_map()->address_for_slot(dest->double_stack_ix());
      lmem = 8; lcon = 8; cbits = (int64_t)(c->as_jlong_bits());
      break;

    default:
      ShouldNotReachHere();
  }

  __ store_const(dest_addr, cbits, lmem, lcon);
}

void LIR_Assembler::const2mem(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info, bool wide) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_address(), "should not call otherwise");

  LIR_Const* c = src->as_constant_ptr();
  Address addr = as_Address(dest->as_address_ptr());

  int store_offset = -1;

  if (dest->as_address_ptr()->index()->is_valid()) {
    switch (type) {
      case T_INT:    // fall through
      case T_FLOAT:
        __ load_const_optimized(Z_R0_scratch, c->as_jint_bits());
        store_offset = __ offset();
        if (Immediate::is_uimm12(addr.disp())) {
          __ z_st(Z_R0_scratch, addr);
        } else {
          __ z_sty(Z_R0_scratch, addr);
        }
        break;

      case T_ADDRESS:
        __ load_const_optimized(Z_R1_scratch, c->as_jint_bits());
        store_offset = __ reg2mem_opt(Z_R1_scratch, addr, true);
        break;

      case T_OBJECT:  // fall through
      case T_ARRAY:
        if (c->as_jobject() == NULL) {
          if (UseCompressedOops && !wide) {
            __ clear_reg(Z_R1_scratch, false);
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, false);
          } else {
            __ clear_reg(Z_R1_scratch, true);
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, true);
          }
        } else {
          jobject2reg(c->as_jobject(), Z_R1_scratch);
          if (UseCompressedOops && !wide) {
            __ encode_heap_oop(Z_R1_scratch);
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, false);
          } else {
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, true);
          }
        }
        assert(store_offset >= 0, "check");
        break;

      case T_LONG:    // fall through
      case T_DOUBLE:
        __ load_const_optimized(Z_R1_scratch, (int64_t)(c->as_jlong_bits()));
        store_offset = __ reg2mem_opt(Z_R1_scratch, addr, true);
        break;

      case T_BOOLEAN: // fall through
      case T_BYTE:
        __ load_const_optimized(Z_R0_scratch, (int8_t)(c->as_jint()));
        store_offset = __ offset();
        if (Immediate::is_uimm12(addr.disp())) {
          __ z_stc(Z_R0_scratch, addr);
        } else {
          __ z_stcy(Z_R0_scratch, addr);
        }
        break;

      case T_CHAR:    // fall through
      case T_SHORT:
        __ load_const_optimized(Z_R0_scratch, (int16_t)(c->as_jint()));
        store_offset = __ offset();
        if (Immediate::is_uimm12(addr.disp())) {
          __ z_sth(Z_R0_scratch, addr);
        } else {
          __ z_sthy(Z_R0_scratch, addr);
        }
        break;

      default:
        ShouldNotReachHere();
    }

  } else { // no index

    unsigned int lmem = 0;
    unsigned int lcon = 0;
    int64_t cbits = 0;

    switch (type) {
      case T_INT:    // fall through
      case T_FLOAT:
        lmem = 4; lcon = 4; cbits = c->as_jint_bits();
        break;

      case T_ADDRESS:
        lmem = 8; lcon = 4; cbits = c->as_jint_bits();
        break;

      case T_OBJECT:  // fall through
      case T_ARRAY:
        if (c->as_jobject() == NULL) {
          if (UseCompressedOops && !wide) {
            store_offset = __ store_const(addr, (int32_t)NULL_WORD, 4, 4);
          } else {
            store_offset = __ store_const(addr, (int64_t)NULL_WORD, 8, 8);
          }
        } else {
          jobject2reg(c->as_jobject(), Z_R1_scratch);
          if (UseCompressedOops && !wide) {
            __ encode_heap_oop(Z_R1_scratch);
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, false);
          } else {
            store_offset = __ reg2mem_opt(Z_R1_scratch, addr, true);
          }
        }
        assert(store_offset >= 0, "check");
        break;

      case T_LONG:    // fall through
      case T_DOUBLE:
        lmem = 8; lcon = 8; cbits = (int64_t)(c->as_jlong_bits());
        break;

      case T_BOOLEAN: // fall through
      case T_BYTE:
        lmem = 1; lcon = 1; cbits = (int8_t)(c->as_jint());
        break;

      case T_CHAR:    // fall through
      case T_SHORT:
        lmem = 2; lcon = 2; cbits = (int16_t)(c->as_jint());
        break;

      default:
        ShouldNotReachHere();
    }

    if (store_offset == -1) {
      store_offset = __ store_const(addr, cbits, lmem, lcon);
      assert(store_offset >= 0, "check");
    }
  }

  if (info != NULL) {
    add_debug_info_for_null_check(store_offset, info);
  }
}

void LIR_Assembler::const2reg(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();

  switch (c->type()) {
    case T_INT: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register(), c->as_jint());
      break;
    }

    case T_ADDRESS: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register(), c->as_jint());
      break;
    }

    case T_LONG: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register_lo(), (intptr_t)c->as_jlong());
      break;
    }

    case T_OBJECT: {
      if (patch_code != lir_patch_none) {
        jobject2reg_with_patching(dest->as_register(), info);
      } else {
        jobject2reg(c->as_jobject(), dest->as_register());
      }
      break;
    }

    case T_METADATA: {
      if (patch_code != lir_patch_none) {
        klass2reg_with_patching(dest->as_register(), info);
      } else {
        metadata2reg(c->as_metadata(), dest->as_register());
      }
      break;
    }

    case T_FLOAT: {
      Register toc_reg = Z_R1_scratch;
      __ load_toc(toc_reg);
      address const_addr = __ float_constant(c->as_jfloat());
      if (const_addr == NULL) {
        bailout("const section overflow");
        break;
      }
      int displ = const_addr - _masm->code()->consts()->start();
      if (dest->is_single_fpu()) {
        __ z_ley(dest->as_float_reg(), displ, toc_reg);
      } else {
        assert(dest->is_single_cpu(), "Must be a cpu register.");
        __ z_ly(dest->as_register(), displ, toc_reg);
      }
    }
    break;

    case T_DOUBLE: {
      Register toc_reg = Z_R1_scratch;
      __ load_toc(toc_reg);
      address const_addr = __ double_constant(c->as_jdouble());
      if (const_addr == NULL) {
        bailout("const section overflow");
        break;
      }
      int displ = const_addr - _masm->code()->consts()->start();
      if (dest->is_double_fpu()) {
        __ z_ldy(dest->as_double_reg(), displ, toc_reg);
      } else {
        assert(dest->is_double_cpu(), "Must be a long register.");
        __ z_lg(dest->as_register_lo(), displ, toc_reg);
      }
    }
    break;

    default:
      ShouldNotReachHere();
  }
}

Address LIR_Assembler::as_Address(LIR_Address* addr) {
  if (addr->base()->is_illegal()) {
    Unimplemented();
  }

  Register base = addr->base()->as_pointer_register();

  if (addr->index()->is_illegal()) {
    return Address(base, addr->disp());
  } else if (addr->index()->is_cpu_register()) {
    Register index = addr->index()->as_pointer_register();
    return Address(base, index, addr->disp());
  } else if (addr->index()->is_constant()) {
    intptr_t addr_offset = addr->index()->as_constant_ptr()->as_jint() + addr->disp();
    return Address(base, addr_offset);
  } else {
    ShouldNotReachHere();
    return Address();
  }
}

void LIR_Assembler::stack2stack(LIR_Opr src, LIR_Opr dest, BasicType type) {
  switch (type) {
    case T_INT:
    case T_FLOAT: {
      Register tmp = Z_R1_scratch;
      Address from = frame_map()->address_for_slot(src->single_stack_ix());
      Address to   = frame_map()->address_for_slot(dest->single_stack_ix());
      __ mem2reg_opt(tmp, from, false);
      __ reg2mem_opt(tmp, to, false);
      break;
    }
    case T_ADDRESS:
    case T_OBJECT: {
      Register tmp = Z_R1_scratch;
      Address from = frame_map()->address_for_slot(src->single_stack_ix());
      Address to   = frame_map()->address_for_slot(dest->single_stack_ix());
      __ mem2reg_opt(tmp, from, true);
      __ reg2mem_opt(tmp, to, true);
      break;
    }
    case T_LONG:
    case T_DOUBLE: {
      Register tmp = Z_R1_scratch;
      Address from = frame_map()->address_for_double_slot(src->double_stack_ix());
      Address to   = frame_map()->address_for_double_slot(dest->double_stack_ix());
      __ mem2reg_opt(tmp, from, true);
      __ reg2mem_opt(tmp, to, true);
      break;
    }

    default:
      ShouldNotReachHere();
  }
}

// 4-byte accesses only! Don't use it to access 8 bytes!
Address LIR_Assembler::as_Address_hi(LIR_Address* addr) {
  ShouldNotCallThis();
  return 0; // unused
}

// 4-byte accesses only! Don't use it to access 8 bytes!
Address LIR_Assembler::as_Address_lo(LIR_Address* addr) {
  ShouldNotCallThis();
  return 0; // unused
}

void LIR_Assembler::mem2reg(LIR_Opr src_opr, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code,
                            CodeEmitInfo* info, bool wide) {

  assert(type != T_METADATA, "load of metadata ptr not supported");
  LIR_Address* addr = src_opr->as_address_ptr();
  LIR_Opr to_reg = dest;

  Register src = addr->base()->as_pointer_register();
  Register disp_reg = Z_R0;
  int disp_value = addr->disp();
  bool needs_patching = (patch_code != lir_patch_none);

  if (addr->base()->type() == T_OBJECT) {
    __ verify_oop(src, FILE_AND_LINE);
  }

  PatchingStub* patch = NULL;
  if (needs_patching) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
    assert(!to_reg->is_double_cpu() ||
           patch_code == lir_patch_none ||
           patch_code == lir_patch_normal, "patching doesn't match register");
  }

  if (addr->index()->is_illegal()) {
    if (!Immediate::is_simm20(disp_value)) {
      if (needs_patching) {
        __ load_const(Z_R1_scratch, (intptr_t)0);
      } else {
        __ load_const_optimized(Z_R1_scratch, disp_value);
      }
      disp_reg = Z_R1_scratch;
      disp_value = 0;
    }
  } else {
    if (!Immediate::is_simm20(disp_value)) {
      __ load_const_optimized(Z_R1_scratch, disp_value);
      __ z_la(Z_R1_scratch, 0, Z_R1_scratch, addr->index()->as_register());
      disp_reg = Z_R1_scratch;
      disp_value = 0;
    }
    disp_reg = addr->index()->as_pointer_register();
  }

  // Remember the offset of the load. The patching_epilog must be done
  // before the call to add_debug_info, otherwise the PcDescs don't get
  // entered in increasing order.
  int offset = code_offset();

  assert(disp_reg != Z_R0 || Immediate::is_simm20(disp_value), "should have set this up");

  bool short_disp = Immediate::is_uimm12(disp_value);

  switch (type) {
    case T_BOOLEAN: // fall through
    case T_BYTE  :  __ z_lb(dest->as_register(),   disp_value, disp_reg, src); break;
    case T_CHAR  :  __ z_llgh(dest->as_register(), disp_value, disp_reg, src); break;
    case T_SHORT :
      if (short_disp) {
                    __ z_lh(dest->as_register(),   disp_value, disp_reg, src);
      } else {
                    __ z_lhy(dest->as_register(),  disp_value, disp_reg, src);
      }
      break;
    case T_INT   :
      if (short_disp) {
                    __ z_l(dest->as_register(),    disp_value, disp_reg, src);
      } else {
                    __ z_ly(dest->as_register(),   disp_value, disp_reg, src);
      }
      break;
    case T_ADDRESS:
      if (UseCompressedClassPointers && addr->disp() == oopDesc::klass_offset_in_bytes()) {
        __ z_llgf(dest->as_register(), disp_value, disp_reg, src);
        __ decode_klass_not_null(dest->as_register());
      } else {
        __ z_lg(dest->as_register(), disp_value, disp_reg, src);
      }
      break;
    case T_ARRAY : // fall through
    case T_OBJECT:
    {
      if (UseCompressedOops && !wide) {
        __ z_llgf(dest->as_register(), disp_value, disp_reg, src);
        __ oop_decoder(dest->as_register(), dest->as_register(), true);
      } else {
        __ z_lg(dest->as_register(), disp_value, disp_reg, src);
      }
      __ verify_oop(dest->as_register(), FILE_AND_LINE);
      break;
    }
    case T_FLOAT:
      if (short_disp) {
                    __ z_le(dest->as_float_reg(),  disp_value, disp_reg, src);
      } else {
                    __ z_ley(dest->as_float_reg(), disp_value, disp_reg, src);
      }
      break;
    case T_DOUBLE:
      if (short_disp) {
                    __ z_ld(dest->as_double_reg(),  disp_value, disp_reg, src);
      } else {
                    __ z_ldy(dest->as_double_reg(), disp_value, disp_reg, src);
      }
      break;
    case T_LONG  :  __ z_lg(dest->as_register_lo(), disp_value, disp_reg, src); break;
    default      : ShouldNotReachHere();
  }

  if (patch != NULL) {
    patching_epilog(patch, patch_code, src, info);
  }
  if (info != NULL) add_debug_info_for_null_check(offset, info);
}

void LIR_Assembler::stack2reg(LIR_Opr src, LIR_Opr dest, BasicType type) {
  assert(src->is_stack(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");

  if (dest->is_single_cpu()) {
    if (is_reference_type(type)) {
      __ mem2reg_opt(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()), true);
      __ verify_oop(dest->as_register(), FILE_AND_LINE);
    } else if (type == T_METADATA || type == T_ADDRESS) {
      __ mem2reg_opt(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()), true);
    } else {
      __ mem2reg_opt(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()), false);
    }
  } else if (dest->is_double_cpu()) {
    Address src_addr_LO = frame_map()->address_for_slot(src->double_stack_ix());
    __ mem2reg_opt(dest->as_register_lo(), src_addr_LO, true);
  } else if (dest->is_single_fpu()) {
    Address src_addr = frame_map()->address_for_slot(src->single_stack_ix());
    __ mem2freg_opt(dest->as_float_reg(), src_addr, false);
  } else if (dest->is_double_fpu()) {
    Address src_addr = frame_map()->address_for_slot(src->double_stack_ix());
    __ mem2freg_opt(dest->as_double_reg(), src_addr, true);
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::reg2stack(LIR_Opr src, LIR_Opr dest, BasicType type, bool pop_fpu_stack) {
  assert(src->is_register(), "should not call otherwise");
  assert(dest->is_stack(), "should not call otherwise");

  if (src->is_single_cpu()) {
    const Address dst = frame_map()->address_for_slot(dest->single_stack_ix());
    if (is_reference_type(type)) {
      __ verify_oop(src->as_register(), FILE_AND_LINE);
      __ reg2mem_opt(src->as_register(), dst, true);
    } else if (type == T_METADATA || type == T_ADDRESS) {
      __ reg2mem_opt(src->as_register(), dst, true);
    } else {
      __ reg2mem_opt(src->as_register(), dst, false);
    }
  } else if (src->is_double_cpu()) {
    Address dstLO = frame_map()->address_for_slot(dest->double_stack_ix());
    __ reg2mem_opt(src->as_register_lo(), dstLO, true);
  } else if (src->is_single_fpu()) {
    Address dst_addr = frame_map()->address_for_slot(dest->single_stack_ix());
    __ freg2mem_opt(src->as_float_reg(), dst_addr, false);
  } else if (src->is_double_fpu()) {
    Address dst_addr = frame_map()->address_for_slot(dest->double_stack_ix());
    __ freg2mem_opt(src->as_double_reg(), dst_addr, true);
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::reg2reg(LIR_Opr from_reg, LIR_Opr to_reg) {
  if (from_reg->is_float_kind() && to_reg->is_float_kind()) {
    if (from_reg->is_double_fpu()) {
      // double to double moves
      assert(to_reg->is_double_fpu(), "should match");
      __ z_ldr(to_reg->as_double_reg(), from_reg->as_double_reg());
    } else {
      // float to float moves
      assert(to_reg->is_single_fpu(), "should match");
      __ z_ler(to_reg->as_float_reg(), from_reg->as_float_reg());
    }
  } else if (!from_reg->is_float_kind() && !to_reg->is_float_kind()) {
    if (from_reg->is_double_cpu()) {
      __ z_lgr(to_reg->as_pointer_register(), from_reg->as_pointer_register());
    } else if (to_reg->is_double_cpu()) {
      // int to int moves
      __ z_lgr(to_reg->as_register_lo(), from_reg->as_register());
    } else {
      // int to int moves
      __ z_lgr(to_reg->as_register(), from_reg->as_register());
    }
  } else {
    ShouldNotReachHere();
  }
  if (is_reference_type(to_reg->type())) {
    __ verify_oop(to_reg->as_register(), FILE_AND_LINE);
  }
}

void LIR_Assembler::reg2mem(LIR_Opr from, LIR_Opr dest_opr, BasicType type,
                            LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack,
                            bool wide) {
  assert(type != T_METADATA, "store of metadata ptr not supported");
  LIR_Address* addr = dest_opr->as_address_ptr();

  Register dest = addr->base()->as_pointer_register();
  Register disp_reg = Z_R0;
  int disp_value = addr->disp();
  bool needs_patching = (patch_code != lir_patch_none);

  if (addr->base()->is_oop_register()) {
    __ verify_oop(dest, FILE_AND_LINE);
  }

  PatchingStub* patch = NULL;
  if (needs_patching) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
    assert(!from->is_double_cpu() ||
           patch_code == lir_patch_none ||
           patch_code == lir_patch_normal, "patching doesn't match register");
  }

  assert(!needs_patching || (!Immediate::is_simm20(disp_value) && addr->index()->is_illegal()), "assumption");
  if (addr->index()->is_illegal()) {
    if (!Immediate::is_simm20(disp_value)) {
      if (needs_patching) {
        __ load_const(Z_R1_scratch, (intptr_t)0);
      } else {
        __ load_const_optimized(Z_R1_scratch, disp_value);
      }
      disp_reg = Z_R1_scratch;
      disp_value = 0;
    }
  } else {
    if (!Immediate::is_simm20(disp_value)) {
      __ load_const_optimized(Z_R1_scratch, disp_value);
      __ z_la(Z_R1_scratch, 0, Z_R1_scratch, addr->index()->as_register());
      disp_reg = Z_R1_scratch;
      disp_value = 0;
    }
    disp_reg = addr->index()->as_pointer_register();
  }

  assert(disp_reg != Z_R0 || Immediate::is_simm20(disp_value), "should have set this up");

  if (is_reference_type(type)) {
    __ verify_oop(from->as_register(), FILE_AND_LINE);
  }

  bool short_disp = Immediate::is_uimm12(disp_value);

  // Remember the offset of the store. The patching_epilog must be done
  // before the call to add_debug_info_for_null_check, otherwise the PcDescs don't get
  // entered in increasing order.
  int offset = code_offset();
  switch (type) {
    case T_BOOLEAN: // fall through
    case T_BYTE  :
      if (short_disp) {
                    __ z_stc(from->as_register(),  disp_value, disp_reg, dest);
      } else {
                    __ z_stcy(from->as_register(), disp_value, disp_reg, dest);
      }
      break;
    case T_CHAR  : // fall through
    case T_SHORT :
      if (short_disp) {
                    __ z_sth(from->as_register(),  disp_value, disp_reg, dest);
      } else {
                    __ z_sthy(from->as_register(), disp_value, disp_reg, dest);
      }
      break;
    case T_INT   :
      if (short_disp) {
                    __ z_st(from->as_register(),  disp_value, disp_reg, dest);
      } else {
                    __ z_sty(from->as_register(), disp_value, disp_reg, dest);
      }
      break;
    case T_LONG  :  __ z_stg(from->as_register_lo(), disp_value, disp_reg, dest); break;
    case T_ADDRESS: __ z_stg(from->as_register(),    disp_value, disp_reg, dest); break;
      break;
    case T_ARRAY : // fall through
    case T_OBJECT:
      {
        if (UseCompressedOops && !wide) {
          Register compressed_src = Z_R14;
          __ oop_encoder(compressed_src, from->as_register(), true, (disp_reg != Z_R1) ? Z_R1 : Z_R0, -1, true);
          offset = code_offset();
          if (short_disp) {
            __ z_st(compressed_src,  disp_value, disp_reg, dest);
          } else {
            __ z_sty(compressed_src, disp_value, disp_reg, dest);
          }
        } else {
          __ z_stg(from->as_register(), disp_value, disp_reg, dest);
        }
        break;
      }
    case T_FLOAT :
      if (short_disp) {
        __ z_ste(from->as_float_reg(),  disp_value, disp_reg, dest);
      } else {
        __ z_stey(from->as_float_reg(), disp_value, disp_reg, dest);
      }
      break;
    case T_DOUBLE:
      if (short_disp) {
        __ z_std(from->as_double_reg(),  disp_value, disp_reg, dest);
      } else {
        __ z_stdy(from->as_double_reg(), disp_value, disp_reg, dest);
      }
      break;
    default: ShouldNotReachHere();
  }

  if (patch != NULL) {
    patching_epilog(patch, patch_code, dest, info);
  }

  if (info != NULL) add_debug_info_for_null_check(offset, info);
}


void LIR_Assembler::return_op(LIR_Opr result, C1SafepointPollStub* code_stub) {
  assert(result->is_illegal() ||
         (result->is_single_cpu() && result->as_register() == Z_R2) ||
         (result->is_double_cpu() && result->as_register_lo() == Z_R2) ||
         (result->is_single_fpu() && result->as_float_reg() == Z_F0) ||
         (result->is_double_fpu() && result->as_double_reg() == Z_F0), "convention");

  __ z_lg(Z_R1_scratch, Address(Z_thread, JavaThread::polling_page_offset()));

  // Pop the frame before the safepoint code.
  __ pop_frame_restore_retPC(initial_frame_size_in_bytes());

  if (StackReservedPages > 0 && compilation()->has_reserved_stack_access()) {
    __ reserved_stack_check(Z_R14);
  }

  // We need to mark the code position where the load from the safepoint
  // polling page was emitted as relocInfo::poll_return_type here.
  __ relocate(relocInfo::poll_return_type);
  __ load_from_polling_page(Z_R1_scratch);

  __ z_br(Z_R14); // Return to caller.
}

int LIR_Assembler::safepoint_poll(LIR_Opr tmp, CodeEmitInfo* info) {
  const Register poll_addr = tmp->as_register_lo();
  __ z_lg(poll_addr, Address(Z_thread, JavaThread::polling_page_offset()));
  guarantee(info != NULL, "Shouldn't be NULL");
  add_debug_info_for_branch(info);
  int offset = __ offset();
  __ relocate(relocInfo::poll_type);
  __ load_from_polling_page(poll_addr);
  return offset;
}

void LIR_Assembler::emit_static_call_stub() {

  // Stub is fixed up when the corresponding call is converted from calling
  // compiled code to calling interpreted code.

  address call_pc = __ pc();
  address stub = __ start_a_stub(call_stub_size());
  if (stub == NULL) {
    bailout("static call stub overflow");
    return;
  }

  int start = __ offset();

  __ relocate(static_stub_Relocation::spec(call_pc));

  // See also Matcher::interpreter_method_reg().
  AddressLiteral meta = __ allocate_metadata_address(NULL);
  bool success = __ load_const_from_toc(Z_method, meta);

  __ set_inst_mark();
  AddressLiteral a((address)-1);
  success = success && __ load_const_from_toc(Z_R1, a);
  if (!success) {
    bailout("const section overflow");
    return;
  }

  __ z_br(Z_R1);
  assert(__ offset() - start <= call_stub_size(), "stub too big");
  __ end_a_stub(); // Update current stubs pointer and restore insts_end.
}

void LIR_Assembler::comp_op(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Op2* op) {
  bool unsigned_comp = condition == lir_cond_belowEqual || condition == lir_cond_aboveEqual;
  if (opr1->is_single_cpu()) {
    Register reg1 = opr1->as_register();
    if (opr2->is_single_cpu()) {
      // cpu register - cpu register
      if (is_reference_type(opr1->type())) {
        __ z_clgr(reg1, opr2->as_register());
      } else {
        assert(!is_reference_type(opr2->type()), "cmp int, oop?");
        if (unsigned_comp) {
          __ z_clr(reg1, opr2->as_register());
        } else {
          __ z_cr(reg1, opr2->as_register());
        }
      }
    } else if (opr2->is_stack()) {
      // cpu register - stack
      if (is_reference_type(opr1->type())) {
        __ z_cg(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
      } else {
        if (unsigned_comp) {
          __ z_cly(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
        } else {
          __ z_cy(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
        }
      }
    } else if (opr2->is_constant()) {
      // cpu register - constant
      LIR_Const* c = opr2->as_constant_ptr();
      if (c->type() == T_INT) {
        if (unsigned_comp) {
          __ z_clfi(reg1, c->as_jint());
        } else {
          __ z_cfi(reg1, c->as_jint());
        }
      } else if (c->type() == T_METADATA) {
        // We only need, for now, comparison with NULL for metadata.
        assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "oops");
        Metadata* m = c->as_metadata();
        if (m == NULL) {
          __ z_cghi(reg1, 0);
        } else {
          ShouldNotReachHere();
        }
      } else if (is_reference_type(c->type())) {
        // In 64bit oops are single register.
        jobject o = c->as_jobject();
        if (o == NULL) {
          __ z_ltgr(reg1, reg1);
        } else {
          jobject2reg(o, Z_R1_scratch);
          __ z_cgr(reg1, Z_R1_scratch);
        }
      } else {
        fatal("unexpected type: %s", basictype_to_str(c->type()));
      }
      // cpu register - address
    } else if (opr2->is_address()) {
      if (op->info() != NULL) {
        add_debug_info_for_null_check_here(op->info());
      }
      if (unsigned_comp) {
        __ z_cly(reg1, as_Address(opr2->as_address_ptr()));
      } else {
        __ z_cy(reg1, as_Address(opr2->as_address_ptr()));
      }
    } else {
      ShouldNotReachHere();
    }

  } else if (opr1->is_double_cpu()) {
    assert(!unsigned_comp, "unexpected");
    Register xlo = opr1->as_register_lo();
    Register xhi = opr1->as_register_hi();
    if (opr2->is_double_cpu()) {
      __ z_cgr(xlo, opr2->as_register_lo());
    } else if (opr2->is_constant()) {
      // cpu register - constant 0
      assert(opr2->as_jlong() == (jlong)0, "only handles zero");
      __ z_ltgr(xlo, xlo);
    } else {
      ShouldNotReachHere();
    }

  } else if (opr1->is_single_fpu()) {
    if (opr2->is_single_fpu()) {
      __ z_cebr(opr1->as_float_reg(), opr2->as_float_reg());
    } else {
      // stack slot
      Address addr = frame_map()->address_for_slot(opr2->single_stack_ix());
      if (Immediate::is_uimm12(addr.disp())) {
        __ z_ceb(opr1->as_float_reg(), addr);
      } else {
        __ z_ley(Z_fscratch_1, addr);
        __ z_cebr(opr1->as_float_reg(), Z_fscratch_1);
      }
    }
  } else if (opr1->is_double_fpu()) {
    if (opr2->is_double_fpu()) {
    __ z_cdbr(opr1->as_double_reg(), opr2->as_double_reg());
    } else {
      // stack slot
      Address addr = frame_map()->address_for_slot(opr2->double_stack_ix());
      if (Immediate::is_uimm12(addr.disp())) {
        __ z_cdb(opr1->as_double_reg(), addr);
      } else {
        __ z_ldy(Z_fscratch_1, addr);
        __ z_cdbr(opr1->as_double_reg(), Z_fscratch_1);
      }
    }
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::comp_fl2i(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst, LIR_Op2* op) {
  Label    done;
  Register dreg = dst->as_register();

  if (code == lir_cmp_fd2i || code == lir_ucmp_fd2i) {
    assert((left->is_single_fpu() && right->is_single_fpu()) ||
           (left->is_double_fpu() && right->is_double_fpu()), "unexpected operand types");
    bool is_single = left->is_single_fpu();
    bool is_unordered_less = (code == lir_ucmp_fd2i);
    FloatRegister lreg = is_single ? left->as_float_reg() : left->as_double_reg();
    FloatRegister rreg = is_single ? right->as_float_reg() : right->as_double_reg();
    if (is_single) {
      __ z_cebr(lreg, rreg);
    } else {
      __ z_cdbr(lreg, rreg);
    }
    if (VM_Version::has_LoadStoreConditional()) {
      Register one       = Z_R0_scratch;
      Register minus_one = Z_R1_scratch;
      __ z_lghi(minus_one, -1);
      __ z_lghi(one,  1);
      __ z_lghi(dreg, 0);
      __ z_locgr(dreg, one,       is_unordered_less ? Assembler::bcondHigh            : Assembler::bcondHighOrNotOrdered);
      __ z_locgr(dreg, minus_one, is_unordered_less ? Assembler::bcondLowOrNotOrdered : Assembler::bcondLow);
    } else {
      __ clear_reg(dreg, true, false);
      __ z_bre(done); // if (left == right) dst = 0

      // if (left > right || ((code ~= cmpg) && (left <> right)) dst := 1
      __ z_lhi(dreg, 1);
      __ z_brc(is_unordered_less ? Assembler::bcondHigh : Assembler::bcondHighOrNotOrdered, done);

      // if (left < right || ((code ~= cmpl) && (left <> right)) dst := -1
      __ z_lhi(dreg, -1);
    }
  } else {
    assert(code == lir_cmp_l2i, "check");
    if (VM_Version::has_LoadStoreConditional()) {
      Register one       = Z_R0_scratch;
      Register minus_one = Z_R1_scratch;
      __ z_cgr(left->as_register_lo(), right->as_register_lo());
      __ z_lghi(minus_one, -1);
      __ z_lghi(one,  1);
      __ z_lghi(dreg, 0);
      __ z_locgr(dreg, one, Assembler::bcondHigh);
      __ z_locgr(dreg, minus_one, Assembler::bcondLow);
    } else {
      __ z_cgr(left->as_register_lo(), right->as_register_lo());
      __ z_lghi(dreg,  0);     // eq value
      __ z_bre(done);
      __ z_lghi(dreg,  1);     // gt value
      __ z_brh(done);
      __ z_lghi(dreg, -1);     // lt value
    }
  }
  __ bind(done);
}

// result = condition ? opr1 : opr2
void LIR_Assembler::cmove(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, BasicType type) {
  Assembler::branch_condition acond = Assembler::bcondEqual, ncond = Assembler::bcondNotEqual;
  switch (condition) {
    case lir_cond_equal:        acond = Assembler::bcondEqual;    ncond = Assembler::bcondNotEqual; break;
    case lir_cond_notEqual:     acond = Assembler::bcondNotEqual; ncond = Assembler::bcondEqual;    break;
    case lir_cond_less:         acond = Assembler::bcondLow;      ncond = Assembler::bcondNotLow;   break;
    case lir_cond_lessEqual:    acond = Assembler::bcondNotHigh;  ncond = Assembler::bcondHigh;     break;
    case lir_cond_greaterEqual: acond = Assembler::bcondNotLow;   ncond = Assembler::bcondLow;      break;
    case lir_cond_greater:      acond = Assembler::bcondHigh;     ncond = Assembler::bcondNotHigh;  break;
    case lir_cond_belowEqual:   acond = Assembler::bcondNotHigh;  ncond = Assembler::bcondHigh;     break;
    case lir_cond_aboveEqual:   acond = Assembler::bcondNotLow;   ncond = Assembler::bcondLow;      break;
    default:                    ShouldNotReachHere();
  }

  if (opr1->is_cpu_register()) {
    reg2reg(opr1, result);
  } else if (opr1->is_stack()) {
    stack2reg(opr1, result, result->type());
  } else if (opr1->is_constant()) {
    const2reg(opr1, result, lir_patch_none, NULL);
  } else {
    ShouldNotReachHere();
  }

  if (VM_Version::has_LoadStoreConditional() && !opr2->is_constant()) {
    // Optimized version that does not require a branch.
    if (opr2->is_single_cpu()) {
      assert(opr2->cpu_regnr() != result->cpu_regnr(), "opr2 already overwritten by previous move");
      __ z_locgr(result->as_register(), opr2->as_register(), ncond);
    } else if (opr2->is_double_cpu()) {
      assert(opr2->cpu_regnrLo() != result->cpu_regnrLo() && opr2->cpu_regnrLo() != result->cpu_regnrHi(), "opr2 already overwritten by previous move");
      assert(opr2->cpu_regnrHi() != result->cpu_regnrLo() && opr2->cpu_regnrHi() != result->cpu_regnrHi(), "opr2 already overwritten by previous move");
      __ z_locgr(result->as_register_lo(), opr2->as_register_lo(), ncond);
    } else if (opr2->is_single_stack()) {
      __ z_loc(result->as_register(), frame_map()->address_for_slot(opr2->single_stack_ix()), ncond);
    } else if (opr2->is_double_stack()) {
      __ z_locg(result->as_register_lo(), frame_map()->address_for_slot(opr2->double_stack_ix()), ncond);
    } else {
      ShouldNotReachHere();
    }
  } else {
    Label skip;
    __ z_brc(acond, skip);
    if (opr2->is_cpu_register()) {
      reg2reg(opr2, result);
    } else if (opr2->is_stack()) {
      stack2reg(opr2, result, result->type());
    } else if (opr2->is_constant()) {
      const2reg(opr2, result, lir_patch_none, NULL);
    } else {
      ShouldNotReachHere();
    }
    __ bind(skip);
  }
}

void LIR_Assembler::arith_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest,
                             CodeEmitInfo* info, bool pop_fpu_stack) {
  assert(info == NULL, "should never be used, idiv/irem and ldiv/lrem not handled by this method");

  if (left->is_single_cpu()) {
    assert(left == dest, "left and dest must be equal");
    Register lreg = left->as_register();

    if (right->is_single_cpu()) {
      // cpu register - cpu register
      Register rreg = right->as_register();
      switch (code) {
        case lir_add: __ z_ar (lreg, rreg); break;
        case lir_sub: __ z_sr (lreg, rreg); break;
        case lir_mul: __ z_msr(lreg, rreg); break;
        default: ShouldNotReachHere();
      }

    } else if (right->is_stack()) {
      // cpu register - stack
      Address raddr = frame_map()->address_for_slot(right->single_stack_ix());
      switch (code) {
        case lir_add: __ z_ay(lreg, raddr); break;
        case lir_sub: __ z_sy(lreg, raddr); break;
        default: ShouldNotReachHere();
      }

    } else if (right->is_constant()) {
      // cpu register - constant
      jint c = right->as_constant_ptr()->as_jint();
      switch (code) {
        case lir_add: __ z_agfi(lreg, c);  break;
        case lir_sub: __ z_agfi(lreg, -c); break; // note: -min_jint == min_jint
        case lir_mul: __ z_msfi(lreg, c);  break;
        default: ShouldNotReachHere();
      }

    } else {
      ShouldNotReachHere();
    }

  } else if (left->is_double_cpu()) {
    assert(left == dest, "left and dest must be equal");
    Register lreg_lo = left->as_register_lo();
    Register lreg_hi = left->as_register_hi();

    if (right->is_double_cpu()) {
      // cpu register - cpu register
      Register rreg_lo = right->as_register_lo();
      Register rreg_hi = right->as_register_hi();
      assert_different_registers(lreg_lo, rreg_lo);
      switch (code) {
        case lir_add:
          __ z_agr(lreg_lo, rreg_lo);
          break;
        case lir_sub:
          __ z_sgr(lreg_lo, rreg_lo);
          break;
        case lir_mul:
          __ z_msgr(lreg_lo, rreg_lo);
          break;
        default:
          ShouldNotReachHere();
      }

    } else if (right->is_constant()) {
      // cpu register - constant
      jlong c = right->as_constant_ptr()->as_jlong_bits();
      switch (code) {
        case lir_add: __ z_agfi(lreg_lo, c); break;
        case lir_sub:
          if (c != min_jint) {
                      __ z_agfi(lreg_lo, -c);
          } else {
            // -min_jint cannot be represented as simm32 in z_agfi
            // min_jint sign extended:      0xffffffff80000000
            // -min_jint as 64 bit integer: 0x0000000080000000
            // 0x80000000 can be represented as uimm32 in z_algfi
            // lreg_lo := lreg_lo + -min_jint == lreg_lo + 0x80000000
                      __ z_algfi(lreg_lo, UCONST64(0x80000000));
          }
          break;
        case lir_mul: __ z_msgfi(lreg_lo, c); break;
        default:
          ShouldNotReachHere();
      }

    } else {
      ShouldNotReachHere();
    }

  } else if (left->is_single_fpu()) {
    assert(left == dest, "left and dest must be equal");
    FloatRegister lreg = left->as_float_reg();
    FloatRegister rreg = right->is_single_fpu() ? right->as_float_reg() : fnoreg;
    Address raddr;

    if (rreg == fnoreg) {
      assert(right->is_single_stack(), "constants should be loaded into register");
      raddr = frame_map()->address_for_slot(right->single_stack_ix());
      if (!Immediate::is_uimm12(raddr.disp())) {
        __ mem2freg_opt(rreg = Z_fscratch_1, raddr, false);
      }
    }

    if (rreg != fnoreg) {
      switch (code) {
        case lir_add: __ z_aebr(lreg, rreg);  break;
        case lir_sub: __ z_sebr(lreg, rreg);  break;
        case lir_mul: __ z_meebr(lreg, rreg); break;
        case lir_div: __ z_debr(lreg, rreg);  break;
        default: ShouldNotReachHere();
      }
    } else {
      switch (code) {
        case lir_add: __ z_aeb(lreg, raddr);  break;
        case lir_sub: __ z_seb(lreg, raddr);  break;
        case lir_mul: __ z_meeb(lreg, raddr);  break;
        case lir_div: __ z_deb(lreg, raddr);  break;
        default: ShouldNotReachHere();
      }
    }
  } else if (left->is_double_fpu()) {
    assert(left == dest, "left and dest must be equal");
    FloatRegister lreg = left->as_double_reg();
    FloatRegister rreg = right->is_double_fpu() ? right->as_double_reg() : fnoreg;
    Address raddr;

    if (rreg == fnoreg) {
      assert(right->is_double_stack(), "constants should be loaded into register");
      raddr = frame_map()->address_for_slot(right->double_stack_ix());
      if (!Immediate::is_uimm12(raddr.disp())) {
        __ mem2freg_opt(rreg = Z_fscratch_1, raddr, true);
      }
    }

    if (rreg != fnoreg) {
      switch (code) {
        case lir_add: __ z_adbr(lreg, rreg); break;
        case lir_sub: __ z_sdbr(lreg, rreg); break;
        case lir_mul: __ z_mdbr(lreg, rreg); break;
        case lir_div: __ z_ddbr(lreg, rreg); break;
        default: ShouldNotReachHere();
      }
    } else {
      switch (code) {
        case lir_add: __ z_adb(lreg, raddr); break;
        case lir_sub: __ z_sdb(lreg, raddr); break;
        case lir_mul: __ z_mdb(lreg, raddr); break;
        case lir_div: __ z_ddb(lreg, raddr); break;
        default: ShouldNotReachHere();
      }
    }
  } else if (left->is_address()) {
    assert(left == dest, "left and dest must be equal");
    assert(code == lir_add, "unsupported operation");
    assert(right->is_constant(), "unsupported operand");
    jint c = right->as_constant_ptr()->as_jint();
    LIR_Address* lir_addr = left->as_address_ptr();
    Address addr = as_Address(lir_addr);
    switch (lir_addr->type()) {
      case T_INT:
        __ add2mem_32(addr, c, Z_R1_scratch);
        break;
      case T_LONG:
        __ add2mem_64(addr, c, Z_R1_scratch);
        break;
      default:
        ShouldNotReachHere();
    }
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::intrinsic_op(LIR_Code code, LIR_Opr value, LIR_Opr thread, LIR_Opr dest, LIR_Op* op) {
  switch (code) {
    case lir_sqrt: {
      assert(!thread->is_valid(), "there is no need for a thread_reg for dsqrt");
      FloatRegister src_reg = value->as_double_reg();
      FloatRegister dst_reg = dest->as_double_reg();
      __ z_sqdbr(dst_reg, src_reg);
      break;
    }
    case lir_abs: {
      assert(!thread->is_valid(), "there is no need for a thread_reg for fabs");
      FloatRegister src_reg = value->as_double_reg();
      FloatRegister dst_reg = dest->as_double_reg();
      __ z_lpdbr(dst_reg, src_reg);
      break;
    }
    default: {
      ShouldNotReachHere();
      break;
    }
  }
}

void LIR_Assembler::logic_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst) {
  if (left->is_single_cpu()) {
    Register reg = left->as_register();
    if (right->is_constant()) {
      int val = right->as_constant_ptr()->as_jint();
      switch (code) {
        case lir_logic_and: __ z_nilf(reg, val); break;
        case lir_logic_or:  __ z_oilf(reg, val); break;
        case lir_logic_xor: __ z_xilf(reg, val); break;
        default: ShouldNotReachHere();
      }
    } else if (right->is_stack()) {
      Address raddr = frame_map()->address_for_slot(right->single_stack_ix());
      switch (code) {
        case lir_logic_and: __ z_ny(reg, raddr); break;
        case lir_logic_or:  __ z_oy(reg, raddr); break;
        case lir_logic_xor: __ z_xy(reg, raddr); break;
        default: ShouldNotReachHere();
      }
    } else {
      Register rright = right->as_register();
      switch (code) {
        case lir_logic_and: __ z_nr(reg, rright); break;
        case lir_logic_or : __ z_or(reg, rright); break;
        case lir_logic_xor: __ z_xr(reg, rright); break;
        default: ShouldNotReachHere();
      }
    }
    move_regs(reg, dst->as_register());
  } else {
    Register l_lo = left->as_register_lo();
    if (right->is_constant()) {
      __ load_const_optimized(Z_R1_scratch, right->as_constant_ptr()->as_jlong());
      switch (code) {
        case lir_logic_and:
          __ z_ngr(l_lo, Z_R1_scratch);
          break;
        case lir_logic_or:
          __ z_ogr(l_lo, Z_R1_scratch);
          break;
        case lir_logic_xor:
          __ z_xgr(l_lo, Z_R1_scratch);
          break;
        default: ShouldNotReachHere();
      }
    } else {
      Register r_lo;
      if (is_reference_type(right->type())) {
        r_lo = right->as_register();
      } else {
        r_lo = right->as_register_lo();
      }
      switch (code) {
        case lir_logic_and:
          __ z_ngr(l_lo, r_lo);
          break;
        case lir_logic_or:
          __ z_ogr(l_lo, r_lo);
          break;
        case lir_logic_xor:
          __ z_xgr(l_lo, r_lo);
          break;
        default: ShouldNotReachHere();
      }
    }

    Register dst_lo = dst->as_register_lo();

    move_regs(l_lo, dst_lo);
  }
}

// See operand selection in LIRGenerator::do_ArithmeticOp_Int().
void LIR_Assembler::arithmetic_idiv(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr temp, LIR_Opr result, CodeEmitInfo* info) {
  if (left->is_double_cpu()) {
    // 64 bit integer case
    assert(left->is_double_cpu(), "left must be register");
    assert(right->is_double_cpu() || is_power_of_2(right->as_jlong()),
           "right must be register or power of 2 constant");
    assert(result->is_double_cpu(), "result must be register");

    Register lreg = left->as_register_lo();
    Register dreg = result->as_register_lo();

    if (right->is_constant()) {
      // Convert division by a power of two into some shifts and logical operations.
      Register treg1 = Z_R0_scratch;
      Register treg2 = Z_R1_scratch;
      jlong divisor = right->as_jlong();
      jlong log_divisor = log2i_exact(right->as_jlong());

      if (divisor == min_jlong) {
        // Min_jlong is special. Result is '0' except for min_jlong/min_jlong = 1.
        if (dreg == lreg) {
          NearLabel done;
          __ load_const_optimized(treg2, min_jlong);
          __ z_cgr(lreg, treg2);
          __ z_lghi(dreg, 0);           // Preserves condition code.
          __ z_brne(done);
          __ z_lghi(dreg, 1);           // min_jlong / min_jlong = 1
          __ bind(done);
        } else {
          assert_different_registers(dreg, lreg);
          NearLabel done;
          __ z_lghi(dreg, 0);
          __ compare64_and_branch(lreg, min_jlong, Assembler::bcondNotEqual, done);
          __ z_lghi(dreg, 1);
          __ bind(done);
        }
        return;
      }
      __ move_reg_if_needed(dreg, T_LONG, lreg, T_LONG);
      if (divisor == 2) {
        __ z_srlg(treg2, dreg, 63);     // dividend < 0 ? 1 : 0
      } else {
        __ z_srag(treg2, dreg, 63);     // dividend < 0 ? -1 : 0
        __ and_imm(treg2, divisor - 1, treg1, true);
      }
      if (code == lir_idiv) {
        __ z_agr(dreg, treg2);
        __ z_srag(dreg, dreg, log_divisor);
      } else {
        assert(code == lir_irem, "check");
        __ z_agr(treg2, dreg);
        __ and_imm(treg2, ~(divisor - 1), treg1, true);
        __ z_sgr(dreg, treg2);
      }
      return;
    }

    // Divisor is not a power of 2 constant.
    Register rreg = right->as_register_lo();
    Register treg = temp->as_register_lo();
    assert(right->is_double_cpu(), "right must be register");
    assert(lreg == Z_R11, "see ldivInOpr()");
    assert(rreg != lreg, "right register must not be same as left register");
    assert((code == lir_idiv && dreg == Z_R11 && treg == Z_R10) ||
           (code == lir_irem && dreg == Z_R10 && treg == Z_R11), "see ldivInOpr(), ldivOutOpr(), lremOutOpr()");

    Register R1 = lreg->predecessor();
    Register R2 = rreg;
    assert(code != lir_idiv || lreg==dreg, "see code below");
    if (code == lir_idiv) {
      __ z_lcgr(lreg, lreg);
    } else {
      __ clear_reg(dreg, true, false);
    }
    NearLabel done;
    __ compare64_and_branch(R2, -1, Assembler::bcondEqual, done);
    if (code == lir_idiv) {
      __ z_lcgr(lreg, lreg); // Revert lcgr above.
    }
    if (ImplicitDiv0Checks) {
      // No debug info because the idiv won't trap.
      // Add_debug_info_for_div0 would instantiate another DivByZeroStub,
      // which is unnecessary, too.
      add_debug_info_for_div0(__ offset(), info);
    }
    __ z_dsgr(R1, R2);
    __ bind(done);
    return;
  }

  // 32 bit integer case

  assert(left->is_single_cpu(), "left must be register");
  assert(right->is_single_cpu() || is_power_of_2(right->as_jint()), "right must be register or power of 2 constant");
  assert(result->is_single_cpu(), "result must be register");

  Register lreg = left->as_register();
  Register dreg = result->as_register();

  if (right->is_constant()) {
    // Convert division by a power of two into some shifts and logical operations.
    Register treg1 = Z_R0_scratch;
    Register treg2 = Z_R1_scratch;
    jlong divisor = right->as_jint();
    jlong log_divisor = log2i_exact(right->as_jint());
    __ move_reg_if_needed(dreg, T_LONG, lreg, T_INT); // sign extend
    if (divisor == 2) {
      __ z_srlg(treg2, dreg, 63);     // dividend < 0 ?  1 : 0
    } else {
      __ z_srag(treg2, dreg, 63);     // dividend < 0 ? -1 : 0
      __ and_imm(treg2, divisor - 1, treg1, true);
    }
    if (code == lir_idiv) {
      __ z_agr(dreg, treg2);
      __ z_srag(dreg, dreg, log_divisor);
    } else {
      assert(code == lir_irem, "check");
      __ z_agr(treg2, dreg);
      __ and_imm(treg2, ~(divisor - 1), treg1, true);
      __ z_sgr(dreg, treg2);
    }
    return;
  }

  // Divisor is not a power of 2 constant.
  Register rreg = right->as_register();
  Register treg = temp->as_register();
  assert(right->is_single_cpu(), "right must be register");
  assert(lreg == Z_R11, "left register must be rax,");
  assert(rreg != lreg, "right register must not be same as left register");
  assert((code == lir_idiv && dreg == Z_R11 && treg == Z_R10)
      || (code == lir_irem && dreg == Z_R10 && treg == Z_R11), "see divInOpr(), divOutOpr(), remOutOpr()");

  Register R1 = lreg->predecessor();
  Register R2 = rreg;
  __ move_reg_if_needed(lreg, T_LONG, lreg, T_INT); // sign extend
  if (ImplicitDiv0Checks) {
    // No debug info because the idiv won't trap.
    // Add_debug_info_for_div0 would instantiate another DivByZeroStub,
    // which is unnecessary, too.
    add_debug_info_for_div0(__ offset(), info);
  }
  __ z_dsgfr(R1, R2);
}

void LIR_Assembler::throw_op(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info) {
  assert(exceptionOop->as_register() == Z_EXC_OOP, "should match");
  assert(exceptionPC->as_register() == Z_EXC_PC, "should match");

  // Exception object is not added to oop map by LinearScan
  // (LinearScan assumes that no oops are in fixed registers).
  info->add_register_oop(exceptionOop);

  // Reuse the debug info from the safepoint poll for the throw op itself.
  __ get_PC(Z_EXC_PC);
  add_call_info(__ offset(), info); // for exception handler
  address stub = Runtime1::entry_for (compilation()->has_fpu_code() ? Runtime1::handle_exception_id
                                                                    : Runtime1::handle_exception_nofpu_id);
  emit_call_c(stub);
}

void LIR_Assembler::unwind_op(LIR_Opr exceptionOop) {
  assert(exceptionOop->as_register() == Z_EXC_OOP, "should match");

  __ branch_optimized(Assembler::bcondAlways, _unwind_handler_entry);
}

void LIR_Assembler::emit_arraycopy(LIR_OpArrayCopy* op) {
  ciArrayKlass* default_type = op->expected_type();
  Register src = op->src()->as_register();
  Register dst = op->dst()->as_register();
  Register src_pos = op->src_pos()->as_register();
  Register dst_pos = op->dst_pos()->as_register();
  Register length  = op->length()->as_register();
  Register tmp = op->tmp()->as_register();

  CodeStub* stub = op->stub();
  int flags = op->flags();
  BasicType basic_type = default_type != NULL ? default_type->element_type()->basic_type() : T_ILLEGAL;
  if (basic_type == T_ARRAY) basic_type = T_OBJECT;

  // If we don't know anything, just go through the generic arraycopy.
  if (default_type == NULL) {
    address copyfunc_addr = StubRoutines::generic_arraycopy();

    if (copyfunc_addr == NULL) {
      // Take a slow path for generic arraycopy.
      __ branch_optimized(Assembler::bcondAlways, *stub->entry());
      __ bind(*stub->continuation());
      return;
    }

    // Save outgoing arguments in callee saved registers (C convention) in case
    // a call to System.arraycopy is needed.
    Register callee_saved_src     = Z_R10;
    Register callee_saved_src_pos = Z_R11;
    Register callee_saved_dst     = Z_R12;
    Register callee_saved_dst_pos = Z_R13;
    Register callee_saved_length  = Z_ARG5; // Z_ARG5 == Z_R6 is callee saved.

    __ lgr_if_needed(callee_saved_src, src);
    __ lgr_if_needed(callee_saved_src_pos, src_pos);
    __ lgr_if_needed(callee_saved_dst, dst);
    __ lgr_if_needed(callee_saved_dst_pos, dst_pos);
    __ lgr_if_needed(callee_saved_length, length);

    // C function requires 64 bit values.
    __ z_lgfr(src_pos, src_pos);
    __ z_lgfr(dst_pos, dst_pos);
    __ z_lgfr(length, length);

    // Pass arguments: may push as this is not a safepoint; SP must be fix at each safepoint.

    // The arguments are in the corresponding registers.
    assert(Z_ARG1 == src,     "assumption");
    assert(Z_ARG2 == src_pos, "assumption");
    assert(Z_ARG3 == dst,     "assumption");
    assert(Z_ARG4 == dst_pos, "assumption");
    assert(Z_ARG5 == length,  "assumption");
#ifndef PRODUCT
    if (PrintC1Statistics) {
      __ load_const_optimized(Z_R1_scratch, (address)&Runtime1::_generic_arraycopystub_cnt);
      __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
    }
#endif
    emit_call_c(copyfunc_addr);
    CHECK_BAILOUT();

    __ compare32_and_branch(Z_RET, (intptr_t)0, Assembler::bcondEqual, *stub->continuation());

    __ z_lgr(tmp, Z_RET);
    __ z_xilf(tmp, -1);

    // Restore values from callee saved registers so they are where the stub
    // expects them.
    __ lgr_if_needed(src, callee_saved_src);
    __ lgr_if_needed(src_pos, callee_saved_src_pos);
    __ lgr_if_needed(dst, callee_saved_dst);
    __ lgr_if_needed(dst_pos, callee_saved_dst_pos);
    __ lgr_if_needed(length, callee_saved_length);

    __ z_sr(length, tmp);
    __ z_ar(src_pos, tmp);
    __ z_ar(dst_pos, tmp);
    __ branch_optimized(Assembler::bcondAlways, *stub->entry());

    __ bind(*stub->continuation());
    return;
  }

  assert(default_type != NULL && default_type->is_array_klass() && default_type->is_loaded(), "must be true at this point");

  int elem_size = type2aelembytes(basic_type);
  int shift_amount;

  switch (elem_size) {
    case 1 :
      shift_amount = 0;
      break;
    case 2 :
      shift_amount = 1;
      break;
    case 4 :
      shift_amount = 2;
      break;
    case 8 :
      shift_amount = 3;
      break;
    default:
      shift_amount = -1;
      ShouldNotReachHere();
  }

  Address src_length_addr = Address(src, arrayOopDesc::length_offset_in_bytes());
  Address dst_length_addr = Address(dst, arrayOopDesc::length_offset_in_bytes());
  Address src_klass_addr = Address(src, oopDesc::klass_offset_in_bytes());
  Address dst_klass_addr = Address(dst, oopDesc::klass_offset_in_bytes());

  // Length and pos's are all sign extended at this point on 64bit.

  // test for NULL
  if (flags & LIR_OpArrayCopy::src_null_check) {
    __ compareU64_and_branch(src, (intptr_t)0, Assembler::bcondZero, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_null_check) {
    __ compareU64_and_branch(dst, (intptr_t)0, Assembler::bcondZero, *stub->entry());
  }

  // Check if negative.
  if (flags & LIR_OpArrayCopy::src_pos_positive_check) {
    __ compare32_and_branch(src_pos, (intptr_t)0, Assembler::bcondLow, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_pos_positive_check) {
    __ compare32_and_branch(dst_pos, (intptr_t)0, Assembler::bcondLow, *stub->entry());
  }

  // If the compiler was not able to prove that exact type of the source or the destination
  // of the arraycopy is an array type, check at runtime if the source or the destination is
  // an instance type.
  if (flags & LIR_OpArrayCopy::type_check) {
    assert(Klass::_lh_neutral_value == 0, "or replace z_lt instructions");

    if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
      __ load_klass(tmp, dst);
      __ z_lt(tmp, Address(tmp, in_bytes(Klass::layout_helper_offset())));
      __ branch_optimized(Assembler::bcondNotLow, *stub->entry());
    }

    if (!(flags & LIR_OpArrayCopy::src_objarray)) {
      __ load_klass(tmp, src);
      __ z_lt(tmp, Address(tmp, in_bytes(Klass::layout_helper_offset())));
      __ branch_optimized(Assembler::bcondNotLow, *stub->entry());
    }
  }

  if (flags & LIR_OpArrayCopy::src_range_check) {
    __ z_la(tmp, Address(src_pos, length));
    __ z_cl(tmp, src_length_addr);
    __ branch_optimized(Assembler::bcondHigh, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_range_check) {
    __ z_la(tmp, Address(dst_pos, length));
    __ z_cl(tmp, dst_length_addr);
    __ branch_optimized(Assembler::bcondHigh, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::length_positive_check) {
    __ z_ltr(length, length);
    __ branch_optimized(Assembler::bcondNegative, *stub->entry());
  }

  // Stubs require 64 bit values.
  __ z_lgfr(src_pos, src_pos); // int -> long
  __ z_lgfr(dst_pos, dst_pos); // int -> long
  __ z_lgfr(length, length);   // int -> long

  if (flags & LIR_OpArrayCopy::type_check) {
    // We don't know the array types are compatible.
    if (basic_type != T_OBJECT) {
      // Simple test for basic type arrays.
      if (UseCompressedClassPointers) {
        __ z_l(tmp, src_klass_addr);
        __ z_c(tmp, dst_klass_addr);
      } else {
        __ z_lg(tmp, src_klass_addr);
        __ z_cg(tmp, dst_klass_addr);
      }
      __ branch_optimized(Assembler::bcondNotEqual, *stub->entry());
    } else {
      // For object arrays, if src is a sub class of dst then we can
      // safely do the copy.
      NearLabel cont, slow;
      Register src_klass = Z_R1_scratch;
      Register dst_klass = Z_R10;

      __ load_klass(src_klass, src);
      __ load_klass(dst_klass, dst);

      __ check_klass_subtype_fast_path(src_klass, dst_klass, tmp, &cont, &slow, NULL);

      store_parameter(src_klass, 0); // sub
      store_parameter(dst_klass, 1); // super
      emit_call_c(Runtime1::entry_for (Runtime1::slow_subtype_check_id));
      CHECK_BAILOUT2(cont, slow);
      // Sets condition code 0 for match (2 otherwise).
      __ branch_optimized(Assembler::bcondEqual, cont);

      __ bind(slow);

      address copyfunc_addr = StubRoutines::checkcast_arraycopy();
      if (copyfunc_addr != NULL) { // use stub if available
        // Src is not a sub class of dst so we have to do a
        // per-element check.

        int mask = LIR_OpArrayCopy::src_objarray|LIR_OpArrayCopy::dst_objarray;
        if ((flags & mask) != mask) {
          // Check that at least both of them object arrays.
          assert(flags & mask, "one of the two should be known to be an object array");

          if (!(flags & LIR_OpArrayCopy::src_objarray)) {
            __ load_klass(tmp, src);
          } else if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
            __ load_klass(tmp, dst);
          }
          Address klass_lh_addr(tmp, Klass::layout_helper_offset());
          jint objArray_lh = Klass::array_layout_helper(T_OBJECT);
          __ load_const_optimized(Z_R1_scratch, objArray_lh);
          __ z_c(Z_R1_scratch, klass_lh_addr);
          __ branch_optimized(Assembler::bcondNotEqual, *stub->entry());
        }

        // Save outgoing arguments in callee saved registers (C convention) in case
        // a call to System.arraycopy is needed.
        Register callee_saved_src     = Z_R10;
        Register callee_saved_src_pos = Z_R11;
        Register callee_saved_dst     = Z_R12;
        Register callee_saved_dst_pos = Z_R13;
        Register callee_saved_length  = Z_ARG5; // Z_ARG5 == Z_R6 is callee saved.

        __ lgr_if_needed(callee_saved_src, src);
        __ lgr_if_needed(callee_saved_src_pos, src_pos);
        __ lgr_if_needed(callee_saved_dst, dst);
        __ lgr_if_needed(callee_saved_dst_pos, dst_pos);
        __ lgr_if_needed(callee_saved_length, length);

        __ z_llgfr(length, length); // Higher 32bits must be null.

        __ z_sllg(Z_ARG1, src_pos, shift_amount); // index -> byte offset
        __ z_sllg(Z_ARG2, dst_pos, shift_amount); // index -> byte offset

        __ z_la(Z_ARG1, Address(src, Z_ARG1, arrayOopDesc::base_offset_in_bytes(basic_type)));
        assert_different_registers(Z_ARG1, dst, dst_pos, length);
        __ z_la(Z_ARG2, Address(dst, Z_ARG2, arrayOopDesc::base_offset_in_bytes(basic_type)));
        assert_different_registers(Z_ARG2, dst, length);

        __ z_lgr(Z_ARG3, length);
        assert_different_registers(Z_ARG3, dst);

        __ load_klass(Z_ARG5, dst);
        __ z_lg(Z_ARG5, Address(Z_ARG5, ObjArrayKlass::element_klass_offset()));
        __ z_lg(Z_ARG4, Address(Z_ARG5, Klass::super_check_offset_offset()));
        emit_call_c(copyfunc_addr);
        CHECK_BAILOUT2(cont, slow);

#ifndef PRODUCT
        if (PrintC1Statistics) {
          NearLabel failed;
          __ compareU32_and_branch(Z_RET, (intptr_t)0, Assembler::bcondNotEqual, failed);
          __ load_const_optimized(Z_R1_scratch, (address)&Runtime1::_arraycopy_checkcast_cnt);
          __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
          __ bind(failed);
        }
#endif

        __ compareU32_and_branch(Z_RET, (intptr_t)0, Assembler::bcondEqual, *stub->continuation());

#ifndef PRODUCT
        if (PrintC1Statistics) {
          __ load_const_optimized(Z_R1_scratch, (address)&Runtime1::_arraycopy_checkcast_attempt_cnt);
          __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
        }
#endif

        __ z_lgr(tmp, Z_RET);
        __ z_xilf(tmp, -1);

        // Restore previously spilled arguments
        __ lgr_if_needed(src, callee_saved_src);
        __ lgr_if_needed(src_pos, callee_saved_src_pos);
        __ lgr_if_needed(dst, callee_saved_dst);
        __ lgr_if_needed(dst_pos, callee_saved_dst_pos);
        __ lgr_if_needed(length, callee_saved_length);

        __ z_sr(length, tmp);
        __ z_ar(src_pos, tmp);
        __ z_ar(dst_pos, tmp);
      }

      __ branch_optimized(Assembler::bcondAlways, *stub->entry());

      __ bind(cont);
    }
  }

#ifdef ASSERT
  if (basic_type != T_OBJECT || !(flags & LIR_OpArrayCopy::type_check)) {
    // Sanity check the known type with the incoming class. For the
    // primitive case the types must match exactly with src.klass and
    // dst.klass each exactly matching the default type. For the
    // object array case, if no type check is needed then either the
    // dst type is exactly the expected type and the src type is a
    // subtype which we can't check or src is the same array as dst
    // but not necessarily exactly of type default_type.
    NearLabel known_ok, halt;
    metadata2reg(default_type->constant_encoding(), tmp);
    if (UseCompressedClassPointers) {
      __ encode_klass_not_null(tmp);
    }

    if (basic_type != T_OBJECT) {
      if (UseCompressedClassPointers)         { __ z_c (tmp, dst_klass_addr); }
      else                                    { __ z_cg(tmp, dst_klass_addr); }
      __ branch_optimized(Assembler::bcondNotEqual, halt);
      if (UseCompressedClassPointers)         { __ z_c (tmp, src_klass_addr); }
      else                                    { __ z_cg(tmp, src_klass_addr); }
      __ branch_optimized(Assembler::bcondEqual, known_ok);
    } else {
      if (UseCompressedClassPointers)         { __ z_c (tmp, dst_klass_addr); }
      else                                    { __ z_cg(tmp, dst_klass_addr); }
      __ branch_optimized(Assembler::bcondEqual, known_ok);
      __ compareU64_and_branch(src, dst, Assembler::bcondEqual, known_ok);
    }
    __ bind(halt);
    __ stop("incorrect type information in arraycopy");
    __ bind(known_ok);
  }
#endif

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ load_const_optimized(Z_R1_scratch, Runtime1::arraycopy_count_address(basic_type));
    __ add2mem_32(Address(Z_R1_scratch), 1, Z_R0_scratch);
  }
#endif

  __ z_sllg(tmp, src_pos, shift_amount); // index -> byte offset
  __ z_sllg(Z_R1_scratch, dst_pos, shift_amount); // index -> byte offset

  assert_different_registers(Z_ARG1, dst, dst_pos, length);
  __ z_la(Z_ARG1, Address(src, tmp, arrayOopDesc::base_offset_in_bytes(basic_type)));
  assert_different_registers(Z_ARG2, length);
  __ z_la(Z_ARG2, Address(dst, Z_R1_scratch, arrayOopDesc::base_offset_in_bytes(basic_type)));
  __ lgr_if_needed(Z_ARG3, length);

  bool disjoint = (flags & LIR_OpArrayCopy::overlapping) == 0;
  bool aligned = (flags & LIR_OpArrayCopy::unaligned) == 0;
  const char *name;
  address entry = StubRoutines::select_arraycopy_function(basic_type, aligned, disjoint, name, false);
  __ call_VM_leaf(entry);

  __ bind(*stub->continuation());
}

void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, LIR_Opr count, LIR_Opr dest, LIR_Opr tmp) {
  if (dest->is_single_cpu()) {
    if (left->type() == T_OBJECT) {
      switch (code) {
        case lir_shl:  __ z_sllg (dest->as_register(), left->as_register(), 0, count->as_register()); break;
        case lir_shr:  __ z_srag (dest->as_register(), left->as_register(), 0, count->as_register()); break;
        case lir_ushr: __ z_srlg (dest->as_register(), left->as_register(), 0, count->as_register()); break;
        default: ShouldNotReachHere();
      }
    } else {
      assert(code == lir_shl || left == dest, "left and dest must be equal for 2 operand form right shifts");
      Register masked_count = Z_R1_scratch;
      __ z_lr(masked_count, count->as_register());
      __ z_nill(masked_count, 31);
      switch (code) {
        case lir_shl:  __ z_sllg (dest->as_register(), left->as_register(), 0, masked_count); break;
        case lir_shr:  __ z_sra  (dest->as_register(), 0, masked_count); break;
        case lir_ushr: __ z_srl  (dest->as_register(), 0, masked_count); break;
        default: ShouldNotReachHere();
      }
    }
  } else {
    switch (code) {
      case lir_shl:  __ z_sllg (dest->as_register_lo(), left->as_register_lo(), 0, count->as_register()); break;
      case lir_shr:  __ z_srag (dest->as_register_lo(), left->as_register_lo(), 0, count->as_register()); break;
      case lir_ushr: __ z_srlg (dest->as_register_lo(), left->as_register_lo(), 0, count->as_register()); break;
      default: ShouldNotReachHere();
    }
  }
}

void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, jint count, LIR_Opr dest) {
  if (left->type() == T_OBJECT) {
    count = count & 63;  // Shouldn't shift by more than sizeof(intptr_t).
    Register l = left->as_register();
    Register d = dest->as_register_lo();
    switch (code) {
      case lir_shl:  __ z_sllg (d, l, count); break;
      case lir_shr:  __ z_srag (d, l, count); break;
      case lir_ushr: __ z_srlg (d, l, count); break;
      default: ShouldNotReachHere();
    }
    return;
  }
  if (dest->is_single_cpu()) {
    assert(code == lir_shl || left == dest, "left and dest must be equal for 2 operand form right shifts");
    count = count & 0x1F; // Java spec
    switch (code) {
      case lir_shl:  __ z_sllg (dest->as_register(), left->as_register(), count); break;
      case lir_shr:  __ z_sra  (dest->as_register(), count); break;
      case lir_ushr: __ z_srl  (dest->as_register(), count); break;
      default: ShouldNotReachHere();
    }
  } else if (dest->is_double_cpu()) {
    count = count & 63; // Java spec
    Register l = left->as_pointer_register();
    Register d = dest->as_pointer_register();
    switch (code) {
      case lir_shl:  __ z_sllg (d, l, count); break;
      case lir_shr:  __ z_srag (d, l, count); break;
      case lir_ushr: __ z_srlg (d, l, count); break;
      default: ShouldNotReachHere();
    }
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::emit_alloc_obj(LIR_OpAllocObj* op) {
  if (op->init_check()) {
    // Make sure klass is initialized & doesn't have finalizer.
    const int state_offset = in_bytes(InstanceKlass::init_state_offset());
    Register iklass = op->klass()->as_register();
    add_debug_info_for_null_check_here(op->stub()->info());
    if (Immediate::is_uimm12(state_offset)) {
      __ z_cli(state_offset, iklass, InstanceKlass::fully_initialized);
    } else {
      __ z_cliy(state_offset, iklass, InstanceKlass::fully_initialized);
    }
    __ branch_optimized(Assembler::bcondNotEqual, *op->stub()->entry()); // Use long branch, because slow_case might be far.
  }
  __ allocate_object(op->obj()->as_register(),
                     op->tmp1()->as_register(),
                     op->tmp2()->as_register(),
                     op->header_size(),
                     op->object_size(),
                     op->klass()->as_register(),
                     *op->stub()->entry());
  __ bind(*op->stub()->continuation());
  __ verify_oop(op->obj()->as_register(), FILE_AND_LINE);
}

void LIR_Assembler::emit_alloc_array(LIR_OpAllocArray* op) {
  Register len = op->len()->as_register();
  __ move_reg_if_needed(len, T_LONG, len, T_INT); // sign extend

  if (UseSlowPath ||
      (!UseFastNewObjectArray && (is_reference_type(op->type()))) ||
      (!UseFastNewTypeArray   && (!is_reference_type(op->type())))) {
    __ z_brul(*op->stub()->entry());
  } else {
    __ allocate_array(op->obj()->as_register(),
                      op->len()->as_register(),
                      op->tmp1()->as_register(),
                      op->tmp2()->as_register(),
                      arrayOopDesc::header_size(op->type()),
                      type2aelembytes(op->type()),
                      op->klass()->as_register(),
                      *op->stub()->entry());
  }
  __ bind(*op->stub()->continuation());
}

void LIR_Assembler::type_profile_helper(Register mdo, ciMethodData *md, ciProfileData *data,
                                        Register recv, Register tmp1, Label* update_done) {
  uint i;
  for (i = 0; i < VirtualCallData::row_limit(); i++) {
    Label next_test;
    // See if the receiver is receiver[n].
    Address receiver_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)));
    __ z_cg(recv, receiver_addr);
    __ z_brne(next_test);
    Address data_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)));
    __ add2mem_64(data_addr, DataLayout::counter_increment, tmp1);
    __ branch_optimized(Assembler::bcondAlways, *update_done);
    __ bind(next_test);
  }

  // Didn't find receiver; find next empty slot and fill it in.
  for (i = 0; i < VirtualCallData::row_limit(); i++) {
    Label next_test;
    Address recv_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)));
    __ z_ltg(Z_R0_scratch, recv_addr);
    __ z_brne(next_test);
    __ z_stg(recv, recv_addr);
    __ load_const_optimized(tmp1, DataLayout::counter_increment);
    __ z_stg(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)), mdo);
    __ branch_optimized(Assembler::bcondAlways, *update_done);
    __ bind(next_test);
  }
}

void LIR_Assembler::setup_md_access(ciMethod* method, int bci,
                                    ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias) {
  Unimplemented();
}

void LIR_Assembler::store_parameter(Register r, int param_num) {
  assert(param_num >= 0, "invalid num");
  int offset_in_bytes = param_num * BytesPerWord + FrameMap::first_available_sp_in_frame;
  assert(offset_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ z_stg(r, offset_in_bytes, Z_SP);
}

void LIR_Assembler::store_parameter(jint c, int param_num) {
  assert(param_num >= 0, "invalid num");
  int offset_in_bytes = param_num * BytesPerWord + FrameMap::first_available_sp_in_frame;
  assert(offset_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ store_const(Address(Z_SP, offset_in_bytes), c, Z_R1_scratch, true);
}

void LIR_Assembler::emit_typecheck_helper(LIR_OpTypeCheck *op, Label* success, Label* failure, Label* obj_is_null) {
  // We always need a stub for the failure case.
  CodeStub* stub = op->stub();
  Register obj = op->object()->as_register();
  Register k_RInfo = op->tmp1()->as_register();
  Register klass_RInfo = op->tmp2()->as_register();
  Register dst = op->result_opr()->as_register();
  Register Rtmp1 = Z_R1_scratch;
  ciKlass* k = op->klass();

  assert(!op->tmp3()->is_valid(), "tmp3's not needed");

  // Check if it needs to be profiled.
  ciMethodData* md = NULL;
  ciProfileData* data = NULL;

  if (op->should_profile()) {
    ciMethod* method = op->profiled_method();
    assert(method != NULL, "Should have method");
    int bci = op->profiled_bci();
    md = method->method_data_or_null();
    assert(md != NULL, "Sanity");
    data = md->bci_to_data(bci);
    assert(data != NULL,                "need data for type check");
    assert(data->is_ReceiverTypeData(), "need ReceiverTypeData for type check");
  }

  // Temp operands do not overlap with inputs, if this is their last
  // use (end of range is exclusive), so a register conflict is possible.
  if (obj == k_RInfo) {
    k_RInfo = dst;
  } else if (obj == klass_RInfo) {
    klass_RInfo = dst;
  }
  assert_different_registers(obj, k_RInfo, klass_RInfo);

  if (op->should_profile()) {
    NearLabel not_null;
    __ compareU64_and_branch(obj, (intptr_t) 0, Assembler::bcondNotEqual, not_null);
    // Object is null; update MDO and exit.
    Register mdo = klass_RInfo;
    metadata2reg(md->constant_encoding(), mdo);
    Address data_addr(mdo, md->byte_offset_of_slot(data, DataLayout::header_offset()));
    int header_bits = DataLayout::flag_mask_to_header_mask(BitData::null_seen_byte_constant());
    __ or2mem_8(data_addr, header_bits);
    __ branch_optimized(Assembler::bcondAlways, *obj_is_null);
    __ bind(not_null);
  } else {
    __ compareU64_and_branch(obj, (intptr_t) 0, Assembler::bcondEqual, *obj_is_null);
  }

  NearLabel profile_cast_failure, profile_cast_success;
  Label *failure_target = op->should_profile() ? &profile_cast_failure : failure;
  Label *success_target = op->should_profile() ? &profile_cast_success : success;

  // Patching may screw with our temporaries,
  // so let's do it before loading the class.
  if (k->is_loaded()) {
    metadata2reg(k->constant_encoding(), k_RInfo);
  } else {
    klass2reg_with_patching(k_RInfo, op->info_for_patch());
  }
  assert(obj != k_RInfo, "must be different");

  __ verify_oop(obj, FILE_AND_LINE);

  // Get object class.
  // Not a safepoint as obj null check happens earlier.
  if (op->fast_check()) {
    if (UseCompressedClassPointers) {
      __ load_klass(klass_RInfo, obj);
      __ compareU64_and_branch(k_RInfo, klass_RInfo, Assembler::bcondNotEqual, *failure_target);
    } else {
      __ z_cg(k_RInfo, Address(obj, oopDesc::klass_offset_in_bytes()));
      __ branch_optimized(Assembler::bcondNotEqual, *failure_target);
    }
    // Successful cast, fall through to profile or jump.
  } else {
    bool need_slow_path = !k->is_loaded() ||
                          ((int) k->super_check_offset() == in_bytes(Klass::secondary_super_cache_offset()));
    intptr_t super_check_offset = k->is_loaded() ? k->super_check_offset() : -1L;
    __ load_klass(klass_RInfo, obj);
    // Perform the fast part of the checking logic.
    __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1,
                                     (need_slow_path ? success_target : NULL),
                                     failure_target, NULL,
                                     RegisterOrConstant(super_check_offset));
    if (need_slow_path) {
      // Call out-of-line instance of __ check_klass_subtype_slow_path(...):
      address a = Runtime1::entry_for (Runtime1::slow_subtype_check_id);
      store_parameter(klass_RInfo, 0); // sub
      store_parameter(k_RInfo, 1);     // super
      emit_call_c(a); // Sets condition code 0 for match (2 otherwise).
      CHECK_BAILOUT2(profile_cast_failure, profile_cast_success);
      __ branch_optimized(Assembler::bcondNotEqual, *failure_target);
      // Fall through to success case.
    }
  }

  if (op->should_profile()) {
    Register mdo = klass_RInfo, recv = k_RInfo;
    assert_different_registers(obj, mdo, recv);
    __ bind(profile_cast_success);
    metadata2reg(md->constant_encoding(), mdo);
    __ load_klass(recv, obj);
    type_profile_helper(mdo, md, data, recv, Rtmp1, success);
    __ branch_optimized(Assembler::bcondAlways, *success);

    __ bind(profile_cast_failure);
    metadata2reg(md->constant_encoding(), mdo);
    __ add2mem_64(Address(mdo, md->byte_offset_of_slot(data, CounterData::count_offset())), -(int)DataLayout::counter_increment, Rtmp1);
    __ branch_optimized(Assembler::bcondAlways, *failure);
  } else {
    __ branch_optimized(Assembler::bcondAlways, *success);
  }
}

void LIR_Assembler::emit_opTypeCheck(LIR_OpTypeCheck* op) {
  LIR_Code code = op->code();
  if (code == lir_store_check) {
    Register value = op->object()->as_register();
    Register array = op->array()->as_register();
    Register k_RInfo = op->tmp1()->as_register();
    Register klass_RInfo = op->tmp2()->as_register();
    Register Rtmp1 = Z_R1_scratch;

    CodeStub* stub = op->stub();

    // Check if it needs to be profiled.
    ciMethodData* md = NULL;
    ciProfileData* data = NULL;

    assert_different_registers(value, k_RInfo, klass_RInfo);

    if (op->should_profile()) {
      ciMethod* method = op->profiled_method();
      assert(method != NULL, "Should have method");
      int bci = op->profiled_bci();
      md = method->method_data_or_null();
      assert(md != NULL, "Sanity");
      data = md->bci_to_data(bci);
      assert(data != NULL,                "need data for type check");
      assert(data->is_ReceiverTypeData(), "need ReceiverTypeData for type check");
    }
    NearLabel profile_cast_success, profile_cast_failure, done;
    Label *success_target = op->should_profile() ? &profile_cast_success : &done;
    Label *failure_target = op->should_profile() ? &profile_cast_failure : stub->entry();

    if (op->should_profile()) {
      NearLabel not_null;
      __ compareU64_and_branch(value, (intptr_t) 0, Assembler::bcondNotEqual, not_null);
      // Object is null; update MDO and exit.
      Register mdo = klass_RInfo;
      metadata2reg(md->constant_encoding(), mdo);
      Address data_addr(mdo, md->byte_offset_of_slot(data, DataLayout::header_offset()));
      int header_bits = DataLayout::flag_mask_to_header_mask(BitData::null_seen_byte_constant());
      __ or2mem_8(data_addr, header_bits);
      __ branch_optimized(Assembler::bcondAlways, done);
      __ bind(not_null);
    } else {
      __ compareU64_and_branch(value, (intptr_t) 0, Assembler::bcondEqual, done);
    }

    add_debug_info_for_null_check_here(op->info_for_exception());
    __ load_klass(k_RInfo, array);
    __ load_klass(klass_RInfo, value);

    // Get instance klass (it's already uncompressed).
    __ z_lg(k_RInfo, Address(k_RInfo, ObjArrayKlass::element_klass_offset()));
    // Perform the fast part of the checking logic.
    __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, success_target, failure_target, NULL);
    // Call out-of-line instance of __ check_klass_subtype_slow_path(...):
    address a = Runtime1::entry_for (Runtime1::slow_subtype_check_id);
    store_parameter(klass_RInfo, 0); // sub
    store_parameter(k_RInfo, 1);     // super
    emit_call_c(a); // Sets condition code 0 for match (2 otherwise).
    CHECK_BAILOUT3(profile_cast_success, profile_cast_failure, done);
    __ branch_optimized(Assembler::bcondNotEqual, *failure_target);
    // Fall through to success case.

    if (op->should_profile()) {
      Register mdo = klass_RInfo, recv = k_RInfo;
      assert_different_registers(value, mdo, recv);
      __ bind(profile_cast_success);
      metadata2reg(md->constant_encoding(), mdo);
      __ load_klass(recv, value);
      type_profile_helper(mdo, md, data, recv, Rtmp1, &done);
      __ branch_optimized(Assembler::bcondAlways, done);

      __ bind(profile_cast_failure);
      metadata2reg(md->constant_encoding(), mdo);
      __ add2mem_64(Address(mdo, md->byte_offset_of_slot(data, CounterData::count_offset())), -(int)DataLayout::counter_increment, Rtmp1);
      __ branch_optimized(Assembler::bcondAlways, *stub->entry());
    }

    __ bind(done);
  } else {
    if (code == lir_checkcast) {
      Register obj = op->object()->as_register();
      Register dst = op->result_opr()->as_register();
      NearLabel success;
      emit_typecheck_helper(op, &success, op->stub()->entry(), &success);
      __ bind(success);
      __ lgr_if_needed(dst, obj);
    } else {
      if (code == lir_instanceof) {
        Register obj = op->object()->as_register();
        Register dst = op->result_opr()->as_register();
        NearLabel success, failure, done;
        emit_typecheck_helper(op, &success, &failure, &failure);
        __ bind(failure);
        __ clear_reg(dst);
        __ branch_optimized(Assembler::bcondAlways, done);
        __ bind(success);
        __ load_const_optimized(dst, 1);
        __ bind(done);
      } else {
        ShouldNotReachHere();
      }
    }
  }
}

void LIR_Assembler::emit_compare_and_swap(LIR_OpCompareAndSwap* op) {
  Register addr = op->addr()->as_pointer_register();
  Register t1_cmp = Z_R1_scratch;
  if (op->code() == lir_cas_long) {
    assert(VM_Version::supports_cx8(), "wrong machine");
    Register cmp_value_lo = op->cmp_value()->as_register_lo();
    Register new_value_lo = op->new_value()->as_register_lo();
    __ z_lgr(t1_cmp, cmp_value_lo);
    // Perform the compare and swap operation.
    __ z_csg(t1_cmp, new_value_lo, 0, addr);
  } else if (op->code() == lir_cas_int || op->code() == lir_cas_obj) {
    Register cmp_value = op->cmp_value()->as_register();
    Register new_value = op->new_value()->as_register();
    if (op->code() == lir_cas_obj) {
      if (UseCompressedOops) {
                 t1_cmp = op->tmp1()->as_register();
        Register t2_new = op->tmp2()->as_register();
        assert_different_registers(cmp_value, new_value, addr, t1_cmp, t2_new);
        __ oop_encoder(t1_cmp, cmp_value, true /*maybe null*/);
        __ oop_encoder(t2_new, new_value, true /*maybe null*/);
        __ z_cs(t1_cmp, t2_new, 0, addr);
      } else {
        __ z_lgr(t1_cmp, cmp_value);
        __ z_csg(t1_cmp, new_value, 0, addr);
      }
    } else {
      __ z_lr(t1_cmp, cmp_value);
      __ z_cs(t1_cmp, new_value, 0, addr);
    }
  } else {
    ShouldNotReachHere(); // new lir_cas_??
  }
}

void LIR_Assembler::breakpoint() {
  Unimplemented();
  //  __ breakpoint_trap();
}

void LIR_Assembler::push(LIR_Opr opr) {
  ShouldNotCallThis(); // unused
}

void LIR_Assembler::pop(LIR_Opr opr) {
  ShouldNotCallThis(); // unused
}

void LIR_Assembler::monitor_address(int monitor_no, LIR_Opr dst_opr) {
  Address addr = frame_map()->address_for_monitor_lock(monitor_no);
  __ add2reg(dst_opr->as_register(), addr.disp(), addr.base());
}

void LIR_Assembler::emit_lock(LIR_OpLock* op) {
  Register obj = op->obj_opr()->as_register();  // May not be an oop.
  Register hdr = op->hdr_opr()->as_register();
  Register lock = op->lock_opr()->as_register();
  if (!UseFastLocking) {
    __ branch_optimized(Assembler::bcondAlways, *op->stub()->entry());
  } else if (op->code() == lir_lock) {
    assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
    // Add debug info for NullPointerException only if one is possible.
    if (op->info() != NULL) {
      add_debug_info_for_null_check_here(op->info());
    }
    __ lock_object(hdr, obj, lock, *op->stub()->entry());
    // done
  } else if (op->code() == lir_unlock) {
    assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
    __ unlock_object(hdr, obj, lock, *op->stub()->entry());
  } else {
    ShouldNotReachHere();
  }
  __ bind(*op->stub()->continuation());
}

void LIR_Assembler::emit_profile_call(LIR_OpProfileCall* op) {
  ciMethod* method = op->profiled_method();
  int bci          = op->profiled_bci();
  ciMethod* callee = op->profiled_callee();

  // Update counter for all call types.
  ciMethodData* md = method->method_data_or_null();
  assert(md != NULL, "Sanity");
  ciProfileData* data = md->bci_to_data(bci);
  assert(data != NULL && data->is_CounterData(), "need CounterData for calls");
  assert(op->mdo()->is_single_cpu(),  "mdo must be allocated");
  Register mdo  = op->mdo()->as_register();
  assert(op->tmp1()->is_double_cpu(), "tmp1 must be allocated");
  Register tmp1 = op->tmp1()->as_register_lo();
  metadata2reg(md->constant_encoding(), mdo);

  Address counter_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()));
  // Perform additional virtual call profiling for invokevirtual and
  // invokeinterface bytecodes
  if (op->should_profile_receiver_type()) {
    assert(op->recv()->is_single_cpu(), "recv must be allocated");
    Register recv = op->recv()->as_register();
    assert_different_registers(mdo, tmp1, recv);
    assert(data->is_VirtualCallData(), "need VirtualCallData for virtual calls");
    ciKlass* known_klass = op->known_holder();
    if (C1OptimizeVirtualCallProfiling && known_klass != NULL) {
      // We know the type that will be seen at this call site; we can
      // statically update the MethodData* rather than needing to do
      // dynamic tests on the receiver type.

      // NOTE: we should probably put a lock around this search to
      // avoid collisions by concurrent compilations.
      ciVirtualCallData* vc_data = (ciVirtualCallData*) data;
      uint i;
      for (i = 0; i < VirtualCallData::row_limit(); i++) {
        ciKlass* receiver = vc_data->receiver(i);
        if (known_klass->equals(receiver)) {
          Address data_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)));
          __ add2mem_64(data_addr, DataLayout::counter_increment, tmp1);
          return;
        }
      }

      // Receiver type not found in profile data. Select an empty slot.

      // Note that this is less efficient than it should be because it
      // always does a write to the receiver part of the
      // VirtualCallData rather than just the first time.
      for (i = 0; i < VirtualCallData::row_limit(); i++) {
        ciKlass* receiver = vc_data->receiver(i);
        if (receiver == NULL) {
          Address recv_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_offset(i)));
          metadata2reg(known_klass->constant_encoding(), tmp1);
          __ z_stg(tmp1, recv_addr);
          Address data_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)));
          __ add2mem_64(data_addr, DataLayout::counter_increment, tmp1);
          return;
        }
      }
    } else {
      __ load_klass(recv, recv);
      NearLabel update_done;
      type_profile_helper(mdo, md, data, recv, tmp1, &update_done);
      // Receiver did not match any saved receiver and there is no empty row for it.
      // Increment total counter to indicate polymorphic case.
      __ add2mem_64(counter_addr, DataLayout::counter_increment, tmp1);
      __ bind(update_done);
    }
  } else {
    // static call
    __ add2mem_64(counter_addr, DataLayout::counter_increment, tmp1);
  }
}

void LIR_Assembler::align_backward_branch_target() {
  __ align(OptoLoopAlignment);
}

void LIR_Assembler::emit_delay(LIR_OpDelay* op) {
  ShouldNotCallThis(); // There are no delay slots on ZARCH_64.
}

void LIR_Assembler::negate(LIR_Opr left, LIR_Opr dest, LIR_Opr tmp) {
  // tmp must be unused
  assert(tmp->is_illegal(), "wasting a register if tmp is allocated");
  assert(left->is_register(), "can only handle registers");

  if (left->is_single_cpu()) {
    __ z_lcr(dest->as_register(), left->as_register());
  } else if (left->is_single_fpu()) {
    __ z_lcebr(dest->as_float_reg(), left->as_float_reg());
  } else if (left->is_double_fpu()) {
    __ z_lcdbr(dest->as_double_reg(), left->as_double_reg());
  } else {
    assert(left->is_double_cpu(), "Must be a long");
    __ z_lcgr(dest->as_register_lo(), left->as_register_lo());
  }
}

void LIR_Assembler::rt_call(LIR_Opr result, address dest,
                            const LIR_OprList* args, LIR_Opr tmp, CodeEmitInfo* info) {
  assert(!tmp->is_valid(), "don't need temporary");
  emit_call_c(dest);
  CHECK_BAILOUT();
  if (info != NULL) {
    add_call_info_here(info);
  }
}

void LIR_Assembler::volatile_move_op(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info) {
  ShouldNotCallThis(); // not needed on ZARCH_64
}

void LIR_Assembler::membar() {
  __ z_fence();
}

void LIR_Assembler::membar_acquire() {
  __ z_acquire();
}

void LIR_Assembler::membar_release() {
  __ z_release();
}

void LIR_Assembler::membar_loadload() {
  __ z_acquire();
}

void LIR_Assembler::membar_storestore() {
  __ z_release();
}

void LIR_Assembler::membar_loadstore() {
  __ z_acquire();
}

void LIR_Assembler::membar_storeload() {
  __ z_fence();
}

void LIR_Assembler::on_spin_wait() {
  Unimplemented();
}

void LIR_Assembler::leal(LIR_Opr addr_opr, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  assert(patch_code == lir_patch_none, "Patch code not supported");
  LIR_Address* addr = addr_opr->as_address_ptr();
  assert(addr->scale() == LIR_Address::times_1, "scaling unsupported");
  __ load_address(dest->as_pointer_register(), as_Address(addr));
}

void LIR_Assembler::get_thread(LIR_Opr result_reg) {
  ShouldNotCallThis(); // unused
}

#ifdef ASSERT
// Emit run-time assertion.
void LIR_Assembler::emit_assert(LIR_OpAssert* op) {
  Unimplemented();
}
#endif

void LIR_Assembler::peephole(LIR_List*) {
  // Do nothing for now.
}

void LIR_Assembler::atomic_op(LIR_Code code, LIR_Opr src, LIR_Opr data, LIR_Opr dest, LIR_Opr tmp) {
  assert(code == lir_xadd, "lir_xchg not supported");
  Address src_addr = as_Address(src->as_address_ptr());
  Register base = src_addr.base();
  intptr_t disp = src_addr.disp();
  if (src_addr.index()->is_valid()) {
    // LAA and LAAG do not support index register.
    __ load_address(Z_R1_scratch, src_addr);
    base = Z_R1_scratch;
    disp = 0;
  }
  if (data->type() == T_INT) {
    __ z_laa(dest->as_register(), data->as_register(), disp, base);
  } else if (data->type() == T_LONG) {
    assert(data->as_register_lo() == data->as_register_hi(), "should be a single register");
    __ z_laag(dest->as_register_lo(), data->as_register_lo(), disp, base);
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::emit_profile_type(LIR_OpProfileType* op) {
  Register obj = op->obj()->as_register();
  Register tmp1 = op->tmp()->as_pointer_register();
  Register tmp2 = Z_R1_scratch;
  Address mdo_addr = as_Address(op->mdp()->as_address_ptr());
  ciKlass* exact_klass = op->exact_klass();
  intptr_t current_klass = op->current_klass();
  bool not_null = op->not_null();
  bool no_conflict = op->no_conflict();

  Label update, next, none, null_seen, init_klass;

  bool do_null = !not_null;
  bool exact_klass_set = exact_klass != NULL && ciTypeEntries::valid_ciklass(current_klass) == exact_klass;
  bool do_update = !TypeEntries::is_type_unknown(current_klass) && !exact_klass_set;

  assert(do_null || do_update, "why are we here?");
  assert(!TypeEntries::was_null_seen(current_klass) || do_update, "why are we here?");

  __ verify_oop(obj, FILE_AND_LINE);

  if (do_null || tmp1 != obj DEBUG_ONLY(|| true)) {
    __ z_ltgr(tmp1, obj);
  }
  if (do_null) {
    __ z_brnz(update);
    if (!TypeEntries::was_null_seen(current_klass)) {
      __ z_lg(tmp1, mdo_addr);
      __ z_oill(tmp1, TypeEntries::null_seen);
      __ z_stg(tmp1, mdo_addr);
    }
    if (do_update) {
      __ z_bru(next);
    }
  } else {
    __ asm_assert_ne("unexpect null obj", __LINE__);
  }

  __ bind(update);

  if (do_update) {
#ifdef ASSERT
    if (exact_klass != NULL) {
      __ load_klass(tmp1, tmp1);
      metadata2reg(exact_klass->constant_encoding(), tmp2);
      __ z_cgr(tmp1, tmp2);
      __ asm_assert_eq("exact klass and actual klass differ", __LINE__);
    }
#endif

    Label do_update;
    __ z_lg(tmp2, mdo_addr);

    if (!no_conflict) {
      if (exact_klass == NULL || TypeEntries::is_type_none(current_klass)) {
        if (exact_klass != NULL) {
          metadata2reg(exact_klass->constant_encoding(), tmp1);
        } else {
          __ load_klass(tmp1, tmp1);
        }

        // Klass seen before: nothing to do (regardless of unknown bit).
        __ z_lgr(Z_R0_scratch, tmp2);
        assert(Immediate::is_uimm(~TypeEntries::type_klass_mask, 16), "or change following instruction");
        __ z_nill(Z_R0_scratch, TypeEntries::type_klass_mask & 0xFFFF);
        __ compareU64_and_branch(Z_R0_scratch, tmp1, Assembler::bcondEqual, next);

        // Already unknown: Nothing to do anymore.
        __ z_tmll(tmp2, TypeEntries::type_unknown);
        __ z_brc(Assembler::bcondAllOne, next);

        if (TypeEntries::is_type_none(current_klass)) {
          __ z_lgr(Z_R0_scratch, tmp2);
          assert(Immediate::is_uimm(~TypeEntries::type_mask, 16), "or change following instruction");
          __ z_nill(Z_R0_scratch, TypeEntries::type_mask & 0xFFFF);
          __ compareU64_and_branch(Z_R0_scratch, (intptr_t)0, Assembler::bcondEqual, init_klass);
        }
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "conflict only");

        // Already unknown: Nothing to do anymore.
        __ z_tmll(tmp2, TypeEntries::type_unknown);
        __ z_brc(Assembler::bcondAllOne, next);
      }

      // Different than before. Cannot keep accurate profile.
      __ z_oill(tmp2, TypeEntries::type_unknown);
      __ z_bru(do_update);
    } else {
      // There's a single possible klass at this profile point.
      assert(exact_klass != NULL, "should be");
      if (TypeEntries::is_type_none(current_klass)) {
        metadata2reg(exact_klass->constant_encoding(), tmp1);
        __ z_lgr(Z_R0_scratch, tmp2);
        assert(Immediate::is_uimm(~TypeEntries::type_klass_mask, 16), "or change following instruction");
        __ z_nill(Z_R0_scratch, TypeEntries::type_klass_mask & 0xFFFF);
        __ compareU64_and_branch(Z_R0_scratch, tmp1, Assembler::bcondEqual, next);
#ifdef ASSERT
        {
          Label ok;
          __ z_lgr(Z_R0_scratch, tmp2);
          assert(Immediate::is_uimm(~TypeEntries::type_mask, 16), "or change following instruction");
          __ z_nill(Z_R0_scratch, TypeEntries::type_mask & 0xFFFF);
          __ compareU64_and_branch(Z_R0_scratch, (intptr_t)0, Assembler::bcondEqual, ok);
          __ stop("unexpected profiling mismatch");
          __ bind(ok);
        }
#endif

      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "inconsistent");

        // Already unknown: Nothing to do anymore.
        __ z_tmll(tmp2, TypeEntries::type_unknown);
        __ z_brc(Assembler::bcondAllOne, next);
        __ z_oill(tmp2, TypeEntries::type_unknown);
        __ z_bru(do_update);
      }
    }

    __ bind(init_klass);
    // Combine klass and null_seen bit (only used if (tmp & type_mask)==0).
    __ z_ogr(tmp2, tmp1);

    __ bind(do_update);
    __ z_stg(tmp2, mdo_addr);

    __ bind(next);
  }
}

void LIR_Assembler::emit_updatecrc32(LIR_OpUpdateCRC32* op) {
  assert(op->crc()->is_single_cpu(), "crc must be register");
  assert(op->val()->is_single_cpu(), "byte value must be register");
  assert(op->result_opr()->is_single_cpu(), "result must be register");
  Register crc = op->crc()->as_register();
  Register val = op->val()->as_register();
  Register res = op->result_opr()->as_register();

  assert_different_registers(val, crc, res);

  __ load_const_optimized(res, StubRoutines::crc_table_addr());
  __ kernel_crc32_singleByteReg(crc, val, res, true);
  __ z_lgfr(res, crc);
}

#undef __
