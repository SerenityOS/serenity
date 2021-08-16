/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/copyFailedInfo.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "gc/shared/gcTimer.hpp"
#include "gc/shared/gcTrace.hpp"
#include "gc/shared/gcWhen.hpp"
#include "jfr/jfrEvents.hpp"
#include "runtime/os.hpp"
#include "utilities/macros.hpp"

// All GC dependencies against the trace framework is contained within this file.

typedef uintptr_t TraceAddress;

void GCTracer::send_garbage_collection_event() const {
  EventGarbageCollection event(UNTIMED);
  if (event.should_commit()) {
    event.set_gcId(GCId::current());
    event.set_name(_shared_gc_info.name());
    event.set_cause((u2) _shared_gc_info.cause());
    event.set_sumOfPauses(_shared_gc_info.sum_of_pauses());
    event.set_longestPause(_shared_gc_info.longest_pause());
    event.set_starttime(_shared_gc_info.start_timestamp());
    event.set_endtime(_shared_gc_info.end_timestamp());
    event.commit();
  }
}

void GCTracer::send_reference_stats_event(ReferenceType type, size_t count) const {
  EventGCReferenceStatistics e;
  if (e.should_commit()) {
      e.set_gcId(GCId::current());
      e.set_type((u1)type);
      e.set_count(count);
      e.commit();
  }
}

void GCTracer::send_metaspace_chunk_free_list_summary(GCWhen::Type when, Metaspace::MetadataType mdtype,
                                                      const MetaspaceChunkFreeListSummary& summary) const {
  EventMetaspaceChunkFreeListSummary e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_when(when);
    e.set_metadataType(mdtype);

    e.set_specializedChunks(summary.num_specialized_chunks());
    e.set_specializedChunksTotalSize(summary.specialized_chunks_size_in_bytes());

    e.set_smallChunks(summary.num_small_chunks());
    e.set_smallChunksTotalSize(summary.small_chunks_size_in_bytes());

    e.set_mediumChunks(summary.num_medium_chunks());
    e.set_mediumChunksTotalSize(summary.medium_chunks_size_in_bytes());

    e.set_humongousChunks(summary.num_humongous_chunks());
    e.set_humongousChunksTotalSize(summary.humongous_chunks_size_in_bytes());

    e.commit();
  }
}

void ParallelOldTracer::send_parallel_old_event() const {
  EventParallelOldGarbageCollection e(UNTIMED);
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_densePrefix((TraceAddress)_parallel_old_gc_info.dense_prefix());
    e.set_starttime(_shared_gc_info.start_timestamp());
    e.set_endtime(_shared_gc_info.end_timestamp());
    e.commit();
  }
}

void YoungGCTracer::send_young_gc_event() const {
  EventYoungGarbageCollection e(UNTIMED);
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_tenuringThreshold(_tenuring_threshold);
    e.set_starttime(_shared_gc_info.start_timestamp());
    e.set_endtime(_shared_gc_info.end_timestamp());
    e.commit();
  }
}

bool YoungGCTracer::should_send_promotion_in_new_plab_event() const {
  return EventPromoteObjectInNewPLAB::is_enabled();
}

bool YoungGCTracer::should_send_promotion_outside_plab_event() const {
  return EventPromoteObjectOutsidePLAB::is_enabled();
}

void YoungGCTracer::send_promotion_in_new_plab_event(Klass* klass, size_t obj_size,
                                                     uint age, bool tenured,
                                                     size_t plab_size) const {

  EventPromoteObjectInNewPLAB event;
  if (event.should_commit()) {
    event.set_gcId(GCId::current());
    event.set_objectClass(klass);
    event.set_objectSize(obj_size);
    event.set_tenured(tenured);
    event.set_tenuringAge(age);
    event.set_plabSize(plab_size);
    event.commit();
  }
}

void YoungGCTracer::send_promotion_outside_plab_event(Klass* klass, size_t obj_size,
                                                      uint age, bool tenured) const {

  EventPromoteObjectOutsidePLAB event;
  if (event.should_commit()) {
    event.set_gcId(GCId::current());
    event.set_objectClass(klass);
    event.set_objectSize(obj_size);
    event.set_tenured(tenured);
    event.set_tenuringAge(age);
    event.commit();
  }
}

