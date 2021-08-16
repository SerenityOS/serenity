/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "runtime/threadLocalStorage.hpp"
#include <windows.h>

static DWORD _thread_key;
static bool _initialized = false;


void ThreadLocalStorage::init() {
  assert(!_initialized, "initializing TLS more than once!");
  _thread_key = TlsAlloc();
  // If this assert fails we will get a recursive assertion failure
  // and not see the actual error message or get a hs_err file
  assert(_thread_key != TLS_OUT_OF_INDEXES, "TlsAlloc failed: out of indices");
  _initialized = true;
}

bool ThreadLocalStorage::is_initialized() {
  return _initialized;
}

Thread* ThreadLocalStorage::thread() {
  // If this assert fails we will get a recursive assertion failure
  // and not see the actual error message or get a hs_err file.
  // Which most likely indicates we have taken an error path early in
  // the initialization process, which is using Thread::current without
  // checking TLS is initialized - see java.cpp vm_exit
  assert(_initialized, "TLS not initialized yet!");
  Thread* current = (Thread*) TlsGetValue(_thread_key);
  assert(current != 0 || GetLastError() == ERROR_SUCCESS,
         "TlsGetValue failed with error code: %lu", GetLastError());
  return current;
}

void ThreadLocalStorage::set_thread(Thread* current) {
  assert(_initialized, "TLS not initialized yet!");
  BOOL res = TlsSetValue(_thread_key, current);
  assert(res, "TlsSetValue failed with error code: %lu", GetLastError());
}
