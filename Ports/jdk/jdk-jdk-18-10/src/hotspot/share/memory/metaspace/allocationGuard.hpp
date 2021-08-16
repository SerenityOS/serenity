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

#ifndef SHARE_MEMORY_METASPACE_ALLOCATIONGUARD_HPP
#define SHARE_MEMORY_METASPACE_ALLOCATIONGUARD_HPP

#include "memory/allocation.hpp"
#include "memory/metaspace/chunklevel.hpp"
#include "utilities/globalDefinitions.hpp"

// In Debug builds, Metadata in Metaspace can be optionally guarded - enclosed in canaries -
// to detect memory overwriters.
//
// These canaries are periodically checked, e.g. when the Metaspace is purged in a context
// of a GC.

// The canaries precede any allocated block...
//
// +---------------+
// |  'METAMETA'   |
// +---------------+
// |  block size   |
// +---------------+
// |  block...     |
// .               .
// .               .
// .               .
// |               |
// +---------------+
// . <padding>     .
// +---------------+
// |  'METAMETA'   |
// +---------------+
// |  block size   |
// +---------------+
// |  block...     |

// ... and since the blocks are allocated via pointer bump and closely follow each other,
// one block's prefix is its predecessor's suffix, so apart from the last block all
// blocks have an overwriter canary on both ends.
//

// Note: this feature is only available in debug, and is activated using
//  -XX:+MetaspaceGuardAllocations. When active, it disables deallocation handling - since
//  freeblock handling in the freeblock lists would get too complex - so one may run leaks
//  in deallocation-heavy scenarios (e.g. lots of class redefinitions).
//

namespace metaspace {

#ifdef ASSERT

struct Prefix {
  static const uintx EyeCatcher =
      NOT_LP64(0x77698465) LP64_ONLY(0x7769846577698465ULL); // "META" resp "METAMETA"

  const uintx _mark;
  const size_t _word_size;   // raw word size including prefix
  // MetaWord payload [0];   // varsized (but unfortunately not all our compilers understand that)

  Prefix(size_t word_size) :
    _mark(EyeCatcher),
    _word_size(word_size)
  {}

  MetaWord* payload() const {
    return (MetaWord*)(this + 1);
  }

  bool is_valid() const {
    return _mark == EyeCatcher && _word_size > 0 && _word_size < chunklevel::MAX_CHUNK_WORD_SIZE;
  }

};

// The prefix structure must be aligned to MetaWord size.
STATIC_ASSERT((sizeof(Prefix) & WordAlignmentMask) == 0);

inline size_t prefix_size() {
  return sizeof(Prefix);
}

// Given a pointer to a memory area, establish the prefix at the start of that area and
// return the starting pointer to the payload.
inline MetaWord* establish_prefix(MetaWord* p_raw, size_t raw_word_size) {
  const Prefix* pp = new(p_raw)Prefix(raw_word_size);
  return pp->payload();
}

#endif

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_ALLOCATIONGUARD_HPP
