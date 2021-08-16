/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/generationCounters.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/perfData.hpp"

void GenerationCounters::initialize(const char* name, int ordinal, int spaces,
                                    size_t min_capacity, size_t max_capacity,
                                    size_t curr_capacity) {
  if (UsePerfData) {
    EXCEPTION_MARK;
    ResourceMark rm;

    const char* cns = PerfDataManager::name_space("generation", ordinal);

    _name_space = NEW_C_HEAP_ARRAY(char, strlen(cns)+1, mtGC);
    strcpy(_name_space, cns);

    const char* cname = PerfDataManager::counter_name(_name_space, "name");
    PerfDataManager::create_string_constant(SUN_GC, cname, name, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "spaces");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_None,
                                     spaces, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "minCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     min_capacity, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "maxCapacity");
    PerfDataManager::create_constant(SUN_GC, cname, PerfData::U_Bytes,
                                     max_capacity, CHECK);

    cname = PerfDataManager::counter_name(_name_space, "capacity");
    _current_size =
      PerfDataManager::create_variable(SUN_GC, cname, PerfData::U_Bytes,
                                       curr_capacity, CHECK);
  }
}

GenerationCounters::GenerationCounters(const char* name,
                                       int ordinal, int spaces,
                                       size_t min_capacity, size_t max_capacity,
                                       VirtualSpace* v)
  : _virtual_space(v) {
  assert(v != NULL, "don't call this constructor if v == NULL");
  initialize(name, ordinal, spaces,
             min_capacity, max_capacity, v->committed_size());
}

GenerationCounters::GenerationCounters(const char* name,
                                       int ordinal, int spaces,
                                       size_t min_capacity, size_t max_capacity,
                                       size_t curr_capacity)
  : _virtual_space(NULL) {
  initialize(name, ordinal, spaces, min_capacity, max_capacity, curr_capacity);
}

GenerationCounters::~GenerationCounters() {
  FREE_C_HEAP_ARRAY(char, _name_space);
}

void GenerationCounters::update_all() {
  assert(_virtual_space != NULL, "otherwise, override this method");
  _current_size->set_value(_virtual_space->committed_size());
}
