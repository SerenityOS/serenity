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

#ifndef SHARE_RUNTIME_THREAD_HPP
#define SHARE_RUNTIME_THREAD_HPP

#include "jni.h"
#include "gc/shared/gcThreadLocalData.hpp"
#include "gc/shared/threadLocalAllocBuffer.hpp"
#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "oops/oopHandle.hpp"
#include "runtime/frame.hpp"
#include "runtime/globals.hpp"
#include "runtime/handshake.hpp"
#include "runtime/javaFrameAnchor.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/park.hpp"
#include "runtime/safepointMechanism.hpp"
#include "runtime/stackWatermarkSet.hpp"
#include "runtime/stackOverflow.hpp"
#include "runtime/threadHeapSampler.hpp"
#include "runtime/threadLocalStorage.hpp"
#include "runtime/threadStatisticalInfo.hpp"
#include "runtime/unhandledOops.hpp"
#include "utilities/align.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#if INCLUDE_JFR
#include "jfr/support/jfrThreadExtension.hpp"
#endif

class SafeThreadsListPtr;
class ThreadSafepointState;
class ThreadsList;
class ThreadsSMRSupport;

class JNIHandleBlock;
class JvmtiRawMonitor;
class JvmtiSampledObjectAllocEventCollector;
class JvmtiThreadState;
class JvmtiVMObjectAllocEventCollector;
class OSThread;
class ThreadStatistics;
class ConcurrentLocksDump;
class MonitorInfo;

class vframeArray;
class vframe;
class javaVFrame;

class DeoptResourceMark;
class JvmtiDeferredUpdates;

class ThreadClosure;
class ICRefillVerifier;

class Metadata;
class ResourceArea;

class OopStorage;

DEBUG_ONLY(class ResourceMark;)

class WorkerThread;

class JavaThread;

// Class hierarchy
// - Thread
//   - JavaThread
//     - various subclasses eg CompilerThread, ServiceThread
//   - NonJavaThread
//     - NamedThread
//       - VMThread
//       - ConcurrentGCThread
//       - WorkerThread
//         - GangWorker
//     - WatcherThread
//     - JfrThreadSampler
//     - LogAsyncWriter
//
// All Thread subclasses must be either JavaThread or NonJavaThread.
// This means !t->is_Java_thread() iff t is a NonJavaThread, or t is
// a partially constructed/destroyed Thread.

// Thread execution sequence and actions:
// All threads:
//  - thread_native_entry  // per-OS native entry point
//    - stack initialization
//    - other OS-level initialization (signal masks etc)
//    - handshake with creating thread (if not started suspended)
//    - this->call_run()  // common shared entry point
//      - shared common initialization
//      - this->pre_run()  // virtual per-thread-type initialization
//      - this->run()      // virtual per-thread-type "main" logic
//      - shared common tear-down
//      - this->post_run()  // virtual per-thread-type tear-down
//      - // 'this' no longer referenceable
//    - OS-level tear-down (minimal)
//    - final logging
//
// For JavaThread:
//   - this->run()  // virtual but not normally overridden
//     - this->thread_main_inner()  // extra call level to ensure correct stack calculations
//       - this->entry_point()  // set differently for each kind of JavaThread

class Thread: public ThreadShadow {
  friend class VMStructs;
  friend class JVMCIVMStructs;
 private:

#ifndef USE_LIBRARY_BASED_TLS_ONLY
  // Current thread is maintained as a thread-local variable
  static THREAD_LOCAL Thread* _thr_current;
#endif

  // Thread local data area available to the GC. The internal
  // structure and contents of this data area is GC-specific.
  // Only GC and GC barrier code should access this data area.
  GCThreadLocalData _gc_data;

 public:
  static ByteSize gc_data_offset() {
    return byte_offset_of(Thread, _gc_data);
  }

  template <typename T> T* gc_data() {
    STATIC_ASSERT(sizeof(T) <= sizeof(_gc_data));
    return reinterpret_cast<T*>(&_gc_data);
  }

  // Exception handling
  // (Note: _pending_exception and friends are in ThreadShadow)
  //oop       _pending_exception;                // pending exception for current thread
  // const char* _exception_file;                   // file information for exception (debugging only)
  // int         _exception_line;                   // line information for exception (debugging only)
 protected:

  DEBUG_ONLY(static Thread* _starting_thread;)

  // JavaThread lifecycle support:
  friend class SafeThreadsListPtr;  // for _threads_list_ptr, cmpxchg_threads_hazard_ptr(), {dec_,inc_,}nested_threads_hazard_ptr_cnt(), {g,s}et_threads_hazard_ptr(), inc_nested_handle_cnt(), tag_hazard_ptr() access
  friend class ScanHazardPtrGatherProtectedThreadsClosure;  // for cmpxchg_threads_hazard_ptr(), get_threads_hazard_ptr(), is_hazard_ptr_tagged() access
  friend class ScanHazardPtrGatherThreadsListClosure;  // for get_threads_hazard_ptr(), untag_hazard_ptr() access
  friend class ScanHazardPtrPrintMatchingThreadsClosure;  // for get_threads_hazard_ptr(), is_hazard_ptr_tagged() access
  friend class ThreadsSMRSupport;  // for _nested_threads_hazard_ptr_cnt, _threads_hazard_ptr, _threads_list_ptr access
  friend class ThreadsListHandleTest;  // for _nested_threads_hazard_ptr_cnt, _threads_hazard_ptr, _threads_list_ptr access
  friend class ValidateHazardPtrsClosure;  // for get_threads_hazard_ptr(), untag_hazard_ptr() access

  ThreadsList* volatile _threads_hazard_ptr;
  SafeThreadsListPtr*   _threads_list_ptr;
  ThreadsList*          cmpxchg_threads_hazard_ptr(ThreadsList* exchange_value, ThreadsList* compare_value);
  ThreadsList*          get_threads_hazard_ptr() const;
  void                  set_threads_hazard_ptr(ThreadsList* new_list);
  static bool           is_hazard_ptr_tagged(ThreadsList* list) {
    return (intptr_t(list) & intptr_t(1)) == intptr_t(1);
  }
  static ThreadsList*   tag_hazard_ptr(ThreadsList* list) {
    return (ThreadsList*)(intptr_t(list) | intptr_t(1));
  }
  static ThreadsList*   untag_hazard_ptr(ThreadsList* list) {
    return (ThreadsList*)(intptr_t(list) & ~intptr_t(1));
  }
  // This field is enabled via -XX:+EnableThreadSMRStatistics:
  uint _nested_threads_hazard_ptr_cnt;
  void dec_nested_threads_hazard_ptr_cnt() {
    assert(_nested_threads_hazard_ptr_cnt != 0, "mismatched {dec,inc}_nested_threads_hazard_ptr_cnt()");
    _nested_threads_hazard_ptr_cnt--;
  }
  void inc_nested_threads_hazard_ptr_cnt() {
    _nested_threads_hazard_ptr_cnt++;
  }
  uint nested_threads_hazard_ptr_cnt() {
    return _nested_threads_hazard_ptr_cnt;
  }

 public:
  // Is the target JavaThread protected by the calling Thread
  // or by some other mechanism:
  static bool is_JavaThread_protected(const JavaThread* p);

  void* operator new(size_t size) throw() { return allocate(size, true); }
  void* operator new(size_t size, const std::nothrow_t& nothrow_constant) throw() {
    return allocate(size, false); }
  void  operator delete(void* p);

 protected:
  static void* allocate(size_t size, bool throw_excpt, MEMFLAGS flags = mtThread);

 private:
  DEBUG_ONLY(bool _suspendible_thread;)

 public:
  // Determines if a heap allocation failure will be retried
  // (e.g., by deoptimizing and re-executing in the interpreter).
  // In this case, the failed allocation must raise
  // Universe::out_of_memory_error_retry() and omit side effects
  // such as JVMTI events and handling -XX:+HeapDumpOnOutOfMemoryError
  // and -XX:OnOutOfMemoryError.
  virtual bool in_retryable_allocation() const { return false; }

#ifdef ASSERT
  void set_suspendible_thread() {
    _suspendible_thread = true;
  }

  void clear_suspendible_thread() {
    _suspendible_thread = false;
  }

  bool is_suspendible_thread() { return _suspendible_thread; }
#endif

 private:
  // Active_handles points to a block of handles
  JNIHandleBlock* _active_handles;

  // One-element thread local free list
  JNIHandleBlock* _free_handle_block;

  // Point to the last handle mark
  HandleMark* _last_handle_mark;

  // Claim value for parallel iteration over threads.
  uintx _threads_do_token;

  // Support for GlobalCounter
 private:
  volatile uintx _rcu_counter;
 public:
  volatile uintx* get_rcu_counter() {
    return &_rcu_counter;
  }

 public:
  void set_last_handle_mark(HandleMark* mark)   { _last_handle_mark = mark; }
  HandleMark* last_handle_mark() const          { return _last_handle_mark; }
 private:

#ifdef ASSERT
  ICRefillVerifier* _missed_ic_stub_refill_verifier;

