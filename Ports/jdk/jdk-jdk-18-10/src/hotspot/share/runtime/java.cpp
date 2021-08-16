/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/dynamicArchive.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "code/codeCache.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compilerOracle.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "interpreter/bytecodeHistogram.hpp"
#include "jfr/jfrEvents.hpp"
#include "jfr/support/jfrThreadId.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmci.hpp"
#endif
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/constantPool.hpp"
#include "oops/generateOopMap.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceOop.hpp"
#include "oops/klassVtable.hpp"
#include "oops/method.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/statSampler.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/sweeper.hpp"
#include "runtime/task.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/timer.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vm_version.hpp"
#include "services/memTracker.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/vmError.hpp"
#ifdef COMPILER1
#include "c1/c1_Compiler.hpp"
#include "c1/c1_Runtime1.hpp"
#endif
#ifdef COMPILER2
#include "code/compiledIC.hpp"
#include "opto/compile.hpp"
#include "opto/indexSet.hpp"
#include "opto/runtime.hpp"
#endif
#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

GrowableArray<Method*>* collected_profiled_methods;

int compare_methods(Method** a, Method** b) {
  // compiled_invocation_count() returns int64_t, forcing the entire expression
  // to be evaluated as int64_t. Overflow is not an issue.
  int64_t diff = (((*b)->invocation_count() + (*b)->compiled_invocation_count())
                - ((*a)->invocation_count() + (*a)->compiled_invocation_count()));
  return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
}

void collect_profiled_methods(Method* m) {
  Thread* thread = Thread::current();
  methodHandle mh(thread, m);
  if ((m->method_data() != NULL) &&
      (PrintMethodData || CompilerOracle::should_print(mh))) {
    collected_profiled_methods->push(m);
  }
}

void print_method_profiling_data() {
  if (ProfileInterpreter COMPILER1_PRESENT(|| C1UpdateMethodData) &&
     (PrintMethodData || CompilerOracle::should_print_methods())) {
    ResourceMark rm;
    collected_profiled_methods = new GrowableArray<Method*>(1024);
    SystemDictionary::methods_do(collect_profiled_methods);
    collected_profiled_methods->sort(&compare_methods);

    int count = collected_profiled_methods->length();
    int total_size = 0;
    if (count > 0) {
      for (int index = 0; index < count; index++) {
        Method* m = collected_profiled_methods->at(index);
        ttyLocker ttyl;
        tty->print_cr("------------------------------------------------------------------------");
        m->print_invocation_count();
        tty->print_cr("  mdo size: %d bytes", m->method_data()->size_in_bytes());
        tty->cr();
        // Dump data on parameters if any
        if (m->method_data() != NULL && m->method_data()->parameters_type_data() != NULL) {
          tty->fill_to(2);
          m->method_data()->parameters_type_data()->print_data_on(tty);
        }
        m->print_codes();
        total_size += m->method_data()->size_in_bytes();
      }
      tty->print_cr("------------------------------------------------------------------------");
      tty->print_cr("Total MDO size: %d bytes", total_size);
    }
  }
}


#ifndef PRODUCT

// Statistics printing (method invocation histogram)

GrowableArray<Method*>* collected_invoked_methods;

void collect_invoked_methods(Method* m) {
  if (m->invocation_count() + m->compiled_invocation_count() >= 1) {
    collected_invoked_methods->push(m);
  }
}


