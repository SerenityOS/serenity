/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
#include "cds/metaspaceShared.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaThreadStatus.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/codeCache.hpp"
#include "code/scopeDesc.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileTask.hpp"
#include "compiler/compilerThread.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/gcId.hpp"
#include "gc/shared/gcLocker.inline.hpp"
#include "gc/shared/gcVMOperations.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/tlab_globals.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/linkResolver.hpp"
#include "interpreter/oopMapCache.hpp"
#include "jfr/jfrEvents.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "logging/logAsyncWriter.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/symbol.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "oops/verifyOopClosure.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/flags/jvmFlagLimit.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/handshake.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/jniPeriodicChecker.hpp"
#include "runtime/monitorDeflationThread.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/safepointMechanism.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/serviceThread.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/statSampler.hpp"
#include "runtime/task.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadCritical.hpp"
#include "runtime/threadSMR.inline.hpp"
#include "runtime/threadStatisticalInfo.hpp"
#include "runtime/threadWXSetters.inline.hpp"
#include "runtime/timer.hpp"
#include "runtime/timerTrace.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vframe_hp.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vm_version.hpp"
#include "services/attachListener.hpp"
#include "services/management.hpp"
#include "services/memTracker.hpp"
#include "services/threadService.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/macros.hpp"
#include "utilities/preserveException.hpp"
#include "utilities/spinYield.hpp"
#include "utilities/vmError.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmci.hpp"
#include "jvmci/jvmciEnv.hpp"
#endif
#ifdef COMPILER1
#include "c1/c1_Compiler.hpp"
#endif
#ifdef COMPILER2
#include "opto/c2compiler.hpp"
#include "opto/idealGraphPrinter.hpp"
#endif
#if INCLUDE_RTM_OPT
#include "runtime/rtmLocking.hpp"
#endif
#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

// Initialization after module runtime initialization
void universe_post_module_init();  // must happen after call_initPhase2

#ifdef DTRACE_ENABLED

// Only bother with this argument setup if dtrace is available

  #define HOTSPOT_THREAD_PROBE_start HOTSPOT_THREAD_START
  #define HOTSPOT_THREAD_PROBE_stop HOTSPOT_THREAD_STOP

  #define DTRACE_THREAD_PROBE(probe, javathread)                           \
    {                                                                      \
      ResourceMark rm(this);                                               \
      int len = 0;                                                         \
      const char* name = (javathread)->name();                             \
      len = strlen(name);                                                  \
      HOTSPOT_THREAD_PROBE_##probe(/* probe = start, stop */               \
        (char *) name, len,                                                \
        java_lang_Thread::thread_id((javathread)->threadObj()),            \
        (uintptr_t) (javathread)->osthread()->thread_id(),                 \
        java_lang_Thread::is_daemon((javathread)->threadObj()));           \
    }

#else //  ndef DTRACE_ENABLED

  #define DTRACE_THREAD_PROBE(probe, javathread)

#endif // ndef DTRACE_ENABLED

#ifndef USE_LIBRARY_BASED_TLS_ONLY
// Current thread is maintained as a thread-local variable
THREAD_LOCAL Thread* Thread::_thr_current = NULL;
#endif

// ======= Thread ========
void* Thread::allocate(size_t size, bool throw_excpt, MEMFLAGS flags) {
  return throw_excpt ? AllocateHeap(size, flags, CURRENT_PC)
                       : AllocateHeap(size, flags, CURRENT_PC, AllocFailStrategy::RETURN_NULL);
}

void Thread::operator delete(void* p) {
  FreeHeap(p);
}

void JavaThread::smr_delete() {
  if (_on_thread_list) {
    ThreadsSMRSupport::smr_delete(this);
  } else {
    delete this;
  }
}

// Base class for all threads: VMThread, WatcherThread, ConcurrentMarkSweepThread,
// JavaThread

DEBUG_ONLY(Thread* Thread::_starting_thread = NULL;)

Thread::Thread() {

  DEBUG_ONLY(_run_state = PRE_CALL_RUN;)

  // stack and get_thread
  set_stack_base(NULL);
  set_stack_size(0);
  set_lgrp_id(-1);
  DEBUG_ONLY(clear_suspendible_thread();)

  // allocated data structures
  set_osthread(NULL);
  set_resource_area(new (mtThread)ResourceArea());
  DEBUG_ONLY(_current_resource_mark = NULL;)
  set_handle_area(new (mtThread) HandleArea(NULL));
  set_metadata_handles(new (ResourceObj::C_HEAP, mtClass) GrowableArray<Metadata*>(30, mtClass));
  set_active_handles(NULL);
  set_free_handle_block(NULL);
  set_last_handle_mark(NULL);
  DEBUG_ONLY(_missed_ic_stub_refill_verifier = NULL);

  // Initial value of zero ==> never claimed.
  _threads_do_token = 0;
  _threads_hazard_ptr = NULL;
  _threads_list_ptr = NULL;
  _nested_threads_hazard_ptr_cnt = 0;
  _rcu_counter = 0;

  // the handle mark links itself to last_handle_mark
  new HandleMark(this);

  // plain initialization
  debug_only(_owned_locks = NULL;)
  NOT_PRODUCT(_skip_gcalot = false;)
  _jvmti_env_iteration_count = 0;
  set_allocated_bytes(0);
  _current_pending_raw_monitor = NULL;

  // thread-specific hashCode stream generator state - Marsaglia shift-xor form
  _hashStateX = os::random();
  _hashStateY = 842502087;
  _hashStateZ = 0x8767;    // (int)(3579807591LL & 0xffff) ;
  _hashStateW = 273326509;

  // Many of the following fields are effectively final - immutable
  // Note that nascent threads can't use the Native Monitor-Mutex
  // construct until the _MutexEvent is initialized ...
  // CONSIDER: instead of using a fixed set of purpose-dedicated ParkEvents
  // we might instead use a stack of ParkEvents that we could provision on-demand.
  // The stack would act as a cache to avoid calls to ParkEvent::Allocate()
  // and ::Release()
  _ParkEvent   = ParkEvent::Allocate(this);

#ifdef CHECK_UNHANDLED_OOPS
  if (CheckUnhandledOops) {
    _unhandled_oops = new UnhandledOops(this);
  }
#endif // CHECK_UNHANDLED_OOPS

  // Notify the barrier set that a thread is being created. The initial
  // thread is created before the barrier set is available.  The call to
  // BarrierSet::on_thread_create() for this thread is therefore deferred
  // to BarrierSet::set_barrier_set().
  BarrierSet* const barrier_set = BarrierSet::barrier_set();
  if (barrier_set != NULL) {
    barrier_set->on_thread_create(this);
  } else {
    // Only the main thread should be created before the barrier set
    // and that happens just before Thread::current is set. No other thread
    // can attach as the VM is not created yet, so they can't execute this code.
    // If the main thread creates other threads before the barrier set that is an error.
    assert(Thread::current_or_null() == NULL, "creating thread before barrier set");
  }

  MACOS_AARCH64_ONLY(DEBUG_ONLY(_wx_init = false));
}

void Thread::initialize_tlab() {
  if (UseTLAB) {
    tlab().initialize();
  }
}

void Thread::initialize_thread_current() {
#ifndef USE_LIBRARY_BASED_TLS_ONLY
  assert(_thr_current == NULL, "Thread::current already initialized");
  _thr_current = this;
#endif
  assert(ThreadLocalStorage::thread() == NULL, "ThreadLocalStorage::thread already initialized");
  ThreadLocalStorage::set_thread(this);
  assert(Thread::current() == ThreadLocalStorage::thread(), "TLS mismatch!");
}

void Thread::clear_thread_current() {
  assert(Thread::current() == ThreadLocalStorage::thread(), "TLS mismatch!");
#ifndef USE_LIBRARY_BASED_TLS_ONLY
  _thr_current = NULL;
#endif
  ThreadLocalStorage::set_thread(NULL);
}

void Thread::record_stack_base_and_size() {
  // Note: at this point, Thread object is not yet initialized. Do not rely on
  // any members being initialized. Do not rely on Thread::current() being set.
  // If possible, refrain from doing anything which may crash or assert since
  // quite probably those crash dumps will be useless.
  set_stack_base(os::current_stack_base());
  set_stack_size(os::current_stack_size());

  // Set stack limits after thread is initialized.
  if (is_Java_thread()) {
    JavaThread::cast(this)->stack_overflow_state()->initialize(stack_base(), stack_end());
  }
}

#if INCLUDE_NMT
void Thread::register_thread_stack_with_NMT() {
  MemTracker::record_thread_stack(stack_end(), stack_size());
}

void Thread::unregister_thread_stack_with_NMT() {
  MemTracker::release_thread_stack(stack_end(), stack_size());
}
#endif // INCLUDE_NMT

void Thread::call_run() {
  DEBUG_ONLY(_run_state = CALL_RUN;)

  // At this point, Thread object should be fully initialized and
  // Thread::current() should be set.

  assert(Thread::current_or_null() != NULL, "current thread is unset");
  assert(Thread::current_or_null() == this, "current thread is wrong");

  // Perform common initialization actions

  register_thread_stack_with_NMT();

  MACOS_AARCH64_ONLY(this->init_wx());

  JFR_ONLY(Jfr::on_thread_start(this);)

  log_debug(os, thread)("Thread " UINTX_FORMAT " stack dimensions: "
    PTR_FORMAT "-" PTR_FORMAT " (" SIZE_FORMAT "k).",
    os::current_thread_id(), p2i(stack_end()),
    p2i(stack_base()), stack_size()/1024);

  // Perform <ChildClass> initialization actions
  DEBUG_ONLY(_run_state = PRE_RUN;)
  this->pre_run();

  // Invoke <ChildClass>::run()
  DEBUG_ONLY(_run_state = RUN;)
  this->run();
  // Returned from <ChildClass>::run(). Thread finished.

  // Perform common tear-down actions

  assert(Thread::current_or_null() != NULL, "current thread is unset");
  assert(Thread::current_or_null() == this, "current thread is wrong");

  // Perform <ChildClass> tear-down actions
  DEBUG_ONLY(_run_state = POST_RUN;)
  this->post_run();

  // Note: at this point the thread object may already have deleted itself,
  // so from here on do not dereference *this*. Not all thread types currently
  // delete themselves when they terminate. But no thread should ever be deleted
  // asynchronously with respect to its termination - that is what _run_state can
  // be used to check.

  assert(Thread::current_or_null() == NULL, "current thread still present");
}

Thread::~Thread() {

  // Attached threads will remain in PRE_CALL_RUN, as will threads that don't actually
  // get started due to errors etc. Any active thread should at least reach post_run
  // before it is deleted (usually in post_run()).
  assert(_run_state == PRE_CALL_RUN ||
         _run_state == POST_RUN, "Active Thread deleted before post_run(): "
         "_run_state=%d", (int)_run_state);

  // Notify the barrier set that a thread is being destroyed. Note that a barrier
  // set might not be available if we encountered errors during bootstrapping.
  BarrierSet* const barrier_set = BarrierSet::barrier_set();
  if (barrier_set != NULL) {
    barrier_set->on_thread_destroy(this);
  }

  // deallocate data structures
  delete resource_area();
  // since the handle marks are using the handle area, we have to deallocated the root
  // handle mark before deallocating the thread's handle area,
  assert(last_handle_mark() != NULL, "check we have an element");
  delete last_handle_mark();
  assert(last_handle_mark() == NULL, "check we have reached the end");

  ParkEvent::Release(_ParkEvent);
  // Set to NULL as a termination indicator for has_terminated().
  Atomic::store(&_ParkEvent, (ParkEvent*)NULL);

  delete handle_area();
  delete metadata_handles();

  // osthread() can be NULL, if creation of thread failed.
  if (osthread() != NULL) os::free_thread(osthread());

  // Clear Thread::current if thread is deleting itself and it has not
  // already been done. This must be done before the memory is deallocated.
  // Needed to ensure JNI correctly detects non-attached threads.
  if (this == Thread::current_or_null()) {
    Thread::clear_thread_current();
  }

  CHECK_UNHANDLED_OOPS_ONLY(if (CheckUnhandledOops) delete unhandled_oops();)
}

#ifdef ASSERT
// A JavaThread is considered dangling if it not handshake-safe with respect to
// the current thread, it is not on a ThreadsList, or not at safepoint.
void Thread::check_for_dangling_thread_pointer(Thread *thread) {
  assert(!thread->is_Java_thread() ||
         JavaThread::cast(thread)->is_handshake_safe_for(Thread::current()) ||
         !JavaThread::cast(thread)->on_thread_list() ||
         SafepointSynchronize::is_at_safepoint() ||
         ThreadsSMRSupport::is_a_protected_JavaThread_with_lock(JavaThread::cast(thread)),
         "possibility of dangling Thread pointer");
}
#endif

// Is the target JavaThread protected by the calling Thread
// or by some other mechanism:
bool Thread::is_JavaThread_protected(const JavaThread* p) {
  // Do the simplest check first:
  if (SafepointSynchronize::is_at_safepoint()) {
    // The target is protected since JavaThreads cannot exit
    // while we're at a safepoint.
    return true;
  }

  // If the target hasn't been started yet then it is trivially
  // "protected". We assume the caller is the thread that will do
  // the starting.
  if (p->osthread() == NULL || p->osthread()->get_state() <= INITIALIZED) {
    return true;
  }

  // Now make the simple checks based on who the caller is:
  Thread* current_thread = Thread::current();
  if (current_thread == p || Threads_lock->owner() == current_thread) {
    // Target JavaThread is self or calling thread owns the Threads_lock.
    // Second check is the same as Threads_lock->owner_is_self(),
    // but we already have the current thread so check directly.
    return true;
  }

  // Check the ThreadsLists associated with the calling thread (if any)
  // to see if one of them protects the target JavaThread:
  for (SafeThreadsListPtr* stlp = current_thread->_threads_list_ptr;
       stlp != NULL; stlp = stlp->previous()) {
    if (stlp->list()->includes(p)) {
      // The target JavaThread is protected by this ThreadsList:
      return true;
    }
  }

  // Use this debug code with -XX:+UseNewCode to diagnose locations that
  // are missing a ThreadsListHandle or other protection mechanism:
  // guarantee(!UseNewCode, "current_thread=" INTPTR_FORMAT " is not protecting p="
  //           INTPTR_FORMAT, p2i(current_thread), p2i(p));

  // Note: Since 'p' isn't protected by a TLH, the call to
  // p->is_handshake_safe_for() may crash, but we have debug bits so
  // we'll be able to figure out what protection mechanism is missing.
  assert(p->is_handshake_safe_for(current_thread), "JavaThread=" INTPTR_FORMAT
         " is not protected and not handshake safe.", p2i(p));

  // The target JavaThread is not protected so it is not safe to query:
  return false;
}

ThreadPriority Thread::get_priority(const Thread* const thread) {
  ThreadPriority priority;
  // Can return an error!
  (void)os::get_priority(thread, priority);
  assert(MinPriority <= priority && priority <= MaxPriority, "non-Java priority found");
  return priority;
}

void Thread::set_priority(Thread* thread, ThreadPriority priority) {
  debug_only(check_for_dangling_thread_pointer(thread);)
  // Can return an error!
  (void)os::set_priority(thread, priority);
}


void Thread::start(Thread* thread) {
  // Start is different from resume in that its safety is guaranteed by context or
  // being called from a Java method synchronized on the Thread object.
  if (thread->is_Java_thread()) {
    // Initialize the thread state to RUNNABLE before starting this thread.
    // Can not set it after the thread started because we do not know the
    // exact thread state at that time. It could be in MONITOR_WAIT or
    // in SLEEPING or some other state.
    java_lang_Thread::set_thread_status(JavaThread::cast(thread)->threadObj(),
                                        JavaThreadStatus::RUNNABLE);
  }
  os::start_thread(thread);
}

// GC Support
bool Thread::claim_par_threads_do(uintx claim_token) {
  uintx token = _threads_do_token;
  if (token != claim_token) {
    uintx res = Atomic::cmpxchg(&_threads_do_token, token, claim_token);
    if (res == token) {
      return true;
    }
    guarantee(res == claim_token, "invariant");
  }
  return false;
}

void Thread::oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf) {
  if (active_handles() != NULL) {
    active_handles()->oops_do(f);
  }
  // Do oop for ThreadShadow
  f->do_oop((oop*)&_pending_exception);
  handle_area()->oops_do(f);
}

// If the caller is a NamedThread, then remember, in the current scope,
// the given JavaThread in its _processed_thread field.
class RememberProcessedThread: public StackObj {
  NamedThread* _cur_thr;
public:
  RememberProcessedThread(Thread* thread) {
    Thread* self = Thread::current();
    if (self->is_Named_thread()) {
      _cur_thr = (NamedThread *)self;
      assert(_cur_thr->processed_thread() == NULL, "nesting not supported");
      _cur_thr->set_processed_thread(thread);
    } else {
      _cur_thr = NULL;
    }
  }

  ~RememberProcessedThread() {
    if (_cur_thr) {
      assert(_cur_thr->processed_thread() != NULL, "nesting not supported");
      _cur_thr->set_processed_thread(NULL);
    }
  }
};

void Thread::oops_do(OopClosure* f, CodeBlobClosure* cf) {
  // Record JavaThread to GC thread
  RememberProcessedThread rpt(this);
  oops_do_no_frames(f, cf);
  oops_do_frames(f, cf);
}

