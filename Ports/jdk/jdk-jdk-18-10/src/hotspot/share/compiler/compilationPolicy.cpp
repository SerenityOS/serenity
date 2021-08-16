/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "code/scopeDesc.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compilerOracle.hpp"
#include "memory/resourceArea.hpp"
#include "oops/methodData.hpp"
#include "oops/method.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/arguments.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointVerifiers.hpp"

#if INCLUDE_JVMCI
#include "jvmci/jvmci.hpp"
#endif

#ifdef COMPILER1
#include "c1/c1_Compiler.hpp"
#endif

#ifdef COMPILER2
#include "opto/c2compiler.hpp"
#endif

jlong CompilationPolicy::_start_time = 0;
int CompilationPolicy::_c1_count = 0;
int CompilationPolicy::_c2_count = 0;
double CompilationPolicy::_increase_threshold_at_ratio = 0;

void compilationPolicy_init() {
  CompilationPolicy::initialize();
}

int CompilationPolicy::compiler_count(CompLevel comp_level) {
  if (is_c1_compile(comp_level)) {
    return c1_count();
  } else if (is_c2_compile(comp_level)) {
    return c2_count();
  }
  return 0;
}

// Returns true if m must be compiled before executing it
// This is intended to force compiles for methods (usually for
// debugging) that would otherwise be interpreted for some reason.
bool CompilationPolicy::must_be_compiled(const methodHandle& m, int comp_level) {
  // Don't allow Xcomp to cause compiles in replay mode
  if (ReplayCompiles) return false;

  if (m->has_compiled_code()) return false;       // already compiled
  if (!can_be_compiled(m, comp_level)) return false;

  return !UseInterpreter ||                                              // must compile all methods
         (UseCompiler && AlwaysCompileLoopMethods && m->has_loops() && CompileBroker::should_compile_new_jobs()); // eagerly compile loop methods
}

void CompilationPolicy::compile_if_required(const methodHandle& m, TRAPS) {
  if (must_be_compiled(m)) {
    // This path is unusual, mostly used by the '-Xcomp' stress test mode.

    if (!THREAD->can_call_java() || THREAD->is_Compiler_thread()) {
      // don't force compilation, resolve was on behalf of compiler
      return;
    }
    if (m->method_holder()->is_not_initialized()) {
      // 'is_not_initialized' means not only '!is_initialized', but also that
      // initialization has not been started yet ('!being_initialized')
      // Do not force compilation of methods in uninitialized classes.
      // Note that doing this would throw an assert later,
      // in CompileBroker::compile_method.
      // We sometimes use the link resolver to do reflective lookups
      // even before classes are initialized.
      return;
    }
    CompLevel level = initial_compile_level(m);
    if (PrintTieredEvents) {
      print_event(COMPILE, m(), m(), InvocationEntryBci, level);
    }
    CompileBroker::compile_method(m, InvocationEntryBci, level, methodHandle(), 0, CompileTask::Reason_MustBeCompiled, THREAD);
  }
}

static inline CompLevel adjust_level_for_compilability_query(CompLevel comp_level) {
  if (comp_level == CompLevel_any) {
     if (CompilerConfig::is_c1_only()) {
       comp_level = CompLevel_simple;
     } else if (CompilerConfig::is_c2_or_jvmci_compiler_only()) {
       comp_level = CompLevel_full_optimization;
     }
  }
  return comp_level;
}

// Returns true if m is allowed to be compiled
bool CompilationPolicy::can_be_compiled(const methodHandle& m, int comp_level) {
  // allow any levels for WhiteBox
  assert(WhiteBoxAPI || comp_level == CompLevel_any || is_compile(comp_level), "illegal compilation level");

  if (m->is_abstract()) return false;
  if (DontCompileHugeMethods && m->code_size() > HugeMethodLimit) return false;

  // Math intrinsics should never be compiled as this can lead to
  // monotonicity problems because the interpreter will prefer the
  // compiled code to the intrinsic version.  This can't happen in
  // production because the invocation counter can't be incremented
  // but we shouldn't expose the system to this problem in testing
  // modes.
  if (!AbstractInterpreter::can_be_compiled(m)) {
    return false;
  }
  comp_level = adjust_level_for_compilability_query((CompLevel) comp_level);
  if (comp_level == CompLevel_any || is_compile(comp_level)) {
    return !m->is_not_compilable(comp_level);
  }
  return false;
}

// Returns true if m is allowed to be osr compiled
bool CompilationPolicy::can_be_osr_compiled(const methodHandle& m, int comp_level) {
  bool result = false;
  comp_level = adjust_level_for_compilability_query((CompLevel) comp_level);
  if (comp_level == CompLevel_any || is_compile(comp_level)) {
    result = !m->is_not_osr_compilable(comp_level);
  }
  return (result && can_be_compiled(m, comp_level));
}

bool CompilationPolicy::is_compilation_enabled() {
  // NOTE: CompileBroker::should_compile_new_jobs() checks for UseCompiler
  return CompileBroker::should_compile_new_jobs();
}

CompileTask* CompilationPolicy::select_task_helper(CompileQueue* compile_queue) {
  // Remove unloaded methods from the queue
  for (CompileTask* task = compile_queue->first(); task != NULL; ) {
    CompileTask* next = task->next();
    if (task->is_unloaded()) {
      compile_queue->remove_and_mark_stale(task);
    }
    task = next;
  }
#if INCLUDE_JVMCI
  if (UseJVMCICompiler && !BackgroundCompilation) {
    /*
     * In blocking compilation mode, the CompileBroker will make
     * compilations submitted by a JVMCI compiler thread non-blocking. These
     * compilations should be scheduled after all blocking compilations
     * to service non-compiler related compilations sooner and reduce the
     * chance of such compilations timing out.
     */
    for (CompileTask* task = compile_queue->first(); task != NULL; task = task->next()) {
      if (task->is_blocking()) {
        return task;
      }
    }
  }
#endif
  return compile_queue->first();
}

