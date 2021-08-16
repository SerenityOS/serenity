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
 */

#include "precompiled.hpp"
#include "gc/z/zLiveMap.inline.hpp"
#include "unittest.hpp"

class ZLiveMapTest : public ::testing::Test {
protected:
  static void strongly_live_for_large_zpage() {
    // Large ZPages only have room for one object.
    ZLiveMap livemap(1);

    bool inc_live;
    uintptr_t object = 0u;

    // Mark the object strong.
    livemap.set(object, false /* finalizable */, inc_live);

    // Check that both bits are in the same segment.
    ASSERT_EQ(livemap.index_to_segment(0), livemap.index_to_segment(1));

    // Check that the object was marked.
    ASSERT_TRUE(livemap.get(0));

    // Check that the object was strongly marked.
    ASSERT_TRUE(livemap.get(1));

    ASSERT_TRUE(inc_live);
  }
};

TEST_F(ZLiveMapTest, strongly_live_for_large_zpage) {
  strongly_live_for_large_zpage();
}