// Invocation count accumulators should be unsigned long to shift the
// overflow border. Longer-running workloads tend to create invocation
// counts which already overflow 32-bit counters for individual methods.
void print_method_invocation_histogram() {
  ResourceMark rm;
  collected_invoked_methods = new GrowableArray<Method*>(1024);
  SystemDictionary::methods_do(collect_invoked_methods);
  collected_invoked_methods->sort(&compare_methods);
  //
  tty->cr();
  tty->print_cr("Histogram Over Method Invocation Counters (cutoff = " INTX_FORMAT "):", MethodHistogramCutoff);
  tty->cr();
  tty->print_cr("____Count_(I+C)____Method________________________Module_________________");
  uint64_t total        = 0,
           int_total    = 0,
           comp_total   = 0,
           special_total= 0,
           static_total = 0,
           final_total  = 0,
           synch_total  = 0,
           native_total = 0,
           access_total = 0;
  for (int index = 0; index < collected_invoked_methods->length(); index++) {
    // Counter values returned from getter methods are signed int.
    // To shift the overflow border by a factor of two, we interpret
    // them here as unsigned long. A counter can't be negative anyway.
    Method* m = collected_invoked_methods->at(index);
    uint64_t iic = (uint64_t)m->invocation_count();
    uint64_t cic = (uint64_t)m->compiled_invocation_count();
    if ((iic + cic) >= (uint64_t)MethodHistogramCutoff) m->print_invocation_count();
    int_total  += iic;
    comp_total += cic;
    if (m->is_final())        final_total  += iic + cic;
    if (m->is_static())       static_total += iic + cic;
    if (m->is_synchronized()) synch_total  += iic + cic;
    if (m->is_native())       native_total += iic + cic;
    if (m->is_accessor())     access_total += iic + cic;
  }
  tty->cr();
  total = int_total + comp_total;
  special_total = final_total + static_total +synch_total + native_total + access_total;
  tty->print_cr("Invocations summary for %d methods:", collected_invoked_methods->length());
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (100%%)  total",           total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%) |- interpreted", int_total,     100.0 * int_total    / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%) |- compiled",    comp_total,    100.0 * comp_total   / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%) |- special methods (interpreted and compiled)",
                                                                         special_total, 100.0 * special_total/ total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%)    |- synchronized",synch_total,   100.0 * synch_total  / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%)    |- final",       final_total,   100.0 * final_total  / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%)    |- static",      static_total,  100.0 * static_total / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%)    |- native",      native_total,  100.0 * native_total / total);
  tty->print_cr("\t" UINT64_FORMAT_W(12) " (%4.1f%%)    |- accessor",    access_total,  100.0 * access_total / total);
  tty->cr();
  SharedRuntime::print_call_statistics(comp_total);
}

void print_bytecode_count() {
  if (CountBytecodes || TraceBytecodes || StopInterpreterAt) {
    tty->print_cr("[BytecodeCounter::counter_value = %d]", BytecodeCounter::counter_value());
  }
}


