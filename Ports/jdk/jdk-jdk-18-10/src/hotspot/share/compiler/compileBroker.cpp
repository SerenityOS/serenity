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
#include "jvm.h"
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/codeHeapState.hpp"
#include "code/dependencyContext.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileLog.hpp"
#include "compiler/compilerEvent.hpp"
#include "compiler/compilerOracle.hpp"
#include "compiler/directivesParser.hpp"
#include "interpreter/linkResolver.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/methodData.hpp"
#include "oops/method.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/nativeLookup.hpp"
#include "prims/whitebox.hpp"
#include "runtime/atomic.hpp"
#include "runtime/escapeBarrier.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/perfData.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/sweeper.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/vframe.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/macros.hpp"
#ifdef COMPILER1
#include "c1/c1_Compiler.hpp"
#endif
#if INCLUDE_JVMCI
#include "jvmci/jvmciEnv.hpp"
#include "jvmci/jvmciRuntime.hpp"
#endif
#ifdef COMPILER2
#include "opto/c2compiler.hpp"
#endif

#ifdef DTRACE_ENABLED

// Only bother with this argument setup if dtrace is available

#define DTRACE_METHOD_COMPILE_BEGIN_PROBE(method, comp_name)             \
  {                                                                      \
    Symbol* klass_name = (method)->klass_name();                         \
    Symbol* name = (method)->name();                                     \
    Symbol* signature = (method)->signature();                           \
    HOTSPOT_METHOD_COMPILE_BEGIN(                                        \
      (char *) comp_name, strlen(comp_name),                             \
      (char *) klass_name->bytes(), klass_name->utf8_length(),           \
      (char *) name->bytes(), name->utf8_length(),                       \
      (char *) signature->bytes(), signature->utf8_length());            \
  }

#define DTRACE_METHOD_COMPILE_END_PROBE(method, comp_name, success)      \
  {                                                                      \
    Symbol* klass_name = (method)->klass_name();                         \
    Symbol* name = (method)->name();                                     \
    Symbol* signature = (method)->signature();                           \
    HOTSPOT_METHOD_COMPILE_END(                                          \
      (char *) comp_name, strlen(comp_name),                             \
      (char *) klass_name->bytes(), klass_name->utf8_length(),           \
      (char *) name->bytes(), name->utf8_length(),                       \
      (char *) signature->bytes(), signature->utf8_length(), (success)); \
  }

#else //  ndef DTRACE_ENABLED

#define DTRACE_METHOD_COMPILE_BEGIN_PROBE(method, comp_name)
#define DTRACE_METHOD_COMPILE_END_PROBE(method, comp_name, success)

#endif // ndef DTRACE_ENABLED

bool CompileBroker::_initialized = false;
volatile bool CompileBroker::_should_block = false;
volatile int  CompileBroker::_print_compilation_warning = 0;
volatile jint CompileBroker::_should_compile_new_jobs = run_compilation;

// The installed compiler(s)
AbstractCompiler* CompileBroker::_compilers[2];

// The maximum numbers of compiler threads to be determined during startup.
int CompileBroker::_c1_count = 0;
int CompileBroker::_c2_count = 0;

// An array of compiler names as Java String objects
jobject* CompileBroker::_compiler1_objects = NULL;
jobject* CompileBroker::_compiler2_objects = NULL;

CompileLog** CompileBroker::_compiler1_logs = NULL;
CompileLog** CompileBroker::_compiler2_logs = NULL;

// These counters are used to assign an unique ID to each compilation.
volatile jint CompileBroker::_compilation_id     = 0;
volatile jint CompileBroker::_osr_compilation_id = 0;
volatile jint CompileBroker::_native_compilation_id = 0;

// Performance counters
PerfCounter* CompileBroker::_perf_total_compilation = NULL;
PerfCounter* CompileBroker::_perf_osr_compilation = NULL;
PerfCounter* CompileBroker::_perf_standard_compilation = NULL;

PerfCounter* CompileBroker::_perf_total_bailout_count = NULL;
PerfCounter* CompileBroker::_perf_total_invalidated_count = NULL;
PerfCounter* CompileBroker::_perf_total_compile_count = NULL;
PerfCounter* CompileBroker::_perf_total_osr_compile_count = NULL;
PerfCounter* CompileBroker::_perf_total_standard_compile_count = NULL;

PerfCounter* CompileBroker::_perf_sum_osr_bytes_compiled = NULL;
PerfCounter* CompileBroker::_perf_sum_standard_bytes_compiled = NULL;
PerfCounter* CompileBroker::_perf_sum_nmethod_size = NULL;
PerfCounter* CompileBroker::_perf_sum_nmethod_code_size = NULL;

PerfStringVariable* CompileBroker::_perf_last_method = NULL;
PerfStringVariable* CompileBroker::_perf_last_failed_method = NULL;
PerfStringVariable* CompileBroker::_perf_last_invalidated_method = NULL;
PerfVariable*       CompileBroker::_perf_last_compile_type = NULL;
PerfVariable*       CompileBroker::_perf_last_compile_size = NULL;
PerfVariable*       CompileBroker::_perf_last_failed_type = NULL;
PerfVariable*       CompileBroker::_perf_last_invalidated_type = NULL;

// Timers and counters for generating statistics
elapsedTimer CompileBroker::_t_total_compilation;
elapsedTimer CompileBroker::_t_osr_compilation;
elapsedTimer CompileBroker::_t_standard_compilation;
elapsedTimer CompileBroker::_t_invalidated_compilation;
elapsedTimer CompileBroker::_t_bailedout_compilation;

int CompileBroker::_total_bailout_count            = 0;
int CompileBroker::_total_invalidated_count        = 0;
int CompileBroker::_total_compile_count            = 0;
int CompileBroker::_total_osr_compile_count        = 0;
int CompileBroker::_total_standard_compile_count   = 0;
int CompileBroker::_total_compiler_stopped_count   = 0;
int CompileBroker::_total_compiler_restarted_count = 0;

int CompileBroker::_sum_osr_bytes_compiled         = 0;
int CompileBroker::_sum_standard_bytes_compiled    = 0;
int CompileBroker::_sum_nmethod_size               = 0;
int CompileBroker::_sum_nmethod_code_size          = 0;

long CompileBroker::_peak_compilation_time         = 0;

CompilerStatistics CompileBroker::_stats_per_level[CompLevel_full_optimization];

CompileQueue* CompileBroker::_c2_compile_queue     = NULL;
CompileQueue* CompileBroker::_c1_compile_queue     = NULL;



class CompilationLog : public StringEventLog {
 public:
  CompilationLog() : StringEventLog("Compilation events", "jit") {
  }

  void log_compile(JavaThread* thread, CompileTask* task) {
    StringLogMessage lm;
    stringStream sstr(lm.buffer(), lm.size());
    // msg.time_stamp().update_to(tty->time_stamp().ticks());
    task->print(&sstr, NULL, true, false);
    log(thread, "%s", (const char*)lm);
  }

  void log_nmethod(JavaThread* thread, nmethod* nm) {
    log(thread, "nmethod %d%s " INTPTR_FORMAT " code [" INTPTR_FORMAT ", " INTPTR_FORMAT "]",
        nm->compile_id(), nm->is_osr_method() ? "%" : "",
        p2i(nm), p2i(nm->code_begin()), p2i(nm->code_end()));
  }

  void log_failure(JavaThread* thread, CompileTask* task, const char* reason, const char* retry_message) {
    StringLogMessage lm;
    lm.print("%4d   COMPILE SKIPPED: %s", task->compile_id(), reason);
    if (retry_message != NULL) {
      lm.append(" (%s)", retry_message);
    }
    lm.print("\n");
    log(thread, "%s", (const char*)lm);
  }

  void log_metaspace_failure(const char* reason) {
    ResourceMark rm;
    StringLogMessage lm;
    lm.print("%4d   COMPILE PROFILING SKIPPED: %s", -1, reason);
    lm.print("\n");
    log(JavaThread::current(), "%s", (const char*)lm);
  }
};

static CompilationLog* _compilation_log = NULL;

bool compileBroker_init() {
  if (LogEvents) {
    _compilation_log = new CompilationLog();
  }

  // init directives stack, adding default directive
  DirectivesStack::init();

  if (DirectivesParser::has_file()) {
    return DirectivesParser::parse_from_flag();
  } else if (CompilerDirectivesPrint) {
    // Print default directive even when no other was added
    DirectivesStack::print(tty);
  }

  return true;
}

CompileTaskWrapper::CompileTaskWrapper(CompileTask* task) {
  CompilerThread* thread = CompilerThread::current();
  thread->set_task(task);
  CompileLog*     log  = thread->log();
  if (log != NULL && !task->is_unloaded())  task->log_task_start(log);
}

CompileTaskWrapper::~CompileTaskWrapper() {
  CompilerThread* thread = CompilerThread::current();
  CompileTask* task = thread->task();
  CompileLog*  log  = thread->log();
  if (log != NULL && !task->is_unloaded())  task->log_task_done(log);
  thread->set_task(NULL);
  task->set_code_handle(NULL);
  thread->set_env(NULL);
  if (task->is_blocking()) {
    bool free_task = false;
    {
      MutexLocker notifier(thread, task->lock());
      task->mark_complete();
#if INCLUDE_JVMCI
      if (CompileBroker::compiler(task->comp_level())->is_jvmci()) {
        if (!task->has_waiter()) {
          // The waiting thread timed out and thus did not free the task.
          free_task = true;
        }
        task->set_blocking_jvmci_compile_state(NULL);
      }
#endif
      if (!free_task) {
        // Notify the waiting thread that the compilation has completed
        // so that it can free the task.
        task->lock()->notify_all();
      }
    }
    if (free_task) {
      // The task can only be freed once the task lock is released.
      CompileTask::free(task);
    }
  } else {
    task->mark_complete();

    // By convention, the compiling thread is responsible for
    // recycling a non-blocking CompileTask.
    CompileTask::free(task);
  }
}

/**
 * Check if a CompilerThread can be removed and update count if requested.
 */
bool CompileBroker::can_remove(CompilerThread *ct, bool do_it) {
  assert(UseDynamicNumberOfCompilerThreads, "or shouldn't be here");
  if (!ReduceNumberOfCompilerThreads) return false;

  AbstractCompiler *compiler = ct->compiler();
  int compiler_count = compiler->num_compiler_threads();
  bool c1 = compiler->is_c1();

  // Keep at least 1 compiler thread of each type.
  if (compiler_count < 2) return false;

  // Keep thread alive for at least some time.
  if (ct->idle_time_millis() < (c1 ? 500 : 100)) return false;

#if INCLUDE_JVMCI
  if (compiler->is_jvmci()) {
    // Handles for JVMCI thread objects may get released concurrently.
    if (do_it) {
      assert(CompileThread_lock->owner() == ct, "must be holding lock");
    } else {
      // Skip check if it's the last thread and let caller check again.
      return true;
    }
  }
#endif

  // We only allow the last compiler thread of each type to get removed.
  jobject last_compiler = c1 ? compiler1_object(compiler_count - 1)
                             : compiler2_object(compiler_count - 1);
  if (ct->threadObj() == JNIHandles::resolve_non_null(last_compiler)) {
    if (do_it) {
      assert_locked_or_safepoint(CompileThread_lock); // Update must be consistent.
      compiler->set_num_compiler_threads(compiler_count - 1);
#if INCLUDE_JVMCI
      if (compiler->is_jvmci()) {
        // Old j.l.Thread object can die when no longer referenced elsewhere.
        JNIHandles::destroy_global(compiler2_object(compiler_count - 1));
        _compiler2_objects[compiler_count - 1] = NULL;
      }
#endif
    }
    return true;
  }
  return false;
}

/**
 * Add a CompileTask to a CompileQueue.
 */
void CompileQueue::add(CompileTask* task) {
  assert(MethodCompileQueue_lock->owned_by_self(), "must own lock");

  task->set_next(NULL);
  task->set_prev(NULL);

  if (_last == NULL) {
    // The compile queue is empty.
    assert(_first == NULL, "queue is empty");
    _first = task;
    _last = task;
  } else {
    // Append the task to the queue.
    assert(_last->next() == NULL, "not last");
    _last->set_next(task);
    task->set_prev(_last);
    _last = task;
  }
  ++_size;

  // Mark the method as being in the compile queue.
  task->method()->set_queued_for_compilation();

  if (CIPrintCompileQueue) {
    print_tty();
  }

  if (LogCompilation && xtty != NULL) {
    task->log_task_queued();
  }

  // Notify CompilerThreads that a task is available.
  MethodCompileQueue_lock->notify_all();
}

/**
 * Empties compilation queue by putting all compilation tasks onto
 * a freelist. Furthermore, the method wakes up all threads that are
 * waiting on a compilation task to finish. This can happen if background
 * compilation is disabled.
 */
void CompileQueue::free_all() {
  MutexLocker mu(MethodCompileQueue_lock);
  CompileTask* next = _first;

  // Iterate over all tasks in the compile queue
  while (next != NULL) {
    CompileTask* current = next;
    next = current->next();
    {
      // Wake up thread that blocks on the compile task.
      MutexLocker ct_lock(current->lock());
      current->lock()->notify();
    }
    // Put the task back on the freelist.
    CompileTask::free(current);
  }
  _first = NULL;

  // Wake up all threads that block on the queue.
  MethodCompileQueue_lock->notify_all();
}

/**
 * Get the next CompileTask from a CompileQueue
 */
