/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1BUFFERNODELIST_HPP
#define SHARE_GC_G1_G1BUFFERNODELIST_HPP

#include "utilities/globalDefinitions.hpp"

class BufferNode;

struct G1BufferNodeList {
  BufferNode* _head;            // First node in list or NULL if empty.
  BufferNode* _tail;            // Last node in list or NULL if empty.
  size_t _entry_count;          // Sum of entries in nodes in list.

  G1BufferNodeList();
  G1BufferNodeList(BufferNode* head, BufferNode* tail, size_t entry_count);
};

#endif // SHARE_GC_G1_G1BUFFERNODELIST_HPP

