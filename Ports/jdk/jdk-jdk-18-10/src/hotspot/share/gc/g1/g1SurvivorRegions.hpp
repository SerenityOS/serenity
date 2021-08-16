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

#ifndef SHARE_GC_G1_G1SURVIVORREGIONS_HPP
#define SHARE_GC_G1_G1SURVIVORREGIONS_HPP

#include "gc/g1/g1RegionsOnNodes.hpp"
#include "runtime/globals.hpp"

template <typename T>
class GrowableArray;
class HeapRegion;

class G1SurvivorRegions {
private:
  GrowableArray<HeapRegion*>* _regions;
  volatile size_t             _used_bytes;
  G1RegionsOnNodes            _regions_on_node;

public:
  G1SurvivorRegions();

  virtual uint add(HeapRegion* hr);

  void convert_to_eden();

  void clear();

  uint length() const;
  uint regions_on_node(uint node_index) const;

  const GrowableArray<HeapRegion*>* regions() const {
    return _regions;
  }

  // Used bytes of all survivor regions.
  size_t used_bytes() const { return _used_bytes; }

  void add_used_bytes(size_t used_bytes);
};

#endif // SHARE_GC_G1_G1SURVIVORREGIONS_HPP
