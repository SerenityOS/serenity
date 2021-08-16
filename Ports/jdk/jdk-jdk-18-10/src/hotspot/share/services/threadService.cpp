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

#include "precompiled.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "memory/allocation.hpp"
#include "memory/heapInspection.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "prims/jvmtiRawMonitor.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.inline.hpp"
#include "runtime/vframe.hpp"
#include "runtime/vmThread.hpp"
#include "runtime/vmOperations.hpp"
#include "services/threadService.hpp"

// TODO: we need to define a naming convention for perf counters
// to distinguish counters for:
//   - standard JSR174 use
//   - Hotspot extension (public and committed)
//   - Hotspot extension (private/internal and uncommitted)

// Default is disabled.
bool ThreadService::_thread_monitoring_contention_enabled = false;
bool ThreadService::_thread_cpu_time_enabled = false;
bool ThreadService::_thread_allocated_memory_enabled = false;

PerfCounter*  ThreadService::_total_threads_count = NULL;
PerfVariable* ThreadService::_live_threads_count = NULL;
PerfVariable* ThreadService::_peak_threads_count = NULL;
PerfVariable* ThreadService::_daemon_threads_count = NULL;
volatile int ThreadService::_atomic_threads_count = 0;
volatile int ThreadService::_atomic_daemon_threads_count = 0;

ThreadDumpResult* ThreadService::_threaddump_list = NULL;

static const int INITIAL_ARRAY_SIZE = 10;

// OopStorage for thread stack sampling
static OopStorage* _thread_service_storage = NULL;

void ThreadService::init() {
  EXCEPTION_MARK;

  // These counters are for java.lang.management API support.
  // They are created even if -XX:-UsePerfData is set and in
  // that case, they will be allocated on C heap.

  _total_threads_count =
                PerfDataManager::create_counter(JAVA_THREADS, "started",
                                                PerfData::U_Events, CHECK);

  _live_threads_count =
                PerfDataManager::create_variable(JAVA_THREADS, "live",
                                                 PerfData::U_None, CHECK);

  _peak_threads_count =
                PerfDataManager::create_variable(JAVA_THREADS, "livePeak",
                                                 PerfData::U_None, CHECK);

  _daemon_threads_count =
                PerfDataManager::create_variable(JAVA_THREADS, "daemon",
                                                 PerfData::U_None, CHECK);

  if (os::is_thread_cpu_time_supported()) {
    _thread_cpu_time_enabled = true;
  }

  _thread_allocated_memory_enabled = true; // Always on, so enable it

  // Initialize OopStorage for thread stack sampling walking
  _thread_service_storage = OopStorageSet::create_strong("ThreadService OopStorage",
                                                         mtServiceability);
}

void ThreadService::reset_peak_thread_count() {
  // Acquire the lock to update the peak thread count
  // to synchronize with thread addition and removal.
  MutexLocker mu(Threads_lock);
  _peak_threads_count->set_value(get_live_thread_count());
}

static bool is_hidden_thread(JavaThread *thread) {
  // hide VM internal or JVMTI agent threads
  return thread->is_hidden_from_external_view() || thread->is_jvmti_agent_thread();
}

void ThreadService::add_thread(JavaThread* thread, bool daemon) {
  assert(Threads_lock->owned_by_self(), "must have threads lock");

  // Do not count hidden threads
  if (is_hidden_thread(thread)) {
    return;
  }

  _total_threads_count->inc();
  _live_threads_count->inc();
  Atomic::inc(&_atomic_threads_count);
  int count = _atomic_threads_count;

  if (count > _peak_threads_count->get_value()) {
    _peak_threads_count->set_value(count);
  }

  if (daemon) {
    _daemon_threads_count->inc();
    Atomic::inc(&_atomic_daemon_threads_count);
  }
}

void ThreadService::decrement_thread_counts(JavaThread* jt, bool daemon) {
  Atomic::dec(&_atomic_threads_count);

  if (daemon) {
    Atomic::dec(&_atomic_daemon_threads_count);
  }
}

