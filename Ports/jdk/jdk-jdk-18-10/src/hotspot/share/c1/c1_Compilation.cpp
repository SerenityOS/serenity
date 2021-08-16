/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "c1/c1_CFGPrinter.hpp"
#include "c1/c1_Compilation.hpp"
#include "c1/c1_IR.hpp"
#include "c1/c1_LIRAssembler.hpp"
#include "c1/c1_LinearScan.hpp"
#include "c1/c1_MacroAssembler.hpp"
#include "c1/c1_RangeCheckElimination.hpp"
#include "c1/c1_ValueMap.hpp"
#include "c1/c1_ValueStack.hpp"
#include "code/debugInfoRec.hpp"
#include "compiler/compileLog.hpp"
#include "compiler/compilerDirectives.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/timerTrace.hpp"

typedef enum {
  _t_compile,
    _t_setup,
    _t_buildIR,
      _t_hir_parse,
      _t_gvn,
      _t_optimize_blocks,
      _t_optimize_null_checks,
      _t_rangeCheckElimination,
    _t_emit_lir,
      _t_linearScan,
      _t_lirGeneration,
    _t_codeemit,
    _t_codeinstall,
  max_phase_timers
} TimerName;

static const char * timer_name[] = {
  "compile",
  "setup",
  "buildIR",
  "parse_hir",
  "gvn",
  "optimize_blocks",
  "optimize_null_checks",
  "rangeCheckElimination",
  "emit_lir",
  "linearScan",
  "lirGeneration",
  "codeemit",
  "codeinstall"
};

static elapsedTimer timers[max_phase_timers];
static int totalInstructionNodes = 0;

class PhaseTraceTime: public TraceTime {
 private:
  JavaThread* _thread;
  CompileLog* _log;
  TimerName _timer;

 public:
  PhaseTraceTime(TimerName timer)
  : TraceTime("", &timers[timer], CITime || CITimeEach, Verbose),
    _log(NULL), _timer(timer)
  {
    if (Compilation::current() != NULL) {
      _log = Compilation::current()->log();
    }

    if (_log != NULL) {
      _log->begin_head("phase name='%s'", timer_name[_timer]);
      _log->stamp();
      _log->end_head();
    }
  }

  ~PhaseTraceTime() {
    if (_log != NULL)
      _log->done("phase name='%s'", timer_name[_timer]);
  }
};

// Implementation of Compilation


#ifndef PRODUCT

void Compilation::maybe_print_current_instruction() {
  if (_current_instruction != NULL && _last_instruction_printed != _current_instruction) {
    _last_instruction_printed = _current_instruction;
    _current_instruction->print_line();
  }
}
#endif // PRODUCT


DebugInformationRecorder* Compilation::debug_info_recorder() const {
  return _env->debug_info();
}


Dependencies* Compilation::dependency_recorder() const {
  return _env->dependencies();
}


void Compilation::initialize() {
  // Use an oop recorder bound to the CI environment.
  // (The default oop recorder is ignorant of the CI.)
  OopRecorder* ooprec = new OopRecorder(_env->arena());
  _env->set_oop_recorder(ooprec);
  _env->set_debug_info(new DebugInformationRecorder(ooprec));
  debug_info_recorder()->set_oopmaps(new OopMapSet());
  _env->set_dependencies(new Dependencies(_env));
}


