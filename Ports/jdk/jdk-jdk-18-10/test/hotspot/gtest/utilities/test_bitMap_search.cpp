/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/bitMap.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "unittest.hpp"

typedef BitMap::idx_t idx_t;
typedef BitMap::bm_word_t bm_word_t;

static const idx_t BITMAP_SIZE = 1024;

static const size_t search_chunk_size = 64;

// Entries must be monotonically increasing.
// Maximum entry must be < search_chunk_size.
// Cluster values around possible word-size boundaries.
static const size_t search_offsets[] =
  { 0, 1, 2, 29, 30, 31, 32, 33, 34, 60, 62, 63 };

static const size_t search_noffsets = ARRAY_SIZE(search_offsets);

static const size_t search_nchunks = BITMAP_SIZE / search_chunk_size;
STATIC_ASSERT(search_nchunks * search_chunk_size == BITMAP_SIZE);

namespace {
class TestIteratorFn : public BitMapClosure {
public:
  TestIteratorFn(size_t start, size_t end, size_t left, size_t right);
  virtual bool do_bit(size_t offset);

private:
  size_t _entries[2];
  size_t _index;
  size_t _count;
  size_t _start;
  size_t _end;
  size_t _left;
  size_t _right;

  void do_bit_aux(size_t offset);

};
} // anonymous namespace

TestIteratorFn::TestIteratorFn(size_t start, size_t end,
                               size_t left, size_t right) :
  _index(0), _count(0), _start(start), _end(end), _left(left), _right(right)
{
  if ((_start <= _left) && (_left < _end)) {
    _entries[_count++] = _left;
  }
  if ((_start <= _right) && (_right < _end)) {
    _entries[_count++] = _right;
  }
}

void TestIteratorFn::do_bit_aux(size_t offset) {
  EXPECT_LT(_index, _count);
  if (_index < _count) {
    EXPECT_EQ(_entries[_index], offset);
    _index += 1;
  }
}

bool TestIteratorFn::do_bit(size_t offset) {
  do_bit_aux(offset);
  return true;
}

static idx_t compute_expected(idx_t search_start,
                              idx_t search_end,
                              idx_t left_bit,
                              idx_t right_bit) {
  idx_t expected = search_end;
  if (search_start <= left_bit) {
    if (left_bit < search_end) {
      expected = left_bit;
    }
  } else if (search_start <= right_bit) {
    if (right_bit < search_end) {
      expected = right_bit;
    }
  }
  return expected;
}

static void test_search_ranges(BitMap& test_ones,
                               BitMap& test_zeros,
                               idx_t left,
                               idx_t right) {
  // Test get_next_one_offset with full range of map.
  EXPECT_EQ(left, test_ones.get_next_one_offset(0));
  EXPECT_EQ(right, test_ones.get_next_one_offset(left + 1));
  EXPECT_EQ(BITMAP_SIZE, test_ones.get_next_one_offset(right + 1));

  // Test get_next_one_offset_aligned_right with full range of map.
  EXPECT_EQ(left, test_ones.get_next_one_offset_aligned_right(0, BITMAP_SIZE));
  EXPECT_EQ(right, test_ones.get_next_one_offset_aligned_right(left + 1, BITMAP_SIZE));
  EXPECT_EQ(BITMAP_SIZE, test_ones.get_next_one_offset_aligned_right(right + 1, BITMAP_SIZE));

  // Test get_next_zero_offset with full range of map.
  EXPECT_EQ(left, test_zeros.get_next_zero_offset(0));
  EXPECT_EQ(right, test_zeros.get_next_zero_offset(left + 1));
  EXPECT_EQ(BITMAP_SIZE, test_zeros.get_next_zero_offset(right + 1));

  // Check that iterate invokes the closure function on left and right values.
  TestIteratorFn test_iteration(0, BITMAP_SIZE, left, right);
  test_ones.iterate(&test_iteration, 0, BITMAP_SIZE);

  // Test searches with various start and end ranges.
  for (size_t c_start = 0; c_start < search_nchunks; ++c_start) {
    for (size_t o_start = 0; o_start < search_noffsets; ++o_start) {
      idx_t start = c_start * search_chunk_size + search_offsets[o_start];
      // Terminate start iteration if start is more than two full
      // chunks beyond left.  There isn't anything new to learn by
      // continuing the iteration, and this noticably reduces the
      // time to run the test.
      if (left + 2 * search_chunk_size < start) {
        c_start = search_nchunks; // Set to limit to terminate iteration.
        break;
      }

      for (size_t c_end = c_start; c_end < search_nchunks; ++c_end) {
        for (size_t o_end = (c_start == c_end) ? o_start : 0;
             o_end < search_noffsets;
             ++o_end) {
          idx_t end = c_end * search_chunk_size + search_offsets[o_end];
          // Similarly to start and left, terminate end iteration if
          // end is more than two full chunks beyond right.
          if (right + 2 * search_chunk_size < end) {
            c_end = search_nchunks; // Set to limit to terminate iteration.
            break;
          }
          // Skip this chunk if right is much larger than max(left, start)
          // and this chunk is one of many similar chunks in between,
          // again to reduce testing time.
          if (MAX2(start, left) + 2 * search_chunk_size < end) {
            if (end + 2 * search_chunk_size < right) {
              break;
            }
          }

          bool aligned_right = search_offsets[o_end] == 0;
          ASSERT_LE(start, end);       // test bug if fail
          ASSERT_LT(end, BITMAP_SIZE); // test bug if fail

          idx_t expected = compute_expected(start, end, left, right);

          EXPECT_EQ(expected, test_ones.get_next_one_offset(start, end));
          EXPECT_EQ(expected, test_zeros.get_next_zero_offset(start, end));
          if (aligned_right) {
            EXPECT_EQ(
              expected,
              test_ones.get_next_one_offset_aligned_right(start, end));
          }

          idx_t start2 = MIN2(expected + 1, end);
          idx_t expected2 = compute_expected(start2, end, left, right);

          EXPECT_EQ(expected2, test_ones.get_next_one_offset(start2, end));
          EXPECT_EQ(expected2, test_zeros.get_next_zero_offset(start2, end));
          if (aligned_right) {
            EXPECT_EQ(
              expected2,
              test_ones.get_next_one_offset_aligned_right(start2, end));
          }
        }
      }
    }
  }
}

