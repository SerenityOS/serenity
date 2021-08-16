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
#include "asm/assembler.inline.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_InstructionPrinter.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciInstance.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"

void LIR_Assembler::patching_epilog(PatchingStub* patch, LIR_PatchCode patch_code, Register obj, CodeEmitInfo* info) {
  // We must have enough patching space so that call can be inserted.
  // We cannot use fat nops here, since the concurrent code rewrite may transiently
  // create the illegal instruction sequence.
  while ((intx) _masm->pc() - (intx) patch->pc_start() < NativeGeneralJump::instruction_size) {
    _masm->nop();
  }
  patch->install(_masm, patch_code, obj, info);
  append_code_stub(patch);

#ifdef ASSERT
  Bytecodes::Code code = info->scope()->method()->java_code_at_bci(info->stack()->bci());
  if (patch->id() == PatchingStub::access_field_id) {
    switch (code) {
      case Bytecodes::_putstatic:
      case Bytecodes::_getstatic:
      case Bytecodes::_putfield:
      case Bytecodes::_getfield:
        break;
      default:
        ShouldNotReachHere();
    }
  } else if (patch->id() == PatchingStub::load_klass_id) {
    switch (code) {
      case Bytecodes::_new:
      case Bytecodes::_anewarray:
      case Bytecodes::_multianewarray:
      case Bytecodes::_instanceof:
      case Bytecodes::_checkcast:
        break;
      default:
        ShouldNotReachHere();
    }
  } else if (patch->id() == PatchingStub::load_mirror_id) {
    switch (code) {
      case Bytecodes::_putstatic:
      case Bytecodes::_getstatic:
      case Bytecodes::_ldc:
      case Bytecodes::_ldc_w:
        break;
      default:
        ShouldNotReachHere();
    }
  } else if (patch->id() == PatchingStub::load_appendix_id) {
    Bytecodes::Code bc_raw = info->scope()->method()->raw_code_at_bci(info->stack()->bci());
    assert(Bytecodes::has_optional_appendix(bc_raw), "unexpected appendix resolution");
  } else {
    ShouldNotReachHere();
  }
#endif
}

PatchingStub::PatchID LIR_Assembler::patching_id(CodeEmitInfo* info) {
  IRScope* scope = info->scope();
  Bytecodes::Code bc_raw = scope->method()->raw_code_at_bci(info->stack()->bci());
  if (Bytecodes::has_optional_appendix(bc_raw)) {
    return PatchingStub::load_appendix_id;
  }
  return PatchingStub::load_mirror_id;
}

//---------------------------------------------------------------


LIR_Assembler::LIR_Assembler(Compilation* c):
   _masm(c->masm())
 , _bs(BarrierSet::barrier_set())
 , _compilation(c)
 , _frame_map(c->frame_map())
 , _current_block(NULL)
 , _pending_non_safepoint(NULL)
 , _pending_non_safepoint_offset(0)
{
  _slow_case_stubs = new CodeStubList();
}


LIR_Assembler::~LIR_Assembler() {
  // The unwind handler label may be unnbound if this destructor is invoked because of a bail-out.
  // Reset it here to avoid an assertion.
  _unwind_handler_entry.reset();
}


void LIR_Assembler::check_codespace() {
  CodeSection* cs = _masm->code_section();
  if (cs->remaining() < (int)(NOT_LP64(1*K)LP64_ONLY(2*K))) {
    BAILOUT("CodeBuffer overflow");
  }
}


void LIR_Assembler::append_code_stub(CodeStub* stub) {
  _slow_case_stubs->append(stub);
}

void LIR_Assembler::emit_stubs(CodeStubList* stub_list) {
  for (int m = 0; m < stub_list->length(); m++) {
    CodeStub* s = stub_list->at(m);

    check_codespace();
    CHECK_BAILOUT();

#ifndef PRODUCT
    if (CommentedAssembly) {
      stringStream st;
      s->print_name(&st);
      st.print(" slow case");
      _masm->block_comment(st.as_string());
    }
#endif
    s->emit_code(this);
#ifdef ASSERT
    s->assert_no_unbound_labels();
#endif
  }
}


void LIR_Assembler::emit_slow_case_stubs() {
  emit_stubs(_slow_case_stubs);
}


