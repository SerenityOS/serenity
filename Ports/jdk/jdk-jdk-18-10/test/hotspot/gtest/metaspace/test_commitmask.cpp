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
#include "memory/metaspace/metaspaceSettings.hpp"
#include "metaspaceGtestCommon.hpp"
#include "metaspaceGtestRangeHelpers.hpp"
#include "runtime/os.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"

using metaspace::CommitMask;
using metaspace::Settings;

static int get_random(int limit) { return os::random() % limit; }

class CommitMaskTest {
  const MetaWord* const _base;
  const size_t _word_size;

  CommitMask _mask;

  void verify_mask() {
    // Note: we omit the touch test since we operate on fictional
    // memory
    DEBUG_ONLY(_mask.verify();)
  }

  // Return a random sub range within [_base.._base + word_size),
  // aligned to granule size
  const MetaWord* calc_random_subrange(size_t* p_word_size) {
    size_t l1 = get_random((int)_word_size);
    size_t l2 = get_random((int)_word_size);
    if (l1 > l2) {
      size_t l = l1;
      l1 = l2;
      l2 = l;
    }
    l1 = align_down(l1, Settings::commit_granule_words());
    l2 = align_up(l2, Settings::commit_granule_words());

    const MetaWord* p = _base + l1;
    const size_t len = l2 - l1;

    assert(p >= _base && p + len <= _base + _word_size,
           "Sanity");
    *p_word_size = len;

    return p;
  }

  void test1() {

    LOG("test1");

    // Commit everything
    size_t prior_committed = _mask.mark_range_as_committed(_base, _word_size);
    verify_mask();
    ASSERT_LE(prior_committed, _word_size); // We do not really know

    // Commit everything again, should be a noop
    prior_committed = _mask.mark_range_as_committed(_base, _word_size);
    verify_mask();
    ASSERT_EQ(prior_committed, _word_size);

    ASSERT_EQ(_mask.get_committed_size(),
              _word_size);
    ASSERT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
              _word_size);

    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      ASSERT_TRUE(_mask.is_committed_address(p));
    }

    // Now make an uncommitted hole
    size_t sr_word_size;
    const MetaWord* sr_base = calc_random_subrange(&sr_word_size);
    LOG("subrange " PTR_FORMAT "-" PTR_FORMAT ".",
        p2i(sr_base), p2i(sr_base + sr_word_size));

    size_t prior_uncommitted =
        _mask.mark_range_as_uncommitted(sr_base, sr_word_size);
    verify_mask();
    ASSERT_EQ(prior_uncommitted, (size_t)0);

    // Again, for fun, should be a noop now.
    prior_uncommitted = _mask.mark_range_as_uncommitted(sr_base, sr_word_size);
    verify_mask();
    ASSERT_EQ(prior_uncommitted, sr_word_size);

    ASSERT_EQ(_mask.get_committed_size_in_range(sr_base, sr_word_size),
              (size_t)0);
    ASSERT_EQ(_mask.get_committed_size(),
              _word_size - sr_word_size);
    ASSERT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
              _word_size - sr_word_size);
    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      if (p >= sr_base && p < sr_base + sr_word_size) {
        ASSERT_FALSE(_mask.is_committed_address(p));
      } else {
        ASSERT_TRUE(_mask.is_committed_address(p));
      }
    }

    // Recommit whole range
    prior_committed = _mask.mark_range_as_committed(_base, _word_size);
    verify_mask();
    ASSERT_EQ(prior_committed, _word_size - sr_word_size);

    ASSERT_EQ(_mask.get_committed_size_in_range(sr_base, sr_word_size),
              sr_word_size);
    ASSERT_EQ(_mask.get_committed_size(),
              _word_size);
    ASSERT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
              _word_size);
    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      ASSERT_TRUE(_mask.is_committed_address(p));
    }

  }

  void test2() {

    LOG("test2");

    // Uncommit everything
    size_t prior_uncommitted = _mask.mark_range_as_uncommitted(_base, _word_size);
    verify_mask();
    ASSERT_LE(prior_uncommitted, _word_size);

    // Uncommit everything again, should be a noop
    prior_uncommitted = _mask.mark_range_as_uncommitted(_base, _word_size);
    verify_mask();
    ASSERT_EQ(prior_uncommitted, _word_size);

    ASSERT_EQ(_mask.get_committed_size(),
        (size_t)0);
    ASSERT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
        (size_t)0);

    // Now make an committed region
    size_t sr_word_size;
    const MetaWord* sr_base = calc_random_subrange(&sr_word_size);
    LOG("subrange " PTR_FORMAT "-" PTR_FORMAT ".",
        p2i(sr_base), p2i(sr_base + sr_word_size));

    ASSERT_EQ(_mask.get_committed_size_in_range(sr_base, sr_word_size),
              (size_t)0);
    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      ASSERT_FALSE(_mask.is_committed_address(p));
    }

    size_t prior_committed = _mask.mark_range_as_committed(sr_base, sr_word_size);
    verify_mask();
    ASSERT_EQ(prior_committed, (size_t)0);

    // Again, for fun, should be a noop now.
    prior_committed = _mask.mark_range_as_committed(sr_base, sr_word_size);
    verify_mask();
    ASSERT_EQ(prior_committed, sr_word_size);

    ASSERT_EQ(_mask.get_committed_size_in_range(sr_base, sr_word_size),
        sr_word_size);
    ASSERT_EQ(_mask.get_committed_size(),
        sr_word_size);
    ASSERT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
        sr_word_size);
    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      if (p >= sr_base && p < sr_base + sr_word_size) {
        ASSERT_TRUE(_mask.is_committed_address(p));
      } else {
        ASSERT_FALSE(_mask.is_committed_address(p));
      }
    }

    // Re-uncommit whole range
    prior_uncommitted = _mask.mark_range_as_uncommitted(_base, _word_size);
    verify_mask();
    ASSERT_EQ(prior_uncommitted, _word_size - sr_word_size);

    EXPECT_EQ(_mask.get_committed_size_in_range(sr_base, sr_word_size),
        (size_t)0);
    EXPECT_EQ(_mask.get_committed_size(),
        (size_t)0);
    EXPECT_EQ(_mask.get_committed_size_in_range(_base, _word_size),
        (size_t)0);
    for (const MetaWord* p = _base; p < _base + _word_size; p++) {
      ASSERT_FALSE(_mask.is_committed_address(p));
    }

  }

  void test3() {

    // arbitrary ranges are set and cleared and compared with the test map
    TestMap map(_word_size);

    _mask.clear_large();

    for (int run = 0; run < 100; run++) {

      // A random range
      SizeRange r = SizeRange(_word_size).random_aligned_subrange(Settings::commit_granule_words());

      if (os::random() % 100 < 50) {
        _mask.mark_range_as_committed(_base + r.lowest(), r.size());
        map.set_range(r.lowest(), r.end());
      } else {
        _mask.mark_range_as_uncommitted(_base + r.lowest(), r.size());
        map.clear_range(r.lowest(), r.end());
      }

      ASSERT_EQ(_mask.get_committed_size(), (size_t)map.get_num_set());

      ASSERT_EQ(_mask.get_committed_size_in_range(_base + r.lowest(), r.size()),
                (size_t)map.get_num_set(r.lowest(), r.end()));

    }

  }

