/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1YOUNGGCPOSTEVACUATETASKS_HPP
#define SHARE_GC_G1_G1YOUNGGCPOSTEVACUATETASKS_HPP

#include "gc/g1/g1BatchedGangTask.hpp"
#include "gc/g1/g1EvacFailure.hpp"

class FreeCSetStats;

class G1CollectedHeap;
class G1EvacuationInfo;
class G1ParScanThreadStateSet;
class G1RedirtyCardsQueueSet;

// First set of post evacuate collection set tasks containing ("s" means serial):
// - Merge PSS (s)
// - Recalculate Used (s)
// - Sample Collection Set Candidates (s)
// - Remove Self Forwards (on evacuation failure)
// - Clear Card Table
class G1PostEvacuateCollectionSetCleanupTask1 : public G1BatchedGangTask {
  class MergePssTask;
  class RecalculateUsedTask;
  class SampleCollectionSetCandidatesTask;
  class RemoveSelfForwardPtrsTask;

public:
  G1PostEvacuateCollectionSetCleanupTask1(G1ParScanThreadStateSet* per_thread_states,
                                          G1RedirtyCardsQueueSet* rdcqs);
};

class G1PostEvacuateCollectionSetCleanupTask1::MergePssTask : public G1AbstractSubTask {
  G1ParScanThreadStateSet* _per_thread_states;

public:
  MergePssTask(G1ParScanThreadStateSet* per_thread_states);

  double worker_cost() const override { return 1.0; }
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask1::RecalculateUsedTask : public G1AbstractSubTask {
public:
  RecalculateUsedTask() : G1AbstractSubTask(G1GCPhaseTimes::RecalculateUsed) { }

  double worker_cost() const override;
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask1::SampleCollectionSetCandidatesTask : public G1AbstractSubTask {
public:
  SampleCollectionSetCandidatesTask() : G1AbstractSubTask(G1GCPhaseTimes::SampleCollectionSetCandidates) { }

  static bool should_execute();

  double worker_cost() const override;
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask1::RemoveSelfForwardPtrsTask : public G1AbstractSubTask {
  G1ParRemoveSelfForwardPtrsTask _task;

public:
  RemoveSelfForwardPtrsTask(G1RedirtyCardsQueueSet* rdcqs);
  ~RemoveSelfForwardPtrsTask();

  static bool should_execute();

  double worker_cost() const override;
  void do_work(uint worker_id) override;
};

// Second set of post evacuate collection set tasks containing (s means serial):
// - Eagerly Reclaim Humongous Objects (s)
// - Purge Code Roots (s)
// - Reset Hot Card Cache (s)
// - Update Derived Pointers (s)
// - Redirty Logged Cards
// - Restore Preserved Marks (on evacuation failure)
// - Free Collection Set
class G1PostEvacuateCollectionSetCleanupTask2 : public G1BatchedGangTask {
  class EagerlyReclaimHumongousObjectsTask;
  class PurgeCodeRootsTask;
  class ResetHotCardCacheTask;
#if COMPILER2_OR_JVMCI
  class UpdateDerivedPointersTask;
#endif

  class RedirtyLoggedCardsTask;
  class RestorePreservedMarksTask;
  class FreeCollectionSetTask;

public:
  G1PostEvacuateCollectionSetCleanupTask2(PreservedMarksSet* preserved_marks_set,
                                          G1RedirtyCardsQueueSet* rdcqs,
                                          G1EvacuationInfo* evacuation_info,
                                          const size_t* surviving_young_words);
};

class G1PostEvacuateCollectionSetCleanupTask2::ResetHotCardCacheTask : public G1AbstractSubTask {
public:
  ResetHotCardCacheTask() : G1AbstractSubTask(G1GCPhaseTimes::ResetHotCardCache) { }

  double worker_cost() const override { return 0.5; }
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask2::PurgeCodeRootsTask : public G1AbstractSubTask {
public:
  PurgeCodeRootsTask() : G1AbstractSubTask(G1GCPhaseTimes::PurgeCodeRoots) { }

  double worker_cost() const override { return 1.0; }
  void do_work(uint worker_id) override;
};

#if COMPILER2_OR_JVMCI
class G1PostEvacuateCollectionSetCleanupTask2::UpdateDerivedPointersTask : public G1AbstractSubTask {
public:
  UpdateDerivedPointersTask() : G1AbstractSubTask(G1GCPhaseTimes::UpdateDerivedPointers) { }

  double worker_cost() const override { return 1.0; }
  void do_work(uint worker_id) override;
};
#endif

class G1PostEvacuateCollectionSetCleanupTask2::EagerlyReclaimHumongousObjectsTask : public G1AbstractSubTask {
  uint _humongous_regions_reclaimed;
  size_t _bytes_freed;

public:
  EagerlyReclaimHumongousObjectsTask();
  virtual ~EagerlyReclaimHumongousObjectsTask();

  static bool should_execute();

  double worker_cost() const override { return 1.0; }
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask2::RestorePreservedMarksTask : public G1AbstractSubTask {
  PreservedMarksSet* _preserved_marks;
  AbstractGangTask* _task;

public:
  RestorePreservedMarksTask(PreservedMarksSet* preserved_marks);
  virtual ~RestorePreservedMarksTask();

  static bool should_execute();

  double worker_cost() const override;
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask2::RedirtyLoggedCardsTask : public G1AbstractSubTask {
  G1RedirtyCardsQueueSet* _rdcqs;
  BufferNode* volatile _nodes;

public:
  RedirtyLoggedCardsTask(G1RedirtyCardsQueueSet* rdcqs);
  virtual ~RedirtyLoggedCardsTask();

  double worker_cost() const override;
  void do_work(uint worker_id) override;
};

class G1PostEvacuateCollectionSetCleanupTask2::FreeCollectionSetTask : public G1AbstractSubTask {
  G1CollectedHeap*  _g1h;
  G1EvacuationInfo* _evacuation_info;
  FreeCSetStats*    _worker_stats;
  HeapRegionClaimer _claimer;
  const size_t*     _surviving_young_words;
  uint              _active_workers;

  FreeCSetStats* worker_stats(uint worker);
  void report_statistics();

public:
  FreeCollectionSetTask(G1EvacuationInfo* evacuation_info, const size_t* surviving_young_words);
  virtual ~FreeCollectionSetTask();

  double worker_cost() const override;
  void set_max_workers(uint max_workers) override;

  void do_work(uint worker_id) override;
};

#endif // SHARE_GC_G1_G1YOUNGGCPOSTEVACUATETASKS_HPP

