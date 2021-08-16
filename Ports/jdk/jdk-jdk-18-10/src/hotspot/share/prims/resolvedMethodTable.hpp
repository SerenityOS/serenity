/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_RESOLVEDMETHODTABLE_HPP
#define SHARE_PRIMS_RESOLVEDMETHODTABLE_HPP

#include "memory/allocation.hpp"
#include "oops/symbol.hpp"
#include "oops/weakHandle.hpp"

class ResolvedMethodTable;
class ResolvedMethodTableConfig;

class ResolvedMethodTable : public AllStatic {
  friend class ResolvedMethodTableConfig;

  static volatile bool _has_work;
  static OopStorage* _oop_storage;

  // Callback for GC to notify of changes that might require cleaning or resize.
  static void gc_notification(size_t num_dead);
  static void trigger_concurrent_work();

  static double get_load_factor();
  static double get_dead_factor(size_t num_dead);

  static void grow(JavaThread* jt);
  static void clean_dead_entries(JavaThread* jt);

public:
  // Initialization
  static void create_table();

  static size_t table_size();

  // Lookup and inserts
  static oop find_method(const Method* method);
  static oop add_method(const Method* method, Handle rmethod_name);

  // Callbacks
  static void item_added();
  static void item_removed();

  // Cleaning
  static bool has_work();
  static void do_concurrent_work(JavaThread* jt);


  // JVMTI Support - It is called at safepoint only for RedefineClasses
  JVMTI_ONLY(static void adjust_method_entries(bool * trace_name_printed);)

  // Debugging
  static size_t items_count();
  static void verify();
};

#endif // SHARE_PRIMS_RESOLVEDMETHODTABLE_HPP
