/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/z/zThread.inline.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"

THREAD_LOCAL bool      ZThread::_initialized;
THREAD_LOCAL uintptr_t ZThread::_id;
THREAD_LOCAL bool      ZThread::_is_vm;
THREAD_LOCAL bool      ZThread::_is_java;
THREAD_LOCAL bool      ZThread::_is_worker;
THREAD_LOCAL uint      ZThread::_worker_id;

void ZThread::initialize() {
  assert(!_initialized, "Already initialized");
  const Thread* const thread = Thread::current();
  _initialized = true;
  _id = (uintptr_t)thread;
  _is_vm = thread->is_VM_thread();
  _is_java = thread->is_Java_thread();
  _is_worker = false;
  _worker_id = (uint)-1;
}

const char* ZThread::name() {
  const Thread* const thread = Thread::current();
  if (thread->is_Named_thread()) {
    const NamedThread* const named = (const NamedThread*)thread;
    return named->name();
  } else if (thread->is_Java_thread()) {
    return "Java";
  }

  return "Unknown";
}

void ZThread::set_worker() {
  ensure_initialized();
  _is_worker = true;
}

bool ZThread::has_worker_id() {
  return _initialized &&
         _is_worker &&
         _worker_id != (uint)-1;
}

void ZThread::set_worker_id(uint worker_id) {
  ensure_initialized();
  assert(!has_worker_id(), "Worker id already initialized");
  _worker_id = worker_id;
}

void ZThread::clear_worker_id() {
  assert(has_worker_id(), "Worker id not initialized");
  _worker_id = (uint)-1;
}
