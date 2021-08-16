/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_PRETOUCH_HPP
#define SHARE_GC_SHARED_PRETOUCH_HPP

#include "gc/shared/workgroup.hpp"

class PretouchTask : public AbstractGangTask {
  char* volatile _cur_addr;
  char* const _start_addr;
  char* const _end_addr;
  size_t _page_size;
  size_t _chunk_size;

public:
  PretouchTask(const char* task_name, char* start_address, char* end_address, size_t page_size, size_t chunk_size);

  virtual void work(uint worker_id);

  static size_t chunk_size();

  static void pretouch(const char* task_name, char* start_address, char* end_address,
                       size_t page_size, WorkGang* pretouch_gang);

};

#endif // SHARE_GC_SHARED_PRETOUCH_HPP
