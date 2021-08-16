/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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
#include "nativeInst_ppc.hpp"
#include "oops/compressedOops.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/powerOfTwo.hpp"

#define __ _masm->


const ConditionRegister LIR_Assembler::BOOL_RESULT = CCR5;


bool LIR_Assembler::is_small_constant(LIR_Opr opr) {
  Unimplemented(); return false; // Currently not used on this platform.
}


LIR_Opr LIR_Assembler::receiverOpr() {
  return FrameMap::R3_oop_opr;
}


LIR_Opr LIR_Assembler::osrBufferPointer() {
  return FrameMap::R3_opr;
}


// This specifies the stack pointer decrement needed to build the frame.
int LIR_Assembler::initial_frame_size_in_bytes() const {
  return in_bytes(frame_map()->framesize_in_bytes());
}


// Inline cache check: the inline cached class is in inline_cache_reg;
// we fetch the class of the receiver and compare it with the cached class.
// If they do not match we jump to slow case.
int LIR_Assembler::check_icache() {
  int offset = __ offset();
  __ inline_cache_check(R3_ARG1, R19_inline_cache_reg);
  return offset;
}

void LIR_Assembler::clinit_barrier(ciMethod* method) {
  assert(!method->holder()->is_not_initialized(), "initialization should have been started");

  Label L_skip_barrier;
  Register klass = R20;

  metadata2reg(method->holder()->constant_encoding(), klass);
  __ clinit_barrier(klass, R16_thread, &L_skip_barrier /*L_fast_path*/);

  __ load_const_optimized(klass, SharedRuntime::get_handle_wrong_method_stub(), R0);
  __ mtctr(klass);
  __ bctr();

  __ bind(L_skip_barrier);
}

void LIR_Assembler::osr_entry() {
  // On-stack-replacement entry sequence:
  //
  //   1. Create a new compiled activation.
  //   2. Initialize local variables in the compiled activation. The expression
  //      stack must be empty at the osr_bci; it is not initialized.
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
  // and the last slot is local[0] (receiver) from the interpreter.
  //
  // Similarly with locks. The first lock slot in the osr buffer is the nth lock
  // from the interpreter frame, the nth lock slot in the osr buffer is 0th lock
  // in the interpreter frame (the method lock if a sync method).

  // Initialize monitors in the compiled activation.
  //   R3: pointer to osr buffer
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
#ifdef ASSERT
      // Verify the interpreter's monitor has a non-null object.
      {
        Label L;
        __ ld(R0, slot_offset + 1*BytesPerWord, OSR_buf);
        __ cmpdi(CCR0, R0, 0);
        __ bne(CCR0, L);
        __ stop("locked object is NULL");
        __ bind(L);
      }
#endif // ASSERT
      // Copy the lock field into the compiled activation.
      Address ml = frame_map()->address_for_monitor_lock(i),
              mo = frame_map()->address_for_monitor_object(i);
      assert(ml.index() == noreg && mo.index() == noreg, "sanity");
      __ ld(R0, slot_offset + 0, OSR_buf);
      __ std(R0, ml.disp(), ml.base());
      __ ld(R0, slot_offset + 1*BytesPerWord, OSR_buf);
      __ std(R0, mo.disp(), mo.base());
    }
  }
}