// General statistics printing (profiling ...)
void print_statistics() {
  if (CITime) {
    CompileBroker::print_times();
  }

#ifdef COMPILER1
  if ((PrintC1Statistics || LogVMOutput || LogCompilation) && UseCompiler) {
    FlagSetting fs(DisplayVMOutput, DisplayVMOutput && PrintC1Statistics);
    Runtime1::print_statistics();
    Deoptimization::print_statistics();
    SharedRuntime::print_statistics();
  }
#endif /* COMPILER1 */

#ifdef COMPILER2
  if ((PrintOptoStatistics || LogVMOutput || LogCompilation) && UseCompiler) {
    FlagSetting fs(DisplayVMOutput, DisplayVMOutput && PrintOptoStatistics);
    Compile::print_statistics();
#ifndef COMPILER1
    Deoptimization::print_statistics();
    SharedRuntime::print_statistics();
#endif //COMPILER1
    os::print_statistics();
  }

  if (PrintLockStatistics || PrintPreciseRTMLockingStatistics) {
    OptoRuntime::print_named_counters();
  }
#ifdef ASSERT
  if (CollectIndexSetStatistics) {
    IndexSet::print_statistics();
  }
#endif // ASSERT
#else // COMPILER2
#if INCLUDE_JVMCI
#ifndef COMPILER1
  if ((TraceDeoptimization || LogVMOutput || LogCompilation) && UseCompiler) {
    FlagSetting fs(DisplayVMOutput, DisplayVMOutput && TraceDeoptimization);
    Deoptimization::print_statistics();
    SharedRuntime::print_statistics();
  }
#endif // COMPILER1
#endif // INCLUDE_JVMCI
#endif // COMPILER2

  if (PrintNMethodStatistics) {
    nmethod::print_statistics();
  }
  if (CountCompiledCalls) {
    print_method_invocation_histogram();
  }

  print_method_profiling_data();

  if (TimeOopMap) {
    GenerateOopMap::print_time();
  }
  if (PrintSymbolTableSizeHistogram) {
    SymbolTable::print_histogram();
  }
  if (CountBytecodes || TraceBytecodes || StopInterpreterAt) {
    BytecodeCounter::print();
  }
  if (PrintBytecodePairHistogram) {
    BytecodePairHistogram::print();
  }

  if (PrintCodeCache) {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::print();
  }

  // CodeHeap State Analytics.
  // Does also call NMethodSweeper::print(tty)
  if (PrintCodeHeapAnalytics) {
    CompileBroker::print_heapinfo(NULL, "all", 4096); // details
  } else if (PrintMethodFlushingStatistics) {
    NMethodSweeper::print(tty);
  }

  if (PrintCodeCache2) {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::print_internals();
  }

  if (PrintVtableStats) {
    klassVtable::print_statistics();
    klassItable::print_statistics();
  }
  if (VerifyOops && Verbose) {
    tty->print_cr("+VerifyOops count: %d", StubRoutines::verify_oop_count());
  }

  print_bytecode_count();

  if (PrintSystemDictionaryAtExit) {
    ResourceMark rm;
    MutexLocker mcld(ClassLoaderDataGraph_lock);
    SystemDictionary::print();
  }

  if (PrintClassLoaderDataGraphAtExit) {
    ResourceMark rm;
    MutexLocker mcld(ClassLoaderDataGraph_lock);
    ClassLoaderDataGraph::print();
  }

  if (LogTouchedMethods && PrintTouchedMethodsAtExit) {
    Method::print_touched_methods(tty);
  }

  // Native memory tracking data
  if (PrintNMTStatistics) {
    MemTracker::final_report(tty);
  }

  if (PrintMetaspaceStatisticsAtExit) {
    MetaspaceUtils::print_basic_report(tty, 0);
  }

  ThreadsSMRSupport::log_statistics();
}

#else // PRODUCT MODE STATISTICS

void print_statistics() {

  if (PrintMethodData) {
    print_method_profiling_data();
  }

  if (CITime) {
    CompileBroker::print_times();
  }

  if (PrintCodeCache) {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    CodeCache::print();
  }

  // CodeHeap State Analytics.
  // Does also call NMethodSweeper::print(tty)
  if (PrintCodeHeapAnalytics) {
    CompileBroker::print_heapinfo(NULL, "all", 4096); // details
  } else if (PrintMethodFlushingStatistics) {
    NMethodSweeper::print(tty);
  }

#ifdef COMPILER2
  if (PrintPreciseRTMLockingStatistics) {
    OptoRuntime::print_named_counters();
  }
#endif

  // Native memory tracking data
  if (PrintNMTStatistics) {
    MemTracker::final_report(tty);
  }

  if (PrintMetaspaceStatisticsAtExit) {
    MetaspaceUtils::print_basic_report(tty, 0);
  }

  if (LogTouchedMethods && PrintTouchedMethodsAtExit) {
    Method::print_touched_methods(tty);
  }

  ThreadsSMRSupport::log_statistics();
}

#endif

