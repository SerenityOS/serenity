/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_VMSTRUCTS_GC_HPP
#define SHARE_GC_SHARED_VMSTRUCTS_GC_HPP

#include "gc/shared/ageTable.hpp"
#include "gc/shared/cardGeneration.hpp"
#include "gc/shared/cardTable.hpp"
#include "gc/shared/cardTableRS.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/genCollectedHeap.hpp"
#include "gc/shared/generation.hpp"
#include "gc/shared/generationSpec.hpp"
#include "gc/shared/oopStorage.hpp"
#include "gc/shared/space.hpp"
#if INCLUDE_EPSILONGC
#include "gc/epsilon/vmStructs_epsilon.hpp"
#endif
#if INCLUDE_G1GC
#include "gc/g1/vmStructs_g1.hpp"
#endif
#if INCLUDE_PARALLELGC
#include "gc/parallel/vmStructs_parallelgc.hpp"
#endif
#if INCLUDE_SERIALGC
#include "gc/serial/defNewGeneration.hpp"
#include "gc/serial/vmStructs_serial.hpp"
#endif
#if INCLUDE_SHENANDOAHGC
#include "gc/shenandoah/vmStructs_shenandoah.hpp"
#endif
#if INCLUDE_ZGC
#include "gc/z/vmStructs_z.hpp"
#endif