void Compilation::build_hir() {
  CHECK_BAILOUT();

  // setup ir
  CompileLog* log = this->log();
  if (log != NULL) {
    log->begin_head("parse method='%d' ",
                    log->identify(_method));
    log->stamp();
    log->end_head();
  }
  {
    PhaseTraceTime timeit(_t_hir_parse);
    _hir = new IR(this, method(), osr_bci());
  }
  if (log)  log->done("parse");
  if (!_hir->is_valid()) {
    bailout("invalid parsing");
    return;
  }

#ifndef PRODUCT
  if (PrintCFGToFile) {
    CFGPrinter::print_cfg(_hir, "After Generation of HIR", true, false);
  }
#endif

#ifndef PRODUCT
  if (PrintCFG || PrintCFG0) { tty->print_cr("CFG after parsing"); _hir->print(true); }
  if (PrintIR  || PrintIR0 ) { tty->print_cr("IR after parsing"); _hir->print(false); }
#endif

  _hir->verify();

  if (UseC1Optimizations) {
    NEEDS_CLEANUP
    // optimization
    PhaseTraceTime timeit(_t_optimize_blocks);

    _hir->optimize_blocks();
  }

  _hir->verify();

  _hir->split_critical_edges();

#ifndef PRODUCT
  if (PrintCFG || PrintCFG1) { tty->print_cr("CFG after optimizations"); _hir->print(true); }
  if (PrintIR  || PrintIR1 ) { tty->print_cr("IR after optimizations"); _hir->print(false); }
#endif

  _hir->verify();

  // compute block ordering for code generation
  // the control flow must not be changed from here on
  _hir->compute_code();

  if (UseGlobalValueNumbering) {
    // No resource mark here! LoopInvariantCodeMotion can allocate ValueStack objects.
    PhaseTraceTime timeit(_t_gvn);
    int instructions = Instruction::number_of_instructions();
    GlobalValueNumbering gvn(_hir);
    assert(instructions == Instruction::number_of_instructions(),
           "shouldn't have created an instructions");
  }

  _hir->verify();

#ifndef PRODUCT
  if (PrintCFGToFile) {
    CFGPrinter::print_cfg(_hir, "Before RangeCheckElimination", true, false);
  }
#endif

  if (RangeCheckElimination) {
    if (_hir->osr_entry() == NULL) {
      PhaseTraceTime timeit(_t_rangeCheckElimination);
      RangeCheckElimination::eliminate(_hir);
    }
  }

#ifndef PRODUCT
  if (PrintCFGToFile) {
    CFGPrinter::print_cfg(_hir, "After RangeCheckElimination", true, false);
  }
#endif

  if (UseC1Optimizations) {
    // loop invariant code motion reorders instructions and range
    // check elimination adds new instructions so do null check
    // elimination after.
    NEEDS_CLEANUP
    // optimization
    PhaseTraceTime timeit(_t_optimize_null_checks);

    _hir->eliminate_null_checks();
  }

  _hir->verify();

  // compute use counts after global value numbering
  _hir->compute_use_counts();

#ifndef PRODUCT
  if (PrintCFG || PrintCFG2) { tty->print_cr("CFG before code generation"); _hir->code()->print(true); }
  if (PrintIR  || PrintIR2 ) { tty->print_cr("IR before code generation"); _hir->code()->print(false, true); }
#endif

  _hir->verify();
}


void Compilation::emit_lir() {
  CHECK_BAILOUT();

  LIRGenerator gen(this, method());
  {
    PhaseTraceTime timeit(_t_lirGeneration);
    hir()->iterate_linear_scan_order(&gen);
  }

  CHECK_BAILOUT();

  {
    PhaseTraceTime timeit(_t_linearScan);

    LinearScan* allocator = new LinearScan(hir(), &gen, frame_map());
    set_allocator(allocator);
    // Assign physical registers to LIR operands using a linear scan algorithm.
    allocator->do_linear_scan();
    CHECK_BAILOUT();

    _max_spills = allocator->max_spills();
  }

  if (BailoutAfterLIR) {
    if (PrintLIR && !bailed_out()) {
      print_LIR(hir()->code());
    }
    bailout("Bailing out because of -XX:+BailoutAfterLIR");
  }
}


