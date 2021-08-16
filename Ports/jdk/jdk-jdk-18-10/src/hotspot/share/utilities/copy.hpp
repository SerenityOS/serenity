/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_COPY_HPP
#define SHARE_UTILITIES_COPY_HPP

#include "oops/oopsHierarchy.hpp"
#include "runtime/globals.hpp"
#include "utilities/align.hpp"
#include "utilities/bytes.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"

// Assembly code for platforms that need it.
extern "C" {
  void _Copy_conjoint_words(const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_disjoint_words(const HeapWord* from, HeapWord* to, size_t count);

  void _Copy_conjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count);

  void _Copy_aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count);

  void _Copy_conjoint_bytes(const void* from, void* to, size_t count);

  void _Copy_conjoint_bytes_atomic  (const void*   from, void*   to, size_t count);
  void _Copy_conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count);
  void _Copy_conjoint_jints_atomic  (const jint*   from, jint*   to, size_t count);
  void _Copy_conjoint_jlongs_atomic (const jlong*  from, jlong*  to, size_t count);
  void _Copy_conjoint_oops_atomic   (const oop*    from, oop*    to, size_t count);

  void _Copy_arrayof_conjoint_bytes  (const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_arrayof_conjoint_jshorts(const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_arrayof_conjoint_jints  (const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_arrayof_conjoint_jlongs (const HeapWord* from, HeapWord* to, size_t count);
  void _Copy_arrayof_conjoint_oops   (const HeapWord* from, HeapWord* to, size_t count);
}

class Copy : AllStatic {
 public:
  // Block copy methods have four attributes.  We don't define all possibilities.
  //   alignment: aligned to BytesPerLong
  //   arrayof:   arraycopy operation with both operands aligned on the same
  //              boundary as the first element of an array of the copy unit.
  //              This is currently a HeapWord boundary on all platforms, except
  //              for long and double arrays, which are aligned on an 8-byte
  //              boundary on all platforms.
  //              arraycopy operations are implicitly atomic on each array element.
  //   overlap:   disjoint or conjoint.
  //   copy unit: bytes or words (i.e., HeapWords) or oops (i.e., pointers).
  //   atomicity: atomic or non-atomic on the copy unit.
  //
  // Names are constructed thusly:
  //
  //     [ 'aligned_' | 'arrayof_' ]
  //     ('conjoint_' | 'disjoint_')
  //     ('words' | 'bytes' | 'jshorts' | 'jints' | 'jlongs' | 'oops')
  //     [ '_atomic' ]
  //
  // Except in the arrayof case, whatever the alignment is, we assume we can copy
  // whole alignment units.  E.g., if BytesPerLong is 2x word alignment, an odd
  // count may copy an extra word.  In the arrayof case, we are allowed to copy
  // only the number of copy units specified.
  //
  // All callees check count for 0.
  //

  // HeapWords

  // Word-aligned words,    conjoint, not atomic on each word
  static void conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, HeapWordSize);
    pd_conjoint_words(from, to, count);
  }

  // Word-aligned words,    disjoint, not atomic on each word
  static void disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, HeapWordSize);
    assert_disjoint(from, to, count);
    pd_disjoint_words(from, to, count);
  }

  // Word-aligned words,    disjoint, atomic on each word
  static void disjoint_words_atomic(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, HeapWordSize);
    assert_disjoint(from, to, count);
    pd_disjoint_words_atomic(from, to, count);
  }

  // Object-aligned words,  conjoint, not atomic on each word
  static void aligned_conjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_aligned(from, to);
    pd_aligned_conjoint_words(from, to, count);
  }

  // Object-aligned words,  disjoint, not atomic on each word
  static void aligned_disjoint_words(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_aligned(from, to);
    assert_disjoint(from, to, count);
    pd_aligned_disjoint_words(from, to, count);
  }

  // bytes, jshorts, jints, jlongs, oops

  // bytes,                 conjoint, not atomic on each byte (not that it matters)
  static void conjoint_jbytes(const void* from, void* to, size_t count) {
    pd_conjoint_bytes(from, to, count);
  }

  // bytes,                 conjoint, atomic on each byte (not that it matters)
  static void conjoint_jbytes_atomic(const void* from, void* to, size_t count) {
    pd_conjoint_bytes(from, to, count);
  }

  // jshorts,               conjoint, atomic on each jshort
  static void conjoint_jshorts_atomic(const jshort* from, jshort* to, size_t count) {
    assert_params_ok(from, to, BytesPerShort);
    pd_conjoint_jshorts_atomic(from, to, count);
  }

  // jints,                 conjoint, atomic on each jint
  static void conjoint_jints_atomic(const jint* from, jint* to, size_t count) {
    assert_params_ok(from, to, BytesPerInt);
    pd_conjoint_jints_atomic(from, to, count);
  }

  // jlongs,                conjoint, atomic on each jlong
  static void conjoint_jlongs_atomic(const jlong* from, jlong* to, size_t count) {
    assert_params_ok(from, to, BytesPerLong);
    pd_conjoint_jlongs_atomic(from, to, count);
  }

  // oops,                  conjoint, atomic on each oop
  static void conjoint_oops_atomic(const oop* from, oop* to, size_t count) {
    assert_params_ok(from, to, BytesPerHeapOop);
    pd_conjoint_oops_atomic(from, to, count);
  }

  // overloaded for UseCompressedOops
  static void conjoint_oops_atomic(const narrowOop* from, narrowOop* to, size_t count) {
    assert(sizeof(narrowOop) == sizeof(jint), "this cast is wrong");
    assert_params_ok(from, to, BytesPerInt);
    pd_conjoint_jints_atomic((const jint*)from, (jint*)to, count);
  }

  // Copy a span of memory.  If the span is an integral number of aligned
  // longs, words, or ints, copy those units atomically.
  // The largest atomic transfer unit is 8 bytes, or the largest power
  // of two which divides all of from, to, and size, whichever is smaller.
  static void conjoint_memory_atomic(const void* from, void* to, size_t size);

  // bytes,                 conjoint array, atomic on each byte (not that it matters)
  static void arrayof_conjoint_jbytes(const HeapWord* from, HeapWord* to, size_t count) {
    pd_arrayof_conjoint_bytes(from, to, count);
  }

  // jshorts,               conjoint array, atomic on each jshort
  static void arrayof_conjoint_jshorts(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, BytesPerShort);
    pd_arrayof_conjoint_jshorts(from, to, count);
  }

  // jints,                 conjoint array, atomic on each jint
  static void arrayof_conjoint_jints(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, BytesPerInt);
    pd_arrayof_conjoint_jints(from, to, count);
  }

  // jlongs,                conjoint array, atomic on each jlong
  static void arrayof_conjoint_jlongs(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, BytesPerLong);
    pd_arrayof_conjoint_jlongs(from, to, count);
  }

  // oops,                  conjoint array, atomic on each oop
  static void arrayof_conjoint_oops(const HeapWord* from, HeapWord* to, size_t count) {
    assert_params_ok(from, to, BytesPerHeapOop);
    pd_arrayof_conjoint_oops(from, to, count);
  }

  // Known overlap methods

  // Copy word-aligned words from higher to lower addresses, not atomic on each word
  inline static void conjoint_words_to_lower(const HeapWord* from, HeapWord* to, size_t byte_count) {
    // byte_count is in bytes to check its alignment
    assert_params_ok(from, to, HeapWordSize);
    assert_byte_count_ok(byte_count, HeapWordSize);

    size_t count = align_up(byte_count, HeapWordSize) >> LogHeapWordSize;
    assert(to <= from || from + count <= to, "do not overwrite source data");

    while (count-- > 0) {
      *to++ = *from++;
    }
  }

  // Copy word-aligned words from lower to higher addresses, not atomic on each word
  inline static void conjoint_words_to_higher(const HeapWord* from, HeapWord* to, size_t byte_count) {
    // byte_count is in bytes to check its alignment
    assert_params_ok(from, to, HeapWordSize);
    assert_byte_count_ok(byte_count, HeapWordSize);

    size_t count = align_up(byte_count, HeapWordSize) >> LogHeapWordSize;
    assert(from <= to || to + count <= from, "do not overwrite source data");

    from += count - 1;
    to   += count - 1;
    while (count-- > 0) {
      *to-- = *from--;
    }
  }

  /**
   * Copy elements
   *
   * @param src address of source
   * @param dst address of destination
   * @param byte_count number of bytes to copy
   * @param elem_size size of the elements to copy-swap
   */
  static void conjoint_copy(const void* src, void* dst, size_t byte_count, size_t elem_size);

  /**
   * Copy and *unconditionally* byte swap elements
   *
   * @param src address of source
   * @param dst address of destination
   * @param byte_count number of bytes to copy
   * @param elem_size size of the elements to copy-swap
   */
  static void conjoint_swap(const void* src, void* dst, size_t byte_count, size_t elem_size);

  /**
   * Copy and byte swap elements from the specified endian to the native (cpu) endian if needed (if they differ)
   *
   * @param src address of source
   * @param dst address of destination
   * @param byte_count number of bytes to copy
   * @param elem_size size of the elements to copy-swap
   */
  template <Endian::Order endian>
  static void conjoint_swap_if_needed(const void* src, void* dst, size_t byte_count, size_t elem_size) {
    if (Endian::NATIVE != endian) {
      conjoint_swap(src, dst, byte_count, elem_size);
    } else {
      conjoint_copy(src, dst, byte_count, elem_size);
    }
  }

  // Fill methods

  // Fill word-aligned words, not atomic on each word
  // set_words
  static void fill_to_words(HeapWord* to, size_t count, juint value = 0) {
    assert_params_ok(to, HeapWordSize);
    pd_fill_to_words(to, count, value);
  }

  static void fill_to_aligned_words(HeapWord* to, size_t count, juint value = 0) {
    assert_params_aligned(to);
    pd_fill_to_aligned_words(to, count, value);
  }

  // Fill bytes
  static void fill_to_bytes(void* to, size_t count, jubyte value = 0) {
    pd_fill_to_bytes(to, count, value);
  }

  // Fill a span of memory.  If the span is an integral number of aligned
  // longs, words, or ints, store to those units atomically.
  // The largest atomic transfer unit is 8 bytes, or the largest power
  // of two which divides both to and size, whichever is smaller.
  static void fill_to_memory_atomic(void* to, size_t size, jubyte value = 0);

  // Zero-fill methods

  // Zero word-aligned words, not atomic on each word
  static void zero_to_words(HeapWord* to, size_t count) {
    assert_params_ok(to, HeapWordSize);
    pd_zero_to_words(to, count);
  }

  // Zero bytes
  static void zero_to_bytes(void* to, size_t count) {
    pd_zero_to_bytes(to, count);
  }

 private:
  static bool params_disjoint(const HeapWord* from, HeapWord* to, size_t count) {
    if (from < to) {
      return pointer_delta(to, from) >= count;
    }
    return pointer_delta(from, to) >= count;
  }

  // These methods raise a fatal if they detect a problem.

  static void assert_disjoint(const HeapWord* from, HeapWord* to, size_t count) {
    assert(params_disjoint(from, to, count), "source and dest overlap");
  }

  static void assert_params_ok(const void* from, void* to, intptr_t alignment) {
    assert(is_aligned(from, alignment), "must be aligned: " INTPTR_FORMAT, p2i(from));
    assert(is_aligned(to, alignment),   "must be aligned: " INTPTR_FORMAT, p2i(to));
  }

  static void assert_params_ok(HeapWord* to, intptr_t alignment) {
    assert(is_aligned(to, alignment), "must be aligned: " INTPTR_FORMAT, p2i(to));
  }

  static void assert_params_aligned(const HeapWord* from, HeapWord* to) {
    assert(is_aligned(from, BytesPerLong), "must be aligned: " INTPTR_FORMAT, p2i(from));
    assert(is_aligned(to, BytesPerLong),   "must be aligned: " INTPTR_FORMAT, p2i(to));
  }

  static void assert_params_aligned(HeapWord* to) {
    assert(is_aligned(to, BytesPerLong), "must be aligned: " INTPTR_FORMAT, p2i(to));
  }

  static void assert_byte_count_ok(size_t byte_count, size_t unit_size) {
    assert(is_aligned(byte_count, unit_size), "byte count must be aligned");
  }

  // Platform dependent implementations of the above methods.
#include CPU_HEADER(copy)

};

#endif // SHARE_UTILITIES_COPY_HPP