 public:
  ICRefillVerifier* missed_ic_stub_refill_verifier() {
    return _missed_ic_stub_refill_verifier;
  }

  void set_missed_ic_stub_refill_verifier(ICRefillVerifier* verifier) {
    _missed_ic_stub_refill_verifier = verifier;
  }
#endif // ASSERT

 private:
  // Used by SkipGCALot class.
  NOT_PRODUCT(bool _skip_gcalot;)               // Should we elide gc-a-lot?

  friend class GCLocker;

 private:
  ThreadLocalAllocBuffer _tlab;                 // Thread-local eden
  jlong _allocated_bytes;                       // Cumulative number of bytes allocated on
                                                // the Java heap
  ThreadHeapSampler _heap_sampler;              // For use when sampling the memory.

  ThreadStatisticalInfo _statistical_info;      // Statistics about the thread

  JFR_ONLY(DEFINE_THREAD_LOCAL_FIELD_JFR;)      // Thread-local data for jfr

  JvmtiRawMonitor* _current_pending_raw_monitor; // JvmtiRawMonitor this thread
                                                 // is waiting to lock
 public:
  // Constructor
  Thread();
  virtual ~Thread() = 0;        // Thread is abstract.

  // Manage Thread::current()
  void initialize_thread_current();
  static void clear_thread_current(); // TLS cleanup needed before threads terminate

 protected:
  // To be implemented by children.
  virtual void run() = 0;
  virtual void pre_run() = 0;
  virtual void post_run() = 0;  // Note: Thread must not be deleted prior to calling this!

#ifdef ASSERT
  enum RunState {
    PRE_CALL_RUN,
    CALL_RUN,
    PRE_RUN,
    RUN,
    POST_RUN
    // POST_CALL_RUN - can't define this one as 'this' may be deleted when we want to set it
  };
  RunState _run_state;  // for lifecycle checks
#endif


 public:
  // invokes <ChildThreadClass>::run(), with common preparations and cleanups.
  void call_run();

  // Testers
  virtual bool is_VM_thread()       const            { return false; }
  virtual bool is_Java_thread()     const            { return false; }
  virtual bool is_Compiler_thread() const            { return false; }
  virtual bool is_Code_cache_sweeper_thread() const  { return false; }
  virtual bool is_service_thread() const             { return false; }
  virtual bool is_monitor_deflation_thread() const   { return false; }
  virtual bool is_hidden_from_external_view() const  { return false; }
  virtual bool is_jvmti_agent_thread() const         { return false; }
  // True iff the thread can perform GC operations at a safepoint.
  // Generally will be true only of VM thread and parallel GC WorkGang
  // threads.
  virtual bool is_GC_task_thread() const             { return false; }
  virtual bool is_Watcher_thread() const             { return false; }
  virtual bool is_ConcurrentGC_thread() const        { return false; }
  virtual bool is_Named_thread() const               { return false; }
  virtual bool is_Worker_thread() const              { return false; }
  virtual bool is_JfrSampler_thread() const          { return false; }

  // Can this thread make Java upcalls
  virtual bool can_call_java() const                 { return false; }

  // Is this a JavaThread that is on the VM's current ThreadsList?
  // If so it must participate in the safepoint protocol.
  virtual bool is_active_Java_thread() const         { return false; }

  // All threads are given names. For singleton subclasses we can
  // just hard-wire the known name of the instance. JavaThreads and
  // NamedThreads support multiple named instances, and dynamic
  // changing of the name of an instance.
  virtual const char* name() const { return "Unknown thread"; }

  // A thread's type name is also made available for debugging
  // and logging.
  virtual const char* type_name() const { return "Thread"; }

  // Returns the current thread (ASSERTS if NULL)
  static inline Thread* current();
  // Returns the current thread, or NULL if not attached
  static inline Thread* current_or_null();
  // Returns the current thread, or NULL if not attached, and is
  // safe for use from signal-handlers
  static inline Thread* current_or_null_safe();

  // Common thread operations
#ifdef ASSERT
  static void check_for_dangling_thread_pointer(Thread *thread);
#endif
  static void set_priority(Thread* thread, ThreadPriority priority);
  static ThreadPriority get_priority(const Thread* const thread);
  static void start(Thread* thread);

  void set_native_thread_name(const char *name) {
    assert(Thread::current() == this, "set_native_thread_name can only be called on the current thread");
    os::set_native_thread_name(name);
  }

  // Support for Unhandled Oop detection
  // Add the field for both, fastdebug and debug, builds to keep
  // Thread's fields layout the same.
  // Note: CHECK_UNHANDLED_OOPS is defined only for fastdebug build.
#ifdef CHECK_UNHANDLED_OOPS
 private:
  UnhandledOops* _unhandled_oops;
#elif defined(ASSERT)
 private:
  void* _unhandled_oops;
#endif
#ifdef CHECK_UNHANDLED_OOPS
 public:
  UnhandledOops* unhandled_oops() { return _unhandled_oops; }
  // Mark oop safe for gc.  It may be stack allocated but won't move.
  void allow_unhandled_oop(oop *op) {
    if (CheckUnhandledOops) unhandled_oops()->allow_unhandled_oop(op);
  }
  // Clear oops at safepoint so crashes point to unhandled oop violator
  void clear_unhandled_oops() {
    if (CheckUnhandledOops) unhandled_oops()->clear_unhandled_oops();
  }
#endif // CHECK_UNHANDLED_OOPS

 public:
#ifndef PRODUCT
  bool skip_gcalot()           { return _skip_gcalot; }
  void set_skip_gcalot(bool v) { _skip_gcalot = v;    }
#endif

  // Resource area
  ResourceArea* resource_area() const            { return _resource_area; }
  void set_resource_area(ResourceArea* area)     { _resource_area = area; }

  OSThread* osthread() const                     { return _osthread;   }
  void set_osthread(OSThread* thread)            { _osthread = thread; }

  // JNI handle support
  JNIHandleBlock* active_handles() const         { return _active_handles; }
  void set_active_handles(JNIHandleBlock* block) { _active_handles = block; }
  JNIHandleBlock* free_handle_block() const      { return _free_handle_block; }
  void set_free_handle_block(JNIHandleBlock* block) { _free_handle_block = block; }

  // Internal handle support
  HandleArea* handle_area() const                { return _handle_area; }
  void set_handle_area(HandleArea* area)         { _handle_area = area; }

  GrowableArray<Metadata*>* metadata_handles() const          { return _metadata_handles; }
  void set_metadata_handles(GrowableArray<Metadata*>* handles){ _metadata_handles = handles; }

  // Thread-Local Allocation Buffer (TLAB) support
  ThreadLocalAllocBuffer& tlab()                 { return _tlab; }
  void initialize_tlab();

  jlong allocated_bytes()               { return _allocated_bytes; }
  void set_allocated_bytes(jlong value) { _allocated_bytes = value; }
  void incr_allocated_bytes(jlong size) { _allocated_bytes += size; }
  inline jlong cooked_allocated_bytes();

  ThreadHeapSampler& heap_sampler()     { return _heap_sampler; }

  ThreadStatisticalInfo& statistical_info() { return _statistical_info; }

  JFR_ONLY(DEFINE_THREAD_LOCAL_ACCESSOR_JFR;)

  // For tracking the Jvmti raw monitor the thread is pending on.
  JvmtiRawMonitor* current_pending_raw_monitor() {
    return _current_pending_raw_monitor;
  }
  void set_current_pending_raw_monitor(JvmtiRawMonitor* monitor) {
    _current_pending_raw_monitor = monitor;
  }

