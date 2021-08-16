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

#ifndef SHARE_GC_G1_G1HEAPREGIONATTR_HPP
#define SHARE_GC_G1_G1HEAPREGIONATTR_HPP

#include "gc/g1/g1BiasedArray.hpp"
#include "gc/g1/heapRegion.hpp"

// Per-region attributes often used during garbage collection to avoid costly
// lookups for that information all over the place.
struct G1HeapRegionAttr {
public:
#if defined(_M_ARM64)&& defined(_MSC_VER) && _MSC_VER <= 1927
  // workaround for MSCV ARM64 bug
  // https://developercommunity.visualstudio.com/content/problem/1079221/arm64-bad-code-generation-around-signed-char-arith.html
  typedef int32_t region_type_t;
#else
  typedef int8_t region_type_t;
#endif
  typedef uint8_t needs_remset_update_t;

private:
  needs_remset_update_t _needs_remset_update;
  region_type_t _type;

public:
  // Selection of the values for the _type field were driven to micro-optimize the
  // encoding and frequency of the checks.
  // The most common check for a given reference is whether the region is in the
  // collection set or not, and which generation this region is in.
  // The selected encoding allows us to use a single check (> NotInCSet) for the
  // former.
  //
  // The other values are used for objects in regions requiring various special handling,
  // eager reclamation of humongous objects or optional regions.
  static const region_type_t Optional     =  -3;    // The region is optional not in the current collection set.
  static const region_type_t Humongous    =  -2;    // The region is a humongous candidate not in the current collection set.
  static const region_type_t NotInCSet    =  -1;    // The region is not in the collection set.
  static const region_type_t Young        =   0;    // The region is in the collection set and a young region.
  static const region_type_t Old          =   1;    // The region is in the collection set and an old region.
  static const region_type_t Num          =   2;

  G1HeapRegionAttr(region_type_t type = NotInCSet, bool needs_remset_update = false) :
    _needs_remset_update(needs_remset_update), _type(type) {

    assert(is_valid(), "Invalid type %d", _type);
  }

  region_type_t type() const           { return _type; }

  const char* get_type_str() const {
    switch (type()) {
      case Optional: return "Optional";
      case Humongous: return "Humongous";
      case NotInCSet: return "NotInCSet";
      case Young: return "Young";
      case Old: return "Old";
      default: ShouldNotReachHere(); return "";
    }
  }

  bool needs_remset_update() const     { return _needs_remset_update != 0; }

  void set_old()                       { _type = Old; }
  void clear_humongous()               {
    assert(is_humongous() || !is_in_cset(), "must be");
    _type = NotInCSet;
  }
  void set_has_remset(bool value)      { _needs_remset_update = value ? 1 : 0; }

  bool is_in_cset_or_humongous() const { return is_in_cset() || is_humongous(); }
  bool is_in_cset() const              { return type() >= Young; }

  bool is_humongous() const            { return type() == Humongous; }
  bool is_young() const                { return type() == Young; }
  bool is_old() const                  { return type() == Old; }
  bool is_optional() const             { return type() == Optional; }

#ifdef ASSERT
  bool is_default() const              { return type() == NotInCSet; }
  bool is_valid() const                { return (type() >= Optional && type() < Num); }
  bool is_valid_gen() const            { return (type() >= Young && type() <= Old); }
#endif
};

// Table for all regions in the heap for above.
//
// We use this to speed up reference processing during young collection and
// quickly reclaim humongous objects. For the latter, at the start of GC, by adding
// it as a humongous region we enable special handling for that region. During the
// reference iteration closures, when we see a humongous region, we then simply mark
// it as referenced, i.e. live, and remove it from this table to prevent further
// processing on it.
//
// This means that this does NOT completely correspond to the information stored
// in a HeapRegion, but only to what is interesting for the current young collection.
class G1HeapRegionAttrBiasedMappedArray : public G1BiasedMappedArray<G1HeapRegionAttr> {
 protected:
  G1HeapRegionAttr default_value() const { return G1HeapRegionAttr(G1HeapRegionAttr::NotInCSet); }
 public:
  void set_optional(uintptr_t index, bool needs_remset_update) {
    assert(get_by_index(index).is_default(),
           "Region attributes at index " INTPTR_FORMAT " should be default but is %s", index, get_by_index(index).get_type_str());
    set_by_index(index, G1HeapRegionAttr(G1HeapRegionAttr::Optional, needs_remset_update));
  }

  void set_humongous(uintptr_t index, bool needs_remset_update) {
    assert(get_by_index(index).is_default(),
           "Region attributes at index " INTPTR_FORMAT " should be default but is %s", index, get_by_index(index).get_type_str());
    set_by_index(index, G1HeapRegionAttr(G1HeapRegionAttr::Humongous, needs_remset_update));
  }

  void clear_humongous(uintptr_t index) {
    get_ref_by_index(index)->clear_humongous();
  }

  void set_has_remset(uintptr_t index, bool needs_remset_update) {
    get_ref_by_index(index)->set_has_remset(needs_remset_update);
  }

  void set_in_young(uintptr_t index) {
    assert(get_by_index(index).is_default(),
           "Region attributes at index " INTPTR_FORMAT " should be default but is %s", index, get_by_index(index).get_type_str());
    set_by_index(index, G1HeapRegionAttr(G1HeapRegionAttr::Young, true));
  }

  void set_in_old(uintptr_t index, bool needs_remset_update) {
    assert(get_by_index(index).is_default(),
           "Region attributes at index " INTPTR_FORMAT " should be default but is %s", index, get_by_index(index).get_type_str());
    set_by_index(index, G1HeapRegionAttr(G1HeapRegionAttr::Old, needs_remset_update));
  }

  bool is_in_cset_or_humongous(HeapWord* addr) const { return at(addr).is_in_cset_or_humongous(); }
  bool is_in_cset(HeapWord* addr) const { return at(addr).is_in_cset(); }
  bool is_in_cset(const HeapRegion* hr) const { return get_by_index(hr->hrm_index()).is_in_cset(); }
  G1HeapRegionAttr at(HeapWord* addr) const { return get_by_address(addr); }
  void clear() { G1BiasedMappedArray<G1HeapRegionAttr>::clear(); }
  void clear(const HeapRegion* hr) { return set_by_index(hr->hrm_index(), G1HeapRegionAttr(G1HeapRegionAttr::NotInCSet)); }
};

#endif // SHARE_GC_G1_G1HEAPREGIONATTR_HPP