void ThreadService::remove_thread(JavaThread* thread, bool daemon) {
  assert(Threads_lock->owned_by_self(), "must have threads lock");

  // Do not count hidden threads
  if (is_hidden_thread(thread)) {
    return;
  }

  assert(!thread->is_terminated(), "must not be terminated");
  if (!thread->is_exiting()) {
    // JavaThread::exit() skipped calling current_thread_exiting()
    decrement_thread_counts(thread, daemon);
  }

  int daemon_count = _atomic_daemon_threads_count;
  int count = _atomic_threads_count;

  // Counts are incremented at the same time, but atomic counts are
  // decremented earlier than perf counts.
  assert(_live_threads_count->get_value() > count,
    "thread count mismatch %d : %d",
    (int)_live_threads_count->get_value(), count);

  _live_threads_count->dec(1);
  if (daemon) {
    assert(_daemon_threads_count->get_value() > daemon_count,
      "thread count mismatch %d : %d",
      (int)_daemon_threads_count->get_value(), daemon_count);

    _daemon_threads_count->dec(1);
  }

  // Counts are incremented at the same time, but atomic counts are
  // decremented earlier than perf counts.
  assert(_daemon_threads_count->get_value() >= daemon_count,
    "thread count mismatch %d : %d",
    (int)_daemon_threads_count->get_value(), daemon_count);
  assert(_live_threads_count->get_value() >= count,
    "thread count mismatch %d : %d",
    (int)_live_threads_count->get_value(), count);
  assert(_live_threads_count->get_value() > 0 ||
    (_live_threads_count->get_value() == 0 && count == 0 &&
    _daemon_threads_count->get_value() == 0 && daemon_count == 0),
    "thread counts should reach 0 at the same time, live %d,%d daemon %d,%d",
    (int)_live_threads_count->get_value(), count,
    (int)_daemon_threads_count->get_value(), daemon_count);
  assert(_daemon_threads_count->get_value() > 0 ||
    (_daemon_threads_count->get_value() == 0 && daemon_count == 0),
    "thread counts should reach 0 at the same time, daemon %d,%d",
    (int)_daemon_threads_count->get_value(), daemon_count);
}

void ThreadService::current_thread_exiting(JavaThread* jt, bool daemon) {
  // Do not count hidden threads
  if (is_hidden_thread(jt)) {
    return;
  }

  assert(jt == JavaThread::current(), "Called by current thread");
  assert(!jt->is_terminated() && jt->is_exiting(), "must be exiting");

  decrement_thread_counts(jt, daemon);
}

// FIXME: JVMTI should call this function
Handle ThreadService::get_current_contended_monitor(JavaThread* thread) {
  assert(thread != NULL, "should be non-NULL");
  debug_only(Thread::check_for_dangling_thread_pointer(thread);)

  // This function can be called on a target JavaThread that is not
  // the caller and we are not at a safepoint. So it is possible for
  // the waiting or pending condition to be over/stale and for the
  // first stage of async deflation to clear the object field in
  // the ObjectMonitor. It is also possible for the object to be
  // inflated again and to be associated with a completely different
  // ObjectMonitor by the time this object reference is processed
  // by the caller.
  ObjectMonitor *wait_obj = thread->current_waiting_monitor();

  oop obj = NULL;
  if (wait_obj != NULL) {
    // thread is doing an Object.wait() call
    obj = wait_obj->object();
  } else {
    ObjectMonitor *enter_obj = thread->current_pending_monitor();
    if (enter_obj != NULL) {
      // thread is trying to enter() an ObjectMonitor.
      obj = enter_obj->object();
    }
  }

  Handle h(Thread::current(), obj);
  return h;
}

bool ThreadService::set_thread_monitoring_contention(bool flag) {
  MutexLocker m(Management_lock);

  bool prev = _thread_monitoring_contention_enabled;
  _thread_monitoring_contention_enabled = flag;

  return prev;
}

bool ThreadService::set_thread_cpu_time_enabled(bool flag) {
  MutexLocker m(Management_lock);

  bool prev = _thread_cpu_time_enabled;
  _thread_cpu_time_enabled = flag;

  return prev;
}

bool ThreadService::set_thread_allocated_memory_enabled(bool flag) {
  MutexLocker m(Management_lock);

  bool prev = _thread_allocated_memory_enabled;
  _thread_allocated_memory_enabled = flag;

  return prev;
}

void ThreadService::metadata_do(void f(Metadata*)) {
  for (ThreadDumpResult* dump = _threaddump_list; dump != NULL; dump = dump->next()) {
    dump->metadata_do(f);
  }
}

void ThreadService::add_thread_dump(ThreadDumpResult* dump) {
  MutexLocker ml(Management_lock);
  if (_threaddump_list == NULL) {
    _threaddump_list = dump;
  } else {
    dump->set_next(_threaddump_list);
    _threaddump_list = dump;
  }
}

void ThreadService::remove_thread_dump(ThreadDumpResult* dump) {
  MutexLocker ml(Management_lock);

  ThreadDumpResult* prev = NULL;
  bool found = false;
  for (ThreadDumpResult* d = _threaddump_list; d != NULL; prev = d, d = d->next()) {
    if (d == dump) {
      if (prev == NULL) {
        _threaddump_list = dump->next();
      } else {
        prev->set_next(dump->next());
      }
      found = true;
      break;
    }
  }
  assert(found, "The threaddump result to be removed must exist.");
}