CompileTask* CompileQueue::get() {
  // save methods from RedefineClasses across safepoint
  // across MethodCompileQueue_lock below.
  methodHandle save_method;
  methodHandle save_hot_method;

  MonitorLocker locker(MethodCompileQueue_lock);
  // If _first is NULL we have no more compile jobs. There are two reasons for
  // having no compile jobs: First, we compiled everything we wanted. Second,
  // we ran out of code cache so compilation has been disabled. In the latter
  // case we perform code cache sweeps to free memory such that we can re-enable
  // compilation.
  while (_first == NULL) {
    // Exit loop if compilation is disabled forever
    if (CompileBroker::is_compilation_disabled_forever()) {
      return NULL;
    }

    // If there are no compilation tasks and we can compile new jobs
    // (i.e., there is enough free space in the code cache) there is
    // no need to invoke the sweeper. As a result, the hotness of methods
    // remains unchanged. This behavior is desired, since we want to keep
    // the stable state, i.e., we do not want to evict methods from the
    // code cache if it is unnecessary.
    // We need a timed wait here, since compiler threads can exit if compilation
    // is disabled forever. We use 5 seconds wait time; the exiting of compiler threads
    // is not critical and we do not want idle compiler threads to wake up too often.
    locker.wait(5*1000);

    if (UseDynamicNumberOfCompilerThreads && _first == NULL) {
      // Still nothing to compile. Give caller a chance to stop this thread.
      if (CompileBroker::can_remove(CompilerThread::current(), false)) return NULL;
    }
  }

  if (CompileBroker::is_compilation_disabled_forever()) {
    return NULL;
  }

  CompileTask* task;
  {
    NoSafepointVerifier nsv;
    task = CompilationPolicy::select_task(this);
    if (task != NULL) {
      task = task->select_for_compilation();
    }
  }

  if (task != NULL) {
    // Save method pointers across unlock safepoint.  The task is removed from
    // the compilation queue, which is walked during RedefineClasses.
    Thread* thread = Thread::current();
    save_method = methodHandle(thread, task->method());
    save_hot_method = methodHandle(thread, task->hot_method());

    remove(task);
  }
  purge_stale_tasks(); // may temporarily release MCQ lock
  return task;
}

// Clean & deallocate stale compile tasks.
// Temporarily releases MethodCompileQueue lock.
void CompileQueue::purge_stale_tasks() {
  assert(MethodCompileQueue_lock->owned_by_self(), "must own lock");
  if (_first_stale != NULL) {
    // Stale tasks are purged when MCQ lock is released,
    // but _first_stale updates are protected by MCQ lock.
    // Once task processing starts and MCQ lock is released,
    // other compiler threads can reuse _first_stale.
    CompileTask* head = _first_stale;
    _first_stale = NULL;
    {
      MutexUnlocker ul(MethodCompileQueue_lock);
      for (CompileTask* task = head; task != NULL; ) {
        CompileTask* next_task = task->next();
        CompileTaskWrapper ctw(task); // Frees the task
        task->set_failure_reason("stale task");
        task = next_task;
      }
    }
  }
}

void CompileQueue::remove(CompileTask* task) {
  assert(MethodCompileQueue_lock->owned_by_self(), "must own lock");
  if (task->prev() != NULL) {
    task->prev()->set_next(task->next());
  } else {
    // max is the first element
    assert(task == _first, "Sanity");
    _first = task->next();
  }

  if (task->next() != NULL) {
    task->next()->set_prev(task->prev());
  } else {
    // max is the last element
    assert(task == _last, "Sanity");
    _last = task->prev();
  }
  --_size;
}

void CompileQueue::remove_and_mark_stale(CompileTask* task) {
  assert(MethodCompileQueue_lock->owned_by_self(), "must own lock");
  remove(task);

  // Enqueue the task for reclamation (should be done outside MCQ lock)
  task->set_next(_first_stale);
  task->set_prev(NULL);
  _first_stale = task;
}

// methods in the compile queue need to be marked as used on the stack
// so that they don't get reclaimed by Redefine Classes
void CompileQueue::mark_on_stack() {
  CompileTask* task = _first;
  while (task != NULL) {
    task->mark_on_stack();
    task = task->next();
  }
}


CompileQueue* CompileBroker::compile_queue(int comp_level) {
  if (is_c2_compile(comp_level)) return _c2_compile_queue;
  if (is_c1_compile(comp_level)) return _c1_compile_queue;
  return NULL;
}

void CompileBroker::print_compile_queues(outputStream* st) {
  st->print_cr("Current compiles: ");

  char buf[2000];
  int buflen = sizeof(buf);
  Threads::print_threads_compiling(st, buf, buflen, /* short_form = */ true);

  st->cr();
  if (_c1_compile_queue != NULL) {
    _c1_compile_queue->print(st);
  }
  if (_c2_compile_queue != NULL) {
    _c2_compile_queue->print(st);
  }
}

void CompileQueue::print(outputStream* st) {
  assert_locked_or_safepoint(MethodCompileQueue_lock);
  st->print_cr("%s:", name());
  CompileTask* task = _first;
  if (task == NULL) {
    st->print_cr("Empty");
  } else {
    while (task != NULL) {
      task->print(st, NULL, true, true);
      task = task->next();
    }
  }
  st->cr();
}

void CompileQueue::print_tty() {
  ResourceMark rm;
  stringStream ss;
  // Dump the compile queue into a buffer before locking the tty
  print(&ss);
  {
    ttyLocker ttyl;
    tty->print("%s", ss.as_string());
  }
}

CompilerCounters::CompilerCounters() {
  _current_method[0] = '\0';
  _compile_type = CompileBroker::no_compile;
}

#if INCLUDE_JFR && COMPILER2_OR_JVMCI
// It appends new compiler phase names to growable array phase_names(a new CompilerPhaseType mapping
// in compiler/compilerEvent.cpp) and registers it with its serializer.
//
// c2 uses explicit CompilerPhaseType idToPhase mapping in opto/phasetype.hpp,
// so if c2 is used, it should be always registered first.
// This function is called during vm initialization.
void register_jfr_phasetype_serializer(CompilerType compiler_type) {
  ResourceMark rm;
  static bool first_registration = true;
  if (compiler_type == compiler_jvmci) {
    CompilerEvent::PhaseEvent::get_phase_id("NOT_A_PHASE_NAME", false, false, false);
    first_registration = false;
#ifdef COMPILER2
  } else if (compiler_type == compiler_c2) {
    assert(first_registration, "invariant"); // c2 must be registered first.
    GrowableArray<const char*>* c2_phase_names = new GrowableArray<const char*>(PHASE_NUM_TYPES);
    for (int i = 0; i < PHASE_NUM_TYPES; i++) {
      const char* phase_name = CompilerPhaseTypeHelper::to_string((CompilerPhaseType) i);
      CompilerEvent::PhaseEvent::get_phase_id(phase_name, false, false, false);
    }
    first_registration = false;
#endif // COMPILER2
  }
}
#endif // INCLUDE_JFR && COMPILER2_OR_JVMCI

// ------------------------------------------------------------------
// CompileBroker::compilation_init
//
// Initialize the Compilation object
void CompileBroker::compilation_init_phase1(JavaThread* THREAD) {
  // No need to initialize compilation system if we do not use it.
  if (!UseCompiler) {
    return;
  }
  // Set the interface to the current compiler(s).
  _c1_count = CompilationPolicy::c1_count();
  _c2_count = CompilationPolicy::c2_count();

#if INCLUDE_JVMCI
  if (EnableJVMCI) {
    // This is creating a JVMCICompiler singleton.
    JVMCICompiler* jvmci = new JVMCICompiler();

    if (UseJVMCICompiler) {
      _compilers[1] = jvmci;
      if (FLAG_IS_DEFAULT(JVMCIThreads)) {
        if (BootstrapJVMCI) {
          // JVMCI will bootstrap so give it more threads
          _c2_count = MIN2(32, os::active_processor_count());
        }
      } else {
        _c2_count = JVMCIThreads;
      }
      if (FLAG_IS_DEFAULT(JVMCIHostThreads)) {
      } else {
#ifdef COMPILER1
        _c1_count = JVMCIHostThreads;
#endif // COMPILER1
      }
    }
  }
#endif // INCLUDE_JVMCI

#ifdef COMPILER1
  if (_c1_count > 0) {
    _compilers[0] = new Compiler();
  }
#endif // COMPILER1

#ifdef COMPILER2
  if (true JVMCI_ONLY( && !UseJVMCICompiler)) {
    if (_c2_count > 0) {
      _compilers[1] = new C2Compiler();
      // Register c2 first as c2 CompilerPhaseType idToPhase mapping is explicit.
      // idToPhase mapping for c2 is in opto/phasetype.hpp
      JFR_ONLY(register_jfr_phasetype_serializer(compiler_c2);)
    }
  }
#endif // COMPILER2

#if INCLUDE_JVMCI
   // Register after c2 registration.
   // JVMCI CompilerPhaseType idToPhase mapping is dynamic.
   if (EnableJVMCI) {
     JFR_ONLY(register_jfr_phasetype_serializer(compiler_jvmci);)
   }
#endif // INCLUDE_JVMCI

  // Start the compiler thread(s) and the sweeper thread
  init_compiler_sweeper_threads();
  // totalTime performance counter is always created as it is required
  // by the implementation of java.lang.management.CompilationMXBean.
  {
    // Ensure OOM leads to vm_exit_during_initialization.
    EXCEPTION_MARK;
    _perf_total_compilation =
                 PerfDataManager::create_counter(JAVA_CI, "totalTime",
                                                 PerfData::U_Ticks, CHECK);
  }

  if (UsePerfData) {

    EXCEPTION_MARK;

    // create the jvmstat performance counters
    _perf_osr_compilation =
                 PerfDataManager::create_counter(SUN_CI, "osrTime",
                                                 PerfData::U_Ticks, CHECK);

    _perf_standard_compilation =
                 PerfDataManager::create_counter(SUN_CI, "standardTime",
                                                 PerfData::U_Ticks, CHECK);

    _perf_total_bailout_count =
                 PerfDataManager::create_counter(SUN_CI, "totalBailouts",
                                                 PerfData::U_Events, CHECK);

    _perf_total_invalidated_count =
                 PerfDataManager::create_counter(SUN_CI, "totalInvalidates",
                                                 PerfData::U_Events, CHECK);

    _perf_total_compile_count =
                 PerfDataManager::create_counter(SUN_CI, "totalCompiles",
                                                 PerfData::U_Events, CHECK);
    _perf_total_osr_compile_count =
                 PerfDataManager::create_counter(SUN_CI, "osrCompiles",
                                                 PerfData::U_Events, CHECK);

    _perf_total_standard_compile_count =
                 PerfDataManager::create_counter(SUN_CI, "standardCompiles",
                                                 PerfData::U_Events, CHECK);

    _perf_sum_osr_bytes_compiled =
                 PerfDataManager::create_counter(SUN_CI, "osrBytes",
                                                 PerfData::U_Bytes, CHECK);

    _perf_sum_standard_bytes_compiled =
                 PerfDataManager::create_counter(SUN_CI, "standardBytes",
                                                 PerfData::U_Bytes, CHECK);

    _perf_sum_nmethod_size =
                 PerfDataManager::create_counter(SUN_CI, "nmethodSize",
                                                 PerfData::U_Bytes, CHECK);

    _perf_sum_nmethod_code_size =
                 PerfDataManager::create_counter(SUN_CI, "nmethodCodeSize",
                                                 PerfData::U_Bytes, CHECK);

    _perf_last_method =
                 PerfDataManager::create_string_variable(SUN_CI, "lastMethod",
                                       CompilerCounters::cmname_buffer_length,
                                       "", CHECK);

    _perf_last_failed_method =
            PerfDataManager::create_string_variable(SUN_CI, "lastFailedMethod",
                                       CompilerCounters::cmname_buffer_length,
                                       "", CHECK);

    _perf_last_invalidated_method =
        PerfDataManager::create_string_variable(SUN_CI, "lastInvalidatedMethod",
                                     CompilerCounters::cmname_buffer_length,
                                     "", CHECK);

    _perf_last_compile_type =
             PerfDataManager::create_variable(SUN_CI, "lastType",
                                              PerfData::U_None,
                                              (jlong)CompileBroker::no_compile,
                                              CHECK);

    _perf_last_compile_size =
             PerfDataManager::create_variable(SUN_CI, "lastSize",
                                              PerfData::U_Bytes,
                                              (jlong)CompileBroker::no_compile,
                                              CHECK);


    _perf_last_failed_type =
             PerfDataManager::create_variable(SUN_CI, "lastFailedType",
                                              PerfData::U_None,
                                              (jlong)CompileBroker::no_compile,
                                              CHECK);

    _perf_last_invalidated_type =
         PerfDataManager::create_variable(SUN_CI, "lastInvalidatedType",
                                          PerfData::U_None,
                                          (jlong)CompileBroker::no_compile,
                                          CHECK);
  }
}

// Completes compiler initialization. Compilation requests submitted
// prior to this will be silently ignored.
void CompileBroker::compilation_init_phase2() {
  _initialized = true;
}

Handle CompileBroker::create_thread_oop(const char* name, TRAPS) {
  Handle thread_oop = JavaThread::create_system_thread_object(name, false /* not visible */, CHECK_NH);
  return thread_oop;
}

#if defined(ASSERT) && COMPILER2_OR_JVMCI
// Stress testing. Dedicated threads revert optimizations based on escape analysis concurrently to
// the running java application.  Configured with vm options DeoptimizeObjectsALot*.
class DeoptimizeObjectsALotThread : public JavaThread {

  static void deopt_objs_alot_thread_entry(JavaThread* thread, TRAPS);
  void deoptimize_objects_alot_loop_single();
  void deoptimize_objects_alot_loop_all();

public:
  DeoptimizeObjectsALotThread() : JavaThread(&deopt_objs_alot_thread_entry) { }

