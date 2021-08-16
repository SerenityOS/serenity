/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/support/jfrKlassUnloading.hpp"
#include "jfr/utilities/jfrPredicate.hpp"
#include "jfr/utilities/jfrRelation.hpp"
#include "runtime/mutexLocker.hpp"

static const int initial_array_size = 64;

template <typename T>
static GrowableArray<T>* c_heap_allocate_array(int size = initial_array_size) {
  return new (ResourceObj::C_HEAP, mtTracing) GrowableArray<T>(size, mtTracing);
}

// Track the set of unloaded klasses during a chunk / epoch.
static GrowableArray<traceid>* _unload_set_epoch_0 = NULL;
static GrowableArray<traceid>* _unload_set_epoch_1 = NULL;

static s8 event_klass_unloaded_count = 0;

static GrowableArray<traceid>* unload_set_epoch_0() {
  if (_unload_set_epoch_0 == NULL) {
    _unload_set_epoch_0 = c_heap_allocate_array<traceid>(initial_array_size);
  }
  return _unload_set_epoch_0;
}

static GrowableArray<traceid>* unload_set_epoch_1() {
  if (_unload_set_epoch_1 == NULL) {
    _unload_set_epoch_1 = c_heap_allocate_array<traceid>(initial_array_size);
  }
  return _unload_set_epoch_1;
}

static GrowableArray<traceid>* get_unload_set(u1 epoch) {
  return epoch == 0 ? unload_set_epoch_0() : unload_set_epoch_1();
}

static GrowableArray<traceid>* get_unload_set() {
  return get_unload_set(JfrTraceIdEpoch::current());
}

static GrowableArray<traceid>* get_unload_set_previous_epoch() {
  return get_unload_set(JfrTraceIdEpoch::previous());
}

static void sort_set(GrowableArray<traceid>* set) {
  assert(set != NULL, "invariant");
  assert(set->is_nonempty(), "invariant");
  set->sort(sort_traceid);
}

static bool is_nonempty_set(u1 epoch) {
  if (epoch == 0) {
    return _unload_set_epoch_0 != NULL && _unload_set_epoch_0->is_nonempty();
  }
  return _unload_set_epoch_1 != NULL && _unload_set_epoch_1->is_nonempty();
}

void JfrKlassUnloading::sort(bool previous_epoch) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  if (is_nonempty_set(JfrTraceIdEpoch::current())) {
    sort_set(get_unload_set());
  }
  if (previous_epoch && is_nonempty_set(JfrTraceIdEpoch::previous())) {
    sort_set(get_unload_set_previous_epoch());
  }
}

void JfrKlassUnloading::clear() {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  if (is_nonempty_set(JfrTraceIdEpoch::previous())) {
    get_unload_set_previous_epoch()->clear();
  }
}

static bool add_to_unloaded_klass_set(traceid klass_id, bool current_epoch) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  GrowableArray<traceid>* const unload_set = current_epoch ? get_unload_set() : get_unload_set_previous_epoch();
  assert(unload_set != NULL, "invariant");
  assert(unload_set->find(klass_id) == -1, "invariant");
  unload_set->append(klass_id);
  return true;
}

bool JfrKlassUnloading::on_unload(const Klass* k) {
  assert(k != NULL, "invariant");
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  if (IS_JDK_JFR_EVENT_SUBKLASS(k)) {
    ++event_klass_unloaded_count;
  }
  return USED_ANY_EPOCH(k) && add_to_unloaded_klass_set(JfrTraceId::load_raw(k), USED_THIS_EPOCH(k));
}

bool JfrKlassUnloading::is_unloaded(traceid klass_id, bool previous_epoch /* false */) {
  assert_locked_or_safepoint(ClassLoaderDataGraph_lock);
  if (previous_epoch) {
    if (JfrPredicate<traceid, compare_traceid>::test(get_unload_set_previous_epoch(), klass_id)) {
      return true;
    }
  }
  return JfrPredicate<traceid, compare_traceid>::test(get_unload_set(), klass_id);
}

int64_t JfrKlassUnloading::event_class_count() {
  return event_klass_unloaded_count;
}