void Compilation::emit_code_epilog(LIR_Assembler* assembler) {
  CHECK_BAILOUT();

  CodeOffsets* code_offsets = assembler->offsets();

  // generate code or slow cases
  assembler->emit_slow_case_stubs();
  CHECK_BAILOUT();

  // generate exception adapters
  assembler->emit_exception_entries(exception_info_list());
  CHECK_BAILOUT();

  // Generate code for exception handler.
  code_offsets->set_value(CodeOffsets::Exceptions, assembler->emit_exception_handler());
  CHECK_BAILOUT();

  // Generate code for deopt handler.
  code_offsets->set_value(CodeOffsets::Deopt, assembler->emit_deopt_handler());
  CHECK_BAILOUT();

  // Emit the MethodHandle deopt handler code (if required).
  if (has_method_handle_invokes()) {
    // We can use the same code as for the normal deopt handler, we
    // just need a different entry point address.
    code_offsets->set_value(CodeOffsets::DeoptMH, assembler->emit_deopt_handler());
    CHECK_BAILOUT();
  }

  // Emit the handler to remove the activation from the stack and
  // dispatch to the caller.
  offsets()->set_value(CodeOffsets::UnwindHandler, assembler->emit_unwind_handler());

  // done
  masm()->flush();
}


bool Compilation::setup_code_buffer(CodeBuffer* code, int call_stub_estimate) {
  // Preinitialize the consts section to some large size:
  int locs_buffer_size = 20 * (relocInfo::length_limit + sizeof(relocInfo));
  char* locs_buffer = NEW_RESOURCE_ARRAY(char, locs_buffer_size);
  code->insts()->initialize_shared_locs((relocInfo*)locs_buffer,
                                        locs_buffer_size / sizeof(relocInfo));
  code->initialize_consts_size(Compilation::desired_max_constant_size());
  // Call stubs + two deopt handlers (regular and MH) + exception handler
  int stub_size = (call_stub_estimate * LIR_Assembler::call_stub_size()) +
                   LIR_Assembler::exception_handler_size() +
                   (2 * LIR_Assembler::deopt_handler_size());
  if (stub_size >= code->insts_capacity()) return false;
  code->initialize_stubs_size(stub_size);
  return true;
}


int Compilation::emit_code_body() {
  // emit code
  if (!setup_code_buffer(code(), allocator()->num_calls())) {
    BAILOUT_("size requested greater than avail code buffer size", 0);
  }
  code()->initialize_oop_recorder(env()->oop_recorder());

  _masm = new C1_MacroAssembler(code());
  _masm->set_oop_recorder(env()->oop_recorder());

  LIR_Assembler lir_asm(this);

  lir_asm.emit_code(hir()->code());
  CHECK_BAILOUT_(0);

  emit_code_epilog(&lir_asm);
  CHECK_BAILOUT_(0);

  generate_exception_handler_table();

#ifndef PRODUCT
  if (PrintExceptionHandlers && Verbose) {
    exception_handler_table()->print();
  }
#endif /* PRODUCT */

  return frame_map()->framesize();
}


int Compilation::compile_java_method() {
  assert(!method()->is_native(), "should not reach here");

  if (BailoutOnExceptionHandlers) {
    if (method()->has_exception_handlers()) {
      bailout("linear scan can't handle exception handlers");
    }
  }

  CHECK_BAILOUT_(no_frame_size);

  if (is_profiling() && !method()->ensure_method_data()) {
    BAILOUT_("mdo allocation failed", no_frame_size);
  }

  {
    PhaseTraceTime timeit(_t_buildIR);
    build_hir();
  }
  if (BailoutAfterHIR) {
    BAILOUT_("Bailing out because of -XX:+BailoutAfterHIR", no_frame_size);
  }


  {
    PhaseTraceTime timeit(_t_emit_lir);

    _frame_map = new FrameMap(method(), hir()->number_of_locks(), MAX2(4, hir()->max_stack()));
    emit_lir();
  }
  CHECK_BAILOUT_(no_frame_size);

  {
    PhaseTraceTime timeit(_t_codeemit);
    return emit_code_body();
  }
}

