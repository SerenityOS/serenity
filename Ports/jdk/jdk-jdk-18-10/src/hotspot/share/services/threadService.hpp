/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_THREADSERVICE_HPP
#define SHARE_SERVICES_THREADSERVICE_HPP

#include "classfile/classLoader.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/javaThreadStatus.hpp"
#include "runtime/handles.hpp"
#include "runtime/init.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/perfData.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"
#include "services/management.hpp"

class OopClosure;
class ThreadDumpResult;
class ThreadStackTrace;
class ThreadSnapshot;
class StackFrameInfo;
class ThreadConcurrentLocks;
class DeadlockCycle;

// VM monitoring and management support for the thread and
// synchronization subsystem
//
// Thread contention monitoring is disabled by default.
// When enabled, the VM will begin measuring the accumulated
// elapsed time a thread blocked on synchronization.
//
class ThreadService : public AllStatic {
private:
  // These counters could be moved to Threads class
  static PerfCounter*  _total_threads_count;
  static PerfVariable* _live_threads_count;
  static PerfVariable* _peak_threads_count;
  static PerfVariable* _daemon_threads_count;

  // These 2 counters are like the above thread counts, but are
  // atomically decremented in ThreadService::current_thread_exiting instead of
  // ThreadService::remove_thread, so that the thread count is updated before
  // Thread.join() returns.
  static volatile int  _atomic_threads_count;
  static volatile int  _atomic_daemon_threads_count;

  static bool          _thread_monitoring_contention_enabled;
  static bool          _thread_cpu_time_enabled;
  static bool          _thread_allocated_memory_enabled;

  // Need to keep the list of thread dump result that
  // keep references to Method* since thread dump can be
  // requested by multiple threads concurrently.
  static ThreadDumpResult* _threaddump_list;

  static void decrement_thread_counts(JavaThread* jt, bool daemon);

public:
  static void init();
  static void add_thread(JavaThread* thread, bool daemon);
  static void remove_thread(JavaThread* thread, bool daemon);
  static void current_thread_exiting(JavaThread* jt, bool daemon);

  static bool set_thread_monitoring_contention(bool flag);
  static bool is_thread_monitoring_contention() { return _thread_monitoring_contention_enabled; }

  static bool set_thread_cpu_time_enabled(bool flag);
  static bool is_thread_cpu_time_enabled()    { return _thread_cpu_time_enabled; }

  static bool set_thread_allocated_memory_enabled(bool flag);
  static bool is_thread_allocated_memory_enabled() { return _thread_allocated_memory_enabled; }

  static jlong get_total_thread_count()       { return _total_threads_count->get_value(); }
  static jlong get_peak_thread_count()        { return _peak_threads_count->get_value(); }
  static jlong get_live_thread_count()        { return _atomic_threads_count; }
  static jlong get_daemon_thread_count()      { return _atomic_daemon_threads_count; }

  // Support for thread dump
  static void   add_thread_dump(ThreadDumpResult* dump);
  static void   remove_thread_dump(ThreadDumpResult* dump);

  static Handle get_current_contended_monitor(JavaThread* thread);

  // This function is called by JVM_DumpThreads.
  static Handle dump_stack_traces(GrowableArray<instanceHandle>* threads,
                                  int num_threads, TRAPS);

  static void   reset_peak_thread_count();
  static void   reset_contention_count_stat(JavaThread* thread);
  static void   reset_contention_time_stat(JavaThread* thread);

  static DeadlockCycle*       find_deadlocks_at_safepoint(ThreadsList * t_list, bool object_monitors_only);

  static void   metadata_do(void f(Metadata*));
};

// Per-thread Statistics for synchronization
class ThreadStatistics : public CHeapObj<mtInternal> {
private:
  // The following contention statistics are only updated by
  // the thread owning these statistics when contention occurs.

  jlong        _contended_enter_count;
  elapsedTimer _contended_enter_timer;
  jlong        _monitor_wait_count;
  elapsedTimer _monitor_wait_timer;
  jlong        _sleep_count;
  elapsedTimer _sleep_timer;


  // These two reset flags are set to true when another thread
  // requests to reset the statistics.  The actual statistics
  // are reset when the thread contention occurs and attempts
  // to update the statistics.
  bool         _count_pending_reset;
  bool         _timer_pending_reset;

  // Keep accurate times for potentially recursive class operations
  int           _perf_recursion_counts[PerfClassTraceTime::EVENT_TYPE_COUNT];
  elapsedTimer  _perf_timers[PerfClassTraceTime::EVENT_TYPE_COUNT];

