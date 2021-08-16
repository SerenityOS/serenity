/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CDS_DUMPALLOCSTATS_HPP
#define SHARE_CDS_DUMPALLOCSTATS_HPP

#include "classfile/compactHashtable.hpp"
#include "memory/allocation.hpp"

// This is for dumping detailed statistics for the allocations
// in the shared spaces.
class DumpAllocStats : public ResourceObj {
public:

  // Here's poor man's enum inheritance
#define SHAREDSPACE_OBJ_TYPES_DO(f) \
  METASPACE_OBJ_TYPES_DO(f) \
  f(SymbolHashentry) \
  f(SymbolBucket) \
  f(StringHashentry) \
  f(StringBucket) \
  f(ModulesNatives) \
  f(CppVTables) \
  f(Other)

  enum Type {
    // Types are MetaspaceObj::ClassType, MetaspaceObj::SymbolType, etc
    SHAREDSPACE_OBJ_TYPES_DO(METASPACE_OBJ_TYPE_DECLARE)
    _number_of_types
  };

  static const char* type_name(Type type) {
    switch(type) {
    SHAREDSPACE_OBJ_TYPES_DO(METASPACE_OBJ_TYPE_NAME_CASE)
    default:
      ShouldNotReachHere();
      return NULL;
    }
  }

  CompactHashtableStats _symbol_stats;
  CompactHashtableStats _string_stats;

  int _counts[2][_number_of_types];
  int _bytes [2][_number_of_types];

public:
  enum { RO = 0, RW = 1 };

  DumpAllocStats() {
    memset(_counts, 0, sizeof(_counts));
    memset(_bytes,  0, sizeof(_bytes));
  };

  CompactHashtableStats* symbol_stats() { return &_symbol_stats; }
  CompactHashtableStats* string_stats() { return &_string_stats; }

  void record(MetaspaceObj::Type type, int byte_size, bool read_only) {
    assert(int(type) >= 0 && type < MetaspaceObj::_number_of_types, "sanity");
    int which = (read_only) ? RO : RW;
    _counts[which][type] ++;
    _bytes [which][type] += byte_size;
  }

  void record_modules(int byte_size, bool read_only) {
    int which = (read_only) ? RO : RW;
    _bytes [which][ModulesNativesType] += byte_size;
  }

  void record_other_type(int byte_size, bool read_only) {
    int which = (read_only) ? RO : RW;
    _bytes [which][OtherType] += byte_size;
  }

  void record_cpp_vtables(int byte_size) {
    _bytes[RW][CppVTablesType] += byte_size;
  }

  void print_stats(int ro_all, int rw_all);
};

#endif // SHARE_CDS_DUMPALLOCSTATS_HPP