// Simple methods are as good being compiled with C1 as C2.
// Determine if a given method is such a case.
bool CompilationPolicy::is_trivial(Method* method) {
  if (method->is_accessor() ||
      method->is_constant_getter()) {
    return true;
  }
  return false;
}

bool CompilationPolicy::force_comp_at_level_simple(const methodHandle& method) {
  if (CompilationModeFlag::quick_internal()) {
#if INCLUDE_JVMCI
    if (UseJVMCICompiler) {
      AbstractCompiler* comp = CompileBroker::compiler(CompLevel_full_optimization);
      if (comp != NULL && comp->is_jvmci() && ((JVMCICompiler*) comp)->force_comp_at_level_simple(method)) {
        return true;
      }
    }
#endif
  }
  return false;
}

CompLevel CompilationPolicy::comp_level(Method* method) {
  CompiledMethod *nm = method->code();
  if (nm != NULL && nm->is_in_use()) {
    return (CompLevel)nm->comp_level();
  }
  return CompLevel_none;
}

// Call and loop predicates determine whether a transition to a higher
// compilation level should be performed (pointers to predicate functions
// are passed to common()).
// Tier?LoadFeedback is basically a coefficient that determines of
// how many methods per compiler thread can be in the queue before
// the threshold values double.
class LoopPredicate : AllStatic {
public:
  static bool apply_scaled(const methodHandle& method, CompLevel cur_level, int i, int b, double scale) {
    double threshold_scaling;
    if (CompilerOracle::has_option_value(method, CompileCommand::CompileThresholdScaling, threshold_scaling)) {
      scale *= threshold_scaling;
    }
    switch(cur_level) {
    case CompLevel_none:
    case CompLevel_limited_profile:
      return b >= Tier3BackEdgeThreshold * scale;
    case CompLevel_full_profile:
      return b >= Tier4BackEdgeThreshold * scale;
    default:
      return true;
    }
  }

  static bool apply(int i, int b, CompLevel cur_level, const methodHandle& method) {
    double k = 1;
    switch(cur_level) {
    case CompLevel_none:
    // Fall through
    case CompLevel_limited_profile: {
      k = CompilationPolicy::threshold_scale(CompLevel_full_profile, Tier3LoadFeedback);
      break;
    }
    case CompLevel_full_profile: {
      k = CompilationPolicy::threshold_scale(CompLevel_full_optimization, Tier4LoadFeedback);
      break;
    }
    default:
      return true;
    }
    return apply_scaled(method, cur_level, i, b, k);
  }
};

class CallPredicate : AllStatic {
public:
  static bool apply_scaled(const methodHandle& method, CompLevel cur_level, int i, int b, double scale) {
    double threshold_scaling;
    if (CompilerOracle::has_option_value(method, CompileCommand::CompileThresholdScaling, threshold_scaling)) {
      scale *= threshold_scaling;
    }
    switch(cur_level) {
    case CompLevel_none:
    case CompLevel_limited_profile:
      return (i >= Tier3InvocationThreshold * scale) ||
             (i >= Tier3MinInvocationThreshold * scale && i + b >= Tier3CompileThreshold * scale);
    case CompLevel_full_profile:
      return (i >= Tier4InvocationThreshold * scale) ||
             (i >= Tier4MinInvocationThreshold * scale && i + b >= Tier4CompileThreshold * scale);
    default:
     return true;
    }
  }

  static bool apply(int i, int b, CompLevel cur_level, const methodHandle& method) {
    double k = 1;
    switch(cur_level) {
    case CompLevel_none:
    case CompLevel_limited_profile: {
      k = CompilationPolicy::threshold_scale(CompLevel_full_profile, Tier3LoadFeedback);
      break;
    }
    case CompLevel_full_profile: {
      k = CompilationPolicy::threshold_scale(CompLevel_full_optimization, Tier4LoadFeedback);
      break;
    }
    default:
      return true;
    }
    return apply_scaled(method, cur_level, i, b, k);
  }
};

double CompilationPolicy::threshold_scale(CompLevel level, int feedback_k) {
  int comp_count = compiler_count(level);
  if (comp_count > 0) {
    double queue_size = CompileBroker::queue_size(level);
    double k = queue_size / (feedback_k * comp_count) + 1;

    // Increase C1 compile threshold when the code cache is filled more
    // than specified by IncreaseFirstTierCompileThresholdAt percentage.
    // The main intention is to keep enough free space for C2 compiled code
    // to achieve peak performance if the code cache is under stress.
    if (CompilerConfig::is_tiered() && !CompilationModeFlag::disable_intermediate() && is_c1_compile(level))  {
      double current_reverse_free_ratio = CodeCache::reverse_free_ratio(CodeCache::get_code_blob_type(level));
      if (current_reverse_free_ratio > _increase_threshold_at_ratio) {
        k *= exp(current_reverse_free_ratio - _increase_threshold_at_ratio);
      }
    }
    return k;
  }
  return 1;
}

void CompilationPolicy::print_counters(const char* prefix, const Method* m) {
  int invocation_count = m->invocation_count();
  int backedge_count = m->backedge_count();
  MethodData* mdh = m->method_data();
  int mdo_invocations = 0, mdo_backedges = 0;
  int mdo_invocations_start = 0, mdo_backedges_start = 0;
  if (mdh != NULL) {
    mdo_invocations = mdh->invocation_count();
    mdo_backedges = mdh->backedge_count();
    mdo_invocations_start = mdh->invocation_count_start();
    mdo_backedges_start = mdh->backedge_count_start();
  }
  tty->print(" %stotal=%d,%d %smdo=%d(%d),%d(%d)", prefix,
      invocation_count, backedge_count, prefix,
      mdo_invocations, mdo_invocations_start,
      mdo_backedges, mdo_backedges_start);
  tty->print(" %smax levels=%d,%d", prefix,
      m->highest_comp_level(), m->highest_osr_comp_level());
}

