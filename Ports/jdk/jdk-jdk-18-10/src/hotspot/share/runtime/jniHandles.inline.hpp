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

#ifndef SHARE_RUNTIME_JNIHANDLES_INLINE_HPP
#define SHARE_RUNTIME_JNIHANDLES_INLINE_HPP

#include "runtime/jniHandles.hpp"

#include "oops/access.inline.hpp"
#include "oops/oop.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

inline bool JNIHandles::is_jweak(jobject handle) {
  STATIC_ASSERT(weak_tag_size == 1);
  STATIC_ASSERT(weak_tag_value == 1);
  return (reinterpret_cast<uintptr_t>(handle) & weak_tag_mask) != 0;
}

inline oop* JNIHandles::jobject_ptr(jobject handle) {
  assert(!is_jweak(handle), "precondition");
  return reinterpret_cast<oop*>(handle);
}

inline oop* JNIHandles::jweak_ptr(jobject handle) {
  assert(is_jweak(handle), "precondition");
  char* ptr = reinterpret_cast<char*>(handle) - weak_tag_value;
  return reinterpret_cast<oop*>(ptr);
}

// external_guard is true if called from resolve_external_guard.
template <DecoratorSet decorators, bool external_guard>
inline oop JNIHandles::resolve_impl(jobject handle) {
  assert(handle != NULL, "precondition");
  assert(!current_thread_in_native(), "must not be in native");
  oop result;
  if (is_jweak(handle)) {       // Unlikely
    result = NativeAccess<ON_PHANTOM_OOP_REF|decorators>::oop_load(jweak_ptr(handle));
  } else {
    result = NativeAccess<decorators>::oop_load(jobject_ptr(handle));
    // Construction of jobjects canonicalize a null value into a null
    // jobject, so for non-jweak the pointee should never be null.
    assert(external_guard || result != NULL, "Invalid JNI handle");
  }
  return result;
}

inline oop JNIHandles::resolve(jobject handle) {
  oop result = NULL;
  if (handle != NULL) {
    result = resolve_impl<DECORATORS_NONE, false /* external_guard */>(handle);
  }
  return result;
}

inline oop JNIHandles::resolve_no_keepalive(jobject handle) {
  oop result = NULL;
  if (handle != NULL) {
    result = resolve_impl<AS_NO_KEEPALIVE, false /* external_guard */>(handle);
  }
  return result;
}

inline bool JNIHandles::is_same_object(jobject handle1, jobject handle2) {
  oop obj1 = resolve_no_keepalive(handle1);
  oop obj2 = resolve_no_keepalive(handle2);
  return obj1 == obj2;
}

inline oop JNIHandles::resolve_non_null(jobject handle) {
  assert(handle != NULL, "JNI handle should not be null");
  oop result = resolve_impl<DECORATORS_NONE, false /* external_guard */>(handle);
  assert(result != NULL, "NULL read from jni handle");
  return result;
}

inline void JNIHandles::destroy_local(jobject handle) {
  if (handle != NULL) {
    assert(!is_jweak(handle), "Invalid JNI local handle");
    NativeAccess<>::oop_store(jobject_ptr(handle), (oop)NULL);
  }
}

#endif // SHARE_RUNTIME_JNIHANDLES_INLINE_HPP