  bool is_hidden_from_external_view() const      { return true; }
};

// Entry for DeoptimizeObjectsALotThread. The threads are started in
// CompileBroker::init_compiler_sweeper_threads() iff DeoptimizeObjectsALot is enabled
void DeoptimizeObjectsALotThread::deopt_objs_alot_thread_entry(JavaThread* thread, TRAPS) {
    DeoptimizeObjectsALotThread* dt = ((DeoptimizeObjectsALotThread*) thread);
    bool enter_single_loop;
    {
      MonitorLocker ml(dt, EscapeBarrier_lock, Mutex::_no_safepoint_check_flag);
      static int single_thread_count = 0;
      enter_single_loop = single_thread_count++ < DeoptimizeObjectsALotThreadCountSingle;
    }
    if (enter_single_loop) {
      dt->deoptimize_objects_alot_loop_single();
    } else {
      dt->deoptimize_objects_alot_loop_all();
    }
  }

// Execute EscapeBarriers in an endless loop to revert optimizations based on escape analysis. Each
// barrier targets a single thread which is selected round robin.
void DeoptimizeObjectsALotThread::deoptimize_objects_alot_loop_single() {
  HandleMark hm(this);
  while (true) {
    for (JavaThreadIteratorWithHandle jtiwh; JavaThread *deoptee_thread = jtiwh.next(); ) {
      { // Begin new scope for escape barrier
        HandleMarkCleaner hmc(this);
        ResourceMark rm(this);
        EscapeBarrier eb(true, this, deoptee_thread);
        eb.deoptimize_objects(100);
      }
      // Now sleep after the escape barriers destructor resumed deoptee_thread.
      sleep(DeoptimizeObjectsALotInterval);
    }
  }
}

// Execute EscapeBarriers in an endless loop to revert optimizations based on escape analysis. Each
// barrier targets all java threads in the vm at once.
void DeoptimizeObjectsALotThread::deoptimize_objects_alot_loop_all() {
  HandleMark hm(this);
  while (true) {
    { // Begin new scope for escape barrier
      HandleMarkCleaner hmc(this);
      ResourceMark rm(this);
      EscapeBarrier eb(true, this);
      eb.deoptimize_objects_all_threads();
    }
    // Now sleep after the escape barriers destructor resumed the java threads.
    sleep(DeoptimizeObjectsALotInterval);
  }
}
#endif // defined(ASSERT) && COMPILER2_OR_JVMCI


JavaThread* CompileBroker::make_thread(ThreadType type, jobject thread_handle, CompileQueue* queue, AbstractCompiler* comp, JavaThread* THREAD) {
  JavaThread* new_thread = NULL;

  switch (type) {
    case compiler_t:
      assert(comp != NULL, "Compiler instance missing.");
      if (!InjectCompilerCreationFailure || comp->num_compiler_threads() == 0) {
        CompilerCounters* counters = new CompilerCounters();
        new_thread = new CompilerThread(queue, counters);
      }
      break;
    case sweeper_t:
      new_thread = new CodeCacheSweeperThread();
      break;
#if defined(ASSERT) && COMPILER2_OR_JVMCI
    case deoptimizer_t:
      new_thread = new DeoptimizeObjectsALotThread();
      break;
#endif // ASSERT
    default:
      ShouldNotReachHere();
  }

  // At this point the new CompilerThread data-races with this startup
  // thread (which is the main thread and NOT the VM thread).
  // This means Java bytecodes being executed at startup can
  // queue compile jobs which will run at whatever default priority the
  // newly created CompilerThread runs at.


  // At this point it may be possible that no osthread was created for the
  // JavaThread due to lack of resources. We will handle that failure below.
  // Also check new_thread so that static analysis is happy.
  if (new_thread != NULL && new_thread->osthread() != NULL) {
    Handle thread_oop(THREAD, JNIHandles::resolve_non_null(thread_handle));

    if (type == compiler_t) {
      CompilerThread::cast(new_thread)->set_compiler(comp);
    }

    // Note that we cannot call os::set_priority because it expects Java
    // priorities and we are *explicitly* using OS priorities so that it's
    // possible to set the compiler thread priority higher than any Java
    // thread.

    int native_prio = CompilerThreadPriority;
    if (native_prio == -1) {
      if (UseCriticalCompilerThreadPriority) {
        native_prio = os::java_to_os_priority[CriticalPriority];
      } else {
        native_prio = os::java_to_os_priority[NearMaxPriority];
      }
    }
    os::set_native_priority(new_thread, native_prio);

    // Note that this only sets the JavaThread _priority field, which by
    // definition is limited to Java priorities and not OS priorities.
    JavaThread::start_internal_daemon(THREAD, new_thread, thread_oop, NearMaxPriority);

  } else { // osthread initialization failure
    if (UseDynamicNumberOfCompilerThreads && type == compiler_t
        && comp->num_compiler_threads() > 0) {
      // The new thread is not known to Thread-SMR yet so we can just delete.
      delete new_thread;
      return NULL;
    } else {
      vm_exit_during_initialization("java.lang.OutOfMemoryError",
                                    os::native_thread_creation_failed_msg());
    }
  }

  os::naked_yield(); // make sure that the compiler thread is started early (especially helpful on SOLARIS)

  return new_thread;
}


void CompileBroker::init_compiler_sweeper_threads() {
  NMethodSweeper::set_sweep_threshold_bytes(static_cast<size_t>(SweeperThreshold * ReservedCodeCacheSize / 100.0));
  log_info(codecache, sweep)("Sweeper threshold: " SIZE_FORMAT " bytes", NMethodSweeper::sweep_threshold_bytes());

  // Ensure any exceptions lead to vm_exit_during_initialization.
  EXCEPTION_MARK;
#if !defined(ZERO)
  assert(_c2_count > 0 || _c1_count > 0, "No compilers?");
#endif // !ZERO
  // Initialize the compilation queue
  if (_c2_count > 0) {
    const char* name = JVMCI_ONLY(UseJVMCICompiler ? "JVMCI compile queue" :) "C2 compile queue";
    _c2_compile_queue  = new CompileQueue(name);
    _compiler2_objects = NEW_C_HEAP_ARRAY(jobject, _c2_count, mtCompiler);
    _compiler2_logs = NEW_C_HEAP_ARRAY(CompileLog*, _c2_count, mtCompiler);
  }
  if (_c1_count > 0) {
    _c1_compile_queue  = new CompileQueue("C1 compile queue");
    _compiler1_objects = NEW_C_HEAP_ARRAY(jobject, _c1_count, mtCompiler);
    _compiler1_logs = NEW_C_HEAP_ARRAY(CompileLog*, _c1_count, mtCompiler);
  }

  char name_buffer[256];

  for (int i = 0; i < _c2_count; i++) {
    jobject thread_handle = NULL;
    // Create all j.l.Thread objects for C1 and C2 threads here, but only one
    // for JVMCI compiler which can create further ones on demand.
    JVMCI_ONLY(if (!UseJVMCICompiler || !UseDynamicNumberOfCompilerThreads || i == 0) {)
    // Create a name for our thread.
    sprintf(name_buffer, "%s CompilerThread%d", _compilers[1]->name(), i);
    Handle thread_oop = create_thread_oop(name_buffer, CHECK);
    thread_handle = JNIHandles::make_global(thread_oop);
    JVMCI_ONLY(})
    _compiler2_objects[i] = thread_handle;
    _compiler2_logs[i] = NULL;

    if (!UseDynamicNumberOfCompilerThreads || i == 0) {
      JavaThread *ct = make_thread(compiler_t, thread_handle, _c2_compile_queue, _compilers[1], THREAD);
      assert(ct != NULL, "should have been handled for initial thread");
      _compilers[1]->set_num_compiler_threads(i + 1);
      if (TraceCompilerThreads) {
        ResourceMark rm;
        ThreadsListHandle tlh;  // name() depends on the TLH.
        assert(tlh.includes(ct), "ct=" INTPTR_FORMAT " exited unexpectedly.", p2i(ct));
        tty->print_cr("Added initial compiler thread %s", ct->name());
      }
    }
  }

  for (int i = 0; i < _c1_count; i++) {
    // Create a name for our thread.
    sprintf(name_buffer, "C1 CompilerThread%d", i);
    Handle thread_oop = create_thread_oop(name_buffer, CHECK);
    jobject thread_handle = JNIHandles::make_global(thread_oop);
    _compiler1_objects[i] = thread_handle;
    _compiler1_logs[i] = NULL;

    if (!UseDynamicNumberOfCompilerThreads || i == 0) {
      JavaThread *ct = make_thread(compiler_t, thread_handle, _c1_compile_queue, _compilers[0], THREAD);
      assert(ct != NULL, "should have been handled for initial thread");
      _compilers[0]->set_num_compiler_threads(i + 1);
      if (TraceCompilerThreads) {
        ResourceMark rm;
        ThreadsListHandle tlh;  // name() depends on the TLH.
        assert(tlh.includes(ct), "ct=" INTPTR_FORMAT " exited unexpectedly.", p2i(ct));
        tty->print_cr("Added initial compiler thread %s", ct->name());
      }
    }
  }

  if (UsePerfData) {
    PerfDataManager::create_constant(SUN_CI, "threads", PerfData::U_Bytes, _c1_count + _c2_count, CHECK);
  }

  if (MethodFlushing) {
    // Initialize the sweeper thread
    Handle thread_oop = create_thread_oop("Sweeper thread", CHECK);
    jobject thread_handle = JNIHandles::make_local(THREAD, thread_oop());
    make_thread(sweeper_t, thread_handle, NULL, NULL, THREAD);
  }

#if defined(ASSERT) && COMPILER2_OR_JVMCI
  if (DeoptimizeObjectsALot) {
    // Initialize and start the object deoptimizer threads
    const int total_count = DeoptimizeObjectsALotThreadCountSingle + DeoptimizeObjectsALotThreadCountAll;
    for (int count = 0; count < total_count; count++) {
      Handle thread_oop = create_thread_oop("Deoptimize objects a lot single mode", CHECK);
      jobject thread_handle = JNIHandles::make_local(THREAD, thread_oop());
      make_thread(deoptimizer_t, thread_handle, NULL, NULL, THREAD);
    }
  }
#endif // defined(ASSERT) && COMPILER2_OR_JVMCI
}

void CompileBroker::possibly_add_compiler_threads(JavaThread* THREAD) {

  julong available_memory = os::available_memory();
  // If SegmentedCodeCache is off, both values refer to the single heap (with type CodeBlobType::All).
  size_t available_cc_np  = CodeCache::unallocated_capacity(CodeBlobType::MethodNonProfiled),
         available_cc_p   = CodeCache::unallocated_capacity(CodeBlobType::MethodProfiled);

  // Only do attempt to start additional threads if the lock is free.
  if (!CompileThread_lock->try_lock()) return;

  if (_c2_compile_queue != NULL) {
    int old_c2_count = _compilers[1]->num_compiler_threads();
    int new_c2_count = MIN4(_c2_count,
        _c2_compile_queue->size() / 2,
        (int)(available_memory / (200*M)),
        (int)(available_cc_np / (128*K)));

    for (int i = old_c2_count; i < new_c2_count; i++) {
#if INCLUDE_JVMCI
      if (UseJVMCICompiler) {
        // Native compiler threads as used in C1/C2 can reuse the j.l.Thread
        // objects as their existence is completely hidden from the rest of
        // the VM (and those compiler threads can't call Java code to do the
        // creation anyway). For JVMCI we have to create new j.l.Thread objects
        // as they are visible and we can see unexpected thread lifecycle
        // transitions if we bind them to new JavaThreads.
        if (!THREAD->can_call_java()) break;
        char name_buffer[256];
        sprintf(name_buffer, "%s CompilerThread%d", _compilers[1]->name(), i);
        Handle thread_oop;
        {
          // We have to give up the lock temporarily for the Java calls.
          MutexUnlocker mu(CompileThread_lock);
          thread_oop = create_thread_oop(name_buffer, THREAD);
        }
        if (HAS_PENDING_EXCEPTION) {
          if (TraceCompilerThreads) {
            ResourceMark rm;
            tty->print_cr("JVMCI compiler thread creation failed:");
            PENDING_EXCEPTION->print();
          }
          CLEAR_PENDING_EXCEPTION;
          break;
        }
        // Check if another thread has beaten us during the Java calls.
        if (_compilers[1]->num_compiler_threads() != i) break;
        jobject thread_handle = JNIHandles::make_global(thread_oop);
        assert(compiler2_object(i) == NULL, "Old one must be released!");
        _compiler2_objects[i] = thread_handle;
      }
#endif
      JavaThread *ct = make_thread(compiler_t, compiler2_object(i), _c2_compile_queue, _compilers[1], THREAD);
      if (ct == NULL) break;
      _compilers[1]->set_num_compiler_threads(i + 1);
      if (TraceCompilerThreads) {
        ResourceMark rm;
        ThreadsListHandle tlh;  // name() depends on the TLH.
        assert(tlh.includes(ct), "ct=" INTPTR_FORMAT " exited unexpectedly.", p2i(ct));
        tty->print_cr("Added compiler thread %s (available memory: %dMB, available non-profiled code cache: %dMB)",
                      ct->name(), (int)(available_memory/M), (int)(available_cc_np/M));
      }
    }
  }

  if (_c1_compile_queue != NULL) {
    int old_c1_count = _compilers[0]->num_compiler_threads();
    int new_c1_count = MIN4(_c1_count,
        _c1_compile_queue->size() / 4,
        (int)(available_memory / (100*M)),
        (int)(available_cc_p / (128*K)));

    for (int i = old_c1_count; i < new_c1_count; i++) {
      JavaThread *ct = make_thread(compiler_t, compiler1_object(i), _c1_compile_queue, _compilers[0], THREAD);
      if (ct == NULL) break;
      _compilers[0]->set_num_compiler_threads(i + 1);
      if (TraceCompilerThreads) {
        ResourceMark rm;
        ThreadsListHandle tlh;  // name() depends on the TLH.
        assert(tlh.includes(ct), "ct=" INTPTR_FORMAT " exited unexpectedly.", p2i(ct));
        tty->print_cr("Added compiler thread %s (available memory: %dMB, available profiled code cache: %dMB)",
                      ct->name(), (int)(available_memory/M), (int)(available_cc_p/M));
      }
    }
  }

  CompileThread_lock->unlock();
}