// Print an event.
void CompilationPolicy::print_event(EventType type, const Method* m, const Method* im, int bci, CompLevel level) {
  bool inlinee_event = m != im;

  ttyLocker tty_lock;
  tty->print("%lf: [", os::elapsedTime());

  switch(type) {
  case CALL:
    tty->print("call");
    break;
  case LOOP:
    tty->print("loop");
    break;
  case COMPILE:
    tty->print("compile");
    break;
  case REMOVE_FROM_QUEUE:
    tty->print("remove-from-queue");
    break;
  case UPDATE_IN_QUEUE:
    tty->print("update-in-queue");
    break;
  case REPROFILE:
    tty->print("reprofile");
    break;
  case MAKE_NOT_ENTRANT:
    tty->print("make-not-entrant");
    break;
  default:
    tty->print("unknown");
  }

  tty->print(" level=%d ", level);

  ResourceMark rm;
  char *method_name = m->name_and_sig_as_C_string();
  tty->print("[%s", method_name);
  if (inlinee_event) {
    char *inlinee_name = im->name_and_sig_as_C_string();
    tty->print(" [%s]] ", inlinee_name);
  }
  else tty->print("] ");
  tty->print("@%d queues=%d,%d", bci, CompileBroker::queue_size(CompLevel_full_profile),
                                      CompileBroker::queue_size(CompLevel_full_optimization));

  tty->print(" rate=");
  if (m->prev_time() == 0) tty->print("n/a");
  else tty->print("%f", m->rate());

  tty->print(" k=%.2lf,%.2lf", threshold_scale(CompLevel_full_profile, Tier3LoadFeedback),
                               threshold_scale(CompLevel_full_optimization, Tier4LoadFeedback));

  if (type != COMPILE) {
    print_counters("", m);
    if (inlinee_event) {
      print_counters("inlinee ", im);
    }
    tty->print(" compilable=");
    bool need_comma = false;
    if (!m->is_not_compilable(CompLevel_full_profile)) {
      tty->print("c1");
      need_comma = true;
    }
    if (!m->is_not_osr_compilable(CompLevel_full_profile)) {
      if (need_comma) tty->print(",");
      tty->print("c1-osr");
      need_comma = true;
    }
    if (!m->is_not_compilable(CompLevel_full_optimization)) {
      if (need_comma) tty->print(",");
      tty->print("c2");
      need_comma = true;
    }
    if (!m->is_not_osr_compilable(CompLevel_full_optimization)) {
      if (need_comma) tty->print(",");
      tty->print("c2-osr");
    }
    tty->print(" status=");
    if (m->queued_for_compilation()) {
      tty->print("in-queue");
    } else tty->print("idle");
  }
  tty->print_cr("]");
}

void CompilationPolicy::initialize() {
  if (!CompilerConfig::is_interpreter_only()) {
    int count = CICompilerCount;
    bool c1_only = CompilerConfig::is_c1_only();
    bool c2_only = CompilerConfig::is_c2_or_jvmci_compiler_only();

#ifdef _LP64
    // Turn on ergonomic compiler count selection
    if (FLAG_IS_DEFAULT(CICompilerCountPerCPU) && FLAG_IS_DEFAULT(CICompilerCount)) {
      FLAG_SET_DEFAULT(CICompilerCountPerCPU, true);
    }
    if (CICompilerCountPerCPU) {
      // Simple log n seems to grow too slowly for tiered, try something faster: log n * log log n
      int log_cpu = log2i(os::active_processor_count());
      int loglog_cpu = log2i(MAX2(log_cpu, 1));
      count = MAX2(log_cpu * loglog_cpu * 3 / 2, 2);
      // Make sure there is enough space in the code cache to hold all the compiler buffers
      size_t c1_size = 0;
#ifdef COMPILER1
      c1_size = Compiler::code_buffer_size();
#endif
      size_t c2_size = 0;
#ifdef COMPILER2
      c2_size = C2Compiler::initial_code_buffer_size();
#endif
      size_t buffer_size = c1_only ? c1_size : (c1_size/3 + 2*c2_size/3);
      int max_count = (ReservedCodeCacheSize - (CodeCacheMinimumUseSpace DEBUG_ONLY(* 3))) / (int)buffer_size;
      if (count > max_count) {
        // Lower the compiler count such that all buffers fit into the code cache
        count = MAX2(max_count, c1_only ? 1 : 2);
      }
      FLAG_SET_ERGO(CICompilerCount, count);
    }
#else
    // On 32-bit systems, the number of compiler threads is limited to 3.
    // On these systems, the virtual address space available to the JVM
    // is usually limited to 2-4 GB (the exact value depends on the platform).
    // As the compilers (especially C2) can consume a large amount of
    // memory, scaling the number of compiler threads with the number of
    // available cores can result in the exhaustion of the address space
    /// available to the VM and thus cause the VM to crash.
    if (FLAG_IS_DEFAULT(CICompilerCount)) {
      count = 3;
      FLAG_SET_ERGO(CICompilerCount, count);
    }
#endif

    if (c1_only) {
      // No C2 compiler thread required
      set_c1_count(count);
    } else if (c2_only) {
      set_c2_count(count);
    } else {
      set_c1_count(MAX2(count / 3, 1));
      set_c2_count(MAX2(count - c1_count(), 1));
    }
    assert(count == c1_count() + c2_count(), "inconsistent compiler thread count");
    set_increase_threshold_at_ratio();
  }
  set_start_time(nanos_to_millis(os::javaTimeNanos()));
}