#define VM_STRUCTS_GC(nonstatic_field,                                                                                               \
                      volatile_nonstatic_field,                                                                                      \
                      static_field,                                                                                                  \
                      unchecked_nonstatic_field)                                                                                     \
  EPSILONGC_ONLY(VM_STRUCTS_EPSILONGC(nonstatic_field,                                                                               \
                                      volatile_nonstatic_field,                                                                      \
                                      static_field))                                                                                 \
  G1GC_ONLY(VM_STRUCTS_G1GC(nonstatic_field,                                                                                         \
                            volatile_nonstatic_field,                                                                                \
                            static_field))                                                                                           \
  PARALLELGC_ONLY(VM_STRUCTS_PARALLELGC(nonstatic_field,                                                                             \
                                        volatile_nonstatic_field,                                                                    \
                                        static_field))                                                                               \
  SERIALGC_ONLY(VM_STRUCTS_SERIALGC(nonstatic_field,                                                                                 \
                                    volatile_nonstatic_field,                                                                        \
                                    static_field))                                                                                   \
  SHENANDOAHGC_ONLY(VM_STRUCTS_SHENANDOAH(nonstatic_field,                                                                           \
                               volatile_nonstatic_field,                                                                             \
                               static_field))                                                                                        \
  ZGC_ONLY(VM_STRUCTS_ZGC(nonstatic_field,                                                                                           \
                          volatile_nonstatic_field,                                                                                  \
                          static_field))                                                                                             \
                                                                                                                                     \
  /**********************************************************************************/                                               \
  /* Generation and Space hierarchies                                               */                                               \
  /**********************************************************************************/                                               \
                                                                                                                                     \
  unchecked_nonstatic_field(AgeTable,          sizes,                                         sizeof(AgeTable::sizes))               \
                                                                                                                                     \
  nonstatic_field(BarrierSet,                  _fake_rtti,                                    BarrierSet::FakeRtti)                  \
                                                                                                                                     \
  nonstatic_field(BarrierSet::FakeRtti,        _concrete_tag,                                 BarrierSet::Name)                      \
                                                                                                                                     \
  nonstatic_field(BlockOffsetTable,            _bottom,                                       HeapWord*)                             \
  nonstatic_field(BlockOffsetTable,            _end,                                          HeapWord*)                             \
                                                                                                                                     \
  nonstatic_field(BlockOffsetSharedArray,      _reserved,                                     MemRegion)                             \
  nonstatic_field(BlockOffsetSharedArray,      _end,                                          HeapWord*)                             \
  nonstatic_field(BlockOffsetSharedArray,      _vs,                                           VirtualSpace)                          \
  nonstatic_field(BlockOffsetSharedArray,      _offset_array,                                 u_char*)                               \
                                                                                                                                     \
  nonstatic_field(BlockOffsetArray,            _array,                                        BlockOffsetSharedArray*)               \
  nonstatic_field(BlockOffsetArray,            _sp,                                           Space*)                                \
  nonstatic_field(BlockOffsetArrayContigSpace, _next_offset_threshold,                        HeapWord*)                             \
  nonstatic_field(BlockOffsetArrayContigSpace, _next_offset_index,                            size_t)                                \
                                                                                                                                     \
  nonstatic_field(CardGeneration,              _rs,                                           CardTableRS*)                          \
  nonstatic_field(CardGeneration,              _bts,                                          BlockOffsetSharedArray*)               \
  nonstatic_field(CardGeneration,              _shrink_factor,                                size_t)                                \
  nonstatic_field(CardGeneration,              _capacity_at_prologue,                         size_t)                                \
  nonstatic_field(CardGeneration,              _used_at_prologue,                             size_t)                                \
                                                                                                                                     \
  nonstatic_field(CardTable,                   _whole_heap,                                   const MemRegion)                       \
  nonstatic_field(CardTable,                   _guard_index,                                  const size_t)                          \
  nonstatic_field(CardTable,                   _last_valid_index,                             const size_t)                          \
  nonstatic_field(CardTable,                   _page_size,                                    const size_t)                          \
  nonstatic_field(CardTable,                   _byte_map_size,                                const size_t)                          \
  nonstatic_field(CardTable,                   _byte_map,                                     CardTable::CardValue*)                                \
  nonstatic_field(CardTable,                   _cur_covered_regions,                          int)                                   \
  nonstatic_field(CardTable,                   _covered,                                      MemRegion*)                            \
  nonstatic_field(CardTable,                   _committed,                                    MemRegion*)                            \
  nonstatic_field(CardTable,                   _guard_region,                                 MemRegion)                             \
  nonstatic_field(CardTable,                   _byte_map_base,                                CardTable::CardValue*)                                \
  nonstatic_field(CardTableBarrierSet,         _defer_initial_card_mark,                      bool)                                  \
  nonstatic_field(CardTableBarrierSet,         _card_table,                                   CardTable*)                            \
                                                                                                                                     \
  nonstatic_field(CollectedHeap,               _reserved,                                     MemRegion)                             \
  nonstatic_field(CollectedHeap,               _is_gc_active,                                 bool)                                  \
  nonstatic_field(CollectedHeap,               _total_collections,                            unsigned int)                          \
                                                                                                                                     \
  nonstatic_field(CompactibleSpace,            _compaction_top,                               HeapWord*)                             \
  nonstatic_field(CompactibleSpace,            _first_dead,                                   HeapWord*)                             \
  nonstatic_field(CompactibleSpace,            _end_of_live,                                  HeapWord*)                             \
                                                                                                                                     \
  nonstatic_field(ContiguousSpace,             _top,                                          HeapWord*)                             \
  nonstatic_field(ContiguousSpace,             _saved_mark_word,                              HeapWord*)                             \
                                                                                                                                     \
  nonstatic_field(Generation,                  _reserved,                                     MemRegion)                             \
  nonstatic_field(Generation,                  _virtual_space,                                VirtualSpace)                          \
  nonstatic_field(Generation,                  _stat_record,                                  Generation::StatRecord)                \
                                                                                                                                     \
  nonstatic_field(Generation::StatRecord,      invocations,                                   int)                                   \
  nonstatic_field(Generation::StatRecord,      accumulated_time,                              elapsedTimer)                          \
                                                                                                                                     \
  nonstatic_field(GenerationSpec,              _name,                                         Generation::Name)                      \
  nonstatic_field(GenerationSpec,              _init_size,                                    size_t)                                \
  nonstatic_field(GenerationSpec,              _max_size,                                     size_t)                                \
                                                                                                                                     \
  nonstatic_field(GenCollectedHeap,            _young_gen,                                    Generation*)                           \
  nonstatic_field(GenCollectedHeap,            _old_gen,                                      Generation*)                           \
  nonstatic_field(GenCollectedHeap,            _young_gen_spec,                               GenerationSpec*)                       \
  nonstatic_field(GenCollectedHeap,            _old_gen_spec,                                 GenerationSpec*)                       \
                                                                                                                                     \
  nonstatic_field(MemRegion,                   _start,                                        HeapWord*)                             \
  nonstatic_field(MemRegion,                   _word_size,                                    size_t)                                \
                                                                                                                                     \
  nonstatic_field(OffsetTableContigSpace,      _offsets,                                      BlockOffsetArray)                      \
                                                                                                                                     \
  nonstatic_field(Space,                       _bottom,                                       HeapWord*)                             \
  nonstatic_field(Space,                       _end,                                          HeapWord*)