// Dump stack trace of threads specified in the given threads array.
// Returns StackTraceElement[][] each element is the stack trace of a thread in
// the corresponding entry in the given threads array
Handle ThreadService::dump_stack_traces(GrowableArray<instanceHandle>* threads,
                                        int num_threads,
                                        TRAPS) {
  assert(num_threads > 0, "just checking");

  ThreadDumpResult dump_result;
  VM_ThreadDump op(&dump_result,
                   threads,
                   num_threads,
                   -1,    /* entire stack */
                   false, /* with locked monitors */
                   false  /* with locked synchronizers */);
  VMThread::execute(&op);

  // Allocate the resulting StackTraceElement[][] object

  ResourceMark rm(THREAD);
  Klass* k = SystemDictionary::resolve_or_fail(vmSymbols::java_lang_StackTraceElement_array(), true, CHECK_NH);
  ObjArrayKlass* ik = ObjArrayKlass::cast(k);
  objArrayOop r = oopFactory::new_objArray(ik, num_threads, CHECK_NH);
  objArrayHandle result_obj(THREAD, r);

  int num_snapshots = dump_result.num_snapshots();
  assert(num_snapshots == num_threads, "Must have num_threads thread snapshots");
  assert(num_snapshots == 0 || dump_result.t_list_has_been_set(), "ThreadsList must have been set if we have a snapshot");
  int i = 0;
  for (ThreadSnapshot* ts = dump_result.snapshots(); ts != NULL; i++, ts = ts->next()) {
    ThreadStackTrace* stacktrace = ts->get_stack_trace();
    if (stacktrace == NULL) {
      // No stack trace
      result_obj->obj_at_put(i, NULL);
    } else {
      // Construct an array of java/lang/StackTraceElement object
      Handle backtrace_h = stacktrace->allocate_fill_stack_trace_element_array(CHECK_NH);
      result_obj->obj_at_put(i, backtrace_h());
    }
  }

  return result_obj;
}

void ThreadService::reset_contention_count_stat(JavaThread* thread) {
  ThreadStatistics* stat = thread->get_thread_stat();
  if (stat != NULL) {
    stat->reset_count_stat();
  }
}

void ThreadService::reset_contention_time_stat(JavaThread* thread) {
  ThreadStatistics* stat = thread->get_thread_stat();
  if (stat != NULL) {
    stat->reset_time_stat();
  }
}

// Find deadlocks involving raw monitors, object monitors and concurrent locks
// if concurrent_locks is true.
DeadlockCycle* ThreadService::find_deadlocks_at_safepoint(ThreadsList * t_list, bool concurrent_locks) {
  assert(SafepointSynchronize::is_at_safepoint(), "must be at safepoint");

  // This code was modified from the original Threads::find_deadlocks code.
  int globalDfn = 0, thisDfn;
  ObjectMonitor* waitingToLockMonitor = NULL;
  JvmtiRawMonitor* waitingToLockRawMonitor = NULL;
  oop waitingToLockBlocker = NULL;
  bool blocked_on_monitor = false;
  JavaThread *currentThread, *previousThread;
  int num_deadlocks = 0;

  // Initialize the depth-first-number for each JavaThread.
  JavaThreadIterator jti(t_list);
  for (JavaThread* jt = jti.first(); jt != NULL; jt = jti.next()) {
    jt->set_depth_first_number(-1);
  }

  DeadlockCycle* deadlocks = NULL;
  DeadlockCycle* last = NULL;
  DeadlockCycle* cycle = new DeadlockCycle();
  for (JavaThread* jt = jti.first(); jt != NULL; jt = jti.next()) {
    if (jt->depth_first_number() >= 0) {
      // this thread was already visited
      continue;
    }

    thisDfn = globalDfn;
    jt->set_depth_first_number(globalDfn++);
    previousThread = jt;
    currentThread = jt;

    cycle->reset();

    // The ObjectMonitor* can't be async deflated since we are at a safepoint.
    // When there is a deadlock, all the monitors involved in the dependency
    // cycle must be contended and heavyweight. So we only care about the
    // heavyweight monitor a thread is waiting to lock.
    waitingToLockMonitor = jt->current_pending_monitor();
    // JVM TI raw monitors can also be involved in deadlocks, and we can be
    // waiting to lock both a raw monitor and ObjectMonitor at the same time.
    // It isn't clear how to make deadlock detection work correctly if that
    // happens.
    waitingToLockRawMonitor = jt->current_pending_raw_monitor();

    if (concurrent_locks) {
      waitingToLockBlocker = jt->current_park_blocker();
    }

    while (waitingToLockMonitor != NULL ||
           waitingToLockRawMonitor != NULL ||
           waitingToLockBlocker != NULL) {
      cycle->add_thread(currentThread);
      // Give preference to the raw monitor
      if (waitingToLockRawMonitor != NULL) {
        Thread* owner = waitingToLockRawMonitor->owner();
        if (owner != NULL && // the raw monitor could be released at any time
            owner->is_Java_thread()) {
          currentThread = JavaThread::cast(owner);
        }
      } else if (waitingToLockMonitor != NULL) {
        address currentOwner = (address)waitingToLockMonitor->owner();
        if (currentOwner != NULL) {
          currentThread = Threads::owning_thread_from_monitor_owner(t_list,
                                                                    currentOwner);
          if (currentThread == NULL) {
            // This function is called at a safepoint so the JavaThread
            // that owns waitingToLockMonitor should be findable, but
            // if it is not findable, then the previous currentThread is
            // blocked permanently. We record this as a deadlock.
            num_deadlocks++;

            // add this cycle to the deadlocks list
            if (deadlocks == NULL) {
              deadlocks = cycle;
            } else {
              last->set_next(cycle);
            }
            last = cycle;
            cycle = new DeadlockCycle();
            break;
          }
        }
      } else {
        if (concurrent_locks) {
          if (waitingToLockBlocker->is_a(vmClasses::java_util_concurrent_locks_AbstractOwnableSynchronizer_klass())) {
            oop threadObj = java_util_concurrent_locks_AbstractOwnableSynchronizer::get_owner_threadObj(waitingToLockBlocker);
            // This JavaThread (if there is one) is protected by the
            // ThreadsListSetter in VM_FindDeadlocks::doit().
            currentThread = threadObj != NULL ? java_lang_Thread::thread(threadObj) : NULL;
          } else {
            currentThread = NULL;
          }
        }
      }

      if (currentThread == NULL) {
        // No dependency on another thread
        break;
      }
      if (currentThread->depth_first_number() < 0) {
        // First visit to this thread
        currentThread->set_depth_first_number(globalDfn++);
      } else if (currentThread->depth_first_number() < thisDfn) {
        // Thread already visited, and not on a (new) cycle
        break;
      } else if (currentThread == previousThread) {
        // Self-loop, ignore
        break;
      } else {
        // We have a (new) cycle
        num_deadlocks++;

        // add this cycle to the deadlocks list
        if (deadlocks == NULL) {
          deadlocks = cycle;
        } else {
          last->set_next(cycle);
        }
        last = cycle;
        cycle = new DeadlockCycle();
        break;
      }
      previousThread = currentThread;
      waitingToLockMonitor = (ObjectMonitor*)currentThread->current_pending_monitor();
      if (concurrent_locks) {
        waitingToLockBlocker = currentThread->current_park_blocker();
      }
    }

  }
  delete cycle;
  return deadlocks;
}