bool LIR_Assembler::needs_icache(ciMethod* method) const {
  return !method->is_static();
}

bool LIR_Assembler::needs_clinit_barrier_on_entry(ciMethod* method) const {
  return VM_Version::supports_fast_class_init_checks() && method->needs_clinit_barrier();
}

int LIR_Assembler::code_offset() const {
  return _masm->offset();
}


address LIR_Assembler::pc() const {
  return _masm->pc();
}

// To bang the stack of this compiled method we use the stack size
// that the interpreter would need in case of a deoptimization. This
// removes the need to bang the stack in the deoptimization blob which
// in turn simplifies stack overflow handling.
int LIR_Assembler::bang_size_in_bytes() const {
  return MAX2(initial_frame_size_in_bytes() + os::extra_bang_size_in_bytes(), _compilation->interpreter_frame_size());
}

void LIR_Assembler::emit_exception_entries(ExceptionInfoList* info_list) {
  for (int i = 0; i < info_list->length(); i++) {
    XHandlers* handlers = info_list->at(i)->exception_handlers();

    for (int j = 0; j < handlers->length(); j++) {
      XHandler* handler = handlers->handler_at(j);
      assert(handler->lir_op_id() != -1, "handler not processed by LinearScan");
      assert(handler->entry_code() == NULL ||
             handler->entry_code()->instructions_list()->last()->code() == lir_branch ||
             handler->entry_code()->instructions_list()->last()->code() == lir_delay_slot, "last operation must be branch");

      if (handler->entry_pco() == -1) {
        // entry code not emitted yet
        if (handler->entry_code() != NULL && handler->entry_code()->instructions_list()->length() > 1) {
          handler->set_entry_pco(code_offset());
          if (CommentedAssembly) {
            _masm->block_comment("Exception adapter block");
          }
          emit_lir_list(handler->entry_code());
        } else {
          handler->set_entry_pco(handler->entry_block()->exception_handler_pco());
        }

        assert(handler->entry_pco() != -1, "must be set now");
      }
    }
  }
}


void LIR_Assembler::emit_code(BlockList* hir) {
  if (PrintLIR) {
    print_LIR(hir);
  }

  int n = hir->length();
  for (int i = 0; i < n; i++) {
    emit_block(hir->at(i));
    CHECK_BAILOUT();
  }

  flush_debug_info(code_offset());

  DEBUG_ONLY(check_no_unbound_labels());
}


void LIR_Assembler::emit_block(BlockBegin* block) {
  if (block->is_set(BlockBegin::backward_branch_target_flag)) {
    align_backward_branch_target();
  }

  // if this block is the start of an exception handler, record the
  // PC offset of the first instruction for later construction of
  // the ExceptionHandlerTable
  if (block->is_set(BlockBegin::exception_entry_flag)) {
    block->set_exception_handler_pco(code_offset());
  }

#ifndef PRODUCT
  if (PrintLIRWithAssembly) {
    // don't print Phi's
    InstructionPrinter ip(false);
    block->print(ip);
  }
#endif /* PRODUCT */

  assert(block->lir() != NULL, "must have LIR");
  X86_ONLY(assert(_masm->rsp_offset() == 0, "frame size should be fixed"));

#ifndef PRODUCT
  if (CommentedAssembly) {
    stringStream st;
    st.print_cr(" block B%d [%d, %d]", block->block_id(), block->bci(), block->end()->printable_bci());
    _masm->block_comment(st.as_string());
  }
#endif

  emit_lir_list(block->lir());

  X86_ONLY(assert(_masm->rsp_offset() == 0, "frame size should be fixed"));
}


void LIR_Assembler::emit_lir_list(LIR_List* list) {
  peephole(list);

  int n = list->length();
  for (int i = 0; i < n; i++) {
    LIR_Op* op = list->at(i);

    check_codespace();
    CHECK_BAILOUT();

#ifndef PRODUCT
    if (CommentedAssembly) {
      // Don't record out every op since that's too verbose.  Print
      // branches since they include block and stub names.  Also print
      // patching moves since they generate funny looking code.
      if (op->code() == lir_branch ||
          (op->code() == lir_move && op->as_Op1()->patch_code() != lir_patch_none) ||
          (op->code() == lir_leal && op->as_Op1()->patch_code() != lir_patch_none)) {
        stringStream st;
        op->print_on(&st);
        _masm->block_comment(st.as_string());
      }
    }
    if (PrintLIRWithAssembly) {
      // print out the LIR operation followed by the resulting assembly
      list->at(i)->print(); tty->cr();
    }
#endif /* PRODUCT */

    op->emit_code(this);

    if (compilation()->debug_info_recorder()->recording_non_safepoints()) {
      process_debug_info(op);
    }

#ifndef PRODUCT
    if (PrintLIRWithAssembly) {
      _masm->code()->decode();
    }
#endif /* PRODUCT */
  }
}

