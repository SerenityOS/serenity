/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZTHREAD_HPP
#define SHARE_GC_Z_ZTHREAD_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class ZThread : public AllStatic {
  friend class ZTask;
  friend class ZWorkersInitializeTask;
  friend class ZRuntimeWorkersInitializeTask;

private:
  static THREAD_LOCAL bool      _initialized;
  static THREAD_LOCAL uintptr_t _id;
  static THREAD_LOCAL bool      _is_vm;
  static THREAD_LOCAL bool      _is_java;
  static THREAD_LOCAL bool      _is_worker;
  static THREAD_LOCAL uint      _worker_id;

  static void initialize();
  static void ensure_initialized();

  static void set_worker();

  static bool has_worker_id();
  static void set_worker_id(uint worker_id);
  static void clear_worker_id();

public:
  static const char* name();
  static uintptr_t id();
  static bool is_vm();
  static bool is_java();
  static bool is_worker();
  static uint worker_id();
};

#endif // SHARE_GC_Z_ZTHREAD_HPP