ThreadDumpResult::ThreadDumpResult() : _num_threads(0), _num_snapshots(0), _snapshots(NULL), _last(NULL), _next(NULL), _setter() {

  // Create a new ThreadDumpResult object and append to the list.
  // If GC happens before this function returns, Method*
  // in the stack trace will be visited.
  ThreadService::add_thread_dump(this);
}

ThreadDumpResult::ThreadDumpResult(int num_threads) : _num_threads(num_threads), _num_snapshots(0), _snapshots(NULL), _last(NULL), _next(NULL), _setter() {
  // Create a new ThreadDumpResult object and append to the list.
  // If GC happens before this function returns, oops
  // will be visited.
  ThreadService::add_thread_dump(this);
}

ThreadDumpResult::~ThreadDumpResult() {
  ThreadService::remove_thread_dump(this);

  // free all the ThreadSnapshot objects created during
  // the VM_ThreadDump operation
  ThreadSnapshot* ts = _snapshots;
  while (ts != NULL) {
    ThreadSnapshot* p = ts;
    ts = ts->next();
    delete p;
  }
}

ThreadSnapshot* ThreadDumpResult::add_thread_snapshot() {
  ThreadSnapshot* ts = new ThreadSnapshot();
  link_thread_snapshot(ts);
  return ts;
}

ThreadSnapshot* ThreadDumpResult::add_thread_snapshot(JavaThread* thread) {
  ThreadSnapshot* ts = new ThreadSnapshot();
  link_thread_snapshot(ts);
  ts->initialize(t_list(), thread);
  return ts;
}

void ThreadDumpResult::link_thread_snapshot(ThreadSnapshot* ts) {
  assert(_num_threads == 0 || _num_snapshots < _num_threads,
         "_num_snapshots must be less than _num_threads");
  _num_snapshots++;
  if (_snapshots == NULL) {
    _snapshots = ts;
  } else {
    _last->set_next(ts);
  }
  _last = ts;
}

void ThreadDumpResult::metadata_do(void f(Metadata*)) {
  for (ThreadSnapshot* ts = _snapshots; ts != NULL; ts = ts->next()) {
    ts->metadata_do(f);
  }
}

ThreadsList* ThreadDumpResult::t_list() {
  return _setter.list();
}

StackFrameInfo::StackFrameInfo(javaVFrame* jvf, bool with_lock_info) {
  _method = jvf->method();
  _bci = jvf->bci();
  _class_holder = OopHandle(_thread_service_storage, _method->method_holder()->klass_holder());
  _locked_monitors = NULL;
  if (with_lock_info) {
    Thread* current_thread = Thread::current();
    ResourceMark rm(current_thread);
    HandleMark hm(current_thread);
    GrowableArray<MonitorInfo*>* list = jvf->locked_monitors();
    int length = list->length();
    if (length > 0) {
      _locked_monitors = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<OopHandle>(length, mtServiceability);
      for (int i = 0; i < length; i++) {
        MonitorInfo* monitor = list->at(i);
        assert(monitor->owner() != NULL, "This monitor must have an owning object");
        _locked_monitors->append(OopHandle(_thread_service_storage, monitor->owner()));
      }
    }
  }
}

