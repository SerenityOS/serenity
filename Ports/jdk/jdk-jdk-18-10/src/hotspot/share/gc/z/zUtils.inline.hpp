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

#ifndef SHARE_GC_Z_ZUTILS_INLINE_HPP
#define SHARE_GC_Z_ZUTILS_INLINE_HPP

#include "gc/z/zUtils.hpp"

#include "gc/z/zOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

inline size_t ZUtils::bytes_to_words(size_t size_in_bytes) {
  assert(is_aligned(size_in_bytes, BytesPerWord), "Size not word aligned");
  return size_in_bytes >> LogBytesPerWord;
}

inline size_t ZUtils::words_to_bytes(size_t size_in_words) {
  return size_in_words << LogBytesPerWord;
}

inline size_t ZUtils::object_size(uintptr_t addr) {
  return words_to_bytes(ZOop::from_address(addr)->size());
}

inline void ZUtils::object_copy_disjoint(uintptr_t from, uintptr_t to, size_t size) {
  Copy::aligned_disjoint_words((HeapWord*)from, (HeapWord*)to, bytes_to_words(size));
}

inline void ZUtils::object_copy_conjoint(uintptr_t from, uintptr_t to, size_t size) {
  if (from != to) {
    Copy::aligned_conjoint_words((HeapWord*)from, (HeapWord*)to, bytes_to_words(size));
  }
}

#endif // SHARE_GC_Z_ZUTILS_INLINE_HPP