  // utility functions
  void  check_and_reset_count()            {
                                             if (!_count_pending_reset) return;
                                             _contended_enter_count = 0;
                                             _monitor_wait_count = 0;
                                             _sleep_count = 0;
                                             _count_pending_reset = 0;
                                           }
  void  check_and_reset_timer()            {
                                             if (!_timer_pending_reset) return;
                                             _contended_enter_timer.reset();
                                             _monitor_wait_timer.reset();
                                             _sleep_timer.reset();
                                             _timer_pending_reset = 0;
                                           }

public:
  ThreadStatistics();

  jlong contended_enter_count()            { return (_count_pending_reset ? 0 : _contended_enter_count); }
  jlong contended_enter_ticks()            { return (_timer_pending_reset ? 0 : _contended_enter_timer.active_ticks()); }
  jlong monitor_wait_count()               { return (_count_pending_reset ? 0 : _monitor_wait_count); }
  jlong monitor_wait_ticks()               { return (_timer_pending_reset ? 0 : _monitor_wait_timer.active_ticks()); }
  jlong sleep_count()                      { return (_count_pending_reset ? 0 : _sleep_count); }
  jlong sleep_ticks()                      { return (_timer_pending_reset ? 0 : _sleep_timer.active_ticks()); }

  void monitor_wait()                      { check_and_reset_count(); _monitor_wait_count++; }
  void monitor_wait_begin()                { check_and_reset_timer(); _monitor_wait_timer.start(); }
  void monitor_wait_end()                  { _monitor_wait_timer.stop(); check_and_reset_timer(); }

  void thread_sleep()                      { check_and_reset_count(); _sleep_count++; }
  void thread_sleep_begin()                { check_and_reset_timer(); _sleep_timer.start(); }
  void thread_sleep_end()                  { _sleep_timer.stop(); check_and_reset_timer(); }

  void contended_enter()                   { check_and_reset_count(); _contended_enter_count++; }
  void contended_enter_begin()             { check_and_reset_timer(); _contended_enter_timer.start(); }
  void contended_enter_end()               { _contended_enter_timer.stop(); check_and_reset_timer(); }

  void reset_count_stat()                  { _count_pending_reset = true; }
  void reset_time_stat()                   { _timer_pending_reset = true; }

  int* perf_recursion_counts_addr()        { return _perf_recursion_counts; }
  elapsedTimer* perf_timers_addr()         { return _perf_timers; }
};

// Thread snapshot to represent the thread state and statistics
class ThreadSnapshot : public CHeapObj<mtInternal> {
private:
  // This JavaThread* is protected by being stored in objects that are
  // protected by a ThreadsListSetter (ThreadDumpResult).
  JavaThread* _thread;
  OopHandle   _threadObj;
  JavaThreadStatus _thread_status;

  bool    _is_suspended;
  bool    _is_in_native;

  jlong   _contended_enter_ticks;
  jlong   _contended_enter_count;
  jlong   _monitor_wait_ticks;
  jlong   _monitor_wait_count;
  jlong   _sleep_ticks;
  jlong   _sleep_count;

  OopHandle     _blocker_object;
  OopHandle     _blocker_object_owner;

  ThreadStackTrace*      _stack_trace;
  ThreadConcurrentLocks* _concurrent_locks;
  ThreadSnapshot*        _next;

  // ThreadSnapshot instances should only be created via
  // ThreadDumpResult::add_thread_snapshot.
  friend class ThreadDumpResult;
  ThreadSnapshot() : _thread(NULL),
                     _stack_trace(NULL), _concurrent_locks(NULL), _next(NULL) {};
  void        initialize(ThreadsList * t_list, JavaThread* thread);

public:
  ~ThreadSnapshot();

  JavaThreadStatus thread_status() { return _thread_status; }

  oop         threadObj() const;

  void        set_next(ThreadSnapshot* n) { _next = n; }

  bool        is_suspended()              { return _is_suspended; }
  bool        is_in_native()              { return _is_in_native; }

  jlong       contended_enter_count()     { return _contended_enter_count; }
  jlong       contended_enter_ticks()     { return _contended_enter_ticks; }
  jlong       monitor_wait_count()        { return _monitor_wait_count; }
  jlong       monitor_wait_ticks()        { return _monitor_wait_ticks; }
  jlong       sleep_count()               { return _sleep_count; }
  jlong       sleep_ticks()               { return _sleep_ticks; }