#ifdef ASSERT
void LIR_Assembler::check_no_unbound_labels() {
  CHECK_BAILOUT();

  for (int i = 0; i < _branch_target_blocks.length() - 1; i++) {
    if (!_branch_target_blocks.at(i)->label()->is_bound()) {
      tty->print_cr("label of block B%d is not bound", _branch_target_blocks.at(i)->block_id());
      assert(false, "unbound label");
    }
  }
}
#endif

//----------------------------------debug info--------------------------------


void LIR_Assembler::add_debug_info_for_branch(CodeEmitInfo* info) {
  int pc_offset = code_offset();
  flush_debug_info(pc_offset);
  info->record_debug_info(compilation()->debug_info_recorder(), pc_offset);
  if (info->exception_handlers() != NULL) {
    compilation()->add_exception_handlers_for_pco(pc_offset, info->exception_handlers());
  }
}


void LIR_Assembler::add_call_info(int pc_offset, CodeEmitInfo* cinfo) {
  flush_debug_info(pc_offset);
  cinfo->record_debug_info(compilation()->debug_info_recorder(), pc_offset);
  if (cinfo->exception_handlers() != NULL) {
    compilation()->add_exception_handlers_for_pco(pc_offset, cinfo->exception_handlers());
  }
}

static ValueStack* debug_info(Instruction* ins) {
  StateSplit* ss = ins->as_StateSplit();
  if (ss != NULL) return ss->state();
  return ins->state_before();
}

void LIR_Assembler::process_debug_info(LIR_Op* op) {
  Instruction* src = op->source();
  if (src == NULL)  return;
  int pc_offset = code_offset();
  if (_pending_non_safepoint == src) {
    _pending_non_safepoint_offset = pc_offset;
    return;
  }
  ValueStack* vstack = debug_info(src);
  if (vstack == NULL)  return;
  if (_pending_non_safepoint != NULL) {
    // Got some old debug info.  Get rid of it.
    if (debug_info(_pending_non_safepoint) == vstack) {
      _pending_non_safepoint_offset = pc_offset;
      return;
    }
    if (_pending_non_safepoint_offset < pc_offset) {
      record_non_safepoint_debug_info();
    }
    _pending_non_safepoint = NULL;
  }
  // Remember the debug info.
  if (pc_offset > compilation()->debug_info_recorder()->last_pc_offset()) {
    _pending_non_safepoint = src;
    _pending_non_safepoint_offset = pc_offset;
  }
}

// Index caller states in s, where 0 is the oldest, 1 its callee, etc.
// Return NULL if n is too large.
// Returns the caller_bci for the next-younger state, also.
static ValueStack* nth_oldest(ValueStack* s, int n, int& bci_result) {
  ValueStack* t = s;
  for (int i = 0; i < n; i++) {
    if (t == NULL)  break;
    t = t->caller_state();
  }
  if (t == NULL)  return NULL;
  for (;;) {
    ValueStack* tc = t->caller_state();
    if (tc == NULL)  return s;
    t = tc;
    bci_result = tc->bci();
    s = s->caller_state();
  }
}

void LIR_Assembler::record_non_safepoint_debug_info() {
  int         pc_offset = _pending_non_safepoint_offset;
  ValueStack* vstack    = debug_info(_pending_non_safepoint);
  int         bci       = vstack->bci();

  DebugInformationRecorder* debug_info = compilation()->debug_info_recorder();
  assert(debug_info->recording_non_safepoints(), "sanity");

  debug_info->add_non_safepoint(pc_offset);

  // Visit scopes from oldest to youngest.
  for (int n = 0; ; n++) {
    int s_bci = bci;
    ValueStack* s = nth_oldest(vstack, n, s_bci);
    if (s == NULL)  break;
    IRScope* scope = s->scope();
    //Always pass false for reexecute since these ScopeDescs are never used for deopt
    methodHandle null_mh;
    debug_info->describe_scope(pc_offset, null_mh, scope->method(), s->bci(), false/*reexecute*/);
  }

  debug_info->end_non_safepoint(pc_offset);
}


