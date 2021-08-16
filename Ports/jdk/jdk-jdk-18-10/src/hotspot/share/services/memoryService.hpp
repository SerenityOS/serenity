/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_MEMORYSERVICE_HPP
#define SHARE_SERVICES_MEMORYSERVICE_HPP

#include "gc/shared/gcCause.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "runtime/handles.hpp"
#include "services/memoryUsage.hpp"
#include "utilities/growableArray.hpp"

// Forward declaration
class MemoryPool;
class MemoryManager;
class GCMemoryManager;
class CollectedHeap;
class CodeHeap;

// VM Monitoring and Management Support

class MemoryService : public AllStatic {
private:
  enum {
    init_pools_list_size = 10,
    init_managers_list_size = 5,
    init_code_heap_pools_size = 9
  };

  static GrowableArray<MemoryPool*>*    _pools_list;
  static GrowableArray<MemoryManager*>* _managers_list;

  // memory manager and code heap pools for the CodeCache
  static MemoryManager*                 _code_cache_manager;
  static GrowableArray<MemoryPool*>*    _code_heap_pools;

  static MemoryPool*                    _metaspace_pool;
  static MemoryPool*                    _compressed_class_pool;

public:
  static void set_universe_heap(CollectedHeap* heap);
  static void add_code_heap_memory_pool(CodeHeap* heap, const char* name);
  static void add_metaspace_memory_pools();

  static MemoryPool*    get_memory_pool(instanceHandle pool);
  static MemoryManager* get_memory_manager(instanceHandle mgr);

  static const int num_memory_pools() {
    return _pools_list->length();
  }
  static const int num_memory_managers() {
    return _managers_list->length();
  }

  static MemoryPool* get_memory_pool(int index) {
    return _pools_list->at(index);
  }

  static MemoryManager* get_memory_manager(int index) {
    return _managers_list->at(index);
  }

  static void track_memory_usage();
  static void track_code_cache_memory_usage() {
    // Track memory pool usage of all CodeCache memory pools
    for (int i = 0; i < _code_heap_pools->length(); ++i) {
      track_memory_pool_usage(_code_heap_pools->at(i));
    }
  }
  static void track_metaspace_memory_usage() {
    track_memory_pool_usage(_metaspace_pool);
  }
  static void track_compressed_class_memory_usage() {
    track_memory_pool_usage(_compressed_class_pool);
  }
  static void track_memory_pool_usage(MemoryPool* pool);

  static void gc_begin(GCMemoryManager* manager, bool recordGCBeginTime,
                       bool recordAccumulatedGCTime,
                       bool recordPreGCUsage, bool recordPeakUsage);
  static void gc_end(GCMemoryManager* manager, bool recordPostGCUsage,
                     bool recordAccumulatedGCTime,
                     bool recordGCEndTime, bool countCollection,
                     GCCause::Cause cause,
                     bool allMemoryPoolsAffected);

  static bool get_verbose() { return log_is_enabled(Info, gc); }
  static bool set_verbose(bool verbose);

  // Create an instance of java/lang/management/MemoryUsage
  static Handle create_MemoryUsage_obj(MemoryUsage usage, TRAPS);
};

class TraceMemoryManagerStats : public StackObj {
private:
  GCMemoryManager* _gc_memory_manager;
  bool         _allMemoryPoolsAffected;
  bool         _recordGCBeginTime;
  bool         _recordPreGCUsage;
  bool         _recordPeakUsage;
  bool         _recordPostGCUsage;
  bool         _recordAccumulatedGCTime;
  bool         _recordGCEndTime;
  bool         _countCollection;
  GCCause::Cause _cause;
public:
  TraceMemoryManagerStats() {}
  TraceMemoryManagerStats(GCMemoryManager* gc_memory_manager,
                          GCCause::Cause cause,
                          bool allMemoryPoolsAffected = true,
                          bool recordGCBeginTime = true,
                          bool recordPreGCUsage = true,
                          bool recordPeakUsage = true,
                          bool recordPostGCUsage = true,
                          bool recordAccumulatedGCTime = true,
                          bool recordGCEndTime = true,
                          bool countCollection = true);

  void initialize(GCMemoryManager* gc_memory_manager,
                  GCCause::Cause cause,
                  bool allMemoryPoolsAffected,
                  bool recordGCBeginTime,
                  bool recordPreGCUsage,
                  bool recordPeakUsage,
                  bool recordPostGCUsage,
                  bool recordAccumulatedGCTime,
                  bool recordGCEndTime,
                  bool countCollection);

  ~TraceMemoryManagerStats();
};

#endif // SHARE_SERVICES_MEMORYSERVICE_HPP
