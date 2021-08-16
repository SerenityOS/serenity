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
#include "code/codeBlob.hpp"
#include "memory/resourceArea.hpp"
#include "prims/universalNativeInvoker.hpp"

#define __ _masm->

void ProgrammableInvoker::Generator::generate() {
  __ enter();

  // Name registers used in the stub code. These are all caller-save so
  // may be clobbered by the call to the native function. Avoid using
  // rscratch1 here as it's r8 which is the indirect result register in
  // the standard ABI.
  Register Rctx = r10, Rstack_size = r11;
  Register Rwords = r12, Rtmp = r13;
  Register Rsrc_ptr = r14, Rdst_ptr = r15;

  assert_different_registers(Rctx, Rstack_size, rscratch1, rscratch2);

  // TODO: if the callee is not using the standard C ABI then we need to
  //       preserve more registers here.

  __ block_comment("init_and_alloc_stack");

  __ mov(Rctx, c_rarg0);
  __ str(Rctx, Address(__ pre(sp, -2 * wordSize)));

  assert(_abi->_stack_alignment_bytes % 16 == 0, "stack must be 16 byte aligned");

  __ block_comment("allocate_stack");
  __ ldr(Rstack_size, Address(Rctx, (int) _layout->stack_args_bytes));
  __ add(rscratch2, Rstack_size, _abi->_stack_alignment_bytes - 1);
  __ andr(rscratch2, rscratch2, -_abi->_stack_alignment_bytes);
  __ sub(sp, sp, rscratch2);

  __ block_comment("load_arguments");

  __ ldr(Rsrc_ptr, Address(Rctx, (int) _layout->stack_args));
  __ lsr(Rwords, Rstack_size, LogBytesPerWord);
  __ mov(Rdst_ptr, sp);

  Label Ldone, Lnext;
  __ bind(Lnext);
  __ cbz(Rwords, Ldone);
  __ ldr(Rtmp, __ post(Rsrc_ptr, wordSize));
  __ str(Rtmp, __ post(Rdst_ptr, wordSize));
  __ sub(Rwords, Rwords, 1);
  __ b(Lnext);
  __ bind(Ldone);

  for (int i = 0; i < _abi->_vector_argument_registers.length(); i++) {
    ssize_t offs = _layout->arguments_vector + i * float_reg_size;
    __ ldrq(_abi->_vector_argument_registers.at(i), Address(Rctx, offs));
  }

  for (int i = 0; i < _abi->_integer_argument_registers.length(); i++) {
    ssize_t offs = _layout->arguments_integer + i * sizeof(uintptr_t);
    __ ldr(_abi->_integer_argument_registers.at(i), Address(Rctx, offs));
  }

  assert(_abi->_shadow_space_bytes == 0, "shadow space not supported on AArch64");

  // call target function
  __ block_comment("call target function");
  __ ldr(rscratch2, Address(Rctx, (int) _layout->arguments_next_pc));
  __ blr(rscratch2);

  __ ldr(Rctx, Address(rfp, -2 * wordSize));   // Might have clobbered Rctx

  __ block_comment("store_registers");

  for (int i = 0; i < _abi->_integer_return_registers.length(); i++) {
    ssize_t offs = _layout->returns_integer + i * sizeof(uintptr_t);
    __ str(_abi->_integer_return_registers.at(i), Address(Rctx, offs));
  }

  for (int i = 0; i < _abi->_vector_return_registers.length(); i++) {
    ssize_t offs = _layout->returns_vector + i * float_reg_size;
    __ strq(_abi->_vector_return_registers.at(i), Address(Rctx, offs));
  }

  __ leave();
  __ ret(lr);

  __ flush();
}

address ProgrammableInvoker::generate_adapter(jobject jabi, jobject jlayout) {
  ResourceMark rm;
  const ABIDescriptor abi = ForeignGlobals::parse_abi_descriptor(jabi);
  const BufferLayout layout = ForeignGlobals::parse_buffer_layout(jlayout);

  BufferBlob* _invoke_native_blob = BufferBlob::create("invoke_native_blob", native_invoker_size);

  CodeBuffer code2(_invoke_native_blob);
  ProgrammableInvoker::Generator g2(&code2, &abi, &layout);
  g2.generate();
  code2.log_section_sizes("InvokeNativeBlob");

  return _invoke_native_blob->code_begin();
}
