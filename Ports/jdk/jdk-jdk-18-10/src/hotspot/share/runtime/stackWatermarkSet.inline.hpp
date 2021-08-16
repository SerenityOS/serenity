/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_STACKWATERMARKSET_INLINE_HPP
#define SHARE_RUNTIME_STACKWATERMARKSET_INLINE_HPP

#include "runtime/stackWatermarkSet.hpp"

#include "runtime/stackWatermark.hpp"

inline StackWatermark* StackWatermarkSet::get(JavaThread* jt, StackWatermarkKind kind) {
  for (StackWatermark* stack_watermark = head(jt); stack_watermark != NULL; stack_watermark = stack_watermark->next()) {
    if (stack_watermark->kind() == kind) {
      return stack_watermark;
    }
  }
  return NULL;
}

template <typename T>
inline T* StackWatermarkSet::get(JavaThread* jt, StackWatermarkKind kind) {
  return static_cast<T*>(get(jt, kind));
}

inline bool StackWatermarkSet::has_watermark(JavaThread* jt, StackWatermarkKind kind) {
  return get(jt, kind) != NULL;
}

#endif // SHARE_RUNTIME_STACKWATERMARKSET_INLINE_HPP
