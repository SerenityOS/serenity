/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "code/debugInfoRec.hpp"
#include "code/nmethod.hpp"
#include "code/pcDesc.hpp"
#include "jfr/periodic/sampling/jfrCallTrace.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "oops/method.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/registerMap.hpp"
#include "runtime/thread.inline.hpp"

bool JfrGetCallTrace::find_top_frame(frame& top_frame, Method** method, frame& first_frame) {
  assert(top_frame.cb() != NULL, "invariant");
  RegisterMap map(_thread, false, false);
  frame candidate = top_frame;
  for (u4 i = 0; i < MAX_STACK_DEPTH * 2; ++i) {
    if (candidate.is_entry_frame()) {
      JavaCallWrapper *jcw = candidate.entry_frame_call_wrapper_if_safe(_thread);
      if (jcw == NULL || jcw->is_first_frame()) {
        return false;
      }
    }

    if (candidate.is_interpreted_frame()) {
      JavaThreadState state = _thread->thread_state();
      const bool known_valid = (state == _thread_in_native || state == _thread_in_vm || state == _thread_blocked);
      if (known_valid || candidate.is_interpreted_frame_valid(_thread)) {
        Method* im = candidate.interpreter_frame_method();
        if (known_valid && !Method::is_valid_method(im)) {
          return false;
        }
        *method = im;
        first_frame = candidate;
        return true;
      }
    }

    if (candidate.cb()->is_nmethod()) {
      // first check to make sure that we have a sane stack,
      // the PC is actually inside the code part of the codeBlob,
      // and we are past is_frame_complete_at (stack has been setup)
      if (!candidate.safe_for_sender(_thread)) {
        return false;
      }
      nmethod* nm = (nmethod*)candidate.cb();
      *method = nm->method();

      if (_in_java) {
        PcDesc* pc_desc = nm->pc_desc_near(candidate.pc() + 1);
        if (pc_desc == NULL || pc_desc->scope_decode_offset() == DebugInformationRecorder::serialized_null) {
          return false;
        }
        candidate.set_pc(pc_desc->real_pc(nm));
        assert(nm->pc_desc_at(candidate.pc()) != NULL, "invalid pc");
      }
      first_frame = candidate;
      return true;
    }

    if (!candidate.safe_for_sender(_thread) ||
      candidate.is_stub_frame() ||
      candidate.cb()->frame_size() <= 0) {
      return false;
    }

    candidate = candidate.sender(&map);
    if (candidate.cb() == NULL) {
      return false;
    }
  }
  return false;
}

bool JfrGetCallTrace::get_topframe(void* ucontext, frame& topframe) {
  if (!_thread->pd_get_top_frame_for_profiling(&topframe, ucontext, _in_java)) {
    return false;
  }

  if (topframe.cb() == NULL) {
    return false;
  }

  frame first_java_frame;
  Method* method = NULL;
  if (find_top_frame(topframe, &method, first_java_frame)) {
    if (method == NULL) {
      return false;
    }
    topframe = first_java_frame;
    return true;
  }
  return false;
}
