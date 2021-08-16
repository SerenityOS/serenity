/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_EPSILON_EPSILONTHREADLOCALDATA_HPP
#define SHARE_GC_EPSILON_EPSILONTHREADLOCALDATA_HPP

#include "gc/shared/gc_globals.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"

class EpsilonThreadLocalData {
private:
  size_t _ergo_tlab_size;
  int64_t _last_tlab_time;

  EpsilonThreadLocalData() :
          _ergo_tlab_size(0),
          _last_tlab_time(0) {}

  static EpsilonThreadLocalData* data(Thread* thread) {
    assert(UseEpsilonGC, "Sanity");
    return thread->gc_data<EpsilonThreadLocalData>();
  }

public:
  static void create(Thread* thread) {
    new (data(thread)) EpsilonThreadLocalData();
  }

  static void destroy(Thread* thread) {
    data(thread)->~EpsilonThreadLocalData();
  }

  static size_t ergo_tlab_size(Thread *thread) {
    return data(thread)->_ergo_tlab_size;
  }

  static int64_t last_tlab_time(Thread *thread) {
    return data(thread)->_last_tlab_time;
  }

  static void set_ergo_tlab_size(Thread *thread, size_t val) {
    data(thread)->_ergo_tlab_size = val;
  }

  static void set_last_tlab_time(Thread *thread, int64_t time) {
    data(thread)->_last_tlab_time = time;
  }
};

#endif // SHARE_GC_EPSILON_EPSILONTHREADLOCALDATA_HPP
