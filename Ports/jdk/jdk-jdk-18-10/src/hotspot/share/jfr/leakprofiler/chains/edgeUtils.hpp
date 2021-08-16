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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_EDGEUTILS_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_EDGEUTILS_HPP

#include "memory/allocation.hpp"

class Edge;
class Symbol;

class EdgeUtils : public AllStatic {
 public:
  static const size_t leak_context = 100;
  static const size_t root_context = 100;
  static const size_t max_ref_chain_depth = leak_context + root_context;

  static bool is_leak_edge(const Edge& edge);
  static const Edge* root(const Edge& edge);
  static const Edge* ancestor(const Edge& edge, size_t distance);

  static bool is_array_element(const Edge& edge);
  static int array_index(const Edge& edge);
  static int array_size(const Edge& edge);

  static const Symbol* field_name(const Edge& edge, jshort* modifiers);

};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_EDGEUTILS_HPP