int LIR_Assembler::emit_exception_handler() {
  // If the last instruction is a call (typically to do a throw which
  // is coming at the end after block reordering) the return address
  // must still point into the code area in order to avoid assertion
  // failures when searching for the corresponding bci => add a nop
  // (was bug 5/14/1999 - gri).
  __ nop();

  // Generate code for the exception handler.
  address handler_base = __ start_a_stub(exception_handler_size());

  if (handler_base == NULL) {
    // Not enough space left for the handler.
    bailout("exception handler overflow");
    return -1;
  }

  int offset = code_offset();
  address entry_point = CAST_FROM_FN_PTR(address, Runtime1::entry_for(Runtime1::handle_exception_from_callee_id));
  //__ load_const_optimized(R0, entry_point);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(entry_point));
  __ mtctr(R0);
  __ bctr();

  guarantee(code_offset() - offset <= exception_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}


// Emit the code to remove the frame from the stack in the exception
// unwind path.
int LIR_Assembler::emit_unwind_handler() {
  _masm->block_comment("Unwind handler");

  int offset = code_offset();
  bool preserve_exception = method()->is_synchronized() || compilation()->env()->dtrace_method_probes();
  const Register Rexception = R3 /*LIRGenerator::exceptionOopOpr()*/, Rexception_save = R31;

  // Fetch the exception from TLS and clear out exception related thread state.
  __ ld(Rexception, in_bytes(JavaThread::exception_oop_offset()), R16_thread);
  __ li(R0, 0);
  __ std(R0, in_bytes(JavaThread::exception_oop_offset()), R16_thread);
  __ std(R0, in_bytes(JavaThread::exception_pc_offset()), R16_thread);

  __ bind(_unwind_handler_entry);
  __ verify_not_null_oop(Rexception);
  if (preserve_exception) { __ mr(Rexception_save, Rexception); }

  // Perform needed unlocking
  MonitorExitStub* stub = NULL;
  if (method()->is_synchronized()) {
    monitor_address(0, FrameMap::R4_opr);
    stub = new MonitorExitStub(FrameMap::R4_opr, true, 0);
    __ unlock_object(R5, R6, R4, *stub->entry());
    __ bind(*stub->continuation());
  }

  if (compilation()->env()->dtrace_method_probes()) {
    Unimplemented();
  }

  // Dispatch to the unwind logic.
  address unwind_stub = Runtime1::entry_for(Runtime1::unwind_exception_id);
  //__ load_const_optimized(R0, unwind_stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(unwind_stub));
  if (preserve_exception) { __ mr(Rexception, Rexception_save); }
  __ mtctr(R0);
  __ bctr();

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
  // failures when searching for the corresponding bci => add a nop
  // (was bug 5/14/1999 - gri).
  __ nop();

  // Generate code for deopt handler.
  address handler_base = __ start_a_stub(deopt_handler_size());

  if (handler_base == NULL) {
    // Not enough space left for the handler.
    bailout("deopt handler overflow");
    return -1;
  }

  int offset = code_offset();
  __ bl64_patchable(SharedRuntime::deopt_blob()->unpack(), relocInfo::runtime_call_type);

  guarantee(code_offset() - offset <= deopt_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}


void LIR_Assembler::jobject2reg(jobject o, Register reg) {
  if (o == NULL) {
    __ li(reg, 0);
  } else {
    AddressLiteral addrlit = __ constant_oop_address(o);
    __ load_const(reg, addrlit, (reg != R0) ? R0 : noreg);
  }
}


void LIR_Assembler::jobject2reg_with_patching(Register reg, CodeEmitInfo *info) {
  // Allocate a new index in table to hold the object once it's been patched.
  int oop_index = __ oop_recorder()->allocate_oop_index(NULL);
  PatchingStub* patch = new PatchingStub(_masm, patching_id(info), oop_index);

  AddressLiteral addrlit((address)NULL, oop_Relocation::spec(oop_index));
  __ load_const(reg, addrlit, R0);

  patching_epilog(patch, lir_patch_normal, reg, info);
}


void LIR_Assembler::metadata2reg(Metadata* o, Register reg) {
  AddressLiteral md = __ constant_metadata_address(o); // Notify OOP recorder (don't need the relocation)
  __ load_const_optimized(reg, md.value(), (reg != R0) ? R0 : noreg);
}


void LIR_Assembler::klass2reg_with_patching(Register reg, CodeEmitInfo *info) {
  // Allocate a new index in table to hold the klass once it's been patched.
  int index = __ oop_recorder()->allocate_metadata_index(NULL);
  PatchingStub* patch = new PatchingStub(_masm, PatchingStub::load_klass_id, index);

  AddressLiteral addrlit((address)NULL, metadata_Relocation::spec(index));
  assert(addrlit.rspec().type() == relocInfo::metadata_type, "must be an metadata reloc");
  __ load_const(reg, addrlit, R0);

  patching_epilog(patch, lir_patch_normal, reg, info);
}


void LIR_Assembler::arithmetic_idiv(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr temp, LIR_Opr result, CodeEmitInfo* info) {
  const bool is_int = result->is_single_cpu();
  Register Rdividend = is_int ? left->as_register() : left->as_register_lo();
  Register Rdivisor  = noreg;
  Register Rscratch  = temp->as_register();
  Register Rresult   = is_int ? result->as_register() : result->as_register_lo();
  long divisor = -1;

  if (right->is_register()) {
    Rdivisor = is_int ? right->as_register() : right->as_register_lo();
  } else {
    divisor = is_int ? right->as_constant_ptr()->as_jint()
                     : right->as_constant_ptr()->as_jlong();
  }

  assert(Rdividend != Rscratch, "");
  assert(Rdivisor  != Rscratch, "");
  assert(code == lir_idiv || code == lir_irem, "Must be irem or idiv");

  if (Rdivisor == noreg) {
    if (divisor == 1) { // stupid, but can happen
      if (code == lir_idiv) {
        __ mr_if_needed(Rresult, Rdividend);
      } else {
        __ li(Rresult, 0);
      }

    } else if (is_power_of_2(divisor)) {
      // Convert division by a power of two into some shifts and logical operations.
      int log2 = log2i_exact(divisor);

      // Round towards 0.
      if (divisor == 2) {
        if (is_int) {
          __ srwi(Rscratch, Rdividend, 31);
        } else {
          __ srdi(Rscratch, Rdividend, 63);
        }
      } else {
        if (is_int) {
          __ srawi(Rscratch, Rdividend, 31);
        } else {
          __ sradi(Rscratch, Rdividend, 63);
        }
        __ clrldi(Rscratch, Rscratch, 64-log2);
      }
      __ add(Rscratch, Rdividend, Rscratch);

      if (code == lir_idiv) {
        if (is_int) {
          __ srawi(Rresult, Rscratch, log2);
        } else {
          __ sradi(Rresult, Rscratch, log2);
        }
      } else { // lir_irem
        __ clrrdi(Rscratch, Rscratch, log2);
        __ sub(Rresult, Rdividend, Rscratch);
      }

    } else if (divisor == -1) {
      if (code == lir_idiv) {
        __ neg(Rresult, Rdividend);
      } else {
        __ li(Rresult, 0);
      }

    } else {
      __ load_const_optimized(Rscratch, divisor);
      if (code == lir_idiv) {
        if (is_int) {
          __ divw(Rresult, Rdividend, Rscratch); // Can't divide minint/-1.
        } else {
          __ divd(Rresult, Rdividend, Rscratch); // Can't divide minint/-1.
        }
      } else {
        assert(Rscratch != R0, "need both");
        if (is_int) {
          __ divw(R0, Rdividend, Rscratch); // Can't divide minint/-1.
          __ mullw(Rscratch, R0, Rscratch);
        } else {
          __ divd(R0, Rdividend, Rscratch); // Can't divide minint/-1.
          __ mulld(Rscratch, R0, Rscratch);
        }
        __ sub(Rresult, Rdividend, Rscratch);
      }

    }
    return;
  }

  Label regular, done;
  if (is_int) {
    __ cmpwi(CCR0, Rdivisor, -1);
  } else {
    __ cmpdi(CCR0, Rdivisor, -1);
  }
  __ bne(CCR0, regular);
  if (code == lir_idiv) {
    __ neg(Rresult, Rdividend);
    __ b(done);
    __ bind(regular);
    if (is_int) {
      __ divw(Rresult, Rdividend, Rdivisor); // Can't divide minint/-1.
    } else {
      __ divd(Rresult, Rdividend, Rdivisor); // Can't divide minint/-1.
    }
  } else { // lir_irem
    __ li(Rresult, 0);
    __ b(done);
    __ bind(regular);
    if (is_int) {
      __ divw(Rscratch, Rdividend, Rdivisor); // Can't divide minint/-1.
      __ mullw(Rscratch, Rscratch, Rdivisor);
    } else {
      __ divd(Rscratch, Rdividend, Rdivisor); // Can't divide minint/-1.
      __ mulld(Rscratch, Rscratch, Rdivisor);
    }
    __ sub(Rresult, Rdividend, Rscratch);
  }
  __ bind(done);
}


void LIR_Assembler::emit_op3(LIR_Op3* op) {
  switch (op->code()) {
  case lir_idiv:
  case lir_irem:
    arithmetic_idiv(op->code(), op->in_opr1(), op->in_opr2(), op->in_opr3(),
                    op->result_opr(), op->info());
    break;
  case lir_fmad:
    __ fmadd(op->result_opr()->as_double_reg(), op->in_opr1()->as_double_reg(),
             op->in_opr2()->as_double_reg(), op->in_opr3()->as_double_reg());
    break;
  case lir_fmaf:
    __ fmadds(op->result_opr()->as_float_reg(), op->in_opr1()->as_float_reg(),
              op->in_opr2()->as_float_reg(), op->in_opr3()->as_float_reg());
    break;
  default: ShouldNotReachHere(); break;
  }
}


void LIR_Assembler::emit_opBranch(LIR_OpBranch* op) {
#ifdef ASSERT
  assert(op->block() == NULL || op->block()->label() == op->label(), "wrong label");
  if (op->block() != NULL)  _branch_target_blocks.append(op->block());
  if (op->ublock() != NULL) _branch_target_blocks.append(op->ublock());
  assert(op->info() == NULL, "shouldn't have CodeEmitInfo");
#endif

  Label *L = op->label();
  if (op->cond() == lir_cond_always) {
    __ b(*L);
  } else {
    Label done;
    bool is_unordered = false;
    if (op->code() == lir_cond_float_branch) {
      assert(op->ublock() != NULL, "must have unordered successor");
      is_unordered = true;
    } else {
      assert(op->code() == lir_branch, "just checking");
    }

    bool positive = false;
    Assembler::Condition cond = Assembler::equal;
    switch (op->cond()) {
      case lir_cond_equal:        positive = true ; cond = Assembler::equal  ; is_unordered = false; break;
      case lir_cond_notEqual:     positive = false; cond = Assembler::equal  ; is_unordered = false; break;
      case lir_cond_less:         positive = true ; cond = Assembler::less   ; break;
      case lir_cond_belowEqual:   assert(op->code() != lir_cond_float_branch, ""); // fallthru
      case lir_cond_lessEqual:    positive = false; cond = Assembler::greater; break;
      case lir_cond_greater:      positive = true ; cond = Assembler::greater; break;
      case lir_cond_aboveEqual:   assert(op->code() != lir_cond_float_branch, ""); // fallthru
      case lir_cond_greaterEqual: positive = false; cond = Assembler::less   ; break;
      default:                    ShouldNotReachHere();
    }
    int bo = positive ? Assembler::bcondCRbiIs1 : Assembler::bcondCRbiIs0;
    int bi = Assembler::bi0(BOOL_RESULT, cond);
    if (is_unordered) {
      if (positive) {
        if (op->ublock() == op->block()) {
          __ bc_far_optimized(Assembler::bcondCRbiIs1, __ bi0(BOOL_RESULT, Assembler::summary_overflow), *L);
        }
      } else {
        if (op->ublock() != op->block()) { __ bso(BOOL_RESULT, done); }
      }
    }
    __ bc_far_optimized(bo, bi, *L);
    __ bind(done);
  }
}


void LIR_Assembler::emit_opConvert(LIR_OpConvert* op) {
  Bytecodes::Code code = op->bytecode();
  LIR_Opr src = op->in_opr(),
          dst = op->result_opr();

  switch(code) {
    case Bytecodes::_i2l: {
      __ extsw(dst->as_register_lo(), src->as_register());
      break;
    }
    case Bytecodes::_l2i: {
      __ mr_if_needed(dst->as_register(), src->as_register_lo()); // high bits are garbage
      break;
    }
    case Bytecodes::_i2b: {
      __ extsb(dst->as_register(), src->as_register());
      break;
    }
    case Bytecodes::_i2c: {
      __ clrldi(dst->as_register(), src->as_register(), 64-16);
      break;
    }
    case Bytecodes::_i2s: {
      __ extsh(dst->as_register(), src->as_register());
      break;
    }
    case Bytecodes::_i2d:
    case Bytecodes::_l2d: {
      bool src_in_memory = !VM_Version::has_mtfprd();
      FloatRegister rdst = dst->as_double_reg();
      FloatRegister rsrc;
      if (src_in_memory) {
        rsrc = src->as_double_reg(); // via mem
      } else {
        // move src to dst register
        if (code == Bytecodes::_i2d) {
          __ mtfprwa(rdst, src->as_register());
        } else {
          __ mtfprd(rdst, src->as_register_lo());
        }
        rsrc = rdst;
      }
      __ fcfid(rdst, rsrc);
      break;
    }
    case Bytecodes::_i2f:
    case Bytecodes::_l2f: {
      bool src_in_memory = !VM_Version::has_mtfprd();
      FloatRegister rdst = dst->as_float_reg();
      FloatRegister rsrc;
      if (src_in_memory) {
        rsrc = src->as_double_reg(); // via mem
      } else {
        // move src to dst register
        if (code == Bytecodes::_i2f) {
          __ mtfprwa(rdst, src->as_register());
        } else {
          __ mtfprd(rdst, src->as_register_lo());
        }
        rsrc = rdst;
      }
      if (VM_Version::has_fcfids()) {
        __ fcfids(rdst, rsrc);
      } else {
        assert(code == Bytecodes::_i2f, "fcfid+frsp needs fixup code to avoid rounding incompatibility");
        __ fcfid(rdst, rsrc);
        __ frsp(rdst, rdst);
      }
      break;
    }
    case Bytecodes::_f2d: {
      __ fmr_if_needed(dst->as_double_reg(), src->as_float_reg());
      break;
    }
    case Bytecodes::_d2f: {
      __ frsp(dst->as_float_reg(), src->as_double_reg());
      break;
    }
    case Bytecodes::_d2i:
    case Bytecodes::_f2i: {
      bool dst_in_memory = !VM_Version::has_mtfprd();
      FloatRegister rsrc = (code == Bytecodes::_d2i) ? src->as_double_reg() : src->as_float_reg();
      Address       addr = dst_in_memory ? frame_map()->address_for_slot(dst->double_stack_ix()) : NULL;
      Label L;
      // Result must be 0 if value is NaN; test by comparing value to itself.
      __ fcmpu(CCR0, rsrc, rsrc);
      if (dst_in_memory) {
        __ li(R0, 0); // 0 in case of NAN
        __ std(R0, addr.disp(), addr.base());
      } else {
        __ li(dst->as_register(), 0);
      }
      __ bso(CCR0, L);
      __ fctiwz(rsrc, rsrc); // USE_KILL
      if (dst_in_memory) {
        __ stfd(rsrc, addr.disp(), addr.base());
      } else {
        __ mffprd(dst->as_register(), rsrc);
      }
      __ bind(L);
      break;
    }
    case Bytecodes::_d2l:
    case Bytecodes::_f2l: {
      bool dst_in_memory = !VM_Version::has_mtfprd();
      FloatRegister rsrc = (code == Bytecodes::_d2l) ? src->as_double_reg() : src->as_float_reg();
      Address       addr = dst_in_memory ? frame_map()->address_for_slot(dst->double_stack_ix()) : NULL;
      Label L;
      // Result must be 0 if value is NaN; test by comparing value to itself.
      __ fcmpu(CCR0, rsrc, rsrc);
      if (dst_in_memory) {
        __ li(R0, 0); // 0 in case of NAN
        __ std(R0, addr.disp(), addr.base());
      } else {
        __ li(dst->as_register_lo(), 0);
      }
      __ bso(CCR0, L);
      __ fctidz(rsrc, rsrc); // USE_KILL
      if (dst_in_memory) {
        __ stfd(rsrc, addr.disp(), addr.base());
      } else {
        __ mffprd(dst->as_register_lo(), rsrc);
      }
      __ bind(L);
      break;
    }

    default: ShouldNotReachHere();
  }
}


void LIR_Assembler::align_call(LIR_Code) {
  // do nothing since all instructions are word aligned on ppc
}


bool LIR_Assembler::emit_trampoline_stub_for_call(address target, Register Rtoc) {
  int start_offset = __ offset();
  // Put the entry point as a constant into the constant pool.
  const address entry_point_toc_addr   = __ address_constant(target, RelocationHolder::none);
  if (entry_point_toc_addr == NULL) {
    bailout("const section overflow");
    return false;
  }
  const int     entry_point_toc_offset = __ offset_to_method_toc(entry_point_toc_addr);

  // Emit the trampoline stub which will be related to the branch-and-link below.
  address stub = __ emit_trampoline_stub(entry_point_toc_offset, start_offset, Rtoc);
  if (!stub) {
    bailout("no space for trampoline stub");
    return false;
  }
  return true;
}


void LIR_Assembler::call(LIR_OpJavaCall* op, relocInfo::relocType rtype) {
  assert(rtype==relocInfo::opt_virtual_call_type || rtype==relocInfo::static_call_type, "unexpected rtype");

  bool success = emit_trampoline_stub_for_call(op->addr());
  if (!success) { return; }

  __ relocate(rtype);
  // Note: At this point we do not have the address of the trampoline
  // stub, and the entry point might be too far away for bl, so __ pc()
  // serves as dummy and the bl will be patched later.
  __ code()->set_insts_mark();
  __ bl(__ pc());
  add_call_info(code_offset(), op->info());
}


void LIR_Assembler::ic_call(LIR_OpJavaCall* op) {
  __ calculate_address_from_global_toc(R2_TOC, __ method_toc());

  // Virtual call relocation will point to ic load.
  address virtual_call_meta_addr = __ pc();
  // Load a clear inline cache.
  AddressLiteral empty_ic((address) Universe::non_oop_word());
  bool success = __ load_const_from_method_toc(R19_inline_cache_reg, empty_ic, R2_TOC);
  if (!success) {
    bailout("const section overflow");
    return;
  }
  // Call to fixup routine. Fixup routine uses ScopeDesc info
  // to determine who we intended to call.
  __ relocate(virtual_call_Relocation::spec(virtual_call_meta_addr));

  success = emit_trampoline_stub_for_call(op->addr(), R2_TOC);
  if (!success) { return; }

  // Note: At this point we do not have the address of the trampoline
  // stub, and the entry point might be too far away for bl, so __ pc()
  // serves as dummy and the bl will be patched later.
  __ bl(__ pc());
  add_call_info(code_offset(), op->info());
}

void LIR_Assembler::explicit_null_check(Register addr, CodeEmitInfo* info) {
  ImplicitNullCheckStub* stub = new ImplicitNullCheckStub(code_offset(), info);
  __ null_check(addr, stub->entry());
  append_code_stub(stub);
}


// Attention: caller must encode oop if needed
int LIR_Assembler::store(LIR_Opr from_reg, Register base, int offset, BasicType type, bool wide) {
  int store_offset;
  if (!Assembler::is_simm16(offset)) {
    // For offsets larger than a simm16 we setup the offset.
    assert(wide && !from_reg->is_same_register(FrameMap::R0_opr), "large offset only supported in special case");
    __ load_const_optimized(R0, offset);
    store_offset = store(from_reg, base, R0, type, wide);
  } else {
    store_offset = code_offset();
    switch (type) {
      case T_BOOLEAN: // fall through
      case T_BYTE  : __ stb(from_reg->as_register(), offset, base); break;
      case T_CHAR  :
      case T_SHORT : __ sth(from_reg->as_register(), offset, base); break;
      case T_INT   : __ stw(from_reg->as_register(), offset, base); break;
      case T_LONG  : __ std(from_reg->as_register_lo(), offset, base); break;
      case T_ADDRESS:
      case T_METADATA: __ std(from_reg->as_register(), offset, base); break;
      case T_ARRAY : // fall through
      case T_OBJECT:
        {
          if (UseCompressedOops && !wide) {
            // Encoding done in caller
            __ stw(from_reg->as_register(), offset, base);
            __ verify_coop(from_reg->as_register(), FILE_AND_LINE);
          } else {
            __ std(from_reg->as_register(), offset, base);
            __ verify_oop(from_reg->as_register(), FILE_AND_LINE);
          }
          break;
        }
      case T_FLOAT : __ stfs(from_reg->as_float_reg(), offset, base); break;
      case T_DOUBLE: __ stfd(from_reg->as_double_reg(), offset, base); break;
      default      : ShouldNotReachHere();
    }
  }
  return store_offset;
}


// Attention: caller must encode oop if needed
int LIR_Assembler::store(LIR_Opr from_reg, Register base, Register disp, BasicType type, bool wide) {
  int store_offset = code_offset();
  switch (type) {
    case T_BOOLEAN: // fall through
    case T_BYTE  : __ stbx(from_reg->as_register(), base, disp); break;
    case T_CHAR  :
    case T_SHORT : __ sthx(from_reg->as_register(), base, disp); break;
    case T_INT   : __ stwx(from_reg->as_register(), base, disp); break;
    case T_LONG  :
#ifdef _LP64
      __ stdx(from_reg->as_register_lo(), base, disp);
#else
      Unimplemented();
#endif
      break;
    case T_ADDRESS:
      __ stdx(from_reg->as_register(), base, disp);
      break;
    case T_ARRAY : // fall through
    case T_OBJECT:
      {
        if (UseCompressedOops && !wide) {
          // Encoding done in caller.
          __ stwx(from_reg->as_register(), base, disp);
          __ verify_coop(from_reg->as_register(), FILE_AND_LINE); // kills R0
        } else {
          __ stdx(from_reg->as_register(), base, disp);
          __ verify_oop(from_reg->as_register(), FILE_AND_LINE); // kills R0
        }
        break;
      }
    case T_FLOAT : __ stfsx(from_reg->as_float_reg(), base, disp); break;
    case T_DOUBLE: __ stfdx(from_reg->as_double_reg(), base, disp); break;
    default      : ShouldNotReachHere();
  }
  return store_offset;
}


int LIR_Assembler::load(Register base, int offset, LIR_Opr to_reg, BasicType type, bool wide) {
  int load_offset;
  if (!Assembler::is_simm16(offset)) {
    // For offsets larger than a simm16 we setup the offset.
    __ load_const_optimized(R0, offset);
    load_offset = load(base, R0, to_reg, type, wide);
  } else {
    load_offset = code_offset();
    switch(type) {
      case T_BOOLEAN: // fall through
      case T_BYTE  :   __ lbz(to_reg->as_register(), offset, base);
                       __ extsb(to_reg->as_register(), to_reg->as_register()); break;
      case T_CHAR  :   __ lhz(to_reg->as_register(), offset, base); break;
      case T_SHORT :   __ lha(to_reg->as_register(), offset, base); break;
      case T_INT   :   __ lwa(to_reg->as_register(), offset, base); break;
      case T_LONG  :   __ ld(to_reg->as_register_lo(), offset, base); break;
      case T_METADATA: __ ld(to_reg->as_register(), offset, base); break;
      case T_ADDRESS:
        if (offset == oopDesc::klass_offset_in_bytes() && UseCompressedClassPointers) {
          __ lwz(to_reg->as_register(), offset, base);
          __ decode_klass_not_null(to_reg->as_register());
        } else {
          __ ld(to_reg->as_register(), offset, base);
        }
        break;
      case T_ARRAY : // fall through
      case T_OBJECT:
        {
          if (UseCompressedOops && !wide) {
            __ lwz(to_reg->as_register(), offset, base);
            __ decode_heap_oop(to_reg->as_register());
          } else {
            __ ld(to_reg->as_register(), offset, base);
          }
          __ verify_oop(to_reg->as_register(), FILE_AND_LINE);
          break;
        }
      case T_FLOAT:  __ lfs(to_reg->as_float_reg(), offset, base); break;
      case T_DOUBLE: __ lfd(to_reg->as_double_reg(), offset, base); break;
      default      : ShouldNotReachHere();
    }
  }
  return load_offset;
}


int LIR_Assembler::load(Register base, Register disp, LIR_Opr to_reg, BasicType type, bool wide) {
  int load_offset = code_offset();
  switch(type) {
    case T_BOOLEAN: // fall through
    case T_BYTE  :  __ lbzx(to_reg->as_register(), base, disp);
                    __ extsb(to_reg->as_register(), to_reg->as_register()); break;
    case T_CHAR  :  __ lhzx(to_reg->as_register(), base, disp); break;
    case T_SHORT :  __ lhax(to_reg->as_register(), base, disp); break;
    case T_INT   :  __ lwax(to_reg->as_register(), base, disp); break;
    case T_ADDRESS: __ ldx(to_reg->as_register(), base, disp); break;
    case T_ARRAY : // fall through
    case T_OBJECT:
      {
        if (UseCompressedOops && !wide) {
          __ lwzx(to_reg->as_register(), base, disp);
          __ decode_heap_oop(to_reg->as_register());
        } else {
          __ ldx(to_reg->as_register(), base, disp);
        }
        __ verify_oop(to_reg->as_register(), FILE_AND_LINE);
        break;
      }
    case T_FLOAT:  __ lfsx(to_reg->as_float_reg() , base, disp); break;
    case T_DOUBLE: __ lfdx(to_reg->as_double_reg(), base, disp); break;
    case T_LONG  :
#ifdef _LP64
      __ ldx(to_reg->as_register_lo(), base, disp);
#else
      Unimplemented();
#endif
      break;
    default      : ShouldNotReachHere();
  }
  return load_offset;
}


void LIR_Assembler::const2stack(LIR_Opr src, LIR_Opr dest) {
  LIR_Const* c = src->as_constant_ptr();
  Register src_reg = R0;
  switch (c->type()) {
    case T_INT:
    case T_FLOAT: {
      int value = c->as_jint_bits();
      __ load_const_optimized(src_reg, value);
      Address addr = frame_map()->address_for_slot(dest->single_stack_ix());
      __ stw(src_reg, addr.disp(), addr.base());
      break;
    }
    case T_ADDRESS: {
      int value = c->as_jint_bits();
      __ load_const_optimized(src_reg, value);
      Address addr = frame_map()->address_for_slot(dest->single_stack_ix());
      __ std(src_reg, addr.disp(), addr.base());
      break;
    }
    case T_OBJECT: {
      jobject2reg(c->as_jobject(), src_reg);
      Address addr = frame_map()->address_for_slot(dest->single_stack_ix());
      __ std(src_reg, addr.disp(), addr.base());
      break;
    }
    case T_LONG:
    case T_DOUBLE: {
      int value = c->as_jlong_bits();
      __ load_const_optimized(src_reg, value);
      Address addr = frame_map()->address_for_double_slot(dest->double_stack_ix());
      __ std(src_reg, addr.disp(), addr.base());
      break;
    }
    default:
      Unimplemented();
  }
}


void LIR_Assembler::const2mem(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info, bool wide) {
  LIR_Const* c = src->as_constant_ptr();
  LIR_Address* addr = dest->as_address_ptr();
  Register base = addr->base()->as_pointer_register();
  LIR_Opr tmp = LIR_OprFact::illegalOpr;
  int offset = -1;
  // Null check for large offsets in LIRGenerator::do_StoreField.
  bool needs_explicit_null_check = !ImplicitNullChecks;

  if (info != NULL && needs_explicit_null_check) {
    explicit_null_check(base, info);
  }

  switch (c->type()) {
    case T_FLOAT: type = T_INT;
    case T_INT:
    case T_ADDRESS: {
      tmp = FrameMap::R0_opr;
      __ load_const_optimized(tmp->as_register(), c->as_jint_bits());
      break;
    }
    case T_DOUBLE: type = T_LONG;
    case T_LONG: {
      tmp = FrameMap::R0_long_opr;
      __ load_const_optimized(tmp->as_register_lo(), c->as_jlong_bits());
      break;
    }
    case T_OBJECT: {
      tmp = FrameMap::R0_opr;
      if (UseCompressedOops && !wide && c->as_jobject() != NULL) {
        AddressLiteral oop_addr = __ constant_oop_address(c->as_jobject());
        __ lis(R0, oop_addr.value() >> 16); // Don't care about sign extend (will use stw).
        __ relocate(oop_addr.rspec(), /*compressed format*/ 1);
        __ ori(R0, R0, oop_addr.value() & 0xffff);
      } else {
        jobject2reg(c->as_jobject(), R0);
      }
      break;
    }
    default:
      Unimplemented();
  }

  // Handle either reg+reg or reg+disp address.
  if (addr->index()->is_valid()) {
    assert(addr->disp() == 0, "must be zero");
    offset = store(tmp, base, addr->index()->as_pointer_register(), type, wide);
  } else {
    assert(Assembler::is_simm16(addr->disp()), "can't handle larger addresses");
    offset = store(tmp, base, addr->disp(), type, wide);
  }

  if (info != NULL) {
    assert(offset != -1, "offset should've been set");
    if (!needs_explicit_null_check) {
      add_debug_info_for_null_check(offset, info);
    }
  }
}


void LIR_Assembler::const2reg(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  LIR_Const* c = src->as_constant_ptr();
  LIR_Opr to_reg = dest;

  switch (c->type()) {
    case T_INT: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register(), c->as_jint(), R0);
      break;
    }
    case T_ADDRESS: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register(), c->as_jint(), R0);  // Yes, as_jint ...
      break;
    }
    case T_LONG: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ load_const_optimized(dest->as_register_lo(), c->as_jlong(), R0);
      break;
    }

    case T_OBJECT: {
      if (patch_code == lir_patch_none) {
        jobject2reg(c->as_jobject(), to_reg->as_register());
      } else {
        jobject2reg_with_patching(to_reg->as_register(), info);
      }
      break;
    }

    case T_METADATA:
      {
        if (patch_code == lir_patch_none) {
          metadata2reg(c->as_metadata(), to_reg->as_register());
        } else {
          klass2reg_with_patching(to_reg->as_register(), info);
        }
      }
      break;

    case T_FLOAT:
      {
        if (to_reg->is_single_fpu()) {
          address const_addr = __ float_constant(c->as_jfloat());
          if (const_addr == NULL) {
            bailout("const section overflow");
            break;
          }
          RelocationHolder rspec = internal_word_Relocation::spec(const_addr);
          __ relocate(rspec);
          __ load_const(R0, const_addr);
          __ lfsx(to_reg->as_float_reg(), R0);
        } else {
          assert(to_reg->is_single_cpu(), "Must be a cpu register.");
          __ load_const_optimized(to_reg->as_register(), jint_cast(c->as_jfloat()), R0);
        }
      }
      break;

    case T_DOUBLE:
      {
        if (to_reg->is_double_fpu()) {
          address const_addr = __ double_constant(c->as_jdouble());
          if (const_addr == NULL) {
            bailout("const section overflow");
            break;
          }
          RelocationHolder rspec = internal_word_Relocation::spec(const_addr);
          __ relocate(rspec);
          __ load_const(R0, const_addr);
          __ lfdx(to_reg->as_double_reg(), R0);
        } else {
          assert(to_reg->is_double_cpu(), "Must be a long register.");
          __ load_const_optimized(to_reg->as_register_lo(), jlong_cast(c->as_jdouble()), R0);
        }
      }
      break;

    default:
      ShouldNotReachHere();
  }
}


