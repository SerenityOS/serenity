/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1EvacuationInfo.hpp"
#include "gc/g1/g1HeapRegionTraceType.hpp"
#include "gc/g1/g1Trace.hpp"
#include "gc/g1/g1GCPauseType.hpp"
#include "gc/shared/gcHeapSummary.hpp"
#include "jfr/jfrEvents.hpp"
#if INCLUDE_JFR
#include "jfr/metadata/jfrSerializer.hpp"
#endif

#if INCLUDE_JFR
class G1HeapRegionTypeConstant : public JfrSerializer {
public:
  void serialize(JfrCheckpointWriter& writer) {
    static const u4 nof_entries = G1HeapRegionTraceType::G1HeapRegionTypeEndSentinel;
    writer.write_count(nof_entries);
    for (u4 i = 0; i < nof_entries; ++i) {
      writer.write_key(i);
      writer.write(G1HeapRegionTraceType::to_string((G1HeapRegionTraceType::Type)i));
    }
  }
};

class G1YCTypeConstant : public JfrSerializer {
public:
  void serialize(JfrCheckpointWriter& writer) {
    constexpr EnumRange<G1GCPauseType> types{};
    static const u4 nof_entries = static_cast<u4>(types.size());
    writer.write_count(nof_entries);
    for (auto index : types) {
      writer.write_key(static_cast<uint>(index));
      writer.write(G1GCPauseTypeHelper::to_string(index));
    }
  }
};

static void register_jfr_type_constants() {
  JfrSerializer::register_serializer(TYPE_G1HEAPREGIONTYPE, true,
                                     new G1HeapRegionTypeConstant());

  JfrSerializer::register_serializer(TYPE_G1YCTYPE, true,
                                     new G1YCTypeConstant());
}

#endif

void G1NewTracer::initialize() {
  JFR_ONLY(register_jfr_type_constants());
}

void G1NewTracer::report_young_gc_pause(G1GCPauseType pause) {
  G1GCPauseTypeHelper::assert_is_young_pause(pause);
  _pause = pause;
}

void G1NewTracer::report_gc_end_impl(const Ticks& timestamp, TimePartitions* time_partitions) {
  YoungGCTracer::report_gc_end_impl(timestamp, time_partitions);
  send_g1_young_gc_event();
}

void G1NewTracer::report_evacuation_info(G1EvacuationInfo* info) {
  send_evacuation_info_event(info);
}

void G1NewTracer::report_evacuation_failed(EvacuationFailedInfo& ef_info) {
  send_evacuation_failed_event(ef_info);
  ef_info.reset();
}

void G1NewTracer::report_evacuation_statistics(const G1EvacSummary& young_summary, const G1EvacSummary& old_summary) const {
  send_young_evacuation_statistics(young_summary);
  send_old_evacuation_statistics(old_summary);
}

void G1NewTracer::report_basic_ihop_statistics(size_t threshold,
                                               size_t target_ccupancy,
                                               size_t current_occupancy,
                                               size_t last_allocation_size,
                                               double last_allocation_duration,
                                               double last_marking_length) {
  send_basic_ihop_statistics(threshold,
                             target_ccupancy,
                             current_occupancy,
                             last_allocation_size,
                             last_allocation_duration,
                             last_marking_length);
}

void G1NewTracer::report_adaptive_ihop_statistics(size_t threshold,
                                                  size_t internal_target_occupancy,
                                                  size_t current_occupancy,
                                                  size_t additional_buffer_size,
                                                  double predicted_allocation_rate,
                                                  double predicted_marking_length,
                                                  bool prediction_active) {
  send_adaptive_ihop_statistics(threshold,
                                internal_target_occupancy,
                                current_occupancy,
                                additional_buffer_size,
                                predicted_allocation_rate,
                                predicted_marking_length,
                                prediction_active);
}

void G1NewTracer::send_g1_young_gc_event() {
  // Check that the pause type has been updated to something valid for this event.
  G1GCPauseTypeHelper::assert_is_young_pause(_pause);

  EventG1GarbageCollection e(UNTIMED);
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_type(static_cast<uint>(_pause));
    e.set_starttime(_shared_gc_info.start_timestamp());
    e.set_endtime(_shared_gc_info.end_timestamp());
    e.commit();
  }
}

void G1NewTracer::send_evacuation_info_event(G1EvacuationInfo* info) {
  EventEvacuationInformation e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_cSetRegions(info->collectionset_regions());
    e.set_cSetUsedBefore(info->collectionset_used_before());
    e.set_cSetUsedAfter(info->collectionset_used_after());
    e.set_allocationRegions(info->allocation_regions());
    e.set_allocationRegionsUsedBefore(info->alloc_regions_used_before());
    e.set_allocationRegionsUsedAfter(info->alloc_regions_used_before() + info->bytes_used());
    e.set_bytesCopied(info->bytes_used());
    e.set_regionsFreed(info->regions_freed());
    e.commit();
  }
}

