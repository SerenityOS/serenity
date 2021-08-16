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

#include "precompiled.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "services/lowMemoryDetector.hpp"
#include "services/management.hpp"

volatile bool LowMemoryDetector::_enabled_for_collected_pools = false;

bool LowMemoryDetector::has_pending_requests() {
  assert(Notification_lock->owned_by_self(), "Must own Notification_lock");
  bool has_requests = false;
  int num_memory_pools = MemoryService::num_memory_pools();
  for (int i = 0; i < num_memory_pools; i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    SensorInfo* sensor = pool->usage_sensor();
    if (sensor != NULL) {
      has_requests = has_requests || sensor->has_pending_requests();
    }

    SensorInfo* gc_sensor = pool->gc_usage_sensor();
    if (gc_sensor != NULL) {
      has_requests = has_requests || gc_sensor->has_pending_requests();
    }
  }
  return has_requests;
}

void LowMemoryDetector::process_sensor_changes(TRAPS) {
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);

  // No need to hold Notification_lock to call out to Java
  int num_memory_pools = MemoryService::num_memory_pools();
  for (int i = 0; i < num_memory_pools; i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    SensorInfo* sensor = pool->usage_sensor();
    SensorInfo* gc_sensor = pool->gc_usage_sensor();
    if (sensor != NULL && sensor->has_pending_requests()) {
      sensor->process_pending_requests(CHECK);
    }
    if (gc_sensor != NULL && gc_sensor->has_pending_requests()) {
      gc_sensor->process_pending_requests(CHECK);
    }
  }
}

// This method could be called from any Java threads
// and also VMThread.
void LowMemoryDetector::detect_low_memory() {
  MutexLocker ml(Notification_lock, Mutex::_no_safepoint_check_flag);

  bool has_pending_requests = false;
  int num_memory_pools = MemoryService::num_memory_pools();
  for (int i = 0; i < num_memory_pools; i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    SensorInfo* sensor = pool->usage_sensor();
    if (sensor != NULL &&
        pool->usage_threshold()->is_high_threshold_supported() &&
        pool->usage_threshold()->high_threshold() != 0) {
      MemoryUsage usage = pool->get_memory_usage();
      sensor->set_gauge_sensor_level(usage,
                                     pool->usage_threshold());
      has_pending_requests = has_pending_requests || sensor->has_pending_requests();
    }
  }

  if (has_pending_requests) {
    Notification_lock->notify_all();
  }
}

// This method could be called from any Java threads
// and also VMThread.
void LowMemoryDetector::detect_low_memory(MemoryPool* pool) {
  SensorInfo* sensor = pool->usage_sensor();
  if (sensor == NULL ||
      !pool->usage_threshold()->is_high_threshold_supported() ||
      pool->usage_threshold()->high_threshold() == 0) {
    return;
  }

  {
    MutexLocker ml(Notification_lock, Mutex::_no_safepoint_check_flag);

    MemoryUsage usage = pool->get_memory_usage();
    sensor->set_gauge_sensor_level(usage,
                                   pool->usage_threshold());
    if (sensor->has_pending_requests()) {
      // notify sensor state update
      Notification_lock->notify_all();
    }
  }
}

// Only called by VMThread at GC time
void LowMemoryDetector::detect_after_gc_memory(MemoryPool* pool) {
  SensorInfo* sensor = pool->gc_usage_sensor();
  if (sensor == NULL ||
      !pool->gc_usage_threshold()->is_high_threshold_supported() ||
      pool->gc_usage_threshold()->high_threshold() == 0) {
    return;
  }

  {
    MutexLocker ml(Notification_lock, Mutex::_no_safepoint_check_flag);

    MemoryUsage usage = pool->get_last_collection_usage();
    sensor->set_counter_sensor_level(usage, pool->gc_usage_threshold());

    if (sensor->has_pending_requests()) {
      // notify sensor state update
      Notification_lock->notify_all();
    }
  }
}

// recompute enabled flag
void LowMemoryDetector::recompute_enabled_for_collected_pools() {
  bool enabled = false;
  int num_memory_pools = MemoryService::num_memory_pools();
  for (int i=0; i<num_memory_pools; i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    if (pool->is_collected_pool() && is_enabled(pool)) {
      enabled = true;
      break;
    }
  }
  _enabled_for_collected_pools = enabled;
}

SensorInfo::SensorInfo() {
  _sensor_on = false;
  _sensor_count = 0;
  _pending_trigger_count = 0;
  _pending_clear_count = 0;
}

void SensorInfo::set_sensor(instanceOop sensor) {
  assert(_sensor_obj.peek() == NULL, "Should be set only once");
  _sensor_obj = OopHandle(Universe::vm_global(), sensor);
}


// When this method is used, the memory usage is monitored
// as a gauge attribute.  Sensor notifications (trigger or
// clear) is only emitted at the first time it crosses
// a threshold.
//
// High and low thresholds are designed to provide a
// hysteresis mechanism to avoid repeated triggering
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
void SensorInfo::set_gauge_sensor_level(MemoryUsage usage, ThresholdSupport* high_low_threshold) {
  assert(Notification_lock->owned_by_self(), "Must own Notification_lock");
  assert(high_low_threshold->is_high_threshold_supported(), "just checking");

  bool is_over_high = high_low_threshold->is_high_threshold_crossed(usage);
  bool is_below_low = high_low_threshold->is_low_threshold_crossed(usage);

  assert(!(is_over_high && is_below_low), "Can't be both true");

  if (is_over_high &&
        ((!_sensor_on && _pending_trigger_count == 0) ||
         _pending_clear_count > 0)) {
    // low memory detected and need to increment the trigger pending count
    // if the sensor is off or will be off due to _pending_clear_ > 0
    // Request to trigger the sensor
    _pending_trigger_count++;
    _usage = usage;

    if (_pending_clear_count > 0) {
      // non-zero pending clear requests indicates that there are
      // pending requests to clear this sensor.
      // This trigger request needs to clear this clear count
      // since the resulting sensor flag should be on.
      _pending_clear_count = 0;
    }
  } else if (is_below_low &&
               ((_sensor_on && _pending_clear_count == 0) ||
                (_pending_trigger_count > 0 && _pending_clear_count == 0))) {
    // memory usage returns below the threshold
    // Request to clear the sensor if the sensor is on or will be on due to
    // _pending_trigger_count > 0 and also no clear request
    _pending_clear_count++;
  }
}