void OldGCTracer::send_old_gc_event() const {
  EventOldGarbageCollection e(UNTIMED);
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_starttime(_shared_gc_info.start_timestamp());
    e.set_endtime(_shared_gc_info.end_timestamp());
    e.commit();
  }
}

static JfrStructCopyFailed to_struct(const CopyFailedInfo& cf_info) {
  JfrStructCopyFailed failed_info;
  failed_info.set_objectCount(cf_info.failed_count());
  failed_info.set_firstSize(cf_info.first_size());
  failed_info.set_smallestSize(cf_info.smallest_size());
  failed_info.set_totalSize(cf_info.total_size());
  return failed_info;
}

void YoungGCTracer::send_promotion_failed_event(const PromotionFailedInfo& pf_info) const {
  EventPromotionFailed e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_promotionFailed(to_struct(pf_info));
    e.set_thread(pf_info.thread_trace_id());
    e.commit();
  }
}

// G1
void OldGCTracer::send_concurrent_mode_failure_event() {
  EventConcurrentModeFailure e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.commit();
  }
}

static JfrStructVirtualSpace to_struct(const VirtualSpaceSummary& summary) {
  JfrStructVirtualSpace space;
  space.set_start((TraceAddress)summary.start());
  space.set_committedEnd((TraceAddress)summary.committed_end());
  space.set_committedSize(summary.committed_size());
  space.set_reservedEnd((TraceAddress)summary.reserved_end());
  space.set_reservedSize(summary.reserved_size());
  return space;
}

static JfrStructObjectSpace to_struct(const SpaceSummary& summary) {
  JfrStructObjectSpace space;
  space.set_start((TraceAddress)summary.start());
  space.set_end((TraceAddress)summary.end());
  space.set_used(summary.used());
  space.set_size(summary.size());
  return space;
}

class GCHeapSummaryEventSender : public GCHeapSummaryVisitor {
  GCWhen::Type _when;
 public:
  GCHeapSummaryEventSender(GCWhen::Type when) : _when(when) {}

  void visit(const GCHeapSummary* heap_summary) const {
    const VirtualSpaceSummary& heap_space = heap_summary->heap();

    EventGCHeapSummary e;
    if (e.should_commit()) {
      e.set_gcId(GCId::current());
      e.set_when((u1)_when);
      e.set_heapSpace(to_struct(heap_space));
      e.set_heapUsed(heap_summary->used());
      e.commit();
    }
  }

  void visit(const G1HeapSummary* g1_heap_summary) const {
    visit((GCHeapSummary*)g1_heap_summary);

    EventG1HeapSummary e;
    if (e.should_commit()) {
      e.set_gcId(GCId::current());
      e.set_when((u1)_when);
      e.set_edenUsedSize(g1_heap_summary->edenUsed());
      e.set_edenTotalSize(g1_heap_summary->edenCapacity());
      e.set_survivorUsedSize(g1_heap_summary->survivorUsed());
      e.set_numberOfRegions(g1_heap_summary->numberOfRegions());
      e.commit();
    }
  }

  void visit(const PSHeapSummary* ps_heap_summary) const {
    visit((GCHeapSummary*)ps_heap_summary);

    const VirtualSpaceSummary& old_summary = ps_heap_summary->old();
    const SpaceSummary& old_space = ps_heap_summary->old_space();
    const VirtualSpaceSummary& young_summary = ps_heap_summary->young();
    const SpaceSummary& eden_space = ps_heap_summary->eden();
    const SpaceSummary& from_space = ps_heap_summary->from();
    const SpaceSummary& to_space = ps_heap_summary->to();

    EventPSHeapSummary e;
    if (e.should_commit()) {
      e.set_gcId(GCId::current());
      e.set_when((u1)_when);

      e.set_oldSpace(to_struct(ps_heap_summary->old()));
      e.set_oldObjectSpace(to_struct(ps_heap_summary->old_space()));
      e.set_youngSpace(to_struct(ps_heap_summary->young()));
      e.set_edenSpace(to_struct(ps_heap_summary->eden()));
      e.set_fromSpace(to_struct(ps_heap_summary->from()));
      e.set_toSpace(to_struct(ps_heap_summary->to()));
      e.commit();
    }
  }
};

void GCTracer::send_gc_heap_summary_event(GCWhen::Type when, const GCHeapSummary& heap_summary) const {
  GCHeapSummaryEventSender visitor(when);
  heap_summary.accept(&visitor);
}

