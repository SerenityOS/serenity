/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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
#include "asm/assembler.hpp"
#include "c1/c1_CodeStubs.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_Runtime1.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciArrayKlass.hpp"
#include "ci/ciInstance.hpp"
#include "code/compiledIC.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gc_globals.hpp"
#include "nativeInst_aarch64.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/powerOfTwo.hpp"
#include "vmreg_aarch64.inline.hpp"


#ifndef PRODUCT
#define COMMENT(x)   do { __ block_comment(x); } while (0)
#else
#define COMMENT(x)
#endif

NEEDS_CLEANUP // remove this definitions ?
const Register IC_Klass    = rscratch2;   // where the IC klass is cached
const Register SYNC_header = r0;   // synchronization header
const Register SHIFT_count = r0;   // where count for shift operations must be

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


bool LIR_Assembler::is_small_constant(LIR_Opr opr) { Unimplemented(); return false; }


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

address LIR_Assembler::int_constant(jlong n) {
  address const_addr = __ long_constant(n);
  if (const_addr == NULL) {
    bailout("const section overflow");
    return __ code()->consts()->start();
  } else {
    return const_addr;
  }
}

void LIR_Assembler::breakpoint() { Unimplemented(); }

void LIR_Assembler::push(LIR_Opr opr) { Unimplemented(); }

void LIR_Assembler::pop(LIR_Opr opr) { Unimplemented(); }

bool LIR_Assembler::is_literal_address(LIR_Address* addr) { Unimplemented(); return false; }
//-------------------------------------------

static Register as_reg(LIR_Opr op) {
  return op->is_double_cpu() ? op->as_register_lo() : op->as_register();
}

static jlong as_long(LIR_Opr data) {
  jlong result;
  switch (data->type()) {
  case T_INT:
    result = (data->as_jint());
    break;
  case T_LONG:
    result = (data->as_jlong());
    break;
  default:
    ShouldNotReachHere();
    result = 0;  // unreachable
  }
  return result;
}

Address LIR_Assembler::as_Address(LIR_Address* addr, Register tmp) {
  Register base = addr->base()->as_pointer_register();
  LIR_Opr opr = addr->index();
  if (opr->is_cpu_register()) {
    Register index;
    if (opr->is_single_cpu())
      index = opr->as_register();
    else
      index = opr->as_register_lo();
    assert(addr->disp() == 0, "must be");
    switch(opr->type()) {
      case T_INT:
        return Address(base, index, Address::sxtw(addr->scale()));
      case T_LONG:
        return Address(base, index, Address::lsl(addr->scale()));
      default:
        ShouldNotReachHere();
      }
  } else  {
    intptr_t addr_offset = intptr_t(addr->disp());
    if (Address::offset_ok_for_immed(addr_offset, addr->scale()))
      return Address(base, addr_offset, Address::lsl(addr->scale()));
    else {
      __ mov(tmp, addr_offset);
      return Address(base, tmp, Address::lsl(addr->scale()));
    }
  }
  return Address();
}

Address LIR_Assembler::as_Address_hi(LIR_Address* addr) {
  ShouldNotReachHere();
  return Address();
}

Address LIR_Assembler::as_Address(LIR_Address* addr) {
  return as_Address(addr, rscratch1);
}

Address LIR_Assembler::as_Address_lo(LIR_Address* addr) {
  return as_Address(addr, rscratch1);  // Ouch
  // FIXME: This needs to be much more clever.  See x86.
}

// Ensure a valid Address (base + offset) to a stack-slot. If stack access is
// not encodable as a base + (immediate) offset, generate an explicit address
// calculation to hold the address in a temporary register.
Address LIR_Assembler::stack_slot_address(int index, uint size, Register tmp, int adjust) {
  precond(size == 4 || size == 8);
  Address addr = frame_map()->address_for_slot(index, adjust);
  precond(addr.getMode() == Address::base_plus_offset);
  precond(addr.base() == sp);
  precond(addr.offset() > 0);
  uint mask = size - 1;
  assert((addr.offset() & mask) == 0, "scaled offsets only");
  return __ legitimize_address(addr, size, tmp);
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
  // r2: osr buffer
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
  //   r2: pointer to osr buffer
  //
  // All other registers are dead at this point and the locals will be
  // copied into place by code emitted in the IR.

  Register OSR_buf = osrBufferPointer()->as_pointer_register();
  { assert(frame::interpreter_frame_monitor_size() == BasicObjectLock::size(), "adjust code below");
    int monitor_offset = BytesPerWord * method()->max_locals() +
      (2 * BytesPerWord) * (number_of_locks - 1);
    // SharedRuntime::OSR_migration_begin() packs BasicObjectLocks in
    // the OSR buffer using 2 word entries: first the lock and then
    // the oop.
    for (int i = 0; i < number_of_locks; i++) {
      int slot_offset = monitor_offset - ((i * 2) * BytesPerWord);
#ifdef ASSERT
      // verify the interpreter's monitor has a non-null object
      {
        Label L;
        __ ldr(rscratch1, Address(OSR_buf, slot_offset + 1*BytesPerWord));
        __ cbnz(rscratch1, L);
        __ stop("locked object is NULL");
        __ bind(L);
      }
#endif
      __ ldr(r19, Address(OSR_buf, slot_offset + 0));
      __ str(r19, frame_map()->address_for_monitor_lock(i));
      __ ldr(r19, Address(OSR_buf, slot_offset + 1*BytesPerWord));
      __ str(r19, frame_map()->address_for_monitor_object(i));
    }
  }
}


// inline cache check; done before the frame is built.
int LIR_Assembler::check_icache() {
  Register receiver = FrameMap::receiver_opr->as_register();
  Register ic_klass = IC_Klass;
  int start_offset = __ offset();
  __ inline_cache_check(receiver, ic_klass);

  // if icache check fails, then jump to runtime routine
  // Note: RECEIVER must still contain the receiver!
  Label dont;
  __ br(Assembler::EQ, dont);
  __ far_jump(RuntimeAddress(SharedRuntime::get_ic_miss_stub()));

  // We align the verified entry point unless the method body
  // (including its inline cache check) will fit in a single 64-byte
  // icache line.
  if (! method()->is_accessor() || __ offset() - start_offset > 4 * 4) {
    // force alignment after the cache check.
    __ align(CodeEntryAlignment);
  }

  __ bind(dont);
  return start_offset;
}

void LIR_Assembler::clinit_barrier(ciMethod* method) {
  assert(VM_Version::supports_fast_class_init_checks(), "sanity");
  assert(!method->holder()->is_not_initialized(), "initialization should have been started");

  Label L_skip_barrier;

  __ mov_metadata(rscratch2, method->holder()->constant_encoding());
  __ clinit_barrier(rscratch2, rscratch1, &L_skip_barrier /*L_fast_path*/);
  __ far_jump(RuntimeAddress(SharedRuntime::get_handle_wrong_method_stub()));
  __ bind(L_skip_barrier);
}

void LIR_Assembler::jobject2reg(jobject o, Register reg) {
  if (o == NULL) {
    __ mov(reg, zr);
  } else {
    __ movoop(reg, o, /*immediate*/true);
  }
}

void LIR_Assembler::deoptimize_trap(CodeEmitInfo *info) {
  address target = NULL;
  relocInfo::relocType reloc_type = relocInfo::none;

  switch (patching_id(info)) {
  case PatchingStub::access_field_id:
    target = Runtime1::entry_for(Runtime1::access_field_patching_id);
    reloc_type = relocInfo::section_word_type;
    break;
  case PatchingStub::load_klass_id:
    target = Runtime1::entry_for(Runtime1::load_klass_patching_id);
    reloc_type = relocInfo::metadata_type;
    break;
  case PatchingStub::load_mirror_id:
    target = Runtime1::entry_for(Runtime1::load_mirror_patching_id);
    reloc_type = relocInfo::oop_type;
    break;
  case PatchingStub::load_appendix_id:
    target = Runtime1::entry_for(Runtime1::load_appendix_patching_id);
    reloc_type = relocInfo::oop_type;
    break;
  default: ShouldNotReachHere();
  }

  __ far_call(RuntimeAddress(target));
  add_call_info_here(info);
}

void LIR_Assembler::jobject2reg_with_patching(Register reg, CodeEmitInfo *info) {
  deoptimize_trap(info);
}


// This specifies the rsp decrement needed to build the frame
int LIR_Assembler::initial_frame_size_in_bytes() const {
  // if rounding, must let FrameMap know!

  return in_bytes(frame_map()->framesize_in_bytes());
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

  // the exception oop and pc are in r0, and r3
  // no other registers need to be preserved, so invalidate them
  __ invalidate_registers(false, true, true, false, true, true);

  // check that there is really an exception
  __ verify_not_null_oop(r0);

  // search an exception handler (r0: exception oop, r3: throwing pc)
  __ far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::handle_exception_from_callee_id)));  __ should_not_reach_here();
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
  __ ldr(r0, Address(rthread, JavaThread::exception_oop_offset()));
  __ str(zr, Address(rthread, JavaThread::exception_oop_offset()));
  __ str(zr, Address(rthread, JavaThread::exception_pc_offset()));

  __ bind(_unwind_handler_entry);
  __ verify_not_null_oop(r0);
  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ mov(r19, r0);  // Preserve the exception
  }

  // Preform needed unlocking
  MonitorExitStub* stub = NULL;
  if (method()->is_synchronized()) {
    monitor_address(0, FrameMap::r0_opr);
    stub = new MonitorExitStub(FrameMap::r0_opr, true, 0);
    __ unlock_object(r5, r4, r0, *stub->entry());
    __ bind(*stub->continuation());
  }

  if (compilation()->env()->dtrace_method_probes()) {
    __ mov(c_rarg0, rthread);
    __ mov_metadata(c_rarg1, method()->constant_encoding());
    __ call_VM_leaf(CAST_FROM_FN_PTR(address, SharedRuntime::dtrace_method_exit), c_rarg0, c_rarg1);
  }

  if (method()->is_synchronized() || compilation()->env()->dtrace_method_probes()) {
    __ mov(r0, r19);  // Restore the exception
  }

  // remove the activation and dispatch to the unwind handler
  __ block_comment("remove_frame and dispatch to the unwind handler");
  __ remove_frame(initial_frame_size_in_bytes());
  __ far_jump(RuntimeAddress(Runtime1::entry_for(Runtime1::unwind_exception_id)));

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

  __ adr(lr, pc());
  __ far_jump(RuntimeAddress(SharedRuntime::deopt_blob()->unpack()));
  guarantee(code_offset() - offset <= deopt_handler_size(), "overflow");
  __ end_a_stub();

  return offset;
}

void LIR_Assembler::add_debug_info_for_branch(address adr, CodeEmitInfo* info) {
  _masm->code_section()->relocate(adr, relocInfo::poll_type);
  int pc_offset = code_offset();
  flush_debug_info(pc_offset);
  info->record_debug_info(compilation()->debug_info_recorder(), pc_offset);
  if (info->exception_handlers() != NULL) {
    compilation()->add_exception_handlers_for_pco(pc_offset, info->exception_handlers());
  }
}

void LIR_Assembler::return_op(LIR_Opr result, C1SafepointPollStub* code_stub) {
  assert(result->is_illegal() || !result->is_single_cpu() || result->as_register() == r0, "word returns are in r0,");

  // Pop the stack before the safepoint code
  __ remove_frame(initial_frame_size_in_bytes());

  if (StackReservedPages > 0 && compilation()->has_reserved_stack_access()) {
    __ reserved_stack_check();
  }

  code_stub->set_safepoint_offset(__ offset());
  __ relocate(relocInfo::poll_return_type);
  __ safepoint_poll(*code_stub->entry(), true /* at_return */, false /* acquire */, true /* in_nmethod */);
  __ ret(lr);
}

int LIR_Assembler::safepoint_poll(LIR_Opr tmp, CodeEmitInfo* info) {
  guarantee(info != NULL, "Shouldn't be NULL");
  __ get_polling_page(rscratch1, relocInfo::poll_type);
  add_debug_info_for_branch(info);  // This isn't just debug info:
                                    // it's the oop map
  __ read_polling_page(rscratch1, relocInfo::poll_type);
  return __ offset();
}