void Compilation::install_code(int frame_size) {
  // frame_size is in 32-bit words so adjust it intptr_t words
  assert(frame_size == frame_map()->framesize(), "must match");
  assert(in_bytes(frame_map()->framesize_in_bytes()) % sizeof(intptr_t) == 0, "must be at least pointer aligned");
  _env->register_method(
    method(),
    osr_bci(),
    &_offsets,
    in_bytes(_frame_map->sp_offset_for_orig_pc()),
    code(),
    in_bytes(frame_map()->framesize_in_bytes()) / sizeof(intptr_t),
    debug_info_recorder()->_oopmaps,
    exception_handler_table(),
    implicit_exception_table(),
    compiler(),
    has_unsafe_access(),
    SharedRuntime::is_wide_vector(max_vector_size())
  );
}


void Compilation::compile_method() {
  {
    PhaseTraceTime timeit(_t_setup);

    // setup compilation
    initialize();
  }

  if (!method()->can_be_compiled()) {
    // Prevent race condition 6328518.
    // This can happen if the method is obsolete or breakpointed.
    bailout("Bailing out because method is not compilable");
    return;
  }

  if (_env->jvmti_can_hotswap_or_post_breakpoint()) {
    // We can assert evol_method because method->can_be_compiled is true.
    dependency_recorder()->assert_evol_method(method());
  }

  if (env()->break_at_compile()) {
    BREAKPOINT;
  }

#ifndef PRODUCT
  if (PrintCFGToFile) {
    CFGPrinter::print_compilation(this);
  }
#endif

  // compile method
  int frame_size = compile_java_method();

  // bailout if method couldn't be compiled
  // Note: make sure we mark the method as not compilable!
  CHECK_BAILOUT();

  if (should_install_code()) {
    // install code
    PhaseTraceTime timeit(_t_codeinstall);
    install_code(frame_size);
  }

  if (log() != NULL) // Print code cache state into compiler log
    log()->code_cache_state();

  totalInstructionNodes += Instruction::number_of_instructions();
}


void Compilation::generate_exception_handler_table() {
  // Generate an ExceptionHandlerTable from the exception handler
  // information accumulated during the compilation.
  ExceptionInfoList* info_list = exception_info_list();

  if (info_list->length() == 0) {
    return;
  }

  // allocate some arrays for use by the collection code.
  const int num_handlers = 5;
  GrowableArray<intptr_t>* bcis = new GrowableArray<intptr_t>(num_handlers);
  GrowableArray<intptr_t>* scope_depths = new GrowableArray<intptr_t>(num_handlers);
  GrowableArray<intptr_t>* pcos = new GrowableArray<intptr_t>(num_handlers);

  for (int i = 0; i < info_list->length(); i++) {
    ExceptionInfo* info = info_list->at(i);
    XHandlers* handlers = info->exception_handlers();

    // empty the arrays
    bcis->trunc_to(0);
    scope_depths->trunc_to(0);
    pcos->trunc_to(0);

    int prev_scope = 0;
    for (int i = 0; i < handlers->length(); i++) {
      XHandler* handler = handlers->handler_at(i);
      assert(handler->entry_pco() != -1, "must have been generated");
      assert(handler->scope_count() >= prev_scope, "handlers should be sorted by scope");

      if (handler->scope_count() == prev_scope) {
        int e = bcis->find_from_end(handler->handler_bci());
        if (e >= 0 && scope_depths->at(e) == handler->scope_count()) {
          // two different handlers are declared to dispatch to the same
          // catch bci.  During parsing we created edges for each
          // handler but we really only need one.  The exception handler
          // table will also get unhappy if we try to declare both since
          // it's nonsensical.  Just skip this handler.
          continue;
        }
      }

      bcis->append(handler->handler_bci());
      if (handler->handler_bci() == -1) {
        // insert a wildcard handler at scope depth 0 so that the
        // exception lookup logic with find it.
        scope_depths->append(0);
      } else {
        scope_depths->append(handler->scope_count());
      }
      pcos->append(handler->entry_pco());

      // stop processing once we hit a catch any
      if (handler->is_catch_all()) {
        assert(i == handlers->length() - 1, "catch all must be last handler");
      }
      prev_scope = handler->scope_count();
    }
    exception_handler_table()->add_subtable(info->pco(), bcis, scope_depths, pcos);
  }
}