StackFrameInfo::~StackFrameInfo() {
  if (_locked_monitors != NULL) {
    for (int i = 0; i < _locked_monitors->length(); i++) {
      _locked_monitors->at(i).release(_thread_service_storage);
    }
    delete _locked_monitors;
  }
  _class_holder.release(_thread_service_storage);
}

void StackFrameInfo::metadata_do(void f(Metadata*)) {
  f(_method);
}

void StackFrameInfo::print_on(outputStream* st) const {
  ResourceMark rm;
  java_lang_Throwable::print_stack_element(st, method(), bci());
  int len = (_locked_monitors != NULL ? _locked_monitors->length() : 0);
  for (int i = 0; i < len; i++) {
    oop o = _locked_monitors->at(i).resolve();
    st->print_cr("\t- locked <" INTPTR_FORMAT "> (a %s)", p2i(o), o->klass()->external_name());
  }

}

// Iterate through monitor cache to find JNI locked monitors
class InflatedMonitorsClosure: public MonitorClosure {
private:
  ThreadStackTrace* _stack_trace;
  Thread* _thread;
public:
  InflatedMonitorsClosure(Thread* t, ThreadStackTrace* st) {
    _thread = t;
    _stack_trace = st;
  }
  void do_monitor(ObjectMonitor* mid) {
    if (mid->owner() == _thread) {
      oop object = mid->object();
      if (!_stack_trace->is_owned_monitor_on_stack(object)) {
        _stack_trace->add_jni_locked_monitor(object);
      }
    }
  }
};

ThreadStackTrace::ThreadStackTrace(JavaThread* t, bool with_locked_monitors) {
  _thread = t;
  _frames = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<StackFrameInfo*>(INITIAL_ARRAY_SIZE, mtServiceability);
  _depth = 0;
  _with_locked_monitors = with_locked_monitors;
  if (_with_locked_monitors) {
    _jni_locked_monitors = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<OopHandle>(INITIAL_ARRAY_SIZE, mtServiceability);
  } else {
    _jni_locked_monitors = NULL;
  }
}

void ThreadStackTrace::add_jni_locked_monitor(oop object) {
  _jni_locked_monitors->append(OopHandle(_thread_service_storage, object));
}

ThreadStackTrace::~ThreadStackTrace() {
  for (int i = 0; i < _frames->length(); i++) {
    delete _frames->at(i);
  }
  delete _frames;
  if (_jni_locked_monitors != NULL) {
    for (int i = 0; i < _jni_locked_monitors->length(); i++) {
      _jni_locked_monitors->at(i).release(_thread_service_storage);
    }
    delete _jni_locked_monitors;
  }
}

void ThreadStackTrace::dump_stack_at_safepoint(int maxDepth) {
  assert(SafepointSynchronize::is_at_safepoint(), "all threads are stopped");

  if (_thread->has_last_Java_frame()) {
    RegisterMap reg_map(_thread);
    vframe* start_vf = _thread->last_java_vframe(&reg_map);
    int count = 0;
    for (vframe* f = start_vf; f; f = f->sender() ) {
      if (maxDepth >= 0 && count == maxDepth) {
        // Skip frames if more than maxDepth
        break;
      }
      if (f->is_java_frame()) {
        javaVFrame* jvf = javaVFrame::cast(f);
        add_stack_frame(jvf);
        count++;
      } else {
        // Ignore non-Java frames
      }
    }
  }

  if (_with_locked_monitors) {
    // Iterate inflated monitors and find monitors locked by this thread
    // not found in the stack
    InflatedMonitorsClosure imc(_thread, this);
    ObjectSynchronizer::monitors_iterate(&imc);
  }
}


bool ThreadStackTrace::is_owned_monitor_on_stack(oop object) {
  assert(SafepointSynchronize::is_at_safepoint(), "all threads are stopped");

  bool found = false;
  int num_frames = get_stack_depth();
  for (int depth = 0; depth < num_frames; depth++) {
    StackFrameInfo* frame = stack_frame_at(depth);
    int len = frame->num_locked_monitors();
    GrowableArray<OopHandle>* locked_monitors = frame->locked_monitors();
    for (int j = 0; j < len; j++) {
      oop monitor = locked_monitors->at(j).resolve();
      assert(monitor != NULL, "must be a Java object");
      if (monitor == object) {
        found = true;
        break;
      }
    }
  }
  return found;
}