#ifdef ASSERT
bool CompilationPolicy::verify_level(CompLevel level) {
  if (TieredCompilation && level > TieredStopAtLevel) {
    return false;
  }
  // Check if there is a compiler to process the requested level
  if (!CompilerConfig::is_c1_enabled() && is_c1_compile(level)) {
    return false;
  }
  if (!CompilerConfig::is_c2_or_jvmci_compiler_enabled() && is_c2_compile(level)) {
    return false;
  }

  // Interpreter level is always valid.
  if (level == CompLevel_none) {
    return true;
  }
  if (CompilationModeFlag::normal()) {
    return true;
  } else if (CompilationModeFlag::quick_only()) {
    return level == CompLevel_simple;
  } else if (CompilationModeFlag::high_only()) {
    return level == CompLevel_full_optimization;
  } else if (CompilationModeFlag::high_only_quick_internal()) {
    return level == CompLevel_full_optimization || level == CompLevel_simple;
  }
  return false;
}
#endif


CompLevel CompilationPolicy::highest_compile_level() {
  CompLevel level = CompLevel_none;
  // Setup the maximum level availible for the current compiler configuration.
  if (!CompilerConfig::is_interpreter_only()) {
    if (CompilerConfig::is_c2_or_jvmci_compiler_enabled()) {
      level = CompLevel_full_optimization;
    } else if (CompilerConfig::is_c1_enabled()) {
      if (CompilerConfig::is_c1_simple_only()) {
        level = CompLevel_simple;
      } else {
        level = CompLevel_full_profile;
      }
    }
  }
  // Clamp the maximum level with TieredStopAtLevel.
  if (TieredCompilation) {
    level = MIN2(level, (CompLevel) TieredStopAtLevel);
  }

  // Fix it up if after the clamping it has become invalid.
  // Bring it monotonically down depending on the next available level for
  // the compilation mode.
  if (!CompilationModeFlag::normal()) {
    // a) quick_only - levels 2,3,4 are invalid; levels -1,0,1 are valid;
    // b) high_only - levels 1,2,3 are invalid; levels -1,0,4 are valid;
    // c) high_only_quick_internal - levels 2,3 are invalid; levels -1,0,1,4 are valid.
    if (CompilationModeFlag::quick_only()) {
      if (level == CompLevel_limited_profile || level == CompLevel_full_profile || level == CompLevel_full_optimization) {
        level = CompLevel_simple;
      }
    } else if (CompilationModeFlag::high_only()) {
      if (level == CompLevel_simple || level == CompLevel_limited_profile || level == CompLevel_full_profile) {
        level = CompLevel_none;
      }
    } else if (CompilationModeFlag::high_only_quick_internal()) {
      if (level == CompLevel_limited_profile || level == CompLevel_full_profile) {
        level = CompLevel_simple;
      }
    }
  }

  assert(verify_level(level), "Invalid highest compilation level: %d", level);
  return level;
}

CompLevel CompilationPolicy::limit_level(CompLevel level) {
  level = MIN2(level, highest_compile_level());
  assert(verify_level(level), "Invalid compilation level: %d", level);
  return level;
}

CompLevel CompilationPolicy::initial_compile_level(const methodHandle& method) {
  CompLevel level = CompLevel_any;
  if (CompilationModeFlag::normal()) {
    level = CompLevel_full_profile;
  } else if (CompilationModeFlag::quick_only()) {
    level = CompLevel_simple;
  } else if (CompilationModeFlag::high_only()) {
    level = CompLevel_full_optimization;
  } else if (CompilationModeFlag::high_only_quick_internal()) {
    if (force_comp_at_level_simple(method)) {
      level = CompLevel_simple;
    } else {
      level = CompLevel_full_optimization;
    }
  }
  assert(level != CompLevel_any, "Unhandled compilation mode");
  return limit_level(level);
}

// Set carry flags on the counters if necessary
void CompilationPolicy::handle_counter_overflow(Method* method) {
  MethodCounters *mcs = method->method_counters();
  if (mcs != NULL) {
    mcs->invocation_counter()->set_carry_on_overflow();
    mcs->backedge_counter()->set_carry_on_overflow();
  }
  MethodData* mdo = method->method_data();
  if (mdo != NULL) {
    mdo->invocation_counter()->set_carry_on_overflow();
    mdo->backedge_counter()->set_carry_on_overflow();
  }
}

