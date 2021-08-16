/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/g1/g1CommittedRegionMap.inline.hpp"
#include "runtime/os.hpp"
#include "unittest.hpp"

class G1CommittedRegionMapSerial : public G1CommittedRegionMap {
public:
  static const uint TestRegions = 512;

  void verify_counts() {
    verify_active_count(0, TestRegions, num_active());
    verify_inactive_count(0, TestRegions, num_inactive());
  }

protected:
  void guarantee_mt_safety_active() const { }
  void guarantee_mt_safety_inactive() const { }
};

static bool mutate() {
  return os::random() % 2 == 0;
}

static void generate_random_map(G1CommittedRegionMap* map) {
  for (uint i = 0; i < G1CommittedRegionMapSerial::TestRegions; i++) {
    if (mutate()) {
      map->activate(i, i+1);
    }
  }

  if (map->num_active() == 0) {
    // If we randomly activated 0 regions, activate the first half
    // to have some regions to test.
    map->activate(0, G1CommittedRegionMapSerial::TestRegions / 2);
  }
}

static void random_deactivate(G1CommittedRegionMap* map) {
  uint current_offset = 0;
  do {
    HeapRegionRange current = map->next_active_range(current_offset);
    if (mutate()) {
      if (current.length() < 5) {
        // For short ranges, deactivate whole.
        map->deactivate(current.start(), current.end());
      } else {
        // For larger ranges, deactivate half.
        map->deactivate(current.start(), current.end() - (current.length() / 2));
      }
    }
    current_offset = current.end();
  } while (current_offset != G1CommittedRegionMapSerial::TestRegions);
}

static void random_uncommit_or_reactive(G1CommittedRegionMap* map) {
  uint current_offset = 0;
  do {
    HeapRegionRange current = map->next_inactive_range(current_offset);
    // Randomly either reactivate or uncommit
    if (mutate()) {
      map->reactivate(current.start(), current.end());
    } else {
      map->uncommit(current.start(), current.end());
    }

    current_offset = current.end();
  } while (current_offset != G1CommittedRegionMapSerial::TestRegions);
}

static void random_activate_free(G1CommittedRegionMap* map) {
  uint current_offset = 0;
  do {
    HeapRegionRange current = map->next_committable_range(current_offset);
    // Randomly either reactivate or uncommit
    if (mutate()) {
      if (current.length() < 5) {
        // For short ranges, deactivate whole.
        map->activate(current.start(), current.end());
      } else {
        // For larger ranges, deactivate half.
        map->activate(current.start(), current.end() - (current.length() / 2));
      }
    }

    current_offset = current.end();
  } while (current_offset != G1CommittedRegionMapSerial::TestRegions);
}

TEST(G1CommittedRegionMapTest, serial) {
  G1CommittedRegionMapSerial serial_map;
  serial_map.initialize(G1CommittedRegionMapSerial::TestRegions);

  // Activate some regions
  generate_random_map(&serial_map);

  // Work through the map and mutate it
  for (int i = 0; i < 500; i++) {
    random_deactivate(&serial_map);
    serial_map.verify_counts();
    random_uncommit_or_reactive(&serial_map);
    serial_map.verify_counts();
    random_activate_free(&serial_map);
    serial_map.verify_counts();
    ASSERT_EQ(serial_map.num_inactive(), 0u);
  }
}