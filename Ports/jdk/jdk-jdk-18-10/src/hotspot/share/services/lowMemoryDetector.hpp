/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_LOWMEMORYDETECTOR_HPP
#define SHARE_SERVICES_LOWMEMORYDETECTOR_HPP

#include "memory/allocation.hpp"
#include "oops/oopHandle.hpp"
#include "runtime/atomic.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryService.hpp"
#include "services/memoryUsage.hpp"

// Low Memory Detection Support
// Two memory alarms in the JDK (we called them sensors).
//   - Heap memory sensor
//   - Non-heap memory sensor
// When the VM detects if the memory usage of a memory pool has reached
// or exceeded its threshold, it will trigger the sensor for the type
// of the memory pool (heap or nonheap or both).
//
// If threshold == -1, no low memory detection is supported and
// the threshold value is not allowed to be changed.
// If threshold == 0, no low memory detection is performed for
// that memory pool.  The threshold can be set to any non-negative
// value.
//
// The default threshold of the Hotspot memory pools are:
//   Eden space        -1
//   Survivor space 1  -1
//   Survivor space 2  -1
//   Old generation    0
//   Perm generation   0
//   CodeCache         0
//
// For heap memory, detection will be performed when GC finishes
// and also in the slow path allocation.
// For Code cache, detection will be performed in the allocation
// and deallocation.
//
// May need to deal with hysteresis effect.
//
// Memory detection code runs in the Notification thread or
// ServiceThread depending on UseNotificationThread flag.

class OopClosure;
class MemoryPool;

class ThresholdSupport : public CHeapObj<mtInternal> {
 private:
  bool            _support_high_threshold;
  bool            _support_low_threshold;
  size_t          _high_threshold;
  size_t          _low_threshold;
 public:
  ThresholdSupport(bool support_high, bool support_low) {
    _support_high_threshold = support_high;
    _support_low_threshold = support_low;
    _high_threshold = 0;
    _low_threshold= 0;
  }

  size_t      high_threshold() const        { return _high_threshold; }
  size_t      low_threshold()  const        { return _low_threshold; }
  bool        is_high_threshold_supported() { return _support_high_threshold; }
  bool        is_low_threshold_supported()  { return _support_low_threshold; }

  bool        is_high_threshold_crossed(MemoryUsage usage) {
    if (_support_high_threshold && _high_threshold > 0) {
      return (usage.used() >= _high_threshold);
    }
    return false;
  }
  bool        is_low_threshold_crossed(MemoryUsage usage) {
    if (_support_low_threshold && _low_threshold > 0) {
      return (usage.used() < _low_threshold);
    }
    return false;
  }

  size_t      set_high_threshold(size_t new_threshold) {
    assert(_support_high_threshold, "can only be set if supported");
    assert(new_threshold >= _low_threshold, "new_threshold must be >= _low_threshold");
    size_t prev = _high_threshold;
    _high_threshold = new_threshold;
    return prev;
  }

  size_t      set_low_threshold(size_t new_threshold) {
    assert(_support_low_threshold, "can only be set if supported");
    assert(new_threshold <= _high_threshold, "new_threshold must be <= _high_threshold");
    size_t prev = _low_threshold;
    _low_threshold = new_threshold;
    return prev;
  }
};

class SensorInfo : public CHeapObj<mtInternal> {
private:
  OopHandle       _sensor_obj;
  bool            _sensor_on;
  size_t          _sensor_count;

  // before the actual sensor on flag and sensor count are set
  // we maintain the number of pending triggers and clears.
  // _pending_trigger_count means the number of pending triggers
  // and the sensor count should be incremented by the same number.

  int             _pending_trigger_count;

  // _pending_clear_count takes precedence if it's > 0 which
  // indicates the resulting sensor will be off
  // Sensor trigger requests will reset this clear count to
  // indicate the resulting flag should be on.

  int             _pending_clear_count;

  MemoryUsage     _usage;

  void clear(int count, TRAPS);
  void trigger(int count, TRAPS);
public:
  SensorInfo();
  void set_sensor(instanceOop sensor);

  bool has_pending_requests() {
    return (_pending_trigger_count > 0 || _pending_clear_count > 0);
  }