  oop         blocker_object() const;
  oop         blocker_object_owner() const;

  ThreadSnapshot*   next() const          { return _next; }
  ThreadStackTrace* get_stack_trace()     { return _stack_trace; }
  ThreadConcurrentLocks* get_concurrent_locks()     { return _concurrent_locks; }

  void        dump_stack_at_safepoint(int max_depth, bool with_locked_monitors);
  void        set_concurrent_locks(ThreadConcurrentLocks* l) { _concurrent_locks = l; }
  void        metadata_do(void f(Metadata*));
};

class ThreadStackTrace : public CHeapObj<mtInternal> {
 private:
  JavaThread*                     _thread;
  int                             _depth;  // number of stack frames added
  bool                            _with_locked_monitors;
  GrowableArray<StackFrameInfo*>* _frames;
  GrowableArray<OopHandle>*       _jni_locked_monitors;

 public:

  ThreadStackTrace(JavaThread* thread, bool with_locked_monitors);
  ~ThreadStackTrace();

  JavaThread*     thread()              { return _thread; }
  StackFrameInfo* stack_frame_at(int i) { return _frames->at(i); }
  int             get_stack_depth()     { return _depth; }

  void            add_stack_frame(javaVFrame* jvf);
  void            dump_stack_at_safepoint(int max_depth);
  Handle          allocate_fill_stack_trace_element_array(TRAPS);
  void            metadata_do(void f(Metadata*));
  GrowableArray<OopHandle>* jni_locked_monitors() { return _jni_locked_monitors; }
  int             num_jni_locked_monitors() { return (_jni_locked_monitors != NULL ? _jni_locked_monitors->length() : 0); }

  bool            is_owned_monitor_on_stack(oop object);
  void            add_jni_locked_monitor(oop object);
};

// StackFrameInfo for keeping Method* and bci during
// stack walking for later construction of StackTraceElement[]
// Java instances
class StackFrameInfo : public CHeapObj<mtInternal> {
 private:
  Method*             _method;
  int                 _bci;
  GrowableArray<OopHandle>* _locked_monitors; // list of object monitors locked by this frame
  // We need to save the mirrors in the backtrace to keep the class
  // from being unloaded while we still have this stack trace.
  OopHandle           _class_holder;

 public:

  StackFrameInfo(javaVFrame* jvf, bool with_locked_monitors);
  ~StackFrameInfo();
  Method*   method() const       { return _method; }
  int       bci()    const       { return _bci; }
  void      metadata_do(void f(Metadata*));

  int       num_locked_monitors()       { return (_locked_monitors != NULL ? _locked_monitors->length() : 0); }
  GrowableArray<OopHandle>* locked_monitors() { return _locked_monitors; }

  void      print_on(outputStream* st) const;
};

class ThreadConcurrentLocks : public CHeapObj<mtInternal> {
private:
  GrowableArray<OopHandle>*   _owned_locks;
  ThreadConcurrentLocks*      _next;
  // This JavaThread* is protected in one of two different ways
  // depending on the usage of the ThreadConcurrentLocks object:
  // 1) by being stored in objects that are only allocated and used at a
  // safepoint (ConcurrentLocksDump), or 2) by being stored in objects
  // that are protected by a ThreadsListSetter (ThreadSnapshot inside
  // ThreadDumpResult).
  JavaThread*                 _thread;
 public:
  ThreadConcurrentLocks(JavaThread* thread);
  ~ThreadConcurrentLocks();

  void                        add_lock(instanceOop o);
  void                        set_next(ThreadConcurrentLocks* n) { _next = n; }
  ThreadConcurrentLocks*      next() { return _next; }
  JavaThread*                 java_thread()                      { return _thread; }
  GrowableArray<OopHandle>*   owned_locks()                      { return _owned_locks; }
};

class ConcurrentLocksDump : public StackObj {
 private:
  ThreadConcurrentLocks* _map;
  ThreadConcurrentLocks* _last;   // Last ThreadConcurrentLocks in the map
  bool                   _retain_map_on_free;

  void build_map(GrowableArray<oop>* aos_objects);
  void add_lock(JavaThread* thread, instanceOop o);

 public:
  ConcurrentLocksDump(bool retain_map_on_free) : _map(NULL), _last(NULL), _retain_map_on_free(retain_map_on_free) {
    assert(SafepointSynchronize::is_at_safepoint(), "Must be constructed at a safepoint.");
  };
  ConcurrentLocksDump() : _map(NULL), _last(NULL), _retain_map_on_free(false) {
    assert(SafepointSynchronize::is_at_safepoint(), "Must be constructed at a safepoint.");
  };
  ~ConcurrentLocksDump();

