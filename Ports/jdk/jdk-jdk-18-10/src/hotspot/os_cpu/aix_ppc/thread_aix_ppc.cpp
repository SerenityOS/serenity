/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2014 SAP SE. All rights reserved.
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
#include "runtime/frame.inline.hpp"
#include "runtime/thread.hpp"

frame JavaThread::pd_last_frame() {
  assert(has_last_Java_frame(), "must have last_Java_sp() when suspended");

  intptr_t* sp = last_Java_sp();
  address pc = _anchor.last_Java_pc();

  // Last_Java_pc ist not set, if we come here from compiled code.
  if (pc == NULL)
    pc =  (address) *(sp + 2);

  return frame(sp, pc);
}

bool JavaThread::pd_get_top_frame_for_profiling(frame* fr_addr, void* ucontext, bool isInJava) {
  ucontext_t* uc = (ucontext_t*) ucontext;
  *fr_addr = frame((intptr_t*)uc->uc_mcontext.jmp_context.gpr[1/*REG_SP*/],
                   (address)uc->uc_mcontext.jmp_context.iar);
  return true;
}

// Forte Analyzer AsyncGetCallTrace profiling support is not implemented on Aix/PPC.
bool JavaThread::pd_get_top_frame_for_signal_handler(frame* fr_addr, void* ucontext, bool isInJava) {
  Unimplemented();
  return false;
}

void JavaThread::cache_global_variables() { }