void Thread::metadata_handles_do(void f(Metadata*)) {
  // Only walk the Handles in Thread.
  if (metadata_handles() != NULL) {
    for (int i = 0; i< metadata_handles()->length(); i++) {
      f(metadata_handles()->at(i));
    }
  }
}

void Thread::print_on(outputStream* st, bool print_extended_info) const {
  // get_priority assumes osthread initialized
  if (osthread() != NULL) {
    int os_prio;
    if (os::get_native_priority(this, &os_prio) == OS_OK) {
      st->print("os_prio=%d ", os_prio);
    }

    st->print("cpu=%.2fms ",
              os::thread_cpu_time(const_cast<Thread*>(this), true) / 1000000.0
              );
    st->print("elapsed=%.2fs ",
              _statistical_info.getElapsedTime() / 1000.0
              );
    if (is_Java_thread() && (PrintExtendedThreadInfo || print_extended_info)) {
      size_t allocated_bytes = (size_t) const_cast<Thread*>(this)->cooked_allocated_bytes();
      st->print("allocated=" SIZE_FORMAT "%s ",
                byte_size_in_proper_unit(allocated_bytes),
                proper_unit_for_byte_size(allocated_bytes)
                );
      st->print("defined_classes=" INT64_FORMAT " ", _statistical_info.getDefineClassCount());
    }

    st->print("tid=" INTPTR_FORMAT " ", p2i(this));
    osthread()->print_on(st);
  }
  ThreadsSMRSupport::print_info_on(this, st);
  st->print(" ");
  debug_only(if (WizardMode) print_owned_locks_on(st);)
}

void Thread::print() const { print_on(tty); }

// Thread::print_on_error() is called by fatal error handler. Don't use
// any lock or allocate memory.
void Thread::print_on_error(outputStream* st, char* buf, int buflen) const {
  assert(!(is_Compiler_thread() || is_Java_thread()), "Can't call name() here if it allocates");

  st->print("%s \"%s\"", type_name(), name());

  OSThread* os_thr = osthread();
  if (os_thr != NULL) {
    if (os_thr->get_state() != ZOMBIE) {
      st->print(" [stack: " PTR_FORMAT "," PTR_FORMAT "]",
                p2i(stack_end()), p2i(stack_base()));
      st->print(" [id=%d]", osthread()->thread_id());
    } else {
      st->print(" terminated");
    }
  } else {
    st->print(" unknown state (no osThread)");
  }
  ThreadsSMRSupport::print_info_on(this, st);
}

void Thread::print_value_on(outputStream* st) const {
  if (is_Named_thread()) {
    st->print(" \"%s\" ", name());
  }
  st->print(INTPTR_FORMAT, p2i(this));   // print address
}

#ifdef ASSERT
void Thread::print_owned_locks_on(outputStream* st) const {
  Mutex* cur = _owned_locks;
  if (cur == NULL) {
    st->print(" (no locks) ");
  } else {
    st->print_cr(" Locks owned:");
    while (cur) {
      cur->print_on(st);
      cur = cur->next();
    }
  }
}
#endif // ASSERT

// We had to move these methods here, because vm threads get into ObjectSynchronizer::enter
// However, there is a note in JavaThread::is_lock_owned() about the VM threads not being
// used for compilation in the future. If that change is made, the need for these methods
// should be revisited, and they should be removed if possible.

bool Thread::is_lock_owned(address adr) const {
  return is_in_full_stack(adr);
}

bool Thread::set_as_starting_thread() {
  assert(_starting_thread == NULL, "already initialized: "
         "_starting_thread=" INTPTR_FORMAT, p2i(_starting_thread));
  // NOTE: this must be called inside the main thread.
  DEBUG_ONLY(_starting_thread = this;)
  return os::create_main_thread(JavaThread::cast(this));
}

static void initialize_class(Symbol* class_name, TRAPS) {
  Klass* klass = SystemDictionary::resolve_or_fail(class_name, true, CHECK);
  InstanceKlass::cast(klass)->initialize(CHECK);
}


// Creates the initial ThreadGroup
static Handle create_initial_thread_group(TRAPS) {
  Handle system_instance = JavaCalls::construct_new_instance(
                            vmClasses::ThreadGroup_klass(),
                            vmSymbols::void_method_signature(),
                            CHECK_NH);
  Universe::set_system_thread_group(system_instance());

  Handle string = java_lang_String::create_from_str("main", CHECK_NH);
  Handle main_instance = JavaCalls::construct_new_instance(
                            vmClasses::ThreadGroup_klass(),
                            vmSymbols::threadgroup_string_void_signature(),
                            system_instance,
                            string,
                            CHECK_NH);
  return main_instance;
}

// Creates the initial Thread, and sets it to running.
static void create_initial_thread(Handle thread_group, JavaThread* thread,
                                 TRAPS) {
  InstanceKlass* ik = vmClasses::Thread_klass();
  assert(ik->is_initialized(), "must be");
  instanceHandle thread_oop = ik->allocate_instance_handle(CHECK);

  // Cannot use JavaCalls::construct_new_instance because the java.lang.Thread
  // constructor calls Thread.current(), which must be set here for the
  // initial thread.
  java_lang_Thread::set_thread(thread_oop(), thread);
  java_lang_Thread::set_priority(thread_oop(), NormPriority);
  thread->set_threadObj(thread_oop());

  Handle string = java_lang_String::create_from_str("main", CHECK);

  JavaValue result(T_VOID);
  JavaCalls::call_special(&result, thread_oop,
                          ik,
                          vmSymbols::object_initializer_name(),
                          vmSymbols::threadgroup_string_void_signature(),
                          thread_group,
                          string,
                          CHECK);

  // Set thread status to running since main thread has
  // been started and running.
  java_lang_Thread::set_thread_status(thread_oop(),
                                      JavaThreadStatus::RUNNABLE);
}

// Extract version and vendor specific information from
// java.lang.VersionProps fields.
// Returned char* is allocated in the thread's resource area
// so must be copied for permanency.
static const char* get_java_version_info(InstanceKlass* ik,
                                         Symbol* field_name) {
  fieldDescriptor fd;
  bool found = ik != NULL &&
               ik->find_local_field(field_name,
                                    vmSymbols::string_signature(), &fd);
  if (found) {
    oop name_oop = ik->java_mirror()->obj_field(fd.offset());
    if (name_oop == NULL) {
      return NULL;
    }
    const char* name = java_lang_String::as_utf8_string(name_oop);
    return name;
  } else {
    return NULL;
  }
}

// General purpose hook into Java code, run once when the VM is initialized.
// The Java library method itself may be changed independently from the VM.
static void call_postVMInitHook(TRAPS) {
  Klass* klass = SystemDictionary::resolve_or_null(vmSymbols::jdk_internal_vm_PostVMInitHook(), THREAD);
  if (klass != NULL) {
    JavaValue result(T_VOID);
    JavaCalls::call_static(&result, klass, vmSymbols::run_method_name(),
                           vmSymbols::void_method_signature(),
                           CHECK);
  }
}

// Initialized by VMThread at vm_global_init
static OopStorage* _thread_oop_storage = NULL;

oop  JavaThread::threadObj() const    {
  return _threadObj.resolve();
}

void JavaThread::set_threadObj(oop p) {
  assert(_thread_oop_storage != NULL, "not yet initialized");
  _threadObj = OopHandle(_thread_oop_storage, p);
}

OopStorage* JavaThread::thread_oop_storage() {
  assert(_thread_oop_storage != NULL, "not yet initialized");
  return _thread_oop_storage;
}

void JavaThread::allocate_threadObj(Handle thread_group, const char* thread_name,
                                    bool daemon, TRAPS) {
  assert(thread_group.not_null(), "thread group should be specified");
  assert(threadObj() == NULL, "should only create Java thread object once");

  InstanceKlass* ik = vmClasses::Thread_klass();
  assert(ik->is_initialized(), "must be");
  instanceHandle thread_oop = ik->allocate_instance_handle(CHECK);

  // We are called from jni_AttachCurrentThread/jni_AttachCurrentThreadAsDaemon.
  // We cannot use JavaCalls::construct_new_instance because the java.lang.Thread
  // constructor calls Thread.current(), which must be set here.
  java_lang_Thread::set_thread(thread_oop(), this);
  java_lang_Thread::set_priority(thread_oop(), NormPriority);
  set_threadObj(thread_oop());

  JavaValue result(T_VOID);
  if (thread_name != NULL) {
    Handle name = java_lang_String::create_from_str(thread_name, CHECK);
    // Thread gets assigned specified name and null target
    JavaCalls::call_special(&result,
                            thread_oop,
                            ik,
                            vmSymbols::object_initializer_name(),
                            vmSymbols::threadgroup_string_void_signature(),
                            thread_group,
                            name,
                            THREAD);
  } else {
    // Thread gets assigned name "Thread-nnn" and null target
    // (java.lang.Thread doesn't have a constructor taking only a ThreadGroup argument)
    JavaCalls::call_special(&result,
                            thread_oop,
                            ik,
                            vmSymbols::object_initializer_name(),
                            vmSymbols::threadgroup_runnable_void_signature(),
                            thread_group,
                            Handle(),
                            THREAD);
  }


  if (daemon) {
    java_lang_Thread::set_daemon(thread_oop());
  }

  if (HAS_PENDING_EXCEPTION) {
    return;
  }

  Klass* group = vmClasses::ThreadGroup_klass();
  Handle threadObj(THREAD, this->threadObj());

  JavaCalls::call_special(&result,
                          thread_group,
                          group,
                          vmSymbols::add_method_name(),
                          vmSymbols::thread_void_signature(),
                          threadObj,          // Arg 1
                          THREAD);
}

// ======= JavaThread ========

#if INCLUDE_JVMCI

jlong* JavaThread::_jvmci_old_thread_counters;

bool jvmci_counters_include(JavaThread* thread) {
  return !JVMCICountersExcludeCompiler || !thread->is_Compiler_thread();
}

void JavaThread::collect_counters(jlong* array, int length) {
  assert(length == JVMCICounterSize, "wrong value");
  for (int i = 0; i < length; i++) {
    array[i] = _jvmci_old_thread_counters[i];
  }
  for (JavaThread* tp : ThreadsListHandle()) {
    if (jvmci_counters_include(tp)) {
      for (int i = 0; i < length; i++) {
        array[i] += tp->_jvmci_counters[i];
      }
    }
  }
}

// Attempt to enlarge the array for per thread counters.
jlong* resize_counters_array(jlong* old_counters, int current_size, int new_size) {
  jlong* new_counters = NEW_C_HEAP_ARRAY_RETURN_NULL(jlong, new_size, mtJVMCI);
  if (new_counters == NULL) {
    return NULL;
  }
  if (old_counters == NULL) {
    old_counters = new_counters;
    memset(old_counters, 0, sizeof(jlong) * new_size);
  } else {
    for (int i = 0; i < MIN2((int) current_size, new_size); i++) {
      new_counters[i] = old_counters[i];
    }
    if (new_size > current_size) {
      memset(new_counters + current_size, 0, sizeof(jlong) * (new_size - current_size));
    }
    FREE_C_HEAP_ARRAY(jlong, old_counters);
  }
  return new_counters;
}

// Attempt to enlarge the array for per thread counters.
bool JavaThread::resize_counters(int current_size, int new_size) {
  jlong* new_counters = resize_counters_array(_jvmci_counters, current_size, new_size);
  if (new_counters == NULL) {
    return false;
  } else {
    _jvmci_counters = new_counters;
    return true;
  }
}

class VM_JVMCIResizeCounters : public VM_Operation {
 private:
  int _new_size;
  bool _failed;

 public:
  VM_JVMCIResizeCounters(int new_size) : _new_size(new_size), _failed(false) { }
  VMOp_Type type()                  const        { return VMOp_JVMCIResizeCounters; }
  bool allow_nested_vm_operations() const        { return true; }
  void doit() {
    // Resize the old thread counters array
    jlong* new_counters = resize_counters_array(JavaThread::_jvmci_old_thread_counters, JVMCICounterSize, _new_size);
    if (new_counters == NULL) {
      _failed = true;
      return;
    } else {
      JavaThread::_jvmci_old_thread_counters = new_counters;
    }

    // Now resize each threads array
    for (JavaThread* tp : ThreadsListHandle()) {
      if (!tp->resize_counters(JVMCICounterSize, _new_size)) {
        _failed = true;
        break;
      }
    }
    if (!_failed) {
      JVMCICounterSize = _new_size;
    }
  }

  bool failed() { return _failed; }
};

bool JavaThread::resize_all_jvmci_counters(int new_size) {
  VM_JVMCIResizeCounters op(new_size);
  VMThread::execute(&op);
  return !op.failed();
}

#endif // INCLUDE_JVMCI

#ifdef ASSERT
// Checks safepoint allowed and clears unhandled oops at potential safepoints.
void JavaThread::check_possible_safepoint() {
  if (_no_safepoint_count > 0) {
    print_owned_locks();
    assert(false, "Possible safepoint reached by thread that does not allow it");
  }
#ifdef CHECK_UNHANDLED_OOPS
  // Clear unhandled oops in JavaThreads so we get a crash right away.
  clear_unhandled_oops();
#endif // CHECK_UNHANDLED_OOPS
}

void JavaThread::check_for_valid_safepoint_state() {
  // Check NoSafepointVerifier, which is implied by locks taken that can be
  // shared with the VM thread.  This makes sure that no locks with allow_vm_block
  // are held.
  check_possible_safepoint();

  if (thread_state() != _thread_in_vm) {
    fatal("LEAF method calling lock?");
  }

  if (GCALotAtAllSafepoints) {
    // We could enter a safepoint here and thus have a gc
    InterfaceSupport::check_gc_alot();
  }
}
#endif // ASSERT

// A JavaThread is a normal Java thread

JavaThread::JavaThread() :
  // Initialize fields

  _on_thread_list(false),
  DEBUG_ONLY(_java_call_counter(0) COMMA)
  _entry_point(nullptr),
  _deopt_mark(nullptr),
  _deopt_nmethod(nullptr),
  _vframe_array_head(nullptr),
  _vframe_array_last(nullptr),
  _jvmti_deferred_updates(nullptr),
  _callee_target(nullptr),
  _vm_result(nullptr),
  _vm_result_2(nullptr),

  _current_pending_monitor(NULL),
  _current_pending_monitor_is_from_java(true),
  _current_waiting_monitor(NULL),
  _Stalled(0),

  _monitor_chunks(nullptr),

  _suspend_flags(0),
  _async_exception_condition(_no_async_condition),
  _pending_async_exception(nullptr),

  _thread_state(_thread_new),
  _saved_exception_pc(nullptr),
#ifdef ASSERT
  _no_safepoint_count(0),
  _visited_for_critical_count(false),
#endif

  _terminated(_not_terminated),
  _in_deopt_handler(0),
  _doing_unsafe_access(false),
  _do_not_unlock_if_synchronized(false),
  _jni_attach_state(_not_attaching_via_jni),
#if INCLUDE_JVMCI
  _pending_deoptimization(-1),
  _pending_monitorenter(false),
  _pending_transfer_to_interpreter(false),
  _in_retryable_allocation(false),
  _pending_failed_speculation(0),
  _jvmci{nullptr},
  _jvmci_counters(nullptr),
  _jvmci_reserved0(nullptr),
  _jvmci_reserved1(nullptr),
  _jvmci_reserved_oop0(nullptr),
#endif // INCLUDE_JVMCI

  _exception_oop(oop()),
  _exception_pc(0),
  _exception_handler_pc(0),
  _is_method_handle_return(0),

  _jni_active_critical(0),
  _pending_jni_exception_check_fn(nullptr),
  _depth_first_number(0),

  // JVMTI PopFrame support
  _popframe_condition(popframe_inactive),
  _frames_to_pop_failed_realloc(0),

  _handshake(this),

  _popframe_preserved_args(nullptr),
  _popframe_preserved_args_size(0),

  _jvmti_thread_state(nullptr),
  _interp_only_mode(0),
  _should_post_on_exceptions_flag(JNI_FALSE),
  _thread_stat(new ThreadStatistics()),

  _parker(),

  _class_to_be_initialized(nullptr),

  _SleepEvent(ParkEvent::Allocate(this))
{
  set_jni_functions(jni_functions());

#if INCLUDE_JVMCI
  assert(_jvmci._implicit_exception_pc == nullptr, "must be");
  if (JVMCICounterSize > 0) {
    resize_counters(0, (int) JVMCICounterSize);
  }
#endif // INCLUDE_JVMCI

  // Setup safepoint state info for this thread
  ThreadSafepointState::create(this);

  SafepointMechanism::initialize_header(this);

  set_requires_cross_modify_fence(false);

  pd_initialize();
  assert(deferred_card_mark().is_empty(), "Default MemRegion ctor");
}

JavaThread::JavaThread(bool is_attaching_via_jni) : JavaThread() {
  if (is_attaching_via_jni) {
    _jni_attach_state = _attaching_via_jni;
  }
}


// interrupt support

void JavaThread::interrupt() {
  // All callers should have 'this' thread protected by a
  // ThreadsListHandle so that it cannot terminate and deallocate
  // itself.
  debug_only(check_for_dangling_thread_pointer(this);)

  // For Windows _interrupt_event
  osthread()->set_interrupted(true);

  // For Thread.sleep
  _SleepEvent->unpark();

  // For JSR166 LockSupport.park
  parker()->unpark();

  // For ObjectMonitor and JvmtiRawMonitor
  _ParkEvent->unpark();
}


