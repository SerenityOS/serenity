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

#include "precompiled.hpp"
#include "gc/z/zGlobals.hpp"

uint32_t   ZGlobalPhase                = ZPhaseRelocate;
uint32_t   ZGlobalSeqNum               = 1;

size_t     ZPageSizeMediumShift;
size_t     ZPageSizeMedium;

size_t     ZObjectSizeLimitMedium;

const int& ZObjectAlignmentSmallShift  = LogMinObjAlignmentInBytes;
int        ZObjectAlignmentMediumShift;

const int& ZObjectAlignmentSmall       = MinObjAlignmentInBytes;
int        ZObjectAlignmentMedium;

uintptr_t  ZAddressGoodMask;
uintptr_t  ZAddressBadMask;
uintptr_t  ZAddressWeakBadMask;

static uint32_t* ZAddressCalculateBadMaskHighOrderBitsAddr() {
  const uintptr_t addr = reinterpret_cast<uintptr_t>(&ZAddressBadMask);
  return reinterpret_cast<uint32_t*>(addr + ZAddressBadMaskHighOrderBitsOffset);
}

uint32_t*  ZAddressBadMaskHighOrderBitsAddr = ZAddressCalculateBadMaskHighOrderBitsAddr();

size_t     ZAddressOffsetBits;
uintptr_t  ZAddressOffsetMask;
size_t     ZAddressOffsetMax;

size_t     ZAddressMetadataShift;
uintptr_t  ZAddressMetadataMask;

uintptr_t  ZAddressMetadataMarked;
uintptr_t  ZAddressMetadataMarked0;
uintptr_t  ZAddressMetadataMarked1;
uintptr_t  ZAddressMetadataRemapped;
uintptr_t  ZAddressMetadataFinalizable;

const char* ZGlobalPhaseToString() {
  switch (ZGlobalPhase) {
  case ZPhaseMark:
    return "Mark";

  case ZPhaseMarkCompleted:
    return "MarkCompleted";

  case ZPhaseRelocate:
    return "Relocate";

  default:
    return "Unknown";
  }
}
