/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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
#include "memory/metaspace.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/thread.hpp"

frame JavaThread::pd_last_frame() {
  assert(has_last_Java_frame(), "must have last_Java_sp() when suspended");

  intptr_t* sp = last_Java_sp();
  address pc = _anchor.last_Java_pc();

  // Last_Java_pc ist not set, if we come here from compiled code.
  if (pc == NULL) {
    pc = (address) *(sp + 2);
  }

  return frame(sp, pc);
}

bool JavaThread::pd_get_top_frame_for_profiling(frame* fr_addr, void* ucontext, bool isInJava) {

  // If we have a last_Java_frame, then we should use it even if
  // isInJava == true.  It should be more reliable than ucontext info.
  if (has_last_Java_frame() && frame_anchor()->walkable()) {
    *fr_addr = pd_last_frame();
    return true;
  }

  // At this point, we don't have a last_Java_frame, so
  // we try to glean some information out of the ucontext
  // if we were running Java code when SIGPROF came in.
  if (isInJava) {
    ucontext_t* uc = (ucontext_t*) ucontext;
    frame ret_frame((intptr_t*)uc->uc_mcontext.regs->gpr[1/*REG_SP*/],
                     (address)uc->uc_mcontext.regs->nip);

    if (ret_frame.pc() == NULL) {
      // ucontext wasn't useful
      return false;
    }

    if (ret_frame.is_interpreted_frame()) {
      frame::ijava_state *istate = ret_frame.get_ijava_state();
      const Method *m = (const Method*)(istate->method);
      if (!Method::is_valid_method(m)) return false;
      if (!Metaspace::contains(m->constMethod())) return false;

      uint64_t reg_bcp = uc->uc_mcontext.regs->gpr[14/*R14_bcp*/];
      uint64_t istate_bcp = istate->bcp;
      uint64_t code_start = (uint64_t)(m->code_base());
      uint64_t code_end = (uint64_t)(m->code_base() + m->code_size());
      if (istate_bcp >= code_start && istate_bcp < code_end) {
        // we have a valid bcp, don't touch it, do nothing
      } else if (reg_bcp >= code_start && reg_bcp < code_end) {
        istate->bcp = reg_bcp;
      } else {
        return false;
      }
    }
    if (!ret_frame.safe_for_sender(this)) {
      // nothing else to try if the frame isn't good
      return false;
    }
    *fr_addr = ret_frame;
    return true;
  }
  // nothing else to try
  return false;
}

// Forte Analyzer AsyncGetCallTrace profiling support.
bool JavaThread::pd_get_top_frame_for_signal_handler(frame* fr_addr, void* ucontext, bool isInJava) {
  return pd_get_top_frame_for_profiling(fr_addr, ucontext, isInJava);
}

void JavaThread::cache_global_variables() { }