ImplicitNullCheckStub* LIR_Assembler::add_debug_info_for_null_check_here(CodeEmitInfo* cinfo) {
  return add_debug_info_for_null_check(code_offset(), cinfo);
}

ImplicitNullCheckStub* LIR_Assembler::add_debug_info_for_null_check(int pc_offset, CodeEmitInfo* cinfo) {
  ImplicitNullCheckStub* stub = new ImplicitNullCheckStub(pc_offset, cinfo);
  append_code_stub(stub);
  return stub;
}

void LIR_Assembler::add_debug_info_for_div0_here(CodeEmitInfo* info) {
  add_debug_info_for_div0(code_offset(), info);
}

void LIR_Assembler::add_debug_info_for_div0(int pc_offset, CodeEmitInfo* cinfo) {
  DivByZeroStub* stub = new DivByZeroStub(pc_offset, cinfo);
  append_code_stub(stub);
}

void LIR_Assembler::emit_rtcall(LIR_OpRTCall* op) {
  rt_call(op->result_opr(), op->addr(), op->arguments(), op->tmp(), op->info());
}


void LIR_Assembler::emit_call(LIR_OpJavaCall* op) {
  verify_oop_map(op->info());

  // must align calls sites, otherwise they can't be updated atomically
  align_call(op->code());

  // emit the static call stub stuff out of line
  emit_static_call_stub();
  CHECK_BAILOUT();

  switch (op->code()) {
  case lir_static_call:
  case lir_dynamic_call:
    call(op, relocInfo::static_call_type);
    break;
  case lir_optvirtual_call:
    call(op, relocInfo::opt_virtual_call_type);
    break;
  case lir_icvirtual_call:
    ic_call(op);
    break;
  default:
    fatal("unexpected op code: %s", op->name());
    break;
  }

  // JSR 292
  // Record if this method has MethodHandle invokes.
  if (op->is_method_handle_invoke()) {
    compilation()->set_has_method_handle_invokes(true);
  }

#if defined(IA32) && defined(COMPILER2)
  // C2 leave fpu stack dirty clean it
  if (UseSSE < 2 && !CompilerConfig::is_c1_only_no_jvmci()) {
    int i;
    for ( i = 1; i <= 7 ; i++ ) {
      ffree(i);
    }
    if (!op->result_opr()->is_float_kind()) {
      ffree(0);
    }
  }
#endif // IA32 && COMPILER2
}


void LIR_Assembler::emit_opLabel(LIR_OpLabel* op) {
  _masm->bind (*(op->label()));
}


void LIR_Assembler::emit_op1(LIR_Op1* op) {
  switch (op->code()) {
    case lir_move:
      if (op->move_kind() == lir_move_volatile) {
        assert(op->patch_code() == lir_patch_none, "can't patch volatiles");
        volatile_move_op(op->in_opr(), op->result_opr(), op->type(), op->info());
      } else {
        move_op(op->in_opr(), op->result_opr(), op->type(),
                op->patch_code(), op->info(), op->pop_fpu_stack(),
                op->move_kind() == lir_move_wide);
      }
      break;

    case lir_roundfp: {
      LIR_OpRoundFP* round_op = op->as_OpRoundFP();
      roundfp_op(round_op->in_opr(), round_op->tmp(), round_op->result_opr(), round_op->pop_fpu_stack());
      break;
    }

    case lir_return: {
      assert(op->as_OpReturn() != NULL, "sanity");
      LIR_OpReturn *ret_op = (LIR_OpReturn*)op;
      return_op(ret_op->in_opr(), ret_op->stub());
      if (ret_op->stub() != NULL) {
        append_code_stub(ret_op->stub());
      }
      break;
    }

    case lir_safepoint:
      if (compilation()->debug_info_recorder()->last_pc_offset() == code_offset()) {
        _masm->nop();
      }
      safepoint_poll(op->in_opr(), op->info());
      break;

#ifdef IA32
    case lir_fxch:
      fxch(op->in_opr()->as_jint());
      break;

    case lir_fld:
      fld(op->in_opr()->as_jint());
      break;
#endif // IA32

    case lir_branch:
      break;

    case lir_push:
      push(op->in_opr());
      break;

    case lir_pop:
      pop(op->in_opr());
      break;

    case lir_leal:
      leal(op->in_opr(), op->result_opr(), op->patch_code(), op->info());
      break;

    case lir_null_check: {
      ImplicitNullCheckStub* stub = add_debug_info_for_null_check_here(op->info());

      if (op->in_opr()->is_single_cpu()) {
        _masm->null_check(op->in_opr()->as_register(), stub->entry());
      } else {
        Unimplemented();
      }
      break;
    }

    case lir_monaddr:
      monitor_address(op->in_opr()->as_constant_ptr()->as_jint(), op->result_opr());
      break;

    case lir_unwind:
      unwind_op(op->in_opr());
      break;

    default:
      Unimplemented();
      break;
  }
}