Compilation::Compilation(AbstractCompiler* compiler, ciEnv* env, ciMethod* method,
                         int osr_bci, BufferBlob* buffer_blob, bool install_code, DirectiveSet* directive)
: _next_id(0)
, _next_block_id(0)
, _compiler(compiler)
, _directive(directive)
, _env(env)
, _log(env->log())
, _method(method)
, _osr_bci(osr_bci)
, _hir(NULL)
, _max_spills(-1)
, _frame_map(NULL)
, _masm(NULL)
, _has_exception_handlers(false)
, _has_fpu_code(true)   // pessimistic assumption
, _has_unsafe_access(false)
, _would_profile(false)
, _has_method_handle_invokes(false)
, _has_reserved_stack_access(method->has_reserved_stack_access())
, _install_code(install_code)
, _bailout_msg(NULL)
, _exception_info_list(NULL)
, _allocator(NULL)
, _code(buffer_blob)
, _has_access_indexed(false)
, _interpreter_frame_size(0)
, _current_instruction(NULL)
#ifndef PRODUCT
, _last_instruction_printed(NULL)
, _cfg_printer_output(NULL)
#endif // PRODUCT
{
  PhaseTraceTime timeit(_t_compile);
  _arena = Thread::current()->resource_area();
  _env->set_compiler_data(this);
  _exception_info_list = new ExceptionInfoList();
  _implicit_exception_table.set_size(0);
#ifndef PRODUCT
  if (PrintCFGToFile) {
    _cfg_printer_output = new CFGPrinterOutput(this);
  }
#endif
  compile_method();
  if (bailed_out()) {
    _env->record_method_not_compilable(bailout_msg());
    if (is_profiling()) {
      // Compilation failed, create MDO, which would signal the interpreter
      // to start profiling on its own.
      _method->ensure_method_data();
    }
  } else if (is_profiling()) {
    ciMethodData *md = method->method_data_or_null();
    if (md != NULL) {
      md->set_would_profile(_would_profile);
    }
  }
}

Compilation::~Compilation() {
  // simulate crash during compilation
  assert(CICrashAt < 0 || (uintx)_env->compile_id() != (uintx)CICrashAt, "just as planned");

  _env->set_compiler_data(NULL);
}

void Compilation::add_exception_handlers_for_pco(int pco, XHandlers* exception_handlers) {
#ifndef PRODUCT
  if (PrintExceptionHandlers && Verbose) {
    tty->print_cr("  added exception scope for pco %d", pco);
  }
#endif
  // Note: we do not have program counters for these exception handlers yet
  exception_info_list()->push(new ExceptionInfo(pco, exception_handlers));
}


void Compilation::notice_inlined_method(ciMethod* method) {
  _env->notice_inlined_method(method);
}


void Compilation::bailout(const char* msg) {
  assert(msg != NULL, "bailout message must exist");
  if (!bailed_out()) {
    // keep first bailout message
    if (PrintCompilation || PrintBailouts) tty->print_cr("compilation bailout: %s", msg);
    _bailout_msg = msg;
  }
}

ciKlass* Compilation::cha_exact_type(ciType* type) {
  if (type != NULL && type->is_loaded() && type->is_instance_klass()) {
    ciInstanceKlass* ik = type->as_instance_klass();
    assert(ik->exact_klass() == NULL, "no cha for final klass");
    if (DeoptC1 && UseCHA && !(ik->has_subklass() || ik->is_interface())) {
      dependency_recorder()->assert_leaf_type(ik);
      return ik;
    }
  }
  return NULL;
}