TEST(BitMap, search) {
  CHeapBitMap test_ones(BITMAP_SIZE);
  CHeapBitMap test_zeros(BITMAP_SIZE);

  // test_ones is used to test searching for 1s in a region of 0s.
  // test_zeros is used to test searching for 0s in a region of 1s.
  test_ones.clear_range(0, test_ones.size());
  test_zeros.set_range(0, test_zeros.size());

  // Searching "empty" sequence should return size.
  EXPECT_EQ(BITMAP_SIZE, test_ones.get_next_one_offset(0));
  EXPECT_EQ(BITMAP_SIZE, test_zeros.get_next_zero_offset(0));

  // With left being in the first or second chunk...
  for (size_t c_left = 0; c_left < 2; ++c_left) {
    // Right bit is in the same chunk as left, or next chunk, or far away...
    for (size_t c_right = c_left;
         c_right < search_nchunks;
         (c_right == c_left + 1) ? c_right = search_nchunks - 1 : ++c_right) {
      // For each offset within the left chunk...
      for (size_t o_left = 0; o_left < search_noffsets; ++o_left) {
        // left is start of left chunk + offset.
        idx_t left = c_left * search_chunk_size + search_offsets[o_left];

        // Install the left bit.
        test_ones.set_bit(left);
        test_zeros.clear_bit(left);
        EXPECT_TRUE(test_ones.at(left));
        EXPECT_FALSE(test_zeros.at(left));

        // For each offset within the right chunk and > left...
        for (size_t o_right = (c_left == c_right) ? o_left + 1 : 0;
             o_right < search_noffsets;
             ++o_right) {
          // right is start of right chunk + offset.
          idx_t right = c_right * search_chunk_size + search_offsets[o_right];

          // Install the right bit.
          test_ones.set_bit(right);
          test_zeros.clear_bit(right);
          EXPECT_TRUE(test_ones.at(right));
          EXPECT_FALSE(test_zeros.at(right));

          // Apply the test.
          test_search_ranges(test_ones, test_zeros, left, right);

          // Remove the right bit.
          test_ones.clear_bit(right);
          test_zeros.set_bit(right);
          EXPECT_FALSE(test_ones.at(right));
          EXPECT_TRUE(test_zeros.at(right));
        }

        // Remove the left bit.
        test_ones.clear_bit(left);
        test_zeros.set_bit(left);
        EXPECT_FALSE(test_ones.at(left));
        EXPECT_TRUE(test_zeros.at(left));
      }
    }
  }
}
