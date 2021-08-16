/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArrayKlass.hpp"
#include "ci/ciInstance.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gc_globals.hpp"
#include "nativeInst_x86.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_x86.inline.hpp"


// These masks are used to provide 128-bit aligned bitmasks to the XMM
// instructions, to allow sign-masking or sign-bit flipping.  They allow
// fast versions of NegF/NegD and AbsF/AbsD.

// Note: 'double' and 'long long' have 32-bits alignment on x86.
static jlong* double_quadword(jlong *adr, jlong lo, jlong hi) {
  // Use the expression (adr)&(~0xF) to provide 128-bits aligned address
  // of 128-bits operands for SSE instructions.
  jlong *operand = (jlong*)(((intptr_t)adr) & ((intptr_t)(~0xF)));
  // Store the value to a 128-bits operand.
  operand[0] = lo;
  operand[1] = hi;
  return operand;
}

// Buffer for 128-bits masks used by SSE instructions.
static jlong fp_signmask_pool[(4+1)*2]; // 4*128bits(data) + 128bits(alignment)

// Static initialization during VM startup.
static jlong *float_signmask_pool  = double_quadword(&fp_signmask_pool[1*2],         CONST64(0x7FFFFFFF7FFFFFFF),         CONST64(0x7FFFFFFF7FFFFFFF));
static jlong *double_signmask_pool = double_quadword(&fp_signmask_pool[2*2],         CONST64(0x7FFFFFFFFFFFFFFF),         CONST64(0x7FFFFFFFFFFFFFFF));
static jlong *float_signflip_pool  = double_quadword(&fp_signmask_pool[3*2], (jlong)UCONST64(0x8000000080000000), (jlong)UCONST64(0x8000000080000000));
static jlong *double_signflip_pool = double_quadword(&fp_signmask_pool[4*2], (jlong)UCONST64(0x8000000000000000), (jlong)UCONST64(0x8000000000000000));


NEEDS_CLEANUP // remove this definitions ?
const Register IC_Klass    = rax;   // where the IC klass is cached
const Register SYNC_header = rax;   // synchronization header
const Register SHIFT_count = rcx;   // where count for shift operations must be

#define __ _masm->


static void select_different_registers(Register preserve,
                                       Register extra,
                                       Register &tmp1,
                                       Register &tmp2) {
  if (tmp1 == preserve) {
    assert_different_registers(tmp1, tmp2, extra);
    tmp1 = extra;
  } else if (tmp2 == preserve) {
    assert_different_registers(tmp1, tmp2, extra);
    tmp2 = extra;
  }
  assert_different_registers(preserve, tmp1, tmp2);
}



static void select_different_registers(Register preserve,
                                       Register extra,
                                       Register &tmp1,
                                       Register &tmp2,
                                       Register &tmp3) {
  if (tmp1 == preserve) {
    assert_different_registers(tmp1, tmp2, tmp3, extra);
    tmp1 = extra;
  } else if (tmp2 == preserve) {
    assert_different_registers(tmp1, tmp2, tmp3, extra);
    tmp2 = extra;
  } else if (tmp3 == preserve) {
    assert_different_registers(tmp1, tmp2, tmp3, extra);
    tmp3 = extra;
  }
  assert_different_registers(preserve, tmp1, tmp2, tmp3);
}



bool LIR_Assembler::is_small_constant(LIR_Opr opr) {
  if (opr->is_constant()) {
    LIR_Const* constant = opr->as_constant_ptr();
    switch (constant->type()) {
      case T_INT: {
        return true;
      }

      default:
        return false;
    }
  }
  return false;
}


LIR_Opr LIR_Assembler::receiverOpr() {
  return FrameMap::receiver_opr;
}

LIR_Opr LIR_Assembler::osrBufferPointer() {
  return FrameMap::as_pointer_opr(receiverOpr()->as_register());
}

//--------------fpu register translations-----------------------


address LIR_Assembler::float_constant(float f) {
  address const_addr = __ float_constant(f);
  if (const_addr == NULL) {
    bailout("const section overflow");
    return __ code()->consts()->start();
  } else {
    return const_addr;
  }
}


address LIR_Assembler::double_constant(double d) {
  address const_addr = __ double_constant(d);
  if (const_addr == NULL) {
    bailout("const section overflow");
    return __ code()->consts()->start();
  } else {
    return const_addr;
  }
}

#ifndef _LP64
void LIR_Assembler::fpop() {
  __ fpop();
}

void LIR_Assembler::fxch(int i) {
  __ fxch(i);
}

void LIR_Assembler::fld(int i) {
  __ fld_s(i);
}

void LIR_Assembler::ffree(int i) {
  __ ffree(i);
}
#endif // !_LP64

void LIR_Assembler::breakpoint() {
  __ int3();
}

void LIR_Assembler::push(LIR_Opr opr) {
  if (opr->is_single_cpu()) {
    __ push_reg(opr->as_register());
  } else if (opr->is_double_cpu()) {
    NOT_LP64(__ push_reg(opr->as_register_hi()));
    __ push_reg(opr->as_register_lo());
  } else if (opr->is_stack()) {
    __ push_addr(frame_map()->address_for_slot(opr->single_stack_ix()));
  } else if (opr->is_constant()) {
    LIR_Const* const_opr = opr->as_constant_ptr();
    if (const_opr->type() == T_OBJECT) {
      __ push_oop(const_opr->as_jobject());
    } else if (const_opr->type() == T_INT) {
      __ push_jint(const_opr->as_jint());
    } else {
      ShouldNotReachHere();
    }

  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::pop(LIR_Opr opr) {
  if (opr->is_single_cpu()) {
    __ pop_reg(opr->as_register());
  } else {
    ShouldNotReachHere();
  }
}

bool LIR_Assembler::is_literal_address(LIR_Address* addr) {
  return addr->base()->is_illegal() && addr->index()->is_illegal();
}

//-------------------------------------------

Address LIR_Assembler::as_Address(LIR_Address* addr) {
  return as_Address(addr, rscratch1);
}

Address LIR_Assembler::as_Address(LIR_Address* addr, Register tmp) {
  if (addr->base()->is_illegal()) {
    assert(addr->index()->is_illegal(), "must be illegal too");
    AddressLiteral laddr((address)addr->disp(), relocInfo::none);
    if (! __ reachable(laddr)) {
      __ movptr(tmp, laddr.addr());
      Address res(tmp, 0);
      return res;
    } else {
      return __ as_Address(laddr);
    }
  }

  Register base = addr->base()->as_pointer_register();

  if (addr->index()->is_illegal()) {
    return Address( base, addr->disp());
  } else if (addr->index()->is_cpu_register()) {
    Register index = addr->index()->as_pointer_register();
    return Address(base, index, (Address::ScaleFactor) addr->scale(), addr->disp());
  } else if (addr->index()->is_constant()) {
    intptr_t addr_offset = (addr->index()->as_constant_ptr()->as_jint() << addr->scale()) + addr->disp();
    assert(Assembler::is_simm32(addr_offset), "must be");

    return Address(base, addr_offset);
  } else {
    Unimplemented();
    return Address();
  }
}


Address LIR_Assembler::as_Address_hi(LIR_Address* addr) {
  Address base = as_Address(addr);
  return Address(base._base, base._index, base._scale, base._disp + BytesPerWord);
}


Address LIR_Assembler::as_Address_lo(LIR_Address* addr) {
  return as_Address(addr);
}


void LIR_Assembler::osr_entry() {
  offsets()->set_value(CodeOffsets::OSR_Entry, code_offset());
  BlockBegin* osr_entry = compilation()->hir()->osr_entry();
  ValueStack* entry_state = osr_entry->state();
  int number_of_locks = entry_state->locks_size();

  // we jump here if osr happens with the interpreter
  // state set up to continue at the beginning of the
  // loop that triggered osr - in particular, we have
  // the following registers setup:
  //
  // rcx: osr buffer
  //

  // build frame
  ciMethod* m = compilation()->method();
  __ build_frame(initial_frame_size_in_bytes(), bang_size_in_bytes());

  // OSR buffer is
  //
  // locals[nlocals-1..0]
  // monitors[0..number_of_locks]
  //
  // locals is a direct copy of the interpreter frame so in the osr buffer
  // so first slot in the local array is the last local from the interpreter
  // and last slot is local[0] (receiver) from the interpreter
  //
  // Similarly with locks. The first lock slot in the osr buffer is the nth lock
  // from the interpreter frame, the nth lock slot in the osr buffer is 0th lock
  // in the interpreter frame (the method lock if a sync method)

  // Initialize monitors in the compiled activation.
  //   rcx: pointer to osr buffer
  //
  // All other registers are dead at this point and the locals will be
  // copied into place by code emitted in the IR.

  Register OSR_buf = osrBufferPointer()->as_pointer_register();
  { assert(frame::interpreter_frame_monitor_size() == BasicObjectLock::size(), "adjust code below");
    int monitor_offset = BytesPerWord * method()->max_locals() +
      (BasicObjectLock::size() * BytesPerWord) * (number_of_locks - 1);
    // SharedRuntime::OSR_migration_begin() packs BasicObjectLocks in
    // the OSR buffer using 2 word entries: first the lock and then
    // the oop.
    for (int i = 0; i < number_of_locks; i++) {
      int slot_offset = monitor_offset - ((i * 2) * BytesPerWord);
#ifdef ASSERT
      // verify the interpreter's monitor has a non-null object
      {
        Label L;
        __ cmpptr(Address(OSR_buf, slot_offset + 1*BytesPerWord), (int32_t)NULL_WORD);
        __ jcc(Assembler::notZero, L);
        __ stop("locked object is NULL");
        __ bind(L);
      }
#endif
      __ movptr(rbx, Address(OSR_buf, slot_offset + 0));
      __ movptr(frame_map()->address_for_monitor_lock(i), rbx);
      __ movptr(rbx, Address(OSR_buf, slot_offset + 1*BytesPerWord));
      __ movptr(frame_map()->address_for_monitor_object(i), rbx);
    }
  }
}


// inline cache check; done before the frame is built.
int LIR_Assembler::check_icache() {
  Register receiver = FrameMap::receiver_opr->as_register();
  Register ic_klass = IC_Klass;
  const int ic_cmp_size = LP64_ONLY(10) NOT_LP64(9);
  const bool do_post_padding = VerifyOops || UseCompressedClassPointers;
  if (!do_post_padding) {
    // insert some nops so that the verified entry point is aligned on CodeEntryAlignment
    __ align(CodeEntryAlignment, __ offset() + ic_cmp_size);
  }
  int offset = __ offset();
  __ inline_cache_check(receiver, IC_Klass);
  assert(__ offset() % CodeEntryAlignment == 0 || do_post_padding, "alignment must be correct");
  if (do_post_padding) {
    // force alignment after the cache check.
    // It's been verified to be aligned if !VerifyOops
    __ align(CodeEntryAlignment);
  }
  return offset;
}

void LIR_Assembler::clinit_barrier(ciMethod* method) {
  assert(VM_Version::supports_fast_class_init_checks(), "sanity");
  assert(!method->holder()->is_not_initialized(), "initialization should have been started");

  Label L_skip_barrier;
  Register klass = rscratch1;
  Register thread = LP64_ONLY( r15_thread ) NOT_LP64( noreg );
  assert(thread != noreg, "x86_32 not implemented");

  __ mov_metadata(klass, method->holder()->constant_encoding());
  __ clinit_barrier(klass, thread, &L_skip_barrier /*L_fast_path*/);

  __ jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));

  __ bind(L_skip_barrier);
}

void LIR_Assembler::jobject2reg_with_patching(Register reg, CodeEmitInfo* info) {
  jobject o = NULL;
  PatchingStub* patch = new PatchingStub(_masm, patching_id(info));
  __ movoop(reg, o);
  patching_epilog(patch, lir_patch_normal, reg, info);
}

void LIR_Assembler::klass2reg_with_patching(Register reg, CodeEmitInfo* info) {
  Metadata* o = NULL;
  PatchingStub* patch = new PatchingStub(_masm, PatchingStub::load_klass_id);
  __ mov_metadata(reg, o);
  patching_epilog(patch, lir_patch_normal, reg, info);
}

// This specifies the rsp decrement needed to build the frame
int LIR_Assembler::initial_frame_size_in_bytes() const {
  // if rounding, must let FrameMap know!

  // The frame_map records size in slots (32bit word)

  // subtract two words to account for return address and link
  return (frame_map()->framesize() - (2*VMRegImpl::slots_per_word))  * VMRegImpl::stack_slot_size;
}


int LIR_Assembler::emit_exception_handler() {
  // if the last instruction is a call (typically to do a throw which
  // is coming at the end after block reordering) the return address
  // must still point into the code area in order to avoid assertion
  // failures when searching for the corresponding bci => add a nop
  // (was bug 5/14/1999 - gri)
  __ nop();

  // generate code for exception handler
  address handler_base = __ start_a_stub(exception_handler_size());
  if (handler_base == NULL) {
    // not enough space left for the handler
    bailout("exception handler overflow");
    return -1;
  }

  int offset = code_offset();

  // the exception oop and pc are in rax, and rdx
  // no other registers need to be preserved, so invalidate them
  __ invalidate_registers(false, true, true, false, true, true);

  // check that there is really an exception
  __ verify_not_null_oop(rax);

  // search an exception handler (rax: exception oop, rdx: throwing pc)
  __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::handle_exception_from_callee_id)));
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

  // Fetch the exception from TLS and clear out exception related thread state
  Register thread = NOT_LP64(rsi) LP64_ONLY(r15_thread);
  NOT_LP64(__ get_thread(rsi));
  __ movptr(rax, Address(thread, JavaThread::exception_oop_offset()));
  __ movptr(Address(thread, JavaThread::exception_oop_offset()), (intptr_t)NULL_WORD);
  __ movptr(Address(thread, JavaThread::exception_pc_offset()), (intptr_t)NULL_WORD);

  __ bind(_unwind_handler_entry);
  __ verify_not_null_oop(rax);
  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ mov(rbx, rax);  // Preserve the exception (rbx is always callee-saved)
  }

  // Preform needed unlocking
  MonitorExitStub* stub = NULL;
  if (method()->is_synchronized()) {
    monitor_address(0, FrameMap::rax_opr);
    stub = new MonitorExitStub(FrameMap::rax_opr, true, 0);
    __ unlock_object(rdi, rsi, rax, *stub->entry());
    __ bind(*stub->continuation());
  }

  if (compilation()->env()->dtrace_method_probes()) {
#ifdef _LP64
    __ mov(rdi, r15_thread);
    __ mov_metadata(rsi, method()->constant_encoding());
#else
    __ get_thread(rax);
    __ movptr(Address(rsp, 0), rax);
    __ mov_metadata(Address(rsp, sizeof(void*)), method()->constant_encoding());
#endif
    __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit)));
  }

  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ mov(rax, rbx);  // Restore the exception
  }

  // remove the activation and dispatch to the unwind handler
  __ remove_frame(initial_frame_size_in_bytes());
  __ jump(RuntimeAddress(Runtime1::entry_for(Runtime1::unwind_exception_id)));

  // Emit the slow path assembly
  if (stub != NULL) {
    stub->emit_code(this);
  }

  return offset;
}