Address LIR_Assembler::as_Address(LIR_Address* addr) {
  Unimplemented(); return Address();
}


inline RegisterOrConstant index_or_disp(LIR_Address* addr) {
  if (addr->index()->is_illegal()) {
    return (RegisterOrConstant)(addr->disp());
  } else {
    return (RegisterOrConstant)(addr->index()->as_pointer_register());
  }
}


void LIR_Assembler::stack2stack(LIR_Opr src, LIR_Opr dest, BasicType type) {
  const Register tmp = R0;
  switch (type) {
    case T_INT:
    case T_FLOAT: {
      Address from = frame_map()->address_for_slot(src->single_stack_ix());
      Address to   = frame_map()->address_for_slot(dest->single_stack_ix());
      __ lwz(tmp, from.disp(), from.base());
      __ stw(tmp, to.disp(), to.base());
      break;
    }
    case T_ADDRESS:
    case T_OBJECT: {
      Address from = frame_map()->address_for_slot(src->single_stack_ix());
      Address to   = frame_map()->address_for_slot(dest->single_stack_ix());
      __ ld(tmp, from.disp(), from.base());
      __ std(tmp, to.disp(), to.base());
      break;
    }
    case T_LONG:
    case T_DOUBLE: {
      Address from = frame_map()->address_for_double_slot(src->double_stack_ix());
      Address to   = frame_map()->address_for_double_slot(dest->double_stack_ix());
      __ ld(tmp, from.disp(), from.base());
      __ std(tmp, to.disp(), to.base());
      break;
    }

    default:
      ShouldNotReachHere();
  }
}


Address LIR_Assembler::as_Address_hi(LIR_Address* addr) {
  Unimplemented(); return Address();
}


Address LIR_Assembler::as_Address_lo(LIR_Address* addr) {
  Unimplemented(); return Address();
}


void LIR_Assembler::mem2reg(LIR_Opr src_opr, LIR_Opr dest, BasicType type,
                            LIR_PatchCode patch_code, CodeEmitInfo* info, bool wide) {

  assert(type != T_METADATA, "load of metadata ptr not supported");
  LIR_Address* addr = src_opr->as_address_ptr();
  LIR_Opr to_reg = dest;

  Register src = addr->base()->as_pointer_register();
  Register disp_reg = noreg;
  int disp_value = addr->disp();
  bool needs_patching = (patch_code != lir_patch_none);
  // null check for large offsets in LIRGenerator::do_LoadField
  bool needs_explicit_null_check = !os::zero_page_read_protected() || !ImplicitNullChecks;

  if (info != NULL && needs_explicit_null_check) {
    explicit_null_check(src, info);
  }

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
    if (!Assembler::is_simm16(disp_value)) {
      if (needs_patching) {
        __ load_const32(R0, 0); // patchable int
      } else {
        __ load_const_optimized(R0, disp_value);
      }
      disp_reg = R0;
    }
  } else {
    disp_reg = addr->index()->as_pointer_register();
    assert(disp_value == 0, "can't handle 3 operand addresses");
  }

  // Remember the offset of the load. The patching_epilog must be done
  // before the call to add_debug_info, otherwise the PcDescs don't get
  // entered in increasing order.
  int offset;

  if (disp_reg == noreg) {
    assert(Assembler::is_simm16(disp_value), "should have set this up");
    offset = load(src, disp_value, to_reg, type, wide);
  } else {
    offset = load(src, disp_reg, to_reg, type, wide);
  }

  if (patch != NULL) {
    patching_epilog(patch, patch_code, src, info);
  }
  if (info != NULL && !needs_explicit_null_check) {
    add_debug_info_for_null_check(offset, info);
  }
}


void LIR_Assembler::stack2reg(LIR_Opr src, LIR_Opr dest, BasicType type) {
  Address addr;
  if (src->is_single_word()) {
    addr = frame_map()->address_for_slot(src->single_stack_ix());
  } else if (src->is_double_word())  {
    addr = frame_map()->address_for_double_slot(src->double_stack_ix());
  }

  load(addr.base(), addr.disp(), dest, dest->type(), true /*wide*/);
}


void LIR_Assembler::reg2stack(LIR_Opr from_reg, LIR_Opr dest, BasicType type, bool pop_fpu_stack) {
  Address addr;
  if (dest->is_single_word()) {
    addr = frame_map()->address_for_slot(dest->single_stack_ix());
  } else if (dest->is_double_word())  {
    addr = frame_map()->address_for_slot(dest->double_stack_ix());
  }

  store(from_reg, addr.base(), addr.disp(), from_reg->type(), true /*wide*/);
}


void LIR_Assembler::reg2reg(LIR_Opr from_reg, LIR_Opr to_reg) {
  if (from_reg->is_float_kind() && to_reg->is_float_kind()) {
    if (from_reg->is_double_fpu()) {
      // double to double moves
      assert(to_reg->is_double_fpu(), "should match");
      __ fmr_if_needed(to_reg->as_double_reg(), from_reg->as_double_reg());
    } else {
      // float to float moves
      assert(to_reg->is_single_fpu(), "should match");
      __ fmr_if_needed(to_reg->as_float_reg(), from_reg->as_float_reg());
    }
  } else if (!from_reg->is_float_kind() && !to_reg->is_float_kind()) {
    if (from_reg->is_double_cpu()) {
      __ mr_if_needed(to_reg->as_pointer_register(), from_reg->as_pointer_register());
    } else if (to_reg->is_double_cpu()) {
      // int to int moves
      __ mr_if_needed(to_reg->as_register_lo(), from_reg->as_register());
    } else {
      // int to int moves
      __ mr_if_needed(to_reg->as_register(), from_reg->as_register());
    }
  } else {
    ShouldNotReachHere();
  }
  if (is_reference_type(to_reg->type())) {
    __ verify_oop(to_reg->as_register(), FILE_AND_LINE);
  }
}


void LIR_Assembler::reg2mem(LIR_Opr from_reg, LIR_Opr dest, BasicType type,
                            LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack,
                            bool wide) {
  assert(type != T_METADATA, "store of metadata ptr not supported");
  LIR_Address* addr = dest->as_address_ptr();

  Register src = addr->base()->as_pointer_register();
  Register disp_reg = noreg;
  int disp_value = addr->disp();
  bool needs_patching = (patch_code != lir_patch_none);
  bool compress_oop = (is_reference_type(type)) && UseCompressedOops && !wide &&
                      CompressedOops::mode() != CompressedOops::UnscaledNarrowOop;
  bool load_disp = addr->index()->is_illegal() && !Assembler::is_simm16(disp_value);
  bool use_R29 = compress_oop && load_disp; // Avoid register conflict, also do null check before killing R29.
  // Null check for large offsets in LIRGenerator::do_StoreField.
  bool needs_explicit_null_check = !ImplicitNullChecks || use_R29;

  if (info != NULL && needs_explicit_null_check) {
    explicit_null_check(src, info);
  }

  if (addr->base()->is_oop_register()) {
    __ verify_oop(src, FILE_AND_LINE);
  }

  PatchingStub* patch = NULL;
  if (needs_patching) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
    assert(!from_reg->is_double_cpu() ||
           patch_code == lir_patch_none ||
           patch_code == lir_patch_normal, "patching doesn't match register");
  }

  if (addr->index()->is_illegal()) {
    if (load_disp) {
      disp_reg = use_R29 ? R29_TOC : R0;
      if (needs_patching) {
        __ load_const32(disp_reg, 0); // patchable int
      } else {
        __ load_const_optimized(disp_reg, disp_value);
      }
    }
  } else {
    disp_reg = addr->index()->as_pointer_register();
    assert(disp_value == 0, "can't handle 3 operand addresses");
  }

  // remember the offset of the store. The patching_epilog must be done
  // before the call to add_debug_info_for_null_check, otherwise the PcDescs don't get
  // entered in increasing order.
  int offset;

  if (compress_oop) {
    Register co = __ encode_heap_oop(R0, from_reg->as_register());
    from_reg = FrameMap::as_opr(co);
  }

  if (disp_reg == noreg) {
    assert(Assembler::is_simm16(disp_value), "should have set this up");
    offset = store(from_reg, src, disp_value, type, wide);
  } else {
    offset = store(from_reg, src, disp_reg, type, wide);
  }

  if (use_R29) {
    __ load_const_optimized(R29_TOC, MacroAssembler::global_toc(), R0); // reinit
  }

  if (patch != NULL) {
    patching_epilog(patch, patch_code, src, info);
  }

  if (info != NULL && !needs_explicit_null_check) {
    add_debug_info_for_null_check(offset, info);
  }
}