static JfrStructMetaspaceSizes to_struct(const MetaspaceStats& sizes) {
  JfrStructMetaspaceSizes meta_sizes;
  meta_sizes.set_committed(sizes.committed());
  meta_sizes.set_used(sizes.used());
  meta_sizes.set_reserved(sizes.reserved());
  return meta_sizes;
}

void GCTracer::send_meta_space_summary_event(GCWhen::Type when, const MetaspaceSummary& meta_space_summary) const {
  EventMetaspaceSummary e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_when((u1) when);
    e.set_gcThreshold(meta_space_summary.capacity_until_GC());
    e.set_metaspace(to_struct(meta_space_summary.stats())); // total stats (class + nonclass)
    e.set_dataSpace(to_struct(meta_space_summary.stats().non_class_space_stats())); // "dataspace" aka non-class space
    e.set_classSpace(to_struct(meta_space_summary.stats().class_space_stats()));
    e.commit();
  }
}

class PhaseSender : public PhaseVisitor {
  void visit_pause(GCPhase* phase) {
    assert(phase->level() < PhasesStack::PHASE_LEVELS, "Need more event types for PausePhase");

    switch (phase->level()) {
      case 0: send_phase<EventGCPhasePause>(phase); break;
      case 1: send_phase<EventGCPhasePauseLevel1>(phase); break;
      case 2: send_phase<EventGCPhasePauseLevel2>(phase); break;
      case 3: send_phase<EventGCPhasePauseLevel3>(phase); break;
      case 4: send_phase<EventGCPhasePauseLevel4>(phase); break;
      default: /* Ignore sending this phase */ break;
    }
  }

  void visit_concurrent(GCPhase* phase) {
    assert(phase->level() < 2, "There is only two levels for ConcurrentPhase");

    switch (phase->level()) {
      case 0: send_phase<EventGCPhaseConcurrent>(phase); break;
      case 1: send_phase<EventGCPhaseConcurrentLevel1>(phase); break;
      default: /* Ignore sending this phase */ break;
    }
  }

 public:
  template<typename T>
  void send_phase(GCPhase* phase) {
    T event(UNTIMED);
    if (event.should_commit()) {
      event.set_gcId(GCId::current());
      event.set_name(phase->name());
      event.set_starttime(phase->start());
      event.set_endtime(phase->end());
      event.commit();
    }
  }

  void visit(GCPhase* phase) {
    if (phase->type() == GCPhase::PausePhaseType) {
      visit_pause(phase);
    } else {
      assert(phase->type() == GCPhase::ConcurrentPhaseType, "Should be ConcurrentPhaseType");
      visit_concurrent(phase);
    }
  }
};

void GCTracer::send_phase_events(TimePartitions* time_partitions) const {
  PhaseSender phase_reporter;

  TimePartitionPhasesIterator iter(time_partitions);
  while (iter.has_next()) {
    GCPhase* phase = iter.next();
    phase->accept(&phase_reporter);
  }
}

#if INCLUDE_JFR
Ticks GCLockerTracer::_needs_gc_start_timestamp;
volatile jint GCLockerTracer::_jni_lock_count = 0;
volatile jint GCLockerTracer::_stall_count = 0;

bool GCLockerTracer::is_started() {
  return _needs_gc_start_timestamp != Ticks();
}

void GCLockerTracer::start_gc_locker(const jint jni_lock_count) {
  assert(SafepointSynchronize::is_at_safepoint(), "sanity");
  assert(!is_started(), "sanity");
  assert(_jni_lock_count == 0, "sanity");
  assert(_stall_count == 0, "sanity");
  if (EventGCLocker::is_enabled()) {
    _needs_gc_start_timestamp.stamp();
    _jni_lock_count = jni_lock_count;
  }
}

void GCLockerTracer::inc_stall_count() {
  if (is_started()) {
    _stall_count++;
  }
}

void GCLockerTracer::report_gc_locker() {
  if (is_started()) {
    EventGCLocker event(UNTIMED);
    if (event.should_commit()) {
      event.set_starttime(_needs_gc_start_timestamp);
      event.set_lockCount(_jni_lock_count);
      event.set_stallCount(_stall_count);
      event.commit();
    }
    // reset
    _needs_gc_start_timestamp = Ticks();
    _jni_lock_count = 0;
    _stall_count = 0;

    assert(!is_started(), "sanity");
  }
}
#endif