// When this method is used, the memory usage is monitored as a
// simple counter attribute.  The sensor will be triggered
// whenever the usage is crossing the threshold to keep track
// of the number of times the VM detects such a condition occurs.
//
// High and low thresholds are designed to provide a
// hysteresis mechanism to avoid repeated triggering
// of notifications when the attribute value makes small oscillations
// around the high or low threshold value.
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
void SensorInfo::set_counter_sensor_level(MemoryUsage usage, ThresholdSupport* counter_threshold) {
  assert(Notification_lock->owned_by_self(), "Must own Notification_lock");
  assert(counter_threshold->is_high_threshold_supported(), "just checking");

  bool is_over_high = counter_threshold->is_high_threshold_crossed(usage);
  bool is_below_low = counter_threshold->is_low_threshold_crossed(usage);

  assert(!(is_over_high && is_below_low), "Can't be both true");

  if (is_over_high) {
    _pending_trigger_count++;
    _usage = usage;
    _pending_clear_count = 0;
  } else if (is_below_low && (_sensor_on || _pending_trigger_count > 0)) {
    _pending_clear_count++;
  }
}

void SensorInfo::process_pending_requests(TRAPS) {
  int pending_count = pending_trigger_count();
  if (pending_clear_count() > 0) {
    clear(pending_count, CHECK);
  } else {
    trigger(pending_count, CHECK);
  }

}

void SensorInfo::trigger(int count, TRAPS) {
  assert(count <= _pending_trigger_count, "just checking");
  Handle sensor_h(THREAD, _sensor_obj.resolve());
  if (sensor_h() != NULL) {
    InstanceKlass* sensorKlass = Management::sun_management_Sensor_klass(CHECK);
    Symbol* trigger_method_signature;

    JavaValue result(T_VOID);
    JavaCallArguments args(sensor_h);
    args.push_int((int) count);

    Handle usage_h = MemoryService::create_MemoryUsage_obj(_usage, THREAD);
    // Call Sensor::trigger(int, MemoryUsage) to send notification to listeners.
    // When OOME occurs and fails to allocate MemoryUsage object, call
    // Sensor::trigger(int) instead.  The pending request will be processed
    // but no notification will be sent.
    if (HAS_PENDING_EXCEPTION) {
       assert((PENDING_EXCEPTION->is_a(vmClasses::OutOfMemoryError_klass())), "we expect only an OOME here");
       CLEAR_PENDING_EXCEPTION;
       trigger_method_signature = vmSymbols::int_void_signature();
    } else {
       trigger_method_signature = vmSymbols::trigger_method_signature();
       args.push_oop(usage_h);
    }

    JavaCalls::call_virtual(&result,
                        sensorKlass,
                        vmSymbols::trigger_name(),
                        trigger_method_signature,
                        &args,
                        THREAD);

    if (HAS_PENDING_EXCEPTION) {
       // We just clear the OOM pending exception that we might have encountered
       // in Java's tiggerAction(), and continue with updating the counters since
       // the Java counters have been updated too.
       assert((PENDING_EXCEPTION->is_a(vmClasses::OutOfMemoryError_klass())), "we expect only an OOME here");
       CLEAR_PENDING_EXCEPTION;
     }
  }

  {
    // Holds Notification_lock and update the sensor state
    MutexLocker ml(THREAD, Notification_lock, Mutex::_no_safepoint_check_flag);
    assert(_pending_trigger_count > 0, "Must have pending trigger");
    _sensor_on = true;
    _sensor_count += count;
    _pending_trigger_count = _pending_trigger_count - count;
  }
}

void SensorInfo::clear(int count, TRAPS) {
  {
    // Holds Notification_lock and update the sensor state
    MutexLocker ml(THREAD, Notification_lock, Mutex::_no_safepoint_check_flag);
    if (_pending_clear_count == 0) {
      // Bail out if we lost a race to set_*_sensor_level() which may have
      // reactivated the sensor in the meantime because it was triggered again.
      return;
    }
    _sensor_on = false;
    _sensor_count += count;
    _pending_clear_count = 0;
    _pending_trigger_count = _pending_trigger_count - count;
  }

  Handle sensor(THREAD, _sensor_obj.resolve());
  if (sensor() != NULL) {
    InstanceKlass* sensorKlass = Management::sun_management_Sensor_klass(CHECK);
    JavaValue result(T_VOID);
    JavaCallArguments args(sensor);
    args.push_int((int) count);
    JavaCalls::call_virtual(&result,
                            sensorKlass,
                            vmSymbols::clear_name(),
                            vmSymbols::int_void_signature(),
                            &args,
                            CHECK);
  }
}

//--------------------------------------------------------------
// Non-product code

#ifndef PRODUCT
void SensorInfo::print() {
  tty->print_cr("%s count = " SIZE_FORMAT " pending_triggers = %d pending_clears = %d",
                (_sensor_on ? "on" : "off"),
                _sensor_count, _pending_trigger_count, _pending_clear_count);
}

#endif // PRODUCT
