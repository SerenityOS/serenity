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

#include "precompiled.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/stringTable.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageParState.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/stringdedup/stringDedupProcessor.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "gc/shared/stringdedup/stringDedupStorageUse.hpp"
#include "gc/shared/stringdedup/stringDedupTable.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/access.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalCounter.hpp"
#include "utilities/globalDefinitions.hpp"

StringDedup::Processor::Processor() : ConcurrentGCThread() {
  set_name("StringDedupProcessor");
}

OopStorage* StringDedup::Processor::_storages[2] = {};

StringDedup::StorageUse* volatile StringDedup::Processor::_storage_for_requests = nullptr;
StringDedup::StorageUse* StringDedup::Processor::_storage_for_processing = nullptr;

void StringDedup::Processor::initialize_storage() {
  assert(_storages[0] == nullptr, "storage already created");
  assert(_storages[1] == nullptr, "storage already created");
  assert(_storage_for_requests == nullptr, "storage already created");
  assert(_storage_for_processing == nullptr, "storage already created");
  _storages[0] = OopStorageSet::create_weak("StringDedup Requests0 Weak", mtStringDedup);
  _storages[1] = OopStorageSet::create_weak("StringDedup Requests1 Weak", mtStringDedup);
  _storage_for_requests = new StorageUse(_storages[0]);
  _storage_for_processing = new StorageUse(_storages[1]);
}

void StringDedup::Processor::initialize() {
  _processor = new Processor();
  _processor->create_and_start();
}

bool StringDedup::Processor::wait_for_requests() const {
  // Wait for the current request storage object to be non-empty.  The
  // num-dead notification from the Table notifies the monitor.
  if (!should_terminate()) {
    MonitorLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
    OopStorage* storage = Atomic::load(&_storage_for_requests)->storage();
    while (!should_terminate() &&
           (storage->allocation_count() == 0) &&
           !Table::is_dead_entry_removal_needed()) {
      ml.wait();
    }
  }
  // Swap the request and processing storage objects.
  if (!should_terminate()) {
    log_trace(stringdedup)("swapping request storages");
    _storage_for_processing = Atomic::xchg(&_storage_for_requests, _storage_for_processing);
    GlobalCounter::write_synchronize();
  }
  // Wait for the now current processing storage object to no longer be used
  // by an in-progress GC.  Again here, the num-dead notification from the
  // Table notifies the monitor.
  if (!should_terminate()) {
    log_trace(stringdedup)("waiting for storage to process");
    MonitorLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
    while (_storage_for_processing->is_used_acquire() && !should_terminate()) {
      ml.wait();
    }
  }
  return !should_terminate();
}

StringDedup::StorageUse* StringDedup::Processor::storage_for_requests() {
  return StorageUse::obtain(&_storage_for_requests);
}

bool StringDedup::Processor::yield_or_continue(SuspendibleThreadSetJoiner* joiner,
                                               Stat::Phase phase) const {
  if (joiner->should_yield()) {
    _cur_stat.block_phase(phase);
    joiner->yield();
    _cur_stat.unblock_phase();
  }
  return !should_terminate();
}

void StringDedup::Processor::cleanup_table(SuspendibleThreadSetJoiner* joiner,
                                           bool grow_only,
                                           bool force) const {
  if (Table::cleanup_start_if_needed(grow_only, force)) {
    Stat::Phase phase = Table::cleanup_phase();
    while (yield_or_continue(joiner, phase)) {
      if (!Table::cleanup_step()) break;
    }
    Table::cleanup_end();
  }
}

class StringDedup::Processor::ProcessRequest final : public OopClosure {
  OopStorage* _storage;
  SuspendibleThreadSetJoiner* _joiner;
  size_t _release_index;
  oop* _bulk_release[OopStorage::bulk_allocate_limit];

  void release_ref(oop* ref) {
    assert(_release_index < ARRAY_SIZE(_bulk_release), "invariant");
    NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(ref, nullptr);
    _bulk_release[_release_index++] = ref;
    if (_release_index == ARRAY_SIZE(_bulk_release)) {
      _storage->release(_bulk_release, _release_index);
      _release_index = 0;
    }
  }

public:
  ProcessRequest(OopStorage* storage, SuspendibleThreadSetJoiner* joiner) :
    _storage(storage),
    _joiner(joiner),
    _release_index(0),
    _bulk_release()
  {}

  ~ProcessRequest() {
    _storage->release(_bulk_release, _release_index);
  }

  virtual void do_oop(narrowOop*) { ShouldNotReachHere(); }

  virtual void do_oop(oop* ref) {
    if (_processor->yield_or_continue(_joiner, Stat::Phase::process)) {
      oop java_string = NativeAccess<ON_PHANTOM_OOP_REF>::oop_load(ref);
      release_ref(ref);
      // Dedup java_string, after checking for various reasons to skip it.
      if (java_string == nullptr) {
        // String became unreachable before we got a chance to process it.
        _cur_stat.inc_skipped_dead();
      } else if (java_lang_String::value(java_string) == nullptr) {
        // Request during String construction, before its value array has
        // been initialized.
        _cur_stat.inc_skipped_incomplete();
      } else {
        Table::deduplicate(java_string);
        if (Table::is_grow_needed()) {
          _cur_stat.report_process_pause();
          _processor->cleanup_table(_joiner, true /* grow_only */, false /* force */);
          _cur_stat.report_process_resume();
        }
      }
    }
  }
};

void StringDedup::Processor::process_requests(SuspendibleThreadSetJoiner* joiner) const {
  OopStorage::ParState<true, false> par_state{_storage_for_processing->storage(), 1};
  ProcessRequest processor{_storage_for_processing->storage(), joiner};
  par_state.oops_do(&processor);
}

void StringDedup::Processor::run_service() {
  while (!should_terminate()) {
    _cur_stat.report_idle_start();
    if (!wait_for_requests()) {
      assert(should_terminate(), "invariant");
      break;
    }
    SuspendibleThreadSetJoiner sts_joiner{};
    if (should_terminate()) break;
    _cur_stat.report_idle_end();
    _cur_stat.report_concurrent_start();
    _cur_stat.report_process_start();
    process_requests(&sts_joiner);
    if (should_terminate()) break;
    _cur_stat.report_process_end();
    cleanup_table(&sts_joiner,
                  false /* grow_only */,
                  StringDeduplicationResizeALot /* force */);
    if (should_terminate()) break;
    _cur_stat.report_concurrent_end();
    log_statistics();
  }
}

void StringDedup::Processor::stop_service() {
  MonitorLocker ml(StringDedup_lock, Mutex::_no_safepoint_check_flag);
  ml.notify_all();
}

void StringDedup::Processor::log_statistics() {
  _total_stat.add(&_cur_stat);
  Stat::log_summary(&_cur_stat, &_total_stat);
  if (log_is_enabled(Debug, stringdedup)) {
    _cur_stat.log_statistics(false);
    _total_stat.log_statistics(true);
    Table::log_statistics();
  }
  _cur_stat = Stat{};
}
