/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_REFERENCEPROCESSORSTATS_HPP
#define SHARE_GC_SHARED_REFERENCEPROCESSORSTATS_HPP

#include "utilities/globalDefinitions.hpp"

class ReferenceProcessor;

// ReferenceProcessorStats contains statistics about how many references that
// have been traversed when processing references during garbage collection.
class ReferenceProcessorStats {
  size_t _soft_count;
  size_t _weak_count;
  size_t _final_count;
  size_t _phantom_count;

 public:
  ReferenceProcessorStats() :
    _soft_count(0),
    _weak_count(0),
    _final_count(0),
    _phantom_count(0) {}

  ReferenceProcessorStats(size_t soft_count,
                          size_t weak_count,
                          size_t final_count,
                          size_t phantom_count) :
    _soft_count(soft_count),
    _weak_count(weak_count),
    _final_count(final_count),
    _phantom_count(phantom_count)
  {}

  size_t soft_count() const {
    return _soft_count;
  }

  size_t weak_count() const {
    return _weak_count;
  }

  size_t final_count() const {
    return _final_count;
  }

  size_t phantom_count() const {
    return _phantom_count;
  }
};
#endif // SHARE_GC_SHARED_REFERENCEPROCESSORSTATS_HPP
