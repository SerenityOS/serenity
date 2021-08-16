/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_MEMTRACKER_HPP
#define SHARE_SERVICES_MEMTRACKER_HPP

#include "services/nmtCommon.hpp"
#include "utilities/nativeCallStack.hpp"


#if !INCLUDE_NMT

#define CURRENT_PC   NativeCallStack::empty_stack()
#define CALLER_PC    NativeCallStack::empty_stack()

class Tracker : public StackObj {
 public:
  enum TrackerType {
     uncommit,
     release
  };
  Tracker(enum TrackerType type) : _type(type) { }
  void record(address addr, size_t size) { }
 private:
  enum TrackerType  _type;
};

class MemTracker : AllStatic {
 public:
  static inline NMT_TrackingLevel tracking_level() { return NMT_off; }
  static inline void shutdown() { }
  static inline void init() { }
  static bool check_launcher_nmt_support(const char* value) { return true; }
  static bool verify_nmt_option() { return true; }

  static inline void* record_malloc(void* mem_base, size_t size, MEMFLAGS flag,
    const NativeCallStack& stack, NMT_TrackingLevel level) { return mem_base; }
  static inline size_t malloc_header_size(NMT_TrackingLevel level) { return 0; }
  static inline size_t malloc_header_size(void* memblock) { return 0; }
  static inline void* malloc_base(void* memblock) { return memblock; }
  static inline void* record_free(void* memblock, NMT_TrackingLevel level) { return memblock; }

  static inline void record_new_arena(MEMFLAGS flag) { }
  static inline void record_arena_free(MEMFLAGS flag) { }
  static inline void record_arena_size_change(ssize_t diff, MEMFLAGS flag) { }
  static inline void record_virtual_memory_reserve(void* addr, size_t size, const NativeCallStack& stack,
                       MEMFLAGS flag = mtNone) { }
  static inline void record_virtual_memory_reserve_and_commit(void* addr, size_t size,
    const NativeCallStack& stack, MEMFLAGS flag = mtNone) { }
  static inline void record_virtual_memory_split_reserved(void* addr, size_t size, size_t split) { }
  static inline void record_virtual_memory_commit(void* addr, size_t size, const NativeCallStack& stack) { }
  static inline void record_virtual_memory_type(void* addr, MEMFLAGS flag) { }
  static inline void record_thread_stack(void* addr, size_t size) { }
  static inline void release_thread_stack(void* addr, size_t size) { }

  static void final_report(outputStream*) { }
  static void error_report(outputStream*) { }
};

#else

#include "runtime/mutexLocker.hpp"
#include "runtime/threadCritical.hpp"
#include "services/mallocTracker.hpp"
#include "services/threadStackTracker.hpp"
#include "services/virtualMemoryTracker.hpp"

#define CURRENT_PC ((MemTracker::tracking_level() == NMT_detail) ? \
                    NativeCallStack(0) : NativeCallStack::empty_stack())
#define CALLER_PC  ((MemTracker::tracking_level() == NMT_detail) ?  \
                    NativeCallStack(1) : NativeCallStack::empty_stack())

class MemBaseline;

// Tracker is used for guarding 'release' semantics of virtual memory operation, to avoid
// the other thread obtains and records the same region that is just 'released' by current
// thread but before it can record the operation.
class Tracker : public StackObj {
 public:
  enum TrackerType {
     uncommit,
     release
  };

 public:
  Tracker(enum TrackerType type) : _type(type) { }
  void record(address addr, size_t size);
 private:
  enum TrackerType  _type;
  // Virtual memory tracking data structures are protected by ThreadCritical lock.
  ThreadCritical    _tc;
};

class MemTracker : AllStatic {
  friend class VirtualMemoryTrackerTest;

  // Helper; asserts that we are in post-NMT-init phase
  static void assert_post_init() {
    assert(is_initialized(), "NMT not yet initialized.");
  }

 public:

  // Initializes NMT to whatever -XX:NativeMemoryTracking says.
  //  - Can only be called once.
  //  - NativeMemoryTracking must be validated beforehand.
  static void initialize();

  // Returns true if NMT had been initialized.
  static bool is_initialized()  {
    return _tracking_level != NMT_unknown;
  }

  static inline NMT_TrackingLevel tracking_level() {
    return _tracking_level;
  }

  // Shutdown native memory tracking.
  // This transitions the tracking level:
  //  summary -> minimal
  //  detail  -> minimal
  static void shutdown();

  // Transition the tracking level to specified level
  static bool transition_to(NMT_TrackingLevel level);

  static inline void* record_malloc(void* mem_base, size_t size, MEMFLAGS flag,
    const NativeCallStack& stack, NMT_TrackingLevel level) {
    if (level != NMT_off) {
      return MallocTracker::record_malloc(mem_base, size, flag, stack, level);
    }
    return mem_base;
  }

  static inline size_t malloc_header_size(NMT_TrackingLevel level) {
    return MallocTracker::malloc_header_size(level);
  }