// Called with the queue locked and with at least one element
CompileTask* CompilationPolicy::select_task(CompileQueue* compile_queue) {
  CompileTask *max_blocking_task = NULL;
  CompileTask *max_task = NULL;
  Method* max_method = NULL;

  jlong t = nanos_to_millis(os::javaTimeNanos());
  // Iterate through the queue and find a method with a maximum rate.
  for (CompileTask* task = compile_queue->first(); task != NULL;) {
    CompileTask* next_task = task->next();
    Method* method = task->method();
    // If a method was unloaded or has been stale for some time, remove it from the queue.
    // Blocking tasks and tasks submitted from whitebox API don't become stale
    if (task->is_unloaded() || (task->can_become_stale() && is_stale(t, TieredCompileTaskTimeout, method) && !is_old(method))) {
      if (!task->is_unloaded()) {
        if (PrintTieredEvents) {
          print_event(REMOVE_FROM_QUEUE, method, method, task->osr_bci(), (CompLevel) task->comp_level());
        }
        method->clear_queued_for_compilation();
      }
      compile_queue->remove_and_mark_stale(task);
      task = next_task;
      continue;
    }
    update_rate(t, method);
    if (max_task == NULL || compare_methods(method, max_method)) {
      // Select a method with the highest rate
      max_task = task;
      max_method = method;
    }

    if (task->is_blocking()) {
      if (max_blocking_task == NULL || compare_methods(method, max_blocking_task->method())) {
        max_blocking_task = task;
      }
    }

    task = next_task;
  }

  if (max_blocking_task != NULL) {
    // In blocking compilation mode, the CompileBroker will make
    // compilations submitted by a JVMCI compiler thread non-blocking. These
    // compilations should be scheduled after all blocking compilations
    // to service non-compiler related compilations sooner and reduce the
    // chance of such compilations timing out.
    max_task = max_blocking_task;
    max_method = max_task->method();
  }

  methodHandle max_method_h(Thread::current(), max_method);

  if (max_task != NULL && max_task->comp_level() == CompLevel_full_profile && TieredStopAtLevel > CompLevel_full_profile &&
      max_method != NULL && is_method_profiled(max_method_h) && !Arguments::is_compiler_only()) {
    max_task->set_comp_level(CompLevel_limited_profile);

    if (CompileBroker::compilation_is_complete(max_method_h, max_task->osr_bci(), CompLevel_limited_profile)) {
      if (PrintTieredEvents) {
        print_event(REMOVE_FROM_QUEUE, max_method, max_method, max_task->osr_bci(), (CompLevel)max_task->comp_level());
      }
      compile_queue->remove_and_mark_stale(max_task);
      max_method->clear_queued_for_compilation();
      return NULL;
    }

    if (PrintTieredEvents) {
      print_event(UPDATE_IN_QUEUE, max_method, max_method, max_task->osr_bci(), (CompLevel)max_task->comp_level());
    }
  }

  return max_task;
}

void CompilationPolicy::reprofile(ScopeDesc* trap_scope, bool is_osr) {
  for (ScopeDesc* sd = trap_scope;; sd = sd->sender()) {
    if (PrintTieredEvents) {
      print_event(REPROFILE, sd->method(), sd->method(), InvocationEntryBci, CompLevel_none);
    }
    MethodData* mdo = sd->method()->method_data();
    if (mdo != NULL) {
      mdo->reset_start_counters();
    }
    if (sd->is_top()) break;
  }
}

nmethod* CompilationPolicy::event(const methodHandle& method, const methodHandle& inlinee,
                                      int branch_bci, int bci, CompLevel comp_level, CompiledMethod* nm, TRAPS) {
  if (PrintTieredEvents) {
    print_event(bci == InvocationEntryBci ? CALL : LOOP, method(), inlinee(), bci, comp_level);
  }

  if (comp_level == CompLevel_none &&
      JvmtiExport::can_post_interpreter_events() &&
      THREAD->is_interp_only_mode()) {
    return NULL;
  }
  if (ReplayCompiles) {
    // Don't trigger other compiles in testing mode
    return NULL;
  }

  handle_counter_overflow(method());
  if (method() != inlinee()) {
    handle_counter_overflow(inlinee());
  }

  if (bci == InvocationEntryBci) {
    method_invocation_event(method, inlinee, comp_level, nm, THREAD);
  } else {
    // method == inlinee if the event originated in the main method
    method_back_branch_event(method, inlinee, bci, comp_level, nm, THREAD);
    // Check if event led to a higher level OSR compilation
    CompLevel expected_comp_level = MIN2(CompLevel_full_optimization, static_cast<CompLevel>(comp_level + 1));
    if (!CompilationModeFlag::disable_intermediate() && inlinee->is_not_osr_compilable(expected_comp_level)) {
      // It's not possble to reach the expected level so fall back to simple.
      expected_comp_level = CompLevel_simple;
    }
    CompLevel max_osr_level = static_cast<CompLevel>(inlinee->highest_osr_comp_level());
    if (max_osr_level >= expected_comp_level) { // fast check to avoid locking in a typical scenario
      nmethod* osr_nm = inlinee->lookup_osr_nmethod_for(bci, expected_comp_level, false);
      assert(osr_nm == NULL || osr_nm->comp_level() >= expected_comp_level, "lookup_osr_nmethod_for is broken");
      if (osr_nm != NULL && osr_nm->comp_level() != comp_level) {
        // Perform OSR with new nmethod
        return osr_nm;
      }
    }
  }
  return NULL;
}

// Check if the method can be compiled, change level if necessary
void CompilationPolicy::compile(const methodHandle& mh, int bci, CompLevel level, TRAPS) {
  assert(verify_level(level), "Invalid compilation level requested: %d", level);

  if (level == CompLevel_none) {
    if (mh->has_compiled_code()) {
      // Happens when we switch to interpreter to profile.
      MutexLocker ml(Compile_lock);
      NoSafepointVerifier nsv;
      if (mh->has_compiled_code()) {
        mh->code()->make_not_used();
      }
      // Deoptimize immediately (we don't have to wait for a compile).
      JavaThread* jt = THREAD;
      RegisterMap map(jt, false);
      frame fr = jt->last_frame().sender(&map);
      Deoptimization::deoptimize_frame(jt, fr.id());
    }
    return;
  }

  if (!CompilationModeFlag::disable_intermediate()) {
    // Check if the method can be compiled. If it cannot be compiled with C1, continue profiling
    // in the interpreter and then compile with C2 (the transition function will request that,
    // see common() ). If the method cannot be compiled with C2 but still can with C1, compile it with
    // pure C1.
    if ((bci == InvocationEntryBci && !can_be_compiled(mh, level))) {
      if (level == CompLevel_full_optimization && can_be_compiled(mh, CompLevel_simple)) {
        compile(mh, bci, CompLevel_simple, THREAD);
      }
      return;
    }
    if ((bci != InvocationEntryBci && !can_be_osr_compiled(mh, level))) {
      if (level == CompLevel_full_optimization && can_be_osr_compiled(mh, CompLevel_simple)) {
        nmethod* osr_nm = mh->lookup_osr_nmethod_for(bci, CompLevel_simple, false);
        if (osr_nm != NULL && osr_nm->comp_level() > CompLevel_simple) {
          // Invalidate the existing OSR nmethod so that a compile at CompLevel_simple is permitted.
          osr_nm->make_not_entrant();
        }
        compile(mh, bci, CompLevel_simple, THREAD);
      }
      return;
    }
  }
  if (bci != InvocationEntryBci && mh->is_not_osr_compilable(level)) {
    return;
  }
  if (!CompileBroker::compilation_is_in_queue(mh)) {
    if (PrintTieredEvents) {
      print_event(COMPILE, mh(), mh(), bci, level);
    }
    int hot_count = (bci == InvocationEntryBci) ? mh->invocation_count() : mh->backedge_count();
    update_rate(nanos_to_millis(os::javaTimeNanos()), mh());
    CompileBroker::compile_method(mh, bci, level, mh, hot_count, CompileTask::Reason_Tiered, THREAD);
  }
}

