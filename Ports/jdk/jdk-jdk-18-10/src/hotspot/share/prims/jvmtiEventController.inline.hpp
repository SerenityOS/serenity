/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIEVENTCONTROLLER_INLINE_HPP
#define SHARE_PRIMS_JVMTIEVENTCONTROLLER_INLINE_HPP

#include "prims/jvmtiEventController.hpp"

#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiUtil.hpp"

// these inline functions are in a separate file to break include cycles


///////////////////////////////////////////////////////////////
//
// JvmtiEventEnabled
//

inline jlong JvmtiEventEnabled::bit_for(jvmtiEvent event_type) {
  assert(JvmtiEventController::is_valid_event_type(event_type), "invalid event type");
  return ((jlong)1) << (event_type - TOTAL_MIN_EVENT_TYPE_VAL);
}

inline jlong JvmtiEventEnabled::get_bits() {
  assert(_init_guard == JEE_INIT_GUARD, "enable bits uninitialized or corrupted");
  return _enabled_bits;
}

inline void JvmtiEventEnabled::set_bits(jlong bits) {
  assert(_init_guard == JEE_INIT_GUARD, "enable bits uninitialized or corrupted on set");
  _enabled_bits = bits;
}

inline bool JvmtiEventEnabled::is_enabled(jvmtiEvent event_type) {
  return (bit_for(event_type) & get_bits()) != 0;
}


///////////////////////////////////////////////////////////////
//
// JvmtiEnvThreadEventEnable
//

inline bool JvmtiEnvThreadEventEnable::is_enabled(jvmtiEvent event_type) {
  assert(JvmtiUtil::event_threaded(event_type), "Only thread filtered events should be tested here");
  return _event_enabled.is_enabled(event_type);
}

inline void JvmtiEnvThreadEventEnable::set_user_enabled(jvmtiEvent event_type, bool enabled) {
  _event_user_enabled.set_enabled(event_type, enabled);
}


///////////////////////////////////////////////////////////////
//
// JvmtiThreadEventEnable
//

inline bool JvmtiThreadEventEnable::is_enabled(jvmtiEvent event_type) {
  assert(JvmtiUtil::event_threaded(event_type), "Only thread filtered events should be tested here");
  return _event_enabled.is_enabled(event_type);
}


///////////////////////////////////////////////////////////////
//
// JvmtiEnvEventEnable
//

inline bool JvmtiEnvEventEnable::is_enabled(jvmtiEvent event_type) {
  assert(!JvmtiUtil::event_threaded(event_type), "Only non thread filtered events should be tested here");
  return _event_enabled.is_enabled(event_type);
}

inline void JvmtiEnvEventEnable::set_user_enabled(jvmtiEvent event_type, bool enabled) {
  _event_user_enabled.set_enabled(event_type, enabled);
}


///////////////////////////////////////////////////////////////
//
// JvmtiEventController
//

inline bool JvmtiEventController::is_enabled(jvmtiEvent event_type) {
  return _universal_global_event_enabled.is_enabled(event_type);
}

#endif // SHARE_PRIMS_JVMTIEVENTCONTROLLER_INLINE_HPP