/**
 * Set the methods on the stack as on_stack so that redefine classes doesn't
 * reclaim them. This method is executed at a safepoint.
 */
void CompileBroker::mark_on_stack() {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity check");
  // Since we are at a safepoint, we do not need a lock to access
  // the compile queues.
  if (_c2_compile_queue != NULL) {
    _c2_compile_queue->mark_on_stack();
  }
  if (_c1_compile_queue != NULL) {
    _c1_compile_queue->mark_on_stack();
  }
}

// ------------------------------------------------------------------
// CompileBroker::compile_method
//
// Request compilation of a method.
void CompileBroker::compile_method_base(const methodHandle& method,
                                        int osr_bci,
                                        int comp_level,
                                        const methodHandle& hot_method,
                                        int hot_count,
                                        CompileTask::CompileReason compile_reason,
                                        bool blocking,
                                        Thread* thread) {
  guarantee(!method->is_abstract(), "cannot compile abstract methods");
  assert(method->method_holder()->is_instance_klass(),
         "sanity check");
  assert(!method->method_holder()->is_not_initialized(),
         "method holder must be initialized");
  assert(!method->is_method_handle_intrinsic(), "do not enqueue these guys");

  if (CIPrintRequests) {
    tty->print("request: ");
    method->print_short_name(tty);
    if (osr_bci != InvocationEntryBci) {
      tty->print(" osr_bci: %d", osr_bci);
    }
    tty->print(" level: %d comment: %s count: %d", comp_level, CompileTask::reason_name(compile_reason), hot_count);
    if (!hot_method.is_null()) {
      tty->print(" hot: ");
      if (hot_method() != method()) {
          hot_method->print_short_name(tty);
      } else {
        tty->print("yes");
      }
    }
    tty->cr();
  }

  // A request has been made for compilation.  Before we do any
  // real work, check to see if the method has been compiled
  // in the meantime with a definitive result.
  if (compilation_is_complete(method, osr_bci, comp_level)) {
    return;
  }

#ifndef PRODUCT
  if (osr_bci != -1 && !FLAG_IS_DEFAULT(OSROnlyBCI)) {
    if ((OSROnlyBCI > 0) ? (OSROnlyBCI != osr_bci) : (-OSROnlyBCI == osr_bci)) {
      // Positive OSROnlyBCI means only compile that bci.  Negative means don't compile that BCI.
      return;
    }
  }
#endif

  // If this method is already in the compile queue, then
  // we do not block the current thread.
  if (compilation_is_in_queue(method)) {
    // We may want to decay our counter a bit here to prevent
    // multiple denied requests for compilation.  This is an
    // open compilation policy issue. Note: The other possibility,
    // in the case that this is a blocking compile request, is to have
    // all subsequent blocking requesters wait for completion of
    // ongoing compiles. Note that in this case we'll need a protocol
    // for freeing the associated compile tasks. [Or we could have
    // a single static monitor on which all these waiters sleep.]
    return;
  }

  // Tiered policy requires MethodCounters to exist before adding a method to
  // the queue. Create if we don't have them yet.
  method->get_method_counters(thread);

  // Outputs from the following MutexLocker block:
  CompileTask* task     = NULL;
  CompileQueue* queue  = compile_queue(comp_level);

  // Acquire our lock.
  {
    MutexLocker locker(thread, MethodCompileQueue_lock);

    // Make sure the method has not slipped into the queues since
    // last we checked; note that those checks were "fast bail-outs".
    // Here we need to be more careful, see 14012000 below.
    if (compilation_is_in_queue(method)) {
      return;
    }

    // We need to check again to see if the compilation has
    // completed.  A previous compilation may have registered
    // some result.
    if (compilation_is_complete(method, osr_bci, comp_level)) {
      return;
    }

    // We now know that this compilation is not pending, complete,
    // or prohibited.  Assign a compile_id to this compilation
    // and check to see if it is in our [Start..Stop) range.
    int compile_id = assign_compile_id(method, osr_bci);
    if (compile_id == 0) {
      // The compilation falls outside the allowed range.
      return;
    }

#if INCLUDE_JVMCI
    if (UseJVMCICompiler && blocking) {
      // Don't allow blocking compiles for requests triggered by JVMCI.
      if (thread->is_Compiler_thread()) {
        blocking = false;
      }

      if (!UseJVMCINativeLibrary) {
        // Don't allow blocking compiles if inside a class initializer or while performing class loading
        vframeStream vfst(JavaThread::cast(thread));
        for (; !vfst.at_end(); vfst.next()) {
          if (vfst.method()->is_static_initializer() ||
              (vfst.method()->method_holder()->is_subclass_of(vmClasses::ClassLoader_klass()) &&
                  vfst.method()->name() == vmSymbols::loadClass_name())) {
            blocking = false;
            break;
          }
        }
      }

      // Don't allow blocking compilation requests to JVMCI
      // if JVMCI itself is not yet initialized
      if (!JVMCI::is_compiler_initialized() && compiler(comp_level)->is_jvmci()) {
        blocking = false;
      }

      // Don't allow blocking compilation requests if we are in JVMCIRuntime::shutdown
      // to avoid deadlock between compiler thread(s) and threads run at shutdown
      // such as the DestroyJavaVM thread.
      if (JVMCI::in_shutdown()) {
        blocking = false;
      }
    }
#endif // INCLUDE_JVMCI

    // We will enter the compilation in the queue.
    // 14012000: Note that this sets the queued_for_compile bits in
    // the target method. We can now reason that a method cannot be
    // queued for compilation more than once, as follows:
    // Before a thread queues a task for compilation, it first acquires
    // the compile queue lock, then checks if the method's queued bits
    // are set or it has already been compiled. Thus there can not be two
    // instances of a compilation task for the same method on the
    // compilation queue. Consider now the case where the compilation
    // thread has already removed a task for that method from the queue
    // and is in the midst of compiling it. In this case, the
    // queued_for_compile bits must be set in the method (and these
    // will be visible to the current thread, since the bits were set
    // under protection of the compile queue lock, which we hold now.
    // When the compilation completes, the compiler thread first sets
    // the compilation result and then clears the queued_for_compile
    // bits. Neither of these actions are protected by a barrier (or done
    // under the protection of a lock), so the only guarantee we have
    // (on machines with TSO (Total Store Order)) is that these values
    // will update in that order. As a result, the only combinations of
    // these bits that the current thread will see are, in temporal order:
    // <RESULT, QUEUE> :
    //     <0, 1> : in compile queue, but not yet compiled
    //     <1, 1> : compiled but queue bit not cleared
    //     <1, 0> : compiled and queue bit cleared
    // Because we first check the queue bits then check the result bits,
    // we are assured that we cannot introduce a duplicate task.
    // Note that if we did the tests in the reverse order (i.e. check
    // result then check queued bit), we could get the result bit before
    // the compilation completed, and the queue bit after the compilation
    // completed, and end up introducing a "duplicate" (redundant) task.
    // In that case, the compiler thread should first check if a method
    // has already been compiled before trying to compile it.
    // NOTE: in the event that there are multiple compiler threads and
    // there is de-optimization/recompilation, things will get hairy,
    // and in that case it's best to protect both the testing (here) of
    // these bits, and their updating (here and elsewhere) under a
    // common lock.
    task = create_compile_task(queue,
                               compile_id, method,
                               osr_bci, comp_level,
                               hot_method, hot_count, compile_reason,
                               blocking);
  }

  if (blocking) {
    wait_for_completion(task);
  }
}

nmethod* CompileBroker::compile_method(const methodHandle& method, int osr_bci,
                                       int comp_level,
                                       const methodHandle& hot_method, int hot_count,
                                       CompileTask::CompileReason compile_reason,
                                       TRAPS) {
  // Do nothing if compilebroker is not initalized or compiles are submitted on level none
  if (!_initialized || comp_level == CompLevel_none) {
    return NULL;
  }

  AbstractCompiler *comp = CompileBroker::compiler(comp_level);
  assert(comp != NULL, "Ensure we have a compiler");

  DirectiveSet* directive = DirectivesStack::getMatchingDirective(method, comp);
  // CompileBroker::compile_method can trap and can have pending aysnc exception.
  nmethod* nm = CompileBroker::compile_method(method, osr_bci, comp_level, hot_method, hot_count, compile_reason, directive, THREAD);
  DirectivesStack::release(directive);
  return nm;
}

nmethod* CompileBroker::compile_method(const methodHandle& method, int osr_bci,
                                         int comp_level,
                                         const methodHandle& hot_method, int hot_count,
                                         CompileTask::CompileReason compile_reason,
                                         DirectiveSet* directive,
                                         TRAPS) {

  // make sure arguments make sense
  assert(method->method_holder()->is_instance_klass(), "not an instance method");
  assert(osr_bci == InvocationEntryBci || (0 <= osr_bci && osr_bci < method->code_size()), "bci out of range");
  assert(!method->is_abstract() && (osr_bci == InvocationEntryBci || !method->is_native()), "cannot compile abstract/native methods");
  assert(!method->method_holder()->is_not_initialized(), "method holder must be initialized");
  // return quickly if possible

  // lock, make sure that the compilation
  // isn't prohibited in a straightforward way.
  AbstractCompiler* comp = CompileBroker::compiler(comp_level);
  if (comp == NULL || compilation_is_prohibited(method, osr_bci, comp_level, directive->ExcludeOption)) {
    return NULL;
  }

#if INCLUDE_JVMCI
  if (comp->is_jvmci() && !JVMCI::can_initialize_JVMCI()) {
    return NULL;
  }
#endif

  if (osr_bci == InvocationEntryBci) {
    // standard compilation
    CompiledMethod* method_code = method->code();
    if (method_code != NULL && method_code->is_nmethod()) {
      if (compilation_is_complete(method, osr_bci, comp_level)) {
        return (nmethod*) method_code;
      }
    }
    if (method->is_not_compilable(comp_level)) {
      return NULL;
    }
  } else {
    // osr compilation
    // We accept a higher level osr method
    nmethod* nm = method->lookup_osr_nmethod_for(osr_bci, comp_level, false);
    if (nm != NULL) return nm;
    if (method->is_not_osr_compilable(comp_level)) return NULL;
  }

  assert(!HAS_PENDING_EXCEPTION, "No exception should be present");
  // some prerequisites that are compiler specific
  if (comp->is_c2()) {
    method->constants()->resolve_string_constants(CHECK_AND_CLEAR_NONASYNC_NULL);
    // Resolve all classes seen in the signature of the method
    // we are compiling.
    Method::load_signature_classes(method, CHECK_AND_CLEAR_NONASYNC_NULL);
  }

  // If the method is native, do the lookup in the thread requesting
  // the compilation. Native lookups can load code, which is not
  // permitted during compilation.
  //
  // Note: A native method implies non-osr compilation which is
  //       checked with an assertion at the entry of this method.
  if (method->is_native() && !method->is_method_handle_intrinsic()) {
    address adr = NativeLookup::lookup(method, THREAD);
    if (HAS_PENDING_EXCEPTION) {
      // In case of an exception looking up the method, we just forget
      // about it. The interpreter will kick-in and throw the exception.
      method->set_not_compilable("NativeLookup::lookup failed"); // implies is_not_osr_compilable()
      CLEAR_PENDING_EXCEPTION;
      return NULL;
    }
    assert(method->has_native_function(), "must have native code by now");
  }

  // RedefineClasses() has replaced this method; just return
  if (method->is_old()) {
    return NULL;
  }

  // JVMTI -- post_compile_event requires jmethod_id() that may require
  // a lock the compiling thread can not acquire. Prefetch it here.
  if (JvmtiExport::should_post_compiled_method_load()) {
    method->jmethod_id();
  }

  // do the compilation
  if (method->is_native()) {
    if (!PreferInterpreterNativeStubs || method->is_method_handle_intrinsic()) {
#if defined(X86) && !defined(ZERO)
      // The following native methods:
      //
      // java.lang.Float.intBitsToFloat
      // java.lang.Float.floatToRawIntBits
      // java.lang.Double.longBitsToDouble
      // java.lang.Double.doubleToRawLongBits
      //
      // are called through the interpreter even if interpreter native stubs
      // are not preferred (i.e., calling through adapter handlers is preferred).
      // The reason is that on x86_32 signaling NaNs (sNaNs) are not preserved
      // if the version of the methods from the native libraries is called.
      // As the interpreter and the C2-intrinsified version of the methods preserves
      // sNaNs, that would result in an inconsistent way of handling of sNaNs.
      if ((UseSSE >= 1 &&
          (method->intrinsic_id() == vmIntrinsics::_intBitsToFloat ||
           method->intrinsic_id() == vmIntrinsics::_floatToRawIntBits)) ||
          (UseSSE >= 2 &&
           (method->intrinsic_id() == vmIntrinsics::_longBitsToDouble ||
            method->intrinsic_id() == vmIntrinsics::_doubleToRawLongBits))) {
        return NULL;
      }
#endif // X86 && !ZERO

      // To properly handle the appendix argument for out-of-line calls we are using a small trampoline that
      // pops off the appendix argument and jumps to the target (see gen_special_dispatch in SharedRuntime).
      //
      // Since normal compiled-to-compiled calls are not able to handle such a thing we MUST generate an adapter
      // in this case.  If we can't generate one and use it we can not execute the out-of-line method handle calls.
      AdapterHandlerLibrary::create_native_wrapper(method);
    } else {
      return NULL;
    }
  } else {
    // If the compiler is shut off due to code cache getting full
    // fail out now so blocking compiles dont hang the java thread
    if (!should_compile_new_jobs()) {
      return NULL;
    }
    bool is_blocking = !directive->BackgroundCompilationOption || ReplayCompiles;
    compile_method_base(method, osr_bci, comp_level, hot_method, hot_count, compile_reason, is_blocking, THREAD);
  }

  // return requested nmethod
  // We accept a higher level osr method
  if (osr_bci == InvocationEntryBci) {
    CompiledMethod* code = method->code();
    if (code == NULL) {
      return (nmethod*) code;
    } else {
      return code->as_nmethod_or_null();
    }
  }
  return method->lookup_osr_nmethod_for(osr_bci, comp_level, false);
}