// update_rate() is called from select_task() while holding a compile queue lock.
void CompilationPolicy::update_rate(jlong t, Method* m) {
  // Skip update if counters are absent.
  // Can't allocate them since we are holding compile queue lock.
  if (m->method_counters() == NULL)  return;

  if (is_old(m)) {
    // We don't remove old methods from the queue,
    // so we can just zero the rate.
    m->set_rate(0);
    return;
  }

  // We don't update the rate if we've just came out of a safepoint.
  // delta_s is the time since last safepoint in milliseconds.
  jlong delta_s = t - SafepointTracing::end_of_last_safepoint_ms();
  jlong delta_t = t - (m->prev_time() != 0 ? m->prev_time() : start_time()); // milliseconds since the last measurement
  // How many events were there since the last time?
  int event_count = m->invocation_count() + m->backedge_count();
  int delta_e = event_count - m->prev_event_count();

  // We should be running for at least 1ms.
  if (delta_s >= TieredRateUpdateMinTime) {
    // And we must've taken the previous point at least 1ms before.
    if (delta_t >= TieredRateUpdateMinTime && delta_e > 0) {
      m->set_prev_time(t);
      m->set_prev_event_count(event_count);
      m->set_rate((float)delta_e / (float)delta_t); // Rate is events per millisecond
    } else {
      if (delta_t > TieredRateUpdateMaxTime && delta_e == 0) {
        // If nothing happened for 25ms, zero the rate. Don't modify prev values.
        m->set_rate(0);
      }
    }
  }
}

// Check if this method has been stale for a given number of milliseconds.
// See select_task().
bool CompilationPolicy::is_stale(jlong t, jlong timeout, Method* m) {
  jlong delta_s = t - SafepointTracing::end_of_last_safepoint_ms();
  jlong delta_t = t - m->prev_time();
  if (delta_t > timeout && delta_s > timeout) {
    int event_count = m->invocation_count() + m->backedge_count();
    int delta_e = event_count - m->prev_event_count();
    // Return true if there were no events.
    return delta_e == 0;
  }
  return false;
}

// We don't remove old methods from the compile queue even if they have
// very low activity. See select_task().
bool CompilationPolicy::is_old(Method* method) {
  return method->invocation_count() > 50000 || method->backedge_count() > 500000;
}

double CompilationPolicy::weight(Method* method) {
  return (double)(method->rate() + 1) *
    (method->invocation_count() + 1) * (method->backedge_count() + 1);
}

// Apply heuristics and return true if x should be compiled before y
bool CompilationPolicy::compare_methods(Method* x, Method* y) {
  if (x->highest_comp_level() > y->highest_comp_level()) {
    // recompilation after deopt
    return true;
  } else
    if (x->highest_comp_level() == y->highest_comp_level()) {
      if (weight(x) > weight(y)) {
        return true;
      }
    }
  return false;
}

// Is method profiled enough?
bool CompilationPolicy::is_method_profiled(const methodHandle& method) {
  MethodData* mdo = method->method_data();
  if (mdo != NULL) {
    int i = mdo->invocation_count_delta();
    int b = mdo->backedge_count_delta();
    return CallPredicate::apply_scaled(method, CompLevel_full_profile, i, b, 1);
  }
  return false;
}


// Determine is a method is mature.
bool CompilationPolicy::is_mature(Method* method) {
  methodHandle mh(Thread::current(), method);
  MethodData* mdo = method->method_data();
  if (mdo != NULL) {
    int i = mdo->invocation_count();
    int b = mdo->backedge_count();
    double k = ProfileMaturityPercentage / 100.0;
    return CallPredicate::apply_scaled(mh, CompLevel_full_profile, i, b, k) || LoopPredicate::apply_scaled(mh, CompLevel_full_profile, i, b, k);
  }
  return false;
}

// If a method is old enough and is still in the interpreter we would want to
// start profiling without waiting for the compiled method to arrive.
// We also take the load on compilers into the account.
bool CompilationPolicy::should_create_mdo(const methodHandle& method, CompLevel cur_level) {
  if (cur_level != CompLevel_none || force_comp_at_level_simple(method) || CompilationModeFlag::quick_only() || !ProfileInterpreter) {
    return false;
  }
  int i = method->invocation_count();
  int b = method->backedge_count();
  double k = Tier0ProfilingStartPercentage / 100.0;

  // If the top level compiler is not keeping up, delay profiling.
  if (CompileBroker::queue_size(CompLevel_full_optimization) <= Tier0Delay * compiler_count(CompLevel_full_optimization)) {
    return CallPredicate::apply_scaled(method, CompLevel_full_profile, i, b, k) || LoopPredicate::apply_scaled(method, CompLevel_full_profile, i, b, k);
  }
  return false;
}

