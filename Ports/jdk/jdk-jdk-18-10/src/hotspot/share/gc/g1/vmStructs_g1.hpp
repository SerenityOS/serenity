/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_VMSTRUCTS_G1_HPP
#define SHARE_GC_G1_VMSTRUCTS_G1_HPP

#include "gc/g1/g1CollectedHeap.hpp"
#include "gc/g1/heapRegion.hpp"
#include "gc/g1/heapRegionManager.hpp"
#include "utilities/macros.hpp"

#define VM_STRUCTS_G1GC(nonstatic_field,                                      \
                        volatile_nonstatic_field,                             \
                        static_field)                                         \
                                                                              \
  static_field(HeapRegion, GrainBytes,        size_t)                         \
  static_field(HeapRegion, LogOfHRGrainBytes, int)                            \
                                                                              \
  nonstatic_field(HeapRegion, _type,           HeapRegionType)                \
  nonstatic_field(HeapRegion, _bottom,         HeapWord* const)               \
  nonstatic_field(HeapRegion, _top,            HeapWord* volatile)            \
  nonstatic_field(HeapRegion, _end,            HeapWord* const)               \
  nonstatic_field(HeapRegion, _compaction_top, HeapWord*)                     \
                                                                              \
  nonstatic_field(HeapRegionType, _tag,       HeapRegionType::Tag volatile)   \
                                                                              \
                                                                              \
  nonstatic_field(G1HeapRegionTable, _base,             address)              \
  nonstatic_field(G1HeapRegionTable, _length,           size_t)               \
  nonstatic_field(G1HeapRegionTable, _biased_base,      address)              \
  nonstatic_field(G1HeapRegionTable, _bias,             size_t)               \
  nonstatic_field(G1HeapRegionTable, _shift_by,         uint)                 \
                                                                              \
  nonstatic_field(HeapRegionManager, _regions,          G1HeapRegionTable)    \
                                                                              \
  volatile_nonstatic_field(G1CollectedHeap, _summary_bytes_used, size_t)      \
  nonstatic_field(G1CollectedHeap, _hrm,                HeapRegionManager)    \
  nonstatic_field(G1CollectedHeap, _monitoring_support, G1MonitoringSupport*) \
  nonstatic_field(G1CollectedHeap, _old_set,            HeapRegionSetBase)    \
  nonstatic_field(G1CollectedHeap, _archive_set,        HeapRegionSetBase)    \
  nonstatic_field(G1CollectedHeap, _humongous_set,      HeapRegionSetBase)    \
                                                                              \
  nonstatic_field(G1MonitoringSupport, _eden_space_committed,     size_t)     \
  nonstatic_field(G1MonitoringSupport, _eden_space_used,          size_t)     \
  nonstatic_field(G1MonitoringSupport, _survivor_space_committed, size_t)     \
  nonstatic_field(G1MonitoringSupport, _survivor_space_used,      size_t)     \
  nonstatic_field(G1MonitoringSupport, _old_gen_committed,        size_t)     \
  nonstatic_field(G1MonitoringSupport, _old_gen_used,             size_t)     \
                                                                              \
  nonstatic_field(HeapRegionSetBase,   _length,         uint)                 \
                                                                              \
  nonstatic_field(SATBMarkQueue,       _active,         bool)                 \
  nonstatic_field(PtrQueue,            _buf,            void**)               \
  nonstatic_field(PtrQueue,            _index,          size_t)

#define VM_INT_CONSTANTS_G1GC(declare_constant, declare_constant_with_value)  \
  declare_constant(HeapRegionType::FreeTag)                                   \
  declare_constant(HeapRegionType::YoungMask)                                 \
  declare_constant(HeapRegionType::EdenTag)                                   \
  declare_constant(HeapRegionType::SurvTag)                                   \
  declare_constant(HeapRegionType::HumongousMask)                             \
  declare_constant(HeapRegionType::PinnedMask)                                \
  declare_constant(HeapRegionType::ArchiveMask)                               \
  declare_constant(HeapRegionType::StartsHumongousTag)                        \
  declare_constant(HeapRegionType::ContinuesHumongousTag)                     \
  declare_constant(HeapRegionType::OldMask)                                   \
  declare_constant(BarrierSet::G1BarrierSet)                                  \
  declare_constant(G1CardTable::g1_young_gen)

#define VM_TYPES_G1GC(declare_type,                                           \
                      declare_toplevel_type,                                  \
                      declare_integer_type)                                   \
                                                                              \
  declare_toplevel_type(G1HeapRegionTable)                                    \
                                                                              \
  declare_type(G1CollectedHeap, CollectedHeap)                                \
                                                                              \
  declare_toplevel_type(HeapRegion)                                           \
  declare_toplevel_type(HeapRegionManager)                                    \
  declare_toplevel_type(HeapRegionSetBase)                                    \
  declare_toplevel_type(G1MonitoringSupport)                                  \
  declare_toplevel_type(PtrQueue)                                             \
  declare_toplevel_type(HeapRegionType)                                       \
  declare_toplevel_type(SATBMarkQueue)                                        \
  declare_toplevel_type(G1DirtyCardQueue)                                     \
                                                                              \
  declare_toplevel_type(G1CollectedHeap*)                                     \
  declare_toplevel_type(HeapRegion*)                                          \
  declare_toplevel_type(G1MonitoringSupport*)                                 \
                                                                              \
  declare_integer_type(HeapRegionType::Tag volatile)

#endif // SHARE_GC_G1_VMSTRUCTS_G1_HPP
