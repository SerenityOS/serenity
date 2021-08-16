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

#ifndef SHARE_GC_SHARED_REFERENCEPROCESSOR_INLINE_HPP
#define SHARE_GC_SHARED_REFERENCEPROCESSOR_INLINE_HPP

#include "gc/shared/referenceProcessor.hpp"

#include "oops/compressedOops.inline.hpp"
#include "oops/oop.hpp"

oop DiscoveredList::head() const {
  return UseCompressedOops ?  CompressedOops::decode(_compressed_head) :
    _oop_head;
}

void DiscoveredList::set_head(oop o) {
  if (UseCompressedOops) {
    // Must compress the head ptr.
    _compressed_head = CompressedOops::encode(o);
  } else {
    _oop_head = o;
  }
}

bool DiscoveredList::is_empty() const {
 return head() == NULL;
}

void DiscoveredList::clear() {
  set_head(NULL);
  set_length(0);
}

DiscoveredListIterator::DiscoveredListIterator(DiscoveredList&    refs_list,
                                               OopClosure*        keep_alive,
                                               BoolObjectClosure* is_alive):
  _refs_list(refs_list),
  _prev_discovered_addr(refs_list.adr_head()),
  _prev_discovered(NULL),
  _current_discovered(refs_list.head()),
  _current_discovered_addr(NULL),
  _next_discovered(NULL),
  _referent(NULL),
  _keep_alive(keep_alive),
  _is_alive(is_alive),
#ifdef ASSERT
  _first_seen(refs_list.head()),
#endif
  _processed(0),
  _removed(0) {
}

#endif // SHARE_GC_SHARED_REFERENCEPROCESSOR_INLINE_HPP
