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
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "runtime/vm_version.hpp"
#include "services/attachListener.hpp"
#include "services/management.hpp"
#include "services/runtimeService.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/macros.hpp"

#if INCLUDE_MANAGEMENT
PerfCounter*  RuntimeService::_sync_time_ticks = NULL;
PerfCounter*  RuntimeService::_total_safepoints = NULL;
PerfCounter*  RuntimeService::_safepoint_time_ticks = NULL;
PerfCounter*  RuntimeService::_application_time_ticks = NULL;

void RuntimeService::init() {
  if (UsePerfData) {
    EXCEPTION_MARK;

    _sync_time_ticks =
              PerfDataManager::create_counter(SUN_RT, "safepointSyncTime",
                                              PerfData::U_Ticks, CHECK);

    _total_safepoints =
              PerfDataManager::create_counter(SUN_RT, "safepoints",
                                              PerfData::U_Events, CHECK);

    _safepoint_time_ticks =
              PerfDataManager::create_counter(SUN_RT, "safepointTime",
                                              PerfData::U_Ticks, CHECK);

    _application_time_ticks =
              PerfDataManager::create_counter(SUN_RT, "applicationTime",
                                              PerfData::U_Ticks, CHECK);


    // create performance counters for jvm_version and its capabilities
    PerfDataManager::create_constant(SUN_RT, "jvmVersion", PerfData::U_None,
                                     (jlong) VM_Version::jvm_version(), CHECK);

    // The capabilities counter is a binary representation of the VM capabilities in string.
    // This string respresentation simplifies the implementation of the client side
    // to parse the value.
    char capabilities[65];
    size_t len = sizeof(capabilities);
    memset((void*) capabilities, '0', len);
    capabilities[len-1] = '\0';
    capabilities[0] = AttachListener::is_attach_supported() ? '1' : '0';
#if INCLUDE_SERVICES
    capabilities[1] = '1';
#endif // INCLUDE_SERVICES
    PerfDataManager::create_string_constant(SUN_RT, "jvmCapabilities",
                                            capabilities, CHECK);
  }
}

void RuntimeService::record_safepoint_begin(jlong app_ticks) {
  HS_PRIVATE_SAFEPOINT_BEGIN();
  if (UsePerfData) {
    _total_safepoints->inc();
    _application_time_ticks->inc(app_ticks);
  }
}

void RuntimeService::record_safepoint_synchronized(jlong sync_ticks) {
  if (UsePerfData) {
    _sync_time_ticks->inc(sync_ticks);
  }
}

void RuntimeService::record_safepoint_end(jlong safepoint_ticks) {
  HS_PRIVATE_SAFEPOINT_END();
  if (UsePerfData) {
    _safepoint_time_ticks->inc(safepoint_ticks);
  }
}

jlong RuntimeService::safepoint_sync_time_ms() {
  return UsePerfData ?
    Management::ticks_to_ms(_sync_time_ticks->get_value()) : -1;
}

jlong RuntimeService::safepoint_count() {
  return UsePerfData ?
    _total_safepoints->get_value() : -1;
}
jlong RuntimeService::safepoint_time_ms() {
  return UsePerfData ?
    Management::ticks_to_ms(_safepoint_time_ticks->get_value()) : -1;
}

jlong RuntimeService::application_time_ms() {
  return UsePerfData ?
    Management::ticks_to_ms(_application_time_ticks->get_value()) : -1;
}

#endif // INCLUDE_MANAGEMENT