// Note: before_exit() can be executed only once, if more than one threads
//       are trying to shutdown the VM at the same time, only one thread
//       can run before_exit() and all other threads must wait.
void before_exit(JavaThread* thread) {
  #define BEFORE_EXIT_NOT_RUN 0
  #define BEFORE_EXIT_RUNNING 1
  #define BEFORE_EXIT_DONE    2
  static jint volatile _before_exit_status = BEFORE_EXIT_NOT_RUN;

  // Note: don't use a Mutex to guard the entire before_exit(), as
  // JVMTI post_thread_end_event and post_vm_death_event will run native code.
  // A CAS or OSMutex would work just fine but then we need to manipulate
  // thread state for Safepoint. Here we use Monitor wait() and notify_all()
  // for synchronization.
  { MonitorLocker ml(BeforeExit_lock);
    switch (_before_exit_status) {
    case BEFORE_EXIT_NOT_RUN:
      _before_exit_status = BEFORE_EXIT_RUNNING;
      break;
    case BEFORE_EXIT_RUNNING:
      while (_before_exit_status == BEFORE_EXIT_RUNNING) {
        ml.wait();
      }
      assert(_before_exit_status == BEFORE_EXIT_DONE, "invalid state");
      return;
    case BEFORE_EXIT_DONE:
      // need block to avoid SS compiler bug
      {
        return;
      }
    }
  }

#if INCLUDE_JVMCI
  if (EnableJVMCI) {
    JVMCI::shutdown();
  }
#endif

  // Hang forever on exit if we're reporting an error.
  if (ShowMessageBoxOnError && VMError::is_error_reported()) {
    os::infinite_sleep();
  }

  EventThreadEnd event;
  if (event.should_commit()) {
    event.set_thread(JFR_THREAD_ID(thread));
    event.commit();
  }

  JFR_ONLY(Jfr::on_vm_shutdown();)

  // Stop the WatcherThread. We do this before disenrolling various
  // PeriodicTasks to reduce the likelihood of races.
  if (PeriodicTask::num_tasks() > 0) {
    WatcherThread::stop();
  }

  // shut down the StatSampler task
  StatSampler::disengage();
  StatSampler::destroy();

  // Shut down string deduplication if running.
  if (StringDedup::is_enabled()) {
    StringDedup::stop();
  }

  // Stop concurrent GC threads
  Universe::heap()->stop();

  // Print GC/heap related information.
  Log(gc, heap, exit) log;
  if (log.is_info()) {
    ResourceMark rm;
    LogStream ls_info(log.info());
    Universe::print_on(&ls_info);
    if (log.is_trace()) {
      LogStream ls_trace(log.trace());
      MutexLocker mcld(ClassLoaderDataGraph_lock);
      ClassLoaderDataGraph::print_on(&ls_trace);
    }
  }

  if (PrintBytecodeHistogram) {
    BytecodeHistogram::print();
  }

#ifdef LINUX
  if (DumpPerfMapAtExit) {
    CodeCache::write_perf_map();
  }
#endif

  if (JvmtiExport::should_post_thread_life()) {
    JvmtiExport::post_thread_end(thread);
  }

  // Always call even when there are not JVMTI environments yet, since environments
  // may be attached late and JVMTI must track phases of VM execution
  JvmtiExport::post_vm_death();
  Threads::shutdown_vm_agents();

  // Terminate the signal thread
  // Note: we don't wait until it actually dies.
  os::terminate_signal_thread();

#if INCLUDE_CDS
  if (DynamicDumpSharedSpaces) {
    ExceptionMark em(thread);
    DynamicArchive::dump(thread);
    if (thread->has_pending_exception()) {
      ResourceMark rm(thread);
      oop pending_exception = thread->pending_exception();
      log_error(cds)("ArchiveClassesAtExit has failed %s: %s", pending_exception->klass()->external_name(),
                     java_lang_String::as_utf8_string(java_lang_Throwable::message(pending_exception)));
      thread->clear_pending_exception();
    }
  }
#endif

  print_statistics();
  Universe::heap()->print_tracing_info();

  { MutexLocker ml(BeforeExit_lock);
    _before_exit_status = BEFORE_EXIT_DONE;
    BeforeExit_lock->notify_all();
  }

  if (VerifyStringTableAtExit) {
    size_t fail_cnt = StringTable::verify_and_compare_entries();
    if (fail_cnt != 0) {
      tty->print_cr("ERROR: fail_cnt=" SIZE_FORMAT, fail_cnt);
      guarantee(fail_cnt == 0, "unexpected StringTable verification failures");
    }
  }

  #undef BEFORE_EXIT_NOT_RUN
  #undef BEFORE_EXIT_RUNNING
  #undef BEFORE_EXIT_DONE
}