int LIR_Assembler::emit_deopt_handler() {
  // if the last instruction is a call (typically to do a throw which
  // is coming at the end after block reordering) the return address
  // must still point into the code area in order to avoid assertion
  // failures when searching for the corresponding bci => add a nop
  // (was bug 5/14/1999 - gri)
  __ nop();

  // generate code for exception handler
  address handler_base = __ start_a_stub(deopt_handler_size());
  if (handler_base == NULL) {
    // not enough space left for the handler
    bailout("deopt handler overflow");
    return -1;
  }

  int offset = code_offset();
  InternalAddress here(__ pc());

  __ pushptr(here.addr());
  __ jump(RuntimeAddress(SharedRuntime::deopt_blob()->unpack()));
  guarantee(code_offset() - offset <= deopt_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}

void LIR_Assembler::return_op(LIR_Opr result, C1SafepointPollStub* code_stub) {
  assert(result->is_illegal() || !result->is_single_cpu() || result->as_register() == rax, "word returns are in rax,");
  if (!result->is_illegal() && result->is_float_kind() && !result->is_xmm_register()) {
    assert(result->fpu() == 0, "result must already be on TOS");
  }

  // Pop the stack before the safepoint code
  __ remove_frame(initial_frame_size_in_bytes());

  if (StackReservedPages > 0 && compilation()->has_reserved_stack_access()) {
    __ reserved_stack_check();
  }

  // Note: we do not need to round double result; float result has the right precision
  // the poll sets the condition code, but no data registers

#ifdef _LP64
  const Register thread = r15_thread;
#else
  const Register thread = rbx;
  __ get_thread(thread);
#endif
  code_stub->set_safepoint_offset(__ offset());
  __ relocate(relocInfo::poll_return_type);
  __ safepoint_poll(*code_stub->entry(), thread, true /* at_return */, true /* in_nmethod */);
  __ ret(0);
}


int LIR_Assembler::safepoint_poll(LIR_Opr tmp, CodeEmitInfo* info) {
  guarantee(info != NULL, "Shouldn't be NULL");
  int offset = __ offset();
#ifdef _LP64
  const Register poll_addr = rscratch1;
  __ movptr(poll_addr, Address(r15_thread, JavaThread::polling_page_offset()));
#else
  assert(tmp->is_cpu_register(), "needed");
  const Register poll_addr = tmp->as_register();
  __ get_thread(poll_addr);
  __ movptr(poll_addr, Address(poll_addr, in_bytes(JavaThread::polling_page_offset())));
#endif
  add_debug_info_for_branch(info);
  __ relocate(relocInfo::poll_type);
  address pre_pc = __ pc();
  __ testl(rax, Address(poll_addr, 0));
  address post_pc = __ pc();
  guarantee(pointer_delta(post_pc, pre_pc, 1) == 2 LP64_ONLY(+1), "must be exact length");
  return offset;
}


void LIR_Assembler::move_regs(Register from_reg, Register to_reg) {
  if (from_reg != to_reg) __ mov(to_reg, from_reg);
}

void LIR_Assembler::swap_reg(Register a, Register b) {
  __ xchgptr(a, b);
}


void LIR_Assembler::const2reg(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();

  switch (c->type()) {
    case T_INT: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ movl(dest->as_register(), c->as_jint());
      break;
    }

    case T_ADDRESS: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ movptr(dest->as_register(), c->as_jint());
      break;
    }

    case T_LONG: {
      assert(patch_code == lir_patch_none, "no patching handled here");
#ifdef _LP64
      __ movptr(dest->as_register_lo(), (intptr_t)c->as_jlong());
#else
      __ movptr(dest->as_register_lo(), c->as_jint_lo());
      __ movptr(dest->as_register_hi(), c->as_jint_hi());
#endif // _LP64
      break;
    }

    case T_OBJECT: {
      if (patch_code != lir_patch_none) {
        jobject2reg_with_patching(dest->as_register(), info);
      } else {
        __ movoop(dest->as_register(), c->as_jobject());
      }
      break;
    }

    case T_METADATA: {
      if (patch_code != lir_patch_none) {
        klass2reg_with_patching(dest->as_register(), info);
      } else {
        __ mov_metadata(dest->as_register(), c->as_metadata());
      }
      break;
    }

    case T_FLOAT: {
      if (dest->is_single_xmm()) {
        if (LP64_ONLY(UseAVX <= 2 &&) c->is_zero_float()) {
          __ xorps(dest->as_xmm_float_reg(), dest->as_xmm_float_reg());
        } else {
          __ movflt(dest->as_xmm_float_reg(),
                   InternalAddress(float_constant(c->as_jfloat())));
        }
      } else {
#ifndef _LP64
        assert(dest->is_single_fpu(), "must be");
        assert(dest->fpu_regnr() == 0, "dest must be TOS");
        if (c->is_zero_float()) {
          __ fldz();
        } else if (c->is_one_float()) {
          __ fld1();
        } else {
          __ fld_s (InternalAddress(float_constant(c->as_jfloat())));
        }
#else
        ShouldNotReachHere();
#endif // !_LP64
      }
      break;
    }

    case T_DOUBLE: {
      if (dest->is_double_xmm()) {
        if (LP64_ONLY(UseAVX <= 2 &&) c->is_zero_double()) {
          __ xorpd(dest->as_xmm_double_reg(), dest->as_xmm_double_reg());
        } else {
          __ movdbl(dest->as_xmm_double_reg(),
                    InternalAddress(double_constant(c->as_jdouble())));
        }
      } else {
#ifndef _LP64
        assert(dest->is_double_fpu(), "must be");
        assert(dest->fpu_regnrLo() == 0, "dest must be TOS");
        if (c->is_zero_double()) {
          __ fldz();
        } else if (c->is_one_double()) {
          __ fld1();
        } else {
          __ fld_d (InternalAddress(double_constant(c->as_jdouble())));
        }
#else
        ShouldNotReachHere();
#endif // !_LP64
      }
      break;
    }

    default:
      ShouldNotReachHere();
  }
}

void LIR_Assembler::const2stack(LIR_Opr src, LIR_Opr dest) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_stack(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();

  switch (c->type()) {
    case T_INT:  // fall through
    case T_FLOAT:
      __ movl(frame_map()->address_for_slot(dest->single_stack_ix()), c->as_jint_bits());
      break;

    case T_ADDRESS:
      __ movptr(frame_map()->address_for_slot(dest->single_stack_ix()), c->as_jint_bits());
      break;

    case T_OBJECT:
      __ movoop(frame_map()->address_for_slot(dest->single_stack_ix()), c->as_jobject());
      break;

    case T_LONG:  // fall through
    case T_DOUBLE:
#ifdef _LP64
      __ movptr(frame_map()->address_for_slot(dest->double_stack_ix(),
                                            lo_word_offset_in_bytes), (intptr_t)c->as_jlong_bits());
#else
      __ movptr(frame_map()->address_for_slot(dest->double_stack_ix(),
                                              lo_word_offset_in_bytes), c->as_jint_lo_bits());
      __ movptr(frame_map()->address_for_slot(dest->double_stack_ix(),
                                              hi_word_offset_in_bytes), c->as_jint_hi_bits());
#endif // _LP64
      break;

    default:
      ShouldNotReachHere();
  }
}

void LIR_Assembler::const2mem(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info, bool wide) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_address(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();
  LIR_Address* addr = dest->as_address_ptr();

  int null_check_here = code_offset();
  switch (type) {
    case T_INT:    // fall through
    case T_FLOAT:
      __ movl(as_Address(addr), c->as_jint_bits());
      break;

    case T_ADDRESS:
      __ movptr(as_Address(addr), c->as_jint_bits());
      break;

    case T_OBJECT:  // fall through
    case T_ARRAY:
      if (c->as_jobject() == NULL) {
        if (UseCompressedOops && !wide) {
          __ movl(as_Address(addr), (int32_t)NULL_WORD);
        } else {
#ifdef _LP64
          __ xorptr(rscratch1, rscratch1);
          null_check_here = code_offset();
          __ movptr(as_Address(addr), rscratch1);
#else
          __ movptr(as_Address(addr), NULL_WORD);
#endif
        }
      } else {
        if (is_literal_address(addr)) {
          ShouldNotReachHere();
          __ movoop(as_Address(addr, noreg), c->as_jobject());
        } else {
#ifdef _LP64
          __ movoop(rscratch1, c->as_jobject());
          if (UseCompressedOops && !wide) {
            __ encode_heap_oop(rscratch1);
            null_check_here = code_offset();
            __ movl(as_Address_lo(addr), rscratch1);
          } else {
            null_check_here = code_offset();
            __ movptr(as_Address_lo(addr), rscratch1);
          }
#else
          __ movoop(as_Address(addr), c->as_jobject());
#endif
        }
      }
      break;

    case T_LONG:    // fall through
    case T_DOUBLE:
#ifdef _LP64
      if (is_literal_address(addr)) {
        ShouldNotReachHere();
        __ movptr(as_Address(addr, r15_thread), (intptr_t)c->as_jlong_bits());
      } else {
        __ movptr(r10, (intptr_t)c->as_jlong_bits());
        null_check_here = code_offset();
        __ movptr(as_Address_lo(addr), r10);
      }
#else
      // Always reachable in 32bit so this doesn't produce useless move literal
      __ movptr(as_Address_hi(addr), c->as_jint_hi_bits());
      __ movptr(as_Address_lo(addr), c->as_jint_lo_bits());
#endif // _LP64
      break;

    case T_BOOLEAN: // fall through
    case T_BYTE:
      __ movb(as_Address(addr), c->as_jint() & 0xFF);
      break;

    case T_CHAR:    // fall through
    case T_SHORT:
      __ movw(as_Address(addr), c->as_jint() & 0xFFFF);
      break;

    default:
      ShouldNotReachHere();
  };

  if (info != NULL) {
    add_debug_info_for_null_check(null_check_here, info);
  }
}