  // GC support
  // Apply "f->do_oop" to all root oops in "this".
  //   Used by JavaThread::oops_do.
  // Apply "cf->do_code_blob" (if !NULL) to all code blobs active in frames
  virtual void oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf);
  virtual void oops_do_frames(OopClosure* f, CodeBlobClosure* cf) {}
  void oops_do(OopClosure* f, CodeBlobClosure* cf);

  // Handles the parallel case for claim_threads_do.
 private:
  bool claim_par_threads_do(uintx claim_token);
 public:
  // Requires that "claim_token" is that of the current iteration.
  // If "is_par" is false, sets the token of "this" to
  // "claim_token", and returns "true".  If "is_par" is true,
  // uses an atomic instruction to set the current thread's token to
  // "claim_token", if it is not already.  Returns "true" iff the
  // calling thread does the update, this indicates that the calling thread
  // has claimed the thread in the current iteration.
  bool claim_threads_do(bool is_par, uintx claim_token) {
    if (!is_par) {
      _threads_do_token = claim_token;
      return true;
    } else {
      return claim_par_threads_do(claim_token);
    }
  }

  uintx threads_do_token() const { return _threads_do_token; }

  // jvmtiRedefineClasses support
  void metadata_handles_do(void f(Metadata*));

 private:
  // Check if address is within the given range of this thread's
  // stack:  stack_base() > adr >/>= limit
  // The check is inclusive of limit if passed true, else exclusive.
  bool is_in_stack_range(address adr, address limit, bool inclusive) const {
    assert(stack_base() > limit && limit >= stack_end(), "limit is outside of stack");
    return stack_base() > adr && (inclusive ? adr >= limit : adr > limit);
  }

 public:
  // Used by fast lock support
  virtual bool is_lock_owned(address adr) const;

  // Check if address is within the given range of this thread's
  // stack:  stack_base() > adr >= limit
  bool is_in_stack_range_incl(address adr, address limit) const {
    return is_in_stack_range(adr, limit, true);
  }

  // Check if address is within the given range of this thread's
  // stack:  stack_base() > adr > limit
  bool is_in_stack_range_excl(address adr, address limit) const {
    return is_in_stack_range(adr, limit, false);
  }

  // Check if address is in the stack mapped to this thread. Used mainly in
  // error reporting (so has to include guard zone) and frame printing.
  // Expects _stack_base to be initialized - checked with assert.
  bool is_in_full_stack_checked(address adr) const {
    return is_in_stack_range_incl(adr, stack_end());
  }

  // Like is_in_full_stack_checked but without the assertions as this
  // may be called in a thread before _stack_base is initialized.
  bool is_in_full_stack(address adr) const {
    address stack_end = _stack_base - _stack_size;
    return _stack_base > adr && adr >= stack_end;
  }

  // Check if address is in the live stack of this thread (not just for locks).
  // Warning: can only be called by the current thread on itself.
  bool is_in_live_stack(address adr) const {
    assert(Thread::current() == this, "is_in_live_stack can only be called from current thread");
    return is_in_stack_range_incl(adr, os::current_stack_pointer());
  }

  // Sets this thread as starting thread. Returns failure if thread
  // creation fails due to lack of memory, too many threads etc.
  bool set_as_starting_thread();

protected:
  // OS data associated with the thread
  OSThread* _osthread;  // Platform-specific thread information

  // Thread local resource area for temporary allocation within the VM
  ResourceArea* _resource_area;

  DEBUG_ONLY(ResourceMark* _current_resource_mark;)

  // Thread local handle area for allocation of handles within the VM
  HandleArea* _handle_area;
  GrowableArray<Metadata*>* _metadata_handles;

  // Support for stack overflow handling, get_thread, etc.
  address          _stack_base;
  size_t           _stack_size;
  int              _lgrp_id;

 public:
  // Stack overflow support
  address stack_base() const           { assert(_stack_base != NULL,"Sanity check"); return _stack_base; }
  void    set_stack_base(address base) { _stack_base = base; }
  size_t  stack_size() const           { return _stack_size; }
  void    set_stack_size(size_t size)  { _stack_size = size; }
  address stack_end()  const           { return stack_base() - stack_size(); }
  void    record_stack_base_and_size();
  void    register_thread_stack_with_NMT() NOT_NMT_RETURN;
  void    unregister_thread_stack_with_NMT() NOT_NMT_RETURN;

  int     lgrp_id() const        { return _lgrp_id; }
  void    set_lgrp_id(int value) { _lgrp_id = value; }

  // Printing
  void print_on(outputStream* st, bool print_extended_info) const;
  virtual void print_on(outputStream* st) const { print_on(st, false); }
  void print() const;
  virtual void print_on_error(outputStream* st, char* buf, int buflen) const;
  // Basic, non-virtual, printing support that is simple and always safe.
  void print_value_on(outputStream* st) const;

  // Debug-only code
#ifdef ASSERT
 private:
  // Deadlock detection support for Mutex locks. List of locks own by thread.
  Mutex* _owned_locks;
  // Mutex::set_owner_implementation is the only place where _owned_locks is modified,
  // thus the friendship
  friend class Mutex;
  friend class Monitor;

 public:
  void print_owned_locks_on(outputStream* st) const;
  void print_owned_locks() const                 { print_owned_locks_on(tty);    }
  Mutex* owned_locks() const                     { return _owned_locks;          }
  bool owns_locks() const                        { return owned_locks() != NULL; }

  // Deadlock detection
  ResourceMark* current_resource_mark()          { return _current_resource_mark; }
  void set_current_resource_mark(ResourceMark* rm) { _current_resource_mark = rm; }
#endif // ASSERT

 private:
  volatile int _jvmti_env_iteration_count;

 public:
  void entering_jvmti_env_iteration()            { ++_jvmti_env_iteration_count; }
  void leaving_jvmti_env_iteration()             { --_jvmti_env_iteration_count; }
  bool is_inside_jvmti_env_iteration()           { return _jvmti_env_iteration_count > 0; }

  // Code generation
  static ByteSize exception_file_offset()        { return byte_offset_of(Thread, _exception_file); }
  static ByteSize exception_line_offset()        { return byte_offset_of(Thread, _exception_line); }
  static ByteSize active_handles_offset()        { return byte_offset_of(Thread, _active_handles); }

  static ByteSize stack_base_offset()            { return byte_offset_of(Thread, _stack_base); }
  static ByteSize stack_size_offset()            { return byte_offset_of(Thread, _stack_size); }

  static ByteSize tlab_start_offset()            { return byte_offset_of(Thread, _tlab) + ThreadLocalAllocBuffer::start_offset(); }
  static ByteSize tlab_end_offset()              { return byte_offset_of(Thread, _tlab) + ThreadLocalAllocBuffer::end_offset(); }
  static ByteSize tlab_top_offset()              { return byte_offset_of(Thread, _tlab) + ThreadLocalAllocBuffer::top_offset(); }
  static ByteSize tlab_pf_top_offset()           { return byte_offset_of(Thread, _tlab) + ThreadLocalAllocBuffer::pf_top_offset(); }

  static ByteSize allocated_bytes_offset()       { return byte_offset_of(Thread, _allocated_bytes); }

  JFR_ONLY(DEFINE_THREAD_LOCAL_OFFSET_JFR;)

 public:
  ParkEvent * volatile _ParkEvent;            // for Object monitors, JVMTI raw monitors,
                                              // and ObjectSynchronizer::read_stable_mark

  // Termination indicator used by the signal handler.
  // _ParkEvent is just a convenient field we can NULL out after setting the JavaThread termination state
  // (which can't itself be read from the signal handler if a signal hits during the Thread destructor).
  bool has_terminated()                       { return Atomic::load(&_ParkEvent) == NULL; };

  jint _hashStateW;                           // Marsaglia Shift-XOR thread-local RNG
  jint _hashStateX;                           // thread-specific hashCode generator state
  jint _hashStateY;
  jint _hashStateZ;

  // Low-level leaf-lock primitives used to implement synchronization.
  // Not for general synchronization use.
  static void SpinAcquire(volatile int * Lock, const char * Name);
  static void SpinRelease(volatile int * Lock);

#if defined(__APPLE__) && defined(AARCH64)
 private:
  DEBUG_ONLY(bool _wx_init);
  WXMode _wx_state;
 public:
  void init_wx();
  WXMode enable_wx(WXMode new_state);

  void assert_wx_state(WXMode expected) {
    assert(_wx_state == expected, "wrong state");
  }
#endif // __APPLE__ && AARCH64
};

// Inline implementation of Thread::current()
inline Thread* Thread::current() {
  Thread* current = current_or_null();
  assert(current != NULL, "Thread::current() called on detached thread");
  return current;
}

inline Thread* Thread::current_or_null() {
#ifndef USE_LIBRARY_BASED_TLS_ONLY
  return _thr_current;
#else
  if (ThreadLocalStorage::is_initialized()) {
    return ThreadLocalStorage::thread();
  }
  return NULL;
#endif
}

inline Thread* Thread::current_or_null_safe() {
  if (ThreadLocalStorage::is_initialized()) {
    return ThreadLocalStorage::thread();
  }
  return NULL;
}

class CompilerThread;

typedef void (*ThreadFunction)(JavaThread*, TRAPS);

class JavaThread: public Thread {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class WhiteBox;
  friend class ThreadsSMRSupport; // to access _threadObj for exiting_threads_oops_do
  friend class HandshakeState;
 private:
  bool           _on_thread_list;                // Is set when this JavaThread is added to the Threads list
  OopHandle      _threadObj;                     // The Java level thread object

#ifdef ASSERT
 private:
  int _java_call_counter;

 public:
  int  java_call_counter()                       { return _java_call_counter; }
  void inc_java_call_counter()                   { _java_call_counter++; }
  void dec_java_call_counter() {
    assert(_java_call_counter > 0, "Invalid nesting of JavaCallWrapper");
    _java_call_counter--;
  }
 private:  // restore original namespace restriction
#endif  // ifdef ASSERT

  JavaFrameAnchor _anchor;                       // Encapsulation of current java frame and it state

  ThreadFunction _entry_point;

  JNIEnv        _jni_environment;