void LIR_Assembler::emit_op0(LIR_Op0* op) {
  switch (op->code()) {
    case lir_nop:
      assert(op->info() == NULL, "not supported");
      _masm->nop();
      break;

    case lir_label:
      Unimplemented();
      break;

    case lir_std_entry:
      // init offsets
      offsets()->set_value(CodeOffsets::OSR_Entry, _masm->offset());
      _masm->align(CodeEntryAlignment);
      if (needs_icache(compilation()->method())) {
        check_icache();
      }
      offsets()->set_value(CodeOffsets::Verified_Entry, _masm->offset());
      _masm->verified_entry();
      if (needs_clinit_barrier_on_entry(compilation()->method())) {
        clinit_barrier(compilation()->method());
      }
      build_frame();
      offsets()->set_value(CodeOffsets::Frame_Complete, _masm->offset());
      break;

    case lir_osr_entry:
      offsets()->set_value(CodeOffsets::OSR_Entry, _masm->offset());
      osr_entry();
      break;

#ifdef IA32
    case lir_fpop_raw:
      fpop();
      break;
#endif // IA32

    case lir_breakpoint:
      breakpoint();
      break;

    case lir_membar:
      membar();
      break;

    case lir_membar_acquire:
      membar_acquire();
      break;

    case lir_membar_release:
      membar_release();
      break;

    case lir_membar_loadload:
      membar_loadload();
      break;

    case lir_membar_storestore:
      membar_storestore();
      break;

    case lir_membar_loadstore:
      membar_loadstore();
      break;

    case lir_membar_storeload:
      membar_storeload();
      break;

    case lir_get_thread:
      get_thread(op->result_opr());
      break;

    case lir_on_spin_wait:
      on_spin_wait();
      break;

    default:
      ShouldNotReachHere();
      break;
  }
}


void LIR_Assembler::emit_op2(LIR_Op2* op) {
  switch (op->code()) {
    case lir_cmp:
      if (op->info() != NULL) {
        assert(op->in_opr1()->is_address() || op->in_opr2()->is_address(),
               "shouldn't be codeemitinfo for non-address operands");
        add_debug_info_for_null_check_here(op->info()); // exception possible
      }
      comp_op(op->condition(), op->in_opr1(), op->in_opr2(), op);
      break;

    case lir_cmp_l2i:
    case lir_cmp_fd2i:
    case lir_ucmp_fd2i:
      comp_fl2i(op->code(), op->in_opr1(), op->in_opr2(), op->result_opr(), op);
      break;

    case lir_cmove:
      cmove(op->condition(), op->in_opr1(), op->in_opr2(), op->result_opr(), op->type());
      break;

    case lir_shl:
    case lir_shr:
    case lir_ushr:
      if (op->in_opr2()->is_constant()) {
        shift_op(op->code(), op->in_opr1(), op->in_opr2()->as_constant_ptr()->as_jint(), op->result_opr());
      } else {
        shift_op(op->code(), op->in_opr1(), op->in_opr2(), op->result_opr(), op->tmp1_opr());
      }
      break;

    case lir_add:
    case lir_sub:
    case lir_mul:
    case lir_div:
    case lir_rem:
      assert(op->fpu_pop_count() < 2, "");
      arith_op(
        op->code(),
        op->in_opr1(),
        op->in_opr2(),
        op->result_opr(),
        op->info(),
        op->fpu_pop_count() == 1);
      break;

    case lir_abs:
    case lir_sqrt:
    case lir_tan:
    case lir_log10:
      intrinsic_op(op->code(), op->in_opr1(), op->in_opr2(), op->result_opr(), op);
      break;

    case lir_neg:
      negate(op->in_opr1(), op->result_opr(), op->in_opr2());
      break;

    case lir_logic_and:
    case lir_logic_or:
    case lir_logic_xor:
      logic_op(
        op->code(),
        op->in_opr1(),
        op->in_opr2(),
        op->result_opr());
      break;

    case lir_throw:
      throw_op(op->in_opr1(), op->in_opr2(), op->info());
      break;

    case lir_xadd:
    case lir_xchg:
      atomic_op(op->code(), op->in_opr1(), op->in_opr2(), op->result_opr(), op->tmp1_opr());
      break;

    default:
      Unimplemented();
      break;
  }
}