void G1NewTracer::send_evacuation_failed_event(const EvacuationFailedInfo& ef_info) const {
  EventEvacuationFailed e;
  if (e.should_commit()) {
    // Create JFR structured failure data
    JfrStructCopyFailed evac_failed;
    evac_failed.set_objectCount(ef_info.failed_count());
    evac_failed.set_firstSize(ef_info.first_size());
    evac_failed.set_smallestSize(ef_info.smallest_size());
    evac_failed.set_totalSize(ef_info.total_size());
    // Add to the event
    e.set_gcId(GCId::current());
    e.set_evacuationFailed(evac_failed);
    e.commit();
  }
}

static JfrStructG1EvacuationStatistics
create_g1_evacstats(unsigned gcid, const G1EvacSummary& summary) {
  JfrStructG1EvacuationStatistics s;
  s.set_gcId(gcid);
  s.set_allocated(summary.allocated() * HeapWordSize);
  s.set_wasted(summary.wasted() * HeapWordSize);
  s.set_used(summary.used() * HeapWordSize);
  s.set_undoWaste(summary.undo_wasted() * HeapWordSize);
  s.set_regionEndWaste(summary.region_end_waste() * HeapWordSize);
  s.set_regionsRefilled(summary.regions_filled());
  s.set_directAllocated(summary.direct_allocated() * HeapWordSize);
  s.set_failureUsed(summary.failure_used() * HeapWordSize);
  s.set_failureWaste(summary.failure_waste() * HeapWordSize);
  return s;
}

void G1NewTracer::send_young_evacuation_statistics(const G1EvacSummary& summary) const {
  EventG1EvacuationYoungStatistics surv_evt;
  if (surv_evt.should_commit()) {
    surv_evt.set_statistics(create_g1_evacstats(GCId::current(), summary));
    surv_evt.commit();
  }
}

void G1NewTracer::send_old_evacuation_statistics(const G1EvacSummary& summary) const {
  EventG1EvacuationOldStatistics old_evt;
  if (old_evt.should_commit()) {
    old_evt.set_statistics(create_g1_evacstats(GCId::current(), summary));
    old_evt.commit();
  }
}

void G1NewTracer::send_basic_ihop_statistics(size_t threshold,
                                             size_t target_occupancy,
                                             size_t current_occupancy,
                                             size_t last_allocation_size,
                                             double last_allocation_duration,
                                             double last_marking_length) {
  EventG1BasicIHOP evt;
  if (evt.should_commit()) {
    evt.set_gcId(GCId::current());
    evt.set_threshold(threshold);
    evt.set_targetOccupancy(target_occupancy);
    evt.set_thresholdPercentage(target_occupancy > 0 ? ((double)threshold / target_occupancy) : 0.0);
    evt.set_currentOccupancy(current_occupancy);
    evt.set_recentMutatorAllocationSize(last_allocation_size);
    evt.set_recentMutatorDuration(last_allocation_duration * MILLIUNITS);
    evt.set_recentAllocationRate(last_allocation_duration != 0.0 ? last_allocation_size / last_allocation_duration : 0.0);
    evt.set_lastMarkingDuration(last_marking_length * MILLIUNITS);
    evt.commit();
  }
}

void G1NewTracer::send_adaptive_ihop_statistics(size_t threshold,
                                                size_t internal_target_occupancy,
                                                size_t current_occupancy,
                                                size_t additional_buffer_size,
                                                double predicted_allocation_rate,
                                                double predicted_marking_length,
                                                bool prediction_active) {
  EventG1AdaptiveIHOP evt;
  if (evt.should_commit()) {
    evt.set_gcId(GCId::current());
    evt.set_threshold(threshold);
    evt.set_thresholdPercentage(internal_target_occupancy > 0 ? ((double)threshold / internal_target_occupancy) : 0.0);
    evt.set_ihopTargetOccupancy(internal_target_occupancy);
    evt.set_currentOccupancy(current_occupancy);
    evt.set_additionalBufferSize(additional_buffer_size);
    evt.set_predictedAllocationRate(predicted_allocation_rate);
    evt.set_predictedMarkingDuration(predicted_marking_length * MILLIUNITS);
    evt.set_predictionActive(prediction_active);
    evt.commit();
  }
}

void G1OldTracer::report_gc_start_impl(GCCause::Cause cause, const Ticks& timestamp) {
  _shared_gc_info.set_start_timestamp(timestamp);
}

void G1OldTracer::set_gc_cause(GCCause::Cause cause) {
  _shared_gc_info.set_cause(cause);
}

void G1MMUTracer::report_mmu(double time_slice_sec, double gc_time_sec, double max_time_sec) {
  send_g1_mmu_event(time_slice_sec * MILLIUNITS,
                    gc_time_sec * MILLIUNITS,
                    max_time_sec * MILLIUNITS);
}

void G1MMUTracer::send_g1_mmu_event(double time_slice_ms, double gc_time_ms, double max_time_ms) {
  EventG1MMU e;
  if (e.should_commit()) {
    e.set_gcId(GCId::current());
    e.set_timeSlice(time_slice_ms);
    e.set_gcTime(gc_time_ms);
    e.set_pauseTarget(max_time_ms);
    e.commit();
  }
}
