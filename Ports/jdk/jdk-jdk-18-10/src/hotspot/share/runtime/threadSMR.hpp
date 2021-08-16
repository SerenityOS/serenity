/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_THREADSMR_HPP
#define SHARE_RUNTIME_THREADSMR_HPP

#include "memory/allocation.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.hpp"
#include "runtime/timer.hpp"
#include "utilities/debug.hpp"

class JavaThread;
class Monitor;
class outputStream;
class Thread;
class ThreadClosure;
class ThreadsList;

// Thread Safe Memory Reclamation (Thread-SMR) support.
//
// ThreadsListHandles are used to safely perform operations on one or more
// threads without the risk of the thread or threads exiting during the
// operation. It is no longer necessary to hold the Threads_lock to safely
// perform an operation on a target thread.
//
// There are several different ways to refer to java.lang.Thread objects
// so we have a few ways to get a protected JavaThread *:
//
// JNI jobject example:
//   jobject jthread = ...;
//   :
//   ThreadsListHandle tlh;
//   JavaThread* jt = NULL;
//   bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &jt, NULL);
//   if (is_alive) {
//     :  // do stuff with 'jt'...
//   }
//
// JVM/TI jthread example:
//   jthread thread = ...;
//   :
//   JavaThread* jt = NULL;
//   ThreadsListHandle tlh;
//   jvmtiError err = JvmtiExport::cv_external_thread_to_JavaThread(tlh.list(), thread, &jt, NULL);
//   if (err != JVMTI_ERROR_NONE) {
//     return err;
//   }
//   :  // do stuff with 'jt'...
//
// JVM/TI oop example (this one should be very rare):
//   oop thread_obj = ...;
//   :
//   JavaThread *jt = NULL;
//   ThreadsListHandle tlh;
//   jvmtiError err = JvmtiExport::cv_oop_to_JavaThread(tlh.list(), thread_obj, &jt);
//   if (err != JVMTI_ERROR_NONE) {
//     return err;
//   }
//   :  // do stuff with 'jt'...
//
// A JavaThread * that is included in the ThreadsList that is held by
// a ThreadsListHandle is protected as long as the ThreadsListHandle
// remains in scope. The target JavaThread * may have logically exited,
// but that target JavaThread * will not be deleted until it is no
// longer protected by a ThreadsListHandle.
//
// SMR Support for the Threads class.
//
class ThreadsSMRSupport : AllStatic {
  friend class VMStructs;
  friend class SafeThreadsListPtr;  // for _nested_thread_list_max, delete_notify(), release_stable_list_wake_up() access

  // The coordination between ThreadsSMRSupport::release_stable_list() and
  // ThreadsSMRSupport::smr_delete() uses the delete_lock in order to
  // reduce the traffic on the Threads_lock.
  static Monitor* delete_lock() { return ThreadsSMRDelete_lock; }

  // The '_cnt', '_max' and '_times" fields are enabled via
  // -XX:+EnableThreadSMRStatistics (see thread.cpp for a
  // description about each field):
  static uint                  _delete_lock_wait_cnt;
  static uint                  _delete_lock_wait_max;
  // The delete_notify flag is used for proper double-check
  // locking in order to reduce the traffic on the system wide
  // Thread-SMR delete_lock.
  static volatile uint         _delete_notify;
  static volatile uint         _deleted_thread_cnt;
  static volatile uint         _deleted_thread_time_max;
  static volatile uint         _deleted_thread_times;
  static ThreadsList           _bootstrap_list;
  static ThreadsList* volatile _java_thread_list;
  static uint64_t              _java_thread_list_alloc_cnt;
  static uint64_t              _java_thread_list_free_cnt;
  static uint                  _java_thread_list_max;
  static uint                  _nested_thread_list_max;
  static volatile uint         _tlh_cnt;
  static volatile uint         _tlh_time_max;
  static volatile uint         _tlh_times;
  static ThreadsList*          _to_delete_list;
  static uint                  _to_delete_list_cnt;
  static uint                  _to_delete_list_max;

  static ThreadsList *acquire_stable_list_fast_path(Thread *self);
  static ThreadsList *acquire_stable_list_nested_path(Thread *self);
  static void add_deleted_thread_times(uint add_value);
  static void add_tlh_times(uint add_value);
  static void clear_delete_notify();
  static bool delete_notify();
  static void free_list(ThreadsList* threads);
  static void inc_deleted_thread_cnt();
  static void inc_java_thread_list_alloc_cnt();
  static void inc_tlh_cnt();
  static void release_stable_list_wake_up(bool is_nested);
  static void set_delete_notify();
  static void threads_do(ThreadClosure *tc);
  static void threads_do(ThreadClosure *tc, ThreadsList *list);
  static void update_deleted_thread_time_max(uint new_value);
  static void update_java_thread_list_max(uint new_value);
  static void update_tlh_time_max(uint new_value);
  static void verify_hazard_ptr_scanned(Thread *self, ThreadsList *threads);
  static ThreadsList* xchg_java_thread_list(ThreadsList* new_list);