void vm_exit(int code) {
  Thread* thread =
      ThreadLocalStorage::is_initialized() ? Thread::current_or_null() : NULL;
  if (thread == NULL) {
    // very early initialization failure -- just exit
    vm_direct_exit(code);
  }

  // We'd like to add an entry to the XML log to show that the VM is
  // terminating, but we can't safely do that here. The logic to make
  // XML termination logging safe is tied to the termination of the
  // VMThread, and it doesn't terminate on this exit path. See 8222534.

  if (VMThread::vm_thread() != NULL) {
    if (thread->is_Java_thread()) {
      // We must be "in_vm" for the code below to work correctly.
      // Historically there must have been some exit path for which
      // that was not the case and so we set it explicitly - even
      // though we no longer know what that path may be.
      JavaThread::cast(thread)->set_thread_state(_thread_in_vm);
    }

    // Fire off a VM_Exit operation to bring VM to a safepoint and exit
    VM_Exit op(code);

    // 4945125 The vm thread comes to a safepoint during exit.
    // GC vm_operations can get caught at the safepoint, and the
    // heap is unparseable if they are caught. Grab the Heap_lock
    // to prevent this. The GC vm_operations will not be able to
    // queue until after we release it, but we never do that as we
    // are terminating the VM process.
    MutexLocker ml(Heap_lock);

    VMThread::execute(&op);
    // should never reach here; but in case something wrong with VM Thread.
    vm_direct_exit(code);
  } else {
    // VM thread is gone, just exit
    vm_direct_exit(code);
  }
  ShouldNotReachHere();
}

void notify_vm_shutdown() {
  // For now, just a dtrace probe.
  HOTSPOT_VM_SHUTDOWN();
}

void vm_direct_exit(int code) {
  notify_vm_shutdown();
  os::wait_for_keypress_at_exit();
  os::exit(code);
}

void vm_direct_exit(int code, const char* message) {
  if (message != nullptr) {
    tty->print_cr("%s", message);
  }
  vm_direct_exit(code);
}

void vm_perform_shutdown_actions() {
  if (is_init_completed()) {
    Thread* thread = Thread::current_or_null();
    if (thread != NULL && thread->is_Java_thread()) {
      // We are leaving the VM, set state to native (in case any OS exit
      // handlers call back to the VM)
      JavaThread* jt = JavaThread::cast(thread);
      // Must always be walkable or have no last_Java_frame when in
      // thread_in_native
      jt->frame_anchor()->make_walkable(jt);
      jt->set_thread_state(_thread_in_native);
    }
  }
  notify_vm_shutdown();
}

void vm_shutdown()
{
  vm_perform_shutdown_actions();
  os::wait_for_keypress_at_exit();
  os::shutdown();
}

void vm_abort(bool dump_core) {
  vm_perform_shutdown_actions();
  os::wait_for_keypress_at_exit();

  // Flush stdout and stderr before abort.
  fflush(stdout);
  fflush(stderr);

  os::abort(dump_core);
  ShouldNotReachHere();
}

void vm_notify_during_cds_dumping(const char* error, const char* message) {
  if (error != NULL) {
    tty->print_cr("Error occurred during CDS dumping");
    tty->print("%s", error);
    if (message != NULL) {
      tty->print_cr(": %s", message);
    }
    else {
      tty->cr();
    }
  }
}

void vm_exit_during_cds_dumping(const char* error, const char* message) {
  vm_notify_during_cds_dumping(error, message);

  // Failure during CDS dumping, we don't want to dump core
  vm_abort(false);
}

void vm_notify_during_shutdown(const char* error, const char* message) {
  if (error != NULL) {
    tty->print_cr("Error occurred during initialization of VM");
    tty->print("%s", error);
    if (message != NULL) {
      tty->print_cr(": %s", message);
    }
    else {
      tty->cr();
    }
  }
  if (ShowMessageBoxOnError && WizardMode) {
    fatal("Error occurred during initialization of VM");
  }
}