  // Deopt support
  DeoptResourceMark*  _deopt_mark;               // Holds special ResourceMark for deoptimization

  CompiledMethod*       _deopt_nmethod;         // CompiledMethod that is currently being deoptimized
  vframeArray*  _vframe_array_head;              // Holds the heap of the active vframeArrays
  vframeArray*  _vframe_array_last;              // Holds last vFrameArray we popped
  // Holds updates by JVMTI agents for compiled frames that cannot be performed immediately. They
  // will be carried out as soon as possible which, in most cases, is just before deoptimization of
  // the frame, when control returns to it.
  JvmtiDeferredUpdates* _jvmti_deferred_updates;

  // Handshake value for fixing 6243940. We need a place for the i2c
  // adapter to store the callee Method*. This value is NEVER live
  // across a gc point so it does NOT have to be gc'd
  // The handshake is open ended since we can't be certain that it will
  // be NULLed. This is because we rarely ever see the race and end up
  // in handle_wrong_method which is the backend of the handshake. See
  // code in i2c adapters and handle_wrong_method.

  Method*       _callee_target;

  // Used to pass back results to the interpreter or generated code running Java code.
  oop           _vm_result;    // oop result is GC-preserved
  Metadata*     _vm_result_2;  // non-oop result

  // See ReduceInitialCardMarks: this holds the precise space interval of
  // the most recent slow path allocation for which compiled code has
  // elided card-marks for performance along the fast-path.
  MemRegion     _deferred_card_mark;

  ObjectMonitor* volatile _current_pending_monitor;     // ObjectMonitor this thread is waiting to lock
  bool           _current_pending_monitor_is_from_java; // locking is from Java code
  ObjectMonitor* volatile _current_waiting_monitor;     // ObjectMonitor on which this thread called Object.wait()
 public:
  volatile intptr_t _Stalled;

  // For tracking the heavyweight monitor the thread is pending on.
  ObjectMonitor* current_pending_monitor() {
    // Use Atomic::load() to prevent data race between concurrent modification and
    // concurrent readers, e.g. ThreadService::get_current_contended_monitor().
    // Especially, reloading pointer from thread after NULL check must be prevented.
    return Atomic::load(&_current_pending_monitor);
  }
  void set_current_pending_monitor(ObjectMonitor* monitor) {
    Atomic::store(&_current_pending_monitor, monitor);
  }
  void set_current_pending_monitor_is_from_java(bool from_java) {
    _current_pending_monitor_is_from_java = from_java;
  }
  bool current_pending_monitor_is_from_java() {
    return _current_pending_monitor_is_from_java;
  }
  ObjectMonitor* current_waiting_monitor() {
    // See the comment in current_pending_monitor() above.
    return Atomic::load(&_current_waiting_monitor);
  }
  void set_current_waiting_monitor(ObjectMonitor* monitor) {
    Atomic::store(&_current_waiting_monitor, monitor);
  }

 private:
  MonitorChunk* _monitor_chunks;              // Contains the off stack monitors
                                              // allocated during deoptimization
                                              // and by JNI_MonitorEnter/Exit

  enum SuspendFlags {
    // NOTE: avoid using the sign-bit as cc generates different test code
    //       when the sign-bit is used, and sometimes incorrectly - see CR 6398077
    _has_async_exception    = 0x00000001U, // there is a pending async exception
    _trace_flag             = 0x00000004U, // call tracing backend
    _obj_deopt              = 0x00000008U  // suspend for object reallocation and relocking for JVMTI agent
  };

  // various suspension related flags - atomically updated
  // overloaded with async exceptions so that we do a single check when transitioning from native->Java
  volatile uint32_t _suspend_flags;

  inline void set_suspend_flag(SuspendFlags f);
  inline void clear_suspend_flag(SuspendFlags f);

 public:
  inline void set_trace_flag();
  inline void clear_trace_flag();
  inline void set_obj_deopt_flag();
  inline void clear_obj_deopt_flag();
  bool is_trace_suspend()      { return (_suspend_flags & _trace_flag) != 0; }
  bool is_obj_deopt_suspend()  { return (_suspend_flags & _obj_deopt) != 0; }

  // Asynchronous exceptions support
 private:
  enum AsyncExceptionCondition {
    _no_async_condition = 0,
    _async_exception,
    _async_unsafe_access_error
  };
  AsyncExceptionCondition _async_exception_condition;
  oop                     _pending_async_exception;

  void set_async_exception_condition(AsyncExceptionCondition aec) { _async_exception_condition = aec; }
  AsyncExceptionCondition clear_async_exception_condition() {
    AsyncExceptionCondition x = _async_exception_condition;
    _async_exception_condition = _no_async_condition;
    return x;
  }

 public:
  bool has_async_exception_condition(bool check_unsafe_access_error = true) {
    return check_unsafe_access_error ? _async_exception_condition != _no_async_condition
                                     : _async_exception_condition == _async_exception;
  }
  inline void set_pending_async_exception(oop e);
  void set_pending_unsafe_access_error()  {
    // Don't overwrite an asynchronous exception sent by another thread
    if (_async_exception_condition == _no_async_condition) {
      set_async_exception_condition(_async_unsafe_access_error);
    }
  }
  void check_and_handle_async_exceptions();
  // Installs a pending exception to be inserted later
  static void send_async_exception(oop thread_oop, oop java_throwable);
  void send_thread_stop(oop throwable);

  // Safepoint support
 public:                                                        // Expose _thread_state for SafeFetchInt()
  volatile JavaThreadState _thread_state;
 private:
  SafepointMechanism::ThreadData _poll_data;
  ThreadSafepointState*          _safepoint_state;              // Holds information about a thread during a safepoint
  address                        _saved_exception_pc;           // Saved pc of instruction where last implicit exception happened
  NOT_PRODUCT(bool               _requires_cross_modify_fence;) // State used by VerifyCrossModifyFence
#ifdef ASSERT
  // Debug support for checking if code allows safepoints or not.
  // Safepoints in the VM can happen because of allocation, invoking a VM operation, or blocking on
  // mutex, or blocking on an object synchronizer (Java locking).
  // If _no_safepoint_count is non-zero, then an assertion failure will happen in any of
  // the above cases. The class NoSafepointVerifier is used to set this counter.
  int _no_safepoint_count;                             // If 0, thread allow a safepoint to happen

 public:
  void inc_no_safepoint_count() { _no_safepoint_count++; }
  void dec_no_safepoint_count() { _no_safepoint_count--; }
#endif // ASSERT
 public:
  // These functions check conditions before possibly going to a safepoint.
  // including NoSafepointVerifier.
  void check_for_valid_safepoint_state() NOT_DEBUG_RETURN;
  void check_possible_safepoint()        NOT_DEBUG_RETURN;

#ifdef ASSERT
 private:
  volatile uint64_t _visited_for_critical_count;

 public:
  void set_visited_for_critical_count(uint64_t safepoint_id) {
    assert(_visited_for_critical_count == 0, "Must be reset before set");
    assert((safepoint_id & 0x1) == 1, "Must be odd");
    _visited_for_critical_count = safepoint_id;
  }
  void reset_visited_for_critical_count(uint64_t safepoint_id) {
    assert(_visited_for_critical_count == safepoint_id, "Was not visited");
    _visited_for_critical_count = 0;
  }
  bool was_visited_for_critical_count(uint64_t safepoint_id) const {
    return _visited_for_critical_count == safepoint_id;
  }
#endif // ASSERT

  // JavaThread termination support
 public:
  enum TerminatedTypes {
    _not_terminated = 0xDEAD - 2,
    _thread_exiting,                             // JavaThread::exit() has been called for this thread
    _thread_terminated,                          // JavaThread is removed from thread list
    _vm_exited                                   // JavaThread is still executing native code, but VM is terminated
                                                 // only VM_Exit can set _vm_exited
  };

 private:
  // In general a JavaThread's _terminated field transitions as follows:
  //
  //   _not_terminated => _thread_exiting => _thread_terminated
  //
  // _vm_exited is a special value to cover the case of a JavaThread
  // executing native code after the VM itself is terminated.
  volatile TerminatedTypes _terminated;

  jint                  _in_deopt_handler;       // count of deoptimization
                                                 // handlers thread is in
  volatile bool         _doing_unsafe_access;    // Thread may fault due to unsafe access
  bool                  _do_not_unlock_if_synchronized;  // Do not unlock the receiver of a synchronized method (since it was
                                                         // never locked) when throwing an exception. Used by interpreter only.

  // JNI attach states:
  enum JNIAttachStates {
    _not_attaching_via_jni = 1,  // thread is not attaching via JNI
    _attaching_via_jni,          // thread is attaching via JNI
    _attached_via_jni            // thread has attached via JNI
  };

  // A regular JavaThread's _jni_attach_state is _not_attaching_via_jni.
  // A native thread that is attaching via JNI starts with a value
  // of _attaching_via_jni and transitions to _attached_via_jni.
  volatile JNIAttachStates _jni_attach_state;


#if INCLUDE_JVMCI
  // The _pending_* fields below are used to communicate extra information
  // from an uncommon trap in JVMCI compiled code to the uncommon trap handler.