// ------------------------------------------------------------------
// CompileBroker::compilation_is_complete
//
// See if compilation of this method is already complete.
bool CompileBroker::compilation_is_complete(const methodHandle& method,
                                            int                 osr_bci,
                                            int                 comp_level) {
  bool is_osr = (osr_bci != standard_entry_bci);
  if (is_osr) {
    if (method->is_not_osr_compilable(comp_level)) {
      return true;
    } else {
      nmethod* result = method->lookup_osr_nmethod_for(osr_bci, comp_level, true);
      return (result != NULL);
    }
  } else {
    if (method->is_not_compilable(comp_level)) {
      return true;
    } else {
      CompiledMethod* result = method->code();
      if (result == NULL) return false;
      return comp_level == result->comp_level();
    }
  }
}


/**
 * See if this compilation is already requested.
 *
 * Implementation note: there is only a single "is in queue" bit
 * for each method.  This means that the check below is overly
 * conservative in the sense that an osr compilation in the queue
 * will block a normal compilation from entering the queue (and vice
 * versa).  This can be remedied by a full queue search to disambiguate
 * cases.  If it is deemed profitable, this may be done.
 */
bool CompileBroker::compilation_is_in_queue(const methodHandle& method) {
  return method->queued_for_compilation();
}

// ------------------------------------------------------------------
// CompileBroker::compilation_is_prohibited
//
// See if this compilation is not allowed.
bool CompileBroker::compilation_is_prohibited(const methodHandle& method, int osr_bci, int comp_level, bool excluded) {
  bool is_native = method->is_native();
  // Some compilers may not support the compilation of natives.
  AbstractCompiler *comp = compiler(comp_level);
  if (is_native && (!CICompileNatives || comp == NULL)) {
    method->set_not_compilable_quietly("native methods not supported", comp_level);
    return true;
  }

  bool is_osr = (osr_bci != standard_entry_bci);
  // Some compilers may not support on stack replacement.
  if (is_osr && (!CICompileOSR || comp == NULL)) {
    method->set_not_osr_compilable("OSR not supported", comp_level);
    return true;
  }

  // The method may be explicitly excluded by the user.
  double scale;
  if (excluded || (CompilerOracle::has_option_value(method, CompileCommand::CompileThresholdScaling, scale) && scale == 0)) {
    bool quietly = CompilerOracle::be_quiet();
    if (PrintCompilation && !quietly) {
      // This does not happen quietly...
      ResourceMark rm;
      tty->print("### Excluding %s:%s",
                 method->is_native() ? "generation of native wrapper" : "compile",
                 (method->is_static() ? " static" : ""));
      method->print_short_name(tty);
      tty->cr();
    }
    method->set_not_compilable("excluded by CompileCommand", comp_level, !quietly);
  }

  return false;
}

/**
 * Generate serialized IDs for compilation requests. If certain debugging flags are used
 * and the ID is not within the specified range, the method is not compiled and 0 is returned.
 * The function also allows to generate separate compilation IDs for OSR compilations.
 */
int CompileBroker::assign_compile_id(const methodHandle& method, int osr_bci) {
#ifdef ASSERT
  bool is_osr = (osr_bci != standard_entry_bci);
  int id;
  if (method->is_native()) {
    assert(!is_osr, "can't be osr");
    // Adapters, native wrappers and method handle intrinsics
    // should be generated always.
    return Atomic::add(CICountNative ? &_native_compilation_id : &_compilation_id, 1);
  } else if (CICountOSR && is_osr) {
    id = Atomic::add(&_osr_compilation_id, 1);
    if (CIStartOSR <= id && id < CIStopOSR) {
      return id;
    }
  } else {
    id = Atomic::add(&_compilation_id, 1);
    if (CIStart <= id && id < CIStop) {
      return id;
    }
  }

  // Method was not in the appropriate compilation range.
  method->set_not_compilable_quietly("Not in requested compile id range");
  return 0;
#else
  // CICountOSR is a develop flag and set to 'false' by default. In a product built,
  // only _compilation_id is incremented.
  return Atomic::add(&_compilation_id, 1);
#endif
}

// ------------------------------------------------------------------
// CompileBroker::assign_compile_id_unlocked
//
// Public wrapper for assign_compile_id that acquires the needed locks
uint CompileBroker::assign_compile_id_unlocked(Thread* thread, const methodHandle& method, int osr_bci) {
  MutexLocker locker(thread, MethodCompileQueue_lock);
  return assign_compile_id(method, osr_bci);
}

// ------------------------------------------------------------------
// CompileBroker::create_compile_task
//
// Create a CompileTask object representing the current request for
// compilation.  Add this task to the queue.
CompileTask* CompileBroker::create_compile_task(CompileQueue*       queue,
                                                int                 compile_id,
                                                const methodHandle& method,
                                                int                 osr_bci,
                                                int                 comp_level,
                                                const methodHandle& hot_method,
                                                int                 hot_count,
                                                CompileTask::CompileReason compile_reason,
                                                bool                blocking) {
  CompileTask* new_task = CompileTask::allocate();
  new_task->initialize(compile_id, method, osr_bci, comp_level,
                       hot_method, hot_count, compile_reason,
                       blocking);
  queue->add(new_task);
  return new_task;
}

#if INCLUDE_JVMCI
// The number of milliseconds to wait before checking if
// JVMCI compilation has made progress.
static const long JVMCI_COMPILATION_PROGRESS_WAIT_TIMESLICE = 1000;

// The number of JVMCI compilation progress checks that must fail
// before unblocking a thread waiting for a blocking compilation.
static const int JVMCI_COMPILATION_PROGRESS_WAIT_ATTEMPTS = 10;

/**
 * Waits for a JVMCI compiler to complete a given task. This thread
 * waits until either the task completes or it sees no JVMCI compilation
 * progress for N consecutive milliseconds where N is
 * JVMCI_COMPILATION_PROGRESS_WAIT_TIMESLICE *
 * JVMCI_COMPILATION_PROGRESS_WAIT_ATTEMPTS.
 *
 * @return true if this thread needs to free/recycle the task
 */
bool CompileBroker::wait_for_jvmci_completion(JVMCICompiler* jvmci, CompileTask* task, JavaThread* thread) {
  assert(UseJVMCICompiler, "sanity");
  MonitorLocker ml(thread, task->lock());
  int progress_wait_attempts = 0;
  jint thread_jvmci_compilation_ticks = 0;
  jint global_jvmci_compilation_ticks = jvmci->global_compilation_ticks();
  while (!task->is_complete() && !is_compilation_disabled_forever() &&
         ml.wait(JVMCI_COMPILATION_PROGRESS_WAIT_TIMESLICE)) {
    JVMCICompileState* jvmci_compile_state = task->blocking_jvmci_compile_state();

    bool progress;
    if (jvmci_compile_state != NULL) {
      jint ticks = jvmci_compile_state->compilation_ticks();
      progress = (ticks - thread_jvmci_compilation_ticks) != 0;
      JVMCI_event_1("waiting on compilation %d [ticks=%d]", task->compile_id(), ticks);
      thread_jvmci_compilation_ticks = ticks;
    } else {
      // Still waiting on JVMCI compiler queue. This thread may be holding a lock
      // that all JVMCI compiler threads are blocked on. We use the global JVMCI
      // compilation ticks to determine whether JVMCI compilation
      // is still making progress through the JVMCI compiler queue.
      jint ticks = jvmci->global_compilation_ticks();
      progress = (ticks - global_jvmci_compilation_ticks) != 0;
      JVMCI_event_1("waiting on compilation %d to be queued [ticks=%d]", task->compile_id(), ticks);
      global_jvmci_compilation_ticks = ticks;
    }

    if (!progress) {
      if (++progress_wait_attempts == JVMCI_COMPILATION_PROGRESS_WAIT_ATTEMPTS) {
        if (PrintCompilation) {
          task->print(tty, "wait for blocking compilation timed out");
        }
        JVMCI_event_1("waiting on compilation %d timed out", task->compile_id());
        break;
      }
    } else {
      progress_wait_attempts = 0;
    }
  }
  task->clear_waiter();
  return task->is_complete();
}
#endif

/**
 *  Wait for the compilation task to complete.
 */
void CompileBroker::wait_for_completion(CompileTask* task) {
  if (CIPrintCompileQueue) {
    ttyLocker ttyl;
    tty->print_cr("BLOCKING FOR COMPILE");
  }

  assert(task->is_blocking(), "can only wait on blocking task");

  JavaThread* thread = JavaThread::current();

  methodHandle method(thread, task->method());
  bool free_task;
#if INCLUDE_JVMCI
  AbstractCompiler* comp = compiler(task->comp_level());
  if (comp->is_jvmci() && !task->should_wait_for_compilation()) {
    // It may return before compilation is completed.
    free_task = wait_for_jvmci_completion((JVMCICompiler*) comp, task, thread);
  } else
#endif
  {
    MonitorLocker ml(thread, task->lock());
    free_task = true;
    while (!task->is_complete() && !is_compilation_disabled_forever()) {
      ml.wait();
    }
  }

  if (free_task) {
    if (is_compilation_disabled_forever()) {
      CompileTask::free(task);
      return;
    }

    // It is harmless to check this status without the lock, because
    // completion is a stable property (until the task object is recycled).
    assert(task->is_complete(), "Compilation should have completed");
    assert(task->code_handle() == NULL, "must be reset");

    // By convention, the waiter is responsible for recycling a
    // blocking CompileTask. Since there is only one waiter ever
    // waiting on a CompileTask, we know that no one else will
    // be using this CompileTask; we can free it.
    CompileTask::free(task);
  }
}

/**
 * Initialize compiler thread(s) + compiler object(s). The postcondition
 * of this function is that the compiler runtimes are initialized and that
 * compiler threads can start compiling.
 */
bool CompileBroker::init_compiler_runtime() {
  CompilerThread* thread = CompilerThread::current();
  AbstractCompiler* comp = thread->compiler();
  // Final sanity check - the compiler object must exist
  guarantee(comp != NULL, "Compiler object must exist");

  {
    // Must switch to native to allocate ci_env
    ThreadToNativeFromVM ttn(thread);
    ciEnv ci_env((CompileTask*)NULL);
    // Cache Jvmti state
    ci_env.cache_jvmti_state();
    // Cache DTrace flags
    ci_env.cache_dtrace_flags();

    // Switch back to VM state to do compiler initialization
    ThreadInVMfromNative tv(thread);

    // Perform per-thread and global initializations
    comp->initialize();
  }

  if (comp->is_failed()) {
    disable_compilation_forever();
    // If compiler initialization failed, no compiler thread that is specific to a
    // particular compiler runtime will ever start to compile methods.
    shutdown_compiler_runtime(comp, thread);
    return false;
  }

  // C1 specific check
  if (comp->is_c1() && (thread->get_buffer_blob() == NULL)) {
    warning("Initialization of %s thread failed (no space to run compilers)", thread->name());
    return false;
  }

  return true;
}

/**
 * If C1 and/or C2 initialization failed, we shut down all compilation.
 * We do this to keep things simple. This can be changed if it ever turns
 * out to be a problem.
 */
void CompileBroker::shutdown_compiler_runtime(AbstractCompiler* comp, CompilerThread* thread) {
  // Free buffer blob, if allocated
  if (thread->get_buffer_blob() != NULL) {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::free(thread->get_buffer_blob());
  }

  if (comp->should_perform_shutdown()) {
    // There are two reasons for shutting down the compiler
    // 1) compiler runtime initialization failed
    // 2) The code cache is full and the following flag is set: -XX:-UseCodeCacheFlushing
    warning("%s initialization failed. Shutting down all compilers", comp->name());

    // Only one thread per compiler runtime object enters here
    // Set state to shut down
    comp->set_shut_down();

    // Delete all queued compilation tasks to make compiler threads exit faster.
    if (_c1_compile_queue != NULL) {
      _c1_compile_queue->free_all();
    }

    if (_c2_compile_queue != NULL) {
      _c2_compile_queue->free_all();
    }

    // Set flags so that we continue execution with using interpreter only.
    UseCompiler    = false;
    UseInterpreter = true;

    // We could delete compiler runtimes also. However, there are references to
    // the compiler runtime(s) (e.g.,  nmethod::is_compiled_by_c1()) which then
    // fail. This can be done later if necessary.
  }
}

/**
 * Helper function to create new or reuse old CompileLog.
 */