void LIR_Assembler::reg2reg(LIR_Opr src, LIR_Opr dest) {
  assert(src->is_register(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");

  // move between cpu-registers
  if (dest->is_single_cpu()) {
#ifdef _LP64
    if (src->type() == T_LONG) {
      // Can do LONG -> OBJECT
      move_regs(src->as_register_lo(), dest->as_register());
      return;
    }
#endif
    assert(src->is_single_cpu(), "must match");
    if (src->type() == T_OBJECT) {
      __ verify_oop(src->as_register());
    }
    move_regs(src->as_register(), dest->as_register());

  } else if (dest->is_double_cpu()) {
#ifdef _LP64
    if (is_reference_type(src->type())) {
      // Surprising to me but we can see move of a long to t_object
      __ verify_oop(src->as_register());
      move_regs(src->as_register(), dest->as_register_lo());
      return;
    }
#endif
    assert(src->is_double_cpu(), "must match");
    Register f_lo = src->as_register_lo();
    Register f_hi = src->as_register_hi();
    Register t_lo = dest->as_register_lo();
    Register t_hi = dest->as_register_hi();
#ifdef _LP64
    assert(f_hi == f_lo, "must be same");
    assert(t_hi == t_lo, "must be same");
    move_regs(f_lo, t_lo);
#else
    assert(f_lo != f_hi && t_lo != t_hi, "invalid register allocation");


    if (f_lo == t_hi && f_hi == t_lo) {
      swap_reg(f_lo, f_hi);
    } else if (f_hi == t_lo) {
      assert(f_lo != t_hi, "overwriting register");
      move_regs(f_hi, t_hi);
      move_regs(f_lo, t_lo);
    } else {
      assert(f_hi != t_lo, "overwriting register");
      move_regs(f_lo, t_lo);
      move_regs(f_hi, t_hi);
    }
#endif // LP64

#ifndef _LP64
    // special moves from fpu-register to xmm-register
    // necessary for method results
  } else if (src->is_single_xmm() && !dest->is_single_xmm()) {
    __ movflt(Address(rsp, 0), src->as_xmm_float_reg());
    __ fld_s(Address(rsp, 0));
  } else if (src->is_double_xmm() && !dest->is_double_xmm()) {
    __ movdbl(Address(rsp, 0), src->as_xmm_double_reg());
    __ fld_d(Address(rsp, 0));
  } else if (dest->is_single_xmm() && !src->is_single_xmm()) {
    __ fstp_s(Address(rsp, 0));
    __ movflt(dest->as_xmm_float_reg(), Address(rsp, 0));
  } else if (dest->is_double_xmm() && !src->is_double_xmm()) {
    __ fstp_d(Address(rsp, 0));
    __ movdbl(dest->as_xmm_double_reg(), Address(rsp, 0));
#endif // !_LP64

    // move between xmm-registers
  } else if (dest->is_single_xmm()) {
    assert(src->is_single_xmm(), "must match");
    __ movflt(dest->as_xmm_float_reg(), src->as_xmm_float_reg());
  } else if (dest->is_double_xmm()) {
    assert(src->is_double_xmm(), "must match");
    __ movdbl(dest->as_xmm_double_reg(), src->as_xmm_double_reg());

#ifndef _LP64
    // move between fpu-registers (no instruction necessary because of fpu-stack)
  } else if (dest->is_single_fpu() || dest->is_double_fpu()) {
    assert(src->is_single_fpu() || src->is_double_fpu(), "must match");
    assert(src->fpu() == dest->fpu(), "currently should be nothing to do");
#endif // !_LP64

  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::reg2stack(LIR_Opr src, LIR_Opr dest, BasicType type, bool pop_fpu_stack) {
  assert(src->is_register(), "should not call otherwise");
  assert(dest->is_stack(), "should not call otherwise");

  if (src->is_single_cpu()) {
    Address dst = frame_map()->address_for_slot(dest->single_stack_ix());
    if (is_reference_type(type)) {
      __ verify_oop(src->as_register());
      __ movptr (dst, src->as_register());
    } else if (type == T_METADATA || type == T_ADDRESS) {
      __ movptr (dst, src->as_register());
    } else {
      __ movl (dst, src->as_register());
    }

  } else if (src->is_double_cpu()) {
    Address dstLO = frame_map()->address_for_slot(dest->double_stack_ix(), lo_word_offset_in_bytes);
    Address dstHI = frame_map()->address_for_slot(dest->double_stack_ix(), hi_word_offset_in_bytes);
    __ movptr (dstLO, src->as_register_lo());
    NOT_LP64(__ movptr (dstHI, src->as_register_hi()));

  } else if (src->is_single_xmm()) {
    Address dst_addr = frame_map()->address_for_slot(dest->single_stack_ix());
    __ movflt(dst_addr, src->as_xmm_float_reg());

  } else if (src->is_double_xmm()) {
    Address dst_addr = frame_map()->address_for_slot(dest->double_stack_ix());
    __ movdbl(dst_addr, src->as_xmm_double_reg());

#ifndef _LP64
  } else if (src->is_single_fpu()) {
    assert(src->fpu_regnr() == 0, "argument must be on TOS");
    Address dst_addr = frame_map()->address_for_slot(dest->single_stack_ix());
    if (pop_fpu_stack)     __ fstp_s (dst_addr);
    else                   __ fst_s  (dst_addr);

  } else if (src->is_double_fpu()) {
    assert(src->fpu_regnrLo() == 0, "argument must be on TOS");
    Address dst_addr = frame_map()->address_for_slot(dest->double_stack_ix());
    if (pop_fpu_stack)     __ fstp_d (dst_addr);
    else                   __ fst_d  (dst_addr);
#endif // !_LP64

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::reg2mem(LIR_Opr src, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack, bool wide) {
  LIR_Address* to_addr = dest->as_address_ptr();
  PatchingStub* patch = NULL;
  Register compressed_src = rscratch1;

  if (is_reference_type(type)) {
    __ verify_oop(src->as_register());
#ifdef _LP64
    if (UseCompressedOops && !wide) {
      __ movptr(compressed_src, src->as_register());
      __ encode_heap_oop(compressed_src);
      if (patch_code != lir_patch_none) {
        info->oop_map()->set_narrowoop(compressed_src->as_VMReg());
      }
    }
#endif
  }

  if (patch_code != lir_patch_none) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
    Address toa = as_Address(to_addr);
    assert(toa.disp() != 0, "must have");
  }

  int null_check_here = code_offset();
  switch (type) {
    case T_FLOAT: {
#ifdef _LP64
      assert(src->is_single_xmm(), "not a float");
      __ movflt(as_Address(to_addr), src->as_xmm_float_reg());
#else
      if (src->is_single_xmm()) {
        __ movflt(as_Address(to_addr), src->as_xmm_float_reg());
      } else {
        assert(src->is_single_fpu(), "must be");
        assert(src->fpu_regnr() == 0, "argument must be on TOS");
        if (pop_fpu_stack)      __ fstp_s(as_Address(to_addr));
        else                    __ fst_s (as_Address(to_addr));
      }
#endif // _LP64
      break;
    }

    case T_DOUBLE: {
#ifdef _LP64
      assert(src->is_double_xmm(), "not a double");
      __ movdbl(as_Address(to_addr), src->as_xmm_double_reg());
#else
      if (src->is_double_xmm()) {
        __ movdbl(as_Address(to_addr), src->as_xmm_double_reg());
      } else {
        assert(src->is_double_fpu(), "must be");
        assert(src->fpu_regnrLo() == 0, "argument must be on TOS");
        if (pop_fpu_stack)      __ fstp_d(as_Address(to_addr));
        else                    __ fst_d (as_Address(to_addr));
      }
#endif // _LP64
      break;
    }

    case T_ARRAY:   // fall through
    case T_OBJECT:  // fall through
      if (UseCompressedOops && !wide) {
        __ movl(as_Address(to_addr), compressed_src);
      } else {
        __ movptr(as_Address(to_addr), src->as_register());
      }
      break;
    case T_METADATA:
      // We get here to store a method pointer to the stack to pass to
      // a dtrace runtime call. This can't work on 64 bit with
      // compressed klass ptrs: T_METADATA can be a compressed klass
      // ptr or a 64 bit method pointer.
      LP64_ONLY(ShouldNotReachHere());
      __ movptr(as_Address(to_addr), src->as_register());
      break;
    case T_ADDRESS:
      __ movptr(as_Address(to_addr), src->as_register());
      break;
    case T_INT:
      __ movl(as_Address(to_addr), src->as_register());
      break;

    case T_LONG: {
      Register from_lo = src->as_register_lo();
      Register from_hi = src->as_register_hi();
#ifdef _LP64
      __ movptr(as_Address_lo(to_addr), from_lo);
#else
      Register base = to_addr->base()->as_register();
      Register index = noreg;
      if (to_addr->index()->is_register()) {
        index = to_addr->index()->as_register();
      }
      if (base == from_lo || index == from_lo) {
        assert(base != from_hi, "can't be");
        assert(index == noreg || (index != base && index != from_hi), "can't handle this");
        __ movl(as_Address_hi(to_addr), from_hi);
        if (patch != NULL) {
          patching_epilog(patch, lir_patch_high, base, info);
          patch = new PatchingStub(_masm, PatchingStub::access_field_id);
          patch_code = lir_patch_low;
        }
        __ movl(as_Address_lo(to_addr), from_lo);
      } else {
        assert(index == noreg || (index != base && index != from_lo), "can't handle this");
        __ movl(as_Address_lo(to_addr), from_lo);
        if (patch != NULL) {
          patching_epilog(patch, lir_patch_low, base, info);
          patch = new PatchingStub(_masm, PatchingStub::access_field_id);
          patch_code = lir_patch_high;
        }
        __ movl(as_Address_hi(to_addr), from_hi);
      }
#endif // _LP64
      break;
    }

    case T_BYTE:    // fall through
    case T_BOOLEAN: {
      Register src_reg = src->as_register();
      Address dst_addr = as_Address(to_addr);
      assert(VM_Version::is_P6() || src_reg->has_byte_register(), "must use byte registers if not P6");
      __ movb(dst_addr, src_reg);
      break;
    }

    case T_CHAR:    // fall through
    case T_SHORT:
      __ movw(as_Address(to_addr), src->as_register());
      break;

    default:
      ShouldNotReachHere();
  }
  if (info != NULL) {
    add_debug_info_for_null_check(null_check_here, info);
  }

  if (patch_code != lir_patch_none) {
    patching_epilog(patch, patch_code, to_addr->base()->as_register(), info);
  }
}


void LIR_Assembler::stack2reg(LIR_Opr src, LIR_Opr dest, BasicType type) {
  assert(src->is_stack(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");

  if (dest->is_single_cpu()) {
    if (is_reference_type(type)) {
      __ movptr(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()));
      __ verify_oop(dest->as_register());
    } else if (type == T_METADATA || type == T_ADDRESS) {
      __ movptr(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()));
    } else {
      __ movl(dest->as_register(), frame_map()->address_for_slot(src->single_stack_ix()));
    }

  } else if (dest->is_double_cpu()) {
    Address src_addr_LO = frame_map()->address_for_slot(src->double_stack_ix(), lo_word_offset_in_bytes);
    Address src_addr_HI = frame_map()->address_for_slot(src->double_stack_ix(), hi_word_offset_in_bytes);
    __ movptr(dest->as_register_lo(), src_addr_LO);
    NOT_LP64(__ movptr(dest->as_register_hi(), src_addr_HI));

  } else if (dest->is_single_xmm()) {
    Address src_addr = frame_map()->address_for_slot(src->single_stack_ix());
    __ movflt(dest->as_xmm_float_reg(), src_addr);

  } else if (dest->is_double_xmm()) {
    Address src_addr = frame_map()->address_for_slot(src->double_stack_ix());
    __ movdbl(dest->as_xmm_double_reg(), src_addr);

#ifndef _LP64
  } else if (dest->is_single_fpu()) {
    assert(dest->fpu_regnr() == 0, "dest must be TOS");
    Address src_addr = frame_map()->address_for_slot(src->single_stack_ix());
    __ fld_s(src_addr);

  } else if (dest->is_double_fpu()) {
    assert(dest->fpu_regnrLo() == 0, "dest must be TOS");
    Address src_addr = frame_map()->address_for_slot(src->double_stack_ix());
    __ fld_d(src_addr);
#endif // _LP64

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::stack2stack(LIR_Opr src, LIR_Opr dest, BasicType type) {
  if (src->is_single_stack()) {
    if (is_reference_type(type)) {
      __ pushptr(frame_map()->address_for_slot(src ->single_stack_ix()));
      __ popptr (frame_map()->address_for_slot(dest->single_stack_ix()));
    } else {
#ifndef _LP64
      __ pushl(frame_map()->address_for_slot(src ->single_stack_ix()));
      __ popl (frame_map()->address_for_slot(dest->single_stack_ix()));
#else
      //no pushl on 64bits
      __ movl(rscratch1, frame_map()->address_for_slot(src ->single_stack_ix()));
      __ movl(frame_map()->address_for_slot(dest->single_stack_ix()), rscratch1);
#endif
    }

  } else if (src->is_double_stack()) {
#ifdef _LP64
    __ pushptr(frame_map()->address_for_slot(src ->double_stack_ix()));
    __ popptr (frame_map()->address_for_slot(dest->double_stack_ix()));
#else
    __ pushl(frame_map()->address_for_slot(src ->double_stack_ix(), 0));
    // push and pop the part at src + wordSize, adding wordSize for the previous push
    __ pushl(frame_map()->address_for_slot(src ->double_stack_ix(), 2 * wordSize));
    __ popl (frame_map()->address_for_slot(dest->double_stack_ix(), 2 * wordSize));
    __ popl (frame_map()->address_for_slot(dest->double_stack_ix(), 0));
#endif // _LP64

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::mem2reg(LIR_Opr src, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code, CodeEmitInfo* info, bool wide) {
  assert(src->is_address(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");

  LIR_Address* addr = src->as_address_ptr();
  Address from_addr = as_Address(addr);
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);

  if (addr->base()->type() == T_OBJECT) {
    __ verify_oop(addr->base()->as_pointer_register());
  }

  switch (type) {
    case T_BOOLEAN: // fall through
    case T_BYTE:    // fall through
    case T_CHAR:    // fall through
    case T_SHORT:
      if (!VM_Version::is_P6() && !from_addr.uses(dest->as_register())) {
        // on pre P6 processors we may get partial register stalls
        // so blow away the value of to_rinfo before loading a
        // partial word into it.  Do it here so that it precedes
        // the potential patch point below.
        __ xorptr(dest->as_register(), dest->as_register());
      }
      break;
   default:
     break;
  }

  PatchingStub* patch = NULL;
  if (patch_code != lir_patch_none) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
    assert(from_addr.disp() != 0, "must have");
  }
  if (info != NULL) {
    add_debug_info_for_null_check_here(info);
  }

  switch (type) {
    case T_FLOAT: {
      if (dest->is_single_xmm()) {
        __ movflt(dest->as_xmm_float_reg(), from_addr);
      } else {
#ifndef _LP64
        assert(dest->is_single_fpu(), "must be");
        assert(dest->fpu_regnr() == 0, "dest must be TOS");
        __ fld_s(from_addr);
#else
        ShouldNotReachHere();
#endif // !LP64
      }
      break;
    }

    case T_DOUBLE: {
      if (dest->is_double_xmm()) {
        __ movdbl(dest->as_xmm_double_reg(), from_addr);
      } else {
#ifndef _LP64
        assert(dest->is_double_fpu(), "must be");
        assert(dest->fpu_regnrLo() == 0, "dest must be TOS");
        __ fld_d(from_addr);
#else
        ShouldNotReachHere();
#endif // !LP64
      }
      break;
    }

    case T_OBJECT:  // fall through
    case T_ARRAY:   // fall through
      if (UseCompressedOops && !wide) {
        __ movl(dest->as_register(), from_addr);
      } else {
        __ movptr(dest->as_register(), from_addr);
      }
      break;

    case T_ADDRESS:
      if (UseCompressedClassPointers && addr->disp() == oopDesc::klass_offset_in_bytes()) {
        __ movl(dest->as_register(), from_addr);
      } else {
        __ movptr(dest->as_register(), from_addr);
      }
      break;
    case T_INT:
      __ movl(dest->as_register(), from_addr);
      break;

    case T_LONG: {
      Register to_lo = dest->as_register_lo();
      Register to_hi = dest->as_register_hi();
#ifdef _LP64
      __ movptr(to_lo, as_Address_lo(addr));
#else
      Register base = addr->base()->as_register();
      Register index = noreg;
      if (addr->index()->is_register()) {
        index = addr->index()->as_register();
      }
      if ((base == to_lo && index == to_hi) ||
          (base == to_hi && index == to_lo)) {
        // addresses with 2 registers are only formed as a result of
        // array access so this code will never have to deal with
        // patches or null checks.
        assert(info == NULL && patch == NULL, "must be");
        __ lea(to_hi, as_Address(addr));
        __ movl(to_lo, Address(to_hi, 0));
        __ movl(to_hi, Address(to_hi, BytesPerWord));
      } else if (base == to_lo || index == to_lo) {
        assert(base != to_hi, "can't be");
        assert(index == noreg || (index != base && index != to_hi), "can't handle this");
        __ movl(to_hi, as_Address_hi(addr));
        if (patch != NULL) {
          patching_epilog(patch, lir_patch_high, base, info);
          patch = new PatchingStub(_masm, PatchingStub::access_field_id);
          patch_code = lir_patch_low;
        }
        __ movl(to_lo, as_Address_lo(addr));
      } else {
        assert(index == noreg || (index != base && index != to_lo), "can't handle this");
        __ movl(to_lo, as_Address_lo(addr));
        if (patch != NULL) {
          patching_epilog(patch, lir_patch_low, base, info);
          patch = new PatchingStub(_masm, PatchingStub::access_field_id);
          patch_code = lir_patch_high;
        }
        __ movl(to_hi, as_Address_hi(addr));
      }
#endif // _LP64
      break;
    }

    case T_BOOLEAN: // fall through
    case T_BYTE: {
      Register dest_reg = dest->as_register();
      assert(VM_Version::is_P6() || dest_reg->has_byte_register(), "must use byte registers if not P6");
      if (VM_Version::is_P6() || from_addr.uses(dest_reg)) {
        __ movsbl(dest_reg, from_addr);
      } else {
        __ movb(dest_reg, from_addr);
        __ shll(dest_reg, 24);
        __ sarl(dest_reg, 24);
      }
      break;
    }

    case T_CHAR: {
      Register dest_reg = dest->as_register();
      assert(VM_Version::is_P6() || dest_reg->has_byte_register(), "must use byte registers if not P6");
      if (VM_Version::is_P6() || from_addr.uses(dest_reg)) {
        __ movzwl(dest_reg, from_addr);
      } else {
        __ movw(dest_reg, from_addr);
      }
      break;
    }

    case T_SHORT: {
      Register dest_reg = dest->as_register();
      if (VM_Version::is_P6() || from_addr.uses(dest_reg)) {
        __ movswl(dest_reg, from_addr);
      } else {
        __ movw(dest_reg, from_addr);
        __ shll(dest_reg, 16);
        __ sarl(dest_reg, 16);
      }
      break;
    }

    default:
      ShouldNotReachHere();
  }

  if (patch != NULL) {
    patching_epilog(patch, patch_code, addr->base()->as_register(), info);
  }

  if (is_reference_type(type)) {
#ifdef _LP64
    if (UseCompressedOops && !wide) {
      __ decode_heap_oop(dest->as_register());
    }
#endif

    // Load barrier has not yet been applied, so ZGC can't verify the oop here
    if (!UseZGC) {
      __ verify_oop(dest->as_register());
    }
  } else if (type == T_ADDRESS && addr->disp() == oopDesc::klass_offset_in_bytes()) {
#ifdef _LP64
    if (UseCompressedClassPointers) {
      __ decode_klass_not_null(dest->as_register(), tmp_load_klass);
    }
#endif
  }
}


NEEDS_CLEANUP; // This could be static?
Address::ScaleFactor LIR_Assembler::array_element_size(BasicType type) const {
  int elem_size = type2aelembytes(type);
  switch (elem_size) {
    case 1: return Address::times_1;
    case 2: return Address::times_2;
    case 4: return Address::times_4;
    case 8: return Address::times_8;
  }
  ShouldNotReachHere();
  return Address::no_scale;
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
    case lir_fmad:
      __ fmad(op->result_opr()->as_xmm_double_reg(),
              op->in_opr1()->as_xmm_double_reg(),
              op->in_opr2()->as_xmm_double_reg(),
              op->in_opr3()->as_xmm_double_reg());
      break;
    case lir_fmaf:
      __ fmaf(op->result_opr()->as_xmm_float_reg(),
              op->in_opr1()->as_xmm_float_reg(),
              op->in_opr2()->as_xmm_float_reg(),
              op->in_opr3()->as_xmm_float_reg());
      break;
    default:      ShouldNotReachHere(); break;
  }
}

void LIR_Assembler::emit_opBranch(LIR_OpBranch* op) {
#ifdef ASSERT
  assert(op->block() == NULL || op->block()->label() == op->label(), "wrong label");
  if (op->block() != NULL)  _branch_target_blocks.append(op->block());
  if (op->ublock() != NULL) _branch_target_blocks.append(op->ublock());
#endif

  if (op->cond() == lir_cond_always) {
    if (op->info() != NULL) add_debug_info_for_branch(op->info());
    __ jmp (*(op->label()));
  } else {
    Assembler::Condition acond = Assembler::zero;
    if (op->code() == lir_cond_float_branch) {
      assert(op->ublock() != NULL, "must have unordered successor");
      __ jcc(Assembler::parity, *(op->ublock()->label()));
      switch(op->cond()) {
        case lir_cond_equal:        acond = Assembler::equal;      break;
        case lir_cond_notEqual:     acond = Assembler::notEqual;   break;
        case lir_cond_less:         acond = Assembler::below;      break;
        case lir_cond_lessEqual:    acond = Assembler::belowEqual; break;
        case lir_cond_greaterEqual: acond = Assembler::aboveEqual; break;
        case lir_cond_greater:      acond = Assembler::above;      break;
        default:                         ShouldNotReachHere();
      }
    } else {
      switch (op->cond()) {
        case lir_cond_equal:        acond = Assembler::equal;       break;
        case lir_cond_notEqual:     acond = Assembler::notEqual;    break;
        case lir_cond_less:         acond = Assembler::less;        break;
        case lir_cond_lessEqual:    acond = Assembler::lessEqual;   break;
        case lir_cond_greaterEqual: acond = Assembler::greaterEqual;break;
        case lir_cond_greater:      acond = Assembler::greater;     break;
        case lir_cond_belowEqual:   acond = Assembler::belowEqual;  break;
        case lir_cond_aboveEqual:   acond = Assembler::aboveEqual;  break;
        default:                         ShouldNotReachHere();
      }
    }
    __ jcc(acond,*(op->label()));
  }
}

void LIR_Assembler::emit_opConvert(LIR_OpConvert* op) {
  LIR_Opr src  = op->in_opr();
  LIR_Opr dest = op->result_opr();

  switch (op->bytecode()) {
    case Bytecodes::_i2l:
#ifdef _LP64
      __ movl2ptr(dest->as_register_lo(), src->as_register());
#else
      move_regs(src->as_register(), dest->as_register_lo());
      move_regs(src->as_register(), dest->as_register_hi());
      __ sarl(dest->as_register_hi(), 31);
#endif // LP64
      break;

    case Bytecodes::_l2i:
#ifdef _LP64
      __ movl(dest->as_register(), src->as_register_lo());
#else
      move_regs(src->as_register_lo(), dest->as_register());
#endif
      break;

    case Bytecodes::_i2b:
      move_regs(src->as_register(), dest->as_register());
      __ sign_extend_byte(dest->as_register());
      break;

    case Bytecodes::_i2c:
      move_regs(src->as_register(), dest->as_register());
      __ andl(dest->as_register(), 0xFFFF);
      break;

    case Bytecodes::_i2s:
      move_regs(src->as_register(), dest->as_register());
      __ sign_extend_short(dest->as_register());
      break;


#ifdef _LP64
    case Bytecodes::_f2d:
      __ cvtss2sd(dest->as_xmm_double_reg(), src->as_xmm_float_reg());
      break;

    case Bytecodes::_d2f:
      __ cvtsd2ss(dest->as_xmm_float_reg(), src->as_xmm_double_reg());
      break;

    case Bytecodes::_i2f:
      __ cvtsi2ssl(dest->as_xmm_float_reg(), src->as_register());
      break;

    case Bytecodes::_i2d:
      __ cvtsi2sdl(dest->as_xmm_double_reg(), src->as_register());
      break;

    case Bytecodes::_l2f:
      __ cvtsi2ssq(dest->as_xmm_float_reg(), src->as_register_lo());
      break;

    case Bytecodes::_l2d:
      __ cvtsi2sdq(dest->as_xmm_double_reg(), src->as_register_lo());
      break;

    case Bytecodes::_f2i:
      __ convert_f2i(dest->as_register(), src->as_xmm_float_reg());
      break;

    case Bytecodes::_d2i:
      __ convert_d2i(dest->as_register(), src->as_xmm_double_reg());
      break;

    case Bytecodes::_f2l:
      __ convert_f2l(dest->as_register_lo(), src->as_xmm_float_reg());
      break;

    case Bytecodes::_d2l:
      __ convert_d2l(dest->as_register_lo(), src->as_xmm_double_reg());
      break;
#else
    case Bytecodes::_f2d:
    case Bytecodes::_d2f:
      if (dest->is_single_xmm()) {
        __ cvtsd2ss(dest->as_xmm_float_reg(), src->as_xmm_double_reg());
      } else if (dest->is_double_xmm()) {
        __ cvtss2sd(dest->as_xmm_double_reg(), src->as_xmm_float_reg());
      } else {
        assert(src->fpu() == dest->fpu(), "register must be equal");
        // do nothing (float result is rounded later through spilling)
      }
      break;

    case Bytecodes::_i2f:
    case Bytecodes::_i2d:
      if (dest->is_single_xmm()) {
        __ cvtsi2ssl(dest->as_xmm_float_reg(), src->as_register());
      } else if (dest->is_double_xmm()) {
        __ cvtsi2sdl(dest->as_xmm_double_reg(), src->as_register());
      } else {
        assert(dest->fpu() == 0, "result must be on TOS");
        __ movl(Address(rsp, 0), src->as_register());
        __ fild_s(Address(rsp, 0));
      }
      break;

    case Bytecodes::_l2f:
    case Bytecodes::_l2d:
      assert(!dest->is_xmm_register(), "result in xmm register not supported (no SSE instruction present)");
      assert(dest->fpu() == 0, "result must be on TOS");
      __ movptr(Address(rsp, 0),          src->as_register_lo());
      __ movl(Address(rsp, BytesPerWord), src->as_register_hi());
      __ fild_d(Address(rsp, 0));
      // float result is rounded later through spilling
      break;

    case Bytecodes::_f2i:
    case Bytecodes::_d2i:
      if (src->is_single_xmm()) {
        __ cvttss2sil(dest->as_register(), src->as_xmm_float_reg());
      } else if (src->is_double_xmm()) {
        __ cvttsd2sil(dest->as_register(), src->as_xmm_double_reg());
      } else {
        assert(src->fpu() == 0, "input must be on TOS");
        __ fldcw(ExternalAddress(StubRoutines::x86::addr_fpu_cntrl_wrd_trunc()));
        __ fist_s(Address(rsp, 0));
        __ movl(dest->as_register(), Address(rsp, 0));
        __ fldcw(ExternalAddress(StubRoutines::x86::addr_fpu_cntrl_wrd_std()));
      }
      // IA32 conversion instructions do not match JLS for overflow, underflow and NaN -> fixup in stub
      assert(op->stub() != NULL, "stub required");
      __ cmpl(dest->as_register(), 0x80000000);
      __ jcc(Assembler::equal, *op->stub()->entry());
      __ bind(*op->stub()->continuation());
      break;

    case Bytecodes::_f2l:
    case Bytecodes::_d2l:
      assert(!src->is_xmm_register(), "input in xmm register not supported (no SSE instruction present)");
      assert(src->fpu() == 0, "input must be on TOS");
      assert(dest == FrameMap::long0_opr, "runtime stub places result in these registers");

      // instruction sequence too long to inline it here
      {
        __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::fpu2long_stub_id)));
      }
      break;
#endif // _LP64

    default: ShouldNotReachHere();
  }
}

void LIR_Assembler::emit_alloc_obj(LIR_OpAllocObj* op) {
  if (op->init_check()) {
    add_debug_info_for_null_check_here(op->stub()->info());
    __ cmpb(Address(op->klass()->as_register(),
                    InstanceKlass::init_state_offset()),
                    InstanceKlass::fully_initialized);
    __ jcc(Assembler::notEqual, *op->stub()->entry());
  }
  __ allocate_object(op->obj()->as_register(),
                     op->tmp1()->as_register(),
                     op->tmp2()->as_register(),
                     op->header_size(),
                     op->object_size(),
                     op->klass()->as_register(),
                     *op->stub()->entry());
  __ bind(*op->stub()->continuation());
}

void LIR_Assembler::emit_alloc_array(LIR_OpAllocArray* op) {
  Register len =  op->len()->as_register();
  LP64_ONLY( __ movslq(len, len); )

  if (UseSlowPath ||
      (!UseFastNewObjectArray && is_reference_type(op->type())) ||
      (!UseFastNewTypeArray   && !is_reference_type(op->type()))) {
    __ jmp(*op->stub()->entry());
  } else {
    Register tmp1 = op->tmp1()->as_register();
    Register tmp2 = op->tmp2()->as_register();
    Register tmp3 = op->tmp3()->as_register();
    if (len == tmp1) {
      tmp1 = tmp3;
    } else if (len == tmp2) {
      tmp2 = tmp3;
    } else if (len == tmp3) {
      // everything is ok
    } else {
      __ mov(tmp3, len);
    }
    __ allocate_array(op->obj()->as_register(),
                      len,
                      tmp1,
                      tmp2,
                      arrayOopDesc::header_size(op->type()),
                      array_element_size(op->type()),
                      op->klass()->as_register(),
                      *op->stub()->entry());
  }
  __ bind(*op->stub()->continuation());
}

void LIR_Assembler::type_profile_helper(Register mdo,
                                        ciMethodData *md, ciProfileData *data,
                                        Register recv, Label* update_done) {
  for (uint i = 0; i < ReceiverTypeData::row_limit(); i++) {
    Label next_test;
    // See if the receiver is receiver[n].
    __ cmpptr(recv, Address(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i))));
    __ jccb(Assembler::notEqual, next_test);
    Address data_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)));
    __ addptr(data_addr, DataLayout::counter_increment);
    __ jmp(*update_done);
    __ bind(next_test);
  }

  // Didn't find receiver; find next empty slot and fill it in
  for (uint i = 0; i < ReceiverTypeData::row_limit(); i++) {
    Label next_test;
    Address recv_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i)));
    __ cmpptr(recv_addr, (intptr_t)NULL_WORD);
    __ jccb(Assembler::notEqual, next_test);
    __ movptr(recv_addr, recv);
    __ movptr(Address(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i))), DataLayout::counter_increment);
    __ jmp(*update_done);
    __ bind(next_test);
  }
}