  // Communicates the DeoptReason and DeoptAction of the uncommon trap
  int       _pending_deoptimization;

  // Specifies whether the uncommon trap is to bci 0 of a synchronized method
  // before the monitor has been acquired.
  bool      _pending_monitorenter;

  // Specifies if the DeoptReason for the last uncommon trap was Reason_transfer_to_interpreter
  bool      _pending_transfer_to_interpreter;

  // True if in a runtime call from compiled code that will deoptimize
  // and re-execute a failed heap allocation in the interpreter.
  bool      _in_retryable_allocation;

  // An id of a speculation that JVMCI compiled code can use to further describe and
  // uniquely identify the speculative optimization guarded by an uncommon trap.
  // See JVMCINMethodData::SPECULATION_LENGTH_BITS for further details.
  jlong     _pending_failed_speculation;

  // These fields are mutually exclusive in terms of live ranges.
  union {
    // Communicates the pc at which the most recent implicit exception occurred
    // from the signal handler to a deoptimization stub.
    address   _implicit_exception_pc;

    // Communicates an alternative call target to an i2c stub from a JavaCall .
    address   _alternate_call_target;
  } _jvmci;

  // Support for high precision, thread sensitive counters in JVMCI compiled code.
  jlong*    _jvmci_counters;

  // Fast thread locals for use by JVMCI
  intptr_t*  _jvmci_reserved0;
  intptr_t*  _jvmci_reserved1;
  oop        _jvmci_reserved_oop0;

 public:
  static jlong* _jvmci_old_thread_counters;
  static void collect_counters(jlong* array, int length);

  bool resize_counters(int current_size, int new_size);

  static bool resize_all_jvmci_counters(int new_size);

 private:
#endif // INCLUDE_JVMCI

  StackOverflow    _stack_overflow_state;

  // Compiler exception handling (NOTE: The _exception_oop is *NOT* the same as _pending_exception. It is
  // used to temp. parsing values into and out of the runtime system during exception handling for compiled
  // code)
  volatile oop     _exception_oop;               // Exception thrown in compiled code
  volatile address _exception_pc;                // PC where exception happened
  volatile address _exception_handler_pc;        // PC for handler of exception
  volatile int     _is_method_handle_return;     // true (== 1) if the current exception PC is a MethodHandle call site.

 private:
  // support for JNI critical regions
  jint    _jni_active_critical;                  // count of entries into JNI critical region

  // Checked JNI: function name requires exception check
  char* _pending_jni_exception_check_fn;

  // For deadlock detection.
  int _depth_first_number;

  // JVMTI PopFrame support
  // This is set to popframe_pending to signal that top Java frame should be popped immediately
  int _popframe_condition;

  // If reallocation of scalar replaced objects fails, we throw OOM
  // and during exception propagation, pop the top
  // _frames_to_pop_failed_realloc frames, the ones that reference
  // failed reallocations.
  int _frames_to_pop_failed_realloc;

  friend class VMThread;
  friend class ThreadWaitTransition;
  friend class VM_Exit;

  // Stack watermark barriers.
  StackWatermarks _stack_watermarks;

 public:
  inline StackWatermarks* stack_watermarks() { return &_stack_watermarks; }

 public:
  // Constructor
  JavaThread();                            // delegating constructor
  JavaThread(bool is_attaching_via_jni);   // for main thread and JNI attached threads
  JavaThread(ThreadFunction entry_point, size_t stack_size = 0);
  ~JavaThread();

#ifdef ASSERT
  // verify this JavaThread hasn't be published in the Threads::list yet
  void verify_not_published();
#endif // ASSERT

  StackOverflow* stack_overflow_state() { return &_stack_overflow_state; }

  //JNI functiontable getter/setter for JVMTI jni function table interception API.
  void set_jni_functions(struct JNINativeInterface_* functionTable) {
    _jni_environment.functions = functionTable;
  }
  struct JNINativeInterface_* get_jni_functions() {
    return (struct JNINativeInterface_ *)_jni_environment.functions;
  }

  // This function is called at thread creation to allow
  // platform specific thread variables to be initialized.
  void cache_global_variables();

  // Executes Shutdown.shutdown()
  void invoke_shutdown_hooks();

  // Cleanup on thread exit
  enum ExitType {
    normal_exit,
    jni_detach
  };
  void exit(bool destroy_vm, ExitType exit_type = normal_exit);

  void cleanup_failed_attach_current_thread(bool is_daemon);

  // Testers
  virtual bool is_Java_thread() const            { return true;  }
  virtual bool can_call_java() const             { return true; }

  virtual bool is_active_Java_thread() const {
    return on_thread_list() && !is_terminated();
  }

  // Thread oop. threadObj() can be NULL for initial JavaThread
  // (or for threads attached via JNI)
  oop threadObj() const;
  void set_threadObj(oop p);

  // Prepare thread and add to priority queue.  If a priority is
  // not specified, use the priority of the thread object. Threads_lock
  // must be held while this function is called.
  void prepare(jobject jni_thread, ThreadPriority prio=NoPriority);

  void set_saved_exception_pc(address pc)        { _saved_exception_pc = pc; }
  address saved_exception_pc()                   { return _saved_exception_pc; }

  ThreadFunction entry_point() const             { return _entry_point; }

  // Allocates a new Java level thread object for this thread. thread_name may be NULL.
  void allocate_threadObj(Handle thread_group, const char* thread_name, bool daemon, TRAPS);

  // Last frame anchor routines

  JavaFrameAnchor* frame_anchor(void)            { return &_anchor; }

  // last_Java_sp
  bool has_last_Java_frame() const               { return _anchor.has_last_Java_frame(); }
  intptr_t* last_Java_sp() const                 { return _anchor.last_Java_sp(); }

  // last_Java_pc

  address last_Java_pc(void)                     { return _anchor.last_Java_pc(); }

  // Safepoint support
  inline JavaThreadState thread_state() const;
  inline void set_thread_state(JavaThreadState s);
  inline void set_thread_state_fence(JavaThreadState s);  // fence after setting thread state
  inline ThreadSafepointState* safepoint_state() const;
  inline void set_safepoint_state(ThreadSafepointState* state);
  inline bool is_at_poll_safepoint();

  // JavaThread termination and lifecycle support:
  void smr_delete();
  bool on_thread_list() const { return _on_thread_list; }
  void set_on_thread_list() { _on_thread_list = true; }

  // thread has called JavaThread::exit() or is terminated
  bool is_exiting() const;
  // thread is terminated (no longer on the threads list); we compare
  // against the two non-terminated values so that a freed JavaThread
  // will also be considered terminated.
  bool check_is_terminated(TerminatedTypes l_terminated) const {
    return l_terminated != _not_terminated && l_terminated != _thread_exiting;
  }
  bool is_terminated() const;
  void set_terminated(TerminatedTypes t);

  void block_if_vm_exited();

  bool doing_unsafe_access()                     { return _doing_unsafe_access; }
  void set_doing_unsafe_access(bool val)         { _doing_unsafe_access = val; }

  bool do_not_unlock_if_synchronized()             { return _do_not_unlock_if_synchronized; }
  void set_do_not_unlock_if_synchronized(bool val) { _do_not_unlock_if_synchronized = val; }

  SafepointMechanism::ThreadData* poll_data() { return &_poll_data; }

  void set_requires_cross_modify_fence(bool val) PRODUCT_RETURN NOT_PRODUCT({ _requires_cross_modify_fence = val; })

 private:
  DEBUG_ONLY(void verify_frame_info();)

  // Support for thread handshake operations
  HandshakeState _handshake;
 public:
  HandshakeState* handshake_state() { return &_handshake; }

  // A JavaThread can always safely operate on it self and other threads
  // can do it safely if they are the active handshaker.
  bool is_handshake_safe_for(Thread* th) const {
    return _handshake.active_handshaker() == th || this == th;
  }

  // Suspend/resume support for JavaThread
  bool java_suspend(); // higher-level suspension logic called by the public APIs
  bool java_resume();  // higher-level resume logic called by the public APIs
  bool is_suspended()     { return _handshake.is_suspended(); }

  // Check for async exception in addition to safepoint.
  static void check_special_condition_for_native_trans(JavaThread *thread);

  // Synchronize with another thread that is deoptimizing objects of the
  // current thread, i.e. reverts optimizations based on escape analysis.
  void wait_for_object_deoptimization();

  // these next two are also used for self-suspension and async exception support
  void handle_special_runtime_exit_condition(bool check_asyncs = true);

  // Return true if JavaThread has an asynchronous condition or
  // if external suspension is requested.
  bool has_special_runtime_exit_condition() {
    return (_async_exception_condition != _no_async_condition) ||
           (_suspend_flags & (_obj_deopt JFR_ONLY(| _trace_flag))) != 0;
  }