CompileLog* CompileBroker::get_log(CompilerThread* ct) {
  if (!LogCompilation) return NULL;

  AbstractCompiler *compiler = ct->compiler();
  bool c1 = compiler->is_c1();
  jobject* compiler_objects = c1 ? _compiler1_objects : _compiler2_objects;
  assert(compiler_objects != NULL, "must be initialized at this point");
  CompileLog** logs = c1 ? _compiler1_logs : _compiler2_logs;
  assert(logs != NULL, "must be initialized at this point");
  int count = c1 ? _c1_count : _c2_count;

  // Find Compiler number by its threadObj.
  oop compiler_obj = ct->threadObj();
  int compiler_number = 0;
  bool found = false;
  for (; compiler_number < count; compiler_number++) {
    if (JNIHandles::resolve_non_null(compiler_objects[compiler_number]) == compiler_obj) {
      found = true;
      break;
    }
  }
  assert(found, "Compiler must exist at this point");

  // Determine pointer for this thread's log.
  CompileLog** log_ptr = &logs[compiler_number];

  // Return old one if it exists.
  CompileLog* log = *log_ptr;
  if (log != NULL) {
    ct->init_log(log);
    return log;
  }

  // Create a new one and remember it.
  init_compiler_thread_log();
  log = ct->log();
  *log_ptr = log;
  return log;
}

// ------------------------------------------------------------------
// CompileBroker::compiler_thread_loop
//
// The main loop run by a CompilerThread.
void CompileBroker::compiler_thread_loop() {
  CompilerThread* thread = CompilerThread::current();
  CompileQueue* queue = thread->queue();
  // For the thread that initializes the ciObjectFactory
  // this resource mark holds all the shared objects
  ResourceMark rm;

  // First thread to get here will initialize the compiler interface

  {
    ASSERT_IN_VM;
    MutexLocker only_one (thread, CompileThread_lock);
    if (!ciObjectFactory::is_initialized()) {
      ciObjectFactory::initialize();
    }
  }

  // Open a log.
  CompileLog* log = get_log(thread);
  if (log != NULL) {
    log->begin_elem("start_compile_thread name='%s' thread='" UINTX_FORMAT "' process='%d'",
                    thread->name(),
                    os::current_thread_id(),
                    os::current_process_id());
    log->stamp();
    log->end_elem();
  }

  // If compiler thread/runtime initialization fails, exit the compiler thread
  if (!init_compiler_runtime()) {
    return;
  }

  thread->start_idle_timer();

  // Poll for new compilation tasks as long as the JVM runs. Compilation
  // should only be disabled if something went wrong while initializing the
  // compiler runtimes. This, in turn, should not happen. The only known case
  // when compiler runtime initialization fails is if there is not enough free
  // space in the code cache to generate the necessary stubs, etc.
  while (!is_compilation_disabled_forever()) {
    // We need this HandleMark to avoid leaking VM handles.
    HandleMark hm(thread);

    CompileTask* task = queue->get();
    if (task == NULL) {
      if (UseDynamicNumberOfCompilerThreads) {
        // Access compiler_count under lock to enforce consistency.
        MutexLocker only_one(CompileThread_lock);
        if (can_remove(thread, true)) {
          if (TraceCompilerThreads) {
            tty->print_cr("Removing compiler thread %s after " JLONG_FORMAT " ms idle time",
                          thread->name(), thread->idle_time_millis());
          }
          // Free buffer blob, if allocated
          if (thread->get_buffer_blob() != NULL) {
            MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
            CodeCache::free(thread->get_buffer_blob());
          }
          return; // Stop this thread.
        }
      }
    } else {
      // Assign the task to the current thread.  Mark this compilation
      // thread as active for the profiler.
      // CompileTaskWrapper also keeps the Method* from being deallocated if redefinition
      // occurs after fetching the compile task off the queue.
      CompileTaskWrapper ctw(task);
      nmethodLocker result_handle;  // (handle for the nmethod produced by this task)
      task->set_code_handle(&result_handle);
      methodHandle method(thread, task->method());

      // Never compile a method if breakpoints are present in it
      if (method()->number_of_breakpoints() == 0) {
        // Compile the method.
        if ((UseCompiler || AlwaysCompileLoopMethods) && CompileBroker::should_compile_new_jobs()) {
          invoke_compiler_on_method(task);
          thread->start_idle_timer();
        } else {
          // After compilation is disabled, remove remaining methods from queue
          method->clear_queued_for_compilation();
          task->set_failure_reason("compilation is disabled");
        }
      }

      if (UseDynamicNumberOfCompilerThreads) {
        possibly_add_compiler_threads(thread);
        assert(!thread->has_pending_exception(), "should have been handled");
      }
    }
  }

  // Shut down compiler runtime
  shutdown_compiler_runtime(thread->compiler(), thread);
}

// ------------------------------------------------------------------
// CompileBroker::init_compiler_thread_log
//
// Set up state required by +LogCompilation.
void CompileBroker::init_compiler_thread_log() {
    CompilerThread* thread = CompilerThread::current();
    char  file_name[4*K];
    FILE* fp = NULL;
    intx thread_id = os::current_thread_id();
    for (int try_temp_dir = 1; try_temp_dir >= 0; try_temp_dir--) {
      const char* dir = (try_temp_dir ? os::get_temp_directory() : NULL);
      if (dir == NULL) {
        jio_snprintf(file_name, sizeof(file_name), "hs_c" UINTX_FORMAT "_pid%u.log",
                     thread_id, os::current_process_id());
      } else {
        jio_snprintf(file_name, sizeof(file_name),
                     "%s%shs_c" UINTX_FORMAT "_pid%u.log", dir,
                     os::file_separator(), thread_id, os::current_process_id());
      }

      fp = fopen(file_name, "wt");
      if (fp != NULL) {
        if (LogCompilation && Verbose) {
          tty->print_cr("Opening compilation log %s", file_name);
        }
        CompileLog* log = new(ResourceObj::C_HEAP, mtCompiler) CompileLog(file_name, fp, thread_id);
        if (log == NULL) {
          fclose(fp);
          return;
        }
        thread->init_log(log);

        if (xtty != NULL) {
          ttyLocker ttyl;
          // Record any per thread log files
          xtty->elem("thread_logfile thread='" INTX_FORMAT "' filename='%s'", thread_id, file_name);
        }
        return;
      }
    }
    warning("Cannot open log file: %s", file_name);
}

void CompileBroker::log_metaspace_failure() {
  const char* message = "some methods may not be compiled because metaspace "
                        "is out of memory";
  if (_compilation_log != NULL) {
    _compilation_log->log_metaspace_failure(message);
  }
  if (PrintCompilation) {
    tty->print_cr("COMPILE PROFILING SKIPPED: %s", message);
  }
}


// ------------------------------------------------------------------
// CompileBroker::set_should_block
//
// Set _should_block.
// Call this from the VM, with Threads_lock held and a safepoint requested.
void CompileBroker::set_should_block() {
  assert(Threads_lock->owner() == Thread::current(), "must have threads lock");
  assert(SafepointSynchronize::is_at_safepoint(), "must be at a safepoint already");
#ifndef PRODUCT
  if (PrintCompilation && (Verbose || WizardMode))
    tty->print_cr("notifying compiler thread pool to block");
#endif
  _should_block = true;
}

// ------------------------------------------------------------------
// CompileBroker::maybe_block
//
// Call this from the compiler at convenient points, to poll for _should_block.
void CompileBroker::maybe_block() {
  if (_should_block) {
#ifndef PRODUCT
    if (PrintCompilation && (Verbose || WizardMode))
      tty->print_cr("compiler thread " INTPTR_FORMAT " poll detects block request", p2i(Thread::current()));
#endif
    ThreadInVMfromNative tivfn(JavaThread::current());
  }
}

// wrapper for CodeCache::print_summary()
static void codecache_print(bool detailed)
{
  ResourceMark rm;
  stringStream s;
  // Dump code cache  into a buffer before locking the tty,
  {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::print_summary(&s, detailed);
  }
  ttyLocker ttyl;
  tty->print("%s", s.as_string());
}

// wrapper for CodeCache::print_summary() using outputStream
static void codecache_print(outputStream* out, bool detailed) {
  ResourceMark rm;
  stringStream s;

  // Dump code cache into a buffer
  {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::print_summary(&s, detailed);
  }

  char* remaining_log = s.as_string();
  while (*remaining_log != '\0') {
    char* eol = strchr(remaining_log, '\n');
    if (eol == NULL) {
      out->print_cr("%s", remaining_log);
      remaining_log = remaining_log + strlen(remaining_log);
    } else {
      *eol = '\0';
      out->print_cr("%s", remaining_log);
      remaining_log = eol + 1;
    }
  }
}

void CompileBroker::post_compile(CompilerThread* thread, CompileTask* task, bool success, ciEnv* ci_env,
                                 int compilable, const char* failure_reason) {
  if (success) {
    task->mark_success();
    if (ci_env != NULL) {
      task->set_num_inlined_bytecodes(ci_env->num_inlined_bytecodes());
    }
    if (_compilation_log != NULL) {
      nmethod* code = task->code();
      if (code != NULL) {
        _compilation_log->log_nmethod(thread, code);
      }
    }
  } else if (AbortVMOnCompilationFailure) {
    if (compilable == ciEnv::MethodCompilable_not_at_tier) {
      fatal("Not compilable at tier %d: %s", task->comp_level(), failure_reason);
    }
    if (compilable == ciEnv::MethodCompilable_never) {
      fatal("Never compilable: %s", failure_reason);
    }
  }
}

static void post_compilation_event(EventCompilation& event, CompileTask* task) {
  assert(task != NULL, "invariant");
  CompilerEvent::CompilationEvent::post(event,
                                        task->compile_id(),
                                        task->compiler()->type(),
                                        task->method(),
                                        task->comp_level(),
                                        task->is_success(),
                                        task->osr_bci() != CompileBroker::standard_entry_bci,
                                        (task->code() == NULL) ? 0 : task->code()->total_size(),
                                        task->num_inlined_bytecodes());
}

int DirectivesStack::_depth = 0;
CompilerDirectives* DirectivesStack::_top = NULL;
CompilerDirectives* DirectivesStack::_bottom = NULL;

