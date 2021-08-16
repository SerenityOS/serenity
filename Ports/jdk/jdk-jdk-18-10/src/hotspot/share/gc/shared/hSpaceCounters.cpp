/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/hSpaceCounters.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/perfData.hpp"

HSpaceCounters::HSpaceCounters(const char* name_space,
                               const char* name,
                               int ordinal,
                               size_t max_size,
                               size_t initial_capacity) {

  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cns =
      PerfDataManager::name_space(name_space, "space", ordinal);

    _name_space = NEW_C_HEAP_ARRAY(char, strlen(cns)+1, mtGC);
    strcpy(_name_space, cns);

    const char* cname = PerfDataManager::counter_name(_name_space, "name");
    PerfDataManager::create_string_constant(SUN_GC, cname, name, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "maxCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     (jlong)max_size, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "capacity");
    _capacity = PerfDataManager::create_variable(SUN_GC, cname,
                                                 PerfData::U_Bytes,
                                                 initial_capacity, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "used");
    _used = PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
                                             (jlong) 0, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "initCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     initial_capacity, CHECK);
  }
}

HSpaceCounters::~HSpaceCounters() {
  FREE_C_HEAP_ARRAY(char, _name_space);
}

void HSpaceCounters::update_capacity(size_t v) {
  _capacity->set_value(v);
}

void HSpaceCounters::update_used(size_t v) {
  _used->set_value(v);
}

void HSpaceCounters::update_all(size_t capacity, size_t used) {
  update_capacity(capacity);
  update_used(used);
}

debug_only(
  // for security reasons, we do not allow arbitrary reads from
  // the counters as they may live in shared memory.
  jlong HSpaceCounters::used() {
    return _used->get_value();
  }
  jlong HSpaceCounters::capacity() {
    return _used->get_value();
  }
)