  // Fast-locking support
  bool is_lock_owned(address adr) const;

  // Accessors for vframe array top
  // The linked list of vframe arrays are sorted on sp. This means when we
  // unpack the head must contain the vframe array to unpack.
  void set_vframe_array_head(vframeArray* value) { _vframe_array_head = value; }
  vframeArray* vframe_array_head() const         { return _vframe_array_head;  }

  // Side structure for deferring update of java frame locals until deopt occurs
  JvmtiDeferredUpdates* deferred_updates() const      { return _jvmti_deferred_updates; }
  void set_deferred_updates(JvmtiDeferredUpdates* du) { _jvmti_deferred_updates = du; }

  // These only really exist to make debugging deopt problems simpler

  void set_vframe_array_last(vframeArray* value) { _vframe_array_last = value; }
  vframeArray* vframe_array_last() const         { return _vframe_array_last;  }

  // The special resourceMark used during deoptimization

  void set_deopt_mark(DeoptResourceMark* value)  { _deopt_mark = value; }
  DeoptResourceMark* deopt_mark(void)            { return _deopt_mark; }

  void set_deopt_compiled_method(CompiledMethod* nm)  { _deopt_nmethod = nm; }
  CompiledMethod* deopt_compiled_method()        { return _deopt_nmethod; }

  Method*    callee_target() const               { return _callee_target; }
  void set_callee_target  (Method* x)          { _callee_target   = x; }

  // Oop results of vm runtime calls
  oop  vm_result() const                         { return _vm_result; }
  void set_vm_result  (oop x)                    { _vm_result   = x; }

  Metadata*    vm_result_2() const               { return _vm_result_2; }
  void set_vm_result_2  (Metadata* x)          { _vm_result_2   = x; }

  MemRegion deferred_card_mark() const           { return _deferred_card_mark; }
  void set_deferred_card_mark(MemRegion mr)      { _deferred_card_mark = mr;   }

#if INCLUDE_JVMCI
  int  pending_deoptimization() const             { return _pending_deoptimization; }
  jlong pending_failed_speculation() const        { return _pending_failed_speculation; }
  bool has_pending_monitorenter() const           { return _pending_monitorenter; }
  void set_pending_monitorenter(bool b)           { _pending_monitorenter = b; }
  void set_pending_deoptimization(int reason)     { _pending_deoptimization = reason; }
  void set_pending_failed_speculation(jlong failed_speculation) { _pending_failed_speculation = failed_speculation; }
  void set_pending_transfer_to_interpreter(bool b) { _pending_transfer_to_interpreter = b; }
  void set_jvmci_alternate_call_target(address a) { assert(_jvmci._alternate_call_target == NULL, "must be"); _jvmci._alternate_call_target = a; }
  void set_jvmci_implicit_exception_pc(address a) { assert(_jvmci._implicit_exception_pc == NULL, "must be"); _jvmci._implicit_exception_pc = a; }

  virtual bool in_retryable_allocation() const    { return _in_retryable_allocation; }
  void set_in_retryable_allocation(bool b)        { _in_retryable_allocation = b; }
#endif // INCLUDE_JVMCI

  // Exception handling for compiled methods
  oop      exception_oop() const;
  address  exception_pc() const                  { return _exception_pc; }
  address  exception_handler_pc() const          { return _exception_handler_pc; }
  bool     is_method_handle_return() const       { return _is_method_handle_return == 1; }

  void set_exception_oop(oop o);
  void set_exception_pc(address a)               { _exception_pc = a; }
  void set_exception_handler_pc(address a)       { _exception_handler_pc = a; }
  void set_is_method_handle_return(bool value)   { _is_method_handle_return = value ? 1 : 0; }

  void clear_exception_oop_and_pc() {
    set_exception_oop(NULL);
    set_exception_pc(NULL);
  }

  // Check if address is in the usable part of the stack (excludes protected
  // guard pages). Can be applied to any thread and is an approximation for
  // using is_in_live_stack when the query has to happen from another thread.
  bool is_in_usable_stack(address adr) const {
    return is_in_stack_range_incl(adr, _stack_overflow_state.stack_reserved_zone_base());
  }

  // Misc. accessors/mutators
  void set_do_not_unlock(void)                   { _do_not_unlock_if_synchronized = true; }
  void clr_do_not_unlock(void)                   { _do_not_unlock_if_synchronized = false; }
  bool do_not_unlock(void)                       { return _do_not_unlock_if_synchronized; }

  // For assembly stub generation
  static ByteSize threadObj_offset()             { return byte_offset_of(JavaThread, _threadObj); }
  static ByteSize jni_environment_offset()       { return byte_offset_of(JavaThread, _jni_environment); }
  static ByteSize pending_jni_exception_check_fn_offset() {
    return byte_offset_of(JavaThread, _pending_jni_exception_check_fn);
  }
  static ByteSize last_Java_sp_offset() {
    return byte_offset_of(JavaThread, _anchor) + JavaFrameAnchor::last_Java_sp_offset();
  }
  static ByteSize last_Java_pc_offset() {
    return byte_offset_of(JavaThread, _anchor) + JavaFrameAnchor::last_Java_pc_offset();
  }
  static ByteSize frame_anchor_offset() {
    return byte_offset_of(JavaThread, _anchor);
  }
  static ByteSize callee_target_offset()         { return byte_offset_of(JavaThread, _callee_target); }
  static ByteSize vm_result_offset()             { return byte_offset_of(JavaThread, _vm_result); }
  static ByteSize vm_result_2_offset()           { return byte_offset_of(JavaThread, _vm_result_2); }
  static ByteSize thread_state_offset()          { return byte_offset_of(JavaThread, _thread_state); }
  static ByteSize polling_word_offset()          { return byte_offset_of(JavaThread, _poll_data) + byte_offset_of(SafepointMechanism::ThreadData, _polling_word);}
  static ByteSize polling_page_offset()          { return byte_offset_of(JavaThread, _poll_data) + byte_offset_of(SafepointMechanism::ThreadData, _polling_page);}
  static ByteSize saved_exception_pc_offset()    { return byte_offset_of(JavaThread, _saved_exception_pc); }
  static ByteSize osthread_offset()              { return byte_offset_of(JavaThread, _osthread); }
#if INCLUDE_JVMCI
  static ByteSize pending_deoptimization_offset() { return byte_offset_of(JavaThread, _pending_deoptimization); }
  static ByteSize pending_monitorenter_offset()  { return byte_offset_of(JavaThread, _pending_monitorenter); }
  static ByteSize pending_failed_speculation_offset() { return byte_offset_of(JavaThread, _pending_failed_speculation); }
  static ByteSize jvmci_alternate_call_target_offset() { return byte_offset_of(JavaThread, _jvmci._alternate_call_target); }
  static ByteSize jvmci_implicit_exception_pc_offset() { return byte_offset_of(JavaThread, _jvmci._implicit_exception_pc); }
  static ByteSize jvmci_counters_offset()        { return byte_offset_of(JavaThread, _jvmci_counters); }
#endif // INCLUDE_JVMCI
  static ByteSize exception_oop_offset()         { return byte_offset_of(JavaThread, _exception_oop); }
  static ByteSize exception_pc_offset()          { return byte_offset_of(JavaThread, _exception_pc); }
  static ByteSize exception_handler_pc_offset()  { return byte_offset_of(JavaThread, _exception_handler_pc); }
  static ByteSize is_method_handle_return_offset() { return byte_offset_of(JavaThread, _is_method_handle_return); }

  // StackOverflow offsets
  static ByteSize stack_overflow_limit_offset()  {
    return byte_offset_of(JavaThread, _stack_overflow_state._stack_overflow_limit);
  }
  static ByteSize stack_guard_state_offset()     {
    return byte_offset_of(JavaThread, _stack_overflow_state._stack_guard_state);
  }
  static ByteSize reserved_stack_activation_offset() {
    return byte_offset_of(JavaThread, _stack_overflow_state._reserved_stack_activation);
  }

  static ByteSize suspend_flags_offset()         { return byte_offset_of(JavaThread, _suspend_flags); }

  static ByteSize do_not_unlock_if_synchronized_offset() { return byte_offset_of(JavaThread, _do_not_unlock_if_synchronized); }
  static ByteSize should_post_on_exceptions_flag_offset() {
    return byte_offset_of(JavaThread, _should_post_on_exceptions_flag);
  }
  static ByteSize doing_unsafe_access_offset() { return byte_offset_of(JavaThread, _doing_unsafe_access); }
  NOT_PRODUCT(static ByteSize requires_cross_modify_fence_offset()  { return byte_offset_of(JavaThread, _requires_cross_modify_fence); })

  // Returns the jni environment for this thread
  JNIEnv* jni_environment()                      { return &_jni_environment; }

