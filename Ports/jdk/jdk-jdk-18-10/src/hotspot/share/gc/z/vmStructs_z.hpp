/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_VMSTRUCTS_Z_HPP
#define SHARE_GC_Z_VMSTRUCTS_Z_HPP

#include "gc/z/zAttachedArray.hpp"
#include "gc/z/zCollectedHeap.hpp"
#include "gc/z/zForwarding.hpp"
#include "gc/z/zGranuleMap.hpp"
#include "gc/z/zHeap.hpp"
#include "gc/z/zPageAllocator.hpp"
#include "utilities/macros.hpp"

// Expose some ZGC globals to the SA agent.
class ZGlobalsForVMStructs {
  static ZGlobalsForVMStructs _instance;

public:
  static ZGlobalsForVMStructs* _instance_p;

  ZGlobalsForVMStructs();

  uint32_t* _ZGlobalPhase;

  uint32_t* _ZGlobalSeqNum;

  uintptr_t* _ZAddressOffsetMask;
  uintptr_t* _ZAddressMetadataMask;
  uintptr_t* _ZAddressMetadataFinalizable;
  uintptr_t* _ZAddressGoodMask;
  uintptr_t* _ZAddressBadMask;
  uintptr_t* _ZAddressWeakBadMask;

  const int* _ZObjectAlignmentSmallShift;
  const int* _ZObjectAlignmentSmall;
};

typedef ZGranuleMap<ZPage*> ZGranuleMapForPageTable;
typedef ZGranuleMap<ZForwarding*> ZGranuleMapForForwarding;
typedef ZAttachedArray<ZForwarding, ZForwardingEntry> ZAttachedArrayForForwarding;

#define VM_STRUCTS_ZGC(nonstatic_field, volatile_nonstatic_field, static_field)                      \
  static_field(ZGlobalsForVMStructs,            _instance_p,          ZGlobalsForVMStructs*)         \
  nonstatic_field(ZGlobalsForVMStructs,         _ZGlobalPhase,        uint32_t*)                     \
  nonstatic_field(ZGlobalsForVMStructs,         _ZGlobalSeqNum,       uint32_t*)                     \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressOffsetMask,  uintptr_t*)                    \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressMetadataMask, uintptr_t*)                   \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressMetadataFinalizable, uintptr_t*)            \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressGoodMask,    uintptr_t*)                    \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressBadMask,     uintptr_t*)                    \
  nonstatic_field(ZGlobalsForVMStructs,         _ZAddressWeakBadMask, uintptr_t*)                    \
  nonstatic_field(ZGlobalsForVMStructs,         _ZObjectAlignmentSmallShift, const int*)             \
  nonstatic_field(ZGlobalsForVMStructs,         _ZObjectAlignmentSmall, const int*)                  \
                                                                                                     \
  nonstatic_field(ZCollectedHeap,               _heap,                ZHeap)                         \
                                                                                                     \
  nonstatic_field(ZHeap,                        _page_allocator,      ZPageAllocator)                \
  nonstatic_field(ZHeap,                        _page_table,          ZPageTable)                    \
  nonstatic_field(ZHeap,                        _forwarding_table,    ZForwardingTable)              \
  nonstatic_field(ZHeap,                        _relocate,            ZRelocate)                     \
                                                                                                     \
  nonstatic_field(ZPage,                        _type,                const uint8_t)                 \
  nonstatic_field(ZPage,                        _seqnum,              uint32_t)                      \
  nonstatic_field(ZPage,                        _virtual,             const ZVirtualMemory)          \
  volatile_nonstatic_field(ZPage,               _top,                 uintptr_t)                     \
                                                                                                     \
  nonstatic_field(ZPageAllocator,               _max_capacity,        const size_t)                  \
  volatile_nonstatic_field(ZPageAllocator,      _capacity,            size_t)                        \
  volatile_nonstatic_field(ZPageAllocator,      _used,                size_t)                        \
                                                                                                     \
  nonstatic_field(ZPageTable,                   _map,                 ZGranuleMapForPageTable)       \
                                                                                                     \
  nonstatic_field(ZGranuleMapForPageTable,      _map,                 ZPage** const)                 \
  nonstatic_field(ZGranuleMapForForwarding,     _map,                 ZForwarding** const)           \
                                                                                                     \
  nonstatic_field(ZForwardingTable,             _map,                 ZGranuleMapForForwarding)      \
                                                                                                     \
  nonstatic_field(ZVirtualMemory,               _start,               const uintptr_t)               \
  nonstatic_field(ZVirtualMemory,               _end,                 const uintptr_t)               \
                                                                                                     \
  nonstatic_field(ZForwarding,                  _virtual,             const ZVirtualMemory)          \
  nonstatic_field(ZForwarding,                  _object_alignment_shift, const size_t)               \
  volatile_nonstatic_field(ZForwarding,         _ref_count,           int)                           \
  nonstatic_field(ZForwarding,                  _entries,             const ZAttachedArrayForForwarding) \
  nonstatic_field(ZForwardingEntry,             _entry,               uint64_t)                      \
  nonstatic_field(ZAttachedArrayForForwarding,  _length,              const size_t)

#define VM_INT_CONSTANTS_ZGC(declare_constant, declare_constant_with_value)                          \
  declare_constant(ZPhaseRelocate)                                                                   \
  declare_constant(ZPageTypeSmall)                                                                   \
  declare_constant(ZPageTypeMedium)                                                                  \
  declare_constant(ZPageTypeLarge)                                                                   \
  declare_constant(ZObjectAlignmentMediumShift)                                                      \
  declare_constant(ZObjectAlignmentLargeShift)

#define VM_LONG_CONSTANTS_ZGC(declare_constant)                                                      \
  declare_constant(ZGranuleSizeShift)                                                                \
  declare_constant(ZPageSizeSmallShift)                                                              \
  declare_constant(ZPageSizeMediumShift)                                                             \
  declare_constant(ZAddressOffsetShift)                                                              \
  declare_constant(ZAddressOffsetBits)                                                               \
  declare_constant(ZAddressOffsetMask)                                                               \
  declare_constant(ZAddressOffsetMax)

#define VM_TYPES_ZGC(declare_type, declare_toplevel_type, declare_integer_type)                      \
  declare_toplevel_type(ZGlobalsForVMStructs)                                                        \
  declare_type(ZCollectedHeap, CollectedHeap)                                                        \
  declare_toplevel_type(ZHeap)                                                                       \
  declare_toplevel_type(ZRelocate)                                                                   \
  declare_toplevel_type(ZPage)                                                                       \
  declare_toplevel_type(ZPageAllocator)                                                              \
  declare_toplevel_type(ZPageTable)                                                                  \
  declare_toplevel_type(ZAttachedArrayForForwarding)                                                 \
  declare_toplevel_type(ZGranuleMapForPageTable)                                                     \
  declare_toplevel_type(ZGranuleMapForForwarding)                                                    \
  declare_toplevel_type(ZVirtualMemory)                                                              \
  declare_toplevel_type(ZForwardingTable)                                                            \
  declare_toplevel_type(ZForwarding)                                                                 \
  declare_toplevel_type(ZForwardingEntry)                                                            \
  declare_toplevel_type(ZPhysicalMemoryManager)

#endif // SHARE_GC_Z_VMSTRUCTS_Z_HPP
