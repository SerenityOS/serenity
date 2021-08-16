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
#include "runtime/jniHandles.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/foreign_globals.hpp"
#include "prims/foreign_globals.inline.hpp"

bool ABIDescriptor::is_volatile_reg(Register reg) const {
  return _integer_argument_registers.contains(reg)
    || _integer_additional_volatile_registers.contains(reg);
}

bool ABIDescriptor::is_volatile_reg(FloatRegister reg) const {
    return _vector_argument_registers.contains(reg)
        || _vector_additional_volatile_registers.contains(reg);
}

#define INTEGER_TYPE 0
#define VECTOR_TYPE 1

const ABIDescriptor ForeignGlobals::parse_abi_descriptor_impl(jobject jabi) const {
  oop abi_oop = JNIHandles::resolve_non_null(jabi);
  ABIDescriptor abi;
  const Register (*to_Register)(int) = as_Register;

  objArrayOop inputStorage = cast<objArrayOop>(abi_oop->obj_field(ABI.inputStorage_offset));
  loadArray(inputStorage, INTEGER_TYPE, abi._integer_argument_registers, to_Register);
  loadArray(inputStorage, VECTOR_TYPE, abi._vector_argument_registers, as_FloatRegister);

  objArrayOop outputStorage = cast<objArrayOop>(abi_oop->obj_field(ABI.outputStorage_offset));
  loadArray(outputStorage, INTEGER_TYPE, abi._integer_return_registers, to_Register);
  loadArray(outputStorage, VECTOR_TYPE, abi._vector_return_registers, as_FloatRegister);

  objArrayOop volatileStorage = cast<objArrayOop>(abi_oop->obj_field(ABI.volatileStorage_offset));
  loadArray(volatileStorage, INTEGER_TYPE, abi._integer_additional_volatile_registers, to_Register);
  loadArray(volatileStorage, VECTOR_TYPE, abi._vector_additional_volatile_registers, as_FloatRegister);

  abi._stack_alignment_bytes = abi_oop->int_field(ABI.stackAlignment_offset);
  abi._shadow_space_bytes = abi_oop->int_field(ABI.shadowSpace_offset);

  return abi;
}

const BufferLayout ForeignGlobals::parse_buffer_layout_impl(jobject jlayout) const {
  oop layout_oop = JNIHandles::resolve_non_null(jlayout);
  BufferLayout layout;

  layout.stack_args_bytes = layout_oop->long_field(BL.stack_args_bytes_offset);
  layout.stack_args = layout_oop->long_field(BL.stack_args_offset);
  layout.arguments_next_pc = layout_oop->long_field(BL.arguments_next_pc_offset);

  typeArrayOop input_offsets = cast<typeArrayOop>(layout_oop->obj_field(BL.input_type_offsets_offset));
  layout.arguments_integer = (size_t) input_offsets->long_at(INTEGER_TYPE);
  layout.arguments_vector = (size_t) input_offsets->long_at(VECTOR_TYPE);

  typeArrayOop output_offsets = cast<typeArrayOop>(layout_oop->obj_field(BL.output_type_offsets_offset));
  layout.returns_integer = (size_t) output_offsets->long_at(INTEGER_TYPE);
  layout.returns_vector = (size_t) output_offsets->long_at(VECTOR_TYPE);

  layout.buffer_size = layout_oop->long_field(BL.size_offset);

  return layout;
}

const CallRegs ForeignGlobals::parse_call_regs_impl(jobject jconv) const {
  ShouldNotCallThis();
  return {};
}
