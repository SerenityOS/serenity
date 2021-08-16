/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#include "precompiled.hpp"
#include "memory/metaspace/commitMask.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "runtime/stubRoutines.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

namespace metaspace {

CommitMask::CommitMask(const MetaWord* start, size_t word_size) :
  CHeapBitMap(mask_size(word_size, Settings::commit_granule_words())),
  _base(start),
  _word_size(word_size),
  _words_per_bit(Settings::commit_granule_words())
{
  assert(_word_size > 0 && _words_per_bit > 0 &&
         is_aligned(_word_size, _words_per_bit), "Sanity");
}

#ifdef ASSERT

// Given a pointer, check if it points into the range this bitmap covers.
bool CommitMask::is_pointer_valid(const MetaWord* p) const {
  return p >= _base && p < _base + _word_size;
}

// Given a pointer, check if it points into the range this bitmap covers.
void CommitMask::check_pointer(const MetaWord* p) const {
  assert(is_pointer_valid(p),
         "Pointer " PTR_FORMAT " not in range of this bitmap [" PTR_FORMAT ", " PTR_FORMAT ").",
         p2i(p), p2i(_base), p2i(_base + _word_size));
}
// Given a pointer, check if it points into the range this bitmap covers,
// and if it is aligned to commit granule border.
void CommitMask::check_pointer_aligned(const MetaWord* p) const {
  check_pointer(p);
  assert(is_aligned(p, _words_per_bit * BytesPerWord),
         "Pointer " PTR_FORMAT " should be aligned to commit granule size " SIZE_FORMAT ".",
         p2i(p), _words_per_bit * BytesPerWord);
}
// Given a range, check if it points into the range this bitmap covers,
// and if its borders are aligned to commit granule border.
void CommitMask::check_range(const MetaWord* start, size_t word_size) const {
  check_pointer_aligned(start);
  assert(is_aligned(word_size, _words_per_bit),
         "Range " SIZE_FORMAT " should be aligned to commit granule size " SIZE_FORMAT ".",
         word_size, _words_per_bit);
  check_pointer(start + word_size - 1);
}

void CommitMask::verify() const {
  // Walk the whole commit mask.
  // For each 1 bit, check if the associated granule is accessible.
  // For each 0 bit, check if the associated granule is not accessible. Slow mode only.
  assert(_base != NULL && _word_size > 0 && _words_per_bit > 0, "Sanity");
  assert_is_aligned(_base, _words_per_bit * BytesPerWord);
  assert_is_aligned(_word_size, _words_per_bit);
}

#endif // ASSERT

void CommitMask::print_on(outputStream* st) const {
  st->print("commit mask, base " PTR_FORMAT ":", p2i(base()));
  for (idx_t i = 0; i < size(); i++) {
    st->print("%c", at(i) ? 'X' : '-');
  }
  st->cr();
}

} // namespace metaspace

