/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1EDENREGIONS_HPP
#define SHARE_GC_G1_G1EDENREGIONS_HPP

#include "gc/g1/g1RegionsOnNodes.hpp"
#include "gc/g1/heapRegion.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"

class G1EdenRegions {
private:
  int    _length;
  // Sum of used bytes from all retired eden regions.
  // I.e. updated when mutator regions are retired.
  volatile size_t _used_bytes;
  G1RegionsOnNodes  _regions_on_node;

public:
  G1EdenRegions() : _length(0), _used_bytes(0), _regions_on_node() { }

  virtual uint add(HeapRegion* hr) {
    assert(!hr->is_eden(), "should not already be set");
    _length++;
    return _regions_on_node.add(hr);
  }

  void clear() {
    _length = 0;
    _used_bytes = 0;
    _regions_on_node.clear();
  }

  uint length() const { return _length; }
  uint regions_on_node(uint node_index) const { return _regions_on_node.count(node_index); }

  size_t used_bytes() const { return _used_bytes; }

  void add_used_bytes(size_t used_bytes) {
    _used_bytes += used_bytes;
  }
};

#endif // SHARE_GC_G1_G1EDENREGIONS_HPP