// ------------------------------------------------------------------
// CompileBroker::invoke_compiler_on_method
//
// Compile a method.
//
void CompileBroker::invoke_compiler_on_method(CompileTask* task) {
  task->print_ul();
  if (PrintCompilation) {
    ResourceMark rm;
    task->print_tty();
  }
  elapsedTimer time;

  CompilerThread* thread = CompilerThread::current();
  ResourceMark rm(thread);

  if (LogEvents) {
    _compilation_log->log_compile(thread, task);
  }

  // Common flags.
  uint compile_id = task->compile_id();
  int osr_bci = task->osr_bci();
  bool is_osr = (osr_bci != standard_entry_bci);
  bool should_log = (thread->log() != NULL);
  bool should_break = false;
  const int task_level = task->comp_level();
  AbstractCompiler* comp = task->compiler();

  DirectiveSet* directive;
  {
    // create the handle inside it's own block so it can't
    // accidentally be referenced once the thread transitions to
    // native.  The NoHandleMark before the transition should catch
    // any cases where this occurs in the future.
    methodHandle method(thread, task->method());
    assert(!method->is_native(), "no longer compile natives");

    // Look up matching directives
    directive = DirectivesStack::getMatchingDirective(method, comp);

    // Update compile information when using perfdata.
    if (UsePerfData) {
      update_compile_perf_data(thread, method, is_osr);
    }

    DTRACE_METHOD_COMPILE_BEGIN_PROBE(method, compiler_name(task_level));
  }

  should_break = directive->BreakAtCompileOption || task->check_break_at_flags();
  if (should_log && !directive->LogOption) {
    should_log = false;
  }

  // Allocate a new set of JNI handles.
  push_jni_handle_block();
  Method* target_handle = task->method();
  int compilable = ciEnv::MethodCompilable;
  const char* failure_reason = NULL;
  bool failure_reason_on_C_heap = false;
  const char* retry_message = NULL;

#if INCLUDE_JVMCI
  if (UseJVMCICompiler && comp != NULL && comp->is_jvmci()) {
    JVMCICompiler* jvmci = (JVMCICompiler*) comp;

    TraceTime t1("compilation", &time);
    EventCompilation event;
    JVMCICompileState compile_state(task, jvmci);
    JVMCIRuntime *runtime = NULL;

    if (JVMCI::in_shutdown()) {
      failure_reason = "in JVMCI shutdown";
      retry_message = "not retryable";
      compilable = ciEnv::MethodCompilable_never;
    } else if (compile_state.target_method_is_old()) {
      // Skip redefined methods
      failure_reason = "redefined method";
      retry_message = "not retryable";
      compilable = ciEnv::MethodCompilable_never;
    } else {
      JVMCIEnv env(thread, &compile_state, __FILE__, __LINE__);
      methodHandle method(thread, target_handle);
      runtime = env.runtime();
      runtime->compile_method(&env, jvmci, method, osr_bci);

      failure_reason = compile_state.failure_reason();
      failure_reason_on_C_heap = compile_state.failure_reason_on_C_heap();
      if (!compile_state.retryable()) {
        retry_message = "not retryable";
        compilable = ciEnv::MethodCompilable_not_at_tier;
      }
      if (task->code() == NULL) {
        assert(failure_reason != NULL, "must specify failure_reason");
      }
    }
    post_compile(thread, task, task->code() != NULL, NULL, compilable, failure_reason);
    if (event.should_commit()) {
      post_compilation_event(event, task);
    }

  } else
#endif // INCLUDE_JVMCI
  {
    NoHandleMark  nhm;
    ThreadToNativeFromVM ttn(thread);

    ciEnv ci_env(task);
    if (should_break) {
      ci_env.set_break_at_compile(true);
    }
    if (should_log) {
      ci_env.set_log(thread->log());
    }
    assert(thread->env() == &ci_env, "set by ci_env");
    // The thread-env() field is cleared in ~CompileTaskWrapper.

    // Cache Jvmti state
    bool method_is_old = ci_env.cache_jvmti_state();

    // Skip redefined methods
    if (method_is_old) {
      ci_env.record_method_not_compilable("redefined method", true);
    }

    // Cache DTrace flags
    ci_env.cache_dtrace_flags();

    ciMethod* target = ci_env.get_method_from_handle(target_handle);

    TraceTime t1("compilation", &time);
    EventCompilation event;

    if (comp == NULL) {
      ci_env.record_method_not_compilable("no compiler");
    } else if (!ci_env.failing()) {
      if (WhiteBoxAPI && WhiteBox::compilation_locked) {
        MonitorLocker locker(Compilation_lock, Mutex::_no_safepoint_check_flag);
        while (WhiteBox::compilation_locked) {
          locker.wait();
        }
      }
      comp->compile_method(&ci_env, target, osr_bci, true, directive);

      /* Repeat compilation without installing code for profiling purposes */
      int repeat_compilation_count = directive->RepeatCompilationOption;
      while (repeat_compilation_count > 0) {
        task->print_ul("NO CODE INSTALLED");
        comp->compile_method(&ci_env, target, osr_bci, false , directive);
        repeat_compilation_count--;
      }
    }

    if (!ci_env.failing() && task->code() == NULL) {
      //assert(false, "compiler should always document failure");
      // The compiler elected, without comment, not to register a result.
      // Do not attempt further compilations of this method.
      ci_env.record_method_not_compilable("compile failed");
    }

    // Copy this bit to the enclosing block:
    compilable = ci_env.compilable();

    if (ci_env.failing()) {
      failure_reason = ci_env.failure_reason();
      retry_message = ci_env.retry_message();
      ci_env.report_failure(failure_reason);
    }

    post_compile(thread, task, !ci_env.failing(), &ci_env, compilable, failure_reason);
    if (event.should_commit()) {
      post_compilation_event(event, task);
    }
  }
  // Remove the JNI handle block after the ciEnv destructor has run in
  // the previous block.
  pop_jni_handle_block();

  if (failure_reason != NULL) {
    task->set_failure_reason(failure_reason, failure_reason_on_C_heap);
    if (_compilation_log != NULL) {
      _compilation_log->log_failure(thread, task, failure_reason, retry_message);
    }
    if (PrintCompilation) {
      FormatBufferResource msg = retry_message != NULL ?
        FormatBufferResource("COMPILE SKIPPED: %s (%s)", failure_reason, retry_message) :
        FormatBufferResource("COMPILE SKIPPED: %s",      failure_reason);
      task->print(tty, msg);
    }
  }

  methodHandle method(thread, task->method());

  DTRACE_METHOD_COMPILE_END_PROBE(method, compiler_name(task_level), task->is_success());

  collect_statistics(thread, time, task);

  nmethod* nm = task->code();
  if (nm != NULL) {
    nm->maybe_print_nmethod(directive);
  }
  DirectivesStack::release(directive);

  if (PrintCompilation && PrintCompilation2) {
    tty->print("%7d ", (int) tty->time_stamp().milliseconds());  // print timestamp
    tty->print("%4d ", compile_id);    // print compilation number
    tty->print("%s ", (is_osr ? "%" : " "));
    if (task->code() != NULL) {
      tty->print("size: %d(%d) ", task->code()->total_size(), task->code()->insts_size());
    }
    tty->print_cr("time: %d inlined: %d bytes", (int)time.milliseconds(), task->num_inlined_bytecodes());
  }

  Log(compilation, codecache) log;
  if (log.is_debug()) {
    LogStream ls(log.debug());
    codecache_print(&ls, /* detailed= */ false);
  }
  if (PrintCodeCacheOnCompilation) {
    codecache_print(/* detailed= */ false);
  }
  // Disable compilation, if required.
  switch (compilable) {
  case ciEnv::MethodCompilable_never:
    if (is_osr)
      method->set_not_osr_compilable_quietly("MethodCompilable_never");
    else
      method->set_not_compilable_quietly("MethodCompilable_never");
    break;
  case ciEnv::MethodCompilable_not_at_tier:
    if (is_osr)
      method->set_not_osr_compilable_quietly("MethodCompilable_not_at_tier", task_level);
    else
      method->set_not_compilable_quietly("MethodCompilable_not_at_tier", task_level);
    break;
  }

  // Note that the queued_for_compilation bits are cleared without
  // protection of a mutex. [They were set by the requester thread,
  // when adding the task to the compile queue -- at which time the
  // compile queue lock was held. Subsequently, we acquired the compile
  // queue lock to get this task off the compile queue; thus (to belabour
  // the point somewhat) our clearing of the bits must be occurring
  // only after the setting of the bits. See also 14012000 above.
  method->clear_queued_for_compilation();
}

/**
 * The CodeCache is full. Print warning and disable compilation.
 * Schedule code cache cleaning so compilation can continue later.
 * This function needs to be called only from CodeCache::allocate(),
 * since we currently handle a full code cache uniformly.
 */
void CompileBroker::handle_full_code_cache(int code_blob_type) {
  UseInterpreter = true;
  if (UseCompiler || AlwaysCompileLoopMethods ) {
    if (xtty != NULL) {
      ResourceMark rm;
      stringStream s;
      // Dump code cache state into a buffer before locking the tty,
      // because log_state() will use locks causing lock conflicts.
      CodeCache::log_state(&s);
      // Lock to prevent tearing
      ttyLocker ttyl;
      xtty->begin_elem("code_cache_full");
      xtty->print("%s", s.as_string());
      xtty->stamp();
      xtty->end_elem();
    }

#ifndef PRODUCT
    if (ExitOnFullCodeCache) {
      codecache_print(/* detailed= */ true);
      before_exit(JavaThread::current());
      exit_globals(); // will delete tty
      vm_direct_exit(1);
    }
#endif
    if (UseCodeCacheFlushing) {
      // Since code cache is full, immediately stop new compiles
      if (CompileBroker::set_should_compile_new_jobs(CompileBroker::stop_compilation)) {
        NMethodSweeper::log_sweep("disable_compiler");
      }
    } else {
      disable_compilation_forever();
    }

    CodeCache::report_codemem_full(code_blob_type, should_print_compiler_warning());
  }
}

// ------------------------------------------------------------------
// CompileBroker::update_compile_perf_data
//
// Record this compilation for debugging purposes.
void CompileBroker::update_compile_perf_data(CompilerThread* thread, const methodHandle& method, bool is_osr) {
  ResourceMark rm;
  char* method_name = method->name()->as_C_string();
  char current_method[CompilerCounters::cmname_buffer_length];
  size_t maxLen = CompilerCounters::cmname_buffer_length;

  const char* class_name = method->method_holder()->name()->as_C_string();

  size_t s1len = strlen(class_name);
  size_t s2len = strlen(method_name);

  // check if we need to truncate the string
  if (s1len + s2len + 2 > maxLen) {

    // the strategy is to lop off the leading characters of the
    // class name and the trailing characters of the method name.

    if (s2len + 2 > maxLen) {
      // lop of the entire class name string, let snprintf handle
      // truncation of the method name.
      class_name += s1len; // null string
    }
    else {
      // lop off the extra characters from the front of the class name
      class_name += ((s1len + s2len + 2) - maxLen);
    }
  }

  jio_snprintf(current_method, maxLen, "%s %s", class_name, method_name);

  int last_compile_type = normal_compile;
  if (CICountOSR && is_osr) {
    last_compile_type = osr_compile;
  } else if (CICountNative && method->is_native()) {
    last_compile_type = native_compile;
  }

  CompilerCounters* counters = thread->counters();
  counters->set_current_method(current_method);
  counters->set_compile_type((jlong) last_compile_type);
}

// ------------------------------------------------------------------
// CompileBroker::push_jni_handle_block
//
// Push on a new block of JNI handles.
void CompileBroker::push_jni_handle_block() {
  JavaThread* thread = JavaThread::current();

  // Allocate a new block for JNI handles.
  // Inlined code from jni_PushLocalFrame()
  JNIHandleBlock* java_handles = thread->active_handles();
  JNIHandleBlock* compile_handles = JNIHandleBlock::allocate_block(thread);
  assert(compile_handles != NULL && java_handles != NULL, "should not be NULL");
  compile_handles->set_pop_frame_link(java_handles);  // make sure java handles get gc'd.
  thread->set_active_handles(compile_handles);
}


// ------------------------------------------------------------------
// CompileBroker::pop_jni_handle_block
//
// Pop off the current block of JNI handles.
void CompileBroker::pop_jni_handle_block() {
  JavaThread* thread = JavaThread::current();

  // Release our JNI handle block
  JNIHandleBlock* compile_handles = thread->active_handles();
  JNIHandleBlock* java_handles = compile_handles->pop_frame_link();
  thread->set_active_handles(java_handles);
  compile_handles->set_pop_frame_link(NULL);
  JNIHandleBlock::release_block(compile_handles, thread); // may block
}

// ------------------------------------------------------------------
// CompileBroker::collect_statistics
//
// Collect statistics about the compilation.

void CompileBroker::collect_statistics(CompilerThread* thread, elapsedTimer time, CompileTask* task) {
  bool success = task->is_success();
  methodHandle method (thread, task->method());
  uint compile_id = task->compile_id();
  bool is_osr = (task->osr_bci() != standard_entry_bci);
  const int comp_level = task->comp_level();
  nmethod* code = task->code();
  CompilerCounters* counters = thread->counters();

  assert(code == NULL || code->is_locked_by_vm(), "will survive the MutexLocker");
  MutexLocker locker(CompileStatistics_lock);

  // _perf variables are production performance counters which are
  // updated regardless of the setting of the CITime and CITimeEach flags
  //

  // account all time, including bailouts and failures in this counter;
  // C1 and C2 counters are counting both successful and unsuccessful compiles
  _t_total_compilation.add(time);

  if (!success) {
    _total_bailout_count++;
    if (UsePerfData) {
      _perf_last_failed_method->set_value(counters->current_method());
      _perf_last_failed_type->set_value(counters->compile_type());
      _perf_total_bailout_count->inc();
    }
    _t_bailedout_compilation.add(time);
  } else if (code == NULL) {
    if (UsePerfData) {
      _perf_last_invalidated_method->set_value(counters->current_method());
      _perf_last_invalidated_type->set_value(counters->compile_type());
      _perf_total_invalidated_count->inc();
    }
    _total_invalidated_count++;
    _t_invalidated_compilation.add(time);
  } else {
    // Compilation succeeded

    // update compilation ticks - used by the implementation of
    // java.lang.management.CompilationMXBean
    _perf_total_compilation->inc(time.ticks());
    _peak_compilation_time = time.milliseconds() > _peak_compilation_time ? time.milliseconds() : _peak_compilation_time;

    if (CITime) {
      int bytes_compiled = method->code_size() + task->num_inlined_bytecodes();
      if (is_osr) {
        _t_osr_compilation.add(time);
        _sum_osr_bytes_compiled += bytes_compiled;
      } else {
        _t_standard_compilation.add(time);
        _sum_standard_bytes_compiled += method->code_size() + task->num_inlined_bytecodes();
      }

      // Collect statistic per compilation level
      if (comp_level > CompLevel_none && comp_level <= CompLevel_full_optimization) {
        CompilerStatistics* stats = &_stats_per_level[comp_level-1];
        if (is_osr) {
          stats->_osr.update(time, bytes_compiled);
        } else {
          stats->_standard.update(time, bytes_compiled);
        }
        stats->_nmethods_size += code->total_size();
        stats->_nmethods_code_size += code->insts_size();
      } else {
        assert(false, "CompilerStatistics object does not exist for compilation level %d", comp_level);
      }

      // Collect statistic per compiler
      AbstractCompiler* comp = compiler(comp_level);
      if (comp) {
        CompilerStatistics* stats = comp->stats();
        if (is_osr) {
          stats->_osr.update(time, bytes_compiled);
        } else {
          stats->_standard.update(time, bytes_compiled);
        }
        stats->_nmethods_size += code->total_size();
        stats->_nmethods_code_size += code->insts_size();
      } else { // if (!comp)
        assert(false, "Compiler object must exist");
      }
    }

    if (UsePerfData) {
      // save the name of the last method compiled
      _perf_last_method->set_value(counters->current_method());
      _perf_last_compile_type->set_value(counters->compile_type());
      _perf_last_compile_size->set_value(method->code_size() +
                                         task->num_inlined_bytecodes());
      if (is_osr) {
        _perf_osr_compilation->inc(time.ticks());
        _perf_sum_osr_bytes_compiled->inc(method->code_size() + task->num_inlined_bytecodes());
      } else {
        _perf_standard_compilation->inc(time.ticks());
        _perf_sum_standard_bytes_compiled->inc(method->code_size() + task->num_inlined_bytecodes());
      }
    }

    if (CITimeEach) {
      double compile_time = time.seconds();
      double bytes_per_sec = compile_time == 0.0 ? 0.0 : (double)(method->code_size() + task->num_inlined_bytecodes()) / compile_time;
      tty->print_cr("%3d   seconds: %6.3f bytes/sec : %f (bytes %d + %d inlined)",
                    compile_id, compile_time, bytes_per_sec, method->code_size(), task->num_inlined_bytecodes());
    }

    // Collect counts of successful compilations
    _sum_nmethod_size      += code->total_size();
    _sum_nmethod_code_size += code->insts_size();
    _total_compile_count++;

    if (UsePerfData) {
      _perf_sum_nmethod_size->inc(     code->total_size());
      _perf_sum_nmethod_code_size->inc(code->insts_size());
      _perf_total_compile_count->inc();
    }

    if (is_osr) {
      if (UsePerfData) _perf_total_osr_compile_count->inc();
      _total_osr_compile_count++;
    } else {
      if (UsePerfData) _perf_total_standard_compile_count->inc();
      _total_standard_compile_count++;
    }
  }
  // set the current method for the thread to null
  if (UsePerfData) counters->set_current_method("");
}

