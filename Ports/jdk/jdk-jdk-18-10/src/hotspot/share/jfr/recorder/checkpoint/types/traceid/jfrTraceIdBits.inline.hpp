/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_INLINE_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_INLINE_HPP

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceIdBits.hpp"

#include "oops/method.hpp"
#include "runtime/atomic.hpp"
#include "utilities/macros.hpp"

#ifdef VM_LITTLE_ENDIAN
const int low_offset = 0;
const int meta_offset = low_offset + 1;
#else
const int low_offset = 7;
const int meta_offset = low_offset - 1;
#endif

inline jbyte* low_addr(jbyte* addr) {
  assert(addr != NULL, "invariant");
  return addr + low_offset;
}

inline jbyte* low_addr(traceid* addr) {
  return low_addr((jbyte*)addr);
}

inline jbyte* meta_addr(jbyte* addr) {
  assert(addr != NULL, "invariant");
  return addr + meta_offset;
}

inline jbyte* meta_addr(traceid* addr) {
  return meta_addr((jbyte*)addr);
}

template <typename T>
inline jbyte* traceid_tag_byte(const T* ptr) {
  assert(ptr != NULL, "invariant");
  return low_addr(ptr->trace_id_addr());
}

template <>
inline jbyte* traceid_tag_byte<Method>(const Method* ptr) {
  assert(ptr != NULL, "invariant");
  return ptr->trace_flags_addr();
}

template <typename T>
inline jbyte* traceid_meta_byte(const T* ptr) {
  assert(ptr != NULL, "invariant");
  return meta_addr(ptr->trace_id_addr());
}

template <>
inline jbyte* traceid_meta_byte<Method>(const Method* ptr) {
  assert(ptr != NULL, "invariant");
  return ptr->trace_meta_addr();
}

inline jbyte traceid_and(jbyte bits, jbyte current) {
  return bits & current;
}

inline jbyte traceid_or(jbyte bits, jbyte current) {
  return bits | current;
}

inline jbyte traceid_xor(jbyte bits, jbyte current) {
  return bits ^ current;
}

template <jbyte op(jbyte, jbyte)>
inline void set_form(jbyte bits, jbyte* dest) {
  assert(dest != NULL, "invariant");
  *dest = op(bits, *dest);
  OrderAccess::storestore();
}

template <jbyte op(jbyte, jbyte)>
inline void set_cas_form(jbyte bits, jbyte volatile* dest) {
  assert(dest != NULL, "invariant");
  do {
    const jbyte current = *dest;
    const jbyte new_value = op(bits, current);
    if (current == new_value || Atomic::cmpxchg(dest, current, new_value) == current) {
      return;
    }
  } while (true);
}

template <typename T>
inline void JfrTraceIdBits::cas(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  set_cas_form<traceid_or>(bits, traceid_tag_byte(ptr));
}

template <typename T>
inline traceid JfrTraceIdBits::load(const T* ptr) {
  assert(ptr != NULL, "invariant");
  return ptr->trace_id();
}

inline void set(jbyte bits, jbyte* dest) {
  assert(dest != NULL, "invariant");
  set_form<traceid_or>(bits, dest);
}

template <typename T>
inline void JfrTraceIdBits::store(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  set(bits, traceid_tag_byte(ptr));
}

template <typename T>
inline void JfrTraceIdBits::meta_store(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  set(bits, traceid_meta_byte(ptr));
}

inline void set_mask(jbyte mask, jbyte* dest) {
  set_cas_form<traceid_and>(mask, dest);
}

template <typename T>
inline void JfrTraceIdBits::mask_store(jbyte mask, const T* ptr) {
  assert(ptr != NULL, "invariant");
  set_mask(mask, traceid_tag_byte(ptr));
}

template <typename T>
inline void JfrTraceIdBits::meta_mask_store(jbyte mask, const T* ptr) {
  assert(ptr != NULL, "invariant");
  set_mask(mask, traceid_meta_byte(ptr));
}

inline void clear_bits(jbyte bits, jbyte* dest) {
  set_form<traceid_xor>(bits, dest);
}

template <typename T>
inline void JfrTraceIdBits::clear(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  clear_bits(bits, traceid_tag_byte(ptr));
}

inline void clear_bits_cas(jbyte bits, jbyte* dest) {
  set_cas_form<traceid_xor>(bits, dest);
}

template <typename T>
inline void JfrTraceIdBits::clear_cas(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  clear_bits_cas(bits, traceid_tag_byte(ptr));
}

template <typename T>
inline void JfrTraceIdBits::meta_clear(jbyte bits, const T* ptr) {
  assert(ptr != NULL, "invariant");
  clear_bits(bits, traceid_meta_byte(ptr));
}

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDBITS_INLINE_HPP
