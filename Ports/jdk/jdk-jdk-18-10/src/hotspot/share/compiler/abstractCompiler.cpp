//
// Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//

#include "precompiled.hpp"
#include "compiler/abstractCompiler.hpp"
#include "compiler/compileBroker.hpp"
#include "runtime/mutexLocker.hpp"

bool AbstractCompiler::should_perform_init() {
  if (_compiler_state != initialized) {
    MonitorLocker only_one(CompileThread_lock);

    if (_compiler_state == uninitialized) {
      _compiler_state = initializing;
      return true;
    } else {
      while (_compiler_state == initializing) {
        only_one.wait();
      }
    }
  }
  return false;
}

bool AbstractCompiler::should_perform_shutdown() {
  // Since this method can be called by multiple threads, the lock ensures atomicity of
  // decrementing '_num_compiler_threads' and the following operations.
  MutexLocker only_one(CompileThread_lock);
  _num_compiler_threads--;
  assert (CompileBroker::is_compilation_disabled_forever(), "Must be set, otherwise thread waits forever");

  // Only the last thread will perform shutdown operations
  if (_num_compiler_threads == 0) {
    return true;
  }
  return false;
}

void AbstractCompiler::set_state(int state) {
  // Ensure that state is only set by one thread at a time
  MutexLocker only_one(CompileThread_lock);
  _compiler_state =  state;
  CompileThread_lock->notify_all();
}
