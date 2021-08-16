/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_JFREVENTSETTING_INLINE_HPP
#define SHARE_JFR_RECORDER_JFREVENTSETTING_INLINE_HPP

#include "jfr/recorder/jfrEventSetting.hpp"

inline jfrNativeEventSetting& JfrEventSetting::setting(JfrEventId event_id) {
  return _jvm_event_settings.bits[event_id];
}

inline bool JfrEventSetting::is_enabled(JfrEventId event_id) {
  return 0 != setting(event_id).enabled;
}

inline bool JfrEventSetting::has_stacktrace(JfrEventId event_id) {
  return 0 != setting(event_id).stacktrace;
}

inline jlong JfrEventSetting::threshold(JfrEventId event_id) {
  return setting(event_id).threshold_ticks;
}

inline jlong JfrEventSetting::cutoff(JfrEventId event_id) {
  return setting(event_id).cutoff_ticks;
}

inline bool JfrEventSetting::is_large(JfrEventId event_id) {
  return setting(event_id).large != 0;
}
#endif // SHARE_JFR_RECORDER_JFREVENTSETTING_INLINE_HPP
