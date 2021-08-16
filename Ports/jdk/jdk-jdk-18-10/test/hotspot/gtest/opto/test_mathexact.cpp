/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "opto/mulnode.hpp"
#include "opto/mathexactnode.hpp"
#include "unittest.hpp"

TEST_VM(opto, mathexact) {
  ASSERT_FALSE(OverflowMulLNode::is_overflow(1, 1));
  ASSERT_FALSE(OverflowMulLNode::is_overflow(1, min_jlong));
  ASSERT_TRUE(OverflowMulLNode::is_overflow(-1, min_jlong));
  ASSERT_FALSE(OverflowMulLNode::is_overflow(-1, max_jlong));
  ASSERT_TRUE(OverflowMulLNode::is_overflow(max_jlong / 2 + 1, 2));
  ASSERT_FALSE(OverflowMulLNode::is_overflow(min_jlong, 0));
  ASSERT_FALSE(OverflowMulLNode::is_overflow(min_jlong + 1, -1));
  ASSERT_TRUE(OverflowMulLNode::is_overflow(min_jlong + 1, 2));
}

