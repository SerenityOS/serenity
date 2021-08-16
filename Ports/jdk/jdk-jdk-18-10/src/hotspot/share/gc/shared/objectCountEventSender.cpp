/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcId.hpp"
#include "gc/shared/objectCountEventSender.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/heapInspection.hpp"
#include "utilities/macros.hpp"
#include "utilities/ticks.hpp"
#if INCLUDE_SERVICES

bool ObjectCountEventSender::should_send_event() {
#if INCLUDE_JFR
  return _should_send_requestable_event || EventObjectCountAfterGC::is_enabled();
#else
  return false;
#endif // INCLUDE_JFR
}

bool ObjectCountEventSender::_should_send_requestable_event = false;

void ObjectCountEventSender::enable_requestable_event() {
  _should_send_requestable_event = true;
}

void ObjectCountEventSender::disable_requestable_event() {
  _should_send_requestable_event = false;
}

template <typename T>
void ObjectCountEventSender::send_event_if_enabled(Klass* klass, jlong count, julong size, const Ticks& timestamp) {
  T event(UNTIMED);
  if (event.should_commit()) {
    event.set_gcId(GCId::current());
    event.set_objectClass(klass);
    event.set_count(count);
    event.set_totalSize(size);
    event.set_endtime(timestamp);
    event.commit();
  }
}

void ObjectCountEventSender::send(const KlassInfoEntry* entry, const Ticks& timestamp) {
  Klass* klass = entry->klass();
  jlong count = entry->count();
  julong total_size = entry->words() * BytesPerWord;

  send_event_if_enabled<EventObjectCount>(klass, count, total_size, timestamp);
  send_event_if_enabled<EventObjectCountAfterGC>(klass, count, total_size, timestamp);
}

#endif // INCLUDE_SERVICES