void LIR_Assembler::move_regs(Register from_reg, Register to_reg) {
  if (from_reg == r31_sp)
    from_reg = sp;
  if (to_reg == r31_sp)
    to_reg = sp;
  __ mov(to_reg, from_reg);
}

void LIR_Assembler::swap_reg(Register a, Register b) { Unimplemented(); }


void LIR_Assembler::const2reg(LIR_Opr src, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  assert(src->is_constant(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();

  switch (c->type()) {
    case T_INT: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ movw(dest->as_register(), c->as_jint());
      break;
    }

    case T_ADDRESS: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ mov(dest->as_register(), c->as_jint());
      break;
    }

    case T_LONG: {
      assert(patch_code == lir_patch_none, "no patching handled here");
      __ mov(dest->as_register_lo(), (intptr_t)c->as_jlong());
      break;
    }

    case T_OBJECT: {
        if (patch_code == lir_patch_none) {
          jobject2reg(c->as_jobject(), dest->as_register());
        } else {
          jobject2reg_with_patching(dest->as_register(), info);
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
      if (__ operand_valid_for_float_immediate(c->as_jfloat())) {
        __ fmovs(dest->as_float_reg(), (c->as_jfloat()));
      } else {
        __ adr(rscratch1, InternalAddress(float_constant(c->as_jfloat())));
        __ ldrs(dest->as_float_reg(), Address(rscratch1));
      }
      break;
    }

    case T_DOUBLE: {
      if (__ operand_valid_for_float_immediate(c->as_jdouble())) {
        __ fmovd(dest->as_double_reg(), (c->as_jdouble()));
      } else {
        __ adr(rscratch1, InternalAddress(double_constant(c->as_jdouble())));
        __ ldrd(dest->as_double_reg(), Address(rscratch1));
      }
      break;
    }

    default:
      ShouldNotReachHere();
  }
}

void LIR_Assembler::const2stack(LIR_Opr src, LIR_Opr dest) {
  LIR_Const* c = src->as_constant_ptr();
  switch (c->type()) {
  case T_OBJECT:
    {
      if (! c->as_jobject())
        __ str(zr, frame_map()->address_for_slot(dest->single_stack_ix()));
      else {
        const2reg(src, FrameMap::rscratch1_opr, lir_patch_none, NULL);
        reg2stack(FrameMap::rscratch1_opr, dest, c->type(), false);
      }
    }
    break;
  case T_ADDRESS:
    {
      const2reg(src, FrameMap::rscratch1_opr, lir_patch_none, NULL);
      reg2stack(FrameMap::rscratch1_opr, dest, c->type(), false);
    }
  case T_INT:
  case T_FLOAT:
    {
      Register reg = zr;
      if (c->as_jint_bits() == 0)
        __ strw(zr, frame_map()->address_for_slot(dest->single_stack_ix()));
      else {
        __ movw(rscratch1, c->as_jint_bits());
        __ strw(rscratch1, frame_map()->address_for_slot(dest->single_stack_ix()));
      }
    }
    break;
  case T_LONG:
  case T_DOUBLE:
    {
      Register reg = zr;
      if (c->as_jlong_bits() == 0)
        __ str(zr, frame_map()->address_for_slot(dest->double_stack_ix(),
                                                 lo_word_offset_in_bytes));
      else {
        __ mov(rscratch1, (intptr_t)c->as_jlong_bits());
        __ str(rscratch1, frame_map()->address_for_slot(dest->double_stack_ix(),
                                                        lo_word_offset_in_bytes));
      }
    }
    break;
  default:
    ShouldNotReachHere();
  }
}

void LIR_Assembler::const2mem(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info, bool wide) {
  assert(src->is_constant(), "should not call otherwise");
  LIR_Const* c = src->as_constant_ptr();
  LIR_Address* to_addr = dest->as_address_ptr();

  void (Assembler::* insn)(Register Rt, const Address &adr);

  switch (type) {
  case T_ADDRESS:
    assert(c->as_jint() == 0, "should be");
    insn = &Assembler::str;
    break;
  case T_LONG:
    assert(c->as_jlong() == 0, "should be");
    insn = &Assembler::str;
    break;
  case T_INT:
    assert(c->as_jint() == 0, "should be");
    insn = &Assembler::strw;
    break;
  case T_OBJECT:
  case T_ARRAY:
    assert(c->as_jobject() == 0, "should be");
    if (UseCompressedOops && !wide) {
      insn = &Assembler::strw;
    } else {
      insn = &Assembler::str;
    }
    break;
  case T_CHAR:
  case T_SHORT:
    assert(c->as_jint() == 0, "should be");
    insn = &Assembler::strh;
    break;
  case T_BOOLEAN:
  case T_BYTE:
    assert(c->as_jint() == 0, "should be");
    insn = &Assembler::strb;
    break;
  default:
    ShouldNotReachHere();
    insn = &Assembler::str;  // unreachable
  }

  if (info) add_debug_info_for_null_check_here(info);
  (_masm->*insn)(zr, as_Address(to_addr, rscratch1));
}

void LIR_Assembler::reg2reg(LIR_Opr src, LIR_Opr dest) {
  assert(src->is_register(), "should not call otherwise");
  assert(dest->is_register(), "should not call otherwise");

  // move between cpu-registers
  if (dest->is_single_cpu()) {
    if (src->type() == T_LONG) {
      // Can do LONG -> OBJECT
      move_regs(src->as_register_lo(), dest->as_register());
      return;
    }
    assert(src->is_single_cpu(), "must match");
    if (src->type() == T_OBJECT) {
      __ verify_oop(src->as_register());
    }
    move_regs(src->as_register(), dest->as_register());

  } else if (dest->is_double_cpu()) {
    if (is_reference_type(src->type())) {
      // Surprising to me but we can see move of a long to t_object
      __ verify_oop(src->as_register());
      move_regs(src->as_register(), dest->as_register_lo());
      return;
    }
    assert(src->is_double_cpu(), "must match");
    Register f_lo = src->as_register_lo();
    Register f_hi = src->as_register_hi();
    Register t_lo = dest->as_register_lo();
    Register t_hi = dest->as_register_hi();
    assert(f_hi == f_lo, "must be same");
    assert(t_hi == t_lo, "must be same");
    move_regs(f_lo, t_lo);

  } else if (dest->is_single_fpu()) {
    __ fmovs(dest->as_float_reg(), src->as_float_reg());

  } else if (dest->is_double_fpu()) {
    __ fmovd(dest->as_double_reg(), src->as_double_reg());

  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::reg2stack(LIR_Opr src, LIR_Opr dest, BasicType type, bool pop_fpu_stack) {
  precond(src->is_register() && dest->is_stack());

  uint const c_sz32 = sizeof(uint32_t);
  uint const c_sz64 = sizeof(uint64_t);

  if (src->is_single_cpu()) {
    int index = dest->single_stack_ix();
    if (is_reference_type(type)) {
      __ str(src->as_register(), stack_slot_address(index, c_sz64, rscratch1));
      __ verify_oop(src->as_register());
    } else if (type == T_METADATA || type == T_DOUBLE || type == T_ADDRESS) {
      __ str(src->as_register(), stack_slot_address(index, c_sz64, rscratch1));
    } else {
      __ strw(src->as_register(), stack_slot_address(index, c_sz32, rscratch1));
    }

  } else if (src->is_double_cpu()) {
    int index = dest->double_stack_ix();
    Address dest_addr_LO = stack_slot_address(index, c_sz64, rscratch1, lo_word_offset_in_bytes);
    __ str(src->as_register_lo(), dest_addr_LO);

  } else if (src->is_single_fpu()) {
    int index = dest->single_stack_ix();
    __ strs(src->as_float_reg(), stack_slot_address(index, c_sz32, rscratch1));

  } else if (src->is_double_fpu()) {
    int index = dest->double_stack_ix();
    __ strd(src->as_double_reg(), stack_slot_address(index, c_sz64, rscratch1));

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::reg2mem(LIR_Opr src, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack, bool wide) {
  LIR_Address* to_addr = dest->as_address_ptr();
  PatchingStub* patch = NULL;
  Register compressed_src = rscratch1;

  if (patch_code != lir_patch_none) {
    deoptimize_trap(info);
    return;
  }

  if (is_reference_type(type)) {
    __ verify_oop(src->as_register());

    if (UseCompressedOops && !wide) {
      __ encode_heap_oop(compressed_src, src->as_register());
    } else {
      compressed_src = src->as_register();
    }
  }

  int null_check_here = code_offset();
  switch (type) {
    case T_FLOAT: {
      __ strs(src->as_float_reg(), as_Address(to_addr));
      break;
    }

    case T_DOUBLE: {
      __ strd(src->as_double_reg(), as_Address(to_addr));
      break;
    }

    case T_ARRAY:   // fall through
    case T_OBJECT:  // fall through
      if (UseCompressedOops && !wide) {
        __ strw(compressed_src, as_Address(to_addr, rscratch2));
      } else {
         __ str(compressed_src, as_Address(to_addr));
      }
      break;
    case T_METADATA:
      // We get here to store a method pointer to the stack to pass to
      // a dtrace runtime call. This can't work on 64 bit with
      // compressed klass ptrs: T_METADATA can be a compressed klass
      // ptr or a 64 bit method pointer.
      ShouldNotReachHere();
      __ str(src->as_register(), as_Address(to_addr));
      break;
    case T_ADDRESS:
      __ str(src->as_register(), as_Address(to_addr));
      break;
    case T_INT:
      __ strw(src->as_register(), as_Address(to_addr));
      break;

    case T_LONG: {
      __ str(src->as_register_lo(), as_Address_lo(to_addr));
      break;
    }

    case T_BYTE:    // fall through
    case T_BOOLEAN: {
      __ strb(src->as_register(), as_Address(to_addr));
      break;
    }

    case T_CHAR:    // fall through
    case T_SHORT:
      __ strh(src->as_register(), as_Address(to_addr));
      break;

    default:
      ShouldNotReachHere();
  }
  if (info != NULL) {
    add_debug_info_for_null_check(null_check_here, info);
  }
}


void LIR_Assembler::stack2reg(LIR_Opr src, LIR_Opr dest, BasicType type) {
  precond(src->is_stack() && dest->is_register());

  uint const c_sz32 = sizeof(uint32_t);
  uint const c_sz64 = sizeof(uint64_t);

  if (dest->is_single_cpu()) {
    int index = src->single_stack_ix();
    if (is_reference_type(type)) {
      __ ldr(dest->as_register(), stack_slot_address(index, c_sz64, rscratch1));
      __ verify_oop(dest->as_register());
    } else if (type == T_METADATA || type == T_ADDRESS) {
      __ ldr(dest->as_register(), stack_slot_address(index, c_sz64, rscratch1));
    } else {
      __ ldrw(dest->as_register(), stack_slot_address(index, c_sz32, rscratch1));
    }

  } else if (dest->is_double_cpu()) {
    int index = src->double_stack_ix();
    Address src_addr_LO = stack_slot_address(index, c_sz64, rscratch1, lo_word_offset_in_bytes);
    __ ldr(dest->as_register_lo(), src_addr_LO);

  } else if (dest->is_single_fpu()) {
    int index = src->single_stack_ix();
    __ ldrs(dest->as_float_reg(), stack_slot_address(index, c_sz32, rscratch1));

  } else if (dest->is_double_fpu()) {
    int index = src->double_stack_ix();
    __ ldrd(dest->as_double_reg(), stack_slot_address(index, c_sz64, rscratch1));

  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::klass2reg_with_patching(Register reg, CodeEmitInfo* info) {
  address target = NULL;
  relocInfo::relocType reloc_type = relocInfo::none;

  switch (patching_id(info)) {
  case PatchingStub::access_field_id:
    target = Runtime1::entry_for(Runtime1::access_field_patching_id);
    reloc_type = relocInfo::section_word_type;
    break;
  case PatchingStub::load_klass_id:
    target = Runtime1::entry_for(Runtime1::load_klass_patching_id);
    reloc_type = relocInfo::metadata_type;
    break;
  case PatchingStub::load_mirror_id:
    target = Runtime1::entry_for(Runtime1::load_mirror_patching_id);
    reloc_type = relocInfo::oop_type;
    break;
  case PatchingStub::load_appendix_id:
    target = Runtime1::entry_for(Runtime1::load_appendix_patching_id);
    reloc_type = relocInfo::oop_type;
    break;
  default: ShouldNotReachHere();
  }

  __ far_call(RuntimeAddress(target));
  add_call_info_here(info);
}

void LIR_Assembler::stack2stack(LIR_Opr src, LIR_Opr dest, BasicType type) {

  LIR_Opr temp;
  if (type == T_LONG || type == T_DOUBLE)
    temp = FrameMap::rscratch1_long_opr;
  else
    temp = FrameMap::rscratch1_opr;

  stack2reg(src, temp, src->type());
  reg2stack(temp, dest, dest->type(), false);
}


void LIR_Assembler::mem2reg(LIR_Opr src, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code, CodeEmitInfo* info, bool wide) {
  LIR_Address* addr = src->as_address_ptr();
  LIR_Address* from_addr = src->as_address_ptr();

  if (addr->base()->type() == T_OBJECT) {
    __ verify_oop(addr->base()->as_pointer_register());
  }

  if (patch_code != lir_patch_none) {
    deoptimize_trap(info);
    return;
  }

  if (info != NULL) {
    add_debug_info_for_null_check_here(info);
  }
  int null_check_here = code_offset();
  switch (type) {
    case T_FLOAT: {
      __ ldrs(dest->as_float_reg(), as_Address(from_addr));
      break;
    }

    case T_DOUBLE: {
      __ ldrd(dest->as_double_reg(), as_Address(from_addr));
      break;
    }

    case T_ARRAY:   // fall through
    case T_OBJECT:  // fall through
      if (UseCompressedOops && !wide) {
        __ ldrw(dest->as_register(), as_Address(from_addr));
      } else {
         __ ldr(dest->as_register(), as_Address(from_addr));
      }
      break;
    case T_METADATA:
      // We get here to store a method pointer to the stack to pass to
      // a dtrace runtime call. This can't work on 64 bit with
      // compressed klass ptrs: T_METADATA can be a compressed klass
      // ptr or a 64 bit method pointer.
      ShouldNotReachHere();
      __ ldr(dest->as_register(), as_Address(from_addr));
      break;
    case T_ADDRESS:
      // FIXME: OMG this is a horrible kludge.  Any offset from an
      // address that matches klass_offset_in_bytes() will be loaded
      // as a word, not a long.
      if (UseCompressedClassPointers && addr->disp() == oopDesc::klass_offset_in_bytes()) {
        __ ldrw(dest->as_register(), as_Address(from_addr));
      } else {
        __ ldr(dest->as_register(), as_Address(from_addr));
      }
      break;
    case T_INT:
      __ ldrw(dest->as_register(), as_Address(from_addr));
      break;

    case T_LONG: {
      __ ldr(dest->as_register_lo(), as_Address_lo(from_addr));
      break;
    }

    case T_BYTE:
      __ ldrsb(dest->as_register(), as_Address(from_addr));
      break;
    case T_BOOLEAN: {
      __ ldrb(dest->as_register(), as_Address(from_addr));
      break;
    }

    case T_CHAR:
      __ ldrh(dest->as_register(), as_Address(from_addr));
      break;
    case T_SHORT:
      __ ldrsh(dest->as_register(), as_Address(from_addr));
      break;

    default:
      ShouldNotReachHere();
  }

  if (is_reference_type(type)) {
    if (UseCompressedOops && !wide) {
      __ decode_heap_oop(dest->as_register());
    }

    if (!UseZGC) {
      // Load barrier has not yet been applied, so ZGC can't verify the oop here
      __ verify_oop(dest->as_register());
    }
  } else if (type == T_ADDRESS && addr->disp() == oopDesc::klass_offset_in_bytes()) {
    if (UseCompressedClassPointers) {
      __ decode_klass_not_null(dest->as_register());
    }
  }
}


int LIR_Assembler::array_element_size(BasicType type) const {
  int elem_size = type2aelembytes(type);
  return exact_log2(elem_size);
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
    __ fmaddd(op->result_opr()->as_double_reg(),
              op->in_opr1()->as_double_reg(),
              op->in_opr2()->as_double_reg(),
              op->in_opr3()->as_double_reg());
    break;
  case lir_fmaf:
    __ fmadds(op->result_opr()->as_float_reg(),
              op->in_opr1()->as_float_reg(),
              op->in_opr2()->as_float_reg(),
              op->in_opr3()->as_float_reg());
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
    __ b(*(op->label()));
  } else {
    Assembler::Condition acond;
    if (op->code() == lir_cond_float_branch) {
      bool is_unordered = (op->ublock() == op->block());
      // Assembler::EQ does not permit unordered branches, so we add
      // another branch here.  Likewise, Assembler::NE does not permit
      // ordered branches.
      if ((is_unordered && op->cond() == lir_cond_equal)
          || (!is_unordered && op->cond() == lir_cond_notEqual))
        __ br(Assembler::VS, *(op->ublock()->label()));
      switch(op->cond()) {
      case lir_cond_equal:        acond = Assembler::EQ; break;
      case lir_cond_notEqual:     acond = Assembler::NE; break;
      case lir_cond_less:         acond = (is_unordered ? Assembler::LT : Assembler::LO); break;
      case lir_cond_lessEqual:    acond = (is_unordered ? Assembler::LE : Assembler::LS); break;
      case lir_cond_greaterEqual: acond = (is_unordered ? Assembler::HS : Assembler::GE); break;
      case lir_cond_greater:      acond = (is_unordered ? Assembler::HI : Assembler::GT); break;
      default:                    ShouldNotReachHere();
        acond = Assembler::EQ;  // unreachable
      }
    } else {
      switch (op->cond()) {
        case lir_cond_equal:        acond = Assembler::EQ; break;
        case lir_cond_notEqual:     acond = Assembler::NE; break;
        case lir_cond_less:         acond = Assembler::LT; break;
        case lir_cond_lessEqual:    acond = Assembler::LE; break;
        case lir_cond_greaterEqual: acond = Assembler::GE; break;
        case lir_cond_greater:      acond = Assembler::GT; break;
        case lir_cond_belowEqual:   acond = Assembler::LS; break;
        case lir_cond_aboveEqual:   acond = Assembler::HS; break;
        default:                    ShouldNotReachHere();
          acond = Assembler::EQ;  // unreachable
      }
    }
    __ br(acond,*(op->label()));
  }
}



void LIR_Assembler::emit_opConvert(LIR_OpConvert* op) {
  LIR_Opr src  = op->in_opr();
  LIR_Opr dest = op->result_opr();

  switch (op->bytecode()) {
    case Bytecodes::_i2f:
      {
        __ scvtfws(dest->as_float_reg(), src->as_register());
        break;
      }
    case Bytecodes::_i2d:
      {
        __ scvtfwd(dest->as_double_reg(), src->as_register());
        break;
      }
    case Bytecodes::_l2d:
      {
        __ scvtfd(dest->as_double_reg(), src->as_register_lo());
        break;
      }
    case Bytecodes::_l2f:
      {
        __ scvtfs(dest->as_float_reg(), src->as_register_lo());
        break;
      }
    case Bytecodes::_f2d:
      {
        __ fcvts(dest->as_double_reg(), src->as_float_reg());
        break;
      }
    case Bytecodes::_d2f:
      {
        __ fcvtd(dest->as_float_reg(), src->as_double_reg());
        break;
      }
    case Bytecodes::_i2c:
      {
        __ ubfx(dest->as_register(), src->as_register(), 0, 16);
        break;
      }
    case Bytecodes::_i2l:
      {
        __ sxtw(dest->as_register_lo(), src->as_register());
        break;
      }
    case Bytecodes::_i2s:
      {
        __ sxth(dest->as_register(), src->as_register());
        break;
      }
    case Bytecodes::_i2b:
      {
        __ sxtb(dest->as_register(), src->as_register());
        break;
      }
    case Bytecodes::_l2i:
      {
        _masm->block_comment("FIXME: This could be a no-op");
        __ uxtw(dest->as_register(), src->as_register_lo());
        break;
      }
    case Bytecodes::_d2l:
      {
        __ fcvtzd(dest->as_register_lo(), src->as_double_reg());
        break;
      }
    case Bytecodes::_f2i:
      {
        __ fcvtzsw(dest->as_register(), src->as_float_reg());
        break;
      }
    case Bytecodes::_f2l:
      {
        __ fcvtzs(dest->as_register_lo(), src->as_float_reg());
        break;
      }
    case Bytecodes::_d2i:
      {
        __ fcvtzdw(dest->as_register(), src->as_double_reg());
        break;
      }
    default: ShouldNotReachHere();
  }
}

void LIR_Assembler::emit_alloc_obj(LIR_OpAllocObj* op) {
  if (op->init_check()) {
    __ ldrb(rscratch1, Address(op->klass()->as_register(),
                               InstanceKlass::init_state_offset()));
    __ cmpw(rscratch1, InstanceKlass::fully_initialized);
    add_debug_info_for_null_check_here(op->stub()->info());
    __ br(Assembler::NE, *op->stub()->entry());
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
  __ uxtw(len, len);

  if (UseSlowPath ||
      (!UseFastNewObjectArray && is_reference_type(op->type())) ||
      (!UseFastNewTypeArray   && !is_reference_type(op->type()))) {
    __ b(*op->stub()->entry());
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
    __ lea(rscratch2, Address(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i))));
    __ ldr(rscratch1, Address(rscratch2));
    __ cmp(recv, rscratch1);
    __ br(Assembler::NE, next_test);
    Address data_addr(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i)));
    __ addptr(data_addr, DataLayout::counter_increment);
    __ b(*update_done);
    __ bind(next_test);
  }

  // Didn't find receiver; find next empty slot and fill it in
  for (uint i = 0; i < ReceiverTypeData::row_limit(); i++) {
    Label next_test;
    __ lea(rscratch2,
           Address(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_offset(i))));
    Address recv_addr(rscratch2);
    __ ldr(rscratch1, recv_addr);
    __ cbnz(rscratch1, next_test);
    __ str(recv, recv_addr);
    __ mov(rscratch1, DataLayout::counter_increment);
    __ lea(rscratch2, Address(mdo, md->byte_offset_of_slot(data, ReceiverTypeData::receiver_count_offset(i))));
    __ str(rscratch1, Address(rscratch2));
    __ b(*update_done);
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

  // check if it needs to be profiled
  ciMethodData* md;
  ciProfileData* data;

  const bool should_profile = op->should_profile();

  if (should_profile) {
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
  Label *success_target = should_profile ? &profile_cast_success : success;
  Label *failure_target = should_profile ? &profile_cast_failure : failure;

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

    if (should_profile) {
      Label not_null;
      __ cbnz(obj, not_null);
      // Object is null; update MDO and exit
      Register mdo  = klass_RInfo;
      __ mov_metadata(mdo, md->constant_encoding());
      Address data_addr
        = __ form_address(rscratch2, mdo,
                          md->byte_offset_of_slot(data, DataLayout::flags_offset()),
                          0);
      __ ldrb(rscratch1, data_addr);
      __ orr(rscratch1, rscratch1, BitData::null_seen_byte_constant());
      __ strb(rscratch1, data_addr);
      __ b(*obj_is_null);
      __ bind(not_null);
    } else {
      __ cbz(obj, *obj_is_null);
    }

  if (!k->is_loaded()) {
    klass2reg_with_patching(k_RInfo, op->info_for_patch());
  } else {
    __ mov_metadata(k_RInfo, k->constant_encoding());
  }
  __ verify_oop(obj);

  if (op->fast_check()) {
    // get object class
    // not a safepoint as obj null check happens earlier
    __ load_klass(rscratch1, obj);
    __ cmp( rscratch1, k_RInfo);

    __ br(Assembler::NE, *failure_target);
    // successful cast, fall through to profile or jump
  } else {
    // get object class
    // not a safepoint as obj null check happens earlier
    __ load_klass(klass_RInfo, obj);
    if (k->is_loaded()) {
      // See if we get an immediate positive hit
      __ ldr(rscratch1, Address(klass_RInfo, int64_t(k->super_check_offset())));
      __ cmp(k_RInfo, rscratch1);
      if ((juint)in_bytes(Klass::secondary_super_cache_offset()) != k->super_check_offset()) {
        __ br(Assembler::NE, *failure_target);
        // successful cast, fall through to profile or jump
      } else {
        // See if we get an immediate positive hit
        __ br(Assembler::EQ, *success_target);
        // check for self
        __ cmp(klass_RInfo, k_RInfo);
        __ br(Assembler::EQ, *success_target);

        __ stp(klass_RInfo, k_RInfo, Address(__ pre(sp, -2 * wordSize)));
        __ far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
        __ ldr(klass_RInfo, Address(__ post(sp, 2 * wordSize)));
        // result is a boolean
        __ cbzw(klass_RInfo, *failure_target);
        // successful cast, fall through to profile or jump
      }
    } else {
      // perform the fast part of the checking logic
      __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, success_target, failure_target, NULL);
      // call out-of-line instance of __ check_klass_subtype_slow_path(...):
      __ stp(klass_RInfo, k_RInfo, Address(__ pre(sp, -2 * wordSize)));
      __ far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
      __ ldp(k_RInfo, klass_RInfo, Address(__ post(sp, 2 * wordSize)));
      // result is a boolean
      __ cbz(k_RInfo, *failure_target);
      // successful cast, fall through to profile or jump
    }
  }
  if (should_profile) {
    Register mdo  = klass_RInfo, recv = k_RInfo;
    __ bind(profile_cast_success);
    __ mov_metadata(mdo, md->constant_encoding());
    __ load_klass(recv, obj);
    Label update_done;
    type_profile_helper(mdo, md, data, recv, success);
    __ b(*success);

    __ bind(profile_cast_failure);
    __ mov_metadata(mdo, md->constant_encoding());
    Address counter_addr
      = __ form_address(rscratch2, mdo,
                        md->byte_offset_of_slot(data, CounterData::count_offset()),
                        0);
    __ ldr(rscratch1, counter_addr);
    __ sub(rscratch1, rscratch1, DataLayout::counter_increment);
    __ str(rscratch1, counter_addr);
    __ b(*failure);
  }
  __ b(*success);
}


void LIR_Assembler::emit_opTypeCheck(LIR_OpTypeCheck* op) {
  const bool should_profile = op->should_profile();

  LIR_Code code = op->code();
  if (code == lir_store_check) {
    Register value = op->object()->as_register();
    Register array = op->array()->as_register();
    Register k_RInfo = op->tmp1()->as_register();
    Register klass_RInfo = op->tmp2()->as_register();
    Register Rtmp1 = op->tmp3()->as_register();

    CodeStub* stub = op->stub();

    // check if it needs to be profiled
    ciMethodData* md;
    ciProfileData* data;

    if (should_profile) {
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
    Label *success_target = should_profile ? &profile_cast_success : &done;
    Label *failure_target = should_profile ? &profile_cast_failure : stub->entry();

    if (should_profile) {
      Label not_null;
      __ cbnz(value, not_null);
      // Object is null; update MDO and exit
      Register mdo  = klass_RInfo;
      __ mov_metadata(mdo, md->constant_encoding());
      Address data_addr
        = __ form_address(rscratch2, mdo,
                          md->byte_offset_of_slot(data, DataLayout::flags_offset()),
                          0);
      __ ldrb(rscratch1, data_addr);
      __ orr(rscratch1, rscratch1, BitData::null_seen_byte_constant());
      __ strb(rscratch1, data_addr);
      __ b(done);
      __ bind(not_null);
    } else {
      __ cbz(value, done);
    }

    add_debug_info_for_null_check_here(op->info_for_exception());
    __ load_klass(k_RInfo, array);
    __ load_klass(klass_RInfo, value);

    // get instance klass (it's already uncompressed)
    __ ldr(k_RInfo, Address(k_RInfo, ObjArrayKlass::element_klass_offset()));
    // perform the fast part of the checking logic
    __ check_klass_subtype_fast_path(klass_RInfo, k_RInfo, Rtmp1, success_target, failure_target, NULL);
    // call out-of-line instance of __ check_klass_subtype_slow_path(...):
    __ stp(klass_RInfo, k_RInfo, Address(__ pre(sp, -2 * wordSize)));
    __ far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
    __ ldp(k_RInfo, klass_RInfo, Address(__ post(sp, 2 * wordSize)));
    // result is a boolean
    __ cbzw(k_RInfo, *failure_target);
    // fall through to the success case

    if (should_profile) {
      Register mdo  = klass_RInfo, recv = k_RInfo;
      __ bind(profile_cast_success);
      __ mov_metadata(mdo, md->constant_encoding());
      __ load_klass(recv, value);
      Label update_done;
      type_profile_helper(mdo, md, data, recv, &done);
      __ b(done);

      __ bind(profile_cast_failure);
      __ mov_metadata(mdo, md->constant_encoding());
      Address counter_addr(mdo, md->byte_offset_of_slot(data, CounterData::count_offset()));
      __ lea(rscratch2, counter_addr);
      __ ldr(rscratch1, Address(rscratch2));
      __ sub(rscratch1, rscratch1, DataLayout::counter_increment);
      __ str(rscratch1, Address(rscratch2));
      __ b(*stub->entry());
    }

    __ bind(done);
  } else if (code == lir_checkcast) {
    Register obj = op->object()->as_register();
    Register dst = op->result_opr()->as_register();
    Label success;
    emit_typecheck_helper(op, &success, op->stub()->entry(), &success);
    __ bind(success);
    if (dst != obj) {
      __ mov(dst, obj);
    }
  } else if (code == lir_instanceof) {
    Register obj = op->object()->as_register();
    Register dst = op->result_opr()->as_register();
    Label success, failure, done;
    emit_typecheck_helper(op, &success, &failure, &failure);
    __ bind(failure);
    __ mov(dst, zr);
    __ b(done);
    __ bind(success);
    __ mov(dst, 1);
    __ bind(done);
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::casw(Register addr, Register newval, Register cmpval) {
  __ cmpxchg(addr, cmpval, newval, Assembler::word, /* acquire*/ true, /* release*/ true, /* weak*/ false, rscratch1);
  __ cset(rscratch1, Assembler::NE);
  __ membar(__ AnyAny);
}

void LIR_Assembler::casl(Register addr, Register newval, Register cmpval) {
  __ cmpxchg(addr, cmpval, newval, Assembler::xword, /* acquire*/ true, /* release*/ true, /* weak*/ false, rscratch1);
  __ cset(rscratch1, Assembler::NE);
  __ membar(__ AnyAny);
}


void LIR_Assembler::emit_compare_and_swap(LIR_OpCompareAndSwap* op) {
  assert(VM_Version::supports_cx8(), "wrong machine");
  Register addr;
  if (op->addr()->is_register()) {
    addr = as_reg(op->addr());
  } else {
    assert(op->addr()->is_address(), "what else?");
    LIR_Address* addr_ptr = op->addr()->as_address_ptr();
    assert(addr_ptr->disp() == 0, "need 0 disp");
    assert(addr_ptr->index() == LIR_OprDesc::illegalOpr(), "need 0 index");
    addr = as_reg(addr_ptr->base());
  }
  Register newval = as_reg(op->new_value());
  Register cmpval = as_reg(op->cmp_value());

  if (op->code() == lir_cas_obj) {
    if (UseCompressedOops) {
      Register t1 = op->tmp1()->as_register();
      assert(op->tmp1()->is_valid(), "must be");
      __ encode_heap_oop(t1, cmpval);
      cmpval = t1;
      __ encode_heap_oop(rscratch2, newval);
      newval = rscratch2;
      casw(addr, newval, cmpval);
    } else {
      casl(addr, newval, cmpval);
    }
  } else if (op->code() == lir_cas_int) {
    casw(addr, newval, cmpval);
  } else {
    casl(addr, newval, cmpval);
  }
}


void LIR_Assembler::cmove(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, BasicType type) {

  Assembler::Condition acond, ncond;
  switch (condition) {
  case lir_cond_equal:        acond = Assembler::EQ; ncond = Assembler::NE; break;
  case lir_cond_notEqual:     acond = Assembler::NE; ncond = Assembler::EQ; break;
  case lir_cond_less:         acond = Assembler::LT; ncond = Assembler::GE; break;
  case lir_cond_lessEqual:    acond = Assembler::LE; ncond = Assembler::GT; break;
  case lir_cond_greaterEqual: acond = Assembler::GE; ncond = Assembler::LT; break;
  case lir_cond_greater:      acond = Assembler::GT; ncond = Assembler::LE; break;
  case lir_cond_belowEqual:
  case lir_cond_aboveEqual:
  default:                    ShouldNotReachHere();
    acond = Assembler::EQ; ncond = Assembler::NE;  // unreachable
  }

  assert(result->is_single_cpu() || result->is_double_cpu(),
         "expect single register for result");
  if (opr1->is_constant() && opr2->is_constant()
      && opr1->type() == T_INT && opr2->type() == T_INT) {
    jint val1 = opr1->as_jint();
    jint val2 = opr2->as_jint();
    if (val1 == 0 && val2 == 1) {
      __ cset(result->as_register(), ncond);
      return;
    } else if (val1 == 1 && val2 == 0) {
      __ cset(result->as_register(), acond);
      return;
    }
  }

  if (opr1->is_constant() && opr2->is_constant()
      && opr1->type() == T_LONG && opr2->type() == T_LONG) {
    jlong val1 = opr1->as_jlong();
    jlong val2 = opr2->as_jlong();
    if (val1 == 0 && val2 == 1) {
      __ cset(result->as_register_lo(), ncond);
      return;
    } else if (val1 == 1 && val2 == 0) {
      __ cset(result->as_register_lo(), acond);
      return;
    }
  }

  if (opr1->is_stack()) {
    stack2reg(opr1, FrameMap::rscratch1_opr, result->type());
    opr1 = FrameMap::rscratch1_opr;
  } else if (opr1->is_constant()) {
    LIR_Opr tmp
      = opr1->type() == T_LONG ? FrameMap::rscratch1_long_opr : FrameMap::rscratch1_opr;
    const2reg(opr1, tmp, lir_patch_none, NULL);
    opr1 = tmp;
  }

  if (opr2->is_stack()) {
    stack2reg(opr2, FrameMap::rscratch2_opr, result->type());
    opr2 = FrameMap::rscratch2_opr;
  } else if (opr2->is_constant()) {
    LIR_Opr tmp
      = opr2->type() == T_LONG ? FrameMap::rscratch2_long_opr : FrameMap::rscratch2_opr;
    const2reg(opr2, tmp, lir_patch_none, NULL);
    opr2 = tmp;
  }

  if (result->type() == T_LONG)
    __ csel(result->as_register_lo(), opr1->as_register_lo(), opr2->as_register_lo(), acond);
  else
    __ csel(result->as_register(), opr1->as_register(), opr2->as_register(), acond);
}

void LIR_Assembler::arith_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dest, CodeEmitInfo* info, bool pop_fpu_stack) {
  assert(info == NULL, "should never be used, idiv/irem and ldiv/lrem not handled by this method");

  if (left->is_single_cpu()) {
    Register lreg = left->as_register();
    Register dreg = as_reg(dest);

    if (right->is_single_cpu()) {
      // cpu register - cpu register

      assert(left->type() == T_INT && right->type() == T_INT && dest->type() == T_INT,
             "should be");
      Register rreg = right->as_register();
      switch (code) {
      case lir_add: __ addw (dest->as_register(), lreg, rreg); break;
      case lir_sub: __ subw (dest->as_register(), lreg, rreg); break;
      case lir_mul: __ mulw (dest->as_register(), lreg, rreg); break;
      default:      ShouldNotReachHere();
      }

    } else if (right->is_double_cpu()) {
      Register rreg = right->as_register_lo();
      // single_cpu + double_cpu: can happen with obj+long
      assert(code == lir_add || code == lir_sub, "mismatched arithmetic op");
      switch (code) {
      case lir_add: __ add(dreg, lreg, rreg); break;
      case lir_sub: __ sub(dreg, lreg, rreg); break;
      default: ShouldNotReachHere();
      }
    } else if (right->is_constant()) {
      // cpu register - constant
      jlong c;

      // FIXME.  This is fugly: we really need to factor all this logic.
      switch(right->type()) {
      case T_LONG:
        c = right->as_constant_ptr()->as_jlong();
        break;
      case T_INT:
      case T_ADDRESS:
        c = right->as_constant_ptr()->as_jint();
        break;
      default:
        ShouldNotReachHere();
        c = 0;  // unreachable
        break;
      }

      assert(code == lir_add || code == lir_sub, "mismatched arithmetic op");
      if (c == 0 && dreg == lreg) {
        COMMENT("effective nop elided");
        return;
      }
      switch(left->type()) {
      case T_INT:
        switch (code) {
        case lir_add: __ addw(dreg, lreg, c); break;
        case lir_sub: __ subw(dreg, lreg, c); break;
        default: ShouldNotReachHere();
        }
        break;
      case T_OBJECT:
      case T_ADDRESS:
        switch (code) {
        case lir_add: __ add(dreg, lreg, c); break;
        case lir_sub: __ sub(dreg, lreg, c); break;
        default: ShouldNotReachHere();
        }
        break;
      default:
        ShouldNotReachHere();
      }
    } else {
      ShouldNotReachHere();
    }

  } else if (left->is_double_cpu()) {
    Register lreg_lo = left->as_register_lo();

    if (right->is_double_cpu()) {
      // cpu register - cpu register
      Register rreg_lo = right->as_register_lo();
      switch (code) {
      case lir_add: __ add (dest->as_register_lo(), lreg_lo, rreg_lo); break;
      case lir_sub: __ sub (dest->as_register_lo(), lreg_lo, rreg_lo); break;
      case lir_mul: __ mul (dest->as_register_lo(), lreg_lo, rreg_lo); break;
      case lir_div: __ corrected_idivq(dest->as_register_lo(), lreg_lo, rreg_lo, false, rscratch1); break;
      case lir_rem: __ corrected_idivq(dest->as_register_lo(), lreg_lo, rreg_lo, true, rscratch1); break;
      default:
        ShouldNotReachHere();
      }

    } else if (right->is_constant()) {
      jlong c = right->as_constant_ptr()->as_jlong();
      Register dreg = as_reg(dest);
      switch (code) {
        case lir_add:
        case lir_sub:
          if (c == 0 && dreg == lreg_lo) {
            COMMENT("effective nop elided");
            return;
          }
          code == lir_add ? __ add(dreg, lreg_lo, c) : __ sub(dreg, lreg_lo, c);
          break;
        case lir_div:
          assert(c > 0 && is_power_of_2(c), "divisor must be power-of-2 constant");
          if (c == 1) {
            // move lreg_lo to dreg if divisor is 1
            __ mov(dreg, lreg_lo);
          } else {
            unsigned int shift = log2i_exact(c);
            // use rscratch1 as intermediate result register
            __ asr(rscratch1, lreg_lo, 63);
            __ add(rscratch1, lreg_lo, rscratch1, Assembler::LSR, 64 - shift);
            __ asr(dreg, rscratch1, shift);
          }
          break;
        case lir_rem:
          assert(c > 0 && is_power_of_2(c), "divisor must be power-of-2 constant");
          if (c == 1) {
            // move 0 to dreg if divisor is 1
            __ mov(dreg, zr);
          } else {
            // use rscratch1 as intermediate result register
            __ negs(rscratch1, lreg_lo);
            __ andr(dreg, lreg_lo, c - 1);
            __ andr(rscratch1, rscratch1, c - 1);
            __ csneg(dreg, dreg, rscratch1, Assembler::MI);
          }
          break;
        default:
          ShouldNotReachHere();
      }
    } else {
      ShouldNotReachHere();
    }
  } else if (left->is_single_fpu()) {
    assert(right->is_single_fpu(), "right hand side of float arithmetics needs to be float register");
    switch (code) {
    case lir_add: __ fadds (dest->as_float_reg(), left->as_float_reg(), right->as_float_reg()); break;
    case lir_sub: __ fsubs (dest->as_float_reg(), left->as_float_reg(), right->as_float_reg()); break;
    case lir_mul: __ fmuls (dest->as_float_reg(), left->as_float_reg(), right->as_float_reg()); break;
    case lir_div: __ fdivs (dest->as_float_reg(), left->as_float_reg(), right->as_float_reg()); break;
    default:
      ShouldNotReachHere();
    }
  } else if (left->is_double_fpu()) {
    if (right->is_double_fpu()) {
      // fpu register - fpu register
      switch (code) {
      case lir_add: __ faddd (dest->as_double_reg(), left->as_double_reg(), right->as_double_reg()); break;
      case lir_sub: __ fsubd (dest->as_double_reg(), left->as_double_reg(), right->as_double_reg()); break;
      case lir_mul: __ fmuld (dest->as_double_reg(), left->as_double_reg(), right->as_double_reg()); break;
      case lir_div: __ fdivd (dest->as_double_reg(), left->as_double_reg(), right->as_double_reg()); break;
      default:
        ShouldNotReachHere();
      }
    } else {
      if (right->is_constant()) {
        ShouldNotReachHere();
      }
      ShouldNotReachHere();
    }
  } else if (left->is_single_stack() || left->is_address()) {
    assert(left == dest, "left and dest must be equal");
    ShouldNotReachHere();
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::arith_fpu_implementation(LIR_Code code, int left_index, int right_index, int dest_index, bool pop_fpu_stack) { Unimplemented(); }


void LIR_Assembler::intrinsic_op(LIR_Code code, LIR_Opr value, LIR_Opr unused, LIR_Opr dest, LIR_Op* op) {
  switch(code) {
  case lir_abs : __ fabsd(dest->as_double_reg(), value->as_double_reg()); break;
  case lir_sqrt: __ fsqrtd(dest->as_double_reg(), value->as_double_reg()); break;
  default      : ShouldNotReachHere();
  }
}

void LIR_Assembler::logic_op(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst) {

  assert(left->is_single_cpu() || left->is_double_cpu(), "expect single or double register");
  Register Rleft = left->is_single_cpu() ? left->as_register() :
                                           left->as_register_lo();
   if (dst->is_single_cpu()) {
     Register Rdst = dst->as_register();
     if (right->is_constant()) {
       switch (code) {
         case lir_logic_and: __ andw (Rdst, Rleft, right->as_jint()); break;
         case lir_logic_or:  __ orrw (Rdst, Rleft, right->as_jint()); break;
         case lir_logic_xor: __ eorw (Rdst, Rleft, right->as_jint()); break;
         default: ShouldNotReachHere(); break;
       }
     } else {
       Register Rright = right->is_single_cpu() ? right->as_register() :
                                                  right->as_register_lo();
       switch (code) {
         case lir_logic_and: __ andw (Rdst, Rleft, Rright); break;
         case lir_logic_or:  __ orrw (Rdst, Rleft, Rright); break;
         case lir_logic_xor: __ eorw (Rdst, Rleft, Rright); break;
         default: ShouldNotReachHere(); break;
       }
     }
   } else {
     Register Rdst = dst->as_register_lo();
     if (right->is_constant()) {
       switch (code) {
         case lir_logic_and: __ andr (Rdst, Rleft, right->as_jlong()); break;
         case lir_logic_or:  __ orr (Rdst, Rleft, right->as_jlong()); break;
         case lir_logic_xor: __ eor (Rdst, Rleft, right->as_jlong()); break;
         default: ShouldNotReachHere(); break;
       }
     } else {
       Register Rright = right->is_single_cpu() ? right->as_register() :
                                                  right->as_register_lo();
       switch (code) {
         case lir_logic_and: __ andr (Rdst, Rleft, Rright); break;
         case lir_logic_or:  __ orr (Rdst, Rleft, Rright); break;
         case lir_logic_xor: __ eor (Rdst, Rleft, Rright); break;
         default: ShouldNotReachHere(); break;
       }
     }
   }
}



void LIR_Assembler::arithmetic_idiv(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr illegal, LIR_Opr result, CodeEmitInfo* info) {

  // opcode check
  assert((code == lir_idiv) || (code == lir_irem), "opcode must be idiv or irem");
  bool is_irem = (code == lir_irem);

  // operand check
  assert(left->is_single_cpu(),   "left must be register");
  assert(right->is_single_cpu() || right->is_constant(),  "right must be register or constant");
  assert(result->is_single_cpu(), "result must be register");
  Register lreg = left->as_register();
  Register dreg = result->as_register();

  // power-of-2 constant check and codegen
  if (right->is_constant()) {
    int c = right->as_constant_ptr()->as_jint();
    assert(c > 0 && is_power_of_2(c), "divisor must be power-of-2 constant");
    if (is_irem) {
      if (c == 1) {
        // move 0 to dreg if divisor is 1
        __ movw(dreg, zr);
      } else {
        // use rscratch1 as intermediate result register
        __ negsw(rscratch1, lreg);
        __ andw(dreg, lreg, c - 1);
        __ andw(rscratch1, rscratch1, c - 1);
        __ csnegw(dreg, dreg, rscratch1, Assembler::MI);
      }
    } else {
      if (c == 1) {
        // move lreg to dreg if divisor is 1
        __ movw(dreg, lreg);
      } else {
        unsigned int shift = exact_log2(c);
        // use rscratch1 as intermediate result register
        __ asrw(rscratch1, lreg, 31);
        __ addw(rscratch1, lreg, rscratch1, Assembler::LSR, 32 - shift);
        __ asrw(dreg, rscratch1, shift);
      }
    }
  } else {
    Register rreg = right->as_register();
    __ corrected_idivl(dreg, lreg, rreg, is_irem, rscratch1);
  }
}


void LIR_Assembler::comp_op(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Op2* op) {
  if (opr1->is_constant() && opr2->is_single_cpu()) {
    // tableswitch
    Register reg = as_reg(opr2);
    struct tableswitch &table = switches[opr1->as_constant_ptr()->as_jint()];
    __ tableswitch(reg, table._first_key, table._last_key, table._branches, table._after);
  } else if (opr1->is_single_cpu() || opr1->is_double_cpu()) {
    Register reg1 = as_reg(opr1);
    if (opr2->is_single_cpu()) {
      // cpu register - cpu register
      Register reg2 = opr2->as_register();
      if (is_reference_type(opr1->type())) {
        __ cmpoop(reg1, reg2);
      } else {
        assert(!is_reference_type(opr2->type()), "cmp int, oop?");
        __ cmpw(reg1, reg2);
      }
      return;
    }
    if (opr2->is_double_cpu()) {
      // cpu register - cpu register
      Register reg2 = opr2->as_register_lo();
      __ cmp(reg1, reg2);
      return;
    }

    if (opr2->is_constant()) {
      bool is_32bit = false; // width of register operand
      jlong imm;

      switch(opr2->type()) {
      case T_INT:
        imm = opr2->as_constant_ptr()->as_jint();
        is_32bit = true;
        break;
      case T_LONG:
        imm = opr2->as_constant_ptr()->as_jlong();
        break;
      case T_ADDRESS:
        imm = opr2->as_constant_ptr()->as_jint();
        break;
      case T_METADATA:
        imm = (intptr_t)(opr2->as_constant_ptr()->as_metadata());
        break;
      case T_OBJECT:
      case T_ARRAY:
        jobject2reg(opr2->as_constant_ptr()->as_jobject(), rscratch1);
        __ cmpoop(reg1, rscratch1);
        return;
      default:
        ShouldNotReachHere();
        imm = 0;  // unreachable
        break;
      }

      if (Assembler::operand_valid_for_add_sub_immediate(imm)) {
        if (is_32bit)
          __ cmpw(reg1, imm);
        else
          __ subs(zr, reg1, imm);
        return;
      } else {
        __ mov(rscratch1, imm);
        if (is_32bit)
          __ cmpw(reg1, rscratch1);
        else
          __ cmp(reg1, rscratch1);
        return;
      }
    } else
      ShouldNotReachHere();
  } else if (opr1->is_single_fpu()) {
    FloatRegister reg1 = opr1->as_float_reg();
    assert(opr2->is_single_fpu(), "expect single float register");
    FloatRegister reg2 = opr2->as_float_reg();
    __ fcmps(reg1, reg2);
  } else if (opr1->is_double_fpu()) {
    FloatRegister reg1 = opr1->as_double_reg();
    assert(opr2->is_double_fpu(), "expect double float register");
    FloatRegister reg2 = opr2->as_double_reg();
    __ fcmpd(reg1, reg2);
  } else {
    ShouldNotReachHere();
  }
}

void LIR_Assembler::comp_fl2i(LIR_Code code, LIR_Opr left, LIR_Opr right, LIR_Opr dst, LIR_Op2* op){
  if (code == lir_cmp_fd2i || code == lir_ucmp_fd2i) {
    bool is_unordered_less = (code == lir_ucmp_fd2i);
    if (left->is_single_fpu()) {
      __ float_cmp(true, is_unordered_less ? -1 : 1, left->as_float_reg(), right->as_float_reg(), dst->as_register());
    } else if (left->is_double_fpu()) {
      __ float_cmp(false, is_unordered_less ? -1 : 1, left->as_double_reg(), right->as_double_reg(), dst->as_register());
    } else {
      ShouldNotReachHere();
    }
  } else if (code == lir_cmp_l2i) {
    Label done;
    __ cmp(left->as_register_lo(), right->as_register_lo());
    __ mov(dst->as_register(), (uint64_t)-1L);
    __ br(Assembler::LT, done);
    __ csinc(dst->as_register(), zr, zr, Assembler::EQ);
    __ bind(done);
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::align_call(LIR_Code code) {  }


void LIR_Assembler::call(LIR_OpJavaCall* op, relocInfo::relocType rtype) {
  address call = __ trampoline_call(Address(op->addr(), rtype));
  if (call == NULL) {
    bailout("trampoline stub overflow");
    return;
  }
  add_call_info(code_offset(), op->info());
}


void LIR_Assembler::ic_call(LIR_OpJavaCall* op) {
  address call = __ ic_call(op->addr());
  if (call == NULL) {
    bailout("trampoline stub overflow");
    return;
  }
  add_call_info(code_offset(), op->info());
}

void LIR_Assembler::emit_static_call_stub() {
  address call_pc = __ pc();
  address stub = __ start_a_stub(call_stub_size());
  if (stub == NULL) {
    bailout("static call stub overflow");
    return;
  }

  int start = __ offset();

  __ relocate(static_stub_Relocation::spec(call_pc));
  __ emit_static_call_stub();

  assert(__ offset() - start + CompiledStaticCall::to_trampoline_stub_size()
        <= call_stub_size(), "stub too big");
  __ end_a_stub();
}


void LIR_Assembler::throw_op(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info) {
  assert(exceptionOop->as_register() == r0, "must match");
  assert(exceptionPC->as_register() == r3, "must match");

  // exception object is not added to oop map by LinearScan
  // (LinearScan assumes that no oops are in fixed registers)
  info->add_register_oop(exceptionOop);
  Runtime1::StubID unwind_id;

  // get current pc information
  // pc is only needed if the method has an exception handler, the unwind code does not need it.
  if (compilation()->debug_info_recorder()->last_pc_offset() == __ offset()) {
    // As no instructions have been generated yet for this LIR node it's
    // possible that an oop map already exists for the current offset.
    // In that case insert an dummy NOP here to ensure all oop map PCs
    // are unique. See JDK-8237483.
    __ nop();
  }
  int pc_for_athrow_offset = __ offset();
  InternalAddress pc_for_athrow(__ pc());
  __ adr(exceptionPC->as_register(), pc_for_athrow);
  add_call_info(pc_for_athrow_offset, info); // for exception handler

  __ verify_not_null_oop(r0);
  // search an exception handler (r0: exception oop, r3: throwing pc)
  if (compilation()->has_fpu_code()) {
    unwind_id = Runtime1::handle_exception_id;
  } else {
    unwind_id = Runtime1::handle_exception_nofpu_id;
  }
  __ far_call(RuntimeAddress(Runtime1::entry_for(unwind_id)));

  // FIXME: enough room for two byte trap   ????
  __ nop();
}


void LIR_Assembler::unwind_op(LIR_Opr exceptionOop) {
  assert(exceptionOop->as_register() == r0, "must match");

  __ b(_unwind_handler_entry);
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, LIR_Opr count, LIR_Opr dest, LIR_Opr tmp) {
  Register lreg = left->is_single_cpu() ? left->as_register() : left->as_register_lo();
  Register dreg = dest->is_single_cpu() ? dest->as_register() : dest->as_register_lo();

  switch (left->type()) {
    case T_INT: {
      switch (code) {
      case lir_shl:  __ lslvw (dreg, lreg, count->as_register()); break;
      case lir_shr:  __ asrvw (dreg, lreg, count->as_register()); break;
      case lir_ushr: __ lsrvw (dreg, lreg, count->as_register()); break;
      default:
        ShouldNotReachHere();
        break;
      }
      break;
    case T_LONG:
    case T_ADDRESS:
    case T_OBJECT:
      switch (code) {
      case lir_shl:  __ lslv (dreg, lreg, count->as_register()); break;
      case lir_shr:  __ asrv (dreg, lreg, count->as_register()); break;
      case lir_ushr: __ lsrv (dreg, lreg, count->as_register()); break;
      default:
        ShouldNotReachHere();
        break;
      }
      break;
    default:
      ShouldNotReachHere();
      break;
    }
  }
}


void LIR_Assembler::shift_op(LIR_Code code, LIR_Opr left, jint count, LIR_Opr dest) {
  Register dreg = dest->is_single_cpu() ? dest->as_register() : dest->as_register_lo();
  Register lreg = left->is_single_cpu() ? left->as_register() : left->as_register_lo();

  switch (left->type()) {
    case T_INT: {
      switch (code) {
      case lir_shl:  __ lslw (dreg, lreg, count); break;
      case lir_shr:  __ asrw (dreg, lreg, count); break;
      case lir_ushr: __ lsrw (dreg, lreg, count); break;
      default:
        ShouldNotReachHere();
        break;
      }
      break;
    case T_LONG:
    case T_ADDRESS:
    case T_OBJECT:
      switch (code) {
      case lir_shl:  __ lsl (dreg, lreg, count); break;
      case lir_shr:  __ asr (dreg, lreg, count); break;
      case lir_ushr: __ lsr (dreg, lreg, count); break;
      default:
        ShouldNotReachHere();
        break;
      }
      break;
    default:
      ShouldNotReachHere();
      break;
    }
  }
}


void LIR_Assembler::store_parameter(Register r, int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ str (r, Address(sp, offset_from_rsp_in_bytes));
}


void LIR_Assembler::store_parameter(jint c,     int offset_from_rsp_in_words) {
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ mov (rscratch1, c);
  __ str (rscratch1, Address(sp, offset_from_rsp_in_bytes));
}


void LIR_Assembler::store_parameter(jobject o,  int offset_from_rsp_in_words) {
  ShouldNotReachHere();
  assert(offset_from_rsp_in_words >= 0, "invalid offset from rsp");
  int offset_from_rsp_in_bytes = offset_from_rsp_in_words * BytesPerWord;
  assert(offset_from_rsp_in_bytes < frame_map()->reserved_argument_area_size(), "invalid offset");
  __ lea(rscratch1, __ constant_oop_address(o));
  __ str(rscratch1, Address(sp, offset_from_rsp_in_bytes));
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

  CodeStub* stub = op->stub();
  int flags = op->flags();
  BasicType basic_type = default_type != NULL ? default_type->element_type()->basic_type() : T_ILLEGAL;
  if (is_reference_type(basic_type)) basic_type = T_OBJECT;

  // if we don't know anything, just go through the generic arraycopy
  if (default_type == NULL // || basic_type == T_OBJECT
      ) {
    Label done;
    assert(src == r1 && src_pos == r2, "mismatch in calling convention");

    // Save the arguments in case the generic arraycopy fails and we
    // have to fall back to the JNI stub
    __ stp(dst,     dst_pos, Address(sp, 0*BytesPerWord));
    __ stp(length,  src_pos, Address(sp, 2*BytesPerWord));
    __ str(src,              Address(sp, 4*BytesPerWord));

    address copyfunc_addr = StubRoutines::generic_arraycopy();
    assert(copyfunc_addr != NULL, "generic arraycopy stub required");

    // The arguments are in java calling convention so we shift them
    // to C convention
    assert_different_registers(c_rarg0, j_rarg1, j_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg0, j_rarg0);
    assert_different_registers(c_rarg1, j_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg1, j_rarg1);
    assert_different_registers(c_rarg2, j_rarg3, j_rarg4);
    __ mov(c_rarg2, j_rarg2);
    assert_different_registers(c_rarg3, j_rarg4);
    __ mov(c_rarg3, j_rarg3);
    __ mov(c_rarg4, j_rarg4);
#ifndef PRODUCT
    if (PrintC1Statistics) {
      __ incrementw(ExternalAddress((address)&Runtime1::_generic_arraycopystub_cnt));
    }
#endif
    __ far_call(RuntimeAddress(copyfunc_addr));

    __ cbz(r0, *stub->continuation());

    // Reload values from the stack so they are where the stub
    // expects them.
    __ ldp(dst,     dst_pos, Address(sp, 0*BytesPerWord));
    __ ldp(length,  src_pos, Address(sp, 2*BytesPerWord));
    __ ldr(src,              Address(sp, 4*BytesPerWord));

    // r0 is -1^K where K == partial copied count
    __ eonw(rscratch1, r0, zr);
    // adjust length down and src/end pos up by partial copied count
    __ subw(length, length, rscratch1);
    __ addw(src_pos, src_pos, rscratch1);
    __ addw(dst_pos, dst_pos, rscratch1);
    __ b(*stub->entry());

    __ bind(*stub->continuation());
    return;
  }

  assert(default_type != NULL && default_type->is_array_klass() && default_type->is_loaded(), "must be true at this point");

  int elem_size = type2aelembytes(basic_type);
  int scale = exact_log2(elem_size);

  Address src_length_addr = Address(src, arrayOopDesc::length_offset_in_bytes());
  Address dst_length_addr = Address(dst, arrayOopDesc::length_offset_in_bytes());
  Address src_klass_addr = Address(src, oopDesc::klass_offset_in_bytes());
  Address dst_klass_addr = Address(dst, oopDesc::klass_offset_in_bytes());

  // test for NULL
  if (flags & LIR_OpArrayCopy::src_null_check) {
    __ cbz(src, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_null_check) {
    __ cbz(dst, *stub->entry());
  }

  // If the compiler was not able to prove that exact type of the source or the destination
  // of the arraycopy is an array type, check at runtime if the source or the destination is
  // an instance type.
  if (flags & LIR_OpArrayCopy::type_check) {
    if (!(flags & LIR_OpArrayCopy::LIR_OpArrayCopy::dst_objarray)) {
      __ load_klass(tmp, dst);
      __ ldrw(rscratch1, Address(tmp, in_bytes(Klass::layout_helper_offset())));
      __ cmpw(rscratch1, Klass::_lh_neutral_value);
      __ br(Assembler::GE, *stub->entry());
    }

    if (!(flags & LIR_OpArrayCopy::LIR_OpArrayCopy::src_objarray)) {
      __ load_klass(tmp, src);
      __ ldrw(rscratch1, Address(tmp, in_bytes(Klass::layout_helper_offset())));
      __ cmpw(rscratch1, Klass::_lh_neutral_value);
      __ br(Assembler::GE, *stub->entry());
    }
  }

  // check if negative
  if (flags & LIR_OpArrayCopy::src_pos_positive_check) {
    __ cmpw(src_pos, 0);
    __ br(Assembler::LT, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_pos_positive_check) {
    __ cmpw(dst_pos, 0);
    __ br(Assembler::LT, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::length_positive_check) {
    __ cmpw(length, 0);
    __ br(Assembler::LT, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::src_range_check) {
    __ addw(tmp, src_pos, length);
    __ ldrw(rscratch1, src_length_addr);
    __ cmpw(tmp, rscratch1);
    __ br(Assembler::HI, *stub->entry());
  }
  if (flags & LIR_OpArrayCopy::dst_range_check) {
    __ addw(tmp, dst_pos, length);
    __ ldrw(rscratch1, dst_length_addr);
    __ cmpw(tmp, rscratch1);
    __ br(Assembler::HI, *stub->entry());
  }

  if (flags & LIR_OpArrayCopy::type_check) {
    // We don't know the array types are compatible
    if (basic_type != T_OBJECT) {
      // Simple test for basic type arrays
      if (UseCompressedClassPointers) {
        __ ldrw(tmp, src_klass_addr);
        __ ldrw(rscratch1, dst_klass_addr);
        __ cmpw(tmp, rscratch1);
      } else {
        __ ldr(tmp, src_klass_addr);
        __ ldr(rscratch1, dst_klass_addr);
        __ cmp(tmp, rscratch1);
      }
      __ br(Assembler::NE, *stub->entry());
    } else {
      // For object arrays, if src is a sub class of dst then we can
      // safely do the copy.
      Label cont, slow;

#define PUSH(r1, r2)                                    \
      stp(r1, r2, __ pre(sp, -2 * wordSize));

#define POP(r1, r2)                                     \
      ldp(r1, r2, __ post(sp, 2 * wordSize));

      __ PUSH(src, dst);

      __ load_klass(src, src);
      __ load_klass(dst, dst);

      __ check_klass_subtype_fast_path(src, dst, tmp, &cont, &slow, NULL);

      __ PUSH(src, dst);
      __ far_call(RuntimeAddress(Runtime1::entry_for(Runtime1::slow_subtype_check_id)));
      __ POP(src, dst);

      __ cbnz(src, cont);

      __ bind(slow);
      __ POP(src, dst);

      address copyfunc_addr = StubRoutines::checkcast_arraycopy();
      if (copyfunc_addr != NULL) { // use stub if available
        // src is not a sub class of dst so we have to do a
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
          int lh_offset = in_bytes(Klass::layout_helper_offset());
          Address klass_lh_addr(tmp, lh_offset);
          jint objArray_lh = Klass::array_layout_helper(T_OBJECT);
          __ ldrw(rscratch1, klass_lh_addr);
          __ mov(rscratch2, objArray_lh);
          __ eorw(rscratch1, rscratch1, rscratch2);
          __ cbnzw(rscratch1, *stub->entry());
        }

       // Spill because stubs can use any register they like and it's
       // easier to restore just those that we care about.
        __ stp(dst,     dst_pos, Address(sp, 0*BytesPerWord));
        __ stp(length,  src_pos, Address(sp, 2*BytesPerWord));
        __ str(src,              Address(sp, 4*BytesPerWord));

        __ lea(c_rarg0, Address(src, src_pos, Address::uxtw(scale)));
        __ add(c_rarg0, c_rarg0, arrayOopDesc::base_offset_in_bytes(basic_type));
        assert_different_registers(c_rarg0, dst, dst_pos, length);
        __ lea(c_rarg1, Address(dst, dst_pos, Address::uxtw(scale)));
        __ add(c_rarg1, c_rarg1, arrayOopDesc::base_offset_in_bytes(basic_type));
        assert_different_registers(c_rarg1, dst, length);
        __ uxtw(c_rarg2, length);
        assert_different_registers(c_rarg2, dst);

        __ load_klass(c_rarg4, dst);
        __ ldr(c_rarg4, Address(c_rarg4, ObjArrayKlass::element_klass_offset()));
        __ ldrw(c_rarg3, Address(c_rarg4, Klass::super_check_offset_offset()));
        __ far_call(RuntimeAddress(copyfunc_addr));

#ifndef PRODUCT
        if (PrintC1Statistics) {
          Label failed;
          __ cbnz(r0, failed);
          __ incrementw(ExternalAddress((address)&Runtime1::_arraycopy_checkcast_cnt));
          __ bind(failed);
        }
#endif

        __ cbz(r0, *stub->continuation());

#ifndef PRODUCT
        if (PrintC1Statistics) {
          __ incrementw(ExternalAddress((address)&Runtime1::_arraycopy_checkcast_attempt_cnt));
        }
#endif
        assert_different_registers(dst, dst_pos, length, src_pos, src, r0, rscratch1);

        // Restore previously spilled arguments
        __ ldp(dst,     dst_pos, Address(sp, 0*BytesPerWord));
        __ ldp(length,  src_pos, Address(sp, 2*BytesPerWord));
        __ ldr(src,              Address(sp, 4*BytesPerWord));

        // return value is -1^K where K is partial copied count
        __ eonw(rscratch1, r0, zr);
        // adjust length down and src/end pos up by partial copied count
        __ subw(length, length, rscratch1);
        __ addw(src_pos, src_pos, rscratch1);
        __ addw(dst_pos, dst_pos, rscratch1);
      }

      __ b(*stub->entry());

      __ bind(cont);
      __ POP(src, dst);
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
    if (UseCompressedClassPointers) {
      __ encode_klass_not_null(tmp);
    }

    if (basic_type != T_OBJECT) {

      if (UseCompressedClassPointers) {
        __ ldrw(rscratch1, dst_klass_addr);
        __ cmpw(tmp, rscratch1);
      } else {
        __ ldr(rscratch1, dst_klass_addr);
        __ cmp(tmp, rscratch1);
      }
      __ br(Assembler::NE, halt);
      if (UseCompressedClassPointers) {
        __ ldrw(rscratch1, src_klass_addr);
        __ cmpw(tmp, rscratch1);
      } else {
        __ ldr(rscratch1, src_klass_addr);
        __ cmp(tmp, rscratch1);
      }
      __ br(Assembler::EQ, known_ok);
    } else {
      if (UseCompressedClassPointers) {
        __ ldrw(rscratch1, dst_klass_addr);
        __ cmpw(tmp, rscratch1);
      } else {
        __ ldr(rscratch1, dst_klass_addr);
        __ cmp(tmp, rscratch1);
      }
      __ br(Assembler::EQ, known_ok);
      __ cmp(src, dst);
      __ br(Assembler::EQ, known_ok);
    }
    __ bind(halt);
    __ stop("incorrect type information in arraycopy");
    __ bind(known_ok);
  }
#endif

#ifndef PRODUCT
  if (PrintC1Statistics) {
    __ incrementw(ExternalAddress(Runtime1::arraycopy_count_address(basic_type)));
  }
#endif

  __ lea(c_rarg0, Address(src, src_pos, Address::uxtw(scale)));
  __ add(c_rarg0, c_rarg0, arrayOopDesc::base_offset_in_bytes(basic_type));
  assert_different_registers(c_rarg0, dst, dst_pos, length);
  __ lea(c_rarg1, Address(dst, dst_pos, Address::uxtw(scale)));
  __ add(c_rarg1, c_rarg1, arrayOopDesc::base_offset_in_bytes(basic_type));
  assert_different_registers(c_rarg1, dst, length);
  __ uxtw(c_rarg2, length);
  assert_different_registers(c_rarg2, dst);

  bool disjoint = (flags & LIR_OpArrayCopy::overlapping) == 0;
  bool aligned = (flags & LIR_OpArrayCopy::unaligned) == 0;
  const char *name;
  address entry = StubRoutines::select_arraycopy_function(basic_type, aligned, disjoint, name, false);

 CodeBlob *cb = CodeCache::find_blob(entry);
 if (cb) {
   __ far_call(RuntimeAddress(entry));
 } else {
   __ call_VM_leaf(entry, 3);
 }

  __ bind(*stub->continuation());
}




void LIR_Assembler::emit_lock(LIR_OpLock* op) {
  Register obj = op->obj_opr()->as_register();  // may not be an oop
  Register hdr = op->hdr_opr()->as_register();
  Register lock = op->lock_opr()->as_register();
  if (!UseFastLocking) {
    __ b(*op->stub()->entry());
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
          __ mov_metadata(rscratch1, known_klass->constant_encoding());
          __ lea(rscratch2, recv_addr);
          __ str(rscratch1, Address(rscratch2));
          Address data_addr(mdo, md->byte_offset_of_slot(data, VirtualCallData::receiver_count_offset(i)));
          __ addptr(data_addr, DataLayout::counter_increment);
          return;
        }
      }
    } else {
      __ load_klass(recv, recv);
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


void LIR_Assembler::emit_delay(LIR_OpDelay*) {
  Unimplemented();
}


void LIR_Assembler::monitor_address(int monitor_no, LIR_Opr dst) {
  __ lea(dst->as_register(), frame_map()->address_for_monitor_lock(monitor_no));
}

void LIR_Assembler::emit_updatecrc32(LIR_OpUpdateCRC32* op) {
  assert(op->crc()->is_single_cpu(),  "crc must be register");
  assert(op->val()->is_single_cpu(),  "byte value must be register");
  assert(op->result_opr()->is_single_cpu(), "result must be register");
  Register crc = op->crc()->as_register();
  Register val = op->val()->as_register();
  Register res = op->result_opr()->as_register();

  assert_different_registers(val, crc, res);
  uint64_t offset;
  __ adrp(res, ExternalAddress(StubRoutines::crc_table_addr()), offset);
  if (offset) __ add(res, res, offset);

  __ mvnw(crc, crc); // ~crc
  __ update_byte_crc32(crc, val, res);
  __ mvnw(res, crc); // ~crc
}

void LIR_Assembler::emit_profile_type(LIR_OpProfileType* op) {
  COMMENT("emit_profile_type {");
  Register obj = op->obj()->as_register();
  Register tmp = op->tmp()->as_pointer_register();
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
  assert(mdo_addr.base() != rscratch1, "wrong register");

  __ verify_oop(obj);

  if (tmp != obj) {
    __ mov(tmp, obj);
  }
  if (do_null) {
    __ cbnz(tmp, update);
    if (!TypeEntries::was_null_seen(current_klass)) {
      __ ldr(rscratch2, mdo_addr);
      __ orr(rscratch2, rscratch2, TypeEntries::null_seen);
      __ str(rscratch2, mdo_addr);
    }
    if (do_update) {
#ifndef ASSERT
      __ b(next);
    }
#else
      __ b(next);
    }
  } else {
    __ cbnz(tmp, update);
    __ stop("unexpected null obj");
#endif
  }

  __ bind(update);

  if (do_update) {
#ifdef ASSERT
    if (exact_klass != NULL) {
      Label ok;
      __ load_klass(tmp, tmp);
      __ mov_metadata(rscratch1, exact_klass->constant_encoding());
      __ eor(rscratch1, tmp, rscratch1);
      __ cbz(rscratch1, ok);
      __ stop("exact klass and actual klass differ");
      __ bind(ok);
    }
#endif
    if (!no_conflict) {
      if (exact_klass == NULL || TypeEntries::is_type_none(current_klass)) {
        if (exact_klass != NULL) {
          __ mov_metadata(tmp, exact_klass->constant_encoding());
        } else {
          __ load_klass(tmp, tmp);
        }

        __ ldr(rscratch2, mdo_addr);
        __ eor(tmp, tmp, rscratch2);
        __ andr(rscratch1, tmp, TypeEntries::type_klass_mask);
        // klass seen before, nothing to do. The unknown bit may have been
        // set already but no need to check.
        __ cbz(rscratch1, next);

        __ tbnz(tmp, exact_log2(TypeEntries::type_unknown), next); // already unknown. Nothing to do anymore.

        if (TypeEntries::is_type_none(current_klass)) {
          __ cbz(rscratch2, none);
          __ cmp(rscratch2, (u1)TypeEntries::null_seen);
          __ br(Assembler::EQ, none);
          // There is a chance that the checks above (re-reading profiling
          // data from memory) fail if another thread has just set the
          // profiling to this obj's klass
          __ dmb(Assembler::ISHLD);
          __ ldr(rscratch2, mdo_addr);
          __ eor(tmp, tmp, rscratch2);
          __ andr(rscratch1, tmp, TypeEntries::type_klass_mask);
          __ cbz(rscratch1, next);
        }
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "conflict only");

        __ ldr(tmp, mdo_addr);
        __ tbnz(tmp, exact_log2(TypeEntries::type_unknown), next); // already unknown. Nothing to do anymore.
      }

      // different than before. Cannot keep accurate profile.
      __ ldr(rscratch2, mdo_addr);
      __ orr(rscratch2, rscratch2, TypeEntries::type_unknown);
      __ str(rscratch2, mdo_addr);

      if (TypeEntries::is_type_none(current_klass)) {
        __ b(next);

        __ bind(none);
        // first time here. Set profile type.
        __ str(tmp, mdo_addr);
      }
    } else {
      // There's a single possible klass at this profile point
      assert(exact_klass != NULL, "should be");
      if (TypeEntries::is_type_none(current_klass)) {
        __ mov_metadata(tmp, exact_klass->constant_encoding());
        __ ldr(rscratch2, mdo_addr);
        __ eor(tmp, tmp, rscratch2);
        __ andr(rscratch1, tmp, TypeEntries::type_klass_mask);
        __ cbz(rscratch1, next);
#ifdef ASSERT
        {
          Label ok;
          __ ldr(rscratch1, mdo_addr);
          __ cbz(rscratch1, ok);
          __ cmp(rscratch1, (u1)TypeEntries::null_seen);
          __ br(Assembler::EQ, ok);
          // may have been set by another thread
          __ dmb(Assembler::ISHLD);
          __ mov_metadata(rscratch1, exact_klass->constant_encoding());
          __ ldr(rscratch2, mdo_addr);
          __ eor(rscratch2, rscratch1, rscratch2);
          __ andr(rscratch2, rscratch2, TypeEntries::type_mask);
          __ cbz(rscratch2, ok);

          __ stop("unexpected profiling mismatch");
          __ bind(ok);
        }
#endif
        // first time here. Set profile type.
        __ str(tmp, mdo_addr);
      } else {
        assert(ciTypeEntries::valid_ciklass(current_klass) != NULL &&
               ciTypeEntries::valid_ciklass(current_klass) != exact_klass, "inconsistent");

        __ ldr(tmp, mdo_addr);
        __ tbnz(tmp, exact_log2(TypeEntries::type_unknown), next); // already unknown. Nothing to do anymore.

        __ orr(tmp, tmp, TypeEntries::type_unknown);
        __ str(tmp, mdo_addr);
        // FIXME: Write barrier needed here?
      }
    }

    __ bind(next);
  }
  COMMENT("} emit_profile_type");
}


void LIR_Assembler::align_backward_branch_target() {
}


void LIR_Assembler::negate(LIR_Opr left, LIR_Opr dest, LIR_Opr tmp) {
  // tmp must be unused
  assert(tmp->is_illegal(), "wasting a register if tmp is allocated");

  if (left->is_single_cpu()) {
    assert(dest->is_single_cpu(), "expect single result reg");
    __ negw(dest->as_register(), left->as_register());
  } else if (left->is_double_cpu()) {
    assert(dest->is_double_cpu(), "expect double result reg");
    __ neg(dest->as_register_lo(), left->as_register_lo());
  } else if (left->is_single_fpu()) {
    assert(dest->is_single_fpu(), "expect single float result reg");
    __ fnegs(dest->as_float_reg(), left->as_float_reg());
  } else {
    assert(left->is_double_fpu(), "expect double float operand reg");
    assert(dest->is_double_fpu(), "expect double float result reg");
    __ fnegd(dest->as_double_reg(), left->as_double_reg());
  }
}


void LIR_Assembler::leal(LIR_Opr addr, LIR_Opr dest, LIR_PatchCode patch_code, CodeEmitInfo* info) {
  if (patch_code != lir_patch_none) {
    deoptimize_trap(info);
    return;
  }

  __ lea(dest->as_register_lo(), as_Address(addr->as_address_ptr()));
}


void LIR_Assembler::rt_call(LIR_Opr result, address dest, const LIR_OprList* args, LIR_Opr tmp, CodeEmitInfo* info) {
  assert(!tmp->is_valid(), "don't need temporary");

  CodeBlob *cb = CodeCache::find_blob(dest);
  if (cb) {
    __ far_call(RuntimeAddress(dest));
  } else {
    __ mov(rscratch1, RuntimeAddress(dest));
    __ blr(rscratch1);
  }

  if (info != NULL) {
    add_call_info_here(info);
  }
}

void LIR_Assembler::volatile_move_op(LIR_Opr src, LIR_Opr dest, BasicType type, CodeEmitInfo* info) {
  if (dest->is_address() || src->is_address()) {
    move_op(src, dest, type, lir_patch_none, info,
            /*pop_fpu_stack*/false, /*wide*/false);
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
    Assembler::Condition acond = Assembler::AL;
    switch (op->condition()) {
      case lir_cond_equal:        acond = Assembler::EQ;  break;
      case lir_cond_notEqual:     acond = Assembler::NE;  break;
      case lir_cond_less:         acond = Assembler::LT;  break;
      case lir_cond_lessEqual:    acond = Assembler::LE;  break;
      case lir_cond_greaterEqual: acond = Assembler::GE;  break;
      case lir_cond_greater:      acond = Assembler::GT;  break;
      case lir_cond_belowEqual:   acond = Assembler::LS;  break;
      case lir_cond_aboveEqual:   acond = Assembler::HS;  break;
      default:                    ShouldNotReachHere();
    }
    __ br(acond, ok);
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

#ifndef PRODUCT
#define COMMENT(x)   do { __ block_comment(x); } while (0)
#else
#define COMMENT(x)
#endif

void LIR_Assembler::membar() {
  COMMENT("membar");
  __ membar(MacroAssembler::AnyAny);
}

void LIR_Assembler::membar_acquire() {
  __ membar(Assembler::LoadLoad|Assembler::LoadStore);
}

void LIR_Assembler::membar_release() {
  __ membar(Assembler::LoadStore|Assembler::StoreStore);
}

void LIR_Assembler::membar_loadload() {
  __ membar(Assembler::LoadLoad);
}

void LIR_Assembler::membar_storestore() {
  __ membar(MacroAssembler::StoreStore);
}

void LIR_Assembler::membar_loadstore() { __ membar(MacroAssembler::LoadStore); }

void LIR_Assembler::membar_storeload() { __ membar(MacroAssembler::StoreLoad); }

void LIR_Assembler::on_spin_wait() {
  Unimplemented();
}

void LIR_Assembler::get_thread(LIR_Opr result_reg) {
  __ mov(result_reg->as_register(), rthread);
}


void LIR_Assembler::peephole(LIR_List *lir) {
#if 0
  if (tableswitch_count >= max_tableswitches)
    return;

  /*
    This finite-state automaton recognizes sequences of compare-and-
    branch instructions.  We will turn them into a tableswitch.  You
    could argue that C1 really shouldn't be doing this sort of
    optimization, but without it the code is really horrible.
  */

  enum { start_s, cmp1_s, beq_s, cmp_s } state;
  int first_key, last_key = -2147483648;
  int next_key = 0;
  int start_insn = -1;
  int last_insn = -1;
  Register reg = noreg;
  LIR_Opr reg_opr;
  state = start_s;

  LIR_OpList* inst = lir->instructions_list();
  for (int i = 0; i < inst->length(); i++) {
    LIR_Op* op = inst->at(i);
    switch (state) {
    case start_s:
      first_key = -1;
      start_insn = i;
      switch (op->code()) {
      case lir_cmp:
        LIR_Opr opr1 = op->as_Op2()->in_opr1();
        LIR_Opr opr2 = op->as_Op2()->in_opr2();
        if (opr1->is_cpu_register() && opr1->is_single_cpu()
            && opr2->is_constant()
            && opr2->type() == T_INT) {
          reg_opr = opr1;
          reg = opr1->as_register();
          first_key = opr2->as_constant_ptr()->as_jint();
          next_key = first_key + 1;
          state = cmp_s;
          goto next_state;
        }
        break;
      }
      break;
    case cmp_s:
      switch (op->code()) {
      case lir_branch:
        if (op->as_OpBranch()->cond() == lir_cond_equal) {
          state = beq_s;
          last_insn = i;
          goto next_state;
        }
      }
      state = start_s;
      break;
    case beq_s:
      switch (op->code()) {
      case lir_cmp: {
        LIR_Opr opr1 = op->as_Op2()->in_opr1();
        LIR_Opr opr2 = op->as_Op2()->in_opr2();
        if (opr1->is_cpu_register() && opr1->is_single_cpu()
            && opr1->as_register() == reg
            && opr2->is_constant()
            && opr2->type() == T_INT
            && opr2->as_constant_ptr()->as_jint() == next_key) {
          last_key = next_key;
          next_key++;
          state = cmp_s;
          goto next_state;
        }
      }
      }
      last_key = next_key;
      state = start_s;
      break;
    default:
      assert(false, "impossible state");
    }
    if (state == start_s) {
      if (first_key < last_key - 5L && reg != noreg) {
        {
          // printf("found run register %d starting at insn %d low value %d high value %d\n",
          //        reg->encoding(),
          //        start_insn, first_key, last_key);
          //   for (int i = 0; i < inst->length(); i++) {
          //     inst->at(i)->print();
          //     tty->print("\n");
          //   }
          //   tty->print("\n");
        }

        struct tableswitch *sw = &switches[tableswitch_count];
        sw->_insn_index = start_insn, sw->_first_key = first_key,
          sw->_last_key = last_key, sw->_reg = reg;
        inst->insert_before(last_insn + 1, new LIR_OpLabel(&sw->_after));
        {
          // Insert the new table of branches
          int offset = last_insn;
          for (int n = first_key; n < last_key; n++) {
            inst->insert_before
              (last_insn + 1,
               new LIR_OpBranch(lir_cond_always, T_ILLEGAL,
                                inst->at(offset)->as_OpBranch()->label()));
            offset -= 2, i++;
          }
        }
        // Delete all the old compare-and-branch instructions
        for (int n = first_key; n < last_key; n++) {
          inst->remove_at(start_insn);
          inst->remove_at(start_insn);
        }
        // Insert the tableswitch instruction
        inst->insert_before(start_insn,
                            new LIR_Op2(lir_cmp, lir_cond_always,
                                        LIR_OprFact::intConst(tableswitch_count),
                                        reg_opr));
        inst->insert_before(start_insn + 1, new LIR_OpLabel(&sw->_branches));
        tableswitch_count++;
      }
      reg = noreg;
      last_key = -2147483648;
    }
  next_state:
    ;
  }
#endif
}

void LIR_Assembler::atomic_op(LIR_Code code, LIR_Opr src, LIR_Opr data, LIR_Opr dest, LIR_Opr tmp_op) {
  Address addr = as_Address(src->as_address_ptr());
  BasicType type = src->type();
  bool is_oop = is_reference_type(type);

  void (MacroAssembler::* add)(Register prev, RegisterOrConstant incr, Register addr);
  void (MacroAssembler::* xchg)(Register prev, Register newv, Register addr);

  switch(type) {
  case T_INT:
    xchg = &MacroAssembler::atomic_xchgalw;
    add = &MacroAssembler::atomic_addalw;
    break;
  case T_LONG:
    xchg = &MacroAssembler::atomic_xchgal;
    add = &MacroAssembler::atomic_addal;
    break;
  case T_OBJECT:
  case T_ARRAY:
    if (UseCompressedOops) {
      xchg = &MacroAssembler::atomic_xchgalw;
      add = &MacroAssembler::atomic_addalw;
    } else {
      xchg = &MacroAssembler::atomic_xchgal;
      add = &MacroAssembler::atomic_addal;
    }
    break;
  default:
    ShouldNotReachHere();
    xchg = &MacroAssembler::atomic_xchgal;
    add = &MacroAssembler::atomic_addal; // unreachable
  }

  switch (code) {
  case lir_xadd:
    {
      RegisterOrConstant inc;
      Register tmp = as_reg(tmp_op);
      Register dst = as_reg(dest);
      if (data->is_constant()) {
        inc = RegisterOrConstant(as_long(data));
        assert_different_registers(dst, addr.base(), tmp,
                                   rscratch1, rscratch2);
      } else {
        inc = RegisterOrConstant(as_reg(data));
        assert_different_registers(inc.as_register(), dst, addr.base(), tmp,
                                   rscratch1, rscratch2);
      }
      __ lea(tmp, addr);
      (_masm->*add)(dst, inc, tmp);
      break;
    }
  case lir_xchg:
    {
      Register tmp = tmp_op->as_register();
      Register obj = as_reg(data);
      Register dst = as_reg(dest);
      if (is_oop && UseCompressedOops) {
        __ encode_heap_oop(rscratch2, obj);
        obj = rscratch2;
      }
      assert_different_registers(obj, addr.base(), tmp, rscratch1, dst);
      __ lea(tmp, addr);
      (_masm->*xchg)(dst, obj, tmp);
      if (is_oop && UseCompressedOops) {
        __ decode_heap_oop(dst);
      }
    }
    break;
  default:
    ShouldNotReachHere();
  }
  __ membar(__ AnyAny);
}

#undef __