#define VM_TYPES_GC(declare_type,                                         \
                    declare_toplevel_type,                                \
                    declare_integer_type)                                 \
  EPSILONGC_ONLY(VM_TYPES_EPSILONGC(declare_type,                         \
                                    declare_toplevel_type,                \
                                    declare_integer_type))                \
  G1GC_ONLY(VM_TYPES_G1GC(declare_type,                                   \
                          declare_toplevel_type,                          \
                          declare_integer_type))                          \
  PARALLELGC_ONLY(VM_TYPES_PARALLELGC(declare_type,                       \
                                      declare_toplevel_type,              \
                                      declare_integer_type))              \
  SERIALGC_ONLY(VM_TYPES_SERIALGC(declare_type,                           \
                                  declare_toplevel_type,                  \
                                  declare_integer_type))                  \
  SHENANDOAHGC_ONLY(VM_TYPES_SHENANDOAH(declare_type,                     \
                             declare_toplevel_type,                       \
                             declare_integer_type))                       \
  ZGC_ONLY(VM_TYPES_ZGC(declare_type,                                     \
                        declare_toplevel_type,                            \
                        declare_integer_type))                            \
                                                                          \
  /******************************************/                            \
  /* Generation and space hierarchies       */                            \
  /* (needed for run-time type information) */                            \
  /******************************************/                            \
                                                                          \
  declare_toplevel_type(CollectedHeap)                                    \
           declare_type(GenCollectedHeap,             CollectedHeap)      \
  declare_toplevel_type(Generation)                                       \
           declare_type(CardGeneration,               Generation)         \
  declare_toplevel_type(Space)                                            \
           declare_type(CompactibleSpace,             Space)              \
           declare_type(ContiguousSpace,              CompactibleSpace)   \
           declare_type(OffsetTableContigSpace,       ContiguousSpace)    \
  declare_toplevel_type(BarrierSet)                                       \
           declare_type(ModRefBarrierSet,             BarrierSet)         \
           declare_type(CardTableBarrierSet,          ModRefBarrierSet)   \
  declare_toplevel_type(CardTable)                                        \
           declare_type(CardTableRS, CardTable)                           \
  declare_toplevel_type(BarrierSet::Name)                                 \
  declare_toplevel_type(BlockOffsetSharedArray)                           \
  declare_toplevel_type(BlockOffsetTable)                                 \
           declare_type(BlockOffsetArray,             BlockOffsetTable)   \
           declare_type(BlockOffsetArrayContigSpace,  BlockOffsetArray)   \
                                                                          \
  /* Miscellaneous other GC types */                                      \
                                                                          \
  declare_toplevel_type(AgeTable)                                         \
  declare_toplevel_type(CardTable::CardValue)                             \
  declare_toplevel_type(Generation::StatRecord)                           \
  declare_toplevel_type(GenerationSpec)                                   \
  declare_toplevel_type(HeapWord)                                         \
  declare_toplevel_type(MemRegion)                                        \
  declare_toplevel_type(ThreadLocalAllocBuffer)                           \
  declare_toplevel_type(VirtualSpace)                                     \
                                                                          \
  /* Pointers to Garbage Collection types */                              \
                                                                          \
  declare_toplevel_type(BarrierSet*)                                      \
  declare_toplevel_type(BlockOffsetSharedArray*)                          \
  declare_toplevel_type(CardTable*)                                       \
  declare_toplevel_type(CardTable*const)                                  \
  declare_toplevel_type(CardTableRS*)                                     \
  declare_toplevel_type(CardTableBarrierSet*)                             \
  declare_toplevel_type(CardTableBarrierSet**)                            \
  declare_toplevel_type(CollectedHeap*)                                   \
  declare_toplevel_type(ContiguousSpace*)                                 \
  declare_toplevel_type(DefNewGeneration*)                                \
  declare_toplevel_type(GenCollectedHeap*)                                \
  declare_toplevel_type(Generation*)                                      \
  declare_toplevel_type(GenerationSpec**)                                 \
  declare_toplevel_type(HeapWord*)                                        \
  declare_toplevel_type(HeapWord* volatile)                               \
  declare_toplevel_type(MemRegion*)                                       \
  declare_toplevel_type(OffsetTableContigSpace*)                          \
  declare_toplevel_type(Space*)                                           \
  declare_toplevel_type(ThreadLocalAllocBuffer*)                          \
                                                                          \
  declare_toplevel_type(BarrierSet::FakeRtti)