void vm_exit_during_initialization() {
  vm_notify_during_shutdown(NULL, NULL);

  // Failure during initialization, we don't want to dump core
  vm_abort(false);
}

void vm_exit_during_initialization(Handle exception) {
  tty->print_cr("Error occurred during initialization of VM");
  // If there are exceptions on this thread it must be cleared
  // first and here. Any future calls to EXCEPTION_MARK requires
  // that no pending exceptions exist.
  JavaThread* THREAD = JavaThread::current(); // can't be NULL
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
  }
  java_lang_Throwable::print_stack_trace(exception, tty);
  tty->cr();
  vm_notify_during_shutdown(NULL, NULL);

  // Failure during initialization, we don't want to dump core
  vm_abort(false);
}

void vm_exit_during_initialization(Symbol* ex, const char* message) {
  ResourceMark rm;
  vm_notify_during_shutdown(ex->as_C_string(), message);

  // Failure during initialization, we don't want to dump core
  vm_abort(false);
}

void vm_exit_during_initialization(const char* error, const char* message) {
  vm_notify_during_shutdown(error, message);

  // Failure during initialization, we don't want to dump core
  vm_abort(false);
}

void vm_shutdown_during_initialization(const char* error, const char* message) {
  vm_notify_during_shutdown(error, message);
  vm_shutdown();
}

JDK_Version JDK_Version::_current;
const char* JDK_Version::_java_version;
const char* JDK_Version::_runtime_name;
const char* JDK_Version::_runtime_version;
const char* JDK_Version::_runtime_vendor_version;
const char* JDK_Version::_runtime_vendor_vm_bug_url;

void JDK_Version::initialize() {
  assert(!_current.is_valid(), "Don't initialize twice");

  int major = VM_Version::vm_major_version();
  int minor = VM_Version::vm_minor_version();
  int security = VM_Version::vm_security_version();
  int build = VM_Version::vm_build_number();
  int patch = VM_Version::vm_patch_version();
  _current = JDK_Version(major, minor, security, patch, build);
}

void JDK_Version_init() {
  JDK_Version::initialize();
}

static int64_t encode_jdk_version(const JDK_Version& v) {
  return
    ((int64_t)v.major_version()          << (BitsPerByte * 4)) |
    ((int64_t)v.minor_version()          << (BitsPerByte * 3)) |
    ((int64_t)v.security_version()       << (BitsPerByte * 2)) |
    ((int64_t)v.patch_version()          << (BitsPerByte * 1)) |
    ((int64_t)v.build_number()           << (BitsPerByte * 0));
}

int JDK_Version::compare(const JDK_Version& other) const {
  assert(is_valid() && other.is_valid(), "Invalid version (uninitialized?)");
  uint64_t e = encode_jdk_version(*this);
  uint64_t o = encode_jdk_version(other);
  return (e > o) ? 1 : ((e == o) ? 0 : -1);
}

/* See JEP 223 */
void JDK_Version::to_string(char* buffer, size_t buflen) const {
  assert(buffer && buflen > 0, "call with useful buffer");
  size_t index = 0;

  if (!is_valid()) {
    jio_snprintf(buffer, buflen, "%s", "(uninitialized)");
  } else {
    int rc = jio_snprintf(
        &buffer[index], buflen - index, "%d.%d", _major, _minor);
    if (rc == -1) return;
    index += rc;
    if (_patch > 0) {
      rc = jio_snprintf(&buffer[index], buflen - index, ".%d.%d", _security, _patch);
      if (rc == -1) return;
      index += rc;
    } else if (_security > 0) {
      rc = jio_snprintf(&buffer[index], buflen - index, ".%d", _security);
      if (rc == -1) return;
      index += rc;
    }
    if (_build > 0) {
      rc = jio_snprintf(&buffer[index], buflen - index, "+%d", _build);
      if (rc == -1) return;
      index += rc;
    }
  }
}