  void                        dump_at_safepoint();
  ThreadConcurrentLocks*      thread_concurrent_locks(JavaThread* thread);
  void                        print_locks_on(JavaThread* t, outputStream* st);
};

class ThreadDumpResult : public StackObj {
 private:
  int                  _num_threads;
  int                  _num_snapshots;
  ThreadSnapshot*      _snapshots;
  ThreadSnapshot*      _last;
  ThreadDumpResult*    _next;
  ThreadsListSetter    _setter;  // Helper to set hazard ptr in the originating thread
                                 // which protects the JavaThreads in _snapshots.

  void                 link_thread_snapshot(ThreadSnapshot* ts);

 public:
  ThreadDumpResult();
  ThreadDumpResult(int num_threads);
  ~ThreadDumpResult();

  ThreadSnapshot*      add_thread_snapshot();
  ThreadSnapshot*      add_thread_snapshot(JavaThread* thread);

  void                 set_next(ThreadDumpResult* next) { _next = next; }
  ThreadDumpResult*    next()                           { return _next; }
  int                  num_threads()                    { return _num_threads; }
  int                  num_snapshots()                  { return _num_snapshots; }
  ThreadSnapshot*      snapshots()                      { return _snapshots; }
  void                 set_t_list()                     { _setter.set(); }
  ThreadsList*         t_list();
  bool                 t_list_has_been_set()            { return _setter.is_set(); }
  void                 metadata_do(void f(Metadata*));
};

class DeadlockCycle : public CHeapObj<mtInternal> {
 private:
  GrowableArray<JavaThread*>* _threads;
  DeadlockCycle*              _next;
 public:
  DeadlockCycle();
  ~DeadlockCycle();

  DeadlockCycle* next()                     { return _next; }
  void           set_next(DeadlockCycle* d) { _next = d; }
  void           add_thread(JavaThread* t)  { _threads->append(t); }
  void           reset()                    { _threads->clear(); }
  int            num_threads()              { return _threads->length(); }
  GrowableArray<JavaThread*>* threads()     { return _threads; }
  void           print_on_with(ThreadsList * t_list, outputStream* st) const;
};

// Utility class to get list of java threads.
class ThreadsListEnumerator : public StackObj {
private:
  GrowableArray<instanceHandle>* _threads_array;
public:
  ThreadsListEnumerator(Thread* cur_thread,
                        bool include_jvmti_agent_threads = false,
                        bool include_jni_attaching_threads = true);
  int            num_threads()            { return _threads_array->length(); }
  instanceHandle get_threadObj(int index) { return _threads_array->at(index); }
};


// abstract utility class to set new thread states, and restore previous after the block exits
class JavaThreadStatusChanger : public StackObj {
 private:
  JavaThreadStatus _old_state;
  JavaThread*  _java_thread;
  bool _is_alive;

  void save_old_state(JavaThread* java_thread) {
    _java_thread  = java_thread;
    _is_alive = is_alive(java_thread);
    if (is_alive()) {
      _old_state = java_lang_Thread::get_thread_status(_java_thread->threadObj());
    }
  }

 public:
  static void set_thread_status(JavaThread* java_thread,
                                JavaThreadStatus state) {
    java_lang_Thread::set_thread_status(java_thread->threadObj(), state);
  }

  void set_thread_status(JavaThreadStatus state) {
    if (is_alive()) {
      set_thread_status(_java_thread, state);
    }
  }

  JavaThreadStatusChanger(JavaThread* java_thread,
                          JavaThreadStatus state) : _old_state(JavaThreadStatus::NEW) {
    save_old_state(java_thread);
    set_thread_status(state);
  }

  JavaThreadStatusChanger(JavaThread* java_thread) : _old_state(JavaThreadStatus::NEW) {
    save_old_state(java_thread);
  }

  ~JavaThreadStatusChanger() {
    set_thread_status(_old_state);
  }

  static bool is_alive(JavaThread* java_thread) {
    return java_thread != NULL && java_thread->threadObj() != NULL;
  }

  bool is_alive() {
    return _is_alive;
  }
};

// Change status to waiting on an object  (timed or indefinite)
class JavaThreadInObjectWaitState : public JavaThreadStatusChanger {
 private:
  ThreadStatistics* _stat;
  bool _active;

