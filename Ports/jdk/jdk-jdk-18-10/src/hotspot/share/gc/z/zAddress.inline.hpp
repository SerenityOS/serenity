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

#ifndef SHARE_GC_Z_ZADDRESS_INLINE_HPP
#define SHARE_GC_Z_ZADDRESS_INLINE_HPP

#include "gc/z/zAddress.hpp"

#include "gc/z/zGlobals.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/powerOfTwo.hpp"

inline bool ZAddress::is_null(uintptr_t value) {
  return value == 0;
}

inline bool ZAddress::is_bad(uintptr_t value) {
  return value & ZAddressBadMask;
}

inline bool ZAddress::is_good(uintptr_t value) {
  return !is_bad(value) && !is_null(value);
}

inline bool ZAddress::is_good_or_null(uintptr_t value) {
  // Checking if an address is "not bad" is an optimized version of
  // checking if it's "good or null", which eliminates an explicit
  // null check. However, the implicit null check only checks that
  // the mask bits are zero, not that the entire address is zero.
  // This means that an address without mask bits would pass through
  // the barrier as if it was null. This should be harmless as such
  // addresses should ever be passed through the barrier.
  const bool result = !is_bad(value);
  assert((is_good(value) || is_null(value)) == result, "Bad address");
  return result;
}

inline bool ZAddress::is_weak_bad(uintptr_t value) {
  return value & ZAddressWeakBadMask;
}

inline bool ZAddress::is_weak_good(uintptr_t value) {
  return !is_weak_bad(value) && !is_null(value);
}

inline bool ZAddress::is_weak_good_or_null(uintptr_t value) {
  return !is_weak_bad(value);
}

inline bool ZAddress::is_marked(uintptr_t value) {
  return value & ZAddressMetadataMarked;
}

inline bool ZAddress::is_marked_or_null(uintptr_t value) {
  return is_marked(value) || is_null(value);
}

inline bool ZAddress::is_finalizable(uintptr_t value) {
  return value & ZAddressMetadataFinalizable;
}

inline bool ZAddress::is_finalizable_good(uintptr_t value) {
  return is_finalizable(value) && is_good(value ^ ZAddressMetadataFinalizable);
}

inline bool ZAddress::is_remapped(uintptr_t value) {
  return value & ZAddressMetadataRemapped;
}

inline bool ZAddress::is_in(uintptr_t value) {
  // Check that exactly one non-offset bit is set
  if (!is_power_of_2(value & ~ZAddressOffsetMask)) {
    return false;
  }

  // Check that one of the non-finalizable metadata is set
  return value & (ZAddressMetadataMask & ~ZAddressMetadataFinalizable);
}

inline uintptr_t ZAddress::offset(uintptr_t value) {
  return value & ZAddressOffsetMask;
}

inline uintptr_t ZAddress::good(uintptr_t value) {
  return offset(value) | ZAddressGoodMask;
}

inline uintptr_t ZAddress::good_or_null(uintptr_t value) {
  return is_null(value) ? 0 : good(value);
}

inline uintptr_t ZAddress::finalizable_good(uintptr_t value) {
  return offset(value) | ZAddressMetadataFinalizable | ZAddressGoodMask;
}

inline uintptr_t ZAddress::marked(uintptr_t value) {
  return offset(value) | ZAddressMetadataMarked;
}

inline uintptr_t ZAddress::marked0(uintptr_t value) {
  return offset(value) | ZAddressMetadataMarked0;
}

inline uintptr_t ZAddress::marked1(uintptr_t value) {
  return offset(value) | ZAddressMetadataMarked1;
}

inline uintptr_t ZAddress::remapped(uintptr_t value) {
  return offset(value) | ZAddressMetadataRemapped;
}

inline uintptr_t ZAddress::remapped_or_null(uintptr_t value) {
  return is_null(value) ? 0 : remapped(value);
}

#endif // SHARE_GC_Z_ZADDRESS_INLINE_HPP
