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

#ifndef SHARE_UTILITIES_FAKERTTISUPPORT_HPP
#define SHARE_UTILITIES_FAKERTTISUPPORT_HPP

#include "utilities/globalDefinitions.hpp"
#include "utilities/debug.hpp"

// Provides support for checked downcasts in a hierarchy of classes.
// The base class provides a member of this type, specialized on that
// base class and an associated tag type.  Tags are small non-negative
// integer values uniquely associated with distinct classes in the
// hierarchy.  A tag type is often an enum type.
//
// The concrete class specifies the concrete tag.
//
// The tag set specifies the set of classes in the derivation
// sequence.  Classes in the derivation sequence add their associated
// tag during construction.  Given the tag associated with a class, an
// object is an instance of that class if the tag is included in the
// object's set of recorded tags.
//
// A tag T is present in a tag set if the T'th bit of the tag set is
// one.
//
// Note: The representation of a tag set being uintx sets an upper
// bound on the size of a class hierarchy this utility can be used
// with.
template<typename T, typename TagType>
class FakeRttiSupport {
  friend class VMStructs;
public:
  // Construct with the indicated concrete tag, and include the
  // concrete tag in the associated tag set.
  explicit FakeRttiSupport(TagType concrete_tag) :
    _tag_set(tag_bit(concrete_tag)), _concrete_tag(concrete_tag) { }

  // Construct with the indicated concrete tag and tag set.
  // Note: This constructor is public only to allow clients to set up
  // "unusual" (or perhaps buggy) fake RTTI configurations.
  FakeRttiSupport(TagType concrete_tag, uintx tag_set) :
    _tag_set(tag_set), _concrete_tag(validate_tag(concrete_tag)) { }

  // Get the concrete tag.
  TagType concrete_tag() const { return _concrete_tag; }

  // Test whether tag is in the tag set.
  bool has_tag(TagType tag) const {
    return (_tag_set & tag_bit(tag)) != 0;
  }

  // Return a new support object which is the same as this, except tag
  // has been added to the tag set.  The tag must not already be
  // present in the tag set.
  FakeRttiSupport add_tag(TagType tag) const {
    uintx tbit = tag_bit(tag);
    assert((_tag_set & tbit) == 0,
           "Tag " UINTX_FORMAT " is already present in tag set: " UINTX_FORMAT,
           (uintx)tag, _tag_set);
    return FakeRttiSupport(_concrete_tag, _tag_set | tbit);
  }

private:
  uintx _tag_set;
  TagType _concrete_tag;

  static uintx tag_bit(TagType tag) {
    return ((uintx)1) << validate_tag(tag);
  }

  static TagType validate_tag(TagType tag) {
    assert(0 <= tag, "Tag " INTX_FORMAT " is negative", (intx)tag);
    assert(tag < BitsPerWord,
           "Tag " UINTX_FORMAT " is too large", (uintx)tag);
    return tag;
  }
};

#endif // SHARE_UTILITIES_FAKERTTISUPPORT_HPP
