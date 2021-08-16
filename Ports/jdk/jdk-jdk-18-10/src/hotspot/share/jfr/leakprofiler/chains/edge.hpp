/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_EDGE_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_EDGE_HPP

#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"
#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"

class Edge {
 protected:
  const Edge* _parent;
  UnifiedOopRef _reference;
 public:
  Edge(const Edge* parent, UnifiedOopRef reference);

  UnifiedOopRef reference() const {
    return _reference;
  }
  const Edge* parent() const {
    return _parent;
  }
  bool is_root() const {
    return _parent == NULL;
  }
  const oop pointee() const;
  const oop reference_owner() const;
  size_t distance_to_root() const;

  void* operator new (size_t sz, void* here) {
    return here;
  }
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_EDGE_HPP
