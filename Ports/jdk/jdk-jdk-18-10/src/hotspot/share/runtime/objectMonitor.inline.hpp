/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_OBJECTMONITOR_INLINE_HPP
#define SHARE_RUNTIME_OBJECTMONITOR_INLINE_HPP

#include "runtime/objectMonitor.hpp"

#include "logging/log.hpp"
#include "oops/access.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/synchronizer.hpp"

inline intptr_t ObjectMonitor::is_entered(JavaThread* current) const {
  void* owner = owner_raw();
  if (current == owner || current->is_lock_owned((address)owner)) {
    return 1;
  }
  return 0;
}

inline markWord ObjectMonitor::header() const {
  return Atomic::load(&_header);
}

inline volatile markWord* ObjectMonitor::header_addr() {
  return &_header;
}

inline void ObjectMonitor::set_header(markWord hdr) {
  Atomic::store(&_header, hdr);
}

inline int ObjectMonitor::waiters() const {
  return _waiters;
}

// Returns NULL if DEFLATER_MARKER is observed.
inline void* ObjectMonitor::owner() const {
  void* owner = owner_raw();
  return owner != DEFLATER_MARKER ? owner : NULL;
}

inline void* ObjectMonitor::owner_raw() const {
  return Atomic::load(&_owner);
}

// Returns true if owner field == DEFLATER_MARKER and false otherwise.
// This accessor is called when we really need to know if the owner
// field == DEFLATER_MARKER and any non-NULL value won't do the trick.
inline bool ObjectMonitor::owner_is_DEFLATER_MARKER() const {
  return owner_raw() == DEFLATER_MARKER;
}

// Returns true if 'this' is being async deflated and false otherwise.
inline bool ObjectMonitor::is_being_async_deflated() {
  return contentions() < 0;
}

// Return number of threads contending for this monitor.
inline int ObjectMonitor::contentions() const {
  return Atomic::load(&_contentions);
}

// Add value to the contentions field.
inline void ObjectMonitor::add_to_contentions(int value) {
  Atomic::add(&_contentions, value);
}

// Clear _owner field; current value must match old_value.
inline void ObjectMonitor::release_clear_owner(void* old_value) {
#ifdef ASSERT
  void* prev = Atomic::load(&_owner);
  assert(prev == old_value, "unexpected prev owner=" INTPTR_FORMAT
         ", expected=" INTPTR_FORMAT, p2i(prev), p2i(old_value));
#endif
  Atomic::release_store(&_owner, (void*)NULL);
  log_trace(monitorinflation, owner)("release_clear_owner(): mid="
                                     INTPTR_FORMAT ", old_value=" INTPTR_FORMAT,
                                     p2i(this), p2i(old_value));
}

// Simply set _owner field to new_value; current value must match old_value.
// (Simple means no memory sync needed.)
inline void ObjectMonitor::set_owner_from(void* old_value, void* new_value) {
#ifdef ASSERT
  void* prev = Atomic::load(&_owner);
  assert(prev == old_value, "unexpected prev owner=" INTPTR_FORMAT
         ", expected=" INTPTR_FORMAT, p2i(prev), p2i(old_value));
#endif
  Atomic::store(&_owner, new_value);
  log_trace(monitorinflation, owner)("set_owner_from(): mid="
                                     INTPTR_FORMAT ", old_value=" INTPTR_FORMAT
                                     ", new_value=" INTPTR_FORMAT, p2i(this),
                                     p2i(old_value), p2i(new_value));
}

// Simply set _owner field to self; current value must match basic_lock_p.
inline void ObjectMonitor::set_owner_from_BasicLock(void* basic_lock_p, JavaThread* current) {
#ifdef ASSERT
  void* prev = Atomic::load(&_owner);
  assert(prev == basic_lock_p, "unexpected prev owner=" INTPTR_FORMAT
         ", expected=" INTPTR_FORMAT, p2i(prev), p2i(basic_lock_p));
#endif
  // Non-null owner field to non-null owner field is safe without
  // cmpxchg() as long as all readers can tolerate either flavor.
  Atomic::store(&_owner, current);
  log_trace(monitorinflation, owner)("set_owner_from_BasicLock(): mid="
                                     INTPTR_FORMAT ", basic_lock_p="
                                     INTPTR_FORMAT ", new_value=" INTPTR_FORMAT,
                                     p2i(this), p2i(basic_lock_p), p2i(current));
}

// Try to set _owner field to new_value if the current value matches
// old_value. Otherwise, does not change the _owner field. Returns
// the prior value of the _owner field.
inline void* ObjectMonitor::try_set_owner_from(void* old_value, void* new_value) {
  void* prev = Atomic::cmpxchg(&_owner, old_value, new_value);
  if (prev == old_value) {
    log_trace(monitorinflation, owner)("try_set_owner_from(): mid="
                                       INTPTR_FORMAT ", prev=" INTPTR_FORMAT
                                       ", new=" INTPTR_FORMAT, p2i(this),
                                       p2i(prev), p2i(new_value));
  }
  return prev;
}

// The _next_om field can be concurrently read and modified so we
// use Atomic operations to disable compiler optimizations that
// might try to elide loading and/or storing this field.

// Simply get _next_om field.
inline ObjectMonitor* ObjectMonitor::next_om() const {
  return Atomic::load(&_next_om);
}

// Get _next_om field with acquire semantics.
inline ObjectMonitor* ObjectMonitor::next_om_acquire() const {
  return Atomic::load_acquire(&_next_om);
}

// Simply set _next_om field to new_value.
inline void ObjectMonitor::set_next_om(ObjectMonitor* new_value) {
  Atomic::store(&_next_om, new_value);
}

// Set _next_om field to new_value with release semantics.
inline void ObjectMonitor::release_set_next_om(ObjectMonitor* new_value) {
  Atomic::release_store(&_next_om, new_value);
}

// Try to set _next_om field to new_value if the current value matches
// old_value. Otherwise, does not change the _next_om field. Returns
// the prior value of the _next_om field.
inline ObjectMonitor* ObjectMonitor::try_set_next_om(ObjectMonitor* old_value, ObjectMonitor* new_value) {
  return Atomic::cmpxchg(&_next_om, old_value, new_value);
}

#endif // SHARE_RUNTIME_OBJECTMONITOR_INLINE_HPP