  static JavaThread* thread_from_jni_environment(JNIEnv* env) {
    JavaThread *thread_from_jni_env = (JavaThread*)((intptr_t)env - in_bytes(jni_environment_offset()));
    // Only return NULL if thread is off the thread list; starting to
    // exit should not return NULL.
    if (thread_from_jni_env->is_terminated()) {
      thread_from_jni_env->block_if_vm_exited();
      return NULL;
    } else {
      return thread_from_jni_env;
    }
  }

  // JNI critical regions. These can nest.
  bool in_critical()    { return _jni_active_critical > 0; }
  bool in_last_critical()  { return _jni_active_critical == 1; }
  inline void enter_critical();
  void exit_critical() {
    assert(Thread::current() == this, "this must be current thread");
    _jni_active_critical--;
    assert(_jni_active_critical >= 0, "JNI critical nesting problem?");
  }

  // Checked JNI: is the programmer required to check for exceptions, if so specify
  // which function name. Returning to a Java frame should implicitly clear the
  // pending check, this is done for Native->Java transitions (i.e. user JNI code).
  // VM->Java transistions are not cleared, it is expected that JNI code enclosed
  // within ThreadToNativeFromVM makes proper exception checks (i.e. VM internal).
  bool is_pending_jni_exception_check() const { return _pending_jni_exception_check_fn != NULL; }
  void clear_pending_jni_exception_check() { _pending_jni_exception_check_fn = NULL; }
  const char* get_pending_jni_exception_check() const { return _pending_jni_exception_check_fn; }
  void set_pending_jni_exception_check(const char* fn_name) { _pending_jni_exception_check_fn = (char*) fn_name; }

  // For deadlock detection
  int depth_first_number() { return _depth_first_number; }
  void set_depth_first_number(int dfn) { _depth_first_number = dfn; }

 private:
  void set_monitor_chunks(MonitorChunk* monitor_chunks) { _monitor_chunks = monitor_chunks; }

 public:
  MonitorChunk* monitor_chunks() const           { return _monitor_chunks; }
  void add_monitor_chunk(MonitorChunk* chunk);
  void remove_monitor_chunk(MonitorChunk* chunk);
  bool in_deopt_handler() const                  { return _in_deopt_handler > 0; }
  void inc_in_deopt_handler()                    { _in_deopt_handler++; }
  void dec_in_deopt_handler() {
    assert(_in_deopt_handler > 0, "mismatched deopt nesting");
    if (_in_deopt_handler > 0) { // robustness
      _in_deopt_handler--;
    }
  }

 private:
  void set_entry_point(ThreadFunction entry_point) { _entry_point = entry_point; }

  // factor out low-level mechanics for use in both normal and error cases
  const char* get_thread_name_string(char* buf = NULL, int buflen = 0) const;

 public:

  // Frame iteration; calls the function f for all frames on the stack
  void frames_do(void f(frame*, const RegisterMap*));

  // Memory operations
  void oops_do_frames(OopClosure* f, CodeBlobClosure* cf);
  void oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf);

  // Sweeper operations
  virtual void nmethods_do(CodeBlobClosure* cf);

  // RedefineClasses Support
  void metadata_do(MetadataClosure* f);

  // Debug method asserting thread states are correct during a handshake operation.
  DEBUG_ONLY(void verify_states_for_handshake();)

  // Misc. operations
  const char* name() const;
  const char* type_name() const { return "JavaThread"; }
  static const char* name_for(oop thread_obj);

  void print_on(outputStream* st, bool print_extended_info) const;
  void print_on(outputStream* st) const { print_on(st, false); }
  void print() const;
  void print_thread_state_on(outputStream*) const      PRODUCT_RETURN;
  void print_on_error(outputStream* st, char* buf, int buflen) const;
  void print_name_on_error(outputStream* st, char* buf, int buflen) const;
  void verify();

  // Accessing frames
  frame last_frame() {
    _anchor.make_walkable(this);
    return pd_last_frame();
  }
  javaVFrame* last_java_vframe(RegisterMap* reg_map);

  // Returns method at 'depth' java or native frames down the stack
  // Used for security checks
  Klass* security_get_caller_class(int depth);

  // Print stack trace in external format
  void print_stack_on(outputStream* st);
  void print_stack() { print_stack_on(tty); }

  // Print stack traces in various internal formats
  void trace_stack()                             PRODUCT_RETURN;
  void trace_stack_from(vframe* start_vf)        PRODUCT_RETURN;
  void trace_frames()                            PRODUCT_RETURN;

  // Print an annotated view of the stack frames
  void print_frame_layout(int depth = 0, bool validate_only = false) NOT_DEBUG_RETURN;
  void validate_frame_layout() {
    print_frame_layout(0, true);
  }

  // Function for testing deoptimization
  void deoptimize();
  void make_zombies();

  void deoptimize_marked_methods();

 public:
  // Returns the running thread as a JavaThread
  static JavaThread* current() {
    return JavaThread::cast(Thread::current());
  }

  // Returns the current thread as a JavaThread, or NULL if not attached
  static inline JavaThread* current_or_null();

  // Casts
  static JavaThread* cast(Thread* t) {
    assert(t->is_Java_thread(), "incorrect cast to JavaThread");
    return static_cast<JavaThread*>(t);
  }

  static const JavaThread* cast(const Thread* t) {
    assert(t->is_Java_thread(), "incorrect cast to const JavaThread");
    return static_cast<const JavaThread*>(t);
  }

  // Returns the active Java thread.  Do not use this if you know you are calling
  // from a JavaThread, as it's slower than JavaThread::current.  If called from
  // the VMThread, it also returns the JavaThread that instigated the VMThread's
  // operation.  You may not want that either.
  static JavaThread* active();

 protected:
  virtual void pre_run();
  virtual void run();
  void thread_main_inner();
  virtual void post_run();

 public:
  // Thread local information maintained by JVMTI.
  void set_jvmti_thread_state(JvmtiThreadState *value)                           { _jvmti_thread_state = value; }
  // A JvmtiThreadState is lazily allocated. This jvmti_thread_state()
  // getter is used to get this JavaThread's JvmtiThreadState if it has
  // one which means NULL can be returned. JvmtiThreadState::state_for()
  // is used to get the specified JavaThread's JvmtiThreadState if it has
  // one or it allocates a new JvmtiThreadState for the JavaThread and
  // returns it. JvmtiThreadState::state_for() will return NULL only if
  // the specified JavaThread is exiting.
  JvmtiThreadState *jvmti_thread_state() const                                   { return _jvmti_thread_state; }
  static ByteSize jvmti_thread_state_offset()                                    { return byte_offset_of(JavaThread, _jvmti_thread_state); }

  // JVMTI PopFrame support
  // Setting and clearing popframe_condition
  // All of these enumerated values are bits. popframe_pending
  // indicates that a PopFrame() has been requested and not yet been
  // completed. popframe_processing indicates that that PopFrame() is in
  // the process of being completed. popframe_force_deopt_reexecution_bit
  // indicates that special handling is required when returning to a
  // deoptimized caller.
  enum PopCondition {
    popframe_inactive                      = 0x00,
    popframe_pending_bit                   = 0x01,
    popframe_processing_bit                = 0x02,
    popframe_force_deopt_reexecution_bit   = 0x04
  };
  PopCondition popframe_condition()                   { return (PopCondition) _popframe_condition; }
  void set_popframe_condition(PopCondition c)         { _popframe_condition = c; }
  void set_popframe_condition_bit(PopCondition c)     { _popframe_condition |= c; }
  void clear_popframe_condition()                     { _popframe_condition = popframe_inactive; }
  static ByteSize popframe_condition_offset()         { return byte_offset_of(JavaThread, _popframe_condition); }
  bool has_pending_popframe()                         { return (popframe_condition() & popframe_pending_bit) != 0; }
  bool popframe_forcing_deopt_reexecution()           { return (popframe_condition() & popframe_force_deopt_reexecution_bit) != 0; }
  void clear_popframe_forcing_deopt_reexecution()     { _popframe_condition &= ~popframe_force_deopt_reexecution_bit; }

  bool pop_frame_in_process(void)                     { return ((_popframe_condition & popframe_processing_bit) != 0); }
  void set_pop_frame_in_process(void)                 { _popframe_condition |= popframe_processing_bit; }
  void clr_pop_frame_in_process(void)                 { _popframe_condition &= ~popframe_processing_bit; }

  int frames_to_pop_failed_realloc() const            { return _frames_to_pop_failed_realloc; }
  void set_frames_to_pop_failed_realloc(int nb)       { _frames_to_pop_failed_realloc = nb; }
  void dec_frames_to_pop_failed_realloc()             { _frames_to_pop_failed_realloc--; }

 private:
  // Saved incoming arguments to popped frame.
  // Used only when popped interpreted frame returns to deoptimized frame.
  void*    _popframe_preserved_args;
  int      _popframe_preserved_args_size;

 public:
  void  popframe_preserve_args(ByteSize size_in_bytes, void* start);
  void* popframe_preserved_args();
  ByteSize popframe_preserved_args_size();
  WordSize popframe_preserved_args_size_in_words();
  void  popframe_free_preserved_args();


 private:
  JvmtiThreadState *_jvmti_thread_state;

  // Used by the interpreter in fullspeed mode for frame pop, method
  // entry, method exit and single stepping support. This field is
  // only set to non-zero at a safepoint or using a direct handshake
  // (see EnterInterpOnlyModeClosure).
  // It can be set to zero asynchronously to this threads execution (i.e., without
  // safepoint/handshake or a lock) so we have to be very careful.
  // Accesses by other threads are synchronized using JvmtiThreadState_lock though.
  int               _interp_only_mode;

 public:
  // used by the interpreter for fullspeed debugging support (see above)
  static ByteSize interp_only_mode_offset() { return byte_offset_of(JavaThread, _interp_only_mode); }
  bool is_interp_only_mode()                { return (_interp_only_mode != 0); }
  int get_interp_only_mode()                { return _interp_only_mode; }
  void increment_interp_only_mode()         { ++_interp_only_mode; }
  void decrement_interp_only_mode()         { --_interp_only_mode; }

  // support for cached flag that indicates whether exceptions need to be posted for this thread
  // if this is false, we can avoid deoptimizing when events are thrown
  // this gets set to reflect whether jvmtiExport::post_exception_throw would actually do anything
 private:
  int    _should_post_on_exceptions_flag;

 public:
  int   should_post_on_exceptions_flag()  { return _should_post_on_exceptions_flag; }
  void  set_should_post_on_exceptions_flag(int val)  { _should_post_on_exceptions_flag = val; }

 private:
  ThreadStatistics *_thread_stat;

 public:
  ThreadStatistics* get_thread_stat() const    { return _thread_stat; }

  // Return a blocker object for which this thread is blocked parking.
  oop current_park_blocker();

 private:
  static size_t _stack_size_at_create;

 public:
  static inline size_t stack_size_at_create(void) {
    return _stack_size_at_create;
  }
  static inline void set_stack_size_at_create(size_t value) {
    _stack_size_at_create = value;
  }

  // Machine dependent stuff