 public:
  JavaThreadInObjectWaitState(JavaThread *java_thread, bool timed) :
    JavaThreadStatusChanger(java_thread,
                            timed ? JavaThreadStatus::IN_OBJECT_WAIT_TIMED : JavaThreadStatus::IN_OBJECT_WAIT) {
    if (is_alive()) {
      _stat = java_thread->get_thread_stat();
      _active = ThreadService::is_thread_monitoring_contention();
      _stat->monitor_wait();
      if (_active) {
        _stat->monitor_wait_begin();
      }
    } else {
      _active = false;
    }
  }

  ~JavaThreadInObjectWaitState() {
    if (_active) {
      _stat->monitor_wait_end();
    }
  }
};

// Change status to parked (timed or indefinite)
class JavaThreadParkedState : public JavaThreadStatusChanger {
 private:
  ThreadStatistics* _stat;
  bool _active;

 public:
  JavaThreadParkedState(JavaThread *java_thread, bool timed) :
    JavaThreadStatusChanger(java_thread,
                            timed ? JavaThreadStatus::PARKED_TIMED : JavaThreadStatus::PARKED) {
    if (is_alive()) {
      _stat = java_thread->get_thread_stat();
      _active = ThreadService::is_thread_monitoring_contention();
      _stat->monitor_wait();
      if (_active) {
        _stat->monitor_wait_begin();
      }
    } else {
      _active = false;
    }
  }

  ~JavaThreadParkedState() {
    if (_active) {
      _stat->monitor_wait_end();
    }
  }
};

// Change status to blocked on (re-)entering a synchronization block
class JavaThreadBlockedOnMonitorEnterState : public JavaThreadStatusChanger {
 private:
  ThreadStatistics* _stat;
  bool _active;

  static bool contended_enter_begin(JavaThread *java_thread) {
    set_thread_status(java_thread, JavaThreadStatus::BLOCKED_ON_MONITOR_ENTER);
    ThreadStatistics* stat = java_thread->get_thread_stat();
    stat->contended_enter();
    bool active = ThreadService::is_thread_monitoring_contention();
    if (active) {
      stat->contended_enter_begin();
    }
    return active;
  }

 public:
  // java_thread is waiting thread being blocked on monitor reenter.
  // Current thread is the notifying thread which holds the monitor.
  static bool wait_reenter_begin(JavaThread *java_thread, ObjectMonitor *obj_m) {
    assert((java_thread != NULL), "Java thread should not be null here");
    bool active = false;
    if (is_alive(java_thread)) {
      active = contended_enter_begin(java_thread);
    }
    return active;
  }

  static void wait_reenter_end(JavaThread *java_thread, bool active) {
    if (active) {
      java_thread->get_thread_stat()->contended_enter_end();
    }
    set_thread_status(java_thread, JavaThreadStatus::RUNNABLE);
  }

  JavaThreadBlockedOnMonitorEnterState(JavaThread *java_thread, ObjectMonitor *obj_m) :
    JavaThreadStatusChanger(java_thread), _stat(NULL), _active(false) {
    assert((java_thread != NULL), "Java thread should not be null here");
    // Change thread status and collect contended enter stats for monitor contended
    // enter done for external java world objects and it is contended. All other cases
    // like for vm internal objects and for external objects which are not contended
    // thread status is not changed and contended enter stat is not collected.
    _active = false;
    if (is_alive() && obj_m->contentions() > 0) {
      _stat = java_thread->get_thread_stat();
      _active = contended_enter_begin(java_thread);
    }
  }

  ~JavaThreadBlockedOnMonitorEnterState() {
    if (_active) {
      _stat->contended_enter_end();
    }
  }
};

// Change status to sleeping
class JavaThreadSleepState : public JavaThreadStatusChanger {
 private:
  ThreadStatistics* _stat;
  bool _active;
 public:
  JavaThreadSleepState(JavaThread *java_thread) :
    JavaThreadStatusChanger(java_thread, JavaThreadStatus::SLEEPING) {
    if (is_alive()) {
      _stat = java_thread->get_thread_stat();
      _active = ThreadService::is_thread_monitoring_contention();
      _stat->thread_sleep();
      if (_active) {
        _stat->thread_sleep_begin();
      }
    } else {
      _active = false;
    }
  }

  ~JavaThreadSleepState() {
    if (_active) {
      _stat->thread_sleep_end();
    }
  }
};

#endif // SHARE_SERVICES_THREADSERVICE_HPP
