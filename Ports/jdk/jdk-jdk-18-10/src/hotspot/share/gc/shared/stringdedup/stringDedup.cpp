/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/gcLogPrecious.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/stringdedup/stringDedupConfig.hpp"
#include "gc/shared/stringdedup/stringDedupProcessor.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "gc/shared/stringdedup/stringDedupStorageUse.hpp"
#include "gc/shared/stringdedup/stringDedupTable.hpp"
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/access.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/markWord.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/globals.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

bool StringDedup::_initialized = false;
bool StringDedup::_enabled = false;

StringDedup::Processor* StringDedup::_processor = nullptr;
StringDedup::Stat StringDedup::_cur_stat{};
StringDedup::Stat StringDedup::_total_stat{};

const Klass* StringDedup::_string_klass_or_null = nullptr;
uint StringDedup::_enabled_age_threshold = 0;
uint StringDedup::_enabled_age_limit = 0;

bool StringDedup::ergo_initialize() {
  return Config::ergo_initialize();
}

void StringDedup::initialize() {
  assert(!_initialized, "already initialized");
  // Unconditionally create the oopstorage objects, to simplify usage
  // elsewhere. OopStorageSet and clients don't support optional oopstorage
  // objects.
  Table::initialize_storage();
  Processor::initialize_storage();
  if (UseStringDeduplication) {
    Config::initialize();
    // Verify klass comparison with _string_klass_or_null is sufficient
    // to determine whether dedup is enabled and the object is a String.
    assert(vmClasses::String_klass()->is_final(), "precondition");
    _string_klass_or_null = vmClasses::String_klass();
    _enabled_age_threshold = Config::age_threshold();
    _enabled_age_limit = Config::age_threshold();
    Table::initialize();
    Processor::initialize();
    _enabled = true;
    log_info_p(stringdedup, init)("String Deduplication is enabled");
  } else {
    // No klass will ever match.
    _string_klass_or_null = nullptr;
    // Age can never equal UINT_MAX.
    static_assert(markWord::max_age < UINT_MAX, "assumption");
    _enabled_age_threshold = UINT_MAX;
    // Age can never be less than zero.
    _enabled_age_limit = 0;
  }
  _initialized = true;
}

void StringDedup::stop() {
  assert(is_enabled(), "precondition");
  assert(_processor != nullptr, "invariant");
  _processor->stop();
}

void StringDedup::threads_do(ThreadClosure* tc) {
  assert(is_enabled(), "precondition");
  assert(_processor != nullptr, "invariant");
  tc->do_thread(_processor);
}

void StringDedup::forbid_deduplication(oop java_string) {
  assert(is_enabled(), "precondition");
  if (java_lang_String::deduplication_forbidden(java_string)) {
    // DCLP - we don't want a caller's access to the value array to float
    // before the check; string dedup could change the value and another
    // thread could set the flag, and this thread uses a stale value.
    OrderAccess::acquire();
  } else {
    MutexLocker ml(StringDedupIntern_lock, Mutex::_no_safepoint_check_flag);
    java_lang_String::set_deduplication_forbidden(java_string);
  }
}

void StringDedup::notify_intern(oop java_string) {
  assert(is_enabled(), "precondition");
  // A String that is interned in the StringTable must not later have its
  // underlying byte array changed, so mark it as not deduplicatable.  But we
  // can still add the byte array to the dedup table for sharing, so add the
  // string to the pending requests.  Triggering request processing is left
  // to the next GC.
  forbid_deduplication(java_string);
  StorageUse* requests = Processor::storage_for_requests();
  oop* ref = requests->storage()->allocate();
  if (ref != nullptr) {
    NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(ref, java_string);
    log_trace(stringdedup)("StringDedup::deduplicate");
  }
  requests->relinquish();
}

StringDedup::Requests::Requests() :
  _storage_for_requests(nullptr), _buffer(nullptr), _index(0), _refill_failed(false)
{}

StringDedup::Requests::~Requests() {
  flush();
}

bool StringDedup::Requests::refill_buffer() {
  assert(_index == 0, "precondition");
  // Treat out of memory failure as sticky; don't keep retrying.
  if (_refill_failed) return false;
  // Lazy initialization of the requests object.  It can be common for
  // many of the marking threads to not encounter any candidates.
  const size_t buffer_size = OopStorage::bulk_allocate_limit;
  if (_buffer == nullptr) {
    // Lazily allocate a buffer to hold pre-allocated storage entries.
    _buffer = NEW_C_HEAP_ARRAY_RETURN_NULL(oop*, buffer_size, mtStringDedup);
    if (_buffer == nullptr) {
      log_debug(stringdedup)("request failed to allocate buffer");
      _refill_failed = true;
      return false;
    }
    // Lazily obtain the storage object to use for requests.
    assert(_storage_for_requests == nullptr, "invariant");
    _storage_for_requests = Processor::storage_for_requests();
  }
  assert(_storage_for_requests != nullptr, "invariant");
  // Bulk pre-allocate some storage entries to satisfy this and future
  // requests.  This amortizes the cost of allocating entries over
  // multiple requests, and reduces contention on the storage object.
  _index = _storage_for_requests->storage()->allocate(_buffer, buffer_size);
  if (_index == 0) {
    log_debug(stringdedup)("request failed to allocate oopstorage entries");
    flush();
    _refill_failed = true;
    return false;
  }
  return true;
}

void StringDedup::Requests::add(oop java_string) {
  assert(is_enabled(), "StringDedup not enabled");
  if ((_index == 0) && !refill_buffer()) return;
  // Store the string in the next pre-allocated storage entry.
  oop* ref = _buffer[--_index];
  NativeAccess<ON_PHANTOM_OOP_REF>::oop_store(ref, java_string);
  log_trace(stringdedup)("request");
}

void StringDedup::Requests::flush() {
  if (_buffer != nullptr) {
    if (_index > 0) {
      assert(_storage_for_requests != nullptr, "invariant");
      _storage_for_requests->storage()->release(_buffer, _index);
    }
    FREE_C_HEAP_ARRAY(oop*, _buffer);
    _buffer = nullptr;
  }
  if (_storage_for_requests != nullptr) {
    _storage_for_requests->relinquish();
    _storage_for_requests = nullptr;
  }
  _index = 0;
  _refill_failed = false;
}

void StringDedup::verify() {
  assert_at_safepoint();
  if (is_enabled()) {
    Table::verify();
  }
}