void LIR_Assembler::return_op(LIR_Opr result, C1SafepointPollStub* code_stub) {
  const Register return_pc = R31;  // Must survive C-call to enable_stack_reserved_zone().
  const Register temp      = R12;

  // Pop the stack before the safepoint code.
  int frame_size = initial_frame_size_in_bytes();
  if (Assembler::is_simm(frame_size, 16)) {
    __ addi(R1_SP, R1_SP, frame_size);
  } else {
    __ pop_frame();
  }

  // Restore return pc relative to callers' sp.
  __ ld(return_pc, _abi0(lr), R1_SP);
  // Move return pc to LR.
  __ mtlr(return_pc);

  if (StackReservedPages > 0 && compilation()->has_reserved_stack_access()) {
    __ reserved_stack_check(return_pc);
  }

  // We need to mark the code position where the load from the safepoint
  // polling page was emitted as relocInfo::poll_return_type here.
  if (!UseSIGTRAP) {
    code_stub->set_safepoint_offset(__ offset());
    __ relocate(relocInfo::poll_return_type);
  }
  __ safepoint_poll(*code_stub->entry(), temp, true /* at_return */, true /* in_nmethod */);

  // Return.
  __ blr();
}


int LIR_Assembler::safepoint_poll(LIR_Opr tmp, CodeEmitInfo* info) {
  const Register poll_addr = tmp->as_register();
  __ ld(poll_addr, in_bytes(JavaThread::polling_page_offset()), R16_thread);
  if (info != NULL) {
    add_debug_info_for_branch(info);
  }
  int offset = __ offset();
  __ relocate(relocInfo::poll_type);
  __ load_from_polling_page(poll_addr);

  return offset;
}


void LIR_Assembler::emit_static_call_stub() {
  address call_pc = __ pc();
  address stub = __ start_a_stub(static_call_stub_size());
  if (stub == NULL) {
    bailout("static call stub overflow");
    return;
  }

  // For java_to_interp stubs we use R11_scratch1 as scratch register
  // and in call trampoline stubs we use R12_scratch2. This way we
  // can distinguish them (see is_NativeCallTrampolineStub_at()).
  const Register reg_scratch = R11_scratch1;

  // Create a static stub relocation which relates this stub
  // with the call instruction at insts_call_instruction_offset in the
  // instructions code-section.
  int start = __ offset();
  __ relocate(static_stub_Relocation::spec(call_pc));

  // Now, create the stub's code:
  // - load the TOC
  // - load the inline cache oop from the constant pool
  // - load the call target from the constant pool
  // - call
  __ calculate_address_from_global_toc(reg_scratch, __ method_toc());
  AddressLiteral ic = __ allocate_metadata_address((Metadata *)NULL);
  bool success = __ load_const_from_method_toc(R19_inline_cache_reg, ic, reg_scratch, /*fixed_size*/ true);

  if (ReoptimizeCallSequences) {
    __ b64_patchable((address)-1, relocInfo::none);
  } else {
    AddressLiteral a((address)-1);
    success = success && __ load_const_from_method_toc(reg_scratch, a, reg_scratch, /*fixed_size*/ true);
    __ mtctr(reg_scratch);
    __ bctr();
  }
  if (!success) {
    bailout("const section overflow");
    return;
  }

  assert(__ offset() - start <= static_call_stub_size(), "stub too big");
  __ end_a_stub();
}