#define VM_INT_CONSTANTS_GC(declare_constant,                               \
                            declare_constant_with_value)                    \
  EPSILONGC_ONLY(VM_INT_CONSTANTS_EPSILONGC(declare_constant,               \
                                            declare_constant_with_value))   \
  G1GC_ONLY(VM_INT_CONSTANTS_G1GC(declare_constant,                         \
                                  declare_constant_with_value))             \
  PARALLELGC_ONLY(VM_INT_CONSTANTS_PARALLELGC(declare_constant,             \
                                              declare_constant_with_value)) \
  SERIALGC_ONLY(VM_INT_CONSTANTS_SERIALGC(declare_constant,                 \
                                          declare_constant_with_value))     \
  SHENANDOAHGC_ONLY(VM_INT_CONSTANTS_SHENANDOAH(declare_constant,           \
                                     declare_constant_with_value))          \
  ZGC_ONLY(VM_INT_CONSTANTS_ZGC(declare_constant,                           \
                                declare_constant_with_value))               \
                                                                            \
  /********************************************/                            \
  /* Generation and Space Hierarchy Constants */                            \
  /********************************************/                            \
                                                                            \
  declare_constant(AgeTable::table_size)                                    \
                                                                            \
  declare_constant(BarrierSet::ModRef)                                      \
  declare_constant(BarrierSet::CardTableBarrierSet)                         \
                                                                            \
  declare_constant(BOTConstants::LogN)                                      \
  declare_constant(BOTConstants::LogN_words)                                \
  declare_constant(BOTConstants::N_bytes)                                   \
  declare_constant(BOTConstants::N_words)                                   \
  declare_constant(BOTConstants::LogBase)                                   \
  declare_constant(BOTConstants::Base)                                      \
  declare_constant(BOTConstants::N_powers)                                  \
                                                                            \
  declare_constant(CardTable::clean_card)                                   \
  declare_constant(CardTable::last_card)                                    \
  declare_constant(CardTable::dirty_card)                                   \
  declare_constant(CardTable::Precise)                                      \
  declare_constant(CardTable::ObjHeadPreciseArray)                          \
  declare_constant(CardTable::card_shift)                                   \
  declare_constant(CardTable::card_size)                                    \
  declare_constant(CardTable::card_size_in_words)                           \
                                                                            \
  declare_constant(CollectedHeap::Serial)                                   \
  declare_constant(CollectedHeap::Parallel)                                 \
  declare_constant(CollectedHeap::G1)                                       \
                                                                            \
  /* constants from Generation::Name enum */                                \
                                                                            \
  declare_constant(Generation::DefNew)                                      \
  declare_constant(Generation::MarkSweepCompact)                            \
  declare_constant(Generation::Other)                                       \
                                                                            \
  declare_constant(Generation::LogOfGenGrain)                               \
  declare_constant(Generation::GenGrain)                                    \

#define VM_LONG_CONSTANTS_GC(declare_constant)                              \
  ZGC_ONLY(VM_LONG_CONSTANTS_ZGC(declare_constant))

#endif // SHARE_GC_SHARED_VMSTRUCTS_GC_HPP