  static size_t malloc_header_size(void* memblock) {
    if (tracking_level() != NMT_off) {
      return MallocTracker::get_header_size(memblock);
    }
    return 0;
  }

  // To malloc base address, which is the starting address
  // of malloc tracking header if tracking is enabled.
  // Otherwise, it returns the same address.
  static void* malloc_base(void* memblock);

  // Record malloc free and return malloc base address
  static inline void* record_free(void* memblock, NMT_TrackingLevel level) {
    // Never turned on
    if (level == NMT_off || memblock == NULL) {
      return memblock;
    }
    return MallocTracker::record_free(memblock);
  }


  // Record creation of an arena
  static inline void record_new_arena(MEMFLAGS flag) {
    if (tracking_level() < NMT_summary) return;
    MallocTracker::record_new_arena(flag);
  }

  // Record destruction of an arena
  static inline void record_arena_free(MEMFLAGS flag) {
    if (tracking_level() < NMT_summary) return;
    MallocTracker::record_arena_free(flag);
  }

  // Record arena size change. Arena size is the size of all arena
  // chuncks that backing up the arena.
  static inline void record_arena_size_change(ssize_t diff, MEMFLAGS flag) {
    if (tracking_level() < NMT_summary) return;
    MallocTracker::record_arena_size_change(diff, flag);
  }

  // Note: virtual memory operations should only ever be called after NMT initialization
  //  (we do not do any reservations before that).

  static inline void record_virtual_memory_reserve(void* addr, size_t size, const NativeCallStack& stack,
    MEMFLAGS flag = mtNone) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadCritical tc;
      // Recheck to avoid potential racing during NMT shutdown
      if (tracking_level() < NMT_summary) return;
      VirtualMemoryTracker::add_reserved_region((address)addr, size, stack, flag);
    }
  }

  static inline void record_virtual_memory_reserve_and_commit(void* addr, size_t size,
    const NativeCallStack& stack, MEMFLAGS flag = mtNone) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadCritical tc;
      if (tracking_level() < NMT_summary) return;
      VirtualMemoryTracker::add_reserved_region((address)addr, size, stack, flag);
      VirtualMemoryTracker::add_committed_region((address)addr, size, stack);
    }
  }

  static inline void record_virtual_memory_commit(void* addr, size_t size,
    const NativeCallStack& stack) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadCritical tc;
      if (tracking_level() < NMT_summary) return;
      VirtualMemoryTracker::add_committed_region((address)addr, size, stack);
    }
  }

  // Given an existing memory mapping registered with NMT and a splitting
  //  address, split the mapping in two. The memory region is supposed to
  //  be fully uncommitted.
  //
  // The two new memory regions will be both registered under stack and
  //  memory flags of the original region.
  static inline void record_virtual_memory_split_reserved(void* addr, size_t size, size_t split) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadCritical tc;
      // Recheck to avoid potential racing during NMT shutdown
      if (tracking_level() < NMT_summary) return;
      VirtualMemoryTracker::split_reserved_region((address)addr, size, split);
    }
  }

  static inline void record_virtual_memory_type(void* addr, MEMFLAGS flag) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadCritical tc;
      if (tracking_level() < NMT_summary) return;
      VirtualMemoryTracker::set_reserved_region_type((address)addr, flag);
    }
  }

  static void record_thread_stack(void* addr, size_t size) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadStackTracker::new_thread_stack((address)addr, size, CALLER_PC);
    }
  }

  static inline void release_thread_stack(void* addr, size_t size) {
    assert_post_init();
    if (tracking_level() < NMT_summary) return;
    if (addr != NULL) {
      ThreadStackTracker::delete_thread_stack((address)addr, size);
    }
  }

  // Query lock is used to synchronize the access to tracking data.
  // So far, it is only used by JCmd query, but it may be used by
  // other tools.
  static inline Mutex* query_lock() {
    assert(NMTQuery_lock != NULL, "not initialized!");
    return NMTQuery_lock;
  }

  // Report during error reporting.
  static void error_report(outputStream* output);

  // Report when handling PrintNMTStatistics before VM shutdown.
  static void final_report(outputStream* output);

  // Stored baseline
  static inline MemBaseline& get_baseline() {
    return _baseline;
  }

  static NMT_TrackingLevel cmdline_tracking_level() {
    return _cmdline_tracking_level;
  }

  static void tuning_statistics(outputStream* out);

 private:
  static NMT_TrackingLevel init_tracking_level();
  static void report(bool summary_only, outputStream* output, size_t scale);

 private:
  // Tracking level
  static volatile NMT_TrackingLevel   _tracking_level;
  // If NMT option value passed by launcher through environment
  // variable is valid
  static bool                         _is_nmt_env_valid;
  // command line tracking level
  static NMT_TrackingLevel            _cmdline_tracking_level;
  // Stored baseline
  static MemBaseline      _baseline;
  // Query lock
  static Mutex*           _query_lock;
};

#endif // INCLUDE_NMT

#endif // SHARE_SERVICES_MEMTRACKER_HPP
