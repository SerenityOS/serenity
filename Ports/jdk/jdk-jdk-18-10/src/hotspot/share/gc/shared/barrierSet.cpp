/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetAssembler.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

BarrierSet* BarrierSet::_barrier_set = NULL;

void BarrierSet::set_barrier_set(BarrierSet* barrier_set) {
  assert(_barrier_set == NULL, "Already initialized");
  _barrier_set = barrier_set;

  // Notify barrier set of the current (main) thread.  Normally the
  // Thread constructor deals with this, but the main thread is
  // created before we get here.  Verify it isn't yet on the thread
  // list, else we'd also need to call BarrierSet::on_thread_attach.
  // This is the only thread that can exist at this point; the Thread
  // constructor objects to other threads being created before the
  // barrier set is available.
  assert(Thread::current()->is_Java_thread(),
         "Expected main thread to be a JavaThread");
  assert(!JavaThread::current()->on_thread_list(),
         "Main thread already on thread list.");
  _barrier_set->on_thread_create(Thread::current());
}

// Called from init.cpp
void gc_barrier_stubs_init() {
  BarrierSet* bs = BarrierSet::barrier_set();
#ifndef ZERO
  BarrierSetAssembler* bs_assembler = bs->barrier_set_assembler();
  bs_assembler->barrier_stubs_init();
#endif
}
