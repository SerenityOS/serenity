/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_PARALLEL_VMSTRUCTS_PARALLELGC_HPP
#define SHARE_GC_PARALLEL_VMSTRUCTS_PARALLELGC_HPP

#include "gc/parallel/mutableSpace.hpp"
#include "gc/parallel/parallelScavengeHeap.hpp"
#include "gc/parallel/psOldGen.hpp"
#include "gc/parallel/psVirtualspace.hpp"
#include "gc/parallel/psYoungGen.hpp"
#include "gc/parallel/vmStructs_parallelgc.hpp"

#define VM_STRUCTS_PARALLELGC(nonstatic_field, \
                              volatile_nonstatic_field, \
                              static_field) \
                                                                                                                                     \
  /**********************/                                                                                                           \
  /* Parallel GC fields */                                                                                                           \
  /**********************/                                                                                                           \
  nonstatic_field(PSVirtualSpace,              _alignment,                                    const size_t)                          \
  nonstatic_field(PSVirtualSpace,              _reserved_low_addr,                            char*)                                 \
  nonstatic_field(PSVirtualSpace,              _reserved_high_addr,                           char*)                                 \
  nonstatic_field(PSVirtualSpace,              _committed_low_addr,                           char*)                                 \
  nonstatic_field(PSVirtualSpace,              _committed_high_addr,                          char*)                                 \
                                                                                                                                     \
  nonstatic_field(MutableSpace,                _bottom,                                       HeapWord*)                             \
  nonstatic_field(MutableSpace,                _end,                                          HeapWord*)                             \
  volatile_nonstatic_field(MutableSpace,       _top,                                          HeapWord*)                             \
                                                                                                                                     \
  nonstatic_field(PSYoungGen,                  _reserved,                                     MemRegion)                             \
  nonstatic_field(PSYoungGen,                  _virtual_space,                                PSVirtualSpace*)                       \
  nonstatic_field(PSYoungGen,                  _eden_space,                                   MutableSpace*)                         \
  nonstatic_field(PSYoungGen,                  _from_space,                                   MutableSpace*)                         \
  nonstatic_field(PSYoungGen,                  _to_space,                                     MutableSpace*)                         \
  nonstatic_field(PSYoungGen,                  _min_gen_size,                                 const size_t)                          \
  nonstatic_field(PSYoungGen,                  _max_gen_size,                                 const size_t)                          \
                                                                                                                                     \
  nonstatic_field(PSOldGen,                    _reserved,                                     MemRegion)                             \
  nonstatic_field(PSOldGen,                    _virtual_space,                                PSVirtualSpace*)                       \
  nonstatic_field(PSOldGen,                    _object_space,                                 MutableSpace*)                         \
  nonstatic_field(PSOldGen,                    _min_gen_size,                                 const size_t)                          \
  nonstatic_field(PSOldGen,                    _max_gen_size,                                 const size_t)                          \
                                                                                                                                     \
                                                                                                                                     \
     static_field(ParallelScavengeHeap,        _young_gen,                                    PSYoungGen*)                           \
     static_field(ParallelScavengeHeap,        _old_gen,                                      PSOldGen*)                             \
                                                                                                                                     \

#define VM_TYPES_PARALLELGC(declare_type,                                 \
                            declare_toplevel_type,                        \
                            declare_integer_type)                         \
                                                                          \
                                                                          \
  /*****************************************/                             \
  /* Parallel GC - space, gen abstractions */                             \
  /*****************************************/                             \
           declare_type(ParallelScavengeHeap,         CollectedHeap)      \
                                                                          \
  declare_toplevel_type(PSVirtualSpace)                                   \
  declare_toplevel_type(MutableSpace)                                     \
  declare_toplevel_type(PSYoungGen)                                       \
  declare_toplevel_type(PSOldGen)                                         \
                                                                          \
  /*****************************/                                         \
  /* Parallel GC pointer types */                                         \
  /*****************************/                                         \
                                                                          \
  declare_toplevel_type(PSVirtualSpace*)                                  \
  declare_toplevel_type(MutableSpace*)                                    \
  declare_toplevel_type(PSYoungGen*)                                      \
  declare_toplevel_type(PSOldGen*)                                        \
  declare_toplevel_type(ParallelScavengeHeap*)

#define VM_INT_CONSTANTS_PARALLELGC(declare_constant,                     \
                                    declare_constant_with_value)

#endif // SHARE_GC_PARALLEL_VMSTRUCTS_PARALLELGC_HPP