// Inlining control: if we're compiling a profiled method with C1 and the callee
// is known to have OSRed in a C2 version, don't inline it.
bool CompilationPolicy::should_not_inline(ciEnv* env, ciMethod* callee) {
  CompLevel comp_level = (CompLevel)env->comp_level();
  if (comp_level == CompLevel_full_profile ||
      comp_level == CompLevel_limited_profile) {
    return callee->highest_osr_comp_level() == CompLevel_full_optimization;
  }
  return false;
}

// Create MDO if necessary.
void CompilationPolicy::create_mdo(const methodHandle& mh, JavaThread* THREAD) {
  if (mh->is_native() ||
      mh->is_abstract() ||
      mh->is_accessor() ||
      mh->is_constant_getter()) {
    return;
  }
  if (mh->method_data() == NULL) {
    Method::build_interpreter_method_data(mh, CHECK_AND_CLEAR);
  }
  if (ProfileInterpreter) {
    MethodData* mdo = mh->method_data();
    if (mdo != NULL) {
      frame last_frame = THREAD->last_frame();
      if (last_frame.is_interpreted_frame() && mh == last_frame.interpreter_frame_method()) {
        int bci = last_frame.interpreter_frame_bci();
        address dp = mdo->bci_to_dp(bci);
        last_frame.interpreter_frame_set_mdp(dp);
      }
    }
  }
}



/*
 * Method states:
 *   0 - interpreter (CompLevel_none)
 *   1 - pure C1 (CompLevel_simple)
 *   2 - C1 with invocation and backedge counting (CompLevel_limited_profile)
 *   3 - C1 with full profiling (CompLevel_full_profile)
 *   4 - C2 or Graal (CompLevel_full_optimization)
 *
 * Common state transition patterns:
 * a. 0 -> 3 -> 4.
 *    The most common path. But note that even in this straightforward case
 *    profiling can start at level 0 and finish at level 3.
 *
 * b. 0 -> 2 -> 3 -> 4.
 *    This case occurs when the load on C2 is deemed too high. So, instead of transitioning
 *    into state 3 directly and over-profiling while a method is in the C2 queue we transition to
 *    level 2 and wait until the load on C2 decreases. This path is disabled for OSRs.
 *
 * c. 0 -> (3->2) -> 4.
 *    In this case we enqueue a method for compilation at level 3, but the C1 queue is long enough
 *    to enable the profiling to fully occur at level 0. In this case we change the compilation level
 *    of the method to 2 while the request is still in-queue, because it'll allow it to run much faster
 *    without full profiling while c2 is compiling.
 *
 * d. 0 -> 3 -> 1 or 0 -> 2 -> 1.
 *    After a method was once compiled with C1 it can be identified as trivial and be compiled to
 *    level 1. These transition can also occur if a method can't be compiled with C2 but can with C1.
 *
 * e. 0 -> 4.
 *    This can happen if a method fails C1 compilation (it will still be profiled in the interpreter)
 *    or because of a deopt that didn't require reprofiling (compilation won't happen in this case because
 *    the compiled version already exists).
 *
 * Note that since state 0 can be reached from any other state via deoptimization different loops
 * are possible.
 *
 */

// Common transition function. Given a predicate determines if a method should transition to another level.
template<typename Predicate>
CompLevel CompilationPolicy::common(const methodHandle& method, CompLevel cur_level, bool disable_feedback) {
  CompLevel next_level = cur_level;
  int i = method->invocation_count();
  int b = method->backedge_count();

  if (force_comp_at_level_simple(method)) {
    next_level = CompLevel_simple;
  } else {
    if (is_trivial(method())) {
      next_level = CompilationModeFlag::disable_intermediate() ? CompLevel_full_optimization : CompLevel_simple;
    } else {
      switch(cur_level) {
      default: break;
      case CompLevel_none:
        // If we were at full profile level, would we switch to full opt?
        if (common<Predicate>(method, CompLevel_full_profile, disable_feedback) == CompLevel_full_optimization) {
          next_level = CompLevel_full_optimization;
        } else if (!CompilationModeFlag::disable_intermediate() && Predicate::apply(i, b, cur_level, method)) {
          // C1-generated fully profiled code is about 30% slower than the limited profile
          // code that has only invocation and backedge counters. The observation is that
          // if C2 queue is large enough we can spend too much time in the fully profiled code
          // while waiting for C2 to pick the method from the queue. To alleviate this problem
          // we introduce a feedback on the C2 queue size. If the C2 queue is sufficiently long
          // we choose to compile a limited profiled version and then recompile with full profiling
          // when the load on C2 goes down.
          if (!disable_feedback && CompileBroker::queue_size(CompLevel_full_optimization) >
              Tier3DelayOn * compiler_count(CompLevel_full_optimization)) {
            next_level = CompLevel_limited_profile;
          } else {
            next_level = CompLevel_full_profile;
          }
        }
        break;
      case CompLevel_limited_profile:
        if (is_method_profiled(method)) {
          // Special case: we got here because this method was fully profiled in the interpreter.
          next_level = CompLevel_full_optimization;
        } else {
          MethodData* mdo = method->method_data();
          if (mdo != NULL) {
            if (mdo->would_profile()) {
              if (disable_feedback || (CompileBroker::queue_size(CompLevel_full_optimization) <=
                                       Tier3DelayOff * compiler_count(CompLevel_full_optimization) &&
                                       Predicate::apply(i, b, cur_level, method))) {
                next_level = CompLevel_full_profile;
              }
            } else {
              next_level = CompLevel_full_optimization;
            }
          } else {
            // If there is no MDO we need to profile
            if (disable_feedback || (CompileBroker::queue_size(CompLevel_full_optimization) <=
                                     Tier3DelayOff * compiler_count(CompLevel_full_optimization) &&
                                     Predicate::apply(i, b, cur_level, method))) {
              next_level = CompLevel_full_profile;
            }
          }
        }
        break;
      case CompLevel_full_profile:
        {
          MethodData* mdo = method->method_data();
          if (mdo != NULL) {
            if (mdo->would_profile() || CompilationModeFlag::disable_intermediate()) {
              int mdo_i = mdo->invocation_count_delta();
              int mdo_b = mdo->backedge_count_delta();
              if (Predicate::apply(mdo_i, mdo_b, cur_level, method)) {
                next_level = CompLevel_full_optimization;
              }
            } else {
              next_level = CompLevel_full_optimization;
            }
          }
        }
        break;
      }
    }
  }
  return (next_level != cur_level) ? limit_level(next_level) : next_level;
}



