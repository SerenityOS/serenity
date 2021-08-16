
/*
* Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "runtime/atomic.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/timerTrace.hpp"
#include "services/threadIdTable.hpp"
#include "utilities/concurrentHashTable.inline.hpp"
#include "utilities/concurrentHashTableTasks.inline.hpp"

typedef ConcurrentHashTable<ThreadIdTableConfig, mtInternal> ThreadIdTableHash;

// 2^24 is max size
static const size_t END_SIZE = 24;
// Default initial size 256
static const size_t DEFAULT_TABLE_SIZE_LOG = 8;
// Prefer short chains of avg 2
static const double PREF_AVG_LIST_LEN = 2.0;
static ThreadIdTableHash* volatile _local_table = NULL;
static volatile size_t _current_size = 0;
static volatile size_t _items_count = 0;

volatile bool ThreadIdTable::_is_initialized = false;
volatile bool ThreadIdTable::_has_work = false;

class ThreadIdTableEntry : public CHeapObj<mtInternal> {
private:
  jlong _tid;
  JavaThread* _java_thread;
public:
  ThreadIdTableEntry(jlong tid, JavaThread* java_thread) :
    _tid(tid), _java_thread(java_thread) {}

  jlong tid() const { return _tid; }
  JavaThread* thread() const { return _java_thread; }
};

class ThreadIdTableConfig : public AllStatic {
  public:
    typedef ThreadIdTableEntry* Value;

    static uintx get_hash(Value const& value, bool* is_dead) {
      jlong tid = value->tid();
      return primitive_hash(tid);
    }
    static void* allocate_node(void* context, size_t size, Value const& value) {
      ThreadIdTable::item_added();
      return AllocateHeap(size, mtInternal);
    }
    static void free_node(void* context, void* memory, Value const& value) {
      delete value;
      FreeHeap(memory);
      ThreadIdTable::item_removed();
    }
};

static size_t ceil_log2(size_t val) {
  size_t ret;
  for (ret = 1; ((size_t)1 << ret) < val; ++ret);
  return ret;
}

// Lazily creates the table and populates it with the given
// thread list
void ThreadIdTable::lazy_initialize(const ThreadsList *threads) {
  if (!_is_initialized) {
    {
      // There is no obvious benefits in allowing the thread table
      // to be concurently populated during the initalization.
      MutexLocker ml(ThreadIdTableCreate_lock);
      if (_is_initialized) {
        return;
      }
      create_table(threads->length());
      _is_initialized = true;
    }
    for (uint i = 0; i < threads->length(); i++) {
      JavaThread* thread = threads->thread_at(i);
      oop tobj = thread->threadObj();
      if (tobj != NULL) {
        jlong java_tid = java_lang_Thread::thread_id(tobj);
        MutexLocker ml(Threads_lock);
        if (!thread->is_exiting()) {
          // Must be inside the lock to ensure that we don't add a thread to the table
          // that has just passed the removal point in ThreadsSMRSupport::remove_thread()
          add_thread(java_tid, thread);
        }
      }
    }
  }
}

void ThreadIdTable::create_table(size_t size) {
  assert(_local_table == NULL, "Thread table is already created");
  size_t size_log = ceil_log2(size);
  size_t start_size_log =
      size_log > DEFAULT_TABLE_SIZE_LOG ? size_log : DEFAULT_TABLE_SIZE_LOG;
  _current_size = (size_t)1 << start_size_log;
  _local_table = new ThreadIdTableHash(start_size_log, END_SIZE);
}

void ThreadIdTable::item_added() {
  Atomic::inc(&_items_count);
  log_trace(thread, table) ("Thread entry added");
}

void ThreadIdTable::item_removed() {
  Atomic::dec(&_items_count);
  log_trace(thread, table) ("Thread entry removed");
}

double ThreadIdTable::get_load_factor() {
  return ((double)_items_count) / _current_size;
}

size_t ThreadIdTable::table_size() {
  return (size_t)1 << _local_table->get_size_log2(Thread::current());
}

void ThreadIdTable::check_concurrent_work() {
  if (_has_work) {
    return;
  }

  double load_factor = get_load_factor();
  // Resize if we have more items than preferred load factor
  if ( load_factor > PREF_AVG_LIST_LEN && !_local_table->is_max_size_reached()) {
    log_debug(thread, table)("Concurrent work triggered, load factor: %g",
                             load_factor);
    trigger_concurrent_work();
  }
}

void ThreadIdTable::trigger_concurrent_work() {
  MutexLocker ml(Service_lock, Mutex::_no_safepoint_check_flag);
  _has_work = true;
  Service_lock->notify_all();
}

void ThreadIdTable::grow(JavaThread* jt) {
  ThreadIdTableHash::GrowTask gt(_local_table);
  if (!gt.prepare(jt)) {
    return;
  }
  log_trace(thread, table)("Started to grow");
  TraceTime timer("Grow", TRACETIME_LOG(Debug, membername, table, perf));
  while (gt.do_task(jt)) {
    gt.pause(jt);
    {
      ThreadBlockInVM tbivm(jt);
    }
    gt.cont(jt);
  }
  gt.done(jt);
  _current_size = table_size();
  log_info(thread, table)("Grown to size:" SIZE_FORMAT, _current_size);
}

class ThreadIdTableLookup : public StackObj {
private:
  jlong _tid;
  uintx _hash;
public:
  ThreadIdTableLookup(jlong tid)
    : _tid(tid), _hash(primitive_hash(tid)) {}
  uintx get_hash() const {
    return _hash;
  }
  bool equals(ThreadIdTableEntry** value, bool* is_dead) {
    bool equals = primitive_equals(_tid, (*value)->tid());
    if (!equals) {
      return false;
    }
    return true;
  }
};

class ThreadGet : public StackObj {
private:
  JavaThread* _return;
public:
  ThreadGet(): _return(NULL) {}
  void operator()(ThreadIdTableEntry** val) {
    _return = (*val)->thread();
  }
  JavaThread* get_res_thread() {
    return _return;
  }
};

void ThreadIdTable::do_concurrent_work(JavaThread* jt) {
  assert(_is_initialized, "Thread table is not initialized");
  _has_work = false;
  double load_factor = get_load_factor();
  log_debug(thread, table)("Concurrent work, load factor: %g", load_factor);
  if (load_factor > PREF_AVG_LIST_LEN && !_local_table->is_max_size_reached()) {
    grow(jt);
  }
}

JavaThread* ThreadIdTable::add_thread(jlong tid, JavaThread* java_thread) {
  assert(_is_initialized, "Thread table is not initialized");
  Thread* thread = Thread::current();
  ThreadIdTableLookup lookup(tid);
  ThreadGet tg;
  while (true) {
    if (_local_table->get(thread, lookup, tg)) {
      return tg.get_res_thread();
    }
    ThreadIdTableEntry* entry = new ThreadIdTableEntry(tid, java_thread);
    // The hash table takes ownership of the ThreadTableEntry,
    // even if it's not inserted.
    if (_local_table->insert(thread, lookup, entry)) {
      check_concurrent_work();
      return java_thread;
    }
  }
}

JavaThread* ThreadIdTable::find_thread_by_tid(jlong tid) {
  assert(_is_initialized, "Thread table is not initialized");
  Thread* thread = Thread::current();
  ThreadIdTableLookup lookup(tid);
  ThreadGet tg;
  _local_table->get(thread, lookup, tg);
  return tg.get_res_thread();
}

bool ThreadIdTable::remove_thread(jlong tid) {
  assert(_is_initialized, "Thread table is not initialized");
  Thread* thread = Thread::current();
  ThreadIdTableLookup lookup(tid);
  return _local_table->remove(thread, lookup);
}
