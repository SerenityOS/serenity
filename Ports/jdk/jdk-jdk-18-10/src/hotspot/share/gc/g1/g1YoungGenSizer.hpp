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

#ifndef SHARE_GC_G1_G1YOUNGGENSIZER_HPP
#define SHARE_GC_G1_G1YOUNGGENSIZER_HPP

#include "utilities/globalDefinitions.hpp"

// There are three command line options related to the young gen size:
// NewSize, MaxNewSize and NewRatio (There is also -Xmn, but that is
// just a short form for NewSize==MaxNewSize). G1 will use its internal
// heuristics to calculate the actual young gen size, so these options
// basically only limit the range within which G1 can pick a young gen
// size. Also, these are general options taking byte sizes. G1 will
// internally work with a number of regions instead. So, some rounding
// will occur.
//
// If nothing related to the the young gen size is set on the command
// line we should allow the young gen to be between G1NewSizePercent
// and G1MaxNewSizePercent of the heap size. This means that every time
// the heap size changes, the limits for the young gen size will be
// recalculated.
//
// If only -XX:NewSize is set we should use the specified value as the
// minimum size for young gen. Still using G1MaxNewSizePercent of the
// heap as maximum.
//
// If only -XX:MaxNewSize is set we should use the specified value as the
// maximum size for young gen. Still using G1NewSizePercent of the heap
// as minimum.
//
// If -XX:NewSize and -XX:MaxNewSize are both specified we use these values.
// No updates when the heap size changes. There is a special case when
// NewSize==MaxNewSize. This is interpreted as "fixed" and will use a
// different heuristic for calculating the collection set when we do mixed
// collection.
//
// If only -XX:NewRatio is set we should use the specified ratio of the heap
// as both min and max. This will be interpreted as "fixed" just like the
// NewSize==MaxNewSize case above. But we will update the min and max
// every time the heap size changes.
//
// NewSize and MaxNewSize override NewRatio. So, NewRatio is ignored if it is
// combined with either NewSize or MaxNewSize. (A warning message is printed.)
class G1YoungGenSizer {
private:
  enum SizerKind {
    SizerDefaults,
    SizerNewSizeOnly,
    SizerMaxNewSizeOnly,
    SizerMaxAndNewSize,
    SizerNewRatio
  };
  SizerKind _sizer_kind;

  // False when using a fixed young generation size due to command-line options,
  // true otherwise.
  bool _use_adaptive_sizing;

  uint _min_desired_young_length;
  uint _max_desired_young_length;

  uint calculate_default_min_length(uint new_number_of_heap_regions);
  uint calculate_default_max_length(uint new_number_of_heap_regions);

  // Update the given values for minimum and maximum young gen length in regions
  // given the number of heap regions depending on the kind of sizing algorithm.
  void recalculate_min_max_young_length(uint number_of_heap_regions, uint* min_young_length, uint* max_young_length);

public:
  G1YoungGenSizer();
  // Calculate the maximum length of the young gen given the number of regions
  // depending on the sizing algorithm.
  virtual void adjust_max_new_size(uint number_of_heap_regions);

  virtual void heap_size_changed(uint new_number_of_heap_regions);
  uint min_desired_young_length() const {
    return _min_desired_young_length;
  }
  uint max_desired_young_length() const {
    return _max_desired_young_length;
  }

  bool use_adaptive_young_list_length() const {
    return _use_adaptive_sizing;
  }
};

#endif // SHARE_GC_G1_G1YOUNGGENSIZER_HPP
