/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SERIAL_VMSTRUCTS_SERIAL_HPP
#define SHARE_GC_SERIAL_VMSTRUCTS_SERIAL_HPP

#include "gc/serial/serialHeap.hpp"
#include "gc/serial/tenuredGeneration.hpp"

#define VM_STRUCTS_SERIALGC(nonstatic_field,                                  \
                            volatile_nonstatic_field,                         \
                            static_field)                                     \
  nonstatic_field(TenuredGeneration, _min_heap_delta_bytes, size_t)           \
  nonstatic_field(TenuredGeneration, _the_space,            ContiguousSpace*) \
                                                                              \
  nonstatic_field(DefNewGeneration,  _old_gen,              Generation*)      \
  nonstatic_field(DefNewGeneration,  _tenuring_threshold,   uint)             \
  nonstatic_field(DefNewGeneration,  _age_table,            AgeTable)         \
  nonstatic_field(DefNewGeneration,  _eden_space,           ContiguousSpace*) \
  nonstatic_field(DefNewGeneration,  _from_space,           ContiguousSpace*) \
  nonstatic_field(DefNewGeneration,  _to_space,             ContiguousSpace*)

#define VM_TYPES_SERIALGC(declare_type,                                       \
                          declare_toplevel_type,                              \
                          declare_integer_type)                               \
  declare_type(SerialHeap,                   GenCollectedHeap)                \
  declare_type(TenuredGeneration,            CardGeneration)                  \
  declare_type(TenuredSpace,                 OffsetTableContigSpace)          \
                                                                              \
  declare_type(DefNewGeneration,             Generation)                      \
                                                                              \
  declare_toplevel_type(TenuredGeneration*)

#define VM_INT_CONSTANTS_SERIALGC(declare_constant,                           \
                                  declare_constant_with_value)

#endif // SHARE_GC_SERIAL_VMSTRUCTS_SERIAL_HPP
