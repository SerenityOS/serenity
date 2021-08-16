/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_DEPENDENCYCONTEXT_HPP
#define SHARE_CODE_DEPENDENCYCONTEXT_HPP

#include "memory/allocation.hpp"
#include "oops/oop.hpp"
#include "runtime/handles.hpp"
#include "runtime/perfData.hpp"
#include "runtime/safepoint.hpp"

class nmethod;
class DepChange;

//
// nmethodBucket is used to record dependent nmethods for
// deoptimization.  nmethod dependencies are actually <klass, method>
// pairs but we really only care about the klass part for purposes of
// finding nmethods which might need to be deoptimized.  Instead of
// recording the method, a count of how many times a particular nmethod
// was recorded is kept.  This ensures that any recording errors are
// noticed since an nmethod should be removed as many times are it's
// added.
//
class nmethodBucket: public CHeapObj<mtClass> {
  friend class VMStructs;
 private:
  nmethod*       _nmethod;
  volatile int   _count;
  nmethodBucket* volatile _next;
  nmethodBucket* volatile _purge_list_next;

 public:
  nmethodBucket(nmethod* nmethod, nmethodBucket* next) :
    _nmethod(nmethod), _count(1), _next(next), _purge_list_next(NULL) {}

  int count()                                { return _count; }
  int increment()                            { _count += 1; return _count; }
  int decrement();
  nmethodBucket* next();
  nmethodBucket* next_not_unloading();
  void set_next(nmethodBucket* b);
  nmethodBucket* purge_list_next();
  void set_purge_list_next(nmethodBucket* b);
  nmethod* get_nmethod()                     { return _nmethod; }
};

//
// Utility class to manipulate nmethod dependency context.
// Dependency context can be attached either to an InstanceKlass (_dep_context field)
// or CallSiteContext oop for call_site_target dependencies (see javaClasses.hpp).
// DependencyContext class operates on some location which holds a nmethodBucket* value
// and uint64_t integer recording the safepoint counter at the last cleanup.
//
class DependencyContext : public StackObj {
  friend class VMStructs;
  friend class TestDependencyContext;
 private:
  nmethodBucket* volatile* _dependency_context_addr;
  volatile uint64_t*       _last_cleanup_addr;

  bool claim_cleanup();
  void set_dependencies(nmethodBucket* b);
  nmethodBucket* dependencies();
  nmethodBucket* dependencies_not_unloading();

  static PerfCounter*            _perf_total_buckets_allocated_count;
  static PerfCounter*            _perf_total_buckets_deallocated_count;
  static PerfCounter*            _perf_total_buckets_stale_count;
  static PerfCounter*            _perf_total_buckets_stale_acc_count;
  static nmethodBucket* volatile _purge_list;
  static uint64_t                _cleaning_epoch_monotonic;
  static volatile uint64_t       _cleaning_epoch;

 public:
#ifdef ASSERT
  // Safepoints are forbidden during DC lifetime. GC can invalidate
  // _dependency_context_addr if it relocates the holder
  // (e.g. CallSiteContext Java object).
  SafepointStateTracker _safepoint_tracker;

  DependencyContext(nmethodBucket* volatile* bucket_addr, volatile uint64_t* last_cleanup_addr)
    : _dependency_context_addr(bucket_addr),
      _last_cleanup_addr(last_cleanup_addr),
      _safepoint_tracker(SafepointSynchronize::safepoint_state_tracker()) {}

  ~DependencyContext() {
    assert(!_safepoint_tracker.safepoint_state_changed(), "must be the same safepoint");
  }
#else
  DependencyContext(nmethodBucket* volatile* bucket_addr, volatile uint64_t* last_cleanup_addr)
    : _dependency_context_addr(bucket_addr),
      _last_cleanup_addr(last_cleanup_addr) {}
#endif // ASSERT

  static void init();

  int  mark_dependent_nmethods(DepChange& changes);
  void add_dependent_nmethod(nmethod* nm);
  void remove_dependent_nmethod(nmethod* nm);
  int  remove_all_dependents();
  void clean_unloading_dependents();
  static void purge_dependency_contexts();
  static void release(nmethodBucket* b);
  static void cleaning_start();
  static void cleaning_end();

#ifndef PRODUCT
  void print_dependent_nmethods(bool verbose);
#endif //PRODUCT
  bool is_dependent_nmethod(nmethod* nm);
};
#endif // SHARE_CODE_DEPENDENCYCONTEXT_HPP
