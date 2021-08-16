/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_INLINE_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_INLINE_HPP

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.hpp"

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdLoadBarrier.inline.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdBits.inline.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdEpoch.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdMacros.hpp"
#include "jfr/support/jfrKlassExtension.hpp"
#include "oops/klass.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/debug.hpp"

inline traceid JfrTraceId::load(const Klass* klass) {
  return JfrTraceIdLoadBarrier::load(klass);
}

inline traceid JfrTraceId::load(const Method* method) {
  return JfrTraceIdLoadBarrier::load(method);
}

inline traceid JfrTraceId::load(const Klass* klass, const Method* method) {
  return JfrTraceIdLoadBarrier::load(klass, method);
}

inline traceid JfrTraceId::load(const ModuleEntry* module) {
  return JfrTraceIdLoadBarrier::load(module);
}

inline traceid JfrTraceId::load(const PackageEntry* package) {
  return JfrTraceIdLoadBarrier::load(package);
}

inline traceid JfrTraceId::load(const ClassLoaderData* cld) {
  return JfrTraceIdLoadBarrier::load(cld);
}

inline traceid JfrTraceId::load_leakp(const Klass* klass, const Method* method) {
  return JfrTraceIdLoadBarrier::load_leakp(klass, method);
}

template <typename T>
inline traceid raw_load(const T* t) {
  assert(t != NULL, "invariant");
  return TRACE_ID(t);
}

inline traceid JfrTraceId::load_raw(const Klass* klass) {
  return raw_load(klass);
}

inline traceid JfrTraceId::load_raw(const Thread* t) {
  assert(t != NULL, "invariant");
  return TRACE_ID_RAW(t->jfr_thread_local());
}

inline traceid JfrTraceId::load_raw(const Method* method) {
  return (METHOD_ID(method->method_holder(), method));
}

inline traceid JfrTraceId::load_raw(const ModuleEntry* module) {
  return raw_load(module);
}

inline traceid JfrTraceId::load_raw(const PackageEntry* package) {
  return raw_load(package);
}

inline traceid JfrTraceId::load_raw(const ClassLoaderData* cld) {
  return raw_load(cld);
}

inline bool JfrTraceId::in_visible_set(const Klass* klass) {
  assert(klass != NULL, "invariant");
  assert(JavaThread::current()->thread_state() == _thread_in_vm, "invariant");
  return (IS_JDK_JFR_EVENT_SUBKLASS(klass) && !klass->is_abstract()) || IS_EVENT_HOST_KLASS(klass);
}

inline bool JfrTraceId::is_jdk_jfr_event(const Klass* k) {
  assert(k != NULL, "invariant");
  return IS_JDK_JFR_EVENT_KLASS(k);
}

inline void JfrTraceId::tag_as_jdk_jfr_event(const Klass* klass) {
  assert(klass != NULL, "invariant");
  SET_JDK_JFR_EVENT_KLASS(klass);
  assert(IS_JDK_JFR_EVENT_KLASS(klass), "invariant");
}

inline bool JfrTraceId::is_jdk_jfr_event_sub(const Klass* k) {
  assert(k != NULL, "invariant");
  return IS_JDK_JFR_EVENT_SUBKLASS(k);
}

inline void JfrTraceId::tag_as_jdk_jfr_event_sub(const Klass* k) {
  assert(k != NULL, "invariant");
  if (IS_NOT_AN_EVENT_SUB_KLASS(k)) {
    SET_JDK_JFR_EVENT_SUBKLASS(k);
  }
  assert(IS_JDK_JFR_EVENT_SUBKLASS(k), "invariant");
}

inline bool JfrTraceId::in_jdk_jfr_event_hierarchy(const Klass* klass) {
  assert(klass != NULL, "invariant");
  if (is_jdk_jfr_event(klass)) {
    return true;
  }
  const Klass* const super = klass->super();
  return super != NULL ? IS_EVENT_KLASS(super) : false;
}

inline bool JfrTraceId::is_event_host(const Klass* k) {
  assert(k != NULL, "invariant");
  return IS_EVENT_HOST_KLASS(k);
}

inline void JfrTraceId::tag_as_event_host(const Klass* k) {
  assert(k != NULL, "invariant");
  SET_EVENT_HOST_KLASS(k);
  assert(IS_EVENT_HOST_KLASS(k), "invariant");
}

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEID_INLINE_HPP
