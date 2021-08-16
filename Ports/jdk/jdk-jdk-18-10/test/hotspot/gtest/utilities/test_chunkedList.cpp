/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "unittest.hpp"
#include "utilities/chunkedList.hpp"

class Metadata;

template <typename T>
class TestChunkedList {
  typedef ChunkedList<T, mtOther> ChunkedListT;

 public:

  static void testEmpty() {
    ChunkedListT buffer;
    ASSERT_EQ((size_t) 0, buffer.size());
  }

  static void testFull() {
    ChunkedListT buffer;
    for (uintptr_t i = 0; i < ChunkedListT::BufferSize; i++) {
      buffer.push((T) i);
    }
    ASSERT_EQ((size_t) ChunkedListT::BufferSize, buffer.size());
    ASSERT_TRUE(buffer.is_full());
  }

  static void testSize() {
    ChunkedListT buffer;
    for (uintptr_t i = 0; i < ChunkedListT::BufferSize; i++) {
      ASSERT_EQ((size_t) i, buffer.size());
      buffer.push((T) i);
      ASSERT_EQ((size_t) (i + 1), buffer.size());
    }
  }

  static void testClear() {
    ChunkedListT buffer;

    buffer.clear();
    ASSERT_EQ((size_t) 0, buffer.size());

    for (uintptr_t i = 0; i < ChunkedListT::BufferSize / 2; i++) {
      buffer.push((T) i);
    }
    buffer.clear();
    ASSERT_EQ((size_t) 0, buffer.size());

    for (uintptr_t i = 0; i < ChunkedListT::BufferSize; i++) {
      buffer.push((T) i);
    }
    buffer.clear();
    ASSERT_EQ((size_t) 0, buffer.size());
  }

  static void testAt() {
    ChunkedListT buffer;

    for (uintptr_t i = 0; i < ChunkedListT::BufferSize; i++) {
      buffer.push((T) i);
      ASSERT_EQ((T) i, buffer.at(i));
    }

    for (uintptr_t i = 0; i < ChunkedListT::BufferSize; i++) {
      ASSERT_EQ((T) i, buffer.at(i));
    }
  }
};

TEST(ChunkedList, metadata_empty) {
  TestChunkedList<Metadata*>::testEmpty();
}

TEST(ChunkedList, metadata_full) {
  TestChunkedList<Metadata*>::testFull();
}

TEST(ChunkedList, metadata_size) {
  TestChunkedList<Metadata*>::testSize();
}

TEST(ChunkedList, metadata_clear) {
  TestChunkedList<Metadata*>::testSize();
}

TEST(ChunkedList, metadata_at) {
  TestChunkedList<Metadata*>::testAt();
}

TEST(ChunkedList, size_t_empty) {
  TestChunkedList<size_t>::testEmpty();
}

TEST(ChunkedList, size_t_full) {
  TestChunkedList<size_t>::testFull();
}

TEST(ChunkedList, size_t_size) {
  TestChunkedList<size_t>::testSize();
}

TEST(ChunkedList, size_t_clear) {
  TestChunkedList<size_t>::testSize();
}

TEST(ChunkedList, size_t_at) {
  TestChunkedList<size_t>::testAt();
}
