/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_PRESERVEDMARKS_INLINE_HPP
#define SHARE_GC_SHARED_PRESERVEDMARKS_INLINE_HPP

#include "gc/shared/preservedMarks.hpp"

#include "logging/log.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/stack.inline.hpp"

inline bool PreservedMarks::should_preserve_mark(oop obj, markWord m) const {
  return obj->mark_must_be_preserved_for_promotion_failure(m);
}

inline void PreservedMarks::push(oop obj, markWord m) {
  assert(should_preserve_mark(obj, m), "pre-condition");
  OopAndMarkWord elem(obj, m);
  _stack.push(elem);
}

inline void PreservedMarks::push_if_necessary(oop obj, markWord m) {
  if (should_preserve_mark(obj, m)) {
    push(obj, m);
  }
}

inline void PreservedMarks::init_forwarded_mark(oop obj) {
  obj->init_mark();
}

inline PreservedMarks::PreservedMarks()
    : _stack(OopAndMarkWordStack::default_segment_size(),
             // This stack should be used very infrequently so there's
             // no point in caching stack segments (there will be a
             // waste of space most of the time). So we set the max
             // cache size to 0.
             0 /* max_cache_size */) { }

void PreservedMarks::OopAndMarkWord::set_mark() const {
  _o->set_mark(_m);
}

#endif // SHARE_GC_SHARED_PRESERVEDMARKS_INLINE_HPP