Handle ThreadStackTrace::allocate_fill_stack_trace_element_array(TRAPS) {
  InstanceKlass* ik = vmClasses::StackTraceElement_klass();
  assert(ik != NULL, "must be loaded in 1.4+");

  // Allocate an array of java/lang/StackTraceElement object
  objArrayOop ste = oopFactory::new_objArray(ik, _depth, CHECK_NH);
  objArrayHandle backtrace(THREAD, ste);
  for (int j = 0; j < _depth; j++) {
    StackFrameInfo* frame = _frames->at(j);
    methodHandle mh(THREAD, frame->method());
    oop element = java_lang_StackTraceElement::create(mh, frame->bci(), CHECK_NH);
    backtrace->obj_at_put(j, element);
  }
  return backtrace;
}

void ThreadStackTrace::add_stack_frame(javaVFrame* jvf) {
  StackFrameInfo* frame = new StackFrameInfo(jvf, _with_locked_monitors);
  _frames->append(frame);
  _depth++;
}

void ThreadStackTrace::metadata_do(void f(Metadata*)) {
  int length = _frames->length();
  for (int i = 0; i < length; i++) {
    _frames->at(i)->metadata_do(f);
  }
}


ConcurrentLocksDump::~ConcurrentLocksDump() {
  if (_retain_map_on_free) {
    return;
  }

  for (ThreadConcurrentLocks* t = _map; t != NULL;)  {
    ThreadConcurrentLocks* tcl = t;
    t = t->next();
    delete tcl;
  }
}

void ConcurrentLocksDump::dump_at_safepoint() {
  // dump all locked concurrent locks
  assert(SafepointSynchronize::is_at_safepoint(), "all threads are stopped");

  GrowableArray<oop>* aos_objects = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<oop>(INITIAL_ARRAY_SIZE, mtServiceability);

  // Find all instances of AbstractOwnableSynchronizer
  HeapInspection::find_instances_at_safepoint(vmClasses::java_util_concurrent_locks_AbstractOwnableSynchronizer_klass(),
                                              aos_objects);
  // Build a map of thread to its owned AQS locks
  build_map(aos_objects);

  delete aos_objects;
}


// build a map of JavaThread to all its owned AbstractOwnableSynchronizer
void ConcurrentLocksDump::build_map(GrowableArray<oop>* aos_objects) {
  int length = aos_objects->length();
  for (int i = 0; i < length; i++) {
    oop o = aos_objects->at(i);
    oop owner_thread_obj = java_util_concurrent_locks_AbstractOwnableSynchronizer::get_owner_threadObj(o);
    if (owner_thread_obj != NULL) {
      // See comments in ThreadConcurrentLocks to see how this
      // JavaThread* is protected.
      JavaThread* thread = java_lang_Thread::thread(owner_thread_obj);
      assert(o->is_instance(), "Must be an instanceOop");
      add_lock(thread, (instanceOop) o);
    }
  }
}

void ConcurrentLocksDump::add_lock(JavaThread* thread, instanceOop o) {
  ThreadConcurrentLocks* tcl = thread_concurrent_locks(thread);
  if (tcl != NULL) {
    tcl->add_lock(o);
    return;
  }

  // First owned lock found for this thread
  tcl = new ThreadConcurrentLocks(thread);
  tcl->add_lock(o);
  if (_map == NULL) {
    _map = tcl;
  } else {
    _last->set_next(tcl);
  }
  _last = tcl;
}

ThreadConcurrentLocks* ConcurrentLocksDump::thread_concurrent_locks(JavaThread* thread) {
  for (ThreadConcurrentLocks* tcl = _map; tcl != NULL; tcl = tcl->next()) {
    if (tcl->java_thread() == thread) {
      return tcl;
    }
  }
  return NULL;
}

void ConcurrentLocksDump::print_locks_on(JavaThread* t, outputStream* st) {
  st->print_cr("   Locked ownable synchronizers:");
  ThreadConcurrentLocks* tcl = thread_concurrent_locks(t);
  GrowableArray<OopHandle>* locks = (tcl != NULL ? tcl->owned_locks() : NULL);
  if (locks == NULL || locks->is_empty()) {
    st->print_cr("\t- None");
    st->cr();
    return;
  }

  for (int i = 0; i < locks->length(); i++) {
    oop obj = locks->at(i).resolve();
    st->print_cr("\t- <" INTPTR_FORMAT "> (a %s)", p2i(obj), obj->klass()->external_name());
  }
  st->cr();
}

ThreadConcurrentLocks::ThreadConcurrentLocks(JavaThread* thread) {
  _thread = thread;
  _owned_locks = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<OopHandle>(INITIAL_ARRAY_SIZE, mtServiceability);
  _next = NULL;
}

ThreadConcurrentLocks::~ThreadConcurrentLocks() {
  for (int i = 0; i < _owned_locks->length(); i++) {
    _owned_locks->at(i).release(_thread_service_storage);
  }
  delete _owned_locks;
}

void ThreadConcurrentLocks::add_lock(instanceOop o) {
  _owned_locks->append(OopHandle(_thread_service_storage, o));
}