const char* CompileBroker::compiler_name(int comp_level) {
  AbstractCompiler *comp = CompileBroker::compiler(comp_level);
  if (comp == NULL) {
    return "no compiler";
  } else {
    return (comp->name());
  }
}

jlong CompileBroker::total_compilation_ticks() {
  return _perf_total_compilation != NULL ? _perf_total_compilation->get_value() : 0;
}

void CompileBroker::print_times(const char* name, CompilerStatistics* stats) {
  tty->print_cr("  %s {speed: %6.3f bytes/s; standard: %6.3f s, %d bytes, %d methods; osr: %6.3f s, %d bytes, %d methods; nmethods_size: %d bytes; nmethods_code_size: %d bytes}",
                name, stats->bytes_per_second(),
                stats->_standard._time.seconds(), stats->_standard._bytes, stats->_standard._count,
                stats->_osr._time.seconds(), stats->_osr._bytes, stats->_osr._count,
                stats->_nmethods_size, stats->_nmethods_code_size);
}

void CompileBroker::print_times(bool per_compiler, bool aggregate) {
  if (per_compiler) {
    if (aggregate) {
      tty->cr();
      tty->print_cr("Individual compiler times (for compiled methods only)");
      tty->print_cr("------------------------------------------------");
      tty->cr();
    }
    for (unsigned int i = 0; i < sizeof(_compilers) / sizeof(AbstractCompiler*); i++) {
      AbstractCompiler* comp = _compilers[i];
      if (comp != NULL) {
        print_times(comp->name(), comp->stats());
      }
    }
    if (aggregate) {
      tty->cr();
      tty->print_cr("Individual compilation Tier times (for compiled methods only)");
      tty->print_cr("------------------------------------------------");
      tty->cr();
    }
    char tier_name[256];
    for (int tier = CompLevel_simple; tier <= CompilationPolicy::highest_compile_level(); tier++) {
      CompilerStatistics* stats = &_stats_per_level[tier-1];
      sprintf(tier_name, "Tier%d", tier);
      print_times(tier_name, stats);
    }
  }

  if (!aggregate) {
    return;
  }

  elapsedTimer standard_compilation = CompileBroker::_t_standard_compilation;
  elapsedTimer osr_compilation = CompileBroker::_t_osr_compilation;
  elapsedTimer total_compilation = CompileBroker::_t_total_compilation;

  int standard_bytes_compiled = CompileBroker::_sum_standard_bytes_compiled;
  int osr_bytes_compiled = CompileBroker::_sum_osr_bytes_compiled;

  int standard_compile_count = CompileBroker::_total_standard_compile_count;
  int osr_compile_count = CompileBroker::_total_osr_compile_count;
  int total_compile_count = CompileBroker::_total_compile_count;
  int total_bailout_count = CompileBroker::_total_bailout_count;
  int total_invalidated_count = CompileBroker::_total_invalidated_count;

  int nmethods_size = CompileBroker::_sum_nmethod_code_size;
  int nmethods_code_size = CompileBroker::_sum_nmethod_size;

  tty->cr();
  tty->print_cr("Accumulated compiler times");
  tty->print_cr("----------------------------------------------------------");
               //0000000000111111111122222222223333333333444444444455555555556666666666
               //0123456789012345678901234567890123456789012345678901234567890123456789
  tty->print_cr("  Total compilation time   : %7.3f s", total_compilation.seconds());
  tty->print_cr("    Standard compilation   : %7.3f s, Average : %2.3f s",
                standard_compilation.seconds(),
                standard_compile_count == 0 ? 0.0 : standard_compilation.seconds() / standard_compile_count);
  tty->print_cr("    Bailed out compilation : %7.3f s, Average : %2.3f s",
                CompileBroker::_t_bailedout_compilation.seconds(),
                total_bailout_count == 0 ? 0.0 : CompileBroker::_t_bailedout_compilation.seconds() / total_bailout_count);
  tty->print_cr("    On stack replacement   : %7.3f s, Average : %2.3f s",
                osr_compilation.seconds(),
                osr_compile_count == 0 ? 0.0 : osr_compilation.seconds() / osr_compile_count);
  tty->print_cr("    Invalidated            : %7.3f s, Average : %2.3f s",
                CompileBroker::_t_invalidated_compilation.seconds(),
                total_invalidated_count == 0 ? 0.0 : CompileBroker::_t_invalidated_compilation.seconds() / total_invalidated_count);

  AbstractCompiler *comp = compiler(CompLevel_simple);
  if (comp != NULL) {
    tty->cr();
    comp->print_timers();
  }
  comp = compiler(CompLevel_full_optimization);
  if (comp != NULL) {
    tty->cr();
    comp->print_timers();
  }
#if INCLUDE_JVMCI
  if (EnableJVMCI) {
    tty->cr();
    JVMCICompiler::print_hosted_timers();
  }
#endif

  tty->cr();
  tty->print_cr("  Total compiled methods    : %8d methods", total_compile_count);
  tty->print_cr("    Standard compilation    : %8d methods", standard_compile_count);
  tty->print_cr("    On stack replacement    : %8d methods", osr_compile_count);
  int tcb = osr_bytes_compiled + standard_bytes_compiled;
  tty->print_cr("  Total compiled bytecodes  : %8d bytes", tcb);
  tty->print_cr("    Standard compilation    : %8d bytes", standard_bytes_compiled);
  tty->print_cr("    On stack replacement    : %8d bytes", osr_bytes_compiled);
  double tcs = total_compilation.seconds();
  int bps = tcs == 0.0 ? 0 : (int)(tcb / tcs);
  tty->print_cr("  Average compilation speed : %8d bytes/s", bps);
  tty->cr();
  tty->print_cr("  nmethod code size         : %8d bytes", nmethods_code_size);
  tty->print_cr("  nmethod total size        : %8d bytes", nmethods_size);
}

// Print general/accumulated JIT information.
void CompileBroker::print_info(outputStream *out) {
  if (out == NULL) out = tty;
  out->cr();
  out->print_cr("======================");
  out->print_cr("   General JIT info   ");
  out->print_cr("======================");
  out->cr();
  out->print_cr("            JIT is : %7s",     should_compile_new_jobs() ? "on" : "off");
  out->print_cr("  Compiler threads : %7d",     (int)CICompilerCount);
  out->cr();
  out->print_cr("CodeCache overview");
  out->print_cr("--------------------------------------------------------");
  out->cr();
  out->print_cr("         Reserved size : " SIZE_FORMAT_W(7) " KB", CodeCache::max_capacity() / K);
  out->print_cr("        Committed size : " SIZE_FORMAT_W(7) " KB", CodeCache::capacity() / K);
  out->print_cr("  Unallocated capacity : " SIZE_FORMAT_W(7) " KB", CodeCache::unallocated_capacity() / K);
  out->cr();

  out->cr();
  out->print_cr("CodeCache cleaning overview");
  out->print_cr("--------------------------------------------------------");
  out->cr();
  NMethodSweeper::print(out);
  out->print_cr("--------------------------------------------------------");
  out->cr();
}

// Note: tty_lock must not be held upon entry to this function.
//       Print functions called from herein do "micro-locking" on tty_lock.
//       That's a tradeoff which keeps together important blocks of output.
//       At the same time, continuous tty_lock hold time is kept in check,
//       preventing concurrently printing threads from stalling a long time.
void CompileBroker::print_heapinfo(outputStream* out, const char* function, size_t granularity) {
  TimeStamp ts_total;
  TimeStamp ts_global;
  TimeStamp ts;

  bool allFun = !strcmp(function, "all");
  bool aggregate = !strcmp(function, "aggregate") || !strcmp(function, "analyze") || allFun;
  bool usedSpace = !strcmp(function, "UsedSpace") || allFun;
  bool freeSpace = !strcmp(function, "FreeSpace") || allFun;
  bool methodCount = !strcmp(function, "MethodCount") || allFun;
  bool methodSpace = !strcmp(function, "MethodSpace") || allFun;
  bool methodAge = !strcmp(function, "MethodAge") || allFun;
  bool methodNames = !strcmp(function, "MethodNames") || allFun;
  bool discard = !strcmp(function, "discard") || allFun;

  if (out == NULL) {
    out = tty;
  }

  if (!(aggregate || usedSpace || freeSpace || methodCount || methodSpace || methodAge || methodNames || discard)) {
    out->print_cr("\n__ CodeHeapStateAnalytics: Function %s is not supported", function);
    out->cr();
    return;
  }

  ts_total.update(); // record starting point

  if (aggregate) {
    print_info(out);
  }

  // We hold the CodeHeapStateAnalytics_lock all the time, from here until we leave this function.
  // That prevents other threads from destroying (making inconsistent) our view on the CodeHeap.
  // When we request individual parts of the analysis via the jcmd interface, it is possible
  // that in between another thread (another jcmd user or the vm running into CodeCache OOM)
  // updated the aggregated data. We will then see a modified, but again consistent, view
  // on the CodeHeap. That's a tolerable tradeoff we have to accept because we can't hold
  // a lock across user interaction.

  // We should definitely acquire this lock before acquiring Compile_lock and CodeCache_lock.
  // CodeHeapStateAnalytics_lock may be held by a concurrent thread for a long time,
  // leading to an unnecessarily long hold time of the other locks we acquired before.
  ts.update(); // record starting point
  MutexLocker mu0(CodeHeapStateAnalytics_lock, Mutex::_safepoint_check_flag);
  out->print_cr("\n__ CodeHeapStateAnalytics lock wait took %10.3f seconds _________\n", ts.seconds());

  // Holding the CodeCache_lock protects from concurrent alterations of the CodeCache.
  // Unfortunately, such protection is not sufficient:
  // When a new nmethod is created via ciEnv::register_method(), the
  // Compile_lock is taken first. After some initializations,
  // nmethod::new_nmethod() takes over, grabbing the CodeCache_lock
  // immediately (after finalizing the oop references). To lock out concurrent
  // modifiers, we have to grab both locks as well in the described sequence.
  //
  // If we serve an "allFun" call, it is beneficial to hold CodeCache_lock and Compile_lock
  // for the entire duration of aggregation and printing. That makes sure we see
  // a consistent picture and do not run into issues caused by concurrent alterations.
  bool should_take_Compile_lock   = !SafepointSynchronize::is_at_safepoint() &&
                                    !Compile_lock->owned_by_self();
  bool should_take_CodeCache_lock = !SafepointSynchronize::is_at_safepoint() &&
                                    !CodeCache_lock->owned_by_self();
  Mutex*   global_lock_1   = allFun ? (should_take_Compile_lock   ? Compile_lock   : NULL) : NULL;
  Monitor* global_lock_2   = allFun ? (should_take_CodeCache_lock ? CodeCache_lock : NULL) : NULL;
  Mutex*   function_lock_1 = allFun ? NULL : (should_take_Compile_lock   ? Compile_lock    : NULL);
  Monitor* function_lock_2 = allFun ? NULL : (should_take_CodeCache_lock ? CodeCache_lock  : NULL);
  ts_global.update(); // record starting point
  MutexLocker mu1(global_lock_1, Mutex::_safepoint_check_flag);
  MutexLocker mu2(global_lock_2, Mutex::_no_safepoint_check_flag);
  if ((global_lock_1 != NULL) || (global_lock_2 != NULL)) {
    out->print_cr("\n__ Compile & CodeCache (global) lock wait took %10.3f seconds _________\n", ts_global.seconds());
    ts_global.update(); // record starting point
  }

  if (aggregate) {
    ts.update(); // record starting point
    MutexLocker mu11(function_lock_1, Mutex::_safepoint_check_flag);
    MutexLocker mu22(function_lock_2, Mutex::_no_safepoint_check_flag);
    if ((function_lock_1 != NULL) || (function_lock_1 != NULL)) {
      out->print_cr("\n__ Compile & CodeCache (function) lock wait took %10.3f seconds _________\n", ts.seconds());
    }

    ts.update(); // record starting point
    CodeCache::aggregate(out, granularity);
    if ((function_lock_1 != NULL) || (function_lock_1 != NULL)) {
      out->print_cr("\n__ Compile & CodeCache (function) lock hold took %10.3f seconds _________\n", ts.seconds());
    }
  }

  if (usedSpace) CodeCache::print_usedSpace(out);
  if (freeSpace) CodeCache::print_freeSpace(out);
  if (methodCount) CodeCache::print_count(out);
  if (methodSpace) CodeCache::print_space(out);
  if (methodAge) CodeCache::print_age(out);
  if (methodNames) {
    if (allFun) {
      // print_names() can only be used safely if the locks have been continuously held
      // since aggregation begin. That is true only for function "all".
      CodeCache::print_names(out);
    } else {
      out->print_cr("\nCodeHeapStateAnalytics: Function 'MethodNames' is only available as part of function 'all'");
    }
  }
  if (discard) CodeCache::discard(out);

  if ((global_lock_1 != NULL) || (global_lock_2 != NULL)) {
    out->print_cr("\n__ Compile & CodeCache (global) lock hold took %10.3f seconds _________\n", ts_global.seconds());
  }
  out->print_cr("\n__ CodeHeapStateAnalytics total duration %10.3f seconds _________\n", ts_total.seconds());
}