bool JavaThread::is_interrupted(bool clear_interrupted) {
  debug_only(check_for_dangling_thread_pointer(this);)

  if (_threadObj.peek() == NULL) {
    // If there is no j.l.Thread then it is impossible to have
    // been interrupted. We can find NULL during VM initialization
    // or when a JNI thread is still in the process of attaching.
    // In such cases this must be the current thread.
    assert(this == Thread::current(), "invariant");
    return false;
  }

  bool interrupted = java_lang_Thread::interrupted(threadObj());

  // NOTE that since there is no "lock" around the interrupt and
  // is_interrupted operations, there is the possibility that the
  // interrupted flag will be "false" but that the
  // low-level events will be in the signaled state. This is
  // intentional. The effect of this is that Object.wait() and
  // LockSupport.park() will appear to have a spurious wakeup, which
  // is allowed and not harmful, and the possibility is so rare that
  // it is not worth the added complexity to add yet another lock.
  // For the sleep event an explicit reset is performed on entry
  // to JavaThread::sleep, so there is no early return. It has also been
  // recommended not to put the interrupted flag into the "event"
  // structure because it hides the issue.
  // Also, because there is no lock, we must only clear the interrupt
  // state if we are going to report that we were interrupted; otherwise
  // an interrupt that happens just after we read the field would be lost.
  if (interrupted && clear_interrupted) {
    assert(this == Thread::current(), "only the current thread can clear");
    java_lang_Thread::set_interrupted(threadObj(), false);
    osthread()->set_interrupted(false);
  }

  return interrupted;
}

void JavaThread::block_if_vm_exited() {
  if (_terminated == _vm_exited) {
    // _vm_exited is set at safepoint, and Threads_lock is never released
    // we will block here forever.
    // Here we can be doing a jump from a safe state to an unsafe state without
    // proper transition, but it happens after the final safepoint has begun.
    set_thread_state(_thread_in_vm);
    Threads_lock->lock();
    ShouldNotReachHere();
  }
}

JavaThread::JavaThread(ThreadFunction entry_point, size_t stack_sz) : JavaThread() {
  _jni_attach_state = _not_attaching_via_jni;
  set_entry_point(entry_point);
  // Create the native thread itself.
  // %note runtime_23
  os::ThreadType thr_type = os::java_thread;
  thr_type = entry_point == &CompilerThread::thread_entry ? os::compiler_thread :
                                                            os::java_thread;
  os::create_thread(this, thr_type, stack_sz);
  // The _osthread may be NULL here because we ran out of memory (too many threads active).
  // We need to throw and OutOfMemoryError - however we cannot do this here because the caller
  // may hold a lock and all locks must be unlocked before throwing the exception (throwing
  // the exception consists of creating the exception object & initializing it, initialization
  // will leave the VM via a JavaCall and then all locks must be unlocked).
  //
  // The thread is still suspended when we reach here. Thread must be explicit started
  // by creator! Furthermore, the thread must also explicitly be added to the Threads list
  // by calling Threads:add. The reason why this is not done here, is because the thread
  // object must be fully initialized (take a look at JVM_Start)
}

JavaThread::~JavaThread() {

  // Ask ServiceThread to release the threadObj OopHandle
  ServiceThread::add_oop_handle_release(_threadObj);

  // Return the sleep event to the free list
  ParkEvent::Release(_SleepEvent);
  _SleepEvent = NULL;

  // Free any remaining  previous UnrollBlock
  vframeArray* old_array = vframe_array_last();

  if (old_array != NULL) {
    Deoptimization::UnrollBlock* old_info = old_array->unroll_block();
    old_array->set_unroll_block(NULL);
    delete old_info;
    delete old_array;
  }

  JvmtiDeferredUpdates* updates = deferred_updates();
  if (updates != NULL) {
    // This can only happen if thread is destroyed before deoptimization occurs.
    assert(updates->count() > 0, "Updates holder not deleted");
    // free deferred updates.
    delete updates;
    set_deferred_updates(NULL);
  }

  // All Java related clean up happens in exit
  ThreadSafepointState::destroy(this);
  if (_thread_stat != NULL) delete _thread_stat;

#if INCLUDE_JVMCI
  if (JVMCICounterSize > 0) {
    FREE_C_HEAP_ARRAY(jlong, _jvmci_counters);
  }
#endif // INCLUDE_JVMCI
}


// First JavaThread specific code executed by a new Java thread.
void JavaThread::pre_run() {
  // empty - see comments in run()
}

// The main routine called by a new Java thread. This isn't overridden
// by subclasses, instead different subclasses define a different "entry_point"
// which defines the actual logic for that kind of thread.
void JavaThread::run() {
  // initialize thread-local alloc buffer related fields
  initialize_tlab();

  _stack_overflow_state.create_stack_guard_pages();

  cache_global_variables();

  // Thread is now sufficiently initialized to be handled by the safepoint code as being
  // in the VM. Change thread state from _thread_new to _thread_in_vm
  ThreadStateTransition::transition(this, _thread_new, _thread_in_vm);
  // Before a thread is on the threads list it is always safe, so after leaving the
  // _thread_new we should emit a instruction barrier. The distance to modified code
  // from here is probably far enough, but this is consistent and safe.
  OrderAccess::cross_modify_fence();

  assert(JavaThread::current() == this, "sanity check");
  assert(!Thread::current()->owns_locks(), "sanity check");

  DTRACE_THREAD_PROBE(start, this);

  // This operation might block. We call that after all safepoint checks for a new thread has
  // been completed.
  set_active_handles(JNIHandleBlock::allocate_block());

  if (JvmtiExport::should_post_thread_life()) {
    JvmtiExport::post_thread_start(this);

  }

  // We call another function to do the rest so we are sure that the stack addresses used
  // from there will be lower than the stack base just computed.
  thread_main_inner();
}

void JavaThread::thread_main_inner() {
  assert(JavaThread::current() == this, "sanity check");
  assert(_threadObj.peek() != NULL, "just checking");

  // Execute thread entry point unless this thread has a pending exception
  // or has been stopped before starting.
  // Note: Due to JVM_StopThread we can have pending exceptions already!
  if (!this->has_pending_exception() &&
      !java_lang_Thread::is_stillborn(this->threadObj())) {
    {
      ResourceMark rm(this);
      this->set_native_thread_name(this->name());
    }
    HandleMark hm(this);
    this->entry_point()(this, this);
  }

  DTRACE_THREAD_PROBE(stop, this);

  // Cleanup is handled in post_run()
}

// Shared teardown for all JavaThreads
void JavaThread::post_run() {
  this->exit(false);
  this->unregister_thread_stack_with_NMT();
  // Defer deletion to here to ensure 'this' is still referenceable in call_run
  // for any shared tear-down.
  this->smr_delete();
}

static void ensure_join(JavaThread* thread) {
  // We do not need to grab the Threads_lock, since we are operating on ourself.
  Handle threadObj(thread, thread->threadObj());
  assert(threadObj.not_null(), "java thread object must exist");
  ObjectLocker lock(threadObj, thread);
  // Ignore pending exception (ThreadDeath), since we are exiting anyway
  thread->clear_pending_exception();
  // Thread is exiting. So set thread_status field in  java.lang.Thread class to TERMINATED.
  java_lang_Thread::set_thread_status(threadObj(), JavaThreadStatus::TERMINATED);
  // Clear the native thread instance - this makes isAlive return false and allows the join()
  // to complete once we've done the notify_all below
  java_lang_Thread::set_thread(threadObj(), NULL);
  lock.notify_all(thread);
  // Ignore pending exception (ThreadDeath), since we are exiting anyway
  thread->clear_pending_exception();
}

static bool is_daemon(oop threadObj) {
  return (threadObj != NULL && java_lang_Thread::is_daemon(threadObj));
}

// For any new cleanup additions, please check to see if they need to be applied to
// cleanup_failed_attach_current_thread as well.
void JavaThread::exit(bool destroy_vm, ExitType exit_type) {
  assert(this == JavaThread::current(), "thread consistency check");

  elapsedTimer _timer_exit_phase1;
  elapsedTimer _timer_exit_phase2;
  elapsedTimer _timer_exit_phase3;
  elapsedTimer _timer_exit_phase4;

  if (log_is_enabled(Debug, os, thread, timer)) {
    _timer_exit_phase1.start();
  }

  HandleMark hm(this);
  Handle uncaught_exception(this, this->pending_exception());
  this->clear_pending_exception();
  Handle threadObj(this, this->threadObj());
  assert(threadObj.not_null(), "Java thread object should be created");

  if (!destroy_vm) {
    if (uncaught_exception.not_null()) {
      EXCEPTION_MARK;
      // Call method Thread.dispatchUncaughtException().
      Klass* thread_klass = vmClasses::Thread_klass();
      JavaValue result(T_VOID);
      JavaCalls::call_virtual(&result,
                              threadObj, thread_klass,
                              vmSymbols::dispatchUncaughtException_name(),
                              vmSymbols::throwable_void_signature(),
                              uncaught_exception,
                              THREAD);
      if (HAS_PENDING_EXCEPTION) {
        ResourceMark rm(this);
        jio_fprintf(defaultStream::error_stream(),
                    "\nException: %s thrown from the UncaughtExceptionHandler"
                    " in thread \"%s\"\n",
                    pending_exception()->klass()->external_name(),
                    name());
        CLEAR_PENDING_EXCEPTION;
      }
    }

    // Call Thread.exit(). We try 3 times in case we got another Thread.stop during
    // the execution of the method. If that is not enough, then we don't really care. Thread.stop
    // is deprecated anyhow.
    if (!is_Compiler_thread()) {
      int count = 3;
      while (java_lang_Thread::threadGroup(threadObj()) != NULL && (count-- > 0)) {
        EXCEPTION_MARK;
        JavaValue result(T_VOID);
        Klass* thread_klass = vmClasses::Thread_klass();
        JavaCalls::call_virtual(&result,
                                threadObj, thread_klass,
                                vmSymbols::exit_method_name(),
                                vmSymbols::void_method_signature(),
                                THREAD);
        CLEAR_PENDING_EXCEPTION;
      }
    }
    // notify JVMTI
    if (JvmtiExport::should_post_thread_life()) {
      JvmtiExport::post_thread_end(this);
    }

    // The careful dance between thread suspension and exit is handled here.
    // Since we are in thread_in_vm state and suspension is done with handshakes,
    // we can just put in the exiting state and it will be correctly handled.
    set_terminated(_thread_exiting);

    ThreadService::current_thread_exiting(this, is_daemon(threadObj()));
  } else {
    assert(!is_terminated() && !is_exiting(), "must not be exiting");
    // before_exit() has already posted JVMTI THREAD_END events
  }

  if (log_is_enabled(Debug, os, thread, timer)) {
    _timer_exit_phase1.stop();
    _timer_exit_phase2.start();
  }

  // Capture daemon status before the thread is marked as terminated.
  bool daemon = is_daemon(threadObj());

  // Notify waiters on thread object. This has to be done after exit() is called
  // on the thread (if the thread is the last thread in a daemon ThreadGroup the
  // group should have the destroyed bit set before waiters are notified).
  ensure_join(this);
  assert(!this->has_pending_exception(), "ensure_join should have cleared");

  if (log_is_enabled(Debug, os, thread, timer)) {
    _timer_exit_phase2.stop();
    _timer_exit_phase3.start();
  }
  // 6282335 JNI DetachCurrentThread spec states that all Java monitors
  // held by this thread must be released. The spec does not distinguish
  // between JNI-acquired and regular Java monitors. We can only see
  // regular Java monitors here if monitor enter-exit matching is broken.
  //
  // ensure_join() ignores IllegalThreadStateExceptions, and so does
  // ObjectSynchronizer::release_monitors_owned_by_thread().
  if (exit_type == jni_detach) {
    // Sanity check even though JNI DetachCurrentThread() would have
    // returned JNI_ERR if there was a Java frame. JavaThread exit
    // should be done executing Java code by the time we get here.
    assert(!this->has_last_Java_frame(),
           "should not have a Java frame when detaching or exiting");
    ObjectSynchronizer::release_monitors_owned_by_thread(this);
    assert(!this->has_pending_exception(), "release_monitors should have cleared");
  }

  // These things needs to be done while we are still a Java Thread. Make sure that thread
  // is in a consistent state, in case GC happens
  JFR_ONLY(Jfr::on_thread_exit(this);)

  if (active_handles() != NULL) {
    JNIHandleBlock* block = active_handles();
    set_active_handles(NULL);
    JNIHandleBlock::release_block(block);
  }

  if (free_handle_block() != NULL) {
    JNIHandleBlock* block = free_handle_block();
    set_free_handle_block(NULL);
    JNIHandleBlock::release_block(block);
  }

  // These have to be removed while this is still a valid thread.
  _stack_overflow_state.remove_stack_guard_pages();

  if (UseTLAB) {
    tlab().retire();
  }

  if (JvmtiEnv::environments_might_exist()) {
    JvmtiExport::cleanup_thread(this);
  }

  // We need to cache the thread name for logging purposes below as once
  // we have called on_thread_detach this thread must not access any oops.
  char* thread_name = NULL;
  if (log_is_enabled(Debug, os, thread, timer)) {
    ResourceMark rm(this);
    thread_name = os::strdup(name());
  }

  log_info(os, thread)("JavaThread %s (tid: " UINTX_FORMAT ").",
    exit_type == JavaThread::normal_exit ? "exiting" : "detaching",
    os::current_thread_id());

  if (log_is_enabled(Debug, os, thread, timer)) {
    _timer_exit_phase3.stop();
    _timer_exit_phase4.start();
  }

#if INCLUDE_JVMCI
  if (JVMCICounterSize > 0) {
    if (jvmci_counters_include(this)) {
      for (int i = 0; i < JVMCICounterSize; i++) {
        _jvmci_old_thread_counters[i] += _jvmci_counters[i];
      }
    }
  }
#endif // INCLUDE_JVMCI

  // Remove from list of active threads list, and notify VM thread if we are the last non-daemon thread
  Threads::remove(this, daemon);

  if (log_is_enabled(Debug, os, thread, timer)) {
    _timer_exit_phase4.stop();
    log_debug(os, thread, timer)("name='%s'"
                                 ", exit-phase1=" JLONG_FORMAT
                                 ", exit-phase2=" JLONG_FORMAT
                                 ", exit-phase3=" JLONG_FORMAT
                                 ", exit-phase4=" JLONG_FORMAT,
                                 thread_name,
                                 _timer_exit_phase1.milliseconds(),
                                 _timer_exit_phase2.milliseconds(),
                                 _timer_exit_phase3.milliseconds(),
                                 _timer_exit_phase4.milliseconds());
    os::free(thread_name);
  }
}

void JavaThread::cleanup_failed_attach_current_thread(bool is_daemon) {
  if (active_handles() != NULL) {
    JNIHandleBlock* block = active_handles();
    set_active_handles(NULL);
    JNIHandleBlock::release_block(block);
  }

  if (free_handle_block() != NULL) {
    JNIHandleBlock* block = free_handle_block();
    set_free_handle_block(NULL);
    JNIHandleBlock::release_block(block);
  }

  // These have to be removed while this is still a valid thread.
  _stack_overflow_state.remove_stack_guard_pages();

  if (UseTLAB) {
    tlab().retire();
  }

  Threads::remove(this, is_daemon);
  this->smr_delete();
}

JavaThread* JavaThread::active() {
  Thread* thread = Thread::current();
  if (thread->is_Java_thread()) {
    return JavaThread::cast(thread);
  } else {
    assert(thread->is_VM_thread(), "this must be a vm thread");
    VM_Operation* op = ((VMThread*) thread)->vm_operation();
    JavaThread *ret = op == NULL ? NULL : JavaThread::cast(op->calling_thread());
    return ret;
  }
}

bool JavaThread::is_lock_owned(address adr) const {
  if (Thread::is_lock_owned(adr)) return true;

  for (MonitorChunk* chunk = monitor_chunks(); chunk != NULL; chunk = chunk->next()) {
    if (chunk->contains(adr)) return true;
  }

  return false;
}

oop JavaThread::exception_oop() const {
  return Atomic::load(&_exception_oop);
}

void JavaThread::set_exception_oop(oop o) {
  Atomic::store(&_exception_oop, o);
}

void JavaThread::add_monitor_chunk(MonitorChunk* chunk) {
  chunk->set_next(monitor_chunks());
  set_monitor_chunks(chunk);
}

void JavaThread::remove_monitor_chunk(MonitorChunk* chunk) {
  guarantee(monitor_chunks() != NULL, "must be non empty");
  if (monitor_chunks() == chunk) {
    set_monitor_chunks(chunk->next());
  } else {
    MonitorChunk* prev = monitor_chunks();
    while (prev->next() != chunk) prev = prev->next();
    prev->set_next(chunk->next());
  }
}


