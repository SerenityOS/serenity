/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZGLOBALS_HPP
#define SHARE_GC_Z_ZGLOBALS_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include CPU_HEADER(gc/z/zGlobals)

// Collector name
const char* const ZName                         = "The Z Garbage Collector";

// Global phase state
extern uint32_t   ZGlobalPhase;
const uint32_t    ZPhaseMark                    = 0;
const uint32_t    ZPhaseMarkCompleted           = 1;
const uint32_t    ZPhaseRelocate                = 2;
const char*       ZGlobalPhaseToString();

// Global sequence number
extern uint32_t   ZGlobalSeqNum;

// Granule shift/size
const size_t      ZGranuleSizeShift             = ZPlatformGranuleSizeShift;
const size_t      ZGranuleSize                  = (size_t)1 << ZGranuleSizeShift;

// Number of heap views
const size_t      ZHeapViews                    = ZPlatformHeapViews;

// Virtual memory to physical memory ratio
const size_t      ZVirtualToPhysicalRatio       = 16; // 16:1

// Page types
const uint8_t     ZPageTypeSmall                = 0;
const uint8_t     ZPageTypeMedium               = 1;
const uint8_t     ZPageTypeLarge                = 2;

// Page size shifts
const size_t      ZPageSizeSmallShift           = ZGranuleSizeShift;
extern size_t     ZPageSizeMediumShift;

// Page sizes
const size_t      ZPageSizeSmall                = (size_t)1 << ZPageSizeSmallShift;
extern size_t     ZPageSizeMedium;

// Object size limits
const size_t      ZObjectSizeLimitSmall         = ZPageSizeSmall / 8; // 12.5% max waste
extern size_t     ZObjectSizeLimitMedium;

// Object alignment shifts
extern const int& ZObjectAlignmentSmallShift;
extern int        ZObjectAlignmentMediumShift;
const int         ZObjectAlignmentLargeShift    = ZGranuleSizeShift;

// Object alignments
extern const int& ZObjectAlignmentSmall;
extern int        ZObjectAlignmentMedium;
const int         ZObjectAlignmentLarge         = 1 << ZObjectAlignmentLargeShift;

//
// Good/Bad mask states
// --------------------
//
//                 GoodMask         BadMask          WeakGoodMask     WeakBadMask
//                 --------------------------------------------------------------
//  Marked0        001              110              101              010
//  Marked1        010              101              110              001
//  Remapped       100              011              100              011
//

// Good/bad masks
extern uintptr_t  ZAddressGoodMask;
extern uintptr_t  ZAddressBadMask;
extern uintptr_t  ZAddressWeakBadMask;

// The bad mask is 64 bit. Its high order 32 bits contain all possible value combinations
// that this mask will have. Therefore, the memory where the 32 high order bits are stored,
// can be used as a 32 bit GC epoch counter, that has a different bit pattern every time
// the bad mask is flipped. This provides a pointer to said 32 bits.
extern uint32_t*  ZAddressBadMaskHighOrderBitsAddr;
const int         ZAddressBadMaskHighOrderBitsOffset = LITTLE_ENDIAN_ONLY(4) BIG_ENDIAN_ONLY(0);

// Pointer part of address
extern size_t     ZAddressOffsetBits;
const  size_t     ZAddressOffsetShift           = 0;
extern uintptr_t  ZAddressOffsetMask;
extern size_t     ZAddressOffsetMax;

// Metadata part of address
const size_t      ZAddressMetadataBits          = 4;
extern size_t     ZAddressMetadataShift;
extern uintptr_t  ZAddressMetadataMask;

// Metadata types
extern uintptr_t  ZAddressMetadataMarked;
extern uintptr_t  ZAddressMetadataMarked0;
extern uintptr_t  ZAddressMetadataMarked1;
extern uintptr_t  ZAddressMetadataRemapped;
extern uintptr_t  ZAddressMetadataFinalizable;

// Cache line size
const size_t      ZCacheLineSize                = ZPlatformCacheLineSize;
#define           ZCACHE_ALIGNED                ATTRIBUTE_ALIGNED(ZCacheLineSize)

// Mark stack space
extern uintptr_t  ZMarkStackSpaceStart;
const size_t      ZMarkStackSpaceExpandSize     = (size_t)1 << 25; // 32M

// Mark stack and magazine sizes
const size_t      ZMarkStackSizeShift           = 11; // 2K
const size_t      ZMarkStackSize                = (size_t)1 << ZMarkStackSizeShift;
const size_t      ZMarkStackHeaderSize          = (size_t)1 << 4; // 16B
const size_t      ZMarkStackSlots               = (ZMarkStackSize - ZMarkStackHeaderSize) / sizeof(uintptr_t);
const size_t      ZMarkStackMagazineSize        = (size_t)1 << 15; // 32K
const size_t      ZMarkStackMagazineSlots       = (ZMarkStackMagazineSize / ZMarkStackSize) - 1;

// Mark stripe size
const size_t      ZMarkStripeShift              = ZGranuleSizeShift;

// Max number of mark stripes
const size_t      ZMarkStripesMax               = 16; // Must be a power of two

// Mark cache size
const size_t      ZMarkCacheSize                = 1024; // Must be a power of two

// Partial array minimum size
const size_t      ZMarkPartialArrayMinSizeShift = 12; // 4K
const size_t      ZMarkPartialArrayMinSize      = (size_t)1 << ZMarkPartialArrayMinSizeShift;

// Max number of proactive/terminate flush attempts
const size_t      ZMarkProactiveFlushMax        = 10;
const size_t      ZMarkTerminateFlushMax        = 3;

// Try complete mark timeout
const uint64_t    ZMarkCompleteTimeout          = 200; // us

#endif // SHARE_GC_Z_ZGLOBALS_HPP