 public:
  static void add_thread(JavaThread *thread);
  static ThreadsList* get_java_thread_list();
  static bool is_a_protected_JavaThread(JavaThread *thread);
  static bool is_a_protected_JavaThread_with_lock(JavaThread *thread);
  static void wait_until_not_protected(JavaThread *thread);
  static bool is_bootstrap_list(ThreadsList* list);
  static void remove_thread(JavaThread *thread);
  static void smr_delete(JavaThread *thread);
  static void update_tlh_stats(uint millis);

  // Logging and printing support:
  static void log_statistics();
  static void print_info_elements_on(outputStream* st, ThreadsList* t_list);
  static void print_info_on(outputStream* st);
  static void print_info_on(const Thread* thread, outputStream* st);
};

// A fast list of JavaThreads.
//
class ThreadsList : public CHeapObj<mtThread> {
  enum { THREADS_LIST_MAGIC = (int)(('T' << 24) | ('L' << 16) | ('S' << 8) | 'T') };
  friend class VMStructs;
  friend class SafeThreadsListPtr;  // for {dec,inc}_nested_handle_cnt() access
  friend class ThreadsSMRSupport;  // for _nested_handle_cnt, {add,remove}_thread(), {,set_}next_list() access
  friend class ThreadsListHandleTest;  // for _nested_handle_cnt access

  uint _magic;
  const uint _length;
  ThreadsList* _next_list;
  JavaThread *const *const _threads;
  volatile intx _nested_handle_cnt;

  NONCOPYABLE(ThreadsList);

  template <class T>
  void threads_do_dispatch(T *cl, JavaThread *const thread) const;

  ThreadsList *next_list() const        { return _next_list; }
  void set_next_list(ThreadsList *list) { _next_list = list; }

  void inc_nested_handle_cnt();
  void dec_nested_handle_cnt();

  static ThreadsList* add_thread(ThreadsList* list, JavaThread* java_thread);
  static ThreadsList* remove_thread(ThreadsList* list, JavaThread* java_thread);

public:
  explicit ThreadsList(int entries);
  ~ThreadsList();

  class Iterator;
  inline Iterator begin();
  inline Iterator end();

  template <class T>
  void threads_do(T *cl) const;

  uint length() const                       { return _length; }

  JavaThread *const thread_at(uint i) const { return _threads[i]; }

  JavaThread *const *threads() const        { return _threads; }

  // Returns -1 if target is not found.
  int find_index_of_JavaThread(JavaThread* target);
  JavaThread* find_JavaThread_from_java_tid(jlong java_tid) const;
  bool includes(const JavaThread * const p) const;

#ifdef ASSERT
  static bool is_valid(ThreadsList* list) { return list->_magic == THREADS_LIST_MAGIC; }
#endif
};

class ThreadsList::Iterator {
  JavaThread* const* _thread_ptr;
  DEBUG_ONLY(ThreadsList* _list;)

  static uint check_index(ThreadsList* list, uint i) NOT_DEBUG({ return i; });
  void assert_not_singular() const NOT_DEBUG_RETURN;
  void assert_dereferenceable() const NOT_DEBUG_RETURN;
  void assert_same_list(Iterator i) const NOT_DEBUG_RETURN;

public:
  Iterator() NOT_DEBUG(= default); // Singular iterator.
  inline Iterator(ThreadsList* list, uint i);

  inline bool operator==(Iterator other) const;
  inline bool operator!=(Iterator other) const;

  inline JavaThread* operator*() const;
  inline JavaThread* operator->() const;

  inline Iterator& operator++();
  inline Iterator operator++(int);
};

// An abstract safe ptr to a ThreadsList comprising either a stable hazard ptr
// for leaves, or a retained reference count for nested uses. The user of this
// API does not need to know which mechanism is providing the safety.
class SafeThreadsListPtr {
  friend class ThreadsListHandleTest;  // for access to the fields
  friend class ThreadsListSetter;

  SafeThreadsListPtr* _previous;
  Thread*                 _thread;
  ThreadsList*            _list;
  bool                    _has_ref_count;
  bool                    _needs_release;

