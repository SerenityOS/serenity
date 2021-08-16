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

#ifndef SHARE_GC_SHARED_WEAKPROCESSOR_HPP
#define SHARE_GC_SHARED_WEAKPROCESSOR_HPP

#include "gc/shared/oopStorageParState.hpp"
#include "gc/shared/oopStorageSetParState.hpp"
#include "gc/shared/workgroup.hpp"
#include "memory/allocation.hpp"

class WeakProcessorTimes;
class WorkGang;

// Helper class to aid in root scanning and cleaning of weak oops in the VM.
//
// New containers of weak oops added to this class will automatically
// be cleaned by all GCs, including the young generation GCs.
class WeakProcessor : AllStatic {
public:
  // Visit all oop*s and apply the keep_alive closure if the referenced
  // object is considered alive by the is_alive closure, otherwise do some
  // container specific cleanup of element holding the oop.
  static void weak_oops_do(BoolObjectClosure* is_alive, OopClosure* keep_alive);

  // Visit all oop*s and apply the given closure.
  static void oops_do(OopClosure* closure);

  // Parallel version.  Uses ergo_workers() to determine the number of
  // threads to use, limited by the total workers and phase_times'
  // max_threads.
  // IsAlive must be derived from BoolObjectClosure.
  // KeepAlive must be derived from OopClosure.
  template<typename IsAlive, typename KeepAlive>
  static void weak_oops_do(WorkGang* workers,
                           IsAlive* is_alive,
                           KeepAlive* keep_alive,
                           WeakProcessorTimes* times);

  // Convenience parallel version.  Uses ergo_workers() to determine the
  // number of threads to use, limited by the total workers.  Implicitly
  // logs phase times.
  // IsAlive must be derived from BoolObjectClosure.
  // KeepAlive must be derived from OopClosure.
  template<typename IsAlive, typename KeepAlive>
  static void weak_oops_do(WorkGang* workers,
                           IsAlive* is_alive,
                           KeepAlive* keep_alive,
                           uint indent_log);

  // Uses the total number of weak references and ReferencesPerThread to
  // determine the number of threads to use, limited by max_workers.
  static uint ergo_workers(uint max_workers);

  class Task;

private:
  template<typename IsAlive, typename KeepAlive>
  class CountingClosure;

  class GangTask;
};

class WeakProcessor::Task {
  typedef OopStorage::ParState<false, false> StorageState;

  WeakProcessorTimes* _times;
  uint _nworkers;
  OopStorageSetWeakParState<false, false> _storage_states;

  void initialize();

public:
  Task(uint nworkers);          // No time tracking.
  Task(WeakProcessorTimes* times, uint nworkers);

  template<typename IsAlive, typename KeepAlive>
  void work(uint worker_id, IsAlive* is_alive, KeepAlive* keep_alive);

  void report_num_dead();
};

#endif // SHARE_GC_SHARED_WEAKPROCESSOR_HPP
