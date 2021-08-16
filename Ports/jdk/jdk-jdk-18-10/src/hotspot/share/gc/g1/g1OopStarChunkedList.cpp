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

#include "precompiled.hpp"
#include "gc/g1/g1OopStarChunkedList.inline.hpp"

G1OopStarChunkedList::~G1OopStarChunkedList() {
  delete_list(_roots);
  delete_list(_croots);
  delete_list(_oops);
  delete_list(_coops);
}

size_t G1OopStarChunkedList::oops_do(OopClosure* obj_cl, OopClosure* root_cl) {
  size_t result = 0;
  result += chunks_do(_roots, root_cl);
  result += chunks_do(_croots, root_cl);
  result += chunks_do(_oops, obj_cl);
  result += chunks_do(_coops, obj_cl);
  return result;
}