// Asynchronous exceptions support
//
// Note: this function shouldn't block if it's called in
// _thread_in_native_trans state (such as from
// check_special_condition_for_native_trans()).
void JavaThread::check_and_handle_async_exceptions() {
  if (has_last_Java_frame() && has_async_exception_condition()) {
    // If we are at a polling page safepoint (not a poll return)
    // then we must defer async exception because live registers
    // will be clobbered by the exception path. Poll return is
    // ok because the call we a returning from already collides
    // with exception handling registers and so there is no issue.
    // (The exception handling path kills call result registers but
    //  this is ok since the exception kills the result anyway).

    if (is_at_poll_safepoint()) {
      // if the code we are returning to has deoptimized we must defer
      // the exception otherwise live registers get clobbered on the
      // exception path before deoptimization is able to retrieve them.
      //
      RegisterMap map(this, false);
      frame caller_fr = last_frame().sender(&map);
      assert(caller_fr.is_compiled_frame(), "what?");
      if (caller_fr.is_deoptimized_frame()) {
        log_info(exceptions)("deferred async exception at compiled safepoint");
        return;
      }
    }
  }

  AsyncExceptionCondition condition = clear_async_exception_condition();
  if (condition == _no_async_condition) {
    // Conditions have changed since has_special_runtime_exit_condition()
    // was called:
    // - if we were here only because of an external suspend request,
    //   then that was taken care of above (or cancelled) so we are done
    // - if we were here because of another async request, then it has
    //   been cleared between the has_special_runtime_exit_condition()
    //   and now so again we are done
    return;
  }

  // Check for pending async. exception
  if (_pending_async_exception != NULL) {
    // Only overwrite an already pending exception, if it is not a threadDeath.
    if (!has_pending_exception() || !pending_exception()->is_a(vmClasses::ThreadDeath_klass())) {

      // We cannot call Exceptions::_throw(...) here because we cannot block
      set_pending_exception(_pending_async_exception, __FILE__, __LINE__);

      LogTarget(Info, exceptions) lt;
      if (lt.is_enabled()) {
        ResourceMark rm;
        LogStream ls(lt);
        ls.print("Async. exception installed at runtime exit (" INTPTR_FORMAT ")", p2i(this));
          if (has_last_Java_frame()) {
            frame f = last_frame();
           ls.print(" (pc: " INTPTR_FORMAT " sp: " INTPTR_FORMAT " )", p2i(f.pc()), p2i(f.sp()));
          }
        ls.print_cr(" of type: %s", _pending_async_exception->klass()->external_name());
      }
      _pending_async_exception = NULL;
      // Clear condition from _suspend_flags since we have finished processing it.
      clear_suspend_flag(_has_async_exception);
    }
  }

  if (condition == _async_unsafe_access_error && !has_pending_exception()) {
    // We may be at method entry which requires we save the do-not-unlock flag.
    UnlockFlagSaver fs(this);
    switch (thread_state()) {
    case _thread_in_vm: {
      JavaThread* THREAD = this;
      Exceptions::throw_unsafe_access_internal_error(THREAD, __FILE__, __LINE__, "a fault occurred in an unsafe memory access operation");
      return;
    }
    case _thread_in_native: {
      ThreadInVMfromNative tiv(this);
      JavaThread* THREAD = this;
      Exceptions::throw_unsafe_access_internal_error(THREAD, __FILE__, __LINE__, "a fault occurred in an unsafe memory access operation");
      return;
    }
    case _thread_in_Java: {
      ThreadInVMfromJava tiv(this);
      JavaThread* THREAD = this;
      Exceptions::throw_unsafe_access_internal_error(THREAD, __FILE__, __LINE__, "a fault occurred in a recent unsafe memory access operation in compiled Java code");
      return;
    }
    default:
      ShouldNotReachHere();
    }
  }

  assert(has_pending_exception(), "must have handled the async condition if no exception");
}

void JavaThread::handle_special_runtime_exit_condition(bool check_asyncs) {

  if (is_obj_deopt_suspend()) {
    frame_anchor()->make_walkable(this);
    wait_for_object_deoptimization();
  }

  // We might be here for reasons in addition to the self-suspend request
  // so check for other async requests.
  if (check_asyncs) {
    check_and_handle_async_exceptions();
  }

  JFR_ONLY(SUSPEND_THREAD_CONDITIONAL(this);)
}

class InstallAsyncExceptionClosure : public HandshakeClosure {
  Handle _throwable; // The Throwable thrown at the target Thread
public:
  InstallAsyncExceptionClosure(Handle throwable) : HandshakeClosure("InstallAsyncException"), _throwable(throwable) {}

  void do_thread(Thread* thr) {
    JavaThread* target = JavaThread::cast(thr);
    // Note that this now allows multiple ThreadDeath exceptions to be
    // thrown at a thread.
    // The target thread has run and has not exited yet.
    target->send_thread_stop(_throwable());
  }
};

void JavaThread::send_async_exception(oop java_thread, oop java_throwable) {
  Handle throwable(Thread::current(), java_throwable);
  JavaThread* target = java_lang_Thread::thread(java_thread);
  InstallAsyncExceptionClosure vm_stop(throwable);
  Handshake::execute(&vm_stop, target);
}

void JavaThread::send_thread_stop(oop java_throwable)  {
  ResourceMark rm;
  assert(is_handshake_safe_for(Thread::current()),
         "should be self or handshakee");

  // Do not throw asynchronous exceptions against the compiler thread
  // (the compiler thread should not be a Java thread -- fix in 1.4.2)
  if (!can_call_java()) return;

  {
    // Actually throw the Throwable against the target Thread - however
    // only if there is no thread death exception installed already.
    if (_pending_async_exception == NULL || !_pending_async_exception->is_a(vmClasses::ThreadDeath_klass())) {
      // If the topmost frame is a runtime stub, then we are calling into
      // OptoRuntime from compiled code. Some runtime stubs (new, monitor_exit..)
      // must deoptimize the caller before continuing, as the compiled  exception handler table
      // may not be valid
      if (has_last_Java_frame()) {
        frame f = last_frame();
        if (f.is_runtime_frame() || f.is_safepoint_blob_frame()) {
          RegisterMap reg_map(this, false);
          frame compiled_frame = f.sender(&reg_map);
          if (!StressCompiledExceptionHandlers && compiled_frame.can_be_deoptimized()) {
            Deoptimization::deoptimize(this, compiled_frame);
          }
        }
      }

      // Set async. pending exception in thread.
      set_pending_async_exception(java_throwable);

      if (log_is_enabled(Info, exceptions)) {
         ResourceMark rm;
        log_info(exceptions)("Pending Async. exception installed of type: %s",
                             InstanceKlass::cast(_pending_async_exception->klass())->external_name());
      }
      // for AbortVMOnException flag
      Exceptions::debug_check_abort(_pending_async_exception->klass()->external_name());
    }
  }


  // Interrupt thread so it will wake up from a potential wait()/sleep()/park()
  java_lang_Thread::set_interrupted(threadObj(), true);
  this->interrupt();
}


// External suspension mechanism.
//
// Guarantees on return (for a valid target thread):
//   - Target thread will not execute any new bytecode.
//   - Target thread will not enter any new monitors.
//
bool JavaThread::java_suspend() {
  ThreadsListHandle tlh;
  if (!tlh.includes(this)) {
    log_trace(thread, suspend)("JavaThread:" INTPTR_FORMAT " not on ThreadsList, no suspension", p2i(this));
    return false;
  }
  return this->handshake_state()->suspend();
}

bool JavaThread::java_resume() {
  ThreadsListHandle tlh;
  if (!tlh.includes(this)) {
    log_trace(thread, suspend)("JavaThread:" INTPTR_FORMAT " not on ThreadsList, nothing to resume", p2i(this));
    return false;
  }
  return this->handshake_state()->resume();
}

// Wait for another thread to perform object reallocation and relocking on behalf of
// this thread.
// Raw thread state transition to _thread_blocked and back again to the original
// state before returning are performed. The current thread is required to
// change to _thread_blocked in order to be seen to be safepoint/handshake safe
// whilst suspended and only after becoming handshake safe, the other thread can
// complete the handshake used to synchronize with this thread and then perform
// the reallocation and relocking. We cannot use the thread state transition
// helpers because we arrive here in various states and also because the helpers
// indirectly call this method.  After leaving _thread_blocked we have to check
// for safepoint/handshake, except if _thread_in_native. The thread is safe
// without blocking then. Allowed states are enumerated in
// SafepointSynchronize::block(). See also EscapeBarrier::sync_and_suspend_*()

void JavaThread::wait_for_object_deoptimization() {
  assert(!has_last_Java_frame() || frame_anchor()->walkable(), "should have walkable stack");
  assert(this == Thread::current(), "invariant");
  JavaThreadState state = thread_state();

  bool spin_wait = os::is_MP();
  do {
    set_thread_state(_thread_blocked);
    // Wait for object deoptimization if requested.
    if (spin_wait) {
      // A single deoptimization is typically very short. Microbenchmarks
      // showed 5% better performance when spinning.
      const uint spin_limit = 10 * SpinYield::default_spin_limit;
      SpinYield spin(spin_limit);
      for (uint i = 0; is_obj_deopt_suspend() && i < spin_limit; i++) {
        spin.wait();
      }
      // Spin just once
      spin_wait = false;
    } else {
      MonitorLocker ml(this, EscapeBarrier_lock, Monitor::_no_safepoint_check_flag);
      if (is_obj_deopt_suspend()) {
        ml.wait();
      }
    }
    // The current thread could have been suspended again. We have to check for
    // suspend after restoring the saved state. Without this the current thread
    // might return to _thread_in_Java and execute bytecode.
    set_thread_state_fence(state);

    if (state != _thread_in_native) {
      SafepointMechanism::process_if_requested(this);
    }
    // A handshake for obj. deoptimization suspend could have been processed so
    // we must check after processing.
  } while (is_obj_deopt_suspend());
}

#ifdef ASSERT
// Verify the JavaThread has not yet been published in the Threads::list, and
// hence doesn't need protection from concurrent access at this stage.
void JavaThread::verify_not_published() {
  // Cannot create a ThreadsListHandle here and check !tlh.includes(this)
  // since an unpublished JavaThread doesn't participate in the
  // Thread-SMR protocol for keeping a ThreadsList alive.
  assert(!on_thread_list(), "JavaThread shouldn't have been published yet!");
}
#endif

// Slow path when the native==>Java barriers detect a safepoint/handshake is
// pending, when _suspend_flags is non-zero or when we need to process a stack
// watermark. Also check for pending async exceptions (except unsafe access error).
// Note only the native==>Java barriers can call this function when thread state
// is _thread_in_native_trans.
void JavaThread::check_special_condition_for_native_trans(JavaThread *thread) {
  assert(thread->thread_state() == _thread_in_native_trans, "wrong state");
  assert(!thread->has_last_Java_frame() || thread->frame_anchor()->walkable(), "Unwalkable stack in native->Java transition");

  // Enable WXWrite: called directly from interpreter native wrapper.
  MACOS_AARCH64_ONLY(ThreadWXEnable wx(WXWrite, thread));

  SafepointMechanism::process_if_requested_with_exit_check(thread, false /* check asyncs */);

  // After returning from native, it could be that the stack frames are not
  // yet safe to use. We catch such situations in the subsequent stack watermark
  // barrier, which will trap unsafe stack frames.
  StackWatermarkSet::before_unwind(thread);

  if (thread->has_async_exception_condition(false /* check unsafe access error */)) {
    // We are in _thread_in_native_trans state, don't handle unsafe
    // access error since that may block.
    thread->check_and_handle_async_exceptions();
  }
}

#ifndef PRODUCT
// Deoptimization
// Function for testing deoptimization
void JavaThread::deoptimize() {
  StackFrameStream fst(this, false /* update */, true /* process_frames */);
  bool deopt = false;           // Dump stack only if a deopt actually happens.
  bool only_at = strlen(DeoptimizeOnlyAt) > 0;
  // Iterate over all frames in the thread and deoptimize
  for (; !fst.is_done(); fst.next()) {
    if (fst.current()->can_be_deoptimized()) {

      if (only_at) {
        // Deoptimize only at particular bcis.  DeoptimizeOnlyAt
        // consists of comma or carriage return separated numbers so
        // search for the current bci in that string.
        address pc = fst.current()->pc();
        nmethod* nm =  (nmethod*) fst.current()->cb();
        ScopeDesc* sd = nm->scope_desc_at(pc);
        char buffer[8];
        jio_snprintf(buffer, sizeof(buffer), "%d", sd->bci());
        size_t len = strlen(buffer);
        const char * found = strstr(DeoptimizeOnlyAt, buffer);
        while (found != NULL) {
          if ((found[len] == ',' || found[len] == '\n' || found[len] == '\0') &&
              (found == DeoptimizeOnlyAt || found[-1] == ',' || found[-1] == '\n')) {
            // Check that the bci found is bracketed by terminators.
            break;
          }
          found = strstr(found + 1, buffer);
        }
        if (!found) {
          continue;
        }
      }

      if (DebugDeoptimization && !deopt) {
        deopt = true; // One-time only print before deopt
        tty->print_cr("[BEFORE Deoptimization]");
        trace_frames();
        trace_stack();
      }
      Deoptimization::deoptimize(this, *fst.current());
    }
  }

  if (DebugDeoptimization && deopt) {
    tty->print_cr("[AFTER Deoptimization]");
    trace_frames();
  }
}


// Make zombies
void JavaThread::make_zombies() {
  for (StackFrameStream fst(this, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
    if (fst.current()->can_be_deoptimized()) {
      // it is a Java nmethod
      nmethod* nm = CodeCache::find_nmethod(fst.current()->pc());
      nm->make_not_entrant();
    }
  }
}
#endif // PRODUCT


void JavaThread::deoptimize_marked_methods() {
  if (!has_last_Java_frame()) return;
  StackFrameStream fst(this, false /* update */, true /* process_frames */);
  for (; !fst.is_done(); fst.next()) {
    if (fst.current()->should_be_deoptimized()) {
      Deoptimization::deoptimize(this, *fst.current());
    }
  }
}

#ifdef ASSERT
void JavaThread::verify_frame_info() {
  assert((!has_last_Java_frame() && java_call_counter() == 0) ||
         (has_last_Java_frame() && java_call_counter() > 0),
         "unexpected frame info: has_last_frame=%s, java_call_counter=%d",
         has_last_Java_frame() ? "true" : "false", java_call_counter());
}
#endif

void JavaThread::oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf) {
  // Verify that the deferred card marks have been flushed.
  assert(deferred_card_mark().is_empty(), "Should be empty during GC");

  // Traverse the GCHandles
  Thread::oops_do_no_frames(f, cf);

  DEBUG_ONLY(verify_frame_info();)

  if (has_last_Java_frame()) {
    // Traverse the monitor chunks
    for (MonitorChunk* chunk = monitor_chunks(); chunk != NULL; chunk = chunk->next()) {
      chunk->oops_do(f);
    }
  }

  assert(vframe_array_head() == NULL, "deopt in progress at a safepoint!");
  // If we have deferred set_locals there might be oops waiting to be
  // written
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list = JvmtiDeferredUpdates::deferred_locals(this);
  if (list != NULL) {
    for (int i = 0; i < list->length(); i++) {
      list->at(i)->oops_do(f);
    }
  }

  // Traverse instance variables at the end since the GC may be moving things
  // around using this function
  f->do_oop((oop*) &_vm_result);
  f->do_oop((oop*) &_exception_oop);
  f->do_oop((oop*) &_pending_async_exception);
#if INCLUDE_JVMCI
  f->do_oop((oop*) &_jvmci_reserved_oop0);
#endif

  if (jvmti_thread_state() != NULL) {
    jvmti_thread_state()->oops_do(f, cf);
  }
}

void JavaThread::oops_do_frames(OopClosure* f, CodeBlobClosure* cf) {
  if (!has_last_Java_frame()) {
    return;
  }
  // Finish any pending lazy GC activity for the frames
  StackWatermarkSet::finish_processing(this, NULL /* context */, StackWatermarkKind::gc);
  // Traverse the execution stack
  for (StackFrameStream fst(this, true /* update */, false /* process_frames */); !fst.is_done(); fst.next()) {
    fst.current()->oops_do(f, cf, fst.register_map());
  }
}

#ifdef ASSERT
void JavaThread::verify_states_for_handshake() {
  // This checks that the thread has a correct frame state during a handshake.
  verify_frame_info();
}
#endif