public:

  CommitMaskTest(const MetaWord* base, size_t size) :
    _base(base),
    _word_size(size),
    _mask(base, size)
  {}

  void test() {
    LOG("mask range: " PTR_FORMAT "-" PTR_FORMAT
         " (" SIZE_FORMAT " words).",
         p2i(_base), p2i(_base + _word_size), _word_size);
    for (int i = 0; i < 5; i++) {
      test1(); test2(); test3();
    }
  }

};

TEST_VM(metaspace, commit_mask_basics) {

  const MetaWord* const base = (const MetaWord*) 0x100000;

  CommitMask mask1(base, Settings::commit_granule_words());
  ASSERT_EQ(mask1.size(), (BitMap::idx_t)1);

  CommitMask mask2(base, Settings::commit_granule_words() * 4);
  ASSERT_EQ(mask2.size(), (BitMap::idx_t)4);

  CommitMask mask3(base, Settings::commit_granule_words() * 43);
  ASSERT_EQ(mask3.size(), (BitMap::idx_t)43);

  mask3.mark_range_as_committed(base, Settings::commit_granule_words());
  mask3.mark_range_as_committed(base + (Settings::commit_granule_words() * 42), Settings::commit_granule_words());

  ASSERT_EQ(mask3.at(0), 1);
  for (int i = 1; i < 42; i++) {
    ASSERT_EQ(mask3.at(i), 0);
  }
  ASSERT_EQ(mask3.at(42), 1);

}

TEST_VM(metaspace, commit_mask_small) {

  const MetaWord* const base = (const MetaWord*) 0x100000;

  CommitMaskTest test(base, Settings::commit_granule_words());
  test.test();

}

TEST_VM(metaspace, commit_mask_range) {

  const MetaWord* const base = (const MetaWord*) 0x100000;
  const size_t len = Settings::commit_granule_words() * 4;
  const MetaWord* const end = base + len;
  CommitMask mask(base, len);

  LOG("total range: " PTR_FORMAT "-" PTR_FORMAT "\n", p2i(base), p2i(end));

  size_t l = mask.mark_range_as_committed(base, len);
  ASSERT_LE(l, len);

  for (const MetaWord* p = base; p <= end - Settings::commit_granule_words();
       p += Settings::commit_granule_words()) {
    for (const MetaWord* p2 = p + Settings::commit_granule_words();
         p2 <= end; p2 += Settings::commit_granule_words()) {
      LOG(PTR_FORMAT "-" PTR_FORMAT "\n", p2i(p), p2i(p2));
      EXPECT_EQ(mask.get_committed_size_in_range(p, p2 - p),
                (size_t)(p2 - p));
    }
  }

  l = mask.mark_range_as_uncommitted(base, len);
  ASSERT_EQ(l, (size_t)0);

  for (const MetaWord* p = base; p <= end - Settings::commit_granule_words();
       p += Settings::commit_granule_words()) {
    for (const MetaWord* p2 = p + Settings::commit_granule_words();
         p2 <= end; p2 += Settings::commit_granule_words()) {
      LOG(PTR_FORMAT "-" PTR_FORMAT "\n", p2i(p), p2i(p2));
      EXPECT_EQ(mask.get_committed_size_in_range(p, p2 - p),
                (size_t)(0));
    }
  }

}

TEST_VM(metaspace, commit_mask_random) {

  for (int i = 0; i < 5; i++) {

    // make up a range out of thin air
    const MetaWord* const base =
        align_down( (const MetaWord*) ((uintptr_t) os::random() * os::random()),
                    Settings::commit_granule_bytes());
    const size_t len = align_up( 1 + (os::random() % M),
                    Settings::commit_granule_words());

    CommitMaskTest test(base, len);
    test.test();

  }

}
