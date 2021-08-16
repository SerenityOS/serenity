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

#ifndef SHARE_GC_Z_ZTHREAD_INLINE_HPP
#define SHARE_GC_Z_ZTHREAD_INLINE_HPP

#include "gc/z/zThread.hpp"

#include "utilities/debug.hpp"

inline void ZThread::ensure_initialized() {
  if (!_initialized) {
    initialize();
  }
}

inline uintptr_t ZThread::id() {
  ensure_initialized();
  return _id;
}

inline bool ZThread::is_vm() {
  ensure_initialized();
  return _is_vm;
}

inline bool ZThread::is_java() {
  ensure_initialized();
  return _is_java;
}

inline bool ZThread::is_worker() {
  ensure_initialized();
  return _is_worker;
}

inline uint ZThread::worker_id() {
  assert(has_worker_id(), "Worker id not initialized");
  return _worker_id;
}

#endif // SHARE_GC_Z_ZTHREAD_INLINE_HPP