void JavaThread::nmethods_do(CodeBlobClosure* cf) {
  DEBUG_ONLY(verify_frame_info();)

  if (has_last_Java_frame()) {
    // Traverse the execution stack
    for (StackFrameStream fst(this, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
      fst.current()->nmethods_do(cf);
    }
  }

  if (jvmti_thread_state() != NULL) {
    jvmti_thread_state()->nmethods_do(cf);
  }
}

void JavaThread::metadata_do(MetadataClosure* f) {
  if (has_last_Java_frame()) {
    // Traverse the execution stack to call f() on the methods in the stack
    for (StackFrameStream fst(this, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
      fst.current()->metadata_do(f);
    }
  } else if (is_Compiler_thread()) {
    // need to walk ciMetadata in current compile tasks to keep alive.
    CompilerThread* ct = (CompilerThread*)this;
    if (ct->env() != NULL) {
      ct->env()->metadata_do(f);
    }
    CompileTask* task = ct->task();
    if (task != NULL) {
      task->metadata_do(f);
    }
  }
}

// Printing
const char* _get_thread_state_name(JavaThreadState _thread_state) {
  switch (_thread_state) {
  case _thread_uninitialized:     return "_thread_uninitialized";
  case _thread_new:               return "_thread_new";
  case _thread_new_trans:         return "_thread_new_trans";
  case _thread_in_native:         return "_thread_in_native";
  case _thread_in_native_trans:   return "_thread_in_native_trans";
  case _thread_in_vm:             return "_thread_in_vm";
  case _thread_in_vm_trans:       return "_thread_in_vm_trans";
  case _thread_in_Java:           return "_thread_in_Java";
  case _thread_in_Java_trans:     return "_thread_in_Java_trans";
  case _thread_blocked:           return "_thread_blocked";
  case _thread_blocked_trans:     return "_thread_blocked_trans";
  default:                        return "unknown thread state";
  }
}

#ifndef PRODUCT
void JavaThread::print_thread_state_on(outputStream *st) const {
  st->print_cr("   JavaThread state: %s", _get_thread_state_name(_thread_state));
};
#endif // PRODUCT

// Called by Threads::print() for VM_PrintThreads operation
void JavaThread::print_on(outputStream *st, bool print_extended_info) const {
  st->print_raw("\"");
  st->print_raw(name());
  st->print_raw("\" ");
  oop thread_oop = threadObj();
  if (thread_oop != NULL) {
    st->print("#" INT64_FORMAT " ", (int64_t)java_lang_Thread::thread_id(thread_oop));
    if (java_lang_Thread::is_daemon(thread_oop))  st->print("daemon ");
    st->print("prio=%d ", java_lang_Thread::priority(thread_oop));
  }
  Thread::print_on(st, print_extended_info);
  // print guess for valid stack memory region (assume 4K pages); helps lock debugging
  st->print_cr("[" INTPTR_FORMAT "]", (intptr_t)last_Java_sp() & ~right_n_bits(12));
  if (thread_oop != NULL) {
    st->print_cr("   java.lang.Thread.State: %s", java_lang_Thread::thread_status_name(thread_oop));
  }
#ifndef PRODUCT
  _safepoint_state->print_on(st);
#endif // PRODUCT
  if (is_Compiler_thread()) {
    CompileTask *task = ((CompilerThread*)this)->task();
    if (task != NULL) {
      st->print("   Compiling: ");
      task->print(st, NULL, true, false);
    } else {
      st->print("   No compile task");
    }
    st->cr();
  }
}

void JavaThread::print() const { print_on(tty); }

void JavaThread::print_name_on_error(outputStream* st, char *buf, int buflen) const {
  st->print("%s", get_thread_name_string(buf, buflen));
}

// Called by fatal error handler. The difference between this and
// JavaThread::print() is that we can't grab lock or allocate memory.
void JavaThread::print_on_error(outputStream* st, char *buf, int buflen) const {
  st->print("%s \"%s\"", type_name(), get_thread_name_string(buf, buflen));
  oop thread_obj = threadObj();
  if (thread_obj != NULL) {
    if (java_lang_Thread::is_daemon(thread_obj)) st->print(" daemon");
  }
  st->print(" [");
  st->print("%s", _get_thread_state_name(_thread_state));
  if (osthread()) {
    st->print(", id=%d", osthread()->thread_id());
  }
  st->print(", stack(" PTR_FORMAT "," PTR_FORMAT ")",
            p2i(stack_end()), p2i(stack_base()));
  st->print("]");

  ThreadsSMRSupport::print_info_on(this, st);
  return;
}


// Verification

void JavaThread::frames_do(void f(frame*, const RegisterMap* map)) {
  // ignore if there is no stack
  if (!has_last_Java_frame()) return;
  // traverse the stack frames. Starts from top frame.
  for (StackFrameStream fst(this, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
    frame* fr = fst.current();
    f(fr, fst.register_map());
  }
}

static void frame_verify(frame* f, const RegisterMap *map) { f->verify(map); }

void JavaThread::verify() {
  // Verify oops in the thread.
  oops_do(&VerifyOopClosure::verify_oop, NULL);

  // Verify the stack frames.
  frames_do(frame_verify);
}

// CR 6300358 (sub-CR 2137150)
// Most callers of this method assume that it can't return NULL but a
// thread may not have a name whilst it is in the process of attaching to
// the VM - see CR 6412693, and there are places where a JavaThread can be
// seen prior to having its threadObj set (e.g., JNI attaching threads and
// if vm exit occurs during initialization). These cases can all be accounted
// for such that this method never returns NULL.
const char* JavaThread::name() const  {
  if (Thread::is_JavaThread_protected(this)) {
    // The target JavaThread is protected so get_thread_name_string() is safe:
    return get_thread_name_string();
  }

  // The target JavaThread is not protected so we return the default:
  return Thread::name();
}

// Returns a non-NULL representation of this thread's name, or a suitable
// descriptive string if there is no set name.
const char* JavaThread::get_thread_name_string(char* buf, int buflen) const {
  const char* name_str;
  oop thread_obj = threadObj();
  if (thread_obj != NULL) {
    oop name = java_lang_Thread::name(thread_obj);
    if (name != NULL) {
      if (buf == NULL) {
        name_str = java_lang_String::as_utf8_string(name);
      } else {
        name_str = java_lang_String::as_utf8_string(name, buf, buflen);
      }
    } else if (is_attaching_via_jni()) { // workaround for 6412693 - see 6404306
      name_str = "<no-name - thread is attaching>";
    } else {
      name_str = "<un-named>";
    }
  } else {
    name_str = Thread::name();
  }
  assert(name_str != NULL, "unexpected NULL thread name");
  return name_str;
}

// Helper to extract the name from the thread oop for logging.
const char* JavaThread::name_for(oop thread_obj) {
  assert(thread_obj != NULL, "precondition");
  oop name = java_lang_Thread::name(thread_obj);
  const char* name_str;
  if (name != NULL) {
    name_str = java_lang_String::as_utf8_string(name);
  } else {
    name_str = "<un-named>";
  }
  return name_str;
}

void JavaThread::prepare(jobject jni_thread, ThreadPriority prio) {

  assert(Threads_lock->owner() == Thread::current(), "must have threads lock");
  assert(NoPriority <= prio && prio <= MaxPriority, "sanity check");
  // Link Java Thread object <-> C++ Thread

  // Get the C++ thread object (an oop) from the JNI handle (a jthread)
  // and put it into a new Handle.  The Handle "thread_oop" can then
  // be used to pass the C++ thread object to other methods.

  // Set the Java level thread object (jthread) field of the
  // new thread (a JavaThread *) to C++ thread object using the
  // "thread_oop" handle.

  // Set the thread field (a JavaThread *) of the
  // oop representing the java_lang_Thread to the new thread (a JavaThread *).

  Handle thread_oop(Thread::current(),
                    JNIHandles::resolve_non_null(jni_thread));
  assert(InstanceKlass::cast(thread_oop->klass())->is_linked(),
         "must be initialized");
  set_threadObj(thread_oop());
  java_lang_Thread::set_thread(thread_oop(), this);

  if (prio == NoPriority) {
    prio = java_lang_Thread::priority(thread_oop());
    assert(prio != NoPriority, "A valid priority should be present");
  }

  // Push the Java priority down to the native thread; needs Threads_lock
  Thread::set_priority(this, prio);

  // Add the new thread to the Threads list and set it in motion.
  // We must have threads lock in order to call Threads::add.
  // It is crucial that we do not block before the thread is
  // added to the Threads list for if a GC happens, then the java_thread oop
  // will not be visited by GC.
  Threads::add(this);
}

oop JavaThread::current_park_blocker() {
  // Support for JSR-166 locks
  oop thread_oop = threadObj();
  if (thread_oop != NULL) {
    return java_lang_Thread::park_blocker(thread_oop);
  }
  return NULL;
}


void JavaThread::print_stack_on(outputStream* st) {
  if (!has_last_Java_frame()) return;

  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);

  RegisterMap reg_map(this);
  vframe* start_vf = last_java_vframe(&reg_map);
  int count = 0;
  for (vframe* f = start_vf; f != NULL; f = f->sender()) {
    if (f->is_java_frame()) {
      javaVFrame* jvf = javaVFrame::cast(f);
      java_lang_Throwable::print_stack_element(st, jvf->method(), jvf->bci());

      // Print out lock information
      if (JavaMonitorsInStackTrace) {
        jvf->print_lock_info_on(st, count);
      }
    } else {
      // Ignore non-Java frames
    }

    // Bail-out case for too deep stacks if MaxJavaStackTraceDepth > 0
    count++;
    if (MaxJavaStackTraceDepth > 0 && MaxJavaStackTraceDepth == count) return;
  }
}


// JVMTI PopFrame support
void JavaThread::popframe_preserve_args(ByteSize size_in_bytes, void* start) {
  assert(_popframe_preserved_args == NULL, "should not wipe out old PopFrame preserved arguments");
  if (in_bytes(size_in_bytes) != 0) {
    _popframe_preserved_args = NEW_C_HEAP_ARRAY(char, in_bytes(size_in_bytes), mtThread);
    _popframe_preserved_args_size = in_bytes(size_in_bytes);
    Copy::conjoint_jbytes(start, _popframe_preserved_args, _popframe_preserved_args_size);
  }
}

void* JavaThread::popframe_preserved_args() {
  return _popframe_preserved_args;
}

ByteSize JavaThread::popframe_preserved_args_size() {
  return in_ByteSize(_popframe_preserved_args_size);
}

WordSize JavaThread::popframe_preserved_args_size_in_words() {
  int sz = in_bytes(popframe_preserved_args_size());
  assert(sz % wordSize == 0, "argument size must be multiple of wordSize");
  return in_WordSize(sz / wordSize);
}

void JavaThread::popframe_free_preserved_args() {
  assert(_popframe_preserved_args != NULL, "should not free PopFrame preserved arguments twice");
  FREE_C_HEAP_ARRAY(char, (char*)_popframe_preserved_args);
  _popframe_preserved_args = NULL;
  _popframe_preserved_args_size = 0;
}

#ifndef PRODUCT

void JavaThread::trace_frames() {
  tty->print_cr("[Describe stack]");
  int frame_no = 1;
  for (StackFrameStream fst(this, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
    tty->print("  %d. ", frame_no++);
    fst.current()->print_value_on(tty, this);
    tty->cr();
  }
}

class PrintAndVerifyOopClosure: public OopClosure {
 protected:
  template <class T> inline void do_oop_work(T* p) {
    oop obj = RawAccess<>::oop_load(p);
    if (obj == NULL) return;
    tty->print(INTPTR_FORMAT ": ", p2i(p));
    if (oopDesc::is_oop_or_null(obj)) {
      if (obj->is_objArray()) {
        tty->print_cr("valid objArray: " INTPTR_FORMAT, p2i(obj));
      } else {
        obj->print();
      }
    } else {
      tty->print_cr("invalid oop: " INTPTR_FORMAT, p2i(obj));
    }
    tty->cr();
  }
 public:
  virtual void do_oop(oop* p) { do_oop_work(p); }
  virtual void do_oop(narrowOop* p)  { do_oop_work(p); }
};

#ifdef ASSERT
// Print or validate the layout of stack frames
void JavaThread::print_frame_layout(int depth, bool validate_only) {
  ResourceMark rm;
  PreserveExceptionMark pm(this);
  FrameValues values;
  int frame_no = 0;
  for (StackFrameStream fst(this, false /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
    fst.current()->describe(values, ++frame_no);
    if (depth == frame_no) break;
  }
  if (validate_only) {
    values.validate();
  } else {
    tty->print_cr("[Describe stack layout]");
    values.print(this);
  }
}
#endif

void JavaThread::trace_stack_from(vframe* start_vf) {
  ResourceMark rm;
  int vframe_no = 1;
  for (vframe* f = start_vf; f; f = f->sender()) {
    if (f->is_java_frame()) {
      javaVFrame::cast(f)->print_activation(vframe_no++);
    } else {
      f->print();
    }
    if (vframe_no > StackPrintLimit) {
      tty->print_cr("...<more frames>...");
      return;
    }
  }
}


void JavaThread::trace_stack() {
  if (!has_last_Java_frame()) return;
  Thread* current_thread = Thread::current();
  ResourceMark rm(current_thread);
  HandleMark hm(current_thread);
  RegisterMap reg_map(this);
  trace_stack_from(last_java_vframe(&reg_map));
}


#endif // PRODUCT


javaVFrame* JavaThread::last_java_vframe(RegisterMap *reg_map) {
  assert(reg_map != NULL, "a map must be given");
  frame f = last_frame();
  for (vframe* vf = vframe::new_vframe(&f, reg_map, this); vf; vf = vf->sender()) {
    if (vf->is_java_frame()) return javaVFrame::cast(vf);
  }
  return NULL;
}


Klass* JavaThread::security_get_caller_class(int depth) {
  vframeStream vfst(this);
  vfst.security_get_caller_frame(depth);
  if (!vfst.at_end()) {
    return vfst.method()->method_holder();
  }
  return NULL;
}

// java.lang.Thread.sleep support
// Returns true if sleep time elapsed as expected, and false
// if the thread was interrupted.
bool JavaThread::sleep(jlong millis) {
  assert(this == Thread::current(),  "thread consistency check");

  ParkEvent * const slp = this->_SleepEvent;
  // Because there can be races with thread interruption sending an unpark()
  // to the event, we explicitly reset it here to avoid an immediate return.
  // The actual interrupt state will be checked before we park().
  slp->reset();
  // Thread interruption establishes a happens-before ordering in the
  // Java Memory Model, so we need to ensure we synchronize with the
  // interrupt state.
  OrderAccess::fence();

  jlong prevtime = os::javaTimeNanos();

  for (;;) {
    // interruption has precedence over timing out
    if (this->is_interrupted(true)) {
      return false;
    }

    if (millis <= 0) {
      return true;
    }

    {
      ThreadBlockInVM tbivm(this);
      OSThreadWaitState osts(this->osthread(), false /* not Object.wait() */);
      slp->park(millis);
    }

    // Update elapsed time tracking
    jlong newtime = os::javaTimeNanos();
    if (newtime - prevtime < 0) {
      // time moving backwards, should only happen if no monotonic clock
      // not a guarantee() because JVM should not abort on kernel/glibc bugs
      assert(false,
             "unexpected time moving backwards detected in JavaThread::sleep()");
    } else {
      millis -= (newtime - prevtime) / NANOSECS_PER_MILLISEC;
    }
    prevtime = newtime;
  }
}


// ======= Threads ========

// The Threads class links together all active threads, and provides
// operations over all threads. It is protected by the Threads_lock,
// which is also used in other global contexts like safepointing.
// ThreadsListHandles are used to safely perform operations on one
// or more threads without the risk of the thread exiting during the
// operation.
//
// Note: The Threads_lock is currently more widely used than we
// would like. We are actively migrating Threads_lock uses to other
// mechanisms in order to reduce Threads_lock contention.

int         Threads::_number_of_threads = 0;
int         Threads::_number_of_non_daemon_threads = 0;
int         Threads::_return_code = 0;
uintx       Threads::_thread_claim_token = 1; // Never zero.
size_t      JavaThread::_stack_size_at_create = 0;

#ifdef ASSERT
bool        Threads::_vm_complete = false;
#endif

// All NonJavaThreads (i.e., every non-JavaThread in the system).
void Threads::non_java_threads_do(ThreadClosure* tc) {
  NoSafepointVerifier nsv;
  for (NonJavaThread::Iterator njti; !njti.end(); njti.step()) {
    tc->do_thread(njti.current());
  }
}

// All JavaThreads
#define ALL_JAVA_THREADS(X) \
  for (JavaThread* X : *ThreadsSMRSupport::get_java_thread_list())

// All JavaThreads
void Threads::java_threads_do(ThreadClosure* tc) {
  assert_locked_or_safepoint(Threads_lock);
  // ALL_JAVA_THREADS iterates through all JavaThreads.
  ALL_JAVA_THREADS(p) {
    tc->do_thread(p);
  }
}

void Threads::java_threads_and_vm_thread_do(ThreadClosure* tc) {
  assert_locked_or_safepoint(Threads_lock);
  java_threads_do(tc);
  tc->do_thread(VMThread::vm_thread());
}

// All JavaThreads + all non-JavaThreads (i.e., every thread in the system).
void Threads::threads_do(ThreadClosure* tc) {
  assert_locked_or_safepoint(Threads_lock);
  java_threads_do(tc);
  non_java_threads_do(tc);
}

void Threads::possibly_parallel_threads_do(bool is_par, ThreadClosure* tc) {
  uintx claim_token = Threads::thread_claim_token();
  ALL_JAVA_THREADS(p) {
    if (p->claim_threads_do(is_par, claim_token)) {
      tc->do_thread(p);
    }
  }
  VMThread* vmt = VMThread::vm_thread();
  if (vmt->claim_threads_do(is_par, claim_token)) {
    tc->do_thread(vmt);
  }
}

// The system initialization in the library has three phases.
//
// Phase 1: java.lang.System class initialization
//     java.lang.System is a primordial class loaded and initialized
//     by the VM early during startup.  java.lang.System.<clinit>
//     only does registerNatives and keeps the rest of the class
//     initialization work later until thread initialization completes.
//
//     System.initPhase1 initializes the system properties, the static
//     fields in, out, and err. Set up java signal handlers, OS-specific
//     system settings, and thread group of the main thread.
static void call_initPhase1(TRAPS) {
  Klass* klass = vmClasses::System_klass();
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result, klass, vmSymbols::initPhase1_name(),
                                         vmSymbols::void_method_signature(), CHECK);
}

// Phase 2. Module system initialization
//     This will initialize the module system.  Only java.base classes
//     can be loaded until phase 2 completes.
//
//     Call System.initPhase2 after the compiler initialization and jsr292
//     classes get initialized because module initialization runs a lot of java
//     code, that for performance reasons, should be compiled.  Also, this will
//     enable the startup code to use lambda and other language features in this
//     phase and onward.
//
//     After phase 2, The VM will begin search classes from -Xbootclasspath/a.
static void call_initPhase2(TRAPS) {
  TraceTime timer("Initialize module system", TRACETIME_LOG(Info, startuptime));

  Klass* klass = vmClasses::System_klass();

  JavaValue result(T_INT);
  JavaCallArguments args;
  args.push_int(DisplayVMOutputToStderr);
  args.push_int(log_is_enabled(Debug, init)); // print stack trace if exception thrown
  JavaCalls::call_static(&result, klass, vmSymbols::initPhase2_name(),
                                         vmSymbols::boolean_boolean_int_signature(), &args, CHECK);
  if (result.get_jint() != JNI_OK) {
    vm_exit_during_initialization(); // no message or exception
  }

  universe_post_module_init();
}

// Phase 3. final setup - set security manager, system class loader and TCCL
//
//     This will instantiate and set the security manager, set the system class
//     loader as well as the thread context class loader.  The security manager
//     and system class loader may be a custom class loaded from -Xbootclasspath/a,
//     other modules or the application's classpath.
static void call_initPhase3(TRAPS) {
  Klass* klass = vmClasses::System_klass();
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result, klass, vmSymbols::initPhase3_name(),
                                         vmSymbols::void_method_signature(), CHECK);
}

void Threads::initialize_java_lang_classes(JavaThread* main_thread, TRAPS) {
  TraceTime timer("Initialize java.lang classes", TRACETIME_LOG(Info, startuptime));

  if (EagerXrunInit && Arguments::init_libraries_at_startup()) {
    create_vm_init_libraries();
  }

  initialize_class(vmSymbols::java_lang_String(), CHECK);

  // Inject CompactStrings value after the static initializers for String ran.
  java_lang_String::set_compact_strings(CompactStrings);

  // Initialize java_lang.System (needed before creating the thread)
  initialize_class(vmSymbols::java_lang_System(), CHECK);
  // The VM creates & returns objects of this class. Make sure it's initialized.
  initialize_class(vmSymbols::java_lang_Class(), CHECK);
  initialize_class(vmSymbols::java_lang_ThreadGroup(), CHECK);
  Handle thread_group = create_initial_thread_group(CHECK);
  Universe::set_main_thread_group(thread_group());
  initialize_class(vmSymbols::java_lang_Thread(), CHECK);
  create_initial_thread(thread_group, main_thread, CHECK);

  // The VM creates objects of this class.
  initialize_class(vmSymbols::java_lang_Module(), CHECK);

#ifdef ASSERT
  InstanceKlass *k = vmClasses::UnsafeConstants_klass();
  assert(k->is_not_initialized(), "UnsafeConstants should not already be initialized");
#endif

  // initialize the hardware-specific constants needed by Unsafe
  initialize_class(vmSymbols::jdk_internal_misc_UnsafeConstants(), CHECK);
  jdk_internal_misc_UnsafeConstants::set_unsafe_constants();

  // The VM preresolves methods to these classes. Make sure that they get initialized
  initialize_class(vmSymbols::java_lang_reflect_Method(), CHECK);
  initialize_class(vmSymbols::java_lang_ref_Finalizer(), CHECK);

  // Phase 1 of the system initialization in the library, java.lang.System class initialization
  call_initPhase1(CHECK);

  // Get the Java runtime name, version, and vendor info after java.lang.System is initialized.
  // Some values are actually configure-time constants but some can be set via the jlink tool and
  // so must be read dynamically. We treat them all the same.
  InstanceKlass* ik = SystemDictionary::find_instance_klass(vmSymbols::java_lang_VersionProps(),
                                                            Handle(), Handle());
  {
    ResourceMark rm(main_thread);
    JDK_Version::set_java_version(get_java_version_info(ik, vmSymbols::java_version_name()));

    JDK_Version::set_runtime_name(get_java_version_info(ik, vmSymbols::java_runtime_name_name()));

    JDK_Version::set_runtime_version(get_java_version_info(ik, vmSymbols::java_runtime_version_name()));

    JDK_Version::set_runtime_vendor_version(get_java_version_info(ik, vmSymbols::java_runtime_vendor_version_name()));

    JDK_Version::set_runtime_vendor_vm_bug_url(get_java_version_info(ik, vmSymbols::java_runtime_vendor_vm_bug_url_name()));
  }

  // an instance of OutOfMemory exception has been allocated earlier
  initialize_class(vmSymbols::java_lang_OutOfMemoryError(), CHECK);
  initialize_class(vmSymbols::java_lang_NullPointerException(), CHECK);
  initialize_class(vmSymbols::java_lang_ClassCastException(), CHECK);
  initialize_class(vmSymbols::java_lang_ArrayStoreException(), CHECK);
  initialize_class(vmSymbols::java_lang_ArithmeticException(), CHECK);
  initialize_class(vmSymbols::java_lang_StackOverflowError(), CHECK);
  initialize_class(vmSymbols::java_lang_IllegalMonitorStateException(), CHECK);
  initialize_class(vmSymbols::java_lang_IllegalArgumentException(), CHECK);
}

void Threads::initialize_jsr292_core_classes(TRAPS) {
  TraceTime timer("Initialize java.lang.invoke classes", TRACETIME_LOG(Info, startuptime));

  initialize_class(vmSymbols::java_lang_invoke_MethodHandle(), CHECK);
  initialize_class(vmSymbols::java_lang_invoke_ResolvedMethodName(), CHECK);
  initialize_class(vmSymbols::java_lang_invoke_MemberName(), CHECK);
  initialize_class(vmSymbols::java_lang_invoke_MethodHandleNatives(), CHECK);
}

jint Threads::create_vm(JavaVMInitArgs* args, bool* canTryAgain) {
  extern void JDK_Version_init();

  // Preinitialize version info.
  VM_Version::early_initialize();

  // Check version
  if (!is_supported_jni_version(args->version)) return JNI_EVERSION;

  // Initialize library-based TLS
  ThreadLocalStorage::init();

  // Initialize the output stream module
  ostream_init();

  // Process java launcher properties.
  Arguments::process_sun_java_launcher_properties(args);

  // Initialize the os module
  os::init();

  MACOS_AARCH64_ONLY(os::current_thread_enable_wx(WXWrite));

  // Record VM creation timing statistics
  TraceVmCreationTime create_vm_timer;
  create_vm_timer.start();

  // Initialize system properties.
  Arguments::init_system_properties();

  // So that JDK version can be used as a discriminator when parsing arguments
  JDK_Version_init();

  // Update/Initialize System properties after JDK version number is known
  Arguments::init_version_specific_system_properties();

  // Make sure to initialize log configuration *before* parsing arguments
  LogConfiguration::initialize(create_vm_timer.begin_time());

  // Parse arguments
  // Note: this internally calls os::init_container_support()
  jint parse_result = Arguments::parse(args);
  if (parse_result != JNI_OK) return parse_result;

#if INCLUDE_NMT
  // Initialize NMT right after argument parsing to keep the pre-NMT-init window small.
  MemTracker::initialize();
#endif // INCLUDE_NMT

  os::init_before_ergo();

  jint ergo_result = Arguments::apply_ergo();
  if (ergo_result != JNI_OK) return ergo_result;

  // Final check of all ranges after ergonomics which may change values.
  if (!JVMFlagLimit::check_all_ranges()) {
    return JNI_EINVAL;
  }

  // Final check of all 'AfterErgo' constraints after ergonomics which may change values.
  bool constraint_result = JVMFlagLimit::check_all_constraints(JVMFlagConstraintPhase::AfterErgo);
  if (!constraint_result) {
    return JNI_EINVAL;
  }

  if (PauseAtStartup) {
    os::pause();
  }

  HOTSPOT_VM_INIT_BEGIN();

  // Timing (must come after argument parsing)
  TraceTime timer("Create VM", TRACETIME_LOG(Info, startuptime));

  // Initialize the os module after parsing the args
  jint os_init_2_result = os::init_2();
  if (os_init_2_result != JNI_OK) return os_init_2_result;

#ifdef CAN_SHOW_REGISTERS_ON_ASSERT
  // Initialize assert poison page mechanism.
  if (ShowRegistersOnAssert) {
    initialize_assert_poison();
  }
#endif // CAN_SHOW_REGISTERS_ON_ASSERT

  SafepointMechanism::initialize();

  jint adjust_after_os_result = Arguments::adjust_after_os();
  if (adjust_after_os_result != JNI_OK) return adjust_after_os_result;

  // Initialize output stream logging
  ostream_init_log();

  // Convert -Xrun to -agentlib: if there is no JVM_OnLoad
  // Must be before create_vm_init_agents()
  if (Arguments::init_libraries_at_startup()) {
    convert_vm_init_libraries_to_agents();
  }

  // Launch -agentlib/-agentpath and converted -Xrun agents
  if (Arguments::init_agents_at_startup()) {
    create_vm_init_agents();
  }

  // Initialize Threads state
  _number_of_threads = 0;
  _number_of_non_daemon_threads = 0;

  // Initialize global data structures and create system classes in heap
  vm_init_globals();

#if INCLUDE_JVMCI
  if (JVMCICounterSize > 0) {
    JavaThread::_jvmci_old_thread_counters = NEW_C_HEAP_ARRAY(jlong, JVMCICounterSize, mtJVMCI);
    memset(JavaThread::_jvmci_old_thread_counters, 0, sizeof(jlong) * JVMCICounterSize);
  } else {
    JavaThread::_jvmci_old_thread_counters = NULL;
  }
#endif // INCLUDE_JVMCI

  // Initialize OopStorage for threadObj
  _thread_oop_storage = OopStorageSet::create_strong("Thread OopStorage", mtThread);

  // Attach the main thread to this os thread
  JavaThread* main_thread = new JavaThread();
  main_thread->set_thread_state(_thread_in_vm);
  main_thread->initialize_thread_current();
  // must do this before set_active_handles
  main_thread->record_stack_base_and_size();
  main_thread->register_thread_stack_with_NMT();
  main_thread->set_active_handles(JNIHandleBlock::allocate_block());
  MACOS_AARCH64_ONLY(main_thread->init_wx());

  if (!main_thread->set_as_starting_thread()) {
    vm_shutdown_during_initialization(
                                      "Failed necessary internal allocation. Out of swap space");
    main_thread->smr_delete();
    *canTryAgain = false; // don't let caller call JNI_CreateJavaVM again
    return JNI_ENOMEM;
  }

  // Enable guard page *after* os::create_main_thread(), otherwise it would
  // crash Linux VM, see notes in os_linux.cpp.
  main_thread->stack_overflow_state()->create_stack_guard_pages();

  // Initialize Java-Level synchronization subsystem
  ObjectMonitor::Initialize();
  ObjectSynchronizer::initialize();

  // Initialize global modules
  jint status = init_globals();
  if (status != JNI_OK) {
    main_thread->smr_delete();
    *canTryAgain = false; // don't let caller call JNI_CreateJavaVM again
    return status;
  }

  JFR_ONLY(Jfr::on_create_vm_1();)

  // Should be done after the heap is fully created
  main_thread->cache_global_variables();

  { MutexLocker mu(Threads_lock);
    Threads::add(main_thread);
  }

  // Any JVMTI raw monitors entered in onload will transition into
  // real raw monitor. VM is setup enough here for raw monitor enter.
  JvmtiExport::transition_pending_onload_raw_monitors();

  // Create the VMThread
  { TraceTime timer("Start VMThread", TRACETIME_LOG(Info, startuptime));

    VMThread::create();
    Thread* vmthread = VMThread::vm_thread();

    if (!os::create_thread(vmthread, os::vm_thread)) {
      vm_exit_during_initialization("Cannot create VM thread. "
                                    "Out of system resources.");
    }

    // Wait for the VM thread to become ready, and VMThread::run to initialize
    // Monitors can have spurious returns, must always check another state flag
    {
      MonitorLocker ml(Notify_lock);
      os::start_thread(vmthread);
      while (vmthread->active_handles() == NULL) {
        ml.wait();
      }
    }
  }

  assert(Universe::is_fully_initialized(), "not initialized");
  if (VerifyDuringStartup) {
    // Make sure we're starting with a clean slate.
    VM_Verify verify_op;
    VMThread::execute(&verify_op);
  }

  // We need this to update the java.vm.info property in case any flags used
  // to initially define it have been changed. This is needed for both CDS
  // since UseSharedSpaces may be changed after java.vm.info
  // is initially computed. See Abstract_VM_Version::vm_info_string().
  // This update must happen before we initialize the java classes, but
  // after any initialization logic that might modify the flags.
  Arguments::update_vm_info_property(VM_Version::vm_info_string());

  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  HandleMark hm(THREAD);

  // Always call even when there are not JVMTI environments yet, since environments
  // may be attached late and JVMTI must track phases of VM execution
  JvmtiExport::enter_early_start_phase();

  // Notify JVMTI agents that VM has started (JNI is up) - nop if no agents.
  JvmtiExport::post_early_vm_start();

  initialize_java_lang_classes(main_thread, CHECK_JNI_ERR);

  quicken_jni_functions();

  // No more stub generation allowed after that point.
  StubCodeDesc::freeze();

  // Set flag that basic initialization has completed. Used by exceptions and various
  // debug stuff, that does not work until all basic classes have been initialized.
  set_init_completed();

  LogConfiguration::post_initialize();
  Metaspace::post_initialize();

  HOTSPOT_VM_INIT_END();

  // record VM initialization completion time
#if INCLUDE_MANAGEMENT
  Management::record_vm_init_completed();
#endif // INCLUDE_MANAGEMENT

  // Signal Dispatcher needs to be started before VMInit event is posted
  os::initialize_jdk_signal_support(CHECK_JNI_ERR);

  // Start Attach Listener if +StartAttachListener or it can't be started lazily
  if (!DisableAttachMechanism) {
    AttachListener::vm_start();
    if (StartAttachListener || AttachListener::init_at_startup()) {
      AttachListener::init();
    }
  }

  // Launch -Xrun agents
  // Must be done in the JVMTI live phase so that for backward compatibility the JDWP
  // back-end can launch with -Xdebug -Xrunjdwp.
  if (!EagerXrunInit && Arguments::init_libraries_at_startup()) {
    create_vm_init_libraries();
  }

  Chunk::start_chunk_pool_cleaner_task();

  // Start the service thread
  // The service thread enqueues JVMTI deferred events and does various hashtable
  // and other cleanups.  Needs to start before the compilers start posting events.
  ServiceThread::initialize();

  // Start the monitor deflation thread:
  MonitorDeflationThread::initialize();

  // initialize compiler(s)
#if defined(COMPILER1) || COMPILER2_OR_JVMCI
#if INCLUDE_JVMCI
  bool force_JVMCI_intialization = false;
  if (EnableJVMCI) {
    // Initialize JVMCI eagerly when it is explicitly requested.
    // Or when JVMCILibDumpJNIConfig or JVMCIPrintProperties is enabled.
    force_JVMCI_intialization = EagerJVMCI || JVMCIPrintProperties || JVMCILibDumpJNIConfig;

    if (!force_JVMCI_intialization) {
      // 8145270: Force initialization of JVMCI runtime otherwise requests for blocking
      // compilations via JVMCI will not actually block until JVMCI is initialized.
      force_JVMCI_intialization = UseJVMCICompiler && (!UseInterpreter || !BackgroundCompilation);
    }
  }
#endif
  CompileBroker::compilation_init_phase1(CHECK_JNI_ERR);
  // Postpone completion of compiler initialization to after JVMCI
  // is initialized to avoid timeouts of blocking compilations.
  if (JVMCI_ONLY(!force_JVMCI_intialization) NOT_JVMCI(true)) {
    CompileBroker::compilation_init_phase2();
  }
#endif

  // Pre-initialize some JSR292 core classes to avoid deadlock during class loading.
  // It is done after compilers are initialized, because otherwise compilations of
  // signature polymorphic MH intrinsics can be missed
  // (see SystemDictionary::find_method_handle_intrinsic).
  initialize_jsr292_core_classes(CHECK_JNI_ERR);

  // This will initialize the module system.  Only java.base classes can be
  // loaded until phase 2 completes
  call_initPhase2(CHECK_JNI_ERR);

  JFR_ONLY(Jfr::on_create_vm_2();)

  // Always call even when there are not JVMTI environments yet, since environments
  // may be attached late and JVMTI must track phases of VM execution
  JvmtiExport::enter_start_phase();

  // Notify JVMTI agents that VM has started (JNI is up) - nop if no agents.
  JvmtiExport::post_vm_start();

  // Final system initialization including security manager and system class loader
  call_initPhase3(CHECK_JNI_ERR);

  // cache the system and platform class loaders
  SystemDictionary::compute_java_loaders(CHECK_JNI_ERR);

#if INCLUDE_CDS
  // capture the module path info from the ModuleEntryTable
  ClassLoader::initialize_module_path(THREAD);
  if (HAS_PENDING_EXCEPTION) {
    java_lang_Throwable::print(PENDING_EXCEPTION, tty);
    vm_exit_during_initialization("ClassLoader::initialize_module_path() failed unexpectedly");
  }
#endif

#if INCLUDE_JVMCI
  if (force_JVMCI_intialization) {
    JVMCI::initialize_compiler(CHECK_JNI_ERR);
    CompileBroker::compilation_init_phase2();
  }
#endif

  // Always call even when there are not JVMTI environments yet, since environments
  // may be attached late and JVMTI must track phases of VM execution
  JvmtiExport::enter_live_phase();

  // Make perfmemory accessible
  PerfMemory::set_accessible(true);

  // Notify JVMTI agents that VM initialization is complete - nop if no agents.
  JvmtiExport::post_vm_initialized();

  JFR_ONLY(Jfr::on_create_vm_3();)

#if INCLUDE_MANAGEMENT
  Management::initialize(THREAD);

  if (HAS_PENDING_EXCEPTION) {
    // management agent fails to start possibly due to
    // configuration problem and is responsible for printing
    // stack trace if appropriate. Simply exit VM.
    vm_exit(1);
  }
#endif // INCLUDE_MANAGEMENT

  StatSampler::engage();
  if (CheckJNICalls)                  JniPeriodicChecker::engage();

#if INCLUDE_RTM_OPT
  RTMLockingCounters::init();
#endif

  call_postVMInitHook(THREAD);
  // The Java side of PostVMInitHook.run must deal with all
  // exceptions and provide means of diagnosis.
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
  }

  {
    MutexLocker ml(PeriodicTask_lock);
    // Make sure the WatcherThread can be started by WatcherThread::start()
    // or by dynamic enrollment.
    WatcherThread::make_startable();
    // Start up the WatcherThread if there are any periodic tasks
    // NOTE:  All PeriodicTasks should be registered by now. If they
    //   aren't, late joiners might appear to start slowly (we might
    //   take a while to process their first tick).
    if (PeriodicTask::num_tasks() > 0) {
      WatcherThread::start();
    }
  }

  create_vm_timer.end();
#ifdef ASSERT
  _vm_complete = true;
#endif

  if (DumpSharedSpaces) {
    MetaspaceShared::preload_and_dump();
    ShouldNotReachHere();
  }

  return JNI_OK;
}

// type for the Agent_OnLoad and JVM_OnLoad entry points
extern "C" {
  typedef jint (JNICALL *OnLoadEntry_t)(JavaVM *, char *, void *);
}
// Find a command line agent library and return its entry point for
//         -agentlib:  -agentpath:   -Xrun
// num_symbol_entries must be passed-in since only the caller knows the number of symbols in the array.
static OnLoadEntry_t lookup_on_load(AgentLibrary* agent,
                                    const char *on_load_symbols[],
                                    size_t num_symbol_entries) {
  OnLoadEntry_t on_load_entry = NULL;
  void *library = NULL;

  if (!agent->valid()) {
    char buffer[JVM_MAXPATHLEN];
    char ebuf[1024] = "";
    const char *name = agent->name();
    const char *msg = "Could not find agent library ";

    // First check to see if agent is statically linked into executable
    if (os::find_builtin_agent(agent, on_load_symbols, num_symbol_entries)) {
      library = agent->os_lib();
    } else if (agent->is_absolute_path()) {
      library = os::dll_load(name, ebuf, sizeof ebuf);
      if (library == NULL) {
        const char *sub_msg = " in absolute path, with error: ";
        size_t len = strlen(msg) + strlen(name) + strlen(sub_msg) + strlen(ebuf) + 1;
        char *buf = NEW_C_HEAP_ARRAY(char, len, mtThread);
        jio_snprintf(buf, len, "%s%s%s%s", msg, name, sub_msg, ebuf);
        // If we can't find the agent, exit.
        vm_exit_during_initialization(buf, NULL);
        FREE_C_HEAP_ARRAY(char, buf);
      }
    } else {
      // Try to load the agent from the standard dll directory
      if (os::dll_locate_lib(buffer, sizeof(buffer), Arguments::get_dll_dir(),
                             name)) {
        library = os::dll_load(buffer, ebuf, sizeof ebuf);
      }
      if (library == NULL) { // Try the library path directory.
        if (os::dll_build_name(buffer, sizeof(buffer), name)) {
          library = os::dll_load(buffer, ebuf, sizeof ebuf);
        }
        if (library == NULL) {
          const char *sub_msg = " on the library path, with error: ";
          const char *sub_msg2 = "\nModule java.instrument may be missing from runtime image.";

          size_t len = strlen(msg) + strlen(name) + strlen(sub_msg) +
                       strlen(ebuf) + strlen(sub_msg2) + 1;
          char *buf = NEW_C_HEAP_ARRAY(char, len, mtThread);
          if (!agent->is_instrument_lib()) {
            jio_snprintf(buf, len, "%s%s%s%s", msg, name, sub_msg, ebuf);
          } else {
            jio_snprintf(buf, len, "%s%s%s%s%s", msg, name, sub_msg, ebuf, sub_msg2);
          }
          // If we can't find the agent, exit.
          vm_exit_during_initialization(buf, NULL);
          FREE_C_HEAP_ARRAY(char, buf);
        }
      }
    }
    agent->set_os_lib(library);
    agent->set_valid();
  }

  // Find the OnLoad function.
  on_load_entry =
    CAST_TO_FN_PTR(OnLoadEntry_t, os::find_agent_function(agent,
                                                          false,
                                                          on_load_symbols,
                                                          num_symbol_entries));
  return on_load_entry;
}

// Find the JVM_OnLoad entry point
static OnLoadEntry_t lookup_jvm_on_load(AgentLibrary* agent) {
  const char *on_load_symbols[] = JVM_ONLOAD_SYMBOLS;
  return lookup_on_load(agent, on_load_symbols, sizeof(on_load_symbols) / sizeof(char*));
}

// Find the Agent_OnLoad entry point
static OnLoadEntry_t lookup_agent_on_load(AgentLibrary* agent) {
  const char *on_load_symbols[] = AGENT_ONLOAD_SYMBOLS;
  return lookup_on_load(agent, on_load_symbols, sizeof(on_load_symbols) / sizeof(char*));
}

// For backwards compatibility with -Xrun
// Convert libraries with no JVM_OnLoad, but which have Agent_OnLoad to be
// treated like -agentpath:
// Must be called before agent libraries are created
void Threads::convert_vm_init_libraries_to_agents() {
  AgentLibrary* agent;
  AgentLibrary* next;

  for (agent = Arguments::libraries(); agent != NULL; agent = next) {
    next = agent->next();  // cache the next agent now as this agent may get moved off this list
    OnLoadEntry_t on_load_entry = lookup_jvm_on_load(agent);

    // If there is an JVM_OnLoad function it will get called later,
    // otherwise see if there is an Agent_OnLoad
    if (on_load_entry == NULL) {
      on_load_entry = lookup_agent_on_load(agent);
      if (on_load_entry != NULL) {
        // switch it to the agent list -- so that Agent_OnLoad will be called,
        // JVM_OnLoad won't be attempted and Agent_OnUnload will
        Arguments::convert_library_to_agent(agent);
      } else {
        vm_exit_during_initialization("Could not find JVM_OnLoad or Agent_OnLoad function in the library", agent->name());
      }
    }
  }
}

// Create agents for -agentlib:  -agentpath:  and converted -Xrun
// Invokes Agent_OnLoad
// Called very early -- before JavaThreads exist
void Threads::create_vm_init_agents() {
  extern struct JavaVM_ main_vm;
  AgentLibrary* agent;

  JvmtiExport::enter_onload_phase();

  for (agent = Arguments::agents(); agent != NULL; agent = agent->next()) {
    // CDS dumping does not support native JVMTI agent.
    // CDS dumping supports Java agent if the AllowArchivingWithJavaAgent diagnostic option is specified.
    if (Arguments::is_dumping_archive()) {
      if(!agent->is_instrument_lib()) {
        vm_exit_during_cds_dumping("CDS dumping does not support native JVMTI agent, name", agent->name());
      } else if (!AllowArchivingWithJavaAgent) {
        vm_exit_during_cds_dumping(
          "Must enable AllowArchivingWithJavaAgent in order to run Java agent during CDS dumping");
      }
    }

    OnLoadEntry_t  on_load_entry = lookup_agent_on_load(agent);

    if (on_load_entry != NULL) {
      // Invoke the Agent_OnLoad function
      jint err = (*on_load_entry)(&main_vm, agent->options(), NULL);
      if (err != JNI_OK) {
        vm_exit_during_initialization("agent library failed to init", agent->name());
      }
    } else {
      vm_exit_during_initialization("Could not find Agent_OnLoad function in the agent library", agent->name());
    }
  }

  JvmtiExport::enter_primordial_phase();
}

extern "C" {
  typedef void (JNICALL *Agent_OnUnload_t)(JavaVM *);
}

void Threads::shutdown_vm_agents() {
  // Send any Agent_OnUnload notifications
  const char *on_unload_symbols[] = AGENT_ONUNLOAD_SYMBOLS;
  size_t num_symbol_entries = ARRAY_SIZE(on_unload_symbols);
  extern struct JavaVM_ main_vm;
  for (AgentLibrary* agent = Arguments::agents(); agent != NULL; agent = agent->next()) {

    // Find the Agent_OnUnload function.
    Agent_OnUnload_t unload_entry = CAST_TO_FN_PTR(Agent_OnUnload_t,
                                                   os::find_agent_function(agent,
                                                   false,
                                                   on_unload_symbols,
                                                   num_symbol_entries));

    // Invoke the Agent_OnUnload function
    if (unload_entry != NULL) {
      JavaThread* thread = JavaThread::current();
      ThreadToNativeFromVM ttn(thread);
      HandleMark hm(thread);
      (*unload_entry)(&main_vm);
    }
  }
}

// Called for after the VM is initialized for -Xrun libraries which have not been converted to agent libraries
// Invokes JVM_OnLoad
void Threads::create_vm_init_libraries() {
  extern struct JavaVM_ main_vm;
  AgentLibrary* agent;

  for (agent = Arguments::libraries(); agent != NULL; agent = agent->next()) {
    OnLoadEntry_t on_load_entry = lookup_jvm_on_load(agent);

    if (on_load_entry != NULL) {
      // Invoke the JVM_OnLoad function
      JavaThread* thread = JavaThread::current();
      ThreadToNativeFromVM ttn(thread);
      HandleMark hm(thread);
      jint err = (*on_load_entry)(&main_vm, agent->options(), NULL);
      if (err != JNI_OK) {
        vm_exit_during_initialization("-Xrun library failed to init", agent->name());
      }
    } else {
      vm_exit_during_initialization("Could not find JVM_OnLoad function in -Xrun library", agent->name());
    }
  }
}


// Last thread running calls java.lang.Shutdown.shutdown()
void JavaThread::invoke_shutdown_hooks() {
  HandleMark hm(this);

  // We could get here with a pending exception, if so clear it now or
  // it will cause MetaspaceShared::link_shared_classes to
  // fail for dynamic dump.
  if (this->has_pending_exception()) {
    this->clear_pending_exception();
  }

#if INCLUDE_CDS
  // Link all classes for dynamic CDS dumping before vm exit.
  // Same operation is being done in JVM_BeforeHalt for handling the
  // case where the application calls System.exit().
  if (DynamicDumpSharedSpaces) {
    DynamicArchive::prepare_for_dynamic_dumping();
  }
#endif

  EXCEPTION_MARK;
  Klass* shutdown_klass =
    SystemDictionary::resolve_or_null(vmSymbols::java_lang_Shutdown(),
                                      THREAD);
  if (shutdown_klass != NULL) {
    // SystemDictionary::resolve_or_null will return null if there was
    // an exception.  If we cannot load the Shutdown class, just don't
    // call Shutdown.shutdown() at all.  This will mean the shutdown hooks
    // won't be run.  Note that if a shutdown hook was registered,
    // the Shutdown class would have already been loaded
    // (Runtime.addShutdownHook will load it).
    JavaValue result(T_VOID);
    JavaCalls::call_static(&result,
                           shutdown_klass,
                           vmSymbols::shutdown_name(),
                           vmSymbols::void_method_signature(),
                           THREAD);
  }
  CLEAR_PENDING_EXCEPTION;
}

// Threads::destroy_vm() is normally called from jni_DestroyJavaVM() when
// the program falls off the end of main(). Another VM exit path is through
// vm_exit() when the program calls System.exit() to return a value or when
// there is a serious error in VM. The two shutdown paths are not exactly
// the same, but they share Shutdown.shutdown() at Java level and before_exit()
// and VM_Exit op at VM level.
//
// Shutdown sequence:
//   + Shutdown native memory tracking if it is on
//   + Wait until we are the last non-daemon thread to execute
//     <-- every thing is still working at this moment -->
//   + Call java.lang.Shutdown.shutdown(), which will invoke Java level
//        shutdown hooks
//   + Call before_exit(), prepare for VM exit
//      > run VM level shutdown hooks (they are registered through JVM_OnExit(),
//        currently the only user of this mechanism is File.deleteOnExit())
//      > stop StatSampler, watcher thread,
//        post thread end and vm death events to JVMTI,
//        stop signal thread
//   + Call JavaThread::exit(), it will:
//      > release JNI handle blocks, remove stack guard pages
//      > remove this thread from Threads list
//     <-- no more Java code from this thread after this point -->
//   + Stop VM thread, it will bring the remaining VM to a safepoint and stop
//     the compiler threads at safepoint
//     <-- do not use anything that could get blocked by Safepoint -->
//   + Disable tracing at JNI/JVM barriers
//   + Set _vm_exited flag for threads that are still running native code
//   + Call exit_globals()
//      > deletes tty
//      > deletes PerfMemory resources
//   + Delete this thread
//   + Return to caller

void Threads::destroy_vm() {
  JavaThread* thread = JavaThread::current();

#ifdef ASSERT
  _vm_complete = false;
#endif
  // Wait until we are the last non-daemon thread to execute
  {
    MonitorLocker nu(Threads_lock);
    while (Threads::number_of_non_daemon_threads() > 1)
      // This wait should make safepoint checks, wait without a timeout.
      nu.wait(0);
  }

  EventShutdown e;
  if (e.should_commit()) {
    e.set_reason("No remaining non-daemon Java threads");
    e.commit();
  }

  // Hang forever on exit if we are reporting an error.
  if (ShowMessageBoxOnError && VMError::is_error_reported()) {
    os::infinite_sleep();
  }
  os::wait_for_keypress_at_exit();

  // run Java level shutdown hooks
  thread->invoke_shutdown_hooks();

  before_exit(thread);

  thread->exit(true);

  // We are no longer on the main thread list but could still be in a
  // secondary list where another thread may try to interact with us.
  // So wait until all such interactions are complete before we bring
  // the VM to the termination safepoint. Normally this would be done
  // using thread->smr_delete() below where we delete the thread, but
  // we can't call that after the termination safepoint is active as
  // we will deadlock on the Threads_lock. Once all interactions are
  // complete it is safe to directly delete the thread at any time.
  ThreadsSMRSupport::wait_until_not_protected(thread);

  // Stop VM thread.
  {
    // 4945125 The vm thread comes to a safepoint during exit.
    // GC vm_operations can get caught at the safepoint, and the
    // heap is unparseable if they are caught. Grab the Heap_lock
    // to prevent this. The GC vm_operations will not be able to
    // queue until after the vm thread is dead. After this point,
    // we'll never emerge out of the safepoint before the VM exits.
    // Assert that the thread is terminated so that acquiring the
    // Heap_lock doesn't cause the terminated thread to participate in
    // the safepoint protocol.

    assert(thread->is_terminated(), "must be terminated here");
    MutexLocker ml(Heap_lock);

    VMThread::wait_for_vm_thread_exit();
    assert(SafepointSynchronize::is_at_safepoint(), "VM thread should exit at Safepoint");
    VMThread::destroy();
  }

  // Now, all Java threads are gone except daemon threads. Daemon threads
  // running Java code or in VM are stopped by the Safepoint. However,
  // daemon threads executing native code are still running.  But they
  // will be stopped at native=>Java/VM barriers. Note that we can't
  // simply kill or suspend them, as it is inherently deadlock-prone.

  VM_Exit::set_vm_exited();

  // Clean up ideal graph printers after the VMThread has started
  // the final safepoint which will block all the Compiler threads.
  // Note that this Thread has already logically exited so the
  // clean_up() function's use of a JavaThreadIteratorWithHandle
  // would be a problem except set_vm_exited() has remembered the
  // shutdown thread which is granted a policy exception.
#if defined(COMPILER2) && !defined(PRODUCT)
  IdealGraphPrinter::clean_up();
#endif

  notify_vm_shutdown();

  // exit_globals() will delete tty
  exit_globals();

  // Deleting the shutdown thread here is safe. See comment on
  // wait_until_not_protected() above.
  delete thread;

#if INCLUDE_JVMCI
  if (JVMCICounterSize > 0) {
    FREE_C_HEAP_ARRAY(jlong, JavaThread::_jvmci_old_thread_counters);
  }
#endif

  LogConfiguration::finalize();
}


jboolean Threads::is_supported_jni_version_including_1_1(jint version) {
  if (version == JNI_VERSION_1_1) return JNI_TRUE;
  return is_supported_jni_version(version);
}


jboolean Threads::is_supported_jni_version(jint version) {
  if (version == JNI_VERSION_1_2) return JNI_TRUE;
  if (version == JNI_VERSION_1_4) return JNI_TRUE;
  if (version == JNI_VERSION_1_6) return JNI_TRUE;
  if (version == JNI_VERSION_1_8) return JNI_TRUE;
  if (version == JNI_VERSION_9) return JNI_TRUE;
  if (version == JNI_VERSION_10) return JNI_TRUE;
  return JNI_FALSE;
}


void Threads::add(JavaThread* p, bool force_daemon) {
  // The threads lock must be owned at this point
  assert(Threads_lock->owned_by_self(), "must have threads lock");

  BarrierSet::barrier_set()->on_thread_attach(p);

  // Once a JavaThread is added to the Threads list, smr_delete() has
  // to be used to delete it. Otherwise we can just delete it directly.
  p->set_on_thread_list();

  _number_of_threads++;
  oop threadObj = p->threadObj();
  bool daemon = true;
  // Bootstrapping problem: threadObj can be null for initial
  // JavaThread (or for threads attached via JNI)
  if ((!force_daemon) && !is_daemon((threadObj))) {
    _number_of_non_daemon_threads++;
    daemon = false;
  }

  ThreadService::add_thread(p, daemon);

  // Maintain fast thread list
  ThreadsSMRSupport::add_thread(p);

  // Increase the ObjectMonitor ceiling for the new thread.
  ObjectSynchronizer::inc_in_use_list_ceiling();

  // Possible GC point.
  Events::log(p, "Thread added: " INTPTR_FORMAT, p2i(p));

  // Make new thread known to active EscapeBarrier
  EscapeBarrier::thread_added(p);
}

void Threads::remove(JavaThread* p, bool is_daemon) {
  // Extra scope needed for Thread_lock, so we can check
  // that we do not remove thread without safepoint code notice
  { MonitorLocker ml(Threads_lock);

    // BarrierSet state must be destroyed after the last thread transition
    // before the thread terminates. Thread transitions result in calls to
    // StackWatermarkSet::on_safepoint(), which performs GC processing,
    // requiring the GC state to be alive.
    BarrierSet::barrier_set()->on_thread_detach(p);

    assert(ThreadsSMRSupport::get_java_thread_list()->includes(p), "p must be present");

    // Maintain fast thread list
    ThreadsSMRSupport::remove_thread(p);

    _number_of_threads--;
    if (!is_daemon) {
      _number_of_non_daemon_threads--;

      // Only one thread left, do a notify on the Threads_lock so a thread waiting
      // on destroy_vm will wake up.
      if (number_of_non_daemon_threads() == 1) {
        ml.notify_all();
      }
    }
    ThreadService::remove_thread(p, is_daemon);

    // Make sure that safepoint code disregard this thread. This is needed since
    // the thread might mess around with locks after this point. This can cause it
    // to do callbacks into the safepoint code. However, the safepoint code is not aware
    // of this thread since it is removed from the queue.
    p->set_terminated(JavaThread::_thread_terminated);

    // Notify threads waiting in EscapeBarriers
    EscapeBarrier::thread_removed(p);
  } // unlock Threads_lock

  // Reduce the ObjectMonitor ceiling for the exiting thread.
  ObjectSynchronizer::dec_in_use_list_ceiling();

  // Since Events::log uses a lock, we grab it outside the Threads_lock
  Events::log(p, "Thread exited: " INTPTR_FORMAT, p2i(p));
}

// Operations on the Threads list for GC.  These are not explicitly locked,
// but the garbage collector must provide a safe context for them to run.
// In particular, these things should never be called when the Threads_lock
// is held by some other thread. (Note: the Safepoint abstraction also
// uses the Threads_lock to guarantee this property. It also makes sure that
// all threads gets blocked when exiting or starting).

void Threads::oops_do(OopClosure* f, CodeBlobClosure* cf) {
  ALL_JAVA_THREADS(p) {
    p->oops_do(f, cf);
  }
  VMThread::vm_thread()->oops_do(f, cf);
}

void Threads::change_thread_claim_token() {
  if (++_thread_claim_token == 0) {
    // On overflow of the token counter, there is a risk of future
    // collisions between a new global token value and a stale token
    // for a thread, because not all iterations visit all threads.
    // (Though it's pretty much a theoretical concern for non-trivial
    // token counter sizes.)  To deal with the possibility, reset all
    // the thread tokens to zero on global token overflow.
    struct ResetClaims : public ThreadClosure {
      virtual void do_thread(Thread* t) {
        t->claim_threads_do(false, 0);
      }
    } reset_claims;
    Threads::threads_do(&reset_claims);
    // On overflow, update the global token to non-zero, to
    // avoid the special "never claimed" initial thread value.
    _thread_claim_token = 1;
  }
}

#ifdef ASSERT
void assert_thread_claimed(const char* kind, Thread* t, uintx expected) {
  const uintx token = t->threads_do_token();
  assert(token == expected,
         "%s " PTR_FORMAT " has incorrect value " UINTX_FORMAT " != "
         UINTX_FORMAT, kind, p2i(t), token, expected);
}

void Threads::assert_all_threads_claimed() {
  ALL_JAVA_THREADS(p) {
    assert_thread_claimed("Thread", p, _thread_claim_token);
  }
  assert_thread_claimed("VMThread", VMThread::vm_thread(), _thread_claim_token);
}
#endif // ASSERT

class ParallelOopsDoThreadClosure : public ThreadClosure {
private:
  OopClosure* _f;
  CodeBlobClosure* _cf;
public:
  ParallelOopsDoThreadClosure(OopClosure* f, CodeBlobClosure* cf) : _f(f), _cf(cf) {}
  void do_thread(Thread* t) {
    t->oops_do(_f, _cf);
  }
};

void Threads::possibly_parallel_oops_do(bool is_par, OopClosure* f, CodeBlobClosure* cf) {
  ParallelOopsDoThreadClosure tc(f, cf);
  possibly_parallel_threads_do(is_par, &tc);
}

void Threads::metadata_do(MetadataClosure* f) {
  ALL_JAVA_THREADS(p) {
    p->metadata_do(f);
  }
}

class ThreadHandlesClosure : public ThreadClosure {
  void (*_f)(Metadata*);
 public:
  ThreadHandlesClosure(void f(Metadata*)) : _f(f) {}
  virtual void do_thread(Thread* thread) {
    thread->metadata_handles_do(_f);
  }
};

void Threads::metadata_handles_do(void f(Metadata*)) {
  // Only walk the Handles in Thread.
  ThreadHandlesClosure handles_closure(f);
  threads_do(&handles_closure);
}

// Get count Java threads that are waiting to enter the specified monitor.
GrowableArray<JavaThread*>* Threads::get_pending_threads(ThreadsList * t_list,
                                                         int count,
                                                         address monitor) {
  GrowableArray<JavaThread*>* result = new GrowableArray<JavaThread*>(count);

  int i = 0;
  for (JavaThread* p : *t_list) {
    if (!p->can_call_java()) continue;

    // The first stage of async deflation does not affect any field
    // used by this comparison so the ObjectMonitor* is usable here.
    address pending = (address)p->current_pending_monitor();
    if (pending == monitor) {             // found a match
      if (i < count) result->append(p);   // save the first count matches
      i++;
    }
  }

  return result;
}


JavaThread *Threads::owning_thread_from_monitor_owner(ThreadsList * t_list,
                                                      address owner) {
  // NULL owner means not locked so we can skip the search
  if (owner == NULL) return NULL;

  for (JavaThread* p : *t_list) {
    // first, see if owner is the address of a Java thread
    if (owner == (address)p) return p;
  }

  // Cannot assert on lack of success here since this function may be
  // used by code that is trying to report useful problem information
  // like deadlock detection.
  if (UseHeavyMonitors) return NULL;

  // If we didn't find a matching Java thread and we didn't force use of
  // heavyweight monitors, then the owner is the stack address of the
  // Lock Word in the owning Java thread's stack.
  //
  JavaThread* the_owner = NULL;
  for (JavaThread* q : *t_list) {
    if (q->is_lock_owned(owner)) {
      the_owner = q;
      break;
    }
  }

  // cannot assert on lack of success here; see above comment
  return the_owner;
}

class PrintOnClosure : public ThreadClosure {
private:
  outputStream* _st;

public:
  PrintOnClosure(outputStream* st) :
      _st(st) {}

  virtual void do_thread(Thread* thread) {
    if (thread != NULL) {
      thread->print_on(_st);
      _st->cr();
    }
  }
};

// Threads::print_on() is called at safepoint by VM_PrintThreads operation.
void Threads::print_on(outputStream* st, bool print_stacks,
                       bool internal_format, bool print_concurrent_locks,
                       bool print_extended_info) {
  char buf[32];
  st->print_raw_cr(os::local_time_string(buf, sizeof(buf)));

  st->print_cr("Full thread dump %s (%s %s):",
               VM_Version::vm_name(),
               VM_Version::vm_release(),
               VM_Version::vm_info_string());
  st->cr();

#if INCLUDE_SERVICES
  // Dump concurrent locks
  ConcurrentLocksDump concurrent_locks;
  if (print_concurrent_locks) {
    concurrent_locks.dump_at_safepoint();
  }
#endif // INCLUDE_SERVICES

  ThreadsSMRSupport::print_info_on(st);
  st->cr();

  ALL_JAVA_THREADS(p) {
    ResourceMark rm;
    p->print_on(st, print_extended_info);
    if (print_stacks) {
      if (internal_format) {
        p->trace_stack();
      } else {
        p->print_stack_on(st);
      }
    }
    st->cr();
#if INCLUDE_SERVICES
    if (print_concurrent_locks) {
      concurrent_locks.print_locks_on(p, st);
    }
#endif // INCLUDE_SERVICES
  }

  PrintOnClosure cl(st);
  cl.do_thread(VMThread::vm_thread());
  Universe::heap()->gc_threads_do(&cl);
  if (StringDedup::is_enabled()) {
    StringDedup::threads_do(&cl);
  }
  cl.do_thread(WatcherThread::watcher_thread());
  cl.do_thread(AsyncLogWriter::instance());

  st->flush();
}

void Threads::print_on_error(Thread* this_thread, outputStream* st, Thread* current, char* buf,
                             int buflen, bool* found_current) {
  if (this_thread != NULL) {
    bool is_current = (current == this_thread);
    *found_current = *found_current || is_current;
    st->print("%s", is_current ? "=>" : "  ");

    st->print(PTR_FORMAT, p2i(this_thread));
    st->print(" ");
    this_thread->print_on_error(st, buf, buflen);
    st->cr();
  }
}

class PrintOnErrorClosure : public ThreadClosure {
  outputStream* _st;
  Thread* _current;
  char* _buf;
  int _buflen;
  bool* _found_current;
 public:
  PrintOnErrorClosure(outputStream* st, Thread* current, char* buf,
                      int buflen, bool* found_current) :
   _st(st), _current(current), _buf(buf), _buflen(buflen), _found_current(found_current) {}

  virtual void do_thread(Thread* thread) {
    Threads::print_on_error(thread, _st, _current, _buf, _buflen, _found_current);
  }
};

// Threads::print_on_error() is called by fatal error handler. It's possible
// that VM is not at safepoint and/or current thread is inside signal handler.
// Don't print stack trace, as the stack may not be walkable. Don't allocate
// memory (even in resource area), it might deadlock the error handler.
void Threads::print_on_error(outputStream* st, Thread* current, char* buf,
                             int buflen) {
  ThreadsSMRSupport::print_info_on(st);
  st->cr();

  bool found_current = false;
  st->print_cr("Java Threads: ( => current thread )");
  ALL_JAVA_THREADS(thread) {
    print_on_error(thread, st, current, buf, buflen, &found_current);
  }
  st->cr();

  st->print_cr("Other Threads:");
  print_on_error(VMThread::vm_thread(), st, current, buf, buflen, &found_current);
  print_on_error(WatcherThread::watcher_thread(), st, current, buf, buflen, &found_current);
  print_on_error(AsyncLogWriter::instance(), st, current, buf, buflen, &found_current);

  if (Universe::heap() != NULL) {
    PrintOnErrorClosure print_closure(st, current, buf, buflen, &found_current);
    Universe::heap()->gc_threads_do(&print_closure);
  }

  if (StringDedup::is_enabled()) {
    PrintOnErrorClosure print_closure(st, current, buf, buflen, &found_current);
    StringDedup::threads_do(&print_closure);
  }

  if (!found_current) {
    st->cr();
    st->print("=>" PTR_FORMAT " (exited) ", p2i(current));
    current->print_on_error(st, buf, buflen);
    st->cr();
  }
  st->cr();

  st->print_cr("Threads with active compile tasks:");
  print_threads_compiling(st, buf, buflen);
}

void Threads::print_threads_compiling(outputStream* st, char* buf, int buflen, bool short_form) {
  ALL_JAVA_THREADS(thread) {
    if (thread->is_Compiler_thread()) {
      CompilerThread* ct = (CompilerThread*) thread;

      // Keep task in local variable for NULL check.
      // ct->_task might be set to NULL by concurring compiler thread
      // because it completed the compilation. The task is never freed,
      // though, just returned to a free list.
      CompileTask* task = ct->task();
      if (task != NULL) {
        thread->print_name_on_error(st, buf, buflen);
        st->print("  ");
        task->print(st, NULL, short_form, true);
      }
    }
  }
}


// Ad-hoc mutual exclusion primitives: SpinLock
//
// We employ SpinLocks _only for low-contention, fixed-length
// short-duration critical sections where we're concerned
// about native mutex_t or HotSpot Mutex:: latency.
//
// TODO-FIXME: ListLock should be of type SpinLock.
// We should make this a 1st-class type, integrated into the lock
// hierarchy as leaf-locks.  Critically, the SpinLock structure
// should have sufficient padding to avoid false-sharing and excessive
// cache-coherency traffic.


typedef volatile int SpinLockT;

void Thread::SpinAcquire(volatile int * adr, const char * LockName) {
  if (Atomic::cmpxchg(adr, 0, 1) == 0) {
    return;   // normal fast-path return
  }

  // Slow-path : We've encountered contention -- Spin/Yield/Block strategy.
  int ctr = 0;
  int Yields = 0;
  for (;;) {
    while (*adr != 0) {
      ++ctr;
      if ((ctr & 0xFFF) == 0 || !os::is_MP()) {
        if (Yields > 5) {
          os::naked_short_sleep(1);
        } else {
          os::naked_yield();
          ++Yields;
        }
      } else {
        SpinPause();
      }
    }
    if (Atomic::cmpxchg(adr, 0, 1) == 0) return;
  }
}

void Thread::SpinRelease(volatile int * adr) {
  assert(*adr != 0, "invariant");
  OrderAccess::fence();      // guarantee at least release consistency.
  // Roach-motel semantics.
  // It's safe if subsequent LDs and STs float "up" into the critical section,
  // but prior LDs and STs within the critical section can't be allowed
  // to reorder or float past the ST that releases the lock.
  // Loads and stores in the critical section - which appear in program
  // order before the store that releases the lock - must also appear
  // before the store that releases the lock in memory visibility order.
  // Conceptually we need a #loadstore|#storestore "release" MEMBAR before
  // the ST of 0 into the lock-word which releases the lock, so fence
  // more than covers this on all platforms.
  *adr = 0;
}


void Threads::verify() {
  ALL_JAVA_THREADS(p) {
    p->verify();
  }
  VMThread* thread = VMThread::vm_thread();
  if (thread != NULL) thread->verify();
}

#ifndef PRODUCT
void JavaThread::verify_cross_modify_fence_failure(JavaThread *thread) {
   report_vm_error(__FILE__, __LINE__, "Cross modify fence failure", "%p", thread);
}
#endif

// Helper function to create the java.lang.Thread object for a
// VM-internal thread. The thread will have the given name, be
// part of the System ThreadGroup and if is_visible is true will be
// discoverable via the system ThreadGroup.
Handle JavaThread::create_system_thread_object(const char* name,
                                               bool is_visible, TRAPS) {
  Handle string = java_lang_String::create_from_str(name, CHECK_NH);

  // Initialize thread_oop to put it into the system threadGroup.
  // This is done by calling the Thread(ThreadGroup tg, String name)
  // constructor, which adds the new thread to the group as an unstarted
  // thread.
  Handle thread_group(THREAD, Universe::system_thread_group());
  Handle thread_oop =
    JavaCalls::construct_new_instance(vmClasses::Thread_klass(),
                                      vmSymbols::threadgroup_string_void_signature(),
                                      thread_group,
                                      string,
                                      CHECK_NH);

  // If the Thread is intended to be visible then we have to mimic what
  // Thread.start() would do, by adding it to its ThreadGroup: tg.add(t).
  if (is_visible) {
    Klass* group = vmClasses::ThreadGroup_klass();
    JavaValue result(T_VOID);
    JavaCalls::call_special(&result,
                            thread_group,
                            group,
                            vmSymbols::add_method_name(),
                            vmSymbols::thread_void_signature(),
                            thread_oop,
                            CHECK_NH);
  }

  return thread_oop;
}

// Starts the target JavaThread as a daemon of the given priority, and
// bound to the given java.lang.Thread instance.
// The Threads_lock is held for the duration.
void JavaThread::start_internal_daemon(JavaThread* current, JavaThread* target,
                                       Handle thread_oop, ThreadPriority prio) {

  assert(target->osthread() != NULL, "target thread is not properly initialized");

  MutexLocker mu(current, Threads_lock);

  // Initialize the fields of the thread_oop first.

  java_lang_Thread::set_thread(thread_oop(), target); // isAlive == true now

  if (prio != NoPriority) {
    java_lang_Thread::set_priority(thread_oop(), prio);
    // Note: we don't call os::set_priority here. Possibly we should,
    // else all threads should call it themselves when they first run.
  }

  java_lang_Thread::set_daemon(thread_oop());

  // Now bind the thread_oop to the target JavaThread.
  target->set_threadObj(thread_oop());

  Threads::add(target); // target is now visible for safepoint/handshake
  Thread::start(target);
}

void JavaThread::vm_exit_on_osthread_failure(JavaThread* thread) {
  // At this point it may be possible that no osthread was created for the
  // JavaThread due to lack of resources. However, since this must work
  // for critical system threads just check and abort if this fails.
  if (thread->osthread() == nullptr) {
    // This isn't really an OOM condition, but historically this is what
    // we report.
    vm_exit_during_initialization("java.lang.OutOfMemoryError",
                                  os::native_thread_creation_failed_msg());
  }
}
