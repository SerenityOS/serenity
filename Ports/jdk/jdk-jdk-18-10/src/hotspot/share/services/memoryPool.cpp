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
#include "classfile/javaClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "services/lowMemoryDetector.hpp"
#include "services/management.hpp"
#include "services/memoryManager.hpp"
#include "services/memoryPool.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

MemoryPool::MemoryPool(const char* name,
                       PoolType type,
                       size_t init_size,
                       size_t max_size,
                       bool support_usage_threshold,
                       bool support_gc_threshold) :
  _name(name),
  _type(type),
  _initial_size(init_size),
  _max_size(max_size),
  _available_for_allocation(true),
  _managers(),
  _num_managers(0),
  _peak_usage(),
  _after_gc_usage(init_size, 0, 0, max_size),
  // usage threshold supports both high and low threshold
  _usage_threshold(new ThresholdSupport(support_usage_threshold, support_usage_threshold)),
  // gc usage threshold supports only high threshold
  _gc_usage_threshold(new ThresholdSupport(support_gc_threshold, support_gc_threshold)),
  _usage_sensor(),
  _gc_usage_sensor(),
  _memory_pool_obj()
{}

bool MemoryPool::is_pool(instanceHandle pool) const {
  return pool() == Atomic::load(&_memory_pool_obj).resolve();
}

void MemoryPool::add_manager(MemoryManager* mgr) {
  assert(_num_managers < MemoryPool::max_num_managers, "_num_managers exceeds the max");
  if (_num_managers < MemoryPool::max_num_managers) {
    _managers[_num_managers] = mgr;
    _num_managers++;
  }
}


// Returns an instanceOop of a MemoryPool object.
// It creates a MemoryPool instance when the first time
// this function is called.
instanceOop MemoryPool::get_memory_pool_instance(TRAPS) {
  // Must do an acquire so as to force ordering of subsequent
  // loads from anything _memory_pool_obj points to or implies.
  oop pool_obj = Atomic::load_acquire(&_memory_pool_obj).resolve();
  if (pool_obj == NULL) {
    // It's ok for more than one thread to execute the code up to the locked region.
    // Extra pool instances will just be gc'ed.
    InstanceKlass* ik = Management::sun_management_ManagementFactoryHelper_klass(CHECK_NULL);

    Handle pool_name = java_lang_String::create_from_str(_name, CHECK_NULL);
    jlong usage_threshold_value = (_usage_threshold->is_high_threshold_supported() ? 0 : -1L);
    jlong gc_usage_threshold_value = (_gc_usage_threshold->is_high_threshold_supported() ? 0 : -1L);

    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_oop(pool_name);           // Argument 1
    args.push_int((int) is_heap());     // Argument 2

    Symbol* method_name = vmSymbols::createMemoryPool_name();
    Symbol* signature = vmSymbols::createMemoryPool_signature();

    args.push_long(usage_threshold_value);    // Argument 3
    args.push_long(gc_usage_threshold_value); // Argument 4

    JavaCalls::call_static(&result,
                           ik,
                           method_name,
                           signature,
                           &args,
                           CHECK_NULL);

    instanceOop p = (instanceOop) result.get_oop();
    instanceHandle pool(THREAD, p);

    {
      // Get lock since another thread may have create the instance
      MutexLocker ml(THREAD, Management_lock);

      // Check if another thread has created the pool.  We reload
      // _memory_pool_obj here because some other thread may have
      // initialized it while we were executing the code before the lock.
      pool_obj = Atomic::load(&_memory_pool_obj).resolve();
      if (pool_obj != NULL) {
         return (instanceOop)pool_obj;
      }

      // Get the address of the object we created via call_special.
      pool_obj = pool();

      // Use store barrier to make sure the memory accesses associated
      // with creating the pool are visible before publishing its address.
      // The unlock will publish the store to _memory_pool_obj because
      // it does a release first.
      Atomic::release_store(&_memory_pool_obj, OopHandle(Universe::vm_global(), pool_obj));
    }
  }

  return (instanceOop)pool_obj;
}

inline static size_t get_max_value(size_t val1, size_t val2) {
    return (val1 > val2 ? val1 : val2);
}

void MemoryPool::record_peak_memory_usage() {
  // Caller in JDK is responsible for synchronization -
  // acquire the lock for this memory pool before calling VM
  MemoryUsage usage = get_memory_usage();
  size_t peak_used = get_max_value(usage.used(), _peak_usage.used());
  size_t peak_committed = get_max_value(usage.committed(), _peak_usage.committed());
  size_t peak_max_size = get_max_value(usage.max_size(), _peak_usage.max_size());

  _peak_usage = MemoryUsage(initial_size(), peak_used, peak_committed, peak_max_size);
}

static void set_sensor_obj_at(SensorInfo** sensor_ptr, instanceHandle sh) {
  assert(*sensor_ptr == NULL, "Should be called only once");
  SensorInfo* sensor = new SensorInfo();
  sensor->set_sensor(sh());
  *sensor_ptr = sensor;
}

void MemoryPool::set_usage_sensor_obj(instanceHandle sh) {
  set_sensor_obj_at(&_usage_sensor, sh);
}

void MemoryPool::set_gc_usage_sensor_obj(instanceHandle sh) {
  set_sensor_obj_at(&_gc_usage_sensor, sh);
}

CodeHeapPool::CodeHeapPool(CodeHeap* codeHeap, const char* name, bool support_usage_threshold) :
  MemoryPool(name, NonHeap, codeHeap->capacity(), codeHeap->max_capacity(),
             support_usage_threshold, false), _codeHeap(codeHeap) {
}

MemoryUsage CodeHeapPool::get_memory_usage() {
  size_t used      = used_in_bytes();
  size_t committed = _codeHeap->capacity();
  size_t maxSize   = (available_for_allocation() ? max_size() : 0);

  return MemoryUsage(initial_size(), used, committed, maxSize);
}

MetaspacePool::MetaspacePool() :
  MemoryPool("Metaspace", NonHeap, 0, calculate_max_size(), true, false) { }

MemoryUsage MetaspacePool::get_memory_usage() {
  MetaspaceCombinedStats stats = MetaspaceUtils::get_combined_statistics();
  return MemoryUsage(initial_size(), stats.used(), stats.committed(), max_size());
}

size_t MetaspacePool::used_in_bytes() {
  return MetaspaceUtils::used_bytes();
}

size_t MetaspacePool::calculate_max_size() const {
  return !FLAG_IS_DEFAULT(MaxMetaspaceSize) ? MaxMetaspaceSize :
                                              MemoryUsage::undefined_size();
}

CompressedKlassSpacePool::CompressedKlassSpacePool() :
  MemoryPool("Compressed Class Space", NonHeap, 0, CompressedClassSpaceSize, true, false) { }

size_t CompressedKlassSpacePool::used_in_bytes() {
  return MetaspaceUtils::used_bytes(Metaspace::ClassType);
}

MemoryUsage CompressedKlassSpacePool::get_memory_usage() {
  MetaspaceStats stats = MetaspaceUtils::get_statistics(Metaspace::ClassType);
  return MemoryUsage(initial_size(), stats.used(), stats.committed(), max_size());
}