void LIR_Assembler::emit_typecheck_helper(LIR_OpTypeCheck *op, Label* success, Label* failure, Label* obj_is_null) {
  // we always need a stub for the failure case.
  CodeStub* stub = op->stub();
  Register obj = op->object()->as_register();
  Register k_RInfo = op->tmp1()->as_register();
  Register klass_RInfo = op->tmp2()->as_register();
  Register dst = op->result_opr()->as_register();
  ciKlass* k = op->klass();
  Register Rtmp1 = noreg;
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);

  // check if it needs to be profiled
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
  Label profile_cast_success, profile_cast_failure;
  Label *success_target = op->should_profile() ? &profile_cast_success : success;
  Label *failure_target = op->should_profile() ? &profile_cast_failure : failure;

  if (obj == k_RInfo) {
    k_RInfo = dst;
  } else if (obj == klass_RInfo) {
    klass_RInfo = dst;
  }
  if (k->is_loaded() && !UseCompressedClassPointers) {
    select_different_registers(obj, dst, k_RInfo, klass_RInfo);
  } else {
    Rtmp1 = op->tmp3()->as_register();
    select_different_registers(obj, dst, k_RInfo, klass_RInfo, Rtmp1);
  }

  assert_different_registers(obj, k_RInfo, klass_RInfo);

  __ cmpptr(obj, (int32_t)NULL_WORD);
  if (op->should_profile()) {
    Label not_null;
    __ jccb(Assembler::notEqual, not_null);
    // Object is null; update MDO and exit
    Register mdo  = klass_RInfo;
    __ mov_metadata(mdo, md->constant_encoding());
    Address data_addr(mdo, md->byte_offset_of_slot(data, DataLayout::flags_offset()));
    int header_bits = BitData::null_seen_byte_constant();
    __ orb(data_addr, header_bits);
    __ jmp(*obj_is_null);
    __ bind(not_null);
  } else {
    __ jcc(Assembler::equal, *obj_is_null);
  }

  if (!k->is_loaded()) {
    klass2reg_with_patching(k_RInfo, op->info_for_patch());
  } else {
#ifdef _LP64
    __ mov_metadata(k_RInfo, k->constant_encoding());
#endif // _LP64
  }
  __ verify_oop(obj);

  if (op->fast_check()) {
    // get object class
    // not a safepoint as obj null check happens earlier
#ifdef _LP64
    if (UseCompressedClassPointers) {
      __ load_klass(Rtmp1, obj, tmp_load_klass);
      __ cmpptr(k_RInfo, Rtmp1);
    } else {
      __ cmpptr(k_RInfo, Address(obj, oopDesc::klass_offset_in_bytes()));
    }
#else
    if (k->is_loaded()) {
      __ cmpklass(Address(obj, oopDesc::klass_offset_in_bytes()), k->constant_encoding());
    } else {
      __ cmpptr(k_RInfo, Address(obj, oopDesc::klass_offset_in_bytes()));
    }
#endif
    __ jcc(Assembler::notEqual, *failure_target);
    // successful cast, fall through to profile or jump
  } else {
    // get object class
    // not a safepoint as obj null check happens earlier
    __ load_klass(klass_RInfo, obj, tmp_load_klass);
    if (k->is_loaded()) {
      // See if we get an immediate positive hit
#ifdef _LP64
      __ cmpptr(k_RInfo, Address(klass_RInfo, k->super_check_offset()));
#else
      __ cmpklass(Address(klass_RInfo, k->super_check_offset()), k->constant_encoding());
#endif // _LP64
      if ((juint)in_bytes(Klass::secondary_super_cache_offset()) != k->super_check_offset()) {
        __ jcc(Assembler::notEqual, *failure_target);
        // successful cast, fall through to profile or jump
      } else {
        // See if we get an immediate positive hit
        __ jcc(Assembler::equal, *success_target);
        // check for self
#ifdef _LP64
        __ cmpptr(klass_RInfo, k_RInfo);
#else
        __ cmpklass(klass_RInfo, k->constant_encoding());
#endif // _LP64
        __ jcc(Assembler::equal, *success_target);

        __ push(klass_RInfo);
#ifdef _LP64
        __ push(k_RInfo);
#else
        __ pushklass(k->constant_encoding());
#endif // _LP64
        __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
        __ pop(klass_RInfo);
        __ pop(klass_RInfo);
        // result is a boolean
        __ cmpl(klass_RInfo, 0);
        __ jcc(Assembler::equal, *failure_target);
        // successful cast, fall through to profile or jump
      }
    } else {
      // perform the fast part of the checking logic
      __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, success_target, failure_target, NULL);
      // call out-of-line instance of __ check_klass_subtype_slow_path(...):
      __ push(klass_RInfo);
      __ push(k_RInfo);
      __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
      __ pop(klass_RInfo);
      __ pop(k_RInfo);
      // result is a boolean
      __ cmpl(k_RInfo, 0);
      __ jcc(Assembler::equal, *failure_target);
      // successful cast, fall through to profile or jump
    }
  }
  if (op->should_profile()) {
    Register mdo  = klass_RInfo, recv = k_RInfo;
    __ bind(profile_cast_success);
    __ mov_metadata(mdo, md->constant_encoding());
    __ load_klass(recv, obj, tmp_load_klass);
    type_profile_helper(mdo, md, data, recv, success);
    __ jmp(*success);

    __ bind(profile_cast_failure);
    __ mov_metadata(mdo, md->constant_encoding());
    Address counter_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()));
    __ subptr(counter_addr, DataLayout::counter_increment);
    __ jmp(*failure);
  }
  __ jmp(*success);
}


void LIR_Assembler::emit_opTypeCheck(LIR_OpTypeCheck* op) {
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);
  LIR_Code code = op->code();
  if (code == lir_store_check) {
    Register value = op->object()->as_register();
    Register array = op->array()->as_register();
    Register k_RInfo = op->tmp1()->as_register();
    Register klass_RInfo = op->tmp2()->as_register();
    Register Rtmp1 = op->tmp3()->as_register();

    CodeStub* stub = op->stub();

    // check if it needs to be profiled
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
    Label profile_cast_success, profile_cast_failure, done;
    Label *success_target = op->should_profile() ? &profile_cast_success : &done;
    Label *failure_target = op->should_profile() ? &profile_cast_failure : stub->entry();

    __ cmpptr(value, (int32_t)NULL_WORD);
    if (op->should_profile()) {
      Label not_null;
      __ jccb(Assembler::notEqual, not_null);
      // Object is null; update MDO and exit
      Register mdo  = klass_RInfo;
      __ mov_metadata(mdo, md->constant_encoding());
      Address data_addr(mdo, md->byte_offset_of_slot(data, DataLayout::flags_offset()));
      int header_bits = BitData::null_seen_byte_constant();
      __ orb(data_addr, header_bits);
      __ jmp(done);
      __ bind(not_null);
    } else {
      __ jcc(Assembler::equal, done);
    }

    add_debug_info_for_null_check_here(op->info_for_exception());
    __ load_klass(k_RInfo, array, tmp_load_klass);
    __ load_klass(klass_RInfo, value, tmp_load_klass);

    // get instance klass (it's already uncompressed)
    __ movptr(k_RInfo, Address(k_RInfo, ObjArrayKlass::element_klass_offset()));
    // perform the fast part of the checking logic
    __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, success_target, failure_target, NULL);
    // call out-of-line instance of __ check_klass_subtype_slow_path(...):
    __ push(klass_RInfo);
    __ push(k_RInfo);
    __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
    __ pop(klass_RInfo);
    __ pop(k_RInfo);
    // result is a boolean
    __ cmpl(k_RInfo, 0);
    __ jcc(Assembler::equal, *failure_target);
    // fall through to the success case

    if (op->should_profile()) {
      Register mdo  = klass_RInfo, recv = k_RInfo;
      __ bind(profile_cast_success);
      __ mov_metadata(mdo, md->constant_encoding());
      __ load_klass(recv, value, tmp_load_klass);
      type_profile_helper(mdo, md, data, recv, &done);
      __ jmpb(done);

      __ bind(profile_cast_failure);
      __ mov_metadata(mdo, md->constant_encoding());
      Address counter_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()));
      __ subptr(counter_addr, DataLayout::counter_increment);
      __ jmp(*stub->entry());
    }

    __ bind(done);
  } else
    if (code == lir_checkcast) {
      Register obj = op->object()->as_register();
      Register dst = op->result_opr()->as_register();
      Label success;
      emit_typecheck_helper(op, &success, op->stub()->entry(), &success);
      __ bind(success);
      if (dst != obj) {
        __ mov(dst, obj);
      }
    } else
      if (code == lir_instanceof) {
        Register obj = op->object()->as_register();
        Register dst = op->result_opr()->as_register();
        Label success, failure, done;
        emit_typecheck_helper(op, &success, &failure, &failure);
        __ bind(failure);
        __ xorptr(dst, dst);
        __ jmpb(done);
        __ bind(success);
        __ movptr(dst, 1);
        __ bind(done);
      } else {
        ShouldNotReachHere();
      }

}


void LIR_Assembler::emit_compare_and_swap(LIR_OpCompareAndSwap* op) {
  if (LP64_ONLY(false &&) op->code() == lir_cas_long && VM_Version::supports_cx8()) {
    assert(op->cmp_value()->as_register_lo() == rax, "wrong register");
    assert(op->cmp_value()->as_register_hi() == rdx, "wrong register");
    assert(op->new_value()->as_register_lo() == rbx, "wrong register");
    assert(op->new_value()->as_register_hi() == rcx, "wrong register");
    Register addr = op->addr()->as_register();
    __ lock();
    NOT_LP64(__ cmpxchg8(Address(addr, 0)));

  } else if (op->code() == lir_cas_int || op->code() == lir_cas_obj ) {
    NOT_LP64(assert(op->addr()->is_single_cpu(), "must be single");)
    Register addr = (op->addr()->is_single_cpu() ? op->addr()->as_register() : op->addr()->as_register_lo());
    Register newval = op->new_value()->as_register();
    Register cmpval = op->cmp_value()->as_register();
    assert(cmpval == rax, "wrong register");
    assert(newval != NULL, "new val must be register");
    assert(cmpval != newval, "cmp and new values must be in different registers");
    assert(cmpval != addr, "cmp and addr must be in different registers");
    assert(newval != addr, "new value and addr must be in different registers");

    if ( op->code() == lir_cas_obj) {
#ifdef _LP64
      if (UseCompressedOops) {
        __ encode_heap_oop(cmpval);
        __ mov(rscratch1, newval);
        __ encode_heap_oop(rscratch1);
        __ lock();
        // cmpval (rax) is implicitly used by this instruction
        __ cmpxchgl(rscratch1, Address(addr, 0));
      } else
#endif
      {
        __ lock();
        __ cmpxchgptr(newval, Address(addr, 0));
      }
    } else {
      assert(op->code() == lir_cas_int, "lir_cas_int expected");
      __ lock();
      __ cmpxchgl(newval, Address(addr, 0));
    }
#ifdef _LP64
  } else if (op->code() == lir_cas_long) {
    Register addr = (op->addr()->is_single_cpu() ? op->addr()->as_register() : op->addr()->as_register_lo());
    Register newval = op->new_value()->as_register_lo();
    Register cmpval = op->cmp_value()->as_register_lo();
    assert(cmpval == rax, "wrong register");
    assert(newval != NULL, "new val must be register");
    assert(cmpval != newval, "cmp and new values must be in different registers");
    assert(cmpval != addr, "cmp and addr must be in different registers");
    assert(newval != addr, "new value and addr must be in different registers");
    __ lock();
    __ cmpxchgq(newval, Address(addr, 0));
#endif // _LP64
  } else {
    Unimplemented();
  }
}