void LIR_Assembler::comp_op(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Op2* op) {
  bool unsigned_comp = (condition == lir_cond_belowEqual || condition == lir_cond_aboveEqual);
  if (opr1->is_single_fpu()) {
    __ fcmpu(BOOL_RESULT, opr1->as_float_reg(), opr2->as_float_reg());
  } else if (opr1->is_double_fpu()) {
    __ fcmpu(BOOL_RESULT, opr1->as_double_reg(), opr2->as_double_reg());
  } else if (opr1->is_single_cpu()) {
    if (opr2->is_constant()) {
      switch (opr2->as_constant_ptr()->type()) {
        case T_INT:
          {
            jint con = opr2->as_constant_ptr()->as_jint();
            if (unsigned_comp) {
              if (Assembler::is_uimm(con, 16)) {
                __ cmplwi(BOOL_RESULT, opr1->as_register(), con);
              } else {
                __ load_const_optimized(R0, con);
                __ cmplw(BOOL_RESULT, opr1->as_register(), R0);
              }
            } else {
              if (Assembler::is_simm(con, 16)) {
                __ cmpwi(BOOL_RESULT, opr1->as_register(), con);
              } else {
                __ load_const_optimized(R0, con);
                __ cmpw(BOOL_RESULT, opr1->as_register(), R0);
              }
            }
          }
          break;

        case T_OBJECT:
          // There are only equal/notequal comparisons on objects.
          {
            assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "oops");
            jobject con = opr2->as_constant_ptr()->as_jobject();
            if (con == NULL) {
              __ cmpdi(BOOL_RESULT, opr1->as_register(), 0);
            } else {
              jobject2reg(con, R0);
              __ cmpd(BOOL_RESULT, opr1->as_register(), R0);
            }
          }
          break;

        case T_METADATA:
          // We only need, for now, comparison with NULL for metadata.
          {
            assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "oops");
            Metadata* p = opr2->as_constant_ptr()->as_metadata();
            if (p == NULL) {
              __ cmpdi(BOOL_RESULT, opr1->as_register(), 0);
            } else {
              ShouldNotReachHere();
            }
          }
          break;

        default:
          ShouldNotReachHere();
          break;
      }
    } else {
      assert(opr1->type() != T_ADDRESS && opr2->type() != T_ADDRESS, "currently unsupported");
      if (is_reference_type(opr1->type())) {
        // There are only equal/notequal comparisons on objects.
        assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "oops");
        __ cmpd(BOOL_RESULT, opr1->as_register(), opr2->as_register());
      } else {
        if (unsigned_comp) {
          __ cmplw(BOOL_RESULT, opr1->as_register(), opr2->as_register());
        } else {
          __ cmpw(BOOL_RESULT, opr1->as_register(), opr2->as_register());
        }
      }
    }
  } else if (opr1->is_double_cpu()) {
    if (opr2->is_constant()) {
      jlong con = opr2->as_constant_ptr()->as_jlong();
      if (unsigned_comp) {
        if (Assembler::is_uimm(con, 16)) {
          __ cmpldi(BOOL_RESULT, opr1->as_register_lo(), con);
        } else {
          __ load_const_optimized(R0, con);
          __ cmpld(BOOL_RESULT, opr1->as_register_lo(), R0);
        }
      } else {
        if (Assembler::is_simm(con, 16)) {
          __ cmpdi(BOOL_RESULT, opr1->as_register_lo(), con);
        } else {
          __ load_const_optimized(R0, con);
          __ cmpd(BOOL_RESULT, opr1->as_register_lo(), R0);
        }
      }
    } else if (opr2->is_register()) {
      if (unsigned_comp) {
        __ cmpld(BOOL_RESULT, opr1->as_register_lo(), opr2->as_register_lo());
      } else {
        __ cmpd(BOOL_RESULT, opr1->as_register_lo(), opr2->as_register_lo());
      }
    } else {
      ShouldNotReachHere();
    }
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::comp_fl2i(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst, LIR_Op2* op){
  const Register Rdst = dst->as_register();
  if (code == lir_cmp_fd2i || code == lir_ucmp_fd2i) {
    bool is_unordered_less = (code == lir_ucmp_fd2i);
    if (left->is_single_fpu()) {
      __ fcmpu(CCR0, left->as_float_reg(), right->as_float_reg());
    } else if (left->is_double_fpu()) {
      __ fcmpu(CCR0, left->as_double_reg(), right->as_double_reg());
    } else {
      ShouldNotReachHere();
    }
    __ set_cmpu3(Rdst, is_unordered_less); // is_unordered_less ? -1 : 1
  } else if (code == lir_cmp_l2i) {
    __ cmpd(CCR0, left->as_register_lo(), right->as_register_lo());
    __ set_cmp3(Rdst);  // set result as follows: <: -1, =: 0, >: 1
  } else {
    ShouldNotReachHere();
  }
}


inline void load_to_reg(LIR_Assembler *lasm, LIR_Opr src, LIR_Opr dst) {
  if (src->is_constant()) {
    lasm->const2reg(src, dst, lir_patch_none, NULL);
  } else if (src->is_register()) {
    lasm->reg2reg(src, dst);
  } else if (src->is_stack()) {
    lasm->stack2reg(src, dst, dst->type());
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::cmove(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, BasicType type) {
  if (opr1->is_equal(opr2) || opr1->is_same_register(opr2)) {
    load_to_reg(this, opr1, result); // Condition doesn't matter.
    return;
  }

  bool positive = false;
  Assembler::Condition cond = Assembler::equal;
  switch (condition) {
    case lir_cond_equal:        positive = true ; cond = Assembler::equal  ; break;
    case lir_cond_notEqual:     positive = false; cond = Assembler::equal  ; break;
    case lir_cond_less:         positive = true ; cond = Assembler::less   ; break;
    case lir_cond_belowEqual:
    case lir_cond_lessEqual:    positive = false; cond = Assembler::greater; break;
    case lir_cond_greater:      positive = true ; cond = Assembler::greater; break;
    case lir_cond_aboveEqual:
    case lir_cond_greaterEqual: positive = false; cond = Assembler::less   ; break;
    default:                    ShouldNotReachHere();
  }

  // Try to use isel on >=Power7.
  if (VM_Version::has_isel() && result->is_cpu_register()) {
    bool o1_is_reg = opr1->is_cpu_register(), o2_is_reg = opr2->is_cpu_register();
    const Register result_reg = result->is_single_cpu() ? result->as_register() : result->as_register_lo();

    // We can use result_reg to load one operand if not already in register.
    Register first  = o1_is_reg ? (opr1->is_single_cpu() ? opr1->as_register() : opr1->as_register_lo()) : result_reg,
             second = o2_is_reg ? (opr2->is_single_cpu() ? opr2->as_register() : opr2->as_register_lo()) : result_reg;

    if (first != second) {
      if (!o1_is_reg) {
        load_to_reg(this, opr1, result);
      }

      if (!o2_is_reg) {
        load_to_reg(this, opr2, result);
      }

      __ isel(result_reg, BOOL_RESULT, cond, !positive, first, second);
      return;
    }
  } // isel

  load_to_reg(this, opr1, result);

  Label skip;
  int bo = positive ? Assembler::bcondCRbiIs1 : Assembler::bcondCRbiIs0;
  int bi = Assembler::bi0(BOOL_RESULT, cond);
  __ bc(bo, bi, skip);

  load_to_reg(this, opr2, result);
  __ bind(skip);
}


void LIR_Assembler::arith_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest,
                             CodeEmitInfo* info, bool pop_fpu_stack) {
  assert(info == NULL, "unused on this code path");
  assert(left->is_register(), "wrong items state");
  assert(dest->is_register(), "wrong items state");

  if (right->is_register()) {
    if (dest->is_float_kind()) {

      FloatRegister lreg, rreg, res;
      if (right->is_single_fpu()) {
        lreg = left->as_float_reg();
        rreg = right->as_float_reg();
        res  = dest->as_float_reg();
        switch (code) {
          case lir_add: __ fadds(res, lreg, rreg); break;
          case lir_sub: __ fsubs(res, lreg, rreg); break;
          case lir_mul: __ fmuls(res, lreg, rreg); break;
          case lir_div: __ fdivs(res, lreg, rreg); break;
          default: ShouldNotReachHere();
        }
      } else {
        lreg = left->as_double_reg();
        rreg = right->as_double_reg();
        res  = dest->as_double_reg();
        switch (code) {
          case lir_add: __ fadd(res, lreg, rreg); break;
          case lir_sub: __ fsub(res, lreg, rreg); break;
          case lir_mul: __ fmul(res, lreg, rreg); break;
          case lir_div: __ fdiv(res, lreg, rreg); break;
          default: ShouldNotReachHere();
        }
      }

    } else if (dest->is_double_cpu()) {

      Register dst_lo = dest->as_register_lo();
      Register op1_lo = left->as_pointer_register();
      Register op2_lo = right->as_pointer_register();

      switch (code) {
        case lir_add: __ add(dst_lo, op1_lo, op2_lo); break;
        case lir_sub: __ sub(dst_lo, op1_lo, op2_lo); break;
        case lir_mul: __ mulld(dst_lo, op1_lo, op2_lo); break;
        default: ShouldNotReachHere();
      }
    } else {
      assert (right->is_single_cpu(), "Just Checking");

      Register lreg = left->as_register();
      Register res  = dest->as_register();
      Register rreg = right->as_register();
      switch (code) {
        case lir_add:  __ add  (res, lreg, rreg); break;
        case lir_sub:  __ sub  (res, lreg, rreg); break;
        case lir_mul:  __ mullw(res, lreg, rreg); break;
        default: ShouldNotReachHere();
      }
    }
  } else {
    assert (right->is_constant(), "must be constant");

    if (dest->is_single_cpu()) {
      Register lreg = left->as_register();
      Register res  = dest->as_register();
      int    simm16 = right->as_constant_ptr()->as_jint();

      switch (code) {
        case lir_sub:  assert(Assembler::is_simm16(-simm16), "cannot encode"); // see do_ArithmeticOp_Int
                       simm16 = -simm16;
        case lir_add:  if (res == lreg && simm16 == 0) break;
                       __ addi(res, lreg, simm16); break;
        case lir_mul:  if (res == lreg && simm16 == 1) break;
                       __ mulli(res, lreg, simm16); break;
        default: ShouldNotReachHere();
      }
    } else {
      Register lreg = left->as_pointer_register();
      Register res  = dest->as_register_lo();
      long con = right->as_constant_ptr()->as_jlong();
      assert(Assembler::is_simm16(con), "must be simm16");

      switch (code) {
        case lir_sub:  assert(Assembler::is_simm16(-con), "cannot encode");  // see do_ArithmeticOp_Long
                       con = -con;
        case lir_add:  if (res == lreg && con == 0) break;
                       __ addi(res, lreg, (int)con); break;
        case lir_mul:  if (res == lreg && con == 1) break;
                       __ mulli(res, lreg, (int)con); break;
        default: ShouldNotReachHere();
      }
    }
  }
}


void LIR_Assembler::intrinsic_op(LIR_Code code, LIR_Opr value, LIR_Opr thread, LIR_Opr dest, LIR_Op* op) {
  switch (code) {
    case lir_sqrt: {
      __ fsqrt(dest->as_double_reg(), value->as_double_reg());
      break;
    }
    case lir_abs: {
      __ fabs(dest->as_double_reg(), value->as_double_reg());
      break;
    }
    default: {
      ShouldNotReachHere();
      break;
    }
  }
}


void LIR_Assembler::logic_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest) {
  if (right->is_constant()) { // see do_LogicOp
    long uimm;
    Register d, l;
    if (dest->is_single_cpu()) {
      uimm = right->as_constant_ptr()->as_jint();
      d = dest->as_register();
      l = left->as_register();
    } else {
      uimm = right->as_constant_ptr()->as_jlong();
      d = dest->as_register_lo();
      l = left->as_register_lo();
    }
    long uimms  = (unsigned long)uimm >> 16,
         uimmss = (unsigned long)uimm >> 32;

    switch (code) {
      case lir_logic_and:
        if (uimmss != 0 || (uimms != 0 && (uimm & 0xFFFF) != 0) || is_power_of_2(uimm)) {
          __ andi(d, l, uimm); // special cases
        } else if (uimms != 0) { __ andis_(d, l, uimms); }
        else { __ andi_(d, l, uimm); }
        break;

      case lir_logic_or:
        if (uimms != 0) { assert((uimm & 0xFFFF) == 0, "sanity"); __ oris(d, l, uimms); }
        else { __ ori(d, l, uimm); }
        break;

      case lir_logic_xor:
        if (uimm == -1) { __ nand(d, l, l); } // special case
        else if (uimms != 0) { assert((uimm & 0xFFFF) == 0, "sanity"); __ xoris(d, l, uimms); }
        else { __ xori(d, l, uimm); }
        break;

      default: ShouldNotReachHere();
    }
  } else {
    assert(right->is_register(), "right should be in register");

    if (dest->is_single_cpu()) {
      switch (code) {
        case lir_logic_and: __ andr(dest->as_register(), left->as_register(), right->as_register()); break;
        case lir_logic_or:  __ orr (dest->as_register(), left->as_register(), right->as_register()); break;
        case lir_logic_xor: __ xorr(dest->as_register(), left->as_register(), right->as_register()); break;
        default: ShouldNotReachHere();
      }
    } else {
      Register l = (left->is_single_cpu() && left->is_oop_register()) ? left->as_register() :
                                                                        left->as_register_lo();
      Register r = (right->is_single_cpu() && right->is_oop_register()) ? right->as_register() :
                                                                          right->as_register_lo();

      switch (code) {
        case lir_logic_and: __ andr(dest->as_register_lo(), l, r); break;
        case lir_logic_or:  __ orr (dest->as_register_lo(), l, r); break;
        case lir_logic_xor: __ xorr(dest->as_register_lo(), l, r); break;
        default: ShouldNotReachHere();
      }
    }
  }
}


int LIR_Assembler::shift_amount(BasicType t) {
  int elem_size = type2aelembytes(t);
  switch (elem_size) {
    case 1 : return 0;
    case 2 : return 1;
    case 4 : return 2;
    case 8 : return 3;
  }
  ShouldNotReachHere();
  return -1;
}


void LIR_Assembler::throw_op(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info) {
  info->add_register_oop(exceptionOop);

  // Reuse the debug info from the safepoint poll for the throw op itself.
  address pc_for_athrow = __ pc();
  int pc_for_athrow_offset = __ offset();
  //RelocationHolder rspec = internal_word_Relocation::spec(pc_for_athrow);
  //__ relocate(rspec);
  //__ load_const(exceptionPC->as_register(), pc_for_athrow, R0);
  __ calculate_address_from_global_toc(exceptionPC->as_register(), pc_for_athrow, true, true, /*add_relocation*/ true);
  add_call_info(pc_for_athrow_offset, info); // for exception handler

  address stub = Runtime1::entry_for(compilation()->has_fpu_code() ? Runtime1::handle_exception_id
                                                                   : Runtime1::handle_exception_nofpu_id);
  //__ load_const_optimized(R0, stub);
  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mtctr(R0);
  __ bctr();
}


void LIR_Assembler::unwind_op(LIR_Opr exceptionOop) {
  // Note: Not used with EnableDebuggingOnDemand.
  assert(exceptionOop->as_register() == R3, "should match");
  __ b(_unwind_handler_entry);
}


void LIR_Assembler::emit_arraycopy(LIR_OpArrayCopy* op) {
  Register src = op->src()->as_register();
  Register dst = op->dst()->as_register();
  Register src_pos = op->src_pos()->as_register();
  Register dst_pos = op->dst_pos()->as_register();
  Register length  = op->length()->as_register();
  Register tmp = op->tmp()->as_register();
  Register tmp2 = R0;

  int flags = op->flags();
  ciArrayKlass* default_type = op->expected_type();
  BasicType basic_type = default_type != NULL ? default_type->element_type()->basic_type() : T_ILLEGAL;
  if (basic_type == T_ARRAY) basic_type = T_OBJECT;

  // Set up the arraycopy stub information.
  ArrayCopyStub* stub = op->stub();
  const int frame_resize = frame::abi_reg_args_size - sizeof(frame::jit_abi); // C calls need larger frame.

  // Always do stub if no type information is available. It's ok if
  // the known type isn't loaded since the code sanity checks
  // in debug mode and the type isn't required when we know the exact type
  // also check that the type is an array type.
  if (op->expected_type() == NULL) {
    assert(src->is_nonvolatile() && src_pos->is_nonvolatile() && dst->is_nonvolatile() && dst_pos->is_nonvolatile() &&
           length->is_nonvolatile(), "must preserve");
    address copyfunc_addr = StubRoutines::generic_arraycopy();
    assert(copyfunc_addr != NULL, "generic arraycopy stub required");

    // 3 parms are int. Convert to long.
    __ mr(R3_ARG1, src);
    __ extsw(R4_ARG2, src_pos);
    __ mr(R5_ARG3, dst);
    __ extsw(R6_ARG4, dst_pos);
    __ extsw(R7_ARG5, length);

#ifndef PRODUCT
    if (PrintC1Statistics) {
      address counter = (address)&Runtime1::_generic_arraycopystub_cnt;
      int simm16_offs = __ load_const_optimized(tmp, counter, tmp2, true);
      __ lwz(R11_scratch1, simm16_offs, tmp);
      __ addi(R11_scratch1, R11_scratch1, 1);
      __ stw(R11_scratch1, simm16_offs, tmp);
    }
#endif
    __ call_c_with_frame_resize(copyfunc_addr, /*stub does not need resized frame*/ 0);

    __ nand(tmp, R3_RET, R3_RET);
    __ subf(length, tmp, length);
    __ add(src_pos, tmp, src_pos);
    __ add(dst_pos, tmp, dst_pos);

    __ cmpwi(CCR0, R3_RET, 0);
    __ bc_far_optimized(Assembler::bcondCRbiIs1, __ bi0(CCR0, Assembler::less), *stub->entry());
    __ bind(*stub->continuation());
    return;
  }

  assert(default_type != NULL && default_type->is_array_klass(), "must be true at this point");
  Label cont, slow, copyfunc;

  bool simple_check_flag_set = flags & (LIR_OpArrayCopy::src_null_check |
                                        LIR_OpArrayCopy::dst_null_check |
                                        LIR_OpArrayCopy::src_pos_positive_check |
                                        LIR_OpArrayCopy::dst_pos_positive_check |
                                        LIR_OpArrayCopy::length_positive_check);

  // Use only one conditional branch for simple checks.
  if (simple_check_flag_set) {
    ConditionRegister combined_check = CCR1, tmp_check = CCR1;

    // Make sure src and dst are non-null.
    if (flags & LIR_OpArrayCopy::src_null_check) {
      __ cmpdi(combined_check, src, 0);
      tmp_check = CCR0;
    }

    if (flags & LIR_OpArrayCopy::dst_null_check) {
      __ cmpdi(tmp_check, dst, 0);
      if (tmp_check != combined_check) {
        __ cror(combined_check, Assembler::equal, tmp_check, Assembler::equal);
      }
      tmp_check = CCR0;
    }

    // Clear combined_check.eq if not already used.
    if (tmp_check == combined_check) {
      __ crandc(combined_check, Assembler::equal, combined_check, Assembler::equal);
      tmp_check = CCR0;
    }

    if (flags & LIR_OpArrayCopy::src_pos_positive_check) {
      // Test src_pos register.
      __ cmpwi(tmp_check, src_pos, 0);
      __ cror(combined_check, Assembler::equal, tmp_check, Assembler::less);
    }

    if (flags & LIR_OpArrayCopy::dst_pos_positive_check) {
      // Test dst_pos register.
      __ cmpwi(tmp_check, dst_pos, 0);
      __ cror(combined_check, Assembler::equal, tmp_check, Assembler::less);
    }

    if (flags & LIR_OpArrayCopy::length_positive_check) {
      // Make sure length isn't negative.
      __ cmpwi(tmp_check, length, 0);
      __ cror(combined_check, Assembler::equal, tmp_check, Assembler::less);
    }

    __ beq(combined_check, slow);
  }

  // If the compiler was not able to prove that exact type of the source or the destination
  // of the arraycopy is an array type, check at runtime if the source or the destination is
  // an instance type.
  if (flags & LIR_OpArrayCopy::type_check) {
    if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
      __ load_klass(tmp, dst);
      __ lwz(tmp2, in_bytes(Klass::layout_helper_offset()), tmp);
      __ cmpwi(CCR0, tmp2, Klass::_lh_neutral_value);
      __ bge(CCR0, slow);
    }

    if (!(flags & LIR_OpArrayCopy::src_objarray)) {
      __ load_klass(tmp, src);
      __ lwz(tmp2, in_bytes(Klass::layout_helper_offset()), tmp);
      __ cmpwi(CCR0, tmp2, Klass::_lh_neutral_value);
      __ bge(CCR0, slow);
    }
  }

  // Higher 32bits must be null.
  __ extsw(length, length);

  __ extsw(src_pos, src_pos);
  if (flags & LIR_OpArrayCopy::src_range_check) {
    __ lwz(tmp2, arrayOopDesc::length_offset_in_bytes(), src);
    __ add(tmp, length, src_pos);
    __ cmpld(CCR0, tmp2, tmp);
    __ ble(CCR0, slow);
  }

  __ extsw(dst_pos, dst_pos);
  if (flags & LIR_OpArrayCopy::dst_range_check) {
    __ lwz(tmp2, arrayOopDesc::length_offset_in_bytes(), dst);
    __ add(tmp, length, dst_pos);
    __ cmpld(CCR0, tmp2, tmp);
    __ ble(CCR0, slow);
  }

  int shift = shift_amount(basic_type);

  if (!(flags & LIR_OpArrayCopy::type_check)) {
    __ b(cont);
  } else {
    // We don't know the array types are compatible.
    if (basic_type != T_OBJECT) {
      // Simple test for basic type arrays.
      if (UseCompressedClassPointers) {
        // We don't need decode because we just need to compare.
        __ lwz(tmp, oopDesc::klass_offset_in_bytes(), src);
        __ lwz(tmp2, oopDesc::klass_offset_in_bytes(), dst);
        __ cmpw(CCR0, tmp, tmp2);
      } else {
        __ ld(tmp, oopDesc::klass_offset_in_bytes(), src);
        __ ld(tmp2, oopDesc::klass_offset_in_bytes(), dst);
        __ cmpd(CCR0, tmp, tmp2);
      }
      __ beq(CCR0, cont);
    } else {
      // For object arrays, if src is a sub class of dst then we can
      // safely do the copy.
      address copyfunc_addr = StubRoutines::checkcast_arraycopy();

      const Register sub_klass = R5, super_klass = R4; // like CheckCast/InstanceOf
      assert_different_registers(tmp, tmp2, sub_klass, super_klass);

      __ load_klass(sub_klass, src);
      __ load_klass(super_klass, dst);

      __ check_klass_subtype_fast_path(sub_klass, super_klass, tmp, tmp2,
                                       &cont, copyfunc_addr != NULL ? &copyfunc : &slow, NULL);

      address slow_stc = Runtime1::entry_for(Runtime1::slow_subtype_check_id);
      //__ load_const_optimized(tmp, slow_stc, tmp2);
      __ calculate_address_from_global_toc(tmp, slow_stc, true, true, false);
      __ mtctr(tmp);
      __ bctrl(); // sets CR0
      __ beq(CCR0, cont);

      if (copyfunc_addr != NULL) { // Use stub if available.
        __ bind(copyfunc);
        // Src is not a sub class of dst so we have to do a
        // per-element check.
        int mask = LIR_OpArrayCopy::src_objarray|LIR_OpArrayCopy::dst_objarray;
        if ((flags & mask) != mask) {
          assert(flags & mask, "one of the two should be known to be an object array");

          if (!(flags & LIR_OpArrayCopy::src_objarray)) {
            __ load_klass(tmp, src);
          } else if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
            __ load_klass(tmp, dst);
          }

          __ lwz(tmp2, in_bytes(Klass::layout_helper_offset()), tmp);

          jint objArray_lh = Klass::array_layout_helper(T_OBJECT);
          __ load_const_optimized(tmp, objArray_lh);
          __ cmpw(CCR0, tmp, tmp2);
          __ bne(CCR0, slow);
        }

        Register src_ptr = R3_ARG1;
        Register dst_ptr = R4_ARG2;
        Register len     = R5_ARG3;
        Register chk_off = R6_ARG4;
        Register super_k = R7_ARG5;

        __ addi(src_ptr, src, arrayOopDesc::base_offset_in_bytes(basic_type));
        __ addi(dst_ptr, dst, arrayOopDesc::base_offset_in_bytes(basic_type));
        if (shift == 0) {
          __ add(src_ptr, src_pos, src_ptr);
          __ add(dst_ptr, dst_pos, dst_ptr);
        } else {
          __ sldi(tmp, src_pos, shift);
          __ sldi(tmp2, dst_pos, shift);
          __ add(src_ptr, tmp, src_ptr);
          __ add(dst_ptr, tmp2, dst_ptr);
        }

        __ load_klass(tmp, dst);
        __ mr(len, length);

        int ek_offset = in_bytes(ObjArrayKlass::element_klass_offset());
        __ ld(super_k, ek_offset, tmp);

        int sco_offset = in_bytes(Klass::super_check_offset_offset());
        __ lwz(chk_off, sco_offset, super_k);

        __ call_c_with_frame_resize(copyfunc_addr, /*stub does not need resized frame*/ 0);

#ifndef PRODUCT
        if (PrintC1Statistics) {
          Label failed;
          __ cmpwi(CCR0, R3_RET, 0);
          __ bne(CCR0, failed);
          address counter = (address)&Runtime1::_arraycopy_checkcast_cnt;
          int simm16_offs = __ load_const_optimized(tmp, counter, tmp2, true);
          __ lwz(R11_scratch1, simm16_offs, tmp);
          __ addi(R11_scratch1, R11_scratch1, 1);
          __ stw(R11_scratch1, simm16_offs, tmp);
          __ bind(failed);
        }
#endif

        __ nand(tmp, R3_RET, R3_RET);
        __ cmpwi(CCR0, R3_RET, 0);
        __ beq(CCR0, *stub->continuation());

#ifndef PRODUCT
        if (PrintC1Statistics) {
          address counter = (address)&Runtime1::_arraycopy_checkcast_attempt_cnt;
          int simm16_offs = __ load_const_optimized(tmp, counter, tmp2, true);
          __ lwz(R11_scratch1, simm16_offs, tmp);
          __ addi(R11_scratch1, R11_scratch1, 1);
          __ stw(R11_scratch1, simm16_offs, tmp);
        }
#endif

        __ subf(length, tmp, length);
        __ add(src_pos, tmp, src_pos);
        __ add(dst_pos, tmp, dst_pos);
      }
    }
  }
  __ bind(slow);
  __ b(*stub->entry());
  __ bind(cont);