  int pending_trigger_count()      { return _pending_trigger_count; }
  int pending_clear_count()        { return _pending_clear_count; }

  // When this method is used, the memory usage is monitored
  // as a gauge attribute.  High and low thresholds are designed
  // to provide a hysteresis mechanism to avoid repeated triggering
  // of notifications when the attribute value makes small oscillations
  // around the high or low threshold value.
  //
  // The sensor will be triggered if:
  //  (1) the usage is crossing above the high threshold and
  //      the sensor is currently off and no pending
  //      trigger requests; or
  //  (2) the usage is crossing above the high threshold and
  //      the sensor will be off (i.e. sensor is currently on
  //      and has pending clear requests).
  //
  // Subsequent crossings of the high threshold value do not cause
  // any triggers unless the usage becomes less than the low threshold.
  //
  // The sensor will be cleared if:
  //  (1) the usage is crossing below the low threshold and
  //      the sensor is currently on and no pending
  //      clear requests; or
  //  (2) the usage is crossing below the low threshold and
  //      the sensor will be on (i.e. sensor is currently off
  //      and has pending trigger requests).
  //
  // Subsequent crossings of the low threshold value do not cause
  // any clears unless the usage becomes greater than or equal
  // to the high threshold.
  //
  // If the current level is between high and low threshold, no change.
  //
  void set_gauge_sensor_level(MemoryUsage usage, ThresholdSupport* high_low_threshold);

  // When this method is used, the memory usage is monitored as a
  // simple counter attribute.  The sensor will be triggered
  // whenever the usage is crossing the threshold to keep track
  // of the number of times the VM detects such a condition occurs.
  //
  // The sensor will be triggered if:
  //   - the usage is crossing above the high threshold regardless
  //     of the current sensor state.
  //
  // The sensor will be cleared if:
  //  (1) the usage is crossing below the low threshold and
  //      the sensor is currently on; or
  //  (2) the usage is crossing below the low threshold and
  //      the sensor will be on (i.e. sensor is currently off
  //      and has pending trigger requests).
  //
  void set_counter_sensor_level(MemoryUsage usage, ThresholdSupport* counter_threshold);

  void process_pending_requests(TRAPS);

#ifndef PRODUCT
  // printing on default output stream;
  void print();
#endif // PRODUCT
};

class LowMemoryDetector : public AllStatic {
  friend class ServiceThread;
  friend class NotificationThread;
private:
  // true if any collected heap has low memory detection enabled
  static volatile bool _enabled_for_collected_pools;

  static bool has_pending_requests();
  static void process_sensor_changes(TRAPS);

public:
  static void detect_low_memory();
  static void detect_low_memory(MemoryPool* pool);
  static void detect_after_gc_memory(MemoryPool* pool);

  static bool is_enabled(MemoryPool* pool) {
    // low memory detection is enabled for collected memory pools
    // iff one of the collected memory pool has a sensor and the
    // threshold set non-zero
    if (pool->usage_sensor() == NULL) {
      return false;
    } else {
      ThresholdSupport* threshold_support = pool->usage_threshold();
      return (threshold_support->is_high_threshold_supported() ?
               (threshold_support->high_threshold() > 0) : false);
    }
  }

  // recompute enabled flag
  static void recompute_enabled_for_collected_pools();

  // low memory detection for collected memory pools.
  static inline void detect_low_memory_for_collected_pools() {
    // no-op if low memory detection not enabled
    if (!_enabled_for_collected_pools) {
      return;
    }
    int num_memory_pools = MemoryService::num_memory_pools();
    for (int i=0; i<num_memory_pools; i++) {
      MemoryPool* pool = MemoryService::get_memory_pool(i);

      // if low memory detection is enabled then check if the
      // current used exceeds the high threshold
      if (pool->is_collected_pool() && is_enabled(pool)) {
        size_t used = pool->used_in_bytes();
        size_t high = pool->usage_threshold()->high_threshold();
        if (used > high) {
          detect_low_memory(pool);
        }
      }
    }
  }
};

#endif // SHARE_SERVICES_LOWMEMORYDETECTOR_HPP