ThreadStatistics::ThreadStatistics() {
  _contended_enter_count = 0;
  _monitor_wait_count = 0;
  _sleep_count = 0;
  _count_pending_reset = false;
  _timer_pending_reset = false;
  memset((void*) _perf_recursion_counts, 0, sizeof(_perf_recursion_counts));
}

oop ThreadSnapshot::threadObj() const { return _threadObj.resolve(); }

void ThreadSnapshot::initialize(ThreadsList * t_list, JavaThread* thread) {
  _thread = thread;
  oop threadObj = thread->threadObj();
  _threadObj = OopHandle(_thread_service_storage, threadObj);

  ThreadStatistics* stat = thread->get_thread_stat();
  _contended_enter_ticks = stat->contended_enter_ticks();
  _contended_enter_count = stat->contended_enter_count();
  _monitor_wait_ticks = stat->monitor_wait_ticks();
  _monitor_wait_count = stat->monitor_wait_count();
  _sleep_ticks = stat->sleep_ticks();
  _sleep_count = stat->sleep_count();

  // If thread is still attaching then threadObj will be NULL.
  _thread_status = threadObj == NULL ? JavaThreadStatus::NEW
                                     : java_lang_Thread::get_thread_status(threadObj);

  _is_suspended = thread->is_suspended();
  _is_in_native = (thread->thread_state() == _thread_in_native);

  Handle obj = ThreadService::get_current_contended_monitor(thread);

  oop blocker_object = NULL;
  oop blocker_object_owner = NULL;

  if (_thread_status == JavaThreadStatus::BLOCKED_ON_MONITOR_ENTER ||
      _thread_status == JavaThreadStatus::IN_OBJECT_WAIT ||
      _thread_status == JavaThreadStatus::IN_OBJECT_WAIT_TIMED) {

    if (obj() == NULL) {
      // monitor no longer exists; thread is not blocked
      _thread_status = JavaThreadStatus::RUNNABLE;
    } else {
      blocker_object = obj();
      JavaThread* owner = ObjectSynchronizer::get_lock_owner(t_list, obj);
      if ((owner == NULL && _thread_status == JavaThreadStatus::BLOCKED_ON_MONITOR_ENTER)
          || (owner != NULL && owner->is_attaching_via_jni())) {
        // ownership information of the monitor is not available
        // (may no longer be owned or releasing to some other thread)
        // make this thread in RUNNABLE state.
        // And when the owner thread is in attaching state, the java thread
        // is not completely initialized. For example thread name and id
        // and may not be set, so hide the attaching thread.
        _thread_status = JavaThreadStatus::RUNNABLE;
        blocker_object = NULL;
      } else if (owner != NULL) {
        blocker_object_owner = owner->threadObj();
      }
    }
  }

  // Support for JSR-166 locks
  if (_thread_status == JavaThreadStatus::PARKED || _thread_status == JavaThreadStatus::PARKED_TIMED) {
    blocker_object = thread->current_park_blocker();
    if (blocker_object != NULL && blocker_object->is_a(vmClasses::java_util_concurrent_locks_AbstractOwnableSynchronizer_klass())) {
      blocker_object_owner = java_util_concurrent_locks_AbstractOwnableSynchronizer::get_owner_threadObj(blocker_object);
    }
  }

  if (blocker_object != NULL) {
    _blocker_object = OopHandle(_thread_service_storage, blocker_object);
  }
  if (blocker_object_owner != NULL) {
    _blocker_object_owner = OopHandle(_thread_service_storage, blocker_object_owner);
  }
}

oop ThreadSnapshot::blocker_object() const           { return _blocker_object.resolve(); }
oop ThreadSnapshot::blocker_object_owner() const     { return _blocker_object_owner.resolve(); }

ThreadSnapshot::~ThreadSnapshot() {
  _blocker_object.release(_thread_service_storage);
  _blocker_object_owner.release(_thread_service_storage);
  _threadObj.release(_thread_service_storage);

  delete _stack_trace;
  delete _concurrent_locks;
}

void ThreadSnapshot::dump_stack_at_safepoint(int max_depth, bool with_locked_monitors) {
  _stack_trace = new ThreadStackTrace(_thread, with_locked_monitors);
  _stack_trace->dump_stack_at_safepoint(max_depth);
}


void ThreadSnapshot::metadata_do(void f(Metadata*)) {
  if (_stack_trace != NULL) {
    _stack_trace->metadata_do(f);
  }
}


DeadlockCycle::DeadlockCycle() {
  _threads = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<JavaThread*>(INITIAL_ARRAY_SIZE, mtServiceability);
  _next = NULL;
}

DeadlockCycle::~DeadlockCycle() {
  delete _threads;
}

