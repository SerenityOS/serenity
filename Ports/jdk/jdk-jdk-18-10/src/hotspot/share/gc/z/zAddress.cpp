/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zAddress.hpp"
#include "gc/z/zGlobals.hpp"

void ZAddress::set_good_mask(uintptr_t mask) {
  ZAddressGoodMask = mask;
  ZAddressBadMask = ZAddressGoodMask ^ ZAddressMetadataMask;
  ZAddressWeakBadMask = (ZAddressGoodMask | ZAddressMetadataRemapped | ZAddressMetadataFinalizable) ^ ZAddressMetadataMask;
}

void ZAddress::initialize() {
  ZAddressOffsetBits = ZPlatformAddressOffsetBits();
  ZAddressOffsetMask = (((uintptr_t)1 << ZAddressOffsetBits) - 1) << ZAddressOffsetShift;
  ZAddressOffsetMax = (uintptr_t)1 << ZAddressOffsetBits;

  ZAddressMetadataShift = ZPlatformAddressMetadataShift();
  ZAddressMetadataMask = (((uintptr_t)1 << ZAddressMetadataBits) - 1) << ZAddressMetadataShift;

  ZAddressMetadataMarked0 = (uintptr_t)1 << (ZAddressMetadataShift + 0);
  ZAddressMetadataMarked1 = (uintptr_t)1 << (ZAddressMetadataShift + 1);
  ZAddressMetadataRemapped = (uintptr_t)1 << (ZAddressMetadataShift + 2);
  ZAddressMetadataFinalizable = (uintptr_t)1 << (ZAddressMetadataShift + 3);

  ZAddressMetadataMarked = ZAddressMetadataMarked0;
  set_good_mask(ZAddressMetadataRemapped);
}

void ZAddress::flip_to_marked() {
  ZAddressMetadataMarked ^= (ZAddressMetadataMarked0 | ZAddressMetadataMarked1);
  set_good_mask(ZAddressMetadataMarked);
}

void ZAddress::flip_to_remapped() {
  set_good_mask(ZAddressMetadataRemapped);
}