void Compilation::print_timers() {
  tty->print_cr("    C1 Compile Time:      %7.3f s",      timers[_t_compile].seconds());
  tty->print_cr("       Setup time:          %7.3f s",    timers[_t_setup].seconds());

  {
    tty->print_cr("       Build HIR:           %7.3f s",    timers[_t_buildIR].seconds());
    tty->print_cr("         Parse:               %7.3f s", timers[_t_hir_parse].seconds());
    tty->print_cr("         Optimize blocks:     %7.3f s", timers[_t_optimize_blocks].seconds());
    tty->print_cr("         GVN:                 %7.3f s", timers[_t_gvn].seconds());
    tty->print_cr("         Null checks elim:    %7.3f s", timers[_t_optimize_null_checks].seconds());
    tty->print_cr("         Range checks elim:   %7.3f s", timers[_t_rangeCheckElimination].seconds());

    double other = timers[_t_buildIR].seconds() -
      (timers[_t_hir_parse].seconds() +
       timers[_t_optimize_blocks].seconds() +
       timers[_t_gvn].seconds() +
       timers[_t_optimize_null_checks].seconds() +
       timers[_t_rangeCheckElimination].seconds());
    if (other > 0) {
      tty->print_cr("         Other:               %7.3f s", other);
    }
  }

  {
    tty->print_cr("       Emit LIR:            %7.3f s",    timers[_t_emit_lir].seconds());
    tty->print_cr("         LIR Gen:             %7.3f s",   timers[_t_lirGeneration].seconds());
    tty->print_cr("         Linear Scan:         %7.3f s",   timers[_t_linearScan].seconds());
    NOT_PRODUCT(LinearScan::print_timers(timers[_t_linearScan].seconds()));

    double other = timers[_t_emit_lir].seconds() -
      (timers[_t_lirGeneration].seconds() +
       timers[_t_linearScan].seconds());
    if (other > 0) {
      tty->print_cr("         Other:               %7.3f s", other);
    }
  }

  tty->print_cr("       Code Emission:       %7.3f s",    timers[_t_codeemit].seconds());
  tty->print_cr("       Code Installation:   %7.3f s",    timers[_t_codeinstall].seconds());

  double other = timers[_t_compile].seconds() -
      (timers[_t_setup].seconds() +
       timers[_t_buildIR].seconds() +
       timers[_t_emit_lir].seconds() +
       timers[_t_codeemit].seconds() +
       timers[_t_codeinstall].seconds());
  if (other > 0) {
    tty->print_cr("       Other:               %7.3f s", other);
  }

  NOT_PRODUCT(LinearScan::print_statistics());
}


#ifndef PRODUCT
void Compilation::compile_only_this_method() {
  ResourceMark rm;
  fileStream stream(fopen("c1_compile_only", "wt"));
  stream.print_cr("# c1 compile only directives");
  compile_only_this_scope(&stream, hir()->top_scope());
}

void Compilation::compile_only_this_scope(outputStream* st, IRScope* scope) {
  st->print("CompileOnly=");
  scope->method()->holder()->name()->print_symbol_on(st);
  st->print(".");
  scope->method()->name()->print_symbol_on(st);
  st->cr();
}

void Compilation::exclude_this_method() {
  fileStream stream(fopen(".hotspot_compiler", "at"));
  stream.print("exclude ");
  method()->holder()->name()->print_symbol_on(&stream);
  stream.print(" ");
  method()->name()->print_symbol_on(&stream);
  stream.cr();
  stream.cr();
}

// Called from debugger to get the interval with 'reg_num' during register allocation.
Interval* find_interval(int reg_num) {
  return Compilation::current()->allocator()->find_interval_at(reg_num);
}

#endif // NOT PRODUCT