void LIR_Assembler::build_frame() {
  _masm->build_frame(initial_frame_size_in_bytes(), bang_size_in_bytes());
}


void LIR_Assembler::roundfp_op(LIR_Opr src, LIR_Opr tmp, LIR_Opr dest, bool pop_fpu_stack) {
  assert(strict_fp_requires_explicit_rounding, "not required");
  assert((src->is_single_fpu() && dest->is_single_stack()) ||
         (src->is_double_fpu() && dest->is_double_stack()),
         "round_fp: rounds register -> stack location");

  reg2stack (src, dest, src->type(), pop_fpu_stack);
}


void LIR_Assembler::move_op(LIR_Opr src, LIR_Opr dest, BasicType type, LIR_PatchCode patch_code, CodeEmitInfo* info, bool pop_fpu_stack, bool wide) {
  if (src->is_register()) {
    if (dest->is_register()) {
      assert(patch_code == lir_patch_none && info == NULL, "no patching and info allowed here");
      reg2reg(src,  dest);
    } else if (dest->is_stack()) {
      assert(patch_code == lir_patch_none && info == NULL, "no patching and info allowed here");
      reg2stack(src, dest, type, pop_fpu_stack);
    } else if (dest->is_address()) {
      reg2mem(src, dest, type, patch_code, info, pop_fpu_stack, wide);
    } else {
      ShouldNotReachHere();
    }

  } else if (src->is_stack()) {
    assert(patch_code == lir_patch_none && info == NULL, "no patching and info allowed here");
    if (dest->is_register()) {
      stack2reg(src, dest, type);
    } else if (dest->is_stack()) {
      stack2stack(src, dest, type);
    } else {
      ShouldNotReachHere();
    }

  } else if (src->is_constant()) {
    if (dest->is_register()) {
      const2reg(src, dest, patch_code, info); // patching is possible
    } else if (dest->is_stack()) {
      assert(patch_code == lir_patch_none && info == NULL, "no patching and info allowed here");
      const2stack(src, dest);
    } else if (dest->is_address()) {
      assert(patch_code == lir_patch_none, "no patching allowed here");
      const2mem(src, dest, type, info, wide);
    } else {
      ShouldNotReachHere();
    }

  } else if (src->is_address()) {
    mem2reg(src, dest, type, patch_code, info, wide);
  } else {
    ShouldNotReachHere();
  }
}


void LIR_Assembler::verify_oop_map(CodeEmitInfo* info) {
#ifndef PRODUCT
  if (VerifyOops) {
    OopMapStream s(info->oop_map());
    while (!s.is_done()) {
      OopMapValue v = s.current();
      if (v.is_oop()) {
        VMReg r = v.reg();
        if (!r->is_stack()) {
          stringStream st;
          st.print("bad oop %s at %d", r->as_Register()->name(), _masm->offset());
          _masm->verify_oop(r->as_Register());
        } else {
          _masm->verify_stack_oop(r->reg2stack() * VMRegImpl::stack_slot_size);
        }
      }
      check_codespace();
      CHECK_BAILOUT();

      s.next();
    }
  }
#endif
}
