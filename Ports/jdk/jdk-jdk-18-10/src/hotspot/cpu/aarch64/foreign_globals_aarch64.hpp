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

#ifndef CPU_AARCH64_VM_FOREIGN_GLOBALS_AARCH64_HPP
#define CPU_AARCH64_VM_FOREIGN_GLOBALS_AARCH64_HPP

#include "asm/macroAssembler.hpp"
#include "utilities/growableArray.hpp"

constexpr size_t float_reg_size = 16; // bytes

struct ABIDescriptor {
  GrowableArray<Register> _integer_argument_registers;
  GrowableArray<Register> _integer_return_registers;
  GrowableArray<FloatRegister> _vector_argument_registers;
  GrowableArray<FloatRegister> _vector_return_registers;

  GrowableArray<Register> _integer_additional_volatile_registers;
  GrowableArray<FloatRegister> _vector_additional_volatile_registers;

  int32_t _stack_alignment_bytes;
  int32_t _shadow_space_bytes;

  bool is_volatile_reg(Register reg) const;
  bool is_volatile_reg(FloatRegister reg) const;
};

struct BufferLayout {
  size_t stack_args_bytes;
  size_t stack_args;
  size_t arguments_vector;
  size_t arguments_integer;
  size_t arguments_next_pc;
  size_t returns_vector;
  size_t returns_integer;
  size_t buffer_size;
};

#endif // CPU_AARCH64_VM_FOREIGN_GLOBALS_AARCH64_HPP