// Determine if a method should be compiled with a normal entry point at a different level.
CompLevel CompilationPolicy::call_event(const methodHandle& method, CompLevel cur_level, Thread* thread) {
  CompLevel osr_level = MIN2((CompLevel) method->highest_osr_comp_level(), common<LoopPredicate>(method, cur_level, true));
  CompLevel next_level = common<CallPredicate>(method, cur_level);

  // If OSR method level is greater than the regular method level, the levels should be
  // equalized by raising the regular method level in order to avoid OSRs during each
  // invocation of the method.
  if (osr_level == CompLevel_full_optimization && cur_level == CompLevel_full_profile) {
    MethodData* mdo = method->method_data();
    guarantee(mdo != NULL, "MDO should not be NULL");
    if (mdo->invocation_count() >= 1) {
      next_level = CompLevel_full_optimization;
    }
  } else {
    next_level = MAX2(osr_level, next_level);
  }
  return next_level;
}

// Determine if we should do an OSR compilation of a given method.
CompLevel CompilationPolicy::loop_event(const methodHandle& method, CompLevel cur_level, Thread* thread) {
  CompLevel next_level = common<LoopPredicate>(method, cur_level, true);
  if (cur_level == CompLevel_none) {
    // If there is a live OSR method that means that we deopted to the interpreter
    // for the transition.
    CompLevel osr_level = MIN2((CompLevel)method->highest_osr_comp_level(), next_level);
    if (osr_level > CompLevel_none) {
      return osr_level;
    }
  }
  return next_level;
}

// Handle the invocation event.
void CompilationPolicy::method_invocation_event(const methodHandle& mh, const methodHandle& imh,
                                                      CompLevel level, CompiledMethod* nm, TRAPS) {
  if (should_create_mdo(mh, level)) {
    create_mdo(mh, THREAD);
  }
  CompLevel next_level = call_event(mh, level, THREAD);
  if (next_level != level) {
    if (is_compilation_enabled() && !CompileBroker::compilation_is_in_queue(mh)) {
      compile(mh, InvocationEntryBci, next_level, THREAD);
    }
  }
}

// Handle the back branch event. Notice that we can compile the method
// with a regular entry from here.
void CompilationPolicy::method_back_branch_event(const methodHandle& mh, const methodHandle& imh,
                                                     int bci, CompLevel level, CompiledMethod* nm, TRAPS) {
  if (should_create_mdo(mh, level)) {
    create_mdo(mh, THREAD);
  }
  // Check if MDO should be created for the inlined method
  if (should_create_mdo(imh, level)) {
    create_mdo(imh, THREAD);
  }

  if (is_compilation_enabled()) {
    CompLevel next_osr_level = loop_event(imh, level, THREAD);
    CompLevel max_osr_level = (CompLevel)imh->highest_osr_comp_level();
    // At the very least compile the OSR version
    if (!CompileBroker::compilation_is_in_queue(imh) && (next_osr_level != level)) {
      compile(imh, bci, next_osr_level, CHECK);
    }

    // Use loop event as an opportunity to also check if there's been
    // enough calls.
    CompLevel cur_level, next_level;
    if (mh() != imh()) { // If there is an enclosing method
      {
        guarantee(nm != NULL, "Should have nmethod here");
        cur_level = comp_level(mh());
        next_level = call_event(mh, cur_level, THREAD);

        if (max_osr_level == CompLevel_full_optimization) {
          // The inlinee OSRed to full opt, we need to modify the enclosing method to avoid deopts
          bool make_not_entrant = false;
          if (nm->is_osr_method()) {
            // This is an osr method, just make it not entrant and recompile later if needed
            make_not_entrant = true;
          } else {
            if (next_level != CompLevel_full_optimization) {
              // next_level is not full opt, so we need to recompile the
              // enclosing method without the inlinee
              cur_level = CompLevel_none;
              make_not_entrant = true;
            }
          }
          if (make_not_entrant) {
            if (PrintTieredEvents) {
              int osr_bci = nm->is_osr_method() ? nm->osr_entry_bci() : InvocationEntryBci;
              print_event(MAKE_NOT_ENTRANT, mh(), mh(), osr_bci, level);
            }
            nm->make_not_entrant();
          }
        }
        // Fix up next_level if necessary to avoid deopts
        if (next_level == CompLevel_limited_profile && max_osr_level == CompLevel_full_profile) {
          next_level = CompLevel_full_profile;
        }
        if (cur_level != next_level) {
          if (!CompileBroker::compilation_is_in_queue(mh)) {
            compile(mh, InvocationEntryBci, next_level, THREAD);
          }
        }
      }
    } else {
      cur_level = comp_level(mh());
      next_level = call_event(mh, cur_level, THREAD);
      if (next_level != cur_level) {
        if (!CompileBroker::compilation_is_in_queue(mh)) {
          compile(mh, InvocationEntryBci, next_level, THREAD);
        }
      }
    }
  }
}