  void acquire_stable_list();
  void acquire_stable_list_fast_path();
  void acquire_stable_list_nested_path();

  void release_stable_list();

  void verify_hazard_ptr_scanned();

public:
  // Constructor that attaches the list onto a thread.
  SafeThreadsListPtr(Thread *thread, bool acquire) :
    _previous(NULL),
    _thread(thread),
    _list(NULL),
    _has_ref_count(false),
    _needs_release(false)
  {
    if (acquire) {
      acquire_stable_list();
    }
  }

  // Constructor that transfers ownership of the pointer.
  SafeThreadsListPtr(SafeThreadsListPtr& other) :
    _previous(other._previous),
    _thread(other._thread),
    _list(other._list),
    _has_ref_count(other._has_ref_count),
    _needs_release(other._needs_release)
  {
    other._needs_release = false;
  }

  ~SafeThreadsListPtr() {
    if (_needs_release) {
      release_stable_list();
    }
  }

  ThreadsList* list() const { return _list; }
  SafeThreadsListPtr* previous() const { return _previous; }
  void print_on(outputStream* st);
};

// A helper to optionally set the hazard ptr in ourself. This helper can
// be used by ourself or by another thread. If the hazard ptr is set(),
// then the destructor will release it.
//
class ThreadsListSetter : public StackObj {
private:
  SafeThreadsListPtr _list_ptr;

public:
  ThreadsListSetter() : _list_ptr(Thread::current(), /* acquire */ false) {}
  ThreadsList* list() { return _list_ptr.list(); }
  void set() { _list_ptr.acquire_stable_list(); }
  bool is_set() { return _list_ptr._needs_release; }
};

// This stack allocated ThreadsListHandle keeps all JavaThreads in the
// ThreadsList from being deleted until it is safe.
//
class ThreadsListHandle : public StackObj {
  friend class ThreadsListHandleTest;  // for _list_ptr access

  SafeThreadsListPtr _list_ptr;
  elapsedTimer _timer;  // Enabled via -XX:+EnableThreadSMRStatistics.

public:
  ThreadsListHandle(Thread *self = Thread::current());
  ~ThreadsListHandle();

  ThreadsList *list() const {
    return _list_ptr.list();
  }

  using Iterator = ThreadsList::Iterator;
  inline Iterator begin();
  inline Iterator end();

  template <class T>
  void threads_do(T *cl) const {
    return list()->threads_do(cl);
  }

  bool cv_internal_thread_to_JavaThread(jobject jthread, JavaThread ** jt_pp, oop * thread_oop_p);

  bool includes(JavaThread* p) {
    return list()->includes(p);
  }

  uint length() const {
    return list()->length();
  }

  JavaThread *thread_at(uint i) const {
    return list()->thread_at(i);
  }
};

// This stack allocated JavaThreadIterator is used to walk the
// specified ThreadsList using the following style:
//
//   JavaThreadIterator jti(t_list);
//   for (JavaThread *jt = jti.first(); jt != NULL; jt = jti.next()) {
//     ...
//   }
//
class JavaThreadIterator : public StackObj {
  ThreadsList * _list;
  uint _index;

public:
  JavaThreadIterator(ThreadsList *list) : _list(list), _index(0) {
    assert(list != NULL, "ThreadsList must not be NULL.");
  }

  JavaThread *first() {
    _index = 0;
    return _list->thread_at(_index);
  }

  uint length() const {
    return _list->length();
  }

  ThreadsList *list() const {
    return _list;
  }

  JavaThread *next() {
    if (++_index >= length()) {
      return NULL;
    }
    return _list->thread_at(_index);
  }
};

// This stack allocated ThreadsListHandle and JavaThreadIterator combo
// is used to walk the ThreadsList in the included ThreadsListHandle
// using the following style:
//
//   for (JavaThreadIteratorWithHandle jtiwh; JavaThread *jt = jtiwh.next(); ) {
//     ...
//   }
//
class JavaThreadIteratorWithHandle : public StackObj {
  ThreadsListHandle _tlh;
  uint _index;

public:
  JavaThreadIteratorWithHandle() : _index(0) {}

  uint length() const {
    return _tlh.length();
  }

  ThreadsList *list() const {
    return _tlh.list();
  }

  JavaThread *next() {
    if (_index >= length()) {
      return NULL;
    }
    return _tlh.list()->thread_at(_index++);
  }

  void rewind() {
    _index = 0;
  }
};

#endif // SHARE_RUNTIME_THREADSMR_HPP
