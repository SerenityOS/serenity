/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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

#ifndef SHARE_MEMORY_METASPACE_METASPACEARENAGROWTHPOLICY_HPP
#define SHARE_MEMORY_METASPACE_METASPACEARENAGROWTHPOLICY_HPP

#include "memory/metaspace.hpp" // For Metaspace::MetaspaceType
#include "memory/metaspace/chunklevel.hpp"
#include "utilities/debug.hpp"

namespace metaspace {

// ArenaGrowthPolicy encodes the growth policy of a MetaspaceArena.
//
// These arenas grow in steps (by allocating new chunks). The coarseness of growth
// (chunk size, level) depends on what the arena is used for. Used for a class loader
// which is expected to load only one or very few classes should grow in tiny steps.
// For normal classloaders, it can grow in coarser steps, and arenas used by
// the boot loader will grow in even larger steps since we expect it to load a lot of
// classes.
// Note that when growing in large steps (in steps larger than a commit granule,
// by default 64K), costs diminish somewhat since we do not commit the whole space
// immediately.

class ArenaGrowthPolicy {

  // const array specifying chunk level allocation progression (growth steps). Last
  //  chunk is to be an endlessly repeated allocation.
  const chunklevel_t* const _entries;
  const int _num_entries;

public:

  ArenaGrowthPolicy(const chunklevel_t* array, int num_entries) :
    _entries(array),
    _num_entries(num_entries)
  {
    assert(_num_entries > 0, "must not be empty.");
  }

  chunklevel_t get_level_at_step(int num_allocated) const {
    if (num_allocated >= _num_entries) {
      // Caller shall repeat last allocation
      return _entries[_num_entries - 1];
    }
    return _entries[num_allocated];
  }

  // Given a space type, return the correct policy to use.
  // The returned object is static and read only.
  static const ArenaGrowthPolicy* policy_for_space_type(Metaspace::MetaspaceType space_type, bool is_class);

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METASPACEARENAGROWTHPOLICY_HPP
