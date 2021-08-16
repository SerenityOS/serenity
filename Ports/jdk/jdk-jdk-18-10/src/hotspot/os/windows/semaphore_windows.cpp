/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "semaphore_windows.hpp"
#include "utilities/debug.hpp"

#include <windows.h>
#include <errno.h>

WindowsSemaphore::WindowsSemaphore(uint value) {
  _semaphore = ::CreateSemaphore(NULL, value, LONG_MAX, NULL);

  guarantee(_semaphore != NULL, "CreateSemaphore failed with error code: %lu", GetLastError());
}

WindowsSemaphore::~WindowsSemaphore() {
  ::CloseHandle(_semaphore);
}

void WindowsSemaphore::signal(uint count) {
  if (count > 0) {
    BOOL ret = ::ReleaseSemaphore(_semaphore, count, NULL);

    assert(ret != 0, "ReleaseSemaphore failed with error code: %lu", GetLastError());
  }
}

void WindowsSemaphore::wait() {
  DWORD ret = ::WaitForSingleObject(_semaphore, INFINITE);
  assert(ret != WAIT_FAILED,   "WaitForSingleObject failed with error code: %lu", GetLastError());
  assert(ret == WAIT_OBJECT_0, "WaitForSingleObject failed with return value: %lu", ret);
}

bool WindowsSemaphore::trywait() {
  DWORD ret = ::WaitForSingleObject(_semaphore, 0);
  assert(ret != WAIT_FAILED,   "WaitForSingleObject failed with error code: %lu", GetLastError());
  return ret == WAIT_OBJECT_0;
}
