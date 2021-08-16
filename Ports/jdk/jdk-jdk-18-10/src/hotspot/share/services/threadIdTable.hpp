
/*
* Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_THREADIDTABLE_HPP
#define SHARE_SERVICES_THREADIDTABLE_HPP

#include "memory/allocation.hpp"

class JavaThread;
class ThreadsList;
class ThreadIdTableConfig;

class ThreadIdTable : public AllStatic {
  friend class ThreadIdTableConfig;

  static volatile bool _is_initialized;
  static volatile bool _has_work;

public:
  // Initialization
  static void lazy_initialize(const ThreadsList* threads);
  static bool is_initialized() { return _is_initialized; }

  // Lookup and list management
  static JavaThread* find_thread_by_tid(jlong tid);
  static JavaThread* add_thread(jlong tid, JavaThread* thread);
  static bool remove_thread(jlong tid);

  // Growing
  static bool has_work() { return _has_work; }
  static void do_concurrent_work(JavaThread* jt);

private:
  static void create_table(size_t size);

  static size_t table_size();
  static double get_load_factor();
  static void check_concurrent_work();
  static void trigger_concurrent_work();
  static void grow(JavaThread* jt);

  static void item_added();
  static void item_removed();
};

#endif // SHARE_SERVICES_THREADIDTABLE_HPP
