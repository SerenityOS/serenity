/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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
#include "macroAssembler_ppc.inline.hpp"
#include "opto/compile.hpp"
#include "opto/node.hpp"
#include "opto/output.hpp"
#include "runtime/sharedRuntime.hpp"

#define __ masm.
void C2SafepointPollStubTable::emit_stub_impl(MacroAssembler& masm, C2SafepointPollStub* entry) const {
  assert(SharedRuntime::polling_page_return_handler_blob() != NULL,
         "polling page return stub not created yet");
  address stub = SharedRuntime::polling_page_return_handler_blob()->entry_point();

  __ bind(entry->_stub_label);
  // Using pc relative address computation.
  {
    Label next_pc;
    __ bl(next_pc);
    __ bind(next_pc);
  }
  int current_offset = __ offset();
  // Code size should not depend on offset: see _stub_size computation in output.cpp
  __ load_const32(R12, entry->_safepoint_offset - current_offset);
  __ mflr(R0);
  __ add(R12, R12, R0);
  __ std(R12, in_bytes(JavaThread::saved_exception_pc_offset()), R16_thread);

  __ add_const_optimized(R0, R29_TOC, MacroAssembler::offset_to_global_toc(stub));
  __ mtctr(R0);
  __ bctr();
}
#undef __
