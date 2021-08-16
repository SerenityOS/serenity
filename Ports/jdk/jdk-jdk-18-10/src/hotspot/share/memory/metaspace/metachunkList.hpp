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

#ifndef SHARE_MEMORY_METASPACE_METACHUNKLIST_HPP
#define SHARE_MEMORY_METASPACE_METACHUNKLIST_HPP

#include "memory/metaspace/counters.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "utilities/globalDefinitions.hpp"

class outputStream;

namespace metaspace {

// A simple single-linked list of chunks, used in MetaspaceArena to keep
//  a list of retired chunks, as well as in the ChunkHeaderPool to keep
//  a cache of unused chunk headers.

class MetachunkList {

  Metachunk* _first;
  IntCounter _num_chunks;

  // Note: The chunks inside this list may be dead (->chunk header pool).
  // So, do not call c->word size on them or anything else which may not
  // work with dead chunks.

  // Check that list does not contain the given chunk; Note that since that check
  //  is expensive, it is subject to VerifyMetaspaceInterval.
  DEBUG_ONLY(void verify_does_not_contain(const Metachunk* c) const;)

public:

  MetachunkList() : _first(NULL), _num_chunks() {}

  int count() const { return _num_chunks.get(); }

  void add(Metachunk* c) {
    DEBUG_ONLY(verify_does_not_contain(c);)
    c->set_next(_first);
    if (_first) {
      _first->set_prev(c);
    }
    _first = c;
    _num_chunks.increment();
  }

  Metachunk* remove_first() {
    if (_first) {
      Metachunk* c = _first;
      _first = _first->next();
      if (_first) {
        _first->set_prev(NULL);
      }
      _num_chunks.decrement();
      c->set_prev(NULL);
      c->set_next(NULL);
      return c;
    }
    return NULL;
  }

  Metachunk* first()              { return _first; }
  const Metachunk* first() const  { return _first; }

#ifdef ASSERT
  // Note: linear search
  bool contains(const Metachunk* c) const;
  void verify() const;
#endif

  size_t calc_committed_word_size() const;
  size_t calc_word_size() const;

  void print_on(outputStream* st) const;

};

} // namespace metaspace

#endif // SHARE_MEMORY_METASPACE_METACHUNKLIST_HPP
