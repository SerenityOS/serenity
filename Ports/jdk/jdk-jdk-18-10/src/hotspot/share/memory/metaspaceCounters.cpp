/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metaspace.hpp"
#include "memory/metaspaceCounters.hpp"
#include "memory/metaspaceStats.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/globals.hpp"
#include "runtime/perfData.hpp"
#include "utilities/exceptions.hpp"

class MetaspacePerfCounters {
  friend class VMStructs;
  PerfVariable*      _capacity;
  PerfVariable*      _used;
  PerfVariable*      _max_capacity;

  PerfVariable* create_variable(const char *ns, const char *name, size_t value, TRAPS) {
    const char *path = PerfDataManager::counter_name(ns, name);
    return PerfDataManager::create_variable(SUN_GC, path, PerfData::U_Bytes, value, THREAD);
  }

  void create_constant(const char *ns, const char *name, size_t value, TRAPS) {
    const char *path = PerfDataManager::counter_name(ns, name);
    PerfDataManager::create_constant(SUN_GC, path, PerfData::U_Bytes, value, THREAD);
  }

 public:
  MetaspacePerfCounters() : _capacity(NULL), _used(NULL), _max_capacity(NULL) {}

  void initialize(const char* ns) {
    assert(_capacity == NULL, "Only initialize once");
    EXCEPTION_MARK;
    ResourceMark rm;

    create_constant(ns, "minCapacity", 0, THREAD); // min_capacity makes little sense in the context of metaspace
    _capacity = create_variable(ns, "capacity", 0, THREAD);
    _max_capacity = create_variable(ns, "maxCapacity", 0, THREAD);
    _used = create_variable(ns, "used", 0, THREAD);
  }

  void update(const MetaspaceStats& stats) {
    _capacity->set_value(stats.committed());
    _max_capacity->set_value(stats.reserved());
    _used->set_value(stats.used());
  }
};

static MetaspacePerfCounters g_meta_space_perf_counters; // class + nonclass
static MetaspacePerfCounters g_class_space_perf_counters;

void MetaspaceCounters::initialize_performance_counters() {
  if (UsePerfData) {
    g_meta_space_perf_counters.initialize("metaspace");
    g_class_space_perf_counters.initialize("compressedclassspace");
    update_performance_counters();
  }
}

void MetaspaceCounters::update_performance_counters() {
  if (UsePerfData) {
    g_meta_space_perf_counters.update(MetaspaceUtils::get_combined_statistics());
    g_class_space_perf_counters.update(MetaspaceUtils::get_statistics(Metaspace::ClassType));
  }
}