#ifdef ASSERT
  if (basic_type != T_OBJECT || !(flags & LIR_OpArrayCopy::type_check)) {
    // Sanity check the known type with the incoming class. For the
    // primitive case the types must match exactly with src.klass and
    // dst.klass each exactly matching the default type. For the
    // object array case, if no type check is needed then either the
    // dst type is exactly the expected type and the src type is a
    // subtype which we can't check or src is the same array as dst
    // but not necessarily exactly of type default_type.
    Label known_ok, halt;
    metadata2reg(op->expected_type()->constant_encoding(), tmp);
    if (UseCompressedClassPointers) {
      // Tmp holds the default type. It currently comes uncompressed after the
      // load of a constant, so encode it.
      __ encode_klass_not_null(tmp);
      // Load the raw value of the dst klass, since we will be comparing
      // uncompressed values directly.
      __ lwz(tmp2, oopDesc::klass_offset_in_bytes(), dst);
      __ cmpw(CCR0, tmp, tmp2);
      if (basic_type != T_OBJECT) {
        __ bne(CCR0, halt);
        // Load the raw value of the src klass.
        __ lwz(tmp2, oopDesc::klass_offset_in_bytes(), src);
        __ cmpw(CCR0, tmp, tmp2);
        __ beq(CCR0, known_ok);
      } else {
        __ beq(CCR0, known_ok);
        __ cmpw(CCR0, src, dst);
        __ beq(CCR0, known_ok);
      }
    } else {
      __ ld(tmp2, oopDesc::klass_offset_in_bytes(), dst);
      __ cmpd(CCR0, tmp, tmp2);
      if (basic_type != T_OBJECT) {
        __ bne(CCR0, halt);
        // Load the raw value of the src klass.
        __ ld(tmp2, oopDesc::klass_offset_in_bytes(), src);
        __ cmpd(CCR0, tmp, tmp2);
        __ beq(CCR0, known_ok);
      } else {
        __ beq(CCR0, known_ok);
        __ cmpd(CCR0, src, dst);
        __ beq(CCR0, known_ok);
      }
    }
    __ bind(halt);
    __ stop("incorrect type information in arraycopy");
    __ bind(known_ok);
  }
#endif

#ifndef PRODUCT
  if (PrintC1Statistics) {
    address counter = Runtime1::arraycopy_count_address(basic_type);
    int simm16_offs = __ load_const_optimized(tmp, counter, tmp2, true);
    __ lwz(R11_scratch1, simm16_offs, tmp);
    __ addi(R11_scratch1, R11_scratch1, 1);
    __ stw(R11_scratch1, simm16_offs, tmp);
  }
#endif

  Register src_ptr = R3_ARG1;
  Register dst_ptr = R4_ARG2;
  Register len     = R5_ARG3;

  __ addi(src_ptr, src, arrayOopDesc::base_offset_in_bytes(basic_type));
  __ addi(dst_ptr, dst, arrayOopDesc::base_offset_in_bytes(basic_type));
  if (shift == 0) {
    __ add(src_ptr, src_pos, src_ptr);
    __ add(dst_ptr, dst_pos, dst_ptr);
  } else {
    __ sldi(tmp, src_pos, shift);
    __ sldi(tmp2, dst_pos, shift);
    __ add(src_ptr, tmp, src_ptr);
    __ add(dst_ptr, tmp2, dst_ptr);
  }

  bool disjoint = (flags & LIR_OpArrayCopy::overlapping) == 0;
  bool aligned = (flags & LIR_OpArrayCopy::unaligned) == 0;
  const char *name;
  address entry = StubRoutines::select_arraycopy_function(basic_type, aligned, disjoint, name, false);

  // Arraycopy stubs takes a length in number of elements, so don't scale it.
  __ mr(len, length);
  __ call_c_with_frame_resize(entry, /*stub does not need resized frame*/ 0);

  __ bind(*stub->continuation());
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, LIR_Opr count, LIR_Opr dest, LIR_Opr tmp) {
  if (dest->is_single_cpu()) {
    __ rldicl(tmp->as_register(), count->as_register(), 0, 64-5);
#ifdef _LP64
    if (left->type() == T_OBJECT) {
      switch (code) {
        case lir_shl:  __ sld(dest->as_register(), left->as_register(), tmp->as_register()); break;
        case lir_shr:  __ srad(dest->as_register(), left->as_register(), tmp->as_register()); break;
        case lir_ushr: __ srd(dest->as_register(), left->as_register(), tmp->as_register()); break;
        default: ShouldNotReachHere();
      }
    } else
#endif
      switch (code) {
        case lir_shl:  __ slw(dest->as_register(), left->as_register(), tmp->as_register()); break;
        case lir_shr:  __ sraw(dest->as_register(), left->as_register(), tmp->as_register()); break;
        case lir_ushr: __ srw(dest->as_register(), left->as_register(), tmp->as_register()); break;
        default: ShouldNotReachHere();
      }
  } else {
    __ rldicl(tmp->as_register(), count->as_register(), 0, 64-6);
    switch (code) {
      case lir_shl:  __ sld(dest->as_register_lo(), left->as_register_lo(), tmp->as_register()); break;
      case lir_shr:  __ srad(dest->as_register_lo(), left->as_register_lo(), tmp->as_register()); break;
      case lir_ushr: __ srd(dest->as_register_lo(), left->as_register_lo(), tmp->as_register()); break;
      default: ShouldNotReachHere();
    }
  }
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, jint count, LIR_Opr dest) {
#ifdef _LP64
  if (left->type() == T_OBJECT) {
    count = count & 63;  // Shouldn't shift by more than sizeof(intptr_t).
    if (count == 0) { __ mr_if_needed(dest->as_register_lo(), left->as_register()); }
    else {
      switch (code) {
        case lir_shl:  __ sldi(dest->as_register_lo(), left->as_register(), count); break;
        case lir_shr:  __ sradi(dest->as_register_lo(), left->as_register(), count); break;
        case lir_ushr: __ srdi(dest->as_register_lo(), left->as_register(), count); break;
        default: ShouldNotReachHere();
      }
    }
    return;
  }
#endif

  if (dest->is_single_cpu()) {
    count = count & 0x1F; // Java spec
    if (count == 0) { __ mr_if_needed(dest->as_register(), left->as_register()); }
    else {
      switch (code) {
        case lir_shl: __ slwi(dest->as_register(), left->as_register(), count); break;
        case lir_shr:  __ srawi(dest->as_register(), left->as_register(), count); break;
        case lir_ushr: __ srwi(dest->as_register(), left->as_register(), count); break;
        default: ShouldNotReachHere();
      }
    }
  } else if (dest->is_double_cpu()) {
    count = count & 63; // Java spec
    if (count == 0) { __ mr_if_needed(dest->as_pointer_register(), left->as_pointer_register()); }
    else {
      switch (code) {
        case lir_shl:  __ sldi(dest->as_pointer_register(), left->as_pointer_register(), count); break;
        case lir_shr:  __ sradi(dest->as_pointer_register(), left->as_pointer_register(), count); break;
        case lir_ushr: __ srdi(dest->as_pointer_register(), left->as_pointer_register(), count); break;
        default: ShouldNotReachHere();
      }
    }
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::emit_alloc_obj(LIR_OpAllocObj* op) {
  if (op->init_check()) {
    if (!os::zero_page_read_protected() || !ImplicitNullChecks) {
      explicit_null_check(op->klass()->as_register(), op->stub()->info());
    } else {
      add_debug_info_for_null_check_here(op->stub()->info());
    }
    __ lbz(op->tmp1()->as_register(),
           in_bytes(InstanceKlass::init_state_offset()), op->klass()->as_register());
    __ cmpwi(CCR0, op->tmp1()->as_register(), InstanceKlass::fully_initialized);
    __ bc_far_optimized(Assembler::bcondCRbiIs0, __ bi0(CCR0, Assembler::equal), *op->stub()->entry());
  }
  __ allocate_object(op->obj()->as_register(),
                     op->tmp1()->as_register(),
                     op->tmp2()->as_register(),
                     op->tmp3()->as_register(),
                     op->header_size(),
                     op->object_size(),
                     op->klass()->as_register(),
                     *op->stub()->entry());

  __ bind(*op->stub()->continuation());
  __ verify_oop(op->obj()->as_register(), FILE_AND_LINE);
}


void LIR_Assembler::emit_alloc_array(LIR_OpAllocArray* op) {
  LP64_ONLY( __ extsw(op->len()->as_register(), op->len()->as_register()); )
  if (UseSlowPath ||
      (!UseFastNewObjectArray && (is_reference_type(op->type()))) ||
      (!UseFastNewTypeArray   && (!is_reference_type(op->type())))) {
    __ b(*op->stub()->entry());
  } else {
    __ allocate_array(op->obj()->as_register(),
                      op->len()->as_register(),
                      op->tmp1()->as_register(),
                      op->tmp2()->as_register(),
                      op->tmp3()->as_register(),
                      arrayOopDesc::header_size(op->type()),
                      type2aelembytes(op->type()),
                      op->klass()->as_register(),
                      *op->stub()->entry());
  }
  __ bind(*op->stub()->continuation());
}


void LIR_Assembler::type_profile_helper(Register mdo, int mdo_offset_bias,
                                        ciMethodData *md, ciProfileData *data,
                                        Register recv, Register tmp1, Label* update_done) {
  uint i;
  for (i = 0; i < VirtualCallData::row_limit(); i++) {
    Label next_test;
    // See if the receiver is receiver[n].
    __ ld(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)) - mdo_offset_bias, mdo);
    __ verify_klass_ptr(tmp1);
    __ cmpd(CCR0, recv, tmp1);
    __ bne(CCR0, next_test);

    __ ld(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
    __ addi(tmp1, tmp1, DataLayout::counter_increment);
    __ std(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
    __ b(*update_done);

    __ bind(next_test);
  }

  // Didn't find receiver; find next empty slot and fill it in.
  for (i = 0; i < VirtualCallData::row_limit(); i++) {
    Label next_test;
    __ ld(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)) - mdo_offset_bias, mdo);
    __ cmpdi(CCR0, tmp1, 0);
    __ bne(CCR0, next_test);
    __ li(tmp1, DataLayout::counter_increment);
    __ std(recv, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)) - mdo_offset_bias, mdo);
    __ std(tmp1, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
    __ b(*update_done);

    __ bind(next_test);
  }
}


void LIR_Assembler::setup_md_access(ciMethod* method, int bci,
                                    ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias) {
  md = method->method_data_or_null();
  assert(md != NULL, "Sanity");
  data = md->bci_to_data(bci);
  assert(data != NULL,       "need data for checkcast");
  assert(data->is_ReceiverTypeData(), "need ReceiverTypeData for type check");
  if (!Assembler::is_simm16(md->byte_offset_of_slot(data, DataLayout::header_offset()) + data->size_in_bytes())) {
    // The offset is large so bias the mdo by the base of the slot so
    // that the ld can use simm16s to reference the slots of the data.
    mdo_offset_bias = md->byte_offset_of_slot(data, DataLayout::header_offset());
  }
}


