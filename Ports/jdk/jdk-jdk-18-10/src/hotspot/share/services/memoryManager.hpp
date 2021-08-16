/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_MEMORYMANAGER_HPP
#define SHARE_SERVICES_MEMORYMANAGER_HPP

#include "gc/shared/gcCause.hpp"
#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/handles.hpp"
#include "runtime/timer.hpp"
#include "services/memoryUsage.hpp"

// A memory manager is responsible for managing one or more memory pools.
// The garbage collector is one type of memory managers responsible
// for reclaiming memory occupied by unreachable objects.  A Java virtual
// machine may have one or more memory managers.   It may
// add or remove memory managers during execution.
// A memory pool can be managed by more than one memory managers.

class MemoryPool;
class GCMemoryManager;
class OopClosure;

class MemoryManager : public CHeapObj<mtInternal> {
protected:
  enum {
    max_num_pools = 10
  };

private:
  MemoryPool* _pools[max_num_pools];
  int         _num_pools;

  const char* _name;

protected:
  volatile OopHandle _memory_mgr_obj;

public:
  MemoryManager(const char* name);

  int num_memory_pools() const           { return _num_pools; }
  MemoryPool* get_memory_pool(int index) {
    assert(index >= 0 && index < _num_pools, "Invalid index");
    return _pools[index];
  }

  int add_pool(MemoryPool* pool);

  bool is_manager(instanceHandle mh) const;

  virtual instanceOop get_memory_manager_instance(TRAPS);
  virtual bool is_gc_memory_manager()    { return false; }

  const char* name() const { return _name; }

  // Static factory methods to get a memory manager of a specific type
  static MemoryManager*   get_code_cache_memory_manager();
  static MemoryManager*   get_metaspace_memory_manager();
};

class GCStatInfo : public ResourceObj {
private:
  size_t _index;
  jlong  _start_time;
  jlong  _end_time;

  // We keep memory usage of all memory pools
  MemoryUsage* _before_gc_usage_array;
  MemoryUsage* _after_gc_usage_array;
  int          _usage_array_size;

  void set_gc_usage(int pool_index, MemoryUsage, bool before_gc);

public:
  GCStatInfo(int num_pools);
  ~GCStatInfo();

  size_t gc_index()               { return _index; }
  jlong  start_time()             { return _start_time; }
  jlong  end_time()               { return _end_time; }
  int    usage_array_size()       { return _usage_array_size; }
  MemoryUsage before_gc_usage_for_pool(int pool_index) {
    assert(pool_index >= 0 && pool_index < _usage_array_size, "Range checking");
    return _before_gc_usage_array[pool_index];
  }
  MemoryUsage after_gc_usage_for_pool(int pool_index) {
    assert(pool_index >= 0 && pool_index < _usage_array_size, "Range checking");
    return _after_gc_usage_array[pool_index];
  }

  MemoryUsage* before_gc_usage_array() { return _before_gc_usage_array; }
  MemoryUsage* after_gc_usage_array()  { return _after_gc_usage_array; }

  void set_index(size_t index)    { _index = index; }
  void set_start_time(jlong time) { _start_time = time; }
  void set_end_time(jlong time)   { _end_time = time; }
  void set_before_gc_usage(int pool_index, MemoryUsage usage) {
    assert(pool_index >= 0 && pool_index < _usage_array_size, "Range checking");
    set_gc_usage(pool_index, usage, true /* before gc */);
  }
  void set_after_gc_usage(int pool_index, MemoryUsage usage) {
    assert(pool_index >= 0 && pool_index < _usage_array_size, "Range checking");
    set_gc_usage(pool_index, usage, false /* after gc */);
  }

  void clear();
};

class GCMemoryManager : public MemoryManager {
private:
  // TODO: We should unify the GCCounter and GCMemoryManager statistic
  size_t       _num_collections;
  elapsedTimer _accumulated_timer;
  GCStatInfo*  _last_gc_stat;
  Mutex*       _last_gc_lock;
  GCStatInfo*  _current_gc_stat;
  int          _num_gc_threads;
  volatile bool _notification_enabled;
  const char*  _gc_end_message;
  bool         _pool_always_affected_by_gc[MemoryManager::max_num_pools];

public:
  GCMemoryManager(const char* name, const char* gc_end_message);
  ~GCMemoryManager();

  void add_pool(MemoryPool* pool);
  void add_pool(MemoryPool* pool, bool always_affected_by_gc);

  bool pool_always_affected_by_gc(int index) {
    assert(index >= 0 && index < num_memory_pools(), "Invalid index");
    return _pool_always_affected_by_gc[index];
  }

  void   initialize_gc_stat_info();

  bool   is_gc_memory_manager()         { return true; }
  jlong  gc_time_ms()                   { return _accumulated_timer.milliseconds(); }
  size_t gc_count()                     { return _num_collections; }
  int    num_gc_threads()               { return _num_gc_threads; }
  void   set_num_gc_threads(int count)  { _num_gc_threads = count; }

  void   gc_begin(bool recordGCBeginTime, bool recordPreGCUsage,
                  bool recordAccumulatedGCTime);
  void   gc_end(bool recordPostGCUsage, bool recordAccumulatedGCTime,
                bool recordGCEndTime, bool countCollection, GCCause::Cause cause,
                bool allMemoryPoolsAffected);

  void        reset_gc_stat()   { _num_collections = 0; _accumulated_timer.reset(); }

  // Copy out _last_gc_stat to the given destination, returning
  // the collection count. Zero signifies no gc has taken place.
  size_t get_last_gc_stat(GCStatInfo* dest);

  void set_notification_enabled(bool enabled) { _notification_enabled = enabled; }
  bool is_notification_enabled() { return _notification_enabled; }
};

#endif // SHARE_SERVICES_MEMORYMANAGER_HPP