#include OS_CPU_HEADER(thread)

  // JSR166 per-thread parker
 private:
  Parker _parker;
 public:
  Parker* parker() { return &_parker; }

 public:
  // clearing/querying jni attach status
  bool is_attaching_via_jni() const { return _jni_attach_state == _attaching_via_jni; }
  bool has_attached_via_jni() const { return is_attaching_via_jni() || _jni_attach_state == _attached_via_jni; }
  inline void set_done_attaching_via_jni();

  // Stack dump assistance:
  // Track the class we want to initialize but for which we have to wait
  // on its init_lock() because it is already being initialized.
  void set_class_to_be_initialized(InstanceKlass* k);
  InstanceKlass* class_to_be_initialized() const;

private:
  InstanceKlass* _class_to_be_initialized;

  // java.lang.Thread.sleep support
  ParkEvent * _SleepEvent;
public:
  bool sleep(jlong millis);

  // java.lang.Thread interruption support
  void interrupt();
  bool is_interrupted(bool clear_interrupted);

  static OopStorage* thread_oop_storage();

  static void verify_cross_modify_fence_failure(JavaThread *thread) PRODUCT_RETURN;

  // Helper function to create the java.lang.Thread object for a
  // VM-internal thread. The thread will have the given name, be
  // part of the System ThreadGroup and if is_visible is true will be
  // discoverable via the system ThreadGroup.
  static Handle create_system_thread_object(const char* name, bool is_visible, TRAPS);

  // Helper function to start a VM-internal daemon thread.
  // E.g. ServiceThread, NotificationThread, CompilerThread etc.
  static void start_internal_daemon(JavaThread* current, JavaThread* target,
                                    Handle thread_oop, ThreadPriority prio);

  // Helper function to do vm_exit_on_initialization for osthread
  // resource allocation failure.
  static void vm_exit_on_osthread_failure(JavaThread* thread);
};

inline JavaThread* JavaThread::current_or_null() {
  Thread* current = Thread::current_or_null();
  return current != nullptr ? JavaThread::cast(current) : nullptr;
}

// The active thread queue. It also keeps track of the current used
// thread priorities.
class Threads: AllStatic {
  friend class VMStructs;
 private:
  static int         _number_of_threads;
  static int         _number_of_non_daemon_threads;
  static int         _return_code;
  static uintx       _thread_claim_token;
#ifdef ASSERT
  static bool        _vm_complete;
#endif

  static void initialize_java_lang_classes(JavaThread* main_thread, TRAPS);
  static void initialize_jsr292_core_classes(TRAPS);

 public:
  // Thread management
  // force_daemon is a concession to JNI, where we may need to add a
  // thread to the thread list before allocating its thread object
  static void add(JavaThread* p, bool force_daemon = false);
  static void remove(JavaThread* p, bool is_daemon);
  static void non_java_threads_do(ThreadClosure* tc);
  static void java_threads_do(ThreadClosure* tc);
  static void java_threads_and_vm_thread_do(ThreadClosure* tc);
  static void threads_do(ThreadClosure* tc);
  static void possibly_parallel_threads_do(bool is_par, ThreadClosure* tc);

  // Initializes the vm and creates the vm thread
  static jint create_vm(JavaVMInitArgs* args, bool* canTryAgain);
  static void convert_vm_init_libraries_to_agents();
  static void create_vm_init_libraries();
  static void create_vm_init_agents();
  static void shutdown_vm_agents();
  static void destroy_vm();
  // Supported VM versions via JNI
  // Includes JNI_VERSION_1_1
  static jboolean is_supported_jni_version_including_1_1(jint version);
  // Does not include JNI_VERSION_1_1
  static jboolean is_supported_jni_version(jint version);

  // The "thread claim token" provides a way for threads to be claimed
  // by parallel worker tasks.
  //
  // Each thread contains a "token" field. A task will claim the
  // thread only if its token is different from the global token,
  // which is updated by calling change_thread_claim_token().  When
  // a thread is claimed, it's token is set to the global token value
  // so other threads in the same iteration pass won't claim it.
  //
  // For this to work change_thread_claim_token() needs to be called
  // exactly once in sequential code before starting parallel tasks
  // that should claim threads.
  //
  // New threads get their token set to 0 and change_thread_claim_token()
  // never sets the global token to 0.
  static uintx thread_claim_token() { return _thread_claim_token; }
  static void change_thread_claim_token();
  static void assert_all_threads_claimed() NOT_DEBUG_RETURN;

  // Apply "f->do_oop" to all root oops in all threads.
  // This version may only be called by sequential code.
  static void oops_do(OopClosure* f, CodeBlobClosure* cf);
  // This version may be called by sequential or parallel code.
  static void possibly_parallel_oops_do(bool is_par, OopClosure* f, CodeBlobClosure* cf);

  // RedefineClasses support
  static void metadata_do(MetadataClosure* f);
  static void metadata_handles_do(void f(Metadata*));

#ifdef ASSERT
  static bool is_vm_complete() { return _vm_complete; }
#endif // ASSERT

  // Verification
  static void verify();
  static void print_on(outputStream* st, bool print_stacks, bool internal_format, bool print_concurrent_locks, bool print_extended_info);
  static void print(bool print_stacks, bool internal_format) {
    // this function is only used by debug.cpp
    print_on(tty, print_stacks, internal_format, false /* no concurrent lock printed */, false /* simple format */);
  }
  static void print_on_error(outputStream* st, Thread* current, char* buf, int buflen);
  static void print_on_error(Thread* this_thread, outputStream* st, Thread* current, char* buf,
                             int buflen, bool* found_current);
  static void print_threads_compiling(outputStream* st, char* buf, int buflen, bool short_form = false);

  // Get Java threads that are waiting to enter a monitor.
  static GrowableArray<JavaThread*>* get_pending_threads(ThreadsList * t_list,
                                                         int count, address monitor);

  // Get owning Java thread from the monitor's owner field.
  static JavaThread *owning_thread_from_monitor_owner(ThreadsList * t_list,
                                                      address owner);

  // Number of threads on the active threads list
  static int number_of_threads()                 { return _number_of_threads; }
  // Number of non-daemon threads on the active threads list
  static int number_of_non_daemon_threads()      { return _number_of_non_daemon_threads; }

  // Deoptimizes all frames tied to marked nmethods
  static void deoptimized_wrt_marked_nmethods();

  struct Test;                  // For private gtest access.
};

class UnlockFlagSaver {
  private:
    JavaThread* _thread;
    bool _do_not_unlock;
  public:
    UnlockFlagSaver(JavaThread* t) {
      _thread = t;
      _do_not_unlock = t->do_not_unlock_if_synchronized();
      t->set_do_not_unlock_if_synchronized(false);
    }
    ~UnlockFlagSaver() {
      _thread->set_do_not_unlock_if_synchronized(_do_not_unlock);
    }
};

#endif // SHARE_RUNTIME_THREAD_HPP
