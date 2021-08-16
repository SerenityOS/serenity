/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1HEAPTRANSITION_HPP
#define SHARE_GC_G1_G1HEAPTRANSITION_HPP

#include "gc/shared/plab.hpp"
#include "memory/metaspaceStats.hpp"

class G1CollectedHeap;

class G1HeapTransition {
  struct Data {
    size_t _eden_length;
    size_t _survivor_length;
    size_t _old_length;
    size_t _archive_length;
    size_t _humongous_length;
    const MetaspaceCombinedStats _meta_sizes;

    // Only includes current eden regions.
    uint* _eden_length_per_node;
    // Only includes current survivor regions.
    uint* _survivor_length_per_node;

    Data(G1CollectedHeap* g1_heap);
    ~Data();
  };

  G1CollectedHeap* _g1_heap;
  Data _before;

public:
  G1HeapTransition(G1CollectedHeap* g1_heap);

  void print();
};

#endif // SHARE_GC_G1_G1HEAPTRANSITION_HPP
