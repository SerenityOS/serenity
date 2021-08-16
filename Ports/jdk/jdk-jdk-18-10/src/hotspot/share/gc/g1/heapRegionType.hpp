/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_HEAPREGIONTYPE_HPP
#define SHARE_GC_G1_HEAPREGIONTYPE_HPP

#include "gc/g1/g1HeapRegionTraceType.hpp"

#define hrt_assert_is_valid(tag) \
  assert(is_valid((tag)), "invalid HR type: %u", (uint) (tag))

class HeapRegionType {
friend class VMStructs;

private:
  // We encode the value of the heap region type so the generation can be
  // determined quickly. The tag is split into two parts:
  //
  //   major type (young, old, humongous, archive)           : top N-1 bits
  //   minor type (eden / survivor, starts / cont hum, etc.) : bottom 1 bit
  //
  // If there's need to increase the number of minor types in the
  // future, we'll have to increase the size of the latter and hence
  // decrease the size of the former.
  //
  // 00000 0 [ 0] Free
  //
  // 00001 0 [ 2] Young Mask
  // 00001 0 [ 2] Eden
  // 00001 1 [ 3] Survivor
  //
  // 00010 0 [ 4] Humongous Mask
  // 00100 0 [ 8] Pinned Mask
  // 00110 0 [12] Starts Humongous
  // 00110 1 [13] Continues Humongous
  //
  // 01000 0 [16] Old Mask
  //
  // 10000 0 [32] Archive Mask
  // 11100 0 [56] Open Archive
  // 11100 1 [57] Closed Archive
  //
  typedef enum {
    FreeTag               = 0,

    YoungMask             = 2,
    EdenTag               = YoungMask,
    SurvTag               = YoungMask + 1,

    HumongousMask         = 4,
    PinnedMask            = 8,
    StartsHumongousTag    = HumongousMask | PinnedMask,
    ContinuesHumongousTag = HumongousMask | PinnedMask + 1,

    OldMask               = 16,
    OldTag                = OldMask,

    // Archive regions are regions with immutable content (i.e. not reclaimed, and
    // not allocated into during regular operation). They differ in the kind of references
    // allowed for the contained objects:
    // - Closed archive regions form a separate self-contained (closed) object graph
    // within the set of all of these regions. No references outside of closed
    // archive regions are allowed.
    // - Open archive regions have no restrictions on the references of their objects.
    // Objects within these regions are allowed to have references to objects
    // contained in any other kind of regions.
    ArchiveMask           = 32,
    OpenArchiveTag        = ArchiveMask | PinnedMask,
    ClosedArchiveTag      = ArchiveMask | PinnedMask + 1
  } Tag;

  volatile Tag _tag;

  static bool is_valid(Tag tag);

  Tag get() const {
    hrt_assert_is_valid(_tag);
    return _tag;
  }

  // Sets the type to 'tag'.
  void set(Tag tag) {
    hrt_assert_is_valid(tag);
    hrt_assert_is_valid(_tag);
    _tag = tag;
  }

  // Sets the type to 'tag', expecting the type to be 'before'. This
  // is available for when we want to add sanity checking to the type
  // transition.
  void set_from(Tag tag, Tag before) {
    hrt_assert_is_valid(tag);
    hrt_assert_is_valid(before);
    hrt_assert_is_valid(_tag);
    assert(_tag == before, "HR tag: %u, expected: %u new tag; %u", _tag, before, tag);
    _tag = tag;
  }

  // Private constructor used for static constants
  HeapRegionType(Tag t) : _tag(t) { hrt_assert_is_valid(_tag); }

public:
  // Queries

  bool is_free() const { return get() == FreeTag; }

  bool is_young()    const { return (get() & YoungMask) != 0; }
  bool is_eden()     const { return get() == EdenTag;  }
  bool is_survivor() const { return get() == SurvTag;  }

  bool is_humongous()           const { return (get() & HumongousMask) != 0;   }
  bool is_starts_humongous()    const { return get() == StartsHumongousTag;    }
  bool is_continues_humongous() const { return get() == ContinuesHumongousTag; }

  bool is_archive()        const { return (get() & ArchiveMask) != 0; }
  bool is_open_archive()   const { return get() == OpenArchiveTag; }
  bool is_closed_archive() const { return get() == ClosedArchiveTag; }

  // is_old regions may or may not also be pinned
  bool is_old() const { return (get() & OldMask) != 0; }

  bool is_old_or_humongous() const { return (get() & (OldMask | HumongousMask)) != 0; }

  bool is_old_or_humongous_or_archive() const { return (get() & (OldMask | HumongousMask | ArchiveMask)) != 0; }

  // is_pinned regions may be archive or humongous
  bool is_pinned() const { return (get() & PinnedMask) != 0; }

  // Setters

  void set_free() { set(FreeTag); }

  void set_eden()        { set_from(EdenTag, FreeTag); }
  void set_eden_pre_gc() { set_from(EdenTag, SurvTag); }
  void set_survivor()    { set_from(SurvTag, FreeTag); }

  void set_starts_humongous()    { set_from(StartsHumongousTag,    FreeTag); }
  void set_continues_humongous() { set_from(ContinuesHumongousTag, FreeTag); }

  void set_old() { set(OldTag); }

  // Change the current region type to be of an old region type if not already done so.
  // Returns whether the region type has been changed or not.
  bool relabel_as_old() {
    //assert(!is_free(), "Should not try to move Free region");
    assert(!is_humongous(), "Should not try to move Humongous region");
    if (is_old()) {
      return false;
    }
    if (is_eden()) {
      set_from(OldTag, EdenTag);
      return true;
    } else if (is_free()) {
      set_from(OldTag, FreeTag);
      return true;
    } else {
      set_from(OldTag, SurvTag);
      return true;
    }
  }
  void set_open_archive()   { set_from(OpenArchiveTag, FreeTag); }
  void set_closed_archive() { set_from(ClosedArchiveTag, FreeTag); }

  // Misc

  const char* get_str() const;
  const char* get_short_str() const;
  G1HeapRegionTraceType::Type get_trace_type();

  HeapRegionType() : _tag(FreeTag) { hrt_assert_is_valid(_tag); }

  static const HeapRegionType Eden;
  static const HeapRegionType Survivor;
  static const HeapRegionType Old;
  static const HeapRegionType Humongous;
};

#endif // SHARE_GC_G1_HEAPREGIONTYPE_HPP
