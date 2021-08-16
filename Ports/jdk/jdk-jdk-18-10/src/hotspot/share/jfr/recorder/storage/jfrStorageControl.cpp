/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/storage/jfrStorageControl.hpp"
#include "runtime/atomic.hpp"

const size_t max_lease_factor = 2;
JfrStorageControl::JfrStorageControl(size_t global_count_total, size_t in_memory_discard_threshold) :
  _global_count_total(global_count_total),
  _full_count(0),
  _global_lease_count(0),
  _to_disk_threshold(0),
  _in_memory_discard_threshold(in_memory_discard_threshold),
  _global_lease_threshold(global_count_total / max_lease_factor),
  _to_disk(false) {}

bool JfrStorageControl::to_disk() const {
  return _to_disk;
}

void JfrStorageControl::set_to_disk(bool enable) {
  _to_disk = enable;
}

size_t JfrStorageControl::full_count() const {
  return _full_count;
}

bool JfrStorageControl::increment_full() {
  const size_t result = Atomic::add(&_full_count, (size_t)1);
  return to_disk() && result > _to_disk_threshold;
}

size_t JfrStorageControl::decrement_full() {
  assert(_full_count > 0, "invariant");
  size_t current;
  size_t exchange;
  do {
    current = _full_count;
    exchange = current - 1;
  } while (Atomic::cmpxchg(&_full_count, current, exchange) != current);
  return exchange;
}

void JfrStorageControl::reset_full() {
  Atomic::store(&_full_count, (size_t)0);
}

bool JfrStorageControl::should_post_buffer_full_message() const {
  return to_disk() && (full_count() > _to_disk_threshold);
}

bool JfrStorageControl::should_discard() const {
  return !to_disk() && full_count() >= _in_memory_discard_threshold;
}

size_t JfrStorageControl::global_lease_count() const {
  return Atomic::load(&_global_lease_count);
}

size_t JfrStorageControl::increment_leased() {
  return Atomic::add(&_global_lease_count, (size_t)1);
}

size_t JfrStorageControl::decrement_leased() {
  size_t current;
  size_t exchange;
  do {
    current = _global_lease_count;
    exchange = current - 1;
  } while (Atomic::cmpxchg(&_global_lease_count, current, exchange) != current);
  return exchange;
}

bool JfrStorageControl::is_global_lease_allowed() const {
  return global_lease_count() <= _global_lease_threshold;
}