void DeadlockCycle::print_on_with(ThreadsList * t_list, outputStream* st) const {
  st->cr();
  st->print_cr("Found one Java-level deadlock:");
  st->print("=============================");

  JavaThread* currentThread;
  JvmtiRawMonitor* waitingToLockRawMonitor;
  oop waitingToLockBlocker;
  int len = _threads->length();
  for (int i = 0; i < len; i++) {
    currentThread = _threads->at(i);
    // The ObjectMonitor* can't be async deflated since we are at a safepoint.
    ObjectMonitor* waitingToLockMonitor = currentThread->current_pending_monitor();
    waitingToLockRawMonitor = currentThread->current_pending_raw_monitor();
    waitingToLockBlocker = currentThread->current_park_blocker();
    st->cr();
    st->print_cr("\"%s\":", currentThread->name());
    const char* owner_desc = ",\n  which is held by";

    // Note: As the JVM TI "monitor contended enter" event callback is executed after ObjectMonitor
    // sets the current pending monitor, it is possible to then see a pending raw monitor as well.
    if (waitingToLockRawMonitor != NULL) {
      st->print("  waiting to lock JVM TI raw monitor " INTPTR_FORMAT, p2i(waitingToLockRawMonitor));
      Thread* owner = waitingToLockRawMonitor->owner();
      // Could be NULL as the raw monitor could be released at any time if held by non-JavaThread
      if (owner != NULL) {
        if (owner->is_Java_thread()) {
          currentThread = JavaThread::cast(owner);
          st->print_cr("%s \"%s\"", owner_desc, currentThread->name());
        } else {
          st->print_cr(",\n  which has now been released");
        }
      } else {
        st->print_cr("%s non-Java thread=" PTR_FORMAT, owner_desc, p2i(owner));
      }
    }

    if (waitingToLockMonitor != NULL) {
      st->print("  waiting to lock monitor " INTPTR_FORMAT, p2i(waitingToLockMonitor));
      oop obj = waitingToLockMonitor->object();
      st->print(" (object " INTPTR_FORMAT ", a %s)", p2i(obj),
                 obj->klass()->external_name());

      if (!currentThread->current_pending_monitor_is_from_java()) {
        owner_desc = "\n  in JNI, which is held by";
      }
      currentThread = Threads::owning_thread_from_monitor_owner(t_list,
                                                                (address)waitingToLockMonitor->owner());
      if (currentThread == NULL) {
        // The deadlock was detected at a safepoint so the JavaThread
        // that owns waitingToLockMonitor should be findable, but
        // if it is not findable, then the previous currentThread is
        // blocked permanently.
        st->print_cr("%s UNKNOWN_owner_addr=" PTR_FORMAT, owner_desc,
                  p2i(waitingToLockMonitor->owner()));
        continue;
      }
    } else {
      st->print("  waiting for ownable synchronizer " INTPTR_FORMAT ", (a %s)",
                p2i(waitingToLockBlocker),
                waitingToLockBlocker->klass()->external_name());
      assert(waitingToLockBlocker->is_a(vmClasses::java_util_concurrent_locks_AbstractOwnableSynchronizer_klass()),
             "Must be an AbstractOwnableSynchronizer");
      oop ownerObj = java_util_concurrent_locks_AbstractOwnableSynchronizer::get_owner_threadObj(waitingToLockBlocker);
      currentThread = java_lang_Thread::thread(ownerObj);
      assert(currentThread != NULL, "AbstractOwnableSynchronizer owning thread is unexpectedly NULL");
    }
    st->print_cr("%s \"%s\"", owner_desc, currentThread->name());
  }

  st->cr();

  // Print stack traces
  bool oldJavaMonitorsInStackTrace = JavaMonitorsInStackTrace;
  JavaMonitorsInStackTrace = true;
  st->print_cr("Java stack information for the threads listed above:");
  st->print_cr("===================================================");
  for (int j = 0; j < len; j++) {
    currentThread = _threads->at(j);
    st->print_cr("\"%s\":", currentThread->name());
    currentThread->print_stack_on(st);
  }
  JavaMonitorsInStackTrace = oldJavaMonitorsInStackTrace;
}

ThreadsListEnumerator::ThreadsListEnumerator(Thread* cur_thread,
                                             bool include_jvmti_agent_threads,
                                             bool include_jni_attaching_threads) {
  assert(cur_thread == Thread::current(), "Check current thread");

  int init_size = ThreadService::get_live_thread_count();
  _threads_array = new GrowableArray<instanceHandle>(init_size);

  for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
    // skips JavaThreads in the process of exiting
    // and also skips VM internal JavaThreads
    // Threads in _thread_new or _thread_new_trans state are included.
    // i.e. threads have been started but not yet running.
    if (jt->threadObj() == NULL   ||
        jt->is_exiting() ||
        !java_lang_Thread::is_alive(jt->threadObj())   ||
        jt->is_hidden_from_external_view()) {
      continue;
    }

    // skip agent threads
    if (!include_jvmti_agent_threads && jt->is_jvmti_agent_thread()) {
      continue;
    }

    // skip jni threads in the process of attaching
    if (!include_jni_attaching_threads && jt->is_attaching_via_jni()) {
      continue;
    }

    instanceHandle h(cur_thread, (instanceOop) jt->threadObj());
    _threads_array->append(h);
  }
}