void LIR_Assembler::emit_typecheck_helper(LIR_OpTypeCheck *op, Label* success, Label* failure, Label* obj_is_null) {
  const Register obj = op->object()->as_register(); // Needs to live in this register at safepoint (patching stub).
  Register k_RInfo = op->tmp1()->as_register();
  Register klass_RInfo = op->tmp2()->as_register();
  Register Rtmp1 = op->tmp3()->as_register();
  Register dst = op->result_opr()->as_register();
  ciKlass* k = op->klass();
  bool should_profile = op->should_profile();
  // Attention: do_temp(opTypeCheck->_object) is not used, i.e. obj may be same as one of the temps.
  bool reg_conflict = false;
  if (obj == k_RInfo) {
    k_RInfo = dst;
    reg_conflict = true;
  } else if (obj == klass_RInfo) {
    klass_RInfo = dst;
    reg_conflict = true;
  } else if (obj == Rtmp1) {
    Rtmp1 = dst;
    reg_conflict = true;
  }
  assert_different_registers(obj, k_RInfo, klass_RInfo, Rtmp1);

  __ cmpdi(CCR0, obj, 0);

  ciMethodData* md = NULL;
  ciProfileData* data = NULL;
  int mdo_offset_bias = 0;
  if (should_profile) {
    ciMethod* method = op->profiled_method();
    assert(method != NULL, "Should have method");
    setup_md_access(method, op->profiled_bci(), md, data, mdo_offset_bias);

    Register mdo      = k_RInfo;
    Register data_val = Rtmp1;
    Label not_null;
    __ bne(CCR0, not_null);
    metadata2reg(md->constant_encoding(), mdo);
    __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
    __ lbz(data_val, md->byte_offset_of_slot(data, DataLayout::flags_offset()) - mdo_offset_bias, mdo);
    __ ori(data_val, data_val, BitData::null_seen_byte_constant());
    __ stb(data_val, md->byte_offset_of_slot(data, DataLayout::flags_offset()) - mdo_offset_bias, mdo);
    __ b(*obj_is_null);
    __ bind(not_null);
  } else {
    __ beq(CCR0, *obj_is_null);
  }

  // get object class
  __ load_klass(klass_RInfo, obj);

  if (k->is_loaded()) {
    metadata2reg(k->constant_encoding(), k_RInfo);
  } else {
    klass2reg_with_patching(k_RInfo, op->info_for_patch());
  }

  Label profile_cast_failure, failure_restore_obj, profile_cast_success;
  Label *failure_target = should_profile ? &profile_cast_failure : failure;
  Label *success_target = should_profile ? &profile_cast_success : success;

  if (op->fast_check()) {
    assert_different_registers(klass_RInfo, k_RInfo);
    __ cmpd(CCR0, k_RInfo, klass_RInfo);
    if (should_profile) {
      __ bne(CCR0, *failure_target);
      // Fall through to success case.
    } else {
      __ beq(CCR0, *success);
      // Fall through to failure case.
    }
  } else {
    bool need_slow_path = true;
    if (k->is_loaded()) {
      if ((int) k->super_check_offset() != in_bytes(Klass::secondary_super_cache_offset())) {
        need_slow_path = false;
      }
      // Perform the fast part of the checking logic.
      __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, R0, (need_slow_path ? success_target : NULL),
                                       failure_target, NULL, RegisterOrConstant(k->super_check_offset()));
    } else {
      // Perform the fast part of the checking logic.
      __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, R0, success_target, failure_target);
    }
    if (!need_slow_path) {
      if (!should_profile) { __ b(*success); }
    } else {
      // Call out-of-line instance of __ check_klass_subtype_slow_path(...):
      address entry = Runtime1::entry_for(Runtime1::slow_subtype_check_id);
      // Stub needs fixed registers (tmp1-3).
      Register original_k_RInfo = op->tmp1()->as_register();
      Register original_klass_RInfo = op->tmp2()->as_register();
      Register original_Rtmp1 = op->tmp3()->as_register();
      bool keep_obj_alive = reg_conflict && (op->code() == lir_checkcast);
      bool keep_klass_RInfo_alive = (obj == original_klass_RInfo) && should_profile;
      if (keep_obj_alive && (obj != original_Rtmp1)) { __ mr(R0, obj); }
      __ mr_if_needed(original_k_RInfo, k_RInfo);
      __ mr_if_needed(original_klass_RInfo, klass_RInfo);
      if (keep_obj_alive) { __ mr(dst, (obj == original_Rtmp1) ? obj : R0); }
      //__ load_const_optimized(original_Rtmp1, entry, R0);
      __ calculate_address_from_global_toc(original_Rtmp1, entry, true, true, false);
      __ mtctr(original_Rtmp1);
      __ bctrl(); // sets CR0
      if (keep_obj_alive) {
        if (keep_klass_RInfo_alive) { __ mr(R0, obj); }
        __ mr(obj, dst);
      }
      if (should_profile) {
        __ bne(CCR0, *failure_target);
        if (keep_klass_RInfo_alive) { __ mr(klass_RInfo, keep_obj_alive ? R0 : obj); }
        // Fall through to success case.
      } else {
        __ beq(CCR0, *success);
        // Fall through to failure case.
      }
    }
  }

  if (should_profile) {
    Register mdo = k_RInfo, recv = klass_RInfo;
    assert_different_registers(mdo, recv, Rtmp1);
    __ bind(profile_cast_success);
    metadata2reg(md->constant_encoding(), mdo);
    __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
    type_profile_helper(mdo, mdo_offset_bias, md, data, recv, Rtmp1, success);
    __ b(*success);

    // Cast failure case.
    __ bind(profile_cast_failure);
    metadata2reg(md->constant_encoding(), mdo);
    __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
    __ ld(Rtmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
    __ addi(Rtmp1, Rtmp1, -DataLayout::counter_increment);
    __ std(Rtmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
  }

  __ bind(*failure);
}


void LIR_Assembler::emit_opTypeCheck(LIR_OpTypeCheck* op) {
  LIR_Code code = op->code();
  if (code == lir_store_check) {
    Register value = op->object()->as_register();
    Register array = op->array()->as_register();
    Register k_RInfo = op->tmp1()->as_register();
    Register klass_RInfo = op->tmp2()->as_register();
    Register Rtmp1 = op->tmp3()->as_register();
    bool should_profile = op->should_profile();

    __ verify_oop(value, FILE_AND_LINE);
    CodeStub* stub = op->stub();
    // Check if it needs to be profiled.
    ciMethodData* md = NULL;
    ciProfileData* data = NULL;
    int mdo_offset_bias = 0;
    if (should_profile) {
      ciMethod* method = op->profiled_method();
      assert(method != NULL, "Should have method");
      setup_md_access(method, op->profiled_bci(), md, data, mdo_offset_bias);
    }
    Label profile_cast_success, failure, done;
    Label *success_target = should_profile ? &profile_cast_success : &done;

    __ cmpdi(CCR0, value, 0);
    if (should_profile) {
      Label not_null;
      __ bne(CCR0, not_null);
      Register mdo      = k_RInfo;
      Register data_val = Rtmp1;
      metadata2reg(md->constant_encoding(), mdo);
      __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
      __ lbz(data_val, md->byte_offset_of_slot(data, DataLayout::flags_offset()) - mdo_offset_bias, mdo);
      __ ori(data_val, data_val, BitData::null_seen_byte_constant());
      __ stb(data_val, md->byte_offset_of_slot(data, DataLayout::flags_offset()) - mdo_offset_bias, mdo);
      __ b(done);
      __ bind(not_null);
    } else {
      __ beq(CCR0, done);
    }
    if (!os::zero_page_read_protected() || !ImplicitNullChecks) {
      explicit_null_check(array, op->info_for_exception());
    } else {
      add_debug_info_for_null_check_here(op->info_for_exception());
    }
    __ load_klass(k_RInfo, array);
    __ load_klass(klass_RInfo, value);

    // Get instance klass.
    __ ld(k_RInfo, in_bytes(ObjArrayKlass::element_klass_offset()), k_RInfo);
    // Perform the fast part of the checking logic.
    __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, R0, success_target, &failure, NULL);

    // Call out-of-line instance of __ check_klass_subtype_slow_path(...):
    const address slow_path = Runtime1::entry_for(Runtime1::slow_subtype_check_id);
    //__ load_const_optimized(R0, slow_path);
    __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(slow_path));
    __ mtctr(R0);
    __ bctrl(); // sets CR0
    if (!should_profile) {
      __ beq(CCR0, done);
      __ bind(failure);
    } else {
      __ bne(CCR0, failure);
      // Fall through to the success case.

      Register mdo  = klass_RInfo, recv = k_RInfo, tmp1 = Rtmp1;
      assert_different_registers(value, mdo, recv, tmp1);
      __ bind(profile_cast_success);
      metadata2reg(md->constant_encoding(), mdo);
      __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
      __ load_klass(recv, value);
      type_profile_helper(mdo, mdo_offset_bias, md, data, recv, tmp1, &done);
      __ b(done);

      // Cast failure case.
      __ bind(failure);
      metadata2reg(md->constant_encoding(), mdo);
      __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
      Address data_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias);
      __ ld(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
      __ addi(tmp1, tmp1, -DataLayout::counter_increment);
      __ std(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
    }
    __ b(*stub->entry());
    __ bind(done);

  } else if (code == lir_checkcast) {
    Label success, failure;
    emit_typecheck_helper(op, &success, /*fallthru*/&failure, &success);
    __ b(*op->stub()->entry());
    __ align(32, 12);
    __ bind(success);
    __ mr_if_needed(op->result_opr()->as_register(), op->object()->as_register());
  } else if (code == lir_instanceof) {
    Register dst = op->result_opr()->as_register();
    Label success, failure, done;
    emit_typecheck_helper(op, &success, /*fallthru*/&failure, &failure);
    __ li(dst, 0);
    __ b(done);
    __ align(32, 12);
    __ bind(success);
    __ li(dst, 1);
    __ bind(done);
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::emit_compare_and_swap(LIR_OpCompareAndSwap* op) {
  Register addr = op->addr()->as_pointer_register();
  Register cmp_value = noreg, new_value = noreg;
  bool is_64bit = false;

  if (op->code() == lir_cas_long) {
    cmp_value = op->cmp_value()->as_register_lo();
    new_value = op->new_value()->as_register_lo();
    is_64bit = true;
  } else if (op->code() == lir_cas_int || op->code() == lir_cas_obj) {
    cmp_value = op->cmp_value()->as_register();
    new_value = op->new_value()->as_register();
    if (op->code() == lir_cas_obj) {
      if (UseCompressedOops) {
        Register t1 = op->tmp1()->as_register();
        Register t2 = op->tmp2()->as_register();
        cmp_value = __ encode_heap_oop(t1, cmp_value);
        new_value = __ encode_heap_oop(t2, new_value);
      } else {
        is_64bit = true;
      }
    }
  } else {
    Unimplemented();
  }

  if (is_64bit) {
    __ cmpxchgd(BOOL_RESULT, /*current_value=*/R0, cmp_value, new_value, addr,
                MacroAssembler::MemBarNone,
                MacroAssembler::cmpxchgx_hint_atomic_update(),
                noreg, NULL, /*check without ldarx first*/true);
  } else {
    __ cmpxchgw(BOOL_RESULT, /*current_value=*/R0, cmp_value, new_value, addr,
                MacroAssembler::MemBarNone,
                MacroAssembler::cmpxchgx_hint_atomic_update(),
                noreg, /*check without ldarx first*/true);
  }

  if (support_IRIW_for_not_multiple_copy_atomic_cpu) {
    __ isync();
  } else {
    __ sync();
  }
}

void LIR_Assembler::breakpoint() {
  __ illtrap();
}


void LIR_Assembler::push(LIR_Opr opr) {
  Unimplemented();
}

void LIR_Assembler::pop(LIR_Opr opr) {
  Unimplemented();
}


void LIR_Assembler::monitor_address(int monitor_no, LIR_Opr dst_opr) {
  Address mon_addr = frame_map()->address_for_monitor_lock(monitor_no);
  Register dst = dst_opr->as_register();
  Register reg = mon_addr.base();
  int offset = mon_addr.disp();
  // Compute pointer to BasicLock.
  __ add_const_optimized(dst, reg, offset);
}


void LIR_Assembler::emit_lock(LIR_OpLock* op) {
  Register obj = op->obj_opr()->as_register();
  Register hdr = op->hdr_opr()->as_register();
  Register lock = op->lock_opr()->as_register();

  // Obj may not be an oop.
  if (op->code() == lir_lock) {
    MonitorEnterStub* stub = (MonitorEnterStub*)op->stub();
    if (UseFastLocking) {
      assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
      // Add debug info for NullPointerException only if one is possible.
      if (op->info() != NULL) {
        if (!os::zero_page_read_protected() || !ImplicitNullChecks) {
          explicit_null_check(obj, op->info());
        } else {
          add_debug_info_for_null_check_here(op->info());
        }
      }
      __ lock_object(hdr, obj, lock, op->scratch_opr()->as_register(), *op->stub()->entry());
    } else {
      // always do slow locking
      // note: The slow locking code could be inlined here, however if we use
      //       slow locking, speed doesn't matter anyway and this solution is
      //       simpler and requires less duplicated code - additionally, the
      //       slow locking code is the same in either case which simplifies
      //       debugging.
      __ b(*op->stub()->entry());
    }
  } else {
    assert (op->code() == lir_unlock, "Invalid code, expected lir_unlock");
    if (UseFastLocking) {
      assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
      __ unlock_object(hdr, obj, lock, *op->stub()->entry());
    } else {
      // always do slow unlocking
      // note: The slow unlocking code could be inlined here, however if we use
      //       slow unlocking, speed doesn't matter anyway and this solution is
      //       simpler and requires less duplicated code - additionally, the
      //       slow unlocking code is the same in either case which simplifies
      //       debugging.
      __ b(*op->stub()->entry());
    }
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
  Register mdo = op->mdo()->as_register();
#ifdef _LP64
  assert(op->tmp1()->is_double_cpu(), "tmp1 must be allocated");
  Register tmp1 = op->tmp1()->as_register_lo();
#else
  assert(op->tmp1()->is_single_cpu(), "tmp1 must be allocated");
  Register tmp1 = op->tmp1()->as_register();
#endif
  metadata2reg(md->constant_encoding(), mdo);
  int mdo_offset_bias = 0;
  if (!Assembler::is_simm16(md->byte_offset_of_slot(data, CounterData::count_offset()) +
                            data->size_in_bytes())) {
    // The offset is large so bias the mdo by the base of the slot so
    // that the ld can use simm16s to reference the slots of the data.
    mdo_offset_bias = md->byte_offset_of_slot(data, CounterData::count_offset());
    __ add_const_optimized(mdo, mdo, mdo_offset_bias, R0);
  }

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
          __ ld(tmp1, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
          __ addi(tmp1, tmp1, DataLayout::counter_increment);
          __ std(tmp1, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
          return;
        }
      }

      // Receiver type not found in profile data; select an empty slot.

      // Note that this is less efficient than it should be because it
      // always does a write to the receiver part of the
      // VirtualCallData rather than just the first time.
      for (i = 0; i < VirtualCallData::row_limit(); i++) {
        ciKlass* receiver = vc_data->receiver(i);
        if (receiver == NULL) {
          metadata2reg(known_klass->constant_encoding(), tmp1);
          __ std(tmp1, md->byte_offset_of_slot(data, VirtualCallData::receiver_offset(i)) - mdo_offset_bias, mdo);

          __ ld(tmp1, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
          __ addi(tmp1, tmp1, DataLayout::counter_increment);
          __ std(tmp1, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)) - mdo_offset_bias, mdo);
          return;
        }
      }
    } else {
      __ load_klass(recv, recv);
      Label update_done;
      type_profile_helper(mdo, mdo_offset_bias, md, data, recv, tmp1, &update_done);
      // Receiver did not match any saved receiver and there is no empty row for it.
      // Increment total counter to indicate polymorphic case.
      __ ld(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
      __ addi(tmp1, tmp1, DataLayout::counter_increment);
      __ std(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);

      __ bind(update_done);
    }
  } else {
    // Static call
    __ ld(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
    __ addi(tmp1, tmp1, DataLayout::counter_increment);
    __ std(tmp1, md->byte_offset_of_slot(data, CounterData::count_offset()) - mdo_offset_bias, mdo);
  }
}


void LIR_Assembler::align_backward_branch_target() {
  __ align(32, 12); // Insert up to 3 nops to align with 32 byte boundary.
}


void LIR_Assembler::emit_delay(LIR_OpDelay* op) {
  Unimplemented();
}


void LIR_Assembler::negate(LIR_Opr left, LIR_Opr dest, LIR_Opr tmp) {
  // tmp must be unused
  assert(tmp->is_illegal(), "wasting a register if tmp is allocated");
  assert(left->is_register(), "can only handle registers");

  if (left->is_single_cpu()) {
    __ neg(dest->as_register(), left->as_register());
  } else if (left->is_single_fpu()) {
    __ fneg(dest->as_float_reg(), left->as_float_reg());
  } else if (left->is_double_fpu()) {
    __ fneg(dest->as_double_reg(), left->as_double_reg());
  } else {
    assert (left->is_double_cpu(), "Must be a long");
    __ neg(dest->as_register_lo(), left->as_register_lo());
  }
}


void LIR_Assembler::rt_call(LIR_Opr result, address dest,
                            const LIR_OprList* args, LIR_Opr tmp, CodeEmitInfo* info) {
  // Stubs: Called via rt_call, but dest is a stub address (no function descriptor).
  if (dest == Runtime1::entry_for(Runtime1::register_finalizer_id) ||
      dest == Runtime1::entry_for(Runtime1::new_multi_array_id   )) {
    //__ load_const_optimized(R0, dest);
    __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(dest));
    __ mtctr(R0);
    __ bctrl();
    assert(info != NULL, "sanity");
    add_call_info_here(info);
    return;
  }

  __ call_c_with_frame_resize(dest, /*no resizing*/ 0);
  if (info != NULL) {
    add_call_info_here(info);
  }
}


void LIR_Assembler::volatile_move_op(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info) {
  ShouldNotReachHere(); // Not needed on _LP64.
}

void LIR_Assembler::membar() {
  __ fence();
}

void LIR_Assembler::membar_acquire() {
  __ acquire();
}

void LIR_Assembler::membar_release() {
  __ release();
}

void LIR_Assembler::membar_loadload() {
  __ membar(Assembler::LoadLoad);
}

void LIR_Assembler::membar_storestore() {
  __ membar(Assembler::StoreStore);
}

void LIR_Assembler::membar_loadstore() {
  __ membar(Assembler::LoadStore);
}

void LIR_Assembler::membar_storeload() {
  __ membar(Assembler::StoreLoad);
}

void LIR_Assembler::on_spin_wait() {
  Unimplemented();
}

void LIR_Assembler::leal(LIR_Opr addr_opr, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  LIR_Address* addr = addr_opr->as_address_ptr();
  assert(addr->scale() == LIR_Address::times_1, "no scaling on this platform");

  if (addr->index()->is_illegal()) {
    if (patch_code != lir_patch_none) {
      PatchingStub* patch = new PatchingStub(_masm, PatchingStub::access_field_id);
      __ load_const32(R0, 0); // patchable int
      __ add(dest->as_pointer_register(), addr->base()->as_pointer_register(), R0);
      patching_epilog(patch, patch_code, addr->base()->as_register(), info);
    } else {
      __ add_const_optimized(dest->as_pointer_register(), addr->base()->as_pointer_register(), addr->disp());
    }
  } else {
    assert(patch_code == lir_patch_none, "Patch code not supported");
    assert(addr->disp() == 0, "can't have both: index and disp");
    __ add(dest->as_pointer_register(), addr->index()->as_pointer_register(), addr->base()->as_pointer_register());
  }
}


void LIR_Assembler::get_thread(LIR_Opr result_reg) {
  ShouldNotReachHere();
}


#ifdef ASSERT
// Emit run-time assertion.
void LIR_Assembler::emit_assert(LIR_OpAssert* op) {
  Unimplemented();
}
#endif


void LIR_Assembler::peephole(LIR_List* lir) {
  // Optimize instruction pairs before emitting.
  LIR_OpList* inst = lir->instructions_list();
  for (int i = 1; i < inst->length(); i++) {
    LIR_Op* op = inst->at(i);

    // 2 register-register-moves
    if (op->code() == lir_move) {
      LIR_Opr in2  = ((LIR_Op1*)op)->in_opr(),
              res2 = ((LIR_Op1*)op)->result_opr();
      if (in2->is_register() && res2->is_register()) {
        LIR_Op* prev = inst->at(i - 1);
        if (prev && prev->code() == lir_move) {
          LIR_Opr in1  = ((LIR_Op1*)prev)->in_opr(),
                  res1 = ((LIR_Op1*)prev)->result_opr();
          if (in1->is_same_register(res2) && in2->is_same_register(res1)) {
            inst->remove_at(i);
          }
        }
      }
    }

  }
  return;
}


void LIR_Assembler::atomic_op(LIR_Code code, LIR_Opr src, LIR_Opr data, LIR_Opr dest, LIR_Opr tmp) {
  const LIR_Address *addr = src->as_address_ptr();
  assert(addr->disp() == 0 && addr->index()->is_illegal(), "use leal!");
  const Register Rptr = addr->base()->as_pointer_register(),
                 Rtmp = tmp->as_register();
  Register Rco = noreg;
  if (UseCompressedOops && data->is_oop()) {
    Rco = __ encode_heap_oop(Rtmp, data->as_register());
  }

  Label Lretry;
  __ bind(Lretry);

  if (data->type() == T_INT) {
    const Register Rold = dest->as_register(),
                   Rsrc = data->as_register();
    assert_different_registers(Rptr, Rtmp, Rold, Rsrc);
    __ lwarx(Rold, Rptr, MacroAssembler::cmpxchgx_hint_atomic_update());
    if (code == lir_xadd) {
      __ add(Rtmp, Rsrc, Rold);
      __ stwcx_(Rtmp, Rptr);
    } else {
      __ stwcx_(Rsrc, Rptr);
    }
  } else if (data->is_oop()) {
    assert(code == lir_xchg, "xadd for oops");
    const Register Rold = dest->as_register();
    if (UseCompressedOops) {
      assert_different_registers(Rptr, Rold, Rco);
      __ lwarx(Rold, Rptr, MacroAssembler::cmpxchgx_hint_atomic_update());
      __ stwcx_(Rco, Rptr);
    } else {
      const Register Robj = data->as_register();
      assert_different_registers(Rptr, Rold, Robj);
      __ ldarx(Rold, Rptr, MacroAssembler::cmpxchgx_hint_atomic_update());
      __ stdcx_(Robj, Rptr);
    }
  } else if (data->type() == T_LONG) {
    const Register Rold = dest->as_register_lo(),
                   Rsrc = data->as_register_lo();
    assert_different_registers(Rptr, Rtmp, Rold, Rsrc);
    __ ldarx(Rold, Rptr, MacroAssembler::cmpxchgx_hint_atomic_update());
    if (code == lir_xadd) {
      __ add(Rtmp, Rsrc, Rold);
      __ stdcx_(Rtmp, Rptr);
    } else {
      __ stdcx_(Rsrc, Rptr);
    }
  } else {
    ShouldNotReachHere();
  }

  if (UseStaticBranchPredictionInCompareAndSwapPPC64) {
    __ bne_predict_not_taken(CCR0, Lretry);
  } else {
    __ bne(                  CCR0, Lretry);
  }

  if (UseCompressedOops && data->is_oop()) {
    __ decode_heap_oop(dest->as_register());
  }
}


void LIR_Assembler::emit_profile_type(LIR_OpProfileType* op) {
  Register obj = op->obj()->as_register();
  Register tmp = op->tmp()->as_pointer_register();
  LIR_Address* mdo_addr = op->mdp()->as_address_ptr();
  ciKlass* exact_klass = op->exact_klass();
  intptr_t current_klass = op->current_klass();
  bool not_null = op->not_null();
  bool no_conflict = op->no_conflict();

  Label Lupdate, Ldo_update, Ldone;

  bool do_null = !not_null;
  bool exact_klass_set = exact_klass != NULL && ciTypeEntries::valid_ciklass(current_klass) == exact_klass;
  bool do_update = !TypeEntries::is_type_unknown(current_klass) && !exact_klass_set;

  assert(do_null || do_update, "why are we here?");
  assert(!TypeEntries::was_null_seen(current_klass) || do_update, "why are we here?");

  __ verify_oop(obj, FILE_AND_LINE);

  if (do_null) {
    if (!TypeEntries::was_null_seen(current_klass)) {
      __ cmpdi(CCR0, obj, 0);
      __ bne(CCR0, Lupdate);
      __ ld(R0, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());
      __ ori(R0, R0, TypeEntries::null_seen);
      if (do_update) {
        __ b(Ldo_update);
      } else {
        __ std(R0, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());
      }
    } else {
      if (do_update) {
        __ cmpdi(CCR0, obj, 0);
        __ beq(CCR0, Ldone);
      }
    }
#ifdef ASSERT
  } else {
    __ cmpdi(CCR0, obj, 0);
    __ bne(CCR0, Lupdate);
    __ stop("unexpect null obj");
#endif
  }

  __ bind(Lupdate);
  if (do_update) {
    Label Lnext;
    const Register klass = R29_TOC; // kill and reload
    bool klass_reg_used = false;
#ifdef ASSERT
    if (exact_klass != NULL) {
      Label ok;
      klass_reg_used = true;
      __ load_klass(klass, obj);
      metadata2reg(exact_klass->constant_encoding(), R0);
      __ cmpd(CCR0, klass, R0);
      __ beq(CCR0, ok);
      __ stop("exact klass and actual klass differ");
      __ bind(ok);
    }
#endif

    if (!no_conflict) {
      if (exact_klass == NULL || TypeEntries::is_type_none(current_klass)) {
        klass_reg_used = true;
        if (exact_klass != NULL) {
          __ ld(tmp, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());
          metadata2reg(exact_klass->constant_encoding(), klass);
        } else {
          __ load_klass(klass, obj);
          __ ld(tmp, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register()); // may kill obj
        }

        // Like InterpreterMacroAssembler::profile_obj_type
        __ clrrdi(R0, tmp, exact_log2(-TypeEntries::type_klass_mask));
        // Basically same as andi(R0, tmp, TypeEntries::type_klass_mask);
        __ cmpd(CCR1, R0, klass);
        // Klass seen before, nothing to do (regardless of unknown bit).
        //beq(CCR1, do_nothing);

        __ andi_(R0, klass, TypeEntries::type_unknown);
        // Already unknown. Nothing to do anymore.
        //bne(CCR0, do_nothing);
        __ crorc(CCR0, Assembler::equal, CCR1, Assembler::equal); // cr0 eq = cr1 eq or cr0 ne
        __ beq(CCR0, Lnext);

        if (TypeEntries::is_type_none(current_klass)) {
          __ clrrdi_(R0, tmp, exact_log2(-TypeEntries::type_mask));
          __ orr(R0, klass, tmp); // Combine klass and null_seen bit (only used if (tmp & type_mask)==0).
          __ beq(CCR0, Ldo_update); // First time here. Set profile type.
        }

      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "conflict only");

        __ ld(tmp, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());
        __ andi_(R0, tmp, TypeEntries::type_unknown);
        // Already unknown. Nothing to do anymore.
        __ bne(CCR0, Lnext);
      }

      // Different than before. Cannot keep accurate profile.
      __ ori(R0, tmp, TypeEntries::type_unknown);
    } else {
      // There's a single possible klass at this profile point
      assert(exact_klass != NULL, "should be");
      __ ld(tmp, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());

      if (TypeEntries::is_type_none(current_klass)) {
        klass_reg_used = true;
        metadata2reg(exact_klass->constant_encoding(), klass);

        __ clrrdi(R0, tmp, exact_log2(-TypeEntries::type_klass_mask));
        // Basically same as andi(R0, tmp, TypeEntries::type_klass_mask);
        __ cmpd(CCR1, R0, klass);
        // Klass seen before, nothing to do (regardless of unknown bit).
        __ beq(CCR1, Lnext);
#ifdef ASSERT
        {
          Label ok;
          __ clrrdi_(R0, tmp, exact_log2(-TypeEntries::type_mask));
          __ beq(CCR0, ok); // First time here.

          __ stop("unexpected profiling mismatch");
          __ bind(ok);
        }
#endif
        // First time here. Set profile type.
        __ orr(R0, klass, tmp); // Combine klass and null_seen bit (only used if (tmp & type_mask)==0).
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "inconsistent");

        // Already unknown. Nothing to do anymore.
        __ andi_(R0, tmp, TypeEntries::type_unknown);
        __ bne(CCR0, Lnext);

        // Different than before. Cannot keep accurate profile.
        __ ori(R0, tmp, TypeEntries::type_unknown);
      }
    }

    __ bind(Ldo_update);
    __ std(R0, index_or_disp(mdo_addr), mdo_addr->base()->as_pointer_register());

    __ bind(Lnext);
    if (klass_reg_used) { __ load_const_optimized(R29_TOC, MacroAssembler::global_toc(), R0); } // reinit
  }
  __ bind(Ldone);
}


void LIR_Assembler::emit_updatecrc32(LIR_OpUpdateCRC32* op) {
  assert(op->crc()->is_single_cpu(), "crc must be register");
  assert(op->val()->is_single_cpu(), "byte value must be register");
  assert(op->result_opr()->is_single_cpu(), "result must be register");
  Register crc = op->crc()->as_register();
  Register val = op->val()->as_register();
  Register res = op->result_opr()->as_register();

  assert_different_registers(val, crc, res);

  __ load_const_optimized(res, StubRoutines::crc_table_addr(), R0);
  __ kernel_crc32_singleByteReg(crc, val, res, true);
  __ mr(res, crc);
}

#undef __