void LIR_Assembler::cmove(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, BasicType type) {
  Assembler::Condition acond, ncond;
  switch (condition) {
    case lir_cond_equal:        acond = Assembler::equal;        ncond = Assembler::notEqual;     break;
    case lir_cond_notEqual:     acond = Assembler::notEqual;     ncond = Assembler::equal;        break;
    case lir_cond_less:         acond = Assembler::less;         ncond = Assembler::greaterEqual; break;
    case lir_cond_lessEqual:    acond = Assembler::lessEqual;    ncond = Assembler::greater;      break;
    case lir_cond_greaterEqual: acond = Assembler::greaterEqual; ncond = Assembler::less;         break;
    case lir_cond_greater:      acond = Assembler::greater;      ncond = Assembler::lessEqual;    break;
    case lir_cond_belowEqual:   acond = Assembler::belowEqual;   ncond = Assembler::above;        break;
    case lir_cond_aboveEqual:   acond = Assembler::aboveEqual;   ncond = Assembler::below;        break;
    default:                    acond = Assembler::equal;        ncond = Assembler::notEqual;
                                ShouldNotReachHere();
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

  if (VM_Version::supports_cmov() && !opr2->is_constant()) {
    // optimized version that does not require a branch
    if (opr2->is_single_cpu()) {
      assert(opr2->cpu_regnr() != result->cpu_regnr(), "opr2 already overwritten by previous move");
      __ cmov(ncond, result->as_register(), opr2->as_register());
    } else if (opr2->is_double_cpu()) {
      assert(opr2->cpu_regnrLo() != result->cpu_regnrLo() && opr2->cpu_regnrLo() != result->cpu_regnrHi(), "opr2 already overwritten by previous move");
      assert(opr2->cpu_regnrHi() != result->cpu_regnrLo() && opr2->cpu_regnrHi() != result->cpu_regnrHi(), "opr2 already overwritten by previous move");
      __ cmovptr(ncond, result->as_register_lo(), opr2->as_register_lo());
      NOT_LP64(__ cmovptr(ncond, result->as_register_hi(), opr2->as_register_hi());)
    } else if (opr2->is_single_stack()) {
      __ cmovl(ncond, result->as_register(), frame_map()->address_for_slot(opr2->single_stack_ix()));
    } else if (opr2->is_double_stack()) {
      __ cmovptr(ncond, result->as_register_lo(), frame_map()->address_for_slot(opr2->double_stack_ix(), lo_word_offset_in_bytes));
      NOT_LP64(__ cmovptr(ncond, result->as_register_hi(), frame_map()->address_for_slot(opr2->double_stack_ix(), hi_word_offset_in_bytes));)
    } else {
      ShouldNotReachHere();
    }

  } else {
    Label skip;
    __ jcc (acond, skip);
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


void LIR_Assembler::arith_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest, CodeEmitInfo* info, bool pop_fpu_stack) {
  assert(info == NULL, "should never be used, idiv/irem and ldiv/lrem not handled by this method");

  if (left->is_single_cpu()) {
    assert(left == dest, "left and dest must be equal");
    Register lreg = left->as_register();

    if (right->is_single_cpu()) {
      // cpu register - cpu register
      Register rreg = right->as_register();
      switch (code) {
        case lir_add: __ addl (lreg, rreg); break;
        case lir_sub: __ subl (lreg, rreg); break;
        case lir_mul: __ imull(lreg, rreg); break;
        default:      ShouldNotReachHere();
      }

    } else if (right->is_stack()) {
      // cpu register - stack
      Address raddr = frame_map()->address_for_slot(right->single_stack_ix());
      switch (code) {
        case lir_add: __ addl(lreg, raddr); break;
        case lir_sub: __ subl(lreg, raddr); break;
        default:      ShouldNotReachHere();
      }

    } else if (right->is_constant()) {
      // cpu register - constant
      jint c = right->as_constant_ptr()->as_jint();
      switch (code) {
        case lir_add: {
          __ incrementl(lreg, c);
          break;
        }
        case lir_sub: {
          __ decrementl(lreg, c);
          break;
        }
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
      NOT_LP64(assert_different_registers(lreg_lo, lreg_hi, rreg_lo, rreg_hi));
      LP64_ONLY(assert_different_registers(lreg_lo, rreg_lo));
      switch (code) {
        case lir_add:
          __ addptr(lreg_lo, rreg_lo);
          NOT_LP64(__ adcl(lreg_hi, rreg_hi));
          break;
        case lir_sub:
          __ subptr(lreg_lo, rreg_lo);
          NOT_LP64(__ sbbl(lreg_hi, rreg_hi));
          break;
        case lir_mul:
#ifdef _LP64
          __ imulq(lreg_lo, rreg_lo);
#else
          assert(lreg_lo == rax && lreg_hi == rdx, "must be");
          __ imull(lreg_hi, rreg_lo);
          __ imull(rreg_hi, lreg_lo);
          __ addl (rreg_hi, lreg_hi);
          __ mull (rreg_lo);
          __ addl (lreg_hi, rreg_hi);
#endif // _LP64
          break;
        default:
          ShouldNotReachHere();
      }

    } else if (right->is_constant()) {
      // cpu register - constant
#ifdef _LP64
      jlong c = right->as_constant_ptr()->as_jlong_bits();
      __ movptr(r10, (intptr_t) c);
      switch (code) {
        case lir_add:
          __ addptr(lreg_lo, r10);
          break;
        case lir_sub:
          __ subptr(lreg_lo, r10);
          break;
        default:
          ShouldNotReachHere();
      }
#else
      jint c_lo = right->as_constant_ptr()->as_jint_lo();
      jint c_hi = right->as_constant_ptr()->as_jint_hi();
      switch (code) {
        case lir_add:
          __ addptr(lreg_lo, c_lo);
          __ adcl(lreg_hi, c_hi);
          break;
        case lir_sub:
          __ subptr(lreg_lo, c_lo);
          __ sbbl(lreg_hi, c_hi);
          break;
        default:
          ShouldNotReachHere();
      }
#endif // _LP64

    } else {
      ShouldNotReachHere();
    }

  } else if (left->is_single_xmm()) {
    assert(left == dest, "left and dest must be equal");
    XMMRegister lreg = left->as_xmm_float_reg();

    if (right->is_single_xmm()) {
      XMMRegister rreg = right->as_xmm_float_reg();
      switch (code) {
        case lir_add: __ addss(lreg, rreg);  break;
        case lir_sub: __ subss(lreg, rreg);  break;
        case lir_mul: __ mulss(lreg, rreg);  break;
        case lir_div: __ divss(lreg, rreg);  break;
        default: ShouldNotReachHere();
      }
    } else {
      Address raddr;
      if (right->is_single_stack()) {
        raddr = frame_map()->address_for_slot(right->single_stack_ix());
      } else if (right->is_constant()) {
        // hack for now
        raddr = __ as_Address(InternalAddress(float_constant(right->as_jfloat())));
      } else {
        ShouldNotReachHere();
      }
      switch (code) {
        case lir_add: __ addss(lreg, raddr);  break;
        case lir_sub: __ subss(lreg, raddr);  break;
        case lir_mul: __ mulss(lreg, raddr);  break;
        case lir_div: __ divss(lreg, raddr);  break;
        default: ShouldNotReachHere();
      }
    }

  } else if (left->is_double_xmm()) {
    assert(left == dest, "left and dest must be equal");

    XMMRegister lreg = left->as_xmm_double_reg();
    if (right->is_double_xmm()) {
      XMMRegister rreg = right->as_xmm_double_reg();
      switch (code) {
        case lir_add: __ addsd(lreg, rreg);  break;
        case lir_sub: __ subsd(lreg, rreg);  break;
        case lir_mul: __ mulsd(lreg, rreg);  break;
        case lir_div: __ divsd(lreg, rreg);  break;
        default: ShouldNotReachHere();
      }
    } else {
      Address raddr;
      if (right->is_double_stack()) {
        raddr = frame_map()->address_for_slot(right->double_stack_ix());
      } else if (right->is_constant()) {
        // hack for now
        raddr = __ as_Address(InternalAddress(double_constant(right->as_jdouble())));
      } else {
        ShouldNotReachHere();
      }
      switch (code) {
        case lir_add: __ addsd(lreg, raddr);  break;
        case lir_sub: __ subsd(lreg, raddr);  break;
        case lir_mul: __ mulsd(lreg, raddr);  break;
        case lir_div: __ divsd(lreg, raddr);  break;
        default: ShouldNotReachHere();
      }
    }

#ifndef _LP64
  } else if (left->is_single_fpu()) {
    assert(dest->is_single_fpu(),  "fpu stack allocation required");

    if (right->is_single_fpu()) {
      arith_fpu_implementation(code, left->fpu_regnr(), right->fpu_regnr(), dest->fpu_regnr(), pop_fpu_stack);

    } else {
      assert(left->fpu_regnr() == 0, "left must be on TOS");
      assert(dest->fpu_regnr() == 0, "dest must be on TOS");

      Address raddr;
      if (right->is_single_stack()) {
        raddr = frame_map()->address_for_slot(right->single_stack_ix());
      } else if (right->is_constant()) {
        address const_addr = float_constant(right->as_jfloat());
        assert(const_addr != NULL, "incorrect float/double constant maintainance");
        // hack for now
        raddr = __ as_Address(InternalAddress(const_addr));
      } else {
        ShouldNotReachHere();
      }

      switch (code) {
        case lir_add: __ fadd_s(raddr); break;
        case lir_sub: __ fsub_s(raddr); break;
        case lir_mul: __ fmul_s(raddr); break;
        case lir_div: __ fdiv_s(raddr); break;
        default:      ShouldNotReachHere();
      }
    }

  } else if (left->is_double_fpu()) {
    assert(dest->is_double_fpu(),  "fpu stack allocation required");

    if (code == lir_mul || code == lir_div) {
      // Double values require special handling for strictfp mul/div on x86
      __ fld_x(ExternalAddress(StubRoutines::x86::addr_fpu_subnormal_bias1()));
      __ fmulp(left->fpu_regnrLo() + 1);
    }

    if (right->is_double_fpu()) {
      arith_fpu_implementation(code, left->fpu_regnrLo(), right->fpu_regnrLo(), dest->fpu_regnrLo(), pop_fpu_stack);

    } else {
      assert(left->fpu_regnrLo() == 0, "left must be on TOS");
      assert(dest->fpu_regnrLo() == 0, "dest must be on TOS");

      Address raddr;
      if (right->is_double_stack()) {
        raddr = frame_map()->address_for_slot(right->double_stack_ix());
      } else if (right->is_constant()) {
        // hack for now
        raddr = __ as_Address(InternalAddress(double_constant(right->as_jdouble())));
      } else {
        ShouldNotReachHere();
      }

      switch (code) {
        case lir_add: __ fadd_d(raddr); break;
        case lir_sub: __ fsub_d(raddr); break;
        case lir_mul: __ fmul_d(raddr); break;
        case lir_div: __ fdiv_d(raddr); break;
        default: ShouldNotReachHere();
      }
    }

    if (code == lir_mul || code == lir_div) {
      // Double values require special handling for strictfp mul/div on x86
      __ fld_x(ExternalAddress(StubRoutines::x86::addr_fpu_subnormal_bias2()));
      __ fmulp(dest->fpu_regnrLo() + 1);
    }
#endif // !_LP64

  } else if (left->is_single_stack() || left->is_address()) {
    assert(left == dest, "left and dest must be equal");

    Address laddr;
    if (left->is_single_stack()) {
      laddr = frame_map()->address_for_slot(left->single_stack_ix());
    } else if (left->is_address()) {
      laddr = as_Address(left->as_address_ptr());
    } else {
      ShouldNotReachHere();
    }

    if (right->is_single_cpu()) {
      Register rreg = right->as_register();
      switch (code) {
        case lir_add: __ addl(laddr, rreg); break;
        case lir_sub: __ subl(laddr, rreg); break;
        default:      ShouldNotReachHere();
      }
    } else if (right->is_constant()) {
      jint c = right->as_constant_ptr()->as_jint();
      switch (code) {
        case lir_add: {
          __ incrementl(laddr, c);
          break;
        }
        case lir_sub: {
          __ decrementl(laddr, c);
          break;
        }
        default: ShouldNotReachHere();
      }
    } else {
      ShouldNotReachHere();
    }

  } else {
    ShouldNotReachHere();
  }
}

#ifndef _LP64
void LIR_Assembler::arith_fpu_implementation(LIR_Code code, int left_index, int right_index, int dest_index, bool pop_fpu_stack) {
  assert(pop_fpu_stack  || (left_index     == dest_index || right_index     == dest_index), "invalid LIR");
  assert(!pop_fpu_stack || (left_index - 1 == dest_index || right_index - 1 == dest_index), "invalid LIR");
  assert(left_index == 0 || right_index == 0, "either must be on top of stack");

  bool left_is_tos = (left_index == 0);
  bool dest_is_tos = (dest_index == 0);
  int non_tos_index = (left_is_tos ? right_index : left_index);

  switch (code) {
    case lir_add:
      if (pop_fpu_stack)       __ faddp(non_tos_index);
      else if (dest_is_tos)    __ fadd (non_tos_index);
      else                     __ fadda(non_tos_index);
      break;

    case lir_sub:
      if (left_is_tos) {
        if (pop_fpu_stack)     __ fsubrp(non_tos_index);
        else if (dest_is_tos)  __ fsub  (non_tos_index);
        else                   __ fsubra(non_tos_index);
      } else {
        if (pop_fpu_stack)     __ fsubp (non_tos_index);
        else if (dest_is_tos)  __ fsubr (non_tos_index);
        else                   __ fsuba (non_tos_index);
      }
      break;

    case lir_mul:
      if (pop_fpu_stack)       __ fmulp(non_tos_index);
      else if (dest_is_tos)    __ fmul (non_tos_index);
      else                     __ fmula(non_tos_index);
      break;

    case lir_div:
      if (left_is_tos) {
        if (pop_fpu_stack)     __ fdivrp(non_tos_index);
        else if (dest_is_tos)  __ fdiv  (non_tos_index);
        else                   __ fdivra(non_tos_index);
      } else {
        if (pop_fpu_stack)     __ fdivp (non_tos_index);
        else if (dest_is_tos)  __ fdivr (non_tos_index);
        else                   __ fdiva (non_tos_index);
      }
      break;

    case lir_rem:
      assert(left_is_tos && dest_is_tos && right_index == 1, "must be guaranteed by FPU stack allocation");
      __ fremr(noreg);
      break;

    default:
      ShouldNotReachHere();
  }
}
#endif // _LP64


void LIR_Assembler::intrinsic_op(LIR_Code code, LIR_Opr value, LIR_Opr tmp, LIR_Opr dest, LIR_Op* op) {
  if (value->is_double_xmm()) {
    switch(code) {
      case lir_abs :
        {
#ifdef _LP64
          if (UseAVX > 2 && !VM_Version::supports_avx512vl()) {
            assert(tmp->is_valid(), "need temporary");
            __ vpandn(dest->as_xmm_double_reg(), tmp->as_xmm_double_reg(), value->as_xmm_double_reg(), 2);
          } else
#endif
          {
            if (dest->as_xmm_double_reg() != value->as_xmm_double_reg()) {
              __ movdbl(dest->as_xmm_double_reg(), value->as_xmm_double_reg());
            }
            assert(!tmp->is_valid(), "do not need temporary");
            __ andpd(dest->as_xmm_double_reg(),
                     ExternalAddress((address)double_signmask_pool));
          }
        }
        break;

      case lir_sqrt: __ sqrtsd(dest->as_xmm_double_reg(), value->as_xmm_double_reg()); break;
      // all other intrinsics are not available in the SSE instruction set, so FPU is used
      default      : ShouldNotReachHere();
    }

#ifndef _LP64
  } else if (value->is_double_fpu()) {
    assert(value->fpu_regnrLo() == 0 && dest->fpu_regnrLo() == 0, "both must be on TOS");
    switch(code) {
      case lir_abs   : __ fabs() ; break;
      case lir_sqrt  : __ fsqrt(); break;
      default      : ShouldNotReachHere();
    }
#endif // !_LP64
  } else {
    Unimplemented();
  }
}

void LIR_Assembler::logic_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst) {
  // assert(left->destroys_register(), "check");
  if (left->is_single_cpu()) {
    Register reg = left->as_register();
    if (right->is_constant()) {
      int val = right->as_constant_ptr()->as_jint();
      switch (code) {
        case lir_logic_and: __ andl (reg, val); break;
        case lir_logic_or:  __ orl  (reg, val); break;
        case lir_logic_xor: __ xorl (reg, val); break;
        default: ShouldNotReachHere();
      }
    } else if (right->is_stack()) {
      // added support for stack operands
      Address raddr = frame_map()->address_for_slot(right->single_stack_ix());
      switch (code) {
        case lir_logic_and: __ andl (reg, raddr); break;
        case lir_logic_or:  __ orl  (reg, raddr); break;
        case lir_logic_xor: __ xorl (reg, raddr); break;
        default: ShouldNotReachHere();
      }
    } else {
      Register rright = right->as_register();
      switch (code) {
        case lir_logic_and: __ andptr (reg, rright); break;
        case lir_logic_or : __ orptr  (reg, rright); break;
        case lir_logic_xor: __ xorptr (reg, rright); break;
        default: ShouldNotReachHere();
      }
    }
    move_regs(reg, dst->as_register());
  } else {
    Register l_lo = left->as_register_lo();
    Register l_hi = left->as_register_hi();
    if (right->is_constant()) {
#ifdef _LP64
      __ mov64(rscratch1, right->as_constant_ptr()->as_jlong());
      switch (code) {
        case lir_logic_and:
          __ andq(l_lo, rscratch1);
          break;
        case lir_logic_or:
          __ orq(l_lo, rscratch1);
          break;
        case lir_logic_xor:
          __ xorq(l_lo, rscratch1);
          break;
        default: ShouldNotReachHere();
      }
#else
      int r_lo = right->as_constant_ptr()->as_jint_lo();
      int r_hi = right->as_constant_ptr()->as_jint_hi();
      switch (code) {
        case lir_logic_and:
          __ andl(l_lo, r_lo);
          __ andl(l_hi, r_hi);
          break;
        case lir_logic_or:
          __ orl(l_lo, r_lo);
          __ orl(l_hi, r_hi);
          break;
        case lir_logic_xor:
          __ xorl(l_lo, r_lo);
          __ xorl(l_hi, r_hi);
          break;
        default: ShouldNotReachHere();
      }
#endif // _LP64
    } else {
#ifdef _LP64
      Register r_lo;
      if (is_reference_type(right->type())) {
        r_lo = right->as_register();
      } else {
        r_lo = right->as_register_lo();
      }
#else
      Register r_lo = right->as_register_lo();
      Register r_hi = right->as_register_hi();
      assert(l_lo != r_hi, "overwriting registers");
#endif
      switch (code) {
        case lir_logic_and:
          __ andptr(l_lo, r_lo);
          NOT_LP64(__ andptr(l_hi, r_hi);)
          break;
        case lir_logic_or:
          __ orptr(l_lo, r_lo);
          NOT_LP64(__ orptr(l_hi, r_hi);)
          break;
        case lir_logic_xor:
          __ xorptr(l_lo, r_lo);
          NOT_LP64(__ xorptr(l_hi, r_hi);)
          break;
        default: ShouldNotReachHere();
      }
    }

    Register dst_lo = dst->as_register_lo();
    Register dst_hi = dst->as_register_hi();

#ifdef _LP64
    move_regs(l_lo, dst_lo);
#else
    if (dst_lo == l_hi) {
      assert(dst_hi != l_lo, "overwriting registers");
      move_regs(l_hi, dst_hi);
      move_regs(l_lo, dst_lo);
    } else {
      assert(dst_lo != l_hi, "overwriting registers");
      move_regs(l_lo, dst_lo);
      move_regs(l_hi, dst_hi);
    }
#endif // _LP64
  }
}


// we assume that rax, and rdx can be overwritten
void LIR_Assembler::arithmetic_idiv(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr temp, LIR_Opr result, CodeEmitInfo* info) {

  assert(left->is_single_cpu(),   "left must be register");
  assert(right->is_single_cpu() || right->is_constant(),  "right must be register or constant");
  assert(result->is_single_cpu(), "result must be register");

  //  assert(left->destroys_register(), "check");
  //  assert(right->destroys_register(), "check");

  Register lreg = left->as_register();
  Register dreg = result->as_register();

  if (right->is_constant()) {
    jint divisor = right->as_constant_ptr()->as_jint();
    assert(divisor > 0 && is_power_of_2(divisor), "must be");
    if (code == lir_idiv) {
      assert(lreg == rax, "must be rax,");
      assert(temp->as_register() == rdx, "tmp register must be rdx");
      __ cdql(); // sign extend into rdx:rax
      if (divisor == 2) {
        __ subl(lreg, rdx);
      } else {
        __ andl(rdx, divisor - 1);
        __ addl(lreg, rdx);
      }
      __ sarl(lreg, log2i_exact(divisor));
      move_regs(lreg, dreg);
    } else if (code == lir_irem) {
      Label done;
      __ mov(dreg, lreg);
      __ andl(dreg, 0x80000000 | (divisor - 1));
      __ jcc(Assembler::positive, done);
      __ decrement(dreg);
      __ orl(dreg, ~(divisor - 1));
      __ increment(dreg);
      __ bind(done);
    } else {
      ShouldNotReachHere();
    }
  } else {
    Register rreg = right->as_register();
    assert(lreg == rax, "left register must be rax,");
    assert(rreg != rdx, "right register must not be rdx");
    assert(temp->as_register() == rdx, "tmp register must be rdx");

    move_regs(lreg, rax);

    int idivl_offset = __ corrected_idivl(rreg);
    if (ImplicitDiv0Checks) {
      add_debug_info_for_div0(idivl_offset, info);
    }
    if (code == lir_irem) {
      move_regs(rdx, dreg); // result is in rdx
    } else {
      move_regs(rax, dreg);
    }
  }
}


void LIR_Assembler::comp_op(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Op2* op) {
  if (opr1->is_single_cpu()) {
    Register reg1 = opr1->as_register();
    if (opr2->is_single_cpu()) {
      // cpu register - cpu register
      if (is_reference_type(opr1->type())) {
        __ cmpoop(reg1, opr2->as_register());
      } else {
        assert(!is_reference_type(opr2->type()), "cmp int, oop?");
        __ cmpl(reg1, opr2->as_register());
      }
    } else if (opr2->is_stack()) {
      // cpu register - stack
      if (is_reference_type(opr1->type())) {
        __ cmpoop(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
      } else {
        __ cmpl(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
      }
    } else if (opr2->is_constant()) {
      // cpu register - constant
      LIR_Const* c = opr2->as_constant_ptr();
      if (c->type() == T_INT) {
        __ cmpl(reg1, c->as_jint());
      } else if (c->type() == T_METADATA) {
        // All we need for now is a comparison with NULL for equality.
        assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "oops");
        Metadata* m = c->as_metadata();
        if (m == NULL) {
          __ cmpptr(reg1, (int32_t)0);
        } else {
          ShouldNotReachHere();
        }
      } else if (is_reference_type(c->type())) {
        // In 64bit oops are single register
        jobject o = c->as_jobject();
        if (o == NULL) {
          __ cmpptr(reg1, (int32_t)NULL_WORD);
        } else {
          __ cmpoop(reg1, o);
        }
      } else {
        fatal("unexpected type: %s", basictype_to_str(c->type()));
      }
      // cpu register - address
    } else if (opr2->is_address()) {
      if (op->info() != NULL) {
        add_debug_info_for_null_check_here(op->info());
      }
      __ cmpl(reg1, as_Address(opr2->as_address_ptr()));
    } else {
      ShouldNotReachHere();
    }

  } else if(opr1->is_double_cpu()) {
    Register xlo = opr1->as_register_lo();
    Register xhi = opr1->as_register_hi();
    if (opr2->is_double_cpu()) {
#ifdef _LP64
      __ cmpptr(xlo, opr2->as_register_lo());
#else
      // cpu register - cpu register
      Register ylo = opr2->as_register_lo();
      Register yhi = opr2->as_register_hi();
      __ subl(xlo, ylo);
      __ sbbl(xhi, yhi);
      if (condition == lir_cond_equal || condition == lir_cond_notEqual) {
        __ orl(xhi, xlo);
      }
#endif // _LP64
    } else if (opr2->is_constant()) {
      // cpu register - constant 0
      assert(opr2->as_jlong() == (jlong)0, "only handles zero");
#ifdef _LP64
      __ cmpptr(xlo, (int32_t)opr2->as_jlong());
#else
      assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "only handles equals case");
      __ orl(xhi, xlo);
#endif // _LP64
    } else {
      ShouldNotReachHere();
    }

  } else if (opr1->is_single_xmm()) {
    XMMRegister reg1 = opr1->as_xmm_float_reg();
    if (opr2->is_single_xmm()) {
      // xmm register - xmm register
      __ ucomiss(reg1, opr2->as_xmm_float_reg());
    } else if (opr2->is_stack()) {
      // xmm register - stack
      __ ucomiss(reg1, frame_map()->address_for_slot(opr2->single_stack_ix()));
    } else if (opr2->is_constant()) {
      // xmm register - constant
      __ ucomiss(reg1, InternalAddress(float_constant(opr2->as_jfloat())));
    } else if (opr2->is_address()) {
      // xmm register - address
      if (op->info() != NULL) {
        add_debug_info_for_null_check_here(op->info());
      }
      __ ucomiss(reg1, as_Address(opr2->as_address_ptr()));
    } else {
      ShouldNotReachHere();
    }

  } else if (opr1->is_double_xmm()) {
    XMMRegister reg1 = opr1->as_xmm_double_reg();
    if (opr2->is_double_xmm()) {
      // xmm register - xmm register
      __ ucomisd(reg1, opr2->as_xmm_double_reg());
    } else if (opr2->is_stack()) {
      // xmm register - stack
      __ ucomisd(reg1, frame_map()->address_for_slot(opr2->double_stack_ix()));
    } else if (opr2->is_constant()) {
      // xmm register - constant
      __ ucomisd(reg1, InternalAddress(double_constant(opr2->as_jdouble())));
    } else if (opr2->is_address()) {
      // xmm register - address
      if (op->info() != NULL) {
        add_debug_info_for_null_check_here(op->info());
      }
      __ ucomisd(reg1, as_Address(opr2->pointer()->as_address()));
    } else {
      ShouldNotReachHere();
    }

#ifndef _LP64
  } else if(opr1->is_single_fpu() || opr1->is_double_fpu()) {
    assert(opr1->is_fpu_register() && opr1->fpu() == 0, "currently left-hand side must be on TOS (relax this restriction)");
    assert(opr2->is_fpu_register(), "both must be registers");
    __ fcmp(noreg, opr2->fpu(), op->fpu_pop_count() > 0, op->fpu_pop_count() > 1);
#endif // LP64

  } else if (opr1->is_address() && opr2->is_constant()) {
    LIR_Const* c = opr2->as_constant_ptr();
#ifdef _LP64
    if (is_reference_type(c->type())) {
      assert(condition == lir_cond_equal || condition == lir_cond_notEqual, "need to reverse");
      __ movoop(rscratch1, c->as_jobject());
    }
#endif // LP64
    if (op->info() != NULL) {
      add_debug_info_for_null_check_here(op->info());
    }
    // special case: address - constant
    LIR_Address* addr = opr1->as_address_ptr();
    if (c->type() == T_INT) {
      __ cmpl(as_Address(addr), c->as_jint());
    } else if (is_reference_type(c->type())) {
#ifdef _LP64
      // %%% Make this explode if addr isn't reachable until we figure out a
      // better strategy by giving noreg as the temp for as_Address
      __ cmpoop(rscratch1, as_Address(addr, noreg));
#else
      __ cmpoop(as_Address(addr), c->as_jobject());
#endif // _LP64
    } else {
      ShouldNotReachHere();
    }

  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::comp_fl2i(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst, LIR_Op2* op) {
  if (code == lir_cmp_fd2i || code == lir_ucmp_fd2i) {
    if (left->is_single_xmm()) {
      assert(right->is_single_xmm(), "must match");
      __ cmpss2int(left->as_xmm_float_reg(), right->as_xmm_float_reg(), dst->as_register(), code == lir_ucmp_fd2i);
    } else if (left->is_double_xmm()) {
      assert(right->is_double_xmm(), "must match");
      __ cmpsd2int(left->as_xmm_double_reg(), right->as_xmm_double_reg(), dst->as_register(), code == lir_ucmp_fd2i);

    } else {
#ifdef _LP64
      ShouldNotReachHere();
#else
      assert(left->is_single_fpu() || left->is_double_fpu(), "must be");
      assert(right->is_single_fpu() || right->is_double_fpu(), "must match");

      assert(left->fpu() == 0, "left must be on TOS");
      __ fcmp2int(dst->as_register(), code == lir_ucmp_fd2i, right->fpu(),
                  op->fpu_pop_count() > 0, op->fpu_pop_count() > 1);
#endif // LP64
    }
  } else {
    assert(code == lir_cmp_l2i, "check");
#ifdef _LP64
    Label done;
    Register dest = dst->as_register();
    __ cmpptr(left->as_register_lo(), right->as_register_lo());
    __ movl(dest, -1);
    __ jccb(Assembler::less, done);
    __ set_byte_if_not_zero(dest);
    __ movzbl(dest, dest);
    __ bind(done);
#else
    __ lcmp2int(left->as_register_hi(),
                left->as_register_lo(),
                right->as_register_hi(),
                right->as_register_lo());
    move_regs(left->as_register_hi(), dst->as_register());
#endif // _LP64
  }
}


void LIR_Assembler::align_call(LIR_Code code) {
  // make sure that the displacement word of the call ends up word aligned
  int offset = __ offset();
  switch (code) {
  case lir_static_call:
  case lir_optvirtual_call:
  case lir_dynamic_call:
    offset += NativeCall::displacement_offset;
    break;
  case lir_icvirtual_call:
    offset += NativeCall::displacement_offset + NativeMovConstReg::instruction_size;
    break;
  default: ShouldNotReachHere();
  }
  __ align(BytesPerWord, offset);
}


void LIR_Assembler::call(LIR_OpJavaCall* op, relocInfo::relocType rtype) {
  assert((__ offset() + NativeCall::displacement_offset) % BytesPerWord == 0,
         "must be aligned");
  __ call(AddressLiteral(op->addr(), rtype));
  add_call_info(code_offset(), op->info());
}


void LIR_Assembler::ic_call(LIR_OpJavaCall* op) {
  __ ic_call(op->addr());
  add_call_info(code_offset(), op->info());
  assert((__ offset() - NativeCall::instruction_size + NativeCall::displacement_offset) % BytesPerWord == 0,
         "must be aligned");
}


void LIR_Assembler::emit_static_call_stub() {
  address call_pc = __ pc();
  address stub = __ start_a_stub(call_stub_size());
  if (stub == NULL) {
    bailout("static call stub overflow");
    return;
  }

  int start = __ offset();

  // make sure that the displacement word of the call ends up word aligned
  __ align(BytesPerWord, __ offset() + NativeMovConstReg::instruction_size + NativeCall::displacement_offset);
  __ relocate(static_stub_Relocation::spec(call_pc));
  __ mov_metadata(rbx, (Metadata*)NULL);
  // must be set to -1 at code generation time
  assert(((__ offset() + 1) % BytesPerWord) == 0, "must be aligned");
  // On 64bit this will die since it will take a movq & jmp, must be only a jmp
  __ jump(RuntimeAddress(__ pc()));

  assert(__ offset() - start <= call_stub_size(), "stub too big");
  __ end_a_stub();
}


void LIR_Assembler::throw_op(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info) {
  assert(exceptionOop->as_register() == rax, "must match");
  assert(exceptionPC->as_register() == rdx, "must match");

  // exception object is not added to oop map by LinearScan
  // (LinearScan assumes that no oops are in fixed registers)
  info->add_register_oop(exceptionOop);
  Runtime1::StubID unwind_id;

  // get current pc information
  // pc is only needed if the method has an exception handler, the unwind code does not need it.
  int pc_for_athrow_offset = __ offset();
  InternalAddress pc_for_athrow(__ pc());
  __ lea(exceptionPC->as_register(), pc_for_athrow);
  add_call_info(pc_for_athrow_offset, info); // for exception handler

  __ verify_not_null_oop(rax);
  // search an exception handler (rax: exception oop, rdx: throwing pc)
  if (compilation()->has_fpu_code()) {
    unwind_id = Runtime1::handle_exception_id;
  } else {
    unwind_id = Runtime1::handle_exception_nofpu_id;
  }
  __ call(RuntimeAddress(Runtime1::entry_for(unwind_id)));

  // enough room for two byte trap
  __ nop();
}


void LIR_Assembler::unwind_op(LIR_Opr exceptionOop) {
  assert(exceptionOop->as_register() == rax, "must match");

  __ jmp(_unwind_handler_entry);
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, LIR_Opr count, LIR_Opr dest, LIR_Opr tmp) {

  // optimized version for linear scan:
  // * count must be already in ECX (guaranteed by LinearScan)
  // * left and dest must be equal
  // * tmp must be unused
  assert(count->as_register() == SHIFT_count, "count must be in ECX");
  assert(left == dest, "left and dest must be equal");
  assert(tmp->is_illegal(), "wasting a register if tmp is allocated");

  if (left->is_single_cpu()) {
    Register value = left->as_register();
    assert(value != SHIFT_count, "left cannot be ECX");

    switch (code) {
      case lir_shl:  __ shll(value); break;
      case lir_shr:  __ sarl(value); break;
      case lir_ushr: __ shrl(value); break;
      default: ShouldNotReachHere();
    }
  } else if (left->is_double_cpu()) {
    Register lo = left->as_register_lo();
    Register hi = left->as_register_hi();
    assert(lo != SHIFT_count && hi != SHIFT_count, "left cannot be ECX");
#ifdef _LP64
    switch (code) {
      case lir_shl:  __ shlptr(lo);        break;
      case lir_shr:  __ sarptr(lo);        break;
      case lir_ushr: __ shrptr(lo);        break;
      default: ShouldNotReachHere();
    }
#else

    switch (code) {
      case lir_shl:  __ lshl(hi, lo);        break;
      case lir_shr:  __ lshr(hi, lo, true);  break;
      case lir_ushr: __ lshr(hi, lo, false); break;
      default: ShouldNotReachHere();
    }
#endif // LP64
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, jint count, LIR_Opr dest) {
  if (dest->is_single_cpu()) {
    // first move left into dest so that left is not destroyed by the shift
    Register value = dest->as_register();
    count = count & 0x1F; // Java spec

    move_regs(left->as_register(), value);
    switch (code) {
      case lir_shl:  __ shll(value, count); break;
      case lir_shr:  __ sarl(value, count); break;
      case lir_ushr: __ shrl(value, count); break;
      default: ShouldNotReachHere();
    }
  } else if (dest->is_double_cpu()) {
#ifndef _LP64
    Unimplemented();
#else
    // first move left into dest so that left is not destroyed by the shift
    Register value = dest->as_register_lo();
    count = count & 0x1F; // Java spec

    move_regs(left->as_register_lo(), value);
    switch (code) {
      case lir_shl:  __ shlptr(value, count); break;
      case lir_shr:  __ sarptr(value, count); break;
      case lir_ushr: __ shrptr(value, count); break;
      default: ShouldNotReachHere();
    }
#endif // _LP64
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::store_parameter(Register r, int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ movptr (Address(rsp, offset_from_rsp_in_bytes), r);
}


void LIR_Assembler::store_parameter(jint c,     int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ movptr (Address(rsp, offset_from_rsp_in_bytes), c);
}


void LIR_Assembler::store_parameter(jobject o,  int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ movoop (Address(rsp, offset_from_rsp_in_bytes), o);
}


void LIR_Assembler::store_parameter(Metadata* m,  int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ mov_metadata(Address(rsp, offset_from_rsp_in_bytes), m);
}


// This code replaces a call to arraycopy; no exception may
// be thrown in this code, they must be thrown in the System.arraycopy
// activation frame; we could save some checks if this would not be the case
void LIR_Assembler::emit_arraycopy(LIR_OpArrayCopy* op) {
  ciArrayKlass* default_type = op->expected_type();
  Register src = op->src()->as_register();
  Register dst = op->dst()->as_register();
  Register src_pos = op->src_pos()->as_register();
  Register dst_pos = op->dst_pos()->as_register();
  Register length  = op->length()->as_register();
  Register tmp = op->tmp()->as_register();
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);

  CodeStub* stub = op->stub();
  int flags = op->flags();
  BasicType basic_type = default_type != NULL ? default_type->element_type()->basic_type() : T_ILLEGAL;
  if (is_reference_type(basic_type)) basic_type = T_OBJECT;

  // if we don't know anything, just go through the generic arraycopy
  if (default_type == NULL) {
    // save outgoing arguments on stack in case call to System.arraycopy is needed
    // HACK ALERT. This code used to push the parameters in a hardwired fashion
    // for interpreter calling conventions. Now we have to do it in new style conventions.
    // For the moment until C1 gets the new register allocator I just force all the
    // args to the right place (except the register args) and then on the back side
    // reload the register args properly if we go slow path. Yuck

    // These are proper for the calling convention
    store_parameter(length, 2);
    store_parameter(dst_pos, 1);
    store_parameter(dst, 0);

    // these are just temporary placements until we need to reload
    store_parameter(src_pos, 3);
    store_parameter(src, 4);
    NOT_LP64(assert(src == rcx && src_pos == rdx, "mismatch in calling convention");)

    address copyfunc_addr = StubRoutines::generic_arraycopy();
    assert(copyfunc_addr != NULL, "generic arraycopy stub required");

    // pass arguments: may push as this is not a safepoint; SP must be fix at each safepoint
#ifdef _LP64
    // The arguments are in java calling convention so we can trivially shift them to C
    // convention
    assert_different_registers(c_rarg0, j_rarg1, j_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg0, j_rarg0);
    assert_different_registers(c_rarg1, j_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg1, j_rarg1);
    assert_different_registers(c_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg2, j_rarg2);
    assert_different_registers(c_rarg3, j_rarg4);
    __ mov(c_rarg3, j_rarg3);
#ifdef _WIN64
    // Allocate abi space for args but be sure to keep stack aligned
    __ subptr(rsp, 6*wordSize);
    store_parameter(j_rarg4, 4);
#ifndef PRODUCT
    if (PrintC1Statistics) {
      __ incrementl(ExternalAddress((address)&Runtime1::_generic_arraycopystub_cnt));
    }
#endif
    __ call(RuntimeAddress(copyfunc_addr));
    __ addptr(rsp, 6*wordSize);
#else
    __ mov(c_rarg4, j_rarg4);
#ifndef PRODUCT
    if (PrintC1Statistics) {
      __ incrementl(ExternalAddress((address)&Runtime1::_generic_arraycopystub_cnt));
    }
#endif
    __ call(RuntimeAddress(copyfunc_addr));
#endif // _WIN64
#else
    __ push(length);
    __ push(dst_pos);
    __ push(dst);
    __ push(src_pos);
    __ push(src);

#ifndef PRODUCT
    if (PrintC1Statistics) {
      __ incrementl(ExternalAddress((address)&Runtime1::_generic_arraycopystub_cnt));
    }
#endif
    __ call_VM_leaf(copyfunc_addr, 5); // removes pushed parameter from the stack

#endif // _LP64

    __ cmpl(rax, 0);
    __ jcc(Assembler::equal, *stub->continuation());

    __ mov(tmp, rax);
    __ xorl(tmp, -1);

    // Reload values from the stack so they are where the stub
    // expects them.
    __ movptr   (dst,     Address(rsp, 0*BytesPerWord));
    __ movptr   (dst_pos, Address(rsp, 1*BytesPerWord));
    __ movptr   (length,  Address(rsp, 2*BytesPerWord));
    __ movptr   (src_pos, Address(rsp, 3*BytesPerWord));
    __ movptr   (src,     Address(rsp, 4*BytesPerWord));

    __ subl(length, tmp);
    __ addl(src_pos, tmp);
    __ addl(dst_pos, tmp);
    __ jmp(*stub->entry());

    __ bind(*stub->continuation());
    return;
  }

  assert(default_type != NULL && default_type->is_array_klass() && default_type->is_loaded(), "must be true at this point");

  int elem_size = type2aelembytes(basic_type);
  Address::ScaleFactor scale;

  switch (elem_size) {
    case 1 :
      scale = Address::times_1;
      break;
    case 2 :
      scale = Address::times_2;
      break;
    case 4 :
      scale = Address::times_4;
      break;
    case 8 :
      scale = Address::times_8;
      break;
    default:
      scale = Address::no_scale;
      ShouldNotReachHere();
  }

  Address src_length_addr = Address(src, arrayOopDesc::length_offset_in_bytes());
  Address dst_length_addr = Address(dst, arrayOopDesc::length_offset_in_bytes());
  Address src_klass_addr = Address(src, oopDesc::klass_offset_in_bytes());
  Address dst_klass_addr = Address(dst, oopDesc::klass_offset_in_bytes());

  // length and pos's are all sign extended at this point on 64bit

  // test for NULL
  if (flags & LIR_OpArrayCopy::src_null_check) {
    __ testptr(src, src);
    __ jcc(Assembler::zero, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_null_check) {
    __ testptr(dst, dst);
    __ jcc(Assembler::zero, *stub->entry());
  }

  // If the compiler was not able to prove that exact type of the source or the destination
  // of the arraycopy is an array type, check at runtime if the source or the destination is
  // an instance type.
  if (flags & LIR_OpArrayCopy::type_check) {
    if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
      __ load_klass(tmp, dst, tmp_load_klass);
      __ cmpl(Address(tmp, in_bytes(Klass::layout_helper_offset())), Klass::_lh_neutral_value);
      __ jcc(Assembler::greaterEqual, *stub->entry());
    }

    if (!(flags & LIR_OpArrayCopy::src_objarray)) {
      __ load_klass(tmp, src, tmp_load_klass);
      __ cmpl(Address(tmp, in_bytes(Klass::layout_helper_offset())), Klass::_lh_neutral_value);
      __ jcc(Assembler::greaterEqual, *stub->entry());
    }
  }

  // check if negative
  if (flags & LIR_OpArrayCopy::src_pos_positive_check) {
    __ testl(src_pos, src_pos);
    __ jcc(Assembler::less, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_pos_positive_check) {
    __ testl(dst_pos, dst_pos);
    __ jcc(Assembler::less, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::src_range_check) {
    __ lea(tmp, Address(src_pos, length, Address::times_1, 0));
    __ cmpl(tmp, src_length_addr);
    __ jcc(Assembler::above, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_range_check) {
    __ lea(tmp, Address(dst_pos, length, Address::times_1, 0));
    __ cmpl(tmp, dst_length_addr);
    __ jcc(Assembler::above, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::length_positive_check) {
    __ testl(length, length);
    __ jcc(Assembler::less, *stub->entry());
  }

#ifdef _LP64
  __ movl2ptr(src_pos, src_pos); //higher 32bits must be null
  __ movl2ptr(dst_pos, dst_pos); //higher 32bits must be null
#endif

  if (flags & LIR_OpArrayCopy::type_check) {
    // We don't know the array types are compatible
    if (basic_type != T_OBJECT) {
      // Simple test for basic type arrays
      if (UseCompressedClassPointers) {
        __ movl(tmp, src_klass_addr);
        __ cmpl(tmp, dst_klass_addr);
      } else {
        __ movptr(tmp, src_klass_addr);
        __ cmpptr(tmp, dst_klass_addr);
      }
      __ jcc(Assembler::notEqual, *stub->entry());
    } else {
      // For object arrays, if src is a sub class of dst then we can
      // safely do the copy.
      Label cont, slow;

      __ push(src);
      __ push(dst);

      __ load_klass(src, src, tmp_load_klass);
      __ load_klass(dst, dst, tmp_load_klass);

      __ check_klass_subtype_fast_path(src, dst, tmp, &cont, &slow, NULL);

      __ push(src);
      __ push(dst);
      __ call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
      __ pop(dst);
      __ pop(src);

      __ cmpl(src, 0);
      __ jcc(Assembler::notEqual, cont);

      __ bind(slow);
      __ pop(dst);
      __ pop(src);

      address copyfunc_addr = StubRoutines::checkcast_arraycopy();
      if (copyfunc_addr != NULL) { // use stub if available
        // src is not a sub class of dst so we have to do a
        // per-element check.

        int mask = LIR_OpArrayCopy::src_objarray|LIR_OpArrayCopy::dst_objarray;
        if ((flags & mask) != mask) {
          // Check that at least both of them object arrays.
          assert(flags & mask, "one of the two should be known to be an object array");

          if (!(flags & LIR_OpArrayCopy::src_objarray)) {
            __ load_klass(tmp, src, tmp_load_klass);
          } else if (!(flags & LIR_OpArrayCopy::dst_objarray)) {
            __ load_klass(tmp, dst, tmp_load_klass);
          }
          int lh_offset = in_bytes(Klass::layout_helper_offset());
          Address klass_lh_addr(tmp, lh_offset);
          jint objArray_lh = Klass::array_layout_helper(T_OBJECT);
          __ cmpl(klass_lh_addr, objArray_lh);
          __ jcc(Assembler::notEqual, *stub->entry());
        }

       // Spill because stubs can use any register they like and it's
       // easier to restore just those that we care about.
       store_parameter(dst, 0);
       store_parameter(dst_pos, 1);
       store_parameter(length, 2);
       store_parameter(src_pos, 3);
       store_parameter(src, 4);

#ifndef _LP64
        __ movptr(tmp, dst_klass_addr);
        __ movptr(tmp, Address(tmp, ObjArrayKlass::element_klass_offset()));
        __ push(tmp);
        __ movl(tmp, Address(tmp, Klass::super_check_offset_offset()));
        __ push(tmp);
        __ push(length);
        __ lea(tmp, Address(dst, dst_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
        __ push(tmp);
        __ lea(tmp, Address(src, src_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
        __ push(tmp);

        __ call_VM_leaf(copyfunc_addr, 5);
#else
        __ movl2ptr(length, length); //higher 32bits must be null

        __ lea(c_rarg0, Address(src, src_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
        assert_different_registers(c_rarg0, dst, dst_pos, length);
        __ lea(c_rarg1, Address(dst, dst_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
        assert_different_registers(c_rarg1, dst, length);

        __ mov(c_rarg2, length);
        assert_different_registers(c_rarg2, dst);

#ifdef _WIN64
        // Allocate abi space for args but be sure to keep stack aligned
        __ subptr(rsp, 6*wordSize);
        __ load_klass(c_rarg3, dst, tmp_load_klass);
        __ movptr(c_rarg3, Address(c_rarg3, ObjArrayKlass::element_klass_offset()));
        store_parameter(c_rarg3, 4);
        __ movl(c_rarg3, Address(c_rarg3, Klass::super_check_offset_offset()));
        __ call(RuntimeAddress(copyfunc_addr));
        __ addptr(rsp, 6*wordSize);
#else
        __ load_klass(c_rarg4, dst, tmp_load_klass);
        __ movptr(c_rarg4, Address(c_rarg4, ObjArrayKlass::element_klass_offset()));
        __ movl(c_rarg3, Address(c_rarg4, Klass::super_check_offset_offset()));
        __ call(RuntimeAddress(copyfunc_addr));
#endif

#endif

#ifndef PRODUCT
        if (PrintC1Statistics) {
          Label failed;
          __ testl(rax, rax);
          __ jcc(Assembler::notZero, failed);
          __ incrementl(ExternalAddress((address)&Runtime1::_arraycopy_checkcast_cnt));
          __ bind(failed);
        }
#endif

        __ testl(rax, rax);
        __ jcc(Assembler::zero, *stub->continuation());

#ifndef PRODUCT
        if (PrintC1Statistics) {
          __ incrementl(ExternalAddress((address)&Runtime1::_arraycopy_checkcast_attempt_cnt));
        }
#endif

        __ mov(tmp, rax);

        __ xorl(tmp, -1);

        // Restore previously spilled arguments
        __ movptr   (dst,     Address(rsp, 0*BytesPerWord));
        __ movptr   (dst_pos, Address(rsp, 1*BytesPerWord));
        __ movptr   (length,  Address(rsp, 2*BytesPerWord));
        __ movptr   (src_pos, Address(rsp, 3*BytesPerWord));
        __ movptr   (src,     Address(rsp, 4*BytesPerWord));


        __ subl(length, tmp);
        __ addl(src_pos, tmp);
        __ addl(dst_pos, tmp);
      }

      __ jmp(*stub->entry());

      __ bind(cont);
      __ pop(dst);
      __ pop(src);
    }
  }

#ifdef ASSERT
  if (basic_type != T_OBJECT || !(flags & LIR_OpArrayCopy::type_check)) {
    // Sanity check the known type with the incoming class.  For the
    // primitive case the types must match exactly with src.klass and
    // dst.klass each exactly matching the default type.  For the
    // object array case, if no type check is needed then either the
    // dst type is exactly the expected type and the src type is a
    // subtype which we can't check or src is the same array as dst
    // but not necessarily exactly of type default_type.
    Label known_ok, halt;
    __ mov_metadata(tmp, default_type->constant_encoding());
#ifdef _LP64
    if (UseCompressedClassPointers) {
      __ encode_klass_not_null(tmp, rscratch1);
    }
#endif

    if (basic_type != T_OBJECT) {

      if (UseCompressedClassPointers)          __ cmpl(tmp, dst_klass_addr);
      else                   __ cmpptr(tmp, dst_klass_addr);
      __ jcc(Assembler::notEqual, halt);
      if (UseCompressedClassPointers)          __ cmpl(tmp, src_klass_addr);
      else                   __ cmpptr(tmp, src_klass_addr);
      __ jcc(Assembler::equal, known_ok);
    } else {
      if (UseCompressedClassPointers)          __ cmpl(tmp, dst_klass_addr);
      else                   __ cmpptr(tmp, dst_klass_addr);
      __ jcc(Assembler::equal, known_ok);
      __ cmpptr(src, dst);
      __ jcc(Assembler::equal, known_ok);
    }
    __ bind(halt);
    __ stop("incorrect type information in arraycopy");
    __ bind(known_ok);
  }
#endif

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ incrementl(ExternalAddress(Runtime1::arraycopy_count_address(basic_type)));
  }
#endif

#ifdef _LP64
  assert_different_registers(c_rarg0, dst, dst_pos, length);
  __ lea(c_rarg0, Address(src, src_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
  assert_different_registers(c_rarg1, length);
  __ lea(c_rarg1, Address(dst, dst_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
  __ mov(c_rarg2, length);

#else
  __ lea(tmp, Address(src, src_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
  store_parameter(tmp, 0);
  __ lea(tmp, Address(dst, dst_pos, scale, arrayOopDesc::base_offset_in_bytes(basic_type)));
  store_parameter(tmp, 1);
  store_parameter(length, 2);
#endif // _LP64

  bool disjoint = (flags & LIR_OpArrayCopy::overlapping) == 0;
  bool aligned = (flags & LIR_OpArrayCopy::unaligned) == 0;
  const char *name;
  address entry = StubRoutines::select_arraycopy_function(basic_type, aligned, disjoint, name, false);
  __ call_VM_leaf(entry, 0);

  __ bind(*stub->continuation());
}

void LIR_Assembler::emit_updatecrc32(LIR_OpUpdateCRC32* op) {
  assert(op->crc()->is_single_cpu(),  "crc must be register");
  assert(op->val()->is_single_cpu(),  "byte value must be register");
  assert(op->result_opr()->is_single_cpu(), "result must be register");
  Register crc = op->crc()->as_register();
  Register val = op->val()->as_register();
  Register res = op->result_opr()->as_register();

  assert_different_registers(val, crc, res);

  __ lea(res, ExternalAddress(StubRoutines::crc_table_addr()));
  __ notl(crc); // ~crc
  __ update_byte_crc32(crc, val, res);
  __ notl(crc); // ~crc
  __ mov(res, crc);
}

void LIR_Assembler::emit_lock(LIR_OpLock* op) {
  Register obj = op->obj_opr()->as_register();  // may not be an oop
  Register hdr = op->hdr_opr()->as_register();
  Register lock = op->lock_opr()->as_register();
  if (!UseFastLocking) {
    __ jmp(*op->stub()->entry());
  } else if (op->code() == lir_lock) {
    assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
    // add debug info for NullPointerException only if one is possible
    int null_check_offset = __ lock_object(hdr, obj, lock, *op->stub()->entry());
    if (op->info() != NULL) {
      add_debug_info_for_null_check(null_check_offset, op->info());
    }
    // done
  } else if (op->code() == lir_unlock) {
    assert(BasicLock::displaced_header_offset_in_bytes() == 0, "lock_reg must point to the displaced header");
    __ unlock_object(hdr, obj, lock, *op->stub()->entry());
  } else {
    Unimplemented();
  }
  __ bind(*op->stub()->continuation());
}


void LIR_Assembler::emit_profile_call(LIR_OpProfileCall* op) {
  ciMethod* method = op->profiled_method();
  int bci          = op->profiled_bci();
  ciMethod* callee = op->profiled_callee();
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);

  // Update counter for all call types
  ciMethodData* md = method->method_data_or_null();
  assert(md != NULL, "Sanity");
  ciProfileData* data = md->bci_to_data(bci);
  assert(data != NULL && data->is_CounterData(), "need CounterData for calls");
  assert(op->mdo()->is_single_cpu(),  "mdo must be allocated");
  Register mdo  = op->mdo()->as_register();
  __ mov_metadata(mdo, md->constant_encoding());
  Address counter_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()));
  // Perform additional virtual call profiling for invokevirtual and
  // invokeinterface bytecodes
  if (op->should_profile_receiver_type()) {
    assert(op->recv()->is_single_cpu(), "recv must be allocated");
    Register recv = op->recv()->as_register();
    assert_different_registers(mdo, recv);
    assert(data->is_VirtualCallData(), "need VirtualCallData for virtual calls");
    ciKlass* known_klass = op->known_holder();
    if (C1OptimizeVirtualCallProfiling && known_klass != NULL) {
      // We know the type that will be seen at this call site; we can
      // statically update the MethodData* rather than needing to do
      // dynamic tests on the receiver type

      // NOTE: we should probably put a lock around this search to
      // avoid collisions by concurrent compilations
      ciVirtualCallData* vc_data = (ciVirtualCallData*) data;
      uint i;
      for (i = 0; i < VirtualCallData::row_limit(); i++) {
        ciKlass* receiver = vc_data->receiver(i);
        if (known_klass->equals(receiver)) {
          Address data_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)));
          __ addptr(data_addr, DataLayout::counter_increment);
          return;
        }
      }

      // Receiver type not found in profile data; select an empty slot

      // Note that this is less efficient than it should be because it
      // always does a write to the receiver part of the
      // VirtualCallData rather than just the first time
      for (i = 0; i < VirtualCallData::row_limit(); i++) {
        ciKlass* receiver = vc_data->receiver(i);
        if (receiver == NULL) {
          Address recv_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_offset(i)));
          __ mov_metadata(recv_addr, known_klass->constant_encoding());
          Address data_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)));
          __ addptr(data_addr, DataLayout::counter_increment);
          return;
        }
      }
    } else {
      __ load_klass(recv, recv, tmp_load_klass);
      Label update_done;
      type_profile_helper(mdo, md, data, recv, &update_done);
      // Receiver did not match any saved receiver and there is no empty row for it.
      // Increment total counter to indicate polymorphic case.
      __ addptr(counter_addr, DataLayout::counter_increment);

      __ bind(update_done);
    }
  } else {
    // Static call
    __ addptr(counter_addr, DataLayout::counter_increment);
  }
}

void LIR_Assembler::emit_profile_type(LIR_OpProfileType* op) {
  Register obj = op->obj()->as_register();
  Register tmp = op->tmp()->as_pointer_register();
  Register tmp_load_klass = LP64_ONLY(rscratch1) NOT_LP64(noreg);
  Address mdo_addr = as_Address(op->mdp()->as_address_ptr());
  ciKlass* exact_klass = op->exact_klass();
  intptr_t current_klass = op->current_klass();
  bool not_null = op->not_null();
  bool no_conflict = op->no_conflict();

  Label update, next, none;

  bool do_null = !not_null;
  bool exact_klass_set = exact_klass != NULL && ciTypeEntries::valid_ciklass(current_klass) == exact_klass;
  bool do_update = !TypeEntries::is_type_unknown(current_klass) && !exact_klass_set;

  assert(do_null || do_update, "why are we here?");
  assert(!TypeEntries::was_null_seen(current_klass) || do_update, "why are we here?");

  __ verify_oop(obj);

  if (tmp != obj) {
    __ mov(tmp, obj);
  }
  if (do_null) {
    __ testptr(tmp, tmp);
    __ jccb(Assembler::notZero, update);
    if (!TypeEntries::was_null_seen(current_klass)) {
      __ orptr(mdo_addr, TypeEntries::null_seen);
    }
    if (do_update) {
#ifndef ASSERT
      __ jmpb(next);
    }
#else
      __ jmp(next);
    }
  } else {
    __ testptr(tmp, tmp);
    __ jcc(Assembler::notZero, update);
    __ stop("unexpect null obj");
#endif
  }

  __ bind(update);

  if (do_update) {
#ifdef ASSERT
    if (exact_klass != NULL) {
      Label ok;
      __ load_klass(tmp, tmp, tmp_load_klass);
      __ push(tmp);
      __ mov_metadata(tmp, exact_klass->constant_encoding());
      __ cmpptr(tmp, Address(rsp, 0));
      __ jcc(Assembler::equal, ok);
      __ stop("exact klass and actual klass differ");
      __ bind(ok);
      __ pop(tmp);
    }
#endif
    if (!no_conflict) {
      if (exact_klass == NULL || TypeEntries::is_type_none(current_klass)) {
        if (exact_klass != NULL) {
          __ mov_metadata(tmp, exact_klass->constant_encoding());
        } else {
          __ load_klass(tmp, tmp, tmp_load_klass);
        }

        __ xorptr(tmp, mdo_addr);
        __ testptr(tmp, TypeEntries::type_klass_mask);
        // klass seen before, nothing to do. The unknown bit may have been
        // set already but no need to check.
        __ jccb(Assembler::zero, next);

        __ testptr(tmp, TypeEntries::type_unknown);
        __ jccb(Assembler::notZero, next); // already unknown. Nothing to do anymore.

        if (TypeEntries::is_type_none(current_klass)) {
          __ cmpptr(mdo_addr, 0);
          __ jccb(Assembler::equal, none);
          __ cmpptr(mdo_addr, TypeEntries::null_seen);
          __ jccb(Assembler::equal, none);
          // There is a chance that the checks above (re-reading profiling
          // data from memory) fail if another thread has just set the
          // profiling to this obj's klass
          __ xorptr(tmp, mdo_addr);
          __ testptr(tmp, TypeEntries::type_klass_mask);
          __ jccb(Assembler::zero, next);
        }
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "conflict only");

        __ movptr(tmp, mdo_addr);
        __ testptr(tmp, TypeEntries::type_unknown);
        __ jccb(Assembler::notZero, next); // already unknown. Nothing to do anymore.
      }

      // different than before. Cannot keep accurate profile.
      __ orptr(mdo_addr, TypeEntries::type_unknown);

      if (TypeEntries::is_type_none(current_klass)) {
        __ jmpb(next);

        __ bind(none);
        // first time here. Set profile type.
        __ movptr(mdo_addr, tmp);
      }
    } else {
      // There's a single possible klass at this profile point
      assert(exact_klass != NULL, "should be");
      if (TypeEntries::is_type_none(current_klass)) {
        __ mov_metadata(tmp, exact_klass->constant_encoding());
        __ xorptr(tmp, mdo_addr);
        __ testptr(tmp, TypeEntries::type_klass_mask);
#ifdef ASSERT
        __ jcc(Assembler::zero, next);

        {
          Label ok;
          __ push(tmp);
          __ cmpptr(mdo_addr, 0);
          __ jcc(Assembler::equal, ok);
          __ cmpptr(mdo_addr, TypeEntries::null_seen);
          __ jcc(Assembler::equal, ok);
          // may have been set by another thread
          __ mov_metadata(tmp, exact_klass->constant_encoding());
          __ xorptr(tmp, mdo_addr);
          __ testptr(tmp, TypeEntries::type_mask);
          __ jcc(Assembler::zero, ok);

          __ stop("unexpected profiling mismatch");
          __ bind(ok);
          __ pop(tmp);
        }
#else
        __ jccb(Assembler::zero, next);
#endif
        // first time here. Set profile type.
        __ movptr(mdo_addr, tmp);
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "inconsistent");

        __ movptr(tmp, mdo_addr);
        __ testptr(tmp, TypeEntries::type_unknown);
        __ jccb(Assembler::notZero, next); // already unknown. Nothing to do anymore.

        __ orptr(mdo_addr, TypeEntries::type_unknown);
      }
    }

    __ bind(next);
  }
}

void LIR_Assembler::emit_delay(LIR_OpDelay*) {
  Unimplemented();
}


void LIR_Assembler::monitor_address(int monitor_no, LIR_Opr dst) {
  __ lea(dst->as_register(), frame_map()->address_for_monitor_lock(monitor_no));
}


void LIR_Assembler::align_backward_branch_target() {
  __ align(BytesPerWord);
}


void LIR_Assembler::negate(LIR_Opr left, LIR_Opr dest, LIR_Opr tmp) {
  if (left->is_single_cpu()) {
    __ negl(left->as_register());
    move_regs(left->as_register(), dest->as_register());

  } else if (left->is_double_cpu()) {
    Register lo = left->as_register_lo();
#ifdef _LP64
    Register dst = dest->as_register_lo();
    __ movptr(dst, lo);
    __ negptr(dst);
#else
    Register hi = left->as_register_hi();
    __ lneg(hi, lo);
    if (dest->as_register_lo() == hi) {
      assert(dest->as_register_hi() != lo, "destroying register");
      move_regs(hi, dest->as_register_hi());
      move_regs(lo, dest->as_register_lo());
    } else {
      move_regs(lo, dest->as_register_lo());
      move_regs(hi, dest->as_register_hi());
    }
#endif // _LP64

  } else if (dest->is_single_xmm()) {
#ifdef _LP64
    if (UseAVX > 2 && !VM_Version::supports_avx512vl()) {
      assert(tmp->is_valid(), "need temporary");
      assert_different_registers(left->as_xmm_float_reg(), tmp->as_xmm_float_reg());
      __ vpxor(dest->as_xmm_float_reg(), tmp->as_xmm_float_reg(), left->as_xmm_float_reg(), 2);
    }
    else
#endif
    {
      assert(!tmp->is_valid(), "do not need temporary");
      if (left->as_xmm_float_reg() != dest->as_xmm_float_reg()) {
        __ movflt(dest->as_xmm_float_reg(), left->as_xmm_float_reg());
      }
      __ xorps(dest->as_xmm_float_reg(),
               ExternalAddress((address)float_signflip_pool));
    }
  } else if (dest->is_double_xmm()) {
#ifdef _LP64
    if (UseAVX > 2 && !VM_Version::supports_avx512vl()) {
      assert(tmp->is_valid(), "need temporary");
      assert_different_registers(left->as_xmm_double_reg(), tmp->as_xmm_double_reg());
      __ vpxor(dest->as_xmm_double_reg(), tmp->as_xmm_double_reg(), left->as_xmm_double_reg(), 2);
    }
    else
#endif
    {
      assert(!tmp->is_valid(), "do not need temporary");
      if (left->as_xmm_double_reg() != dest->as_xmm_double_reg()) {
        __ movdbl(dest->as_xmm_double_reg(), left->as_xmm_double_reg());
      }
      __ xorpd(dest->as_xmm_double_reg(),
               ExternalAddress((address)double_signflip_pool));
    }
#ifndef _LP64
  } else if (left->is_single_fpu() || left->is_double_fpu()) {
    assert(left->fpu() == 0, "arg must be on TOS");
    assert(dest->fpu() == 0, "dest must be TOS");
    __ fchs();
#endif // !_LP64

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::leal(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  assert(src->is_address(), "must be an address");
  assert(dest->is_register(), "must be a register");

  PatchingStub* patch = NULL;
  if (patch_code != lir_patch_none) {
    patch = new PatchingStub(_masm, PatchingStub::access_field_id);
  }

  Register reg = dest->as_pointer_register();
  LIR_Address* addr = src->as_address_ptr();
  __ lea(reg, as_Address(addr));

  if (patch != NULL) {
    patching_epilog(patch, patch_code, addr->base()->as_register(), info);
  }
}



void LIR_Assembler::rt_call(LIR_Opr result, address dest, const LIR_OprList* args, LIR_Opr tmp, CodeEmitInfo* info) {
  assert(!tmp->is_valid(), "don't need temporary");
  __ call(RuntimeAddress(dest));
  if (info != NULL) {
    add_call_info_here(info);
  }
}


void LIR_Assembler::volatile_move_op(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info) {
  assert(type == T_LONG, "only for volatile long fields");

  if (info != NULL) {
    add_debug_info_for_null_check_here(info);
  }

  if (src->is_double_xmm()) {
    if (dest->is_double_cpu()) {
#ifdef _LP64
      __ movdq(dest->as_register_lo(), src->as_xmm_double_reg());
#else
      __ movdl(dest->as_register_lo(), src->as_xmm_double_reg());
      __ psrlq(src->as_xmm_double_reg(), 32);
      __ movdl(dest->as_register_hi(), src->as_xmm_double_reg());
#endif // _LP64
    } else if (dest->is_double_stack()) {
      __ movdbl(frame_map()->address_for_slot(dest->double_stack_ix()), src->as_xmm_double_reg());
    } else if (dest->is_address()) {
      __ movdbl(as_Address(dest->as_address_ptr()), src->as_xmm_double_reg());
    } else {
      ShouldNotReachHere();
    }

  } else if (dest->is_double_xmm()) {
    if (src->is_double_stack()) {
      __ movdbl(dest->as_xmm_double_reg(), frame_map()->address_for_slot(src->double_stack_ix()));
    } else if (src->is_address()) {
      __ movdbl(dest->as_xmm_double_reg(), as_Address(src->as_address_ptr()));
    } else {
      ShouldNotReachHere();
    }

#ifndef _LP64
  } else if (src->is_double_fpu()) {
    assert(src->fpu_regnrLo() == 0, "must be TOS");
    if (dest->is_double_stack()) {
      __ fistp_d(frame_map()->address_for_slot(dest->double_stack_ix()));
    } else if (dest->is_address()) {
      __ fistp_d(as_Address(dest->as_address_ptr()));
    } else {
      ShouldNotReachHere();
    }

  } else if (dest->is_double_fpu()) {
    assert(dest->fpu_regnrLo() == 0, "must be TOS");
    if (src->is_double_stack()) {
      __ fild_d(frame_map()->address_for_slot(src->double_stack_ix()));
    } else if (src->is_address()) {
      __ fild_d(as_Address(src->as_address_ptr()));
    } else {
      ShouldNotReachHere();
    }
#endif // !_LP64

  } else {
    ShouldNotReachHere();
  }
}

#ifdef ASSERT
// emit run-time assertion
void LIR_Assembler::emit_assert(LIR_OpAssert* op) {
  assert(op->code() == lir_assert, "must be");

  if (op->in_opr1()->is_valid()) {
    assert(op->in_opr2()->is_valid(), "both operands must be valid");
    comp_op(op->condition(), op->in_opr1(), op->in_opr2(), op);
  } else {
    assert(op->in_opr2()->is_illegal(), "both operands must be illegal");
    assert(op->condition() == lir_cond_always, "no other conditions allowed");
  }

  Label ok;
  if (op->condition() != lir_cond_always) {
    Assembler::Condition acond = Assembler::zero;
    switch (op->condition()) {
      case lir_cond_equal:        acond = Assembler::equal;       break;
      case lir_cond_notEqual:     acond = Assembler::notEqual;    break;
      case lir_cond_less:         acond = Assembler::less;        break;
      case lir_cond_lessEqual:    acond = Assembler::lessEqual;   break;
      case lir_cond_greaterEqual: acond = Assembler::greaterEqual;break;
      case lir_cond_greater:      acond = Assembler::greater;     break;
      case lir_cond_belowEqual:   acond = Assembler::belowEqual;  break;
      case lir_cond_aboveEqual:   acond = Assembler::aboveEqual;  break;
      default:                    ShouldNotReachHere();
    }
    __ jcc(acond, ok);
  }
  if (op->halt()) {
    const char* str = __ code_string(op->msg());
    __ stop(str);
  } else {
    breakpoint();
  }
  __ bind(ok);
}
#endif

void LIR_Assembler::membar() {
  // QQQ sparc TSO uses this,
  __ membar( Assembler::Membar_mask_bits(Assembler::StoreLoad));
}

void LIR_Assembler::membar_acquire() {
  // No x86 machines currently require load fences
}

void LIR_Assembler::membar_release() {
  // No x86 machines currently require store fences
}

void LIR_Assembler::membar_loadload() {
  // no-op
  //__ membar(Assembler::Membar_mask_bits(Assembler::loadload));
}

void LIR_Assembler::membar_storestore() {
  // no-op
  //__ membar(Assembler::Membar_mask_bits(Assembler::storestore));
}

void LIR_Assembler::membar_loadstore() {
  // no-op
  //__ membar(Assembler::Membar_mask_bits(Assembler::loadstore));
}

void LIR_Assembler::membar_storeload() {
  __ membar(Assembler::Membar_mask_bits(Assembler::StoreLoad));
}

void LIR_Assembler::on_spin_wait() {
  __ pause ();
}

void LIR_Assembler::get_thread(LIR_Opr result_reg) {
  assert(result_reg->is_register(), "check");
#ifdef _LP64
  // __ get_thread(result_reg->as_register_lo());
  __ mov(result_reg->as_register(), r15_thread);
#else
  __ get_thread(result_reg->as_register());
#endif // _LP64
}


void LIR_Assembler::peephole(LIR_List*) {
  // do nothing for now
}

void LIR_Assembler::atomic_op(LIR_Code code, LIR_Opr src, LIR_Opr data, LIR_Opr dest, LIR_Opr tmp) {
  assert(data == dest, "xchg/xadd uses only 2 operands");

  if (data->type() == T_INT) {
    if (code == lir_xadd) {
      __ lock();
      __ xaddl(as_Address(src->as_address_ptr()), data->as_register());
    } else {
      __ xchgl(data->as_register(), as_Address(src->as_address_ptr()));
    }
  } else if (data->is_oop()) {
    assert (code == lir_xchg, "xadd for oops");
    Register obj = data->as_register();
#ifdef _LP64
    if (UseCompressedOops) {
      __ encode_heap_oop(obj);
      __ xchgl(obj, as_Address(src->as_address_ptr()));
      __ decode_heap_oop(obj);
    } else {
      __ xchgptr(obj, as_Address(src->as_address_ptr()));
    }
#else
    __ xchgl(obj, as_Address(src->as_address_ptr()));
#endif
  } else if (data->type() == T_LONG) {
#ifdef _LP64
    assert(data->as_register_lo() == data->as_register_hi(), "should be a single register");
    if (code == lir_xadd) {
      __ lock();
      __ xaddq(as_Address(src->as_address_ptr()), data->as_register_lo());
    } else {
      __ xchgq(data->as_register_lo(), as_Address(src->as_address_ptr()));
    }
#else
    ShouldNotReachHere();
#endif
  } else {
    ShouldNotReachHere();
  }
}

#undef __
