/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Arm Limited. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "memory/resourceArea.hpp"
#include "prims/universalUpcallHandler.hpp"

#define __ _masm->

// 1. Create buffer according to layout
// 2. Load registers & stack args into buffer
// 3. Call upcall helper with upcall handler instance & buffer pointer (C++ ABI)
// 4. Load return value from buffer into foreign ABI registers
// 5. Return
address ProgrammableUpcallHandler::generate_upcall_stub(jobject rec, jobject jabi, jobject jlayout) {
  ResourceMark rm;
  const ABIDescriptor abi = ForeignGlobals::parse_abi_descriptor(jabi);
  const BufferLayout layout = ForeignGlobals::parse_buffer_layout(jlayout);

  CodeBuffer buffer("upcall_stub", 1024, upcall_stub_size);

  MacroAssembler* _masm = new MacroAssembler(&buffer);

  // stub code
  __ enter();

  // save pointer to JNI receiver handle into constant segment
  Address rec_adr = InternalAddress(__ address_constant((address)rec));

  assert(abi._stack_alignment_bytes % 16 == 0, "stack must be 16 byte aligned");

  __ sub(sp, sp, (int) align_up(layout.buffer_size, abi._stack_alignment_bytes));

  // TODO: This stub only uses registers which are caller-save in the
  //       standard C ABI. If this is called from a different ABI then
  //       we need to save registers here according to abi.is_volatile_reg.

  for (int i = 0; i < abi._integer_argument_registers.length(); i++) {
    Register reg = abi._integer_argument_registers.at(i);
    ssize_t offset = layout.arguments_integer + i * sizeof(uintptr_t);
    __ str(reg, Address(sp, offset));
  }

  for (int i = 0; i < abi._vector_argument_registers.length(); i++) {
    FloatRegister reg = abi._vector_argument_registers.at(i);
    ssize_t offset = layout.arguments_vector + i * float_reg_size;
    __ strq(reg, Address(sp, offset));
  }

  // Capture prev stack pointer (stack arguments base)
  __ add(rscratch1, rfp, 16);   // Skip saved FP and LR
  __ str(rscratch1, Address(sp, layout.stack_args));

  // Call upcall helper
  __ ldr(c_rarg0, rec_adr);
  __ mov(c_rarg1, sp);
  __ movptr(rscratch1, CAST_FROM_FN_PTR(uint64_t, ProgrammableUpcallHandler::attach_thread_and_do_upcall));
  __ blr(rscratch1);

  for (int i = 0; i < abi._integer_return_registers.length(); i++) {
    ssize_t offs = layout.returns_integer + i * sizeof(uintptr_t);
    __ ldr(abi._integer_return_registers.at(i), Address(sp, offs));
  }

  for (int i = 0; i < abi._vector_return_registers.length(); i++) {
    FloatRegister reg = abi._vector_return_registers.at(i);
    ssize_t offs = layout.returns_vector + i * float_reg_size;
    __ ldrq(reg, Address(sp, offs));
  }

  __ leave();
  __ ret(lr);

  __ flush();

  BufferBlob* blob = BufferBlob::create("upcall_stub", &buffer);

  return blob->code_begin();
}

address ProgrammableUpcallHandler::generate_optimized_upcall_stub(jobject mh, Method* entry, jobject jabi, jobject jconv) {
  ShouldNotCallThis();
  return nullptr;
}

bool ProgrammableUpcallHandler::supports_optimized_upcalls() {
  return false;
}
