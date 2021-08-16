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

#include "runtime/threadLocalStorage.hpp"
#include <pthread.h>

static pthread_key_t _thread_key;
static bool _initialized = false;

// Restore the thread pointer if the destructor is called. This is in case
// someone from JNI code sets up a destructor with pthread_key_create to run
// detachCurrentThread on thread death. Unless we restore the thread pointer we
// will hang or crash. When detachCurrentThread is called the key will be set
// to null and we will not be called again. If detachCurrentThread is never
// called we could loop forever depending on the pthread implementation.
extern "C" void restore_thread_pointer(void* p) {
  ThreadLocalStorage::set_thread((Thread*) p);
}

void ThreadLocalStorage::init() {
  assert(!_initialized, "initializing TLS more than once!");
  int rslt = pthread_key_create(&_thread_key, restore_thread_pointer);
  // If this assert fails we will get a recursive assertion failure
  // and not see the actual error message or get a hs_err file
  assert_status(rslt == 0, rslt, "pthread_key_create");
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
  return (Thread*) pthread_getspecific(_thread_key); // may be NULL
}

void ThreadLocalStorage::set_thread(Thread* current) {
  assert(_initialized, "TLS not initialized yet!");
  int rslt = pthread_setspecific(_thread_key, current);
  assert_status(rslt == 0, rslt, "pthread_setspecific");
}
