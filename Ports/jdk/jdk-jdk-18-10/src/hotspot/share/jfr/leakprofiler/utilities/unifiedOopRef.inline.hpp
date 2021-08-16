/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_UTILITIES_UNIFIEDOOPREF_INLINE_HPP
#define SHARE_JFR_LEAKPROFILER_UTILITIES_UNIFIEDOOPREF_INLINE_HPP

#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"

#include "oops/access.inline.hpp"
#include "utilities/debug.hpp"

template <typename T>
inline T UnifiedOopRef::addr() const {
  return reinterpret_cast<T>(_value & ~uintptr_t(3));
}

// Visual Studio 2019 and earlier have a problem with reinterpret_cast
// when the new type is the same as the expression type. For example:
//  reinterpret_cast<int>(1);
//  "error C2440: 'reinterpret_cast': cannot convert from 'int' to 'int'"
// this specialization provides a workaround.
template<>
inline uintptr_t UnifiedOopRef::addr<uintptr_t>() const {
  return _value & ~uintptr_t(3);
}

inline bool UnifiedOopRef::is_narrow() const {
  return _value & 1;
}

inline bool UnifiedOopRef::is_native() const {
  return _value & 2;
}

inline bool UnifiedOopRef::is_null() const {
  return _value == 0;
}

inline UnifiedOopRef UnifiedOopRef::encode_in_native(const narrowOop* ref) {
  assert(ref != NULL, "invariant");
  UnifiedOopRef result = { reinterpret_cast<uintptr_t>(ref) | 3 };
  assert(result.addr<narrowOop*>() == ref, "sanity");
  return result;
}

inline UnifiedOopRef UnifiedOopRef::encode_in_native(const oop* ref) {
  assert(ref != NULL, "invariant");
  UnifiedOopRef result = { reinterpret_cast<uintptr_t>(ref) | 2 };
  assert(result.addr<oop*>() == ref, "sanity");
  return result;
}

inline UnifiedOopRef UnifiedOopRef::encode_in_heap(const narrowOop* ref) {
  assert(ref != NULL, "invariant");
  UnifiedOopRef result = { reinterpret_cast<uintptr_t>(ref) | 1 };
  assert(result.addr<narrowOop*>() == ref, "sanity");
  return result;
}

inline UnifiedOopRef UnifiedOopRef::encode_in_heap(const oop* ref) {
  assert(ref != NULL, "invariant");
  UnifiedOopRef result = { reinterpret_cast<uintptr_t>(ref) | 0 };
  assert(result.addr<oop*>() == ref, "sanity");
  return result;
}

inline UnifiedOopRef UnifiedOopRef::encode_null() {
  UnifiedOopRef result = { 0 };
  return result;
}

inline oop UnifiedOopRef::dereference() const {
  if (is_native()) {
    if (is_narrow()) {
      return NativeAccess<AS_NO_KEEPALIVE>::oop_load(addr<narrowOop*>());
    } else {
      return NativeAccess<AS_NO_KEEPALIVE>::oop_load(addr<oop*>());
    }
  } else {
    if (is_narrow()) {
      return HeapAccess<AS_NO_KEEPALIVE>::oop_load(addr<narrowOop*>());
    } else {
      return HeapAccess<AS_NO_KEEPALIVE>::oop_load(addr<oop*>());
    }
  }
}

#endif // SHARE_JFR_LEAKPROFILER_UTILITIES_UNIFIEDOOPREF_INLINE_HPP
